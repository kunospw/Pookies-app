package com.example.pookies;

import android.content.Intent;
import android.content.SharedPreferences;
import android.graphics.Bitmap;
import android.net.Uri;
import android.os.Bundle;
import android.provider.MediaStore;
import android.view.View;
import android.widget.ImageButton;
import android.widget.ImageView;
import android.widget.TextView;
import android.widget.Toast;

import androidx.appcompat.app.AppCompatActivity;
import androidx.drawerlayout.widget.DrawerLayout;
import androidx.fragment.app.Fragment;

import com.google.android.material.navigation.NavigationView;

import java.io.IOException;

public class ChatActivity extends AppCompatActivity {

    DrawerLayout drawerLayout;
    ImageButton buttonDrawerToggle;
    NavigationView navigationView;
    ImageView userImage;
    TextView textUsername, textEmail;
    SharedPreferences prefs;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_chat);

        prefs = getSharedPreferences("APP_PREFS", MODE_PRIVATE);
        String userID = prefs.getString("USER_ID", null);

        if (userID != null) {
            DBHelper dbHelper = new DBHelper(this);
            User currentUser = dbHelper.getUserByEmail(userID);
            if (currentUser != null) {
                String displayName = currentUser.getName();
                String email = currentUser.getEmail();

                Toast.makeText(this, "Welcome, " + displayName, Toast.LENGTH_SHORT).show();

                drawerLayout = findViewById(R.id.drawerLayout);
                buttonDrawerToggle = findViewById(R.id.buttonDrawerToggle);
                navigationView = findViewById(R.id.navigationView);

                buttonDrawerToggle.setOnClickListener(v -> drawerLayout.open());

                View headerView = navigationView.getHeaderView(0);
                userImage = headerView.findViewById(R.id.userImage);
                textUsername = headerView.findViewById(R.id.textUsername);
                textEmail = headerView.findViewById(R.id.textEmail);

                textUsername.setText(displayName);
                textEmail.setText(email);

                loadProfilePicture();

                userImage.setOnClickListener(v -> loadProfileFragment());
                textUsername.setOnClickListener(v -> loadProfileFragment());

            } else {
                redirectToLogin();
            }
        } else {
            redirectToLogin();
        }

        navigationView.setNavigationItemSelectedListener(item -> {
            Fragment selectedFragment = null;
            int itemId = item.getItemId();
            if (itemId == R.id.navChat) {
                selectedFragment = new ChatFragment();
            } else if (itemId == R.id.navSettings) {
                selectedFragment = new SettingsFragment();
            } else if (itemId == R.id.navLogout) {
                logOutUser();
                return true;
            }

            if (selectedFragment != null) {
                getSupportFragmentManager().beginTransaction()
                        .replace(R.id.fragment_container, selectedFragment)
                        .commit();
            }

            drawerLayout.close();
            return true;
        });

        if (savedInstanceState == null) {
            getSupportFragmentManager().beginTransaction()
                    .replace(R.id.fragment_container, new ChatFragment())
                    .commit();
        }
    }

    private void loadProfilePicture() {
        String profilePicUri = prefs.getString("PROFILE_PIC_URI", null);
        if (profilePicUri != null) {
            try {
                Uri uri = Uri.parse(profilePicUri);
                userImage.setImageURI(uri);

                // If setImageURI fails (returns null drawable), fall back to bitmap method
                if (userImage.getDrawable() == null) {
                    Bitmap bitmap = MediaStore.Images.Media.getBitmap(getContentResolver(), uri);
                    userImage.setImageBitmap(bitmap);
                }
            } catch (IOException e) {
                e.printStackTrace();
                Toast.makeText(this, "Failed to load profile picture", Toast.LENGTH_SHORT).show();
                userImage.setImageResource(R.drawable.person);
            }
        } else {
            userImage.setImageResource(R.drawable.person);
        }
    }

    private void loadProfileFragment() {
        getSupportFragmentManager().beginTransaction()
                .replace(R.id.fragment_container, new ProfileFragment())
                .commit();
        drawerLayout.close();
    }

    private void redirectToLogin() {
        Intent intent = new Intent(ChatActivity.this, LoginActivity.class);
        startActivity(intent);
        finish();
    }

    private void logOutUser() {
        SharedPreferences prefs = getSharedPreferences("APP_PREFS", MODE_PRIVATE);
        prefs.edit().remove("USER_ID").apply();

        Toast.makeText(ChatActivity.this, "You have been logged out", Toast.LENGTH_SHORT).show();

        // Redirect to LoginActivity
        Intent intent = new Intent(ChatActivity.this, LoginActivity.class);
        intent.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK | Intent.FLAG_ACTIVITY_CLEAR_TASK);
        startActivity(intent);
        finish();
    }

    @Override
    protected void onResume() {
        super.onResume();
        loadProfilePicture();
    }
}