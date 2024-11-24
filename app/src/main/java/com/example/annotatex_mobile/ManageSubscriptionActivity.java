package com.example.annotatex_mobile;

import android.content.Intent;
import android.os.Bundle;
import android.widget.ImageView;
import android.widget.LinearLayout;

import androidx.annotation.Nullable;
import androidx.appcompat.app.AppCompatActivity;

public class ManageSubscriptionActivity extends AppCompatActivity {

    private LinearLayout annualPlan;
    private LinearLayout monthlyPlan;

    @Override
    protected void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_manage_subscription); // Updated layout name

        // Set up the go-back button
        ImageView goBackButton = findViewById(R.id.goBackButton);
        goBackButton.setOnClickListener(v -> {
            // Go back to the ProfileFragment
            Intent intent = new Intent(ManageSubscriptionActivity.this, MainActivity.class);
            intent.putExtra("navigateTo", "ProfileFragment");
            startActivity(intent);
            finish();
        });

        // Set up the subscription plans
        annualPlan = findViewById(R.id.annualPlan);
        monthlyPlan = findViewById(R.id.monthlyPlan);

        // Initial state: Annual plan selected
        selectPlan(annualPlan);

        // Set up click listeners for plan selection
        annualPlan.setOnClickListener(v -> selectPlan(annualPlan));
        monthlyPlan.setOnClickListener(v -> selectPlan(monthlyPlan));
    }

    private void selectPlan(LinearLayout selectedPlan) {
        // Reset selection for all plans
        annualPlan.setSelected(false);
        monthlyPlan.setSelected(false);

        // Set the selected plan
        selectedPlan.setSelected(true);
    }
}
