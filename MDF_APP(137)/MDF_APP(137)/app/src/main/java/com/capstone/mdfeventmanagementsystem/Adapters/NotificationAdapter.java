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

    // Interface for click handling
    public interface OnNotificationClickListener {
        void onNotificationClick(Notification notification);
    }

    public NotificationAdapter(Context context, List<Notification> notificationList, OnNotificationClickListener clickListener) {
        this.context = context;
        this.notificationList = notificationList != null ? notificationList : new ArrayList<>();
        this.clickListener = clickListener;
        Log.d(TAG, "NotificationAdapter initialized with " + this.notificationList.size() + " notifications");
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
        Log.d(TAG, "Binding notification at position " + position + ": Title=" + notification.getTitle() + ", EventId=" + notification.getEventId());

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
            Log.d(TAG, "Notification clicked at position " + position + ": EventId=" + eventId);
            if (eventId != null && !eventId.isEmpty()) {
                if (clickListener != null) {
                    clickListener.onNotificationClick(notification);
                    Log.d(TAG, "Triggered click listener for notification with eventId: " + eventId);
                } else {
                    Log.w(TAG, "Click listener is null for notification at position " + position);
                    // Fallback to student navigation if no listener is provided
                    Intent intent = new Intent(context, StudentDashboardInside.class);
                    intent.putExtra("eventUID", eventId);
                    context.startActivity(intent);
                    Log.d(TAG, "Fallback: Starting StudentDashboardInside with eventUID: " + eventId);
                }
            } else {
                Log.w(TAG, "No eventId for notification at position " + position + ": Title=" + notification.getTitle());
                Toast.makeText(context, "No event associated with this notification", Toast.LENGTH_SHORT).show();
            }
        });
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