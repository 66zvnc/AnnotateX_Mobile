package com.example.annotatex_mobile;

import android.content.Context;
import android.content.Intent;
import android.graphics.drawable.Drawable;
import android.os.Parcelable;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageView;
import android.util.Log;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.recyclerview.widget.RecyclerView;

import com.bumptech.glide.Glide;
import com.bumptech.glide.load.engine.DiskCacheStrategy;
import com.bumptech.glide.load.engine.GlideException;
import com.bumptech.glide.request.RequestOptions;
import com.bumptech.glide.load.resource.drawable.DrawableTransitionOptions;
import com.bumptech.glide.request.RequestListener;
import com.bumptech.glide.request.target.Target;
import com.bumptech.glide.load.DataSource;
import com.bumptech.glide.load.resource.drawable.DrawableResource;
import com.google.firebase.storage.FirebaseStorage;

import java.util.ArrayList;
import java.util.List;

public class CategoryBooksAdapter extends RecyclerView.Adapter<CategoryBooksAdapter.ViewHolder> {
    private static final String TAG = "CategoryBooksAdapter";
    private final Context context;
    private final List<Book> books;
    private List<Book> filteredBooks;

    public CategoryBooksAdapter(Context context) {
        this.context = context;
        this.books = new ArrayList<>();
        this.filteredBooks = new ArrayList<>();
    }

    @NonNull
    @Override
    public ViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        View view = LayoutInflater.from(context).inflate(R.layout.item_book, parent, false);
        return new ViewHolder(view);
    }

    @Override
    public void onBindViewHolder(@NonNull ViewHolder holder, int position) {
        Book book = filteredBooks.get(position);
        loadBookCover(book, holder.imageView);

        holder.itemView.setOnClickListener(v -> {
            Intent intent = new Intent(context, DetailsActivity.class);
            intent.putExtra("book", (Parcelable) book);
            context.startActivity(intent);
        });
    }

    private void loadBookCover(Book book, ImageView imageView) {
        RequestOptions requestOptions = new RequestOptions()
            .diskCacheStrategy(DiskCacheStrategy.ALL)
            .placeholder(R.drawable.book_handle)
            .error(R.drawable.book_handle)
            .centerCrop();

        String coverUrl = book.getCoverImageUrl();
        Log.d(TAG, "Loading cover for book: " + book.getTitle());
        Log.d(TAG, "Cover URL: " + coverUrl);

        if (coverUrl != null && !coverUrl.isEmpty()) {
            if (coverUrl.startsWith("gs://")) {
                // Handle Firebase Storage URLs
                FirebaseStorage.getInstance().getReferenceFromUrl(coverUrl)
                    .getDownloadUrl()
                    .addOnSuccessListener(uri -> {
                        Log.d(TAG, "Converted Storage URL: " + uri.toString());
                        Glide.with(context)
                            .load(uri)
                            .apply(requestOptions)
                            .transition(DrawableTransitionOptions.withCrossFade())
                            .into(imageView);
                    })
                    .addOnFailureListener(e -> {
                        Log.e(TAG, "Failed to load image from Storage: " + e.getMessage());
                        imageView.setImageResource(R.drawable.book_handle);
                    });
            } else {
                Glide.with(context)
                    .load(coverUrl)
                    .apply(requestOptions)
                    .transition(DrawableTransitionOptions.withCrossFade())
                    .listener(new RequestListener<Drawable>() {
                        @Override
                        public boolean onLoadFailed(@Nullable GlideException e, Object model,
                                                    Target<Drawable> target, boolean isFirstResource) {
                            Log.e(TAG, "Failed to load image: " + coverUrl, e);
                            return false;
                        }

                        @Override
                        public boolean onResourceReady(Drawable resource, Object model,
                                Target<Drawable> target, DataSource dataSource, boolean isFirstResource) {
                            Log.d(TAG, "Successfully loaded image: " + coverUrl);
                            return false;
                        }
                    })
                    .into(imageView);
            }
        } else if (book.hasResIdCover()) {
            Log.d(TAG, "Using resource ID: " + book.getImageResId());
            imageView.setImageResource(book.getImageResId());
        } else {
            Log.d(TAG, "No cover available, using default");
            imageView.setImageResource(R.drawable.book_handle);
        }
    }

    @Override
    public int getItemCount() {
        return filteredBooks.size();
    }

    public void updateBooks(List<Book> newBooks) {
        Log.d(TAG, "Updating books. New size: " + newBooks.size());
        books.clear();
        books.addAll(newBooks);
        filteredBooks.clear();
        filteredBooks.addAll(newBooks);
        notifyDataSetChanged();
    }

    public void filterBooks(String query) {
        filteredBooks.clear();
        if (query.isEmpty()) {
            filteredBooks.addAll(books);
        } else {
            String lowerCaseQuery = query.toLowerCase();
            for (Book book : books) {
                if (book.getTitle().toLowerCase().contains(lowerCaseQuery) ||
                    (book.getAuthor() != null && book.getAuthor().toLowerCase().contains(lowerCaseQuery))) {
                    filteredBooks.add(book);
                }
            }
        }
        Log.d(TAG, "Filtered books size: " + filteredBooks.size());
        notifyDataSetChanged();
    }

    static class ViewHolder extends RecyclerView.ViewHolder {
        ImageView imageView;

        ViewHolder(@NonNull View itemView) {
            super(itemView);
            imageView = itemView.findViewById(R.id.imageView);
        }
    }
} 