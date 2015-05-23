package com.dict.hm.dictionary;

import android.app.Fragment;
import android.graphics.drawable.Drawable;
import android.os.Bundle;
import android.support.annotation.Nullable;
import android.text.method.ScrollingMovementMethod;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageView;
import android.widget.TextView;

import com.dict.hm.dictionary.dict.UserDictSQLiteOpenHelper;

import java.util.HashMap;

/**
 * Created by hm on 15-3-6.
 */
public class DefinitionFragment extends Fragment {
    View view;
    public static final String WORD = "word";
    public static final String DEF = "def";
    TextView word_textView;
    TextView def_textView;
    ImageView favorite;

    @Nullable
    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {
        view = inflater.inflate(R.layout.word_definition_item, container, false);
        Bundle bundle = getArguments();
        final String word = bundle.getString(WORD);
        String definition = bundle.getString(DEF);
        word_textView = (TextView) view.findViewById(R.id.word);
        def_textView = (TextView) view.findViewById(R.id.definition);
        favorite = (ImageView) view.findViewById(R.id.favorite);
        def_textView.setMovementMethod(ScrollingMovementMethod.getInstance());
        favorite.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                Drawable drawable = getResources().getDrawable(R.drawable.ic_favorite_black_48dp);
                favorite.setImageDrawable(drawable);
                //TODO: store the word to my dictionary.
                UserDictSQLiteOpenHelper helper = UserDictSQLiteOpenHelper.getInstance(getActivity());
                HashMap<String, Integer> words = new HashMap<>();
                words.put(word, 1);
                helper.insertWords(words);
            }
        });

        updateViewData(word, definition);

        return view;
    }

    public void updateViewData(String word, String definition) {
        word_textView.setText(word);
        def_textView.setText(definition);
        def_textView.setScrollY(0);
    }

}
