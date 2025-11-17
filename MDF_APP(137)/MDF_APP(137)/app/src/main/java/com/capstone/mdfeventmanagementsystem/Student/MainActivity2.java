package com.capstone.mdfeventmanagementsystem.Student;

import android.Manifest;
import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.content.pm.PackageManager;
import android.content.res.ColorStateList;
import android.graphics.Bitmap;
import android.graphics.Color;
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

import com.capstone.mdfeventmanagementsystem.Adapters.EventAdapter;
import com.capstone.mdfeventmanagementsystem.Adapters.NotificationAdapter;
import com.capstone.mdfeventmanagementsystem.Models.Notification;
import com.capstone.mdfeventmanagementsystem.R;
import com.capstone.mdfeventmanagementsystem.Utilities.BaseActivity;
import com.google.android.gms.tasks.Task;
import com.google.android.gms.tasks.Tasks;
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
import com.squareup.picasso.Picasso;
import com.squareup.picasso.Transformation;

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
import java.util.concurrent.atomic.AtomicInteger;

import com.google.android.material.badge.BadgeDrawable;
import com.google.android.material.floatingactionbutton.FloatingActionButton;

import com.google.firebase.database.Query;



class CircleTransformMain implements Transformation {
    @Override
    public Bitmap transform(Bitmap source) {
        int size = Math.min(source.getWidth(), source.getHeight());
        int x = (source.getWidth() - size) / 2;
        int y = (source.getHeight() - size) / 2;

        Bitmap squaredBitmap = Bitmap.createBitmap(source, x, y, size, size);
        if (squaredBitmap != source) {
            source.recycle();
        }

        Bitmap bitmap = Bitmap.createBitmap(size, size, source.getConfig());
        android.graphics.Canvas canvas = new android.graphics.Canvas(bitmap);
        android.graphics.Paint paint = new android.graphics.Paint();
        android.graphics.BitmapShader shader = new android.graphics.BitmapShader(
                squaredBitmap, android.graphics.Shader.TileMode.CLAMP,
                android.graphics.Shader.TileMode.CLAMP);

        paint.setShader(shader);
        paint.setAntiAlias(true);

        float r = size / 2f;
        canvas.drawCircle(r, r, r, paint);

        squaredBitmap.recycle();
        return bitmap;
    }

    @Override
    public String key() {
        return "circle";
    }
}

public class MainActivity2 extends BaseActivity {

    private RecyclerView recyclerView;
    private EventAdapter eventAdapter;
    private List<Event> eventList;
    private List<Event> assignedEventList;
    private DatabaseReference databaseReference;
    private DatabaseReference userDatabaseReference;
    private static final DateTimeFormatter DATE_FORMATTER = DateTimeFormatter.ofPattern("yyyy-MM-dd");
    private String currentUserYearLevel;
    private static final String TAG = "MainActivity2Test";
    private TextView firstNameTextView;
    private ImageView profileImageView;
    private DatabaseReference studentsRef;
    private DatabaseReference profilesRef;
    private MaterialCalendarView calendarView;
    private SwipeRefreshLayout swipeRefreshLayout;
    private DatabaseReference notificationsRef;
    private String currentStudentId;
    private TextView badgeTextView;
    private ValueEventListener notificationListener;



    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main2);

        // Initialize Firebase Database references
        studentsRef = FirebaseDatabase.getInstance().getReference().child("students");
        profilesRef = FirebaseDatabase.getInstance().getReference().child("user_profiles");
        notificationsRef = FirebaseDatabase.getInstance().getReference().child("notificationsHistory/students");

        // Initialize badge for fab_notifications
        FloatingActionButton fabNotifications = findViewById(R.id.fab_notifications);
        badgeTextView = findViewById(R.id.notification_badge);

        // Initialize currentStudentId from SharedPreferences
        SharedPreferences prefs = getSharedPreferences("UserSession", MODE_PRIVATE);
        currentStudentId = prefs.getString("studentID", null);
        Log.d(TAG, "Initialized currentStudentId from SharedPreferences: " + currentStudentId);

        if (currentStudentId == null || currentStudentId.isEmpty()) {
            Log.e(TAG, "No studentID in SharedPreferences. Redirecting to login.");
            Toast.makeText(this, "Session expired. Please log in again.", Toast.LENGTH_LONG).show();
            startActivity(new Intent(this, StudentLogin.class));
            finish();
            return;
        }

        loadCachedProfileImage();

        // Initialize the TextView for firstName and ImageView for profile picture
        firstNameTextView = findViewById(R.id.firstName);
        profileImageView = findViewById(R.id.profile_image);



        loadCachedProfileImage();

        // Initialize the TextView for firstName and ImageView for profile picture
        firstNameTextView = findViewById(R.id.firstName);
        profileImageView = findViewById(R.id.profile_image);

        // Initialize calendar view
        calendarView = findViewById(R.id.calendarView);
        if (calendarView != null) {
            calendarView.setOnDateChangedListener((widget, date, selected) -> {
                LocalDate selectedDate = LocalDate.of(date.getYear(), date.getMonth() + 1, date.getDay());
                String formattedDate = selectedDate.format(DATE_FORMATTER);
                fetchEventsForDate(formattedDate);
            });
        }

        findViewById(R.id.fab_notifications).setOnClickListener(v -> showNotificationsDialog());

        // Initialize SwipeRefreshLayout
        swipeRefreshLayout = findViewById(R.id.swipeRefreshLayout);
        swipeRefreshLayout.setColorSchemeResources(R.color.primary);
        swipeRefreshLayout.setOnRefreshListener(() -> {
            if (isNetworkAvailable()) {
                Log.d(TAG, "Swipe refresh triggered with network available");
                getCurrentUserData();
            } else {
                Log.d(TAG, "Swipe refresh triggered but no network available");
                Toast.makeText(this, "No network available", Toast.LENGTH_SHORT).show();
                swipeRefreshLayout.setRefreshing(false);
            }
        });

        findViewById(R.id.fab_scan).setOnClickListener(v -> {
            startActivity(new Intent(getApplicationContext(), QRCheckInActivity.class));
            overridePendingTransition(100, 100);
        });

        BottomAppBar bottomAppBar = findViewById(R.id.bottomAppBar);
        bottomAppBar.setBackgroundTint(ColorStateList.valueOf(Color.WHITE));

        profileImageView.setOnClickListener(v -> {
            Intent intent = new Intent(MainActivity2.this, ProfileActivity.class);
            startActivity(intent);
        });

        MaterialButton btnViewAllUpcoming = findViewById(R.id.btnViewAllUpcoming);
        btnViewAllUpcoming.setOnClickListener(v -> {
            Intent intent = new Intent(MainActivity2.this, StudentDashboard.class);
            startActivity(intent);
            overridePendingTransition(0, 0);

        });



        // Setup RecyclerView for upcoming events
        recyclerView = findViewById(R.id.recyclerViewEvents);
        recyclerView.setLayoutManager(new LinearLayoutManager(this));
        eventList = new ArrayList<>();
        eventAdapter = new EventAdapter(this, eventList, event -> navigateToEventDetails(event));
        recyclerView.setAdapter(eventAdapter);

        // Initialize assigned event list for calendar only
        assignedEventList = new ArrayList<>();

        // Firebase reference to "events"
        databaseReference = FirebaseDatabase.getInstance().getReference("events");

        // Get current user's data
        getCurrentUserData();

        // Load profile image
        loadUserProfile();

        // Setup bottom navigation
        BottomNavigationView bottomNavigationView = findViewById(R.id.bottom_navigation);
        bottomNavigationView.setSelectedItemId(R.id.nav_home);
        bottomNavigationView.setBackground(null);

        bottomNavigationView.setOnNavigationItemSelectedListener(item -> {
            int itemId = item.getItemId();
            if (itemId == R.id.nav_home) {
                return true;
            } else if (itemId == R.id.nav_event) {
                startActivity(new Intent(getApplicationContext(), StudentDashboard.class));
            } else if (itemId == R.id.nav_ticket) {
                startActivity(new Intent(getApplicationContext(), StudentTickets.class));
            } else if (itemId == R.id.nav_cert) {
                startActivity(new Intent(getApplicationContext(), StudentCertificate.class));
            }
            overridePendingTransition(0, 0);
            return true;
        });
    }

    private boolean isNetworkAvailable() {
        android.net.ConnectivityManager connectivityManager = (android.net.ConnectivityManager) getSystemService(Context.CONNECTIVITY_SERVICE);
        return connectivityManager != null && connectivityManager.getActiveNetworkInfo() != null && connectivityManager.getActiveNetworkInfo().isConnected();
    }


    @Override
    protected void onResume() {
        super.onResume();
        loadUserProfile();
        requestNotificationPermission();

        // Get studentID from SharedPreferences
        SharedPreferences prefs = getSharedPreferences("UserSession", MODE_PRIVATE);
        currentStudentId = prefs.getString("studentID", null);

        if (currentStudentId == null || currentStudentId.isEmpty()) {
            Log.e(TAG, "CRITICAL: studentID missing! Logging out...");
            Toast.makeText(this, "Session expired. Please log in again.", Toast.LENGTH_LONG).show();
            FirebaseAuth.getInstance().signOut();
            startActivity(new Intent(this, StudentLogin.class));
            finish();
            return;
        }

        Log.d(TAG, "Notifications initialized with studentID: " + currentStudentId);

        // Start real-time listeners
        startNotificationBadgeListener();   // Real-time badge
        startRealtimeNotificationListener(); // Real-time dialog content

        // Refresh user data to ensure consistency
        getCurrentUserData();
    }

    // 3. REPLACE startNotificationBadgeListener() WITH THIS (unchanged path — still correct!)
    private void startNotificationBadgeListener() {
        if (currentStudentId == null) return;

        DatabaseReference unreadRef = FirebaseDatabase.getInstance()
                .getReference("unreadNotifications/students/" + currentStudentId);

        Log.d(TAG, "Badge listener → unreadNotifications/students/" + currentStudentId);

        notificationListener = unreadRef.addValueEventListener(new ValueEventListener() {
            @Override
            public void onDataChange(@NonNull DataSnapshot snapshot) {
                Integer count = snapshot.getValue(Integer.class);
                int unreadCount = (count != null) ? count : 0;

                if (unreadCount > 0) {
                    badgeTextView.setText(String.valueOf(unreadCount));
                    badgeTextView.setVisibility(View.VISIBLE);
                } else {
                    badgeTextView.setVisibility(View.GONE);
                }
            }

            @Override
            public void onCancelled(@NonNull DatabaseError error) {
                badgeTextView.setVisibility(View.GONE);
            }
        });
    }

    private ValueEventListener realtimeNotifListener; // Separate listener

    private void startRealtimeNotificationListener() {
        if (currentStudentId == null) return;

        DatabaseReference notifRef = FirebaseDatabase.getInstance()
                .getReference("notificationsHistory/students/" + currentStudentId);

        Log.d(TAG, "Real-time notification list listener STARTED → " + notifRef.toString());

        realtimeNotifListener = notifRef.addValueEventListener(new ValueEventListener() {
            @Override
            public void onDataChange(@NonNull DataSnapshot snapshot) {
                Log.d(TAG, "Real-time notification update → " + snapshot.getChildrenCount() + " items");

                // You can optionally store this in a global list if other parts need it
                // For now, we just know it's live — dialog will reload fresh every time
            }

            @Override
            public void onCancelled(@NonNull DatabaseError error) {
                Log.e(TAG, "Realtime notification listener failed: " + error.getMessage());
            }
        });
    }

    @Override
    protected void onPause() {
        super.onPause();
        // Remove both listeners to prevent leaks
        if (notificationListener != null) {
            FirebaseDatabase.getInstance()
                    .getReference("unreadNotifications/students/" + currentStudentId)
                    .removeEventListener(notificationListener);
            notificationListener = null;
        }
        if (realtimeNotifListener != null) {
            FirebaseDatabase.getInstance()
                    .getReference("notificationsHistory/students/" + currentStudentId)
                    .removeEventListener(realtimeNotifListener);
            realtimeNotifListener = null;
        }
        Log.d(TAG, "All notification listeners stopped");
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



    private void loadCachedProfileImage() {
        SharedPreferences prefs = getSharedPreferences("ProfileImageCache", MODE_PRIVATE);
        String cachedImageUrl = prefs.getString("profileImageUrl", "");
        if (!cachedImageUrl.isEmpty()) {
            loadProfileImageFromCache(cachedImageUrl);
        } else {
            setDefaultProfileImage();
        }
    }

    private void loadProfileImageFromCache(String imageUrl) {
        if (profileImageView == null) {
            return;
        }
        Picasso.get()
                .load(imageUrl)
                .transform(new CircleTransformMain())
                .placeholder(R.drawable.profile_placeholder)
                .error(R.drawable.profile_placeholder)
                .networkPolicy(com.squareup.picasso.NetworkPolicy.OFFLINE)
                .into(profileImageView, new com.squareup.picasso.Callback() {
                    @Override
                    public void onSuccess() {
                        Log.d(TAG, "Profile image loaded from cache");
                    }
                    @Override
                    public void onError(Exception e) {
                        setDefaultProfileImage();
                    }
                });
    }

    private void cacheProfileImageUrl(String imageUrl) {
        SharedPreferences prefs = getSharedPreferences("ProfileImageCache", MODE_PRIVATE);
        SharedPreferences.Editor editor = prefs.edit();
        editor.putString("profileImageUrl", imageUrl);
        editor.apply();
    }

    private void loadUserProfile() {
        FirebaseUser user = FirebaseAuth.getInstance().getCurrentUser();
        if (user != null) {
            checkProfileImage(user.getUid());
        }
    }

    private void fetchEventsForDate(String selectedDate) {
        List<Event> eventsOnDate = new ArrayList<>();
        DatabaseReference liveRef = FirebaseDatabase.getInstance().getReference("events");
        DatabaseReference archiveRef = FirebaseDatabase.getInstance().getReference("archive_events");

        ValueEventListener listener = new ValueEventListener() {
            @Override
            public void onDataChange(@NonNull DataSnapshot snapshot) {
                for (DataSnapshot ds : snapshot.getChildren()) {
                    Event event = ds.getValue(Event.class);
                    if (event != null && selectedDate.equals(event.getStartDate())) {
                        event.setEventUID(ds.getKey());

                        String eventFor = event.getEventFor();
                        boolean isRelevant = false;
                        if (eventFor != null) {
                            String normalized = eventFor.toLowerCase().replace("-", "").trim();
                            String yearLevel = currentUserYearLevel.toLowerCase().replace("-", "").trim();
                            String gradeNum = yearLevel.replaceAll("[^0-9]", "");

                            if (normalized.contains("all") || normalized.contains("everyone")) {
                                isRelevant = true;
                            } else if (normalized.contains(yearLevel) ||
                                    normalized.contains("grade" + gradeNum) ||
                                    normalized.contains(gradeNum)) {
                                isRelevant = true;
                            }
                        }

                        if (isRelevant || eventFor == null) {
                            eventsOnDate.add(event);
                        }
                    }
                }

                // After both queries complete
                if (snapshot.getRef().equals(liveRef) || snapshot.getRef().equals(archiveRef)) {
                    // We rely on second call to trigger show (simple way)
                    if (eventsOnDate.size() > 0 || snapshot.getRef().equals(archiveRef)) {
                        showEventDialog(MainActivity2.this, eventsOnDate, selectedDate);
                    }
                }
            }

            @Override
            public void onCancelled(@NonNull DatabaseError error) {
                Toast.makeText(MainActivity2.this, "Error loading events", Toast.LENGTH_SHORT).show();
            }
        };

        // Simple dual fetch — second one triggers display
        final AtomicInteger completed = new AtomicInteger(0);
        ValueEventListener dualListener = new ValueEventListener() {
            @Override
            public void onDataChange(@NonNull DataSnapshot snapshot) {
                for (DataSnapshot ds : snapshot.getChildren()) {
                    Event event = ds.getValue(Event.class);
                    if (event != null && selectedDate.equals(event.getStartDate())) {
                        event.setEventUID(ds.getKey());
                        // Relevance check same as above
                        String eventFor = event.getEventFor();
                        boolean isRelevant = false;
                        if (eventFor != null) {
                            String normalized = eventFor.toLowerCase().replace("-", "").trim();
                            String yearLevelKey = currentUserYearLevel.toLowerCase().replaceAll("[^0-9a-z]", "");
                            if (normalized.contains("all") || normalized.contains("everyone")) {
                                isRelevant = true;
                            } else if (normalized.contains(yearLevelKey) || normalized.matches(".*\\b" + yearLevelKey + "\\b.*")) {
                                isRelevant = true;
                            }
                        } else {
                            isRelevant = true;
                        }
                        if (isRelevant) eventsOnDate.add(event);
                    }
                }

                if (completed.incrementAndGet() == 2) {
                    showEventDialog(MainActivity2.this, eventsOnDate, selectedDate);
                }
            }

            @Override
            public void onCancelled(@NonNull DatabaseError error) {
                if (completed.incrementAndGet() == 2) {
                    Toast.makeText(MainActivity2.this, "Error loading events", Toast.LENGTH_SHORT).show();
                }
            }
        };

        liveRef.addListenerForSingleValueEvent(dualListener);
        archiveRef.addListenerForSingleValueEvent(dualListener);
    }

    private void showEventDialog(MainActivity2 context, List<Event> events, String selectedDate) {
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

            if (eventContainer != null) {
                eventContainer.setOnClickListener(v -> {
                    navigateToEventDetails(event);
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

    private void setEventTypeStyle(MainActivity2 context, String eventType, View lineView, ImageView badgeIcon) {
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

    private void checkProfileImage(String uid) {
        profilesRef.child(uid).addListenerForSingleValueEvent(new ValueEventListener() {
            @Override
            public void onDataChange(@NonNull DataSnapshot dataSnapshot) {
                if (dataSnapshot.exists() && dataSnapshot.hasChild("profileImage")) {
                    String profileImageUrl = dataSnapshot.child("profileImage").getValue(String.class);
                    if (profileImageUrl != null && !profileImageUrl.isEmpty()) {
                        Log.d(TAG, "Found profile image in user_profiles: " + profileImageUrl);
                        loadProfileImage(profileImageUrl);
                    } else {
                        Log.d(TAG, "Profile image field empty in user_profiles");
                        setDefaultProfileImage();
                    }
                } else {
                    Log.d(TAG, "No profile image found in user_profiles");
                    setDefaultProfileImage();
                }
            }

            @Override
            public void onCancelled(@NonNull DatabaseError error) {
                Log.e(TAG, "Error checking user_profiles: " + error.getMessage());
                setDefaultProfileImage();
            }
        });
    }

    private void loadProfileImage(String imageUrl) {
        if (profileImageView == null) {
            Log.e(TAG, "Cannot load profile image: ImageView is null");
            return;
        }
        if (imageUrl != null && !imageUrl.isEmpty()) {
            Log.d(TAG, "Loading profile image from: " + imageUrl);
            Picasso.get()
                    .load(imageUrl)
                    .transform(new CircleTransformMain())
                    .placeholder(R.drawable.profile_placeholder)
                    .error(R.drawable.profile_placeholder)
                    .into(profileImageView, new com.squareup.picasso.Callback() {
                        @Override
                        public void onSuccess() {
                            Log.d(TAG, "Profile image loaded successfully");
                            cacheProfileImageUrl(imageUrl);
                        }
                        @Override
                        public void onError(Exception e) {
                            Log.e(TAG, "Error loading profile image: " + (e != null ? e.getMessage() : "unknown error"));
                        }
                    });
        }
    }

    private void setDefaultProfileImage() {
        if (profileImageView != null) {
            profileImageView.setImageResource(R.drawable.profile_placeholder);
        }
    }

    private void navigateToEventDetails(Event event) {
        Log.d(TAG, "Navigating to StudentDashboardInside for event: " + event.getEventName());
        Intent intent = new Intent(MainActivity2.this, StudentDashboardInside.class);
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
        overridePendingTransition(0, 0);
    }

    private void getCurrentUserData() {
        FirebaseUser currentUser = FirebaseAuth.getInstance().getCurrentUser();
        if (currentUser != null) {
            String userEmail = currentUser.getEmail();
            userDatabaseReference = FirebaseDatabase.getInstance().getReference("students");
            userDatabaseReference.orderByChild("email").equalTo(userEmail).addListenerForSingleValueEvent(new ValueEventListener() {
                @Override
                public void onDataChange(@NonNull DataSnapshot snapshot) {
                    if (snapshot.exists()) {
                        for (DataSnapshot userSnapshot : snapshot.getChildren()) {
                            Student student = userSnapshot.getValue(Student.class);
                            if (student != null) {
                                currentStudentId = userSnapshot.getKey(); // Use database key (e.g., -Ocjk4G6KT8sfI8xUxLu)
                                currentUserYearLevel = student.getYearLevel();
                                Log.d(TAG, "Retrieved student data - Database Key: " + currentStudentId +
                                        ", IDNumber: " + student.getIdNumber() +
                                        ", Name: " + student.getFirstName() + " " + student.getLastName() +
                                        ", Year Level: " + currentUserYearLevel);

                                // Update SharedPreferences with studentID and yearLevel
                                updateSharedPreferences(currentStudentId, student.getYearLevel());

                                String firstName = student.getFirstName();
                                firstNameTextView.setText("Hello, " + firstName + "!");
                                fetchUpcomingEvents();
                                fetchAssignedEvents();
                            }
                        }
                    } else {
                        Log.e(TAG, "No student record found for email: " + userEmail);
                        Toast.makeText(MainActivity2.this, "No student record found!", Toast.LENGTH_SHORT).show();
                        fetchUpcomingEvents();
                    }
                    Log.d(TAG, "Stopping SwipeRefreshLayout in getCurrentUserData onDataChange");
                    swipeRefreshLayout.setRefreshing(false);
                }

                @Override
                public void onCancelled(@NonNull DatabaseError error) {
                    Log.e(TAG, "Error fetching student data: " + error.getMessage());
                    Toast.makeText(MainActivity2.this, "Error fetching student data", Toast.LENGTH_SHORT).show();
                    fetchUpcomingEvents();
                    Log.d(TAG, "Stopping SwipeRefreshLayout in getCurrentUserData onCancelled");
                    swipeRefreshLayout.setRefreshing(false);
                }
            });
        } else {
            Log.e(TAG, "No user is currently logged in");
            Toast.makeText(this, "No user logged in", Toast.LENGTH_SHORT).show();
            fetchUpcomingEvents();
            Log.d(TAG, "Stopping SwipeRefreshLayout in getCurrentUserData no user");
            swipeRefreshLayout.setRefreshing(false);
        }
    }

    private void updateSharedPreferences(String studentId, String yearLevel) {
        SharedPreferences prefs = getSharedPreferences("UserSession", MODE_PRIVATE);
        SharedPreferences.Editor editor = prefs.edit();
        editor.putString("studentID", studentId);
        editor.putString("yearLevel", yearLevel);
        editor.apply();
        Log.d(TAG, "Updated SharedPreferences - studentID: " + studentId + ", yearLevel: " + yearLevel);
    }

    private void startNotificationListener() {
        if (currentStudentId == null) {
            Log.e(TAG, "startNotificationListener() skipped - currentStudentId is NULL");
            return;
        }

        DatabaseReference unreadRef = FirebaseDatabase.getInstance()
                .getReference("unreadNotifications/students/" + currentStudentId); // ← Now correct!

        Log.d(TAG, "Listening for unread count at: unreadNotifications/students/" + currentStudentId);

        notificationListener = unreadRef.addValueEventListener(new ValueEventListener() {
            @Override
            public void onDataChange(@NonNull DataSnapshot snapshot) {
                Integer count = snapshot.getValue(Integer.class);
                int unreadCount = (count != null) ? count : 0;

                Log.d(TAG, "Unread count updated → " + unreadCount + " (from Firebase)");

                if (unreadCount > 0) {
                    badgeTextView.setText(String.valueOf(unreadCount));
                    badgeTextView.setVisibility(View.VISIBLE);
                } else {
                    badgeTextView.setVisibility(View.GONE);
                }
            }

            @Override
            public void onCancelled(@NonNull DatabaseError error) {
                Log.e(TAG, "Unread count listener cancelled: " + error.getMessage());
                badgeTextView.setVisibility(View.GONE);
            }
        });
    }

    private void stopNotificationListener() {
        if (notificationListener != null && currentStudentId != null) {
            notificationsRef.child(currentStudentId).removeEventListener(notificationListener);
            notificationListener = null;
            Log.d(TAG, "Stopped notification listener");
        }
    }

    // 2. REPLACE THE ENTIRE showNotificationsDialog() METHOD WITH THIS ONE
    private void showNotificationsDialog() {
        Log.d(TAG, "showNotificationsDialog: Opening notifications dialog for studentId: " + currentStudentId);
        if (currentStudentId == null) {
            Log.e(TAG, "showNotificationsDialog: CRITICAL: currentStudentId is null! Cannot load notifications.");
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
                            Intent intent = new Intent(MainActivity2.this, StudentDashboardInside.class);
                            intent.putExtra("eventUID", eventId);
                            startActivity(intent);
                            Log.d(TAG, "showNotificationsDialog: Started StudentDashboardInside for eventId: " + eventId);
                        } else {
                            // Try archived events
                            archivedEventRef.addListenerForSingleValueEvent(new ValueEventListener() {
                                @Override
                                public void onDataChange(@NonNull DataSnapshot archivedSnapshot) {
                                    if (archivedSnapshot.exists()) {
                                        Intent intent = new Intent(MainActivity2.this, StudentDashboardInside.class);
                                        intent.putExtra("eventUID", eventId);
                                        startActivity(intent);
                                        Log.d(TAG, "showNotificationsDialog: Started StudentDashboardInside for archived eventId: " + eventId);
                                    } else {
                                        Log.w(TAG, "showNotificationsDialog: Event not found in events or archive_events for eventId: " + eventId);
                                        Toast.makeText(MainActivity2.this, "Event not found", Toast.LENGTH_SHORT).show();
                                    }
                                }

                                @Override
                                public void onCancelled(@NonNull DatabaseError error) {
                                    Log.e(TAG, "showNotificationsDialog: Failed to fetch archived event details for eventId: " + eventId, error.toException());
                                    Toast.makeText(MainActivity2.this, "Error loading event details", Toast.LENGTH_SHORT).show();
                                }
                            });
                        }
                    }

                    @Override
                    public void onCancelled(@NonNull DatabaseError error) {
                        Log.e(TAG, "showNotificationsDialog: Failed to fetch event details for eventId: " + eventId, error.toException());
                        Toast.makeText(MainActivity2.this, "Error loading event details", Toast.LENGTH_SHORT).show();
                    }
                });
            } else {
                Log.w(TAG, "showNotificationsDialog: No eventId for notification: " + notification.getTitle());
                Toast.makeText(MainActivity2.this, "No event associated with this notification", Toast.LENGTH_SHORT).show();
            }
        });
        recyclerView.setAdapter(adapter);

        DatabaseReference indexRef = FirebaseDatabase.getInstance()
                .getReference("notificationsIndex/students/" + currentStudentId);
        Log.d(TAG, "showNotificationsDialog: Reading notification index from: " + indexRef.toString());

        indexRef.addListenerForSingleValueEvent(new ValueEventListener() {
            @Override
            public void onDataChange(@NonNull DataSnapshot indexSnapshot) {
                Log.d(TAG, "showNotificationsDialog: Index snapshot exists: " + indexSnapshot.exists() +
                        ", children count: " + indexSnapshot.getChildrenCount());

                if (!indexSnapshot.exists() || indexSnapshot.getChildrenCount() == 0) {
                    Log.w(TAG, "showNotificationsDialog: No notifications in index for this student.");
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
                    Toast.makeText(MainActivity2.this, "Failed to load notifications", Toast.LENGTH_LONG).show();
                    dialog.dismiss();
                });
            }

            @Override
            public void onCancelled(@NonNull DatabaseError error) {
                Log.e(TAG, "showNotificationsDialog: Index listener cancelled: " + error.getMessage());
                Toast.makeText(MainActivity2.this, "Network error", Toast.LENGTH_SHORT).show();
            }
        });

        dialogView.findViewById(R.id.btn_close).setOnClickListener(v -> {
            Log.d(TAG, "showNotificationsDialog: User closed notifications dialog");
            dialog.dismiss();
        });
    }

    private void clearUnreadCount() {
        if (currentStudentId != null) {
            FirebaseDatabase.getInstance()
                    .getReference("unreadNotifications/students/" + currentStudentId)
                    .setValue(0);
            Log.d(TAG, "Cleared unread count for studentID: " + currentStudentId);
        }
    }

    private void fetchUpcomingEvents() {
        databaseReference.addValueEventListener(new ValueEventListener() {
            @Override
            public void onDataChange(@NonNull DataSnapshot snapshot) {
                eventList.clear();
                LocalDate today = LocalDate.now();
                LocalDate maxDate = today.plusDays(7);

                for (DataSnapshot dataSnapshot : snapshot.getChildren()) {
                    Event event = dataSnapshot.getValue(Event.class);
                    if (event != null) {
                        event.setEventUID(dataSnapshot.getKey());
                        try {
                            LocalDate eventDate = LocalDate.parse(event.getStartDate(), DATE_FORMATTER);
                            // Only include events after the current date
                            if (eventDate.isAfter(today) && !eventDate.isAfter(maxDate)) {
                                eventList.add(event);
                                Log.d(TAG, "Added event: " + event.getEventName() + ", Date: " + event.getStartDate());
                            } else {
                                Log.d(TAG, "Excluding event: " + event.getEventName() + ", Date: " + event.getStartDate() +
                                        " (either on or before today or after max date)");
                            }
                        } catch (Exception e) {
                            Log.e(TAG, "Error parsing event date for event " + event.getEventUID(), e);
                        }
                    }
                }

                Collections.sort(eventList, (e1, e2) -> {
                    try {
                        LocalDate date1 = LocalDate.parse(e1.getStartDate(), DATE_FORMATTER);
                        LocalDate date2 = LocalDate.parse(e2.getStartDate(), DATE_FORMATTER);
                        return date1.compareTo(date2);
                    } catch (Exception e) {
                        Log.e(TAG, "Error sorting events", e);
                        return 0;
                    }
                });

                if (eventList.size() > 3) {
                    eventList = new ArrayList<>(eventList.subList(0, 3));
                }

                for (Event event : eventList) {
                    Log.d(TAG, "Displaying event: " + event.getEventName() + ", Date: " + event.getStartDate());
                }

                eventAdapter.updateEventList(eventList);
                Log.d(TAG, "Fetched " + eventList.size() + " upcoming events (limited to 3)");
                swipeRefreshLayout.setRefreshing(false);
            }

            @Override
            public void onCancelled(@NonNull DatabaseError error) {
                Log.e(TAG, "Firebase data fetch failed: " + error.getMessage(), error.toException());
                swipeRefreshLayout.setRefreshing(false);
            }
        });
    }

    // FIXED & FINAL: Calendar dots now NEVER disappear (includes today + archived)
    private void fetchAssignedEvents() {
        if (currentUserYearLevel == null || currentUserYearLevel.isEmpty()) {
            Log.e(TAG, "Year level not available");
            swipeRefreshLayout.setRefreshing(false);
            return;
        }

        DatabaseReference liveRef = FirebaseDatabase.getInstance().getReference("events");
        DatabaseReference archiveRef = FirebaseDatabase.getInstance().getReference("archive_events");

        Set<CalendarDay> allEventDates = new HashSet<>();
        Map<CalendarDay, String> eventTypeMap = new HashMap<>();

        String yearLevel = currentUserYearLevel.toLowerCase().trim();
        String gradeNum = yearLevel.replaceAll("[^0-9]", "");

        List<String> possibleFormats = new ArrayList<>();
        possibleFormats.add(yearLevel);
        possibleFormats.add("grade" + gradeNum);
        possibleFormats.add("grade " + gradeNum);
        possibleFormats.add("g" + gradeNum);
        possibleFormats.add("g " + gradeNum);
        possibleFormats.add(gradeNum);

        AtomicInteger completed = new AtomicInteger(0);
        ValueEventListener listener = new ValueEventListener() {
            @Override
            public void onDataChange(@NonNull DataSnapshot snapshot) {
                for (DataSnapshot ds : snapshot.getChildren()) {
                    Event event = ds.getValue(Event.class);
                    if (event != null && event.getStartDate() != null) {
                        event.setEventUID(ds.getKey());

                        String eventFor = event.getEventFor();
                        boolean isRelevant = false;

                        if (eventFor != null) {
                            String normalized = eventFor.toLowerCase().replace("-", "").trim();
                            if (normalized.contains("all") || normalized.contains("everyone") || normalized.contains("allyear")) {
                                isRelevant = true;
                            } else {
                                for (String format : possibleFormats) {
                                    String nf = format.toLowerCase().replace("-", "").trim();
                                    if (normalized.contains(nf) || nf.contains(normalized)) {
                                        isRelevant = true;
                                        break;
                                    }
                                }
                            }
                        } else {
                            isRelevant = true;
                        }

                        if (isRelevant) {
                            try {
                                LocalDate date = LocalDate.parse(event.getStartDate(), DATE_FORMATTER);
                                CalendarDay calDay = CalendarDay.from(date.getYear(), date.getMonthValue() - 1, date.getDayOfMonth());

                                allEventDates.add(calDay);
                                String type = event.getEventType() != null ? event.getEventType() : "other";
                                eventTypeMap.putIfAbsent(calDay, type); // First event type wins
                            } catch (Exception e) {
                                Log.e(TAG, "Date parse error: " + e.getMessage());
                            }
                        }
                    }
                }

                // When BOTH live + archive are done → update calendar
                if (completed.incrementAndGet() == 2) {
                    markEventDatesOnCalendar(allEventDates, eventTypeMap);
                    Log.d(TAG, "Calendar fully updated with " + allEventDates.size() + " dates (live + archived)");
                    swipeRefreshLayout.setRefreshing(false);
                }
            }

            @Override
            public void onCancelled(@NonNull DatabaseError error) {
                if (completed.incrementAndGet() == 2) {
                    swipeRefreshLayout.setRefreshing(false);
                }
                Log.e(TAG, "Error loading events for calendar: " + error.getMessage());
            }
        };

        // CRITICAL: Use SINGLE value event, NOT continuous
        liveRef.addListenerForSingleValueEvent(listener);
        archiveRef.addListenerForSingleValueEvent(listener);
    }

    private void markEventDatesOnCalendar(Set<CalendarDay> eventDates, Map<CalendarDay, String> eventTypeMap) {
        if (calendarView == null) return;

        calendarView.removeDecorators();

        for (Map.Entry<CalendarDay, String> entry : eventTypeMap.entrySet()) {
            CalendarDay day = entry.getKey();
            String eventType = entry.getValue();
            int colorRes = getEventTypeColor(eventType);

            DayViewDecorator decorator = new DayViewDecorator() {
                @Override
                public boolean shouldDecorate(CalendarDay calendarDay) {
                    return calendarDay.equals(day);
                }

                @Override
                public void decorate(DayViewFacade view) {
                    view.addSpan(new DotSpan(8, ContextCompat.getColor(MainActivity2.this, colorRes)));
                }
            };
            calendarView.addDecorator(decorator);
        }
        calendarView.invalidateDecorators();
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