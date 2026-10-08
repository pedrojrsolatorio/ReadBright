package com.readbright.app;

import android.content.SharedPreferences;
import android.database.Cursor;
import android.os.Bundle;
import android.view.View;
import android.widget.Button;
import android.widget.TextView;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.content.ContextCompat;

public class StudentProfileActivity extends AppCompatActivity {

    private TextView tvProfileName, tvProfileGrade, tvProfileStars;
    private TextView tvProfileBirthday, tvProfileGender, tvProfileAge;
    private TextView tvAvatar;
    private TextView tvStatusAlphabet, tvStatusPhonics, tvStatusWords, tvStatusSentences, tvStatusStories;
    private Button btnBackProfile;
    private DatabaseHelper dbHelper;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_student_profile);

        dbHelper = new DatabaseHelper(this);

        // Initialize Views
        tvProfileName = findViewById(R.id.tvProfileName);
        tvAvatar = findViewById(R.id.tvAvatar);
        tvProfileGrade = findViewById(R.id.tvProfileGrade);
        tvProfileStars = findViewById(R.id.tvProfileStars);
        tvProfileBirthday = findViewById(R.id.tvProfileBirthday);
        tvProfileGender = findViewById(R.id.tvProfileGender);
        tvProfileAge = findViewById(R.id.tvProfileAge);
        
        tvStatusAlphabet = findViewById(R.id.tvStatusAlphabet);
        tvStatusPhonics = findViewById(R.id.tvStatusPhonics);
        tvStatusWords = findViewById(R.id.tvStatusWords);
        tvStatusSentences = findViewById(R.id.tvStatusSentences);
        tvStatusStories = findViewById(R.id.tvStatusStories);
        btnBackProfile = findViewById(R.id.btnBackProfile);

        loadProfileData();

        btnBackProfile.setOnClickListener(v -> finish());
    }

    private void loadProfileData() {
        SharedPreferences preferences = getSharedPreferences("UserDatabase", MODE_PRIVATE);
        String username = preferences.getString("current_user", "");

        if (!username.isEmpty()) {
            String name = "Student";
            String grade = "N/A";
            String birthday = "N/A";
            String gender = "N/A";
            int age = 0;
            int stars = 0;

            tvAvatar.setText(AvatarShopActivity.getEquippedAvatar(this, username));

            // Fetch from SQLite database
            Cursor cursor = dbHelper.getUser(username);
            if (cursor != null && cursor.moveToFirst()) {
                name = cursor.getString(cursor.getColumnIndexOrThrow(DatabaseHelper.COL_NAME));
                grade = cursor.getString(cursor.getColumnIndexOrThrow(DatabaseHelper.COL_GRADE));
                stars = cursor.getInt(cursor.getColumnIndexOrThrow(DatabaseHelper.COL_STARS));
                
                int birthdayIdx = cursor.getColumnIndex(DatabaseHelper.COL_BIRTHDAY);
                int genderIdx = cursor.getColumnIndex(DatabaseHelper.COL_GENDER);
                int ageIdx = cursor.getColumnIndex(DatabaseHelper.COL_AGE);
                
                if (birthdayIdx != -1) birthday = cursor.getString(birthdayIdx);
                if (genderIdx != -1) gender = cursor.getString(genderIdx);
                if (ageIdx != -1) age = cursor.getInt(ageIdx);
                
                cursor.close();
            }

            tvProfileName.setText(name);
            tvProfileGrade.setText("Grade Level: " + grade);
            tvProfileStars.setText(String.valueOf(stars));
            
            tvProfileBirthday.setText("Birthday: " + (birthday != null ? birthday : "N/A"));
            tvProfileGender.setText("Gender: " + (gender != null ? gender : "N/A"));
            tvProfileAge.setText("Age: " + (age > 0 ? age : "N/A"));

            // Hide or show Alphabet view based on grade level
            boolean isGrade1 = grade != null && grade.equalsIgnoreCase("Grade 1");
            if (tvStatusAlphabet != null) {
                if (isGrade1) {
                    tvStatusAlphabet.setVisibility(View.VISIBLE);
                    setModuleStatus(tvStatusAlphabet, preferences.getBoolean(username + "_alphabet_completed", false), "Alphabet");
                } else {
                    tvStatusAlphabet.setVisibility(View.GONE);
                }
            }

            setModuleStatus(tvStatusPhonics, preferences.getBoolean(username + "_phonics_" + grade + "_completed", false), "Phonics");
            setModuleStatus(tvStatusWords, preferences.getBoolean(username + "_words_" + grade + "_completed", false), "Words");
            setModuleStatus(tvStatusSentences, preferences.getBoolean(username + "_sentences_" + grade + "_completed", false), "Sentences");
            setModuleStatus(tvStatusStories, preferences.getBoolean(username + "_story_" + grade + "_completed", false), "Rhyming");
        }
    }

    private void setModuleStatus(TextView textView, boolean isCompleted, String moduleName) {
        if (textView == null) return;
        if (isCompleted) {
            textView.setText(moduleName + ": Completed");
            textView.setTextColor(ContextCompat.getColor(this, android.R.color.holo_green_dark));
        } else {
            textView.setText(moduleName + ": Not Started");
            textView.setTextColor(ContextCompat.getColor(this, android.R.color.holo_red_dark));
        }
    }
}
