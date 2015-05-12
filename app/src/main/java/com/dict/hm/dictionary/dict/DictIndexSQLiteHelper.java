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
 */
public class DictIndexSQLiteHelper extends SQLiteOpenHelper{
    private static final String TAG = "DictIndexSQLiteHelper";
    private SQLiteDatabase database = null;
    private String mTableName;
    private static DictIndexSQLiteHelper helper;

    public static DictIndexSQLiteHelper getInstance(Context context) {
        if (helper == null) {
            File externalDir = context.getExternalFilesDir(null);
            String databaseName;
            if (externalDir != null) {
                databaseName = externalDir.getAbsolutePath() + File.separator + DictSQLiteDefine.INDEX_TABLE_NAME;
            } else {
                databaseName = DictSQLiteDefine.INDEX_TABLE_NAME;
            }
            helper = new DictIndexSQLiteHelper(context.getApplicationContext(), databaseName);
        }
        return helper;
    }

    public DictIndexSQLiteHelper(Context context, String mDatabaseName) {
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
            SQLiteDatabase db = getWritableDatabase();
            db.execSQL("DROP TABLE IF EXISTS [" + table + "]");
        }
    }

    public Cursor getTables() {
        SQLiteDatabase db = getWritableDatabase();
        return db.rawQuery("SELECT * FROM sqlite_master  WHERE TYPE='table'", null);
    }

    private long addWord(int offset, int size) {
        ContentValues values = new ContentValues();
        values.put(DictSQLiteDefine.OFFSET, offset);
        values.put(DictSQLiteDefine.SIZE, size);
        return database.insert(mTableName, null, values);
    }

    public void addWords(ArrayList<IdxParser.WordEntry> list) {
        if (database == null) {
            return;
        }
        database.beginTransaction();
        long id;

        for (IdxParser.WordEntry entry : list) {
            id = addWord(entry.offset, entry.size);
            if (id < 0) {
                Log.e(TAG, "unable to add word:" + new String(entry.word).trim());
            }
        }
        database.setTransactionSuccessful();
        database.endTransaction();
    }

    public void createTable(String bookName) {
        mTableName = "[" + bookName + "]";
        String CreateTableSQL = DictSQLiteDefine.getCreateTable(mTableName);
        database = getWritableDatabase();
        database.execSQL(CreateTableSQL);
    }

}
