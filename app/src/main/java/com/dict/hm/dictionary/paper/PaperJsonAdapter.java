package com.dict.hm.dictionary.paper;

import android.content.Context;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.BaseAdapter;
import android.widget.TextView;

import com.dict.hm.dictionary.R;


/**
 * Created by hm on 15-3-20.
 */
public class PaperJsonAdapter extends BaseAdapter{
    LayoutInflater inflater;
    PaperJsonReader reader;
    int size;

    public PaperJsonAdapter(Context context, PaperJsonReader reader) {
        inflater = (LayoutInflater) context.getSystemService(Context.LAYOUT_INFLATER_SERVICE);
        size = Integer.MAX_VALUE;
        this.reader = reader;
    }

    @Override
    public int getCount() {
        return size;
    }

    @Override
    public Object getItem(int position) {
        if (reader.size() > position) {
            return reader.getJsonKey(position);
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
            view = inflater.inflate(R.layout.textview_item, parent, false);
//            view.setBackgroundResource(R.drawable.list_selector);
        }
        TextView textView = (TextView) view.findViewById(R.id.text_item);
        String text = reader.getJsonKey(position);
        if (text != null) {
            textView.setText(text);
        } else {
            size = position;
            notifyDataSetChanged();
        }
        return view;
    }

    public void remove(int position) {
        reader.remove(position);
        if (size < Integer.MAX_VALUE) {
            size--;
        }
        notifyDataSetChanged();
    }

    public void cancelRemove() {
        int count = reader.restore();
        if (size < Integer.MAX_VALUE) {
            size += count;
        }
        notifyDataSetChanged();
    }

}
