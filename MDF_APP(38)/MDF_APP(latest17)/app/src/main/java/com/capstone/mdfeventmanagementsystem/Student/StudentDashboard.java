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
import com.capstone.mdfeventmanagementsystem.Teacher.TeacherEventsInside;
import com.capstone.mdfeventmanagementsystem.Utilities.BaseActivity;
import com.google.android.material.bottomappbar.BottomAppBar;
import com.google.android.material.bottomnavigation.BottomNavigationView;
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

class CircleTransformDashboard implements Transformation {
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

public class StudentDashboard extends BaseActivity implements EventAdapter.OnEventClickListener {

    private RecyclerView recyclerView;
    private EventAdapter eventAdapter;
    private List<Event> eventList;
    private DatabaseReference databaseReference;
    private static final DateTimeFormatter DATE_FORMATTER = DateTimeFormatter.ofPattern("yyyy-MM-dd");

    private TextView tabActive, tabExpired;
    private ImageView profileImageView;
    private DatabaseReference studentsRef;
    private DatabaseReference profilesRef;
    private static final String TAG = "StudentDashboard";

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_student_dashboard);

        profileImageView = findViewById(R.id.profile_image);

        // Load cached image immediately first
        loadCachedProfileImage();

        // Then start Firebase references
        studentsRef = FirebaseDatabase.getInstance().getReference().child("students");
        profilesRef = FirebaseDatabase.getInstance().getReference().child("user_profiles");

        findViewById(R.id.fab_scan).setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                startActivity(new Intent(getApplicationContext(), QRCheckInActivity.class));
                overridePendingTransition(0, 0);
            }
        });

        BottomAppBar bottomAppBar = findViewById(R.id.bottomAppBar);
        bottomAppBar.setBackgroundTint(ColorStateList.valueOf(Color.WHITE));

        // Profile section click event
        findViewById(R.id.profile_image).setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                Intent intent = new Intent(StudentDashboard.this, ProfileActivity.class);
                startActivity(intent);
            }
        });

        // Tab elements
        // tabActive = findViewById(R.id.tabActive);
        // tabExpired = findViewById(R.id.tabExpired);
        recyclerView = findViewById(R.id.recyclerViewEvents);

        recyclerView.setLayoutManager(new LinearLayoutManager(this));
        eventList = new ArrayList<>();
        eventAdapter = new EventAdapter(this, eventList, this);
        recyclerView.setAdapter(eventAdapter);

        databaseReference = FirebaseDatabase.getInstance().getReference("events");

        // Load Active Events by default
        loadEvents("active");

        // Load profile image in background after displaying cached version
        loadUserProfile();

        /**
         // Tab Click Listeners
         tabActive.setOnClickListener(v -> {
         setActiveTab(true);
         loadEvents("active");
         });

         tabExpired.setOnClickListener(v -> {
         setActiveTab(false);
         loadEvents("expired");
         }); */

        SharedPreferences prefs = getSharedPreferences("UserSession", MODE_PRIVATE);
        String studentID = prefs.getString("studentID", null);

        if (studentID == null) {
            Log.e("TestApp", "No studentID found in SharedPreferences!");
            Toast.makeText(this, "Student ID not found. Please log in again.", Toast.LENGTH_SHORT).show();
            Intent intent = new Intent(StudentDashboard.this, StudentLogin.class);
            startActivity(intent);
            finish();
        } else {
            Log.d("TestApp", "StudentID found: " + studentID);
        }

        // Setup bottom navigation
        BottomNavigationView bottomNavigationView = findViewById(R.id.bottom_navigation);
        bottomNavigationView.setSelectedItemId(R.id.nav_event);
        bottomNavigationView.setBackground(null);

        bottomNavigationView.setOnNavigationItemSelectedListener(item -> {
            int itemId = item.getItemId();
            if (itemId == R.id.nav_home) {
                startActivity(new Intent(getApplicationContext(), MainActivity2.class));
            } else if (itemId == R.id.nav_event) {
                return true;
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

    /**
     * Loads events from Firebase based on the selected tab (Active/Expired)
     */
    private void loadEvents(String type) {
        Log.d("TestApp", "Loading " + type + " events...");

        databaseReference.addValueEventListener(new ValueEventListener() {
            @Override
            public void onDataChange(@NonNull DataSnapshot snapshot) {
                eventList.clear();

                LocalDate today = LocalDate.now();

                for (DataSnapshot dataSnapshot : snapshot.getChildren()) {
                    Event event = dataSnapshot.getValue(Event.class);
                    if (event != null) {
                        try {
                            LocalDate eventDate = LocalDate.parse(event.getEndDate(), DATE_FORMATTER);
                            boolean isActive = !eventDate.isBefore(today);

                            // Filter events based on type
                            if ((type.equals("active") && isActive) || (type.equals("expired") && !isActive)) {
                                event.setEventUID(dataSnapshot.getKey());
                                eventList.add(event);
                                Log.d("TestApp", "Event loaded: " + event.getEventName());
                            }
                        } catch (Exception e) {
                            Log.e("TestApp", "Date parsing error: " + e.getMessage());
                        }
                    }
                }

                // Sort events by start date
                Collections.sort(eventList, (e1, e2) -> {
                    try {
                        LocalDate date1 = LocalDate.parse(e1.getStartDate(), DATE_FORMATTER);
                        LocalDate date2 = LocalDate.parse(e2.getStartDate(), DATE_FORMATTER);
                        return date1.compareTo(date2);
                    } catch (Exception e) {
                        Log.e("TestApp", "Sorting error: " + e.getMessage());
                        return 0;
                    }
                });

                eventAdapter.updateEventList(eventList);
                Log.d("TestApp", "Updated adapter with " + eventList.size() + " events.");
            }

            @Override
            public void onCancelled(@NonNull DatabaseError error) {
                Log.e("TestApp", "Firebase data fetch failed: " + error.getMessage());
            }
        });
    }

    @Override
    public void onEventClick(Event event) {
        Log.d("TestApp", "Event selected: " + event.getEventName());

        SharedPreferences prefs = getSharedPreferences("UserSession", MODE_PRIVATE);
        String userType = prefs.getString("userType", "student");

        Intent intent;
        if ("teacher".equals(userType)) {
            intent = new Intent(this, TeacherEventsInside.class);
        } else {
            intent = new Intent(this, StudentDashboardInside.class);
        }

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

        startActivity(intent);
    }
}