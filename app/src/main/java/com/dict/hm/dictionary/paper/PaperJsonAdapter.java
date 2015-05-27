package com.dict.hm.dictionary.paper;

import android.content.Context;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.BaseAdapter;
import android.widget.TextView;

import com.dict.hm.dictionary.R;

import java.util.ArrayList;


/**
 * Created by hm on 15-3-20.
 */
public class PaperJsonAdapter extends BaseAdapter{
    LayoutInflater inflater;
    ArrayList<JsonEntry> list;

    public PaperJsonAdapter(Context context, ArrayList<JsonEntry> list) {
        inflater = (LayoutInflater) context.getSystemService(Context.LAYOUT_INFLATER_SERVICE);
        this.list= list;
    }

    @Override
    public int getCount() {
        return list.size();
    }

    @Override
    public Object getItem(int position) {
        if (list.size() > position) {
            return list.get(position).getWord();
        } else {
            return null;
        }
    }

    @Override
    public long getItemId(int position) {
        return position;
    }

    @Override
    public View getView(int position, View convertView, ViewGroup parent) {
        View view;
        if (convertView != null) {
            view = convertView;
        } else {
            view = inflater.inflate(R.layout.paper_json_item, parent, false);
//            view.setBackgroundResource(R.drawable.list_selector);
        }
        TextView wordView = (TextView) view.findViewById(R.id.paper_json_word);
        TextView countView = (TextView) view.findViewById(R.id.paper_json_count);
        JsonEntry entry = list.get(position);
        wordView.setText(entry.getWord());
        countView.setText(Long.toString(entry.getCount()));
        return view;
    }

}
