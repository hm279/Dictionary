package com.dict.hm.dictionary.ui;

import android.app.Activity;
import android.app.ListFragment;
import android.os.Bundle;

import com.dict.hm.dictionary.paper.PaperJsonReader;
import com.dict.hm.dictionary.ui.MainActivity;
import com.dict.hm.dictionary.dict.DictManager;
import com.dict.hm.dictionary.dict.parse.DictParser;
import com.dict.hm.dictionary.ui.adapter.PaperViewerAdapter;

import java.io.File;

/**
 * Created by hm on 15-5-28.
 */
public class PaperViewerFragment extends ListFragment{
    public static final String PAPER_NAME = "paper_name";
    DictParser dictParser;
    PaperJsonReader jsonReader;

    @Override
    public void onAttach(Activity activity) {
        super.onAttach(activity);
        dictParser = ((MainActivity)activity).getDictParser();
    }

    @Override
    public void onActivityCreated(Bundle savedInstanceState) {
        super.onActivityCreated(savedInstanceState);
        Bundle bundle = getArguments();
        String name = bundle.getString(PAPER_NAME);
        if (name != null) {
            File json = new File(DictManager.getInstance(getActivity()).getPaperDir(), name);
            jsonReader = new PaperJsonReader(json);
            jsonReader.openJson();
            PaperViewerAdapter adapter = new PaperViewerAdapter(getActivity(), jsonReader, dictParser);
            setListAdapter(adapter);
        } else {
            setEmptyText("Missing Paper");
            //setListShown to true will display empty text. Or it will display the ProgressBar.
            setListShown(true);
        }
    }

    @Override
    public void onDestroyView() {
        super.onDestroyView();
        if (jsonReader != null) {
            jsonReader.closeJson();
        }
    }
}
