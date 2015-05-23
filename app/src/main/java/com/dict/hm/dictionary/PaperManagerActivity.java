package com.dict.hm.dictionary;

import android.app.Activity;
import android.app.Fragment;
import android.content.Intent;
import android.net.Uri;
import android.os.Bundle;
import android.os.Handler;
import android.os.Message;
import android.util.Log;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.widget.AdapterView;
import android.widget.ArrayAdapter;

import com.dict.hm.dictionary.lib.ZBarActivity;
import com.dict.hm.dictionary.paper.PaperViewerFragment;
import com.dict.hm.dictionary.parse.PaperParser;

import java.io.File;
import java.util.ArrayList;

/**
 * Created by hm on 15-1-30.
 * manage paper
 */
public class PaperManagerActivity extends BaseManagerActivity {
    ArrayAdapter<String> adapter;
    String deleteItem;
    File paperDir;
    PaperViewerFragment paperViewerFragment = null;
    String url = null;
    int type;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        title = getString(R.string.title_paper);
        empty.setText("No Paper");
        setTitle(title);
        paperDir = new File(getExternalFilesDir(null), "paper");
        if (!paperDir.exists()) {
            paperDir.mkdirs();
        }

        listView.setOnItemClickListener(paperClickListener);
        listView.setOnItemLongClickListener(paperLongClickListener);
        adapter = new ArrayAdapter<>(this, R.layout.textview_item, getListViewData());
        listView.setAdapter(adapter);
    }

    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        getMenuInflater().inflate(R.menu.menu_paper_manager, menu);
        return true;
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        switch (item.getItemId()) {
            case R.id.action_clear:
                AlertDialogFragment dialogFragment = AlertDialogFragment
                        .newInstance(null, "Clear All Paper?", "Clear", "Cancel");
                dialogFragment.show(getFragmentManager(), null);
                action = CLEAR;
                return true;
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
            case android.R.id.home:
                if (paperViewerFragment != null && paperViewerFragment.isArchiving()) {
                    return false;
                }
                dismissFragment();
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
        if (resultCode == Activity.RESULT_OK) {
            ArrayList<String> list = data.getStringArrayListExtra(ZBarActivity.RESULT);
            if (list.size() == 1) {
                onEditDialogPositiveClick(list.get(0));
                Log.v("result", list.get(0));
            } else if (list.size() > 1) {
                //TODO: handle more than one scanning result.
                onEditDialogPositiveClick(list.get(0));
            }
        }
    }

    boolean existed = false;
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
            String name = uri.getLastPathSegment();
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
            case CLEAR:
                if (paperDir.isDirectory()) {
                    for (File file : paperDir.listFiles()) {
                        if (file.isFile()) {
                            file.delete();
                        }
                    }
                }
                adapter.clear();
                break;
            case DEL:
                File paperFile = new File(paperDir, deleteItem);
                if (paperFile.isFile()) {
                    paperFile.delete();
                }
                adapter.remove(deleteItem);
                break;
        }
        action = -1;
    }

    @Override
    protected void showFragment(Fragment fragment) {
        super.showFragment(fragment);
        Menu menu = toolbar.getMenu();
        menu.findItem(R.id.action_clear).setVisible(false);
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
        menu.findItem(R.id.action_clear).setVisible(true);
        menu.findItem(R.id.action_attachment).setVisible(true);
        menu.findItem(R.id.action_add_url).setVisible(true);
        menu.findItem(R.id.action_scan_qrcode).setVisible(true);
        return true;
    }

    private ArrayList<String> getListViewData() {
        ArrayList<String> strings = new ArrayList<>();
        if (paperDir.exists()) {
            for (String name : paperDir.list()) {
                strings.add(name);
            }
        }
        return strings;
    }

    /**
     *
     * @param out
     * variable type, selectedFile or url, need be prepared
     */
    private void addPaper(File out) {
        if (type == PaperParser.TXT) {
            new PaperParser(selectedFile, out, "UTF-8", PaperParser.TXT, handler).start();
        } else if (type == PaperParser.HTML){
            new PaperParser(selectedFile, out, "UTF-8", PaperParser.HTML, handler).start();
        } else if (type == PaperParser.URL) {
            new PaperParser(url, out, handler).start();
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

    private AdapterView.OnItemClickListener paperClickListener = new AdapterView.OnItemClickListener() {
        @Override
        public void onItemClick(AdapterView<?> parent, View view, int position, long id) {
            paperViewerFragment = new PaperViewerFragment();
            String json = paperDir.getAbsolutePath() + File.separator +
                    parent.getItemAtPosition(position).toString();
            Bundle bundle = new Bundle();
            bundle.putString(PaperViewerFragment.TAG, json);
            paperViewerFragment.setArguments(bundle);
            showFragment(paperViewerFragment);
        }
    };

    private AdapterView.OnItemLongClickListener paperLongClickListener = new AdapterView.OnItemLongClickListener() {
        @Override
        public boolean onItemLongClick(AdapterView<?> parent, View view, int position, long id) {
            AlertDialogFragment dialogFragment = AlertDialogFragment
                    .newInstance(null, "Remove Paper?", "Remove", "Cancel");
            dialogFragment.show(getFragmentManager(), null);
            deleteItem = (String) parent.getItemAtPosition(position);
            action = DEL;
            return true;
        }
    };

    Handler handler = new Handler(new Handler.Callback() {
        @Override
        public boolean handleMessage(Message msg) {
            switch (msg.what) {
                case ERR:    // something wrong happened
                    Log.d("msg.what", "-1");
                    return true;
                case DELETE: //delete paper
                    setNotification(msg.obj + " has been removed");
                    adapter.remove((String) msg.obj);
                    return true;
                case OK: // done parsing paper
                    dismissProgressDialog();
                    setNotification(msg.obj + " has been added");
                    adapter.remove((String) msg.obj);
                    adapter.add((String) msg.obj);
                    return true;
                default:
                    return false;
            }
        }
    });

}
