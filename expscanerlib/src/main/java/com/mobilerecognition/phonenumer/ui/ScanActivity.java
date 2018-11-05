/**
 * Project Name:IDCardScanCaller
 * File Name:ScanActivity.java
 * Package Name:com.intsig.idcardscancaller
 * Date:2016年3月15日下午2:14:46
 * Copyright (c) 2016, 上海合合信息 All Rights Reserved.
 */

package com.mobilerecognition.phonenumer.ui;

import android.app.AlertDialog;
import android.content.DialogInterface;
import android.graphics.Bitmap;
import android.hardware.Camera;
import android.hardware.Camera.CameraInfo;
import android.hardware.Camera.Size;
import android.os.Bundle;
import android.os.Handler;
import android.os.Message;
import android.os.PowerManager;
import android.support.v7.app.AppCompatActivity;
import android.text.TextUtils;
import android.util.Log;
import android.view.MotionEvent;
import android.view.Surface;
import android.view.View;
import android.view.WindowManager;
import android.widget.ImageView;
import android.widget.Toast;

import com.mobilerecognition.engine.RecogEngine;
import com.mobilerecognition.engine.RecogResult;
import com.mobilerecognition.phonenumer.R;
import com.mobilerecognition.phonenumer.general.CGlobal;
import com.mobilerecognition.phonenumer.handler.ScanHandler;
import com.mobilerecognition.phonenumer.utils.CameraSetting;
import com.mobilerecognition.phonenumer.utils.SoundClips;

import java.lang.ref.WeakReference;
import java.util.List;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * ClassName:ScanActivity <br/>
 * Function: TODO ADD FUNCTION. <br/>
 * Reason: TODO ADD REASON. <br/>
 */
public abstract class ScanActivity extends AppCompatActivity implements Camera.PreviewCallback, Camera.AutoFocusCallback, View.OnClickListener {

    private static final String TAG = "ScanActivity";
    private static final int MSG_AUTO_FOCUS = 100;
    private static final int MSG_RESCAN = 200;

    private Preview mPreview = null;
    private Camera mCamera = null;
    private int numberOfCameras;
    private String lastRecgResultString = null;
    private boolean mNeedInitCameraInResume = false;
    private SoundClips.Player mSoundPlayer;
    private ImageView iv_camera_back;
    private ImageView iv_camera_flash;

    // The first rear facing camera
    private int defaultCameraId;

    private boolean isFlashOn = false;
    private long lastResultTime;
    private long lastSuccessTime;

    public ScanHandler m_scanHandler;
    PowerManager.WakeLock wakeLock;
    public boolean bIsAvailable;

    private Handler mHandler = new MsgHandler(this);

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        //保持屏幕常亮
        getWindow().addFlags(WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON);
        //记录打开时间
        lastSuccessTime =  System.currentTimeMillis();

        initContentView();

        mPreview = (Preview) findViewById(R.id.preview_scan);
        mPreview.setDetectView((DetectView) findViewById(R.id.detect_scan));
        iv_camera_back = (ImageView) this.findViewById(R.id.iv_camera_back);
        iv_camera_back.setOnClickListener(this);
        iv_camera_flash = (ImageView) this.findViewById(R.id.iv_camera_flash);
        iv_camera_flash.setOnClickListener(this);

        //// 隐藏当前Activity界面的导航栏, 隐藏后,点击屏幕又会显示出来.
        //View decorView = getWindow().getDecorView();
        //int uiOptions = View.SYSTEM_UI_FLAG_LAYOUT_STABLE
        //        | View.SYSTEM_UI_FLAG_LAYOUT_HIDE_NAVIGATION
        //        | View.SYSTEM_UI_FLAG_LAYOUT_FULLSCREEN
        //        | View.SYSTEM_UI_FLAG_HIDE_NAVIGATION // hide nav bar
        //        | View.SYSTEM_UI_FLAG_FULLSCREEN // hide status bar
        //        ;
        //decorView.setSystemUiVisibility(uiOptions);

        /*************************** Find the ID of the default camera******START ***********************/
        // Find the total number of cameras available
        numberOfCameras = Camera.getNumberOfCameras();
        // Find the ID of the default camera
        CameraInfo cameraInfo = new CameraInfo();
        for (int i = 0; i < numberOfCameras; i++) {
            Camera.getCameraInfo(i, cameraInfo);
            if (cameraInfo.facing == CameraInfo.CAMERA_FACING_BACK) {
                defaultCameraId = i;
            }
        }
        /*************************** Find the ID of the default camera******END ***********************/

        /*************************** Add mPreview Touch Listener******START ***********************/
        mPreview.setOnTouchListener(new View.OnTouchListener() {
            @Override
            public boolean onTouch(View v, MotionEvent event) {
                if (mCamera != null) {
                    mCamera.autoFocus(null);
                }
                return false;
            }
        });
        /*************************** Add mPreview Touch Listener******END ***********************/
        initEngin();
    }

    protected void initContentView(){
        setContentView(R.layout.activity_scan);
    }

    private void initEngin() {
        /*************************** init recog appkey ******START ***********************/
        try {
            if (CGlobal.myEngine == null) {
                CGlobal.myEngine = new RecogEngine();
                CGlobal.myEngine.initEngine();
            }
        } catch (Exception e) {
            e.printStackTrace();
        }
        mSoundPlayer = SoundClips.getPlayer(ScanActivity.this);
        /*************************** init recog appkey ******END ***********************/
    }

    /**
     * 刷新闪光灯开关
     * */
    protected void refreshFlashIcon(boolean isOn){
        isFlashOn = isOn;
        if(isFlashOn == false){
            iv_camera_flash.setImageResource(R.drawable.flash_on);
        }else{
            iv_camera_flash.setImageResource(R.drawable.flash_off);
        }
    }

    @Override
    protected void onResume() {
        super.onResume();
        try {
            mSoundPlayer.play(SoundClips.PICTURE_BEGIN);
            mCamera = Camera.open(defaultCameraId);// open the default camera

            //if (m_scanHandler == null) m_scanHandler = new ScanHandler(this, mPreview);
            //m_scanHandler.sendEmptyMessageDelayed(R.id.auto_focus, 1000);
            //关闭闪光灯
            CameraSetting.getInstance(this).closedCameraFlash(mCamera);
            refreshFlashIcon(false);
        } catch (Exception e) {
            e.printStackTrace();
            showFailedDialogAndFinish();
            return;
        }
        /********************************* preview是自定义的viewgroup 继承了surfaceview,将相机和surfaceview 通过holder关联 ***********************/
        mPreview.setCamera(mCamera);
        /********************************* 设置显示的图片和预览角度一致 ***********************/
        setDisplayOrientation();
        try {
            /********************************* 对surfaceview的PreviewCallback的 callback监听，回调onPreviewFrame ***********************/
            mCamera.setOneShotPreviewCallback(this);
        } catch (Exception e) {
            e.printStackTrace();

        }
        /*************************** 当按power键后,再回到程序,surface 不会调用created/changed,所以需要主动初始化相机参数******START ***********************/
        if (mNeedInitCameraInResume) {
            mPreview.surfaceCreated(mPreview.getHolder());
            if(mPreview.getSurfaceView() != null){
                mPreview.surfaceChanged(mPreview.getHolder(), 0, mPreview.getSurfaceView().getWidth(), mPreview.getSurfaceView().getHeight());
            }
            mHandler.sendEmptyMessageDelayed(100, 100);
        }

        mNeedInitCameraInResume = true;
        /********************************* END ***********************/
    }

    @Override
    protected void onPause() {
        super.onPause();
        // Because the Camera object is a shared resource, it's very
        // important to release it when the activity is paused.
        if (mCamera != null) {
            Camera camera = mCamera;
            mCamera.setOneShotPreviewCallback(null);
            mCamera = null;
            mPreview.setCamera(null);
            camera.release();
            camera = null;
        }
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
        if (CGlobal.myEngine != null) {
            CGlobal.myEngine.endEngine();
            CGlobal.myEngine = null;
        }
        if (mSoundPlayer != null) {
            mSoundPlayer.release();
            mSoundPlayer = null;
        }

        //取消屏幕常亮
        getWindow().clearFlags(WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON);

        mHandler.removeMessages(MSG_AUTO_FOCUS);
    }

    @Override
    public void onPreviewFrame(byte[] data, Camera camera) {
        Size size = camera.getParameters().getPreviewSize();
    }

    protected void showFailedDialogAndFinish() {
        AlertDialog dialog = new AlertDialog.Builder(this,R.style.AlertDialogCustom)
                .setMessage(R.string.fail_to_contect_camcard)
                .setCancelable(false)
                .setPositiveButton(android.R.string.ok,
                        new DialogInterface.OnClickListener() {
                            @Override
                            public void onClick(DialogInterface dialog, int which) {
                                finish();
                            }
                        }).create();
        dialog.show();
    }

    private void resumePreviewCallback() {
        if (mCamera != null) {
            mCamera.setOneShotPreviewCallback(this);
        }
    }

    /**
     * 功能：将显示的照片和预览的方向一致
     */
    private void setDisplayOrientation() {
        CameraInfo info = new CameraInfo();
        Camera.getCameraInfo(defaultCameraId, info);
        int rotation = getWindowManager().getDefaultDisplay().getRotation();
        int degrees = 0;
        switch (rotation) {
            case Surface.ROTATION_0:
                degrees = 0;
                break;
            case Surface.ROTATION_90:
                degrees = 90;
                break;
            case Surface.ROTATION_180:
                degrees = 180;
                break;
            case Surface.ROTATION_270:
                degrees = 270;
                break;
        }
        int result = (info.orientation - degrees + 360) % 360;
        mCamera.setDisplayOrientation(result);
        /**
         * 注释原因：因为FOCUS_MODE_CONTINUOUS_PICTURE 不一定兼容所有手机
         * 小米4华为mate8对焦有问题，现在考虑用定时器来实现自动对焦
         */
        /********************************* 20170810--updte--- 使用camera参数设置连续对焦 ***********************/

        Camera.Parameters params = mCamera.getParameters();
        String focusMode = Camera.Parameters.FOCUS_MODE_AUTO;
        if (!TextUtils.equals("samsung", android.os.Build.MANUFACTURER)) {
            focusMode = Camera.Parameters.FOCUS_MODE_CONTINUOUS_PICTURE;
        }
        Log.d("focusMode", focusMode);

        if (!isSupported(focusMode, params.getSupportedFocusModes())) {
            // For some reasons, the driver does not support the current
            // focus mode. Fall back to auto.
            Log.d(" not isSupported", "not");

            if (isSupported(Camera.Parameters.FOCUS_MODE_AUTO, params.getSupportedFocusModes())) {
                focusMode = Camera.Parameters.FOCUS_MODE_AUTO;
            } else {
                focusMode = params.getFocusMode();
            }
            Log.d(" not isSupported", focusMode);

        }
        params.setFocusMode(focusMode);
        mCamera.setParameters(params);
        if (!TextUtils.equals(focusMode, Camera.Parameters.FOCUS_MODE_CONTINUOUS_PICTURE)) {
            Log.d(TAG, "FOCUS_MODE_CONTINUOUS_PICTURE not");
            mHandler.sendEmptyMessageDelayed(MSG_AUTO_FOCUS, 2000);
        }
        /********************************* 20170810--updte--- 使用camera参数设置连续对焦 ***********************/
    }

    public boolean isSupported(String value, List<String> supported) {
        return supported == null ? false : supported.indexOf(value) >= 0;
    }

    private class MsgHandler extends Handler {

        WeakReference<ScanActivity> wr;

        public MsgHandler(ScanActivity activity) {
            this.wr = new WeakReference<>(activity);
        }
        public void handleMessage(Message msg) {
            ScanActivity activity = wr.get();
            if(activity == null){
                return;
            }
            if (msg.what == MSG_AUTO_FOCUS) {
                activity.autoFocus();
                activity.mHandler.removeMessages(MSG_AUTO_FOCUS);
                // 两秒后进行聚焦
                activity.mHandler.sendEmptyMessageDelayed(MSG_AUTO_FOCUS, 1000);
            }else if(msg.what == MSG_RESCAN){
                activity.resumePreviewCallback();
            }
        }
    }

    private void autoFocus() {
        if (mCamera != null) {
            try {
                mCamera.autoFocus(this);
            } catch (Exception e) {
                e.printStackTrace();
            }
        }
    }

    public void returnRecogedData(RecogResult result, Bitmap bmImage) {
        // playBeepSoundAndVibrate();
        //if (vibrator != null) vibrator.vibrate(200L);
        CGlobal.g_RecogResult = result;
        CGlobal.g_bitmapPhoneNumber = bmImage;
        String resultNum = CGlobal.MakePhoneNumberTypeString(result.m_szNumber);
        Log.i(TAG,"------------->>>resultNum:"+resultNum);
        //识别结果过滤
        if (result.equals(lastRecgResultString)) {
            if(lastResultTime > 0 && (System.currentTimeMillis() - lastResultTime) < 1000*3){
                Log.i(TAG,"3S内重复扫描无效");
            }else if(isMobileNum(resultNum)){
                mSoundPlayer.play(SoundClips.PICTURE_COMPLETE);
                resultHandle(resultNum,bmImage);
                lastSuccessTime =  System.currentTimeMillis();
            }
            lastResultTime = System.currentTimeMillis();
            lastRecgResultString = resultNum;
        } else {
            if(isMobileNum(resultNum)){
                //1S间隔以内重复扫描，视为无效扫描
                if((System.currentTimeMillis() - lastResultTime) < 1000*1){
                    Log.i(TAG,"1S内重复扫描屏蔽，防止重复扫描");
                }else{
                    mSoundPlayer.play(SoundClips.PICTURE_COMPLETE);
                    resultHandle(resultNum,bmImage);
                    lastSuccessTime =  System.currentTimeMillis();
                    lastResultTime = System.currentTimeMillis();
                    lastRecgResultString = resultNum;
                }
            }
        }
        restartScan();
    }

    /**
     * 功能：将每一次预览的data 存入ArrayBlockingQueue 队列中，
     * 然后依次进行ismatch的验证，如果匹配就会就会进行进一步的识别
     * 注意点： 1.其中 控制预览框的位置大小，需要
     */
    public void resultHandle(final String result,final Bitmap bmImage) {
        runOnUiThread(new Runnable() {
            @Override
            public void run() {
                if(result != null && !result.isEmpty()){
                    Log.i(TAG, "当前识别结果：" + result);
                    Toast.makeText(getApplicationContext(),result,Toast.LENGTH_SHORT).show();
                }
                ImageView img_recog = findViewById(R.id.img_recog);
                if(img_recog != null && bmImage != null){
                    img_recog.setImageBitmap(bmImage);
                }
            }
        });
    }

    @Override
    public void onAutoFocus(boolean arg0, Camera arg1) {}


    @Override
    public void onClick(View v) {
        if (v.getId() == R.id.iv_camera_back) {
            this.finish();
        } else if (v.getId() == R.id.iv_camera_flash) {
            if (isFlashOn == false) {
                refreshFlashIcon(!isFlashOn);
                CameraSetting.getInstance(this).openCameraFlash(mCamera);
            } else {
                refreshFlashIcon(!isFlashOn);
                CameraSetting.getInstance(this).closedCameraFlash(mCamera);
            }
        }
    }

    public boolean isMobileNum(CharSequence phoneNumber) {
        if (phoneNumber == null || phoneNumber.length() == 0) return false;
        Pattern p = Pattern.compile("^((1[3-9][0-9]))\\d{8}$");
        Matcher m = p.matcher(phoneNumber);
        return m.matches();
    }

    protected abstract void outTimeWarning();

    protected boolean isScanOutTime(){
        if((System.currentTimeMillis() - lastSuccessTime) > 1000*60*2){
            return true;
        }
        return false;
    }

    private void restartScan(){
        bIsAvailable = true;
        m_scanHandler.sendEmptyMessage(R.id.restart_preview);
    }
}
