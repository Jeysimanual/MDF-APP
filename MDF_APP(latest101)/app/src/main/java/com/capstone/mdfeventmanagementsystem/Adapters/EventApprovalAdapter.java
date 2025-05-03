package com.capstone.mdfeventmanagementsystem.Adapters;

import android.content.Context;
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

import java.util.List;

public class EventApprovalAdapter extends RecyclerView.Adapter<EventApprovalAdapter.EventViewHolder> {

    private List<Event> pendingEvents;
    private Context context;

    public EventApprovalAdapter(Context context, List<Event> pendingEvents) {
        this.context = context;
        this.pendingEvents = pendingEvents;
    }

    @NonNull
    @Override
    public EventViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        View view = LayoutInflater.from(parent.getContext()).inflate(R.layout.item_events, parent, false);
        return new EventViewHolder(view);
    }

    @Override
    public void onBindViewHolder(@NonNull EventViewHolder holder, int position) {
        Event event = pendingEvents.get(position);

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

        // Since we're in the EventApprovalAdapter and we're only showing pending events,
        // we can safely make the status indicator visible for all events
        holder.statusIndicator.setVisibility(View.VISIBLE);

        // No need to update the text as it's already set to "PENDING" in the layout
    }

    @Override
    public int getItemCount() {
        return pendingEvents != null ? pendingEvents.size() : 0;
    }

    // Helper method to extract day from date string (assuming format is "MM-DD-YYYY")
    private String extractDayOfMonth(String dateString) {
        if (dateString != null && dateString.length() >= 5) {
            String[] parts = dateString.split("-");
            if (parts.length >= 2) {
                return parts[1]; // Day is the second part
            }
        }
        return "";
    }

    // Helper method to extract month from date string (assuming format is "MM-DD-YYYY")
    private String extractMonth(String dateString) {
        if (dateString != null && dateString.length() >= 2) {
            String[] parts = dateString.split("-");
            if (parts.length >= 1) {
                // Convert month number to month name
                String monthNumber = parts[0];
                switch (monthNumber) {
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
        }
        return "";
    }

    public void updateEvents(List<Event> newEvents) {
        this.pendingEvents = newEvents;
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

            // We don't need to get the TextView inside statusIndicator
            // The text is already set in the layout
        }
    }
}