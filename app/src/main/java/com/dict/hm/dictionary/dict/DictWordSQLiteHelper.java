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
public class DictWordSQLiteHelper extends SQLiteOpenHelper{
    private static final String TAG = "DictWordSQLiteHelper";
    private SQLiteDatabase database = null;
    private String mTableName;
    private static DictWordSQLiteHelper helper;

    public static DictWordSQLiteHelper getInstance(Context context) {
        if (helper == null) {
            File externalDir = context.getExternalFilesDir(null);
            String databaseName;
            if (externalDir != null) {
                databaseName = externalDir.getAbsolutePath() + File.separator + DictSQLiteDefine.WORD_TABLE_NAME;
            } else {
                databaseName = DictSQLiteDefine.WORD_TABLE_NAME;
            }
            helper = new DictWordSQLiteHelper(context.getApplicationContext(), databaseName);
        }
        return helper;
    }

    public DictWordSQLiteHelper(Context context, String mDatabaseName) {
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

    private long addWord(String word) {
        ContentValues values = new ContentValues();
        values.put(DictSQLiteDefine.KEY_WORD, word);
        return database.insert(mTableName, null, values);
    }

    public void addWords(ArrayList<IdxParser.WordEntry> list) {
        if (database == null) {
            return;
        }
        database.beginTransaction();
        long id;

        for (IdxParser.WordEntry entry : list) {
            id = addWord(new String(entry.word));
            if (id < 0) {
                Log.e(TAG, "unable to add word:" + new String(entry.word).trim());
            }
        }
        database.setTransactionSuccessful();
        database.endTransaction();
    }

    public void createTable(String bookName) {
        mTableName = "[" + bookName + "]";
        String CreateTableSQL = DictSQLiteDefine.getCreateFTSTable(mTableName);
        database = getWritableDatabase();
        database.execSQL(CreateTableSQL);
    }

}
