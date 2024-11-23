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

import java.util.Arrays;
import java.util.List;

public class LibraryAdapter extends RecyclerView.Adapter<LibraryAdapter.ViewHolder> {
    private static final String TAG = "LibraryAdapter";
    private final Context context;
    private final List<Book> bookList;
    private final OnPdfClickListener listener;
    private final boolean isInSelectionMode;
    private int selectedPosition = -1;

    public interface OnPdfClickListener {
        void onPdfClick(Book book);
        void onPdfClick(String pdfUrl);
    }

    public LibraryAdapter(Context context, List<Book> bookList, OnPdfClickListener listener, boolean isInSelectionMode) {
        this.context = context;
        this.bookList = bookList;
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
        Book book = bookList.get(position);

        // Load book cover
        if (book.hasBitmapCover()) {
            holder.imageView.setImageBitmap(book.getCoverImageBitmap());
        } else if (book.hasUrlCover()) {
            Glide.with(context).load(book.getCoverImageUrl()).into(holder.imageView);
        } else if (book.hasResIdCover()) {
            holder.imageView.setImageResource(book.getImageResId());
        }

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

        holder.itemView.setOnClickListener(v -> listener.onPdfClick(book));
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
            String currentUserId = FirebaseAuth.getInstance().getCurrentUser().getUid();

            if (currentUserId == null || friendId == null || book == null) {
                Toast.makeText(context, "Invalid data for collaboration", Toast.LENGTH_SHORT).show();
                return;
            }

            // Add collaborators to the book object
            book.setCollaborators(Arrays.asList(currentUserId, friendId));

            // Add the book to the current user's collaborativeBooks collection
            firestore.collection("users")
                    .document(currentUserId)
                    .collection("collaborativeBooks")
                    .document(book.getId())
                    .set(book)
                    .addOnSuccessListener(aVoid -> {
                        Log.d(TAG, "Book added to current user's collaborativeBooks");

                        // Add the book to the friend's collaborativeBooks collection
                        firestore.collection("users")
                                .document(friendId)
                                .collection("collaborativeBooks")
                                .document(book.getId())
                                .set(book)
                                .addOnSuccessListener(aVoid1 -> {
                                    Toast.makeText(context, "Book shared with " + friendName, Toast.LENGTH_SHORT).show();
                                })
                                .addOnFailureListener(e -> {
                                    Log.e(TAG, "Failed to add book to friend's collaborativeBooks", e);
                                    Toast.makeText(context, "Failed to share book with " + friendName, Toast.LENGTH_SHORT).show();
                                });
                    })
                    .addOnFailureListener(e -> {
                        Log.e(TAG, "Failed to add book to current user's collaborativeBooks", e);
                        Toast.makeText(context, "Failed to collaborate on book", Toast.LENGTH_SHORT).show();
                    });
        });
    }


    private void markBookAsHidden(Book book) {
        book.setHidden(true);
        Log.d(TAG, "Marked as 'Don't Suggest': " + book.getTitle());
        Toast.makeText(context, "Marked as 'Don't Suggest'", Toast.LENGTH_SHORT).show();

        int position = bookList.indexOf(book);
        if (position != -1) {
            bookList.remove(position);
            notifyItemRemoved(position);
        }
    }

    private void deleteBook(Book book) {
        FirebaseStorage storage = FirebaseStorage.getInstance();
        StorageReference pdfRef = storage.getReferenceFromUrl(book.getPdfUrl());

        pdfRef.delete().addOnSuccessListener(aVoid -> {
            Log.d(TAG, "Successfully deleted from Firebase Storage");

            FirebaseFirestore firestore = FirebaseFirestore.getInstance();
            firestore.collection("books")
                    .whereEqualTo("pdfUrl", book.getPdfUrl())
                    .get()
                    .addOnCompleteListener(task -> {
                        if (task.isSuccessful() && !task.getResult().isEmpty()) {
                            String docId = task.getResult().getDocuments().get(0).getId();
                            firestore.collection("books").document(docId).delete()
                                    .addOnSuccessListener(aVoid1 -> Log.d(TAG, "Successfully deleted from Firestore"))
                                    .addOnFailureListener(e -> Log.e(TAG, "Failed to delete from Firestore", e));
                        } else {
                            Log.e(TAG, "No document found in Firestore matching the pdfUrl");
                        }
                    });

            int position = bookList.indexOf(book);
            if (position != -1) {
                bookList.remove(position);
                notifyItemRemoved(position);
            }

        }).addOnFailureListener(e -> Log.e(TAG, "Failed to delete from Firebase Storage", e));
    }

    @Override
    public int getItemCount() {
        return bookList.size();
    }

    public static class ViewHolder extends RecyclerView.ViewHolder {
        ImageView imageView;
        ImageView menuIcon;
        RadioButton radioButton;

        public ViewHolder(@NonNull View itemView) {
            super(itemView);
            imageView = itemView.findViewById(R.id.imageView);
            menuIcon = itemView.findViewById(R.id.menu_icon);
            radioButton = itemView.findViewById(R.id.radioButton);
        }
    }
}
