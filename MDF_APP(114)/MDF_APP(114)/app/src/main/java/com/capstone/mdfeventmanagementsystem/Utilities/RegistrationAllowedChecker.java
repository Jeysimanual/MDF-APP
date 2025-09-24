package com.capstone.mdfeventmanagementsystem.Utilities;

import android.util.Log;

import com.google.firebase.database.DataSnapshot;
import com.google.firebase.database.DatabaseError;
import com.google.firebase.database.DatabaseReference;
import com.google.firebase.database.FirebaseDatabase;
import com.google.firebase.database.ValueEventListener;

import java.text.SimpleDateFormat;
import java.util.Calendar;
import java.util.Date;
import java.util.Locale;
import java.util.TimeZone;

public class RegistrationAllowedChecker {

    private static final String TAG = "regWorker";

    /**
     * Method that checks the registration status of all events.
     * It will disable registration for expired events.
     */
    public static void checkAndDisableExpiredRegistrations(final RegistrationStatusCallback callback) {
        Log.d(TAG, "checkAndDisableExpiredRegistrations: Checking registration status for events...");

        DatabaseReference eventsRef = FirebaseDatabase.getInstance().getReference("events");

        eventsRef.addListenerForSingleValueEvent(new ValueEventListener() {
            @Override
            public void onDataChange(DataSnapshot snapshot) {
                for (DataSnapshot eventSnapshot : snapshot.getChildren()) {
                    String eventId = eventSnapshot.getKey();
                    String endDate = eventSnapshot.child("endDate").getValue(String.class);
                    String endTime = eventSnapshot.child("endTime").getValue(String.class);
                    Boolean registrationAllowed = eventSnapshot.child("registrationAllowed").getValue(Boolean.class);

                    if (eventId == null || endDate == null || endTime == null || registrationAllowed == null) {
                        continue;
                    }

                    if (Boolean.TRUE.equals(registrationAllowed) && isEventExpired(endDate, endTime)) {
                        eventSnapshot.getRef().child("registrationAllowed").setValue(false)
                                .addOnSuccessListener(aVoid -> Log.d(TAG, "🔒 registrationAllowed disabled for: " + eventId))
                                .addOnFailureListener(e -> Log.e(TAG, "❌ Failed to update event: " + eventId + " | " + e.getMessage()));
                    }
                }

                // Callback once finished
                callback.onCompletion();
            }

            @Override
            public void onCancelled(DatabaseError error) {
                Log.e(TAG, "Database listener cancelled: " + error.getMessage());
                callback.onCompletion();
            }
        });
    }

    /**
     * Helper method to check if the event is expired.
     */
    private static boolean isEventExpired(String endDate, String endTime) {
        try {
            SimpleDateFormat dateTimeFormat = new SimpleDateFormat("yyyy-MM-dd HH:mm", Locale.getDefault());
            dateTimeFormat.setTimeZone(TimeZone.getTimeZone("Asia/Manila"));

            String combinedDateTime = endDate + " " + endTime;
            Date eventEndDateTime = dateTimeFormat.parse(combinedDateTime);

            Calendar now = Calendar.getInstance(TimeZone.getTimeZone("Asia/Manila"));
            Log.d(TAG, "Event end time: " + eventEndDateTime);
            Log.d(TAG, "Current time: " + now.getTime());

            return eventEndDateTime != null && eventEndDateTime.before(now.getTime());

        } catch (Exception e) {
            Log.e("DateParseError", "Failed to parse date/time: " + e.getMessage());
            return false;
        }
    }

    // Callback to indicate completion
    public interface RegistrationStatusCallback {
        void onCompletion();
    }
}
