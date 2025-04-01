package com.capstone.mdfeventmanagementsystem;

import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageView;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;

import java.util.List;

public class EmailAdapter extends RecyclerView.Adapter<EmailAdapter.EmailViewHolder> {

    private List<String> emails;
    private OnEmailDeleteListener deleteListener;

    // Constructor
    public EmailAdapter(List<String> emails, OnEmailDeleteListener deleteListener) {
        this.emails = emails;
        this.deleteListener = deleteListener;
    }

    @NonNull
    @Override
    public EmailViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        View view = LayoutInflater.from(parent.getContext()).inflate(R.layout.item_email, parent, false);
        return new EmailViewHolder(view);
    }

    @Override
    public void onBindViewHolder(@NonNull EmailViewHolder holder, int position) {
        String email = emails.get(position);
        holder.emailTextView.setText(email);

        holder.deleteButton.setOnClickListener(v -> deleteListener.onEmailDelete(email));
    }

    @Override
    public int getItemCount() {
        return emails.size();
    }

    static class EmailViewHolder extends RecyclerView.ViewHolder {
        TextView emailTextView;
        ImageView deleteButton;

        public EmailViewHolder(@NonNull View itemView) {
            super(itemView);
            emailTextView = itemView.findViewById(R.id.emailTextView);
            deleteButton = itemView.findViewById(R.id.deleteButton);
        }
    }

    // Interface for email deletion
    public interface OnEmailDeleteListener {
        void onEmailDelete(String email);
    }
}