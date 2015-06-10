package com.dict.hm.dictionary.ui.dialog;

import android.app.Dialog;
import android.app.DialogFragment;
import android.os.Bundle;
import android.support.v7.app.AlertDialog;

/**
 * Created by hm on 15-6-10.
 */
public class DefinitionDialog extends DialogFragment{
    private static final String WORD = "word";
    private static final String DEF = "def";

    public static DefinitionDialog getDefinitionDialog(String word, String def) {
        Bundle bundle = new Bundle();
        bundle.putString(WORD, word);
        bundle.putString(DEF, def);
        DefinitionDialog dialog = new DefinitionDialog();
        dialog.setArguments(bundle);
        return dialog;
    }

    @Override
    public Dialog onCreateDialog(Bundle savedInstanceState) {
        Bundle bundle = getArguments();
        String definition = bundle.getString(DEF);
        String word = bundle.getString(WORD);
        AlertDialog.Builder builder = new AlertDialog.Builder(getActivity());
        builder.setTitle(word);
        builder.setMessage(definition);
        return builder.create();
    }
}
