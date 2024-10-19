package com.example.pookies;

import android.app.AlertDialog;
import android.content.Intent;
import android.os.Bundle;
import android.text.TextUtils;
import android.view.LayoutInflater;
import android.view.View;
import android.widget.Button;
import android.widget.EditText;
import android.widget.TextView;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;

import com.google.android.gms.tasks.OnCompleteListener;
import com.google.android.gms.tasks.Task;
import com.google.android.material.textfield.TextInputEditText;
import com.google.firebase.auth.AuthResult;
import com.google.firebase.auth.FirebaseAuth;

import android.database.Cursor;
import android.database.sqlite.SQLiteDatabase;
import android.database.sqlite.SQLiteOpenHelper;

public class LoginActivity extends AppCompatActivity {

    EditText etEmail, etPassword;
    Button btnLogin;
    TextView tvSignUp;

    TextView tvForgotPassword;
    DBHelper dbHelper;
    FirebaseAuth mAuth;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_login);

        dbHelper = new DBHelper(this); // Initialize SQLite DB Helper
        SQLiteDatabase db = dbHelper.getWritableDatabase();

        mAuth = FirebaseAuth.getInstance(); // Initialize Firebase Auth

        etEmail = findViewById(R.id.emailInput);
        etPassword = findViewById(R.id.passwordInput);
        btnLogin = findViewById(R.id.loginButton);
        tvSignUp = findViewById(R.id.alreadyHaveAccount);
        tvForgotPassword = findViewById(R.id.forgotPassword);

        tvSignUp.setOnClickListener(v -> {
            // Navigate to RegisterActivity
            Intent intent = new Intent(LoginActivity.this, RegisterActivity.class);
            startActivity(intent);
        });

        tvForgotPassword.setOnClickListener(v -> {
            showForgotPasswordDialog();
        });

        btnLogin.setOnClickListener(v -> {
            String email = etEmail.getText().toString().trim();
            String password = etPassword.getText().toString().trim();

            if (TextUtils.isEmpty(email) || TextUtils.isEmpty(password)) {
                Toast.makeText(LoginActivity.this, "Please enter both email and password", Toast.LENGTH_SHORT).show();
            } else {
                // Check both Firebase and SQLite authentication
                authenticateWithFirebaseAndSQLite(email, password);
            }
        });
    }

    private void showForgotPasswordDialog() {
        AlertDialog.Builder builder = new AlertDialog.Builder(this);
        LayoutInflater inflater = getLayoutInflater();
        View dialogView = inflater.inflate(R.layout.dialog_forgot_password, null);
        builder.setView(dialogView);

        final TextInputEditText emailInput = dialogView.findViewById(R.id.emailInput);

        builder.setTitle("Forgot Password")
                .setPositiveButton("Reset", null)
                .setNegativeButton("Cancel", (dialog, which) -> dialog.cancel());

        final AlertDialog dialog = builder.create();

        dialog.setOnShowListener(dialogInterface -> {
            Button button = dialog.getButton(AlertDialog.BUTTON_POSITIVE);
            button.setOnClickListener(view -> {
                String email = emailInput.getText().toString().trim();
                if (!TextUtils.isEmpty(email)) {
                    resetPassword(email);
                    dialog.dismiss();
                } else {
                    Toast.makeText(LoginActivity.this, "Please enter your email", Toast.LENGTH_SHORT).show();
                }
            });
        });

        dialog.show();
    }

    private void resetPassword(String email) {
        // First, try to reset password using Firebase
        mAuth.sendPasswordResetEmail(email)
                .addOnCompleteListener(task -> {
                    if (task.isSuccessful()) {
                        Toast.makeText(LoginActivity.this, "Password reset email sent", Toast.LENGTH_SHORT).show();
                    } else {
                        // If Firebase reset fails, check if the user exists in SQLite
                        User user = dbHelper.getUserByEmail(email);
                        if (user != null) {
                            // For SQLite users, you might want to implement a custom password reset mechanism
                            // For now, we'll just show a message
                            Toast.makeText(LoginActivity.this, "Please contact support to reset your password", Toast.LENGTH_LONG).show();
                        } else {
                            Toast.makeText(LoginActivity.this, "No account found with this email", Toast.LENGTH_SHORT).show();
                        }
                    }
                });
    }

    /**
     * Try both Firebase and SQLite authentication.
     */
    private void authenticateWithFirebaseAndSQLite(String email, String password) {
        // First, try Firebase authentication
        mAuth.signInWithEmailAndPassword(email, password)
                .addOnCompleteListener(LoginActivity.this, task -> {
                    if (task.isSuccessful()) {
                        // Firebase authentication successful
                        loginSuccess(email);
                    } else {
                        // Firebase authentication failed, now try SQLite
                        authenticateWithSQLite(email, password);
                    }
                });
    }

    /**
     * Try SQLite authentication if Firebase fails.
     */
    private void authenticateWithSQLite(String email, String password) {
        User user = dbHelper.getUserByEmail(email);
        if (user != null && user.getPassword().equals(password)) {
            // SQLite authentication successful
            loginSuccess(email);
        } else {
            // Both Firebase and SQLite failed, show account not found alert
            showAccountNotFoundAlert("Authentication failed: Invalid email or password");
        }
    }

    /**
     * Show an alert dialog if both Firebase and SQLite authentication fail.
     */
    private void showAccountNotFoundAlert(String message) {
        new AlertDialog.Builder(LoginActivity.this)
                .setTitle("Account Not Found")
                .setMessage(message)
                .setPositiveButton("OK", (dialog, which) -> dialog.dismiss())
                .show();
    }

    /**
     * Handle successful login.
     */
    private void loginSuccess(String email) {
        // Save the user ID (email) in SharedPreferences for session management
        getSharedPreferences("APP_PREFS", MODE_PRIVATE)
                .edit()
                .putString("USER_ID", email)
                .apply();

        // Login successful, navigate to ChatActivity
        Toast.makeText(getApplicationContext(), "Login successful!", Toast.LENGTH_SHORT).show();
        Intent intent = new Intent(getApplicationContext(), ChatActivity.class);
        startActivity(intent);
        finish(); // Finish LoginActivity
    }
}
