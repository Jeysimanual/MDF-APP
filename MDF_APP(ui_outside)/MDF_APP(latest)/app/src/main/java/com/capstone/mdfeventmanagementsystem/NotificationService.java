package com.capstone.mdfeventmanagementsystem;

import android.app.Notification;
import android.app.NotificationChannel;
import android.app.NotificationManager;
import android.app.PendingIntent;
import android.app.Service;
import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.os.Build;
import android.os.IBinder;
import android.provider.Settings;
import android.util.Log;
import androidx.annotation.NonNull;
import androidx.core.app.NotificationCompat;
import com.google.firebase.database.ChildEventListener;
import com.google.firebase.database.DataSnapshot;
import com.google.firebase.database.DatabaseError;
import com.google.firebase.database.DatabaseReference;
import com.google.firebase.database.FirebaseDatabase;

public class NotificationService extends Service {
    private static final String TAG = "NotifTest";
    private static final String SERVICE_CHANNEL_ID = "persistent_service_channel";
    private static final String EVENT_CHANNEL_ID = "event_notifications";

    private static final String PREFS_NAME = "EventPrefs";
    private static final String LAST_EVENT_KEY = "lastEventTimestamp";

    private DatabaseReference eventsRef;
    private SharedPreferences sharedPreferences;
    private boolean firstRun = true;

    @Override
    public void onCreate() {
        super.onCreate();
        Log.d(TAG, "NotificationService Created");

        createPersistentNotificationChannel();
        createEventNotificationChannel();

        sharedPreferences = getSharedPreferences(PREFS_NAME, MODE_PRIVATE);

        // ✅ Start the service as a foreground service with a nearly hidden notification
        startForeground(1, createPersistentNotification());

        // ✅ Start listening for Firebase events
        listenForNewEvents();
    }

    private void listenForNewEvents() {
        eventsRef = FirebaseDatabase.getInstance().getReference("events");

        eventsRef.addChildEventListener(new ChildEventListener() {
            @Override
            public void onChildAdded(@NonNull DataSnapshot snapshot, String previousChildName) {
                if (firstRun) {
                    firstRun = false;
                    return;
                }

                Log.d(TAG, "New event detected");

                String eventName = snapshot.child("eventName").getValue(String.class);
                Long eventTimestamp = snapshot.child("timestamp").getValue(Long.class);

                if (eventName != null && eventTimestamp != null) {
                    long lastEventTimestamp = sharedPreferences.getLong(LAST_EVENT_KEY, 0);

                    if (eventTimestamp > lastEventTimestamp) {
                        Log.d(TAG, "New event: " + eventName);

                        sendNotification("New Event Posted", "A new event \"" + eventName + "\" has been posted!");

                        sharedPreferences.edit().putLong(LAST_EVENT_KEY, eventTimestamp).apply();
                    }
                }
            }

            @Override
            public void onChildChanged(@NonNull DataSnapshot snapshot, String previousChildName) {}

            @Override
            public void onChildRemoved(@NonNull DataSnapshot snapshot) {}

            @Override
            public void onChildMoved(@NonNull DataSnapshot snapshot, String previousChildName) {}

            @Override
            public void onCancelled(@NonNull DatabaseError error) {
                Log.e(TAG, "Firebase database error: " + error.getMessage());
            }
        });
    }

    private void sendNotification(String title, String message) {
        NotificationManager notificationManager = (NotificationManager) getSystemService(NOTIFICATION_SERVICE);

        Intent intent = new Intent(this, MainActivity.class);
        PendingIntent pendingIntent = PendingIntent.getActivity(
                this, 0, intent, PendingIntent.FLAG_UPDATE_CURRENT | PendingIntent.FLAG_IMMUTABLE
        );

        NotificationCompat.Builder builder = new NotificationCompat.Builder(this, EVENT_CHANNEL_ID)
                .setSmallIcon(R.drawable.ic_notification)
                .setContentTitle(title)
                .setContentText(message)
                .setAutoCancel(true)
                .setPriority(NotificationCompat.PRIORITY_HIGH)
                .setContentIntent(pendingIntent);

        notificationManager.notify((int) System.currentTimeMillis(), builder.build());

        Log.d(TAG, "Notification sent: " + title);
    }

    private void createPersistentNotificationChannel() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            NotificationChannel channel = new NotificationChannel(
                    SERVICE_CHANNEL_ID,
                    "Persistent Service",
                    NotificationManager.IMPORTANCE_MIN // 👈 Lowest visibility (almost hidden)
            );
            channel.setShowBadge(false); // ❌ No app badge
            channel.setDescription("This notification keeps the service running.");
            NotificationManager manager = getSystemService(NotificationManager.class);
            if (manager != null) {
                manager.createNotificationChannel(channel);
            }
        }
    }

    private void createEventNotificationChannel() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            NotificationChannel channel = new NotificationChannel(
                    EVENT_CHANNEL_ID, "Event Notifications", NotificationManager.IMPORTANCE_HIGH
            );
            channel.setDescription("Notifications for new events.");
            NotificationManager manager = getSystemService(NotificationManager.class);
            if (manager != null) {
                manager.createNotificationChannel(channel);
            }
        }
    }

    private Notification createPersistentNotification() {
        return new NotificationCompat.Builder(this, SERVICE_CHANNEL_ID)
                .setSmallIcon(R.drawable.transparent_icon) // 👈 Use an invisible icon if possible
                .setPriority(NotificationCompat.PRIORITY_MIN) // 👈 Almost hidden
                .setSilent(true) // ✅ No sound or vibration
                .setOngoing(true) // Persistent notification
                .setAutoCancel(false) // Should not be dismissible
                .build();
    }

    // ✅ Opens the notification settings screen to let the user manually disable the persistent notification
    private void openNotificationSettings() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            Intent intent = new Intent(Settings.ACTION_CHANNEL_NOTIFICATION_SETTINGS);
            intent.putExtra(Settings.EXTRA_APP_PACKAGE, getPackageName());
            intent.putExtra(Settings.EXTRA_CHANNEL_ID, SERVICE_CHANNEL_ID);
            intent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
            startActivity(intent);
        }
    }

    @Override
    public IBinder onBind(Intent intent) {
        return null;
    }

    @Override
    public int onStartCommand(Intent intent, int flags, int startId) {
        return START_STICKY; // Ensures service is restarted if killed
    }
}
