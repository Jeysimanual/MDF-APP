package com.capstone.mdfeventmanagementsystem.Student;

import android.content.Intent;
import android.content.res.ColorStateList;
import android.graphics.Color;
import android.os.Bundle;
import android.util.Log;
import android.view.View;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.capstone.mdfeventmanagementsystem.Adapters.EventTicketAdapter;
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

public class StudentTickets extends BaseActivity {

    private RecyclerView recyclerView;
    private EventTicketAdapter adapter;
    private List<EventTicket> ticketList;

    private DatabaseReference studentTicketsRef;
    private FirebaseAuth mAuth;
    private static final String TAG = "ticketTesting";

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_student_tickets);

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
                Intent intent = new Intent(StudentTickets.this, ProfileActivity.class);
                startActivity(intent);
            }
        });

        setupBottomNavigation();

        recyclerView = findViewById(R.id.recyclerViewTickets);
        recyclerView.setLayoutManager(new LinearLayoutManager(this));

        ticketList = new ArrayList<>();
        adapter = new EventTicketAdapter(StudentTickets.this, ticketList);
        recyclerView.setAdapter(adapter);

        mAuth = FirebaseAuth.getInstance();
        FirebaseUser currentUser = mAuth.getCurrentUser();

        if (currentUser != null) {
            fetchStudentUID();
        } else {
            Log.e(TAG, "User not logged in!");
            Toast.makeText(this, "User not logged in!", Toast.LENGTH_SHORT).show();
            finish();
        }
    }

    private void fetchStudentUID() {
        FirebaseUser currentUser = mAuth.getCurrentUser();
        if (currentUser == null) {
            Log.e(TAG, "No user logged in!");
            Toast.makeText(StudentTickets.this, "User not logged in!", Toast.LENGTH_SHORT).show();
            finish();
            return;
        }

        String userEmail = currentUser.getEmail();
        if (userEmail == null) {
            Log.e(TAG, "User email is null!");
            Toast.makeText(StudentTickets.this, "Error fetching user email!", Toast.LENGTH_SHORT).show();
            return;
        }

        DatabaseReference studentsRef = FirebaseDatabase.getInstance().getReference("students");
        studentsRef.orderByChild("email").equalTo(userEmail).addListenerForSingleValueEvent(new ValueEventListener() {
            @Override
            public void onDataChange(@NonNull DataSnapshot snapshot) {
                if (!snapshot.exists()) {
                    Log.w(TAG, "No matching student found in database!");
                    Toast.makeText(StudentTickets.this, "No student data found!", Toast.LENGTH_SHORT).show();
                    return;
                }

                for (DataSnapshot studentSnapshot : snapshot.getChildren()) {
                    String studentUID = studentSnapshot.getKey();
                    if (studentUID != null) {
                        Log.d(TAG, "Student UID found: " + studentUID);
                        fetchTicketsForStudent(studentUID);
                    }
                }
            }

            @Override
            public void onCancelled(@NonNull DatabaseError error) {
                Log.e(TAG, "Error fetching student UID: " + error.getMessage());
            }
        });
    }


    private void fetchTicketsForStudent(String studentUID) {
        studentTicketsRef = FirebaseDatabase.getInstance()
                .getReference("students")
                .child(studentUID)
                .child("tickets");

        Log.d(TAG, "Fetching tickets for student UID: " + studentUID);

        studentTicketsRef.addValueEventListener(new ValueEventListener() {
            @Override
            public void onDataChange(@NonNull DataSnapshot snapshot) {
                ticketList.clear();
                if (!snapshot.exists()) {
                    Log.w(TAG, "No tickets found for student: " + studentUID);
                    Toast.makeText(StudentTickets.this, "No tickets found!", Toast.LENGTH_SHORT).show();
                    return;
                }

                for (DataSnapshot ticketSnapshot : snapshot.getChildren()) {
                    String eventUID = ticketSnapshot.getKey();
                    String qrCodeUrl = ticketSnapshot.child("qrCodeUrl").getValue(String.class);
                    String ticketID = ticketSnapshot.child("ticketID").getValue(String.class);

                    if (eventUID != null && qrCodeUrl != null && ticketID != null) {
                        fetchEventDetails(eventUID, qrCodeUrl, ticketID);
                    } else {
                        Log.e(TAG, "Missing ticket details!");
                    }
                }
            }

            @Override
            public void onCancelled(@NonNull DatabaseError error) {
                Log.e(TAG, "Error fetching tickets: " + error.getMessage());
            }
        });
    }

    private void fetchEventDetails(String eventUID, String qrCodeUrl, String ticketID) {
        DatabaseReference eventRef = FirebaseDatabase.getInstance().getReference("events").child(eventUID);

        eventRef.addListenerForSingleValueEvent(new ValueEventListener() {
            @Override
            public void onDataChange(@NonNull DataSnapshot snapshot) {
                if (!snapshot.exists()) {
                    Log.w(TAG, "Event not found for UID: " + eventUID);
                    return;
                }

                String eventName = snapshot.child("eventName").getValue(String.class);
                String eventType = snapshot.child("eventType").getValue(String.class);
                String startDate = snapshot.child("startDate").getValue(String.class);
                String endDate = snapshot.child("endDate").getValue(String.class);
                String startTime = snapshot.child("startTime").getValue(String.class);
                String endTime = snapshot.child("endTime").getValue(String.class);
                String graceTime = snapshot.child("graceTime").getValue(String.class);
                String eventSpan = snapshot.child("eventSpan").getValue(String.class);
                String venue = snapshot.child("venue").getValue(String.class);
                String eventDescription = snapshot.child("eventDescription").getValue(String.class);

                if (eventName != null && eventType != null && startDate != null && startTime != null && venue != null) {
                    EventTicket ticket = new EventTicket(
                            eventName, eventType, startDate, endDate, startTime, endTime,
                            graceTime, eventSpan, venue, eventDescription, qrCodeUrl, ticketID
                    );

                    ticketList.add(ticket);
                    adapter.notifyDataSetChanged();
                } else {
                    Log.e(TAG, "Missing event details for EventUID: " + eventUID);
                }
            }

            @Override
            public void onCancelled(@NonNull DatabaseError error) {
                Log.e(TAG, "Error fetching event details: " + error.getMessage());
            }
        });
    }


    private void setupBottomNavigation() {
        BottomNavigationView bottomNavigationView = findViewById(R.id.bottom_navigation);
        bottomNavigationView.setSelectedItemId(R.id.nav_ticket);
        bottomNavigationView.setBackground(null);

        bottomNavigationView.setOnNavigationItemSelectedListener(item -> {
            int itemId = item.getItemId();
            if (itemId == R.id.nav_home) {
                startActivity(new Intent(getApplicationContext(), MainActivity2.class));
                overridePendingTransition(0, 0);
                finish();
                return true;
            } else if (itemId == R.id.nav_event) {
                startActivity(new Intent(getApplicationContext(), StudentDashboard.class));
                overridePendingTransition(0, 0);
                finish();
                return true;
            } else if (itemId == R.id.nav_ticket) {
                return true;
            } else if (itemId == R.id.nav_cert) {
                startActivity(new Intent(getApplicationContext(), StudentCertificate.class));
                overridePendingTransition(0, 0);
                finish();
                return true;
            }
            return false;
        });
    }
}