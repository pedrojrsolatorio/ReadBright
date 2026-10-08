package com.example.casanesapps3;

import android.Manifest;
import android.content.Intent;
import android.animation.Animator;
import android.animation.AnimatorListenerAdapter;
import android.animation.ObjectAnimator;
import android.content.SharedPreferences;
import android.content.pm.PackageManager;
import android.content.res.TypedArray;
import android.graphics.Color;
import android.os.Bundle;
import android.os.Handler;
import android.os.Looper;
import android.speech.RecognitionListener;
import android.speech.RecognizerIntent;
import android.speech.SpeechRecognizer;
import android.speech.tts.TextToSpeech;
import android.view.View;
import android.widget.Button;
import android.widget.ImageView;
import android.widget.TextView;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.app.ActivityCompat;
import androidx.core.content.ContextCompat;

import com.bumptech.glide.Glide;

import java.util.ArrayList;
import java.util.Locale;

public class RhymingActivity extends AppCompatActivity {

    private static final int REQUEST_RECORD_AUDIO_PERMISSION = 200;

    private TextView tvStoryTitle, tvStoryContent, tvFeedback;
    private ImageView ivStoryImage;
    private View layoutStoryPage;
    private Button btnPrevious, btnNext, btnMic, btnStartOver;
    private TextToSpeech textToSpeech;
    private SpeechRecognizer speechRecognizer;
    private Intent recognizerIntent;

    private String[] titles;
    private String[] stories;
    private int[] images;
    private String grade;
    private String username = "";
    private boolean starsAwarded = false;
    private boolean allMastered = false;
    private boolean autoAdvancing = false;
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
        btnMic = findViewById(R.id.btnMic);
        btnStartOver = findViewById(R.id.btnStartOver);
        Button btnReadAloud = findViewById(R.id.btnReadAloud);
        Button btnBack = findViewById(R.id.btnBack);

        SharedPreferences preferences = getSharedPreferences("UserDatabase", MODE_PRIVATE);
        username = preferences.getString("current_user", "");
        grade = getIntent().getStringExtra("grade");
        if (grade == null) grade = preferences.getString(username + "_grade", getString(R.string.default_grade));

        starsAwarded = preferences.getBoolean(username + "_story_" + grade + "_completed", false);
        
        storyEngine = new ContinuousLearningEngine(this, username, grade + "_rhyming");

        setupRhymingList(grade);
        updateUI();

        textToSpeech = new TextToSpeech(this, status -> {
            if (status == TextToSpeech.SUCCESS) applyGlobalVoiceSettings();
        });

        initializeSpeechRecognizer();

        btnReadAloud.setOnClickListener(v -> {
            if (textToSpeech != null && stories != null && index < stories.length) {
                applyGlobalVoiceSettings();
                String toSpeak = titles[index] + ". " + stories[index].replace("•", ",");
                textToSpeech.speak(toSpeak, TextToSpeech.QUEUE_FLUSH, null, null);
            }
        });

        if (btnMic != null) {
            btnMic.setOnClickListener(v -> {
                if (ContextCompat.checkSelfPermission(this, Manifest.permission.RECORD_AUDIO) != PackageManager.PERMISSION_GRANTED) {
                    ActivityCompat.requestPermissions(this, new String[]{Manifest.permission.RECORD_AUDIO}, REQUEST_RECORD_AUDIO_PERMISSION);
                } else {
                    startListening();
                }
            });
        }

        btnNext.setOnClickListener(v -> {
            if (stories == null || stories.length == 0) return;
            storyEngine.markCompleted(titles[index]);
            if (index < stories.length - 1) {
                playFlipAnimation(true);
            } else {
                reloadBatch();
            }
        });

        btnPrevious.setOnClickListener(v -> {
            if (index > 0) {
                playFlipAnimation(false);
            }
        });

        btnStartOver.setOnClickListener(v -> {
            storyEngine.clearProgress();
            allMastered = false;
            setupRhymingList(grade);
            updateUI();
        });

        btnBack.setOnClickListener(v -> finish());
    }

    private void initializeSpeechRecognizer() {
        if (SpeechRecognizer.isRecognitionAvailable(this)) {
            if (speechRecognizer != null) speechRecognizer.destroy();
            speechRecognizer = SpeechRecognizer.createSpeechRecognizer(this);
            recognizerIntent = new Intent(RecognizerIntent.ACTION_RECOGNIZE_SPEECH);
            recognizerIntent.putExtra(RecognizerIntent.EXTRA_LANGUAGE_MODEL, RecognizerIntent.LANGUAGE_MODEL_FREE_FORM);
            recognizerIntent.putExtra(RecognizerIntent.EXTRA_LANGUAGE, Locale.US);

            speechRecognizer.setRecognitionListener(new RecognitionListener() {
                @Override public void onReadyForSpeech(Bundle params) { if (tvFeedback != null) { tvFeedback.setText("Listening..."); tvFeedback.setTextColor(Color.BLUE); } }
                @Override public void onEndOfSpeech() {}
                @Override public void onError(int error) { 
                    if (tvFeedback != null) { tvFeedback.setText("Try again!"); tvFeedback.setTextColor(Color.RED); } 
                    if (speechRecognizer != null) speechRecognizer.cancel();
                }
                @Override public void onResults(Bundle results) {
                    ArrayList<String> matches = results.getStringArrayList(SpeechRecognizer.RESULTS_RECOGNITION);
                    if (matches != null && !matches.isEmpty()) {
                        checkSpokenRhyme(matches.get(0));
                    }
                }
                @Override public void onBeginningOfSpeech() {}
                @Override public void onRmsChanged(float rmsdB) {}
                @Override public void onBufferReceived(byte[] buffer) {}
                @Override public void onPartialResults(Bundle partialResults) {}
                @Override public void onEvent(int eventType, Bundle params) {}
            });
        }
    }

    private void startListening() {
        if (!SpeechRecognizer.isRecognitionAvailable(this)) {
            Toast.makeText(this, "Speech recognition is not available on this device", Toast.LENGTH_SHORT).show();
            return;
        }
        if (speechRecognizer == null) {
            initializeSpeechRecognizer();
        }
        if (speechRecognizer != null) {
            speechRecognizer.cancel();
            speechRecognizer.startListening(recognizerIntent);
        }
    }

    @Override
    public void onRequestPermissionsResult(int requestCode, @NonNull String[] permissions, @NonNull int[] grantResults) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults);
        if (requestCode == REQUEST_RECORD_AUDIO_PERMISSION && grantResults.length > 0 && grantResults[0] == PackageManager.PERMISSION_GRANTED) {
            startListening();
        }
    }

    private void checkSpokenRhyme(String spokenText) {
        if (stories == null || stories.length == 0 || index >= stories.length) return;
        String spoken = spokenText.toLowerCase().trim();
        String wordsList = stories[index].toLowerCase().trim();

        boolean matched = false;
        String[] words = wordsList.split("•");
        for (String w : words) {
            String clean = w.trim();
            if (!clean.isEmpty() && (spoken.contains(clean) || clean.contains(spoken))) {
                matched = true;
                break;
            }
        }

        if (matched) {
            if (autoAdvancing) return;
            autoAdvancing = true;
            if (tvFeedback != null) {
                tvFeedback.setText("Great Job! ⭐ You said: " + spokenText);
                tvFeedback.setTextColor(Color.parseColor("#2E7D32"));
            }
            storyEngine.markCompleted(titles[index]);
            ConfettiView.show(this, textToSpeech);

            new Handler(Looper.getMainLooper()).postDelayed(() -> {
                autoAdvancing = false;
                if (stories != null && index < stories.length - 1) {
                    playFlipAnimation(true);
                } else {
                    reloadBatch();
                }
            }, 1500);
        } else {
            if (tvFeedback != null) {
                tvFeedback.setText("You said: " + spokenText + ". Try saying one of the rhyming words!");
                tvFeedback.setTextColor(Color.RED);
            }
        }
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

    private void setupRhymingList(String grade) {
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

        String[] freshTitles = storyEngine.getFreshItems(allTitles);
        int count = Math.min(500, freshTitles.length);
        
        if (count == 0) {
            // All rhyming items completed!
            if (!starsAwarded) awardCompletionStars();
            allMastered = true;
            titles = null;
            stories = null;
            images = null;
            index = 0;
            return;
        }

        allMastered = false;
        titles = new String[count];
        stories = new String[count];
        images = new int[count];

        for (int i = 0; i < count; i++) {
            String title = freshTitles[i];
            titles[i] = title;
            for (int j = 0; j < allTitles.length; j++) {
                if (allTitles[j].equals(title)) {
                    stories[i] = allStories[j];
                    images[i] = allImages[j];
                    break;
                }
            }
        }
    }

    private void reloadBatch() {
        if (!starsAwarded) awardCompletionStars();
        setupRhymingList(grade);
        index = 0;
        updateUI();
    }

    private void updateUI() {
        if (allMastered) {
            tvStoryTitle.setText("All Mastered!");
            tvStoryContent.setText("You mastered all the rhyming word families! \uD83C\uDF1F");
            if (tvFeedback != null) tvFeedback.setText("");
            if (ivStoryImage != null) ivStoryImage.setVisibility(View.GONE);
            btnPrevious.setEnabled(false);
            btnNext.setEnabled(false);
            if (btnMic != null) btnMic.setEnabled(false);
            if (btnStartOver != null) btnStartOver.setVisibility(View.VISIBLE);
            return;
        }
        if (stories == null || stories.length == 0 || index >= stories.length) {
            tvStoryTitle.setText("Rhyming Words - " + grade);
            tvStoryContent.setText("Great job! All rhyming word families completed!");
            if (ivStoryImage != null) ivStoryImage.setVisibility(View.GONE);
            return;
        }
        tvStoryTitle.setText(titles[index]);
        tvStoryContent.setText(stories[index]);
        if (tvFeedback != null) tvFeedback.setText("");
        
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
        if (btnMic != null) btnMic.setEnabled(true);
        if (btnStartOver != null) btnStartOver.setVisibility(View.GONE);
    }

    private void awardCompletionStars() {
        dbHelper.addStars(username, 50);
        SharedPreferences preferences = getSharedPreferences("UserDatabase", MODE_PRIVATE);
        preferences.edit()
                .putBoolean(username + "_story_" + grade + "_completed", true)
                .apply();
        starsAwarded = true;
        Toast.makeText(this, "Rhyming words set completed! ⭐ +50 Stars", Toast.LENGTH_LONG).show();
    }

    @Override 
    protected void onDestroy() {
        if (textToSpeech != null) { 
            textToSpeech.stop(); 
            textToSpeech.shutdown(); 
        }
        if (speechRecognizer != null) {
            speechRecognizer.destroy();
        }
        super.onDestroy();
    }
}
