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

import androidx.appcompat.app.AppCompatActivity;

import com.google.android.material.textfield.TextInputEditText;

import java.util.UUID;

public class LoginActivity extends AppCompatActivity {

    EditText etEmail, etPassword;
    Button btnLogin;
    TextView tvSignUp, tvForgotPassword;
    DBHelper dbHelper;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_login);

        dbHelper = new DBHelper(this);

        etEmail = findViewById(R.id.emailInput);
        etPassword = findViewById(R.id.passwordInput);
        btnLogin = findViewById(R.id.loginButton);
        tvSignUp = findViewById(R.id.alreadyHaveAccount);
        tvForgotPassword = findViewById(R.id.forgotPassword);

        tvSignUp.setOnClickListener(v -> {
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
                Toast.makeText(LoginActivity.this,
                        "Please enter both email and password", Toast.LENGTH_SHORT).show();
            } else {
                authenticateUser(email, password);
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
                    handlePasswordReset(email);
                    dialog.dismiss();
                } else {
                    Toast.makeText(LoginActivity.this,
                            "Please enter your email", Toast.LENGTH_SHORT).show();
                }
            });
        });

        dialog.show();
    }

    private void handlePasswordReset(String email) {
        User user = dbHelper.getUserByEmail(email);
        if (user != null) {
            // Generate reset token and set expiry time (24 hours from now)
            String resetToken = UUID.randomUUID().toString();
            long expiryTime = System.currentTimeMillis() + (24 * 60 * 60 * 1000);

            if (dbHelper.storeResetToken(email, resetToken, expiryTime)) {
                // In a real app, you would send this token via email
                // For demo purposes, we'll show it in a toast
                Toast.makeText(LoginActivity.this,
                        "Reset token generated: " + resetToken, Toast.LENGTH_LONG).show();
            } else {
                Toast.makeText(LoginActivity.this,
                        "Failed to generate reset token", Toast.LENGTH_SHORT).show();
            }
        } else {
            Toast.makeText(LoginActivity.this,
                    "No account found with this email", Toast.LENGTH_SHORT).show();
        }
    }

    private void authenticateUser(String email, String password) {
        // First check if the email exists
        User user = dbHelper.getUserByEmail(email);

        if (user == null) {
            new AlertDialog.Builder(LoginActivity.this)
                    .setTitle("Account Not Found")
                    .setMessage("No account exists with this email. Would you like to register?")
                    .setPositiveButton("Register", (dialog, which) -> {
                        Intent intent = new Intent(LoginActivity.this, RegisterActivity.class);
                        intent.putExtra("email", email); // Pre-fill email in registration
                        startActivity(intent);
                    })
                    .setNegativeButton("Cancel", (dialog, which) -> dialog.dismiss())
                    .show();
            return;
        }

        // If email exists, verify password
        if (user.getPassword().equals(password)) {
            // Save user session
            getSharedPreferences("UserPrefs", MODE_PRIVATE)
                    .edit()
                    .putString("email", email)
                    .putString("user_id", user.getUserId())
                    .apply();

            Toast.makeText(getApplicationContext(),
                    "Login successful!", Toast.LENGTH_SHORT).show();
            Intent intent = new Intent(getApplicationContext(), ChatActivity.class);
            intent.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK | Intent.FLAG_ACTIVITY_CLEAR_TASK);
            startActivity(intent);
            finish();
        } else {
            new AlertDialog.Builder(LoginActivity.this)
                    .setTitle("Login Failed")
                    .setMessage("Incorrect password")
                    .setPositiveButton("OK", (dialog, which) -> dialog.dismiss())
                    .show();
        }
    }
}