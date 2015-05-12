package com.dict.hm.dictionary.dict;

import android.app.SearchManager;
import android.content.Context;
import android.database.Cursor;
import android.database.sqlite.SQLiteQueryBuilder;
import android.provider.BaseColumns;
import android.util.Log;

import java.util.HashMap;

/**
 * Created by hm on 15-1-8.
 */
public class DictSQLiteDatabase {
    private final DictSQLiteOpenHelper databaseOpenHelper;
    private static final HashMap<String, String> columnMap = buildColumnMap();
    DictManager manager;

    public DictSQLiteDatabase(Context context) {
        databaseOpenHelper = DictSQLiteOpenHelper.getInstance(context.getApplicationContext());
        manager  = DictManager.getInstance(context);
    }

    public Cursor getWord(String rowId, String[] columns) {
        String selection = "rowid = ?";
        String[] selectionArgs = new String[] {rowId};
        return query(selection, selectionArgs, columns);
    }

    public Cursor getWordMatches(String query, String[] columns) {
        String selection = DictSQLiteOpenHelper.KEY_WORD + " MATCH ?";
        String[] selectionArgs = new String[] {"^"+query+"*"};
        return query(selection, selectionArgs, columns);
    }

    public Cursor getWordMatchesInLength(String word, String[] columns) {
        int length = word.length();
        String selection = DictSQLiteOpenHelper.KEY_WORD + " MATCH ? AND length("
                + DictSQLiteOpenHelper.KEY_WORD + ")=" + length;
        String[] selectionArgs = new String[] {"^"+word};
        return query(selection, selectionArgs, columns);
    }

    private Cursor query(String selection, String[] selectionArgs, String[] columns) {
        if (manager.getActiveBook() == null) {
            Log.d(manager.toString(), "null");
            return null;
        }
        String table = "[" + manager.getActiveBook() + "]";
        SQLiteQueryBuilder builder = new SQLiteQueryBuilder();
        builder.setTables(table);
        builder.setProjectionMap(columnMap);

        Cursor cursor = builder.query(databaseOpenHelper.getReadableDatabase(),
                columns, selection, selectionArgs, null, null, databaseOpenHelper.sortOrder);
        if (cursor == null) {
            return null;
        } else if (!cursor.moveToFirst()) {
            cursor.close();
            return null;
        }
        return cursor;
    }

    private static HashMap<String, String> buildColumnMap() {
        HashMap<String, String> map = new HashMap<>();
        map.put(DictSQLiteOpenHelper.KEY_WORD, DictSQLiteOpenHelper.KEY_WORD);
        map.put(DictSQLiteOpenHelper.KEY_SIZE, DictSQLiteOpenHelper.KEY_SIZE);
        map.put(DictSQLiteOpenHelper.KEY_OFFSET, DictSQLiteOpenHelper.KEY_OFFSET);
        map.put(BaseColumns._ID, "rowid AS " + BaseColumns._ID);
        map.put(SearchManager.SUGGEST_COLUMN_INTENT_DATA_ID, "rowid AS " +
                SearchManager.SUGGEST_COLUMN_INTENT_DATA_ID);
        return map;
    }

}
