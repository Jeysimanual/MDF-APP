package com.capstone.mdfeventmanagementsystem;

import android.content.Intent;
import android.os.Bundle;
import android.util.Log;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

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

public class StudentTickets extends AppCompatActivity {

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

        setupBottomNavigation();

        recyclerView = findViewById(R.id.recyclerViewTickets);
        recyclerView.setLayoutManager(new LinearLayoutManager(this));

        ticketList = new ArrayList<>();
        adapter = new EventTicketAdapter(this, ticketList);
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
        DatabaseReference studentsRef = FirebaseDatabase.getInstance().getReference("students");
        Log.d(TAG, "Fetching student UID from database...");

        studentsRef.addListenerForSingleValueEvent(new ValueEventListener() {
            @Override
            public void onDataChange(@NonNull DataSnapshot snapshot) {
                if (!snapshot.exists()) {
                    Log.w(TAG, "No students found in database!");
                    Toast.makeText(StudentTickets.this, "No student data found!", Toast.LENGTH_SHORT).show();
                    return;
                }

                for (DataSnapshot studentSnapshot : snapshot.getChildren()) {
                    String studentUID = studentSnapshot.getKey();
                    if (studentUID != null) {
                        Log.d(TAG, "Student UID found: " + studentUID);
                        fetchTicketsForStudent(studentUID);
                        break;
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
                String startTime = snapshot.child("startTime").getValue(String.class);
                String venue = snapshot.child("venue").getValue(String.class);

                if (eventName != null && eventType != null && startDate != null && startTime != null && venue != null) {
                    EventTicket ticket = new EventTicket(eventName, eventType, startDate, startTime, venue, qrCodeUrl, ticketID);
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
            } else if (itemId == R.id.nav_scan) {
                startActivity(new Intent(getApplicationContext(), QRCheckInActivity.class));
                overridePendingTransition(0, 0);
                finish();
                return true;
            }
            return false;
        });
    }
}