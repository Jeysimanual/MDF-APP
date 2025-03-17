package com.capstone.mdfeventmanagementsystem;

import android.content.Context;
import android.content.SharedPreferences;
import android.graphics.Bitmap;
import android.util.Log;

import androidx.annotation.NonNull;

import com.google.firebase.database.DataSnapshot;
import com.google.firebase.database.DatabaseError;
import com.google.firebase.database.DatabaseReference;
import com.google.firebase.database.FirebaseDatabase;
import com.google.firebase.database.ValueEventListener;
import com.google.firebase.storage.FirebaseStorage;
import com.google.firebase.storage.StorageReference;
import com.google.firebase.storage.UploadTask;
import com.google.zxing.BarcodeFormat;
import com.google.zxing.EncodeHintType;
import com.google.zxing.WriterException;
import com.google.zxing.common.BitMatrix;
import com.google.zxing.qrcode.QRCodeWriter;

import java.io.ByteArrayOutputStream;
import java.util.Calendar;
import java.util.HashMap;
import java.util.Map;
import java.util.Random;

public class QrCodeGenerator {

    public interface OnQRCodeGeneratedListener {
        void onQRCodeGenerated(Bitmap qrCodeBitmap);
        void onQRCodeUploaded(String downloadUrl);
        void onError(String errorMessage);
    }

    public static void generateQRCodeWithEventAndStudentInfo(Context context, final String eventUID, final OnQRCodeGeneratedListener listener) {
        SharedPreferences sharedPreferences = context.getSharedPreferences("MyAppPrefs", Context.MODE_PRIVATE);
        String studentID = sharedPreferences.getString("studentID", null);

        if (studentID == null) {
            listener.onError("Student ID not found in SharedPreferences.");
            return;
        }

        DatabaseReference studentRef = FirebaseDatabase.getInstance().getReference("students").child(studentID);
        studentRef.addListenerForSingleValueEvent(new ValueEventListener() {
            @Override
            public void onDataChange(DataSnapshot studentSnapshot) {
                if (studentSnapshot.exists()) {
                    String firstName = studentSnapshot.child("firstName").getValue(String.class);
                    String lastName = studentSnapshot.child("lastName").getValue(String.class);
                    String yearLevel = studentSnapshot.child("yearLevel").getValue(String.class);
                    String section = studentSnapshot.child("section").getValue(String.class);

                    DatabaseReference eventRef = FirebaseDatabase.getInstance().getReference("events").child(eventUID);
                    eventRef.addListenerForSingleValueEvent(new ValueEventListener() {
                        @Override
                        public void onDataChange(DataSnapshot eventSnapshot) {
                            if (eventSnapshot.exists()) {
                                String eventName = eventSnapshot.child("eventName").getValue(String.class);
                                String startDate = eventSnapshot.child("startDate").getValue(String.class);
                                String startTime = eventSnapshot.child("startTime").getValue(String.class);
                                String endDate = eventSnapshot.child("endDate").getValue(String.class);
                                String endTime = eventSnapshot.child("endTime").getValue(String.class);
                                String eventSpan = eventSnapshot.child("eventSpan").getValue(String.class);
                                String graceTime = eventSnapshot.child("graceTime").getValue(String.class);

                                generateUniqueTicketID(eventUID, studentID, new OnTicketIDGeneratedListener() {
                                    @Override
                                    public void onTicketIDGenerated(String ticketID) {
                                        Map<String, String> qrData = new HashMap<>();
                                        qrData.put("ticketID", ticketID);
                                        qrData.put("eventUID", eventUID);
                                        qrData.put("eventName", eventName);
                                        qrData.put("startDate", startDate);
                                        qrData.put("startTime", startTime);
                                        qrData.put("endDate", endDate);
                                        qrData.put("endTime", endTime);
                                        qrData.put("eventSpan", eventSpan);
                                        qrData.put("graceTime", graceTime);
                                        qrData.put("studentID", studentID);
                                        qrData.put("firstName", firstName);
                                        qrData.put("lastName", lastName);
                                        qrData.put("yearLevel", yearLevel);
                                        qrData.put("section", section);

                                        String qrContent = qrData.toString();
                                        try {
                                            Bitmap qrCodeBitmap = generateQRCodeBitmap(qrContent);
                                            uploadQRCodeToFirebase(qrCodeBitmap, eventName, ticketID, listener);
                                        } catch (WriterException e) {
                                            listener.onError("Error generating QR code: " + e.getMessage());
                                        }
                                    }

                                    @Override
                                    public void onError(String errorMessage) {
                                        listener.onError(errorMessage);
                                    }
                                });

                            } else {
                                listener.onError("Event not found.");
                            }
                        }

                        @Override
                        public void onCancelled(@NonNull DatabaseError error) {
                            listener.onError("Failed to fetch event data.");
                        }
                    });
                } else {
                    listener.onError("Student data not found.");
                }
            }

            @Override
            public void onCancelled(@NonNull DatabaseError error) {
                listener.onError("Failed to fetch student data.");
            }
        });
    }

    private static void uploadQRCodeToFirebase(Bitmap qrCodeBitmap, String eventName, String ticketID, OnQRCodeGeneratedListener listener) {
        FirebaseStorage storage = FirebaseStorage.getInstance();
        StorageReference storageRef = storage.getReference();

        // Sanitize event name to avoid issues in Firebase Storage
        String sanitizedEventName = eventName.replaceAll("\\s+", "_").replaceAll("[^a-zA-Z0-9_]", "");
        String fileName = "QRCode_" + ticketID + ".png"; // File naming format

        // Correct storage path: /[eventName]/eventQr/QRCode_[ticketID].png
        StorageReference qrCodeRef = storageRef.child(sanitizedEventName + "/eventQr/" + fileName);

        // Convert bitmap to byte array
        ByteArrayOutputStream baos = new ByteArrayOutputStream();
        qrCodeBitmap.compress(Bitmap.CompressFormat.PNG, 100, baos);
        byte[] qrData = baos.toByteArray();

        // Upload the QR code image
        UploadTask uploadTask = qrCodeRef.putBytes(qrData);
        uploadTask.addOnSuccessListener(taskSnapshot ->
                qrCodeRef.getDownloadUrl().addOnSuccessListener(uri ->
                        listener.onQRCodeUploaded(uri.toString())
                ).addOnFailureListener(e ->
                        listener.onError("Failed to get download URL: " + e.getMessage())
                )
        ).addOnFailureListener(e ->
                listener.onError("QR Code upload failed: " + e.getMessage())
        );
    }


    public interface OnTicketIDGeneratedListener {
        void onTicketIDGenerated(String ticketID);
        void onError(String errorMessage);
    }

    private static void generateUniqueTicketID(String eventUID, String studentID, OnTicketIDGeneratedListener listener) {
        String year = String.valueOf(Calendar.getInstance().get(Calendar.YEAR));
        String randomPart = generateRandomAlphanumeric(4);
        String ticketID = year + randomPart;

        DatabaseReference ticketRef = FirebaseDatabase.getInstance().getReference("tickets").child(ticketID);

        ticketRef.addListenerForSingleValueEvent(new ValueEventListener() {
            @Override
            public void onDataChange(@NonNull DataSnapshot snapshot) {
                if (snapshot.exists()) {
                    generateUniqueTicketID(eventUID, studentID, listener);
                } else {
                    ticketRef.setValue(true);
                    listener.onTicketIDGenerated(ticketID);
                }
            }

            @Override
            public void onCancelled(@NonNull DatabaseError error) {
                listener.onError("Failed to check ticket ID uniqueness.");
            }
        });
    }

    private static Bitmap generateQRCodeBitmap(String content) throws WriterException {
        QRCodeWriter writer = new QRCodeWriter();
        Map<EncodeHintType, Object> hints = new HashMap<>();
        hints.put(EncodeHintType.MARGIN, 1);
        BitMatrix bitMatrix = writer.encode(content, BarcodeFormat.QR_CODE, 512, 512, hints);
        int width = bitMatrix.getWidth();
        int height = bitMatrix.getHeight();
        Bitmap bitmap = Bitmap.createBitmap(width, height, Bitmap.Config.RGB_565);

        for (int x = 0; x < width; x++) {
            for (int y = 0; y < height; y++) {
                bitmap.setPixel(x, y, bitMatrix.get(x, y) ? 0xFF000000 : 0xFFFFFFFF);
            }
        }
        return bitmap;
    }

    private static String generateRandomAlphanumeric(int length) {
        String chars = "ABCDEFGHIJKLMNOPQRSTUVWXYZ0123456789";
        StringBuilder randomStr = new StringBuilder();
        Random random = new Random();

        for (int i = 0; i < length; i++) {
            randomStr.append(chars.charAt(random.nextInt(chars.length())));
        }
        return randomStr.toString();
    }
}
