package com.capstone.mdfeventmanagementsystem.Teacher;

import android.Manifest;
import android.content.ContentResolver;
import android.content.ContentValues;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.database.Cursor;
import android.net.Uri;
import android.os.Bundle;
import android.os.Environment;
import android.os.Handler;
import android.provider.MediaStore;
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
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseUser;
import com.google.firebase.database.DataSnapshot;
import com.google.firebase.database.DatabaseError;
import com.google.firebase.database.DatabaseReference;
import com.google.firebase.database.FirebaseDatabase;
import com.google.firebase.database.ValueEventListener;
import com.google.firebase.firestore.FirebaseFirestore;
import com.google.firebase.firestore.QueryDocumentSnapshot;

import java.io.BufferedWriter;
import java.io.File;
import java.io.FileWriter;
import java.io.IOException;
import java.io.OutputStream;
import java.io.OutputStreamWriter;
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
    private String eventName = "event"; // Default value
    private RecyclerView recyclerView;
    private ParticipantsAdapter adapter;
    private TextView noParticipantsText;
    private EditText searchEditText;
    private ImageButton filterButton;
    private Button exportButton;
    private List<Participant> participantList = new ArrayList<>();

    // Event data
    private String eventStartTime;
    private String eventEndTime;
    private String eventStartDate;
    private String eventEndDate;
    private String eventGraceTime;
    private boolean isMultiDayEvent = false;
    private boolean isFilteringBySection = true; // Start with filtering by section

    // Handler for automatic status updates
    private Handler statusUpdateHandler = new Handler();
    private Runnable statusUpdateRunnable;

    private ValueEventListener participantsListener;
    private DatabaseReference studentsRef;
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
    }

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container,
                             Bundle savedInstanceState) {
        // Inflate the layout for this fragment
        View view = inflater.inflate(R.layout.fragment_participants, container, false);

        initViews(view);
        setupRecyclerView();

        // Load event details first, but use section filtering for participants
        loadEventDetails();

        // Fetch event name if available from Firebase Realtime Database
        fetchEventName();

        // Add a toggle button for filtering by section
        Button filterBySection = new Button(getContext());
        filterBySection.setText("Filter by My Section");
        filterBySection.setOnClickListener(v -> {
            // Toggle between all participants and section-filtered participants
            if (isFilteringBySection) {
                loadParticipants(); // Load all participants
                filterBySection.setText("Filter by My Section");
                isFilteringBySection = false;
            } else {
                loadParticipantsForTeacherSection(); // Load filtered participants
                filterBySection.setText("Show All Participants");
                isFilteringBySection = true;
            }
        });


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
            updatePendingStatusesForPastDays(); // Add this line to check past days
        }

        // Initialize multi-day event handling when returning to the fragment
        if (isMultiDayEvent) {
            initMultiDayEventHandling();
        }
    }

    // This method will initialize the handling of multi-day events
    private void initMultiDayEventHandling() {
        if (!isMultiDayEvent) {
            return;
        }

        try {
            SimpleDateFormat dateFormat = new SimpleDateFormat("yyyy-MM-dd", Locale.getDefault());
            Date startDate = dateFormat.parse(eventStartDate);
            Date endDate = dateFormat.parse(eventEndDate);
            Date currentDate = dateFormat.parse(getCurrentDate());

            // Check if we're within the event date range
            boolean isWithinEventDates = (currentDate.equals(startDate) || currentDate.after(startDate)) &&
                    (currentDate.equals(endDate) || currentDate.before(endDate));

            if (isWithinEventDates) {
                String dayKey = findDayKeyForCurrentDate();
                Log.d(TAG, "Within multi-day event date range. Current day key: " + dayKey);

                // You could perform additional initialization here if needed
            } else {
                Log.d(TAG, "Current date is outside the multi-day event range.");
            }
        } catch (ParseException e) {
            Log.e(TAG, "Error initializing multi-day event handling", e);
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

    }

    private void setupRecyclerView() {
        adapter = new ParticipantsAdapter(getContext());
        recyclerView.setLayoutManager(new LinearLayoutManager(getContext(), LinearLayoutManager.VERTICAL, false));
        // Removed the divider decoration since our layout styling handles visual separation
        recyclerView.setAdapter(adapter);
    }

    private void fetchEventName() {
        if (eventId == null || eventId.isEmpty()) {
            Log.e(TAG, "Cannot fetch event name: eventId is null or empty");
            return;
        }

        // Extract the event key from the full path if needed
        String eventKey;
        if (eventId.contains("/")) {
            String[] parts = eventId.split("/");
            eventKey = parts[parts.length - 1];
        } else {
            eventKey = eventId;
        }

        // Use only Realtime Database for fetching event name
        DatabaseReference eventRef = FirebaseDatabase.getInstance().getReference("events").child(eventKey);
        eventRef.addListenerForSingleValueEvent(new ValueEventListener() {
            @Override
            public void onDataChange(@NonNull DataSnapshot dataSnapshot) {
                if (dataSnapshot.exists() && dataSnapshot.hasChild("title")) {
                    eventName = dataSnapshot.child("title").getValue(String.class);
                    Log.d(TAG, "Fetched event name from Realtime DB: " + eventName);
                } else {
                    Log.d(TAG, "No event title found in Realtime Database");
                }
            }

            @Override
            public void onCancelled(@NonNull DatabaseError error) {
                Log.e(TAG, "Error fetching event name from Realtime DB: " + error.getMessage());
            }
        });
    }


    // Add this method to ParticipantsFragment class
    private void loadParticipantsForTeacherSection() {
        if (eventId == null || eventId.isEmpty()) {
            Log.e(TAG, "Event ID is null or empty");
            noParticipantsText.setVisibility(View.VISIBLE);
            recyclerView.setVisibility(View.GONE);
            return;
        }

        Log.d(TAG, "Loading participants for event: " + eventId + " filtered by teacher's section");

        // Show loading indicator
        Toast.makeText(getContext(), "Loading participants for your section...", Toast.LENGTH_SHORT).show();

        // First get the current teacher's year level and section
        FirebaseUser currentUser = FirebaseAuth.getInstance().getCurrentUser();
        if (currentUser == null) {
            Log.e(TAG, "No user logged in");
            Toast.makeText(getContext(), "Please log in to view section participants", Toast.LENGTH_SHORT).show();
            return;
        }

        String userEmail = currentUser.getEmail();
        DatabaseReference teachersRef = FirebaseDatabase.getInstance().getReference("teachers");

        teachersRef.orderByChild("email").equalTo(userEmail).addListenerForSingleValueEvent(new ValueEventListener() {
            @Override
            public void onDataChange(@NonNull DataSnapshot snapshot) {
                if (snapshot.exists()) {
                    for (DataSnapshot teacherSnapshot : snapshot.getChildren()) {
                        // Get year level (grade) and section from database
                        String yearLevel = teacherSnapshot.child("year_level_advisor").getValue(String.class);
                        String section = teacherSnapshot.child("section").getValue(String.class);

                        Log.d(TAG, "Teacher year level: " + yearLevel + ", section: " + section);

                        if (yearLevel != null && !yearLevel.isEmpty() && section != null && !section.isEmpty()) {
                            // Now load students matching this teacher's year level and section
                            loadFilteredParticipants(yearLevel, section);
                        } else {
                            // No year level or section assigned, fall back to loading all participants
                            Log.d(TAG, "Teacher has no year level or section assigned, loading all participants");
                            loadParticipants();
                        }
                    }
                } else {
                    // No teacher record found, fall back to loading all participants
                    Log.e(TAG, "No teacher record found for email: " + userEmail);
                    loadParticipants();
                }
            }

            @Override
            public void onCancelled(@NonNull DatabaseError error) {
                Log.e(TAG, "Error fetching teacher data: " + error.getMessage());
                Toast.makeText(getContext(), "Error loading teacher data", Toast.LENGTH_SHORT).show();
                // Fall back to loading all participants
                loadParticipants();
            }
        });
    }

    // Add this method to load participants filtered by teacher's section
    private void loadFilteredParticipants(String teacherYearLevel, String teacherSection) {
        // Extract the event key from the full path
        String eventKey = eventId;
        if (eventId.contains("/")) {
            String[] parts = eventId.split("/");
            eventKey = parts[parts.length - 1];
        }

        Log.d(TAG, "Loading participants for event: " + eventKey +
                " filtered by Grade " + teacherYearLevel + ", Section " + teacherSection);

        // Use the same database reference as where your student data is coming from
        DatabaseReference studentsRef = FirebaseDatabase.getInstance().getReference("students");

        String finalEventKey = eventKey;
        studentsRef.addListenerForSingleValueEvent(new ValueEventListener() {
            @Override
            public void onDataChange(@NonNull DataSnapshot dataSnapshot) {
                participantList.clear();
                int count = 0;

                for (DataSnapshot studentSnapshot : dataSnapshot.getChildren()) {
                    // Get student ID for debugging
                    String studentId = studentSnapshot.getKey();

                    // Check if student grade level and section match the teacher's
                    String studentYearLevel = studentSnapshot.child("yearLevel").getValue(String.class);
                    String studentSection = studentSnapshot.child("section").getValue(String.class);

                    // Format comparison - handle "Grade X" vs "X" format (similar to TeacherDashboard)
                    boolean yearLevelMatches = false;
                    if (studentYearLevel != null) {
                        // Match either exactly, or "Grade X" format with just "X"
                        yearLevelMatches = studentYearLevel.equals(teacherYearLevel) ||
                                studentYearLevel.equals("Grade " + teacherYearLevel) ||
                                (teacherYearLevel.startsWith("Grade ") &&
                                        studentYearLevel.equals(teacherYearLevel.substring(6)));
                    }

                    // Check if section matches
                    boolean sectionMatches = (studentSection != null && studentSection.equals(teacherSection));

                    // If student doesn't match teacher's year level and section, skip this student
                    if (!yearLevelMatches || !sectionMatches) {
                        Log.d(TAG, "Student " + studentId + " doesn't match teacher's section - skipping");
                        continue;
                    }

                    Log.d(TAG, "Student " + studentId + " matches teacher's section - checking for event ticket");

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

                    // Get student section
                    String studentSectionDisplay = "Unknown Section";
                    if (studentSnapshot.hasChild("section")) {
                        studentSectionDisplay = studentSnapshot.child("section").getValue(String.class);
                    } else if (studentSnapshot.hasChild("studentSection")) {
                        studentSectionDisplay = studentSnapshot.child("studentSection").getValue(String.class);
                    } else if (studentSnapshot.hasChild("class")) {
                        studentSectionDisplay = studentSnapshot.child("class").getValue(String.class);
                    }

                    if (studentSnapshot.hasChild("tickets")) {
                        DataSnapshot ticketsSnapshot = studentSnapshot.child("tickets");

                        // Check if this student has a ticket for this event
                        for (DataSnapshot ticketSnapshot : ticketsSnapshot.getChildren()) {
                            String ticketKey = ticketSnapshot.getKey();

                            boolean isMatchingTicket = false;

                            // Method 1: Check if ticket key matches the event key
                            if (finalEventKey.equals(ticketKey)) {
                                isMatchingTicket = true;
                                Log.d(TAG, "Found matching ticket by key: " + ticketKey);
                            }

                            // Method 2: Look for an eventUID field in the ticket
                            if (!isMatchingTicket && ticketSnapshot.hasChild("eventUID")) {
                                String ticketEventUID = ticketSnapshot.child("eventUID").getValue(String.class);
                                if (eventId.equals(ticketEventUID) || finalEventKey.equals(ticketEventUID)) {
                                    isMatchingTicket = true;
                                    Log.d(TAG, "Found matching ticket by eventUID field: " + ticketEventUID);
                                }
                            }

                            if (isMatchingTicket) {
                                // Create a participant from this student with ticket
                                Participant participant = new Participant();
                                participant.setId(studentId);
                                participant.setName(studentName);
                                participant.setSection(studentSectionDisplay);
                                participant.setTicketRef(ticketKey);

                                // Get attendance data based on multi-day or single-day event
                                processAttendanceData(participant, ticketSnapshot);

                                participantList.add(participant);
                                count++;
                                Log.d(TAG, "Added participant: " + participant.getName() +
                                        " with status: " + participant.getStatus());
                            }
                        }
                    }
                }

                Log.d(TAG, "Total section participants found: " + count);

                if (participantList.isEmpty()) {
                    Log.d(TAG, "No participants from your section found for this event");
                    noParticipantsText.setVisibility(View.VISIBLE);
                    recyclerView.setVisibility(View.GONE);
                } else {
                    Log.d(TAG, "Section participants loaded: " + participantList.size());
                    noParticipantsText.setVisibility(View.GONE);
                    recyclerView.setVisibility(View.VISIBLE);

                    // Check and update statuses based on current time before displaying
                    updateAttendanceStatusForAll();

                    adapter.setParticipants(participantList);
                }
            }

            @Override
            public void onCancelled(@NonNull DatabaseError error) {
                Log.e(TAG, "Error loading section participants: " + error.getMessage());
                Toast.makeText(getContext(), "Error loading participants: " + error.getMessage(),
                        Toast.LENGTH_SHORT).show();
                noParticipantsText.setVisibility(View.VISIBLE);
                recyclerView.setVisibility(View.GONE);
            }
        });
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
                    // By default, load participants filtered by teacher's section
                    loadParticipantsForTeacherSection();

                    // Initialize multi-day event handling if applicable
                    if (isMultiDayEvent) {
                        initMultiDayEventHandling();
                    }
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

    // Update the processAttendanceData method to better handle multi-day events
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
            // For multi-day events, find the current day's data or the most recent day if current day not found
            boolean foundCurrentDay = false;
            DataSnapshot mostRecentDaySnapshot = null;
            String mostRecentDate = "";

            for (DataSnapshot daySnapshot : attendanceDaysSnapshot.getChildren()) {
                if (daySnapshot.child("date").exists()) {
                    String dayDate = daySnapshot.child("date").getValue(String.class);

                    // Check if this is current day
                    if (currentDate.equals(dayDate)) {
                        extractAttendanceData(participant, daySnapshot);
                        foundCurrentDay = true;
                        break;
                    }

                    // Track the most recent day in case we don't find current day
                    if (mostRecentDate.isEmpty() || dayDate.compareTo(mostRecentDate) > 0) {
                        mostRecentDate = dayDate;
                        mostRecentDaySnapshot = daySnapshot;
                    }
                }
            }

            if (!foundCurrentDay) {
                if (mostRecentDaySnapshot != null) {
                    // Use the most recent day's data if current day not found
                    extractAttendanceData(participant, mostRecentDaySnapshot);
                } else {
                    // No data for any day yet
                    participant.setTimeIn("");
                    participant.setTimeOut("");
                    participant.setStatus("Pending");
                }
            }
        } else {
            // For single-day events
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


    // Update the updateAttendanceStatusForAll method to handle multi-day events
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

            boolean eventEnded = isEventEnded(currentDate);
            boolean pastCutoffTime = currentTimeDate.after(cutoffTimeDate);
            boolean pastThirtyMinCutoff = currentTimeDate.after(thirtyMinCutoffTime);

            // Debug logs
            Log.d(TAG, "Current date: " + currentDate + ", Current time: " + currentTime);
            Log.d(TAG, "Event end time: " + eventEndTime + ", Event end date: " +
                    (isMultiDayEvent ? eventEndDate : eventStartDate));
            Log.d(TAG, "Is multi-day event: " + isMultiDayEvent);
            Log.d(TAG, "Event ended: " + eventEnded + ", Past cutoff time: " + pastCutoffTime);
            Log.d(TAG, "Past 30-min cutoff: " + pastThirtyMinCutoff);
            Log.d(TAG, "Current day key: " + findDayKeyForCurrentDate());

            // If event has ended + 1 hour, update statuses
            if (eventEnded && pastCutoffTime) {
                Log.d(TAG, "Event has ended and 1-hour grace period has passed. Updating attendance statuses.");
                updateStatusesAfterEvent();
            } else if (eventEnded && pastThirtyMinCutoff) {
                Log.d(TAG, "Event has ended and 30-min grace period has passed. Updating incomplete check-outs.");
                updateIncompleteCheckouts();
            } else if (eventEnded) {
                Log.d(TAG, "Event has ended but waiting for grace periods to update statuses.");
            }

        } catch (ParseException e) {
            Log.e(TAG, "Error parsing times for status update", e);
        }
    }

    // Update the isEventEnded method to better handle multi-day events
    private boolean isEventEnded(String currentDate) {
        try {
            SimpleDateFormat dateFormat = new SimpleDateFormat("yyyy-MM-dd", Locale.getDefault());
            SimpleDateFormat timeFormat = new SimpleDateFormat("HH:mm", Locale.getDefault());

            Date currentDateObj = dateFormat.parse(currentDate);
            String currentTime = getCurrentTime();
            Date currentTimeObj = timeFormat.parse(currentTime);

            // For multi-day events
            if (isMultiDayEvent && eventEndDate != null) {
                Date endDateObj = dateFormat.parse(eventEndDate);

                // If current date is after end date, event has ended
                if (currentDateObj.after(endDateObj)) {
                    return true;
                }

                // If current date is the end date, check the time
                if (currentDateObj.equals(endDateObj) ||
                        (currentDateObj.getYear() == endDateObj.getYear() &&
                                currentDateObj.getMonth() == endDateObj.getMonth() &&
                                currentDateObj.getDate() == endDateObj.getDate())) {

                    Date endTimeObj = timeFormat.parse(eventEndTime);
                    return currentTimeObj.after(endTimeObj);
                }

                return false;
            }
            // For single-day events
            else {
                Date startDateObj = dateFormat.parse(eventStartDate);

                // If current date is after event date, event has ended
                if (currentDateObj.after(startDateObj)) {
                    return true;
                }

                // If current date is event date, check the time
                if (currentDateObj.equals(startDateObj) ||
                        (currentDateObj.getYear() == startDateObj.getYear() &&
                                currentDateObj.getMonth() == startDateObj.getMonth() &&
                                currentDateObj.getDate() == startDateObj.getDate())) {

                    Date endTimeObj = timeFormat.parse(eventEndTime);
                    return currentTimeObj.after(endTimeObj);
                }

                return false;
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

    // Update the updatePendingToAbsentInFirebase method to handle multi-day events
    private void updatePendingToAbsentInFirebase(String studentId, String ticketRef) {
        if (studentId == null || studentId.isEmpty() || ticketRef == null || ticketRef.isEmpty()) {
            Log.e(TAG, "Cannot update Firebase: studentId or ticketRef is null or empty");
            return;
        }

        // Get the appropriate day key based on the current date
        String dayKey = findDayKeyForCurrentDate();
        Log.d(TAG, "Updating status to Absent for student " + studentId + " on " + dayKey);

        // Get reference to the database
        DatabaseReference studentsRef = FirebaseDatabase.getInstance().getReference("students");

        // Safely construct the path now
        DatabaseReference attendanceDaysRef = studentsRef
                .child(studentId)
                .child("tickets")
                .child(ticketRef)
                .child("attendanceDays")
                .child(dayKey);

        // Check if this node exists first
        attendanceDaysRef.addListenerForSingleValueEvent(new ValueEventListener() {
            @Override
            public void onDataChange(@NonNull DataSnapshot snapshot) {
                if (snapshot.exists()) {
                    // Update both attendance and status fields in Firebase
                    Map<String, Object> updates = new HashMap<>();
                    updates.put("attendance", "Absent");
                    updates.put("status", "Absent");

                    attendanceDaysRef.updateChildren(updates)
                            .addOnSuccessListener(aVoid -> {
                                Log.d(TAG, "Successfully updated status to 'Absent' for student " +
                                        studentId + " on " + dayKey);
                            })
                            .addOnFailureListener(e -> {
                                Log.e(TAG, "Failed to update status for student " + studentId, e);
                            });
                } else {
                    // Create the day node if it doesn't exist
                    Map<String, Object> dayData = new HashMap<>();
                    dayData.put("attendance", "Absent");
                    dayData.put("status", "Absent");
                    dayData.put("date", getCurrentDate());

                    attendanceDaysRef.setValue(dayData)
                            .addOnSuccessListener(aVoid -> {
                                Log.d(TAG, "Created new day node with 'Absent' status for student " +
                                        studentId + " on " + dayKey);
                            })
                            .addOnFailureListener(e -> {
                                Log.e(TAG, "Failed to create day node for student " + studentId, e);
                            });
                }
            }

            @Override
            public void onCancelled(@NonNull DatabaseError error) {
                Log.e(TAG, "Error checking attendance day node: " + error.getMessage());
            }
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

    // Update the updateIncompleteCheckoutToAbsentInFirebase method to handle multi-day events
    private void updateIncompleteCheckoutToAbsentInFirebase(String studentId, String ticketRef) {
        if (studentId == null || studentId.isEmpty() || ticketRef == null || ticketRef.isEmpty()) {
            Log.e(TAG, "Cannot update Firebase: studentId or ticketRef is null or empty");
            return;
        }

        // Get the appropriate day key based on the current date
        String dayKey = findDayKeyForCurrentDate();
        Log.d(TAG, "Updating incomplete checkout to Absent for student " + studentId + " on " + dayKey);

        // Get reference to the database
        DatabaseReference studentsRef = FirebaseDatabase.getInstance().getReference("students");

        // Safely construct the path
        DatabaseReference attendanceDaysRef = studentsRef
                .child(studentId)
                .child("tickets")
                .child(ticketRef)
                .child("attendanceDays")
                .child(dayKey);

        // Check if this node exists first
        attendanceDaysRef.addListenerForSingleValueEvent(new ValueEventListener() {
            @Override
            public void onDataChange(@NonNull DataSnapshot snapshot) {
                if (snapshot.exists() && snapshot.hasChild("in") && !snapshot.hasChild("out")) {
                    // Update BOTH attendance AND status fields to "Absent"
                    Map<String, Object> updates = new HashMap<>();
                    updates.put("attendance", "Absent");
                    updates.put("status", "Absent");

                    attendanceDaysRef.updateChildren(updates)
                            .addOnSuccessListener(aVoid -> {
                                Log.d(TAG, "Successfully updated status to 'Absent' for student " +
                                        studentId + " with incomplete checkout on " + dayKey);
                            })
                            .addOnFailureListener(e -> {
                                Log.e(TAG, "Failed to update status for student with incomplete checkout " +
                                        studentId, e);
                            });
                }
            }

            @Override
            public void onCancelled(@NonNull DatabaseError error) {
                Log.e(TAG, "Error checking attendance day node: " + error.getMessage());
            }
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

    private String findDayKeyForCurrentDate() {
        if (!isMultiDayEvent) {
            return "day_1"; // For single-day events, always use day_1
        }

        String currentDate = getCurrentDate();

        // For multi-day events, determine which day we're on
        try {
            SimpleDateFormat dateFormat = new SimpleDateFormat("yyyy-MM-dd", Locale.getDefault());
            Date startDate = dateFormat.parse(eventStartDate);
            Date currentDateObj = dateFormat.parse(currentDate);

            // Calculate the difference in days
            long diffInMillies = Math.abs(currentDateObj.getTime() - startDate.getTime());
            long diffInDays = diffInMillies / (24 * 60 * 60 * 1000);

            // Add 1 because day_1 is the first day (not day_0)
            int dayNumber = (int) diffInDays + 1;

            return "day_" + dayNumber;
        } catch (ParseException e) {
            Log.e(TAG, "Error calculating day key for date: " + currentDate, e);
            return "day_1"; // Fallback to day_1
        }
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
            Log.d(TAG, "Export button clicked");
            checkPermissionsAndExport();
        });

    }
    private void checkPermissionsAndExport() {
        if (android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.Q) {
            // For Android 10+, we don't need WRITE_EXTERNAL_STORAGE for app-specific directories
            exportToCSV();
        } else {
            // For older Android versions, check and request storage permission
            if (ContextCompat.checkSelfPermission(requireContext(), Manifest.permission.WRITE_EXTERNAL_STORAGE)
                    != PackageManager.PERMISSION_GRANTED) {
                Log.d(TAG, "Requesting WRITE_EXTERNAL_STORAGE permission");
                ActivityCompat.requestPermissions(requireActivity(),
                        new String[]{Manifest.permission.WRITE_EXTERNAL_STORAGE},
                        WRITE_EXTERNAL_STORAGE_REQUEST_CODE);
            } else {
                Log.d(TAG, "Storage permission already granted");
                exportToCSV();
            }
        }
    }


    // Add this new method to update attendance status for all days in a multi-day event
    private void updatePendingStatusesForPastDays() {
        if (!isMultiDayEvent) {
            Log.d(TAG, "Not a multi-day event, skipping past days check");
            return;
        }

        String currentDate = getCurrentDate();
        Log.d(TAG, "Checking for missed attendance on previous days before " + currentDate);

        try {
            SimpleDateFormat dateFormat = new SimpleDateFormat("yyyy-MM-dd", Locale.getDefault());
            Date currentDateObj = dateFormat.parse(currentDate);
            Date startDateObj = dateFormat.parse(eventStartDate);
            Date endDateObj = dateFormat.parse(eventEndDate);

            // If current date is before the event start, nothing to do
            if (currentDateObj.before(startDateObj)) {
                Log.d(TAG, "Current date is before event start date, no past days to check");
                return;
            }

            // Calculate the date range to check (from start date to yesterday)
            Calendar calendar = Calendar.getInstance();
            calendar.setTime(currentDateObj);
            calendar.add(Calendar.DAY_OF_MONTH, -1); // Check up to yesterday
            Date yesterdayObj = calendar.getTime();

            // If yesterday is before the event start, nothing to do
            if (yesterdayObj.before(startDateObj)) {
                Log.d(TAG, "No past days within event range to check");
                return;
            }

            // Adjust end date of our check to be either yesterday or event end date, whichever is earlier
            Date checkEndDate = endDateObj.before(yesterdayObj) ? endDateObj : yesterdayObj;

            Log.d(TAG, "Will check days from " + dateFormat.format(startDateObj) +
                    " to " + dateFormat.format(checkEndDate));

            // Loop through each day from start to check end date
            calendar.setTime(startDateObj);
            Date checkDate = calendar.getTime();

            while (!checkDate.after(checkEndDate)) {
                String dateString = dateFormat.format(checkDate);
                int dayNumber = calculateDayNumber(dateString);
                String dayKey = "day_" + dayNumber;

                Log.d(TAG, "Checking for missed attendance on " + dateString + " (day key: " + dayKey + ")");

                // Update all participants for this day
                updatePendingStatusesForDay(dayKey, dateString);

                // Move to next day
                calendar.add(Calendar.DAY_OF_MONTH, 1);
                checkDate = calendar.getTime();
            }

        } catch (ParseException e) {
            Log.e(TAG, "Error parsing dates when checking past days", e);
        }
    }

    // Calculate day number based on date
    private int calculateDayNumber(String dateString) {
        try {
            SimpleDateFormat dateFormat = new SimpleDateFormat("yyyy-MM-dd", Locale.getDefault());
            Date date = dateFormat.parse(dateString);
            Date startDate = dateFormat.parse(eventStartDate);

            long diffInMillies = Math.abs(date.getTime() - startDate.getTime());
            long diffInDays = diffInMillies / (24 * 60 * 60 * 1000);

            return (int) diffInDays + 1; // Day 1 is the first day
        } catch (ParseException e) {
            Log.e(TAG, "Error calculating day number for date: " + dateString, e);
            return 1; // Default to day 1 if there's an error
        }
    }

    // Update participants for a specific day
    private void updatePendingStatusesForDay(String dayKey, String dateString) {
        Log.d(TAG, "Processing attendance status updates for " + dayKey + " (" + dateString + ")");

        // Extract the event key from the full path
        String eventKey = eventId;
        if (eventId.contains("/")) {
            String[] parts = eventId.split("/");
            eventKey = parts[parts.length - 1];
        }

        // Loop through all participants
        for (Participant participant : participantList) {
            // Check attendance status in Firebase first before updating
            checkAndUpdateAttendanceForPastDay(participant.getId(), participant.getTicketRef(), dayKey, dateString);
        }
    }

    // Check and update attendance status for a past day
    private void checkAndUpdateAttendanceForPastDay(String studentId, String ticketRef, String dayKey, String dateString) {
        if (studentId == null || studentId.isEmpty() || ticketRef == null || ticketRef.isEmpty()) {
            Log.e(TAG, "Cannot check status: studentId or ticketRef is null or empty");
            return;
        }

        DatabaseReference studentsRef = FirebaseDatabase.getInstance().getReference("students");
        DatabaseReference attendanceDayRef = studentsRef
                .child(studentId)
                .child("tickets")
                .child(ticketRef)
                .child("attendanceDays")
                .child(dayKey);

        attendanceDayRef.addListenerForSingleValueEvent(new ValueEventListener() {
            @Override
            public void onDataChange(@NonNull DataSnapshot snapshot) {
                // If the day node doesn't exist, need to create it with "Absent" attendance
                if (!snapshot.exists()) {
                    Log.d(TAG, "No attendance data for student " + studentId + " on " + dayKey +
                            ", marking attendance as Absent");

                    // Create the day node with Absent attendance (but not changing status)
                    Map<String, Object> dayData = new HashMap<>();
                    dayData.put("attendance", "Absent");
                    dayData.put("date", dateString);
                    // Note: We're not setting status to "Absent" here

                    attendanceDayRef.setValue(dayData)
                            .addOnSuccessListener(aVoid -> {
                                Log.d(TAG, "Created new day node with 'Absent' attendance for student " +
                                        studentId + " on " + dayKey);
                            })
                            .addOnFailureListener(e -> {
                                Log.e(TAG, "Failed to create day node for student " + studentId, e);
                            });
                    return;
                }

                // If node exists but has no check-in and check-out and status is pending, update only attendance to absent
                boolean hasCheckIn = snapshot.hasChild("in") &&
                        snapshot.child("in").getValue(String.class) != null &&
                        !snapshot.child("in").getValue(String.class).isEmpty();

                boolean hasCheckOut = snapshot.hasChild("out") &&
                        snapshot.child("out").getValue(String.class) != null &&
                        !snapshot.child("out").getValue(String.class).isEmpty();

                String status = "Pending";
                if (snapshot.hasChild("attendance")) {
                    status = snapshot.child("attendance").getValue(String.class);
                } else if (snapshot.hasChild("status")) {
                    status = snapshot.child("status").getValue(String.class);
                }

                if (!hasCheckIn && !hasCheckOut && status.equalsIgnoreCase("Pending")) {
                    Log.d(TAG, "Found participant with pending status and no check-in/out on " +
                            dayKey + ", updating attendance to Absent");

                    // Update only attendance field to "Absent"
                    Map<String, Object> updates = new HashMap<>();
                    updates.put("attendance", "Absent");
                    // Removed: updates.put("status", "Absent");

                    attendanceDayRef.updateChildren(updates)
                            .addOnSuccessListener(aVoid -> {
                                Log.d(TAG, "Successfully updated attendance to 'Absent' for student " +
                                        studentId + " on " + dayKey);
                            })
                            .addOnFailureListener(e -> {
                                Log.e(TAG, "Failed to update attendance for student " + studentId, e);
                            });
                } else if (hasCheckIn && !hasCheckOut) {
                    Log.d(TAG, "Found participant with check-in but no check-out on " +
                            dayKey + ", updating attendance to Absent");

                    // Update only attendance field to "Absent" for incomplete checkout
                    Map<String, Object> updates = new HashMap<>();
                    updates.put("attendance", "Absent");
                    // Removed: updates.put("status", "Absent");

                    attendanceDayRef.updateChildren(updates)
                            .addOnSuccessListener(aVoid -> {
                                Log.d(TAG, "Successfully updated attendance for incomplete checkout to 'Absent' for student " +
                                        studentId + " on " + dayKey);
                            })
                            .addOnFailureListener(e -> {
                                Log.e(TAG, "Failed to update attendance for student " + studentId, e);
                            });
                }
            }

            @Override
            public void onCancelled(@NonNull DatabaseError error) {
                Log.e(TAG, "Error checking attendance day node: " + error.getMessage());
            }
        });
    }

    // Update the setupStatusUpdateChecker method to check past days too
    private void setupStatusUpdateChecker() {
        // Initialize the status update runnable
        statusUpdateRunnable = new Runnable() {
            @Override
            public void run() {
                Log.d(TAG, "Running scheduled attendance status update check");
                updateAttendanceStatusForAll();
                updatePendingStatusesForPastDays(); // Add this line to check past days

                // Schedule the next update
                statusUpdateHandler.postDelayed(this, STATUS_UPDATE_INTERVAL);
            }
        };

        // Start the periodic updates
        statusUpdateHandler.post(statusUpdateRunnable);
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
                Log.d(TAG, "No data to export");
                Toast.makeText(getContext(), "No data to export", Toast.LENGTH_SHORT).show();
                return;
            }

            Log.d(TAG, "Starting CSV export of " + dataToExport.size() + " participants");

            // Use event name for filename (sanitized)
            String sanitizedEventName = eventName.replaceAll("[^a-zA-Z0-9]", "_");
            String baseFileName = sanitizedEventName + "_PARTICIPANTS.csv";
            Log.d(TAG, "Base export filename: " + baseFileName);

            // Check if the file already exists and handle accordingly
            checkFileExistsAndProceed(baseFileName, dataToExport);

        } catch (Exception e) {
            Log.e(TAG, "Error starting export process: " + e.getMessage(), e);
            Toast.makeText(getContext(), "Error exporting data: " + e.getMessage(), Toast.LENGTH_SHORT).show();
        }
    }

    private void checkFileExistsAndProceed(String baseFileName, List<Participant> dataToExport) {
        if (android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.Q) {
            // For Android 10+, check MediaStore for existing files
            checkFileExistsInMediaStore(baseFileName, dataToExport);
        } else {
            // For older Android versions, check file existence directly
            File downloadsDir = Environment.getExternalStoragePublicDirectory(Environment.DIRECTORY_DOWNLOADS);
            File file = new File(downloadsDir, baseFileName);

            if (file.exists()) {
                // File exists, ask user if they want to download again
                showFileExistsDialog(baseFileName, dataToExport);
            } else {
                // File doesn't exist, proceed with export
                writeCSVFile(baseFileName, dataToExport);
            }
        }
    }

    private void checkFileExistsInMediaStore(String baseFileName, List<Participant> dataToExport) {
        ContentResolver resolver = requireContext().getContentResolver();
        Uri contentUri = MediaStore.Downloads.EXTERNAL_CONTENT_URI;

        // Query to check if file exists
        Cursor cursor = resolver.query(
                contentUri,
                new String[] { MediaStore.Downloads._ID },
                MediaStore.Downloads.DISPLAY_NAME + "=?",
                new String[] { baseFileName },
                null
        );

        boolean fileExists = cursor != null && cursor.getCount() > 0;
        if (cursor != null) cursor.close();

        if (fileExists) {
            // File exists, ask user if they want to download again
            showFileExistsDialog(baseFileName, dataToExport);
        } else {
            // File doesn't exist, proceed with export
            writeCSVFile(baseFileName, dataToExport);
        }
    }

    private void showFileExistsDialog(String baseFileName, List<Participant> dataToExport) {
        AlertDialog.Builder builder = new AlertDialog.Builder(requireContext());
        builder.setTitle("File Already Exists")
                .setMessage("A file named '" + baseFileName + "' already exists. Do you want to download it again?")
                .setPositiveButton("Yes", (dialog, which) -> {
                    // Get a new filename with incremented counter
                    String newFileName = getIncrementedFileName(baseFileName);
                    writeCSVFile(newFileName, dataToExport);
                })
                .setNegativeButton("Cancel", (dialog, which) -> dialog.dismiss())
                .show();
    }

    private String getIncrementedFileName(String baseFileName) {
        String nameWithoutExtension = baseFileName.substring(0, baseFileName.lastIndexOf("."));
        String extension = baseFileName.substring(baseFileName.lastIndexOf("."));

        // Try to find existing files with incremented numbers
        int counter = 2;
        String newFileName = nameWithoutExtension + "(" + counter + ")" + extension;

        if (android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.Q) {
            // For Android 10+, check in MediaStore
            ContentResolver resolver = requireContext().getContentResolver();

            while (true) {
                Cursor cursor = resolver.query(
                        MediaStore.Downloads.EXTERNAL_CONTENT_URI,
                        new String[] { MediaStore.Downloads._ID },
                        MediaStore.Downloads.DISPLAY_NAME + "=?",
                        new String[] { newFileName },
                        null
                );

                boolean exists = cursor != null && cursor.getCount() > 0;
                if (cursor != null) cursor.close();

                if (!exists) break;

                counter++;
                newFileName = nameWithoutExtension + "(" + counter + ")" + extension;
            }
        } else {
            // For older Android versions, check file system directly
            File downloadsDir = Environment.getExternalStoragePublicDirectory(Environment.DIRECTORY_DOWNLOADS);
            File newFile = new File(downloadsDir, newFileName);

            while (newFile.exists()) {
                counter++;
                newFileName = nameWithoutExtension + "(" + counter + ")" + extension;
                newFile = new File(downloadsDir, newFileName);
            }
        }

        return newFileName;
    }

    private void writeCSVFile(String fileName, List<Participant> dataToExport) {
        try {
            Log.d(TAG, "Writing to file: " + fileName);
            Uri fileUri;

            if (android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.Q) {
                // For Android 10+, use MediaStore to make the file visible in Downloads
                ContentValues contentValues = new ContentValues();
                contentValues.put(MediaStore.Downloads.DISPLAY_NAME, fileName);
                contentValues.put(MediaStore.Downloads.MIME_TYPE, "text/csv");
                contentValues.put(MediaStore.Downloads.RELATIVE_PATH, Environment.DIRECTORY_DOWNLOADS);

                ContentResolver resolver = requireContext().getContentResolver();
                fileUri = resolver.insert(MediaStore.Downloads.EXTERNAL_CONTENT_URI, contentValues);

                if (fileUri == null) {
                    Log.e(TAG, "Failed to create file in Downloads");
                    Toast.makeText(getContext(), "Failed to create file in Downloads", Toast.LENGTH_SHORT).show();
                    return;
                }

                Log.d(TAG, "File URI in Downloads: " + fileUri.toString());

                // Write to the file using OutputStream
                try (OutputStream outputStream = resolver.openOutputStream(fileUri);
                     OutputStreamWriter writer = new OutputStreamWriter(outputStream);
                     BufferedWriter bufferedWriter = new BufferedWriter(writer)) {

                    // Write CSV header with adjusted order to match Excel's expectations
                    bufferedWriter.write("Name,Section,Status,Time In,Time Out\n");
                    Log.d(TAG, "CSV header written");

                    // Write data for each participant
                    int writtenRows = 0;
                    for (Participant participant : dataToExport) {
                        StringBuilder row = new StringBuilder();

                        // Process name - escape quotes and enclose in quotes
                        String name = participant.getName() != null ? participant.getName() : "";
                        // Replace any double quotes with two double quotes (Excel standard for escaping quotes)
                        name = name.replace("\"", "\"\"");
                        row.append("\"").append(name).append("\"");
                        row.append(",");

                        // Process section - escape quotes and enclose in quotes
                        String section = participant.getSection() != null ? participant.getSection() : "";
                        section = section.replace("\"", "\"\"");
                        row.append("\"").append(section).append("\"");
                        row.append(",");

                        // Status - escape quotes and enclose in quotes
                        String status = participant.getStatus() != null ? participant.getStatus() : "";
                        status = status.replace("\"", "\"\"");
                        row.append("\"").append(status).append("\"");
                        row.append(",");

                        // Time In - escape quotes and enclose in quotes
                        String timeIn = participant.getTimeIn() != null ? participant.getTimeIn() : "";
                        timeIn = timeIn.replace("\"", "\"\"");
                        row.append("\"").append(timeIn).append("\"");
                        row.append(",");

                        // Time Out - escape quotes and enclose in quotes
                        String timeOut = participant.getTimeOut() != null ? participant.getTimeOut() : "";
                        timeOut = timeOut.replace("\"", "\"\"");
                        row.append("\"").append(timeOut).append("\"");

                        row.append("\n");
                        bufferedWriter.write(row.toString());
                        writtenRows++;
                    }

                    Log.d(TAG, "CSV file successfully written with " + writtenRows + " rows of data");
                }

                // Display path that user can easily access
                String userFriendlyPath = Environment.DIRECTORY_DOWNLOADS + "/" + fileName;
                showExportSuccessDialog(userFriendlyPath, fileUri);

            } else {
                // For older versions, use the old method
                File exportDir = Environment.getExternalStoragePublicDirectory(Environment.DIRECTORY_DOWNLOADS);
                Log.d(TAG, "Export directory (pre-Android 10): " + exportDir.getAbsolutePath());

                if (!exportDir.exists()) {
                    boolean created = exportDir.mkdirs();
                    Log.d(TAG, "Created export directory: " + created);
                }

                File file = new File(exportDir, fileName);
                Log.d(TAG, "Full export file path: " + file.getAbsolutePath());

                // Create the file
                boolean fileCreated = file.createNewFile();
                Log.d(TAG, "File created: " + fileCreated);

                FileWriter writer = new FileWriter(file);

                // Write CSV header with consistent order
                writer.append("Name,Section,Status,Time In,Time Out\n");
                Log.d(TAG, "CSV header written");

                // Write data for each participant
                int writtenRows = 0;
                for (Participant participant : dataToExport) {
                    StringBuilder row = new StringBuilder();

                    // Process name - escape quotes and enclose in quotes
                    String name = participant.getName() != null ? participant.getName() : "";
                    // Replace any double quotes with two double quotes (Excel standard for escaping quotes)
                    name = name.replace("\"", "\"\"");
                    row.append("\"").append(name).append("\"");
                    row.append(",");

                    // Process section - escape quotes and enclose in quotes
                    String section = participant.getSection() != null ? participant.getSection() : "";
                    section = section.replace("\"", "\"\"");
                    row.append("\"").append(section).append("\"");
                    row.append(",");

                    // Status - escape quotes and enclose in quotes
                    String status = participant.getStatus() != null ? participant.getStatus() : "";
                    status = status.replace("\"", "\"\"");
                    row.append("\"").append(status).append("\"");
                    row.append(",");

                    // Time In - escape quotes and enclose in quotes
                    String timeIn = participant.getTimeIn() != null ? participant.getTimeIn() : "";
                    timeIn = timeIn.replace("\"", "\"\"");
                    row.append("\"").append(timeIn).append("\"");
                    row.append(",");

                    // Time Out - escape quotes and enclose in quotes
                    String timeOut = participant.getTimeOut() != null ? participant.getTimeOut() : "";
                    timeOut = timeOut.replace("\"", "\"\"");
                    row.append("\"").append(timeOut).append("\"");

                    row.append("\n");
                    writer.append(row.toString());
                    writtenRows++;
                }

                writer.flush();
                writer.close();
                Log.d(TAG, "CSV file successfully written with " + writtenRows + " rows of data");

                // Create file URI for sharing
                fileUri = FileProvider.getUriForFile(
                        requireContext(),
                        requireContext().getApplicationContext().getPackageName() + ".provider",
                        file
                );

                showExportSuccessDialog(file.getAbsolutePath(), fileUri);
            }

        } catch (IOException e) {
            Log.e(TAG, "Error exporting data: " + e.getMessage(), e);
            Toast.makeText(getContext(), "Error exporting data: " + e.getMessage(), Toast.LENGTH_SHORT).show();
        }
    }

    private void showExportSuccessDialog(String filePath, Uri fileUri) {
        AlertDialog.Builder builder = new AlertDialog.Builder(requireContext());
        builder.setTitle("Export Successful")
                .setMessage("File saved to: " + filePath + "\n\nYou can find it in your Downloads folder.")
                .setPositiveButton("Share", (dialog, which) -> shareCSVFile(fileUri))
                .setNegativeButton("OK", null)
                .show();
        Log.d(TAG, "Export success dialog shown");
    }

    private void shareCSVFile(Uri fileUri) {
        try {
            Intent shareIntent = new Intent(Intent.ACTION_SEND);
            shareIntent.setType("text/csv");
            shareIntent.putExtra(Intent.EXTRA_SUBJECT, eventName + " Participants Data");
            shareIntent.putExtra(Intent.EXTRA_STREAM, fileUri);
            shareIntent.addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION);

            Log.d(TAG, "Starting share intent");
            startActivity(Intent.createChooser(shareIntent, "Share via"));
        } catch (Exception e) {
            Log.e(TAG, "Error sharing file: " + e.getMessage(), e);
            Toast.makeText(getContext(), "Error sharing file: " + e.getMessage(), Toast.LENGTH_SHORT).show();
        }
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