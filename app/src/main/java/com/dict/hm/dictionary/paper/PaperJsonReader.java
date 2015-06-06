package com.dict.hm.dictionary.paper;

import android.util.JsonReader;
import android.util.JsonWriter;
import android.util.Log;

import java.io.File;
import java.io.FileNotFoundException;
import java.io.FileReader;
import java.io.FileWriter;
import java.io.IOException;
import java.util.ArrayList;

/**
 * Created by hm on 15-3-25.
 */
public class PaperJsonReader {
    JsonReader reader;

    public PaperJsonReader(File json) {
        try {
            reader = new JsonReader(new FileReader(json));
        } catch (FileNotFoundException e) {
            reader = null;
            e.printStackTrace();
        }
    }

    public void openJson() {
        try {
            reader.beginObject();
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    public JsonEntry getNextJsonEntry() {
        JsonEntry entry = null;
        if (reader != null) {
            try {
                if (reader.hasNext()) {
                    entry = new JsonEntry(reader.nextName(), reader.nextInt());
                }
            } catch (IOException e) {
                e.printStackTrace();
                Log.d("PaperJsonReader", "read json error");
            }
        }
        return entry;
    }

    public void closeJson() {
        try {
            reader.close();
            reader = null;
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    /**
     * when you call endObject() at last, and you call hasNext() again, it will return true,
     * mark down and test it if needed.
     */
    public ArrayList<JsonEntry> readAll() {
        ArrayList<JsonEntry> list = new ArrayList<>();
        if (reader != null) {
            try {
                reader.beginObject();
                while (reader.hasNext()) {
                    list.add(new JsonEntry(reader.nextName(), reader.nextInt()));
                }
                reader.endObject();
            } catch (IOException e) {
                e.printStackTrace();
                Log.d("PaperJsonReader", "read json error");
            } finally {
                try {
                    reader.close();
                    reader = null;
                } catch (IOException e) {
                    e.printStackTrace();
                }
            }
        }
        return list;
    }

}

