// LibraryFragment.java
package com.example.annotatex_mobile;

import android.content.Intent;
import android.os.Bundle;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageView;
import android.widget.TextView;
import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.fragment.app.Fragment;
import androidx.interpolator.view.animation.FastOutSlowInInterpolator;
import androidx.recyclerview.widget.GridLayoutManager;
import androidx.recyclerview.widget.RecyclerView;
import com.google.android.material.button.MaterialButton;
import com.google.firebase.firestore.CollectionReference;
import com.google.firebase.firestore.FirebaseFirestore;
import com.google.firebase.firestore.QueryDocumentSnapshot;
import java.util.ArrayList;
import java.util.List;

public class LibraryFragment extends Fragment implements PdfGalleryAdapter.OnPdfClickListener {

    private static final String TAG = "LibraryFragment";
    private RecyclerView pdfGalleryRecyclerView;
    private PdfGalleryAdapter adapter;
    private List<Book> bookList;
    private View bookOfTheDayContainer;
    private boolean isBookOfTheDayVisible = true;
    private FirebaseFirestore firestore;

    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container,
                             @Nullable Bundle savedInstanceState) {
        View view = inflater.inflate(R.layout.activity_library, container, false);

        // Initialize Firestore
        firestore = FirebaseFirestore.getInstance();

        // Initialize the Book of the Day container and UI elements
        bookOfTheDayContainer = view.findViewById(R.id.bookOfTheDayContainer);
        TextView bodTitle = bookOfTheDayContainer.findViewById(R.id.textView);
        ImageView bodImage = bookOfTheDayContainer.findViewById(R.id.imageView);
        MaterialButton bodButton = bookOfTheDayContainer.findViewById(R.id.mReadBookBtn);

        // Initialize book list and adapter
        bookList = new ArrayList<>();
        adapter = new PdfGalleryAdapter(getContext(), bookList, this);
        pdfGalleryRecyclerView = view.findViewById(R.id.pdfGalleryRecyclerView);
        pdfGalleryRecyclerView.setLayoutManager(new GridLayoutManager(getContext(), 2));
        pdfGalleryRecyclerView.setAdapter(adapter);

        // Add spacing decoration
        int spacingInPixels = getResources().getDimensionPixelSize(R.dimen.grid_spacing);
        pdfGalleryRecyclerView.addItemDecoration(new SpacingItemDecoration(spacingInPixels));

        // Set up scroll listener for Book of the Day visibility
        pdfGalleryRecyclerView.addOnScrollListener(new RecyclerView.OnScrollListener() {
            @Override
            public void onScrolled(@NonNull RecyclerView recyclerView, int dx, int dy) {
                super.onScrolled(recyclerView, dx, dy);
                FastOutSlowInInterpolator interpolator = new FastOutSlowInInterpolator();

                if (dy > 0 && isBookOfTheDayVisible) {
                    bookOfTheDayContainer.animate()
                            .translationY(-bookOfTheDayContainer.getHeight())
                            .alpha(0f)
                            .setInterpolator(interpolator)
                            .setDuration(500)
                            .withEndAction(() -> bookOfTheDayContainer.setVisibility(View.GONE));
                    isBookOfTheDayVisible = false;
                } else if (dy < 0 && !isBookOfTheDayVisible) {
                    bookOfTheDayContainer.setVisibility(View.VISIBLE);
                    bookOfTheDayContainer.animate()
                            .translationY(0)
                            .alpha(1f)
                            .setInterpolator(interpolator)
                            .setDuration(500);
                    isBookOfTheDayVisible = true;
                }
            }
        });

        // Load books from Firestore
        loadBooksFromFirestore();

        return view;
    }

    private void loadBooksFromFirestore() {
        CollectionReference booksCollection = firestore.collection("books");
        booksCollection.get().addOnCompleteListener(task -> {
            if (task.isSuccessful()) {
                bookList.clear(); // Clear the list to avoid duplicates
                for (QueryDocumentSnapshot document : task.getResult()) {
                    // Retrieve data from Firestore document
                    String pdfUrl = document.getString("pdfUrl");
                    String title = document.getString("title");
                    String author = document.getString("author");
                    String description = "No description available";

                    // Create a Book object
                    Book book = new Book(R.drawable.book_handle, pdfUrl, title, author, description);

                    // Add the book to the list
                    bookList.add(book);
                }
                adapter.notifyDataSetChanged();

                // Set the first book as Book of the Day if available
                if (!bookList.isEmpty()) {
                    Book bookOfTheDay = bookList.get(0);
                    TextView bodTitle = bookOfTheDayContainer.findViewById(R.id.textView);
                    ImageView bodImage = bookOfTheDayContainer.findViewById(R.id.imageView);
                    bodTitle.setText("Today's Book: " + bookOfTheDay.getTitle());
                    bodImage.setImageResource(bookOfTheDay.getImageResId());
                }

            } else {
                Log.e(TAG, "Error getting documents: ", task.getException());
            }
        });
    }

    @Override
    public void onPdfClick(Book book) {
        Intent intent = new Intent(getActivity(), DetailsActivity.class);
        intent.putExtra("book", book);
        startActivity(intent);
    }

    @Override
    public void onPdfClick(String pdfUrl) {
        // Optional, not required if using the Book object
    }

    public void addBookToLibrary(Book book) {
        bookList.add(book);
        adapter.notifyItemInserted(bookList.size() - 1);
    }
}
