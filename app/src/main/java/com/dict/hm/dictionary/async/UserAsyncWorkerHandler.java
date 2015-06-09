package com.dict.hm.dictionary.async;

import android.content.Context;
import android.database.Cursor;
import android.os.Handler;
import android.os.HandlerThread;
import android.os.Looper;
import android.os.Message;

import com.dict.hm.dictionary.dict.DictFormat;
import com.dict.hm.dictionary.dict.UserDictSQLiteHelper;

import java.io.File;
import java.lang.ref.WeakReference;
import java.util.ArrayList;

/**
 * Created by hm on 15-5-6.
 */
public class UserAsyncWorkerHandler extends Handler {
    private static final int EVENT_ARG_QUERY = 1;
    private static final int EVENT_ARG_INSERT = 2;
    private static final int EVENT_ARG_UPDATE = 3;
    private static final int EVENT_ARG_DELETE = 4;
    private static final int EVENT_USER_QUERY = 5;
    private static final int EVENT_PAPER_LIST = 6;

    private static Looper sLooper = null;
    private Handler mWorkerThreadHandler;

    WeakReference<UserDictQueryListener> listenerWeakReference;
    DictManagerCallback callback;
    UserDictSQLiteHelper helper;

    static UserAsyncWorkerHandler handler = null;

    protected static final class WorkerArgs {
        Handler handler;
        long rowid;
        Cursor result;
        DictFormat format;
        //paper list
        File dir;
        ArrayList<String> files;
    }

    protected class WorkerHandler extends Handler {

        public WorkerHandler(Looper looper) {
            super(looper);
        }

        @Override
        public void handleMessage(Message msg) {
            if (helper == null) {
                return;
            }
            WorkerArgs args = (WorkerArgs) msg.obj;
            Message message = args.handler.obtainMessage(msg.what);
            switch (msg.what) {
                case EVENT_ARG_QUERY:
                    args.result = helper.queryDictionaries();
                    break;
                case EVENT_ARG_INSERT:
                    args.rowid = helper.insertDictionary(args.format);
                    break;
                case EVENT_ARG_UPDATE:
                    args.rowid = helper.updateDictionary(args.rowid, msg.arg1);
                    break;
                case EVENT_ARG_DELETE:
                    args.rowid = helper.deleteDictionary(args.rowid);
                    break;
                case EVENT_USER_QUERY:
                    args.result = helper.getWordsOrderBy(msg.arg1);
                    break;
                case EVENT_PAPER_LIST:
                    if (args.dir.isDirectory()) {
                        String[] names = args.dir.list();
                        ArrayList<String> list = new ArrayList<>(names.length);
                        for (String name : args.dir.list()) {
                            list.add(name);
                        }
                        args.files = list;
                    } else {
                        args.files = null;
                    }
                    break;
                default:
                    return;
            }
            message.obj = args;
            message.sendToTarget();
        }
    }

    public static UserAsyncWorkerHandler getInstance(Context context, DictManagerCallback mCallback) {
        if (handler == null) {
            handler = new UserAsyncWorkerHandler(context.getApplicationContext(), mCallback);
        }
        return handler;
    }

    public UserAsyncWorkerHandler(Context context, DictManagerCallback callback) {
        synchronized (UserAsyncWorkerHandler.class) {
            if (sLooper == null) {
                HandlerThread thread = new HandlerThread("AsyncQueryWorker");
                thread.start();

                sLooper = thread.getLooper();
            }
        }
        mWorkerThreadHandler = new WorkerHandler(sLooper);
        helper = UserDictSQLiteHelper.getInstance(context);
        this.callback = callback;
    }

    public void setUserDictQueryListener(UserDictQueryListener listener) {
        listenerWeakReference = new WeakReference<>(listener);
    }

    @Override
    public void handleMessage(Message msg) {
        WorkerArgs args = (WorkerArgs) msg.obj;

        switch (msg.what) {
            case EVENT_ARG_QUERY:
                callback.onQueryComplete(args.result);
                break;
            case EVENT_ARG_INSERT:
                callback.onInsertComplete(args.rowid);
                break;
            case EVENT_ARG_UPDATE:
                callback.onUpdateComplete(args.rowid);
                break;
            case EVENT_ARG_DELETE:
                callback.onDeleteComplete(args.rowid);
                break;
            case EVENT_USER_QUERY:
                if (listenerWeakReference == null) {
                    return;
                }
                UserDictQueryListener listener = listenerWeakReference.get();
                if (listener == null) {
                    return;
                }
                listener.onUserDictQueryComplete(args.result);
                break;
            case EVENT_PAPER_LIST:
                callback.onUserPaperListComplete(args.files);
                break;
        }
    }

    /**
     * start to get a list names of papers
     */
    public void startList(File paperDir) {
        WorkerArgs args = new WorkerArgs();
        args.handler = this;
        args.dir = paperDir;
        Message message = mWorkerThreadHandler.obtainMessage(EVENT_PAPER_LIST);
        message.obj = args;
        message.sendToTarget();
    }


    public void startQuery() {
        WorkerArgs args = new WorkerArgs();
        args.handler = this;
        Message message = mWorkerThreadHandler.obtainMessage(EVENT_ARG_QUERY);
        message.obj = args;
        message.sendToTarget();
    }

    public void startInsert(DictFormat dictFormat) {
        WorkerArgs args = new WorkerArgs();
        args.handler = this;
        args.format = dictFormat;
        Message message = mWorkerThreadHandler.obtainMessage(EVENT_ARG_INSERT);
        message.obj = args;
        message.sendToTarget();
    }

    public void startUpdate(long rowid, int on) {
        WorkerArgs args = new WorkerArgs();
        args.handler = this;
        args.rowid = rowid;
        Message message = mWorkerThreadHandler.obtainMessage(EVENT_ARG_UPDATE);
        message.obj = args;
        message.arg1 = on;
        message.sendToTarget();
    }

    public void startDelete(long rowid) {
        WorkerArgs args = new WorkerArgs();
        args.handler = this;
        args.rowid = rowid;
        Message message = mWorkerThreadHandler.obtainMessage(EVENT_ARG_DELETE);
        message.obj = args;
        message.sendToTarget();
    }

    /**
     * get user dict's words
     * @param which
     */
    public void startQuery(int which) {
        WorkerArgs args = new WorkerArgs();
        args.handler = this;

        Message message = mWorkerThreadHandler.obtainMessage(EVENT_USER_QUERY);
        message .obj = args;
        message.arg1 = which;
        message.sendToTarget();
    }

    public interface UserDictQueryListener {
        void onUserDictQueryComplete(Cursor result);
    }

    public interface DictManagerCallback {
        void onQueryComplete(Cursor result);
        void onInsertComplete(long id);
        void onUpdateComplete(long id);
        void onDeleteComplete(long id);
        void onUserPaperListComplete(ArrayList<String> list);
    }
}
