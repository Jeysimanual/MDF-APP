package com.capstone.mdfeventmanagementsystem.Utilities;

import android.content.SharedPreferences;
import android.os.Bundle;

import androidx.appcompat.app.AppCompatActivity;

import com.capstone.mdfeventmanagementsystem.R;

public class BaseActivity extends AppCompatActivity {

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        // ✅ Apply theme before calling super
        SharedPreferences prefs = getSharedPreferences("MyAppPrefs", MODE_PRIVATE);
        String role = prefs.getString("USER_ROLE", "");

        if ("Student".equals(role)) {
            setTheme(R.style.GreenTheme);
        } else if ("Teacher".equals(role)) {
            setTheme(R.style.BlueTheme);
        } else {
            setTheme(R.style.AppTheme); // fallback/default
        }

        super.onCreate(savedInstanceState);
    }
}
