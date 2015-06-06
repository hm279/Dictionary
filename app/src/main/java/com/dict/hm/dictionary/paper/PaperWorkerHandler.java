package com.dict.hm.dictionary.paper;

import android.content.Context;
import android.os.Handler;
import android.os.HandlerThread;
import android.os.Looper;
import android.os.Message;

import com.dict.hm.dictionary.dict.UserDictSQLiteHelper;

import java.io.File;
import java.lang.ref.WeakReference;
import java.util.ArrayList;

/**
 * Created by hm on 15-5-5.
 */
public class PaperWorkerHandler extends Handler {
    public static final int ALL = 0;
    public static final int FILTER = 1;
    public static final int MANUAL = 2;

    public static final int JSON_WRITE = 3;
    public static final int JSON_READ = 4;
    public static final int ARCHIVE = 5;
    public static final int PARSER = 6;

    private static Looper sLooper = null;

    UserDictSQLiteHelper helper;
    WorkerHandler workerHandler;
    WeakReference<PaperWorkerListener> listenerWeakReference;

    protected static final class ParseWorkerArgs {
        Handler handler; //hold the handler will ensure it can't be garbage collect.
        File in;
        File out;
        String url;
        String charset;
        int type;
        boolean filter;
        int error;
    }

    protected static final class ArchiveWorkerArgs {
        Handler handler;
        ArrayList<JsonEntry> words;
        File json;
        int wordCount;
        int type;
    }


    public interface PaperWorkerListener {
        void onArchiveComplete(int type, int count, ArrayList<JsonEntry> arrayList);
        void onJsonReadComplete(ArrayList<JsonEntry> arrayList);
        void onJsonWriteComplete(ArrayList<JsonEntry> arrayList);
        void onPaperParseComplete(File json, int error);
    }

    @Override
    public void handleMessage(Message msg) {
        PaperWorkerListener listener = listenerWeakReference.get();
        if (listener == null) {
            return;
        }
        switch (msg.what) {
            case ARCHIVE:
                ArchiveWorkerArgs args0 = (ArchiveWorkerArgs) msg.obj;
                listener.onArchiveComplete(args0.type, args0.wordCount, args0.words);
                break;
            case PARSER:
                ParseWorkerArgs args1 = (ParseWorkerArgs) msg.obj;
                listener.onPaperParseComplete(args1.out, args1.error);
                break;
            case JSON_READ:
                ArchiveWorkerArgs args2 = (ArchiveWorkerArgs) msg.obj;
                listener.onJsonReadComplete(args2.words);
                break;
            case JSON_WRITE:
                ArchiveWorkerArgs args3 = (ArchiveWorkerArgs) msg.obj;
                listener.onJsonWriteComplete(args3.words);
                break;
            default:
                super.handleMessage(msg);
        }
    }

    public PaperWorkerHandler(Context context, PaperWorkerListener listener) {
        super();
        listenerWeakReference = new WeakReference<>(listener);
        synchronized (WorkerHandler.class) {
            if (sLooper == null) {
                HandlerThread thread = new HandlerThread("ArchiveWord");
                thread.start();
                sLooper = thread.getLooper();
            }
        }
        helper = UserDictSQLiteHelper.getInstance(context);
        workerHandler = new WorkerHandler(sLooper);
    }

    /**
     * @param type must be one of the three value, ALL / FILTER / MANUAL
     */
    public void startArchive(ArrayList<JsonEntry> words, File json, int type) {
        ArchiveWorkerArgs args = new ArchiveWorkerArgs();
        args.handler = this;
        args.words = words;
        args.json = json;
        args.type = type;

        Message message = workerHandler.obtainMessage(ARCHIVE);
        message.obj = args;
        message.sendToTarget();
    }

    public void startJsonRead(File json) {
        ArchiveWorkerArgs args = new ArchiveWorkerArgs();
        args.handler = this;
        args.json = json;

        Message message = workerHandler.obtainMessage(JSON_READ);
        message.obj = args;
        message.sendToTarget();
    }

    public void startJsonWrite(File json, ArrayList<JsonEntry> words) {
        ArchiveWorkerArgs args = new ArchiveWorkerArgs();
        args.handler = this;
        args.json = json;
        args.words = words;

        Message message = workerHandler.obtainMessage(JSON_WRITE);
        message.obj = args;
        message.sendToTarget();
    }

    public void startParse(File in, File out, String charset, int type, boolean filter) {
        ParseWorkerArgs args = new ParseWorkerArgs();
        args.handler = this;
        args.in = in;
        args.out = out;
        args.charset = charset;
        args.type = type;
        args.filter = filter;

        Message message = workerHandler.obtainMessage(PARSER);
        message.obj = args;
        message.sendToTarget();
    }

    public void startParse(String url, File out, int type, boolean filter) {
        ParseWorkerArgs args = new ParseWorkerArgs();
        args.handler = this;
        args.url = url;
        args.out = out;
        args.type = type;
        args.filter = filter;

        Message message = workerHandler.obtainMessage(PARSER);
        message.obj = args;
        message.sendToTarget();
    }

    private class WorkerHandler extends Handler {
        public WorkerHandler(Looper looper) {
            super(looper);
        }

        @Override
        public void handleMessage(Message msg) {
            switch (msg.what) {
                case ARCHIVE:
                    archive((ArchiveWorkerArgs) msg.obj);
                    break;
                case PARSER:
                    parser((ParseWorkerArgs) msg.obj);
                    break;
                case JSON_READ:
                    ArchiveWorkerArgs args = (ArchiveWorkerArgs) msg.obj;
                    PaperJsonReader jsonReader = new PaperJsonReader(args.json);
                    args.words = jsonReader.readAll();

                    Message message = args.handler.obtainMessage(JSON_READ);
                    message.obj = args;
                    message.sendToTarget();
                    break;
                case JSON_WRITE:
                    ArchiveWorkerArgs args1 = (ArchiveWorkerArgs) msg.obj;
                    PaperJsonWriter jsonWriter = new PaperJsonWriter(args1.json);
                    if (!jsonWriter.storePaper(args1.words)) {
                        args1.words = null;
                    }

                    Message message1 = args1.handler.obtainMessage(JSON_WRITE);
                    message1.obj = args1;
                    message1.sendToTarget();
                    break;
            }
        }

        private void archive(ArchiveWorkerArgs args) {
            switch (args.type) {
                case ALL:
                    helper.insertWords(args.words);
                    args.wordCount = args.words.size();
                    if (args.json != null) {
                        args.json.delete();
                    }
                    break;
                case FILTER:
                    args.wordCount = args.words.size();
                    args.words = helper.filterWords(args.words);
                    args.wordCount = args.wordCount - args.words.size();
                    if (args.wordCount > 0) {
                        PaperJsonWriter jsonWriter = new PaperJsonWriter(args.json);
                        jsonWriter.storePaper(args.words);
                    }
                    break;
                case MANUAL:
                    helper.insertWords(args.words);
                    args.wordCount = args.words.size();
                    break;
            }
            Message message = args.handler.obtainMessage(ARCHIVE);
            message.obj = args;
            message.sendToTarget();
        }

        private void parser(ParseWorkerArgs args) {
            PaperParser parser = null;
            switch (args.type) {
                case PaperParser.HTML:
                    parser =  new PaperParser(args.in, args.charset, args.type);
                    break;
                case PaperParser.TXT:
                    parser =  new PaperParser(args.in, args.charset, args.type);
                    break;
                case PaperParser.URL:
                    parser = new PaperParser(args.url);
                    break;
            }
            if (parser != null) {
                ArrayList<JsonEntry> result = parser.parse();
                if (result == null) {
                    args.error = parser.getError();
                } else {
                    if (args.filter) {
                        result = helper.filterWords(result);
                    }
                    PaperJsonWriter jsonWriter = new PaperJsonWriter(args.out);
                    jsonWriter.storePaper(result);
                }
            } else {
                args.error = PaperErrorCode.ERR_GEN;
            }
            Message message = args.handler.obtainMessage(PARSER);
            message.obj = args;
            message.sendToTarget();
        }
    }

}
