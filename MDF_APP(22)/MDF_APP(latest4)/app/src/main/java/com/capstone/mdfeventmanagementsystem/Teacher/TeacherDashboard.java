package com.capstone.mdfeventmanagementsystem.Teacher;

import android.content.Intent;
import android.os.Bundle;
import android.util.Log;
import android.widget.CalendarView;
import android.widget.TextView;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.capstone.mdfeventmanagementsystem.Adapters.EventAdapter;
import com.capstone.mdfeventmanagementsystem.R;
import com.capstone.mdfeventmanagementsystem.Student.Event;
import com.google.android.material.bottomnavigation.BottomNavigationView;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseUser;
import com.google.firebase.database.DataSnapshot;
import com.google.firebase.database.DatabaseError;
import com.google.firebase.database.DatabaseReference;
import com.google.firebase.database.FirebaseDatabase;
import com.google.firebase.database.ValueEventListener;

import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.Calendar;
import java.util.Collections;
import java.util.List;

public class TeacherDashboard extends AppCompatActivity {
    private RecyclerView recyclerViewAssignedEvents;
    private EventAdapter assignedEventAdapter;
    private List<Event> assignedEventList;
    private DatabaseReference databaseReference;
    private DatabaseReference teacherDatabaseReference;
    private static final DateTimeFormatter DATE_FORMATTER = DateTimeFormatter.ofPattern("yyyy-MM-dd");
    private String teacherYearLevelAdvisor;
    private static final String TAG = "TeacherDashboard";
    private CalendarView calendarView;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_teacher_dashboard);

        // Setup RecyclerView for assigned events with horizontal orientation
        recyclerViewAssignedEvents = findViewById(R.id.recyclerViewEvents1);
        LinearLayoutManager horizontalLayoutManager = new LinearLayoutManager(this, LinearLayoutManager.HORIZONTAL, false);
        recyclerViewAssignedEvents.setLayoutManager(horizontalLayoutManager);
        assignedEventList = new ArrayList<>();

        // Initialize calendar view
        calendarView = findViewById(R.id.calendarView);
        if (calendarView != null) {
            calendarView.setOnDateChangeListener((view, year, month, dayOfMonth) -> {
                // Handle date selection
                Calendar selectedDate = Calendar.getInstance();
                selectedDate.set(year, month, dayOfMonth);
                // You can add filtering based on selected date here
                Toast.makeText(TeacherDashboard.this,
                        "Selected date: " + (month + 1) + "/" + dayOfMonth + "/" + year,
                        Toast.LENGTH_SHORT).show();
            });
        }

        // Initialize adapters with click listener
        assignedEventAdapter = new EventAdapter(this, assignedEventList, event -> {
            // Open event details when an event is clicked
            openEventDetails(event);
        });
        recyclerViewAssignedEvents.setAdapter(assignedEventAdapter);

        // Firebase reference to "events"
        databaseReference = FirebaseDatabase.getInstance().getReference("events");

        // Get current teacher's data
        getCurrentTeacherData();

        // Setup bottom navigation
        BottomNavigationView bottomNavigationView = findViewById(R.id.bottom_navigation_teacher);
        bottomNavigationView.setSelectedItemId(R.id.nav_home_teacher);

        bottomNavigationView.setOnItemSelectedListener(item -> {
            int itemId = item.getItemId();

            if (itemId == R.id.nav_home_teacher) {
                return true; // Stay on the same page
            } else if (itemId == R.id.nav_event_teacher) {
                startActivity(new Intent(this, TeacherEvents.class));
                finish();
            } else if (itemId == R.id.nav_scan_teacher) {
                startActivity(new Intent(this, TeacherScanning.class));
                finish();
            } else if (itemId == R.id.nav_profile_teacher) {
                startActivity(new Intent(this, TeacherProfile.class));
                finish();
            }

            overridePendingTransition(0, 0);
            return true;
        });
    }

    // Method to open event details when clicked
    private void openEventDetails(Event event) {
        if (event == null) {
            Toast.makeText(this, "Cannot open event details: Event is null", Toast.LENGTH_SHORT).show();
            return;
        }

        Intent intent = new Intent(this, TeacherEventsInside.class);

        // Pass all event details to TeacherEventsInside
        intent.putExtra("eventUID", event.getEventUID());
        intent.putExtra("eventName", event.getEventName());
        intent.putExtra("eventDescription", event.getEventDescription());
        intent.putExtra("startDate", event.getStartDate());
        intent.putExtra("endDate", event.getEndDate());
        intent.putExtra("startTime", event.getStartTime());
        intent.putExtra("endTime", event.getEndTime());
        intent.putExtra("venue", event.getVenue());
        intent.putExtra("eventSpan", event.getEventSpan());
        intent.putExtra("graceTime", event.getGraceTime());
        intent.putExtra("eventType", event.getEventType());
        intent.putExtra("eventFor", event.getEventFor());
        intent.putExtra("eventPhotoUrl", event.getEventPhotoUrl());

        startActivity(intent);
        Log.d(TAG, "Opening event details for: " + event.getEventName());
    }

    // Display teacher's grade and section
    private void displayTeacherGradeAndSection() {
        FirebaseUser currentUser = FirebaseAuth.getInstance().getCurrentUser();
        if (currentUser != null) {
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

                            // Format display text (e.g., "Grade 10 - Newton")
                            StringBuilder displayText = new StringBuilder();
                            if (yearLevel != null && !yearLevel.isEmpty()) {
                                displayText.append("Grade ").append(yearLevel);

                                if (section != null && !section.isEmpty()) {
                                    displayText.append(" - ").append(section);
                                }
                            } else {
                                displayText.append("No Grade Assigned");
                            }

                            // Update the TextView
                            TextView tvGradeTitle = findViewById(R.id.tvGradeTitle);
                            tvGradeTitle.setText(displayText.toString());

                            Log.d(TAG, "Updated grade display: " + displayText);
                        }
                    } else {
                        // No teacher record found
                        TextView tvGradeTitle = findViewById(R.id.tvGradeTitle);
                        tvGradeTitle.setText("Teacher Profile Not Found");
                        Log.d(TAG, "No teacher record found for: " + userEmail);
                    }
                }

                @Override
                public void onCancelled(@NonNull DatabaseError error) {
                    Log.e(TAG, "Error fetching teacher grade/section: " + error.getMessage());
                    TextView tvGradeTitle = findViewById(R.id.tvGradeTitle);
                    tvGradeTitle.setText("Error Loading Grade Info");
                }
            });
        } else {
            // No user logged in
            TextView tvGradeTitle = findViewById(R.id.tvGradeTitle);
            tvGradeTitle.setText("Please Log In");
            Log.d(TAG, "No user logged in to display grade/section");
        }
    }

    private void getCurrentTeacherData() {
        FirebaseUser currentUser = FirebaseAuth.getInstance().getCurrentUser();
        if (currentUser != null) {
            String userEmail = currentUser.getEmail();
            teacherDatabaseReference = FirebaseDatabase.getInstance().getReference("teachers");

            teacherDatabaseReference.orderByChild("email").equalTo(userEmail).addListenerForSingleValueEvent(new ValueEventListener() {
                @Override
                public void onDataChange(@NonNull DataSnapshot snapshot) {
                    if (snapshot.exists()) {
                        for (DataSnapshot teacherSnapshot : snapshot.getChildren()) {
                            Teacher teacher = teacherSnapshot.getValue(Teacher.class);
                            if (teacher != null) {
                                teacherYearLevelAdvisor = teacher.getYear_level_advisor();

                                Log.d(TAG, "Retrieved teacher data - Name: " + teacher.getFirstname() + " " +
                                        teacher.getLastname() + ", Year Level Advisor: " + teacherYearLevelAdvisor);

                                // Now that we have the year level advisor, fetch events
                                fetchAssignedEvents();

                                // Call the new function to display grade and section
                                displayTeacherGradeAndSection();

                                // Call the new function to display total students
                                displayTotalStudents();
                            }
                        }
                    } else {
                        Log.e(TAG, "No teacher record found for email: " + userEmail);
                        Toast.makeText(TeacherDashboard.this, "No teacher record found!", Toast.LENGTH_SHORT).show();

                        // Show all events even without teacher record
                        fetchAllEvents();
                    }
                }

                @Override
                public void onCancelled(@NonNull DatabaseError error) {
                    Log.e(TAG, "Error fetching teacher data: " + error.getMessage());
                    Toast.makeText(TeacherDashboard.this, "Error fetching teacher data", Toast.LENGTH_SHORT).show();

                    // Show all events even with error
                    fetchAllEvents();
                }
            });
        } else {
            Log.e(TAG, "No user is currently logged in");
            Toast.makeText(this, "No user logged in", Toast.LENGTH_SHORT).show();

            // Show all events even without login
            fetchAllEvents();
        }
    }

    // If we can't find teacher data, just show all events
    private void fetchAllEvents() {
        databaseReference.addValueEventListener(new ValueEventListener() {
            @Override
            public void onDataChange(@NonNull DataSnapshot snapshot) {
                assignedEventList.clear();

                for (DataSnapshot dataSnapshot : snapshot.getChildren()) {
                    Event event = dataSnapshot.getValue(Event.class);
                    if (event != null) {
                        event.setEventUID(dataSnapshot.getKey());
                        assignedEventList.add(event);
                    }
                }

                // Sort by date
                Collections.sort(assignedEventList, (e1, e2) -> {
                    try {
                        LocalDate date1 = LocalDate.parse(e1.getStartDate(), DATE_FORMATTER);
                        LocalDate date2 = LocalDate.parse(e2.getStartDate(), DATE_FORMATTER);
                        return date1.compareTo(date2);
                    } catch (Exception e) {
                        e.printStackTrace();
                        return 0;
                    }
                });

                assignedEventAdapter.updateEventList(assignedEventList);
                Log.d(TAG, "Showing all events: " + assignedEventList.size());
            }

            @Override
            public void onCancelled(@NonNull DatabaseError error) {
                Log.e(TAG, "Firebase data fetch failed: " + error.getMessage(), error.toException());
            }
        });
    }

    // Method to fetch events assigned to the teacher's year level
    private void fetchAssignedEvents() {
        databaseReference.addValueEventListener(new ValueEventListener() {
            @Override
            public void onDataChange(@NonNull DataSnapshot snapshot) {
                assignedEventList.clear();
                List<Event> allEventsList = new ArrayList<>(); // Temporary list to collect all events

                // First, get all events to analyze them
                for (DataSnapshot dataSnapshot : snapshot.getChildren()) {
                    Event event = dataSnapshot.getValue(Event.class);
                    if (event != null) {
                        event.setEventUID(dataSnapshot.getKey());
                        allEventsList.add(event);

                        // Debug log all events
                        Log.d(TAG, "EVENT FOUND: " + event.getEventName() +
                                ", EventFor: '" + event.getEventFor() + "'" +
                                ", StartDate: " + event.getStartDate());
                    }
                }

                Log.d(TAG, "Total events in database: " + allEventsList.size());

                // Process each event to check if it's relevant to this teacher
                for (Event event : allEventsList) {
                    boolean shouldAdd = false;
                    String eventFor = event.getEventFor();

                    if (eventFor != null) {
                        // Convert to lowercase for case-insensitive comparison
                        String normalizedEventFor = eventFor.toLowerCase();

                        // 1. Add events for "all" or "everyone" or "teachers"
                        if (normalizedEventFor.contains("all") ||
                                normalizedEventFor.contains("everyone") ||
                                normalizedEventFor.contains("teacher")) {
                            shouldAdd = true;
                            Log.d(TAG, "✓ Adding general event: " + event.getEventName() +
                                    ", EventFor: '" + eventFor + "'");
                        }

                        // 2. Add if we have teacher year level advisor and it matches
                        if (teacherYearLevelAdvisor != null && !teacherYearLevelAdvisor.isEmpty()) {
                            String yearLevel = teacherYearLevelAdvisor.toLowerCase().trim();

                            // Different formats for matching (grade8, grade 8, g8, etc.)
                            if (normalizedEventFor.contains(yearLevel) ||
                                    normalizedEventFor.contains("grade" + yearLevel) ||
                                    normalizedEventFor.contains("grade " + yearLevel) ||
                                    normalizedEventFor.contains("g" + yearLevel) ||
                                    normalizedEventFor.contains("grade-" + yearLevel)) {
                                shouldAdd = true;
                                Log.d(TAG, "✓ Adding year-specific event: " + event.getEventName() +
                                        " for year level: " + yearLevel);
                            }
                        }
                    }

                    // Add the event if it meets any criteria
                    if (shouldAdd && !assignedEventList.contains(event)) {
                        assignedEventList.add(event);
                    }
                }

                // Sort assigned events by date
                Collections.sort(assignedEventList, (e1, e2) -> {
                    try {
                        LocalDate date1 = LocalDate.parse(e1.getStartDate(), DATE_FORMATTER);
                        LocalDate date2 = LocalDate.parse(e2.getStartDate(), DATE_FORMATTER);
                        return date1.compareTo(date2);
                    } catch (Exception e) {
                        e.printStackTrace();
                        return 0;
                    }
                });

                // Update the adapter
                assignedEventAdapter.updateEventList(assignedEventList);

                // Log the final result
                Log.d(TAG, "Final assigned events count: " + assignedEventList.size());

                if (assignedEventList.isEmpty()) {
                    Toast.makeText(TeacherDashboard.this,
                            "No events found for your advising grade level: " +
                                    (teacherYearLevelAdvisor != null ? teacherYearLevelAdvisor : "N/A"),
                            Toast.LENGTH_LONG).show();
                }
            }

            @Override
            public void onCancelled(@NonNull DatabaseError error) {
                Log.e(TAG, "Fetching assigned events failed: " + error.getMessage());
            }
        });
    }

    // Method to fetch and display total students in teacher's section
    private void displayTotalStudents() {
        FirebaseUser currentUser = FirebaseAuth.getInstance().getCurrentUser();
        if (currentUser != null) {
            String userEmail = currentUser.getEmail();
            DatabaseReference teachersRef = FirebaseDatabase.getInstance().getReference("teachers");

            teachersRef.orderByChild("email").equalTo(userEmail).addListenerForSingleValueEvent(new ValueEventListener() {
                @Override
                public void onDataChange(@NonNull DataSnapshot snapshot) {
                    if (snapshot.exists()) {
                        for (DataSnapshot teacherSnapshot : snapshot.getChildren()) {
                            // Get the teacher's year level and section
                            String yearLevel = teacherSnapshot.child("year_level_advisor").getValue(String.class);
                            String section = teacherSnapshot.child("section").getValue(String.class);

                            // If we have both year level and section, count matching students
                            if (yearLevel != null && !yearLevel.isEmpty() && section != null && !section.isEmpty()) {
                                countMatchingStudents(yearLevel, section);
                            } else {
                                // No year level or section assigned
                                TextView tvTotalStudents = findViewById(R.id.tvTotalStudents);
                                tvTotalStudents.setText("Total Students: N/A");
                                Log.d(TAG, "Teacher has no year level or section assigned");
                            }
                        }
                    } else {
                        // No teacher record found
                        TextView tvTotalStudents = findViewById(R.id.tvTotalStudents);
                        tvTotalStudents.setText("Total Students: N/A");
                        Log.d(TAG, "No teacher record found for: " + userEmail);
                    }
                }

                @Override
                public void onCancelled(@NonNull DatabaseError error) {
                    Log.e(TAG, "Error fetching teacher data for student count: " + error.getMessage());
                    TextView tvTotalStudents = findViewById(R.id.tvTotalStudents);
                    tvTotalStudents.setText("Total Students: Error");
                }
            });
        } else {
            // No user logged in
            TextView tvTotalStudents = findViewById(R.id.tvTotalStudents);
            tvTotalStudents.setText("Total Students: Log in required");
            Log.d(TAG, "No user logged in to display student count");
        }
    }

    // Method to count students matching the teacher's year level and section
    private void countMatchingStudents(String yearLevel, String section) {
        DatabaseReference studentsRef = FirebaseDatabase.getInstance().getReference("students");

        // Query students where yearLevel and section match
        studentsRef.addValueEventListener(new ValueEventListener() {
            @Override
            public void onDataChange(@NonNull DataSnapshot snapshot) {
                int studentCount = 0;

                // Debug log
                Log.d(TAG, "Looking for students with yearLevel: " + yearLevel + " and section: " + section);

                // Loop through all students
                for (DataSnapshot studentSnapshot : snapshot.getChildren()) {
                    String studentYearLevel = studentSnapshot.child("yearLevel").getValue(String.class);
                    String studentSection = studentSnapshot.child("section").getValue(String.class);

                    Log.d(TAG, "Student: " + studentSnapshot.getKey() +
                            " | yearLevel: " + studentYearLevel +
                            " | section: " + studentSection);

                    // Format comparison - handle "Grade X" vs "X" format
                    boolean yearLevelMatches = false;
                    if (studentYearLevel != null) {
                        // Match either exactly, or "Grade X" format with just "X"
                        yearLevelMatches = studentYearLevel.equals(yearLevel) ||
                                studentYearLevel.equals("Grade " + yearLevel) ||
                                (yearLevel.startsWith("Grade ") && studentYearLevel.equals(yearLevel.substring(6)));
                    }

                    // Check if this student matches the teacher's year level and section
                    if (yearLevelMatches && section.equals(studentSection)) {
                        studentCount++;
                        Log.d(TAG, "✓ MATCH FOUND! Student: " + studentSnapshot.getKey());
                    }
                }

                // Update the UI with the student count
                TextView tvTotalStudents = findViewById(R.id.tvTotalStudents);
                tvTotalStudents.setText("Total Students: " + studentCount);

                Log.d(TAG, "Found " + studentCount + " students in Grade " + yearLevel + " - " + section);
            }

            @Override
            public void onCancelled(@NonNull DatabaseError error) {
                Log.e(TAG, "Error counting students: " + error.getMessage());
                TextView tvTotalStudents = findViewById(R.id.tvTotalStudents);
                tvTotalStudents.setText("Total Students: Error");
            }
        });
    }
}