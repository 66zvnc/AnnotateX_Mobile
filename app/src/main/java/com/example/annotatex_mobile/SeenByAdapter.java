package com.example.annotatex_mobile;

import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageView;
import android.widget.TextView;
import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;
import com.bumptech.glide.Glide;
import com.google.firebase.firestore.FirebaseFirestore;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Date;
import java.util.List;
import java.util.Locale;
import java.util.Map;

public class SeenByAdapter extends RecyclerView.Adapter<SeenByAdapter.ViewHolder> {
    private static final String TAG = "SeenByAdapter";
    private final List<UserSeenInfo> seenUsers;
    private final SimpleDateFormat dateFormat;
    private final FirebaseFirestore firestore;

    public SeenByAdapter(Map<String, Long> seenByMap) {
        this.seenUsers = new ArrayList<>();
        this.dateFormat = new SimpleDateFormat("MMM d, HH:mm", Locale.getDefault());
        this.firestore = FirebaseFirestore.getInstance();
        loadUserDetails(seenByMap);
    }

    private void loadUserDetails(Map<String, Long> seenByMap) {
        for (Map.Entry<String, Long> entry : seenByMap.entrySet()) {
            String userId = entry.getKey();
            Long seenTimestamp = entry.getValue();

            firestore.collection("users")
                    .document(userId)
                    .get()
                    .addOnSuccessListener(documentSnapshot -> {
                        if (documentSnapshot.exists()) {
                            String username = documentSnapshot.getString("username");
                            String profileImageUrl = documentSnapshot.getString("profileImageUrl");
                            
                            UserSeenInfo userInfo = new UserSeenInfo(
                                userId,
                                username != null ? username : "Unknown User",
                                profileImageUrl,
                                seenTimestamp
                            );
                            
                            seenUsers.add(userInfo);
                            notifyItemInserted(seenUsers.size() - 1);
                        } else {
                            Log.w(TAG, "User document not found for ID: " + userId);
                        }
                    })
                    .addOnFailureListener(e -> 
                        Log.e(TAG, "Error loading user details for ID: " + userId, e));
        }
    }

    @NonNull
    @Override
    public ViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        View view = LayoutInflater.from(parent.getContext())
                .inflate(R.layout.item_seen_by, parent, false);
        return new ViewHolder(view);
    }

    @Override
    public void onBindViewHolder(@NonNull ViewHolder holder, int position) {
        UserSeenInfo user = seenUsers.get(position);
        
        holder.userName.setText(user.username);
        holder.seenTime.setText(dateFormat.format(new Date(user.seenTimestamp)));

        if (user.profileImageUrl != null && !user.profileImageUrl.isEmpty()) {
            Glide.with(holder.itemView.getContext())
                    .load(user.profileImageUrl)
                    .placeholder(R.drawable.ic_default_profile)
                    .error(R.drawable.ic_default_profile)
                    .circleCrop()
                    .into(holder.userProfileImage);
        } else {
            holder.userProfileImage.setImageResource(R.drawable.ic_default_profile);
        }
    }

    @Override
    public int getItemCount() {
        return seenUsers.size();
    }

    static class ViewHolder extends RecyclerView.ViewHolder {
        ImageView userProfileImage;
        TextView userName;
        TextView seenTime;

        ViewHolder(View view) {
            super(view);
            userProfileImage = view.findViewById(R.id.userProfileImage);
            userName = view.findViewById(R.id.userName);
            seenTime = view.findViewById(R.id.seenTime);
        }
    }

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
} 