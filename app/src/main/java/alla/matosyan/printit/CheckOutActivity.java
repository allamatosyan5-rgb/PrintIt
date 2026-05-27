package alla.matosyan.printit;

import android.app.Activity;
import android.app.Dialog;
import android.os.Bundle;
import android.view.Window;
import android.widget.Button;
import android.widget.ImageView;
import android.widget.RatingBar;
import android.widget.TextView;
import android.widget.Toast;

import androidx.appcompat.app.AppCompatActivity;

import com.google.android.material.textfield.TextInputEditText;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseUser;
import com.google.firebase.firestore.FirebaseFirestore;
import com.stripe.android.PaymentConfiguration;
import com.stripe.android.paymentsheet.PaymentSheet;
import com.stripe.android.paymentsheet.PaymentSheetResult;

import org.json.JSONObject;

import java.io.BufferedReader;
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
    private ImageView ivCheckoutProductImage;

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
        ivCheckoutProductImage = findViewById(R.id.ivCheckoutProductImage);

        db = FirebaseFirestore.getInstance();

        targetIndex = getIntent().getIntExtra("TARGET_ITEM_INDEX", -1);
        if (targetIndex != -1 && targetIndex < CartManager.cartList.size()) {
            singleItemToBuy = CartManager.cartList.get(targetIndex);
            double productTotal = singleItemToBuy.getPrice();

            tvBillProductName.setText(singleItemToBuy.getName());
            tvBillSubtotal.setText(String.format(Locale.US, "$%.2f", productTotal));
            tvBillTotal.setText(String.format(Locale.US, "$%.2f", productTotal));

            if (singleItemToBuy.getImage() != null) {
                ivCheckoutProductImage.setImageBitmap(singleItemToBuy.getImage());
            }
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
                    resetCheckoutButton();
                });
            }
        }).start();
    }

    private void onPaymentSheetResult(final PaymentSheetResult paymentSheetResult) {
        if (paymentSheetResult instanceof PaymentSheetResult.Completed) {
            saveOrderToDatabase();
        } else {
            resetCheckoutButton();
            if (paymentSheetResult instanceof PaymentSheetResult.Failed) {
                Toast.makeText(this, "Payment Failed", Toast.LENGTH_SHORT).show();
            }
        }
    }

    private void saveOrderToDatabase() {
        FirebaseUser currentUser = FirebaseAuth.getInstance().getCurrentUser();
        if (currentUser == null) return;

        String userId = currentUser.getUid();
        String userEmail = currentUser.getEmail() != null ? currentUser.getEmail() : "Guest User";
        String cartItemId = singleItemToBuy.getId();

        if (cartItemId == null) {
            Toast.makeText(this, "Error finding item ID.", Toast.LENGTH_SHORT).show();
            resetCheckoutButton();
            return;
        }

        btnPlaceOrder.setText("FINALIZING ORDER...");

        db.collection("Orders").get().addOnSuccessListener(queryDocumentSnapshots -> {
            int currentOrderCount = queryDocumentSnapshots.size();
            String customOrderId = String.format(Locale.US, "ord-%06d", currentOrderCount + 1);

            db.collection("Users").document(userId).collection("Cart").document(cartItemId)
                    .get()
                    .addOnSuccessListener(cartDoc -> {
                        if (cartDoc.exists()) {
                            Map<String, Object> orderData = new HashMap<>(cartDoc.getData());

                            orderData.put("orderId", customOrderId);
                            orderData.put("customerName", etFullName.getText().toString().trim());
                            orderData.put("phone", etPhone.getText().toString().trim());
                            orderData.put("shippingAddress", etShippingAddress.getText().toString().trim() + ", " + etCity.getText().toString().trim() + " " + etZip.getText().toString().trim() + ", " + etCountry.getText().toString().trim());
                            orderData.put("customerEmail", userEmail);
                            orderData.put("customerId", userId);
                            orderData.put("status", "Pending");
                            orderData.put("paymentMethod", "Stripe Credit Card");

                            String currentDate = new SimpleDateFormat("MMM dd, yyyy - hh:mm a", Locale.getDefault()).format(new Date());
                            orderData.put("orderDate", currentDate);

                            db.collection("Orders").document(customOrderId).set(orderData)
                                    .addOnSuccessListener(aVoid -> {
                                        db.collection("Users").document(userId).collection("Cart").document(cartItemId).delete();
                                        CartManager.cartList.remove(targetIndex);

                                        showRatingDialog(customOrderId);
                                    })
                                    .addOnFailureListener(e -> {
                                        Toast.makeText(this, "Order creation failed.", Toast.LENGTH_SHORT).show();
                                        resetCheckoutButton();
                                    });
                        } else {
                            Toast.makeText(this, "Error: Item no longer in cart.", Toast.LENGTH_SHORT).show();
                            resetCheckoutButton();
                        }
                    }).addOnFailureListener(e -> {
                        Toast.makeText(this, "Failed to retrieve cart item data.", Toast.LENGTH_SHORT).show();
                        resetCheckoutButton();
                    });
        }).addOnFailureListener(e -> {
            Toast.makeText(this, "Failed to initialize order sequence.", Toast.LENGTH_SHORT).show();
            resetCheckoutButton();
        });
    }

    private void showRatingDialog(String customOrderId) {
        Dialog ratingDialog = new Dialog(this);
        ratingDialog.requestWindowFeature(Window.FEATURE_NO_TITLE);
        ratingDialog.setContentView(R.layout.dialog_rating);
        ratingDialog.setCancelable(false);
        RatingBar rb = ratingDialog.findViewById(R.id.rbCheckoutRating);
        Button btnSubmit = ratingDialog.findViewById(R.id.btnSubmitRating);

        btnSubmit.setOnClickListener(v -> {
            float rating = rb.getRating();

            db.collection("Orders").document(customOrderId)
                    .update("experienceRating", rating);

            Toast.makeText(this, "Thank you for your order and feedback!", Toast.LENGTH_LONG).show();
            ratingDialog.dismiss();

            setResult(Activity.RESULT_OK);
            finish();
        });

        ratingDialog.show();
    }

    private void resetCheckoutButton() {
        btnPlaceOrder.setEnabled(true);
        btnPlaceOrder.setText("PAY & PLACE ORDER");
    }
}