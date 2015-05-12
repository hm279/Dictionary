package com.dict.hm.dictionary.dict;

import android.content.Context;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.BaseAdapter;
import android.widget.TextView;

import com.dict.hm.dictionary.R;
import com.dict.hm.dictionary.async.MyDictAsyncQueryHandler;

import java.util.ArrayList;

/**
 * Created by hm on 15-5-6.
 */
public class MyDictAdapter extends BaseAdapter
        implements MyDictAsyncQueryHandler.MyDictQueryListener{
    private LayoutInflater layoutInflater;
    private int count;
    private MyDictAsyncQueryHandler queryHandler;
    ArrayList<String> words;
    ArrayList<Long> times;

    private static final int size = 10;
    boolean hasNext = true;
    private int queryPosition = size;

    public MyDictAdapter(Context context) {
        count = 0;
        layoutInflater = (LayoutInflater) context.getSystemService(Context.LAYOUT_INFLATER_SERVICE);
        queryHandler = new MyDictAsyncQueryHandler(MyDictSQLiteOpenHelper.getInstance(context), this);
        words = new ArrayList<>();
        times = new ArrayList<>();

        queryHandler.startQuery(size, 0);
    }

    @Override
    public void updateAdapterData(ArrayList<String> words, ArrayList<Long> times) {
        if (words.size() < size) {
            hasNext = false;
        }
        this.words.addAll(words);
        this.times.addAll(times);
        count = this.words.size();
        notifyDataSetChanged();
    }

    @Override
    public int getCount() {
        return count;
    }

    @Override
    public Object getItem(int position) {
        if (position < words.size()) {
            return words.get(position);
        }
        return null;
    }

    @Override
    public long getItemId(int position) {
        if (position < words.size()) {
            return position;
        }
        return 0;
    }

    @Override
    public View getView(int position, View convertView, ViewGroup parent) {
        View view;
        if (convertView != null) {
            view = convertView;
        } else {
            view = layoutInflater.inflate(R.layout.paper_viewer_item, parent, false);
        }
        TextView word = (TextView) view.findViewById(R.id.paper_word);
        TextView time = (TextView) view.findViewById(R.id.paper_definition);
        word.setText(words.get(position));
        time.setText(times.get(position).toString());
        /**
         * preload words, when to start nextQuery()?
         */
        if (hasNext && (queryPosition < position + size)) {
            queryHandler.nextQuery(size);
            queryPosition += size;
        }
        return view;
    }

}
