package com.example.annotatex_mobile;

import android.content.Context;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageView;

import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;

import com.bumptech.glide.Glide;

import java.util.List;

public class BookSelectionAdapter extends RecyclerView.Adapter<BookSelectionAdapter.BookViewHolder> {

    private Context context;
    private List<Book> books;
    private OnBookClickListener listener;

    // Constructor
    public BookSelectionAdapter(Context context, List<Book> books, OnBookClickListener listener) {
        this.context = context;
        this.books = books;
        this.listener = listener;
    }

    @NonNull
    @Override
    public BookViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        // Inflate the item_book layout
        View view = LayoutInflater.from(context).inflate(R.layout.item_book, parent, false);
        return new BookViewHolder(view);
    }

    @Override
    public void onBindViewHolder(@NonNull BookViewHolder holder, int position) {
        Book book = books.get(position);

        // Load book cover using Glide
        Glide.with(context)
                .load(book.getCoverImageUrl())
                .placeholder(R.drawable.book_handle) // Placeholder image
                .into(holder.bookCoverImage);

        // Set up the click listener for the book item
        holder.itemView.setOnClickListener(v -> listener.onBookClick(book));
    }

    @Override
    public int getItemCount() {
        return books.size(); // Return the size of the books list
    }

    public static class BookViewHolder extends RecyclerView.ViewHolder {

        ImageView bookCoverImage;

        public BookViewHolder(View itemView) {
            super(itemView);
            // Bind the book cover image
            bookCoverImage = itemView.findViewById(R.id.imageView);
        }
    }

    // Interface to handle book selection
    public interface OnBookClickListener {
        void onBookClick(Book book); // Handle book click events
    }
}
