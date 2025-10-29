package com.capstone.mdfeventmanagementsystem.Student;

import android.annotation.SuppressLint;
import android.app.AlertDialog;
import android.content.Intent;
import android.content.SharedPreferences;
import android.os.AsyncTask;
import android.os.Bundle;
import android.text.Editable;
import android.text.TextWatcher;
import android.util.Log;
import android.view.View;
import android.widget.AdapterView;
import android.widget.ArrayAdapter;
import android.widget.AutoCompleteTextView;
import android.widget.Button;
import android.widget.EditText;
import android.widget.ImageView;
import android.widget.Spinner;
import android.widget.TextView;
import android.widget.Toast;
import android.text.TextUtils;
import android.text.method.PasswordTransformationMethod;
import android.view.MotionEvent;
import android.widget.ProgressBar;

import androidx.activity.EdgeToEdge;
import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.content.ContextCompat;

import com.capstone.mdfeventmanagementsystem.Utilities.BaseActivity;
import com.capstone.mdfeventmanagementsystem.Utilities.MailSender;
import com.capstone.mdfeventmanagementsystem.Utilities.OtpVerificationActivity;
import com.capstone.mdfeventmanagementsystem.R;
import com.google.firebase.database.DataSnapshot;
import com.google.firebase.database.DatabaseError;
import com.google.firebase.database.DatabaseReference;
import com.google.firebase.database.FirebaseDatabase;
import com.google.firebase.database.ValueEventListener;

import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Locale;

import javax.mail.MessagingException;

public class StudentSignUp extends BaseActivity {

    private static final String TAG = "SignUpTest";

    private EditText idNumber, etEmail;
    AutoCompleteTextView etFirstName, etMiddleName, etLastName;
    private EditText etPassword, etConfirmPassword;
    private TextView checkLength, checkUpperCase, checkLowerCase, checkDigit, checkSpecialChar;
    private Spinner spinnerYearLevel, spinnerSection;
    private TextView tvSignUp;
    private Button btnSignUp;
    private ProgressBar signUpProgressBar;
    private DatabaseReference databaseReference, yearLvlsReference;

    private String capitalizeFirstLetter(String input) {
        if (input == null || input.isEmpty()) return "";
        return input.substring(0, 1).toUpperCase() + input.substring(1).toLowerCase();
    }

    @SuppressLint("MissingInflatedId")
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        EdgeToEdge.enable(this);
        setContentView(R.layout.activity_student_sign_up);

        tvSignUp = findViewById(R.id.tvSignUp);
        idNumber = findViewById(R.id.idNumber);
        etFirstName = findViewById(R.id.etFirstName);
        etLastName = findViewById(R.id.etLastName);
        etMiddleName = findViewById(R.id.etMiddleName);
        etEmail = findViewById(R.id.etEmail);
        etPassword = findViewById(R.id.etPassword);
        etConfirmPassword = findViewById(R.id.etConfirmPassword);
        spinnerYearLevel = findViewById(R.id.spinnerYearLevel);
        spinnerSection = findViewById(R.id.spinnerSection);
        btnSignUp = findViewById(R.id.btnSignUp);
        signUpProgressBar = findViewById(R.id.signUpProgressBar);
        ImageView backBtn = findViewById(R.id.backBtn);
        TextView backText = findViewById(R.id.back_text);

        checkLength = findViewById(R.id.checkLength);
        checkUpperCase = findViewById(R.id.checkUpperCase);
        checkLowerCase = findViewById(R.id.checkLowerCase);
        checkDigit = findViewById(R.id.checkDigit);
        checkSpecialChar = findViewById(R.id.checkSpecialChar);

        hidePasswordRequirements();
        setPasswordToggle();

        tvSignUp.setOnClickListener(v -> {
            Intent intent = new Intent(StudentSignUp.this, StudentLogin.class);
            startActivity(intent);
        });

        etPassword.setOnFocusChangeListener((v, hasFocus) -> {
            if (hasFocus) showPasswordRequirements();
            else hidePasswordRequirements();
        });

        View.OnClickListener goBackListener = v -> {
            Intent intent = new Intent(StudentSignUp.this, StudentLogin.class);
            startActivity(intent);
            overridePendingTransition(R.anim.slide_in_left, R.anim.slide_out_right);
            finish();
        };

        backBtn.setOnClickListener(goBackListener);
        backText.setOnClickListener(goBackListener);

        ArrayAdapter<String> firstNameAdapter = new ArrayAdapter<>(this, android.R.layout.simple_dropdown_item_1line, new ArrayList<>());
        ArrayAdapter<String> middleNameAdapter = new ArrayAdapter<>(this, android.R.layout.simple_dropdown_item_1line, new ArrayList<>());
        ArrayAdapter<String> lastNameAdapter = new ArrayAdapter<>(this, android.R.layout.simple_dropdown_item_1line, new ArrayList<>());

        etFirstName.setAdapter(firstNameAdapter);
        etMiddleName.setAdapter(middleNameAdapter);
        etLastName.setAdapter(lastNameAdapter);

        databaseReference = FirebaseDatabase.getInstance().getReference("students");
        databaseReference.addListenerForSingleValueEvent(new ValueEventListener() {
            @Override
            public void onDataChange(@NonNull DataSnapshot snapshot) {
                for (DataSnapshot studentSnapshot : snapshot.getChildren()) {
                    Student student = studentSnapshot.getValue(Student.class);
                    if (student != null && !student.isVerified()) {
                        if (!TextUtils.isEmpty(student.getFirstName())) {
                            firstNameAdapter.add(student.getFirstName());
                        }
                        if (!TextUtils.isEmpty(student.getMiddleName())) {
                            middleNameAdapter.add(student.getMiddleName());
                        }
                        if (!TextUtils.isEmpty(student.getLastName())) {
                            lastNameAdapter.add(student.getLastName());
                        }
                    }
                }
                firstNameAdapter.notifyDataSetChanged();
                middleNameAdapter.notifyDataSetChanged();
                lastNameAdapter.notifyDataSetChanged();
            }

            @Override
            public void onCancelled(@NonNull DatabaseError error) {
                Log.e(TAG, "Failed to load name suggestions: " + error.getMessage());
            }
        });

        etFirstName.addTextChangedListener(new TextWatcher() {
            private boolean isEditing = false;
            @Override public void beforeTextChanged(CharSequence s, int start, int count, int after) {}
            @Override public void onTextChanged(CharSequence s, int start, int before, int count) {
                validateFirstName(s.toString());
            }
            @Override public void afterTextChanged(Editable s) {
                if (isEditing) return;
                isEditing = true;
                String input = s.toString().trim();
                String[] parts = input.split("\\s+");
                for (int i = 0; i < parts.length; i++) {
                    parts[i] = capitalizeFirstLetter(parts[i]);
                }
                String formatted = String.join(" ", parts);
                if (!input.equals(formatted)) {
                    etFirstName.setText(formatted);
                    etFirstName.setSelection(formatted.length());
                }
                validateFirstName(etFirstName.getText().toString());
                isEditing = false;
            }
        });

        etLastName.addTextChangedListener(new TextWatcher() {
            private boolean isEditing = false;
            @Override public void beforeTextChanged(CharSequence s, int start, int count, int after) {}
            @Override public void onTextChanged(CharSequence s, int start, int before, int count) {
                validateLastName(s.toString());
            }
            @Override public void afterTextChanged(Editable s) {
                if (isEditing) return;
                isEditing = true;
                String input = s.toString().trim();
                String[] parts = input.split("\\s+");
                if (parts.length > 2) {
                    etLastName.setError("Only one space is allowed!");
                    return;
                }
                for (int i = 0; i < parts.length; i++) {
                    parts[i] = capitalizeFirstLetter(parts[i]);
                }
                String formatted = String.join(" ", parts);
                if (!input.equals(formatted)) {
                    etLastName.setText(formatted);
                    etLastName.setSelection(formatted.length());
                }
                validateLastName(etLastName.getText().toString());
                isEditing = false;
            }
        });

        etMiddleName.addTextChangedListener(new TextWatcher() {
            private boolean isEditing = false;
            @Override public void beforeTextChanged(CharSequence s, int start, int count, int after) {}
            @Override public void onTextChanged(CharSequence s, int start, int before, int count) {
                if (!s.toString().isEmpty()) {
                    validateMiddleName(s.toString());
                }
            }
            @Override public void afterTextChanged(Editable s) {
                if (isEditing) return;
                isEditing = true;
                String input = s.toString().trim();
                String[] parts = input.split("\\s+");
                if (parts.length > 2) {
                    etMiddleName.setError("Only one space is allowed!");
                    return;
                }
                for (int i = 0; i < parts.length; i++) {
                    parts[i] = capitalizeFirstLetter(parts[i]);
                }
                String formatted = String.join(" ", parts);
                if (!input.equals(formatted)) {
                    etMiddleName.setText(formatted);
                    etMiddleName.setSelection(formatted.length());
                }
                if (!etMiddleName.getText().toString().isEmpty()) {
                    validateMiddleName(etMiddleName.getText().toString());
                }
                isEditing = false;
            }
        });

        idNumber.addTextChangedListener(new TextWatcher() {
            @Override public void beforeTextChanged(CharSequence s, int start, int count, int after) {}
            @Override public void onTextChanged(CharSequence s, int start, int before, int count) {
                if (!s.toString().matches("\\d{0,7}")) {
                    idNumber.setError("ID must be exactly 7 digits and contain no letters or symbols!");
                }
            }
            @Override public void afterTextChanged(Editable s) {
                String input = s.toString();
                if (input.length() > 7) {
                    idNumber.setText(input.substring(0, 7));
                    idNumber.setSelection(7);
                }
            }
        });

        etPassword.addTextChangedListener(new TextWatcher() {
            @Override public void beforeTextChanged(CharSequence s, int start, int count, int after) {}
            @Override public void onTextChanged(CharSequence s, int start, int before, int count) {}
            @Override public void afterTextChanged(Editable s) {
                if (etPassword.hasFocus()) validatePassword(s.toString());
            }
        });

        etConfirmPassword.addTextChangedListener(new TextWatcher() {
            @Override public void beforeTextChanged(CharSequence s, int start, int count, int after) {}
            @Override public void onTextChanged(CharSequence s, int start, int before, int count) {}
            @Override public void afterTextChanged(Editable s) {
                validateConfirmPassword();
            }
        });

        yearLvlsReference = FirebaseDatabase.getInstance().getReference("yearLvls");
        List<String> yearLevels = new ArrayList<>();
        yearLevels.add("Select");
        yearLevels.add("Grade 7");
        yearLevels.add("Grade 8");
        yearLevels.add("Grade 9");
        yearLevels.add("Grade 10");
        yearLevels.add("Grade 11");
        yearLevels.add("Grade 12");
        ArrayAdapter<String> yearAdapter = new ArrayAdapter<>(this, android.R.layout.simple_spinner_dropdown_item, yearLevels);
        spinnerYearLevel.setAdapter(yearAdapter);

        spinnerYearLevel.setOnItemSelectedListener(new AdapterView.OnItemSelectedListener() {
            @Override
            public void onItemSelected(AdapterView<?> parent, View view, int position, long id) {
                if (position == 0) {
                    spinnerSection.setAdapter(null);
                    return;
                }
                String selectedYear = parent.getItemAtPosition(position).toString().replace(" ", "-");
                DatabaseReference sectionsRef = yearLvlsReference.child(selectedYear);
                sectionsRef.addListenerForSingleValueEvent(new ValueEventListener() {
                    @Override
                    public void onDataChange(DataSnapshot snapshot) {
                        if (snapshot.exists()) {
                            List<String> sectionsList = new ArrayList<>();
                            sectionsList.add("Select");
                            for (DataSnapshot sectionSnapshot : snapshot.getChildren()) {
                                String sectionName = sectionSnapshot.getValue(String.class);
                                sectionsList.add(sectionName);
                            }
                            ArrayAdapter<String> sectionAdapter =
                                    new ArrayAdapter<>(StudentSignUp.this, android.R.layout.simple_spinner_dropdown_item, sectionsList);
                            spinnerSection.setAdapter(sectionAdapter);
                        } else {
                            spinnerSection.setAdapter(null);
                        }
                    }
                    @Override
                    public void onCancelled(DatabaseError databaseError) {
                        Log.e(TAG, "Error fetching sections: " + databaseError.getMessage());
                    }
                });
            }
            @Override
            public void onNothingSelected(AdapterView<?> parent) {
                spinnerSection.setAdapter(null);
            }
        });

        btnSignUp.setOnClickListener(view -> validateAndRegister());
    }

    private void validateFirstName(String input) {
        if (!input.matches("[a-zA-ZñÑ\\s.]*")) {
            etFirstName.setError("Only letters (including ñ), spaces, and one dot are allowed!");
        } else if (input.chars().filter(ch -> ch == '.').count() > 1) {
            etFirstName.setError("Only one dot is allowed!");
        } else if (input.contains("  ")) {
            etFirstName.setError("Only single spaces are allowed!");
        } else {
            etFirstName.setError(null);
        }
    }

    private void validateLastName(String input) {
        if (!input.matches("[a-zA-ZñÑ\\s]*")) {
            etLastName.setError("Only letters (including ñ) and one space are allowed!");
        } else if (input.contains("  ")) {
            etLastName.setError("Only single spaces are allowed!");
        } else if (input.split("\\s+").length > 2) {
            etLastName.setError("Only one space is allowed!");
        } else {
            etLastName.setError(null);
        }
    }

    private void validateMiddleName(String input) {
        if (!input.matches("[a-zA-ZñÑ\\s]*")) {
            etMiddleName.setError("Only letters (including ñ) and one space are allowed!");
        } else if (input.contains("  ")) {
            etMiddleName.setError("Only single spaces are allowed!");
        } else if (input.split("\\s+").length > 2) {
            etMiddleName.setError("Only one space is allowed!");
        } else {
            etMiddleName.setError(null);
        }
    }

    private void showPasswordRequirements() {
        checkLength.setVisibility(View.VISIBLE);
        checkUpperCase.setVisibility(View.VISIBLE);
        checkLowerCase.setVisibility(View.VISIBLE);
        checkDigit.setVisibility(View.VISIBLE);
        checkSpecialChar.setVisibility(View.VISIBLE);

        if (!TextUtils.isEmpty(etPassword.getText())) {
            validatePassword(etPassword.getText().toString());
        }
    }

    private void hidePasswordRequirements() {
        checkLength.setVisibility(View.GONE);
        checkUpperCase.setVisibility(View.GONE);
        checkLowerCase.setVisibility(View.GONE);
        checkDigit.setVisibility(View.GONE);
        checkSpecialChar.setVisibility(View.GONE);
    }

    private void setPasswordToggle() {
        etPassword.setCompoundDrawablesRelativeWithIntrinsicBounds(0, 0, R.drawable.hide, 0);
        etConfirmPassword.setCompoundDrawablesRelativeWithIntrinsicBounds(0, 0, R.drawable.hide, 0);

        etPassword.setOnTouchListener((v, event) -> {
            final int DRAWABLE_RIGHT = 2;
            if (event.getAction() == MotionEvent.ACTION_UP) {
                if (etPassword.getCompoundDrawables()[DRAWABLE_RIGHT] != null) {
                    if (event.getRawX() >= (etPassword.getRight() - etPassword.getCompoundDrawables()[DRAWABLE_RIGHT].getBounds().width() - etPassword.getPaddingRight())) {
                        togglePasswordVisibility(etPassword);
                        return true;
                    }
                }
            }
            return false;
        });

        etConfirmPassword.setOnTouchListener((v, event) -> {
            final int DRAWABLE_RIGHT = 2;
            if (event.getAction() == MotionEvent.ACTION_UP) {
                if (etConfirmPassword.getCompoundDrawables()[DRAWABLE_RIGHT] != null) {
                    if (event.getRawX() >= (etConfirmPassword.getRight() - etConfirmPassword.getCompoundDrawables()[DRAWABLE_RIGHT].getBounds().width() - etConfirmPassword.getPaddingRight())) {
                        togglePasswordVisibility(etConfirmPassword);
                        return true;
                    }
                }
            }
            return false;
        });
    }

    private void togglePasswordVisibility(EditText editText) {
        if (editText.getTransformationMethod() instanceof PasswordTransformationMethod) {
            editText.setTransformationMethod(null);
            editText.setCompoundDrawablesRelativeWithIntrinsicBounds(0, 0, R.drawable.view, 0);
        } else {
            editText.setTransformationMethod(PasswordTransformationMethod.getInstance());
            editText.setCompoundDrawablesRelativeWithIntrinsicBounds(0, 0, R.drawable.hide, 0);
        }
    }

    private void validatePassword(String password) {
        boolean isLengthValid = password.length() >= 8;
        boolean hasUpperCase = !password.equals(password.toLowerCase());
        boolean hasLowerCase = !password.equals(password.toUpperCase());
        boolean hasDigit = password.matches(".*\\d.*");
        boolean hasSpecialChar = password.matches(".*[!@#$%^&*(),.?\":{}|<>].*");

        checkLength.setTextColor(isLengthValid ? getColor(R.color.green) : getColor(R.color.red));
        checkUpperCase.setTextColor(hasUpperCase ? getColor(R.color.green) : getColor(R.color.red));
        checkLowerCase.setTextColor(hasLowerCase ? getColor(R.color.green) : getColor(R.color.red));
        checkDigit.setTextColor(hasDigit ? getColor(R.color.green) : getColor(R.color.red));
        checkSpecialChar.setTextColor(hasSpecialChar ? getColor(R.color.green) : getColor(R.color.red));

        btnSignUp.setEnabled(isLengthValid && hasUpperCase && hasLowerCase && hasDigit && hasSpecialChar);
    }

    private void validateConfirmPassword() {
        if (!TextUtils.equals(etPassword.getText().toString(), etConfirmPassword.getText().toString())) {
            etConfirmPassword.setError("Passwords do not match!");
        } else {
            etConfirmPassword.setError(null);
        }
    }

    private Boolean isStrongPassword(String password) {
        return password.length() >= 8 &&
                password.matches(".*[A-Z].*") &&
                password.matches(".*[a-z].*") &&
                password.matches(".*\\d.*") &&
                password.matches(".*[!@#$%^&*(),.?\":{}|<>].*");
    }

    private void setLoading(boolean isLoading) {
        if (isLoading) {
            signUpProgressBar.setVisibility(View.VISIBLE);
            btnSignUp.setText("");
        } else {
            signUpProgressBar.setVisibility(View.GONE);
            btnSignUp.setText("Sign Up");
        }
        btnSignUp.setEnabled(!isLoading);
    }

    private void validateAndRegister() {
        Log.d(TAG, "validateAndRegister() called");

        String studentIdNumber = idNumber.getText().toString().trim();
        String firstName = etFirstName.getText().toString().trim();
        String lastName = etLastName.getText().toString().trim();
        String middleName = etMiddleName.getText().toString().trim();
        String email = etEmail.getText().toString().trim().toLowerCase();
        String password = etPassword.getText().toString();
        String confirmPassword = etConfirmPassword.getText().toString();
        String yearLevel = spinnerYearLevel.getSelectedItem() != null ? spinnerYearLevel.getSelectedItem().toString() : "";
        String section = spinnerSection.getSelectedItem() != null ? spinnerSection.getSelectedItem().toString() : "";

        Log.d(TAG, "User input: ID=" + studentIdNumber + ", First Name=" + firstName + ", Last Name=" + lastName +
                ", Middle Name=" + middleName + ", Email=" + email + ", Year Level=" + yearLevel + ", Section=" + section);

        if (etFirstName.getError() != null || etLastName.getError() != null || etMiddleName.getError() != null) {
            Log.w(TAG, "Validation failed: Fields contain errors");
            Toast.makeText(this, "Please correct the errors before submitting!", Toast.LENGTH_SHORT).show();
            return;
        }

        if (!studentIdNumber.matches("\\d{7}")) {
            Log.w(TAG, "Validation failed: Invalid ID number");
            Toast.makeText(this, "ID must be exactly 7 digits!", Toast.LENGTH_SHORT).show();
            return;
        }

        if (!isValidEmail(email)) {
            Log.w(TAG, "Validation failed: Invalid email format");
            Toast.makeText(this, "Enter a valid email address (e.g. @gmail.com, @yahoo.com)!", Toast.LENGTH_SHORT).show();
            return;
        }

        if (studentIdNumber.isEmpty() || firstName.isEmpty() || lastName.isEmpty() || email.isEmpty()
                || password.isEmpty() || confirmPassword.isEmpty() || yearLevel.isEmpty() || section.isEmpty()) {
            Log.w(TAG, "Validation failed: Missing required fields");
            Toast.makeText(this, "Please fill in all required fields!", Toast.LENGTH_SHORT).show();
            return;
        }

        if (yearLevel.equals("Select") || section.equals("Select")) {
            Log.w(TAG, "Validation failed: Year Level or Section not selected");
            Toast.makeText(this, "Please select a valid Year Level and Section!", Toast.LENGTH_SHORT).show();
            return;
        }

        if (!android.util.Patterns.EMAIL_ADDRESS.matcher(email).matches()) {
            Log.w(TAG, "Validation failed: Invalid email format");
            Toast.makeText(this, "Enter a valid email address!", Toast.LENGTH_SHORT).show();
            return;
        }

        if (!password.equals(confirmPassword)) {
            Log.w(TAG, "Validation failed: Passwords do not match");
            Toast.makeText(this, "Passwords do not match!", Toast.LENGTH_SHORT).show();
            return;
        }

        Log.d(TAG, "Validation passed. Checking student existence and email uniqueness in Firebase...");

        setLoading(true);

        DatabaseReference studentsRef = FirebaseDatabase.getInstance().getReference("students");

        studentsRef.addListenerForSingleValueEvent(new ValueEventListener() {
            @Override
            public void onDataChange(@NonNull DataSnapshot snapshot) {
                // Check for email uniqueness first
                for (DataSnapshot studentSnapshot : snapshot.getChildren()) {
                    Student student = studentSnapshot.getValue(Student.class);
                    if (student != null && email.equalsIgnoreCase(student.getEmail())) {
                        new androidx.appcompat.app.AlertDialog.Builder(StudentSignUp.this)
                                .setTitle("Email Already in Use")
                                .setMessage("This email address is already registered. Please use a different email or log in.")
                                .setPositiveButton("OK", (dialog, which) -> {
                                    dialog.dismiss();
                                    setLoading(false);
                                    Intent intent = new Intent(StudentSignUp.this, StudentLogin.class);
                                    startActivity(intent);
                                    finish();
                                })
                                .setCancelable(false)
                                .show();
                        return;
                    }
                }

                // Check student ID and verification status
                boolean found = false;
                for (DataSnapshot studentSnapshot : snapshot.getChildren()) {
                    Student student = studentSnapshot.getValue(Student.class);

                    if (student != null && studentIdNumber.equals(student.getIdNumber())) {
                        found = true;
                        String studentKey = studentSnapshot.getKey();
                        String studentYearLevel = student.getYearLevel();
                        String studentSection = student.getSection();

                        // Check if the student is already verified
                        if (student.isVerified()) {
                            new androidx.appcompat.app.AlertDialog.Builder(StudentSignUp.this)
                                    .setTitle("Account Already Exists")
                                    .setMessage("This student ID already has a verified account. Please log in instead.")
                                    .setPositiveButton("OK", (dialog, which) -> {
                                        dialog.dismiss();
                                        setLoading(false);
                                        Intent intent = new Intent(StudentSignUp.this, StudentLogin.class);
                                        startActivity(intent);
                                        finish();
                                    })
                                    .setCancelable(false)
                                    .show();
                            return;
                        }

                        String yearLevelNumber = extractNumberFromYearLevel(yearLevel);

                        if ((studentYearLevel.equals(yearLevelNumber) || studentYearLevel.equalsIgnoreCase(yearLevel)) &&
                                studentSection.trim().equalsIgnoreCase(section.trim())) {
                            updateStudentData(studentKey, firstName, lastName, middleName, email, password);
                            return;
                        } else {
                            new androidx.appcompat.app.AlertDialog.Builder(StudentSignUp.this)
                                    .setTitle("Invalid Information")
                                    .setMessage("The ID number exists, but the Year Level or Section you entered does not match our records.\n\nPlease check and try again.")
                                    .setPositiveButton("OK", (dialog, which) -> {
                                        dialog.dismiss();
                                        setLoading(false);
                                    })
                                    .setCancelable(false)
                                    .show();
                            return;
                        }
                    }
                }

                if (!found) {
                    new androidx.appcompat.app.AlertDialog.Builder(StudentSignUp.this)
                            .setTitle("Student Not Found")
                            .setMessage("No student record found for this ID, Year Level, and Section.\n\nPlease double-check your information.")
                            .setPositiveButton("OK", (dialog, which) -> {
                                dialog.dismiss();
                                setLoading(false);
                            })
                            .setCancelable(false)
                            .show();
                }
            }

            @Override
            public void onCancelled(@NonNull DatabaseError error) {
                Log.e(TAG, "Database error: " + error.getMessage());
                Toast.makeText(StudentSignUp.this, "Failed to validate student data!", Toast.LENGTH_SHORT).show();
                setLoading(false);
            }
        });
    }

    private String extractNumberFromYearLevel(String yearLevel) {
        if (yearLevel == null || yearLevel.isEmpty()) {
            return "";
        }
        return yearLevel.replaceAll("\\D+", "");
    }

    private boolean isValidEmail(String email) {
        if (!android.util.Patterns.EMAIL_ADDRESS.matcher(email).matches()) {
            return false;
        }

        String[] validDomains = {"gmail.com", "yahoo.com", "outlook.com", "hotmail.com"};
        String domain = email.substring(email.indexOf('@') + 1).toLowerCase();

        for (String validDomain : validDomains) {
            if (domain.equals(validDomain)) {
                return true;
            }
        }

        return false;
    }

    private void updateStudentData(String studentKey, String firstName, String lastName, String middleName, String email, String password) {
        int otp = (int) (Math.random() * 900000) + 100000;
        String hashedOtp = hashOtp(String.valueOf(otp));

        String fullName = middleName.isEmpty()
                ? lastName + ", " + firstName
                : lastName + ", " + firstName + " " + middleName.charAt(0) + ".";

        Map<String, Object> studentUpdates = new HashMap<>();
        studentUpdates.put("email", email.toLowerCase());
        studentUpdates.put("firstName", firstName);
        studentUpdates.put("lastName", lastName);
        if (!middleName.isEmpty()) {
            studentUpdates.put("middleName", middleName);
        }
        studentUpdates.put("fullName", fullName);
        studentUpdates.put("role", "student");
        studentUpdates.put("otp", hashedOtp);
        studentUpdates.put("isVerified", false); // Only for unverified students

        databaseReference.child(studentKey).updateChildren(studentUpdates)
                .addOnSuccessListener(aVoid -> {
                    Log.d(TAG, "Student data updated successfully");
                    SharedPreferences sharedPreferences = getSharedPreferences("MyAppPrefs", MODE_PRIVATE);
                    SharedPreferences.Editor editor = sharedPreferences.edit();
                    editor.putString("studentID", studentKey);
                    editor.putString("idNumber", idNumber.getText().toString().trim());
                    editor.putString("tempEmail", email);
                    editor.putString("tempPassword", password);
                    editor.apply();
                    sendOtpEmail(email, firstName, otp);
                    setLoading(false);
                    navigateToOtpVerification(studentKey, email, password);
                })
                .addOnFailureListener(e -> {
                    Log.e(TAG, "Failed to update student data: " + e.getMessage());
                    Toast.makeText(StudentSignUp.this, "Failed to update student data!", Toast.LENGTH_SHORT).show();
                    setLoading(false);
                });
    }

    private String hashOtp(String otp) {
        try {
            MessageDigest digest = MessageDigest.getInstance("SHA-256");
            byte[] hash = digest.digest(otp.getBytes());
            StringBuilder hexString = new StringBuilder();
            for (byte b : hash) {
                hexString.append(String.format(Locale.US, "%02x", b));
            }
            return hexString.toString();
        } catch (NoSuchAlgorithmException e) {
            e.printStackTrace();
            return null;
        }
    }

    private void navigateToOtpVerification(String studentId, String email, String password) {
        Intent intent = new Intent(StudentSignUp.this, OtpVerificationActivity.class);
        intent.putExtra("STUDENT_ID", studentId);
        intent.putExtra("EMAIL", email);
        intent.putExtra("PASSWORD", password);
        startActivity(intent);
        finish();
    }

    private void sendOtpEmail(String recipientEmail, String firstName, int otp) {
        String subject = "Your MDF Event Verification Code";
        String message = "Dear " + firstName + ",\n\n"
                + "Your verification code is: " + otp + "\n\n"
                + "Enter this code in the app to verify your email.\n\n"
                + "Best Regards,\nMDF Event Management Team";

        AsyncTask.execute(() -> {
            try {
                MailSender mailSender = new MailSender();
                mailSender.sendEmail(recipientEmail, subject, message);
            } catch (MessagingException e) {
                Log.e(TAG, "Failed to send OTP email: " + e.getMessage(), e);
            }
        });
    }
}