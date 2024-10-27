package com.example.pookies;

import android.content.ContentValues;
import android.content.Context;
import android.database.Cursor;
import android.database.sqlite.SQLiteDatabase;
import android.database.sqlite.SQLiteOpenHelper;

import java.util.ArrayList;
import java.util.List;

public class DBHelper extends SQLiteOpenHelper {

    private static final String DATABASE_NAME = "pookies.db";
    private static final int DATABASE_VERSION = 4;

    // User table
    private static final String TABLE_USERS = "users";
    private static final String COL_USER_ID = "id";
    private static final String COL_USER_EMAIL = "email";
    private static final String COL_USER_NAME = "name";
    private static final String COL_USER_PASSWORD = "password";
    private static final String COL_USER_PROFILEPICT = "profilepict";

    // Feedback table
    private static final String TABLE_FEEDBACK = "feedback";
    private static final String COL_FEEDBACK_ID = "id";
    private static final String COL_FEEDBACK_USER_ID = "userId";
    private static final String COL_FEEDBACK_USERNAME = "username";
    private static final String COL_FEEDBACK_EMAIL = "email";
    private static final String COL_FEEDBACK_TYPE = "feedbackType";
    private static final String COL_FEEDBACK_DESCRIPTION = "description";
    private static final String COL_FEEDBACK_TIME = "feedbackTime";
    // Message table
    private static final String TABLE_MESSAGES = "messages";
    private static final String COL_MESSAGE_ID = "id";
    private static final String COL_MESSAGE_CONTENT = "content";
    private static final String COL_MESSAGE_SENT_BY = "sent_by";
    private static final String COL_MESSAGE_USER_ID = "user_id";
    private static final String COL_MESSAGE_TIMESTAMP = "timestamp";

    public DBHelper(Context context) {
        super(context, DATABASE_NAME, null, DATABASE_VERSION);
    }

    @Override
    public void onCreate(SQLiteDatabase db) {
        // Create users table
        String CREATE_USERS_TABLE = "CREATE TABLE " + TABLE_USERS + "("
                + COL_USER_ID + " INTEGER PRIMARY KEY AUTOINCREMENT,"
                + COL_USER_EMAIL + " TEXT,"
                + COL_USER_NAME + " TEXT,"
                + COL_USER_PASSWORD + " TEXT,"
                + COL_USER_PROFILEPICT + " BLOB" + ")";
        db.execSQL(CREATE_USERS_TABLE);

        // Create feedback table
        String CREATE_FEEDBACK_TABLE = "CREATE TABLE " + TABLE_FEEDBACK + "("
                + COL_FEEDBACK_ID + " INTEGER PRIMARY KEY AUTOINCREMENT,"
                + COL_FEEDBACK_USER_ID + " TEXT,"
                + COL_FEEDBACK_USERNAME + " TEXT,"
                + COL_FEEDBACK_EMAIL + " TEXT,"
                + COL_FEEDBACK_TYPE + " TEXT,"
                + COL_FEEDBACK_DESCRIPTION + " TEXT,"
                + COL_FEEDBACK_TIME + " TEXT" + ")";
        db.execSQL(CREATE_FEEDBACK_TABLE);

        // Create messages table
        String CREATE_MESSAGES_TABLE = "CREATE TABLE " + TABLE_MESSAGES + "("
                + COL_MESSAGE_ID + " INTEGER PRIMARY KEY AUTOINCREMENT,"
                + COL_MESSAGE_CONTENT + " TEXT,"
                + COL_MESSAGE_SENT_BY + " TEXT,"
                + COL_MESSAGE_USER_ID + " TEXT,"
                + COL_MESSAGE_TIMESTAMP + " TIMESTAMP DEFAULT CURRENT_TIMESTAMP" + ")";
        db.execSQL(CREATE_MESSAGES_TABLE);
    }

    @Override
    public void onUpgrade(SQLiteDatabase db, int oldVersion, int newVersion) {
        if (oldVersion < 4) {
            // Add the profilepict column to the existing table
            db.execSQL("ALTER TABLE " + TABLE_USERS + " ADD COLUMN " + COL_USER_PROFILEPICT + " BLOB");
        }
        // Drop older tables if existed
        db.execSQL("DROP TABLE IF EXISTS " + TABLE_USERS);
        db.execSQL("DROP TABLE IF EXISTS " + TABLE_FEEDBACK);
        db.execSQL("DROP TABLE IF EXISTS " + TABLE_MESSAGES);
        onCreate(db);
    }

    // User-related methods

    public boolean insertUser(String email, String name, String password) {
        SQLiteDatabase db = this.getWritableDatabase();
        ContentValues contentValues = new ContentValues();
        contentValues.put(COL_USER_EMAIL, email);
        contentValues.put(COL_USER_NAME, name);
        contentValues.put(COL_USER_PASSWORD, password);
        long result = db.insert(TABLE_USERS, null, contentValues);
        return result != -1;
    }

    public User getUserByEmail(String email) {
        SQLiteDatabase db = this.getReadableDatabase();
        Cursor cursor = db.query(TABLE_USERS, null, COL_USER_EMAIL + "=?", new String[]{email}, null, null, null);
        if (cursor != null && cursor.moveToFirst()) {
            int id = cursor.getInt(cursor.getColumnIndexOrThrow(COL_USER_ID));
            String name = cursor.getString(cursor.getColumnIndexOrThrow(COL_USER_NAME));
            String password = cursor.getString(cursor.getColumnIndexOrThrow(COL_USER_PASSWORD));
            cursor.close();
            return new User(id, email, name, password);
        }
        return null;
    }

    public boolean updateUsername(String email, String newUsername) {
        SQLiteDatabase db = this.getWritableDatabase();
        ContentValues contentValues = new ContentValues();
        contentValues.put(COL_USER_NAME, newUsername);
        int result = db.update(TABLE_USERS, contentValues, COL_USER_EMAIL + "=?", new String[]{email});
        return result > 0;
    }

    // Add this method to update the user's email in the SQLite database
    public boolean updateUserEmail(String oldEmail, String newEmail) {
        SQLiteDatabase db = this.getWritableDatabase();
        ContentValues contentValues = new ContentValues();
        contentValues.put(COL_USER_EMAIL, newEmail);
        int result = db.update(TABLE_USERS, contentValues, COL_USER_EMAIL + "=?", new String[]{oldEmail});
        return result > 0;
    }


    public boolean updatePassword(String email, String newPassword) {
        SQLiteDatabase db = this.getWritableDatabase();
        ContentValues contentValues = new ContentValues();
        contentValues.put(COL_USER_PASSWORD, newPassword);
        int result = db.update(TABLE_USERS, contentValues, COL_USER_EMAIL + "=?", new String[]{email});
        return result > 0;
    }

    // Update user's profile picture
    public boolean updateProfilePicture(String email, byte[] profilePic) {
        SQLiteDatabase db = this.getWritableDatabase();
        ContentValues contentValues = new ContentValues();
        contentValues.put(COL_USER_PROFILEPICT, profilePic);
        int result = db.update(TABLE_USERS, contentValues, COL_USER_EMAIL + "=?", new String[]{email});
        return result > 0;
    }

    // Retrieve user's profile picture
    public byte[] getProfilePictureByEmail(String email) {
        SQLiteDatabase db = this.getReadableDatabase();
        Cursor cursor = db.query(TABLE_USERS, new String[]{COL_USER_PROFILEPICT},
                COL_USER_EMAIL + "=?", new String[]{email}, null, null, null);

        if (cursor != null && cursor.moveToFirst()) {
            byte[] profilePic = cursor.getBlob(cursor.getColumnIndexOrThrow(COL_USER_PROFILEPICT));
            cursor.close();
            return profilePic;
        }
        return null; // Return null if no picture found
    }

    // Feedback-related methods

    public long insertFeedback(Feedback feedback) {
        SQLiteDatabase db = this.getWritableDatabase();
        ContentValues values = new ContentValues();
        values.put(COL_FEEDBACK_USER_ID, feedback.getUserId());
        values.put(COL_FEEDBACK_USERNAME, feedback.getUsername());
        values.put(COL_FEEDBACK_EMAIL, feedback.getEmail());
        values.put(COL_FEEDBACK_TYPE, feedback.getFeedbackType());
        values.put(COL_FEEDBACK_DESCRIPTION, feedback.getDescription());
        values.put(COL_FEEDBACK_TIME, feedback.getFeedbackTime());
        return db.insert(TABLE_FEEDBACK, null, values);
    }

    public List<Feedback> getAllFeedback() {
        List<Feedback> feedbackList = new ArrayList<>();
        String selectQuery = "SELECT * FROM " + TABLE_FEEDBACK;
        SQLiteDatabase db = this.getReadableDatabase();
        Cursor cursor = db.rawQuery(selectQuery, null);
        if (cursor.moveToFirst()) {
            do {
                Feedback feedback = new Feedback();
                feedback.setUserId(cursor.getString(cursor.getColumnIndexOrThrow(COL_FEEDBACK_USER_ID)));
                feedback.setUsername(cursor.getString(cursor.getColumnIndexOrThrow(COL_FEEDBACK_USERNAME)));
                feedback.setEmail(cursor.getString(cursor.getColumnIndexOrThrow(COL_FEEDBACK_EMAIL)));
                feedback.setFeedbackType(cursor.getString(cursor.getColumnIndexOrThrow(COL_FEEDBACK_TYPE)));
                feedback.setDescription(cursor.getString(cursor.getColumnIndexOrThrow(COL_FEEDBACK_DESCRIPTION)));
                feedback.setFeedbackTime(cursor.getString(cursor.getColumnIndexOrThrow(COL_FEEDBACK_TIME)));
                feedbackList.add(feedback);
            } while (cursor.moveToNext());
        }
        cursor.close();
        return feedbackList;
    }

    public List<Feedback> getFeedbackByUserId(String userId) {
        List<Feedback> feedbackList = new ArrayList<>();
        SQLiteDatabase db = this.getReadableDatabase();
        Cursor cursor = db.query(TABLE_FEEDBACK, null, COL_FEEDBACK_USER_ID + "=?", new String[]{userId}, null, null, null);
        if (cursor.moveToFirst()) {
            do {
                Feedback feedback = new Feedback();
                feedback.setUserId(cursor.getString(cursor.getColumnIndexOrThrow(COL_FEEDBACK_USER_ID)));
                feedback.setUsername(cursor.getString(cursor.getColumnIndexOrThrow(COL_FEEDBACK_USERNAME)));
                feedback.setEmail(cursor.getString(cursor.getColumnIndexOrThrow(COL_FEEDBACK_EMAIL)));
                feedback.setFeedbackType(cursor.getString(cursor.getColumnIndexOrThrow(COL_FEEDBACK_TYPE)));
                feedback.setDescription(cursor.getString(cursor.getColumnIndexOrThrow(COL_FEEDBACK_DESCRIPTION)));
                feedback.setFeedbackTime(cursor.getString(cursor.getColumnIndexOrThrow(COL_FEEDBACK_TIME)));
                feedbackList.add(feedback);
            } while (cursor.moveToNext());
        }
        cursor.close();
        return feedbackList;
    }

    public int deleteFeedback(String userId, String feedbackTime) {
        SQLiteDatabase db = this.getWritableDatabase();
        return db.delete(TABLE_FEEDBACK, COL_FEEDBACK_USER_ID + "=? AND " + COL_FEEDBACK_TIME + "=?", new String[]{userId, feedbackTime});
    }
    public long insertMessage(Message message, String userId) {
        SQLiteDatabase db = this.getWritableDatabase();
        ContentValues values = new ContentValues();
        values.put(COL_MESSAGE_CONTENT, message.getMessage());
        values.put(COL_MESSAGE_SENT_BY, message.getSentBy());
        values.put(COL_MESSAGE_USER_ID, userId);
        return db.insert(TABLE_MESSAGES, null, values);
    }

    public List<Message> getMessages(String userId) {
        List<Message> messageList = new ArrayList<>();
        SQLiteDatabase db = this.getReadableDatabase();

        String selectQuery = "SELECT * FROM " + TABLE_MESSAGES +
                " WHERE " + COL_MESSAGE_USER_ID + " = ?" +
                " ORDER BY " + COL_MESSAGE_TIMESTAMP + " ASC";

        Cursor cursor = db.rawQuery(selectQuery, new String[]{userId});

        if (cursor.moveToFirst()) {
            do {
                String content = cursor.getString(cursor.getColumnIndexOrThrow(COL_MESSAGE_CONTENT));
                String sentBy = cursor.getString(cursor.getColumnIndexOrThrow(COL_MESSAGE_SENT_BY));
                Message message = new Message(content, sentBy);
                messageList.add(message);
            } while (cursor.moveToNext());
        }
        cursor.close();
        return messageList;
    }
    public boolean deleteAllMessages(String userId) {
        SQLiteDatabase db = this.getWritableDatabase();
        int result = db.delete(TABLE_MESSAGES, COL_MESSAGE_USER_ID + "=?", new String[]{userId});
        return result > 0;
    }

    public boolean deleteUser(String userId) {
        SQLiteDatabase db = this.getWritableDatabase();
        // First delete all messages
        db.delete(TABLE_MESSAGES, COL_MESSAGE_USER_ID + "=?", new String[]{userId});
        // Then delete the user
        int result = db.delete(TABLE_USERS, COL_USER_ID + "=?", new String[]{userId});
        return result > 0;
    }
}