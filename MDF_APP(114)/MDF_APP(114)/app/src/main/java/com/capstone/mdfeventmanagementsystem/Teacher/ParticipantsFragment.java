package com.capstone.mdfeventmanagementsystem.Teacher;

import android.Manifest;
import android.content.ContentResolver;
import android.content.ContentValues;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.database.Cursor;
import android.net.Uri;
import android.os.Build;
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
import android.widget.HorizontalScrollView;
import android.widget.ImageButton;
import android.widget.LinearLayout;
import android.widget.RadioGroup;
import android.widget.TextView;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.appcompat.app.AlertDialog;
import androidx.core.app.ActivityCompat;
import androidx.core.content.ContextCompat;
import androidx.core.content.FileProvider;
import androidx.fragment.app.Fragment;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.capstone.mdfeventmanagementsystem.R;
import com.capstone.mdfeventmanagementsystem.Adapters.ParticipantsAdapter;
import com.capstone.mdfeventmanagementsystem.Adapters.Participant;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseUser;
import com.google.firebase.database.DataSnapshot;
import com.google.firebase.database.DatabaseError;
import com.google.firebase.database.DatabaseReference;
import com.google.firebase.database.FirebaseDatabase;
import com.google.firebase.database.ValueEventListener;

import org.apache.poi.ss.usermodel.*;
import org.apache.poi.ss.util.CellRangeAddress;
import org.apache.poi.xssf.usermodel.XSSFWorkbook;

import java.io.BufferedWriter;
import java.io.File;
import java.io.FileOutputStream;
import java.io.FileWriter;
import java.io.IOException;
import java.io.OutputStream;
import java.io.OutputStreamWriter;
import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Calendar;
import java.util.Collections;
import java.util.Comparator;
import java.util.Date;
import java.util.HashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.TimeZone;
import java.util.concurrent.atomic.AtomicInteger;

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

    // Multi-day event variables
    private HorizontalScrollView dayTabsScroll;
    private LinearLayout dayTabsContainer;
    private TextView eventDateText;
    private int selectedDayIndex = 0; // Default to day 1 (index 0)
    private List<Date> eventDates = new ArrayList<>();
    private SimpleDateFormat dateFormat = new SimpleDateFormat("yyyy-MM-dd", Locale.getDefault());
    private SimpleDateFormat displayDateFormat = new SimpleDateFormat("MMM d, yyyy", Locale.getDefault());

    // Handler for automatic status updates
    private Handler statusUpdateHandler = new Handler();
    private Runnable statusUpdateRunnable;

    private ValueEventListener participantsListener;
    private DatabaseReference studentsRef;

    private DatabaseReference eventsRef; // Added reference for events
    private static final long STATUS_UPDATE_INTERVAL = 5 * 60 * 1000; // 5 minutes
    private AttendanceData p;

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

            // Initialize the Firebase references
            FirebaseDatabase database = FirebaseDatabase.getInstance();
            eventsRef = database.getReference("events").child(eventId);

            // Fetch event details including the event name
            fetchEventDetails();
        } else {
            Log.e(TAG, "No event ID provided to fragment");
        }
    }

    private void fetchEventDetails() {
        eventsRef.addListenerForSingleValueEvent(new ValueEventListener() {
            @Override
            public void onDataChange(@NonNull DataSnapshot dataSnapshot) {
                if (dataSnapshot.exists()) {
                    // Get event name
                    if (dataSnapshot.child("eventName").exists()) {
                        eventName = dataSnapshot.child("eventName").getValue(String.class);
                        Log.d(TAG, "Event name fetched: " + eventName);
                    } else {
                        Log.w(TAG, "eventName field not found, using default value");
                    }

                    // Get other event details if needed
                    if (dataSnapshot.child("startTime").exists()) {
                        eventStartTime = dataSnapshot.child("startTime").getValue(String.class);
                    }
                    if (dataSnapshot.child("endTime").exists()) {
                        eventEndTime = dataSnapshot.child("endTime").getValue(String.class);
                    }
                    if (dataSnapshot.child("startDate").exists()) {
                        eventStartDate = dataSnapshot.child("startDate").getValue(String.class);
                    }
                    if (dataSnapshot.child("endDate").exists()) {
                        eventEndDate = dataSnapshot.child("endDate").getValue(String.class);
                    }
                    if (dataSnapshot.child("graceTime").exists()) {
                        eventGraceTime = dataSnapshot.child("graceTime").getValue(String.class);
                    }

                    // Check if this is a multi-day event
                    if (eventStartDate != null && eventEndDate != null && !eventStartDate.equals(eventEndDate)) {
                        isMultiDayEvent = true;
                        setupMultiDayEvent();
                    }

                    // Update UI if needed
                    if (getActivity() != null) {
                        getActivity().runOnUiThread(() -> {
                            // You could update any UI elements that display the event name here
                            setupDayTabsVisibility();
                        });
                    }
                } else {
                    Log.w(TAG, "Event data not found");
                }
            }

            @Override
            public void onCancelled(@NonNull DatabaseError databaseError) {
                Log.e(TAG, "Error fetching event details: " + databaseError.getMessage());
            }
        });
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
            updatePendingStatusesForAllDays(); // Add this line to check past days
        }

        // Initialize multi-day event handling when returning to the fragment
        if (isMultiDayEvent) {
            initMultiDayEventHandling();
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

        // Initialize multi-day event views
        dayTabsScroll = view.findViewById(R.id.day_tabs_scroll);
        dayTabsContainer = view.findViewById(R.id.day_tabs_container);
        eventDateText = view.findViewById(R.id.event_date_text);
    }

    /**
     * Sets up the day tabs visibility based on multi-day event status
     */
    private void setupDayTabsVisibility() {
        if (isMultiDayEvent) {
            dayTabsScroll.setVisibility(View.VISIBLE);
            eventDateText.setVisibility(View.VISIBLE);
        } else {
            dayTabsScroll.setVisibility(View.GONE);
            eventDateText.setVisibility(View.GONE);
        }
    }

    /**
     * Setup multi-day event by calculating all dates between start and end dates
     */
    private void setupMultiDayEvent() {
        try {
            Date startDate = dateFormat.parse(eventStartDate);
            Date endDate = dateFormat.parse(eventEndDate);

            if (startDate != null && endDate != null) {
                eventDates.clear();

                // Add all dates between start and end dates (inclusive)
                Calendar calendar = Calendar.getInstance();
                calendar.setTime(startDate);

                while (!calendar.getTime().after(endDate)) {
                    eventDates.add(calendar.getTime());
                    calendar.add(Calendar.DAY_OF_MONTH, 1);
                }

                // Find which day of the event is today (if within event dates)
                Date today = new Date();
                String todayStr = dateFormat.format(today);

                boolean foundTodayInEventDates = false;
                for (int i = 0; i < eventDates.size(); i++) {
                    String dateStr = dateFormat.format(eventDates.get(i));
                    if (dateStr.equals(todayStr)) {
                        selectedDayIndex = i;
                        foundTodayInEventDates = true;
                        Log.d(TAG, "Found today in event dates at index " + i);
                        break;
                    }
                }

                // If today is not within event dates, select first day by default
                if (!foundTodayInEventDates) {
                    Log.d(TAG, "Today is not within event dates, selecting first day by default");
                    selectedDayIndex = 0;
                }

                Log.d(TAG, "Set up multi-day event with " + eventDates.size() + " days, selected day index: " + selectedDayIndex);

                if (getActivity() != null) {
                    getActivity().runOnUiThread(this::createDayTabs);
                }
            }
        } catch (ParseException e) {
            Log.e(TAG, "Error parsing event dates", e);
        }
    }

    /**
     * Create day tabs for multi-day events
     */
    /**
     * Create day tabs for multi-day events
     */
    private void createDayTabs() {
        dayTabsContainer.removeAllViews();

        for (int i = 0; i < eventDates.size(); i++) {
            final int dayIndex = i;
            Date date = eventDates.get(i);
            View dayTab = LayoutInflater.from(getContext()).inflate(R.layout.layout_day_tab, dayTabsContainer, false);

            TextView dayNumberText = dayTab.findViewById(R.id.day_number_text);
            TextView dayDateText = dayTab.findViewById(R.id.day_date_text);

            dayNumberText.setText("Day " + (i + 1));
            dayDateText.setText(displayDateFormat.format(date));

            // Set selected state for current day
            dayTab.setSelected(i == selectedDayIndex);

            // Add click listener
            dayTab.setOnClickListener(v -> {
                // Update selected state
                for (int j = 0; j < dayTabsContainer.getChildCount(); j++) {
                    dayTabsContainer.getChildAt(j).setSelected(j == dayIndex);
                }

                // Update selected day index
                selectedDayIndex = dayIndex;
                updateEventDateDisplay();

                // Log that we're changing day
                Log.d(TAG, "Changing to day " + (selectedDayIndex + 1));

                // Reload participants for selected day with fresh data
                if (isFilteringBySection) {
                    loadParticipantsForTeacherSection();
                } else {
                    loadParticipants();
                }
            });

            dayTabsContainer.addView(dayTab);
        }

        updateEventDateDisplay();
    }

    /**
     * Update the event date display based on selected day
     */
    private void updateEventDateDisplay() {
        if (eventDates.size() > selectedDayIndex) {
            Date selectedDate = eventDates.get(selectedDayIndex);
            eventDateText.setText("Selected Date: " + displayDateFormat.format(selectedDate));
        }
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

    /**
     * This method will initialize the handling of multi-day events
     */
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

                // Setup day tabs if they're not already set up
                if (eventDates.isEmpty()) {
                    setupMultiDayEvent();
                }
            } else {
                Log.d(TAG, "Current date is outside the multi-day event range.");
            }
        } catch (ParseException e) {
            Log.e(TAG, "Error initializing multi-day event handling", e);
        }
    }

    private void sortParticipantsByName() {
        if (participantList != null && !participantList.isEmpty()) {
            Log.d(TAG, "Sorting participants by name");
            Collections.sort(participantList, new Comparator<Participant>() {
                @Override
                public int compare(Participant p1, Participant p2) {
                    // Handle null names gracefully
                    String name1 = p1.getName() != null ? p1.getName() : "";
                    String name2 = p2.getName() != null ? p2.getName() : "";
                    return name1.compareToIgnoreCase(name2); // Case-insensitive sorting
                }
            });
        }
    }

    /**
     * Get the current date in yyyy-MM-dd format
     */

    /**
     * Find the day key for the current date
     * Returns "day1", "day2", etc. based on event start date
     */

    // Add this method to ParticipantsFragment class
    private void loadParticipantsForTeacherSection() {
        if (eventId == null || eventId.isEmpty()) {
            Log.e(TAG, "Event ID is null or empty");
            noParticipantsText.setVisibility(View.VISIBLE);
            recyclerView.setVisibility(View.GONE);
            return;
        }

        Log.d(TAG, "Loading participants for event: " + eventId + " filtered by teacher's section for day " + (selectedDayIndex + 1));

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
                    sortParticipantsByName();

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

                    // Alternative check for multi-day events if eventSpan is not available
                    if (eventStartDate != null && eventEndDate != null && !eventStartDate.equals(eventEndDate)) {
                        isMultiDayEvent = true;
                    }

                    Log.d(TAG, "Event details loaded - Start: " + eventStartDate + " " + eventStartTime +
                            ", End: " + (eventEndDate != null ? eventEndDate : eventStartDate) + " " + eventEndTime +
                            ", Grace: " + eventGraceTime + ", Multi-day: " + isMultiDayEvent);

                    // Initialize multi-day event handling if applicable
                    if (isMultiDayEvent) {
                        setupMultiDayEvent();
                        setupDayTabsVisibility();
                    }

                    // Now load participants after getting event details
                    // By default, load participants filtered by teacher's section
                    loadParticipantsForTeacherSection();
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

        Log.d(TAG, "Loading participants for event: " + eventId + " for day " + (selectedDayIndex + 1));

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
                    sortParticipantsByName();

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

    // Update the processAttendanceData method to handle day-specific attendance data
    private void processAttendanceData(Participant participant, DataSnapshot ticketSnapshot) {
        DataSnapshot attendanceDaysSnapshot = ticketSnapshot.child("attendanceDays");

        if (!attendanceDaysSnapshot.exists()) {
            // No attendance data exists yet - this means ticket is registered but no check-in
            participant.setTimeIn("");
            participant.setTimeOut("");
            participant.setStatus("Pending");
            return;
        }

        if (isMultiDayEvent) {
            // For multi-day events, use the currently selected day from the tab
            String dayKey = "day_" + (selectedDayIndex + 1);
            Log.d(TAG, "Processing attendance for day: " + dayKey + " (index: " + selectedDayIndex + ")");

            // Get attendance data for the selected day
            DataSnapshot selectedDaySnapshot = attendanceDaysSnapshot.child(dayKey);

            if (selectedDaySnapshot.exists()) {
                // There is data for this day
                extractAttendanceData(participant, selectedDaySnapshot);
            } else {
                // No data for this day - set to pending
                participant.setTimeIn("");
                participant.setTimeOut("");
                participant.setStatus("Pending");
                Log.d(TAG, "No attendance data for " + participant.getName() + " on " + dayKey);
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

    // 1. First, add this helper method to convert 24-hour time to 12-hour format with AM/PM
    private String formatTo12Hour(String time24) {
        if (time24 == null || time24.isEmpty() || "N/A".equals(time24)) {
            return "";
        }

        try {
            // Parse the 24-hour time
            SimpleDateFormat format24 = new SimpleDateFormat("HH:mm", Locale.getDefault());
            Date dateTime = format24.parse(time24);

            // Format to 12-hour time with AM/PM
            SimpleDateFormat format12 = new SimpleDateFormat("hh:mm a", Locale.getDefault());
            return format12.format(dateTime);
        } catch (ParseException e) {
            Log.e(TAG, "Error converting time format: " + e.getMessage());
            return time24; // Return original if there's an error
        }
    }

    // 2. Modify the extractAttendanceData method to format times
    private void extractAttendanceData(Participant participant, DataSnapshot daySnapshot) {
        // Get check-in time
        String timeIn = "";
        if (daySnapshot.child("in").exists()) {
            timeIn = daySnapshot.child("in").getValue(String.class);
        }

        // Store the original 24-hour time format in Participant object (for backend processing)
        participant.setTimeIn24(timeIn == null || "N/A".equals(timeIn) ? "" : timeIn);

        // Set the formatted 12-hour time for display
        participant.setTimeIn(formatTo12Hour(timeIn));

        // Get check-out time
        String timeOut = "";
        if (daySnapshot.child("out").exists()) {
            timeOut = daySnapshot.child("out").getValue(String.class);
        }

        // Store the original 24-hour time format in Participant object (for backend processing)
        participant.setTimeOut24(timeOut == null || "N/A".equals(timeOut) ? "" : timeOut);

        // Set the formatted 12-hour time for display
        participant.setTimeOut(formatTo12Hour(timeOut));

        // Rest of the method remains the same...

        // Get attendance status from database if it exists
        String status = "Pending"; // Default status

        if (daySnapshot.child("attendance").exists()) {
            status = daySnapshot.child("attendance").getValue(String.class);
        } else if (daySnapshot.child("status").exists()) {
            status = daySnapshot.child("status").getValue(String.class);
        } else {
            // If no status found, determine it based on check-in/out times
            if (!participant.getTimeIn24().isEmpty() && participant.getTimeOut24().isEmpty()) {
                status = "Ongoing";
            } else if (!participant.getTimeIn24().isEmpty() && !participant.getTimeOut24().isEmpty()) {
                // Check if the student was late
                boolean isLate = false;
                if (daySnapshot.child("isLate").exists()) {
                    isLate = daySnapshot.child("isLate").getValue(Boolean.class);
                } else {
                    isLate = wasCheckInLate(participant.getTimeIn24());
                }
                status = isLate ? "Late" : "Present";
            } else if (participant.getTimeIn24().isEmpty() && participant.getTimeOut24().isEmpty()) {
                status = "Pending";
            }
        }

        participant.setStatus(status);
    }

    // 3. Modify the wasCheckInLate method to use the 24-hour time
    private boolean wasCheckInLate(String checkInTime) {
        if (checkInTime == null || checkInTime.isEmpty() || eventStartTime == null || eventGraceTime == null) {
            return false;
        }

        try {
            // Parse times - make sure we're using the 24-hour format time for comparison
            SimpleDateFormat timeFormat = new SimpleDateFormat("HH:mm", Locale.getDefault());
            Date startTimeDate = timeFormat.parse(eventStartTime);

            // If the check-in time is already in 12-hour format, we need to convert it back
            Date checkInTimeDate;
            if (checkInTime.contains("AM") || checkInTime.contains("PM")) {
                SimpleDateFormat format12 = new SimpleDateFormat("hh:mm a", Locale.getDefault());
                checkInTimeDate = format12.parse(checkInTime);
            } else {
                checkInTimeDate = timeFormat.parse(checkInTime);
            }

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

            // For multi-day events, check if the selected day has ended, not just the current date
            boolean selectedDayHasEnded = false;

            if (isMultiDayEvent && eventDates.size() > selectedDayIndex) {
                // Get the date of the selected day
                Date selectedDate = eventDates.get(selectedDayIndex);
                String selectedDateStr = new SimpleDateFormat("yyyy-MM-dd", Locale.getDefault()).format(selectedDate);

                // Check if current date is after the selected day
                if (currentDate.compareTo(selectedDateStr) > 0) {
                    selectedDayHasEnded = true;
                }
                // If it's the same day as selected, check if current time is after event end time
                else if (currentDate.equals(selectedDateStr) && currentTimeDate.after(endTimeDate)) {
                    selectedDayHasEnded = true;
                }
            } else {
                // For single-day event, use normal isEventEnded logic
                selectedDayHasEnded = isEventEnded(currentDate);
            }

            boolean pastCutoffTime = currentTimeDate.after(cutoffTimeDate);
            boolean pastThirtyMinCutoff = currentTimeDate.after(thirtyMinCutoffTime);

            // Debug logs
            Log.d(TAG, "Current date: " + currentDate + ", Current time: " + currentTime);
            Log.d(TAG, "Selected day index: " + selectedDayIndex);
            Log.d(TAG, "Event end time: " + eventEndTime);
            Log.d(TAG, "Is multi-day event: " + isMultiDayEvent);
            Log.d(TAG, "Selected day has ended: " + selectedDayHasEnded + ", Past cutoff time: " + pastCutoffTime);
            Log.d(TAG, "Past 30-min cutoff: " + pastThirtyMinCutoff);

            // If event has ended + 1 hour, update statuses
            if (selectedDayHasEnded && pastCutoffTime) {
                Log.d(TAG, "Selected day has ended and 1-hour grace period has passed. Updating attendance statuses.");
                updateStatusesAfterEvent();
            } else if (selectedDayHasEnded && pastThirtyMinCutoff) {
                Log.d(TAG, "Selected day has ended and 30-min grace period has passed. Updating incomplete check-outs.");
                updateIncompleteCheckouts();
            } else if (selectedDayHasEnded) {
                Log.d(TAG, "Selected day has ended but waiting for grace periods to update statuses.");
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

            // Check if status is "Pending" (no check-in, no check-out)
            if ((currentStatus.equalsIgnoreCase("Pending") || currentStatus.equalsIgnoreCase("pending"))
                    && timeIn.isEmpty() && timeOut.isEmpty()) {

                // Mark status as "absent" for display
                participant.setStatus("Absent");

                // Update in Firebase with lowercase "absent"
                updatePendingToAbsentInFirebase(participant.getId(), participant.getTicketRef());

                Log.d(TAG, "Changed status from pending to absent for student " +
                        participant.getId() + " after event end + 1 hour");
            }
            // ADDING THIS NEW CONDITION: Check if status is "Ongoing" (has check-in but no check-out)
            else if ((currentStatus.equalsIgnoreCase("Ongoing") || currentStatus.equalsIgnoreCase("ongoing"))
                    && !timeIn.isEmpty() && timeOut.isEmpty()) {

                // Mark status as "absent" for display
                participant.setStatus("Absent");

                // Update in Firebase
                updateIncompleteCheckoutToAbsentInFirebase(participant.getId(), participant.getTicketRef());

                Log.d(TAG, "Changed status from ongoing to absent for student " +
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

        // Use the selected day index for multi-day events
        String dayKey = "day_" + (selectedDayIndex + 1);

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

                    // If we have the date for this day, include it
                    if (eventDates.size() > selectedDayIndex) {
                        Date selectedDate = eventDates.get(selectedDayIndex);
                        String dateStr = new SimpleDateFormat("yyyy-MM-dd", Locale.getDefault()).format(selectedDate);
                        dayData.put("date", dateStr);
                    } else {
                        dayData.put("date", getCurrentDate());
                    }

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

        // Use the selected day index for multi-day events
        String dayKey = "day_" + (selectedDayIndex + 1);

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
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
            // For Android 10+, we don't need WRITE_EXTERNAL_STORAGE for app-specific directories
            exportToExcel();
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
                exportToExcel();
            }
        }
    }

    // Add this field to your class to track export status
    private boolean hasExportedSuccessfully = false;
    private String lastExportedFileName = null;


    private void exportToExcel() {
        try {
            List<Participant> dataToExport = adapter.getFilteredList();
            if (dataToExport.isEmpty()) {
                Log.d(TAG, "No data to export");
                Toast.makeText(getContext(), "No data to export", Toast.LENGTH_SHORT).show();
                return;
            }

            // Use event name for filename (sanitized)
            String sanitizedEventName = eventName.replaceAll("[^a-zA-Z0-9]", "_");
            String baseFileName = sanitizedEventName + "_Attendance.xlsx";

            // If already exported successfully, check if file exists directly
            if (hasExportedSuccessfully) {
                Log.d(TAG, "Already exported successfully before, checking if file exists");
                checkFileExistsAndProceed(baseFileName, dataToExport);
            } else {
                Log.d(TAG, "Showing export confirmation dialog");
                // Show confirmation dialog first
                showExportConfirmationDialog(dataToExport);
            }

        } catch (Exception e) {
            Log.e(TAG, "Error starting export process: " + e.getMessage(), e);
            Toast.makeText(getContext(), "Error exporting data: " + e.getMessage(), Toast.LENGTH_SHORT).show();
        }
    }

    private void showExportConfirmationDialog(List<Participant> dataToExport) {
        AlertDialog.Builder builder = new AlertDialog.Builder(requireContext());
        builder.setTitle("Export Attendance")
                .setMessage("Are you sure you want to export attendance data?")
                .setPositiveButton("Yes", (dialog, which) -> {
                    // User confirmed, proceed with export
                    Log.d(TAG, "User confirmed export, proceeding with " + dataToExport.size() + " participants");

                    // Use event name for filename (sanitized)
                    String sanitizedEventName = eventName.replaceAll("[^a-zA-Z0-9]", "_");
                    String baseFileName = sanitizedEventName + "_Attendance.xlsx";
                    Log.d(TAG, "Base export filename: " + baseFileName);

                    // Check if the file already exists and handle accordingly
                    checkFileExistsAndProceed(baseFileName, dataToExport);
                })
                .setNegativeButton("Cancel", (dialog, which) -> {
                    // User cancelled the export
                    Log.d(TAG, "Export cancelled by user");
                    dialog.dismiss();
                })
                .show();
    }

    private void checkFileExistsAndProceed(String baseFileName, List<Participant> dataToExport) {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
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
                writeExcelFile(baseFileName, dataToExport);
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
            writeExcelFile(baseFileName, dataToExport);
        }
    }

    private void showFileExistsDialog(String baseFileName, List<Participant> dataToExport) {
        AlertDialog.Builder builder = new AlertDialog.Builder(requireContext());
        builder.setTitle("File Already Exists")
                .setMessage("A file named '" + baseFileName + "' already exists. Do you want to download it again?")
                .setPositiveButton("Yes", (dialog, which) -> {
                    // Get a new filename with incremented counter
                    String newFileName = getIncrementedFileName(baseFileName);
                    writeExcelFile(newFileName, dataToExport);
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

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
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

    // Reset export status if there's an error
    private void resetExportStatus() {
        hasExportedSuccessfully = false;
        lastExportedFileName = null;
        Log.d(TAG, "Export status reset due to error");
    }
    // Fixed getAttendanceDataForDay method
    private void writeExcelFile(String fileName, List<Participant> participants) {
        try {
            Log.d(TAG, "Writing to Excel file: " + fileName);
            Uri fileUri;
            OutputStream outputStream;

            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
                // For Android 10+, use MediaStore
                ContentValues contentValues = new ContentValues();
                contentValues.put(MediaStore.Downloads.DISPLAY_NAME, fileName);
                contentValues.put(MediaStore.Downloads.MIME_TYPE, "application/vnd.openxmlformats-officedocument.spreadsheetml.sheet");
                contentValues.put(MediaStore.Downloads.RELATIVE_PATH, Environment.DIRECTORY_DOWNLOADS);

                ContentResolver resolver = requireContext().getContentResolver();
                fileUri = resolver.insert(MediaStore.Downloads.EXTERNAL_CONTENT_URI, contentValues);

                if (fileUri == null) {
                    Log.e(TAG, "Failed to create file in Downloads");
                    Toast.makeText(getContext(), "Failed to create file in Downloads", Toast.LENGTH_SHORT).show();
                    resetExportStatus();
                    return;
                }

                outputStream = resolver.openOutputStream(fileUri);
            } else {
                // For older versions, use the old method
                File exportDir = Environment.getExternalStoragePublicDirectory(Environment.DIRECTORY_DOWNLOADS);
                if (!exportDir.exists()) {
                    boolean created = exportDir.mkdirs();
                    Log.d(TAG, "Created export directory: " + created);
                }

                File file = new File(exportDir, fileName);
                outputStream = new FileOutputStream(file);
                fileUri = FileProvider.getUriForFile(
                        requireContext(),
                        requireContext().getApplicationContext().getPackageName() + ".provider",
                        file
                );
            }

            if (outputStream == null) {
                Log.e(TAG, "Failed to create output stream");
                Toast.makeText(getContext(), "Failed to create output file", Toast.LENGTH_SHORT).show();
                resetExportStatus();
                return;
            }

            // Start by calculating the number of days
            int numberOfDays = calculateNumberOfDays(eventStartDate, eventEndDate);

            // Now fetch all attendance data for all participants for all days
            fetchAllAttendanceData(participants, numberOfDays, new OnAllDataFetchCompleteListener() {
                @Override
                public void onComplete(List<ParticipantFullData> completeData) {
                    try {
                        // Create Excel workbook with the complete data
                        createExcelWorkbook(outputStream, fileUri, fileName, completeData, numberOfDays);
                    } catch (Exception e) {
                        Log.e(TAG, "Error creating Excel workbook: " + e.getMessage(), e);
                        Toast.makeText(getContext(), "Error creating Excel file: " + e.getMessage(), Toast.LENGTH_SHORT).show();
                        resetExportStatus();
                    }
                }

                @Override
                public void onError(Exception e) {
                    Log.e(TAG, "Error fetching attendance data: " + e.getMessage(), e);
                    Toast.makeText(getContext(), "Error fetching attendance data: " + e.getMessage(), Toast.LENGTH_SHORT).show();
                    resetExportStatus();
                }
            });
        } catch (Exception e) {
            Log.e(TAG, "Unexpected error exporting data: " + e.getMessage(), e);
            Toast.makeText(getContext(), "Error exporting data: " + e.getMessage(), Toast.LENGTH_SHORT).show();
            resetExportStatus(); // Reset the export status flag on error
        }
    }


    // Method to create the Excel workbook with all data
    private void createExcelWorkbook(OutputStream outputStream, Uri fileUri, String fileName,
                                     List<ParticipantFullData> completeData, int numberOfDays) throws IOException {
        // Create Excel workbook
        Workbook workbook = new XSSFWorkbook();
        Sheet sheet = workbook.createSheet("Attendance");

        // Create cell styles
        CellStyle headerStyle = workbook.createCellStyle();
        Font headerFont = workbook.createFont();
        headerFont.setBold(true);
        headerStyle.setFont(headerFont);
        headerStyle.setAlignment(HorizontalAlignment.CENTER);

        // Create title style with larger font
        CellStyle titleStyle = workbook.createCellStyle();
        Font titleFont = workbook.createFont();
        titleFont.setBold(true);
        titleFont.setFontHeightInPoints((short) 14); // Larger font for event name
        titleStyle.setFont(titleFont);
        titleStyle.setAlignment(HorizontalAlignment.CENTER);

        // Add vertical alignment for title style to center text vertically
        titleStyle.setVerticalAlignment(VerticalAlignment.CENTER);

        // Row 0: Event Name (merged across all columns)
        Row eventNameRow = sheet.createRow(0);
        // Set higher row height for better visibility of the event name
        eventNameRow.setHeight((short) 600); // Increased row height (in twips) - about 30 pixels

        Cell eventNameCell = eventNameRow.createCell(0);
        eventNameCell.setCellValue(eventName);
        eventNameCell.setCellStyle(titleStyle);

        // Calculate total columns based on event days (each day needs 3 columns: Status, Time In, Time Out)
        int totalColumns = 2 + (numberOfDays * 3); // Name + Section + (Status, Time In, Time Out) * days

        // Merge cells for Event Name heading (all columns)
        sheet.addMergedRegion(new CellRangeAddress(0, 0, 0, totalColumns - 1));

        // Add empty row for spacing after event name
        Row spacerRow = sheet.createRow(1);
        spacerRow.setHeight((short) 300); // About 15 pixels of empty space

        // Headers now start from row 2 (was row 1)
        Row headersRow = sheet.createRow(2);
        Cell nameCell = headersRow.createCell(0);
        nameCell.setCellValue("Name");
        nameCell.setCellStyle(headerStyle);

        Cell sectionCell = headersRow.createCell(1);
        sectionCell.setCellValue("Section");
        sectionCell.setCellStyle(headerStyle);

        // Generate date headers for each day of the event
        List<String> formattedDates = getFormattedEventDates(eventStartDate, eventEndDate);
        for (int i = 0; i < formattedDates.size(); i++) {
            // Create date header and merge it across 3 columns (for Status, Time In, Time Out)
            int startCol = 2 + (i * 3);
            Cell dateHeaderCell = headersRow.createCell(startCol);
            dateHeaderCell.setCellValue(formattedDates.get(i));
            dateHeaderCell.setCellStyle(headerStyle);

            // Merge cells for date heading (columns for this day)
            sheet.addMergedRegion(new CellRangeAddress(2, 2, startCol, startCol + 2));
        }

        // Row 3: Status, Time In, Time Out headers for each day
        Row subHeadersRow = sheet.createRow(3);
        for (int i = 0; i < numberOfDays; i++) {
            int startCol = 2 + (i * 3);

            Cell statusHeader = subHeadersRow.createCell(startCol);
            statusHeader.setCellValue("Status");
            statusHeader.setCellStyle(headerStyle);

            Cell timeInHeader = subHeadersRow.createCell(startCol + 1);
            timeInHeader.setCellValue("Time In");
            timeInHeader.setCellStyle(headerStyle);

            Cell timeOutHeader = subHeadersRow.createCell(startCol + 2);
            timeOutHeader.setCellValue("Time Out");
            timeOutHeader.setCellStyle(headerStyle);
        }

        // Set column widths manually
        sheet.setColumnWidth(0, 20 * 256); // Name column
        sheet.setColumnWidth(1, 15 * 256); // Section column

        // Set widths for all date columns
        for (int i = 0; i < numberOfDays * 3; i++) {
            sheet.setColumnWidth(2 + i, 15 * 256);
        }

        // Create center-aligned cell style for data
        CellStyle centerAlignedStyle = workbook.createCellStyle();
        centerAlignedStyle.setAlignment(HorizontalAlignment.CENTER);

        // Fill participant data starting from row 4
        Log.d(TAG, "Adding " + completeData.size() + " participants to Excel");
        int rowIndex = 4;
        for (ParticipantFullData pData : completeData) {
            Participant p = pData.getBasicInfo();
            Row row = sheet.createRow(rowIndex++);

            // Common participant info
            Cell nameValueCell = row.createCell(0);
            nameValueCell.setCellValue(p.getName() != null ? p.getName() : "");

            Cell sectionValueCell = row.createCell(1);
            sectionValueCell.setCellValue(p.getSection() != null ? p.getSection() : "");
            sectionValueCell.setCellStyle(centerAlignedStyle);  // Apply center alignment

            // For each day, fetch the appropriate attendance data from our complete dataset
            for (int day = 0; day < numberOfDays; day++) {
                int startCol = 2 + (day * 3);

                // Get attendance data for this day from our pre-fetched data
                AttendanceData attendanceData = pData.getAttendanceForDay(day + 1);

                Cell statusValueCell = row.createCell(startCol);
                statusValueCell.setCellValue(attendanceData.getStatus() != null ? attendanceData.getStatus() : "");
                statusValueCell.setCellStyle(centerAlignedStyle);

                Cell timeInValueCell = row.createCell(startCol + 1);
                String formattedTimeIn = formatTo12Hour(attendanceData.getTimeIn());
                timeInValueCell.setCellValue(formattedTimeIn); // Use the formatted time!
                timeInValueCell.setCellStyle(centerAlignedStyle);

                Cell timeOutValueCell = row.createCell(startCol + 2);
                String formattedTimeOut = formatTo12Hour(attendanceData.getTimeOut());
                timeOutValueCell.setCellValue(formattedTimeOut); // Use the formatted time!
                timeOutValueCell.setCellStyle(centerAlignedStyle);
            }
        }

        // Write workbook to file
        try {
            workbook.write(outputStream);
            workbook.close();
            outputStream.flush();
            outputStream.close();
            Log.d(TAG, "Excel file successfully written with " + completeData.size() + " rows of data");
        } catch (IOException e) {
            Log.e(TAG, "Error while writing Excel file: " + e.getMessage(), e);
            throw e;
        }

        // Display path and show success dialog
        String userFriendlyPath;
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
            userFriendlyPath = Environment.DIRECTORY_DOWNLOADS + "/" + fileName;
        } else {
            File exportDir = Environment.getExternalStoragePublicDirectory(Environment.DIRECTORY_DOWNLOADS);
            userFriendlyPath = new File(exportDir, fileName).getAbsolutePath();
        }

        showExportSuccessDialog(userFriendlyPath, fileUri);

        // Mark as successfully exported
        hasExportedSuccessfully = true;
        lastExportedFileName = fileName;
    }

    // Helper class to store attendance data for a specific day
    private static class AttendanceData {
        private String status;
        private String timeIn;
        private String timeOut;

        public AttendanceData(String status, String timeIn, String timeOut) {
            this.status = status;
            this.timeIn = timeIn;
            this.timeOut = timeOut;
        }

        public String getStatus() { return status; }
        public String getTimeIn() { return timeIn; }
        public String getTimeOut() { return timeOut; }
    }

    // Calculate number of days between start and end dates (inclusive)
    private int calculateNumberOfDays(String startDateStr, String endDateStr) {
        try {
            if (startDateStr == null || startDateStr.isEmpty()) {
                return 1; // Default to 1 day if no start date
            }

            if (endDateStr == null || endDateStr.isEmpty()) {
                return 1; // Default to 1 day if no end date
            }

            SimpleDateFormat dateFormat = new SimpleDateFormat("yyyy-MM-dd", Locale.getDefault());
            Date startDate = dateFormat.parse(startDateStr);
            Date endDate = dateFormat.parse(endDateStr);

            if (startDate == null || endDate == null) {
                return 1; // Default to 1 day if parsing fails
            }

            // Calculate difference in days
            long diffInMillies = Math.abs(endDate.getTime() - startDate.getTime());
            int diffInDays = (int) (diffInMillies / (1000 * 60 * 60 * 24)) + 1; // +1 to include both start and end days

            return diffInDays;
        } catch (Exception e) {
            Log.e(TAG, "Error calculating number of days: " + e.getMessage(), e);
            return 1; // Default to 1 day if any error occurs
        }
    }

    // Generate formatted dates for each day of the event
    private List<String> getFormattedEventDates(String startDateStr, String endDateStr) {
        List<String> formattedDates = new ArrayList<>();

        try {
            SimpleDateFormat inputFormat = new SimpleDateFormat("yyyy-MM-dd", Locale.getDefault());
            SimpleDateFormat outputFormat = new SimpleDateFormat("MMMM d, yyyy", Locale.getDefault());

            Date startDate = inputFormat.parse(startDateStr);
            Date endDate = inputFormat.parse(endDateStr);

            if (startDate == null || endDate == null) {
                // Fallback to "DAY 1" if parsing fails
                formattedDates.add("DAY 1");
                return formattedDates;
            }

            Calendar calendar = Calendar.getInstance();
            calendar.setTime(startDate);

            while (!calendar.getTime().after(endDate)) {
                Date currentDate = calendar.getTime();
                formattedDates.add(outputFormat.format(currentDate));
                calendar.add(Calendar.DAY_OF_MONTH, 1);
            }

            // If list is still empty, add at least one date
            if (formattedDates.isEmpty()) {
                formattedDates.add("DAY 1");
            }

        } catch (Exception e) {
            Log.e(TAG, "Error generating formatted dates: " + e.getMessage(), e);
            formattedDates.add("DAY 1"); // Fallback
        }

        return formattedDates;
    }

    // Modified method to fetch all attendance data for all participants
    private void fetchAllAttendanceData(List<Participant> participants, int totalDays,
                                        OnAllDataFetchCompleteListener listener) {
        // Initialize a counter for completed participants
        final AtomicInteger completedCount = new AtomicInteger(0);
        final List<ParticipantFullData> completeData = new ArrayList<>();

        // If no participants, return immediately
        if (participants.isEmpty()) {
            listener.onComplete(completeData);
            return;
        }

        for (Participant p : participants) {
            final ParticipantFullData fullData = new ParticipantFullData(p);

            // For each day, fetch the attendance data
            final AtomicInteger completedDays = new AtomicInteger(0);

            for (int day = 1; day <= totalDays; day++) {
                final int currentDay = day;

                // Reference to this participant's attendance for this day
                DatabaseReference ref = FirebaseDatabase.getInstance().getReference()
                        .child("students")
                        .child(p.getId())
                        .child("tickets")
                        .child(eventId)
                        .child("attendanceDays")
                        .child("day_" + currentDay);

                ref.addListenerForSingleValueEvent(new ValueEventListener() {
                    @Override
                    public void onDataChange(DataSnapshot dataSnapshot) {
                        try {
                            // Extract attendance data for this day
                            // IMPORTANT: Use 'attendance' field first, then fall back to 'status' if needed
                            String status = "";
                            String timeIn = "";
                            String timeOut = "";

                            // First try to get the attendance field (as per your requirement)
                            if (dataSnapshot.child("attendance").exists()) {
                                status = dataSnapshot.child("attendance").getValue(String.class);
                            }
                            // If no attendance field, try status field
                            else if (dataSnapshot.child("status").exists()) {
                                status = dataSnapshot.child("status").getValue(String.class);
                            }

                            // Get time in and time out
                            if (dataSnapshot.child("in").exists()) {
                                timeIn = dataSnapshot.child("in").getValue(String.class);
                            }

                            if (dataSnapshot.child("out").exists()) {
                                timeOut = dataSnapshot.child("out").getValue(String.class);
                            }

                            // Store this day's data
                            fullData.addDayData(currentDay, new AttendanceData(status, timeIn, timeOut));

                            Log.d(TAG, "Fetched data for participant " + p.getName() +
                                    ", day " + currentDay + ": status=" + status +
                                    ", timeIn=" + timeIn + ", timeOut=" + timeOut);
                        } catch (Exception e) {
                            Log.e(TAG, "Error parsing attendance data: " + e.getMessage(), e);
                            fullData.addDayData(currentDay, new AttendanceData("", "", ""));
                        }

                        // Check if we've completed all days for this participant
                        if (completedDays.incrementAndGet() == totalDays) {
                            completeData.add(fullData);

                            // Check if we've completed all participants
                            if (completedCount.incrementAndGet() == participants.size()) {
                                // All data fetched, notify listener
                                listener.onComplete(completeData);
                            }
                        }
                    }

                    @Override
                    public void onCancelled(DatabaseError databaseError) {
                        Log.e(TAG, "Database error: " + databaseError.getMessage());
                        // Handle error case, still continue with partial data
                        fullData.addDayData(currentDay, new AttendanceData("", "", ""));

                        if (completedDays.incrementAndGet() == totalDays) {
                            completeData.add(fullData);
                            if (completedCount.incrementAndGet() == participants.size()) {
                                listener.onComplete(completeData);
                            }
                        }
                    }
                });
            }
        }
    }

    // Helper class to store full participant data across all days
    private static class ParticipantFullData {
        private Participant basicInfo;
        private Map<Integer, AttendanceData> attendanceByDay = new HashMap<>();

        public ParticipantFullData(Participant basicInfo) {
            this.basicInfo = basicInfo;
        }

        public void addDayData(int day, AttendanceData data) {
            attendanceByDay.put(day, data);
        }

        public Participant getBasicInfo() {
            return basicInfo;
        }

        public AttendanceData getAttendanceForDay(int day) {
            return attendanceByDay.getOrDefault(day, new AttendanceData("", "", ""));
        }
    }

    // Interface for the callback when all data fetching is complete
    private interface OnAllDataFetchCompleteListener {
        void onComplete(List<ParticipantFullData> completeData);
        void onError(Exception e);
    }

    private void showExportSuccessDialog(String filePath, Uri fileUri) {
        AlertDialog.Builder builder = new AlertDialog.Builder(requireContext());
        builder.setTitle("Export Successful")
                .setMessage("Excel file saved to: " + filePath + "\n\nYou can find it in your Downloads folder.")
                .setPositiveButton("Share", (dialog, which) -> shareExcelFile(fileUri))
                .setNegativeButton("OK", null)
                .show();
        Log.d(TAG, "Export success dialog shown and marked as successfully exported");
    }

    private void shareExcelFile(Uri fileUri) {
        try {
            Intent shareIntent = new Intent(Intent.ACTION_SEND);
            shareIntent.setType("application/vnd.openxmlformats-officedocument.spreadsheetml.sheet");
            shareIntent.putExtra(Intent.EXTRA_SUBJECT, eventName + " Attendance Excel");
            shareIntent.putExtra(Intent.EXTRA_STREAM, fileUri);
            shareIntent.addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION);

            Log.d(TAG, "Starting share intent");
            startActivity(Intent.createChooser(shareIntent, "Share via"));
        } catch (Exception e) {
            Log.e(TAG, "Error sharing file: " + e.getMessage(), e);
            Toast.makeText(getContext(), "Error sharing file: " + e.getMessage(), Toast.LENGTH_SHORT).show();
        }
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

    @Override
    public void onRequestPermissionsResult(int requestCode, @NonNull String[] permissions, @NonNull int[] grantResults) {
        if (requestCode == WRITE_EXTERNAL_STORAGE_REQUEST_CODE) {
            if (grantResults.length > 0 && grantResults[0] == PackageManager.PERMISSION_GRANTED) {
                exportToExcel();
            } else {
                Toast.makeText(getContext(), "Storage permission denied. Cannot export Excel file.", Toast.LENGTH_SHORT).show();
            }
        }
    }

    // The following methods remain unchanged as they're not related to the export functionality
    private void updatePendingStatusesForAllDays() {
        if (!isMultiDayEvent) {
            Log.d(TAG, "Not a multi-day event, skipping daily attendance check");
            return;
        }

        String currentDate = getCurrentDate();
        String currentTime = getCurrentTime();
        Log.d(TAG, "Checking attendance for all days in multi-day event, current date: " + currentDate + ", time: " + currentTime);

        try {
            SimpleDateFormat dateFormat = new SimpleDateFormat("yyyy-MM-dd", Locale.getDefault());
            SimpleDateFormat timeFormat = new SimpleDateFormat("HH:mm", Locale.getDefault());
            Date currentDateObj = dateFormat.parse(currentDate);
            Date currentTimeObj = timeFormat.parse(currentTime);
            Date startDateObj = dateFormat.parse(eventStartDate);
            Date endDateObj = dateFormat.parse(eventEndDate);

            // For each day in the event range, check if we need to update attendance status
            Calendar calendar = Calendar.getInstance();
            calendar.setTime(startDateObj);
            Date checkDate = calendar.getTime();

            while (!checkDate.after(endDateObj)) {
                String dateString = dateFormat.format(checkDate);
                int dayNumber = calculateDayNumber(dateString);
                String dayKey = "day_" + dayNumber;

                // Calculate if we should check this day
                boolean shouldCheckDay = false;

                // If the day is today, check if event end time + 1 hour has passed
                if (dateString.equals(currentDate)) {
                    try {
                        // Add 1 hour to event end time
                        Date eventEndTimeObj = timeFormat.parse(eventEndTime);
                        Calendar endTimePlusOneHour = Calendar.getInstance();
                        endTimePlusOneHour.setTime(eventEndTimeObj);
                        endTimePlusOneHour.add(Calendar.HOUR_OF_DAY, 1);
                        Date endTimePlusOneHourObj = endTimePlusOneHour.getTime();

                        // If current time is after event end time + 1 hour, we should check this day
                        if (currentTimeObj.after(endTimePlusOneHourObj)) {
                            shouldCheckDay = true;
                            Log.d(TAG, "Today's end time + 1 hour has passed, checking day: " + dateString);
                        }
                    } catch (ParseException e) {
                        Log.e(TAG, "Error parsing event time for today's check", e);
                    }
                }
                // If the day is in the past, we should always check it
                else if (checkDate.before(currentDateObj)) {
                    shouldCheckDay = true;
                    Log.d(TAG, "Past day detected, checking day: " + dateString);
                }

                // If we should check this day, update the attendance status
                if (shouldCheckDay) {
                    Log.d(TAG, "Checking for missed attendance on " + dateString + " (day key: " + dayKey + ")");
                    updatePendingStatusesForDay(dayKey, dateString);
                } else {
                    Log.d(TAG, "Skipping attendance check for " + dateString + " (future day or today's event not yet ended)");
                }

                // Move to next day
                calendar.add(Calendar.DAY_OF_MONTH, 1);
                checkDate = calendar.getTime();
            }

        } catch (ParseException e) {
            Log.e(TAG, "Error parsing dates when checking all days", e);
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
            checkAndUpdateAttendanceForDay(participant.getId(), participant.getTicketRef(), dayKey, dateString);
        }
    }

    // Check and update attendance status for any day
    private void checkAndUpdateAttendanceForDay(String studentId, String ticketRef, String dayKey, String dateString) {
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

    // Update the setupStatusUpdateChecker method to use the new function
    private void setupStatusUpdateChecker() {
        // Initialize the status update runnable
        statusUpdateRunnable = new Runnable() {
            @Override
            public void run() {
                Log.d(TAG, "Running scheduled attendance status update check");
                updateAttendanceStatusForAll();
                updatePendingStatusesForAllDays(); // Use the new method that checks all days

                // Schedule the next update
                statusUpdateHandler.postDelayed(this, STATUS_UPDATE_INTERVAL);
            }
        };

        // Start the periodic updates
        statusUpdateHandler.post(statusUpdateRunnable);
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
}