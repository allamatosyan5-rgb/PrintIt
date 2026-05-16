package alla.matosyan.printit;

import android.os.Bundle;
import android.widget.Button;
import android.widget.Toast;
import androidx.appcompat.app.AppCompatActivity;
import com.google.android.material.textfield.TextInputEditText;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseUser;
import com.google.firebase.firestore.FirebaseFirestore;
import java.util.HashMap;
import java.util.Map;

public class EditProfileActivity extends AppCompatActivity {

    private TextInputEditText etEditName, etEditEmail, etEditPhone;
    private Button btnSaveProfile;
    private FirebaseFirestore db;
    private FirebaseUser currentUser;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_edit_profile);

        etEditName = findViewById(R.id.etEditName);
        etEditEmail = findViewById(R.id.etEditEmail);
        etEditPhone = findViewById(R.id.etEditPhone);
        btnSaveProfile = findViewById(R.id.btnSaveProfile);

        db = FirebaseFirestore.getInstance();
        currentUser = FirebaseAuth.getInstance().getCurrentUser();

        if (currentUser != null) {
            etEditEmail.setText(currentUser.getEmail());

            db.collection("Users").document(currentUser.getUid()).get()
                    .addOnSuccessListener(documentSnapshot -> {
                        if (documentSnapshot.exists()) {
                            etEditName.setText(documentSnapshot.getString("fullName"));
                            etEditPhone.setText(documentSnapshot.getString("phone"));
                        }
                    });
        }

        btnSaveProfile.setOnClickListener(v -> saveProfileData());
    }

    private void saveProfileData() {
        String name = etEditName.getText().toString().trim();
        String newEmail = etEditEmail.getText().toString().trim();
        String phone = etEditPhone.getText().toString().trim();

        if (name.isEmpty() || newEmail.isEmpty() || phone.isEmpty()) {
            Toast.makeText(this, "Please fill out all fields", Toast.LENGTH_SHORT).show();
            return;
        }

        if (currentUser == null) return;
        currentUser.verifyBeforeUpdateEmail(newEmail).addOnCompleteListener(task -> {
            if (task.isSuccessful() || currentUser.getEmail().equals(newEmail)) {

                Map<String, Object> userData = new HashMap<>();
                userData.put("fullName", name);
                userData.put("phone", phone);
                userData.put("email", newEmail);

                db.collection("Users").document(currentUser.getUid())
                        .update(userData)
                        .addOnSuccessListener(aVoid -> {
                            Toast.makeText(this, "Profile Updated!", Toast.LENGTH_SHORT).show();
                            finish();
                        })
                        .addOnFailureListener(e -> {
                            db.collection("Users").document(currentUser.getUid()).set(userData);
                            Toast.makeText(this, "Profile Saved!", Toast.LENGTH_SHORT).show();
                            finish();
                        });
            } else {
                Toast.makeText(this, "Email update failed. You may need to log out and back in.", Toast.LENGTH_LONG).show();
            }
        });
    }
}