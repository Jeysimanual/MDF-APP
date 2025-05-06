package com.capstone.mdfeventmanagementsystem.Teacher;

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
import com.capstone.mdfeventmanagementsystem.Utilities.BaseActivity;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseUser;
import com.google.firebase.database.DataSnapshot;
import com.google.firebase.database.DatabaseError;
import com.google.firebase.database.DatabaseReference;
import com.google.firebase.database.FirebaseDatabase;
import com.google.firebase.database.ValueEventListener;
import com.squareup.picasso.Picasso;

public class TeacherInformation extends BaseActivity {

    private static final String TAG = "TeacherInformation";

    // UI Elements
    private TextView userFullname, userEmail;
    private TextView firstName, lastName, email, contactNumber, birthday, advisor, section;
    private ImageView profileImageView;

    // Firebase
    private FirebaseAuth mAuth;
    private DatabaseReference teachersRef;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        EdgeToEdge.enable(this);
        setContentView(R.layout.activity_teacher_information);
        ViewCompat.setOnApplyWindowInsetsListener(findViewById(R.id.main), (v, insets) -> {
            Insets systemBars = insets.getInsets(WindowInsetsCompat.Type.systemBars());
            v.setPadding(systemBars.left, systemBars.top, systemBars.right, systemBars.bottom);
            return insets;
        });

        // Initialize Firebase components
        mAuth = FirebaseAuth.getInstance();
        teachersRef = FirebaseDatabase.getInstance().getReference().child("teachers");

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
            firstName = findViewById(R.id.first_Name);
            lastName = findViewById(R.id.last_Name);
            email = findViewById(R.id.Email);
            contactNumber = findViewById(R.id.Number);
            birthday = findViewById(R.id.birthday);
            advisor = findViewById(R.id.advisor);
            section = findViewById(R.id.Section);

            // Profile image
            profileImageView = findViewById(R.id.profileImageView);

            // Profile image click listener
            findViewById(R.id.add_profile).setOnClickListener(v -> {
                Toast.makeText(TeacherInformation.this, "Profile image change feature to be implemented", Toast.LENGTH_SHORT).show();
            });

        } catch (Exception e) {
            Log.e(TAG, "Error initializing UI elements: " + e.getMessage());
            Toast.makeText(this, "Error initializing UI", Toast.LENGTH_SHORT).show();
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
                            loadTeacherData(teacherUid);
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
                    loadTeacherData(uid);
                } else {
                    Log.w(TAG, "No teacher record found for UID: " + uid);
                    Toast.makeText(TeacherInformation.this, "Your teacher profile was not found", Toast.LENGTH_LONG).show();

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
                Toast.makeText(TeacherInformation.this, "Database Error: " + error.getMessage(), Toast.LENGTH_SHORT).show();
            }
        });
    }

    private void loadTeacherData(String uid) {
        Log.d(TAG, "Loading teacher data for UID: " + uid);

        teachersRef.child(uid).addValueEventListener(new ValueEventListener() {
            @Override
            public void onDataChange(@NonNull DataSnapshot dataSnapshot) {
                try {
                    if (dataSnapshot.exists()) {
                        Log.d(TAG, "Teacher data found!");

                        // Use the Teacher model class to get the data
                        Teacher teacher = dataSnapshot.getValue(Teacher.class);

                        if (teacher != null) {
                            // Set data to views with null checks
                            safeSetText(firstName, teacher.getFirstname());
                            safeSetText(lastName, teacher.getLastname());
                            safeSetText(email, teacher.getEmail());
                            safeSetText(userEmail, teacher.getEmail());
                            safeSetText(contactNumber, teacher.getContact_number());

                            // For birthday which isn't in the Teacher class but may be in the database
                            safeSetText(birthday, safeGetString(dataSnapshot, "birthday"));

                            safeSetText(advisor, formatYearLevelAdvisor(teacher.getYear_level_advisor()));
                            safeSetText(section, teacher.getSection());
                            safeSetText(userFullname, teacher.getRole() != null ? teacher.getRole() : "Teacher");

                            // 👉 Set full name on top card
                            String fullName = (teacher.getFirstname() != null ? teacher.getFirstname() : "") + " " +
                                    (teacher.getLastname() != null ? teacher.getLastname() : "");
                            safeSetText(userFullname, fullName.trim());
                        } else {
                            // Fallback to direct field extraction if teacher object couldn't be parsed
                            String firstNameText = safeGetString(dataSnapshot, "firstname");
                            String lastNameText = safeGetString(dataSnapshot, "lastname");
                            String emailText = safeGetString(dataSnapshot, "email");
                            String phoneNumber = safeGetString(dataSnapshot, "contact_number");
                            String birthdayText = safeGetString(dataSnapshot, "birthday");
                            String yearLevelAdvisor = safeGetString(dataSnapshot, "year_level_advisor");
                            String sectionText = safeGetString(dataSnapshot, "section");
                            String role = safeGetString(dataSnapshot, "role");

                            safeSetText(firstName, firstNameText);
                            safeSetText(lastName, lastNameText);
                            safeSetText(email, emailText);
                            safeSetText(userEmail, emailText);
                            safeSetText(contactNumber, phoneNumber);
                            safeSetText(birthday, birthdayText);
                            safeSetText(advisor, formatYearLevelAdvisor(yearLevelAdvisor));
                            safeSetText(section, sectionText);
                            safeSetText(userFullname, role != null ? role : "Teacher");

                            // 👉 Set full name on top card (fallback)
                            String fullName = (firstNameText != null ? firstNameText : "") + " " +
                                    (lastNameText != null ? lastNameText : "");
                            safeSetText(userFullname, fullName.trim());
                        }

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
                        Log.w(TAG, "Teacher data not found for UID: " + uid);
                        Toast.makeText(TeacherInformation.this, "Teacher information not found!", Toast.LENGTH_SHORT).show();
                    }
                } catch (Exception e) {
                    Log.e(TAG, "Error processing data: " + e.getMessage());
                    Toast.makeText(TeacherInformation.this, "Error processing teacher data", Toast.LENGTH_SHORT).show();
                }
            }

            @Override
            public void onCancelled(@NonNull DatabaseError databaseError) {
                Log.e(TAG, "Database Error: " + databaseError.getMessage());
                Toast.makeText(TeacherInformation.this, "Database Error: " + databaseError.getMessage(), Toast.LENGTH_SHORT).show();
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

    // Helper method to format year level advisor
    private String formatYearLevelAdvisor(String yearLevel) {
        if (yearLevel == null || yearLevel.isEmpty() || yearLevel.equals("--")) {
            return "--";
        }

        // Check if the year level is just a number
        if (yearLevel.matches("\\d+")) {
            return "Grade " + yearLevel;
        }

        return yearLevel;
    }
}