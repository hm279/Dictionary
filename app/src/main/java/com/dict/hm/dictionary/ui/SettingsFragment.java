package com.dict.hm.dictionary.ui;

import android.os.Bundle;
import android.preference.PreferenceFragment;

import com.dict.hm.dictionary.R;

/**
 * Created by hm on 15-6-1.
 */
public class SettingsFragment extends PreferenceFragment {

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        addPreferencesFromResource(R.xml.preferences);
        getActivity().setTheme(R.style.SettingsPreferenceStyle);
    }


}
