package com.example.casanesapps3;

import android.app.AlertDialog;
import android.app.DatePickerDialog;
import android.os.Bundle;
import android.widget.ArrayAdapter;
import android.widget.Button;
import android.widget.EditText;
import android.widget.Spinner;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;

import java.util.Calendar;
import java.util.Random;

public class StudentRegisterActivity extends AppCompatActivity {

    private EditText etName, etUsername, etPassword, etBirthday, etAge;
    private Spinner spinnerGrade, spinnerGender;
    private DatabaseHelper dbHelper;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_student_register);

        dbHelper = new DatabaseHelper(this);

        // Initialize Views
        etName = findViewById(R.id.etName);
        etBirthday = findViewById(R.id.etBirthday);
        etAge = findViewById(R.id.etAge);
        spinnerGender = findViewById(R.id.spinnerGender);
        etUsername = findViewById(R.id.etUsername);
        etPassword = findViewById(R.id.etPassword);
        spinnerGrade = findViewById(R.id.spinnerGrade);
        Button btnRegister = findViewById(R.id.btnRegister);
        Button btnBack = findViewById(R.id.btnBack);

        // Setup Gender Spinner
        String[] genders = {"Male", "Female", "Other"};
        ArrayAdapter<String> genderAdapter = new ArrayAdapter<>(this, android.R.layout.simple_spinner_item, genders);
        genderAdapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item);
        spinnerGender.setAdapter(genderAdapter);

        // Setup Grade Spinner with values from strings.xml
        String[] grades = getResources().getStringArray(R.array.grade_levels);
        ArrayAdapter<String> adapter = new ArrayAdapter<>(this, android.R.layout.simple_spinner_item, grades);
        adapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item);
        spinnerGrade.setAdapter(adapter);

        // Setup DatePicker for Birthday
        etBirthday.setOnClickListener(v -> {
            Calendar calendar = Calendar.getInstance();
            int year = calendar.get(Calendar.YEAR);
            int month = calendar.get(Calendar.MONTH);
            int day = calendar.get(Calendar.DAY_OF_MONTH);

            DatePickerDialog datePickerDialog = new DatePickerDialog(this, (view, selectedYear, selectedMonth, selectedDay) -> {
                String date = selectedYear + "-" + String.format("%02d", (selectedMonth + 1)) + "-" + String.format("%02d", selectedDay);
                etBirthday.setText(date);

                // Auto-calculate age approximately
                int currentYear = Calendar.getInstance().get(Calendar.YEAR);
                int age = currentYear - selectedYear;
                etAge.setText(String.valueOf(age));
            }, year, month, day);
            datePickerDialog.show();
        });

        if (btnBack != null) {
            btnBack.setOnClickListener(v -> finish());
        }

        btnRegister.setOnClickListener(v -> {
            String name = etName.getText().toString().trim();
            String birthday = etBirthday.getText().toString().trim();
            String ageStr = etAge.getText().toString().trim();
            String gender = spinnerGender.getSelectedItem().toString();
            String username = etUsername.getText().toString().trim();
            String password = etPassword.getText().toString().trim();
            String grade = spinnerGrade.getSelectedItem().toString();

            if (name.isEmpty() || birthday.isEmpty() || ageStr.isEmpty() || username.isEmpty() || password.isEmpty()) {
                Toast.makeText(this, "Please fill in all fields", Toast.LENGTH_SHORT).show();
                return;
            }

            int age;
            try {
                age = Integer.parseInt(ageStr);
            } catch (NumberFormatException e) {
                Toast.makeText(this, "Please enter a valid age", Toast.LENGTH_SHORT).show();
                return;
            }

            if (password.length() < 6) {
                Toast.makeText(this, "Password must be at least 6 characters", Toast.LENGTH_SHORT).show();
                return;
            }

            // Check if username already exists using SQLite
            if (dbHelper.checkUsernameExists(username)) {
                Toast.makeText(this, "Username already exists", Toast.LENGTH_SHORT).show();
                return;
            }

            // Generate Student ID
            String studentId = generateStudentId();

            // Save to SQLite
            boolean inserted = dbHelper.addUser(studentId, username, password, name, grade, "student", birthday, gender, age);

            if (inserted) {
                showSuccessDialog(studentId, name);
            } else {
                Toast.makeText(this, "Registration failed. Try again.", Toast.LENGTH_SHORT).show();
            }
        });
    }

    @NonNull
    private String generateStudentId() {
        Random random = new Random();
        int number = 1000 + random.nextInt(9000);
        return "STU" + number;
    }

    private void showSuccessDialog(String studentId, String name) {
        AlertDialog.Builder builder = new AlertDialog.Builder(this);
        builder.setTitle("Registration Successful");
        builder.setMessage("Welcome " + name + "!\nYour Student ID is: " + studentId + "\nPlease share this ID with your parent.");
        builder.setCancelable(false);
        builder.setPositiveButton("Got it", (dialog, which) -> finish());
        builder.show();
    }
}
