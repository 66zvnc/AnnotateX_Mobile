package com.example.annotatex_mobile;

import android.content.Context;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageView;
import android.widget.TextView;
import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;
import com.bumptech.glide.Glide;
import java.util.ArrayList;
import java.util.List;

public class UsersAdapter extends RecyclerView.Adapter<UsersAdapter.UserViewHolder> {

    private List<Friend> usersList;
    private final Context context;

    public UsersAdapter(Context context, List<Friend> usersList) {
        this.context = context;
        this.usersList = usersList;
    }

    @NonNull
    @Override
    public UserViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        View view = LayoutInflater.from(context).inflate(R.layout.item_user, parent, false);
        return new UserViewHolder(view);
    }

    @Override
    public void onBindViewHolder(@NonNull UserViewHolder holder, int position) {
        Friend user = usersList.get(position);

        // Check if user data is valid before setting
        if (user != null && user.isValid()) {
            holder.nameTextView.setText(user.getName());
            holder.statusTextView.setText(user.getStatus());

            // Load profile image using Glide
            if (user.hasProfileImage()) {
                Glide.with(context)
                        .load(user.getProfileImageUrl())
                        .placeholder(R.drawable.ic_default_profile)
                        .into(holder.profileImageView);
            } else {
                holder.profileImageView.setImageResource(R.drawable.ic_default_profile);
            }
        } else {
            holder.nameTextView.setText("Unknown User");
            holder.statusTextView.setText("Status Unknown");
            holder.profileImageView.setImageResource(R.drawable.ic_default_profile);
        }
    }

    @Override
    public int getItemCount() {
        return usersList.size();
    }

    public void updateList(List<Friend> newUsersList) {
        this.usersList.clear();
        this.usersList.addAll(newUsersList);
        notifyDataSetChanged();
    }


    public static class UserViewHolder extends RecyclerView.ViewHolder {
        ImageView profileImageView;
        TextView nameTextView, statusTextView;

        public UserViewHolder(@NonNull View itemView) {
            super(itemView);
            profileImageView = itemView.findViewById(R.id.profileImageView);
            nameTextView = itemView.findViewById(R.id.nameTextView);
            statusTextView = itemView.findViewById(R.id.statusTextView);
        }
    }
}
