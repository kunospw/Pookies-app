package com.example.pookies;

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

import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.EditText;
import android.widget.ImageButton;
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
    String userId;
    DBHelper dbHelper;
    MessagingService messagingService;
    boolean isBound = false;
    public static final MediaType JSON = MediaType.get("application/json; charset=utf-8");
    OkHttpClient client = new OkHttpClient();

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
        dbHelper = new DBHelper(getContext());

        // Get userId from SharedPreferences using the correct key
        if (getActivity() != null) {
            userId = getActivity().getSharedPreferences("UserPrefs", Context.MODE_PRIVATE)
                    .getString("user_id", null); // Changed key from "userId" to "user_id" to match LoginActivity

            if (userId == null) {
                // Also check email as fallback since it's also stored in LoginActivity
                String email = getActivity().getSharedPreferences("UserPrefs", Context.MODE_PRIVATE)
                        .getString("email", null);

                if (email != null) {
                    // If we have email but no user_id, try to get user from database
                    User user = dbHelper.getUserByEmail(email);
                    if (user != null) {
                        userId = user.getUserId();
                        // Store it for future use
                        getActivity().getSharedPreferences("UserPrefs", Context.MODE_PRIVATE)
                                .edit()
                                .putString("user_id", userId)
                                .apply();
                    }
                }
            }

            if (userId == null) {
                Log.e("ChatFragment", "No user ID found!");
                // Redirect to login
                Intent intent = new Intent(getActivity(), LoginActivity.class);
                intent.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK | Intent.FLAG_ACTIVITY_CLEAR_TASK);
                startActivity(intent);
                getActivity().finish();
                return;
            }
        } else {
            Log.e("ChatFragment", "Activity is null!");
            return;
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

        messageAdapter = new MessageAdapter(messageList);
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
        if (userId != null && !userId.isEmpty()) {
            List<Message> messages = dbHelper.getMessages(userId);
            messageList.addAll(messages);
            messageAdapter.notifyDataSetChanged();
            recyclerView.smoothScrollToPosition(messageAdapter.getItemCount());
        } else {
            Log.e("ChatFragment", "Cannot load chat history: no user ID");
        }
    }
    void addToChat(String message, String sentBy) {
        if (getActivity() != null && userId != null && !userId.isEmpty()) {
            getActivity().runOnUiThread(() -> {
                Message newMessage = new Message(message, sentBy);
                newMessage.setUserId(userId); // Set the user ID before saving

                // Save message to SQLite
                boolean saved = dbHelper.insertMessage(newMessage, userId);
                if (!saved) {
                    Log.e("ChatFragment", "Failed to save message to database");
                }

                // Update UI
                messageList.add(newMessage);
                messageAdapter.notifyItemInserted(messageList.size() - 1);
                recyclerView.smoothScrollToPosition(messageAdapter.getItemCount());
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
                recyclerView.smoothScrollToPosition(messageAdapter.getItemCount());
            });
        }
    }
}