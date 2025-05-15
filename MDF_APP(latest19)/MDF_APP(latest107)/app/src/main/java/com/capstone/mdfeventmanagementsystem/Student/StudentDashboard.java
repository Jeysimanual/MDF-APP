package com.capstone.mdfeventmanagementsystem.Student;

import android.app.DatePickerDialog;
import android.app.Dialog;
import android.content.Intent;
import android.content.SharedPreferences;
import android.content.res.ColorStateList;
import android.graphics.Bitmap;
import android.graphics.Color;
import android.graphics.drawable.ColorDrawable;
import android.os.Bundle;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.view.Window;
import android.widget.ArrayAdapter;
import android.widget.Button;
import android.widget.EditText;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.Spinner;
import android.widget.TextView;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.appcompat.app.AlertDialog;
import androidx.appcompat.app.AppCompatActivity;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.capstone.mdfeventmanagementsystem.Adapters.EventAdapter;
import com.capstone.mdfeventmanagementsystem.R;
import com.capstone.mdfeventmanagementsystem.Teacher.TeacherEventsInside;
import com.capstone.mdfeventmanagementsystem.Utilities.BaseActivity;
import com.google.android.material.bottomappbar.BottomAppBar;
import com.google.android.material.bottomnavigation.BottomNavigationView;
import com.google.android.material.chip.Chip;
import com.google.android.material.chip.ChipGroup;
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
import java.util.Calendar;
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
    private ImageView filterImageView;  // Declare filterImageView
    private TextView noEventMessage;    // Declare noEventMessage
    private DatabaseReference studentsRef;
    private DatabaseReference profilesRef;
    private static final String TAG = "StudentDashboard";

    // Filter variables
    private String selectedEventType = "All";  // Declare and initialize filter variables
    private String selectedEventFor = "All";
    private LocalDate filterStartDate = null;
    private LocalDate filterEndDate = null;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_student_dashboard);

        profileImageView = findViewById(R.id.profile_image);
        filterImageView = findViewById(R.id.filter);
        noEventMessage = findViewById(R.id.noEventMessage);

        noEventMessage.setText("No events found with the current filters.");

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

        filterImageView.setOnClickListener(v -> showFilterDialog());

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

    private void showFilterDialog() {
        // Create custom dialog
        final Dialog dialog = new Dialog(this);
        dialog.requestWindowFeature(Window.FEATURE_NO_TITLE);
        dialog.setContentView(R.layout.dialog_filter);
        dialog.getWindow().setBackgroundDrawable(new ColorDrawable(Color.TRANSPARENT));
        dialog.getWindow().setLayout(ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.WRAP_CONTENT);

        // Get references to the chip groups
        ChipGroup chipGroupEventType = dialog.findViewById(R.id.chipGroupEventType);
        ChipGroup chipGroupEventFor = dialog.findViewById(R.id.chipGroupEventFor);

        // Get references to individual chips
        Chip chipEventTypeAll = dialog.findViewById(R.id.chipEventTypeAll);
        Chip chipSeminar = dialog.findViewById(R.id.chipSeminar);
        Chip chipOffCampus = dialog.findViewById(R.id.chipOffCampus);
        Chip chipSports = dialog.findViewById(R.id.chipSports);
        Chip chipOther = dialog.findViewById(R.id.chipOther);

        Chip chipAll = dialog.findViewById(R.id.chipAll);
        Chip chipGrade7 = dialog.findViewById(R.id.chipGrade7);
        Chip chipGrade8 = dialog.findViewById(R.id.chipGrade8);
        Chip chipGrade9 = dialog.findViewById(R.id.chipGrade9);
        Chip chipGrade10 = dialog.findViewById(R.id.chipGrade10);
        Chip chipGrade11 = dialog.findViewById(R.id.chipGrade11);
        Chip chipGrade12 = dialog.findViewById(R.id.chipGrade12);

        // Set the initial state based on current selections
        // For Event Type
        switch (selectedEventType) {
            case "All":
                chipEventTypeAll.setChecked(true);
                break;
            case "Seminar":
                chipSeminar.setChecked(true);
                break;
            case "Off-Campus Activity":
                chipOffCampus.setChecked(true);
                break;
            case "Sports Event":
                chipSports.setChecked(true);
                break;
            case "Other":
                chipOther.setChecked(true);
                break;
        }

        // For Event For
        switch (selectedEventFor) {
            case "All":
                chipAll.setChecked(true);
                break;
            case "Grade 7":
                chipGrade7.setChecked(true);
                break;
            case "Grade 8":
                chipGrade8.setChecked(true);
                break;
            case "Grade 9":
                chipGrade9.setChecked(true);
                break;
            case "Grade 10":
                chipGrade10.setChecked(true);
                break;
            case "Grade 11":
                chipGrade11.setChecked(true);
                break;
            case "Grade 12":
                chipGrade12.setChecked(true);
                break;
        }

        // Initialize buttons with custom styling
        Button buttonClear = dialog.findViewById(R.id.buttonClear);
        Button buttonApply = dialog.findViewById(R.id.buttonApply);

        // Add animations to chips
        addAnimationToChips(chipGroupEventType);
        addAnimationToChips(chipGroupEventFor);

        // Clear button
        buttonClear.setOnClickListener(v -> {
            // Reset all filters to default values with animation
            buttonClear.animate().alpha(0.7f).setDuration(100).withEndAction(() -> {
                buttonClear.animate().alpha(1.0f).setDuration(100);

                selectedEventType = "All";
                selectedEventFor = "All";
                filterStartDate = null;
                filterEndDate = null;

                // Select the "All" chips
                chipEventTypeAll.setChecked(true);
                chipAll.setChecked(true);

                // Apply the reset filters immediately
                loadEvents("active");
                dialog.dismiss();

                // Show toast to inform user
                Toast.makeText(StudentDashboard.this, "Filters have been reset", Toast.LENGTH_SHORT).show();
            }).start();
        });

        // Apply button
        buttonApply.setOnClickListener(v -> {
            // Add animation
            buttonApply.animate().alpha(0.7f).setDuration(100).withEndAction(() -> {
                buttonApply.animate().alpha(1.0f).setDuration(100);

                // Get selected values from chips
                int eventTypeCheckedId = chipGroupEventType.getCheckedChipId();
                int eventForCheckedId = chipGroupEventFor.getCheckedChipId();

                // Get the selected event type
                if (eventTypeCheckedId == R.id.chipEventTypeAll) {
                    selectedEventType = "All";
                } else if (eventTypeCheckedId == R.id.chipSeminar) {
                    selectedEventType = "Seminar";
                } else if (eventTypeCheckedId == R.id.chipOffCampus) {
                    selectedEventType = "Off-Campus Activity";
                } else if (eventTypeCheckedId == R.id.chipSports) {
                    selectedEventType = "Sports Event";
                } else if (eventTypeCheckedId == R.id.chipOther) {
                    selectedEventType = "Other";
                }

                // Get the selected event for
                if (eventForCheckedId == R.id.chipAll) {
                    selectedEventFor = "All";
                } else if (eventForCheckedId == R.id.chipGrade7) {
                    selectedEventFor = "Grade-7";
                } else if (eventForCheckedId == R.id.chipGrade8) {
                    selectedEventFor = "Grade-8";
                } else if (eventForCheckedId == R.id.chipGrade9) {
                    selectedEventFor = "Grade-9";
                } else if (eventForCheckedId == R.id.chipGrade10) {
                    selectedEventFor = "Grade-10";
                } else if (eventForCheckedId == R.id.chipGrade11) {
                    selectedEventFor = "Grade-11";
                } else if (eventForCheckedId == R.id.chipGrade12) {
                    selectedEventFor = "Grade-12";
                }

                // Reload events with new filters
                loadEvents("active");
                dialog.dismiss();
            }).start();
        });

        dialog.show();
    }

    /**
     * Add click animation to all chips in a chip group
     */
    private void addAnimationToChips(ChipGroup chipGroup) {
        for (int i = 0; i < chipGroup.getChildCount(); i++) {
            View chip = chipGroup.getChildAt(i);
            if (chip instanceof Chip) {
                chip.setOnTouchListener((v, event) -> {
                    switch (event.getAction()) {
                        case android.view.MotionEvent.ACTION_DOWN:
                            v.animate().scaleX(0.95f).scaleY(0.95f).setDuration(100).start();
                            break;
                        case android.view.MotionEvent.ACTION_UP:
                        case android.view.MotionEvent.ACTION_CANCEL:
                            v.animate().scaleX(1f).scaleY(1f).setDuration(100).start();
                            break;
                    }
                    return false;
                });
            }
        }
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
     * Loads events from Firebase based on the selected tab and filter criteria
     */
    private void loadEvents(String type) {
        Log.d("TestApp", "Loading " + type + " events with filters - EventType: " + selectedEventType +
                ", EventFor: " + selectedEventFor +
                ", StartDate: " + (filterStartDate != null ? filterStartDate.toString() : "null") +
                ", EndDate: " + (filterEndDate != null ? filterEndDate.toString() : "null"));

        databaseReference.addValueEventListener(new ValueEventListener() {
            @Override
            public void onDataChange(@NonNull DataSnapshot snapshot) {
                eventList.clear();

                LocalDate today = LocalDate.now();
                boolean hasEvents = false;

                for (DataSnapshot dataSnapshot : snapshot.getChildren()) {
                    Event event = dataSnapshot.getValue(Event.class);
                    if (event != null) {
                        try {
                            LocalDate eventEndDate = LocalDate.parse(event.getEndDate(), DATE_FORMATTER);
                            LocalDate eventStartDate = LocalDate.parse(event.getStartDate(), DATE_FORMATTER);
                            boolean isActive = !eventEndDate.isBefore(today);

                            // Apply type filter (active or expired)
                            if ((type.equals("active") && isActive) || (type.equals("expired") && !isActive)) {
                                // Apply additional filters
                                boolean matchesFilters = true;

                                // Filter by event type
                                if (!selectedEventType.equals("All") && !event.getEventType().equalsIgnoreCase(selectedEventType)) {
                                    matchesFilters = false;
                                }

                                // Filter by event for
                                if (!selectedEventFor.equals("All") && !event.getEventFor().equalsIgnoreCase(selectedEventFor)) {
                                    matchesFilters = false;
                                }

                                // Filter by date range
                                if (filterStartDate != null && eventEndDate.isBefore(filterStartDate)) {
                                    matchesFilters = false;
                                }
                                if (filterEndDate != null && eventStartDate.isAfter(filterEndDate)) {
                                    matchesFilters = false;
                                }

                                if (matchesFilters) {
                                    event.setEventUID(dataSnapshot.getKey());
                                    eventList.add(event);
                                    hasEvents = true;
                                    Log.d("TestApp", "Event loaded: " + event.getEventName());
                                }
                            }
                        } catch (Exception e) {
                            Log.e("TestApp", "Date parsing error for event " + event.getEventName() + ": " + e.getMessage());
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

                // Update adapter and show/hide elements based on results
                eventAdapter.updateEventList(eventList);

                // Show no events message if no events match the filters
                if (hasEvents) {
                    noEventMessage.setVisibility(View.GONE);
                    recyclerView.setVisibility(View.VISIBLE);
                } else {
                    // Update message based on active filters
                    updateNoEventsMessage();
                    noEventMessage.setVisibility(View.VISIBLE);
                    recyclerView.setVisibility(View.GONE);
                }

                Log.d("TestApp", "Updated adapter with " + eventList.size() + " events.");
            }

            @Override
            public void onCancelled(@NonNull DatabaseError error) {
                Log.e("TestApp", "Firebase data fetch failed: " + error.getMessage());
                noEventMessage.setText("Error loading events. Please try again.");
                noEventMessage.setVisibility(View.VISIBLE);
                recyclerView.setVisibility(View.GONE);
            }
        });
    }

    /**
     * Updates the no events message based on current filters
     */
    private void updateNoEventsMessage() {
        StringBuilder message = new StringBuilder("No events found");

        // Add filter information to the message
        if (!selectedEventType.equals("All") || !selectedEventFor.equals("All")) {
            message.append(" for ");

            // Add event type information if filtered
            if (!selectedEventType.equals("All")) {
                message.append(selectedEventType);

                // Add "and" if both filters are applied
                if (!selectedEventFor.equals("All")) {
                    message.append(" and ");
                }
            }

            // Add event for information if filtered
            if (!selectedEventFor.equals("All")) {
                message.append(selectedEventFor);
            }
        }

        message.append(".");
        noEventMessage.setText(message.toString());
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