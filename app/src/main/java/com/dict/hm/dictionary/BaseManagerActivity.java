package com.dict.hm.dictionary;

import android.app.Fragment;
import android.app.FragmentTransaction;
import android.os.Bundle;
import android.support.v7.app.ActionBar;
import android.support.v7.app.AppCompatActivity;
import android.support.v7.widget.Toolbar;
import android.util.TypedValue;
import android.view.View;
import android.widget.ArrayAdapter;
import android.widget.ImageView;
import android.widget.ListView;
import android.widget.TextView;
import java.io.File;

/**
 * Created by hm on 15-3-18.
 */
public abstract class BaseManagerActivity extends AppCompatActivity
        implements FileListFragment.FileSelectedListener,
        AlertDialogFragment.ConfirmDialogListener,
        EditDialog.EditDialogListener {

    public static final int ERR = -1;
    public static final int OK = 0;
    public static final int PROCESSING = 1;
    public static final int DELETE = 2;

    public static final String FRAGMENT_TAG = "top";
    Toolbar toolbar;
    ListView listView;
    TextView empty;
    ImageView fab;
    FileListFragment fileListFragment;

//    ArrayAdapter<String> adapter;
    File selectedFile;
    String title;

    int action = -1;
    static final int ADD = 0;
    static final int DEL = 1;
    static final int CLEAR = 2;

    String notification = null;
    boolean isStop;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        setContentView(R.layout.activity_manager);
        toolbar = (Toolbar) findViewById(R.id.manager_toolbar);
        listView = (ListView) findViewById(R.id.manager_listView);
        empty = (TextView) findViewById(R.id.emptyView);
        fab = (ImageView) findViewById(R.id.manager_fab);
        setSupportActionBar(toolbar);
        listView.setEmptyView(empty);
    }

    @Override
    public void onBackPressed() {
        if (fileListFragment != null ) {
            if (fileListFragment.onBackPressed()) {
                return;
            } else {
                dismissFragment();
                return;
            }
        }
        if (dismissFragment()) {
            return;
        }
        super.onBackPressed();
    }

    @Override
    protected void onStop() {
        super.onStop();
        isStop = true;
    }

    @Override
    protected void onStart() {
        super.onStart();
        isStop = false;
        if (notification != null) {
           setNotification(notification);
        }
    }

    protected void showFragment(Fragment fragment) {
        getFragmentManager().beginTransaction()
                .add(R.id.manager_frame, fragment, FRAGMENT_TAG)
                .setTransition(FragmentTransaction.TRANSIT_FRAGMENT_OPEN)
                .commit();
        listView.setVisibility(View.INVISIBLE);
        empty.setVisibility(View.INVISIBLE);
        ActionBar actionBar = getSupportActionBar();
        if (actionBar != null) {
            getSupportActionBar().setHomeButtonEnabled(true);
            getSupportActionBar().setDisplayHomeAsUpEnabled(true);
        }
    }

    protected boolean dismissFragment() {
        Fragment fragment = getFragmentManager().findFragmentByTag(FRAGMENT_TAG);
        if (fragment == null) {
            return false;
        }
        getFragmentManager().beginTransaction()
                .remove(fragment)
                .setTransition(FragmentTransaction.TRANSIT_FRAGMENT_CLOSE)
                .commit();
        listView.setVisibility(View.VISIBLE);
        if (listView.getAdapter().isEmpty()) {
            empty.setVisibility(View.VISIBLE);
        }
        ActionBar actionBar = getSupportActionBar();
        if (actionBar != null) {
            getSupportActionBar().setHomeButtonEnabled(false);
            getSupportActionBar().setDisplayHomeAsUpEnabled(false);
        }
        toolbar.setTitle(title);
        fileListFragment = null;
        return true;
    }

    /**
     * after onStop() call this method (fragment's commit() method) will cause IllegalStateException.
     *
     * @param text
     */
    protected void setNotification(String text) {
        if (isStop) {
            notification = text;
            return;
        } else {
            notification = null;
        }
        NotificationDialog dialog = NotificationDialog.newInstance(text);
        dialog.show(getFragmentManager(), null);

        if (fab.getVisibility() == View.VISIBLE) {
            final float y = TypedValue.applyDimension(TypedValue.COMPLEX_UNIT_DIP, 72,
                    getResources().getDisplayMetrics());
            fab.animate().translationYBy(-y);
            dialog.setFab(fab, y);
        }
    }

    public void setProgressDialog(int progress) {
        if (dialog != null && dialog.isAdded()) {
            dialog.setProgressBar(progress);
        }
    }

    public void dismissProgressDialog() {
        if (dialog != null) {
            dialog.dismissAllowingStateLoss();
        }
    }

    public void initProgressDialog(String msg, int max) {
        dialog = ProgressDialog.newInstance(msg, max);
        dialog.show(getFragmentManager(), null);
    }
    ProgressDialog dialog;

}
