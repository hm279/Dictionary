package com.dict.hm.dictionary.async;

import android.os.Handler;
import android.os.Message;

import com.dict.hm.dictionary.BaseManagerActivity;
import com.dict.hm.dictionary.DictManagerActivity;
import com.dict.hm.dictionary.dict.DictSQLiteOpenHelper;
import com.dict.hm.dictionary.parse.IdxParser;

import java.io.File;
import java.io.IOException;
import java.util.ArrayList;

/**
 * Created by hm on 15-5-12.
 */
public class LoadDictionary extends Thread{
    File idxFile;
    Handler handler;
    String bookName;
    DictSQLiteOpenHelper helper;

    public LoadDictionary(String bookName, File idxFile, DictSQLiteOpenHelper helper, Handler handler) {
        super();
        this.idxFile = idxFile;
        this.handler = handler;
        this.bookName = bookName;
        this.helper = helper;
    }

    @Override
    public void run() {
        try {
            helper.createTable(bookName);
            loadWords(idxFile);
        } catch (IOException e) {
            throw new RuntimeException(e);
        }
    }

    private void loadWords(File idxFile) throws IOException {
        IdxParser parser = new IdxParser(idxFile);
        ArrayList<IdxParser.WordEntry> list;
        int count = 0;

        while (true) {
            list = parser.getWordEntries();
            if (list == null || list.size() == 0) {
                break;
            }
            helper.addWords(list);
            count += list.size();
            Message message = handler.obtainMessage(DictManagerActivity.PROCESSING);
            message.arg1 = count;
            handler.sendMessage(message);
        }
        Message message = handler.obtainMessage(BaseManagerActivity.OK);
        message.obj = bookName;
        handler.sendMessage(message);
    }

}


