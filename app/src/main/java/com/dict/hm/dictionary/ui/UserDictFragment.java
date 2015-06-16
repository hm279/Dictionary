package com.dict.hm.dictionary.ui;

import android.app.ListFragment;
import android.content.ContentResolver;
import android.database.Cursor;
import android.net.Uri;
import android.os.Bundle;
import android.view.Menu;
import android.view.MenuInflater;
import android.view.MenuItem;
import android.view.View;
import android.widget.ListView;

import com.dict.hm.dictionary.R;
import com.dict.hm.dictionary.async.UserAsyncWorkerHandler;
import com.dict.hm.dictionary.dict.DictContentProvider;
import com.dict.hm.dictionary.dict.DictManager;
import com.dict.hm.dictionary.dict.DictSQLiteDefine;
import com.dict.hm.dictionary.dict.UserDictSQLiteHelper;
import com.dict.hm.dictionary.dict.parse.DictParser;
import com.dict.hm.dictionary.ui.adapter.UserDictAdapter;
import com.dict.hm.dictionary.ui.dialog.DefinitionDialog;
import com.dict.hm.dictionary.ui.dialog.SelectDialog;

import java.util.ArrayList;

/**
 * Created by hm on 15-6-9.
 */
public class UserDictFragment extends ListFragment
        implements UserAsyncWorkerHandler.UserDictQueryListener,
        SelectDialog.OrderSelectListener {
    public static final int ORDER_COUNT = 0;
    public static final int ORDER_TIME = 1;
    public static final int ORDER_OTHER = 2;
    public static final int size = 100;
    private UserDictAdapter userDictAdapter = null;
    private UserAsyncWorkerHandler userHandler;
    private Uri uri;
    private DictManager manager;

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setHasOptionsMenu(true);
    }

    @Override
    public void onActivityCreated(Bundle savedInstanceState) {
        super.onActivityCreated(savedInstanceState);

        userHandler = UserAsyncWorkerHandler.getInstance(getActivity(), null);
        userHandler.setUserDictQueryListener(this);
        userHandler.startQuery(ORDER_OTHER);

        userDictAdapter = new UserDictAdapter(getActivity());
        setEmptyText(getResources().getString(R.string.action_user_dict));
        setListAdapter(userDictAdapter);
        setListShown(true);

        manager = DictManager.getInstance(getActivity());
        uri = Uri.withAppendedPath(DictContentProvider.CONTENT_URI, "word");
    }

    @Override
    public void onCreateOptionsMenu(Menu menu, MenuInflater inflater) {
        inflater.inflate(R.menu.menu_user_dict, menu);
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        if (item.getItemId() == R.id.order) {
            SelectDialog dialog = new SelectDialog();
            dialog.setTargetFragment(this, 0);
            dialog.show(getFragmentManager(), null);
            return true;
        } else {
            return super.onOptionsItemSelected(item);
        }
    }

    @Override
    public void onListItemClick(ListView l, View v, int position, long id) {
        DictParser dictParser = manager.getDictParser();
        if (dictParser == null) {
            return;
        }
        String word = (String) l.getItemAtPosition(position);
        ContentResolver contentResolver = getActivity().getContentResolver();
        Cursor cursor = contentResolver.query(uri, null, null, new String[]{word}, null);
        if (cursor != null) {
            if (cursor.moveToFirst()) {
                int offset = cursor.getInt(cursor.getColumnIndex(DictSQLiteDefine.COLUMN_OFFSET));
                int size = cursor.getInt(cursor.getColumnIndex(DictSQLiteDefine.COLUMN_SIZE));
                String definition = dictParser.getWordDefinition(offset, size);
                DefinitionDialog.getDefinitionDialog(word, definition).show(getFragmentManager(), null);
            }
            cursor.close();
        }
    }

    /** -----------------------------------------------------------------------------------------*/
    //TODO: remove the lastID, sort the words by count or time. get all the words in one time.
    @Override
    public void onUserDictQueryComplete(Cursor cursor) {
        ArrayList<String> words = new ArrayList<>();
        ArrayList<Long> counts = new ArrayList<>();
        ArrayList<String> times = new ArrayList<>();

        if (cursor != null) {
            if (cursor.moveToFirst()) {
                int wordIndex = cursor.getColumnIndex(UserDictSQLiteHelper.COLUMN_WORD);
                int countIndex = cursor.getColumnIndex(UserDictSQLiteHelper.COLUMN_COUNT);
                int timeIndex = cursor.getColumnIndex(UserDictSQLiteHelper.COLUMN_TIME);
                do {
                    words.add(cursor.getString(wordIndex));
                    counts.add(cursor.getLong(countIndex));
                    times.add(cursor.getString(timeIndex));
                } while (cursor.moveToNext());
                cursor.moveToLast();
            }
            cursor.close();
        }
        userDictAdapter.updateAdapterData(words, counts, times);
        setListShown(true);
    }

    @Override
    public void onOrderSelectListener(int which) {
        userDictAdapter.clearAdapterData();
        userHandler.startQuery(which);
        setListShown(false);
    }
}
