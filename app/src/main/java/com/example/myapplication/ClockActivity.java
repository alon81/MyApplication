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
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseUser;
import com.google.firebase.firestore.DocumentReference;
import com.google.firebase.firestore.FirebaseFirestore;

import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Calendar;
import java.util.List;
import java.util.TimeZone;

import retrofit2.Call;
import retrofit2.Callback;
import retrofit2.Response;

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
        recyclerViewArticles.setLayoutManager(new LinearLayoutManager(this));
        newsAdapter = new NewsAdapter(new ArrayList<>());
        recyclerViewArticles.setAdapter(newsAdapter);

        // Display greeting message, start clock update, and fetch user preferences
        displayUserGreeting();
        updateClock();
        fetchFollowedDomains();  // Call the new method to fetch followed domains
    }

    private void fetchFollowedDomains() {
        FirebaseUser user = FirebaseAuth.getInstance().getCurrentUser();
        if (user == null) {
            Log.d("ClockActivity", "User not authenticated");
            return;
        }
        String userId = user.getUid();

        // Corrected the collection name from "user" to "users" based on your Firestore structure
        FirebaseFirestore.getInstance().collection("users").document(userId)
                .get()
                .addOnSuccessListener(documentSnapshot -> {
                    if (documentSnapshot.exists()) {
                        List<String> followedDomains = (List<String>) documentSnapshot.get("followedDomains");
                        Log.d("ClockActivity", "Raw Firestore Data: " + documentSnapshot.getData());
                        if (followedDomains == null || followedDomains.isEmpty()) {
                            Log.d("ClockActivity", "No domains found for this user.");
                            // Optionally show a message to the user
                            showNoFollowedDomainsMessage();
                        } else {
                            Log.d("ClockActivity", "Followed Domains: " + followedDomains);
                            fetchArticlesFromDomains(followedDomains);  // Fetch articles if domains exist
                        }
                    } else {
                        Log.d("ClockActivity", "Document does not exist for user: " + userId);
                        // Handle case where user document is missing
                        showNoFollowedDomainsMessage();
                    }
                })
                .addOnFailureListener(e -> {
                    Log.e("ClockActivity", "Error fetching domains", e);
                    // Optionally show error to user
                    showErrorMessage();
                });
    }

    private void showNoFollowedDomainsMessage() {
        // Display a message to the user if they have no followed domains
        Toast.makeText(ClockActivity.this, "You have no followed news sources.", Toast.LENGTH_SHORT).show();
    }

    private void showErrorMessage() {
        // Display a message to the user in case of an error fetching the domains
        Toast.makeText(ClockActivity.this, "Failed to load your followed news sources.", Toast.LENGTH_SHORT).show();
    }

    private void fetchArticlesFromDomains(List<String> followedDomains) {
        String domainQuery = String.join(",", followedDomains);
        fetchArticles(domainQuery);
    }

    private void fetchArticles(String domains) {
        Log.d(TAG, "Fetching articles for domains: " + domains); // Log the requested domains

        newsRepository.getArticlesByDomains(domains, "publishedAt", new ApiCallBack<NewsResponse>() {
            @Override
            public void OnSucces(NewsResponse response) {
                if (response != null) {
                    Log.d(TAG, "API Response: " + response.toString()); // Log full API response

                    List<Article> articles = response.getArticles();
                    if (articles != null && !articles.isEmpty()) {
                        Log.d(TAG, "Fetched " + articles.size() + " articles.");
                        newsAdapter.updateArticles(articles); // Update adapter with articles
                    } else {
                        Log.d(TAG, "No articles found for the given domains.");
                    }
                } else {
                    Log.e(TAG, "Error: API returned null response.");
                }
            }

            @Override
            public void OnFail() {
                Log.e(TAG, "Error fetching articles from the API.");
            }
        });
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
        String userId = fbAuth.getCurrentUser() != null ? fbAuth.getCurrentUser().getUid() : null;
        if (userId != null) {
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
        } else {
            Log.e(TAG, "No user is logged in.");
            // Handle no user logged in case (maybe redirect to login screen)
        }
    }

    private void updateClock() {
        clockHandler.postDelayed(new Runnable() {
            @Override
            public void run() {
                Calendar calendar = Calendar.getInstance(TimeZone.getTimeZone("Asia/Jerusalem"));
                SimpleDateFormat timeFormat = new SimpleDateFormat("HH:mm:ss");
                txtClock.setText(timeFormat.format(calendar.getTime()));
                updateBackground(calendar.get(Calendar.HOUR_OF_DAY));
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

    @Override
    protected void onResume() {
        super.onResume();
        fetchFollowedDomains(); // Ensure fetching followed domains every time the activity is resumed
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
        clockHandler.removeCallbacksAndMessages(null); // Ensure we stop the clock updates when the activity is destroyed
    }
}
