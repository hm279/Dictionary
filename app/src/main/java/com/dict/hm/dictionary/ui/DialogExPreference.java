package com.dict.hm.dictionary.ui;

import android.content.Context;
import android.preference.DialogPreference;
import android.util.AttributeSet;

import com.dict.hm.dictionary.R;
import com.dict.hm.dictionary.dict.DictManager;

/**
 * Created by hm on 15-6-5.
 */
public class DialogExPreference extends DialogPreference{
    String key_paper;
    String key_dict;
    public DialogExPreference(Context context, AttributeSet attrs) {
        super(context, attrs);
        setDialogTitle(null);
        key_paper = context.getString(R.string.action_clear_paper);
        key_dict = context.getString(R.string.action_clear_user_dict);
    }

    @Override
    protected void onDialogClosed(boolean positiveResult) {
        if (positiveResult) {
            DictManager manager = DictManager.getInstance(getContext());
            CharSequence key = getTitle();
            if (key.equals(key_dict)) {
                manager.clearUserDict();
            } else {
                manager.clearPaper();
            }
        }
    }
}
