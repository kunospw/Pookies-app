package com.example.pookies;

import android.app.AlertDialog;
import android.app.ProgressDialog;
import android.content.Context;
import android.content.Intent;
import android.os.Bundle;
import android.text.InputType;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Button;
import android.widget.EditText;
import android.widget.Toast;
import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.fragment.app.Fragment;

import com.google.firebase.auth.AuthCredential;
import com.google.firebase.auth.EmailAuthProvider;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseUser;
import com.google.firebase.database.DataSnapshot;
import com.google.firebase.database.DatabaseError;
import com.google.firebase.database.DatabaseReference;
import com.google.firebase.database.FirebaseDatabase;
import com.google.firebase.database.ValueEventListener;
import com.google.firebase.storage.FirebaseStorage;
import com.google.firebase.storage.StorageException;
import com.google.firebase.storage.StorageReference;

public class SettingsFragment extends Fragment {

    private Button btnDeleteChat, btnDeleteAccount;
    private DatabaseReference mDatabase;
    private FirebaseAuth auth;
    private String userId;
    private Context safeContext;

    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container, @Nullable Bundle savedInstanceState) {
        View view = inflater.inflate(R.layout.fragment_settings, container, false);

        // Initialize Firebase instances
        auth = FirebaseAuth.getInstance();
        userId = auth.getCurrentUser() != null ? auth.getCurrentUser().getUid() : "";
        mDatabase = FirebaseDatabase.getInstance().getReference("users").child(userId).child("messages");

        btnDeleteChat = view.findViewById(R.id.btnDeleteChat);
        btnDeleteAccount = view.findViewById(R.id.btnDeleteAccount);

        btnDeleteChat.setOnClickListener(v -> showDeleteChatConfirmationDialog());
        btnDeleteAccount.setOnClickListener(v -> showDeleteAccountConfirmationDialog());

        return view;
    }

    @Override
    public void onAttach(@NonNull Context context) {
        super.onAttach(context);
        safeContext = context; // Store context when Fragment is attached
    }

    @Override
    public void onDetach() {
        super.onDetach();
        safeContext = null; // Clear context when Fragment is detached
    }
    private void showDeleteChatConfirmationDialog() {
        if (safeContext == null) return; // Check if context is valid

        new AlertDialog.Builder(safeContext)
                .setTitle("Delete Chat History")
                .setMessage("Are you sure you want to delete all chat history? This action cannot be undone.")
                .setPositiveButton("Delete", (dialog, which) -> deleteAllChatHistory())
                .setNegativeButton("Cancel", null)
                .show();
    }

    private void deleteAllChatHistory() {
        if (userId.isEmpty()) {
            Toast.makeText(getContext(), "User not authenticated", Toast.LENGTH_SHORT).show();
            return;
        }

        // Check if the messages node exists before attempting to delete
        mDatabase.addListenerForSingleValueEvent(new ValueEventListener() {
            @Override
            public void onDataChange(@NonNull DataSnapshot snapshot) {
                if (snapshot.exists()) {
                    // Messages exist, proceed to delete
                    mDatabase.removeValue()
                            .addOnSuccessListener(aVoid -> {
                                if (getContext() != null) {
                                    Toast.makeText(getContext(), "Chat history deleted successfully", Toast.LENGTH_SHORT).show();
                                }
                            })
                            .addOnFailureListener(e -> {
                                if (getContext() != null) {
                                    Toast.makeText(getContext(), "Failed to delete chat history: " + e.getMessage(),
                                            Toast.LENGTH_SHORT).show();
                                }
                            });
                } else {
                    // No messages to delete
                    if (getContext() != null) {
                        Toast.makeText(getContext(), "No chat history to delete.", Toast.LENGTH_SHORT).show();
                    }
                }
            }

            @Override
            public void onCancelled(@NonNull DatabaseError error) {
                if (getContext() != null) {
                    Toast.makeText(getContext(), "Failed to check chat history: " + error.getMessage(),
                            Toast.LENGTH_SHORT).show();
                }
            }
        });
    }


    private void showDeleteAccountConfirmationDialog() {
        new AlertDialog.Builder(getContext())
                .setTitle("Delete Account")
                .setMessage("Are you sure you want to delete your account? This action cannot be undone.")
                .setPositiveButton("Delete", (dialog, which) -> deleteUserAccount())
                .setNegativeButton("Cancel", null)
                .show();
    }

    private void deleteUserAccount() {
        FirebaseUser currentUser = auth.getCurrentUser();
        if (userId.isEmpty() || currentUser == null) {
            Toast.makeText(getContext(), "User not authenticated", Toast.LENGTH_SHORT).show();
            return;
        }

        Log.d("DeleteUserAccount", "Starting account deletion for user: " + userId);

        // Show loading indicator
        ProgressDialog progressDialog = new ProgressDialog(getContext());
        progressDialog.setMessage("Deleting account...");
        progressDialog.setCancelable(false);
        progressDialog.show();

        // Re-authenticate the user before proceeding
        reAuthenticateUser(currentUser, progressDialog);
    }

    private void reAuthenticateUser(FirebaseUser currentUser, ProgressDialog progressDialog) {
        // Prompt the user to re-enter their password
        AlertDialog.Builder builder = new AlertDialog.Builder(getContext());
        builder.setTitle("Re-authenticate");
        builder.setMessage("Please enter your password to confirm.");

        final EditText input = new EditText(getContext());
        input.setInputType(InputType.TYPE_CLASS_TEXT | InputType.TYPE_TEXT_VARIATION_PASSWORD);
        builder.setView(input);

        builder.setPositiveButton("Confirm", (dialog, which) -> {
            String password = input.getText().toString();
            if (!password.isEmpty()) {
                AuthCredential credential = EmailAuthProvider.getCredential(currentUser.getEmail(), password);

                currentUser.reauthenticate(credential)
                        .addOnSuccessListener(aVoid -> {
                            Log.d("DeleteUserAccount", "Re-authentication successful.");
                            // After re-authentication, proceed to delete profile picture
                            StorageReference profilePicRef = FirebaseStorage.getInstance().getReference()
                                    .child("profile_pictures/" + userId);

                            Log.d("DeleteUserAccount", "Attempting to delete profile picture: profile_pictures/" + userId);
                            profilePicRef.delete()
                                    .addOnSuccessListener(aVoid1 -> {
                                        Log.d("DeleteUserAccount", "Profile picture deleted successfully.");
                                        // Proceed to delete Realtime Database records
                                        DatabaseReference userRef = FirebaseDatabase.getInstance().getReference("users").child(userId);
                                        deleteDatabaseRecords(userRef, progressDialog);
                                    })
                                    .addOnFailureListener(e -> {
                                        if (e instanceof StorageException &&
                                                ((StorageException) e).getErrorCode() == StorageException.ERROR_OBJECT_NOT_FOUND) {
                                            Log.w("DeleteUserAccount", "Profile picture not found. Proceeding with account deletion.");
                                            Toast.makeText(getContext(), "Profile picture not found, skipping deletion.", Toast.LENGTH_SHORT).show();
                                            DatabaseReference userRef = FirebaseDatabase.getInstance().getReference("users").child(userId);
                                            deleteDatabaseRecords(userRef, progressDialog);
                                        } else {
                                            progressDialog.dismiss();
                                            Log.e("DeleteUserAccount", "Error deleting profile picture: " + e.getMessage());
                                            Toast.makeText(getContext(), "Failed to delete profile picture: " + e.getMessage(), Toast.LENGTH_SHORT).show();
                                        }
                                    });
                        })
                        .addOnFailureListener(e -> {
                            progressDialog.dismiss();
                            Log.e("DeleteUserAccount", "Re-authentication failed: " + e.getMessage());
                            Toast.makeText(getContext(), "Re-authentication failed: " + e.getMessage(), Toast.LENGTH_SHORT).show();
                        });
            } else {
                progressDialog.dismiss();
                Toast.makeText(getContext(), "Password is required.", Toast.LENGTH_SHORT).show();
            }
        });

        builder.setNegativeButton("Cancel", (dialog, which) -> {
            dialog.cancel();
            progressDialog.dismiss();
        });

        builder.show();
    }

    private void deleteDatabaseRecords(DatabaseReference userRef, ProgressDialog progressDialog) {
        Log.d("DeleteUserAccount", "Attempting to delete user data from Realtime Database at: " + userRef.getPath());

        // Step 2: Delete user data from Realtime Database
        userRef.removeValue()
                .addOnSuccessListener(aVoid -> {
                    Log.d("DeleteUserAccount", "User data deleted successfully.");
                    // Step 3: Delete the Firebase Authentication account
                    FirebaseUser currentUser = auth.getCurrentUser();
                    if (currentUser != null) {
                        Log.d("DeleteUserAccount", "Attempting to delete Firebase Authentication user.");
                        currentUser.delete()
                                .addOnSuccessListener(void_ -> {
                                    progressDialog.dismiss();
                                    Log.d("DeleteUserAccount", "Firebase user account deleted successfully.");
                                    Toast.makeText(getContext(), "Account deleted successfully", Toast.LENGTH_SHORT).show();

                                    // Clear shared preferences
                                    if (getActivity() != null) {
                                        Log.d("DeleteUserAccount", "Clearing shared preferences.");
                                        getActivity().getSharedPreferences("UserPrefs", Context.MODE_PRIVATE)
                                                .edit()
                                                .clear()
                                                .apply();
                                    }

                                    // Redirect to LoginActivity
                                    navigateToLogin();
                                })
                                .addOnFailureListener(e -> {
                                    progressDialog.dismiss();
                                    Log.e("DeleteUserAccount", "Failed to delete Firebase user account: " + e.getMessage());
                                    Toast.makeText(getContext(), "Failed to delete account: " + e.getMessage(), Toast.LENGTH_SHORT).show();
                                });
                    } else {
                        progressDialog.dismiss();
                        Log.e("DeleteUserAccount", "Failed to authenticate user for account deletion.");
                        Toast.makeText(getContext(), "Failed to authenticate user for account deletion", Toast.LENGTH_SHORT).show();
                    }
                })
                .addOnFailureListener(e -> {
                    progressDialog.dismiss();
                    Log.e("DeleteUserAccount", "Error deleting user data from Realtime Database: " + e.getMessage());
                    Toast.makeText(getContext(), "Failed to delete user data: " + e.getMessage(), Toast.LENGTH_SHORT).show();
                });
    }

    private void navigateToLogin() {
        Intent intent = new Intent(getActivity(), LoginActivity.class);
        intent.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK | Intent.FLAG_ACTIVITY_CLEAR_TASK);
        startActivity(intent);
        if (getActivity() != null) {
            getActivity().finish();
        }
    }
}