package com.example.pookies;

import android.app.AlertDialog;
import android.content.Intent;
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

import android.database.Cursor;
import android.database.sqlite.SQLiteDatabase;
import android.database.sqlite.SQLiteOpenHelper;

public class LoginActivity extends AppCompatActivity {

    EditText etEmail, etPassword;
    Button btnLogin;
    TextView tvSignUp;
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

        tvSignUp.setOnClickListener(v -> {
            // Navigate to RegisterActivity
            Intent intent = new Intent(LoginActivity.this, RegisterActivity.class);
            startActivity(intent);
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
