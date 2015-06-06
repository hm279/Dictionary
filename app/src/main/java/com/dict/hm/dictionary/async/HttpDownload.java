package com.dict.hm.dictionary.async;

import android.os.AsyncTask;
import android.util.Log;

import com.dict.hm.dictionary.ui.BaseManagerActivity;

import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.net.HttpURLConnection;
import java.net.URL;

/**
 * Created by hm on 15-4-28.
 */
public class HttpDownload extends AsyncTask<String, Integer, Long> {
    BaseManagerActivity activity;

    public HttpDownload(BaseManagerActivity activity) {
        super();
        this.activity = activity;
        activity.initProgressDialog("Download dict file", 100);
    }

    @Override
    protected Long doInBackground(String... params) {
        HttpURLConnection connection = null;
        InputStream in = null;
        FileOutputStream out = null;
        long total = 0;
        if (params.length != 2) {
            return ((long) -1);
        }
        try {
            URL url = new URL(params[0]);
            connection = (HttpURLConnection) url.openConnection();
            connection.connect();
            if (connection.getResponseCode() != HttpURLConnection.HTTP_OK) {
               return ((long) -1);
            }
            int fileLength = connection.getContentLength();
            in = connection.getInputStream();
            out = new FileOutputStream(params[1]);
            byte[] bytes = new byte[4096];
            int read;
            while ((read = in.read(bytes)) != -1) {
                total += read;
                out.write(bytes, 0, read);
                if (fileLength > 0) {
                   publishProgress((int) total *100 / fileLength);
                }
            }
        } catch (IOException e) {
            e.printStackTrace();
        } finally {
            try {
                if (connection != null) {
                    connection.disconnect();
                }
                if (in != null) {
                    in.close();
                }
                if (out != null) {
                    out.close();
                }
            } catch (IOException e) {
                e.printStackTrace();
            }
        }

        return total;
    }

    @Override
    protected void onProgressUpdate(Integer... values) {
        Log.d("Download process", values[0] + "%");
        activity.setProgress(values[0]);
//        super.onProgressUpdate(values);
    }

    @Override
    protected void onPostExecute(Long aLong) {
        Log.d("Download success", "download size " + aLong);
        activity.dismissProgressDialog();
//        super.onPostExecute(aLong);
    }

}
