package com.capstone.mdfeventmanagementsystem.Teacher;

import android.content.Context;
import android.os.Bundle;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ProgressBar;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.fragment.app.Fragment;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.capstone.mdfeventmanagementsystem.Adapters.EventAdapterTeacher;
import com.capstone.mdfeventmanagementsystem.R;
import com.capstone.mdfeventmanagementsystem.Student.Event;
import com.google.firebase.database.DataSnapshot;
import com.google.firebase.database.DatabaseError;
import com.google.firebase.database.DatabaseReference;
import com.google.firebase.database.FirebaseDatabase;
import com.google.firebase.database.ValueEventListener;

import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.List;

public class ExpiredEventsFragment extends Fragment {
    private static final String TAG = "ExpiredEventsFragment";
    private RecyclerView recyclerView;
    private EventAdapterTeacher eventAdapterTeacher;
    private List<Event> allEvents; // Full list of events
    private List<Event> expiredEventList; // Filtered list of expired events
    private TextView noEventMessage;
    private ProgressBar progressBar; // Add a ProgressBar for loading state
    private EventAdapterTeacher.OnEventClickListener onEventClickListener;
    private static final DateTimeFormatter DATE_FORMATTER = DateTimeFormatter.ofPattern("yyyy-MM-dd");
    private DatabaseReference databaseReference; // Firebase reference

    public ExpiredEventsFragment() {
        // Required empty public constructor
    }

    @Override
    public void onAttach(@NonNull Context context) {
        super.onAttach(context);
        try {
            onEventClickListener = (EventAdapterTeacher.OnEventClickListener) context;
        } catch (ClassCastException e) {
            Log.e(TAG, "onAttach: Context does not implement OnEventClickListener", e);
            throw new RuntimeException(context.toString() + " must implement OnEventClickListener");
        }
    }

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container,
                             Bundle savedInstanceState) {
        View view = inflater.inflate(R.layout.fragment_expired_events, container, false);
        return view;
    }

    @Override
    public void onViewCreated(@NonNull View view, @Nullable Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);

        recyclerView = view.findViewById(R.id.recyclerViewExpiredEvents);
        noEventMessage = view.findViewById(R.id.noExpiredEventMessage);
        progressBar = view.findViewById(R.id.progressBar); // Add this ID to your layout

        if (recyclerView == null) {
            Log.e(TAG, "recyclerView is null. Check R.layout.fragment_expired_events for recyclerViewExpiredEvents ID.");
            return;
        }
        if (noEventMessage == null) {
            Log.e(TAG, "noEventMessage is null. Check R.layout.fragment_expired_events for noExpiredEventMessage ID.");
            return;
        }
        if (progressBar == null) {
            Log.e(TAG, "progressBar is null. Check R.layout.fragment_expired_events for progressBar ID.");
            return;
        }

        recyclerView.setLayoutManager(new LinearLayoutManager(getContext()));
        allEvents = new ArrayList<>();
        expiredEventList = new ArrayList<>();

        eventAdapterTeacher = new EventAdapterTeacher(getContext(), expiredEventList, onEventClickListener);
        if (eventAdapterTeacher == null) {
            Log.e(TAG, "eventAdapterTeacher initialization failed.");
            return;
        }
        recyclerView.setAdapter(eventAdapterTeacher);

        // Show loading state initially
        progressBar.setVisibility(View.VISIBLE);
        noEventMessage.setVisibility(View.GONE);
        recyclerView.setVisibility(View.GONE);

        // Set up Firebase real-time listener
        setupFirebaseListener();
    }

    private void setupFirebaseListener() {
        databaseReference = FirebaseDatabase.getInstance().getReference("events"); // Adjust path as needed
        databaseReference.addValueEventListener(new ValueEventListener() {
            @Override
            public void onDataChange(@NonNull DataSnapshot snapshot) {
                List<Event> events = new ArrayList<>();
                for (DataSnapshot eventSnapshot : snapshot.getChildren()) {
                    Event event = eventSnapshot.getValue(Event.class);
                    if (event != null) {
                        events.add(event);
                    } else {
                        Log.w(TAG, "Failed to parse event from snapshot: " + eventSnapshot.toString());
                    }
                }
                updateExpiredEvents(events);
                progressBar.setVisibility(View.GONE); // Hide loading when data is ready
            }

            @Override
            public void onCancelled(@NonNull DatabaseError error) {
                Log.e(TAG, "Firebase data retrieval cancelled: " + error.getMessage());
                progressBar.setVisibility(View.GONE);
                noEventMessage.setVisibility(View.VISIBLE);
                noEventMessage.setText("Failed to load events. Check your internet connection.");
            }
        });
    }

    public void updateExpiredEvents(List<Event> allEvents) {
        if (getContext() == null || !isAdded()) {
            Log.e(TAG, "Context is null or fragment not added, cannot update events.");
            return;
        }

        // Initialize lists if null
        if (this.allEvents == null) this.allEvents = new ArrayList<>();
        if (expiredEventList == null) expiredEventList = new ArrayList<>();

        // Clear and update the full events list
        this.allEvents.clear();
        if (allEvents != null) {
            this.allEvents.addAll(allEvents);
        } else {
            Log.w(TAG, "updateExpiredEvents: allEvents is null, no data to process.");
        }

        // Filter for expired events
        expiredEventList.clear();
        LocalDate currentDate = LocalDate.now(); // Current date: 2025-09-13
        for (Event event : this.allEvents) {
            if (event == null) {
                Log.w(TAG, "Event is null in the list, skipping.");
                continue;
            }
            String endDateStr = event.getEndDate();
            if (endDateStr == null || endDateStr.isEmpty()) {
                Log.w(TAG, "Event endDate is null or empty for event: " + (event.getEventName() != null ? event.getEventName() : "Unknown"));
                continue;
            }
            try {
                LocalDate endDate = LocalDate.parse(endDateStr, DATE_FORMATTER);
                if (endDate.isBefore(currentDate) || endDate.isEqual(currentDate)) {
                    expiredEventList.add(event);
                }
            } catch (Exception e) {
                Log.e(TAG, "Error parsing endDate: " + endDateStr + " for event: " + event.getEventName(), e);
            }
        }

        // Log for debugging
        Log.d(TAG, "Updating expired events list with " + expiredEventList.size() + " events");

        // Update UI on the main thread
        if (noEventMessage != null && recyclerView != null) {
            if (expiredEventList.isEmpty()) {
                Log.d(TAG, "No expired events found, showing message");
                noEventMessage.setVisibility(View.VISIBLE);
                noEventMessage.setText("No expired events found. Past events will appear here once they expire.");
                recyclerView.setVisibility(View.GONE);
            } else {
                Log.d(TAG, "Expired events found, hiding message and showing events");
                noEventMessage.setVisibility(View.GONE); // Hide TextView when events are shown
                recyclerView.setVisibility(View.VISIBLE);
            }

            // Update adapter
            if (eventAdapterTeacher != null) {
                eventAdapterTeacher.updateEventList(expiredEventList);
                eventAdapterTeacher.notifyDataSetChanged();
            } else {
                Log.e(TAG, "eventAdapterTeacher is null, cannot update list.");
            }
        } else {
            Log.w(TAG, "UI components are null, skipping update.");
        }
    }

    @Override
    public void onDestroyView() {
        super.onDestroyView();
        if (databaseReference != null) {
            databaseReference.removeEventListener(new ValueEventListener() {
                @Override
                public void onDataChange(@NonNull DataSnapshot snapshot) {}
                @Override
                public void onCancelled(@NonNull DatabaseError error) {}
            }); // Remove listener to prevent memory leaks
        }
    }
}