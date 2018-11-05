package com.mobilerecognition.phonenumer.handler;

import java.util.concurrent.CountDownLatch;

import android.os.Handler;
import android.os.Looper;

import com.mobilerecognition.phonenumer.ui.ScanOldActivity;

/**
 * This thread does all the heavy lifting of decoding the images.
 */
final class RecogThread extends Thread {

    public static final String RECOG_BITMAP = "recog_bitmap";
    private final RecogListener recogListener;
    private Handler handler;
    private final CountDownLatch handlerInitLatch;

    RecogThread(RecogListener recogListener) {
        this.recogListener = recogListener;
        handlerInitLatch = new CountDownLatch(1);
    }

    Handler getHandler() {
        try {
            handlerInitLatch.await();
        } catch (InterruptedException ie) {
            // continue?
        }
        return handler;
    }

    @Override
    public void run() {
        Looper.prepare();
        handler = new RecogHandler(recogListener);
        handlerInitLatch.countDown();
        Looper.loop();
    }

}
