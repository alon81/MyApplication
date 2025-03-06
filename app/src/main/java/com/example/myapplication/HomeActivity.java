package com.example.myapplication;

import android.os.Bundle;
import android.view.MenuItem;

import androidx.annotation.Nullable;
import androidx.appcompat.app.AppCompatActivity;
import androidx.fragment.app.Fragment;
import com.google.android.material.bottomnavigation.BottomNavigationView;

public class HomeActivity extends AppCompatActivity {

    private BottomNavigationView bottomNavigationView;
    private Fragment selectedFragment = new ClockFragment(); // Default fragment

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_home); // Set layout for HomeActivity

        // Initialize BottomNavigationView
        bottomNavigationView = findViewById(R.id.bottom_navigation);

        // Set item selection listener for BottomNavigation
        bottomNavigationView.setOnNavigationItemSelectedListener(item -> {
            Fragment selectedFragment = getFragment(item);


            if (selectedFragment != null) {
                // Perform the fragment transaction
                getSupportFragmentManager().beginTransaction()
                        .replace(R.id.fragment_container, selectedFragment) // Replace the existing fragment with the selected one
                        .commit(); // Commit the transaction
            }
            return true; // Return true to indicate that the item selection was handled
        });


        // Load the default fragment (ClockFragment)
        if (savedInstanceState == null) {
            getSupportFragmentManager().beginTransaction()
                    .replace(R.id.fragment_container, new ClockFragment())
                    .commit();
        }
    }

    @Nullable
    private static Fragment getFragment(MenuItem item) {
        Fragment selectedFragment = null; // Declare the selectedFragment variable inside the method to avoid issues with null references.

        if (item.getItemId()==R.id.nav_clock)
        {
                selectedFragment = new ClockFragment(); // Load ClockFragment

        }
        else if  (item.getItemId()==R.id.nav_follow_page) {
            selectedFragment = new FollowPageFragment(); // Load FollowPageFragment
        }
          else if  (item.getItemId()==R.id.nav_change_info)
          {
                selectedFragment = new ChangeInfoFragment(); // Load ChangeInfoFragment
          }
        return selectedFragment;
    }
}
