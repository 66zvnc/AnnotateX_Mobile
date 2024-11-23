package com.example.annotatex_mobile;

import android.content.Intent;
import android.os.Bundle;
import android.view.View;
import android.widget.ImageView;

import androidx.annotation.Nullable;
import androidx.appcompat.app.AppCompatActivity;

public class PaymentMethodsProfileActivity extends AppCompatActivity {

    @Override
    protected void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.fragment_profile_payment_methods);

        // Set up the go-back button
        ImageView goBackButton = findViewById(R.id.goBackButton);
        goBackButton.setOnClickListener(v -> {
            // Go back to the ProfileFragment
            Intent intent = new Intent(PaymentMethodsProfileActivity.this, MainActivity.class);
            intent.putExtra("navigateTo", "ProfileFragment");
            startActivity(intent);
            finish();
        });
    }
}
