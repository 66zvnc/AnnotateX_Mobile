package com.example.annotatex_mobile;

import android.graphics.Rect;
import android.os.Bundle;
import android.text.Editable;
import android.text.TextWatcher;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.view.inputmethod.EditorInfo;
import android.widget.EditText;
import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.fragment.app.Fragment;
import androidx.recyclerview.widget.GridLayoutManager;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;
import java.util.ArrayList;
import java.util.List;
import java.util.Random;

public class CategoriesFragment extends Fragment {

    private RecyclerView popularCategoriesRecyclerView;
    private RecyclerView allCategoriesRecyclerView;
    private CategoriesAdapter popularCategoriesAdapter;
    private CategoriesAdapter allCategoriesAdapter;
    private List<Categories> allCategoriesList;
    private EditText searchView;

    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container, @Nullable Bundle savedInstanceState) {
        View view = inflater.inflate(R.layout.fragment_categories, container, false);

        // Initialize search view
        searchView = view.findViewById(R.id.searchInCategories);
        setupSearchView();

        // Initialize RecyclerViews
        popularCategoriesRecyclerView = view.findViewById(R.id.popularCategoriesRecyclerView);
        allCategoriesRecyclerView = view.findViewById(R.id.allCategoriesRecyclerView);

        // Set up layouts with span count of 2 (for grid layout)
        GridLayoutManager popularGridManager = new GridLayoutManager(getContext(), 2);
        GridLayoutManager allGridManager = new GridLayoutManager(getContext(), 2);
        
        popularCategoriesRecyclerView.setLayoutManager(popularGridManager);
        allCategoriesRecyclerView.setLayoutManager(allGridManager);

        // Add spacing
        int spacingInPixels = getResources().getDimensionPixelSize(R.dimen.grid_spacing);
        popularCategoriesRecyclerView.addItemDecoration(new SpaceItemDecoration(spacingInPixels));
        allCategoriesRecyclerView.addItemDecoration(new SpaceItemDecoration(spacingInPixels));

        // Initialize categories
        allCategoriesList = new ArrayList<>();
        
        // Add all categories
        allCategoriesList.add(new Categories("Classic", R.drawable.img_category_classic));
        allCategoriesList.add(new Categories("Fantasy", R.drawable.img_category_fantasy));
        allCategoriesList.add(new Categories("For Kids", R.drawable.img_category_kids));
        allCategoriesList.add(new Categories("Novels", R.drawable.img_category_novels));
        allCategoriesList.add(new Categories("Comedy", R.drawable.img_category_comedy));
        allCategoriesList.add(new Categories("Horror", R.drawable.img_category_horror));
        allCategoriesList.add(new Categories("Science Fiction", R.drawable.img_category_scifi));

        // Get 4 random categories for popular section
        List<Categories> popularCategories = getRandomCategories(new ArrayList<>(allCategoriesList), 4);

        // Set up adapters with fixed size
        popularCategoriesAdapter = new CategoriesAdapter(getContext(), popularCategories);
        allCategoriesAdapter = new CategoriesAdapter(getContext(), allCategoriesList);

        popularCategoriesRecyclerView.setAdapter(popularCategoriesAdapter);
        allCategoriesRecyclerView.setAdapter(allCategoriesAdapter);

        // Force layout measurement
        popularCategoriesRecyclerView.setHasFixedSize(true);
        allCategoriesRecyclerView.setHasFixedSize(true);

        // Post a runnable to notify adapter after layout
        allCategoriesRecyclerView.post(() -> {
            allCategoriesAdapter.notifyDataSetChanged();
        });

        return view;
    }

    private List<Categories> getRandomCategories(List<Categories> source, int count) {
        List<Categories> copy = new ArrayList<>(source);
        List<Categories> randomCategories = new ArrayList<>();
        Random random = new Random();

        for (int i = 0; i < count && !copy.isEmpty(); i++) {
            int index = random.nextInt(copy.size());
            randomCategories.add(copy.remove(index));
        }

        return randomCategories;
    }

    private void setupSearchView() {
        searchView.setOnEditorActionListener((v, actionId, event) -> {
            if (actionId == EditorInfo.IME_ACTION_SEARCH) {
                filterCategories(searchView.getText().toString());
                return true;
            }
            return false;
        });

        searchView.addTextChangedListener(new TextWatcher() {
            @Override
            public void beforeTextChanged(CharSequence s, int start, int count, int after) {}

            @Override
            public void onTextChanged(CharSequence s, int start, int before, int count) {
                filterCategories(s.toString());
            }

            @Override
            public void afterTextChanged(Editable s) {}
        });
    }

    private void filterCategories(String query) {
        List<Categories> filteredList = new ArrayList<>();
        String lowerCaseQuery = query.toLowerCase();

        for (Categories category : allCategoriesList) {
            if (category.getName().toLowerCase().contains(lowerCaseQuery)) {
                filteredList.add(category);
            }
        }

        allCategoriesAdapter.updateCategories(filteredList);
    }

    private static class SpaceItemDecoration extends RecyclerView.ItemDecoration {
        private final int space;

        public SpaceItemDecoration(int space) {
            this.space = space;
        }

        @Override
        public void getItemOffsets(Rect outRect, View view, RecyclerView parent, RecyclerView.State state) {
            outRect.left = space;
            outRect.right = space;
            outRect.bottom = space;
            if (parent.getChildLayoutPosition(view) < 2) {
                outRect.top = space;
            }
        }
    }
}
