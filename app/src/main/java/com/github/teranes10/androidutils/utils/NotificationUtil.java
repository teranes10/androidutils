package com.github.teranes10.androidutils.utils;

import android.app.Notification;
import android.app.NotificationChannel;
import android.app.NotificationManager;
import android.app.PendingIntent;
import android.content.Context;
import android.content.ContextWrapper;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.graphics.Color;
import android.media.RingtoneManager;
import android.net.Uri;
import android.os.Build;

import androidx.annotation.RequiresApi;
import androidx.core.app.ActivityCompat;
import androidx.core.app.NotificationCompat;
import androidx.core.app.NotificationManagerCompat;

import com.github.teranes10.androidutils.R;

import java.util.Random;

public class NotificationUtil extends ContextWrapper {
    private static final String TAG = "NotificationHelper";

    public NotificationUtil(Context base) {
        super(base);
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            createChannels();
        }

    }

    private String CHANNEL_NAME = "High Priority Channel";
    //private String CHANNEL_ID = "au.com.softclient.notification1" + CHANNEL_NAME;
    private String CHANNEL_ID = "au.com.softclient.utils.HighPriority";
    private Uri uri = RingtoneManager.getDefaultUri(RingtoneManager.TYPE_NOTIFICATION);
    private Uri uri2 = Uri.parse("android.resource://au.com.softclient.utils/" + R.raw.samsung_bird_ringtone);

    @RequiresApi(api = Build.VERSION_CODES.O)
    private void createChannels() {
        //crating channel object
        NotificationChannel notificationChannel1 = new NotificationChannel(CHANNEL_ID, CHANNEL_NAME, NotificationManager.IMPORTANCE_HIGH);
        //defining values for this channel
        notificationChannel1.enableLights(true);
        notificationChannel1.enableVibration(true);
        notificationChannel1.setDescription("This is the Description of Channel");
        notificationChannel1.setLightColor(Color.RED);
        notificationChannel1.setLockscreenVisibility(Notification.VISIBILITY_PRIVATE);

        //Create  Notification Manager
        NotificationManager manager = (NotificationManager) getSystemService(Context.NOTIFICATION_SERVICE);
        //With the help of Notification manager , our described channel will br created by passing our channel object(value assigned)
        manager.createNotificationChannel(notificationChannel1);
    }

    public void sendHighPriorityNotification(String title, String body, Class activityname) {

        Intent intent = new Intent(this, activityname);
        intent.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK | Intent.FLAG_ACTIVITY_CLEAR_TASK);

        // Use FLAG_IMMUTABLE to create a PendingIntent for starting an activity
        PendingIntent pendingIntent = PendingIntent.getActivity(this, 267, intent, PendingIntent.FLAG_IMMUTABLE);

        Notification notification = new NotificationCompat.Builder(this, CHANNEL_ID)
                .setContentTitle(title)
                .setContentText(body)
                .setSmallIcon(R.drawable.ic_launcher_foreground)
                .setPriority(NotificationCompat.PRIORITY_HIGH)
                .setStyle(new NotificationCompat.BigTextStyle().setSummaryText("summary").setBigContentTitle(title).bigText(body))
                .setContentIntent(pendingIntent)
                .setAutoCancel(true)
                .setSound(uri)
                .build();
        if (ActivityCompat.checkSelfPermission(this, android.Manifest.permission.POST_NOTIFICATIONS) != PackageManager.PERMISSION_GRANTED) {
            // TODO: Consider calling
            //    ActivityCompat#requestPermissions
            // here to request the missing permissions, and then overriding
            //   public void onRequestPermissionsResult(int requestCode, String[] permissions,
            //                                          int[] grantResults)
            // to handle the case where the user grants the permission. See the documentation
            // for ActivityCompat#requestPermissions for more details.
            return;
        }
        NotificationManagerCompat.from(this).notify(new Random().nextInt(), notification);
    }

    public void sendHighPriorityNotification(String title, String body) {


        Notification notification = new NotificationCompat.Builder(this, CHANNEL_ID)
                .setContentTitle(title)
                .setContentText(body)
                .setSmallIcon(R.drawable.ic_launcher_foreground)
                .setPriority(NotificationCompat.PRIORITY_HIGH)
                .setStyle(new NotificationCompat.BigTextStyle().setSummaryText("summary").setBigContentTitle(title).bigText(body))
                .setAutoCancel(true)
                .setSound(uri2)
                .build();
        if (ActivityCompat.checkSelfPermission(this, android.Manifest.permission.POST_NOTIFICATIONS) != PackageManager.PERMISSION_GRANTED) {
            // TODO: Consider calling
            //    ActivityCompat#requestPermissions
            // here to request the missing permissions, and then overriding
            //   public void onRequestPermissionsResult(int requestCode, String[] permissions,
            //                                          int[] grantResults)
            // to handle the case where the user grants the permission. See the documentation
            // for ActivityCompat#requestPermissions for more details.
            return;
        }
        NotificationManagerCompat.from(this).notify(new Random().nextInt(), notification);
    }
}
