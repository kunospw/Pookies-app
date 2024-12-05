package com.example.pookies;

import android.content.Intent;
import android.os.Bundle;
import android.text.TextUtils;
import android.view.View;
import android.widget.Button;
import android.widget.EditText;
import android.widget.TextView;
import android.widget.Toast;
import android.util.Log;

import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;

import com.google.android.material.textfield.TextInputEditText;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseUser;
import com.google.firebase.database.DataSnapshot;
import com.google.firebase.database.DatabaseError;
import com.google.firebase.database.DatabaseReference;
import com.google.firebase.database.FirebaseDatabase;
import com.google.firebase.database.ValueEventListener;


public class LoginActivity extends AppCompatActivity {

    private EditText etEmail, etPassword;
    private Button btnLogin;
    private TextView tvSignUp, tvForgotPassword;
    private FirebaseAuth mAuth;
    private DatabaseReference mDatabase;
    private SessionManager sessionManager;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_login);
        Log.d("LoginActivity", "onCreate: Checking session state");

        // Initialize Firebase
        mAuth = FirebaseAuth.getInstance();
        mDatabase = FirebaseDatabase.getInstance().getReference();
        sessionManager = SessionManager.getInstance(this);

        // Check if user is already logged in
        if (sessionManager.isLoggedIn() && !sessionManager.isUserJustRegistered()) {
            Log.d("LoginActivity", "User is already logged in, navigating to ChatActivity");
            navigateToChatActivity();
            return;
        }

        initializeViews();
        setupClickListeners();
    }

    private void initializeViews() {
        etEmail = findViewById(R.id.emailInput);
        etPassword = findViewById(R.id.passwordInput);
        btnLogin = findViewById(R.id.loginButton);
        tvSignUp = findViewById(R.id.alreadyHaveAccount);
        tvForgotPassword = findViewById(R.id.forgotPassword);
    }

    private void setupClickListeners() {
        tvSignUp.setOnClickListener(v -> {
            Intent intent = new Intent(LoginActivity.this, RegisterActivity.class);
            startActivity(intent);
        });

        tvForgotPassword.setOnClickListener(v -> showForgotPasswordDialog());

        btnLogin.setOnClickListener(v -> {
            String email = etEmail.getText().toString().trim();
            String password = etPassword.getText().toString().trim();

            if (validateInput(email, password)) {
                loginUser(email, password);
            }
        });
    }

    private boolean validateInput(String email, String password) {
        if (TextUtils.isEmpty(email) || TextUtils.isEmpty(password)) {
            Toast.makeText(this, "Please enter both email and password", Toast.LENGTH_SHORT).show();
            return false;
        }
        return true;
    }

    private void loginUser(String email, String password) {
        mAuth.signInWithEmailAndPassword(email, password)
                .addOnCompleteListener(this, task -> {
                    if (task.isSuccessful()) {
                        FirebaseUser firebaseUser = mAuth.getCurrentUser();
                        if (firebaseUser != null) {
                            // Verify user in Realtime Database
                            verifyUserInDatabase(firebaseUser);
                        }
                    } else {
                        // Login failed
                        showLoginFailedDialog(task.getException());
                    }
                });
    }

    private void verifyUserInDatabase(FirebaseUser firebaseUser) {
        mDatabase.child("user-info").child(firebaseUser.getUid())
                .addListenerForSingleValueEvent(new ValueEventListener() {
                    @Override
                    public void onDataChange(@NonNull DataSnapshot snapshot) {
                        if (snapshot.exists()) {
                            // User exists in database, proceed with login
                            User user = snapshot.getValue(User.class);
                            if (user != null && user.isActive()) {
                                // Create session
                                sessionManager.createLoginSession(user.getEmail(), user.getUid(), user.getName());

                                // Clear the "just registered" flag
                                sessionManager.clearRegisteredFlag();

                                // Update user's active status
                                updateUserActiveStatus(user.getUid(), true);

                                Toast.makeText(LoginActivity.this,
                                        "Login successful!", Toast.LENGTH_SHORT).show();
                                navigateToChatActivity();
                            } else {
                                // User account is not active
                                Log.d("LoginActivity", "User account is not active, signing out");
                                mAuth.signOut();
                                Toast.makeText(LoginActivity.this,
                                        "Account is not active", Toast.LENGTH_SHORT).show();
                            }
                        } else {
                            // User not found in database
                            mAuth.signOut();
                            Toast.makeText(LoginActivity.this,
                                    "User data not found", Toast.LENGTH_SHORT).show();
                        }
                    }

                    @Override
                    public void onCancelled(@NonNull DatabaseError error) {
                        // Database error
                        mAuth.signOut();
                        Toast.makeText(LoginActivity.this,
                                "Authentication failed", Toast.LENGTH_SHORT).show();
                    }
                });
    }

    private void updateUserActiveStatus(String userId, boolean isActive) {
        mDatabase.child("user-info").child(userId)
                .child("active").setValue(isActive);
    }

    private void navigateToChatActivity() {
        Intent intent = new Intent(LoginActivity.this, ChatActivity.class);
        intent.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK | Intent.FLAG_ACTIVITY_CLEAR_TASK);
        startActivity(intent);
        finish();
    }

    private void showForgotPasswordDialog() {
        // Similar to previous implementation, but potentially updated to match new structure
        android.app.AlertDialog.Builder builder = new android.app.AlertDialog.Builder(this);
        View dialogView = getLayoutInflater().inflate(R.layout.dialog_forgot_password, null);
        builder.setView(dialogView);

        final TextInputEditText emailInput = dialogView.findViewById(R.id.emailInput);

        builder.setTitle("Forgot Password")
                .setPositiveButton("Reset", null)
                .setNegativeButton("Cancel", (dialog, which) -> dialog.cancel());

        android.app.AlertDialog dialog = builder.create();

        dialog.setOnShowListener(dialogInterface -> {
            Button button = dialog.getButton(android.app.AlertDialog.BUTTON_POSITIVE);
            button.setOnClickListener(view -> {
                String email = emailInput.getText().toString().trim();
                if (!TextUtils.isEmpty(email)) {
                    resetPassword(email);
                    dialog.dismiss();
                } else {
                    Toast.makeText(LoginActivity.this,
                            "Please enter your email", Toast.LENGTH_SHORT).show();
                }
            });
        });

        dialog.show();
    }

    private void resetPassword(String email) {
        mAuth.sendPasswordResetEmail(email)
                .addOnCompleteListener(task -> {
                    if (task.isSuccessful()) {
                        Toast.makeText(LoginActivity.this,
                                "Password reset email sent", Toast.LENGTH_SHORT).show();
                    } else {
                        Toast.makeText(LoginActivity.this,
                                "Failed to send reset email", Toast.LENGTH_SHORT).show();
                    }
                });
    }

    private void showLoginFailedDialog(Exception exception) {
        String errorMessage = exception != null ?
                exception.getMessage() : "Authentication failed";

        new android.app.AlertDialog.Builder(LoginActivity.this)
                .setTitle("Login Failed")
                .setMessage(errorMessage)
                .setPositiveButton("OK", (dialog, which) -> dialog.dismiss())
                .show();
    }
}