package com.dict.hm.dictionary.ui.adapter;

import android.content.Context;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.BaseAdapter;
import android.widget.TextView;

import com.dict.hm.dictionary.R;
import com.dict.hm.dictionary.paper.JsonEntry;

import java.util.ArrayList;


/**
 * Created by hm on 15-3-20.
 */
public class PaperJsonAdapter extends BaseAdapter{
    LayoutInflater inflater;
    ArrayList<JsonEntry> list;
    ArrayList<JsonEntry> removedList;

    public PaperJsonAdapter(Context context, ArrayList<JsonEntry> list) {
        inflater = (LayoutInflater) context.getSystemService(Context.LAYOUT_INFLATER_SERVICE);
        this.list= list;
        removedList = new ArrayList<>();
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

    public void removeItem(int position) {
        removedList.add(list.get(position));
        list.remove(position);
        notifyDataSetChanged();
    }

    public void setList(ArrayList<JsonEntry> list) {
        if (list == null) {
            return;
        }
        this.list = list;
        notifyDataSetChanged();
    }

    public ArrayList<JsonEntry> getList() {
        return list;
    }

    public ArrayList<JsonEntry> getRemovedList() {
        return removedList;
    }

    public void clearRemovedList() {
        removedList.clear();
    }

}
