package com.dict.hm.dictionary.async;

import android.content.AsyncQueryHandler;
import android.content.ContentResolver;
import android.database.Cursor;
import android.os.Handler;
import android.os.Looper;
import android.util.Log;

import java.lang.ref.WeakReference;

/**
 * Created by hm on 15-5-4.
 */
public class WordAsyncQueryHandler extends AsyncQueryHandler{
    WeakReference<AsyncQueryListener> listenerWeakReference;
    Handler worker = null;

    public interface AsyncQueryListener {
        void onQueryComplete(int token, Object cookie, Cursor cursor);
    }

    public WordAsyncQueryHandler(ContentResolver cr, AsyncQueryListener listener) {
        super(cr);
        listenerWeakReference = new WeakReference<>(listener);
    }

    @Override
    protected void onQueryComplete(int token, Object cookie, Cursor cursor) {
        final AsyncQueryListener listener = listenerWeakReference.get();
        if (listener != null) {
            listener.onQueryComplete(token, cookie, cursor);
        } else if (cursor != null) {
            cursor.close();
        }
    }

    @Override
    protected Handler createHandler(Looper looper) {
        worker = super.createHandler(looper);
        Log.d("thread id", "" + looper.getThread().getId());
        return worker;
    }

    //TODO:does need to quit the worker thread?
    public void quit() {
        if (worker != null) {
            worker.getLooper().quit();
        }
    }

}
