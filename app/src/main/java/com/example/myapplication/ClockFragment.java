package com.example.myapplication;

import android.content.Intent;
import android.os.Bundle;
import android.os.Handler;
import android.speech.tts.TextToSpeech;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.Menu;
import android.view.MenuInflater;
import android.view.MenuItem;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageView;
import android.widget.TextView;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.appcompat.app.AppCompatActivity;
import androidx.appcompat.widget.Toolbar;
import androidx.fragment.app.Fragment;
import androidx.fragment.app.FragmentTransaction;
import androidx.recyclerview.widget.DividerItemDecoration;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseUser;
import com.google.firebase.firestore.DocumentReference;
import com.google.firebase.firestore.DocumentSnapshot;
import com.google.firebase.firestore.FirebaseFirestore;

import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Calendar;
import java.util.List;
import java.util.Locale;
import java.util.TimeZone;

public class ClockFragment extends Fragment {

    private List<String> favoriteUrls = new ArrayList<>();

    private TextView txtClock, txtGreeting;
    private ImageView imgBackground;
    private RecyclerView recyclerViewArticles;
    private NewsAdapter newsAdapter;
    private FirebaseAuth fbAuth;
    private FirebaseFirestore db;
    private final Handler clockHandler = new Handler();
    private NewsRepository newsRepository;
    private TextToSpeech textToSpeech;

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {
        View view = inflater.inflate(R.layout.fragment_clock, container, false);
        Toolbar toolbar = view.findViewById(R.id.toolbar);
        ((AppCompatActivity) getActivity()).setSupportActionBar(toolbar);

        setHasOptionsMenu(true);

        txtClock = view.findViewById(R.id.txtClock);
        txtGreeting = view.findViewById(R.id.txtGreeting);
        imgBackground = view.findViewById(R.id.imgBackground);
        recyclerViewArticles = view.findViewById(R.id.recyclerViewArticles);

        fbAuth = FirebaseAuth.getInstance();
        db = FirebaseFirestore.getInstance();



        newsRepository = new NewsRepository();

        recyclerViewArticles.setLayoutManager(new LinearLayoutManager(getContext()));
        recyclerViewArticles.addItemDecoration(new DividerItemDecoration(getContext(), DividerItemDecoration.VERTICAL));

        newsAdapter = new NewsAdapter(new ArrayList<>(),getContext());
        recyclerViewArticles.setAdapter(newsAdapter);

        // Initialize Text-to-Speech
        // Initialize Text-to-Speech
        textToSpeech = new TextToSpeech(getContext(), status -> {
            if (status == TextToSpeech.SUCCESS) {
                // Set default to English first
                int langResult = textToSpeech.setLanguage(Locale.US);
                if (langResult == TextToSpeech.LANG_MISSING_DATA || langResult == TextToSpeech.LANG_NOT_SUPPORTED) {
                    Log.e("TextToSpeech", "English language not supported or missing data.");
                }

                // Try setting Hebrew
                int hebrewResult = textToSpeech.setLanguage(new Locale("he"));
                if (hebrewResult == TextToSpeech.LANG_MISSING_DATA || hebrewResult == TextToSpeech.LANG_NOT_SUPPORTED) {
                    Log.e("TextToSpeech", "Hebrew language not supported or missing data.");

                    // Prompt user to install Hebrew voice data
                    Toast.makeText(getContext(), "Hebrew TTS data missing. Redirecting to install it...", Toast.LENGTH_LONG).show();
                    Intent installIntent = new Intent(TextToSpeech.Engine.ACTION_INSTALL_TTS_DATA);
                    requireActivity().startActivity(installIntent);
                }

            } else {
                Log.e("TextToSpeech", "TTS initialization failed.");
            }
        });


// Set TTS on adapter
        newsAdapter.setTextToSpeech(textToSpeech);


        // Display greeting message, start clock update, and fetch articles
        displayUserGreeting();
        updateClock();
        fetchArticlesFromFollowedSources();

        return view;
    }

    // Fetch articles from followed sources
    private void fetchArticlesFromFollowedSources() {
        newsRepository.getArticlesByFollowedSources("publishedAt", new ApiCallBack<NewsResponse>() {
            @Override
            public void OnSucces(NewsResponse response) {
                if (response != null && response.getArticles() != null && !response.getArticles().isEmpty()) {
                    List<Article> articles = response.getArticles();
                    loadFavoriteArticles(articles);  // Fetch favorites and update the adapter
                } else {
                    showNoFollowedSourcesMessage();
                }
            }

            @Override
            public void OnFail() {
                showErrorMessage();
            }
        });
    }

    // Fetch the user's favorite articles and update the adapter
    private void loadFavoriteArticles(List<Article> allArticles) {
        FirebaseFirestore.getInstance().collection("user")
                .document(FirebaseAuth.getInstance().getCurrentUser().getUid())
                .collection("favorites")
                .get()
                .addOnSuccessListener(queryDocumentSnapshots -> {
                    if (queryDocumentSnapshots != null) {
                        favoriteUrls.clear();  // Clear existing data
                        for (DocumentSnapshot documentSnapshot : queryDocumentSnapshots) {
                            String url = documentSnapshot.getString("url");
                            if (url != null) {
                                favoriteUrls.add(url);  // Add the URL to the favorite list
                            }
                        }
                        // Now that favorites are loaded, update the articles
                        updateArticlesWithFavorites(allArticles);
                    }
                })
                .addOnFailureListener(e -> {
                    Log.e("ClockFragment", "Error loading favorites", e);
                });
    }



    // Update the articles in the adapter, after loading favorites
    private void updateArticlesWithFavorites(List<Article> articles) {
        for (Article article : articles) {
            boolean isFavorited = favoriteUrls.contains(article.getUrl());
            article.setFavorited(isFavorited);
        }
        newsAdapter.updateArticles(articles);  // Update the adapter to reflect the new favorite state
    }


    private void showNoFollowedSourcesMessage() {
        Toast.makeText(getContext(), "You have no followed news sources.", Toast.LENGTH_SHORT).show();
    }

    private void showErrorMessage() {
        Toast.makeText(getContext(), "Failed to load your news sources.", Toast.LENGTH_SHORT).show();
    }

    private void displayUserGreeting() {
        FirebaseUser currentUser = fbAuth.getCurrentUser();
        if (currentUser == null) {
            return;
        }

        String userId = currentUser.getUid();
        DocumentReference userRef = db.collection("user").document(userId);

        userRef.get().addOnCompleteListener(task -> {
            if (task.isSuccessful() && task.getResult() != null) {
                String firstName = task.getResult().getString("firstName");
                String lastName = task.getResult().getString("lastName");

                int hourOfDay = Calendar.getInstance().get(Calendar.HOUR_OF_DAY);

                String greeting;
                if (hourOfDay >= 19 || hourOfDay < 6) {
                    greeting = "Good night, " + firstName + " " + lastName + "!";
                } else if (hourOfDay >= 6 && hourOfDay < 12) {
                    greeting = "Good morning, " + firstName + " " + lastName + "!";
                } else {
                    greeting = "Good afternoon, " + firstName + " " + lastName + "!";
                }

                txtGreeting.setText(greeting);
            }
        });
    }

    private void updateClock() {
        clockHandler.postDelayed(new Runnable() {
            @Override
            public void run() {
                Calendar calendar = Calendar.getInstance(TimeZone.getDefault());
                SimpleDateFormat dateFormat = new SimpleDateFormat("HH:mm:ss");
                txtClock.setText(dateFormat.format(calendar.getTime()));

                // Update background based on current hour
                updateBackground(calendar.get(Calendar.HOUR_OF_DAY));

                // Run clock update every 1 second
                clockHandler.postDelayed(this, 1000);
            }
        }, 0);
    }

    // Update background based on the time of day
    private void updateBackground(int hourOfDay) {
        if (hourOfDay >= 6 && hourOfDay < 12) {
            imgBackground.setImageResource(R.drawable.morning_image);
        } else if (hourOfDay >= 12 && hourOfDay < 19) {
            imgBackground.setImageResource(R.drawable.afternoon_image);
        } else {
            imgBackground.setImageResource(R.drawable.night_image);
        }
    }

    @Override
    public void onDestroy() {
        super.onDestroy();
        clockHandler.removeCallbacksAndMessages(null);
        if (textToSpeech != null) {
            textToSpeech.stop();
            textToSpeech.shutdown();
        }
    }

    @Override
    public void onCreateOptionsMenu(Menu menu, MenuInflater inflater) {
        super.onCreateOptionsMenu(menu, inflater);
        inflater.inflate(R.menu.menu, menu);
    }

    // Handle menu options
    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        Log.d("ClockFragment", "Menu item selected: " + item.getItemId());

        if (item.getItemId() == R.id.menu_clock) {
            Toast.makeText(getContext(), "You are already on the Clock page.", Toast.LENGTH_SHORT).show();
            return true;
        } else if (item.getItemId() == R.id.menu_change_info) {
            navigateToFragment(new ChangeInfoFragment());
            return true;
        } else if (item.getItemId() == R.id.menu_follow_page) {
            navigateToFragment(new FollowPageFragment());
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
