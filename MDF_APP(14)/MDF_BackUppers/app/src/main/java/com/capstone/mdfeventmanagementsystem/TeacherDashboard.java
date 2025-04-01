package com.capstone.mdfeventmanagementsystem;

import android.content.Intent;
import android.os.Bundle;
import androidx.appcompat.app.AppCompatActivity;
import com.google.android.material.bottomnavigation.BottomNavigationView;

public class TeacherDashboard extends AppCompatActivity {

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_teacher_dashboard); // 🔥 Ensure correct layout is set

        BottomNavigationView bottomNavigationView = findViewById(R.id.bottom_navigation_teacher);
        bottomNavigationView.setSelectedItemId(R.id.nav_home_teacher);

        bottomNavigationView.setOnItemSelectedListener(item -> {
            int itemId = item.getItemId();

            if (itemId == R.id.nav_home_teacher) {
                return true; // Stay on the same page
            } else if (itemId == R.id.nav_event_teacher) {
                startActivity(new Intent(this, TeacherEvents.class));
                finish(); // 🔥 Prevent stacking activities
            } else if (itemId == R.id.nav_scan_teacher) {
                startActivity(new Intent(this, TeacherScanning.class));
                finish();
            } else if (itemId == R.id.nav_profile_teacher) {
                startActivity(new Intent(this, TeacherProfile.class));
                finish();
            }

            overridePendingTransition(0, 0); // Remove animation between activities
            return true;
        });
    }
}
