package com.capstone.mdfeventmanagementsystem;

import android.app.NotificationChannel;
import android.app.PendingIntent;
import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.os.Build;
import android.util.Log;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.core.app.NotificationCompat;

import com.google.firebase.database.ChildEventListener;
import com.google.firebase.database.DataSnapshot;
import com.google.firebase.database.DatabaseError;
import com.google.firebase.database.DatabaseReference;
import com.google.firebase.database.FirebaseDatabase;

import java.util.Arrays;
import java.util.HashSet;
import java.util.Set;

public class NotificationManager {
    private DatabaseReference notificationsRef;
    private Context context;
    private static final String CHANNEL_ID = "event_channel";
    private static final String TAG = "NotificationManager";
    private static final String PREFS_NAME = "NotificationPrefs";
    private static final String SHOWN_NOTIFICATIONS = "shown_notifications"; // Persistent storage key
    private SharedPreferences prefs;

    public NotificationManager(Context context) {
        this.context = context;
        FirebaseDatabase database = FirebaseDatabase.getInstance();
        notificationsRef = database.getReference("notifications");
        prefs = context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE);

        // Create notification channel
        createNotificationChannel();

        // Setup listener for notifications
        setupNotificationListener();
    }

    private void setupNotificationListener() {
        notificationsRef.addChildEventListener(new ChildEventListener() {
            @Override
            public void onChildAdded(@NonNull DataSnapshot snapshot, @Nullable String previousChildName) {
                if (snapshot.exists()) {
                    String notificationId = snapshot.getKey();
                    NotificationData notification = snapshot.getValue(NotificationData.class);

                    if (notification != null) {
                        Set<String> shownNotifications = getShownNotifications();

                        // Check if the notification has already been shown
                        if (!shownNotifications.contains(notificationId)) {
                            showNotification(notificationId, notification.getTitle(), notification.getMessage());

                            // Mark this notification as shown and save persistently
                            shownNotifications.add(notificationId);
                            saveShownNotifications(shownNotifications);
                        }
                    }
                }
            }

            @Override
            public void onChildChanged(@NonNull DataSnapshot snapshot, @Nullable String previousChildName) {}

            @Override
            public void onChildRemoved(@NonNull DataSnapshot snapshot) {}

            @Override
            public void onChildMoved(@NonNull DataSnapshot snapshot, @Nullable String previousChildName) {}

            @Override
            public void onCancelled(@NonNull DatabaseError error) {
                Log.e(TAG, "Database error: " + error.getMessage());
            }
        });
    }

    private void createNotificationChannel() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            CharSequence name = "Event Notifications";
            String description = "Notifications for new events";
            int importance = android.app.NotificationManager.IMPORTANCE_DEFAULT;

            NotificationChannel channel = new NotificationChannel(CHANNEL_ID, name, importance);
            channel.setDescription(description);

            android.app.NotificationManager notificationManager =
                    context.getSystemService(android.app.NotificationManager.class);
            if (notificationManager != null) {
                notificationManager.createNotificationChannel(channel);
            }
        }
    }

    private void showNotification(String notificationId, String title, String message) {
        // Create intent to open the MainActivity when notification is tapped
        Intent intent = new Intent(context, MainActivity.class);
        intent.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK | Intent.FLAG_ACTIVITY_CLEAR_TASK);
        intent.putExtra("notification_id", notificationId); // Add notification ID

        PendingIntent pendingIntent = PendingIntent.getActivity(context, 0, intent,
                PendingIntent.FLAG_IMMUTABLE);

        // Build the notification
        NotificationCompat.Builder builder = new NotificationCompat.Builder(context, CHANNEL_ID)
                .setSmallIcon(R.drawable.ic_check) // Make sure to add this icon to your drawable resources
                .setContentTitle(title)
                .setContentText(message)
                .setPriority(NotificationCompat.PRIORITY_DEFAULT)
                .setContentIntent(pendingIntent)
                .setAutoCancel(true);

        // Show the notification
        android.app.NotificationManager notificationManager =
                (android.app.NotificationManager) context.getSystemService(Context.NOTIFICATION_SERVICE);

        if (notificationManager != null) {
            int notificationIntId = notificationId.hashCode();
            notificationManager.notify(notificationIntId, builder.build());
        }
    }

    // Call this method when the app opens or when a notification is clicked
    public void markNotificationAsViewed(String notificationId) {
        Set<String> shownNotifications = getShownNotifications();
        shownNotifications.add(notificationId);
        saveShownNotifications(shownNotifications);

        // Cancel the notification if it's showing
        android.app.NotificationManager notificationManager =
                (android.app.NotificationManager) context.getSystemService(Context.NOTIFICATION_SERVICE);
        if (notificationManager != null) {
            notificationManager.cancel(notificationId.hashCode());
        }
    }

    // Call this method to clear all notifications
    public void clearAllNotifications() {
        android.app.NotificationManager notificationManager =
                (android.app.NotificationManager) context.getSystemService(Context.NOTIFICATION_SERVICE);
        if (notificationManager != null) {
            notificationManager.cancelAll();
        }
    }

    // ---- Persist notification IDs across app restarts ----
    private Set<String> getShownNotifications() {
        return new HashSet<>(prefs.getStringSet(SHOWN_NOTIFICATIONS, new HashSet<>()));
    }

    private void saveShownNotifications(Set<String> shownNotifications) {
        prefs.edit().putStringSet(SHOWN_NOTIFICATIONS, shownNotifications).apply();
    }

}
