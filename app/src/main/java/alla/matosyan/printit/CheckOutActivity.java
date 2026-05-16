package alla.matosyan.printit;

import android.app.Activity;
import android.graphics.Bitmap;
import android.os.Bundle;
import android.widget.Button;
import android.widget.TextView;
import android.widget.Toast;
import androidx.appcompat.app.AppCompatActivity;
import com.google.android.material.textfield.TextInputEditText;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseUser;
import com.google.firebase.firestore.FieldValue;
import com.google.firebase.firestore.FirebaseFirestore;
import com.stripe.android.PaymentConfiguration;
import com.stripe.android.paymentsheet.PaymentSheet;
import com.stripe.android.paymentsheet.PaymentSheetResult;
import org.json.JSONObject;
import java.io.BufferedReader;
import java.io.ByteArrayOutputStream;
import java.io.InputStreamReader;
import java.io.OutputStream;
import java.net.HttpURLConnection;
import java.net.URL;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.HashMap;
import java.util.Locale;
import java.util.Map;

public class CheckOutActivity extends AppCompatActivity {

    private TextInputEditText etFullName, etPhone, etShippingAddress, etCity, etZip, etCountry;
    private Button btnPlaceOrder;
    private TextView tvBillProductName, tvBillSubtotal, tvBillTotal;

    private FirebaseFirestore db;
    private PaymentSheet paymentSheet;

    private CartItem singleItemToBuy;
    private int targetIndex;

    private final String PUBLISHABLE_KEY = "pk_test_51TX5YnIkmLY8bTRGW8tz3xDD6t6xJd0iCyGMqc6lSuB4HLVSSRvgyiaSApemhiFruu1EIUmnpmujGvhZKg0RH2vW00ER7rIzck";
    private final String SECRET_KEY = "sk_test_51TX5YnIkmLY8bTRG3fyanuRtyBSUmOKuIjugVH4dxrkTqtcfqjyza2BIVOIuN4QzHm2YRoZiPuHu8drlGnEfuDHW00ctAYoq47";

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_checkout);

        PaymentConfiguration.init(getApplicationContext(), PUBLISHABLE_KEY);
        paymentSheet = new PaymentSheet(this, this::onPaymentSheetResult);

        etFullName = findViewById(R.id.etFullName);
        etPhone = findViewById(R.id.etPhone);
        etShippingAddress = findViewById(R.id.etShippingAddress);
        etCity = findViewById(R.id.etCity);
        etZip = findViewById(R.id.etZip);
        etCountry = findViewById(R.id.etCountry);
        btnPlaceOrder = findViewById(R.id.btnPlaceOrder);
        tvBillProductName = findViewById(R.id.tvBillProductName);
        tvBillSubtotal = findViewById(R.id.tvBillSubtotal);
        tvBillTotal = findViewById(R.id.tvBillTotal);

        db = FirebaseFirestore.getInstance();

        targetIndex = getIntent().getIntExtra("TARGET_ITEM_INDEX", -1);
        if (targetIndex != -1 && targetIndex < CartManager.cartList.size()) {
            singleItemToBuy = CartManager.cartList.get(targetIndex);
            double productTotal = singleItemToBuy.getPrice();

            tvBillProductName.setText(singleItemToBuy.getName());
            tvBillSubtotal.setText(String.format(Locale.US, "$%.2f", productTotal));
            tvBillTotal.setText(String.format(Locale.US, "$%.2f", productTotal));
        } else {
            Toast.makeText(this, "Error loading cart.", Toast.LENGTH_SHORT).show();
            finish();
        }

        btnPlaceOrder.setOnClickListener(v -> {
            if (isInputValid()) {
                initiatePureJavaPayment();
            }
        });
    }

    private boolean isInputValid() {
        String name = etFullName.getText().toString().trim();
        String phone = etPhone.getText().toString().trim();
        String address = etShippingAddress.getText().toString().trim();
        String city = etCity.getText().toString().trim();
        String zip = etZip.getText().toString().trim();
        String country = etCountry.getText().toString().trim();

        if (name.isEmpty() || phone.isEmpty() || address.isEmpty() || city.isEmpty() || zip.isEmpty() || country.isEmpty()) {
            Toast.makeText(this, "Please fill in all shipping details.", Toast.LENGTH_SHORT).show();
            return false;
        }

        if (phone.length() < 7) {
            etPhone.setError("Enter a valid phone number");
            return false;
        }

        if (zip.length() < 3) {
            etZip.setError("ZIP code is too short");
            return false;
        }

        if (address.length() < 5) {
            etShippingAddress.setError("Please provide a full street address");
            return false;
        }

        return true;
    }

    private void initiatePureJavaPayment() {
        btnPlaceOrder.setText("CONNECTING TO BANK...");
        btnPlaceOrder.setEnabled(false);

        new Thread(() -> {
            try {
                int amountInCents = (int) Math.round(singleItemToBuy.getPrice() * 100);

                URL url = new URL("https://api.stripe.com/v1/payment_intents");
                HttpURLConnection conn = (HttpURLConnection) url.openConnection();
                conn.setRequestMethod("POST");
                conn.setRequestProperty("Authorization", "Bearer " + SECRET_KEY);
                conn.setRequestProperty("Content-Type", "application/x-www-form-urlencoded");
                conn.setDoOutput(true);

                String postData = "amount=" + amountInCents + "&currency=usd";
                OutputStream os = conn.getOutputStream();
                os.write(postData.getBytes());
                os.flush();
                os.close();

                BufferedReader br = new BufferedReader(new InputStreamReader(conn.getInputStream()));
                StringBuilder response = new StringBuilder();
                String line;
                while ((line = br.readLine()) != null) {
                    response.append(line);
                }
                br.close();

                JSONObject jsonResponse = new JSONObject(response.toString());
                String clientSecret = jsonResponse.getString("client_secret");

                runOnUiThread(() -> {
                    PaymentSheet.Configuration configuration = new PaymentSheet.Configuration("CAICON PrintIt");
                    paymentSheet.presentWithPaymentIntent(clientSecret, configuration);
                });

            } catch (Exception e) {
                e.printStackTrace();
                runOnUiThread(() -> {
                    Toast.makeText(this, "Network Error. Check connection.", Toast.LENGTH_SHORT).show();
                    btnPlaceOrder.setText("PAY & PLACE ORDER");
                    btnPlaceOrder.setEnabled(true);
                });
            }
        }).start();
    }

    private void onPaymentSheetResult(final PaymentSheetResult paymentSheetResult) {
        if (paymentSheetResult instanceof PaymentSheetResult.Completed) {
            saveOrderToDatabase();
        } else {
            btnPlaceOrder.setText("PAY & PLACE ORDER");
            btnPlaceOrder.setEnabled(true);
            if (paymentSheetResult instanceof PaymentSheetResult.Failed) {
                Toast.makeText(this, "Payment Failed", Toast.LENGTH_SHORT).show();
            }
        }
    }

    private void saveOrderToDatabase() {
        FirebaseUser currentUser = FirebaseAuth.getInstance().getCurrentUser();
        String userEmail = (currentUser != null && currentUser.getEmail() != null) ? currentUser.getEmail() : "Guest User";
        String userId = (currentUser != null) ? currentUser.getUid() : "UnknownDevice";

        db.collection("Orders").get().addOnSuccessListener(queryDocumentSnapshots -> {
            int currentOrderCount = queryDocumentSnapshots.size();
            String customOrderId = String.format(Locale.US, "ord-%06d", currentOrderCount);

            Map<String, Object> orderData = new HashMap<>();
            orderData.put("orderId", customOrderId);
            orderData.put("customerName", etFullName.getText().toString().trim());
            orderData.put("phone", etPhone.getText().toString().trim());
            orderData.put("shippingAddress", etShippingAddress.getText().toString().trim() + ", " + etCity.getText().toString().trim() + " " + etZip.getText().toString().trim() + ", " + etCountry.getText().toString().trim());
            orderData.put("customerEmail", userEmail);
            orderData.put("customerId", userId);
            orderData.put("status", "Pending");
            orderData.put("productName", singleItemToBuy.getName());
            orderData.put("paymentMethod", "Stripe Credit Card");
            orderData.put("price", singleItemToBuy.getPrice());

            String encodedImage = "";
            Bitmap bitmap = singleItemToBuy.getImage();
            if (bitmap != null) {
                ByteArrayOutputStream baos = new ByteArrayOutputStream();
                bitmap.compress(Bitmap.CompressFormat.JPEG, 50, baos);
                byte[] b = baos.toByteArray();
                encodedImage = android.util.Base64.encodeToString(b, android.util.Base64.DEFAULT);
            }
            orderData.put("designPath", encodedImage);

            String currentDate = new SimpleDateFormat("MMM dd, yyyy - hh:mm a", Locale.getDefault()).format(new Date());
            orderData.put("orderDate", currentDate);
            orderData.put("timestamp", FieldValue.serverTimestamp());

            db.collection("Orders").document(customOrderId).set(orderData)
                    .addOnSuccessListener(aVoid -> {
                        CartManager.cartList.remove(targetIndex);
                        CartManager.saveCart(this);

                        Toast.makeText(this, "SUCCESS! ORDER PLACED.", Toast.LENGTH_LONG).show();

                        setResult(Activity.RESULT_OK);
                        finish();
                    })
                    .addOnFailureListener(e -> {
                        Toast.makeText(this, "Failed to complete order document creation.", Toast.LENGTH_SHORT).show();
                        btnPlaceOrder.setEnabled(true);
                        btnPlaceOrder.setText("PAY & PLACE ORDER");
                    });
        }).addOnFailureListener(e -> {
            Toast.makeText(this, "Failed to initialize order sequencing sequence.", Toast.LENGTH_SHORT).show();
            btnPlaceOrder.setEnabled(true);
            btnPlaceOrder.setText("PAY & PLACE ORDER");
        });
    }
}