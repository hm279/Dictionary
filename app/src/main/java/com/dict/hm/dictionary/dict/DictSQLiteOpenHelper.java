package com.dict.hm.dictionary.dict;

import android.content.ContentValues;
import android.content.Context;
import android.database.Cursor;
import android.database.sqlite.SQLiteDatabase;
import android.database.sqlite.SQLiteOpenHelper;
import android.util.Log;

import com.dict.hm.dictionary.parse.IdxParser;

import java.io.File;
import java.util.ArrayList;

/**
 * Created by hm on 15-5-12.
 * Every normal table has a corresponding fts3 table.
 * Define the fts3 table name to "fts3- + 'normal table name'", like: table1 ---> fts3-table1
 */
public class DictSQLiteOpenHelper extends SQLiteOpenHelper{
    private static final String TAG = "DictSQLiteOpenHelper";
    private SQLiteDatabase database = null;
    private String wordTableName;
    private String indexTableName;
    private static DictSQLiteOpenHelper helper;

    public static DictSQLiteOpenHelper getInstance(Context context) {
        if (helper == null) {
            File externalDir = context.getExternalFilesDir(null);
            String databaseName;
            if (externalDir != null) {
                databaseName = externalDir.getAbsolutePath() + File.separator + DictSQLiteDefine.DATABASE_NAME;
            } else {
                databaseName = DictSQLiteDefine.DATABASE_NAME;
            }
            helper = new DictSQLiteOpenHelper(context.getApplicationContext(), databaseName);
        }
        return helper;
    }

    public DictSQLiteOpenHelper(Context context, String mDatabaseName) {
        super(context, mDatabaseName, null, DictSQLiteDefine.VERSION);
    }

    @Override
    public void onCreate(SQLiteDatabase db) {
        database = db;
    }

    @Override
    public void onUpgrade(SQLiteDatabase db, int oldVersion, int newVersion) {
        database = db;
    }

    public void dropTable(String table) {
        if (table != null) {
            database = getWritableDatabase();
            database.execSQL("DROP TABLE IF EXISTS [" + table + "]");
            database.execSQL("DROP TABLE IF EXISTS [" + DictSQLiteDefine.FTS_PREFIX + table + "]");
        }
    }

    public Cursor getTables() {
        database = getWritableDatabase();
        return database.rawQuery("SELECT * FROM sqlite_master WHERE TYPE='table'", null);
    }

    private long addWord(String word) {
        ContentValues values = new ContentValues();
        values.put(DictSQLiteDefine.COLUMN_KEY_WORD, word);
        return database.insert(wordTableName, null, values);
    }

    private long addWord(int offset, int size) {
        ContentValues values = new ContentValues();
        values.put(DictSQLiteDefine.COLUMN_OFFSET, offset);
        values.put(DictSQLiteDefine.COLUMN_SIZE, size);
        return database.insert(indexTableName, null, values);
    }

    private void deleteWord(String mTableName, long id) {
        database.delete(mTableName, "rowid = ?", new String[]{Long.toString(id)});
    }

    public void addWords(ArrayList<IdxParser.WordEntry> list) {
        if (database == null) {
            return;
        }
        database.beginTransaction();
        long id0, id1;

        for (IdxParser.WordEntry entry : list) {
            id0 = addWord(new String(entry.word));
            id1 = addWord(entry.offset, entry.size);
            if (id0 < 0 || id1 < 0 || id0 != id1) {
                Log.e(TAG, "unable to add word:" + new String(entry.word).trim());
                if (id0 >= 0) {
                    deleteWord(wordTableName, id0);
                }
                if (id1 >= 0) {
                    deleteWord(indexTableName, id1);
                }
            }
        }
        database.setTransactionSuccessful();
        database.endTransaction();
    }

    public void createTable(String bookName) {
        wordTableName = "[" + DictSQLiteDefine.FTS_PREFIX + bookName + "]";
        indexTableName = "[" + bookName + "]";
        String createWordTableSQL = DictSQLiteDefine.getCreateFTSTable(wordTableName);
        String createIndexTableSQL = DictSQLiteDefine.getCreateTable(indexTableName);
        database = getWritableDatabase();
        database.execSQL(createWordTableSQL);
        database.execSQL(createIndexTableSQL);
    }

}
