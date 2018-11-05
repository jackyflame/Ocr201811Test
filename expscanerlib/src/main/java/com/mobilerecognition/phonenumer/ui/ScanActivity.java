package com.mobilerecognition.phonenumer.ui;

import android.app.Activity;
import android.graphics.Bitmap;
import android.hardware.Camera;
import android.os.Bundle;
import android.os.Handler;
import android.os.PowerManager;
import android.os.Vibrator;
import android.util.Log;
import android.view.View;
import android.view.View.OnClickListener;
import android.widget.ImageView;
import android.widget.RelativeLayout;
import android.widget.RelativeLayout.LayoutParams;
import android.widget.Toast;

import com.mobilerecognition.engine.RecogEngine;
import com.mobilerecognition.engine.RecogResult;
import com.mobilerecognition.phonenumer.R;
import com.mobilerecognition.phonenumer.camera.CameraPreview;
import com.mobilerecognition.phonenumer.general.CGlobal;
import com.mobilerecognition.phonenumer.handler.RecogListener;
import com.mobilerecognition.phonenumer.handler.ScanHandler;
import com.mobilerecognition.phonenumer.utils.CameraSetting;
import com.mobilerecognition.phonenumer.utils.SoundClips;

import java.util.regex.Matcher;
import java.util.regex.Pattern;

public abstract class ScanActivity extends Activity implements OnClickListener,RecogListener {

	protected static final String TAG = "ScanTestActivity";
	protected CameraPreview mCameraPreview;
	protected RelativeLayout mHomeLayout;
	protected Vibrator vibrator;
	protected ScanHandler m_scanHandler;
	protected PowerManager.WakeLock wakeLock;
	protected boolean bIsAvailable;

	protected long lastResultTime;
	protected long lastSuccessTime;
	protected String lastRecgResultString = null;

	protected SoundClips.Player mSoundPlayer;
	protected boolean isFlashOn = false;
	protected ImageView iv_camera_back;
	protected ImageView iv_camera_flash;

	public void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);
		setContentView(R.layout.activity_scan_old);
		iv_camera_back = findViewById(R.id.iv_camera_back);
		iv_camera_back.setOnClickListener(this);
		iv_camera_flash = findViewById(R.id.iv_camera_flash);
		iv_camera_flash.setOnClickListener(this);


		if (CGlobal.myEngine == null) {
			CGlobal.myEngine = new RecogEngine();
			CGlobal.myEngine.initEngine();
		}
		bIsAvailable = true;
		mSoundPlayer = SoundClips.getPlayer(ScanActivity.this);

		mHomeLayout = findViewById(R.id.previewLayout);
		mHomeLayout.setOnClickListener(new OnClickListener() {
			@Override
			public void onClick(View v) {
				if (mCameraPreview != null) {
					mCameraPreview.autoCameraFocuse();
				}
			}
		});

		vibrator = (Vibrator) getSystemService(VIBRATOR_SERVICE);
		wakeLock = ((PowerManager) getSystemService(POWER_SERVICE)).newWakeLock(
				PowerManager.SCREEN_BRIGHT_WAKE_LOCK | PowerManager.ON_AFTER_RELEASE, "WakeLockActivity");

	}

	public Handler getHandler() {
		return m_scanHandler;
	}

	@Override
	protected void onResume() {
		super.onResume();

		mCameraPreview = new CameraPreview(this, 0, CameraPreview.LayoutMode.FitToParent);
		LayoutParams previewLayoutParams = new LayoutParams(LayoutParams.MATCH_PARENT, LayoutParams.MATCH_PARENT);
		mHomeLayout.addView(mCameraPreview, 0, previewLayoutParams);

		mSoundPlayer.play(SoundClips.PICTURE_BEGIN);

		if (m_scanHandler == null) {
			m_scanHandler = new ScanHandler(this, mCameraPreview);
		}
		m_scanHandler.sendEmptyMessageDelayed(R.id.auto_focus, 1000);

		if (wakeLock != null) wakeLock.acquire();
	}

	@Override
	protected void onPause() {
		super.onPause();
		mCameraPreview.cancelAutoFocus();
		if (m_scanHandler != null) {
			m_scanHandler.quitSynchronously();
			m_scanHandler = null;
		}

		mCameraPreview.stop();
		mHomeLayout.removeView(mCameraPreview); // This is necessary.
		mCameraPreview = null;

		if (wakeLock != null) {
			wakeLock.release();
		}
	}

	@Override
	protected void onDestroy() {
		super.onDestroy();
		if (mSoundPlayer != null) {
			mSoundPlayer.release();
			mSoundPlayer = null;
		}
		if (CGlobal.myEngine != null) {
			CGlobal.myEngine.endEngine();
			CGlobal.myEngine = null;
		}
	}

	public void setAndshowPreviewSize() {
		Camera.Size previewSize = mCameraPreview.getPreviewSize();
		String strPreviewSize = String.valueOf(previewSize.width) + " x " + String.valueOf(previewSize.height);
		Log.i(TAG, "窗口尺寸：" + strPreviewSize);
	}

	@Override
	public void returnRecogedData(RecogResult result, Bitmap bmImage) {
		CGlobal.g_RecogResult = result;
		CGlobal.g_bitmapPhoneNumber = bmImage;
		String resultNum = CGlobal.MakePhoneNumberTypeString(result.m_szNumber);
		Log.i(TAG,"------------->>>resultNum:"+resultNum);
		//识别结果过滤
		if (result.equals(lastRecgResultString)) {
			if(lastResultTime > 0 && (System.currentTimeMillis() - lastResultTime) < 1000*3){
				Log.i(TAG,"3S内重复扫描无效");
			}else if(isMobileNum(resultNum)){
				if (vibrator != null) vibrator.vibrate(200L);
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
					if (vibrator != null) vibrator.vibrate(200L);
					resultHandle(resultNum,bmImage);
					lastSuccessTime =  System.currentTimeMillis();
					lastResultTime = System.currentTimeMillis();
					lastRecgResultString = resultNum;
				}
			}
		}
		restartScan();
	}

	@Override
	public void recogedFailed() {
		if(isScanOutTime()){
			outTimeWarning();
		}
	}

	@Override
	public void onClick(View v) {
		if (v.getId() == R.id.iv_camera_back) {
			this.finish();
		} else if (v.getId() == R.id.iv_camera_flash) {
			if (isFlashOn == false) {
				refreshFlashIcon(!isFlashOn);
				CameraSetting.getInstance(this).openCameraFlash(mCameraPreview.mCamera);
			} else {
				refreshFlashIcon(!isFlashOn);
				CameraSetting.getInstance(this).closedCameraFlash(mCameraPreview.mCamera);
			}
		}else{
			if (mCameraPreview != null) {
				mCameraPreview.autoCameraFocuse();
			}
		}
	}

	private void restartScan(){
		bIsAvailable = true;
		m_scanHandler.sendEmptyMessage(R.id.restart_preview);
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

	public boolean isMobileNum(CharSequence phoneNumber) {
		if (phoneNumber == null || phoneNumber.length() == 0) return false;
		Pattern p = Pattern.compile("^((1[3-9][0-9]))\\d{8}$");
		Matcher m = p.matcher(phoneNumber);
		return m.matches();
	}

	protected abstract void outTimeWarning();

	protected boolean isScanOutTime(){
		if(lastSuccessTime > 0 && (System.currentTimeMillis() - lastSuccessTime) > 1000*60*2){
			return true;
		}
		return false;
	}

	@Override
	public boolean isAvailable() {
		return bIsAvailable;
	}

	@Override
	public void setIsAvailable(boolean isAvailable) {
		bIsAvailable = isAvailable;
	}
}