package com.dict.hm.dictionary.paper;

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

import com.dict.hm.dictionary.PaperManagerActivity;
import com.dict.hm.dictionary.R;


/**
 * Created by hm on 15-1-22.
 */
public class PaperViewerFragment extends ListFragment {
    public static final String TAG = "PaperViewerFragment";
    File json;
    PaperJsonReader reader;
    PaperJsonAdapter adapter;
    boolean isArchiving;

    PaperManagerActivity activity = null;
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
        setEmptyText("No Word");
        setListShown(true);
        setListAdapter(null);
        Bundle bundle = getArguments();
        String jsonFilePath = bundle.getString(TAG);
        json = new File(jsonFilePath);
        reader = new PaperJsonReader(json);
        setListAdapter(null);
//        getListView().setChoiceMode(AbsListView.CHOICE_MODE_SINGLE);
//        drawable = getListView().getSelector();

        activity = (PaperManagerActivity) getActivity();
        isArchiving = false;
        activity.setTitle(getActivity().getResources().getString(R.string.title_paper_archive));
        actionBar = ((PaperManagerActivity) getActivity()).getSupportActionBar();

        archiveWordHandler = new ArchiveWordHandler(getActivity(), archiveWordListener);
        archiveWordHandler.startJsonRead(reader);
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
                manual();
                return true;
            case R.id.action_save:
                archiveManual(reader.getRemovedList());
                archiveCancel();
                return true;
//            case android.R.id.home:
//                if (isArchiving) {
//                    archiveCancel();
//                    jsonAdapter.cancelRemove();
//                }
//                return true;
        }
        return super.onOptionsItemSelected(item);
    }

    /**
     * record the click item's position
     */
    @Override
    public void onListItemClick(ListView l, View v, int position, long id) {
        if (isArchiving) {
            reader.remove(position);
            adapter.notifyDataSetChanged();
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

    ArchiveWordHandler.ArchiveWordListener archiveWordListener =
            new ArchiveWordHandler.ArchiveWordListener() {
                @Override
                public void onArchiveComplete(int what, ArrayList<JsonEntry> left) {
                    activity.dismissProgressDialog();
                    switch (what) {
                        case ArchiveWordHandler.ALL:
                            activity.setNotification("Archive " + reader.size() + " words");
                            activity.deletePaper(json);
                            break;
                        case ArchiveWordHandler.FILTER:
                            activity.setNotification("Archive " + (reader.size() - left.size()) + " words");

                            reader.setList(left);
                            archiveWordHandler.startJsonWrite(reader);
                            adapter.notifyDataSetChanged();
                            break;
                        case ArchiveWordHandler.MANUAL:
                            activity.setNotification("Archive " + reader.getRemovedList().size() + " words");

                            reader.clearRemovedList();
                            archiveWordHandler.startJsonWrite(reader);
                            adapter.notifyDataSetChanged();
                            break;
                        case ArchiveWordHandler.JSON_READ:
//                            adapter = new ArrayAdapter<>(getActivity(), R.layout.textview_item, reader.readAll());
                            adapter = new PaperJsonAdapter(getActivity(), reader.readAll());
                            setListAdapter(adapter);
                            break;
                        case ArchiveWordHandler.JSON_WRITE:
                            break;
                    }
                }
            };

    private void archiveALL(boolean filter) {
        ArrayList<JsonEntry> words = new ArrayList<>();
        words.addAll(reader.readAll());
        if (filter) {
            archiveWordHandler.startArchive(words, ArchiveWordHandler.FILTER);
            activity.initProgressDialog("Filter words...", 0);
        } else {
            archiveWordHandler.startArchive(words, ArchiveWordHandler.ALL);
            activity.initProgressDialog("Archive all words", 0);
        }
    }

    private void archiveManual(ArrayList<JsonEntry> words) {
        archiveWordHandler.startArchive(words, ArchiveWordHandler.MANUAL);
        activity.initProgressDialog("Archive selected words", 0);
    }

}

