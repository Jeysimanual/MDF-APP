package com.capstone.mdfeventmanagementsystem;

import android.annotation.SuppressLint;
import android.content.Intent;
import android.os.Bundle;
import android.util.Log;
import android.view.View;
import android.widget.AdapterView;
import android.widget.ArrayAdapter;
import android.widget.Button;
import android.widget.Spinner;
import android.widget.Toast;
import androidx.appcompat.app.AppCompatActivity;

public class MainActivity extends AppCompatActivity {

    private Spinner roleSpinner;
    private Button selectButton;
    private String selectedRole = "Student"; // Default selection
    private static final String TAG = "MainActivity";
    private NotificationManager notificationManager;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        Log.d(TAG, "onCreate: MainActivity started");

        // Initialize the notification manager
        notificationManager = new NotificationManager(this);
        Log.d(TAG, "onCreate: NotificationManager initialized");

        // Check if opened from notification
        if (getIntent().hasExtra("notification_id")) {
            String notificationId = getIntent().getStringExtra("notification_id");
            notificationManager.markNotificationAsViewed(notificationId);
            Log.d(TAG, "onCreate: Marked notification " + notificationId + " as viewed");
        }

        roleSpinner = findViewById(R.id.roleSpinner);
        selectButton = findViewById(R.id.btnSelect);

        if (roleSpinner == null || selectButton == null) {
            Log.e(TAG, "Error: roleSpinner or selectButton is NULL. Check activity_main.xml!");
            return;
        }

        Log.d(TAG, "onCreate: UI elements initialized");

        // Spinner with roles
        String[] roles = {"Student", "Teacher"};
        ArrayAdapter<String> adapter = new ArrayAdapter<>(this, android.R.layout.simple_spinner_dropdown_item, roles);
        roleSpinner.setAdapter(adapter);

        Log.d(TAG, "onCreate: Spinner populated with roles");

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

        selectButton.setOnClickListener(v -> {
            Log.d(TAG, "onClick: Select button clicked, navigating to " + selectedRole + " login");
            navigateToLogin();
        });
    }

    @Override
    protected void onNewIntent(Intent intent) {
        super.onNewIntent(intent);

        // Check if opened from notification while app is running
        if (intent.hasExtra("notification_id")) {
            String notificationId = intent.getStringExtra("notification_id");
            notificationManager.markNotificationAsViewed(notificationId);
            Log.d(TAG, "onNewIntent: Marked notification " + notificationId + " as viewed");
        }
    }

    private void navigateToLogin() {
        Intent intent;
        if (selectedRole.equals("Teacher")) {
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