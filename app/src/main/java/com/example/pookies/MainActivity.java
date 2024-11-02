package com.example.pookies;

import android.os.Bundle;
import android.content.Intent;
import androidx.appcompat.app.AppCompatActivity;
import android.view.View;
import android.widget.Button;

public class MainActivity extends AppCompatActivity {

    private Button rgBtn, lgBtn;
    private SessionManager sessionManager;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        // Initialize SessionManager
        sessionManager = SessionManager.getInstance(this);

        // Check if user is already logged in
        if (sessionManager.isLoggedIn()) {
            // User is logged in, redirect to ChatActivity
            startActivity(new Intent(MainActivity.this, ChatActivity.class));
            finish(); // Close MainActivity so user can't go back to it
            return;
        }

        // If user is not logged in, show the main layout
        setContentView(R.layout.activity_main);

        rgBtn = findViewById(R.id.rgBtn);
        lgBtn = findViewById(R.id.lgBtn);

        rgBtn.setOnClickListener(view -> {
            Intent openRegister = new Intent(getApplicationContext(), RegisterActivity.class);
            startActivity(openRegister);
        });

        lgBtn.setOnClickListener(view -> {
            Intent openLogin = new Intent(getApplicationContext(), LoginActivity.class);
            startActivity(openLogin);
        });
    }

    @Override
    protected void onResume() {
        super.onResume();
        // Check login status when activity resumes
        if (sessionManager.isLoggedIn()) {
            startActivity(new Intent(MainActivity.this, ChatActivity.class));
            finish();
        }
    }
}