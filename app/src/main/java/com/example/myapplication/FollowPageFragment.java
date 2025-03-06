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
import android.widget.Button;
import android.widget.EditText;
import android.widget.ProgressBar;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;
import androidx.appcompat.widget.Toolbar;
import androidx.fragment.app.Fragment;
import androidx.fragment.app.FragmentTransaction;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.firestore.FirebaseFirestore;

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
    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {
        View view = inflater.inflate(R.layout.fragment_follow_page, container, false);

        // Set up the toolbar
        Toolbar toolbar = view.findViewById(R.id.toolbar);
        ((AppCompatActivity) getActivity()).setSupportActionBar(toolbar);
        if (((AppCompatActivity) getActivity()).getSupportActionBar() != null) {
            ((AppCompatActivity) getActivity()).getSupportActionBar().setTitle("Follow News Sources");
        }
        buttonAddSource = view.findViewById(R.id.buttonAddSource);
        buttonAddSource.setOnClickListener(this::onAddSourceButtonClicked);
        // Enable options menu
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

        // Load followed news sources from Firestore
        loadFollowedSources();

        return view;
    }

    // Load followed news sources from Firestore
    private void loadFollowedSources() {
        progressBar.setVisibility(View.VISIBLE);
        firestoreHelper.loadFollowedSources(followedSources -> {
            progressBar.setVisibility(View.GONE);
            if (followedSources.isEmpty()) {
                Toast.makeText(getContext(), "No followed sources yet!", Toast.LENGTH_SHORT).show();
            }
            followedAdapter.setSources(followedSources);
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
    private void checkIfSourceIsSupported(final String sourceName) {
        NewsRepository newsRepository = new NewsRepository();
        newsRepository.getSources(new Callback<SourcesResponse>() {
            @Override
            public void onResponse(Call<SourcesResponse> call, Response<SourcesResponse> response) {
                Log.d("FollowPageFragment", "onResponse: " + response.code());  // Log response code
                if (response.isSuccessful() && response.body() != null) {
                    Log.d("FollowPageFragment", "Response body: " + response.body());  // Log the response body

                    boolean isSourceValid = false;
                    SourcesResponse sourcesResponse = response.body();

                    for (Source source : sourcesResponse.getSources()) {
                        if (source.getUrl().contains(sourceName)) {
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
                                .document(sourceName)
                                .get()
                                .addOnCompleteListener(task -> {
                                    if (task.isSuccessful() && !task.getResult().exists()) {
                                        firestoreHelper.addFollowedSource(sourceName);
                                        Toast.makeText(getContext(), sourceName + " added to favorites!", Toast.LENGTH_SHORT).show();
                                        loadFollowedSources();
                                        editTextSourceName.setText("");
                                    } else {
                                        Toast.makeText(getContext(), sourceName + " is already followed!", Toast.LENGTH_SHORT).show();
                                    }
                                });
                    } else {
                        Toast.makeText(getContext(), "Source is not supported by the API.", Toast.LENGTH_SHORT).show();
                    }
                } else {
                    Log.e("FollowPageFragment", "Error: Response not successful or body is null.");
                    Toast.makeText(getContext(), "Error fetching sources.", Toast.LENGTH_SHORT).show();
                }
            }

            @Override
            public void onFailure(Call<SourcesResponse> call, Throwable t) {
                Log.e("FollowPageFragment", "onFailure: " + t.getMessage());  // Log error message
                Toast.makeText(getContext(), "Failed to connect to API.", Toast.LENGTH_SHORT).show();
            }
        });
    }



    @Override
    public void onCreateOptionsMenu(Menu menu, MenuInflater inflater) {
        super.onCreateOptionsMenu(menu, inflater);
        inflater.inflate(R.menu.menu, menu); // Inflate your menu
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        Log.d("FollowPageFragment", "Menu item selected: " + item.getItemId());  // Debug log

        if (item.getItemId() == R.id.menu_follow_page) {
            Toast.makeText(getContext(), "You are already on the Follow Page.", Toast.LENGTH_SHORT).show();
            return true;
        } else if (item.getItemId() == R.id.menu_change_info) {
            navigateToFragment(new ChangeInfoFragment());
            return true;
        } else if (item.getItemId() == R.id.menu_clock) {
            navigateToFragment(new ClockFragment());
            return true;
        } else if (item.getItemId() == R.id.menu_logout) {
            FirebaseAuth.getInstance().signOut();
            Intent logoutIntent = new Intent(getContext(), MainActivity.class);
            startActivity(logoutIntent);
            getActivity().finish();
            return true;
        }
        return super.onOptionsItemSelected(item);
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
