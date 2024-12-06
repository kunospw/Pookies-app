package com.example.pookies;

import android.content.Context;
import android.net.Uri;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;

import com.bumptech.glide.Glide;
import com.bumptech.glide.request.RequestOptions;

import java.util.List;

public class MessageAdapter extends RecyclerView.Adapter<MessageAdapter.MyViewHolder> {
    private final List<Message> messageList;
    private final MessageClickListener messageClickListener;
    private final String userProfileUri; // User profile picture URI
    private final Context context;

    public interface MessageClickListener {
        void onMessageClick(View view, Message message, int position);
    }

    // Constructor to accept the context, profile URI, and click listener
    public MessageAdapter(Context context, List<Message> messageList, MessageClickListener messageClickListener, String userProfileUri) {
        this.context = context;
        this.messageList = messageList;
        this.messageClickListener = messageClickListener;
        this.userProfileUri = userProfileUri;
    }

    @NonNull
    @Override
    public MyViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        View chatView = LayoutInflater.from(parent.getContext()).inflate(R.layout.chat_item, parent, false);
        MyViewHolder viewHolder = new MyViewHolder(chatView);

        // Set up click listeners
        viewHolder.leftTextView.setOnClickListener(v -> {
            int position = viewHolder.getAdapterPosition();
            if (position != RecyclerView.NO_POSITION && messageClickListener != null) {
                messageClickListener.onMessageClick(v, messageList.get(position), position);
            }
        });

        viewHolder.rightTextView.setOnClickListener(v -> {
            int position = viewHolder.getAdapterPosition();
            if (position != RecyclerView.NO_POSITION && messageClickListener != null) {
                messageClickListener.onMessageClick(v, messageList.get(position), position);
            }
        });

        return viewHolder;
    }

    @Override
    public void onBindViewHolder(@NonNull MyViewHolder holder, int position) {
        Message message = messageList.get(position);

        if (message.getSentBy().equals(Message.SENT_BY_ME)) {
            // User message: Show right chat view
            holder.leftChatView.setVisibility(View.GONE);
            holder.rightChatView.setVisibility(View.VISIBLE);

            holder.rightTextView.setText(message.getMessage());

            // Load user profile picture or default placeholder
            if (userProfileUri != null && !userProfileUri.isEmpty()) {
                Glide.with(context)
                        .load(userProfileUri)
                        .apply(RequestOptions.circleCropTransform())
                        .placeholder(R.drawable.person) // Default placeholder
                        .error(R.drawable.person) // Error placeholder
                        .into(holder.profileImage);
            } else {
                holder.profileImage.setImageResource(R.drawable.person); // Default placeholder
            }
        } else {
            // Bot message: Show left chat view
            holder.rightChatView.setVisibility(View.GONE);
            holder.leftChatView.setVisibility(View.VISIBLE);

            holder.leftTextView.setText(message.getMessage());

            // Set Pookies logo for bot messages
            holder.botLogo.setImageResource(R.drawable.pink_logo); // Pookies logo
        }
    }


    @Override
    public int getItemCount() {
        return messageList.size();
    }

    // ViewHolder class for chat items
    public static class MyViewHolder extends RecyclerView.ViewHolder {
        LinearLayout leftChatView, rightChatView;
        TextView leftTextView, rightTextView;
        ImageView profileImage, botLogo;

        public MyViewHolder(@NonNull View itemView) {
            super(itemView);
            leftChatView = itemView.findViewById(R.id.left_chat_view);
            rightChatView = itemView.findViewById(R.id.right_chat_view);
            leftTextView = itemView.findViewById(R.id.left_chat_text_view);
            rightTextView = itemView.findViewById(R.id.right_chat_text_view);
            profileImage = itemView.findViewById(R.id.profile_image); // Right-side user profile picture
            botLogo = itemView.findViewById(R.id.bot_logo); // Left-side Pookies logo
        }
    }
}
