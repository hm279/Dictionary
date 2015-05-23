package com.dict.hm.dictionary.dict;

import android.app.SearchManager;
import android.content.Context;
import android.database.Cursor;
import android.database.sqlite.SQLiteDatabase;
import android.database.sqlite.SQLiteQueryBuilder;
import android.provider.BaseColumns;

import java.util.HashMap;

/**
 * Created by hm on 15-1-8.
 */
public class DictSQLiteDatabase {
    private final SQLiteDatabase database;
    private static final HashMap<String, String> columnMap = buildColumnMap();
    DictManager manager;

    public DictSQLiteDatabase(Context context) {
        DictSQLiteOpenHelper helper = DictSQLiteOpenHelper.getInstance(context);
        database = helper.getReadableDatabase();
        manager  = DictManager.getInstance(context);
    }

    public Cursor getWord(String rowId, String[] columns){
        String selection = "rowid = ?";
        String[] selectionArgs = new String[] {rowId};
        return getWord(selection, selectionArgs, columns);
    }

    public Cursor getIndexByRowId(String rowId) {
        String mTableName = getActiveTableName();
        if (mTableName == null) {
            return null;
        }
        String selection = "rowid = ?";
        String[] selectionArgs = new String[] {rowId};
        String mIndexTable = "[" + mTableName + "]";
        return database.query(mIndexTable, null, selection, selectionArgs, null, null, null);
    }

    /**
     * PageViewerAdapter will this function
     * need inner join
     *
     * @param word
     * @return
     */
    public Cursor getIndexByWord(String word) {
        String mTableName = getActiveTableName();
        if (mTableName == null) {
            return null;
        }
        String mIndexTable = "[" + mTableName + "]";
        String mWordTable = DictSQLiteDefine.getWordFtsTable(mTableName);
        int len = word.length();
        String sql = DictSQLiteDefine.getInnerJoinSql(mIndexTable, mWordTable, len);
        return database.rawQuery(sql, new String[]{word});
    }

    public Cursor getWordMatches(String query, String[] columns) {
        String selection = DictSQLiteDefine.KEY_WORD + " MATCH ?";
        String[] selectionArgs = new String[] {"^"+query+"*"};
        return query(selection, selectionArgs, columns);
    }

    public Cursor getWordMatchesInLength(String word, String[] columns) {
        int length = word.length();
        String selection = DictSQLiteDefine.KEY_WORD + " MATCH ? AND length("
                + DictSQLiteDefine.KEY_WORD + ")=" + length;
        String[] selectionArgs = new String[] {"^"+word};
        return query(selection, selectionArgs, columns);
    }

    private Cursor query(String selection, String[] selectionArgs, String[] columns) {
        String mTableName = getActiveTableName();
        if (mTableName == null) {
            return null;
        }
        String table = DictSQLiteDefine.getWordFtsTable(mTableName);
        SQLiteQueryBuilder builder = new SQLiteQueryBuilder();
        builder.setTables(table);
        builder.setProjectionMap(columnMap);

        Cursor cursor = builder.query(database,
                columns, selection, selectionArgs, null, null, DictSQLiteDefine.sortOrder);
        if (cursor == null) {
            return null;
        } else if (!cursor.moveToFirst()) {
            cursor.close();
            return null;
        }
        return cursor;
    }

    private Cursor getWord(String selection, String[] selectionArgs, String[] columns) {
        String mTableName = getActiveTableName();
        if (mTableName == null) {
            return null;
        }
        String table = "[" + mTableName + "]";
        SQLiteQueryBuilder builder = new SQLiteQueryBuilder();
        builder.setTables(table);
        builder.setProjectionMap(columnMap);

        Cursor cursor = builder.query(database,
                columns, selection, selectionArgs, null, null, null);
        if (cursor == null) {
            return null;
        } else if (!cursor.moveToFirst()) {
            cursor.close();
            return null;
        }
        return cursor;
    }

    private String getActiveTableName() {
        int i = manager.getActiveDict();
        if (i < 0) {
            return null;
        } else {
            DictFormat format = manager.getDictFormat(i);
            return format.mTableName;
        }
    }

    private static HashMap<String, String> buildColumnMap() {
        HashMap<String, String> map = new HashMap<>();
        map.put(DictSQLiteDefine.KEY_WORD, DictSQLiteDefine.KEY_WORD);
        map.put(BaseColumns._ID, "rowid AS " + BaseColumns._ID);
        map.put(SearchManager.SUGGEST_COLUMN_INTENT_DATA_ID, "rowid AS " +
                SearchManager.SUGGEST_COLUMN_INTENT_DATA_ID);
        map.put(SearchManager.SUGGEST_COLUMN_INTENT_EXTRA_DATA, DictSQLiteDefine.KEY_WORD + " AS " +
                SearchManager.SUGGEST_COLUMN_INTENT_EXTRA_DATA);
        return map;
    }

}
