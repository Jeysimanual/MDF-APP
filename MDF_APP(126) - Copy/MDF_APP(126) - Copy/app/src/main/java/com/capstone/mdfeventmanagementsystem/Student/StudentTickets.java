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
import android.widget.LinearLayout;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;
import androidx.swiperefreshlayout.widget.SwipeRefreshLayout;

import com.capstone.mdfeventmanagementsystem.Adapters.EventTicketAdapter;
import com.capstone.mdfeventmanagementsystem.R;
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

import java.util.ArrayList;
import java.util.List;

class CircleTransformTicket implements Transformation {
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

public class StudentTickets extends BaseActivity {

    private RecyclerView recyclerView;
    private EventTicketAdapter adapter;
    private List<EventTicket> ticketList;
    private LinearLayout emptyStateLayout;
    private SwipeRefreshLayout swipeRefreshLayout;
    private DatabaseReference studentTicketsRef;
    private FirebaseAuth mAuth;
    private static final String TAG = "ticketTesting";
    private ImageView profileImageView;
    private DatabaseReference studentsRef;
    private DatabaseReference profilesRef;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_student_tickets);

        profileImageView = findViewById(R.id.profile_image);
        emptyStateLayout = findViewById(R.id.empty_state);
        swipeRefreshLayout = findViewById(R.id.swipe_refresh_layout);

        swipeRefreshLayout.setOnRefreshListener(() -> {
            Log.d(TAG, "Pull-to-refresh triggered");
            fetchStudentUID();
        });

        swipeRefreshLayout.setColorSchemeResources(R.color.bg_green, R.color.white);

        // Load cached image with user validation
        loadCachedProfileImage();

        // Initialize Firebase references
        studentsRef = FirebaseDatabase.getInstance().getReference().child("students");
        profilesRef = FirebaseDatabase.getInstance().getReference().child("user_profiles");

        findViewById(R.id.fab_scan).setOnClickListener(v -> {
            startActivity(new Intent(getApplicationContext(), QRCheckInActivity.class));
            overridePendingTransition(0, 0);
        });

        BottomAppBar bottomAppBar = findViewById(R.id.bottomAppBar);
        bottomAppBar.setBackgroundTint(ColorStateList.valueOf(Color.WHITE));

        findViewById(R.id.profile_image).setOnClickListener(v -> {
            Intent intent = new Intent(StudentTickets.this, ProfileActivity.class);
            startActivity(intent);
        });

        setupBottomNavigation();
        loadUserProfile();

        recyclerView = findViewById(R.id.recyclerViewTickets);
        recyclerView.setLayoutManager(new LinearLayoutManager(this));

        ticketList = new ArrayList<>();
        adapter = new EventTicketAdapter(StudentTickets.this, ticketList);
        recyclerView.setAdapter(adapter);

        mAuth = FirebaseAuth.getInstance();
        FirebaseUser currentUser = mAuth.getCurrentUser();

        if (currentUser != null) {
            fetchStudentUID();
        } else {
            Log.e(TAG, "User not logged in!");
            Toast.makeText(this, "User not logged in!", Toast.LENGTH_SHORT).show();
            finish();
        }
    }

    /**
     * Logout the user and clear cache

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
     } */

    private void fetchStudentUID() {
        FirebaseUser currentUser = mAuth.getCurrentUser();
        if (currentUser == null) {
            Log.e(TAG, "No user logged in!");
            Toast.makeText(this, "User not logged in!", Toast.LENGTH_SHORT).show();
            swipeRefreshLayout.setRefreshing(false);
            finish();
            return;
        }

        String userEmail = currentUser.getEmail();
        if (userEmail == null) {
            Log.e(TAG, "User email is null!");
            Toast.makeText(this, "Error fetching user email!", Toast.LENGTH_SHORT).show();
            swipeRefreshLayout.setRefreshing(false);
            return;
        }

        DatabaseReference studentsRef = FirebaseDatabase.getInstance().getReference("students");
        studentsRef.orderByChild("email").equalTo(userEmail).addListenerForSingleValueEvent(new ValueEventListener() {
            @Override
            public void onDataChange(@NonNull DataSnapshot snapshot) {
                if (!snapshot.exists()) {
                    Log.w(TAG, "No matching student found in database!");
                    Toast.makeText(StudentTickets.this, "No student data found!", Toast.LENGTH_SHORT).show();
                    updateEmptyState(true);
                    swipeRefreshLayout.setRefreshing(false);
                    return;
                }

                for (DataSnapshot studentSnapshot : snapshot.getChildren()) {
                    String studentUID = studentSnapshot.getKey();
                    if (studentUID != null) {
                        Log.d(TAG, "Student UID found: " + studentUID);
                        fetchTicketsForStudent(studentUID);
                    }
                }
            }

            @Override
            public void onCancelled(@NonNull DatabaseError error) {
                Log.e(TAG, "Error fetching student UID: " + error.getMessage());
                updateEmptyState(true);
                swipeRefreshLayout.setRefreshing(false);
            }
        });
    }

    private void fetchTicketsForStudent(String studentUID) {
        studentTicketsRef = FirebaseDatabase.getInstance()
                .getReference("students")
                .child(studentUID)
                .child("tickets");

        Log.d(TAG, "Fetching tickets for student UID: " + studentUID);

        studentTicketsRef.addListenerForSingleValueEvent(new ValueEventListener() {
            @Override
            public void onDataChange(@NonNull DataSnapshot snapshot) {
                ticketList.clear();
                if (!snapshot.exists()) {
                    Log.w(TAG, "No tickets found for student: " + studentUID);
                    updateEmptyState(true);
                    Toast.makeText(StudentTickets.this, "No tickets found!", Toast.LENGTH_SHORT).show();
                    swipeRefreshLayout.setRefreshing(false);
                    return;
                }

                for (DataSnapshot ticketSnapshot : snapshot.getChildren()) {
                    String eventUID = ticketSnapshot.getKey();
                    String qrCodeUrl = ticketSnapshot.child("qrCodeUrl").getValue(String.class);
                    String ticketID = ticketSnapshot.child("ticketID").getValue(String.class);

                    if (eventUID != null && qrCodeUrl != null && ticketID != null) {
                        fetchEventDetails(eventUID, qrCodeUrl, ticketID);
                    } else {
                        Log.e(TAG, "Missing ticket details!");
                    }
                }

                updateEmptyState(ticketList.isEmpty());
                swipeRefreshLayout.setRefreshing(false);
            }

            @Override
            public void onCancelled(@NonNull DatabaseError error) {
                Log.e(TAG, "Error fetching tickets: " + error.getMessage());
                updateEmptyState(true);
                swipeRefreshLayout.setRefreshing(false);
            }
        });
    }

    private void fetchEventDetails(String eventUID, String qrCodeUrl, String ticketID) {
        DatabaseReference eventRef = FirebaseDatabase.getInstance().getReference("events").child(eventUID);

        eventRef.addListenerForSingleValueEvent(new ValueEventListener() {
            @Override
            public void onDataChange(@NonNull DataSnapshot snapshot) {
                if (snapshot.exists()) {
                    processEventSnapshot(snapshot, eventUID, qrCodeUrl, ticketID);
                } else {
                    Log.w(TAG, "Event not found in events for UID: " + eventUID);
                    // Check archive_events collection
                    DatabaseReference archiveEventRef = FirebaseDatabase.getInstance().getReference("archive_events").child(eventUID);
                    archiveEventRef.addListenerForSingleValueEvent(new ValueEventListener() {
                        @Override
                        public void onDataChange(@NonNull DataSnapshot archiveSnapshot) {
                            if (archiveSnapshot.exists()) {
                                Log.d(TAG, "Event found in archive_events for UID: " + eventUID);
                                processEventSnapshot(archiveSnapshot, eventUID, qrCodeUrl, ticketID);
                            } else {
                                Log.w(TAG, "Event not found in archive_events for UID: " + eventUID);
                                swipeRefreshLayout.setRefreshing(false);
                            }
                        }

                        @Override
                        public void onCancelled(@NonNull DatabaseError error) {
                            Log.e(TAG, "Error fetching event details from archive_events: " + error.getMessage());
                            swipeRefreshLayout.setRefreshing(false);
                        }
                    });
                }
            }

            @Override
            public void onCancelled(@NonNull DatabaseError error) {
                Log.e(TAG, "Error fetching event details from events: " + error.getMessage());
                swipeRefreshLayout.setRefreshing(false);
            }
        });
    }

    private void processEventSnapshot(DataSnapshot snapshot, String eventUID, String qrCodeUrl, String ticketID) {
        String eventName = snapshot.child("eventName").getValue(String.class);
        String eventType = snapshot.child("eventType").getValue(String.class);
        String startDate = snapshot.child("startDate").getValue(String.class);
        String endDate = snapshot.child("endDate").getValue(String.class);
        String startTime = snapshot.child("startTime").getValue(String.class);
        String endTime = snapshot.child("endTime").getValue(String.class);
        String graceTime = snapshot.child("graceTime").getValue(String.class);
        String eventSpan = snapshot.child("eventSpan").getValue(String.class);
        String venue = snapshot.child("venue").getValue(String.class);
        String eventDescription = snapshot.child("eventDescription").getValue(String.class);

        // Truncate venue to 20 characters and add ellipses if longer
        if (venue != null && venue.length() > 20) {
            venue = venue.substring(0, 17) + "...";
        }

        if (eventName != null && eventType != null && startDate != null && startTime != null && venue != null) {
            EventTicket ticket = new EventTicket(
                    eventName, eventType, startDate, endDate, startTime, endTime,
                    graceTime, eventSpan, venue, eventDescription, qrCodeUrl, ticketID
            );

            ticketList.add(ticket);
            adapter.notifyDataSetChanged();
            updateEmptyState(ticketList.isEmpty());
            swipeRefreshLayout.setRefreshing(false);
        } else {
            Log.e(TAG, "Missing event details for EventUID: " + eventUID);
            swipeRefreshLayout.setRefreshing(false);
        }
    }

    private void updateEmptyState(boolean showEmptyState) {
        if (showEmptyState) {
            emptyStateLayout.setVisibility(View.VISIBLE);
            recyclerView.setVisibility(View.GONE);
            findViewById(R.id.noEventMessage).setVisibility(View.GONE);
        } else {
            emptyStateLayout.setVisibility(View.GONE);
            recyclerView.setVisibility(View.VISIBLE);
            findViewById(R.id.noEventMessage).setVisibility(View.GONE);
        }
    }

    private void setupBottomNavigation() {
        BottomNavigationView bottomNavigationView = findViewById(R.id.bottom_navigation);
        bottomNavigationView.setSelectedItemId(R.id.nav_ticket);
        bottomNavigationView.setBackground(null);

        bottomNavigationView.setOnNavigationItemSelectedListener(item -> {
            int itemId = item.getItemId();
            if (itemId == R.id.nav_home) {
                startActivity(new Intent(getApplicationContext(), MainActivity2.class));
                overridePendingTransition(0, 0);
                finish();
                return true;
            } else if (itemId == R.id.nav_event) {
                startActivity(new Intent(getApplicationContext(), StudentDashboard.class));
                overridePendingTransition(0, 0);
                finish();
                return true;
            } else if (itemId == R.id.nav_ticket) {
                return true;
            } else if (itemId == R.id.nav_cert) {
                startActivity(new Intent(getApplicationContext(), StudentCertificate.class));
                overridePendingTransition(0, 0);
                finish();
                return true;
            }
            return false;
        });
    }

    @Override
    protected void onResume() {
        super.onResume();
        loadUserProfile();
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
                .transform(new CircleTransformTicket())
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
                    .transform(new CircleTransformTicket()) // Fixed to use CircleTransformTicket
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
}