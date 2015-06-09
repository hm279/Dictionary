package com.dict.hm.dictionary.ui.adapter;

import android.content.Context;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.BaseAdapter;
import android.widget.TextView;

import com.dict.hm.dictionary.R;

import java.util.ArrayList;

/**
 * Created by hm on 15-5-6.
 */
public class UserDictAdapter extends BaseAdapter {
    private LayoutInflater layoutInflater;
    private int count;
    ArrayList<String> words;
    ArrayList<Long> counts;
    ArrayList<String> times;

    public UserDictAdapter(Context context) {
        layoutInflater = (LayoutInflater) context.getSystemService(Context.LAYOUT_INFLATER_SERVICE);
        words = new ArrayList<>();
        counts = new ArrayList<>();
        times = new ArrayList<>();
        count = 0;
    }

    public void updateAdapterData(ArrayList<String> words, ArrayList<Long> counts,
                                  ArrayList<String> times) {
        this.words.addAll(words);
        this.counts.addAll(counts);
        this.times.addAll(times);
        count = this.words.size();
        notifyDataSetChanged();
    }

    public void clearAdapterData () {
        words.clear();
        counts.clear();
        times.clear();
        count = 0;
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
            view = layoutInflater.inflate(R.layout.user_word_item, parent, false);
        }
        TextView word = (TextView) view.findViewById(R.id.user_word);
        TextView count = (TextView) view.findViewById(R.id.user_count);
        TextView time = (TextView) view.findViewById(R.id.user_time);
        word.setText(words.get(position));
        count.setText(counts.get(position).toString());
        time.setText(times.get(position));

        return view;
    }

}
