package com.example.pookies;

public class Message {
    public static String SENT_BY_ME = "me";
    public static String SENT_BY_BOT = "bot";

    private long id;
    private String message;
    private String sentBy;
    private long timestamp;
    private String userId;

    public Message(String message, String sentBy) {
        this.message = message;
        this.sentBy = sentBy;
        this.timestamp = System.currentTimeMillis();
    }

    public Message(long id, String message, String sentBy, long timestamp, String userId) {
        this.id = id;
        this.message = message;
        this.sentBy = sentBy;
        this.timestamp = timestamp;
        this.userId = userId;
    }

    public long getId() {
        return id;
    }

    public void setId(long id) {
        this.id = id;
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

    public String getUserId() {
        return userId;
    }

    public void setUserId(String userId) {
        this.userId = userId;
    }
}