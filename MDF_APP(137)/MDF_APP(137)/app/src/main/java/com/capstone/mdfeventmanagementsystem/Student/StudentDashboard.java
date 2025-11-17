package com.capstone.mdfeventmanagementsystem.Student;

import android.app.Dialog;
import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.content.res.ColorStateList;
import android.graphics.Bitmap;
import android.graphics.Color;
import android.graphics.drawable.ColorDrawable;
import android.os.Bundle;
import android.util.Log;
import android.view.View;
import android.view.ViewGroup;
import android.view.Window;
import android.widget.ImageView;
import android.widget.TextView;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.fragment.app.Fragment;
import androidx.swiperefreshlayout.widget.SwipeRefreshLayout;

import com.capstone.mdfeventmanagementsystem.Adapters.EventAdapter;
import com.capstone.mdfeventmanagementsystem.R;
import com.capstone.mdfeventmanagementsystem.Teacher.TeacherEventsInside;
import com.capstone.mdfeventmanagementsystem.Utilities.BaseActivity;
import com.google.android.material.bottomappbar.BottomAppBar;
import com.google.android.material.bottomnavigation.BottomNavigationView;
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

    private TextView tabActive, tabExpired;
    private ImageView profileImageView;
    private ImageView filterImageView;
    private DatabaseReference databaseReference;
    private DatabaseReference studentsRef;
    private DatabaseReference profilesRef;
    private static final String TAG = "StudentDashboard";
    private SwipeRefreshLayout swipeRefreshLayout;
    private static final DateTimeFormatter DATE_FORMATTER = DateTimeFormatter.ofPattern("yyyy-MM-dd");

    // Filter variables
    private String selectedEventType = "All";
    private String selectedEventFor = "All";
    private LocalDate filterStartDate = null;
    private LocalDate filterEndDate = null;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_student_dashboard);

        profileImageView = findViewById(R.id.profile_image);
        filterImageView = findViewById(R.id.filter);
        tabActive = findViewById(R.id.tabActive);
        tabExpired = findViewById(R.id.tabExpired);

        // Load cached image with user validation
        loadCachedProfileImage();

        // Initialize Firebase references
        studentsRef = FirebaseDatabase.getInstance().getReference().child("students");
        profilesRef = FirebaseDatabase.getInstance().getReference().child("user_profiles");
        databaseReference = FirebaseDatabase.getInstance().getReference("events");

        findViewById(R.id.fab_scan).setOnClickListener(v -> {
            startActivity(new Intent(getApplicationContext(), QRCheckInActivity.class));
            overridePendingTransition(0, 0);
        });

        BottomAppBar bottomAppBar = findViewById(R.id.bottomAppBar);
        bottomAppBar.setBackgroundTint(ColorStateList.valueOf(Color.WHITE));

        // Profile section click event
        findViewById(R.id.profile_image).setOnClickListener(v -> {
            Intent intent = new Intent(StudentDashboard.this, ProfileActivity.class);
            startActivity(intent);
        });

        filterImageView.setOnClickListener(v -> showFilterDialog());

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

        // Setup SwipeRefreshLayout
        swipeRefreshLayout = findViewById(R.id.swipeRefreshLayout);
        swipeRefreshLayout.setColorSchemeResources(R.color.primary);
        swipeRefreshLayout.setOnRefreshListener(() -> {
            if (isNetworkAvailable()) {
                refreshCurrentFragment();
                swipeRefreshLayout.setRefreshing(false);
            } else {
                Toast.makeText(this, "No network available", Toast.LENGTH_SHORT).show();
                swipeRefreshLayout.setRefreshing(false);
            }
        });

        // Setup tabs
        setupTabs();

        // Load Active Events Fragment by default
        loadFragment(new StudentActiveEventsFragment(), "active");

        SharedPreferences prefs = getSharedPreferences("UserSession", MODE_PRIVATE);
        String studentID = prefs.getString("studentID", null);

        if (studentID == null) {
            Log.e(TAG, "No studentID found in SharedPreferences!");
            Toast.makeText(this, "Student ID not found. Please log in again.", Toast.LENGTH_SHORT).show();
            Intent intent = new Intent(StudentDashboard.this, StudentLogin.class);
            startActivity(intent);
            finish();
        } else {
            Log.d(TAG, "StudentID found: " + studentID);
        }
    }

    private void setupTabs() {
        tabActive.setOnClickListener(v -> {
            tabActive.setBackgroundResource(R.drawable.tab_selected);
            tabActive.setTextColor(getResources().getColor(R.color.black));
            tabExpired.setBackgroundResource(android.R.color.transparent);
            tabExpired.setTextColor(getResources().getColor(R.color.gray));
            loadFragment(new StudentActiveEventsFragment(), "active");
        });

        tabExpired.setOnClickListener(v -> {
            tabExpired.setBackgroundResource(R.drawable.tab_selected);
            tabExpired.setTextColor(getResources().getColor(R.color.black));
            tabActive.setBackgroundResource(android.R.color.transparent);
            tabActive.setTextColor(getResources().getColor(R.color.gray));
            loadFragment(new StudentExpiredEventsFragment(), "expired");
        });
    }

    private void loadFragment(Fragment fragment, String tag) {
        Bundle args = new Bundle();
        args.putString("eventType", selectedEventType);
        args.putString("eventFor", selectedEventFor);
        if (filterStartDate != null) args.putString("startDate", filterStartDate.format(DATE_FORMATTER));
        if (filterEndDate != null) args.putString("endDate", filterEndDate.format(DATE_FORMATTER));
        fragment.setArguments(args);

        getSupportFragmentManager()
                .beginTransaction()
                .replace(R.id.fragment_container, fragment, tag)
                .commit();
    }

    private void refreshCurrentFragment() {
        Fragment currentFragment = getSupportFragmentManager().findFragmentById(R.id.fragment_container);
        if (currentFragment instanceof StudentActiveEventsFragment) {
            loadFragment(new StudentActiveEventsFragment(), "active");
        } else if (currentFragment instanceof StudentExpiredEventsFragment) {
            loadFragment(new StudentExpiredEventsFragment(), "expired");
        }
    }

    private boolean isNetworkAvailable() {
        android.net.ConnectivityManager connectivityManager = (android.net.ConnectivityManager) getSystemService(Context.CONNECTIVITY_SERVICE);
        return connectivityManager != null && connectivityManager.getActiveNetworkInfo() != null && connectivityManager.getActiveNetworkInfo().isConnected();
    }

    @Override
    protected void onResume() {
        super.onResume();
        // Update profile image from Firebase in background
        loadUserProfile();
    }

    /**
     * Logout the user and clear cache
     */
    public void logout() {
        // Sign out from Firebase
        FirebaseAuth.getInstance().signOut();

        // Clear profile image cache
        clearProfileImageCache();

        // Clear user session data
        SharedPreferences sessionPrefs = getSharedPreferences("UserSession", MODE_PRIVATE);
        sessionPrefs.edit().clear().apply();

        // Redirect to login screen
        Intent intent = new Intent(this, StudentLogin.class);
        startActivity(intent);
        finish();
    }

    private void showFilterDialog() {
        final Dialog dialog = new Dialog(this);
        dialog.requestWindowFeature(Window.FEATURE_NO_TITLE);
        dialog.setContentView(R.layout.dialog_filter);
        dialog.getWindow().setBackgroundDrawable(new ColorDrawable(Color.TRANSPARENT));
        dialog.getWindow().setLayout(ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.WRAP_CONTENT);

        ChipGroup chipGroupEventType = dialog.findViewById(R.id.chipGroupEventType);
        ChipGroup chipGroupEventFor = dialog.findViewById(R.id.chipGroupEventFor);

        com.google.android.material.chip.Chip chipEventTypeAll = dialog.findViewById(R.id.chipEventTypeAll);
        com.google.android.material.chip.Chip chipSeminar = dialog.findViewById(R.id.chipSeminar);
        com.google.android.material.chip.Chip chipOffCampus = dialog.findViewById(R.id.chipOffCampus);
        com.google.android.material.chip.Chip chipSports = dialog.findViewById(R.id.chipSports);
        com.google.android.material.chip.Chip chipOther = dialog.findViewById(R.id.chipOther);

        com.google.android.material.chip.Chip chipAll = dialog.findViewById(R.id.chipAll);
        com.google.android.material.chip.Chip chipGrade7 = dialog.findViewById(R.id.chipGrade7);
        com.google.android.material.chip.Chip chipGrade8 = dialog.findViewById(R.id.chipGrade8);
        com.google.android.material.chip.Chip chipGrade9 = dialog.findViewById(R.id.chipGrade9);
        com.google.android.material.chip.Chip chipGrade10 = dialog.findViewById(R.id.chipGrade10);
        com.google.android.material.chip.Chip chipGrade11 = dialog.findViewById(R.id.chipGrade11);
        com.google.android.material.chip.Chip chipGrade12 = dialog.findViewById(R.id.chipGrade12);

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

        switch (selectedEventFor) {
            case "All":
                chipAll.setChecked(true);
                break;
            case "Grade-7":
                chipGrade7.setChecked(true);
                break;
            case "Grade-8":
                chipGrade8.setChecked(true);
                break;
            case "Grade-9":
                chipGrade9.setChecked(true);
                break;
            case "Grade-10":
                chipGrade10.setChecked(true);
                break;
            case "Grade-11":
                chipGrade11.setChecked(true);
                break;
            case "Grade-12":
                chipGrade12.setChecked(true);
                break;
        }

        com.google.android.material.button.MaterialButton buttonClear = dialog.findViewById(R.id.buttonClear);
        com.google.android.material.button.MaterialButton buttonApply = dialog.findViewById(R.id.buttonApply);

        addAnimationToChips(chipGroupEventType);
        addAnimationToChips(chipGroupEventFor);

        buttonClear.setOnClickListener(v -> {
            buttonClear.animate().alpha(0.7f).setDuration(100).withEndAction(() -> {
                buttonClear.animate().alpha(1.0f).setDuration(100);
                selectedEventType = "All";
                selectedEventFor = "All";
                filterStartDate = null;
                filterEndDate = null;
                chipEventTypeAll.setChecked(true);
                chipAll.setChecked(true);
                refreshCurrentFragment();
                dialog.dismiss();
                Toast.makeText(StudentDashboard.this, "Filters have been reset", Toast.LENGTH_SHORT).show();
            }).start();
        });

        buttonApply.setOnClickListener(v -> {
            buttonApply.animate().alpha(0.7f).setDuration(100).withEndAction(() -> {
                buttonApply.animate().alpha(1.0f).setDuration(100);
                int eventTypeCheckedId = chipGroupEventType.getCheckedChipId();
                int eventForCheckedId = chipGroupEventFor.getCheckedChipId();

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

                refreshCurrentFragment();
                dialog.dismiss();
            }).start();
        });

        dialog.show();
    }

    private void addAnimationToChips(ChipGroup chipGroup) {
        for (int i = 0; i < chipGroup.getChildCount(); i++) {
            View chip = chipGroup.getChildAt(i);
            if (chip instanceof com.google.android.material.chip.Chip) {
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
     * Load cached profile image, validating against current user
     */
    private void loadCachedProfileImage() {
        FirebaseUser user = FirebaseAuth.getInstance().getCurrentUser();
        if (user == null) {
            setDefaultProfileImage();
            return;
        }
        String userId = user.getUid();
        SharedPreferences prefs = getSharedPreferences("ProfileImageCache", MODE_PRIVATE);
        String cachedUserId = prefs.getString("cachedUserId", "");
        String cachedImageUrl = prefs.getString("profileImageUrl_" + userId, "");

        if (cachedUserId.equals(userId) && !cachedImageUrl.isEmpty()) {
            loadProfileImageFromCache(cachedImageUrl);
        } else {
            // Clear outdated cache if it exists
            if (!cachedUserId.isEmpty() && !cachedUserId.equals(userId)) {
                SharedPreferences.Editor editor = prefs.edit();
                editor.remove("profileImageUrl_" + cachedUserId);
                editor.remove("cachedUserId");
                editor.apply();
            }
            setDefaultProfileImage();
        }
    }

    /**
     * Load profile image from cache with no network operations
     */
    private void loadProfileImageFromCache(String imageUrl) {
        if (profileImageView == null) {
            Log.e(TAG, "ProfileImageView is null");
            return;
        }

        Picasso.get()
                .load(imageUrl)
                .transform(new CircleTransformDashboard())
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
                        Log.e(TAG, "Error loading cached image: " + e.getMessage());
                        setDefaultProfileImage();
                    }
                });
    }

    /**
     * Save profile image URL to cache for the current user
     */
    private void cacheProfileImageUrl(String imageUrl) {
        FirebaseUser user = FirebaseAuth.getInstance().getCurrentUser();
        if (user != null) {
            String userId = user.getUid();
            SharedPreferences prefs = getSharedPreferences("ProfileImageCache", MODE_PRIVATE);
            SharedPreferences.Editor editor = prefs.edit();
            editor.putString("profileImageUrl_" + userId, imageUrl);
            editor.putString("cachedUserId", userId);
            editor.apply();
            Log.d(TAG, "Cached profile image URL for user: " + userId);
        }
    }

    /**
     * Clear profile image cache for all users
     */
    private void clearProfileImageCache() {
        SharedPreferences prefs = getSharedPreferences("ProfileImageCache", MODE_PRIVATE);
        SharedPreferences.Editor editor = prefs.edit();
        editor.clear();
        editor.apply();
        Log.d(TAG, "Profile image cache cleared");
    }

    private void loadUserProfile() {
        FirebaseUser user = FirebaseAuth.getInstance().getCurrentUser();
        if (user != null) {
            checkProfileImage(user.getUid());
        } else {
            setDefaultProfileImage();
            clearProfileImageCache();
        }
    }

    /**
     * Checks for profile image in user_profiles collection
     */
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
                        checkStudentProfileImage(uid);
                    }
                } else {
                    Log.d(TAG, "No profile image found in user_profiles, checking students collection");
                    checkStudentProfileImage(uid);
                }
            }

            @Override
            public void onCancelled(@NonNull DatabaseError error) {
                Log.e(TAG, "Error checking user_profiles: " + error.getMessage());
                checkStudentProfileImage(uid);
            }
        });
    }

    /**
     * Checks for profile image in students collection
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
                        Log.d(TAG, "Profile image field exists but is empty in students collection");
                        clearProfileImageCache();
                        setDefaultProfileImage();
                    }
                } else {
                    Log.d(TAG, "No profile image in students collection");
                    FirebaseUser user = FirebaseAuth.getInstance().getCurrentUser();
                    if (user != null && user.getPhotoUrl() != null) {
                        loadProfileImage(user.getPhotoUrl().toString());
                    } else {
                        clearProfileImageCache();
                        setDefaultProfileImage();
                    }
                }
            }

            @Override
            public void onCancelled(@NonNull DatabaseError error) {
                Log.e(TAG, "Error checking student profile image: " + error.getMessage());
                clearProfileImageCache();
                setDefaultProfileImage();
            }
        });
    }

    /**
     * Loads profile image using Picasso with circle transformation
     */
    private void loadProfileImage(String imageUrl) {
        if (profileImageView == null) {
            Log.e(TAG, "Cannot load profile image: ImageView is null");
            return;
        }

        if (imageUrl != null && !imageUrl.isEmpty()) {
            Log.d(TAG, "Loading profile image from: " + imageUrl);
            Picasso.get()
                    .load(imageUrl)
                    .transform(new CircleTransformDashboard())
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
                            Log.e(TAG, "Error loading profile image: " + e.getMessage());
                            clearProfileImageCache();
                            setDefaultProfileImage();
                        }
                    });
        } else {
            clearProfileImageCache();
            setDefaultProfileImage();
        }
    }

    /**
     * Sets default profile image placeholder
     */
    private void setDefaultProfileImage() {
        if (profileImageView != null) {
            profileImageView.setImageResource(R.drawable.profile_placeholder);
            Log.d(TAG, "Set default profile image");
        }
    }

    @Override
    public void onEventClick(Event event) {
        Log.d(TAG, "Event selected: " + event.getEventName());
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