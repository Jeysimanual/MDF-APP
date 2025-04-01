package com.capstone.mdfeventmanagementsystem;

import android.content.Intent;
import android.os.Bundle;
import android.view.View;
import android.widget.ImageView;
import android.widget.TextView;
import android.util.Log;

import androidx.appcompat.app.AppCompatActivity;

import com.bumptech.glide.Glide;

import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.Calendar;
import java.util.Locale;

public class StudentTicketsInside extends AppCompatActivity {

    private TextView eventName, startDate, endDate, startTime, endTime, graceTime, eventSpan, venue, eventDescription, dayText;
    private ImageView qrCodeImage;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_student_tickets_inside);

        // Initialize UI elements
        eventName = findViewById(R.id.eventName);
        startDate = findViewById(R.id.startDate);
        endDate = findViewById(R.id.endDate);
        graceTime = findViewById(R.id.graceTime);
        eventSpan = findViewById(R.id.eventSpan);
        venue = findViewById(R.id.venue);
        eventDescription = findViewById(R.id.eventDescription);
        qrCodeImage = findViewById(R.id.qrCodeImage);
        dayText = findViewById(R.id.day);
        TextView timeText = findViewById(R.id.time); // This will display the combined start and end time

        // Get data from intent
        Intent intent = getIntent();
        String eventNameText = intent.getStringExtra("eventName");
        String startDateText = intent.getStringExtra("startDate");
        String endDateText = intent.getStringExtra("endDate");
        String startTimeText = intent.getStringExtra("startTime");
        String endTimeText = intent.getStringExtra("endTime");
        String graceTimeText = intent.getStringExtra("graceTime");
        String eventSpanText = intent.getStringExtra("eventSpan");
        String venueText = intent.getStringExtra("venue");
        String eventDescriptionText = intent.getStringExtra("eventDescription");
        String qrCodeUrl = intent.getStringExtra("qrCodeUrl");

        // Set data to UI
        eventName.setText(eventNameText);
        graceTime.setText(graceTimeText);
        eventSpan.setText(eventSpanText);
        venue.setText(venueText);
        eventDescription.setText(eventDescriptionText);

        // Log eventSpan value to check if it's received correctly
        Log.d("Event Span", "Event Span: " + eventSpanText);

        // Check eventSpan to decide whether to show only the start date or both start and end date
        if ("single-day".equals(eventSpanText)) {
            // If it's a single-day event, only show startDate
            Log.d("Event Span", "Single day event");
            startDate.setText(startDateText);
            endDate.setVisibility(View.GONE); // Hide endDate
        } else if ("multi-day".equals(eventSpanText)) {
            // If it's a multi-day event, show both startDate and endDate
            Log.d("Event Span", "Multi-day event");
            startDate.setText(startDateText);
            if (endDateText != null && !endDateText.isEmpty()) {
                endDate.setText(endDateText);
                endDate.setVisibility(View.VISIBLE); // Ensure endDate is visible
            } else {
                Log.d("Event Span", "End date is empty or null");
                endDate.setVisibility(View.GONE); // Hide endDate if it's not provided
            }
        }

        // Combine startTime and endTime for the "time" TextView in the format "1:00pm - 2:00pm"
        if (startTimeText != null && endTimeText != null) {
            String combinedTime = formatTime(startTimeText) + " - " + formatTime(endTimeText);
            timeText.setText(combinedTime);
        }

        // Get day from startDate and set it
        if (startDateText != null && !startDateText.isEmpty()) {
            dayText.setText(getDayOfWeek(startDateText));
        }

        // Load QR Code using Glide
        if (qrCodeUrl != null && !qrCodeUrl.isEmpty()) {
            Glide.with(this).load(qrCodeUrl).into(qrCodeImage);
        } else {
            qrCodeImage.setImageResource(R.drawable.placeholder_qr);
        }
    }

    // Method to format time to "1:00pm" or "1:00am"
    private String formatTime(String timeStr) {
        try {
            SimpleDateFormat inputFormat = new SimpleDateFormat("HH:mm", Locale.getDefault()); // 24-hour format
            SimpleDateFormat outputFormat = new SimpleDateFormat("h:mm a", Locale.getDefault()); // 12-hour format
            return outputFormat.format(inputFormat.parse(timeStr));
        } catch (ParseException e) {
            e.printStackTrace();
            return timeStr; // Return the original time string if formatting fails
        }
    }

    // Method to get day of the week from a given date
    private String getDayOfWeek(String dateStr) {
        SimpleDateFormat sdf = new SimpleDateFormat("yyyy-MM-dd", Locale.getDefault()); // Adjust date format if needed
        try {
            Calendar calendar = Calendar.getInstance();
            calendar.setTime(sdf.parse(dateStr));

            // Format to short day name (e.g., Sat)
            return new SimpleDateFormat("E", Locale.getDefault()).format(calendar.getTime());
        } catch (ParseException e) {
            e.printStackTrace();
            return ""; // Return empty if parsing fails
        }
    }
}
