package com.example.pookies;

import android.app.AlertDialog;
import android.content.DialogInterface;
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
import com.google.firebase.database.DataSnapshot;
import com.google.firebase.database.DatabaseError;
import com.google.firebase.database.DatabaseReference;
import com.google.firebase.database.FirebaseDatabase;
import com.google.firebase.database.ValueEventListener;

public class SettingsFragment extends Fragment {

    private Button btnDeleteChat, btnDeleteAccount;
    private FirebaseAuth mAuth;
    private DatabaseReference mDatabase;
    private DBHelper dbHelper;

    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container, @Nullable Bundle savedInstanceState) {
        View view = inflater.inflate(R.layout.fragment_settings, container, false);

        mAuth = FirebaseAuth.getInstance();
        FirebaseUser currentUser = mAuth.getCurrentUser();
        if (currentUser != null) {
            mDatabase = FirebaseDatabase.getInstance().getReference("users").child(currentUser.getUid());
        }
        dbHelper = new DBHelper(getContext());

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
            mDatabase.child("messages").orderByChild("senderUid").equalTo(currentUser.getUid())
                    .addListenerForSingleValueEvent(new ValueEventListener() {
                        @Override
                        public void onDataChange(DataSnapshot dataSnapshot) {
                            for (DataSnapshot snapshot : dataSnapshot.getChildren()) {
                                snapshot.getRef().removeValue();
                            }

                            // Delete user's chat history from SQLite
                            dbHelper.deleteUserMessages(currentUser.getUid());
                            Toast.makeText(getContext(), "Chat history deleted successfully", Toast.LENGTH_SHORT).show();
                        }

                        @Override
                        public void onCancelled(DatabaseError databaseError) {
                            Toast.makeText(getContext(), "Failed to delete chat history", Toast.LENGTH_SHORT).show();
                        }
                    });
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
            // Delete user data from Firebase
            mDatabase.removeValue()
                    .addOnSuccessListener(aVoid -> {
                        // Delete user authentication
                        user.delete()
                                .addOnCompleteListener(task -> {
                                    if (task.isSuccessful()) {
                                        // Delete user data from SQLite
                                        dbHelper.deleteUser(user.getUid());
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
        startActivity(intent);
        if (getActivity() != null) {
            getActivity().finish();  // Close the current activity
        }
    }
}