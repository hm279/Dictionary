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
    public static final String WORD_TABLE_NAME = "dict.fts3.db";
    public static final String KEY_WORD = SearchManager.SUGGEST_COLUMN_TEXT_1;

    /**
     * define word table
     */
    public static final String WORD_FTS_TABLE =
            "CREATE VIRTUAL TABLE %s USING fts3 (" + KEY_WORD + " TEXT)";

    /**-------------------------------------------------------------------------------------------*
     * ------------------------- normal table defined for word index -----------------------------*
     *
     * define dictionary's word indexing.
     * this table will be created as normal table in SQLiteDatabase.
     */
    public static final String INDEX_TABLE_NAME = "dict.index-0.db";
    public static final String OFFSET = "offset";
    public static final String SIZE = "size";

    /**
     * define index table
     */
    public static final String WORD_INDEX_TABLE = "CREATE TABLE %s (" +
            OFFSET + " INTEGER, " + SIZE + " INTEGER)";

    /** ------------------------------------------------------------------------------------------
     * define SQLiteDatabase version
     */
    public static final int VERSION = 1;

    public static String getCreateFTSTable(String name) {
        return String.format(WORD_FTS_TABLE, name);
    }

    public static String getCreateTable(String name) {
        return String.format(WORD_INDEX_TABLE, name);
    }

}
