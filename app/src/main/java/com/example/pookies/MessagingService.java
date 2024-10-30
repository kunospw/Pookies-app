package com.example.pookies;

import android.app.Service;
import android.content.Intent;
import android.os.Binder;
import android.os.IBinder;
import android.os.Handler;
import android.os.Looper;
import android.util.Log;

import androidx.annotation.Nullable;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

public class MessagingService extends Service {
    private final IBinder binder = new LocalBinder();
    private final List<MessageListener> listeners = new ArrayList<>();
    private DBHelper dbHelper;
    private String currentUserId;
    private final ExecutorService executorService = Executors.newSingleThreadExecutor();
    private final Handler mainHandler = new Handler(Looper.getMainLooper());

    public class LocalBinder extends Binder {
        MessagingService getService() {
            return MessagingService.this;
        }
    }

    @Override
    public void onCreate() {
        super.onCreate();
        dbHelper = new DBHelper(this);
        // Get current user ID from SharedPreferences
        currentUserId = getSharedPreferences("UserPrefs", MODE_PRIVATE)
                .getString("user_id", null);

        if (currentUserId == null) {
            Log.e("MessagingService", "No user ID found in SharedPreferences");
        }
    }

    @Nullable
    @Override
    public IBinder onBind(Intent intent) {
        return binder;
    }

    public void sendMessage(Message message) {
        if (currentUserId == null) {
            Log.e("MessagingService", "Cannot send message: No user ID");
            return;
        }

        executorService.execute(() -> {
            // Save message to database
            message.setUserId(currentUserId);
            boolean success = dbHelper.insertMessage(message, currentUserId);

            if (success) {
                // Notify listeners on main thread
                mainHandler.post(() -> {
                    for (MessageListener listener : listeners) {
                        listener.onNewMessage(message);
                    }
                });
            } else {
                Log.e("MessagingService", "Failed to save message to database");
            }
        });
    }

    public void loadMessages() {
        if (currentUserId == null) {
            Log.e("MessagingService", "Cannot load messages: No user ID");
            return;
        }

        executorService.execute(() -> {
            List<Message> messages = dbHelper.getMessages(currentUserId);
            mainHandler.post(() -> {
                for (MessageListener listener : listeners) {
                    for (Message message : messages) {
                        listener.onNewMessage(message);
                    }
                }
            });
        });
    }

    public void deleteAllMessages() {
        if (currentUserId == null) {
            Log.e("MessagingService", "Cannot delete messages: No user ID");
            return;
        }

        executorService.execute(() -> {
            boolean success = dbHelper.deleteAllMessages(currentUserId);
            if (!success) {
                Log.e("MessagingService", "Failed to delete messages");
            }
        });
    }

    public void setCurrentUserId(String userId) {
        this.currentUserId = userId;
        getSharedPreferences("UserPrefs", MODE_PRIVATE)
                .edit()
                .putString("user_id", userId)
                .apply();
    }

    public void addMessageListener(MessageListener listener) {
        listeners.add(listener);
    }

    public void removeMessageListener(MessageListener listener) {
        listeners.remove(listener);
    }

    @Override
    public void onDestroy() {
        super.onDestroy();
        executorService.shutdown();
    }

    public interface MessageListener {
        void onNewMessage(Message message);
    }
}