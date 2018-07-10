package com.yh.jsbridge.x5;

import android.annotation.SuppressLint;
import android.content.Context;
import android.os.Build;
import android.os.Looper;
import android.os.SystemClock;
import android.text.TextUtils;
import android.util.AttributeSet;

import com.tencent.smtt.sdk.WebView;
import com.tencent.smtt.sdk.WebViewClient;
import com.yh.jsbridge.BridgeHandler;
import com.yh.jsbridge.CallBackFunction;
import com.yh.jsbridge.CanNotUseAPI;
import com.yh.jsbridge.DefaultHandler;
import com.yh.jsbridge.Message;
import com.yh.jsbridge.WebViewJavascriptBridge;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;


@SuppressLint("SetJavaScriptEnabled")
public class X5BridgeWebView extends WebView implements WebViewJavascriptBridge {
    
    private final String TAG = "BridgeWebView";
    
    public static final String toLoadJs = "WebViewJavascriptBridge.js";
    Map<String, CallBackFunction> responseCallbacks = new HashMap<>();
    Map<String, BridgeHandler> messageHandlers = new HashMap<>();
    BridgeHandler defaultHandler = new DefaultHandler();
    
    private List<Message> mStartupMessage = new ArrayList<>();
    
    private X5BridgeWebViewClient mProxyBridgeClient;
    
    public List<Message> getStartupMsg() {
        return mStartupMessage;
    }
    
    public void clearStartupMsg() {
        mStartupMessage.clear();
    }
    
    private long uniqueId = 0;
    
    public X5BridgeWebView(Context context, AttributeSet attrs) {
        super(context, attrs);
        init();
    }
    
    public X5BridgeWebView(Context context, AttributeSet attrs, int defStyle) {
        super(context, attrs, defStyle);
        init();
    }
    
    public X5BridgeWebView(Context context) {
        super(context);
        init();
    }
    
    /**
     * @param handler default handler,handle messages send by js without assigned handler name,
     *                if js message has handler name, it will be handled by named handlers registered by native
     */
    public void setDefaultHandler(BridgeHandler handler) {
        this.defaultHandler = handler;
    }
    
    private void init() {
        this.setVerticalScrollBarEnabled(false);
        this.setHorizontalScrollBarEnabled(false);
        this.getSettings().setJavaScriptEnabled(true);
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.KITKAT) {
            WebView.setWebContentsDebuggingEnabled(true);
        }
        mProxyBridgeClient = generateBridgeWebViewClient();
        this.setWebViewClient(generateBridgeWebViewClient());
    }
    
    protected X5BridgeWebViewClient generateBridgeWebViewClient() {
        return new X5BridgeWebViewClient(this);
    }
    
    @Override
    @Deprecated
    public void setWebViewClient(WebViewClient webViewClient) {
        throw new CanNotUseAPI("Can't call this method");
    }
    
    public void setWebViewClient(X5BridgeWebViewClient webViewClient) {
        if (null != mProxyBridgeClient) {
            webViewClient.setLoaded(mProxyBridgeClient.isLoaded());
        }
        mProxyBridgeClient = webViewClient;
        super.setWebViewClient(webViewClient);
    }
    
    void handlerReturnData(String url) {
        String functionName = X5BridgeUtil.getFunctionFromReturnUrl(url);
        CallBackFunction f = responseCallbacks.get(functionName);
        String data = X5BridgeUtil.getDataFromReturnUrl(url);
        if (f != null) {
            f.onCallBack(data);
            responseCallbacks.remove(functionName);
            return;
        }
    }
    
    /**
     * It is recommended to call this function after {@link WebViewClient#onPageFinished(WebView, String)}
     */
    @Override
    public void send(String data) {
        send(data, null);
    }
    
    /**
     * It is recommended to call this function after {@link WebViewClient#onPageFinished(WebView, String)}
     */
    @Override
    public void send(String data, CallBackFunction responseCallback) {
        doSend(null, data, responseCallback);
    }
    
    private void doSend(String handlerName, String data, CallBackFunction responseCallback) {
        Message m = new Message();
        if (!TextUtils.isEmpty(data)) {
            m.setData(data);
        }
        if (responseCallback != null) {
            String callbackStr = String.format(X5BridgeUtil.CALLBACK_ID_FORMAT,
                    ++uniqueId + (X5BridgeUtil.UNDERLINE_STR + SystemClock.currentThreadTimeMillis()));
            responseCallbacks.put(callbackStr, responseCallback);
            m.setCallbackId(callbackStr);
        }
        if (!TextUtils.isEmpty(handlerName)) {
            m.setHandlerName(handlerName);
        }
        queueMessage(m);
    }
    
    private void queueMessage(Message m) {
        if (mProxyBridgeClient.isLoaded()) {
            dispatchMessage(m);
        } else {
            mStartupMessage.add(m);
        }
    }
    
    void dispatchMessage(Message m) {
        String messageJson = m.toJson();
        //escape special characters for json string
        messageJson = messageJson.replaceAll("(\\\\)([^utrn])", "\\\\\\\\$1$2");
        messageJson = messageJson.replaceAll("(?<=[^\\\\])(\")", "\\\\\"");
        String javascriptCommand = String.format(X5BridgeUtil.JS_HANDLE_MESSAGE_FROM_JAVA, messageJson);
        if (Thread.currentThread() == Looper.getMainLooper().getThread()) {
            this.loadUrl(javascriptCommand);
        }
    }
    
    void flushMessageQueue() {
        if (Thread.currentThread() == Looper.getMainLooper().getThread()) {
            loadUrl(X5BridgeUtil.JS_FETCH_QUEUE_FROM_JAVA, new CallBackFunction() {
                
                @Override
                public void onCallBack(String data) {
                    // deserializeMessage
                    List<Message> list = null;
                    try {
                        list = Message.toArrayList(data);
                    } catch (Exception e) {
                        e.printStackTrace();
                        return;
                    }
                    if (list == null || list.size() == 0) {
                        return;
                    }
                    for (int i = 0; i < list.size(); i++) {
                        Message m = list.get(i);
                        String responseId = m.getResponseId();
                        // 是否是response
                        if (!TextUtils.isEmpty(responseId)) {
                            CallBackFunction function = responseCallbacks.get(responseId);
                            String responseData = m.getResponseData();
                            function.onCallBack(responseData);
                            responseCallbacks.remove(responseId);
                        } else {
                            CallBackFunction responseFunction = null;
                            // if had callbackId
                            final String callbackId = m.getCallbackId();
                            if (!TextUtils.isEmpty(callbackId)) {
                                responseFunction = new CallBackFunction() {
                                    @Override
                                    public void onCallBack(String data) {
                                        Message responseMsg = new Message();
                                        responseMsg.setResponseId(callbackId);
                                        responseMsg.setResponseData(data);
                                        mStartupMessage.add(responseMsg);
                                    }
                                };
                            } else {
                                responseFunction = new CallBackFunction() {
                                    @Override
                                    public void onCallBack(String data) {
                                        // do nothing
                                    }
                                };
                            }
                            BridgeHandler handler;
                            if (!TextUtils.isEmpty(m.getHandlerName())) {
                                handler = messageHandlers.get(m.getHandlerName());
                            } else {
                                handler = defaultHandler;
                            }
                            if (handler != null) {
                                handler.handler(m.getData(), responseFunction);
                            }
                        }
                    }
                }
            });
        }
    }
    
    public void loadUrl(String jsUrl, CallBackFunction returnCallback) {
        this.loadUrl(jsUrl);
        responseCallbacks.put(X5BridgeUtil.parseFunctionName(jsUrl), returnCallback);
    }
    
    /**
     * register handler,so that javascript can call it
     *
     * @param handlerName
     * @param handler
     */
    public void registerHandler(String handlerName, BridgeHandler handler) {
        if (handler != null) {
            messageHandlers.put(handlerName, handler);
        }
    }
    
    /**
     * call javascript registered handler
     *
     * @param handlerName
     * @param data
     * @param callBack
     */
    public void callHandler(String handlerName, String data, CallBackFunction callBack) {
        doSend(handlerName, data, callBack);
    }
    
}
