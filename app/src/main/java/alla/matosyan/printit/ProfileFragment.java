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
import androidx.appcompat.app.AlertDialog;
import androidx.fragment.app.Fragment;

import com.bumptech.glide.Glide;
import com.bumptech.glide.load.engine.DiskCacheStrategy;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseUser;
import com.google.firebase.firestore.FirebaseFirestore;
import com.google.firebase.storage.FirebaseStorage;
import com.google.firebase.storage.StorageReference;

public class ProfileFragment extends Fragment {

    private TextView tvProfileName, tvProfileEmail;
    private ImageView ivProfilePhoto;
    private Button btnLogout, btnEditProfile, btnDeleteAccount;
    private FirebaseFirestore db;

    private final ActivityResultLauncher<String> galleryLauncher = registerForActivityResult(
            new ActivityResultContracts.GetContent(),
            uri -> {
                if (uri != null) {
                    ivProfilePhoto.setImageURI(uri);
                    uploadProfileImage(uri);
                }
            }
    );

    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container, @Nullable Bundle savedInstanceState) {
        View view = inflater.inflate(R.layout.fragment_profile, container, false);

        tvProfileName = view.findViewById(R.id.tvProfileName);
        tvProfileEmail = view.findViewById(R.id.tvProfileEmail);
        ivProfilePhoto = view.findViewById(R.id.ivProfilePhoto);
        btnLogout = view.findViewById(R.id.btnLogout);
        btnEditProfile = view.findViewById(R.id.btnEditProfile);
        btnDeleteAccount = view.findViewById(R.id.btnDeleteAccount);

        db = FirebaseFirestore.getInstance();

        if (ivProfilePhoto != null) {
            ivProfilePhoto.setOnClickListener(v -> {
                FirebaseUser user = FirebaseAuth.getInstance().getCurrentUser();
                if (user == null) return;

                db.collection("Users").document(user.getUid()).get()
                        .addOnSuccessListener(documentSnapshot -> {
                            String url = documentSnapshot.getString("profileImageUrl");
                            boolean hasPhoto = url != null && !url.isEmpty();

                            String[] options = hasPhoto ? new String[]{"Change Photo", "Remove Photo"} : new String[]{"Change Photo"};

                            new AlertDialog.Builder(requireContext())
                                    .setTitle("Profile Photo")
                                    .setItems(options, (dialog, which) -> {
                                        if (options[which].equals("Change Photo")) {
                                            galleryLauncher.launch("image/*");
                                        } else {
                                            removeProfileImage();
                                        }
                                    }).show();
                        });
            });
        }

        btnEditProfile.setOnClickListener(v -> startActivity(new Intent(getActivity(), EditProfileActivity.class)));

        btnLogout.setOnClickListener(v -> {
            FirebaseAuth.getInstance().signOut();
            startActivity(new Intent(getActivity(), LoginActivity.class));
            if (getActivity() != null) getActivity().finish();
        });

        btnDeleteAccount.setOnClickListener(v -> {
            FirebaseUser user = FirebaseAuth.getInstance().getCurrentUser();
            if (user != null) {
                String userId = user.getUid();
                btnDeleteAccount.setEnabled(false);
                btnDeleteAccount.setText("Deleting...");

                db.collection("Users").document(userId).delete()
                        .addOnSuccessListener(aVoid -> {
                            FirebaseStorage.getInstance().getReference().child("Users/" + userId + "/profile.jpg").delete();
                            user.delete().addOnCompleteListener(task -> {
                                if (task.isSuccessful()) {
                                    Toast.makeText(getContext(), "Account deleted. Order history preserved.", Toast.LENGTH_SHORT).show();
                                    startActivity(new Intent(getActivity(), LoginActivity.class));
                                    if (getActivity() != null) getActivity().finish();
                                } else {
                                    btnDeleteAccount.setEnabled(true);
                                    btnDeleteAccount.setText("Delete Account");
                                    Toast.makeText(getContext(), "Security requirement: Re-login to delete.", Toast.LENGTH_LONG).show();
                                }
                            });
                        });
            }
        });

        return view;
    }

    private void removeProfileImage() {
        FirebaseUser user = FirebaseAuth.getInstance().getCurrentUser();
        if (user == null) return;

        FirebaseStorage.getInstance().getReference().child("Users/" + user.getUid() + "/profile.jpg").delete()
                .addOnCompleteListener(task -> {
                    db.collection("Users").document(user.getUid()).update("profileImageUrl", null)
                            .addOnSuccessListener(aVoid -> {
                                ivProfilePhoto.setImageResource(R.drawable.ic_profile);
                                Toast.makeText(getContext(), "Photo removed.", Toast.LENGTH_SHORT).show();
                            });
                });
    }

    private void uploadProfileImage(Uri imageUri) {
        FirebaseUser user = FirebaseAuth.getInstance().getCurrentUser();
        if (user == null) return;
        StorageReference fileRef = FirebaseStorage.getInstance().getReference().child("Users/" + user.getUid() + "/profile.jpg");
        fileRef.putFile(imageUri).addOnSuccessListener(taskSnapshot ->
                fileRef.getDownloadUrl().addOnSuccessListener(uri ->
                        db.collection("Users").document(user.getUid()).update("profileImageUrl", uri.toString())
                                .addOnSuccessListener(aVoid -> loadUserData())
                )
        );
    }

    @Override
    public void onResume() {
        super.onResume();
        loadUserData();
    }

    private void loadUserData() {
        FirebaseUser currentUser = FirebaseAuth.getInstance().getCurrentUser();
        if (currentUser != null) {
            tvProfileEmail.setText(currentUser.getEmail());
            db.collection("Users").document(currentUser.getUid()).get()
                    .addOnSuccessListener(doc -> {
                        if (doc.exists()) {
                            String name = doc.getString("fullName");
                            if (name != null) tvProfileName.setText(name);

                            String img = doc.getString("profileImageUrl");
                            if (img != null && !img.isEmpty() && getContext() != null) {
                                Glide.with(getContext())
                                        .load(img)
                                        .skipMemoryCache(true)
                                        .diskCacheStrategy(DiskCacheStrategy.NONE)
                                        .circleCrop()
                                        .into(ivProfilePhoto);
                            } else {
                                ivProfilePhoto.setImageResource(R.drawable.ic_profile);
                            }
                        }
                    });
        }
    }
}