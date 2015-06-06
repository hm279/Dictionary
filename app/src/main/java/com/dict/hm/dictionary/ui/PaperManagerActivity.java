package com.dict.hm.dictionary.ui;

import android.app.Fragment;
import android.content.Intent;
import android.content.SharedPreferences;
import android.net.Uri;
import android.os.Bundle;
import android.preference.PreferenceManager;
import android.util.Log;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.widget.AdapterView;
import android.widget.ArrayAdapter;
import android.widget.Toast;

import com.dict.hm.dictionary.R;
import com.dict.hm.dictionary.dict.DictManager;
import com.dict.hm.dictionary.lib.ZBarActivity;
import com.dict.hm.dictionary.paper.JsonEntry;
import com.dict.hm.dictionary.paper.PaperErrorCode;
import com.dict.hm.dictionary.paper.PaperParser;
import com.dict.hm.dictionary.paper.PaperWorkerHandler;
import com.dict.hm.dictionary.ui.dialog.AlertDialogFragment;
import com.dict.hm.dictionary.ui.dialog.EditDialog;

import java.io.File;
import java.util.ArrayList;

/**
 * Created by hm on 15-1-30.
 * manage paper
 */
public class PaperManagerActivity extends BaseManagerActivity
        implements PaperWorkerHandler.PaperWorkerListener{
    ArrayAdapter<String> adapter;
    File paperDir;
    File openingPaper;
    PaperArchiveFragment paperArchiveFragment = null;
    String url = null;
    int type;
    boolean existed = false;
    boolean autoFilter = false;

    DictManager manager;
    PaperWorkerHandler paperWorkerHandler = null;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        fab.setVisibility(View.GONE);

//        title = getString(R.string.title_paper);
        empty.setText("No Paper");
//        setTitle(title);
        manager = DictManager.getInstance(this);
        paperDir = manager.getPaperDir();

        listView.setOnItemClickListener(paperClickListener);
        listView.setOnItemLongClickListener(paperLongClickListener);
        adapter = new ArrayAdapter<>(this, R.layout.textview_item, manager.getPapers());
        listView.setAdapter(adapter);

        SharedPreferences preferences = PreferenceManager.getDefaultSharedPreferences(this);
        autoFilter = preferences.getBoolean(getString(R.string.key_auto_filter), false);

        paperWorkerHandler = new PaperWorkerHandler(this, this);
    }

    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        getMenuInflater().inflate(R.menu.menu_paper_manager, menu);
        return true;
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        switch (item.getItemId()) {
            case R.id.action_attachment:
                fileListFragment = new FileListFragment();
                showFragment(fileListFragment);
                return true;
            case R.id.action_add_url:
                EditDialog editDialog = EditDialog.newInstance("Input Url", "http://");
                editDialog.show(getFragmentManager(), null);
                return true;
            case R.id.action_scan_qrcode:
                Intent intent = new Intent(this, ZBarActivity.class);
                startActivityForResult(intent, 0);
                return true;
        }
        return super.onOptionsItemSelected(item);
    }

    /**
     * Get the QR Code's scan result
     */
    @Override
    protected void onActivityResult(int requestCode, int resultCode, Intent data) {
        super.onActivityResult(requestCode, resultCode, data);
        if (resultCode == RESULT_OK) {
            ArrayList<String> list = data.getStringArrayListExtra(ZBarActivity.RESULT);
            if (list.size() == 1) {
                EditDialog editDialog = EditDialog.newInstance("Scan result", list.get(0), "Add");
                editDialog.show(getFragmentManager(), null);
//                onEditDialogPositiveClick(list.get(0));
//                Log.v("result", list.get(0));
            } else if (list.size() > 1) {
                //TODO: handle more than one scanning result.
                EditDialog editDialog = EditDialog.newInstance("Scan result", list.get(0), "Add");
                editDialog.show(getFragmentManager(), null);
//                onEditDialogPositiveClick(list.get(0));
            }
        }
    }

    /**
     *
     * @param url
     * Get the Input URL
     */
    @Override
    public void onEditDialogPositiveClick(String url) {
        if (existed) {
            File paper = new File(paperDir, url);
            addPaper(paper);
            existed = false;
            return;
        }
        if (url != null) {
            Uri uri = Uri.parse(url.trim());
//            String name = uri.getLastPathSegment();
            String name = uri.getPath();
            if (name == null) {
                return;
            }
            this.url = url;
            type = PaperParser.URL;
            File paper = new File(paperDir, name);
            if (paper.exists()) {
                EditDialog editDialog = EditDialog.newInstance("Paper Existed", name, "Rename");
                editDialog.show(getFragmentManager(), null);
                existed = true;
            } else {
                addPaper(paper);
            }
        }
    }

    /**
     *
     * @param file The selected file.
     *
     */
    @Override
    public void onFileSelectedListener(File file) {
        String name = file.getName();
        type = checkSupportFileType(name);
        if (type >= 0) {
            File paper = new File(paperDir, name);
            AlertDialogFragment dialogFragment;
            if (paper.exists()) {
                dialogFragment = AlertDialogFragment.newInstance("Overwrite Paper?",
                        name + " paper has been added", "Overwrite", "Cancel");
            } else {
                dialogFragment = AlertDialogFragment
                        .newInstance(null, "Add Paper", "Add", "Cancel");
            }
            dialogFragment.show(getFragmentManager(), null);
            action = ADD;
            selectedFile = file;
        }
    }

    @Override
    public void onDialogPositiveClick() {
        switch (action) {
            case ADD:
                if (fileListFragment != null) {
                    //add paper from file
                    File paper = new File(paperDir, selectedFile.getName());
                    addPaper(paper);
                    dismissFragment();
                }
                Log.d("add paper", "start...");
                break;
            case DEL:
                manager.removePaper(openingPaper);
                adapter.notifyDataSetChanged();
                break;
        }
        action = -1;
    }

    @Override
    protected void showFragment(Fragment fragment) {
        super.showFragment(fragment);
        Menu menu = toolbar.getMenu();
        menu.findItem(R.id.action_attachment).setVisible(false);
        menu.findItem(R.id.action_add_url).setVisible(false);
        menu.findItem(R.id.action_scan_qrcode).setVisible(false);
    }

    @Override
    protected boolean dismissFragment() {
        if (!super.dismissFragment()) {
            return false;
        }
        Menu menu = toolbar.getMenu();
        menu.findItem(R.id.action_attachment).setVisible(true);
        menu.findItem(R.id.action_add_url).setVisible(true);
        menu.findItem(R.id.action_scan_qrcode).setVisible(true);
        return true;
    }

    /**
     *
     * @param out
     * variable type, selectedFile or url, need be prepared
     */
    private void addPaper(File out) {
        manager.addPaper(out);
        adapter.notifyDataSetChanged();

        if (type == PaperParser.TXT) {
            paperWorkerHandler.startParse(selectedFile, out, "UTF-8", type, autoFilter);
        } else if (type == PaperParser.HTML){
            paperWorkerHandler.startParse(selectedFile, out, "UTF-8", type, autoFilter);
        } else if (type == PaperParser.URL) {
            paperWorkerHandler.startParse(url, out, type, autoFilter);
        } else {
            return;
        }
        initProgressDialog("Parsing...", 0);
    }

    private int checkSupportFileType(String mFileName) {
        if (mFileName.endsWith(".txt") || mFileName.endsWith(".TXT")) {
            return PaperParser.TXT;
        }
        if (mFileName.endsWith(".html") || mFileName.endsWith(".HTML")) {
            return PaperParser.HTML;
        }
        return -1;
    }

    public PaperWorkerHandler getPaperWorkerHandler() {
        return paperWorkerHandler;
    }

    private AdapterView.OnItemClickListener paperClickListener = new AdapterView.OnItemClickListener() {
        @Override
        public void onItemClick(AdapterView<?> parent, View view, int position, long id) {
            listView.setItemChecked(position, true);
            String name = (String) parent.getItemAtPosition(position);
            if (name == null) {
                return;
            }
            openingPaper = new File(paperDir, name);
            if (!openingPaper.isFile()) {
                manager.removePaper(openingPaper);
                adapter.notifyDataSetChanged();
                Toast.makeText(getApplicationContext(), "paper not exist", Toast.LENGTH_SHORT).show();
                return;
            }
            Bundle bundle = new Bundle();
            bundle.putString(PaperArchiveFragment.JSON_PATH, openingPaper.getAbsolutePath());

            paperArchiveFragment = new PaperArchiveFragment();
            paperArchiveFragment.setArguments(bundle);
            showFragment(paperArchiveFragment);
        }
    };

    private AdapterView.OnItemLongClickListener paperLongClickListener = new AdapterView.OnItemLongClickListener() {
        @Override
        public boolean onItemLongClick(AdapterView<?> parent, View view, int position, long id) {
            AlertDialogFragment dialogFragment = AlertDialogFragment
                    .newInstance(null, "Remove Paper?", "Remove", "Cancel");
            dialogFragment.show(getFragmentManager(), null);
            String name = (String) parent.getItemAtPosition(position);
            if (name == null) {
                return true;
            }
            openingPaper = new File(paperDir, name);
            action = DEL;
            return true;
        }
    };

    @Override
    public void onArchiveComplete(int type, int count, ArrayList<JsonEntry> arrayList) {
        setNotification("Archive " + count + " words");
        dismissProgressDialog();
        switch (type) {
            case PaperWorkerHandler.ALL:
                dismissFragment();
                manager.removePaper(openingPaper);
                adapter.notifyDataSetChanged();
                Log.d("all", "remove");
                break;
            case PaperWorkerHandler.FILTER:
                paperArchiveFragment.onFilterComplete(arrayList);
                break;
        }
    }

    @Override
    public void onJsonReadComplete(ArrayList<JsonEntry> arrayList) {
        paperArchiveFragment.setAdapter(arrayList);
    }

    @Override
    public void onJsonWriteComplete(ArrayList<JsonEntry> arrayList) {
        if (arrayList == null) {
            //means json write failed
            Toast.makeText(getApplicationContext(), "failed to update paper", Toast.LENGTH_SHORT).show();
        }
    }

    @Override
    public void onPaperParseComplete(File json, int error) {
        dismissProgressDialog();
        if (error == 0) {
            setNotification(json.getName() + " has been added");
        } else {
            //failed to parse. if json file exist, means it existed before parse.
            if (!json.exists()) {
                manager.removePaper(json);
                adapter.notifyDataSetChanged();
            }
            if (error == PaperErrorCode.ERR_NET) {
                setNotification("Network error");
            } else {
                setNotification("failed to parse");
            }
        }
    }

}
