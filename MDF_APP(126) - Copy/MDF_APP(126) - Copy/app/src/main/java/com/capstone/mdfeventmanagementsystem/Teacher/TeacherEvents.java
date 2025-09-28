package com.capstone.mdfeventmanagementsystem.Teacher;

import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.os.Bundle;
import android.os.Handler;
import android.os.Looper;
import android.util.Log;
import android.view.View;
import android.widget.TextView;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.fragment.app.Fragment;
import androidx.swiperefreshlayout.widget.SwipeRefreshLayout;
import androidx.viewpager2.widget.ViewPager2;

import com.capstone.mdfeventmanagementsystem.Adapters.EventAdapterTeacher;
import com.capstone.mdfeventmanagementsystem.Student.Event;
import com.capstone.mdfeventmanagementsystem.R;
import com.capstone.mdfeventmanagementsystem.Utilities.BaseActivity;
import com.google.android.material.bottomnavigation.BottomNavigationView;
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

public class TeacherEvents extends BaseActivity implements EventAdapterTeacher.OnEventClickListener {

    private List<Event> activeAndApprovalEvents; // From 'events' node
    private List<Event> expiredEvents; // From 'archive_events' node
    private DatabaseReference eventsReference;
    private DatabaseReference archiveEventsReference;
    private static final DateTimeFormatter DATE_FORMATTER = DateTimeFormatter.ofPattern("yyyy-MM-dd");
    private static final String TAG = "TeacherEvents";

    private ViewPager2 viewPager;
    private TextView tabActive, tabExpired, tabApproval;
    private ActiveEventsFragment activeEventsFragment;
    private ExpiredEventsFragment expiredEventsFragment;
    private EventApprovalFragment approvalEventsFragment;
    private SwipeRefreshLayout swipeRefreshLayout;
    private boolean isViewPagerSetupComplete = false;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_teacher_events);

        findViewById(R.id.fab_create).setOnClickListener(v -> {
            startActivity(new Intent(getApplicationContext(), TeacherCreateEventActivity.class));
            overridePendingTransition(0, 0);
        });

        // Initialize UI components
        viewPager = findViewById(R.id.viewPager);
        tabActive = findViewById(R.id.tabActive);
        tabExpired = findViewById(R.id.tabExpired);
        tabApproval = findViewById(R.id.tabApproval);
        swipeRefreshLayout = findViewById(R.id.swipeRefreshLayout);

        // Initialize fragments
        activeEventsFragment = new ActiveEventsFragment();
        expiredEventsFragment = new ExpiredEventsFragment();
        approvalEventsFragment = new EventApprovalFragment();

        // Initialize event lists
        activeAndApprovalEvents = new ArrayList<>();
        expiredEvents = new ArrayList<>();

        // Initialize Firebase connections
        eventsReference = FirebaseDatabase.getInstance().getReference("events");
        archiveEventsReference = FirebaseDatabase.getInstance().getReference("archive_events");

        // Setup ViewPager2
        setupViewPager();
        setUpTabListeners();
        setUpViewPagerListener();

        // Wait for ViewPager to be ready before fetching events
        viewPager.post(() -> {
            isViewPagerSetupComplete = true;
            Log.d(TAG, "ViewPager setup complete, fetching events...");
            fetchEventsFromFirebase();
        });

        // Setup SwipeRefreshLayout
        swipeRefreshLayout.setColorSchemeResources(R.color.primary);
        swipeRefreshLayout.setOnRefreshListener(() -> {
            if (isNetworkAvailable()) {
                fetchEventsFromFirebase();
            } else {
                Toast.makeText(this, "No network available", Toast.LENGTH_SHORT).show();
                swipeRefreshLayout.setRefreshing(false);
            }
        });

        // Bottom Navigation
        BottomNavigationView bottomNavigationView = findViewById(R.id.bottom_navigation_teacher);
        bottomNavigationView.setSelectedItemId(R.id.nav_event_teacher);
        bottomNavigationView.setBackground(null);

        bottomNavigationView.setOnItemSelectedListener(item -> {
            int itemId = item.getItemId();
            if (itemId == R.id.nav_home_teacher) {
                startActivity(new Intent(getApplicationContext(), TeacherDashboard.class));
            } else if (itemId == R.id.nav_event_teacher) {
                return true;
            } else if (itemId == R.id.nav_scan_teacher) {
                startActivity(new Intent(getApplicationContext(), TeacherScanning.class));
            } else if (itemId == R.id.nav_profile_teacher) {
                startActivity(new Intent(getApplicationContext(), TeacherProfile.class));
            }
            overridePendingTransition(0, 0);
            return true;
        });
    }

    private boolean isNetworkAvailable() {
        android.net.ConnectivityManager connectivityManager = (android.net.ConnectivityManager) getSystemService(Context.CONNECTIVITY_SERVICE);
        return connectivityManager != null && connectivityManager.getActiveNetworkInfo() != null && connectivityManager.getActiveNetworkInfo().isConnected();
    }

    private void setupViewPager() {
        viewPager.setAdapter(new androidx.viewpager2.adapter.FragmentStateAdapter(this) {
            @NonNull
            @Override
            public Fragment createFragment(int position) {
                switch (position) {
                    case 0:
                        Log.d(TAG, "Creating ActiveEventsFragment");
                        return activeEventsFragment;
                    case 1:
                        Log.d(TAG, "Creating EventApprovalFragment");
                        return approvalEventsFragment;
                    case 2:
                        Log.d(TAG, "Creating ExpiredEventsFragment");
                        return expiredEventsFragment;
                    default:
                        Log.w(TAG, "Invalid position, returning ActiveEventsFragment");
                        return activeEventsFragment;
                }
            }

            @Override
            public int getItemCount() {
                return 3;
            }
        });
        viewPager.setOffscreenPageLimit(3); // Keep all fragments in memory
    }

    private void setUpTabListeners() {
        tabActive.setOnClickListener(v -> {
            viewPager.setCurrentItem(0);
            updateTabUI(0);
        });

        tabApproval.setOnClickListener(v -> {
            viewPager.setCurrentItem(1);
            updateTabUI(1);
        });

        tabExpired.setOnClickListener(v -> {
            viewPager.setCurrentItem(2);
            updateTabUI(2);
        });
    }

    private void setUpViewPagerListener() {
        viewPager.registerOnPageChangeCallback(new ViewPager2.OnPageChangeCallback() {
            @Override
            public void onPageSelected(int position) {
                super.onPageSelected(position);
                updateTabUI(position);
                if (position == 2 && !expiredEvents.isEmpty()) {
                    Log.d(TAG, "Expired tab selected, updating ExpiredEventsFragment");
                    processAndUpdateEvents();
                }
            }
        });
    }

    private void updateTabUI(int position) {
        tabActive.setBackground(null);
        tabActive.setTextColor(getResources().getColor(R.color.gray));
        tabApproval.setBackground(null);
        tabApproval.setTextColor(getResources().getColor(R.color.gray));
        tabExpired.setBackground(null);
        tabExpired.setTextColor(getResources().getColor(R.color.gray));

        if (position == 0) {
            tabActive.setBackground(getResources().getDrawable(R.drawable.tab_selected));
            tabActive.setTextColor(getResources().getColor(R.color.black));
        } else if (position == 1) {
            tabApproval.setBackground(getResources().getDrawable(R.drawable.tab_selected));
            tabApproval.setTextColor(getResources().getColor(R.color.black));
        } else {
            tabExpired.setBackground(getResources().getDrawable(R.drawable.tab_selected));
            tabExpired.setTextColor(getResources().getColor(R.color.black));
        }
    }

    private void fetchEventsFromFirebase() {
        Log.d(TAG, "Fetching events from Firebase...");
        swipeRefreshLayout.setRefreshing(true);
        activeAndApprovalEvents.clear();
        expiredEvents.clear();

        // Counter to track completion of both fetches
        final int[] fetchCounter = {0};
        final int totalFetches = 2;

        // Fetch active and approval events from 'events'
        eventsReference.addListenerForSingleValueEvent(new ValueEventListener() {
            @Override
            public void onDataChange(@NonNull DataSnapshot snapshot) {
                if (snapshot.exists()) {
                    Log.d(TAG, "Events data received. Children count: " + snapshot.getChildrenCount());
                } else {
                    Log.w(TAG, "No events found in 'events' node.");
                }

                int eventCount = 0;
                for (DataSnapshot dataSnapshot : snapshot.getChildren()) {
                    Event event = dataSnapshot.getValue(Event.class);
                    if (event != null) {
                        event.setEventUID(dataSnapshot.getKey());
                        activeAndApprovalEvents.add(event);
                        Log.d(TAG, "Event fetched from 'events': UID=" + event.getEventUID() +
                                ", Name=" + (event.getEventName() != null ? event.getEventName() : "null") +
                                ", Status=" + (event.getStatus() != null ? event.getStatus() : "null") +
                                ", EndDate=" + (event.getEndDate() != null ? event.getEndDate() : "null"));
                        eventCount++;
                    } else {
                        Log.w(TAG, "Null event object found in 'events' for key: " + dataSnapshot.getKey());
                    }
                }
                Log.d(TAG, "Total events fetched from 'events': " + eventCount);

                fetchCounter[0]++;
                if (fetchCounter[0] == totalFetches) {
                    processAndUpdateEvents();
                }
            }

            @Override
            public void onCancelled(@NonNull DatabaseError error) {
                Log.e(TAG, "Firebase 'events' fetch failed: " + error.getMessage(), error.toException());
                fetchCounter[0]++;
                if (fetchCounter[0] == totalFetches) {
                    processAndUpdateEvents();
                }
                Toast.makeText(TeacherEvents.this, "Failed to fetch events: " + error.getMessage(), Toast.LENGTH_SHORT).show();
            }
        });

        // Fetch expired events from 'archive_events'
        archiveEventsReference.addListenerForSingleValueEvent(new ValueEventListener() {
            @Override
            public void onDataChange(@NonNull DataSnapshot snapshot) {
                if (snapshot.exists()) {
                    Log.d(TAG, "Archive events data received. Children count: " + snapshot.getChildrenCount());
                } else {
                    Log.w(TAG, "No events found in 'archive_events' node.");
                }

                int archiveEventCount = 0;
                for (DataSnapshot dataSnapshot : snapshot.getChildren()) {
                    Event event = dataSnapshot.getValue(Event.class);
                    if (event != null) {
                        event.setEventUID(dataSnapshot.getKey());
                        expiredEvents.add(event);
                        Log.d(TAG, "Event fetched from 'archive_events': UID=" + event.getEventUID() +
                                ", Name=" + (event.getEventName() != null ? event.getEventName() : "null") +
                                ", Status=" + (event.getStatus() != null ? event.getStatus() : "null") +
                                ", EndDate=" + (event.getEndDate() != null ? event.getEndDate() : "null"));
                        archiveEventCount++;
                    } else {
                        Log.w(TAG, "Null event object found in 'archive_events' for key: " + dataSnapshot.getKey());
                    }
                }
                Log.d(TAG, "Total events fetched from 'archive_events': " + archiveEventCount);

                fetchCounter[0]++;
                if (fetchCounter[0] == totalFetches) {
                    processAndUpdateEvents();
                }
            }

            @Override
            public void onCancelled(@NonNull DatabaseError error) {
                Log.e(TAG, "Firebase 'archive_events' fetch failed: " + error.getMessage(), error.toException());
                fetchCounter[0]++;
                if (fetchCounter[0] == totalFetches) {
                    processAndUpdateEvents();
                }
                Toast.makeText(TeacherEvents.this, "Failed to fetch archive events: " + error.getMessage(), Toast.LENGTH_SHORT).show();
            }
        });
    }

    private void processAndUpdateEvents() {
        // Sort events
        List<Event> allEvents = new ArrayList<>();
        allEvents.addAll(activeAndApprovalEvents);
        allEvents.addAll(expiredEvents);

        Collections.sort(allEvents, (e1, e2) -> {
            try {
                LocalDate date1 = LocalDate.parse(e1.getStartDate(), DATE_FORMATTER);
                LocalDate date2 = LocalDate.parse(e2.getStartDate(), DATE_FORMATTER);
                return date1.compareTo(date2);
            } catch (Exception e) {
                Log.e(TAG, "Error sorting events: " + e.getMessage(), e);
                return 0;
            }
        });

        Log.d(TAG, "Events sorted successfully. Total events: " + allEvents.size());

        // Categorize events
        LocalDate currentDate = LocalDate.now();
        List<Event> activeEvents = new ArrayList<>();
        List<Event> approvalEvents = new ArrayList<>();

        for (Event event : activeAndApprovalEvents) {
            try {
                String eventName = event.getEventName() != null ? event.getEventName() : "Unknown";
                String eventUID = event.getEventUID() != null ? event.getEventUID() : "Unknown";
                String endDateStr = event.getEndDate();
                if (endDateStr == null || endDateStr.isEmpty()) {
                    Log.w(TAG, "Event endDate is null or empty for event: UID=" + eventUID + ", Name=" + eventName);
                    continue;
                }
                LocalDate endDate = LocalDate.parse(endDateStr, DATE_FORMATTER);
                if (!endDate.isBefore(currentDate)) {
                    activeEvents.add(event);
                    Log.d(TAG, "Classified as active: UID=" + eventUID + ", Name=" + eventName + ", EndDate=" + endDateStr);
                }
                if ("pending".equals(event.getStatus())) {
                    approvalEvents.add(event);
                    Log.d(TAG, "Classified as approval: UID=" + eventUID + ", Name=" + eventName);
                }
            } catch (Exception e) {
                Log.e(TAG, "Error parsing date for event: UID=" + (event.getEventUID() != null ? event.getEventUID() : "Unknown") +
                        ", Name=" + (event.getEventName() != null ? event.getEventName() : "Unknown") +
                        ", Error=" + e.getMessage());
            }
        }

        Log.d(TAG, "Active events: " + activeEvents.size());
        Log.d(TAG, "Expired events: " + expiredEvents.size());
        Log.d(TAG, "Approval events: " + approvalEvents.size());

        // Update fragments
        if (activeEventsFragment != null && activeEventsFragment.isAdded()) {
            activeEventsFragment.updateActiveEvents(activeEvents);
            Log.d(TAG, "Updated ActiveEventsFragment with " + activeEvents.size() + " events");
        } else {
            Log.w(TAG, "ActiveEventsFragment not ready, will retry");
        }

        if (expiredEventsFragment != null && expiredEventsFragment.isAdded()) {
            expiredEventsFragment.filterExpiredEvents(expiredEvents);
            Log.d(TAG, "Updated ExpiredEventsFragment with " + expiredEvents.size() + " events");
        } else {
            Log.w(TAG, "ExpiredEventsFragment not ready, will retry");
        }

        if (approvalEventsFragment != null && approvalEventsFragment.isAdded()) {
            // Assuming a similar method exists for approvalEventsFragment
            // approvalEventsFragment.updateApprovalEvents(approvalEvents);
            Log.d(TAG, "Updated EventApprovalFragment with " + approvalEvents.size() + " events");
        } else {
            Log.w(TAG, "EventApprovalFragment not ready, will retry");
        }

        // Retry if any fragment is not ready
        if (!areFragmentsReady()) {
            Log.d(TAG, "One or more fragments not ready, scheduling retry...");
            new Handler(Looper.getMainLooper()).postDelayed(this::processAndUpdateEvents, 500);
        }

        swipeRefreshLayout.setRefreshing(false);
    }

    private boolean areFragmentsReady() {
        return (activeEventsFragment != null && activeEventsFragment.isAdded()) &&
                (expiredEventsFragment != null && expiredEventsFragment.isAdded()) &&
                (approvalEventsFragment != null && approvalEventsFragment.isAdded());
    }

    @Override
    public void onEventClick(Event event) {
        Log.d(TAG, "Event selected: " + (event.getEventName() != null ? event.getEventName() : "Unknown") +
                " | UID: " + (event.getEventUID() != null ? event.getEventUID() : "Unknown"));

        SharedPreferences prefs = getSharedPreferences("UserSession", MODE_PRIVATE);
        String userType = prefs.getString("userType", "");

        if (!"teacher".equals(userType)) {
            Log.e(TAG, "Unauthorized access: Only teachers can view event details.");
            Toast.makeText(this, "Access denied. Only teachers can view event details.", Toast.LENGTH_SHORT).show();
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
        intent.putExtra("ticketType", event.getTicketType());
        intent.putExtra("graceTime", event.getGraceTime());
        intent.putExtra("eventPhotoUrl", event.getEventPhotoUrl());
        intent.putExtra("eventType", event.getEventType());
        intent.putExtra("eventFor", event.getEventFor());

        Log.d(TAG, "Navigating to TeacherEventsInside");
        startActivity(intent);
    }
}