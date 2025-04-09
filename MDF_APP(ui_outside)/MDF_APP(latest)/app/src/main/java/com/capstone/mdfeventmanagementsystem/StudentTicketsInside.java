package com.capstone.mdfeventmanagementsystem;


import android.app.AlertDialog;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.graphics.Bitmap;
import android.graphics.Canvas;
import android.graphics.pdf.PdfDocument;
import android.os.Bundle;
import android.os.Environment;
import android.util.Log;
import android.view.View;
import android.widget.Button;
import android.widget.ImageView;
import android.widget.ScrollView;
import android.widget.TextView;
import android.widget.Toast;

import androidx.appcompat.app.AppCompatActivity;
import androidx.core.app.ActivityCompat;
import androidx.core.content.ContextCompat;

import com.bumptech.glide.Glide;

import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.Calendar;
import java.util.Date;
import java.util.Locale;

public class StudentTicketsInside extends AppCompatActivity {

    private ScrollView ticketLayout; // Main content layout
    private TextView eventName, startDate, endDate, startTime, endTime, graceTime, eventSpan, venue, eventDescription, dayText;
    private ImageView qrCodeImage;
    private static final int STORAGE_PERMISSION_CODE = 100;
    private static final String TAG = "StudentTicketsInside"; // Tag for logging

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_student_tickets_inside);

        // Initialize UI elements
        ticketLayout = findViewById(R.id.ticketLayout); // Main ticket view
        Button downloadButton = findViewById(R.id.downloadButton);
        eventName = findViewById(R.id.eventName);
        startDate = findViewById(R.id.startDate);
        endDate = findViewById(R.id.endDate);
        graceTime = findViewById(R.id.graceTime);
        eventSpan = findViewById(R.id.eventSpan);
        venue = findViewById(R.id.venue);
        eventDescription = findViewById(R.id.eventDescription);
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
        String eventDescriptionText = intent.getStringExtra("eventDescription");
        String qrCodeUrl = intent.getStringExtra("qrCodeUrl");

        // Set data to UI
        eventName.setText(eventNameText);
        graceTime.setText(graceTimeText);
        eventSpan.setText(eventSpanText);
        venue.setText(venueText);
        eventDescription.setText(eventDescriptionText);

        // Log eventSpan value to check if it's received correctly
        Log.d("Event Span", "Event Span: " + eventSpanText);

        // Check eventSpan to decide whether to show only the start date or both start and end date
        if ("single-day".equals(eventSpanText)) {
            startDate.setText(startDateText);
            endDate.setVisibility(View.GONE); // Hide endDate
        } else if ("multi-day".equals(eventSpanText)) {
            startDate.setText(startDateText);
            if (endDateText != null && !endDateText.isEmpty()) {
                endDate.setText(endDateText);
                endDate.setVisibility(View.VISIBLE); // Ensure endDate is visible
            } else {
                endDate.setVisibility(View.GONE); // Hide endDate if it's not provided
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

        // Set up download button functionality
        downloadButton.setOnClickListener(v -> {
            Log.d(TAG, "Download button clicked.");
            if (checkStoragePermission()) {
                showFormatDialog();
            } else {
                Log.d(TAG, "Storage permission not granted, requesting permission.");
                requestStoragePermission();
            }
        });
    }

    // Check if storage permission is granted
    private boolean checkStoragePermission() {
        boolean permissionGranted = ContextCompat.checkSelfPermission(this, android.Manifest.permission.WRITE_EXTERNAL_STORAGE) == PackageManager.PERMISSION_GRANTED;
        Log.d(TAG, "Storage permission check: " + permissionGranted);
        return permissionGranted;
    }

    // Request storage permission
    private void requestStoragePermission() {
        Log.d(TAG, "Requesting storage permission.");
        ActivityCompat.requestPermissions(this, new String[]{android.Manifest.permission.WRITE_EXTERNAL_STORAGE}, STORAGE_PERMISSION_CODE);
    }

    // Handle the result of the permission request
    @Override
    public void onRequestPermissionsResult(int requestCode, String[] permissions, int[] grantResults) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults);
        if (requestCode == STORAGE_PERMISSION_CODE) {
            if (grantResults.length > 0 && grantResults[0] == PackageManager.PERMISSION_GRANTED) {
                Log.d(TAG, "Storage permission granted.");
                showFormatDialog();
            } else {
                Log.d(TAG, "Storage permission denied.");
                Toast.makeText(this, "Permission denied. Unable to save ticket.", Toast.LENGTH_SHORT).show();
            }
        }
    }

    private void showFormatDialog() {
        Log.d(TAG, "Displaying format dialog.");
        AlertDialog.Builder builder = new AlertDialog.Builder(this);
        builder.setTitle("Choose File Format")
                .setItems(new CharSequence[]{"JPG", "PDF"}, (dialog, which) -> {
                    Log.d(TAG, "Format chosen: " + (which == 0 ? "JPG" : "PDF"));
                    if (which == 0) {
                        saveAsImage();
                    } else {
                        saveAsPDF();
                    }
                })
                .show();
    }

    private void saveAsImage() {
        Log.d(TAG, "Saving as JPG.");
        Bitmap bitmap = getBitmapFromView(ticketLayout);
        if (bitmap == null) {
            Log.e(TAG, "Failed to capture ticket as image.");
            Toast.makeText(this, "Failed to capture ticket.", Toast.LENGTH_SHORT).show();
            return;
        }

        String timeStamp = new SimpleDateFormat("yyyyMMdd_HHmmss", Locale.getDefault()).format(new Date());
        String fileName = "StudentTicket_" + timeStamp + ".jpg";

        File directory = new File(Environment.getExternalStoragePublicDirectory(Environment.DIRECTORY_DOWNLOADS), "EventTickets");
        if (!directory.exists()) {
            if (directory.mkdirs()) {
                Log.d(TAG, "Created directory: " + directory.getAbsolutePath());
            } else {
                Log.e(TAG, "Failed to create directory.");
            }
        }

        File file = new File(directory, fileName);
        try (FileOutputStream fos = new FileOutputStream(file)) {
            if (bitmap.compress(Bitmap.CompressFormat.JPEG, 100, fos)) {
                Log.d(TAG, "Ticket saved as JPG: " + file.getAbsolutePath());
                Toast.makeText(this, "Ticket saved as JPG: " + file.getAbsolutePath(), Toast.LENGTH_LONG).show();
            } else {
                Log.e(TAG, "Failed to compress bitmap.");
                Toast.makeText(this, "Failed to save JPG.", Toast.LENGTH_SHORT).show();
            }
        } catch (Exception e) {
            Log.e(TAG, "Error while saving JPG: " + e.getMessage(), e);
            Toast.makeText(this, "Failed to save JPG.", Toast.LENGTH_SHORT).show();
        }
    }

    private void saveAsPDF() {
        Log.d(TAG, "Saving as PDF.");
        Bitmap bitmap = getBitmapFromView(ticketLayout);
        if (bitmap == null) {
            Log.e(TAG, "Failed to capture ticket as image.");
            Toast.makeText(this, "Failed to capture ticket.", Toast.LENGTH_SHORT).show();
            return;
        }

        String timeStamp = new SimpleDateFormat("yyyyMMdd_HHmmss", Locale.getDefault()).format(new Date());
        String fileName = "StudentTicket_" + timeStamp + ".pdf";

        File directory = new File(Environment.getExternalStoragePublicDirectory(Environment.DIRECTORY_DOWNLOADS), "EventTickets");
        if (!directory.exists()) {
            if (directory.mkdirs()) {
                Log.d(TAG, "Created directory: " + directory.getAbsolutePath());
            } else {
                Log.e(TAG, "Failed to create directory.");
            }
        }

        File file = new File(directory, fileName);

        try (FileOutputStream fos = new FileOutputStream(file)) {
            PdfDocument pdfDocument = new PdfDocument();
            PdfDocument.PageInfo pageInfo = new PdfDocument.PageInfo.Builder(bitmap.getWidth(), bitmap.getHeight(), 1).create();
            PdfDocument.Page page = pdfDocument.startPage(pageInfo);

            Canvas canvas = page.getCanvas();
            canvas.drawBitmap(bitmap, 0, 0, null);
            pdfDocument.finishPage(page);

            pdfDocument.writeTo(fos);
            pdfDocument.close();
            Log.d(TAG, "Ticket saved as PDF: " + file.getAbsolutePath());
            Toast.makeText(this, "Ticket saved as PDF: " + file.getAbsolutePath(), Toast.LENGTH_LONG).show();
        } catch (IOException e) {
            Log.e(TAG, "Error while saving PDF: " + e.getMessage(), e);
            Toast.makeText(this, "Failed to save PDF.", Toast.LENGTH_SHORT).show();
        }
    }

    private Bitmap getBitmapFromView(View view) {
        Bitmap bitmap = Bitmap.createBitmap(view.getWidth(), view.getHeight(), Bitmap.Config.ARGB_8888);
        Canvas canvas = new Canvas(bitmap);
        view.draw(canvas);
        Log.d(TAG, "Captured bitmap from view: " + (bitmap != null ? "success" : "failure"));
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
