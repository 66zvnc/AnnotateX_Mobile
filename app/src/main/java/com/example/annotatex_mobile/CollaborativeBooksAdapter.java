package com.example.annotatex_mobile;

import android.content.Context;
import android.content.Intent;
import android.graphics.drawable.Drawable;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.MenuInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageView;
import android.widget.PopupMenu;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.recyclerview.widget.RecyclerView;

import com.bumptech.glide.Glide;
import com.bumptech.glide.request.RequestListener;
import com.bumptech.glide.request.target.Target;
import com.bumptech.glide.load.DataSource;
import com.bumptech.glide.load.engine.GlideException;
import com.google.firebase.storage.FirebaseStorage;

import java.util.ArrayList;
import java.util.List;

public class CollaborativeBooksAdapter extends RecyclerView.Adapter<CollaborativeBooksAdapter.ViewHolder> {

    private final Context context;
    private final List<Book> originalBooks; // Keeps the original list for filtering
    private final List<Book> books; // The list displayed in the RecyclerView
    private final OnBookInteractionListener listener;

    public CollaborativeBooksAdapter(Context context, List<Book> books, OnBookInteractionListener listener) {
        this.context = context;
        this.originalBooks = new ArrayList<>(books); // Initialize with a copy of the original list
        this.books = new ArrayList<>(books); // Initialize the displayed list
        this.listener = listener;
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
        String coverUrl = book.getCoverImageUrl();
        
        Log.d("CollaborativeBooksAdapter", "Binding book: " + book.getTitle());
        Log.d("CollaborativeBooksAdapter", "Cover URL from book: " + coverUrl);

        if (coverUrl != null && !coverUrl.isEmpty()) {
            // Check if it's a Firebase Storage URL
            if (coverUrl.startsWith("gs://")) {
                Log.d("CollaborativeBooksAdapter", "Converting Firebase Storage URL: " + coverUrl);
                FirebaseStorage.getInstance().getReferenceFromUrl(coverUrl)
                    .getDownloadUrl()
                    .addOnSuccessListener(uri -> {
                        Log.d("CollaborativeBooksAdapter", "Converted URL: " + uri.toString());
                        loadImageWithGlide(uri.toString(), holder.coverImageView, book);
                    })
                    .addOnFailureListener(e -> {
                        Log.e("CollaborativeBooksAdapter", "Failed to get download URL for " + book.getTitle(), e);
                        holder.coverImageView.setImageResource(R.drawable.book_handle);
                    });
            } else {
                Log.d("CollaborativeBooksAdapter", "Using direct URL: " + coverUrl);
                loadImageWithGlide(coverUrl, holder.coverImageView, book);
            }
        } else {
            Log.e("CollaborativeBooksAdapter", "No cover URL for book: " + book.getTitle());
            holder.coverImageView.setImageResource(R.drawable.book_handle);
        }

        // Set click listener to open book details
        holder.itemView.setOnClickListener(v -> {
            Intent intent = new Intent(context, DetailsActivity.class);
            intent.putExtra("book", book); // Assuming Book implements Serializable or Parcelable
            context.startActivity(intent);
        });

        // Set long-press listener to show the popup menu
        holder.itemView.setOnLongClickListener(v -> {
            showPopupMenu(v, book);
            return true;
        });
    }

    private void loadImageWithGlide(String imageUrl, ImageView imageView, Book book) {
        Glide.with(context)
            .load(imageUrl)
            .placeholder(R.drawable.book_handle)
            .error(R.drawable.book_handle)
            .centerCrop()
            .listener(new RequestListener<Drawable>() {
                @Override
                public boolean onLoadFailed(@Nullable GlideException e, Object model, 
                        Target<Drawable> target, boolean isFirstResource) {
                    Log.e("CollaborativeBooksAdapter", "Glide failed to load image for " + book.getTitle(), e);
                    return false;
                }

                @Override
                public boolean onResourceReady(Drawable resource, Object model, 
                        Target<Drawable> target, DataSource dataSource, boolean isFirstResource) {
                    Log.d("CollaborativeBooksAdapter", "Successfully loaded image for " + book.getTitle());
                    return false;
                }
            })
            .into(imageView);
    }

    @Override
    public int getItemCount() {
        return books.size();
    }

    // Method to show the popup menu on long press
    private void showPopupMenu(View view, Book book) {
        PopupMenu popupMenu = new PopupMenu(context, view);
        MenuInflater inflater = popupMenu.getMenuInflater();
        inflater.inflate(R.menu.collaborative_book_menu, popupMenu.getMenu());

        popupMenu.setOnMenuItemClickListener(item -> {
            if (listener == null) return false;

            // Handle menu item clicks
            if (item.getItemId() == R.id.menu_view_details) {
                listener.onViewDetails(book);
                return true;
            } else if (item.getItemId() == R.id.menu_stop_collab) {
                listener.onStopCollab(book);
                return true;
            } else {
                return false;
            }
        });

        popupMenu.show();
    }

    // Method to update the book list dynamically (e.g., for filtering)
    public void updateBooks(List<Book> newBooks) {
        books.clear();
        books.addAll(newBooks);
        notifyDataSetChanged();
    }

    // Method to reset the book list to the original list
    public void resetBooks() {
        books.clear();
        books.addAll(originalBooks);
        notifyDataSetChanged();
    }

    // Method to filter books based on a query
    public void filterBooks(String query) {
        if (query == null || query.trim().isEmpty()) {
            resetBooks(); // Show the original list if the query is empty
            return;
        }

        List<Book> filteredList = new ArrayList<>();
        for (Book book : originalBooks) {
            if (book.getTitle().toLowerCase().contains(query.toLowerCase())) {
                filteredList.add(book);
            }
        }
        updateBooks(filteredList); // Update the displayed list with the filtered results
    }

    static class ViewHolder extends RecyclerView.ViewHolder {
        ImageView coverImageView; // Displays the book cover

        ViewHolder(View itemView) {
            super(itemView);
            coverImageView = itemView.findViewById(R.id.bookCoverImageView);
        }
    }

    // Interface for handling interactions with books
    public interface OnBookInteractionListener {
        void onViewDetails(Book book);

        void onStopCollab(Book book);
    }
}
