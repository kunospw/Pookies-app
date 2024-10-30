package com.example.pookies;

import android.app.AlertDialog;
import android.content.Context;
import android.content.Intent;
import android.os.Bundle;
import android.util.Log;
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

    @Override
    public void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        dbHelper = new DBHelper(requireContext());
        userId = requireActivity().getSharedPreferences("UserPrefs", Context.MODE_PRIVATE)
                .getString("user_id", "");

        // Validate userId exists
        if (userId.isEmpty()) {
            Log.e("SettingsFragment", "UserId is empty");
            Toast.makeText(requireContext(), "Session expired. Please login again.", Toast.LENGTH_SHORT).show();
            navigateToLogin();
            return;
        }

        // Check if user exists in database
        if (!dbHelper.doesUserExist(userId)) {
            Log.e("SettingsFragment", "User does not exist in database: " + userId);
            Toast.makeText(requireContext(), "User account not found. Please login again.", Toast.LENGTH_SHORT).show();
            navigateToLogin();
            return;
        }

    }

    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container,
                             @Nullable Bundle savedInstanceState) {
        View view = inflater.inflate(R.layout.fragment_settings, container, false);

        btnDeleteChat = view.findViewById(R.id.btnDeleteChat);
        btnDeleteAccount = view.findViewById(R.id.btnDeleteAccount);

        setupClickListeners();

        return view;
    }

    private void setupClickListeners() {
        btnDeleteChat.setOnClickListener(v -> showDeleteChatConfirmationDialog());
        btnDeleteAccount.setOnClickListener(v -> showDeleteAccountConfirmationDialog());
    }

    private void showDeleteChatConfirmationDialog() {
        new AlertDialog.Builder(requireContext())
                .setTitle("Delete Chat History")
                .setMessage("Are you sure you want to delete all chat history? This action cannot be undone.")
                .setPositiveButton("Delete", (dialog, which) -> deleteAllChatHistory())
                .setNegativeButton("Cancel", null)
                .show();
    }

    private void deleteAllChatHistory() {
        boolean success = dbHelper.deleteAllMessages(userId);
        String message = success ? "Chat history deleted successfully" : "Failed to delete chat history";
        Toast.makeText(requireContext(), message, Toast.LENGTH_SHORT).show();
    }

    private void showDeleteAccountConfirmationDialog() {
        new AlertDialog.Builder(requireContext())
                .setTitle("Delete Account")
                .setMessage("Are you sure you want to delete your account? This action cannot be undone.")
                .setPositiveButton("Delete", (dialog, which) -> deleteUserAccount())
                .setNegativeButton("Cancel", null)
                .show();
    }

    private void deleteUserAccount() {
        boolean success = dbHelper.deleteUser(userId);
        if (success) {
            Toast.makeText(requireContext(), "Account deleted successfully", Toast.LENGTH_SHORT).show();
            // Clear shared preferences
            requireActivity().getSharedPreferences("UserPrefs", Context.MODE_PRIVATE)
                    .edit()
                    .clear()
                    .apply();
            navigateToLogin();
        } else {
            Toast.makeText(requireContext(), "Failed to delete account", Toast.LENGTH_SHORT).show();
        }
    }

    private void navigateToLogin() {
        Intent intent = new Intent(requireActivity(), LoginActivity.class);
        intent.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK | Intent.FLAG_ACTIVITY_CLEAR_TASK);
        startActivity(intent);
        requireActivity().finish();
    }

    @Override
    public void onDestroy() {
        super.onDestroy();
        if (dbHelper != null) {
            dbHelper.close();
        }
    }
}