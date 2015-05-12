package com.dict.hm.dictionary.paper;

import android.content.Context;
import android.os.Handler;
import android.os.HandlerThread;
import android.os.Looper;
import android.os.Message;

import com.dict.hm.dictionary.dict.MyDictSQLiteOpenHelper;

import java.lang.ref.WeakReference;
import java.util.HashMap;

/**
 * Created by hm on 15-5-5.
 * TODO:should add function that archive all words, not just the word in myDict;
 */
public class ArchiveWordHandler extends Handler {
    public static final int ALL = 0;
    public static final int FILTER = 1;
    public static final int MANUAL = 2;

    private static Looper sLooper = null;

    MyDictSQLiteOpenHelper helper;
    WorkerHandler handler;
    ArchiveWordListener listener;

    public interface ArchiveWordListener {
        void onArchiveComplete(int what, HashMap<String, Integer> left);
    }

    @Override
    public void handleMessage(Message msg) {
        switch (msg.what) {
            case ALL:
            case MANUAL:
                listener.onArchiveComplete(msg.what, null);
                break;
            case FILTER:
                listener.onArchiveComplete(FILTER, (HashMap<String, Integer>) msg.obj);
                break;
            default:
                super.handleMessage(msg);
        }
    }

    public ArchiveWordHandler(Context context, ArchiveWordListener listener) {
        super();
        this.listener = listener;
        synchronized (WorkerHandler.class) {
            if (sLooper == null) {
                HandlerThread thread = new HandlerThread("ArchiveWord");
                thread.start();
                sLooper = thread.getLooper();
            }
        }
        helper = MyDictSQLiteOpenHelper.getInstance(context);
        handler = new WorkerHandler(sLooper, this);
    }

    /**
     *
     */
    public void startArchive(HashMap<String, Integer> words, int type) {
        Message message = handler.obtainMessage(type);
        message.obj = words;
        message.sendToTarget();
    }

    public void quit() {
        if (sLooper != null) {
            sLooper.quit();
        }
    }

    private class WorkerHandler extends Handler {
        WeakReference<Handler> handlerWeakReference;

        public WorkerHandler(Looper looper, Handler handler) {
            super(looper);
            handlerWeakReference = new WeakReference<>(handler);
        }

        @Override
        public void handleMessage(Message msg) {
            switch (msg.what) {
                case ALL:
                    helper.insertWords((HashMap<String, Integer>) msg.obj);
                    replyMessage(msg.what, null);
                    break;
                case FILTER:
                    HashMap<String, Integer> left;
                    left = helper.filterWords((HashMap<String, Integer>) msg.obj);
                    replyMessage(msg.what, left);
                    break;
                case MANUAL:
                    helper.insertWords((HashMap<String, Integer>) msg.obj);
                    replyMessage(msg.what, null);
                    break;
                default:
            }
        }

        private void replyMessage(int what, HashMap<String, Integer> words) {
            Handler mUIHandler = handlerWeakReference.get();
            if (mUIHandler != null) {
                Message message = mUIHandler.obtainMessage(what);
                if (words != null) {
                    message.obj = words;
                }
                message.sendToTarget();
            }
        }
    }

}
