package alla.matosyan.printit;

import android.content.Intent;
import android.os.Bundle;
import android.text.TextUtils;
import android.view.View;
import android.widget.Button;
import android.widget.EditText;
import android.widget.RadioButton;
import android.widget.RadioGroup;
import android.widget.Toast;

import androidx.appcompat.app.AppCompatActivity;

import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseUser;
import com.google.firebase.firestore.DocumentSnapshot;
import com.google.firebase.firestore.FirebaseFirestore;

import java.util.HashMap;
import java.util.Map;

public class LoginActivity extends AppCompatActivity {

    private EditText etEmail, etPassword;
    private RadioGroup rgRole;
    private RadioButton rbCustomer;
    private Button btnLogin, btnRegister;

    private FirebaseAuth mAuth;
    private FirebaseFirestore db;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_login);

        mAuth = FirebaseAuth.getInstance();
        db = FirebaseFirestore.getInstance();

        etEmail = findViewById(R.id.et_email);
        etPassword = findViewById(R.id.et_password);
        rgRole = findViewById(R.id.rg_role);
        rbCustomer = findViewById(R.id.rb_customer);
        btnLogin = findViewById(R.id.btn_login);
        btnRegister = findViewById(R.id.btn_register);

        btnLogin.setOnClickListener(v -> loginUser());
        btnRegister.setOnClickListener(v -> registerUser());
    }

    private void loginUser() {
        String email = etEmail.getText().toString().trim();
        String password = etPassword.getText().toString().trim();

        if (TextUtils.isEmpty(email) || TextUtils.isEmpty(password)) {
            Toast.makeText(this, "Please enter email and password", Toast.LENGTH_SHORT).show();
            return;
        }

        mAuth.signInWithEmailAndPassword(email, password)
                .addOnSuccessListener(authResult -> {
                    checkUserRole(authResult.getUser().getUid());
                })
                .addOnFailureListener(e -> {
                    Toast.makeText(this, "Login Failed: " + e.getMessage(), Toast.LENGTH_SHORT).show();
                });
    }

    private void registerUser() {
        String email = etEmail.getText().toString().trim();
        String password = etPassword.getText().toString().trim();

        if (TextUtils.isEmpty(email) || TextUtils.isEmpty(password)) {
            Toast.makeText(this, "Please enter email and password", Toast.LENGTH_SHORT).show();
            return;
        }

        String selectedRole = rbCustomer.isChecked() ? "customer" : "employee";

        mAuth.createUserWithEmailAndPassword(email, password)
                .addOnSuccessListener(authResult -> {
                    String userId = authResult.getUser().getUid();
                    saveRoleToDatabase(userId, email, selectedRole);
                })
                .addOnFailureListener(e -> {
                    Toast.makeText(this, "Registration Failed: " + e.getMessage(), Toast.LENGTH_SHORT).show();
                });
    }

    private void saveRoleToDatabase(String userId, String email, String role) {
        Map<String, Object> userMap = new HashMap<>();
        userMap.put("email", email);
        userMap.put("role", role);

        db.collection("Users").document(userId)
                .set(userMap)
                .addOnSuccessListener(aVoid -> {
                    Toast.makeText(this, "Account Created!", Toast.LENGTH_SHORT).show();
                    routeUser(role);
                })
                .addOnFailureListener(e -> {
                    Toast.makeText(this, "Failed to save role.", Toast.LENGTH_SHORT).show();
                });
    }

    private void checkUserRole(String userId) {
        db.collection("Users").document(userId).get()
                .addOnSuccessListener(documentSnapshot -> {
                    if (documentSnapshot.exists()) {
                        String role = documentSnapshot.getString("role");
                        routeUser(role);
                    } else {
                        Toast.makeText(this, "User data not found.", Toast.LENGTH_SHORT).show();
                    }
                })
                .addOnFailureListener(e -> {
                    Toast.makeText(this, "Failed to fetch user data.", Toast.LENGTH_SHORT).show();
                });
    }

    private void routeUser(String role) {
        Intent intent;
        if ("employee".equals(role)) {
            intent = new Intent(LoginActivity.this, MainActivity.class);
            Toast.makeText(this, "Welcome Employee!", Toast.LENGTH_SHORT).show();
        } else {
            intent = new Intent(LoginActivity.this, MainActivity.class);
            Toast.makeText(this, "Welcome Customer!", Toast.LENGTH_SHORT).show();
        }
        startActivity(intent);
        finish();
    }
}