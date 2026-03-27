package alla.matosyan.printit;

import android.content.Intent;
import android.net.Uri;
import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Button;
import android.widget.ImageView;
import android.widget.TextView;
import android.widget.Toast;

import androidx.activity.result.ActivityResultLauncher;
import androidx.activity.result.contract.ActivityResultContracts;
import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.fragment.app.Fragment;

import com.bumptech.glide.Glide;
import com.bumptech.glide.load.engine.DiskCacheStrategy;
import com.bumptech.glide.signature.ObjectKey;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseUser;
import com.google.firebase.firestore.FirebaseFirestore;
import com.google.firebase.storage.FirebaseStorage;
import com.google.firebase.storage.StorageReference;

public class ProfileFragment extends Fragment {

    private TextView tvName, tvEmail;
    private ImageView profileImage;
    private FirebaseAuth mAuth;
    private FirebaseFirestore db;
    private FirebaseStorage storage;
    private Uri imageUri;

    private final ActivityResultLauncher<String> galleryLauncher = registerForActivityResult(
            new ActivityResultContracts.GetContent(),
            uri -> {
                if (uri != null) {
                    imageUri = uri;
                    profileImage.setImageURI(uri);
                    uploadProfileImage();
                }
            }
    );

    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container, @Nullable Bundle savedInstanceState) {
        View view = inflater.inflate(R.layout.fragment_profile, container, false);

        mAuth = FirebaseAuth.getInstance();
        db = FirebaseFirestore.getInstance();
        storage = FirebaseStorage.getInstance();

        tvName = view.findViewById(R.id.profile_name);
        tvEmail = view.findViewById(R.id.profile_email);
        profileImage = view.findViewById(R.id.profile_image);
        Button btnOrders = view.findViewById(R.id.btn_my_orders);
        Button btnLogout = view.findViewById(R.id.btn_logout);

        loadUserData();

        profileImage.setOnClickListener(v -> galleryLauncher.launch("image/*"));

        btnOrders.setOnClickListener(v -> {
            Toast.makeText(getActivity(), "Opening Orders...", Toast.LENGTH_SHORT).show();
        });

        btnLogout.setOnClickListener(v -> {
            mAuth.signOut();
            startActivity(new Intent(getActivity(), LoginActivity.class));
            getActivity().finish();
        });

        return view;
    }

    private void loadUserData() {
        FirebaseUser user = mAuth.getCurrentUser();
        if (user != null) {
            tvEmail.setText(user.getEmail());

            db.collection("Users").document(user.getUid()).get()
                    .addOnSuccessListener(documentSnapshot -> {
                        if (documentSnapshot.exists() && isAdded()) {
                            tvName.setText(documentSnapshot.getString("fullName"));
                            String photoUrl = documentSnapshot.getString("profileImageUrl");

                            if (photoUrl != null && !photoUrl.isEmpty()) {
                                Glide.with(this)
                                        .load(photoUrl)
                                        .signature(new ObjectKey(System.currentTimeMillis()))
                                        .placeholder(R.drawable.profile_placeholder)
                                        .into(profileImage);
                            }
                        }
                    });
        }
    }

    private void uploadProfileImage() {
        FirebaseUser user = mAuth.getCurrentUser();
        if (user == null || imageUri == null) return;

        Toast.makeText(getContext(), "Updating Profile...", Toast.LENGTH_SHORT).show();
        StorageReference fileRef = storage.getReference().child("profile_pics/" + user.getUid() + ".jpg");

        fileRef.putFile(imageUri).addOnSuccessListener(taskSnapshot -> {
            fileRef.getDownloadUrl().addOnSuccessListener(uri -> {
                db.collection("Users").document(user.getUid())
                        .update("profileImageUrl", uri.toString())
                        .addOnSuccessListener(aVoid -> {
                            if (isAdded()) {
                                Glide.with(this)
                                        .load(uri)
                                        .signature(new ObjectKey(System.currentTimeMillis()))
                                        .circleCrop()
                                        .into(profileImage);
                                Toast.makeText(getContext(), "Profile Photo Updated!", Toast.LENGTH_SHORT).show();
                            }
                        });
            });
        }).addOnFailureListener(e -> Toast.makeText(getContext(), "Upload Failed", Toast.LENGTH_SHORT).show());
    }
}