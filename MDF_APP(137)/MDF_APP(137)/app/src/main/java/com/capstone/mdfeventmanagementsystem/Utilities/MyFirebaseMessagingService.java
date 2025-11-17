package com.capstone.mdfeventmanagementsystem.Utilities;

import android.app.NotificationChannel;
import android.app.NotificationManager;
import android.app.PendingIntent;
import android.content.Context;
import android.content.Intent;
import android.media.RingtoneManager;
import android.os.Build;
import android.util.Log;

import androidx.core.app.NotificationCompat;

import com.capstone.mdfeventmanagementsystem.R;
import com.capstone.mdfeventmanagementsystem.Student.MainActivity2;
import com.google.firebase.messaging.FirebaseMessagingService;
import com.google.firebase.messaging.RemoteMessage;

import java.util.concurrent.atomic.AtomicInteger;

public class MyFirebaseMessagingService extends FirebaseMessagingService {

    private static final String TAG = "FCMService";
    private static final String CHANNEL_ID = "event_notifications";
    private static final String CHANNEL_NAME = "Event Notifications";

    // This generates a unique ID for every notification
    private static final AtomicInteger notificationId = new AtomicInteger(1000);

    @Override
    public void onMessageReceived(RemoteMessage remoteMessage) {
        Log.d(TAG, "From: " + remoteMessage.getFrom());

        if (remoteMessage.getNotification() != null) {
            String title = remoteMessage.getNotification().getTitle();
            String body = remoteMessage.getNotification().getBody();

            Log.d(TAG, "Notification Title: " + title);
            Log.d(TAG, "Notification Body: " + body);

            sendNotification(title, body);
        }
    }

    private void sendNotification(String title, String body) {
        createNotificationChannel();

        Intent intent = new Intent(this, MainActivity2.class);
        intent.addFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP);

        // Unique request code for each notification
        int requestCode = notificationId.incrementAndGet();

        PendingIntent pendingIntent = PendingIntent.getActivity(
                this, requestCode, intent,
                PendingIntent.FLAG_ONE_SHOT | PendingIntent.FLAG_IMMUTABLE
        );

        NotificationCompat.Builder builder = new NotificationCompat.Builder(this, CHANNEL_ID)
                .setSmallIcon(R.drawable.ic_notification)
                .setContentTitle(title != null ? title : "New Event")
                .setContentText(body)
                .setStyle(new NotificationCompat.BigTextStyle().bigText(body))
                .setPriority(NotificationCompat.PRIORITY_HIGH)
                .setDefaults(NotificationCompat.DEFAULT_ALL)
                .setAutoCancel(true)
                .setSound(RingtoneManager.getDefaultUri(RingtoneManager.TYPE_NOTIFICATION))
                .setContentIntent(pendingIntent);

        NotificationManager manager = (NotificationManager) getSystemService(Context.NOTIFICATION_SERVICE);
        manager.notify(requestCode, builder.build());  // ← UNIQUE ID = NEW NOTIFICATION EVERY TIME!
    }

    private void createNotificationChannel() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            NotificationChannel channel = new NotificationChannel(
                    CHANNEL_ID,
                    CHANNEL_NAME,
                    NotificationManager.IMPORTANCE_HIGH
            );
            channel.setDescription("Notifications for new events, registration, and certificates");
            channel.enableVibration(true);
            channel.enableLights(true);

            NotificationManager manager = getSystemService(NotificationManager.class);
            if (manager != null) {
                manager.createNotificationChannel(channel);
            }
        }
    }

    @Override
    public void onNewToken(String token) {
        Log.d(TAG, "FCM Token Refreshed: " + token.substring(0, 30) + "...");
    }
}