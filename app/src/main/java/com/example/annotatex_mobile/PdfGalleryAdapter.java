// PdfGalleryAdapter.java
package com.example.annotatex_mobile;

import android.content.Context;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageView;
import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;
import java.util.List;

public class PdfGalleryAdapter extends RecyclerView.Adapter<PdfGalleryAdapter.ViewHolder> {
    private Context context;
    private List<Book> bookList;
    private OnPdfClickListener listener;

    public interface OnPdfClickListener {
        void onPdfClick(Book book);  // Pass the Book object

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
        holder.imageView.setImageResource(book.getImageResId());
        holder.itemView.setOnClickListener(v -> listener.onPdfClick(book));
    }

    @Override
    public int getItemCount() {
        return bookList.size();
    }

    public static class ViewHolder extends RecyclerView.ViewHolder {
        ImageView imageView;

        public ViewHolder(@NonNull View itemView) {
            super(itemView);
            imageView = itemView.findViewById(R.id.imageView);
        }
    }
}
