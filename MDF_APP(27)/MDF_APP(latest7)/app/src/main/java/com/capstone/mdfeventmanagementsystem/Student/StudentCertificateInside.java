package com.capstone.mdfeventmanagementsystem.Student;

import android.Manifest;
import android.content.ContentValues;
import android.content.SharedPreferences;
import android.graphics.Bitmap;
import android.graphics.Canvas;
import android.graphics.Paint;
import android.graphics.pdf.PdfDocument;
import android.net.Uri;
import android.os.Bundle;
import android.os.Environment;
import android.provider.MediaStore;
import android.util.Log;
import android.view.View;
import android.widget.Button;
import android.widget.ImageView;
import android.widget.TextView;
import android.widget.Toast;

import androidx.appcompat.app.AppCompatActivity;

import com.bumptech.glide.Glide;
import com.bumptech.glide.request.target.CustomTarget;
import com.bumptech.glide.request.transition.Transition;
import com.capstone.mdfeventmanagementsystem.R;
import com.capstone.mdfeventmanagementsystem.Utilities.BaseActivity;
import com.google.firebase.database.DataSnapshot;
import com.google.firebase.database.DatabaseError;
import com.google.firebase.database.DatabaseReference;
import com.google.firebase.database.FirebaseDatabase;
import com.google.firebase.database.ValueEventListener;

import java.io.FileOutputStream;
import java.io.OutputStream;

public class StudentCertificateInside extends BaseActivity {

    private static final String TAG = "StudentCertInside";
    private ImageView certificateImageView;
    private TextView templateNameTextView;
    private Button downloadButton;
    private String previewImageUrl;
    private String templateName;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_student_certificate_inside);

        // Initialize views
        certificateImageView = findViewById(R.id.certificateImageView);
        templateNameTextView = findViewById(R.id.templateNameTextView);
        downloadButton = findViewById(R.id.downloadCert);

        // Get certificate key from intent
        String certificateKey = getIntent().getStringExtra("certificateKey");
        if (certificateKey == null || certificateKey.isEmpty()) {
            Log.e(TAG, "No certificate key provided.");
            finish();
            return;
        }

        // Get student ID from SharedPreferences
        SharedPreferences sharedPreferences = getSharedPreferences("UserSession", MODE_PRIVATE);
        String studentID = sharedPreferences.getString("studentID", null);
        if (studentID == null) {
            Log.e(TAG, "No studentID found in SharedPreferences.");
            finish();
            return;
        }

        // Firebase reference
        DatabaseReference certRef = FirebaseDatabase.getInstance()
                .getReference("students")
                .child(studentID)
                .child("certificates")
                .child(certificateKey);

        certRef.addListenerForSingleValueEvent(new ValueEventListener() {
            @Override
            public void onDataChange(DataSnapshot snapshot) {
                if (!snapshot.exists()) {
                    Log.e(TAG, "Certificate not found.");
                    return;
                }

                templateName = snapshot.child("templateName").getValue(String.class);
                previewImageUrl = snapshot.child("previewImageUrl").getValue(String.class);

                templateNameTextView.setText(templateName != null ? templateName : "Certificate");

                if (previewImageUrl != null && !previewImageUrl.isEmpty()) {
                    Glide.with(StudentCertificateInside.this)
                            .load(previewImageUrl)
                            .placeholder(R.drawable.cert_nav)
                            .error(R.drawable.cert_nav)
                            .into(certificateImageView);
                } else {
                    Log.e(TAG, "PreviewImageUrl is missing.");
                    certificateImageView.setImageResource(R.drawable.cert_nav);
                }
            }

            @Override
            public void onCancelled(DatabaseError error) {
                Log.e(TAG, "Database error: " + error.getMessage());
            }
        });

        // Set up download button
        downloadButton.setOnClickListener(v -> downloadCertificateAsPDF());
    }

    private void downloadCertificateAsPDF() {
        if (previewImageUrl == null || previewImageUrl.isEmpty()) {
            Toast.makeText(this, "Image URL is empty", Toast.LENGTH_SHORT).show();
            return;
        }

        Glide.with(this)
                .asBitmap()
                .load(previewImageUrl)
                .into(new CustomTarget<Bitmap>() {
                    @Override
                    public void onResourceReady(Bitmap bitmap, Transition<? super Bitmap> transition) {
                        try {
                            PdfDocument pdfDocument = new PdfDocument();
                            PdfDocument.PageInfo pageInfo = new PdfDocument.PageInfo.Builder(bitmap.getWidth(), bitmap.getHeight(), 1).create();
                            PdfDocument.Page page = pdfDocument.startPage(pageInfo);

                            Canvas canvas = page.getCanvas();
                            canvas.drawBitmap(bitmap, 0, 0, new Paint());
                            pdfDocument.finishPage(page);

                            String fileName = (templateName != null ? templateName : "certificate") + ".pdf";

                            // Save to Downloads
                            ContentValues values = new ContentValues();
                            values.put(MediaStore.MediaColumns.DISPLAY_NAME, fileName);
                            values.put(MediaStore.MediaColumns.MIME_TYPE, "application/pdf");
                            values.put(MediaStore.MediaColumns.RELATIVE_PATH, Environment.DIRECTORY_DOWNLOADS);

                            Uri uri = getContentResolver().insert(MediaStore.Downloads.EXTERNAL_CONTENT_URI, values);
                            if (uri != null) {
                                OutputStream outputStream = getContentResolver().openOutputStream(uri);
                                pdfDocument.writeTo(outputStream);
                                outputStream.close();
                                Toast.makeText(StudentCertificateInside.this, "Certificate downloaded!", Toast.LENGTH_SHORT).show();
                            } else {
                                Toast.makeText(StudentCertificateInside.this, "Failed to save PDF", Toast.LENGTH_SHORT).show();
                            }

                            pdfDocument.close();
                        } catch (Exception e) {
                            Log.e(TAG, "PDF generation error: " + e.getMessage());
                            Toast.makeText(StudentCertificateInside.this, "Error saving PDF", Toast.LENGTH_SHORT).show();
                        }
                    }

                    @Override
                    public void onLoadCleared(android.graphics.drawable.Drawable placeholder) {
                    }
                });
    }
}
