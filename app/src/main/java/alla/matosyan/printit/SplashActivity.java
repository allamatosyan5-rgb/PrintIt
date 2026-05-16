package alla.matosyan.printit;

import android.content.Intent;
import android.os.Bundle;
import android.os.Handler;
import android.os.Looper;
import android.widget.Toast;
import androidx.appcompat.app.AppCompatActivity;

import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseUser;
import com.google.firebase.firestore.FirebaseFirestore;

public class SplashActivity extends AppCompatActivity {

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_splash);

        new Handler(Looper.getMainLooper()).postDelayed(this::checkUserAndRoute, 3000);
    }

    private void checkUserAndRoute() {
        FirebaseAuth mAuth = FirebaseAuth.getInstance();
        FirebaseUser currentUser = mAuth.getCurrentUser();

        if (currentUser != null) {
            FirebaseFirestore db = FirebaseFirestore.getInstance();

            db.collection("Users").document(currentUser.getUid()).get()
                    .addOnSuccessListener(documentSnapshot -> {
                        if (documentSnapshot.exists()) {
                            String role = documentSnapshot.getString("role");

                            if (role != null && role.trim().equalsIgnoreCase("EMPLOYEE")) {
                                startActivity(new Intent(SplashActivity.this, EmployeeDashboardActivity.class));
                            } else {
                                startActivity(new Intent(SplashActivity.this, MainActivity.class));
                            }
                        } else {
                            mAuth.signOut();
                            Toast.makeText(this, "Session expired. Please log in again.", Toast.LENGTH_LONG).show();
                            startActivity(new Intent(SplashActivity.this, LoginActivity.class));
                        }
                        finish();
                    })
                    .addOnFailureListener(e -> {
                        mAuth.signOut();
                        Toast.makeText(this, "Network error. Please log in again.", Toast.LENGTH_SHORT).show();
                        startActivity(new Intent(SplashActivity.this, LoginActivity.class));
                        finish();
                    });

        } else {
            startActivity(new Intent(SplashActivity.this, LoginActivity.class));
            finish();
        }
    }
}