package com.yh.jsbridge;

import android.app.Application;
import android.content.Intent;
import android.util.Log;

import com.tencent.smtt.export.external.extension.interfaces.IX5WebViewExtension;
import com.tencent.smtt.sdk.QbSdk;
import com.tencent.smtt.sdk.WebView;

public class BaseApplication extends Application {

    private final static String LOG_TAG = "BaseApplication";

    private boolean mX5Used = false;

    protected static BaseApplication mInstance;

    public static final String ACTION_LOAD_URL = "action_load_url";
    public static final String ACTION_RELOAD_URL = "action_reload_url";

    private QbSdk.PreInitCallback mLoadCallback = new QbSdk.PreInitCallback() {
        @Override
        public void onCoreInitFinished() {
            Log.w(LOG_TAG, "onCoreInitFinished");
        }

        /**
         * x5 内核初始化完成回调接口,可通过参数判断是否加载起来了 x5 内核
         * <br/>
         * <b>
         *     NOTE:<br/>
         *      如果在此回调前创建 webview 会导致使用系统内核
         * </b>
         * @param isX5Core true 表示 x5 内核加载成功,
         *                 false 表示加载失败,此时会自动切换到系统内核
         */
        public void onViewInitFinished(boolean isX5Core) {
            Log.w(LOG_TAG, "onViewInitFinished: " + isX5Core);
            if (isX5Core) {
                WebView webView = new WebView(mInstance);
                IX5WebViewExtension extension = webView.getX5WebViewExtension();
                Log.d(LOG_TAG, "x5: " + extension);
                if (null != extension) {
                    mX5Used = true;
                    mInstance.sendBroadcast(new Intent(ACTION_RELOAD_URL));
                }
                webView.destroy();

            }
        }
    };

    public BaseApplication() {
        mInstance = this;
    }

    @Override
    public void onCreate() {
        super.onCreate();

        QbSdk.initX5Environment(this, mLoadCallback);

    }

    public static BaseApplication get() {
        return mInstance;
    }

}
