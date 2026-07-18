package com.avtoforward.automaster;

import android.content.Intent;
import android.os.Bundle;
import android.view.MenuItem;

import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;
import androidx.appcompat.widget.Toolbar;
import androidx.fragment.app.Fragment;
import androidx.fragment.app.FragmentTransaction;

import com.avtoforward.automaster.fragments.ClientOrdersFragment;
import com.avtoforward.automaster.fragments.ClientProfileFragment;
import com.avtoforward.automaster.fragments.ServicesFragment;
import com.google.android.material.bottomnavigation.BottomNavigationView;

public class MainActivity extends AppCompatActivity {

    private BottomNavigationView bottomNavigationView;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        Toolbar toolbar = findViewById(R.id.toolbarMain);
        setSupportActionBar(toolbar);
        if (getSupportActionBar() != null) {
            getSupportActionBar().setTitle("АвтоТехПомощь");
        }

        bottomNavigationView = findViewById(R.id.bottom_navigation_main);
        bottomNavigationView.setOnItemSelectedListener(this::onNavigationItemSelected);

        if (savedInstanceState == null) {
            loadFragment(new ServicesFragment());
        }

        // ✅ ЗАПУСКАЕМ СЕРВИС УВЕДОМЛЕНИЙ (если ещё не запущен)
        Intent serviceIntent = new Intent(this, ForegroundNotificationService.class);
        startService(serviceIntent);
    }

    private boolean onNavigationItemSelected(@NonNull MenuItem item) {
        Fragment fragment = null;
        int id = item.getItemId();
        if (id == R.id.nav_services) {
            fragment = new ServicesFragment();
        } else if (id == R.id.nav_orders) {
            fragment = new ClientOrdersFragment();
        } else if (id == R.id.nav_profile) {
            fragment = new ClientProfileFragment();
        }
        if (fragment != null) {
            loadFragment(fragment);
            return true;
        }
        return false;
    }

    private void loadFragment(Fragment fragment) {
        FragmentTransaction transaction = getSupportFragmentManager().beginTransaction();
        transaction.replace(R.id.container_main, fragment);
        transaction.commit();
    }
}