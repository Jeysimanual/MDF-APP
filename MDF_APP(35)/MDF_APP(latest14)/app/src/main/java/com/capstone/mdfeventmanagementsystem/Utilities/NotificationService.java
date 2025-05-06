package com.capstone.mdfeventmanagementsystem.Utilities;

import android.app.Notification;
import android.app.NotificationChannel;
import android.app.NotificationManager;
import android.app.PendingIntent;
import android.app.Service;
import android.content.Intent;
import android.content.SharedPreferences;
import android.os.Build;
import android.os.Handler;
import android.os.IBinder;
import android.os.Looper;
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
import com.google.firebase.database.ValueEventListener;

import java.text.DateFormat;
import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.HashSet;
import java.util.Locale;
import java.util.Set;
import android.os.Handler;
import android.os.Looper;
import android.os.SystemClock;

public class NotificationService extends Service {
    private static final String TAG = "NotifTest";
    private static final long CHECK_INTERVAL = 30 * 1000;
    private static final String SERVICE_CHANNEL_ID = "persistent_service_channel";
    private static final String EVENT_CHANNEL_ID = "event_notifications";

    private static final String LAST_EVENT_KEY = "lastEventTimestamp";

    private DatabaseReference eventsRef;
    private SharedPreferences sharedPreferences;
    private boolean firstRun = true;
    private Handler handler;


    @Override
    public void onCreate() {
        super.onCreate();
        Log.d(TAG, "NotificationService Created");

        createPersistentNotificationChannel();
        createEventNotificationChannel();

        // Start the periodic check for expired events
        handler = new Handler(Looper.getMainLooper());
        handler.postDelayed(checkExpiredEventsRunnable, CHECK_INTERVAL);

        // ✅ Use UserSession instead of EventPrefs
        sharedPreferences = getSharedPreferences("UserSession", MODE_PRIVATE);

        startForeground(1, createPersistentNotification());

        listenForNewEvents();
        listenForCertificates();
    }

    private Runnable checkExpiredEventsRunnable = new Runnable() {
        @Override
        public void run() {
            // Trigger the check for expired registrations
            RegistrationAllowedChecker.checkAndDisableExpiredRegistrations(new RegistrationAllowedChecker.RegistrationStatusCallback() {
                @Override
                public void onCompletion() {
                    Log.d(TAG, "Event registration check completed.");
                }
            });

            // Check and update expired events (with single notification)
            checkAndUpdateExpiredEvents();

            // Schedule the next check
            handler.postDelayed(this, CHECK_INTERVAL);
        }
    };

    /**
     * Check all events and mark them as expired if their end date/time has passed
     * Also handles status change notifications
     */
    private void checkAndUpdateExpiredEvents() {
        DatabaseReference eventsRef = FirebaseDatabase.getInstance().getReference("events");

        eventsRef.addListenerForSingleValueEvent(new ValueEventListener() {
            @Override
            public void onDataChange(@NonNull DataSnapshot dataSnapshot) {
                Date currentDateTime = new Date();

                for (DataSnapshot eventSnapshot : dataSnapshot.getChildren()) {
                    String eventId = eventSnapshot.getKey();
                    String status = eventSnapshot.child("status").getValue(String.class);
                    String eventName = eventSnapshot.child("eventName").getValue(String.class);
                    String endDate = eventSnapshot.child("endDate").getValue(String.class);
                    String endTime = eventSnapshot.child("endTime").getValue(String.class);

                    // Skip events that are already marked as expired
                    if ("expired".equalsIgnoreCase(status)) {
                        continue;
                    }

                    // If we have end date and time, check if the event has ended
                    if (endDate != null && endTime != null && eventName != null) {
                        try {
                            String endDateTimeStr = endDate + " " + endTime;
                            DateFormat formatter = new SimpleDateFormat("yyyy-MM-dd HH:mm", Locale.getDefault());
                            Date eventEndDateTime = formatter.parse(endDateTimeStr);

                            if (eventEndDateTime != null && currentDateTime.after(eventEndDateTime)) {
                                // Event has ended, update status to expired
                                Log.d(TAG, "Marking event as expired: " + eventId);
                                eventsRef.child(eventId).child("status").setValue("expired");
                                // Also ensure registration is closed
                                eventsRef.child(eventId).child("registrationAllowed").setValue(false);

                                // Send a notification about the event expiring
                                String notifyKey = "expiredNotify_" + eventId;
                                boolean alreadyNotified = sharedPreferences.getBoolean(notifyKey, false);

                                if (!alreadyNotified) {
                                    sendNotification("Event Ended", "The event \"" + eventName + "\" has ended.");
                                    sharedPreferences.edit().putBoolean(notifyKey, true).apply();
                                    Log.d(TAG, "Sent end notification for event: " + eventName);
                                }
                            }
                        } catch (ParseException e) {
                            Log.e(TAG, "Failed to parse endDate and endTime for event " + eventId + ": " + e.getMessage());
                        }
                    }
                }
            }

            @Override
            public void onCancelled(@NonNull DatabaseError databaseError) {
                Log.e(TAG, "Failed to check expired events: " + databaseError.getMessage());
            }
        });
    }

    private void listenForNewEvents() {
        eventsRef = FirebaseDatabase.getInstance().getReference("events");

        eventsRef.addChildEventListener(new ChildEventListener() {
            @Override
            public void onChildAdded(@NonNull DataSnapshot snapshot, String previousChildName) {
                if (firstRun) {
                    firstRun = false; // Skip the first run
                    return; // Prevent sending notifications on the first run
                }

                Log.d(TAG, "New event detected");

                String eventName = snapshot.child("eventName").getValue(String.class);
                Long eventTimestamp = snapshot.child("timestamp").getValue(Long.class);

                if (eventName != null && eventTimestamp != null) {
                    long lastEventTimestamp = sharedPreferences.getLong(LAST_EVENT_KEY, 0);

                    if (lastEventTimestamp == 0) {
                        Log.d(TAG, "First time setup - initializing lastEventTimestamp to " + eventTimestamp);
                        sharedPreferences.edit().putLong(LAST_EVENT_KEY, eventTimestamp).apply();
                        return; // First run - don't notify, but set timestamp
                    }

                    if (eventTimestamp > lastEventTimestamp) {
                        Log.d(TAG, "New event: " + eventName);
                        sendNotification("New Event Posted", "A new event \"" + eventName + "\" has been posted!");
                        sharedPreferences.edit().putLong(LAST_EVENT_KEY, eventTimestamp).apply();
                    }
                }

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

                    if ("All".equalsIgnoreCase(eventFor)) {
                        shouldNotify = true;
                    } else if (isYearLevelMatch(eventFor, yearLevel)) {
                        shouldNotify = true;
                    }

                    if (shouldNotify) {
                        if (registrationAllowed && !hasNotified) {
                            Log.d(TAG, "Sending registration OPEN notification for event: " + eventName);
                            sendNotification("Registration Open", "You can now register for \"" + eventName + "\".");
                            sharedPreferences.edit().putBoolean(regKey, true).apply();

                        } else if (!registrationAllowed && hasNotified) {
                            // Check if event has ended
                            String endDate = snapshot.child("endDate").getValue(String.class); // e.g. "2025-04-25"
                            String endTime = snapshot.child("endTime").getValue(String.class); // e.g. "18:00"

                            if (endDate != null && endTime != null) {
                                try {
                                    String endDateTimeStr = endDate + " " + endTime;
                                    DateFormat formatter = new SimpleDateFormat("yyyy-MM-dd HH:mm", Locale.getDefault());
                                    Date eventEndDateTime = formatter.parse(endDateTimeStr);
                                    Date currentDateTime = new Date();

                                    if (eventEndDateTime != null && currentDateTime.after(eventEndDateTime)) {
                                        Log.d(TAG, "Sending EVENT ENDED notification for: " + eventName);
                                        sendNotification("Event Ended", "The event \"" + eventName + "\" has ended.");
                                        sharedPreferences.edit().remove(regKey).apply();

                                        // Also update the event status to expired in Firebase
                                        DatabaseReference eventRef = FirebaseDatabase.getInstance().getReference("events").child(eventId);
                                        eventRef.child("status").setValue("expired");

                                        // Send explicit expiration notification
                                        String expiredKey = "expiredNotify_" + eventId;
                                        boolean expiredNotified = sharedPreferences.getBoolean(expiredKey, false);

                                        if (!expiredNotified) {
                                            sendNotification("Event Expired", "The event \"" + eventName + "\" has expired and is no longer available.");
                                            sharedPreferences.edit().putBoolean(expiredKey, true).apply();
                                            Log.d(TAG, "Sent expiration notification for: " + eventName);
                                        }

                                        Log.d(TAG, "Updated event status to 'expired' for eventId: " + eventId);
                                    } else {
                                        Log.d(TAG, "Sending registration TEMPORARILY CLOSED notification for event: " + eventName);
                                        sendNotification("Registration Temporarily Closed",
                                                "Registration for \"" + eventName + "\" is currently closed due to upcoming changes.");
                                        sharedPreferences.edit().remove(regKey).apply();
                                    }

                                } catch (ParseException e) {
                                    Log.e(TAG, "Failed to parse endDate and endTime: " + e.getMessage());
                                    // Fallback notification if parsing fails
                                    sendNotification("Registration Temporarily Closed",
                                            "Registration for \"" + eventName + "\" is currently closed due to upcoming changes.");
                                    sharedPreferences.edit().remove(regKey).apply();
                                }
                            } else {
                                Log.d(TAG, "Missing endDate or endTime — can't determine if event has ended.");
                                sendNotification("Registration Temporarily Closed",
                                        "Registration for \"" + eventName + "\" is currently closed due to upcoming changes.");
                                sharedPreferences.edit().remove(regKey).apply();
                            }

                        } else {
                            Log.d(TAG, "No action needed: Already notified or registration is not allowed.");
                        }
                    } else {
                        Log.d(TAG, "Event not relevant for this student's year level. No notification sent.");
                    }

                } else {
                    Log.d(TAG, "Missing data - cannot process notification for event change.");
                }

                checkIfCoordinatorAndNotify(snapshot);
            }

            @Override
            public void onChildRemoved(@NonNull DataSnapshot snapshot) {
                Log.d(TAG, "onChildRemoved() called");
                checkIfCoordinatorAndNotify(snapshot);
            }

            @Override
            public void onChildMoved(@NonNull DataSnapshot snapshot, String previousChildName) {}

            @Override
            public void onCancelled(@NonNull DatabaseError error) {
                Log.e(TAG, "Firebase database error: " + error.getMessage());
            }
        });
    }

    private boolean isYearLevelMatch(String eventFor, String yearLevel) {
        if (eventFor == null || yearLevel == null) return false;

        // Normalize both strings to compare: lowercase, remove spaces and hyphens
        String normalizedEventFor = eventFor.trim().toLowerCase().replace("-", "").replace(" ", "");
        String normalizedYearLevel = yearLevel.trim().toLowerCase().replace("-", "").replace(" ", "");

        return normalizedEventFor.equals(normalizedYearLevel);
    }



    private void checkIfCoordinatorAndNotify(DataSnapshot eventSnapshot) {
        String eventId = eventSnapshot.getKey();
        String eventName = eventSnapshot.child("eventName").getValue(String.class);

        if (eventId == null || eventName == null) {
            Log.d(TAG, "Coordinator check skipped: Missing eventId or eventName.");
            return;
        }

        // 🔒 Ensure sharedPreferences is initialized
        if (sharedPreferences == null) {
            sharedPreferences = getSharedPreferences("UserSession", MODE_PRIVATE);
        }

        String currentUserEmail = sharedPreferences.getString("email", null);
        if (currentUserEmail == null || currentUserEmail.trim().isEmpty()) {
            Log.d(TAG, "Coordinator check skipped: Current user's email not found.");
            return;
        }

        String normalizedEmailKey = currentUserEmail.replace(".", ",");
        String notifyKey = "coordinatorNotify_" + eventId;
        boolean alreadyNotified = sharedPreferences.getBoolean(notifyKey, false);

        // 👁 Check if coordinators node exists and contains user
        boolean isNowCoordinator = false;
        if (eventSnapshot.hasChild("eventCoordinators")) {
            DataSnapshot coordinatorsSnapshot = eventSnapshot.child("eventCoordinators");
            isNowCoordinator = coordinatorsSnapshot.hasChild(normalizedEmailKey);
        }

        Log.d(TAG, "User is now coordinator? " + isNowCoordinator + " | Already notified? " + alreadyNotified);

        if (isNowCoordinator && !alreadyNotified) {
            sendNotification("You're a Coordinator!", "You've been added as a coordinator for \"" + eventName + "\".");
            sharedPreferences.edit().putBoolean(notifyKey, true).apply();
            Log.d(TAG, "Added notification sent for eventId: " + eventId);
        } else if (!isNowCoordinator && alreadyNotified) {
            sendNotification("You're no longer a Coordinator!", "You have been removed as a coordinator for \"" + eventName + "\".");
            sharedPreferences.edit().remove(notifyKey).apply();
            Log.d(TAG, "Removal notification sent for eventId: " + eventId);
        } else {
            Log.d(TAG, "No coordinator status change. No notification needed.");
        }
    }


    private void listenForCertificates() {
        // Initialize SharedPreferences if not done already
        if (sharedPreferences == null) {
            sharedPreferences = getSharedPreferences("UserSession", MODE_PRIVATE);
            Log.d(TAG, "SharedPreferences initialized.");
        }

        // Retrieve the studentId from SharedPreferences
        String studentId = sharedPreferences.getString("studentID", null);
        if (studentId == null || studentId.trim().isEmpty()) {
            Log.d(TAG, "Student ID not found in SharedPreferences. Certificate listener not started.");
            return;
        }
        Log.d(TAG, "Student ID retrieved: " + studentId);

        // Create a reference to Firebase Database to retrieve student's certificates
        DatabaseReference studentCertificatesRef = FirebaseDatabase.getInstance()
                .getReference("students")
                .child(studentId)
                .child("certificates");

        Log.d(TAG, "Fetching certificates for studentId: " + studentId);

        // Retrieve the last processed certificate timestamp from SharedPreferences
        long lastProcessedTimestamp = sharedPreferences.getLong("lastProcessedCertificateTimestamp", 0);

        // Add a listener for the certificates data under the studentId
        studentCertificatesRef.addChildEventListener(new ChildEventListener() {
            @Override
            public void onChildAdded(@NonNull DataSnapshot eventSnapshot, String previousChildName) {
                // Each time a new eventId is added to the certificates collection
                String eventId = eventSnapshot.getKey();  // The key here is the eventId
                Log.d(TAG, "New Event ID added to certificates: " + eventId);

                // Retrieve the timestamp of the certificate (if it exists)
                Long certificateTimestamp = eventSnapshot.child("timestamp").getValue(Long.class);

                if (certificateTimestamp == null) {
                    Log.d(TAG, "Certificate timestamp is null. Skipping notification.");
                    return; // Skip if no timestamp exists
                }

                // Check if this certificate's timestamp is greater than the last processed timestamp
                if (certificateTimestamp <= lastProcessedTimestamp) {
                    Log.d(TAG, "Certificate timestamp is older or equal to the last processed timestamp. Skipping notification.");
                    return; // Skip sending notification for older certificates
                }

                // Fetch the templateName directly from the eventId node inside certificates
                String certificateTitle = eventSnapshot.child("templateName").getValue(String.class);

                if (certificateTitle == null) {
                    certificateTitle = "a new certificate";
                    Log.d(TAG, "Certificate title not found. Defaulting to: " + certificateTitle);
                } else {
                    Log.d(TAG, "Certificate title found: " + certificateTitle);
                }

                // Send notification with the certificate title
                sendNotification("🎓 Certificate Awarded", "You received " + certificateTitle + "!");

                // Update the last processed timestamp to this certificate's timestamp
                sharedPreferences.edit().putLong("lastProcessedCertificateTimestamp", certificateTimestamp).apply();
            }

            @Override
            public void onChildChanged(@NonNull DataSnapshot snapshot, String previousChildName) {
                Log.d(TAG, "Child changed event triggered.");
            }

            @Override
            public void onChildRemoved(@NonNull DataSnapshot snapshot) {
                Log.d(TAG, "Child removed event triggered.");
            }

            @Override
            public void onChildMoved(@NonNull DataSnapshot snapshot, String previousChildName) {
                Log.d(TAG, "Child moved event triggered.");
            }

            @Override
            public void onCancelled(@NonNull DatabaseError error) {
                Log.e(TAG, "Certificate listener error: " + error.getMessage());
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

    @Override
    public void onDestroy() {
        super.onDestroy();
        if (handler != null) {
            handler.removeCallbacks(checkExpiredEventsRunnable);  // Remove the periodic check when the service is destroyed
        }
    }
}