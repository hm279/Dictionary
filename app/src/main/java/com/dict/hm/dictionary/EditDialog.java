package com.dict.hm.dictionary;

import android.app.Activity;
import android.app.Dialog;
import android.app.DialogFragment;
import android.content.DialogInterface;
import android.os.Bundle;
import android.support.v7.app.AlertDialog;
import android.view.View;
import android.widget.EditText;

/**
 * Created by hm on 14-12-21.
 */
public class EditDialog extends DialogFragment {
    private static final String TEXT = "text";
    private static final String HINT = "hint";
    private static final String TITLE = "title";
    private static final String POSITIVE = "positive";
    EditDialogListener listener;

    static EditDialog newInstance(String title, String hint) {
        EditDialog dialogFragment = new EditDialog();
        Bundle bundle = new Bundle();
        bundle.putString(HINT, hint);
        bundle.putString(TITLE, title);
        dialogFragment.setArguments(bundle);

        return dialogFragment;
    }

    static EditDialog newInstance(String title, String editText, String positive) {
        EditDialog dialogFragment = new EditDialog();
        Bundle bundle = new Bundle();
        bundle.putString(TEXT, editText);
        bundle.putString(TITLE, title);
        bundle.putString(POSITIVE, positive);
        dialogFragment.setArguments(bundle);

        return dialogFragment;
    }

    public interface EditDialogListener{
        void onEditDialogPositiveClick(String url);
    }

    @Override
    public void onAttach(Activity activity) {
        super.onAttach(activity);
        try {
            listener = (EditDialogListener) activity;
        } catch (ClassCastException e) {
            throw new ClassCastException(getTargetFragment().toString()
                    + " must implement WarningDialogListener");
        }
    }

    @Override
    public Dialog onCreateDialog(Bundle savedInstanceState) {
        Bundle bundle = getArguments();
        String editText = bundle.getString(TEXT);
        String title = bundle.getString(TITLE);
        String hint = bundle.getString(HINT);
        String positive = bundle.getString(POSITIVE);
        if (positive == null) {
            positive = "Ok";
        }

        AlertDialog.Builder builder = new AlertDialog.Builder(getActivity());
        builder.setTitle(title);
        View view = getActivity().getLayoutInflater().inflate(R.layout.edit_text, null);
        builder.setView(view);
        EditText edit = (EditText)view.findViewById(R.id.editText);
        edit.setHint(hint);
        edit.setText(editText);

        builder.setPositiveButton(positive, new DialogInterface.OnClickListener() {
            @Override
            public void onClick(DialogInterface dialogInterface, int i) {
                EditText edit = (EditText)getDialog().findViewById(R.id.editText);
                listener.onEditDialogPositiveClick(edit.getText().toString());
            }
        });
        builder.setNegativeButton("Cancel", new DialogInterface.OnClickListener() {
            @Override
            public void onClick(DialogInterface dialogInterface, int i) {
                dismiss();
            }
        });

        return builder.create();
    }

}
