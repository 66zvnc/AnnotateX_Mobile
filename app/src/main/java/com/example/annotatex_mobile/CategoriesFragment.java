package com.example.annotatex_mobile;

import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.fragment.app.Fragment;
import androidx.recyclerview.widget.GridLayoutManager;
import androidx.recyclerview.widget.RecyclerView;
import java.util.ArrayList;
import java.util.List;

public class CategoriesFragment extends Fragment {

    private RecyclerView categoriesRecyclerView;
    private CategoriesAdapter categoriesAdapter;
    private List<String> categoriesList;

    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container, @Nullable Bundle savedInstanceState) {
        View view = inflater.inflate(R.layout.fragment_categories, container, false);

        // Initialize RecyclerView
        categoriesRecyclerView = view.findViewById(R.id.categoriesRecyclerView);
        categoriesRecyclerView.setLayoutManager(new GridLayoutManager(getContext(), 2));

        // Sample data for categories
        categoriesList = new ArrayList<>();
        categoriesList.add("Fiction");
        categoriesList.add("Non-fiction");
        categoriesList.add("Self-Help");
        categoriesList.add("Science");
        categoriesList.add("History");
        categoriesList.add("Biographies");

        categoriesAdapter = new CategoriesAdapter(categoriesList);
        categoriesRecyclerView.setAdapter(categoriesAdapter);

        return view;
    }
}
