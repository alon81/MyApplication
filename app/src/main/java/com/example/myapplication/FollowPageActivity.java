package com.example.myapplication;

import android.content.Context;
import android.content.Intent;
import android.os.Bundle;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.widget.EditText;
import android.widget.ProgressBar;
import android.widget.Toast;

import androidx.appcompat.app.AppCompatActivity;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;
import androidx.appcompat.widget.Toolbar;

import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.firestore.FirebaseFirestore;

import retrofit2.Call;
import retrofit2.Callback;
import retrofit2.Response;

public class FollowPageActivity extends AppCompatActivity {

    private RecyclerView rvFollowedSources;
    private FollowedAdapter followedAdapter;
    private ProgressBar progressBar;
    private FirestoreHelper firestoreHelper;
    private EditText editTextSourceName;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_follow_page);

        // Set up the toolbar
        Toolbar toolbar = findViewById(R.id.toolbar);
        setSupportActionBar(toolbar);
        if (getSupportActionBar() != null) {
            getSupportActionBar().setTitle("Follow News Sources");
            getSupportActionBar().setDisplayHomeAsUpEnabled(true);
        }

        // Initialize views and helpers
        rvFollowedSources = findViewById(R.id.rvFollowedSources);
        progressBar = findViewById(R.id.progressBar);
        editTextSourceName = findViewById(R.id.editTextSourceName);
        firestoreHelper = new FirestoreHelper(this);

        // Set up RecyclerView
        rvFollowedSources.setLayoutManager(new LinearLayoutManager(this));
        followedAdapter = new FollowedAdapter(this, firestoreHelper);
        rvFollowedSources.setAdapter(followedAdapter);

        // Load followed news sources from Firestore
        loadFollowedSources();
    }

    @Override
    public boolean onSupportNavigateUp() {
        finish();
        return true;
    }

    // Load followed news sources from Firestore
    private void loadFollowedSources() {
        progressBar.setVisibility(View.VISIBLE);
        firestoreHelper.loadFollowedSources(followedSources -> {
            progressBar.setVisibility(View.GONE);
            if (followedSources.isEmpty()) {
                Toast.makeText(this, "No followed sources yet!", Toast.LENGTH_SHORT).show();
            }
            followedAdapter.setSources(followedSources);
        });
    }

    // Add source to favorites when button is clicked
    public void onAddSourceButtonClicked(View view) {
        String sourceName = editTextSourceName.getText().toString().trim();

        if (sourceName.isEmpty()) {
            Toast.makeText(this, "Please enter a source name.", Toast.LENGTH_SHORT).show();
            return;
        }

        // Check if the source is supported by NewsAPI before adding it
        checkIfSourceIsSupported(sourceName);
    }

    // Check if the source is supported by NewsAPI
    private void checkIfSourceIsSupported(final String sourceName) {
        NewsRepository newsRepository = new NewsRepository();  // Create an instance
        newsRepository.getSources(new Callback<SourcesResponse>() {
            @Override
            public void onResponse(Call<SourcesResponse> call, Response<SourcesResponse> response) {
                if (response.isSuccessful() && response.body() != null) {
                    boolean isSourceValid = false;
                    SourcesResponse sourcesResponse = response.body();

                    // Check if the entered source exists in the response list of sources
                    for (Source source : sourcesResponse.getSources()) {
                        if (source.getUrl().contains(sourceName)) {
                            isSourceValid = true;
                            break;
                        }
                    }

                    // If source is valid, check if it is already followed by the user
                    if (isSourceValid) {
                        String userId = FirebaseAuth.getInstance().getCurrentUser().getUid();
                        FirebaseFirestore.getInstance()
                                .collection("user")
                                .document(userId)  // Using the user's UID
                                .collection("followedSources")
                                .document(sourceName)  // Use source as document reference
                                .get()
                                .addOnCompleteListener(task -> {
                                    if (task.isSuccessful() && !task.getResult().exists()) {
                                        // If source is not already followed, add it
                                        firestoreHelper.addFollowedSource(sourceName);
                                        Toast.makeText(FollowPageActivity.this, sourceName + " added to favorites!", Toast.LENGTH_SHORT).show();
                                        loadFollowedSources(); // Reload followed sources
                                        editTextSourceName.setText(""); // Clear input field
                                    } else {
                                        // If source is already followed, show a message
                                        Toast.makeText(FollowPageActivity.this, sourceName + " is already followed!", Toast.LENGTH_SHORT).show();
                                    }
                                });
                    } else {
                        Toast.makeText(FollowPageActivity.this, "Source is not supported by the API.", Toast.LENGTH_SHORT).show();
                    }
                } else {
                    Toast.makeText(FollowPageActivity.this, "Error fetching sources.", Toast.LENGTH_SHORT).show();
                }
            }

            @Override
            public void onFailure(Call<SourcesResponse> call, Throwable t) {
                Toast.makeText(FollowPageActivity.this, "Failed to connect to API.", Toast.LENGTH_SHORT).show();
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
            // Go to the Clock page
            startActivity(new Intent(this, ClockActivity.class));
            return true;
        } else if (item.getItemId() == R.id.menu_change_info) {
            // Go to Change Info page
            startActivity(new Intent(this, ChangeInfoActivity.class));
            return true;
        } else if (item.getItemId() == R.id.menu_follow_page) {
            // You're already on the FollowPage, so just show a toast
            Toast.makeText(this, "You are already on the Follow page.", Toast.LENGTH_SHORT).show();
            return true;
        } else if (item.getItemId() == R.id.menu_logout) {
            // Log out the user and navigate to MainActivity (Login page)
            FirebaseAuth.getInstance().signOut();
            startActivity(new Intent(this, MainActivity.class));
            finish();
            return true;
        }
        return super.onOptionsItemSelected(item);
    }
}
