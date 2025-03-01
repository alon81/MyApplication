package com.example.myapplication;

import android.content.Intent;
import android.os.Bundle;
import android.os.Handler;
import android.view.Menu;
import android.view.MenuItem;
import android.widget.Button;
import android.widget.ImageView;
import android.widget.TextView;
import android.widget.Toast;
import androidx.appcompat.widget.Toolbar;


import androidx.appcompat.app.AppCompatActivity;

import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.firestore.DocumentReference;
import com.google.firebase.firestore.FirebaseFirestore;

import java.text.SimpleDateFormat;
import java.util.Calendar;
import java.util.TimeZone;

public class ClockActivity extends AppCompatActivity {

    private TextView txtClock, txtGreeting;
    private ImageView imgBackground;
    private Button btnLogout;
    private FirebaseAuth fbAuth;
    private FirebaseFirestore db;
    private final Handler clockHandler = new Handler();

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_clock);

        // Set up the toolbar as the action bar
        Toolbar toolbar = findViewById(R.id.toolbar);
        setSupportActionBar(toolbar);

        // Initialize views
        txtClock = findViewById(R.id.txtClock);
        txtGreeting = findViewById(R.id.txtGreeting);
        imgBackground = findViewById(R.id.imgBackground);
        //btnLogout = findViewById(R.id.btnLogout);

        // Initialize Firebase instances
        fbAuth = FirebaseAuth.getInstance();
        db = FirebaseFirestore.getInstance();

        // Display greeting and time
        displayUserGreeting();
        updateClock();
        /*
        // Logout button listener
        btnLogout.setOnClickListener(v -> {
            FirebaseAuth.getInstance().signOut();
            Intent intent = new Intent(ClockActivity.this, MainActivity.class);
            startActivity(intent);
            finish();
        });*/
    }

    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        getMenuInflater().inflate(R.menu.menu, menu); // Inflate the menu
        return true;
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {

        if (item.getItemId() == R.id.menu_clock) {
            // You are already on the Clock page
            Toast.makeText(this, "You are already on the Clock page.", Toast.LENGTH_SHORT).show();
            return true;
        } else if (item.getItemId() == R.id.menu_change_info) {
            // Go to ChangeInfoActivity
            Intent changeInfoIntent = new Intent(this, ChangeInfoActivity.class);
            startActivity(changeInfoIntent);
            return true;
        } else if (item.getItemId() == R.id.menu_logout) {

            // Handle logout
            FirebaseAuth.getInstance().signOut();
            Intent logoutIntent = new Intent(this, MainActivity.class);
            startActivity(logoutIntent);
            finish();
            return true;
        }
        return super.onOptionsItemSelected(item);
    }

    private void displayUserGreeting() {
        String userId = fbAuth.getCurrentUser().getUid();

        // Fetch user data from Firebase 'user' Firestore collection
        DocumentReference userRef = db.collection("user").document(userId);
        userRef.get().addOnCompleteListener(task -> {
            if (task.isSuccessful()) {
                String firstName = task.getResult().getString("firstName");
                String lastName = task.getResult().getString("lastName");

                // Display personalized greeting
                if (firstName != null && lastName != null) {
                    txtGreeting.setText("Hello " + firstName + " " + lastName);
                } else {
                    Toast.makeText(ClockActivity.this, "User data incomplete", Toast.LENGTH_SHORT).show();
                }
            } else {
                Toast.makeText(ClockActivity.this, "Error fetching user data", Toast.LENGTH_SHORT).show();
            }
        });
    }

    private void updateClock() {
        clockHandler.postDelayed(() -> {
            // Get current time in Israel timezone
            Calendar calendar = Calendar.getInstance(TimeZone.getTimeZone("Asia/Jerusalem"));
            SimpleDateFormat timeFormat = new SimpleDateFormat("HH:mm:ss");
            String time = timeFormat.format(calendar.getTime());

            // Update the clock TextView
            txtClock.setText(time);

            // Set background based on the time of day
            updateBackground(calendar.get(Calendar.HOUR_OF_DAY));

            // Schedule the next update after 1 second
            clockHandler.postDelayed(this::updateClock, 1000);
        }, 0);
    }


    private void updateBackground(int hourOfDay) {
        // Set background image based on time of day
        if (hourOfDay >= 7 && hourOfDay < 15) {
            // Morning (7:00 AM to 3:00 PM)
            imgBackground.setImageResource(R.drawable.morning_image); // Replace with your actual image
        } else if (hourOfDay >= 15 && hourOfDay < 19) {
            // Afternoon (3:00 PM to 7:00 PM)
            imgBackground.setImageResource(R.drawable.afternoon_image); // Replace with your actual image
        } else {
            // Night (7:00 PM to 7:00 AM)
            imgBackground.setImageResource(R.drawable.night_image); // Replace with your actual image
        }
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
        // Stop the clock handler to prevent memory leaks
        clockHandler.removeCallbacksAndMessages(null);
    }
}
