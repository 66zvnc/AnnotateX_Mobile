package com.example.annotatex_mobile;

import android.content.Context;
import android.util.Log;
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

public class ActivitiesAdapter extends RecyclerView.Adapter<ActivitiesAdapter.ActivityViewHolder> {

    private final List<FriendRequest> activitiesList;
    private final Context context;
    private final FirebaseFirestore firestore;
    private final FirebaseAuth auth;

    public ActivitiesAdapter(Context context, List<FriendRequest> activitiesList) {
        this.context = context;
        this.activitiesList = activitiesList;
        this.firestore = FirebaseFirestore.getInstance();
        this.auth = FirebaseAuth.getInstance();
    }

    @NonNull
    @Override
    public ActivityViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        View view = LayoutInflater.from(context).inflate(R.layout.item_activity, parent, false);
        return new ActivityViewHolder(view);
    }

    @Override
    public void onBindViewHolder(@NonNull ActivityViewHolder holder, int position) {
        FriendRequest request = activitiesList.get(position);

        // Set the friend request text
        holder.requestTextView.setText(request.getSenderName() + " sent you a friend request.");

        // Handle accept button click
        holder.acceptButton.setOnClickListener(v -> {
            Log.d("ActivitiesAdapter", "Accept button clicked for: " + request.getSenderName());
            holder.acceptButton.setEnabled(false);
            holder.denyButton.setEnabled(false); // Disable both buttons after action
            acceptFriendRequest(request, holder);
        });

        // Handle deny button click
        holder.denyButton.setOnClickListener(v -> {
            Log.d("ActivitiesAdapter", "Deny button clicked for: " + request.getSenderName());
            holder.acceptButton.setEnabled(false);
            holder.denyButton.setEnabled(false); // Disable both buttons after action
            denyFriendRequest(request);
        });
    }


    private void acceptFriendRequest(FriendRequest request, ActivityViewHolder holder) {
        String currentUserId = auth.getCurrentUser().getUid();

        if (currentUserId == null) {
            Toast.makeText(context, "User not logged in", Toast.LENGTH_SHORT).show();
            holder.acceptButton.setEnabled(true);
            return;
        }

        // Step 1: Fetch the receiver's (current user's) profile data
        firestore.collection("users")
                .document(currentUserId)
                .get()
                .addOnSuccessListener(receiverDoc -> {
                    if (receiverDoc.exists()) {
                        String receiverName = receiverDoc.getString("fullName");
                        String receiverProfileImageUrl = receiverDoc.getString("profileImageUrl");

                        // Step 2: Add the receiver to the sender's friends list
                        Friend receiverAsFriend = new Friend(
                                currentUserId,
                                receiverName,
                                receiverProfileImageUrl,
                                "Online"
                        );

                        firestore.collection("users")
                                .document(request.getSenderId())
                                .collection("friends")
                                .document(currentUserId)
                                .set(receiverAsFriend)
                                .addOnSuccessListener(aVoid1 -> {
                                    // Step 3: Fetch the sender's profile data
                                    firestore.collection("users")
                                            .document(request.getSenderId())
                                            .get()
                                            .addOnSuccessListener(senderDoc -> {
                                                if (senderDoc.exists()) {
                                                    String senderName = senderDoc.getString("fullName");
                                                    String senderProfileImageUrl = senderDoc.getString("profileImageUrl");

                                                    Friend senderAsFriend = new Friend(
                                                            request.getSenderId(),
                                                            senderName,
                                                            senderProfileImageUrl,
                                                            "Online"
                                                    );

                                                    // Step 4: Add the sender to the receiver's friends list
                                                    firestore.collection("users")
                                                            .document(currentUserId)
                                                            .collection("friends")
                                                            .document(request.getSenderId())
                                                            .set(senderAsFriend)
                                                            .addOnSuccessListener(aVoid2 -> {
                                                                // Step 5: Delete the friend request
                                                                deleteFriendRequest(request);
                                                                holder.acceptButton.setImageResource(R.drawable.ic_check);
                                                                Toast.makeText(context, "Friend request accepted", Toast.LENGTH_SHORT).show();
                                                            })
                                                            .addOnFailureListener(e -> {
                                                                Toast.makeText(context, "Failed to add sender to friends list", Toast.LENGTH_SHORT).show();
                                                            });
                                                }
                                            })
                                            .addOnFailureListener(e -> {
                                                Toast.makeText(context, "Failed to fetch sender's data", Toast.LENGTH_SHORT).show();
                                            });
                                })
                                .addOnFailureListener(e -> {
                                    Toast.makeText(context, "Failed to add receiver to sender's friends list", Toast.LENGTH_SHORT).show();
                                });
                    }
                })
                .addOnFailureListener(e -> {
                    Toast.makeText(context, "Failed to fetch receiver's data", Toast.LENGTH_SHORT).show();
                });
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
                    .addOnFailureListener(e -> {
                        Toast.makeText(context, "Failed to delete request", Toast.LENGTH_SHORT).show();
                    });
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

            // Check if any view is null
            if (requestTextView == null || acceptButton == null || denyButton == null) {
                throw new RuntimeException("View IDs not found in item_activity.xml");
            }
        }
    }
}