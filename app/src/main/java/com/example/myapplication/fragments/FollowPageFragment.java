package com.example.myapplication.fragments;

import android.os.Bundle;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Button;
import android.widget.EditText;
import android.widget.ImageButton;
import android.widget.PopupMenu;
import android.widget.Toast;

import androidx.fragment.app.Fragment;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.example.myapplication.R;
import com.example.myapplication.api.NewsRepository;
import com.example.myapplication.help.FirestoreHelper;
import com.example.myapplication.help.FollowedAdapter;
import com.example.myapplication.objects.Source;
import com.example.myapplication.objects.SourcesResponse;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.firestore.FirebaseFirestore;

import java.lang.reflect.Field;
import java.lang.reflect.Method;
import java.util.ArrayList;
import java.util.List;

import retrofit2.Call;
import retrofit2.Callback;
import retrofit2.Response;
 public class FollowPageFragment extends Fragment {


    private RecyclerView rvFollowedSources;
    private FollowedAdapter followedAdapter;
    private FirestoreHelper firestoreHelper;
    private EditText editTextSourceName;
    private Button buttonAddSource;
    private String selectedCategory;
    private Button buttonAddCategorySources;
    private ImageButton buttonSelectCategory;

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {
        View view = inflater.inflate(R.layout.fragment_follow_page, container, false);

        buttonAddSource = view.findViewById(R.id.buttonAddSource);
        buttonAddSource.setOnClickListener(this::onAddSourceButtonClicked);

        buttonSelectCategory = view.findViewById(R.id.imageButtonSelectCategory);
        buttonAddCategorySources = view.findViewById(R.id.buttonAddCategorySources);
        Button removeAllSourcesButton = view.findViewById(R.id.removeAllSourcesButton);

        rvFollowedSources = view.findViewById(R.id.rvFollowedSources);
        editTextSourceName = view.findViewById(R.id.editTextSourceName);
        firestoreHelper = new FirestoreHelper(getContext());

        rvFollowedSources.setLayoutManager(new LinearLayoutManager(getContext()));
        followedAdapter = new FollowedAdapter(getContext(), firestoreHelper);
        rvFollowedSources.setAdapter(followedAdapter);

        removeAllSourcesButton.setOnClickListener(v -> firestoreHelper.removeAllFollowedSources());

        buttonSelectCategory.setOnClickListener(v -> {
            PopupMenu popup = new PopupMenu(getContext(), buttonSelectCategory);
            popup.getMenuInflater().inflate(R.menu.menu_filter, popup.getMenu());

            try {
                Field[] fields = popup.getClass().getDeclaredFields();
                for (Field field : fields) {
                    if ("mPopup".equals(field.getName())) {
                        field.setAccessible(true);
                        Object menuPopupHelper = field.get(popup);
                        Class<?> classPopupHelper = Class.forName(menuPopupHelper.getClass().getName());
                        Method setForceShowIcon = classPopupHelper.getMethod("setForceShowIcon", boolean.class);
                        setForceShowIcon.invoke(menuPopupHelper, true);
                        break;
                    }
                }
            } catch (Exception e) {
                Log.e("PopupMenu", "Error forcing menu icons to show", e);
            }


            popup.setOnMenuItemClickListener(item -> {
                if (item.getItemId() == R.id.category_business) {
                    selectedCategory = "business";
                } else if (item.getItemId() == R.id.category_entertainment) {
                    selectedCategory = "entertainment";
                } else if (item.getItemId() == R.id.category_general) {
                    selectedCategory = "general";
                } else if (item.getItemId() == R.id.category_health) {
                    selectedCategory = "health";
                } else if (item.getItemId() == R.id.category_science) {
                    selectedCategory = "science";
                } else if (item.getItemId() == R.id.category_sports) {
                    selectedCategory = "sports";
                } else if (item.getItemId() == R.id.category_technology) {
                    selectedCategory = "technology";
                }

                if (getContext() != null) {
                    Toast.makeText(getContext(), "Selected: " + selectedCategory, Toast.LENGTH_SHORT).show();
                }
                return true;
            });

            popup.show();
        });

        buttonAddCategorySources.setOnClickListener(v -> {
            if (selectedCategory != null) {
                addSourcesByCategory(selectedCategory);
                if (getContext() != null) {
                    Toast.makeText(getContext(), "Sources added for category: " + selectedCategory, Toast.LENGTH_SHORT).show();
                }
            } else {
                if (getContext() != null) {
                    Toast.makeText(getContext(), "Please select a category first", Toast.LENGTH_SHORT).show();
                }
            }
        });

        loadFollowedSources();

        return view;
    }

    private void addSourcesByCategory(String category) {
        NewsRepository newsRepository = new NewsRepository();

        newsRepository.getSources(new Callback<SourcesResponse>() {
            @Override
            public void onResponse(Call<SourcesResponse> call, Response<SourcesResponse> response) {
                if (response.isSuccessful() && response.body() != null) {
                    List<Source> matchingSources = new ArrayList<>();
                    for (Source source : response.body().getSources()) {
                        if (source.getCategory().equalsIgnoreCase(category)) {
                            matchingSources.add(source);
                        }
                    }

                    if (matchingSources.isEmpty()) {
                        if (getContext() != null) {
                            Toast.makeText(getContext(), "No sources found for this category.", Toast.LENGTH_SHORT).show();
                        }
                        return;
                    }

                    for (Source source : matchingSources) {
                        firestoreHelper.addFollowedSource(source.getId());
                        loadFollowedSources();
                    }

                    loadFollowedSources();

                    if (getContext() != null) {
                        Toast.makeText(getContext(), "Added " + matchingSources.size() + " sources.", Toast.LENGTH_SHORT).show();
                    }
                } else {
                    if (getContext() != null) {
                        Toast.makeText(getContext(), "Failed to fetch sources.", Toast.LENGTH_SHORT).show();
                    }
                }
            }

            @Override
            public void onFailure(Call<SourcesResponse> call, Throwable t) {
                if (getContext() != null) {
                    Toast.makeText(getContext(), "Error connecting to API.", Toast.LENGTH_SHORT).show();
                }
            }
        });
    }

    private void loadFollowedSources() {
        firestoreHelper.loadFollowedSources(followedSources -> {
            if (getActivity() != null && isAdded()) {
                if (followedSources.isEmpty()) {
                    if (getContext() != null) {
                        Toast.makeText(getContext(), "No followed sources yet!", Toast.LENGTH_SHORT).show();
                    }
                }
                followedAdapter.setSources(followedSources);
                followedAdapter.notifyDataSetChanged();
            }
        });
    }

    // Add source to favorites when button is clicked
    public void onAddSourceButtonClicked(View view) {
        String sourceName = editTextSourceName.getText().toString().trim();

        if (sourceName.isEmpty()) {
            if (getContext() != null) {
                Toast.makeText(getContext(), "Please enter a source name.", Toast.LENGTH_SHORT).show();
            }
            return;
        }

        checkIfSourceIsSupported(sourceName);
    }

    // Check if the source is supported by NewsAPI
    private void checkIfSourceIsSupported(final String sourceIdInput) {
        NewsRepository newsRepository = new NewsRepository();
        newsRepository.getSources(new Callback<>() {
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
                                        if (getContext() != null) {
                                            Toast.makeText(getContext(), sourceIdInput + " added to favorites!", Toast.LENGTH_SHORT).show();
                                        }
                                        loadFollowedSources();
                                        editTextSourceName.setText("");
                                    } else {
                                        if (getContext() != null) {
                                            Toast.makeText(getContext(), sourceIdInput + " is already followed!", Toast.LENGTH_SHORT).show();
                                        }
                                    }
                                });
                    } else {
                        if (getContext() != null) {
                            Toast.makeText(getContext(), "Source ID \"" + sourceIdInput + "\" is not supported by the API.", Toast.LENGTH_SHORT).show();
                        }
                    }
                } else {
                    Log.e("FollowPageFragment", "Error: Response not successful or body is null.");
                    if (getContext() != null) {
                        Toast.makeText(getContext(), "Error fetching sources.", Toast.LENGTH_SHORT).show();
                    }
                }
            }

            @Override
            public void onFailure(Call<SourcesResponse> call, Throwable t) {
                Log.e("FollowPageFragment", "onFailure: " + t.getMessage());
                if (getContext() != null) {
                    Toast.makeText(getContext(), "Failed to connect to API.", Toast.LENGTH_SHORT).show();
                }
            }
        });
    }
}
