package com.dict.hm.dictionary.ui;

import android.app.Activity;
import android.app.Fragment;
import android.content.Context;
import android.os.Bundle;
import android.os.Environment;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.AdapterView;
import android.widget.ArrayAdapter;
import android.widget.ListView;
import android.widget.TextView;

import com.dict.hm.dictionary.R;

import java.io.File;
import java.util.ArrayList;
import java.util.Collections;

/**
 * Created by hm on 15-1-7.
 *
 * Help to load file like *.ifo, *.txt and so on.
 * load Dictionary *.ifo
 * load text file, which contains your own words, like top 5000 words.
 */
public class FileListFragment extends Fragment {
    public static final String FILEPATH = "FileListFragment";
    private File curDir = null;
    private Context context;
    private ListView listView;
    private TextView emptyText;
    private FileSelectedListener listener;

    public interface FileSelectedListener {
        void onFileSelectedListener(File file);
    }
    @Override
    public void onAttach(Activity activity) {
        super.onAttach(activity);
        context = activity;
        try {
            listener = (FileSelectedListener) activity;
        } catch (ClassCastException e) {
            throw new ClassCastException(activity.toString()
                    + " must implement interface FileSelectedListener");
        }
    }

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {
        View view = inflater.inflate(R.layout.fragment_file, container, false);
        listView = (ListView) view.findViewById(R.id.file_listView);
        emptyText = (TextView) view.findViewById(R.id.file_emptyView);

        return view;
    }

    @Override
    public void onActivityCreated(Bundle savedInstanceState) {
        Log.d("FileListFragment", "onActivityCreated");
        super.onCreate(savedInstanceState);
        emptyText.setText("No File");
        listView.setOnItemClickListener(fileListViewListener);
        listView.setEmptyView(emptyText);

        Bundle bundle = getArguments();
        File rootDir;
        if (bundle != null) {
            String path = bundle.getString(FILEPATH);
            rootDir = new File(path);
        } else {
            rootDir = Environment.getExternalStorageDirectory();
        }

        if (rootDir.isDirectory()) {
            length = rootDir.getAbsolutePath().length();
            onDictionarySelected(rootDir);
        }
//        getActivity().setTitle("Select File");
    }

    int length;
    public boolean onBackPressed() {
        Log.d("FileListFragment", "onBackPressed");
        if(curDir != null) {
            if (curDir.getAbsolutePath().length() > length) {
                onDictionarySelected(curDir.getParentFile());
                return true;
            }
        }
        return false;
    }

    AdapterView.OnItemClickListener fileListViewListener = new AdapterView.OnItemClickListener(){
        @Override
        public void onItemClick(AdapterView<?> parent, View view, int position, long id) {
            String fileName = (String) parent.getItemAtPosition(position);
            File file = new File(curDir, fileName);
            onDictionarySelected(file);
        }
    };

    private void onDictionarySelected(File file) {
        Log.d(FILEPATH, file.getPath());
        if (file.isFile()) {
            listener.onFileSelectedListener(file);
            return;
        }
        String[] names = file.list();
        if (names == null) {
            return;
        }

        ArrayList<String> list = new ArrayList<>();
        for (String name : names) {
            list.add(name);
        }
        Collections.sort(list);
        curDir = file;
        ArrayAdapter<String> adapter = new ArrayAdapter<>(context, R.layout.textview_item, list);
        listView.setAdapter(adapter);
    }
}
