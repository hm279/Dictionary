package com.dict.hm.dictionary.dict;

/**
 * Created by hm on 15-5-20.
 */
public class DictFormat {
    /**
     * dictionary type
     */
    public static final int STAR_DICT = 0;

    String name;
    int type;
    int on;
    String data;
    String mTableName;

    public DictFormat(String name, int type, int on, String data, String mTableName) {
        this.name = name;
        this.type = type;
        this.on = on;
        this.data = data;
        this.mTableName = mTableName;
    }

    public void setOn(int on) {
        this.on = on;
    }

    public String getName() {
        return name;
    }

    public String getData() {
        return data;
    }

    public int getType() {
        return type;
    }

    @Override
    public String toString() {
        return name;
    }

    public int getOn() {
        return on;
    }
}
