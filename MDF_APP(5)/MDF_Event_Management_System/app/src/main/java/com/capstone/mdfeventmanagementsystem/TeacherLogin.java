package com.capstone.mdfeventmanagementsystem;

import android.content.Intent;
import android.os.Bundle;
import android.widget.Button;
import android.widget.EditText;
import android.widget.Toast;

import androidx.activity.EdgeToEdge;
import androidx.appcompat.app.AppCompatActivity;

import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseUser;
import com.google.firebase.database.DataSnapshot;
import com.google.firebase.database.DatabaseError;
import com.google.firebase.database.DatabaseReference;
import com.google.firebase.database.FirebaseDatabase;
import com.google.firebase.database.ValueEventListener;

public class TeacherLogin extends AppCompatActivity {

    private EditText etEmail, etPassword;
    private Button btnLogin;
    private FirebaseAuth auth;
    private DatabaseReference databaseReference;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        EdgeToEdge.enable(this);
        setContentView(R.layout.activity_teacher_login);

        etEmail = findViewById(R.id.etEmail);
        etPassword = findViewById(R.id.etPassword);
        btnLogin = findViewById(R.id.btnLogin);

        auth = FirebaseAuth.getInstance();
        databaseReference = FirebaseDatabase.getInstance().getReference("teachers");

        btnLogin.setOnClickListener(v -> loginTeacher());
    }

    private void loginTeacher() {
        String email = etEmail.getText().toString().trim();
        String password = etPassword.getText().toString();

        if (email.isEmpty() || password.isEmpty()) {
            Toast.makeText(this, "Please enter both email and password", Toast.LENGTH_SHORT).show();
            return;
        }

        // Check if email exists in the "teachers" collection
        databaseReference.orderByChild("email").equalTo(email)
                .addListenerForSingleValueEvent(new ValueEventListener() {
                    @Override
                    public void onDataChange(DataSnapshot dataSnapshot) {
                        if (dataSnapshot.exists()) {
                            for (DataSnapshot snapshot : dataSnapshot.getChildren()) {
                                String role = snapshot.child("role").getValue(String.class);

                                if (role != null && role.equals("teacher")) {
                                    // Authenticate with FirebaseAuth
                                    auth.signInWithEmailAndPassword(email, password)
                                            .addOnCompleteListener(task -> {
                                                if (task.isSuccessful()) {
                                                    FirebaseUser user = auth.getCurrentUser();
                                                    if (user != null) {
                                                        Toast.makeText(TeacherLogin.this, "Login Successful!", Toast.LENGTH_SHORT).show();
                                                        startActivity(new Intent(TeacherLogin.this, TeacherDashboard.class));
                                                        finish();

                                                        // Go to scanning activity after login
                                                        startActivity(new Intent(TeacherLogin.this, TeacherScanning.class));
                                                    }
                                                } else {
                                                    Toast.makeText(TeacherLogin.this, "Authentication Failed: " + task.getException().getMessage(), Toast.LENGTH_SHORT).show();
                                                }
                                            });
                                } else {
                                    Toast.makeText(TeacherLogin.this, "You are not authorized as a Teacher", Toast.LENGTH_SHORT).show();
                                }
                            }
                        } else {
                            Toast.makeText(TeacherLogin.this, "No teacher found with this email", Toast.LENGTH_SHORT).show();
                        }
                    }

                    @Override
                    public void onCancelled(DatabaseError databaseError) {
                        Toast.makeText(TeacherLogin.this, "Database error: " + databaseError.getMessage(), Toast.LENGTH_SHORT).show();
                    }
                });
    }
}
