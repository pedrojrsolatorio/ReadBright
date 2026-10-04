package com.example.casanesapps3;

import android.content.SharedPreferences;
import android.os.Bundle;
import android.widget.Button;
import android.widget.EditText;
import android.widget.Toast;

import androidx.appcompat.app.AppCompatActivity;

public class ParentRegisterActivity extends AppCompatActivity {

    EditText etParentName, etStudentId, etUsername, etPassword;
    Button btnRegister;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_parent_register);

        etParentName = findViewById(R.id.etParentName);
        etStudentId = findViewById(R.id.etStudentId);
        etUsername = findViewById(R.id.etUsername);
        etPassword = findViewById(R.id.etPassword);
        btnRegister = findViewById(R.id.btnRegister);

        btnRegister.setOnClickListener(v -> {
            String parentName = etParentName.getText().toString().trim();
            String studentId = etStudentId.getText().toString().trim();
            String username = etUsername.getText().toString().trim();
            String password = etPassword.getText().toString().trim();

            if (parentName.isEmpty() || studentId.isEmpty() || username.isEmpty() || password.isEmpty()) {
                Toast.makeText(this, R.string.fill_all_fields, Toast.LENGTH_SHORT).show();
                return;
            }

            SharedPreferences preferences = getSharedPreferences("UserDatabase", MODE_PRIVATE);
            
            // Look up the child username using the Student ID
            String childUsername = preferences.getString("id_map_" + studentId, "");

            if (childUsername.isEmpty()) {
                Toast.makeText(this, R.string.invalid_stu_id, Toast.LENGTH_LONG).show();
                return;
            }

            // Check if Parent username already exists
            if (preferences.contains(username + "_password")) {
                Toast.makeText(this, R.string.user_exists, Toast.LENGTH_SHORT).show();
                return;
            }

            SharedPreferences.Editor editor = preferences.edit();

            // Save parent details
            editor.putString(username + "_password", password);
            editor.putString(username + "_name", parentName);
            editor.putString(username + "_child", childUsername); // Link to student's username
            editor.putString(username + "_role", "parent");

            editor.apply();

            Toast.makeText(this, getString(R.string.parent_reg_success, childUsername), Toast.LENGTH_SHORT).show();
            finish();
        });
    }
}
