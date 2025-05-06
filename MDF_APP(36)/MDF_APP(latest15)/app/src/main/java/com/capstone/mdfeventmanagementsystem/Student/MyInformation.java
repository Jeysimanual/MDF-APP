package com.capstone.mdfeventmanagementsystem.Student;

import android.content.Intent;
import android.os.Bundle;
import android.util.Log;
import android.widget.ImageView;
import android.widget.TextView;
import android.widget.Toast;

import androidx.activity.EdgeToEdge;
import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.graphics.Insets;
import androidx.core.view.ViewCompat;
import androidx.core.view.WindowInsetsCompat;

import com.capstone.mdfeventmanagementsystem.R;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseUser;
import com.google.firebase.database.DataSnapshot;
import com.google.firebase.database.DatabaseError;
import com.google.firebase.database.DatabaseReference;
import com.google.firebase.database.FirebaseDatabase;
import com.google.firebase.database.ValueEventListener;
import com.squareup.picasso.Picasso;

public class MyInformation extends AppCompatActivity {

    private static final String TAG = "MyInformation";

    // UI Elements
    private TextView userFullname, userEmail;
    private TextView firstName, lastName, idNumber, email, yearLevel, section;
    private ImageView profileImageView, btnBack;

    // Firebase
    private FirebaseAuth mAuth;
    private DatabaseReference studentsRef;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        EdgeToEdge.enable(this);
        setContentView(R.layout.activity_my_information);

        ViewCompat.setOnApplyWindowInsetsListener(findViewById(R.id.main), (v, insets) -> {
            Insets systemBars = insets.getInsets(WindowInsetsCompat.Type.systemBars());
            v.setPadding(systemBars.left, systemBars.top, systemBars.right, systemBars.bottom);
            return insets;
        });

        // Initialize Firebase components
        mAuth = FirebaseAuth.getInstance();
        studentsRef = FirebaseDatabase.getInstance().getReference().child("students");

        // Initialize UI elements
        initializeUI();

        // Load the current user's data
        loadCurrentUserData();
    }

    private void initializeUI() {
        try {
            // Card view at the top
            userFullname = findViewById(R.id.user_fullname);
            userEmail = findViewById(R.id.user_email);

            // Profile info section
            firstName = findViewById(R.id.firstName);
            lastName = findViewById(R.id.lastName);
            idNumber = findViewById(R.id.idNumber);
            email = findViewById(R.id.email);
            yearLevel = findViewById(R.id.yrlvl);
            section = findViewById(R.id.section);
            btnBack = findViewById(R.id.btnBack);

            // Profile image
            profileImageView = findViewById(R.id.profileImageView);

            // Set up back button with animation
            btnBack.setOnClickListener(view -> {
                finish(); // Just finish the current activity
                overridePendingTransition(R.anim.slide_in_left, R.anim.slide_out_right);
            });

            // Profile image click listener
            findViewById(R.id.add_profile).setOnClickListener(v -> {
                Toast.makeText(MyInformation.this, "Profile image change feature to be implemented", Toast.LENGTH_SHORT).show();
            });

            // Edit profile button click listener
            findViewById(R.id.editProfileButton).setOnClickListener(v -> {
                Toast.makeText(MyInformation.this, "Edit profile feature to be implemented", Toast.LENGTH_SHORT).show();
            });
        } catch (Exception e) {
            Log.e(TAG, "Error initializing UI elements: " + e.getMessage());
        }
    }

    private void loadCurrentUserData() {
        FirebaseUser currentUser = mAuth.getCurrentUser();

        if (currentUser == null) {
            Toast.makeText(this, "User not authenticated!", Toast.LENGTH_SHORT).show();
            Log.e(TAG, "No user is currently logged in");
            finish();
            return;
        }

        String uid = currentUser.getUid();
        Log.d(TAG, "Currently logged in with UID: " + uid);

        // First check if email is in students collection
        String userEmailStr = currentUser.getEmail();
        if (userEmailStr != null) {
            findStudentByEmail(userEmailStr, uid);
        } else {
            // Fallback to direct UID lookup
            findStudentByUid(uid);
        }
    }

    private void findStudentByEmail(String email, String fallbackUid) {
        studentsRef.orderByChild("email").equalTo(email).addListenerForSingleValueEvent(new ValueEventListener() {
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
                findStudentByUid(fallbackUid);
            }

            @Override
            public void onCancelled(@NonNull DatabaseError error) {
                Log.e(TAG, "Database Error while searching by email: " + error.getMessage());
                findStudentByUid(fallbackUid);
            }
        });
    }

    private void findStudentByUid(String uid) {
        studentsRef.child(uid).addListenerForSingleValueEvent(new ValueEventListener() {
            @Override
            public void onDataChange(@NonNull DataSnapshot dataSnapshot) {
                if (dataSnapshot.exists()) {
                    // Load data for this UID
                    loadStudentData(uid);
                } else {
                    Log.w(TAG, "No student record found for UID: " + uid);
                    Toast.makeText(MyInformation.this, "Your student profile was not found", Toast.LENGTH_LONG).show();

                    // Display empty profile with current authenticated user's email
                    FirebaseUser user = mAuth.getCurrentUser();
                    if (user != null && user.getEmail() != null) {
                        safeSetText(email, user.getEmail());
                        safeSetText(userEmail, user.getEmail());
                    }
                }
            }

            @Override
            public void onCancelled(@NonNull DatabaseError error) {
                Log.e(TAG, "Database Error: " + error.getMessage());
                Toast.makeText(MyInformation.this, "Database Error: " + error.getMessage(), Toast.LENGTH_SHORT).show();
            }
        });
    }

    private void loadStudentData(String uid) {
        Log.d(TAG, "Loading student data for UID: " + uid);

        studentsRef.child(uid).addValueEventListener(new ValueEventListener() {
            @Override
            public void onDataChange(@NonNull DataSnapshot dataSnapshot) {
                try {
                    if (dataSnapshot.exists()) {
                        Log.d(TAG, "Student data found!");

                        // Get student data from Firebase
                        String firstNameText = safeGetString(dataSnapshot, "firstName");
                        String lastNameText = safeGetString(dataSnapshot, "lastName");
                        String middleNameText = safeGetString(dataSnapshot, "middleName");
                        String emailText = safeGetString(dataSnapshot, "email");
                        String idNumberText = safeGetString(dataSnapshot, "idNumber");
                        String yearLevelText = safeGetString(dataSnapshot, "yearLevel");
                        String sectionText = safeGetString(dataSnapshot, "section");

                        // If your database structure uses different field names (based on your screenshot)
                        if (firstNameText.equals("--")) {
                            firstNameText = safeGetString(dataSnapshot, "firstname");
                        }

                        if (lastNameText.equals("--")) {
                            lastNameText = safeGetString(dataSnapshot, "lastname");
                        }

                        if (middleNameText.equals("--")) {
                            middleNameText = safeGetString(dataSnapshot, "middleName");
                        }

                        if (idNumberText.equals("--")) {
                            idNumberText = safeGetString(dataSnapshot, "idNumber");
                        }

                        if (yearLevelText.equals("--")) {
                            yearLevelText = safeGetString(dataSnapshot, "yearLevel");
                        }

                        if (sectionText.equals("--")) {
                            sectionText = safeGetString(dataSnapshot, "section");
                        }

                        // Set data to UI elements
                        safeSetText(firstName, firstNameText);
                        safeSetText(lastName, lastNameText);
                        safeSetText(email, emailText);
                        safeSetText(userEmail, emailText);
                        safeSetText(idNumber, idNumberText);
                        safeSetText(yearLevel, yearLevelText);
                        safeSetText(section, sectionText);

                        // Set full name on top card
                        String fullName = (firstNameText != null ? firstNameText : "") + " " +
                                (middleNameText != null && !middleNameText.equals("--") ? middleNameText + " " : "") +
                                (lastNameText != null ? lastNameText : "");
                        safeSetText(userFullname, fullName.trim());

                        // Load profile image if available
                        if (profileImageView != null && dataSnapshot.hasChild("profileImage")) {
                            String profileImageUrl = dataSnapshot.child("profileImage").getValue(String.class);
                            if (profileImageUrl != null && !profileImageUrl.isEmpty()) {
                                Picasso.get().load(profileImageUrl)
                                        .placeholder(R.drawable.profile_placeholder)
                                        .error(R.drawable.profile_placeholder)
                                        .into(profileImageView);
                            }
                        }
                    } else {
                        Log.w(TAG, "Student data not found for UID: " + uid);
                        Toast.makeText(MyInformation.this, "Student information not found!", Toast.LENGTH_SHORT).show();
                    }
                } catch (Exception e) {
                    Log.e(TAG, "Error processing data: " + e.getMessage());
                    Toast.makeText(MyInformation.this, "Error processing student data", Toast.LENGTH_SHORT).show();
                }
            }

            @Override
            public void onCancelled(@NonNull DatabaseError databaseError) {
                Log.e(TAG, "Database Error: " + databaseError.getMessage());
                Toast.makeText(MyInformation.this, "Database Error: " + databaseError.getMessage(), Toast.LENGTH_SHORT).show();
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

    // Helper method to safely set text on TextViews
    private void safeSetText(TextView textView, String value) {
        if (textView != null) {
            textView.setText(value != null && !value.isEmpty() && !value.equals("null") ? value : "--");
        } else {
            Log.e(TAG, "Attempted to set text on null TextView");
        }
    }

    @Override
    public void onBackPressed() {
        // Override back button to add animation
        super.onBackPressed();
        overridePendingTransition(R.anim.slide_in_left, R.anim.slide_out_right);
    }
}