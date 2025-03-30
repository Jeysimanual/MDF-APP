package com.capstone.mdfeventmanagementsystem;

import android.annotation.SuppressLint;
import android.content.Intent;
import android.content.SharedPreferences;
import android.os.Bundle;
import android.util.Log;
import android.widget.Button;

import androidx.appcompat.app.AppCompatActivity;
import com.google.android.material.bottomnavigation.BottomNavigationView;

public class QRCheckInActivity extends AppCompatActivity {
    private Button btnLogout;
    @SuppressLint("MissingInflatedId")
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_qrcheck_in);

        // ✅ Initialize Logout Button
        btnLogout = findViewById(R.id.btnLogout);
        btnLogout.setOnClickListener(v -> logoutUser());

        // Initialize Bottom Navigation
        BottomNavigationView bottomNavigationView = findViewById(R.id.bottom_navigation);
        bottomNavigationView.setSelectedItemId(R.id.nav_scan); // Set active tab

        bottomNavigationView.setOnNavigationItemSelectedListener(item -> {
            int itemId = item.getItemId();
            if (itemId == R.id.nav_home) {
                startActivity(new Intent(getApplicationContext(), MainActivity2.class));
            } else if (itemId == R.id.nav_event) {
                startActivity(new Intent(getApplicationContext(), StudentDashboard.class));
            } else if (itemId == R.id.nav_ticket) {
                startActivity(new Intent(getApplicationContext(), StudentTickets.class));
            } else if (itemId == R.id.nav_scan) {
                return true;
            }
            overridePendingTransition(0, 0);
            return true;
        });
    }
    /** ✅ Handles user logout */
    private void logoutUser() {
        // ✅ Clear saved session
        SharedPreferences sharedPreferences = getSharedPreferences("UserSession", MODE_PRIVATE);
        SharedPreferences.Editor editor = sharedPreferences.edit();
        editor.clear(); // Removes all session data
        editor.apply();

        Log.d("MainActivity2", "User logged out successfully");

        // ✅ Redirect to MainActivity (Login/Role Selection)
        Intent intent = new Intent(QRCheckInActivity.this, MainActivity.class);
        startActivity(intent);
        finish(); // Prevent returning to dashboard
    }
}
