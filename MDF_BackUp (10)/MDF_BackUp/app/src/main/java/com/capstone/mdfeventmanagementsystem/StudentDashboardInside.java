package com.capstone.mdfeventmanagementsystem;

import android.annotation.SuppressLint;
import android.content.Intent;
import android.content.SharedPreferences;
import android.graphics.Bitmap;
import android.os.Bundle;
import android.util.Log;
import android.view.View;
import android.widget.Button;
import android.widget.ImageView;
import android.widget.TextView;
import android.widget.Toast;

import androidx.appcompat.app.AppCompatActivity;

import com.bumptech.glide.Glide;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseUser;
import com.google.firebase.database.DatabaseReference;
import com.google.firebase.database.FirebaseDatabase;

import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.HashMap;
import java.util.Locale;

public class StudentDashboardInside extends AppCompatActivity {

    private TextView eventName, eventDescription, startDate, endDate, startTime, endTime, venue, eventSpan, ticketType, graceTime;
    private ImageView eventImage;
    private Button registerButton, ticketButton;

    private FirebaseAuth mAuth;
    private DatabaseReference studentTicketsRef;
    private String eventUID; // Store event UID as a variable
    private String studentID; // Store the student ID retrieved from SharedPreferences

    @SuppressLint("MissingInflatedId")
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_student_dashboard_inside);

        // Initialize UI elements
        eventName = findViewById(R.id.eventName);
        eventDescription = findViewById(R.id.eventDescription);
        startDate = findViewById(R.id.startDate);
        endDate = findViewById(R.id.endDate);
        startTime = findViewById(R.id.startTime);
        endTime = findViewById(R.id.endTime);
        venue = findViewById(R.id.venue);
        eventSpan = findViewById(R.id.eventSpan);
        graceTime = findViewById(R.id.graceTime);
        eventImage = findViewById(R.id.eventPhotoUrl);
        registerButton = findViewById(R.id.registerButton);
        ticketButton = findViewById(R.id.ticketButton);

        // ✅ Add new TextViews for eventType and eventFor
        TextView eventType = findViewById(R.id.eventType);
        TextView eventFor = findViewById(R.id.eventFor);

        mAuth = FirebaseAuth.getInstance();
        FirebaseUser currentUser = mAuth.getCurrentUser();

        if (currentUser == null) {
            Toast.makeText(this, "User not authenticated!", Toast.LENGTH_SHORT).show();
            finish();
            return;
        }

        // Retrieve studentID from SharedPreferences
        SharedPreferences sharedPreferences = getSharedPreferences("MyAppPrefs", MODE_PRIVATE);
        studentID = sharedPreferences.getString("studentID", null);

        if (studentID == null || studentID.isEmpty()) {
            Toast.makeText(this, "Student ID not found!", Toast.LENGTH_SHORT).show();
            Log.e("TestApp", "No studentID found in SharedPreferences!");
            finish();
            return;
        }

        Log.d("TestApp", "Using studentID from SharedPreferences: " + studentID);

        // ✅ Get event details from intent and display them
        Intent intent = getIntent();
        eventUID = intent.getStringExtra("eventUID");
        eventName.setText(intent.getStringExtra("eventName"));
        eventDescription.setText(intent.getStringExtra("eventDescription"));
        startDate.setText(intent.getStringExtra("startDate"));
        endDate.setText(intent.getStringExtra("endDate"));
        startTime.setText(intent.getStringExtra("startTime"));
        endTime.setText(intent.getStringExtra("endTime"));
        venue.setText(intent.getStringExtra("venue"));
        eventSpan.setText(intent.getStringExtra("eventSpan"));
        graceTime.setText(intent.getStringExtra("graceTime"));

        // ✅ Display the new fields: eventType and eventFor
        eventType.setText(intent.getStringExtra("eventType"));
        eventFor.setText(intent.getStringExtra("eventFor"));

        Log.d("TestApp", "Received Event UID: " + eventUID);

        // Load event image using Glide
        String eventPhotoUrl = intent.getStringExtra("eventPhotoUrl");
        if (eventPhotoUrl != null && !eventPhotoUrl.isEmpty()) {
            Glide.with(this)
                    .load(eventPhotoUrl)
                    .placeholder(R.drawable.placeholder_image)
                    .error(R.drawable.error_image)
                    .into(eventImage);
        } else {
            eventImage.setImageResource(R.drawable.placeholder_image);
        }

        // Register button click
        registerButton.setOnClickListener(v -> registerForEvent());
    }


    private void registerForEvent() {
        if (eventUID == null || eventUID.isEmpty()) {
            Toast.makeText(this, "Event ID is missing!", Toast.LENGTH_SHORT).show();
            return;
        }

        DatabaseReference studentRef = FirebaseDatabase.getInstance().getReference("students").child(studentID);
        DatabaseReference ticketRef = studentRef.child("tickets").child(eventUID);

        ticketRef.get().addOnCompleteListener(ticketTask -> {
            if (ticketTask.isSuccessful() && ticketTask.getResult().exists()) {
                Toast.makeText(this, "You are already registered for this event!", Toast.LENGTH_SHORT).show();
                Log.d("TestApp", "Student already registered for event: " + eventUID);
                updateButtonState(true);
            } else {
                long currentTimeMillis = System.currentTimeMillis();
                String formattedTimestamp = getCurrentTimestamp();

                HashMap<String, Object> ticketData = new HashMap<>();
                ticketData.put("registeredAt", formattedTimestamp);
                ticketData.put("timestampMillis", currentTimeMillis);

                ticketRef.setValue(ticketData)
                        .addOnSuccessListener(aVoid -> {
                            Toast.makeText(this, "Registered successfully!", Toast.LENGTH_SHORT).show();
                            Log.d("TestApp", "Event registered at " + formattedTimestamp);
                            generateAndUploadQRCode();
                            updateButtonState(true);
                        })
                        .addOnFailureListener(e -> {
                            Toast.makeText(this, "Registration failed: " + e.getMessage(), Toast.LENGTH_SHORT).show();
                            Log.e("TestApp", "Error registering event: " + e.getMessage());
                        });
            }
        });
    }

    private void checkRegistrationStatus() {
        DatabaseReference ticketRef = FirebaseDatabase.getInstance()
                .getReference("students")
                .child(studentID)
                .child("tickets")
                .child(eventUID);

        ticketRef.get().addOnCompleteListener(task -> {
            if (task.isSuccessful() && task.getResult().exists()) {
                updateButtonState(true);
            } else {
                updateButtonState(false);
            }
        });
    }

    private void updateButtonState(boolean isRegistered) {
        if (isRegistered) {
            registerButton.setVisibility(View.GONE);
            ticketButton.setVisibility(View.VISIBLE);
            ticketButton.setClickable(true);
        } else {
            registerButton.setVisibility(View.VISIBLE);
            ticketButton.setVisibility(View.GONE);
            ticketButton.setClickable(false);
        }
    }

    private void generateAndUploadQRCode() {
        QrCodeGenerator.generateQRCodeWithEventAndStudentInfo(this, eventUID, new QrCodeGenerator.OnQRCodeGeneratedListener() {
            @Override
            public void onQRCodeGenerated(Bitmap qrCodeBitmap) {
                Log.d("TestApp", "QR Code successfully generated.");
            }

            @Override
            public void onQRCodeUploaded(String downloadUrl, String ticketID) { // Updated to include ticketID
                // Reference to the student’s ticket entry
                DatabaseReference ticketRef = FirebaseDatabase.getInstance()
                        .getReference("students")
                        .child(studentID)
                        .child("tickets")
                        .child(eventUID);

                // Add ticketID, QR code URL, and "pending" status
                HashMap<String, Object> ticketData = new HashMap<>();
                ticketData.put("ticketID", ticketID); // Save the ticket ID
                ticketData.put("qrCodeUrl", downloadUrl);
                ticketData.put("status", "pending"); // Add the "pending" status

                ticketRef.updateChildren(ticketData)
                        .addOnSuccessListener(aVoid -> {
                            Toast.makeText(StudentDashboardInside.this, "QR Code saved and status set to pending!", Toast.LENGTH_SHORT).show();
                            Log.d("TestApp", "QR Code URL and status saved in Firebase: " + downloadUrl);
                        })
                        .addOnFailureListener(e -> {
                            Toast.makeText(StudentDashboardInside.this, "Failed to save QR Code URL and status!", Toast.LENGTH_SHORT).show();
                            Log.e("TestApp", "Error saving QR Code URL and status: " + e.getMessage());
                        });
            }

            @Override
            public void onError(String errorMessage) {
                Toast.makeText(StudentDashboardInside.this, "QR Code Generation Failed: " + errorMessage, Toast.LENGTH_SHORT).show();
                Log.e("TestApp", "QR Code Generation Error: " + errorMessage);
            }
        });
    }



    /**
     * Returns the current timestamp in the format MM/dd/yyyy hh:mm a (e.g., 03/14/2025 5:00 AM)
     */
    private String getCurrentTimestamp() {
        SimpleDateFormat sdf = new SimpleDateFormat("MM/dd/yyyy hh:mm a", Locale.getDefault());
        return sdf.format(new Date());
    }
}
