package com.example.annotatex_mobile;

import android.app.Dialog;
import android.content.Intent;
import android.content.res.Resources;
import android.net.Uri;
import android.os.Bundle;
import android.util.Log;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.widget.Button;
import android.widget.ImageView;
import android.widget.RatingBar;
import android.widget.TextView;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;

import com.bumptech.glide.Glide;
import com.example.annotatex_mobile.databinding.ActivityDetailsBinding;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.firestore.CollectionReference;
import com.google.firebase.firestore.DocumentSnapshot;
import com.google.firebase.firestore.FirebaseFirestore;
import com.google.firebase.storage.FirebaseStorage;
import com.google.firebase.storage.StorageReference;
import com.pspdfkit.configuration.activity.PdfActivityConfiguration;
import com.pspdfkit.document.PdfDocument;
import com.pspdfkit.ui.PdfActivityIntentBuilder;

import java.io.File;
import java.io.IOException;
import java.util.HashMap;
import java.util.Map;

public class DetailsActivity extends AppCompatActivity {
    private static final String TAG = "DetailsActivity";
    private ActivityDetailsBinding binding;
    private PdfDocument pdfDocument;
    private File annotationsFile;
    private CollectionReference annotationsCollection;
    private RatingBar ratingBar;
    private TextView ratingText;
    private FirebaseFirestore db;
    private String bookId;
    private float currentRating = 0;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        binding = ActivityDetailsBinding.inflate(getLayoutInflater());
        setContentView(binding.getRoot());

        db = FirebaseFirestore.getInstance();
        
        // Initialize views
        ratingText = binding.ratingText;
        
        // Set up click listener for rating container
        binding.ratingContainer.setOnClickListener(v -> {
            showRatingDialog();
        });

        annotationsFile = new File(getFilesDir(), "annotations.json");

        // Get the Book object passed from the previous Activity
        Book book = (Book) getIntent().getSerializableExtra("book");

        if (book != null) {
            bookId = book.getId();
            
            // Initialize Firestore collection reference for annotations
            annotationsCollection = FirebaseFirestore.getInstance()
                    .collection("books")
                    .document(book.getId())
                    .collection("annotations");

            binding.mBookTitle.setText(book.getTitle());
            binding.mAuthorName.setText(book.getAuthor());
            binding.mBookDesc.setText(book.getDescription());

            // Load the book cover using the helper method
            loadBookCover(book);
            
            // Load the initial rating
            if (FirebaseAuth.getInstance().getCurrentUser() != null) {
                loadBookRating();
            }

            // Add click listener for read book button
            binding.mReadBookBtn.setOnClickListener(v -> {
                String pdfUrl = book.getPdfUrl();
                if (pdfUrl != null && !pdfUrl.isEmpty()) {
                    downloadAndOpenPdf(pdfUrl);
                } else {
                    Toast.makeText(this, "PDF URL not available", Toast.LENGTH_SHORT).show();
                }
            });
        }

        // Set up back button
        binding.goBackButton.setOnClickListener(v -> finish());
    }

    private void loadBookCover(Book book) {
        // Check if the book has a Bitmap cover
        if (book.hasBitmapCover()) {
            binding.mBookImage.setImageBitmap(book.getCoverImageBitmap());
        }
        // Check if the book has a URL cover
        else if (book.hasUrlCover()) {
            // Load the cover from a URL using Glide
            Glide.with(this)
                    .load(book.getCoverImageUrl())
                    .placeholder(R.drawable.default_cover)
                    .error(R.drawable.default_cover)
                    .into(binding.mBookImage);
        }
        // Fallback to resource ID if available
        else if (book.hasResIdCover()) {
            try {
                binding.mBookImage.setImageResource(book.getImageResId());
            } catch (Resources.NotFoundException e) {
                Log.e(TAG, "Image resource not found", e);
                binding.mBookImage.setImageResource(R.drawable.default_cover);
            }
        } else {
            // If no cover is available, use a default image
            binding.mBookImage.setImageResource(R.drawable.default_cover);
        }
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

    private void loadBookRating() {
        String userId = FirebaseAuth.getInstance().getCurrentUser().getUid();
        db.collection("books")
            .document(bookId)
            .collection("ratings")
            .document(userId)
            .get()
            .addOnSuccessListener(documentSnapshot -> {
                if (documentSnapshot.exists()) {
                    float rating = documentSnapshot.getDouble("rating").floatValue();
                    currentRating = rating;
                    updateRatingDisplay(rating);
                    updateRatingText(rating);
                }
            })
            .addOnFailureListener(e -> {
                Log.e(TAG, "Error loading rating", e);
                Toast.makeText(this, "Error loading rating", Toast.LENGTH_SHORT).show();
            });
    }

    private void updateBookRating(float rating) {
        if (FirebaseAuth.getInstance().getCurrentUser() == null) {
            Toast.makeText(this, "Please sign in to rate books", Toast.LENGTH_SHORT).show();
            return;
        }

        String userId = FirebaseAuth.getInstance().getCurrentUser().getUid();
        if (bookId == null || bookId.isEmpty()) {
            Log.e(TAG, "Book ID is null or empty");
            Toast.makeText(this, "Error: Invalid book reference", Toast.LENGTH_SHORT).show();
            return;
        }

        try {
            Map<String, Object> ratingData = new HashMap<>();
            ratingData.put("rating", Double.valueOf(rating)); // Convert to Double for Firestore
            ratingData.put("timestamp", System.currentTimeMillis());
            ratingData.put("userId", userId);

            db.collection("books")
                .document(bookId)
                .collection("ratings")
                .document(userId)
                .set(ratingData)
                .addOnSuccessListener(aVoid -> {
                    Log.d(TAG, "Rating updated successfully");
                    updateRatingText(rating);
                    updateAverageRating();
                    currentRating = rating;
                    Toast.makeText(this, "Rating updated successfully", Toast.LENGTH_SHORT).show();
                })
                .addOnFailureListener(e -> {
                    Log.e(TAG, "Error updating rating", e);
                    Toast.makeText(this, "Failed to update rating: " + e.getMessage(), Toast.LENGTH_SHORT).show();
                });
        } catch (Exception e) {
            Log.e(TAG, "Error preparing rating data", e);
            Toast.makeText(this, "Error preparing rating data", Toast.LENGTH_SHORT).show();
        }

        // Update the UI
        updateRatingDisplay(rating);
    }

    private void updateRatingText(float rating) {
        if (rating == 0) {
            ratingText.setText("Rate this book");
        } else {
            ratingText.setText(String.format("%.1f/5.0", rating));
        }
    }

    private void updateAverageRating() {
        db.collection("books")
            .document(bookId)
            .collection("ratings")
            .get()
            .addOnSuccessListener(querySnapshot -> {
                double totalRating = 0;
                int count = querySnapshot.size();
                
                for (DocumentSnapshot doc : querySnapshot.getDocuments()) {
                    totalRating += doc.getDouble("rating");
                }
                
                double averageRating = count > 0 ? totalRating / count : 0;
                
                // Update book's average rating
                db.collection("books")
                    .document(bookId)
                    .update("averageRating", averageRating)
                    .addOnFailureListener(e -> Log.e(TAG, "Error updating average rating", e));
            })
            .addOnFailureListener(e -> Log.e(TAG, "Error calculating average rating", e));
    }

    private void showRatingDialog() {
        Dialog dialog = new Dialog(this);
        dialog.setContentView(R.layout.dialog_rating);
        dialog.getWindow().setBackgroundDrawableResource(android.R.color.transparent);

        RatingBar dialogRatingBar = dialog.findViewById(R.id.ratingBar);
        Button submitButton = dialog.findViewById(R.id.submitButton);

        // Set initial rating if exists
        dialogRatingBar.setRating(currentRating);

        submitButton.setOnClickListener(v -> {
            float rating = dialogRatingBar.getRating();
            updateBookRating(rating);
            dialog.dismiss();
        });

        dialog.show();
    }

    private void updateRatingDisplay(float rating) {
        TextView ratingText = findViewById(R.id.ratingText);
        ratingText.setText(String.format("%.1f", rating));
    }
}
