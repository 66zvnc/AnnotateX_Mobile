package com.example.annotatex_mobile;

import android.app.Dialog;
import android.content.Intent;
import android.net.Uri;
import android.os.Bundle;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Button;
import android.widget.ProgressBar;
import android.widget.TextView;

import androidx.activity.result.ActivityResultLauncher;
import androidx.activity.result.contract.ActivityResultContracts;
import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.fragment.app.Fragment;

import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.firestore.CollectionReference;
import com.google.firebase.firestore.FirebaseFirestore;
import com.google.firebase.storage.FirebaseStorage;
import com.google.firebase.storage.StorageReference;

import com.pspdfkit.configuration.activity.PdfActivityConfiguration;
import com.pspdfkit.ui.PdfActivityIntentBuilder;
import com.pspdfkit.ui.special_mode.controller.AnnotationTool;

import java.io.File;
import java.io.IOException;
import java.util.EnumSet;
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

    private Dialog uploadDialog;
    private ProgressBar uploadProgressBar;
    private TextView uploadProgressPercent;

    private final ActivityResultLauncher<Intent> pdfFileLauncher = registerForActivityResult(
            new ActivityResultContracts.StartActivityForResult(),
            result -> {
                if (result.getResultCode() == getActivity().RESULT_OK && result.getData() != null) {
                    Uri pdfUri = result.getData().getData();
                    uploadPdfToFirebase(pdfUri);
                }
            }
    );

    public static PdfViewerFragment newInstance(String pdfUrl) {
        PdfViewerFragment fragment = new PdfViewerFragment();
        Bundle args = new Bundle();
        args.putString(ARG_PDF_URL, pdfUrl);
        fragment.setArguments(args);
        return fragment;
    }

    public void setShouldLoadPdf(boolean shouldLoadPdf) {
        this.shouldLoadPdf = shouldLoadPdf;
    }

    @Override
    public void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        if (getArguments() != null) {
            pdfUrl = getArguments().getString(ARG_PDF_URL);
        }
        storage = FirebaseStorage.getInstance();
        firestore = FirebaseFirestore.getInstance();
        auth = FirebaseAuth.getInstance();
    }

    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container, @Nullable Bundle savedInstanceState) {
        View view = inflater.inflate(R.layout.fragment_pdf_viewer, container, false);

        // Set up the Upload button to open file chooser
        Button uploadButton = view.findViewById(R.id.uploadButton);
        uploadButton.setOnClickListener(v -> openFileChooser());

        if (shouldLoadPdf && pdfUrl != null) {
            downloadAndOpenPdfWithPSPDFKit(pdfUrl);
        }

        return view;
    }

    private void openFileChooser() {
        Intent intent = new Intent(Intent.ACTION_GET_CONTENT);
        intent.setType("application/pdf");
        pdfFileLauncher.launch(Intent.createChooser(intent, "Select PDF"));
    }

    private void showUploadDialog() {
        uploadDialog = new Dialog(requireContext());
        uploadDialog.setContentView(R.layout.upload_progress_dialog);
        uploadDialog.setCancelable(false);

        uploadProgressBar = uploadDialog.findViewById(R.id.uploadProgressBar);
        uploadProgressPercent = uploadDialog.findViewById(R.id.uploadProgressPercent);

        uploadDialog.show();
    }

    private void uploadPdfToFirebase(Uri pdfUri) {
        if (pdfUri != null) {
            showUploadDialog();

            StorageReference pdfRef = storage.getReference().child("uploads/" + System.currentTimeMillis() + ".pdf");

            pdfRef.putFile(pdfUri)
                    .addOnProgressListener(snapshot -> {
                        double progress = (100.0 * snapshot.getBytesTransferred()) / snapshot.getTotalByteCount();
                        uploadProgressBar.setProgress((int) progress);
                        uploadProgressPercent.setText((int) progress + "%");
                    })
                    .addOnSuccessListener(taskSnapshot -> pdfRef.getDownloadUrl().addOnSuccessListener(uri -> {
                        Log.d(TAG, "Uploaded PDF URL: " + uri.toString());
                        savePdfMetadataToFirestore(uri.toString());
                        downloadAndOpenPdfWithPSPDFKit(uri.toString());
                        uploadDialog.dismiss();
                    }))
                    .addOnFailureListener(e -> {
                        Log.e(TAG, "Failed to upload PDF", e);
                        uploadDialog.dismiss();
                    });
        }
    }

    private void savePdfMetadataToFirestore(String pdfUrl) {
        String userId = auth.getCurrentUser() != null ? auth.getCurrentUser().getUid() : null;
        if (userId == null) {
            Log.e(TAG, "User is not authenticated. Cannot save PDF metadata.");
            return;
        }

        CollectionReference booksCollection = firestore.collection("books");

        Map<String, Object> pdfData = new HashMap<>();
        pdfData.put("userId", userId);
        pdfData.put("pdfUrl", pdfUrl);
        pdfData.put("timestamp", System.currentTimeMillis());

        booksCollection.add(pdfData)
                .addOnSuccessListener(documentReference -> Log.d(TAG, "PDF metadata saved with ID: " + documentReference.getId()))
                .addOnFailureListener(e -> Log.e(TAG, "Failed to save PDF metadata", e));
    }

    private void downloadAndOpenPdfWithPSPDFKit(String url) {
        StorageReference pdfRef = storage.getReferenceFromUrl(url);

        try {
            File localFile = File.createTempFile("tempPdf", ".pdf");

            pdfRef.getFile(localFile).addOnSuccessListener(taskSnapshot -> openPdfWithPSPDFKit(Uri.fromFile(localFile)))
                    .addOnFailureListener(e -> Log.e(TAG, "Failed to download PDF", e));
        } catch (IOException e) {
            Log.e(TAG, "Error creating temp file", e);
        }
    }

    private void openPdfWithPSPDFKit(Uri fileUri) {
        PdfActivityConfiguration configuration = new PdfActivityConfiguration.Builder(requireContext())
                .theme(R.style.MyApp_PSPDFKitTheme)
                .enableAnnotationEditing() // Enables default annotation tools
                .disableOutline()
                .disableSearch()
                .build();

        Intent intent = PdfActivityIntentBuilder.fromUri(requireContext(), fileUri)
                .configuration(configuration)
                .build();

        startActivity(intent);
    }
}
