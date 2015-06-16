package com.dict.hm.dictionary.ui;

import android.app.Fragment;
import android.os.Bundle;
import android.support.annotation.Nullable;
import android.support.v7.widget.LinearLayoutManager;
import android.support.v7.widget.RecyclerView;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;

import com.dict.hm.dictionary.R;
import com.dict.hm.dictionary.dict.DictItem;

import java.util.ArrayList;

/**
 * Created by hm on 15-6-16.
 */
public class DefinitionFragment extends Fragment{
    TextView textView;
    RecyclerView recyclerView;

    @Nullable
    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {
        View view = inflater.inflate(R.layout.fragment_definition, container, false);
        textView  = (TextView) view.findViewById(R.id.text_item);
        recyclerView = (RecyclerView) view.findViewById(R.id.f_recyclerView);
        return view;
    }

    public void setRecyclerViewAdapter(ArrayList<DictItem> data) {
        recyclerView.setLayoutManager(new LinearLayoutManager(getActivity()));
        recyclerView.setAdapter(new DefAdapter(data));
        recyclerView.addItemDecoration(
                new DividerItemDecoration(getActivity(), DividerItemDecoration.VERTICAL_LIST));
    }

    private static class DefViewHolder extends RecyclerView.ViewHolder {
        TextView textView;
        public DefViewHolder(View itemView) {
            super(itemView);
            textView  = (TextView) itemView.findViewById(R.id.text_item);
        }
    }

    private static class DefAdapter extends RecyclerView.Adapter<DefViewHolder> {
        ArrayList<DictItem> data;

        public DefAdapter(ArrayList<DictItem> data) {
            this.data = data;
        }

        @Override
        public DefViewHolder onCreateViewHolder(ViewGroup viewGroup, int i) {
            View view = LayoutInflater.from(viewGroup.getContext())
                    .inflate(R.layout.textview_item, viewGroup, false);
            return new DefViewHolder(view);
        }

        @Override
        public void onBindViewHolder(DefViewHolder defViewHolder, int i) {
            defViewHolder.textView.setText(data.get(i).toString());
        }

        @Override
        public int getItemCount() {
            return data.size();
        }
    }

}
