package com.capstone.mdfeventmanagementsystem.Student;

import android.content.Intent;
import android.content.SharedPreferences;
import android.content.res.ColorStateList;
import android.graphics.Bitmap;
import android.graphics.Color;
import android.os.Bundle;
import android.util.Log;
import android.view.View;
import android.widget.ImageView;

import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.capstone.mdfeventmanagementsystem.Adapters.CertificateAdapter;
import com.capstone.mdfeventmanagementsystem.R;
import com.capstone.mdfeventmanagementsystem.Utilities.BaseActivity;
import com.google.android.material.bottomappbar.BottomAppBar;
import com.google.android.material.bottomnavigation.BottomNavigationView;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseUser;
import com.google.firebase.database.DataSnapshot;
import com.google.firebase.database.DatabaseError;
import com.google.firebase.database.DatabaseReference;
import com.google.firebase.database.FirebaseDatabase;
import com.google.firebase.database.ValueEventListener;
import com.squareup.picasso.Picasso;
import com.squareup.picasso.Transformation;

import java.util.ArrayList;
import java.util.List;

class CircleTransformCertificate implements Transformation {
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

public class StudentCertificate extends BaseActivity {

    private RecyclerView recyclerView;
    private CertificateAdapter certificateAdapter;
    private List<Certificate> certificateList = new ArrayList<>();
    private DatabaseReference certRef;
    private static final String TAG = "certTesting";
    private ImageView profileImageView;
    private DatabaseReference studentsRef;
    private DatabaseReference profilesRef;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_student_certificate);

        profileImageView = findViewById(R.id.profile_image);

        // Load cached image immediately first
        loadCachedProfileImage();

        // Then start Firebase references
        studentsRef = FirebaseDatabase.getInstance().getReference().child("students");
        profilesRef = FirebaseDatabase.getInstance().getReference().child("user_profiles");

        findViewById(R.id.fab_scan).setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                startActivity(new Intent(getApplicationContext(), QRCheckInActivity.class));
                overridePendingTransition(0, 0);
            }
        });

        BottomAppBar bottomAppBar = findViewById(R.id.bottomAppBar);
        bottomAppBar.setBackgroundTint(ColorStateList.valueOf(Color.WHITE));

        findViewById(R.id.profile_image).setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                Intent intent = new Intent(StudentCertificate.this, ProfileActivity.class);
                startActivity(intent);
            }
        });

        recyclerView = findViewById(R.id.recyclerViewCert);
        recyclerView.setLayoutManager(new LinearLayoutManager(this));

        certificateAdapter = new CertificateAdapter(this, certificateList);
        recyclerView.setAdapter(certificateAdapter);

        setupBottomNavigation();
        fetchCertificates();
        loadUserProfile();
    }

    private void fetchCertificates() {
        SharedPreferences sharedPreferences = getSharedPreferences("UserSession", MODE_PRIVATE);
        String studentID = sharedPreferences.getString("studentID", null);

        if (studentID == null) {
            Log.e(TAG, "No studentID found in SharedPreferences.");
            return;
        }

        certRef = FirebaseDatabase.getInstance()
                .getReference("students")
                .child(studentID)
                .child("certificates");

        Log.d(TAG, "Fetching certificates from path: students/" + studentID + "/certificates");

        certRef.addValueEventListener(new ValueEventListener() {
            @Override
            public void onDataChange(DataSnapshot snapshot) {
                Log.d(TAG, "onDataChange triggered.");
                certificateList.clear();

                for (DataSnapshot certSnap : snapshot.getChildren()) {
                    Certificate certificate = certSnap.getValue(Certificate.class);
                    if (certificate != null) {
                        // ✅ Add this line to assign the Firebase key
                        certificate.setCertificateKey(certSnap.getKey());

                        certificateList.add(certificate);
                        Log.d(TAG, "Fetched certificate: " + certificate.getTemplateName() + " | Received: " + certificate.getReceivedDate());
                    } else {
                        Log.d(TAG, "Null certificate found in snapshot.");
                    }
                }

                Log.d(TAG, "Total certificates fetched: " + certificateList.size());
                certificateAdapter.notifyDataSetChanged();
            }

            @Override
            public void onCancelled(DatabaseError error) {
                Log.e(TAG, "Database error: " + error.getMessage());
            }
        });
    }



    private void setupBottomNavigation() {
        BottomNavigationView bottomNavigationView = findViewById(R.id.bottom_navigation);
        bottomNavigationView.setSelectedItemId(R.id.nav_cert);
        bottomNavigationView.setBackground(null);

        bottomNavigationView.setOnItemSelectedListener(item -> {
            int itemId = item.getItemId();

            if (itemId == R.id.nav_home) {
                startActivity(new Intent(this, MainActivity2.class));
                finish();
            } else if (itemId == R.id.nav_event) {
                startActivity(new Intent(this, StudentDashboard.class));
                finish();
            } else if (itemId == R.id.nav_ticket) {
                startActivity(new Intent(this, StudentTickets.class));
                finish();
            }  else if (itemId == R.id.nav_cert) {
                return true;
            }

            overridePendingTransition(0, 0);
            return true;
        });
    }

    @Override
    protected void onResume() {
        super.onResume();
        // We already have cached image displayed, now just update in background
        loadUserProfile();
    }

    /**
     * Load cached profile image immediately on startup
     */
    private void loadCachedProfileImage() {
        SharedPreferences prefs = getSharedPreferences("ProfileImageCache", MODE_PRIVATE);
        String cachedImageUrl = prefs.getString("profileImageUrl", "");

        if (!cachedImageUrl.isEmpty()) {
            // Load the cached image immediately
            loadProfileImageFromCache(cachedImageUrl);
        } else {
            // Set default if no cached image
            setDefaultProfileImage();
        }
    }

    /**
     * Load profile image from cache with no network operations
     */
    private void loadProfileImageFromCache(String imageUrl) {
        if (profileImageView == null) {
            return;
        }

        // Use Picasso to load from cache only - no network
        Picasso.get()
                .load(imageUrl)
                .transform(new CircleTransformCertificate())
                .placeholder(R.drawable.profile_placeholder)
                .error(R.drawable.profile_placeholder)
                .networkPolicy(com.squareup.picasso.NetworkPolicy.OFFLINE) // Only load from cache
                .into(profileImageView, new com.squareup.picasso.Callback() {
                    @Override
                    public void onSuccess() {
                        Log.d(TAG, "Profile image loaded from cache");
                    }

                    @Override
                    public void onError(Exception e) {
                        // If cache loading fails, use the default
                        setDefaultProfileImage();
                    }
                });
    }

    /**
     * Save profile image URL to cache when successfully loaded
     */
    private void cacheProfileImageUrl(String imageUrl) {
        SharedPreferences prefs = getSharedPreferences("ProfileImageCache", MODE_PRIVATE);
        SharedPreferences.Editor editor = prefs.edit();
        editor.putString("profileImageUrl", imageUrl);
        editor.apply();
    }

    private void loadUserProfile() {
        FirebaseUser user = FirebaseAuth.getInstance().getCurrentUser();
        if (user != null) {
            // Check for profile image
            checkProfileImage(user.getUid());
        }
    }

    /**
     * Checks for profile image in student_profiles collection
     * @param uid User ID for lookup
     */
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
                        // If no image in profiles, check the students collection
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
                // Fallback to students collection
                checkStudentProfileImage(uid);
            }
        });
    }

    /**
     * Checks for profile image in students collection
     * @param uid User ID for lookup
     */
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
                        // Don't set default here as we already have cached or default
                    }
                } else {
                    Log.d(TAG, "No profile image in students collection");
                    // Check if firebase user has photo URL as last resort
                    FirebaseUser user = FirebaseAuth.getInstance().getCurrentUser();
                    if (user != null && user.getPhotoUrl() != null) {
                        loadProfileImage(user.getPhotoUrl().toString());
                    }
                }
            }

            @Override
            public void onCancelled(@NonNull DatabaseError error) {
                Log.e(TAG, "Error checking student profile image: " + error.getMessage());
                // Don't set default here as we already have cached or default
            }
        });
    }

    /**
     * Loads profile image using Picasso with circle transformation
     * @param imageUrl URL of the profile image
     */
    private void loadProfileImage(String imageUrl) {
        if (profileImageView == null) {
            Log.e(TAG, "Cannot load profile image: ImageView is null");
            return;
        }

        if (imageUrl != null && !imageUrl.isEmpty()) {
            Log.d(TAG, "Loading profile image from: " + imageUrl);

            // Use Picasso to load the image with transformation for circular display
            Picasso.get()
                    .load(imageUrl)
                    .transform(new CircleTransformDashboard()) // Use CircleTransform for circular images
                    .placeholder(R.drawable.profile_placeholder)
                    .error(R.drawable.profile_placeholder)
                    .into(profileImageView, new com.squareup.picasso.Callback() {
                        @Override
                        public void onSuccess() {
                            Log.d(TAG, "Profile image loaded successfully");
                            // Cache the successful URL for next time
                            cacheProfileImageUrl(imageUrl);
                        }

                        @Override
                        public void onError(Exception e) {
                            Log.e(TAG, "Error loading profile image: " + (e != null ? e.getMessage() : "unknown error"));
                            // Don't set default here as we already have cached or default
                        }
                    });
        }
    }

    /**
     * Sets default profile image placeholder
     */
    private void setDefaultProfileImage() {
        if (profileImageView != null) {
            profileImageView.setImageResource(R.drawable.profile_placeholder);
        }
    }
}
