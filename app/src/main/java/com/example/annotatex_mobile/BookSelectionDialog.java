package com.example.annotatex_mobile;

import android.app.Dialog;
import android.content.Context;
import android.os.Bundle;

import androidx.recyclerview.widget.RecyclerView;

import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.firestore.FirebaseFirestore;
import com.google.firebase.firestore.QueryDocumentSnapshot;

import java.util.List;

public class BookSelectionDialog extends Dialog {

    private List<Book> books;

    public BookSelectionDialog(Context context, List<Book> books) {
        super(context);
        this.books = books;

        // Initialize Firebase services directly inside the constructor
        FirebaseFirestore firestore = FirebaseFirestore.getInstance();
        String currentUserId = FirebaseAuth.getInstance().getCurrentUser().getUid();

        // Query Firestore for the books owned by the current user
        firestore.collection("users")
                .document(currentUserId) // Get the current user document
                .collection("books") // Get the books subcollection
                .get()
                .addOnSuccessListener(queryDocumentSnapshots -> {
                    if (!queryDocumentSnapshots.isEmpty()) {
                        // Clear previous list of books
                        books.clear();

                        // Loop through the query snapshot and add books to the list
                        for (QueryDocumentSnapshot documentSnapshot : queryDocumentSnapshots) {
                            Book book = documentSnapshot.toObject(Book.class);
                            books.add(book);
                        }

                        // Set up the adapter for the RecyclerView
                        BookSelectionAdapter bookSelectionAdapter = new BookSelectionAdapter(context, books, book -> {
                            // When a book is selected, add it to both users' libraries
                            ((CollaborativeChatActivity) context).addCollaborativeBook(book);
                            dismiss(); // Dismiss the dialog after selecting a book
                        });

                        // Set the adapter to the RecyclerView
                        RecyclerView recyclerView = findViewById(R.id.recyclerViewBooks);
                        recyclerView.setAdapter(bookSelectionAdapter);
                    }
                })
                .addOnFailureListener(e -> {
                    // Handle any errors (e.g., query failure)
                });
    }

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.dialog_book_selection);

        // You could place additional setup or UI updates here if needed
    }
}
