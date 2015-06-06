package com.dict.hm.dictionary.ui.dialog;

import android.app.Activity;
import android.app.Dialog;
import android.app.DialogFragment;
import android.content.DialogInterface;
import android.os.Bundle;
import android.support.v7.app.AlertDialog;

/**
 * Created by hm on 15-1-20.
 */
public class AlertDialogFragment extends DialogFragment {
    private static final String TITLE = "title";
    private static final String POSITIVE = "positive";
    private static final String NEGATIVE = "negative";
    private static final String MSG = "msg";
    ConfirmDialogListener listener;

    public static AlertDialogFragment newInstance(String title, String msg,
                                                     String positive , String negative) {
        Bundle bundle = new Bundle();
        bundle.putString(TITLE, title);
        bundle.putString(MSG, msg);
        bundle.putString(POSITIVE, positive);
        bundle.putString(NEGATIVE, negative);
        AlertDialogFragment dialogFragment = new AlertDialogFragment();
        dialogFragment.setArguments(bundle);
        return dialogFragment;
    }

    public interface ConfirmDialogListener {
        void onDialogPositiveClick();
    }

    @Override
    public void onAttach(Activity activity) {
        super.onAttach(activity);
        try {
            listener = (ConfirmDialogListener) activity;
        } catch (ClassCastException e) {
            throw new ClassCastException(activity.toString()
                    + " must implement ConfirmDialogListener");
        }
    }

    @Override
    public Dialog onCreateDialog(Bundle savedInstanceState) {
        Bundle bundle = getArguments();
        String title = bundle.getString(TITLE);
        String message= bundle.getString(MSG);
        String positive = bundle.getString(POSITIVE);
        String negative = bundle.getString(NEGATIVE);

        AlertDialog.Builder builder = new AlertDialog.Builder(getActivity());
        builder.setTitle(title);
        builder.setMessage(message);
        builder.setPositiveButton(positive, new DialogInterface.OnClickListener() {
            @Override
            public void onClick(DialogInterface dialog, int which) {
                listener.onDialogPositiveClick();
            }
        });
        builder.setNegativeButton(negative, new DialogInterface.OnClickListener() {
            @Override
            public void onClick(DialogInterface dialog, int which) {
                dismiss();
            }
        });

        return builder.create();
    }

}
