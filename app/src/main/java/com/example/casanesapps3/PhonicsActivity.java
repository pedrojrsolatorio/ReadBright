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
import android.widget.ImageView;
import android.widget.TextView;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.app.ActivityCompat;
import androidx.core.content.ContextCompat;

import com.bumptech.glide.Glide;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;

public class PhonicsActivity extends AppCompatActivity {

    private static final int REQUEST_RECORD_AUDIO_PERMISSION = 200;

    private static final Map<String, String> WORD_EMOJIS = new HashMap<>();
    static {
        WORD_EMOJIS.put("apple", "\uD83C\uDF4E");
        WORD_EMOJIS.put("ball", "\u26BD");
        WORD_EMOJIS.put("cat", "\uD83D\uDC08");
        WORD_EMOJIS.put("dog", "\uD83D\uDC36");
        WORD_EMOJIS.put("egg", "\uD83E\uDD5A");
        WORD_EMOJIS.put("fan", "\uD83E\uDEAD");
        WORD_EMOJIS.put("map", "\uD83D\uDDFA\uFE0F");
        WORD_EMOJIS.put("net", "\uD83E\uDD45");
        WORD_EMOJIS.put("pen", "\uD83D\uDD8A\uFE0F");
        WORD_EMOJIS.put("tin", "\uD83E\uDD6B");
        WORD_EMOJIS.put("ant", "\uD83D\uDC1C");
        WORD_EMOJIS.put("bat", "\uD83E\uDD87");
        WORD_EMOJIS.put("cap", "\uD83E\uDDE2");
        WORD_EMOJIS.put("dad", "\uD83D\uDC68");
        WORD_EMOJIS.put("hen", "\uD83D\uDC14");
        WORD_EMOJIS.put("igloo", "\uD83D\uDED6");
        WORD_EMOJIS.put("octopus", "\uD83D\uDC19");
        WORD_EMOJIS.put("pig", "\uD83D\uDC37");
        WORD_EMOJIS.put("sun", "\u2600\uFE0F");
        WORD_EMOJIS.put("box", "\uD83D\uDCE6");
        WORD_EMOJIS.put("bed", "\uD83D\uDECF\uFE0F");
        WORD_EMOJIS.put("cup", "\uD83E\uDD64");
        WORD_EMOJIS.put("hat", "\uD83C\uDFA9");
        WORD_EMOJIS.put("leg", "\uD83E\uDDB5");
        WORD_EMOJIS.put("rat", "\uD83D\uDC00");
        WORD_EMOJIS.put("frog", "\uD83D\uDC38");
        WORD_EMOJIS.put("milk", "\uD83E\uDD5B");
        WORD_EMOJIS.put("nest", "\uD83E\uDEBA");
        WORD_EMOJIS.put("star", "\u2B50");
        WORD_EMOJIS.put("tree", "\uD83C\uDF33");
        WORD_EMOJIS.put("bread", "\uD83C\uDF5E");
        WORD_EMOJIS.put("chair", "\uD83E\uDE91");
        WORD_EMOJIS.put("clock", "\uD83D\uDD50");
        WORD_EMOJIS.put("ship", "\uD83D\uDEA2");
        WORD_EMOJIS.put("thumb", "\uD83D\uDC4D");
        WORD_EMOJIS.put("blue", "\uD83D\uDD35");
        WORD_EMOJIS.put("green", "\uD83D\uDFE2");
        WORD_EMOJIS.put("train", "\uD83D\uDE86");
        WORD_EMOJIS.put("smile", "\uD83D\uDE0A");
        WORD_EMOJIS.put("cloud", "\u2601\uFE0F");
        WORD_EMOJIS.put("beach", "\uD83C\uDFD6\uFE0F");
        WORD_EMOJIS.put("fruit", "\uD83C\uDF49");
        WORD_EMOJIS.put("grape", "\uD83C\uDF47");
        WORD_EMOJIS.put("plane", "\u2708\uFE0F");
        WORD_EMOJIS.put("truck", "\uD83D\uDE9A");
        WORD_EMOJIS.put("umbrella", "\u2602\uFE0F");
        WORD_EMOJIS.put("whale", "\uD83D\uDC33");
        WORD_EMOJIS.put("fish", "\uD83D\uDC1F");
        WORD_EMOJIS.put("grass", "\uD83C\uDF3F");
        WORD_EMOJIS.put("stone", "\uD83E\uDEA8");
        WORD_EMOJIS.put("water", "\uD83D\uDCA7");
        WORD_EMOJIS.put("light", "\uD83D\uDCA1");
        WORD_EMOJIS.put("earth", "\uD83C\uDF0D");
        WORD_EMOJIS.put("flower", "\uD83C\uDF38");
        WORD_EMOJIS.put("garden", "\uD83E\uDEB4");
        WORD_EMOJIS.put("river", "\uD83C\uDF0A");
        WORD_EMOJIS.put("school", "\uD83C\uDFEB");
        WORD_EMOJIS.put("winter", "\u2744\uFE0F");
        WORD_EMOJIS.put("pencil", "\u270F\uFE0F");
        WORD_EMOJIS.put("marker", "\uD83D\uDD8D\uFE0F");
        WORD_EMOJIS.put("paper", "\uD83D\uDCC4");
        WORD_EMOJIS.put("crayon", "\uD83D\uDD8D\uFE0F");
        WORD_EMOJIS.put("eraser", "\uD83E\uDDFD");
        WORD_EMOJIS.put("window", "\uD83E\uDE9F");
        WORD_EMOJIS.put("mirror", "\uD83E\uDE9E");
        WORD_EMOJIS.put("bottle", "\uD83C\uDF7E");
        WORD_EMOJIS.put("pocket", "\uD83D\uDC56");
        WORD_EMOJIS.put("bridge", "\uD83C\uDF09");
        WORD_EMOJIS.put("animal", "\uD83E\uDD81");
        WORD_EMOJIS.put("banana", "\uD83C\uDF4C");
        WORD_EMOJIS.put("basket", "\uD83E\uDDFA");
        WORD_EMOJIS.put("butter", "\uD83E\uDDC8");
        WORD_EMOJIS.put("coffee", "\u2615");
        WORD_EMOJIS.put("laptop", "\uD83D\uDCBB");
        WORD_EMOJIS.put("planet", "\uD83E\uDE90");
        WORD_EMOJIS.put("science", "\uD83D\uDD2C");
        WORD_EMOJIS.put("history", "\uD83D\uDCDC");
        WORD_EMOJIS.put("future", "\uD83D\uDD2E");
        WORD_EMOJIS.put("system", "\u2699\uFE0F");
        WORD_EMOJIS.put("energy", "\u26A1");
        WORD_EMOJIS.put("nature", "\uD83C\uDF3F");
        WORD_EMOJIS.put("ocean", "\uD83C\uDF0A");
        WORD_EMOJIS.put("theory", "\uD83D\uDCD6");
        WORD_EMOJIS.put("library", "\uD83D\uDCDA");
        WORD_EMOJIS.put("message", "\uD83D\uDCAC");
        WORD_EMOJIS.put("network", "\uD83C\uDF10");
        WORD_EMOJIS.put("quality", "\uD83D\uDC8E");
        WORD_EMOJIS.put("village", "\uD83C\uDFD8\uFE0F");
    }

    private TextView tvWord, tvFeedback;
    private ImageView ivWordImage;
    private TextView tvEmojiFallback;
    private Button btnSpeak, btnMic, btnPrevious, btnSoundOut, btnNext, btnStartOver, btnBack;
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
        ivWordImage = findViewById(R.id.ivWordImage);
        tvEmojiFallback = findViewById(R.id.tvEmojiFallback);
        btnSpeak = findViewById(R.id.btnSpeak);
        btnMic = findViewById(R.id.btnMic);
        btnPrevious = findViewById(R.id.btnPrevious);
        btnSoundOut = findViewById(R.id.btnSoundOut);
        btnNext = findViewById(R.id.btnNext);
        btnStartOver = findViewById(R.id.btnStartOver);
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
            if (ContextCompat.checkSelfPermission(this, Manifest.permission.RECORD_AUDIO) != PackageManager.PERMISSION_GRANTED) {
                ActivityCompat.requestPermissions(this, new String[]{Manifest.permission.RECORD_AUDIO}, REQUEST_RECORD_AUDIO_PERMISSION);
            } else {
                startListening();
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

        btnPrevious.setOnClickListener(v -> {
            if (items != null && index > 0) {
                index--;
                updateUI();
            }
        });

        btnSoundOut.setOnClickListener(v -> {
            if (items != null && index < items.length) {
                speakSoundOut(items[index]);
            }
        });

        btnBack.setOnClickListener(v -> finish());

        btnStartOver.setOnClickListener(v -> {
            learningEngine.clearProgress();
            setupPhonicsList();
        });
        
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
            btnPrevious.setEnabled(false);
            btnNext.setEnabled(false);
            if (btnSpeak != null) btnSpeak.setEnabled(false);
            if (btnMic != null) btnMic.setEnabled(false);
            if (btnSoundOut != null) btnSoundOut.setEnabled(false);
            if (btnStartOver != null) btnStartOver.setVisibility(View.VISIBLE);
            return;
        }

        if (btnSpeak != null) btnSpeak.setEnabled(true);
        if (btnMic != null) btnMic.setEnabled(true);
        if (btnSoundOut != null) btnSoundOut.setEnabled(true);
        if (btnStartOver != null) btnStartOver.setVisibility(View.GONE);
        updateUI();
    }

    private void updateUI() {
        btnPrevious.setEnabled(items != null && index > 0);
        btnNext.setEnabled(items != null && items.length > 0);

        if (items == null || items.length == 0 || index >= items.length) return;
        tvWord.setText(items[index]);
        if (tvFeedback != null) tvFeedback.setText("");
        
        updateWordImage(items[index]);
        
        // Track seen status immediately
        learningEngine.markAsSeen(items[index]);
    }

    // Sound out the word letter by letter, then say the whole word.
    private void speakSoundOut(String word) {
        if (word == null || word.trim().length() == 0 || tts == null) return;
        String clean = word.trim().toLowerCase(Locale.US);
        tts.stop();

        boolean first = true;
        for (int i = 0; i < clean.length(); i++) {
            char c = clean.charAt(i);
            if (c == ' ') continue;
            tts.speak(String.valueOf(c), first ? TextToSpeech.QUEUE_FLUSH : TextToSpeech.QUEUE_ADD, null, null);
            first = false;
        }
        tts.speak(word.trim(), first ? TextToSpeech.QUEUE_FLUSH : TextToSpeech.QUEUE_ADD, null, null);
    }

    private void updateWordImage(String text) {
        if (ivWordImage == null || text == null) return;
        String cleanName = text.toLowerCase().replaceAll("[^a-z]", "").trim();
        int resId = getResources().getIdentifier(cleanName + "_image", "drawable", getPackageName());
        if (resId == 0) resId = getResources().getIdentifier(cleanName + "_real", "drawable", getPackageName());
        if (resId == 0) resId = getResources().getIdentifier(cleanName, "drawable", getPackageName());

        if (resId == 0) {
            String[] words = text.toLowerCase().split("\\s+");
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
            String emoji = emojiFor(text);
            if (emoji != null) {
                tvEmojiFallback.setText(emoji);
                tvEmojiFallback.setVisibility(View.VISIBLE);
            } else {
                tvEmojiFallback.setVisibility(View.GONE);
            }
        }
    }

    private String emojiFor(String text) {
        if (text == null) return null;
        String key = text.trim().toLowerCase(Locale.US);
        String emoji = WORD_EMOJIS.get(key);
        if (emoji != null) return emoji;
        int space = key.indexOf(' ');
        if (space > 0) return WORD_EMOJIS.get(key.substring(0, space));
        return null;
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
        if (items == null || items.length == 0) return;
        String target = items[index].toLowerCase().trim();
        String spoken = spokenText.toLowerCase().trim();
        
        if (spoken.contains(target) || target.contains(spoken)) {
            tvFeedback.setText("Excellent!");
            tvFeedback.setTextColor(Color.parseColor("#2E7D32"));
            
            // MASTERED: Save permanently to history
            learningEngine.markCompleted(items[index]);

            ConfettiView.show(this, tts);
            
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
