package com.example.myapplication;

import android.content.Intent;
import android.os.Bundle;
import android.os.Handler;
import android.util.Log;
import android.view.Menu;
import android.view.MenuItem;
import android.widget.ImageView;
import android.widget.TextView;
import android.widget.Toast;

import androidx.appcompat.app.AppCompatActivity;
import androidx.appcompat.widget.Toolbar;

import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.firestore.DocumentReference;
import com.google.firebase.firestore.FirebaseFirestore;

import java.text.SimpleDateFormat;
import java.util.Calendar;
import java.util.List;
import java.util.TimeZone;

import retrofit2.Call;
import retrofit2.Callback;
import retrofit2.Response;

public class ClockActivity extends AppCompatActivity {

    private TextView txtClock, txtGreeting, txtArticles;
    private ImageView imgBackground;
    private FirebaseAuth fbAuth;
    private FirebaseFirestore db;
    private final Handler clockHandler = new Handler();
    private NewsRepository newsRepository;
    private static final String TAG = "ClockActivity";

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_clock);

        // Set up the toolbar
        Toolbar toolbar = findViewById(R.id.toolbar);
        setSupportActionBar(toolbar);

        // Initialize UI components
        txtClock = findViewById(R.id.txtClock);
        txtGreeting = findViewById(R.id.txtGreeting);
        txtArticles = findViewById(R.id.txtArticles);
        imgBackground = findViewById(R.id.imgBackground);

        // Initialize Firebase
        fbAuth = FirebaseAuth.getInstance();
        db = FirebaseFirestore.getInstance();

        // Initialize repository
        newsRepository = new NewsRepository();

        // Display greeting message, start clock update, and fetch user preferences
        displayUserGreeting();
        updateClock();
        fetchUserPreferences();
    }

    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        getMenuInflater().inflate(R.menu.menu, menu);
        return true;
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        if (item.getItemId() == R.id.menu_clock) {
            Toast.makeText(this, "You are already on the Clock page.", Toast.LENGTH_SHORT).show();
            return true;
        } else if (item.getItemId() == R.id.menu_change_info) {
            startActivity(new Intent(this, ChangeInfoActivity.class));
            return true;
        } else if (item.getItemId() == R.id.menu_follow_page) {
            startActivity(new Intent(this, FollowPageActivity.class));
            return true;
        } else if (item.getItemId() == R.id.menu_logout) {
            fbAuth.signOut();
            startActivity(new Intent(this, MainActivity.class));
            finish();
            return true;
        }
        return super.onOptionsItemSelected(item);
    }

    private void displayUserGreeting() {
        String userId = fbAuth.getCurrentUser().getUid();
        DocumentReference userRef = db.collection("user").document(userId);
        userRef.get().addOnCompleteListener(task -> {
            if (task.isSuccessful() && task.getResult() != null) {
                String firstName = task.getResult().getString("firstName");
                String lastName = task.getResult().getString("lastName");
                txtGreeting.setText(firstName != null && lastName != null
                        ? "Hello " + firstName + " " + lastName
                        : "Hello User!");
            } else {
                Toast.makeText(this, "Error fetching user data", Toast.LENGTH_SHORT).show();
            }
        });
    }

    private void updateClock() {
        clockHandler.postDelayed(new Runnable() {
            @Override
            public void run() {
                // Get current time in Jerusalem timezone
                Calendar calendar = Calendar.getInstance(TimeZone.getTimeZone("Asia/Jerusalem"));
                SimpleDateFormat timeFormat = new SimpleDateFormat("HH:mm:ss");
                txtClock.setText(timeFormat.format(calendar.getTime()));

                // Update background based on time of day
                updateBackground(calendar.get(Calendar.HOUR_OF_DAY));

                // Update every second
                clockHandler.postDelayed(this, 1000);
            }
        }, 0);
    }

    private void updateBackground(int hourOfDay) {
        if (hourOfDay >= 7 && hourOfDay < 15) {
            imgBackground.setImageResource(R.drawable.morning_image);
        } else if (hourOfDay >= 15 && hourOfDay < 19) {
            imgBackground.setImageResource(R.drawable.afternoon_image);
        } else {
            imgBackground.setImageResource(R.drawable.night_image);
        }
    }

    private void fetchUserPreferences() {
        String userId = fbAuth.getCurrentUser().getUid();
        DocumentReference userRef = db.collection("user").document(userId);
        userRef.get().addOnCompleteListener(task -> {
            if (task.isSuccessful() && task.getResult() != null) {
                List<String> domains = (List<String>) task.getResult().get("domains");
                if (domains != null && !domains.isEmpty()) {
                    String domainQuery = String.join(",", domains);
                    fetchArticles(domainQuery);
                }
            } else {
                Log.e(TAG, "Error fetching user preferences");
            }
        });
    }
    // In ClockActivity.java
    private void fetchArticles(String domains) {
        newsRepository.getArticlesByDomains(domains, new ApiCallBack<NewsResponse>() {
            @Override
            public void OnSucces(NewsResponse response) {
                if (response != null && response.getArticles() != null) {
                    StringBuilder articleText = new StringBuilder();
                    for (Article article : response.getArticles()) {
                        articleText.append(article.getTitle()).append("\n");
                    }
                    txtArticles.setText(articleText.toString());
                }
            }

            @Override
            public void OnFail() {
                Log.e(TAG, "Error fetching articles");
            }
        });
    }




    @Override
    protected void onDestroy() {
        super.onDestroy();
        clockHandler.removeCallbacksAndMessages(null); // Ensure we stop the clock updates when the activity is destroyed
    }
}
