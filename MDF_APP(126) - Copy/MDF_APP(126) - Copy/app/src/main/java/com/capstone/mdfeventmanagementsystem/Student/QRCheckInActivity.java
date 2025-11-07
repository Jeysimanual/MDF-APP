package com.capstone.mdfeventmanagementsystem.Student;

import android.content.ContentValues;
import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.content.res.ColorStateList;
import android.database.Cursor;
import android.database.sqlite.SQLiteDatabase;
import android.database.sqlite.SQLiteOpenHelper;
import android.graphics.Bitmap;
import android.graphics.Color;
import android.net.ConnectivityManager;
import android.os.Bundle;
import android.util.Log;
import android.view.View;
import android.widget.Button;
import android.widget.ImageView;
import android.widget.TextView;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;

import com.capstone.mdfeventmanagementsystem.R;
import com.capstone.mdfeventmanagementsystem.Utilities.BaseActivity;
import com.google.android.material.bottomappbar.BottomAppBar;
import com.google.android.material.bottomnavigation.BottomNavigationView;
import com.google.android.material.floatingactionbutton.FloatingActionButton;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseUser;
import com.google.firebase.database.*;
import com.journeyapps.barcodescanner.BarcodeCallback;
import com.journeyapps.barcodescanner.BarcodeResult;
import com.journeyapps.barcodescanner.CaptureManager;
import com.journeyapps.barcodescanner.DecoratedBarcodeView;
import com.squareup.picasso.Picasso;
import com.squareup.picasso.Transformation;

import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.*;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.atomic.AtomicInteger;

class CircleTransformQRCheckIn implements Transformation {
    @Override
    public Bitmap transform(Bitmap source) {
        int size = Math.min(source.getWidth(), source.getHeight());
        int x = (source.getWidth() - size) / 2;
        int y = (source.getHeight() - size) / 2;

        Bitmap squaredBitmap = Bitmap.createBitmap(source, x, y, size, size);
        if (squaredBitmap != source) {
            source.recycle();
        }

        Bitmap bitmap = Bitmap.createBitmap(size, size, source.getConfig());
        android.graphics.Canvas canvas = new android.graphics.Canvas(bitmap);
        android.graphics.Paint paint = new android.graphics.Paint();
        android.graphics.BitmapShader shader = new android.graphics.BitmapShader(
                squaredBitmap, android.graphics.Shader.TileMode.CLAMP,
                android.graphics.Shader.TileMode.CLAMP);

        paint.setShader(shader);
        paint.setAntiAlias(true);

        float r = size / 2f;
        canvas.drawCircle(r, r, r, paint);

        squaredBitmap.recycle();
        return bitmap;
    }

    @Override
    public String key() {
        return "circle";
    }
}

public class QRCheckInActivity extends BaseActivity {

    private TextView instructionForScanning, validText, usedText, invalidText, notAllowedText;
    private TextView getStarted, noPermissionText, noScanPermissionText;
    private ImageView validTicket, usedTicket, invalidTicket, notAllowedTicket;
    private Button scanTicketBtn, cancelScanBtn;
    private DecoratedBarcodeView barcodeView;
    private CaptureManager captureManager;
    private boolean scanning = false;
    private boolean persistNotAllowedTicket = false;

    private DatabaseReference databaseRef;
    private SharedPreferences sharedPreferences;
    private TicketDatabaseHelper dbHelper;

    private static final int TIME_STATUS_TOO_EARLY = -1;
    private static final int TIME_STATUS_ON_TIME = 0;
    private static final int TIME_STATUS_LATE = 1;
    private static final int TIME_STATUS_CAN_CHECKOUT = 2;
    private static final int TIME_STATUS_ENDED = 3;
    private static final int TIME_STATUS_CHECKIN_ENDED = 4;
    private static final String TAG = "QRCheckInActivity";
    private static final int BATCH_SIZE = 10;

    private static final long RECENT_SCAN_WINDOW_MS = 30 * 60 * 1000;
    private static final String PREF_LAST_SCAN = "last_scan_";

    private ImageView profileImageView;
    private DatabaseReference studentsRef;
    private DatabaseReference profilesRef;

    private static class TicketDatabaseHelper extends SQLiteOpenHelper {
        private static final String DATABASE_NAME = "StudentEvents.db";
        private static final int DATABASE_VERSION = 2;

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
                    "UNIQUE(" + COLUMN_STUDENT_ID + ", " + COLUMN_EVENT_UID + ", " + COLUMN_DAY_KEY + ", " + COLUMN_DATE + ") ON CONFLICT REPLACE)";
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
            }if (oldVersion < 3) {
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
                        "UNIQUE(" + COLUMN_STUDENT_ID + ", " + COLUMN_EVENT_UID + ", " + COLUMN_DAY_KEY + ", " + COLUMN_DATE + ") ON CONFLICT REPLACE)";
                db.execSQL(createScanRecordsTable);
            }
        }
    }

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(com.capstone.mdfeventmanagementsystem.R.layout.activity_qrcheck_in);

        profileImageView = findViewById(R.id.profile_image);
        studentsRef = FirebaseDatabase.getInstance().getReference().child("students");
        profilesRef = FirebaseDatabase.getInstance().getReference().child("user_profiles");

        loadCachedProfileImage();

        findViewById(R.id.fab_scan).setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                startActivity(new Intent(getApplicationContext(), QRCheckInActivity.class));
                overridePendingTransition(0, 0);
            }
        });

        BottomAppBar bottomAppBar = findViewById(R.id.bottomAppBar);
        bottomAppBar.setBackgroundTint(ColorStateList.valueOf(Color.WHITE));

        FloatingActionButton fab = findViewById(R.id.fab_scan);
        fab.setColorFilter(getResources().getColor(R.color.green));

        findViewById(R.id.profile_image).setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                Intent intent = new Intent(QRCheckInActivity.this, ProfileActivity.class);
                startActivity(intent);
            }
        });

        instructionForScanning = findViewById(com.capstone.mdfeventmanagementsystem.R.id.instruction_for_scanning);
        getStarted = findViewById(com.capstone.mdfeventmanagementsystem.R.id.getStarted);
        validText = findViewById(com.capstone.mdfeventmanagementsystem.R.id.valid_text);
        usedText = findViewById(com.capstone.mdfeventmanagementsystem.R.id.used_text);
        invalidText = findViewById(com.capstone.mdfeventmanagementsystem.R.id.invalid_text);
        notAllowedText = findViewById(com.capstone.mdfeventmanagementsystem.R.id.not_allowed_text);
        validTicket = findViewById(com.capstone.mdfeventmanagementsystem.R.id.validTicket);
        usedTicket = findViewById(com.capstone.mdfeventmanagementsystem.R.id.usedTicket);
        invalidTicket = findViewById(com.capstone.mdfeventmanagementsystem.R.id.invalidTicket);
        notAllowedTicket = findViewById(com.capstone.mdfeventmanagementsystem.R.id.notAllowedTicket);
        scanTicketBtn = findViewById(com.capstone.mdfeventmanagementsystem.R.id.scanTicketBtn);
        cancelScanBtn = findViewById(com.capstone.mdfeventmanagementsystem.R.id.cancelScanBtn);
        barcodeView = findViewById(com.capstone.mdfeventmanagementsystem.R.id.barcode_scanner);
        noPermissionText = findViewById(com.capstone.mdfeventmanagementsystem.R.id.noPermission);
        noScanPermissionText = findViewById(com.capstone.mdfeventmanagementsystem.R.id.noScanPermission); // Initialize new TextView

        // Initialize UI to a default state
        barcodeView.setVisibility(DecoratedBarcodeView.GONE);
        getStarted.setVisibility(TextView.GONE);
        noPermissionText.setVisibility(TextView.GONE);
        noScanPermissionText.setVisibility(TextView.GONE);
        scanTicketBtn.setVisibility(Button.GONE);
        cancelScanBtn.setVisibility(Button.GONE);

        captureManager = new CaptureManager(this, barcodeView);
        captureManager.initializeFromIntent(getIntent(), savedInstanceState);

        scanTicketBtn.setOnClickListener(v -> startScanning());
        cancelScanBtn.setOnClickListener(v -> stopScanning());

        databaseRef = FirebaseDatabase.getInstance().getReference();
        databaseRef.keepSynced(true);
        dbHelper = new TicketDatabaseHelper(this);
        initializeDatabase();

        sharedPreferences = getSharedPreferences("TicketStatus", MODE_PRIVATE);

        // Check coordinator permissions on activity creation
        FirebaseUser user = FirebaseAuth.getInstance().getCurrentUser();
        if (user != null) {
            String userEmail = user.getEmail();
            checkIfUserIsCoordinator(userEmail, (isCoordinator, eventUID) -> {
                if (isCoordinator && eventUID != null) {
                    // User is a coordinator with scan permission
                    noPermissionText.setVisibility(TextView.GONE);
                    noScanPermissionText.setVisibility(TextView.GONE);
                    getStarted.setVisibility(TextView.VISIBLE);
                    scanTicketBtn.setVisibility(Button.VISIBLE);
                    barcodeView.setVisibility(DecoratedBarcodeView.GONE);
                } else {
                    // Check if user is a coordinator without scan permission
                    checkIfUserIsCoordinatorButNotAllowed((isCoordinatorNoScan, eventUID2) -> {
                        if (isCoordinatorNoScan) {
                            // User is a coordinator but scanPermission is false or null
                            noPermissionText.setVisibility(TextView.GONE);
                            noScanPermissionText.setVisibility(TextView.VISIBLE);
                            getStarted.setVisibility(TextView.GONE);
                            instructionForScanning.setVisibility(TextView.GONE);
                            scanTicketBtn.setVisibility(Button.GONE);
                            cancelScanBtn.setVisibility(Button.GONE);
                            barcodeView.setVisibility(DecoratedBarcodeView.GONE);
                        } else {
                            // User is not a coordinator
                            noPermissionText.setVisibility(TextView.VISIBLE);
                            noScanPermissionText.setVisibility(TextView.GONE);
                            getStarted.setVisibility(TextView.GONE);
                            instructionForScanning.setVisibility(TextView.GONE);
                            scanTicketBtn.setVisibility(Button.GONE);
                            cancelScanBtn.setVisibility(Button.GONE);
                            barcodeView.setVisibility(DecoratedBarcodeView.GONE);
                        }
                    });
                }
            });
        } else {
            Log.e(TAG, "User not logged in.");
            noPermissionText.setVisibility(TextView.VISIBLE);
            noScanPermissionText.setVisibility(TextView.GONE);
            getStarted.setVisibility(TextView.GONE);
            instructionForScanning.setVisibility(TextView.GONE);
            scanTicketBtn.setVisibility(Button.GONE);
            cancelScanBtn.setVisibility(Button.GONE);
            barcodeView.setVisibility(DecoratedBarcodeView.GONE);
        }

        if (isNetworkAvailable()) {
            syncEventsFromFirebase();
        }

        setupBottomNavigation();
        loadUserProfile();
    }

    private boolean isTicketRecentlyScanned(String studentId, String eventId) {
        String key = studentId + "_" + eventId;
        long lastScanTime = sharedPreferences.getLong(PREF_LAST_SCAN + key, 0);
        long currentTime = System.currentTimeMillis();
        return (currentTime - lastScanTime) < RECENT_SCAN_WINDOW_MS;
    }

    private void saveRecentScan(String studentId, String eventId) {
        String key = studentId + "_" + eventId;
        SharedPreferences.Editor editor = sharedPreferences.edit();
        editor.putLong(PREF_LAST_SCAN + key, System.currentTimeMillis());
        editor.apply();
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

    private void syncScanDataToFirebase() {
        SQLiteDatabase db = dbHelper.getReadableDatabase();
        Cursor cursor = null;
        ExecutorService executor = Executors.newFixedThreadPool(3);
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
                        Toast.makeText(QRCheckInActivity.this, message, Toast.LENGTH_LONG).show();
                        Log.d(TAG, message);
                    });
                } catch (InterruptedException e) {
                    Log.e(TAG, "Sync interrupted: " + e.getMessage());
                    runOnUiThread(() -> Toast.makeText(QRCheckInActivity.this, "Sync interrupted", Toast.LENGTH_SHORT).show());
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
        if (attendance != null) values.put(TicketDatabaseHelper.COLUMN_ATTENDANCE, attendance);
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

    public interface OnCoordinatorCheckListener {
        void onCheck(boolean isCoordinator, String eventUID);
    }

    private void startScanning() {
        FirebaseUser user = FirebaseAuth.getInstance().getCurrentUser();
        if (user == null) {
            Log.e("coordinatorTesting", "User not logged in.");
            Toast.makeText(this, "User not logged in.", Toast.LENGTH_SHORT).show();
            noPermissionText.setVisibility(TextView.VISIBLE);
            noScanPermissionText.setVisibility(TextView.GONE);
            getStarted.setVisibility(TextView.GONE);
            instructionForScanning.setVisibility(TextView.GONE);
            barcodeView.setVisibility(DecoratedBarcodeView.GONE);
            scanTicketBtn.setVisibility(Button.GONE);
            cancelScanBtn.setVisibility(Button.GONE);
            return;
        }

        String userEmail = user.getEmail();
        Log.d("coordinatorTesting", "Checking if user is a coordinator: " + userEmail);
        checkIfUserIsCoordinator(userEmail, (isCoordinator, eventUID) -> {
            if (isCoordinator && eventUID != null) {
                Log.d("coordinatorTesting", "User is a coordinator for event: " + eventUID + ". Starting scanning process.");

                noPermissionText.setVisibility(TextView.GONE);
                noScanPermissionText.setVisibility(TextView.GONE);
                getStarted.setVisibility(TextView.GONE);
                instructionForScanning.setVisibility(TextView.VISIBLE);
                barcodeView.setVisibility(DecoratedBarcodeView.VISIBLE);
                cancelScanBtn.setVisibility(Button.VISIBLE);
                scanTicketBtn.setVisibility(Button.GONE);

                hideAllTicketViews();
                scanning = true;

                barcodeView.decodeSingle(new BarcodeCallback() {
                    @Override
                    public void barcodeResult(BarcodeResult result) {
                        if (result.getText() != null && scanning) {
                            Log.d("coordinatorTesting", "Scanned ticket: " + result.getText());
                            scanning = false;
                            validateTicket(result.getText(), eventUID);
                            barcodeView.setVisibility(DecoratedBarcodeView.GONE);
                            cancelScanBtn.setVisibility(Button.GONE);
                        }
                    }
                });
                barcodeView.resume();
            } else {
                Log.w("coordinatorTesting", "Checking if user is a coordinator without scan permission.");
                checkIfUserIsCoordinatorButNotAllowed((isCoordinatorNoScan, eventUID2) -> {
                    if (isCoordinatorNoScan) {
                        Log.w("coordinatorTesting", "User is a coordinator for event: " + eventUID2 + " but not allowed to scan.");
                        Toast.makeText(this, "You are a coordinator but not allowed to scan yet.", Toast.LENGTH_SHORT).show();
                        noPermissionText.setVisibility(TextView.GONE);
                        noScanPermissionText.setVisibility(TextView.VISIBLE);
                        getStarted.setVisibility(TextView.GONE);
                        instructionForScanning.setVisibility(TextView.GONE);
                        barcodeView.setVisibility(DecoratedBarcodeView.GONE);
                        scanTicketBtn.setVisibility(Button.GONE);
                        cancelScanBtn.setVisibility(Button.GONE);
                    } else {
                        Log.w("coordinatorTesting", "User is not a coordinator. Access denied.");
                        Toast.makeText(this, "You do not have permission to scan tickets.", Toast.LENGTH_SHORT).show();
                        noPermissionText.setVisibility(TextView.VISIBLE);
                        noScanPermissionText.setVisibility(TextView.GONE);
                        getStarted.setVisibility(TextView.GONE);
                        instructionForScanning.setVisibility(TextView.GONE);
                        barcodeView.setVisibility(DecoratedBarcodeView.GONE);
                        scanTicketBtn.setVisibility(Button.GONE);
                        cancelScanBtn.setVisibility(Button.GONE);
                    }
                });
            }
        });
    }

    private void checkIfUserIsCoordinator(String email, OnCoordinatorCheckListener listener) {
        FirebaseUser user = FirebaseAuth.getInstance().getCurrentUser();
        if (user == null || email == null) {
            Log.e("coordinatorTesting", "User not logged in or email is null. Cannot check permissions.");
            listener.onCheck(false, null);
            return;
        }

        // Retrieve studentID from SharedPreferences
        SharedPreferences sharedPreferences = getSharedPreferences("UserSession", MODE_PRIVATE);
        String studentId = sharedPreferences.getString("studentID", null);
        if (studentId == null || studentId.isEmpty()) {
            Log.e("coordinatorTesting", "No studentID found in SharedPreferences for user: " + user.getUid());
            listener.onCheck(false, null);
            return;
        }

        DatabaseReference studentRef = FirebaseDatabase.getInstance().getReference("students").child(studentId);
        Log.d("coordinatorTesting", "Checking student data for studentID: " + studentId);

        // Verify student exists in the students node
        studentRef.get().addOnCompleteListener(studentTask -> {
            if (studentTask.isSuccessful() && studentTask.getResult().exists()) {
                Log.d("coordinatorTesting", "Student data found for studentID: " + studentId);

                DatabaseReference eventsRef = FirebaseDatabase.getInstance().getReference("events");
                Log.d("coordinatorTesting", "Fetching events from Firebase to check eventCoordinators...");
                eventsRef.get().addOnCompleteListener(task -> {
                    if (task.isSuccessful()) {
                        Log.d("coordinatorTesting", "Successfully fetched events.");
                        if (task.getResult().exists()) {
                            Log.d("coordinatorTesting", "Events found. Checking if studentID " + studentId + " is in eventCoordinators...");
                            for (DataSnapshot eventSnapshot : task.getResult().getChildren()) {
                                String eventID = eventSnapshot.getKey();
                                DataSnapshot coordinatorsSnapshot = eventSnapshot.child("eventCoordinators");
                                Log.d("coordinatorTesting", "Checking eventCoordinators for event ID: " + eventID);

                                StringBuilder coordinatorsList = new StringBuilder();
                                coordinatorsList.append("[");
                                boolean first = true;
                                for (DataSnapshot coordinator : coordinatorsSnapshot.getChildren()) {
                                    if (!first) {
                                        coordinatorsList.append(", ");
                                    }
                                    coordinatorsList.append(coordinator.getKey());
                                    first = false;
                                }
                                coordinatorsList.append("]");
                                Log.d("coordinatorTesting", "Event " + eventID + " coordinators: " + coordinatorsList.toString());

                                Boolean scanPermission = eventSnapshot.child("scanPermission").getValue(Boolean.class);
                                Log.d("coordinatorTesting", "Event " + eventID + " scanPermission: " + (scanPermission != null ? scanPermission : "null"));

                                if (coordinatorsSnapshot.hasChild(studentId)) {
                                    if (scanPermission != null && scanPermission) {
                                        String eventUID = eventSnapshot.getKey();
                                        Log.d("coordinatorTesting", "User with studentID " + studentId + " is a coordinator for event: " + eventUID + " with scanPermission: true");
                                        listener.onCheck(true, eventUID);
                                        return;
                                    } else {
                                        Log.w("coordinatorTesting", "User with studentID " + studentId + " is a coordinator for event: " + eventID + " but scanPermission is false or null");
                                        listener.onCheck(false, eventID); // Pass eventID even if scanPermission is false
                                        return;
                                    }
                                }
                            }
                            Log.w("coordinatorTesting", "User with studentID " + studentId + " is not a coordinator for any event.");
                            listener.onCheck(false, null);
                        } else {
                            Log.w("coordinatorTesting", "No events found in the database.");
                            listener.onCheck(false, null);
                        }
                    } else {
                        Log.e("coordinatorTesting", "Failed to fetch events. Error: " + (task.getException() != null ? task.getException().getMessage() : "Unknown error"));
                        listener.onCheck(false, null);
                    }
                });
            } else {
                Log.e("coordinatorTesting", "No student data found for studentID: " + studentId + ". Error: " + (studentTask.getException() != null ? studentTask.getException().getMessage() : "Unknown error"));
                listener.onCheck(false, null);
            }
        });
    }

    private void checkIfUserIsCoordinatorButNotAllowed(OnCoordinatorCheckListener listener) {
        FirebaseUser user = FirebaseAuth.getInstance().getCurrentUser();
        if (user == null) {
            Log.e("coordinatorTesting", "User not logged in. Cannot check coordinator status.");
            listener.onCheck(false, null);
            return;
        }

        SharedPreferences sharedPreferences = getSharedPreferences("UserSession", MODE_PRIVATE);
        String studentId = sharedPreferences.getString("studentID", null);
        if (studentId == null || studentId.isEmpty()) {
            Log.e("coordinatorTesting", "No studentID found in SharedPreferences for user: " + user.getUid());
            listener.onCheck(false, null);
            return;
        }

        DatabaseReference eventsRef = FirebaseDatabase.getInstance().getReference("events");
        eventsRef.get().addOnCompleteListener(task -> {
            if (task.isSuccessful() && task.getResult().exists()) {
                for (DataSnapshot eventSnapshot : task.getResult().getChildren()) {
                    DataSnapshot coordinatorsSnapshot = eventSnapshot.child("eventCoordinators");
                    if (coordinatorsSnapshot.hasChild(studentId)) {
                        String eventUID = eventSnapshot.getKey();
                        Log.d("coordinatorTesting", "User with studentID " + studentId + " is a coordinator for event: " + eventUID);
                        listener.onCheck(true, eventUID);
                        return;
                    }
                }
                Log.w("coordinatorTesting", "User with studentID " + studentId + " is not a coordinator for any event.");
                listener.onCheck(false, null);
            } else {
                Log.e("coordinatorTesting", "Failed to fetch events. Error: " + (task.getException() != null ? task.getException().getMessage() : "Unknown error"));
                listener.onCheck(false, null);
            }
        });
    }

    private void validateTicket(String scannedTicket, String coordinatorEventUID) {
        if (persistNotAllowedTicket) {
            showNotAllowedTicket();
            return;
        }

        Map<String, String> ticketData = parseQRContent(scannedTicket);
        String studentId = ticketData.get("studentID");
        String eventId = ticketData.get("eventUID");

        if (studentId == null || eventId == null) {
            showInvalidTicket();
            return;
        }

        if (!eventId.equals(coordinatorEventUID)) {
            Log.w("coordinatorTesting", "Ticket does not belong to the event the coordinator manages.");
            showNotAllowedTicket();
            notAllowedText.setText("You are not assigned as coordinator for this event.");
            persistNotAllowedTicket = true;
            return;
        }

        if (isTicketRecentlyScanned(studentId, eventId)) {
            showUsedTicket();
            usedText.setText("Ticket already scanned recently. Try again after 30 minutes for check-out.");
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
                Toast.makeText(QRCheckInActivity.this, "Error checking event: " + error.getMessage(), Toast.LENGTH_SHORT).show();
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

    private void checkAttendanceStatus(String studentId, String eventId, Map<String, String> ticketData, boolean isMultiDay) {
        final String[] dayKey = {getCurrentDayKey(ticketData.get("startDate"), ticketData.get("endDate"), ticketData.get("eventSpan"))};
        String currentDate = getCurrentDate();

        if (isNetworkAvailable()) {
            DatabaseReference ticketRef = databaseRef.child("students").child(studentId).child("tickets").child(eventId);
            ticketRef.addListenerForSingleValueEvent(new ValueEventListener() {
                @Override
                public void onDataChange(DataSnapshot snapshot) {
                    if (snapshot.exists()) {
                        boolean hasCheckedIn = false;
                        boolean hasCheckedOut = false;
                        boolean isLateCheckin = false;
                        DataSnapshot dayData = null;
                        String currentStatus = "N/A";

                        if (isMultiDay) {
                            DataSnapshot attendanceDays = snapshot.child("attendanceDays");
                            if (attendanceDays.exists()) {
                                for (DataSnapshot daySnapshot : attendanceDays.getChildren()) {
                                    if (daySnapshot.child("date").exists() &&
                                            currentDate.equals(daySnapshot.child("date").getValue(String.class))) {
                                        dayData = daySnapshot;
                                        dayKey[0] = daySnapshot.getKey();

                                        String checkInTime = daySnapshot.child("in").getValue(String.class);
                                        hasCheckedIn = checkInTime != null && !"N/A".equals(checkInTime);

                                        String checkOutTime = daySnapshot.child("out").getValue(String.class);
                                        hasCheckedOut = checkOutTime != null && !"N/A".equals(checkOutTime);

                                        if (daySnapshot.child("isLate").exists()) {
                                            isLateCheckin = daySnapshot.child("isLate").getValue(Boolean.class);
                                        }

                                        if (daySnapshot.child("status").exists()) {
                                            currentStatus = daySnapshot.child("status").getValue(String.class);
                                        }
                                        break;
                                    }
                                }
                            }
                        } else {
                            dayData = snapshot.child("attendanceDays").child("day_1");
                            dayKey[0] = "day_1";

                            if (dayData.exists()) {
                                String checkInTime = dayData.child("in").getValue(String.class);
                                hasCheckedIn = checkInTime != null && !"N/A".equals(checkInTime);

                                String checkOutTime = dayData.child("out").getValue(String.class);
                                hasCheckedOut = checkOutTime != null && !"N/A".equals(checkOutTime);

                                if (dayData.child("isLate").exists()) {
                                    isLateCheckin = dayData.child("isLate").getValue(Boolean.class);
                                }

                                if (dayData.child("status").exists()) {
                                    currentStatus = dayData.child("status").getValue(String.class);
                                }
                            }
                        }

                        int timeStatus = checkTimeStatus(ticketData);

                        if (dayData == null) {
                            showNotAllowedTicket();
                            notAllowedText.setText("No attendance record found for today");
                            persistNotAllowedTicket = true;
                        } else if (hasCheckedOut) {
                            showUsedTicket();
                            usedText.setText("Already checked out for today");
                        } else if (hasCheckedIn) {
                            if (timeStatus == TIME_STATUS_CAN_CHECKOUT) {
                                processCheckOut(ticketRef, dayKey[0], isMultiDay, isLateCheckin);
                            } else if (timeStatus == TIME_STATUS_ENDED) {
                                showNotAllowedTicket();
                                notAllowedText.setText("The event has ended. Check-out period closed.");
                                persistNotAllowedTicket = true;
                            } else {
                                showUsedTicket();
                                usedText.setText("Already checked in. Check-out available after event ends.");
                            }
                        } else {
                            if (timeStatus == TIME_STATUS_TOO_EARLY) {
                                showNotAllowedTicket();
                                notAllowedText.setText("The event hasn't started yet");
                                persistNotAllowedTicket = true;
                            } else if (timeStatus == TIME_STATUS_ENDED || timeStatus == TIME_STATUS_CHECKIN_ENDED) {
                                showNotAllowedTicket();
                                notAllowedText.setText("The event has ended. Check-in/out period closed.");
                                persistNotAllowedTicket = true;
                            } else {
                                boolean isLate = (timeStatus == TIME_STATUS_LATE);
                                processCheckIn(ticketRef, dayKey[0], isLate);
                            }
                        }
                    } else {
                        showNotAllowedTicket();
                        notAllowedText.setText("No ticket found for this student");
                        persistNotAllowedTicket = true;
                    }
                }

                @Override
                public void onCancelled(DatabaseError error) {
                    Toast.makeText(QRCheckInActivity.this, "Error: " + error.getMessage(), Toast.LENGTH_SHORT).show();
                    showInvalidTicket();
                }
            });
        } else {
            SQLiteDatabase db = dbHelper.getReadableDatabase();
            Cursor cursor = db.query(TicketDatabaseHelper.TABLE_SCAN_RECORDS,
                    null,
                    TicketDatabaseHelper.COLUMN_STUDENT_ID + " = ? AND " +
                            TicketDatabaseHelper.COLUMN_EVENT_UID + " = ? AND " +
                            TicketDatabaseHelper.COLUMN_DAY_KEY + " = ? AND " +
                            TicketDatabaseHelper.COLUMN_DATE + " = ?",
                    new String[]{studentId, eventId, dayKey[0], currentDate},
                    null, null, null);

            boolean hasCheckedIn = false;
            boolean hasCheckedOut = false;
            boolean isLateCheckin = false;
            String checkInTime = null;

            if (cursor.moveToFirst()) {
                checkInTime = cursor.getString(cursor.getColumnIndexOrThrow(TicketDatabaseHelper.COLUMN_CHECK_IN_TIME));
                hasCheckedIn = checkInTime != null && !"N/A".equals(checkInTime);
                String checkOutTime = cursor.getString(cursor.getColumnIndexOrThrow(TicketDatabaseHelper.COLUMN_CHECK_OUT_TIME));
                hasCheckedOut = checkOutTime != null && !"N/A".equals(checkOutTime);
                isLateCheckin = cursor.getInt(cursor.getColumnIndexOrThrow(TicketDatabaseHelper.COLUMN_IS_LATE)) == 1;
            }
            cursor.close();
            db.close();

            // Check if ticket exists in local database
            db = dbHelper.getReadableDatabase();
            Cursor ticketCursor = db.query(TicketDatabaseHelper.TABLE_SCAN_RECORDS,
                    null,
                    TicketDatabaseHelper.COLUMN_STUDENT_ID + " = ? AND " +
                            TicketDatabaseHelper.COLUMN_EVENT_UID + " = ?",
                    new String[]{studentId, eventId},
                    null, null, null);
            boolean ticketExists = ticketCursor.moveToFirst();
            ticketCursor.close();
            db.close();

            int timeStatus = checkTimeStatus(ticketData);

            if (!ticketExists) {
                showNotAllowedTicket();
                notAllowedText.setText("No ticket found for this student");
                persistNotAllowedTicket = true;
            } else if (hasCheckedOut) {
                showUsedTicket();
                usedText.setText("Already checked out for today");
            } else if (hasCheckedIn) {
                if (timeStatus == TIME_STATUS_CAN_CHECKOUT) {
                    processCheckOut(studentId, eventId, dayKey[0], currentDate, isLateCheckin);
                } else if (timeStatus == TIME_STATUS_ENDED) {
                    showNotAllowedTicket();
                    notAllowedText.setText("The event has ended. Check-out period closed.");
                    persistNotAllowedTicket = true;
                } else {
                    showUsedTicket();
                    usedText.setText("Already checked in. Check-out available after event ends.");
                }
            } else {
                if (timeStatus == TIME_STATUS_TOO_EARLY) {
                    showNotAllowedTicket();
                    notAllowedText.setText("The event hasn't started yet");
                    persistNotAllowedTicket = true;
                } else if (timeStatus == TIME_STATUS_ENDED || timeStatus == TIME_STATUS_CHECKIN_ENDED) {
                    showNotAllowedTicket();
                    notAllowedText.setText("The event has ended. Check-in/out period closed.");
                    persistNotAllowedTicket = true;
                } else {
                    boolean isLate = (timeStatus == TIME_STATUS_LATE);
                    processCheckIn(studentId, eventId, dayKey[0], currentDate, isLate);
                }
            }
        }
    }

    private void updateAttendanceStatus(DatabaseReference ticketRef, String dayKey, String newStatus) {
        ticketRef.child("attendanceDays").child(dayKey).child("status").setValue(newStatus)
                .addOnSuccessListener(aVoid -> {
                    Log.d("StatusUpdate", "Status updated to: " + newStatus);
                })
                .addOnFailureListener(e -> {
                    Log.e("StatusUpdate", "Failed to update status: " + e.getMessage());
                });
    }

    private void processCheckIn(DatabaseReference ticketRef, String dayKey, boolean isLate) {
        String currentTime = getCurrentTime();
        String currentDate = getCurrentDate();
        String status = "Ongoing";
        String attendance = "Ongoing";
        String studentId = ticketRef.getParent().getParent().getKey();
        String eventId = ticketRef.getParent().getKey();

        saveToLocalDatabase(studentId, eventId, dayKey, currentDate, currentTime, null, attendance, isLate, status);

        saveRecentScan(studentId, eventId);

        if (isNetworkAvailable()) {
            Map<String, Object> updates = new HashMap<>();
            updates.put("in", currentTime);
            updates.put("isLate", isLate);
            updates.put("status", status);
            updates.put("attendance", attendance);
            updates.put("date", currentDate);

            ticketRef.child("attendanceDays").child(dayKey).updateChildren(updates)
                    .addOnSuccessListener(aVoid -> {
                        updateSynchronizedStatus(ticketRef.getParent().getParent().getKey(), ticketRef.getParent().getKey(), dayKey);
                        String msg = isLate ? "Check-in successful. Note: Arrived late!" : "Check-in successful";
                        Toast.makeText(QRCheckInActivity.this, msg, Toast.LENGTH_SHORT).show();
                    })
                    .addOnFailureListener(e -> {
                        Toast.makeText(QRCheckInActivity.this, "Check-in saved locally due to error", Toast.LENGTH_SHORT).show();
                        Log.e(TAG, "Failed to sync check-in: " + e.getMessage());
                    });
        } else {
            Toast.makeText(this, isLate ? "Check-in saved locally (Late). Will sync when online." : "Check-in saved locally. Will sync when online.", Toast.LENGTH_SHORT).show();
        }

        showValidTicket();
        if (isLate) {
            validText.setText("Checked in at " + currentTime + " (Arrived late)");
        } else {
            validText.setText("Checked in at " + currentTime);
        }
    }

    private void processCheckIn(String studentId, String eventId, String dayKey, String date, boolean isLate) {
        String currentTime = getCurrentTime();
        String status = "Ongoing";
        String attendance = "Ongoing";

        saveToLocalDatabase(studentId, eventId, dayKey, date, currentTime, null, attendance, isLate, status);
        saveRecentScan(studentId, eventId);
        if (isNetworkAvailable()) {
            DatabaseReference ticketRef = databaseRef.child("students").child(studentId).child("tickets").child(eventId);
            Map<String, Object> updates = new HashMap<>();
            updates.put("in", currentTime);
            updates.put("status", status);
            updates.put("attendance", attendance);
            updates.put("isLate", isLate);
            updates.put("date", date);
            ticketRef.child("attendanceDays").child(dayKey).updateChildren(updates)
                    .addOnSuccessListener(aVoid -> {
                        updateSynchronizedStatus(studentId, eventId, dayKey);
                        String msg = isLate ? "Check-in successful. Note: Arrived late!" : "Check-in successful";
                        Toast.makeText(QRCheckInActivity.this, msg, Toast.LENGTH_SHORT).show();
                    })
                    .addOnFailureListener(e -> {
                        Toast.makeText(QRCheckInActivity.this, "Check-in saved locally due to error", Toast.LENGTH_SHORT).show();
                        Log.e(TAG, "Failed to sync check-in: " + e.getMessage());
                    });
        } else {
            Toast.makeText(this, isLate ? "Check-in saved locally (Late). Will sync when online." : "Check-in saved locally. Will sync when online.", Toast.LENGTH_SHORT).show();
        }

        showValidTicket();
        validText.setText(isLate ? "Checked in at " + currentTime + " (Arrived late)" : "Checked in at " + currentTime);
    }

    private void processCheckOut(DatabaseReference ticketRef, String dayKey, boolean isMultiDay, boolean isLateCheckin) {
        String currentTime = getCurrentTime();
        String currentDate = getCurrentDate();
        String status = "Pending";
        String attendance = isLateCheckin ? "Late" : "Present";
        String studentId = ticketRef.getParent().getParent().getKey();
        String eventId = ticketRef.getParent().getKey();

        saveToLocalDatabase(studentId, eventId, dayKey, currentDate, null, currentTime, attendance, isLateCheckin, status);

        saveRecentScan(studentId, eventId);  // ← ADD THIS
        if (isNetworkAvailable()) {
            Map<String, Object> updates = new HashMap<>();
            updates.put("out", currentTime);
            updates.put("attendance", attendance);
            updates.put("status", status);
            updates.put("date", currentDate);

            ticketRef.child("attendanceDays").child(dayKey).updateChildren(updates)
                    .addOnSuccessListener(aVoid -> {
                        updateSynchronizedStatus(ticketRef.getParent().getParent().getKey(), ticketRef.getParent().getKey(), dayKey);
                        String msg = isLateCheckin ? "Check-out successful (Attendance marked as Late)" : "Check-out successful (Attendance marked as Present)";
                        Toast.makeText(QRCheckInActivity.this, msg, Toast.LENGTH_SHORT).show();
                    })
                    .addOnFailureListener(e -> {
                        Toast.makeText(QRCheckInActivity.this, "Check-out saved locally due to error", Toast.LENGTH_SHORT).show();
                        Log.e(TAG, "Failed to sync check-out: " + e.getMessage());
                    });
        } else {
            Toast.makeText(this, "Check-out saved locally. Will sync when online.", Toast.LENGTH_SHORT).show();
        }

        showValidTicket();
        if (isLateCheckin) {
            validText.setText("Checked out at " + currentTime + " (Attendance: Late)");
        } else {
            validText.setText("Checked out at " + currentTime + " (Attendance: Present)");
        }
    }

    private void processCheckOut(String studentId, String eventId, String dayKey, String date, boolean isLateCheckin) {
        String currentTime = getCurrentTime();
        String status = "Pending";
        String attendance = isLateCheckin ? "Late" : "Present";

        saveToLocalDatabase(studentId, eventId, dayKey, date, null, currentTime, attendance, isLateCheckin, status);
        saveRecentScan(studentId, eventId);
        if (isNetworkAvailable()) {
            DatabaseReference ticketRef = databaseRef.child("students").child(studentId).child("tickets").child(eventId);
            Map<String, Object> updates = new HashMap<>();
            updates.put("out", currentTime);
            updates.put("status", status);
            updates.put("attendance", attendance);
            updates.put("date", date);
            ticketRef.child("attendanceDays").child(dayKey).updateChildren(updates)
                    .addOnSuccessListener(aVoid -> {
                        updateSynchronizedStatus(studentId, eventId, dayKey);
                        String msg = isLateCheckin ? "Check-out successful (Attendance marked as Late)" : "Check-out successful (Attendance marked as Present)";
                        Toast.makeText(QRCheckInActivity.this, msg, Toast.LENGTH_SHORT).show();
                    })
                    .addOnFailureListener(e -> {
                        Toast.makeText(QRCheckInActivity.this, "Check-out saved locally due to error", Toast.LENGTH_SHORT).show();
                        Log.e(TAG, "Failed to sync check-out: " + e.getMessage());
                    });
        } else {
            Toast.makeText(this, "Check-out saved locally. Will sync when online.", Toast.LENGTH_SHORT).show();
        }

        showValidTicket();
        validText.setText(isLateCheckin ? "Checked out at " + currentTime + " (Attendance: Late)" : "Checked out at " + currentTime + " (Attendance: Present)");
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
            noPermissionText.setVisibility(TextView.GONE);
            noScanPermissionText.setVisibility(TextView.GONE);
            scanTicketBtn.setVisibility(Button.VISIBLE);
        } else {
            FirebaseUser user = FirebaseAuth.getInstance().getCurrentUser();
            if (user != null) {
                String userEmail = user.getEmail();
                checkIfUserIsCoordinator(userEmail, (isCoordinator, eventUID) -> {
                    if (isCoordinator && eventUID != null) {
                        hideAllTicketViews();
                        getStarted.setVisibility(TextView.VISIBLE);
                        instructionForScanning.setVisibility(TextView.GONE);
                        noPermissionText.setVisibility(TextView.GONE);
                        noScanPermissionText.setVisibility(TextView.GONE);
                        scanTicketBtn.setVisibility(Button.VISIBLE);
                    } else {
                        checkIfUserIsCoordinatorButNotAllowed((isCoordinatorNoScan, eventUID2) -> {
                            if (isCoordinatorNoScan) {
                                hideAllTicketViews();
                                getStarted.setVisibility(TextView.GONE);
                                instructionForScanning.setVisibility(TextView.GONE);
                                noPermissionText.setVisibility(TextView.GONE);
                                noScanPermissionText.setVisibility(TextView.VISIBLE);
                                scanTicketBtn.setVisibility(Button.GONE);
                            } else {
                                hideAllTicketViews();
                                getStarted.setVisibility(TextView.GONE);
                                instructionForScanning.setVisibility(TextView.GONE);
                                noPermissionText.setVisibility(TextView.VISIBLE);
                                noScanPermissionText.setVisibility(TextView.GONE);
                                scanTicketBtn.setVisibility(Button.GONE);
                            }
                        });
                    }
                });
            } else {
                hideAllTicketViews();
                getStarted.setVisibility(TextView.GONE);
                instructionForScanning.setVisibility(TextView.GONE);
                noPermissionText.setVisibility(TextView.VISIBLE);
                noScanPermissionText.setVisibility(TextView.GONE);
                scanTicketBtn.setVisibility(Button.GONE);
            }
        }
    }

    private String getCurrentTime() {
        SimpleDateFormat sdf = new SimpleDateFormat("HH:mm", Locale.getDefault());
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
                Calendar checkoutDeadline = Calendar.getInstance();
                checkoutDeadline.setTime(eventEndTime);
                checkoutDeadline.add(Calendar.HOUR, 1);

                if (currentDate.after(checkoutDeadline.getTime())) {
                    return TIME_STATUS_ENDED;
                } else {
                    return TIME_STATUS_CAN_CHECKOUT;
                }
            }

            if (graceTimeStr == null || graceTimeStr.isEmpty() ||
                    graceTimeStr.equalsIgnoreCase("none") || graceTimeStr.equalsIgnoreCase("null")) {
                return TIME_STATUS_ON_TIME;
            }

            int graceTime;
            try {
                graceTime = Integer.parseInt(graceTimeStr);
            } catch (NumberFormatException e) {
                graceTime = 0;
            }

            Calendar graceEndCalendar = Calendar.getInstance();
            graceEndCalendar.setTime(eventStartTime);
            graceEndCalendar.add(Calendar.MINUTE, graceTime);
            Date graceEndTime = graceEndCalendar.getTime();

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
        noPermissionText.setVisibility(TextView.GONE);
        noScanPermissionText.setVisibility(TextView.GONE);
    }

    public void resetPersistentState() {
        persistNotAllowedTicket = false;
        resetScanUI();
    }

    @Override
    protected void onResume() {
        super.onResume();
        captureManager.onResume();
        loadUserProfile();
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

    private void setupBottomNavigation() {
        BottomNavigationView bottomNavigationView = findViewById(com.capstone.mdfeventmanagementsystem.R.id.bottom_navigation);

        bottomNavigationView.getMenu().setGroupCheckable(0, false, true);

        for (int i = 0; i < bottomNavigationView.getMenu().size(); i++) {
            bottomNavigationView.getMenu().getItem(i).setChecked(false);
        }

        findViewById(R.id.fab_scan).setSelected(true);

        bottomNavigationView.setLabelVisibilityMode(BottomNavigationView.LABEL_VISIBILITY_UNLABELED);
        bottomNavigationView.setBackground(null);

        bottomNavigationView.setOnItemSelectedListener(item -> {
            int itemId = item.getItemId();

            if (itemId == com.capstone.mdfeventmanagementsystem.R.id.nav_home) {
                startActivity(new Intent(this, MainActivity2.class));
                finish();
            } else if (itemId == com.capstone.mdfeventmanagementsystem.R.id.nav_event) {
                startActivity(new Intent(this, StudentDashboard.class));
                finish();
            } else if (itemId == com.capstone.mdfeventmanagementsystem.R.id.nav_ticket) {
                startActivity(new Intent(this, StudentTickets.class));
                finish();
            } else if (itemId == com.capstone.mdfeventmanagementsystem.R.id.nav_cert) {
                startActivity(new Intent(this, StudentCertificate.class));
                finish();
            }

            overridePendingTransition(0, 0);
            return true;
        });
    }

    private void loadCachedProfileImage() {
        SharedPreferences prefs = getSharedPreferences("ProfileImageCache", MODE_PRIVATE);
        String cachedImageUrl = prefs.getString("profileImageUrl", "");

        if (!cachedImageUrl.isEmpty()) {
            loadProfileImageFromCache(cachedImageUrl);
        } else {
            setDefaultProfileImage();
        }
    }

    private void loadProfileImageFromCache(String imageUrl) {
        if (profileImageView == null) {
            return;
        }

        Picasso.get()
                .load(imageUrl)
                .transform(new CircleTransformQRCheckIn())
                .placeholder(R.drawable.profile_placeholder)
                .error(R.drawable.profile_placeholder)
                .networkPolicy(com.squareup.picasso.NetworkPolicy.OFFLINE)
                .into(profileImageView, new com.squareup.picasso.Callback() {
                    @Override
                    public void onSuccess() {
                        Log.d(TAG, "Profile image loaded from cache");
                    }

                    @Override
                    public void onError(Exception e) {
                        setDefaultProfileImage();
                    }
                });
    }

    private void cacheProfileImageUrl(String imageUrl) {
        SharedPreferences prefs = getSharedPreferences("ProfileImageCache", MODE_PRIVATE);
        SharedPreferences.Editor editor = prefs.edit();
        editor.putString("profileImageUrl", imageUrl);
        editor.apply();
    }

    private void loadUserProfile() {
        FirebaseUser user = FirebaseAuth.getInstance().getCurrentUser();
        if (user != null) {
            checkProfileImage(user.getUid());
        }
    }

    private void checkProfileImage(String uid) {
        profilesRef.child(uid).addListenerForSingleValueEvent(new ValueEventListener() {
            @Override
            public void onDataChange(@NonNull DataSnapshot dataSnapshot) {
                if (dataSnapshot.exists() && dataSnapshot.hasChild("profileImage")) {
                    String profileImageUrl = dataSnapshot.child("profileImage").getValue(String.class);
                    if (profileImageUrl != null && !profileImageUrl.isEmpty()) {
                        Log.d(TAG, "Found profile image in student_profiles: " + profileImageUrl);
                        loadProfileImage(profileImageUrl);
                    } else {
                        checkStudentProfileImage(uid);
                    }
                } else {
                    Log.d(TAG, "No profile image found in student_profiles, checking students collection");
                    checkStudentProfileImage(uid);
                }
            }

            @Override
            public void onCancelled(@NonNull DatabaseError error) {
                Log.e(TAG, "Error checking student_profiles: " + error.getMessage());
                checkStudentProfileImage(uid);
            }
        });
    }

    private void checkStudentProfileImage(String uid) {
        studentsRef.child(uid).child("profileImage").addListenerForSingleValueEvent(new ValueEventListener() {
            @Override
            public void onDataChange(@NonNull DataSnapshot dataSnapshot) {
                if (dataSnapshot.exists()) {
                    String profileImageUrl = dataSnapshot.getValue(String.class);
                    if (profileImageUrl != null && !profileImageUrl.isEmpty()) {
                        Log.d(TAG, "Found profile image in students collection: " + profileImageUrl);
                        loadProfileImage(profileImageUrl);
                    } else {
                        Log.d(TAG, "Profile image field exists but is empty");
                    }
                } else {
                    Log.d(TAG, "No profile image in students collection");
                    FirebaseUser user = FirebaseAuth.getInstance().getCurrentUser();
                    if (user != null && user.getPhotoUrl() != null) {
                        loadProfileImage(user.getPhotoUrl().toString());
                    }
                }
            }

            @Override
            public void onCancelled(@NonNull DatabaseError error) {
                Log.e(TAG, "Error checking student profile image: " + error.getMessage());
            }
        });
    }

    private void loadProfileImage(String imageUrl) {
        if (profileImageView == null) {
            Log.e(TAG, "Cannot load profile image: ImageView is null");
            return;
        }

        if (imageUrl != null && !imageUrl.isEmpty()) {
            Log.d(TAG, "Loading profile image from: " + imageUrl);

            Picasso.get()
                    .load(imageUrl)
                    .transform(new CircleTransformQRCheckIn())
                    .placeholder(R.drawable.profile_placeholder)
                    .error(R.drawable.profile_placeholder)
                    .into(profileImageView, new com.squareup.picasso.Callback() {
                        @Override
                        public void onSuccess() {
                            Log.d(TAG, "Profile image loaded successfully");
                            cacheProfileImageUrl(imageUrl);
                        }

                        @Override
                        public void onError(Exception e) {
                            Log.e(TAG, "Error loading profile image: " + (e != null ? e.getMessage() : "unknown error"));
                        }
                    });
        }
    }

    private void setDefaultProfileImage() {
        if (profileImageView != null) {
            profileImageView.setImageResource(R.drawable.profile_placeholder);
        }
    }
}