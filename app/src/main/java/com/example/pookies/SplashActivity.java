package com.example.pookies;

import android.content.Intent;
import android.os.Bundle;
import android.view.animation.Animation;
import android.view.animation.AnimationUtils;
import android.widget.ImageView;
import androidx.appcompat.app.AppCompatActivity;

public class SplashActivity extends AppCompatActivity {

    private ShakeListener shakeListener;
    private SessionManager sessionManager;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_splash);

        // Initialize SessionManager
        sessionManager = SessionManager.getInstance(this);

        ImageView loadingImage = findViewById(R.id.Loading_vw);
        Animation rotateAnimation = AnimationUtils.loadAnimation(this, R.anim.spin);

        rotateAnimation.setAnimationListener(new Animation.AnimationListener() {
            @Override
            public void onAnimationStart(Animation animation) {
                // Check login status at animation start
                if (sessionManager.isLoggedIn()) {
                    navigateToChat();
                }
            }

            @Override
            public void onAnimationEnd(Animation animation) {
                // Only navigate to MainActivity if user is not logged in
                if (!sessionManager.isLoggedIn()) {
                    navigateToMain();
                }
            }

            @Override
            public void onAnimationRepeat(Animation animation) {
                // Do nothing
            }
        });

        shakeListener = new ShakeListener(this, this::onShakeDetected);
        shakeListener.start();
        loadingImage.startAnimation(rotateAnimation);
    }

    private void navigateToChat() {
        Intent intent = new Intent(SplashActivity.this, ChatActivity.class);
        startActivity(intent);
        finish();
    }

    private void navigateToMain() {
        Intent intent = new Intent(SplashActivity.this, MainActivity.class);
        startActivity(intent);
        finish();
    }

    private void onShakeDetected() {
        if (sessionManager.isLoggedIn()) {
            navigateToChat();
        } else {
            navigateToMain();
        }
    }

    @Override
    protected void onResume() {
        super.onResume();
        shakeListener.start();
    }

    @Override
    protected void onPause() {
        super.onPause();
        shakeListener.stop();
    }
}
