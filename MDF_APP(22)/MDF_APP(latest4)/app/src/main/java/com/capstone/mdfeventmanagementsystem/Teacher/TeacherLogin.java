package com.capstone.mdfeventmanagementsystem.Teacher;

import android.content.Intent;
import android.content.SharedPreferences;
import android.os.Bundle;
import android.widget.Button;
import android.widget.EditText;
import android.widget.ImageView;
import android.widget.TextView;
import android.widget.Toast;

import androidx.appcompat.app.AppCompatActivity;

import com.capstone.mdfeventmanagementsystem.MainActivity;
import com.capstone.mdfeventmanagementsystem.R;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseUser;
import com.google.firebase.database.DataSnapshot;
import com.google.firebase.database.DatabaseError;
import com.google.firebase.database.DatabaseReference;
import com.google.firebase.database.FirebaseDatabase;
import com.google.firebase.database.ValueEventListener;

public class TeacherLogin extends AppCompatActivity {

    private EditText etEmail, etPassword;
    private Button btnLogin;
    private FirebaseAuth auth;
    private DatabaseReference databaseReference;

    private ImageView backBtn;
    private TextView backText;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_teacher_login);

        etEmail = findViewById(R.id.etEmail);
        etPassword = findViewById(R.id.etPassword);
        btnLogin = findViewById(R.id.btnLogin);
        backBtn = findViewById(R.id.backBtn);       // 🆕 Back button
        backText = findViewById(R.id.back_text);    // 🆕 Back text
        auth = FirebaseAuth.getInstance();
        databaseReference = FirebaseDatabase.getInstance().getReference("teachers");

        // 🔥 Check if user is already logged in (and is a teacher)
        SharedPreferences sharedPreferences = getSharedPreferences("UserSession", MODE_PRIVATE);
        boolean isLoggedIn = sharedPreferences.getBoolean("isLoggedIn", false);
        String userType = sharedPreferences.getString("userType", "");

        if (isLoggedIn && "teacher".equals(userType)) {
            Intent intent = new Intent(TeacherLogin.this, TeacherDashboard.class);
            startActivity(intent);
            finish(); // Prevents back navigation
            return;
        }

        btnLogin.setOnClickListener(v -> loginTeacher());
        // ✅ Back button and text click listener with transition
        backBtn.setOnClickListener(v -> goBackToMain());
        backText.setOnClickListener(v -> goBackToMain());
    }

    private void loginTeacher() {
        String email = etEmail.getText().toString().trim();
        String password = etPassword.getText().toString().trim();

        if (email.isEmpty() || password.isEmpty()) {
            Toast.makeText(this, "Please enter both email and password", Toast.LENGTH_SHORT).show();
            return;
        }

        // Check if email exists in the "teachers" collection
        databaseReference.orderByChild("email").equalTo(email)
                .addListenerForSingleValueEvent(new ValueEventListener() {
                    @Override
                    public void onDataChange(DataSnapshot dataSnapshot) {
                        if (dataSnapshot.exists()) {
                            for (DataSnapshot snapshot : dataSnapshot.getChildren()) {
                                String role = snapshot.child("role").getValue(String.class);

                                if ("teacher".equals(role)) {
                                    // Authenticate with FirebaseAuth
                                    auth.signInWithEmailAndPassword(email, password)
                                            .addOnCompleteListener(task -> {
                                                if (task.isSuccessful()) {
                                                    FirebaseUser user = auth.getCurrentUser();
                                                    if (user != null) {
                                                        // 🔥 Save login session & user type
                                                        // 🔥 Save login session & user type
                                                        saveSession("teacher");

                                                        // ✅ Start NotificationService
                                                        Intent serviceIntent = new Intent(TeacherLogin.this, com.capstone.mdfeventmanagementsystem.Utilities.NotificationService.class);
                                                        startService(serviceIntent);

                                                        // 🔥 Redirect to TeacherDashboard
                                                        Intent intent = new Intent(TeacherLogin.this, TeacherDashboard.class);
                                                        intent.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK | Intent.FLAG_ACTIVITY_CLEAR_TASK);
                                                        startActivity(intent);
                                                        finish();

                                                    }
                                                } else {
                                                    Toast.makeText(TeacherLogin.this, "Authentication Failed: " + task.getException().getMessage(), Toast.LENGTH_SHORT).show();
                                                }
                                            });
                                } else {
                                    Toast.makeText(TeacherLogin.this, "You are not authorized as a Teacher", Toast.LENGTH_SHORT).show();
                                }
                            }
                        } else {
                            Toast.makeText(TeacherLogin.this, "No teacher found with this email", Toast.LENGTH_SHORT).show();
                        }
                    }

                    @Override
                    public void onCancelled(DatabaseError databaseError) {
                        Toast.makeText(TeacherLogin.this, "Database error: " + databaseError.getMessage(), Toast.LENGTH_SHORT).show();
                    }
                });
    }

    /** ✅ Saves login session & userType in SharedPreferences */
    private void saveSession(String userType) {
        SharedPreferences sharedPreferences = getSharedPreferences("UserSession", MODE_PRIVATE);
        SharedPreferences.Editor editor = sharedPreferences.edit();
        editor.putBoolean("isLoggedIn", true);
        editor.putString("userType", userType); // Save user type
        editor.apply();
    }
    /** ✅ Back button functionality with left-to-right transition */
    private void goBackToMain() {
        Intent intent = new Intent(TeacherLogin.this, MainActivity.class);
        startActivity(intent);
        overridePendingTransition(R.anim.slide_in_left, R.anim.slide_out_right); // 🌀 Smooth back animation
        finish();
    }
}
