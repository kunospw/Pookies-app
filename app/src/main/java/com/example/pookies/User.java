package com.example.pookies;

import com.google.firebase.database.IgnoreExtraProperties;

@IgnoreExtraProperties
public class User {
    private String uid;
    private String email;
    private String name;
    private String profilePicUrl;
    private long createdAt;
    private boolean isActive;

    // Required empty constructor for Firebase
    public User() {}

    public User(String uid, String email, String name) {
        this.uid = uid;
        this.email = email;
        this.name = name;
        this.createdAt = System.currentTimeMillis();
        this.isActive = true;
        this.profilePicUrl = ""; // Default empty
    }

    // Full constructor
    public User(String uid, String email, String name, String profilePicUrl) {
        this(uid, email, name);
        this.profilePicUrl = profilePicUrl;
    }

    // Getters and setters
    public String getUid() { return uid; }
    public void setUid(String uid) { this.uid = uid; }

    public String getEmail() { return email; }
    public void setEmail(String email) { this.email = email; }

    public String getName() { return name; }
    public void setName(String name) { this.name = name; }

    public String getProfilePicUrl() { return profilePicUrl; }
    public void setProfilePicUrl(String profilePicUrl) { this.profilePicUrl = profilePicUrl; }

    public long getCreatedAt() { return createdAt; }
    public void setCreatedAt(long createdAt) { this.createdAt = createdAt; }

    public boolean isActive() { return isActive; }
    public void setActive(boolean active) { isActive = active; }
}