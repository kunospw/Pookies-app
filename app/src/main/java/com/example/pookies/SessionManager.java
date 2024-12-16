package com.example.pookies;

import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.util.Log;
import android.widget.Toast;

import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseUser;
import com.google.firebase.database.DataSnapshot;
import com.google.firebase.database.DatabaseReference;
import com.google.firebase.database.FirebaseDatabase;

public class SessionManager {
    private static final String TAG = "SessionManager"; // Logging tag
    private static final String PREF_NAME = "PookiesSession";
    private static final String KEY_IS_LOGGED_IN = "isLoggedIn";
    private static final String KEY_USER_EMAIL = "userEmail";
    private static final String KEY_USER_ID = "userId";
    private static final String KEY_USER_NAME = "userName";
    private static final String KEY_USER_PROFILE_PICTURE = "profilePicture";

    private static SessionManager instance;
    private final SharedPreferences pref;
    private final SharedPreferences.Editor editor;
    private final Context context;
    private final FirebaseAuth firebaseAuth;

    private SessionManager(Context context) {
        this.context = context.getApplicationContext();
        pref = context.getSharedPreferences(PREF_NAME, Context.MODE_PRIVATE);
        editor = pref.edit();
        firebaseAuth = FirebaseAuth.getInstance();

        // Sync SharedPreferences with Firebase state on initialization
        syncWithFirebase();
    }

    public static synchronized SessionManager getInstance(Context context) {
        if (instance == null) {
            instance = new SessionManager(context);
        }
        return instance;
    }

    private void syncWithFirebase() {
        FirebaseUser user = firebaseAuth.getCurrentUser();
        if (user != null) {
            Log.d(TAG, "User is logged in. Syncing with info table.");
            fetchAndSyncInfoTable(user.getUid());
        } else {
            Log.d(TAG, "No user found in Firebase. Clearing session.");
            clearSession();
        }
    }

    public void createLoginSession(String email, String userId) {
        editor.putBoolean(KEY_IS_LOGGED_IN, true);
        editor.putString(KEY_USER_EMAIL, email);
        editor.putString(KEY_USER_ID, userId);
        editor.commit();

        Log.d(TAG, "Login session created for userId: " + userId);
        // Fetch additional data from info table and update session
        fetchAndSyncInfoTable(userId);
    }

    private void fetchAndSyncInfoTable(String userId) {
        DatabaseReference infoTableRef = FirebaseDatabase.getInstance()
                .getReference("users").child(userId).child("info");

        infoTableRef.get().addOnCompleteListener(task -> {
            if (task.isSuccessful() && task.getResult() != null) {
                DataSnapshot snapshot = task.getResult();
                String name = snapshot.child("name").getValue(String.class);
                String profilePicture = snapshot.child("profilePicture").getValue(String.class);

                // Handle missing fields gracefully
                if (name == null || profilePicture == null) {
                    Log.w(TAG, "Some fields are missing in info table for userId: " + userId);
                    Toast.makeText(context, "Incomplete profile data. Please update your profile.", Toast.LENGTH_SHORT).show();
                    // Proceed with partial data
                    name = (name != null) ? name : "Unknown";
                    profilePicture = (profilePicture != null) ? profilePicture : "";
                }

                Log.d(TAG, "Info table fetched successfully for userId: " + userId);
                updateSession(name, profilePicture);

            } else {
                Log.e(TAG, "Failed to fetch info table for userId: " + userId, task.getException());
                Toast.makeText(context, "Error loading profile. Please try again later.", Toast.LENGTH_SHORT).show();
                // Allow login but warn about incomplete state
                updateSession("Unknown", "");
            }
        });
    }



    public void updateSession(String name, String profilePicture) {
        editor.putString(KEY_USER_NAME, name);
        editor.putString(KEY_USER_PROFILE_PICTURE, profilePicture);
        editor.commit();
        Log.d(TAG, "Session updated: name=" + name + ", profilePicture=" + profilePicture);
    }

    public boolean isLoggedIn() {
        boolean isLoggedIn = pref.getBoolean(KEY_IS_LOGGED_IN, false) && firebaseAuth.getCurrentUser() != null;
        Log.d(TAG, "isLoggedIn check: " + isLoggedIn);
        return isLoggedIn;
    }

    public String getUserEmail() {
        return pref.getString(KEY_USER_EMAIL, null);
    }

    public String getUserId() {
        return pref.getString(KEY_USER_ID, null);
    }

    public String getUserName() {
        return pref.getString(KEY_USER_NAME, null);
    }

    public String getUserProfilePicture() {
        return pref.getString(KEY_USER_PROFILE_PICTURE, null);
    }

    public FirebaseUser getCurrentUser() {
        return firebaseAuth.getCurrentUser();
    }

    public void clearSession() {
        Log.d(TAG, "Clearing session data.");
        editor.clear();
        editor.commit();
    }

    public void logoutUser() {
        Log.d(TAG, "Logging out user.");
        firebaseAuth.signOut();
        clearSession();

        // Redirect to MainActivity
        Intent intent = new Intent(context, MainActivity.class);
        intent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK | Intent.FLAG_ACTIVITY_CLEAR_TASK);
        context.startActivity(intent);
    }

    public void checkLogin() {
        if (!isLoggedIn()) {
            Log.d(TAG, "User is not logged in. Redirecting to MainActivity.");
            // User is not logged in, redirect to MainActivity
            redirectToMainActivity(null);
        }
    }

    private void redirectToMainActivity(String message) {
        Intent intent = new Intent(context, MainActivity.class);
        intent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK | Intent.FLAG_ACTIVITY_CLEAR_TASK);

        if (message != null) {
            intent.putExtra("error_message", message);
        }

        context.startActivity(intent);
    }
}
