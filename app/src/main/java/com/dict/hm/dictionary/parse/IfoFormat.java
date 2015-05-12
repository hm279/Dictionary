package com.dict.hm.dictionary.parse;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileNotFoundException;
import java.io.FileReader;
import java.io.IOException;

/**
 * Created by hm on 15-1-22.
 */
public class IfoFormat {
    String version;
    String bookName;
    int wordCount;
    int idxFileSize;
    int sameTypeSequence;

    public IfoFormat(File ifo) {
        if (ifo.isFile()) {
            getIfoFormat(ifo);
        } else {
            bookName = "The file " + ifo.getName() + " not found";
        }
    }

    private void getIfoFormat(File ifo) {
        BufferedReader reader = null;
        try {
            reader = new BufferedReader(new FileReader(ifo));
            String line;
            String name;
            while ((line = reader.readLine()) != null) {
                int index = line.indexOf('=');
                if (index > 0) {
                    name = line.substring(index + 1);
                    name = name.trim();

                    if (line.contains("bookname")) {
                        bookName = name;
                    } else if (line.contains("wordcount")) {
                        wordCount = Integer.parseInt(name);
                    } else if (line.contains("idxfilesize")) {
                        idxFileSize = Integer.parseInt(name);
                    } else if (line.contains("version")) {
                        version = name;
                    } else if (line.contains("sametypesequence")) {
                        setSameTypeSequence(name);
                    }
                }
            }
        } catch (IOException e) {
            e.printStackTrace();
        } finally {
            if (reader != null) {
                try {
                    reader.close();
                } catch (IOException e) {
                    e.printStackTrace();
                }
            }
        }
    }

     /**
     * sameTypeSequence meaning:
     * 0x1 : 'm'
     * 0x2 : 'l'
     * 0x4 : 'g'
     * 0x8 : 't'
     * 0x10: 'y'
     * 0x20: 'W'
     * 0x40: 'P'
     * 0x80: 'X'
     */
    private void setSameTypeSequence(String name) {
        sameTypeSequence = 0;
        if (name.contains("m")) {
            sameTypeSequence |= 0x1;
        } else if (name.contains("l")) {
            sameTypeSequence |= 0x2;
        } else if (name.contains("g")) {
            sameTypeSequence |= 0x4;
        } else if (name.contains("t")) {
            sameTypeSequence |= 0x8;
        } else if (name.contains("y")) {
            sameTypeSequence |= 0x10;
        } else if (name.contains("W")) {
            sameTypeSequence |= 0x20;
        } else if (name.contains("P")) {
            sameTypeSequence |= 0x40;
        } else if (name.contains("X")) {
            sameTypeSequence |= 0x80;
        }
    }

    public String getVersion() {
        return version;
    }

    public String getBookName() {
        return bookName;
    }

    public int getWordCount() {
        return wordCount;
    }

    public int getIdxFileSize() {
        return idxFileSize;
    }

    public int getSameTypeSequence() {
        return sameTypeSequence;
    }

}
