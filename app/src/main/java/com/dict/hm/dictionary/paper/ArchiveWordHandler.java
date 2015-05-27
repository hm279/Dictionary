package com.dict.hm.dictionary.paper;

import android.content.Context;
import android.os.Handler;
import android.os.HandlerThread;
import android.os.Looper;
import android.os.Message;

import com.dict.hm.dictionary.dict.UserDictSQLiteOpenHelper;

import java.lang.ref.WeakReference;
import java.util.ArrayList;

/**
 * Created by hm on 15-5-5.
 * TODO:should add function that archive all words, not just the word in myDict;
 */
public class ArchiveWordHandler extends Handler {
    public static final int ALL = 0;
    public static final int FILTER = 1;
    public static final int MANUAL = 2;
    public static final int JSON_READ = 3;
    public static final int JSON_WRITE = 4;

    private static Looper sLooper = null;

    UserDictSQLiteOpenHelper helper;
    WorkerHandler handler;
    ArchiveWordListener listener;

    public interface ArchiveWordListener {
        void onArchiveComplete(int what, ArrayList<JsonEntry> left);
    }

    @Override
    public void handleMessage(Message msg) {
        listener.onArchiveComplete(msg.what, (ArrayList<JsonEntry>) msg.obj);
//        switch (msg.what) {
//            case ALL:
//            case MANUAL:
//                listener.onArchiveComplete(msg.what, null);
//                break;
//            case FILTER:
//                break;
//            case JSON_READ:
//                listener.onArchiveComplete(msg.what,null);
//            default:
//                super.handleMessage(msg);
//        }
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
        helper = UserDictSQLiteOpenHelper.getInstance(context);
        handler = new WorkerHandler(sLooper, this);
    }

    /**
     *
     */
    public void startArchive(ArrayList<JsonEntry> words, int type) {
        Message message = handler.obtainMessage(type);
        message.obj = words;
        message.sendToTarget();
    }

    public void startJsonRead(PaperJsonReader reader) {
        Message message = handler.obtainMessage(JSON_READ);
        message.obj = reader;
        message.sendToTarget();
    }

    public void startJsonWrite(PaperJsonReader reader) {
        Message message = handler.obtainMessage(JSON_WRITE);
        message.obj = reader;
        message.sendToTarget();
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
                    helper.insertWords((ArrayList<JsonEntry>) msg.obj);
                    replyMessage(msg.what, null);
                    break;
                case FILTER:
                    ArrayList<JsonEntry> left;
                    left = helper.filterWords((ArrayList<JsonEntry>) msg.obj);
                    replyMessage(msg.what, left);
                    break;
                case MANUAL:
                    helper.insertWords((ArrayList<JsonEntry>) msg.obj);
                    replyMessage(msg.what, null);
                    break;
                case JSON_READ:
                    PaperJsonReader reader = (PaperJsonReader) msg.obj;
                    reader.readAll();
                    replyMessage(msg.what, null);
                    break;
                case JSON_WRITE:
                    ((PaperJsonReader) msg.obj).updateJsonFile();
                    replyMessage(msg.what, null);
                    break;
            }
        }

        private void replyMessage(int what, ArrayList<JsonEntry> words) {
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
