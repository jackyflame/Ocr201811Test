package com.mobilerecognition.phonenumer.handler;

import android.graphics.Bitmap;
import android.os.Handler;

import com.mobilerecognition.engine.RecogResult;

/**
 * Created by admin on 2018/11/5.
 */

public interface RecogListener {

    void returnRecogedData(RecogResult result, Bitmap bmImage);

    void recogedFailed();

    Handler getHandler();

    boolean isAvailable();

    void setIsAvailable(boolean isAvailable);
}
