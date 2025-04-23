package com.example.myapplication.activity;

import android.content.Intent;
import android.os.Bundle;
import android.view.MenuItem;

import androidx.annotation.Nullable;
import androidx.appcompat.app.AppCompatActivity;
import androidx.fragment.app.Fragment;

import com.example.myapplication.R;
import com.example.myapplication.fragments.ChangeInfoFragment;
import com.example.myapplication.fragments.ClockFragment;
import com.example.myapplication.fragments.FollowPageFragment;
import com.google.android.material.bottomnavigation.BottomNavigationView;
import com.google.firebase.auth.FirebaseAuth;

public class HomeActivity extends AppCompatActivity {

    private BottomNavigationView bottomNavigationView;
    private Fragment selectedFragment = new ClockFragment();

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        // Check if user is signed in before continuing
        if (FirebaseAuth.getInstance().getCurrentUser() == null) {
            // If not signed in, redirect to the login activity
            Intent intent = new Intent(HomeActivity.this, MainActivity.class);
            startActivity(intent);
            finish(); // Close the current activity so the user can't return here without signing in
            return;
        }

        setContentView(R.layout.activity_home);

        bottomNavigationView = findViewById(R.id.bottom_navigation);

        bottomNavigationView.setOnNavigationItemSelectedListener(item -> {
            Fragment selectedFragment = getFragment(item);

            if (selectedFragment != null) {
                getSupportFragmentManager().beginTransaction()
                        .replace(R.id.fragment_container, selectedFragment)
                        .commit();
            }
            return true;
        });

        // Set default fragment if no saved instance state
        if (savedInstanceState == null) {
            getSupportFragmentManager().beginTransaction()
                    .replace(R.id.fragment_container, new ClockFragment())
                    .commit();
        }
    }

    @Nullable
    private static Fragment getFragment(MenuItem item) {
        Fragment selectedFragment = null;

        if (item.getItemId() == R.id.nav_clock) {
            selectedFragment = new ClockFragment();
        } else if (item.getItemId() == R.id.nav_follow_page) {
            selectedFragment = new FollowPageFragment();
        } else if (item.getItemId() == R.id.nav_change_info) {
            selectedFragment = new ChangeInfoFragment();
        }

        return selectedFragment;
    }
}
