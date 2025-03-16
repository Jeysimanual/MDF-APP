package com.capstone.mdfeventmanagementsystem;

import android.content.Intent;
import android.os.Bundle;
import android.util.Log;
import android.widget.Button;
import android.widget.EditText;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;

import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseUser;
import com.google.firebase.database.DataSnapshot;
import com.google.firebase.database.DatabaseError;
import com.google.firebase.database.DatabaseReference;
import com.google.firebase.database.FirebaseDatabase;
import com.google.firebase.database.ValueEventListener;

import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.util.Locale;

public class OtpVerificationActivity extends AppCompatActivity {
    private EditText etOtp;
    private Button btnVerify;
    private DatabaseReference databaseReference;
    private FirebaseAuth auth;
    private String studentId, email, password;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_otp_verification);

        etOtp = findViewById(R.id.etOtp);
        btnVerify = findViewById(R.id.btnVerifyOtp);

        // Get data from Intent
        studentId = getIntent().getStringExtra("STUDENT_ID");
        email = getIntent().getStringExtra("EMAIL");
        password = getIntent().getStringExtra("PASSWORD");

        // Firebase Initialization
        databaseReference = FirebaseDatabase.getInstance().getReference("students");
        auth = FirebaseAuth.getInstance();

        btnVerify.setOnClickListener(view -> checkOtp());
    }

    private void checkOtp() {
        String enteredOtp = etOtp.getText().toString().trim();

        if (enteredOtp.isEmpty()) {
            Toast.makeText(this, "Please enter the OTP", Toast.LENGTH_SHORT).show();
            return;
        }

        databaseReference.child(studentId).addListenerForSingleValueEvent(new ValueEventListener() {
            @Override
            public void onDataChange(@NonNull DataSnapshot snapshot) {
                if (!snapshot.exists()) {
                    Toast.makeText(OtpVerificationActivity.this, "User not found!", Toast.LENGTH_SHORT).show();
                    return;
                }

                String storedHashedOtp = snapshot.child("otp").getValue(String.class);
                if (storedHashedOtp == null) {
                    Toast.makeText(OtpVerificationActivity.this, "Error retrieving OTP. Try again!", Toast.LENGTH_SHORT).show();
                    return;
                }

                if (!storedHashedOtp.equals(hashOtp(enteredOtp))) {
                    Toast.makeText(OtpVerificationActivity.this, "Incorrect OTP! Please try again.", Toast.LENGTH_SHORT).show();
                    return;
                }

                // OTP matches, create an account in Firebase Authentication
                registerUserInAuth();
            }

            @Override
            public void onCancelled(@NonNull DatabaseError error) {
                Toast.makeText(OtpVerificationActivity.this, "Error fetching OTP. Try again!", Toast.LENGTH_SHORT).show();
            }
        });
    }

    private void registerUserInAuth() {
        auth.createUserWithEmailAndPassword(email, password)
                .addOnCompleteListener(task -> {
                    if (task.isSuccessful()) {
                        // Update verification status in Firebase Database
                        databaseReference.child(studentId).child("isVerified").setValue(true)
                                .addOnSuccessListener(aVoid -> {
                                    Toast.makeText(OtpVerificationActivity.this, "Email Verified! You can now log in.", Toast.LENGTH_LONG).show();
                                    navigateToLogin();
                                })
                                .addOnFailureListener(e ->
                                        Toast.makeText(OtpVerificationActivity.this, "Verification failed. Try again!", Toast.LENGTH_SHORT).show()
                                );
                    } else {
                        Toast.makeText(OtpVerificationActivity.this, "Error creating account. " + task.getException().getMessage(), Toast.LENGTH_LONG).show();
                    }
                });
    }

    private String hashOtp(String otp) {
        try {
            MessageDigest digest = MessageDigest.getInstance("SHA-256");
            byte[] hash = digest.digest(otp.getBytes());
            StringBuilder hexString = new StringBuilder();
            for (byte b : hash) {
                hexString.append(String.format(Locale.US, "%02x", b));
            }
            return hexString.toString();
        } catch (NoSuchAlgorithmException e) {
            e.printStackTrace();
            return null;
        }
    }

    private void navigateToLogin() {
        Intent intent = new Intent(OtpVerificationActivity.this, StudentLogin.class);
        startActivity(intent);
        finish();
    }
}
