package com.capstone.mdfeventmanagementsystem.Utilities;

import android.app.Notification;
import android.app.NotificationChannel;
import android.app.NotificationManager;
import android.app.PendingIntent;
import android.app.Service;
import android.content.Intent;
import android.content.SharedPreferences;
import android.os.Build;
import android.os.IBinder;
import android.provider.Settings;
import android.util.Log;
import androidx.annotation.NonNull;
import androidx.core.app.NotificationCompat;

import com.capstone.mdfeventmanagementsystem.MainActivity;
import com.capstone.mdfeventmanagementsystem.R;
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

        startForeground(1, createPersistentNotification());

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

                // 🔔 Check for coordinator notification
                checkIfCoordinatorAndNotify(snapshot);
            }

            @Override
            public void onChildChanged(@NonNull DataSnapshot snapshot, String previousChildName) {
                String eventId = snapshot.getKey();
                String eventName = snapshot.child("eventName").getValue(String.class);
                Boolean registrationAllowed = snapshot.child("registrationAllowed").getValue(Boolean.class);
                String eventFor = snapshot.child("eventFor").getValue(String.class);

                Log.d(TAG, "onChildChanged() called");
                Log.d(TAG, "Event ID: " + eventId);
                Log.d(TAG, "Event Name: " + eventName);
                Log.d(TAG, "registrationAllowed: " + registrationAllowed);
                Log.d(TAG, "eventFor: " + eventFor);

                if (eventId != null && eventName != null && registrationAllowed != null && eventFor != null) {
                    String regKey = "regAllowed_" + eventId;
                    boolean hasNotified = sharedPreferences.getBoolean(regKey, false);

                    String yearLevel = sharedPreferences.getString("yearLevel", null);

                    Log.d(TAG, "Current user's yearLevel: " + yearLevel);

                    boolean shouldNotify = false;

                    if (eventFor.equals("All")) {
                        shouldNotify = true;
                    } else if (yearLevel != null && eventFor.equalsIgnoreCase("Grade-" + yearLevel.replace(" ", ""))) {
                        shouldNotify = true;
                    }

                    if (shouldNotify) {
                        if (registrationAllowed && !hasNotified) {
                            Log.d(TAG, "Sending registration OPEN notification for event: " + eventName);
                            sendNotification("Registration Open", "You can now register for \"" + eventName + "\".");
                            sharedPreferences.edit().putBoolean(regKey, true).apply();

                        } else if (!registrationAllowed && hasNotified) {
                            Log.d(TAG, "Sending registration TEMPORARILY CLOSED notification for event: " + eventName);
                            sendNotification("Registration Temporarily Closed",
                                    "Registration for \"" + eventName + "\" is currently closed due to upcoming changes.");
                            sharedPreferences.edit().remove(regKey).apply();
                        } else {
                            Log.d(TAG, "No action needed: Already notified or registration is not allowed.");
                        }
                    } else {
                        Log.d(TAG, "Event not relevant for this student's year level. No notification sent.");
                    }
                } else {
                    Log.d(TAG, "Missing data - cannot process notification for event change.");
                }

                // 🔔 Check for coordinator notification
                checkIfCoordinatorAndNotify(snapshot);
            }

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

    // 🔔 Coordinator Notification Logic
    private void checkIfCoordinatorAndNotify(DataSnapshot eventSnapshot) {
        String eventId = eventSnapshot.getKey();
        String eventName = eventSnapshot.child("eventName").getValue(String.class);
        DataSnapshot coordinatorsSnapshot = eventSnapshot.child("eventCoordinators");

        if (eventId == null || eventName == null || !coordinatorsSnapshot.exists()) {
            Log.d(TAG, "Coordinator check skipped: Missing eventId, eventName, or no coordinators found.");
            return;
        }

        String currentUserEmail = sharedPreferences.getString("email", null);
        if (currentUserEmail == null) {
            Log.d(TAG, "Coordinator check skipped: Current user's email not found in SharedPreferences.");
            return;
        }

        String normalizedEmailKey = currentUserEmail.replace(".", ",");

        Log.d(TAG, "Checking if current user (" + currentUserEmail + ") is a coordinator for event: " + eventName);

        if (coordinatorsSnapshot.hasChild(normalizedEmailKey)) {
            String notifyKey = "coordinatorNotify_" + eventId;
            boolean alreadyNotified = sharedPreferences.getBoolean(notifyKey, false);

            if (!alreadyNotified) {
                Log.d(TAG, "User is a coordinator and has not been notified yet. Sending notification...");
                sendNotification("You're a Coordinator!", "You’ve been added as a coordinator for \"" + eventName + "\".");
                sharedPreferences.edit().putBoolean(notifyKey, true).apply();
                Log.d(TAG, "Notification sent and stored in preferences for key: " + notifyKey);
            } else {
                Log.d(TAG, "User is a coordinator but was already notified for this event.");
            }
        } else {
            Log.d(TAG, "Current user is NOT listed as a coordinator for event: " + eventName);
        }
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
                    NotificationManager.IMPORTANCE_MIN
            );
            channel.setShowBadge(false);
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
                .setSmallIcon(R.drawable.transparent_icon)
                .setPriority(NotificationCompat.PRIORITY_MIN)
                .setSilent(true)
                .setOngoing(true)
                .setAutoCancel(false)
                .build();
    }

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
        return START_STICKY;
    }
}
