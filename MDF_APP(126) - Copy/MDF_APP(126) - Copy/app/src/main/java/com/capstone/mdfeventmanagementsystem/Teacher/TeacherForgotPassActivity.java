package com.capstone.mdfeventmanagementsystem.Teacher;

import android.os.Bundle;
import android.text.TextUtils;
import android.view.View;
import android.widget.Button;
import android.widget.EditText;
import android.widget.ImageView;
import android.widget.TextView;
import android.widget.Toast;

import androidx.appcompat.app.AppCompatActivity;
import androidx.cardview.widget.CardView;

import com.capstone.mdfeventmanagementsystem.R;
import com.capstone.mdfeventmanagementsystem.Utilities.BaseActivity;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.database.DataSnapshot;
import com.google.firebase.database.DatabaseError;
import com.google.firebase.database.DatabaseReference;
import com.google.firebase.database.FirebaseDatabase;
import com.google.firebase.database.ValueEventListener;

public class TeacherForgotPassActivity extends BaseActivity {

    // UI Elements
    private EditText emailEditText;
    private Button sendButton;
    private Button loginPromptButton;
    private ImageView backButton;
    private TextView backText;
    private CardView page1, page2;

    // Firebase
    private FirebaseAuth mAuth;
    private DatabaseReference teachersRef;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_teacher_forgot_pass);

        // Initialize Firebase Auth and Database
        mAuth = FirebaseAuth.getInstance();
        teachersRef = FirebaseDatabase.getInstance().getReference("teachers");

        // Initialize UI elements
        initializeViews();

        // Set click listeners
        setupClickListeners();
    }

    private void initializeViews() {
        emailEditText = findViewById(R.id.email);
        sendButton = findViewById(R.id.sendBtn);
        loginPromptButton = findViewById(R.id.loginPrompt);
        backButton = findViewById(R.id.backBtn);
        backText = findViewById(R.id.back_text);
        page1 = findViewById(R.id.page1);
        page2 = findViewById(R.id.page2);
    }

    private void setupClickListeners() {
        // Send password reset email button
        sendButton.setOnClickListener(v -> {
            String email = emailEditText.getText().toString().trim();
            validateAndSendPasswordResetEmail(email);
        });

        // Back button
        View.OnClickListener backClickListener = v -> finish();
        backButton.setOnClickListener(backClickListener);
        backText.setOnClickListener(backClickListener);

        // Login button from confirmation page
        loginPromptButton.setOnClickListener(v -> {
            // Close this activity to go back to login screen
            finish();
        });
    }

    private void validateAndSendPasswordResetEmail(String email) {
        // Validate email
        if (TextUtils.isEmpty(email)) {
            emailEditText.setError("Email is required");
            return;
        }

        // Show loading (optional: add a ProgressBar or ProgressDialog here)
        // e.g., ProgressBar progressBar = findViewById(R.id.progressBar);
        // progressBar.setVisibility(View.VISIBLE);

        // Check if email exists in teachers collection
        teachersRef.orderByChild("email").equalTo(email).addListenerForSingleValueEvent(new ValueEventListener() {
            @Override
            public void onDataChange(DataSnapshot dataSnapshot) {
                // Hide loading
                // progressBar.setVisibility(View.GONE);

                if (dataSnapshot.exists()) {
                    // Email found in teachers collection, proceed with password reset
                    sendPasswordResetEmail(email);
                } else {
                    // Email not found in teachers collection
                    emailEditText.setError("No teacher account found with this email address.");
                }
            }

            @Override
            public void onCancelled(DatabaseError databaseError) {
                // Hide loading
                // progressBar.setVisibility(View.GONE);

                // Handle database error
                Toast.makeText(TeacherForgotPassActivity.this,
                        "Error checking teacher database: " + databaseError.getMessage(),
                        Toast.LENGTH_LONG).show();
            }
        });
    }

    private void sendPasswordResetEmail(String email) {
        // Send password reset email
        mAuth.sendPasswordResetEmail(email)
                .addOnCompleteListener(task -> {
                    if (task.isSuccessful()) {
                        // Show success page
                        page1.setVisibility(View.GONE);
                        page2.setVisibility(View.VISIBLE);
                        Toast.makeText(TeacherForgotPassActivity.this,
                                "Password reset email sent successfully",
                                Toast.LENGTH_SHORT).show();
                    } else {
                        // Show error message
                        String errorMessage = task.getException() != null ?
                                task.getException().getMessage() :
                                "Failed to send password reset email";
                        Toast.makeText(TeacherForgotPassActivity.this,
                                errorMessage,
                                Toast.LENGTH_LONG).show();
                    }
                });
    }
}