package com.capstone.mdfeventmanagementsystem.Student;

import android.annotation.SuppressLint;
import android.content.Intent;
import android.content.SharedPreferences;
import android.os.Bundle;
import android.os.Looper;
import android.text.method.PasswordTransformationMethod;
import android.util.Log;
import android.view.MotionEvent;
import android.view.View;
import android.widget.Button;
import android.widget.EditText;
import android.widget.ImageView;
import android.widget.ProgressBar;
import android.widget.TextView;
import android.widget.Toast;
import com.google.firebase.messaging.FirebaseMessaging;

import androidx.activity.EdgeToEdge;
import androidx.appcompat.app.AppCompatActivity;

import com.capstone.mdfeventmanagementsystem.MainActivity;
import com.capstone.mdfeventmanagementsystem.R;
import com.capstone.mdfeventmanagementsystem.Utilities.BaseActivity;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseUser;
import com.google.firebase.database.DataSnapshot;
import com.google.firebase.database.DatabaseReference;
import com.google.firebase.database.FirebaseDatabase;

public class StudentLogin extends BaseActivity {

    private EditText etEmail, etPassword;
    private Button btnLogin;
    private TextView tvSignUp;
    private FirebaseAuth mAuth;
    private DatabaseReference studentRef;
    private ProgressBar loginProgressBar;
    private static final String TAG = "StudentLoginTest";


    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        SharedPreferences sharedPreferences = getSharedPreferences("UserSession", MODE_PRIVATE);
        boolean isLoggedIn = sharedPreferences.getBoolean("isLoggedIn", false);
        String userType = sharedPreferences.getString("userType", "");

        if (isLoggedIn && "student".equals(userType)) {
            String studentID = sharedPreferences.getString("studentID", null);

            if (studentID == null) {
                Log.e(TAG, "No studentID found in SharedPreferences!");
                Toast.makeText(this, "Student ID not found. Please log in again.", Toast.LENGTH_SHORT).show();
                Intent intent = new Intent(StudentLogin.this, StudentLogin.class);
                startActivity(intent);
                finish();
                return;
            }

            Log.d(TAG, "User session found. Redirecting to Student Dashboard...");
            Intent intent = new Intent(StudentLogin.this, MainActivity2.class);
            startActivity(intent);
            finish();
            return;
        }

        EdgeToEdge.enable(this);
        setContentView(R.layout.activity_student_login);

        etEmail = findViewById(R.id.etEmail);
        etPassword = findViewById(R.id.etPassword);
        btnLogin = findViewById(R.id.btnLogin);
        tvSignUp = findViewById(R.id.tvSignUp);
        ImageView backBtn = findViewById(R.id.backBtn);
        TextView backText = findViewById(R.id.back_text);
        loginProgressBar = findViewById(R.id.loginProgressBar);

        mAuth = FirebaseAuth.getInstance();

        setPasswordToggle();

        btnLogin.setOnClickListener(v -> loginStudent());
        tvSignUp.setOnClickListener(v -> {
            Intent intent = new Intent(StudentLogin.this, StudentSignUp.class);
            startActivity(intent);
        });

        View.OnClickListener goBackListener = v -> {
            Intent intent = new Intent(StudentLogin.this, MainActivity.class);
            startActivity(intent);
            overridePendingTransition(R.anim.slide_in_left, R.anim.slide_out_right);
            finish();
        };

        backBtn.setOnClickListener(goBackListener);
        backText.setOnClickListener(goBackListener);

        TextView forgotPassText = findViewById(R.id.tvForgotPassword);
        forgotPassText.setOnClickListener(view -> {
            Intent intent = new Intent(this, StudentForgetPass.class);
            startActivity(intent);
        });
    }

    @SuppressLint("ClickableViewAccessibility")
    private void setPasswordToggle() {
        etPassword.setOnTouchListener((v, event) -> {
            if (event.getAction() == MotionEvent.ACTION_UP) {
                int drawableRightIndex = 2;
                if (etPassword.getCompoundDrawablesRelative()[drawableRightIndex] != null) {
                    int drawableWidth = etPassword.getCompoundDrawablesRelative()[drawableRightIndex].getBounds().width();
                    int touchAreaStart = etPassword.getRight() - etPassword.getPaddingEnd() - drawableWidth;

                    if (event.getRawX() >= touchAreaStart) {
                        togglePasswordVisibility();
                        return true;
                    }
                }
            }
            return false;
        });
    }

    private void togglePasswordVisibility() {
        if (etPassword.getTransformationMethod() instanceof PasswordTransformationMethod) {
            etPassword.setTransformationMethod(null);
            etPassword.setCompoundDrawablesRelativeWithIntrinsicBounds(0, 0, R.drawable.view, 0);
        } else {
            etPassword.setTransformationMethod(PasswordTransformationMethod.getInstance());
            etPassword.setCompoundDrawablesRelativeWithIntrinsicBounds(0, 0, R.drawable.hide, 0);
        }
        etPassword.setSelection(etPassword.getText().length());
    }

    private void loginStudent() {
        String email = etEmail.getText().toString().trim();
        String password = etPassword.getText().toString();

        if (email.isEmpty() || password.isEmpty()) {
            Toast.makeText(this, "Incorrect email or password. Please try again.", Toast.LENGTH_SHORT).show();
            return;
        }

        setLoading(true);

        Log.d(TAG, "Attempting to log in user: " + email);

        mAuth.signInWithEmailAndPassword(email, password)
                .addOnSuccessListener(authResult -> {
                    FirebaseUser user = mAuth.getCurrentUser();
                    if (user != null) {
                        Log.d(TAG, "Login successful for user: " + user.getEmail());
                        fetchStudentID(user.getEmail());
                    } else {
                        Log.e(TAG, "Login failed: User object is null.");
                        Toast.makeText(StudentLogin.this, "Login unsuccessful. Please try again.", Toast.LENGTH_SHORT).show();
                        setLoading(false);
                    }
                })
                .addOnFailureListener(e -> {
                    Log.e(TAG, "Login failed: " + e.getMessage(), e);
                    Toast.makeText(StudentLogin.this, "Invalid email or password. Please try again.", Toast.LENGTH_SHORT).show();
                    setLoading(false);
                });
    }

    private void setLoading(boolean isLoading) {
        if (isLoading) {
            loginProgressBar.setVisibility(View.VISIBLE);
            btnLogin.setText("");
        } else {
            loginProgressBar.setVisibility(View.GONE);
            btnLogin.setText("Login");
        }
        btnLogin.setEnabled(!isLoading);
    }

    private void fetchStudentID(String userEmail) {
        studentRef = FirebaseDatabase.getInstance().getReference("students");
        Log.d(TAG, "Querying database at path: " + studentRef.toString());
        // Use .get() for a one-time server query to avoid cache
        studentRef.orderByChild("email").equalTo(userEmail).get().addOnCompleteListener(task -> {
            if (task.isSuccessful()) {
                DataSnapshot dataSnapshot = task.getResult();
                if (dataSnapshot.exists()) {
                    DataSnapshot firstMatch = null;
                    int matchCount = 0;
                    for (DataSnapshot studentSnapshot : dataSnapshot.getChildren()) {
                        String emailInDatabase = studentSnapshot.child("email").getValue(String.class);
                        if (emailInDatabase != null && emailInDatabase.equalsIgnoreCase(userEmail)) {
                            firstMatch = studentSnapshot;
                            matchCount++;
                        }
                    }
                    if (matchCount > 1) {
                        Log.w(TAG, "Multiple studentIDs found for email: " + userEmail);
                    }
                    if (firstMatch != null) {
                        String studentID = firstMatch.getKey();
                        String yearLevel = firstMatch.child("yearLevel").getValue(String.class);
                        saveStudentSession(studentID, userEmail);
                        if (yearLevel != null) {
                            SharedPreferences userSession = getSharedPreferences("UserSession", MODE_PRIVATE);
                            userSession.edit().putString("yearLevel", yearLevel).apply();
                        }
                        navigateToDashboard();
                    } else {
                        Log.e(TAG, "No matching studentID found for email: " + userEmail);
                        Toast.makeText(this, "Student ID not found for this email", Toast.LENGTH_SHORT).show();
                        setLoading(false);
                    }
                } else {
                    Log.e(TAG, "No data found for email: " + userEmail);
                    Toast.makeText(this, "Student data not found", Toast.LENGTH_SHORT).show();
                    setLoading(false);
                }
            } else {
                Log.e(TAG, "Failed to fetch student list: " + (task.getException() != null ? task.getException().getMessage() : "Unknown error"));
                Toast.makeText(this, "Failed to fetch student data. Please check your connection.", Toast.LENGTH_SHORT).show();
                setLoading(false);
            }
        });
    }

    private void saveStudentSession(String studentID, String email) {
        SharedPreferences sharedPreferences = getSharedPreferences("UserSession", MODE_PRIVATE);
        SharedPreferences.Editor editor = sharedPreferences.edit();
        editor.putBoolean("isLoggedIn", true);
        editor.putString("userType", "student");
        editor.putString("studentID", studentID);
        editor.putString("email", email);
        editor.apply();
        Log.d(TAG, "Student session saved: " + studentID + ", email: " + email);
    }


    private void updateFcmToken() {
        Log.d(TAG, "updateFcmToken() STARTED — using studentID from SharedPreferences");

        // 1. Get studentID from session (you already saved it during login)
        SharedPreferences prefs = getSharedPreferences("UserSession", MODE_PRIVATE);
        String studentID = prefs.getString("studentID", null);

        if (studentID == null) {
            Log.e(TAG, "Cannot save FCM token — studentID is NULL in SharedPreferences!");
            Toast.makeText(this, "Error: Student ID not found", Toast.LENGTH_SHORT).show();
            return;
        }

        // 2. Get FCM token
        FirebaseMessaging.getInstance().getToken()
                .addOnCompleteListener(task -> {
                    if (!task.isSuccessful()) {
                        Log.w(TAG, "Fetching FCM token failed", task.getException());
                        return;
                    }

                    String token = task.getResult();
                    if (token == null) {
                        Log.e(TAG, "FCM token is NULL");
                        return;
                    }

                    Log.d(TAG, "FCM Token generated: " + token.substring(0, 30) + "...");
                    Log.d(TAG, "StudentID from session: " + studentID);
                    Log.d(TAG, "Saving FCM token to: /students/" + studentID + "/fcmToken");

                    // 3. SAVE TOKEN UNDER THE REAL STUDENT NODE (not Auth UID!)
                    FirebaseDatabase.getInstance()
                            .getReference("students")
                            .child(studentID)
                            .child("fcmToken")
                            .setValue(token)
                            .addOnSuccessListener(aVoid -> {
                                Log.d(TAG, "FCM TOKEN SAVED SUCCESSFULLY!");
                                Log.d(TAG, "Location: /students/" + studentID + "/fcmToken");
                            })
                            .addOnFailureListener(e -> {
                                Log.e(TAG, "FAILED to save FCM token", e);
                                Toast.makeText(StudentLogin.this, "Notification setup failed", Toast.LENGTH_LONG).show();
                            });
                });
    }

    private void navigateToDashboard() {
        Log.d(TAG, "Navigating to Student Dashboard.");
        updateFcmToken();

        // Success Toast - matches your Teacher logout style
        Toast.makeText(StudentLogin.this, "Login Successfully!", Toast.LENGTH_SHORT).show();

        // Small delay so the toast is clearly visible before screen changes
        new android.os.Handler(Looper.getMainLooper()).postDelayed(() -> {
            Intent intent = new Intent(StudentLogin.this, MainActivity2.class);
            intent.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK | Intent.FLAG_ACTIVITY_CLEAR_TASK);
            startActivity(intent);
            finish();
        }, 800);
    }
}