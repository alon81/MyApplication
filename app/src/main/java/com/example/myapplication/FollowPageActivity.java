package com.example.myapplication;

import android.content.Intent;
import android.content.SharedPreferences;
import android.os.Bundle;
import android.text.Editable;
import android.text.TextWatcher;
import android.util.Log;
import android.view.Menu;
import android.view.MenuItem;
import android.widget.Button;
import android.widget.EditText;
import android.widget.LinearLayout;
import android.widget.TextView;
import android.widget.Toast;

import androidx.appcompat.app.AppCompatActivity;
import androidx.appcompat.widget.Toolbar;

import com.google.firebase.auth.FirebaseAuth;

import java.util.HashSet;
import java.util.List;
import java.util.Set;

import retrofit2.Call;
import retrofit2.Callback;
import retrofit2.Response;

public class FollowPageActivity extends AppCompatActivity {

    private LinearLayout newsSourcesContainer;
    private LinearLayout followedSourcesContainer;
    private EditText etSearchSources;
    private Button btnSavePreferences;
    private SharedPreferences sharedPreferences;
    private Set<String> followedSources;
    private static final String TAG = "FollowPageActivity";

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_follow_page);

        // Set up the toolbar
        Toolbar toolbar = findViewById(R.id.toolbar);
        setSupportActionBar(toolbar);

        // Initialize views
        newsSourcesContainer = findViewById(R.id.newsSourcesContainer);
        followedSourcesContainer = findViewById(R.id.followedSourcesContainer);
        etSearchSources = findViewById(R.id.etSearchSources);
        btnSavePreferences = findViewById(R.id.btnSavePreferences);

        // Initialize SharedPreferences
        sharedPreferences = getSharedPreferences("NewsPrefs", MODE_PRIVATE);
        followedSources = sharedPreferences.getStringSet("followed_sources", new HashSet<>());

        // Display followed sources on startup
        displayFollowedSources();

        // Search functionality for news sources
        etSearchSources.addTextChangedListener(new TextWatcher() {
            @Override
            public void beforeTextChanged(CharSequence s, int start, int count, int after) {}

            @Override
            public void onTextChanged(CharSequence s, int start, int before, int count) {
                fetchNewsSources(s.toString());
            }

            @Override
            public void afterTextChanged(Editable s) {}
        });

        // Save preferences when the button is clicked
        btnSavePreferences.setOnClickListener(v -> savePreferences());
    }

    // Fetch the news sources that match the search query
    private void fetchNewsSources(String query) {
        NewsRepository newsRepository = new NewsRepository();
        newsRepository.getSources(new Callback<SourcesResponse>() {
            @Override
            public void onResponse(Call<SourcesResponse> call, Response<SourcesResponse> response) {
                if (response.isSuccessful() && response.body() != null) {
                    List<Source> sources = response.body().getSources();
                    newsSourcesContainer.removeAllViews();

                    for (Source source : sources) {
                        String sourceId = source.getId();
                        String sourceName = source.getName();

                        // Null check for sourceName
                        if (sourceName != null && sourceName.toLowerCase().contains(query.toLowerCase())) {
                            TextView sourceView = new TextView(FollowPageActivity.this);
                            sourceView.setText(sourceName);
                            sourceView.setTag(sourceId);  // Set the source ID as tag for identification

                            // Set OnClickListener to follow/unfollow
                            sourceView.setOnClickListener(v -> toggleFollowSource(sourceId, sourceName));

                            newsSourcesContainer.addView(sourceView);
                        }
                    }
                }
            }

            @Override
            public void onFailure(Call<SourcesResponse> call, Throwable t) {
                Log.e(TAG, "Error fetching news sources: " + t.getMessage());
            }
        });
    }

    // Follow or unfollow a source
    private void toggleFollowSource(String sourceId, String sourceName) {
        if (sourceName == null) {
            sourceName = "Unknown Source";  // Fallback if sourceName is null
        }

        if (followedSources.contains(sourceId)) {
            followedSources.remove(sourceId);
            Toast.makeText(this, "Unfollowed " + sourceName, Toast.LENGTH_SHORT).show();
        } else {
            followedSources.add(sourceId);
            Toast.makeText(this, "Followed " + sourceName, Toast.LENGTH_SHORT).show();
        }
        displayFollowedSources();  // Update the followed sources display
    }

    // Display the followed sources at the bottom of the screen
    private void displayFollowedSources() {
        followedSourcesContainer.removeAllViews();
        for (String sourceId : followedSources) {
            TextView followedSourceView = new TextView(this);
            followedSourceView.setText(sourceId);  // Use sourceId, or consider adding more info
            followedSourcesContainer.addView(followedSourceView);
        }
    }

    // Save followed sources to SharedPreferences
    private void savePreferences() {
        sharedPreferences.edit().putStringSet("followed_sources", followedSources).apply();
        Toast.makeText(this, "Preferences Saved!", Toast.LENGTH_SHORT).show();
    }

    // Adding the menu to the activity
    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        getMenuInflater().inflate(R.menu.menu, menu);
        return true;
    }

    // Handling menu item selection
    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        if (item.getItemId() == R.id.menu_clock) {
            startActivity(new Intent(this, ClockActivity.class));
            return true;
        } else if (item.getItemId() == R.id.menu_change_info) {
            startActivity(new Intent(this, ChangeInfoActivity.class));
            return true;
        } else if (item.getItemId() == R.id.menu_follow_page) {
            Toast.makeText(this, "You are already on the Follow Page.", Toast.LENGTH_SHORT).show();
            return true;
        } else if (item.getItemId() == R.id.menu_logout) {
            FirebaseAuth.getInstance().signOut();
            startActivity(new Intent(this, MainActivity.class));
            finish();
            return true;
        }
        return super.onOptionsItemSelected(item);
    }
}
