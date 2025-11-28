package com.capstone.mdfeventmanagementsystem.Teacher;

import android.content.Intent;
import android.content.SharedPreferences;
import android.os.Bundle;
import android.util.Log;
import android.view.View;
import android.widget.Button;
import android.widget.ImageView;
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
import com.capstone.mdfeventmanagementsystem.Student.StudentLogin;
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

import android.graphics.Bitmap;
import com.squareup.picasso.Transformation;

class CircleTransformOutside implements Transformation {
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

public class TeacherProfile extends BaseActivity {
    private static final String TAG = "TeacherProfile";

    private LinearLayout btnMyInfo,btnChangePassword;
    private Button btnLogout;

    // Added for teacher information display
    private TextView txtFullname, txtUserEmail;
    private ImageView profileImageView; // Added ImageView for profile picture
    private DatabaseReference teachersRef;
    private DatabaseReference profilesRef; // Added reference to teacher_profiles
    private FirebaseAuth mAuth;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_teacher_profile);

        // Initialize Firebase components
        mAuth = FirebaseAuth.getInstance();
        teachersRef = FirebaseDatabase.getInstance().getReference().child("teachers");
        profilesRef = FirebaseDatabase.getInstance().getReference().child("teacher_profiles"); // Initialize profiles reference

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

    @Override
    protected void onResume() {
        super.onResume();
        // Reload teacher data when returning to this screen
        loadTeacherData();
    }

    private void initializeUI() {
        btnMyInfo = findViewById(R.id.btnMyInfo);
        btnChangePassword = findViewById(R.id.btnChangePassword);
        btnLogout = findViewById(R.id.btnLogout);

        // Initialize text views for teacher information
        txtFullname = findViewById(R.id.txtFullname);
        txtUserEmail = findViewById(R.id.txtUserEmail);

        // Initialize profile image view
        profileImageView = findViewById(R.id.imgProfile);
        if (profileImageView == null) {
            Log.e(TAG, "Profile ImageView not found in layout");
        }
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

        // Check for profile image in teacher_profiles
        checkProfileImage(uid);
    }

    private void checkProfileImage(String uid) {
        profilesRef.child(uid).addListenerForSingleValueEvent(new ValueEventListener() {
            @Override
            public void onDataChange(@NonNull DataSnapshot dataSnapshot) {
                if (dataSnapshot.exists() && dataSnapshot.hasChild("profileImage")) {
                    String profileImageUrl = dataSnapshot.child("profileImage").getValue(String.class);
                    if (profileImageUrl != null && !profileImageUrl.isEmpty()) {
                        Log.d(TAG, "Found profile image in teacher_profiles: " + profileImageUrl);
                        loadProfileImage(profileImageUrl);
                    }
                } else {
                    Log.d(TAG, "No profile image found in teacher_profiles, checking teachers collection");
                    // If no image in profiles, check the teachers collection
                    checkTeacherProfileImage(uid);
                }
            }

            @Override
            public void onCancelled(@NonNull DatabaseError error) {
                Log.e(TAG, "Error checking teacher_profiles: " + error.getMessage());
                // Fallback to teachers collection
                checkTeacherProfileImage(uid);
            }
        });
    }

    private void checkTeacherProfileImage(String uid) {
        teachersRef.child(uid).child("profileImage").addListenerForSingleValueEvent(new ValueEventListener() {
            @Override
            public void onDataChange(@NonNull DataSnapshot dataSnapshot) {
                if (dataSnapshot.exists()) {
                    String profileImageUrl = dataSnapshot.getValue(String.class);
                    if (profileImageUrl != null && !profileImageUrl.isEmpty()) {
                        Log.d(TAG, "Found profile image in teachers collection: " + profileImageUrl);
                        loadProfileImage(profileImageUrl);
                    } else {
                        Log.d(TAG, "Profile image field exists but is empty");
                        setDefaultProfileImage();
                    }
                } else {
                    Log.d(TAG, "No profile image in teachers collection");
                    setDefaultProfileImage();
                }
            }

            @Override
            public void onCancelled(@NonNull DatabaseError error) {
                Log.e(TAG, "Error checking teacher profile image: " + error.getMessage());
                setDefaultProfileImage();
            }
        });
    }

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
                    .transform(new CircleTransformOutside()) // Use the same CircleTransform as in TeacherInformation
                    .placeholder(R.drawable.profile_placeholder)
                    .error(R.drawable.profile_placeholder)
                    .networkPolicy(com.squareup.picasso.NetworkPolicy.NO_CACHE)
                    .memoryPolicy(com.squareup.picasso.MemoryPolicy.NO_CACHE, com.squareup.picasso.MemoryPolicy.NO_STORE)
                    .into(profileImageView, new com.squareup.picasso.Callback() {
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

    private void setDefaultProfileImage() {
        if (profileImageView != null) {
            profileImageView.setImageResource(R.drawable.profile_placeholder);
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
        Log.d("Logout", "Teacher logout started...");

        // 1. Get user info from SharedPreferences
        SharedPreferences prefs = getSharedPreferences("UserSession", MODE_PRIVATE);
        String userType = prefs.getString("userType", "");
        String teacherId = prefs.getString("teacherId", null);
        String studentID = prefs.getString("studentID", null);

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

        Log.d("TeacherProfile", "User logged out completely — FCM token cleaned");

        // 5. Show appropriate logout message
        String logoutMessage = "Logged out successfully";
        if ("teacher".equals(userType)) {
            logoutMessage = "Teacher logged out successfully";
        } else if ("student".equals(userType)) {
            logoutMessage = "Student logged out successfully";
        }
        Toast.makeText(TeacherProfile.this, logoutMessage, Toast.LENGTH_SHORT).show();

        // 6. Go back to StudentLogin (UNIFIED LOGIN)
        Intent intent = new Intent(TeacherProfile.this, StudentLogin.class);
        intent.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK | Intent.FLAG_ACTIVITY_CLEAR_TASK);
        startActivity(intent);
        finish();
    }
}