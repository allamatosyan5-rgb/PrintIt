package alla.matosyan.printit;

import android.os.Bundle;
import android.view.View;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.TextView;
import android.widget.Toast;
import androidx.appcompat.app.AppCompatActivity;

import com.bumptech.glide.Glide;
import com.google.firebase.firestore.FirebaseFirestore;

public class OrderDetailsActivity extends AppCompatActivity {

    private TextView tvVisualId, tvName, tvEmail, tvPhone, tvAddress, tvDate;
    private ImageView ivProductImage;
    private LinearLayout llShippingNotice;
    private FirebaseFirestore db;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_order_details);

        tvVisualId = findViewById(R.id.tvDetailVisualId);
        tvName = findViewById(R.id.tvDetailName);
        tvEmail = findViewById(R.id.tvDetailEmail);
        tvPhone = findViewById(R.id.tvDetailPhone);
        tvAddress = findViewById(R.id.tvDetailAddress);
        tvDate = findViewById(R.id.tvDetailDate);
        ivProductImage = findViewById(R.id.ivDetailProductImage);
        llShippingNotice = findViewById(R.id.llShippingNotice);

        db = FirebaseFirestore.getInstance();

        String visualId = getIntent().getStringExtra("ORDER_ID");
        String email = getIntent().getStringExtra("ORDER_EMAIL");
        String docId = getIntent().getStringExtra("DOC_ID");

        tvVisualId.setText(visualId != null ? visualId : "Order Details");
        tvEmail.setText("Email: " + (email != null ? email : "Unknown"));

        if (docId != null) {
            fetchFullOrderDetails(docId);
        } else {
            Toast.makeText(this, "Error: Could not find Order Database ID", Toast.LENGTH_SHORT).show();
        }
    }

    private void fetchFullOrderDetails(String docId) {
        db.collection("Orders").document(docId).get()
                .addOnSuccessListener(documentSnapshot -> {
                    if (documentSnapshot.exists()) {

                        String name = documentSnapshot.getString("customerName");
                        String phone = documentSnapshot.getString("phone");
                        String address = documentSnapshot.getString("shippingAddress");
                        String date = documentSnapshot.getString("orderDate");
                        String status = documentSnapshot.getString("status");
                        String base64Image = documentSnapshot.getString("designPath");

                        tvName.setText("Name: " + (name != null ? name : "N/A"));
                        tvPhone.setText("Phone: " + (phone != null ? phone : "N/A"));
                        tvAddress.setText("Shipping: " + (address != null ? address : "N/A"));
                        tvDate.setText("Ordered On: " + (date != null ? date : "N/A"));

                        if ("Approved".equals(status)) {
                            llShippingNotice.setVisibility(View.VISIBLE);
                        } else {
                            llShippingNotice.setVisibility(View.GONE);
                        }

                        if (base64Image != null && !base64Image.isEmpty()) {
                            try {
                                byte[] decodedString = android.util.Base64.decode(base64Image, android.util.Base64.DEFAULT);
                                Glide.with(this)
                                        .asBitmap()
                                        .load(decodedString)
                                        .into(ivProductImage);
                            } catch (Exception e) {
                                ivProductImage.setImageResource(R.drawable.blank_pillow);
                            }
                        }
                    }
                })
                .addOnFailureListener(e -> {
                    Toast.makeText(this, "Failed to load order details.", Toast.LENGTH_SHORT).show();
                });
    }
}