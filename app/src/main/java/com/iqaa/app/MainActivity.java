package com.iqaa.app;

import android.Manifest;
import android.app.Activity;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.graphics.Color;
import android.net.Uri;
import android.os.Build;
import android.os.Bundle;
import android.view.WindowManager;
import android.webkit.ValueCallback;
import android.webkit.WebChromeClient;
import android.webkit.WebResourceRequest;
import android.webkit.WebSettings;
import android.webkit.WebView;
import android.webkit.WebViewClient;

public class MainActivity extends Activity {
    static boolean visible = false;
    WebView web;
    ValueCallback<Uri[]> fileCallback;

    @Override
    protected void onCreate(Bundle state) {
        super.onCreate(state);
        if (Build.VERSION.SDK_INT >= 27) { setShowWhenLocked(true); setTurnScreenOn(true); }
        else getWindow().addFlags(WindowManager.LayoutParams.FLAG_SHOW_WHEN_LOCKED | WindowManager.LayoutParams.FLAG_TURN_SCREEN_ON);
        getWindow().setStatusBarColor(Color.parseColor("#070913"));
        getWindow().setNavigationBarColor(Color.parseColor("#070913"));
        Alarms.createChannels(this);

        web = new WebView(this);
        web.setBackgroundColor(Color.parseColor("#070913"));
        WebSettings s = web.getSettings();
        s.setJavaScriptEnabled(true);
        s.setDomStorageEnabled(true);
        s.setDatabaseEnabled(true);
        s.setMediaPlaybackRequiresUserGesture(false);
        s.setAllowFileAccess(true);
        s.setTextZoom(100);
        web.addJavascriptInterface(new Bridge(this), "AndroidBridge");
        web.setWebViewClient(new WebViewClient() {
            @Override
            public boolean shouldOverrideUrlLoading(WebView v, WebResourceRequest r) {
                Uri u = r.getUrl();
                if ("file".equals(u.getScheme())) return false;
                try { startActivity(new Intent(Intent.ACTION_VIEW, u)); } catch (Exception ignored) {}
                return true;
            }
        });
        web.setWebChromeClient(new WebChromeClient() {
            @Override
            public boolean onShowFileChooser(WebView v, ValueCallback<Uri[]> cb, FileChooserParams p) {
                fileCallback = cb;
                try { startActivityForResult(p.createIntent(), 42); } catch (Exception e) { fileCallback = null; return false; }
                return true;
            }
        });
        setContentView(web);
        web.loadUrl("file:///android_asset/index.html");

        if (Build.VERSION.SDK_INT >= 33 && checkSelfPermission(Manifest.permission.POST_NOTIFICATIONS) != PackageManager.PERMISSION_GRANTED) {
            requestPermissions(new String[]{Manifest.permission.POST_NOTIFICATIONS}, 1);
        }
    }

    @Override
    protected void onActivityResult(int req, int res, Intent data) {
        if (req == 42 && fileCallback != null) {
            fileCallback.onReceiveValue(WebChromeClient.FileChooserParams.parseResult(res, data));
            fileCallback = null;
        } else super.onActivityResult(req, res, data);
    }

    @Override
    protected void onNewIntent(Intent intent) {
        super.onNewIntent(intent);
        if (web != null) web.evaluateJavascript("window.tick && tick()", null);
    }

    @Override protected void onResume() { super.onResume(); visible = true; if (web != null) web.evaluateJavascript("window.tick && tick()", null); }
    @Override protected void onPause() { super.onPause(); visible = false; }

    @Override
    public void onBackPressed() {
        if (web != null) {
            web.evaluateJavascript("(function(){var d=document.querySelector('dialog[open]');if(d&&d.id!=='ring-dialog'){d.close();return 1}var m=document.getElementById('dim');if(m&&!m.hidden){m.click();return 1}if(window.S&&S.tab!=='sleep'){switchTab('sleep');return 1}return 0})()",
                new ValueCallback<String>() { @Override public void onReceiveValue(String v) { if ("0".equals(v)) moveTaskToBack(true); } });
        } else super.onBackPressed();
    }
}
