package com.readbright.app;

import android.content.Intent;
import android.content.SharedPreferences;
import android.os.Bundle;
import android.speech.tts.TextToSpeech;
import android.view.View;
import android.widget.Button;
import android.widget.TextView;
import android.widget.Toast;

import androidx.appcompat.app.AppCompatActivity;
import java.util.List;
import java.util.Locale;

public class ReadingActivity extends AppCompatActivity {

    private DatabaseHelper dbHelper;
    private ContinuousWordEngine wordEngine;
    private String username = "";
    private String userGrade = "";

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_reading);

        dbHelper = DatabaseHelper.getInstance(this);

        SharedPreferences preferences = getSharedPreferences("UserDatabase", MODE_PRIVATE);
        username = preferences.getString("current_user", "guest");
        userGrade = preferences.getString(username + "_grade", "Grade 1");

        TextView tvStars = findViewById(R.id.tvReadingStars);
        if (tvStars != null) {
            tvStars.setText(String.valueOf(dbHelper.getStars(username)));
        }

        // Setup the buttons in the reading menu
        findViewById(R.id.btnAITutor).setOnClickListener(v -> startModule(ReadingAIActivity.class));
        findViewById(R.id.btnAlphabet).setOnClickListener(v -> startModule(AlphabetActivity.class));
        findViewById(R.id.btnPhonics).setOnClickListener(v -> startModule(PhonicsActivity.class));
        findViewById(R.id.btnWords).setOnClickListener(v -> startModule(WordsActivity.class));
        
        Button btnSentence = findViewById(R.id.btnSentence);
        if (btnSentence != null) {
            btnSentence.setOnClickListener(v -> startModule(SentenceActivity.class));
        }

        findViewById(R.id.btnStory).setOnClickListener(v -> startModule(StoryActivity.class));
        findViewById(R.id.btnBack).setOnClickListener(v -> finish());
    }

    private void startModule(Class<?> activityClass) {
        Intent intent = new Intent(this, activityClass);
        intent.putExtra("username", username);
        intent.putExtra("grade", userGrade);
        startActivity(intent);
    }
}
