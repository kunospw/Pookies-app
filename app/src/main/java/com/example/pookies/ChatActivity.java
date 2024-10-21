package com.example.pookies;

import android.Manifest;
import android.content.Intent;
import android.content.SharedPreferences;
import android.content.pm.PackageManager;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.graphics.drawable.Drawable;
import android.net.Uri;
import android.os.Bundle;
import android.util.Log;
import android.view.View;
import android.widget.ImageButton;
import android.widget.ImageView;
import android.widget.TextView;
import android.widget.Toast;

import androidx.annotation.Nullable;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.app.ActivityCompat;
import androidx.drawerlayout.widget.DrawerLayout;
import androidx.fragment.app.Fragment;

import com.bumptech.glide.Glide;
import com.bumptech.glide.load.DataSource;
import com.bumptech.glide.load.engine.GlideException;
import com.bumptech.glide.request.RequestListener;
import com.bumptech.glide.request.RequestOptions;
import com.bumptech.glide.request.target.Target;
import com.bumptech.glide.signature.ObjectKey;
import com.google.android.material.navigation.NavigationView;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseUser;
import com.google.firebase.storage.FirebaseStorage;
import com.google.firebase.storage.StorageReference;

public class ChatActivity extends AppCompatActivity {
    private static final int PROFILE_UPDATE_REQUEST = 1001;
    private static final String TAG = "ChatActivity";

    DrawerLayout drawerLayout;
    ImageButton buttonDrawerToggle;
    NavigationView navigationView;
    ImageView userImage;
    TextView textUsername, textEmail;
    DBHelper dbHelper;
    FirebaseStorage storage;
    StorageReference storageRef;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_chat);
        checkBluetoothPermission();

        dbHelper = new DBHelper(this);
        storage = FirebaseStorage.getInstance();
        storageRef = storage.getReference();

        FirebaseUser currentUser = FirebaseAuth.getInstance().getCurrentUser();
        if (currentUser != null) {
            String firebaseUserID = currentUser.getUid();
            String email = currentUser.getEmail();

            SharedPreferences prefs = getSharedPreferences("APP_PREFS", MODE_PRIVATE);
            SharedPreferences.Editor editor = prefs.edit();
            editor.putString("USER_ID", firebaseUserID);
            editor.apply();

            User localUser = dbHelper.getUserByEmail(email);
            if (localUser == null) {
                String displayName = currentUser.getDisplayName();
                dbHelper.insertUser(email, displayName, ""); // Password left empty as managed by Firebase
                localUser = dbHelper.getUserByEmail(email);
            }

            initializeUI();
            updateDrawerHeader();

            View.OnClickListener profileClickListener = view -> {
                loadProfileFragment();  // Load the ProfileFragment
                drawerLayout.close();   // Close the drawer
            };
            userImage.setOnClickListener(profileClickListener);
            textUsername.setOnClickListener(profileClickListener);

        } else {
            redirectToLogin();
        }

        setupNavigationDrawer();

        if (savedInstanceState == null) {
            loadFragment(new ChatFragment());
        }
    }

    private void initializeUI() {
        drawerLayout = findViewById(R.id.drawerLayout);
        buttonDrawerToggle = findViewById(R.id.buttonDrawerToggle);
        navigationView = findViewById(R.id.navigationView);

        View headerView = navigationView.getHeaderView(0);
        userImage = headerView.findViewById(R.id.userImage);
        textUsername = headerView.findViewById(R.id.textUsername);
        textEmail = headerView.findViewById(R.id.textEmail);

        buttonDrawerToggle.setOnClickListener(v -> drawerLayout.open());
        loadProfilePicture();
    }
    private void loadProfilePicture() {
        FirebaseUser currentUser = FirebaseAuth.getInstance().getCurrentUser();
        if (currentUser != null) {
            SharedPreferences prefs = getSharedPreferences("APP_PREFS", MODE_PRIVATE);
            String profilePicUri = prefs.getString("PROFILE_PIC_URI_" + currentUser.getUid(), null);
            if (profilePicUri != null) {
                // Load the image from URI
                userImage.setImageURI(Uri.parse(profilePicUri));
            } else {
                // Set a default profile image if no URI is found
                userImage.setImageResource(R.drawable.person); // Default image resource
            }
        } else {
            userImage.setImageResource(R.drawable.person); // Default image if user is not logged in
        }
    }

    public void updateDrawerHeader() {
        Log.d(TAG, "updateDrawerHeader: Updating drawer header");
        FirebaseUser currentUser = FirebaseAuth.getInstance().getCurrentUser();
        if (currentUser != null) {
            String email = currentUser.getEmail();
            User localUser = dbHelper.getUserByEmail(email);
            if (localUser != null) {
                textUsername.setText(localUser.getName());
                textEmail.setText(localUser.getEmail());

                // Load profile picture
                loadProfilePicture(currentUser.getUid(), email);
            } else {
                Log.e(TAG, "updateDrawerHeader: Local user is null for email: " + email);
            }
        } else {
            Log.e(TAG, "updateDrawerHeader: Current Firebase user is null");
        }
    }
    private void loadProfilePicture(String uid, String email) {
        if (uid != null) {
            loadProfilePictureFromFirebase(uid);
        } else {
            userImage.setImageResource(R.drawable.baseline_person_24);
        }
    }

    private void loadProfilePictureFromFirebase(String uid) {
        StorageReference profilePicRef = storageRef.child("profile_pictures").child(uid).child("profile.jpg");
        profilePicRef.getDownloadUrl().addOnSuccessListener(uri -> {
            Glide.with(this)
                    .load(uri)
                    .apply(RequestOptions.circleCropTransform())
                    .signature(new ObjectKey(System.currentTimeMillis())) // Add a unique signature to force refresh
                    .placeholder(R.drawable.baseline_person_24)
                    .error(R.drawable.baseline_person_24)
                    .listener(new RequestListener<Drawable>() {
                        @Override
                        public boolean onLoadFailed(@Nullable GlideException e, Object model, Target<Drawable> target, boolean isFirstResource) {
                            Log.e(TAG, "onLoadFailed: Failed to load image with Glide", e);
                            return false;
                        }

                        @Override
                        public boolean onResourceReady(Drawable resource, Object model, Target<Drawable> target, DataSource dataSource, boolean isFirstResource) {
                            Log.d(TAG, "onResourceReady: Image loaded successfully with Glide");
                            return false;
                        }
                    })
                    .into(userImage);
        }).addOnFailureListener(e -> {
            Log.e(TAG, "loadProfilePictureFromFirebase: Failed to get download URL", e);
            userImage.setImageResource(R.drawable.baseline_person_24);
        });
    }

    private void loadImageWithGlide(Object imageSource) {
        Glide.with(this)
                .load(imageSource)
                .apply(RequestOptions.circleCropTransform())
                .signature(new ObjectKey(System.currentTimeMillis())) // Add a unique signature to force refresh
                .placeholder(R.drawable.baseline_person_24)
                .error(R.drawable.baseline_person_24)
                .listener(new RequestListener<Drawable>() {
                    @Override
                    public boolean onLoadFailed(@Nullable GlideException e, Object model, Target<Drawable> target, boolean isFirstResource) {
                        Log.e(TAG, "onLoadFailed: Failed to load image with Glide", e);
                        return false;
                    }

                    @Override
                    public boolean onResourceReady(Drawable resource, Object model, Target<Drawable> target, DataSource dataSource, boolean isFirstResource) {
                        Log.d(TAG, "onResourceReady: Image loaded successfully with Glide");
                        return false;
                    }
                })
                .into(userImage);
    }


    public void refreshHeader() {
        Log.d(TAG, "refreshHeader: Refreshing header");
        runOnUiThread(() -> {
            updateDrawerHeader();
            // Force Glide to reload the image
            if (userImage != null) {
                FirebaseUser currentUser = FirebaseAuth.getInstance().getCurrentUser();
                if (currentUser != null) {
                    Glide.with(ChatActivity.this).clear(userImage);
                    loadProfilePicture(currentUser.getUid(), currentUser.getEmail());
                }
            }
        });
    }

    private void setupNavigationDrawer() {
        navigationView.setNavigationItemSelectedListener(item -> {
            Fragment selectedFragment = null;
            int itemId = item.getItemId();

            if (itemId == R.id.navChat) {
                selectedFragment = new ChatFragment();
            } else if (itemId == R.id.navSettings) {
                selectedFragment = new SettingsFragment();
            } else if (itemId == R.id.navFeedback) {
                selectedFragment = new FeedbackFragment();
            } else if (itemId == R.id.navAbout) {
                selectedFragment = new AboutFragment();
            } else if (itemId == R.id.navPolicy) {
                selectedFragment = new PolicyFragment();
            } else if (itemId == R.id.navBluetooth) {
                selectedFragment = new BluetoothFragment();
            }else if (itemId == R.id.navlocation){
                selectedFragment = new GPSFragment();
            } else if (itemId == R.id.navLogout) {
                logOutUser();
                return true;
            } else if (itemId == R.id.navlocation) {
                selectedFragment = new GPSFragment();
            }

            if (selectedFragment != null) {
                loadFragment(selectedFragment);
            }

            drawerLayout.close();
            return true;
        });
    }

    private void loadFragment(Fragment fragment) {
        getSupportFragmentManager().beginTransaction()
                .replace(R.id.fragment_container, fragment)
                .commit();
    }

    private void loadProfileFragment() {
        ProfileFragment profileFragment = new ProfileFragment();
        loadFragment(profileFragment);
    }

    private void logOutUser() {
        FirebaseAuth.getInstance().signOut();
        getSharedPreferences("APP_PREFS", MODE_PRIVATE).edit().remove("USER_ID").apply();
        Toast.makeText(ChatActivity.this, "You have been logged out", Toast.LENGTH_SHORT).show();
        redirectToLogin();
    }

    private void redirectToLogin() {
        Intent intent = new Intent(ChatActivity.this, LoginActivity.class);
        intent.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK | Intent.FLAG_ACTIVITY_CLEAR_TASK);
        startActivity(intent);
        finish();
    }

    private void checkBluetoothPermission() {
        if (ActivityCompat.checkSelfPermission(this, android.Manifest.permission.BLUETOOTH) != PackageManager.PERMISSION_GRANTED ||
                ActivityCompat.checkSelfPermission(this, android.Manifest.permission.BLUETOOTH_ADMIN) != PackageManager.PERMISSION_GRANTED ||
                ActivityCompat.checkSelfPermission(this, Manifest.permission.ACCESS_FINE_LOCATION) != PackageManager.PERMISSION_GRANTED) {
            ActivityCompat.requestPermissions(this, new String[]{
                    android.Manifest.permission.BLUETOOTH,
                    android.Manifest.permission.BLUETOOTH_ADMIN,
                    android.Manifest.permission.ACCESS_FINE_LOCATION
            }, 1);
        }
    }

    @Override
    protected void onActivityResult(int requestCode, int resultCode, @Nullable Intent data) {
        super.onActivityResult(requestCode, resultCode, data);
        if (requestCode == PROFILE_UPDATE_REQUEST && resultCode == RESULT_OK) {
            updateDrawerHeader();
        }
    }
}
