package com.mobilerecognition.engine;

import android.graphics.Bitmap;
import android.graphics.Rect;

public class RecogEngine {

    public native int initEngine();

    public native int endEngine();

    public native int doRecogBitmap(Bitmap bitmap, int rot, int digit_count, int[] result);

    public native int doGetImg(byte[] data, int width, int height, int rot, int digit_count, int[] result);

    public RecogEngine() {
    }

//    public RecogResult RecogPhoneNumber_bitmap(Bitmap bitmap, int rot, Rect cropRect) {
//        RecogResult rst = null;
//
//        int[] intData = new int[100];
//        intData[0] = cropRect.left;
//        intData[1] = cropRect.top;
//        intData[2] = cropRect.right;
//        intData[3] = cropRect.bottom;
//
//        int ret = doRecogBitmap(bitmap, rot, intData);
//        if (ret > 0) {
//            rst = new RecogResult();
//            int i, k = 0;
//            int nPhoneNum = intData[k++];
//            int num = intData[k++];
//            if (num > 11) num = 11;
//            for (i = 0; i < num; ++i) {
//                rst.m_szNumber[i] = (char) intData[k++];
//                rst.m_diffPos[i] = 0;
//            }
//            rst.m_szNumber[i] = 0;
//            rst.m_nRecogDis = (double) intData[k++];
//            rst.m_nResultCount = 1;
//
//            if (nPhoneNum == 2) {
//                num = intData[k++];
//                if (num > 11) num = 11;
//                for (i = 0; i < num; ++i) {
//                    rst.m_szNumber1[i] = (char) intData[k++];
//                    if (rst.m_szNumber[i] != rst.m_szNumber1[i])
//                        rst.m_diffPos[i] = 1;
//                }
//                rst.m_szNumber1[i] = 0;
//                rst.m_nResultCount = 2;
//            }
//
//        }
//        return rst;
//    }

    public static String MakePhoneNumberTypeString(char[] szNumber) {
        String full = String.valueOf(szNumber);
        String typeStr = full.substring(0, 3) + "-" + full.substring(3, 7) + "-" + full.substring(7, 11);
        return typeStr;
    }

    public RecogResult RecogPhoneNumber_data(byte[] data, int width, int height, int rot, Rect cropRect) {
        return RecogPhoneNumber_data(data,width,height,rot,11,cropRect);
    }

    public RecogResult RecogPhoneNumber_data(byte[] data, int width, int height, int rot, int digit_count, Rect cropRect) {
        RecogResult rst = new RecogResult();

        int[] intData = new int[100];
        intData[0] = cropRect.left;
        intData[1] = cropRect.top;
        intData[2] = cropRect.right;
        intData[3] = cropRect.bottom;

        int ret = doGetImg(data, width, height, rot, digit_count, intData);

        if (ret > 0) {
            int i, k = 0;
            int num = intData[k++];
            if (num > 11) num = 11;
            for (i = 0; i < num; ++i) {
                rst.m_szNumber[i] = (char) intData[k++];
                rst.m_diffPos[i] = 0;
            }
            rst.m_szNumber[i] = 0;
            rst.m_nRecogDis = (double) intData[k++];

            String szNumber = MakePhoneNumberTypeString(rst.m_szNumber);
            rst.m_nResultCount = 2;
        } else {
            rst.m_nResultCount = 0;
        }

        return rst;
    }

    static {
        System.loadLibrary("RecogEngine");
    }
}
