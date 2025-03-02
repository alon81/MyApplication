package com.example.myapplication;

import android.content.Intent;
import android.os.Bundle;
import android.util.Log;
import android.view.Menu;
import android.view.MenuItem;
import android.widget.Button;
import android.widget.EditText;
import android.widget.TextView;
import android.widget.Toast;

import androidx.appcompat.app.AppCompatActivity;
import androidx.appcompat.widget.Toolbar;

import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.firestore.DocumentReference;
import com.google.firebase.firestore.FirebaseFirestore;

public class ChangeInfoActivity extends AppCompatActivity {

    private EditText etFirstName, etLastName;
    private TextView txtEmail;
    private FirebaseAuth fbAuth;
    private FirebaseFirestore db;
    private String userId;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_change_info);

        Toolbar toolbar = findViewById(R.id.toolbar);
        setSupportActionBar(toolbar); // Set Toolbar as ActionBar

        etFirstName = findViewById(R.id.etFirstName);
        etLastName = findViewById(R.id.etLastName);
        txtEmail = findViewById(R.id.txtEmail);
        Button btnSaveChanges = findViewById(R.id.btnSaveChanges);

        fbAuth = FirebaseAuth.getInstance();
        db = FirebaseFirestore.getInstance();

        // Ensure the user is authenticated before retrieving data
        if (fbAuth.getCurrentUser() != null) {
            userId = fbAuth.getCurrentUser().getUid();
            displayUserDetails();
        } else {
            Toast.makeText(ChangeInfoActivity.this, "User is not authenticated", Toast.LENGTH_SHORT).show();
            // Redirect to login or main activity if not authenticated
            startActivity(new Intent(ChangeInfoActivity.this, MainActivity.class));
            finish();
        }

        btnSaveChanges.setOnClickListener(v -> saveChanges());
    }

    // Inflate the menu
    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        getMenuInflater().inflate(R.menu.menu, menu);  // Inflate the menu
        Log.d("ChangeInfoActivity", "Menu inflated");  // Debug log
        return true;
    }

    // Handle menu item selection
    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        Log.d("ChangeInfoActivity", "Menu item selected: " + item.getItemId());  // Debug log

        if (item.getItemId() == R.id.menu_change_info) {
            Toast.makeText(this, "You are already on the Change Info page.", Toast.LENGTH_SHORT).show();
            return true;
        } else if (item.getItemId() == R.id.menu_clock) {
            Intent clockIntent = new Intent(this, ClockActivity.class);
            startActivity(clockIntent);
            return true;
        } else if (item.getItemId() == R.id.menu_follow_page) {
            startActivity(new Intent(this, FollowPageActivity.class));
            return true;
        } else if (item.getItemId() == R.id.menu_logout) {
            FirebaseAuth.getInstance().signOut();
            Intent logoutIntent = new Intent(this, MainActivity.class);
            startActivity(logoutIntent);
            finish();
            return true;
        }
        return super.onOptionsItemSelected(item);
    }

    // Fetch user details from Firestore
    private void displayUserDetails() {
        DocumentReference userRef = db.collection("user").document(userId);
        userRef.get().addOnCompleteListener(task -> {
            if (task.isSuccessful()) {
                // Log the fetched data for debugging purposes
                Log.d("ChangeInfoActivity", "User Data: " + task.getResult().getData());

                String email = task.getResult().getString("email");
                String firstName = task.getResult().getString("firstName");
                String lastName = task.getResult().getString("lastName");

                // Display user data
                if (email != null) {
                    txtEmail.setText("Email: " + email);
                } else {
                    txtEmail.setText("Email: Not available");
                }
                etFirstName.setText(firstName != null ? firstName : "");
                etLastName.setText(lastName != null ? lastName : "");
            } else {
                Toast.makeText(ChangeInfoActivity.this, "Error fetching user details", Toast.LENGTH_SHORT).show();
            }
        });
    }

    // Save user changes to Firestore
    private void saveChanges() {
        String newFirstName = etFirstName.getText().toString();
        String newLastName = etLastName.getText().toString();

        if (newFirstName.isEmpty() || newLastName.isEmpty()) {
            Toast.makeText(ChangeInfoActivity.this, "Please fill out both fields", Toast.LENGTH_SHORT).show();
            return;
        }

        // Update user information in Firebase Firestore
        DocumentReference userRef = db.collection("user").document(userId);
        userRef.update("firstName", newFirstName, "lastName", newLastName)
                .addOnCompleteListener(task -> {
                    if (task.isSuccessful()) {
                        Toast.makeText(ChangeInfoActivity.this, "Changes saved successfully", Toast.LENGTH_SHORT).show();
                        // Redirect to ClockActivity after saving changes
                        Intent intent = new Intent(ChangeInfoActivity.this, ClockActivity.class);
                        startActivity(intent);
                        finish();
                    } else {
                        Toast.makeText(ChangeInfoActivity.this, "Error saving changes", Toast.LENGTH_SHORT).show();
                    }
                });
    }
}
