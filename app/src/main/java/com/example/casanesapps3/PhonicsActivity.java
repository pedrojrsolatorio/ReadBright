package com.example.casanesapps3;

import android.content.Intent;
import android.content.SharedPreferences;
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
import android.widget.TextView;
import android.widget.Toast;

import androidx.appcompat.app.AppCompatActivity;
import java.util.ArrayList;
import java.util.List;
import java.util.Locale;

public class PhonicsActivity extends AppCompatActivity {

    private TextView tvWord, tvFeedback;
    private Button btnSpeak, btnMic, btnNext, btnBack;
    private TextToSpeech tts;
    private SpeechRecognizer speechRecognizer;
    private Intent recognizerIntent;
    private String[] items = new String[0];
    private int index = 0;
    private String userGrade;
    private String username = "";
    private DatabaseHelper dbHelper;
    private ContinuousLearningEngine learningEngine;
    private boolean starsAwarded = false;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_phonics);

        dbHelper = DatabaseHelper.getInstance(this);

        tvWord = findViewById(R.id.tvWord);
        tvFeedback = findViewById(R.id.tvFeedback);
        btnSpeak = findViewById(R.id.btnSpeak);
        btnMic = findViewById(R.id.btnMic);
        btnNext = findViewById(R.id.btnNext);
        btnBack = findViewById(R.id.btnBack);

        SharedPreferences preferences = getSharedPreferences("UserDatabase", MODE_PRIVATE);
        username = preferences.getString("current_user", "");
        userGrade = preferences.getString(username + "_grade", "Grade 1");

        starsAwarded = preferences.getBoolean(username + "_phonics_" + userGrade + "_completed", false);
        
        learningEngine = new ContinuousLearningEngine(this, username, userGrade + "_phonics");

        setupPhonicsList();

        tts = new TextToSpeech(this, status -> {
            if (status != TextToSpeech.ERROR) {
                tts.setLanguage(Locale.US);
            }
        });

        initializeSpeechRecognizer();

        btnSpeak.setOnClickListener(v -> {
            if (items != null && index < items.length) {
                tts.speak(items[index], TextToSpeech.QUEUE_FLUSH, null, null);
            }
        });

        btnMic.setOnClickListener(v -> {
            if (speechRecognizer != null) {
                speechRecognizer.startListening(recognizerIntent);
            }
        });

        btnNext.setOnClickListener(v -> {
            // Mark as seen before moving to next so it doesn't cycle back immediately
            if (items != null && index < items.length) {
                learningEngine.markAsSeen(items[index]);
            }

            if (index < items.length - 1) {
                index++;
                updateUI();
            } else {
                setupPhonicsList();
            }
        });

        btnBack.setOnClickListener(v -> finish());
        
        updateUI();
    }

    private void setupPhonicsList() {
        String[] source;
        if (userGrade.contains("1")) source = getResources().getStringArray(R.array.phonics_g1);
        else if (userGrade.contains("2")) source = getResources().getStringArray(R.array.phonics_g2);
        else if (userGrade.contains("3")) source = getResources().getStringArray(R.array.phonics_g3);
        else source = getResources().getStringArray(R.array.phonics_g4);

        // Fetch 50 items at a time
        List<String> freshItems = learningEngine.getFreshItems(source, 50);
        items = freshItems.toArray(new String[0]);
        index = 0;

        if (items.length == 0) {
            tvWord.setText("Mastered!");
        }
    }

    private void updateUI() {
        if (items == null || items.length == 0 || index >= items.length) return;
        tvWord.setText(items[index]);
        if (tvFeedback != null) tvFeedback.setText("");
        
        // Track seen status immediately
        learningEngine.markAsSeen(items[index]);
    }

    private void initializeSpeechRecognizer() {
        speechRecognizer = SpeechRecognizer.createSpeechRecognizer(this);
        recognizerIntent = new Intent(RecognizerIntent.ACTION_RECOGNIZE_SPEECH);
        recognizerIntent.putExtra(RecognizerIntent.EXTRA_LANGUAGE_MODEL, RecognizerIntent.LANGUAGE_MODEL_FREE_FORM);
        recognizerIntent.putExtra(RecognizerIntent.EXTRA_LANGUAGE, Locale.US);

        speechRecognizer.setRecognitionListener(new RecognitionListener() {
            @Override public void onReadyForSpeech(Bundle params) { tvFeedback.setText("Listening..."); }
            @Override public void onEndOfSpeech() {}
            @Override public void onError(int error) { tvFeedback.setText("Try again."); }
            @Override public void onResults(Bundle results) {
                ArrayList<String> matches = results.getStringArrayList(SpeechRecognizer.RESULTS_RECOGNITION);
                if (matches != null && !matches.isEmpty()) {
                    checkSpokenWord(matches.get(0));
                }
            }
            @Override public void onBeginningOfSpeech() {}
            @Override public void onRmsChanged(float rmsdB) {}
            @Override public void onBufferReceived(byte[] buffer) {}
            @Override public void onPartialResults(Bundle partialResults) {}
            @Override public void onEvent(int eventType, Bundle params) {}
        });
    }

    private void checkSpokenWord(String spokenText) {
        if (items == null || items.length == 0) return;
        String target = items[index].toLowerCase().trim();
        String spoken = spokenText.toLowerCase().trim();
        
        if (spoken.contains(target) || target.contains(spoken)) {
            tvFeedback.setText("Excellent!");
            tvFeedback.setTextColor(Color.parseColor("#2E7D32"));
            
            // MASTERED: Save permanently to history
            learningEngine.markCompleted(items[index]);
            
            new Handler(Looper.getMainLooper()).postDelayed(() -> {
                if (index < items.length - 1) {
                    index++;
                    updateUI();
                } else {
                    awardCompletionStars();
                    setupPhonicsList();
                }
            }, 1500);
            
        } else {
            tvFeedback.setText("You said: " + spokenText);
            tvFeedback.setTextColor(Color.RED);
        }
    }

    private void awardCompletionStars() {
        if (!starsAwarded) {
            dbHelper.addStars(username, 30);
            getSharedPreferences("UserDatabase", MODE_PRIVATE).edit()
                    .putBoolean(username + "_phonics_" + userGrade + "_completed", true).apply();
            starsAwarded = true;
        }
    }

    @Override
    protected void onDestroy() {
        if (tts != null) { tts.stop(); tts.shutdown(); }
        if (speechRecognizer != null) { speechRecognizer.destroy(); }
        super.onDestroy();
    }
}
