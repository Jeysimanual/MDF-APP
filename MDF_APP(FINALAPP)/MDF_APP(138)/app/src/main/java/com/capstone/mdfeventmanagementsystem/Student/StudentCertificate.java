package com.capstone.mdfeventmanagementsystem.Student;

import android.content.Intent;
import android.content.SharedPreferences;
import android.content.res.ColorStateList;
import android.graphics.Color;
import android.os.Bundle;
import android.text.Editable;
import android.text.TextWatcher;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.AdapterView;
import android.widget.ArrayAdapter;
import android.widget.EditText;
import android.widget.GridLayout;
import android.widget.ImageButton;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.Spinner;
import androidx.appcompat.app.AlertDialog;
import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;
import androidx.swiperefreshlayout.widget.SwipeRefreshLayout;
import com.capstone.mdfeventmanagementsystem.Adapters.CertificateAdapter;
import com.capstone.mdfeventmanagementsystem.R;
import com.capstone.mdfeventmanagementsystem.Utilities.BaseActivity;
import com.google.android.material.bottomnavigation.BottomNavigationView;
import com.google.android.material.button.MaterialButton;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseUser;
import com.google.firebase.database.DataSnapshot;
import com.google.firebase.database.DatabaseError;
import com.google.firebase.database.DatabaseReference;
import com.google.firebase.database.FirebaseDatabase;
import com.google.firebase.database.ValueEventListener;
import com.squareup.picasso.Picasso;
import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.Date;
import java.util.HashSet;
import java.util.List;
import java.util.Locale;
import java.util.Set;

public class StudentCertificate extends BaseActivity {

    private RecyclerView recyclerView;
    private CertificateAdapter certificateAdapter;
    private List<Certificate> certificateList = new ArrayList<>();
    private List<Certificate> filteredCertificateList = new ArrayList<>();
    private DatabaseReference certRef;
    private static final String TAG = "certTesting";
    private ImageView profileImageView;
    private ImageView filterImageView;
    private DatabaseReference studentsRef;
    private DatabaseReference profilesRef;
    private LinearLayout emptyStateLayout;
    private SwipeRefreshLayout swipeRefreshLayout;
    private ValueEventListener certificateListener;
    private EditText searchEditText;
    private ImageButton clearSearchButton;
    private String selectedYear = "All";
    private Set<String> selectedMonths = new HashSet<>();

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_student_certificate);

        profileImageView = findViewById(R.id.profile_image);
        filterImageView = findViewById(R.id.filter);
        emptyStateLayout = findViewById(R.id.empty_state);
        swipeRefreshLayout = findViewById(R.id.swipe_refresh_layout);
        searchEditText = findViewById(R.id.searchEditText);
        clearSearchButton = findViewById(R.id.clearSearchButton);

        swipeRefreshLayout.setOnRefreshListener(() -> {
            Log.d(TAG, "Pull-to-refresh triggered");
            fetchCertificates();
        });

        swipeRefreshLayout.setColorSchemeResources(R.color.bg_green, R.color.white);

        // Load cached image immediately first
        loadCachedProfileImage();

        // Then start Firebase references
        studentsRef = FirebaseDatabase.getInstance().getReference().child("students");
        profilesRef = FirebaseDatabase.getInstance().getReference().child("user_profiles");

        findViewById(R.id.fab_scan).setOnClickListener(v -> {
            startActivity(new Intent(getApplicationContext(), QRCheckInActivity.class));
            overridePendingTransition(0, 0);
        });

        profileImageView.setOnClickListener(v -> {
            Intent intent = new Intent(StudentCertificate.this, ProfileActivity.class);
            startActivity(intent);
        });

        recyclerView = findViewById(R.id.recyclerViewCert);
        recyclerView.setLayoutManager(new LinearLayoutManager(this));

        filteredCertificateList.addAll(certificateList); // Initialize filtered list
        certificateAdapter = new CertificateAdapter(this, filteredCertificateList);
        recyclerView.setAdapter(certificateAdapter);

        setupBottomNavigation();
        setupSearchFunctionality();
        setupFilterFunctionality();
        fetchCertificates();
        loadUserProfile();
    }

    private void setupSearchFunctionality() {
        searchEditText.addTextChangedListener(new TextWatcher() {
            @Override
            public void beforeTextChanged(CharSequence s, int start, int count, int after) {
                // No action needed
            }

            @Override
            public void onTextChanged(CharSequence s, int start, int before, int count) {
                // No action needed
            }

            @Override
            public void afterTextChanged(Editable s) {
                String query = s.toString().trim().toLowerCase(Locale.getDefault());
                filterCertificates(query, selectedMonths, selectedYear);
                clearSearchButton.setVisibility(query.isEmpty() ? View.GONE : View.VISIBLE);
            }
        });

        clearSearchButton.setOnClickListener(v -> {
            searchEditText.setText("");
            clearSearchButton.setVisibility(View.GONE);
        });
    }

    private void setupFilterFunctionality() {
        filterImageView.setOnClickListener(v -> showFilterDialog());
    }

    private void showFilterDialog() {
        LayoutInflater inflater = LayoutInflater.from(this);
        View dialogView = inflater.inflate(R.layout.dialog_certificate_filter, null);

        Spinner yearSpinner = dialogView.findViewById(R.id.yearSpinner);
        GridLayout monthGrid = dialogView.findViewById(R.id.monthGrid);

        // Populate year spinner
        List<String> years = getAvailableYears();
        years.add(0, "All"); // Add "All" as the first option
        ArrayAdapter<String> yearAdapter = new ArrayAdapter<>(this, android.R.layout.simple_spinner_item, years);
        yearAdapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item);
        yearSpinner.setAdapter(yearAdapter);
        yearSpinner.setSelection(years.indexOf(selectedYear));

        // Populate month buttons
        String[] months = {"January", "February", "March", "April", "May", "June",
                "July", "August", "September", "October", "November", "December"};
        monthGrid.removeAllViews();
        for (int i = 0; i < months.length; i++) {
            String month = months[i];
            MaterialButton button = new MaterialButton(this, null, com.google.android.material.R.style.Widget_MaterialComponents_Button);
            GridLayout.LayoutParams params = new GridLayout.LayoutParams();
            params.width = 0;
            params.height = (int) (60 * getResources().getDisplayMetrics().density); // Fixed height of 60dp
            params.columnSpec = GridLayout.spec(i % 3, 1f);
            params.rowSpec = GridLayout.spec(i / 3);
            params.setMargins(4, 4, 4, 4);
            button.setLayoutParams(params);
            button.setText(month);
            button.setTextSize(15);
            button.setStrokeWidth(10);
            button.setPadding(12, 12, 12, 12); // Symmetrical padding for better text alignment
            button.setGravity(android.view.Gravity.CENTER); // Center text both horizontally and vertically
            button.setCornerRadius(10);
            button.setTag(month);
            button.setChecked(selectedMonths.contains(month));
            button.setStrokeWidth(0); // Remove border
            button.setBackgroundTintList(ColorStateList.valueOf(selectedMonths.contains(month) ? getResources().getColor(R.color.bg_green) : Color.WHITE));
            button.setTextColor(selectedMonths.contains(month) ? Color.WHITE : getResources().getColor(R.color.bg_green));
            button.setElevation(selectedMonths.contains(month) ? 6 : 4); // Increased elevation
            button.setRippleColor(ColorStateList.valueOf(getResources().getColor(R.color.bg_green)));
            button.setOnClickListener(v -> {
                String selectedMonth = (String) v.getTag();
                MaterialButton btn = (MaterialButton) v;
                if (selectedMonths.contains(selectedMonth)) {
                    selectedMonths.remove(selectedMonth);
                    btn.setBackgroundTintList(ColorStateList.valueOf(Color.WHITE));
                    btn.setTextColor(getResources().getColor(R.color.bg_green));
                    btn.setElevation(4); // Increased elevation for unselected state
                } else {
                    selectedMonths.add(selectedMonth);
                    btn.setBackgroundTintList(ColorStateList.valueOf(getResources().getColor(R.color.bg_green)));
                    btn.setTextColor(Color.WHITE);
                    btn.setElevation(6); // Increased elevation for selected state
                }
                btn.setStrokeWidth(0); // Ensure no border after click
                btn.setChecked(!btn.isChecked());
            });
            monthGrid.addView(button);
        }

        AlertDialog.Builder builder = new AlertDialog.Builder(this, R.style.CustomDialogTheme);
        builder.setView(dialogView);
        builder.setCancelable(true);

        AlertDialog dialog = builder.create();

        dialogView.findViewById(R.id.buttonClear).setOnClickListener(v -> {
            selectedMonths.clear();
            selectedYear = "All";
            yearSpinner.setSelection(0);
            for (int i = 0; i < monthGrid.getChildCount(); i++) {
                MaterialButton button = (MaterialButton) monthGrid.getChildAt(i);
                button.setChecked(false);
                button.setBackgroundTintList(ColorStateList.valueOf(Color.WHITE));
                button.setTextColor(getResources().getColor(R.color.bg_green));
                button.setElevation(4); // Increased elevation for unselected state
                button.setStrokeWidth(0); // Ensure no border on clear
            }
            filterCertificates(searchEditText.getText().toString().trim().toLowerCase(Locale.getDefault()),
                    selectedMonths, selectedYear);
            dialog.dismiss();
        });

        dialogView.findViewById(R.id.buttonApply).setOnClickListener(v -> {
            selectedYear = yearSpinner.getSelectedItem().toString();
            filterCertificates(searchEditText.getText().toString().trim().toLowerCase(Locale.getDefault()),
                    selectedMonths, selectedYear);
            dialog.dismiss();
        });

        dialog.show();
    }

    private List<String> getAvailableYears() {
        Set<String> years = new HashSet<>();
        SimpleDateFormat yearFormat = new SimpleDateFormat("yyyy", Locale.getDefault());
        for (Certificate certificate : certificateList) {
            try {
                Date date = new SimpleDateFormat("yyyy-MM-dd", Locale.getDefault()).parse(certificate.getReceivedDate());
                if (date != null) {
                    years.add(yearFormat.format(date));
                }
            } catch (ParseException e) {
                Log.e(TAG, "Error parsing certificate date: " + e.getMessage());
            }
        }
        List<String> yearList = new ArrayList<>(years);
        Collections.sort(yearList, Collections.reverseOrder()); // Sort descending
        return yearList;
    }

    private void filterCertificates(String query, Set<String> months, String year) {
        filteredCertificateList.clear();
        SimpleDateFormat sdf = new SimpleDateFormat("yyyy-MM-dd", Locale.getDefault());
        SimpleDateFormat monthFormat = new SimpleDateFormat("MMMM", Locale.getDefault());
        SimpleDateFormat yearFormat = new SimpleDateFormat("yyyy", Locale.getDefault());

        for (Certificate certificate : certificateList) {
            boolean matchesSearch = query.isEmpty() ||
                    (certificate.getTemplateName() != null &&
                            certificate.getTemplateName().toLowerCase(Locale.getDefault()).contains(query));

            boolean matchesMonth = months.isEmpty();
            boolean matchesYear = year.equals("All");

            try {
                Date date = sdf.parse(certificate.getReceivedDate());
                if (date != null) {
                    if (!months.isEmpty()) {
                        matchesMonth = months.contains(monthFormat.format(date));
                    }
                    if (!year.equals("All")) {
                        matchesYear = yearFormat.format(date).equals(year);
                    }
                }
            } catch (ParseException e) {
                Log.e(TAG, "Error parsing certificate date: " + e.getMessage());
                continue;
            }

            if (matchesSearch && matchesMonth && matchesYear) {
                filteredCertificateList.add(certificate);
            }
        }

        certificateAdapter.notifyDataSetChanged();
        updateEmptyState(filteredCertificateList.isEmpty());
        recyclerView.scrollToPosition(0);
    }

    private void fetchCertificates() {
        SharedPreferences sharedPreferences = getSharedPreferences("UserSession", MODE_PRIVATE);
        String studentID = sharedPreferences.getString("studentID", null);

        if (studentID == null) {
            Log.e(TAG, "No studentID found in SharedPreferences.");
            updateEmptyState(true);
            swipeRefreshLayout.setRefreshing(false);
            return;
        }

        certRef = FirebaseDatabase.getInstance()
                .getReference("students")
                .child(studentID)
                .child("certificates");

        Log.d(TAG, "Setting up real-time listener for certificates at path: students/" + studentID + "/certificates");

        // Remove existing listener if it exists
        if (certificateListener != null) {
            certRef.removeEventListener(certificateListener);
        }

        certificateListener = certRef.addValueEventListener(new ValueEventListener() {
            @Override
            public void onDataChange(@NonNull DataSnapshot snapshot) {
                Log.d(TAG, "onDataChange triggered for real-time updates.");
                certificateList.clear();
                filteredCertificateList.clear();

                for (DataSnapshot certSnap : snapshot.getChildren()) {
                    Certificate certificate = certSnap.getValue(Certificate.class);
                    if (certificate != null) {
                        certificate.setCertificateKey(certSnap.getKey());
                        certificateList.add(certificate);
                        Log.d(TAG, "Fetched certificate: " + certificate.getTemplateName() + " | Received: " + certificate.getReceivedDate());
                    } else {
                        Log.d(TAG, "Null certificate found in snapshot.");
                    }
                }

                // Sort certificates by received date (newest first)
                Collections.sort(certificateList, new Comparator<Certificate>() {
                    @Override
                    public int compare(Certificate c1, Certificate c2) {
                        SimpleDateFormat sdf = new SimpleDateFormat("yyyy-MM-dd", Locale.getDefault());
                        try {
                            Date date1 = sdf.parse(c1.getReceivedDate());
                            Date date2 = sdf.parse(c2.getReceivedDate());
                            return date2.compareTo(date1); // Descending order
                        } catch (ParseException e) {
                            Log.e(TAG, "Error parsing dates: " + e.getMessage());
                            return 0;
                        }
                    }
                });

                // Update filtered list based on current search query and filters
                String query = searchEditText.getText().toString().trim().toLowerCase(Locale.getDefault());
                filterCertificates(query, selectedMonths, selectedYear);

                Log.d(TAG, "Total certificates fetched: " + certificateList.size());
                updateEmptyState(filteredCertificateList.isEmpty());
                swipeRefreshLayout.setRefreshing(false);
            }

            @Override
            public void onCancelled(@NonNull DatabaseError error) {
                Log.e(TAG, "Database error: " + error.getMessage());
                updateEmptyState(true);
                swipeRefreshLayout.setRefreshing(false);
            }
        });
    }

    private void updateEmptyState(boolean showEmptyState) {
        if (showEmptyState) {
            emptyStateLayout.setVisibility(View.VISIBLE);
            recyclerView.setVisibility(View.GONE);
        } else {
            emptyStateLayout.setVisibility(View.GONE);
            recyclerView.setVisibility(View.VISIBLE);
        }
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
            } else if (itemId == R.id.nav_cert) {
                return true;
            }

            overridePendingTransition(0, 0);
            return true;
        });
    }

    @Override
    protected void onResume() {
        super.onResume();
        loadUserProfile();
        fetchCertificates(); // Refresh certificates on resume
    }

    @Override
    protected void onPause() {
        super.onPause();
        // Remove listener to prevent memory leaks
        if (certificateListener != null && certRef != null) {
            certRef.removeEventListener(certificateListener);
        }
    }

    private void loadCachedProfileImage() {
        FirebaseUser user = FirebaseAuth.getInstance().getCurrentUser();
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
            }
            setDefaultProfileImage();
        }
    }

    private void loadProfileImageFromCache(String imageUrl) {
        if (profileImageView == null) {
            Log.e(TAG, "ProfileImageView is null");
            return;
        }

        Picasso.get()
                .load(imageUrl)
                .transform(new CircleTransformTicket())
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
                        setDefaultProfileImage();
                    }
                });
    }

    private void cacheProfileImageUrl(String imageUrl) {
        FirebaseUser user = FirebaseAuth.getInstance().getCurrentUser();
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

    private void loadUserProfile() {
        FirebaseUser user = FirebaseAuth.getInstance().getCurrentUser();
        if (user != null) {
            checkProfileImage(user.getUid());
        } else {
            setDefaultProfileImage();
            clearProfileImageCache();
        }
    }

    private void checkProfileImage(String uid) {
        profilesRef.child(uid).addListenerForSingleValueEvent(new ValueEventListener() {
            @Override
            public void onDataChange(@NonNull DataSnapshot dataSnapshot) {
                if (dataSnapshot.exists() && dataSnapshot.hasChild("profileImage")) {
                    String profileImageUrl = dataSnapshot.child("profileImage").getValue(String.class);
                    if (profileImageUrl != null && !profileImageUrl.isEmpty()) {
                        Log.d(TAG, "Found profile image in user_profiles: " + profileImageUrl);
                        loadProfileImage(profileImageUrl);
                    } else {
                        Log.d(TAG, "Profile image field empty in user_profiles");
                        checkStudentProfileImage(uid);
                    }
                } else {
                    Log.d(TAG, "No profile image found in user_profiles, checking students collection");
                    checkStudentProfileImage(uid);
                }
            }

            @Override
            public void onCancelled(@NonNull DatabaseError error) {
                Log.e(TAG, "Error checking user_profiles: " + error.getMessage());
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
                        Log.d(TAG, "Profile image field exists but is empty in students collection");
                        clearProfileImageCache();
                        setDefaultProfileImage();
                    }
                } else {
                    Log.d(TAG, "No profile image in students collection");
                    FirebaseUser user = FirebaseAuth.getInstance().getCurrentUser();
                    if (user != null && user.getPhotoUrl() != null) {
                        loadProfileImage(user.getPhotoUrl().toString());
                    } else {
                        clearProfileImageCache();
                        setDefaultProfileImage();
                    }
                }
            }

            @Override
            public void onCancelled(@NonNull DatabaseError error) {
                Log.e(TAG, "Error checking student profile image: " + error.getMessage());
                clearProfileImageCache();
                setDefaultProfileImage();
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
                    .transform(new CircleTransformTicket())
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
                            clearProfileImageCache();
                            setDefaultProfileImage();
                        }
                    });
        } else {
            clearProfileImageCache();
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