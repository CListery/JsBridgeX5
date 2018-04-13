package com.yhcai.jsbridgex5;

import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.os.Bundle;
import android.support.v7.app.AppCompatActivity;
import android.util.Log;

import com.yh.jsbridge.BaseApplication;

public class MainActivity extends AppCompatActivity {
    private static final String LOG_TAG = "MainActivity";
    private BroadcastReceiver mLoadReceiver = new BroadcastReceiver() {
        @Override
        public void onReceive(Context context, Intent intent) {
            Log.d(LOG_TAG, "onReceive");
            startActivity(new Intent(MainActivity.this, MainActivity2.class));
        }
    };

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        IntentFilter filter = new IntentFilter(BaseApplication.ACTION_RELOAD_URL);
        registerReceiver(mLoadReceiver, filter);
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
        unregisterReceiver(mLoadReceiver);
    }

}
