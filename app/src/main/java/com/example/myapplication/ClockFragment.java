package com.example.myapplication;

import android.content.Intent;
import android.os.Bundle;
import android.os.Handler;
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
import com.google.firebase.firestore.FirebaseFirestore;

import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Calendar;
import java.util.List;
import java.util.TimeZone;

public class ClockFragment extends Fragment {

    private TextView txtClock, txtGreeting;
    private ImageView imgBackground;
    private RecyclerView recyclerViewArticles;
    private NewsAdapter newsAdapter;
    private FirebaseAuth fbAuth;
    private FirebaseFirestore db;
    private final Handler clockHandler = new Handler();
    private NewsRepository newsRepository;

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

        newsAdapter = new NewsAdapter(new ArrayList<>());
        recyclerViewArticles.setAdapter(newsAdapter);

        // Display greeting message, start clock update, and fetch articles
        displayUserGreeting();
        updateClock();
        fetchArticlesFromFollowedSources();

        return view;
    }

    // takes from api
    private void fetchArticlesFromFollowedSources() {
        newsRepository.getArticlesByFollowedSources("publishedAt", new ApiCallBack<NewsResponse>() {
            @Override
            public void OnSucces(NewsResponse response) {
                if (response != null && response.getArticles() != null && !response.getArticles().isEmpty()) {
                    List<Article> articles = response.getArticles();
                    newsAdapter.updateArticles(articles);
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

    private void showNoFollowedSourcesMessage() {
        Toast.makeText(getContext(), "You have no followed news sources.", Toast.LENGTH_SHORT).show();
    }

    private void showErrorMessage() {
        Toast.makeText(getContext(), "Failed to load your news sources.", Toast.LENGTH_SHORT).show();
    }
// user messege
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

    // background based on the time of day
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
    }

    @Override
    public void onCreateOptionsMenu(Menu menu, MenuInflater inflater) {
        super.onCreateOptionsMenu(menu, inflater);
        inflater.inflate(R.menu.menu, menu);
    }
    //menu
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
