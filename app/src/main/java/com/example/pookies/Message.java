package com.example.pookies;

public class Message {
    public static String SENT_BY_ME = "ME";
    public static String SENT_BY_BOT = "BOT";
    private String message;
    private String sentBy;
    private long timestamp;   // Timestamp field
    private String imageUrl;  // Image URL field
    private String caption;   // Caption field

    // Default constructor for Firebase
    public Message() {
    }

    // Constructor for text-only messages
    public Message(String message, String sentBy) {
        this.message = message;
        this.sentBy = sentBy;
        this.timestamp = System.currentTimeMillis();  // Set timestamp
    }

    // Getters and setters
    public String getMessage() {
        return message;
    }

    public void setMessage(String message) {
        this.message = message;
    }

    public String getSentBy() {
        return sentBy;
    }

    public void setSentBy(String sentBy) {
        this.sentBy = sentBy;
    }

    public long getTimestamp() {
        return timestamp;
    }

    public void setTimestamp(long timestamp) {
        this.timestamp = timestamp;
    }

    public String getImageUrl() {
        return imageUrl;
    }

    public void setImageUrl(String imageUrl) {
        this.imageUrl = imageUrl;
    }

    public String getCaption() {
        return caption;
    }

    public void setCaption(String caption) {
        this.caption = caption;
    }
}
