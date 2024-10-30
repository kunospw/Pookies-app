package com.example.pookies;

import android.content.ContentValues;
import android.content.Context;
import android.database.Cursor;
import android.database.sqlite.SQLiteDatabase;
import android.database.sqlite.SQLiteOpenHelper;
import android.util.Log;

import java.util.ArrayList;
import java.util.List;

public class DBHelper extends SQLiteOpenHelper {
    private static final String DATABASE_NAME = "PookiesDB";
    private static final int DATABASE_VERSION = 3;

    // User table
    public static final String COLUMN_USER_ID = "user_id";
    public static final String TABLE_USERS = "users";
    public static final String COLUMN_EMAIL = "email";
    public static final String COLUMN_NAME = "name";
    public static final String COLUMN_PASSWORD = "password";
    public static final String COLUMN_RESET_TOKEN = "reset_token";
    public static final String COLUMN_RESET_TOKEN_EXPIRY = "reset_token_expiry";
    public static final String COLUMN_PROFILE_PIC_PATH = "profile_pic_path";

    // Messages table
    public static final String TABLE_MESSAGES = "messages";
    public static final String COLUMN_ID = "id";
    public static final String COLUMN_MESSAGE = "message";
    public static final String COLUMN_SENT_BY = "sent_by";
    public static final String COLUMN_TIMESTAMP = "timestamp";
    // Feedback table
    public static final String TABLE_FEEDBACK = "feedback";
    public static final String COLUMN_FEEDBACK_TYPE = "feedback_type";
    public static final String COLUMN_DESCRIPTION = "description";
    public static final String COLUMN_FEEDBACK_TIME = "feedback_time";

    // Create users table query
    private static final String CREATE_USER_TABLE =
            "CREATE TABLE " + TABLE_USERS + "(" +
                    COLUMN_USER_ID + " TEXT PRIMARY KEY, " +
                    COLUMN_EMAIL + " TEXT UNIQUE, " +
                    COLUMN_NAME + " TEXT, " +
                    COLUMN_PASSWORD + " TEXT, " +
                    COLUMN_RESET_TOKEN + " TEXT, " +
                    COLUMN_RESET_TOKEN_EXPIRY + " INTEGER, " +
                    COLUMN_PROFILE_PIC_PATH + " TEXT" +
                    ")";

    // Create messages table query
    private static final String CREATE_MESSAGES_TABLE =
            "CREATE TABLE " + TABLE_MESSAGES + "(" +
                    COLUMN_ID + " INTEGER PRIMARY KEY AUTOINCREMENT, " +
                    COLUMN_MESSAGE + " TEXT, " +
                    COLUMN_SENT_BY + " TEXT, " +
                    COLUMN_TIMESTAMP + " INTEGER, " +
                    COLUMN_USER_ID + " TEXT, " +
                    "FOREIGN KEY(" + COLUMN_USER_ID + ") REFERENCES " +
                    TABLE_USERS + "(" + COLUMN_USER_ID + ")" +
                    ")";

    // Create Feedback table query
    private static final String CREATE_FEEDBACK_TABLE =
            "CREATE TABLE " + TABLE_FEEDBACK + "(" +
                    COLUMN_ID + " INTEGER PRIMARY KEY AUTOINCREMENT, " +
                    COLUMN_USER_ID + " TEXT, " +
                    COLUMN_NAME + " TEXT, " +
                    COLUMN_EMAIL + " TEXT, " +
                    COLUMN_FEEDBACK_TYPE + " TEXT, " +
                    COLUMN_DESCRIPTION + " TEXT, " +
                    COLUMN_FEEDBACK_TIME + " TEXT" +
                    ")";



    public DBHelper(Context context) {
        super(context, DATABASE_NAME, null, DATABASE_VERSION);
    }

    @Override
    public void onCreate(SQLiteDatabase db) {
        db.execSQL(CREATE_USER_TABLE);
        db.execSQL(CREATE_MESSAGES_TABLE);
        db.execSQL(CREATE_FEEDBACK_TABLE);
    }

    @Override
    public void onUpgrade(SQLiteDatabase db, int oldVersion, int newVersion) {
        db.execSQL("DROP TABLE IF EXISTS " + TABLE_MESSAGES);
        db.execSQL("DROP TABLE IF EXISTS " + TABLE_USERS);
        db.execSQL("DROP TABLE IF EXISTS " + TABLE_FEEDBACK);
        onCreate(db);
    }

    // User related methods
    public boolean insertUser(String email, String name, String password) {
        SQLiteDatabase db = this.getWritableDatabase();
        ContentValues values = new ContentValues();

        String userId = generateUserId(); // Generate a unique user ID

        values.put(COLUMN_USER_ID, userId);
        values.put(COLUMN_EMAIL, email);
        values.put(COLUMN_NAME, name);
        values.put(COLUMN_PASSWORD, password);

        long result = db.insert(TABLE_USERS, null, values);
        return result != -1;
    }

    private String generateUserId() {
        return "USER_" + System.currentTimeMillis() + "_" + Math.random();
    }

    public User getUserByEmail(String email) {
        SQLiteDatabase db = this.getReadableDatabase();

        Cursor cursor = db.query(TABLE_USERS,
                new String[]{COLUMN_USER_ID, COLUMN_EMAIL, COLUMN_NAME, COLUMN_PASSWORD,
                        COLUMN_PROFILE_PIC_PATH, COLUMN_RESET_TOKEN, COLUMN_RESET_TOKEN_EXPIRY},
                COLUMN_EMAIL + "=?",
                new String[]{email},
                null, null, null);

        if (cursor != null && cursor.moveToFirst()) {
            User user = new User(
                    cursor.getString(cursor.getColumnIndex(COLUMN_USER_ID)),
                    cursor.getString(cursor.getColumnIndex(COLUMN_EMAIL)),
                    cursor.getString(cursor.getColumnIndex(COLUMN_NAME)),
                    cursor.getString(cursor.getColumnIndex(COLUMN_PASSWORD))
            );
            user.setProfilePicturePath(cursor.getString(cursor.getColumnIndex(COLUMN_PROFILE_PIC_PATH)));
            user.setResetToken(cursor.getString(cursor.getColumnIndex(COLUMN_RESET_TOKEN)));
            user.setResetTokenExpiry(cursor.getLong(cursor.getColumnIndex(COLUMN_RESET_TOKEN_EXPIRY)));
            cursor.close();
            return user;
        }
        if (cursor != null) {
            cursor.close();
        }
        return null;
    }
    public User getUserById(String userId) {
        SQLiteDatabase db = this.getReadableDatabase();

        Cursor cursor = db.query(TABLE_USERS,
                new String[]{COLUMN_USER_ID, COLUMN_EMAIL, COLUMN_NAME, COLUMN_PASSWORD,
                        COLUMN_PROFILE_PIC_PATH, COLUMN_RESET_TOKEN, COLUMN_RESET_TOKEN_EXPIRY},
                COLUMN_USER_ID + "=?",
                new String[]{userId},
                null, null, null);

        if (cursor != null && cursor.moveToFirst()) {
            User user = new User(
                    cursor.getString(cursor.getColumnIndex(COLUMN_USER_ID)),
                    cursor.getString(cursor.getColumnIndex(COLUMN_EMAIL)),
                    cursor.getString(cursor.getColumnIndex(COLUMN_NAME)),
                    cursor.getString(cursor.getColumnIndex(COLUMN_PASSWORD))
            );
            user.setProfilePicturePath(cursor.getString(cursor.getColumnIndex(COLUMN_PROFILE_PIC_PATH)));
            user.setResetToken(cursor.getString(cursor.getColumnIndex(COLUMN_RESET_TOKEN)));
            user.setResetTokenExpiry(cursor.getLong(cursor.getColumnIndex(COLUMN_RESET_TOKEN_EXPIRY)));
            cursor.close();
            return user;
        }
        if (cursor != null) {
            cursor.close();
        }
        return null;
    }
    public boolean updateUsername(String email, String newUsername) {
        SQLiteDatabase db = this.getWritableDatabase();
        ContentValues values = new ContentValues();
        values.put("name", newUsername);

        int result = db.update("users", values, "email = ?", new String[]{email});
        return result > 0;
    }

    public boolean updateEmail(String oldEmail, String newEmail) {
        SQLiteDatabase db = this.getWritableDatabase();
        ContentValues values = new ContentValues();
        values.put("email", newEmail);

        int result = db.update("users", values, "email = ?", new String[]{oldEmail});
        return result > 0;
    }


    // Message related methods
    public boolean insertMessage(Message message, String userId) {
        SQLiteDatabase db = this.getWritableDatabase();
        ContentValues values = new ContentValues();

        try {
            values.put(COLUMN_MESSAGE, message.getMessage());
            values.put(COLUMN_SENT_BY, message.getSentBy());
            values.put(COLUMN_TIMESTAMP, System.currentTimeMillis());
            values.put(COLUMN_USER_ID, userId);

            long result = db.insert(TABLE_MESSAGES, null, values);
            Log.d("DBHelper", "Message inserted for user " + userId + ": " + (result != -1));
            return result != -1;
        } catch (Exception e) {
            Log.e("DBHelper", "Error inserting message: " + e.getMessage());
            return false;
        }
    }

    // Existing methods...
    public boolean updatePassword(String email, String newPassword) {
        SQLiteDatabase db = this.getWritableDatabase();
        ContentValues values = new ContentValues();
        values.put(COLUMN_PASSWORD, newPassword);

        int rowsAffected = db.update(TABLE_USERS, values,
                COLUMN_EMAIL + "=?", new String[]{email});
        return rowsAffected > 0;
    }

    public boolean storeResetToken(String email, String token, long expiryTime) {
        SQLiteDatabase db = this.getWritableDatabase();
        ContentValues values = new ContentValues();
        values.put(COLUMN_RESET_TOKEN, token);
        values.put(COLUMN_RESET_TOKEN_EXPIRY, expiryTime);

        int rowsAffected = db.update(TABLE_USERS, values,
                COLUMN_EMAIL + "=?", new String[]{email});
        return rowsAffected > 0;
    }

    public boolean verifyResetToken(String email, String token) {
        SQLiteDatabase db = this.getReadableDatabase();
        Cursor cursor = db.query(TABLE_USERS,
                new String[]{COLUMN_RESET_TOKEN, COLUMN_RESET_TOKEN_EXPIRY},
                COLUMN_EMAIL + "=?",
                new String[]{email},
                null, null, null);

        if (cursor != null && cursor.moveToFirst()) {
            String storedToken = cursor.getString(cursor.getColumnIndex(COLUMN_RESET_TOKEN));
            long expiryTime = cursor.getLong(cursor.getColumnIndex(COLUMN_RESET_TOKEN_EXPIRY));
            cursor.close();

            return token.equals(storedToken) && System.currentTimeMillis() < expiryTime;
        }
        if (cursor != null) {
            cursor.close();
        }
        return false;
    }

    public boolean updateProfilePicturePath(String email, String profilePicturePath) {
        SQLiteDatabase db = this.getWritableDatabase();
        ContentValues values = new ContentValues();
        values.put(COLUMN_PROFILE_PIC_PATH, profilePicturePath);  // Use the constant

        int result = db.update(TABLE_USERS, values, COLUMN_EMAIL + " = ?", new String[]{email});
        return result > 0;
    }

    public boolean deleteUser(String userId) {
        SQLiteDatabase db = this.getWritableDatabase();
        db.beginTransaction();
        try {
            // First delete all messages associated with the user
            db.delete(TABLE_MESSAGES,
                    COLUMN_USER_ID + "=?",
                    new String[]{userId});

            // Then delete the user
            int result = db.delete(TABLE_USERS,
                    COLUMN_USER_ID + "=?",
                    new String[]{userId});

            db.setTransactionSuccessful();
            return result > 0;
        } catch (Exception e) {
            Log.e("DBHelper", "Error deleting user: " + e.getMessage());
            return false;
        } finally {
            db.endTransaction();
        }
    }

    public boolean doesUserExist(String userId) {
        SQLiteDatabase db = this.getReadableDatabase();
        Cursor cursor = db.query(TABLE_USERS,
                new String[]{COLUMN_USER_ID},
                COLUMN_USER_ID + "=?",
                new String[]{userId},
                null, null, null);

        boolean exists = cursor != null && cursor.getCount() > 0;
        if (cursor != null) {
            cursor.close();
        }
        return exists;
    }

    public List<Message> getMessages(String userId) {
        List<Message> messages = new ArrayList<>();
        SQLiteDatabase db = this.getReadableDatabase();

        // Add logging to debug
        Log.d("DBHelper", "Fetching messages for user: " + userId);

        String selection = COLUMN_USER_ID + "=?";
        String[] selectionArgs = {userId};

        Cursor cursor = null;
        try {
            cursor = db.query(TABLE_MESSAGES,
                    new String[]{COLUMN_ID, COLUMN_MESSAGE, COLUMN_SENT_BY, COLUMN_TIMESTAMP, COLUMN_USER_ID},
                    selection,
                    selectionArgs,
                    null,
                    null,
                    COLUMN_TIMESTAMP + " ASC");

            Log.d("DBHelper", "Found " + (cursor != null ? cursor.getCount() : 0) + " messages");

            if (cursor != null && cursor.moveToFirst()) {
                do {
                    Message message = new Message(
                            cursor.getLong(cursor.getColumnIndex(COLUMN_ID)),
                            cursor.getString(cursor.getColumnIndex(COLUMN_MESSAGE)),
                            cursor.getString(cursor.getColumnIndex(COLUMN_SENT_BY)),
                            cursor.getLong(cursor.getColumnIndex(COLUMN_TIMESTAMP)),
                            cursor.getString(cursor.getColumnIndex(COLUMN_USER_ID))
                    );
                    messages.add(message);
                } while (cursor.moveToNext());
            }
        } catch (Exception e) {
            Log.e("DBHelper", "Error fetching messages: " + e.getMessage());
        } finally {
            if (cursor != null && !cursor.isClosed()) {
                cursor.close();
            }
        }

        return messages;
    }
    public boolean deleteAllMessages(String userId) {
        SQLiteDatabase db = this.getWritableDatabase();
        try {
            int result = db.delete(TABLE_MESSAGES,
                    COLUMN_USER_ID + "=?",
                    new String[]{userId});
            return result > 0;
        } catch (Exception e) {
            Log.e("DBHelper", "Error deleting messages: " + e.getMessage());
            return false;
        }
    }
}