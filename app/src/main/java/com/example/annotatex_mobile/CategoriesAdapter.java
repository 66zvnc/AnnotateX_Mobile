package com.example.annotatex_mobile;

import android.content.Context;
import android.content.Intent;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageView;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;
import java.util.List;

public class CategoriesAdapter extends RecyclerView.Adapter<CategoriesAdapter.ViewHolder> {

    private final List<Categories> categories;
    private final Context context;

    public CategoriesAdapter(Context context, List<Categories> categories) {
        this.context = context;
        this.categories = categories;
    }

    @Override
    public int getItemViewType(int position) {
        return categories.get(position).isHeader() ? 1 : 0;
    }

    @NonNull
    @Override
    public ViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        if (viewType == 1) {
            View view = LayoutInflater.from(parent.getContext())
                    .inflate(R.layout.item_category_header, parent, false);
            return new HeaderViewHolder(view);
        }
        View view = LayoutInflater.from(parent.getContext())
                .inflate(R.layout.item_category, parent, false);
        return new CategoryViewHolder(view);
    }

    @Override
    public void onBindViewHolder(@NonNull ViewHolder holder, int position) {
        Categories category = categories.get(position);
        if (holder instanceof HeaderViewHolder) {
            ((HeaderViewHolder) holder).headerText.setText(category.getName());
        } else {
            CategoryViewHolder categoryHolder = (CategoryViewHolder) holder;
            categoryHolder.categoryImage.setImageResource(category.getImageResId());
            
            holder.itemView.setOnClickListener(v -> {
                Intent intent = new Intent(context, CategoryActivity.class);
                intent.putExtra("CATEGORY_NAME", category.getName());
                context.startActivity(intent);
            });
        }
    }

    @Override
    public int getItemCount() {
        return categories.size();
    }

    public void updateCategories(List<Categories> newCategories) {
        categories.clear();
        categories.addAll(newCategories);
        notifyDataSetChanged();
    }

    public static class ViewHolder extends RecyclerView.ViewHolder {
        ImageView categoryImage;

        public ViewHolder(@NonNull View itemView) {
            super(itemView);
            categoryImage = itemView.findViewById(R.id.categoryImage);
        }
    }

    public static class HeaderViewHolder extends ViewHolder {
        TextView headerText;

        public HeaderViewHolder(@NonNull View itemView) {
            super(itemView);
            headerText = itemView.findViewById(R.id.headerText);
        }
    }

    public static class CategoryViewHolder extends ViewHolder {
        public CategoryViewHolder(@NonNull View itemView) {
            super(itemView);
        }
    }
}
