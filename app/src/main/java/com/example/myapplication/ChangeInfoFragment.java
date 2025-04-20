package com.example.myapplication;

import android.app.AlertDialog;
import android.content.ActivityNotFoundException;
import android.content.Intent;
import android.graphics.Color;
import android.net.Uri;
import android.os.Bundle;
import android.speech.tts.TextToSpeech;
import android.text.TextUtils;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.Menu;
import android.view.MenuInflater;
import android.view.MenuItem;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Button;
import android.widget.EditText;
import android.widget.ImageView;
import android.widget.TextView;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.appcompat.app.AppCompatActivity;
import androidx.appcompat.widget.Toolbar;
import androidx.fragment.app.Fragment;
import androidx.fragment.app.FragmentTransaction;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.firestore.DocumentReference;
import com.google.firebase.firestore.FirebaseFirestore;

import java.util.ArrayList;
import java.util.List;
import java.util.Locale;

public class ChangeInfoFragment extends Fragment {

    private EditText etFirstName, etLastName;
    private TextView txtEmail;
    private Button btnSaveChanges;
    private FirebaseAuth fbAuth;
    private FirebaseFirestore db;
    private String userId;
    private ImageView starImageView; // For the star button
    private TextToSpeech textToSpeech; // Text-to-Speech engine

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {
        View view = inflater.inflate(R.layout.fragment_change_info, container, false);

        // Toolbar setup
        Toolbar toolbar = view.findViewById(R.id.toolbar);
        ((AppCompatActivity) getActivity()).setSupportActionBar(toolbar);

        etFirstName = view.findViewById(R.id.etFirstName);
        etLastName = view.findViewById(R.id.etLastName);
        txtEmail = view.findViewById(R.id.txtEmail);
        btnSaveChanges = view.findViewById(R.id.btnSaveChanges);
        starImageView = view.findViewById(R.id.imgFavoriteStar);  // Initialize star image view

        setHasOptionsMenu(true);

        fbAuth = FirebaseAuth.getInstance();
        db = FirebaseFirestore.getInstance();

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


        // Check login
        if (fbAuth.getCurrentUser() != null) {
            userId = fbAuth.getCurrentUser().getUid();
            displayUserDetails();
        } else {
            redirectToMainActivity();
        }

        btnSaveChanges.setOnClickListener(v -> saveChanges());

        // Star icon click listener to open favorites popup
        starImageView.setOnClickListener(v -> openFavoritesPopup());

        return view;
    }

    // Display user details in the form
    private void displayUserDetails() {
        DocumentReference userRef = db.collection("user").document(userId);
        userRef.get().addOnCompleteListener(task -> {
            if (task.isSuccessful() && task.getResult() != null) {
                String firstName = task.getResult().getString("firstName");
                String lastName = task.getResult().getString("lastName");
                String email = task.getResult().getString("email");

                etFirstName.setText(firstName);
                etLastName.setText(lastName);
                txtEmail.setText(email);
            }
        });
    }

    // Open the favorites popup when star is clicked
    private void openFavoritesPopup() {
        // Inflate the popup layout
        View popupView = LayoutInflater.from(getContext()).inflate(R.layout.popup_favorites, null);

        // Set up RecyclerView
        RecyclerView recyclerViewFavorites = popupView.findViewById(R.id.recyclerViewFavorites);
        recyclerViewFavorites.setLayoutManager(new LinearLayoutManager(getContext()));

        // Load favorite articles
        loadFavoriteArticles(recyclerViewFavorites);

        // Close button logic
        Button closeButton = popupView.findViewById(R.id.btnClosePopup); // Ensure this button exists in your layout
        android.widget.PopupWindow popupWindow = new android.widget.PopupWindow(popupView,
                ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.MATCH_PARENT);

        closeButton.setOnClickListener(v -> popupWindow.dismiss()); // Dismiss popup when close button is clicked

        // Show the PopupWindow
        popupWindow.showAtLocation(getView(), android.view.Gravity.CENTER, 0, 0);
    }

    // Load favorite articles from Firestore and populate RecyclerView
    private void loadFavoriteArticles(RecyclerView recyclerViewFavorites) {
        db.collection("user")
                .document(userId)
                .collection("favorites")
                .get()
                .addOnSuccessListener(queryDocumentSnapshots -> {
                    List<Article> favoriteArticles = new ArrayList<>();
                    for (var document : queryDocumentSnapshots) {
                        Article article = document.toObject(Article.class);
                        favoriteArticles.add(article);
                    }

                    FavoriteArticlesAdapter adapter = new FavoriteArticlesAdapter(favoriteArticles);
                    recyclerViewFavorites.setAdapter(adapter);
                })
                .addOnFailureListener(e -> {
                    // Handle error
                });
    }

    // Adapter for the favorite articles RecyclerView
    private class FavoriteArticlesAdapter extends RecyclerView.Adapter<FavoriteArticlesAdapter.FavoriteViewHolder> {

        private List<Article> favoriteArticles;

        public FavoriteArticlesAdapter(List<Article> favoriteArticles) {
            this.favoriteArticles = favoriteArticles;
        }

        @Override
        public FavoriteViewHolder onCreateViewHolder(ViewGroup parent, int viewType) {
            View view = LayoutInflater.from(parent.getContext()).inflate(R.layout.item_article, parent, false);
            return new FavoriteViewHolder(view);
        }
        @Override
        public void onBindViewHolder(FavoriteViewHolder holder, int position) {
            Article article = favoriteArticles.get(position);
            holder.titleTextView.setText(article.getTitle());
            holder.sourceTextView.setText(article.getSource().getName());

            // Set URL as a dot (instead of the full URL text)
            holder.urlTextView.setText(".");  // Display a dot instead of the full URL

            // Set URL dot clickable
            holder.urlTextView.setOnClickListener(v -> {
                // Open the URL in the browser when clicked
                Intent intent = new Intent(Intent.ACTION_VIEW, Uri.parse(article.getUrl()));
                v.getContext().startActivity(intent);
            });

            // Star icon: always filled for favorites
            holder.starImageView.setImageResource(R.drawable.ic_star_filled);
            holder.starImageView.setColorFilter(Color.YELLOW);

            // Handle the unfavoriting action when the star is clicked
            holder.starImageView.setOnClickListener(v -> {
                removeFromFavorites(article.getUrl());
                favoriteArticles.remove(position);
                notifyItemRemoved(position);
            });

            holder.titleTextView.setOnClickListener(v -> {
                if (textToSpeech != null) {
                    String title = article.getTitle();

                    // Detect Hebrew (basic check based on Unicode range)
                    boolean isHebrew = title.matches(".*\\p{InHebrew}.*");

                    // Set language accordingly
                    Locale targetLocale = isHebrew ? new Locale("he") : Locale.US;
                    int result = textToSpeech.setLanguage(targetLocale);

                    if (result == TextToSpeech.LANG_MISSING_DATA || result == TextToSpeech.LANG_NOT_SUPPORTED) {
                        Toast.makeText(getContext(), "Selected language not supported on this device.", Toast.LENGTH_SHORT).show();
                    } else {
                        textToSpeech.speak(title, TextToSpeech.QUEUE_FLUSH, null, null);
                    }
                }
            });


            // Arrow button to send the article via email
            holder.shareButton.setOnClickListener(v -> {
                // Show email sending dialog
                shareArticle(article);
            });
        }

        private void shareArticle(Article article) {
            Intent shareIntent = new Intent(Intent.ACTION_SEND);
            shareIntent.setType("text/plain");

            String shareMessage = "Check out this article:\n" +
                    article.getTitle() + "\n" + article.getUrl();

            shareIntent.putExtra(Intent.EXTRA_TEXT, shareMessage);

            try {
                getContext().startActivity(Intent.createChooser(shareIntent, "Share article via"));
            } catch (ActivityNotFoundException e) {
                Toast.makeText(getContext(), "No app available to share the article.", Toast.LENGTH_SHORT).show();
            }
        }


        @Override
        public int getItemCount() {
            return favoriteArticles.size();
        }

        public class FavoriteViewHolder extends RecyclerView.ViewHolder {


            TextView titleTextView, sourceTextView, urlTextView;
            ImageView starImageView, shareButton;  // Add the button to send email

            public FavoriteViewHolder(View itemView) {
                super(itemView);
                titleTextView = itemView.findViewById(R.id.txtArticleTitle);
                sourceTextView = itemView.findViewById(R.id.txtArticleSource);
                urlTextView = itemView.findViewById(R.id.txtArticleUrl);
                starImageView = itemView.findViewById(R.id.imgFavoriteStar);
                shareButton = itemView.findViewById(R.id.imgSendEmail);  // Initialize the send email button
            }
        }

        private void removeFromFavorites(String articleUrl) {
            db.collection("user")
                    .document(userId)
                    .collection("favorites")
                    .whereEqualTo("url", articleUrl)
                    .get()
                    .addOnCompleteListener(task -> {
                        if (task.isSuccessful() && !task.getResult().isEmpty()) {
                            String documentId = task.getResult().getDocuments().get(0).getId();
                            db.collection("user")
                                    .document(userId)
                                    .collection("favorites")
                                    .document(documentId)
                                    .delete();
                        }
                    });
        }
    }

    @Override
    public void onCreateOptionsMenu(Menu menu, MenuInflater inflater) {
        super.onCreateOptionsMenu(menu, inflater);
        inflater.inflate(R.menu.menu, menu);
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        Log.d("ChangeInfoFragment", "Menu item selected: " + item.getItemId());

        if (item.getItemId() == R.id.menu_change_info) {
            Toast.makeText(getContext(), "You are already on the Change Info page.", Toast.LENGTH_SHORT).show();
            return true;
        } else if (item.getItemId() == R.id.menu_clock) {
            navigateToFragment(new ClockFragment());
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

    private void saveChanges() {
        String firstName = etFirstName.getText().toString();
        String lastName = etLastName.getText().toString();

        if (TextUtils.isEmpty(firstName) || TextUtils.isEmpty(lastName)) {
            Toast.makeText(getContext(), "Please fill all fields.", Toast.LENGTH_SHORT).show();
            return;
        }

        DocumentReference userRef = db.collection("user").document(userId);
        userRef.update("firstName", firstName, "lastName", lastName)
                .addOnSuccessListener(aVoid -> Toast.makeText(getContext(), "Changes saved successfully!", Toast.LENGTH_SHORT).show())
                .addOnFailureListener(e -> Toast.makeText(getContext(), "Failed to save changes.", Toast.LENGTH_SHORT).show());
    }

    private void redirectToMainActivity() {
        startActivity(new android.content.Intent(getContext(), MainActivity.class));
        if (getActivity() != null) getActivity().finish();
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
