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

import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.firestore.DocumentReference;
import com.google.firebase.firestore.FirebaseFirestore;
import com.google.firebase.firestore.SetOptions;
import com.google.firebase.storage.FirebaseStorage;
import com.google.firebase.storage.StorageReference;
import com.pspdfkit.configuration.activity.PdfActivityConfiguration;
import com.pspdfkit.ui.PdfActivityIntentBuilder;

import java.io.File;
import java.io.IOException;
import java.util.HashMap;
import java.util.Map;

public class PdfViewerFragment extends Fragment {

    private static final String ARG_PDF_URL = "pdfUrl";
    private static final String TAG = "PdfViewerFragment";

    private String pdfUrl;
    private boolean shouldLoadPdf = true;
    private FirebaseStorage storage;
    private FirebaseFirestore firestore;
    private FirebaseAuth auth;
    private Dialog addBookInfoDialog;
    private Uri coverImageUri;
    private DocumentReference pdfDocRef;

    // Method to set Firestore instance
    public void setFirestore(FirebaseFirestore firestore) {
        this.firestore = firestore;
    }

    // Method to control PDF loading behavior
    public void setShouldLoadPdf(boolean shouldLoad) {
        this.shouldLoadPdf = shouldLoad;
    }

    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container, @Nullable Bundle savedInstanceState) {
        View view = inflater.inflate(R.layout.fragment_pdf_viewer, container, false);

        storage = FirebaseStorage.getInstance();
        auth = FirebaseAuth.getInstance();

        Button uploadButton = view.findViewById(R.id.uploadButton);
        uploadButton.setOnClickListener(v -> openFileChooser());

        if (shouldLoadPdf && pdfUrl != null) {
            downloadAndOpenPdfWithPSPDFKit(pdfUrl);
        }

        return view;
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
        // Get a reference to the Firestore document
        pdfDocRef = firestore.collection("pdfDocuments").document(documentId);

        // Set up a real-time listener on the Firestore document
        pdfDocRef.addSnapshotListener((snapshot, e) -> {
            if (e != null) {
                Log.w(TAG, "Listen failed.", e);
                return;
            }

            if (snapshot != null && snapshot.exists()) {
                Map<String, Object> data = snapshot.getData();
                syncAnnotations(data);
            } else {
                Log.d(TAG, "No data found for real-time updates.");
            }
        });
    }

    private void syncAnnotations(Map<String, Object> data) {
        if (data != null) {
            Log.d(TAG, "Syncing annotations: " + data);
            // Implement your logic to update annotations based on the data retrieved
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

    private void showAddBookInfoDialog(Uri pdfUri) {
        addBookInfoDialog = new Dialog(requireContext());
        addBookInfoDialog.setContentView(R.layout.dialog_add_book_info);

        if (addBookInfoDialog.getWindow() != null) {
            addBookInfoDialog.getWindow().setLayout(ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.WRAP_CONTENT);
        }

        ImageView coverPreviewImage = addBookInfoDialog.findViewById(R.id.coverPreviewImage);
        Button selectCoverButton = addBookInfoDialog.findViewById(R.id.selectCoverButton);
        EditText bookTitleInput = addBookInfoDialog.findViewById(R.id.bookTitleInput);
        EditText bookDescriptionInput = addBookInfoDialog.findViewById(R.id.bookDescriptionInput);
        Button confirmButton = addBookInfoDialog.findViewById(R.id.confirmButton);

        selectCoverButton.setOnClickListener(v -> {
            Intent intent = new Intent(Intent.ACTION_PICK);
            intent.setType("image/*");
            imagePickerLauncher.launch(intent);
        });

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
        StorageReference pdfRef = storage.getReference().child("uploads/" + System.currentTimeMillis() + ".pdf");

        pdfRef.putFile(pdfUri).addOnSuccessListener(taskSnapshot -> pdfRef.getDownloadUrl().addOnSuccessListener(pdfDownloadUrl -> {
            uploadCoverImageToFirebase(pdfDownloadUrl.toString(), title, description, coverImageUri);
        })).addOnFailureListener(e -> Log.e(TAG, "Failed to upload PDF", e));
    }

    private void uploadCoverImageToFirebase(String pdfUrl, String title, String description, Uri coverImageUri) {
        StorageReference coverRef = storage.getReference().child("covers/" + System.currentTimeMillis() + ".jpg");

        coverRef.putFile(coverImageUri).addOnSuccessListener(taskSnapshot -> coverRef.getDownloadUrl().addOnSuccessListener(coverUrl -> {
            saveBookMetadataToFirestore(pdfUrl, title, description, coverUrl.toString());
        })).addOnFailureListener(e -> Log.e(TAG, "Failed to upload cover image", e));
    }

    private void saveBookMetadataToFirestore(String pdfUrl, String title, String description, String coverUrl) {
        String userId = auth.getCurrentUser() != null ? auth.getCurrentUser().getUid() : "Unknown";

        Map<String, Object> bookData = new HashMap<>();
        bookData.put("userId", userId);
        bookData.put("pdfUrl", pdfUrl);
        bookData.put("title", title);
        bookData.put("description", description);
        bookData.put("coverUrl", coverUrl);
        bookData.put("timestamp", System.currentTimeMillis());

        firestore.collection("books").add(bookData)
                .addOnSuccessListener(documentReference -> Log.d(TAG, "Book metadata saved with ID: " + documentReference.getId()))
                .addOnFailureListener(e -> Log.e(TAG, "Failed to save book metadata", e));
    }

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
}
