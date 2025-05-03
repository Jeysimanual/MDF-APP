package com.capstone.mdfeventmanagementsystem.Student;

import android.Manifest;
import android.app.AlertDialog;
import android.content.ContentResolver;
import android.content.ContentValues;
import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.content.pm.PackageManager;
import android.database.Cursor;
import android.graphics.Bitmap;
import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.Paint;
import android.graphics.Path;
import android.graphics.PorterDuff;
import android.graphics.PorterDuffXfermode;
import android.graphics.RectF;
import android.media.MediaScannerConnection;
import android.net.Uri;
import android.os.Build;
import android.os.Bundle;
import android.os.Environment;
import android.provider.MediaStore;
import android.util.Log;
import android.view.View;
import android.widget.Button;
import android.widget.ImageView;
import android.widget.ScrollView;
import android.widget.TextView;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;
import androidx.cardview.widget.CardView;
import androidx.core.app.ActivityCompat;
import androidx.core.content.ContextCompat;

import com.bumptech.glide.Glide;
import com.capstone.mdfeventmanagementsystem.R;
import com.capstone.mdfeventmanagementsystem.Utilities.BaseActivity;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseUser;
import com.google.firebase.database.DataSnapshot;
import com.google.firebase.database.DatabaseError;
import com.google.firebase.database.DatabaseReference;
import com.google.firebase.database.FirebaseDatabase;
import com.google.firebase.database.ValueEventListener;

import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.OutputStream;
import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.Calendar;
import java.util.Date;
import java.util.HashMap;
import java.util.Locale;
import java.util.Map;


public class StudentTicketsInside extends BaseActivity {

    private ScrollView ticketLayout;
    private CardView mdfTicketCardView;
    private Button downloadButton;
    private TextView eventName, startDate, endDate, graceTime, eventSpan, venue, dayText;
    private ImageView qrCodeImage;
    private static final int STORAGE_PERMISSION_CODE = 100;
    private static final String TAG = "StudentTicketsInsider";
    private String ticketId;
    private String studentId; // Will store student ID from SharedPreferences
    private DatabaseReference mDatabase;
    private FirebaseUser currentUser;
    private SharedPreferences sharedPreferences;
    private static final String PREFS_NAME = "MDFUserSession";
    private static final String DOWNLOADS_PREFS = "MDFTicketDownloads"; // For tracking downloads
    private String safeEventName; // Store safe event name for reuse

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_student_tickets_inside);

        // Initialize Firebase
        mDatabase = FirebaseDatabase.getInstance().getReference();
        currentUser = FirebaseAuth.getInstance().getCurrentUser();

        // Get studentId from SharedPreferences using "UserSession" consistently
        sharedPreferences = getSharedPreferences("UserSession", Context.MODE_PRIVATE);
        studentId = sharedPreferences.getString("studentID", "");

        if (studentId == null || studentId.isEmpty()) {
            Toast.makeText(this, "Student ID not found! Please log in again.", Toast.LENGTH_SHORT).show();
            Log.e(TAG, "No studentID found in SharedPreferences!");
            // Redirect back to login
            Intent intent = new Intent(StudentTicketsInside.this, StudentLogin.class);
            startActivity(intent);
            finish();
            return;
        }

        Log.d(TAG, "Using studentID from SharedPreferences: " + studentId);

        // Initialize UI elements
        ticketLayout = findViewById(R.id.ticketLayout);
        mdfTicketCardView = findViewById(R.id.mdfTicketCardView);
        downloadButton = findViewById(R.id.downloadButton);
        eventName = findViewById(R.id.eventName);
        startDate = findViewById(R.id.startDate);
        endDate = findViewById(R.id.endDate);
        graceTime = findViewById(R.id.graceTime);
        eventSpan = findViewById(R.id.eventSpan);
        venue = findViewById(R.id.venue);
        qrCodeImage = findViewById(R.id.qrCodeImage);
        dayText = findViewById(R.id.day);
        TextView timeText = findViewById(R.id.time);

        // Get data from intent
        Intent intent = getIntent();
        String eventNameText = intent.getStringExtra("eventName");
        String startDateText = intent.getStringExtra("startDate");
        String endDateText = intent.getStringExtra("endDate");
        String startTimeText = intent.getStringExtra("startTime");
        String endTimeText = intent.getStringExtra("endTime");
        String graceTimeText = intent.getStringExtra("graceTime");
        String eventSpanText = intent.getStringExtra("eventSpan");
        String venueText = intent.getStringExtra("venue");
        String qrCodeUrl = intent.getStringExtra("qrCodeUrl");
        ticketId = intent.getStringExtra("ticketId");

        // Set safe event name for reuse
        safeEventName = getSafeFileName(eventNameText);

        // Validate ticketId from intent
        if (ticketId == null || ticketId.isEmpty()) {
            Log.e(TAG, "Warning: Received null or empty ticketId from intent");
            // Generate a fallback ticketId using event name and current time if needed
            if (eventNameText != null && !eventNameText.isEmpty()) {
                ticketId = safeEventName + "_" + System.currentTimeMillis();
                Log.d(TAG, "Generated fallback ticketId: " + ticketId);
            }
        } else {
            Log.d(TAG, "Received ticketId: " + ticketId);
        }

        Log.d(TAG, "Current user: " + (currentUser != null ? currentUser.getUid() : "null"));

        // Set data to UI
        eventName.setText(eventNameText);
        graceTime.setText(graceTimeText);
        eventSpan.setText(eventSpanText);
        venue.setText(venueText);

        if ("single-day".equals(eventSpanText)) {
            startDate.setText(startDateText);
            endDate.setVisibility(View.GONE);
        } else if ("multi-day".equals(eventSpanText)) {
            startDate.setText(startDateText);
            if (endDateText != null && !endDateText.isEmpty()) {
                endDate.setText(endDateText);
                endDate.setVisibility(View.VISIBLE);
            } else {
                endDate.setVisibility(View.GONE);
            }
        }

        // Combine startTime and endTime for the "time" TextView
        if (startTimeText != null && endTimeText != null) {
            String combinedTime = formatTime(startTimeText) + " - " + formatTime(endTimeText);
            timeText.setText(combinedTime);
        }

        // Get day from startDate and set it
        if (startDateText != null && !startDateText.isEmpty()) {
            dayText.setText(getDayOfWeek(startDateText));
        }

        // Load QR Code using Glide
        if (qrCodeUrl != null && !qrCodeUrl.isEmpty()) {
            Glide.with(this).load(qrCodeUrl).into(qrCodeImage);
        } else {
            qrCodeImage.setImageResource(R.drawable.placeholder_qr);
        }

        // Update download button text based on physical file existence
        updateDownloadButtonState();

        // Set up download button functionality
        downloadButton.setOnClickListener(v -> {
            Log.d(TAG, "Save button clicked.");
            if (checkStoragePermission()) {
                if (isTicketImageExistsInStorage()) {
                    new AlertDialog.Builder(this)
                            .setTitle("Ticket Already Downloaded")
                            .setMessage("You've already saved this ticket. Do you want to download it again?")
                            .setPositiveButton("Yes", (dialog, which) -> {
                                downloadButton.setText("DOWNLOADING...");
                                saveTicketImage();
                            })
                            .setNegativeButton("No", null)
                            .show();
                } else {
                    downloadButton.setText("DOWNLOADING...");
                    saveTicketImage();
                }
            } else {
                requestStoragePermission();
            }
        });
    }

    @Override
    protected void onResume() {
        super.onResume();
        // Check file existence whenever the activity comes to foreground
        updateDownloadButtonState();
    }

    /**
     * Update download button text based on physical file existence in storage
     */
    private void updateDownloadButtonState() {
        if (isTicketImageExistsInStorage()) {
            downloadButton.setText("DOWNLOADED");
        } else {
            // Even if SharedPreferences says downloaded, but file doesn't exist
            downloadButton.setText("DOWNLOAD");

            // If the file doesn't exist but we have a record in SharedPreferences, clear it
            if (isTicketRecordedAsDownloaded()) {
                // Clear the SharedPreferences record since the file is deleted
                clearDownloadRecord();
            }
        }
    }

    /**
     * Check if the ticket is recorded as downloaded in SharedPreferences
     */
    private boolean isTicketRecordedAsDownloaded() {
        SharedPreferences downloadPrefs = getSharedPreferences(DOWNLOADS_PREFS, Context.MODE_PRIVATE);
        String key = studentId + "_" + safeEventName;
        return downloadPrefs.getBoolean(key, false);
    }

    /**
     * Clear download record from SharedPreferences if file doesn't exist anymore
     */
    private void clearDownloadRecord() {
        SharedPreferences downloadPrefs = getSharedPreferences(DOWNLOADS_PREFS, Context.MODE_PRIVATE);
        String key = studentId + "_" + safeEventName;
        SharedPreferences.Editor editor = downloadPrefs.edit();
        editor.remove(key);
        editor.apply();
        Log.d(TAG, "Cleared download record for deleted file: " + key);
    }

    /**
     * Check if the ticket image physically exists in storage
     */
    private boolean isTicketImageExistsInStorage() {
        String fileName = safeEventName + "_ticket.png";

        if (android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.Q) {
            // For Android 10+, check in MediaStore
            return isImageExistsInMediaStore(fileName);
        } else {
            // For older Android versions, check the file directly
            File directory = new File(Environment.getExternalStoragePublicDirectory(Environment.DIRECTORY_PICTURES), "MDF Events");
            File file = new File(directory, fileName);
            boolean exists = file.exists();
            Log.d(TAG, "File check: " + file.getAbsolutePath() + " exists: " + exists);
            return exists;
        }
    }

    /**
     * Check if image exists in MediaStore (Android 10+)
     */
    private boolean isImageExistsInMediaStore(String fileName) {
        boolean exists = false;

        ContentResolver resolver = getContentResolver();
        Uri imageCollection = MediaStore.Images.Media.EXTERNAL_CONTENT_URI;

        // Define the projection (columns to return)
        String[] projection = new String[]{
                MediaStore.Images.Media._ID
        };

        // Define the selection criteria
        String selection = MediaStore.Images.Media.DISPLAY_NAME + "=? AND " +
                MediaStore.Images.Media.RELATIVE_PATH + " LIKE ?";
        String[] selectionArgs = new String[]{
                fileName,
                "%" + Environment.DIRECTORY_PICTURES + "/MDF Events%"
        };

        try (Cursor cursor = resolver.query(
                imageCollection,
                projection,
                selection,
                selectionArgs,
                null)) {

            exists = (cursor != null && cursor.getCount() > 0);
            Log.d(TAG, "MediaStore check for " + fileName + ": " + exists);
        } catch (Exception e) {
            Log.e(TAG, "Error checking MediaStore: " + e.getMessage());
        }

        return exists;
    }

    /**
     * Convert event name to a safe filename
     */
    private String getSafeFileName(String name) {
        return name.replaceAll("[^a-zA-Z0-9]", "_").toLowerCase();
    }

    /**
     * Record that user has downloaded this ticket
     */
    private void recordTicketDownload(String eventName) {
        SharedPreferences downloadPrefs = getSharedPreferences(DOWNLOADS_PREFS, Context.MODE_PRIVATE);
        String key = studentId + "_" + eventName;
        SharedPreferences.Editor editor = downloadPrefs.edit();
        editor.putBoolean(key, true);
        editor.apply();
    }

    // Check if storage permission is granted based on Android version
    private boolean checkStoragePermission() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            // For Android 13+ (API 33+), check only image permission since that's all we need
            boolean readImagesGranted = ContextCompat.checkSelfPermission(this,
                    Manifest.permission.READ_MEDIA_IMAGES) == PackageManager.PERMISSION_GRANTED;

            Log.d(TAG, "Android 13+ permission check - Images: " + readImagesGranted);
            return readImagesGranted;
        } else if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.R) {
            // For Android 11 and 12 (API 30-32), we should use scoped storage
            Log.d(TAG, "Android 11-12: Using scoped storage approach");
            return Environment.isExternalStorageManager();
        } else {
            // For Android 10 and below (API 29 and below)
            boolean readPermission = ContextCompat.checkSelfPermission(this,
                    Manifest.permission.READ_EXTERNAL_STORAGE) == PackageManager.PERMISSION_GRANTED;
            boolean writePermission = ContextCompat.checkSelfPermission(this,
                    Manifest.permission.WRITE_EXTERNAL_STORAGE) == PackageManager.PERMISSION_GRANTED;

            Log.d(TAG, "Android 10 and below permission check - Read: " + readPermission +
                    ", Write: " + writePermission);

            return readPermission && writePermission;
        }
    }

    /** Request storage permissions based on Android version */
    private void requestStoragePermission() {
        Log.d(TAG, "Requesting storage permission for Android SDK: " + Build.VERSION.SDK_INT);

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            // For Android 13+ (API 33+), only request image permission
            if (ContextCompat.checkSelfPermission(this, Manifest.permission.READ_MEDIA_IMAGES) !=
                    PackageManager.PERMISSION_GRANTED) {

                ActivityCompat.requestPermissions(this, new String[]{
                        Manifest.permission.READ_MEDIA_IMAGES
                }, STORAGE_PERMISSION_CODE);

                Log.d(TAG, "requestStoragePermission: Requesting only READ_MEDIA_IMAGES permission for Android 13+");
            } else {
                Log.d(TAG, "requestStoragePermission: Image permission already granted");
                downloadButton.setText("DOWNLOADING...");
                saveTicketImage(); // Permissions already granted, save image
            }
        } else if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.R) {
            // For Android 11 and 12 (API 30-32), use MANAGE_EXTERNAL_STORAGE for full access
            Log.d(TAG, "requestStoragePermission: For Android 11-12, app should use scoped storage or get user to allow 'All Files Access'");

            // Show dialog explaining the need to go to settings
            new AlertDialog.Builder(this)
                    .setTitle("Storage Permission Required")
                    .setMessage("For Android 11 and above, please grant 'All Files Access' permission from settings.")
                    .setPositiveButton("Go to Settings", (dialog, which) -> {
                        try {
                            Intent intent = new Intent();
                            intent.setAction(android.provider.Settings.ACTION_MANAGE_APP_ALL_FILES_ACCESS_PERMISSION);
                            Uri uri = Uri.fromParts("package", getPackageName(), null);
                            intent.setData(uri);
                            startActivity(intent);
                        } catch (Exception e) {
                            Log.e(TAG, "Error opening settings: " + e.getMessage());
                            Toast.makeText(this, "Unable to open settings", Toast.LENGTH_SHORT).show();
                        }
                    })
                    .setNegativeButton("Cancel", null)
                    .show();
        } else {
            // For Android 10 and below (API 29 and below)
            if (ContextCompat.checkSelfPermission(this, Manifest.permission.READ_EXTERNAL_STORAGE) !=
                    PackageManager.PERMISSION_GRANTED ||
                    ContextCompat.checkSelfPermission(this, Manifest.permission.WRITE_EXTERNAL_STORAGE) !=
                            PackageManager.PERMISSION_GRANTED) {

                // Check if we should show rationale
                if (ActivityCompat.shouldShowRequestPermissionRationale(this,
                        Manifest.permission.WRITE_EXTERNAL_STORAGE)) {

                    new AlertDialog.Builder(this)
                            .setTitle("Storage Permission Required")
                            .setMessage("This app needs permission to save tickets to your device storage.")
                            .setPositiveButton("OK", (dialog, which) -> {
                                // Request permission after showing rationale
                                ActivityCompat.requestPermissions(this, new String[]{
                                        Manifest.permission.READ_EXTERNAL_STORAGE,
                                        Manifest.permission.WRITE_EXTERNAL_STORAGE
                                }, STORAGE_PERMISSION_CODE);
                            })
                            .setNegativeButton("Cancel", (dialog, which) -> {
                                Toast.makeText(this, "Cannot save ticket without storage permission",
                                        Toast.LENGTH_LONG).show();
                            })
                            .create()
                            .show();
                } else {
                    // No explanation needed; request the permission
                    ActivityCompat.requestPermissions(this, new String[]{
                            Manifest.permission.READ_EXTERNAL_STORAGE,
                            Manifest.permission.WRITE_EXTERNAL_STORAGE
                    }, STORAGE_PERMISSION_CODE);
                }

                Log.d(TAG, "requestStoragePermission: Requesting READ/WRITE permissions for Android 10 and below");
            } else {
                Log.d(TAG, "requestStoragePermission: Storage permissions already granted");
                downloadButton.setText("DOWNLOADING...");
                saveTicketImage(); // Permissions already granted, save image
            }
        }
    }

    // Handle the result of the permission request
    @Override
    public void onRequestPermissionsResult(int requestCode, @NonNull String[] permissions, @NonNull int[] grantResults) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults);

        if (requestCode == STORAGE_PERMISSION_CODE) {
            if (grantResults.length > 0) {
                boolean allPermissionsGranted = true;
                for (int result : grantResults) {
                    if (result != PackageManager.PERMISSION_GRANTED) {
                        allPermissionsGranted = false;
                        break;
                    }
                }

                if (allPermissionsGranted) {
                    Log.d(TAG, "onRequestPermissionsResult: Storage permissions granted");
                    Toast.makeText(this, "Storage permissions granted", Toast.LENGTH_SHORT).show();
                    downloadButton.setText("DOWNLOADING...");
                    saveTicketImage(); // Proceed with the download after permissions granted
                } else {
                    Log.d(TAG, "onRequestPermissionsResult: Storage permissions denied");
                    Toast.makeText(this, "Storage permissions are required for saving tickets", Toast.LENGTH_LONG).show();
                    // Reset button text if permissions denied
                    updateDownloadButtonState();
                }
            }
        }
    }

    /**
     * Main method to handle saving the ticket image
     */
    private void saveTicketImage() {
        Log.d(TAG, "Starting image download.");
        if (android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.Q) {
            // Use Storage Access Framework for Android 10+
            saveAsImageModern();
        } else {
            // Use old method for Android 9 and below
            saveAsImage();
        }
    }

    private void saveAsImageModern() {
        // Hide download button before capturing
        downloadButton.setVisibility(View.GONE);

        // Capture the bitmap
        Bitmap bitmap = getTicketBitmap();

        // Create standardized file name
        String fileName = safeEventName + "_ticket.png";

        ContentValues contentValues = new ContentValues();
        contentValues.put(MediaStore.MediaColumns.DISPLAY_NAME, fileName);
        contentValues.put(MediaStore.MediaColumns.MIME_TYPE, "image/png");
        contentValues.put(MediaStore.MediaColumns.RELATIVE_PATH, Environment.DIRECTORY_PICTURES + File.separator + "MDF Events");

        Uri imageUri = getContentResolver().insert(MediaStore.Images.Media.EXTERNAL_CONTENT_URI, contentValues);

        if (imageUri != null) {
            try (OutputStream fos = getContentResolver().openOutputStream(imageUri)) {
                bitmap.compress(Bitmap.CompressFormat.PNG, 100, fos);

                // Mark as downloaded in both SharedPreferences and Firebase
                recordTicketDownload(safeEventName);

                // Restore button visibility and change text to "DOWNLOADED"
                downloadButton.setVisibility(View.VISIBLE);
                downloadButton.setText("DOWNLOADED");

                Toast.makeText(this, "Ticket saved to gallery!", Toast.LENGTH_LONG).show();
                Log.d(TAG, "Ticket saved as PNG using modern Storage API");
            } catch (IOException e) {
                Log.e(TAG, "Error saving image: " + e.getMessage(), e);
                Toast.makeText(this, "Failed to save image", Toast.LENGTH_SHORT).show();

                // Restore button visibility and reset text on error
                downloadButton.setVisibility(View.VISIBLE);
                updateDownloadButtonState();
            }
        } else {
            Log.e(TAG, "Failed to create media store entry");
            Toast.makeText(this, "Failed to create image file", Toast.LENGTH_SHORT).show();

            // Restore button visibility and reset text on error
            downloadButton.setVisibility(View.VISIBLE);
            updateDownloadButtonState();
        }
    }

    private void saveAsImage() {
        Log.d(TAG, "Saving as PNG using legacy method.");

        // Hide download button before capturing
        downloadButton.setVisibility(View.GONE);

        // Capture the bitmap
        Bitmap bitmap = getTicketBitmap();

        if (bitmap == null) {
            Log.e(TAG, "Failed to capture ticket as image.");
            Toast.makeText(this, "Failed to capture ticket.", Toast.LENGTH_SHORT).show();

            // Restore button visibility and reset text on error
            downloadButton.setVisibility(View.VISIBLE);
            updateDownloadButtonState();
            return;
        }

        // Create standardized file name
        String fileName = safeEventName + "_ticket.png";

        // Directory name "MDF Events"
        File directory = new File(Environment.getExternalStoragePublicDirectory(Environment.DIRECTORY_PICTURES), "MDF Events");
        if (!directory.exists()) {
            if (directory.mkdirs()) {
                Log.d(TAG, "Created directory: " + directory.getAbsolutePath());
            } else {
                Log.e(TAG, "Failed to create directory.");
            }
        }

        File file = new File(directory, fileName);
        try (FileOutputStream fos = new FileOutputStream(file)) {
            if (bitmap.compress(Bitmap.CompressFormat.PNG, 100, fos)) {
                Log.d(TAG, "Ticket saved as PNG: " + file.getAbsolutePath());

                // Make the file visible in the gallery
                MediaScannerConnection.scanFile(this,
                        new String[]{file.getAbsolutePath()},
                        new String[]{"image/png"},
                        null);

                // Mark as downloaded in both SharedPreferences and Firebase
                recordTicketDownload(safeEventName);

                // Restore button visibility and change text to "DOWNLOADED"
                downloadButton.setVisibility(View.VISIBLE);
                downloadButton.setText("DOWNLOADED");

                // Show success message
                Toast.makeText(this, "Ticket saved to gallery!", Toast.LENGTH_LONG).show();
            } else {
                Log.e(TAG, "Failed to compress bitmap.");
                Toast.makeText(this, "Failed to save image.", Toast.LENGTH_SHORT).show();

                // Restore button visibility and reset text on error
                downloadButton.setVisibility(View.VISIBLE);
                updateDownloadButtonState();
            }
        } catch (Exception e) {
            Log.e(TAG, "Error while saving PNG: " + e.getMessage(), e);
            Toast.makeText(this, "Failed to save image.", Toast.LENGTH_SHORT).show();

            // Restore button visibility and reset text on error
            downloadButton.setVisibility(View.VISIBLE);
            updateDownloadButtonState();
        }
    }


    // Method to get only the ticket bitmap with transparent background and preserve cutout design
    private Bitmap getTicketBitmap() {
        if (mdfTicketCardView == null) {
            Log.e(TAG, "mdfTicketCardView is null");
            return null;
        }

        // Find circular cutout views to get their dimensions and positions
        View firstCircle = findViewById(R.id.first_circle);
        View secondCircle = findViewById(R.id.second_circle);

        // Step 1: Create a bitmap with transparent background
        Bitmap bitmap = Bitmap.createBitmap(
                mdfTicketCardView.getWidth(),
                mdfTicketCardView.getHeight(),
                Bitmap.Config.ARGB_8888);
        Canvas canvas = new Canvas(bitmap);

        // Step 2: Create the ticket path with circular cutouts
        Path ticketPath = new Path();

        // Get the bounds of the card view - this will be our ticket outline
        RectF ticketBounds = new RectF(
                0,
                0,
                mdfTicketCardView.getWidth(),
                mdfTicketCardView.getHeight());

        // Add the rectangular shape of the ticket
        ticketPath.addRect(ticketBounds, Path.Direction.CW);

        // Now subtract the circular cutouts while preserving their positions
        if (firstCircle != null && firstCircle.getVisibility() == View.VISIBLE) {
            int[] parentLocation = new int[2];
            int[] circleLocation = new int[2];

            mdfTicketCardView.getLocationOnScreen(parentLocation);
            firstCircle.getLocationOnScreen(circleLocation);

            // Calculate relative position of the circle
            int relativeLeft = circleLocation[0] - parentLocation[0];
            int relativeTop = circleLocation[1] - parentLocation[1];

            // Get circle radius (assumes the view is circular)
            float radius = Math.min(firstCircle.getWidth(), firstCircle.getHeight()) / 2f;

            // Create a circular path for cutout and add it with DIFFERENCE operation
            Path circlePath = new Path();
            circlePath.addCircle(
                    relativeLeft + radius,  // center X
                    relativeTop + radius,   // center Y
                    radius,                 // radius
                    Path.Direction.CW);

            // Subtract this circle from the main path
            ticketPath.op(circlePath, Path.Op.DIFFERENCE);
        }

        // Repeat for the second circle cutout
        if (secondCircle != null && secondCircle.getVisibility() == View.VISIBLE) {
            int[] parentLocation = new int[2];
            int[] circleLocation = new int[2];

            mdfTicketCardView.getLocationOnScreen(parentLocation);
            secondCircle.getLocationOnScreen(circleLocation);

            // Calculate relative position
            int relativeLeft = circleLocation[0] - parentLocation[0];
            int relativeTop = circleLocation[1] - parentLocation[1];

            // Get circle radius
            float radius = Math.min(secondCircle.getWidth(), secondCircle.getHeight()) / 2f;

            // Create circular path for cutout
            Path circlePath = new Path();
            circlePath.addCircle(
                    relativeLeft + radius,  // center X
                    relativeTop + radius,   // center Y
                    radius,                 // radius
                    Path.Direction.CW);

            // Subtract this circle from the main path
            ticketPath.op(circlePath, Path.Op.DIFFERENCE);
        }

        // Step 3: Create a clip path to only draw inside our ticket outline with cutouts
        canvas.clipPath(ticketPath);

        // Step 4: Draw the card view content through the clip path
        mdfTicketCardView.draw(canvas);

        Log.d(TAG, "Captured ticket bitmap with cutout design: " + (bitmap != null ? "success" : "failure"));
        return bitmap;
    }

    // Method to format time to "1:00pm" or "1:00am"
    private String formatTime(String timeStr) {
        try {
            SimpleDateFormat inputFormat = new SimpleDateFormat("HH:mm", Locale.getDefault()); // 24-hour format
            SimpleDateFormat outputFormat = new SimpleDateFormat("h:mm a", Locale.getDefault()); // 12-hour format
            return outputFormat.format(inputFormat.parse(timeStr));
        } catch (ParseException e) {
            e.printStackTrace();
            return timeStr; // Return the original time string if formatting fails
        }
    }

    // Method to get day of the week from a given date
    private String getDayOfWeek(String dateStr) {
        SimpleDateFormat sdf = new SimpleDateFormat("yyyy-MM-dd", Locale.getDefault());
        try {
            Calendar calendar = Calendar.getInstance();
            calendar.setTime(sdf.parse(dateStr));

            // Format to short day name (e.g., Sat)
            return new SimpleDateFormat("E", Locale.getDefault()).format(calendar.getTime());
        } catch (ParseException e) {
            e.printStackTrace();
            return ""; // Return empty if parsing fails
        }
    }
}