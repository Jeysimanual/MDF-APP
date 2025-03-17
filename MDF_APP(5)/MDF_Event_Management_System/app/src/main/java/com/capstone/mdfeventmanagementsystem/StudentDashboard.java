package com.capstone.mdfeventmanagementsystem;

import android.content.Intent;
import android.os.Bundle;
import android.util.Log;

import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

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
    }

    private void fetchEventsFromFirebase() {
        databaseReference.addValueEventListener(new ValueEventListener() {
            @Override
            public void onDataChange(@NonNull DataSnapshot snapshot) {
                eventList.clear();

                for (DataSnapshot dataSnapshot : snapshot.getChildren()) {
                    Event event = dataSnapshot.getValue(Event.class);
                    if (event != null) {
                        event.setEventUID(dataSnapshot.getKey()); // Set eventUID from Firebase key
                        eventList.add(event);
                    }
                }

                // Sort events by start date
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

        Intent intent = new Intent(this, StudentDashboardInside.class);
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
        intent.putExtra("graceTime", event.getgraceTime());
        intent.putExtra("eventPhotoUrl", event.getEventPhotoUrl());

        startActivity(intent);
    }
}
