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

public class ReadingAIActivity extends AppCompatActivity {

    private static final int REQUEST_RECORD_AUDIO_PERMISSION = 200;

    private TextView tvTargetText, tvFeedback;
    private Button btnListen, btnMic, btnNext, btnBack;
    private TextToSpeech tts;
    private SpeechRecognizer speechRecognizer;
    private Intent recognizerIntent;
    private String[] words = new String[0];
    private int index = 0;
    private String username = "";
    private String userGrade = "";
    private ContinuousWordEngine wordEngine;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_reading_ai);

        // Corrected IDs to match activity_reading_ai.xml
        tvTargetText = findViewById(R.id.tvTargetText);
        tvFeedback = findViewById(R.id.tvFeedback);
        btnListen = findViewById(R.id.btnListen);
        btnMic = findViewById(R.id.btnMic);
        btnNext = findViewById(R.id.btnNext);
        btnBack = findViewById(R.id.btnBack);

        SharedPreferences preferences = getSharedPreferences("UserDatabase", MODE_PRIVATE);
        username = preferences.getString("current_user", "guest");
        userGrade = preferences.getString(username + "_grade", "Grade 1");

        // Initialize with 500-word bank and SMART sorting
        wordEngine = new ContinuousWordEngine(this, username, userGrade + "_reading_ai");

        setupBatch();

        tts = new TextToSpeech(this, status -> {
            if (status != TextToSpeech.ERROR) {
                tts.setLanguage(Locale.US);
            }
        });

        initializeSpeechRecognizer();

        btnListen.setOnClickListener(v -> {
            if (words.length > 0) tts.speak(words[index], TextToSpeech.QUEUE_FLUSH, null, null);
        });

        btnMic.setOnClickListener(v -> {
            if (ContextCompat.checkSelfPermission(this, Manifest.permission.RECORD_AUDIO) != PackageManager.PERMISSION_GRANTED) {
                ActivityCompat.requestPermissions(this, new String[]{Manifest.permission.RECORD_AUDIO}, REQUEST_RECORD_AUDIO_PERMISSION);
            } else {
                startListening();
            }
        });

        btnNext.setOnClickListener(v -> {
            // Mark as seen so it goes to the back of the queue
            if (words.length > 0) wordEngine.markWordAsSeen(words[index]);
            
            if (index < words.length - 1) {
                index++;
                updateUI();
            } else {
                setupBatch();
            }
        });

        btnBack.setOnClickListener(v -> finish());
    }

    private void setupBatch() {
        // Fetch fresh words (never seen or oldest first)
        List<String> freshBatch = wordEngine.getNextWords(50);
        words = freshBatch.toArray(new String[0]);
        index = 0;

        if (words.length == 0) {
            tvTargetText.setText("Done!");
            Toast.makeText(this, "Mastered all words!", Toast.LENGTH_SHORT).show();
            return;
        }
        updateUI();
    }

    private void updateUI() {
        if (words.length > 0 && index < words.length) {
            tvTargetText.setText(words[index]);
            if (tvFeedback != null) tvFeedback.setText("");
            
            // Mark as seen immediately when shown
            wordEngine.markWordAsSeen(words[index]);
        }
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
                @Override public void onEndOfSpeech() {}
                @Override public void onError(int error) { 
                    tvFeedback.setText("Try again."); 
                    if (speechRecognizer != null) speechRecognizer.cancel();
                }
                @Override public void onResults(Bundle results) {
                    ArrayList<String> matches = results.getStringArrayList(SpeechRecognizer.RESULTS_RECOGNITION);
                    if (matches != null && !matches.isEmpty()) {
                        checkSpeech(matches.get(0));
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

    private void checkSpeech(String spoken) {
        String target = words[index].toLowerCase().trim();
        String result = spoken.toLowerCase().trim();

        if (result.equalsIgnoreCase(target) || result.contains(target)) {
            tvFeedback.setText("Excellent!");
            tvFeedback.setTextColor(Color.parseColor("#2E7D32"));

            // MASTERED: Will never cycle back
            wordEngine.markWordCompleted(words[index]);

            new Handler(Looper.getMainLooper()).postDelayed(() -> {
                if (index < words.length - 1) {
                    index++;
                    updateUI();
                } else {
                    setupBatch();
                }
            }, 1500);
        } else {
            tvFeedback.setText("You said: " + spoken);
            tvFeedback.setTextColor(Color.RED);
        }
    }

    @Override
    protected void onDestroy() {
        if (tts != null) { tts.stop(); tts.shutdown(); }
        if (speechRecognizer != null) { speechRecognizer.destroy(); }
        super.onDestroy();
    }
}
