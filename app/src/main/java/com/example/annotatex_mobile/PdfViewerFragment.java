package com.example.annotatex_mobile;

import android.app.Dialog;
import android.content.Intent;
import android.graphics.Bitmap;
import android.net.Uri;
import android.os.Bundle;
import android.provider.MediaStore;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Button;
import android.widget.EditText;
import android.widget.ImageView;
import android.widget.Toast;

import androidx.activity.result.ActivityResultLauncher;
import androidx.activity.result.contract.ActivityResultContracts;
import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.fragment.app.Fragment;

import com.google.android.gms.ads.AdListener;
import com.google.android.gms.ads.AdRequest;
import com.google.android.gms.ads.AdView;
import com.google.android.gms.ads.LoadAdError;
import com.google.android.gms.ads.MobileAds;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.firestore.DocumentReference;
import com.google.firebase.firestore.FirebaseFirestore;
import com.google.firebase.storage.FirebaseStorage;
import com.google.firebase.storage.StorageReference;
import com.pspdfkit.configuration.activity.PdfActivityConfiguration;
import com.pspdfkit.ui.PdfActivityIntentBuilder;

import java.io.File;
import java.io.IOException;
import java.util.HashMap;
import java.util.Map;

public class PdfViewerFragment extends Fragment {

    private static final String TAG = "PdfViewerFragment";

    private String pdfUrl;
    private boolean shouldLoadPdf = true;
    private FirebaseStorage storage;
    private FirebaseFirestore firestore;
    private FirebaseAuth auth;
    private Dialog addBookInfoDialog;
    private Uri coverImageUri;
    private DocumentReference pdfDocRef;
    private AdView adView;

    // Ensure Firestore instance is initialized
    public void setFirestore(FirebaseFirestore firestore) {
        this.firestore = firestore;
    }

    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container, @Nullable Bundle savedInstanceState) {
        View view = inflater.inflate(R.layout.fragment_pdf_viewer, container, false);

        // Initialize Firebase
        storage = FirebaseStorage.getInstance();
        auth = FirebaseAuth.getInstance();
        firestore = FirebaseFirestore.getInstance();  // Make sure Firestore is initialized

        // Initialize AdMob
        MobileAds.initialize(requireContext(), initializationStatus -> {});

        // Set up the AdView
        adView = view.findViewById(R.id.adView);
        loadBannerAd();

        Button uploadButton = view.findViewById(R.id.uploadButton);
        uploadButton.setOnClickListener(v -> openFileChooser());

        // Load PDF if flag is true
        if (shouldLoadPdf && pdfUrl != null) {
            downloadAndOpenPdfWithPSPDFKit(pdfUrl);
        }

        return view;
    }

    private void loadBannerAd() {
        AdRequest adRequest = new AdRequest.Builder().build();
        adView.loadAd(adRequest);
        adView.setAdListener(new AdListener() {
            @Override
            public void onAdLoaded() {
                Log.d(TAG, "Ad loaded successfully");
            }

            @Override
            public void onAdFailedToLoad(LoadAdError adError) {
                Log.e(TAG, "Ad failed to load: " + adError.getMessage());
            }
        });
    }

    private void downloadAndOpenPdfWithPSPDFKit(String url) {
        StorageReference pdfRef = storage.getReferenceFromUrl(url);

        try {
            File localFile = File.createTempFile("tempPdf", ".pdf", requireContext().getCacheDir());

            pdfRef.getFile(localFile).addOnSuccessListener(taskSnapshot -> {
                openPdfWithPSPDFKit(Uri.fromFile(localFile));
                initializeRealTimeUpdates(localFile.getName());
            }).addOnFailureListener(e -> {
                Log.e(TAG, "Failed to download PDF", e);
                Toast.makeText(requireContext(), "Failed to open PDF", Toast.LENGTH_SHORT).show();
            });
        } catch (IOException e) {
            Log.e(TAG, "Error creating temp file", e);
        }
    }

    private void initializeRealTimeUpdates(String documentId) {
        pdfDocRef = firestore.collection("pdfDocuments").document(documentId);
        pdfDocRef.addSnapshotListener((snapshot, e) -> {
            if (e != null) {
                Log.w(TAG, "Listen failed.", e);
                return;
            }

            if (snapshot != null && snapshot.exists()) {
                Map<String, Object> data = snapshot.getData();
                syncAnnotations(data);
            }
        });
    }

    private void syncAnnotations(Map<String, Object> data) {
        if (data != null) {
            Log.d(TAG, "Syncing annotations: " + data);
        }
    }

    private void openPdfWithPSPDFKit(Uri fileUri) {
        PdfActivityConfiguration configuration = new PdfActivityConfiguration.Builder(requireContext())
                .theme(R.style.MyApp_PSPDFKitTheme)
                .enableAnnotationEditing()
                .disableOutline()
                .disableSearch()
                .build();

        Intent intent = PdfActivityIntentBuilder.fromUri(requireContext(), fileUri)
                .configuration(configuration)
                .build();

        startActivity(intent);
    }

    private void openFileChooser() {
        Intent intent = new Intent(Intent.ACTION_GET_CONTENT);
        intent.setType("application/pdf");
        pdfFileLauncher.launch(Intent.createChooser(intent, "Select PDF"));
    }

    private final ActivityResultLauncher<Intent> pdfFileLauncher = registerForActivityResult(
            new ActivityResultContracts.StartActivityForResult(),
            result -> {
                if (result.getResultCode() == getActivity().RESULT_OK && result.getData() != null) {
                    Uri pdfUri = result.getData().getData();
                    showAddBookInfoDialog(pdfUri);
                }
            }
    );

    private final ActivityResultLauncher<Intent> imagePickerLauncher = registerForActivityResult(
            new ActivityResultContracts.StartActivityForResult(),
            result -> {
                if (result.getResultCode() == getActivity().RESULT_OK && result.getData() != null) {
                    coverImageUri = result.getData().getData();
                    try {
                        Bitmap bitmap = MediaStore.Images.Media.getBitmap(requireActivity().getContentResolver(), coverImageUri);
                        ImageView coverPreviewImage = addBookInfoDialog.findViewById(R.id.coverPreviewImage);
                        coverPreviewImage.setImageBitmap(bitmap);
                    } catch (IOException e) {
                        Log.e(TAG, "Error loading image", e);
                    }
                }
            }
    );

    private void showAddBookInfoDialog(Uri pdfUri) {
        // Initialize the dialog
        addBookInfoDialog = new Dialog(requireContext());
        addBookInfoDialog.setContentView(R.layout.dialog_add_book_info);

        // Set the dialog width to match parent
        if (addBookInfoDialog.getWindow() != null) {
            addBookInfoDialog.getWindow().setLayout(ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.WRAP_CONTENT);
        }

        // Initialize dialog views
        ImageView coverPreviewImage = addBookInfoDialog.findViewById(R.id.coverPreviewImage);
        Button selectCoverButton = addBookInfoDialog.findViewById(R.id.selectCoverButton);
        EditText bookTitleInput = addBookInfoDialog.findViewById(R.id.bookTitleInput);
        EditText bookDescriptionInput = addBookInfoDialog.findViewById(R.id.bookDescriptionInput);
        Button confirmButton = addBookInfoDialog.findViewById(R.id.confirmButton);

        // Handle selecting a cover image
        selectCoverButton.setOnClickListener(v -> {
            Intent intent = new Intent(Intent.ACTION_PICK);
            intent.setType("image/*");
            imagePickerLauncher.launch(intent);
        });

        // Handle the confirm button click
        confirmButton.setOnClickListener(v -> {
            String title = bookTitleInput.getText().toString().trim();
            String description = bookDescriptionInput.getText().toString().trim();

            if (title.isEmpty() || coverImageUri == null) {
                Toast.makeText(requireContext(), "Title and cover image are required.", Toast.LENGTH_SHORT).show();
                return;
            }

            uploadPdfToFirebase(pdfUri, title, description, coverImageUri);
            addBookInfoDialog.dismiss();
        });

        addBookInfoDialog.show();
    }

    private void uploadPdfToFirebase(Uri pdfUri, String title, String description, Uri coverImageUri) {
        // Get a reference to Firebase Storage
        StorageReference pdfRef = storage.getReference().child("uploads/" + System.currentTimeMillis() + ".pdf");

        // Upload the PDF file
        pdfRef.putFile(pdfUri).addOnSuccessListener(taskSnapshot -> {
            // Get the download URL of the uploaded PDF
            pdfRef.getDownloadUrl().addOnSuccessListener(pdfDownloadUrl -> {
                // Upload the cover image after the PDF is successfully uploaded
                uploadCoverImageToFirebase(pdfDownloadUrl.toString(), title, description, coverImageUri);
            }).addOnFailureListener(e -> {
                Log.e(TAG, "Failed to get PDF download URL", e);
                Toast.makeText(requireContext(), "Failed to upload PDF", Toast.LENGTH_SHORT).show();
            });
        }).addOnFailureListener(e -> {
            Log.e(TAG, "Failed to upload PDF", e);
            Toast.makeText(requireContext(), "Failed to upload PDF", Toast.LENGTH_SHORT).show();
        });
    }

    private void uploadCoverImageToFirebase(String pdfUrl, String title, String description, Uri coverImageUri) {
        // Get a reference to Firebase Storage for the cover image
        StorageReference coverRef = storage.getReference().child("covers/" + System.currentTimeMillis() + ".jpg");

        // Upload the cover image
        coverRef.putFile(coverImageUri).addOnSuccessListener(taskSnapshot -> {
            // Get the download URL of the uploaded cover image
            coverRef.getDownloadUrl().addOnSuccessListener(coverUrl -> {
                // Save the book metadata to Firestore
                saveBookMetadataToFirestore(pdfUrl, title, description, coverUrl.toString());
            }).addOnFailureListener(e -> {
                Log.e(TAG, "Failed to get cover image download URL", e);
                Toast.makeText(requireContext(), "Failed to upload cover image", Toast.LENGTH_SHORT).show();
            });
        }).addOnFailureListener(e -> {
            Log.e(TAG, "Failed to upload cover image", e);
            Toast.makeText(requireContext(), "Failed to upload cover image", Toast.LENGTH_SHORT).show();
        });
    }

    private void saveBookMetadataToFirestore(String pdfUrl, String title, String description, String coverUrl) {
        String userId = auth.getCurrentUser() != null ? auth.getCurrentUser().getUid() : "Unknown";

        // Prepare book data
        Map<String, Object> bookData = new HashMap<>();
        bookData.put("userId", userId);
        bookData.put("pdfUrl", pdfUrl);
        bookData.put("title", title);
        bookData.put("description", description);
        bookData.put("coverUrl", coverUrl);
        bookData.put("timestamp", System.currentTimeMillis());

        // Add the book data to Firestore
        firestore.collection("books")
                .add(bookData)
                .addOnSuccessListener(documentReference -> {
                    Log.d(TAG, "Book metadata saved with ID: " + documentReference.getId());
                    Toast.makeText(requireContext(), "Book uploaded successfully", Toast.LENGTH_SHORT).show();
                })
                .addOnFailureListener(e -> {
                    Log.e(TAG, "Failed to save book metadata", e);
                    Toast.makeText(requireContext(), "Failed to save book metadata", Toast.LENGTH_SHORT).show();
                });
    }
}
