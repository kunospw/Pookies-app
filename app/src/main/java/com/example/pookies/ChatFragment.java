package com.example.pookies;

import android.app.Activity;
import android.app.AlertDialog;
import android.content.Context;
import android.content.Intent;
import android.net.Uri;
import android.os.Bundle;

import androidx.activity.result.ActivityResultLauncher;
import androidx.activity.result.contract.ActivityResultContracts;
import androidx.annotation.NonNull;
import androidx.fragment.app.Fragment;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import android.provider.MediaStore;
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
import java.util.Arrays;
import java.util.HashMap;
import java.util.List;
import java.util.concurrent.TimeUnit;

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
    OkHttpClient client = new OkHttpClient.Builder()
            .connectTimeout(60, TimeUnit.SECONDS)  // Increase connection timeout
            .readTimeout(60, TimeUnit.SECONDS)     // Increase read timeout
            .writeTimeout(60, TimeUnit.SECONDS)    // Increase write timeout
            .build();
    DatabaseReference mDatabase;
    private String userProfileUri = null;
    private String userName = "User"; // Default value if name isn't fetched
    private List<String> chatContext = new ArrayList<>();
    private Uri selectedImageUri;
    private ImageButton attachImageBtn;
    private ActivityResultLauncher<Intent> imagePickerLauncher;
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
            fetchUserProfilePictureAndInitializeAdapter();
        }
        imagePickerLauncher = registerForActivityResult(
                new ActivityResultContracts.StartActivityForResult(),
                result -> {
                    if (result.getResultCode() == Activity.RESULT_OK) {
                        Intent data = result.getData();
                        if (data != null) {
                            selectedImageUri = data.getData();
                            uploadImageAndSend();
                        }
                    }
                }
        );
    }

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {
        View view = inflater.inflate(R.layout.fragment_chat, container, false);
        recyclerView = view.findViewById(R.id.recycler_view);
        messageEditText = view.findViewById(R.id.message_edit_text);
        sendButton = view.findViewById(R.id.send_btn);
        attachImageBtn = view.findViewById(R.id.attach_image_btn);
        attachImageBtn.setOnClickListener(v -> openImagePicker());

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
    private void openImagePicker() {
        Intent intent = new Intent(Intent.ACTION_PICK, MediaStore.Images.Media.EXTERNAL_CONTENT_URI);
        imagePickerLauncher.launch(intent);
    }

    private void uploadImageAndSend() {
        if (selectedImageUri != null) {
            // Optional: Add a caption dialog
            AlertDialog.Builder builder = new AlertDialog.Builder(getContext());
            final EditText captionInput = new EditText(getContext());
            captionInput.setHint("Add a caption (optional)");

            builder.setTitle("Image Caption")
                    .setView(captionInput)
                    .setPositiveButton("Send", (dialog, which) -> {
                        String caption = captionInput.getText().toString().trim();

                        StorageReference storageRef = FirebaseStorage.getInstance().getReference()
                                .child("chat_images/" + userId + "/" + System.currentTimeMillis() + ".jpg");

                        storageRef.putFile(selectedImageUri)
                                .addOnSuccessListener(taskSnapshot -> {
                                    storageRef.getDownloadUrl().addOnSuccessListener(uri -> {
                                        // Create a message with the image and optional caption
                                        Message imageMessage = new Message("Sent an image", Message.SENT_BY_ME);
                                        imageMessage.setImageUrl(uri.toString());

                                        if (!caption.isEmpty()) {
                                            imageMessage.setCaption(caption);
                                        }

                                        // Save to messages table
                                        String messageId = mDatabase.push().getKey();
                                        if (messageId != null) {
                                            mDatabase.child(messageId).setValue(imageMessage)
                                                    .addOnSuccessListener(aVoid -> {
                                                        saveContextMessage(imageMessage);

                                                        // Call AI to analyze the image with optional caption
                                                        analyzeImage(uri.toString(), caption);
                                                    })
                                                    .addOnFailureListener(e -> {
                                                        Toast.makeText(getContext(), "Failed to send image: " + e.getMessage(),
                                                                Toast.LENGTH_SHORT).show();
                                                    });
                                        }
                                    });
                                })
                                .addOnFailureListener(e -> {
                                    Toast.makeText(getContext(), "Image upload failed: " + e.getMessage(),
                                            Toast.LENGTH_SHORT).show();
                                });
                    })
                    .setNegativeButton("Send without caption", (dialog, which) -> {
                        // Proceed without a caption
                        StorageReference storageRef = FirebaseStorage.getInstance().getReference()
                                .child("chat_images/" + userId + "/" + System.currentTimeMillis() + ".jpg");

                        storageRef.putFile(selectedImageUri)
                                .addOnSuccessListener(taskSnapshot -> {
                                    storageRef.getDownloadUrl().addOnSuccessListener(uri -> {
                                        // Create a message with the image
                                        Message imageMessage = new Message("Sent an image", Message.SENT_BY_ME);
                                        imageMessage.setImageUrl(uri.toString());

                                        // Save to messages table
                                        String messageId = mDatabase.push().getKey();
                                        if (messageId != null) {
                                            mDatabase.child(messageId).setValue(imageMessage)
                                                    .addOnSuccessListener(aVoid -> {
                                                        saveContextMessage(imageMessage);

                                                        // Call AI to analyze the image without a caption
                                                        analyzeImage(uri.toString(), null);
                                                    })
                                                    .addOnFailureListener(e -> {
                                                        Toast.makeText(getContext(), "Failed to send image: " + e.getMessage(),
                                                                Toast.LENGTH_SHORT).show();
                                                    });
                                        }
                                    });
                                })
                                .addOnFailureListener(e -> {
                                    Toast.makeText(getContext(), "Image upload failed: " + e.getMessage(),
                                            Toast.LENGTH_SHORT).show();
                                });
                    })
                    .show();
        }
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

        chatContextRef.limitToLast(20) // Limit to last 20 messages
                .addValueEventListener(new ValueEventListener() {
                    @Override
                    public void onDataChange(@NonNull DataSnapshot snapshot) {
                        chatContext.clear();
                        for (DataSnapshot childSnapshot : snapshot.getChildren()) {
                            Message message = childSnapshot.getValue(Message.class);
                            if (message != null) {
                                chatContext.add(message.getMessage());
                            }
                        }
                    }

                    @Override
                    public void onCancelled(@NonNull DatabaseError error) {
                        Log.e("ChatFragment", "Error fetching chat context: " + error.getMessage());
                    }
                });
    }
    private void saveContextMessage(Message message) {
        DatabaseReference chatContextRef = FirebaseDatabase.getInstance()
                .getReference("users").child(userId).child("chatContext");

        chatContextRef.addListenerForSingleValueEvent(new ValueEventListener() {
            @Override
            public void onDataChange(@NonNull DataSnapshot snapshot) {
                long contextSize = snapshot.getChildrenCount();

                // If context is getting too large, remove oldest messages
                if (contextSize >= 20) {
                    List<String> keysToRemove = new ArrayList<>();
                    for (DataSnapshot childSnapshot : snapshot.getChildren()) {
                        if (keysToRemove.size() < contextSize - 19) {
                            keysToRemove.add(childSnapshot.getKey());
                        }
                    }

                    // Remove oldest messages
                    for (String key : keysToRemove) {
                        chatContextRef.child(key).removeValue();
                    }
                }

                // Add new context record with minimal information
                HashMap<String, Object> contextEntry = new HashMap<>();
                contextEntry.put("message", message.getMessage());
                contextEntry.put("sentBy", message.getSentBy());
                contextEntry.put("timestamp", message.getTimestamp());

                chatContextRef.push().setValue(contextEntry);
            }

            @Override
            public void onCancelled(@NonNull DatabaseError error) {
                Log.e("ChatFragment", "Error managing chat context: " + error.getMessage());
            }
        });
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

    private void fetchUserProfilePictureAndInitializeAdapter() {
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
        // Capture the current value of userProfileUri before using it in the lambda
        final String profileUri = userProfileUri;

        messageAdapter = new MessageAdapter(getContext(), messageList, (view, message, position) -> {
            showMessageOptions(view, message, position);
        }, profileUri);

        recyclerView.setAdapter(messageAdapter);
        LinearLayoutManager llm = new LinearLayoutManager(getContext());
        llm.setStackFromEnd(true);
        recyclerView.setLayoutManager(llm);

        loadChatHistory();
    }
    @Override
    public void onDestroy() {
        super.onDestroy();
    }

    private void loadChatHistory() {
        mDatabase.orderByChild("timestamp").addValueEventListener(new ValueEventListener() {
            @Override
            public void onDataChange(@NonNull DataSnapshot dataSnapshot) {
                // Clear the list before adding messages
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
        if (message != null && !message.trim().isEmpty()) {
            if (getActivity() != null) {
                getActivity().runOnUiThread(() -> {
                    Message newMessage = new Message(message, sentBy);

                    // Save to messages table
                    String messageId = mDatabase.push().getKey();
                    if (messageId != null) {
                        mDatabase.child(messageId).setValue(newMessage)
                                .addOnSuccessListener(aVoid -> {
                                    saveContextMessage(newMessage);
                                })
                                .addOnFailureListener(e -> {
                                    Toast.makeText(getContext(), "Failed to send message: " + e.getMessage(),
                                            Toast.LENGTH_SHORT).show();
                                });
                    }
                });
            }
        }
    }



    void addResponse(String response) {
        addToChat(response, Message.SENT_BY_BOT);
    }

    void callAPI(String question) {
        if (getActivity() != null) {
            getActivity().runOnUiThread(() -> {
                // Check if the message is an image generation request
                if (isImageGenerationRequest(question)) {
                    generateImage(question);
                } else {
                    // Fetch recent context messages from Firebase
                    DatabaseReference chatContextRef = FirebaseDatabase.getInstance()
                            .getReference("users").child(userId).child("chatContext");
                    chatContextRef.limitToLast(10).addListenerForSingleValueEvent(new ValueEventListener() {
                        @Override
                        public void onDataChange(@NonNull DataSnapshot snapshot) {
                            try {
                                JSONObject jsonBody = new JSONObject();
                                JSONArray messagesArray = new JSONArray();

                                // Add system message
                                messagesArray.put(new JSONObject()
                                        .put("role", "system")
                                        .put("content", "You are a friendly buddy called Pookies. Your main job is to accompany, support, and give positive feedback to the user named " + userName +
                                                "Here are your constraints: 1. Refrain from using any bad words. 2. Avoid talking or engaging in any negative or inappropriate topics, including drugs, explicit content, or NSFW discussions. " +
                                                "3. Keep the user in high spirits and maintain a good mood. 4. Keep the chat casual and fun, adjusting to the latest internet trends and slang. Don't be overly descriptive and sounds unnatural like a bot. Behave humanly as much as possible. " +
                                                "5. Do not be overly excited at the start of the conversation."));

                                // Add recent context messages
                                for (DataSnapshot messageSnapshot : snapshot.getChildren()) {
                                    Message contextMessage = messageSnapshot.getValue(Message.class);
                                    if (contextMessage != null) {
                                        String role = contextMessage.getSentBy().equals(Message.SENT_BY_ME) ? "user" : "assistant";
                                        messagesArray.put(new JSONObject()
                                                .put("role", role)
                                                .put("content", contextMessage.getMessage()));
                                    }
                                }

                                // Add current user question
                                messagesArray.put(new JSONObject()
                                        .put("role", "user")
                                        .put("content", question));

                                // Prepare the full JSON body
                                jsonBody.put("model", "gpt-4");
                                jsonBody.put("messages", messagesArray);
                                jsonBody.put("max_tokens", 4000);
                                jsonBody.put("temperature", 0.5);

                                // Prepare and send the API request
                                RequestBody body = RequestBody.create(jsonBody.toString(), JSON);
                                Request request = new Request.Builder()
                                        .url("https://api.openai.com/v1/chat/completions")
                                        .header("Authorization", "Bearer HEre")
                                        .post(body)
                                        .build();

                                client.newCall(request).enqueue(new Callback() {
                                    @Override
                                    public void onFailure(@NonNull Call call, @NonNull IOException e) {
                                        // Ensure response is added on main thread
                                        if (getActivity() != null) {
                                            getActivity().runOnUiThread(() ->
                                                    addResponse("Failed to load response: " + e.getMessage())
                                            );
                                        }
                                    }

                                    @Override
                                    public void onResponse(@NonNull Call call, @NonNull Response response) throws IOException {
                                        if (getActivity() != null) {
                                            getActivity().runOnUiThread(() -> {
                                                try {
                                                    if (response.isSuccessful()) {
                                                        String responseBody = response.body().string();
                                                        JSONObject jsonObject = new JSONObject(responseBody);
                                                        JSONArray jsonArray = jsonObject.getJSONArray("choices");
                                                        JSONObject firstChoice = jsonArray.getJSONObject(0);
                                                        String result = firstChoice.getJSONObject("message").getString("content");

                                                        // Add the bot's response to the chat
                                                        addResponse(result.trim());
                                                    } else {
                                                        // Handle unsuccessful response
                                                        addResponse("Failed to load response: " + response.code() + " " + response.message());
                                                    }
                                                } catch (JSONException | IOException e) {
                                                    // Handle parsing errors
                                                    addResponse("Error processing response: " + e.getMessage());
                                                }
                                            });
                                        }
                                    }
                                });

                            } catch (JSONException e) {
                                // Handle JSON creation errors
                                Log.e("ChatFragment", "Error creating JSON for API call", e);
                                addResponse("Error preparing API request: " + e.getMessage());
                            }
                        }

                        @Override
                        public void onCancelled(@NonNull DatabaseError error) {
                            // Handle database error
                            Log.e("ChatFragment", "Error fetching context messages", error.toException());
                            addResponse("Error fetching chat history: " + error.getMessage());
                        }
                    });
                }
            });
        }
    }
    private boolean isImageGenerationRequest(String message) {
        // List of keywords that might indicate an image generation request
        String[] imageGenerationTriggers = {
                "generate an image of",
                "create an image of",
                "draw a picture of",
                "make an image of",
                "generate image of",
                "create image of"
        };

        // Convert message to lowercase for case-insensitive matching
        String lowerMessage = message.toLowerCase().trim();

        // Check if the message starts with any of the triggers
        for (String trigger : imageGenerationTriggers) {
            if (lowerMessage.startsWith(trigger)) {
                return true;
            }
        }
        return false;
    }
    private void generateImage(String prompt) {
        // Extract the actual image description by removing the generation trigger
        String[] imageGenerationTriggers = {
                "generate an image of",
                "create an image of",
                "draw a picture of",
                "make an image of",
                "generate image of",
                "create image of"
        };

        String imageDescription = Arrays.stream(imageGenerationTriggers).filter(trigger -> prompt.toLowerCase().startsWith(trigger)).findFirst().map(trigger -> prompt.substring(trigger.length()).trim()).orElse(prompt);

        try {
            JSONObject jsonBody = new JSONObject();
            jsonBody.put("model", "dall-e-3");
            jsonBody.put("prompt", imageDescription);
            jsonBody.put("n", 1);
            jsonBody.put("size", "1024x1024");

            RequestBody body = RequestBody.create(jsonBody.toString(), JSON);
            Request request = new Request.Builder()
                    .url("https://api.openai.com/v1/images/generations")
                    .header("Authorization", "Bearer Here")
                    .post(body)
                    .build();

            client.newCall(request).enqueue(new Callback() {
                @Override
                public void onFailure(@NonNull Call call, @NonNull IOException e) {
                    if (getActivity() != null) {
                        getActivity().runOnUiThread(() ->
                                addResponse("Failed to generate image: " + e.getMessage())
                        );
                    }
                }

                @Override
                public void onResponse(@NonNull Call call, @NonNull Response response) throws IOException {
                    if (getActivity() != null) {
                        getActivity().runOnUiThread(() -> {
                            try {
                                if (response.isSuccessful()) {
                                    String responseBody = response.body().string();
                                    JSONObject jsonObject = new JSONObject(responseBody);
                                    JSONArray dataArray = jsonObject.getJSONArray("data");

                                    if (dataArray.length() > 0) {
                                        String imageUrl = dataArray.getJSONObject(0).getString("url");

                                        // Create a new Message with the image URL
                                        Message imageMessage = new Message(imageDescription, Message.SENT_BY_BOT);
                                        imageMessage.setImageUrl(imageUrl);
                                        imageMessage.setCaption("Generated image: " + imageDescription);

                                        // Save to messages table
                                        String messageId = mDatabase.push().getKey();
                                        if (messageId != null) {
                                            mDatabase.child(messageId).setValue(imageMessage)
                                                    .addOnSuccessListener(aVoid -> {
                                                        saveContextMessage(imageMessage);
                                                    })
                                                    .addOnFailureListener(e -> {
                                                        Toast.makeText(getContext(), "Failed to save image message: " + e.getMessage(),
                                                                Toast.LENGTH_SHORT).show();
                                                    });
                                        }
                                    } else {
                                        addResponse("No image was generated.");
                                    }
                                } else {
                                    addResponse("Failed to generate image: " + response.code() + " " + response.message());
                                }
                            } catch (JSONException | IOException e) {
                                addResponse("Error processing image generation response: " + e.getMessage());
                            }
                        });
                    }
                }
            });

        } catch (JSONException e) {
            Log.e("ChatFragment", "Error creating JSON for image generation", e);
            addResponse("Error preparing image generation request: " + e.getMessage());
        }
    }
    private void analyzeImage(String imageUrl, String caption) {
        try {
            JSONObject jsonBody = new JSONObject();
            JSONArray messagesArray = new JSONArray();

            // System message with context
            messagesArray.put(new JSONObject()
                    .put("role", "system")
                    .put("content", "You are a friendly AI assistant named Pookies. " +
                            "Describe the image in detail and engage in a conversation about what you see. " +
                            "Consider the user's caption when analyzing the image. " +
                            "Be creative, descriptive, and keep the tone friendly and casual."));

            // Add current chat context
            for (String contextMessage : chatContext) {
                messagesArray.put(new JSONObject()
                        .put("role", "assistant")
                        .put("content", contextMessage));
            }

            // Image analysis message with optional caption
            String analysisPrompt = "[IMAGE]" + imageUrl;
            if (caption != null && !caption.isEmpty()) {
                analysisPrompt += " Caption: " + caption;
            }

            messagesArray.put(new JSONObject()
                    .put("role", "user")
                    .put("content", analysisPrompt));

            // Prepare the full JSON body
            jsonBody.put("model", "gpt-4-vision-preview");
            jsonBody.put("messages", messagesArray);
            jsonBody.put("max_tokens", 4000);

            // Prepare and send the API request
            RequestBody body = RequestBody.create(jsonBody.toString(), JSON);
            Request request = new Request.Builder()
                    .url("https://api.openai.com/v1/chat/completions")
                    .header("Authorization", "Bearer HEre")
                    .post(body)
                    .build();

            client.newCall(request).enqueue(new Callback() {
                @Override
                public void onResponse(@NonNull Call call, @NonNull Response response) throws IOException {
                    if (getActivity() != null) {
                        getActivity().runOnUiThread(() -> {
                            try {
                                if (response.isSuccessful()) {
                                    String responseBody = response.body().string();
                                    JSONObject jsonObject = new JSONObject(responseBody);
                                    JSONArray choicesArray = jsonObject.getJSONArray("choices");

                                    if (choicesArray.length() > 0) {
                                        String imageAnalysis = choicesArray.getJSONObject(0)
                                                .getJSONObject("message")
                                                .getString("content");

                                        addResponse(imageAnalysis);
                                    }
                                } else {
                                    Log.e("API_RESPONSE", "Response code: " + response.code());
                                    Log.e("API_RESPONSE", "Response body: " + response.body().string());
                                    addResponse("Sorry, I couldn't analyze the image right now.");
                                }
                            } catch (Exception e) {
                                Log.e("API_PROCESSING", "Error parsing response", e);
                                addResponse("Error processing image: " + e.getMessage());
                            }
                        });
                    }
                }

                @Override
                public void onFailure(@NonNull Call call, @NonNull IOException e) {
                    if (getActivity() != null) {
                        getActivity().runOnUiThread(() -> {
                            Log.e("API_CALL", "Network failure", e);
                            addResponse("Failed to analyze image: " + e.getMessage());
                        });
                    }
                }
            });

        } catch (JSONException e) {
            Log.e("ChatFragment", "Error creating JSON for image analysis", e);
            addResponse("Error preparing image analysis request: " + e.getMessage());
        }
    }
}