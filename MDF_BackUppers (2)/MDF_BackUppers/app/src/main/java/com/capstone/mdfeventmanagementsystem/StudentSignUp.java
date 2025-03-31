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
import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;

import com.google.firebase.database.DataSnapshot;
import com.google.firebase.database.DatabaseError;
import com.google.firebase.database.DatabaseReference;
import com.google.firebase.database.FirebaseDatabase;
import com.google.firebase.database.ValueEventListener;

import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;

import javax.mail.MessagingException;

public class StudentSignUp extends AppCompatActivity {

    private static final String TAG = "SignUpTest";

    private EditText idNumber, etFirstName, etMiddleName, etLastName, etEmail, etPassword, etConfirmPassword;
    private Spinner spinnerYearLevel, spinnerSection;
    private Button btnSignUp;
    private DatabaseReference databaseReference, yearLvlsReference;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        EdgeToEdge.enable(this);
        setContentView(R.layout.activity_student_sign_up);

        idNumber = findViewById(R.id.idNumber);
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
        yearLvlsReference = FirebaseDatabase.getInstance().getReference("yearLvls");

        // Populate Year Level Spinner
        String[] yearLevels = {"Grade 7", "Grade 8", "Grade 9", "Grade 10", "Grade 11", "Grade 12"};
        ArrayAdapter<String> yearAdapter = new ArrayAdapter<>(this, android.R.layout.simple_spinner_dropdown_item, yearLevels);
        spinnerYearLevel.setAdapter(yearAdapter);

        // Fetch sections dynamically from Firebase when Year Level changes
        spinnerYearLevel.setOnItemSelectedListener(new AdapterView.OnItemSelectedListener() {
            @Override
            public void onItemSelected(AdapterView<?> parent, View view, int position, long id) {
                String selectedYear = parent.getItemAtPosition(position).toString().replace(" ", "-"); // Ensure it matches Firebase keys
                Log.d(TAG, "Year level selected: " + selectedYear);

                DatabaseReference sectionsRef = FirebaseDatabase.getInstance().getReference("yearLvls").child(selectedYear);
                Log.d(TAG, "Fetching sections for: " + selectedYear);

                sectionsRef.addListenerForSingleValueEvent(new ValueEventListener() {
                    @Override
                    public void onDataChange(DataSnapshot snapshot) {
                        if (snapshot.exists()) {
                            List<String> sectionsList = new ArrayList<>();

                            for (DataSnapshot sectionSnapshot : snapshot.getChildren()) {
                                String sectionName = sectionSnapshot.getValue(String.class);
                                sectionsList.add(sectionName);
                            }

                            Log.d(TAG, "Sections retrieved for " + selectedYear + ": " + sectionsList);

                            // Populate Section Spinner
                            ArrayAdapter<String> sectionAdapter = new ArrayAdapter<>(StudentSignUp.this, android.R.layout.simple_spinner_dropdown_item, sectionsList);
                            spinnerSection.setAdapter(sectionAdapter);
                        } else {
                            Log.w(TAG, "No sections found for " + selectedYear);
                            spinnerSection.setAdapter(null);
                        }
                    }

                    @Override
                    public void onCancelled(DatabaseError databaseError) {
                        Log.e(TAG, "Error fetching sections: " + databaseError.getMessage());
                    }
                });
            }


            @Override
            public void onNothingSelected(AdapterView<?> parent) {
                spinnerSection.setAdapter(null);
            }
        });

        btnSignUp.setOnClickListener(view -> validateAndRegister());
    }



    private void validateAndRegister() {
        Log.d(TAG, "validateAndRegister() called");

        String studentIdNumber = idNumber.getText().toString().trim();
        String firstName = etFirstName.getText().toString().trim();
        String middleName = etMiddleName.getText().toString().trim();
        String lastName = etLastName.getText().toString().trim();
        String email = etEmail.getText().toString().trim();
        String password = etPassword.getText().toString();
        String confirmPassword = etConfirmPassword.getText().toString();
        String yearLevel = spinnerYearLevel.getSelectedItem() != null ? spinnerYearLevel.getSelectedItem().toString() : "";
        String section = spinnerSection.getSelectedItem() != null ? spinnerSection.getSelectedItem().toString() : "";

        Log.d(TAG, "User input: ID=" + studentIdNumber + ", First Name=" + firstName + ", Last Name=" + lastName +
                ", Email=" + email + ", Year Level=" + yearLevel + ", Section=" + section);

        if (studentIdNumber.isEmpty() || firstName.isEmpty() || lastName.isEmpty() || email.isEmpty()
                || password.isEmpty() || confirmPassword.isEmpty() || yearLevel.isEmpty() || section.isEmpty()) {
            Log.w(TAG, "Validation failed: Missing required fields");
            Toast.makeText(this, "Please fill in all required fields!", Toast.LENGTH_SHORT).show();
            return;
        }

        if (!android.util.Patterns.EMAIL_ADDRESS.matcher(email).matches()) {
            Log.w(TAG, "Validation failed: Invalid email format");
            Toast.makeText(this, "Enter a valid email address!", Toast.LENGTH_SHORT).show();
            return;
        }

        if (!password.equals(confirmPassword)) {
            Log.w(TAG, "Validation failed: Passwords do not match");
            Toast.makeText(this, "Passwords do not match!", Toast.LENGTH_SHORT).show();
            return;
        }

        Log.d(TAG, "Validation passed. Checking student existence in Firebase...");

        // Validate student existence in Firebase
        DatabaseReference studentsRef = FirebaseDatabase.getInstance().getReference("students");

        studentsRef.addListenerForSingleValueEvent(new ValueEventListener() {
            @Override
            public void onDataChange(@NonNull DataSnapshot snapshot) {
                boolean found = false;

                for (DataSnapshot studentSnapshot : snapshot.getChildren()) {
                    Student student = studentSnapshot.getValue(Student.class);

                    if (student != null && studentIdNumber.equals(student.getIdNumber())) {
                        found = true;
                        String studentKey = studentSnapshot.getKey();
                        String studentYearLevel = student.getYearLevel();
                        String studentSection = student.getSection();

                        Log.d(TAG, "Checking student record: ID=" + student.getIdNumber() +
                                ", Year Level=" + studentYearLevel + ", Section=" + studentSection);

                        if (studentYearLevel.trim().equalsIgnoreCase(yearLevel.trim()) &&
                                studentSection.trim().equalsIgnoreCase(section.trim())) {

                            Log.d(TAG, "Match found! Updating student record...");
                            updateStudentData(studentKey, firstName, middleName, lastName, email, password);
                            return;
                        } else {
                            Log.d(TAG, "Mismatch: Found ID but year level/section does not match.");
                        }
                    }
                }

                if (!found) {
                    Log.w(TAG, "No matching student record found. Registration denied.");
                    Toast.makeText(StudentSignUp.this, "No matching student record found. Please check ID, Year Level, and Section.", Toast.LENGTH_LONG).show();
                }
            }

            @Override
            public void onCancelled(@NonNull DatabaseError error) {
                Log.e(TAG, "Database error: " + error.getMessage());
                Toast.makeText(StudentSignUp.this, "Failed to validate student data!", Toast.LENGTH_SHORT).show();
            }
        });
    }

    // Update student data instead of creating a new record
    private void updateStudentData(String studentKey, String firstName, String middleName, String lastName, String email, String password) {
        int otp = (int) (Math.random() * 900000) + 100000; // Generate 6-digit OTP
        String hashedOtp = hashOtp(String.valueOf(otp));

        // Format fullName as "LastName, FirstName M."
        String middleInitial = middleName.isEmpty() ? "" : middleName.substring(0, 1).toUpperCase() + ".";
        String fullName = lastName + ", " + firstName + (middleInitial.isEmpty() ? "" : " " + middleInitial);

        Map<String, Object> studentUpdates = new HashMap<>();
        studentUpdates.put("firstName", firstName);
        studentUpdates.put("middleName", middleName);
        studentUpdates.put("lastName", lastName);
        studentUpdates.put("fullName", fullName); // Add fullName field
        studentUpdates.put("email", email);
        studentUpdates.put("role", "student");
        studentUpdates.put("isVerified", false);
        studentUpdates.put("otp", hashedOtp);

        databaseReference.child(studentKey).updateChildren(studentUpdates)
                .addOnSuccessListener(aVoid -> {
                    Log.d(TAG, "Student data successfully updated!");

                    SharedPreferences sharedPreferences = getSharedPreferences("MyAppPrefs", MODE_PRIVATE);
                    SharedPreferences.Editor editor = sharedPreferences.edit();
                    editor.putString("studentID", studentKey);
                    editor.putString("idNumber", studentKey);
                    editor.apply();

                    sendOtpEmail(email, firstName, otp);
                    navigateToOtpVerification(studentKey, email, password);
                })
                .addOnFailureListener(e -> {
                    Log.e(TAG, "Failed to update student data: " + e.getMessage());
                    Toast.makeText(StudentSignUp.this, "Failed to update student data!", Toast.LENGTH_SHORT).show();
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
            } catch (MessagingException e) {
                Log.e(TAG, "Failed to send OTP email: " + e.getMessage(), e);
            }
        });
    }
}
