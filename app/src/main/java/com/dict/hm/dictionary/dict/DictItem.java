package com.dict.hm.dictionary.dict;

/**
 * Created by hm on 15-6-16.
 */
public class DictItem {
    Long id;
    String text;

    public DictItem(Long id, String text) {
        this.id = id;
        this.text = text;
    }

    @Override
    public String toString() {
        return text;
    }

    public Long getId() {
        return id;
    }
}
