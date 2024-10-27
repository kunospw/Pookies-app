package com.example.pookies;

public class Message {
    public static String SENT_BY_ME = "ME";
    public static String SENT_BY_BOT = "BOT";
    public String message;
    public String sentBy;
    public long timestamp;   // Added timestamp field

    // Default constructor for Firebase
    public Message() {
    }

    public Message(String message, String sentBy) {
        this.message = message;
        this.sentBy = sentBy;
        this.timestamp = System.currentTimeMillis();  // Set timestamp on creation
    }

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
}