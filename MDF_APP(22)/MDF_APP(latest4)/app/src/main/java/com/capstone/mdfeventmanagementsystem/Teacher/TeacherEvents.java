package com.capstone.mdfeventmanagementsystem.Teacher;

import android.content.Intent;
import android.content.SharedPreferences;
import android.os.Bundle;
import android.util.Log;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.capstone.mdfeventmanagementsystem.Adapters.EventAdapter;
import com.capstone.mdfeventmanagementsystem.Student.Event;
import com.capstone.mdfeventmanagementsystem.R;
import com.capstone.mdfeventmanagementsystem.Utilities.BaseActivity;
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

public class TeacherEvents extends BaseActivity implements EventAdapter.OnEventClickListener {

    private RecyclerView recyclerView;
    private EventAdapter eventAdapter;
    private List<Event> eventList;
    private DatabaseReference databaseReference;
    private static final DateTimeFormatter DATE_FORMATTER = DateTimeFormatter.ofPattern("yyyy-MM-dd");
    private static final String TAG = "TeacherEvents";  // For logging

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_teacher_events);

        recyclerView = findViewById(R.id.recyclerViewTeacherEvents);
        recyclerView.setLayoutManager(new LinearLayoutManager(this));
        eventList = new ArrayList<>();

        // Initialize the adapter and pass "this" for click listener
        eventAdapter = new EventAdapter(this, eventList, this);
        recyclerView.setAdapter(eventAdapter);

        databaseReference = FirebaseDatabase.getInstance().getReference("events");
        fetchEventsFromFirebase();

        // Bottom Navigation
        BottomNavigationView bottomNavigationView = findViewById(R.id.bottom_navigation_teacher);
        bottomNavigationView.setSelectedItemId(R.id.nav_event_teacher);

        bottomNavigationView.setOnNavigationItemSelectedListener(item -> {
            int itemId = item.getItemId();
            if (itemId == R.id.nav_home_teacher) {
                startActivity(new Intent(getApplicationContext(), TeacherDashboard.class));
            } else if (itemId == R.id.nav_event_teacher) {
                return true;
            } else if (itemId == R.id.nav_scan_teacher) {
                startActivity(new Intent(getApplicationContext(), TeacherScanning.class));
            } else if (itemId == R.id.nav_profile_teacher) {
                startActivity(new Intent(getApplicationContext(), TeacherProfile.class));
            }
            overridePendingTransition(0, 0);
            return true;
        });
    }

    private void fetchEventsFromFirebase() {
        Log.d(TAG, "Fetching events from Firebase...");

        databaseReference.addValueEventListener(new ValueEventListener() {
            @Override
            public void onDataChange(@NonNull DataSnapshot snapshot) {
                if (snapshot.exists()) {
                    Log.d(TAG, "Firebase data received. Children count: " + snapshot.getChildrenCount());
                } else {
                    Log.w(TAG, "No events found in Firebase.");
                }

                eventList.clear();
                int eventCount = 0;

                for (DataSnapshot dataSnapshot : snapshot.getChildren()) {
                    Event event = dataSnapshot.getValue(Event.class);
                    if (event != null) {
                        event.setEventUID(dataSnapshot.getKey()); // Set eventUID from Firebase key
                        eventList.add(event);
                        Log.d(TAG, "Event fetched: " + event.getEventName() + " | UID: " + event.getEventUID());
                        eventCount++;
                    } else {
                        Log.w(TAG, "Null event object found in Firebase.");
                    }
                }

                Log.d(TAG, "Total events fetched: " + eventCount);

                // Sort events by start date
                Collections.sort(eventList, (e1, e2) -> {
                    try {
                        LocalDate date1 = LocalDate.parse(e1.getStartDate(), DATE_FORMATTER);
                        LocalDate date2 = LocalDate.parse(e2.getStartDate(), DATE_FORMATTER);
                        return date1.compareTo(date2);
                    } catch (Exception e) {
                        Log.e(TAG, "Error sorting events: " + e.getMessage(), e);
                        return 0;
                    }
                });

                Log.d(TAG, "Events sorted successfully. First event: " + (eventList.isEmpty() ? "None" : eventList.get(0).getEventName()));

                // Update the adapter and refresh RecyclerView
                eventAdapter.updateEventList(eventList);
                recyclerView.post(() -> eventAdapter.notifyDataSetChanged()); // Force UI update
            }

            @Override
            public void onCancelled(@NonNull DatabaseError error) {
                Log.e(TAG, "Firebase data fetch failed: " + error.getMessage(), error.toException());
            }
        });
    }


    @Override
    public void onEventClick(Event event) {
        Log.d(TAG, "Event selected: " + event.getEventName() + " | UID: " + event.getEventUID());

        // ✅ Check user session from SharedPreferences
        SharedPreferences prefs = getSharedPreferences("UserSession", MODE_PRIVATE);
        String userType = prefs.getString("userType", ""); // Default empty if not found

        if (!"teacher".equals(userType)) {
            Log.e(TAG, "Unauthorized access: Only teachers can view event details.");
            Toast.makeText(this, "Access denied. Only teachers can view event details.", Toast.LENGTH_SHORT).show();
            return; // Prevent unauthorized access
        }

        Intent intent = new Intent(this, TeacherEventsInside.class);

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

        Log.d(TAG, "Navigating to TeacherEventsInside");

        startActivity(intent);
    }


}
