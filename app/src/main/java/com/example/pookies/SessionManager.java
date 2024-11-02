package com.example.pookies;

import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.util.Log;

import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseUser;

import java.util.concurrent.atomic.AtomicBoolean;

public class SessionManager {
    private static final String TAG = "SessionManager";
    private static final String PREF_NAME = "PookiesSession";
    private static final String KEY_IS_LOGGED_IN = "isLoggedIn";
    private static final String KEY_USER_EMAIL = "userEmail";
    private static final String KEY_USER_ID = "userId";
    private static final String KEY_LAST_SYNC = "lastSync";

    private static volatile SessionManager instance;
    private final SharedPreferences pref;
    private final SharedPreferences.Editor editor;
    private final Context context;
    private final FirebaseAuth firebaseAuth;
    private final AtomicBoolean isSyncing;
    private FirebaseAuth.AuthStateListener authStateListener;

    private SessionManager(Context context) {
        this.context = context.getApplicationContext();
        this.pref = context.getSharedPreferences(PREF_NAME, Context.MODE_PRIVATE);
        this.editor = pref.edit();
        this.firebaseAuth = FirebaseAuth.getInstance();
        this.isSyncing = new AtomicBoolean(false);

        setupAuthStateListener();
        syncWithFirebase();
    }

    public static synchronized SessionManager getInstance(Context context) {
        if (instance == null) {
            synchronized (SessionManager.class) {
                if (instance == null) {
                    instance = new SessionManager(context);
                }
            }
        }
        return instance;
    }

    private void setupAuthStateListener() {
        authStateListener = firebaseAuth -> {
            FirebaseUser user = firebaseAuth.getCurrentUser();
            if (user != null) {
                // User is signed in
                if (!isLoggedIn()) {
                    createLoginSession(user.getEmail(), user.getUid());
                }
            } else {
                // User is signed out
                if (isLoggedIn()) {
                    clearSession();
                }
            }
        };
        firebaseAuth.addAuthStateListener(authStateListener);
    }

    private void syncWithFirebase() {
        if (isSyncing.compareAndSet(false, true)) {
            try {
                FirebaseUser user = firebaseAuth.getCurrentUser();
                if (user != null) {
                    // Update session only if the data is different
                    String currentEmail = getUserEmail();
                    String currentUserId = getUserId();

                    if (!user.getEmail().equals(currentEmail) || !user.getUid().equals(currentUserId)) {
                        createLoginSession(user.getEmail(), user.getUid());
                    }

                    // Update last sync timestamp
                    editor.putLong(KEY_LAST_SYNC, System.currentTimeMillis());
                    editor.apply();
                } else {
                    clearSession();
                }
            } catch (Exception e) {
                Log.e(TAG, "Error during sync with Firebase: ", e);
            } finally {
                isSyncing.set(false);
            }
        }
    }

    public void createLoginSession(String email, String userId) {
        if (email == null || userId == null) {
            Log.e(TAG, "Attempted to create login session with null values");
            return;
        }

        editor.putBoolean(KEY_IS_LOGGED_IN, true);
        editor.putString(KEY_USER_EMAIL, email);
        editor.putString(KEY_USER_ID, userId);
        editor.putLong(KEY_LAST_SYNC, System.currentTimeMillis());
        editor.apply(); // Using apply() instead of commit() for better performance
    }

    public boolean isLoggedIn() {
        FirebaseUser currentUser = firebaseAuth.getCurrentUser();
        boolean sharedPrefLoggedIn = pref.getBoolean(KEY_IS_LOGGED_IN, false);

        // Check if both Firebase and SharedPreferences agree on login status
        return currentUser != null && sharedPrefLoggedIn;
    }

    public String getUserEmail() {
        if (!isLoggedIn()) {
            return null;
        }
        return pref.getString(KEY_USER_EMAIL, null);
    }

    public String getUserId() {
        if (!isLoggedIn()) {
            return null;
        }
        return pref.getString(KEY_USER_ID, null);
    }

    public FirebaseUser getCurrentUser() {
        return isLoggedIn() ? firebaseAuth.getCurrentUser() : null;
    }

    public void clearSession() {
        editor.clear();
        editor.apply();
    }

    public void logoutUser() {
        try {
            firebaseAuth.signOut();
            clearSession();

            Intent intent = new Intent(context, MainActivity.class);
            intent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK | Intent.FLAG_ACTIVITY_CLEAR_TASK);
            context.startActivity(intent);
        } catch (Exception e) {
            Log.e(TAG, "Error during logout: ", e);
        }
    }

    public void checkLogin() {
        if (!isLoggedIn()) {
            Intent intent = new Intent(context, MainActivity.class);
            intent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK | Intent.FLAG_ACTIVITY_CLEAR_TASK);
            context.startActivity(intent);
        }
    }

    // Call this method in your Application's onDestroy or when you're done with the SessionManager
    public void cleanup() {
        if (authStateListener != null) {
            firebaseAuth.removeAuthStateListener(authStateListener);
        }
        instance = null;
    }
}