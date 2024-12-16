package com.example.pookies;

import android.content.Intent;
import android.os.Bundle;
import android.text.TextUtils;
import android.widget.Button;
import android.widget.EditText;
import android.widget.TextView;
import android.widget.Toast;

import androidx.appcompat.app.AppCompatActivity;

import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseUser;
import com.google.firebase.database.DatabaseReference;
import com.google.firebase.database.FirebaseDatabase;

import java.util.HashMap;
import java.util.Map;

public class RegisterActivity extends AppCompatActivity {

    private EditText etEmail, etName, etPassword, etConfirmPassword;
    private Button btnSignUp;
    private TextView tvAlreadyHaveAccount;

    private FirebaseAuth mAuth;
    private DatabaseReference usersDatabaseRef;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_register);

        mAuth = FirebaseAuth.getInstance();
        usersDatabaseRef = FirebaseDatabase.getInstance().getReference("users"); // Reference to 'users' table

        // Initialize views
        etEmail = findViewById(R.id.emailInput);
        etName = findViewById(R.id.nameInput);
        etPassword = findViewById(R.id.passwordInput);
        etConfirmPassword = findViewById(R.id.confirmPasswordInput);
        btnSignUp = findViewById(R.id.signupButton);
        tvAlreadyHaveAccount = findViewById(R.id.alreadyHaveAccount);

        // Navigate to LoginActivity if user already has an account
        tvAlreadyHaveAccount.setOnClickListener(v -> {
            Intent intent = new Intent(RegisterActivity.this, LoginActivity.class);
            startActivity(intent);
        });

        // Register user when sign up button is clicked
        btnSignUp.setOnClickListener(v -> registerUser());
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

        // Register user with Firebase
        mAuth.createUserWithEmailAndPassword(email, password)
                .addOnCompleteListener(this, task -> {
                    if (task.isSuccessful()) {
                        // Firebase registration successful
                        FirebaseUser user = mAuth.getCurrentUser();
                        if (user != null) {
                            saveUserData(user.getUid(), name, email, password); // Save user info in database
                        }
                    } else {
                        // Firebase registration failed
                        Toast.makeText(RegisterActivity.this, "Registration failed: " + task.getException().getMessage(), Toast.LENGTH_SHORT).show();
                    }
                });
    }

    private void saveUserData(String userId, String name, String email, String password) {
        // Prepare user data to be saved
        Map<String, Object> userData = new HashMap<>();
        userData.put("name", name);
        userData.put("email", email);
        userData.put("profilePicture", null);
        userData.put("password", password);

        // Save user data under 'users/{userId}/info'
        usersDatabaseRef.child(userId).child("info").setValue(userData)
                .addOnCompleteListener(task -> {
                    if (task.isSuccessful()) {
                        Toast.makeText(RegisterActivity.this, "Registration successful", Toast.LENGTH_SHORT).show();
                        redirectToLogin();
                    } else {
                        Toast.makeText(RegisterActivity.this, "Failed to save user data", Toast.LENGTH_SHORT).show();
                    }
                });
    }

    private void redirectToLogin() {
        Intent intent = new Intent(RegisterActivity.this, LoginActivity.class);
        startActivity(intent);
        finish();
    }
}