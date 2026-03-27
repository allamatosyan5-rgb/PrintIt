package alla.matosyan.printit;

import android.os.Bundle;
import android.widget.Button;
import android.widget.EditText;
import android.widget.Toast;
import androidx.appcompat.app.AppCompatActivity;

import com.google.firebase.firestore.DocumentSnapshot;
import com.google.firebase.firestore.FirebaseFirestore;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class CheckoutActivity extends AppCompatActivity {

    private EditText etName, etPhone, etAddress;
    private Button btnPlaceOrder;
    private FirebaseFirestore db;

    private String singleCartItemId = null;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_checkout);

        db = FirebaseFirestore.getInstance();

        etName = findViewById(R.id.et_checkout_name);
        etPhone = findViewById(R.id.et_checkout_phone);
        etAddress = findViewById(R.id.et_checkout_address);
        btnPlaceOrder = findViewById(R.id.btn_place_order);

        if (getIntent().hasExtra("CART_ITEM_ID")) {
            singleCartItemId = getIntent().getStringExtra("CART_ITEM_ID");
        }

        btnPlaceOrder.setOnClickListener(v -> processSingleOrder());
    }

    private void processSingleOrder() {
        if (singleCartItemId == null) {
            Toast.makeText(this, "Error: No item selected!", Toast.LENGTH_SHORT).show();
            return;
        }

        String name = etName.getText().toString().trim();
        String phone = etPhone.getText().toString().trim();
        String address = etAddress.getText().toString().trim();

        if (name.isEmpty() || phone.isEmpty() || address.isEmpty()) {
            Toast.makeText(this, "Please fill in all shipping details", Toast.LENGTH_SHORT).show();
            return;
        }

        btnPlaceOrder.setText("Processing...");
        btnPlaceOrder.setEnabled(false);

        db.collection("cart_items").document(singleCartItemId).get().addOnSuccessListener(documentSnapshot -> {

            if (!documentSnapshot.exists()) {
                Toast.makeText(this, "Item no longer exists!", Toast.LENGTH_SHORT).show();
                btnPlaceOrder.setText("Place Order");
                btnPlaceOrder.setEnabled(true);
                return;
            }

            Map<String, Object> itemData = documentSnapshot.getData();

            List<Map<String, Object>> orderedItems = new ArrayList<>();
            orderedItems.add(itemData);

            double totalPrice = 0.0;
            if (itemData != null && itemData.containsKey("price")) {
                totalPrice = documentSnapshot.getDouble("price");
            }

            Map<String, Object> orderData = new HashMap<>();
            orderData.put("customerName", name);
            orderData.put("customerPhone", phone);
            orderData.put("shippingAddress", address);
            orderData.put("orderStatus", "Pending Production");

            java.text.SimpleDateFormat formatter = new java.text.SimpleDateFormat("dd MMM yyyy, HH:mm", java.util.Locale.getDefault());
            String exactDate = formatter.format(new java.util.Date());
            orderData.put("orderDate", exactDate);

            orderData.put("items", orderedItems);
            orderData.put("totalPrice", totalPrice);

            db.collection("orders").add(orderData)
                    .addOnSuccessListener(documentReference -> {

                        db.collection("cart_items").document(singleCartItemId).delete();
                        Toast.makeText(this, "Order Placed Successfully!", Toast.LENGTH_LONG).show();
                        finish();
                    })
                    .addOnFailureListener(e -> {
                        Toast.makeText(this, "Order failed: " + e.getMessage(), Toast.LENGTH_LONG).show();
                        btnPlaceOrder.setText("Place Order");
                        btnPlaceOrder.setEnabled(true);
                    });

        }).addOnFailureListener(e -> {
            Toast.makeText(this, "Failed to connect to database", Toast.LENGTH_SHORT).show();
            btnPlaceOrder.setText("Place Order");
            btnPlaceOrder.setEnabled(true);
        });
    }
}