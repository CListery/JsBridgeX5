package com.yhcai.jsbridgex5;

import android.content.Intent;
import android.net.Uri;
import android.os.Bundle;
import android.support.v7.app.AppCompatActivity;
import android.util.Log;

import com.tencent.smtt.sdk.ValueCallback;
import com.tencent.smtt.sdk.WebChromeClient;
import com.yh.jsbridge.BridgeHandler;
import com.yh.jsbridge.CallBackFunction;
import com.yh.jsbridge.x5.X5BridgeWebView;

import org.json.JSONException;
import org.json.JSONObject;

public class MainActivity2 extends AppCompatActivity {

    private static final String LOG_TAG = "MainActivity";

    private X5BridgeWebView mWebView;

    private int RESULT_CODE = 0;
    private ValueCallback<Uri> mUploadMessage;

    static class Location {
        String address;
    }

    static class User {
        String name;
        Location location;
        String address;
    }

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        mWebView = findViewById(R.id.web_view);

        mWebView.setDefaultHandler(new BridgeHandler() {
            @Override
            public void handler(String data, CallBackFunction function) {
                Log.d(LOG_TAG, "handler: " + data + " - " + function);
                JSONObject jsonUser = new JSONObject();
                try {
                    JSONObject jsonLocation = new JSONObject();
                    jsonLocation.put("address", "SDU");
                    jsonUser.put("location", jsonLocation);
                    jsonUser.put("name", "CListery");
                    jsonUser.put("address", "https://github.com/CListery");
                } catch (JSONException e) {
                    e.printStackTrace();
                }
                mWebView.callHandler("functionAutoIpt", jsonUser.toString(), new CallBackFunction() {
                    @Override
                    public void onCallBack(String data) {
                        Log.d(LOG_TAG, "onCallBack: " + data);
                    }
                });
            }
        });

        mWebView.setWebChromeClient(new WebChromeClient() {

            public void openFileChooser(ValueCallback<Uri> uploadMsg, String AcceptType, String capture) {
                this.openFileChooser(uploadMsg);
            }

            public void openFileChooser(ValueCallback<Uri> uploadMsg, String AcceptType) {
                this.openFileChooser(uploadMsg);
            }

            private void openFileChooser(ValueCallback<Uri> uploadMsg) {
                mUploadMessage = uploadMsg;
                pickFile();
            }
        });

        mWebView.loadUrl("file:///android_asset/demo.html");

//        mWebView.loadUrl("http://soft.imtt.qq.com/browser/tes/feedback.html");

        mWebView.registerHandler("submitFromWeb", new BridgeHandler() {

            @Override
            public void handler(String data, CallBackFunction function) {
                Log.i(LOG_TAG, "handler = submitFromWeb, data from web = " + data);
                function.onCallBack("submitFromWeb exe, response data 中文 from Java");
            }

        });

        mWebView.send("ok", new CallBackFunction() {
            @Override
            public void onCallBack(String data) {
                Log.d(LOG_TAG, data);
            }
        });

    }

    @Override
    protected void onDestroy() {
        super.onDestroy();

    }

    public void pickFile() {
        Intent chooserIntent = new Intent(Intent.ACTION_GET_CONTENT);
        chooserIntent.setType("image/*");
        startActivityForResult(chooserIntent, RESULT_CODE);
    }

    @Override
    protected void onActivityResult(int requestCode, int resultCode, Intent intent) {
        if (requestCode == RESULT_CODE) {
            if (null == mUploadMessage) {
                return;
            }
            Uri result = intent == null || resultCode != RESULT_OK ? null : intent.getData();
            mUploadMessage.onReceiveValue(result);
            mUploadMessage = null;
        }
    }
}
