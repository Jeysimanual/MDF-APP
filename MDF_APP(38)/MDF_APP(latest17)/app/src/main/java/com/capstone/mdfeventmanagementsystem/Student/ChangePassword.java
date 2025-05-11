package com.capstone.mdfeventmanagementsystem.Student;

import android.content.Intent;
import android.content.SharedPreferences;
import android.os.Bundle;
import android.text.Editable;
import android.text.TextUtils;
import android.text.TextWatcher;
import android.text.method.PasswordTransformationMethod;
import android.util.Log;
import android.view.MotionEvent;
import android.view.View;
import android.widget.Button;
import android.widget.EditText;
import android.widget.ImageView;
import android.widget.TextView;
import android.widget.Toast;

import androidx.activity.EdgeToEdge;
import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.graphics.Insets;
import androidx.core.view.ViewCompat;
import androidx.core.view.WindowInsetsCompat;

import com.capstone.mdfeventmanagementsystem.MainActivity;
import com.capstone.mdfeventmanagementsystem.R;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseUser;
import com.google.firebase.database.DataSnapshot;
import com.google.firebase.database.DatabaseError;
import com.google.firebase.database.DatabaseReference;
import com.google.firebase.database.FirebaseDatabase;
import com.google.firebase.database.ValueEventListener;

import java.util.HashMap;
import java.util.Map;

public class ChangePassword extends AppCompatActivity {

    private static final String TAG = "StudentChangePassword";

    private EditText changeOldPassword, newPassword, etConfirmPassword;
    private TextView checkLength, checkUpperCase, checkLowerCase, checkDigit, checkSpecialChar;
    private Button changePassBtn;
    private DatabaseReference studentRef;
    private FirebaseAuth auth;
    private String studentId, studentEmail;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        EdgeToEdge.enable(this);
        setContentView(R.layout.activity_change_password);

        ImageView backBtn = findViewById(R.id.backBtn);

        View.OnClickListener goBackListener = v -> {
            Intent intent = new Intent(ChangePassword.this, ProfileActivity.class);
            startActivity(intent);
            overridePendingTransition(R.anim.slide_in_left, R.anim.slide_out_right);
            finish();
        };

        backBtn.setOnClickListener(goBackListener);

        View mainView = findViewById(R.id.main);
        if (mainView != null) {
            ViewCompat.setOnApplyWindowInsetsListener(mainView, (v, insets) -> {
                Insets systemBars = insets.getInsets(WindowInsetsCompat.Type.systemBars());
                v.setPadding(systemBars.left, systemBars.top, systemBars.right, systemBars.bottom);
                return insets;
            });
        } else {
            Log.e(TAG, "mainView is null!");
        }

        // Initialize UI components
        changeOldPassword = findViewById(R.id.changeOldPassword);
        newPassword = findViewById(R.id.newPassword);
        etConfirmPassword = findViewById(R.id.etConfirmPassword);
        changePassBtn = findViewById(R.id.changePassBtn);

        // Initialize password requirement TextViews
        checkLength = findViewById(R.id.checkLength);
        checkUpperCase = findViewById(R.id.checkUpperCase);
        checkLowerCase = findViewById(R.id.checkLowerCase);
        checkDigit = findViewById(R.id.checkDigit);
        checkSpecialChar = findViewById(R.id.checkSpecialChar);

        // Initialize Firebase Auth
        auth = FirebaseAuth.getInstance();

        // Get current Firebase user
        FirebaseUser currentUser = auth.getCurrentUser();
        if (currentUser == null) {
            Log.e(TAG, "No user is signed in");
            Toast.makeText(this, "Please log in to change your password", Toast.LENGTH_SHORT).show();
            navigateToMainActivity();
            return;
        }

        studentEmail = currentUser.getEmail();

        // Get student ID from UserSession SharedPreferences
        SharedPreferences sharedPreferences = getSharedPreferences("UserSession", MODE_PRIVATE);
        boolean isLoggedIn = sharedPreferences.getBoolean("isLoggedIn", false);
        String userType = sharedPreferences.getString("userType", "");

        if (!isLoggedIn || !"student".equals(userType)) {
            Log.e(TAG, "Invalid user session: isLoggedIn=" + isLoggedIn + ", userType=" + userType);
            Toast.makeText(this, "User session not found. Please log in again.", Toast.LENGTH_SHORT).show();
            navigateToMainActivity();
            return;
        }

        // Find the student's ID from Firebase using their email
        DatabaseReference studentsRef = FirebaseDatabase.getInstance().getReference("students");
        studentsRef.orderByChild("email").equalTo(studentEmail)
                .addListenerForSingleValueEvent(new ValueEventListener() {
                    @Override
                    public void onDataChange(@NonNull DataSnapshot dataSnapshot) {
                        if (dataSnapshot.exists()) {
                            for (DataSnapshot snapshot : dataSnapshot.getChildren()) {
                                studentId = snapshot.getKey();
                                studentRef = FirebaseDatabase.getInstance().getReference("students").child(studentId);
                                Log.d(TAG, "Found student ID: " + studentId);
                            }
                        } else {
                            Log.e(TAG, "No student found with email: " + studentEmail);
                            Toast.makeText(ChangePassword.this, "Student data not found", Toast.LENGTH_SHORT).show();
                            navigateToMainActivity();
                        }
                    }

                    @Override
                    public void onCancelled(@NonNull DatabaseError error) {
                        Log.e(TAG, "Error fetching student data: " + error.getMessage());
                        Toast.makeText(ChangePassword.this, "Error fetching user data", Toast.LENGTH_SHORT).show();
                    }
                });

        // Hide password requirements initially
        hidePasswordRequirements();

        // Set up password toggles
        setPasswordToggle();

        // Set up focus listener for password field
        newPassword.setOnFocusChangeListener((v, hasFocus) -> {
            if (hasFocus) {
                showPasswordRequirements();
            } else {
                hidePasswordRequirements();
            }
        });

        // Set up password validation
        newPassword.addTextChangedListener(new TextWatcher() {
            @Override
            public void beforeTextChanged(CharSequence s, int start, int count, int after) {}

            @Override
            public void onTextChanged(CharSequence s, int start, int before, int count) {}

            @Override
            public void afterTextChanged(Editable s) {
                if (newPassword.hasFocus()) {
                    validatePassword(s.toString());
                }
            }
        });

        // Set up confirm password validation
        etConfirmPassword.addTextChangedListener(new TextWatcher() {
            @Override
            public void beforeTextChanged(CharSequence s, int start, int count, int after) {}

            @Override
            public void onTextChanged(CharSequence s, int start, int before, int count) {}

            @Override
            public void afterTextChanged(Editable s) {
                validateConfirmPassword();
            }
        });

        // Set up change password button click listener
        changePassBtn.setOnClickListener(view -> validateAndChangePassword());
    }

    private void showPasswordRequirements() {
        checkLength.setVisibility(View.VISIBLE);
        checkUpperCase.setVisibility(View.VISIBLE);
        checkLowerCase.setVisibility(View.VISIBLE);
        checkDigit.setVisibility(View.VISIBLE);
        checkSpecialChar.setVisibility(View.VISIBLE);

        // If password has been entered, update the validation status
        if (!TextUtils.isEmpty(newPassword.getText())) {
            validatePassword(newPassword.getText().toString());
        }
    }

    private void hidePasswordRequirements() {
        checkLength.setVisibility(View.GONE);
        checkUpperCase.setVisibility(View.GONE);
        checkLowerCase.setVisibility(View.GONE);
        checkDigit.setVisibility(View.GONE);
        checkSpecialChar.setVisibility(View.GONE);
    }

    private void setPasswordToggle() {
        // Initially set the drawables
        changeOldPassword.setCompoundDrawablesRelativeWithIntrinsicBounds(0, 0, R.drawable.hide, 0);
        newPassword.setCompoundDrawablesRelativeWithIntrinsicBounds(0, 0, R.drawable.hide, 0);
        etConfirmPassword.setCompoundDrawablesRelativeWithIntrinsicBounds(0, 0, R.drawable.hide, 0);

        // Set up toggle for old password
        changeOldPassword.setOnTouchListener((v, event) -> {
            final int DRAWABLE_RIGHT = 2;

            if (event.getAction() == MotionEvent.ACTION_UP) {
                if (changeOldPassword.getCompoundDrawables()[DRAWABLE_RIGHT] != null) {
                    // Check if touch is within the bounds of the right drawable
                    if (event.getRawX() >= (changeOldPassword.getRight() - changeOldPassword.getCompoundDrawables()[DRAWABLE_RIGHT].getBounds().width() - changeOldPassword.getPaddingRight())) {
                        togglePasswordVisibility(changeOldPassword);
                        return true;
                    }
                }
            }
            return false;
        });

        // Set up toggle for new password
        newPassword.setOnTouchListener((v, event) -> {
            final int DRAWABLE_RIGHT = 2;

            if (event.getAction() == MotionEvent.ACTION_UP) {
                if (newPassword.getCompoundDrawables()[DRAWABLE_RIGHT] != null) {
                    // Check if touch is within the bounds of the right drawable
                    if (event.getRawX() >= (newPassword.getRight() - newPassword.getCompoundDrawables()[DRAWABLE_RIGHT].getBounds().width() - newPassword.getPaddingRight())) {
                        togglePasswordVisibility(newPassword);
                        return true;
                    }
                }
            }
            return false;
        });

        // Set up toggle for confirm password
        etConfirmPassword.setOnTouchListener((v, event) -> {
            final int DRAWABLE_RIGHT = 2;

            if (event.getAction() == MotionEvent.ACTION_UP) {
                if (etConfirmPassword.getCompoundDrawables()[DRAWABLE_RIGHT] != null) {
                    // Check if touch is within the bounds of the right drawable
                    if (event.getRawX() >= (etConfirmPassword.getRight() - etConfirmPassword.getCompoundDrawables()[DRAWABLE_RIGHT].getBounds().width() - etConfirmPassword.getPaddingRight())) {
                        togglePasswordVisibility(etConfirmPassword);
                        return true;
                    }
                }
            }
            return false;
        });
    }

    private void togglePasswordVisibility(EditText editText) {
        if (editText.getTransformationMethod() instanceof PasswordTransformationMethod) {
            // Show password
            editText.setTransformationMethod(null);
            editText.setCompoundDrawablesRelativeWithIntrinsicBounds(0, 0, R.drawable.view, 0);
        } else {
            // Hide password
            editText.setTransformationMethod(PasswordTransformationMethod.getInstance());
            editText.setCompoundDrawablesRelativeWithIntrinsicBounds(0, 0, R.drawable.hide, 0);
        }
    }

    private void validatePassword(String password) {
        boolean isLengthValid = password.length() >= 8;
        boolean hasUpperCase = !password.equals(password.toLowerCase());
        boolean hasLowerCase = !password.equals(password.toUpperCase());
        boolean hasDigit = password.matches(".*\\d.*");
        boolean hasSpecialChar = password.matches(".*[!@#$%^&*(),.?\":{}|<>].*");

        // Update UI indicators - color only, no visibility change
        checkLength.setTextColor(isLengthValid ? getColor(R.color.green) : getColor(R.color.red));
        checkUpperCase.setTextColor(hasUpperCase ? getColor(R.color.green) : getColor(R.color.red));
        checkLowerCase.setTextColor(hasLowerCase ? getColor(R.color.green) : getColor(R.color.red));
        checkDigit.setTextColor(hasDigit ? getColor(R.color.green) : getColor(R.color.red));
        checkSpecialChar.setTextColor(hasSpecialChar ? getColor(R.color.green) : getColor(R.color.red));
    }

    private void validateConfirmPassword() {
        if (!TextUtils.equals(newPassword.getText().toString(), etConfirmPassword.getText().toString())) {
            etConfirmPassword.setError("Passwords do not match!");
        } else {
            etConfirmPassword.setError(null); // Clear the error if passwords match
        }
    }

    private boolean isStrongPassword(String password) {
        return password.length() >= 8 &&
                password.matches(".*[A-Z].*") &&
                password.matches(".*[a-z].*") &&
                password.matches(".*\\d.*") &&
                password.matches(".*[!@#$%^&*(),.?\":{}|<>].*");
    }

    private void validateAndChangePassword() {
        String oldPassword = changeOldPassword.getText().toString().trim();
        String newPass = newPassword.getText().toString().trim();
        String confirmPass = etConfirmPassword.getText().toString().trim();

        // Check for empty fields
        if (TextUtils.isEmpty(oldPassword) || TextUtils.isEmpty(newPass) || TextUtils.isEmpty(confirmPass)) {
            Toast.makeText(this, "Please fill in all fields", Toast.LENGTH_SHORT).show();
            return;
        }

        // Check if new password meets requirements
        if (!isStrongPassword(newPass)) {
            Toast.makeText(this, "New password does not meet all requirements", Toast.LENGTH_SHORT).show();
            return;
        }

        // Check if new passwords match
        if (!newPass.equals(confirmPass)) {
            Toast.makeText(this, "New passwords do not match", Toast.LENGTH_SHORT).show();
            return;
        }

        // First, re-authenticate the user to verify old password
        FirebaseUser user = auth.getCurrentUser();
        if (user != null && studentEmail != null) {
            // Re-authenticate with Firebase Auth
            auth.signInWithEmailAndPassword(studentEmail, oldPassword)
                    .addOnSuccessListener(authResult -> {
                        // Old password verified, proceed to update
                        updatePassword(newPass);
                    })
                    .addOnFailureListener(e -> {
                        Toast.makeText(this, "Old password is incorrect", Toast.LENGTH_SHORT).show();
                        Log.e(TAG, "Authentication failed: " + e.getMessage());
                    });
        } else {
            Toast.makeText(this, "User session error. Please log in again.", Toast.LENGTH_SHORT).show();
            navigateToMainActivity();
        }
    }

    private void updatePassword(String newPassword) {
        if (studentRef == null) {
            Log.e(TAG, "Student reference is null");
            Toast.makeText(this, "Error: User data not initialized", Toast.LENGTH_SHORT).show();
            return;
        }

        // Update Firebase Auth password
        FirebaseUser user = auth.getCurrentUser();
        if (user != null) {
            user.updatePassword(newPassword)
                    .addOnSuccessListener(aVoid -> {
                        // Now update the password in Realtime Database
                        Map<String, Object> updates = new HashMap<>();
                        updates.put("password", newPassword);

                        studentRef.updateChildren(updates)
                                .addOnSuccessListener(dbVoid -> {
                                    Log.d(TAG, "Password successfully updated in both Auth and Database");
                                    Toast.makeText(ChangePassword.this, "Password successfully changed", Toast.LENGTH_SHORT).show();

                                    // Log the user out and redirect to MainActivity
                                    logout();
                                })
                                .addOnFailureListener(e -> {
                                    Log.e(TAG, "Error updating password in Database: " + e.getMessage());
                                    Toast.makeText(ChangePassword.this, "Password updated in Auth but failed in Database", Toast.LENGTH_SHORT).show();
                                    logout(); // Still logout as auth password is changed
                                });
                    })
                    .addOnFailureListener(e -> {
                        Log.e(TAG, "Error updating password in Auth: " + e.getMessage());
                        Toast.makeText(ChangePassword.this, "Failed to update password: " + e.getMessage(), Toast.LENGTH_SHORT).show();
                    });
        }
    }

    private void logout() {
        // Sign out from Firebase Auth
        FirebaseAuth.getInstance().signOut();

        // Clear user session data
        SharedPreferences sharedPreferences = getSharedPreferences("UserSession", MODE_PRIVATE);
        SharedPreferences.Editor editor = sharedPreferences.edit();
        editor.clear();
        editor.apply();

        // Navigate to MainActivity
        navigateToMainActivity();
    }

    private void navigateToMainActivity() {
        Intent intent = new Intent(ChangePassword.this, MainActivity.class);
        intent.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK | Intent.FLAG_ACTIVITY_CLEAR_TASK);
        startActivity(intent);
        finish();
    }
}