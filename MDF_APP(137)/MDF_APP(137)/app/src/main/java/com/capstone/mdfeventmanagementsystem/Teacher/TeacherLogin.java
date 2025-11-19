package com.capstone.mdfeventmanagementsystem.Teacher;

import android.annotation.SuppressLint;
import android.content.Intent;
import android.content.SharedPreferences;
import android.os.Bundle;
import android.text.method.PasswordTransformationMethod;
import android.util.Log;
import android.view.MotionEvent;
import android.view.View;
import android.widget.Button;
import android.widget.EditText;
import android.widget.ImageView;
import android.widget.ProgressBar;
import android.widget.TextView;
import android.widget.Toast;

import androidx.appcompat.app.AppCompatActivity;

import com.capstone.mdfeventmanagementsystem.MainActivity;
import com.capstone.mdfeventmanagementsystem.R;
import com.capstone.mdfeventmanagementsystem.Utilities.BaseActivity;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseUser;
import com.google.firebase.database.DataSnapshot;
import com.google.firebase.database.DatabaseError;
import com.google.firebase.database.DatabaseReference;
import com.google.firebase.database.FirebaseDatabase;
import com.google.firebase.database.ValueEventListener;
import com.google.firebase.messaging.FirebaseMessaging;

public class TeacherLogin extends BaseActivity {
    private static final String TAG = "TeacherLoginTest";
    private EditText etEmail, etPassword;
    private Button btnLogin;
    private FirebaseAuth auth;
    private DatabaseReference databaseReference;
    private ProgressBar loginProgressBar;
    private ImageView backBtn;
    private TextView backText;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_teacher_login);
        Log.d(TAG, "onCreate: Initializing TeacherLogin activity");

        etEmail = findViewById(R.id.etEmail);
        etPassword = findViewById(R.id.etPassword);
        btnLogin = findViewById(R.id.btnLogin);
        backBtn = findViewById(R.id.backBtn);
        backText = findViewById(R.id.back_text);
        loginProgressBar = findViewById(R.id.loginProgressBar);
        Log.d(TAG, "onCreate: UI components initialized");

        auth = FirebaseAuth.getInstance();
        databaseReference = FirebaseDatabase.getInstance().getReference("teachers");
        Log.d(TAG, "onCreate: Firebase Auth and Database references initialized");

        setPasswordToggle();
        Log.d(TAG, "onCreate: Password toggle set up");

        SharedPreferences sharedPreferences = getSharedPreferences("UserSession", MODE_PRIVATE);
        boolean isLoggedIn = sharedPreferences.getBoolean("isLoggedIn", false);
        String userType = sharedPreferences.getString("userType", "");
        Log.d(TAG, "onCreate: Checked SharedPreferences - isLoggedIn: " + isLoggedIn + ", userType: " + userType);

        if (isLoggedIn && "teacher".equals(userType)) {
            Log.i(TAG, "onCreate: Teacher already logged in, redirecting to TeacherDashboard");
            Intent intent = new Intent(TeacherLogin.this, TeacherDashboard.class);
            startActivity(intent);
            finish();
            return;
        }

        btnLogin.setOnClickListener(v -> {
            Log.d(TAG, "onCreate: Login button clicked");
            loginTeacher();
        });
        backBtn.setOnClickListener(v -> {
            Log.d(TAG, "onCreate: Back button clicked");
            goBackToMain();
        });
        backText.setOnClickListener(v -> {
            Log.d(TAG, "onCreate: Back text clicked");
            goBackToMain();
        });

        TextView forgotPassText = findViewById(R.id.forgot_pass_teacher);
        forgotPassText.setOnClickListener(view -> {
            Log.d(TAG, "onCreate: Forgot password text clicked");
            Intent intent = new Intent(this, TeacherForgotPassActivity.class);
            startActivity(intent);
        });
        Log.d(TAG, "onCreate: Forgot password listener set up");
    }

    @SuppressLint("ClickableViewAccessibility")
    private void setPasswordToggle() {
        Log.d(TAG, "setPasswordToggle: Setting up password visibility toggle");
        etPassword.setOnTouchListener((v, event) -> {
            if (event.getAction() == MotionEvent.ACTION_UP) {
                Log.d(TAG, "setPasswordToggle: Touch event detected on password field");
                int drawableRightIndex = 2;
                if (etPassword.getCompoundDrawablesRelative()[drawableRightIndex] != null) {
                    int drawableWidth = etPassword.getCompoundDrawablesRelative()[drawableRightIndex].getBounds().width();
                    int touchAreaStart = etPassword.getRight() - etPassword.getPaddingEnd() - drawableWidth;

                    if (event.getRawX() >= touchAreaStart) {
                        Log.d(TAG, "setPasswordToggle: Touch within toggle icon area");
                        togglePasswordVisibility();
                        return true;
                    } else {
                        Log.d(TAG, "setPasswordToggle: Touch outside toggle icon area");
                    }
                } else {
                    Log.w(TAG, "setPasswordToggle: No drawable found for password toggle");
                }
            }
            return false;
        });
    }

    private void togglePasswordVisibility() {
        Log.d(TAG, "togglePasswordVisibility: Toggling password visibility");
        if (etPassword.getTransformationMethod() instanceof PasswordTransformationMethod) {
            etPassword.setTransformationMethod(null);
            etPassword.setCompoundDrawablesRelativeWithIntrinsicBounds(0, 0, R.drawable.view, 0);
            Log.d(TAG, "togglePasswordVisibility: Password set to visible");
        } else {
            etPassword.setTransformationMethod(PasswordTransformationMethod.getInstance());
            etPassword.setCompoundDrawablesRelativeWithIntrinsicBounds(0, 0, R.drawable.hide, 0);
            Log.d(TAG, "togglePasswordVisibility: Password set to hidden");
        }
        etPassword.setSelection(etPassword.getText().length());
        Log.d(TAG, "togglePasswordVisibility: Cursor moved to end of password field");
    }

    private void setLoading(boolean isLoading) {
        Log.d(TAG, "setLoading: Setting loading state to " + isLoading);
        if (isLoading) {
            loginProgressBar.setVisibility(View.VISIBLE);
            btnLogin.setText("");
            Log.d(TAG, "setLoading: ProgressBar visible, button text cleared");
        } else {
            loginProgressBar.setVisibility(View.GONE);
            btnLogin.setText("Login");
            Log.d(TAG, "setLoading: ProgressBar hidden, button text restored");
        }
        btnLogin.setEnabled(!isLoading);
        Log.d(TAG, "setLoading: Login button enabled: " + !isLoading);
    }

    private void loginTeacher() {
        String email = etEmail.getText().toString().trim();
        String password = etPassword.getText().toString().trim();
        Log.d(TAG, "loginTeacher: Attempting login with email: " + email);

        if (email.isEmpty() || password.isEmpty()) {
            Log.w(TAG, "loginTeacher: Email or password empty");
            Toast.makeText(this, "Please enter both email and password", Toast.LENGTH_SHORT).show();
            return;
        }

        setLoading(true);
        Log.d(TAG, "loginTeacher: Querying Firebase for teacher with email: " + email);

        databaseReference.orderByChild("email").equalTo(email)
                .addListenerForSingleValueEvent(new ValueEventListener() {
                    @Override
                    public void onDataChange(DataSnapshot dataSnapshot) {
                        Log.d(TAG, "loginTeacher: Database query completed, snapshot exists: " + dataSnapshot.exists());
                        if (dataSnapshot.exists()) {
                            for (DataSnapshot snapshot : dataSnapshot.getChildren()) {
                                String role = snapshot.child("role").getValue(String.class);
                                String teacherId = snapshot.getKey();
                                Log.d(TAG, "loginTeacher: Found teacher - ID: " + teacherId + ", Role: " + role);

                                if ("teacher".equals(role)) {
                                    Boolean verified = snapshot.child("verified").getValue(Boolean.class);
                                    Log.d(TAG, "loginTeacher: Verification status: " + (verified != null ? verified : "null"));
                                    if (verified == null || !verified) {
                                        setLoading(false);
                                        Log.w(TAG, "loginTeacher: Account not verified for teacher ID: " + teacherId);
                                        Toast.makeText(TeacherLogin.this,
                                                "Your account is not verified", Toast.LENGTH_SHORT).show();
                                        return;
                                    }

                                    Log.d(TAG, "loginTeacher: Initiating Firebase Auth sign-in");
                                    auth.signInWithEmailAndPassword(email, password)
                                            .addOnCompleteListener(task -> {
                                                Log.d(TAG, "loginTeacher: Firebase Auth attempt completed, success: " + task.isSuccessful());
                                                if (task.isSuccessful()) {
                                                    FirebaseUser user = auth.getCurrentUser();
                                                    if (user != null) {
                                                        Log.i(TAG, "loginTeacher: Auth successful, user UID: " + user.getUid());

                                                        // === SESSION SAVED ===
                                                        saveTeacherSession(teacherId, email);

                                                        // === UPDATE FCM TOKEN ===
                                                        updateFcmToken(teacherId);

                                                        // === SUCCESS TOAST (NEW) ===
                                                        Toast.makeText(TeacherLogin.this, "Login Successfully", Toast.LENGTH_SHORT).show();

                                                        // === NAVIGATE TO DASHBOARD ===
                                                        navigateToDashboard();
                                                    } else {
                                                        setLoading(false);
                                                        Log.e(TAG, "loginTeacher: FirebaseUser is null after successful auth");
                                                        Toast.makeText(TeacherLogin.this,
                                                                "Authentication error: User data missing", Toast.LENGTH_SHORT).show();
                                                    }
                                                } else {
                                                    setLoading(false);
                                                    Log.e(TAG, "loginTeacher: Authentication failed: " + task.getException().getMessage(), task.getException());
                                                    Toast.makeText(TeacherLogin.this,
                                                            "Authentication Failed: " + task.getException().getMessage(),
                                                            Toast.LENGTH_SHORT).show();
                                                }
                                            });
                                } else {
                                    setLoading(false);
                                    Log.w(TAG, "loginTeacher: User is not a teacher, role: " + role);
                                    Toast.makeText(TeacherLogin.this,
                                            "You are not authorized as a Teacher", Toast.LENGTH_SHORT).show();
                                }
                            }
                        } else {
                            setLoading(false);
                            Log.w(TAG, "loginTeacher: No teacher found with email: " + email);
                            Toast.makeText(TeacherLogin.this,
                                    "No teacher found with this email", Toast.LENGTH_SHORT).show();
                        }
                    }

                    @Override
                    public void onCancelled(DatabaseError databaseError) {
                        setLoading(false);
                        Log.e(TAG, "loginTeacher: Database query cancelled: " + databaseError.getMessage(), databaseError.toException());
                        Toast.makeText(TeacherLogin.this,
                                "Database error: " + databaseError.getMessage(), Toast.LENGTH_SHORT).show();
                    }
                });
    }

    private void saveTeacherSession(String teacherId, String email) {
        SharedPreferences sharedPreferences = getSharedPreferences("UserSession", MODE_PRIVATE);
        SharedPreferences.Editor editor = sharedPreferences.edit();
        editor.putBoolean("isLoggedIn", true);
        editor.putString("userType", "teacher");
        editor.putString("teacherId", teacherId);
        editor.putString("email", email);
        editor.apply();
        Log.d(TAG, "saveTeacherSession: Teacher session saved - teacherId: " + teacherId + ", email: " + email);
    }

    private void updateFcmToken(String teacherId) {
        Log.d(TAG, "updateFcmToken: Updating FCM token for teacherId: " + teacherId);
        FirebaseMessaging.getInstance().getToken()
                .addOnCompleteListener(task -> {
                    if (!task.isSuccessful()) {
                        Log.w(TAG, "updateFcmToken: Fetching FCM token failed", task.getException());
                        return;
                    }
                    String token = task.getResult();
                    if (token == null) {
                        Log.e(TAG, "updateFcmToken: FCM token is null");
                        return;
                    }
                    Log.d(TAG, "updateFcmToken: FCM token generated: " + token.substring(0, 30) + "...");
                    FirebaseDatabase.getInstance()
                            .getReference("teachers")
                            .child(teacherId)
                            .child("fcmToken")
                            .setValue(token)
                            .addOnSuccessListener(aVoid -> {
                                Log.d(TAG, "updateFcmToken: FCM token saved successfully at /teachers/" + teacherId + "/fcmToken");
                            })
                            .addOnFailureListener(e -> {
                                Log.e(TAG, "updateFcmToken: Failed to save FCM token", e);
                                Toast.makeText(TeacherLogin.this, "Notification setup failed", Toast.LENGTH_LONG).show();
                            });
                });
    }

    private void navigateToDashboard() {
        Log.d(TAG, "navigateToDashboard: Navigating to TeacherDashboard");
        Intent intent = new Intent(TeacherLogin.this, TeacherDashboard.class);
        intent.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK | Intent.FLAG_ACTIVITY_CLEAR_TASK);
        startActivity(intent);
        finish();
    }

    private void goBackToMain() {
        Log.d(TAG, "goBackToMain: Navigating back to MainActivity");
        Intent intent = new Intent(TeacherLogin.this, MainActivity.class);
        startActivity(intent);
        overridePendingTransition(R.anim.slide_in_left, R.anim.slide_out_right);
        finish();
        Log.d(TAG, "goBackToMain: Activity finished");
    }
}