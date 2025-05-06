package com.capstone.mdfeventmanagementsystem.Student;

import android.content.Intent;
import android.content.SharedPreferences;
import android.content.res.ColorStateList;
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

public class ProfileActivity extends BaseActivity {

    private static final String TAG = "ProfileActivity";
    private TextView txtUserName, txtUserEmail;
    private ImageView imgProfile, btnBack;
    private LinearLayout btnMyInfo, btnChangePassword;
    private Button btnLogout;
    private DatabaseReference studentsRef;

    @Override
    protected void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_profile);

        // Initialize Firebase Database reference
        studentsRef = FirebaseDatabase.getInstance().getReference().child("students");

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
        imgProfile = findViewById(R.id.imgProfile);
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

        // ✅ Initialize Logout Button
        btnLogout = findViewById(R.id.btnLogout);
        btnLogout.setOnClickListener(v -> logoutUser());
    }

    private void loadUserProfile() {
        FirebaseUser user = FirebaseAuth.getInstance().getCurrentUser();
        if (user != null) {
            // Set email immediately
            txtUserEmail.setText(user.getEmail());

            // Try to fetch full name from Firebase Database
            fetchUserFullName(user.getUid(), user.getEmail());

            // Load profile image if available
            if (user.getPhotoUrl() != null) {
                Glide.with(this).load(user.getPhotoUrl()).into(imgProfile);
            }
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
                        String fullName = (firstNameText != null && !firstNameText.equals("--") ? firstNameText : "") + " " +
                                (middleNameText != null && !middleNameText.equals("--") ? middleNameText + " " : "") +
                                (lastNameText != null && !lastNameText.equals("--") ? lastNameText : "");

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

    /** ✅ Handles user logout */
    private void logoutUser() {
        // Log out from Firebase Authentication
        FirebaseAuth.getInstance().signOut();

        // Clear all shared preferences
        SharedPreferences userSession = getSharedPreferences("UserSession", MODE_PRIVATE);
        userSession.edit().clear().apply();

        // Also clear MyAppPrefs if it's used elsewhere
        SharedPreferences myAppPrefs = getSharedPreferences("MyAppPrefs", MODE_PRIVATE);
        myAppPrefs.edit().clear().apply();

        Log.d("QRCheckInActivity", "User logged out successfully");

        // Redirect to login/role selection screen
        Intent intent = new Intent(ProfileActivity.this, MainActivity.class);
        intent.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK | Intent.FLAG_ACTIVITY_CLEAR_TASK); // Clear entire activity stack
        startActivity(intent);
        finish();
    }
}