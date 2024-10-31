// LibraryFragment.java
package com.example.annotatex_mobile;

import android.content.Intent;
import android.os.Bundle;
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
import java.util.ArrayList;
import java.util.List;

public class LibraryFragment extends Fragment implements PdfGalleryAdapter.OnPdfClickListener {

    private RecyclerView pdfGalleryRecyclerView;
    private PdfGalleryAdapter adapter;
    private List<Book> bookList;
    private View bookOfTheDayContainer;
    private boolean isBookOfTheDayVisible = true;

    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container,
                             @Nullable Bundle savedInstanceState) {
        View view = inflater.inflate(R.layout.activity_library, container, false);

        // Book of the Day setup
        bookOfTheDayContainer = view.findViewById(R.id.bookOfTheDayContainer);
        TextView bodTitle = bookOfTheDayContainer.findViewById(R.id.textView);
        ImageView bodImage = bookOfTheDayContainer.findViewById(R.id.imageView);
        MaterialButton bodButton = bookOfTheDayContainer.findViewById(R.id.mReadBookBtn);

        // Initialize book list with sample data
        bookList = new ArrayList<>();
        bookList.add(new Book(R.drawable.book_1, "url_to_pdf_1", "Rich Dad Poor Dad", "James Clear", "What the rich teach their kids about money."));
        bookList.add(new Book(R.drawable.book_2, "url_to_pdf_2", "Atomic Habits", "Robert T. Kiyosaki", "An easy & proven way to build good habits."));
        bookList.add(new Book(R.drawable.book_3, "url_to_pdf_3", "Best Self", "Mike Bayer", "Be you, only better."));
        bookList.add(new Book(R.drawable.book_4, "url_to_pdf_4", "How to Be Fine", "Kristen Meinzer", "What we learned from living by the rules of 50 self-help books."));
        bookList.add(new Book(R.drawable.book_5, "url_to_pdf_5", "Out of the Box", "Suzanne Dudley", "A journey in and out of emotional captivity."));
        bookList.add(new Book(R.drawable.book_6, "url_to_pdf_6", "Stripped", "Brenda M. Gonzalez", "Learning to live and love after trauma."));
        bookList.add(new Book(R.drawable.book_7, "url_to_pdf_7", "12 Rules for Life", "Jordan B. Peterson", "An antidote to chaos."));
        bookList.add(new Book(R.drawable.book_8, "url_to_pdf_8", "Readistan", "Shah Rukh Nadeem", "The best books summarized in 10 minutes."));
        bookList.add(new Book(R.drawable.book_9, "url_to_pdf_9", "Reclaim Your Heart", "Yasmin Mogahed", "Breaking free from life’s shackles."));
        bookList.add(new Book(R.drawable.book_10, "url_to_pdf_10", "Lost Islamic History", "Firas Alkhateeb", "Reclaiming Muslim civilization from the past."));

        // Set the first book as the Book of the Day
        Book bookOfTheDay = bookList.get(0);
        bodTitle.setText("Today's Book: " + bookOfTheDay.getTitle());
        bodImage.setImageResource(bookOfTheDay.getImageResId());
        bodButton.setOnClickListener(v -> {
            Intent intent = new Intent(getActivity(), DetailsActivity.class);
            intent.putExtra("book", bookOfTheDay);
            startActivity(intent);
        });

        // Set up RecyclerView
        pdfGalleryRecyclerView = view.findViewById(R.id.pdfGalleryRecyclerView);
        pdfGalleryRecyclerView.setLayoutManager(new GridLayoutManager(getContext(), 2));

        // Add spacing decoration
        int spacingInPixels = getResources().getDimensionPixelSize(R.dimen.grid_spacing);
        pdfGalleryRecyclerView.addItemDecoration(new SpacingItemDecoration(spacingInPixels));

        // Set up adapter with the book list and listener
        adapter = new PdfGalleryAdapter(getContext(), bookList, this);
        pdfGalleryRecyclerView.setAdapter(adapter);

        // Add scroll listener to handle Book of the Day visibility
        pdfGalleryRecyclerView.addOnScrollListener(new RecyclerView.OnScrollListener() {
            @Override
            public void onScrolled(@NonNull RecyclerView recyclerView, int dx, int dy) {
                super.onScrolled(recyclerView, dx, dy);

                FastOutSlowInInterpolator interpolator = new FastOutSlowInInterpolator();

                if (dy > 0 && isBookOfTheDayVisible) {
                    // Scrolling down: hide Book of the Day with fade-out and slide-up
                    bookOfTheDayContainer.animate()
                            .translationY(-bookOfTheDayContainer.getHeight())
                            .alpha(0f)
                            .setInterpolator(interpolator)
                            .setDuration(500)
                            .withEndAction(() -> bookOfTheDayContainer.setVisibility(View.GONE));
                    isBookOfTheDayVisible = false;
                } else if (dy < 0 && !isBookOfTheDayVisible) {
                    // Scrolling up: show Book of the Day with fade-in and slide-down
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

        return view;
    }

    @Override
    public void onPdfClick(Book book) {
        Intent intent = new Intent(getActivity(), DetailsActivity.class);
        intent.putExtra("book", book);
        startActivity(intent);
    }

    @Override
    public void onPdfClick(String pdfUrl) {

    }
}
