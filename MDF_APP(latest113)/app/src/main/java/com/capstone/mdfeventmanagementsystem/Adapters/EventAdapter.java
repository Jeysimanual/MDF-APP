package com.capstone.mdfeventmanagementsystem.Adapters;

import android.content.Context;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;

import com.capstone.mdfeventmanagementsystem.Student.Event;
import com.capstone.mdfeventmanagementsystem.R;

import java.util.ArrayList;
import java.util.List;

public class    EventAdapter extends RecyclerView.Adapter<EventAdapter.EventViewHolder> {

    private List<Event> eventList;
    private final Context context;
    private final OnEventClickListener eventClickListener;

    // Interface for event click listener
    public interface OnEventClickListener {
        void onEventClick(Event event);
    }

    // ✅ Fixed constructor parameter order
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
        holder.eventFor.setText(event.getEventFor());
        holder.dateCreated.setText("Posted: " + event.getDateCreated());
        holder.startDate.setText("Start: " + event.getStartDate());
        holder.endDate.setText("End: " + event.getEndDate());
        holder.startTime.setText(event.getStartTime());
        holder.endTime.setText(event.getEndTime());
        holder.dayOfMonth.setText(event.getDayOfMonth());
        holder.month.setText(event.getMonthShort());

        // Handle click
        holder.itemView.setOnClickListener(v -> {
            Log.d("EventAdapter", "Clicked on event: " + event.getEventName() + " (UID: " + event.getEventUID() + ")");
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
        if (newEventList == null || newEventList.isEmpty()) {
            Log.w("TestApp", "updateEventList: Received empty list.");
            return;
        }

        Log.d("TestApp", "updateEventList: Updating adapter with " + newEventList.size() + " events.");

        this.eventList = new ArrayList<>(newEventList); // Avoid clearing then adding
        notifyDataSetChanged(); // Ensure RecyclerView refresh
    }



    public static class EventViewHolder extends RecyclerView.ViewHolder {
        TextView eventName, eventFor, dateCreated, startDate, endDate, startTime, endTime;
        TextView dayOfMonth, month;

        public EventViewHolder(@NonNull View itemView) {
            super(itemView);
            eventName = itemView.findViewById(R.id.eventName);
            eventFor = itemView.findViewById(R.id.eventFor);
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
