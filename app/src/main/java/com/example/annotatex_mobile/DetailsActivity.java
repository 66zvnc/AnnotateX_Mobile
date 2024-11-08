package com.example.annotatex_mobile;

import android.content.Intent;
import android.content.res.Resources;
import android.net.Uri;
import android.os.Bundle;
import android.util.Log;
import android.view.Menu;
import android.view.MenuItem;
import android.widget.ImageView;
import android.widget.Toast;
import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;
import com.example.annotatex_mobile.databinding.ActivityDetailsBinding;
import com.google.firebase.firestore.CollectionReference;
import com.google.firebase.firestore.FirebaseFirestore;
import com.google.firebase.storage.FirebaseStorage;
import com.google.firebase.storage.StorageReference;
import com.pspdfkit.configuration.activity.PdfActivityConfiguration;
import com.pspdfkit.document.PdfDocument;
import com.pspdfkit.ui.PdfActivityIntentBuilder;
import com.pspdfkit.annotations.Annotation;
import com.pspdfkit.annotations.AnnotationType;

import java.io.File;
import java.io.IOException;
import java.util.ArrayList;
import java.util.EnumSet;
import java.util.List;

public class DetailsActivity extends AppCompatActivity {
    private static final String TAG = "DetailsActivity";
    private ActivityDetailsBinding binding;
    private PdfDocument pdfDocument;
    private File annotationsFile;
    private CollectionReference annotationsCollection;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        binding = ActivityDetailsBinding.inflate(getLayoutInflater());
        setContentView(binding.getRoot());

        annotationsFile = new File(getFilesDir(), "annotations.json");

        // Get the Book object passed from the previous Activity
        Book book = (Book) getIntent().getSerializableExtra("book");

        if (book != null) {
            // Initialize Firestore collection reference for annotations
            annotationsCollection = FirebaseFirestore.getInstance()
                    .collection("books")
                    .document(book.getId())
                    .collection("annotations");

            binding.mBookTitle.setText(book.getTitle());
            binding.mAuthorName.setText(book.getAuthor());
            binding.mBookDesc.setText(book.getDescription());

            if (book.hasBitmapCover()) {
                binding.mBookImage.setImageBitmap(book.getCoverImageBitmap());
            } else if (book.hasUrlCover()) {
                // Load image using Glide if needed
            } else {
                try {
                    binding.mBookImage.setImageResource(book.getImageResId());
                } catch (Resources.NotFoundException e) {
                    Log.e(TAG, "Image resource not found", e);
                    binding.mBookImage.setImageResource(R.drawable.default_cover);
                }
            }

            binding.mReadBookBtn.setOnClickListener(v -> {
                if (book.getPdfUrl() != null && !book.getPdfUrl().isEmpty()) {
                    downloadAndOpenPdf(book.getPdfUrl());
                } else {
                    Toast.makeText(this, "Invalid PDF URL", Toast.LENGTH_SHORT).show();
                }
            });
        } else {
            Log.e(TAG, "Book data is missing");
        }

        // Back button functionality
        ImageView goBackButton = findViewById(R.id.goBackButton);
        goBackButton.setOnClickListener(v -> {
            // Navigate back to the library
            Intent intent = new Intent(DetailsActivity.this, MainActivity.class);
            intent.addFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP | Intent.FLAG_ACTIVITY_NEW_TASK);
            startActivity(intent);
            finish();
        });
    }

    private void downloadAndOpenPdf(String pdfUrl) {
        FirebaseStorage storage = FirebaseStorage.getInstance();
        StorageReference pdfRef = storage.getReferenceFromUrl(pdfUrl);

        try {
            File localFile = File.createTempFile("tempPdf", ".pdf", getCacheDir());

            pdfRef.getFile(localFile).addOnSuccessListener(taskSnapshot -> {
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
                .hideDocumentTitleOverlay()
                .disableContentEditing()
                .hideThumbnailGrid()
                .disableDocumentEditor()
                .build();

        Intent intent = PdfActivityIntentBuilder.fromUri(this, fileUri)
                .configuration(configuration)
                .build();

        startActivity(intent);
    }
}
