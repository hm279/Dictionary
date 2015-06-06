package com.dict.hm.dictionary.ui.dialog;

import android.app.Dialog;
import android.app.DialogFragment;
import android.os.Bundle;
import android.support.v7.app.AlertDialog;

import com.dict.hm.dictionary.R;

/**
 * Created by hm on 15-6-1.
 */
public class AboutDialog extends DialogFragment {

    @Override
    public Dialog onCreateDialog(Bundle savedInstanceState) {
        AlertDialog.Builder builder = new AlertDialog.Builder(getActivity());
        builder.setTitle(getString(R.string.about_title));
        builder.setMessage(getString(R.string.about_message));
        builder.setPositiveButton("OK", null);

        return builder.create();
    }
}
