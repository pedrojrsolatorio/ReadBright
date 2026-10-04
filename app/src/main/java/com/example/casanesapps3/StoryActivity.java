package com.example.casanesapps3;

import android.animation.Animator;
import android.animation.AnimatorListenerAdapter;
import android.animation.ObjectAnimator;
import android.content.SharedPreferences;
import android.content.res.TypedArray;
import android.os.Bundle;
import android.speech.tts.TextToSpeech;
import android.view.View;
import android.widget.Button;
import android.widget.ImageView;
import android.widget.TextView;
import android.widget.Toast;

import androidx.appcompat.app.AppCompatActivity;

import com.bumptech.glide.Glide;

import java.util.ArrayList;
import java.util.List;
import java.util.Locale;

public class StoryActivity extends AppCompatActivity {

    private TextView tvStoryTitle, tvStoryContent, tvFeedback;
    private ImageView ivStoryImage;
    private View layoutStoryPage;
    private Button btnPrevious, btnNext;
    private TextToSpeech textToSpeech;

    private String[] titles;
    private String[] stories;
    private int[] images;
    private String grade;
    private String username = "";
    private boolean starsAwarded = false;
    private int index = 0;
    private DatabaseHelper dbHelper;
    private ContinuousLearningEngine storyEngine;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_story);

        dbHelper = new DatabaseHelper(this);

        // Initialize Views
        tvStoryTitle = findViewById(R.id.tvStoryTitle);
        tvStoryContent = findViewById(R.id.tvStoryContent);
        ivStoryImage = findViewById(R.id.ivStoryImage);
        layoutStoryPage = findViewById(R.id.layoutStoryPage);
        tvFeedback = findViewById(R.id.tvFeedback);
        btnPrevious = findViewById(R.id.btnPrevious);
        btnNext = findViewById(R.id.btnNext);
        Button btnReadAloud = findViewById(R.id.btnReadAloud);
        Button btnBack = findViewById(R.id.btnBack);

        SharedPreferences preferences = getSharedPreferences("UserDatabase", MODE_PRIVATE);
        username = preferences.getString("current_user", "");
        grade = getIntent().getStringExtra("grade");
        if (grade == null) grade = preferences.getString(username + "_grade", getString(R.string.default_grade));

        starsAwarded = preferences.getBoolean(username + "_story_" + grade + "_completed", false);
        
        storyEngine = new ContinuousLearningEngine(this, username, grade + "_stories");

        setupStoryList(grade);
        updateUI();

        textToSpeech = new TextToSpeech(this, status -> {
            if (status == TextToSpeech.SUCCESS) applyGlobalVoiceSettings();
        });

        btnReadAloud.setOnClickListener(v -> {
            if (textToSpeech != null && stories != null && index < stories.length) {
                applyGlobalVoiceSettings();
                textToSpeech.speak(stories[index], TextToSpeech.QUEUE_FLUSH, null, null);
            }
        });

        btnNext.setOnClickListener(v -> {
            if (stories != null && index < stories.length - 1) {
                // Save current story to history
                storyEngine.markCompleted(titles[index]);
                playFlipAnimation(true);
            } else if (stories != null && index == stories.length - 1) {
                // Last story in set finished
                storyEngine.markCompleted(titles[index]);
                
                if (!starsAwarded) awardCompletionStars();
                
                Toast.makeText(this, "Loading more new stories...", Toast.LENGTH_SHORT).show();
                setupStoryList(grade);
                index = 0;
                updateUI();
            }
        });

        btnPrevious.setOnClickListener(v -> {
            if (index > 0) {
                playFlipAnimation(false);
            }
        });

        btnBack.setOnClickListener(v -> finish());
    }

    private void playFlipAnimation(boolean next) {
        ObjectAnimator animator = ObjectAnimator.ofFloat(layoutStoryPage, "rotationY", 0f, 90f);
        animator.setDuration(400);
        animator.addListener(new AnimatorListenerAdapter() {
            @Override
            public void onAnimationEnd(Animator animation) {
                if (next) index++; else index--;
                updateUI();
                
                ObjectAnimator.ofFloat(layoutStoryPage, "rotationY", -90f, 0f).setDuration(400).start();
            }
        });
        animator.start();
    }

    private void applyGlobalVoiceSettings() {
        if (textToSpeech == null) return;
        SharedPreferences preferences = getSharedPreferences("UserDatabase", MODE_PRIVATE);
        float speed = preferences.getFloat("voice_speed", 1.0f);
        String accent = preferences.getString("voice_accent", "US");
        textToSpeech.setSpeechRate(speed);
        
        Locale locale = Locale.US;
        if ("FIL".equals(accent)) locale = new Locale("tl", "PH");
        else if ("UK".equals(accent)) locale = Locale.UK;
        textToSpeech.setLanguage(locale);
    }

    private void setupStoryList(String grade) {
        int titlesRes, storiesRes, imagesRes;

        if (grade.contains("3") || grade.contains("4")) {
            titlesRes = R.array.intermediate_titles;
            storiesRes = R.array.intermediate_stories;
            imagesRes = R.array.intermediate_images;
        } else if (grade.contains("5") || grade.contains("6")) {
            titlesRes = R.array.advanced_titles;
            storiesRes = R.array.advanced_stories;
            imagesRes = R.array.advanced_images;
        } else {
            titlesRes = R.array.beginner_titles;
            storiesRes = R.array.beginner_stories;
            imagesRes = R.array.beginner_images;
        }

        String[] allTitles = getResources().getStringArray(titlesRes);
        String[] allStories = getResources().getStringArray(storiesRes);
        TypedArray ta = getResources().obtainTypedArray(imagesRes);
        int[] allImages = new int[ta.length()];
        for (int i = 0; i < ta.length(); i++) {
            allImages[i] = ta.getResourceId(i, 0);
        }
        ta.recycle();

        // Use Engine to get 500 new stories at a time
        String[] freshTitles = storyEngine.getFreshItems(allTitles);
        int count = Math.min(500, freshTitles.length);
        
        if (count == 0) {
            titles = new String[0];
            stories = new String[0];
            images = new int[0];
            Toast.makeText(this, "All stories completed!", Toast.LENGTH_LONG).show();
            return;
        }

        titles = new String[count];
        stories = new String[count];
        images = new int[count];

        for (int i = 0; i < count; i++) {
            String title = freshTitles[i];
            titles[i] = title;
            // Find index in original array to match story and image
            for (int j = 0; j < allTitles.length; j++) {
                if (allTitles[j].equals(title)) {
                    stories[i] = allStories[j];
                    images[i] = allImages[j];
                    break;
                }
            }
        }
    }

    private void updateUI() {
        if (stories == null || stories.length == 0 || index >= stories.length) return;
        tvStoryTitle.setText(titles[index]);
        tvStoryContent.setText(stories[index]);
        
        if (ivStoryImage != null && images != null && index < images.length && images[index] != 0) {
            Glide.with(this)
                 .load(images[index])
                 .into(ivStoryImage);
            ivStoryImage.setVisibility(View.VISIBLE);
        } else if (ivStoryImage != null) {
            ivStoryImage.setVisibility(View.GONE);
        }

        btnPrevious.setEnabled(index > 0);
        btnNext.setEnabled(true);
    }

    private void awardCompletionStars() {
        dbHelper.addStars(username, 50);
        SharedPreferences preferences = getSharedPreferences("UserDatabase", MODE_PRIVATE);
        preferences.edit()
                .putBoolean(username + "_story_" + grade + "_completed", true)
                .apply();
        starsAwarded = true;
        Toast.makeText(this, R.string.story_completed_toast, Toast.LENGTH_LONG).show();
    }

    @Override 
    protected void onDestroy() {
        if (textToSpeech != null) { 
            textToSpeech.stop(); 
            textToSpeech.shutdown(); 
        }
        super.onDestroy();
    }
}
