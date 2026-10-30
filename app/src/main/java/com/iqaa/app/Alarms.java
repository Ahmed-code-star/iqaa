package com.iqaa.app;

import android.app.AlarmManager;
import android.app.Notification;
import android.app.NotificationChannel;
import android.app.NotificationManager;
import android.app.PendingIntent;
import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.media.AudioAttributes;
import android.media.RingtoneManager;
import android.net.Uri;
import android.os.Build;

public class Alarms {
    static final String CH_ALARM = "wake_alarm_v1", CH_GENERAL = "reminders_v1";

    static void createChannels(Context c) {
        if (Build.VERSION.SDK_INT < 26) return;
        NotificationManager nm = c.getSystemService(NotificationManager.class);
        if (nm == null) return;
        NotificationChannel a = new NotificationChannel(CH_ALARM, "منبه الاستيقاظ", NotificationManager.IMPORTANCE_HIGH);
        a.setDescription("رنين منبه دورات النوم");
        Uri sound = RingtoneManager.getDefaultUri(RingtoneManager.TYPE_ALARM);
        if (sound == null) sound = RingtoneManager.getDefaultUri(RingtoneManager.TYPE_NOTIFICATION);
        a.setSound(sound, new AudioAttributes.Builder().setUsage(AudioAttributes.USAGE_ALARM).setContentType(AudioAttributes.CONTENT_TYPE_SONIFICATION).build());
        a.enableVibration(true);
        a.setVibrationPattern(new long[]{0, 600, 400, 600, 400, 600});
        a.setBypassDnd(true);
        a.setLockscreenVisibility(Notification.VISIBILITY_PUBLIC);
        NotificationChannel g = new NotificationChannel(CH_GENERAL, "تذكيرات التركيز والماء", NotificationManager.IMPORTANCE_HIGH);
        g.setDescription("انتهاء جلسات بومودورو وتذكير شرب الماء");
        nm.createNotificationChannel(a);
        nm.createNotificationChannel(g);
    }

    static PendingIntent receiverIntent(Context c, int id, String title, String body) {
        Intent i = new Intent(c, AlarmReceiver.class).putExtra("id", id).putExtra("title", title).putExtra("body", body);
        return PendingIntent.getBroadcast(c, id, i, PendingIntent.FLAG_UPDATE_CURRENT | PendingIntent.FLAG_IMMUTABLE);
    }

    static PendingIntent openIntent(Context c, int id) {
        Intent i = new Intent(c, MainActivity.class).addFlags(Intent.FLAG_ACTIVITY_NEW_TASK | Intent.FLAG_ACTIVITY_SINGLE_TOP);
        return PendingIntent.getActivity(c, 100 + id, i, PendingIntent.FLAG_UPDATE_CURRENT | PendingIntent.FLAG_IMMUTABLE);
    }

    static void schedule(Context c, int id, long at, String title, String body) {
        if (at <= System.currentTimeMillis()) return;
        AlarmManager am = (AlarmManager) c.getSystemService(Context.ALARM_SERVICE);
        if (am == null) return;
        PendingIntent pi = receiverIntent(c, id, title, body);
        try {
            if (id == 1) am.setAlarmClock(new AlarmManager.AlarmClockInfo(at, openIntent(c, id)), pi);
            else if (Build.VERSION.SDK_INT >= 31 && !am.canScheduleExactAlarms()) am.setAndAllowWhileIdle(AlarmManager.RTC_WAKEUP, at, pi);
            else am.setExactAndAllowWhileIdle(AlarmManager.RTC_WAKEUP, at, pi);
        } catch (SecurityException e) {
            am.setAndAllowWhileIdle(AlarmManager.RTC_WAKEUP, at, pi);
        }
        prefs(c).edit().putLong("at" + id, at).putString("t" + id, title).putString("b" + id, body).apply();
    }

    static void cancel(Context c, int id) {
        AlarmManager am = (AlarmManager) c.getSystemService(Context.ALARM_SERVICE);
        if (am != null) am.cancel(receiverIntent(c, id, "", ""));
        NotificationManager nm = (NotificationManager) c.getSystemService(Context.NOTIFICATION_SERVICE);
        if (nm != null) nm.cancel(id);
        prefs(c).edit().remove("at" + id).apply();
    }

    static void show(Context c, int id, String title, String body, boolean alarm) {
        Notification.Builder b = Build.VERSION.SDK_INT >= 26
            ? new Notification.Builder(c, alarm ? CH_ALARM : CH_GENERAL)
            : new Notification.Builder(c).setPriority(Notification.PRIORITY_MAX).setDefaults(Notification.DEFAULT_ALL);
        b.setSmallIcon(R.drawable.ic_stat)
         .setContentTitle(title).setContentText(body)
         .setStyle(new Notification.BigTextStyle().bigText(body))
         .setContentIntent(openIntent(c, id))
         .setAutoCancel(true)
         .setColor(0xFF38BDF8)
         .setVisibility(Notification.VISIBILITY_PUBLIC);
        if (alarm) {
            b.setCategory(Notification.CATEGORY_ALARM).setOngoing(true).setFullScreenIntent(openIntent(c, id), true);
            if (Build.VERSION.SDK_INT < 26) b.setSound(RingtoneManager.getDefaultUri(RingtoneManager.TYPE_ALARM));
        } else b.setCategory(Notification.CATEGORY_REMINDER);
        Notification n = b.build();
        if (alarm) n.flags |= Notification.FLAG_INSISTENT;
        try {
            NotificationManager nm = (NotificationManager) c.getSystemService(Context.NOTIFICATION_SERVICE);
            if (nm != null) nm.notify(id, n);
        } catch (SecurityException ignored) {}
    }

    static SharedPreferences prefs(Context c) { return c.getSharedPreferences("alarms", Context.MODE_PRIVATE); }

    static void restoreAll(Context c) {
        SharedPreferences p = prefs(c);
        for (int id = 1; id <= 3; id++) {
            long at = p.getLong("at" + id, 0);
            if (at > System.currentTimeMillis()) schedule(c, id, at, p.getString("t" + id, ""), p.getString("b" + id, ""));
        }
    }
}
