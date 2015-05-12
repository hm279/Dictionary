package com.dict.hm.dictionary.dict;

import android.content.ContentValues;
import android.content.Context;
import android.database.Cursor;
import android.database.sqlite.SQLiteDatabase;
import android.database.sqlite.SQLiteOpenHelper;
import android.support.v4.app.INotificationSideChannel;
import android.util.Log;

import com.dict.hm.dictionary.paper.PaperJsonReader;

import java.util.HashMap;
import java.util.Set;

/**
 * Created by hm on 15-3-15.
 */
public class MyDictSQLiteOpenHelper extends SQLiteOpenHelper{
    private static final int version = 1;
    private static final String mTableName = "myDict";
    public static final String WORD = "word";
    public static final String TIME = "time";
    private static final String TABLE_CREATE = "CREATE TABLE " + mTableName + "("
            + WORD + " TEXT, " + TIME + " INTEGER);";

    Context context;
    private static final String queryColumn[] = {"rowid", TIME};
    private static final String querySelection = WORD + " = ?";
    private static final String updateSelection = "rowid = ?";
    /**
     * for 'select * from Table LIMIT 100 OFFSET 0'
     */
//    private static final String querySelectionBetween = "limit ? offset ?";
//    private static final String queryOrderBy = "rowid ASC";
//    private static final String queryColumnAll[] = {"rowid", WORD, TIME};
    private static final String queryWordsSQL = "select rowid, * from " + mTableName + " order by rowid";

    private SQLiteDatabase database = null;
    private static MyDictSQLiteOpenHelper instance = null;

    public static MyDictSQLiteOpenHelper getInstance(Context content) {
        if (instance == null) {
            instance = new MyDictSQLiteOpenHelper(content.getApplicationContext());
        }
        return instance;
    }

    public MyDictSQLiteOpenHelper(Context context) {
        super(context, mTableName, null, version);
        this.context = context;
    }

    @Override
    public void onCreate(SQLiteDatabase db) {
        db.execSQL(TABLE_CREATE);
        Log.d(mTableName, "onCreate");
    }

    @Override
    public void onUpgrade(SQLiteDatabase db, int oldVersion, int newVersion) {
        Log.d(mTableName, "onUpgrade");
    }

    public void insertWords(HashMap<String, Integer> words) {
        insertWords(words, false);
    }

    public HashMap<String, Integer> filterWords(HashMap<String, Integer> words) {
        return insertWords(words, true);
    }

    private HashMap<String, Integer> insertWords(HashMap<String, Integer> words, boolean filter) {
        if (database == null) {
            database = getWritableDatabase();
        }
        database.beginTransaction();

        HashMap<String, Integer> left = new HashMap<>();
        long time;
        Set<String> strings = words.keySet();
        for (String word : strings) {
            index result = queryWord(word);
            int increase = words.get(word);
            if (result != null) {
                if (Long.MAX_VALUE - result.time < increase) {
                    time = Long.MAX_VALUE;
                } else {
                    time = result.time + increase;
                }
                updateWord(result.id, time);
            } else {
                if (filter) {
                    left.put(word, increase);
                } else {
                    insertWord(word, increase);
                }
            }
            Log.d("Insert word", word + "-" + increase);
        }
        database.setTransactionSuccessful();
        database.endTransaction();

        return left;
    }

    public Cursor getWords(long size, long offset) {
        if (database == null) {
            database = getReadableDatabase();
        }
        String sql = queryWordsSQL + " limit " + size + " offset " + offset;
        Log.d("sql", sql);
        return database.rawQuery(sql, null);
//        return database.query(mTableName, null, querySelectionBetween,
//                new String[]{Long.toString(size), Long.toString(offset)}, null, null, queryOrderBy);
    }

    private long insertWord(String word, int time) {
        ContentValues values = new ContentValues();
        values.put(WORD, word);
        values.put(TIME, time);
        return database.insert(mTableName, null, values);
    }

    private long updateWord(long id, long time) {
        ContentValues values = new ContentValues();
        values.put(TIME, time);
        return database.update(mTableName, values, updateSelection, new String[]{Long.toString(id)});
    }

    private index queryWord(String word) {
        index result = null;
        Cursor cursor = database.query(mTableName, queryColumn, querySelection, new String[]{word},
                null, null, null);
        if (cursor != null && cursor.getCount() > 0) {
            cursor.moveToFirst();
            int idIndex = cursor.getColumnIndex("rowid");
            int timeIndex = cursor.getColumnIndex(TIME);
            long id = cursor.getLong(idIndex);
            long time = cursor.getLong(timeIndex);
            result = new index(id, time);
        }
        if (cursor != null) {
            cursor.close();
        }
        return result;
    }

    private class index {
        long id;
        long time;

        public index(long id, long time) {
            this.id = id;
            this.time = time;
        }
    }

}
