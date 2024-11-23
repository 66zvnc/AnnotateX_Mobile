package com.example.annotatex_mobile;

import android.content.Context;
import android.view.LayoutInflater;
import android.view.MenuInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageView;
import android.widget.PopupMenu;

import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;

import com.bumptech.glide.Glide;

import java.util.List;

public class CollaborativeBooksAdapter extends RecyclerView.Adapter<CollaborativeBooksAdapter.ViewHolder> {

    private final Context context;
    private final List<Book> books;
    private final OnBookInteractionListener listener;

    public CollaborativeBooksAdapter(Context context, List<Book> books, OnBookInteractionListener listener) {
        this.context = context;
        this.books = books;
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

        // Load the book cover image using Glide or set a default image
        if (book.hasUrlCover()) {
            Glide.with(context).load(book.getCoverImageUrl()).into(holder.coverImageView);
        } else {
            holder.coverImageView.setImageResource(R.drawable.book_handle);
        }

        // Set long-press listener to show the popup menu
        holder.itemView.setOnLongClickListener(v -> {
            showPopupMenu(v, book);
            return true;
        });
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
