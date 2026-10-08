package com.readbright.app;

import android.content.Intent;
import android.content.SharedPreferences;
import android.database.Cursor;
import android.os.Bundle;
import android.widget.Button;
import android.widget.TextView;
import android.widget.Toast;

import androidx.appcompat.app.AppCompatActivity;

public class StudentDashboardActivity extends AppCompatActivity {

    private TextView tvWelcome, tvGrade, tvStars, tvAvatar;
    private DatabaseHelper dbHelper;
    private String username;
    private String userGrade;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_student_dashboard);

        dbHelper = DatabaseHelper.getInstance(this);

        tvWelcome = findViewById(R.id.tvWelcome);
        tvGrade = findViewById(R.id.tvGrade);
        tvStars = findViewById(R.id.tvStars);
        tvAvatar = findViewById(R.id.tvAvatar);

        SharedPreferences preferences = getSharedPreferences("UserDatabase", MODE_PRIVATE);
        username = preferences.getString("current_user", "");
        
        if (username.isEmpty()) {
            Toast.makeText(this, "Session expired. Please login again.", Toast.LENGTH_SHORT).show();
            finish();
            return;
        }

        loadUserData();

        // Buttons
        findViewById(R.id.btnAlphabet).setOnClickListener(v -> startModule(AlphabetActivity.class));
        findViewById(R.id.btnPhonics).setOnClickListener(v -> startModule(PhonicsActivity.class));
        findViewById(R.id.btnWords).setOnClickListener(v -> startModule(WordsActivity.class));
        findViewById(R.id.btnSentences).setOnClickListener(v -> startModule(SentenceActivity.class));
        findViewById(R.id.btnStories).setOnClickListener(v -> startModule(StoryActivity.class));
        findViewById(R.id.btnSpelling).setOnClickListener(v -> startModule(SpellingActivity.class));
        findViewById(R.id.btnReadingAI).setOnClickListener(v -> startModule(ReadingAIActivity.class));
        findViewById(R.id.btnAssessment).setOnClickListener(v -> startModule(AssessmentActivity.class));

        findViewById(R.id.btnAvatarShop).setOnClickListener(v -> {
            startActivity(new Intent(this, AvatarShopActivity.class));
        });

        findViewById(R.id.btnProfile).setOnClickListener(v -> {
            Intent intent = new Intent(this, StudentProfileActivity.class);
            startActivity(intent);
        });

        findViewById(R.id.btnLogout).setOnClickListener(v -> {
            SharedPreferences.Editor editor = preferences.edit();
            editor.remove("current_user");
            editor.apply();
            finish();
        });
    }

    private void loadUserData() {
        Cursor cursor = dbHelper.getUser(username);
        if (cursor != null && cursor.moveToFirst()) {
            String name = cursor.getString(cursor.getColumnIndexOrThrow(DatabaseHelper.COL_NAME));
            userGrade = cursor.getString(cursor.getColumnIndexOrThrow(DatabaseHelper.COL_GRADE));
            int stars = cursor.getInt(cursor.getColumnIndexOrThrow(DatabaseHelper.COL_STARS));
            
            tvWelcome.setText("Welcome, " + name + "!");
            tvGrade.setText("Level: " + userGrade);
            tvStars.setText("Stars: " + stars);
            tvAvatar.setText(AvatarShopActivity.getEquippedAvatar(this, username));
            
            // Save current grade to SharedPreferences to ensure engine consistency
            getSharedPreferences("UserDatabase", MODE_PRIVATE).edit()
                .putString(username + "_grade", userGrade).apply();
            
            cursor.close();
        }
    }

    private void startModule(Class<?> activityClass) {
        Intent intent = new Intent(this, activityClass);
        intent.putExtra("username", username);
        intent.putExtra("grade", userGrade);
        startActivity(intent);
    }

    @Override
    protected void onResume() {
        super.onResume();
        loadUserData(); // Refresh stars and grade when returning to dashboard
    }
}
