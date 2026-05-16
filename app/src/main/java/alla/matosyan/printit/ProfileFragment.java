package alla.matosyan.printit;

import android.content.Intent;
import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Button;
import android.widget.TextView;
import android.widget.Toast;
import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.fragment.app.Fragment;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseUser;
import com.google.firebase.firestore.DocumentSnapshot;
import com.google.firebase.firestore.FirebaseFirestore;

public class ProfileFragment extends Fragment {

    private TextView tvProfileName, tvProfileEmail;
    private Button btnLogout, btnEditProfile, btnShippingInfo, btnDeleteAccount;
    private FirebaseFirestore db;

    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container, @Nullable Bundle savedInstanceState) {
        View view = inflater.inflate(R.layout.fragment_profile, container, false);

        tvProfileName = view.findViewById(R.id.tvProfileName);
        tvProfileEmail = view.findViewById(R.id.tvProfileEmail);
        btnLogout = view.findViewById(R.id.btnLogout);
        btnEditProfile = view.findViewById(R.id.btnEditProfile);
        btnShippingInfo = view.findViewById(R.id.btnShippingInfo);
        btnDeleteAccount = view.findViewById(R.id.btnDeleteAccount);

        db = FirebaseFirestore.getInstance();

        btnEditProfile.setOnClickListener(v -> {
            Intent intent = new Intent(getActivity(), EditProfileActivity.class);
            startActivity(intent);
        });

        btnLogout.setOnClickListener(v -> {
            FirebaseAuth.getInstance().signOut();
            Toast.makeText(getContext(), "Logged out successfully", Toast.LENGTH_SHORT).show();
            startActivity(new Intent(getActivity(), LoginActivity.class));
            if (getActivity() != null) {
                getActivity().finish();
            }
        });

        btnShippingInfo.setOnClickListener(v -> Toast.makeText(getContext(), "Shipping coming soon!", Toast.LENGTH_SHORT).show());

        btnDeleteAccount.setOnClickListener(v -> {
            FirebaseUser user = FirebaseAuth.getInstance().getCurrentUser();
            if (user != null) {
                String userId = user.getUid();

                db.collection("Orders")
                        .whereEqualTo("userId", userId)
                        .get()
                        .addOnSuccessListener(queryDocumentSnapshots -> {
                            for (DocumentSnapshot document : queryDocumentSnapshots) {
                                db.collection("Orders").document(document.getId()).delete();
                            }

                            db.collection("Users").document(userId).delete()
                                    .addOnSuccessListener(aVoid -> {
                                        user.delete().addOnCompleteListener(task -> {
                                            if (task.isSuccessful()) {
                                                if (getContext() != null) {
                                                    Toast.makeText(getContext(), "Account and all history deleted.", Toast.LENGTH_SHORT).show();
                                                }
                                                startActivity(new Intent(getActivity(), LoginActivity.class));
                                                if (getActivity() != null) {
                                                    getActivity().finish();
                                                }
                                            } else {
                                                if (getContext() != null) {
                                                    Toast.makeText(getContext(), "Failed to delete auth account.", Toast.LENGTH_SHORT).show();
                                                }
                                            }
                                        });
                                    });
                        })
                        .addOnFailureListener(e -> {
                            if (getContext() != null) {
                                Toast.makeText(getContext(), "Error connecting to database.", Toast.LENGTH_SHORT).show();
                            }
                        });
            }
        });

        return view;
    }

    @Override
    public void onResume() {
        super.onResume();
        loadUserData();
    }

    private void loadUserData() {
        FirebaseUser currentUser = FirebaseAuth.getInstance().getCurrentUser();

        if (currentUser != null) {
            String email = currentUser.getEmail();
            tvProfileEmail.setText(email != null ? email : "No Email Found");

            String authName = currentUser.getDisplayName();
            if (authName != null && !authName.isEmpty()) {
                tvProfileName.setText(authName);
            } else {
                tvProfileName.setText("Loading...");
            }

            String uid = currentUser.getUid();

            db.collection("Users").document(uid).get()
                    .addOnSuccessListener(documentSnapshot -> {
                        if (documentSnapshot.exists()) {
                            String realName = documentSnapshot.getString("fullName");
                            if (realName == null) {
                                realName = documentSnapshot.getString("name");
                            }

                            if (realName != null && !realName.isEmpty()) {
                                tvProfileName.setText(realName);
                            } else if (authName == null || authName.isEmpty()) {
                                tvProfileName.setText("Name Not Set");
                            }

                        } else if (authName == null || authName.isEmpty()) {
                            tvProfileName.setText("Name Not Set");
                        }
                    })
                    .addOnFailureListener(e -> {
                        if (authName == null || authName.isEmpty()) {
                            tvProfileName.setText("Error loading data");
                        }
                    });
        } else {
            if (getContext() != null) {
                Toast.makeText(getContext(), "Please log in to continue.", Toast.LENGTH_SHORT).show();
            }
            startActivity(new Intent(getActivity(), LoginActivity.class));
            if (getActivity() != null) {
                getActivity().finish();
            }
        }
    }
}