package com.dict.hm.dictionary.dict;

import android.content.Context;
import android.content.SharedPreferences;
import android.database.Cursor;
import android.os.Handler;
import android.os.Message;
import android.util.Log;
import android.widget.Toast;

import com.dict.hm.dictionary.BaseManagerActivity;
import com.dict.hm.dictionary.R;
import com.dict.hm.dictionary.async.LoadDictionary;
import com.dict.hm.dictionary.lib.UnGzipThread;
import com.dict.hm.dictionary.parse.IfoFormat;

import java.io.File;
import java.util.HashSet;
import java.util.Set;

/**
 * Created by hm on 15-1-23.
 */
public class DictManager {
    /**
     * KEY ---> SharedPreferences
     * KEY ---> books set
     * ACTIVE ---> the current active dictionary
     * 'booName' ---> the '.ifo' file's path
     *
     * the bookName is also the database name
     */
    private static final String ACTIVE = "active";
    private String KEY;
    private Set<String> books;
    private String activeBook = null;
    private boolean init_finished = false;

    Context context;
    static DictManager manager;

    public static DictManager getInstance(Context context) {
        if (manager == null) {
            manager = new DictManager(context);
        }
        return manager;
    }

    /**
     *
     * @param context
     *
     */
    public DictManager(Context context) {
        this.context = context.getApplicationContext();
        KEY = context.getString(R.string.preference_file_key);

        SharedPreferences preferences = context.getSharedPreferences(KEY, Context.MODE_PRIVATE);
        books = preferences.getStringSet(KEY, new HashSet<String>());
    }

    /**
     *
     * All a new dictionary. Storing the book name and book name's '.ifo' file path.
     * while the book name exist, the path doesn't exist, it means that it hadn't finished loading
     * the dictionary to database.
     *
     */
    public void saveDictionaryFilePath(String bookName, String path) {
        SharedPreferences sharedPreferences = context.getSharedPreferences(KEY, Context.MODE_PRIVATE);
        SharedPreferences.Editor editor = sharedPreferences.edit();

        if (books == null) {
            books = new HashSet<>();
        }
        if (bookName != null) {
            books.add(bookName);
            editor.putStringSet(KEY, books);
        }
        if (path != null) {
            editor.putString(bookName, path);
        }
        editor.apply();
        Log.d("Add Dictionary", bookName + "@" + path);
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
    public String addDictionary(File ifoFile, Handler mHandler) {
        if (ifoFile == null) {
            return null;
        }
        IfoFormat format = new IfoFormat(ifoFile);
        if (books.contains(format.getBookName())) {
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
            if (!idxFile.exists() && gzipIdxFile.exists()) {
                DictSQLiteOpenHelper.getInstance(context)
                        .loadDictionary(format.getBookName(), gzipIdxFile, mHandler);
            } else {
                DictSQLiteOpenHelper.getInstance(context)
                        .loadDictionary(format.getBookName(), idxFile, mHandler);
            }
            manager.saveDictionaryFilePath(format.getBookName(), ifoFile.getAbsolutePath());
            return format.getBookName();
        } else if (idxFile.exists()){
            Toast.makeText(context, dictFile.getName() + " file missing", Toast.LENGTH_LONG).show();
        } else if (dictFile.exists()) {
            Toast.makeText(context, idxFile.getName() + " file missing", Toast.LENGTH_LONG).show();
        } else {
            Toast.makeText(context, "files missing", Toast.LENGTH_LONG).show();
        }
        return null;
    }

    public void loadDictionary(String mBookName, File idxFile, Handler handler) {
        DictIndexSQLiteHelper indexSQLiteHelper = DictIndexSQLiteHelper.getInstance(context);
        DictWordSQLiteHelper wordSQLiteHelper = DictWordSQLiteHelper.getInstance(context);
        new LoadDictionary(mBookName, idxFile, wordSQLiteHelper, indexSQLiteHelper, handler).start();
    }


    /**
     *
     * @param bookName
     * Remove a dictionary. Delete the book name and the book name's path
     */
    public void removeDictionary(String bookName, Handler handler) {
        new DropTableThread(bookName, handler).start();
        if (books != null && books.contains(bookName)) {
            books.remove(bookName);
            SharedPreferences preferences = context.getSharedPreferences(KEY, Context.MODE_PRIVATE);
            SharedPreferences.Editor editor = preferences.edit();
            editor.putStringSet(KEY, books);
            editor.remove(bookName);
            if (activeBook.equals(bookName)) {
                activeBook = null;
                editor.remove(ACTIVE);
            }
            editor.apply();
            /**
             * reset the active book
             */
            getActiveBook();
        }
    }

    public void clearAllData() {
        File file = context.getExternalFilesDir(null);
        if (file != null && context.deleteDatabase(file.getAbsolutePath() + File.separator
                + DictSQLiteOpenHelper.DATABASE_NAME)) {
            Toast.makeText(context, "Clear!!!", Toast.LENGTH_SHORT)
                    .show();
        }
        SharedPreferences preferences = context.getSharedPreferences(KEY, Context.MODE_PRIVATE);
        SharedPreferences.Editor editor = preferences.edit();
        for (String book : books) {
            editor.remove(book);
        }
        editor.remove(ACTIVE);
        editor.remove(KEY);
        editor.apply();
        activeBook = null;
        books.clear();
    }

    public String getBookFilePath(String bookName) {
        SharedPreferences preferences = context.getSharedPreferences(KEY, Context.MODE_PRIVATE);
        return preferences.getString(bookName, null);
    }

    /**
     *
     * @return the book which is current using for word query.
     */
    public String getActiveBook() {
        if (books == null) {
            return null;
        }
        if (activeBook == null) {
            SharedPreferences preferences = context.getSharedPreferences(KEY, Context.MODE_PRIVATE);
            activeBook = preferences.getString(ACTIVE, null);
            if (activeBook == null || !books.contains(activeBook)) {
                if (books.size() < 1) {
                    return null;
                } else {
                    activeBook = books.iterator().next();
                    SharedPreferences.Editor editor = preferences.edit();
                    editor.putString(ACTIVE, activeBook);
                    editor.apply();
                }
            }
        }
        if (books.contains(activeBook)) {
            return activeBook;
        } else {
            return null;
        }
    }

    public void saveActiveBook() {
        SharedPreferences preferences = context.getSharedPreferences(KEY, Context.MODE_PRIVATE);
        SharedPreferences.Editor editor = preferences.edit();
        editor.putString(ACTIVE, activeBook);
        editor.apply();
    }

    /**
     *
     * @param booKName change the using book name for word query.
     * @return true if change successfully, otherwise false
     */
    public boolean setActiveBook(String booKName) {
        if (books.contains(booKName)) {
            activeBook = booKName;
            return true;
        }
        return false;
    }

    public Set<String> getBooks() {
        return books;
    }

    private class InitDictThread extends Thread {
        Context context;
        Handler handler;

        public InitDictThread(Context context, Handler handler) {
            this.context = context;
            this.handler = handler;
        }

        @Override
        public void run() {
            Cursor cursor = DictSQLiteOpenHelper.getInstance(this.context).getTables();
            if (cursor != null) {
                int index = cursor.getColumnIndex("name");
                cursor.moveToFirst();
                do {
                    String name = cursor.getString(index);
                    if (name.endsWith("_content") || name.endsWith("_segdir") ||
                            name.endsWith("_segments") || name.equals("android_metadata")) {
                        Log.d("table", cursor.getString(index));
                    } else {
                        books.add(name);
                    }
                } while (cursor.moveToNext());
                init_finished= true;
                Message msg = handler.obtainMessage(0);
                handler.sendMessage(msg);
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
}
