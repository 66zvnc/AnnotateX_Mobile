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
import androidx.recyclerview.widget.RecyclerView;

import com.bumptech.glide.Glide;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.firestore.FirebaseFirestore;
import com.google.firebase.storage.FirebaseStorage;
import com.google.firebase.storage.StorageReference;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

public class LibraryAdapter extends RecyclerView.Adapter<LibraryAdapter.ViewHolder> {
    private static final String TAG = "LibraryAdapter";
    private final Context context;
    private final List<Book> bookList; // Original list
    private List<Book> filteredList; // For filtered results
    private final OnPdfClickListener listener;
    private final boolean isInSelectionMode;
    private int selectedPosition = -1;

    /**
     * Updates the adapter with a new list of books, ensuring proper updates.
     * @param updatedBooks The new list of books to display.
     */
    public void updateBooks(List<Book> updatedBooks) {
        Log.d(TAG, "Updating books in adapter. New size: " + updatedBooks.size());
        bookList.clear(); // Clear the original list
        bookList.addAll(updatedBooks); // Add the updated list
        resetBooks(); // Reset filteredList to match bookList
    }

    /**
     * Resets the adapter to the original list of books.
     */
    public void resetBooks() {
        Log.d(TAG, "Resetting to original book list. Size: " + bookList.size());
        filteredList = new ArrayList<>(bookList); // Reset to the full book list
        notifyDataSetChanged();
    }

    /**
     * Listener interface for handling PDF clicks.
     */
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

        // Load book cover
        if (book.hasBitmapCover()) {
            holder.imageView.setImageBitmap(book.getCoverImageBitmap());
        } else if (book.hasUrlCover()) {
            Glide.with(context).load(book.getCoverImageUrl()).into(holder.imageView);
        } else if (book.hasResIdCover()) {
            holder.imageView.setImageResource(book.getImageResId());
        }

        // Configure collaborator profile picture
        if (book.getCollaborators() != null && !book.getCollaborators().isEmpty()) {
            String collaboratorId = getCollaboratorId(book);
            if (collaboratorId != null) {
                holder.collaboratorImageView.setVisibility(View.VISIBLE);
                loadCollaboratorProfilePicture(collaboratorId, holder.collaboratorImageView);
            } else {
                holder.collaboratorImageView.setVisibility(View.GONE);
            }
        } else {
            holder.collaboratorImageView.setVisibility(View.GONE);
        }

        // Configure selection or menu mode
        if (isInSelectionMode) {
            holder.menuIcon.setVisibility(View.GONE);
            holder.radioButton.setVisibility(View.VISIBLE);
            holder.radioButton.setChecked(position == selectedPosition);
            holder.radioButton.setOnClickListener(v -> {
                selectedPosition = holder.getAdapterPosition();
                notifyDataSetChanged();
                if (listener != null) {
                    listener.onPdfClick(book);
                }
            });
        } else {
            holder.menuIcon.setVisibility(View.VISIBLE);
            holder.radioButton.setVisibility(View.GONE);
            holder.menuIcon.setOnClickListener(v -> showPopupMenu(v, book));
        }

        // Handle item click
        holder.itemView.setOnClickListener(v -> listener.onPdfClick(book));
    }

    private void loadCollaboratorProfilePicture(String collaboratorId, ImageView imageView) {
        FirebaseFirestore.getInstance().collection("users").document(collaboratorId)
                .get()
                .addOnSuccessListener(documentSnapshot -> {
                    if (documentSnapshot.exists()) {
                        String profilePictureUrl = documentSnapshot.getString("profileImageUrl");
                        if (profilePictureUrl != null && !profilePictureUrl.isEmpty()) {
                            Glide.with(context)
                                    .load(profilePictureUrl)
                                    .placeholder(R.drawable.ic_default_profile)
                                    .error(R.drawable.ic_default_profile)
                                    .circleCrop()
                                    .into(imageView);
                        } else {
                            imageView.setImageResource(R.drawable.ic_default_profile);
                        }
                    } else {
                        imageView.setImageResource(R.drawable.ic_default_profile);
                    }
                })
                .addOnFailureListener(e -> {
                    Log.e(TAG, "Failed to load collaborator profile picture", e);
                    imageView.setImageResource(R.drawable.ic_default_profile);
                });
    }

    private String getCollaboratorId(Book book) {
        String currentUserId = FirebaseAuth.getInstance().getUid();
        for (String collaborator : book.getCollaborators()) {
            if (!collaborator.equals(currentUserId)) {
                return collaborator;
            }
        }
        return null;
    }

    private void showPopupMenu(View view, Book book) {
        PopupMenu popupMenu = new PopupMenu(context, view);
        MenuInflater inflater = popupMenu.getMenuInflater();
        inflater.inflate(R.menu.item_book_menu, popupMenu.getMenu());

        MenuItem deleteItem = popupMenu.getMenu().findItem(R.id.menu_delete);
        if (book.isPreloaded()) {
            deleteItem.setTitle("Don't Suggest");
        } else {
            deleteItem.setTitle("Delete");
        }

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
        } else {
            return false;
        }
    }

    private void showFriendSelectionDialog(Book book) {
        new FriendSelectionDialog(context).show((friendId, friendName) -> {
            FirebaseFirestore firestore = FirebaseFirestore.getInstance();
            String currentUserId = FirebaseAuth.getInstance().getUid();

            if (currentUserId == null || friendId == null || book == null) {
                Toast.makeText(context, "Invalid data for collaboration", Toast.LENGTH_SHORT).show();
                return;
            }

            book.setCollaborators(Arrays.asList(currentUserId, friendId));
            firestore.collection("users")
                    .document(currentUserId)
                    .collection("collaborativeBooks")
                    .document(book.getId())
                    .set(book)
                    .addOnSuccessListener(aVoid -> firestore.collection("users")
                            .document(friendId)
                            .collection("collaborativeBooks")
                            .document(book.getId())
                            .set(book)
                            .addOnSuccessListener(aVoid1 -> Toast.makeText(context, "Book shared with " + friendName, Toast.LENGTH_SHORT).show())
                            .addOnFailureListener(e -> Toast.makeText(context, "Failed to share book with " + friendName, Toast.LENGTH_SHORT).show()))
                    .addOnFailureListener(e -> Toast.makeText(context, "Failed to collaborate on book", Toast.LENGTH_SHORT).show());
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
        StorageReference pdfRef = FirebaseStorage.getInstance().getReferenceFromUrl(book.getPdfUrl());
        pdfRef.delete()
                .addOnSuccessListener(aVoid -> FirebaseFirestore.getInstance().collection("books")
                        .whereEqualTo("pdfUrl", book.getPdfUrl())
                        .get()
                        .addOnCompleteListener(task -> {
                            if (task.isSuccessful() && !task.getResult().isEmpty()) {
                                String docId = task.getResult().getDocuments().get(0).getId();
                                FirebaseFirestore.getInstance().collection("books").document(docId).delete();
                            }
                            int position = filteredList.indexOf(book);
                            if (position != -1) {
                                filteredList.remove(position);
                                notifyItemRemoved(position);
                            }
                        }))
                .addOnFailureListener(e -> Log.e(TAG, "Failed to delete book", e));
    }

    @Override
    public int getItemCount() {
        return filteredList.size();
    }

    public static class ViewHolder extends RecyclerView.ViewHolder {
        ImageView imageView;
        ImageView menuIcon;
        RadioButton radioButton;
        ImageView collaboratorImageView;

        public ViewHolder(@NonNull View itemView) {
            super(itemView);
            imageView = itemView.findViewById(R.id.imageView);
            menuIcon = itemView.findViewById(R.id.menu_icon);
            radioButton = itemView.findViewById(R.id.radioButton);
            collaboratorImageView = itemView.findViewById(R.id.collaboratorProfilePicture);
        }
    }
}
