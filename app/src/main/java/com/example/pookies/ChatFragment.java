package com.example.pookies;

import android.app.AlertDialog;
import android.content.ComponentName;
import android.content.Context;
import android.content.Intent;
import android.content.ServiceConnection;
import android.os.Bundle;
import android.os.IBinder;
import androidx.annotation.NonNull;
import androidx.fragment.app.Fragment;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;
import android.view.LayoutInflater;
import android.view.MenuItem;
import android.view.View;
import android.view.ViewGroup;
import android.widget.EditText;
import android.widget.ImageButton;
import android.widget.PopupMenu;
import android.widget.Toast;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseUser;
import com.google.firebase.database.DataSnapshot;
import com.google.firebase.database.DatabaseError;
import com.google.firebase.database.DatabaseReference;
import com.google.firebase.database.FirebaseDatabase;
import com.google.firebase.database.ValueEventListener;
import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;
import java.io.IOException;
import java.util.ArrayList;
import java.util.List;
import okhttp3.Call;
import okhttp3.Callback;
import okhttp3.MediaType;
import okhttp3.OkHttpClient;
import okhttp3.Request;
import okhttp3.RequestBody;
import okhttp3.Response;

public class ChatFragment extends Fragment implements MessagingService.MessageListener {
    RecyclerView recyclerView;
    EditText messageEditText;
    ImageButton sendButton;
    List<Message> messageList;
    MessageAdapter messageAdapter;
    FirebaseAuth mAuth;
    String userId;
    MessagingService messagingService;
    boolean isBound = false;
    public static final MediaType JSON = MediaType.get("application/json; charset=utf-8");
    OkHttpClient client = new OkHttpClient();
    DatabaseReference mDatabase;
    private ServiceConnection connection = new ServiceConnection() {
        @Override
        public void onServiceConnected(ComponentName className, IBinder service) {
            MessagingService.LocalBinder binder = (MessagingService.LocalBinder) service;
            messagingService = binder.getService();
            messagingService.addMessageListener(ChatFragment.this);
            isBound = true;
        }

        @Override
        public void onServiceDisconnected(ComponentName arg0) {
            isBound = false;
        }
    };

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        messageList = new ArrayList<>();
        mAuth = FirebaseAuth.getInstance();
        FirebaseUser user = mAuth.getCurrentUser();
        if (user != null) {
            userId = user.getUid();
            mDatabase = FirebaseDatabase.getInstance().getReference("users").child(userId).child("messages");
        }
        Intent intent = new Intent(getContext(), MessagingService.class);
        getActivity().bindService(intent, connection, Context.BIND_AUTO_CREATE);
    }
    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container,
                             Bundle savedInstanceState) {
        View view = inflater.inflate(R.layout.fragment_chat, container, false);
        recyclerView = view.findViewById(R.id.recycler_view);
        messageEditText = view.findViewById(R.id.message_edit_text);
        sendButton = view.findViewById(R.id.send_btn);

        // Create adapter with click listener
        messageAdapter = new MessageAdapter(messageList, new MessageAdapter.MessageClickListener() {
            @Override
            public void onMessageClick(View view, Message message, int position) {
                showMessageOptions(view, message, position);
            }
        });

        recyclerView.setAdapter(messageAdapter);
        LinearLayoutManager llm = new LinearLayoutManager(getContext());
        llm.setStackFromEnd(true);
        recyclerView.setLayoutManager(llm);

        sendButton.setOnClickListener((v) -> {
            String question = messageEditText.getText().toString().trim();
            addToChat(question, Message.SENT_BY_ME);
            messageEditText.setText("");
            callAPI(question);
        });

        loadChatHistory();

        return view;
    }
    private void showMessageOptions(View view, Message message, int position) {
        PopupMenu popup = new PopupMenu(getContext(), view);
        popup.getMenuInflater().inflate(R.menu.message_options_menu, popup.getMenu());

        popup.setOnMenuItemClickListener(item -> {
            int itemId = item.getItemId();
            if (itemId == R.id.edit_message) {
                showEditDialog(message);
                return true;
            } else if (itemId == R.id.delete_message) {
                showDeleteConfirmation(message, position);
                return true;
            }
            return false;
        });

        popup.show();
    }

    private void showEditDialog(Message message) {
        AlertDialog.Builder builder = new AlertDialog.Builder(getContext());
        final EditText editText = new EditText(getContext());
        editText.setText(message.getMessage());

        builder.setTitle("Edit Message")
                .setView(editText)
                .setPositiveButton("Save", (dialog, which) -> {
                    String newContent = editText.getText().toString().trim();
                    if (!newContent.isEmpty()) {
                        updateMessage(message, newContent);
                    }
                })
                .setNegativeButton("Cancel", null)
                .show();
    }

    private void showDeleteConfirmation(Message message, int position) {
        AlertDialog.Builder builder = new AlertDialog.Builder(getContext());
        builder.setTitle("Delete Message")
                .setMessage("This will delete this message and all subsequent messages. Continue?")
                .setPositiveButton("Delete", (dialog, which) -> {
                    deleteMessageAndSubsequent(message, position);
                })
                .setNegativeButton("Cancel", null)
                .show();
    }

    private void updateMessage(Message message, String newContent) {
        // Find and update the message in Firebase
        mDatabase.orderByChild("timestamp").equalTo(message.getTimestamp())
                .addListenerForSingleValueEvent(new ValueEventListener() {
                    @Override
                    public void onDataChange(@NonNull DataSnapshot dataSnapshot) {
                        for (DataSnapshot snapshot : dataSnapshot.getChildren()) {
                            snapshot.getRef().child("message").setValue(newContent)
                                    .addOnSuccessListener(aVoid -> {
                                        Toast.makeText(getContext(), "Message updated", Toast.LENGTH_SHORT).show();
                                    })
                                    .addOnFailureListener(e -> {
                                        Toast.makeText(getContext(), "Failed to update message", Toast.LENGTH_SHORT).show();
                                    });
                        }
                    }

                    @Override
                    public void onCancelled(@NonNull DatabaseError databaseError) {
                        Toast.makeText(getContext(), "Error: " + databaseError.getMessage(), Toast.LENGTH_SHORT).show();
                    }
                });
    }

    private void deleteMessageAndSubsequent(Message message, int position) {
        // Delete the message and all subsequent messages from Firebase
        mDatabase.orderByChild("timestamp")
                .startAt(message.getTimestamp())
                .addListenerForSingleValueEvent(new ValueEventListener() {
                    @Override
                    public void onDataChange(@NonNull DataSnapshot dataSnapshot) {
                        for (DataSnapshot snapshot : dataSnapshot.getChildren()) {
                            snapshot.getRef().removeValue();
                        }
                        Toast.makeText(getContext(), "Messages deleted", Toast.LENGTH_SHORT).show();
                    }

                    @Override
                    public void onCancelled(@NonNull DatabaseError databaseError) {
                        Toast.makeText(getContext(), "Error: " + databaseError.getMessage(), Toast.LENGTH_SHORT).show();
                    }
                });
    }

    @Override
    public void onDestroy() {
        super.onDestroy();
        if (isBound) {
            messagingService.removeMessageListener(this);
            getActivity().unbindService(connection);
            isBound = false;
        }
    }

    private void loadChatHistory() {
        messageList.clear();
        mDatabase.orderByChild("timestamp").addValueEventListener(new ValueEventListener() {
            @Override
            public void onDataChange(@NonNull DataSnapshot dataSnapshot) {
                messageList.clear();
                for (DataSnapshot messageSnapshot : dataSnapshot.getChildren()) {
                    Message message = messageSnapshot.getValue(Message.class);
                    if (message != null) {
                        messageList.add(message);
                    }
                }
                messageAdapter.notifyDataSetChanged();
                if (!messageList.isEmpty()) {
                    recyclerView.smoothScrollToPosition(messageList.size() - 1);
                }
            }

            @Override
            public void onCancelled(@NonNull DatabaseError databaseError) {
                Toast.makeText(getContext(), "Failed to load messages: " + databaseError.getMessage(),
                        Toast.LENGTH_SHORT).show();
            }
        });
    }
    void addToChat(String message, String sentBy) {
        if (getActivity() != null) {
            getActivity().runOnUiThread(() -> {
                Message newMessage = new Message(message, sentBy);

                // Save message to Firebase
                String messageId = mDatabase.push().getKey();
                if (messageId != null) {
                    mDatabase.child(messageId).setValue(newMessage)
                            .addOnSuccessListener(aVoid -> {
                                messageEditText.setText("");
                                recyclerView.smoothScrollToPosition(messageList.size());
                            })
                            .addOnFailureListener(e -> {
                                Toast.makeText(getContext(), "Failed to send message: " + e.getMessage(),
                                        Toast.LENGTH_SHORT).show();
                            });
                }

                // Send message through MessagingService
                if (isBound) {
                    messagingService.sendMessage(newMessage);
                }
            });
        }
    }

    void addResponse(String response) {
        addToChat(response, Message.SENT_BY_BOT);
    }

    void callAPI(String question) {
        JSONObject jsonBody = new JSONObject();
        try {
            JSONObject systemMessage = new JSONObject()
                    .put("role", "system")
                    .put("content", "You are a friendly buddy called Pookies. Your main job is to accompany, support, and give positive feedback to the user. " +
                            "Here are your constraints: 1. Refrain from using any bad words. 2. Avoid talking or engaging in any negative or inappropriate topics, including drugs, explicit content, or NSFW discussions. " +
                            "3. Keep the user in high spirits and maintain a good mood. 4. Keep the chat casual and fun, adjusting to the latest internet trends and slang. Don't be overly descriptive and sounds unnatural like a bot. Behave humanly as much as possible. " +
                            "5. Do not be overly excited at the start of the conversation.");

            JSONObject userMessage = new JSONObject()
                    .put("role", "user")
                    .put("content", question);

            JSONArray messages = new JSONArray()
                    .put(systemMessage)
                    .put(userMessage);

            jsonBody.put("model", "llama3-groq-70b-8192-tool-use-preview");
            jsonBody.put("messages", messages);
            jsonBody.put("max_tokens", 4000);
            jsonBody.put("temperature", 0.5);
        } catch (JSONException e) {
            e.printStackTrace();
        }
        RequestBody body = RequestBody.create(jsonBody.toString(), JSON);
        Request request = new Request.Builder()
                .url("https://api.groq.com/openai/v1/chat/completions")
                .header("Authorization", "Bearer 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")
                .post(body)
                .build();

        client.newCall(request).enqueue(new Callback() {
            @Override
            public void onFailure(@NonNull Call call, @NonNull IOException e) {
                addResponse("Failed to load response due to " + e.getMessage());
            }

            @Override
            public void onResponse(@NonNull Call call, @NonNull Response response) throws IOException {
                if (response.isSuccessful()) {
                    JSONObject jsonObject;
                    try {
                        jsonObject = new JSONObject(response.body().string());
                        JSONArray jsonArray = jsonObject.getJSONArray("choices");
                        JSONObject firstChoice = jsonArray.getJSONObject(0);
                        JSONObject message = firstChoice.getJSONObject("message");
                        String result = message.getString("content");
                        addResponse(result.trim());
                    } catch (JSONException e) {
                        throw new RuntimeException(e);
                    }
                } else {
                    addResponse("Failed to load response due to " + response.body().string());
                }
            }
        });
    }

    @Override
    public void onNewMessage(Message message) {
        if (getActivity() != null) {
            getActivity().runOnUiThread(() -> {
                messageList.add(message);
                messageAdapter.notifyItemInserted(messageList.size() - 1);
                recyclerView.smoothScrollToPosition(messageList.size() - 1);
            });
        }
    }
}