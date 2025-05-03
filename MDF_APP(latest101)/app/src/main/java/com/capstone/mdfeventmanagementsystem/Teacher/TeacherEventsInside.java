package com.capstone.mdfeventmanagementsystem.Teacher;

import android.content.Intent;
import android.graphics.Color;
import android.os.Bundle;
import android.util.Log;
import android.view.Gravity;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Button;
import android.widget.EditText;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.ScrollView;
import android.widget.TextView;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.appcompat.app.AlertDialog;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;
import androidx.viewpager2.widget.ViewPager2;

import com.bumptech.glide.Glide;
import com.capstone.mdfeventmanagementsystem.Adapters.DuplicateEmailAdapter;
import com.capstone.mdfeventmanagementsystem.Adapters.EmailAdapter;
import com.capstone.mdfeventmanagementsystem.Adapters.EventTabsPagerAdapter;
import com.capstone.mdfeventmanagementsystem.R;
import com.capstone.mdfeventmanagementsystem.Utilities.BaseActivity;
import com.google.firebase.database.DataSnapshot;
import com.google.firebase.database.DatabaseError;
import com.google.firebase.database.DatabaseReference;
import com.google.firebase.database.FirebaseDatabase;
import com.google.firebase.database.ValueEventListener;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.concurrent.atomic.AtomicInteger;

public class TeacherEventsInside extends BaseActivity {

    private TextView eventName, eventDescription, startDate, endDate, startTime, endTime, venue, eventSpan, totalCoordinatorTextView, graceTime, eventType, eventFor;
    private TextView ticketGeneratedTextView; // Added for ticket counting
    private ImageView eventImage;
    private String eventUID; // Store event UID
    private Button showCoordinatorsBtn, addCoordinatorBtn;
    private EmailAdapter emailAdapter;

    // Tab UI elements
    private ViewPager2 viewPager;
    private TextView tabEventDetails, tabParticipants;
    private EventTabsPagerAdapter tabAdapter;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_teacher_events_inside); // Updated layout file name

        // Initialize tab-related UI elements
        viewPager = findViewById(R.id.viewPager);
        tabEventDetails = findViewById(R.id.tabEventDetails);
        tabParticipants = findViewById(R.id.tabParticipants);

        // Setup back button
        ImageView btnBack = findViewById(R.id.btnBack);
        btnBack.setOnClickListener(v -> finish());

        // Initialize regular UI elements
        eventName = findViewById(R.id.eventName);

        // Get event UID from Intent
        eventUID = getIntent().getStringExtra("eventUID");

        // Set button click listeners
        if (showCoordinatorsBtn != null) {
            showCoordinatorsBtn.setOnClickListener(v -> showCoordinatorsDialog());
        }

        if (addCoordinatorBtn != null) {
            addCoordinatorBtn.setOnClickListener(v -> showAddCoordinatorDialog());
        }

        // Get event details from intent
        Intent intent = getIntent();
        if (intent != null) {
            eventUID = intent.getStringExtra("eventUID");
            String name = intent.getStringExtra("eventName");
            String description = intent.getStringExtra("eventDescription");
            String eventPhotoUrl = intent.getStringExtra("eventPhotoUrl");
            String eventFor = getIntent().getStringExtra("eventFor");


            // Set data to UI with null checks
            if (eventName != null) {
                eventName.setText(name != null ? name : "N/A");
            }

            // Setup ViewPager2 with tabs
            setupTabs(eventUID, description, eventPhotoUrl, eventFor);

            // Load data for coordinators and tickets
            if (eventUID != null) {
                getTotalCoordinators(eventUID);
                getTicketCount(eventUID);
            }
        } else {
            Toast.makeText(this, "Error loading event details!", Toast.LENGTH_SHORT).show();
            Log.e("dataTest", "No event data found in intent.");
            finish();
        }
    }

    private void setupTabs(String eventId, String description, String photoUrl, String eventFor) {
        tabAdapter = new EventTabsPagerAdapter(this, eventId, description, photoUrl, eventFor);
        viewPager.setAdapter(tabAdapter);


    // Disable swiping if you want to control tab changes only through buttons
        // viewPager.setUserInputEnabled(false);

        // Set up tab click listeners
        tabEventDetails.setOnClickListener(v -> {
            viewPager.setCurrentItem(0);
            updateTabStyles(0);
        });

        tabParticipants.setOnClickListener(v -> {
            viewPager.setCurrentItem(1);
            updateTabStyles(1);
        });

        // Listen for page changes
        viewPager.registerOnPageChangeCallback(new ViewPager2.OnPageChangeCallback() {
            @Override
            public void onPageSelected(int position) {
                super.onPageSelected(position);
                updateTabStyles(position);
            }
        });
    }

    private void updateTabStyles(int selectedTab) {
        // Update tab styles based on selection
        if (selectedTab == 0) {
            tabEventDetails.setBackground(getResources().getDrawable(R.drawable.tab_selected));
            tabEventDetails.setTextColor(Color.BLACK);
            tabParticipants.setBackground(null);
            tabParticipants.setTextColor(getResources().getColor(R.color.gray));
        } else {
            tabParticipants.setBackground(getResources().getDrawable(R.drawable.tab_selected));
            tabParticipants.setTextColor(Color.BLACK);
            tabEventDetails.setBackground(null);
            tabEventDetails.setTextColor(getResources().getColor(R.color.gray));
        }
    }

    // Keep the rest of your methods unchanged...
    // getTicketCount, showAddCoordinatorDialog, findMatchingStudentByEmail, etc.

    // Method to count and display number of tickets for this event
    private void getTicketCount(String eventId) {
        if (eventId == null || eventId.isEmpty()) {
            Log.e("TicketCount", "Cannot fetch tickets: eventId is null or empty");
            if (ticketGeneratedTextView != null) {
                ticketGeneratedTextView.setText("0");
            }
            return;
        }

        DatabaseReference studentsRef = FirebaseDatabase.getInstance().getReference("students");

        studentsRef.addListenerForSingleValueEvent(new ValueEventListener() {
            @Override
            public void onDataChange(@NonNull DataSnapshot dataSnapshot) {
                int ticketCount = 0;

                // Extract the event key from the full path
                String eventKey = eventId;
                if (eventId.contains("/")) {
                    String[] parts = eventId.split("/");
                    eventKey = parts[parts.length - 1];
                }

                Log.d("TicketCount", "Searching for tickets matching event key: " + eventKey);

                for (DataSnapshot studentSnapshot : dataSnapshot.getChildren()) {
                    if (studentSnapshot.hasChild("tickets")) {
                        DataSnapshot ticketsSnapshot = studentSnapshot.child("tickets");

                        // Check if this student has a ticket for this event
                        for (DataSnapshot ticketSnapshot : ticketsSnapshot.getChildren()) {
                            String ticketKey = ticketSnapshot.getKey();
                            Log.d("TicketCount", "Checking ticket: " + ticketKey);

                            // Method 1: Check if ticket key matches the event key
                            if (eventKey.equals(ticketKey)) {
                                ticketCount++;
                                Log.d("TicketCount", "Found matching ticket by key: " + ticketKey);
                                continue; // Found a match, move to next ticket
                            }

                            // Method 2: Look for an eventUID field in the ticket
                            if (ticketSnapshot.hasChild("eventUID")) {
                                String ticketEventUID = ticketSnapshot.child("eventUID").getValue(String.class);
                                if (eventId.equals(ticketEventUID) || eventKey.equals(ticketEventUID)) {
                                    ticketCount++;
                                    Log.d("TicketCount", "Found matching ticket by eventUID field: " + ticketEventUID);
                                }
                            }
                        }
                    }
                }

                // Update the UI with only the number
                if (ticketGeneratedTextView != null) {
                    ticketGeneratedTextView.setText(String.valueOf(ticketCount));
                }
                Log.d("TicketCount", "Final ticket count: " + ticketCount);
            }

            @Override
            public void onCancelled(@NonNull DatabaseError error) {
                Log.e("FirebaseError", "Error fetching tickets: " + error.getMessage());
                if (ticketGeneratedTextView != null) {
                    ticketGeneratedTextView.setText("0");
                }
            }
        });
    }

    private void getTotalCoordinators(String eventId) {
        // Ensure eventId is valid before making a Firebase call
        if (eventId == null || eventId.isEmpty() || totalCoordinatorTextView == null) {
            Log.e("OrganizerDetails", "Cannot fetch coordinators: eventId is null or empty");
            if (totalCoordinatorTextView != null) {
                totalCoordinatorTextView.setText("Event ID is invalid");
            }
            return;
        }

        // Reference to the coordinators node in Firebase
        DatabaseReference ref = FirebaseDatabase.getInstance().getReference("events").child(eventId).child("eventCoordinators");

        // Listen for real-time updates
        ref.orderByValue().equalTo(true).addValueEventListener(new ValueEventListener() {
            @Override
            public void onDataChange(@NonNull DataSnapshot snapshot) {
                // Check if coordinators exist
                if (snapshot.exists()) {
                    long coordinatorCount = snapshot.getChildrenCount(); // Count coordinators
                    totalCoordinatorTextView.setText(coordinatorCount + " Coordinator(s)");
                } else {
                    totalCoordinatorTextView.setText("0 Coordinator(s)");
                }
            }

            @Override
            public void onCancelled(@NonNull DatabaseError error) {
                Log.e("FirebaseError", "Error fetching coordinators: " + error.getMessage());
                totalCoordinatorTextView.setText("Error loading coordinators");
            }
        });
    }

    private void showCoordinatorsDialog() {
        // Implementation omitted for brevity - same as your original code
    }

    private void showAddCoordinatorDialog() {
        // Implementation omitted for brevity - same as your original code
    }

    // All other methods remain unchanged
}