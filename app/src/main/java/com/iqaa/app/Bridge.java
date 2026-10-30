package com.iqaa.app;

import android.app.Activity;
import android.content.ContentValues;
import android.net.Uri;
import android.os.Build;
import android.os.Environment;
import android.provider.MediaStore;
import android.view.WindowManager;
import android.webkit.JavascriptInterface;
import java.io.File;
import java.io.FileOutputStream;
import java.io.OutputStream;
import java.nio.charset.StandardCharsets;

public class Bridge {
    private final Activity act;
    Bridge(Activity a) { act = a; }

    @JavascriptInterface public boolean isNative() { return true; }

    /** id=1 منبه الاستيقاظ (نغمة منبه + شاشة كاملة)، غير ذلك إشعار عادي */
    @JavascriptInterface public void schedule(int id, double atMs, String title, String body) {
        Alarms.schedule(act, id, (long) atMs, title, body);
    }

    @JavascriptInterface public void cancel(int id) { Alarms.cancel(act, id); }

    @JavascriptInterface public void notify(String title, String body) {
        Alarms.show(act, 10, title, body, false);
    }

    @JavascriptInterface public void keepScreenOn(final boolean on) {
        act.runOnUiThread(new Runnable() { @Override public void run() {
            if (on) act.getWindow().addFlags(WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON);
            else act.getWindow().clearFlags(WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON);
        }});
    }

    @JavascriptInterface public String saveFile(String name, String content) {
        try {
            byte[] data = content.getBytes(StandardCharsets.UTF_8);
            if (Build.VERSION.SDK_INT >= 29) {
                ContentValues v = new ContentValues();
                v.put(MediaStore.Downloads.DISPLAY_NAME, name);
                v.put(MediaStore.Downloads.MIME_TYPE, "application/json");
                v.put(MediaStore.Downloads.RELATIVE_PATH, Environment.DIRECTORY_DOWNLOADS);
                Uri uri = act.getContentResolver().insert(MediaStore.Downloads.EXTERNAL_CONTENT_URI, v);
                if (uri == null) return "";
                try (OutputStream os = act.getContentResolver().openOutputStream(uri)) {
                    if (os != null) os.write(data);
                }
                return "Download/" + name;
            } else {
                File f = new File(act.getExternalFilesDir(null), name);
                try (FileOutputStream os = new FileOutputStream(f)) { os.write(data); }
                return f.getAbsolutePath();
            }
        } catch (Exception e) { return ""; }
    }
}
