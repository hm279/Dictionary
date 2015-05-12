package com.dict.hm.dictionary;

import android.app.FragmentTransaction;
import android.app.Notification;
import android.app.NotificationManager;
import android.app.PendingIntent;
import android.app.SearchManager;
import android.content.BroadcastReceiver;
import android.content.ContentResolver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
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

import com.dict.hm.dictionary.dict.DictContentProvider;
import com.dict.hm.dictionary.dict.DictManager;
import com.dict.hm.dictionary.dict.DictSQLiteOpenHelper;
import com.dict.hm.dictionary.dict.MyDictAdapter;
import com.dict.hm.dictionary.paper.PaperJsonReader;
import com.dict.hm.dictionary.paper.PaperViewerAdapter;
import com.dict.hm.dictionary.parse.DictParser;

import java.io.File;
import java.util.ArrayList;


public class MainActivity extends AppCompatActivity {

    private String TAG = "MainActivity";

    private Toolbar toolbar;
    private DrawerLayout drawerLayout;
    private LinearLayout leftDrawerLayout;
    private ListView drawerListView;
    private ActionBarDrawerToggle drawerToggle;

    private SearchView searchView;
    private DefinitionFragment definitionFragment = null;
    private boolean canDismiss = false;
    private TextView wordView;
    private ListView resultListView;
    private MenuItem searchItem;

    private DictParser dictParser = null;
    private DictManager manager = null;
    private PaperJsonReader jsonReader = null;

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
        handleIntent(getIntent());

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
    protected void onStart() {
        super.onStart();
        if (manager == null) {
            manager = DictManager.getInstance(this);
            updateDrawerListViewAdapterData();
            switchActiveBookName(manager.getActiveBook());
            if (manager.getBooks().size() < 0) {
                wordView.setText("Hello!\nYou Haven't got any Dictionary");
            }
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
        manager.saveActiveBook();
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
//        setIntent(intent);
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
//                initNotification();
//                setNotification(1, "first");
                NotificationDialog.newInstance("this is a test").show(getFragmentManager(), null);
                return true;
            case R.id.action_add_dict:
                startManagerActivity(R.id.action_add_dict);
                return true;
            case R.id.action_add_paper:
                startManagerActivity(R.id.action_add_paper);
                return true;
            case R.id.action_my_dict:
                showMyDict();
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
            updateDrawerListViewAdapterData();
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

        drawerListView = (ListView) findViewById(R.id.drawer_listView);
        final DrawerListViewAdapter adapter = new DrawerListViewAdapter(this);
        drawerListView.setAdapter(adapter);
        drawerListView.setOnItemClickListener(new AdapterView.OnItemClickListener() {
            @Override
            public void onItemClick(AdapterView<?> parent, View view, int position, long id) {
                drawerLayout.closeDrawer(leftDrawerLayout);

                int type = adapter.getItemViewType(position);
                String item = (String) adapter.getItem(position);
//                String title = getString(R.string.dict);
                if (type == DrawerListViewAdapter.TYPE_ITEM_Book) {
                    adapter.setChecked(position);
                    switchActiveBookName(item);
                } else if (type == DrawerListViewAdapter.TYPE_ITEM_Paper) {
                    showPaper(item);
//                    title = getString(R.string.paper);
                } else if (type == DrawerListViewAdapter.TYPE_TITLE) {
                    if (position == 0) {
                        startManagerActivity(R.id.action_add_dict);
                    } else {
                        startManagerActivity(R.id.action_add_paper);
                    }
                }
//                getSupportActionBar().setTitle(title);
            }
        });
    }

    private void updateDrawerListViewAdapterData() {
        ArrayList<String> books = new ArrayList<>();
        books.addAll(manager.getBooks());
        DrawerListViewAdapter adapter = (DrawerListViewAdapter) drawerListView.getAdapter();
        adapter.setBookNames(books);
        adapter.setPapers(getPapers());
        if (manager.getActiveBook() != null) {
            adapter.setChecked(books.indexOf(manager.getActiveBook()) + 1);
        }
        adapter.notifyDataSetChanged();
        //changed to onStart()
//        switchActiveBookName(manager.getActiveBook());
    }

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

    /**
     *
     * @param activeBookName
     */
    private void switchActiveBookName(String activeBookName) {
        if (activeBookName != null) {
            String ifoPath = manager.getBookFilePath(activeBookName);
            if (ifoPath == null) {
                Log.d("error", "can not find book " + activeBookName + "'s file path");
                return;
            }
            String dictPath = ifoPath.substring(0, ifoPath.lastIndexOf(".ifo")).concat(".dict");
            File dict = new File(dictPath);
            if (dict.isFile()) {
                setDictFile(dict);
                manager.setActiveBook(activeBookName);
            }
            wordView.setText(activeBookName);
            resultListView.setAdapter(null);
        }
    }

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
            Cursor cursor = getContentResolver().query(uri, null, null, null, null);
            showWordDefinition(cursor);
        } else if (Intent.ACTION_SEARCH.equals(intent.getAction())) {
            String query = intent.getStringExtra(SearchManager.QUERY);
            showQueryResults(query);
        }else if (PI2.equals(intent.getAction())) {
            Toast.makeText(this, "Notification!-->2", Toast.LENGTH_LONG).show();
        }
    }

    AdapterView.OnItemClickListener resultListViewListener = new AdapterView.OnItemClickListener(){
        @Override
        public void onItemClick(AdapterView<?> parent, View view, int position, long id) {
            if (!(parent.getAdapter().getItem(position) instanceof Item)) {
                return;
            }
            Item item = (Item) parent.getAdapter().getItem(position);
            String definition = null;
            if (dictParser != null) {
                definition = dictParser.getWordDefinition(item.offset, item.size);
            }
            if (definition == null){
                definition = "occur error while reading dictionary file";
            }
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
            int wordIndex = cursor.getColumnIndexOrThrow(DictSQLiteOpenHelper.KEY_WORD);
            int offsetIndex = cursor.getColumnIndexOrThrow(DictSQLiteOpenHelper.KEY_OFFSET);
            int sizeIndex = cursor.getColumnIndexOrThrow(DictSQLiteOpenHelper.KEY_SIZE);
            cursor.moveToFirst();
            query = query.toLowerCase();
            String lowerCase;
            do {
                String text = cursor.getString(wordIndex);
                lowerCase = text.toLowerCase();
                Item item = new Item(cursor.getInt(idIndex), text, cursor.getInt(offsetIndex),
                        cursor.getInt(sizeIndex));
                if (lowerCase.startsWith(query)) {
                    words.add(item);
                    //the b
                    if (b && lowerCase.equals(query)) {
                        String definition = null;
                        if (dictParser != null) {
                            definition = dictParser.getWordDefinition(item.offset, item.size);
                        }
                        if (definition == null){
                            definition = "occur error while reading dictionary file";
                        }
                        Log.d(TAG, "-" + text);
                        showDefinition(text, definition);
                        b = false;
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

    /**
     *
     * @param cursor contains the definition of a word.
     *               will create a Dialog fragment to show word definition.
     */
    private void showWordDefinition(Cursor cursor) {
        if (cursor != null) {
            cursor.moveToFirst();
            int wordIndex = cursor.getColumnIndexOrThrow(DictSQLiteOpenHelper.KEY_WORD);
            int offsetIndex = cursor.getColumnIndexOrThrow(DictSQLiteOpenHelper.KEY_OFFSET);
            int sizeIndex = cursor.getColumnIndexOrThrow(DictSQLiteOpenHelper.KEY_SIZE);
            String word = cursor.getString(wordIndex);
            int offset = cursor.getInt(offsetIndex);
            int size = cursor.getInt(sizeIndex);
            String definition = null;
            if (dictParser != null) {
                definition = dictParser.getWordDefinition(offset, size);
            }
            if (definition == null){
                definition = "occur error while reading dictionary file";
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

    /**
     * TODO: show my dictionary order by time will need to load the whole dictionary
     */
    private void showMyDict() {
        MyDictAdapter myDictAdapter = new MyDictAdapter(this);
        wordView.setText("My Dict");
        resultListView.setAdapter(myDictAdapter);
    }

    /**
     * TODO: use WordAsyncQueryHandler to query words
     * TODO: there are two thread to interact with two SQLiteDatabase
     */

    public void setDictFile(File dictFile) {
        if (dictParser != null) {
            dictParser.closeFile();
        }
        dictParser = new DictParser(dictFile);
    }

    private class Item {
        int id;
        String text;
        int offset;
        int size;

        private Item(int id, String text, int offset, int size) {
            this.id = id;
            this.text = text;
            this.offset = offset;
            this.size = size;
        }

        @Override
        public String toString() {
            return text;
        }
    }

}
