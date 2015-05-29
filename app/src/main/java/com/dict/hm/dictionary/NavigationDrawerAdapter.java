package com.dict.hm.dictionary;

import android.content.Context;
import android.content.res.Resources;
import android.graphics.drawable.ClipDrawable;
import android.util.TypedValue;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.BaseAdapter;
import android.widget.TextView;

/**
 * Created by hm on 15-5-28.
 */
public class NavigationDrawerAdapter extends BaseAdapter{
    public static final int PERSONAL_DICT = 1;
    public static final int SWITCH_DICT = 2;
    public static final int LIST_PAPER = 3;
    public static final int MANAGE_DICT = 4;
    public static final int MANAGE_PAPER = 5;
    public static final int SETTINGS = 6;
    public static final int ABOUT = 7;

    private Context context;
    private LayoutInflater inflater;
    private Resources resources;
    private final int count = 12;

    public NavigationDrawerAdapter(Context context) {
        this.context = context;
        inflater = (LayoutInflater) context.getSystemService(Context.LAYOUT_INFLATER_SERVICE);
        resources = context.getResources();
    }

    @Override
    public View getView(int position, View convertView, ViewGroup parent) {
        View view;
        if (convertView == null) {
            view = inflater.inflate(R.layout.drawer_item, parent, false);
        } else {
            view = convertView;
        }
        String string;
        switch (position) {
            case 2:     //personal dict
                string = context.getString(R.string.drawer_personal_dict);
                break;
            case 3:     //switch dict
                string = context.getString(R.string.drawer_dict_switch);
                break;
            case 4:     //list paper
                string = context.getString(R.string.drawer_paper_list);
                break;
            case 6:     //manage dict
                string = context.getString(R.string.drawer_dict_manager);
                break;
            case 7:     //manage paper
                string = context.getString(R.string.drawer_paper_manager);
                break;
            case 9:     //settings
                string = context.getString(R.string.drawer_settings);
                break;
            case 10:     //about
                string = context.getString(R.string.drawer_about);
                break;
            case 0:     //image
            case 1:     //space
            case 5:     //divider
            case 8:     //divider
            case 11:    //space
                string = null;
                break;
            default:
                string = "";
        }
        TextView textView = (TextView) view.findViewById(R.id.drawer_text_test);
        textView.setText(string);

        view.setBackground(null);
        if (string == null) {
            if (position == 0) {
                view.getLayoutParams().height = (int) TypedValue.applyDimension(TypedValue.COMPLEX_UNIT_DIP,
                        152, resources.getDisplayMetrics());
                view.setBackgroundResource(R.drawable.ic_wallpaper);
            } else if (position == 1 || position == 11){
                view.getLayoutParams().height = (int) TypedValue.applyDimension(TypedValue.COMPLEX_UNIT_DIP,
                        8, resources.getDisplayMetrics());
            } else {
                view.getLayoutParams().height = (int) TypedValue.applyDimension(TypedValue.COMPLEX_UNIT_DIP,
                        16, resources.getDisplayMetrics());
                ClipDrawable drawable = (ClipDrawable) resources.getDrawable(R.drawable.drawer_divider);
                if (drawable != null) {
                    //set title divider height to 1dp, view's height is 16dp
                    int level = 10000 / 16;
                    drawable.setLevel(level);
                }
                view.setBackground(drawable);
            }
        }
        return view;
    }

    @Override
    public int getItemViewType(int position) {
        int type;
        switch (position) {
            case 2:     //personal dict
                type = PERSONAL_DICT;
                break;
            case 3:     //switch dict
                type = SWITCH_DICT;
                break;
            case 4:     //list paper
                type = LIST_PAPER;
                break;
            case 6:     //manage dict
                type = MANAGE_DICT;
                break;
            case 7:     //manage paper
                type = MANAGE_PAPER;
                break;
            case 9:     //settings
                type = SETTINGS;
                break;
            case 10:     //about
                type = ABOUT;
                break;
//            case 0:     //image
//            case 1:     //space
//            case 5:     //divider
//            case 8:     //divider
//            case 11:    //space
            default:
                type = 0;
        }
        return type;
    }

    @Override
    public int getCount() {
        return count;
    }

    @Override
    public Object getItem(int position) {
        return null;
    }

    @Override
    public long getItemId(int position) {
        return position;
    }

}
