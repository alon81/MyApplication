package com.example.myapplication;

import android.os.Bundle;
import android.view.MenuItem;

import androidx.annotation.Nullable;
import androidx.appcompat.app.AppCompatActivity;
import androidx.fragment.app.Fragment;
import com.google.android.material.bottomnavigation.BottomNavigationView;

public class HomeActivity extends AppCompatActivity {

    private BottomNavigationView bottomNavigationView;
    private Fragment selectedFragment = new ClockFragment();

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
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


        if (savedInstanceState == null) {
            getSupportFragmentManager().beginTransaction()
                    .replace(R.id.fragment_container, new ClockFragment())
                    .commit();
        }
    }

    @Nullable
    private static Fragment getFragment(MenuItem item) {
        Fragment selectedFragment = null;

        if (item.getItemId()==R.id.nav_clock)
        {
                selectedFragment = new ClockFragment();

        }
        else if  (item.getItemId()==R.id.nav_follow_page) {
            selectedFragment = new FollowPageFragment();
        }
          else if  (item.getItemId()==R.id.nav_change_info)
          {
                selectedFragment = new ChangeInfoFragment();
          }
        return selectedFragment;
    }
}
