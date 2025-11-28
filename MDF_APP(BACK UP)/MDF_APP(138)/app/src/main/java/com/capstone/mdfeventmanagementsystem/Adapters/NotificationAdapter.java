package com.capstone.mdfeventmanagementsystem.Adapters;

import android.content.Context;
import android.content.Intent;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;

import com.capstone.mdfeventmanagementsystem.Models.Notification;
import com.capstone.mdfeventmanagementsystem.R;
import com.capstone.mdfeventmanagementsystem.Student.StudentDashboardInside;
import com.capstone.mdfeventmanagementsystem.Student.StudentCertificate;
import com.capstone.mdfeventmanagementsystem.Student.StudentTickets;
import com.capstone.mdfeventmanagementsystem.Student.QRCheckInActivity;
import com.capstone.mdfeventmanagementsystem.Teacher.TeacherEvents;

import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Date;
import java.util.List;
import java.util.Locale;

public class NotificationAdapter extends RecyclerView.Adapter<NotificationAdapter.NotificationViewHolder> {

    private static final String TAG = "NotificationAdapter";
    private List<Notification> notificationList;
    private final Context context;
    private final OnNotificationClickListener clickListener;
    private final boolean isTeacher; // Add this flag to distinguish between student and teacher

    // Interface for click handling
    public interface OnNotificationClickListener {
        void onNotificationClick(Notification notification);
    }

    // Updated constructor with isTeacher flag
    public NotificationAdapter(Context context, List<Notification> notificationList,
                               OnNotificationClickListener clickListener, boolean isTeacher) {
        this.context = context;
        this.notificationList = notificationList != null ? notificationList : new ArrayList<>();
        this.clickListener = clickListener;
        this.isTeacher = isTeacher;
        Log.d(TAG, "NotificationAdapter initialized with " + this.notificationList.size() +
                " notifications, isTeacher: " + isTeacher);
    }

    // Keep old constructor for backward compatibility
    public NotificationAdapter(Context context, List<Notification> notificationList,
                               OnNotificationClickListener clickListener) {
        this(context, notificationList, clickListener, false);
    }

    public void updateNotificationList(List<Notification> newList) {
        this.notificationList = newList != null ? newList : new ArrayList<>();
        Log.d(TAG, "Updated notification list with " + this.notificationList.size() + " items");
        notifyDataSetChanged();
    }

    @NonNull
    @Override
    public NotificationViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        View view = LayoutInflater.from(context).inflate(R.layout.item_notification, parent, false);
        Log.d(TAG, "Creating ViewHolder for notification item");
        return new NotificationViewHolder(view);
    }

    @Override
    public void onBindViewHolder(@NonNull NotificationViewHolder holder, int position) {
        Notification notification = notificationList.get(position);
        Log.d(TAG, "Binding notification at position " + position +
                ": Title=" + notification.getTitle() +
                ", EventId=" + notification.getEventId() +
                ", Type=" + notification.getType());

        holder.titleTextView.setText(notification.getTitle());
        holder.bodyTextView.setText(notification.getBody());

        // Format timestamp
        try {
            SimpleDateFormat sdf = new SimpleDateFormat("MMM d, yyyy h:mm a", Locale.getDefault());
            String formattedDate = sdf.format(new Date(notification.getTimestamp()));
            holder.timestampTextView.setText(formattedDate);
            Log.d(TAG, "Formatted timestamp for notification: " + formattedDate);
        } catch (Exception e) {
            holder.timestampTextView.setText("Unknown time");
            Log.e(TAG, "Error formatting timestamp for notification at position " + position, e);
        }

        // Set click listener for the notification item
        holder.itemView.setOnClickListener(v -> {
            String eventId = notification.getEventId();
            String notificationType = notification.getType();
            Log.d(TAG, "Notification clicked at position " + position +
                    ": EventId=" + eventId +
                    ", Type=" + notificationType +
                    ", isTeacher=" + isTeacher);

            if (clickListener != null) {
                clickListener.onNotificationClick(notification);
                Log.d(TAG, "Triggered click listener for notification with eventId: " + eventId);
            } else {
                // Handle navigation based on notification type and user role
                handleNotificationNavigation(notification, eventId, notificationType);
            }
        });
    }

    private void handleNotificationNavigation(Notification notification, String eventId, String notificationType) {
        Intent intent = null;

        if (notificationType == null) {
            Log.w(TAG, "Notification type is null, using default navigation");
            // Fallback to default behavior based on user role
            if (eventId != null && !eventId.isEmpty()) {
                if (isTeacher) {
                    intent = new Intent(context, TeacherEvents.class);
                } else {
                    intent = new Intent(context, StudentDashboardInside.class);
                    intent.putExtra("eventUID", eventId);
                }
            }
        } else {
            if (isTeacher) {
                // TEACHER-SPECIFIC NAVIGATION
                switch (notificationType) {
                    // Event-related notifications - go to TeacherEvents
                    case "eventCreated":
                    case "eventReminder1Days":
                    case "eventReminder2Days":
                    case "eventReminder3Days":
                    case "eventOngoing":
                    case "eventEnded":
                        intent = new Intent(context, TeacherEvents.class);
                        Log.d(TAG, "Teacher: Navigating to TeacherEvents for notification type: " + notificationType);
                        break;

                    // Proposal-related notifications - go to EventApprovalFragment
                    case "proposalApproved":
                    case "proposalRejected":
                    case "proposalRestored":
                    case "proposalReasonUpdated":
                        // Navigate to TeacherEvents with a flag to open EventApprovalFragment
                        intent = new Intent(context, TeacherEvents.class);
                        intent.putExtra("openFragment", "EventApprovalFragment");
                        intent.putExtra("eventUID", eventId); // Pass eventId if available
                        Log.d(TAG, "Teacher: Navigating to TeacherEvents with EventApprovalFragment for proposal: " + notificationType);
                        break;

                    // Edit request notifications - go to TeacherEvents
                    case "editRequestApproved":
                    case "editRequestDenied":
                        intent = new Intent(context, TeacherEvents.class);
                        Log.d(TAG, "Teacher: Navigating to TeacherEvents for edit request: " + notificationType);
                        break;

                    // Default case for teachers
                    default:
                        Log.w(TAG, "Teacher: Unknown notification type: " + notificationType + ", using default navigation");
                        intent = new Intent(context, TeacherEvents.class);
                        break;
                }
            } else {
                // STUDENT-SPECIFIC NAVIGATION (existing logic)
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
                            intent = new Intent(context, StudentDashboardInside.class);
                            intent.putExtra("eventUID", eventId);
                            Log.d(TAG, "Student: Navigating to StudentDashboardInside for event: " + eventId);
                        }
                        break;

                    // Certificate notifications - go to certificates
                    case "certificateAwarded":
                        intent = new Intent(context, StudentCertificate.class);
                        if (eventId != null && !eventId.isEmpty()) {
                            intent.putExtra("eventUID", eventId);
                        }
                        Log.d(TAG, "Student: Navigating to StudentCertificate for event: " + eventId);
                        break;

                    // Student Assistant notifications - go to tickets
                    case "studentAssistantAdded":
                    case "studentAssistantRemoved":
                    case "coordinatorRemoved":
                        intent = new Intent(context, StudentTickets.class);
                        if (eventId != null && !eventId.isEmpty()) {
                            intent.putExtra("eventUID", eventId);
                        }
                        Log.d(TAG, "Student: Navigating to StudentTickets for event: " + eventId);
                        break;

                    // Scan permission notifications - go to QR checkin
                    case "scanPermissionAllowed":
                    case "scanPermissionClosed":
                        intent = new Intent(context, QRCheckInActivity.class);
                        if (eventId != null && !eventId.isEmpty()) {
                            intent.putExtra("eventUID", eventId);
                        }
                        Log.d(TAG, "Student: Navigating to QRCheckinActivity for event: " + eventId);
                        break;

                    // Default case for students
                    default:
                        Log.w(TAG, "Student: Unknown notification type: " + notificationType + ", using default navigation");
                        if (eventId != null && !eventId.isEmpty()) {
                            intent = new Intent(context, StudentDashboardInside.class);
                            intent.putExtra("eventUID", eventId);
                        }
                        break;
                }
            }
        }

        if (intent != null) {
            // Add notification data to intent for potential use in target activity
            intent.putExtra("notificationTitle", notification.getTitle());
            intent.putExtra("notificationBody", notification.getBody());
            intent.putExtra("notificationType", notificationType);
            intent.putExtra("notificationTimestamp", notification.getTimestamp());

            // For teacher events, you might want to pass the eventId if available
            if (eventId != null && !eventId.isEmpty()) {
                intent.putExtra("eventUID", eventId);
            }

            context.startActivity(intent);
            Log.d(TAG, "Started activity with intent for notification type: " + notificationType +
                    ", isTeacher: " + isTeacher);
        } else {
            Log.w(TAG, "No valid navigation intent created for notification");
            Toast.makeText(context, "No destination available for this notification", Toast.LENGTH_SHORT).show();
        }
    }

    @Override
    public int getItemCount() {
        return notificationList.size();
    }

    static class NotificationViewHolder extends RecyclerView.ViewHolder {
        TextView titleTextView, bodyTextView, timestampTextView;

        NotificationViewHolder(@NonNull View itemView) {
            super(itemView);
            titleTextView = itemView.findViewById(R.id.notification_title);
            bodyTextView = itemView.findViewById(R.id.notification_body);
            timestampTextView = itemView.findViewById(R.id.notification_timestamp);
            Log.d(TAG, "NotificationViewHolder initialized");
        }
    }
}