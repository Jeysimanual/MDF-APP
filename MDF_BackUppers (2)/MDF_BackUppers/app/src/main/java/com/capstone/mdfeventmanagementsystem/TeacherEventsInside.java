package com.capstone.mdfeventmanagementsystem;

import android.content.Intent;
import android.graphics.Color;
import android.os.Bundle;
import android.util.Log;
import android.widget.Button;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.ScrollView;
import android.widget.TextView;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.appcompat.app.AlertDialog;
import androidx.appcompat.app.AppCompatActivity;

import com.bumptech.glide.Glide;
import com.google.firebase.database.DataSnapshot;
import com.google.firebase.database.DatabaseError;
import com.google.firebase.database.DatabaseReference;
import com.google.firebase.database.FirebaseDatabase;
import com.google.firebase.database.ValueEventListener;

import java.util.ArrayList;
import java.util.List;

public class TeacherEventsInside extends AppCompatActivity {

    private TextView eventName, eventDescription, startDate, endDate, startTime, endTime, venue, eventSpan, ticketType, graceTime, eventType, eventFor;
    private ImageView eventImage;
    private String eventUID; // Store event UID
    private Button showCoordinatorsBtn;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_teacher_events_inside);


        showCoordinatorsBtn = findViewById(R.id.showCoordinatorsBtn);
        // Get event UID from Intent
        eventUID = getIntent().getStringExtra("eventUID");

        showCoordinatorsBtn.setOnClickListener(v -> showCoordinatorsDialog());

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

        // Check if any view is null
        if (eventName == null || eventDescription == null || startDate == null || endDate == null ||
                startTime == null || endTime == null || venue == null || eventSpan == null ||
                graceTime == null || eventType == null || eventFor == null || eventImage == null) {
            Log.e("UI_ERROR", "One or more views are null. Check IDs in XML.");
            Toast.makeText(this, "Error loading UI components!", Toast.LENGTH_LONG).show();
            finish();
            return;
        }

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

    private void showCoordinatorsDialog() {
        if (eventUID == null || eventUID.isEmpty()) {
            Toast.makeText(this, "Event ID is missing!", Toast.LENGTH_SHORT).show();
            return;
        }

        Log.d("CoordinatorDebug", "Fetching coordinators for event ID: " + eventUID);

        DatabaseReference eventRef = FirebaseDatabase.getInstance().getReference("events")
                .child(eventUID).child("eventCoordinators");

        eventRef.orderByValue().equalTo(true).addListenerForSingleValueEvent(new ValueEventListener() {
            @Override
            public void onDataChange(@NonNull DataSnapshot dataSnapshot) {
                List<String> coordinatorEmails = new ArrayList<>();

                for (DataSnapshot snapshot : dataSnapshot.getChildren()) {
                    String email = snapshot.getKey();
                    if (email != null) {
                        coordinatorEmails.add(email.replace(",", "."));  // Replace stored commas with dots
                    }
                }

                if (coordinatorEmails.isEmpty()) {
                    Toast.makeText(TeacherEventsInside.this, "No coordinators found.", Toast.LENGTH_SHORT).show();
                } else {
                    showCoordinatorEmailDialog(coordinatorEmails);
                }
            }

            @Override
            public void onCancelled(@NonNull DatabaseError databaseError) {
                Log.e("FirebaseError", "Error fetching coordinators: " + databaseError.getMessage());
                Toast.makeText(TeacherEventsInside.this, "Error loading coordinator list", Toast.LENGTH_SHORT).show();
            }
        });
    }

    private void showCoordinatorEmailDialog(List<String> coordinatorEmails) {
        AlertDialog.Builder builder = new AlertDialog.Builder(this);
        builder.setTitle("Event Coordinators");

        ScrollView scrollView = new ScrollView(this);
        LinearLayout layout = new LinearLayout(this);
        layout.setOrientation(LinearLayout.VERTICAL);

        for (String email : coordinatorEmails) {
            TextView emailTextView = new TextView(this);
            emailTextView.setText(email);
            emailTextView.setTextSize(16);
            emailTextView.setTextColor(Color.BLACK);
            emailTextView.setPadding(20, 10, 10, 10);

            layout.addView(emailTextView);
        }

        scrollView.addView(layout);
        builder.setView(scrollView);
        builder.setPositiveButton("Close", (dialog, which) -> dialog.dismiss());
        builder.create().show();
    }
}

