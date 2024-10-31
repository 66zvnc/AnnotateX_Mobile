// DetailsActivity.java
package com.example.annotatex_mobile;

import android.content.Intent;
import android.net.Uri;
import android.os.Bundle;
import android.util.Log;
import android.widget.Toast;
import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;
import com.example.annotatex_mobile.databinding.ActivityDetailsBinding;
import com.google.firebase.storage.FirebaseStorage;
import com.google.firebase.storage.StorageReference;
import com.pspdfkit.configuration.activity.PdfActivityConfiguration;
import com.pspdfkit.ui.PdfActivityIntentBuilder;

import java.io.File;
import java.io.IOException;

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
            binding.mReadBookBtn.setOnClickListener(v -> downloadAndOpenPdf(book.getPdfUrl()));
        } else {
            Log.e(TAG, "Book data is missing");
        }
    }

    private void downloadAndOpenPdf(String pdfUrl) {
        FirebaseStorage storage = FirebaseStorage.getInstance();
        StorageReference pdfRef = storage.getReferenceFromUrl(pdfUrl);

        try {
            // Create a temporary file in the app's cache directory
            File localFile = File.createTempFile("tempPdf", ".pdf", getCacheDir());

            // Download the file to the temporary location
            pdfRef.getFile(localFile).addOnSuccessListener(taskSnapshot -> {
                // After downloading, open the file with PSPDFKit
                openPdfWithPSPDFKit(Uri.fromFile(localFile));
            }).addOnFailureListener(e -> {
                Log.e(TAG, "Failed to download PDF", e);
                Toast.makeText(this, "Failed to open PDF", Toast.LENGTH_SHORT).show();
            });
        } catch (IOException e) {
            Log.e(TAG, "Error creating temp file", e);
        }
    }

    private void openPdfWithPSPDFKit(@NonNull Uri fileUri) {
        PdfActivityConfiguration configuration = new PdfActivityConfiguration.Builder(this)
                .theme(R.style.MyApp_PSPDFKitTheme)
                .enableAnnotationEditing()
                .disableOutline()
                .disableSearch()
                .build();

        Intent intent = PdfActivityIntentBuilder.fromUri(this, fileUri)
                .configuration(configuration)
                .build();

        startActivity(intent);
    }
}
