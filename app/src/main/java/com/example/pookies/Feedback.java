package com.example.pookies;

// Feedback.java
public class Feedback {
    private String userId;
    private String username;
    private String email;
    private String feedbackType;
    private String description;
    private String feedbackTime;

    public Feedback(String userId, String username, String email, String feedbackType,
                    String description, String feedbackTime) {
        this.userId = userId;
        this.username = username;
        this.email = email;
        this.feedbackType = feedbackType;
        this.description = description;
        this.feedbackTime = feedbackTime;
    }

    // Getters
    public String getUserId() { return userId; }
    public String getUsername() { return username; }
    public String getEmail() { return email; }
    public String getFeedbackType() { return feedbackType; }
    public String getDescription() { return description; }
    public String getFeedbackTime() { return feedbackTime; }
}

