package com.capstone.mdfeventmanagementsystem.Teacher;

import android.content.Intent;
import android.content.SharedPreferences;
import android.os.Bundle;
import android.util.Log;
import android.view.View;
import android.widget.TextView;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.fragment.app.Fragment;
import androidx.viewpager2.widget.ViewPager2;

import com.capstone.mdfeventmanagementsystem.Adapters.EventAdapter;
import com.capstone.mdfeventmanagementsystem.Adapters.EventsPagerAdapter;
import com.capstone.mdfeventmanagementsystem.Student.Event;
import com.capstone.mdfeventmanagementsystem.R;
import com.capstone.mdfeventmanagementsystem.Utilities.BaseActivity;
import com.google.android.material.bottomappbar.BottomAppBar;
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

public class TeacherEvents extends BaseActivity implements EventAdapter.OnEventClickListener {

    private List<Event> eventList;
    private DatabaseReference databaseReference;
    private static final DateTimeFormatter DATE_FORMATTER = DateTimeFormatter.ofPattern("yyyy-MM-dd");
    private static final String TAG = "TeacherEvents";

    private ViewPager2 viewPager;
    private TextView tabActive, tabExpired, tabApproval;
    private ActiveEventsFragment activeEventsFragment;
    private ExpiredEventsFragment expiredEventsFragment;
    private EventApprovalFragment approvalEventsFragment;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_teacher_events);

        findViewById(R.id.fab_create).setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                startActivity(new Intent(getApplicationContext(), TeacherCreateEventActivity.class));
                overridePendingTransition(0, 0);
            }
        });

        // Initialize UI components
        viewPager = findViewById(R.id.viewPager);
        tabActive = findViewById(R.id.tabActive);
        tabExpired = findViewById(R.id.tabExpired);
        tabApproval = findViewById(R.id.tabApproval);

        // Initialize fragments
        activeEventsFragment = new ActiveEventsFragment();
        expiredEventsFragment = new ExpiredEventsFragment();
        approvalEventsFragment = new EventApprovalFragment(); // Generic fragment for approval tab

        // Initialize ViewPager2 adapter with three fragments
        setupViewPager();

        // Initialize event list
        eventList = new ArrayList<>();

        // Set up tab click listeners
        setUpTabListeners();

        // Set up ViewPager page change listener
        setUpViewPagerListener();

        // Initialize Firebase connection
        databaseReference = FirebaseDatabase.getInstance().getReference("events");
        fetchEventsFromFirebase();

        // Bottom Navigation
        BottomNavigationView bottomNavigationView = findViewById(R.id.bottom_navigation_teacher);
        bottomNavigationView.setSelectedItemId(R.id.nav_event_teacher);
        bottomNavigationView.setBackground(null);

        bottomNavigationView.setOnNavigationItemSelectedListener(item -> {
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

    private void setupViewPager() {
        // Custom adapter implementation for three tabs
        viewPager.setAdapter(new androidx.viewpager2.adapter.FragmentStateAdapter(this) {
            @NonNull
            @Override
            public Fragment createFragment(int position) {
                switch (position) {
                    case 0:
                        return activeEventsFragment;
                    case 1:
                        return approvalEventsFragment;
                    case 2:
                        return expiredEventsFragment;
                    default:
                        return activeEventsFragment;
                }
            }

            @Override
            public int getItemCount() {
                return 3; // Three tabs: Active, Approval, Expired
            }
        });
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
            }
        });
    }

    private void updateTabUI(int position) {
        // Reset all tabs
        tabActive.setBackground(null);
        tabActive.setTextColor(getResources().getColor(R.color.gray));
        tabApproval.setBackground(null);
        tabApproval.setTextColor(getResources().getColor(R.color.gray));
        tabExpired.setBackground(null);
        tabExpired.setTextColor(getResources().getColor(R.color.gray));

        // Highlight selected tab
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

        databaseReference.addValueEventListener(new ValueEventListener() {
            @Override
            public void onDataChange(@NonNull DataSnapshot snapshot) {
                if (snapshot.exists()) {
                    Log.d(TAG, "Firebase data received. Children count: " + snapshot.getChildrenCount());
                } else {
                    Log.w(TAG, "No events found in Firebase.");
                }

                eventList.clear();
                int eventCount = 0;

                for (DataSnapshot dataSnapshot : snapshot.getChildren()) {
                    Event event = dataSnapshot.getValue(Event.class);
                    if (event != null) {
                        event.setEventUID(dataSnapshot.getKey()); // Set eventUID from Firebase key
                        eventList.add(event);
                        Log.d(TAG, "Event fetched: " + event.getEventName() + " | UID: " + event.getEventUID());
                        eventCount++;
                    } else {
                        Log.w(TAG, "Null event object found in Firebase.");
                    }
                }

                Log.d(TAG, "Total events fetched: " + eventCount);

                // Sort events by start date
                Collections.sort(eventList, (e1, e2) -> {
                    try {
                        LocalDate date1 = LocalDate.parse(e1.getStartDate(), DATE_FORMATTER);
                        LocalDate date2 = LocalDate.parse(e2.getStartDate(), DATE_FORMATTER);
                        return date1.compareTo(date2);
                    } catch (Exception e) {
                        Log.e(TAG, "Error sorting events: " + e.getMessage(), e);
                        return 0;
                    }
                });

                Log.d(TAG, "Events sorted successfully. First event: " + (eventList.isEmpty() ? "None" : eventList.get(0).getEventName()));

                // Update fragments with sorted events
                updateFragmentsWithEvents();
            }

            @Override
            public void onCancelled(@NonNull DatabaseError error) {
                Log.e(TAG, "Firebase data fetch failed: " + error.getMessage(), error.toException());
            }
        });
    }

    private void updateFragmentsWithEvents() {
        // Get current date
        LocalDate currentDate = LocalDate.now();

        // Create filtered lists
        List<Event> activeEvents = new ArrayList<>();
        List<Event> expiredEvents = new ArrayList<>();
        // We're not actually filtering for approval events since we're using a generic fragment

        // Filter events
        for (Event event : eventList) {
            try {
                LocalDate endDate = LocalDate.parse(event.getEndDate(), DATE_FORMATTER);
                if (endDate.isBefore(currentDate)) {
                    expiredEvents.add(event);
                } else {
                    activeEvents.add(event);
                }
            } catch (Exception e) {
                Log.e(TAG, "Error parsing date: " + e.getMessage());
            }
        }

        // Log counts for debugging
        Log.d(TAG, "Active events: " + activeEvents.size());
        Log.d(TAG, "Expired events: " + expiredEvents.size());

        // Update active events fragment
        if (activeEventsFragment != null) {
            activeEventsFragment.updateActiveEvents(activeEvents);
        }

        // Update expired events fragment
        if (expiredEventsFragment != null) {
            expiredEventsFragment.updateExpiredEvents(expiredEvents);
        }

        // No need to update approvalEventsFragment since we're using a generic Fragment
    }

    @Override
    public void onEventClick(Event event) {
        Log.d(TAG, "Event selected: " + event.getEventName() + " | UID: " + event.getEventUID());

        // Check user session from SharedPreferences
        SharedPreferences prefs = getSharedPreferences("UserSession", MODE_PRIVATE);
        String userType = prefs.getString("userType", ""); // Default empty if not found

        if (!"teacher".equals(userType)) {
            Log.e(TAG, "Unauthorized access: Only teachers can view event details.");
            Toast.makeText(this, "Access denied. Only teachers can view event details.", Toast.LENGTH_SHORT).show();
            return; // Prevent unauthorized access
        }

        Intent intent = new Intent(this, TeacherEventsInside.class);

        // Pass event details to the next activity
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