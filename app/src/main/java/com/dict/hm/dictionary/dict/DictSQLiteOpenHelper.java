package com.dict.hm.dictionary.dict;

/**
 * Created by hm on 15-1-15.
 */


import android.app.SearchManager;
import android.content.ContentValues;
import android.content.Context;
import android.database.Cursor;
import android.database.sqlite.SQLiteDatabase;
import android.database.sqlite.SQLiteOpenHelper;
import android.os.Handler;
import android.os.Message;
import android.util.Log;

import com.dict.hm.dictionary.BaseManagerActivity;
import com.dict.hm.dictionary.DictManagerActivity;
import com.dict.hm.dictionary.parse.IdxParser;

import java.io.File;
import java.io.IOException;
import java.util.ArrayList;

/**
 * FTS is short for "full-text search"
 * A table that lacks the WITHOUT ROWID clause is called a "rowid table",
 * which has a column called "rowid".
 * the DATABASE_NAME is the name of the database file, or null for an in-memory database
 */
public class DictSQLiteOpenHelper extends SQLiteOpenHelper {
    private static final String TAG = "DictSQLiteOpenHelper";
    private SQLiteDatabase database;

    public static final String KEY_WORD = SearchManager.SUGGEST_COLUMN_TEXT_1;
    public static final String KEY_OFFSET = "offset";
    public static final String KEY_SIZE = "size";

    public static final String DATABASE_NAME = "dictionary.db";
    private static final int DATABASE_VERSION = 2;

    private static final String FTS_TABLE_CREATE =
                "CREATE VIRTUAL TABLE ";
    private static final String FTS_TABLE =" USING fts3 (" +
                        KEY_WORD + ", " +
                        KEY_OFFSET + " INTEGER, " +
                        KEY_SIZE + " INTEGER" +
                        ");";
    public final String sortOrder = KEY_WORD + " ASC"; //"ASC"

    private String mTableName;
    private static DictSQLiteOpenHelper helper;

    public static DictSQLiteOpenHelper getInstance(Context context) {
        if (helper == null) {
            File externalDir = context.getExternalFilesDir(null);
            String databaseName;
            if (externalDir != null) {
                databaseName = externalDir.getAbsolutePath() + File.separator + DATABASE_NAME;
            } else {
                databaseName = DATABASE_NAME;
            }
            helper = new DictSQLiteOpenHelper(context.getApplicationContext(), databaseName);
        }
        return helper;
    }
    /**
     * constructor for query SQLite database
     * @param context
     */
    public DictSQLiteOpenHelper(Context context, String databaseName) {
        super(context, databaseName, null, DATABASE_VERSION);
    }

    @Override
    public void onCreate(SQLiteDatabase db) {
        Log.d(TAG, "onCreate DictSQLiteOpenHelper...");
        database = db;
//        database.execSQL(FTS_TABLE_CREATE);
    }

    @Override
    public void onUpgrade(SQLiteDatabase db, int oldVersion, int newVersion) {
        Log.w(TAG, "onUpgrade");
//        db.execSQL("DROP TABLE IF EXISTS " + FTS_VIRTUAL_TABLE);
        onCreate(db);
    }

    public void dropTable(String table) {
        if (table != null) {
            SQLiteDatabase db = getWritableDatabase();
            db.execSQL("DROP TABLE IF EXISTS [" + table + "]");
        }
    }

    public Cursor getTables() {
        SQLiteDatabase db = getWritableDatabase();
        return db.rawQuery("SELECT * FROM sqlite_master  WHERE TYPE='table'", null);
    }

    public String getCreateTableSQL(String mBookName) {
        return FTS_TABLE_CREATE +  mBookName + FTS_TABLE;
    }

    private long addWord(String word, int offset, int size) {
        ContentValues values = new ContentValues();
        values.put(KEY_WORD, word);
        values.put(KEY_OFFSET, offset);
        values.put(KEY_SIZE, size);
        return database.insert(mTableName, null, values);
    }

    private void addWords(ArrayList<IdxParser.WordEntry> list) {
        if (database == null) {
            return;
        }
        database.beginTransaction();
        long id;

        for (IdxParser.WordEntry entry : list) {
            id = addWord(new String(entry.word), entry.offset, entry.size);
            if (id < 0) {
                Log.e(TAG, "unable to add word:" + new String(entry.word).trim());
            }
        }
        database.setTransactionSuccessful();
        database.endTransaction();
    }

    private void createTable(String bookName) {
        mTableName = "[" + bookName + "]";
        String CreateTableSQL = getCreateTableSQL(mTableName);
        database = getWritableDatabase();
        database.execSQL(CreateTableSQL);
    }

    /**
     *
     * @param bookName book name is used for the SQLiteDatabase's name
     * @param idxFile
     * @param handler
     */
    public void loadDictionary(String bookName, File idxFile, Handler handler) {
        new LoadDictionary(bookName, idxFile, handler).start();
    }

    private class LoadDictionary extends Thread {
        File idxFile;
        Handler handler;
        String bookName;

        private LoadDictionary(String bookName, File idxFile, Handler handler) {
            this.idxFile = idxFile;
            this.handler = handler;
            this.bookName = bookName;
        }

        @Override
        public void run() {
            try {
                createTable(bookName);
                loadWords(idxFile);
            } catch (IOException e) {
                throw new RuntimeException(e);
            }
        }

        private void loadWords(File idxFile) throws IOException {
            Log.d(TAG, "Loading words...");
            IdxParser parser = new IdxParser(idxFile);
            ArrayList<IdxParser.WordEntry> list;
            int count = 0;

            while (true) {
                list = parser.getWordEntries();
                if (list == null || list.size() == 0) {
                    break;
                }
                addWords(list);
                count += list.size();
                Message message = handler.obtainMessage(DictManagerActivity.PROCESSING);
                message.arg1 = count;
                handler.sendMessage(message);
            }
            Message message = handler.obtainMessage(BaseManagerActivity.OK);
            message.obj = bookName;
            handler.sendMessage(message);
            Log.d(TAG, "DONE Loading words.");
        }
    }

}
