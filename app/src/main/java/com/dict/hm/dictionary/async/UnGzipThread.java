package com.dict.hm.dictionary.async;

import android.os.Handler;
import android.util.Log;

import com.dict.hm.dictionary.lib.CustomGZIPInputStream;
import com.dict.hm.dictionary.ui.DictManagerActivity;

import java.io.BufferedInputStream;
import java.io.BufferedOutputStream;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;

/**
 * Created by hm on 15-1-31.
 */
public class UnGzipThread extends Thread {
    File zip;
    File out;
    Handler handler;

    public UnGzipThread(File zip, File out, Handler handler) {
        this.zip = zip;
        this.out = out;
        this.handler = handler;
    }

    @Override
    public void run() {
        unzipFile(zip, out);
    }

    private boolean unzipFile(File gzipInputFile, File outputFile) {
        BufferedInputStream in = null;
        BufferedOutputStream out = null;
        CustomGZIPInputStream zin = null;
        int BUF_SIZE = 4096;
        try {
            in = new BufferedInputStream(new FileInputStream(gzipInputFile));
            out = new BufferedOutputStream(new FileOutputStream(outputFile));
            zin = new CustomGZIPInputStream(in);
            byte[] bytes = new byte[BUF_SIZE];
            int read;
//            int count = 0;
            Log.d("in", "" + in.available());
            while ((read = zin.read(bytes, 0, BUF_SIZE)) >= 0) {
                out.write(bytes, 0, read);
//                count += read;
//                Message message = handler.obtainMessage(BaseManagerActivity.PROCESSING);
//                message.arg1 = count;
//                handler.sendMessage(message);
            }
            handler.sendEmptyMessage(DictManagerActivity.DECOMPRESS);
        } catch (IOException e) {
            outputFile.delete();
            handler.sendEmptyMessage(DictManagerActivity.DECOMPRESS_ERR);
            e.printStackTrace();
            return false;
        } finally {
            try {
                if (in != null) {
                    in.close();
                }
                if (out != null) {
                    out.close();
                }
                if (zin != null) {
                    zin .close();
                }
            } catch (IOException e) {
                e.printStackTrace();
            }
        }
        return true;
    }
}

