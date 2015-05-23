package com.dict.hm.dictionary.dict;


import android.app.SearchManager;
import android.content.ContentProvider;
import android.content.ContentResolver;
import android.content.ContentValues;
import android.content.UriMatcher;
import android.database.Cursor;
import android.net.Uri;
import android.provider.BaseColumns;
import android.text.TextUtils;
import android.util.Log;

/**
 * Created by hm on 15-1-7.
 */
public class DictContentProvider extends ContentProvider{
    String TAG = "DictContentProvider";
    public static String AUTHORITY = "com.dict.hm.dictionary.provider";
    public static final Uri CONTENT_URI = Uri.parse("content://" + AUTHORITY + "/dictionary");
    public static final String WORDS_MIME_TYPE = ContentResolver.CURSOR_DIR_BASE_TYPE +
            "/vnd.com.dict.hm.provider";
    public static final String DEFINITION_MIME_TYPE = ContentResolver.CURSOR_ITEM_BASE_TYPE +
            "/vnd.com.dict.hm.provider";

    private String[] suggestionColumns = new String[] {
            BaseColumns._ID,
            DictSQLiteDefine.KEY_WORD,
            /**
             * alias of KEY_WORD
             * This column allows suggestions to provide additional data that is included
             * as an extra in the intent’s EXTRA_DATA_KEY key.
             */
            SearchManager.SUGGEST_COLUMN_INTENT_EXTRA_DATA,
            /* SearchManager.SUGGEST_COLUMN_SHORTCUT_ID,
                        (only if you want to refresh shortcuts) */
            SearchManager.SUGGEST_COLUMN_INTENT_DATA_ID};

    private String[] wordColumns = new String[] {
            BaseColumns._ID,
            DictSQLiteDefine.KEY_WORD};

    //UriMatcher stuff
    private static final int SEARCH_WORDS = 0;
    private static final int GET_WORD = 1;
    private static final int SEARCH_SUGGEST = 2;
    private static final int REFRESH_SHORTCUT = 3;
    private static final int GET_INDEX_BY_WORD = 4;
    private static final UriMatcher uriMatcher = buildUriMatcher();

    private DictSQLiteDatabase mDictionary;

    public boolean onCreate() {
        mDictionary = new DictSQLiteDatabase(getContext());
        Log.d(TAG, "onCreate");
        return true;
    }

    private static UriMatcher buildUriMatcher() {
        UriMatcher matcher = new UriMatcher(UriMatcher.NO_MATCH);
        matcher.addURI(AUTHORITY, "dictionary", SEARCH_WORDS);
        matcher.addURI(AUTHORITY, "dictionary/#", GET_WORD);
        matcher.addURI(AUTHORITY, SearchManager.SUGGEST_URI_PATH_QUERY, SEARCH_SUGGEST);
        matcher.addURI(AUTHORITY, SearchManager.SUGGEST_URI_PATH_QUERY + "/*", SEARCH_SUGGEST);
        matcher.addURI(AUTHORITY, SearchManager.SUGGEST_URI_PATH_SHORTCUT, REFRESH_SHORTCUT);
        matcher.addURI(AUTHORITY, SearchManager.SUGGEST_URI_PATH_SHORTCUT + "/*", REFRESH_SHORTCUT);
        /**
         * this is for the PaperViewAdapter
         */
        matcher.addURI(AUTHORITY, "dictionary/word", GET_INDEX_BY_WORD);
        return matcher;
    }

    @Override
    public Cursor query(Uri uri, String[] projection, String selection, String[] selectionArgs,
                        String sortOrder) {
        switch (uriMatcher.match(uri)) {
            case SEARCH_SUGGEST:
                if (selectionArgs == null) {
                  throw new IllegalArgumentException(
                      "selectionArgs must be provided for the Uri: " + uri);
                }
                return getSuggestions(selectionArgs[0]);
            case SEARCH_WORDS:
                if (selectionArgs == null) {
                  throw new IllegalArgumentException(
                      "selectionArgs must be provided for the Uri: " + uri);
                }
                return search(selectionArgs[0]);
            case GET_INDEX_BY_WORD:
                if (selectionArgs == null) {
                    throw new IllegalArgumentException(
                    "selectionArgs must be provided for the Uri: " + uri);
                }
                return getWordIndex(selectionArgs[0]);
            case GET_WORD:
                return getWordIndex(uri);
            case REFRESH_SHORTCUT:
                return refreshShortcut(uri);
            default:
                throw new IllegalArgumentException("Unknown Uri: " + uri);
        }
    }

    /**
     *
     * @param query
     * @return
     * TODO: getSuggestions() can only get the suggest words without offset and size.
     */
    private Cursor getSuggestions(String query) {
        Log.d(TAG, "getSuggestions:" + query);
        if (TextUtils.isEmpty(query)) {
            return null;
        }
        query = query.toLowerCase();

//        return mDictionary.getWordMatches(query, columns);
        return mDictionary.getWordMatchesInLength(query, suggestionColumns);
    }

    private Cursor search(String query) {
        Log.d(TAG, "search:" + query);
        if (TextUtils.isEmpty(query)) {
            return null;
        }
        query = query.toLowerCase();
        return mDictionary.getWordMatches(query, wordColumns);
    }

    /**
     * get the word's index by word
     *
     * @param word word to query
     * @return word's index to return
     */
    private Cursor getWordIndex(String word) {
        Log.d(TAG, "getWord:" + word);
        if (TextUtils.isEmpty(word)) {
            return null;
        }
        word = word.toLowerCase();
        return mDictionary.getIndexByWord(word);
    }

    /**
     * get word's index by word's id
     *
     * @param uri
     * @return
     */
    private Cursor getWordIndex(Uri uri) {
        Log.d(TAG, "getWord:" + uri.toString());
        String rowId = uri.getLastPathSegment();
        return mDictionary.getIndexByRowId(rowId);
    }

    private Cursor refreshShortcut(Uri uri) {
        Log.d(TAG, "refreshShortcut:" + uri.toString());
        /* This won't be called with the current implementation, but if we include
       * {@link SearchManager#SUGGEST_COLUMN_SHORTCUT_ID} as a column in our suggestions table, we
       * could expect to receive refresh queries when a shortcutted suggestion is displayed in
       * Quick Search Box. In which case, this method will query the table for the specific
       * word, using the given item Uri and provide all the columns originally provided with the
       * suggestion query.
       */
        String rowId = uri.getLastPathSegment();
        String[] columns = new String[] {
                BaseColumns._ID,
                DictSQLiteDefine.KEY_WORD,
                SearchManager.SUGGEST_COLUMN_SHORTCUT_ID,
                SearchManager.SUGGEST_COLUMN_INTENT_DATA_ID};

        return mDictionary.getWord(rowId, columns);
    }

    @Override
    public Uri insert(Uri uri, ContentValues values) {
        throw new UnsupportedOperationException();
    }

    @Override
    public int delete(Uri uri, String selection, String[] selectionArgs) {
        throw new UnsupportedOperationException();
    }

    @Override
    public int update(Uri uri, ContentValues values, String selection, String[] selectionArgs) {
        throw new UnsupportedOperationException();
    }

    @Override
    public String getType(Uri uri) {
        switch (uriMatcher.match(uri)) {
            case SEARCH_WORDS:
                return WORDS_MIME_TYPE;
            case GET_WORD:
                return DEFINITION_MIME_TYPE;
            case SEARCH_SUGGEST:
                return SearchManager.SUGGEST_MIME_TYPE;
            case REFRESH_SHORTCUT:
                return SearchManager.SHORTCUT_MIME_TYPE;
            default:
                throw new IllegalArgumentException("Unknown URI " + uri);
        }
    }
}
