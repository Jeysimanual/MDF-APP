package com.capstone.mdfeventmanagementsystem.Adapters;

import android.content.Context;
import android.content.Intent;
import android.graphics.Typeface;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;

import com.capstone.mdfeventmanagementsystem.Models.NotificationItem;
import com.capstone.mdfeventmanagementsystem.R;
import com.capstone.mdfeventmanagementsystem.Teacher.TeacherEventsInside;

import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.List;

public class NotificationAdapter extends RecyclerView.Adapter<NotificationAdapter.NotificationViewHolder> {

    private List<NotificationItem> notificationList;
    private Context context;

    public NotificationAdapter(List<NotificationItem> notificationList) {
        this.notificationList = notificationList != null ? new ArrayList<>(notificationList) : new ArrayList<>();
    }

    @NonNull
    @Override
    public NotificationViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        View view = LayoutInflater.from(parent.getContext()).inflate(R.layout.item_notification, parent, false);
        context = parent.getContext(); // Store context for intent
        return new NotificationViewHolder(view);
    }

    @Override
    public void onBindViewHolder(@NonNull NotificationViewHolder holder, int position) {
        NotificationItem item = notificationList.get(position);
        String notificationText = "New event \"" + item.getEventName() + "\" has been posted at " + item.getVenue();
        holder.tvNotificationText.setText(notificationText);
        holder.tvDateTime.setText(item.getDateCreated() + item.getTime());

        // Apply styling based on isRead status
        if (!item.isRead()) {
            // Unread: Bold text and highlight background
            holder.tvNotificationText.setTypeface(null, Typeface.BOLD);
            holder.tvDateTime.setTypeface(null, Typeface.BOLD);
            holder.itemView.setBackgroundResource(R.drawable.notification_unread_background); // Define this drawable
        } else {
            // Read: Normal text and no highlight
            holder.tvNotificationText.setTypeface(null, Typeface.NORMAL);
            holder.tvDateTime.setTypeface(null, Typeface.NORMAL);
            holder.itemView.setBackgroundResource(android.R.color.transparent);
        }

        // Set click listener to navigate to TeacherEventsInside using eventId and other fields
        holder.itemView.setOnClickListener(v -> {
            Intent intent = new Intent(context, TeacherEventsInside.class);
            intent.putExtra("eventId", item.getEventId());
            intent.putExtra("eventName", item.getEventName());
            intent.putExtra("eventDescription", item.getEventDescription());
            intent.putExtra("eventPhotoUrl", item.getEventPhotoUrl());
            intent.putExtra("isRead", item.isRead());
            context.startActivity(intent);
        });
    }

    @Override
    public int getItemCount() {
        return notificationList != null ? notificationList.size() : 0;
    }

    public static class NotificationViewHolder extends RecyclerView.ViewHolder {
        TextView tvNotificationText, tvDateTime;

        public NotificationViewHolder(@NonNull View itemView) {
            super(itemView);
            tvNotificationText = itemView.findViewById(R.id.tvNotificationText);
            tvDateTime = itemView.findViewById(R.id.tvDateTime);
        }
    }

    public void updateNotifications(List<NotificationItem> newList) {
        this.notificationList.clear();
        if (newList != null) {
            // Sort notifications by timestamp in descending order (newest first)
            List<NotificationItem> sortedList = new ArrayList<>(newList);
            Collections.sort(sortedList, new Comparator<NotificationItem>() {
                @Override
                public int compare(NotificationItem item1, NotificationItem item2) {
                    return Long.compare(item2.getTimestamp(), item1.getTimestamp()); // Descending order
                }
            });
            this.notificationList.addAll(sortedList);
        }
        notifyDataSetChanged();
    }
}