package com.iqaa.app;

import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;

public class AlarmReceiver extends BroadcastReceiver {
    @Override
    public void onReceive(Context c, Intent i) {
        int id = i.getIntExtra("id", 0);
        Alarms.prefs(c).edit().remove("at" + id).apply();
        boolean alarm = id == 1;
        // التذكيرات العادية تُعرض داخل التطبيق إن كان مفتوحاً، أما المنبه فيرن دائماً
        if (!alarm && MainActivity.visible) return;
        Alarms.show(c, id, i.getStringExtra("title"), i.getStringExtra("body"), alarm);
        if (alarm) {
            try {
                Intent open = new Intent(c, MainActivity.class).addFlags(Intent.FLAG_ACTIVITY_NEW_TASK | Intent.FLAG_ACTIVITY_SINGLE_TOP);
                c.startActivity(open);
            } catch (Exception ignored) {}
        }
    }
}
