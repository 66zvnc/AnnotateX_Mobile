package com.example.annotatex_mobile;

import android.content.Context;
import android.graphics.Color;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageView;
import android.widget.TextView;
import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;
import java.util.List;
import com.bumptech.glide.Glide;

public class LeaderboardAdapter extends RecyclerView.Adapter<LeaderboardAdapter.ViewHolder> {
    private List<LeaderboardItem> items;
    private Context context;

    public LeaderboardAdapter(List<LeaderboardItem> items, Context context) {
        this.items = items;
        this.context = context;
    }

    @NonNull
    @Override
    public ViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        View view = LayoutInflater.from(parent.getContext())
                .inflate(R.layout.item_leaderboard, parent, false);
        return new ViewHolder(view);
    }

    @Override
    public void onBindViewHolder(@NonNull ViewHolder holder, int position) {
        LeaderboardItem item = items.get(position);
        
        // Always show the actual position (1-based indexing)
        holder.rankTextView.setText(String.valueOf(position + 1));
        holder.userNameTextView.setText(item.getUserName());
        holder.xpTextView.setText(String.valueOf(item.getBooksRead()) + " books");

        // Load profile image using Glide
        if (item.getProfileImageUrl() != null && !item.getProfileImageUrl().isEmpty()) {
            Glide.with(context)
                    .load(item.getProfileImageUrl())
                    .placeholder(R.drawable.ic_default_profile)
                    .error(R.drawable.ic_default_profile)
                    .circleCrop()
                    .into(holder.userImageView);
        } else {
            holder.userImageView.setImageResource(R.drawable.ic_default_profile);
        }

        // Highlight current user
        if (item.isCurrentUser()) {
            holder.itemView.setBackgroundColor(context.getResources().getColor(R.color.highlight_color));
            holder.userNameTextView.setTextColor(context.getResources().getColor(R.color.white));
            holder.xpTextView.setTextColor(context.getResources().getColor(R.color.white));
            holder.rankTextView.setTextColor(context.getResources().getColor(R.color.white));
        } else {
            holder.itemView.setBackgroundColor(context.getResources().getColor(android.R.color.transparent));
            holder.userNameTextView.setTextColor(context.getResources().getColor(R.color.black));
            holder.xpTextView.setTextColor(context.getResources().getColor(R.color.black));
            holder.rankTextView.setTextColor(context.getResources().getColor(R.color.black));
        }
    }

    @Override
    public int getItemCount() {
        return items.size();
    }

    static class ViewHolder extends RecyclerView.ViewHolder {
        TextView rankTextView;
        ImageView userImageView;
        TextView userNameTextView;
        TextView xpTextView;

        ViewHolder(View view) {
            super(view);
            rankTextView = view.findViewById(R.id.rankTextView);
            userImageView = view.findViewById(R.id.userImageView);
            userNameTextView = view.findViewById(R.id.userNameTextView);
            xpTextView = view.findViewById(R.id.xpTextView);
        }
    }
} 