package com.capstone.mdfeventmanagementsystem.Teacher;

import android.content.Intent;
import android.os.Bundle;
import android.view.View;
import android.widget.Button;
import android.widget.LinearLayout;

import androidx.appcompat.app.AppCompatActivity;

import com.capstone.mdfeventmanagementsystem.MainActivity;
import com.capstone.mdfeventmanagementsystem.R;
import com.capstone.mdfeventmanagementsystem.Student.ChangePassword;
import com.capstone.mdfeventmanagementsystem.Student.MyInformation;
import com.capstone.mdfeventmanagementsystem.Student.ProfileActivity;
import com.google.android.material.bottomnavigation.BottomNavigationView;
import com.google.firebase.auth.FirebaseAuth;

public class TeacherProfile extends AppCompatActivity {
    private LinearLayout btnMyInfo,btnChangePassword;

    private Button btnLogout;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_teacher_profile);

        // Initialize Logout Button
        btnMyInfo = findViewById(R.id.btnMyInfo);
        btnChangePassword = findViewById(R.id.btnChangePassword);
        btnLogout = findViewById(R.id.btnLogout);

        btnMyInfo.setOnClickListener(view -> {
            Intent intent = new Intent(TeacherProfile.this, TeacherInformation.class);
            startActivity(intent);
        });

        btnChangePassword.setOnClickListener(view -> {
            Intent intent = new Intent(TeacherProfile.this, Teacher_ChangePassword.class);
            startActivity(intent);
        });

        btnLogout.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                logoutUser();
            }
        });

        // Initialize Bottom Navigation
        BottomNavigationView bottomNavigationView = findViewById(R.id.bottom_navigation_teacher);
        bottomNavigationView.setSelectedItemId(R.id.nav_profile_teacher);

        bottomNavigationView.setOnItemSelectedListener(item -> {
            int itemId = item.getItemId();

            if (itemId == R.id.nav_home_teacher) {
                startActivity(new Intent(this, TeacherDashboard.class));
                finish();
            } else if (itemId == R.id.nav_event_teacher) {
                startActivity(new Intent(this, TeacherEvents.class));
                finish();
            } else if (itemId == R.id.nav_scan_teacher) {
                startActivity(new Intent(this, TeacherScanning.class));
                finish();
            } else if (itemId == R.id.nav_profile_teacher) {
                return true; // Stay on the same page
            }

            overridePendingTransition(0, 0); // Smooth transition
            return true;
        });
    }

    private void logoutUser() {
        FirebaseAuth.getInstance().signOut();

        // Clear SharedPreferences login state
        getSharedPreferences("UserSession", MODE_PRIVATE)
                .edit()
                .putBoolean("isLoggedIn", false)
                .apply();

        // Redirect to MainActivity instead of TeacherLogin
        Intent intent = new Intent(TeacherProfile.this, MainActivity.class);
        intent.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK | Intent.FLAG_ACTIVITY_CLEAR_TASK);
        startActivity(intent);
    }

}
