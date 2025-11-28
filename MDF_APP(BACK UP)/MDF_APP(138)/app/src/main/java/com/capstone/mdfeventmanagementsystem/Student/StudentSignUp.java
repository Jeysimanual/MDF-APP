package com.capstone.mdfeventmanagementsystem.Student;

import android.annotation.SuppressLint;
import android.content.Intent;
import android.content.SharedPreferences;
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
import android.widget.ProgressBar;
import android.widget.Spinner;
import android.widget.TextView;
import android.widget.Toast;
import android.text.TextUtils;
import android.text.method.PasswordTransformationMethod;
import android.view.MotionEvent;

import androidx.activity.EdgeToEdge;
import androidx.annotation.NonNull;
import androidx.appcompat.app.AlertDialog;
import androidx.appcompat.app.AppCompatActivity;

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
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

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

        // Initialize the validation TextViews
        checkLength = findViewById(R.id.checkLength);
        checkUpperCase = findViewById(R.id.checkUpperCase);
        checkLowerCase = findViewById(R.id.checkLowerCase);
        checkDigit = findViewById(R.id.checkDigit);
        checkSpecialChar = findViewById(R.id.checkSpecialChar);

        // Restore form data from Intent extras if available
        Intent intent = getIntent();
        if (intent != null) {
            String email = intent.getStringExtra("EMAIL");
            String firstName = intent.getStringExtra("FIRST_NAME");
            String middleName = intent.getStringExtra("MIDDLE_NAME");
            String studentId = intent.getStringExtra("STUDENT_ID");
            String password = intent.getStringExtra("PASSWORD");
            if (email != null) {
                etEmail.setText(email);
                etEmail.requestFocus();
            }
            if (firstName != null) {
                etFirstName.setText(firstName);
            }
            if (middleName != null && !middleName.equals("N/A")) {
                etMiddleName.setText(middleName);
            }
            if (studentId != null) {
                idNumber.setText(studentId);
            }
            if (password != null) {
                etPassword.setText(password);
                etConfirmPassword.setText(password);
            }
        }

        hidePasswordRequirements();
        setPasswordToggle();

        tvSignUp.setOnClickListener(v -> {
            Intent loginIntent = new Intent(StudentSignUp.this, StudentLogin.class);
            startActivity(loginIntent);
        });

        etPassword.setOnFocusChangeListener((v, hasFocus) -> {
            if (hasFocus) showPasswordRequirements();
            else hidePasswordRequirements();
        });

        View.OnClickListener goBackListener = v -> {
            Intent loginIntent = new Intent(StudentSignUp.this, StudentLogin.class);
            startActivity(loginIntent);
            overridePendingTransition(R.anim.slide_in_left, R.anim.slide_out_right);
            finish();
        };

        backBtn.setOnClickListener(goBackListener);
        backText.setOnClickListener(goBackListener);

        // 🔹 Setup Adapters for suggestions
        ArrayAdapter<String> firstNameAdapter = new ArrayAdapter<>(this, android.R.layout.simple_dropdown_item_1line, new ArrayList<>());
        ArrayAdapter<String> middleNameAdapter = new ArrayAdapter<>(this, android.R.layout.simple_dropdown_item_1line, new ArrayList<>());
        ArrayAdapter<String> lastNameAdapter = new ArrayAdapter<>(this, android.R.layout.simple_dropdown_item_1line, new ArrayList<>());

        etFirstName.setAdapter(firstNameAdapter);
        etMiddleName.setAdapter(middleNameAdapter);
        etLastName.setAdapter(lastNameAdapter);

        // 🔹 Fetch unverified student names
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

        // 🔹 Real-time validation for First Name
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

        // 🔹 Real-time validation for Middle Name (optional)
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

        // 🔹 ID validation
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

        // 🔹 Password validation
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

        // 🔹 Populate Year Level Spinner with "Select" as first option
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
                    // "Select" option selected, clear section spinner
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
                            sectionsList.add("Select"); // Add "Select" as first option
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

        // If password has been entered, update the validation status
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
        // Initially set the drawables
        etPassword.setCompoundDrawablesRelativeWithIntrinsicBounds(0, 0, R.drawable.hide, 0);
        etConfirmPassword.setCompoundDrawablesRelativeWithIntrinsicBounds(0, 0, R.drawable.hide, 0);

        etPassword.setOnTouchListener((v, event) -> {
            final int DRAWABLE_RIGHT = 2;

            if (event.getAction() == MotionEvent.ACTION_UP) {
                if (etPassword.getCompoundDrawables()[DRAWABLE_RIGHT] != null) {
                    // Check if touch is within the bounds of the right drawable
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
                    // Check if touch is within the bounds of the right drawable
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
            // Show password
            editText.setTransformationMethod(null);
            editText.setCompoundDrawablesRelativeWithIntrinsicBounds(0, 0, R.drawable.view, 0);
        } else {
            // Hide password
            editText.setTransformationMethod(PasswordTransformationMethod.getInstance());
            editText.setCompoundDrawablesRelativeWithIntrinsicBounds(0, 0, R.drawable.hide, 0);
        }
    }

    /** PASSWORD VALIDATION **/
    private void validatePassword(String password) {
        boolean isLengthValid = password.length() >= 8;
        boolean hasUpperCase = !password.equals(password.toLowerCase());
        boolean hasLowerCase = !password.equals(password.toUpperCase());
        boolean hasDigit = password.matches(".*\\d.*");
        boolean hasSpecialChar = password.matches(".*[!@#$%^&*(),.?\":{}|<>].*");

        // Update UI indicators - color only, no visibility change
        checkLength.setTextColor(isLengthValid ? getColor(R.color.green) : getColor(R.color.red));
        checkUpperCase.setTextColor(hasUpperCase ? getColor(R.color.green) : getColor(R.color.red));
        checkLowerCase.setTextColor(hasLowerCase ? getColor(R.color.green) : getColor(R.color.red));
        checkDigit.setTextColor(hasDigit ? getColor(R.color.green) : getColor(R.color.red));
        checkSpecialChar.setTextColor(hasSpecialChar ? getColor(R.color.green) : getColor(R.color.red));

        // Enable sign-up button only if all conditions are met
        btnSignUp.setEnabled(isLengthValid && hasUpperCase && hasLowerCase && hasDigit && hasSpecialChar);
    }

    /** CONFIRM PASSWORD VALIDATION **/
    private void validateConfirmPassword() {
        if (!TextUtils.equals(etPassword.getText().toString(), etConfirmPassword.getText().toString())) {
            etConfirmPassword.setError("Passwords do not match!");
        } else {
            etConfirmPassword.setError(null); // Clear the error if passwords match
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
        Log.d(TAG, "validateAndRegister: Started");

        String studentIdNumber = idNumber.getText().toString().trim();
        String firstName = etFirstName.getText().toString().trim();
        String lastName = etLastName.getText().toString().trim();
        String middleName = etMiddleName.getText().toString().trim();
        String email = etEmail.getText().toString().trim().toLowerCase();
        String password = etPassword.getText().toString();
        String confirmPassword = etConfirmPassword.getText().toString();
        String yearLevel = spinnerYearLevel.getSelectedItem() != null ? spinnerYearLevel.getSelectedItem().toString() : "";
        String section = spinnerSection.getSelectedItem() != null ? spinnerSection.getSelectedItem().toString() : "";

        Log.d(TAG, "validateAndRegister: User input - ID=" + studentIdNumber + ", FirstName=" + firstName +
                ", LastName=" + lastName + ", MiddleName=" + middleName + ", Email=" + email +
                ", YearLevel=" + yearLevel + ", Section=" + section);

        // Show loading indicator and disable button
        setLoading(true);

        // Validation checks for input fields
        if (etFirstName.getError() != null || etLastName.getError() != null || etMiddleName.getError() != null) {
            Log.w(TAG, "validateAndRegister: Validation failed - Fields contain errors");
            runOnUiThread(() -> {
                Toast.makeText(StudentSignUp.this, "Please correct the errors before submitting!", Toast.LENGTH_SHORT).show();
                setLoading(false);
            });
            return;
        }


        if (studentIdNumber.isEmpty() || firstName.isEmpty() || lastName.isEmpty() || email.isEmpty()
                || password.isEmpty() || confirmPassword.isEmpty() || yearLevel.isEmpty() || section.isEmpty()) {
            Log.w(TAG, "validateAndRegister: Validation failed - Missing required fields");
            runOnUiThread(() -> {
                Toast.makeText(StudentSignUp.this, "Please fill up all fields!", Toast.LENGTH_SHORT).show();
                setLoading(false);
            });
            return;
        }

        if (!isValidEmail(email)) {
            Log.w(TAG, "validateAndRegister: Validation failed - Invalid email format: " + email);
            runOnUiThread(() -> {
                Toast.makeText(StudentSignUp.this, "Enter a valid email address (e.g. @gmail.com, @yahoo.com)!", Toast.LENGTH_SHORT).show();
                setLoading(false);
            });
            return;
        }
        if (yearLevel.equals("Select") || section.equals("Select")) {
            Log.w(TAG, "validateAndRegister: Validation failed - Year Level or Section not selected");
            runOnUiThread(() -> {
                Toast.makeText(StudentSignUp.this, "Please select a valid Year Level and Section!", Toast.LENGTH_SHORT).show();
                setLoading(false);
            });
            return;
        }

        if (!android.util.Patterns.EMAIL_ADDRESS.matcher(email).matches()) {
            Log.w(TAG, "validateAndRegister: Validation failed - Invalid email format: " + email);
            runOnUiThread(() -> {
                Toast.makeText(StudentSignUp.this, "Enter a valid email address!", Toast.LENGTH_SHORT).show();
                setLoading(false);
            });
            return;
        }

        if (!password.equals(confirmPassword)) {
            Log.w(TAG, "validateAndRegister: Validation failed - Passwords do not match");
            runOnUiThread(() -> {
                Toast.makeText(StudentSignUp.this, "Passwords do not match!", Toast.LENGTH_SHORT).show();
                setLoading(false);
            });
            return;
        }

        Log.d(TAG, "validateAndRegister: Input validation passed. Proceeding to check student ID...");

        // Proceed to check student ID and verification status
        checkStudentIdAndProceed(studentIdNumber, firstName, lastName, middleName, email, password, yearLevel, section, () -> {
            // Callback: Check email and name combination in Realtime Database
            Log.d(TAG, "validateAndRegister: Checking email and name combination in Realtime Database...");
            DatabaseReference studentsRef = FirebaseDatabase.getInstance().getReference("students");

            // Check email
            studentsRef.orderByChild("email").equalTo(email).addListenerForSingleValueEvent(new ValueEventListener() {
                @Override
                public void onDataChange(@NonNull DataSnapshot snapshot) {
                    boolean emailExists = snapshot.exists();
                    if (emailExists) {
                        for (DataSnapshot studentSnapshot : snapshot.getChildren()) {
                            Object isVerifiedRaw = studentSnapshot.child("isVerified").getValue();
                            Log.d(TAG, "validateAndRegister: Found student by email, studentId: " +
                                    studentSnapshot.getKey() + ", raw isVerified: " + isVerifiedRaw);
                            boolean isVerified = (isVerifiedRaw instanceof Boolean) ? (boolean) isVerifiedRaw : false;
                            if (isVerified) {
                                Log.w(TAG, "validateAndRegister: Email is associated with a verified account, studentId: " +
                                        studentSnapshot.getKey());
                                runOnUiThread(() -> {
                                    Toast.makeText(StudentSignUp.this,
                                            "This email already has a verified account! Please log in.",
                                            Toast.LENGTH_LONG).show();
                                    setLoading(false);
                                    navigateToLogin();
                                });
                                return;
                            }
                        }
                    }

                    // If email doesn't exist or is not verified, check for name combination
                    Log.d(TAG, "validateAndRegister: Checking for existing name combination (FirstName=" + firstName +
                            ", MiddleName=" + middleName + ", LastName=" + lastName + ")...");
                    studentsRef.orderByChild("fullName").startAt(lastName + ", " + firstName)
                            .endAt(lastName + ", " + firstName + "\uf8ff").addListenerForSingleValueEvent(new ValueEventListener() {
                                @Override
                                public void onDataChange(@NonNull DataSnapshot nameSnapshot) {
                                    if (nameSnapshot.exists()) {
                                        for (DataSnapshot studentSnapshot : nameSnapshot.getChildren()) {
                                            String dbFirstName = studentSnapshot.child("firstName").getValue(String.class);
                                            String dbMiddleName = studentSnapshot.child("middleName").getValue(String.class);
                                            String dbLastName = studentSnapshot.child("lastName").getValue(String.class);
                                            Object isVerifiedRaw = studentSnapshot.child("isVerified").getValue();

                                            Log.d(TAG, "validateAndRegister: Found student by name, studentId: " + studentSnapshot.getKey() +
                                                    ", dbFirstName=" + dbFirstName + ", dbMiddleName=" + dbMiddleName +
                                                    ", dbLastName=" + dbLastName + ", raw isVerified: " + isVerifiedRaw);

                                            boolean isVerified = (isVerifiedRaw instanceof Boolean) ? (boolean) isVerifiedRaw : false;
                                            if (isVerified && dbFirstName != null && dbFirstName.equalsIgnoreCase(firstName) &&
                                                    dbLastName != null && dbLastName.equalsIgnoreCase(lastName) &&
                                                    (middleName.isEmpty() || (dbMiddleName != null && dbMiddleName.equalsIgnoreCase(middleName)))) {
                                                Log.w(TAG, "validateAndRegister: Name combination already associated with a verified account, studentId: " +
                                                        studentSnapshot.getKey());
                                                runOnUiThread(() -> {
                                                    Toast.makeText(StudentSignUp.this,
                                                            "You already have a verified account! Please log in.",
                                                            Toast.LENGTH_LONG).show();
                                                    setLoading(false);
                                                    navigateToLogin();
                                                });
                                                return;
                                            }
                                        }
                                    }
                                    Log.d(TAG, "validateAndRegister: No matching verified name combination found, proceeding...");
                                    // If no verified name combination, allow signup to proceed (already validated in checkStudentIdAndProceed)
                                }

                                @Override
                                public void onCancelled(@NonNull DatabaseError error) {
                                    Log.e(TAG, "validateAndRegister: Error checking name combination in Realtime Database: " + error.getMessage());
                                    runOnUiThread(() -> {
                                        Toast.makeText(StudentSignUp.this,
                                                "Failed to validate name combination in database: " + error.getMessage(),
                                                Toast.LENGTH_SHORT).show();
                                        setLoading(false);
                                    });
                                }
                            });
                }

                @Override
                public void onCancelled(@NonNull DatabaseError error) {
                    Log.e(TAG, "validateAndRegister: Error checking email in Realtime Database: " + error.getMessage());
                    runOnUiThread(() -> {
                        Toast.makeText(StudentSignUp.this,
                                "Failed to validate email in database: " + error.getMessage(),
                                Toast.LENGTH_SHORT).show();
                        setLoading(false);
                    });
                }
            });
        });
    }

    // Helper method to navigate to login screen
    private void navigateToLogin() {
        Intent intent = new Intent(StudentSignUp.this, StudentLogin.class);
        startActivity(intent);
        overridePendingTransition(R.anim.slide_in_left, R.anim.slide_out_right);
        finish();
    }


    private void checkStudentIdAndProceed(String studentIdNumber, String firstName, String lastName, String middleName,
                                          String email, String password, String yearLevel, String section, Runnable onValidId) {
        DatabaseReference studentsRef = FirebaseDatabase.getInstance().getReference("students");

        // Check if idNumber exists in the database
        studentsRef.orderByChild("idNumber").equalTo(studentIdNumber).get()
                .addOnSuccessListener((DataSnapshot snapshot) -> {
                    if (!snapshot.exists()) {
                        Log.w(TAG, "checkStudentIdAndProceed: No student record found for idNumber: " + studentIdNumber);
                        runOnUiThread(() -> {
                            AlertDialog dialog = new AlertDialog.Builder(StudentSignUp.this)
                                    .setTitle("Student Not Found")
                                    .setMessage("No student record found for this ID in the database.\n\nPlease double-check your ID number.")
                                    .setPositiveButton("OK", (dlg, which) -> {
                                        dlg.dismiss();
                                        setLoading(false);
                                    })
                                    .setCancelable(false)
                                    .create();
                            dialog.show();
                            dialog.getButton(AlertDialog.BUTTON_POSITIVE).setBackgroundColor(getResources().getColor(R.color.bg_green));
                        });
                        return;
                    }

                    // ID exists, check verification status and year/section
                    for (DataSnapshot studentSnapshot : snapshot.getChildren()) {
                        Object isVerifiedRaw = studentSnapshot.child("isVerified").getValue();
                        Log.d(TAG, "checkStudentIdAndProceed: Found student by idNumber: " + studentIdNumber +
                                ", studentId: " + studentSnapshot.getKey() + ", raw isVerified: " + isVerifiedRaw);
                        boolean isVerified = (isVerifiedRaw instanceof Boolean) ? (Boolean) isVerifiedRaw : false;

                        if (isVerified) {
                            Log.w(TAG, "checkStudentIdAndProceed: idNumber is associated with a verified account, studentId: " +
                                    studentSnapshot.getKey());
                            runOnUiThread(() -> {
                                AlertDialog dialog = new AlertDialog.Builder(StudentSignUp.this)
                                        .setTitle("Account Already Verified")
                                        .setMessage("This ID already has a verified account. Please log in instead.")
                                        .setPositiveButton("OK", (dlg, which) -> {
                                            dlg.dismiss();
                                            setLoading(false);
                                            navigateToLogin();
                                        })
                                        .setCancelable(false)
                                        .create();
                                dialog.show();
                                dialog.getButton(AlertDialog.BUTTON_POSITIVE).setBackgroundColor(getResources().getColor(R.color.bg_green));
                            });
                            return;
                        }

                        // ID exists and is not verified, check year and section
                        Student student = studentSnapshot.getValue(Student.class);
                        if (student != null) {
                            String studentKey = studentSnapshot.getKey();
                            String studentYearLevel = student.getYearLevel();
                            String studentSection = student.getSection();
                            String yearLevelNumber = extractNumberFromYearLevel(yearLevel);

                            if ((studentYearLevel.equals(yearLevelNumber) || studentYearLevel.equalsIgnoreCase(yearLevel)) &&
                                    studentSection.trim().equalsIgnoreCase(section.trim())) {
                                // Valid match and not verified → update student data
                                Log.d(TAG, "checkStudentIdAndProceed: Year and section match for idNumber: " + studentIdNumber);
                                updateStudentData(studentKey, firstName, lastName, middleName, email, password);
                                return;
                            } else {
                                // ID found but wrong year/section
                                Log.w(TAG, "checkStudentIdAndProceed: Year or section mismatch for idNumber: " + studentIdNumber);
                                runOnUiThread(() -> {
                                    AlertDialog dialog = new AlertDialog.Builder(StudentSignUp.this)
                                            .setTitle("Invalid Information")
                                            .setMessage("The ID number exists, but the Year Level or Section you entered does not match our records.\n\nPlease check and try again.")
                                            .setPositiveButton("OK", (dlg, which) -> {
                                                dlg.dismiss();
                                                setLoading(false);
                                            })
                                            .setCancelable(false)
                                            .create();
                                    dialog.show();
                                    dialog.getButton(AlertDialog.BUTTON_POSITIVE).setBackgroundColor(getResources().getColor(R.color.bg_green));
                                });
                                return;
                            }
                        }
                    }

                    // If ID is not verified, proceed to email and name checks
                    if (onValidId != null) {
                        runOnUiThread(onValidId);
                    }
                })
                .addOnFailureListener(e -> {
                    Log.e(TAG, "checkStudentIdAndProceed: Error checking idNumber in Realtime Database: " + e.getMessage());
                    runOnUiThread(() -> {
                        Toast.makeText(StudentSignUp.this,
                                "Failed to validate ID number in database: " + e.getMessage(),
                                Toast.LENGTH_SHORT).show();
                        setLoading(false);
                    });
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

    // Update student data including middleName
    private void updateStudentData(String studentKey, String firstName, String lastName, String middleName, String email, String password) {
        int otp = (int) (Math.random() * 900000) + 100000; // Generate 6-digit OTP
        String hashedOtp = hashOtp(String.valueOf(otp));

        String fullName = middleName.isEmpty()
                ? lastName + ", " + firstName
                : lastName + ", " + firstName + " " + middleName.charAt(0) + ".";

        // Update database with student data
        Map<String, Object> studentUpdates = new HashMap<>();
        studentUpdates.put("email", email.toLowerCase());
        studentUpdates.put("firstName", firstName);
        studentUpdates.put("lastName", lastName);
        studentUpdates.put("middleName", middleName.isEmpty() ? "N/A" : middleName);
        studentUpdates.put("fullName", fullName);
        studentUpdates.put("role", "student");
        studentUpdates.put("otp", hashedOtp);

        databaseReference.child(studentKey).updateChildren(studentUpdates)
                .addOnSuccessListener(aVoid -> {
                    Log.d(TAG, "Updating student with studentKey: " + studentKey);
                    Log.d(TAG, "Student data updated (without email/password). Waiting for OTP email confirmation.");

                    SharedPreferences sharedPreferences = getSharedPreferences("MyAppPrefs", MODE_PRIVATE);
                    SharedPreferences.Editor editor = sharedPreferences.edit();
                    editor.putString("studentID", studentKey);
                    editor.putString("idNumber", idNumber.getText().toString().trim());
                    editor.putString("tempEmail", email);
                    editor.putString("tempPassword", password);
                    editor.apply();

                    // Send OTP email and let it handle navigation and loading state
                    sendOtpEmail(email, firstName, otp, studentKey, password);
                })
                .addOnFailureListener(e -> {
                    Log.e(TAG, "Failed to update student data: " + e.getMessage());
                    runOnUiThread(() -> Toast.makeText(StudentSignUp.this, "Failed to update student data!", Toast.LENGTH_SHORT).show());
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

    private void sendOtpEmail(String recipientEmail, String firstName, int otp, String studentKey, String password) {
        String subject = "Your MDF Event Verification Code";
        String message = "Dear " + firstName + ",\n\n"
                + "Your verification code is: " + otp + "\n\n"
                + "Enter this code in the app to verify your email.\n\n"
                + "Best Regards,\nMDF Event Management Team";

        ExecutorService executor = Executors.newSingleThreadExecutor();
        executor.execute(() -> {
            try {
                MailSender mailSender = new MailSender();
                mailSender.sendEmail(recipientEmail, subject, message);
                Log.d(TAG, "OTP Email Sent Successfully to " + recipientEmail);
                runOnUiThread(() -> {
                    navigateToOtpVerification(studentKey, recipientEmail, password);
                    setLoading(false); // Stop loading after successful email send
                });
            } catch (MessagingException e) {
                Log.e(TAG, "Failed to send OTP email: " + e.getMessage(), e);
                runOnUiThread(() -> {
                    AlertDialog.Builder builder = new AlertDialog.Builder(StudentSignUp.this)
                            .setTitle("Failed to Send OTP")
                            .setPositiveButton("Change Email", (dialog, which) -> {
                                etEmail.requestFocus();
                                dialog.dismiss();
                            })
                            .setNegativeButton("OK", (dialog, which) -> dialog.dismiss())
                            .setCancelable(false);
                    if (e.getMessage() != null) {
                        String errorMessage = e.getMessage().toLowerCase();
                        if (errorMessage.contains("address not found")) {
                            builder.setMessage("The email address was not found. Please check your email or use a different one.");
                        } else if (errorMessage.contains("inbox full") || errorMessage.contains("too much mail")) {
                            builder.setMessage("The recipient's inbox is full or receiving too much mail. Please try a different email address.");
                        } else {
                            builder.setMessage("Failed to send OTP: " + e.getMessage());
                        }
                    } else {
                        builder.setMessage("Failed to send OTP: Unknown error occurred.");
                    }
                    builder.show();
                    setLoading(false); // Stop loading after error dialog is shown
                });
            } finally {
                executor.shutdown();
            }
        });
    }
}