package com.example.pookies;

import android.app.Activity;
import android.app.AlertDialog;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.SharedPreferences;
import android.graphics.Bitmap;
import android.net.Uri;
import android.os.Bundle;
import android.provider.MediaStore;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.EditText;
import android.widget.Toast;
import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.core.content.ContextCompat;
import androidx.fragment.app.Fragment;
import com.google.android.material.floatingactionbutton.FloatingActionButton;
import com.google.android.material.imageview.ShapeableImageView;

import java.io.IOException;

import static android.Manifest.permission.CAMERA;
import static android.content.pm.PackageManager.PERMISSION_GRANTED;

public class ProfileFragment extends Fragment {

    private EditText usernameEditText, emailEditText, passwordEditText;
    private ShapeableImageView profileImageView;
    private FloatingActionButton floatingActionButton;
    private DBHelper dbHelper;
    private String currentUserEmail;
    private SharedPreferences prefs;

    private static final int REQUEST_CAMERA = 100;
    private static final int REQUEST_GALLERY = 101;
    private static final int REQUEST_CAMERA_PERMISSION = 102;

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {
        View view = inflater.inflate(R.layout.fragment_profile, container, false);

        usernameEditText = view.findViewById(R.id.username);
        emailEditText = view.findViewById(R.id.email);
        passwordEditText = view.findViewById(R.id.password);
        profileImageView = view.findViewById(R.id.imageView2);
        floatingActionButton = view.findViewById(R.id.floatingActionButton);

        dbHelper = new DBHelper(getActivity());
        prefs = getActivity().getSharedPreferences("APP_PREFS", Activity.MODE_PRIVATE);
        currentUserEmail = prefs.getString("USER_ID", null);

        if (currentUserEmail != null) {
            User currentUser = dbHelper.getUserByEmail(currentUserEmail);
            if (currentUser != null) {
                emailEditText.setText(currentUser.getEmail());
                usernameEditText.setText(currentUser.getName());
                passwordEditText.setText("********"); // Asterisk password
            }
        } else {
            Toast.makeText(getActivity(), "No user data found", Toast.LENGTH_SHORT).show();
        }

        // Load saved profile picture
        String profilePicUri = prefs.getString("PROFILE_PIC_URI", null);
        if (profilePicUri != null) {
            loadProfilePicture(Uri.parse(profilePicUri));
        }

        floatingActionButton.setOnClickListener(v -> showImagePickerDialog());

        view.findViewById(R.id.editProfileButton).setOnClickListener(v -> showEditProfileDialog());
        view.findViewById(R.id.forgotPasswordButton).setOnClickListener(v -> showChangePasswordDialog());

        return view;
    }

    private void showImagePickerDialog() {
        String[] options = {"Take Photo", "Choose from Gallery", "Remove Profile Picture", "Cancel"};
        AlertDialog.Builder builder = new AlertDialog.Builder(getActivity());
        builder.setTitle("Update Profile Picture")
                .setItems(options, (dialog, which) -> {
                    switch (which) {
                        case 0:
                            checkCameraPermissionAndOpen();
                            break;
                        case 1:
                            openGallery();
                            break;
                        case 2:
                            removeProfilePicture();
                            break;
                        case 3:
                            dialog.dismiss();
                            break;
                    }
                })
                .show();
    }

    private void checkCameraPermissionAndOpen() {
        if (ContextCompat.checkSelfPermission(getActivity(), CAMERA) != PERMISSION_GRANTED) {
            requestPermissions(new String[]{CAMERA}, REQUEST_CAMERA_PERMISSION);
        } else {
            openCamera();
        }
    }

    private void openCamera() {
        Intent intent = new Intent(MediaStore.ACTION_IMAGE_CAPTURE);
        startActivityForResult(intent, REQUEST_CAMERA);
    }

    private void openGallery() {
        Intent intent = new Intent(Intent.ACTION_PICK, MediaStore.Images.Media.EXTERNAL_CONTENT_URI);
        intent.setType("image/*");
        startActivityForResult(intent, REQUEST_GALLERY);
    }

    private void removeProfilePicture() {
        profileImageView.setImageResource(R.drawable.person);
        prefs.edit().remove("PROFILE_PIC_URI").apply();
        Toast.makeText(getActivity(), "Profile picture removed", Toast.LENGTH_SHORT).show();
    }

    private void loadProfilePicture(Uri imageUri) {
        try {
            Bitmap bitmap = MediaStore.Images.Media.getBitmap(getActivity().getContentResolver(), imageUri);
            profileImageView.setImageBitmap(bitmap);
        } catch (IOException e) {
            e.printStackTrace();
            Toast.makeText(getActivity(), "Failed to load profile picture", Toast.LENGTH_SHORT).show();
        }
    }

    private void saveProfilePicture(Uri imageUri) {
        // Convert to content URI if it's not already
        if (!"content".equals(imageUri.getScheme())) {
            Bitmap bitmap;
            try {
                bitmap = MediaStore.Images.Media.getBitmap(getActivity().getContentResolver(), imageUri);
                String path = MediaStore.Images.Media.insertImage(getActivity().getContentResolver(), bitmap, "ProfilePicture", null);
                imageUri = Uri.parse(path);
            } catch (IOException e) {
                e.printStackTrace();
                Toast.makeText(getActivity(), "Failed to process image", Toast.LENGTH_SHORT).show();
                return;
            }
        }

        prefs.edit().putString("PROFILE_PIC_URI", imageUri.toString()).apply();
        loadProfilePicture(imageUri);
        Toast.makeText(getActivity(), "Profile picture updated", Toast.LENGTH_SHORT).show();
    }

    @Override
    public void onActivityResult(int requestCode, int resultCode, @Nullable Intent data) {
        super.onActivityResult(requestCode, resultCode, data);
        if (resultCode == Activity.RESULT_OK) {
            if (requestCode == REQUEST_CAMERA && data != null) {
                Bitmap imageBitmap = (Bitmap) data.getExtras().get("data");
                Uri imageUri = getImageUri(imageBitmap);
                showConfirmationDialog(imageUri);
            } else if (requestCode == REQUEST_GALLERY && data != null) {
                Uri selectedImage = data.getData();
                showConfirmationDialog(selectedImage);
            }
        }
    }

    private Uri getImageUri(Bitmap bitmap) {
        String path = MediaStore.Images.Media.insertImage(getActivity().getContentResolver(), bitmap, "ProfilePicture", null);
        return Uri.parse(path);
    }

    private void showConfirmationDialog(Uri imageUri) {
        AlertDialog.Builder builder = new AlertDialog.Builder(getActivity());
        LayoutInflater inflater = getLayoutInflater();
        View dialogView = inflater.inflate(R.layout.dialog_confirm_profile_picture, null);
        ShapeableImageView previewImageView = dialogView.findViewById(R.id.previewImageView);

        try {
            Bitmap bitmap = MediaStore.Images.Media.getBitmap(getActivity().getContentResolver(), imageUri);
            previewImageView.setImageBitmap(bitmap);
        } catch (IOException e) {
            e.printStackTrace();
        }

        builder.setView(dialogView)
                .setTitle("Confirm Profile Picture")
                .setPositiveButton("Save", (dialog, which) -> saveProfilePicture(imageUri))
                .setNegativeButton("Cancel", (dialog, which) -> dialog.dismiss())
                .show();
    }

    private void showEditProfileDialog() {
        AlertDialog.Builder builder = new AlertDialog.Builder(getActivity());
        View view = getLayoutInflater().inflate(R.layout.dialog_edit_profile, null);
        EditText newUsernameEditText = view.findViewById(R.id.newUsername);

        builder.setView(view)
                .setTitle("Edit Profile")
                .setPositiveButton("Save", (dialog, which) -> {
                    String newUsername = newUsernameEditText.getText().toString();
                    if (!newUsername.isEmpty()) {
                        if (dbHelper.updateUsername(currentUserEmail, newUsername)) {
                            usernameEditText.setText(newUsername);
                            Toast.makeText(getActivity(), "Username updated successfully", Toast.LENGTH_SHORT).show();
                        } else {
                            Toast.makeText(getActivity(), "Failed to update username", Toast.LENGTH_SHORT).show();
                        }
                    } else {
                        Toast.makeText(getActivity(), "Username cannot be empty", Toast.LENGTH_SHORT).show();
                    }
                })
                .setNegativeButton("Cancel", (dialog, which) -> dialog.cancel())
                .show();
    }

    private void showChangePasswordDialog() {
        AlertDialog.Builder builder = new AlertDialog.Builder(getActivity());
        View view = getLayoutInflater().inflate(R.layout.dialog_forgot_password, null);
        EditText currentPasswordEditText = view.findViewById(R.id.currentPassword);
        EditText newPasswordEditText = view.findViewById(R.id.newPassword);
        EditText confirmPasswordEditText = view.findViewById(R.id.confirmPassword);

        builder.setView(view)
                .setTitle("Change Password")
                .setPositiveButton("Save", (dialog, which) -> {
                    String currentPassword = currentPasswordEditText.getText().toString();
                    String newPassword = newPasswordEditText.getText().toString();
                    String confirmPassword = confirmPasswordEditText.getText().toString();

                    if (currentPassword.isEmpty() || newPassword.isEmpty() || confirmPassword.isEmpty()) {
                        Toast.makeText(getActivity(), "All fields are required", Toast.LENGTH_SHORT).show();
                        return;
                    }

                    if (!newPassword.equals(confirmPassword)) {
                        Toast.makeText(getActivity(), "New passwords do not match", Toast.LENGTH_SHORT).show();
                        return;
                    }

                    User currentUser = dbHelper.getUserByEmail(currentUserEmail);
                    if (currentUser != null && currentUser.getPassword().equals(currentPassword)) {
                        if (dbHelper.updatePassword(currentUserEmail, newPassword)) {
                            Toast.makeText(getActivity(), "Password updated successfully", Toast.LENGTH_SHORT).show();
                        } else {
                            Toast.makeText(getActivity(), "Failed to update password", Toast.LENGTH_SHORT).show();
                        }
                    } else {
                        Toast.makeText(getActivity(), "Current password is incorrect", Toast.LENGTH_SHORT).show();
                    }
                })
                .setNegativeButton("Cancel", (dialog, which) -> dialog.cancel())
                .show();
    }

    @Override
    public void onRequestPermissionsResult(int requestCode, @NonNull String[] permissions, @NonNull int[] grantResults) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults);
        if (requestCode == REQUEST_CAMERA_PERMISSION) {
            if (grantResults.length > 0 && grantResults[0] == PERMISSION_GRANTED) {
                openCamera();
            } else {
                Toast.makeText(getActivity(), "Camera permission is required to take a photo", Toast.LENGTH_SHORT).show();
            }
        }
    }
}