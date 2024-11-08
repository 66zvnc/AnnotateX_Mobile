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
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;
import com.bumptech.glide.Glide;
import com.google.firebase.firestore.FirebaseFirestore;
import com.google.firebase.storage.FirebaseStorage;
import com.google.firebase.storage.StorageReference;

import java.util.List;

public class PdfGalleryAdapter extends RecyclerView.Adapter<PdfGalleryAdapter.ViewHolder> {
    private static final String TAG = "PdfGalleryAdapter";
    private final Context context;
    private final List<Book> bookList;
    private final OnPdfClickListener listener;

    public interface OnPdfClickListener {
        void onPdfClick(Book book);
        void onPdfClick(String pdfUrl);
    }

    public PdfGalleryAdapter(Context context, List<Book> bookList, OnPdfClickListener listener) {
        this.context = context;
        this.bookList = bookList;
        this.listener = listener;
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

        holder.itemView.setOnClickListener(v -> listener.onPdfClick(book));
        holder.menuIcon.setOnClickListener(v -> showPopupMenu(v, book));
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
            listener.onPdfClick(book.getPdfUrl());
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

    private void markBookAsHidden(Book book) {
        book.setHidden(true); // Mark the book as hidden
        Log.d(TAG, "Marked as 'Don't Suggest': " + book.getTitle());
        Toast.makeText(context, "Marked as 'Don't Suggest'", Toast.LENGTH_SHORT).show();

        // Update the book list and notify adapter
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

            // Delete from Firestore
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

        public ViewHolder(@NonNull View itemView) {
            super(itemView);
            imageView = itemView.findViewById(R.id.imageView);
            menuIcon = itemView.findViewById(R.id.menu_icon);
        }
    }
}
