package com.example.annotatex_mobile;

import android.content.Context;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.MenuInflater;
import android.view.MenuItem;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageView;
import android.widget.PopupMenu;
import android.widget.RadioButton;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.appcompat.app.AlertDialog;
import androidx.recyclerview.widget.RecyclerView;

import com.bumptech.glide.Glide;
import com.bumptech.glide.load.resource.drawable.DrawableTransitionOptions;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.firestore.FirebaseFirestore;
import com.google.firebase.storage.FirebaseStorage;
import com.google.firebase.storage.StorageReference;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

public class LibraryAdapter extends RecyclerView.Adapter<LibraryAdapter.ViewHolder> {
    private static final String TAG = "LibraryAdapter";
    private final Context context;
    private final List<Book> bookList; // Original list
    private List<Book> filteredList; // For filtered results
    private final OnPdfClickListener listener;
    private final boolean isInSelectionMode;
    private int selectedPosition = -1;

    public void updateBooks(List<Book> updatedBooks) {
        Log.d(TAG, "Updating books in adapter. Initial size: " + updatedBooks.size());
        bookList.clear(); // Clear the original list
        List<Book> allBooks = new ArrayList<>(updatedBooks);

        // Add preloaded books
        addPreloadedBooks(allBooks);

        // Remove duplicates
        bookList.addAll(removeDuplicateBooks(allBooks)); // Add unique books
        resetBooks(); // Reset filteredList to match bookList
        Log.d(TAG, "Books updated. Final size: " + bookList.size());
    }

    private void addPreloadedBooks(List<Book> allBooks) {

        bookList.add(new Book(R.drawable.book_1, "url_to_pdf_1", "Rich Dad Poor Dad", "Robert T. Kiyosaki", "Financial wisdom from the rich."));
        bookList.add(new Book(R.drawable.book_2, "url_to_pdf_2", "Atomic Habits", "James Clear", "Build good habits, break bad ones."));
        bookList.add(new Book(R.drawable.book_3, "url_to_pdf_3", "Best Self", "Mike Bayer", "Be you, only better."));
        bookList.add(new Book(R.drawable.book_4, "url_to_pdf_4", "How to Be Fine", "Kristen Meinzer", "Lessons from self-help books."));
        bookList.add(new Book(R.drawable.book_5, "url_to_pdf_5", "Out of the Box", "Suzanne Dudley", "A journey of emotional resilience."));
    }

    /**
     * Removes duplicate books from the list, prioritizing collaborative books.
     * @param books The list of books to process.
     * @return A list with duplicates removed.
     */
    private List<Book> removeDuplicateBooks(List<Book> books) {
        Set<String> seenIds = new HashSet<>();
        List<Book> uniqueBooks = new ArrayList<>();

        for (Book book : books) {
            String id = book.getId();

            // Skip books with null IDs
            if (id == null) {
                Log.w(TAG, "Skipping book with null ID: " + book.getTitle());
                continue;
            }

            // If the book is not already added, add it to the unique list
            if (!seenIds.contains(id)) {
                uniqueBooks.add(book);
                seenIds.add(id);
            } else {
                // If the book is already added, prioritize collaborative version
                for (int i = 0; i < uniqueBooks.size(); i++) {
                    if (uniqueBooks.get(i).getId() != null && uniqueBooks.get(i).getId().equals(id) && book.getCollaborators() != null) {
                        uniqueBooks.set(i, book); // Replace with collaborative version
                        break;
                    }
                }
            }
        }

        return uniqueBooks;
    }

    public void resetBooks() {
        Log.d(TAG, "Resetting to original book list. Size: " + bookList.size());
        filteredList = new ArrayList<>(bookList); // Reset to the full book list
        notifyDataSetChanged();
    }

    public interface OnPdfClickListener {
        void onPdfClick(Book book);
        void onPdfClick(String pdfUrl);
    }

    public LibraryAdapter(Context context, List<Book> bookList, OnPdfClickListener listener, boolean isInSelectionMode) {
        this.context = context;
        this.bookList = bookList != null ? new ArrayList<>(bookList) : new ArrayList<>(); // Ensure non-null and preserve the original list
        this.filteredList = new ArrayList<>(this.bookList); // Start with the full book list
        this.listener = listener;
        this.isInSelectionMode = isInSelectionMode;
    }

    @NonNull
    @Override
    public ViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        View view = LayoutInflater.from(context).inflate(R.layout.item_book, parent, false);
        return new ViewHolder(view);
    }

    @Override
    public void onBindViewHolder(@NonNull ViewHolder holder, int position) {
        Book book = filteredList.get(position);
        
        // Improved cover image loading logic with better logging
        if (book.getCoverImageUrl() != null && !book.getCoverImageUrl().isEmpty()) {
            Log.d(TAG, "Loading cover URL for " + book.getTitle() + ": " + book.getCoverImageUrl());
            
            if (book.getCoverImageUrl().startsWith("gs://") || book.getCoverImageUrl().startsWith("https://firebasestorage.googleapis.com")) {
                // Handle Firebase Storage URLs
                StorageReference storageRef = FirebaseStorage.getInstance()
                    .getReferenceFromUrl(book.getCoverImageUrl());
                    
                storageRef.getDownloadUrl()
                    .addOnSuccessListener(uri -> {
                        Log.d(TAG, "Resolved download URL: " + uri.toString());
                        Glide.with(context)
                            .load(uri)
                            .placeholder(R.drawable.book_handle)
                            .error(R.drawable.book_handle)
                            .transition(DrawableTransitionOptions.withCrossFade())
                            .into(holder.imageView);
                    })
                    .addOnFailureListener(e -> {
                        Log.e(TAG, "Failed to get download URL for " + book.getTitle(), e);
                        holder.imageView.setImageResource(R.drawable.book_handle);
                    });
            } else {
                // Direct HTTP URL
                Log.d(TAG, "Loading direct URL: " + book.getCoverImageUrl());
                Glide.with(context)
                    .load(book.getCoverImageUrl())
                    .placeholder(R.drawable.book_handle)
                    .error(R.drawable.book_handle)
                    .transition(DrawableTransitionOptions.withCrossFade())
                    .into(holder.imageView);
            }
        } else if (book.hasResIdCover()) {
            // Load resource ID cover
            Log.d(TAG, "Loading resource cover for " + book.getTitle());
            holder.imageView.setImageResource(book.getImageResId());
        } else {
            // Fallback to default cover
            Log.d(TAG, "Using default cover for " + book.getTitle());
            holder.imageView.setImageResource(R.drawable.book_handle);
        }

        // Configure view based on mode
        if (isInSelectionMode) {
            holder.itemView.setOnClickListener(v -> {
                if (listener != null) {
                    listener.onPdfClick(book);
                }
            });
        } else {
            holder.itemView.setOnLongClickListener(v -> {
                showPopupMenu(v, book);
                return true;
            });
            holder.itemView.setOnClickListener(v -> listener.onPdfClick(book));
        }
    }

    private void showPopupMenu(View view, Book book) {
        PopupMenu popupMenu = new PopupMenu(context, view);
        MenuInflater inflater = popupMenu.getMenuInflater();
        inflater.inflate(R.menu.item_book_menu, popupMenu.getMenu());

        MenuItem deleteItem = popupMenu.getMenu().findItem(R.id.menu_delete);
        deleteItem.setTitle(book.isPreloaded() ? "Don't Suggest" : "Delete");

        popupMenu.setOnMenuItemClickListener(item -> onMenuItemClick(item, book));
        popupMenu.show();
    }

    private boolean onMenuItemClick(MenuItem item, Book book) {
        int itemId = item.getItemId();

        if (itemId == R.id.menu_view_details) {
            listener.onPdfClick(book);
            return true;
        } else if (itemId == R.id.menu_share) {
            showFriendSelectionDialog(book);
            return true;
        } else if (itemId == R.id.menu_delete) {
            if (book.isPreloaded()) {
                markBookAsHidden(book);
            } else {
                deleteBook(book);
            }
            return true;
        }
        return false;
    }

    private void showFriendSelectionDialog(Book book) {
        // Pass an empty list for preselected friends since this method does not use them
        new FriendSelectionDialog(context, new ArrayList<>()).show(selectedFriends -> {
            FirebaseFirestore firestore = FirebaseFirestore.getInstance();
            String currentUserId = FirebaseAuth.getInstance().getUid();

            if (currentUserId == null || selectedFriends == null || selectedFriends.isEmpty() || book == null) {
                Toast.makeText(context, "Invalid data for collaboration", Toast.LENGTH_SHORT).show();
                return;
            }

            // Add the current user to the collaborators list
            List<String> collaborators = new ArrayList<>(selectedFriends.keySet());
            if (!collaborators.contains(currentUserId)) {
                collaborators.add(currentUserId); // Ensure the current user is included
            }

            // Update the book's collaborators list for this instance
            book.setCollaborators(collaborators);

            // Save the book for each collaborator
            for (String friendId : collaborators) {
                firestore.collection("users")
                        .document(friendId)
                        .collection("collaborativeBooks")
                        .document(book.getId())
                        .set(book)
                        .addOnSuccessListener(aVoid -> Log.d("Collaboration", "Book shared with user: " + friendId))
                        .addOnFailureListener(e -> Log.e("Collaboration", "Failed to share book with user: " + friendId, e));
            }

            Toast.makeText(context, "Book shared successfully with selected collaborators!", Toast.LENGTH_SHORT).show();
        });
    }

    private void markBookAsHidden(Book book) {
        book.setHidden(true);
        int position = filteredList.indexOf(book);
        if (position != -1) {
            filteredList.remove(position);
            notifyItemRemoved(position);
        }
    }

    private void deleteBook(Book book) {
        new AlertDialog.Builder(context)
            .setTitle("Delete Book")
            .setMessage("Are you sure you want to delete this book? This action cannot be undone.")
            .setPositiveButton("Delete", (dialog, which) -> {
                StorageReference pdfRef = FirebaseStorage.getInstance().getReferenceFromUrl(book.getPdfUrl());
                pdfRef.delete()
                    .addOnSuccessListener(aVoid -> FirebaseFirestore.getInstance()
                        .collection("books")
                        .whereEqualTo("id", book.getId())
                        .get()
                        .addOnCompleteListener(task -> {
                            if (task.isSuccessful() && !task.getResult().isEmpty()) {
                                String docId = task.getResult().getDocuments().get(0).getId();
                                FirebaseFirestore.getInstance().collection("books").document(docId).delete()
                                    .addOnSuccessListener(aVoid1 -> {
                                        // Find positions in both lists before removing
                                        int filteredPosition = filteredList.indexOf(book);
                                        int originalPosition = bookList.indexOf(book);
                                        
                                        // Remove from both lists
                                        if (filteredPosition != -1) {
                                            filteredList.remove(filteredPosition);
                                            notifyItemRemoved(filteredPosition);
                                            // Notify adapter of changes in item positions after removal
                                            if (filteredPosition < filteredList.size()) {
                                                notifyItemRangeChanged(filteredPosition, filteredList.size() - filteredPosition);
                                            }
                                        }
                                        
                                        if (originalPosition != -1) {
                                            bookList.remove(originalPosition);
                                        }
                                        
                                        Toast.makeText(context, "Book deleted successfully", Toast.LENGTH_SHORT).show();
                                        Log.d(TAG, "Book deleted successfully.");
                                    })
                                    .addOnFailureListener(e -> {
                                        Toast.makeText(context, "Failed to delete book", Toast.LENGTH_SHORT).show();
                                        Log.e(TAG, "Failed to delete document from Firestore", e);
                                    });
                            }
                        }))
                    .addOnFailureListener(e -> {
                        Toast.makeText(context, "Failed to delete book file", Toast.LENGTH_SHORT).show();
                        Log.e(TAG, "Failed to delete file from Storage", e);
                    });
            })
            .setNegativeButton("Cancel", null)
            .show();
    }


    @Override
    public int getItemCount() {
        return filteredList.size();
    }

    public static class ViewHolder extends RecyclerView.ViewHolder {
        ImageView imageView;

        public ViewHolder(@NonNull View itemView) {
            super(itemView);
            imageView = itemView.findViewById(R.id.imageView);
        }
    }

}
