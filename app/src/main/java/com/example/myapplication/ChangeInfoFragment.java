package com.example.myapplication;

import android.app.AlertDialog;
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
                int langResult = textToSpeech.setLanguage(Locale.US);
                if (langResult == TextToSpeech.LANG_MISSING_DATA ||
                        langResult == TextToSpeech.LANG_NOT_SUPPORTED) {
                    Log.e("TextToSpeech", "Language not supported or missing data.");
                }
            } else {
                Log.e("TextToSpeech", "Initialization failed.");
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

            // Text-to-speech for article titles
            holder.itemView.setOnClickListener(v -> {
                if (textToSpeech != null) {
                    textToSpeech.speak(article.getTitle(), TextToSpeech.QUEUE_FLUSH, null, null);
                }
            });

            // Arrow button to send the article via email
            holder.sendEmailButton.setOnClickListener(v -> {
                // Show email sending dialog
                showEmailDialog(article);
            });
        }

        private void showEmailDialog(Article article) {
            // Create the dialog view directly from XML without the title at the top
            AlertDialog.Builder builder = new AlertDialog.Builder(getContext());

            // Inflate the email dialog layout
            LayoutInflater inflater = LayoutInflater.from(getContext());
            View dialogView = inflater.inflate(R.layout.custom_email_dialog, null);

            // Find the email EditText and buttons in the XML layout
            EditText emailEditText = dialogView.findViewById(R.id.emailEditText);
            Button sendButton = dialogView.findViewById(R.id.sendButton);

            // Remove the default title by setting a null title (if needed)
            builder.setTitle(null);

            // Set the custom view
            builder.setView(dialogView);

            // Handle the Send button click event
            sendButton.setOnClickListener(v -> {
                String email = emailEditText.getText().toString().trim();
                if (!email.isEmpty()) {
                    if (isValidEmail(email)) {
                        sendEmail(email, article);
                    } else {
                        Toast.makeText(getContext(), "Invalid email format.", Toast.LENGTH_SHORT).show();
                    }
                } else {
                    Toast.makeText(getContext(), "Please enter an email address.", Toast.LENGTH_SHORT).show();
                }
            });

            // Show the dialog without a cancel button
            builder.create().show();
        }


        // Helper method to validate email format
        private boolean isValidEmail(String email) {
            String emailPattern = "^[a-zA-Z0-9._%+-]+@[a-zA-Z0-9.-]+\\.[a-zA-Z]{2,}$";
            return email.matches(emailPattern);
        }


        // Method to send the email
        private void sendEmail(String recipientEmail, Article article) {
            Intent emailIntent = new Intent(Intent.ACTION_SENDTO);
            emailIntent.setData(Uri.parse("mailto:"));  // Only email apps should handle this

            // Set the email recipient, subject, and body
            emailIntent.putExtra(Intent.EXTRA_EMAIL, new String[]{recipientEmail});
            emailIntent.putExtra(Intent.EXTRA_SUBJECT, "Check out this article: " + article.getTitle());
            emailIntent.putExtra(Intent.EXTRA_TEXT, "Here's an interesting article:\n\n" + article.getTitle() + "\n" + article.getUrl());

            try {
                startActivity(Intent.createChooser(emailIntent, "Send email..."));
            } catch (android.content.ActivityNotFoundException ex) {
                Toast.makeText(getContext(), "No email client installed.", Toast.LENGTH_SHORT).show();
            }
        }



        @Override
        public int getItemCount() {
            return favoriteArticles.size();
        }

        public class FavoriteViewHolder extends RecyclerView.ViewHolder {


            TextView titleTextView, sourceTextView, urlTextView;
            ImageView starImageView, sendEmailButton;  // Add the button to send email

            public FavoriteViewHolder(View itemView) {
                super(itemView);
                titleTextView = itemView.findViewById(R.id.txtArticleTitle);
                sourceTextView = itemView.findViewById(R.id.txtArticleSource);
                urlTextView = itemView.findViewById(R.id.txtArticleUrl);
                starImageView = itemView.findViewById(R.id.imgFavoriteStar);
                sendEmailButton = itemView.findViewById(R.id.imgSendEmail);  // Initialize the send email button
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
