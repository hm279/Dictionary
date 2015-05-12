package com.dict.hm.dictionary.async;

import android.database.Cursor;
import android.os.Handler;
import android.os.HandlerThread;
import android.os.Looper;
import android.os.Message;
import android.util.Log;

import com.dict.hm.dictionary.dict.MyDictSQLiteOpenHelper;

import java.lang.ref.WeakReference;
import java.util.ArrayList;

/**
 * Created by hm on 15-5-6.
 */
public class MyDictAsyncQueryHandler extends Handler {

    ArrayList<String> words;
    ArrayList<Long> times;
    long lastID;

    private static Looper sLooper = null;
    private Handler mWorkerThreadHandler;
    WeakReference<MyDictQueryListener> listenerWeakReference;
    WeakReference<MyDictSQLiteOpenHelper> helperWeakReference;

    protected static final class WorkerArgs {
        long size;
        long offset;
        Handler handler;
        Cursor result;
    }

    protected class WorkerHandler extends Handler {

        public WorkerHandler(Looper looper) {
            super(looper);
        }

        @Override
        public void handleMessage(Message msg) {
            MyDictSQLiteOpenHelper helper = helperWeakReference.get();
            if (helper == null) {
                return;
            }
            WorkerArgs args = (WorkerArgs) msg.obj;
            args.result = helper.getWords(args.size, args.offset);

            Message message = args.handler.obtainMessage(0);
            message.obj = args;
            message.sendToTarget();
        }
    }

    public MyDictAsyncQueryHandler(MyDictSQLiteOpenHelper helper, MyDictQueryListener listener) {
        synchronized (MyDictAsyncQueryHandler.class) {
            if (sLooper == null) {
                HandlerThread thread = new HandlerThread("AsyncQueryWorker");
                thread.start();

                sLooper = thread.getLooper();
            }
        }
        helperWeakReference = new WeakReference<>(helper);
        listenerWeakReference = new WeakReference<>(listener);
        mWorkerThreadHandler = new WorkerHandler(sLooper);
        words = new ArrayList<>();
        times = new ArrayList<>();
    }

    @Override
    public void handleMessage(Message msg) {
        WorkerArgs args = (WorkerArgs) msg.obj;
        Cursor cursor = args.result;

        if (cursor != null && cursor.moveToFirst()) {
            int idIndex = cursor.getColumnIndex("rowid");
            int wordIndex = cursor.getColumnIndex(MyDictSQLiteOpenHelper.WORD);
            int timeIndex = cursor.getColumnIndex(MyDictSQLiteOpenHelper.TIME);
            do {
                words.add(cursor.getString(wordIndex));
                times.add(cursor.getLong(timeIndex));
//                Log.d("debug", cursor.getLong(idIndex) + "-" + cursor.getString(wordIndex) + "-" + cursor.getLong(timeIndex));
            } while (cursor.moveToNext());
            cursor.moveToLast();
            lastID = cursor.getLong(idIndex);
            Log.d("lastID", "" + lastID);
            cursor.close();
        }
        MyDictQueryListener listener = listenerWeakReference.get();
        if (listener != null) {
            listener.updateAdapterData(words, times);
        }
        words.clear();
        times.clear();
    }

    public void startQuery(long size, long offset) {
        WorkerArgs args = new WorkerArgs();
        args.size = size;
        args.offset = offset;
        args.handler = this;

        Message message = mWorkerThreadHandler.obtainMessage(0);
        message .obj = args;
        message.sendToTarget();
    }

    public void nextQuery(long size) {
        startQuery(size, lastID);
    }

    public void quit() {
        if (sLooper != null) {
            sLooper.quit();
        }
    }

    public interface MyDictQueryListener {
        void updateAdapterData(ArrayList<String> words, ArrayList<Long> times);
    }

}
