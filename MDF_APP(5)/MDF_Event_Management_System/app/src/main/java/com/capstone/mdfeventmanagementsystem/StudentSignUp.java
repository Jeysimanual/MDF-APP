package com.capstone.mdfeventmanagementsystem;

import android.content.Intent;
import android.content.SharedPreferences;
import android.os.AsyncTask;
import android.os.Bundle;
import android.util.Log;
import android.view.View;
import android.widget.AdapterView;
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
import java.util.HashMap;
import java.util.Locale;
import java.util.Map;

import javax.mail.MessagingException;

public class StudentSignUp extends AppCompatActivity {

    private EditText etFirstName, etMiddleName, etLastName, etEmail, etPassword, etConfirmPassword;
    private Spinner spinnerYearLevel, spinnerSection;
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
        spinnerSection = findViewById(R.id.spinnerSection);
        btnSignUp = findViewById(R.id.btnSignUp);

        // Initialize Firebase
        databaseReference = FirebaseDatabase.getInstance().getReference("students");

        // Populate Year Level Spinner
        String[] yearLevels = {"Grade 7", "Grade 8", "Grade 9", "Grade 10", "Grade 11", "Grade 12"};
        ArrayAdapter<String> yearAdapter = new ArrayAdapter<>(this, android.R.layout.simple_spinner_dropdown_item, yearLevels);
        spinnerYearLevel.setAdapter(yearAdapter);

        // Define Sections for each Year Level
        Map<String, String[]> sectionsMap = new HashMap<>();
        sectionsMap.put("Grade 7", new String[]{"Einstein"});
        sectionsMap.put("Grade 8", new String[]{"Euclid"});
        sectionsMap.put("Grade 9", new String[]{"Plato"});
        sectionsMap.put("Grade 10", new String[]{"Newton"});
        sectionsMap.put("Grade 11", new String[]{"Moses (STEM)", "Jonah (ABM)", "Esther (HUMSS)"});
        sectionsMap.put("Grade 12", new String[]{"Abraham (STEM)", "David (STEM)", "Job (HUMSS)"});

        // Update Section Spinner when Year Level changes
        spinnerYearLevel.setOnItemSelectedListener(new AdapterView.OnItemSelectedListener() {
            @Override
            public void onItemSelected(AdapterView<?> parent, View view, int position, long id) {
                String selectedYear = parent.getItemAtPosition(position).toString();
                String[] sections = sectionsMap.getOrDefault(selectedYear, new String[]{});

                ArrayAdapter<String> sectionAdapter = new ArrayAdapter<>(StudentSignUp.this, android.R.layout.simple_spinner_dropdown_item, sections);
                spinnerSection.setAdapter(sectionAdapter);
            }

            @Override
            public void onNothingSelected(AdapterView<?> parent) {
                spinnerSection.setAdapter(null);
            }
        });

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
        String section = spinnerSection.getSelectedItem().toString();

        if (firstName.isEmpty() || lastName.isEmpty() || email.isEmpty() || password.isEmpty() || confirmPassword.isEmpty() || section.isEmpty()) {
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
        saveStudentData(firstName, middleName, lastName, email, password, yearLevel, section);
    }

    private void saveStudentData(String firstName, String middleName, String lastName, String email, String password, String yearLevel, String section) {
        String studentId = databaseReference.push().getKey();
        if (studentId == null) return;

        int otp = (int) (Math.random() * 900000) + 100000; // Generate 6-digit OTP
        String hashedOtp = hashOtp(String.valueOf(otp));

        // Convert Year Level format before storing in Firebase
        String firebaseYearLevel = yearLevel.replace(" ", "-");

        Student student = new Student(firstName, middleName, lastName, email, firebaseYearLevel, section, "student", false, hashedOtp);

        databaseReference.child(studentId).setValue(student)
                .addOnSuccessListener(aVoid -> {
                    // Save studentID in SharedPreferences
                    SharedPreferences sharedPreferences = getSharedPreferences("MyAppPrefs", MODE_PRIVATE);
                    SharedPreferences.Editor editor = sharedPreferences.edit();
                    editor.putString("studentID", studentId);
                    editor.apply();

                    sendOtpEmail(email, firstName, otp);
                    navigateToOtpVerification(studentId, email, password);
                })
                .addOnFailureListener(e -> {
                    Toast.makeText(StudentSignUp.this, "Failed to save student data!", Toast.LENGTH_SHORT).show();
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
