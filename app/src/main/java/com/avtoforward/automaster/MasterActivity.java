package com.avtoforward.automaster;

import android.content.Intent;
import android.os.Bundle;
import android.widget.Button;

import androidx.appcompat.app.AppCompatActivity;
import androidx.appcompat.widget.Toolbar;
import androidx.fragment.app.Fragment;

public class MasterActivity extends AppCompatActivity {

    private Button navHome, navForum, navOrders, navStatistics, navProfile;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_master);

        Toolbar toolbar = findViewById(R.id.toolbar);
        setSupportActionBar(toolbar);

        navHome = findViewById(R.id.nav_home);
        navForum = findViewById(R.id.nav_forum);
        navOrders = findViewById(R.id.nav_orders);
        navStatistics = findViewById(R.id.nav_statistics);
        navProfile = findViewById(R.id.nav_profile);

        navHome.setOnClickListener(v -> showFragment(new MenuFragment()));
        navForum.setOnClickListener(v -> startActivity(new Intent(this, ForumActivity.class)));
        // ✅ Исправлено: открываем ActiveOrdersActivity (новые заказы)
        navOrders.setOnClickListener(v -> startActivity(new Intent(this, ActiveOrdersActivity.class)));
        navStatistics.setOnClickListener(v -> startActivity(new Intent(this, StatisticsActivity.class)));
        navProfile.setOnClickListener(v -> startActivity(new Intent(this, EditMasterProfileActivity.class)));

        if (savedInstanceState == null) {
            showFragment(new MenuFragment());
        }
    }

    private void showFragment(Fragment fragment) {
        getSupportFragmentManager().beginTransaction()
                .replace(R.id.container, fragment)
                .commit();
    }
}