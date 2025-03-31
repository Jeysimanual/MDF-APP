package com.capstone.mdfeventmanagementsystem;

import android.content.Intent;
import android.os.Bundle;
import android.widget.ImageView;
import android.widget.TextView;

import androidx.appcompat.app.AppCompatActivity;

import com.bumptech.glide.Glide;

public class StudentTicketsInside extends AppCompatActivity {

    private TextView eventName, eventType, startDate, startTime, venue;
    private ImageView qrCodeImage;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_student_tickets_inside);

        // Initialize UI elements
        eventName = findViewById(R.id.eventName);
        eventType = findViewById(R.id.eventType);
        startDate = findViewById(R.id.startDate);
        startTime = findViewById(R.id.startTime);
        venue = findViewById(R.id.venue);
        qrCodeImage = findViewById(R.id.qrCodeImage);

        // Get data from intent
        Intent intent = getIntent();
        String eventNameText = intent.getStringExtra("eventName");
        String eventTypeText = intent.getStringExtra("eventType");
        String startDateText = intent.getStringExtra("startDate");
        String startTimeText = intent.getStringExtra("startTime");
        String venueText = intent.getStringExtra("venue");
        String qrCodeUrl = intent.getStringExtra("qrCodeUrl");

        // Set data to UI
        eventName.setText(eventNameText);
        eventType.setText(eventTypeText);
        startDate.setText(startDateText);
        startTime.setText(startTimeText);
        venue.setText(venueText);

        // Load QR Code using Glide
        if (qrCodeUrl != null && !qrCodeUrl.isEmpty()) {
            Glide.with(this).load(qrCodeUrl).into(qrCodeImage);
        } else {
            qrCodeImage.setImageResource(R.drawable.placeholder_qr);
        }
    }
}
