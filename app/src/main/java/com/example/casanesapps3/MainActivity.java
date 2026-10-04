package com.example.casanesapps3;

import android.content.Intent;
import android.os.Bundle;
import android.widget.Button;
import androidx.appcompat.app.AppCompatActivity;

public class MainActivity extends AppCompatActivity {

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        Button btnStudent = findViewById(R.id.btnStudent);
        Button btnParent = findViewById(R.id.btnParent);

        btnStudent.setOnClickListener(v -> {
            Intent intent = new Intent(MainActivity.this, StudentLoginActivity.class);
            startActivity(intent);
        });

        btnParent.setOnClickListener(v -> {
            Intent intent = new Intent(MainActivity.this, ParentLoginActivity.class);
            startActivity(intent);
        });
    }
}
