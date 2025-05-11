package com.capstone.mdfeventmanagementsystem.Student;

import android.content.Intent;
import android.content.SharedPreferences;
import android.content.res.ColorStateList;
import android.graphics.Bitmap;
import android.graphics.Color;
import android.os.Bundle;
import android.util.Log;
import android.view.View;
import android.widget.ImageView;
import android.widget.TextView;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;
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
import com.squareup.picasso.Picasso;
import com.squareup.picasso.Transformation;

import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

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
    private RecyclerView recyclerViewAssignedEvents;
    private EventAdapter eventAdapter;
    private EventAdapter assignedEventAdapter;
    private List<Event> eventList;
    private List<Event> assignedEventList;
    private DatabaseReference databaseReference;
    private DatabaseReference userDatabaseReference;
    private static final DateTimeFormatter DATE_FORMATTER = DateTimeFormatter.ofPattern("yyyy-MM-dd");
    private String currentUserYearLevel;
    private static final String TAG = "MainActivity2"; // For consistent logging

    private TextView firstNameTextView; // Declare TextView for firstName
    private ImageView profileImageView; // Added for profile image
    private DatabaseReference studentsRef;
    private DatabaseReference profilesRef; // Added for student profiles

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main2);

        // Initialize Firebase Database references
        studentsRef = FirebaseDatabase.getInstance().getReference().child("students");
        profilesRef = FirebaseDatabase.getInstance().getReference().child("user_profiles"); // New reference for profile images

        loadCachedProfileImage();

        // Initialize the TextView for firstName and ImageView for profile picture
        firstNameTextView = findViewById(R.id.firstName);
        profileImageView = findViewById(R.id.profile_image);

        findViewById(R.id.fab_scan).setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                startActivity(new Intent(getApplicationContext(), QRCheckInActivity.class));
                overridePendingTransition(0, 0);
            }
        });

        BottomAppBar bottomAppBar = findViewById(R.id.bottomAppBar);
        bottomAppBar.setBackgroundTint(ColorStateList.valueOf(Color.WHITE));

        profileImageView.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                Intent intent = new Intent(MainActivity2.this, ProfileActivity.class);
                startActivity(intent);
            }
        });

        // Set up "View All" button click listener
        MaterialButton btnViewAllUpcoming = findViewById(R.id.btnViewAllUpcoming);
        btnViewAllUpcoming.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                // Navigate to StudentDashboard when View All is clicked
                Intent intent = new Intent(MainActivity2.this, StudentDashboard.class);
                startActivity(intent);
                overridePendingTransition(0, 0);
            }
        });

        // Setup RecyclerView for upcoming events
        recyclerView = findViewById(R.id.recyclerViewEvents);
        recyclerView.setLayoutManager(new LinearLayoutManager(this));
        eventList = new ArrayList<>();

        // Setup RecyclerView for assigned events
        recyclerViewAssignedEvents = findViewById(R.id.recyclerViewEvents);
        recyclerViewAssignedEvents.setLayoutManager(new LinearLayoutManager(this));
        assignedEventList = new ArrayList<>();

        // Initialize adapters with click listeners
        eventAdapter = new EventAdapter(this, eventList, new EventAdapter.OnEventClickListener() {
            @Override
            public void onEventClick(Event event) {
                navigateToEventDetails(event);
            }
        });
        recyclerView.setAdapter(eventAdapter);

        assignedEventAdapter = new EventAdapter(this, assignedEventList, new EventAdapter.OnEventClickListener() {
            @Override
            public void onEventClick(Event event) {
                navigateToEventDetails(event);
            }
        });
        recyclerViewAssignedEvents.setAdapter(assignedEventAdapter);

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
        // We already have cached image displayed, now just update in background
        loadUserProfile();
    }

    /**
     * Load cached profile image immediately on startup
     */
    private void loadCachedProfileImage() {
        SharedPreferences prefs = getSharedPreferences("ProfileImageCache", MODE_PRIVATE);
        String cachedImageUrl = prefs.getString("profileImageUrl", "");

        if (!cachedImageUrl.isEmpty()) {
            // Load the cached image immediately
            loadProfileImageFromCache(cachedImageUrl);
        } else {
            // Set default if no cached image
            setDefaultProfileImage();
        }
    }

    /**
     * Load profile image from cache with no network operations
     */
    private void loadProfileImageFromCache(String imageUrl) {
        if (profileImageView == null) {
            return;
        }

        // Use Picasso to load from cache only - no network
        Picasso.get()
                .load(imageUrl)
                .transform(new CircleTransformDashboard())
                .placeholder(R.drawable.profile_placeholder)
                .error(R.drawable.profile_placeholder)
                .networkPolicy(com.squareup.picasso.NetworkPolicy.OFFLINE) // Only load from cache
                .into(profileImageView, new com.squareup.picasso.Callback() {
                    @Override
                    public void onSuccess() {
                        Log.d(TAG, "Profile image loaded from cache");
                    }

                    @Override
                    public void onError(Exception e) {
                        // If cache loading fails, use the default
                        setDefaultProfileImage();
                    }
                });
    }

    /**
     * Save profile image URL to cache when successfully loaded
     */
    private void cacheProfileImageUrl(String imageUrl) {
        SharedPreferences prefs = getSharedPreferences("ProfileImageCache", MODE_PRIVATE);
        SharedPreferences.Editor editor = prefs.edit();
        editor.putString("profileImageUrl", imageUrl);
        editor.apply();
    }

    private void loadUserProfile() {
        FirebaseUser user = FirebaseAuth.getInstance().getCurrentUser();
        if (user != null) {
            // Check for profile image
            checkProfileImage(user.getUid());
        }
    }

    /**
     * Checks for profile image in student_profiles collection
     * @param uid User ID for lookup
     */
    private void checkProfileImage(String uid) {
        profilesRef.child(uid).addListenerForSingleValueEvent(new ValueEventListener() {
            @Override
            public void onDataChange(@NonNull DataSnapshot dataSnapshot) {
                if (dataSnapshot.exists() && dataSnapshot.hasChild("profileImage")) {
                    String profileImageUrl = dataSnapshot.child("profileImage").getValue(String.class);
                    if (profileImageUrl != null && !profileImageUrl.isEmpty()) {
                        Log.d(TAG, "Found profile image in student_profiles: " + profileImageUrl);
                        loadProfileImage(profileImageUrl);
                    } else {
                        // If no image in profiles, check the students collection
                        checkStudentProfileImage(uid);
                    }
                } else {
                    Log.d(TAG, "No profile image found in student_profiles, checking students collection");
                    checkStudentProfileImage(uid);
                }
            }

            @Override
            public void onCancelled(@NonNull DatabaseError error) {
                Log.e(TAG, "Error checking student_profiles: " + error.getMessage());
                // Fallback to students collection
                checkStudentProfileImage(uid);
            }
        });
    }

    /**
     * Checks for profile image in students collection
     * @param uid User ID for lookup
     */
    private void checkStudentProfileImage(String uid) {
        studentsRef.child(uid).child("profileImage").addListenerForSingleValueEvent(new ValueEventListener() {
            @Override
            public void onDataChange(@NonNull DataSnapshot dataSnapshot) {
                if (dataSnapshot.exists()) {
                    String profileImageUrl = dataSnapshot.getValue(String.class);
                    if (profileImageUrl != null && !profileImageUrl.isEmpty()) {
                        Log.d(TAG, "Found profile image in students collection: " + profileImageUrl);
                        loadProfileImage(profileImageUrl);
                    } else {
                        Log.d(TAG, "Profile image field exists but is empty");
                        // Don't set default here as we already have cached or default
                    }
                } else {
                    Log.d(TAG, "No profile image in students collection");
                    // Check if firebase user has photo URL as last resort
                    FirebaseUser user = FirebaseAuth.getInstance().getCurrentUser();
                    if (user != null && user.getPhotoUrl() != null) {
                        loadProfileImage(user.getPhotoUrl().toString());
                    }
                }
            }

            @Override
            public void onCancelled(@NonNull DatabaseError error) {
                Log.e(TAG, "Error checking student profile image: " + error.getMessage());
                // Don't set default here as we already have cached or default
            }
        });
    }

    /**
     * Loads profile image using Picasso with circle transformation
     * @param imageUrl URL of the profile image
     */
    private void loadProfileImage(String imageUrl) {
        if (profileImageView == null) {
            Log.e(TAG, "Cannot load profile image: ImageView is null");
            return;
        }

        if (imageUrl != null && !imageUrl.isEmpty()) {
            Log.d(TAG, "Loading profile image from: " + imageUrl);

            // Use Picasso to load the image with transformation for circular display
            Picasso.get()
                    .load(imageUrl)
                    .transform(new CircleTransformDashboard()) // Use CircleTransform for circular images
                    .placeholder(R.drawable.profile_placeholder)
                    .error(R.drawable.profile_placeholder)
                    .into(profileImageView, new com.squareup.picasso.Callback() {
                        @Override
                        public void onSuccess() {
                            Log.d(TAG, "Profile image loaded successfully");
                            // Cache the successful URL for next time
                            cacheProfileImageUrl(imageUrl);
                        }

                        @Override
                        public void onError(Exception e) {
                            Log.e(TAG, "Error loading profile image: " + (e != null ? e.getMessage() : "unknown error"));
                            // Don't set default here as we already have cached or default
                        }
                    });
        }
    }

    /**
     * Sets default profile image placeholder
     */
    private void setDefaultProfileImage() {
        if (profileImageView != null) {
            profileImageView.setImageResource(R.drawable.profile_placeholder);
        }
    }

    // New method to handle navigation to StudentDashboardInside
    private void navigateToEventDetails(Event event) {
        Log.d(TAG, "Navigating to StudentDashboardInside for event: " + event.getEventName());
        Intent intent = new Intent(MainActivity2.this, StudentDashboardInside.class);

        // Pass event data to the destination activity
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
        // Add any other event data you need to pass

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

                                // Log the data for debugging purposes
                                Log.d(TAG, "Retrieved student data - ID: " + student.getIdNumber() +
                                        ", Name: " + student.getFirstName() + " " + student.getLastName() +
                                        ", Year Level: " + currentUserYearLevel);

                                // Update the TextView with the firstName
                                String firstName = student.getFirstName();
                                firstNameTextView.setText("Hello, " + firstName + "!");

                                // Now that we have the year level, fetch events
                                fetchUpcomingEvents();
                                fetchAssignedEvents();
                            }
                        }
                    } else {
                        Log.e(TAG, "No student record found for email: " + userEmail);
                        Toast.makeText(MainActivity2.this, "No student record found!", Toast.LENGTH_SHORT).show();

                        // Fetch events anyway (showing all upcoming events)
                        fetchUpcomingEvents();
                    }
                }

                @Override
                public void onCancelled(@NonNull DatabaseError error) {
                    Log.e(TAG, "Error fetching student data: " + error.getMessage());
                    Toast.makeText(MainActivity2.this, "Error fetching student data", Toast.LENGTH_SHORT).show();

                    // Fetch events anyway (showing all upcoming events)
                    fetchUpcomingEvents();
                }
            });
        } else {
            Log.e(TAG, "No user is currently logged in");
            Toast.makeText(this, "No user logged in", Toast.LENGTH_SHORT).show();

            // Fetch events anyway (showing all upcoming events)
            fetchUpcomingEvents();
        }
    }

    private void fetchUpcomingEvents() {
        databaseReference.addValueEventListener(new ValueEventListener() {
            @Override
            public void onDataChange(@NonNull DataSnapshot snapshot) {
                eventList.clear();
                LocalDate today = LocalDate.now();
                LocalDate maxDate = today.plusDays(7); // Set max range to 7 days from today

                for (DataSnapshot dataSnapshot : snapshot.getChildren()) {
                    Event event = dataSnapshot.getValue(Event.class);
                    if (event != null) {
                        event.setEventUID(dataSnapshot.getKey());

                        // Filter events within the next 7 days
                        try {
                            LocalDate eventDate = LocalDate.parse(event.getStartDate(), DATE_FORMATTER);
                            if (!eventDate.isBefore(today) && !eventDate.isAfter(maxDate)) {
                                eventList.add(event); // Add only if within the date range
                            }
                        } catch (Exception e) {
                            Log.e(TAG, "Error parsing event date", e);
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
                        e.printStackTrace();
                        return 0;
                    }
                });

                eventAdapter.updateEventList(eventList);
                Log.d(TAG, "Fetched " + eventList.size() + " upcoming events");
            }

            @Override
            public void onCancelled(@NonNull DatabaseError error) {
                Log.e(TAG, "Firebase data fetch failed: " + error.getMessage(), error.toException());
            }
        });
    }

    // New method to fetch assigned events based on student's year level
    private void fetchAssignedEvents() {
        if (currentUserYearLevel == null || currentUserYearLevel.isEmpty()) {
            Log.e(TAG, "Year level not available");
            return;
        }

        // Log all events to see what's available
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

                // Different possible formats for Grade 9
                String yearLevel = currentUserYearLevel.toLowerCase().trim();
                String grade = "";
                String gradeNum = "";

                // Extract the grade number if it contains "grade" or "g"
                if (yearLevel.contains("grade")) {
                    gradeNum = yearLevel.replace("grade", "").trim();
                } else if (yearLevel.startsWith("g")) {
                    gradeNum = yearLevel.substring(1).trim();
                } else {
                    // Just use what we have
                    gradeNum = yearLevel;
                }

                // Create different possible formats to match against
                List<String> possibleFormats = new ArrayList<>();
                possibleFormats.add(yearLevel);                // original format
                possibleFormats.add("grade" + gradeNum);       // grade9
                possibleFormats.add("grade " + gradeNum);      // grade 9
                possibleFormats.add("g" + gradeNum);           // g9
                possibleFormats.add("g " + gradeNum);          // g 9
                possibleFormats.add(gradeNum);                 // just 9

                // Log all formats we're checking
                Log.d(TAG, "Checking for events matching user's year level: " + yearLevel);
                Log.d(TAG, "Possible formats to match: " + possibleFormats.toString());

                int matchCount = 0;

                for (DataSnapshot dataSnapshot : snapshot.getChildren()) {
                    Event event = dataSnapshot.getValue(Event.class);
                    if (event != null) {
                        event.setEventUID(dataSnapshot.getKey());

                        // Get eventFor and check against all possible formats
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

                            // Also add events for all students
                            if (normalizedEventFor.contains("all") ||
                                    normalizedEventFor.contains("everyone") ||
                                    normalizedEventFor.contains("allyear")) {

                                // Check if it's not already in the list (to avoid duplicates)
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
                Log.d(TAG, "Found " + matchCount + " events for year level: " + currentUserYearLevel);

                // Toast message removed as requested
                if (assignedEventList.isEmpty()) {
                    // Keep the empty list notification
                    Toast.makeText(MainActivity2.this,
                            "No events found for your grade level: " + currentUserYearLevel,
                            Toast.LENGTH_LONG).show();
                }
                // Removed the "Found X events for your grade" toast
            }

            @Override
            public void onCancelled(@NonNull DatabaseError error) {
                Log.e(TAG, "Fetching assigned events failed: " + error.getMessage());
            }
        });
    }
}