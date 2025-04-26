
package com.example.myapplication.fragments;

import android.app.TimePickerDialog;
import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.icu.util.Calendar;
import android.os.Bundle;
import android.speech.tts.TextToSpeech;
import android.text.TextUtils;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Button;
import android.widget.EditText;
import android.widget.ImageButton;
import android.widget.ImageView;
import android.widget.TextView;
import android.widget.Toast;

import androidx.fragment.app.Fragment;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.example.myapplication.R;
import com.example.myapplication.activity.MainActivity;
import com.example.myapplication.help.FirestoreHelper;
import com.example.myapplication.help.NewsAdapter;
import com.example.myapplication.help.NotificationScheduler;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.firestore.DocumentReference;
import com.google.firebase.firestore.FirebaseFirestore;

import java.util.ArrayList;
import java.util.Locale;

public class ChangeInfoFragment extends Fragment {

    private EditText etFirstName, etLastName;
    private TextView txtEmail;
    private Button btnSaveChanges;
    private FirebaseAuth fbAuth;
    private FirebaseFirestore db;
    private String userId;
    private NewsAdapter newsAdapter;
    private ImageView starImageView; // For the star button
    private TextToSpeech textToSpeech; // Text-to-Speech engine
    private static final String PREFS_NAME = "MyPrefs";
    private static final String KEY_NOTIF_ENABLED = "notifications_enabled";
    private static final String KEY_HOUR = "notification_hour";
    private static final String KEY_MINUTE = "notification_minute";


    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {
        View view = inflater.inflate(R.layout.fragment_change_info, container, false);

        newsAdapter = new NewsAdapter(new ArrayList<>(), getContext(), false);
        etFirstName = view.findViewById(R.id.etFirstName);
        etLastName = view.findViewById(R.id.etLastName);
        txtEmail = view.findViewById(R.id.txtEmail);
        btnSaveChanges = view.findViewById(R.id.btnSaveChanges);
        starImageView = view.findViewById(R.id.imgFavoriteStar);  // Initialize star image view
        ImageButton notifToggleBtn;

        setHasOptionsMenu(true);

        notifToggleBtn = view.findViewById(R.id.notificationButton);
        notifToggleBtn.setOnClickListener(v -> handleNotificationToggle());

        Button logoutButton = view.findViewById(R.id.logoutButton);

        logoutButton.setOnClickListener(v -> {
            FirebaseAuth.getInstance().signOut();
            Intent intent = new Intent(getActivity(), MainActivity.class); // back to login
            intent.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK | Intent.FLAG_ACTIVITY_CLEAR_TASK);
            startActivity(intent);
        });


        fbAuth = FirebaseAuth.getInstance();
        db = FirebaseFirestore.getInstance();

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
    private void handleNotificationToggle() {
        SharedPreferences prefs = requireContext().getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE);
        boolean isEnabled = prefs.getBoolean(KEY_NOTIF_ENABLED, false);

        if (!isEnabled) {
            showTimePickerDialog(); // Pick time and schedule
        } else {
            cancelNotifications();
            SharedPreferences.Editor editor = prefs.edit();
            editor.putBoolean(KEY_NOTIF_ENABLED, false);
            editor.apply();
            Toast.makeText(getContext(), "Notifications Disabled", Toast.LENGTH_SHORT).show();
        }
    }



    private void showTimePickerDialog() {
        Calendar currentTime = Calendar.getInstance();
        int hour = currentTime.get(Calendar.HOUR_OF_DAY);
        int minute = currentTime.get(Calendar.MINUTE);

        TimePickerDialog timePickerDialog = new TimePickerDialog(
                getContext(),
                (view, hourOfDay, minute1) -> {
                    saveNotificationTime(hourOfDay, minute1);
                    scheduleDailyNotification(hourOfDay, minute1);
                    Toast.makeText(getContext(), "Notifications set for " + hourOfDay + ":" + minute1, Toast.LENGTH_SHORT).show();
                },
                hour, minute, true
        );

        timePickerDialog.show();
    }
    private void saveNotificationTime(int hour, int minute) {
        SharedPreferences prefs = requireContext().getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE);
        SharedPreferences.Editor editor = prefs.edit();
        editor.putInt(KEY_HOUR, hour);
        editor.putInt(KEY_MINUTE, minute);
        editor.putBoolean(KEY_NOTIF_ENABLED, true);
        editor.apply();
    }

    private void scheduleDailyNotification(int hour, int minute) {
        NotificationScheduler.setDailyNotification(requireContext(), hour, minute);
    }
    private void cancelNotifications() {
        NotificationScheduler.cancelNotification(requireContext());
    }
    // Open the favorites popup when star is clicked
    private void openFavoritesPopup() {
        // Inflate the popup layout
        View popupView = LayoutInflater.from(getContext()).inflate(R.layout.popup_favorites, null);

        // Set up RecyclerView
        RecyclerView recyclerViewFavorites = popupView.findViewById(R.id.recyclerViewFavorites);
        recyclerViewFavorites.setLayoutManager(new LinearLayoutManager(getContext()));

        // Load favorite articles
        FirestoreHelper firestoreHelper = new FirestoreHelper(requireContext());
        firestoreHelper.loadFavoriteArticles(requireContext(), recyclerViewFavorites, textToSpeech);

        // Create the PopupWindow
        android.widget.PopupWindow popupWindow = new android.widget.PopupWindow(
                popupView,
                ViewGroup.LayoutParams.MATCH_PARENT,
                ViewGroup.LayoutParams.MATCH_PARENT,
                true // Focusable to allow dismissal when clicking outside
        );

        // Allow dismissal when tapping outside
        popupWindow.setBackgroundDrawable(new android.graphics.drawable.ColorDrawable(android.graphics.Color.TRANSPARENT));
        popupWindow.setOutsideTouchable(true);

        // Close button logic
        Button closeButton = popupView.findViewById(R.id.btnClosePopup);
        closeButton.setOnClickListener(v -> popupWindow.dismiss());

        // Show the PopupWindow
        popupWindow.showAtLocation(getView(), android.view.Gravity.CENTER, 0, 0);
    }

    private void saveChanges() {
        String firstName = etFirstName.getText().toString();
        String lastName = etLastName.getText().toString();

        if (TextUtils.isEmpty(firstName) || TextUtils.isEmpty(lastName)) {
            Toast.makeText(getContext(), "Please fill all fields.", Toast.LENGTH_SHORT).show();
            return;
        }
        if(firstName.length()>20||lastName.length()<20) {
            Toast.makeText(getContext(), "name too long.", Toast.LENGTH_SHORT).show();
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

    @Override
    public void onStart() {
        super.onStart();
        SharedPreferences prefs = requireContext().getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE);
        boolean isEnabled = prefs.getBoolean(KEY_NOTIF_ENABLED, false);
    }


}
