package com.capstone.mdfeventmanagementsystem.Student;

import android.content.Intent;
import android.content.SharedPreferences;
import android.content.res.ColorStateList;
import android.graphics.Bitmap;
import android.graphics.Color;
import android.os.Bundle;
import android.util.Log;
import android.view.View;
import android.widget.Button;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.TextView;
import android.widget.Toast;

import androidx.annotation.Nullable;
import androidx.appcompat.app.AppCompatActivity;
import androidx.annotation.NonNull;

import com.bumptech.glide.Glide;
import com.capstone.mdfeventmanagementsystem.MainActivity;
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

/**
 * Circle transformation for Picasso to display profile images as circles
 */
class CircleTransformStudent implements Transformation {
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

public class ProfileActivity extends BaseActivity {

    private static final String TAG = "ProfileActivity";
    private TextView txtUserName, txtUserEmail;
    private ImageView imgProfileStudent, btnBack;
    private LinearLayout btnMyInfo, btnChangePassword;
    private Button btnLogout;
    private DatabaseReference studentsRef;
    private DatabaseReference profilesRef; // Added for student profiles

    @Override
    protected void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_profile);

        // Initialize Firebase Database references
        studentsRef = FirebaseDatabase.getInstance().getReference().child("students");
        profilesRef = FirebaseDatabase.getInstance().getReference().child("user_profiles"); // New reference for profile images

        findViewById(R.id.fab_scan).setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                startActivity(new Intent(getApplicationContext(), QRCheckInActivity.class));
                overridePendingTransition(0, 0);
            }
        });

        BottomAppBar bottomAppBar = findViewById(R.id.bottomAppBar);
        bottomAppBar.setBackgroundTint(ColorStateList.valueOf(Color.WHITE));

        txtUserName = findViewById(R.id.txtUserName);
        txtUserEmail = findViewById(R.id.txtUserEmail);
        imgProfileStudent = findViewById(R.id.imgProfileStudent);
        btnBack = findViewById(R.id.btnBack);
        btnMyInfo = findViewById(R.id.btnMyInfo);
        btnChangePassword = findViewById(R.id.btnChangePassword);
        btnLogout = findViewById(R.id.btnLogout);

        loadUserProfile();

        btnBack.setOnClickListener(view -> finish());

        btnMyInfo.setOnClickListener(view -> {
            Intent intent = new Intent(ProfileActivity.this, MyInformation.class);
            startActivity(intent);
        });

        btnChangePassword.setOnClickListener(view -> {
            Intent intent = new Intent(ProfileActivity.this, ChangePassword.class);
            startActivity(intent);
        });

        BottomNavigationView bottomNavigationView = findViewById(R.id.bottom_navigation);
        bottomNavigationView.setBackground(null);

        bottomNavigationView.getMenu().setGroupCheckable(0, true, false);
        for (int i = 0; i < bottomNavigationView.getMenu().size(); i++) {
            bottomNavigationView.getMenu().getItem(i).setChecked(false);
        }
        bottomNavigationView.getMenu().setGroupCheckable(0, true, true);

        bottomNavigationView.setOnNavigationItemSelectedListener(item -> {
            int itemId = item.getItemId();
            if (itemId == R.id.nav_home) {
                startActivity(new Intent(getApplicationContext(), StudentDashboard.class));
            } else if (itemId == R.id.nav_event) {
                startActivity(new Intent(getApplicationContext(), StudentDashboard.class));
            } else if (itemId == R.id.nav_ticket) {
                startActivity(new Intent(getApplicationContext(), StudentTickets.class));
            } else if (itemId == R.id.nav_cert) {
                startActivity(new Intent(getApplicationContext(), StudentCertificate.class));
            }
            overridePendingTransition(0, 0);
            return false;
        });

        // Initialize Logout Button
        btnLogout = findViewById(R.id.btnLogout);
        btnLogout.setOnClickListener(v -> logoutUser());
    }

    @Override
    protected void onResume() {
        super.onResume();
        // Reload profile data when returning to this screen
        loadUserProfile();
    }

    private void loadUserProfile() {
        FirebaseUser user = FirebaseAuth.getInstance().getCurrentUser();
        if (user != null) {
            // Set email immediately
            txtUserEmail.setText(user.getEmail());

            // Try to fetch full name from Firebase Database
            fetchUserFullName(user.getUid(), user.getEmail());

            // Check for profile image - first check student_profiles collection
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
                        setDefaultProfileImage();
                    }
                } else {
                    Log.d(TAG, "No profile image in students collection");
                    // Check if firebase user has photo URL as last resort
                    FirebaseUser user = FirebaseAuth.getInstance().getCurrentUser();
                    if (user != null && user.getPhotoUrl() != null) {
                        loadProfileImage(user.getPhotoUrl().toString());
                    } else {
                        setDefaultProfileImage();
                    }
                }
            }

            @Override
            public void onCancelled(@NonNull DatabaseError error) {
                Log.e(TAG, "Error checking student profile image: " + error.getMessage());
                setDefaultProfileImage();
            }
        });
    }

    /**
     * Loads profile image using Picasso with circle transformation
     * @param imageUrl URL of the profile image
     */
    private void loadProfileImage(String imageUrl) {
        if (imgProfileStudent == null) {
            Log.e(TAG, "Cannot load profile image: ImageView is null");
            return;
        }

        if (imageUrl != null && !imageUrl.isEmpty()) {
            Log.d(TAG, "Loading profile image from: " + imageUrl);

            // Use Picasso to load the image with transformation for circular display
            Picasso.get()
                    .load(imageUrl)
                    .transform(new CircleTransformStudent()) // Use CircleTransform for circular images
                    .placeholder(R.drawable.profile_placeholder)
                    .error(R.drawable.profile_placeholder)
                    .networkPolicy(com.squareup.picasso.NetworkPolicy.NO_CACHE)
                    .memoryPolicy(com.squareup.picasso.MemoryPolicy.NO_CACHE, com.squareup.picasso.MemoryPolicy.NO_STORE)
                    .into(imgProfileStudent, new com.squareup.picasso.Callback() {
                        @Override
                        public void onSuccess() {
                            Log.d(TAG, "Profile image loaded successfully");
                        }

                        @Override
                        public void onError(Exception e) {
                            Log.e(TAG, "Error loading profile image: " + (e != null ? e.getMessage() : "unknown error"));
                            setDefaultProfileImage();
                        }
                    });
        } else {
            setDefaultProfileImage();
        }
    }

    /**
     * Sets default profile image placeholder
     */
    private void setDefaultProfileImage() {
        if (imgProfileStudent != null) {
            imgProfileStudent.setImageResource(R.drawable.profile_placeholder);
        }
    }

    /**
     * Fetches the user's full name from Firebase Database
     * @param uid User ID for direct lookup
     * @param email User email for lookup by email
     */
    private void fetchUserFullName(String uid, String email) {
        Log.d(TAG, "Attempting to fetch user full name for UID: " + uid);

        // Try to find student by email first
        if (email != null && !email.isEmpty()) {
            studentsRef.orderByChild("email").equalTo(email)
                    .addListenerForSingleValueEvent(new ValueEventListener() {
                        @Override
                        public void onDataChange(@NonNull DataSnapshot dataSnapshot) {
                            if (dataSnapshot.exists()) {
                                // Found student with matching email
                                for (DataSnapshot studentSnapshot : dataSnapshot.getChildren()) {
                                    // Get the student's UID from the database
                                    String studentUid = studentSnapshot.getKey();
                                    if (studentUid != null) {
                                        loadStudentData(studentUid);
                                        return;
                                    }
                                }
                            }
                            // If we get here, no student was found with matching email
                            loadStudentData(uid);
                        }

                        @Override
                        public void onCancelled(@NonNull DatabaseError error) {
                            Log.e(TAG, "Database Error while searching by email: " + error.getMessage());
                            // Fallback to Firebase Auth display name
                            setDefaultDisplayName();
                        }
                    });
        } else {
            // Directly try UID lookup
            loadStudentData(uid);
        }
    }

    private void loadStudentData(String uid) {
        studentsRef.child(uid).addListenerForSingleValueEvent(new ValueEventListener() {
            @Override
            public void onDataChange(@NonNull DataSnapshot dataSnapshot) {
                if (dataSnapshot.exists()) {
                    try {
                        // Get student data from Firebase
                        String firstNameText = safeGetString(dataSnapshot, "firstName");
                        String lastNameText = safeGetString(dataSnapshot, "lastName");
                        String middleNameText = safeGetString(dataSnapshot, "middleName");

                        // Try alternate field names if needed
                        if (firstNameText.equals("--")) {
                            firstNameText = safeGetString(dataSnapshot, "firstname");
                        }

                        if (lastNameText.equals("--")) {
                            lastNameText = safeGetString(dataSnapshot, "lastname");
                        }

                        if (middleNameText.equals("--")) {
                            middleNameText = safeGetString(dataSnapshot, "middlename");
                        }

                        // Build full name
                        String fullName = (firstNameText != null && !firstNameText.equals("--") ? firstNameText : "") + " ";
                        if (middleNameText != null && !middleNameText.equals("--") && !middleNameText.equals("N/A")) {
                            fullName += middleNameText + " ";
                        } else {
                            fullName += " ";
                        }
                        fullName += (lastNameText != null && !lastNameText.equals("--") ? lastNameText : "");

                        // Set the user's full name
                        if (!fullName.trim().isEmpty()) {
                            txtUserName.setText(fullName.trim());
                        } else {
                            setDefaultDisplayName();
                        }

                        Log.d(TAG, "Set user full name to: " + fullName.trim());
                    } catch (Exception e) {
                        Log.e(TAG, "Error processing student data: " + e.getMessage());
                        setDefaultDisplayName();
                    }
                } else {
                    Log.w(TAG, "No student record found for UID: " + uid);
                    setDefaultDisplayName();
                }
            }

            @Override
            public void onCancelled(@NonNull DatabaseError error) {
                Log.e(TAG, "Database Error: " + error.getMessage());
                setDefaultDisplayName();
            }
        });
    }

    // Helper method to safely get string from DataSnapshot
    private String safeGetString(DataSnapshot dataSnapshot, String key) {
        if (dataSnapshot.hasChild(key)) {
            Object value = dataSnapshot.child(key).getValue();
            return value != null ? value.toString() : "--";
        }
        return "--";
    }

    // Fallback to display name from Firebase Auth
    private void setDefaultDisplayName() {
        FirebaseUser user = FirebaseAuth.getInstance().getCurrentUser();
        if (user != null) {
            String displayName = user.getDisplayName();
            txtUserName.setText(displayName != null && !displayName.isEmpty() ? displayName : "User");
        } else {
            txtUserName.setText("User");
        }
    }
    /** Handles user logout – FULLY CLEAN & SERVER-SAFE (2025 VERSION) */
    private void logoutUser() {
        Log.d("Logout", "Starting logout process...");

        // 1. Get user info from SharedPreferences
        SharedPreferences prefs = getSharedPreferences("UserSession", MODE_PRIVATE);
        String userType = prefs.getString("userType", "");
        String studentID = prefs.getString("studentID", null);
        String teacherId = prefs.getString("teacherId", null);

        // 2. REMOVE FCM TOKEN FROM REALTIME DATABASE based on user type
        if ("teacher".equals(userType) && teacherId != null) {
            // Remove teacher FCM token
            FirebaseDatabase.getInstance()
                    .getReference("teachers")
                    .child(teacherId)
                    .child("fcmToken")
                    .removeValue()
                    .addOnSuccessListener(aVoid -> {
                        Log.d("Logout", "Teacher FCM token removed successfully: " + teacherId);
                    })
                    .addOnFailureListener(e -> {
                        Log.e("Logout", "Failed to remove teacher FCM token", e);
                    });
        } else if ("student".equals(userType) && studentID != null) {
            // Remove student FCM token
            FirebaseDatabase.getInstance()
                    .getReference("students")
                    .child(studentID)
                    .child("fcmToken")
                    .removeValue()
                    .addOnSuccessListener(aVoid -> {
                        Log.d("Logout", "Student FCM token removed successfully: " + studentID);
                    })
                    .addOnFailureListener(e -> {
                        Log.e("Logout", "Failed to remove student FCM token", e);
                    });
        } else {
            Log.w("Logout", "Unknown user type or missing ID. UserType: " + userType);
        }

        // 3. Sign out from Firebase Auth
        FirebaseAuth.getInstance().signOut();
        Log.d("Logout", "Signed out from Firebase Auth");

        // 4. FULLY CLEAR UserSession
        prefs.edit().clear().apply();

        // Optional: Clear any other app preferences if needed
        getSharedPreferences("MyAppPrefs", MODE_PRIVATE).edit().clear().apply();

        Log.d("ProfileActivity", "User logged out completely — FCM token cleaned");

        // 5. Show appropriate logout message
        String logoutMessage = "Logged out successfully";
        if ("teacher".equals(userType)) {
            logoutMessage = "Teacher logged out successfully";
        } else if ("student".equals(userType)) {
            logoutMessage = "Student logged out successfully";
        }
        Toast.makeText(ProfileActivity.this, logoutMessage, Toast.LENGTH_SHORT).show();

        // 6. Go back to StudentLogin (UNIFIED LOGIN)
        Intent intent = new Intent(ProfileActivity.this, StudentLogin.class);
        intent.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK | Intent.FLAG_ACTIVITY_CLEAR_TASK);
        startActivity(intent);
        finish();
    }
}