    package com.example.pookies;

    import android.app.AlertDialog;
    import android.content.Intent;
    import android.os.Bundle;
    import android.text.TextUtils;
    import android.util.Log;
    import android.view.LayoutInflater;
    import android.view.View;
    import android.widget.Button;
    import android.widget.EditText;
    import android.widget.TextView;
    import android.widget.Toast;

    import androidx.appcompat.app.AppCompatActivity;

    import com.google.android.material.textfield.TextInputEditText;
    import com.google.firebase.auth.FirebaseAuth;
    import com.google.firebase.auth.FirebaseUser;
    import com.google.firebase.database.DataSnapshot;
    import com.google.firebase.database.DatabaseReference;
    import com.google.firebase.database.FirebaseDatabase;
    import com.google.firebase.storage.FirebaseStorage;
    import com.google.firebase.storage.StorageReference;

    import java.util.HashMap;
    import java.util.Map;

    public class LoginActivity extends AppCompatActivity {

        private EditText etEmail, etPassword;
        private Button btnLogin;
        private TextView tvSignUp, tvForgotPassword;
        private FirebaseAuth mAuth;
        private SessionManager sessionManager;

        @Override
        protected void onCreate(Bundle savedInstanceState) {
            super.onCreate(savedInstanceState);
            setContentView(R.layout.activity_login);

            mAuth = FirebaseAuth.getInstance();
            sessionManager = SessionManager.getInstance(this);

            // Check if user is already logged in
            if (sessionManager.isLoggedIn()) {
                startActivity(new Intent(LoginActivity.this, ChatActivity.class));
                finish();
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

                if (TextUtils.isEmpty(email) || TextUtils.isEmpty(password)) {
                    Toast.makeText(LoginActivity.this, "Please enter both email and password", Toast.LENGTH_SHORT).show();
                } else {
                    loginUser(email, password);
                }
            });
        }

        private void showForgotPasswordDialog() {
            AlertDialog.Builder builder = new AlertDialog.Builder(this);
            View dialogView = getLayoutInflater().inflate(R.layout.dialog_forgot_password, null);
            builder.setView(dialogView);

            final TextInputEditText emailInput = dialogView.findViewById(R.id.emailInput);

            builder.setTitle("Forgot Password")
                    .setPositiveButton("Reset", null)
                    .setNegativeButton("Cancel", (dialog, which) -> dialog.cancel());

            AlertDialog dialog = builder.create();

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

        private void loginUser(String email, String password) {
            Log.d("LoginActivity", "Attempting login for email: " + email);
            mAuth.signInWithEmailAndPassword(email, password)
                    .addOnCompleteListener(this, task -> {
                        if (task.isSuccessful()) {
                            FirebaseUser user = mAuth.getCurrentUser();
                            if (user != null) {
                                Log.d("LoginActivity", "Firebase authentication successful for userId: " + user.getUid());
                                checkAndMigrateUserData(user.getUid(), email);
                            } else {
                                Log.e("LoginActivity", "Firebase authentication succeeded, but user is null.");
                                showErrorDialog("Authentication error. Please try again.");
                            }
                        } else {
                            String errorMessage = task.getException() != null ?
                                    task.getException().getMessage() : "Authentication failed";
                            Log.e("LoginActivity", "Firebase authentication failed: " + errorMessage);
                            showErrorDialog(errorMessage);
                        }
                    });
        }

        private void checkAndMigrateUserData(String userId, String email) {
            Log.d("LoginActivity", "Checking info table for userId: " + userId);
            DatabaseReference infoTableRef = FirebaseDatabase.getInstance()
                    .getReference("users").child(userId).child("info");

            infoTableRef.get().addOnCompleteListener(task -> {
                if (task.isSuccessful() && task.getResult() != null) {
                    DataSnapshot snapshot = task.getResult();
                    if (snapshot.exists()) {
                        String password = snapshot.child("password").getValue(String.class);
                        if ("N/A".equals(password)) {
                            Log.d("LoginActivity", "Old user detected with missing password for userId: " + userId);
                            showOldUserPasswordSuggestion(email, userId);
                        } else {
                            Log.d("LoginActivity", "Info table exists and is valid for userId: " + userId);
                            SessionManager.getInstance(this).createLoginSession(email, userId);
                            redirectToChat();
                        }
                    } else {
                        Log.d("LoginActivity", "Info table is empty. Initiating data migration for userId: " + userId);
                        retrieveAndMigrateUserData(userId, email, infoTableRef);
                    }
                } else {
                    Log.e("LoginActivity", "Failed to fetch info table for userId: " + userId, task.getException());
                    showErrorDialog("Failed to validate user data. Please try again.");
                }
            });
        }


        private void showOldUserPasswordSuggestion(String email, String userId) {
            new AlertDialog.Builder(this)
                    .setTitle("Important: Update Your Password")
                    .setMessage("Uh oh! It looks like you're an old user. For your safety, we recommend updating your password in your profile settings after logging in.")
                    .setPositiveButton("Understood", (dialog, which) -> {
                        Log.d("LoginActivity", "User acknowledged password update suggestion.");
                        // Allow the user to proceed without updating the password immediately
                        SessionManager.getInstance(this).createLoginSession(email, userId);
                        redirectToChat();
                    })
                    .setCancelable(false) // Make the dialog persistent until the user acknowledges
                    .show();
        }
        private void resetPassword(String email) {
            mAuth.sendPasswordResetEmail(email)
                    .addOnCompleteListener(task -> {
                        if (task.isSuccessful()) {
                            Toast.makeText(LoginActivity.this, "Password reset email sent. Please check your inbox.", Toast.LENGTH_SHORT).show();
                        } else {
                            Toast.makeText(LoginActivity.this, "Failed to send reset email. Please try again later.", Toast.LENGTH_SHORT).show();
                        }
                    });
        }

        private void retrieveAndMigrateUserData(String userId, String email, DatabaseReference infoTableRef) {
            Log.d("LoginActivity", "Retrieving user data for migration for userId: " + userId);
            FirebaseUser firebaseUser = mAuth.getCurrentUser();

            if (firebaseUser != null) {
                String name = firebaseUser.getDisplayName() != null ? firebaseUser.getDisplayName() : "Unknown";
                String password = "N/A"; // Password cannot be retrieved directly for security reasons.

                // Create the new info data structure
                Map<String, Object> newInfoData = new HashMap<>();
                newInfoData.put("name", name);
                newInfoData.put("email", email);
                newInfoData.put("password", password);
                newInfoData.put("profilePicture", null); // Placeholder for profile picture

                Log.d("LoginActivity", "Basic info retrieved: " + newInfoData);

                // Fetch profile picture and complete migration
                fetchAndMigrateProfilePicture(userId, newInfoData, infoTableRef);
            } else {
                Log.e("LoginActivity", "Failed to retrieve user data from FirebaseAuth.");
                showErrorDialog("User data not found. Please contact support.");
            }
        }

        private void fetchAndMigrateProfilePicture(String userId, Map<String, Object> newInfoData, DatabaseReference infoTableRef) {
            Log.d("LoginActivity", "Fetching profile picture for userId: " + userId);

            StorageReference profilePicRef = FirebaseStorage.getInstance()
                    .getReference("profile_pictures").child(userId).child("profile.jpg");

            profilePicRef.getDownloadUrl().addOnSuccessListener(uri -> {
                newInfoData.put("profilePicture", uri.toString());
                Log.d("LoginActivity", "Profile picture URL retrieved for userId: " + userId);
                saveMigratedData(infoTableRef, newInfoData, userId, (String) newInfoData.get("email"));
            }).addOnFailureListener(e -> {
                Log.e("LoginActivity", "Failed to fetch profile picture for userId: " + userId, e);
                newInfoData.put("profilePicture", null); // Proceed without profile picture
                saveMigratedData(infoTableRef, newInfoData, userId, (String) newInfoData.get("email"));
            });
        }

        private void saveMigratedData(DatabaseReference infoTableRef, Map<String, Object> newInfoData, String userId, String email) {
            Log.d("LoginActivity", "Saving migrated data to info table for userId: " + userId);
            infoTableRef.setValue(newInfoData).addOnCompleteListener(task -> {
                if (task.isSuccessful()) {
                    Log.d("LoginActivity", "Data successfully migrated for userId: " + userId);
                    SessionManager.getInstance(this).createLoginSession(email, userId);
                    redirectToChat();
                } else {
                    Log.e("LoginActivity", "Failed to save migrated data for userId: " + userId, task.getException());
                    showErrorDialog("Failed to save user data. Please try again.");
                }
            });
        }

        private void redirectToChat() {
            Log.d("LoginActivity", "Redirecting to ChatActivity.");
            Intent intent = new Intent(getApplicationContext(), ChatActivity.class);
            startActivity(intent);
            finish();
        }

        private void showErrorDialog(String message) {
            Log.e("LoginActivity", "Error: " + message);
            new AlertDialog.Builder(this)
                    .setTitle("Error")
                    .setMessage(message)
                    .setPositiveButton("OK", (dialog, which) -> dialog.dismiss())
                    .show();
        }

    }