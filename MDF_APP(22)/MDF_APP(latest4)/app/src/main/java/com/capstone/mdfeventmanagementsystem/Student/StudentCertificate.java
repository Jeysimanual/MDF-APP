package com.capstone.mdfeventmanagementsystem.Student;

import android.content.Intent;
import android.content.SharedPreferences;
import android.content.res.ColorStateList;
import android.graphics.Color;
import android.os.Bundle;
import android.util.Log;
import android.view.View;

import androidx.appcompat.app.AppCompatActivity;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.capstone.mdfeventmanagementsystem.Adapters.CertificateAdapter;
import com.capstone.mdfeventmanagementsystem.R;
import com.capstone.mdfeventmanagementsystem.Utilities.BaseActivity;
import com.google.android.material.bottomappbar.BottomAppBar;
import com.google.android.material.bottomnavigation.BottomNavigationView;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseUser;
import com.google.firebase.database.DataSnapshot;
import com.google.firebase.database.DatabaseError;
import com.google.firebase.database.DatabaseReference;
import com.google.firebase.database.FirebaseDatabase;
import com.google.firebase.database.ValueEventListener;

import java.util.ArrayList;
import java.util.List;

public class StudentCertificate extends BaseActivity {

    private RecyclerView recyclerView;
    private CertificateAdapter certificateAdapter;
    private List<Certificate> certificateList = new ArrayList<>();
    private DatabaseReference certRef;
    private static final String TAG = "certTesting";

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_student_certificate);

        findViewById(R.id.fab_scan).setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                startActivity(new Intent(getApplicationContext(), QRCheckInActivity.class));
                overridePendingTransition(0, 0);
            }
        });

        BottomAppBar bottomAppBar = findViewById(R.id.bottomAppBar);
        bottomAppBar.setBackgroundTint(ColorStateList.valueOf(Color.WHITE));

        findViewById(R.id.profile_image).setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                Intent intent = new Intent(StudentCertificate.this, ProfileActivity.class);
                startActivity(intent);
            }
        });

        recyclerView = findViewById(R.id.recyclerViewCert);
        recyclerView.setLayoutManager(new LinearLayoutManager(this));

        certificateAdapter = new CertificateAdapter(this, certificateList);
        recyclerView.setAdapter(certificateAdapter);

        setupBottomNavigation();
        fetchCertificates();
    }

    private void fetchCertificates() {
        SharedPreferences sharedPreferences = getSharedPreferences("UserSession", MODE_PRIVATE);
        String studentID = sharedPreferences.getString("studentID", null);

        if (studentID == null) {
            Log.e(TAG, "No studentID found in SharedPreferences.");
            return;
        }

        certRef = FirebaseDatabase.getInstance()
                .getReference("students")
                .child(studentID)
                .child("certificates");

        Log.d(TAG, "Fetching certificates from path: students/" + studentID + "/certificates");

        certRef.addValueEventListener(new ValueEventListener() {
            @Override
            public void onDataChange(DataSnapshot snapshot) {
                Log.d(TAG, "onDataChange triggered.");
                certificateList.clear();

                for (DataSnapshot certSnap : snapshot.getChildren()) {
                    Certificate certificate = certSnap.getValue(Certificate.class);
                    if (certificate != null) {
                        // ✅ Add this line to assign the Firebase key
                        certificate.setCertificateKey(certSnap.getKey());

                        certificateList.add(certificate);
                        Log.d(TAG, "Fetched certificate: " + certificate.getTemplateName() + " | Received: " + certificate.getReceivedDate());
                    } else {
                        Log.d(TAG, "Null certificate found in snapshot.");
                    }
                }

                Log.d(TAG, "Total certificates fetched: " + certificateList.size());
                certificateAdapter.notifyDataSetChanged();
            }

            @Override
            public void onCancelled(DatabaseError error) {
                Log.e(TAG, "Database error: " + error.getMessage());
            }
        });
    }



    private void setupBottomNavigation() {
        BottomNavigationView bottomNavigationView = findViewById(R.id.bottom_navigation);
        bottomNavigationView.setSelectedItemId(R.id.nav_cert);
        bottomNavigationView.setBackground(null);

        bottomNavigationView.setOnItemSelectedListener(item -> {
            int itemId = item.getItemId();

            if (itemId == R.id.nav_home) {
                startActivity(new Intent(this, MainActivity2.class));
                finish();
            } else if (itemId == R.id.nav_event) {
                startActivity(new Intent(this, StudentDashboard.class));
                finish();
            } else if (itemId == R.id.nav_ticket) {
                startActivity(new Intent(this, StudentTickets.class));
                finish();
            }  else if (itemId == R.id.nav_cert) {
                return true;
            }

            overridePendingTransition(0, 0);
            return true;
        });
    }
}
