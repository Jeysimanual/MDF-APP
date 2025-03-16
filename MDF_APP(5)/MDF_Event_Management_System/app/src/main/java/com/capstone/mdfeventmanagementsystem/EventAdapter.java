package com.capstone.mdfeventmanagementsystem;

import android.content.Context;
import android.content.Intent;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;

import java.util.List;

public class EventAdapter extends RecyclerView.Adapter<EventAdapter.EventViewHolder> {

    private List<Event> eventList;
    private Context context;
    private OnEventClickListener eventClickListener;

    // Interface for event click listener
    public interface OnEventClickListener {
        void onEventClick(Event event);
    }

    public EventAdapter(Context context, List<Event> eventList, OnEventClickListener listener) {
        this.context = context;
        this.eventList = eventList;
        this.eventClickListener = listener;
    }

    @NonNull
    @Override
    public EventViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        View view = LayoutInflater.from(parent.getContext()).inflate(R.layout.item_events, parent, false);
        return new EventViewHolder(view);
    }

    @Override
    public void onBindViewHolder(@NonNull EventViewHolder holder, int position) {
        Event event = eventList.get(position);

        holder.eventName.setText(event.getEventName());
        holder.venue.setText(event.getVenue());
        holder.dateCreated.setText("Posted: " + event.getDateCreated());
        holder.startDate.setText("Start: " + event.getStartDate());
        holder.endDate.setText("End  : " + event.getEndDate());
        holder.startTime.setText(event.getStartTime());
        holder.endTime.setText(event.getEndTime());
        holder.dayOfMonth.setText(event.getDayOfMonth());
        holder.month.setText(event.getMonthShort());

        // Handle click
        holder.itemView.setOnClickListener(v -> {
            Log.d("TestApp", "Clicked on event: " + event.getEventName() + " (UID: " + event.getEventUID() + ")");
            if (eventClickListener != null) {
                eventClickListener.onEventClick(event);
            }
        });
    }

    @Override
    public int getItemCount() {
        return (eventList != null) ? eventList.size() : 0;
    }

    public void updateEventList(List<Event> newEventList) {
        this.eventList = newEventList;
        notifyDataSetChanged();
    }

    public static class EventViewHolder extends RecyclerView.ViewHolder {
        TextView eventName, venue, dateCreated, startDate, endDate, startTime, endTime;
        TextView dayOfMonth, month;

        public EventViewHolder(@NonNull View itemView) {
            super(itemView);
            eventName = itemView.findViewById(R.id.eventName);
            venue = itemView.findViewById(R.id.venue);
            dateCreated = itemView.findViewById(R.id.dateCreated);
            startDate = itemView.findViewById(R.id.startDate);
            endDate = itemView.findViewById(R.id.endDate);
            startTime = itemView.findViewById(R.id.startTime);
            endTime = itemView.findViewById(R.id.endTime);
            dayOfMonth = itemView.findViewById(R.id.dayOfMonth);
            month = itemView.findViewById(R.id.month);
        }
    }
}
