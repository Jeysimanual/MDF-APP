package com.capstone.mdfeventmanagementsystem.Student;

import android.content.Intent;
import android.content.SharedPreferences;
import android.content.res.ColorStateList;
import android.graphics.Bitmap;
import android.graphics.Color;
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

import com.capstone.mdfeventmanagementsystem.Adapters.EventAdapter;
import com.capstone.mdfeventmanagementsystem.R;
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
    private static final String TAG = "MainActivity2";
    private TextView firstNameTextView;
    private ImageView profileImageView;
    private DatabaseReference studentsRef;
    private DatabaseReference profilesRef;
    private MaterialCalendarView calendarView;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main2);

        // Initialize Firebase Database references
        studentsRef = FirebaseDatabase.getInstance().getReference().child("students");
        profilesRef = FirebaseDatabase.getInstance().getReference().child("user_profiles");

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

        findViewById(R.id.fab_scan).setOnClickListener(v -> {
            startActivity(new Intent(getApplicationContext(), QRCheckInActivity.class));
            overridePendingTransition(0, 0);
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

    @Override
    protected void onResume() {
        super.onResume();
        loadUserProfile();
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
        databaseReference.addListenerForSingleValueEvent(new ValueEventListener() {
            @Override
            public void onDataChange(@NonNull DataSnapshot snapshot) {
                List<Event> eventsOnDate = new ArrayList<>();
                for (DataSnapshot dataSnapshot : snapshot.getChildren()) {
                    Event event = dataSnapshot.getValue(Event.class);
                    if (event != null) {
                        event.setEventUID(dataSnapshot.getKey());
                        if (event.getStartDate() != null && event.getStartDate().equals(selectedDate)) {
                            eventsOnDate.add(event);
                        } else if (event.getStartDate() == null) {
                            Log.w(TAG, "Event with UID: " + dataSnapshot.getKey() + " has null startDate");
                        }
                    } else {
                        Log.w(TAG, "Failed to parse event for UID: " + dataSnapshot.getKey());
                    }
                }
                showEventDialog(MainActivity2.this, eventsOnDate, selectedDate);
            }

            @Override
            public void onCancelled(@NonNull DatabaseError error) {
                Log.e(TAG, "Error fetching events for date: " + error.getMessage());
                Toast.makeText(MainActivity2.this, "Error fetching events", Toast.LENGTH_SHORT).show();
            }
        });
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

        if (!events.isEmpty()) {
            Event event = events.get(0);
            TextView eventName = dialogView.findViewById(R.id.event_name);
            TextView eventFor = dialogView.findViewById(R.id.event_for);
            TextView eventTime = dialogView.findViewById(R.id.event_time);
            View eventTypeLine = dialogView.findViewById(R.id.event_type_line);
            LinearLayout badgeContainer = dialogView.findViewById(R.id.event_badge);
            ImageView badgeIcon = dialogView.findViewById(R.id.badge_icon);

            String eventNameText = event.getEventName() != null ? event.getEventName() : "Unnamed Event";
            if (eventNameText.length() > 20) {
                eventNameText = eventNameText.substring(0, 17) + "...";
            }
            eventName.setText(eventNameText);
            eventFor.setText("For: " + (event.getEventFor() != null ? event.getEventFor() : "All"));
            String timeText = formatEventTime(event.getStartTime(), event.getEndTime());
            eventTime.setText(timeText);

            setEventTypeStyle(context, event.getEventType(), eventTypeLine, badgeIcon);
        }

        AlertDialog.Builder builder = new AlertDialog.Builder(context, R.style.CustomDialogTheme);
        builder.setView(dialogView);
        builder.setCancelable(true);

        ImageButton btnClose = dialogView.findViewById(R.id.btn_close);
        AlertDialog dialog = builder.create();
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
                                currentUserYearLevel = student.getYearLevel();
                                Log.d(TAG, "Retrieved student data - ID: " + student.getIdNumber() +
                                        ", Name: " + student.getFirstName() + " " + student.getLastName() +
                                        ", Year Level: " + currentUserYearLevel);
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
                }

                @Override
                public void onCancelled(@NonNull DatabaseError error) {
                    Log.e(TAG, "Error fetching student data: " + error.getMessage());
                    Toast.makeText(MainActivity2.this, "Error fetching student data", Toast.LENGTH_SHORT).show();
                    fetchUpcomingEvents();
                }
            });
        } else {
            Log.e(TAG, "No user is currently logged in");
            Toast.makeText(this, "No user logged in", Toast.LENGTH_SHORT).show();
            fetchUpcomingEvents();
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
                            if (!eventDate.isBefore(today) && !eventDate.isAfter(maxDate)) {
                                eventList.add(event);
                            }
                        } catch (Exception e) {
                            Log.e(TAG, "Error parsing event date for event " + event.getEventUID(), e);
                        }
                    }
                }

                // Sort by start date (nearest date first)
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

                // Limit to the top 3 events
                if (eventList.size() > 3) {
                    eventList = new ArrayList<>(eventList.subList(0, 3));
                }

                // Log events for debugging
                for (Event event : eventList) {
                    Log.d(TAG, "Displaying event: " + event.getEventName() + ", Date: " + event.getStartDate());
                }

                eventAdapter.updateEventList(eventList);
                Log.d(TAG, "Fetched " + eventList.size() + " upcoming events (limited to 3)");
            }

            @Override
            public void onCancelled(@NonNull DatabaseError error) {
                Log.e(TAG, "Firebase data fetch failed: " + error.getMessage(), error.toException());
            }
        });
    }

    private void fetchAssignedEvents() {
        if (currentUserYearLevel == null || currentUserYearLevel.isEmpty()) {
            Log.e(TAG, "Year level not available");
            return;
        }

        databaseReference.addListenerForSingleValueEvent(new ValueEventListener() {
            @Override
            public void onDataChange(@NonNull DataSnapshot snapshot) {
                Log.d(TAG, "======= DEBUGGING ALL EVENTS =======");
                for (DataSnapshot dataSnapshot : snapshot.getChildren()) {
                    Event event = dataSnapshot.getValue(Event.class);
                    if (event != null) {
                        Log.d(TAG, "Event: " + event.getEventName() +
                                ", EventFor: '" + event.getEventFor() + "'");
                    }
                }
                Log.d(TAG, "===================================");
            }

            @Override
            public void onCancelled(@NonNull DatabaseError error) {
                Log.e(TAG, "Debug events loading failed", error.toException());
            }
        });

        databaseReference.addValueEventListener(new ValueEventListener() {
            @Override
            public void onDataChange(@NonNull DataSnapshot snapshot) {
                assignedEventList.clear();
                Set<CalendarDay> eventDates = new HashSet<>();

                String yearLevel = currentUserYearLevel.toLowerCase().trim();
                String gradeNum = "";
                if (yearLevel.contains("grade")) {
                    gradeNum = yearLevel.replace("grade", "").trim();
                } else if (yearLevel.startsWith("g")) {
                    gradeNum = yearLevel.substring(1).trim();
                } else {
                    gradeNum = yearLevel;
                }

                List<String> possibleFormats = new ArrayList<>();
                possibleFormats.add(yearLevel);
                possibleFormats.add("grade" + gradeNum);
                possibleFormats.add("grade " + gradeNum);
                possibleFormats.add("g" + gradeNum);
                possibleFormats.add("g " + gradeNum);
                possibleFormats.add(gradeNum);

                Log.d(TAG, "Checking for events matching user's year level: " + yearLevel);
                Log.d(TAG, "Possible formats to match: " + possibleFormats.toString());

                int matchCount = 0;

                for (DataSnapshot dataSnapshot : snapshot.getChildren()) {
                    Event event = dataSnapshot.getValue(Event.class);
                    if (event != null) {
                        event.setEventUID(dataSnapshot.getKey());
                        try {
                            LocalDate date = LocalDate.parse(event.getStartDate(), DATE_FORMATTER);
                            eventDates.add(CalendarDay.from(date.getYear(), date.getMonthValue() - 1, date.getDayOfMonth()));
                        } catch (Exception e) {
                            Log.e(TAG, "Error parsing date for event: " + event.getEventName(), e);
                        }

                        String eventFor = event.getEventFor();
                        if (eventFor != null) {
                            String normalizedEventFor = eventFor.toLowerCase().replace("-", "").trim();
                            boolean isMatch = false;
                            for (String format : possibleFormats) {
                                String normalizedFormat = format.toLowerCase().replace("-", "").trim();
                                if (normalizedEventFor.contains(normalizedFormat) ||
                                        normalizedFormat.contains(normalizedEventFor)) {
                                    isMatch = true;
                                    break;
                                }
                            }

                            if (isMatch) {
                                Log.d(TAG, "✓ MATCH FOUND - Event: " + event.getEventName() +
                                        ", EventFor: '" + eventFor + "' matches with student grade: '" +
                                        yearLevel + "'");
                                assignedEventList.add(event);
                                matchCount++;
                            } else {
                                Log.d(TAG, "✗ NO MATCH - Event: " + event.getEventName() +
                                        ", EventFor: '" + eventFor + "' doesn't match with student grade");
                            }

                            if (normalizedEventFor.contains("all") ||
                                    normalizedEventFor.contains("everyone") ||
                                    normalizedEventFor.contains("allyear")) {
                                if (!assignedEventList.contains(event)) {
                                    Log.d(TAG, "✓ ADDED AS GENERAL EVENT - Event: " + event.getEventName() +
                                            ", EventFor: '" + eventFor + "'");
                                    assignedEventList.add(event);
                                    matchCount++;
                                }
                            }
                        }
                    }
                }

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

                markEventDatesOnCalendar(eventDates);
                Log.d(TAG, "Found " + matchCount + " events for year level: " + currentUserYearLevel);

                if (assignedEventList.isEmpty()) {
                    Toast.makeText(MainActivity2.this,
                            "No events found for your grade level: " + currentUserYearLevel,
                            Toast.LENGTH_LONG).show();
                }
            }

            @Override
            public void onCancelled(@NonNull DatabaseError error) {
                Log.e(TAG, "Fetching assigned events failed: " + error.getMessage());
            }
        });
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
                        LocalDate date = LocalDate.parse(event.getStartDate(), DATE_FORMATTER);
                        CalendarDay calDay = CalendarDay.from(date.getYear(), date.getMonthValue() - 1, date.getDayOfMonth());
                        if (eventDates.contains(calDay)) {
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
                            view.addSpan(new DotSpan(8, ContextCompat.getColor(MainActivity2.this, colorRes)));
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
}