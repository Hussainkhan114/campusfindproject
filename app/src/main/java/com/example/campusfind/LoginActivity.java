package com.example.campusfind;

import android.content.Intent;
import android.content.SharedPreferences;
import android.os.Bundle;
import android.widget.Button;
import android.widget.EditText;
import android.widget.TextView;
import android.widget.Toast;
import androidx.appcompat.app.AppCompatActivity;

public class LoginActivity extends AppCompatActivity {

    EditText etEmail, etPassword;
    Button btnLogin;
    TextView tvGoRegister;
    DBHelper db;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        // R2 & F9: User stays logged in until Logout
        SharedPreferences sp = getSharedPreferences("CampusSession", MODE_PRIVATE);
        if (sp.contains("email")) {
            startActivity(new Intent(this, MainActivity.class));
            finish();
            return;
        }

        setContentView(R.layout.activity_login);
        db = new DBHelper(this);

        etEmail = findViewById(R.id.etLoginEmail);
        etPassword = findViewById(R.id.etLoginPassword);
        btnLogin = findViewById(R.id.btnLogin);
        tvGoRegister = findViewById(R.id.tvGoRegister);

        btnLogin.setOnClickListener(v -> {
            String email = etEmail.getText().toString().trim();
            String pass = etPassword.getText().toString().trim();

            String userName = db.loginUser(email, pass);
            if (userName != null) {
                sp.edit().putString("email", email).putString("name", userName).apply();
                startActivity(new Intent(this, MainActivity.class));
                finish();
            } else {
                // R2 exact message
                Toast.makeText(this, "Invalid email or password", Toast.LENGTH_SHORT).show();
            }
        });

        tvGoRegister.setOnClickListener(v -> startActivity(new Intent(this, RegisterActivity.class)));
    }
}
