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

import java.util.List;

public class CollaborativeBooksAdapter extends RecyclerView.Adapter<CollaborativeBooksAdapter.ViewHolder> {

    private final Context context;
    private final List<Book> books;

    public CollaborativeBooksAdapter(Context context, List<Book> books) {
        this.context = context;
        this.books = books;
    }

    @NonNull
    @Override
    public ViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        View view = LayoutInflater.from(context).inflate(R.layout.item_collaborative_book, parent, false);
        return new ViewHolder(view);
    }

    @Override
    public void onBindViewHolder(@NonNull ViewHolder holder, int position) {
        Book book = books.get(position);
        holder.titleTextView.setText(book.getTitle());

        if (book.hasUrlCover()) {
            Glide.with(context).load(book.getCoverImageUrl()).into(holder.coverImageView);
        } else {
            holder.coverImageView.setImageResource(R.drawable.book_handle);
        }
    }

    @Override
    public int getItemCount() {
        return books.size();
    }

    static class ViewHolder extends RecyclerView.ViewHolder {
        ImageView coverImageView;
        TextView titleTextView;

        ViewHolder(View itemView) {
            super(itemView);
            coverImageView = itemView.findViewById(R.id.bookCoverImageView);
            titleTextView = itemView.findViewById(R.id.bookTitleTextView);
        }
    }
}
