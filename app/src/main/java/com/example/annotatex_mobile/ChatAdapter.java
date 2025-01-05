package com.example.annotatex_mobile;

import android.app.AlertDialog;
import android.content.Context;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageView;
import android.widget.TextView;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.bumptech.glide.Glide;
import com.google.firebase.firestore.DocumentSnapshot;
import com.google.firebase.firestore.FieldPath;
import com.google.firebase.firestore.FirebaseFirestore;

import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Date;
import java.util.List;
import java.util.Locale;
import java.util.Map;

public class ChatAdapter extends RecyclerView.Adapter<ChatAdapter.MessageViewHolder> {
    private static final int VIEW_TYPE_SENT = 1;
    private static final int VIEW_TYPE_RECEIVED = 2;
    
    private final List<Message> messages;
    private final String currentUserId;
    private final SimpleDateFormat timeFormat;
    private final FirebaseFirestore firestore;

    public ChatAdapter(String currentUserId) {
        this.messages = new ArrayList<>();
        this.currentUserId = currentUserId;
        this.timeFormat = new SimpleDateFormat("HH:mm", Locale.getDefault());
        this.firestore = FirebaseFirestore.getInstance();
    }

    @NonNull
    @Override
    public MessageViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        View view;
        if (viewType == VIEW_TYPE_SENT) {
            view = LayoutInflater.from(parent.getContext())
                    .inflate(R.layout.item_message_sent, parent, false);
        } else {
            view = LayoutInflater.from(parent.getContext())
                    .inflate(R.layout.item_message_received, parent, false);
        }
        return new MessageViewHolder(view);
    }

    @Override
    public void onBindViewHolder(@NonNull MessageViewHolder holder, int position) {
        Message message = messages.get(position);
        holder.messageText.setText(message.getContent());
        holder.timeText.setText(timeFormat.format(new Date(message.getTimestamp())));
        
        if (getItemViewType(position) == VIEW_TYPE_SENT) {
            holder.seenIndicator.setVisibility(message.isSeen() ? View.VISIBLE : View.GONE);
        }
        
        if (getItemViewType(position) == VIEW_TYPE_RECEIVED) {
            holder.senderName.setText(message.getSenderName());
            holder.senderName.setVisibility(View.VISIBLE);
            
            // Load profile picture
            if (holder.profileImage != null) {
                loadProfilePicture(message.getSenderId(), holder.profileImage);
            }
        }

        // Add long click listener
        holder.itemView.setOnLongClickListener(v -> {
            showSeenByDialog(v.getContext(), message);
            return true;
        });
    }

    private void loadProfilePicture(String userId, ImageView imageView) {
        FirebaseFirestore.getInstance()
            .collection("users")
            .document(userId)
            .get()
            .addOnSuccessListener(documentSnapshot -> {
                if (documentSnapshot.exists()) {
                    String profileImageUrl = documentSnapshot.getString("profileImageUrl");
                    if (profileImageUrl != null && !profileImageUrl.isEmpty()) {
                        Glide.with(imageView.getContext())
                            .load(profileImageUrl)
                            .placeholder(R.drawable.ic_default_profile)
                            .error(R.drawable.ic_default_profile)
                            .circleCrop()
                            .into(imageView);
                    } else {
                        imageView.setImageResource(R.drawable.ic_default_profile);
                    }
                }
            })
            .addOnFailureListener(e -> {
                Log.e("ChatAdapter", "Error loading profile picture", e);
                imageView.setImageResource(R.drawable.ic_default_profile);
            });
    }

    @Override
    public int getItemCount() {
        return messages.size();
    }

    @Override
    public int getItemViewType(int position) {
        Message message = messages.get(position);
        if (message.getSenderId().equals(currentUserId)) {
            return VIEW_TYPE_SENT;
        } else {
            return VIEW_TYPE_RECEIVED;
        }
    }

    public void addMessage(Message message) {
        messages.add(message);
        notifyItemInserted(messages.size() - 1);
    }

    public void clearMessages() {
        messages.clear();
        notifyDataSetChanged();
    }

    public Message getMessage(int position) {
        return messages.get(position);
    }

    private void showSeenByDialog(Context context, Message message) {
        Map<String, Long> seenBy = message.getSeenBy();
        if (seenBy == null || seenBy.isEmpty()) {
            showNoViewersDialog(context);
            return;
        }

        // Create the custom layout for the dialog
        View dialogView = LayoutInflater.from(context).inflate(R.layout.dialog_seen_by, null);
        RecyclerView recyclerView = dialogView.findViewById(R.id.seenByRecyclerView);
        recyclerView.setLayoutManager(new LinearLayoutManager(context));
        
        // Create adapter with the seenBy map from the message
        SeenByAdapter adapter = new SeenByAdapter(seenBy);
        recyclerView.setAdapter(adapter);

        new AlertDialog.Builder(context)
            .setTitle("Seen by")
            .setView(dialogView)
            .setPositiveButton("Close", null)
            .show();
    }

    private void showNoViewersDialog(Context context) {
        new AlertDialog.Builder(context)
            .setTitle("Message Details")
            .setMessage("No one has seen this message yet")
            .setPositiveButton("OK", null)
            .show();
    }

    // Helper class to store user seen information
    static class UserSeenInfo {
        String userId;
        String username;
        String profileImageUrl;
        long seenTimestamp;

        UserSeenInfo(String userId, String username, String profileImageUrl, long seenTimestamp) {
            this.userId = userId;
            this.username = username;
            this.profileImageUrl = profileImageUrl;
            this.seenTimestamp = seenTimestamp;
        }
    }

    static class MessageViewHolder extends RecyclerView.ViewHolder {
        TextView messageText;
        TextView timeText;
        TextView senderName;
        ImageView seenIndicator;
        ImageView profileImage;

        MessageViewHolder(View itemView) {
            super(itemView);
            messageText = itemView.findViewById(R.id.messageText);
            timeText = itemView.findViewById(R.id.timeText);
            senderName = itemView.findViewById(R.id.senderName);
            seenIndicator = itemView.findViewById(R.id.seenIndicator);
            profileImage = itemView.findViewById(R.id.profileImage);
        }
    }
} 