package com.dict.hm.dictionary.dict.parse;

import android.util.Log;

import java.io.BufferedInputStream;
import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.util.ArrayList;
import java.util.zip.GZIPInputStream;

/**
 * Created by hm on 15-1-11.
 */
public class IdxParser {
    InputStream in;
    byte[] bytes = new byte[256];

    public IdxParser(File idxFile) {
        try {
            if (idxFile.getName().endsWith(".idx.gz")) {
                in = new GZIPInputStream(new BufferedInputStream(new FileInputStream(idxFile)));
            } else {
                in = new BufferedInputStream(new FileInputStream(idxFile));
            }

        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    /**
     * @return 10000 WordEntries
     *
     */
    public ArrayList<WordEntry> getWordEntries() {
        ArrayList<WordEntry> list = new ArrayList<>(10000);
        WordEntry entry;
        int i = 0;
        try {
            if (in == null) {
                return null;
            }
            if (in.available() <= 0) {
                in.close();
                return null;
            }
            Log.d("loading dictionary", "" + in.available());
        } catch (IOException e) {
            e.printStackTrace();
        }
        while ((entry = getNextWordEntry()) != null && i < 10000) {
            list.add(entry);
            i++;
        }
        return list;
    }

    private WordEntry getNextWordEntry() {
        WordEntry entry = new WordEntry();
        int i = 0;
        int[] tmp = new int[4];

        try {
            if (in.available() <= 0) {
                return null;
            }
            while ((bytes[i] = (byte)in.read()) != '\0') {
               i++;
            }
            byte[] word = new byte[i];
            for (int j = 0; j < i; j++) {
                word[j] = bytes[j];
            }
            entry.word = word;
            for (i = 0; i < 4; i++) {
                tmp[i] = in.read();
            }
            entry.offset = (tmp[0] << 24) + (tmp[1] << 16) + (tmp[2] << 8) + tmp[3];
            for (i = 0; i < 4; i++) {
                tmp[i] = in.read();
            }
            entry.size = (tmp[0] << 24) + (tmp[1] << 16) + (tmp[2] << 8) + tmp[3];
        } catch (IOException e) {
            e.printStackTrace();
        }
        return entry;
    }

    public class WordEntry {
        public byte[] word;
        public int offset;
        public int size;
    }
}
