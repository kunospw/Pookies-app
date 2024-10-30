package com.example.pookies;

import android.os.Bundle;
import android.content.Intent;
import android.content.SharedPreferences;
import androidx.appcompat.app.AppCompatActivity;
import android.view.View;
import android.widget.Button;

public class MainActivity extends AppCompatActivity {

    Button rgBtn, lgBtn;
    DBHelper dbHelper;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        // Check for existing session before setting content view
        if (checkSession()) {
            redirectToChatActivity();
            return;
        }

        setContentView(R.layout.activity_main);
        dbHelper = new DBHelper(this);

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

    private boolean checkSession() {
        SharedPreferences prefs = getSharedPreferences("UserPrefs", MODE_PRIVATE);
        String email = prefs.getString("email", null);
        String userId = prefs.getString("user_id", null);

        // Check if both email and userId exist in preferences
        if (email != null && userId != null) {
            // Verify that the user still exists in the database
            dbHelper = new DBHelper(this);
            User user = dbHelper.getUserByEmail(email);
            return user != null && userId.equals(user.getUserId());
        }
        return false;
    }

    private void redirectToChatActivity() {
        Intent intent = new Intent(this, ChatActivity.class);
        intent.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK | Intent.FLAG_ACTIVITY_CLEAR_TASK);
        startActivity(intent);
        finish();
    }
}