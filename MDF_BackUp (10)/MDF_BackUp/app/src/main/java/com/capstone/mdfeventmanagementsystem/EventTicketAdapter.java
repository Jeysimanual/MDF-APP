package com.capstone.mdfeventmanagementsystem;

import android.content.Context;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageView;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;

import com.bumptech.glide.Glide;
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
            qrCodeImage = itemView.findViewById(R.id.qrCodeImage); // Ensure this matches your XML ID
        }
    }
}
