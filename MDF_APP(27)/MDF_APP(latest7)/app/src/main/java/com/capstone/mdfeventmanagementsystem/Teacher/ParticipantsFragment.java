package com.capstone.mdfeventmanagementsystem.Teacher;

import android.Manifest;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.net.Uri;
import android.os.Bundle;
import android.os.Environment;
import android.os.Handler;
import android.text.Editable;
import android.text.TextWatcher;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Button;
import android.widget.EditText;
import android.widget.ImageButton;
import android.widget.RadioGroup;
import android.widget.TextView;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.appcompat.app.AlertDialog;
import androidx.core.app.ActivityCompat;
import androidx.core.content.ContextCompat;
import androidx.core.content.FileProvider;
import androidx.fragment.app.Fragment;
import androidx.recyclerview.widget.DividerItemDecoration;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.capstone.mdfeventmanagementsystem.R;
import com.capstone.mdfeventmanagementsystem.Adapters.ParticipantsAdapter;
import com.capstone.mdfeventmanagementsystem.Models.Participant;
import com.google.firebase.database.DataSnapshot;
import com.google.firebase.database.DatabaseError;
import com.google.firebase.database.DatabaseReference;
import com.google.firebase.database.FirebaseDatabase;
import com.google.firebase.database.ValueEventListener;
import com.google.firebase.firestore.FirebaseFirestore;
import com.google.firebase.firestore.QueryDocumentSnapshot;

import java.io.File;
import java.io.FileWriter;
import java.io.IOException;
import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Calendar;
import java.util.Date;
import java.util.HashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.TimeZone;

public class ParticipantsFragment extends Fragment {
    private static final String TAG = "ParticipantsFragment";
    private static final int WRITE_EXTERNAL_STORAGE_REQUEST_CODE = 101;
    private String eventId;
    private RecyclerView recyclerView;
    private ParticipantsAdapter adapter;
    private TextView noParticipantsText;
    private EditText searchEditText;
    private ImageButton filterButton;
    private Button exportButton;
    private Button refreshButton;
    private FirebaseFirestore db;
    private List<Participant> participantList = new ArrayList<>();

    // Event data
    private String eventStartTime;
    private String eventEndTime;
    private String eventStartDate;
    private String eventEndDate;
    private String eventGraceTime;
    private boolean isMultiDayEvent = false;

    // Handler for automatic status updates
    private Handler statusUpdateHandler = new Handler();
    private Runnable statusUpdateRunnable;
    private static final long STATUS_UPDATE_INTERVAL = 5 * 60 * 1000; // 5 minutes

    public ParticipantsFragment() {
        // Required empty public constructor
    }

    public static ParticipantsFragment newInstance(String eventId) {
        ParticipantsFragment fragment = new ParticipantsFragment();
        Bundle args = new Bundle();
        args.putString("eventId", eventId);
        fragment.setArguments(args);
        return fragment;
    }

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        if (getArguments() != null) {
            eventId = getArguments().getString("eventId");
            Log.d(TAG, "Event ID received: " + eventId);
        } else {
            Log.e(TAG, "No event ID provided to fragment");
        }
        db = FirebaseFirestore.getInstance();
    }

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container,
                             Bundle savedInstanceState) {
        // Inflate the layout for this fragment
        View view = inflater.inflate(R.layout.fragment_participants, container, false);

        initViews(view);
        setupRecyclerView();

        // Load event details first
        loadEventDetails();

        setupListeners();
        setupStatusUpdateChecker();

        return view;
    }

    @Override
    public void onResume() {
        super.onResume();
        if (eventStartTime != null && eventEndTime != null) {
            // Update status when returning to fragment
            updateAttendanceStatusForAll();
        }
    }

    @Override
    public void onDestroy() {
        super.onDestroy();
        // Remove callbacks when fragment is destroyed
        if (statusUpdateHandler != null && statusUpdateRunnable != null) {
            statusUpdateHandler.removeCallbacks(statusUpdateRunnable);
        }
    }

    private void initViews(View view) {
        recyclerView = view.findViewById(R.id.participants_recycler_view);
        noParticipantsText = view.findViewById(R.id.no_participants_text);
        searchEditText = view.findViewById(R.id.search_edit_text);
        filterButton = view.findViewById(R.id.filter_button);
        exportButton = view.findViewById(R.id.export_button);

        // Add refresh button if not already in layout
        refreshButton = view.findViewById(R.id.refresh_button);
        if (refreshButton == null) {
            // If refresh button doesn't exist in layout, you might need to add it
            Log.d(TAG, "Refresh button not found in layout");
        }
    }

    private void setupRecyclerView() {
        adapter = new ParticipantsAdapter(getContext());
        recyclerView.setLayoutManager(new LinearLayoutManager(getContext()));
        recyclerView.addItemDecoration(new DividerItemDecoration(getContext(), DividerItemDecoration.VERTICAL));
        recyclerView.setAdapter(adapter);
    }

    private void loadEventDetails() {
        if (eventId == null || eventId.isEmpty()) {
            Log.e(TAG, "Event ID is null or empty");
            Toast.makeText(getContext(), "Invalid event ID", Toast.LENGTH_SHORT).show();
            return;
        }

        // Extract the event key from the full path
        String eventKey = eventId;
        if (eventId.contains("/")) {
            String[] parts = eventId.split("/");
            eventKey = parts[parts.length - 1];
        }

        DatabaseReference eventRef = FirebaseDatabase.getInstance().getReference("events").child(eventKey);
        String finalEventKey = eventKey;
        eventRef.addListenerForSingleValueEvent(new ValueEventListener() {
            @Override
            public void onDataChange(@NonNull DataSnapshot snapshot) {
                if (snapshot.exists()) {
                    // Get event details
                    eventStartDate = snapshot.child("startDate").getValue(String.class);
                    eventEndDate = snapshot.child("endDate").getValue(String.class);
                    eventStartTime = snapshot.child("startTime").getValue(String.class);
                    eventEndTime = snapshot.child("endTime").getValue(String.class);
                    eventGraceTime = snapshot.child("graceTime").getValue(String.class);
                    String eventSpan = snapshot.child("eventSpan").getValue(String.class);
                    isMultiDayEvent = "multi-day".equals(eventSpan);

                    Log.d(TAG, "Event details loaded - Start: " + eventStartDate + " " + eventStartTime +
                            ", End: " + (eventEndDate != null ? eventEndDate : eventStartDate) + " " + eventEndTime +
                            ", Grace: " + eventGraceTime);

                    // Now load participants after getting event details
                    loadParticipants();
                } else {
                    Log.e(TAG, "Event not found: " + finalEventKey);
                    Toast.makeText(getContext(), "Event not found", Toast.LENGTH_SHORT).show();
                }
            }

            @Override
            public void onCancelled(@NonNull DatabaseError error) {
                Log.e(TAG, "Error loading event details: " + error.getMessage());
                Toast.makeText(getContext(), "Error loading event details", Toast.LENGTH_SHORT).show();
            }
        });
    }

    private void loadParticipants() {
        if (eventId == null || eventId.isEmpty()) {
            Log.e(TAG, "Event ID is null or empty");
            noParticipantsText.setVisibility(View.VISIBLE);
            recyclerView.setVisibility(View.GONE);
            return;
        }

        Log.d(TAG, "Loading participants for event: " + eventId);

        // Show loading indicator
        Toast.makeText(getContext(), "Loading participants...", Toast.LENGTH_SHORT).show();

        // Use the same database reference as where your ticket count is coming from
        DatabaseReference studentsRef = FirebaseDatabase.getInstance().getReference("students");

        studentsRef.addListenerForSingleValueEvent(new ValueEventListener() {
            @Override
            public void onDataChange(@NonNull DataSnapshot dataSnapshot) {
                participantList.clear();

                // Extract the event key from the full path
                String eventKey = eventId;
                if (eventId.contains("/")) {
                    String[] parts = eventId.split("/");
                    eventKey = parts[parts.length - 1];
                }

                Log.d(TAG, "Searching for participants matching event key: " + eventKey);
                int count = 0;

                for (DataSnapshot studentSnapshot : dataSnapshot.getChildren()) {
                    // Log out the student ID to debug
                    String studentId = studentSnapshot.getKey();
                    Log.d(TAG, "Checking student: " + studentId);

                    // Get student name first - try different paths where name might be stored
                    String studentName = "Unknown";
                    if (studentSnapshot.hasChild("name")) {
                        studentName = studentSnapshot.child("name").getValue(String.class);
                    } else if (studentSnapshot.hasChild("studentName")) {
                        studentName = studentSnapshot.child("studentName").getValue(String.class);
                    } else if (studentSnapshot.hasChild("userName")) {
                        studentName = studentSnapshot.child("userName").getValue(String.class);
                    } else if (studentSnapshot.hasChild("fullName")) {
                        studentName = studentSnapshot.child("fullName").getValue(String.class);
                    } else if (studentSnapshot.hasChild("displayName")) {
                        studentName = studentSnapshot.child("displayName").getValue(String.class);
                    }

                    Log.d(TAG, "Student name: " + studentName);

                    // Get student section
                    String studentSection = "Unknown Section";
                    if (studentSnapshot.hasChild("section")) {
                        studentSection = studentSnapshot.child("section").getValue(String.class);
                    } else if (studentSnapshot.hasChild("studentSection")) {
                        studentSection = studentSnapshot.child("studentSection").getValue(String.class);
                    } else if (studentSnapshot.hasChild("class")) {
                        studentSection = studentSnapshot.child("class").getValue(String.class);
                    }

                    if (studentSnapshot.hasChild("tickets")) {
                        DataSnapshot ticketsSnapshot = studentSnapshot.child("tickets");
                        Log.d(TAG, "Student has " + ticketsSnapshot.getChildrenCount() + " tickets");

                        // Check if this student has a ticket for this event
                        for (DataSnapshot ticketSnapshot : ticketsSnapshot.getChildren()) {
                            String ticketKey = ticketSnapshot.getKey();
                            Log.d(TAG, "Checking ticket: " + ticketKey);

                            boolean isMatchingTicket = false;

                            // Method 1: Check if ticket key matches the event key
                            if (eventKey.equals(ticketKey)) {
                                isMatchingTicket = true;
                                Log.d(TAG, "Found matching ticket by key: " + ticketKey);
                            }

                            // Method 2: Look for an eventUID field in the ticket
                            if (!isMatchingTicket && ticketSnapshot.hasChild("eventUID")) {
                                String ticketEventUID = ticketSnapshot.child("eventUID").getValue(String.class);
                                if (eventId.equals(ticketEventUID) || eventKey.equals(ticketEventUID)) {
                                    isMatchingTicket = true;
                                    Log.d(TAG, "Found matching ticket by eventUID field: " + ticketEventUID);
                                }
                            }

                            if (isMatchingTicket) {
                                // Create a participant from this student with ticket
                                Participant participant = new Participant();
                                participant.setId(studentId);
                                participant.setName(studentName);
                                participant.setSection(studentSection);
                                participant.setTicketRef(ticketKey);

                                // Get attendance data based on multi-day or single-day event
                                processAttendanceData(participant, ticketSnapshot);

                                participantList.add(participant);
                                count++;
                                Log.d(TAG, "Added participant: " + participant.getName() + " with status: " + participant.getStatus());
                            }
                        }
                    }
                }

                Log.d(TAG, "Total participants found: " + count);

                if (participantList.isEmpty()) {
                    Log.d(TAG, "No participants found for this event");
                    noParticipantsText.setVisibility(View.VISIBLE);
                    recyclerView.setVisibility(View.GONE);
                } else {
                    Log.d(TAG, "Participants loaded: " + participantList.size());
                    noParticipantsText.setVisibility(View.GONE);
                    recyclerView.setVisibility(View.VISIBLE);

                    // Check and update statuses based on current time before displaying
                    updateAttendanceStatusForAll();

                    adapter.setParticipants(participantList);
                }
            }

            @Override
            public void onCancelled(@NonNull DatabaseError error) {
                Log.e(TAG, "Error loading participants: " + error.getMessage());
                Toast.makeText(getContext(), "Error loading participants: " + error.getMessage(), Toast.LENGTH_SHORT).show();
                noParticipantsText.setVisibility(View.VISIBLE);
                recyclerView.setVisibility(View.GONE);
            }
        });
    }

    // Modified to better handle attendance data for multi-day events
    private void processAttendanceData(Participant participant, DataSnapshot ticketSnapshot) {
        String currentDate = getCurrentDate();
        DataSnapshot attendanceDaysSnapshot = ticketSnapshot.child("attendanceDays");

        if (!attendanceDaysSnapshot.exists()) {
            // No attendance data exists yet - this means ticket is registered but no check-in
            participant.setTimeIn("");
            participant.setTimeOut("");
            participant.setStatus("Pending");
            return;
        }

        if (isMultiDayEvent) {
            // For multi-day events, find the current day's data
            boolean foundCurrentDay = false;

            for (DataSnapshot daySnapshot : attendanceDaysSnapshot.getChildren()) {
                if (daySnapshot.child("date").exists() &&
                        currentDate.equals(daySnapshot.child("date").getValue(String.class))) {

                    extractAttendanceData(participant, daySnapshot);
                    foundCurrentDay = true;
                    break;
                }
            }

            if (!foundCurrentDay) {
                // No data for today yet
                participant.setTimeIn("");
                participant.setTimeOut("");
                participant.setStatus("Pending");

                // Check if we're on a day after the start date but before the end date
                try {
                    SimpleDateFormat dateFormat = new SimpleDateFormat("yyyy-MM-dd", Locale.getDefault());
                    Date current = dateFormat.parse(currentDate);
                    Date startDate = dateFormat.parse(eventStartDate);
                    Date endDate = eventEndDate != null ? dateFormat.parse(eventEndDate) : startDate;

                    // If today's date is within the event span but no attendance record exists,
                    // we should try to create it when updating statuses
                    if ((current.after(startDate) || current.equals(startDate)) &&
                            (current.before(endDate) || current.equals(endDate))) {
                        Log.d(TAG, "Current date is within event span but no attendance record for: " +
                                participant.getName());
                    }
                } catch (ParseException e) {
                    Log.e(TAG, "Error parsing dates", e);
                }
            }
        } else {
            // For single-day events (original logic)
            DataSnapshot daySnapshot = attendanceDaysSnapshot.child("day_1");
            if (daySnapshot.exists()) {
                extractAttendanceData(participant, daySnapshot);
            } else {
                // No day_1 data exists yet
                participant.setTimeIn("");
                participant.setTimeOut("");
                participant.setStatus("Pending");
            }
        }
    }

    private void extractAttendanceData(Participant participant, DataSnapshot daySnapshot) {
        // Get check-in time
        String timeIn = "";
        if (daySnapshot.child("in").exists()) {
            timeIn = daySnapshot.child("in").getValue(String.class);
        }
        participant.setTimeIn(timeIn == null || "N/A".equals(timeIn) ? "" : timeIn);

        // Get check-out time
        String timeOut = "";
        if (daySnapshot.child("out").exists()) {
            timeOut = daySnapshot.child("out").getValue(String.class);
        }
        participant.setTimeOut(timeOut == null || "N/A".equals(timeOut) ? "" : timeOut);

        // Get attendance status from database if it exists
        String status = "Pending"; // Default status

        if (daySnapshot.child("attendance").exists()) {
            status = daySnapshot.child("attendance").getValue(String.class);
        } else if (daySnapshot.child("status").exists()) {
            status = daySnapshot.child("status").getValue(String.class);
        } else {
            // If no status found, determine it based on check-in/out times
            if (!participant.getTimeIn().isEmpty() && participant.getTimeOut().isEmpty()) {
                status = "Ongoing";
            } else if (!participant.getTimeIn().isEmpty() && !participant.getTimeOut().isEmpty()) {
                // Check if the student was late
                boolean isLate = false;
                if (daySnapshot.child("isLate").exists()) {
                    isLate = daySnapshot.child("isLate").getValue(Boolean.class);
                } else {
                    isLate = wasCheckInLate(participant.getTimeIn());
                }
                status = isLate ? "Late" : "Present";
            } else if (participant.getTimeIn().isEmpty() && participant.getTimeOut().isEmpty()) {
                status = "Pending";
            }
        }

        participant.setStatus(status);
    }

    private boolean wasCheckInLate(String checkInTime) {
        if (checkInTime == null || checkInTime.isEmpty() || eventStartTime == null || eventGraceTime == null) {
            return false;
        }

        try {
            // Parse times
            SimpleDateFormat timeFormat = new SimpleDateFormat("HH:mm", Locale.getDefault());
            Date startTimeDate = timeFormat.parse(eventStartTime);
            Date checkInTimeDate = timeFormat.parse(checkInTime);

            // Calculate grace period
            int graceMinutes = 0;
            try {
                graceMinutes = Integer.parseInt(eventGraceTime);
            } catch (NumberFormatException e) {
                Log.e(TAG, "Error parsing grace time: " + eventGraceTime);
            }

            // Add grace period to start time
            Calendar calendar = Calendar.getInstance();
            calendar.setTime(startTimeDate);
            calendar.add(Calendar.MINUTE, graceMinutes);
            Date graceEndTime = calendar.getTime();

            // Check if check-in time is after grace period
            return checkInTimeDate.after(graceEndTime);

        } catch (ParseException e) {
            Log.e(TAG, "Error parsing times for late check: " + e.getMessage());
            return false;
        }
    }


    private void updateAttendanceStatusForAll() {
        if (eventEndTime == null || eventStartTime == null) {
            Log.e(TAG, "Event time details not available for status update");
            return;
        }

        // Get current time to check if event has ended
        String currentDate = getCurrentDate();
        String currentTime = getCurrentTime();

        try {
            // Parse times
            SimpleDateFormat timeFormat = new SimpleDateFormat("HH:mm", Locale.getDefault());
            Date endTimeDate = timeFormat.parse(eventEndTime);
            Date currentTimeDate = timeFormat.parse(currentTime);

            // Add 1 hour to end time to determine absolute cutoff for automatic absent marking
            Calendar calendar = Calendar.getInstance();
            calendar.setTime(endTimeDate);
            calendar.add(Calendar.HOUR_OF_DAY, 1);
            Date cutoffTimeDate = calendar.getTime();

            // Calculate the 30-minute grace period cutoff time for participants who checked in but didn't check out
            Calendar calendarThirtyMin = Calendar.getInstance();
            calendarThirtyMin.setTime(endTimeDate);
            calendarThirtyMin.add(Calendar.MINUTE, 30);
            Date thirtyMinCutoffTime = calendarThirtyMin.getTime();

            // For multi-day events, we need to check both the current day and previous days
            if (isMultiDayEvent) {
                // Check if current day has ended
                boolean isCurrentDayEnded = isCurrentDayEnded(currentDate, currentTime);
                boolean pastCutoffTime = currentTimeDate.after(cutoffTimeDate);
                boolean pastThirtyMinCutoff = currentTimeDate.after(thirtyMinCutoffTime);

                Log.d(TAG, "Multi-day event - Current date: " + currentDate +
                        ", Current day ended: " + isCurrentDayEnded +
                        ", Past cutoff time: " + pastCutoffTime +
                        ", Past 30-min cutoff: " + pastThirtyMinCutoff);

                if (isCurrentDayEnded && pastCutoffTime) {
                    Log.d(TAG, "Multi-day event current day has ended and 1-hour grace period has passed. Updating attendance statuses.");
                    updateStatusesForMultiDayEvent(currentDate);
                } else if (isCurrentDayEnded && pastThirtyMinCutoff) {
                    Log.d(TAG, "Multi-day event current day has ended and 30-min grace period has passed. Updating incomplete check-outs.");
                    updateIncompleteCheckoutsForMultiDayEvent(currentDate);
                }
            } else {
                // Single day event logic (unchanged)
                boolean eventEnded = isEventEnded(currentDate);
                boolean pastCutoffTime = currentTimeDate.after(cutoffTimeDate);
                boolean pastThirtyMinCutoff = currentTimeDate.after(thirtyMinCutoffTime);

                Log.d(TAG, "Current time: " + currentTime + ", Event end time: " + eventEndTime);
                Log.d(TAG, "Event ended: " + eventEnded + ", Past cutoff time: " + pastCutoffTime);
                Log.d(TAG, "Past 30-min cutoff: " + pastThirtyMinCutoff);

                if (eventEnded && pastCutoffTime) {
                    Log.d(TAG, "Event has ended and 1-hour grace period has passed. Updating attendance statuses.");
                    updateStatusesAfterEvent();
                } else if (eventEnded && pastThirtyMinCutoff) {
                    Log.d(TAG, "Event has ended and 30-min grace period has passed. Updating incomplete check-outs.");
                    updateIncompleteCheckouts();
                } else if (eventEnded) {
                    Log.d(TAG, "Event has ended but waiting for grace periods to update statuses.");
                }
            }
        } catch (ParseException e) {
            Log.e(TAG, "Error parsing times for status update", e);
        }
    }

    // New method to check if the current day of a multi-day event has ended
    private boolean isCurrentDayEnded(String currentDate, String currentTime) {
        try {
            SimpleDateFormat dateFormat = new SimpleDateFormat("yyyy-MM-dd", Locale.getDefault());
            SimpleDateFormat timeFormat = new SimpleDateFormat("HH:mm", Locale.getDefault());

            // Parse dates
            Date current = dateFormat.parse(currentDate);
            Date startDate = dateFormat.parse(eventStartDate);
            Date endDate = eventEndDate != null ? dateFormat.parse(eventEndDate) : startDate;

            // If current date is after end date, the whole event has ended
            if (current.after(endDate)) {
                return true;
            }

            // If current date is before start date, event hasn't started yet
            if (current.before(startDate)) {
                return false;
            }

            // If we're on the end date, check if current time is after end time
            if (current.equals(endDate)) {
                Date endTime = timeFormat.parse(eventEndTime);
                Date currTime = timeFormat.parse(currentTime);
                return currTime.after(endTime);
            }

            // If we're on a day between start and end date, check if we've passed midnight
            // (assuming each day ends at midnight if not the end date)
            Date midnightTime = timeFormat.parse("23:59");
            Date currTime = timeFormat.parse(currentTime);
            return currTime.after(midnightTime);

        } catch (ParseException e) {
            Log.e(TAG, "Error checking if current day ended", e);
            return false;
        }
    }

    // New method to update statuses for multi-day events
    private void updateStatusesForMultiDayEvent(String currentDate) {
        // Extract the event key from the full path
        String eventKey = eventId;
        if (eventId.contains("/")) {
            String[] parts = eventId.split("/");
            eventKey = parts[parts.length - 1];
        }

        Log.d(TAG, "Starting updateStatusesForMultiDayEvent for date: " + currentDate + ", eventKey: " + eventKey);

        DatabaseReference studentsRef = FirebaseDatabase.getInstance().getReference("students");

        // For each participant
        for (Participant participant : participantList) {
            String studentId = participant.getId();
            String ticketRef = participant.getTicketRef();

            // Get reference to this participant's attendance days
            DatabaseReference attendanceDaysRef = studentsRef
                    .child(studentId)
                    .child("tickets")
                    .child(ticketRef)
                    .child("attendanceDays");

            // Query to get all attendance days for this participant
            attendanceDaysRef.addListenerForSingleValueEvent(new ValueEventListener() {
                @Override
                public void onDataChange(@NonNull DataSnapshot dataSnapshot) {
                    boolean foundCurrentDay = false;
                    String currentDayKey = null;

                    // Step 1: Find the key for the current day
                    for (DataSnapshot daySnapshot : dataSnapshot.getChildren()) {
                        if (daySnapshot.child("date").exists() &&
                                currentDate.equals(daySnapshot.child("date").getValue(String.class))) {
                            currentDayKey = daySnapshot.getKey();
                            foundCurrentDay = true;
                            break;
                        }
                    }

                    // Step 2: If we found the current day's data
                    if (foundCurrentDay && currentDayKey != null) {
                        DataSnapshot dayData = dataSnapshot.child(currentDayKey);

                        // Check if they have check-in time
                        boolean hasCheckIn = dayData.child("in").exists() &&
                                !dayData.child("in").getValue(String.class).isEmpty() &&
                                !"N/A".equals(dayData.child("in").getValue(String.class));

                        // Check if they have check-out time
                        boolean hasCheckOut = dayData.child("out").exists() &&
                                !dayData.child("out").getValue(String.class).isEmpty() &&
                                !"N/A".equals(dayData.child("out").getValue(String.class));

                        // If no check in and no check out, mark as absent
                        if (!hasCheckIn && !hasCheckOut) {
                            // Update status to "absent" in Firebase (lowercase to match your Firebase data structure)
                            attendanceDaysRef.child(currentDayKey).child("attendance").setValue("absent");
                            attendanceDaysRef.child(currentDayKey).child("status").setValue("absent");

                            // Update local participant object for UI
                            participant.setStatus("Absent");

                            Log.d(TAG, "Multi-day event: Marked student " + studentId +
                                    " as absent for day " + currentDayKey + " (no check-in or check-out)");
                        }
                        // If has check-in but no check-out
                        else if (hasCheckIn && !hasCheckOut) {
                            // Will be handled by updateIncompleteCheckoutsForMultiDayEvent
                        }
                    } else {
                        // No data found for current day - may need to create it
                        Log.d(TAG, "No attendance data found for current day: " + currentDate +
                                " for student: " + studentId);

                        // Create a new day entry with "absent" status
                        Map<String, Object> newDayData = new HashMap<>();
                        newDayData.put("date", currentDate);
                        newDayData.put("attendance", "absent");
                        newDayData.put("status", "absent");
                        newDayData.put("in", "N/A");
                        newDayData.put("out", "N/A");

                        // Generate a new day key (day_X where X is number of existing days + 1)
                        long dayCount = dataSnapshot.getChildrenCount();
                        String newDayKey = "day_" + (dayCount + 1);

                        attendanceDaysRef.child(newDayKey).setValue(newDayData)
                                .addOnSuccessListener(aVoid -> {
                                    Log.d(TAG, "Successfully added absent record for new day " +
                                            newDayKey + " for student " + studentId);

                                    // Update local participant object for UI
                                    participant.setStatus("Absent");
                                })
                                .addOnFailureListener(e -> {
                                    Log.e(TAG, "Failed to add absent record for new day", e);
                                });
                    }

                    // Update RecyclerView adapter if needed
                    if (adapter != null) {
                        adapter.notifyDataSetChanged();
                    }
                }

                @Override
                public void onCancelled(@NonNull DatabaseError error) {
                    Log.e(TAG, "Error querying attendance days", error.toException());
                }
            });
        }
    }

    // New method to update incomplete checkouts for multi-day events
    private void updateIncompleteCheckoutsForMultiDayEvent(String currentDate) {
        Log.d(TAG, "Starting updateIncompleteCheckoutsForMultiDayEvent for date: " + currentDate);

        // Extract the event key from the full path
        String eventKey = eventId;
        if (eventId.contains("/")) {
            String[] parts = eventId.split("/");
            eventKey = parts[parts.length - 1];
        }

        DatabaseReference studentsRef = FirebaseDatabase.getInstance().getReference("students");

        // For each participant
        for (Participant participant : participantList) {
            String studentId = participant.getId();
            String ticketRef = participant.getTicketRef();

            // Get reference to this participant's attendance days
            DatabaseReference attendanceDaysRef = studentsRef
                    .child(studentId)
                    .child("tickets")
                    .child(ticketRef)
                    .child("attendanceDays");

            // Query to get all attendance days for this participant
            attendanceDaysRef.addListenerForSingleValueEvent(new ValueEventListener() {
                @Override
                public void onDataChange(@NonNull DataSnapshot dataSnapshot) {
                    // Find the current day entry
                    String currentDayKey = null;

                    for (DataSnapshot daySnapshot : dataSnapshot.getChildren()) {
                        if (daySnapshot.child("date").exists() &&
                                currentDate.equals(daySnapshot.child("date").getValue(String.class))) {
                            currentDayKey = daySnapshot.getKey();
                            break;
                        }
                    }

                    // If we found the current day
                    if (currentDayKey != null) {
                        DataSnapshot dayData = dataSnapshot.child(currentDayKey);

                        // Check if they have check-in time but no check-out time
                        boolean hasCheckIn = dayData.child("in").exists() &&
                                !dayData.child("in").getValue(String.class).isEmpty() &&
                                !"N/A".equals(dayData.child("in").getValue(String.class));

                        boolean hasCheckOut = dayData.child("out").exists() &&
                                !dayData.child("out").getValue(String.class).isEmpty() &&
                                !"N/A".equals(dayData.child("out").getValue(String.class));

                        if (hasCheckIn && !hasCheckOut) {
                            // Update status to "absent" in Firebase
                            attendanceDaysRef.child(currentDayKey).child("attendance").setValue("absent");
                            attendanceDaysRef.child(currentDayKey).child("status").setValue("absent");

                            // Update local participant object for UI
                            participant.setStatus("Absent");

                            Log.d(TAG, "Multi-day event: Marked student " + studentId +
                                    " as absent for day " + currentDayKey + " (incomplete checkout)");
                        }
                    }

                    // Update RecyclerView adapter
                    if (adapter != null) {
                        adapter.notifyDataSetChanged();
                    }
                }

                @Override
                public void onCancelled(@NonNull DatabaseError error) {
                    Log.e(TAG, "Error querying attendance days", error.toException());
                }
            });
        }
    }

    private boolean isEventEnded(String currentDate) {
        try {
            SimpleDateFormat dateFormat = new SimpleDateFormat("yyyy-MM-dd", Locale.getDefault());

            // For multi-day events
            if (isMultiDayEvent && eventEndDate != null) {
                Date current = dateFormat.parse(currentDate);
                Date endDate = dateFormat.parse(eventEndDate);

                // If current date is after end date, event has ended
                if (current.after(endDate)) {
                    return true;
                }

                // If current date is the end date, need to check time
                if (current.equals(endDate)) {
                    SimpleDateFormat timeFormat = new SimpleDateFormat("HH:mm", Locale.getDefault());
                    Date endTime = timeFormat.parse(eventEndTime);
                    Date currentTime = timeFormat.parse(getCurrentTime());
                    return currentTime.after(endTime);
                }

                return false;
            }
            // For single-day events
            else {
                if (!currentDate.equals(eventStartDate)) {
                    Date current = dateFormat.parse(currentDate);
                    Date start = dateFormat.parse(eventStartDate);

                    // If current date is after event date, event has ended
                    return current.after(start);
                }

                // If current date is event date, check the time
                SimpleDateFormat timeFormat = new SimpleDateFormat("HH:mm", Locale.getDefault());
                Date endTime = timeFormat.parse(eventEndTime);
                Date currentTime = timeFormat.parse(getCurrentTime());
                return currentTime.after(endTime);
            }
        } catch (ParseException e) {
            Log.e(TAG, "Error checking if event ended", e);
            return false;
        }
    }

    private void updateStatusesAfterEvent() {
        // Extract the event key from the full path
        String eventKey = eventId;
        if (eventId.contains("/")) {
            String[] parts = eventId.split("/");
            eventKey = parts[parts.length - 1];
        }

        Log.d(TAG, "Starting updateStatusesAfterEvent for eventKey: " + eventKey);

        // Loop through all participants
        for (Participant participant : participantList) {
            String timeIn = participant.getTimeIn();
            String timeOut = participant.getTimeOut();
            String currentStatus = participant.getStatus();

            // Debug log to show current participant status
            Log.d(TAG, "Checking participant: " + participant.getName() +
                    ", Status: " + currentStatus +
                    ", TimeIn: " + (timeIn.isEmpty() ? "empty" : timeIn) +
                    ", TimeOut: " + (timeOut.isEmpty() ? "empty" : timeOut));

            // Check if status is lowercase "pending" (matching Firebase value)
            if ((currentStatus.equalsIgnoreCase("Pending") || currentStatus.equalsIgnoreCase("pending"))
                    && timeIn.isEmpty() && timeOut.isEmpty()) {

                // Mark status as "absent" for display
                participant.setStatus("Absent");

                // Update in Firebase with lowercase "absent"
                updatePendingToAbsentInFirebase(participant.getId(), participant.getTicketRef());

                Log.d(TAG, "Changed status from pending to absent for student " +
                        participant.getId() + " after event end + 1 hour");
            }
        }

        // Update adapter to refresh UI
        if (adapter != null) {
            adapter.notifyDataSetChanged();
        }
    }

    // New method specifically for updating "pending" to "absent"
    private void updatePendingToAbsentInFirebase(String studentId, String ticketRef) {
        if (studentId == null || studentId.isEmpty()) {
            Log.e(TAG, "Cannot update Firebase: studentId is null or empty");
            return;
        }
        if (ticketRef == null || ticketRef.isEmpty()) {
            Log.e(TAG, "Cannot update Firebase: ticketRef is null or empty");
            return;
        }

        // For now we use "day_1" as a static key — but even that can be null if dynamically set in future
        String dayKey = "day_1";
        if (dayKey == null || dayKey.isEmpty()) {
            Log.e(TAG, "Cannot update Firebase: dayKey is null or empty");
            return;
        }

        // Get reference to the database
        DatabaseReference studentsRef = FirebaseDatabase.getInstance().getReference("students");

        // Safely construct the path now
        DatabaseReference attendanceDaysRef = studentsRef
                .child(studentId)
                .child("tickets")
                .child(ticketRef)
                .child("attendanceDays")
                .child(dayKey);

        // Update both attendance and status fields in Firebase
        attendanceDaysRef.child("attendance").setValue("Absent")
                .addOnSuccessListener(aVoid -> {
                    Log.d(TAG, "Successfully updated attendance to 'absent' for student " + studentId);
                })
                .addOnFailureListener(e -> {
                    Log.e(TAG, "Failed to update attendance for student " + studentId, e);
                });
    }

    // New function to handle participants who checked in but didn't check out
    private void updateIncompleteCheckouts() {
        Log.d(TAG, "Starting updateIncompleteCheckouts for participants with check-in but no check-out");

        // Loop through all participants
        for (Participant participant : participantList) {
            String timeIn = participant.getTimeIn();
            String timeOut = participant.getTimeOut();
            String currentStatus = participant.getStatus();

            // Check for participants who have checked in (have inTime) but haven't checked out (no outTime)
            // regardless of their current display status
            if (!timeIn.isEmpty() && timeOut.isEmpty()) {
                // Debug log
                Log.d(TAG, "Found participant with incomplete checkout: " + participant.getName() +
                        ", TimeIn: " + timeIn + ", Status: " + currentStatus);

                // Update status to "Absent" in Firebase
                updateIncompleteCheckoutToAbsentInFirebase(participant.getId(), participant.getTicketRef());

                // Also update the local UI status
                participant.setStatus("Absent");
            }
        }

        // Update adapter to refresh UI with the new statuses
        if (adapter != null) {
            adapter.notifyDataSetChanged();
        }
    }

    // New method specifically for updating incomplete checkouts to "absent" in Firebase
    private void updateIncompleteCheckoutToAbsentInFirebase(String studentId, String ticketRef) {
        if (studentId == null || studentId.isEmpty()) {
            Log.e(TAG, "Cannot update Firebase: studentId is null or empty");
            return;
        }
        if (ticketRef == null || ticketRef.isEmpty()) {
            Log.e(TAG, "Cannot update Firebase: ticketRef is null or empty");
            return;
        }

        // For now we use "day_1" as a static key
        String dayKey = "day_1";
        if (dayKey == null || dayKey.isEmpty()) {
            Log.e(TAG, "Cannot update Firebase: dayKey is null or empty");
            return;
        }

        // Get reference to the database
        DatabaseReference studentsRef = FirebaseDatabase.getInstance().getReference("students");

        // Safely construct the path
        DatabaseReference attendanceDaysRef = studentsRef
                .child(studentId)
                .child("tickets")
                .child(ticketRef)
                .child("attendanceDays")
                .child(dayKey);

        // Update BOTH attendance AND status fields to "Absent"
        attendanceDaysRef.child("attendance").setValue("Absent")
                .addOnSuccessListener(aVoid -> {
                    Log.d(TAG, "Successfully updated attendance to 'Absent' for student "
                            + studentId + " who checked in but didn't check out");

                    // Also update the status field to "Absent"
                    attendanceDaysRef.child("status").setValue("Absent")
                            .addOnSuccessListener(aVoid2 -> {
                                Log.d(TAG, "Successfully updated status to 'Absent' for student "
                                        + studentId + " who checked in but didn't check out");
                            })
                            .addOnFailureListener(e -> {
                                Log.e(TAG, "Failed to update status for student with incomplete checkout "
                                        + studentId, e);
                            });
                })
                .addOnFailureListener(e -> {
                    Log.e(TAG, "Failed to update attendance for student with incomplete checkout "
                            + studentId, e);
                });
    }

    // Helper method to verify event times for debugging
    private void logEventTimes() {
        Log.d(TAG, "Event times - Start: " + eventStartTime +
                ", End: " + eventEndTime +
                ", Grace: " + eventGraceTime);

        try {
            SimpleDateFormat timeFormat = new SimpleDateFormat("HH:mm", Locale.getDefault());
            Date endTimeDate = timeFormat.parse(eventEndTime);

            Calendar calendar = Calendar.getInstance();
            calendar.setTime(endTimeDate);
            calendar.add(Calendar.HOUR_OF_DAY, 1);

            String cutoffTime = timeFormat.format(calendar.getTime());
            Log.d(TAG, "Cutoff time (end + 1hr): " + cutoffTime);

        } catch (ParseException e) {
            Log.e(TAG, "Error parsing event times", e);
        }
    }

    private String findCurrentDayKey(String studentId, String eventKey) {
        // For a multi-day event, find the correct day key based on current date
        String currentDate = getCurrentDate();

        // In a production app, you would query Firebase to find the exact day key
        // For this implementation, we'll use a simple day_N format where N starts from 1

        // Default fallback
        return "day_1";
    }
    private void setupListeners() {
        searchEditText.addTextChangedListener(new TextWatcher() {
            @Override
            public void beforeTextChanged(CharSequence s, int start, int count, int after) {
            }

            @Override
            public void onTextChanged(CharSequence s, int start, int before, int count) {
                adapter.filter(s.toString());
                if (adapter.getItemCount() == 0) {
                    noParticipantsText.setVisibility(View.VISIBLE);
                    recyclerView.setVisibility(View.GONE);
                } else {
                    noParticipantsText.setVisibility(View.GONE);
                    recyclerView.setVisibility(View.VISIBLE);
                }
            }

            @Override
            public void afterTextChanged(Editable s) {
            }
        });

        filterButton.setOnClickListener(v -> {
            showFilterDialog();
        });

        exportButton.setOnClickListener(v -> {
            if (ContextCompat.checkSelfPermission(requireContext(), Manifest.permission.WRITE_EXTERNAL_STORAGE)
                    != PackageManager.PERMISSION_GRANTED) {
                ActivityCompat.requestPermissions(requireActivity(),
                        new String[]{Manifest.permission.WRITE_EXTERNAL_STORAGE},
                        WRITE_EXTERNAL_STORAGE_REQUEST_CODE);
            } else {
                exportToCSV();
            }
        });

        // If the refresh button exists in the layout
        if (refreshButton != null) {
            refreshButton.setOnClickListener(v -> {
                refreshParticipants();
            });
        }
    }

    private void setupStatusUpdateChecker() {
        statusUpdateRunnable = new Runnable() {
            @Override
            public void run() {
                // Update attendance statuses based on current time
                updateAttendanceStatusForAll();

                // Schedule the next update
                statusUpdateHandler.postDelayed(this, STATUS_UPDATE_INTERVAL);
            }
        };

        // Start the periodic updates
        statusUpdateHandler.postDelayed(statusUpdateRunnable, STATUS_UPDATE_INTERVAL);
    }

    private void showFilterDialog() {
        // Create dialog
        AlertDialog.Builder builder = new AlertDialog.Builder(requireContext());
        View dialogView = getLayoutInflater().inflate(R.layout.filter_dialog, null);
        builder.setView(dialogView);
        AlertDialog dialog = builder.create();

        // Find views
        RadioGroup statusRadioGroup = dialogView.findViewById(R.id.status_radio_group);
        Button cancelButton = dialogView.findViewById(R.id.cancel_button);
        Button applyButton = dialogView.findViewById(R.id.apply_button);

        // Set click listeners
        cancelButton.setOnClickListener(v -> dialog.dismiss());

        applyButton.setOnClickListener(v -> {
            int selectedId = statusRadioGroup.getCheckedRadioButtonId();

            if (selectedId == R.id.radio_all) {
                filterByStatus(null); // No filter
            } else if (selectedId == R.id.radio_present) {
                filterByStatus("Present");
            } else if (selectedId == R.id.radio_absent) {
                filterByStatus("Absent");
            } else if (selectedId == R.id.radio_late) {
                filterByStatus("Late");
            } else if (selectedId == R.id.radio_ongoing) {
                filterByStatus("Ongoing");
            } else if (selectedId == R.id.radio_pending) {
                filterByStatus("Pending");
            }

            dialog.dismiss();
        });

        dialog.show();
    }

    private void filterByStatus(String status) {
        if (status == null) {
            // Reset to show all participants
            adapter.setParticipants(participantList);
            Toast.makeText(getContext(), "Showing all participants", Toast.LENGTH_SHORT).show();
        } else {
            // Filter by status
            List<Participant> filteredByStatus = new ArrayList<>();
            for (Participant participant : participantList) {
                if (status.equalsIgnoreCase(participant.getStatus())) {
                    filteredByStatus.add(participant);
                }
            }

            adapter.setParticipants(filteredByStatus);
            Toast.makeText(getContext(), "Filtered by status: " + status, Toast.LENGTH_SHORT).show();
        }
    }

    private void exportToCSV() {
        try {
            List<Participant> dataToExport = adapter.getFilteredList();
            if (dataToExport.isEmpty()) {
                Toast.makeText(getContext(), "No data to export", Toast.LENGTH_SHORT).show();
                return;
            }

            // Get event name to use in filename
            String eventName = "event"; // Default value
            // If you have the event name available, use it instead
            // eventName = currentEventName;

            SimpleDateFormat sdf = new SimpleDateFormat("yyyyMMdd_HHmmss", Locale.getDefault());
            String timestamp = sdf.format(new Date());
            String fileName = eventName + "_participants_" + timestamp + ".csv";

            File downloadsDir;
            if (android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.Q) {
                // For Android 10 and above, use app-specific storage
                downloadsDir = new File(requireContext().getExternalFilesDir(Environment.DIRECTORY_DOCUMENTS), "Exports");
            } else {
                // For older versions
                downloadsDir = Environment.getExternalStoragePublicDirectory(Environment.DIRECTORY_DOWNLOADS);
            }

            if (!downloadsDir.exists()) {
                if (!downloadsDir.mkdirs()) {
                    Log.e(TAG, "Failed to create directory for exports");
                    Toast.makeText(getContext(), "Failed to create directory for exports", Toast.LENGTH_SHORT).show();
                    return;
                }
            }

            File file = new File(downloadsDir, fileName);
            FileWriter writer = new FileWriter(file);

            // Write CSV header
            writer.append("Student ID,Name,Section,Status,Check In,Check Out\n");

            // Write data rows
            for (Participant participant : dataToExport) {
                writer.append(participant.getId())
                        .append(",")
                        .append(escapeCSV(participant.getName()))
                        .append(",")
                        .append(escapeCSV(participant.getSection()))
                        .append(",")
                        .append(participant.getStatus())
                        .append(",")
                        .append(participant.getTimeIn().isEmpty() ? "N/A" : participant.getTimeIn())
                        .append(",")
                        .append(participant.getTimeOut().isEmpty() ? "N/A" : participant.getTimeOut())
                        .append("\n");
            }

            writer.flush();
            writer.close();

            // Show success message
            Toast.makeText(getContext(), "Exported to " + file.getAbsolutePath(), Toast.LENGTH_LONG).show();

            // Share the file
            shareCSVFile(file);

        } catch (IOException e) {
            Log.e(TAG, "Error exporting to CSV: " + e.getMessage());
            Toast.makeText(getContext(), "Error exporting to CSV: " + e.getMessage(), Toast.LENGTH_SHORT).show();
        }
    }

    private String escapeCSV(String data) {
        if (data == null) {
            return "";
        }

        // Replace any double quotes with two double quotes
        String escaped = data.replace("\"", "\"\"");

        // If the data contains commas, newlines, or quotes, enclose it in quotes
        if (escaped.contains(",") || escaped.contains("\"") || escaped.contains("\n")) {
            escaped = "\"" + escaped + "\"";
        }

        return escaped;
    }

    private void shareCSVFile(File file) {
        Uri contentUri = FileProvider.getUriForFile(
                requireContext(),
                requireContext().getPackageName() + ".fileprovider",
                file);

        Intent shareIntent = new Intent();
        shareIntent.setAction(Intent.ACTION_SEND);
        shareIntent.putExtra(Intent.EXTRA_STREAM, contentUri);
        shareIntent.setType("text/csv");
        shareIntent.addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION);

        startActivity(Intent.createChooser(shareIntent, "Share CSV file via"));
    }

    private void refreshParticipants() {
        // Show loading indicator
        Toast.makeText(getContext(), "Refreshing participants list...", Toast.LENGTH_SHORT).show();

        // Clear search field
        searchEditText.setText("");

        // Reload participants
        loadParticipants();

        // Update attendance statuses
        updateAttendanceStatusForAll();
    }

    private String getCurrentDate() {
        SimpleDateFormat dateFormat = new SimpleDateFormat("yyyy-MM-dd", Locale.getDefault());
        dateFormat.setTimeZone(TimeZone.getDefault());
        return dateFormat.format(new Date());
    }

    private String getCurrentTime() {
        SimpleDateFormat timeFormat = new SimpleDateFormat("HH:mm", Locale.getDefault());
        timeFormat.setTimeZone(TimeZone.getDefault());
        return timeFormat.format(new Date());
    }

    @Override
    public void onRequestPermissionsResult(int requestCode, @NonNull String[] permissions, @NonNull int[] grantResults) {
        if (requestCode == WRITE_EXTERNAL_STORAGE_REQUEST_CODE) {
            if (grantResults.length > 0 && grantResults[0] == PackageManager.PERMISSION_GRANTED) {
                exportToCSV();
            } else {
                Toast.makeText(getContext(), "Storage permission denied. Cannot export CSV.", Toast.LENGTH_SHORT).show();
            }
        }
    }
}