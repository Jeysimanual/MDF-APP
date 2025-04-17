package com.capstone.mdfeventmanagementsystem.Student;

import android.content.SharedPreferences;
import android.os.Bundle;
import android.util.Log;
import android.widget.ImageView;
import android.widget.TextView;

import androidx.appcompat.app.AppCompatActivity;

import com.bumptech.glide.Glide;
import com.capstone.mdfeventmanagementsystem.R;
import com.google.firebase.database.DataSnapshot;
import com.google.firebase.database.DatabaseError;
import com.google.firebase.database.DatabaseReference;
import com.google.firebase.database.FirebaseDatabase;
import com.google.firebase.database.ValueEventListener;

public class StudentCertificateInside extends AppCompatActivity {

    private static final String TAG = "StudentCertInside";
    private ImageView certificateImageView;
    private TextView templateNameTextView;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_student_certificate_inside);

        // Initialize views
        certificateImageView = findViewById(R.id.certificateImageView);
        templateNameTextView = findViewById(R.id.templateNameTextView);

        // Get certificate key from intent
        String certificateKey = getIntent().getStringExtra("certificateKey");
        if (certificateKey == null || certificateKey.isEmpty()) {
            Log.e(TAG, "No certificate key provided.");
            finish(); // Close activity if no key
            return;
        }

        // Get student ID from SharedPreferences
        SharedPreferences sharedPreferences = getSharedPreferences("UserSession", MODE_PRIVATE);
        String studentID = sharedPreferences.getString("studentID", null);
        if (studentID == null) {
            Log.e(TAG, "No studentID found in SharedPreferences.");
            finish(); // Close activity if no student ID
            return;
        }

        // Reference to certificate in Firebase
        DatabaseReference certRef = FirebaseDatabase.getInstance()
                .getReference("students")
                .child(studentID)
                .child("certificates")
                .child(certificateKey);

        Log.d(TAG, "Fetching certificate at: students/" + studentID + "/certificates/" + certificateKey);

        // Add try-catch to handle any exceptions during data loading
        try {
            certRef.addListenerForSingleValueEvent(new ValueEventListener() {
                @Override
                public void onDataChange(DataSnapshot snapshot) {
                    if (!snapshot.exists()) {
                        Log.e(TAG, "Certificate not found in database");
                        return;
                    }

                    try {
                        // Get certificate data
                        String templateName = snapshot.child("templateName").getValue(String.class);
                        String previewImageUrl = snapshot.child("previewImageUrl").getValue(String.class);

                        // Set template name
                        if (templateName != null) {
                            templateNameTextView.setText(templateName);
                        } else {
                            templateNameTextView.setText("Certificate");
                        }

                        // Load image with error handling
                        if (previewImageUrl != null && !previewImageUrl.isEmpty()) {
                            Glide.with(StudentCertificateInside.this)
                                    .load(previewImageUrl)
                                    .placeholder(R.drawable.cert_nav)
                                    .error(R.drawable.cert_nav)
                                    .into(certificateImageView);
                        } else {
                            Log.e(TAG, "PreviewImageUrl is missing.");
                            // Set a default image
                            certificateImageView.setImageResource(R.drawable.cert_nav);
                        }
                    } catch (Exception e) {
                        Log.e(TAG, "Error parsing certificate data: " + e.getMessage());
                    }
                }

                @Override
                public void onCancelled(DatabaseError error) {
                    Log.e(TAG, "Database error: " + error.getMessage());
                }
            });
        } catch (Exception e) {
            Log.e(TAG, "Error setting up database listener: " + e.getMessage());
        }
    }
}
