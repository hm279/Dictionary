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
    File json;
    JsonReader reader;
    ArrayList<JsonEntry> list;
    ArrayList<JsonEntry> removedList;
    private boolean needWrite = false;

    public PaperJsonReader(File json) {
        this.json = json;
        try {
            reader = new JsonReader(new FileReader(json));
        } catch (FileNotFoundException e) {
            reader = null;
            e.printStackTrace();
        }
        list = new ArrayList<>();
        removedList = new ArrayList<>();
    }

    public JsonEntry getJsonKeyValue(int position) {
        if (list.size() > position) {
            return list.get(position);
        }
        return null;
    }

    public void openJson() {
        try {
            reader.beginObject();
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    public JsonEntry loadJsonKeyValue(int position) {
        if (list.size() > position) {
            return list.get(position);
        }
        JsonEntry entry = null;
        if (reader != null) {
            try {
                if (reader.hasNext()) {
                    entry = new JsonEntry(reader.nextName(), reader.nextInt());
                    list.add(entry);
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

    public int size() {
        return list.size();
    }

    public void remove(int position) {
        removedList.add(list.get(position));
        list.remove(position);
    }

    public ArrayList<JsonEntry> getRemovedList() {
        return removedList;
    }

    public void clearRemovedList() {
        //manually
        if (removedList.size() > 0) {
            removedList.clear();
            needWrite = true;
        }
    }

    public void setList(ArrayList<JsonEntry> left) {
        //filter
        if (left.size() < list.size()) {
            list.clear();
            list.addAll(left);
            needWrite = true;
        }
    }

    public void updateJsonFile() {
        //TODO: should auto delete the empty json file? if so, how user know that.
//        if (needWrite && (list.size() > 0)) {
        if (needWrite) {
            JsonWriter jsonWriter = null;
            try {
                jsonWriter = new JsonWriter(new FileWriter(json));
                jsonWriter.beginObject();
                for (JsonEntry entry : list) {
                    jsonWriter.name(entry.word);
                    jsonWriter.value(entry.count);
                }
                jsonWriter.endObject();
                jsonWriter.flush();
                needWrite = false;
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
        }
    }

}

