package com.dict.hm.dictionary;

import android.app.AlertDialog;
import android.app.Dialog;
import android.app.DialogFragment;
import android.content.DialogInterface;
import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.Window;
import android.view.WindowManager;
import android.widget.ProgressBar;
import android.widget.Toast;


/**
 * Created by hm on 15-4-8.
 */
public class ProgressDialog extends DialogFragment{
    private static final String MAX = "max";
    private static final String MSG = "msg";
    ProgressBar progressBar;


    public static ProgressDialog newInstance(String msg, int max) {
        ProgressDialog dialogFragment = new ProgressDialog();
        Bundle bundle = new Bundle();
        bundle.putString(MSG, msg);
        bundle.putInt(MAX, max);
        dialogFragment.setArguments(bundle);

        return dialogFragment;
    }

    @Override
    public Dialog onCreateDialog(Bundle savedInstanceState) {
        Bundle bundle = getArguments();
        int max = bundle.getInt(MAX);
        String msg = bundle.getString(MSG);

        LayoutInflater inflater = getActivity().getLayoutInflater();
        View view;
        if (max > 0) {
            view = inflater.inflate(R.layout.progress, null);
            progressBar = (ProgressBar) view.findViewById(R.id.progressBar);
            progressBar.setMax(max);
        } else {
            view = inflater.inflate(R.layout.progress_circle, null);
            progressBar = (ProgressBar) view.findViewById(R.id.progressBar_circle);
        }

        AlertDialog.Builder builder = new AlertDialog.Builder(getActivity());
        builder.setView(view);
        builder.setMessage(msg);
        Dialog dialog = builder.create();

        dialog.getWindow().requestFeature(Window.FEATURE_NO_TITLE);
        dialog.setCanceledOnTouchOutside(false);
//        setCancelable(false);

        return dialog;
    }

    @Override
    public void onStart() {
        super.onStart();
        //set dialog's style
        Window window = getDialog().getWindow();
        WindowManager.LayoutParams layoutParams = window.getAttributes();
        layoutParams.dimAmount = 0.0f;
        layoutParams.flags |= WindowManager.LayoutParams.FLAG_DIM_BEHIND;
        window.setAttributes(layoutParams);
    }

    public void setProgressBar(int value) {
        progressBar.setProgress(value);
        if (value == progressBar.getMax()) {
            dismiss();
        }
    }

}
