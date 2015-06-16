package com.dict.hm.dictionary.ui;

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
import android.preference.PreferenceManager;
import android.provider.BaseColumns;
import android.support.v4.view.GestureDetectorCompat;
import android.support.v4.view.MenuItemCompat;
import android.support.v4.widget.DrawerLayout;
import android.support.v7.app.ActionBar;
import android.support.v7.app.ActionBarDrawerToggle;
import android.support.v7.app.AppCompatActivity;
import android.support.v7.widget.SearchView;
import android.support.v7.widget.Toolbar;
import android.util.Log;
import android.util.TypedValue;
import android.view.GestureDetector;
import android.view.Menu;
import android.view.MenuItem;
import android.view.MotionEvent;
import android.view.View;
import android.view.inputmethod.InputMethodManager;
import android.widget.AbsListView;
import android.widget.AdapterView;
import android.widget.ArrayAdapter;
import android.widget.AutoCompleteTextView;
import android.widget.ListView;
import android.widget.TextView;
import android.widget.Toast;

import com.dict.hm.dictionary.BuildConfig;
import com.dict.hm.dictionary.R;
import com.dict.hm.dictionary.dict.DictContentProvider;
import com.dict.hm.dictionary.dict.DictFormat;
import com.dict.hm.dictionary.dict.DictItem;
import com.dict.hm.dictionary.dict.DictManager;
import com.dict.hm.dictionary.dict.DictSQLiteDefine;
import com.dict.hm.dictionary.dict.UserDictSQLiteHelper;
import com.dict.hm.dictionary.lib.ScrimInsetsFrameLayout;
import com.dict.hm.dictionary.dict.parse.DictParser;
import com.dict.hm.dictionary.ui.adapter.NavigationDrawerAdapter;
import com.dict.hm.dictionary.ui.dialog.AboutDialog;
import com.dict.hm.dictionary.ui.dialog.DefinitionDialog;
import com.dict.hm.dictionary.ui.dialog.SwitchDictDialog;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.util.ArrayList;


public class MainActivity extends AppCompatActivity
        implements SwitchDictDialog.SwitchDictDialogListener{

    private String TAG = "MainActivity";

    private Toolbar toolbar;
    private DrawerLayout drawerLayout;
    private ScrimInsetsFrameLayout leftDrawerLayout;
    private ActionBarDrawerToggle drawerToggle;
    private NavigationDrawerAdapter adapter;

    private SearchView searchView;
    private TextView wordView;
    private ListView resultListView;
    private MenuItem searchItem;
    private boolean canDismiss = false;

    private final int action_query_word = 0;
    private final int action_show_paper = 1;
    private int listAction;

    private DictManager manager = null;

    GestureDetectorCompat detector;

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
//        if (manager.isInited()) {
//            manager.switchActiveDict(manager.getActiveDict());
//        }

        detector = new GestureDetectorCompat(this, new MyGestureListener());

        PreferenceManager.setDefaultValues(this, R.xml.preferences, false);

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
        Log.d(TAG, "onDestroy");
        //activity destroy,but process is still exist.
//        if (manager != null) {
//            manager.closeDictParser();
//        }
        super.onDestroy();
    }

    @Override
    public void onBackPressed() {
        if (drawerLayout.isDrawerOpen(leftDrawerLayout)) {
            drawerLayout.closeDrawer(leftDrawerLayout);
            return;
        }
        if (canDismiss) {
            dismissFragment();
            return;
        }
        super.onBackPressed();
    }

    @Override
    public boolean onTouchEvent(MotionEvent event) {
        //TODO: gesture detection not work
        detector.onTouchEvent(event);
        return super.onTouchEvent(event);
    }

    class MyGestureListener extends GestureDetector.SimpleOnGestureListener {
        @Override
        public boolean onDown(MotionEvent e) {
            return true;
        }

        @Override
        public boolean onFling(MotionEvent e1, MotionEvent e2, float velocityX, float velocityY) {
            if (super.onFling(e1, e2, velocityX, velocityY)) {
                return true;
            }
            searchItem.expandActionView();
            searchView.requestFocus();
            InputMethodManager imm = (InputMethodManager) getSystemService(INPUT_METHOD_SERVICE);
            imm.toggleSoftInput(0, InputMethodManager.HIDE_NOT_ALWAYS);
            return true;
        }
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
        if (id == NavigationDrawerAdapter.MANAGE_DICT) {
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
            parseDefinition(word, uri);
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
//        drawerLayout.setStatusBarBackgroundColor(getResources().getColor(
//                R.color.material_teal_700));
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.LOLLIPOP) {
            getWindow().setStatusBarColor(0x0);
        }

        leftDrawerLayout = (ScrimInsetsFrameLayout) findViewById(R.id.left_drawerLayout);
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
                ((ListView) parent).setItemChecked(position, true);
                if (canDismiss) {
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
                        startManagerActivity(NavigationDrawerAdapter.MANAGE_DICT);
                        break;
                    case NavigationDrawerAdapter.MANAGE_PAPER:
                        startManagerActivity(NavigationDrawerAdapter.MANAGE_PAPER);
                        break;
                    case NavigationDrawerAdapter.SETTINGS:
                        SettingsFragment fragment = new SettingsFragment();
                        showFragment(fragment);
                        break;
                    case NavigationDrawerAdapter.ABOUT:
                        AboutDialog dialog = new AboutDialog();
                        dialog.show(getFragmentManager(), null);
                        break;
                }
            }
        });
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
        manager.switchActiveDict(which);
        wordView.setText(manager.getDictFormat(which).getName());
        resultListView.setAdapter(null);
    }

    /** -----------------------------------------------------------------------------------------*/

    AdapterView.OnItemClickListener resultListViewListener = new AdapterView.OnItemClickListener(){
        @Override
        public void onItemClick(AdapterView<?> parent, View view, int position, long id) {
            //position 0 is the headView.
            //the two method is right, will get the right item.
//            Object object = parent.getAdapter().getItem(position);
            Object object = parent.getItemAtPosition(position);
            if (listAction == action_query_word) {
                if (object instanceof DictItem) {
                    DictItem item = (DictItem) object;
                    Uri uri = Uri.parse(DictContentProvider.CONTENT_URI + "/" + item.getId());
                    parseDefinition(item.toString(), uri);
                }
            } else if (listAction == action_show_paper){
                if (object instanceof String) {
                    showPaper((String) object);
                }
            }
        }
    };

    /** ----------------------------Search and Query---------------------------------------------*/
    /**
     * TODO: should use WordAsyncQueryHandler to query words?
     */

    /**
     * have query action
     */
    public void getQueryResults(String word) {
        String countString;
        boolean noWordEqual = true;
        ArrayAdapter<DictItem> words = null;
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
            ArrayList<DictItem> list = new ArrayList<>();
            int idIndex = cursor.getColumnIndex(BaseColumns._ID);
            int wordIndex = cursor.getColumnIndexOrThrow(DictSQLiteDefine.COLUMN_KEY_WORD);
            cursor.moveToFirst();
            word = word.toLowerCase();
            String lowerCase;
            do {
                String text = cursor.getString(wordIndex);
                lowerCase = text.toLowerCase();
                DictItem item = new DictItem(cursor.getLong(idIndex), text);
                if (lowerCase.startsWith(word)) {
                    words.add(item);
                    if (noWordEqual && lowerCase.equals(word)) {
                        //parse and show definition
                        Uri uri = Uri.parse(DictContentProvider.CONTENT_URI + "/" + item.getId());
                        parseDefinition(text, uri);
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
        if (canDismiss) {
            dismissFragment();
        }
        wordView.setText(countString);
        resultListView.setAdapter(words);
        listAction = action_query_word;
    }

    /**
     *
     * @param word word to query.
     * @param uri the uri to get word's index information.
     *
     * will create a Dialog fragment to show word definition.
     * TODO: different dict format should have different parseDefinition() function;
     */
    private void parseDefinition(String word, Uri uri) {
        Cursor cursor = getContentResolver().query(uri, null, null, null, null);
        if (cursor != null) {
            cursor.moveToFirst();
            int offsetIndex = cursor.getColumnIndex(DictSQLiteDefine.COLUMN_OFFSET);
            int sizeIndex = cursor.getColumnIndex(DictSQLiteDefine.COLUMN_SIZE);
            int offset = cursor.getInt(offsetIndex);
            int size = cursor.getInt(sizeIndex);
            String definition = null;
            DictParser dictParser = manager.getDictParser();
            if (dictParser != null) {
                definition = dictParser.getWordDefinition(offset, size);
            }
            if (definition == null){
                //TODO: define the string in strings.xml
                definition = "occur error while reading text from .dict file";
            }
            DefinitionDialog dialog = DefinitionDialog.getDefinitionDialog(word, definition);
            dialog.show(getFragmentManager(), null);
            cursor.close();
        }
    }

    private void expandSearchView() {
        searchItem.expandActionView();
        searchView.requestFocus();
        InputMethodManager imm = (InputMethodManager) getSystemService(INPUT_METHOD_SERVICE);
        imm.toggleSoftInput(0, InputMethodManager.HIDE_NOT_ALWAYS);
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
        bundle.putString(PaperViewerFragment.PAPER_NAME, fileName);
        PaperViewerFragment fragment = new PaperViewerFragment();
        fragment.setArguments(bundle);
        showFragment(fragment);
    }

    /** ----------------------------Fragment manager---------------------------------------------*/

    private void showFragment(Fragment fragment) {
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
        UserDictFragment fragment = new UserDictFragment();
        showFragment(fragment);
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
        File src = getDatabasePath(UserDictSQLiteHelper.getInstance(this).getDatabaseName());
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
