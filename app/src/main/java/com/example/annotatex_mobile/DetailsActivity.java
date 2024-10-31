// DetailsActivity.java
package com.example.annotatex_mobile;

import android.content.Intent;
import android.net.Uri;
import android.os.Bundle;
import android.util.Log;
import androidx.appcompat.app.AppCompatActivity;
import com.example.annotatex_mobile.databinding.ActivityDetailsBinding;
import com.pspdfkit.configuration.activity.PdfActivityConfiguration;
import com.pspdfkit.ui.PdfActivityIntentBuilder;

public class DetailsActivity extends AppCompatActivity {
    private static final String TAG = "DetailsActivity";
    private ActivityDetailsBinding binding;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        binding = ActivityDetailsBinding.inflate(getLayoutInflater());
        setContentView(binding.getRoot());

        Book book = (Book) getIntent().getSerializableExtra("book");

        if (book != null) {
            binding.mBookTitle.setText(book.getTitle());
            binding.mAuthorName.setText(book.getAuthor());
            binding.mBookDesc.setText(book.getDescription());

            // Use the Bitmap cover image if available
            if (book.hasBitmapCover()) {
                binding.mBookImage.setImageBitmap(book.getCoverImageBitmap());
            } else {
                binding.mBookImage.setImageResource(book.getImageResId());
            }

            // Set the click listener for the Read Book button
            binding.mReadBookBtn.setOnClickListener(v -> openPdf(book.getPdfUrl()));
        } else {
            Log.e(TAG, "Book data is missing");
        }
    }

    private void openPdf(String pdfUrl) {
        PdfActivityConfiguration configuration = new PdfActivityConfiguration.Builder(this)
                .theme(R.style.MyApp_PSPDFKitTheme)
                .enableAnnotationEditing()
                .disableOutline()
                .disableSearch()
                .build();

        Intent intent = PdfActivityIntentBuilder.fromUri(this, Uri.parse(pdfUrl))
                .configuration(configuration)
                .build();

        startActivity(intent);
    }
}
