package com.capstone.mdfeventmanagementsystem.Teacher;

import android.os.Bundle;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.LinearLayout;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.fragment.app.Fragment;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.capstone.mdfeventmanagementsystem.Adapters.EventApprovalAdapter;
import com.capstone.mdfeventmanagementsystem.Models.Event;
import com.capstone.mdfeventmanagementsystem.R;
import com.google.firebase.database.DataSnapshot;
import com.google.firebase.database.DatabaseError;
import com.google.firebase.database.DatabaseReference;
import com.google.firebase.database.FirebaseDatabase;
import com.google.firebase.database.Query;
import com.google.firebase.database.ValueEventListener;

import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Calendar;
import java.util.Date;
import java.util.List;
import java.util.Locale;

public class EventApprovalFragment extends Fragment {

    private static final String TAG = "EventApprovalTest";
    private RecyclerView recyclerView;
    private LinearLayout emptyContainer;
    private List<Event> eventsToDisplay = new ArrayList<>();
    private EventApprovalAdapter adapter;
    private DatabaseReference eventProposalsRef;

    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container, @Nullable Bundle savedInstanceState) {
        Log.d(TAG, "onCreateView: Fragment view creation started");
        View view = inflater.inflate(R.layout.fragment_event_approval, container, false);

        Log.d(TAG, "onCreateView: Initializing views");
        recyclerView = view.findViewById(R.id.recycler_approval_view);
        emptyContainer = view.findViewById(R.id.empty_approval_container);

        Log.d(TAG, "onCreateView: Setting up RecyclerView");
        recyclerView.setLayoutManager(new LinearLayoutManager(getContext()));
        adapter = new EventApprovalAdapter(getContext(), eventsToDisplay);
        recyclerView.setAdapter(adapter);

        Log.d(TAG, "onCreateView: Setting initial visibility state - Empty view shown");
        recyclerView.setVisibility(View.GONE);
        emptyContainer.setVisibility(View.VISIBLE);

        Log.d(TAG, "onCreateView: Initializing Firebase references");
        initFirebase();

        Log.d(TAG, "onCreateView: Starting to fetch events from Firebase");
        fetchEvents();

        Log.d(TAG, "onCreateView: Fragment view creation completed");
        return view;
    }

    private void initFirebase() {
        Log.d(TAG, "initFirebase: Initializing Firebase database reference");
        FirebaseDatabase database = FirebaseDatabase.getInstance();
        eventProposalsRef = database.getReference("eventProposals");
        Log.d(TAG, "initFirebase: Firebase reference set to 'eventProposals'");
    }

    private void fetchEvents() {
        Log.d(TAG, "fetchEvents: Setting up listener for events");

        eventProposalsRef.addValueEventListener(new ValueEventListener() {
            @Override
            public void onDataChange(@NonNull DataSnapshot dataSnapshot) {
                Log.d(TAG, "onDataChange: Data received from Firebase");
                eventsToDisplay.clear();
                Log.d(TAG, "onDataChange: Cleared existing event list");

                int count = 0;
                for (DataSnapshot eventSnapshot : dataSnapshot.getChildren()) {
                    count++;
                    Log.d(TAG, "onDataChange: Processing event snapshot #" + count);

                    try {
                        String status = eventSnapshot.child("status").exists() ?
                                String.valueOf(eventSnapshot.child("status").getValue()) : "pending";

                        if (status.equals("pending")) {
                            if (shouldDeleteEvent(eventSnapshot)) {
                                Log.d(TAG, "onDataChange: Deleting expired event: " + eventSnapshot.getKey());
                                eventSnapshot.getRef().removeValue()
                                        .addOnSuccessListener(aVoid -> Log.d(TAG, "onDataChange: Successfully deleted expired event"))
                                        .addOnFailureListener(e -> Log.e(TAG, "onDataChange: Failed to delete expired event: " + e.getMessage()));
                                continue;
                            }
                        }

                        if (status.equals("pending") || status.equals("rejected")) {
                            Event event = new Event();
                            String eventId = eventSnapshot.getKey();
                            event.setEventId(eventId);

                            if (eventSnapshot.child("eventName").exists()) {
                                event.setEventName(String.valueOf(eventSnapshot.child("eventName").getValue()));
                            }
                            if (eventSnapshot.child("description").exists()) {
                                event.setDescription(String.valueOf(eventSnapshot.child("description").getValue()));
                            }
                            if (eventSnapshot.child("venue").exists()) {
                                event.setVenue(String.valueOf(eventSnapshot.child("venue").getValue()));
                            }
                            if (eventSnapshot.child("startDate").exists()) {
                                event.setStartDate(String.valueOf(eventSnapshot.child("startDate").getValue()));
                            }
                            if (eventSnapshot.child("endDate").exists()) {
                                event.setEndDate(String.valueOf(eventSnapshot.child("endDate").getValue()));
                            }
                            if (eventSnapshot.child("startTime").exists()) {
                                event.setStartTime(String.valueOf(eventSnapshot.child("startTime").getValue()));
                            }
                            if (eventSnapshot.child("endTime").exists()) {
                                event.setEndTime(String.valueOf(eventSnapshot.child("endTime").getValue()));
                            }
                            if (eventSnapshot.child("dateCreated").exists()) {
                                event.setDateCreated(String.valueOf(eventSnapshot.child("dateCreated").getValue()));
                            }

                            event.setStatus(status);

                            if (eventSnapshot.child("photoUrl").exists()) {
                                event.setPhotoUrl(String.valueOf(eventSnapshot.child("photoUrl").getValue()));
                            }
                            if (eventSnapshot.child("eventType").exists()) {
                                event.setEventType(String.valueOf(eventSnapshot.child("eventType").getValue()));
                            }
                            if (eventSnapshot.child("eventFor").exists()) {
                                event.setEventFor(String.valueOf(eventSnapshot.child("eventFor").getValue()));
                            }
                            if (eventSnapshot.child("eventSpan").exists()) {
                                event.setEventSpan(String.valueOf(eventSnapshot.child("eventSpan").getValue()));
                            }
                            if (eventSnapshot.child("graceTime").exists()) {
                                event.setGraceTime(String.valueOf(eventSnapshot.child("graceTime").getValue()));
                            }
                            if (eventSnapshot.child("rejectionReason").exists()) {
                                event.setRejectionReason(String.valueOf(eventSnapshot.child("rejectionReason").getValue()));
                            }
                            if (eventSnapshot.child("userId").exists()) {
                                event.setUserId(String.valueOf(eventSnapshot.child("userId").getValue()));
                            }

                            Log.d(TAG, "onDataChange: Adding event with ID: " + eventId + ", status: " + status);
                            eventsToDisplay.add(event);
                        }
                    } catch (Exception e) {
                        Log.e(TAG, "onDataChange: Error processing event: " + e.getMessage());
                    }
                }

                Log.d(TAG, "onDataChange: Processed " + count + " events, found " + eventsToDisplay.size() + " events to display");
                updateUI();
            }

            @Override
            public void onCancelled(@NonNull DatabaseError databaseError) {
                Log.e(TAG, "onCancelled: Error fetching events: " + databaseError.getMessage());
                Log.e(TAG, "onCancelled: Error details: " + databaseError.getDetails());
                Log.d(TAG, "onCancelled: Showing empty state due to error");
                recyclerView.setVisibility(View.GONE);
                emptyContainer.setVisibility(View.VISIBLE);
            }
        });
        Log.d(TAG, "fetchEvents: Firebase listener setup completed");
    }

    private boolean shouldDeleteEvent(DataSnapshot eventSnapshot) {
        try {
            Calendar currentDate = Calendar.getInstance();
            currentDate.setTime(new Date()); // Ensure current date is set correctly

            // Check creation date (7 days limit)
            if (eventSnapshot.child("dateCreated").exists()) {
                String dateCreatedStr = String.valueOf(eventSnapshot.child("dateCreated").getValue());
                SimpleDateFormat sdf = new SimpleDateFormat("MMMM dd, yyyy", Locale.US); // Adjusted for "July 23, 2025"
                Date dateCreated = sdf.parse(dateCreatedStr);

                if (dateCreated != null) {
                    Calendar creationLimit = Calendar.getInstance();
                    creationLimit.setTime(dateCreated);
                    creationLimit.add(Calendar.DAY_OF_MONTH, 7);

                    if (currentDate.after(creationLimit)) {
                        Log.d(TAG, "shouldDeleteEvent: Event expired (7 days limit) - ID: " + eventSnapshot.getKey());
                        return true;
                    }
                }
            }

            // Check start date
            if (eventSnapshot.child("startDate").exists()) {
                String startDateStr = String.valueOf(eventSnapshot.child("startDate").getValue());
                SimpleDateFormat sdf = new SimpleDateFormat("yyyy-MM-dd", Locale.getDefault()); // Assuming startDate is in "yyyy-MM-dd"
                Date startDate = sdf.parse(startDateStr);

                if (startDate != null) {
                    Calendar startDateCal = Calendar.getInstance();
                    startDateCal.setTime(startDate);

                    if (currentDate.after(startDateCal)) {
                        Log.d(TAG, "shouldDeleteEvent: Event expired (start date passed) - ID: " + eventSnapshot.getKey());
                        return true;
                    }
                }
            }
        } catch (ParseException e) {
            Log.e(TAG, "shouldDeleteEvent: Error parsing dates: " + e.getMessage());
        }
        return false;
    }

    private void updateUI() {
        Log.d(TAG, "updateUI: Updating UI based on event list size: " + eventsToDisplay.size());

        if (eventsToDisplay.isEmpty()) {
            Log.d(TAG, "updateUI: No events to display, showing empty state");
            recyclerView.setVisibility(View.GONE);
            emptyContainer.setVisibility(View.VISIBLE);
        } else {
            Log.d(TAG, "updateUI: Found " + eventsToDisplay.size() + " events, showing recycler view");
            recyclerView.setVisibility(View.VISIBLE);
            emptyContainer.setVisibility(View.GONE);
            Log.d(TAG, "updateUI: Updating adapter with new events");
            adapter.updateEvents(eventsToDisplay);
        }
    }

    public void updateEvents(List<Event> events) {
        Log.d(TAG, "updateEvents: Manual update called from external source");

        if (events != null && !events.isEmpty()) {
            Log.d(TAG, "updateEvents: Received " + events.size() + " events");
            eventsToDisplay.clear();
            eventsToDisplay.addAll(events);
            Log.d(TAG, "updateEvents: Updating UI with manually provided events");
            updateUI();
        } else {
            Log.d(TAG, "updateEvents: Received null or empty events list");
            recyclerView.setVisibility(View.GONE);
            emptyContainer.setVisibility(View.VISIBLE);
        }
    }

    @Override
    public void onResume() {
        super.onResume();
        Log.d(TAG, "onResume: Fragment resumed");
        fetchEvents();
    }

    @Override
    public void onPause() {
        super.onPause();
        Log.d(TAG, "onPause: Fragment paused");
    }

    @Override
    public void onDestroy() {
        super.onDestroy();
        Log.d(TAG, "onDestroy: Fragment destroyed");
    }
}