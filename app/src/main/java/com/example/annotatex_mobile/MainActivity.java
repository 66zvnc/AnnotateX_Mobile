package com.example.annotatex_mobile;

import android.content.Intent;
import android.os.Bundle;
import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;
import androidx.fragment.app.Fragment;
import com.google.android.gms.ads.MobileAds;
import com.google.android.material.bottomnavigation.BottomNavigationView;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseUser;
import com.google.firebase.firestore.FirebaseFirestore;

public class MainActivity extends AppCompatActivity {

    private FirebaseAuth auth;
    private FirebaseFirestore firestore;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        auth = FirebaseAuth.getInstance();
        firestore = FirebaseFirestore.getInstance();

        // Initialize Google Mobile Ads SDK in a background thread
        new Thread(() -> {
            MobileAds.initialize(this, initializationStatus -> {

            });
        }).start();

        FirebaseUser currentUser = auth.getCurrentUser();
        if (currentUser == null) {
            startActivity(new Intent(MainActivity.this, LoginActivity.class));
            finish();
            return;
        }

        BottomNavigationView bottomNav = findViewById(R.id.bottom_nav);
        bottomNav.setOnNavigationItemSelectedListener(navListener);

        if (savedInstanceState == null) {
            bottomNav.setSelectedItemId(R.id.nav_library);
        }
    }

    private final BottomNavigationView.OnNavigationItemSelectedListener navListener = item -> {
        Fragment selectedFragment = null;

        int itemId = item.getItemId();

        if (itemId == R.id.nav_library) {
            selectedFragment = new LibraryFragment();
        } else if (itemId == R.id.nav_categories) {
            selectedFragment = new CategoriesFragment();
        } else if (itemId == R.id.nav_add_book) {
            PdfViewerFragment pdfViewerFragment = new PdfViewerFragment();
            pdfViewerFragment.setFirestore(firestore);
            pdfViewerFragment.setShouldLoadPdf(false);
            selectedFragment = pdfViewerFragment;
        } else if (itemId == R.id.nav_friends) {
            selectedFragment = new FriendsFragment();
        } else if (itemId == R.id.nav_profile) {
            selectedFragment = new ProfileFragment();
        }

        if (selectedFragment != null) {
            getSupportFragmentManager().beginTransaction()
                    .replace(R.id.fragment_container, selectedFragment)
                    .commit();
            return true;
        }
        return false;
    };
}
