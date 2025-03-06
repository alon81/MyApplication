package com.example.myapplication;

import android.content.Intent;
import android.os.Bundle;
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
import android.widget.TextView;
import android.widget.Toast;


import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.appcompat.app.AppCompatActivity;
import androidx.appcompat.widget.Toolbar;
import androidx.fragment.app.Fragment;
import androidx.fragment.app.FragmentTransaction;

import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.firestore.DocumentReference;
import com.google.firebase.firestore.FirebaseFirestore;

public class ChangeInfoFragment extends Fragment {

    private EditText etFirstName, etLastName;
    private TextView txtEmail;
    private Button btnSaveChanges;
    private FirebaseAuth fbAuth;
    private FirebaseFirestore db;
    private String userId;

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {
        View view = inflater.inflate(R.layout.fragment_change_info, container, false);

        // Set up the toolbar as the action bar
        Toolbar toolbar = view.findViewById(R.id.toolbar);
        ((AppCompatActivity) getActivity()).setSupportActionBar(toolbar);

        // Initialize views
        etFirstName = view.findViewById(R.id.etFirstName);
        etLastName = view.findViewById(R.id.etLastName);
        txtEmail = view.findViewById(R.id.txtEmail);
        btnSaveChanges = view.findViewById(R.id.btnSaveChanges);

        // Enable options menu
        setHasOptionsMenu(true);

        // Initialize Firebase
        fbAuth = FirebaseAuth.getInstance();
        db = FirebaseFirestore.getInstance();

        // Check if user is logged in and fetch user details
        if (fbAuth.getCurrentUser() != null) {
            userId = fbAuth.getCurrentUser().getUid();
            displayUserDetails();
        } else {
            redirectToMainActivity();
        }

        // Set up save button functionality
        btnSaveChanges.setOnClickListener(v -> saveChanges());

        return view;
    }


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
    @Override
    public void onCreateOptionsMenu(Menu menu, MenuInflater inflater) {
        super.onCreateOptionsMenu(menu, inflater);
        inflater.inflate(R.menu.menu, menu); // Inflate your menu
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        Log.d("ChangeInfoFragment", "Menu item selected: " + item.getItemId());  // Debug log

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
