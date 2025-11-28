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
import com.capstone.mdfeventmanagementsystem.Teacher.TeacherDashboard;
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
    private DatabaseReference studentRef, teacherRef;
    private ProgressBar loginProgressBar;
    private static final String TAG = "StudentLoginTest";

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        SharedPreferences sharedPreferences = getSharedPreferences("UserSession", MODE_PRIVATE);
        boolean isLoggedIn = sharedPreferences.getBoolean("isLoggedIn", false);
        String userType = sharedPreferences.getString("userType", "");

        // Check if user is already logged in (either student or teacher)
        if (isLoggedIn) {
            if ("student".equals(userType)) {
                String studentID = sharedPreferences.getString("studentID", null);
                if (studentID == null) {
                    Log.e(TAG, "No studentID found in SharedPreferences!");
                    Toast.makeText(this, "Student ID not found. Please log in again.", Toast.LENGTH_SHORT).show();
                    Intent intent = new Intent(StudentLogin.this, StudentLogin.class);
                    startActivity(intent);
                    finish();
                    return;
                }
                Log.d(TAG, "Student session found. Redirecting to Student Dashboard...");
                Intent intent = new Intent(StudentLogin.this, MainActivity2.class);
                startActivity(intent);
                finish();
                return;
            } else if ("teacher".equals(userType)) {
                String teacherId = sharedPreferences.getString("teacherId", null);
                if (teacherId == null) {
                    Log.e(TAG, "No teacherId found in SharedPreferences!");
                    Toast.makeText(this, "Teacher ID not found. Please log in again.", Toast.LENGTH_SHORT).show();
                    Intent intent = new Intent(StudentLogin.this, StudentLogin.class);
                    startActivity(intent);
                    finish();
                    return;
                }
                Log.d(TAG, "Teacher session found. Redirecting to Teacher Dashboard...");
                Intent intent = new Intent(StudentLogin.this, TeacherDashboard.class);
                startActivity(intent);
                finish();
                return;
            }
        }

        EdgeToEdge.enable(this);
        setContentView(R.layout.activity_student_login);

        etEmail = findViewById(R.id.etEmail);
        etPassword = findViewById(R.id.etPassword);
        btnLogin = findViewById(R.id.btnLogin);
        tvSignUp = findViewById(R.id.tvSignUp);

        loginProgressBar = findViewById(R.id.loginProgressBar);

        mAuth = FirebaseAuth.getInstance();
        studentRef = FirebaseDatabase.getInstance().getReference("students");
        teacherRef = FirebaseDatabase.getInstance().getReference("teachers");

        setPasswordToggle();

        btnLogin.setOnClickListener(v -> loginUser());
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

    private void loginUser() {
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
                        identifyUserRole(user.getEmail());
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

    private void identifyUserRole(String userEmail) {
        Log.d(TAG, "Identifying user role for: " + userEmail);

        // First check if user is a teacher
        teacherRef.get().addOnCompleteListener(teacherTask -> {
            if (teacherTask.isSuccessful()) {
                DataSnapshot teacherSnapshot = teacherTask.getResult();
                boolean teacherFound = false;

                if (teacherSnapshot.exists()) {
                    // Manually search through all teachers
                    for (DataSnapshot teacherData : teacherSnapshot.getChildren()) {
                        String emailInDatabase = teacherData.child("email").getValue(String.class);
                        if (emailInDatabase != null && emailInDatabase.equalsIgnoreCase(userEmail)) {
                            // User is a teacher
                            String teacherId = teacherData.getKey();
                            String role = teacherData.child("role").getValue(String.class);
                            Boolean verified = teacherData.child("verified").getValue(Boolean.class);

                            if ("teacher".equals(role)) {
                                if (verified == null || !verified) {
                                    setLoading(false);
                                    Log.w(TAG, "Teacher account not verified for: " + teacherId);
                                    Toast.makeText(StudentLogin.this, "Your teacher account is not verified", Toast.LENGTH_SHORT).show();
                                    return;
                                }

                                Log.d(TAG, "User identified as TEACHER: " + teacherId);
                                saveTeacherSession(teacherId, userEmail);
                                updateTeacherFcmToken(teacherId);
                                navigateToTeacherDashboard();
                                teacherFound = true;
                                return;
                            }
                        }
                    }
                }

                if (!teacherFound) {
                    // If not a teacher, check if user is a student
                    checkIfStudent(userEmail);
                }
            } else {
                Log.e(TAG, "Failed to query teachers: " + (teacherTask.getException() != null ? teacherTask.getException().getMessage() : "Unknown error"));
                // Even if teacher query fails, still check if user is a student
                checkIfStudent(userEmail);
            }
        });
    }

    private void checkIfStudent(String userEmail) {
        studentRef.orderByChild("email").equalTo(userEmail).get().addOnCompleteListener(studentTask -> {
            if (studentTask.isSuccessful()) {
                DataSnapshot studentSnapshot = studentTask.getResult();
                if (studentSnapshot.exists()) {
                    DataSnapshot firstMatch = null;
                    int matchCount = 0;
                    for (DataSnapshot studentData : studentSnapshot.getChildren()) {
                        String emailInDatabase = studentData.child("email").getValue(String.class);
                        if (emailInDatabase != null && emailInDatabase.equalsIgnoreCase(userEmail)) {
                            firstMatch = studentData;
                            matchCount++;
                        }
                    }

                    if (matchCount > 1) {
                        Log.w(TAG, "Multiple studentIDs found for email: " + userEmail);
                    }

                    if (firstMatch != null) {
                        String studentID = firstMatch.getKey();
                        String yearLevel = firstMatch.child("yearLevel").getValue(String.class);
                        Log.d(TAG, "User identified as STUDENT: " + studentID);
                        saveStudentSession(studentID, userEmail);
                        if (yearLevel != null) {
                            SharedPreferences userSession = getSharedPreferences("UserSession", MODE_PRIVATE);
                            userSession.edit().putString("yearLevel", yearLevel).apply();
                        }
                        updateStudentFcmToken(studentID);
                        navigateToStudentDashboard();
                    } else {
                        Log.e(TAG, "No matching studentID found for email: " + userEmail);
                        Toast.makeText(this, "User account not found", Toast.LENGTH_SHORT).show();
                        setLoading(false);
                    }
                } else {
                    Log.e(TAG, "No student account found for email: " + userEmail);
                    Toast.makeText(this, "No account found with this email", Toast.LENGTH_SHORT).show();
                    setLoading(false);
                }
            } else {
                Log.e(TAG, "Failed to fetch student data: " + (studentTask.getException() != null ? studentTask.getException().getMessage() : "Unknown error"));
                Toast.makeText(this, "Failed to fetch user data. Please check your connection.", Toast.LENGTH_SHORT).show();
                setLoading(false);
            }
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

    private void saveTeacherSession(String teacherId, String email) {
        SharedPreferences sharedPreferences = getSharedPreferences("UserSession", MODE_PRIVATE);
        SharedPreferences.Editor editor = sharedPreferences.edit();
        editor.putBoolean("isLoggedIn", true);
        editor.putString("userType", "teacher");
        editor.putString("teacherId", teacherId);
        editor.putString("email", email);
        editor.apply();
        Log.d(TAG, "Teacher session saved: " + teacherId + ", email: " + email);
    }

    private void updateStudentFcmToken(String studentID) {
        Log.d(TAG, "updateStudentFcmToken() STARTED for student: " + studentID);

        if (studentID == null) {
            Log.e(TAG, "Cannot save FCM token — studentID is NULL!");
            return;
        }

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
                    Log.d(TAG, "Saving FCM token to: /students/" + studentID + "/fcmToken");

                    studentRef.child(studentID).child("fcmToken")
                            .setValue(token)
                            .addOnSuccessListener(aVoid -> {
                                Log.d(TAG, "FCM TOKEN SAVED SUCCESSFULLY for student!");
                            })
                            .addOnFailureListener(e -> {
                                Log.e(TAG, "FAILED to save FCM token for student", e);
                            });
                });
    }

    private void updateTeacherFcmToken(String teacherId) {
        Log.d(TAG, "updateTeacherFcmToken() STARTED for teacher: " + teacherId);

        if (teacherId == null) {
            Log.e(TAG, "Cannot save FCM token — teacherId is NULL!");
            return;
        }

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
                    Log.d(TAG, "Saving FCM token to: /teachers/" + teacherId + "/fcmToken");

                    teacherRef.child(teacherId).child("fcmToken")
                            .setValue(token)
                            .addOnSuccessListener(aVoid -> {
                                Log.d(TAG, "FCM TOKEN SAVED SUCCESSFULLY for teacher!");
                            })
                            .addOnFailureListener(e -> {
                                Log.e(TAG, "FAILED to save FCM token for teacher", e);
                            });
                });
    }

    private void navigateToStudentDashboard() {
        Log.d(TAG, "Navigating to Student Dashboard.");

        Toast.makeText(StudentLogin.this, "Login Successfully!", Toast.LENGTH_SHORT).show();

        new android.os.Handler(Looper.getMainLooper()).postDelayed(() -> {
            Intent intent = new Intent(StudentLogin.this, MainActivity2.class);
            intent.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK | Intent.FLAG_ACTIVITY_CLEAR_TASK);
            startActivity(intent);
            finish();
        }, 800);
    }

    private void navigateToTeacherDashboard() {
        Log.d(TAG, "Navigating to Teacher Dashboard.");

        Toast.makeText(StudentLogin.this, "Login Successfully!", Toast.LENGTH_SHORT).show();

        new android.os.Handler(Looper.getMainLooper()).postDelayed(() -> {
            Intent intent = new Intent(StudentLogin.this, TeacherDashboard.class);
            intent.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK | Intent.FLAG_ACTIVITY_CLEAR_TASK);
            startActivity(intent);
            finish();
        }, 800);
    }
}