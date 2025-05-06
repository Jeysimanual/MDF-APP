package com.capstone.mdfeventmanagementsystem.Student;

import android.content.Intent;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.net.Uri;
import android.os.Bundle;
import android.provider.MediaStore;
import android.util.Log;
import android.widget.ImageView;
import android.widget.TextView;
import android.widget.Toast;

import androidx.activity.EdgeToEdge;
import androidx.activity.result.ActivityResultLauncher;
import androidx.activity.result.contract.ActivityResultContracts;
import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.graphics.Insets;
import androidx.core.view.ViewCompat;
import androidx.core.view.WindowInsetsCompat;

import com.capstone.mdfeventmanagementsystem.R;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseUser;
import com.google.firebase.database.DataSnapshot;
import com.google.firebase.database.DatabaseError;
import com.google.firebase.database.DatabaseReference;
import com.google.firebase.database.FirebaseDatabase;
import com.google.firebase.database.ServerValue;
import com.google.firebase.database.ValueEventListener;
import com.google.firebase.storage.FirebaseStorage;
import com.google.firebase.storage.StorageReference;
import com.squareup.picasso.Picasso;
import com.squareup.picasso.Transformation;

import java.io.FileNotFoundException;
import java.io.InputStream;
import java.util.HashMap;
import java.util.Map;

// Circular image transformation class
class CircleTransform implements Transformation {
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

public class MyInformation extends AppCompatActivity {

    private static final String TAG = "MyInformation";

    // UI Elements
    private TextView userFullname, userEmail;
    private TextView firstName, lastName, idNumber, email, yearLevel, section;
    private ImageView profileImageView;
    private ImageView add_profile;

    // Firebase
    private FirebaseAuth mAuth;
    private DatabaseReference studentsRef;
    private StorageReference storageRef;
    private String currentUserId;

    // For image persistence
    private boolean imageUploading = false;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        EdgeToEdge.enable(this);
        setContentView(R.layout.activity_my_information);

        ViewCompat.setOnApplyWindowInsetsListener(findViewById(R.id.main), (v, insets) -> {
            Insets systemBars = insets.getInsets(WindowInsetsCompat.Type.systemBars());
            v.setPadding(systemBars.left, systemBars.top, systemBars.right, systemBars.bottom);
            return insets;
        });

        // Initialize Firebase components
        mAuth = FirebaseAuth.getInstance();
        studentsRef = FirebaseDatabase.getInstance().getReference().child("students");
        storageRef = FirebaseStorage.getInstance().getReference();

        // Log initialization for debugging
        Log.d(TAG, "Firebase initialized. Database path: " + studentsRef.toString());

        // Initialize UI elements
        initializeUI();

        // Load the current user's data
        loadCurrentUserData();
    }

    @Override
    protected void onResume() {
        super.onResume();
        // Reload user data when returning to this screen
        if (!imageUploading) {
            loadCurrentUserData();
        }
    }

    private void initializeUI() {
        try {
            // Card view at the top
            userFullname = findViewById(R.id.user_fullname);
            userEmail = findViewById(R.id.user_email);

            // Profile info section
            firstName = findViewById(R.id.firstName);
            lastName = findViewById(R.id.lastName);
            idNumber = findViewById(R.id.idNumber);
            email = findViewById(R.id.email);
            yearLevel = findViewById(R.id.yrlvl);
            section = findViewById(R.id.section);

            // Profile image
            profileImageView = findViewById(R.id.profileImageView);
            add_profile = findViewById(R.id.add_profile);

            // Profile image click listener
            add_profile.setOnClickListener(v -> {
                Intent intent = new Intent(Intent.ACTION_PICK, MediaStore.Images.Media.EXTERNAL_CONTENT_URI);
                intent.addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION);
                pickImage.launch(intent);
            });

            // Back button click listener
            ImageView btnBack = findViewById(R.id.btnBack);
            if (btnBack != null) {
                btnBack.setOnClickListener(v -> onBackPressed());
            }
        } catch (Exception e) {
            Log.e(TAG, "Error initializing UI elements: " + e.getMessage());
            Toast.makeText(this, "Error initializing UI", Toast.LENGTH_SHORT).show();
        }
    }

    private final ActivityResultLauncher<Intent> pickImage = registerForActivityResult(
            new ActivityResultContracts.StartActivityForResult(),
            result -> {
                if (result.getResultCode() == RESULT_OK && result.getData() != null) {
                    Uri imageUri = result.getData().getData();
                    try {
                        // Mark that we're uploading an image
                        imageUploading = true;

                        // Show a loading message
                        Toast.makeText(MyInformation.this, "Uploading profile image...", Toast.LENGTH_SHORT).show();

                        // Display the circular image preview while uploading
                        InputStream inputStream = getContentResolver().openInputStream(imageUri);
                        Bitmap bitmap = BitmapFactory.decodeStream(inputStream);

                        // Apply circular transformation for preview
                        Bitmap circularBitmap = new CircleTransform().transform(bitmap);
                        profileImageView.setImageBitmap(circularBitmap);

                        // Upload the image to Firebase Storage
                        uploadImageToFirebase(imageUri);

                    } catch (FileNotFoundException e) {
                        imageUploading = false;
                        Log.e(TAG, "File not found: " + e.getMessage());
                        Toast.makeText(MyInformation.this, "Error loading image", Toast.LENGTH_SHORT).show();
                    }
                }
            }
    );

    private void uploadImageToFirebase(Uri imageUri) {
        if (mAuth.getCurrentUser() == null) {
            imageUploading = false;
            Toast.makeText(this, "User not authenticated", Toast.LENGTH_SHORT).show();
            return;
        }

        currentUserId = mAuth.getCurrentUser().getUid();
        Log.d(TAG, "Starting image upload for user: " + currentUserId);

        // Create a storage reference specifically for the user_profiles collection
        StorageReference fileRef = storageRef.child("user_profiles/" + currentUserId + "/profile_image.jpg");

        // Upload file to Firebase Storage with proper error handling
        fileRef.putFile(imageUri)
                .addOnSuccessListener(taskSnapshot -> {
                    Log.d(TAG, "Image uploaded successfully to Firebase Storage");

                    // Get the download URL
                    fileRef.getDownloadUrl().addOnSuccessListener(uri -> {
                        String imageUrl = uri.toString();
                        Log.d(TAG, "Got download URL: " + imageUrl);

                        // Save URL to the new user_profiles collection
                        saveImageUrlToNewCollection(imageUrl);
                    }).addOnFailureListener(e -> {
                        imageUploading = false;
                        Log.e(TAG, "Failed to get download URL: " + e.getMessage());
                        Toast.makeText(MyInformation.this, "Failed to process uploaded image", Toast.LENGTH_SHORT).show();
                    });
                })
                .addOnFailureListener(e -> {
                    imageUploading = false;
                    Log.e(TAG, "Upload failed: " + e.getMessage());
                    Toast.makeText(MyInformation.this, "Failed to upload image", Toast.LENGTH_SHORT).show();
                })
                .addOnProgressListener(snapshot -> {
                    double progress = (100.0 * snapshot.getBytesTransferred()) / snapshot.getTotalByteCount();
                    Log.d(TAG, "Upload is " + progress + "% done");
                });
    }

    private void saveImageUrlToNewCollection(String imageUrl) {
        // Double check that we have a valid user ID
        if (currentUserId == null || currentUserId.isEmpty()) {
            if (mAuth.getCurrentUser() != null) {
                currentUserId = mAuth.getCurrentUser().getUid();
            } else {
                imageUploading = false;
                Toast.makeText(this, "Cannot save image: User not authenticated", Toast.LENGTH_SHORT).show();
                return;
            }
        }

        // Create a completely new reference to a user_profiles collection
        DatabaseReference profilesRef = FirebaseDatabase.getInstance().getReference().child("user_profiles");

        // Get current user information to store alongside the profile image
        FirebaseUser user = mAuth.getCurrentUser();
        if (user != null) {
            String email = user.getEmail() != null ? user.getEmail() : "";

            // Create a map with profile data
            Map<String, Object> profileData = new HashMap<>();
            profileData.put("profileImage", imageUrl);
            profileData.put("email", email);
            profileData.put("uid", currentUserId);
            profileData.put("updatedAt", ServerValue.TIMESTAMP);

            // Also add any available student info
            studentsRef.child(currentUserId).addListenerForSingleValueEvent(new ValueEventListener() {
                @Override
                public void onDataChange(@NonNull DataSnapshot dataSnapshot) {
                    if (dataSnapshot.exists()) {
                        // Add student info to profile data if available
                        if (dataSnapshot.hasChild("firstName")) {
                            profileData.put("firstName", dataSnapshot.child("firstName").getValue(String.class));
                        }
                        if (dataSnapshot.hasChild("lastName")) {
                            profileData.put("lastName", dataSnapshot.child("lastName").getValue(String.class));
                        }
                        if (dataSnapshot.hasChild("idNumber")) {
                            profileData.put("idNumber", dataSnapshot.child("idNumber").getValue(String.class));
                        }
                    }

                    // Save the complete profile data to the new collection
                    saveProfileData(profilesRef, profileData);
                }

                @Override
                public void onCancelled(@NonNull DatabaseError error) {
                    // Continue saving even if we can't get student data
                    saveProfileData(profilesRef, profileData);
                }
            });
        } else {
            // Minimal profile data if no user info available
            Map<String, Object> profileData = new HashMap<>();
            profileData.put("profileImage", imageUrl);
            profileData.put("uid", currentUserId);
            profileData.put("updatedAt", ServerValue.TIMESTAMP);

            saveProfileData(profilesRef, profileData);
        }
    }

    private void saveProfileData(DatabaseReference profilesRef, Map<String, Object> profileData) {
        // Save profile data to the user_profiles collection
        profilesRef.child(currentUserId).setValue(profileData)
                .addOnSuccessListener(aVoid -> {
                    imageUploading = false;
                    Log.d(TAG, "✅ Profile data successfully saved to user_profiles collection");
                    Toast.makeText(MyInformation.this, "Profile image updated successfully", Toast.LENGTH_SHORT).show();

                    // Load the image with Picasso with circular transformation
                    String imageUrl = (String) profileData.get("profileImage");
                    if (imageUrl != null && !imageUrl.isEmpty()) {
                        loadProfileImage(imageUrl);
                    }
                })
                .addOnFailureListener(e -> {
                    imageUploading = false;
                    Log.e(TAG, "Failed to save profile data: " + e.getMessage());
                    Toast.makeText(MyInformation.this, "Failed to update profile", Toast.LENGTH_SHORT).show();
                });
    }



    // Modified update function to handle both approaches
    private void updateProfileImageUrl(DatabaseReference userRef, String imageUrl) {
        Log.d(TAG, "Updating profile image URL: " + imageUrl);
        Log.d(TAG, "Database reference path: " + userRef.toString());

        // Try first approach - standard child node
        userRef.child("profileImage").setValue(imageUrl)
                .addOnSuccessListener(aVoid -> {
                    Log.d(TAG, "✅ Profile image URL successfully saved to database");
                    imageUploading = false;
                    Toast.makeText(MyInformation.this, "Profile image updated successfully", Toast.LENGTH_SHORT).show();
                    loadProfileImage(imageUrl);
                })
                .addOnFailureListener(e -> {
                    Log.e(TAG, "Failed with first approach, trying alternative: " + e.getMessage());

                    // Try second approach - direct field format like in screenshot
                    Map<String, Object> updates = new HashMap<>();
                    updates.put("profileImage", imageUrl);

                    userRef.updateChildren(updates)
                            .addOnSuccessListener(aVoid -> {
                                imageUploading = false;
                                Log.d(TAG, "✅ Profile image URL saved with updateChildren");
                                Toast.makeText(MyInformation.this, "Profile image updated successfully", Toast.LENGTH_SHORT).show();
                                loadProfileImage(imageUrl);
                            })
                            .addOnFailureListener(e2 -> {
                                imageUploading = false;
                                Log.e(TAG, "All attempts to save profile image failed: " + e2.getMessage());
                                Toast.makeText(MyInformation.this, "Failed to update profile in database", Toast.LENGTH_SHORT).show();
                            });
                });
    }

    private void loadProfileImage(String imageUrl) {
        if (imageUrl != null && !imageUrl.isEmpty()) {
            Log.d(TAG, "Loading profile image from: " + imageUrl);

            // Use Picasso to load the image with explicit no-cache policies
            Picasso.get()
                    .load(imageUrl)
                    .transform(new CircleTransform())
                    .networkPolicy(com.squareup.picasso.NetworkPolicy.NO_CACHE)
                    .memoryPolicy(com.squareup.picasso.MemoryPolicy.NO_CACHE, com.squareup.picasso.MemoryPolicy.NO_STORE)
                    .placeholder(R.drawable.profile_placeholder)
                    .error(R.drawable.profile_placeholder)
                    .into(profileImageView, new com.squareup.picasso.Callback() {
                        @Override
                        public void onSuccess() {
                            Log.d(TAG, "Profile image loaded successfully");
                        }

                        @Override
                        public void onError(Exception e) {
                            Log.e(TAG, "Error loading profile image: " + (e != null ? e.getMessage() : "unknown error"));
                            // Show placeholder on error
                            profileImageView.setImageResource(R.drawable.profile_placeholder);
                        }
                    });
        } else {
            Log.w(TAG, "No profile image URL provided");
            profileImageView.setImageResource(R.drawable.profile_placeholder);
        }
    }

    private void loadCurrentUserData() {
        FirebaseUser currentUser = mAuth.getCurrentUser();

        if (currentUser == null) {
            Toast.makeText(this, "User not authenticated!", Toast.LENGTH_SHORT).show();
            Log.e(TAG, "No user is currently logged in");
            finish();
            return;
        }

        currentUserId = currentUser.getUid();
        Log.d(TAG, "Currently logged in with UID: " + currentUserId);

        // First check if email is in students collection
        String userEmailStr = currentUser.getEmail();
        if (userEmailStr != null) {
            findStudentByEmail(userEmailStr, currentUserId);
        } else {
            // Fallback to direct UID lookup
            findStudentByUid(currentUserId);
        }

        // Also check the user_profiles collection for profile image
        checkUserProfiles(currentUserId);
    }

    private void checkUserProfiles(String uid) {
        DatabaseReference profilesRef = FirebaseDatabase.getInstance().getReference().child("user_profiles").child(uid);

        profilesRef.addListenerForSingleValueEvent(new ValueEventListener() {
            @Override
            public void onDataChange(@NonNull DataSnapshot dataSnapshot) {
                if (dataSnapshot.exists() && dataSnapshot.hasChild("profileImage")) {
                    String profileImageUrl = dataSnapshot.child("profileImage").getValue(String.class);
                    if (profileImageUrl != null && !profileImageUrl.isEmpty()) {
                        Log.d(TAG, "Found profile image in user_profiles collection: " + profileImageUrl);
                        loadProfileImage(profileImageUrl);
                    }
                }
            }

            @Override
            public void onCancelled(@NonNull DatabaseError error) {
                Log.e(TAG, "Error checking user_profiles: " + error.getMessage());
            }
        });
    }

    private void findStudentByEmail(String email, String fallbackUid) {
        studentsRef.orderByChild("email").equalTo(email).addListenerForSingleValueEvent(new ValueEventListener() {
            @Override
            public void onDataChange(@NonNull DataSnapshot dataSnapshot) {
                if (dataSnapshot.exists()) {
                    // Found student with matching email
                    for (DataSnapshot studentSnapshot : dataSnapshot.getChildren()) {
                        // Get the student's UID from the database
                        String studentUid = studentSnapshot.getKey();
                        if (studentUid != null) {
                            currentUserId = studentUid; // Update the current user ID
                            loadStudentData(studentUid);
                            return;
                        }
                    }
                }
                // If we get here, no student was found with matching email
                findStudentByUid(fallbackUid);
            }

            @Override
            public void onCancelled(@NonNull DatabaseError error) {
                Log.e(TAG, "Database Error while searching by email: " + error.getMessage());
                findStudentByUid(fallbackUid);
            }
        });
    }

    private void findStudentByUid(String uid) {
        studentsRef.child(uid).addListenerForSingleValueEvent(new ValueEventListener() {
            @Override
            public void onDataChange(@NonNull DataSnapshot dataSnapshot) {
                if (dataSnapshot.exists()) {
                    // Load data for this UID
                    loadStudentData(uid);
                } else {
                    Log.w(TAG, "No student record found for UID: " + uid);
                    Toast.makeText(MyInformation.this, "Your student profile was not found", Toast.LENGTH_LONG).show();

                    // Display empty profile with current authenticated user's email
                    FirebaseUser user = mAuth.getCurrentUser();
                    if (user != null && user.getEmail() != null) {
                        safeSetText(email, user.getEmail());
                        safeSetText(userEmail, user.getEmail());
                    }
                }
            }

            @Override
            public void onCancelled(@NonNull DatabaseError error) {
                Log.e(TAG, "Database Error: " + error.getMessage());
                Toast.makeText(MyInformation.this, "Database Error: " + error.getMessage(), Toast.LENGTH_SHORT).show();
            }
        });
    }

    private void loadStudentData(String uid) {
        Log.d(TAG, "Loading student data for UID: " + uid);

        studentsRef.child(uid).addListenerForSingleValueEvent(new ValueEventListener() {
            @Override
            public void onDataChange(@NonNull DataSnapshot dataSnapshot) {
                try {
                    if (dataSnapshot.exists()) {
                        Log.d(TAG, "Student data found!");

                        // Get student data from Firebase
                        String firstNameText = safeGetString(dataSnapshot, "firstName");
                        String lastNameText = safeGetString(dataSnapshot, "lastName");
                        String middleNameText = safeGetString(dataSnapshot, "middleName");
                        String emailText = safeGetString(dataSnapshot, "email");
                        String idNumberText = safeGetString(dataSnapshot, "idNumber");
                        String yearLevelText = safeGetString(dataSnapshot, "yearLevel");
                        String sectionText = safeGetString(dataSnapshot, "section");

                        // If your database structure uses different field names (based on your screenshot)
                        if (firstNameText.equals("--")) {
                            firstNameText = safeGetString(dataSnapshot, "firstname");
                        }

                        if (lastNameText.equals("--")) {
                            lastNameText = safeGetString(dataSnapshot, "lastname");
                        }

                        if (middleNameText.equals("--")) {
                            middleNameText = safeGetString(dataSnapshot, "middleName");
                        }

                        if (idNumberText.equals("--")) {
                            idNumberText = safeGetString(dataSnapshot, "idNumber");
                        }

                        if (yearLevelText.equals("--")) {
                            yearLevelText = safeGetString(dataSnapshot, "yearLevel");
                        }

                        if (sectionText.equals("--")) {
                            sectionText = safeGetString(dataSnapshot, "section");
                        }

                        // Set data to UI elements
                        safeSetText(firstName, firstNameText);
                        safeSetText(lastName, lastNameText);
                        safeSetText(email, emailText);
                        safeSetText(userEmail, emailText);
                        safeSetText(idNumber, idNumberText);
                        safeSetText(yearLevel, yearLevelText);
                        safeSetText(section, sectionText);

                        // Set full name on top card
                        String fullName = (firstNameText != null ? firstNameText : "") + " " +
                                (middleNameText != null && !middleNameText.equals("--") ? middleNameText + " " : "") +
                                (lastNameText != null ? lastNameText : "");
                        safeSetText(userFullname, fullName.trim());

                        // IMPORTANT: Load profile image if available
                        if (dataSnapshot.hasChild("profileImage")) {
                            String profileImageUrl = dataSnapshot.child("profileImage").getValue(String.class);
                            if (profileImageUrl != null && !profileImageUrl.isEmpty()) {
                                Log.d(TAG, "Found profile image in database: " + profileImageUrl);
                                loadProfileImage(profileImageUrl);
                            } else {
                                Log.d(TAG, "Profile image field exists but is empty");
                                profileImageView.setImageResource(R.drawable.profile_placeholder);
                            }
                        } else {
                            Log.d(TAG, "No profile image field in database");
                            profileImageView.setImageResource(R.drawable.profile_placeholder);
                        }
                    } else {
                        Log.w(TAG, "Student data not found for UID: " + uid);
                        Toast.makeText(MyInformation.this, "Student information not found!", Toast.LENGTH_SHORT).show();
                    }
                } catch (Exception e) {
                    Log.e(TAG, "Error processing data: " + e.getMessage());
                    Toast.makeText(MyInformation.this, "Error processing student data", Toast.LENGTH_SHORT).show();
                }
            }

            @Override
            public void onCancelled(@NonNull DatabaseError databaseError) {
                Log.e(TAG, "Database Error: " + databaseError.getMessage());
                Toast.makeText(MyInformation.this, "Database Error: " + databaseError.getMessage(), Toast.LENGTH_SHORT).show();
            }
        });
    }

    // Helper method to safely get string from DataSnapshot
    private String safeGetString(DataSnapshot dataSnapshot, String key) {
        if (dataSnapshot.hasChild(key)) {
            Object value = dataSnapshot.child(key).getValue();
            return value != null ? value.toString() : "--";
        }
        return "--";
    }

    // Helper method to safely set text on TextViews
    private void safeSetText(TextView textView, String value) {
        if (textView != null) {
            textView.setText(value != null && !value.isEmpty() && !value.equals("null") ? value : "--");
        } else {
            Log.e(TAG, "Attempted to set text on null TextView");
        }
    }
}