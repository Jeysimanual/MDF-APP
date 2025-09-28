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
import android.widget.RemoteViews;

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
import com.google.gson.Gson;
import com.google.gson.reflect.TypeToken;

import java.lang.reflect.Type;
import java.text.DateFormat;
import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Date;
import java.util.HashSet;
import java.util.List;
import java.util.Locale;
import java.util.Set;

public class NotificationService extends Service {
    private static final String TAG = "NotifTest";
    private static final long CHECK_INTERVAL = 30 * 1000;
    private static final String SERVICE_CHANNEL_ID = "persistent_service_channel";
    private static final String EVENT_CHANNEL_ID = "event_notifications";
    private static final String LAST_EVENT_KEY = "lastEventTimestamp";
    private static final String NOTIFICATION_HISTORY_KEY = "notification_history_";
    private static final String NOTIFIED_IDS_KEY = "notified_ids_";
    private static final String REG_STATE_KEY = "reg_state_";
    private static final String LAST_REG_CHANGE_TIMESTAMP_KEY = "last_reg_change_timestamp_";
    private static final String COORDINATOR_STATE_KEY = "coordinator_state_";
    private static final String PENDING_NOTIFICATIONS_KEY = "pending_notifications";
    private static final int MAX_HISTORY_SIZE = 100;
    private static final long THREE_DAYS_MS = 3 * 24 * 60 * 60 * 1000;
    private static final long NOTIFICATION_WINDOW_MS = 30 * 60 * 1000;

    private DatabaseReference eventsRef;
    private SharedPreferences sharedPreferences;
    private Handler handler;
    private Gson gson;
    private ChangeHistoryManager changeHistoryManager;

    private static class NotificationItem {
        String id;
        String title;
        String message;
        long timestamp;

        NotificationItem(String id, String title, String message, long timestamp) {
            this.id = id;
            this.title = title;
            this.message = message;
            this.timestamp = timestamp;
        }
    }

    private static class PendingNotificationItem {
        String eventId;
        String changeType;
        long timestamp;
        String eventName;
        String description;
        String startDateTimeStr;

        PendingNotificationItem(String eventId, String changeType, long timestamp, String eventName, String description, String startDateTimeStr) {
            this.eventId = eventId;
            this.changeType = changeType;
            this.timestamp = timestamp;
            this.eventName = eventName;
            this.description = description;
            this.startDateTimeStr = startDateTimeStr;
        }
    }

    @Override
    public void onCreate() {
        super.onCreate();
        Log.d(TAG, "NotificationService Created");

        createPersistentNotificationChannel();
        createEventNotificationChannel();

        gson = new Gson();
        sharedPreferences = getSharedPreferences("UserSession", MODE_PRIVATE);
        changeHistoryManager = new ChangeHistoryManager(sharedPreferences);
        handler = new Handler(Looper.getMainLooper());
        handler.postDelayed(checkExpiredEventsRunnable, CHECK_INTERVAL);

        startForeground(1, createPersistentNotification());

        initializeTimestamps();

        listenForNewEvents();
        listenForCertificates();
        listenForEventProposals();
        listenForEditRequests();

        String userId = getUserId();
        if (userId != null) {
            processMissedNotifications(userId);
            changeHistoryManager.cleanChangeHistory(); // Clean up old history entries on startup
        }
    }


    private void initializeTimestamps() {
        long currentTime = System.currentTimeMillis();

        long lastEventTimestamp = sharedPreferences.getLong(LAST_EVENT_KEY, 0);
        if (lastEventTimestamp == 0) {
            sharedPreferences.edit().putLong(LAST_EVENT_KEY, currentTime).apply();
            Log.d(TAG, "New account: Set lastEventTimestamp to current time");
        }

        long lastProcessedCertificateTimestamp = sharedPreferences.getLong("lastProcessedCertificateTimestamp", 0);
        if (lastProcessedCertificateTimestamp == 0) {
            sharedPreferences.edit().putLong("lastProcessedCertificateTimestamp", currentTime).apply();
            Log.d(TAG, "New account: Set lastProcessedCertificateTimestamp to current time");
        }
    }

    private Runnable checkExpiredEventsRunnable = new Runnable() {
        @Override
        public void run() {
            RegistrationAllowedChecker.checkAndDisableExpiredRegistrations(new RegistrationAllowedChecker.RegistrationStatusCallback() {
                @Override
                public void onCompletion() {
                    Log.d(TAG, "Event registration check completed.");
                }
            });

            checkAndUpdateExpiredEvents();
            checkUpcomingEvents();

            handler.postDelayed(this, CHECK_INTERVAL);
        }
    };

    private void checkAndUpdateExpiredEvents() {
        DatabaseReference eventsRef = FirebaseDatabase.getInstance().getReference("events");

        eventsRef.addListenerForSingleValueEvent(new ValueEventListener() {
            @Override
            public void onDataChange(@NonNull DataSnapshot dataSnapshot) {
                Date currentDateTime = new Date();
                String userId = getUserId();
                if (userId == null) {
                    Log.e(TAG, "checkAndUpdateExpiredEvents: No user ID found");
                    return;
                }

                for (DataSnapshot eventSnapshot : dataSnapshot.getChildren()) {
                    String eventId = eventSnapshot.getKey();
                    String status = eventSnapshot.child("status").getValue(String.class);
                    String eventName = eventSnapshot.child("eventName").getValue(String.class);
                    String endDate = eventSnapshot.child("endDate").getValue(String.class);
                    String endTime = eventSnapshot.child("endTime").getValue(String.class);
                    String description = eventSnapshot.child("description").getValue(String.class);
                    String startDate = eventSnapshot.child("startDate").getValue(String.class);
                    String startTime = eventSnapshot.child("startTime").getValue(String.class);
                    String startDateTimeStr = (startDate != null && startTime != null) ? startDate + " " + startTime : null;

                    if ("expired".equalsIgnoreCase(status)) {
                        continue;
                    }

                    if (endDate != null && endTime != null && eventName != null) {
                        try {
                            String endDateTimeStr = endDate + " " + endTime;
                            DateFormat formatter = new SimpleDateFormat("yyyy-MM-dd HH:mm", Locale.getDefault());
                            Date eventEndDateTime = formatter.parse(endDateTimeStr);

                            if (eventEndDateTime != null && currentDateTime.after(eventEndDateTime)) {
                                Log.d(TAG, "Marking event as expired: " + eventId);
                                eventsRef.child(eventId).child("status").setValue("expired");
                                eventsRef.child(eventId).child("registrationAllowed").setValue(false);

                                String notifyKey = "expiredNotify_" + eventId;
                                if (!hasNotified(userId, notifyKey)) {
                                    sendNotification(notifyKey, "Event Ended", "The event \"" + eventName + "\" has ended.", eventId, description, startDateTimeStr);
                                    markAsNotified(userId, notifyKey);
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

    private void checkUpcomingEvents() {
        String studentId = sharedPreferences.getString("studentID", null);
        if (studentId == null || studentId.trim().isEmpty()) {
            Log.d(TAG, "checkUpcomingEvents: Not a student account, skipping.");
            return;
        }

        DatabaseReference eventsRef = FirebaseDatabase.getInstance().getReference("events");

        eventsRef.addListenerForSingleValueEvent(new ValueEventListener() {
            @Override
            public void onDataChange(@NonNull DataSnapshot dataSnapshot) {
                Date currentDateTime = new Date();
                long currentTimeMs = currentDateTime.getTime();
                String yearLevel = sharedPreferences.getString("yearLevel", null);

                for (DataSnapshot eventSnapshot : dataSnapshot.getChildren()) {
                    String eventId = eventSnapshot.getKey();
                    String status = eventSnapshot.child("status").getValue(String.class);
                    String eventName = eventSnapshot.child("eventName").getValue(String.class);
                    String startDate = eventSnapshot.child("startDate").getValue(String.class);
                    String startTime = eventSnapshot.child("startTime").getValue(String.class);
                    String eventFor = eventSnapshot.child("eventFor").getValue(String.class);
                    String description = eventSnapshot.child("description").getValue(String.class);
                    String startDateTimeStr = (startDate != null && startTime != null) ? startDate + " " + startTime : null;

                    if ("expired".equalsIgnoreCase(status) || startDate == null || startTime == null || eventName == null || eventFor == null) {
                        continue;
                    }

                    boolean shouldNotify = "All".equalsIgnoreCase(eventFor) || isYearLevelMatch(eventFor, yearLevel);
                    if (!shouldNotify) {
                        continue;
                    }

                    try {
                        String startDateTimeStrForParse = startDate + " " + startTime;
                        DateFormat formatter = new SimpleDateFormat("yyyy-MM-dd HH:mm", Locale.getDefault());
                        Date eventStartDateTime = formatter.parse(startDateTimeStrForParse);

                        if (eventStartDateTime != null) {
                            long eventStartTimeMs = eventStartDateTime.getTime();
                            long timeUntilStart = eventStartTimeMs - currentTimeMs;

                            if (timeUntilStart > (THREE_DAYS_MS - NOTIFICATION_WINDOW_MS) && timeUntilStart <= (THREE_DAYS_MS + NOTIFICATION_WINDOW_MS)) {
                                String notifyKey = "upcomingNotify_" + eventId;
                                if (!hasNotified(studentId, notifyKey)) {
                                    String message = "The event \"" + eventName + "\" is happening in 3 days!";
                                    sendNotification(notifyKey, "Upcoming Event", message, eventId, description, startDateTimeStr);
                                    markAsNotified(studentId, notifyKey);
                                    Log.d(TAG, "Sent 3-day reminder for event: " + eventName);
                                }
                            }
                        }
                    } catch (ParseException e) {
                        Log.e(TAG, "Failed to parse startDate and startTime for event " + eventId + ": " + e.getMessage());
                    }
                }
            }

            @Override
            public void onCancelled(@NonNull DatabaseError databaseError) {
                Log.e(TAG, "Failed to check upcoming events: " + databaseError.getMessage());
            }
        });
    }

    private void listenForNewEvents() {
        eventsRef = FirebaseDatabase.getInstance().getReference("events");

        eventsRef.addChildEventListener(new ChildEventListener() {
            @Override
            public void onChildAdded(@NonNull DataSnapshot snapshot, String previousChildName) {
                String eventId = snapshot.getKey();
                String eventName = snapshot.child("eventName").getValue(String.class);
                Long eventTimestamp = snapshot.child("timestamp").getValue(Long.class);
                String description = snapshot.child("description").getValue(String.class);
                String startDate = snapshot.child("startDate").getValue(String.class);
                String startTime = snapshot.child("startTime").getValue(String.class);
                String eventFor = snapshot.child("eventFor").getValue(String.class);
                Boolean registrationAllowed = snapshot.child("registrationAllowed").getValue(Boolean.class);
                Long lastRegChangeTimestamp = snapshot.child("lastRegistrationChangeTimestamp").getValue(Long.class);
                String startDateTimeStr = (startDate != null && startTime != null) ? startDate + " " + startTime : null;

                // Log change to history
                String details = "eventName=" + eventName + ", eventFor=" + eventFor + ", registrationAllowed=" + registrationAllowed;
                changeHistoryManager.saveChangeToHistory("events", eventId, "added", eventTimestamp != null ? eventTimestamp : System.currentTimeMillis(), details);
                Log.d(TAG, "New event detected: " + eventId);

                String userId = getUserId();
                if (eventName != null && eventTimestamp != null) {
                    long lastEventTimestamp = sharedPreferences.getLong(LAST_EVENT_KEY, 0);
                    String yearLevel = sharedPreferences.getString("yearLevel", null);
                    boolean shouldNotify = eventFor != null && ("All".equalsIgnoreCase(eventFor) || isYearLevelMatch(eventFor, yearLevel));

                    if (eventTimestamp > lastEventTimestamp && shouldNotify) {
                        String notifyKey = "newEvent_" + eventId + "_" + eventTimestamp;
                        if (userId != null) {
                            if (!hasNotified(userId, notifyKey)) {
                                sendNotification(notifyKey, "New Event Posted", "A new event \"" + eventName + "\" has been posted!",
                                        eventId, description, startDateTimeStr);
                                markAsNotified(userId, notifyKey);
                                sharedPreferences.edit().putLong(LAST_EVENT_KEY, Math.max(lastEventTimestamp, eventTimestamp)).apply();
                                Log.d(TAG, "Sent new event notification for: " + eventName);
                            }
                        } else {
                            savePendingNotification(eventId, "newEvent", eventTimestamp, eventName, description, startDateTimeStr);
                            Log.d(TAG, "Saved new event to pending notifications: " + eventName);
                        }
                    }
                }

                if (eventId != null && registrationAllowed != null && eventFor != null) {
                    String yearLevel = sharedPreferences.getString("yearLevel", null);
                    boolean shouldNotify = "All".equalsIgnoreCase(eventFor) || isYearLevelMatch(eventFor, yearLevel);
                    Long regTimestamp = lastRegChangeTimestamp != null ? lastRegChangeTimestamp : eventTimestamp;
                    if (regTimestamp == null) {
                        Log.e(TAG, "No valid timestamp for registration state initialization for event: " + eventId);
                        regTimestamp = System.currentTimeMillis();
                    }

                    if (shouldNotify && registrationAllowed) {
                        String regKey = "regAllowed_" + eventId + "_" + regTimestamp;
                        if (userId != null) {
                            if (!hasNotified(userId, regKey)) {
                                sendNotification(regKey, "Registration Open", "You can now register for \"" + eventName + "\".",
                                        eventId, description, startDateTimeStr);
                                markAsNotified(userId, regKey);
                                sharedPreferences.edit()
                                        .putBoolean(REG_STATE_KEY + eventId, true)
                                        .putLong(LAST_REG_CHANGE_TIMESTAMP_KEY + eventId, regTimestamp)
                                        .commit();
                                Log.d(TAG, "Sent initial registration open notification for: " + eventName);
                            }
                        } else {
                            savePendingNotification(eventId, "registrationOpen", regTimestamp, eventName, description, startDateTimeStr);
                            Log.d(TAG, "Saved registration open to pending notifications: " + eventName);
                        }
                    }
                    if (userId != null) {
                        sharedPreferences.edit()
                                .putBoolean(REG_STATE_KEY + eventId, registrationAllowed)
                                .putLong(LAST_REG_CHANGE_TIMESTAMP_KEY + eventId, regTimestamp)
                                .commit();
                    }
                }

                checkIfCoordinatorAndNotify(snapshot);
            }

            @Override
            public void onChildChanged(@NonNull DataSnapshot snapshot, String previousChildName) {
                String userId = getUserId();
                String eventId = snapshot.getKey();
                String eventName = snapshot.child("eventName").getValue(String.class);
                Boolean registrationAllowed = snapshot.child("registrationAllowed").getValue(Boolean.class);
                String eventFor = snapshot.child("eventFor").getValue(String.class);
                String description = snapshot.child("description").getValue(String.class);
                String startDate = snapshot.child("startDate").getValue(String.class);
                String startTime = snapshot.child("startTime").getValue(String.class);
                String endDate = snapshot.child("endDate").getValue(String.class);
                String endTime = snapshot.child("endTime").getValue(String.class);
                Long lastRegChangeTimestamp = snapshot.child("lastRegistrationChangeTimestamp").getValue(Long.class);
                Long eventTimestamp = snapshot.child("timestamp").getValue(Long.class);
                String startDateTimeStr = (startDate != null && startTime != null) ? startDate + " " + startTime : null;

                // Log change to history
                String details = "eventName=" + eventName + ", eventFor=" + eventFor + ", registrationAllowed=" + registrationAllowed;
                changeHistoryManager.saveChangeToHistory("events", eventId, "changed", lastRegChangeTimestamp != null ? lastRegChangeTimestamp : System.currentTimeMillis(), details);
                Log.d(TAG, "Event changed - ID: " + eventId + ", Name: " + eventName + ", RegistrationAllowed: " + registrationAllowed);

                if (eventId == null || eventName == null || registrationAllowed == null || eventFor == null) {
                    Log.e(TAG, "Missing data - cannot process notification for event change. ID: " + eventId +
                            ", Name: " + eventName + ", RegAllowed: " + registrationAllowed + ", EventFor: " + eventFor);
                    return;
                }

                boolean shouldNotify = true;
                if (userId != null) {
                    String yearLevel = sharedPreferences.getString("yearLevel", null);
                    shouldNotify = "All".equalsIgnoreCase(eventFor) || isYearLevelMatch(eventFor, yearLevel);
                    if (!shouldNotify) {
                        Log.d(TAG, "Skipping notification - event not relevant for user. EventFor: " + eventFor + ", YearLevel: " + yearLevel);
                        return;
                    }
                }

                Long regTimestamp = lastRegChangeTimestamp != null ? lastRegChangeTimestamp : eventTimestamp;
                if (regTimestamp == null) {
                    Log.e(TAG, "No valid timestamp for registration change for event: " + eventId);
                    regTimestamp = System.currentTimeMillis();
                }

                boolean lastRegState = sharedPreferences.getBoolean(REG_STATE_KEY + eventId, false);
                long storedRegChangeTimestamp = sharedPreferences.getLong(LAST_REG_CHANGE_TIMESTAMP_KEY + eventId, 0);

                if (registrationAllowed != lastRegState && regTimestamp > storedRegChangeTimestamp) {
                    if (registrationAllowed) {
                        String regKey = "regAllowed_" + eventId + "_" + regTimestamp;
                        if (userId != null) {
                            if (!hasNotified(userId, regKey)) {
                                sendNotification(regKey, "Registration Open", "You can now register for \"" + eventName + "\".",
                                        eventId, description, startDateTimeStr);
                                markAsNotified(userId, regKey);
                                sharedPreferences.edit()
                                        .putBoolean(REG_STATE_KEY + eventId, true)
                                        .putLong(LAST_REG_CHANGE_TIMESTAMP_KEY + eventId, regTimestamp)
                                        .commit();
                                Log.d(TAG, "Sent registration open notification for: " + eventName);
                            }
                        } else {
                            savePendingNotification(eventId, "registrationOpen", regTimestamp, eventName, description, startDateTimeStr);
                            Log.d(TAG, "Saved registration open to pending notifications: " + eventName);
                        }
                    } else {
                        if (endDate != null && endTime != null) {
                            try {
                                String endDateTimeStr = endDate + " " + endTime;
                                DateFormat formatter = new SimpleDateFormat("yyyy-MM-dd HH:mm", Locale.getDefault());
                                Date eventEndDateTime = formatter.parse(endDateTimeStr);
                                Date currentDateTime = new Date();

                                if (eventEndDateTime != null && currentDateTime.after(eventEndDateTime)) {
                                    String notifyKey = "expiredNotify_" + eventId;
                                    if (userId != null) {
                                        if (!hasNotified(userId, notifyKey)) {
                                            sendNotification(notifyKey, "Event Ended", "The event \"" + eventName + "\" has ended.",
                                                    eventId, description, startDateTimeStr);
                                            markAsNotified(userId, notifyKey);
                                            Log.d(TAG, "Sent event ended notification for: " + eventName);

                                            DatabaseReference eventRef = FirebaseDatabase.getInstance().getReference("events").child(eventId);
                                            eventRef.child("status").setValue("expired");

                                            String expiredKey = "expiredNotifyExplicit_" + eventId;
                                            if (!hasNotified(userId, expiredKey)) {
                                                sendNotification(expiredKey, "Event Expired", "The event \"" + eventName + "\" has expired and is no longer available.",
                                                        eventId, description, startDateTimeStr);
                                                markAsNotified(userId, expiredKey);
                                                Log.d(TAG, "Sent expiration notification for: " + eventName);
                                            }
                                        }
                                    } else {
                                        savePendingNotification(eventId, "eventEnded", System.currentTimeMillis(), eventName, description, startDateTimeStr);
                                        savePendingNotification(eventId, "eventExpired", System.currentTimeMillis(), eventName, description, startDateTimeStr);
                                        Log.d(TAG, "Saved event ended/expired to pending notifications: " + eventName);
                                    }
                                } else {
                                    String regKey = "regClosed_" + eventId + "_" + regTimestamp;
                                    if (userId != null) {
                                        if (!hasNotified(userId, regKey)) {
                                            sendNotification(regKey, "Registration Temporarily Closed",
                                                    "Registration for \"" + eventName + "\" is currently closed due to upcoming changes.",
                                                    eventId, description, startDateTimeStr);
                                            markAsNotified(userId, regKey);
                                            Log.d(TAG, "Sent registration temporarily closed notification for: " + eventName);
                                        }
                                    } else {
                                        savePendingNotification(eventId, "registrationClosed", regTimestamp, eventName, description, startDateTimeStr);
                                        Log.d(TAG, "Saved registration closed to pending notifications: " + eventName);
                                    }
                                }
                            } catch (ParseException e) {
                                Log.e(TAG, "Failed to parse endDate and endTime: " + e.getMessage());
                                String regKey = "regClosed_" + eventId + "_" + regTimestamp;
                                if (userId != null) {
                                    if (!hasNotified(userId, regKey)) {
                                        sendNotification(regKey, "Registration Temporarily Closed",
                                                "Registration for \"" + eventName + "\" is currently closed due to upcoming changes.",
                                                eventId, description, startDateTimeStr);
                                        markAsNotified(userId, regKey);
                                        Log.d(TAG, "Sent fallback registration closed notification for: " + eventName);
                                    }
                                } else {
                                    savePendingNotification(eventId, "registrationClosed", regTimestamp, eventName, description, startDateTimeStr);
                                    Log.d(TAG, "Saved fallback registration closed to pending notifications: " + eventName);
                                }
                            }
                        } else {
                            String regKey = "regClosed_" + eventId + "_" + regTimestamp;
                            if (userId != null) {
                                if (!hasNotified(userId, regKey)) {
                                    sendNotification(regKey, "Registration Temporarily Closed",
                                            "Registration for \"" + eventName + "\" is currently closed due to upcoming changes.",
                                            eventId, description, startDateTimeStr);
                                    markAsNotified(userId, regKey);
                                    Log.d(TAG, "Sent registration closed notification (missing end date/time) for: " + eventName);
                                }
                            } else {
                                savePendingNotification(eventId, "registrationClosed", regTimestamp, eventName, description, startDateTimeStr);
                                Log.d(TAG, "Saved registration closed (missing end date/time) to pending notifications: " + eventName);
                            }
                        }
                        if (userId != null) {
                            sharedPreferences.edit()
                                    .putBoolean(REG_STATE_KEY + eventId, registrationAllowed)
                                    .putLong(LAST_REG_CHANGE_TIMESTAMP_KEY + eventId, regTimestamp)
                                    .commit();
                        }
                    }
                }
                checkIfCoordinatorAndNotify(snapshot);
            }

            @Override
            public void onChildRemoved(@NonNull DataSnapshot snapshot) {
                String eventId = snapshot.getKey();
                String eventName = snapshot.child("eventName").getValue(String.class);
                Long eventTimestamp = snapshot.child("timestamp").getValue(Long.class);
                String details = "eventName=" + eventName;
                changeHistoryManager.saveChangeToHistory("events", eventId, "removed", eventTimestamp != null ? eventTimestamp : System.currentTimeMillis(), details);
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
        if ("All".equalsIgnoreCase(eventFor)) return true;

        String normalizedYearLevel = yearLevel.trim().toLowerCase().replace("-", "").replace(" ", "");
        String[] grades = eventFor.split(",");
        for (String grade : grades) {
            String normalizedGrade = grade.trim().toLowerCase().replace("-", "").replace(" ", "");
            if (normalizedGrade.equals(normalizedYearLevel)) {
                return true;
            }
        }
        return false;
    }

    private void checkIfCoordinatorAndNotify(DataSnapshot eventSnapshot) {
        String userId = getUserId();
        String eventId = eventSnapshot.getKey();
        String eventName = eventSnapshot.child("eventName").getValue(String.class);
        String description = eventSnapshot.child("description").getValue(String.class);
        String startDate = eventSnapshot.child("startDate").getValue(String.class);
        String startTime = eventSnapshot.child("startTime").getValue(String.class);
        Long eventTimestamp = eventSnapshot.child("timestamp").getValue(Long.class);
        Long lastCoordinatorChangeTimestamp = eventSnapshot.child("lastCoordinatorChangeTimestamp").getValue(Long.class);
        String startDateTimeStr = (startDate != null && startTime != null) ? startDate + " " + startTime : null;

        if (eventId == null || eventName == null) {
            Log.d(TAG, "Coordinator check skipped: Missing eventId or eventName.");
            return;
        }

        boolean isNowCoordinator = eventSnapshot.hasChild("eventCoordinators") &&
                eventSnapshot.child("eventCoordinators").hasChild(userId != null ? userId : "");
        boolean lastCoordinatorState = sharedPreferences.getBoolean(COORDINATOR_STATE_KEY + eventId, false);

        // Log coordinator change to history
        String details = "eventName=" + eventName + ", isCoordinator=" + isNowCoordinator;
        changeHistoryManager.saveChangeToHistory("events", eventId, "coordinatorChanged", lastCoordinatorChangeTimestamp != null ? lastCoordinatorChangeTimestamp : System.currentTimeMillis(), details);

        long now = System.currentTimeMillis();
        if (isNowCoordinator && !lastCoordinatorState && userId != null) {
            String notifyKey = "coordinatorAdded_" + eventId + "_" + now;
            sendNotification(notifyKey, "You're a Coordinator!", "You've been added as a coordinator for \"" + eventName + "\".",
                    eventId, description, startDateTimeStr);
            sharedPreferences.edit().putBoolean(COORDINATOR_STATE_KEY + eventId, true).apply();
            Log.d(TAG, "Sent coordinator added notification for event: " + eventName);
        } else if (!isNowCoordinator && lastCoordinatorState && userId != null) {
            String notifyKey = "coordinatorRemoved_" + eventId + "_" + now;
            sendNotification(notifyKey, "You're no longer a Coordinator!",
                    "You have been removed as a coordinator for \"" + eventName + "\".", eventId, description, startDateTimeStr);
            sharedPreferences.edit().putBoolean(COORDINATOR_STATE_KEY + eventId, false).apply();
            Log.d(TAG, "Sent coordinator removed notification for event: " + eventName);
        } else if (userId == null && isNowCoordinator) {
            savePendingNotification(eventId, "coordinatorAdded", now, eventName, description, startDateTimeStr);
            Log.d(TAG, "Saved coordinator added to pending notifications: " + eventName);
        }

        if (userId != null) {
            sharedPreferences.edit().putBoolean(COORDINATOR_STATE_KEY + eventId, isNowCoordinator).apply();
        }
    }

    private void savePendingNotification(String eventId, String changeType, long timestamp, String eventName, String description, String startDateTimeStr) {
        List<PendingNotificationItem> pendingNotifications = getPendingNotifications();
        pendingNotifications.add(0, new PendingNotificationItem(eventId, changeType, timestamp, eventName, description, startDateTimeStr));

        if (pendingNotifications.size() > MAX_HISTORY_SIZE) {
            pendingNotifications = pendingNotifications.subList(0, MAX_HISTORY_SIZE);
        }

        String json = gson.toJson(pendingNotifications);
        sharedPreferences.edit().putString(PENDING_NOTIFICATIONS_KEY, json).apply();
        Log.d(TAG, "Saved pending notification: " + changeType + " for event: " + eventId);
    }

    private List<PendingNotificationItem> getPendingNotifications() {
        String json = sharedPreferences.getString(PENDING_NOTIFICATIONS_KEY, null);
        if (json == null) return new ArrayList<>();
        Type type = new TypeToken<List<PendingNotificationItem>>(){}.getType();
        try {
            List<PendingNotificationItem> pendingNotifications = gson.fromJson(json, type);
            return pendingNotifications != null ? pendingNotifications : new ArrayList<>();
        } catch (Exception e) {
            Log.e(TAG, "Failed to parse pending notifications: " + e.getMessage());
            return new ArrayList<>();
        }
    }

    private void processMissedNotifications(String userId) {
        if (userId == null) {
            Log.e(TAG, "processMissedNotifications: No user ID found");
            return;
        }

        List<PendingNotificationItem> pendingNotifications = getPendingNotifications();
        if (pendingNotifications.isEmpty()) {
            Log.d(TAG, "No pending notifications to process for user: " + userId);
            return;
        }

        String yearLevel = sharedPreferences.getString("yearLevel", null);
        DatabaseReference eventsRef = FirebaseDatabase.getInstance().getReference("events");

        for (PendingNotificationItem item : new ArrayList<>(pendingNotifications)) {
            eventsRef.child(item.eventId).addListenerForSingleValueEvent(new ValueEventListener() {
                @Override
                public void onDataChange(@NonNull DataSnapshot snapshot) {
                    if (!snapshot.exists()) {
                        Log.d(TAG, "Event no longer exists: " + item.eventId);
                        pendingNotifications.remove(item);
                        savePendingNotifications(pendingNotifications);
                        return;
                    }

                    String eventName = snapshot.child("eventName").getValue(String.class);
                    String eventFor = snapshot.child("eventFor").getValue(String.class);
                    String description = snapshot.child("description").getValue(String.class);
                    String startDate = snapshot.child("startDate").getValue(String.class);
                    String startTime = snapshot.child("startTime").getValue(String.class);
                    String startDateTimeStr = (startDate != null && startTime != null) ? startDate + " " + startTime : null;
                    String status = snapshot.child("status").getValue(String.class);
                    Boolean registrationAllowed = snapshot.child("registrationAllowed").getValue(Boolean.class);
                    boolean isCoordinator = snapshot.hasChild("eventCoordinators") && snapshot.child("eventCoordinators").hasChild(userId);

                    boolean shouldNotify = eventFor != null && ("All".equalsIgnoreCase(eventFor) || isYearLevelMatch(eventFor, yearLevel));
                    String notifyKey = item.changeType + "_" + item.eventId + "_" + item.timestamp;

                    if (!hasNotified(userId, notifyKey)) {
                        switch (item.changeType) {
                            case "newEvent":
                                if (shouldNotify && !"expired".equalsIgnoreCase(status)) {
                                    sendNotification(notifyKey, "New Event Posted", "A new event \"" + eventName + "\" has been posted!",
                                            item.eventId, description, startDateTimeStr);
                                    markAsNotified(userId, notifyKey);
                                    Log.d(TAG, "Processed pending new event notification: " + eventName);
                                }
                                break;
                            case "registrationOpen":
                                if (shouldNotify && registrationAllowed != null && registrationAllowed && !"expired".equalsIgnoreCase(status)) {
                                    sendNotification(notifyKey, "Registration Open", "You can now register for \"" + eventName + "\".",
                                            item.eventId, description, startDateTimeStr);
                                    markAsNotified(userId, notifyKey);
                                    sharedPreferences.edit()
                                            .putBoolean(REG_STATE_KEY + item.eventId, true)
                                            .putLong(LAST_REG_CHANGE_TIMESTAMP_KEY + item.eventId, item.timestamp)
                                            .apply();
                                    Log.d(TAG, "Processed pending registration open notification: " + eventName);
                                }
                                break;
                            case "registrationClosed":
                                if (shouldNotify && registrationAllowed != null && !registrationAllowed && !"expired".equalsIgnoreCase(status)) {
                                    sendNotification(notifyKey, "Registration Temporarily Closed",
                                            "Registration for \"" + eventName + "\" is currently closed due to upcoming changes.",
                                            item.eventId, description, startDateTimeStr);
                                    markAsNotified(userId, notifyKey);
                                    sharedPreferences.edit()
                                            .putBoolean(REG_STATE_KEY + item.eventId, false)
                                            .putLong(LAST_REG_CHANGE_TIMESTAMP_KEY + item.eventId, item.timestamp)
                                            .apply();
                                    Log.d(TAG, "Processed pending registration closed notification: " + eventName);
                                }
                                break;
                            case "eventEnded":
                            case "eventExpired":
                                if ("expired".equalsIgnoreCase(status)) {
                                    String title = item.changeType.equals("eventEnded") ? "Event Ended" : "Event Expired";
                                    String message = item.changeType.equals("eventEnded") ?
                                            "The event \"" + eventName + "\" has ended." :
                                            "The event \"" + eventName + "\" has expired and is no longer available.";
                                    sendNotification(notifyKey, title, message, item.eventId, description, startDateTimeStr);
                                    markAsNotified(userId, notifyKey);
                                    Log.d(TAG, "Processed pending " + item.changeType + " notification: " + eventName);
                                }
                                break;
                            case "coordinatorAdded":
                                if (isCoordinator && !sharedPreferences.getBoolean(COORDINATOR_STATE_KEY + item.eventId, false)) {
                                    sendNotification(notifyKey, "You're a Coordinator!", "You've been added as a coordinator for \"" + eventName + "\".",
                                            item.eventId, description, startDateTimeStr);
                                    markAsNotified(userId, notifyKey);
                                    sharedPreferences.edit().putBoolean(COORDINATOR_STATE_KEY + item.eventId, true).apply();
                                    Log.d(TAG, "Processed pending coordinator added notification: " + eventName);
                                }
                                break;
                            case "coordinatorRemoved":
                                if (!isCoordinator && sharedPreferences.getBoolean(COORDINATOR_STATE_KEY + item.eventId, false)) {
                                    sendNotification(notifyKey, "You're no longer a Coordinator!",
                                            "You have been removed as a coordinator for \"" + eventName + "\".", item.eventId, description, startDateTimeStr);
                                    markAsNotified(userId, notifyKey);
                                    sharedPreferences.edit().putBoolean(COORDINATOR_STATE_KEY + item.eventId, false).apply();
                                    Log.d(TAG, "Processed pending coordinator removed notification: " + eventName);
                                }
                                break;
                        }
                    }

                    pendingNotifications.remove(item);
                    savePendingNotifications(pendingNotifications);
                }

                @Override
                public void onCancelled(@NonNull DatabaseError error) {
                    Log.e(TAG, "Failed to process pending notification for event: " + item.eventId + ", error: " + error.getMessage());
                }
            });
        }
    }

    private void savePendingNotifications(List<PendingNotificationItem> pendingNotifications) {
        String json = gson.toJson(pendingNotifications);
        sharedPreferences.edit().putString(PENDING_NOTIFICATIONS_KEY, json).apply();
        Log.d(TAG, "Updated pending notifications list, size: " + pendingNotifications.size());
    }

    private void listenForCertificates() {
        String studentId = sharedPreferences.getString("studentID", null);
        if (studentId == null || studentId.trim().isEmpty()) {
            Log.d(TAG, "Student ID not found in SharedPreferences. Certificate listener not started.");
            return;
        }

        DatabaseReference studentCertificatesRef = FirebaseDatabase.getInstance()
                .getReference("students")
                .child(studentId)
                .child("certificates");

        studentCertificatesRef.addChildEventListener(new ChildEventListener() {
            @Override
            public void onChildAdded(@NonNull DataSnapshot eventSnapshot, String previousChildName) {
                String eventId = eventSnapshot.getKey();
                Long certificateTimestamp = eventSnapshot.child("timestamp").getValue(Long.class);
                String templateName = eventSnapshot.child("templateName").getValue(String.class);

                String details = "templateName=" + (templateName != null ? templateName : "unknown");
                changeHistoryManager.saveChangeToHistory("students/certificates", eventId, "added", certificateTimestamp != null ? certificateTimestamp : System.currentTimeMillis(), details);

                if (certificateTimestamp == null) {
                    Log.d(TAG, "Certificate timestamp is null. Skipping notification.");
                    return;
                }

                long lastProcessedTimestamp = sharedPreferences.getLong("lastProcessedCertificateTimestamp", 0);

                if (certificateTimestamp > lastProcessedTimestamp) {
                    String certificateTitle = templateName != null ? templateName : "a new certificate";
                    String notifyKey = "certificate_" + eventId + "_" + certificateTimestamp;
                    if (!hasNotified(studentId, notifyKey)) {
                        sendNotification(notifyKey, "🎓 Certificate Awarded", "You received " + certificateTitle + "!", eventId, null, null);
                        markAsNotified(studentId, notifyKey);
                        sharedPreferences.edit().putLong("lastProcessedCertificateTimestamp", Math.max(lastProcessedTimestamp, certificateTimestamp)).apply();
                    }
                }
            }

            @Override
            public void onChildChanged(@NonNull DataSnapshot snapshot, String previousChildName) {
                String eventId = snapshot.getKey();
                Long certificateTimestamp = snapshot.child("timestamp").getValue(Long.class);
                String templateName = snapshot.child("templateName").getValue(String.class);
                String details = "templateName=" + (templateName != null ? templateName : "unknown");
                changeHistoryManager.saveChangeToHistory("students/certificates", eventId, "changed", certificateTimestamp != null ? certificateTimestamp : System.currentTimeMillis(), details);
            }

            @Override
            public void onChildRemoved(@NonNull DataSnapshot snapshot) {
                String eventId = snapshot.getKey();
                Long certificateTimestamp = snapshot.child("timestamp").getValue(Long.class);
                String templateName = snapshot.child("templateName").getValue(String.class);
                String details = "templateName=" + (templateName != null ? templateName : "unknown");
                changeHistoryManager.saveChangeToHistory("students/certificates", eventId, "removed", certificateTimestamp != null ? certificateTimestamp : System.currentTimeMillis(), details);
            }

            @Override
            public void onChildMoved(@NonNull DataSnapshot snapshot, String previousChildName) {}

            @Override
            public void onCancelled(@NonNull DatabaseError error) {
                Log.e(TAG, "Certificate listener error: " + error.getMessage());
            }
        });
    }

    private void listenForEventProposals() {
        String teacherId = sharedPreferences.getString("teacherId", null);
        if (teacherId == null || teacherId.trim().isEmpty()) {
            Log.d(TAG, "Teacher ID not found in SharedPreferences. Event proposal listener not started.");
            return;
        }

        DatabaseReference eventProposalsRef = FirebaseDatabase.getInstance().getReference("eventProposals");
        DatabaseReference eventsRef = FirebaseDatabase.getInstance().getReference("events");

        eventsRef.addChildEventListener(new ChildEventListener() {
            @Override
            public void onChildAdded(@NonNull DataSnapshot snapshot, String previousChildName) {
                String eventId = snapshot.getKey();
                String eventName = snapshot.child("eventName").getValue(String.class);
                String description = snapshot.child("description").getValue(String.class);
                String startDate = snapshot.child("startDate").getValue(String.class);
                String startTime = snapshot.child("startTime").getValue(String.class);
                Long eventTimestamp = snapshot.child("timestamp").getValue(Long.class);
                String startDateTimeStr = (startDate != null && startTime != null) ? startDate + " " + startTime : null;

                String details = "eventName=" + eventName;
                changeHistoryManager.saveChangeToHistory("events", eventId, "added", eventTimestamp != null ? eventTimestamp : System.currentTimeMillis(), details);

                if (eventId == null || eventName == null || eventTimestamp == null) {
                    Log.d(TAG, "Event approval check skipped: Missing eventId, eventName, or timestamp.");
                    return;
                }

                eventProposalsRef.child(eventId).addListenerForSingleValueEvent(new ValueEventListener() {
                    @Override
                    public void onDataChange(@NonNull DataSnapshot proposalSnapshot) {
                        if (proposalSnapshot.exists()) {
                            String createdBy = proposalSnapshot.child("createdBy").getValue(String.class);
                            String proposalNotifyKey = "proposalApproved_" + eventId + "_" + eventTimestamp;
                            if (createdBy != null && createdBy.equals(teacherId) && !hasNotified(teacherId, proposalNotifyKey)) {
                                sendNotification(proposalNotifyKey, "Event Proposal Approved", "Your proposed event \"" + eventName + "\" has been approved!", eventId, description, startDateTimeStr);
                                markAsNotified(teacherId, proposalNotifyKey);
                                sharedPreferences.edit()
                                        .remove("proposalRejected_" + eventId)
                                        .remove("proposalRestored_" + eventId)
                                        .remove("lastStatus_" + eventId)
                                        .apply();
                            }
                        }
                    }

                    @Override
                    public void onCancelled(@NonNull DatabaseError error) {
                        Log.e(TAG, "Failed to check event proposal: " + error.getMessage());
                    }
                });
            }

            @Override
            public void onChildChanged(@NonNull DataSnapshot snapshot, String previousChildName) {
                String eventId = snapshot.getKey();
                String eventName = snapshot.child("eventName").getValue(String.class);
                Long eventTimestamp = snapshot.child("timestamp").getValue(Long.class);
                String details = "eventName=" + eventName;
                changeHistoryManager.saveChangeToHistory("events", eventId, "changed", eventTimestamp != null ? eventTimestamp : System.currentTimeMillis(), details);
            }

            @Override
            public void onChildRemoved(@NonNull DataSnapshot snapshot) {
                String eventId = snapshot.getKey();
                String eventName = snapshot.child("eventName").getValue(String.class);
                Long eventTimestamp = snapshot.child("timestamp").getValue(Long.class);
                String details = "eventName=" + eventName;
                changeHistoryManager.saveChangeToHistory("events", eventId, "removed", eventTimestamp != null ? eventTimestamp : System.currentTimeMillis(), details);
            }

            @Override
            public void onChildMoved(@NonNull DataSnapshot snapshot, String previousChildName) {}

            @Override
            public void onCancelled(@NonNull DatabaseError error) {
                Log.e(TAG, "Event listener error for proposals: " + error.getMessage());
            }
        });

        eventProposalsRef.addChildEventListener(new ChildEventListener() {
            @Override
            public void onChildAdded(@NonNull DataSnapshot snapshot, String previousChildName) {
                String eventId = snapshot.getKey();
                String status = snapshot.child("status").getValue(String.class);
                Long eventTimestamp = snapshot.child("timestamp").getValue(Long.class);
                String eventName = snapshot.child("eventName").getValue(String.class);
                String details = "eventName=" + eventName + ", status=" + status;
                changeHistoryManager.saveChangeToHistory("eventProposals", eventId, "added", eventTimestamp != null ? eventTimestamp : System.currentTimeMillis(), details);

                if (eventId != null && status != null && eventTimestamp != null) {
                    sharedPreferences.edit().putString("lastStatus_" + eventId, status).putLong("lastStatusTimestamp_" + eventId, eventTimestamp).apply();
                }
            }

            @Override
            public void onChildChanged(@NonNull DataSnapshot snapshot, String previousChildName) {
                String eventId = snapshot.getKey();
                String eventName = snapshot.child("eventName").getValue(String.class);
                String status = snapshot.child("status").getValue(String.class);
                String createdBy = snapshot.child("createdBy").getValue(String.class);
                Long eventTimestamp = snapshot.child("timestamp").getValue(Long.class);
                String description = snapshot.child("description").getValue(String.class);

                String details = "eventName=" + eventName + ", status=" + status + ", createdBy=" + createdBy;
                changeHistoryManager.saveChangeToHistory("eventProposals", eventId, "changed", eventTimestamp != null ? eventTimestamp : System.currentTimeMillis(), details);

                if (eventId == null || eventName == null || status == null || createdBy == null || eventTimestamp == null) {
                    Log.d(TAG, "Proposal status check skipped: Missing data.");
                    return;
                }

                if (!createdBy.equals(teacherId)) {
                    Log.d(TAG, "Proposal not created by this teacher: " + eventId);
                    return;
                }

                String lastStatusKey = "lastStatus_" + eventId;
                String lastStatusTimestampKey = "lastStatusTimestamp_" + eventId;
                String lastStatus = sharedPreferences.getString(lastStatusKey, "pending");
                long lastStatusTimestamp = sharedPreferences.getLong(lastStatusTimestampKey, 0);

                sharedPreferences.edit().putString(lastStatusKey, status).putLong(lastStatusTimestampKey, eventTimestamp).apply();

                if (status.equalsIgnoreCase("rejected") && lastStatus.equalsIgnoreCase("pending") && eventTimestamp > lastStatusTimestamp && !hasNotified(teacherId, "proposalRejected_" + eventId + "_" + eventTimestamp)) {
                    String rejectionNotifyKey = "proposalRejected_" + eventId + "_" + eventTimestamp;
                    sendNotification(rejectionNotifyKey, "Event Proposal Rejected", "Your proposed event \"" + eventName + "\" has been rejected.", eventId, description, null);
                    markAsNotified(teacherId, rejectionNotifyKey);
                    Log.d(TAG, "Sent proposal rejected notification for: " + eventName);
                } else if (status.equalsIgnoreCase("pending") && lastStatus.equalsIgnoreCase("rejected") && eventTimestamp > lastStatusTimestamp && !hasNotified(teacherId, "proposalRestored_" + eventId + "_" + eventTimestamp)) {
                    String restorationNotifyKey = "proposalRestored_" + eventId + "_" + eventTimestamp;
                    sendNotification(restorationNotifyKey, "Event Proposal Restored", "Your proposed event \"" + eventName + "\" has been restored to pending status.", eventId, description, null);
                    markAsNotified(teacherId, restorationNotifyKey);
                    Log.d(TAG, "Sent proposal restored notification for: " + eventName);
                }
            }

            @Override
            public void onChildRemoved(@NonNull DataSnapshot snapshot) {
                String eventId = snapshot.getKey();
                String eventName = snapshot.child("eventName").getValue(String.class);
                Long eventTimestamp = snapshot.child("timestamp").getValue(Long.class);
                String details = "eventName=" + eventName;
                changeHistoryManager.saveChangeToHistory("eventProposals", eventId, "removed", eventTimestamp != null ? eventTimestamp : System.currentTimeMillis(), details);
            }

            @Override
            public void onChildMoved(@NonNull DataSnapshot snapshot, String previousChildName) {}

            @Override
            public void onCancelled(@NonNull DatabaseError error) {
                Log.e(TAG, "Event proposals listener error: " + error.getMessage());
            }
        });
    }

    private void listenForEditRequests() {
        String teacherId = sharedPreferences.getString("teacherId", null);
        if (teacherId == null || teacherId.trim().isEmpty()) {
            Log.d(TAG, "Teacher ID not found in SharedPreferences. Edit request listener not started.");
            return;
        }

        DatabaseReference editRequestsRef = FirebaseDatabase.getInstance().getReference("editRequests");

        editRequestsRef.addChildEventListener(new ChildEventListener() {
            @Override
            public void onChildAdded(@NonNull DataSnapshot snapshot, String previousChildName) {
                String eventId = snapshot.getKey();
                String status = snapshot.child("status").getValue(String.class);
                Long timestamp = snapshot.child("timestamp").getValue(Long.class);
                String requestedBy = snapshot.child("teacherId").getValue(String.class);

                String details = "eventId=" + eventId + ", status=" + status + ", requestedBy=" + requestedBy;
                changeHistoryManager.saveChangeToHistory("editRequests", eventId, "added", timestamp != null ? timestamp : System.currentTimeMillis(), details);
                Log.d(TAG, "Edit request added: ID=" + eventId + ", Details=" + details);
            }

            @Override
            public void onChildChanged(@NonNull DataSnapshot snapshot, String previousChildName) {
                String eventId = snapshot.getKey();
                String status = snapshot.child("status").getValue(String.class);
                String requestedBy = snapshot.child("teacherId").getValue(String.class);
                Long timestamp = snapshot.child("timestamp").getValue(Long.class);

                String details = "eventId=" + eventId + ", status=" + status + ", requestedBy=" + requestedBy;
                changeHistoryManager.saveChangeToHistory("editRequests", eventId, "changed", timestamp != null ? timestamp : System.currentTimeMillis(), details);
                Log.d(TAG, "Edit request changed: ID=" + eventId + ", Details=" + details);

                if (eventId == null || status == null || requestedBy == null || timestamp == null) {
                    Log.e(TAG, "Edit request status check skipped: Missing data. ID=" + eventId + ", status=" + status + ", requestedBy=" + requestedBy + ", timestamp=" + timestamp);
                    return;
                }

                if (!requestedBy.equals(teacherId)) {
                    Log.d(TAG, "Edit request not created by this teacher: eventId=" + eventId + ", teacherId=" + teacherId + ", requestedBy=" + requestedBy);
                    return;
                }

                String lastStatusKey = "lastEditRequestStatus_" + eventId;
                String lastStatusTimestampKey = "lastEditRequestTimestamp_" + eventId;
                String lastStatus = sharedPreferences.getString(lastStatusKey, "pending");
                long lastStatusTimestamp = sharedPreferences.getLong(lastStatusTimestampKey, 0);

                sharedPreferences.edit()
                        .putString(lastStatusKey, status)
                        .putLong(lastStatusTimestampKey, timestamp)
                        .apply();
                Log.d(TAG, "Updated status for eventId=" + eventId + ", newStatus=" + status + ", newTimestamp=" + timestamp + ", lastStatus=" + lastStatus + ", lastTimestamp=" + lastStatusTimestamp);

                // Fetch eventName for notification
                DatabaseReference eventsRef = FirebaseDatabase.getInstance().getReference("events").child(eventId);
                eventsRef.addListenerForSingleValueEvent(new ValueEventListener() {
                    @Override
                    public void onDataChange(@NonNull DataSnapshot eventSnapshot) {
                        String eventName = eventSnapshot.child("eventName").getValue(String.class);
                        String description = eventSnapshot.child("description").getValue(String.class);
                        String startDate = eventSnapshot.child("startDate").getValue(String.class);
                        String startTime = eventSnapshot.child("startTime").getValue(String.class);
                        String startDateTimeStr = (startDate != null && startTime != null) ? startDate + " " + startTime : null;

                        if (eventName == null) {
                            Log.w(TAG, "Event name not found for eventId=" + eventId + ", using fallback name 'Event'");
                            eventName = "Event";
                        } else {
                            Log.d(TAG, "Fetched event details for eventId=" + eventId + ", eventName=" + eventName);
                        }

                        if (status.equalsIgnoreCase("approved") && !lastStatus.equalsIgnoreCase("approved") && !hasNotified(teacherId, "editRequestApproved_" + eventId + "_" + timestamp)) {
                            String notifyKey = "editRequestApproved_" + eventId + "_" + timestamp;
                            sendNotification(notifyKey, "Edit Request Approved", "Your request to edit \"" + eventName + "\" has been approved! You can now edit the event.", eventId, description, startDateTimeStr);
                            markAsNotified(teacherId, notifyKey);
                            Log.d(TAG, "Notification sent: editRequestApproved, notifyKey=" + notifyKey + ", eventName=" + eventName + ", teacherId=" + teacherId);
                        } else if (status.equalsIgnoreCase("denied") && !lastStatus.equalsIgnoreCase("denied") && !hasNotified(teacherId, "editRequestDenied_" + eventId + "_" + timestamp)) {
                            String notifyKey = "editRequestDenied_" + eventId + "_" + timestamp;
                            sendNotification(notifyKey, "Edit Request Denied", "Your request to edit \"" + eventName + "\" has been denied. You cannot edit the event.", eventId, description, startDateTimeStr);
                            markAsNotified(teacherId, notifyKey);
                            Log.d(TAG, "Notification sent: editRequestDenied, notifyKey=" + notifyKey + ", eventName=" + eventName + ", teacherId=" + teacherId);
                        } else {
                            Log.d(TAG, "No notification sent for eventId=" + eventId + ": status=" + status + ", lastStatus=" + lastStatus + ", timestamp=" + timestamp + ", lastTimestamp=" + lastStatusTimestamp + ", hasNotified=" + hasNotified(teacherId, "editRequestApproved_" + eventId + "_" + timestamp) + "/" + hasNotified(teacherId, "editRequestDenied_" + eventId + "_" + timestamp));
                        }
                    }

                    @Override
                    public void onCancelled(@NonNull DatabaseError error) {
                        Log.e(TAG, "Failed to fetch event details for edit request notification: eventId=" + eventId + ", error=" + error.getMessage());
                    }
                });
            }

            @Override
            public void onChildRemoved(@NonNull DataSnapshot snapshot) {
                String eventId = snapshot.getKey();
                String status = snapshot.child("status").getValue(String.class);
                Long timestamp = snapshot.child("timestamp").getValue(Long.class);
                String requestedBy = snapshot.child("teacherId").getValue(String.class);

                String details = "eventId=" + eventId + ", status=" + status + ", requestedBy=" + requestedBy;
                changeHistoryManager.saveChangeToHistory("editRequests", eventId, "removed", timestamp != null ? timestamp : System.currentTimeMillis(), details);
                Log.d(TAG, "Edit request removed: ID=" + eventId + ", Details=" + details);
            }

            @Override
            public void onChildMoved(@NonNull DataSnapshot snapshot, String previousChildName) {}

            @Override
            public void onCancelled(@NonNull DatabaseError error) {
                Log.e(TAG, "Edit requests listener error: " + error.getMessage());
            }
        });
    }


    private String getUserId() {
        String studentId = sharedPreferences.getString("studentID", null);
        String teacherId = sharedPreferences.getString("teacherId", null);
        return studentId != null ? studentId : teacherId;
    }

    private void sendNotification(String notificationId, String title, String message, String eventId, String description, String startDateTime) {
        String userId = getUserId();
        if (userId == null) {
            Log.e(TAG, "sendNotification: No user ID found, cannot send notification");
            return;
        }

        NotificationManager notificationManager = (NotificationManager) getSystemService(NOTIFICATION_SERVICE);

        // Create intent for the notification tap (open MainActivity with eventId)
        Intent intent = new Intent(this, MainActivity.class);
        if (eventId != null) {
            intent.putExtra("eventId", eventId); // Pass eventId to MainActivity
        }
        PendingIntent contentIntent = PendingIntent.getActivity(
                this, (int) System.currentTimeMillis(), intent, PendingIntent.FLAG_UPDATE_CURRENT | PendingIntent.FLAG_IMMUTABLE
        );

        // Create NotificationCompat.Builder with default system layout
        NotificationCompat.Builder builder = new NotificationCompat.Builder(this, EVENT_CHANNEL_ID)
                .setSmallIcon(R.drawable.ic_notification)
                .setContentTitle(title)
                .setContentText(message)
                .setAutoCancel(true)
                .setPriority(NotificationCompat.PRIORITY_HIGH)
                .setContentIntent(contentIntent)
                .setGroup("EVENT_" + (eventId != null ? eventId : "GENERAL")) // Group notifications by eventId
                .setGroupSummary(false);

        // Set up group summary notification
        if (eventId != null) {
            NotificationCompat.Builder summaryBuilder = new NotificationCompat.Builder(this, EVENT_CHANNEL_ID)
                    .setSmallIcon(R.drawable.ic_notification)
                    .setContentTitle("Event Notifications")
                    .setContentText("You have new notifications for events")
                    .setGroup("EVENT_" + eventId)
                    .setGroupSummary(true)
                    .setAutoCancel(true);
            notificationManager.notify("EVENT_" + eventId, 0, summaryBuilder.build());
        }

        // Notify with a unique ID
        notificationManager.notify(notificationId, (int) System.currentTimeMillis(), builder.build());

        // Save to notification history
        saveNotificationToHistory(userId, notificationId, title, message);
        Log.d(TAG, "Notification sent: " + title);
    }

    private boolean hasNotified(String userId, String notificationId) {
        if (userId == null) return false;
        Set<String> notifiedIds = sharedPreferences.getStringSet(NOTIFIED_IDS_KEY + userId, new HashSet<>());
        return notifiedIds.contains(notificationId);
    }

    private void markAsNotified(String userId, String notificationId) {
        if (userId == null) return;
        Set<String> notifiedIds = new HashSet<>(sharedPreferences.getStringSet(NOTIFIED_IDS_KEY + userId, new HashSet<>()));
        notifiedIds.add(notificationId);
        sharedPreferences.edit().putStringSet(NOTIFIED_IDS_KEY + userId, notifiedIds).commit();
        Log.d(TAG, "Marked as notified: " + notificationId + " for userId: " + userId);
    }

    private void saveNotificationToHistory(String userId, String notificationId, String title, String message) {
        if (userId == null) {
            Log.e(TAG, "saveNotificationToHistory: No user ID found");
            return;
        }

        List<NotificationItem> history = getNotificationHistory(userId);
        history.add(0, new NotificationItem(notificationId, title, message, System.currentTimeMillis()));

        if (history.size() > MAX_HISTORY_SIZE) {
            history = history.subList(0, MAX_HISTORY_SIZE);
        }

        String json = gson.toJson(history);
        sharedPreferences.edit().putString(NOTIFICATION_HISTORY_KEY + userId, json).commit();
        Log.d(TAG, "Saved notification to history: " + notificationId);
    }

    private List<NotificationItem> getNotificationHistory(String userId) {
        if (userId == null) return new ArrayList<>();
        String json = sharedPreferences.getString(NOTIFICATION_HISTORY_KEY + userId, null);
        if (json == null) return new ArrayList<>();
        Type type = new TypeToken<List<NotificationItem>>(){}.getType();
        try {
            List<NotificationItem> history = gson.fromJson(json, type);
            return history != null ? history : new ArrayList<>();
        } catch (Exception e) {
            Log.e(TAG, "Failed to parse notification history: " + e.getMessage());
            return new ArrayList<>();
        }
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
        if (intent != null && intent.getBooleanExtra("processMissed", false)) {
            String userId = getUserId();
            if (userId != null) {
                Log.d(TAG, "Processing missed notifications for user: " + userId);
                processMissedNotifications(userId);
            } else {
                Log.e(TAG, "Cannot process missed notifications: userId is null");
            }
        }
        return START_STICKY;
    }

    @Override
    public void onDestroy() {
        super.onDestroy();
        if (handler != null) {
            handler.removeCallbacks(checkExpiredEventsRunnable);
        }
        Log.d(TAG, "NotificationService Destroyed");
    }
}
