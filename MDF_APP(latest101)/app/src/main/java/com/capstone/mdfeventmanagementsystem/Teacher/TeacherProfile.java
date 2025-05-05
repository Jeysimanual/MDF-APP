package com.capstone.mdfeventmanagementsystem.Teacher;

import android.content.Intent;
import android.os.Bundle;
import android.util.Log;
import android.view.View;
import android.widget.Button;
import android.widget.LinearLayout;
import android.widget.TextView;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;

import com.capstone.mdfeventmanagementsystem.MainActivity;
import com.capstone.mdfeventmanagementsystem.R;
import com.capstone.mdfeventmanagementsystem.Student.ChangePassword;
import com.capstone.mdfeventmanagementsystem.Student.MyInformation;
import com.capstone.mdfeventmanagementsystem.Student.ProfileActivity;
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

public class TeacherProfile extends BaseActivity {
    private static final String TAG = "TeacherProfile";

    private LinearLayout btnMyInfo,btnChangePassword;
    private Button btnLogout;

    // Added for teacher information display
    private TextView txtFullname, txtUserEmail;
    private DatabaseReference teachersRef;
    private FirebaseAuth mAuth;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_teacher_profile);

        // Initialize Firebase components
        mAuth = FirebaseAuth.getInstance();
        teachersRef = FirebaseDatabase.getInstance().getReference().child("teachers");

        findViewById(R.id.fab_create).setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                startActivity(new Intent(getApplicationContext(), TeacherCreateEventActivity.class));
                overridePendingTransition(0, 0);
            }
        });

        BottomAppBar bottomAppBar = findViewById(R.id.bottomAppBar);

        // Initialize UI elements
        initializeUI();

        // Load the teacher's data
        loadTeacherData();

        btnMyInfo.setOnClickListener(view -> {
            Intent intent = new Intent(TeacherProfile.this, TeacherInformation.class);
            startActivity(intent);
        });

        btnChangePassword.setOnClickListener(view -> {
            Intent intent = new Intent(TeacherProfile.this, Teacher_ChangePassword.class);
            startActivity(intent);
        });

        btnLogout.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                logoutUser();
            }
        });

        // Initialize Bottom Navigation
        BottomNavigationView bottomNavigationView = findViewById(R.id.bottom_navigation_teacher);
        bottomNavigationView.setSelectedItemId(R.id.nav_profile_teacher);
        bottomNavigationView.setBackground(null);

        bottomNavigationView.setOnItemSelectedListener(item -> {
            int itemId = item.getItemId();

            if (itemId == R.id.nav_home_teacher) {
                startActivity(new Intent(this, TeacherDashboard.class));
                finish();
            } else if (itemId == R.id.nav_event_teacher) {
                startActivity(new Intent(this, TeacherEvents.class));
                finish();
            } else if (itemId == R.id.nav_scan_teacher) {
                startActivity(new Intent(this, TeacherScanning.class));
                finish();
            } else if (itemId == R.id.nav_profile_teacher) {
                return true; // Stay on the same page
            }

            overridePendingTransition(0, 0); // Smooth transition
            return true;
        });
    }

    private void initializeUI() {
        btnMyInfo = findViewById(R.id.btnMyInfo);
        btnChangePassword = findViewById(R.id.btnChangePassword);
        btnLogout = findViewById(R.id.btnLogout);

        // Initialize text views for teacher information
        txtFullname = findViewById(R.id.txtFullname);
        txtUserEmail = findViewById(R.id.txtUserEmail);
    }

    private void loadTeacherData() {
        FirebaseUser currentUser = mAuth.getCurrentUser();

        if (currentUser == null) {
            Toast.makeText(this, "User not authenticated!", Toast.LENGTH_SHORT).show();
            Log.e(TAG, "No user is currently logged in");
            return;
        }

        String uid = currentUser.getUid();
        Log.d(TAG, "Currently logged in with UID: " + uid);

        // First check if email is in teachers collection
        String userEmail = currentUser.getEmail();
        if (userEmail != null) {
            findTeacherByEmail(userEmail, uid);
        } else {
            // Fallback to direct UID lookup
            findTeacherByUid(uid);
        }
    }

    private void findTeacherByEmail(String email, String fallbackUid) {
        teachersRef.orderByChild("email").equalTo(email).addListenerForSingleValueEvent(new ValueEventListener() {
            @Override
            public void onDataChange(@NonNull DataSnapshot dataSnapshot) {
                if (dataSnapshot.exists()) {
                    // Found teacher with matching email
                    for (DataSnapshot teacherSnapshot : dataSnapshot.getChildren()) {
                        // Get the teacher's UID from the database
                        String teacherUid = teacherSnapshot.getKey();
                        if (teacherUid != null) {
                            loadTeacherDetails(teacherUid);
                            return;
                        }
                    }
                }
                // If we get here, no teacher was found with matching email
                findTeacherByUid(fallbackUid);
            }

            @Override
            public void onCancelled(@NonNull DatabaseError error) {
                Log.e(TAG, "Database Error while searching by email: " + error.getMessage());
                findTeacherByUid(fallbackUid);
            }
        });
    }

    private void findTeacherByUid(String uid) {
        teachersRef.child(uid).addListenerForSingleValueEvent(new ValueEventListener() {
            @Override
            public void onDataChange(@NonNull DataSnapshot dataSnapshot) {
                if (dataSnapshot.exists()) {
                    // Load data for this UID
                    loadTeacherDetails(uid);
                } else {
                    Log.w(TAG, "No teacher record found for UID: " + uid);
                    Toast.makeText(TeacherProfile.this, "Your teacher profile was not found", Toast.LENGTH_LONG).show();

                    // Display empty profile with current authenticated user's email
                    FirebaseUser user = mAuth.getCurrentUser();
                    if (user != null && user.getEmail() != null) {
                        safeSetText(txtUserEmail, user.getEmail());
                    }
                }
            }

            @Override
            public void onCancelled(@NonNull DatabaseError error) {
                Log.e(TAG, "Database Error: " + error.getMessage());
                Toast.makeText(TeacherProfile.this, "Database Error: " + error.getMessage(), Toast.LENGTH_SHORT).show();
            }
        });
    }

    private void loadTeacherDetails(String uid) {
        Log.d(TAG, "Loading teacher data for UID: " + uid);

        teachersRef.child(uid).addValueEventListener(new ValueEventListener() {
            @Override
            public void onDataChange(@NonNull DataSnapshot dataSnapshot) {
                try {
                    if (dataSnapshot.exists()) {
                        Log.d(TAG, "Teacher data found!");

                        // Extract teacher information
                        String firstNameText = safeGetString(dataSnapshot, "firstname");
                        String lastNameText = safeGetString(dataSnapshot, "lastname");
                        String emailText = safeGetString(dataSnapshot, "email");

                        // Set teacher's email
                        safeSetText(txtUserEmail, emailText);

                        // Set teacher's full name
                        String fullName = (firstNameText != null ? firstNameText : "") + " " +
                                (lastNameText != null ? lastNameText : "");
                        safeSetText(txtFullname, fullName.trim());
                    } else {
                        Log.w(TAG, "Teacher data not found for UID: " + uid);
                        Toast.makeText(TeacherProfile.this, "Teacher information not found!", Toast.LENGTH_SHORT).show();
                    }
                } catch (Exception e) {
                    Log.e(TAG, "Error processing data: " + e.getMessage());
                    Toast.makeText(TeacherProfile.this, "Error processing teacher data", Toast.LENGTH_SHORT).show();
                }
            }

            @Override
            public void onCancelled(@NonNull DatabaseError databaseError) {
                Log.e(TAG, "Database Error: " + databaseError.getMessage());
                Toast.makeText(TeacherProfile.this, "Database Error: " + databaseError.getMessage(), Toast.LENGTH_SHORT).show();
            }
        });
    }

    // Helper method to safely get string from DataSnapshot
    private String safeGetString(DataSnapshot dataSnapshot, String key) {
        if (dataSnapshot.hasChild(key)) {
            return dataSnapshot.child(key).getValue(String.class);
        }
        return "--";
    }

    // Helper method to safely set text on TextViews
    private void safeSetText(TextView textView, String value) {
        if (textView != null) {
            textView.setText(value != null && !value.isEmpty() ? value : "--");
        } else {
            Log.e(TAG, "Attempted to set text on null TextView");
        }
    }

    private void logoutUser() {
        FirebaseAuth.getInstance().signOut();

        // Clear SharedPreferences login state
        getSharedPreferences("UserSession", MODE_PRIVATE)
                .edit()
                .putBoolean("isLoggedIn", false)
                .apply();

        // Redirect to MainActivity instead of TeacherLogin
        Intent intent = new Intent(TeacherProfile.this, MainActivity.class);
        intent.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK | Intent.FLAG_ACTIVITY_CLEAR_TASK);
        startActivity(intent);
    }
}