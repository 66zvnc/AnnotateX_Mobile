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
import com.bumptech.glide.Glide;
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
    private TextView bodTitle;
    private ImageView bodImage;
    private MaterialButton bodButton;
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
        bodTitle = bookOfTheDayContainer.findViewById(R.id.textView);
        bodImage = bookOfTheDayContainer.findViewById(R.id.imageView);
        bodButton = bookOfTheDayContainer.findViewById(R.id.mReadBookBtn);

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

        // Load books from Firestore and add predefined books
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
                    String description = document.getString("description");
                    String coverUrl = document.getString("coverUrl");

                    // Create a Book object with cover URL
                    Book book = new Book(coverUrl, pdfUrl, title, author, description);

                    // Add the book to the list
                    bookList.add(book);
                }

                // Add predefined books if Firestore has loaded successfully
                addPredefinedBooks();

                adapter.notifyDataSetChanged();

                // Set the first book as Book of the Day if available
                if (!bookList.isEmpty()) {
                    Book bookOfTheDay = bookList.get(0);
                    bodTitle.setText("Today's Book: " + bookOfTheDay.getTitle());

                    if (bookOfTheDay.hasBitmapCover()) {
                        bodImage.setImageBitmap(bookOfTheDay.getCoverImageBitmap());
                    } else if (bookOfTheDay.hasUrlCover()) {
                        Glide.with(this).load(bookOfTheDay.getCoverImageUrl()).into(bodImage);
                    } else {
                        bodImage.setImageResource(bookOfTheDay.getImageResId());
                    }

                    bodButton.setOnClickListener(v -> onPdfClick(bookOfTheDay));
                }

            } else {
                Log.e(TAG, "Error getting documents: ", task.getException());
            }
        });
    }

    private void addPredefinedBooks() {
        // Add predefined books here if necessary
        bookList.add(new Book(R.drawable.book_1, "url_to_pdf_1", "Rich Dad Poor Dad", "Robert T. Kiyosaki", "What the rich teach their kids about money."));
        bookList.add(new Book(R.drawable.book_2, "url_to_pdf_2", "Atomic Habits", "James Clear", "An easy & proven way to build good habits."));
        bookList.add(new Book(R.drawable.book_3, "url_to_pdf_3", "Best Self", "Mike Bayer", "Be you, only better."));
        bookList.add(new Book(R.drawable.book_4, "url_to_pdf_4", "How to Be Fine", "Kristen Meinzer", "What we learned from living by the rules of 50 self-help books."));
        bookList.add(new Book(R.drawable.book_5, "url_to_pdf_5", "Out of the Box", "Suzanne Dudley", "A journey in and out of emotional captivity."));
        bookList.add(new Book(R.drawable.book_6, "url_to_pdf_6", "Stripped", "Brenda M. Gonzalez", "Learning to live and love after trauma."));
        bookList.add(new Book(R.drawable.book_7, "url_to_pdf_7", "12 Rules for Life", "Jordan B. Peterson", "An antidote to chaos."));
        bookList.add(new Book(R.drawable.book_8, "url_to_pdf_8", "Readistan", "Shah Rukh Nadeem", "The best books summarized in 10 minutes."));
        bookList.add(new Book(R.drawable.book_9, "url_to_pdf_9", "Reclaim Your Heart", "Yasmin Mogahed", "Breaking free from life’s shackles."));
        bookList.add(new Book(R.drawable.book_10, "url_to_pdf_10", "Lost Islamic History", "Firas Alkhateeb", "Reclaiming Muslim civilization from the past."));
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
