package com.dict.hm.dictionary.paper;

/**
 * Created by hm on 15-5-24.
 */
public class JsonEntry {
    String word;
    long count;

    public JsonEntry(String word, int count) {
        this.word = word;
        this.count = count;
    }

    public String getWord() {
        return word;
    }

    public long getCount() {
        return count;
    }

    @Override
    public String toString() {
        return word;
    }
}
