package com.example.casanesapps3;

import android.content.Context;
import android.content.SharedPreferences;
import android.graphics.Color;
import android.os.Bundle;
import android.util.TypedValue;
import android.view.Gravity;
import android.view.View;
import android.widget.Button;
import android.widget.GridLayout;
import android.widget.LinearLayout;
import android.widget.TextView;
import android.widget.Toast;

import androidx.appcompat.app.AppCompatActivity;

import java.util.LinkedHashSet;
import java.util.Set;

public class AvatarShopActivity extends AppCompatActivity {

    public static final String DEFAULT_AVATAR = "\uD83E\uDD16"; // robot

    private static final String[][] AVATARS = {
            {DEFAULT_AVATAR, "0"},
            {"\uD83D\uDC31", "100"}, // cat
            {"\uD83D\uDC36", "100"}, // dog
            {"\uD83D\uDC38", "200"}, // frog
            {"\uD83D\uDC3C", "200"}, // panda
            {"\uD83E\uDD8A", "300"}, // fox
            {"\uD83E\uDD81", "300"}, // lion
            {"\uD83D\uDC12", "400"}, // monkey
            {"\uD83E\uDD84", "400"}, // unicorn
            {"\uD83D\uDC2F", "500"}, // tiger
    };

    private TextView tvBalance;
    private GridLayout gridAvatar;
    private DatabaseHelper dbHelper;
    private SharedPreferences prefs;
    private String username;

    public static String getEquippedAvatar(Context context, String username) {
        SharedPreferences prefs = context.getSharedPreferences("UserDatabase", Context.MODE_PRIVATE);
        return prefs.getString(username + "_avatar", DEFAULT_AVATAR);
    }

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_avatar_shop);

        dbHelper = DatabaseHelper.getInstance(this);
        prefs = getSharedPreferences("UserDatabase", MODE_PRIVATE);
        username = prefs.getString("current_user", "");
        if (username.isEmpty()) {
            finish();
            return;
        }

        tvBalance = findViewById(R.id.tvBalance);
        gridAvatar = findViewById(R.id.gridAvatar);
        Button btnBack = findViewById(R.id.btnBack);
        btnBack.setOnClickListener(v -> finish());

        renderGrid();
    }

    private void renderGrid() {
        tvBalance.setText("\u2B50 Balance: " + dbHelper.getStars(username));
        gridAvatar.removeAllViews();

        Set<String> owned = getOwnedAvatars();
        String equipped = getEquippedAvatar(this, username);

        for (String[] entry : AVATARS) {
            String emoji = entry[0];
            int price = Integer.parseInt(entry[1]);
            boolean isOwned = owned.contains(emoji);
            boolean isEquipped = equipped.equals(emoji);

            LinearLayout cell = new LinearLayout(this);
            cell.setOrientation(LinearLayout.VERTICAL);
            cell.setGravity(Gravity.CENTER);
            cell.setPadding(dp(8), dp(18), dp(8), dp(14));
            cell.setBackgroundColor(Color.WHITE);

            GridLayout.LayoutParams params = new GridLayout.LayoutParams();
            params.width = 0;
            params.height = GridLayout.LayoutParams.WRAP_CONTENT;
            params.columnSpec = GridLayout.spec(GridLayout.UNDEFINED, 1f);
            params.setMargins(dp(6), dp(6), dp(6), dp(6));
            cell.setLayoutParams(params);

            TextView tvEmoji = new TextView(this);
            tvEmoji.setText(emoji);
            tvEmoji.setTextSize(TypedValue.COMPLEX_UNIT_SP, 44);
            tvEmoji.setGravity(Gravity.CENTER);
            cell.addView(tvEmoji);

            TextView tvLabel = new TextView(this);
            if (isEquipped) {
                tvLabel.setText("Equipped \u2713");
                tvLabel.setTextColor(Color.parseColor("#2E7D32"));
            } else if (isOwned) {
                tvLabel.setText("Owned");
                tvLabel.setTextColor(Color.parseColor("#1976D2"));
            } else {
                tvLabel.setText("\u2B50 " + price);
                tvLabel.setTextColor(Color.parseColor("#FF8F00"));
            }
            tvLabel.setTextSize(14);
            tvLabel.setGravity(Gravity.CENTER);
            cell.addView(tvLabel);

            cell.setOnClickListener(v -> onAvatarTap(emoji, price, isOwned, isEquipped));

            gridAvatar.addView(cell);
        }
    }

    private void onAvatarTap(String emoji, int price, boolean isOwned, boolean isEquipped) {
        if (isEquipped) return;

        if (isOwned) {
            equip(emoji);
            Toast.makeText(this, "Equipped!", Toast.LENGTH_SHORT).show();
            renderGrid();
            return;
        }

        int balance = dbHelper.getStars(username);
        if (balance < price) {
            Toast.makeText(this, "Not enough stars! You need " + (price - balance) + " more.", Toast.LENGTH_SHORT).show();
            return;
        }

        dbHelper.addStars(username, -price);
        Set<String> owned = getOwnedAvatars();
        owned.add(emoji);
        prefs.edit().putString(username + "_owned_avatars", join(owned)).apply();
        equip(emoji);
        Toast.makeText(this, "Purchased! New avatar equipped!", Toast.LENGTH_SHORT).show();
        renderGrid();
    }

    private void equip(String emoji) {
        prefs.edit().putString(username + "_avatar", emoji).apply();
    }

    private Set<String> getOwnedAvatars() {
        Set<String> owned = new LinkedHashSet<>();
        owned.add(DEFAULT_AVATAR);
        String csv = prefs.getString(username + "_owned_avatars", "");
        if (!csv.isEmpty()) {
            for (String s : csv.split(",")) {
                if (!s.trim().isEmpty()) owned.add(s.trim());
            }
        }
        return owned;
    }

    private String join(Set<String> items) {
        StringBuilder sb = new StringBuilder();
        for (String s : items) {
            if (sb.length() > 0) sb.append(",");
            sb.append(s);
        }
        return sb.toString();
    }

    private int dp(int value) {
        return (int) TypedValue.applyDimension(TypedValue.COMPLEX_UNIT_DIP, value, getResources().getDisplayMetrics());
    }
}
