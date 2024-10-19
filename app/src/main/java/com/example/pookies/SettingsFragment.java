package com.example.pookies;

import android.app.AlertDialog;
import android.content.Intent;
import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Button;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.fragment.app.Fragment;

import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseUser;
import com.google.firebase.database.DatabaseReference;
import com.google.firebase.database.FirebaseDatabase;

public class SettingsFragment extends Fragment {

    private Button btnDeleteChat, btnDeleteAccount;
    private FirebaseAuth mAuth;
    private DatabaseReference mDatabase;

    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container, @Nullable Bundle savedInstanceState) {
        View view = inflater.inflate(R.layout.fragment_settings, container, false);

        mAuth = FirebaseAuth.getInstance();
        FirebaseUser currentUser = mAuth.getCurrentUser();
        if (currentUser != null) {
            mDatabase = FirebaseDatabase.getInstance().getReference("users").child(currentUser.getUid());
        }

        btnDeleteChat = view.findViewById(R.id.btnDeleteChat);
        btnDeleteAccount = view.findViewById(R.id.btnDeleteAccount);

        btnDeleteChat.setOnClickListener(v -> showDeleteChatConfirmationDialog());
        btnDeleteAccount.setOnClickListener(v -> showDeleteAccountConfirmationDialog());

        return view;
    }

    private void showDeleteChatConfirmationDialog() {
        new AlertDialog.Builder(getContext())
                .setTitle("Delete Chat History")
                .setMessage("Are you sure you want to delete all chat history? This action cannot be undone.")
                .setPositiveButton("Delete", (dialog, which) -> deleteAllChatHistory())
                .setNegativeButton("Cancel", null)
                .show();
    }

    private void deleteAllChatHistory() {
        FirebaseUser currentUser = mAuth.getCurrentUser();
        if (currentUser != null) {
            // Delete user's chat history from Firebase
            mDatabase.child("messages").removeValue()
                    .addOnSuccessListener(aVoid -> {
                        Toast.makeText(getContext(), "Chat history deleted successfully", Toast.LENGTH_SHORT).show();
                    })
                    .addOnFailureListener(e -> Toast.makeText(getContext(), "Failed to delete chat history", Toast.LENGTH_SHORT).show());
        }
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
        FirebaseUser user = mAuth.getCurrentUser();
        if (user != null) {
            String userId = user.getUid();
            // Delete user data from Firebase
            mDatabase.removeValue()
                    .addOnSuccessListener(aVoid -> {
                        // Delete user authentication
                        user.delete()
                                .addOnCompleteListener(task -> {
                                    if (task.isSuccessful()) {
                                        Toast.makeText(getContext(), "Account deleted successfully", Toast.LENGTH_SHORT).show();
                                        // Navigate to login screen after account deletion
                                        navigateToLogin();
                                    } else {
                                        Toast.makeText(getContext(), "Failed to delete account", Toast.LENGTH_SHORT).show();
                                    }
                                });
                    })
                    .addOnFailureListener(e -> Toast.makeText(getContext(), "Failed to delete account data", Toast.LENGTH_SHORT).show());
        }
    }

    private void navigateToLogin() {
        // Redirect to the login activity
        Intent intent = new Intent(getActivity(), LoginActivity.class);
        intent.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK | Intent.FLAG_ACTIVITY_CLEAR_TASK);
        startActivity(intent);
        if (getActivity() != null) {
            getActivity().finish();  // Close the current activity
        }
    }
}