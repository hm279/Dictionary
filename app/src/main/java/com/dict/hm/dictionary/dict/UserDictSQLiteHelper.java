package com.dict.hm.dictionary.dict;

import android.content.ContentValues;
import android.content.Context;
import android.database.Cursor;
import android.database.sqlite.SQLiteDatabase;
import android.database.sqlite.SQLiteOpenHelper;
import android.util.Log;

import com.dict.hm.dictionary.paper.JsonEntry;
import com.dict.hm.dictionary.ui.UserDictFragment;
import com.dict.hm.dictionary.ui.dialog.SelectDialog;

import java.util.ArrayList;

/**
 * Created by hm on 15-3-15.
 */
public class UserDictSQLiteHelper extends SQLiteOpenHelper {
    private static final int VERSION = 3;
    private static final String USER_DB = "user.db";

    /** ------------------------------------------------------------------------------------------*
     * A table that record the user's known words.
     */
    public static final String USER_TABLE = "user";
    public static final String COLUMN_WORD = "word";
    public static final String COLUMN_COUNT = "count";
    public static final String COLUMN_TIME = "time";
    private static final String USER_TABLE_CREATE = "CREATE TABLE IF NOT EXISTS " + USER_TABLE + "("
            + COLUMN_WORD + " TEXT, "
            + COLUMN_COUNT + " INTEGER, "
            + COLUMN_TIME + " DATETIME DEFAULT CURRENT_DATE);";

    /** ------------------------------------------------------------------------------------------*
     * A table that record add user's dictionaries.
     * Including dictionary's name, type and table name in the fts table.
     */
    public static final String DICT_TABLE = "dictionaries";
    public static final String COLUMN_DICT_NAME = "name";
    public static final String COLUMN_DICT_TYPE = "type";
    public static final String COLUMN_DICT_DATA = "data";
    public static final String COLUMN_DICT_ACTIVE = "active";
    public static final String COLUMN_TABLE_NAME = "table_name";
    public static final String DICT_TABLE_CREATE = "CREATE TABLE IF NOT EXISTS " + DICT_TABLE + "("
            + COLUMN_DICT_NAME + " TEXT, "
            + COLUMN_DICT_TYPE + " INTEGER, "
            + COLUMN_DICT_DATA + " TEXT, "
            + COLUMN_DICT_ACTIVE + " INTEGER, "
            + COLUMN_TABLE_NAME + " TEXT);";

    /** ------------------------------------------------------------------------------------------*
     *
     */

    private static final String queryColumn[] = {"rowid", COLUMN_COUNT};
    private static final String querySelection = COLUMN_WORD + " = ?";
    private static final String updateSelection = "rowid = ?";
    /**
     * for 'select * from Table LIMIT 100 OFFSET 0'
     */
    private static final String queryWordsSQL = "select rowid, * from " + USER_TABLE;
    private static final String queryDictsSQL = "select rowid, * from " + DICT_TABLE;//+ " order by " + COLUMN_DICT_NAME;

    private SQLiteDatabase database = null;
    private static UserDictSQLiteHelper instance = null;

    public static UserDictSQLiteHelper getInstance(Context content) {
        if (instance == null) {
            instance = new UserDictSQLiteHelper(content.getApplicationContext());
        }
        return instance;
    }

    public UserDictSQLiteHelper(Context context) {
        super(context, USER_DB, null, VERSION);
    }

    @Override
    public void onCreate(SQLiteDatabase db) {
        db.execSQL(USER_TABLE_CREATE);
        db.execSQL(DICT_TABLE_CREATE);
        Log.d(USER_DB, "onCreate");
    }

    /**
     * Database upgrade may take a long time, you should not call method getReadableDatabase() or
     * getWritableDatabase() from the application main thread, including from
     * ContentProvider.onCreate().
     */
    @Override
    public void onUpgrade(SQLiteDatabase db, int oldVersion, int newVersion) {
//        db.execSQL("DROP TABLE IF EXISTS [" + USER_TABLE + "]");
//        onCreate(db);
        Log.d(USER_DB, "onUpgrade");
    }
    /** ------------------------- about dictionary's information --------------------------------*/

    public long insertDictionary(DictFormat format) {
        ContentValues values = new ContentValues();
        values.put(COLUMN_DICT_NAME, format.name);
        values.put(COLUMN_DICT_TYPE, format.type);
        values.put(COLUMN_DICT_DATA, format.data);
        values.put(COLUMN_DICT_ACTIVE, format.on);
        values.put(COLUMN_TABLE_NAME, format.mTableName);
        return database.insert(DICT_TABLE, null, values);
    }

    public long updateDictionary(long rowid, int on) {
        ContentValues values = new ContentValues();
        values.put(COLUMN_DICT_ACTIVE, on);
        return database.update(DICT_TABLE, values, "rowid = ?", new String[]{Long.toString(rowid)});
    }

    public long deleteDictionary(long rowid) {
        return database.delete(DICT_TABLE, "rowid = ?", new String[]{Long.toString(rowid)});
    }

    public Cursor queryDictionaries() {
        if (database == null) {
            database = getReadableDatabase();
        }
        return database.rawQuery(queryDictsSQL, null);
    }

    /** ------------------------ about user dict's words ---------------------------------------
        ------------------------ archive words to user dict ------------------------------------*/

    public void insertWords(ArrayList<JsonEntry> words) {
        insertWords(words, false);
    }

    public ArrayList<JsonEntry> filterWords(ArrayList<JsonEntry> words) {
        return insertWords(words, true);
    }

    private ArrayList<JsonEntry> insertWords(ArrayList<JsonEntry> words, boolean filter) {
        if (database == null) {
            database = getWritableDatabase();
        }
        database.beginTransaction();

        ArrayList<JsonEntry> left = new ArrayList<>();
        long count;
        String word;
        long increase;
        for (JsonEntry entry: words) {
            word = entry.getWord();
            increase = entry.getCount();
            Index result = queryWord(word);
            if (result != null) {
                if (Long.MAX_VALUE - result.count < increase) {
                    count = Long.MAX_VALUE;
                } else {
                    count = result.count + increase;
                }
                updateWord(result.id, count);
            } else {
                if (filter) {
                    left.add(entry);
                } else {
                    insertWord(word, increase);
                }
            }
        }
        database.setTransactionSuccessful();
        database.endTransaction();

        return left;
    }


    private long insertWord(String word, long count) {
        ContentValues values = new ContentValues();
        values.put(COLUMN_WORD, word);
        values.put(COLUMN_COUNT, count);
        return database.insert(USER_TABLE, null, values);
    }

    private long updateWord(long id, long count) {
        ContentValues values = new ContentValues();
        values.put(COLUMN_COUNT, count);
        return database.update(USER_TABLE, values, updateSelection, new String[]{Long.toString(id)});
    }

    private Index queryWord(String word) {
        Index result = null;
        Cursor cursor = database.query(USER_TABLE, queryColumn, querySelection, new String[]{word},
                null, null, null);
        if (cursor != null && cursor.getCount() > 0) {
            cursor.moveToFirst();
            int idIndex = cursor.getColumnIndex("rowid");
            int timeIndex = cursor.getColumnIndex(COLUMN_COUNT);
            long id = cursor.getLong(idIndex);
            long time = cursor.getLong(timeIndex);
            result = new Index(id, time);
        }
        if (cursor != null) {
            cursor.close();
        }
        return result;
    }

    private class Index {
        long id;
        long count;

        public Index(long id, long count) {
            this.id = id;
            this.count = count;
        }
    }

    public void clearUserWords() {
        SQLiteDatabase database = getWritableDatabase();
        database.delete(USER_TABLE, null, null);
    }

    public Cursor getWordsOrderBy(int which) {
        if (database == null) {
            database = getReadableDatabase();
        }
        String sql;
        if (which == UserDictFragment.ORDER_COUNT) {
            sql = queryWordsSQL + " order by " + COLUMN_COUNT + " DESC";
        } else if (which == UserDictFragment.ORDER_TIME){
            sql = queryWordsSQL + " order by " + COLUMN_TIME + " DESC";
        } else {
            sql = queryWordsSQL +  " order by rowid DESC" + " limit " + UserDictFragment.size;// + " offset " + offset;
        }
        return database.rawQuery(sql, null);
    }

}
