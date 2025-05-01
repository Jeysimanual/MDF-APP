package com.capstone.mdfeventmanagementsystem;

import android.Manifest;
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
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.app.ActivityCompat;
import androidx.core.content.ContextCompat;

import com.capstone.mdfeventmanagementsystem.Student.MainActivity2;
import com.capstone.mdfeventmanagementsystem.Student.StudentLogin;
import com.capstone.mdfeventmanagementsystem.Teacher.TeacherDashboard;
import com.capstone.mdfeventmanagementsystem.Teacher.TeacherLogin;
import com.capstone.mdfeventmanagementsystem.Utilities.NotificationService;
import com.capstone.mdfeventmanagementsystem.Utilities.WorkManagerAllowRegistration;

public class MainActivity extends AppCompatActivity {

    private Spinner roleSpinner;
    private Button selectButton;
    private String selectedRole = "Student"; // Default selection
    private static final String TAG = "MainActivity";
    private static final String CHANNEL_ID = "event_notifications";

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        // Start the periodic registration check
        WorkManagerAllowRegistration.startPeriodicRegistrationCheck(this); // Start worker for periodic check

        Log.d(TAG, "onCreate: MainActivity started");

        // ✅ Start foreground service (ensures background notifications)
        startNotificationService();

        // ✅ Check if user is already logged in
        SharedPreferences sharedPreferences = getSharedPreferences("UserSession", MODE_PRIVATE);
        boolean isLoggedIn = sharedPreferences.getBoolean("isLoggedIn", false);
        String userType = sharedPreferences.getString("userType", ""); // Retrieve user type

        if (isLoggedIn) {
            Intent intent;
            if ("teacher".equals(userType)) {
                Log.d(TAG, "User session found. Redirecting to TeacherDashboard...");
                intent = new Intent(MainActivity.this, TeacherDashboard.class);
            } else {
                Log.d(TAG, "User session found. Redirecting to StudentDashboard...");
                intent = new Intent(MainActivity.this, MainActivity2.class);
            }
            startActivity(intent);
            finish(); // Prevents user from returning to this screen
            return;
        }

        setContentView(R.layout.activity_main);

        roleSpinner = findViewById(R.id.roleSpinner);
        selectButton = findViewById(R.id.btnSelect);

        if (roleSpinner == null || selectButton == null) {
            Log.e(TAG, "Error: roleSpinner or selectButton is NULL. Check activity_main.xml!");
            return;
        }

        Log.d(TAG, "onCreate: UI elements initialized");

        // ✅ Request notification permissions for Android 13+
        requestNotificationPermission();

        // ✅ Populate role spinner
        setupRoleSpinner();

        // ✅ Handle select button click
        selectButton.setOnClickListener(v -> {
            Log.d(TAG, "onClick: Select button clicked, navigating to " + selectedRole + " login");
            navigateToLogin();
        });
    }

    /** ✅ Starts the NotificationService as a Foreground Service */
    private void startNotificationService() {
        Intent serviceIntent = new Intent(this, NotificationService.class);
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            ContextCompat.startForegroundService(this, serviceIntent); // Required for Android 8+
        } else {
            startService(serviceIntent);
        }
        Log.d(TAG, "startNotificationService: Foreground service started");
    }

    /** ✅ Requests notification permissions for Android 13+ */
    private void requestNotificationPermission() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) { // Android 13+ (API 33)
            if (ContextCompat.checkSelfPermission(this, Manifest.permission.POST_NOTIFICATIONS)
                    != PackageManager.PERMISSION_GRANTED) {
                ActivityCompat.requestPermissions(this, new String[]{Manifest.permission.POST_NOTIFICATIONS}, 1);
            } else {
                Log.d(TAG, "requestNotificationPermission: Notification permission already granted");
            }
        } else {
            Log.d(TAG, "requestNotificationPermission: POST_NOTIFICATIONS permission not needed for this Android version");
        }
    }

    /** ✅ Populates the spinner with roles and handles selection */
    private void setupRoleSpinner() {
        String[] roles = {"Student", "Teacher"};
        ArrayAdapter<String> adapter = new ArrayAdapter<>(this, android.R.layout.simple_spinner_dropdown_item, roles);
        roleSpinner.setAdapter(adapter);

        roleSpinner.setOnItemSelectedListener(new AdapterView.OnItemSelectedListener() {
            @Override
            public void onItemSelected(AdapterView<?> parent, View view, int position, long id) {
                selectedRole = roles[position];
                Log.d(TAG, "onItemSelected: User selected " + selectedRole);
            }

            @Override
            public void onNothingSelected(AdapterView<?> parent) {
                Log.d(TAG, "onNothingSelected: No role selected, defaulting to Student");
            }
        });

        Log.d(TAG, "setupRoleSpinner: Spinner populated with roles");
    }

    /** ✅ Navigates to the correct login activity */
    private void navigateToLogin() {
        Log.d(TAG, "navigateToLogin: Attempting to navigate to " + selectedRole + " login activity");

        SharedPreferences prefs = getSharedPreferences("MyAppPrefs", MODE_PRIVATE);
        SharedPreferences.Editor editor = prefs.edit();
        editor.putString("USER_ROLE", selectedRole);
        editor.apply();

        Intent intent;
        if ("Teacher".equals(selectedRole)) {
            intent = new Intent(MainActivity.this, TeacherLogin.class);
        } else {
            intent = new Intent(MainActivity.this, StudentLogin.class);
        }

        try {
            startActivity(intent);
            Log.d(TAG, "navigateToLogin: Successfully started " + selectedRole + " login activity");
        } catch (Exception e) {
            Log.e(TAG, "navigateToLogin: Failed to start activity", e);
            Toast.makeText(this, "Error: Unable to open " + selectedRole + " login screen", Toast.LENGTH_LONG).show();
        }
    }
}
