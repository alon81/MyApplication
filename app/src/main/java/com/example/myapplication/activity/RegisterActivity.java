package com.example.myapplication.activity;

import android.content.Intent;
import android.os.Bundle;
import android.view.View;
import android.widget.Button;
import android.widget.EditText;
import android.widget.Toast;
import android.util.Patterns;

import androidx.appcompat.app.AppCompatActivity;
import java.util.Map;
import java.util.HashMap;
import com.google.android.gms.tasks.Task;
import com.example.myapplication.R;
import com.google.android.gms.tasks.OnCompleteListener;
import com.google.firebase.auth.AuthResult;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.firestore.FirebaseFirestore;

public class RegisterActivity extends AppCompatActivity implements View.OnClickListener {

    EditText etRegisterEmail, etRegisterPassword, etRegisterFname, etRegisterLname;
    Button btnRegister;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_register);

        findViews();
        btnRegister.setOnClickListener(this);
    }

    private void findViews() {
        etRegisterEmail = findViewById(R.id.etRegisterEmail);
        etRegisterPassword = findViewById(R.id.etRegisterPassword);
        etRegisterFname = findViewById(R.id.etRegisterFname);
        etRegisterLname = findViewById(R.id.etRegisterLname);
        btnRegister = findViewById(R.id.btnRegister);
    }

    @Override
    public void onClick(View view) {
        if (view == btnRegister) {
            final String email = etRegisterEmail.getText().toString().trim();
            final String password = etRegisterPassword.getText().toString().trim();
            final String firstName = etRegisterFname.getText().toString().trim();
            final String lastName = etRegisterLname.getText().toString().trim();

            if (email.isEmpty() || password.isEmpty() || firstName.isEmpty() || lastName.isEmpty()) {
                Toast.makeText(RegisterActivity.this, "Please fill in all fields", Toast.LENGTH_SHORT).show();
                return;
            }

            if (!Patterns.EMAIL_ADDRESS.matcher(email).matches()) {
                Toast.makeText(RegisterActivity.this, "Invalid email format", Toast.LENGTH_SHORT).show();
                return;
            }

            if (password.length() < 6) {
                Toast.makeText(RegisterActivity.this, "Password must be at least 6 characters", Toast.LENGTH_SHORT).show();
                return;
            }

            if(firstName.length()>20||lastName.length()>20) {
                Toast.makeText(RegisterActivity.this, "name too long", Toast.LENGTH_SHORT).show();
                return;
            }


            btnRegister.setEnabled(false);

            FirebaseAuth fbAuth = FirebaseAuth.getInstance();
            fbAuth.createUserWithEmailAndPassword(email, password).addOnCompleteListener(new OnCompleteListener<AuthResult>() {
                @Override
                public void onComplete(Task<AuthResult> task) {
                    btnRegister.setEnabled(true);

                    if (task.isSuccessful()) {
                        Map<String, Object> userMap = new HashMap<>();
                        userMap.put("email", email);
                        userMap.put("firstName", firstName);
                        userMap.put("lastName", lastName);

                        FirebaseFirestore store = FirebaseFirestore.getInstance();
                        store.collection("user")
                                .document(fbAuth.getCurrentUser().getUid())
                                .set(userMap)
                                .addOnCompleteListener(new OnCompleteListener<Void>() {
                                    @Override
                                    public void onComplete(Task<Void> task) {
                                        if (task.isSuccessful()) {
                                            Toast.makeText(RegisterActivity.this, "User registered successfully", Toast.LENGTH_SHORT).show();
                                            Intent intent = new Intent(RegisterActivity.this, MainActivity.class);
                                            startActivity(intent);
                                            finish();
                                        } else {
                                            String errorMessage = task.getException() != null ? task.getException().getMessage() : "Error saving user data.";
                                            Toast.makeText(RegisterActivity.this, "Error: " + errorMessage, Toast.LENGTH_SHORT).show();
                                        }
                                    }
                                });
                    } else {
                        String errorMessage = task.getException() != null ? task.getException().getMessage() : "Error creating user.";
                        Toast.makeText(RegisterActivity.this, "Error: " + errorMessage, Toast.LENGTH_SHORT).show();
                    }
                }
            });
        }
    }

}
