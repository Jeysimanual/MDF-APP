package com.capstone.mdfeventmanagementsystem;

import android.content.Intent;
import android.content.SharedPreferences;
import android.os.Bundle;
import android.util.Log;

import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

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

public class StudentDashboard extends AppCompatActivity implements EventAdapter.OnEventClickListener {

    private RecyclerView recyclerView;
    private EventAdapter eventAdapter;
    private List<Event> eventList;
    private DatabaseReference databaseReference;
    private static final DateTimeFormatter DATE_FORMATTER = DateTimeFormatter.ofPattern("yyyy-MM-dd");

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_student_dashboard);

        recyclerView = findViewById(R.id.recyclerViewEvents);
        recyclerView.setLayoutManager(new LinearLayoutManager(this));
        eventList = new ArrayList<>();

        // Initialize the adapter and pass "this" for click listener
        eventAdapter = new EventAdapter(this, eventList, this);
        recyclerView.setAdapter(eventAdapter);

        databaseReference = FirebaseDatabase.getInstance().getReference("events");
        fetchEventsFromFirebase();

        BottomNavigationView bottomNavigationView = findViewById(R.id.bottom_navigation);
        bottomNavigationView.setSelectedItemId(R.id.nav_event);

        bottomNavigationView.setOnNavigationItemSelectedListener(item -> {
            int itemId = item.getItemId();
            if (itemId == R.id.nav_home) {
                startActivity(new Intent(getApplicationContext(), MainActivity2.class));
            } else if (itemId == R.id.nav_event) {
                return true;
            } else if (itemId == R.id.nav_ticket) {
                startActivity(new Intent(getApplicationContext(), StudentTickets.class));
            } else if (itemId == R.id.nav_scan) {
                startActivity(new Intent(getApplicationContext(), QRCheckInActivity.class));
            }
            overridePendingTransition(0, 0);
            return true;
        });

    }

    private void fetchEventsFromFirebase() {
        Log.d("TestApp", "Fetching events from Firebase...");

        databaseReference.addValueEventListener(new ValueEventListener() {
            @Override
            public void onDataChange(@NonNull DataSnapshot snapshot) {
                eventList.clear();

                if (!snapshot.exists()) {
                    Log.w("TestApp", "No events found in Firebase.");
                } else {
                    Log.d("TestApp", "Snapshot exists. Children count: " + snapshot.getChildrenCount());

                    for (DataSnapshot dataSnapshot : snapshot.getChildren()) {
                        Event event = dataSnapshot.getValue(Event.class);
                        if (event != null) {
                            event.setEventUID(dataSnapshot.getKey()); // Set eventUID from Firebase key
                            eventList.add(event);
                            Log.d("TestApp", "Event added: " + event.getEventName() + " | UID: " + event.getEventUID());
                        } else {
                            Log.w("TestApp", "Null event object found in snapshot.");
                        }
                    }
                }

                // Sort events by start date
                if (!eventList.isEmpty()) {
                    Collections.sort(eventList, (e1, e2) -> {
                        try {
                            LocalDate date1 = LocalDate.parse(e1.getStartDate(), DATE_FORMATTER);
                            LocalDate date2 = LocalDate.parse(e2.getStartDate(), DATE_FORMATTER);
                            return date1.compareTo(date2);
                        } catch (Exception e) {
                            Log.e("TestApp", "Date parsing error: " + e.getMessage());
                            e.printStackTrace();
                            return 0;
                        }
                    });

                    Log.d("TestApp", "Events sorted. First event: " + eventList.get(0).getEventName());
                } else {
                    Log.w("TestApp", "Event list is empty after fetching.");
                }

                eventAdapter.updateEventList(eventList);
                Log.d("TestApp", "Adapter updated. Total events: " + eventAdapter.getItemCount());
            }

            @Override
            public void onCancelled(@NonNull DatabaseError error) {
                Log.e("TestApp", "Firebase data fetch failed: " + error.getMessage(), error.toException());
            }
        });
    }


    @Override
    public void onEventClick(Event event) {
        Log.d("TestApp", "Event selected: " + event.getEventName() + " | UID: " + event.getEventUID());

        // ✅ Check user session from SharedPreferences
        SharedPreferences prefs = getSharedPreferences("UserSession", MODE_PRIVATE);
        String userType = prefs.getString("userType", "student"); // Default to "student" if not found

        Intent intent;
        if ("teacher".equals(userType)) {
            intent = new Intent(this, TeacherEventsInside.class);
        } else {
            intent = new Intent(this, StudentDashboardInside.class);
        }

        // ✅ Pass event details to the next activity
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

        Log.d("TestApp", "Navigating to: " + intent.getComponent().getClassName());

        startActivity(intent);
    }

}
