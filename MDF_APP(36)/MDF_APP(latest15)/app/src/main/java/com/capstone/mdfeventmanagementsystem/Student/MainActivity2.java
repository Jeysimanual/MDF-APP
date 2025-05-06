package com.capstone.mdfeventmanagementsystem.Student;

import android.content.Intent;
import android.content.res.ColorStateList;
import android.graphics.Color;
import android.os.Bundle;
import android.util.Log;
import android.view.View;
import android.widget.TextView;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.capstone.mdfeventmanagementsystem.Adapters.EventAdapter;
import com.capstone.mdfeventmanagementsystem.R;
import com.capstone.mdfeventmanagementsystem.Utilities.BaseActivity;
import com.google.android.material.bottomappbar.BottomAppBar;
import com.google.android.material.bottomnavigation.BottomNavigationView;
import com.google.android.material.button.MaterialButton;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseUser;
import com.google.firebase.database.DataSnapshot;
import com.google.firebase.database.DatabaseError;
import com.google.firebase.database.DatabaseReference;
import com.google.firebase.database.FirebaseDatabase;
import com.google.firebase.database.ValueEventListener;

import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

public class MainActivity2 extends BaseActivity {

    private RecyclerView recyclerView;
    private RecyclerView recyclerViewAssignedEvents;
    private EventAdapter eventAdapter;
    private EventAdapter assignedEventAdapter;
    private List<Event> eventList;
    private List<Event> assignedEventList;
    private DatabaseReference databaseReference;
    private DatabaseReference userDatabaseReference;
    private static final DateTimeFormatter DATE_FORMATTER = DateTimeFormatter.ofPattern("yyyy-MM-dd");
    private String currentUserYearLevel;
    private static final String TAG = "MainActivity2"; // For consistent logging

    private TextView firstNameTextView; // Declare TextView for firstName

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main2);

        // Initialize the TextView for firstName
        firstNameTextView = findViewById(R.id.firstName);

        findViewById(R.id.fab_scan).setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                startActivity(new Intent(getApplicationContext(), QRCheckInActivity.class));
                overridePendingTransition(0, 0);
            }
        });

        BottomAppBar bottomAppBar = findViewById(R.id.bottomAppBar);
        bottomAppBar.setBackgroundTint(ColorStateList.valueOf(Color.WHITE));

        findViewById(R.id.profile_image).setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                Intent intent = new Intent(MainActivity2.this, ProfileActivity.class);
                startActivity(intent);
            }
        });

        // Set up "View All" button click listener
        MaterialButton btnViewAllUpcoming = findViewById(R.id.btnViewAllUpcoming);
        btnViewAllUpcoming.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                // Navigate to StudentDashboard when View All is clicked
                Intent intent = new Intent(MainActivity2.this, StudentDashboard.class);
                startActivity(intent);
                overridePendingTransition(0, 0);
            }
        });

        // Setup RecyclerView for upcoming events
        recyclerView = findViewById(R.id.recyclerViewEvents);
        recyclerView.setLayoutManager(new LinearLayoutManager(this));
        eventList = new ArrayList<>();

        // Setup RecyclerView for assigned events
        recyclerViewAssignedEvents = findViewById(R.id.recyclerViewEvents);
        recyclerViewAssignedEvents.setLayoutManager(new LinearLayoutManager(this));
        assignedEventList = new ArrayList<>();

        // Initialize adapters with click listeners
        eventAdapter = new EventAdapter(this, eventList, new EventAdapter.OnEventClickListener() {
            @Override
            public void onEventClick(Event event) {
                navigateToEventDetails(event);
            }
        });
        recyclerView.setAdapter(eventAdapter);

        assignedEventAdapter = new EventAdapter(this, assignedEventList, new EventAdapter.OnEventClickListener() {
            @Override
            public void onEventClick(Event event) {
                navigateToEventDetails(event);
            }
        });
        recyclerViewAssignedEvents.setAdapter(assignedEventAdapter);

        // Firebase reference to "events"
        databaseReference = FirebaseDatabase.getInstance().getReference("events");

        // Get current user's data
        getCurrentUserData();

        // Setup bottom navigation
        BottomNavigationView bottomNavigationView = findViewById(R.id.bottom_navigation);
        bottomNavigationView.setSelectedItemId(R.id.nav_home);
        bottomNavigationView.setBackground(null);


        bottomNavigationView.setOnNavigationItemSelectedListener(item -> {
            int itemId = item.getItemId();
            if (itemId == R.id.nav_home) {
                return true;
            } else if (itemId == R.id.nav_event) {
                startActivity(new Intent(getApplicationContext(), StudentDashboard.class));
            } else if (itemId == R.id.nav_ticket) {
                startActivity(new Intent(getApplicationContext(), StudentTickets.class));
            } else if (itemId == R.id.nav_cert) {
                startActivity(new Intent(getApplicationContext(), StudentCertificate.class));
            }
            overridePendingTransition(0, 0);
            return true;
        });
    }

    // New method to handle navigation to StudentDashboardInside
    private void navigateToEventDetails(Event event) {
        Log.d(TAG, "Navigating to StudentDashboardInside for event: " + event.getEventName());
        Intent intent = new Intent(MainActivity2.this, StudentDashboardInside.class);

        // Pass event data to the destination activity
        intent.putExtra("eventUID", event.getEventUID());
        intent.putExtra("eventName", event.getEventName());
        intent.putExtra("eventDescription", event.getEventDescription());
        intent.putExtra("startDate", event.getStartDate());
        intent.putExtra("endDate", event.getEndDate());
        intent.putExtra("startTime", event.getStartTime());
        intent.putExtra("endTime", event.getEndTime());
        intent.putExtra("venue", event.getVenue());
        intent.putExtra("eventSpan", event.getEventSpan());
        intent.putExtra("graceTime", event.getGraceTime());
        intent.putExtra("eventType", event.getEventType());
        intent.putExtra("eventFor", event.getEventFor());
        intent.putExtra("eventPhotoUrl", event.getEventPhotoUrl());
        // Add any other event data you need to pass

        startActivity(intent);
        overridePendingTransition(0, 0);
    }

    private void getCurrentUserData() {
        FirebaseUser currentUser = FirebaseAuth.getInstance().getCurrentUser();
        if (currentUser != null) {
            String userEmail = currentUser.getEmail();
            userDatabaseReference = FirebaseDatabase.getInstance().getReference("students");

            userDatabaseReference.orderByChild("email").equalTo(userEmail).addListenerForSingleValueEvent(new ValueEventListener() {
                @Override
                public void onDataChange(@NonNull DataSnapshot snapshot) {
                    if (snapshot.exists()) {
                        for (DataSnapshot userSnapshot : snapshot.getChildren()) {
                            Student student = userSnapshot.getValue(Student.class);
                            if (student != null) {
                                currentUserYearLevel = student.getYearLevel();

                                // Log the data for debugging purposes
                                Log.d(TAG, "Retrieved student data - ID: " + student.getIdNumber() +
                                        ", Name: " + student.getFirstName() + " " + student.getLastName() +
                                        ", Year Level: " + currentUserYearLevel);

                                // Update the TextView with the firstName
                                String firstName = student.getFirstName();
                                firstNameTextView.setText("Hello, " + firstName + "!");

                                // Now that we have the year level, fetch events
                                fetchUpcomingEvents();
                                fetchAssignedEvents();
                            }
                        }
                    } else {
                        Log.e(TAG, "No student record found for email: " + userEmail);
                        Toast.makeText(MainActivity2.this, "No student record found!", Toast.LENGTH_SHORT).show();

                        // Fetch events anyway (showing all upcoming events)
                        fetchUpcomingEvents();
                    }
                }

                @Override
                public void onCancelled(@NonNull DatabaseError error) {
                    Log.e(TAG, "Error fetching student data: " + error.getMessage());
                    Toast.makeText(MainActivity2.this, "Error fetching student data", Toast.LENGTH_SHORT).show();

                    // Fetch events anyway (showing all upcoming events)
                    fetchUpcomingEvents();
                }
            });
        } else {
            Log.e(TAG, "No user is currently logged in");
            Toast.makeText(this, "No user logged in", Toast.LENGTH_SHORT).show();

            // Fetch events anyway (showing all upcoming events)
            fetchUpcomingEvents();
        }
    }

    private void fetchUpcomingEvents() {
        databaseReference.addValueEventListener(new ValueEventListener() {
            @Override
            public void onDataChange(@NonNull DataSnapshot snapshot) {
                eventList.clear();
                LocalDate today = LocalDate.now();
                LocalDate maxDate = today.plusDays(7); // Set max range to 7 days from today

                for (DataSnapshot dataSnapshot : snapshot.getChildren()) {
                    Event event = dataSnapshot.getValue(Event.class);
                    if (event != null) {
                        event.setEventUID(dataSnapshot.getKey());

                        // Filter events within the next 7 days
                        try {
                            LocalDate eventDate = LocalDate.parse(event.getStartDate(), DATE_FORMATTER);
                            if (!eventDate.isBefore(today) && !eventDate.isAfter(maxDate)) {
                                eventList.add(event); // Add only if within the date range
                            }
                        } catch (Exception e) {
                            Log.e(TAG, "Error parsing event date", e);
                        }
                    }
                }

                // Sort by start date (nearest date first)
                Collections.sort(eventList, (e1, e2) -> {
                    try {
                        LocalDate date1 = LocalDate.parse(e1.getStartDate(), DATE_FORMATTER);
                        LocalDate date2 = LocalDate.parse(e2.getStartDate(), DATE_FORMATTER);
                        return date1.compareTo(date2);
                    } catch (Exception e) {
                        e.printStackTrace();
                        return 0;
                    }
                });

                eventAdapter.updateEventList(eventList);
                Log.d(TAG, "Fetched " + eventList.size() + " upcoming events");
            }

            @Override
            public void onCancelled(@NonNull DatabaseError error) {
                Log.e(TAG, "Firebase data fetch failed: " + error.getMessage(), error.toException());
            }
        });
    }

    // New method to fetch assigned events based on student's year level
    private void fetchAssignedEvents() {
        if (currentUserYearLevel == null || currentUserYearLevel.isEmpty()) {
            Log.e(TAG, "Year level not available");
            return;
        }

        // Log all events to see what's available
        databaseReference.addListenerForSingleValueEvent(new ValueEventListener() {
            @Override
            public void onDataChange(@NonNull DataSnapshot snapshot) {
                Log.d(TAG, "======= DEBUGGING ALL EVENTS =======");
                for (DataSnapshot dataSnapshot : snapshot.getChildren()) {
                    Event event = dataSnapshot.getValue(Event.class);
                    if (event != null) {
                        Log.d(TAG, "Event: " + event.getEventName() +
                                ", EventFor: '" + event.getEventFor() + "'");
                    }
                }
                Log.d(TAG, "===================================");
            }

            @Override
            public void onCancelled(@NonNull DatabaseError error) {
                Log.e(TAG, "Debug events loading failed", error.toException());
            }
        });

        databaseReference.addValueEventListener(new ValueEventListener() {
            @Override
            public void onDataChange(@NonNull DataSnapshot snapshot) {
                assignedEventList.clear();

                // Different possible formats for Grade 9
                String yearLevel = currentUserYearLevel.toLowerCase().trim();
                String grade = "";
                String gradeNum = "";

                // Extract the grade number if it contains "grade" or "g"
                if (yearLevel.contains("grade")) {
                    gradeNum = yearLevel.replace("grade", "").trim();
                } else if (yearLevel.startsWith("g")) {
                    gradeNum = yearLevel.substring(1).trim();
                } else {
                    // Just use what we have
                    gradeNum = yearLevel;
                }

                // Create different possible formats to match against
                List<String> possibleFormats = new ArrayList<>();
                possibleFormats.add(yearLevel);                // original format
                possibleFormats.add("grade" + gradeNum);       // grade9
                possibleFormats.add("grade " + gradeNum);      // grade 9
                possibleFormats.add("g" + gradeNum);           // g9
                possibleFormats.add("g " + gradeNum);          // g 9
                possibleFormats.add(gradeNum);                 // just 9

                // Log all formats we're checking
                Log.d(TAG, "Checking for events matching user's year level: " + yearLevel);
                Log.d(TAG, "Possible formats to match: " + possibleFormats.toString());

                int matchCount = 0;

                for (DataSnapshot dataSnapshot : snapshot.getChildren()) {
                    Event event = dataSnapshot.getValue(Event.class);
                    if (event != null) {
                        event.setEventUID(dataSnapshot.getKey());

                        // Get eventFor and check against all possible formats
                        String eventFor = event.getEventFor();
                        if (eventFor != null) {
                            String normalizedEventFor = eventFor.toLowerCase().replace("-", "").trim();

                            boolean isMatch = false;
                            for (String format : possibleFormats) {
                                String normalizedFormat = format.toLowerCase().replace("-", "").trim();
                                if (normalizedEventFor.contains(normalizedFormat) ||
                                        normalizedFormat.contains(normalizedEventFor)) {
                                    isMatch = true;
                                    break;
                                }
                            }

                            if (isMatch) {
                                Log.d(TAG, "✓ MATCH FOUND - Event: " + event.getEventName() +
                                        ", EventFor: '" + eventFor + "' matches with student grade: '" +
                                        yearLevel + "'");

                                assignedEventList.add(event);
                                matchCount++;
                            } else {
                                Log.d(TAG, "✗ NO MATCH - Event: " + event.getEventName() +
                                        ", EventFor: '" + eventFor + "' doesn't match with student grade");
                            }

                            // Also add events for all students
                            if (normalizedEventFor.contains("all") ||
                                    normalizedEventFor.contains("everyone") ||
                                    normalizedEventFor.contains("allyear")) {

                                // Check if it's not already in the list (to avoid duplicates)
                                if (!assignedEventList.contains(event)) {
                                    Log.d(TAG, "✓ ADDED AS GENERAL EVENT - Event: " + event.getEventName() +
                                            ", EventFor: '" + eventFor + "'");
                                    assignedEventList.add(event);
                                    matchCount++;
                                }
                            }
                        }
                    }
                }

                // Sort assigned events by date
                Collections.sort(assignedEventList, (e1, e2) -> {
                    try {
                        LocalDate date1 = LocalDate.parse(e1.getStartDate(), DATE_FORMATTER);
                        LocalDate date2 = LocalDate.parse(e2.getStartDate(), DATE_FORMATTER);
                        return date1.compareTo(date2);
                    } catch (Exception e) {
                        e.printStackTrace();
                        return 0;
                    }
                });

                // Update the adapter
                assignedEventAdapter.updateEventList(assignedEventList);

                // Log the final result
                Log.d(TAG, "Found " + matchCount + " events for year level: " + currentUserYearLevel);

                // Toast message removed as requested
                if (assignedEventList.isEmpty()) {
                    // Keep the empty list notification
                    Toast.makeText(MainActivity2.this,
                            "No events found for your grade level: " + currentUserYearLevel,
                            Toast.LENGTH_LONG).show();
                }
                // Removed the "Found X events for your grade" toast
            }

            @Override
            public void onCancelled(@NonNull DatabaseError error) {
                Log.e(TAG, "Fetching assigned events failed: " + error.getMessage());
            }
        });
    }
}