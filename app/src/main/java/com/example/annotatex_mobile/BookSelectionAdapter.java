package com.example.annotatex_mobile;

import android.content.Context;
import android.graphics.BitmapFactory;
import android.graphics.drawable.Drawable;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageView;

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

    // Constructor
    public BookSelectionAdapter(Context context, List<Book> books, OnBookClickListener listener) {
        this.context = context;
        this.books = books;
        this.listener = listener;
    }

    @NonNull
    @Override
    public BookViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        // Inflate the item_book_selection layout
        View view = LayoutInflater.from(context).inflate(R.layout.item_book_selection, parent, false);
        return new BookViewHolder(view);
    }

    @Override
    public void onBindViewHolder(@NonNull BookViewHolder holder, int position) {
        Book book = books.get(position);

        // Debugging logs for validation
        Log.d(TAG, "Binding book: Title=" + book.getTitle() + ", Cover URL=" + book.getCoverImageUrl());

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
                            Log.e("GlideError", "Error loading image for book: " + book.getTitle(), e);
                            return false;
                        }

                        @Override
                        public boolean onResourceReady(Drawable resource, Object model, Target<Drawable> target, DataSource dataSource, boolean isFirstResource) {
                            Log.d("GlideSuccess", "Image loaded for book: " + book.getTitle());
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

        // Handle book selection
        holder.itemView.setOnClickListener(v -> {
            if (listener != null) {
                listener.onBookClick(book);
            }
        });
    }

    @Override
    public int getItemCount() {
        return books != null ? books.size() : 0;
    }

    public static class BookViewHolder extends RecyclerView.ViewHolder {

        final ImageView bookCoverImage;

        public BookViewHolder(@NonNull View itemView) {
            super(itemView);
            // Bind ImageView for book cover
            bookCoverImage = itemView.findViewById(R.id.bookCoverImage);

            if (bookCoverImage == null) {
                throw new IllegalStateException("ImageView with id 'bookCoverImage' not found in item_book_selection layout.");
            }
        }
    }

    // Interface to handle book selection
    public interface OnBookClickListener {
        void onBookClick(Book book); // Handle book click events
    }
}
