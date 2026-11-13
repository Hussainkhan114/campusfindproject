package com.example.campusfind;

import android.os.Bundle;
import android.util.Patterns;
import android.widget.Button;
import android.widget.EditText;
import android.widget.TextView;
import android.widget.Toast;
import androidx.appcompat.app.AppCompatActivity;

public class RegisterActivity extends AppCompatActivity {

    EditText etName, etEmail, etPass, etConfirm;
    Button btnRegister;
    TextView tvGoLogin;
    DBHelper db;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_register);
        db = new DBHelper(this);

        etName = findViewById(R.id.etRegName);
        etEmail = findViewById(R.id.etRegEmail);
        etPass = findViewById(R.id.etRegPass);
        etConfirm = findViewById(R.id.etRegConfirm);
        btnRegister = findViewById(R.id.btnRegister);
        tvGoLogin = findViewById(R.id.tvGoLogin);

        btnRegister.setOnClickListener(v -> {
            String name = etName.getText().toString().trim();
            String email = etEmail.getText().toString().trim();
            String pass = etPass.getText().toString();
            String confirm = etConfirm.getText().toString();

            // R1 Strict Validation
            if (!name.matches("[a-zA-Z ]{3,}")) {
                etName.setError("Letters and spaces only, at least 3 characters");
                return;
            }
            if (!Patterns.EMAIL_ADDRESS.matcher(email).matches()) {
                etEmail.setError("Enter a valid email");
                return;
            }
            if (pass.length() < 6 || !pass.matches(".*\\d.*")) {
                etPass.setError("At least 6 characters including 1 digit");
                return;
            }
            if (!pass.equals(confirm)) {
                etConfirm.setError("Passwords must match");
                return;
            }
            if (db.emailExists(email)) {
                // R1 exact message
                Toast.makeText(this, "An account with this email already exists", Toast.LENGTH_LONG).show();
                return;
            }

            if (db.registerUser(name, email, pass)) {
                Toast.makeText(this, "Account created! Please Login.", Toast.LENGTH_SHORT).show();
                finish();
            }
        });

        tvGoLogin.setOnClickListener(v -> finish());
    }
}