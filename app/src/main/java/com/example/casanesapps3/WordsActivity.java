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

public class WordsActivity extends AppCompatActivity {

    private static final int REQUEST_RECORD_AUDIO_PERMISSION = 200;

    private TextView tvWord, tvFeedback;
    private Button btnSpeak, btnMic, btnPrevious, btnNext, btnBack;
    private TextToSpeech tts;
    private SpeechRecognizer speechRecognizer;
    private Intent recognizerIntent;
    private String[] words = new String[0];
    private int index = 0;
    private String userGrade;
    private String username = "";
    private DatabaseHelper dbHelper;
    private ContinuousWordEngine wordEngine; 
    private boolean starsAwarded = false;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_words);

        dbHelper = DatabaseHelper.getInstance(this);

        tvWord = findViewById(R.id.tvWord);
        tvFeedback = findViewById(R.id.tvFeedback);
        btnSpeak = findViewById(R.id.btnSpeak);
        btnMic = findViewById(R.id.btnMic);
        btnPrevious = findViewById(R.id.btnPrevious);
        btnNext = findViewById(R.id.btnNext);
        btnBack = findViewById(R.id.btnBack);

        if (btnPrevious != null) {
            btnPrevious.setVisibility(View.GONE);
        }

        SharedPreferences preferences = getSharedPreferences("UserDatabase", MODE_PRIVATE);
        username = preferences.getString("current_user", "");
        userGrade = getIntent().getStringExtra("grade");
        if (userGrade == null) {
            userGrade = preferences.getString(username + "_grade", "Grade 1");
        }

        starsAwarded = preferences.getBoolean(username + "_words_" + userGrade + "_completed", false);
        
        wordEngine = new ContinuousWordEngine(this, username, userGrade + "_words");

        setupWordsList();

        tts = new TextToSpeech(this, status -> {
            if (status != TextToSpeech.ERROR) {
                tts.setLanguage(Locale.US);
            }
        });

        initializeSpeechRecognizer();
        updateUI();

        btnSpeak.setOnClickListener(v -> {
            if (words != null && index < words.length) {
                tts.speak(words[index], TextToSpeech.QUEUE_FLUSH, null, null);
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
            // Mark as seen before moving to next so it doesn't cycle back immediately
            if (words != null && index < words.length) {
                wordEngine.markWordAsSeen(words[index]);
            }

            if (index < words.length - 1) {
                index++;
                updateUI();
            } else {
                setupWordsList();
            }
        });

        btnBack.setOnClickListener(v -> finish());
    }

    private void setupWordsList() {
        // Fetch 50 words at a time, prioritized by fresh content
        List<String> freshWords = wordEngine.getNextWords(50);
        words = freshWords.toArray(new String[0]);

        index = 0;

        if (words.length == 0) {
            tvWord.setText("Great Job!");
            Toast.makeText(this, "You have mastered all available words!", Toast.LENGTH_LONG).show();
            return;
        }

        updateUI();
    }

    private void updateUI() {
        if (words == null || words.length == 0 || index >= words.length) return;
        tvWord.setText(words[index]);
        if (tvFeedback != null) tvFeedback.setText("");
        
        // Mark as seen immediately when shown
        wordEngine.markWordAsSeen(words[index]);
    }

    private void initializeSpeechRecognizer() {
        if (SpeechRecognizer.isRecognitionAvailable(this)) {
            if (speechRecognizer != null) speechRecognizer.destroy();
            speechRecognizer = SpeechRecognizer.createSpeechRecognizer(this);
            recognizerIntent = new Intent(RecognizerIntent.ACTION_RECOGNIZE_SPEECH);
            recognizerIntent.putExtra(RecognizerIntent.EXTRA_LANGUAGE_MODEL, RecognizerIntent.LANGUAGE_MODEL_FREE_FORM);
            recognizerIntent.putExtra(RecognizerIntent.EXTRA_LANGUAGE, Locale.US);

            speechRecognizer.setRecognitionListener(new RecognitionListener() {
                @Override public void onReadyForSpeech(Bundle params) { tvFeedback.setText(R.string.listening_status); tvFeedback.setTextColor(Color.BLUE); }
                @Override public void onEndOfSpeech() {}
                @Override public void onError(int error) { 
                    tvFeedback.setText(R.string.try_again_status); 
                    tvFeedback.setTextColor(Color.RED); 
                    if (speechRecognizer != null) speechRecognizer.cancel();
                }
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
        if (words == null || words.length == 0) return;
        String targetWord = words[index].toLowerCase().trim();
        String spoken = spokenText.toLowerCase().trim();
        
        if (spoken.equalsIgnoreCase(targetWord) || spoken.contains(targetWord)) {
            tvFeedback.setText(R.string.excellent_status);
            tvFeedback.setTextColor(Color.parseColor("#2E7D32"));
            
            // MASTERED: Move to history (Never show again)
            wordEngine.markWordCompleted(words[index]);
            
            new Handler(Looper.getMainLooper()).postDelayed(() -> {
                if (index < words.length - 1) {
                    index++;
                    updateUI();
                } else {
                    awardCompletionStars();
                    setupWordsList();
                }
            }, 1500);
            
        } else {
            tvFeedback.setText(getString(R.string.you_said_status, spokenText));
            tvFeedback.setTextColor(Color.RED);
        }
    }

    private void awardCompletionStars() {
        if (!starsAwarded) {
            dbHelper.addStars(username, 50);
            getSharedPreferences("UserDatabase", MODE_PRIVATE).edit()
                    .putBoolean(username + "_words_" + userGrade + "_completed", true).apply();
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
