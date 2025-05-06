package com.capstone.mdfeventmanagementsystem.Student;

import android.content.Intent;
import android.content.SharedPreferences;
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
import com.capstone.mdfeventmanagementsystem.Teacher.TeacherEventsInside;
import com.capstone.mdfeventmanagementsystem.Utilities.BaseActivity;
import com.google.android.material.bottomappbar.BottomAppBar;
import com.google.android.material.bottomnavigation.BottomNavigationView;
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

public class StudentDashboard extends BaseActivity implements EventAdapter.OnEventClickListener {

    private RecyclerView recyclerView;
    private EventAdapter eventAdapter;
    private List<Event> eventList;
    private DatabaseReference databaseReference;
    private static final DateTimeFormatter DATE_FORMATTER = DateTimeFormatter.ofPattern("yyyy-MM-dd");

    private TextView tabActive, tabExpired;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_student_dashboard);

        findViewById(R.id.fab_scan).setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                startActivity(new Intent(getApplicationContext(), QRCheckInActivity.class));
                overridePendingTransition(0, 0);
            }
        });

        BottomAppBar bottomAppBar = findViewById(R.id.bottomAppBar);
        bottomAppBar.setBackgroundTint(ColorStateList.valueOf(Color.WHITE));

        // Profile section click event
        findViewById(R.id.profile_image).setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                Intent intent = new Intent(StudentDashboard.this, ProfileActivity.class);
                startActivity(intent);
            }
        });

        // Tab elements
        // tabActive = findViewById(R.id.tabActive);
        // tabExpired = findViewById(R.id.tabExpired);
        recyclerView = findViewById(R.id.recyclerViewEvents);

        recyclerView.setLayoutManager(new LinearLayoutManager(this));
        eventList = new ArrayList<>();
        eventAdapter = new EventAdapter(this, eventList, this);
        recyclerView.setAdapter(eventAdapter);

        databaseReference = FirebaseDatabase.getInstance().getReference("events");

        // Load Active Events by default
        loadEvents("active");

        /**
         // Tab Click Listeners
         tabActive.setOnClickListener(v -> {
         setActiveTab(true);
         loadEvents("active");
         });

         tabExpired.setOnClickListener(v -> {
         setActiveTab(false);
         loadEvents("expired");
         }); */

        SharedPreferences prefs = getSharedPreferences("UserSession", MODE_PRIVATE);
        String studentID = prefs.getString("studentID", null);

        if (studentID == null) {
            Log.e("TestApp", "No studentID found in SharedPreferences!");
            Toast.makeText(this, "Student ID not found. Please log in again.", Toast.LENGTH_SHORT).show();
            Intent intent = new Intent(StudentDashboard.this, StudentLogin.class);
            startActivity(intent);
            finish();
        } else {
            Log.d("TestApp", "StudentID found: " + studentID);
        }

        // Setup bottom navigation
        BottomNavigationView bottomNavigationView = findViewById(R.id.bottom_navigation);
        bottomNavigationView.setSelectedItemId(R.id.nav_event);
        bottomNavigationView.setBackground(null);

        bottomNavigationView.setOnNavigationItemSelectedListener(item -> {
            int itemId = item.getItemId();
            if (itemId == R.id.nav_home) {
                startActivity(new Intent(getApplicationContext(), MainActivity2.class));
            } else if (itemId == R.id.nav_event) {
                return true;
            } else if (itemId == R.id.nav_ticket) {
                startActivity(new Intent(getApplicationContext(), StudentTickets.class));
            } else if (itemId == R.id.nav_cert) {
                startActivity(new Intent(getApplicationContext(), StudentCertificate.class));
            }
            overridePendingTransition(0, 0);
            return true;
        });
    }

    /**
     * Updates the selected tab's visual state

     private void setActiveTab(boolean isActiveTab) {
     if (isActiveTab) {
     tabActive.setBackgroundResource(R.drawable.tab_selected);
     tabExpired.setBackgroundColor(Color.TRANSPARENT);
     tabActive.setTextColor(Color.BLACK);
     tabExpired.setTextColor(Color.GRAY);
     } else {
     tabExpired.setBackgroundResource(R.drawable.tab_selected);
     tabActive.setBackgroundColor(Color.TRANSPARENT);
     tabExpired.setTextColor(Color.BLACK);
     tabActive.setTextColor(Color.GRAY);
     }
     }*/

    /**
     * Loads events from Firebase based on the selected tab (Active/Expired)
     */
    private void loadEvents(String type) {
        Log.d("TestApp", "Loading " + type + " events...");

        databaseReference.addValueEventListener(new ValueEventListener() {
            @Override
            public void onDataChange(@NonNull DataSnapshot snapshot) {
                eventList.clear();

                LocalDate today = LocalDate.now();

                for (DataSnapshot dataSnapshot : snapshot.getChildren()) {
                    Event event = dataSnapshot.getValue(Event.class);
                    if (event != null) {
                        try {
                            LocalDate eventDate = LocalDate.parse(event.getEndDate(), DATE_FORMATTER);
                            boolean isActive = !eventDate.isBefore(today);

                            // Filter events based on type
                            if ((type.equals("active") && isActive) || (type.equals("expired") && !isActive)) {
                                event.setEventUID(dataSnapshot.getKey());
                                eventList.add(event);
                                Log.d("TestApp", "Event loaded: " + event.getEventName());
                            }
                        } catch (Exception e) {
                            Log.e("TestApp", "Date parsing error: " + e.getMessage());
                        }
                    }
                }

                // Sort events by start date
                Collections.sort(eventList, (e1, e2) -> {
                    try {
                        LocalDate date1 = LocalDate.parse(e1.getStartDate(), DATE_FORMATTER);
                        LocalDate date2 = LocalDate.parse(e2.getStartDate(), DATE_FORMATTER);
                        return date1.compareTo(date2);
                    } catch (Exception e) {
                        Log.e("TestApp", "Sorting error: " + e.getMessage());
                        return 0;
                    }
                });

                eventAdapter.updateEventList(eventList);
                Log.d("TestApp", "Updated adapter with " + eventList.size() + " events.");
            }

            @Override
            public void onCancelled(@NonNull DatabaseError error) {
                Log.e("TestApp", "Firebase data fetch failed: " + error.getMessage());
            }
        });
    }

    @Override
    public void onEventClick(Event event) {
        Log.d("TestApp", "Event selected: " + event.getEventName());

        SharedPreferences prefs = getSharedPreferences("UserSession", MODE_PRIVATE);
        String userType = prefs.getString("userType", "student");

        Intent intent;
        if ("teacher".equals(userType)) {
            intent = new Intent(this, TeacherEventsInside.class);
        } else {
            intent = new Intent(this, StudentDashboardInside.class);
        }

        intent.putExtra("eventUID", event.getEventUID());
        intent.putExtra("eventName", event.getEventName());
        intent.putExtra("eventDescription", event.getEventDescription());
        intent.putExtra("startDate", event.getStartDate());
        intent.putExtra("endDate", event.getEndDate());
        intent.putExtra("startTime", event.getStartTime());
        intent.putExtra("endTime", event.getEndTime());
        intent.putExtra("venue", event.getVenue());
        intent.putExtra("eventSpan", event.getEventSpan());
        intent.putExtra("ticketType", event.getTicketType());
        intent.putExtra("graceTime", event.getGraceTime());
        intent.putExtra("eventPhotoUrl", event.getEventPhotoUrl());
        intent.putExtra("eventType", event.getEventType());
        intent.putExtra("eventFor", event.getEventFor());

        startActivity(intent);
    }
}
