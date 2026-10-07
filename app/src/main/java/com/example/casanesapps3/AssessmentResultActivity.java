package com.example.casanesapps3;

import android.content.Intent;
import android.os.Bundle;
import android.view.View;
import android.widget.Button;
import android.widget.TextView;
import android.widget.Toast;

import androidx.appcompat.app.AppCompatActivity;

public class AssessmentResultActivity extends AppCompatActivity {

    private TextView tvFinalScore, tvStarsEarned, tvPromotionMsg, tvGraduationMsg;
    private Button btnDone;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_assessment_result);

        tvFinalScore = findViewById(R.id.tvFinalScore);
        tvStarsEarned = findViewById(R.id.tvStarsEarned);
        tvPromotionMsg = findViewById(R.id.tvPromotionMsg);
        tvGraduationMsg = findViewById(R.id.tvGraduationMsg);
        btnDone = findViewById(R.id.btnDone);

        // Get data from intent
        int score = getIntent().getIntExtra("score", 0);
        int total = getIntent().getIntExtra("total", 0);
        int stars = getIntent().getIntExtra("stars", 0);
        boolean promoted = getIntent().getBooleanExtra("promoted", false);
        boolean graduated = getIntent().getBooleanExtra("graduated", false);
        int bonus = getIntent().getIntExtra("bonus", 0);
        int gradBonus = getIntent().getIntExtra("grad_bonus", 0);
        String nextGrade = getIntent().getStringExtra("next_grade");

        tvFinalScore.setText(getString(R.string.score_label, score + "/" + total));
        tvStarsEarned.setText(getString(R.string.earned_stars_msg, stars + bonus));
        if (bonus > 0) {
            tvStarsEarned.append(" (+" + bonus + " bonus)");
        }

        if (promoted && nextGrade != null) {
            tvPromotionMsg.setVisibility(View.VISIBLE);
            tvPromotionMsg.setText("Congratulations! You are promoted to " + nextGrade + "!");
        } else {
            tvPromotionMsg.setVisibility(View.GONE);
        }

        if (graduated) {
            tvGraduationMsg.setVisibility(View.VISIBLE);
            if (gradBonus > 0) {
                tvGraduationMsg.setText("\uD83C\uDF93 Congratulations, Graduate! \uD83C\uDF93\nYou passed Grade 6!\n\u2B50 +" + gradBonus + " bonus!");
            }
            ConfettiView.show(this);
        } else {
            tvGraduationMsg.setVisibility(View.GONE);
        }

        btnDone.setOnClickListener(v -> {
            // Return to dashboard
            Intent intent = new Intent(this, StudentDashboardActivity.class);
            intent.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK | Intent.FLAG_ACTIVITY_CLEAR_TASK);
            startActivity(intent);
            finish();
        });
    }
}
