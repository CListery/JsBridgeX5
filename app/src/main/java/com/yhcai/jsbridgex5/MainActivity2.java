package com.yhcai.jsbridgex5;

import android.content.Intent;
import android.graphics.Bitmap;
import android.net.Uri;
import android.os.Build;
import android.os.Bundle;
import android.support.v7.app.AppCompatActivity;
import android.util.Log;

import com.tencent.smtt.sdk.ValueCallback;
import com.tencent.smtt.sdk.WebChromeClient;
import com.tencent.smtt.sdk.WebView;
import com.yh.jsbridge.BridgeHandler;
import com.yh.jsbridge.CallBackFunction;
import com.yh.jsbridge.x5.X5BridgeWebView;
import com.yh.jsbridge.x5.X5BridgeWebViewClient;

import org.json.JSONException;
import org.json.JSONObject;

public class MainActivity2 extends AppCompatActivity {
    
    private static final String LOG_TAG = "MainActivity2";
    
    private X5BridgeWebView mWebView;
    
    private int REQUEST_CODE = 0;
    private int REQUEST_CODE2 = 1;
    
    private ValueCallback<Uri> mUploadMessage;
    private ValueCallback<Uri[]> mUploadMessage2;
    
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);
        
        mWebView = findViewById(R.id.web_view);
        
        registerJSCallbacks();
        
        mWebView.setWebViewClient(new X5BridgeWebViewClient(mWebView) {
            @Override
            public void onPageStarted(WebView webView, String s, Bitmap bitmap) {
                super.onPageStarted(webView, s, bitmap);
                callJS();
            }
        });
        
        mWebView.loadUrl("file:///android_asset/demo.html");
        
    }
    
    private void callJS() {
        mWebView.send("ok", new CallBackFunction() {
            @Override
            public void onCallBack(String data) {
                Log.d(LOG_TAG, data);
            }
        });
    }
    
    private void registerJSCallbacks() {
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
            @Override
            public boolean onShowFileChooser(WebView webView, ValueCallback<Uri[]> valueCallback,
                    FileChooserParams fileChooserParams) {
                try {
                    Intent intent = fileChooserParams.createIntent();
                    startActivityForResult(intent, REQUEST_CODE2);
                    mUploadMessage2 = valueCallback;
                } catch (Exception e) {
                    return false;
                }
                return true;
            }
            
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
        
        mWebView.registerHandler("submitFromWeb", new BridgeHandler() {
            @Override
            public void handler(String data, CallBackFunction function) {
                Log.i(LOG_TAG, "handler = submitFromWeb, data from web = " + data);
                function.onCallBack("submitFromWeb exe, response data 中文 from Java");
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
        startActivityForResult(chooserIntent, REQUEST_CODE);
    }
    
    @Override
    protected void onActivityResult(int requestCode, int resultCode, Intent intent) {
        if (REQUEST_CODE == requestCode) {
            if (null == mUploadMessage) {
                return;
            }
            Uri result = intent == null || resultCode != RESULT_OK ? null : intent.getData();
            mUploadMessage.onReceiveValue(result);
            mUploadMessage = null;
        } else if (REQUEST_CODE2 == requestCode) {
            if (null == mUploadMessage2) {
                return;
            }
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.LOLLIPOP) {
                Uri[] result = WebChromeClient.FileChooserParams.parseResult(resultCode, intent);
                mUploadMessage2.onReceiveValue(result);
            }
            mUploadMessage2 = null;
        }
    }
}
