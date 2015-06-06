package com.dict.hm.dictionary.ui;

import android.app.ListFragment;
import android.os.Bundle;
import android.support.v7.app.ActionBar;
import android.view.Menu;
import android.view.MenuInflater;
import android.view.MenuItem;
import android.view.View;
import android.widget.ListView;

import java.io.File;
import java.util.ArrayList;

import com.dict.hm.dictionary.paper.JsonEntry;
import com.dict.hm.dictionary.paper.PaperWorkerHandler;
import com.dict.hm.dictionary.ui.PaperManagerActivity;
import com.dict.hm.dictionary.R;
import com.dict.hm.dictionary.ui.adapter.PaperJsonAdapter;


/**
 * Created by hm on 15-1-22.
 */
public class PaperArchiveFragment extends ListFragment {
    public static final String JSON_PATH = "path";

    File jsonFile;
    boolean isManual;

    PaperJsonAdapter adapter = null;
    PaperWorkerHandler paperWorkerHandler = null;
    PaperManagerActivity activity = null;
    Menu menu;
    ActionBar actionBar;

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setHasOptionsMenu(true);
    }

    @Override
    public void onActivityCreated(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setListShown(true);
        setListAdapter(null);
//        getListView().setChoiceMode(AbsListView.CHOICE_MODE_SINGLE);
//        drawable = getListView().getSelector();

        isManual = false;
        activity = (PaperManagerActivity) getActivity();
//        activity.setTitle(getActivity().getResources().getString(R.string.title_paper_archive));
        actionBar = activity.getSupportActionBar();

        Bundle bundle = getArguments();
        String jsonPath = bundle.getString(JSON_PATH);
        if (jsonPath == null) {
            setEmptyText("missing paper");
        } else {
            jsonFile = new File(jsonPath);
            paperWorkerHandler = activity.getPaperWorkerHandler();
            paperWorkerHandler.startJsonRead(jsonFile);
        }
    }

    @Override
    public void onDestroy() {
        super.onDestroy();
        actionBar.setHomeAsUpIndicator(null);
    }

    @Override
    public void onCreateOptionsMenu(Menu menu, MenuInflater inflater) {
        inflater.inflate(R.menu.menu_archive, menu);
        menu.findItem(R.id.action_save).setVisible(false);
        menu.findItem(R.id.action_attachment).setVisible(false);
        menu.findItem(R.id.action_add_url).setVisible(false);
        menu.findItem(R.id.action_scan_qrcode).setVisible(false);
        this.menu = menu;
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        if (adapter == null) {
            return false;
        }
        int id = item.getItemId();
        switch (id) {
            case R.id.action_filter:
                /**
                 * this will filter the word, that had been existed in UserDict.
                 * And left the new words.
                 */
                archiveALL(true);
                return true;
            case R.id.action_archive:
                /**
                 * this will archive all the words into MyDict.
                 */
                archiveALL(false);
                return true;
            case R.id.action_manual:
                startManual();
                return true;
            case R.id.action_save:
                archiveManual();
                finishManual();
                return true;
        }
        return super.onOptionsItemSelected(item);
    }

    /**
     * record the click item's position
     */
    @Override
    public void onListItemClick(ListView l, View v, int position, long id) {
        if (isManual) {
            adapter.removeItem(position);
        }
    }

    /**
     * start to manually select words
     */
    private void startManual() {
        /**
         * show the archiving interface
         */
        menu.findItem(R.id.action_archive).setVisible(false);
        menu.findItem(R.id.action_filter).setVisible(false);
        menu.findItem(R.id.action_manual).setVisible(false);
        menu.findItem(R.id.action_save).setVisible(true);
        isManual = true;
        actionBar.setHomeAsUpIndicator(R.drawable.ic_close_white_18dp);
//            getListView().setChoiceMode(AbsListView.CHOICE_MODE_MULTIPLE);
//            getListView().setSelector(android.R.color.transparent);
        adapter.clearRemovedList();
    }

    private void finishManual() {
        menu.findItem(R.id.action_archive).setVisible(true);
        menu.findItem(R.id.action_filter).setVisible(true);
        menu.findItem(R.id.action_manual).setVisible(true);
        menu.findItem(R.id.action_save).setVisible(false);

        isManual = false;
        actionBar.setHomeAsUpIndicator(null);
//        getListView().setChoiceMode(AbsListView.CHOICE_MODE_SINGLE);
//        getListView().setSelector(drawable);
    }

    private void archiveALL(boolean filter) {
        ArrayList<JsonEntry> words = adapter.getList();
        if (filter) {
            paperWorkerHandler.startArchive(words, jsonFile, PaperWorkerHandler.FILTER);
            activity.initProgressDialog("Filter words...", 0);
        } else {
            paperWorkerHandler.startArchive(words, jsonFile, PaperWorkerHandler.ALL);
            activity.initProgressDialog("Archive all words", 0);
        }
    }

    private void archiveManual() {
        ArrayList<JsonEntry> words = adapter.getRemovedList();
        paperWorkerHandler.startArchive(words, jsonFile, PaperWorkerHandler.MANUAL);
        paperWorkerHandler.startJsonWrite(jsonFile, adapter.getList());
        activity.initProgressDialog("Archive selected words", 0);
    }

    public void setAdapter(ArrayList<JsonEntry> arrayList) {
        if (arrayList != null) {
            adapter = new PaperJsonAdapter(getActivity(), arrayList);
            setListAdapter(adapter);
        }
        setEmptyText("No Word");
    }

    public void onFilterComplete(ArrayList<JsonEntry> left) {
        adapter.setList(left);
    }

}

