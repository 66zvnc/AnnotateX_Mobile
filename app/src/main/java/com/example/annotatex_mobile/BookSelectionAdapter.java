package com.example.annotatex_mobile;

import android.content.Context;
import android.graphics.BitmapFactory;
import android.graphics.drawable.Drawable;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageView;
import android.widget.RadioButton;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.recyclerview.widget.RecyclerView;

import com.bumptech.glide.Glide;
import com.bumptech.glide.load.DataSource;
import com.bumptech.glide.load.engine.DiskCacheStrategy;
import com.bumptech.glide.load.engine.GlideException;
import com.bumptech.glide.request.RequestListener;
import com.bumptech.glide.request.target.Target;

import java.io.File;
import java.util.List;

public class BookSelectionAdapter extends RecyclerView.Adapter<BookSelectionAdapter.BookViewHolder> {

    private static final String TAG = "BookSelectionAdapter";

    private final Context context;
    private final List<Book> books;
    private final OnBookClickListener listener;
    private int selectedPosition = -1; // Keep track of selected book

    // Constructor
    public BookSelectionAdapter(Context context, List<Book> books, OnBookClickListener listener) {
        this.context = context;
        this.books = books;
        this.listener = listener;
    }

    @NonNull
    @Override
    public BookViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        // Inflate the updated item_selection layout
        View view = LayoutInflater.from(context).inflate(R.layout.item_selection, parent, false);
        return new BookViewHolder(view);
    }

    @Override
    public void onBindViewHolder(@NonNull BookViewHolder holder, int position) {
        Book book = books.get(holder.getAdapterPosition());

        Log.d(TAG, "Binding book: Title=" + book.getTitle() + ", Cover URL=" + book.getCoverImageUrl());

        // Load the book cover
        loadBookCover(holder, book);

        // Handle RadioButton selection
        holder.radioButton.setChecked(holder.getAdapterPosition() == selectedPosition); // Highlight selected book
        holder.radioButton.setOnClickListener(v -> {
            selectedPosition = holder.getAdapterPosition(); // Update the selected position
            notifyDataSetChanged(); // Refresh the view to update the selected state
            if (listener != null) {
                listener.onBookClick(book);
            }
        });

        // Handle item clicks for book selection
        holder.itemView.setOnClickListener(v -> {
            selectedPosition = holder.getAdapterPosition();
            notifyDataSetChanged(); // Update RadioButton state
            if (listener != null) {
                listener.onBookClick(book);
            }
        });
    }


    private void loadBookCover(@NonNull BookViewHolder holder, @NonNull Book book) {
        // Check if the cover image exists locally
        File localCoverFile = new File(context.getFilesDir(), book.getId() + ".png");
        if (localCoverFile.exists()) {
            Log.d(TAG, "Loading cover from local storage for book: " + book.getTitle());
            holder.bookCoverImage.setImageBitmap(BitmapFactory.decodeFile(localCoverFile.getAbsolutePath()));
        } else if (book.hasBitmapCover()) {
            Log.d(TAG, "Loading cover from Bitmap for book: " + book.getTitle());
            holder.bookCoverImage.setImageBitmap(book.getCoverImageBitmap());
        } else if (book.hasUrlCover()) {
            Log.d(TAG, "Loading cover from URL for book: " + book.getTitle());
            Glide.with(context)
                    .load(book.getCoverImageUrl())
                    .placeholder(R.drawable.book_handle) // Placeholder image
                    .diskCacheStrategy(DiskCacheStrategy.ALL) // Cache for better performance
                    .listener(new RequestListener<Drawable>() {
                        @Override
                        public boolean onLoadFailed(@Nullable GlideException e, Object model, Target<Drawable> target, boolean isFirstResource) {
                            Log.e(TAG, "Error loading image for book: " + book.getTitle(), e);
                            return false;
                        }

                        @Override
                        public boolean onResourceReady(Drawable resource, Object model, Target<Drawable> target, DataSource dataSource, boolean isFirstResource) {
                            Log.d(TAG, "Image loaded for book: " + book.getTitle());
                            return false;
                        }
                    })
                    .into(holder.bookCoverImage);
        } else if (book.hasResIdCover()) {
            Log.d(TAG, "Loading cover from resource ID for book: " + book.getTitle());
            holder.bookCoverImage.setImageResource(book.getImageResId());
        } else {
            Log.d(TAG, "Loading default cover for book: " + book.getTitle());
            holder.bookCoverImage.setImageResource(R.drawable.book_handle); // Default fallback image
        }
    }

    @Override
    public int getItemCount() {
        return books != null ? books.size() : 0;
    }

    public static class BookViewHolder extends RecyclerView.ViewHolder {

        final ImageView bookCoverImage;
        final RadioButton radioButton;

        public BookViewHolder(@NonNull View itemView) {
            super(itemView);
            // Bind ImageView and RadioButton for book cover and selection
            bookCoverImage = itemView.findViewById(R.id.bookCoverImage);
            radioButton = itemView.findViewById(R.id.radioButton);

            if (bookCoverImage == null || radioButton == null) {
                throw new IllegalStateException("Required views not found in item_selection layout.");
            }
        }
    }

    // Interface to handle book selection
    public interface OnBookClickListener {
        void onBookClick(Book book); // Handle book click events
    }
}
