package com.capstone.mdfeventmanagementsystem.Teacher;

import android.content.Context;
import android.os.Bundle;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.fragment.app.Fragment;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.capstone.mdfeventmanagementsystem.Adapters.EventAdapterTeacher;
import com.capstone.mdfeventmanagementsystem.R;
import com.capstone.mdfeventmanagementsystem.Student.Event;

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
    private EventAdapterTeacher.OnEventClickListener onEventClickListener;
    private static final DateTimeFormatter DATE_FORMATTER = DateTimeFormatter.ofPattern("yyyy-MM-dd");

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

        if (recyclerView == null) {
            Log.e(TAG, "recyclerView is null. Check R.layout.fragment_expired_events for recyclerViewExpiredEvents ID.");
            return;
        }
        if (noEventMessage == null) {
            Log.e(TAG, "noEventMessage is null. Check R.layout.fragment_expired_events for noExpiredEventMessage ID.");
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

        // Initialize UI state
        noEventMessage.setVisibility(View.VISIBLE);
        noEventMessage.setText("No expired events found. Past events will appear here one day after they expire.");
        recyclerView.setVisibility(View.GONE);
        Log.d(TAG, "Initialized UI: noEventMessage visible, recyclerView gone");
    }

    public void filterExpiredEvents(List<Event> allEvents) {
        // Initialize lists if null
        if (this.allEvents == null) this.allEvents = new ArrayList<>();
        if (expiredEventList == null) expiredEventList = new ArrayList<>();

        // Clear and update the full events list
        this.allEvents.clear();
        if (allEvents != null) {
            this.allEvents.addAll(allEvents);
            Log.d(TAG, "Received " + allEvents.size() + " events from TeacherEvents");
            for (Event event : allEvents) {
                Log.d(TAG, "Received event: UID=" + (event.getEventUID() != null ? event.getEventUID() : "null") +
                        ", Name=" + (event.getEventName() != null ? event.getEventName() : "null") +
                        ", Status=" + (event.getStatus() != null ? event.getStatus() : "null") +
                        ", EndDate=" + (event.getEndDate() != null ? event.getEndDate() : "null"));
            }
        } else {
            Log.w(TAG, "filterExpiredEvents: allEvents is null, no data to process.");
        }

        // Filter for expired events based on status and one day after endDate
        expiredEventList.clear();
        LocalDate currentDate = LocalDate.now(); // Current date: 2025-09-18
        Log.d(TAG, "Current date: " + currentDate);
        for (Event event : this.allEvents) {
            if (event == null) {
                Log.w(TAG, "Event is null in the list, skipping.");
                continue;
            }
            String status = event.getStatus();
            String endDateStr = event.getEndDate();
            String eventName = event.getEventName() != null ? event.getEventName() : "Unknown";
            String eventUID = event.getEventUID() != null ? event.getEventUID() : "Unknown";

            // Fallback: Treat null status as expired if endDate is before current date
            if (status == null) {
                Log.w(TAG, "Event status is null for event: UID=" + eventUID + ", Name=" + eventName + ". Checking endDate as fallback.");
            }
            if (status == null || status.equals("expired")) {
                if (endDateStr == null || endDateStr.isEmpty()) {
                    Log.w(TAG, "Event endDate is null or empty for event: UID=" + eventUID + ", Name=" + eventName);
                    continue;
                }
                try {
                    LocalDate endDate = LocalDate.parse(endDateStr, DATE_FORMATTER);
                    // Check if current date is on or after one day past the endDate
                    LocalDate displayDate = endDate.plusDays(1);
                    if (!currentDate.isBefore(displayDate)) {
                        expiredEventList.add(event);
                        Log.d(TAG, "Added expired event: UID=" + eventUID + ", Name=" + eventName +
                                ", EndDate=" + endDateStr + ", DisplayDate=" + displayDate);
                    } else {
                        Log.d(TAG, "Event not yet displayable (waiting for one day after endDate): UID=" + eventUID +
                                ", Name=" + eventName + ", EndDate=" + endDateStr + ", DisplayDate=" + displayDate);
                    }
                } catch (Exception e) {
                    Log.e(TAG, "Error parsing endDate: " + endDateStr + " for event: UID=" + eventUID + ", Name=" + eventName, e);
                }
            } else {
                Log.d(TAG, "Event status is not 'expired' for event: UID=" + eventUID + ", Name=" + eventName + ", Status=" + status);
            }
        }

        // Update UI
        updateUI(expiredEventList);
    }

    private void updateUI(List<Event> expiredEvents) {
        if (getContext() == null || !isAdded()) {
            Log.e(TAG, "Context is null or fragment not added, cannot update UI.");
            return;
        }

        // Log for debugging
        Log.d(TAG, "Updating UI with " + expiredEvents.size() + " expired events");
        for (Event event : expiredEvents) {
            Log.d(TAG, "UI event: UID=" + (event.getEventUID() != null ? event.getEventUID() : "Unknown") +
                    ", Name=" + (event.getEventName() != null ? event.getEventName() : "Unknown") +
                    ", EndDate=" + (event.getEndDate() != null ? event.getEndDate() : "null"));
        }

        if (noEventMessage != null && recyclerView != null) {
            if (expiredEvents.isEmpty()) {
                Log.d(TAG, "No expired events found, showing message");
                noEventMessage.setVisibility(View.VISIBLE);
                noEventMessage.setText("No expired events found. Past events will appear here one day after they expire.");
                recyclerView.setVisibility(View.GONE);
            } else {
                Log.d(TAG, "Expired events found, hiding message and showing events");
                noEventMessage.setVisibility(View.GONE);
                recyclerView.setVisibility(View.VISIBLE);
            }

            // Update adapter
            if (eventAdapterTeacher != null) {
                eventAdapterTeacher.updateEventList(expiredEvents);
                eventAdapterTeacher.notifyDataSetChanged();
                Log.d(TAG, "Adapter updated with " + expiredEvents.size() + " events");
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
        eventAdapterTeacher = null;
        recyclerView = null;
        noEventMessage = null;
    }
}