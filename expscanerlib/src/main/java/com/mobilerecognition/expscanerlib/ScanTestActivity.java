package com.mobilerecognition.expscanerlib;

import android.util.Log;
import android.view.View.OnClickListener;

import com.mobilerecognition.phonenumer.ui.ScanOldActivity;

public class ScanTestActivity extends ScanOldActivity implements OnClickListener {

    @Override
    protected void outTimeWarning() {
        Log.d("ScanTestActivity","outTimeWarning----------finish!!!!");
        finish();
    }
}