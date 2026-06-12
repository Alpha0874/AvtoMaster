package com.avtoforward.automaster;

import android.os.Bundle;
import androidx.appcompat.app.AppCompatActivity;
import androidx.fragment.app.Fragment;
import com.google.android.material.bottomnavigation.BottomNavigationView;

public class UserProfileActivity extends AppCompatActivity {

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_user_profile);

        BottomNavigationView bottomNav = findViewById(R.id.userBottomNav);

        if (savedInstanceState == null) {
            getSupportFragmentManager().beginTransaction()
                    .replace(R.id.userFragmentContainer, new UserProfileInfoFragment())
                    .commit();
        }

        bottomNav.setOnNavigationItemSelectedListener(item -> {
            Fragment selectedFragment = null;
            if (item.getItemId() == R.id.action_profile) {
                selectedFragment = new UserProfileInfoFragment();
            } else if (item.getItemId() == R.id.action_my_orders) {
                selectedFragment = new UserOrdersFragment();
            }
            if (selectedFragment != null) {
                getSupportFragmentManager().beginTransaction()
                        .replace(R.id.userFragmentContainer, selectedFragment)
                        .commit();
            }
            return true;
        });
    }
}