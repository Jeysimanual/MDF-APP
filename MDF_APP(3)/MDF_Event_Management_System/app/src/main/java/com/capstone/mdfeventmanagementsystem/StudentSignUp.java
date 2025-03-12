package com.capstone.mdfeventmanagementsystem;

import android.content.Intent;
import android.os.AsyncTask;
import android.os.Bundle;
import android.util.Log;
import android.widget.ArrayAdapter;
import android.widget.Button;
import android.widget.EditText;
import android.widget.Spinner;
import android.widget.Toast;

import androidx.activity.EdgeToEdge;
import androidx.appcompat.app.AppCompatActivity;

import com.google.firebase.database.DatabaseReference;
import com.google.firebase.database.FirebaseDatabase;

import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.util.Locale;

import javax.mail.MessagingException;

public class StudentSignUp extends AppCompatActivity {

    private EditText etFirstName, etMiddleName, etLastName, etEmail, etPassword, etConfirmPassword;
    private Spinner spinnerYearLevel;
    private Button btnSignUp;
    private DatabaseReference databaseReference;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        EdgeToEdge.enable(this);
        setContentView(R.layout.activity_student_sign_up);

        etFirstName = findViewById(R.id.etFirstName);
        etMiddleName = findViewById(R.id.etMiddleName);
        etLastName = findViewById(R.id.etLastName);
        etEmail = findViewById(R.id.etEmail);
        etPassword = findViewById(R.id.etPassword);
        etConfirmPassword = findViewById(R.id.etConfirmPassword);
        spinnerYearLevel = findViewById(R.id.spinnerYearLevel);
        btnSignUp = findViewById(R.id.btnSignUp);

        // Initialize Firebase
        databaseReference = FirebaseDatabase.getInstance().getReference("students");

        // Populate Spinner with year levels
        String[] yearLevels = {"Grade 7", "Grade 8", "Grade 9", "Grade 10", "Grade 11", "Grade 12"};
        ArrayAdapter<String> adapter = new ArrayAdapter<>(this, android.R.layout.simple_spinner_dropdown_item, yearLevels);
        spinnerYearLevel.setAdapter(adapter);

        btnSignUp.setOnClickListener(view -> validateAndRegister());
    }

    private void validateAndRegister() {
        String firstName = etFirstName.getText().toString().trim();
        String middleName = etMiddleName.getText().toString().trim();
        String lastName = etLastName.getText().toString().trim();
        String email = etEmail.getText().toString().trim();
        String password = etPassword.getText().toString();
        String confirmPassword = etConfirmPassword.getText().toString();
        String yearLevel = spinnerYearLevel.getSelectedItem().toString();

        if (firstName.isEmpty() || lastName.isEmpty() || email.isEmpty() || password.isEmpty() || confirmPassword.isEmpty()) {
            Toast.makeText(this, "Please fill in all required fields!", Toast.LENGTH_SHORT).show();
            return;
        }

        if (!android.util.Patterns.EMAIL_ADDRESS.matcher(email).matches()) {
            Toast.makeText(this, "Enter a valid email address!", Toast.LENGTH_SHORT).show();
            return;
        }

        if (!password.equals(confirmPassword)) {
            Toast.makeText(this, "Passwords do not match!", Toast.LENGTH_SHORT).show();
            return;
        }

        Log.d("TestApp", "Valid inputs received. Proceeding with OTP verification.");
        saveStudentData(firstName, middleName, lastName, email, password, yearLevel);
    }

    private void saveStudentData(String firstName, String middleName, String lastName, String email, String password, String yearLevel) {
        String studentId = databaseReference.push().getKey(); // Generate unique ID for student
        if (studentId == null) return;

        int otp = (int) (Math.random() * 900000) + 100000; // Generate 6-digit OTP
        String hashedOtp = hashOtp(String.valueOf(otp));

        Student student = new Student(firstName, middleName, lastName, email, yearLevel, "student", false, hashedOtp);

        // Save student data in Firebase Realtime Database (WITHOUT AUTHENTICATION)
        databaseReference.child(studentId).setValue(student)
                .addOnSuccessListener(aVoid -> {
                    sendOtpEmail(email, firstName, otp);
                    navigateToOtpVerification(studentId, email, password);
                })
                .addOnFailureListener(e -> {
                    Toast.makeText(StudentSignUp.this, "Failed to save student data!", Toast.LENGTH_SHORT).show();
                });
    }

    // Function to Hash OTP using SHA-256
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

    private void navigateToOtpVerification(String studentId, String email, String password) {
        Log.d("TestApp", "Navigating to OtpVerificationActivity with studentId: " + studentId);
        Intent intent = new Intent(StudentSignUp.this, OtpVerificationActivity.class);
        intent.putExtra("STUDENT_ID", studentId);
        intent.putExtra("EMAIL", email);
        intent.putExtra("PASSWORD", password);
        startActivity(intent);
        finish();
    }

    private void sendOtpEmail(String recipientEmail, String firstName, int otp) {
        String subject = "Your MDF Event Verification Code";
        String message = "Dear " + firstName + ",\n\n"
                + "Your verification code is: " + otp + "\n\n"
                + "Enter this code in the app to verify your email.\n\n"
                + "Best Regards,\nMDF Event Management Team";

        AsyncTask.execute(() -> {
            try {
                MailSender mailSender = new MailSender();
                mailSender.sendEmail(recipientEmail, subject, message);
                Log.d("TestApp", "OTP Email Sent Successfully");
            } catch (MessagingException e) {
                Log.e("TestApp", "Failed to send OTP email: " + e.getMessage(), e);
            }
        });
    }
}