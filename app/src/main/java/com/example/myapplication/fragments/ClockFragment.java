package com.example.myapplication.fragments;

import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.os.Bundle;
import android.os.Handler;
import android.speech.tts.TextToSpeech;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageButton;
import android.widget.ImageView;
import android.widget.PopupMenu;
import android.widget.TextView;
import android.widget.Toast;

import androidx.fragment.app.Fragment;
import androidx.recyclerview.widget.DividerItemDecoration;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.example.myapplication.R;
import com.example.myapplication.api.IApiCallBack;
import com.example.myapplication.api.NewsRepository;
import com.example.myapplication.help.NewsAdapter;
import com.example.myapplication.objects.Article;
import com.example.myapplication.objects.NewsResponse;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseUser;
import com.google.firebase.firestore.DocumentReference;
import com.google.firebase.firestore.DocumentSnapshot;
import com.google.firebase.firestore.FirebaseFirestore;

import java.lang.reflect.Field;
import java.lang.reflect.Method;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Calendar;
import java.util.List;
import java.util.Locale;
import java.util.TimeZone;

public class ClockFragment extends Fragment {

    private final Handler clockHandler = new Handler();
    private List<String> favoriteUrls = new ArrayList<>();
    private TextView txtClock, txtGreeting;
    private ImageView imgBackground;
    private RecyclerView recyclerViewArticles;
    private NewsAdapter newsAdapter;
    private FirebaseAuth fbAuth;
    private FirebaseFirestore db;
    private NewsRepository newsRepository;
    private TextToSpeech textToSpeech;

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {
        View view = inflater.inflate(R.layout.fragment_clock, container, false);


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

        newsAdapter = new NewsAdapter(new ArrayList<>(), getContext(), false);
        recyclerViewArticles.setAdapter(newsAdapter);

        ImageButton filterButton = view.findViewById(R.id.button_filter);
        filterButton.setOnClickListener(v -> showFilterMenu(v));

        // Initialize Text-to-Speech
        textToSpeech = new TextToSpeech(getContext(), status -> {
            if (status == TextToSpeech.SUCCESS) {
                int langResult = textToSpeech.setLanguage(Locale.US);
                if (langResult == TextToSpeech.LANG_MISSING_DATA || langResult == TextToSpeech.LANG_NOT_SUPPORTED) {
                    Log.e("TextToSpeech", "English language not supported or missing data.");
                }

                int hebrewResult = textToSpeech.setLanguage(new Locale("he"));
                if (hebrewResult == TextToSpeech.LANG_MISSING_DATA || hebrewResult == TextToSpeech.LANG_NOT_SUPPORTED) {
                    Log.e("TextToSpeech", "Hebrew language not supported or missing data.");
                    Toast.makeText(getContext(), "Hebrew TTS data missing. Redirecting to install it...", Toast.LENGTH_LONG).show();
                    Intent installIntent = new Intent(TextToSpeech.Engine.ACTION_INSTALL_TTS_DATA);
                    requireActivity().startActivity(installIntent);
                }
            } else {
                Log.e("TextToSpeech", "TTS initialization failed.");
            }
        });

        newsAdapter.setTextToSpeech(textToSpeech);

        SharedPreferences prefs = requireContext().getSharedPreferences("MyPrefs", Context.MODE_PRIVATE);
        String savedCategory = prefs.getString("selected_category", "none");

        if (savedCategory.equals("none")) {
            fetchArticlesFromFollowedSources();
        } else {
            fetchArticlesWithFilter(savedCategory);
            Toast.makeText(getContext(), "Filter applied: " + savedCategory, Toast.LENGTH_SHORT).show();
        }

        displayUserGreeting();
        updateClock();
        return view;
    }

    private void fetchArticlesFromFollowedSources() {
        Log.d("ArticleFetch", "Fetching articles from followed sources...");
        newsRepository.getArticlesByFollowedSources("publishedAt", new IApiCallBack<NewsResponse>() {
            @Override
            public void OnSucces(NewsResponse response) {
                if (response != null && response.getArticles() != null && !response.getArticles().isEmpty()) {
                    List<Article> articles = response.getArticles();
                    Log.d("ArticleFetch", "Fetched " + articles.size() + " articles from followed sources.");
                    loadfavArticles(articles);
                } else {
                    Log.d("ArticleFetch", "No articles found from followed sources.");
                    Toast.makeText(getContext(), "No articles found for your followed sources.", Toast.LENGTH_SHORT).show();
                }
            }

            @Override
            public void OnFail() {
                Log.e("ArticleFetch", "Failed to fetch articles from followed sources.");
                showErrorMessage();
            }
        });
    }


    private void showFilterMenu(View anchor) {

        PopupMenu popup = new PopupMenu(requireContext(), anchor);
        popup.getMenuInflater().inflate(R.menu.menu_filter, popup.getMenu());

        // Force menu icons to show
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

        // Handle menu item selection
        popup.setOnMenuItemClickListener(item -> {

            String selectedCategory = "";


            if (item.getItemId() == R.id.category_none) {
                selectedCategory = "none";
            } else if (item.getItemId() == R.id.category_business) {
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

            if (selectedCategory != null) {
                Log.d("CategoryFilter", "Selected category: " + selectedCategory);
                SharedPreferences prefs = requireContext().getSharedPreferences("MyPrefs", Context.MODE_PRIVATE);
                prefs.edit().putString("selected_category", selectedCategory).apply();
                fetchArticlesWithFilter(selectedCategory);
                Toast.makeText(getContext(), "Filter applied: " + selectedCategory, Toast.LENGTH_SHORT).show();
            }

            return true;
        });

        popup.show();
    }

    private void fetchArticlesWithFilter(String selectedCategory) {
        Log.d("CategoryFilter", "Fetch initiated. Category: " + selectedCategory);
        if (selectedCategory.equals("none")) {
            Log.d("CategoryFilter", "Selected category is 'none', fetching from followed sources...");
            fetchArticlesFromFollowedSources();
            return;
        }

        Log.d("CategoryFilter", "Clearing old articles from adapter...");
        newsAdapter.updateArticles(new ArrayList<>());
        newsRepository.getArticlesByCategory(selectedCategory, new IApiCallBack<NewsResponse>() {
            @Override
            public void OnSucces(NewsResponse response) {
                List<Article> allCategoryArticles = response.getArticles();
                Log.d("CategoryFilter", " Articles received for category '" + selectedCategory + "': " + (allCategoryArticles != null ? allCategoryArticles.size() : 0));

                if (allCategoryArticles == null || allCategoryArticles.isEmpty()) {
                    Log.w("CategoryFilter", "No articles found for: " + selectedCategory);
                    Toast.makeText(getContext(), "No articles found for category: " + selectedCategory, Toast.LENGTH_SHORT).show();
                    return;
                }

                filterArticlesFromFollowedSources(allCategoryArticles);
            }

            @Override
            public void OnFail() {
                Log.e("CategoryFilter", " Failed to fetch articles for category: " + selectedCategory);
                showErrorMessage();
            }
        });
    }
    // Fetch followed sources from Firebase and filter the articles
    private void filterArticlesFromFollowedSources(List<Article> allCategoryArticles) {
        newsRepository.getArticlesByFollowedSources("publishedAt", new IApiCallBack<NewsResponse>() {
            @Override
            public void OnSucces(NewsResponse response) {
                if (response != null && response.getArticles() != null) {
                    List<Article> followedArticles = response.getArticles();
                    List<String> followedSourceIds = new ArrayList<>();

                    // Collect the ids of all followed sources
                    for (Article article : followedArticles) {
                        String sourceId = article.getSource() != null ? article.getSource().getId() : null;
                        if (sourceId != null) {
                            followedSourceIds.add(sourceId);
                        }
                    }

                    // Filter the articles from the selected category by checking if their source id matches any of the followed sources
                    List<Article> filteredArticles = new ArrayList<>();
                    for (Article article : allCategoryArticles) {
                        if (article.getSource() != null && followedSourceIds.contains(article.getSource().getId())) {
                            filteredArticles.add(article);
                        }
                    }

                    if (!filteredArticles.isEmpty()) {
                        Log.d("CategoryFilter", " Filtered articles from followed sources: " + filteredArticles.size());
                        loadfavArticles(filteredArticles);
                    } else {
                        Log.w("CategoryFilter", " No articles found for the selected category and followed sources.");
                        Toast.makeText(getContext(), "No articles found for this category and your followed sources.", Toast.LENGTH_SHORT).show();
                    }
                } else {
                    Log.e("CategoryFilter", " Failed to fetch followed sources.");
                    showErrorMessage();
                }
            }

            @Override
            public void OnFail() {
                Log.e("CategoryFilter", " Failed to fetch articles from followed sources.");
                showErrorMessage();
            }
        });
    }
    private void loadfavArticles(List<Article> allArticles) {
        FirebaseFirestore.getInstance().collection("user")
                .document(FirebaseAuth.getInstance().getCurrentUser().getUid())
                .collection("favorites")
                .get()
                .addOnSuccessListener(queryDocumentSnapshots -> {
                    if (queryDocumentSnapshots != null) {
                        favoriteUrls.clear();
                        for (DocumentSnapshot documentSnapshot : queryDocumentSnapshots) {
                            String url = documentSnapshot.getString("url");
                            if (url != null) {
                                favoriteUrls.add(url);
                            }
                        }
                        updateArticlesWithFavorites(allArticles);
                    }
                })
                .addOnFailureListener(e -> {
                    Log.e("ClockFragment", "Error loading favorites", e);
                });
    }

    private void updateArticlesWithFavorites(List<Article> articles) {
        for (Article article : articles) {
            boolean isFavorited = favoriteUrls.contains(article.getUrl());
            article.setFavorited(isFavorited);
        }
        newsAdapter.updateArticles(articles);
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
                    greeting = "Good night, " + firstName + " " + lastName + " ";
                } else if (hourOfDay >= 6 && hourOfDay < 12) {
                    greeting = "Good morning, " + firstName + " " + lastName + " ";
                } else {
                    greeting = "Good afternoon, " + firstName + " " + lastName + " ";
                }

                txtGreeting.setText(greeting + "welcome to Newsflow" );
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

                updateBackground(calendar.get(Calendar.HOUR_OF_DAY));


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

}

