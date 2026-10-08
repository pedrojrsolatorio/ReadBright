package com.readbright.app;

import android.app.AlertDialog;
import android.content.Intent;
import android.content.SharedPreferences;
import android.database.Cursor;
import android.database.sqlite.SQLiteDatabase;
import android.os.Bundle;
import android.widget.Button;
import android.widget.EditText;
import android.widget.LinearLayout;
import android.widget.TextView;
import android.widget.Toast;

import androidx.appcompat.app.AppCompatActivity;

public class StudentLoginActivity extends AppCompatActivity {

    EditText etUsername, etPassword;
    Button btnLogin;
    TextView txtRegister, txtForgotPassword;
    DatabaseHelper dbHelper;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_student_login);

        dbHelper = DatabaseHelper.getInstance(this);

        etUsername = findViewById(R.id.etUsername);
        etPassword = findViewById(R.id.etPassword);
        btnLogin = findViewById(R.id.btnLogin);
        txtRegister = findViewById(R.id.txtRegister);
        txtForgotPassword = findViewById(R.id.txtForgotPassword);

        btnLogin.setOnClickListener(view -> {
            String username = etUsername.getText().toString().trim();
            String password = etPassword.getText().toString().trim();

            if (username.isEmpty() || password.isEmpty()) {
                Toast.makeText(this, R.string.fill_all_fields, Toast.LENGTH_SHORT).show();
                return;
            }

            Cursor cursor = dbHelper.checkUser(username, password);

            if (cursor != null && cursor.moveToFirst()) {
                String role = cursor.getString(cursor.getColumnIndexOrThrow(DatabaseHelper.COL_ROLE));
                
                if ("student".equals(role)) {
                    String name = cursor.getString(cursor.getColumnIndexOrThrow(DatabaseHelper.COL_NAME));
                    String grade = cursor.getString(cursor.getColumnIndexOrThrow(DatabaseHelper.COL_GRADE));

                    // SAVE SESSION DATA - IMPORTANT for non-cycling words
                    SharedPreferences preferences = getSharedPreferences("UserDatabase", MODE_PRIVATE);
                    SharedPreferences.Editor editor = preferences.edit();
                    editor.putString("current_user", username);
                    editor.putString(username + "_grade", grade); // Save the grade to session
                    editor.apply();

                    Toast.makeText(this, getString(R.string.login_welcome, name, grade), Toast.LENGTH_SHORT).show();

                    Intent intent = new Intent(this, StudentDashboardActivity.class);
                    startActivity(intent);
                    finish();
                    
                } else {
                    Toast.makeText(this, R.string.not_student_account, Toast.LENGTH_SHORT).show();
                }
                cursor.close();
            } else {
                Toast.makeText(this, R.string.invalid_credentials, Toast.LENGTH_SHORT).show();
            }
        });

        txtRegister.setOnClickListener(view -> {
            Intent intent = new Intent(this, StudentRegisterActivity.class);
            startActivity(intent);
        });

        txtForgotPassword.setOnClickListener(view -> showForgotPasswordDialog());
    }

    private void showForgotPasswordDialog() {
        AlertDialog.Builder builder = new AlertDialog.Builder(this);
        builder.setTitle(R.string.password_recovered_title);

        LinearLayout layout = new LinearLayout(this);
        layout.setOrientation(LinearLayout.VERTICAL);
        layout.setPadding(50, 40, 50, 10);

        final EditText etForgotUsername = new EditText(this);
        etForgotUsername.setHint(R.string.enter_username);
        layout.addView(etForgotUsername);

        final EditText etForgotName = new EditText(this);
        etForgotName.setHint(R.string.profile_student_name);
        layout.addView(etForgotName);

        builder.setView(layout);

        builder.setPositiveButton(R.string.verify_identity, (dialog, which) -> {
            String usernameInput = etForgotUsername.getText().toString().trim();
            String nameInput = etForgotName.getText().toString().trim();

            if (usernameInput.isEmpty() || nameInput.isEmpty()) return;

            SQLiteDatabase db = dbHelper.getReadableDatabase();
            Cursor cursor = db.query(DatabaseHelper.TABLE_USERS, 
                    new String[]{DatabaseHelper.COL_PASSWORD, DatabaseHelper.COL_NAME}, 
                    DatabaseHelper.COL_USERNAME + "=?", 
                    new String[]{usernameInput}, null, null, null);

            if (cursor != null && cursor.moveToFirst()) {
                String savedName = cursor.getString(cursor.getColumnIndexOrThrow(DatabaseHelper.COL_NAME));
                String savedPassword = cursor.getString(cursor.getColumnIndexOrThrow(DatabaseHelper.COL_PASSWORD));

                if (savedName.equalsIgnoreCase(nameInput)) {
                    AlertDialog.Builder innerBuilder = new AlertDialog.Builder(this);
                    innerBuilder.setTitle(R.string.password_recovered_title);
                    innerBuilder.setMessage(getString(R.string.password_recovered_msg, savedName, savedPassword));
                    innerBuilder.setPositiveButton(android.R.string.ok, null);
                    innerBuilder.show();
                } else {
                    Toast.makeText(this, R.string.verification_failed, Toast.LENGTH_SHORT).show();
                }
                cursor.close();
            }
        });

        builder.setNegativeButton(R.string.cancel, null);
        builder.show();
    }
}
