package com.capstone.mdfeventmanagementsystem.Utilities;

import android.app.NotificationChannel;
import android.app.NotificationManager;
import android.app.PendingIntent;
import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.media.RingtoneManager;
import android.os.Build;
import android.util.Log;

import androidx.core.app.NotificationCompat;

import com.capstone.mdfeventmanagementsystem.R;
import com.capstone.mdfeventmanagementsystem.Student.MainActivity2;
import com.capstone.mdfeventmanagementsystem.Student.StudentDashboardInside;
import com.capstone.mdfeventmanagementsystem.Student.StudentCertificate;
import com.capstone.mdfeventmanagementsystem.Student.StudentTickets;
import com.capstone.mdfeventmanagementsystem.Student.QRCheckInActivity;
import com.capstone.mdfeventmanagementsystem.Teacher.TeacherDashboard;
import com.capstone.mdfeventmanagementsystem.Teacher.TeacherEvents;
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
        Log.d(TAG, "Message received from: " + remoteMessage.getFrom());

        String title = null;
        String body = null;
        String eventId = null;
        String notificationType = null;
        String description = null;
        String startDateTime = null;

        // Read from data payload (your cloud functions send data in the payload)
        if (remoteMessage.getData().size() > 0) {
            Log.d(TAG, "Message data payload: " + remoteMessage.getData());

            title = remoteMessage.getData().get("title");
            body = remoteMessage.getData().get("body");
            eventId = remoteMessage.getData().get("eventId");
            notificationType = remoteMessage.getData().get("type"); // Extract notification type
            description = remoteMessage.getData().get("description");
            startDateTime = remoteMessage.getData().get("startDateTime");
        }

        // Fallback to notification payload if data is not available
        if (remoteMessage.getNotification() != null) {
            if (title == null) title = remoteMessage.getNotification().getTitle();
            if (body == null) body = remoteMessage.getNotification().getBody();
        }

        if (title != null || body != null) {
            sendNotification(title, body, eventId, notificationType, description, startDateTime);
        }
    }

    private void sendNotification(String title, String body, String eventId, String notificationType, String description, String startDateTime) {
        createNotificationChannel();

        // Determine which activity to launch based on user role and notification type
        Intent intent = createNavigationIntent(title, body, eventId, notificationType, description, startDateTime);
        intent.addFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP | Intent.FLAG_ACTIVITY_NEW_TASK);

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
        if (manager != null) {
            manager.notify(requestCode, builder.build());
            Log.d(TAG, "Notification sent with type: " + notificationType + ", eventId: " + eventId);
        }
    }

    private Intent createNavigationIntent(String title, String body, String eventId, String notificationType, String description, String startDateTime) {
        // Check if user is teacher or student
        boolean isTeacher = isUserTeacher();
        Log.d(TAG, "Creating navigation intent - isTeacher: " + isTeacher + ", notificationType: " + notificationType);

        Intent intent;

        if (isTeacher) {
            // TEACHER NAVIGATION LOGIC
            if (notificationType != null) {
                switch (notificationType) {
                    // Proposal-related notifications - go to TeacherEvents with EventApprovalFragment
                    case "proposalApproved":
                    case "proposalRejected":
                    case "proposalRestored":
                    case "proposalReasonUpdated":
                        intent = new Intent(this, TeacherEvents.class);
                        intent.putExtra("openFragment", "EventApprovalFragment");
                        Log.d(TAG, "Teacher: Navigating to TeacherEvents with EventApprovalFragment");
                        break;

                    // Event-related notifications - go to TeacherEvents
                    case "eventCreated":
                    case "eventReminder1Days":
                    case "eventReminder2Days":
                    case "eventReminder3Days":
                    case "eventOngoing":
                    case "eventEnded":
                    case "editRequestApproved":
                    case "editRequestDenied":
                        intent = new Intent(this, TeacherEvents.class);
                        Log.d(TAG, "Teacher: Navigating to TeacherEvents");
                        break;

                    // Default for teachers
                    default:
                        intent = new Intent(this, TeacherDashboard.class);
                        Log.d(TAG, "Teacher: Default navigation to TeacherDashboard");
                        break;
                }
            } else {
                // No notification type - default to TeacherDashboard
                intent = new Intent(this, TeacherDashboard.class);
            }
        } else {
            // STUDENT NAVIGATION LOGIC
            if (notificationType != null) {
                switch (notificationType) {
                    // Event-related notifications - go to event details
                    case "eventCreated":
                    case "registrationOpen":
                    case "registrationClosed":
                    case "eventReminder1Days":
                    case "eventReminder2Days":
                    case "eventReminder3Days":
                    case "eventOngoing":
                    case "eventEnded":
                        if (eventId != null && !eventId.isEmpty()) {
                            intent = new Intent(this, StudentDashboardInside.class);
                            intent.putExtra("eventUID", eventId);
                            Log.d(TAG, "Student: Navigating to StudentDashboardInside for event: " + eventId);
                        } else {
                            intent = new Intent(this, MainActivity2.class);
                        }
                        break;

                    // Certificate notifications - go to certificates
                    case "certificateAwarded":
                        intent = new Intent(this, StudentCertificate.class);
                        if (eventId != null && !eventId.isEmpty()) {
                            intent.putExtra("eventUID", eventId);
                        }
                        Log.d(TAG, "Student: Navigating to StudentCertificate");
                        break;

                    // Student Assistant notifications - go to tickets
                    case "studentAssistantAdded":
                    case "studentAssistantRemoved":
                    case "coordinatorRemoved":
                        intent = new Intent(this, StudentTickets.class);
                        if (eventId != null && !eventId.isEmpty()) {
                            intent.putExtra("eventUID", eventId);
                        }
                        Log.d(TAG, "Student: Navigating to StudentTickets");
                        break;

                    // Scan permission notifications - go to QR checkin
                    case "scanPermissionAllowed":
                    case "scanPermissionClosed":
                        intent = new Intent(this, QRCheckInActivity.class);
                        if (eventId != null && !eventId.isEmpty()) {
                            intent.putExtra("eventUID", eventId);
                        }
                        Log.d(TAG, "Student: Navigating to QRCheckInActivity");
                        break;

                    // Default for students
                    default:
                        intent = new Intent(this, MainActivity2.class);
                        Log.d(TAG, "Student: Default navigation to MainActivity2");
                        break;
                }
            } else {
                // No notification type - default to MainActivity2
                intent = new Intent(this, MainActivity2.class);
            }
        }

        // Add common notification data
        intent.putExtra("from_notification", true);
        intent.putExtra("notificationTitle", title);
        intent.putExtra("notificationBody", body);
        intent.putExtra("notificationType", notificationType);

        if (eventId != null && !eventId.isEmpty()) {
            intent.putExtra("eventUID", eventId);
        }
        if (description != null) {
            intent.putExtra("description", description);
        }
        if (startDateTime != null) {
            intent.putExtra("startDateTime", startDateTime);
        }

        return intent;
    }

    private boolean isUserTeacher() {
        // Check SharedPreferences to determine if user is teacher or student
        SharedPreferences prefs = getSharedPreferences("UserSession", MODE_PRIVATE);

        // Check if teacher ID exists
        String teacherId = prefs.getString("teacherId", null);
        if (teacherId != null && !teacherId.isEmpty()) {
            Log.d(TAG, "User identified as teacher with ID: " + teacherId);
            return true;
        }

        // Check if student ID exists
        String studentId = prefs.getString("studentID", null);
        if (studentId != null && !studentId.isEmpty()) {
            Log.d(TAG, "User identified as student with ID: " + studentId);
            return false;
        }

        // Default to student if no session found (shouldn't happen if user is logged in)
        Log.d(TAG, "No user session found, defaulting to student");
        return false;
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

        // You might want to send this token to your server here
        // updateTokenInDatabase(token);
    }
}