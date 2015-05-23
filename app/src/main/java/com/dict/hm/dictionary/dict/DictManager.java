package com.dict.hm.dictionary.dict;

import android.content.Context;
import android.database.Cursor;
import android.os.Handler;
import android.os.Message;
import android.widget.Toast;

import com.dict.hm.dictionary.BaseManagerActivity;
import com.dict.hm.dictionary.MainActivity;
import com.dict.hm.dictionary.async.LoadDictionary;
import com.dict.hm.dictionary.async.UserAsyncWorkerHandler;
import com.dict.hm.dictionary.lib.UnGzipThread;
import com.dict.hm.dictionary.parse.IfoFormat;

import java.io.File;
import java.lang.ref.WeakReference;
import java.util.ArrayList;

/**
 * Created by hm on 15-5-20.
 */
public class DictManager implements UserAsyncWorkerHandler.DictManagerCallback{
    private ArrayList<Long> rowid;
    private ArrayList<DictFormat> dictFormats;
    private int active;
    private boolean inited = false;

    Context context;
    UserDictSQLiteOpenHelper helper;
    UserAsyncWorkerHandler queryHandler;
    WeakReference<MainActivity.QueryCallback> reference = null;

    static DictManager manager;

    public static DictManager getInstance(Context context) {
        if (manager == null) {
            manager = new DictManager(context);
        }
        return manager;
    }

    public DictManager(Context context) {
        this.context = context.getApplicationContext();
        rowid = new ArrayList<>();
        dictFormats = new ArrayList<>();
        active = -1;
        queryHandler = UserAsyncWorkerHandler.getInstance(context, this);
        helper = UserDictSQLiteOpenHelper.getInstance(context);
        queryHandler.startQuery();
    }

    public void removeDictionary(int position, Handler handler) {
        DictFormat format = dictFormats.get(position);
        new DropTableThread(format.mTableName, handler).start();
        deleteUserDict(position);
    }

    /**
     *
     * @param ifoFile The new dictionary ifo file.
     * @param mHandler
     * @return BookName if start to add dictionary, null if do nothing.
     *
     * three dictionary files are needed.
     * .ifo.
     * .idx.
     * .dict.
     */
    public DictFormat addDictionary(File ifoFile, Handler mHandler) {
        if (ifoFile == null) {
            return null;
        }
        IfoFormat format = new IfoFormat(ifoFile);
        if (checkDict(format.getBookName())) {
            Toast.makeText(context, format.getBookName() + " has existed", Toast.LENGTH_LONG).show();
            return null;
        }
        String ifoName = ifoFile.getName();
        String prefixName = ifoName.substring(0, ifoName.lastIndexOf(".ifo"));
        File dir = ifoFile.getParentFile();

        File gzipIdxFile = new File(dir, prefixName + ".idx.gz");
        File gzipDictFile = new File(dir, prefixName + ".dict.dz");
        File idxFile = new File(dir, prefixName + ".idx");
        File dictFile = new File(dir, prefixName + ".dict");

        if ((idxFile.isFile() || gzipIdxFile.isFile())
                && (dictFile.isFile() || gzipDictFile.isFile())) {
            if (!dictFile.exists() && gzipDictFile.exists()) {
                new UnGzipThread(gzipDictFile, dictFile, mHandler).start();
            }

            DictSQLiteOpenHelper helper = DictSQLiteOpenHelper.getInstance(context);
            if (!idxFile.exists() && gzipIdxFile.exists()) {
                new LoadDictionary(format.getBookName(), idxFile, helper, mHandler).start();
            } else {
                new LoadDictionary(format.getBookName(), idxFile, helper, mHandler).start();
            }
            DictFormat dictFormat = new DictFormat(format.getBookName(), DictFormat.STAR_DICT, 0,
                    ifoFile.getAbsolutePath(), format.getBookName());
            insertUserDict(dictFormat);
            return dictFormat;
        } else if (idxFile.exists()){
            Toast.makeText(context, dictFile.getName() + " file missing", Toast.LENGTH_LONG).show();
        } else if (dictFile.exists()) {
            Toast.makeText(context, idxFile.getName() + " file missing", Toast.LENGTH_LONG).show();
        } else {
            Toast.makeText(context, "files missing", Toast.LENGTH_LONG).show();
        }
        return null;
    }

    private boolean checkDict(String name) {
        for (DictFormat format : dictFormats) {
            if (format.name.equals(name)) {
                return true;
            }
        }
        return false;
    }

    /**
     * when add dictionary, insert dictionary data to UserDict.
     */
    private void insertUserDict(DictFormat format) {
        long id = helper.insertDictionary(format);
        if (id == -1) {
            return;
        }
        dictFormats.add(format);
        rowid.add(id);
        if (active == -1) {
            setActiveDict(0);
        }
    }

    /**
     * when remove dictionary, delete dictionary data from UserDict.
     */
    private void deleteUserDict(int position) {
        helper.deleteDictionary(rowid.get(position));
        dictFormats.remove(position);
        rowid.remove(position);
        if (active == position) {
            active = -1;
            if (dictFormats.size() > 0) {
                setActiveDict(0);
            }
        }
    }

//    public void clearAllDictionaries() {
//    }

    public int getActiveDict() {
        return active;
    }

    public void setActiveDict(int active) {
        if (active == this.active || active < 0 || active >= dictFormats.size()) {
            return;
        }
        if (this.active >= 0) {
            dictFormats.get(this.active).setOn(0);
            queryHandler.startUpdate(rowid.get(this.active), 0);
        }
        dictFormats.get(active).setOn(1);
        queryHandler.startUpdate(rowid.get(active), 1);
        this.active = active;
    }

    public DictFormat getDictFormat(int id) {
        if (id >=0 && id < dictFormats.size()) {
            return dictFormats.get(id);
        }
        return null;
    }

    public ArrayList<DictFormat> getDictFormats() {
        return dictFormats;
    }

    public boolean isInited() {
        return inited;
    }

    private void setRowid(ArrayList<Long> rowid) {
        this.rowid.addAll(rowid);
    }

    private void setDictFormats(ArrayList<DictFormat> dictFormats) {
        this.dictFormats.addAll(dictFormats);
        for (int i = 0; i < dictFormats.size(); i++) {
            if (dictFormats.get(i).on > 0) {
                active = i;
                return;
            }
        }
    }

    public class DropTableThread extends Thread {
        String book;
        Handler handler;

        public DropTableThread(String book, Handler handler) {
            this.book = book;
            this.handler = handler;
        }

        @Override
        public void run() {
            DictSQLiteOpenHelper.getInstance(context).dropTable(book);
            Message message = handler.obtainMessage(BaseManagerActivity.DELETE);
            message.obj = book;
            handler.sendMessage(message);
        }
    }

    public void setOnQueryCompleteCallback(MainActivity.QueryCallback callback) {
        reference = new WeakReference<MainActivity.QueryCallback>(callback);
    }

    @Override
    public void onQueryComplete(Cursor result) {
        ArrayList<DictFormat> formats;
        ArrayList<Long> rowids;
        if (result != null) {
            if (result.moveToFirst()) {
                int idIndex = result.getColumnIndex("rowid");
                int nameIndex = result.getColumnIndex(UserDictSQLiteOpenHelper.COLUMN_DICT_NAME);
                int typeIndex = result.getColumnIndex(UserDictSQLiteOpenHelper.COLUMN_DICT_TYPE);
                int dataIndex = result.getColumnIndex(UserDictSQLiteOpenHelper.COLUMN_DICT_DATA);
                int onIndex = result.getColumnIndex(UserDictSQLiteOpenHelper.COLUMN_DICT_ACTIVE);
                int tableIndex = result.getColumnIndex(UserDictSQLiteOpenHelper.COLUMN_TABLE_NAME);
                formats = new ArrayList<>();
                rowids = new ArrayList<>();
                do {
                    formats.add(new DictFormat(
                            result.getString(nameIndex),
                            result.getInt(typeIndex),
                            result.getInt(onIndex),
                            result.getString(dataIndex),
                            result.getString(tableIndex)));
                    rowids.add(result.getLong(idIndex));
                } while (result.moveToNext());
                setDictFormats(formats);
                setRowid(rowids);
            }
            result.close();
        }
        inited = true;
        if (reference != null) {
            MainActivity.QueryCallback callback = reference.get();
            if (callback != null) {
                callback.onQueryComplete();
            }
        }
    }

    @Override
    public void onUpdateComplete(long id) {

    }

    @Override
    public void onInsertComplete(long id) {

    }

    @Override
    public void onDeleteComplete(long id) {

    }
}
