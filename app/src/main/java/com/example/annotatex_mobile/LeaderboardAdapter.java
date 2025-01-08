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
        int rank = position + 1;
        
        // Reset background first
        holder.itemView.setBackgroundColor(context.getColor(android.R.color.white));
        
        // Handle backgrounds for top 3 and show rank number
        holder.rankTextView.setVisibility(View.VISIBLE);
        holder.rankTextView.setText(String.valueOf(rank));
        
        if (rank <= 3) {
            int backgroundResource;
            switch (rank) {
                case 1:
                    backgroundResource = R.drawable.rank_1_background;
                    break;
                case 2:
                    backgroundResource = R.drawable.rank_2_background;
                    break;
                case 3:
                    backgroundResource = R.drawable.rank_3_background;
                    break;
                default:
                    backgroundResource = 0;
                    break;
            }
            holder.itemView.setBackground(context.getDrawable(backgroundResource));
        } else {
            // Set light gray background for items below 3rd place
            holder.itemView.setBackgroundColor(Color.parseColor("#F7F1F1"));
        }
        
        // Set username and points
        holder.userNameTextView.setText(item.getUserName());
        holder.pointsTextView.setText(item.getBooksRead() + " pts.");
        
        // Load profile image
        if (item.getProfileImageUrl() != null && !item.getProfileImageUrl().isEmpty()) {
            Glide.with(context)
                    .load(item.getProfileImageUrl())
                    .placeholder(R.drawable.ic_default_profile)
                    .error(R.drawable.ic_default_profile)
                    .circleCrop()
                    .into(holder.userImageView);
        }
        
        // Highlight current user with a different background if needed
        if (item.isCurrentUser() && rank > 3) {
            holder.itemView.setBackgroundColor(context.getColor(R.color.highlight_color));
        }
    }

    @Override
    public int getItemCount() {
        return items.size();
    }

    static class ViewHolder extends RecyclerView.ViewHolder {
        TextView rankTextView;
        ImageView trophyIcon;
        ImageView userImageView;
        TextView userNameTextView;
        TextView pointsTextView;

        ViewHolder(View view) {
            super(view);
            rankTextView = view.findViewById(R.id.rankTextView);
            trophyIcon = view.findViewById(R.id.trophyIcon);
            userImageView = view.findViewById(R.id.userImageView);
            userNameTextView = view.findViewById(R.id.userNameTextView);
            pointsTextView = view.findViewById(R.id.pointsTextView);
        }
    }
} 