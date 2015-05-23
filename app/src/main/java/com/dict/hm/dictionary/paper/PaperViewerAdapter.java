package com.dict.hm.dictionary.paper;

import android.content.Context;
import android.database.Cursor;
import android.net.Uri;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.BaseAdapter;
import android.widget.TextView;

import com.dict.hm.dictionary.async.WordAsyncQueryHandler;
import com.dict.hm.dictionary.dict.DictContentProvider;
import com.dict.hm.dictionary.dict.DictSQLiteDefine;
import com.dict.hm.dictionary.R;
import com.dict.hm.dictionary.parse.DictParser;

import java.util.ArrayList;

/**
 * Created by hm on 15-2-12.
 */
public class PaperViewerAdapter extends BaseAdapter
        implements WordAsyncQueryHandler.AsyncQueryListener{
    private LayoutInflater inflater;
    private WordAsyncQueryHandler queryWord;
    private DictParser parser;
    private PaperJsonReader reader;
    private ArrayList<Integer> offsetArray;
    private ArrayList<Integer> sizeArray;
    private int size;
    private int token;
    private static final String tip = "Querying word...";
    private static final String error = "can't find word in the dictionary";

    public PaperViewerAdapter(Context context, PaperJsonReader reader, DictParser parser) {
        this.parser = parser;
        inflater = (LayoutInflater) context.getSystemService(Context.LAYOUT_INFLATER_SERVICE);
        this.reader = reader;
        offsetArray = new ArrayList<>();
        sizeArray = new ArrayList<>();
        queryWord = new WordAsyncQueryHandler(context.getContentResolver(), this);
        /**
         * This can't set to Integer.MAX_VALUE, because the ListView which will setAdapter() to
         * this Adapter has a HeadView. The HeadView will be counted as one view. And I think the
         * max views is up to MAX_VALUE. So while I set 'size = Integer.MAX_VALUE - 1', the ListView
         * would show nothing.
         */
        size = Integer.MAX_VALUE - 1;
        token = -1;
    }

    @Override
    public View getView(int position, View convertView, ViewGroup parent) {
        View view;
        if (convertView != null) {
            view = convertView;
        } else {
            view = inflater.inflate(R.layout.paper_viewer_item, parent, false);
        }
        String word_text;
        String definition_text = null;
        word_text = reader.getJsonKey(position);
        if (position > token) {
            definition_text = getDefinition(word_text, position);
        }

        if (word_text != null) {
            if (position < offsetArray.size()) {
                if (offsetArray.get(position) < 0) {
                    definition_text = error;
                } else {
                    definition_text = getDefinition(offsetArray.get(position), sizeArray.get(position));
                }
            } else {
                definition_text = tip;
            }
        } else {
            size = position;
            notifyDataSetChanged();
        }
        TextView word_TextView = (TextView) view.findViewById(R.id.paper_word);
        TextView definition_TextView = (TextView) view.findViewById(R.id.paper_definition);
        word_TextView.setText(word_text);
        definition_TextView.setText(definition_text);

        return view;
    }

    @Override
    public int getCount() {
        return size;
    }

    @Override
    public Object getItem(int position) {
        if (position < reader.size()) {
            return reader.getJsonKey(position);
        }
        return null;
    }

    @Override
    public long getItemId(int position) {
        return position;
    }

    public void remove(int position) {
        if (position < offsetArray.size()) {
            offsetArray.remove(position);
            sizeArray.remove(position);
        }
        if (position <= token) {
            token--;
        }
    }

    private String getDefinition(String word, int position) {
        token = position;
        Uri uri = Uri.parse(DictContentProvider.CONTENT_URI + "/" + "word");
        queryWord.startQuery(position, word, uri, null, null, new String[]{word}, null);
        return tip;
    }

    @Override
    public void onQueryComplete(int token, Object cookie, Cursor cursor) {
        updateWordDefinition(token, cookie, cursor);
    }

    //TODO: need to be changed
    private void updateWordDefinition(int token, Object cookie, Cursor cursor) {
        int offset = -1;
        int size = -1;
        if (cursor != null) {
            try {
                cursor.moveToFirst();
                if (cursor.getCount() > 0) {
                    int i0 = cursor.getColumnIndex(DictSQLiteDefine.OFFSET);
                    int i1 = cursor.getColumnIndex(DictSQLiteDefine.SIZE);
                    offset = cursor.getInt(i0);
                    size = cursor.getInt(i1);
                }
            } finally {
                cursor.close();
            }
        }
        if (token < offsetArray.size()) {
            offsetArray.set(token, offset);
            sizeArray.set(token, size);
        } else {
            offsetArray.add(offset);
            sizeArray.add(size);
        }
        notifyDataSetChanged();
    }

    private String getDefinition(int offset, int size) {
        String definition = null;
        if (parser != null) {
            definition = parser.getWordDefinition(offset, size);
        }
        if (definition != null) {
            return definition;
        }
        return "occur error while reading text from .dict file";
    }

}
