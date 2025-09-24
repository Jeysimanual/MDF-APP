package com.capstone.mdfeventmanagementsystem.Adapters;

import android.content.Context;
import android.content.Intent;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageView;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;

import com.bumptech.glide.Glide;
import com.capstone.mdfeventmanagementsystem.Student.EventTicket;
import com.capstone.mdfeventmanagementsystem.R;
import com.capstone.mdfeventmanagementsystem.Student.StudentTicketsInside;

import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.List;
import java.util.Locale;

public class EventTicketAdapter extends RecyclerView.Adapter<EventTicketAdapter.ViewHolder> {

    private Context context;
    private List<EventTicket> ticketList;

    public EventTicketAdapter(Context context, List<EventTicket> ticketList) {
        this.context = context;
        this.ticketList = ticketList;
    }

    @NonNull
    @Override
    public ViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        View view = LayoutInflater.from(context).inflate(R.layout.item_tickets, parent, false);
        return new ViewHolder(view);
    }

    @Override
    public void onBindViewHolder(@NonNull ViewHolder holder, int position) {
        EventTicket ticket = ticketList.get(position);
        holder.eventName.setText(ticket.getEventName());
        holder.eventType.setText(ticket.getEventType());

        // Format startDate to "Mar 01, 2025"
        String startDate = ticket.getStartDate(); // e.g., "2025-03-01"
        String formattedDate = formatDate(startDate);
        holder.startDate.setText(formattedDate);

        // Format startTime and endTime into "10:00 AM to 11:00 AM"
        String startTime = ticket.getStartTime(); // e.g., "10:00" or "10:00:00"
        String endTime = ticket.getEndTime();     // e.g., "11:00" or "11:00:00"
        String timeRange = formatTimeRange(startTime, endTime);
        holder.timeRange.setText(timeRange);

        holder.venue.setText(ticket.getVenue());
        holder.ticketID.setText(ticket.getTicketID());

        // Load QR code image using Glide
        Glide.with(context).load(ticket.getQrCodeUrl()).into(holder.qrCodeImage);

        // Make each ticket item clickable & pass extra data
        holder.itemView.setOnClickListener(v -> {
            Intent intent = new Intent(context, StudentTicketsInside.class);
            intent.putExtra("eventName", ticket.getEventName());
            intent.putExtra("eventType", ticket.getEventType());
            intent.putExtra("startDate", ticket.getStartDate());
            intent.putExtra("endDate", ticket.getEndDate());
            intent.putExtra("startTime", ticket.getStartTime());
            intent.putExtra("endTime", ticket.getEndTime());
            intent.putExtra("graceTime", ticket.getGraceTime());
            intent.putExtra("eventSpan", ticket.getEventSpan());
            intent.putExtra("venue", ticket.getVenue());
            intent.putExtra("eventDescription", ticket.getEventDescription());
            intent.putExtra("qrCodeUrl", ticket.getQrCodeUrl());
            intent.putExtra("ticketId", ticket.getTicketID());
            context.startActivity(intent);
        });
    }

    private String formatTimeRange(String startTime, String endTime) {
        try {
            // Assuming startTime and endTime are in "HH:mm" or "HH:mm:ss" format (military time)
            SimpleDateFormat inputFormat = new SimpleDateFormat("HH:mm", Locale.US);
            SimpleDateFormat outputFormat = new SimpleDateFormat("h:mm a", Locale.US);

            // Parse the input times
            Date startDate = inputFormat.parse(startTime);
            Date endDate = inputFormat.parse(endTime);

            // Format to 12-hour with AM/PM
            String formattedStartTime = outputFormat.format(startDate);
            String formattedEndTime = outputFormat.format(endDate);

            // Combine into single string
            return formattedStartTime + " to " + formattedEndTime;
        } catch (ParseException e) {
            e.printStackTrace();
            // Fallback in case of parsing error
            return startTime + " to " + endTime;
        }
    }

    private String formatDate(String startDate) {
        try {
            // Assuming startDate is in "yyyy-MM-dd" format (e.g., "2025-03-01")
            SimpleDateFormat inputFormat = new SimpleDateFormat("yyyy-MM-dd", Locale.US);
            SimpleDateFormat outputFormat = new SimpleDateFormat("MMM dd, yyyy", Locale.US);

            // Parse the input date
            Date date = inputFormat.parse(startDate);

            // Format to "Mar 01, 2025"
            return outputFormat.format(date);
        } catch (ParseException e) {
            e.printStackTrace();
            // Fallback in case of parsing error
            return startDate;
        }
    }

    @Override
    public int getItemCount() {
        return ticketList.size();
    }

    public static class ViewHolder extends RecyclerView.ViewHolder {
        TextView eventName, eventType, startDate, timeRange, venue, ticketID;
        ImageView qrCodeImage;

        public ViewHolder(@NonNull View itemView) {
            super(itemView);
            eventName = itemView.findViewById(R.id.eventName);
            eventType = itemView.findViewById(R.id.eventType);
            startDate = itemView.findViewById(R.id.startDate);
            timeRange = itemView.findViewById(R.id.timeRange);
            venue = itemView.findViewById(R.id.venue);
            ticketID = itemView.findViewById(R.id.ticketID);
            qrCodeImage = itemView.findViewById(R.id.qrCodeImage);
        }
    }
}