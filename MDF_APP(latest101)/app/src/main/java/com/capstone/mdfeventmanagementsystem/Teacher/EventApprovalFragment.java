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
import com.google.firebase.database.ValueEventListener;

import java.util.ArrayList;
import java.util.List;

public class EventApprovalFragment extends Fragment {

    private static final String TAG = "EventApprovalTest";
    private RecyclerView recyclerView;
    private LinearLayout emptyContainer;
    private List<Event> pendingApprovalEvents = new ArrayList<>();
    private EventApprovalAdapter adapter;
    private DatabaseReference eventProposalsRef;

    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container, @Nullable Bundle savedInstanceState) {
        Log.d(TAG, "onCreateView: Fragment view creation started");
        View view = inflater.inflate(R.layout.fragment_event_approval, container, false);

        // Initialize views
        Log.d(TAG, "onCreateView: Initializing views");
        recyclerView = view.findViewById(R.id.recycler_approval_view);
        emptyContainer = view.findViewById(R.id.empty_approval_container);

        // Set up RecyclerView
        Log.d(TAG, "onCreateView: Setting up RecyclerView");
        recyclerView.setLayoutManager(new LinearLayoutManager(getContext()));
        adapter = new EventApprovalAdapter(getContext(), pendingApprovalEvents);
        recyclerView.setAdapter(adapter);

        // Show the empty state by default
        Log.d(TAG, "onCreateView: Setting initial visibility state - Empty view shown");
        recyclerView.setVisibility(View.GONE);
        emptyContainer.setVisibility(View.VISIBLE);

        // Initialize Firebase
        Log.d(TAG, "onCreateView: Initializing Firebase references");
        initFirebase();

        // Fetch data
        Log.d(TAG, "onCreateView: Starting to fetch pending events from Firebase");
        fetchPendingEvents();

        Log.d(TAG, "onCreateView: Fragment view creation completed");
        return view;
    }

    private void initFirebase() {
        Log.d(TAG, "initFirebase: Initializing Firebase database reference");
        // Get a reference to the eventProposals collection in the Firebase Realtime Database
        FirebaseDatabase database = FirebaseDatabase.getInstance();
        eventProposalsRef = database.getReference("eventProposals");
        Log.d(TAG, "initFirebase: Firebase reference set to 'eventProposals'");
    }

    /**
     * Fetches events that are pending approval from Firebase Realtime Database
     * Listens for real-time updates to the data
     */
    private void fetchPendingEvents() {
        Log.d(TAG, "fetchPendingEvents: Setting up listener for pending events");
        eventProposalsRef.orderByChild("status").equalTo("pending").addValueEventListener(new ValueEventListener() {
            @Override
            public void onDataChange(@NonNull DataSnapshot dataSnapshot) {
                Log.d(TAG, "onDataChange: Data received from Firebase");
                pendingApprovalEvents.clear();
                Log.d(TAG, "onDataChange: Cleared existing event list");

                int count = 0;
                for (DataSnapshot eventSnapshot : dataSnapshot.getChildren()) {
                    count++;
                    Log.d(TAG, "onDataChange: Processing event snapshot #" + count);

                    Event event = eventSnapshot.getValue(Event.class);

                    // Make sure to set the event ID from the Firebase key and ensure status is set
                    if (event != null) {
                        String eventId = eventSnapshot.getKey();
                        Log.d(TAG, "onDataChange: Adding event with ID: " + eventId);
                        event.setEventId(eventId);

                        // Ensure the status is "pending"
                        if (event.getStatus() == null) {
                            event.setStatus("pending");
                        }

                        pendingApprovalEvents.add(event);
                    } else {
                        Log.w(TAG, "onDataChange: Received null event from snapshot");
                    }
                }

                Log.d(TAG, "onDataChange: Processed " + count + " events, found " + pendingApprovalEvents.size() + " pending events");

                // Update UI based on whether we have events or not
                Log.d(TAG, "onDataChange: Updating UI with fetched events");
                updateUI();
            }

            @Override
            public void onCancelled(@NonNull DatabaseError databaseError) {
                Log.e(TAG, "onCancelled: Error fetching pending events: " + databaseError.getMessage());
                Log.e(TAG, "onCancelled: Error details: " + databaseError.getDetails());

                // Show empty state if there's an error
                Log.d(TAG, "onCancelled: Showing empty state due to error");
                recyclerView.setVisibility(View.GONE);
                emptyContainer.setVisibility(View.VISIBLE);
            }
        });
        Log.d(TAG, "fetchPendingEvents: Firebase listener setup completed");
    }

    /**
     * Updates the UI based on whether we have events pending approval or not
     */
    private void updateUI() {
        Log.d(TAG, "updateUI: Updating UI based on event list size: " + pendingApprovalEvents.size());

        if (pendingApprovalEvents.isEmpty()) {
            // No pending events, show empty state
            Log.d(TAG, "updateUI: No pending events, showing empty state");
            recyclerView.setVisibility(View.GONE);
            emptyContainer.setVisibility(View.VISIBLE);
        } else {
            // We have pending events, show the list
            Log.d(TAG, "updateUI: Found " + pendingApprovalEvents.size() + " pending events, showing recycler view");
            recyclerView.setVisibility(View.VISIBLE);
            emptyContainer.setVisibility(View.GONE);

            // Notify adapter of data change
            Log.d(TAG, "updateUI: Updating adapter with new events");
            adapter.updateEvents(pendingApprovalEvents);
        }
    }

    /**
     * Public method that can be called from activity to update the list manually
     * This can be useful if you need to refresh the data from outside the fragment
     */
    public void updatePendingApprovalEvents(List<Event> events) {
        Log.d(TAG, "updatePendingApprovalEvents: Manual update called from external source");

        if (events != null && !events.isEmpty()) {
            Log.d(TAG, "updatePendingApprovalEvents: Received " + events.size() + " events");
            pendingApprovalEvents.clear();
            pendingApprovalEvents.addAll(events);

            // Update the UI
            Log.d(TAG, "updatePendingApprovalEvents: Updating UI with manually provided events");
            updateUI();
        } else {
            // Keep showing empty view
            Log.d(TAG, "updatePendingApprovalEvents: Received null or empty events list");
            recyclerView.setVisibility(View.GONE);
            emptyContainer.setVisibility(View.VISIBLE);
        }
    }

    @Override
    public void onResume() {
        super.onResume();
        Log.d(TAG, "onResume: Fragment resumed");
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