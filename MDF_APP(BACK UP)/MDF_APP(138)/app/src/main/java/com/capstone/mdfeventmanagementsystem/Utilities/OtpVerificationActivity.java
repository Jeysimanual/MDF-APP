package com.capstone.mdfeventmanagementsystem.Utilities;

import android.content.Intent;
import android.content.SharedPreferences;
import android.os.Bundle;
import android.os.CountDownTimer;
import android.util.Log;
import android.view.View;
import android.view.Window;
import android.view.WindowManager;
import android.widget.Button;
import android.widget.EditText;
import android.widget.ImageView;
import android.widget.ProgressBar;
import android.widget.TextView;
import android.widget.Toast;

import androidx.appcompat.app.AlertDialog;
import androidx.appcompat.app.AppCompatActivity;

import com.capstone.mdfeventmanagementsystem.R;
import com.capstone.mdfeventmanagementsystem.Student.MainActivity2;
import com.capstone.mdfeventmanagementsystem.Student.Student;
import com.capstone.mdfeventmanagementsystem.Student.StudentLogin;
import com.capstone.mdfeventmanagementsystem.Student.StudentSignUp;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseUser;
import com.google.firebase.database.DataSnapshot;
import com.google.firebase.database.DatabaseReference;
import com.google.firebase.database.FirebaseDatabase;

import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.util.HashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;

import javax.mail.MessagingException;

public class OtpVerificationActivity extends AppCompatActivity {
    private EditText etOtp;
    private Button btnVerify;
    private TextView tvResendOtp;
    private DatabaseReference databaseReference;
    private FirebaseAuth auth;
    private String studentId, email, password;
    private CountDownTimer countDownTimer;
    private ProgressBar verifyOtpProgressBar;
    private ProgressBar resendOtpProgressBar;
    private static final long RESEND_OTP_DELAY = 60000; // 1 minute
    private static final String TAG = "OtpVerificationTest";

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_otp_verification);

        Window window = getWindow();
        window.addFlags(WindowManager.LayoutParams.FLAG_DRAWS_SYSTEM_BAR_BACKGROUNDS);
        window.setStatusBarColor(getResources().getColor(R.color.bg_green, getTheme()));

        etOtp = findViewById(R.id.etOtp);
        btnVerify = findViewById(R.id.btnVerifyOtp);
        tvResendOtp = findViewById(R.id.tvResendOtp);
        verifyOtpProgressBar = findViewById(R.id.verifyOtpProgressBar);
        resendOtpProgressBar = findViewById(R.id.resendOtpProgressBar);
        ImageView btnCloseOtp = findViewById(R.id.btnCloseOtp);

        // Get data from Intent
        studentId = getIntent().getStringExtra("STUDENT_ID");
        email = getIntent().getStringExtra("EMAIL");
        password = getIntent().getStringExtra("PASSWORD");

        // Firebase Initialization
        databaseReference = FirebaseDatabase.getInstance().getReference("students");
        auth = FirebaseAuth.getInstance();

        btnVerify.setOnClickListener(view -> checkOtp());
        tvResendOtp.setOnClickListener(view -> resendOtp());

        btnCloseOtp.setOnClickListener(v -> {
            new androidx.appcompat.app.AlertDialog.Builder(this)
                    .setTitle("Cancel Signup")
                    .setMessage("Are you sure you want to cancel signup? Your progress will be lost.")
                    .setPositiveButton("Yes", (dialog, which) -> {
                        // Remove OTP from database before exiting
                        if (studentId != null) {
                            databaseReference.child(studentId).child("otp").removeValue()
                                    .addOnCompleteListener(task -> {
                                        Intent intent = new Intent(OtpVerificationActivity.this, StudentLogin.class);
                                        startActivity(intent);
                                        finish();
                                    });
                        } else {
                            Intent intent = new Intent(OtpVerificationActivity.this, StudentLogin.class);
                            startActivity(intent);
                            finish();
                        }
                    })
                    .setNegativeButton("No", (dialog, which) -> dialog.dismiss())
                    .show();
        });

        startResendOtpCountdown();
    }

    @Override
    public void onBackPressed() {
        new AlertDialog.Builder(this)
                .setTitle("Cancel Signup")
                .setMessage("Are you sure you want to cancel signup? Your progress will be lost.")
                .setPositiveButton("Yes", (dialog, which) -> {
                    if (studentId != null) {
                        databaseReference.child(studentId).child("otp").removeValue()
                                .addOnCompleteListener(task -> {
                                    Intent intent = new Intent(OtpVerificationActivity.this, StudentLogin.class);
                                    startActivity(intent);
                                    super.onBackPressed(); // Call super to ensure proper activity lifecycle handling
                                });
                    } else {
                        Intent intent = new Intent(OtpVerificationActivity.this, StudentLogin.class);
                        startActivity(intent);
                        super.onBackPressed(); // Call super to ensure proper activity lifecycle handling
                    }
                })
                .setNegativeButton("No", (dialog, which) -> dialog.dismiss())
                .show();
    }



    private void startResendOtpCountdown() {
        if (countDownTimer != null) {
            countDownTimer.cancel();
        }

        tvResendOtp.setEnabled(false);
        tvResendOtp.setText("Resend OTP in 1:00");

        countDownTimer = new CountDownTimer(RESEND_OTP_DELAY, 1000) {
            @Override
            public void onTick(long millisUntilFinished) {
                long seconds = (millisUntilFinished / 1000) % 60;
                long minutes = (millisUntilFinished / 1000) / 60;
                tvResendOtp.setText(String.format(Locale.getDefault(), "Resend OTP in %d:%02d", minutes, seconds));
            }

            @Override
            public void onFinish() {
                tvResendOtp.setText("Didn't receive OTP? Resend");
                tvResendOtp.setEnabled(true);
            }
        }.start();
    }

    private void resendOtp() {
        Log.d(TAG, "resendOtp: Starting resend OTP process");
        // Cancel any existing timer to prevent overlap
        if (countDownTimer != null) {
            countDownTimer.cancel();
            countDownTimer = null;
        }

        // Show loading indicator
        runOnUiThread(() -> {
            resendOtpProgressBar.setVisibility(View.VISIBLE);
            tvResendOtp.setText("");
            tvResendOtp.setEnabled(false);
        });

        int newOtp = (int) (Math.random() * 900000) + 100000;
        String hashedOtp = hashOtp(String.valueOf(newOtp));

        databaseReference.child(studentId).get().addOnSuccessListener(snapshot -> {
            if (!snapshot.exists()) {
                runOnUiThread(() -> {
                    Log.w(TAG, "resendOtp: User not found for studentId: " + studentId);
                    Toast.makeText(OtpVerificationActivity.this, "User not found!", Toast.LENGTH_SHORT).show();
                    resendOtpProgressBar.setVisibility(View.GONE);
                    tvResendOtp.setText("Didn't receive OTP? Resend");
                    tvResendOtp.setEnabled(true);
                });
                return;
            }

            final String firstName = snapshot.child("firstName").getValue(String.class);
            String resolvedFirstName = (firstName != null && !firstName.isEmpty()) ? firstName : "User";

            databaseReference.child(studentId).child("otp").setValue(hashedOtp)
                    .addOnSuccessListener(aVoid -> {
                        Log.d(TAG, "resendOtp: OTP updated successfully for studentId: " + studentId);
                        sendOtpEmail(email, newOtp, resolvedFirstName);
                    })
                    .addOnFailureListener(e -> {
                        runOnUiThread(() -> {
                            Log.e(TAG, "resendOtp: Failed to update OTP for studentId: " + studentId + ", error: " + e.getMessage());
                            Toast.makeText(OtpVerificationActivity.this, "Failed to resend OTP!", Toast.LENGTH_SHORT).show();
                            resendOtpProgressBar.setVisibility(View.GONE);
                            tvResendOtp.setText("Didn't receive OTP? Resend");
                            tvResendOtp.setEnabled(true);
                        });
                    });
        }).addOnFailureListener(e -> {
            runOnUiThread(() -> {
                Log.e(TAG, "resendOtp: Error fetching user details for studentId: " + studentId + ", error: " + e.getMessage());
                Toast.makeText(OtpVerificationActivity.this, "Error fetching user details!", Toast.LENGTH_SHORT).show();
                resendOtpProgressBar.setVisibility(View.GONE);
                tvResendOtp.setText("Didn't receive OTP? Resend");
                tvResendOtp.setEnabled(true);
            });
        });
    }

    private void sendOtpEmail(String recipientEmail, int otp, String firstName) {
        Log.d(TAG, "sendOtpEmail: Sending OTP email to " + recipientEmail);
        String subject = "Your MDF Event Verification Code";
        String message = "Dear " + firstName + ",\n\n"
                + "Your new verification code is: " + otp + "\n\n"
                + "Enter this code in the app to verify your email.\n\n"
                + "Best Regards,\nMDF Event Management Team";

        new Thread(() -> {
            try {
                MailSender mailSender = new MailSender();
                mailSender.sendEmail(recipientEmail, subject, message);
                Log.d(TAG, "sendOtpEmail: OTP Email Sent Successfully to " + recipientEmail);
                runOnUiThread(() -> {
                    Toast.makeText(OtpVerificationActivity.this,
                            "OTP sent to " + recipientEmail, Toast.LENGTH_SHORT).show();
                    resendOtpProgressBar.setVisibility(View.GONE);
                    tvResendOtp.setText("Didn't receive OTP? Resend");
                    tvResendOtp.setEnabled(false); // Disable until timer finishes
                    startResendOtpCountdown();
                });
            } catch (MessagingException e) {
                Log.e(TAG, "sendOtpEmail: Failed to send OTP email: " + e.getMessage(), e);
                runOnUiThread(() -> {
                    resendOtpProgressBar.setVisibility(View.GONE);
                    tvResendOtp.setText("Didn't receive OTP? Resend");
                    tvResendOtp.setEnabled(true);
                    AlertDialog.Builder builder = new AlertDialog.Builder(OtpVerificationActivity.this)
                            .setTitle("Failed to Send OTP")
                            .setPositiveButton("Change Email", (dialog, which) -> {
                                // Navigate back to StudentSignUp to allow email correction
                                Intent intent = new Intent(OtpVerificationActivity.this, StudentSignUp.class);
                                // Pass form data to preserve user input
                                intent.putExtra("EMAIL", recipientEmail);
                                intent.putExtra("FIRST_NAME", firstName);
                                intent.putExtra("STUDENT_ID", studentId);
                                intent.putExtra("PASSWORD", password);
                                // Fetch middleName from database
                                databaseReference.child(studentId).get().addOnSuccessListener(snapshot -> {
                                    String middleName = snapshot.child("middleName").getValue(String.class);
                                    intent.putExtra("MIDDLE_NAME", middleName != null ? middleName : "N/A");
                                    startActivity(intent);
                                    dialog.dismiss();
                                    finish();
                                }).addOnFailureListener(ex -> {
                                    Log.e(TAG, "sendOtpEmail: Failed to fetch middleName: " + ex.getMessage());
                                    // Fallback: Proceed without middleName
                                    intent.putExtra("MIDDLE_NAME", "N/A");
                                    startActivity(intent);
                                    dialog.dismiss();
                                    finish();
                                });
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
                });
            }
        }).start();
    }


    private void checkOtp() {
        String enteredOtp = etOtp.getText().toString().trim();
        Log.d(TAG, "checkOtp: Started OTP verification for email: " + email + ", studentId: " + studentId);

        if (enteredOtp.isEmpty()) {
            Log.w(TAG, "checkOtp: OTP is empty");
            Toast.makeText(this, "Please enter the OTP", Toast.LENGTH_SHORT).show();
            return;
        }

        // Show loading indicator
        verifyOtpProgressBar.setVisibility(View.VISIBLE);
        btnVerify.setText("");
        btnVerify.setEnabled(false);

        // Check if the email is already associated with a verified account
        DatabaseReference studentsRef = FirebaseDatabase.getInstance().getReference("students");
        Log.d(TAG, "checkOtp: Querying Realtime Database for email: " + email.toLowerCase());
        studentsRef.orderByChild("email").equalTo(email.toLowerCase()).get().addOnSuccessListener(snapshot -> {
            if (snapshot.exists()) {
                for (DataSnapshot studentSnapshot : snapshot.getChildren()) {
                    Student student = studentSnapshot.getValue(Student.class);
                    if (student != null && student.isVerified()) {
                        Log.w(TAG, "checkOtp: Email is associated with a verified account, studentId: " + studentSnapshot.getKey());
                        Toast.makeText(OtpVerificationActivity.this,
                                "This email is already associated with a verified account! Please log in.",
                                Toast.LENGTH_LONG).show();
                        verifyOtpProgressBar.setVisibility(View.GONE);
                        btnVerify.setText("Verify OTP");
                        btnVerify.setEnabled(true);
                        navigateToLogin();
                        return;
                    }
                }
                Log.d(TAG, "checkOtp: No verified account found for email, proceeding with OTP check");
            } else {
                Log.d(TAG, "checkOtp: No student record found for email: " + email.toLowerCase());
            }

            // Proceed with OTP verification
            Log.d(TAG, "checkOtp: Verifying OTP for studentId: " + studentId);
            databaseReference.child(studentId).get().addOnSuccessListener(studentSnapshot -> {
                if (!studentSnapshot.exists()) {
                    Log.e(TAG, "checkOtp: Student not found for studentId: " + studentId);
                    Toast.makeText(OtpVerificationActivity.this, "User not found!", Toast.LENGTH_SHORT).show();
                    verifyOtpProgressBar.setVisibility(View.GONE);
                    btnVerify.setText("Verify OTP");
                    btnVerify.setEnabled(true);
                    return;
                }

                String storedHashedOtp = studentSnapshot.child("otp").getValue(String.class);
                String hashedEnteredOtp = hashOtp(enteredOtp);
                Log.d(TAG, "checkOtp: Comparing OTP - stored: " + storedHashedOtp + ", entered (hashed): " + hashedEnteredOtp);
                if (storedHashedOtp == null || !storedHashedOtp.equals(hashedEnteredOtp)) {
                    Log.w(TAG, "checkOtp: Incorrect OTP for studentId: " + studentId);
                    Toast.makeText(OtpVerificationActivity.this,
                            "Incorrect OTP! Please try again.",
                            Toast.LENGTH_SHORT).show();
                    verifyOtpProgressBar.setVisibility(View.GONE);
                    btnVerify.setText("Verify OTP");
                    btnVerify.setEnabled(true);
                    return;
                }

                Log.d(TAG, "checkOtp: OTP verified successfully, proceeding to complete registration");
                completeRegistration();
            }).addOnFailureListener(e -> {
                Log.e(TAG, "checkOtp: Error verifying OTP for studentId: " + studentId + ", error: " + e.getMessage());
                Toast.makeText(OtpVerificationActivity.this,
                        "Error verifying OTP. Try again!",
                        Toast.LENGTH_SHORT).show();
                verifyOtpProgressBar.setVisibility(View.GONE);
                btnVerify.setText("Verify OTP");
                btnVerify.setEnabled(true);
            });
        }).addOnFailureListener(e -> {
            Log.e(TAG, "checkOtp: Error checking email verification status: " + e.getMessage());
            Toast.makeText(OtpVerificationActivity.this,
                    "Error checking email status. Try again!",
                    Toast.LENGTH_SHORT).show();
            verifyOtpProgressBar.setVisibility(View.GONE);
            btnVerify.setText("Verify OTP");
            btnVerify.setEnabled(true);
        });
    }

    private void completeRegistration() {
        Log.d(TAG, "completeRegistration: Starting registration for email: " + email.toLowerCase() + ", studentId: " + studentId);

        // Check if the email is already registered in Firebase Authentication
        auth.fetchSignInMethodsForEmail(email.toLowerCase()).addOnCompleteListener(task -> {
            if (task.isSuccessful()) {
                List<String> signInMethods = task.getResult().getSignInMethods();
                Log.d(TAG, "completeRegistration: Fetch sign-in methods result - email: " + email.toLowerCase() + ", methods: " + (signInMethods != null ? signInMethods.toString() : "null"));

                if (signInMethods != null && !signInMethods.isEmpty()) {
                    // Email is already registered in Firebase Authentication
                    Log.d(TAG, "completeRegistration: Email exists in Firebase Auth, attempting to sign in and delete user");
                    auth.signInWithEmailAndPassword(email.toLowerCase(), password).addOnCompleteListener(signInTask -> {
                        if (signInTask.isSuccessful()) {
                            FirebaseUser user = auth.getCurrentUser();
                            if (user != null) {
                                Log.d(TAG, "completeRegistration: Signed in successfully, deleting user with UID: " + user.getUid());
                                user.delete().addOnCompleteListener(deleteTask -> {
                                    if (deleteTask.isSuccessful()) {
                                        Log.d(TAG, "completeRegistration: Existing user deleted successfully");
                                        createNewUser();
                                    } else {
                                        Log.e(TAG, "completeRegistration: Failed to delete existing user: " + deleteTask.getException().getMessage());
                                        Toast.makeText(OtpVerificationActivity.this,
                                                "Error updating account: " + deleteTask.getException().getMessage(),
                                                Toast.LENGTH_LONG).show();
                                        navigateToLogin();
                                    }
                                });
                            } else {
                                Log.e(TAG, "completeRegistration: Sign-in succeeded but user is null");
                                Toast.makeText(OtpVerificationActivity.this,
                                        "Error processing account. Please try again.",
                                        Toast.LENGTH_LONG).show();
                                navigateToLogin();
                            }
                        } else {
                            Log.w(TAG, "completeRegistration: Sign-in failed: " + signInTask.getException().getMessage());
                            // Since isVerified is false, proceed to create new user as fallback
                            Log.d(TAG, "completeRegistration: Sign-in failed, attempting to create new user as fallback");
                            createNewUser();
                        }
                    });
                } else {
                    // Email is not registered or fetchSignInMethodsForEmail failed to detect it
                    Log.d(TAG, "completeRegistration: Email not registered or undetected, attempting to create new user");
                    createNewUser();
                }
            } else {
                Log.e(TAG, "completeRegistration: Error checking email in Firebase Auth: " + task.getException().getMessage());
                // Fallback to createNewUser in case fetchSignInMethodsForEmail fails
                Log.d(TAG, "completeRegistration: Fallback to create new user due to fetch error");
                createNewUser();
            }
        });
    }

    private void createNewUser() {
        Log.d(TAG, "createNewUser: Creating new Firebase Auth user for email: " + email.toLowerCase());
        auth.createUserWithEmailAndPassword(email.toLowerCase(), password)
                .addOnCompleteListener(createTask -> {
                    if (createTask.isSuccessful()) {
                        Log.d(TAG, "createNewUser: Firebase Auth user created successfully for email: " + email.toLowerCase());
                        updateDatabaseAfterVerification();
                    } else {
                        Log.e(TAG, "createNewUser: Error creating Firebase Auth user: " + createTask.getException().getMessage());
                        if (createTask.getException().getMessage().contains("The email address is already in use")) {
                            Log.d(TAG, "createNewUser: Email already in use, attempting to delete existing user");
                            auth.signInWithEmailAndPassword(email.toLowerCase(), password).addOnCompleteListener(signInTask -> {
                                if (signInTask.isSuccessful()) {
                                    FirebaseUser user = auth.getCurrentUser();
                                    if (user != null) {
                                        Log.d(TAG, "createNewUser: Signed in successfully, deleting user with UID: " + user.getUid());
                                        user.delete().addOnCompleteListener(deleteTask -> {
                                            if (deleteTask.isSuccessful()) {
                                                Log.d(TAG, "createNewUser: Existing user deleted successfully, retrying user creation");
                                                retryCreateNewUser();
                                            } else {
                                                Log.e(TAG, "createNewUser: Failed to delete existing user: " + deleteTask.getException().getMessage());
                                                Toast.makeText(OtpVerificationActivity.this,
                                                        "Error updating account: " + deleteTask.getException().getMessage(),
                                                        Toast.LENGTH_LONG).show();
                                                navigateToLogin();
                                            }
                                        });
                                    } else {
                                        Log.e(TAG, "createNewUser: Sign-in succeeded but user is null");
                                        Toast.makeText(OtpVerificationActivity.this,
                                                "Error processing account. Please try again.",
                                                Toast.LENGTH_LONG).show();
                                        navigateToLogin();
                                    }
                                } else {
                                    Log.w(TAG, "createNewUser: Sign-in failed: " + signInTask.getException().getMessage());
                                    // Fallback: Try anonymous sign-in to delete user
                                    Log.d(TAG, "createNewUser: Sign-in failed, attempting anonymous sign-in to delete user");
                                    auth.signInAnonymously().addOnCompleteListener(anonTask -> {
                                        if (anonTask.isSuccessful()) {
                                            Log.d(TAG, "createNewUser: Anonymous sign-in successful, attempting to delete existing user");
                                            FirebaseUser anonUser = auth.getCurrentUser();
                                            if (anonUser != null) {
                                                anonUser.delete().addOnCompleteListener(anonDeleteTask -> {
                                                    if (anonDeleteTask.isSuccessful()) {
                                                        Log.d(TAG, "createNewUser: Anonymous user deleted, retrying user creation");
                                                        retryCreateNewUser();
                                                    } else {
                                                        Log.e(TAG, "createNewUser: Failed to delete user via anonymous auth: " + anonDeleteTask.getException().getMessage());
                                                        Toast.makeText(OtpVerificationActivity.this,
                                                                "Error updating account: " + anonDeleteTask.getException().getMessage(),
                                                                Toast.LENGTH_LONG).show();
                                                        navigateToLogin();
                                                    }
                                                });
                                            } else {
                                                Log.e(TAG, "createNewUser: Anonymous sign-in succeeded but user is null");
                                                Toast.makeText(OtpVerificationActivity.this,
                                                        "Error processing account. Please try again.",
                                                        Toast.LENGTH_LONG).show();
                                                navigateToLogin();
                                            }
                                        } else {
                                            Log.e(TAG, "createNewUser: Anonymous sign-in failed: " + anonTask.getException().getMessage());
                                            Toast.makeText(OtpVerificationActivity.this,
                                                    "Error creating account: " + createTask.getException().getMessage(),
                                                    Toast.LENGTH_LONG).show();
                                            navigateToLogin();
                                        }
                                    });
                                }
                            });
                        } else {
                            Toast.makeText(OtpVerificationActivity.this,
                                    "Error creating account: " + createTask.getException().getMessage(),
                                    Toast.LENGTH_LONG).show();
                            navigateToLogin();
                        }
                    }
                });
    }

    private void retryCreateNewUser() {
        Log.d(TAG, "retryCreateNewUser: Retrying user creation for email: " + email.toLowerCase());
        auth.createUserWithEmailAndPassword(email.toLowerCase(), password)
                .addOnCompleteListener(retryTask -> {
                    if (retryTask.isSuccessful()) {
                        Log.d(TAG, "retryCreateNewUser: Firebase Auth user created successfully for email: " + email.toLowerCase());
                        updateDatabaseAfterVerification();
                    } else {
                        Log.e(TAG, "retryCreateNewUser: Error creating Firebase Auth user: " + retryTask.getException().getMessage());
                        Toast.makeText(OtpVerificationActivity.this,
                                "Error creating account after retry: " + retryTask.getException().getMessage(),
                                Toast.LENGTH_LONG).show();
                        navigateToLogin();
                    }
                });
    }

    private void updateDatabaseAfterVerification() {
        Log.d(TAG, "updateDatabaseAfterVerification: Updating Realtime Database for studentId: " + studentId);
        Map<String, Object> updates = new HashMap<>();
        updates.put("email", email.toLowerCase());
        updates.put("isVerified", true);
        updates.put("otp", null);

        databaseReference.child(studentId).updateChildren(updates)
                .addOnSuccessListener(aVoid -> {
                    Log.d(TAG, "updateDatabaseAfterVerification: Student data updated successfully for studentId: " + studentId + ", email: " + email.toLowerCase());
                    // Clear MyAppPrefs
                    getSharedPreferences("MyAppPrefs", MODE_PRIVATE).edit().clear().apply();

                    // Save minimal session data to facilitate login
                    SharedPreferences userSession = getSharedPreferences("UserSession", MODE_PRIVATE);
                    SharedPreferences.Editor editor = userSession.edit();
                    editor.putString("email", email.toLowerCase());
                    editor.putString("studentID", studentId);
                    editor.apply();

                    Log.d(TAG, "updateDatabaseAfterVerification: Session data saved, navigating to login");
                    Toast.makeText(OtpVerificationActivity.this,
                            "Account verified successfully! Please log in.",
                            Toast.LENGTH_LONG).show();
                    navigateToLogin();
                })
                .addOnFailureListener(e -> {
                    Log.e(TAG, "updateDatabaseAfterVerification: Failed to update student data for studentId: " + studentId + ", error: " + e.getMessage());
                    Toast.makeText(OtpVerificationActivity.this,
                            "Verification failed: " + e.getMessage(),
                            Toast.LENGTH_SHORT).show();
                    navigateToLogin();
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

    private void navigateToLogin() {
        Intent intent = new Intent(OtpVerificationActivity.this, StudentLogin.class);
        startActivity(intent);
        finish();
    }

    @Override
    protected void onDestroy() {
        if (countDownTimer != null) {
            countDownTimer.cancel();
        }
        super.onDestroy();
    }
}
