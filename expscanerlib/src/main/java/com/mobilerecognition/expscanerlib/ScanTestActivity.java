package com.mobilerecognition.expscanerlib;

import android.util.Log;
import android.view.View.OnClickListener;

import com.mobilerecognition.phonenumer.ui.ScanActivity;

public class ScanTestActivity extends ScanActivity implements OnClickListener {

    @Override
    protected void outTimeWarning() {
        Log.d("ScanTestActivity","outTimeWarning----------finish!!!!");
        finish();
    }
}