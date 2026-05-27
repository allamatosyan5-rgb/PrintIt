package alla.matosyan.printit;

import android.content.Intent;
import android.os.Bundle;
import android.text.TextUtils;
import android.view.View;
import android.widget.Button;
import android.widget.EditText;
import android.widget.RadioButton;
import android.widget.RadioGroup;
import android.widget.TextView;
import android.widget.Toast;
import androidx.appcompat.app.AppCompatActivity;

import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseUser;
import com.google.firebase.firestore.FirebaseFirestore;
import java.util.HashMap;
import java.util.Map;

public class RegisterActivity extends AppCompatActivity {

    private EditText etFirstName, etLastName, etEmail, etPassword, etConfirmPassword, etSecretCode;
    private RadioGroup rgRole;
    private RadioButton rbEmployee;
    private Button btnRegister;
    private TextView tvGoToLogin;

    private FirebaseAuth mAuth;
    private FirebaseFirestore db;

    private final String CAICON_SECRET_CODE = "CAICON-2026";

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_register);

        mAuth = FirebaseAuth.getInstance();
        db = FirebaseFirestore.getInstance();

        etFirstName = findViewById(R.id.et_register_firstname);
        etLastName = findViewById(R.id.et_register_lastname);
        etEmail = findViewById(R.id.et_register_email);
        etPassword = findViewById(R.id.et_register_password);
        etConfirmPassword = findViewById(R.id.et_register_confirm_password);
        etSecretCode = findViewById(R.id.et_secret_code);

        rgRole = findViewById(R.id.rg_register_role);
        rbEmployee = findViewById(R.id.rb_register_employee);
        btnRegister = findViewById(R.id.btn_create_account);
        tvGoToLogin = findViewById(R.id.tv_go_to_login);

        rgRole.setOnCheckedChangeListener((group, checkedId) -> {
            if (checkedId == R.id.rb_register_employee) {
                etSecretCode.setVisibility(View.VISIBLE);
            } else {
                etSecretCode.setVisibility(View.GONE);
                etSecretCode.setText("");
            }
        });

        btnRegister.setOnClickListener(v -> registerUser());

        tvGoToLogin.setOnClickListener(v -> {
            startActivity(new Intent(RegisterActivity.this, LoginActivity.class));
            finish();
        });
    }

    private void registerUser() {
        String fName = etFirstName.getText().toString().trim();
        String lName = etLastName.getText().toString().trim();
        String email = etEmail.getText().toString().trim();
        String password = etPassword.getText().toString().trim();
        String confirmPass = etConfirmPassword.getText().toString().trim();
        boolean isEmployee = rbEmployee.isChecked();

        if (TextUtils.isEmpty(fName) || TextUtils.isEmpty(lName) || TextUtils.isEmpty(email) || TextUtils.isEmpty(password)) {
            Toast.makeText(this, "Please fill in all fields", Toast.LENGTH_SHORT).show();
            return;
        }

        if (!password.equals(confirmPass)) {
            etConfirmPassword.setError("Passwords do not match!");
            return;
        }

        if (isEmployee) {
            String enteredCode = etSecretCode.getText().toString().trim();
            if (!enteredCode.equals(CAICON_SECRET_CODE)) {
                etSecretCode.setError("Invalid Corporate Code!");
                return;
            }
        }

        mAuth.createUserWithEmailAndPassword(email, password).addOnCompleteListener(task -> {
            if (task.isSuccessful()) {
                FirebaseUser user = mAuth.getCurrentUser();
                if (user != null) {
                    String userId = user.getUid();
                    String role = isEmployee ? "EMPLOYEE" : "CUSTOMER";

                    Map<String, Object> userProfile = new HashMap<>();
                    userProfile.put("firstName", fName);
                    userProfile.put("lastName", lName);
                    userProfile.put("fullName", fName + " " + lName);
                    userProfile.put("email", email);
                    userProfile.put("role", role);

                    db.collection("Users").document(userId).set(userProfile)
                            .addOnSuccessListener(aVoid -> {
                                user.sendEmailVerification().addOnCompleteListener(verifyTask -> {
                                    if (verifyTask.isSuccessful()) {
                                        Toast.makeText(RegisterActivity.this, "Check your email for verification link.", Toast.LENGTH_LONG).show();
                                    } else {
                                        Toast.makeText(RegisterActivity.this, "Error sending verification email.", Toast.LENGTH_SHORT).show();
                                    }
                                    mAuth.signOut();
                                    startActivity(new Intent(RegisterActivity.this, LoginActivity.class));
                                    finish();
                                });
                            })
                            .addOnFailureListener(e -> Toast.makeText(RegisterActivity.this, "Database Error: " + e.getMessage(), Toast.LENGTH_LONG).show());
                }
            } else {
                String error = task.getException() != null ? task.getException().getMessage() : "Registration failed";
                Toast.makeText(RegisterActivity.this, error, Toast.LENGTH_LONG).show();
            }
        });
    }
}