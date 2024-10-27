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
import android.graphics.drawable.Drawable;
import androidx.core.app.ActivityCompat;
import androidx.core.content.ContextCompat;
import android.net.Uri;
import android.os.Bundle;
import android.provider.MediaStore;
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

import com.bumptech.glide.Glide;
import com.bumptech.glide.request.target.CustomTarget;
import com.bumptech.glide.request.transition.Transition;
import com.bumptech.glide.signature.ObjectKey;
import com.google.android.material.floatingactionbutton.FloatingActionButton;
import com.google.android.material.imageview.ShapeableImageView;
import com.google.android.material.textfield.TextInputEditText;
import com.google.firebase.auth.AuthCredential;
import com.google.firebase.auth.EmailAuthProvider;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseUser;
import com.google.firebase.auth.UserProfileChangeRequest;
import com.google.firebase.storage.FirebaseStorage;
import com.google.firebase.storage.StorageReference;
import com.google.firebase.storage.UploadTask;

import java.io.ByteArrayOutputStream;
import java.io.IOException;

public class ProfileFragment extends Fragment {

    private static final int REQUEST_CAMERA = 100;
    private static final int REQUEST_GALLERY = 101;
    private static final int CAMERA_PERMISSION_CODE = 102;
    private static final String PREFS_NAME = "APP_PREFS";
    private static final String USER_ID_KEY = "USER_ID";

    private EditText usernameEditText, emailEditText, passwordEditText;
    private ShapeableImageView profileImageView;
    private FloatingActionButton floatingActionButton;
    private Button editProfileButton, forgotPasswordButton;

    private FirebaseAuth mAuth;
    private SharedPreferences prefs;
    private FirebaseStorage storage;
    private StorageReference storageRef;

    @Override
    public void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        mAuth = FirebaseAuth.getInstance();
        prefs = requireContext().getSharedPreferences("APP_PREFS", Context.MODE_PRIVATE);
        storage = FirebaseStorage.getInstance();
        storageRef = storage.getReference();
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
        // Replace this with your actual login activity
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

    private void setupListeners() {
        floatingActionButton.setOnClickListener(v -> showImagePickerDialog());
        editProfileButton.setOnClickListener(v -> showEditProfileDialog());
        forgotPasswordButton.setOnClickListener(v -> showChangePasswordDialog());
    }

    private void loadUserData() {
        FirebaseUser firebaseUser = mAuth.getCurrentUser();
        if (firebaseUser != null) {
            emailEditText.setText(firebaseUser.getEmail());
            usernameEditText.setText(firebaseUser.getDisplayName());
            loadProfilePicture(firebaseUser.getUid());
            passwordEditText.setText("********");
        } else {
            navigateToLogin();
        }
    }

    private void loadProfilePicture(String uid) {
        StorageReference profilePicRef = storageRef.child("profile_pictures").child(uid).child("profile.jpg");
        profilePicRef.getDownloadUrl().addOnSuccessListener(uri -> {
            Glide.with(this)
                    .load(uri)
                    .placeholder(R.drawable.baseline_person_24)
                    .error(R.drawable.baseline_person_24)
                    .signature(new ObjectKey(System.currentTimeMillis()))
                    .into(profileImageView);
        }).addOnFailureListener(e -> {
            profileImageView.setImageResource(R.drawable.baseline_person_24);
        });
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
        FirebaseUser user = mAuth.getCurrentUser();

        if (user != null && user.getUid() != null) {
            StorageReference profilePicRef = storageRef.child("profile_pictures").child(user.getUid()).child("profile.jpg");
            profilePicRef.delete().addOnSuccessListener(aVoid -> {
                Log.d("ProfilePicture", "Profile picture deleted from Firebase Storage");

                // Update Firebase user profile
                UserProfileChangeRequest profileUpdates = new UserProfileChangeRequest.Builder()
                        .setPhotoUri(null)
                        .build();
                user.updateProfile(profileUpdates).addOnCompleteListener(task -> {
                    if (task.isSuccessful()) {
                        Log.d("ProfilePicture", "User profile updated.");
                        // Update UI
                        profileImageView.setImageResource(R.drawable.baseline_person_24);
                        // Notify ChatActivity to refresh the header
                        if (getActivity() instanceof ChatActivity) {
                            ((ChatActivity) getActivity()).refreshHeader();
                        }
                        Toast.makeText(getActivity(), "Profile picture removed", Toast.LENGTH_SHORT).show();
                    } else {
                        Log.e("ProfilePicture", "Failed to update user profile", task.getException());
                        Toast.makeText(getActivity(), "Failed to remove profile picture", Toast.LENGTH_SHORT).show();
                    }
                });
            }).addOnFailureListener(e -> {
                Log.e("ProfilePicture", "Failed to delete profile picture from Firebase Storage", e);
                Toast.makeText(getActivity(), "Failed to remove profile picture", Toast.LENGTH_SHORT).show();
            });
        } else {
            Toast.makeText(getActivity(), "User not logged in", Toast.LENGTH_SHORT).show();
        }
    }

    private void showChangePasswordDialog() {
        View dialogView = getLayoutInflater().inflate(R.layout.dialog_change_password, null);
        TextInputEditText currentPasswordInput = dialogView.findViewById(R.id.currentPassword);
        TextInputEditText newPasswordInput = dialogView.findViewById(R.id.newPassword);
        TextInputEditText confirmPasswordInput = dialogView.findViewById(R.id.confirmPassword);

        AlertDialog dialog = new AlertDialog.Builder(getActivity())
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

        return true;
    }

    private void changePassword(String currentPassword, String newPassword, AlertDialog dialog) {
        FirebaseUser firebaseUser = mAuth.getCurrentUser();
        if (firebaseUser != null) {
            firebaseUser.reauthenticate(EmailAuthProvider.getCredential(firebaseUser.getEmail(), currentPassword))
                    .addOnCompleteListener(task -> {
                        if (task.isSuccessful()) {
                            firebaseUser.updatePassword(newPassword)
                                    .addOnCompleteListener(task1 -> {
                                        if (task1.isSuccessful()) {
                                            Toast.makeText(getActivity(), "Password updated successfully", Toast.LENGTH_SHORT).show();
                                            dialog.dismiss();
                                        } else {
                                            Toast.makeText(getActivity(), "Failed to update password", Toast.LENGTH_SHORT).show();
                                        }
                                    });
                        } else {
                            Toast.makeText(getActivity(), "Authentication failed", Toast.LENGTH_SHORT).show();
                        }
                    });
        }
    }

    private void showEditProfileDialog() {
        View dialogView = getLayoutInflater().inflate(R.layout.dialog_edit_profile, null);
        EditText newUsernameInput = dialogView.findViewById(R.id.newUsername);
        EditText newEmailInput = dialogView.findViewById(R.id.newEmail);

        newUsernameInput.setText(usernameEditText.getText());
        newEmailInput.setText(emailEditText.getText());

        new AlertDialog.Builder(getActivity())
                .setView(dialogView)
                .setTitle("Edit Profile")
                .setPositiveButton("Save", (dialog, which) -> {
                    String newUsername = newUsernameInput.getText().toString().trim();
                    String newEmail = newEmailInput.getText().toString().trim();

                    if (!newUsername.equals(usernameEditText.getText().toString())) {
                        updateUsername(newUsername);
                    }

                    if (!newEmail.equals(emailEditText.getText().toString())) {
                        updateEmail(newEmail);
                    }
                })
                .setNegativeButton("Cancel", null)
                .show();
    }

    private void updateUsername(String newUsername) {
        FirebaseUser firebaseUser = mAuth.getCurrentUser();
        if (firebaseUser != null) {
            UserProfileChangeRequest profileUpdates = new UserProfileChangeRequest.Builder()
                    .setDisplayName(newUsername)
                    .build();

            firebaseUser.updateProfile(profileUpdates)
                    .addOnCompleteListener(task -> {
                        if (task.isSuccessful()) {
                            usernameEditText.setText(newUsername);
                            if (getActivity() instanceof ChatActivity) {
                                ((ChatActivity) getActivity()).refreshHeader();
                            }
                            Toast.makeText(getActivity(), "Username updated successfully", Toast.LENGTH_SHORT).show();
                        } else {
                            Toast.makeText(getActivity(), "Failed to update username: " + task.getException().getMessage(), Toast.LENGTH_SHORT).show();
                        }
                    });
        }
    }

    private void updateEmail(String newEmail) {
        FirebaseUser firebaseUser = mAuth.getCurrentUser();

        if (firebaseUser != null) {
            // Re-authenticate the user
            showReauthenticationDialog(firebaseUser, () -> {
                firebaseUser.verifyBeforeUpdateEmail(newEmail)
                        .addOnCompleteListener(task -> {
                            if (task.isSuccessful()) {
                                Toast.makeText(getActivity(), "Verification email sent to " + newEmail + ". Please check your email to complete the change.", Toast.LENGTH_LONG).show();
                                showEmailChangeInstructionsDialog();
                            } else {
                                Toast.makeText(getActivity(), "Failed to send verification email: " + task.getException().getMessage(), Toast.LENGTH_LONG).show();
                            }
                        });
            });
        } else {
            // Handle SQLite user
            Toast.makeText(getActivity(), "Email change is not supported for local accounts.", Toast.LENGTH_SHORT).show();
        }
    }

    private void showReauthenticationDialog(FirebaseUser user, Runnable onSuccess) {
        AlertDialog.Builder builder = new AlertDialog.Builder(getActivity());
        builder.setTitle("Re-authenticate");
        View viewInflated = LayoutInflater.from(getActivity()).inflate(R.layout.dialog_reauthenticate, null);
        final EditText input = viewInflated.findViewById(R.id.input);
        builder.setView(viewInflated);

        builder.setPositiveButton(android.R.string.ok, (dialog, which) -> {
            String password = input.getText().toString();
            AuthCredential credential = EmailAuthProvider.getCredential(user.getEmail(), password);

            user.reauthenticate(credential)
                    .addOnCompleteListener(task -> {
                        if (task.isSuccessful()) {
                            onSuccess.run();
                        } else {
                            Toast.makeText(getActivity(), "Re-authentication failed", Toast.LENGTH_SHORT).show();
                        }
                    });
        });
        builder.setNegativeButton(android.R.string.cancel, (dialog, which) -> dialog.cancel());

        builder.show();
    }

    private void showEmailChangeInstructionsDialog() {
        new AlertDialog.Builder(getActivity())
                .setTitle("Email Change Instructions")
                .setMessage("To complete your email change:\n\n" +
                        "1. Check your new email inbox for a verification link.\n" +
                        "2. Click the link to verify your new email.\n" +
                        "3. After verification, sign out and sign back in to see the changes.\n\n" +
                        "Your email will only be updated after you verify and sign in again.")
                .setPositiveButton("OK", null)
                .show();
    }

    private void updateLocalData(String newUsername, String newEmail) {
        usernameEditText.setText(newUsername);
        emailEditText.setText(newEmail);
        Toast.makeText(getActivity(), "Profile updated successfully", Toast.LENGTH_SHORT).show();
    }

    @Override
    public void onActivityResult(int requestCode, int resultCode, @Nullable Intent data) {
        super.onActivityResult(requestCode, resultCode, data);

        if (resultCode == Activity.RESULT_OK) {
            if ((requestCode == REQUEST_CAMERA || requestCode == REQUEST_GALLERY) && data != null) {
                Bitmap imageBitmap;
                if (requestCode == REQUEST_CAMERA) {
                    imageBitmap = (Bitmap) data.getExtras().get("data");
                } else {
                    Uri selectedImage = data.getData();
                    try {
                        imageBitmap = MediaStore.Images.Media.getBitmap(getActivity().getContentResolver(), selectedImage);
                    } catch (IOException e) {
                        e.printStackTrace();
                        Toast.makeText(getActivity(), "Failed to load image", Toast.LENGTH_SHORT).show();
                        return;
                    }
                }

                if (imageBitmap != null) {
                    // Update UI
                    profileImageView.setImageBitmap(imageBitmap);

                    // Upload to Firebase and save locally
                    uploadProfilePicture(imageBitmap);

                    // Show alert
                    Toast.makeText(getActivity(), "Updating profile picture...", Toast.LENGTH_SHORT).show();

                    // Update header in ChatActivity
                    if (getActivity() instanceof ChatActivity) {
                        ((ChatActivity) getActivity()).refreshHeader();
                    }
                }
            }
        }
    }

    private void uploadProfilePicture(Bitmap bitmap) {
        FirebaseUser user = mAuth.getCurrentUser();
        if (user != null && user.getUid() != null) {
            ByteArrayOutputStream baos = new ByteArrayOutputStream();
            bitmap.compress(Bitmap.CompressFormat.JPEG, 100, baos);
            byte[] data = baos.toByteArray();

            StorageReference profilePicRef = storageRef.child("profile_pictures").child(user.getUid()).child("profile.jpg");

            UploadTask uploadTask = profilePicRef.putBytes(data);
            uploadTask.addOnSuccessListener(taskSnapshot -> {
                profilePicRef.getDownloadUrl().addOnSuccessListener(uri -> {
                    UserProfileChangeRequest profileUpdates = new UserProfileChangeRequest.Builder()
                            .setPhotoUri(uri)
                            .build();
                    user.updateProfile(profileUpdates)
                            .addOnSuccessListener(aVoid -> {
                                Toast.makeText(getActivity(), "Profile picture updated successfully", Toast.LENGTH_SHORT).show();
                                // Update UI
                                profileImageView.setImageBitmap(bitmap);
                                // Notify ChatActivity to refresh the header
                                if (getActivity() instanceof ChatActivity) {
                                    ((ChatActivity) getActivity()).refreshHeader();
                                }
                            })
                            .addOnFailureListener(e -> {
                                Toast.makeText(getActivity(), "Failed to update profile: " + e.getMessage(), Toast.LENGTH_SHORT).show();
                                Log.e("UploadError", "Failed to update profile: " + e.getMessage());
                            });
                }).addOnFailureListener(e -> {
                    Toast.makeText(getActivity(), "Failed to get download URL: " + e.getMessage(), Toast.LENGTH_SHORT).show();
                    Log.e("UploadError", "Failed to get download URL: " + e.getMessage());
                });
            }).addOnFailureListener(e -> {
                Toast.makeText(getActivity(), "Failed to upload profile picture: " + e.getMessage(), Toast.LENGTH_SHORT).show();
                Log.e("UploadError", "Failed to upload profile picture: " + e.getMessage());
            });
        } else {
            Toast.makeText(getActivity(), "User not logged in", Toast.LENGTH_SHORT).show();
        }
    }
}
