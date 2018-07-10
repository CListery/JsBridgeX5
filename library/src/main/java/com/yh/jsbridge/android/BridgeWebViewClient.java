package com.yh.jsbridge.android;

import android.graphics.Bitmap;
import android.os.Build;
import android.support.annotation.CallSuper;
import android.webkit.WebResourceRequest;
import android.webkit.WebView;
import android.webkit.WebViewClient;

import com.yh.jsbridge.Message;

import java.io.UnsupportedEncodingException;
import java.net.URLDecoder;
import java.util.ArrayList;
import java.util.concurrent.atomic.AtomicBoolean;

/**
 * 如果要自定义WebViewClient必须要集成此类
 * Created by bruce on 10/28/15.
 */
public class BridgeWebViewClient extends WebViewClient {
    private BridgeWebView webView;
    
    private AtomicBoolean mLoaded = new AtomicBoolean(Boolean.FALSE);
    
    public BridgeWebViewClient(BridgeWebView webView) {
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
        
        if (url.startsWith(BridgeUtil.YY_RETURN_DATA)) { // 如果是返回数据
            webView.handlerReturnData(url);
            return true;
        } else if (url.startsWith(BridgeUtil.YY_OVERRIDE_SCHEMA)) { //
            webView.flushMessageQueue();
            return true;
        } else {
            return super.shouldOverrideUrlLoading(view, url);
        }
    }
    
    // 增加shouldOverrideUrlLoading在api》=24时
    @CallSuper
    @Override
    public boolean shouldOverrideUrlLoading(WebView view, WebResourceRequest request) {
        
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.N) {
            String url = request.getUrl().toString();
            try {
                url = URLDecoder.decode(url, "UTF-8");
            } catch (UnsupportedEncodingException ex) {
                ex.printStackTrace();
            }
            if (url.startsWith(BridgeUtil.YY_RETURN_DATA)) { // 如果是返回数据
                webView.handlerReturnData(url);
                return true;
            } else if (url.startsWith(BridgeUtil.YY_OVERRIDE_SCHEMA)) { //
                webView.flushMessageQueue();
                return true;
            } else {
                return super.shouldOverrideUrlLoading(view, request);
            }
        } else {
            return super.shouldOverrideUrlLoading(view, request);
        }
    }
    
    @CallSuper
    @Override
    public void onPageStarted(WebView view, String url, Bitmap favicon) {
        mLoaded.set(false);
        super.onPageStarted(view, url, favicon);
    }
    
    @CallSuper
    @Override
    public void onPageFinished(WebView view, String url) {
        super.onPageFinished(view, url);
        
        BridgeUtil.webViewLoadLocalJs(view, BridgeWebView.toLoadJs);
        
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