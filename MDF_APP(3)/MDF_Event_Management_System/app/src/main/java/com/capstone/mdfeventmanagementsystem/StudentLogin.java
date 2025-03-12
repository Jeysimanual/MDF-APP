package com.capstone.mdfeventmanagementsystem;

import android.content.Intent;
import android.os.Bundle;
import android.util.Log;
import android.view.View;
import android.widget.Button;
import android.widget.EditText;
import android.widget.TextView;
import android.widget.Toast;

import androidx.activity.EdgeToEdge;
import androidx.appcompat.app.AppCompatActivity;

import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseUser;

public class StudentLogin extends AppCompatActivity {

    private EditText etEmail, etPassword;
    private Button btnLogin;
    private TextView tvSignUp;
    private FirebaseAuth mAuth;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        EdgeToEdge.enable(this);
        setContentView(R.layout.activity_student_login);

        etEmail = findViewById(R.id.etEmail);
        etPassword = findViewById(R.id.etPassword);
        btnLogin = findViewById(R.id.btnLogin);
        tvSignUp = findViewById(R.id.tvSignUp);

        mAuth = FirebaseAuth.getInstance(); // Initialize Firebase Authentication

        btnLogin.setOnClickListener(v -> loginStudent());
        tvSignUp.setOnClickListener(v -> {
            Intent intent = new Intent(StudentLogin.this, StudentSignUp.class);
            startActivity(intent);
        });
    }

    private void loginStudent() {
        String email = etEmail.getText().toString().trim();
        String password = etPassword.getText().toString();

        if (email.isEmpty() || password.isEmpty()) {
            Toast.makeText(this, "Please enter both email and password", Toast.LENGTH_SHORT).show();
            return;
        }

        Log.d("TestApp", "Attempting to log in user: " + email);

        mAuth.signInWithEmailAndPassword(email, password)
                .addOnSuccessListener(authResult -> {
                    FirebaseUser user = mAuth.getCurrentUser();
                    if (user != null) {
                        Log.d("TestApp", "Login successful for user: " + user.getEmail());
                        Toast.makeText(StudentLogin.this, "Login successful", Toast.LENGTH_SHORT).show();
                        navigateToDashboard();
                    } else {
                        Log.e("TestApp", "Login failed: User object is null.");
                        Toast.makeText(StudentLogin.this, "Login failed. Try again.", Toast.LENGTH_SHORT).show();
                    }
                })
                .addOnFailureListener(e -> {
                    Log.e("TestApp", "Login failed: " + e.getMessage(), e);
                    Toast.makeText(StudentLogin.this, "Login failed: " + e.getMessage(), Toast.LENGTH_SHORT).show();
                });
    }

    private void navigateToDashboard() {
        Log.d("TestApp", "Navigating to StudentDashboard.");
        Intent intent = new Intent(StudentLogin.this, StudentDashboard.class);
        startActivity(intent);
        finish(); // Prevent going back to login screen
    }
}
