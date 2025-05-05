package com.capstone.mdfeventmanagementsystem.Adapters;

import android.content.Context;
import android.content.Intent;
import android.graphics.Color;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;
import android.widget.ImageView;

import androidx.annotation.NonNull;
import androidx.cardview.widget.CardView;
import androidx.recyclerview.widget.RecyclerView;

import com.capstone.mdfeventmanagementsystem.Models.Event;
import com.capstone.mdfeventmanagementsystem.R;
import com.capstone.mdfeventmanagementsystem.Teacher.EventApprovalInside;

import java.util.List;

public class EventApprovalAdapter extends RecyclerView.Adapter<EventApprovalAdapter.EventViewHolder> {

    private List<Event> events; // Renamed from pendingEvents to events
    private Context context;

    public EventApprovalAdapter(Context context, List<Event> events) {
        this.context = context;
        this.events = events;
    }

    @NonNull
    @Override
    public EventViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        View view = LayoutInflater.from(parent.getContext()).inflate(R.layout.item_events, parent, false);
        return new EventViewHolder(view);
    }

    @Override
    public void onBindViewHolder(@NonNull EventViewHolder holder, int position) {
        Event event = events.get(position);

        // Set event data to views
        holder.eventNameText.setText(event.getEventName());
        holder.venueText.setText(event.getVenue());
        holder.dayOfMonthText.setText(extractDayOfMonth(event.getStartDate()));
        holder.monthText.setText(extractMonth(event.getStartDate()));

        // Set dates and times
        holder.startDateText.setText("Start: " + event.getStartDate());
        holder.startTimeText.setText(event.getStartTime());
        holder.endDateText.setText("End: " + event.getEndDate());
        holder.endTimeText.setText(event.getEndTime());

        // Set date created
        holder.dateCreatedText.setText("Posted: " + event.getDateCreated());

        // Make the status indicator visible
        holder.statusIndicator.setVisibility(View.VISIBLE);

        // NEW: Visual indicator for rejected events
        if ("rejected".equals(event.getStatus())) {
            // If we have a status text view inside the status indicator, we can set it
            TextView statusText = holder.statusIndicator.findViewById(R.id.status_text);
            if (statusText != null) {
                statusText.setText("Rejected");
            }

            // Change the card's background color to indicate rejection
            holder.statusIndicator.setCardBackgroundColor(Color.parseColor("#FF0000")); // Light red
        } else {
            // Reset to default color for pending events
            TextView statusText = holder.statusIndicator.findViewById(R.id.status_text);
            if (statusText != null) {
                statusText.setText("Pending");
            }

            // Use default color for pending
            holder.statusIndicator.setCardBackgroundColor(Color.parseColor("#FFC107")); // Amber
        }

        // Set click listener on the card view
        holder.itemView.setOnClickListener(view -> {
            // Create intent to open EventApprovalInside activity
            Intent intent = new Intent(context, EventApprovalInside.class);

            // Pass event data to the activity
            intent.putExtra("EVENT_ID", event.getEventId());
            intent.putExtra("EVENT_NAME", event.getEventName());
            intent.putExtra("EVENT_DESCRIPTION", event.getDescription());
            intent.putExtra("EVENT_VENUE", event.getVenue());
            intent.putExtra("EVENT_START_DATE", event.getStartDate());
            intent.putExtra("EVENT_END_DATE", event.getEndDate());
            intent.putExtra("EVENT_START_TIME", event.getStartTime());
            intent.putExtra("EVENT_END_TIME", event.getEndTime());
            intent.putExtra("EVENT_DATE_CREATED", event.getDateCreated());
            intent.putExtra("EVENT_STATUS", event.getStatus());
            intent.putExtra("EVENT_PHOTO_URL", event.getPhotoUrl());
            intent.putExtra("EVENT_TYPE", event.getEventType());
            intent.putExtra("EVENT_FOR", event.getEventFor());
            intent.putExtra("EVENT_SPAN", event.getEventSpan());
            intent.putExtra("EVENT_GRACE_TIME", event.getGraceTime());

            // Add rejection reason if available
            if (event.getRejectionReason() != null && !event.getRejectionReason().isEmpty()) {
                intent.putExtra("EVENT_REJECTION_REASON", event.getRejectionReason());
            }

            // Start the activity
            context.startActivity(intent);
        });
    }

    @Override
    public int getItemCount() {
        return events != null ? events.size() : 0;
    }

    // Helper method to extract day from date string (assuming format is "MM-DD-YYYY")
    // Update these methods in your EventApprovalAdapter class:

    // Helper method to extract day from date string (handling both "MM-dd-yyyy" and "yyyy-MM-dd" formats)
    private String extractDayOfMonth(String dateString) {
        if (dateString == null || dateString.isEmpty()) {
            return "";
        }

        String[] parts;
        // Check which format we're dealing with
        if (dateString.matches("\\d{4}-\\d{2}-\\d{2}")) {
            // Format is yyyy-MM-dd
            parts = dateString.split("-");
            if (parts.length >= 3) {
                return parts[2]; // Day is the third part
            }
        } else if (dateString.matches("\\d{2}-\\d{2}-\\d{4}")) {
            // Format is MM-dd-yyyy
            parts = dateString.split("-");
            if (parts.length >= 2) {
                return parts[1]; // Day is the second part
            }
        }
        return "";
    }

    // Helper method to extract month from date string (handling both formats)
    private String extractMonth(String dateString) {
        if (dateString == null || dateString.isEmpty()) {
            return "";
        }

        String monthPart = "";
        // Check which format we're dealing with
        if (dateString.matches("\\d{4}-\\d{2}-\\d{2}")) {
            // Format is yyyy-MM-dd
            String[] parts = dateString.split("-");
            if (parts.length >= 2) {
                monthPart = parts[1]; // Month is the second part
            }
        } else if (dateString.matches("\\d{2}-\\d{2}-\\d{4}")) {
            // Format is MM-dd-yyyy
            String[] parts = dateString.split("-");
            if (parts.length >= 1) {
                monthPart = parts[0]; // Month is the first part
            }
        }

        // Convert month number to month name
        switch (monthPart) {
            case "01": return "Jan";
            case "02": return "Feb";
            case "03": return "Mar";
            case "04": return "Apr";
            case "05": return "May";
            case "06": return "Jun";
            case "07": return "Jul";
            case "08": return "Aug";
            case "09": return "Sep";
            case "10": return "Oct";
            case "11": return "Nov";
            case "12": return "Dec";
            default: return "";
        }
    }

    public void updateEvents(List<Event> newEvents) {
        this.events = newEvents;
        notifyDataSetChanged();
    }

    static class EventViewHolder extends RecyclerView.ViewHolder {
        TextView eventNameText;
        TextView venueText;
        TextView dayOfMonthText;
        TextView monthText;
        TextView startDateText;
        TextView startTimeText;
        TextView endDateText;
        TextView endTimeText;
        TextView dateCreatedText;
        CardView statusIndicator;

        public EventViewHolder(@NonNull View itemView) {
            super(itemView);

            // Initialize views from the item layout
            eventNameText = itemView.findViewById(R.id.eventName);
            venueText = itemView.findViewById(R.id.venue);
            dayOfMonthText = itemView.findViewById(R.id.dayOfMonth);
            monthText = itemView.findViewById(R.id.month);
            startDateText = itemView.findViewById(R.id.startDate);
            startTimeText = itemView.findViewById(R.id.startTime);
            endDateText = itemView.findViewById(R.id.endDate);
            endTimeText = itemView.findViewById(R.id.endTime);
            dateCreatedText = itemView.findViewById(R.id.dateCreated);
            statusIndicator = itemView.findViewById(R.id.status_indicator);
        }
    }
}