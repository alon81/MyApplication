package com.example.myapplication;

import android.content.Intent;
import android.os.Bundle;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.Menu;
import android.view.MenuInflater;
import android.view.MenuItem;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ArrayAdapter;
import android.widget.Button;
import android.widget.EditText;
import android.widget.ProgressBar;
import android.widget.Spinner;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;
import androidx.appcompat.widget.Toolbar;
import androidx.fragment.app.Fragment;
import androidx.fragment.app.FragmentTransaction;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.firestore.DocumentSnapshot;
import com.google.firebase.firestore.FirebaseFirestore;

import java.util.ArrayList;
import java.util.List;

import retrofit2.Call;
import retrofit2.Callback;
import retrofit2.Response;

public class FollowPageFragment extends Fragment {

    private RecyclerView rvFollowedSources;
    private FollowedAdapter followedAdapter;
    private ProgressBar progressBar;
    private FirestoreHelper firestoreHelper;
    private EditText editTextSourceName;
    private Button buttonAddSource;
    private Spinner categorySpinner;
    private Button buttonAddCategorySources;

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {
        View view = inflater.inflate(R.layout.fragment_follow_page, container, false);

        //  toolbar

        buttonAddSource = view.findViewById(R.id.buttonAddSource);
        buttonAddSource.setOnClickListener(this::onAddSourceButtonClicked);
        setHasOptionsMenu(true);

        // Initialize views and helpers
        rvFollowedSources = view.findViewById(R.id.rvFollowedSources);
        progressBar = view.findViewById(R.id.progressBar);
        editTextSourceName = view.findViewById(R.id.editTextSourceName);
        firestoreHelper = new FirestoreHelper(getContext());

        // Set up RecyclerView
        rvFollowedSources.setLayoutManager(new LinearLayoutManager(getContext()));
        followedAdapter = new FollowedAdapter(getContext(), firestoreHelper);
        rvFollowedSources.setAdapter(followedAdapter);

        categorySpinner = view.findViewById(R.id.categorySpinner);
        buttonAddCategorySources = view.findViewById(R.id.buttonAddCategorySources);
        Button removeAllSourcesButton = view.findViewById(R.id.removeAllSourcesButton);
        removeAllSourcesButton.setOnClickListener(v -> {
            removeAllFollowedSources();
        });



// Set up category options
        ArrayAdapter<String> adapter = new ArrayAdapter<>(getContext(),
                android.R.layout.simple_spinner_item,
                new String[]{"business", "entertainment", "general", "health", "science", "sports", "technology"});
        adapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item);
        categorySpinner.setAdapter(adapter);

// Button click logic
        buttonAddCategorySources.setOnClickListener(v -> {
            String selectedCategory = categorySpinner.getSelectedItem().toString();
            addSourcesByCategory(selectedCategory);
        });


        loadFollowedSources();

        return view;
    }
    private void addSourcesByCategory(String category) {
        progressBar.setVisibility(View.VISIBLE);
        NewsRepository newsRepository = new NewsRepository();

        newsRepository.getSources(new Callback<SourcesResponse>() {
            @Override
            public void onResponse(Call<SourcesResponse> call, Response<SourcesResponse> response) {
                progressBar.setVisibility(View.GONE);
                if (response.isSuccessful() && response.body() != null) {
                    List<Source> matchingSources = new ArrayList<>();
                    for (Source source : response.body().getSources()) {
                        if (source.getCategory().equalsIgnoreCase(category)) {
                            matchingSources.add(source);
                        }
                    }

                    if (matchingSources.isEmpty()) {
                        Toast.makeText(getContext(), "No sources found for this category.", Toast.LENGTH_SHORT).show();
                        return;
                    }

                    for (Source source : matchingSources) {
                        firestoreHelper.addFollowedSource(source.getId());
                        loadFollowedSources();

                    }

                    // Refresh the list of followed sources
                    loadFollowedSources();  // This will update the RecyclerView with the new data

                    Toast.makeText(getContext(), "Added " + matchingSources.size() + " sources.", Toast.LENGTH_SHORT).show();
                    loadFollowedSources();
                    } else {
                    Toast.makeText(getContext(), "Failed to fetch sources.", Toast.LENGTH_SHORT).show();
                }
            }

            @Override
            public void onFailure(Call<SourcesResponse> call, Throwable t) {
                progressBar.setVisibility(View.GONE);
                Toast.makeText(getContext(), "Error connecting to API.", Toast.LENGTH_SHORT).show();
            }
        });
    }

    public void removeAllFollowedSources() {
        String userId = FirebaseAuth.getInstance().getCurrentUser().getUid();
        FirebaseFirestore db = FirebaseFirestore.getInstance();

        db.collection("user")
                .document(userId)
                .collection("followedSources")
                .get()
                .addOnSuccessListener(queryDocumentSnapshots -> {
                    for (DocumentSnapshot document : queryDocumentSnapshots) {
                        db.collection("user")
                                .document(userId)
                                .collection("followedSources")
                                .document(document.getId())
                                .delete()
                                .addOnSuccessListener(aVoid -> {
                                    Log.d("FirestoreHelper", "Source removed: " + document.getId());
                                })
                                .addOnFailureListener(e -> {
                                    Log.w("FirestoreHelper", "Error removing source: ", e);
                                });
                    }
                    Toast.makeText(getContext(), "All sources removed.", Toast.LENGTH_SHORT).show();
                    loadFollowedSources(); // Refresh UI
                })
                .addOnFailureListener(e -> {
                    Log.e("FirestoreHelper", "Error getting followed sources", e);
                    Toast.makeText(getContext(), "Failed to remove sources.", Toast.LENGTH_SHORT).show();
                });
    }


    private void loadFollowedSources() {
        progressBar.setVisibility(View.VISIBLE);
        firestoreHelper.loadFollowedSources(followedSources -> {
            progressBar.setVisibility(View.GONE);
            if (followedSources.isEmpty()) {
                Toast.makeText(getContext(), "No followed sources yet!", Toast.LENGTH_SHORT).show();
            }
            followedAdapter.setSources(followedSources);
            followedAdapter.notifyDataSetChanged();  // Notify adapter to refresh the data
        });
    }


    // Add source to favorites when button is clicked
    public void onAddSourceButtonClicked(View view) {
        String sourceName = editTextSourceName.getText().toString().trim();

        if (sourceName.isEmpty()) {
            Toast.makeText(getContext(), "Please enter a source name.", Toast.LENGTH_SHORT).show();
            return;
        }

        // Check if the source is supported by NewsAPI before adding it
        checkIfSourceIsSupported(sourceName);
    }

    // Check if the source is supported by NewsAPI
    private void checkIfSourceIsSupported(final String sourceIdInput) {
        NewsRepository newsRepository = new NewsRepository();
        newsRepository.getSources(new Callback<SourcesResponse>() {
            @Override
            public void onResponse(Call<SourcesResponse> call, Response<SourcesResponse> response) {
                Log.d("FollowPageFragment", "onResponse: " + response.code());

                if (response.isSuccessful() && response.body() != null) {
                    Log.d("FollowPageFragment", "Response body: " + response.body());

                    boolean isSourceValid = false;
                    SourcesResponse sourcesResponse = response.body();

                    for (Source source : sourcesResponse.getSources()) {
                        if (source.getId() != null && source.getId().equalsIgnoreCase(sourceIdInput)) {
                            isSourceValid = true;
                            break;
                        }
                    }

                    if (isSourceValid) {
                        String userId = FirebaseAuth.getInstance().getCurrentUser().getUid();
                        FirebaseFirestore.getInstance()
                                .collection("user")
                                .document(userId)
                                .collection("followedSources")
                                .document(sourceIdInput)
                                .get()
                                .addOnCompleteListener(task -> {
                                    if (task.isSuccessful() && !task.getResult().exists()) {
                                        firestoreHelper.addFollowedSource(sourceIdInput);
                                        Toast.makeText(getContext(), sourceIdInput + " added to favorites!", Toast.LENGTH_SHORT).show();
                                        loadFollowedSources();
                                        editTextSourceName.setText("");
                                    } else {
                                        Toast.makeText(getContext(), sourceIdInput + " is already followed!", Toast.LENGTH_SHORT).show();
                                    }
                                });
                    } else {
                        Toast.makeText(getContext(), "Source ID \"" + sourceIdInput + "\" is not supported by the API.", Toast.LENGTH_SHORT).show();
                    }
                } else {
                    Log.e("FollowPageFragment", "Error: Response not successful or body is null.");
                    Toast.makeText(getContext(), "Error fetching sources.", Toast.LENGTH_SHORT).show();
                }
            }

            @Override
            public void onFailure(Call<SourcesResponse> call, Throwable t) {
                Log.e("FollowPageFragment", "onFailure: " + t.getMessage());
                Toast.makeText(getContext(), "Failed to connect to API.", Toast.LENGTH_SHORT).show();
            }
        });
    }




    private void navigateToFragment(Fragment fragment) {
        if (getActivity() != null) {
            FragmentTransaction transaction = getActivity().getSupportFragmentManager().beginTransaction();
            transaction.replace(R.id.fragment_container, fragment);
            transaction.addToBackStack(null);
            transaction.commit();
        }
    }


}
