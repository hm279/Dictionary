package com.dict.hm.dictionary.ui.dialog;

import android.app.Dialog;
import android.app.DialogFragment;
import android.graphics.drawable.Drawable;
import android.os.Bundle;
import android.support.v7.app.AlertDialog;
import android.view.LayoutInflater;
import android.view.View;
import android.widget.ImageView;
import android.widget.TextView;

import com.dict.hm.dictionary.R;
import com.dict.hm.dictionary.dict.UserDictSQLiteHelper;
import com.dict.hm.dictionary.paper.JsonEntry;

import java.util.ArrayList;

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
//        builder.setTitle(word);
        builder.setMessage(definition);
        builder.setCustomTitle(createCustomTitleView(word));
        return builder.create();
    }

    private View createCustomTitleView(final String word) {
        View view = LayoutInflater.from(getActivity()).inflate(R.layout.definition_item, null);
        TextView textView = (TextView) view.findViewById(R.id.word);
        textView.setText(word);
        final ImageView favorite= (ImageView) view.findViewById(R.id.favorite);
        favorite.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                Drawable drawable = getResources().getDrawable(R.drawable.ic_favorite_black_48dp);
                favorite.setImageDrawable(drawable);
                //TODO: store the word to my dictionary.
                UserDictSQLiteHelper helper = UserDictSQLiteHelper.getInstance(getActivity());
                ArrayList<JsonEntry> words = new ArrayList<>();
                JsonEntry entry = new JsonEntry(word, 1);
                words.add(entry);
                helper.insertWords(words);
            }
        });
        return view;
    }
}
