package com.capstone.mdfeventmanagementsystem;

import android.Manifest;
import android.app.NotificationChannel;
import android.app.NotificationManager;
import android.content.Intent;
import android.content.SharedPreferences;
import android.content.pm.PackageManager;
import android.os.Build;
import android.os.Bundle;
import android.util.Log;
import android.view.View;
import android.widget.AdapterView;
import android.widget.ArrayAdapter;
import android.widget.Button;
import android.widget.Spinner;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.core.app.ActivityCompat;
import androidx.core.content.ContextCompat;

import com.capstone.mdfeventmanagementsystem.Student.MainActivity2;
import com.capstone.mdfeventmanagementsystem.Student.StudentLogin;
import com.capstone.mdfeventmanagementsystem.Teacher.TeacherDashboard;
import com.capstone.mdfeventmanagementsystem.Teacher.TeacherLogin;
import com.capstone.mdfeventmanagementsystem.Utilities.BaseActivity;
import com.capstone.mdfeventmanagementsystem.Utilities.WorkManagerAllowRegistration;
import com.google.firebase.messaging.FirebaseMessaging;  // ← ADDED

public class MainActivity extends BaseActivity {

    private Spinner roleSpinner;
    private Button selectButton;
    private String selectedRole = "Student";
    private static final String TAG = "MainActivity";
    private static final String CHANNEL_ID = "event_notifications";
    private static final String CHANNEL_NAME = "Event Notifications";
    private static final String CHANNEL_DESC = "Notifications for new events, registration, and reminders";

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        // CRITICAL: Create notification channel + init FCM BEFORE anything else
        createNotificationChannel();
        initializeFcmAndToken();

        WorkManagerAllowRegistration.startPeriodicRegistrationCheck(this);

        Log.d(TAG, "onCreate: MainActivity started");

        // Check login session
        SharedPreferences sharedPreferences = getSharedPreferences("UserSession", MODE_PRIVATE);
        boolean isLoggedIn = sharedPreferences.getBoolean("isLoggedIn", false);
        String userType = sharedPreferences.getString("userType", "");

        if (isLoggedIn) {
            Intent intent = "teacher".equals(userType)
                    ? new Intent(this, TeacherDashboard.class)
                    : new Intent(this, MainActivity2.class);
            startActivity(intent);
            finish();
            return;
        }

        setContentView(R.layout.activity_main);

        roleSpinner = findViewById(R.id.roleSpinner);
        selectButton = findViewById(R.id.btnSelect);

        requestNotificationPermission();
        setupRoleSpinner();

        selectButton.setOnClickListener(v -> navigateToLogin());
    }

    // THIS IS THE MISSING PIECE #1
    private void createNotificationChannel() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            NotificationChannel channel = new NotificationChannel(
                    CHANNEL_ID,
                    CHANNEL_NAME,
                    NotificationManager.IMPORTANCE_HIGH
            );
            channel.setDescription(CHANNEL_DESC);
            channel.enableVibration(true);
            channel.enableLights(true);

            NotificationManager manager = getSystemService(NotificationManager.class);
            manager.createNotificationChannel(channel);
            Log.d(TAG, "Notification channel created: " + CHANNEL_ID);
        }
    }

    // THIS IS THE MISSING PIECE #2 – Forces FCM token generation early
    private void initializeFcmAndToken() {
        FirebaseMessaging.getInstance().getToken()
                .addOnCompleteListener(task -> {
                    if (task.isSuccessful() && task.getResult() != null) {
                        String token = task.getResult();
                        Log.d(TAG, "FCM Token generated: " + token.substring(0, 20) + "...");
                        // Token will be sent to DB when user logs in (you already added that in StudentLogin)
                    } else {
                        Log.e(TAG, "Failed to get FCM token", task.getException());
                    }
                });

        // Subscribe to global topic (optional but recommended)
        FirebaseMessaging.getInstance().subscribeToTopic("all_events")
                .addOnCompleteListener(task -> {
                    if (task.isSuccessful()) {
                        Log.d(TAG, "Subscribed to topic: all_events");
                    }
                });
    }

    // Rest of your existing methods (unchanged)
    private void requestNotificationPermission() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            if (ContextCompat.checkSelfPermission(this, Manifest.permission.POST_NOTIFICATIONS)
                    != PackageManager.PERMISSION_GRANTED) {
                ActivityCompat.requestPermissions(this,
                        new String[]{Manifest.permission.POST_NOTIFICATIONS}, 1);
            }
        }
    }

    private void setupRoleSpinner() {
        String[] roles = {"Student", "Teacher"};
        ArrayAdapter<String> adapter = new ArrayAdapter<>(this,
                android.R.layout.simple_spinner_dropdown_item, roles);
        roleSpinner.setAdapter(adapter);
        roleSpinner.setOnItemSelectedListener(new AdapterView.OnItemSelectedListener() {
            @Override
            public void onItemSelected(AdapterView<?> parent, View view, int position, long id) {
                selectedRole = roles[position];
            }
            @Override
            public void onNothingSelected(AdapterView<?> parent) {}
        });
    }

    private void navigateToLogin() {
        SharedPreferences prefs = getSharedPreferences("MyAppPrefs", MODE_PRIVATE);
        prefs.edit().putString("USER_ROLE", selectedRole).apply();

        Intent intent = "Teacher".equals(selectedRole)
                ? new Intent(this, TeacherLogin.class)
                : new Intent(this, StudentLogin.class);
        startActivity(intent);
    }

    @Override
    public void onRequestPermissionsResult(int requestCode, @NonNull String[] permissions,
                                           @NonNull int[] grantResults) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults);
        if (requestCode == 1 && grantResults.length > 0
                && grantResults[0] == PackageManager.PERMISSION_GRANTED) {
            Log.d(TAG, "Notification permission granted");
        }
    }
}