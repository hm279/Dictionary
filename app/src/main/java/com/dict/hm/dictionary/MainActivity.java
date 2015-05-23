package com.dict.hm.dictionary;

import android.app.FragmentTransaction;
import android.app.SearchManager;
import android.content.ContentResolver;
import android.content.Context;
import android.content.Intent;
import android.database.Cursor;
import android.net.Uri;
import android.os.Build;
import android.os.Bundle;
import android.os.StrictMode;
import android.provider.BaseColumns;
import android.support.v4.view.MenuItemCompat;
import android.support.v4.widget.DrawerLayout;
import android.support.v7.app.ActionBar;
import android.support.v7.app.ActionBarDrawerToggle;
import android.support.v7.app.AppCompatActivity;
import android.support.v7.widget.SearchView;
import android.support.v7.widget.Toolbar;
import android.util.Log;
import android.util.TypedValue;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.widget.AbsListView;
import android.widget.AdapterView;
import android.widget.ArrayAdapter;
import android.widget.AutoCompleteTextView;
import android.widget.LinearLayout;
import android.widget.ListView;
import android.widget.TextView;

import com.dict.hm.dictionary.async.UserAsyncWorkerHandler;
import com.dict.hm.dictionary.dict.DictContentProvider;
import com.dict.hm.dictionary.dict.DictFormat;
import com.dict.hm.dictionary.dict.DictManager;
import com.dict.hm.dictionary.dict.DictSQLiteDefine;
import com.dict.hm.dictionary.dict.UserDictSQLiteOpenHelper;
import com.dict.hm.dictionary.paper.PaperJsonReader;
import com.dict.hm.dictionary.paper.PaperViewerAdapter;
import com.dict.hm.dictionary.parse.DictParser;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.util.ArrayList;


public class MainActivity extends AppCompatActivity
        implements UserAsyncWorkerHandler.UserDictQueryListener{

    private String TAG = "MainActivity";

    private Toolbar toolbar;
    private DrawerLayout drawerLayout;
    private LinearLayout leftDrawerLayout;
    private ActionBarDrawerToggle drawerToggle;
    private DrawerListViewAdapter drawerAdapter;

    private SearchView searchView;
    private DefinitionFragment definitionFragment = null;
    private TextView wordView;
    private ListView resultListView;
    private MenuItem searchItem;
    private boolean canDismiss = false;

    private DictParser dictParser = null;
    private DictManager manager = null;
    private PaperJsonReader jsonReader = null;
    private UserDictAdapter userDictAdapter = null;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        toolbar = (Toolbar) findViewById(R.id.toolbar);
        setSupportActionBar(toolbar);
        ActionBar actionBar = getSupportActionBar();
        if (actionBar != null) {
            actionBar.setDisplayHomeAsUpEnabled(true);
            actionBar.setHomeButtonEnabled(true);
        }

        resultListView = (ListView) findViewById(R.id.result_listView);
        resultListView.setChoiceMode(AbsListView.CHOICE_MODE_NONE);
        resultListView.setOnItemClickListener(resultListViewListener);
        wordView = (TextView) getLayoutInflater().inflate(R.layout.textview_item, null);
        wordView.setTextColor(getResources()
                .getColor(R.color.secondary_text_default_material_light));
        wordView.setTextSize(TypedValue.COMPLEX_UNIT_SP, 18);
        resultListView.addHeaderView(wordView);

        initDrawerNavigation();
        manager = DictManager.getInstance(this);
        if (manager.isInited()) {
            updateDrawerAdapterData();
        } else {
            QueryCallback callback = new QueryCallback();
            manager.setOnQueryCompleteCallback(callback);
        }
        /**
         * I don't know why this need to handleIntent()
         * maybe the notification need this.
         */
//        if ((getIntent().getFlags() & Intent.FLAG_ACTIVITY_LAUNCHED_FROM_HISTORY) != 0) {
//        handleIntent(getIntent());
//        }

        if (BuildConfig.DEBUG) {
//        setDefaultKeyMode(DEFAULT_KEYS_SEARCH_LOCAL);
//        FragmentManager.enableDebugLogging(true);
            StrictMode.setVmPolicy(new StrictMode.VmPolicy.Builder()
                    .detectLeakedSqlLiteObjects()
                    .detectLeakedClosableObjects()
                    .detectAll()
                    .penaltyLog()
                    .penaltyDeath()
                    .build());
            StrictMode.setThreadPolicy(new StrictMode.ThreadPolicy.Builder()
                    .detectCustomSlowCalls()
                    .penaltyDeath()
                    .penaltyLog()
                    .build());
            Log.d(TAG, "onCreate");
        }
    }

    @Override
    public void onDestroy() {
        if (dictParser != null) {
            dictParser.closeFile();
            dictParser = null;
        }
        if (jsonReader != null) {
            jsonReader.close();
            jsonReader = null;
        }
//        manager.saveActiveBook();
        super.onDestroy();
    }

    @Override
    public void onBackPressed() {
        if (drawerLayout.isDrawerOpen(leftDrawerLayout)) {
            drawerLayout.closeDrawer(leftDrawerLayout);
            return;
        }
        if (canDismiss) {
            dismissDefinition(true);
            return;
        }
        super.onBackPressed();
    }

    @Override
    protected void onNewIntent(Intent intent) {
        Log.d(TAG, "onNewIntent");
        /**
         * When the system calls onNewIntent(Intent), the activity has not been restarted,
         * so the getIntent() method returns the same intent that was received with onCreate().
         * This is why you should call setIntent(Intent) inside onNewIntent(Intent) (so that the
         * intent saved by the activity is updated in case you call getIntent() in the future)
         */
        setIntent(intent);
        handleIntent(intent);
    }

    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        // Inflate the menu; this adds items to the action bar if it is present.
        getMenuInflater().inflate(R.menu.menu_main, menu);

        SearchManager searchManager = (SearchManager) getSystemService(Context.SEARCH_SERVICE);
        searchItem = menu.findItem(R.id.search);
        searchView = (SearchView) MenuItemCompat.getActionView(searchItem);
        searchView.setSubmitButtonEnabled(true);
        searchView.setSearchableInfo(searchManager.getSearchableInfo(getComponentName()));
//        searchView.setIconifiedByDefault(false);

        final AutoCompleteTextView textView = (AutoCompleteTextView) searchView
                .findViewById(R.id.search_src_text);
//        if (textView != null) {
//            int color = getResources().getColor(android.R.color.white);
//            textView.setDropDownBackgroundDrawable(new ColorDrawable(color));
//        }

        searchView.setOnQueryTextFocusChangeListener(new View.OnFocusChangeListener() {
            @Override
            public void onFocusChange(View v, boolean hasFocus) {
                if (hasFocus && textView != null) {
                    textView.setText("");
                }
            }
        });
        return true;
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        /** Handle action bar item clicks here. The action bar will
         *  automatically handle clicks on the Home/Up button, so long
         *  as you specify a parent activity in AndroidManifest.xml.
         */
        int id = item.getItemId();
        if (drawerToggle.onOptionsItemSelected(item)) {
            return true;
        }
        switch (id) {
            case R.id.action_settings:
                //for test
//                NotificationDialog.newInstance("this is a test").show(getFragmentManager(), null);
                copy();
                return true;
            case R.id.action_add_dict:
                startManagerActivity(R.id.action_add_dict);
                return true;
            case R.id.action_add_paper:
                startManagerActivity(R.id.action_add_paper);
                return true;
            case R.id.action_my_dict:
                showUserDict();
                return true;
            case R.id.action_clear_myDict:
                clearUserDict();
                return true;
            case R.id.search:
//                onSearchRequested();
                return true;
            default:
                return super.onOptionsItemSelected(item);
        }
    }

    @Override
    protected void onActivityResult(int requestCode, int resultCode, Intent data) {
        /**
         * check whether the dictionary change, and update the drawerListView's data
         */
        if (manager != null) {
            updateDrawerAdapterData();
        }
        super.onActivityResult(requestCode, resultCode, data);
    }

    private void startManagerActivity(int id) {
//        if (BuildConfig.DEBUG) {
//            System.gc();
//        }
        if (id == R.id.action_add_dict) {
            Intent intent = new Intent(this, DictManagerActivity.class);
            startActivityForResult(intent, 0);
        } else {
            Intent intent = new Intent(this, PaperManagerActivity.class);
            startActivityForResult(intent, 0);
        }
    }

    /**
     *
     * @param intent
     * Handle the query request from SearchView's suggestion query or search query.
     */
    private void handleIntent(Intent intent) {
        Log.d(TAG, "intent action:" + intent.getAction());
        if (Intent.ACTION_VIEW.equals(intent.getAction())) {
            /**
             * called for SearchView's suggestion.
             */
            Uri uri = intent.getData();
            String word = intent.getStringExtra(SearchManager.EXTRA_DATA_KEY);
            Cursor cursor = getContentResolver().query(uri, null, null, null, null);
            showWordDefinition(word, cursor);
        } else if (Intent.ACTION_SEARCH.equals(intent.getAction())) {
            String query = intent.getStringExtra(SearchManager.QUERY);
            showQueryResults(query);
        } /**else if (PI2.equals(intent.getAction())) {
         Toast.makeText(this, "Notification!-->2", Toast.LENGTH_LONG).show();
         }*/
    }

    /** ----------------------------Drawer navigation--------------------------------------------*/

    private void initDrawerNavigation() {
        drawerLayout = (DrawerLayout) findViewById(R.id.drawer_layout);
        drawerLayout.setStatusBarBackgroundColor(getResources().getColor(
                R.color.material_teal_700));
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.LOLLIPOP) {
            getWindow().setStatusBarColor(0x0);
        }

        leftDrawerLayout = (LinearLayout) findViewById(R.id.left_drawerLayout);
        drawerToggle = new ActionBarDrawerToggle(this, drawerLayout, toolbar,
                R.string.drawer_open, R.string.drawer_close) {
            @Override
            public void onDrawerOpened(View drawerView) {
                super.onDrawerOpened(drawerView);
            }

            @Override
            public void onDrawerClosed(View drawerView) {
                super.onDrawerClosed(drawerView);
            }
        };
        drawerToggle.syncState();
        drawerLayout.setDrawerListener(drawerToggle);

        ListView drawerListView = (ListView) findViewById(R.id.drawer_listView);
        drawerAdapter = new DrawerListViewAdapter(this);
        drawerListView.setAdapter(drawerAdapter);
        drawerListView.setOnItemClickListener(new AdapterView.OnItemClickListener() {
            @Override
            public void onItemClick(AdapterView<?> parent, View view, int position, long id) {
                drawerLayout.closeDrawer(leftDrawerLayout);

                int type = drawerAdapter.getItemViewType(position);
                String item = (String) drawerAdapter.getItem(position);
                if (type == DrawerListViewAdapter.TYPE_ITEM_BOOK) {
                    switchActiveDict((int) drawerAdapter.getItemId(position));
                    drawerAdapter.notifyDataSetChanged();
                } else if (type == DrawerListViewAdapter.TYPE_ITEM_PAPER) {
                    showPaper(item);
                } else if (type == DrawerListViewAdapter.TYPE_TITLE) {
                    if (position == 0) {
                        startManagerActivity(R.id.action_add_dict);
                    } else {
                        startManagerActivity(R.id.action_add_paper);
                    }
                }
            }
        });
    }

    private void updateDrawerAdapterData() {
        drawerAdapter.setBookNames(manager.getDictFormats());
        drawerAdapter.setPapers(getPapers());
        drawerAdapter.notifyDataSetChanged();
        switchActiveDict(manager.getActiveDict());
    }

    private void switchActiveDict(int active) {
        Log.d(TAG, "active:" + active);
        DictFormat format = manager.getDictFormat(active);
        if (format != null) {
            setDictFile(format.getType(), format.getData());
            manager.setActiveDict(active);
            wordView.setText(format.getName());
        } else {
            wordView.setText("");
        }
        resultListView.setAdapter(null);
    }

    /** ----------------------------Search and Query---------------------------------------------*/
    /**
     * TODO: should use WordAsyncQueryHandler to query words?
     */

    AdapterView.OnItemClickListener resultListViewListener = new AdapterView.OnItemClickListener(){
        @Override
        public void onItemClick(AdapterView<?> parent, View view, int position, long id) {
            if (!(parent.getAdapter().getItem(position) instanceof Item)) {
                return;
            }
            Item item = (Item) parent.getAdapter().getItem(position);
            String definition = getWordDefinition(item.id);
            showDefinition(item.text, definition);
        }
    };

    public void showQueryResults(String query) {
        String countString;
        boolean b = true;
        ArrayAdapter<Item> words = null;
        ContentResolver contentResolver = getContentResolver();
        final Cursor cursor = contentResolver.query(DictContentProvider.CONTENT_URI, null, null,
                new String[]{query}, null);
        if (null == cursor) {
            countString = getString(R.string.no_results, new Object[]{query});
        } else {
            int count = cursor.getCount();
            countString = getResources().getQuantityString(R.plurals.search_results,
                    count, new Object[] {count, query});

            words = new ArrayAdapter<>(this, R.layout.textview_item);
            ArrayList<Item> list = new ArrayList<>();
            int idIndex = cursor.getColumnIndex(BaseColumns._ID);
            int wordIndex = cursor.getColumnIndexOrThrow(DictSQLiteDefine.KEY_WORD);
            cursor.moveToFirst();
            query = query.toLowerCase();
            String lowerCase;
            do {
                String text = cursor.getString(wordIndex);
                lowerCase = text.toLowerCase();
                Item item = new Item(cursor.getLong(idIndex), text);
                if (lowerCase.startsWith(query)) {
                    words.add(item);
                    if (b && lowerCase.equals(query)) {
                        String definition = getWordDefinition(item.id);
                        showDefinition(text, definition);
                        b = false;
                        Log.d(TAG, "-" + text);
                    }
                } else {
                    list.add(item);
                }
            } while (cursor.moveToNext());
            words.addAll(list);
            cursor.close();
        }
        if (canDismiss && b) {
            dismissDefinition(false);
        }
        wordView.setText(countString);
        resultListView.setAdapter(words);
    }

    private String getWordDefinition(long rowId) {
        Uri uri = Uri.parse(DictContentProvider.CONTENT_URI + "/" + rowId);
        Cursor cursor = getContentResolver().query(uri, null, null, null, null);
        if (cursor != null) {
            cursor.moveToFirst();
            int offsetIndex = cursor.getColumnIndex(DictSQLiteDefine.OFFSET);
            int sizeIndex = cursor.getColumnIndex(DictSQLiteDefine.SIZE);
            String wordDefinition = null;
            if (dictParser != null) {
                wordDefinition = dictParser.getWordDefinition(cursor.getInt(offsetIndex), cursor.getInt(sizeIndex));
            }
            if (wordDefinition == null) {
                wordDefinition = "occur error while reading text from .dict file";
            }
            cursor.close();
            return wordDefinition;
        }
        return null;
    }

    /**
     *
     * @param cursor contains the definition of a word.
     *               will create a Dialog fragment to show word definition.
     */
    private void showWordDefinition(String word, Cursor cursor) {
        if (cursor != null) {
            cursor.moveToFirst();
            int offsetIndex = cursor.getColumnIndex(DictSQLiteDefine.OFFSET);
            int sizeIndex = cursor.getColumnIndex(DictSQLiteDefine.SIZE);
            int offset = cursor.getInt(offsetIndex);
            int size = cursor.getInt(sizeIndex);
            String definition = null;
            if (dictParser != null) {
                definition = dictParser.getWordDefinition(offset, size);
            }
            if (definition == null){
                definition = "occur error while reading text from .dict file";
            }
            showDefinition(word, definition);
            cursor.close();
        }
    }

    private void showDefinition(String word, String definition) {
        if (canDismiss) {
            definitionFragment.updateViewData(word, definition);
        } else {
            if (definitionFragment == null) {
                definitionFragment = new DefinitionFragment();
            }
            Bundle bundle = new Bundle();
            bundle.putString(DefinitionFragment.WORD, word);
            bundle.putString(DefinitionFragment.DEF, definition);
            definitionFragment.setArguments(bundle);
            getFragmentManager().beginTransaction()
                    .add(R.id.main_content, definitionFragment)
                    .setTransition(FragmentTransaction.TRANSIT_FRAGMENT_OPEN)
                    .commit();
            canDismiss = true;
            resultListView.setVisibility(View.INVISIBLE);
        }
        searchItem.collapseActionView();
        Log.d("searchView", "clear focus");
    }

    private void dismissDefinition(boolean focus) {
        getFragmentManager().beginTransaction()
                .remove(definitionFragment)
                .setTransition(FragmentTransaction.TRANSIT_FRAGMENT_CLOSE)
                .commit();
        canDismiss = false;
        resultListView.setVisibility(View.VISIBLE);
//        if (focus) {
//            searchView.requestFocus();
//            InputMethodManager imm = (InputMethodManager) getSystemService(INPUT_METHOD_SERVICE);
//            imm.toggleSoftInput(0, InputMethodManager.HIDE_NOT_ALWAYS);
//        }
        searchItem.expandActionView();
        searchView.clearFocus();
    }

    /** -----------------------------------------------------------------------------------------*/

    private ArrayList<String> getPapers() {
        ArrayList<String> papers = new ArrayList<>();
        File paperDir = new File(getExternalFilesDir(null), "paper");
        if (paperDir.exists()) {
            for (String name : paperDir.list()) {
                papers.add(name);
            }
        }
        return papers;
    }

    private void showPaper(String fileName) {
        File paperDir = new File(getExternalFilesDir(null), "paper");
        File paperFile = new File(paperDir, fileName);
        if (jsonReader != null) {
            jsonReader.close();
        }
        jsonReader = new PaperJsonReader(paperFile);
        //TODO:quit the word query thread
        PaperViewerAdapter paperViewerAdapter = new PaperViewerAdapter(this, jsonReader, dictParser);
        wordView.setText(fileName);
        resultListView.setAdapter(paperViewerAdapter);
        Log.d(TAG, "Paper switch");
    }

    /** -----------------------------------------------------------------------------------------*/

    /**
     * TODO: show my dictionary order by time will need to load the whole dictionary
     */
    private void showUserDict() {
        UserAsyncWorkerHandler userHandler = UserAsyncWorkerHandler.getInstance(this, null);
        userHandler.setUserDictQueryListener(this);
        userDictAdapter = new UserDictAdapter(this, userHandler);
        wordView.setText("Your Words");
        resultListView.setAdapter(userDictAdapter);
    }

    private void clearUserDict() {
        UserDictSQLiteOpenHelper helper = UserDictSQLiteOpenHelper.getInstance(this);
        helper.clearUserWords();
    }

    /** -------------------define a callback for DictManager to update DrawerAdapter-------------*/

    interface QueryCompleteCallback {
        void onQueryComplete();
    }

    public class QueryCallback implements QueryCompleteCallback {
        @Override
        public void onQueryComplete() {
            updateDrawerAdapterData();
            Log.d(TAG, "QueryCallback");
        }
    }

    /** -----------------------------------------------------------------------------------------*/

    @Override
    public void onUserDictQueryComplete(Cursor cursor) {
        ArrayList<String> words = new ArrayList<>();
        ArrayList<Long> times = new ArrayList<>();
        long lastID = -1;

        if (cursor != null) {
            if (cursor.moveToFirst()) {
                int idIndex = cursor.getColumnIndex("rowid");
                int wordIndex = cursor.getColumnIndex(UserDictSQLiteOpenHelper.COLUMN_WORD);
                int timeIndex = cursor.getColumnIndex(UserDictSQLiteOpenHelper.COLUMN_TIME);
                do {
                    words.add(cursor.getString(wordIndex));
                    times.add(cursor.getLong(timeIndex));
                } while (cursor.moveToNext());
                cursor.moveToLast();
                lastID = cursor.getLong(idIndex);
                Log.d("lastID", "" + lastID);
            }
            cursor.close();
        }
        if (userDictAdapter != null) {
            userDictAdapter.updateAdapterData(words, times, lastID);
        }
    }


    /** -----------------------------------------------------------------------------------------*/

    public void setDictFile(int type, String data) {
        if (type == 0) {
            /** type 0 means star dict format */
            if (data == null) {
                Log.d("error", "DictFormat's data field missing!");
                return;
            }
            String dictPath = data.substring(0, data.lastIndexOf(".ifo")).concat(".dict");
            File dict = new File(dictPath);
            if (dict.isFile()) {
                if (dictParser != null) {
                    dictParser.closeFile();
                }
                dictParser = new DictParser(dict);
            }
        }
    }

    private class Item {
        Long id;
        String text;

        private Item(Long id, String text) {
            this.id = id;
            this.text = text;
        }

        @Override
        public String toString() {
            return text;
        }
    }

    /** ------------------------------------------------------------------------------------------

    private void setNotification(int id, String msg) {
        Notification.Builder builder = new Notification.Builder(this)
                .setAutoCancel(true)
                .setSmallIcon(R.drawable.ic_launcher)
                .setContentTitle("notification")
                .setContentText(msg)
                .addAction(R.drawable.ic_action_previous_item, "", pendingIntent1)
                .addAction(R.drawable.ic_action_next_item, "", pendingIntent0)
                .setContentIntent(pendingIntent);

        NotificationManager notificationManager =
                (NotificationManager) getSystemService(NOTIFICATION_SERVICE);
        notificationManager.notify(id, builder.build());
    }

    private void initNotification() {
        Intent intent0 = new Intent(PI0);
        Intent intent1 = new Intent(PI1);
        pendingIntent0 = PendingIntent.getBroadcast(this, 0, intent0,
                PendingIntent.FLAG_UPDATE_CURRENT);
        pendingIntent1 = PendingIntent.getBroadcast(this, 0, intent1,
                PendingIntent.FLAG_UPDATE_CURRENT);
        IntentFilter filter = new IntentFilter();
        filter.addAction(PI0);
        filter.addAction(PI1);
        getApplicationContext().registerReceiver(new MyBroadcastReceiver(), filter);
        //TODO: Add unregisterReceiver() in suitable place

        Intent intent = new Intent(this, MainActivity.class);
        intent.setAction(PI2);
        pendingIntent = PendingIntent.getActivity(this, 0, intent, 0);
    }

    public static final String PI0 = "com.dict.hm.dictionary.0";
    public static final String PI1 = "com.dict.hm.dictionary.1";
    public static final String PI2 = "com.dict.hm.dictionary.2";
    PendingIntent pendingIntent;
    PendingIntent pendingIntent0;
    PendingIntent pendingIntent1;

    private class MyBroadcastReceiver extends BroadcastReceiver {
        @Override
        public void onReceive(Context context, Intent intent) {
            if (PI0.equals(intent.getAction())) {
                setNotification(1, null);
            } else if (PI1.equals(intent.getAction())) {
                setNotification(1, null);
            }
        }
    }
    ---------------------------------------------------------------------------------------------*/
    /**
     * for test
     */
    private void copy() {
        File src = getDatabasePath(UserDictSQLiteOpenHelper.getInstance(this).getDatabaseName());
        File dst = new File("/sdcard/test.db");
        FileInputStream in;
        FileOutputStream out;
        try {
            in = new FileInputStream(src);
            out = new FileOutputStream(dst);
            in.getChannel().transferTo(0, src.length(), out.getChannel());
            in.close();
            out.close();
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

}
