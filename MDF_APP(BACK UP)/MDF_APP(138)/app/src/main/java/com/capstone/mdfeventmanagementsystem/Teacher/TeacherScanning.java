package com.capstone.mdfeventmanagementsystem.Teacher;

import android.content.ContentValues;
import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.database.Cursor;
import android.database.sqlite.SQLiteDatabase;
import android.database.sqlite.SQLiteOpenHelper;
import android.net.ConnectivityManager;
import android.os.Bundle;
import android.util.Log;
import android.view.View;
import android.widget.Button;
import android.widget.ImageView;
import android.widget.TextView;
import android.widget.Toast;

import androidx.appcompat.app.AppCompatActivity;
import androidx.swiperefreshlayout.widget.SwipeRefreshLayout;

import com.capstone.mdfeventmanagementsystem.R;
import com.capstone.mdfeventmanagementsystem.Utilities.BaseActivity;
import com.google.android.material.bottomappbar.BottomAppBar;
import com.google.android.material.bottomnavigation.BottomNavigationView;
import com.google.firebase.database.DataSnapshot;
import com.google.firebase.database.DatabaseError;
import com.google.firebase.database.DatabaseReference;
import com.google.firebase.database.FirebaseDatabase;
import com.google.firebase.database.ValueEventListener;
import com.journeyapps.barcodescanner.BarcodeCallback;
import com.journeyapps.barcodescanner.BarcodeResult;
import com.journeyapps.barcodescanner.CaptureManager;
import com.journeyapps.barcodescanner.DecoratedBarcodeView;

import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Calendar;
import java.util.Date;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Locale;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.atomic.AtomicInteger;

public class TeacherScanning extends BaseActivity {

    private TextView instructionForScanning, validText, usedText, invalidText, notAllowedText;
    private TextView getStarted;
    private ImageView validTicket, usedTicket, invalidTicket, notAllowedTicket;
    private Button scanTicketBtn, cancelScanBtn;
    private DecoratedBarcodeView barcodeView;
    private CaptureManager captureManager;
    private boolean scanning = false;
    private boolean persistNotAllowedTicket = false;

    private DatabaseReference databaseRef;
    private SharedPreferences sharedPreferences;
    private TicketDatabaseHelper dbHelper;

    // Constants for time status results
    private static final int TIME_STATUS_TOO_EARLY = -1;
    private static final int TIME_STATUS_ON_TIME = 0;
    private static final int TIME_STATUS_LATE = 1;
    private static final int TIME_STATUS_CAN_CHECKOUT = 2;
    private static final int TIME_STATUS_ENDED = 3;
    private static final int TIME_STATUS_CHECKIN_ENDED = 4;
    private static final String TAG = "TeacherScanning";

    // Batch size for syncing
    private static final int BATCH_SIZE = 10;
    // Checkout grace period in minutes (e.g., 60 minutes = 1 hour after event end)
    private static final int CHECKOUT_GRACE_PERIOD_MINUTES = 60;
    // Time window for re-scan check (30 minutes)
    private static final long RECENT_SCAN_WINDOW_MS = 30 * 60 * 1000; // 30 minutes in milliseconds

    // SQLite Database Helper
    private static class TicketDatabaseHelper extends SQLiteOpenHelper {
        private static final String DATABASE_NAME = "StudentEvents.db";
        private static final int DATABASE_VERSION = 3;

        // Events table
        private static final String TABLE_EVENTS = "events";
        private static final String EVENT_COLUMN_ID = "id";
        private static final String EVENT_COLUMN_EVENT_UID = "event_uid";
        private static final String EVENT_COLUMN_NAME = "name";
        private static final String EVENT_COLUMN_START_DATE = "start_date";
        private static final String EVENT_COLUMN_END_DATE = "end_date";
        private static final String EVENT_COLUMN_START_TIME = "start_time";
        private static final String EVENT_COLUMN_END_TIME = "end_time";
        private static final String EVENT_COLUMN_GRACE_TIME = "grace_time";
        private static final String EVENT_COLUMN_EVENT_SPAN = "event_span";
        private static final String EVENT_COLUMN_SYNCED = "synced";

        // Scan records table
        private static final String TABLE_SCAN_RECORDS = "scan_records";
        private static final String COLUMN_ID = "id";
        private static final String COLUMN_STUDENT_ID = "student_id";
        private static final String COLUMN_EVENT_UID = "event_uid";
        private static final String COLUMN_DAY_KEY = "day_key";
        private static final String COLUMN_DATE = "date";
        private static final String COLUMN_CHECK_IN_TIME = "check_in_time";
        private static final String COLUMN_CHECK_OUT_TIME = "check_out_time";
        private static final String COLUMN_ATTENDANCE = "attendance";
        private static final String COLUMN_IS_LATE = "is_late";
        private static final String COLUMN_STATUS = "status";
        private static final String COLUMN_SYNCED = "synced";

        public TicketDatabaseHelper(Context context) {
            super(context, DATABASE_NAME, null, DATABASE_VERSION);
        }

        @Override
        public void onCreate(SQLiteDatabase db) {
            String createEventsTable = "CREATE TABLE IF NOT EXISTS " + TABLE_EVENTS + " (" +
                    EVENT_COLUMN_ID + " INTEGER PRIMARY KEY AUTOINCREMENT, " +
                    EVENT_COLUMN_EVENT_UID + " TEXT, " +
                    EVENT_COLUMN_NAME + " TEXT, " +
                    EVENT_COLUMN_START_DATE + " TEXT, " +
                    EVENT_COLUMN_END_DATE + " TEXT, " +
                    EVENT_COLUMN_START_TIME + " TEXT, " +
                    EVENT_COLUMN_END_TIME + " TEXT, " +
                    EVENT_COLUMN_GRACE_TIME + " TEXT, " +
                    EVENT_COLUMN_EVENT_SPAN + " TEXT, " +
                    EVENT_COLUMN_SYNCED + " INTEGER)";
            db.execSQL(createEventsTable);

            String createScanRecordsTable = "CREATE TABLE IF NOT EXISTS " + TABLE_SCAN_RECORDS + " (" +
                    COLUMN_ID + " INTEGER PRIMARY KEY AUTOINCREMENT, " +
                    COLUMN_STUDENT_ID + " TEXT, " +
                    COLUMN_EVENT_UID + " TEXT, " +
                    COLUMN_DAY_KEY + " TEXT, " +
                    COLUMN_DATE + " TEXT, " +
                    COLUMN_CHECK_IN_TIME + " TEXT, " +
                    COLUMN_CHECK_OUT_TIME + " TEXT, " +
                    COLUMN_ATTENDANCE + " TEXT, " +
                    COLUMN_IS_LATE + " INTEGER, " +
                    COLUMN_STATUS + " TEXT, " +
                    COLUMN_SYNCED + " INTEGER, " +
                    "UNIQUE (" + COLUMN_STUDENT_ID + ", " + COLUMN_EVENT_UID + ", " + COLUMN_DAY_KEY + ", " + COLUMN_DATE + "))";
            db.execSQL(createScanRecordsTable);
        }

        @Override
        public void onUpgrade(SQLiteDatabase db, int oldVersion, int newVersion) {
            if (oldVersion < 2) {
                Cursor cursorEvents = db.rawQuery("SELECT name FROM sqlite_master WHERE type='table' AND name='" + TABLE_EVENTS + "'", null);
                if (!cursorEvents.moveToFirst() || cursorEvents.getCount() == 0) {
                    String createEventsTable = "CREATE TABLE " + TABLE_EVENTS + " (" +
                            EVENT_COLUMN_ID + " INTEGER PRIMARY KEY AUTOINCREMENT, " +
                            EVENT_COLUMN_EVENT_UID + " TEXT, " +
                            EVENT_COLUMN_NAME + " TEXT, " +
                            EVENT_COLUMN_START_DATE + " TEXT, " +
                            EVENT_COLUMN_END_DATE + " TEXT, " +
                            EVENT_COLUMN_START_TIME + " TEXT, " +
                            EVENT_COLUMN_END_TIME + " TEXT, " +
                            EVENT_COLUMN_GRACE_TIME + " TEXT, " +
                            EVENT_COLUMN_EVENT_SPAN + " TEXT, " +
                            EVENT_COLUMN_SYNCED + " INTEGER)";
                    db.execSQL(createEventsTable);
                }
                cursorEvents.close();

                Cursor cursorScanRecords = db.rawQuery("SELECT name FROM sqlite_master WHERE type='table' AND name='" + TABLE_SCAN_RECORDS + "'", null);
                if (!cursorScanRecords.moveToFirst() || cursorScanRecords.getCount() == 0) {
                    String createScanRecordsTable = "CREATE TABLE " + TABLE_SCAN_RECORDS + " (" +
                            COLUMN_ID + " INTEGER PRIMARY KEY AUTOINCREMENT, " +
                            COLUMN_STUDENT_ID + " TEXT, " +
                            COLUMN_EVENT_UID + " TEXT, " +
                            COLUMN_DAY_KEY + " TEXT, " +
                            COLUMN_DATE + " TEXT, " +
                            COLUMN_CHECK_IN_TIME + " TEXT, " +
                            COLUMN_CHECK_OUT_TIME + " TEXT, " +
                            COLUMN_ATTENDANCE + " TEXT, " +
                            COLUMN_IS_LATE + " INTEGER, " +
                            COLUMN_STATUS + " TEXT, " +
                            COLUMN_SYNCED + " INTEGER)";
                    db.execSQL(createScanRecordsTable);
                }
                cursorScanRecords.close();
            }
            if (oldVersion < 3) {
                db.execSQL("DROP TABLE IF EXISTS " + TABLE_SCAN_RECORDS);
                String createScanRecordsTable = "CREATE TABLE " + TABLE_SCAN_RECORDS + " (" +
                        COLUMN_ID + " INTEGER PRIMARY KEY AUTOINCREMENT, " +
                        COLUMN_STUDENT_ID + " TEXT, " +
                        COLUMN_EVENT_UID + " TEXT, " +
                        COLUMN_DAY_KEY + " TEXT, " +
                        COLUMN_DATE + " TEXT, " +
                        COLUMN_CHECK_IN_TIME + " TEXT, " +
                        COLUMN_CHECK_OUT_TIME + " TEXT, " +
                        COLUMN_ATTENDANCE + " TEXT, " +
                        COLUMN_IS_LATE + " INTEGER, " +
                        COLUMN_STATUS + " TEXT, " +
                        COLUMN_SYNCED + " INTEGER, " +
                        "UNIQUE (" + COLUMN_STUDENT_ID + ", " + COLUMN_EVENT_UID + ", " + COLUMN_DAY_KEY + ", " + COLUMN_DATE + "))";
                db.execSQL(createScanRecordsTable);
            }
        }
    }

    private SwipeRefreshLayout swipeRefreshLayout;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_teacher_scanning);

        findViewById(R.id.fab_create).setOnClickListener(v -> {
            startActivity(new Intent(getApplicationContext(), TeacherCreateEventActivity.class));
            overridePendingTransition(0, 0);
        });

        BottomAppBar bottomAppBar = findViewById(R.id.bottomAppBar);

        instructionForScanning = findViewById(R.id.instruction_for_scanning);
        getStarted = findViewById(R.id.getStarted);
        validText = findViewById(R.id.valid_text);
        usedText = findViewById(R.id.used_text);
        invalidText = findViewById(R.id.invalid_text);
        notAllowedText = findViewById(R.id.not_allowed_text);
        validTicket = findViewById(R.id.validTicket);
        usedTicket = findViewById(R.id.usedTicket);
        invalidTicket = findViewById(R.id.invalidTicket);
        notAllowedTicket = findViewById(R.id.notAllowedTicket);
        scanTicketBtn = findViewById(R.id.scanTicketBtn);
        cancelScanBtn = findViewById(R.id.cancelScanBtn);
        barcodeView = findViewById(R.id.barcode_scanner);

        barcodeView.setVisibility(DecoratedBarcodeView.GONE);
        getStarted.setVisibility(TextView.VISIBLE);
        scanTicketBtn.setVisibility(Button.VISIBLE);
        cancelScanBtn.setVisibility(Button.GONE);

        captureManager = new CaptureManager(this, barcodeView);
        captureManager.initializeFromIntent(getIntent(), savedInstanceState);

        scanTicketBtn.setOnClickListener(v -> startScanning());
        cancelScanBtn.setOnClickListener(v -> stopScanning());

        databaseRef = FirebaseDatabase.getInstance().getReference();
        dbHelper = new TicketDatabaseHelper(this);

        initializeDatabase();

        sharedPreferences = getSharedPreferences("TicketStatus", MODE_PRIVATE);

        if (isNetworkAvailable()) {
            syncEventsFromFirebase();
        }

        setupBottomNavigation();

        // Setup SwipeRefreshLayout
        swipeRefreshLayout = findViewById(R.id.swipeRefreshLayout);
        swipeRefreshLayout.setColorSchemeResources(R.color.primary);
        swipeRefreshLayout.setOnRefreshListener(() -> {
            if (isNetworkAvailable()) {
                syncEventsFromFirebase();
                syncScanDataToFirebase();
            } else {
                Toast.makeText(this, "No network available", Toast.LENGTH_SHORT).show();
            }
            swipeRefreshLayout.setRefreshing(false);
        });
    }

    private void initializeDatabase() {
        SQLiteDatabase db = dbHelper.getWritableDatabase();
        db.close();
    }

    private boolean isNetworkAvailable() {
        ConnectivityManager connectivityManager = (ConnectivityManager) getSystemService(Context.CONNECTIVITY_SERVICE);
        return connectivityManager != null && connectivityManager.getActiveNetworkInfo() != null && connectivityManager.getActiveNetworkInfo().isConnected();
    }

    private void syncEventsFromFirebase() {
        databaseRef.child("events").addListenerForSingleValueEvent(new ValueEventListener() {
            @Override
            public void onDataChange(DataSnapshot snapshot) {
                SQLiteDatabase db = dbHelper.getWritableDatabase();
                db.delete(TicketDatabaseHelper.TABLE_EVENTS, null, null);
                for (DataSnapshot eventSnapshot : snapshot.getChildren()) {
                    ContentValues values = new ContentValues();
                    values.put(TicketDatabaseHelper.EVENT_COLUMN_EVENT_UID, eventSnapshot.getKey());
                    values.put(TicketDatabaseHelper.EVENT_COLUMN_NAME, eventSnapshot.child("eventName").getValue(String.class));
                    values.put(TicketDatabaseHelper.EVENT_COLUMN_START_DATE, eventSnapshot.child("startDate").getValue(String.class));
                    values.put(TicketDatabaseHelper.EVENT_COLUMN_END_DATE, eventSnapshot.child("endDate").getValue(String.class));
                    values.put(TicketDatabaseHelper.EVENT_COLUMN_START_TIME, eventSnapshot.child("startTime").getValue(String.class));
                    values.put(TicketDatabaseHelper.EVENT_COLUMN_END_TIME, eventSnapshot.child("endTime").getValue(String.class));
                    values.put(TicketDatabaseHelper.EVENT_COLUMN_GRACE_TIME, eventSnapshot.child("graceTime").getValue(String.class));
                    values.put(TicketDatabaseHelper.EVENT_COLUMN_EVENT_SPAN, eventSnapshot.child("eventSpan").getValue(String.class));
                    values.put(TicketDatabaseHelper.EVENT_COLUMN_SYNCED, 1);
                    db.insert(TicketDatabaseHelper.TABLE_EVENTS, null, values);
                }
                db.close();
            }

            @Override
            public void onCancelled(DatabaseError error) {
                Log.e(TAG, "Failed to sync events: " + error.getMessage());
            }
        });
    }

    private void setupBottomNavigation() {
        BottomNavigationView bottomNavigationView = findViewById(R.id.bottom_navigation_teacher);
        bottomNavigationView.setSelectedItemId(R.id.nav_scan_teacher);
        bottomNavigationView.setBackground(null);

        bottomNavigationView.setOnItemSelectedListener(item -> {
            int itemId = item.getItemId();
            if (itemId == R.id.nav_home_teacher) {
                startActivity(new Intent(this, TeacherDashboard.class));
                finish();
            } else if (itemId == R.id.nav_event_teacher) {
                startActivity(new Intent(this, TeacherEvents.class));
                finish();
            } else if (itemId == R.id.nav_scan_teacher) {
                return true;
            } else if (itemId == R.id.nav_profile_teacher) {
                startActivity(new Intent(this, TeacherProfile.class));
                finish();
            }
            overridePendingTransition(0, 0);
            return true;
        });
    }

    private void startScanning() {
        getStarted.setVisibility(TextView.GONE);
        instructionForScanning.setVisibility(TextView.VISIBLE);
        barcodeView.setVisibility(DecoratedBarcodeView.VISIBLE);
        scanTicketBtn.setVisibility(Button.GONE);
        cancelScanBtn.setVisibility(Button.VISIBLE);

        hideAllTicketViews();

        scanning = true;
        barcodeView.decodeSingle(new BarcodeCallback() {
            @Override
            public void barcodeResult(BarcodeResult result) {
                if (result.getText() != null && scanning) {
                    scanning = false;
                    validateTicket(result.getText());
                    barcodeView.setVisibility(DecoratedBarcodeView.GONE);
                    cancelScanBtn.setVisibility(Button.GONE);
                }
            }
        });
        barcodeView.resume();
    }

    private void validateTicket(String qrContent) {
        if (persistNotAllowedTicket) {
            showNotAllowedTicket();
            return;
        }

        Map<String, String> ticketData = parseQRContent(qrContent);
        String studentId = ticketData.get("studentID");
        String eventId = ticketData.get("eventUID");

        if (studentId == null || eventId == null) {
            showInvalidTicket();
            return;
        }

        // Check if ticket was recently scanned
        if (isTicketRecentlyScanned(studentId, eventId)) {
            showUsedTicket();
            usedText.setText("Ticket already scanned recently. Try again later.");
            scanTicketBtn.setVisibility(Button.VISIBLE);
            return;
        }

        if (isNetworkAvailable()) {
            fetchEventDataFromFirebase(eventId, ticketData, studentId);
        } else {
            validateTicketOffline(eventId, ticketData, studentId);
        }
    }

    private void fetchEventDataFromFirebase(String eventId, Map<String, String> ticketData, String studentId) {
        databaseRef.child("events").child(eventId).addListenerForSingleValueEvent(new ValueEventListener() {
            @Override
            public void onDataChange(DataSnapshot snapshot) {
                if (snapshot.exists()) {
                    ticketData.put("startDate", snapshot.child("startDate").getValue(String.class));
                    ticketData.put("endDate", snapshot.child("endDate").getValue(String.class));
                    ticketData.put("startTime", snapshot.child("startTime").getValue(String.class));
                    ticketData.put("endTime", snapshot.child("endTime").getValue(String.class));
                    ticketData.put("graceTime", snapshot.child("graceTime").getValue(String.class));
                    ticketData.put("eventSpan", snapshot.child("eventSpan").getValue(String.class));
                    boolean isMultiDay = "multi-day".equals(ticketData.get("eventSpan"));

                    saveEventToSQLite(eventId, snapshot);
                    checkAttendanceStatus(studentId, eventId, ticketData, isMultiDay);
                } else {
                    showInvalidTicket();
                }
            }

            @Override
            public void onCancelled(DatabaseError error) {
                Toast.makeText(TeacherScanning.this, "Error checking event: " + error.getMessage(), Toast.LENGTH_SHORT).show();
                showInvalidTicket();
            }
        });
    }

    private void validateTicketOffline(String eventId, Map<String, String> ticketData, String studentId) {
        SQLiteDatabase db = dbHelper.getReadableDatabase();
        Cursor cursor = db.query(TicketDatabaseHelper.TABLE_EVENTS,
                null, TicketDatabaseHelper.EVENT_COLUMN_EVENT_UID + " = ?", new String[]{eventId}, null, null, null);

        if (cursor.moveToFirst()) {
            ticketData.put("startDate", cursor.getString(cursor.getColumnIndexOrThrow(TicketDatabaseHelper.EVENT_COLUMN_START_DATE)));
            ticketData.put("endDate", cursor.getString(cursor.getColumnIndexOrThrow(TicketDatabaseHelper.EVENT_COLUMN_END_DATE)));
            ticketData.put("startTime", cursor.getString(cursor.getColumnIndexOrThrow(TicketDatabaseHelper.EVENT_COLUMN_START_TIME)));
            ticketData.put("endTime", cursor.getString(cursor.getColumnIndexOrThrow(TicketDatabaseHelper.EVENT_COLUMN_END_TIME)));
            ticketData.put("graceTime", cursor.getString(cursor.getColumnIndexOrThrow(TicketDatabaseHelper.EVENT_COLUMN_GRACE_TIME)));
            ticketData.put("eventSpan", cursor.getString(cursor.getColumnIndexOrThrow(TicketDatabaseHelper.EVENT_COLUMN_EVENT_SPAN)));
            boolean isMultiDay = "multi-day".equals(ticketData.get("eventSpan"));
            cursor.close();
            db.close();
            checkAttendanceStatus(studentId, eventId, ticketData, isMultiDay);
        } else {
            cursor.close();
            db.close();
            showNotAllowedTicket();
            notAllowedText.setText("Event data not available offline");
            persistNotAllowedTicket = true;
        }
    }

    private void saveEventToSQLite(String eventId, DataSnapshot snapshot) {
        SQLiteDatabase db = dbHelper.getWritableDatabase();
        ContentValues values = new ContentValues();
        values.put(TicketDatabaseHelper.EVENT_COLUMN_EVENT_UID, eventId);
        values.put(TicketDatabaseHelper.EVENT_COLUMN_NAME, snapshot.child("eventName").getValue(String.class));
        values.put(TicketDatabaseHelper.EVENT_COLUMN_START_DATE, snapshot.child("startDate").getValue(String.class));
        values.put(TicketDatabaseHelper.EVENT_COLUMN_END_DATE, snapshot.child("endDate").getValue(String.class));
        values.put(TicketDatabaseHelper.EVENT_COLUMN_START_TIME, snapshot.child("startTime").getValue(String.class));
        values.put(TicketDatabaseHelper.EVENT_COLUMN_END_TIME, snapshot.child("endTime").getValue(String.class));
        values.put(TicketDatabaseHelper.EVENT_COLUMN_GRACE_TIME, snapshot.child("graceTime").getValue(String.class));
        values.put(TicketDatabaseHelper.EVENT_COLUMN_EVENT_SPAN, snapshot.child("eventSpan").getValue(String.class));
        values.put(TicketDatabaseHelper.EVENT_COLUMN_SYNCED, 1);
        db.replace(TicketDatabaseHelper.TABLE_EVENTS, null, values);
        db.close();
    }

    private void checkAttendanceStatus(String studentId, String eventId, Map<String, String> ticketData, boolean isMultiDay) {
        SQLiteDatabase db = dbHelper.getReadableDatabase();
        String dayKey = getCurrentDayKey(ticketData.get("startDate"), ticketData.get("endDate"), ticketData.get("eventSpan"));
        String currentDate = getCurrentDate();

        Cursor cursor = db.query(TicketDatabaseHelper.TABLE_SCAN_RECORDS,
                null,
                TicketDatabaseHelper.COLUMN_STUDENT_ID + " = ? AND " +
                        TicketDatabaseHelper.COLUMN_EVENT_UID + " = ? AND " +
                        TicketDatabaseHelper.COLUMN_DAY_KEY + " = ? AND " +
                        TicketDatabaseHelper.COLUMN_DATE + " = ?",
                new String[]{studentId, eventId, dayKey, currentDate},
                null, null, null);

        String checkInTime = null;
        String checkOutTimeStr = null;
        boolean hasCheckedIn = false;
        boolean hasCheckedOut = false;
        boolean isLateCheckin = false;

        if (cursor.moveToFirst()) {
            checkInTime = cursor.getString(cursor.getColumnIndexOrThrow(TicketDatabaseHelper.COLUMN_CHECK_IN_TIME));
            hasCheckedIn = checkInTime != null;
            checkOutTimeStr = cursor.getString(cursor.getColumnIndexOrThrow(TicketDatabaseHelper.COLUMN_CHECK_OUT_TIME));
            hasCheckedOut = checkOutTimeStr != null;
            isLateCheckin = cursor.getInt(cursor.getColumnIndexOrThrow(TicketDatabaseHelper.COLUMN_IS_LATE)) == 1;
        }
        cursor.close();
        db.close();

        int timeStatus = checkTimeStatus(ticketData);
        if (!hasCheckedIn && !hasCheckedOut) {
            if (timeStatus == TIME_STATUS_TOO_EARLY) {
                showNotAllowedTicket();
                notAllowedText.setText("The event hasn't started yet");
                persistNotAllowedTicket = true;
            } else if (timeStatus == TIME_STATUS_ENDED || timeStatus == TIME_STATUS_CHECKIN_ENDED) {
                showNotAllowedTicket();
                notAllowedText.setText("The event has ended");
                persistNotAllowedTicket = true;
            } else {
                boolean isLate = (timeStatus == TIME_STATUS_LATE);
                processCheckIn(studentId, eventId, dayKey, currentDate, isLate);
                saveRecentScan(studentId, eventId); // Save scan timestamp
            }
        } else if (hasCheckedIn && !hasCheckedOut) {
            if (timeStatus == TIME_STATUS_ENDED || timeStatus == TIME_STATUS_CHECKIN_ENDED) {
                // Allow checkout within the checkout grace period
                if (isWithinCheckoutGracePeriod(ticketData)) {
                    processCheckOut(studentId, eventId, dayKey, currentDate, checkInTime, isLateCheckin);
                    saveRecentScan(studentId, eventId); // Save scan timestamp
                } else {
                    showNotAllowedTicket();
                    notAllowedText.setText("Event has ended and checkout grace period expired");
                    persistNotAllowedTicket = true;
                }
            } else {
                processCheckOut(studentId, eventId, dayKey, currentDate, checkInTime, isLateCheckin);
                saveRecentScan(studentId, eventId); // Save scan timestamp
            }
        } else if (hasCheckedOut) {
            showUsedTicket();
            usedText.setText("Already checked out for today");
        } else {
            showNotAllowedTicket();
            notAllowedText.setText("No attendance record found for today");
            persistNotAllowedTicket = true;
        }
    }

    private boolean isWithinCheckoutGracePeriod(Map<String, String> ticketData) {
        try {
            String endDate = ticketData.get("endDate");
            String endTime = ticketData.get("endTime");
            if (endDate == null || endTime == null) return false;

            SimpleDateFormat dateFormat = new SimpleDateFormat("yyyy-MM-dd", Locale.US);
            SimpleDateFormat timeFormat = new SimpleDateFormat("HH:mm", Locale.US);
            SimpleDateFormat combinedFormat = new SimpleDateFormat("yyyy-MM-dd HH:mm", Locale.US);

            Date currentDate = new Date();
            String currentDateStr = dateFormat.format(currentDate);
            Date eventEndTime = combinedFormat.parse(endDate + " " + endTime);

            Calendar calendar = Calendar.getInstance();
            calendar.setTime(eventEndTime);
            calendar.add(Calendar.MINUTE, CHECKOUT_GRACE_PERIOD_MINUTES);
            Date graceEndTime = calendar.getTime();

            return currentDate.before(graceEndTime);
        } catch (ParseException e) {
            Log.e(TAG, "Error checking checkout grace period: " + e.getMessage());
            return false;
        }
    }

    private String getCurrentDayKey(String startDate, String endDate, String eventSpan) {
        String currentDate = getCurrentDate();
        if ("multi-day".equals(eventSpan) && startDate != null && endDate != null) {
            try {
                SimpleDateFormat sdf = new SimpleDateFormat("yyyy-MM-dd", Locale.US);
                Date current = sdf.parse(currentDate);
                Date start = sdf.parse(startDate);
                Date end = sdf.parse(endDate);

                if (current.before(start) || current.after(end)) {
                    return "day_1";
                }

                long diff = current.getTime() - start.getTime();
                int dayNumber = (int) (diff / (1000 * 60 * 60 * 24)) + 1;
                return "day_" + dayNumber;
            } catch (ParseException e) {
                Log.e(TAG, "Error parsing dates for day key: " + e.getMessage());
            }
        }
        return "day_1";
    }

    private void processCheckIn(String studentId, String eventId, String dayKey, String date, boolean isLate) {
        String currentTime = getCurrentTime();
        String status = "Ongoing";
        String attendance = "Ongoing";

        saveToLocalDatabase(studentId, eventId, dayKey, date, currentTime, null, attendance, isLate, status);

        if (isNetworkAvailable()) {
            DatabaseReference ticketRef = databaseRef.child("students").child(studentId).child("tickets").child(eventId);
            Map<String, Object> updates = new HashMap<>();
            updates.put("in", currentTime);
            updates.put("status", status);
            updates.put("attendance", attendance);
            updates.put("isLate", isLate);
            ticketRef.child("attendanceDays").child(dayKey).updateChildren(updates)
                    .addOnSuccessListener(aVoid -> {
                        updateSynchronizedStatus(studentId, eventId, dayKey);
                        String msg = isLate ? "Check-in successful. Note: Arrived late!" : "Check-in successful";
                        Toast.makeText(TeacherScanning.this, msg, Toast.LENGTH_SHORT).show();
                    })
                    .addOnFailureListener(e -> {
                        Toast.makeText(TeacherScanning.this, "Check-in saved locally due to error", Toast.LENGTH_SHORT).show();
                        Log.e(TAG, "Failed to sync check-in: " + e.getMessage());
                    });
        } else {
            Toast.makeText(this, isLate ? "Check-in saved locally (Late). Will sync when online." : "Check-in saved locally. Will sync when online.", Toast.LENGTH_SHORT).show();
        }

        showValidTicket();
        validText.setText(isLate ? "Checked in at " + currentTime + " (Arrived late)" : "Checked in at " + currentTime);
    }

    private void processCheckOut(String studentId, String eventId, String dayKey, String date, String existingCheckInTime, boolean isLateCheckin) {
        String currentTime = getCurrentTime();
        String status = "Pending";
        String attendance = isLateCheckin ? "Late" : "Present";

        saveToLocalDatabase(studentId, eventId, dayKey, date, existingCheckInTime, currentTime, attendance, isLateCheckin, status);

        if (isNetworkAvailable()) {
            DatabaseReference ticketRef = databaseRef.child("students").child(studentId).child("tickets").child(eventId);
            Map<String, Object> updates = new HashMap<>();
            updates.put("out", currentTime);
            updates.put("status", status);
            updates.put("attendance", attendance);
            ticketRef.child("attendanceDays").child(dayKey).updateChildren(updates)
                    .addOnSuccessListener(aVoid -> {
                        updateSynchronizedStatus(studentId, eventId, dayKey);
                        String msg = isLateCheckin ? "Check-out successful (Attendance marked as Late)" : "Check-out successful (Attendance marked as Present)";
                        Toast.makeText(TeacherScanning.this, msg, Toast.LENGTH_SHORT).show();
                    })
                    .addOnFailureListener(e -> {
                        Toast.makeText(TeacherScanning.this, "Check-out saved locally due to error", Toast.LENGTH_SHORT).show();
                        Log.e(TAG, "Failed to sync check-out: " + e.getMessage());
                    });
        } else {
            Toast.makeText(this, "Check-out saved locally. Will sync when online.", Toast.LENGTH_SHORT).show();
        }

        showValidTicket();
        validText.setText(isLateCheckin ? "Checked out at " + currentTime + " (Attendance: Late)" : "Checked out at " + currentTime + " (Attendance: Present)");
    }

    private void saveToLocalDatabase(String studentId, String eventId, String dayKey, String date, String checkInTime,
                                     String checkOutTime, String attendance, boolean isLate, String status) {
        SQLiteDatabase db = dbHelper.getWritableDatabase();
        ContentValues values = new ContentValues();
        values.put(TicketDatabaseHelper.COLUMN_STUDENT_ID, studentId);
        values.put(TicketDatabaseHelper.COLUMN_EVENT_UID, eventId);
        values.put(TicketDatabaseHelper.COLUMN_DAY_KEY, dayKey);
        values.put(TicketDatabaseHelper.COLUMN_DATE, date);
        if (checkInTime != null) values.put(TicketDatabaseHelper.COLUMN_CHECK_IN_TIME, checkInTime);
        if (checkOutTime != null) values.put(TicketDatabaseHelper.COLUMN_CHECK_OUT_TIME, checkOutTime);
        values.put(TicketDatabaseHelper.COLUMN_ATTENDANCE, attendance);
        values.put(TicketDatabaseHelper.COLUMN_IS_LATE, isLate ? 1 : 0);
        values.put(TicketDatabaseHelper.COLUMN_STATUS, status);
        values.put(TicketDatabaseHelper.COLUMN_SYNCED, isNetworkAvailable() ? 1 : 0);
        db.replace(TicketDatabaseHelper.TABLE_SCAN_RECORDS, null, values);
        db.close();
    }

    private void updateSynchronizedStatus(String studentId, String eventId, String dayKey) {
        SQLiteDatabase db = dbHelper.getWritableDatabase();
        ContentValues values = new ContentValues();
        values.put(TicketDatabaseHelper.COLUMN_SYNCED, 1);
        db.update(TicketDatabaseHelper.TABLE_SCAN_RECORDS,
                values,
                TicketDatabaseHelper.COLUMN_STUDENT_ID + " = ? AND " +
                        TicketDatabaseHelper.COLUMN_EVENT_UID + " = ? AND " +
                        TicketDatabaseHelper.COLUMN_DAY_KEY + " = ?",
                new String[]{studentId, eventId, dayKey});
        db.close();
    }

    private void stopScanning() {
        scanning = false;
        barcodeView.pauseAndWait();
        barcodeView.setVisibility(DecoratedBarcodeView.GONE);
        cancelScanBtn.setVisibility(Button.GONE);

        if (persistNotAllowedTicket) {
            scanTicketBtn.setVisibility(Button.VISIBLE);
        } else {
            resetScanUI();
        }
    }

    private void resetScanUI() {
        if (persistNotAllowedTicket) {
            getStarted.setVisibility(TextView.GONE);
            instructionForScanning.setVisibility(TextView.GONE);
            validTicket.setVisibility(ImageView.GONE);
            validText.setVisibility(TextView.GONE);
            usedTicket.setVisibility(ImageView.GONE);
            usedText.setVisibility(TextView.GONE);
            invalidTicket.setVisibility(ImageView.GONE);
            invalidText.setVisibility(TextView.GONE);
            notAllowedTicket.setVisibility(ImageView.VISIBLE);
            notAllowedText.setVisibility(TextView.VISIBLE);
            scanTicketBtn.setVisibility(Button.VISIBLE);
        } else {
            hideAllTicketViews();
            getStarted.setVisibility(TextView.VISIBLE);
            instructionForScanning.setVisibility(TextView.GONE);
            scanTicketBtn.setVisibility(Button.VISIBLE);
        }
    }

    private String getCurrentTime() {
        SimpleDateFormat sdf = new SimpleDateFormat("hh:mm a", Locale.getDefault());
        return sdf.format(new Date());
    }

    private String getCurrentDate() {
        SimpleDateFormat sdf = new SimpleDateFormat("yyyy-MM-dd", Locale.getDefault());
        return sdf.format(new Date());
    }

    private void saveTicketStatus(String eventId, String status) {
        SharedPreferences.Editor editor = sharedPreferences.edit();
        editor.putString(eventId, status);
        editor.apply();
    }

    private String getTicketStatus(String eventId) {
        return sharedPreferences.getString(eventId, null);
    }

    private Map<String, String> parseQRContent(String qrContent) {
        Map<String, String> map = new HashMap<>();
        try {
            qrContent = qrContent.replaceAll("[{}]", "");
            String[] pairs = qrContent.split(", ");
            for (String pair : pairs) {
                String[] entry = pair.split("=");
                if (entry.length == 2) {
                    map.put(entry[0].trim(), entry[1].trim());
                }
            }
        } catch (Exception e) {
            Toast.makeText(this, "Invalid QR code format", Toast.LENGTH_SHORT).show();
        }
        return map;
    }

    private int checkTimeStatus(Map<String, String> ticketData) {
        try {
            String startDate = ticketData.get("startDate");
            String endDate = ticketData.get("endDate");
            String startTime = ticketData.get("startTime");
            String endTime = ticketData.get("endTime");
            String graceTimeStr = ticketData.get("graceTime");

            if (startDate == null || startTime == null || endTime == null) {
                return TIME_STATUS_ENDED;
            }

            SimpleDateFormat dateFormat = new SimpleDateFormat("yyyy-MM-dd", Locale.US);
            SimpleDateFormat timeFormat = new SimpleDateFormat("HH:mm", Locale.US);
            SimpleDateFormat combinedFormat = new SimpleDateFormat("yyyy-MM-dd HH:mm", Locale.US);

            Date currentDate = new Date();
            String currentDateStr = dateFormat.format(currentDate);
            Date currentDateOnly = dateFormat.parse(currentDateStr);

            Date eventStartDate = dateFormat.parse(startDate);

            if (endDate != null && !endDate.isEmpty()) {
                Date eventEndDate = dateFormat.parse(endDate);
                if (currentDateOnly.before(eventStartDate)) {
                    return TIME_STATUS_TOO_EARLY;
                }
                if (currentDateOnly.after(eventEndDate)) {
                    return TIME_STATUS_ENDED;
                }
            } else {
                if (!currentDateStr.equals(startDate)) {
                    if (currentDateOnly.before(eventStartDate)) {
                        return TIME_STATUS_TOO_EARLY;
                    } else {
                        return TIME_STATUS_ENDED;
                    }
                }
            }

            Date eventStartTime = combinedFormat.parse(currentDateStr + " " + startTime);
            Date eventEndTime = combinedFormat.parse(currentDateStr + " " + endTime);

            if (currentDate.before(eventStartTime)) {
                return TIME_STATUS_TOO_EARLY;
            }

            if (currentDate.after(eventEndTime)) {
                // Check if within checkout grace period
                Calendar calendar = Calendar.getInstance();
                calendar.setTime(eventEndTime);
                calendar.add(Calendar.MINUTE, CHECKOUT_GRACE_PERIOD_MINUTES);
                Date graceEndTime = calendar.getTime();
                if (currentDate.before(graceEndTime)) {
                    return TIME_STATUS_CAN_CHECKOUT; // Allow checkout within grace period
                }
                return TIME_STATUS_CHECKIN_ENDED;
            }

            if (graceTimeStr == null || graceTimeStr.isEmpty() || "none".equalsIgnoreCase(graceTimeStr)) {
                return TIME_STATUS_ON_TIME;
            }

            int graceTime = Integer.parseInt(graceTimeStr);
            Calendar calendar = Calendar.getInstance();
            calendar.setTime(eventStartTime);
            calendar.add(Calendar.MINUTE, graceTime);
            Date graceEndTime = calendar.getTime();

            if (currentDate.after(graceEndTime)) {
                return TIME_STATUS_LATE;
            } else {
                return TIME_STATUS_ON_TIME;
            }
        } catch (ParseException e) {
            return TIME_STATUS_ENDED;
        }
    }

    private void showValidTicket() {
        hideAllTicketViews();
        validTicket.setVisibility(ImageView.VISIBLE);
        validText.setVisibility(TextView.VISIBLE);
        scanTicketBtn.setVisibility(Button.VISIBLE);
        persistNotAllowedTicket = false;
    }

    private void showUsedTicket() {
        hideAllTicketViews();
        usedTicket.setVisibility(ImageView.VISIBLE);
        usedText.setVisibility(TextView.VISIBLE);
        scanTicketBtn.setVisibility(Button.VISIBLE);
        persistNotAllowedTicket = false;
    }

    private void showInvalidTicket() {
        hideAllTicketViews();
        invalidTicket.setVisibility(ImageView.VISIBLE);
        invalidText.setVisibility(TextView.VISIBLE);
        scanTicketBtn.setVisibility(Button.VISIBLE);
        persistNotAllowedTicket = false;
    }

    private void showNotAllowedTicket() {
        hideAllTicketViews();
        notAllowedTicket.setVisibility(ImageView.VISIBLE);
        notAllowedText.setVisibility(TextView.VISIBLE);
        scanTicketBtn.setVisibility(Button.VISIBLE);
    }

    private void hideAllTicketViews() {
        getStarted.setVisibility(TextView.GONE);
        instructionForScanning.setVisibility(TextView.GONE);
        validTicket.setVisibility(ImageView.GONE);
        validText.setVisibility(TextView.GONE);
        usedTicket.setVisibility(ImageView.GONE);
        usedText.setVisibility(TextView.GONE);
        invalidTicket.setVisibility(ImageView.GONE);
        invalidText.setVisibility(TextView.GONE);
        notAllowedTicket.setVisibility(ImageView.GONE);
        notAllowedText.setVisibility(TextView.GONE);
    }

    private boolean isTicketRecentlyScanned(String studentId, String eventId) {
        String key = studentId + "_" + eventId;
        long lastScanTime = sharedPreferences.getLong("last_scan_" + key, 0);
        long currentTime = System.currentTimeMillis();

        if (lastScanTime == 0) {
            return false; // No previous scan recorded
        }

        // Check if the last scan was within the 30-minute window
        return (currentTime - lastScanTime) < RECENT_SCAN_WINDOW_MS;
    }

    private void saveRecentScan(String studentId, String eventId) {
        String key = studentId + "_" + eventId;
        SharedPreferences.Editor editor = sharedPreferences.edit();
        editor.putLong("last_scan_" + key, System.currentTimeMillis());
        editor.apply();
    }

    public void resetPersistentState() {
        persistNotAllowedTicket = false;
        resetScanUI();
    }

    @Override
    protected void onResume() {
        super.onResume();
        captureManager.onResume();
        if (isNetworkAvailable()) {
            syncScanDataToFirebase();
        }
    }

    @Override
    protected void onPause() {
        super.onPause();
        captureManager.onPause();
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
        captureManager.onDestroy();
        dbHelper.close();
    }

    @Override
    protected void onSaveInstanceState(Bundle outState) {
        super.onSaveInstanceState(outState);
        captureManager.onSaveInstanceState(outState);
    }

    private void syncScanDataToFirebase() {
        SQLiteDatabase db = dbHelper.getReadableDatabase();
        Cursor cursor = null;
        ExecutorService executor = Executors.newFixedThreadPool(3); // Adjust thread pool size as needed
        List<Map<String, Object>> batch = new ArrayList<>();
        AtomicInteger pendingTasks = new AtomicInteger(0);
        AtomicInteger successfulSyncs = new AtomicInteger(0);
        AtomicInteger failedSyncs = new AtomicInteger(0);

        try {
            cursor = db.query(TicketDatabaseHelper.TABLE_SCAN_RECORDS,
                    null,
                    TicketDatabaseHelper.COLUMN_SYNCED + " = ?",
                    new String[]{"0"},
                    null, null, null);

            int recordCount = cursor.getCount();
            if (recordCount == 0) {
                runOnUiThread(() -> Toast.makeText(this, "No records to sync", Toast.LENGTH_SHORT).show());
                return;
            }

            runOnUiThread(() -> Toast.makeText(this, "Starting sync of " + recordCount + " records...", Toast.LENGTH_SHORT).show());

            while (cursor.moveToNext()) {
                Map<String, Object> record = new HashMap<>();
                record.put("studentId", cursor.getString(cursor.getColumnIndexOrThrow(TicketDatabaseHelper.COLUMN_STUDENT_ID)));
                record.put("eventId", cursor.getString(cursor.getColumnIndexOrThrow(TicketDatabaseHelper.COLUMN_EVENT_UID)));
                record.put("dayKey", cursor.getString(cursor.getColumnIndexOrThrow(TicketDatabaseHelper.COLUMN_DAY_KEY)));
                record.put("date", cursor.getString(cursor.getColumnIndexOrThrow(TicketDatabaseHelper.COLUMN_DATE)));
                record.put("checkInTime", cursor.getString(cursor.getColumnIndexOrThrow(TicketDatabaseHelper.COLUMN_CHECK_IN_TIME)));
                record.put("checkOutTime", cursor.getString(cursor.getColumnIndexOrThrow(TicketDatabaseHelper.COLUMN_CHECK_OUT_TIME)));
                record.put("attendance", cursor.getString(cursor.getColumnIndexOrThrow(TicketDatabaseHelper.COLUMN_ATTENDANCE)));
                record.put("isLate", cursor.getInt(cursor.getColumnIndexOrThrow(TicketDatabaseHelper.COLUMN_IS_LATE)) == 1);
                record.put("status", cursor.getString(cursor.getColumnIndexOrThrow(TicketDatabaseHelper.COLUMN_STATUS)));
                batch.add(record);

                if (batch.size() >= BATCH_SIZE || cursor.isLast()) {
                    pendingTasks.incrementAndGet();
                    List<Map<String, Object>> batchToSync = new ArrayList<>(batch);
                    executor.submit(() -> syncBatch(batchToSync, successfulSyncs, failedSyncs));
                    batch.clear();
                }
            }

            executor.shutdown();
            new Thread(() -> {
                try {
                    executor.awaitTermination(30, java.util.concurrent.TimeUnit.SECONDS);
                    runOnUiThread(() -> {
                        String message = "Sync completed: " + successfulSyncs.get() + " records synced, " + failedSyncs.get() + " failed.";
                        Toast.makeText(TeacherScanning.this, message, Toast.LENGTH_LONG).show();
                        Log.d(TAG, message);
                    });
                } catch (InterruptedException e) {
                    Log.e(TAG, "Sync interrupted: " + e.getMessage());
                    runOnUiThread(() -> Toast.makeText(TeacherScanning.this, "Sync interrupted", Toast.LENGTH_SHORT).show());
                }
            }).start();

        } catch (Exception e) {
            Log.e(TAG, "Database query failed: " + e.getMessage());
            runOnUiThread(() -> Toast.makeText(this, "Error syncing data: " + e.getMessage(), Toast.LENGTH_SHORT).show());
        } finally {
            if (cursor != null) {
                cursor.close();
            }
            db.close();
        }
    }

    private void syncBatch(List<Map<String, Object>> batch, AtomicInteger successfulSyncs, AtomicInteger failedSyncs) {
        for (Map<String, Object> record : batch) {
            String studentId = (String) record.get("studentId");
            String eventId = (String) record.get("eventId");
            String dayKey = (String) record.get("dayKey");
            String date = (String) record.get("date");
            String checkInTime = (String) record.get("checkInTime");
            String checkOutTime = (String) record.get("checkOutTime");
            String attendance = (String) record.get("attendance");
            Boolean isLate = (Boolean) record.get("isLate");
            String status = (String) record.get("status");

            DatabaseReference ticketRef = databaseRef.child("students").child(studentId).child("tickets").child(eventId);
            Map<String, Object> updates = new HashMap<>();
            if (date != null) updates.put("date", date);
            if (checkInTime != null) updates.put("in", checkInTime);
            if (checkOutTime != null) updates.put("out", checkOutTime);
            if (attendance != null) updates.put("attendance", attendance);
            updates.put("isLate", isLate);
            updates.put("status", status);

            ticketRef.child("attendanceDays").child(dayKey).updateChildren(updates)
                    .addOnSuccessListener(aVoid -> {
                        updateSynchronizedStatus(studentId, eventId, dayKey);
                        successfulSyncs.incrementAndGet();
                        Log.d(TAG, "Synced scan data for student " + studentId + ", event " + eventId + ", day " + dayKey);
                    })
                    .addOnFailureListener(e -> {
                        failedSyncs.incrementAndGet();
                        Log.e(TAG, "Failed to sync scan data for student " + studentId + ", event " + eventId + ": " + e.getMessage());
                    });
        }
    }
}