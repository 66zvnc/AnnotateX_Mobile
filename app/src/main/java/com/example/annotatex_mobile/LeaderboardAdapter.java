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
        
        // Set rank with special styling for top 3
        holder.rankTextView.setText(String.valueOf(position + 1));
        
        if (position < 3) {
            holder.medalBackground.setVisibility(View.VISIBLE);
            int medalResource;
            switch (position) {
                case 0:
                    medalResource = R.drawable.gold_medal_bg;
                    holder.rankTextView.setTextColor(context.getColor(R.color.white));
                    break;
                case 1:
                    medalResource = R.drawable.silver_medal_bg;
                    holder.rankTextView.setTextColor(context.getColor(R.color.white));
                    break;
                case 2:
                    medalResource = R.drawable.bronze_medal_bg;
                    holder.rankTextView.setTextColor(context.getColor(R.color.white));
                    break;
                default:
                    medalResource = 0;
                    break;
            }
            holder.medalBackground.setBackgroundResource(medalResource);
            
            // Show XP badge for top 3
            holder.xpBadge.setVisibility(View.VISIBLE);
            holder.xpBadge.setText(item.getBooksRead() + " XP");
            holder.xpTextView.setVisibility(View.GONE);
        } else {
            holder.medalBackground.setVisibility(View.GONE);
            holder.xpBadge.setVisibility(View.GONE);
            holder.xpTextView.setVisibility(View.VISIBLE);
            holder.xpTextView.setText(item.getBooksRead() + " XP");
        }

        holder.userNameTextView.setText(item.getUserName());
        
        // Load user image with Glide
        if (item.getProfileImageUrl() != null && !item.getProfileImageUrl().isEmpty()) {
            Glide.with(context)
                .load(item.getProfileImageUrl())
                .placeholder(R.drawable.ic_default_profile)
                .error(R.drawable.ic_default_profile)
                .circleCrop()
                .into(holder.userImageView);
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
        ImageView medalBackground;
        TextView xpBadge;

        ViewHolder(View view) {
            super(view);
            rankTextView = view.findViewById(R.id.rankTextView);
            userImageView = view.findViewById(R.id.userImageView);
            userNameTextView = view.findViewById(R.id.userNameTextView);
            xpTextView = view.findViewById(R.id.xpTextView);
            medalBackground = view.findViewById(R.id.medalBackground);
            xpBadge = view.findViewById(R.id.xpBadge);
        }
    }
} 