package com.capstone.mdfeventmanagementsystem.Student;

import android.content.Intent;
import android.content.SharedPreferences;
import android.content.res.ColorStateList;
import android.graphics.Color;
import android.os.Bundle;
import android.util.Log;
import android.view.View;
import android.widget.Button;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.TextView;

import androidx.annotation.Nullable;
import androidx.appcompat.app.AppCompatActivity;

import com.bumptech.glide.Glide;
import com.capstone.mdfeventmanagementsystem.MainActivity;
import com.capstone.mdfeventmanagementsystem.R;
import com.capstone.mdfeventmanagementsystem.Utilities.BaseActivity;
import com.google.android.material.bottomappbar.BottomAppBar;
import com.google.android.material.bottomnavigation.BottomNavigationView;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseUser;

public class ProfileActivity extends BaseActivity {

    private TextView txtUserName, txtUserEmail;
    private ImageView imgProfile, btnBack;
    private LinearLayout btnMyInfo, btnChangePassword;
    private Button btnLogout;

    @Override
    protected void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_profile);

        findViewById(R.id.fab_scan).setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                startActivity(new Intent(getApplicationContext(), QRCheckInActivity.class));
                overridePendingTransition(0, 0);
            }
        });

        BottomAppBar bottomAppBar = findViewById(R.id.bottomAppBar);
        bottomAppBar.setBackgroundTint(ColorStateList.valueOf(Color.WHITE));

        txtUserName = findViewById(R.id.txtUserName);
        txtUserEmail = findViewById(R.id.txtUserEmail);
        imgProfile = findViewById(R.id.imgProfile);
        btnBack = findViewById(R.id.btnBack);
        btnMyInfo = findViewById(R.id.btnMyInfo);
        btnChangePassword = findViewById(R.id.btnChangePassword);
        btnLogout = findViewById(R.id.btnLogout);

        loadUserProfile();

        btnBack.setOnClickListener(view -> finish());

        btnMyInfo.setOnClickListener(view -> {
            Intent intent = new Intent(ProfileActivity.this, MyInformation.class);
            startActivity(intent);
        });

        btnChangePassword.setOnClickListener(view -> {
            Intent intent = new Intent(ProfileActivity.this, ChangePassword.class);
            startActivity(intent);
        });

        BottomNavigationView bottomNavigationView = findViewById(R.id.bottom_navigation);
        bottomNavigationView.setBackground(null);

        bottomNavigationView.getMenu().setGroupCheckable(0, true, false);
        for (int i = 0; i < bottomNavigationView.getMenu().size(); i++) {
            bottomNavigationView.getMenu().getItem(i).setChecked(false);
        }
        bottomNavigationView.getMenu().setGroupCheckable(0, true, true);

        bottomNavigationView.setOnNavigationItemSelectedListener(item -> {
            int itemId = item.getItemId();
            if (itemId == R.id.nav_home) {
                startActivity(new Intent(getApplicationContext(), StudentDashboard.class));
            } else if (itemId == R.id.nav_event) {
                startActivity(new Intent(getApplicationContext(), StudentDashboard.class));
            } else if (itemId == R.id.nav_ticket) {
                startActivity(new Intent(getApplicationContext(), StudentTickets.class));
            } else if (itemId == R.id.nav_cert) {
                startActivity(new Intent(getApplicationContext(), StudentCertificate.class));
            }
            overridePendingTransition(0, 0);
            return false;
        });

        // ✅ Initialize Logout Button
        btnLogout = findViewById(R.id.btnLogout);
        btnLogout.setOnClickListener(v -> logoutUser());

    }

    private void loadUserProfile() {
        FirebaseUser user = FirebaseAuth.getInstance().getCurrentUser();
        if (user != null) {
            txtUserName.setText(user.getDisplayName() != null ? user.getDisplayName() : "User Name");
            txtUserEmail.setText(user.getEmail());

            if (user.getPhotoUrl() != null) {
                Glide.with(this).load(user.getPhotoUrl()).into(imgProfile);
            }
        }
    }

    /** ✅ Handles user logout */
    private void logoutUser() {
        // Log out from Firebase Authentication
        FirebaseAuth.getInstance().signOut();

        // Clear all shared preferences
        SharedPreferences userSession = getSharedPreferences("UserSession", MODE_PRIVATE);
        userSession.edit().clear().apply();

        // Also clear MyAppPrefs if it's used elsewhere
        SharedPreferences myAppPrefs = getSharedPreferences("MyAppPrefs", MODE_PRIVATE);
        myAppPrefs.edit().clear().apply();

        Log.d("QRCheckInActivity", "User logged out successfully");

        // Redirect to login/role selection screen
        Intent intent = new Intent(ProfileActivity.this, MainActivity.class);
        intent.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK | Intent.FLAG_ACTIVITY_CLEAR_TASK); // Clear entire activity stack
        startActivity(intent);
        finish();
    }
}
