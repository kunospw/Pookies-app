package com.example.pookies;

import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.util.Log;

import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseUser;
import com.google.firebase.database.DatabaseReference;
import com.google.firebase.database.FirebaseDatabase;

import java.util.concurrent.atomic.AtomicBoolean;
public class SessionManager {
    private static final String TAG = "SessionManager";
    private static final String PREF_NAME = "PookiesSession";
    private static final String KEY_IS_LOGGED_IN = "isLoggedIn";
    private static final String KEY_IS_REGISTERED = "isRegistered";

    private static final String KEY_USER_EMAIL = "userEmail";
    private static final String KEY_USER_NAME = "userName";
    private static final String KEY_USER_ID = "userId";
    private static final String KEY_LAST_SYNC = "lastSync";

    private static volatile SessionManager instance;
    private final SharedPreferences pref;
    private final SharedPreferences.Editor editor;
    private final Context context;
    private final FirebaseAuth firebaseAuth;
    private final DatabaseReference mDatabase;
    private final AtomicBoolean isSyncing;
    private FirebaseAuth.AuthStateListener authStateListener;

    private SessionManager(Context context) {
        this.context = context.getApplicationContext();
        this.pref = context.getSharedPreferences(PREF_NAME, Context.MODE_PRIVATE);
        this.editor = pref.edit();
        this.firebaseAuth = FirebaseAuth.getInstance();
        this.mDatabase = FirebaseDatabase.getInstance().getReference();
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
                if (isUserJustRegistered()) {
                    Log.d(TAG, "User is just registered, no session created");
                } else if (!isLoggedIn()) {
                    // Fetch the username from Firebase or set a default value
                    String userName = pref.getString(KEY_USER_NAME, "Default User");
                    createLoginSession(user.getEmail(), user.getUid(), userName);
                }
            } else {
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
                    // Sync local session with Firebase
                    updateLocalSessionFromFirebase(user);
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

    private void updateLocalSessionFromFirebase(FirebaseUser user) {
        mDatabase.child("user-info").child(user.getUid())
                .get().addOnCompleteListener(task -> {
                    if (task.isSuccessful() && task.getResult().exists()) {
                        String userName = task.getResult().child("name").getValue(String.class); // Get the username
                        createLoginSession(user.getEmail(), user.getUid(), userName != null ? userName : "Default User");
                    }
                });
    }

    public void createLoginSession(String email, String userId, String userName) {
        if (email == null || userId == null || userName == null) {
            Log.e(TAG, "Attempted to create login session with null values");
            return;
        }

        editor.putBoolean(KEY_IS_LOGGED_IN, true);
        editor.putString(KEY_USER_EMAIL, email);
        editor.putString(KEY_USER_ID, userId);
        editor.putString(KEY_USER_NAME, userName); // Save the username
        editor.putLong(KEY_LAST_SYNC, System.currentTimeMillis());
        editor.apply();
        Log.d(TAG, "Session created: Email=" + email + ", UserId=" + userId + ", UserName=" + userName);
    }


    public boolean isLoggedIn() {
        FirebaseUser currentUser = firebaseAuth.getCurrentUser();
        boolean sharedPrefLoggedIn = pref.getBoolean(KEY_IS_LOGGED_IN, false);

        // Check if both Firebase and SharedPreferences agree on login status
        return currentUser != null && sharedPrefLoggedIn;
    }
    public void markAsRegistered() {
        editor.putBoolean(KEY_IS_REGISTERED, true);
        editor.putBoolean(KEY_IS_LOGGED_IN, false); // Prevent immediate login
        editor.apply();
    }

    public boolean isUserJustRegistered() {
        return pref.getBoolean(KEY_IS_REGISTERED, false);
    }

    public void clearRegisteredFlag() {
        editor.putBoolean(KEY_IS_REGISTERED, false);
        editor.apply();
    }

    public String getUserName() {
        if (!isLoggedIn()) {
            return null;
        }
        return pref.getString(KEY_USER_NAME, null);
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

    public String getProfilePictureUri() {
        String userId = getUserId(); // Fetch the currently logged-in user's ID
        if (userId != null) {
            return pref.getString("PROFILE_PIC_URI_" + userId, null);
        }
        return null;
    }


    public void saveProfilePictureUri(String uri) {
        editor.putString("PROFILE_PIC_URI", uri);
        editor.apply();
    }

    public void clearProfilePictureUri() {
        editor.remove("PROFILE_PIC_URI");
        editor.apply();
    }



    public void clearSession() {
        editor.clear();
        editor.apply();
    }

    public void logoutUser() {
        try {
            // Update user's active status in Realtime Database
            String userId = getUserId();
            if (userId != null) {
                mDatabase.child("user-info").child(userId)
                        .child("active").setValue(false);
            }

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
}