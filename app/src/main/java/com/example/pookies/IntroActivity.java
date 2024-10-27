package com.example.pookies;

import android.content.Intent;
import android.os.Bundle;
import android.view.WindowManager;
import android.widget.TextView;
import androidx.appcompat.app.AppCompatActivity;

public class IntroActivity extends AppCompatActivity implements ShakeListener.ShakeListenerCallback {

    private ShakeListener shakeListener;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_intro);

        // Prevent the screen from sleeping
        getWindow().addFlags(WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON);

        // Set up shake listener
        shakeListener = new ShakeListener(this, this);
        shakeListener.start();
    }

    @Override
    public void onShake() {
        // Stop shake listener and transition to SplashActivity
        shakeListener.stop();
        Intent intent = new Intent(IntroActivity.this, SplashActivity.class);
        startActivity(intent);
        finish();
    }

    @Override
    protected void onResume() {
        super.onResume();
        shakeListener.start(); // Register the shake listener when the activity is resumed
    }

    @Override
    protected void onPause() {
        super.onPause();
        shakeListener.stop(); // Unregister the shake listener when the activity is paused
    }
}
