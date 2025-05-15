package com.capstone.mdfeventmanagementsystem.Utilities;

import android.content.SharedPreferences;
import android.os.Bundle;

import androidx.appcompat.app.AppCompatActivity;

import com.capstone.mdfeventmanagementsystem.R;

public class BaseActivity extends AppCompatActivity {

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        // ✅ Apply theme before calling super
        String className = this.getClass().getSimpleName();

        // Check if current activity is one of the authentication screens
        if (isAuthenticationScreen(className)) {
            // Login/Signup/Forgot Password screens get white theme
            setTheme(R.style.LoginTheme);
        } else {
            // For MainActivity and all other screens, use role-based themes
            SharedPreferences prefs = getSharedPreferences("MyAppPrefs", MODE_PRIVATE);
            String role = prefs.getString("USER_ROLE", "");

            if ("Student".equals(role)) {
                setTheme(R.style.GreenTheme);
            } else if ("Teacher".equals(role)) {
                setTheme(R.style.BlueTheme);
            } else {
                setTheme(R.style.AppTheme); // fallback/default
            }
        }

        super.onCreate(savedInstanceState);
    }

    /**
     * Checks if the current activity is an authentication screen
     * based on its class name
     */
    private boolean isAuthenticationScreen(String className) {
        return className.contains("Login") ||
                className.contains("SignUp") ||
                className.contains("ForgotPass") ||
                className.contains("Register") ||
                className.contains("Auth") ||
                className.equals("StudentForgetPass") ||
                className.equals("MainActivity"); // <- add this line
    }

}