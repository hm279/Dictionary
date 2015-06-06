package com.dict.hm.dictionary.ui.dialog;

import android.app.Activity;
import android.app.Dialog;
import android.app.DialogFragment;
import android.content.DialogInterface;
import android.os.Bundle;
import android.support.v7.app.AlertDialog;

import com.dict.hm.dictionary.R;

/**
 * Created by hm on 15-5-28.
 */
public class SwitchDictDialog extends DialogFragment {
    private SwitchDictDialogListener listener;

    public static final String ARRAY_DATA = "data";
    public static final String CHECKED = "checked";

    public interface SwitchDictDialogListener {
        void onSwitchDictClick(int which);
    }

    @Override
    public void onAttach(Activity activity) {
        super.onAttach(activity);
        try {
            listener = (SwitchDictDialogListener) activity;
        } catch (ClassCastException e) {
            throw new ClassCastException(activity.toString()
                    + " must implement ConfirmDialogListener");
        }
    }

    @Override
    public Dialog onCreateDialog(Bundle savedInstanceState) {
        Bundle bundle = getArguments();
        CharSequence[] items = bundle.getCharSequenceArray(ARRAY_DATA);
        int position = bundle.getInt(CHECKED);

        AlertDialog.Builder builder = new AlertDialog.Builder(getActivity());
        builder.setTitle(R.string.drawer_dict_switch);
        builder.setSingleChoiceItems(items, position, new DialogInterface.OnClickListener() {
            @Override
            public void onClick(DialogInterface dialog, int which) {
                listener.onSwitchDictClick(which);
                dismiss();
            }
        });
        return builder.create();
    }

}
