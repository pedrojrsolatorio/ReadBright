package com.example.casanesapps3;

import android.app.AlertDialog;
import android.content.Intent;
import android.content.SharedPreferences;
import android.os.Bundle;
import android.widget.Button;
import android.widget.EditText;
import android.widget.LinearLayout;
import android.widget.TextView;
import android.widget.Toast;

import androidx.appcompat.app.AppCompatActivity;

public class ParentLoginActivity extends AppCompatActivity {

    EditText etUsername, etPassword;
    Button btnLogin;
    TextView txtRegister, txtForgotPassword;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_parent_login);

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

            SharedPreferences preferences = getSharedPreferences("UserDatabase", MODE_PRIVATE);
            String savedPassword = preferences.getString(username + "_password", "");
            String role = preferences.getString(username + "_role", "");

            if (!savedPassword.isEmpty() && savedPassword.equals(password)) {
                if ("parent".equals(role)) {
                    // Save session
                    SharedPreferences.Editor editor = preferences.edit();
                    editor.putString("current_parent", username);
                    editor.apply();

                    Toast.makeText(this, R.string.parent_login_success, Toast.LENGTH_SHORT).show();
                    Intent intent = new Intent(this, ParentDashboardActivity.class);
                    startActivity(intent);
                    finish();
                } else {
                    Toast.makeText(this, R.string.not_parent_account, Toast.LENGTH_SHORT).show();
                }
            } else {
                Toast.makeText(this, R.string.invalid_credentials, Toast.LENGTH_SHORT).show();
            }
        });

        txtRegister.setOnClickListener(view -> {
            Intent intent = new Intent(this, ParentRegisterActivity.class);
            startActivity(intent);
        });

        txtForgotPassword.setOnClickListener(view -> showForgotPasswordDialog());
    }

    private void showForgotPasswordDialog() {
        AlertDialog.Builder builder = new AlertDialog.Builder(this);
        builder.setTitle(R.string.forgot_password_title);

        LinearLayout layout = new LinearLayout(this);
        layout.setOrientation(LinearLayout.VERTICAL);
        layout.setPadding(50, 40, 50, 10);

        final EditText etForgotUsername = new EditText(this);
        etForgotUsername.setHint(R.string.enter_username);
        layout.addView(etForgotUsername);

        final EditText etForgotName = new EditText(this);
        etForgotName.setHint(R.string.enter_registered_name);
        layout.addView(etForgotName);

        builder.setView(layout);

        builder.setPositiveButton(R.string.verify_identity, (dialog, which) -> {
            String usernameInput = etForgotUsername.getText().toString().trim();
            String nameInput = etForgotName.getText().toString().trim();

            if (usernameInput.isEmpty() || nameInput.isEmpty()) {
                Toast.makeText(this, R.string.fill_all_fields, Toast.LENGTH_SHORT).show();
                return;
            }

            SharedPreferences preferences = getSharedPreferences("UserDatabase", MODE_PRIVATE);
            String savedName = preferences.getString(usernameInput + "_name", "");
            String savedPassword = preferences.getString(usernameInput + "_password", "");
            String role = preferences.getString(usernameInput + "_role", "");

            if ("parent".equals(role) && !savedName.isEmpty() && savedName.equalsIgnoreCase(nameInput)) {
                AlertDialog.Builder innerBuilder = new AlertDialog.Builder(this);
                innerBuilder.setTitle(R.string.password_recovered_title);
                innerBuilder.setMessage(getString(R.string.password_recovered_msg, savedName, savedPassword));
                innerBuilder.setPositiveButton(android.R.string.ok, null);
                innerBuilder.show();
            } else {
                Toast.makeText(this, R.string.verification_failed, Toast.LENGTH_SHORT).show();
            }
        });

        builder.setNegativeButton(R.string.cancel, null);
        builder.show();
    }
}
