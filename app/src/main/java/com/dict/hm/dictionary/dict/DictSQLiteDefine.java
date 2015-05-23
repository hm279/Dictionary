package com.dict.hm.dictionary.dict;

import android.app.SearchManager;

/**
 * Created by hm on 15-5-4.
 */
public class DictSQLiteDefine {
    /** ------------------------------------------------------------------------------------------*
     *  ------------------------ FTS3 table defined for word -------------------------------------*
     *
     * define dictionary's word table.
     * this table will be created as FTS3 table in SQLiteDatabase.
     */
    public static final String COLUMN_KEY_WORD = SearchManager.SUGGEST_COLUMN_TEXT_1;

    /**
     * define word table
     */
    public static final String WORD_FTS_TABLE =
            "CREATE VIRTUAL TABLE %s USING fts3 (" + COLUMN_KEY_WORD + " TEXT)";

    public static final String sortOrder = COLUMN_KEY_WORD + " ASC"; //"ASC"

    public static final String FTS_PREFIX = "fts3-";

    /**-------------------------------------------------------------------------------------------*
     * ------------------------- normal table defined for word index -----------------------------*
     *
     * define dictionary's word indexing.
     * this table will be created as normal table in SQLiteDatabase.
     */
    public static final String COLUMN_OFFSET = "offset";
    public static final String COLUMN_SIZE = "size";

    /**
     * define index table
     */
    public static final String WORD_INDEX_TABLE = "CREATE TABLE %s (" +
            COLUMN_OFFSET + " INTEGER, " + COLUMN_SIZE + " INTEGER)";

    /** ------------------------------------------------------------------------------------------*
     * define SQLiteDatabase name and version
     */
    public static final String DATABASE_NAME = "dictionary.db";
    public static final int VERSION = 3;

    /** ------------------------------------------------------------------------------------------*
     * define a inner join sql
     */
    private static final String inner_join_sql = "select * from %s a inner join %s b" +
            " ON a.rowid = b.rowid WHERE b." + COLUMN_KEY_WORD + " MATCH ? AND length(" + COLUMN_KEY_WORD + ")=%d";


    public static String getCreateFTSTable(String name) {
        return String.format(WORD_FTS_TABLE, name);
    }

    public static String getCreateTable(String name) {
        return String.format(WORD_INDEX_TABLE, name);
    }

    public static String getInnerJoinSql(String mIndexTable, String mWordTable, int wordLength) {
        return String.format(inner_join_sql, mIndexTable, mWordTable, wordLength);
    }

    public static String getWordFtsTable(String mTableName) {
        return "[" + FTS_PREFIX + mTableName + "]";
    }

}
