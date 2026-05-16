package alla.matosyan.printit;

import android.content.Intent;
import android.os.Bundle;
import android.widget.Button;
import android.widget.EditText;
import android.widget.Toast;
import androidx.appcompat.app.AppCompatActivity;

import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.firestore.FirebaseFirestore;

public class LoginActivity extends AppCompatActivity {

    private EditText etEmail, etPassword;
    private Button btnLogin, btnRegister;

    private FirebaseAuth mAuth;
    private FirebaseFirestore db;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_login);



        etEmail = findViewById(R.id.et_email);
        etPassword = findViewById(R.id.et_password);
        btnLogin = findViewById(R.id.btn_login);
        btnRegister = findViewById(R.id.btn_register);

        btnLogin.setOnClickListener(v -> {
            String email = etEmail.getText().toString().trim();
            String password = etPassword.getText().toString().trim();

            if (email.isEmpty() || password.isEmpty()) {
                Toast.makeText(this, "Please enter both email and password.", Toast.LENGTH_SHORT).show();
                return;
            }

            mAuth.signInWithEmailAndPassword(email, password).addOnCompleteListener(task -> {
                if (task.isSuccessful()) {

                    if (!mAuth.getCurrentUser().isEmailVerified()) {
                        Toast.makeText(this, "Access Denied: Please verify your email address first.", Toast.LENGTH_LONG).show();
                        mAuth.signOut();
                        return;
                    }

                    String userId = mAuth.getCurrentUser().getUid();

                    db.collection("Users").document(userId).get()
                            .addOnSuccessListener(documentSnapshot -> {
                                if (documentSnapshot.exists()) {
                                    String role = documentSnapshot.getString("role");

                                    if ("EMPLOYEE".equals(role)) {
                                        Toast.makeText(this, "Welcome to the CAICON Dashboard", Toast.LENGTH_SHORT).show();
                                        startActivity(new Intent(LoginActivity.this, EmployeeDashboardActivity.class));
                                    } else {
                                        Toast.makeText(this, "Welcome back!", Toast.LENGTH_SHORT).show();
                                        startActivity(new Intent(LoginActivity.this, MainActivity.class));
                                    }
                                    finish();
                                } else {
                                    Toast.makeText(this, "User profile not found. Redirecting to Register...", Toast.LENGTH_SHORT).show();
                                    startActivity(new Intent(LoginActivity.this, RegisterActivity.class));
                                    finish();
                                }
                            })
                            .addOnFailureListener(e -> {
                                Toast.makeText(this, "Error fetching role.", Toast.LENGTH_SHORT).show();
                            });
                }
            });
        });

        btnRegister.setOnClickListener(v -> {
            startActivity(new Intent(LoginActivity.this, RegisterActivity.class));
        });
    }
}