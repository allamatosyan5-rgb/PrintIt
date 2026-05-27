package alla.matosyan.printit;

import android.content.Intent;
import android.graphics.Color;
import android.graphics.drawable.ColorDrawable;
import android.net.Uri;
import android.os.Bundle;
import android.view.View;
import android.widget.Button;
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

    private TextView tvProductionText, tvProductionFont, tvProductionSize, tvProductionColor;
    private Button btnDownloadRawImage;

    private Button btnEmailCustomer, btnCallCustomer;

    private String visualId;

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

        tvProductionText = findViewById(R.id.tvProductionText);
        tvProductionFont = findViewById(R.id.tvProductionFont);
        tvProductionSize = findViewById(R.id.tvProductionSize);
        tvProductionColor = findViewById(R.id.tvProductionColor);
        btnDownloadRawImage = findViewById(R.id.btnDownloadRawImage);

        btnEmailCustomer = findViewById(R.id.btnEmailCustomer);
        btnCallCustomer = findViewById(R.id.btnCallCustomer);

        db = FirebaseFirestore.getInstance();

        visualId = getIntent().getStringExtra("ORDER_ID");
        String email = getIntent().getStringExtra("ORDER_EMAIL");
        String docId = getIntent().getStringExtra("DOC_ID");

        tvVisualId.setText(visualId != null ? visualId : "Order Details");
        tvEmail.setText("Email: " + (email != null ? email : "Unknown"));

        btnEmailCustomer.setOnClickListener(v -> {
            if (email != null && !email.isEmpty() && !email.equals("Unknown")) {
                Intent emailIntent = new Intent(Intent.ACTION_SENDTO);
                emailIntent.setData(Uri.parse("mailto:" + email));
                emailIntent.putExtra(Intent.EXTRA_SUBJECT, "Question regarding your PrintIt Order: " + visualId);
                startActivity(Intent.createChooser(emailIntent, "Send Email via..."));
            } else {
                Toast.makeText(this, "No email address found.", Toast.LENGTH_SHORT).show();
            }
        });

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
                        String imageUrl = documentSnapshot.getString("imageUrl");

                        String rawText = documentSnapshot.getString("rawText");
                        String rawImageUrl = documentSnapshot.getString("rawImageUrl");
                        String textColor = documentSnapshot.getString("textColor");
                        String textFont = documentSnapshot.getString("textFont");

                        Object textSizeObj = documentSnapshot.get("textSize");
                        String textSize = textSizeObj != null ? String.valueOf(textSizeObj) : null;

                        tvName.setText("Name: " + (name != null ? name : "N/A"));
                        tvPhone.setText("Phone: " + (phone != null ? phone : "N/A"));
                        tvAddress.setText("Shipping: " + (address != null ? address : "N/A"));
                        tvDate.setText("Ordered On: " + (date != null ? date : "N/A"));

                        btnCallCustomer.setOnClickListener(v -> {
                            if (phone != null && !phone.isEmpty()) {
                                Intent callIntent = new Intent(Intent.ACTION_DIAL);
                                callIntent.setData(Uri.parse("tel:" + phone));
                                startActivity(callIntent);
                            } else {
                                Toast.makeText(this, "No phone number provided.", Toast.LENGTH_SHORT).show();
                            }
                        });

                        if ("Approved".equalsIgnoreCase(status) || "Completed".equalsIgnoreCase(status)) {
                            llShippingNotice.setVisibility(View.VISIBLE);
                        } else {
                            llShippingNotice.setVisibility(View.GONE);
                        }

                        if (imageUrl != null && !imageUrl.isEmpty()) {
                            Glide.with(this)
                                    .load(imageUrl)
                                    .placeholder(new ColorDrawable(Color.LTGRAY))
                                    .into(ivProductImage);
                        } else {
                            ivProductImage.setImageDrawable(new ColorDrawable(Color.LTGRAY));
                        }

                        boolean hasText = rawText != null && !rawText.trim().isEmpty();

                        if (hasText) {
                            tvProductionText.setVisibility(View.VISIBLE);
                            tvProductionText.setText("Printed Text: " + rawText);

                            tvProductionFont.setVisibility(View.VISIBLE);
                            tvProductionFont.setText("Font Style: " + (textFont != null ? textFont : "Standard"));

                            tvProductionSize.setVisibility(View.VISIBLE);
                            tvProductionSize.setText("Text Size: " + (textSize != null ? textSize + " px" : "Default"));

                            tvProductionColor.setVisibility(View.VISIBLE);
                            tvProductionColor.setText("Text Color: " + (textColor != null ? textColor : "Default"));
                        } else {
                            tvProductionText.setVisibility(View.GONE);
                            tvProductionFont.setVisibility(View.GONE);
                            tvProductionSize.setVisibility(View.GONE);
                            tvProductionColor.setVisibility(View.GONE);
                        }

                        if (rawImageUrl != null && !rawImageUrl.isEmpty()) {
                            btnDownloadRawImage.setVisibility(View.VISIBLE);
                            btnDownloadRawImage.setOnClickListener(v -> {
                                Intent browserIntent = new Intent(Intent.ACTION_VIEW, Uri.parse(rawImageUrl));
                                startActivity(browserIntent);
                            });
                        } else {
                            btnDownloadRawImage.setVisibility(View.GONE);
                        }
                    }
                })
                .addOnFailureListener(e -> {
                    Toast.makeText(this, "Failed to load order details.", Toast.LENGTH_SHORT).show();
                });
    }
}