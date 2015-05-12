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
import java.util.HashMap;

/**
 * Created by hm on 15-3-25.
 * //TODO:Think about a better design.
 */
public class PaperJsonReader {
    private final Object mLock = new Object();
    File json;
    JsonReader reader;
    ArrayList<String> keyList;
    ArrayList<Integer> valueList;
    HashMap<String, Integer> removedList;

    ArrayList<String> keyListBackup;
    ArrayList<Integer> valueListBackup;

    public PaperJsonReader(File json) {
        this.json = json;
        try {
            reader = new JsonReader(new FileReader(json));
            reader.beginObject();
        } catch (FileNotFoundException e) {
            reader = null;
            e.printStackTrace();
        } catch (IOException e) {
            e.printStackTrace();
        }
        valueList = new ArrayList<>();
        keyList = new ArrayList<>();
        removedList = new HashMap<>();

        keyListBackup = new ArrayList<>();
        valueListBackup = new ArrayList<>();
    }

    public String getJsonKey(int position) {
        if (reader == null) {
            return null;
        }
        if (keyList.size() > position) {
            return keyList.get(position);
        } else {
            String text = null;
            try {
                if (reader.hasNext()) {
                    text = reader.nextName();
                    int value = reader.nextInt();
                    add(text, value);
                } else {
                    reader.endObject();
                }
            } catch (IOException e) {
                Log.d("PaperJsonAdapter", "read json error");
            }
            return text;
        }
    }

    public int getJsonValue(int position) {
        if (valueList.size() > position) {
            return valueList.get(position);
        }
        return 0;
    }

    public int size() {
        return keyList.size();
    }

    private void add(String key, int value) {
        synchronized (mLock) {
            keyList.add(key);
            valueList.add(value);

            keyListBackup.add(key);
            valueListBackup.add(value);
        }
    }

    public void remove(int position) {
        synchronized (mLock) {
            removedList.put(keyList.get(position), valueList.get(position));
            keyList.remove(position);
            valueList.remove(position);
        }
    }

    public int restore() {
        int size = removedList.size();
        if (size > 0) {
            keyList.clear();
            valueList.clear();
            removedList.clear();
            keyList.addAll(keyListBackup);
            valueList.addAll(valueListBackup);
        }
        return size;
    }

    public HashMap<String, Integer> getRemovedList() {
        return removedList;
    }

    public void updateJsonFile(HashMap<String, Integer> left) {
        synchronized (mLock) {
            if (left.size() > 0) {
                //TODO: update json file
                close();
                JsonWriter jsonWriter = null;
                try {
                    jsonWriter = new JsonWriter(new FileWriter(json));
                    jsonWriter.beginObject();
                    for (HashMap.Entry<String, Integer> entry : left.entrySet()) {
                        jsonWriter.name(entry.getKey())
                                .value(entry.getValue());
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
            }
            keyList.clear();
            valueList.clear();
            removedList.clear();
        }
    }

    public void close() {
        try {
            if (reader != null) {
                reader.close();
            }
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

}

