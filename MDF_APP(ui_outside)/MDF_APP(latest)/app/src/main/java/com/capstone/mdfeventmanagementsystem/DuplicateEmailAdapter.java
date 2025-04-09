package com.capstone.mdfeventmanagementsystem;

import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;

import java.util.List;

public class DuplicateEmailAdapter extends RecyclerView.Adapter<DuplicateEmailAdapter.ViewHolder> {

    private List<String> duplicateEmails;

    public DuplicateEmailAdapter(List<String> duplicateEmails) {
        this.duplicateEmails = duplicateEmails;
    }

    @NonNull
    @Override
    public ViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        View view = LayoutInflater.from(parent.getContext()).inflate(android.R.layout.simple_list_item_1, parent, false);
        return new ViewHolder(view);
    }

    @Override
    public void onBindViewHolder(@NonNull ViewHolder holder, int position) {
        String email = duplicateEmails.get(position);
        holder.emailTextView.setText(email);
    }

    @Override
    public int getItemCount() {
        return duplicateEmails.size();
    }

    public static class ViewHolder extends RecyclerView.ViewHolder {
        TextView emailTextView;

        public ViewHolder(View itemView) {
            super(itemView);
            emailTextView = itemView.findViewById(android.R.id.text1);
        }
    }
}
