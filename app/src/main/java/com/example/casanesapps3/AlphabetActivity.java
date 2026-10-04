package com.example.casanesapps3;

import android.Manifest;
import android.content.Intent;
import android.content.SharedPreferences;
import android.content.pm.PackageManager;
import android.database.Cursor;
import android.graphics.Color;
import android.os.Bundle;
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

public class AlphabetActivity extends AppCompatActivity {

    private static final int REQUEST_RECORD_AUDIO_PERMISSION = 200;
    private TextView tvLetter, tvFeedback;
    private Button btnPrevious, btnNext;
    private TextToSpeech textToSpeech;
    private SpeechRecognizer speechRecognizer;
    private Intent recognizerIntent;

    private List<String> alphabet = new ArrayList<>();
    private int index = 0;
    private String username = "";
    private boolean starsAwarded = false;
    private DatabaseHelper dbHelper;
    private String userGrade = "Grade 1";
    private ContinuousLearningEngine alphabetEngine;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        
        dbHelper = new DatabaseHelper(this);
        SharedPreferences preferences = getSharedPreferences("UserDatabase", MODE_PRIVATE);
        username = preferences.getString("current_user", "");
        
        if (!username.isEmpty()) {
            Cursor cursor = dbHelper.getUser(username);
            if (cursor != null && cursor.moveToFirst()) {
                userGrade = cursor.getString(cursor.getColumnIndexOrThrow(DatabaseHelper.COL_GRADE));
                cursor.close();
            }
        }

        // Safety Check: Only Grade 1
        if (userGrade != null && !userGrade.equalsIgnoreCase("Grade 1")) {
            Toast.makeText(this, "Alphabet is only for Grade 1.", Toast.LENGTH_SHORT).show();
            finish();
            return;
        }

        setContentView(R.layout.activity_alphabet);

        tvLetter = findViewById(R.id.tvLetter);
        TextView tvTitle = findViewById(R.id.tvTitle);
        tvFeedback = findViewById(R.id.tvFeedback);
        btnPrevious = findViewById(R.id.btnPrevious);
        btnNext = findViewById(R.id.btnNext);
        Button btnBack = findViewById(R.id.btnBack);
        Button btnSpeak = findViewById(R.id.btnSpeak);
        Button btnMic = findViewById(R.id.btnMic);

        starsAwarded = preferences.getBoolean(username + "_alphabet_completed", false);

        if (tvTitle != null) tvTitle.setText(getString(R.string.alphabet_title, userGrade));

        alphabetEngine = new ContinuousLearningEngine(this, username, "Grade 1_alphabet");

        setupAlphabet();
        updateUI();

        textToSpeech = new TextToSpeech(this, status -> {
            if (status == TextToSpeech.SUCCESS) applyGlobalVoiceSettings();
        });

        initializeSpeechRecognizer();

        btnSpeak.setOnClickListener(v -> speakTarget());
        btnMic.setOnClickListener(v -> {
            if (ContextCompat.checkSelfPermission(this, Manifest.permission.RECORD_AUDIO) != PackageManager.PERMISSION_GRANTED) {
                ActivityCompat.requestPermissions(this, new String[]{Manifest.permission.RECORD_AUDIO}, REQUEST_RECORD_AUDIO_PERMISSION);
            } else {
                startListening();
            }
        });

        btnNext.setOnClickListener(v -> {
            if (alphabet != null && index < alphabet.size() - 1) {
                index++;
                updateUI();
            } else {
                setupAlphabet();
            }
        });

        btnPrevious.setOnClickListener(v -> {
            if (index > 0) {
                index--;
                updateUI();
            }
        });

        btnBack.setOnClickListener(v -> finish());
    }

    private void setupAlphabet() {
        String[] original = getResources().getStringArray(R.array.alphabet_letters);
        String[] fresh = alphabetEngine.getFreshItems(original);
        
        alphabet = new ArrayList<>();
        // Reverted to 10 items for Alphabet
        int count = Math.min(10, fresh.length);
        for (int i = 0; i < count; i++) {
            alphabet.add(fresh[i]);
        }
        
        index = 0;

        if (alphabet.isEmpty()) {
            Toast.makeText(this, "Mastered all letters in this set!", Toast.LENGTH_SHORT).show();
            return;
        }
        
        updateUI();
    }

    private void applyGlobalVoiceSettings() {
        if (textToSpeech == null) return;
        SharedPreferences preferences = getSharedPreferences("UserDatabase", MODE_PRIVATE);
        float speed = preferences.getFloat("voice_speed", 1.0f);
        String accent = preferences.getString("voice_accent", "US");

        textToSpeech.setSpeechRate(speed);
        Locale locale = Locale.US;
        if ("FIL".equals(accent)) locale = new Locale("en", "PH");
        else if ("UK".equals(accent)) locale = Locale.UK;
        
        int result = textToSpeech.setLanguage(locale);
        if (result == TextToSpeech.LANG_MISSING_DATA || result == TextToSpeech.LANG_NOT_SUPPORTED) {
            textToSpeech.setLanguage(Locale.US);
        }
    }

    private void updateUI() {
        if (alphabet == null || alphabet.isEmpty() || index >= alphabet.size()) return;
        tvLetter.setText(alphabet.get(index));
        if (tvFeedback != null) tvFeedback.setText("");
        btnPrevious.setEnabled(index > 0);
        btnNext.setEnabled(true);
    }

    private void initializeSpeechRecognizer() {
        if (SpeechRecognizer.isRecognitionAvailable(this)) {
            if (speechRecognizer != null) speechRecognizer.destroy();
            speechRecognizer = SpeechRecognizer.createSpeechRecognizer(this);
            recognizerIntent = new Intent(RecognizerIntent.ACTION_RECOGNIZE_SPEECH);
            recognizerIntent.putExtra(RecognizerIntent.EXTRA_LANGUAGE_MODEL, RecognizerIntent.LANGUAGE_MODEL_FREE_FORM);
            recognizerIntent.putExtra(RecognizerIntent.EXTRA_LANGUAGE, Locale.US);
            speechRecognizer.setRecognitionListener(new RecognitionListener() {
                @Override public void onReadyForSpeech(Bundle params) { if (tvFeedback != null) { tvFeedback.setText(getString(R.string.listening_status)); tvFeedback.setTextColor(Color.BLUE); } }
                @Override public void onEndOfSpeech() { if (tvFeedback != null) { tvFeedback.setText(getString(R.string.analyzing_status)); tvFeedback.setTextColor(Color.GRAY); } }
                @Override public void onError(int error) { 
                    if (tvFeedback != null) { tvFeedback.setText(getString(R.string.try_again_status)); tvFeedback.setTextColor(Color.RED); } 
                    if (speechRecognizer != null) speechRecognizer.cancel();
                }
                @Override public void onResults(Bundle results) {
                    ArrayList<String> matches = results.getStringArrayList(SpeechRecognizer.RESULTS_RECOGNITION);
                    if (matches != null && !matches.isEmpty()) checkPronunciation(matches.get(0).toUpperCase());
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

    private void checkPronunciation(String spokenText) {
        if (alphabet == null || alphabet.isEmpty()) return;
        String target = alphabet.get(index).toUpperCase().trim();
        if (spokenText.equals(target) || spokenText.startsWith(target)) {
            if (tvFeedback != null) { 
                tvFeedback.setText(getString(R.string.excellent_status)); 
                tvFeedback.setTextColor(Color.parseColor("#388E3C")); 
            }
            // Save to history
            alphabetEngine.markCompleted(target);

            ConfettiView.show(this, textToSpeech);

            // Auto advance
            tvFeedback.postDelayed(() -> {
                if (index < alphabet.size() - 1) {
                    index++;
                    updateUI();
                } else {
                    awardCompletionStars();
                }
            }, 1500);
        } else {
            if (tvFeedback != null) { 
                tvFeedback.setText(getString(R.string.not_quite_said, spokenText)); 
                tvFeedback.setTextColor(Color.RED); 
            }
            speakTarget();
        }
    }

    private void speakTarget() {
        if (textToSpeech != null && alphabet != null && index < alphabet.size()) { 
            applyGlobalVoiceSettings(); 
            textToSpeech.speak(alphabet.get(index), TextToSpeech.QUEUE_FLUSH, null, null); 
        }
    }

    private void awardCompletionStars() {
        if (!starsAwarded) {
            dbHelper.addStars(username, 50);
            SharedPreferences preferences = getSharedPreferences("UserDatabase", MODE_PRIVATE);
            preferences.edit().putBoolean(username + "_alphabet_completed", true).apply();
            starsAwarded = true;
            Toast.makeText(this, getString(R.string.alphabet_completed_msg), Toast.LENGTH_LONG).show();
        }
        
        setupAlphabet();
    }

    @Override public void onRequestPermissionsResult(int requestCode, @NonNull String[] permissions, @NonNull int[] grantResults) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults);
        if (requestCode == REQUEST_RECORD_AUDIO_PERMISSION && grantResults.length > 0 && grantResults[0] == PackageManager.PERMISSION_GRANTED) startListening();
    }

    @Override protected void onDestroy() {
        super.onDestroy();
        if (textToSpeech != null) { textToSpeech.stop(); textToSpeech.shutdown(); }
        if (speechRecognizer != null) speechRecognizer.destroy();
    }
}
