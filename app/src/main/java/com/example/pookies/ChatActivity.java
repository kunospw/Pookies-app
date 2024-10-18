package com.example.pookies;

import android.content.Intent;
import android.content.SharedPreferences;
import android.net.Uri;
import android.os.Bundle;
import android.view.MenuItem;
import android.view.View;
import android.widget.ImageButton;
import android.widget.ImageView;
import android.widget.TextView;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;
import androidx.drawerlayout.widget.DrawerLayout;
import androidx.fragment.app.Fragment;

import com.google.android.material.navigation.NavigationView;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseUser;

public class ChatActivity extends AppCompatActivity {

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

        dbHelper = new DBHelper(this);

        FirebaseUser currentUser = FirebaseAuth.getInstance().getCurrentUser();
        if (currentUser != null) {
            String firebaseUserID = currentUser.getUid();
            String email = currentUser.getEmail();

            // Save userID to SharedPreferences
            SharedPreferences prefs = getSharedPreferences("APP_PREFS", MODE_PRIVATE);
            SharedPreferences.Editor editor = prefs.edit();
            editor.putString("USER_ID", firebaseUserID);
            editor.apply();

            // Retrieve user data from SQLite
            User localUser = dbHelper.getUserByEmail(email);
            if (localUser == null) {
                // If user doesn't exist in SQLite, create a new entry
                String displayName = currentUser.getDisplayName();
                dbHelper.insertUser(email, displayName, ""); // Password field left empty as it's managed by Firebase
                localUser = dbHelper.getUserByEmail(email);
            }

            // Initialize UI components
            initializeUI();

            // Set user data in the navigation header
            if (localUser != null) {
                textUsername.setText(localUser.getName());
                textEmail.setText(localUser.getEmail());
                Toast.makeText(this, "Welcome, " + localUser.getName(), Toast.LENGTH_SHORT).show();
            } else {
                Toast.makeText(this, "Welcome, User", Toast.LENGTH_SHORT).show();
            }

            // Set click listeners for userImage and textUsername to open ProfileFragment
            userImage.setOnClickListener(new View.OnClickListener() {
                @Override
                public void onClick(View view) {
                    loadProfileFragment();
                }
            });

            textUsername.setOnClickListener(new View.OnClickListener() {
                @Override
                public void onClick(View view) {
                    loadProfileFragment();
                }
            });

        } else {
            // No Firebase user is logged in, redirect to LoginActivity
            redirectToLogin();
        }

        setupNavigationDrawer();

        // Load default fragment (ChatFragment)
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
            } else if (itemId == R.id.navLogout) {
                logOutUser();
                return true;
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
        loadFragment(new ProfileFragment());
        drawerLayout.close();
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
}