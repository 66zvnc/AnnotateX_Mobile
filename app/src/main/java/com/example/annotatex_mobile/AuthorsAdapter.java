package com.example.annotatex_mobile;

import android.content.Context;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;

import com.google.android.material.imageview.ShapeableImageView;

import java.util.List;

public class AuthorsAdapter extends RecyclerView.Adapter<AuthorsAdapter.ViewHolder> {
    private final List<Author> authors;
    private final Context context;

    public AuthorsAdapter(Context context, List<Author> authors) {
        this.context = context;
        this.authors = authors;
    }

    @NonNull
    @Override
    public ViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        View view = LayoutInflater.from(context).inflate(R.layout.item_author, parent, false);
        return new ViewHolder(view);
    }

    @Override
    public void onBindViewHolder(@NonNull ViewHolder holder, int position) {
        Author author = authors.get(position);
        holder.authorName.setText(author.getName());
        holder.authorImage.setImageResource(author.getImageResId());
    }

    @Override
    public int getItemCount() {
        return authors.size();
    }

    static class ViewHolder extends RecyclerView.ViewHolder {
        ShapeableImageView authorImage;
        TextView authorName;

        ViewHolder(@NonNull View itemView) {
            super(itemView);
            authorImage = itemView.findViewById(R.id.authorImage);
            authorName = itemView.findViewById(R.id.authorName);
        }
    }
} 