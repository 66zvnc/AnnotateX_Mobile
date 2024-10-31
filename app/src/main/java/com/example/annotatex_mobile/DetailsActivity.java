// DetailsActivity.java
package com.example.annotatex_mobile;

import android.content.Intent;
import android.os.Bundle;
import android.util.Log;
import androidx.appcompat.app.AppCompatActivity;
import com.example.annotatex_mobile.databinding.ActivityDetailsBinding;

public class DetailsActivity extends AppCompatActivity {

    private static final String TAG = "DetailsActivity";
    private ActivityDetailsBinding binding;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        binding = ActivityDetailsBinding.inflate(getLayoutInflater());
        setContentView(binding.getRoot());

        // Get book details from Intent
        Book book = (Book) getIntent().getSerializableExtra("book");

        if (book != null) {
            // Set book details to views
            binding.mBookTitle.setText(book.getTitle());
            binding.mAuthorName.setText(book.getAuthor());
            binding.mBookDesc.setText(book.getDescription());
            binding.mBookImage.setImageResource(book.getImageResId());

            binding.mReadBookBtn.setOnClickListener(v -> {
                Intent intent = new Intent(DetailsActivity.this, PdfViewerFragment.class);
                intent.putExtra("pdfUrl", book.getPdfUrl());
                startActivity(intent);
            });
        } else {
            Log.e(TAG, "Book data is missing");
        }
    }
}
