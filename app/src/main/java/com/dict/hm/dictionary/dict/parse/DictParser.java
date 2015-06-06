package com.dict.hm.dictionary.dict.parse;

import android.util.Log;

import java.io.File;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.io.RandomAccessFile;

/**
 * Created by hm on 15-1-17.
 */
public class DictParser {
    File dictFile;
    RandomAccessFile randomAccessFile;

    public DictParser(File dictFile) {
        this.dictFile = dictFile;
        try {
            randomAccessFile = new RandomAccessFile(dictFile, "r");
        } catch (FileNotFoundException e) {
            randomAccessFile = null;
            e.printStackTrace();
        }
    }

    public String getWordDefinition(int offset, int size) {
        try {
            if (randomAccessFile != null && offset <= randomAccessFile.length()) {
                randomAccessFile.seek(offset);
                byte[] bytes = new byte[size];
                int actuallySize = randomAccessFile.read(bytes);
                if (actuallySize != size) {
                    return null;
                }
                return new String(bytes);
            }
        } catch (IOException e) {
            e.printStackTrace();
        }
        return null;
    }

    public void closeFile() {
        Log.d("RandomAccessFile", "closed");
        try {
            if (randomAccessFile != null) {
                randomAccessFile.close();
            }
        } catch (IOException e) {
            e.printStackTrace();
        }
    }
}
