package com.capstone.mdfeventmanagementsystem.Teacher;

import android.app.NotificationChannel;
import android.app.NotificationManager;
import android.app.PendingIntent;
import android.content.Context;
import android.content.Intent;
import android.os.Bundle;
import android.util.Log;

import androidx.activity.EdgeToEdge;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.app.NotificationCompat;
import androidx.core.graphics.Insets;
import androidx.core.view.ViewCompat;
import androidx.core.view.WindowInsetsCompat;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.capstone.mdfeventmanagementsystem.Adapters.NotificationAdapter;
import com.capstone.mdfeventmanagementsystem.Models.NotificationItem;
import com.capstone.mdfeventmanagementsystem.R;
import com.google.firebase.database.DataSnapshot;
import com.google.firebase.database.DatabaseError;
import com.google.firebase.database.DatabaseReference;
import com.google.firebase.database.FirebaseDatabase;
import com.google.firebase.database.ValueEventListener;

import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.List;

public class NotificationTeacher extends AppCompatActivity {

    private static final String TAG = "NotificationTeacher";
    private static final String CHANNEL_ID = "event_notifications";
    private static final int NOTIFICATION_ID = 1;
    private DatabaseReference notificationItemsRef;
    private RecyclerView recyclerViewNotifications;
    private NotificationAdapter notificationAdapter;
    private List<NotificationItem> notificationList = new ArrayList<>();

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        EdgeToEdge.enable(this);
        setContentView(R.layout.activity_notification_teacher);
        ViewCompat.setOnApplyWindowInsetsListener(findViewById(R.id.main), (v, insets) -> {
            Insets systemBars = insets.getInsets(WindowInsetsCompat.Type.systemBars());
            v.setPadding(systemBars.left, systemBars.top, systemBars.right, systemBars.bottom);
            return insets;
        });

        // Initialize RecyclerView
        recyclerViewNotifications = findViewById(R.id.recyclerViewNotifications);
        recyclerViewNotifications.setLayoutManager(new LinearLayoutManager(this));
        notificationAdapter = new NotificationAdapter(notificationList);
        recyclerViewNotifications.setAdapter(notificationAdapter);

        // Initialize Firebase for notification_items
        notificationItemsRef = FirebaseDatabase.getInstance().getReference("notification_items");

        // Fetch and display notification items
        fetchNotificationItems();

        // Create notification channel (required for Android 8.0+)
        createNotificationChannel();
    }

    private void fetchNotificationItems() {
        notificationItemsRef.addValueEventListener(new ValueEventListener() {
            @Override
            public void onDataChange(DataSnapshot dataSnapshot) {
                notificationList.clear();
                for (DataSnapshot snapshot : dataSnapshot.getChildren()) {
                    String eventName = snapshot.child("eventName").getValue(String.class);
                    String eventDescription = snapshot.child("eventDescription").getValue(String.class);
                    String eventPhotoUrl = snapshot.child("eventPhotoUrl").getValue(String.class);
                    String dateCreated = snapshot.child("dateCreated").getValue(String.class);
                    String venue = snapshot.child("venue").getValue(String.class);
                    String time = snapshot.child("time").getValue(String.class);
                    Boolean isRead = snapshot.child("isRead").getValue(Boolean.class);
                    Long timestamp = snapshot.child("timestamp").getValue(Long.class);
                    String eventId = snapshot.getKey(); // Use the key as eventId

                    if (eventName != null && dateCreated != null && venue != null && time != null && eventId != null && timestamp != null) {
                        // Use default values for optional fields if null
                        String description = eventDescription != null ? eventDescription : "";
                        String photoUrl = eventPhotoUrl != null ? eventPhotoUrl : "";
                        boolean readStatus = isRead != null ? isRead : false;

                        notificationList.add(new NotificationItem(eventName, description, photoUrl, dateCreated, venue, time, readStatus, eventId, timestamp));
                    }
                }
                notificationAdapter.updateNotifications(notificationList);
            }

            @Override
            public void onCancelled(DatabaseError databaseError) {
                Log.e(TAG, "Failed to read notification items: " + databaseError.getMessage());
            }
        });
    }
    private void createNotificationChannel() {
        if (android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.O) {
            CharSequence name = "Event Notifications";
            String description = "Notifications for new events";
            int importance = NotificationManager.IMPORTANCE_DEFAULT;
            NotificationChannel channel = new NotificationChannel(CHANNEL_ID, name, importance);
            channel.setDescription(description);
            NotificationManager notificationManager = getSystemService(NotificationManager.class);
            if (notificationManager != null) {
                notificationManager.createNotificationChannel(channel);
            }
        }
    }
}