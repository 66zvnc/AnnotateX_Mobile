package com.example.annotatex_mobile;

import android.content.Context;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageView;
import android.widget.TextView;
import android.widget.Toast;
import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.firestore.FirebaseFirestore;
import java.util.List;

public class NotificationsAdapter extends RecyclerView.Adapter<NotificationsAdapter.ActivityViewHolder> {

    private final List<FriendRequest> activitiesList;
    private final Context context;
    private final FirebaseFirestore firestore;
    private final FirebaseAuth auth;

    public NotificationsAdapter(Context context, List<FriendRequest> activitiesList) {
        this.context = context;
        this.activitiesList = activitiesList;
        this.firestore = FirebaseFirestore.getInstance();
        this.auth = FirebaseAuth.getInstance();
    }

    @NonNull
    @Override
    public ActivityViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        View view = LayoutInflater.from(context).inflate(R.layout.item_notification, parent, false);
        return new ActivityViewHolder(view);
    }

    @Override
    public void onBindViewHolder(@NonNull ActivityViewHolder holder, int position) {
        FriendRequest request = activitiesList.get(position);

        holder.requestTextView.setText(request.getSenderName() + " sent you a friend request.");

        holder.acceptButton.setOnClickListener(v -> {
            holder.acceptButton.setEnabled(false);
            holder.denyButton.setEnabled(false);
            acceptFriendRequest(request, holder);
        });

        holder.denyButton.setOnClickListener(v -> {
            holder.acceptButton.setEnabled(false);
            holder.denyButton.setEnabled(false);
            denyFriendRequest(request);
        });
    }

    private void acceptFriendRequest(FriendRequest request, ActivityViewHolder holder) {
        String currentUserId = auth.getCurrentUser() != null ? auth.getCurrentUser().getUid() : null;

        if (currentUserId == null) {
            Toast.makeText(context, "User not logged in", Toast.LENGTH_SHORT).show();
            return;
        }

        // Step 1: Fetch the receiver's profile (current user)
        firestore.collection("users")
                .document(currentUserId)
                .get()
                .addOnSuccessListener(receiverDoc -> {
                    if (receiverDoc.exists()) {
                        String receiverName = receiverDoc.getString("fullName");
                        String receiverProfileImageUrl = receiverDoc.getString("profileImageUrl");

                        // Step 2: Add receiver to sender's friends list
                        addReceiverToSender(request, currentUserId, receiverName, receiverProfileImageUrl, holder);
                    }
                })
                .addOnFailureListener(e -> Toast.makeText(context, "Failed to fetch profile data", Toast.LENGTH_SHORT).show());
    }

    private void addReceiverToSender(FriendRequest request, String currentUserId, String receiverName, String receiverProfileImageUrl, ActivityViewHolder holder) {
        // Create the Friend object with 'removed' status as false
        Friend receiverAsFriend = new Friend(currentUserId, receiverName, receiverProfileImageUrl, "Online", false);

        firestore.collection("users")
                .document(request.getSenderId())
                .collection("friends")
                .document(currentUserId)
                .set(receiverAsFriend)
                .addOnSuccessListener(aVoid -> {
                    // Step 3: Add sender to receiver's friends list
                    addSenderToReceiver(request, currentUserId, holder);
                })
                .addOnFailureListener(e -> Toast.makeText(context, "Failed to add friend", Toast.LENGTH_SHORT).show());
    }

    private void addSenderToReceiver(FriendRequest request, String currentUserId, ActivityViewHolder holder) {
        firestore.collection("users")
                .document(request.getSenderId())
                .get()
                .addOnSuccessListener(senderDoc -> {
                    if (senderDoc.exists()) {
                        String senderName = senderDoc.getString("fullName");
                        String senderProfileImageUrl = senderDoc.getString("profileImageUrl");

                        // Create the Friend object with 'removed' status as false
                        Friend senderAsFriend = new Friend(request.getSenderId(), senderName, senderProfileImageUrl, "Online", false);

                        firestore.collection("users")
                                .document(currentUserId)
                                .collection("friends")
                                .document(request.getSenderId())
                                .set(senderAsFriend)
                                .addOnSuccessListener(aVoid -> {
                                    deleteFriendRequest(request);
                                    holder.acceptButton.setImageResource(R.drawable.ic_check);
                                    Toast.makeText(context, "Friend request accepted", Toast.LENGTH_SHORT).show();
                                })
                                .addOnFailureListener(e -> Toast.makeText(context, "Failed to add friend", Toast.LENGTH_SHORT).show());
                    }
                })
                .addOnFailureListener(e -> Toast.makeText(context, "Failed to fetch profile data", Toast.LENGTH_SHORT).show());
    }

    private void denyFriendRequest(FriendRequest request) {
        deleteFriendRequest(request);
        Toast.makeText(context, "Friend request denied", Toast.LENGTH_SHORT).show();
    }

    private void deleteFriendRequest(FriendRequest request) {
        String currentUserId = auth.getCurrentUser() != null ? auth.getCurrentUser().getUid() : null;

        if (currentUserId != null) {
            firestore.collection("users")
                    .document(currentUserId)
                    .collection("friendRequests")
                    .document(request.getSenderId())
                    .delete()
                    .addOnSuccessListener(aVoid -> {
                        activitiesList.remove(request);
                        notifyDataSetChanged();
                    })
                    .addOnFailureListener(e -> Toast.makeText(context, "Failed to delete request", Toast.LENGTH_SHORT).show());
        }
    }

    @Override
    public int getItemCount() {
        return activitiesList.size();
    }

    public static class ActivityViewHolder extends RecyclerView.ViewHolder {
        TextView requestTextView;
        ImageView acceptButton, denyButton;

        public ActivityViewHolder(@NonNull View itemView) {
            super(itemView);
            requestTextView = itemView.findViewById(R.id.requestTextView);
            acceptButton = itemView.findViewById(R.id.acceptButton);
            denyButton = itemView.findViewById(R.id.denyButton);
        }
    }
}
