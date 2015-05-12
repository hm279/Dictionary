package com.dict.hm.dictionary.paper;

import android.app.ListFragment;
import android.os.Bundle;
import android.os.Handler;
import android.os.Message;
import android.support.v7.app.ActionBar;
import android.util.JsonReader;
import android.util.Log;
import android.view.Menu;
import android.view.MenuInflater;
import android.view.MenuItem;
import android.view.View;
import android.widget.ListView;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.FileReader;
import java.io.IOException;
import java.util.HashMap;

import com.dict.hm.dictionary.PaperManagerActivity;
import com.dict.hm.dictionary.dict.DictManager;
import com.dict.hm.dictionary.dict.MyDictSQLiteOpenHelper;
import com.dict.hm.dictionary.R;
import com.dict.hm.dictionary.parse.DictParser;



/**
 * Created by hm on 15-1-22.
 */
public class PaperViewerFragment extends ListFragment {
    public static final String TAG = "PaperViewerFragment";
    PaperJsonReader reader;
    PaperJsonAdapter jsonAdapter = null;
    Boolean isArchiving;

    Menu menu;
    ActionBar actionBar;
    ArchiveWordHandler archiveWordHandler = null;

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setHasOptionsMenu(true);
    }

    @Override
    public void onActivityCreated(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setEmptyText("No Word To Show");
        setListShown(true);
        setListAdapter(null);
        Bundle bundle = getArguments();
        String jsonFilePath = bundle.getString(TAG);
        File json = new File(jsonFilePath);
        reader = new PaperJsonReader(json);
        jsonAdapter = new PaperJsonAdapter(getActivity(), reader);
        setListAdapter(jsonAdapter);
//        getListView().setChoiceMode(AbsListView.CHOICE_MODE_SINGLE);
//        drawable = getListView().getSelector();

        isArchiving = false;
        getActivity().setTitle(getActivity().getResources().getString(R.string.title_paper_archive));
        actionBar = ((PaperManagerActivity) getActivity()).getSupportActionBar();
    }

    @Override
    public void onCreateOptionsMenu(Menu menu, MenuInflater inflater) {
        inflater.inflate(R.menu.menu_archive, menu);
        menu.findItem(R.id.action_save).setVisible(false);
        menu.findItem(R.id.action_clear).setVisible(false);
        menu.findItem(R.id.action_attachment).setVisible(false);
        menu.findItem(R.id.action_add_url).setVisible(false);
        menu.findItem(R.id.action_scan_qrcode).setVisible(false);
        this.menu = menu;
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        int id = item.getItemId();
        switch (id) {
            case R.id.action_filter:
                /**
                 * this will filter the word, that had been existed in MyDict.
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
                manual();
                return true;
            case R.id.action_save:
                archiveManual(reader.getRemovedList());
                archiveCancel();
                return true;
            case android.R.id.home:
                if (isArchiving) {
                    archiveCancel();
                    jsonAdapter.cancelRemove();
                }
                return true;
            case R.id.action_test:
                copy();
                return true;
        }
        return super.onOptionsItemSelected(item);
    }

    /**
     * record the click item's position
     */
    @Override
    public void onListItemClick(ListView l, View v, int position, long id) {
        if (isArchiving) {
            jsonAdapter.remove(position);
        }
    }

    @Override
    public void onDestroyView() {
        super.onDestroyView();
        if (reader != null) {
            reader.close();
        }
    }

    /**
     * start to manually select words
     */
    private void manual() {
        /**
         * show the archiving interface
         */
        menu.findItem(R.id.action_archive).setVisible(false);
        menu.findItem(R.id.action_filter).setVisible(false);
        menu.findItem(R.id.action_manual).setVisible(false);
        menu.findItem(R.id.action_save).setVisible(true);
        isArchiving = true;
        actionBar.setHomeAsUpIndicator(R.drawable.ic_close_white_18dp);
//            getListView().setChoiceMode(AbsListView.CHOICE_MODE_MULTIPLE);
//            getListView().setSelector(android.R.color.transparent);
    }

    private void archiveCancel() {
        menu.findItem(R.id.action_archive).setVisible(true);
        menu.findItem(R.id.action_filter).setVisible(true);
        menu.findItem(R.id.action_manual).setVisible(true);
        menu.findItem(R.id.action_save).setVisible(false);

        isArchiving = false;
        actionBar.setHomeAsUpIndicator(null);
//        getListView().setChoiceMode(AbsListView.CHOICE_MODE_SINGLE);
//        getListView().setSelector(drawable);
    }

    public Boolean isArchiving() {
        return isArchiving;
    }

    ArchiveWordHandler.ArchiveWordListener archiveWordListener =
            new ArchiveWordHandler.ArchiveWordListener() {
                @Override
                public void onArchiveComplete(int what, HashMap<String, Integer> left) {
                    Log.d("onArchiveComplete", what + "-");
                }
            };

    private void archiveALL(boolean filter) {
        if (archiveWordHandler == null) {
            archiveWordHandler = new ArchiveWordHandler(getActivity(), archiveWordListener);
        }
        HashMap<String, Integer> words = new HashMap<>();
        int position = 0;
        String word;
        while ((word = reader.getJsonKey(position)) != null) {
            words.put(word, reader.getJsonValue(position));
            position++;
        }

        if (filter) {
            archiveWordHandler.startArchive(words, ArchiveWordHandler.FILTER);
        } else {
            archiveWordHandler.startArchive(words, ArchiveWordHandler.ALL);
        }
    }

    private void archiveManual(HashMap<String, Integer> words) {
        if (archiveWordHandler == null) {
            archiveWordHandler = new ArchiveWordHandler(getActivity(), archiveWordListener);
        }
        archiveWordHandler.startArchive(words, ArchiveWordHandler.MANUAL);
    }

    /**
     * for test
     */
    private void copy() {
        File src = getActivity().getDatabasePath(MyDictSQLiteOpenHelper.getInstance(getActivity()).getDatabaseName());
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

