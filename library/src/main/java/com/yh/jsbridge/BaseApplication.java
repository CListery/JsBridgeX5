package com.yh.jsbridge;

import android.app.Application;
import android.content.Context;
import android.content.Intent;
import android.os.Bundle;
import android.util.Log;
import android.widget.Toast;

import com.tencent.smtt.export.external.extension.interfaces.IX5WebViewExtension;
import com.tencent.smtt.sdk.QbSdk;
import com.tencent.smtt.sdk.TbsDownloader;
import com.tencent.smtt.sdk.TbsShareManager;
import com.tencent.smtt.sdk.WebView;
import com.tencent.smtt.utils.TbsLogClient;

import tbsplus.tbs.tencent.com.tbsplus.TbsPlus;
import tbsplus.tbs.tencent.com.tbsplus.TbsPlusMainActivity;

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
         * @param isX5Core true 表示 x5 内核加载成功,<br/>
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
            } else {
                openUrl(mInstance, "http://soft.imtt.qq.com/browser/tes/feedback.html");
            }
        }
    };
    
    public void openUrl(Context context, String url) {
        if (context == null) {
            return;
        }
        if (!url.startsWith("http") && !url.startsWith("https") && !url.startsWith("file") && !url.startsWith("ftp")) {
            url = "http://" + url;
        }
        Intent intent = new Intent(context, TbsPlusMainActivity.class);
        Bundle bundle = new Bundle();
        bundle.putString("url", url);
        bundle.putInt("screenorientation", TbsPlus.eTBSPLUS_SCREENDIR.eTBSPLUS_SCREENDIR_SENSOR.ordinal());
        intent.putExtra("data", bundle);
        try {
            context.startActivity(intent);
        } catch (Exception e) {
            intent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK | Intent.FLAG_ACTIVITY_EXCLUDE_FROM_RECENTS);
            context.startActivity(intent);
        } finally {
            Toast.makeText(mInstance, "TBS load fail", Toast.LENGTH_SHORT).show();
        }
    }
    
    public BaseApplication() {
        mInstance = this;
    }
    
    @Override
    public void onCreate() {
        super.onCreate();
        Log.d(LOG_TAG, "onCreate");
        //Em...Just for print TBS log
        QbSdk.setTbsLogClient(new TbsLogClient(mInstance) {
            @Override
            public void writeLog(String s) {
                Log.d(LOG_TAG, "tbs: " + s);
            }
        });
        QbSdk.initX5Environment(this, mLoadCallback);
        TbsDownloader.needDownload(this, true, false, new bh());
    }
    
    public static BaseApplication get() {
        return mInstance;
    }
    
    private class bh implements TbsDownloader.TbsDownloaderCallback {
        
        @Override
        public void onNeedDownloadFinish(boolean b, int i) {
            if (TbsShareManager.findCoreForThirdPartyApp(mInstance) == 0 && !TbsShareManager.getCoreDisabled()) {
                TbsShareManager.forceToLoadX5ForThirdApp(mInstance, false);
            }
            QbSdk.preInit(mInstance, mLoadCallback);
        }
    }
    
}
