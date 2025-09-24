package com.capstone.mdfeventmanagementsystem.Utilities;

import android.content.Intent;
import android.os.Bundle;
import android.os.CountDownTimer;
import android.util.Log;
import android.widget.Button;
import android.widget.EditText;
import android.widget.TextView;
import android.widget.Toast;

import androidx.appcompat.app.AppCompatActivity;

import com.capstone.mdfeventmanagementsystem.R;
import com.capstone.mdfeventmanagementsystem.Student.StudentLogin;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.database.DatabaseReference;
import com.google.firebase.database.FirebaseDatabase;

import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.util.Locale;

import javax.mail.MessagingException;

public class OtpVerificationActivity extends AppCompatActivity {
    private EditText etOtp;
    private Button btnVerify;
    private TextView tvResendOtp;
    private DatabaseReference databaseReference;
    private FirebaseAuth auth;
    private String studentId, email, password;
    private CountDownTimer countDownTimer;
    private static final long RESEND_OTP_DELAY = 60000; // 1 minute
    private static final String TAG = "OtpVerification";

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_otp_verification);

        etOtp = findViewById(R.id.etOtp);
        btnVerify = findViewById(R.id.btnVerifyOtp);
        tvResendOtp = findViewById(R.id.tvResendOtp);

        // Get data from Intent
        studentId = getIntent().getStringExtra("STUDENT_ID");
        email = getIntent().getStringExtra("EMAIL");
        password = getIntent().getStringExtra("PASSWORD");

        // Firebase Initialization
        databaseReference = FirebaseDatabase.getInstance().getReference("students");
        auth = FirebaseAuth.getInstance();

        btnVerify.setOnClickListener(view -> checkOtp());
        tvResendOtp.setOnClickListener(view -> resendOtp());

        startResendOtpCountdown(); // Start countdown when activity starts
    }

    private void startResendOtpCountdown() {
        // Cancel any existing countdown
        if (countDownTimer != null) {
            countDownTimer.cancel();
        }

        tvResendOtp.setEnabled(false);  // Disable button
        tvResendOtp.setText("Resend OTP in 1:00");

        countDownTimer = new CountDownTimer(RESEND_OTP_DELAY, 1000) { // 1-minute countdown
            @Override
            public void onTick(long millisUntilFinished) {
                long seconds = (millisUntilFinished / 1000) % 60;
                long minutes = (millisUntilFinished / 1000) / 60;
                tvResendOtp.setText(String.format(Locale.getDefault(), "Resend OTP in %d:%02d", minutes, seconds));
            }

            @Override
            public void onFinish() {
                tvResendOtp.setText("Didn't receive OTP? Resend");
                tvResendOtp.setEnabled(true);  // Enable button
            }
        }.start();
    }

    private void resendOtp() {
        tvResendOtp.setEnabled(false);  // Prevent multiple clicks
        int newOtp = (int) (Math.random() * 900000) + 100000; // Generate new 6-digit OTP
        String hashedOtp = hashOtp(String.valueOf(newOtp));

        databaseReference.child(studentId).get().addOnSuccessListener(snapshot -> {
            if (!snapshot.exists()) {
                Toast.makeText(OtpVerificationActivity.this, "User not found!", Toast.LENGTH_SHORT).show();
                tvResendOtp.setEnabled(true); // Re-enable if user not found
                return;
            }

            final String firstName = snapshot.child("firstName").getValue(String.class); // Make final
            String resolvedFirstName = (firstName != null && !firstName.isEmpty()) ? firstName : "User"; // Handle null case

            // Update OTP in Firebase
            databaseReference.child(studentId).child("otp").setValue(hashedOtp)
                    .addOnSuccessListener(aVoid -> {
                        sendOtpEmail(email, newOtp, resolvedFirstName); // Pass first name
                        startResendOtpCountdown(); // Restart countdown after sending OTP
                    })
                    .addOnFailureListener(e -> {
                        Toast.makeText(OtpVerificationActivity.this, "Failed to resend OTP!", Toast.LENGTH_SHORT).show();
                        tvResendOtp.setEnabled(true);
                    });

        }).addOnFailureListener(e -> {
            Toast.makeText(OtpVerificationActivity.this, "Error fetching user details!", Toast.LENGTH_SHORT).show();
            tvResendOtp.setEnabled(true);
        });
    }



    private void sendOtpEmail(String recipientEmail, int otp, String firstName) {
        String subject = "Your MDF Event Verification Code";
        String message = "Dear " + firstName + ",\n\n"
                + "Your new verification code is: " + otp + "\n\n"
                + "Enter this code in the app to verify your email.\n\n"
                + "Best Regards,\nMDF Event Management Team";

        new Thread(() -> {
            try {
                MailSender mailSender = new MailSender();
                mailSender.sendEmail(recipientEmail, subject, message);
                Log.d(TAG, "OTP Email Sent Successfully");
            } catch (MessagingException e) {
                Log.e(TAG, "Failed to send OTP email: " + e.getMessage(), e);
            }
        }).start();
    }



    private void checkOtp() {
        String enteredOtp = etOtp.getText().toString().trim();

        if (enteredOtp.isEmpty()) {
            Toast.makeText(this, "Please enter the OTP", Toast.LENGTH_SHORT).show();
            return;
        }

        databaseReference.child(studentId).get().addOnSuccessListener(snapshot -> {
            if (!snapshot.exists()) {
                Toast.makeText(OtpVerificationActivity.this, "User not found!", Toast.LENGTH_SHORT).show();
                return;
            }

            String storedHashedOtp = snapshot.child("otp").getValue(String.class);
            if (storedHashedOtp == null || !storedHashedOtp.equals(hashOtp(enteredOtp))) {
                Toast.makeText(OtpVerificationActivity.this, "Incorrect OTP! Please try again.", Toast.LENGTH_SHORT).show();
                return;
            }

            registerUserInAuth();
        }).addOnFailureListener(e ->
                Toast.makeText(OtpVerificationActivity.this, "Error verifying OTP. Try again!", Toast.LENGTH_SHORT).show()
        );
    }

    private void registerUserInAuth() {
        auth.createUserWithEmailAndPassword(email, password)
                .addOnCompleteListener(task -> {
                    if (task.isSuccessful()) {
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

    @Override
    protected void onDestroy() {
        if (countDownTimer != null) {
            countDownTimer.cancel();
        }
        super.onDestroy();
    }
}
