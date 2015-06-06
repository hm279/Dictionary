package com.dict.hm.dictionary.paper;

import android.util.JsonWriter;
import android.util.Log;

import java.io.File;
import java.io.FileWriter;
import java.io.IOException;
import java.util.ArrayList;

/**
 * Created by hm on 15-6-3.
 */
public class PaperJsonWriter {
    File json;

    public PaperJsonWriter(File json) {
        this.json = json;
    }

    public boolean storePaper(ArrayList<JsonEntry> sortedList) {
        if (json == null || json.isDirectory() || sortedList == null) {
            return false;
        }
        JsonWriter jsonWriter = null;
        try {
            try {
                json.createNewFile();
            } catch (IOException e) {
                Log.d("error", "failed to create new file");
                return false;
            }
            jsonWriter = new JsonWriter(new FileWriter(json));
            jsonWriter.beginObject();
            for (int i = 0; i < sortedList.size(); i++) {
                JsonEntry entry = sortedList.get(i);
                jsonWriter.name(entry.getWord())
                        .value(entry.getCount());
            }
            jsonWriter.endObject();
            jsonWriter.flush();
        } catch (IOException e) {
            e.printStackTrace();
        } finally {
            try {
                if (jsonWriter != null) {
                    jsonWriter.close();
                }
            } catch (IOException e) {
                e.printStackTrace();
            }
        }
        return true;
    }

}
