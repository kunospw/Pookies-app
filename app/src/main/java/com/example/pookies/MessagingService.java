package com.example.pookies;

import android.app.Service;
import android.content.Intent;
import android.os.Binder;
import android.os.Handler;
import android.os.IBinder;
import android.os.Looper;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseUser;
import com.google.firebase.database.*;

import java.lang.ref.WeakReference;
import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

public class MessagingService extends Service {
    private final Map<String, WeakReference<MessageListener>> listenerMap = new ConcurrentHashMap<>();
    private DatabaseReference mDatabase;
    private FirebaseAuth mAuth;

    public class LocalBinder extends Binder {
        MessagingService getService() {
            return MessagingService.this;
        }
    }

    @Override
    public void onCreate() {
        super.onCreate();
        mAuth = FirebaseAuth.getInstance();
        FirebaseUser currentUser = mAuth.getCurrentUser();
        if (currentUser != null) {
            String userId = currentUser.getUid();
            mDatabase = FirebaseDatabase.getInstance().getReference("users").child(userId).child("messages");
            listenForNewMessages();
        }
    }

    private void listenForNewMessages() {
        mDatabase.addChildEventListener(new ChildEventListener() {
            @Override
            public void onChildAdded(@NonNull DataSnapshot dataSnapshot, @Nullable String previousChildName) {
                Message message = dataSnapshot.getValue(Message.class);
                if (message != null) {
                    notifyListeners(message);
                }
            }

            @Override
            public void onChildChanged(@NonNull DataSnapshot snapshot, @Nullable String previousChildName) {}

            @Override
            public void onChildRemoved(@NonNull DataSnapshot snapshot) {}

            @Override
            public void onChildMoved(@NonNull DataSnapshot snapshot, @Nullable String previousChildName) {}

            @Override
            public void onCancelled(@NonNull DatabaseError error) {}
        });
    }

    public void addMessageListener(MessageListener listener) {
        if (listener != null) {
            String key = listener.toString(); // Use object's toString as unique identifier
            listenerMap.put(key, new WeakReference<>(listener));
        }
    }

    public void removeMessageListener(MessageListener listener) {
        if (listener != null) {
            String key = listener.toString();
            listenerMap.remove(key);
        }
    }

    private void notifyListeners(Message message) {
        // Create a new ArrayList to avoid ConcurrentModificationException
        List<String> deadReferences = new ArrayList<>();

        for (Map.Entry<String, WeakReference<MessageListener>> entry : listenerMap.entrySet()) {
            WeakReference<MessageListener> weakRef = entry.getValue();
            MessageListener listener = weakRef.get();

            if (listener == null) {
                // Mark this reference for removal
                deadReferences.add(entry.getKey());
            } else {
                // Notify the listener on the main thread
                new Handler(Looper.getMainLooper()).post(() ->
                        listener.onNewMessage(message)
                );
            }
        }

        // Remove dead references
        for (String key : deadReferences) {
            listenerMap.remove(key);
        }
    }

    public void sendMessage(Message message) {
        if (mDatabase != null) {
            String messageId = mDatabase.push().getKey();
            if (messageId != null) {
                mDatabase.child(messageId).setValue(message);
            }
        }
    }

    @Nullable
    @Override
    public IBinder onBind(Intent intent) {
        return new LocalBinder();
    }

    public interface MessageListener {
        void onNewMessage(Message message);
    }
}