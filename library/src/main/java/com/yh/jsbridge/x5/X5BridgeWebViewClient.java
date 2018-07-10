package com.yh.jsbridge.x5;

import android.graphics.Bitmap;
import android.support.annotation.CallSuper;

import com.tencent.smtt.sdk.WebView;
import com.tencent.smtt.sdk.WebViewClient;
import com.yh.jsbridge.Message;

import java.io.UnsupportedEncodingException;
import java.net.URLDecoder;
import java.util.ArrayList;
import java.util.concurrent.atomic.AtomicBoolean;

/**
 * Created by bruce on 10/28/15.
 */
public class X5BridgeWebViewClient extends WebViewClient {
    private X5BridgeWebView webView;
    
    private AtomicBoolean mLoaded = new AtomicBoolean(Boolean.FALSE);
    
    public X5BridgeWebViewClient(X5BridgeWebView webView) {
        this.webView = webView;
    }
    
    @CallSuper
    @Override
    public boolean shouldOverrideUrlLoading(WebView view, String url) {
        try {
            url = URLDecoder.decode(url, "UTF-8");
        } catch (UnsupportedEncodingException e) {
            e.printStackTrace();
        }
        
        if (url.startsWith(X5BridgeUtil.YY_RETURN_DATA)) { // 如果是返回数据
            webView.handlerReturnData(url);
            return true;
        } else if (url.startsWith(X5BridgeUtil.YY_OVERRIDE_SCHEMA)) { //
            webView.flushMessageQueue();
            return true;
        } else {
            return super.shouldOverrideUrlLoading(view, url);
        }
    }
    
    @CallSuper
    @Override
    public void onPageStarted(WebView view, String url, Bitmap favicon) {
        super.onPageStarted(view, url, favicon);
        mLoaded.set(false);
    }
    
    @CallSuper
    @Override
    public void onPageFinished(WebView view, String url) {
        super.onPageFinished(view, url);
        
        X5BridgeUtil.webViewLoadLocalJs(view, X5BridgeWebView.toLoadJs);
        
        mLoaded.set(true);
        
        for (Message m : new ArrayList<>(webView.getStartupMsg())) {
            webView.dispatchMessage(m);
        }
        webView.clearStartupMsg();
    }
    
    public boolean isLoaded() {
        return mLoaded.get();
    }
    
    public void setLoaded(boolean loaded) {
        mLoaded.set(loaded);
    }
}