package com.capstone.mdfeventmanagementsystem.Adapters;

import android.content.Context;
import android.graphics.Color;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.core.content.ContextCompat;
import androidx.recyclerview.widget.RecyclerView;

import com.capstone.mdfeventmanagementsystem.R;
import com.capstone.mdfeventmanagementsystem.Models.Participant;

import java.util.ArrayList;
import java.util.List;

public class ParticipantsAdapter extends RecyclerView.Adapter<ParticipantsAdapter.ParticipantViewHolder> {

    private static final String TAG = "ParticipantsAdapter";
    private List<Participant> participantList;
    private List<Participant> filteredList;
    private Context context;

    public ParticipantsAdapter(Context context) {
        this.context = context;
        this.participantList = new ArrayList<>();
        this.filteredList = new ArrayList<>();
    }

    public void setParticipants(List<Participant> participants) {
        this.participantList = participants;
        this.filteredList = new ArrayList<>(participants);
        notifyDataSetChanged();
        Log.d(TAG, "Set " + participants.size() + " participants in adapter");
    }

    public void filter(String query) {
        filteredList.clear();
        if (query.isEmpty()) {
            filteredList.addAll(participantList);
        } else {
            String lowerCaseQuery = query.toLowerCase();
            for (Participant participant : participantList) {
                if ((participant.getName() != null && participant.getName().toLowerCase().contains(lowerCaseQuery)) ||
                        (participant.getSection() != null && participant.getSection().toLowerCase().contains(lowerCaseQuery))) {
                    filteredList.add(participant);
                }
            }
        }
        Log.d(TAG, "Filter applied, found " + filteredList.size() + " participants matching '" + query + "'");
        notifyDataSetChanged();
    }

    @NonNull
    @Override
    public ParticipantViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        View view = LayoutInflater.from(parent.getContext()).inflate(R.layout.item_participant, parent, false);
        return new ParticipantViewHolder(view);
    }

    @Override
    public void onBindViewHolder(@NonNull ParticipantViewHolder holder, int position) {
        Participant participant = filteredList.get(position);

        // Use safe string handling to avoid null pointer exceptions
        holder.nameTextView.setText(participant.getName() != null ? participant.getName() : "");
        holder.sectionTextView.setText(participant.getSection() != null ? participant.getSection() : "");
        holder.timeInTextView.setText(participant.getTimeIn() != null ? participant.getTimeIn() : "");
        holder.timeOutTextView.setText(participant.getTimeOut() != null ? participant.getTimeOut() : "");
        holder.statusTextView.setText(participant.getStatus() != null ? participant.getStatus() : "");

        // Set status background color
        if (participant.getStatus() != null && "Present".equalsIgnoreCase(participant.getStatus())) {
            holder.statusTextView.setBackgroundResource(R.drawable.status_present_background);
        } else if (participant.getStatus() != null && "Absent".equalsIgnoreCase(participant.getStatus())) {
            holder.statusTextView.setBackgroundResource(R.drawable.status_absent_background);
        } else {
            holder.statusTextView.setBackgroundColor(Color.GRAY);
        }
    }

    @Override
    public int getItemCount() {
        return filteredList.size();
    }

    public List<Participant> getFilteredList() {
        return filteredList;
    }

    static class ParticipantViewHolder extends RecyclerView.ViewHolder {
        TextView nameTextView;
        TextView sectionTextView;
        TextView timeInTextView;
        TextView timeOutTextView;
        TextView statusTextView;

        public ParticipantViewHolder(@NonNull View itemView) {
            super(itemView);
            nameTextView = itemView.findViewById(R.id.tv_name);
            sectionTextView = itemView.findViewById(R.id.tv_section);
            timeInTextView = itemView.findViewById(R.id.tv_time_in);
            timeOutTextView = itemView.findViewById(R.id.tv_time_out);
            statusTextView = itemView.findViewById(R.id.tv_status);
        }
    }
}