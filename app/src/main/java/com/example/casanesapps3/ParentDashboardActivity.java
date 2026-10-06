package com.example.casanesapps3;

import android.content.Intent;
import android.content.SharedPreferences;
import android.database.Cursor;
import android.os.Bundle;
import android.view.View;
import android.widget.Button;
import android.widget.LinearLayout;
import android.widget.TextView;
import android.widget.Toast;

import androidx.appcompat.app.AppCompatActivity;
import androidx.core.content.ContextCompat;

public class ParentDashboardActivity extends AppCompatActivity {

    private TextView tvWelcomeParent, tvChildName, tvChildGrade, tvChildScore, tvChildSpellingScore, tvChildStars;
    private TextView tvStatusAlphabet, tvStatusPhonics, tvStatusWords, tvStatusSentences, tvStatusStories;
    private Button btnRefresh, btnLogout;
    private String childUsername;
    private DatabaseHelper dbHelper;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_parent_dashboard);

        dbHelper = new DatabaseHelper(this);

        tvWelcomeParent = findViewById(R.id.tvWelcomeParent);
        tvChildName = findViewById(R.id.tvChildName);
        tvChildGrade = findViewById(R.id.tvChildGrade);
        tvChildScore = findViewById(R.id.tvChildScore);
        tvChildSpellingScore = findViewById(R.id.tvChildSpellingScore);
        tvChildStars = findViewById(R.id.tvChildStars);
        
        tvStatusAlphabet = findViewById(R.id.tvStatusAlphabet);
        tvStatusPhonics = findViewById(R.id.tvStatusPhonics);
        tvStatusWords = findViewById(R.id.tvStatusWords);
        tvStatusSentences = findViewById(R.id.tvStatusSentences);
        tvStatusStories = findViewById(R.id.tvStatusStories);
        
        btnRefresh = findViewById(R.id.btnRefresh);
        btnLogout = findViewById(R.id.btnLogout);

        SharedPreferences preferences = getSharedPreferences("UserDatabase", MODE_PRIVATE);
        String parentUsername = preferences.getString("current_parent", "");
        
        if (!parentUsername.isEmpty()) {
            String parentName = preferences.getString(parentUsername + "_name", getString(R.string.parent));
            childUsername = preferences.getString(parentUsername + "_child", "");
            
            tvWelcomeParent.setText(getString(R.string.parent_welcome, parentName));
            loadChildData();
        }

        btnRefresh.setOnClickListener(v -> {
            loadChildData();
            Toast.makeText(this, R.string.progress_updated, Toast.LENGTH_SHORT).show();
        });

        btnLogout.setOnClickListener(v -> {
            SharedPreferences.Editor editor = preferences.edit();
            editor.remove("current_parent");
            editor.apply();

            Intent intent = new Intent(this, MainActivity.class);
            intent.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK | Intent.FLAG_ACTIVITY_CLEAR_TASK);
            startActivity(intent);
            finish();
        });
    }

    private void loadChildData() {
        if (childUsername == null || childUsername.isEmpty()) {
            tvChildName.setText(R.string.no_child_linked);
            return;
        }

        SharedPreferences preferences = getSharedPreferences("UserDatabase", MODE_PRIVATE);
        
        // Fetch child data from SQLite
        Cursor cursor = dbHelper.getUser(childUsername);
        String name = getString(R.string.na);
        String grade = getString(R.string.na);
        int stars = 0;

        if (cursor != null && cursor.moveToFirst()) {
            name = cursor.getString(cursor.getColumnIndexOrThrow(DatabaseHelper.COL_NAME));
            grade = cursor.getString(cursor.getColumnIndexOrThrow(DatabaseHelper.COL_GRADE));
            stars = cursor.getInt(cursor.getColumnIndexOrThrow(DatabaseHelper.COL_STARS));
            cursor.close();
        }

        String lastScore = preferences.getString(childUsername + "_last_score", getString(R.string.no_assessment_yet));
        String lastSpelling = preferences.getString(childUsername + "_last_spelling_score", getString(R.string.no_assessment_yet));

        tvChildName.setText(getString(R.string.child_name_label, name));
        tvChildGrade.setText(getString(R.string.child_grade_label, grade));
        tvChildScore.setText(getString(R.string.last_assess_score_label, lastScore));
        tvChildSpellingScore.setText(getString(R.string.last_spelling_score_label, lastSpelling));
        tvChildStars.setText(getString(R.string.total_stars_label, stars));

        // Update module statuses and hide Alphabet if not Grade 1
        boolean isGrade1 = grade != null && grade.equalsIgnoreCase("Grade 1");
        
        if (tvStatusAlphabet != null) {
            if (isGrade1) {
                tvStatusAlphabet.setVisibility(View.VISIBLE);
                updateStatusText(tvStatusAlphabet, preferences.getBoolean(childUsername + "_alphabet_completed", false), "Alphabet");
            } else {
                tvStatusAlphabet.setVisibility(View.GONE);
            }
        }

        updateStatusText(tvStatusPhonics, preferences.getBoolean(childUsername + "_phonics_" + grade + "_completed", false), "Phonics");
        updateStatusText(tvStatusWords, preferences.getBoolean(childUsername + "_words_" + grade + "_completed", false), "Words");
        updateStatusText(tvStatusSentences, preferences.getBoolean(childUsername + "_sentences_" + grade + "_completed", false), "Sentences");
        updateStatusText(tvStatusStories, preferences.getBoolean(childUsername + "_story_" + grade + "_completed", false), "Rhyming");
    }

    private void updateStatusText(TextView tv, boolean isCompleted, String moduleName) {
        if (tv == null) return;
        if (isCompleted) {
            tv.setText(moduleName + ": Completed");
            tv.setTextColor(ContextCompat.getColor(this, android.R.color.holo_green_dark));
        } else {
            tv.setText(moduleName + ": Not Started");
            tv.setTextColor(ContextCompat.getColor(this, android.R.color.holo_red_dark));
        }
    }
}
