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

import java.util.List;

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
        holder.startDate.setText(ticket.getStartDate());
        holder.startTime.setText(ticket.getStartTime());
        holder.venue.setText(ticket.getVenue());
        holder.ticketID.setText("Ticket ID: " + ticket.getTicketID());

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

            // Use "ticketId" instead of "ticketID" to match StudentTicketsInside variable name
            intent.putExtra("ticketId", ticket.getTicketID());

            context.startActivity(intent);
        });
    }

    @Override
    public int getItemCount() {
        return ticketList.size();
    }

    public static class ViewHolder extends RecyclerView.ViewHolder {
        TextView eventName, eventType, startDate, startTime, venue, ticketID;
        ImageView qrCodeImage;

        public ViewHolder(@NonNull View itemView) {
            super(itemView);
            eventName = itemView.findViewById(R.id.eventName);
            eventType = itemView.findViewById(R.id.eventType);
            startDate = itemView.findViewById(R.id.startDate);
            startTime = itemView.findViewById(R.id.startTime);
            venue = itemView.findViewById(R.id.venue);
            ticketID = itemView.findViewById(R.id.ticketID);
            qrCodeImage = itemView.findViewById(R.id.qrCodeImage);
        }
    }
}