package com.gmail.axis38akasira.autovolumer.notifications;

import android.app.Activity;
import android.app.Notification;
import android.app.NotificationChannel;
import android.app.NotificationManager;
import android.app.PendingIntent;
import android.content.Context;
import android.content.Intent;
import android.support.annotation.NonNull;
import android.support.v4.app.NotificationCompat;

import com.gmail.axis38akasira.autovolumer.R;

import java.util.ArrayList;

public class NotificationWrapper {

    // 全部の通知を管理
    private static final ArrayList<Integer> notificationIds = new ArrayList<>();

    private Activity activity;
    private NotificationManager notificationManager;
    private int notificationId;
    private int notificationRequestCode;
    private String notificationChannelId;

    public NotificationWrapper(@NonNull Activity activity,
                               @NonNull String notificationChannelId,
                               @NonNull String name) {
        this.activity = activity;
        this.notificationManager =
                (NotificationManager)activity.getSystemService(Context.NOTIFICATION_SERVICE);
        this.notificationChannelId = notificationChannelId;

        // 複数の通知を扱えるようにクラス変数へ登録
        if (notificationIds.size() == 0) {
            notificationId = 0;
        } else {
            notificationId = notificationIds.get(notificationIds.size()-1) + 1;
        }
        notificationIds.add(notificationId);
        notificationRequestCode = notificationId;

        // チャネルの設定
        final NotificationChannel notificationChannel = new NotificationChannel(
                notificationChannelId, name,
                NotificationManager.IMPORTANCE_DEFAULT);
        notificationChannel.setLockscreenVisibility(Notification.VISIBILITY_PUBLIC);
        notificationManager.createNotificationChannel(notificationChannel);
    }

    public void post(@NonNull Class DestinationClass,
                     @NonNull String title, @NonNull String text) {
        final Notification notification =
                new NotificationCompat.Builder(activity, notificationChannelId)
                        .setSmallIcon(R.drawable.ic_launcher_foreground)
                        .setContentTitle(title)
                        .setContentText(text)
                        .setChannelId(notificationChannelId)
                        .setContentIntent(PendingIntent.getActivity(
                                activity.getApplicationContext(),
                                notificationRequestCode,
                                new Intent(activity.getBaseContext(), DestinationClass),
                                    PendingIntent.FLAG_IMMUTABLE
                                )
                        ).build();
        notification.flags = Notification.FLAG_ONGOING_EVENT;
        notificationManager.notify(notificationId, notification);
    }

    public Activity getActivity() {
        return activity;
    }

    public void cancel() {
        notificationManager.cancel(notificationId);
    }
}
