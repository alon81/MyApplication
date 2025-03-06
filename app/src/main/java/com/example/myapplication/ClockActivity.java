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
import androidx.recyclerview.widget.DividerItemDecoration;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseUser;
import com.google.firebase.firestore.DocumentReference;
import com.google.firebase.firestore.FirebaseFirestore;

import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Calendar;
import java.util.Date;
import java.util.List;
import java.util.TimeZone;

public class ClockActivity extends AppCompatActivity {

    private TextView txtClock, txtGreeting;
    private ImageView imgBackground;
    private FirebaseAuth fbAuth;
    private FirebaseFirestore db;
    private final Handler clockHandler = new Handler();
    private NewsRepository newsRepository;
    private RecyclerView recyclerViewArticles;
    private NewsAdapter newsAdapter;
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
        imgBackground = findViewById(R.id.imgBackground);
        recyclerViewArticles = findViewById(R.id.recyclerViewArticles);

        // Initialize Firebase
        fbAuth = FirebaseAuth.getInstance();
        db = FirebaseFirestore.getInstance();

        // Initialize repository
        newsRepository = new NewsRepository();

        // Set up RecyclerView
        recyclerViewArticles.setLayoutManager(new LinearLayoutManager(this)); // Layout Manager for vertical list
        recyclerViewArticles.addItemDecoration(new DividerItemDecoration(this, DividerItemDecoration.VERTICAL)); // Optional: Adds item dividers

        // Add padding at the bottom of RecyclerView to prevent last item from getting cut off
        recyclerViewArticles.setPadding(0, 0, 0, (int) (100 * getResources().getDisplayMetrics().density)); // 100dp padding at the bottom (adjust as needed)

        newsAdapter = new NewsAdapter(new ArrayList<>());
        recyclerViewArticles.setAdapter(newsAdapter);

        // Display greeting message, start clock update, and fetch articles
        displayUserGreeting();
        updateClock();
        fetchArticlesFromFollowedSources(); // Fetch articles from followed domains
    }

    /**
     * Fetch articles from followed domains using the NewsRepository.
     */
    private void fetchArticlesFromFollowedSources() {
        // Fetch the articles from the followed sources without any date filtering
        newsRepository.getArticlesByFollowedSources("publishedAt", new ApiCallBack<NewsResponse>() {
            @Override
            public void OnSucces(NewsResponse response) {
                if (response != null && response.getArticles() != null && !response.getArticles().isEmpty()) {
                    List<Article> articles = response.getArticles();
                    Log.d(TAG, "Fetched " + articles.size() + " articles.");
                    newsAdapter.updateArticles(articles);
                } else {
                    Log.d(TAG, "No articles found for the followed sources.");
                    showNoFollowedSourcesMessage();
                }
            }

            @Override
            public void OnFail() {
                Log.e(TAG, "Error fetching articles from followed sources.");
                showErrorMessage();
            }
        });
    }



    /**
     * Show a message if no followed sources are found.
     */
    private void showNoFollowedSourcesMessage() {
        Toast.makeText(ClockActivity.this, "You have no followed news sources.", Toast.LENGTH_SHORT).show();
    }

    /**
     * Show a generic error message if something goes wrong.
     */
    private void showErrorMessage() {
        Toast.makeText(ClockActivity.this, "Failed to load your news sources.", Toast.LENGTH_SHORT).show();
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

    /**
     * Display a greeting message with the user's name.
     */
    private void displayUserGreeting() {
        FirebaseUser currentUser = fbAuth.getCurrentUser();
        if (currentUser == null) {
            Log.e(TAG, "No user is logged in.");
            return;
        }

        String userId = currentUser.getUid();
        DocumentReference userRef = db.collection("user").document(userId);

        userRef.get().addOnCompleteListener(task -> {
            if (task.isSuccessful() && task.getResult() != null) {
                String firstName = task.getResult().getString("firstName");
                String lastName = task.getResult().getString("lastName");
                txtGreeting.setText((firstName != null && lastName != null) ?
                        "Hello " + firstName + " " + lastName : "Hello User!");
            } else {
                Log.e(TAG, "Error fetching user data", task.getException());
                Toast.makeText(this, "Error fetching user data.", Toast.LENGTH_SHORT).show();
            }
        });
    }

    /**
     * Continuously update the clock display in real time.
     */
    private void updateClock() {
        clockHandler.postDelayed(new Runnable() {
            @Override
            public void run() {
                Calendar calendar = Calendar.getInstance(TimeZone.getTimeZone("Asia/Jerusalem"));
                SimpleDateFormat timeFormat = new SimpleDateFormat("HH:mm:ss");
                txtClock.setText(timeFormat.format(calendar.getTime()));
                updateBackground(calendar.get(Calendar.HOUR_OF_DAY)); // Adjust background based on the time of day
                clockHandler.postDelayed(this, 1000); // Continue updating every second
            }
        }, 0);
    }

    /**
     * Update the background image based on the time of day.
     */
    private void updateBackground(int hourOfDay) {
        if (hourOfDay >= 7 && hourOfDay < 15) {
            imgBackground.setImageResource(R.drawable.morning_image); // Morning image
        } else if (hourOfDay >= 15 && hourOfDay < 19) {
            imgBackground.setImageResource(R.drawable.afternoon_image); // Afternoon image
        } else {
            imgBackground.setImageResource(R.drawable.night_image); // Night image
        }
    }

    @Override
    protected void onResume() {
        super.onResume();
        fetchArticlesFromFollowedSources(); // Refresh articles when returning to this activity
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
        clockHandler.removeCallbacksAndMessages(null); // Stop clock updates when activity is destroyed
    }
}
