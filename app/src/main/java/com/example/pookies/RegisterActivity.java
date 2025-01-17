package com.example.pookies;

import android.content.Intent;
import android.content.SharedPreferences;
import android.database.sqlite.SQLiteDatabase;
import android.os.Bundle;
import android.text.TextUtils;
import android.view.View;
import android.widget.Button;
import android.widget.EditText;
import android.widget.TextView;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;

import com.google.android.gms.tasks.OnCompleteListener;
import com.google.android.gms.tasks.Task;
import com.google.firebase.auth.AuthResult;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.UserProfileChangeRequest;

public class RegisterActivity extends AppCompatActivity {

    EditText etEmail, etName, etPassword, etConfirmPassword;
    Button btnSignUp;
    TextView tvAlreadyHaveAccount;
    DBHelper dbHelper;
    FirebaseAuth mAuth;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_register);

        dbHelper = new DBHelper(this);  // Initialize SQLite DB Helper
        mAuth = FirebaseAuth.getInstance();  // Initialize Firebase Auth

        // Initialize views
        etEmail = findViewById(R.id.emailInput);
        etName = findViewById(R.id.nameInput);
        etPassword = findViewById(R.id.passwordInput);
        etConfirmPassword = findViewById(R.id.confirmPasswordInput);
        btnSignUp = findViewById(R.id.signupButton);
        tvAlreadyHaveAccount = findViewById(R.id.alreadyHaveAccount);

        // Navigate to LoginActivity if user already has an account
        tvAlreadyHaveAccount.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                Intent intent = new Intent(RegisterActivity.this, LoginActivity.class);
                startActivity(intent);
            }
        });

        // Register user when sign up button is clicked
        btnSignUp.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                registerUser();
            }
        });
    }

    private void registerUser() {
        final String email = etEmail.getText().toString().trim();
        final String name = etName.getText().toString().trim();
        String password = etPassword.getText().toString().trim();
        String confirmPassword = etConfirmPassword.getText().toString().trim();

        // Basic validation
        if (TextUtils.isEmpty(email) || TextUtils.isEmpty(name) || TextUtils.isEmpty(password) || TextUtils.isEmpty(confirmPassword)) {
            Toast.makeText(RegisterActivity.this, "All fields are required", Toast.LENGTH_SHORT).show();
            return;
        }

        if (!password.equals(confirmPassword)) {
            Toast.makeText(RegisterActivity.this, "Passwords do not match", Toast.LENGTH_SHORT).show();
            return;
        }

        // Check if the user already exists in SQLite
        User existingUser = dbHelper.getUserByEmail(email);
        if (existingUser != null) {
            Toast.makeText(RegisterActivity.this, "User already exists with this email", Toast.LENGTH_SHORT).show();
            return;
        }

        // Register user with Firebase
        mAuth.createUserWithEmailAndPassword(email, password)
                .addOnCompleteListener(this, new OnCompleteListener<AuthResult>() {
                    @Override
                    public void onComplete(@NonNull Task<AuthResult> task) {
                        if (task.isSuccessful()) {
                            // Firebase registration successful, update Firebase profile and register user in SQLite
                            updateFirebaseProfile(name);
                            registerInSQLite(email, name, password);
                        } else {
                            // Firebase registration failed
                            Toast.makeText(RegisterActivity.this, "Registration failed: " + task.getException().getMessage(), Toast.LENGTH_SHORT).show();
                        }
                    }
                });
    }

    // Update Firebase profile with the user's name
    private void updateFirebaseProfile(String name) {
        UserProfileChangeRequest profileUpdates = new UserProfileChangeRequest.Builder()
                .setDisplayName(name)
                .build();

        mAuth.getCurrentUser().updateProfile(profileUpdates)
                .addOnCompleteListener(new OnCompleteListener<Void>() {
                    @Override
                    public void onComplete(@NonNull Task<Void> task) {
                        if (!task.isSuccessful()) {
                            Toast.makeText(RegisterActivity.this, "Failed to update profile", Toast.LENGTH_SHORT).show();
                        }
                    }
                });
    }

    // Register user in SQLite database
    private void registerInSQLite(String email, String name, String password) {
        boolean success = dbHelper.insertUser(email, name, password);
        if (success) {
            Toast.makeText(RegisterActivity.this, "Registration successful", Toast.LENGTH_SHORT).show();
            // Registration successful, save user ID in SharedPreferences for session management
            getSharedPreferences("APP_PREFS", MODE_PRIVATE)
                    .edit()
                    .putString("USER_ID", email)
                    .apply();

            // Redirect to LoginActivity
            Intent intent = new Intent(RegisterActivity.this, LoginActivity.class);
            startActivity(intent);
            finish();
        } else {
            // SQLite registration failed, rollback Firebase user creation
            Toast.makeText(RegisterActivity.this, "SQLite registration failed", Toast.LENGTH_SHORT).show();
            if (mAuth.getCurrentUser() != null) {
                mAuth.getCurrentUser().delete();  // Remove the user from Firebase
            }
        }
    }
}
