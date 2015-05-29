package com.dict.hm.dictionary.paper;

import android.content.Context;
import android.database.Cursor;
import android.net.Uri;
import android.os.SystemClock;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.view.animation.Animation;
import android.view.animation.AnimationUtils;
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
//    private Context context;
    private LayoutInflater inflater;
    private WordAsyncQueryHandler queryWord;
    private DictParser parser;
    private PaperJsonReader reader;
    private ArrayList<String> definitions;
    private Uri uri;

    private static final String error = "can't find word in the dictionary";
    private static final String error1 = "occur error while reading text from .dict file";

    private int count = 0;
    private int queryPosition = 0;
    private static final int preloadSize = 5;
    private boolean hasNext = true;

//    private int mark = 0;

    public PaperViewerAdapter(Context context, PaperJsonReader reader, DictParser parser) {
//        this.context = context;
        this.parser = parser;
        this.reader = reader;
        inflater = (LayoutInflater) context.getSystemService(Context.LAYOUT_INFLATER_SERVICE);
        definitions = new ArrayList<>();
        queryWord = new WordAsyncQueryHandler(context.getContentResolver(), this);
        uri = Uri.parse(DictContentProvider.CONTENT_URI + "/" + "word");
        /**
         * This can't set to Integer.MAX_VALUE, because the ListView which will setAdapter() to
         * this Adapter has a HeadView. The HeadView will be counted as one view. And I think the
         * max views is up to MAX_VALUE. So while I set 'size = Integer.MAX_VALUE - 1', the ListView
         * would show nothing.
         */
//        size = Integer.MAX_VALUE - 1;
        preload();
    }

    @Override
    public View getView(int position, View convertView, ViewGroup parent) {
//        long start, end;
//        start = SystemClock.currentThreadTimeMillis();

        View view;
        if (convertView != null) {
            view = convertView;
        } else {
            view = inflater.inflate(R.layout.paper_viewer_item, parent, false);
        }
        String word_text = reader.loadJsonKeyValue(position).getWord();
        String definition_text = definitions.get(position);

        TextView word_TextView = (TextView) view.findViewById(R.id.paper_word);
        TextView definition_TextView = (TextView) view.findViewById(R.id.paper_definition);
        word_TextView.setText(word_text);
        definition_TextView.setText(definition_text);

        if (hasNext && (queryPosition < position + preloadSize)) {
            preload();
        }
//        if (position == mark) {
//            Animation animation = AnimationUtils
//                    .loadAnimation(context, R.anim.abc_slide_in_bottom);
//            view.startAnimation(animation);
//        }

//        end = SystemClock.currentThreadTimeMillis();
//        Log.d("preload", "time-" + (end - start));

        return view;
    }

    @Override
    public int getCount() {
        return count;
    }

    @Override
    public Object getItem(int position) {
        if (position < reader.size()) {
            return reader.loadJsonKeyValue(position).getWord();
        }
        return null;
    }

    @Override
    public long getItemId(int position) {
        return position;
    }

    @Override
    public void onQueryComplete(int token, Object cookie, Cursor cursor) {
        updateWordDefinition(token, cookie, cursor);
    }

    private void preload() {
        int i = 0;
        while (i < preloadSize) {
            JsonEntry entry = reader.loadJsonKeyValue(queryPosition);
            if (entry != null) {
                queryWord.startQuery(queryPosition, null, uri, null, null,
                        new String[]{entry.getWord()}, null);
            } else {
                hasNext = false;
                break;
            }
            i++;
            queryPosition++;
        }
    }

    //TODO: need to be changed
    private void updateWordDefinition(int token, Object cookie, Cursor cursor) {
        int offset = -1;
        int size = -1;
        if (cursor != null) {
            try {
                cursor.moveToFirst();
                if (cursor.moveToFirst()) {
                    int i0 = cursor.getColumnIndex(DictSQLiteDefine.COLUMN_OFFSET);
                    int i1 = cursor.getColumnIndex(DictSQLiteDefine.COLUMN_SIZE);
                    offset = cursor.getInt(i0);
                    size = cursor.getInt(i1);
                }
            } finally {
                cursor.close();
            }
        }
        count++;
        definitions.add(getDefinition(offset, size));
        //done preload
        if (token + 1 == queryPosition) {
            notifyDataSetChanged();
        }
        if (token < preloadSize) {
            notifyDataSetChanged();
        }
//        mark = token;
    }

    private String getDefinition(int offset, int size) {
        if (offset < 0 || size < 0) {
           return error;
        }
        String definition = null;
        if (parser != null) {
            definition = parser.getWordDefinition(offset, size);
        }
        if (definition != null) {
            return definition;
        }
        return error1;
    }

}
