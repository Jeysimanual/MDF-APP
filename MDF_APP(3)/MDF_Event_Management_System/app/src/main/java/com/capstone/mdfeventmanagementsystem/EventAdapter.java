package com.capstone.mdfeventmanagementsystem;

import android.content.Context;
import android.content.Intent;
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

    public EventAdapter(Context context, List<Event> eventList) {
        this.context = context;
        this.eventList = eventList;
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

        holder.itemView.setOnClickListener(v -> {
            Intent intent = new Intent(context, StudentDashboardInside.class);
            intent.putExtra("eventName", event.getEventName());
            intent.putExtra("eventDescription", event.getEventDescription());
            intent.putExtra("startDate", event.getStartDate());
            intent.putExtra("endDate", event.getEndDate());
            intent.putExtra("startTime", event.getStartTime());
            intent.putExtra("endTime", event.getEndTime());
            intent.putExtra("venue", event.getVenue());
            intent.putExtra("eventSpan", event.getEventSpan());
            intent.putExtra("ticketType", event.getTicketType());
            intent.putExtra("ticketActivationTime", event.getTicketActivationTime());
            intent.putExtra("eventPhotoUrl", event.getEventPhotoUrl());

            context.startActivity(intent);
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
