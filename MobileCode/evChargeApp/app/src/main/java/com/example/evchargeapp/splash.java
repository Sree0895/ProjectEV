package com.example.evchargeapp;

import androidx.appcompat.app.AppCompatActivity;

import android.content.Intent;
import android.os.Bundle;
import android.os.Handler;
import android.os.Looper;
import android.view.WindowManager;
import android.view.animation.AlphaAnimation;
import android.view.animation.Animation;
import android.view.animation.AnimationUtils;
import android.view.animation.LinearInterpolator;
import android.widget.ImageView;
import android.widget.TextView;


public class splash extends AppCompatActivity {

    Animation flashAnimation;
    Animation topAnimation;
    Animation bottomAnimation;
    ImageView mLogoView;
    TextView mTextView1;
    TextView mTextView2;
    TextView mTextView3;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        setContentView(R.layout.activity_splash);
        getWindow().setFlags(WindowManager.LayoutParams.FLAG_FULLSCREEN,
                WindowManager.LayoutParams.FLAG_FULLSCREEN);

        flashAnimation = new AlphaAnimation((float) 0.5, (float) 0.1); // Change alpha from fully visible to invisible
        flashAnimation.setDuration(500); // duration - half a second
        flashAnimation.setInterpolator(new LinearInterpolator()); // do not alter
        // animation
        // rate
        flashAnimation.setRepeatCount(Animation.INFINITE); // Repeat animation
        // infinitely
        flashAnimation.setRepeatMode(Animation.REVERSE); // Reverse animation at the
        // end so the button will
        // fade back in

        topAnimation = AnimationUtils.loadAnimation(this,R.anim.top_animation);
        bottomAnimation = AnimationUtils.loadAnimation(this, R.anim.bottom_animation);
        mLogoView = findViewById(R.id.imageView);
        mLogoView.startAnimation(topAnimation);

        mTextView1 = findViewById(R.id.textView1);
        mTextView2 = findViewById(R.id.textView2);
        mTextView3 = findViewById(R.id.textView3);

        mTextView1.startAnimation(bottomAnimation);
        mTextView2.startAnimation(bottomAnimation);
        mTextView3.startAnimation(bottomAnimation);

        new Handler(Looper.getMainLooper()).postDelayed(new Runnable() {
            @Override
            public void run() {
                Intent intent;
                intent = new Intent(getApplicationContext(), config.class);
                startActivity(intent);
                finish();
            }
        }, 3000);
    }
}