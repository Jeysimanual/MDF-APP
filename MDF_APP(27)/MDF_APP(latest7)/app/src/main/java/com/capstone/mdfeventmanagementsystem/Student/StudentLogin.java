package com.capstone.mdfeventmanagementsystem.Student;

import android.annotation.SuppressLint;
import android.content.Intent;
import android.content.SharedPreferences;
import android.os.Bundle;
import android.text.method.PasswordTransformationMethod;
import android.util.Log;
import android.view.MotionEvent;
import android.view.View;
import android.widget.Button;
import android.widget.EditText;
import android.widget.ImageView;
import android.widget.TextView;
import android.widget.Toast;

import androidx.activity.EdgeToEdge;
import androidx.appcompat.app.AppCompatActivity;

import com.capstone.mdfeventmanagementsystem.MainActivity;
import com.capstone.mdfeventmanagementsystem.R;
import com.capstone.mdfeventmanagementsystem.Utilities.NotificationService;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseUser;
import com.google.firebase.database.DataSnapshot;
import com.google.firebase.database.DatabaseReference;
import com.google.firebase.database.FirebaseDatabase;

public class StudentLogin extends AppCompatActivity {

    private EditText etEmail, etPassword;
    private Button btnLogin;
    private TextView tvSignUp;
    private FirebaseAuth mAuth;
    private DatabaseReference studentRef;

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
            Toast.makeText(this, "Please enter both email and password", Toast.LENGTH_SHORT).show();
            return;
        }

        Log.d(TAG, "Attempting to log in user: " + email);

        mAuth.signInWithEmailAndPassword(email, password)
                .addOnSuccessListener(authResult -> {
                    FirebaseUser user = mAuth.getCurrentUser();
                    if (user != null) {
                        Log.d(TAG, "Login successful for user: " + user.getEmail());
                        fetchStudentID(user.getEmail());
                    } else {
                        Log.e(TAG, "Login failed: User object is null.");
                        Toast.makeText(StudentLogin.this, "Login failed. Try again.", Toast.LENGTH_SHORT).show();
                    }
                })
                .addOnFailureListener(e -> {
                    Log.e(TAG, "Login failed: " + e.getMessage(), e);
                    Toast.makeText(StudentLogin.this, "Login failed: " + e.getMessage(), Toast.LENGTH_SHORT).show();
                });
    }

    private void fetchStudentID(String userEmail) {
        studentRef = FirebaseDatabase.getInstance().getReference("students");

        studentRef.get().addOnCompleteListener(task -> {
            if (task.isSuccessful() && task.getResult().exists()) {
                for (DataSnapshot studentSnapshot : task.getResult().getChildren()) {
                    String emailInDatabase = studentSnapshot.child("email").getValue(String.class);

                    if (emailInDatabase != null && emailInDatabase.equalsIgnoreCase(userEmail)) {
                        String studentID = studentSnapshot.getKey();
                        String yearLevel = studentSnapshot.child("yearLevel").getValue(String.class); // ✅

                        saveStudentSession(studentID, userEmail);


                        // ✅ Save yearLevel in UserSession
                        if (yearLevel != null) {
                            SharedPreferences userSession = getSharedPreferences("UserSession", MODE_PRIVATE);
                            userSession.edit().putString("yearLevel", yearLevel).apply();
                            Log.d(TAG, "Saved yearLevel in UserSession: " + yearLevel);
                        } else {
                            Log.w(TAG, "yearLevel is null for studentID: " + studentID);
                        }

                        navigateToDashboard();
                        return;
                    }
                }
                Log.e(TAG, "No matching studentID found for email: " + userEmail);
                Toast.makeText(this, "Student ID not found for this email", Toast.LENGTH_SHORT).show();
            } else {
                Log.e(TAG, "Failed to fetch student list from Firebase.");
                Toast.makeText(this, "Failed to fetch student data", Toast.LENGTH_SHORT).show();
            }
        });
    }

    private void saveStudentSession(String studentID, String email) {
        SharedPreferences sharedPreferences = getSharedPreferences("UserSession", MODE_PRIVATE);
        SharedPreferences.Editor editor = sharedPreferences.edit();
        editor.putBoolean("isLoggedIn", true);
        editor.putString("userType", "student");
        editor.putString("studentID", studentID);
        editor.putString("email", email); // ✅ Save email
        editor.apply();
        Log.d(TAG, "Student session saved in SharedPreferences: " + studentID + ", email: " + email);
    }


    private void navigateToDashboard() {
        Log.d(TAG, "Navigating to Student Dashboard.");

        // ✅ Start the NotificationService
        Intent serviceIntent = new Intent(StudentLogin.this, NotificationService.class);
        startService(serviceIntent);


        Intent intent = new Intent(StudentLogin.this, MainActivity2.class);
        startActivity(intent);
        finish();
    }

}
