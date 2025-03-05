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

import java.util.List;

import retrofit2.Call;
import retrofit2.Callback;
import retrofit2.Response;
public class FollowPageActivity extends AppCompatActivity {

    private RecyclerView rvFollowedDomains;
    private FollowedAdapter followedAdapter;
    private ProgressBar progressBar;
    private FirestoreHelper firestoreHelper;
    private EditText editTextDomainName;

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
        rvFollowedDomains = findViewById(R.id.rvFollowedDomains);
        progressBar = findViewById(R.id.progressBar);
        editTextDomainName = findViewById(R.id.editTextDomainName);
        firestoreHelper = new FirestoreHelper(this);

        // Set up RecyclerView
        rvFollowedDomains.setLayoutManager(new LinearLayoutManager(this));
        followedAdapter = new FollowedAdapter(this, firestoreHelper);
        rvFollowedDomains.setAdapter(followedAdapter);

        // Load followed news sources from Firestore
        loadFollowedDomains();
    }

    @Override
    public boolean onSupportNavigateUp() {
        finish();
        return true;
    }

    // Load followed news sources from Firestore
    private void loadFollowedDomains() {
        progressBar.setVisibility(View.VISIBLE);
        firestoreHelper.loadFollowedDomains(followedSources -> {
            progressBar.setVisibility(View.GONE);
            if (followedSources.isEmpty()) {
                Toast.makeText(this, "No followed domains yet!", Toast.LENGTH_SHORT).show();
            }
            followedAdapter.setSources(followedSources);
        });
    }

    // Add domain to favorites when button is clicked
    public void onAddDomainButtonClicked(View view) {
        String domainName = editTextDomainName.getText().toString().trim();

        if (domainName.isEmpty()) {
            Toast.makeText(this, "Please enter a domain name.", Toast.LENGTH_SHORT).show();
            return;
        }

        // Check if the domain is supported by NewsAPI before adding it
        checkIfDomainIsSupported(domainName);
    }

    // Check if the domain is supported by NewsAPI
    private void checkIfDomainIsSupported(final String domainName) {
        // Use the existing NewsRepository to fetch sources
        NewsRepository newsRepository = new NewsRepository();  // Create an instance
        newsRepository.getSources(new Callback<SourcesResponse>() {
            @Override
            public void onResponse(Call<SourcesResponse> call, Response<SourcesResponse> response) {
                if (response.isSuccessful() && response.body() != null) {
                    boolean isDomainValid = false;
                    SourcesResponse sourcesResponse = response.body();

                    // Check if the entered domain exists in the response list of sources
                    for (Source source : sourcesResponse.getSources()) {
                        if (source.getUrl().contains(domainName)) {
                            isDomainValid = true;
                            break;
                        }
                    }

                    // If domain is valid, check if it is already followed by the user
                    if (isDomainValid) {
                        String userId = FirebaseAuth.getInstance().getCurrentUser().getUid();
                        FirebaseFirestore.getInstance()
                                .collection("users")
                                .document(userId)
                                .collection("followedDomains")
                                .document(domainName)
                                .get()
                                .addOnCompleteListener(task -> {
                                    if (task.isSuccessful() && !task.getResult().exists()) {
                                        // If domain is not already followed, add it
                                        firestoreHelper.addFollowedDomain(domainName);
                                        Toast.makeText(FollowPageActivity.this, domainName + " added to favorites!", Toast.LENGTH_SHORT).show();
                                        loadFollowedDomains(); // Reload followed domains
                                        editTextDomainName.setText(""); // Clear input field
                                    } else {
                                        // If domain is already followed, show a message
                                        Toast.makeText(FollowPageActivity.this, domainName + " is already followed!", Toast.LENGTH_SHORT).show();
                                    }
                                });
                    } else {
                        Toast.makeText(FollowPageActivity.this, "Domain is not supported by the API.", Toast.LENGTH_SHORT).show();
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
