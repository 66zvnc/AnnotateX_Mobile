package com.example.annotatex_mobile;

import android.content.Intent;
import android.content.res.Resources;
import android.graphics.RectF;
import android.net.Uri;
import android.os.Bundle;
import android.util.Log;
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
import com.pspdfkit.annotations.HighlightAnnotation;
import com.google.gson.Gson;
import com.google.gson.reflect.TypeToken;

import java.io.File;
import java.io.FileReader;
import java.io.FileWriter;
import java.io.IOException;
import java.lang.reflect.Type;
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
                // You can load the image from URL if you have Glide or Picasso integrated
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

    @Override
    protected void onPause() {
        super.onPause();
        if (pdfDocument != null) {
            saveAnnotationsToFirestore();
        }
    }

    private void saveAnnotationsToFirestore() {
        List<AnnotationData> annotationDataList = new ArrayList<>();

        List<Annotation> annotations = pdfDocument.getAnnotationProvider()
                .getAllAnnotationsOfType(EnumSet.of(AnnotationType.HIGHLIGHT));

        for (Annotation annotation : annotations) {
            AnnotationData data = new AnnotationData(
                    annotation.getPageIndex(),
                    annotation.getBoundingBox().left,
                    annotation.getBoundingBox().top,
                    annotation.getBoundingBox().right,
                    annotation.getBoundingBox().bottom,
                    annotation.getType().toString()
            );
            annotationDataList.add(data);
        }

        for (AnnotationData data : annotationDataList) {
            annotationsCollection.add(data).addOnSuccessListener(documentReference ->
                            Log.d(TAG, "Annotation saved with ID: " + documentReference.getId()))
                    .addOnFailureListener(e ->
                            Log.e(TAG, "Error saving annotation", e));
        }
    }

    // Data class for saving annotation details
    private static class AnnotationData {
        int pageIndex;
        float left;
        float top;
        float right;
        float bottom;
        String type;

        AnnotationData(int pageIndex, float left, float top, float right, float bottom, String type) {
            this.pageIndex = pageIndex;
            this.left = left;
            this.top = top;
            this.right = right;
            this.bottom = bottom;
            this.type = type;
        }
    }
}
