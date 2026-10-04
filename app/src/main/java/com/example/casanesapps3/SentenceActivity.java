package com.example.casanesapps3;

import android.Manifest;
import android.content.Intent;
import android.content.SharedPreferences;
import android.content.pm.PackageManager;
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

import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.app.ActivityCompat;
import androidx.core.content.ContextCompat;

import java.util.ArrayList;
import java.util.List;
import java.util.Locale;

public class SentenceActivity extends AppCompatActivity {

    private static final int REQUEST_RECORD_AUDIO_PERMISSION = 200;

    private TextView tvSentence, tvFeedback;
    private Button btnSpeak, btnMic, btnNext, btnBack;
    private TextToSpeech tts;
    private SpeechRecognizer speechRecognizer;
    private Intent recognizerIntent;
    private String[] sentences = new String[0];
    private int index = 0;
    private String userGrade;
    private String username = "";
    private DatabaseHelper dbHelper;
    private ContinuousLearningEngine learningEngine;
    private boolean starsAwarded = false;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_sentence);

        dbHelper = DatabaseHelper.getInstance(this);

        tvSentence = findViewById(R.id.tvSentence);
        tvFeedback = findViewById(R.id.tvFeedback);
        btnSpeak = findViewById(R.id.btnSpeak);
        btnMic = findViewById(R.id.btnMic);
        btnNext = findViewById(R.id.btnNext);
        btnBack = findViewById(R.id.btnBack);

        SharedPreferences preferences = getSharedPreferences("UserDatabase", MODE_PRIVATE);
        username = preferences.getString("current_user", "guest");
        userGrade = preferences.getString(username + "_grade", "Grade 1");

        starsAwarded = preferences.getBoolean(username + "_sentences_" + userGrade + "_completed", false);
        
        // Initialize Engine
        learningEngine = new ContinuousLearningEngine(this, username, userGrade + "_sentences");

        setupSentencesList();

        tts = new TextToSpeech(this, status -> {
            if (status != TextToSpeech.ERROR) {
                tts.setLanguage(Locale.US);
            }
        });

        initializeSpeechRecognizer();

        btnSpeak.setOnClickListener(v -> {
            if (sentences != null && index < sentences.length) {
                tts.speak(sentences[index], TextToSpeech.QUEUE_FLUSH, null, null);
            }
        });

        btnMic.setOnClickListener(v -> {
            if (ContextCompat.checkSelfPermission(this, Manifest.permission.RECORD_AUDIO) != PackageManager.PERMISSION_GRANTED) {
                ActivityCompat.requestPermissions(this, new String[]{Manifest.permission.RECORD_AUDIO}, REQUEST_RECORD_AUDIO_PERMISSION);
            } else {
                startListening();
            }
        });

        btnNext.setOnClickListener(v -> {
            // Mark as seen so it rotates to the back
            if (sentences != null && index < sentences.length) {
                learningEngine.markAsSeen(sentences[index]);
            }

            if (index < sentences.length - 1) {
                index++;
                updateUI();
            } else {
                setupSentencesList();
            }
        });

        btnBack.setOnClickListener(v -> finish());
        
        updateUI();
    }

    private void setupSentencesList() {
        String[] source;
        if (userGrade.contains("1")) source = getResources().getStringArray(R.array.sentences_g1);
        else if (userGrade.contains("2")) source = getResources().getStringArray(R.array.sentences_g2);
        else if (userGrade.contains("3")) source = getResources().getStringArray(R.array.sentences_g3);
        else source = getResources().getStringArray(R.array.sentences_g4);

        // Fetch 30 sentences at a time with Smart Rotation
        List<String> freshItems = learningEngine.getFreshItems(source, 30);
        sentences = freshItems.toArray(new String[0]);
        index = 0;

        if (sentences.length == 0) {
            tvSentence.setText("All Mastered!");
        }
    }

    private void updateUI() {
        if (sentences == null || sentences.length == 0 || index >= sentences.length) return;
        tvSentence.setText(sentences[index]);
        if (tvFeedback != null) tvFeedback.setText("");
        
        // Mark as seen immediately
        learningEngine.markAsSeen(sentences[index]);
    }

    private void initializeSpeechRecognizer() {
        if (SpeechRecognizer.isRecognitionAvailable(this)) {
            if (speechRecognizer != null) speechRecognizer.destroy();
            speechRecognizer = SpeechRecognizer.createSpeechRecognizer(this);
            recognizerIntent = new Intent(RecognizerIntent.ACTION_RECOGNIZE_SPEECH);
            recognizerIntent.putExtra(RecognizerIntent.EXTRA_LANGUAGE_MODEL, RecognizerIntent.LANGUAGE_MODEL_FREE_FORM);
            recognizerIntent.putExtra(RecognizerIntent.EXTRA_LANGUAGE, Locale.US);

            speechRecognizer.setRecognitionListener(new RecognitionListener() {
                @Override public void onReadyForSpeech(Bundle params) { tvFeedback.setText("Listening..."); }
                @Override public void onResults(Bundle results) {
                    ArrayList<String> matches = results.getStringArrayList(SpeechRecognizer.RESULTS_RECOGNITION);
                    if (matches != null && !matches.isEmpty()) {
                        checkSpokenWord(matches.get(0));
                    }
                }
                @Override public void onError(int error) { 
                    tvFeedback.setText("Try again."); 
                    if (speechRecognizer != null) speechRecognizer.cancel();
                }
                @Override public void onEndOfSpeech() {}
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

    private void checkSpokenWord(String spokenText) {
        if (sentences == null || sentences.length == 0) return;
        String target = sentences[index].toLowerCase().trim();
        String spoken = spokenText.toLowerCase().trim();
        
        if (spoken.contains(target) || target.contains(spoken)) {
            tvFeedback.setText("Excellent!");
            tvFeedback.setTextColor(Color.parseColor("#2E7D32"));
            
            // MASTERED: Move permanently to history
            learningEngine.markCompleted(sentences[index]);
            
            new Handler(Looper.getMainLooper()).postDelayed(() -> {
                if (index < sentences.length - 1) {
                    index++;
                    updateUI();
                } else {
                    awardCompletionStars();
                    setupSentencesList();
                }
            }, 1500);
            
        } else {
            tvFeedback.setText("You said: " + spokenText);
            tvFeedback.setTextColor(Color.RED);
        }
    }

    private void awardCompletionStars() {
        if (!starsAwarded) {
            dbHelper.addStars(username, 40);
            getSharedPreferences("UserDatabase", MODE_PRIVATE).edit()
                    .putBoolean(username + "_sentences_" + userGrade + "_completed", true).apply();
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
