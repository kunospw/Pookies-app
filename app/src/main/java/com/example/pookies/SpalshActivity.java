package com.example.pookies;

import android.content.Intent;
import android.os.Bundle;
import android.view.animation.Animation;
import android.view.animation.AnimationUtils;
import android.widget.ImageView;
import androidx.appcompat.app.AppCompatActivity;

public class SpalshActivity extends AppCompatActivity {

    private ShakeListener shakeListener;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_splash);

        // Find the ImageView
        ImageView loadingImage = findViewById(R.id.Loading_vw);

        // Load the rotate animation
        Animation rotateAnimation = AnimationUtils.loadAnimation(this, R.anim.spin);

        // Set an AnimationListener to detect when the animation ends
        rotateAnimation.setAnimationListener(new Animation.AnimationListener() {
            @Override
            public void onAnimationStart(Animation animation) {
                // Do nothing when the animation starts
            }

            @Override
            public void onAnimationEnd(Animation animation) {
                // When the animation ends, transition to MainActivity
                Intent intent = new Intent(SpalshActivity.this, MainActivity.class);
                startActivity(intent);
                finish(); // Close the splash activity
            }

            @Override
            public void onAnimationRepeat(Animation animation) {
                // Do nothing on animation repeat
            }
        });

        // Start the shake listener
        shakeListener = new ShakeListener(this, this::onShakeDetected);
        shakeListener.start();

        // Start the animation
        loadingImage.startAnimation(rotateAnimation);
    }

    private void onShakeDetected() {
        // Directly transition to MainActivity when shake is detected
        Intent intent = new Intent(SpalshActivity.this, MainActivity.class);
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
