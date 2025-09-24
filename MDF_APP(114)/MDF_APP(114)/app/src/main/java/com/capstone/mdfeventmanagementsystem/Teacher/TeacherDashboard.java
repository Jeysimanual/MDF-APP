package com.capstone.mdfeventmanagementsystem.Teacher;

import android.content.Context;
import android.content.Intent;
import android.graphics.PorterDuff;
import android.graphics.drawable.Drawable;
import android.os.Bundle;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.widget.ImageButton;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.TextView;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.appcompat.app.AlertDialog;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.content.ContextCompat;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;
import androidx.swiperefreshlayout.widget.SwipeRefreshLayout;

import com.capstone.mdfeventmanagementsystem.Adapters.EventAdapterTeacher;
import com.capstone.mdfeventmanagementsystem.R;
import com.capstone.mdfeventmanagementsystem.Student.Event;
import com.capstone.mdfeventmanagementsystem.Utilities.BaseActivity;
import com.google.android.material.bottomappbar.BottomAppBar;
import com.google.android.material.bottomnavigation.BottomNavigationView;
import com.google.android.material.button.MaterialButton;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseUser;
import com.google.firebase.database.DataSnapshot;
import com.google.firebase.database.DatabaseError;
import com.google.firebase.database.DatabaseReference;
import com.google.firebase.database.FirebaseDatabase;
import com.google.firebase.database.ValueEventListener;
import com.prolificinteractive.materialcalendarview.CalendarDay;
import com.prolificinteractive.materialcalendarview.MaterialCalendarView;
import com.prolificinteractive.materialcalendarview.DayViewDecorator;
import com.prolificinteractive.materialcalendarview.DayViewFacade;
import com.prolificinteractive.materialcalendarview.spans.DotSpan;

import java.time.LocalDate;
import java.time.LocalTime;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

public class TeacherDashboard extends BaseActivity {
    private RecyclerView recyclerViewAssignedEvents;
    private EventAdapterTeacher assignedEventAdapter;
    private List<Event> assignedEventList;
    private DatabaseReference databaseReference;
    private DatabaseReference teacherDatabaseReference;
    private static final DateTimeFormatter DATE_FORMATTER = DateTimeFormatter.ofPattern("yyyy-MM-dd");
    private String teacherYearLevelAdvisor;
    private static final String TAG = "TeacherDashboard";
    private MaterialCalendarView calendarView;
    private static final int MAX_EVENTS_TO_SHOW = 3;
    private SwipeRefreshLayout swipeRefreshLayout;
    private ImageView ivNotificationBell;
    private TextView tvNotificationCount;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_teacher_dashboard);

        findViewById(R.id.fab_create).setOnClickListener(v -> {
            startActivity(new Intent(getApplicationContext(), TeacherCreateEventActivity.class));
            overridePendingTransition(0, 0);
        });

        BottomAppBar bottomAppBar = findViewById(R.id.bottomAppBar);

        MaterialButton btnViewAllUpcoming = findViewById(R.id.btnViewAllUpcomingTeacher);
        btnViewAllUpcoming.setOnClickListener(v -> {
            Intent intent = new Intent(TeacherDashboard.this, TeacherEvents.class);
            startActivity(intent);
            overridePendingTransition(0, 0);
        });

        recyclerViewAssignedEvents = findViewById(R.id.recyclerViewEvents1);
        recyclerViewAssignedEvents.setLayoutManager(new LinearLayoutManager(this));
        assignedEventList = new ArrayList<>();

        calendarView = findViewById(R.id.calendarView);
        if (calendarView != null) {
            calendarView.setOnDateChangedListener((widget, date, selected) -> {
                LocalDate selectedDate = LocalDate.of(date.getYear(), date.getMonth() + 1, date.getDay());
                String formattedDate = selectedDate.format(DATE_FORMATTER);
                fetchEventsForDate(formattedDate);
            });
        }

        assignedEventAdapter = new EventAdapterTeacher(this, assignedEventList, event -> openEventDetails(event));
        recyclerViewAssignedEvents.setAdapter(assignedEventAdapter);

        databaseReference = FirebaseDatabase.getInstance().getReference("events");

        getCurrentTeacherData();

        BottomNavigationView bottomNavigationView = findViewById(R.id.bottom_navigation_teacher);
        bottomNavigationView.setSelectedItemId(R.id.nav_home_teacher);
        bottomNavigationView.setBackground(null);

        bottomNavigationView.setOnItemSelectedListener(item -> {
            int itemId = item.getItemId();
            if (itemId == R.id.nav_home_teacher) {
                return true;
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

        swipeRefreshLayout = findViewById(R.id.swipeRefreshLayout);
        swipeRefreshLayout.setColorSchemeResources(R.color.primary);
        swipeRefreshLayout.setOnRefreshListener(() -> {
            if (isNetworkAvailable()) {
                Log.d(TAG, "Swipe refresh triggered with network available");
                getCurrentTeacherData();
            } else {
                Log.d(TAG, "Swipe refresh triggered but no network available");
                Toast.makeText(this, "No network available", Toast.LENGTH_SHORT).show();
                swipeRefreshLayout.setRefreshing(false);
            }
        });

        setupNotification();
    }

    private boolean isNetworkAvailable() {
        android.net.ConnectivityManager connectivityManager = (android.net.ConnectivityManager) getSystemService(Context.CONNECTIVITY_SERVICE);
        return connectivityManager != null && connectivityManager.getActiveNetworkInfo() != null && connectivityManager.getActiveNetworkInfo().isConnected();
    }

    private void openEventDetails(Event event) {
        if (event == null) {
            Toast.makeText(this, "Cannot open event details: Event is null", Toast.LENGTH_SHORT).show();
            return;
        }
        Intent intent = new Intent(this, TeacherEventsInside.class);
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
                            String yearLevel = teacherSnapshot.child("year_level_advisor").getValue(String.class);
                            String section = teacherSnapshot.child("section").getValue(String.class);
                            StringBuilder displayText = new StringBuilder();
                            if (yearLevel != null && !yearLevel.isEmpty()) {
                                displayText.append("Grade ").append(yearLevel);
                                if (section != null && !section.isEmpty()) {
                                    displayText.append(" - ").append(section);
                                }
                            } else {
                                displayText.append("No Grade Assigned");
                            }
                            TextView tvGradeTitle = findViewById(R.id.tvGradeTitle);
                            tvGradeTitle.setText(displayText.toString());
                            Log.d(TAG, "Updated grade display: " + displayText);
                        }
                    } else {
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
                                fetchAssignedEvents();
                                displayTeacherGradeAndSection();
                                displayTotalStudents();
                            }
                        }
                    } else {
                        Log.e(TAG, "No teacher record found for email: " + userEmail);
                        Toast.makeText(TeacherDashboard.this, "No teacher record found!", Toast.LENGTH_SHORT).show();
                        fetchAllEvents();
                    }
                    Log.d(TAG, "Stopping SwipeRefreshLayout in getCurrentTeacherData onDataChange");
                    swipeRefreshLayout.setRefreshing(false);
                }
                @Override
                public void onCancelled(@NonNull DatabaseError error) {
                    Log.e(TAG, "Error fetching teacher data: " + error.getMessage());
                    Toast.makeText(TeacherDashboard.this, "Error fetching teacher data", Toast.LENGTH_SHORT).show();
                    fetchAllEvents();
                    Log.d(TAG, "Stopping SwipeRefreshLayout in getCurrentTeacherData onCancelled");
                    swipeRefreshLayout.setRefreshing(false);
                }
            });
        } else {
            Log.e(TAG, "No user is currently logged in");
            Toast.makeText(this, "No user logged in", Toast.LENGTH_SHORT).show();
            fetchAllEvents();
            Log.d(TAG, "Stopping SwipeRefreshLayout in getCurrentTeacherData no user");
            swipeRefreshLayout.setRefreshing(false);
        }
    }

    private void fetchAllEvents() {
        databaseReference.addValueEventListener(new ValueEventListener() {
            @Override
            public void onDataChange(@NonNull DataSnapshot snapshot) {
                assignedEventList.clear();
                List<Event> allEvents = new ArrayList<>();
                Set<CalendarDay> eventDates = new HashSet<>();
                for (DataSnapshot dataSnapshot : snapshot.getChildren()) {
                    Event event = dataSnapshot.getValue(Event.class);
                    if (event != null) {
                        event.setEventUID(dataSnapshot.getKey());
                        allEvents.add(event);
                        try {
                            LocalDate date = LocalDate.parse(event.getStartDate(), DATE_FORMATTER);
                            eventDates.add(CalendarDay.from(date.getYear(), date.getMonthValue() - 1, date.getDayOfMonth()));
                        } catch (Exception e) {
                            Log.e(TAG, "Error parsing date for event: " + event.getEventName(), e);
                        }
                    }
                }
                Collections.sort(allEvents, (e1, e2) -> {
                    try {
                        LocalDate date1 = LocalDate.parse(e1.getStartDate(), DATE_FORMATTER);
                        LocalDate date2 = LocalDate.parse(e2.getStartDate(), DATE_FORMATTER);
                        return date1.compareTo(date2);
                    } catch (Exception e) {
                        e.printStackTrace();
                        return 0;
                    }
                });
                int eventsToShow = Math.min(allEvents.size(), MAX_EVENTS_TO_SHOW);
                assignedEventList.addAll(allEvents.subList(0, eventsToShow));
                markEventDatesOnCalendar(eventDates);
                assignedEventAdapter.updateEventList(assignedEventList);
                Log.d(TAG, "Showing " + eventsToShow + " events out of " + allEvents.size() + " total events");
                Log.d(TAG, "Stopping SwipeRefreshLayout in fetchAllEvents onDataChange");
                swipeRefreshLayout.setRefreshing(false);
            }
            @Override
            public void onCancelled(@NonNull DatabaseError error) {
                Log.e(TAG, "Firebase data fetch failed: " + error.getMessage(), error.toException());
                Log.d(TAG, "Stopping SwipeRefreshLayout in fetchAllEvents onCancelled");
                swipeRefreshLayout.setRefreshing(false);
            }
        });
    }

    private void fetchAssignedEvents() {
        databaseReference.addValueEventListener(new ValueEventListener() {
            @Override
            public void onDataChange(@NonNull DataSnapshot snapshot) {
                assignedEventList.clear();
                List<Event> allEventsList = new ArrayList<>();
                Set<CalendarDay> eventDates = new HashSet<>();
                for (DataSnapshot dataSnapshot : snapshot.getChildren()) {
                    Event event = dataSnapshot.getValue(Event.class);
                    if (event != null) {
                        event.setEventUID(dataSnapshot.getKey());
                        allEventsList.add(event);
                        Log.d(TAG, "EVENT FOUND: " + event.getEventName() +
                                ", EventFor: '" + event.getEventFor() + "'" +
                                ", StartDate: " + event.getStartDate());
                        try {
                            LocalDate date = LocalDate.parse(event.getStartDate(), DATE_FORMATTER);
                            eventDates.add(CalendarDay.from(date.getYear(), date.getMonthValue() - 1, date.getDayOfMonth()));
                        } catch (Exception e) {
                            Log.e(TAG, "Error parsing date for event: " + event.getEventName(), e);
                        }
                    }
                }
                Log.d(TAG, "Total events in database: " + allEventsList.size());
                List<Event> relevantEvents = new ArrayList<>();
                for (Event event : allEventsList) {
                    boolean shouldAdd = false;
                    String eventFor = event.getEventFor();
                    if (eventFor != null) {
                        String normalizedEventFor = eventFor.toLowerCase();
                        if (normalizedEventFor.contains("all") ||
                                normalizedEventFor.contains("everyone") ||
                                normalizedEventFor.contains("teacher")) {
                            shouldAdd = true;
                            Log.d(TAG, "✓ Adding general event: " + event.getEventName() +
                                    ", EventFor: '" + eventFor + "'");
                        }
                        if (teacherYearLevelAdvisor != null && !teacherYearLevelAdvisor.isEmpty()) {
                            String yearLevel = teacherYearLevelAdvisor.toLowerCase().trim();
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
                    if (shouldAdd && !relevantEvents.contains(event)) {
                        relevantEvents.add(event);
                    }
                }
                Collections.sort(relevantEvents, (e1, e2) -> {
                    try {
                        LocalDate date1 = LocalDate.parse(e1.getStartDate(), DATE_FORMATTER);
                        LocalDate date2 = LocalDate.parse(e2.getStartDate(), DATE_FORMATTER);
                        return date1.compareTo(date2);
                    } catch (Exception e) {
                        e.printStackTrace();
                        return 0;
                    }
                });
                int eventsToShow = Math.min(relevantEvents.size(), MAX_EVENTS_TO_SHOW);
                assignedEventList.addAll(relevantEvents.subList(0, eventsToShow));
                markEventDatesOnCalendar(eventDates);
                assignedEventAdapter.updateEventList(assignedEventList);
                Log.d(TAG, "Showing " + eventsToShow + " events out of " + relevantEvents.size() + " relevant events");
                if (relevantEvents.isEmpty()) {
                    Toast.makeText(TeacherDashboard.this,
                            "No events found for your advising grade level: " +
                                    (teacherYearLevelAdvisor != null ? teacherYearLevelAdvisor : "N/A"),
                            Toast.LENGTH_LONG).show();
                }
                Log.d(TAG, "Stopping SwipeRefreshLayout in fetchAssignedEvents onDataChange");
                swipeRefreshLayout.setRefreshing(false);
            }
            @Override
            public void onCancelled(@NonNull DatabaseError error) {
                Log.e(TAG, "Fetching assigned events failed: " + error.getMessage());
                Log.d(TAG, "Stopping SwipeRefreshLayout in fetchAssignedEvents onCancelled");
                swipeRefreshLayout.setRefreshing(false);
            }
        });
    }

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
                            String yearLevel = teacherSnapshot.child("year_level_advisor").getValue(String.class);
                            String section = teacherSnapshot.child("section").getValue(String.class);
                            if (yearLevel != null && !yearLevel.isEmpty() && section != null && !section.isEmpty()) {
                                countMatchingStudents(yearLevel, section);
                            } else {
                                TextView tvTotalStudents = findViewById(R.id.tvTotalStudents);
                                tvTotalStudents.setText("Total Students: N/A");
                                Log.d(TAG, "Teacher has no year level or section assigned");
                            }
                        }
                    } else {
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
            TextView tvTotalStudents = findViewById(R.id.tvTotalStudents);
            tvTotalStudents.setText("Total Students: Log in required");
            Log.d(TAG, "No user logged in to display student count");
        }
    }

    private void countMatchingStudents(String yearLevel, String section) {
        DatabaseReference studentsRef = FirebaseDatabase.getInstance().getReference("students");
        studentsRef.addValueEventListener(new ValueEventListener() {
            @Override
            public void onDataChange(@NonNull DataSnapshot snapshot) {
                int studentCount = 0;
                Log.d(TAG, "Looking for students with yearLevel: " + yearLevel + " and section: " + section);
                for (DataSnapshot studentSnapshot : snapshot.getChildren()) {
                    String studentYearLevel = studentSnapshot.child("yearLevel").getValue(String.class);
                    String studentSection = studentSnapshot.child("section").getValue(String.class);
                    Log.d(TAG, "Student: " + studentSnapshot.getKey() +
                            " | yearLevel: " + studentYearLevel +
                            " | section: " + studentSection);
                    boolean yearLevelMatches = false;
                    if (studentYearLevel != null) {
                        yearLevelMatches = studentYearLevel.equals(yearLevel) ||
                                studentYearLevel.equals("Grade " + yearLevel) ||
                                (yearLevel.startsWith("Grade ") && studentYearLevel.equals(yearLevel.substring(6)));
                    }
                    if (yearLevelMatches && section.equals(studentSection)) {
                        studentCount++;
                        Log.d(TAG, "✓ MATCH FOUND! Student: " + studentSnapshot.getKey());
                    }
                }
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

    private void fetchEventsForDate(String selectedDate) {
        databaseReference.addListenerForSingleValueEvent(new ValueEventListener() {
            @Override
            public void onDataChange(@NonNull DataSnapshot snapshot) {
                List<Event> eventsOnDate = new ArrayList<>();
                for (DataSnapshot dataSnapshot : snapshot.getChildren()) {
                    Event event = dataSnapshot.getValue(Event.class);
                    if (event != null) {
                        event.setEventUID(dataSnapshot.getKey());
                        if (event.getStartDate() != null && event.getStartDate().equals(selectedDate)) {
                            String eventFor = event.getEventFor();
                            boolean shouldAdd = false;
                            if (eventFor != null) {
                                String normalizedEventFor = eventFor.toLowerCase();
                                if (normalizedEventFor.contains("all") ||
                                        normalizedEventFor.contains("everyone") ||
                                        normalizedEventFor.contains("teacher")) {
                                    shouldAdd = true;
                                }
                                if (teacherYearLevelAdvisor != null && !teacherYearLevelAdvisor.isEmpty()) {
                                    String yearLevel = teacherYearLevelAdvisor.toLowerCase().trim();
                                    if (normalizedEventFor.contains(yearLevel) ||
                                            normalizedEventFor.contains("grade" + yearLevel) ||
                                            normalizedEventFor.contains("grade " + yearLevel) ||
                                            normalizedEventFor.contains("g" + yearLevel) ||
                                            normalizedEventFor.contains("grade-" + yearLevel)) {
                                        shouldAdd = true;
                                    }
                                }
                            }
                            if (shouldAdd) {
                                eventsOnDate.add(event);
                            }
                        } else if (event.getStartDate() == null) {
                            Log.w(TAG, "Event with UID: " + dataSnapshot.getKey() + " has null startDate");
                        }
                    } else {
                        Log.w(TAG, "Failed to parse event for UID: " + dataSnapshot.getKey());
                    }
                }
                showEventDialog(TeacherDashboard.this, eventsOnDate, selectedDate);
            }
            @Override
            public void onCancelled(@NonNull DatabaseError error) {
                Log.e(TAG, "Error fetching events for date: " + error.getMessage());
                Toast.makeText(TeacherDashboard.this, "Error fetching events", Toast.LENGTH_SHORT).show();
            }
        });
    }

    private void showEventDialog(TeacherDashboard context, List<Event> events, String selectedDate) {
        if (events.isEmpty()) {
            LocalDate date = LocalDate.parse(selectedDate, DATE_FORMATTER);
            String formattedDate = date.format(DateTimeFormatter.ofPattern("MMM d, yyyy"));
            Toast.makeText(context, "No events on " + formattedDate, Toast.LENGTH_SHORT).show();
            return;
        }
        LayoutInflater inflater = LayoutInflater.from(context);
        View dialogView = inflater.inflate(R.layout.dialog_calendar_details, null);
        LocalDate date = LocalDate.parse(selectedDate, DATE_FORMATTER);
        String dayName = date.getDayOfWeek().toString().substring(0, 1).toUpperCase() + date.getDayOfWeek().toString().substring(1).toLowerCase();
        String monthDayYear = date.format(DateTimeFormatter.ofPattern("MMM d, yyyy"));
        TextView dialogDay = dialogView.findViewById(R.id.dialog_day);
        TextView dialogDayName = dialogView.findViewById(R.id.dialog_day_name);
        TextView dialogDate = dialogView.findViewById(R.id.dialog_date);
        dialogDay.setText(String.valueOf(date.getDayOfMonth()));
        dialogDayName.setText(dayName);
        dialogDate.setText(monthDayYear);
        LinearLayout eventContainer = null;
        if (!events.isEmpty()) {
            Event event = events.get(0);
            TextView eventName = dialogView.findViewById(R.id.event_name);
            TextView eventFor = dialogView.findViewById(R.id.event_for);
            TextView eventTime = dialogView.findViewById(R.id.event_time);
            View eventTypeLine = dialogView.findViewById(R.id.event_type_line);
            LinearLayout badgeContainer = dialogView.findViewById(R.id.event_badge);
            ImageView badgeIcon = dialogView.findViewById(R.id.badge_icon);
            eventContainer = dialogView.findViewById(R.id.event_container);
            String eventNameText = event.getEventName() != null ? event.getEventName() : "Unnamed Event";
            if (eventNameText.length() > 20) {
                eventNameText = eventNameText.substring(0, 17) + "...";
            }
            eventName.setText(eventNameText);
            eventFor.setText("For: " + (event.getEventFor() != null ? event.getEventFor() : "All"));
            String timeText = formatEventTime(event.getStartTime(), event.getEndTime());
            eventTime.setText(timeText);
            setEventTypeStyle(context, event.getEventType(), eventTypeLine, badgeIcon);
            // Make the event container clickable
            if (eventContainer != null) {
                eventContainer.setOnClickListener(v -> {
                    openEventDetails(event);
                    // Dismiss the dialog after clicking
                    AlertDialog dialog = (AlertDialog) v.getTag();
                    if (dialog != null) {
                        dialog.dismiss();
                    }
                });
            } else {
                Log.e(TAG, "Event container not found in dialog layout");
            }
        }
        AlertDialog.Builder builder = new AlertDialog.Builder(context, R.style.CustomDialogTheme);
        builder.setView(dialogView);
        builder.setCancelable(true);
        ImageButton btnClose = dialogView.findViewById(R.id.btn_close);
        AlertDialog dialog = builder.create();
        // Store the dialog in the event container's tag for dismissal
        if (eventContainer != null) {
            eventContainer.setTag(dialog);
        }
        btnClose.setOnClickListener(v -> dialog.dismiss());
        dialog.show();
    }

    private String formatEventTime(String startTime, String endTime) {
        if (startTime == null || endTime == null) {
            return "Time not available";
        }
        try {
            DateTimeFormatter inputFormatter = DateTimeFormatter.ofPattern("HH:mm");
            LocalTime start = LocalTime.parse(startTime, inputFormatter);
            LocalTime end = LocalTime.parse(endTime, inputFormatter);
            DateTimeFormatter formatter = DateTimeFormatter.ofPattern("h:mm a");
            String formattedStart = start.format(formatter);
            String formattedEnd = end.format(formatter);
            return formattedStart + " to " + formattedEnd;
        } catch (Exception e) {
            Log.e(TAG, "Error formatting event time: " + e.getMessage());
            return "Invalid time";
        }
    }

    private void setEventTypeStyle(TeacherDashboard context, String eventType, View lineView, ImageView badgeIcon) {
        int colorRes = R.color.event_other;
        int iconRes = R.drawable.ellipsis_h;
        if ("off-campus activity".equalsIgnoreCase(eventType)) {
            colorRes = R.color.event_off_campus;
            iconRes = R.drawable.map_marker_alt;
        } else if ("seminar".equalsIgnoreCase(eventType)) {
            colorRes = R.color.event_seminar;
            iconRes = R.drawable.chalkboard_teacher;
        } else if ("Sports Event".equalsIgnoreCase(eventType)) {
            colorRes = R.color.event_sports;
            iconRes = R.drawable.running;
        }
        lineView.setBackgroundColor(ContextCompat.getColor(context, colorRes));
        badgeIcon.setImageResource(iconRes);
        badgeIcon.setColorFilter(ContextCompat.getColor(context, R.color.black), PorterDuff.Mode.SRC_IN);
        LinearLayout badgeContainer = (LinearLayout) badgeIcon.getParent();
        Drawable background = ContextCompat.getDrawable(context, R.drawable.calendar_background);
        if (background != null) {
            background = background.mutate();
            background.setColorFilter(ContextCompat.getColor(context, colorRes), PorterDuff.Mode.SRC_IN);
            badgeContainer.setBackground(background);
        }
        View verticalLine = ((View) badgeContainer.getParent()).findViewById(R.id.event_type_vertical_line);
        if (verticalLine != null) {
            verticalLine.setBackgroundColor(ContextCompat.getColor(context, colorRes));
        }
    }

    private void markEventDatesOnCalendar(Set<CalendarDay> eventDates) {
        if (calendarView == null) {
            Log.e(TAG, "CalendarView is null, cannot mark event dates");
            return;
        }
        calendarView.removeDecorators();
        Map<CalendarDay, String> eventTypeMap = new HashMap<>();
        databaseReference.addListenerForSingleValueEvent(new ValueEventListener() {
            @Override
            public void onDataChange(@NonNull DataSnapshot snapshot) {
                for (DataSnapshot dataSnapshot : snapshot.getChildren()) {
                    Event event = dataSnapshot.getValue(Event.class);
                    if (event != null && event.getStartDate() != null) {
                        String eventFor = event.getEventFor();
                        boolean shouldMark = false;
                        if (eventFor != null) {
                            String normalizedEventFor = eventFor.toLowerCase();
                            if (normalizedEventFor.contains("all") ||
                                    normalizedEventFor.contains("everyone") ||
                                    normalizedEventFor.contains("teacher")) {
                                shouldMark = true;
                            }
                            if (teacherYearLevelAdvisor != null && !teacherYearLevelAdvisor.isEmpty()) {
                                String yearLevel = teacherYearLevelAdvisor.toLowerCase().trim();
                                if (normalizedEventFor.contains(yearLevel) ||
                                        normalizedEventFor.contains("grade" + yearLevel) ||
                                        normalizedEventFor.contains("grade " + yearLevel) ||
                                        normalizedEventFor.contains("g" + yearLevel) ||
                                        normalizedEventFor.contains("grade-" + yearLevel)) {
                                    shouldMark = true;
                                }
                            }
                        }
                        if (shouldMark) {
                            LocalDate date = LocalDate.parse(event.getStartDate(), DATE_FORMATTER);
                            CalendarDay calDay = CalendarDay.from(date.getYear(), date.getMonthValue() - 1, date.getDayOfMonth());
                            eventTypeMap.put(calDay, event.getEventType() != null ? event.getEventType() : "other");
                        }
                    }
                }
                for (Map.Entry<CalendarDay, String> entry : eventTypeMap.entrySet()) {
                    String eventType = entry.getValue();
                    int colorRes = getEventTypeColor(eventType);
                    DayViewDecorator decorator = new DayViewDecorator() {
                        @Override
                        public boolean shouldDecorate(CalendarDay day) {
                            return day.equals(entry.getKey());
                        }
                        @Override
                        public void decorate(DayViewFacade view) {
                            view.addSpan(new DotSpan(8, ContextCompat.getColor(TeacherDashboard.this, colorRes)));
                        }
                    };
                    calendarView.addDecorator(decorator);
                }
                calendarView.invalidateDecorators();
                Log.d(TAG, "Marked " + eventTypeMap.size() + " dates on calendar with dynamic colors");
            }
            @Override
            public void onCancelled(@NonNull DatabaseError error) {
                Log.e(TAG, "Error fetching event types: " + error.getMessage());
            }
        });
    }

    private int getEventTypeColor(String eventType) {
        String normalizedType = (eventType != null ? eventType.toLowerCase() : "other");
        switch (normalizedType) {
            case "off-campus activity":
                return R.color.event_off_campus;
            case "seminar":
                return R.color.event_seminar;
            case "sports event":
                return R.color.event_sports;
            default:
                return R.color.event_other;
        }
    }

    private void setupNotification() {
        ivNotificationBell = findViewById(R.id.ivNotificationBell);
        tvNotificationCount = findViewById(R.id.tvNotificationCount);

        ivNotificationBell.setOnClickListener(v -> {
            Intent intent = new Intent(TeacherDashboard.this, NotificationTeacher.class);
            startActivity(intent);
            overridePendingTransition(0, 0);
        });

        // Simulate notification count (replace with actual logic to fetch from database)
        int notificationCount = 5; // Example count
        if (notificationCount > 0) {
            tvNotificationCount.setText(String.valueOf(notificationCount));
            tvNotificationCount.setVisibility(View.VISIBLE);
        }
    }
}