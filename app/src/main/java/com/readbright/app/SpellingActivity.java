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
    private TextView tvEmojiFallback;
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
    private final List<Button> keyboardButtons = new ArrayList<>();
    private final int[] keyRemaining = new int[26];

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_spelling);

        // FIX: Use Singleton Instance
        dbHelper = DatabaseHelper.getInstance(this);

        TextView tvGradeInfo = findViewById(R.id.tvGradeInfo);
        tvFeedback = findViewById(R.id.tvFeedback);
        ivWordImage = findViewById(R.id.ivWordImage);
        tvEmojiFallback = findViewById(R.id.tvEmojiFallback);
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
            tvFeedback.setTextColor(Color.parseColor("#388E3C"));
            score++;
            
            // MASTERED: Save to DB so it never repeats
            spellingEngine.markWordCompleted(wordList.get(currentIndex));
            boolean bonusStar = awardPracticeStars();
            tvFeedback.setText(bonusStar ? "Correct! \u2B50 +1" : "Correct!");

            ConfettiView.show(this, textToSpeech);
            
            tvFeedback.postDelayed(() -> { 
                currentIndex++; 
                startNewWord(); 
            }, 1200);
        } else {
            tvFeedback.setText("Try Again");
            tvFeedback.setTextColor(Color.RED);
            for (char g : userGuess) {
                if (g != ' ') keyRemaining[g - 'A']++;
            }
            Arrays.fill(userGuess, ' ');
            refreshKeyboardButtons();
            renderAnswerSlots();
        }
    }

    // +1 star per correct spelling, capped at 50 per grade
    private boolean awardPracticeStars() {
        SharedPreferences prefs = getSharedPreferences("UserDatabase", MODE_PRIVATE);
        String key = username + "_spelling_" + userGrade + "_earned";
        int earned = prefs.getInt(key, 0);
        if (earned < 50) {
            dbHelper.addStars(username, 1);
            prefs.edit().putInt(key, earned + 1).apply();
            return true;
        }
        return false;
    }

    private void updateWordImage() {
        if (ivWordImage == null || currentTargetWord == null) return;
        String cleanName = currentTargetWord.toLowerCase().replaceAll("[^a-z]", "").trim();
        int resId = getResources().getIdentifier(cleanName + "_image", "drawable", getPackageName());
        if (resId == 0) resId = getResources().getIdentifier(cleanName + "_real", "drawable", getPackageName());
        if (resId == 0) resId = getResources().getIdentifier(cleanName, "drawable", getPackageName());

        if (resId == 0) {
            String[] words = currentTargetWord.toLowerCase().split("\\s+");
            for (String w : words) {
                String keyword = w.replaceAll("[^a-z]", "");
                if (keyword.isEmpty()) continue;
                resId = getResources().getIdentifier(keyword + "_image", "drawable", getPackageName());
                if (resId == 0) resId = getResources().getIdentifier(keyword + "_real", "drawable", getPackageName());
                if (resId == 0) resId = getResources().getIdentifier(keyword, "drawable", getPackageName());
                if (resId != 0) break;
            }
        }

        if (resId != 0) {
            Glide.with(this).load(resId).into(ivWordImage);
            ivWordImage.setVisibility(View.VISIBLE);
            tvEmojiFallback.setVisibility(View.GONE);
        } else {
            ivWordImage.setVisibility(View.GONE);
            String emoji = EmojiLookup.emojiFor(currentTargetWord);
            if (emoji != null) {
                tvEmojiFallback.setText(emoji);
                tvEmojiFallback.setVisibility(View.VISIBLE);
            } else {
                tvEmojiFallback.setVisibility(View.GONE);
            }
        }
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
        Arrays.fill(keyRemaining, 0);
        for (char c : keyboardLetters) keyRemaining[c - 'A']++;
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
            final int idx = i;
            slot.setOnClickListener(v -> removeLetterFromGuess(idx));
            layoutAnswerSlots.addView(slot);
        }
    }

    private void renderKeyboard() {
        layoutKeyboard.removeAllViews();
        keyboardButtons.clear();
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
            keyboardButtons.add(btn);
            layoutKeyboard.addView(btn);
        }
        refreshKeyboardButtons();
    }

    private void refreshKeyboardButtons() {
        for (Button btn : keyboardButtons) {
            int idx = btn.getText().charAt(0) - 'A';
            boolean available = idx >= 0 && idx < 26 && keyRemaining[idx] > 0;
            btn.setEnabled(available);
            btn.setAlpha(available ? 1f : 0.4f);
            btn.setBackgroundColor(available ? Color.parseColor("#FF9800") : Color.parseColor("#BDBDBD"));
        }
    }

    private void addLetterToGuess(char c) {
        int idx = c - 'A';
        if (idx < 0 || idx >= 26 || keyRemaining[idx] <= 0) return;
        for (int i = 0; i < userGuess.length; i++) {
            if (userGuess[i] == ' ') {
                userGuess[i] = c;
                keyRemaining[idx]--;
                refreshKeyboardButtons();
                renderAnswerSlots();
                boolean full = true;
                for (char g : userGuess) { if (g == ' ') { full = false; break; } }
                if (full) checkAnswer();
                return;
            }
        }
    }

    private void removeLetterFromGuess(int index) {
        if (tvFeedback.getText().toString().startsWith("Correct!")) return;
        if (index < 0 || index >= userGuess.length || userGuess[index] == ' ') return;
        char c = userGuess[index];
        userGuess[index] = ' ';
        keyRemaining[c - 'A']++;
        refreshKeyboardButtons();
        renderAnswerSlots();
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
            recognizerIntent.putExtra(RecognizerIntent.EXTRA_PROMPT, "Say a letter");
            speechRecognizer.setRecognitionListener(new RecognitionListener() {
                @Override public void onReadyForSpeech(Bundle params) { tvFeedback.setText("Listening..."); }
                @Override public void onResults(Bundle results) {
                    ArrayList<String> matches = results.getStringArrayList(SpeechRecognizer.RESULTS_RECOGNITION);
                    int pos = -1;
                    for (int i = 0; i < userGuess.length; i++) {
                        if (userGuess[i] == ' ') { pos = i; break; }
                    }
                    char expected = (pos >= 0 && pos < currentTargetWord.length()) ? currentTargetWord.charAt(pos) : '\0';
                    boolean filled = false;
                    if (matches != null && expected != '\0') {
                        for (String cand : matches) {
                            if (cand == null) continue;
                            String letters = cand.toUpperCase().replaceAll("[^A-Z]", "");
                            if (letters.isEmpty()) continue;
                            if (letters.equals("EYE")) letters = "I";
                            if (letters.charAt(0) == expected || letters.charAt(letters.length() - 1) == expected) {
                                addLetterToGuess(expected);
                                filled = userGuess[pos] == expected;
                                break;
                            }
                        }
                    }
                    if (filled) {
                        if ("Listening...".equals(tvFeedback.getText().toString())) tvFeedback.setText("");
                    } else if (expected != '\0') {
                        tvFeedback.setText("Try again!");
                        tvFeedback.setTextColor(Color.RED);
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
