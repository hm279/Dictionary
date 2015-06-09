package com.dict.hm.dictionary.ui.dialog;

import android.app.Activity;
import android.app.Dialog;
import android.app.DialogFragment;
import android.content.DialogInterface;
import android.os.Bundle;
import android.support.v7.app.AlertDialog;

import com.dict.hm.dictionary.R;

/**
 * Created by hm on 15-6-9.
 */
public class SelectDialog extends DialogFragment {
    OrderSelectListener listener;

    @Override
    public void onAttach(Activity activity) {
        try {
            listener = (OrderSelectListener) getTargetFragment();
        } catch (ClassCastException e) {
            throw new ClassCastException(getTargetFragment().toString()
                    + " must implement OrderSelectListener");
        }
        super.onAttach(activity);
    }

    @Override
    public Dialog onCreateDialog(Bundle savedInstanceState) {
        AlertDialog.Builder builder = new AlertDialog.Builder(getActivity());
        builder.setItems(R.array.order, new DialogInterface.OnClickListener() {
            @Override
            public void onClick(DialogInterface dialog, int which) {
                listener.onOrderSelectListener(which);
            }
        });
        return builder.create();
    }

    public interface OrderSelectListener {
        void onOrderSelectListener(int which);
    }

}
