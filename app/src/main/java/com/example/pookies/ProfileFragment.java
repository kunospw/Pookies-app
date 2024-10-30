package com.example.pookies;

import static android.content.Context.MODE_PRIVATE;

import android.Manifest;
import android.content.pm.PackageManager;
import android.app.Activity;
import android.app.AlertDialog;
import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.net.Uri;
import android.os.Bundle;
import android.os.Handler;
import android.os.Looper;
import android.provider.MediaStore;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Button;
import android.widget.EditText;
import android.widget.Toast;

import androidx.annotation.Nullable;
import androidx.core.app.ActivityCompat;
import androidx.core.content.ContextCompat;
import androidx.fragment.app.Fragment;

import com.bumptech.glide.Glide;
import com.bumptech.glide.request.RequestOptions;
import com.google.android.material.floatingactionbutton.FloatingActionButton;
import com.google.android.material.imageview.ShapeableImageView;
import com.google.android.material.textfield.TextInputEditText;
import com.bumptech.glide.load.engine.DiskCacheStrategy;

import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;

public class ProfileFragment extends Fragment {

    private static final String TAG = "ProfileFragment";
    private static final int REQUEST_CAMERA = 100;
    private static final int REQUEST_GALLERY = 101;
    private static final int CAMERA_PERMISSION_CODE = 102;
    private static final String USER_PREFS = "UserPrefs";

    private EditText usernameEditText, emailEditText, passwordEditText;
    private ShapeableImageView profileImageView;
    private FloatingActionButton floatingActionButton;
    private Button editProfileButton, forgotPasswordButton;

    private DBHelper dbHelper;
    private SharedPreferences prefs;
    private User currentUser;


    @Override
    public void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        dbHelper = new DBHelper(requireContext());
        prefs = requireContext().getSharedPreferences(USER_PREFS, Context.MODE_PRIVATE);
    }

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {
        View view = inflater.inflate(R.layout.fragment_profile, container, false);

        initializeViews(view);
        setupListeners();
        loadUserData();

        return view;
    }

    private void navigateToLogin() {
        Intent intent = new Intent(getActivity(), LoginActivity.class);
        startActivity(intent);
        getActivity().finish();
    }

    private void initializeViews(View view) {
        usernameEditText = view.findViewById(R.id.username);
        emailEditText = view.findViewById(R.id.email);
        passwordEditText = view.findViewById(R.id.password);
        profileImageView = view.findViewById(R.id.imageView2);
        floatingActionButton = view.findViewById(R.id.floatingActionButton);
        editProfileButton = view.findViewById(R.id.editProfileButton);
        forgotPasswordButton = view.findViewById(R.id.forgotPasswordButton);
    }

    private void loadUserData() {
        String email = prefs.getString("email", null);
        String userId = prefs.getString("user_id", null);

        if (email != null && userId != null) {
            currentUser = dbHelper.getUserByEmail(email);
            if (currentUser != null) {
                emailEditText.setText(currentUser.getEmail());
                usernameEditText.setText(currentUser.getName());
                passwordEditText.setText("********");
                loadProfilePicture();
            } else {
                navigateToLogin();
            }
        } else {
            navigateToLogin();
        }
    }

    private void loadProfilePicture() {
        if (currentUser != null && currentUser.getProfilePicturePath() != null) {
            File imgFile = new File(currentUser.getProfilePicturePath());
            if (imgFile.exists()) {
                Bitmap bitmap = BitmapFactory.decodeFile(imgFile.getAbsolutePath());
                profileImageView.setImageBitmap(bitmap);
            } else {
                profileImageView.setImageResource(R.drawable.baseline_person_24);
            }
        } else {
            profileImageView.setImageResource(R.drawable.baseline_person_24);
        }
    }

    private void setupListeners() {
        floatingActionButton.setOnClickListener(v -> showImagePickerDialog());
        editProfileButton.setOnClickListener(v -> showEditProfileDialog());
        forgotPasswordButton.setOnClickListener(v -> showChangePasswordDialog());
    }

    private void showImagePickerDialog() {
        CharSequence[] options = {"Take Photo", "Choose from Gallery", "Remove Profile Picture"};
        new AlertDialog.Builder(getActivity())
                .setTitle("Update Profile Picture")
                .setItems(options, (dialog, which) -> {
                    if (options[which].equals("Take Photo")) {
                        openCamera();
                    } else if (options[which].equals("Choose from Gallery")) {
                        openGallery();
                    } else if (options[which].equals("Remove Profile Picture")) {
                        removeProfilePicture();
                    }
                })
                .show();
    }

    private void openCamera() {
        if (ContextCompat.checkSelfPermission(getActivity(), Manifest.permission.CAMERA)
                != PackageManager.PERMISSION_GRANTED) {
            ActivityCompat.requestPermissions(getActivity(),
                    new String[]{Manifest.permission.CAMERA}, CAMERA_PERMISSION_CODE);
        } else {
            Intent intent = new Intent(MediaStore.ACTION_IMAGE_CAPTURE);
            startActivityForResult(intent, REQUEST_CAMERA);
        }
    }

    private void openGallery() {
        Intent intent = new Intent(Intent.ACTION_PICK, MediaStore.Images.Media.EXTERNAL_CONTENT_URI);
        intent.setType("image/*");
        startActivityForResult(intent, REQUEST_GALLERY);
    }

    private void removeProfilePicture() {
        if (currentUser != null) {
            // Delete existing profile picture file
            if (currentUser.getProfilePicturePath() != null) {
                File file = new File(currentUser.getProfilePicturePath());
                if (file.exists()) {
                    file.delete();
                }
            }

            // Update database
            dbHelper.updateProfilePicturePath(currentUser.getEmail(), null);
            currentUser.setProfilePicturePath(null);

            // Update UI
            profileImageView.setImageResource(R.drawable.baseline_person_24);

            // Update ChatActivity header
            if (getActivity() instanceof ChatActivity) {
                ((ChatActivity) getActivity()).updateDrawerHeader();
            }

            Toast.makeText(getActivity(), "Profile picture removed", Toast.LENGTH_SHORT).show();
        }
    }

    private void showChangePasswordDialog() {
        View dialogView = getLayoutInflater().inflate(R.layout.dialog_change_password, null);
        TextInputEditText currentPasswordInput = dialogView.findViewById(R.id.currentPassword);
        TextInputEditText newPasswordInput = dialogView.findViewById(R.id.newPassword);
        TextInputEditText confirmPasswordInput = dialogView.findViewById(R.id.confirmPassword);

        AlertDialog dialog = new AlertDialog.Builder(requireActivity())
                .setView(dialogView)
                .setTitle("Change Password")
                .setPositiveButton("Change", null)
                .setNegativeButton("Cancel", null)
                .create();

        dialog.setOnShowListener(dialogInterface -> {
            Button button = dialog.getButton(AlertDialog.BUTTON_POSITIVE);
            button.setOnClickListener(view -> {
                String currentPassword = currentPasswordInput.getText().toString().trim();
                String newPassword = newPasswordInput.getText().toString().trim();
                String confirmPassword = confirmPasswordInput.getText().toString().trim();

                if (validatePasswordChange(currentPassword, newPassword, confirmPassword)) {
                    changePassword(currentPassword, newPassword, dialog);
                }
            });
        });

        dialog.show();
    }

    private boolean validatePasswordChange(String currentPassword, String newPassword, String confirmPassword) {
        if (currentPassword.isEmpty() || newPassword.isEmpty() || confirmPassword.isEmpty()) {
            Toast.makeText(getActivity(), "All fields are required", Toast.LENGTH_SHORT).show();
            return false;
        }

        if (!newPassword.equals(confirmPassword)) {
            Toast.makeText(getActivity(), "New passwords do not match", Toast.LENGTH_SHORT).show();
            return false;
        }

        if (currentUser == null || !currentUser.getPassword().equals(currentPassword)) {
            Toast.makeText(getActivity(), "Current password is incorrect", Toast.LENGTH_SHORT).show();
            return false;
        }

        return true;
    }

    private void changePassword(String currentPassword, String newPassword, AlertDialog dialog) {
        if (currentUser != null && dbHelper.updatePassword(currentUser.getEmail(), newPassword)) {
            currentUser.setPassword(newPassword);
            Toast.makeText(getActivity(), "Password updated successfully", Toast.LENGTH_SHORT).show();
            dialog.dismiss();
        } else {
            Toast.makeText(getActivity(), "Failed to update password", Toast.LENGTH_SHORT).show();
        }
    }
    private void showEditProfileDialog() {
        View dialogView = getLayoutInflater().inflate(R.layout.dialog_edit_profile, null);
        EditText newUsernameInput = dialogView.findViewById(R.id.newUsername);
        EditText newEmailInput = dialogView.findViewById(R.id.newEmail);

        newUsernameInput.setText(currentUser.getName());
        newEmailInput.setText(currentUser.getEmail());

        new AlertDialog.Builder(requireActivity())
                .setView(dialogView)
                .setTitle("Edit Profile")
                .setPositiveButton("Save", (dialog, which) -> {
                    String newUsername = newUsernameInput.getText().toString().trim();
                    String newEmail = newEmailInput.getText().toString().trim();
                    updateProfile(newUsername, newEmail);
                })
                .setNegativeButton("Cancel", null)
                .show();
    }
    private void updateProfile(String newUsername, String newEmail) {
        if (currentUser != null) {
            boolean needsUpdate = false;

            // Update username if changed
            if (!currentUser.getName().equals(newUsername)) {
                // Update in database first
                if (dbHelper.updateUsername(currentUser.getEmail(), newUsername)) {
                    currentUser.setName(newUsername);
                    needsUpdate = true;
                } else {
                    Toast.makeText(getActivity(), "Failed to update username", Toast.LENGTH_SHORT).show();
                    return;
                }
            }

            // Update email if changed
            if (!currentUser.getEmail().equals(newEmail)) {
                // Update in database first
                if (dbHelper.updateEmail(currentUser.getEmail(), newEmail)) {
                    currentUser.setEmail(newEmail);
                    // Update SharedPreferences
                    prefs.edit().putString("email", newEmail).apply();
                    needsUpdate = true;
                } else {
                    Toast.makeText(getActivity(), "Failed to update email", Toast.LENGTH_SHORT).show();
                    return;
                }
            }

            if (needsUpdate) {
                // Update UI
                usernameEditText.setText(newUsername);
                emailEditText.setText(newEmail);

                // Update ChatActivity header
                if (getActivity() instanceof ChatActivity) {
                    ((ChatActivity) getActivity()).refreshHeader();
                }

                Toast.makeText(getActivity(), "Profile updated successfully", Toast.LENGTH_SHORT).show();
            }
        }
    }

    @Override
    public void onActivityResult(int requestCode, int resultCode, @Nullable Intent data) {
        super.onActivityResult(requestCode, resultCode, data);

        if (resultCode == Activity.RESULT_OK && data != null) {
            Bitmap imageBitmap = null;

            if (requestCode == REQUEST_CAMERA) {
                imageBitmap = (Bitmap) data.getExtras().get("data");
            } else if (requestCode == REQUEST_GALLERY) {
                try {
                    Uri selectedImage = data.getData();
                    imageBitmap = MediaStore.Images.Media.getBitmap(
                            requireActivity().getContentResolver(),
                            selectedImage
                    );
                } catch (IOException e) {
                    Log.e(TAG, "Error loading gallery image", e);
                    Toast.makeText(getActivity(), "Failed to load image", Toast.LENGTH_SHORT).show();
                    return;
                }
            }

            if (imageBitmap != null) {
                saveProfilePicture(imageBitmap);
            }
        }
    }
    private void saveProfilePicture(Bitmap bitmap) {
        if (currentUser != null) {
            try {
                // Create directory if it doesn't exist
                File directory = new File(requireContext().getFilesDir(), "profile_pictures");
                if (!directory.exists()) {
                    directory.mkdirs();
                }

                // Create file
                String fileName = "profile_" + currentUser.getUserId() + ".jpg";
                File file = new File(directory, fileName);

                // Save bitmap to file with high quality
                FileOutputStream fos = new FileOutputStream(file);
                bitmap.compress(Bitmap.CompressFormat.JPEG, 100, fos);
                fos.close();

                // Update database and current user
                String filePath = file.getAbsolutePath();
                if (dbHelper.updateProfilePicturePath(currentUser.getEmail(), filePath)) {
                    currentUser.setProfilePicturePath(filePath);

                    // Update UI
                    Glide.with(this)
                            .load(bitmap)
                            .apply(RequestOptions.circleCropTransform())
                            .into(profileImageView);

                    // Update ChatActivity header
                    if (getActivity() instanceof ChatActivity) {
                        ChatActivity chatActivity = (ChatActivity) getActivity();
                        // Force refresh the header
                        new Handler(Looper.getMainLooper()).post(() -> {
                            chatActivity.refreshHeader();
                        });
                    }

                    Toast.makeText(getActivity(), "Profile picture updated successfully", Toast.LENGTH_SHORT).show();
                } else {
                    Toast.makeText(getActivity(), "Failed to update profile picture in database", Toast.LENGTH_SHORT).show();
                }
            } catch (IOException e) {
                Log.e(TAG, "Error saving profile picture", e);
                Toast.makeText(getActivity(), "Failed to save profile picture", Toast.LENGTH_SHORT).show();
            }
        }
    }
}