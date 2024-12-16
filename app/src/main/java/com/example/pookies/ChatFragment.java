package com.example.pookies;

import android.app.AlertDialog;
import android.content.Context;
import android.os.Bundle;
import androidx.annotation.NonNull;
import androidx.fragment.app.Fragment;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.EditText;
import android.widget.ImageButton;
import android.widget.PopupMenu;
import android.widget.Toast;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseUser ;
import com.google.firebase.database.DataSnapshot;
import com.google.firebase.database.DatabaseError;
import com.google.firebase.database.DatabaseException;
import com.google.firebase.database.DatabaseReference;
import com.google.firebase.database.FirebaseDatabase;
import com.google.firebase.database.ValueEventListener;
import com.google.firebase.storage.FirebaseStorage;
import com.google.firebase.storage.StorageReference;
import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;
import java.io.IOException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import okhttp3.Call;
import okhttp3.Callback;
import okhttp3.MediaType;
import okhttp3.OkHttpClient;
import okhttp3.Request;
import okhttp3.RequestBody;
import okhttp3.Response;

public class ChatFragment extends Fragment {
    RecyclerView recyclerView;
    EditText messageEditText;
    ImageButton sendButton;
    List<Message> messageList;
    MessageAdapter messageAdapter;
    FirebaseAuth mAuth;
    String userId;
    public static final MediaType JSON = MediaType.get("application/json; charset=utf-8");
    OkHttpClient client = new OkHttpClient();
    DatabaseReference mDatabase;
    String userProfileUri;
    private String userName = "User"; // Default value if name isn't fetched
    private List<String> chatContext = new ArrayList<>();

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        messageList = new ArrayList<>();
        chatContext = new ArrayList<>();
        mAuth = FirebaseAuth.getInstance();
        FirebaseUser  user = mAuth.getCurrentUser ();

        if (user != null) {
            userId = user.getUid();
            mDatabase = FirebaseDatabase.getInstance().getReference("users").child(userId).child("messages");
            fetchChatContext();
            fetchUserName();
            // Fetch the profile picture dynamically
            fetchUserProfilePicture();
        }
    }

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {
        View view = inflater.inflate(R.layout.fragment_chat, container, false);
        recyclerView = view.findViewById(R.id.recycler_view);
        messageEditText = view.findViewById(R.id.message_edit_text);
        sendButton = view.findViewById(R.id.send_btn);

        // Initialize adapter
        messageAdapter = new MessageAdapter(getContext(), messageList, (view1, message, position) -> {
            showMessageOptions(view1, message, position);
        }, userProfileUri); // Pass the userProfileUri

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
    private void fetchUserName() {
        DatabaseReference userNameRef = FirebaseDatabase.getInstance().getReference("users").child(userId).child("info").child("name");

        userNameRef.addListenerForSingleValueEvent(new ValueEventListener() {
            @Override
            public void onDataChange(@NonNull DataSnapshot snapshot) {
                if (snapshot.exists()) {
                    userName = snapshot.getValue(String.class);
                    if (userName == null || userName.isEmpty()) {
                        userName = "User"; // Fallback default
                    }
                    Log.d("ChatFragment", "Fetched username: " + userName);
                } else {
                    userName = "User"; // Fallback default
                    Log.d("ChatFragment", "No username found, defaulting to: " + userName);
                }
            }

            @Override
            public void onCancelled(@NonNull DatabaseError error) {
                Toast.makeText(getContext(), "Failed to fetch user name: " + error.getMessage(), Toast.LENGTH_SHORT).show();
                userName = "User"; // Fallback default
            }
        });
    }



    private void fetchChatContext() {
        DatabaseReference chatContextRef = FirebaseDatabase.getInstance()
                .getReference("users").child(userId).child("chatContext");

        chatContextRef.addListenerForSingleValueEvent(new ValueEventListener() {
            @Override
            public void onDataChange(@NonNull DataSnapshot snapshot) {
                List<Message> messageList = new ArrayList<>();  // List to hold messages

                for (DataSnapshot childSnapshot : snapshot.getChildren()) {
                    Message message = childSnapshot.getValue(Message.class);

                    if (message != null) {
                        messageList.add(message);  // Add to list
                        Log.d("ChatFragment", "Message: " + message.getMessage() + ", Sent By: " + message.getSentBy());
                    }
                }

            }

            @Override
            public void onCancelled(@NonNull DatabaseError error) {
                Log.e("ChatFragment", "Error fetching messages: " + error.getMessage());
            }
        });
    }

    private void saveChatContext(String messageText, String sentBy) {
        DatabaseReference chatContextRef = FirebaseDatabase.getInstance()
                .getReference("users").child(userId).child("chatContext");

        // Create a new Message instance
        Message newMessage = new Message(messageText, sentBy);

        // Push the message to Firebase (avoids overwriting)
        chatContextRef.push().setValue(newMessage)
                .addOnSuccessListener(aVoid -> Log.d("ChatFragment", "Message saved successfully"))
                .addOnFailureListener(e -> Log.e("ChatFragment", "Failed to save message: " + e.getMessage()));
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

    private void fetchUserProfilePicture() {
        DatabaseReference userInfoRef = FirebaseDatabase.getInstance().getReference("user-info").child(userId);

        userInfoRef.addListenerForSingleValueEvent(new ValueEventListener() {
            @Override
            public void onDataChange(@NonNull DataSnapshot snapshot) {
                if (snapshot.exists() && snapshot.hasChild("profilePicUrl")) {
                    String profilePicUrl = snapshot.child("profilePicUrl").getValue(String.class);
                    if (profilePicUrl != null && !profilePicUrl.isEmpty()) {
                        userProfileUri = profilePicUrl;
                        initializeAdapter();
                    } else {
                        fetchProfilePictureFromStorage();
                    }
                } else {
                    fetchProfilePictureFromStorage();
                }
            }

            @Override
            public void onCancelled(@NonNull DatabaseError error) {
                Toast.makeText(getContext(), "Failed to fetch profile picture: " + error.getMessage(), Toast.LENGTH_SHORT).show();
                initializeAdapter();
            }
        });
    }

    private void fetchProfilePictureFromStorage() {
        StorageReference profilePicRef = FirebaseStorage.getInstance()
                .getReference("profile_pictures/" + userId + "/profile.jpg");

        profilePicRef.getDownloadUrl().addOnSuccessListener(uri -> {
            userProfileUri = uri.toString();
            initializeAdapter();
        }).addOnFailureListener(e -> {
            userProfileUri = null;
            initializeAdapter();
        });
    }

    private void initializeAdapter() {
        messageAdapter = new MessageAdapter(getContext(), messageList, (view, message, position) -> {
            showMessageOptions(view, message, position);
        }, userProfileUri);

        recyclerView.setAdapter(messageAdapter);
        LinearLayoutManager llm = new LinearLayoutManager(getContext());
        llm.setStackFromEnd (true);
        recyclerView.setLayoutManager(llm);

        loadChatHistory();
    }

    @Override
    public void onDestroy() {
        super.onDestroy();
    }

    private void loadChatHistory() {
        messageList.clear();
        mDatabase.orderByChild("timestamp").addValueEventListener(new ValueEventListener() {
            @Override
            public void onDataChange(@NonNull DataSnapshot dataSnapshot) {
                messageList.clear();
                for (DataSnapshot messageSnapshot : dataSnapshot.getChildren()) {
                    try {
                        // Attempt to parse the snapshot as a Message object
                        Message message = messageSnapshot.getValue(Message.class);
                        if (message != null) {
                            messageList.add(message);
                        } else {
                            Log.e("ChatFragment", "Invalid message format: " + messageSnapshot.toString());
                        }
                    } catch (DatabaseException e) {
                        Log.e("ChatFragment", "Error parsing message: " + e.getMessage());
                    }
                }
                messageAdapter.notifyDataSetChanged();
                if (!messageList.isEmpty()) {
                    recyclerView.smoothScrollToPosition(messageList.size() - 1);
                }
            }

            @Override
            public void onCancelled(@NonNull DatabaseError databaseError) {
                Toast.makeText(getContext(), "Failed to load messages: " + databaseError.getMessage(), Toast.LENGTH_SHORT).show();
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
                chatContext.add(message); // Add new message to context
                saveChatContext(message, sentBy); // Corrected to pass the message and sentBy
            });
        }
    }


    void addResponse(String response) {
        addToChat(response, Message.SENT_BY_BOT);
    }

    void callAPI(String question) {
        JSONObject jsonBody = new JSONObject();
        try {
            jsonBody.put("model", "gpt-4");  // Updated model name
            jsonBody.put("messages", new JSONArray()
                    .put(new JSONObject()
                            .put("role", "system")
                            .put("content", "You are a friendly buddy called Pookies. Your main job is to accompany, support, and give positive feedback to the user named " + userName +
                                    "Here are your constraints: 1. Refrain from using any bad words. 2. Avoid talking or engaging in any negative or inappropriate topics, including drugs, explicit content, or NSFW discussions. " +
                                    "3. Keep the user in high spirits and maintain a good mood. 4. Keep the chat casual and fun, adjusting to the latest internet trends and slang. Don't be overly descriptive and sounds unnatural like a bot. Behave humanly as much as possible. " +
                                    "5. Do not be overly excited at the start of the conversation."))
                    .put(new JSONObject()
                            .put("role", "user")
                            .put("content", question)));

            jsonBody.put("max_tokens", 4000);
            jsonBody.put("temperature", 0.5);
        } catch (JSONException e) {
            e.printStackTrace();
        }

        RequestBody body = RequestBody.create(jsonBody.toString(), JSON);
        Request request = new Request.Builder()
                .url("https://api.openai.com/v1/chat/completions")
                .header("Authorization", "Bearer OPENAIKEY")
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
                        String result = firstChoice.getJSONObject ("message").getString("content");
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
}