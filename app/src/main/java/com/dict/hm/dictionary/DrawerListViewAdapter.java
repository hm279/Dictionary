package com.dict.hm.dictionary;

import android.content.Context;
import android.content.res.Resources;
import android.graphics.drawable.ClipDrawable;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.BaseAdapter;
import android.widget.CheckBox;
import android.widget.TextView;

import com.dict.hm.dictionary.dict.DictFormat;

import java.util.ArrayList;

/**
 * Created by hm on 15-1-29.
 */
public class DrawerListViewAdapter extends BaseAdapter{
    public static final int TYPE_ITEM_PAPER = 0;
    public static final int TYPE_ITEM_BOOK = 1;
    public static final int TYPE_TITLE = 2;
    ArrayList<DictFormat> books;
    ArrayList<String> papers;
    String title_paper;
    String title_dict;
    int title_color;
    Resources resources;

    private LayoutInflater inflater;

    public DrawerListViewAdapter(Context context) {
        inflater = (LayoutInflater) context.getSystemService(Context.LAYOUT_INFLATER_SERVICE);
        title_paper = context.getString(R.string.title_paper);
        title_dict = context.getString(R.string.title_dict);
        resources = context.getResources();
        title_color = resources.getColor(R.color.secondary_text_default_material_light);
        books = new ArrayList<>();
        papers = new ArrayList<>();
    }

    @Override
    public int getCount() {
        return books.size() + papers.size() + 2;
    }

    /**
     *
     * @param position
     * @return
     * listview item sequence :
     *              title_dict  TYPE_TITLE
     *              'books' TYPE_ITEM
     *              title_paper TYPE_TITLE
     *              'papers'    TYPE_ITEM
     */
    @Override
    public Object getItem(int position) {
        if (position == 0){
            return title_dict;
        } else if (position > 0 && position < books.size() + 1) {
            return books.get(position -1).getName();
        } else if (position == books.size() + 1) {
            return title_paper;
        } else if (position < 2 + books.size() + papers.size()){
            return papers.get(position - 2 - books.size());
        }
        return null;
    }

    @Override
    public long getItemId(int position) {
        if (position > 0 && position < books.size() + 1) {
            return position - 1;
        } else if (position > books.size() + 1){
            return position - books.size() - 2;
        }
        return 0;
    }

    @Override
    public int getItemViewType(int position) {
        if (position > 0 && position < books.size() + 1) {
            return TYPE_ITEM_BOOK;
        } else if (position > books.size() + 1){
            return TYPE_ITEM_PAPER;
        }
        return TYPE_TITLE;
    }

    @Override
    public int getViewTypeCount() {
        return TYPE_TITLE + 1;
    }

    @Override
    public View getView(int position, View convertView, ViewGroup parent) {
        View view;
        if (convertView != null) {
            view = convertView;
        } else {
            view = inflater.inflate(R.layout.drawer_listview_item, parent, false);
        }
        view.setBackground(null);
        TextView textView = (TextView) view.findViewById(R.id.drawer_text);
        textView.setText((String)getItem(position));
        CheckBox checkBox = (CheckBox) view.findViewById(R.id.checkbox);
        switch (getItemViewType(position)) {
            case TYPE_ITEM_BOOK:
                if (books.get((int)getItemId(position)).getOn() > 0) {
                    checkBox.setChecked(true);
                    checkBox.setVisibility(View.VISIBLE);
                } else {
                    checkBox.setChecked(false);
                    checkBox.setVisibility(View.INVISIBLE);
                }
                break;
            case TYPE_ITEM_PAPER:
                checkBox.setVisibility(View.GONE);
                break;
            case TYPE_TITLE:
//                textView.setTextSize(TypedValue.COMPLEX_UNIT_SP, 18);
                textView.setTextColor(title_color);
                checkBox.setVisibility(View.GONE);
                if (position > 0) {
                    ClipDrawable drawable =
                            (ClipDrawable) resources.getDrawable(R.drawable.drawer_divider);
                    if (drawable != null) {
                        //set title divider height to 1dp, suppose view's height is 56dp
                        int level = 10000 / 56;
                        drawable.setLevel(level);
                        view.setBackground(drawable);
                    }
                }
                break;
            default:
                break;
        }
        return view;
    }

//    public void setChecked(int position) {
//        if (getItemViewType(position) == TYPE_ITEM_BOOK) {
//            notifyDataSetChanged();
//        }
//    }

    public void setBookNames(ArrayList<DictFormat> data) {
        if (data != null) {
            books = data;
//        notifyDataSetChanged();
        }
    }

    public void setPapers(ArrayList<String> data) {
        if (data != null) {
            papers = data;
//        notifyDataSetChanged();
        }
    }

}
