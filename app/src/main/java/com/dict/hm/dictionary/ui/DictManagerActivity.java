package com.dict.hm.dictionary.ui;

import android.content.Intent;
import android.graphics.Bitmap;
import android.graphics.Canvas;
import android.graphics.drawable.BitmapDrawable;
import android.graphics.drawable.Drawable;
import android.net.Uri;
import android.os.Build;
import android.os.Bundle;
import android.os.Environment;
import android.os.Handler;
import android.os.Message;
import android.util.Log;
import android.view.MenuItem;
import android.view.View;
import android.widget.AdapterView;
import android.widget.ArrayAdapter;
import android.widget.Toast;

import com.dict.hm.dictionary.R;
import com.dict.hm.dictionary.dict.DictFormat;
import com.dict.hm.dictionary.dict.DictManager;
import com.dict.hm.dictionary.async.HttpDownload;
import com.dict.hm.dictionary.lib.ZBarActivity;
import com.dict.hm.dictionary.dict.parse.IfoFormat;
import com.dict.hm.dictionary.ui.dialog.AlertDialogFragment;
import com.dict.hm.dictionary.ui.dialog.EditDialog;

import java.io.File;
import java.util.ArrayList;

/**
 * Created by hm on 15-3-18.
 */
public class DictManagerActivity extends BaseManagerActivity {

    public static final int ERR = -1;
    public static final int OK = 0;
    public static final int PROCESSING = 1;
    public static final int DELETE = 2;

    public static final int DECOMPRESS = 10;
    public static final int DECOMPRESS_ERR = -10;

    private ArrayAdapter<DictFormat> adapter;
    private int deleteItemPosition;
    private DictManager manager;
    private int wordCount;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        //manage dictionary
//        title = getString(R.string.title_dict);
        empty.setText("No Dictionary");
//        setTitle(title);
        listView.setOnItemClickListener(dictClickListener);
        fab.setVisibility(View.VISIBLE);
        fab.setOnClickListener(fabClickListener);

        manager = DictManager.getInstance(this);
        adapter = new ArrayAdapter<>(this, R.layout.textview_item, manager.getDictFormats());
        listView.setAdapter(adapter);
    }

//    @Override
//    public boolean onCreateOptionsMenu(Menu menu) {
//        getMenuInflater().inflate(R.menu.menu_dict_manager, menu);
//        return true;
//    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        if (item.getItemId() == R.id.action_add_url) {
            EditDialog editDialog = EditDialog.newInstance("Input Url", "http://");
            editDialog.show(getFragmentManager(), null);
            return true;
        } else if (item.getItemId() == R.id.action_scan_qrcode) {
            Intent intent = new Intent(this, ZBarActivity.class);
            startActivityForResult(intent, 0);
            return true;
        }
        return super.onOptionsItemSelected(item);
    }

    @Override
    protected void onActivityResult(int requestCode, int resultCode, Intent data) {
        super.onActivityResult(requestCode, resultCode, data);
        if (resultCode == RESULT_OK) {
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

    @Override
    public void onEditDialogPositiveClick(String url) {
        String filePath = Environment.getExternalStorageDirectory().getPath();
        Uri uri = Uri.parse(url);
        new HttpDownload(this).execute(url, filePath + "/" + uri.getLastPathSegment());
    }

    @Override
    public void onDialogPositiveClick() {
        switch (action) {
            case ADD:
                if (fileListFragment != null) {
                    DictFormat format = manager.addDictionary(selectedFile, handler);
                    if (format != null) {
                        dismissFragment();
                        initProgressDialog("Loading...", wordCount);
                    }
                }
                break;
            case DEL:
                manager.removeDictionary(deleteItemPosition, handler);
                break;
        }
        action = -1;
        adapter.notifyDataSetChanged();
    }

    @Override
    public void onFileSelectedListener(File file) {
        String name = file.getName();
        if (name.endsWith(".ifo")) {
            IfoFormat format = new IfoFormat(file);
            String msg = "Book Name: " + format.getBookName() + '\n'
                    + "Version: " + format.getVersion() + '\n'
                    + "Word Count: " + format.getWordCount();
            AlertDialogFragment dialogFragment = AlertDialogFragment
                    .newInstance("Add Dictionary", msg, "Add", "Cancel");
            dialogFragment.show(getFragmentManager(), null);
            action = ADD;
            selectedFile = file;
            wordCount = format.getWordCount();
        } else {
            Toast.makeText(this, "please select .ifo file", Toast.LENGTH_LONG).show();
        }
    }

    @Override
    protected boolean dismissFragment() {
        fab.setVisibility(View.VISIBLE);
        return super.dismissFragment();
    }

    private View.OnClickListener fabClickListener = new View.OnClickListener() {
        @Override
        public void onClick(View v) {
            fileListFragment = new FileListFragment();
            showFragment(fileListFragment);
            fab.setVisibility(View.INVISIBLE);
        }
    };

    private AdapterView.OnItemClickListener dictClickListener = new AdapterView.OnItemClickListener() {
        @Override
        public void onItemClick(AdapterView<?> parent, View view, int position, long id) {
            DictFormat dictFormat = (DictFormat) parent.getItemAtPosition(position);
            String ifoPath = dictFormat.getData();
            IfoFormat format = new IfoFormat(new File(ifoPath));
            String msg = "Book Name: " + format.getBookName() + '\n'
                    + "Version: " + format.getVersion() + '\n'
                    + "Word Count: " + format.getWordCount();

            AlertDialogFragment dialogFragment = AlertDialogFragment
                    .newInstance("Remove Dictionary?", msg, "Remove", "Cancel");
            dialogFragment.show(getFragmentManager(), null);
            deleteItemPosition = position;
            action = DEL;
        }
    };

    Handler handler = new Handler(new Handler.Callback() {
        @Override
        public boolean handleMessage(Message msg) {
            switch (msg.what) {
                case ERR:    // something wrong happened
                    return true;
                case PROCESSING: //in inserting words to SQLiteDatabase
                    setProgressDialog(msg.arg1);
                    return true;
                case OK: //have done loading new dictionary words index to SQLiteDatabase
                    dismissProgressDialog();
                    setNotification(msg.obj + " has been added");
                    return true;
                case DELETE: //delete SQLiteDatabase table
                    setNotification(msg.obj + " has been removed");
                    return true;
                case DECOMPRESS: //have done decompressing
                    return true;
                case DECOMPRESS_ERR:
                    return true;
                default:
                    return false;
            }
        }
    });

}
