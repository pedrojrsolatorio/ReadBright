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
import android.widget.EditText;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.TextView;
import android.widget.Toast;

import androidx.appcompat.app.AppCompatActivity;
import androidx.core.app.ActivityCompat;
import androidx.core.content.ContextCompat;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Locale;

public class AssessmentActivity extends AppCompatActivity {

    private static final int REQUEST_RECORD_AUDIO_PERMISSION = 200;

    private TextView tvQuestion, tvQuestionCount, tvFeedback, tvReadingStatus, tvEmojiFallback;
    private ImageView ivQuestionImage;
    private LinearLayout layoutReading, layoutSpelling;
    private Button btnSubmit;
    private EditText etSpellingAnswer;

    private List<Question> questionList;
    private int currentQuestionIndex = 0;
    private int score = 0;
    private String username = "";
    private String currentGrade = "";
    private DatabaseHelper dbHelper;

    private SpeechRecognizer speechRecognizer;
    private Intent recognizerIntent;
    private TextToSpeech textToSpeech;
    private String lastSpokenText = "";

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_assessment);

        dbHelper = new DatabaseHelper(this);

        tvQuestion = findViewById(R.id.tvQuestion);
        tvQuestionCount = findViewById(R.id.tvQuestionCount);
        TextView tvGradeInfo = findViewById(R.id.tvGradeInfo);
        tvFeedback = findViewById(R.id.tvFeedback);
        tvReadingStatus = findViewById(R.id.tvReadingStatus);
        ivQuestionImage = findViewById(R.id.ivQuestionImage);
        tvEmojiFallback = findViewById(R.id.tvEmojiFallback);
        layoutReading = findViewById(R.id.layoutReading);
        layoutSpelling = findViewById(R.id.layoutSpelling);
        Button btnMic = findViewById(R.id.btnMic);
        Button btnListen = findViewById(R.id.btnListen);
        btnSubmit = findViewById(R.id.btnSubmit);
        Button btnBack = findViewById(R.id.btnBack);
        etSpellingAnswer = findViewById(R.id.etSpellingAnswer);

        SharedPreferences preferences = getSharedPreferences("UserDatabase", MODE_PRIVATE);
        username = preferences.getString("current_user", "");
        
        // Always get the latest grade from DB to be accurate
        Cursor cursor = dbHelper.getUser(username);
        if (cursor != null && cursor.moveToFirst()) {
            currentGrade = cursor.getString(cursor.getColumnIndexOrThrow(DatabaseHelper.COL_GRADE));
            cursor.close();
        } else {
            currentGrade = preferences.getString(username + "_grade", getString(R.string.default_grade));
        }
        
        if (tvGradeInfo != null) {
            tvGradeInfo.setText(getString(R.string.assessment_title, currentGrade));
        }

        setupQuestions(currentGrade);
        
        initializeSpeechRecognizer();
        textToSpeech = new TextToSpeech(this, status -> {
            if (status == TextToSpeech.SUCCESS) applyGlobalVoiceSettings();
        });

        displayQuestion();

        btnMic.setOnClickListener(v -> {
            if (ContextCompat.checkSelfPermission(this, Manifest.permission.RECORD_AUDIO) != PackageManager.PERMISSION_GRANTED) {
                ActivityCompat.requestPermissions(this, new String[]{Manifest.permission.RECORD_AUDIO}, REQUEST_RECORD_AUDIO_PERMISSION);
            } else {
                startListening();
            }
        });

        btnListen.setOnClickListener(v -> speakText(questionList.get(currentQuestionIndex).getCorrectAnswer()));
        btnSubmit.setOnClickListener(v -> checkAnswer());
        btnBack.setOnClickListener(v -> finish());
    }

    private void setupQuestions(String grade) {
        int typesRes, instRes, contRes;

        if (grade.contains("6")) {
            typesRes = R.array.assess_g6_types;
            instRes = R.array.assess_g6_instructions;
            contRes = R.array.assess_g6_content;
        } else if (grade.contains("5")) {
            typesRes = R.array.assess_g5_types;
            instRes = R.array.assess_g5_instructions;
            contRes = R.array.assess_g5_content;
        } else if (grade.contains("4")) {
            typesRes = R.array.assess_g4_types;
            instRes = R.array.assess_g4_instructions;
            contRes = R.array.assess_g4_content;
        } else if (grade.contains("3")) {
            typesRes = R.array.assess_g3_types;
            instRes = R.array.assess_g3_instructions;
            contRes = R.array.assess_g3_content;
        } else if (grade.contains("2")) {
            typesRes = R.array.assess_g2_types;
            instRes = R.array.assess_g2_instructions;
            contRes = R.array.assess_g2_content;
        } else {
            typesRes = R.array.assess_g1_types;
            instRes = R.array.assess_g1_instructions;
            contRes = R.array.assess_g1_content;
        }

        String[] types = getResources().getStringArray(typesRes);
        String[] insts = getResources().getStringArray(instRes);
        String[] conts = getResources().getStringArray(contRes);

        questionList = new ArrayList<>();
        for (int i = 0; i < types.length; i++) {
            int type = types[i].equals("READING") ? Question.TYPE_READING : Question.TYPE_SPELLING;
            questionList.add(new Question(type, insts[i], conts[i], conts[i]));
        }
        Collections.shuffle(questionList);
    }

    private void displayQuestion() {
        if (currentQuestionIndex >= questionList.size()) return;
        Question q = questionList.get(currentQuestionIndex);
        tvQuestionCount.setText(getString(R.string.question_count_label, currentQuestionIndex + 1, questionList.size()));
        TextView tvInstruction = findViewById(R.id.tvInstruction);
        if (tvInstruction != null) tvInstruction.setText(q.getInstruction());
        
        tvFeedback.setText("");
        lastSpokenText = "";
        layoutReading.setVisibility(View.GONE);
        layoutSpelling.setVisibility(View.GONE);
        
        updateQuestionImage(q.getCorrectAnswer());

        if (q.getType() == Question.TYPE_READING) {
            tvQuestion.setVisibility(View.VISIBLE);
            tvQuestion.setText(q.getQuestionText());
            layoutReading.setVisibility(View.VISIBLE);
            tvReadingStatus.setText(R.string.tap_mic_read);
            tvReadingStatus.setTextColor(Color.GRAY);
        } else {
            tvQuestion.setVisibility(View.GONE);
            layoutSpelling.setVisibility(View.VISIBLE);
            etSpellingAnswer.setText("");
            findViewById(R.id.tvInstruction).postDelayed(() -> speakText(q.getCorrectAnswer()), 500);
        }
    }

    private void updateQuestionImage(String text) {
        if (ivQuestionImage == null) return;
        String cleanName = text.toLowerCase().replaceAll("[^a-z]", "").trim();
        int resId = getResources().getIdentifier(cleanName + "_image", "drawable", getPackageName());
        if (resId == 0) resId = getResources().getIdentifier(cleanName + "_real", "drawable", getPackageName());
        if (resId == 0) resId = getResources().getIdentifier(cleanName, "drawable", getPackageName());
        
        if (resId == 0) {
            String[] words = text.toLowerCase().split("\\s+");
            for (String w : words) {
                String keyword = w.replaceAll("[^a-z]", "");
                resId = getResources().getIdentifier(keyword + "_image", "drawable", getPackageName());
                if (resId == 0) resId = getResources().getIdentifier(keyword + "_real", "drawable", getPackageName());
                if (resId == 0) resId = getResources().getIdentifier(keyword, "drawable", getPackageName());
                if (resId != 0) break;
            }
        }

        if (resId != 0) {
            ivQuestionImage.setImageResource(resId);
            ivQuestionImage.setVisibility(View.VISIBLE);
            tvEmojiFallback.setVisibility(View.GONE);
        } else {
            ivQuestionImage.setVisibility(View.GONE);
            String emoji = EmojiLookup.emojiFor(text);
            if (emoji != null) {
                tvEmojiFallback.setText(emoji);
                tvEmojiFallback.setVisibility(View.VISIBLE);
            } else {
                tvEmojiFallback.setVisibility(View.GONE);
            }
        }
    }

    private void checkAnswer() {
        Question q = questionList.get(currentQuestionIndex);
        boolean correct = false;
        if (q.getType() == Question.TYPE_READING) {
            if (lastSpokenText.isEmpty()) { Toast.makeText(this, R.string.please_read_first, Toast.LENGTH_SHORT).show(); return; }
            String target = q.getCorrectAnswer().toLowerCase().replaceAll("[^a-z ]", "").trim();
            String spoken = lastSpokenText.toLowerCase().replaceAll("[^a-z ]", "").trim();
            
            // Lenient matching for better UX for children
            if (spoken.equals(target) || spoken.contains(target) || target.contains(spoken)) {
                correct = true;
            }
        } else {
            String answer = etSpellingAnswer.getText().toString().trim().toLowerCase();
            if (answer.isEmpty()) { Toast.makeText(this, R.string.please_type_spelling, Toast.LENGTH_SHORT).show(); return; }
            if (answer.equalsIgnoreCase(q.getCorrectAnswer().toLowerCase().trim())) correct = true;
        }

        if (correct) {
            score++; 
            tvFeedback.setText(R.string.correct_feedback); 
            tvFeedback.setTextColor(Color.parseColor("#388E3C"));
            ConfettiView.show(this, textToSpeech);
        } else {
            tvFeedback.setText(R.string.incorrect_better_next_time); 
            tvFeedback.setTextColor(Color.RED);
            speakText(getString(R.string.correct_answer_is, q.getCorrectAnswer()));
        }

        btnSubmit.setEnabled(false);
        tvFeedback.postDelayed(() -> {
            currentQuestionIndex++;
            if (currentQuestionIndex < questionList.size()) { 
                btnSubmit.setEnabled(true); 
                displayQuestion(); 
            }
            else { 
                saveScoreAndGoToResults(); 
            }
        }, 2500);
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

    private void speakText(String text) {
        if (textToSpeech != null) { applyGlobalVoiceSettings(); textToSpeech.speak(text, TextToSpeech.QUEUE_FLUSH, null, null); }
    }

    private void initializeSpeechRecognizer() {
        if (SpeechRecognizer.isRecognitionAvailable(this)) {
            if (speechRecognizer != null) speechRecognizer.destroy();
            speechRecognizer = SpeechRecognizer.createSpeechRecognizer(this);
            recognizerIntent = new Intent(RecognizerIntent.ACTION_RECOGNIZE_SPEECH);
            recognizerIntent.putExtra(RecognizerIntent.EXTRA_LANGUAGE_MODEL, RecognizerIntent.LANGUAGE_MODEL_FREE_FORM);
            recognizerIntent.putExtra(RecognizerIntent.EXTRA_LANGUAGE, Locale.US);
            speechRecognizer.setRecognitionListener(new RecognitionListener() {
                @Override public void onReadyForSpeech(Bundle params) { tvReadingStatus.setText(R.string.listening_status); tvReadingStatus.setTextColor(Color.BLUE); }
                @Override public void onEndOfSpeech() { tvReadingStatus.setText(R.string.processing); }
                @Override public void onError(int error) { 
                    tvReadingStatus.setText(R.string.try_again_status); 
                    tvReadingStatus.setTextColor(Color.RED); 
                    if (speechRecognizer != null) speechRecognizer.cancel();
                }
                @Override public void onResults(Bundle results) {
                    ArrayList<String> matches = results.getStringArrayList(SpeechRecognizer.RESULTS_RECOGNITION);
                    if (matches != null && !matches.isEmpty()) {
                        lastSpokenText = matches.get(0);
                        tvReadingStatus.setText(getString(R.string.spoken_text_label, lastSpokenText));
                        tvReadingStatus.setTextColor(Color.BLACK);
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
    public void onRequestPermissionsResult(int requestCode, String[] permissions, int[] grantResults) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults);
        if (requestCode == REQUEST_RECORD_AUDIO_PERMISSION && grantResults.length > 0 && grantResults[0] == PackageManager.PERMISSION_GRANTED) {
            startListening();
        }
    }

    private void saveScoreAndGoToResults() {
        int starsEarned = score * 20;
        
        // Save stars to SQLite
        dbHelper.addStars(username, starsEarned);
        
        // Logic for Promotion (Proceed to next Grade)
        double percentage = ((double) score / questionList.size()) * 100;
        boolean promoted = false;
        boolean graduated = false;
        String nextGrade = "";

        if (percentage >= 80) { // Passing score is 80%
            if (currentGrade.equalsIgnoreCase("Grade 1")) nextGrade = "Grade 2";
            else if (currentGrade.equalsIgnoreCase("Grade 2")) nextGrade = "Grade 3";
            else if (currentGrade.equalsIgnoreCase("Grade 3")) nextGrade = "Grade 4";
            else if (currentGrade.equalsIgnoreCase("Grade 4")) nextGrade = "Grade 5";
            else if (currentGrade.equalsIgnoreCase("Grade 5")) nextGrade = "Grade 6";
            else if (currentGrade.equalsIgnoreCase("Grade 6")) graduated = true;

            if (!nextGrade.isEmpty()) {
                dbHelper.updateUserGrade(username, nextGrade);
                promoted = true;
                
                // Also update SharedPreferences so the current session knows the new grade
                SharedPreferences.Editor editor = getSharedPreferences("UserDatabase", MODE_PRIVATE).edit();
                editor.putString(username + "_grade", nextGrade);
                editor.apply();
            }
        }

        Intent intent = new Intent(this, AssessmentResultActivity.class);
        intent.putExtra("score", score);
        intent.putExtra("total", questionList.size());
        intent.putExtra("stars", starsEarned);
        intent.putExtra("promoted", promoted);
        intent.putExtra("graduated", graduated);
        intent.putExtra("next_grade", nextGrade);
        startActivity(intent);
        finish();
    }

    @Override
    protected void onDestroy() {
        if (speechRecognizer != null) speechRecognizer.destroy();
        if (textToSpeech != null) { textToSpeech.stop(); textToSpeech.shutdown(); }
        super.onDestroy();
    }
}
