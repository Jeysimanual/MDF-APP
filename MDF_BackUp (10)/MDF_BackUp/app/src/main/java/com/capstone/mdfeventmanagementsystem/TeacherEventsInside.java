package com.capstone.mdfeventmanagementsystem;

import android.content.Intent;
import android.os.Bundle;
import android.util.Log;
import android.widget.ImageView;
import android.widget.TextView;
import android.widget.Toast;

import androidx.appcompat.app.AppCompatActivity;

import com.bumptech.glide.Glide;

public class TeacherEventsInside extends AppCompatActivity {

    private TextView eventName, eventDescription, startDate, endDate, startTime, endTime, venue, eventSpan, ticketType, graceTime, eventType, eventFor;
    private ImageView eventImage;
    private String eventUID; // Store event UID

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_teacher_events_inside);

        // Initialize UI elements
        eventName = findViewById(R.id.eventName);
        eventDescription = findViewById(R.id.eventDescription);
        startDate = findViewById(R.id.startDate);
        endDate = findViewById(R.id.endDate);
        startTime = findViewById(R.id.startTime);
        endTime = findViewById(R.id.endTime);
        venue = findViewById(R.id.venue);
        eventSpan = findViewById(R.id.eventSpan);
        graceTime = findViewById(R.id.graceTime);
        eventType = findViewById(R.id.eventType);
        eventFor = findViewById(R.id.eventFor);
        eventImage = findViewById(R.id.eventPhotoUrl);

        // Get event details from intent
        Intent intent = getIntent();
        if (intent != null) {
            eventUID = intent.getStringExtra("eventUID"); // Store event UID
            String name = intent.getStringExtra("eventName");
            String description = intent.getStringExtra("eventDescription");
            String start = intent.getStringExtra("startDate");
            String end = intent.getStringExtra("endDate");
            String startT = intent.getStringExtra("startTime");
            String endT = intent.getStringExtra("endTime");
            String eventVenue = intent.getStringExtra("venue");
            String span = intent.getStringExtra("eventSpan");
            String grace = intent.getStringExtra("graceTime");
            String eType = intent.getStringExtra("eventType"); // Fetch event type
            String eFor = intent.getStringExtra("eventFor"); // Fetch event for
            String eventPhotoUrl = intent.getStringExtra("eventPhotoUrl");

            // Log fetched data with "dataTest" tag
            Log.d("dataTest", "Event UID: " + (eventUID != null ? eventUID : "NULL"));
            Log.d("dataTest", "Event Name: " + (name != null ? name : "NULL"));
            Log.d("dataTest", "Event Description: " + (description != null ? description : "NULL"));
            Log.d("dataTest", "Start Date: " + (start != null ? start : "NULL"));
            Log.d("dataTest", "End Date: " + (end != null ? end : "NULL"));
            Log.d("dataTest", "Start Time: " + (startT != null ? startT : "NULL"));
            Log.d("dataTest", "End Time: " + (endT != null ? endT : "NULL"));
            Log.d("dataTest", "Venue: " + (eventVenue != null ? eventVenue : "NULL"));
            Log.d("dataTest", "Event Span: " + (span != null ? span : "NULL"));
            Log.d("dataTest", "Grace Time: " + (grace != null ? grace : "NULL"));
            Log.d("dataTest", "Event Type: " + (eType != null ? eType : "NULL"));
            Log.d("dataTest", "Event For: " + (eFor != null ? eFor : "NULL"));
            Log.d("dataTest", "Event Photo URL: " + (eventPhotoUrl != null ? eventPhotoUrl : "NULL"));

            // Set data to UI with null checks
            eventName.setText(name != null ? name : "N/A");
            eventDescription.setText(description != null ? description : "N/A");
            startDate.setText(start != null ? start : "N/A");
            endDate.setText(end != null ? end : "N/A");
            startTime.setText(startT != null ? startT : "N/A");
            endTime.setText(endT != null ? endT : "N/A");
            venue.setText(eventVenue != null ? eventVenue : "N/A");
            eventSpan.setText(span != null ? span : "N/A");
            graceTime.setText(grace != null ? grace : "N/A");
            eventType.setText(eType != null ? eType : "N/A");
            eventFor.setText(eFor != null ? eFor : "N/A");

            // Load event image using Glide
            if (eventPhotoUrl != null && !eventPhotoUrl.isEmpty()) {
                Glide.with(this)
                        .load(eventPhotoUrl)
                        .placeholder(R.drawable.placeholder_image)
                        .error(R.drawable.error_image)
                        .into(eventImage);
            } else {
                eventImage.setImageResource(R.drawable.placeholder_image);
            }
        } else {
            Toast.makeText(this, "Error loading event details!", Toast.LENGTH_SHORT).show();
            Log.e("dataTest", "No event data found in intent.");
            finish();
        }
    }
}
