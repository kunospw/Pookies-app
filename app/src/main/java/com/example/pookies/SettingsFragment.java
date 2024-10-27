package com.example.pookies;

import android.app.AlertDialog;
import android.content.Context;
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

public class SettingsFragment extends Fragment {

    private Button btnDeleteChat, btnDeleteAccount;
    private DBHelper dbHelper;
    private String userId;

    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container, @Nullable Bundle savedInstanceState) {
        View view = inflater.inflate(R.layout.fragment_settings, container, false);

        dbHelper = new DBHelper(getContext());
        // Get userId from SharedPreferences or similar storage
        userId = getActivity().getSharedPreferences("UserPrefs", Context.MODE_PRIVATE)
                .getString("userId", "");

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
        boolean success = dbHelper.deleteAllMessages(userId);
        if (success) {
            Toast.makeText(getContext(), "Chat history deleted successfully", Toast.LENGTH_SHORT).show();
        } else {
            Toast.makeText(getContext(), "Failed to delete chat history", Toast.LENGTH_SHORT).show();
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
        // Delete all user data including messages
        boolean success = dbHelper.deleteUser(userId);
        if (success) {
            Toast.makeText(getContext(), "Account deleted successfully", Toast.LENGTH_SHORT).show();
            // Clear shared preferences
            getActivity().getSharedPreferences("UserPrefs", Context.MODE_PRIVATE)
                    .edit()
                    .clear()
                    .apply();
            navigateToLogin();
        } else {
            Toast.makeText(getContext(), "Failed to delete account", Toast.LENGTH_SHORT).show();
        }
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