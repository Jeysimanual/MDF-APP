package com.capstone.mdfeventmanagementsystem.Student;

import android.content.Intent;
import android.content.SharedPreferences;
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
    private DatabaseReference profilesRef;
    private StorageReference storageRef;
    private String currentUserId;

    // For image persistence
    private boolean imageUploading = false;
    private boolean profileImageChecked = false;

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
        profilesRef = FirebaseDatabase.getInstance().getReference().child("user_profiles");
        storageRef = FirebaseStorage.getInstance().getReference();

        Log.d(TAG, "Firebase initialized. Database path: " + studentsRef.toString());

        // Initialize UI elements
        initializeUI();

        // Load cached profile image immediately
        loadCachedProfileImage();

        // Load the current user's data
        loadCurrentUserData();
    }

    @Override
    protected void onResume() {
        super.onResume();
        if (!imageUploading && !profileImageChecked) {
            loadCachedProfileImage();
            loadCurrentUserData();
        }
    }

    public void logout() {
        FirebaseAuth.getInstance().signOut();
        clearProfileImageCache();
        SharedPreferences sessionPrefs = getSharedPreferences("UserSession", MODE_PRIVATE);
        sessionPrefs.edit().clear().apply();
        Intent intent = new Intent(this, StudentLogin.class);
        startActivity(intent);
        finish();
    }

    private void initializeUI() {
        try {
            userFullname = findViewById(R.id.user_fullname);
            userEmail = findViewById(R.id.user_email);
            firstName = findViewById(R.id.firstName);
            lastName = findViewById(R.id.lastName);
            idNumber = findViewById(R.id.idNumber);
            email = findViewById(R.id.email);
            yearLevel = findViewById(R.id.yrlvl);
            section = findViewById(R.id.section);
            profileImageView = findViewById(R.id.profileImageView);
            add_profile = findViewById(R.id.add_profile);

            add_profile.setOnClickListener(v -> {
                Intent intent = new Intent(Intent.ACTION_PICK, MediaStore.Images.Media.EXTERNAL_CONTENT_URI);
                intent.addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION);
                pickImage.launch(intent);
            });

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
                        imageUploading = true;
                        Toast.makeText(this, "Uploading profile image...", Toast.LENGTH_SHORT).show();
                        InputStream inputStream = getContentResolver().openInputStream(imageUri);
                        Bitmap bitmap = BitmapFactory.decodeStream(inputStream);
                        Bitmap circularBitmap = new CircleTransform().transform(bitmap);
                        profileImageView.setImageBitmap(circularBitmap);
                        uploadImageToFirebase(imageUri);
                    } catch (FileNotFoundException e) {
                        imageUploading = false;
                        Log.e(TAG, "File not found: " + e.getMessage());
                        Toast.makeText(this, "Error loading image", Toast.LENGTH_SHORT).show();
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

        StorageReference fileRef = storageRef.child("user_profiles/" + currentUserId + "/profile_image.jpg");
        fileRef.putFile(imageUri)
                .addOnSuccessListener(taskSnapshot -> fileRef.getDownloadUrl().addOnSuccessListener(uri -> {
                    String imageUrl = uri.toString();
                    Log.d(TAG, "Got download URL: " + imageUrl);
                    saveImageUrlToNewCollection(imageUrl);
                    cacheProfileImageUrl(imageUrl);
                }).addOnFailureListener(e -> {
                    imageUploading = false;
                    Log.e(TAG, "Failed to get download URL: " + e.getMessage());
                    Toast.makeText(this, "Failed to process uploaded image", Toast.LENGTH_SHORT).show();
                }))
                .addOnFailureListener(e -> {
                    imageUploading = false;
                    Log.e(TAG, "Upload failed: " + e.getMessage());
                    Toast.makeText(this, "Failed to upload image", Toast.LENGTH_SHORT).show();
                })
                .addOnProgressListener(snapshot -> {
                    double progress = (100.0 * snapshot.getBytesTransferred()) / snapshot.getTotalByteCount();
                    Log.d(TAG, "Upload is " + progress + "% done");
                });
    }

    private void saveImageUrlToNewCollection(String imageUrl) {
        if (currentUserId == null || currentUserId.isEmpty()) {
            if (mAuth.getCurrentUser() != null) {
                currentUserId = mAuth.getCurrentUser().getUid();
            } else {
                imageUploading = false;
                Toast.makeText(this, "Cannot save image: User not authenticated", Toast.LENGTH_SHORT).show();
                return;
            }
        }

        Map<String, Object> profileData = new HashMap<>();
        profileData.put("profileImage", imageUrl);
        profileData.put("uid", currentUserId);
        profileData.put("updatedAt", ServerValue.TIMESTAMP);

        FirebaseUser user = mAuth.getCurrentUser();
        if (user != null) {
            String email = user.getEmail() != null ? user.getEmail() : "";
            profileData.put("email", email);
        }

        studentsRef.child(currentUserId).addListenerForSingleValueEvent(new ValueEventListener() {
            @Override
            public void onDataChange(@NonNull DataSnapshot dataSnapshot) {
                if (dataSnapshot.exists()) {
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
                saveProfileData(profilesRef, profileData);
            }

            @Override
            public void onCancelled(@NonNull DatabaseError error) {
                saveProfileData(profilesRef, profileData);
            }
        });
    }

    private void saveProfileData(DatabaseReference profilesRef, Map<String, Object> profileData) {
        profilesRef.child(currentUserId).setValue(profileData)
                .addOnSuccessListener(aVoid -> {
                    imageUploading = false;
                    profileImageChecked = false;
                    Log.d(TAG, "Profile data saved to user_profiles: " + profileData);
                    Toast.makeText(this, "Profile image updated successfully", Toast.LENGTH_SHORT).show();
                    String imageUrl = (String) profileData.get("profileImage");
                    if (imageUrl != null && !imageUrl.isEmpty()) {
                        loadProfileImage(imageUrl);
                    }
                })
                .addOnFailureListener(e -> {
                    imageUploading = false;
                    Log.e(TAG, "Failed to save profile data: " + e.getMessage());
                    Toast.makeText(this, "Failed to update profile", Toast.LENGTH_SHORT).show();
                });
    }

    private void loadCachedProfileImage() {
        FirebaseUser user = mAuth.getCurrentUser();
        if (user == null) {
            setDefaultProfileImage();
            return;
        }
        String userId = user.getUid();
        SharedPreferences prefs = getSharedPreferences("ProfileImageCache", MODE_PRIVATE);
        String cachedUserId = prefs.getString("cachedUserId", "");
        String cachedImageUrl = prefs.getString("profileImageUrl_" + userId, "");

        if (cachedUserId.equals(userId) && !cachedImageUrl.isEmpty()) {
            loadProfileImageFromCache(cachedImageUrl);
        } else {
            if (!cachedUserId.isEmpty() && !cachedUserId.equals(userId)) {
                SharedPreferences.Editor editor = prefs.edit();
                editor.remove("profileImageUrl_" + cachedUserId);
                editor.remove("cachedUserId");
                editor.apply();
                Log.d(TAG, "Cleared outdated cache for user: " + cachedUserId);
            }
            setDefaultProfileImage();
            checkUserProfiles(userId); // Check user_profiles with Auth UID
        }
    }

    private void loadProfileImageFromCache(String imageUrl) {
        if (profileImageView == null) {
            Log.e(TAG, "ProfileImageView is null");
            return;
        }

        Picasso.get()
                .load(imageUrl)
                .transform(new CircleTransform())
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
                        Log.e(TAG, "Error loading cached image: " + e.getMessage());
                        FirebaseUser user = mAuth.getCurrentUser();
                        if (user != null) {
                            checkUserProfiles(user.getUid()); // Fallback to user_profiles
                        } else {
                            setDefaultProfileImage();
                        }
                    }
                });
    }

    private void cacheProfileImageUrl(String imageUrl) {
        FirebaseUser user = mAuth.getCurrentUser();
        if (user != null) {
            String userId = user.getUid();
            SharedPreferences prefs = getSharedPreferences("ProfileImageCache", MODE_PRIVATE);
            SharedPreferences.Editor editor = prefs.edit();
            editor.putString("profileImageUrl_" + userId, imageUrl);
            editor.putString("cachedUserId", userId);
            editor.apply();
            Log.d(TAG, "Cached profile image URL for user: " + userId);
        }
    }

    private void clearProfileImageCache() {
        SharedPreferences prefs = getSharedPreferences("ProfileImageCache", MODE_PRIVATE);
        SharedPreferences.Editor editor = prefs.edit();
        editor.clear();
        editor.apply();
        Log.d(TAG, "Profile image cache cleared");
    }

    private void loadCurrentUserData() {
        FirebaseUser currentUser = mAuth.getCurrentUser();
        if (currentUser == null) {
            Toast.makeText(this, "User not authenticated!", Toast.LENGTH_SHORT).show();
            Log.e(TAG, "No user is currently logged in");
            clearProfileImageCache();
            setDefaultProfileImage();
            finish();
            return;
        }

        currentUserId = currentUser.getUid();
        Log.d(TAG, "Currently logged in with UID: " + currentUserId + ", Email: " + (currentUser.getEmail() != null ? currentUser.getEmail() : "null"));

        String userEmailStr = currentUser.getEmail();
        if (userEmailStr != null) {
            findStudentByEmail(userEmailStr, currentUserId);
        } else {
            findStudentByUid(currentUserId);
        }
    }

    private void checkUserProfiles(String uid) {
        if (profileImageChecked) {
            Log.d(TAG, "Skipping redundant profile image check for UID: " + uid);
            return;
        }

        // Always use Firebase Auth UID for profile image
        FirebaseUser user = mAuth.getCurrentUser();
        if (user == null) {
            Log.e(TAG, "No authenticated user for profile image check");
            setDefaultProfileImage();
            return;
        }
        String authUid = user.getUid();
        Log.d(TAG, "Checking user_profiles for Auth UID: " + authUid);

        profilesRef.child(authUid).addListenerForSingleValueEvent(new ValueEventListener() {
            @Override
            public void onDataChange(@NonNull DataSnapshot dataSnapshot) {
                profileImageChecked = true;
                Log.d(TAG, "user_profiles snapshot for UID " + authUid + ": " + dataSnapshot.toString());
                if (dataSnapshot.exists() && dataSnapshot.hasChild("profileImage")) {
                    String profileImageUrl = dataSnapshot.child("profileImage").getValue(String.class);
                    if (profileImageUrl != null && !profileImageUrl.isEmpty()) {
                        Log.d(TAG, "Found profile image in user_profiles: " + profileImageUrl);
                        loadProfileImage(profileImageUrl);
                        cacheProfileImageUrl(profileImageUrl);
                    } else {
                        Log.d(TAG, "Profile image field empty in user_profiles");
                        setDefaultProfileImage();
                    }
                } else {
                    Log.d(TAG, "No profile image found in user_profiles or node does not exist");
                    setDefaultProfileImage();
                }
            }

            @Override
            public void onCancelled(@NonNull DatabaseError error) {
                profileImageChecked = true;
                Log.e(TAG, "Error checking user_profiles for UID " + authUid + ": " + error.getMessage());
                SharedPreferences prefs = getSharedPreferences("ProfileImageCache", MODE_PRIVATE);
                String cachedImageUrl = prefs.getString("profileImageUrl_" + authUid, "");
                if (!cachedImageUrl.isEmpty()) {
                    Log.d(TAG, "Falling back to cached image: " + cachedImageUrl);
                    loadProfileImageFromCache(cachedImageUrl);
                } else {
                    setDefaultProfileImage();
                }
            }
        });
    }

    private void findStudentByEmail(String email, String fallbackUid) {
        Log.d(TAG, "Searching students by email: " + email);
        studentsRef.orderByChild("email").equalTo(email).addListenerForSingleValueEvent(new ValueEventListener() {
            @Override
            public void onDataChange(@NonNull DataSnapshot dataSnapshot) {
                if (dataSnapshot.exists()) {
                    for (DataSnapshot studentSnapshot : dataSnapshot.getChildren()) {
                        String studentUid = studentSnapshot.getKey();
                        Log.d(TAG, "Found student with UID: " + studentUid + " for email: " + email);
                        if (studentUid != null) {
                            currentUserId = studentUid;
                            loadStudentData(studentUid);
                            // Do not call checkUserProfiles here; it's called separately
                            return;
                        }
                    }
                } else {
                    Log.w(TAG, "No student record found for email: " + email);
                }
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
        Log.d(TAG, "Searching students by UID: " + uid);
        studentsRef.child(uid).addListenerForSingleValueEvent(new ValueEventListener() {
            @Override
            public void onDataChange(@NonNull DataSnapshot dataSnapshot) {
                if (dataSnapshot.exists()) {
                    loadStudentData(uid);
                    // Do not call checkUserProfiles here; it's called separately
                } else {
                    Log.w(TAG, "No student record found for UID: " + uid);
                    Toast.makeText(MyInformation.this, "Your student profile was not found", Toast.LENGTH_LONG).show();
                    FirebaseUser user = mAuth.getCurrentUser();
                    if (user != null && user.getEmail() != null) {
                        safeSetText(email, user.getEmail());
                        safeSetText(userEmail, user.getEmail());
                    }
                }
                // Always check user_profiles with Auth UID
                FirebaseUser user = mAuth.getCurrentUser();
                if (user != null) {
                    checkUserProfiles(user.getUid());
                }
            }

            @Override
            public void onCancelled(@NonNull DatabaseError error) {
                Log.e(TAG, "Database Error for UID " + uid + ": " + error.getMessage());
                Toast.makeText(MyInformation.this, "Database Error: " + error.getMessage(), Toast.LENGTH_SHORT).show();
                FirebaseUser user = mAuth.getCurrentUser();
                if (user != null) {
                    checkUserProfiles(user.getUid());
                }
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

                        String firstNameText = safeGetString(dataSnapshot, "firstName");
                        String lastNameText = safeGetString(dataSnapshot, "lastName");
                        String middleNameText = safeGetString(dataSnapshot, "middleName");
                        String emailText = safeGetString(dataSnapshot, "email");
                        String idNumberText = safeGetString(dataSnapshot, "idNumber");
                        String yearLevelText = safeGetString(dataSnapshot, "yearLevel");
                        String sectionText = safeGetString(dataSnapshot, "section");

                        if (firstNameText.equals("--")) {
                            firstNameText = safeGetString(dataSnapshot, "firstname");
                        }
                        if (lastNameText.equals("--")) {
                            lastNameText = safeGetString(dataSnapshot, "lastname");
                        }
                        if (middleNameText.equals("--")) {
                            middleNameText = safeGetString(dataSnapshot, "middlename");
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

                        safeSetText(firstName, firstNameText);
                        safeSetText(lastName, lastNameText);
                        safeSetText(email, emailText);
                        safeSetText(userEmail, emailText);
                        safeSetText(idNumber, idNumberText);
                        safeSetText(yearLevel, yearLevelText);
                        safeSetText(section, sectionText);

                        String fullName = (firstNameText != null ? firstNameText : "") + " ";
                        if (middleNameText != null && !middleNameText.equals("--") && !middleNameText.equals("N/A")) {
                            fullName += middleNameText + " ";
                        } else {
                            fullName += " ";
                        }
                        fullName += (lastNameText != null ? lastNameText : "");

                        safeSetText(userFullname, fullName.trim());
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

    private String safeGetString(DataSnapshot dataSnapshot, String key) {
        if (dataSnapshot.hasChild(key)) {
            Object value = dataSnapshot.child(key).getValue();
            return value != null ? value.toString() : "--";
        }
        return "--";
    }

    private void safeSetText(TextView textView, String value) {
        if (textView != null) {
            textView.setText(value != null && !value.isEmpty() && !value.equals("null") ? value : "--");
        } else {
            Log.e(TAG, "Attempted to set text on null TextView");
        }
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
                    .transform(new CircleTransform())
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
                            Log.e(TAG, "Error loading profile image: " + e.getMessage());
                            setDefaultProfileImage();
                        }
                    });
        } else {
            setDefaultProfileImage();
        }
    }

    private void setDefaultProfileImage() {
        if (profileImageView != null) {
            profileImageView.setImageResource(R.drawable.profile_placeholder);
            Log.d(TAG, "Set default profile image");
        }
    }
}