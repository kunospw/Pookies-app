package com.example.pookies;

import android.Manifest;
import android.content.Intent;
import android.content.SharedPreferences;
import android.content.pm.PackageManager;
import android.os.Bundle;
import android.os.Handler;
import android.os.Looper;
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
import com.bumptech.glide.request.RequestOptions;
import com.bumptech.glide.load.engine.DiskCacheStrategy;
import com.google.android.material.navigation.NavigationView;
import java.io.File;

public class ChatActivity extends AppCompatActivity {
    private static final int PROFILE_UPDATE_REQUEST = 1001;
    private static final String TAG = "ChatActivity";

    DrawerLayout drawerLayout;
    ImageButton buttonDrawerToggle;
    NavigationView navigationView;
    ImageView userImage;
    TextView textUsername, textEmail;
    DBHelper dbHelper;
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_chat);
        checkBluetoothPermission();

        dbHelper = new DBHelper(this);
        SharedPreferences prefs = getSharedPreferences("UserPrefs", MODE_PRIVATE);
        String email = prefs.getString("email", null);
        String userId = prefs.getString("user_id", null);

        if (email != null && userId != null) {
            User currentUser = dbHelper.getUserByEmail(email);
            if (currentUser != null) {
                initializeUI();
                updateDrawerHeader();

                View.OnClickListener profileClickListener = view -> {
                    loadProfileFragment();
                    drawerLayout.close();
                };
                userImage.setOnClickListener(profileClickListener);
                textUsername.setOnClickListener(profileClickListener);
            } else {
                redirectToLogin();
            }
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
        SharedPreferences prefs = getSharedPreferences("UserPrefs", MODE_PRIVATE);
        String email = prefs.getString("email", null);

        if (email != null) {
            User currentUser = dbHelper.getUserByEmail(email);
            if (currentUser != null && currentUser.getProfilePicturePath() != null) {
                File profilePicFile = new File(currentUser.getProfilePicturePath());
                if (profilePicFile.exists()) {
                    Glide.with(this)
                            .load(profilePicFile)
                            .apply(RequestOptions.circleCropTransform())
                            .placeholder(R.drawable.person)
                            .error(R.drawable.person)
                            .skipMemoryCache(true)  // Skip memory cache
                            .diskCacheStrategy(DiskCacheStrategy.NONE)  // Skip disk cache
                            .into(userImage);
                } else {
                    userImage.setImageResource(R.drawable.person);
                }
            } else {
                userImage.setImageResource(R.drawable.person);
            }
        } else {
            userImage.setImageResource(R.drawable.person);
        }
    }
    public void updateDrawerHeader() {
        Log.d(TAG, "updateDrawerHeader: Updating drawer header");
        SharedPreferences prefs = getSharedPreferences("UserPrefs", MODE_PRIVATE);
        String email = prefs.getString("email", null);

        if (email != null) {
            User currentUser = dbHelper.getUserByEmail(email);
            if (currentUser != null) {
                textUsername.setText(currentUser.getName());
                textEmail.setText(currentUser.getEmail());
                loadProfilePicture();
            } else {
                Log.e(TAG, "updateDrawerHeader: Local user is null for email: " + email);
            }
        } else {
            Log.e(TAG, "updateDrawerHeader: No email found in SharedPreferences");
        }
    }

    public void refreshHeader() {
        Log.d(TAG, "refreshHeader: Refreshing header");
        runOnUiThread(() -> {
            // Clear any cached data
            dbHelper = new DBHelper(this);  // Reinitialize DBHelper to ensure fresh data

            // Clear Glide cache for this view
            if (userImage != null) {
                Glide.with(this).clear(userImage);
            }

            updateDrawerHeader();
            loadProfilePicture();
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
        // First clear all preferences
        getSharedPreferences("UserPrefs", MODE_PRIVATE)
                .edit()
                .clear()  // Clear all preferences instead of removing individual items
                .commit();  // Use commit() instead of apply() to ensure immediate execution

        // Clear any other relevant app data
        dbHelper.close();  // Close the database connection

        // Show toast and redirect with a small delay to ensure proper cleanup
        Toast.makeText(ChatActivity.this, "You have been logged out", Toast.LENGTH_SHORT).show();
        new android.os.Handler().postDelayed(() -> {
            Intent intent = new Intent(ChatActivity.this, LoginActivity.class);
            intent.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK | Intent.FLAG_ACTIVITY_CLEAR_TASK);
            startActivity(intent);
            finish();
        }, 100);
    }
    private void redirectToLogin() {
        Intent intent = new Intent(ChatActivity.this, LoginActivity.class);
        // Add these flags to clear the activity stack properly
        intent.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK
                | Intent.FLAG_ACTIVITY_CLEAR_TASK
                | Intent.FLAG_ACTIVITY_CLEAR_TOP);
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