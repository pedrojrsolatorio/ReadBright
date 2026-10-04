package com.example.casanesapps3;

import android.Manifest;
import android.content.Intent;
import android.content.SharedPreferences;
import android.content.pm.PackageManager;
import android.graphics.Color;
import android.os.Bundle;
import android.speech.RecognitionListener;
import android.speech.RecognizerIntent;
import android.speech.SpeechRecognizer;
import android.speech.tts.TextToSpeech;
import android.util.TypedValue;
import android.view.Gravity;
import android.view.View;
import android.view.animation.Animation;
import android.view.animation.ScaleAnimation;
import android.widget.Button;
import android.widget.GridLayout;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.TextView;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.app.ActivityCompat;
import androidx.core.content.ContextCompat;

import com.bumptech.glide.Glide;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;
import java.util.Locale;
import java.util.Random;

public class SpellingActivity extends AppCompatActivity {

    private static final int REQUEST_RECORD_AUDIO_PERMISSION = 200;
    private TextView tvFeedback;
    private ImageView ivWordImage;
    private LinearLayout layoutAnswerSlots;
    private GridLayout layoutKeyboard;
    private TextToSpeech textToSpeech;
    private SpeechRecognizer speechRecognizer;
    private Intent recognizerIntent;

    private List<String> wordList = new ArrayList<>();
    private int currentIndex = 0;
    private int score = 0;
    private String username = "";
    private String userGrade = "";
    private DatabaseHelper dbHelper;
    private ContinuousWordEngine spellingEngine; // Use the 500-word engine

    private String currentTargetWord = "";
    private char[] userGuess;
    private final List<Character> keyboardLetters = new ArrayList<>();

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_spelling);

        // FIX: Use Singleton Instance
        dbHelper = DatabaseHelper.getInstance(this);

        TextView tvGradeInfo = findViewById(R.id.tvGradeInfo);
        tvFeedback = findViewById(R.id.tvFeedback);
        ivWordImage = findViewById(R.id.ivWordImage);
        layoutAnswerSlots = findViewById(R.id.layoutAnswerSlots);
        layoutKeyboard = findViewById(R.id.layoutKeyboard);
        Button btnListen = findViewById(R.id.btnListen);
        Button btnMic = findViewById(R.id.btnMic);
        Button btnBack = findViewById(R.id.btnBack);

        if (layoutKeyboard != null) {
            layoutKeyboard.setColumnCount(7);
        }

        SharedPreferences preferences = getSharedPreferences("UserDatabase", MODE_PRIVATE);
        username = preferences.getString("current_user", "");
        userGrade = preferences.getString(username + "_grade", "Grade 1");
        
        if (tvGradeInfo != null) {
            tvGradeInfo.setText("Level: " + userGrade);
        }

        // Initialize with 500-word bank
        spellingEngine = new ContinuousWordEngine(this, username, userGrade + "_spelling");
        
        setupWordList();

        textToSpeech = new TextToSpeech(this, status -> {
            if (status == TextToSpeech.SUCCESS) {
                applyGlobalVoiceSettings();
                speakWord();
            }
        });

        initializeSpeechRecognizer();
        startNewWord();

        btnListen.setOnClickListener(v -> speakWord());
        btnMic.setOnClickListener(v -> {
            if (ContextCompat.checkSelfPermission(this, Manifest.permission.RECORD_AUDIO) != PackageManager.PERMISSION_GRANTED) {
                ActivityCompat.requestPermissions(this, new String[]{Manifest.permission.RECORD_AUDIO}, REQUEST_RECORD_AUDIO_PERMISSION);
            } else {
                startVoiceTyping();
            }
        });
        btnBack.setOnClickListener(v -> finish());
    }

    private void setupWordList() {
        // Fetch 500 FRESH words from the engine
        wordList = spellingEngine.getNextWords(500);
        currentIndex = 0;

        if (wordList.isEmpty()) {
            Toast.makeText(this, "Mastered all words! Starting over...", Toast.LENGTH_SHORT).show();
            // Optional: dbHelper.clearAIHistory(username, userGrade + "_spelling");
        }
    }

    private void startNewWord() {
        if (wordList == null || wordList.isEmpty() || currentIndex >= wordList.size()) {
            setupWordList();
            if (wordList.isEmpty()) return;
        }

        currentTargetWord = wordList.get(currentIndex).toUpperCase();
        userGuess = new char[currentTargetWord.length()];
        Arrays.fill(userGuess, ' ');
        tvFeedback.setText("");
        
        updateWordImage();
        generateKeyboardLetters();
        renderAnswerSlots();
        renderKeyboard();
        speakWord();
    }

    private void checkAnswer() {
        String answer = new String(userGuess);
        if (answer.equalsIgnoreCase(currentTargetWord)) {
            tvFeedback.setText("Correct!");
            tvFeedback.setTextColor(Color.parseColor("#388E3C"));
            score++;
            
            // MASTERED: Save to DB so it never repeats
            spellingEngine.markWordCompleted(wordList.get(currentIndex));
            
            tvFeedback.postDelayed(() -> { 
                currentIndex++; 
                startNewWord(); 
            }, 1200);
        } else {
            tvFeedback.setText("Try Again");
            tvFeedback.setTextColor(Color.RED);
            Arrays.fill(userGuess, ' ');
            renderAnswerSlots();
        }
    }

    private void updateWordImage() {
        if (ivWordImage == null) return;
        String word = currentTargetWord.toLowerCase().trim();
        int resId = getResources().getIdentifier(word, "drawable", getPackageName());
        if (resId != 0) Glide.with(this).load(resId).into(ivWordImage);
        else ivWordImage.setImageResource(android.R.drawable.ic_menu_gallery);
    }

    private void generateKeyboardLetters() {
        keyboardLetters.clear();
        for (char c : currentTargetWord.toCharArray()) keyboardLetters.add(c);
        Random r = new Random();
        while (keyboardLetters.size() < 14) {
            char randomLetter = (char) ('A' + r.nextInt(26));
            keyboardLetters.add(randomLetter);
        }
        Collections.shuffle(keyboardLetters);
    }

    private void renderAnswerSlots() {
        layoutAnswerSlots.removeAllViews();
        int size = dpToPx(38), margin = dpToPx(2);
        for (int i = 0; i < userGuess.length; i++) {
            TextView slot = new TextView(this);
            slot.setText(userGuess[i] == ' ' ? "" : String.valueOf(userGuess[i]));
            slot.setTextSize(TypedValue.COMPLEX_UNIT_SP, 18);
            slot.setGravity(Gravity.CENTER);
            slot.setTextColor(Color.BLACK);
            slot.setBackgroundResource(android.R.drawable.editbox_background_normal);
            LinearLayout.LayoutParams params = new LinearLayout.LayoutParams(size, size);
            params.setMargins(margin, margin, margin, margin);
            slot.setLayoutParams(params);
            layoutAnswerSlots.addView(slot);
        }
    }

    private void renderKeyboard() {
        layoutKeyboard.removeAllViews();
        int size = dpToPx(40), margin = dpToPx(2);
        for (char c : keyboardLetters) {
            Button btn = new Button(this);
            btn.setText(String.valueOf(c));
            btn.setBackgroundColor(Color.parseColor("#FF9800"));
            btn.setTextColor(Color.WHITE);
            GridLayout.LayoutParams params = new GridLayout.LayoutParams();
            params.width = size; params.height = size;
            params.setMargins(margin, margin, margin, margin);
            btn.setLayoutParams(params);
            btn.setOnClickListener(v -> addLetterToGuess(c));
            layoutKeyboard.addView(btn);
        }
    }

    private void addLetterToGuess(char c) {
        for (int i = 0; i < userGuess.length; i++) {
            if (userGuess[i] == ' ') {
                userGuess[i] = c;
                renderAnswerSlots();
                boolean full = true;
                for (char g : userGuess) { if (g == ' ') { full = false; break; } }
                if (full) checkAnswer();
                return;
            }
        }
    }

    private void speakWord() {
        if (textToSpeech != null && !currentTargetWord.isEmpty()) {
            textToSpeech.speak(currentTargetWord.toLowerCase(), TextToSpeech.QUEUE_FLUSH, null, null);
        }
    }

    private void applyGlobalVoiceSettings() {
        SharedPreferences prefs = getSharedPreferences("UserDatabase", MODE_PRIVATE);
        textToSpeech.setSpeechRate(prefs.getFloat("voice_speed", 1.0f));
        textToSpeech.setLanguage(Locale.US);
    }

    private void startVoiceTyping() { 
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
                        String spoken = matches.get(0).toUpperCase().trim();
                        if (spoken.equals(currentTargetWord)) {
                            for (int i = 0; i < currentTargetWord.length(); i++) userGuess[i] = currentTargetWord.charAt(i);
                            renderAnswerSlots(); checkAnswer();
                        }
                    }
                }
                @Override public void onError(int error) { 
                    tvFeedback.setText("Try again!"); 
                    if (speechRecognizer != null) speechRecognizer.cancel();
                }
                @Override public void onBeginningOfSpeech() {}
                @Override public void onRmsChanged(float rmsdB) {}
                @Override public void onBufferReceived(byte[] buffer) {}
                @Override public void onEndOfSpeech() {}
                @Override public void onPartialResults(Bundle partialResults) {}
                @Override public void onEvent(int eventType, Bundle params) {}
            });
        }
    }

    @Override
    public void onRequestPermissionsResult(int requestCode, @NonNull String[] permissions, @NonNull int[] grantResults) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults);
        if (requestCode == REQUEST_RECORD_AUDIO_PERMISSION && grantResults.length > 0 && grantResults[0] == PackageManager.PERMISSION_GRANTED) {
            startVoiceTyping();
        }
    }

    private int dpToPx(int dp) { return (int) TypedValue.applyDimension(TypedValue.COMPLEX_UNIT_DIP, dp, getResources().getDisplayMetrics()); }

    @Override protected void onDestroy() {
        if (textToSpeech != null) { textToSpeech.stop(); textToSpeech.shutdown(); }
        if (speechRecognizer != null) speechRecognizer.destroy();
        super.onDestroy();
    }
}
