package com.capstone.mdfeventmanagementsystem.Teacher;

import android.Manifest;
import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.content.pm.PackageManager;
import android.graphics.PorterDuff;
import android.graphics.drawable.Drawable;
import android.os.Build;
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
import androidx.core.app.ActivityCompat;
import androidx.core.content.ContextCompat;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;
import androidx.swiperefreshlayout.widget.SwipeRefreshLayout;

import com.capstone.mdfeventmanagementsystem.Adapters.EventAdapterTeacher;
import com.capstone.mdfeventmanagementsystem.Adapters.NotificationAdapter;
import com.capstone.mdfeventmanagementsystem.Models.Notification;
import com.capstone.mdfeventmanagementsystem.R;
import com.capstone.mdfeventmanagementsystem.Student.Event;
import com.capstone.mdfeventmanagementsystem.Utilities.BaseActivity;
import com.google.android.gms.tasks.Task;
import com.google.android.gms.tasks.Tasks;
import com.google.android.material.bottomappbar.BottomAppBar;
import com.google.android.material.bottomnavigation.BottomNavigationView;
import com.google.android.material.button.MaterialButton;
import com.google.android.material.floatingactionbutton.FloatingActionButton;
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
    private String currentTeacherId;
    private TextView badgeTextView;
    private DatabaseReference notificationsRef;
    private ValueEventListener notificationListener;
    private ValueEventListener realtimeNotifListener;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_teacher_dashboard);

        // Initialize notification components
        notificationsRef = FirebaseDatabase.getInstance().getReference("notificationsHistory/teachers");
        badgeTextView = findViewById(R.id.notification_badge);
        FloatingActionButton fabNotifications = findViewById(R.id.fab_notifications);

        // Initialize currentTeacherId from SharedPreferences
        SharedPreferences prefs = getSharedPreferences("UserSession", MODE_PRIVATE);
        currentTeacherId = prefs.getString("teacherId", null);
        Log.d(TAG, "onCreate: Initialized currentTeacherId from SharedPreferences: " + currentTeacherId);

        if (currentTeacherId == null || currentTeacherId.isEmpty()) {
            Log.e(TAG, "onCreate: No teacherId in SharedPreferences. Redirecting to login.");
            Toast.makeText(this, "Session expired. Please log in again.", Toast.LENGTH_LONG).show();
            startActivity(new Intent(this, TeacherLogin.class));
            finish();
            return;
        }

        // Set up FAB click listener for notifications
        fabNotifications.setOnClickListener(v -> showNotificationsDialog());

        // Existing code...
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
    }

    @Override
    protected void onResume() {
        super.onResume();
        // Refresh teacherId from SharedPreferences
        SharedPreferences prefs = getSharedPreferences("UserSession", MODE_PRIVATE);
        currentTeacherId = prefs.getString("teacherId", null);

        if (currentTeacherId == null || currentTeacherId.isEmpty()) {
            Log.e(TAG, "onResume: CRITICAL: teacherId missing! Logging out...");
            Toast.makeText(this, "Session expired. Please log in again.", Toast.LENGTH_LONG).show();
            FirebaseAuth.getInstance().signOut();
            startActivity(new Intent(this, TeacherLogin.class));
            finish();
            return;
        }

        Log.d(TAG, "onResume: Notifications initialized with teacherId: " + currentTeacherId);

        // Start real-time notification listeners
        startNotificationBadgeListener();
        startRealtimeNotificationListener();

        // Request notification permissions
        requestNotificationPermission();

        // Refresh teacher data
        getCurrentTeacherData();
    }

    @Override
    protected void onPause() {
        super.onPause();
        // Remove notification listeners to prevent leaks
        if (notificationListener != null && currentTeacherId != null) {
            FirebaseDatabase.getInstance()
                    .getReference("unreadNotifications/teachers/" + currentTeacherId)
                    .removeEventListener(notificationListener);
            notificationListener = null;
        }
        if (realtimeNotifListener != null && currentTeacherId != null) {
            FirebaseDatabase.getInstance()
                    .getReference("notificationsHistory/teachers/" + currentTeacherId)
                    .removeEventListener(realtimeNotifListener);
            realtimeNotifListener = null;
        }
        Log.d(TAG, "onPause: All notification listeners stopped");
    }

    private void requestNotificationPermission() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            if (ContextCompat.checkSelfPermission(this, Manifest.permission.POST_NOTIFICATIONS) !=
                    PackageManager.PERMISSION_GRANTED) {
                ActivityCompat.requestPermissions(this,
                        new String[]{Manifest.permission.POST_NOTIFICATIONS}, 1001);
            }
        }
    }

    private void startNotificationBadgeListener() {
        if (currentTeacherId == null) {
            Log.e(TAG, "startNotificationBadgeListener: Skipped - currentTeacherId is null");
            return;
        }

        DatabaseReference unreadRef = FirebaseDatabase.getInstance()
                .getReference("unreadNotifications/teachers/" + currentTeacherId);

        Log.d(TAG, "startNotificationBadgeListener: Listening for unread count at: " + unreadRef.toString());

        notificationListener = unreadRef.addValueEventListener(new ValueEventListener() {
            @Override
            public void onDataChange(@NonNull DataSnapshot snapshot) {
                Integer count = snapshot.getValue(Integer.class);
                int unreadCount = (count != null) ? count : 0;
                Log.d(TAG, "startNotificationBadgeListener: Unread count updated → " + unreadCount);

                if (unreadCount > 0) {
                    badgeTextView.setText(String.valueOf(unreadCount));
                    badgeTextView.setVisibility(View.VISIBLE);
                } else {
                    badgeTextView.setVisibility(View.GONE);
                }
            }

            @Override
            public void onCancelled(@NonNull DatabaseError error) {
                Log.e(TAG, "startNotificationBadgeListener: Unread count listener cancelled: " + error.getMessage());
                badgeTextView.setVisibility(View.GONE);
            }
        });
    }

    private void startRealtimeNotificationListener() {
        if (currentTeacherId == null) {
            Log.e(TAG, "startRealtimeNotificationListener: Skipped - currentTeacherId is null");
            return;
        }

        DatabaseReference notifRef = FirebaseDatabase.getInstance()
                .getReference("notificationsHistory/teachers/" + currentTeacherId);

        Log.d(TAG, "startRealtimeNotificationListener: Real-time notification listener started at: " + notifRef.toString());

        realtimeNotifListener = notifRef.addValueEventListener(new ValueEventListener() {
            @Override
            public void onDataChange(@NonNull DataSnapshot snapshot) {
                Log.d(TAG, "startRealtimeNotificationListener: Real-time notification update → " + snapshot.getChildrenCount() + " items");
                // Listener keeps the connection alive; dialog will fetch fresh data
            }

            @Override
            public void onCancelled(@NonNull DatabaseError error) {
                Log.e(TAG, "startRealtimeNotificationListener: Realtime notification listener failed: " + error.getMessage());
            }
        });
    }

    private void showNotificationsDialog() {
        Log.d(TAG, "showNotificationsDialog: Opening notifications dialog for teacherId: " + currentTeacherId);
        if (currentTeacherId == null) {
            Log.e(TAG, "showNotificationsDialog: CRITICAL: currentTeacherId is null! Cannot load notifications.");
            Toast.makeText(this, "Session error. Please log in again.", Toast.LENGTH_SHORT).show();
            return;
        }

        clearUnreadCount();

        View dialogView = LayoutInflater.from(this).inflate(R.layout.dialog_notification, null);
        AlertDialog.Builder builder = new AlertDialog.Builder(this, R.style.CustomDialogTheme);
        builder.setView(dialogView);
        AlertDialog dialog = builder.create();

        RecyclerView recyclerView = dialogView.findViewById(R.id.recyclerViewNotifications);
        TextView noNotifText = dialogView.findViewById(R.id.no_notifications_text);
        recyclerView.setLayoutManager(new LinearLayoutManager(this));

        List<Notification> notificationList = new ArrayList<>();
        NotificationAdapter adapter = new NotificationAdapter(this, notificationList, notification -> {
            String eventId = notification.getEventId();
            Log.d(TAG, "showNotificationsDialog: Notification clicked with eventId: " + eventId);
            if (eventId != null && !eventId.isEmpty()) {
                // Check both events and archive_events
                DatabaseReference eventRef = FirebaseDatabase.getInstance().getReference("events").child(eventId);
                DatabaseReference archivedEventRef = FirebaseDatabase.getInstance().getReference("archive_events").child(eventId);

                // Try live events first
                eventRef.addListenerForSingleValueEvent(new ValueEventListener() {
                    @Override
                    public void onDataChange(@NonNull DataSnapshot snapshot) {
                        if (snapshot.exists()) {
                            Event event = snapshot.getValue(Event.class);
                            if (event != null) {
                                startTeacherEventsInside(event, eventId);
                                Log.d(TAG, "showNotificationsDialog: Found event in events: " + event.getEventName());
                            } else {
                                Log.w(TAG, "showNotificationsDialog: Event data is null for eventId: " + eventId);
                                Toast.makeText(TeacherDashboard.this, "Event details not found", Toast.LENGTH_SHORT).show();
                            }
                        } else {
                            // Try archived events
                            archivedEventRef.addListenerForSingleValueEvent(new ValueEventListener() {
                                @Override
                                public void onDataChange(@NonNull DataSnapshot archivedSnapshot) {
                                    if (archivedSnapshot.exists()) {
                                        Event event = archivedSnapshot.getValue(Event.class);
                                        if (event != null) {
                                            startTeacherEventsInside(event, eventId);
                                            Log.d(TAG, "showNotificationsDialog: Found event in archive_events: " + event.getEventName());
                                        } else {
                                            Log.w(TAG, "showNotificationsDialog: Archived event data is null for eventId: " + eventId);
                                            Toast.makeText(TeacherDashboard.this, "Event details not found", Toast.LENGTH_SHORT).show();
                                        }
                                    } else {
                                        Log.w(TAG, "showNotificationsDialog: Event not found in events or archive_events for eventId: " + eventId);
                                        Toast.makeText(TeacherDashboard.this, "Event not found", Toast.LENGTH_SHORT).show();
                                    }
                                }

                                @Override
                                public void onCancelled(@NonNull DatabaseError error) {
                                    Log.e(TAG, "showNotificationsDialog: Failed to fetch archived event details for eventId: " + eventId, error.toException());
                                    Toast.makeText(TeacherDashboard.this, "Error loading event details", Toast.LENGTH_SHORT).show();
                                }
                            });
                        }
                    }

                    @Override
                    public void onCancelled(@NonNull DatabaseError error) {
                        Log.e(TAG, "showNotificationsDialog: Failed to fetch event details for eventId: " + eventId, error.toException());
                        Toast.makeText(TeacherDashboard.this, "Error loading event details", Toast.LENGTH_SHORT).show();
                    }
                });
            } else {
                Log.w(TAG, "showNotificationsDialog: No eventId for notification: " + notification.getTitle());
                Toast.makeText(TeacherDashboard.this, "No event associated with this notification", Toast.LENGTH_SHORT).show();
            }
        });
        recyclerView.setAdapter(adapter);

        DatabaseReference indexRef = FirebaseDatabase.getInstance()
                .getReference("notificationsIndex/teachers/" + currentTeacherId);
        Log.d(TAG, "showNotificationsDialog: Reading notification index from: " + indexRef.toString());

        indexRef.addListenerForSingleValueEvent(new ValueEventListener() {
            @Override
            public void onDataChange(@NonNull DataSnapshot indexSnapshot) {
                Log.d(TAG, "showNotificationsDialog: Index snapshot exists: " + indexSnapshot.exists() +
                        ", children count: " + indexSnapshot.getChildrenCount());

                if (!indexSnapshot.exists() || indexSnapshot.getChildrenCount() == 0) {
                    Log.w(TAG, "showNotificationsDialog: No notifications in index for this teacher.");
                    noNotifText.setVisibility(View.VISIBLE);
                    recyclerView.setVisibility(View.GONE);
                    dialog.show();
                    return;
                }

                List<String> notifIds = new ArrayList<>();
                for (DataSnapshot child : indexSnapshot.getChildren()) {
                    String id = child.getKey();
                    Boolean value = child.getValue(Boolean.class);
                    Log.d(TAG, "showNotificationsDialog: Found notification ID in index → " + id + " (value: " + value + ")");
                    if (id != null) notifIds.add(id);
                }
                Log.d(TAG, "showNotificationsDialog: Total notification IDs found: " + notifIds.size());

                final List<String> finalNotifIds;
                if (notifIds.size() > 100) {
                    finalNotifIds = new ArrayList<>(notifIds.subList(0, 100));
                    Log.d(TAG, "showNotificationsDialog: Limited to 100 notifications.");
                } else {
                    finalNotifIds = new ArrayList<>(notifIds);
                }

                DatabaseReference feedRef = FirebaseDatabase.getInstance().getReference("notificationsFeed");
                List<Task<DataSnapshot>> tasks = new ArrayList<>();

                for (String notifId : finalNotifIds) {
                    DatabaseReference notifPath = feedRef.child(notifId);
                    Log.d(TAG, "showNotificationsDialog: Fetching notification: " + notifPath.toString());
                    tasks.add(notifPath.get());
                }

                Log.d(TAG, "showNotificationsDialog: Starting parallel fetch of " + tasks.size() + " notifications...");

                Tasks.whenAllComplete(tasks).addOnSuccessListener(results -> {
                    Log.d(TAG, "showNotificationsDialog: All notification fetches completed!");
                    notificationList.clear();
                    int successCount = 0;
                    int failedCount = 0;

                    for (int i = 0; i < tasks.size(); i++) {
                        Task<DataSnapshot> task = tasks.get(i);
                        String notifId = finalNotifIds.get(i);

                        if (task.isSuccessful()) {
                            DataSnapshot snap = task.getResult();
                            if (snap.exists()) {
                                Notification n = snap.getValue(Notification.class);
                                if (n != null) {
                                    n.setRead(true);
                                    notificationList.add(n);
                                    successCount++;
                                    Log.d(TAG, "showNotificationsDialog: Loaded: " + n.getTitle() +
                                            " | Type: " + n.getType() + " | EventId: " + n.getEventId());
                                } else {
                                    Log.w(TAG, "showNotificationsDialog: Notification " + notifId + " parsed as NULL");
                                }
                            } else {
                                Log.w(TAG, "showNotificationsDialog: Notification " + notifId + " does NOT exist in feed");
                                failedCount++;
                            }
                        } else {
                            Log.e(TAG, "showNotificationsDialog: Failed to fetch notification " + notifId, task.getException());
                            failedCount++;
                        }
                    }

                    Collections.sort(notificationList, (a, b) -> Long.compare(b.getTimestamp(), a.getTimestamp()));
                    Log.d(TAG, "showNotificationsDialog: Sorted notifications by timestamp (newest first)");

                    Log.d(TAG, "showNotificationsDialog: Final result → Loaded: " + successCount + " | Missing/Failed: " + failedCount);

                    if (notificationList.isEmpty()) {
                        Log.w(TAG, "showNotificationsDialog: No notifications to display after loading.");
                        noNotifText.setVisibility(View.VISIBLE);
                        recyclerView.setVisibility(View.GONE);
                    } else {
                        noNotifText.setVisibility(View.GONE);
                        recyclerView.setVisibility(View.VISIBLE);
                        adapter.updateNotificationList(notificationList);
                        Log.d(TAG, "showNotificationsDialog: RecyclerView updated with " + notificationList.size() + " notifications");
                    }

                    dialog.show();
                    Log.d(TAG, "showNotificationsDialog: Notifications dialog opened successfully");
                }).addOnFailureListener(e -> {
                    Log.e(TAG, "showNotificationsDialog: CRITICAL: All notification loading failed!", e);
                    Toast.makeText(TeacherDashboard.this, "Failed to load notifications", Toast.LENGTH_LONG).show();
                    dialog.dismiss();
                });
            }

            @Override
            public void onCancelled(@NonNull DatabaseError error) {
                Log.e(TAG, "showNotificationsDialog: Index listener cancelled: " + error.getMessage());
                Toast.makeText(TeacherDashboard.this, "Network error", Toast.LENGTH_SHORT).show();
            }
        });

        dialogView.findViewById(R.id.btn_close).setOnClickListener(v -> {
            Log.d(TAG, "showNotificationsDialog: User closed notifications dialog");
            dialog.dismiss();
        });
    }

    private void startTeacherEventsInside(Event event, String eventId) {
        Intent intent = new Intent(TeacherDashboard.this, TeacherEventsInside.class);
        intent.putExtra("eventUID", eventId);
        intent.putExtra("eventName", event.getEventName());
        intent.putExtra("eventDescription", event.getEventDescription());
        intent.putExtra("eventPhotoUrl", event.getEventPhotoUrl());
        intent.putExtra("eventFor", event.getEventFor());
        startActivity(intent);
        Log.d(TAG, "startTeacherEventsInside: Started TeacherEventsInside for event: " + event.getEventName());
    }

    private void clearUnreadCount() {
        if (currentTeacherId != null) {
            FirebaseDatabase.getInstance()
                    .getReference("unreadNotifications/teachers/" + currentTeacherId)
                    .setValue(0);
            Log.d(TAG, "clearUnreadCount: Cleared unread count for teacherId: " + currentTeacherId);
        }
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
                LocalDate currentDate = LocalDate.now(); // Get current date
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
                    // Check if event start date is after the current date
                    boolean isFutureEvent = true;
                    try {
                        LocalDate eventDate = LocalDate.parse(event.getStartDate(), DATE_FORMATTER);
                        if (!eventDate.isAfter(currentDate)) {
                            isFutureEvent = false;
                            Log.d(TAG, "Excluding event on or before current date: " + event.getEventName() +
                                    ", StartDate: " + event.getStartDate());
                        }
                    } catch (Exception e) {
                        Log.e(TAG, "Error parsing event date for filtering: " + event.getEventName(), e);
                    }
                    if (eventFor != null && isFutureEvent) {
                        String normalizedEventFor = eventFor.toLowerCase();
                        if (normalizedEventFor.contains("all") ||
                                normalizedEventFor.contains("everyone") ||
                                normalizedEventFor.contains("teacher")) {
                            shouldAdd = true;
                            Log.d(TAG, "Adding general event: " + event.getEventName() +
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
                                Log.d(TAG, "Adding year-specific event: " + event.getEventName() +
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

                // REMOVED: Toast about "No events found for your advising grade level"
                // if (relevantEvents.isEmpty()) { ... } → Deleted

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
                        Log.d(TAG, "MATCH FOUND! Student: " + studentSnapshot.getKey());
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
        List<Event> eventsOnDate = new ArrayList<>();
        DatabaseReference liveEventsRef = FirebaseDatabase.getInstance().getReference("events");
        DatabaseReference archivedEventsRef = FirebaseDatabase.getInstance().getReference("archive_events");

        ValueEventListener eventListener = new ValueEventListener() {
            @Override
            public void onDataChange(@NonNull DataSnapshot snapshot) {
                for (DataSnapshot dataSnapshot : snapshot.getChildren()) {
                    Event event = dataSnapshot.getValue(Event.class);
                    if (event != null && event.getStartDate() != null && event.getStartDate().equals(selectedDate)) {
                        event.setEventUID(dataSnapshot.getKey());
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
                    }
                }

                // After both live + archived are processed, show dialog
                if (liveEventsRef.equals(snapshot.getRef()) && archivedEventsRef.equals(snapshot.getRef())) {
                    // This won't trigger — we need to track both
                }
            }

            @Override
            public void onCancelled(@NonNull DatabaseError error) {
                Log.e(TAG, "Error fetching events for date: " + error.getMessage());
            }
        };

        // Use a flag to wait for both queries
        final boolean[] completed = {false, false};

        ValueEventListener combinedListener = new ValueEventListener() {
            @Override
            public void onDataChange(@NonNull DataSnapshot snapshot) {
                for (DataSnapshot dataSnapshot : snapshot.getChildren()) {
                    Event event = dataSnapshot.getValue(Event.class);
                    if (event != null && event.getStartDate() != null && event.getStartDate().equals(selectedDate)) {
                        event.setEventUID(dataSnapshot.getKey());
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
                    }
                }

                // Mark this source as done
                if (snapshot.getRef().equals(liveEventsRef)) {
                    completed[0] = true;
                } else if (snapshot.getRef().equals(archivedEventsRef)) {
                    completed[1] = true;
                }

                // When BOTH are done → show dialog
                if (completed[0] && completed[1]) {
                    showEventDialog(TeacherDashboard.this, eventsOnDate, selectedDate);
                }
            }

            @Override
            public void onCancelled(@NonNull DatabaseError error) {
                Log.e(TAG, "Error fetching events for date: " + error.getMessage());
                Toast.makeText(TeacherDashboard.this, "Error loading events", Toast.LENGTH_SHORT).show();
            }
        };

        // Attach listener to both nodes
        liveEventsRef.addListenerForSingleValueEvent(combinedListener);
        archivedEventsRef.addListenerForSingleValueEvent(combinedListener);
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

        // Reference to both live and archived events
        DatabaseReference liveEventsRef = FirebaseDatabase.getInstance().getReference("events");
        DatabaseReference archivedEventsRef = FirebaseDatabase.getInstance().getReference("archive_events");

        // Helper to process events (live or archived)
        ValueEventListener eventProcessor = new ValueEventListener() {
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
                            try {
                                LocalDate date = LocalDate.parse(event.getStartDate(), DATE_FORMATTER);
                                CalendarDay calDay = CalendarDay.from(date.getYear(), date.getMonthValue() - 1, date.getDayOfMonth());
                                String type = event.getEventType() != null ? event.getEventType() : "other";
                                eventTypeMap.put(calDay, type);
                            } catch (Exception e) {
                                Log.e(TAG, "Failed to parse date for archived/live event: " + event.getEventName(), e);
                            }
                        }
                    }
                }

                // After processing both, apply decorators
                if (eventTypeMap.isEmpty()) {
                    Log.d(TAG, "No relevant events (live or archived) to mark on calendar");
                    return;
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
                Log.d(TAG, "Marked " + eventTypeMap.size() + " dates on calendar (live + archived)");
            }

            @Override
            public void onCancelled(@NonNull DatabaseError error) {
                Log.e(TAG, "Error reading events for calendar: " + error.getMessage());
            }
        };

        // Read live events
        liveEventsRef.addListenerForSingleValueEvent(eventProcessor);

        // Read archived events
        archivedEventsRef.addListenerForSingleValueEvent(eventProcessor);
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
}