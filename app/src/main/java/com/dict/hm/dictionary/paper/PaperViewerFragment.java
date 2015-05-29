package com.dict.hm.dictionary.paper;

import android.app.Activity;
import android.app.ListFragment;
import android.os.Bundle;

import com.dict.hm.dictionary.MainActivity;
import com.dict.hm.dictionary.dict.DictManager;
import com.dict.hm.dictionary.parse.DictParser;

import java.io.File;

/**
 * Created by hm on 15-5-28.
 */
public class PaperViewerFragment extends ListFragment{
    public static final String PAPERNAME = "paper_name";
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
        File json = new File(DictManager.getInstance(getActivity()).getPaperDir(),
                bundle.getString(PAPERNAME));
        jsonReader = new PaperJsonReader(json);
        jsonReader.openJson();
        PaperViewerAdapter adapter = new PaperViewerAdapter(getActivity(), jsonReader, dictParser);
        setListAdapter(adapter);
    }

    @Override
    public void onDestroyView() {
        super.onDestroyView();
        if (jsonReader != null) {
            jsonReader.closeJson();
        }
    }
}
