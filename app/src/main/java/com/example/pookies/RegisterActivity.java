package com.example.pookies;

import android.content.Intent;
import android.os.Bundle;
import android.text.TextUtils;
import android.widget.Button;
import android.widget.EditText;
import android.widget.TextView;
import android.widget.Toast;
import androidx.appcompat.app.AppCompatActivity;

public class RegisterActivity extends AppCompatActivity {

    EditText etEmail, etName, etPassword, etConfirmPassword;
    Button btnSignUp;
    TextView tvAlreadyHaveAccount;
    DBHelper dbHelper;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_register);

        dbHelper = new DBHelper(this);

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
            finish(); // Add this to prevent going back to register screen
        });

        // Register user when sign up button is clicked
        btnSignUp.setOnClickListener(v -> registerUser());
    }

    private void registerUser() {
        String email = etEmail.getText().toString().trim();
        String name = etName.getText().toString().trim();
        String password = etPassword.getText().toString().trim();
        String confirmPassword = etConfirmPassword.getText().toString().trim();

        // Email validation
        if (!isValidEmail(email)) {
            etEmail.setError("Please enter a valid email address");
            return;
        }

        // Basic validation
        if (TextUtils.isEmpty(name)) {
            etName.setError("Name is required");
            return;
        }

        // Password validation
        if (password.length() < 6) {
            etPassword.setError("Password must be at least 6 characters");
            return;
        }

        if (!password.equals(confirmPassword)) {
            etConfirmPassword.setError("Passwords do not match");
            return;
        }

        // Check if user already exists
        User existingUser = dbHelper.getUserByEmail(email);
        if (existingUser != null) {
            etEmail.setError("User already exists with this email");
            return;
        }

        // Register user in SQLite
        boolean success = dbHelper.insertUser(email, name, password);
        if (success) {
            // Get the newly created user to obtain the user_id
            User newUser = dbHelper.getUserByEmail(email);

            // Save user session with both email and user_id
            getSharedPreferences("UserPrefs", MODE_PRIVATE)
                    .edit()
                    .putString("email", email)
                    .putString("user_id", newUser.getUserId())
                    .apply();

            Toast.makeText(RegisterActivity.this,
                    "Registration successful", Toast.LENGTH_SHORT).show();

            // Redirect to ChatActivity
            Intent intent = new Intent(RegisterActivity.this, LoginActivity.class);
            intent.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK | Intent.FLAG_ACTIVITY_CLEAR_TASK);
            startActivity(intent);
            finish();
        } else {
            Toast.makeText(RegisterActivity.this,
                    "Registration failed", Toast.LENGTH_SHORT).show();
        }
    }

    private boolean isValidEmail(String email) {
        if (TextUtils.isEmpty(email)) {
            return false;
        }
        return android.util.Patterns.EMAIL_ADDRESS.matcher(email).matches();
    }
}