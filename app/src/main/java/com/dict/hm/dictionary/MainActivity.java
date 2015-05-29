package com.dict.hm.dictionary;

import android.app.Fragment;
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
import android.widget.Toast;

import com.dict.hm.dictionary.async.UserAsyncWorkerHandler;
import com.dict.hm.dictionary.dict.DictContentProvider;
import com.dict.hm.dictionary.dict.DictFormat;
import com.dict.hm.dictionary.dict.DictManager;
import com.dict.hm.dictionary.dict.DictSQLiteDefine;
import com.dict.hm.dictionary.dict.UserDictSQLiteOpenHelper;
import com.dict.hm.dictionary.paper.PaperJsonReader;
import com.dict.hm.dictionary.paper.PaperViewerAdapter;
import com.dict.hm.dictionary.paper.PaperViewerFragment;
import com.dict.hm.dictionary.parse.DictParser;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.util.ArrayList;


public class MainActivity extends AppCompatActivity
        implements UserAsyncWorkerHandler.UserDictQueryListener,
        SwitchDictDialog.SwitchDictDialogListener{

    private String TAG = "MainActivity";

    private Toolbar toolbar;
    private DrawerLayout drawerLayout;
    private LinearLayout leftDrawerLayout;
    private ActionBarDrawerToggle drawerToggle;
    private NavigationDrawerAdapter adapter;

    private SearchView searchView;
    private DefinitionFragment definitionFragment = null;
    private TextView wordView;
    private ListView resultListView;
    private MenuItem searchItem;
    private boolean canDismiss = false;

    private final int action_query_word = 0;
    private final int action_show_paper = 1;
    private int listAction;

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

        initNavigationDrawer();
        manager = DictManager.getInstance(this);
        if (manager.isInited()) {
            updateDictData();
        } else {
            QueryCallback callback = new QueryCallback();
            manager.setOnQueryCompleteCallback(callback);
        }
        /**
         * I don't clearly remember why this need to handleIntent()
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
            jsonReader.closeJson();
            jsonReader = null;
        }
        Log.d(TAG, "onDestroy");
        super.onDestroy();
    }

    @Override
    public void onBackPressed() {
        if (drawerLayout.isDrawerOpen(leftDrawerLayout)) {
            drawerLayout.closeDrawer(leftDrawerLayout);
            return;
        }
        if (canDismiss) {
//            dismissDefinition(true);
            dismissFragment();
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
//            update adapter data;
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
             * exist query
             */
            Uri uri = intent.getData();
            String word = intent.getStringExtra(SearchManager.EXTRA_DATA_KEY);
            Cursor cursor = getContentResolver().query(uri, null, null, null, null);
            parseDefinition(word, cursor);
        } else if (Intent.ACTION_SEARCH.equals(intent.getAction())) {
            String query = intent.getStringExtra(SearchManager.QUERY);
            getQueryResults(query);
        } /**else if (PI2.equals(intent.getAction())) {
         Toast.makeText(this, "Notification!-->2", Toast.LENGTH_LONG).show();
         }*/
    }

    /** ----------------------------Navigation drawer --------------------------------------------*/

    private void initNavigationDrawer() {
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
        adapter = new NavigationDrawerAdapter(this);
        drawerListView.setAdapter(adapter);
        drawerListView.setOnItemClickListener(new AdapterView.OnItemClickListener() {
            @Override
            public void onItemClick(AdapterView<?> parent, View view, int position, long id) {
                drawerLayout.closeDrawer(leftDrawerLayout);
                if (canDismiss) {
//                    dismissDefinition(false);
                    dismissFragment();
                }
                switch (adapter.getItemViewType(position)) {
                    case NavigationDrawerAdapter.PERSONAL_DICT:
                        showUserDict();
                        break;
                    case NavigationDrawerAdapter.SWITCH_DICT:
                        showSwitchDictDialog();
                        break;
                    case NavigationDrawerAdapter.LIST_PAPER:
                        listPaper();
                        break;
                    case NavigationDrawerAdapter.MANAGE_DICT:
                        startManagerActivity(R.id.action_add_dict);
                        break;
                    case NavigationDrawerAdapter.MANAGE_PAPER:
                        startManagerActivity(R.id.action_add_paper);
                        break;
                    case NavigationDrawerAdapter.SETTINGS:
                        break;
                    case NavigationDrawerAdapter.ABOUT:
                        break;
                }
            }
        });
    }

    private void updateDictData() {
        switchActiveDict(manager.getActiveDict());
    }

    private void switchActiveDict(int active) {
        Log.d(TAG, "active:" + active);
        DictFormat format = manager.getDictFormat(active);
        if (format != null) {
            setDictFile(format.getType(), format.getData());
            manager.setActiveDict(active);
//            wordView.setText(format.getName());
//        } else {
//            wordView.setText("");
        }
//        resultListView.setAdapter(null);
    }

    /** ----------------------------Switch dictionary--------------------------------------------*/

    private void showSwitchDictDialog() {
        ArrayList<DictFormat> arrayList = manager.getDictFormats();
        if (arrayList.size() < 1) {
            Toast.makeText(this, "Haven't got any dictionary", Toast.LENGTH_LONG).show();
            return;
        }
        CharSequence[] items = new CharSequence[arrayList.size()];
        for (int i = 0; i < arrayList.size(); i++) {
            items[i] = arrayList.get(i).getName();
        }

        Bundle bundle = new Bundle();
        bundle.putCharSequenceArray(SwitchDictDialog.ARRAY_DATA, items);
        bundle.putInt(SwitchDictDialog.CHECKED, manager.getActiveDict());
        SwitchDictDialog dialog = new SwitchDictDialog();
        dialog.setArguments(bundle);
        dialog.show(getFragmentManager(), null);
    }

    @Override
    public void onSwitchDictClick(int which) {
        switchActiveDict(which);
    }

    /** ----------------------------Search and Query---------------------------------------------*/
    /**
     * TODO: should use WordAsyncQueryHandler to query words?
     */

    AdapterView.OnItemClickListener resultListViewListener = new AdapterView.OnItemClickListener(){
        @Override
        public void onItemClick(AdapterView<?> parent, View view, int position, long id) {
            if (listAction == action_query_word) {
                Item item = (Item) parent.getAdapter().getItem(position);
                Cursor cursor = getWordIndex(item.id);
                parseDefinition(item.text, cursor);
            } else if (listAction == action_show_paper){
                showPaper((String) parent.getAdapter().getItem(position));
            }
        }
    };

    /**
     * have query action
     */
    public void getQueryResults(String word) {
        String countString;
        boolean noWordEqual = true;
        ArrayAdapter<Item> words = null;
        ContentResolver contentResolver = getContentResolver();
        final Cursor cursor = contentResolver.query(DictContentProvider.CONTENT_URI, null, null,
                new String[]{word}, null);
        if (null == cursor) {
            countString = getString(R.string.no_results, new Object[]{word});
        } else {
            int count = cursor.getCount();
            countString = getResources().getQuantityString(R.plurals.search_results,
                    count, new Object[] {count, word});

            words = new ArrayAdapter<>(this, R.layout.textview_item);
            ArrayList<Item> list = new ArrayList<>();
            int idIndex = cursor.getColumnIndex(BaseColumns._ID);
            int wordIndex = cursor.getColumnIndexOrThrow(DictSQLiteDefine.COLUMN_KEY_WORD);
            cursor.moveToFirst();
            word = word.toLowerCase();
            String lowerCase;
            do {
                String text = cursor.getString(wordIndex);
                lowerCase = text.toLowerCase();
                Item item = new Item(cursor.getLong(idIndex), text);
                if (lowerCase.startsWith(word)) {
                    words.add(item);
                    if (noWordEqual && lowerCase.equals(word)) {
                        //parse and show definition
                        parseDefinition(text, getWordIndex(item.id));
                        noWordEqual = false;
                    }
                } else {
                    list.add(item);
                }
            } while (cursor.moveToNext());
            words.addAll(list);
            cursor.close();
        }
        //no word to show, dismiss definition
        if (canDismiss && noWordEqual) {
            dismissDefinition(true);
        }
        wordView.setText(countString);
        resultListView.setAdapter(words);
        listAction = action_query_word;
    }

    /**
     * have query action
     */
    private Cursor getWordIndex(long rowId) {
        Uri uri = Uri.parse(DictContentProvider.CONTENT_URI + "/" + rowId);
        return getContentResolver().query(uri, null, null, null, null);
    }

    /**
     *
     * @param cursor contains the definition of a word.
     *               will create a Dialog fragment to show word definition.
     * TODO: different dict format should have different parseDefinition() function;
     */
    private void parseDefinition(String word, Cursor cursor) {
        if (cursor != null) {
            cursor.moveToFirst();
            int offsetIndex = cursor.getColumnIndex(DictSQLiteDefine.COLUMN_OFFSET);
            int sizeIndex = cursor.getColumnIndex(DictSQLiteDefine.COLUMN_SIZE);
            int offset = cursor.getInt(offsetIndex);
            int size = cursor.getInt(sizeIndex);
            String definition = null;
            if (dictParser != null) {
                definition = dictParser.getWordDefinition(offset, size);
            }
            if (definition == null){
                //TODO: define the string in strings.xml
                definition = "occur error while reading text from .dict file";
            }
            showDefinition(word, definition);
            cursor.close();
        }
    }

    private void showDefinition(String word, String definition) {
        if (canDismiss && definitionFragment != null) {
            definitionFragment.updateViewData(word, definition);
        } else {
            if (definitionFragment == null) {
                definitionFragment = new DefinitionFragment();
            }
            Bundle bundle = new Bundle();
            bundle.putString(DefinitionFragment.WORD, word);
            bundle.putString(DefinitionFragment.DEF, definition);
            definitionFragment.setArguments(bundle);
//            getFragmentManager().beginTransaction()
//                    .add(R.id.main_content, definitionFragment)
//                    .setTransition(FragmentTransaction.TRANSIT_FRAGMENT_OPEN)
//                    .commit();
//            canDismiss = true;
//            resultListView.setVisibility(View.INVISIBLE);
            displayFragment(definitionFragment);
        }
        searchItem.collapseActionView();
        Log.d("searchView", "clear focus");
    }

    private void dismissDefinition(boolean focus) {
//        getFragmentManager().beginTransaction()
//                .remove(definitionFragment)
//                .setTransition(FragmentTransaction.TRANSIT_FRAGMENT_CLOSE)
//                .commit();
//        canDismiss = false;
//        resultListView.setVisibility(View.VISIBLE);
        dismissFragment();
        definitionFragment = null;
        if (focus) {
//            searchView.requestFocus();
//            InputMethodManager imm = (InputMethodManager) getSystemService(INPUT_METHOD_SERVICE);
//            imm.toggleSoftInput(0, InputMethodManager.HIDE_NOT_ALWAYS);
            searchItem.expandActionView();
            searchView.clearFocus();
        }
    }

    /** ----------------------------Paper viewer-------------------------------------------------*/

    private void listPaper() {
        ArrayList<String> papers = manager.getPapers();
        ArrayAdapter<String> adapter = new ArrayAdapter<>(this, R.layout.textview_item, papers);
        resultListView.setAdapter(adapter);
        wordView.setText(R.string.drawer_paper_list);
        listAction = action_show_paper;
    }

    private void showPaper(String fileName) {
        Bundle bundle = new Bundle();
        bundle.putString(PaperViewerFragment.PAPERNAME, fileName);
        PaperViewerFragment fragment = new PaperViewerFragment();
        fragment.setArguments(bundle);
        displayFragment(fragment);
    }

    /**
    private void showPaper(String fileName) {
        File paperFile = new File(manager.getPaperDir(), fileName);
        if (jsonReader != null) {
            jsonReader.closeJson();
        }
        jsonReader = new PaperJsonReader(paperFile);
        jsonReader.openJson();
        //TODO:quit the word query thread
        PaperViewerAdapter paperViewerAdapter = new PaperViewerAdapter(this, jsonReader, dictParser);
        wordView.setText(fileName);
        resultListView.setAdapter(paperViewerAdapter);
        listAction = -1;
        Log.d(TAG, "Paper switch");
    }
     */

    public DictParser getDictParser() {
        return dictParser;
    }

    /** ----------------------------Fragment manager---------------------------------------------*/

    private void displayFragment(Fragment fragment) {
        FragmentTransaction transaction= getFragmentManager().beginTransaction();
        if (canDismiss) {
            transaction.replace(R.id.main_content, fragment, TAG);
        } else {
            transaction.add(R.id.main_content, fragment, TAG);
        }
        transaction.setTransition(FragmentTransaction.TRANSIT_FRAGMENT_OPEN);
        transaction.commit();
        canDismiss = true;
        resultListView.setVisibility(View.INVISIBLE);
    }

    private void dismissFragment() {
        Fragment fragment = getFragmentManager().findFragmentByTag(TAG);
        getFragmentManager().beginTransaction()
                .remove(fragment)
                .setTransition(FragmentTransaction.TRANSIT_FRAGMENT_CLOSE)
                .commit();
        canDismiss = false;
        resultListView.setVisibility(View.VISIBLE);
    }


    /** -----------------------------------------------------------------------------------------*/
    /**
     * TODO: show my dictionary order by time will need to load the whole dictionary
     */
    private void showUserDict() {
        if (canDismiss) {
//            dismissDefinition(false);
            dismissFragment();
        }
        UserAsyncWorkerHandler userHandler = UserAsyncWorkerHandler.getInstance(this, null);
        userHandler.setUserDictQueryListener(this);
        userDictAdapter = new UserDictAdapter(this, userHandler);
        wordView.setText(R.string.action_user_dict);
        resultListView.setAdapter(userDictAdapter);
        listAction = -1;
    }

    private void clearUserDict() {
        UserDictSQLiteOpenHelper helper = UserDictSQLiteOpenHelper.getInstance(this);
        helper.clearUserWords();
        Toast.makeText(this, "Words Clear!", Toast.LENGTH_LONG).show();
    }

    /** -------------------define a callback for DictManager to update DrawerAdapter-------------*/

    interface QueryCompleteCallback {
        void onQueryComplete();
    }

    public class QueryCallback implements QueryCompleteCallback {
        @Override
        public void onQueryComplete() {
            updateDictData();
            Log.d(TAG, "QueryCallback");
        }
    }

    /** -----------------------------------------------------------------------------------------*/
    //TODO: remove the lastID, sort the words by count or time. get all the words in one time.
    //without sort this function won't help user to know their words better
    @Override
    public void onUserDictQueryComplete(Cursor cursor) {
        ArrayList<String> words = new ArrayList<>();
        ArrayList<Long> counts = new ArrayList<>();
        ArrayList<String> times = new ArrayList<>();
        long lastID = -1;

        if (cursor != null) {
            if (cursor.moveToFirst()) {
                int idIndex = cursor.getColumnIndex("rowid");
                int wordIndex = cursor.getColumnIndex(UserDictSQLiteOpenHelper.COLUMN_WORD);
                int countIndex = cursor.getColumnIndex(UserDictSQLiteOpenHelper.COLUMN_COUNT);
                int timeIndex = cursor.getColumnIndex(UserDictSQLiteOpenHelper.COLUMN_TIME);
                do {
                    words.add(cursor.getString(wordIndex));
                    counts.add(cursor.getLong(countIndex));
                    times.add(cursor.getString(timeIndex));
                } while (cursor.moveToNext());
                cursor.moveToLast();
                lastID = cursor.getLong(idIndex);
                Log.d("lastID", "" + lastID);
            }
            cursor.close();
        }
        if (userDictAdapter != null) {
            userDictAdapter.updateAdapterData(words, counts, times, lastID);
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
