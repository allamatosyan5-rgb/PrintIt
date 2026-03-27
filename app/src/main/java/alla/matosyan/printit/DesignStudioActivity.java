package alla.matosyan.printit;

import android.annotation.SuppressLint;
import android.content.Context;
import android.content.Intent;
import android.net.Uri;
import android.os.Bundle;
import android.util.TypedValue;
import android.view.MotionEvent;
import android.view.View;
import android.view.inputmethod.InputMethodManager;
import android.widget.AdapterView;
import android.widget.ArrayAdapter;
import android.widget.Button;
import android.widget.EditText;
import android.widget.ImageView;
import android.widget.SeekBar;
import android.widget.Spinner;
import android.widget.TextView;
import android.widget.Toast;
import androidx.activity.result.ActivityResultLauncher;
import androidx.activity.result.contract.ActivityResultContracts;
import androidx.appcompat.app.AppCompatActivity;

import com.google.android.material.floatingactionbutton.FloatingActionButton;
import com.google.firebase.firestore.FirebaseFirestore;
import com.google.firebase.storage.FirebaseStorage;
import com.google.firebase.storage.StorageReference;

import java.util.HashMap;
import java.util.Map;

public class DesignStudioActivity extends AppCompatActivity {

    private SeekBar textSizeSlider, imageSizeSlider;
    private EditText customText;
    private ImageView customImage;
    private Button btnAddToCart, btnUploadDesign, btnAddText;
    private FloatingActionButton fabChat;

    // NEW: Printer Variables
    private Spinner spinnerStudioPrinter;
    private TextView tvStudioWarning;

    private TextView tvProductPrice;
    private double currentPrice = 0.00;

    private String currentProductType = "T-Shirt";

    private FirebaseFirestore db;
    private FirebaseStorage storage;
    private Uri selectedImageUri;

    private float dX, dY;

    private final ActivityResultLauncher<String> galleryLauncher = registerForActivityResult(
            new ActivityResultContracts.GetContent(),
            uri -> {
                if (uri != null) {
                    selectedImageUri = uri;
                    customImage.setImageURI(uri);
                }
            }
    );

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_design_studio);

        db = FirebaseFirestore.getInstance();
        storage = FirebaseStorage.getInstance();

        textSizeSlider = findViewById(R.id.textSizeSlider);
        imageSizeSlider = findViewById(R.id.imageSizeSlider);
        customText = findViewById(R.id.tv_custom_text);
        customImage = findViewById(R.id.iv_custom_design);
        btnAddToCart = findViewById(R.id.btn_add_to_cart);
        btnUploadDesign = findViewById(R.id.btn_upload_design);
        btnAddText = findViewById(R.id.btn_add_text);
        fabChat = findViewById(R.id.fab_chat);

        spinnerStudioPrinter = findViewById(R.id.spinner_studio_printer);
        tvStudioWarning = findViewById(R.id.tv_studio_printer_warning);
        tvProductPrice = findViewById(R.id.tv_product_price);

        TextView tvTitle = findViewById(R.id.tv_title);
        ImageView ivBaseProduct = findViewById(R.id.iv_tshirt_base);

        if (getIntent().hasExtra("PRODUCT_TYPE")) {
            currentProductType = getIntent().getStringExtra("PRODUCT_TYPE");
        }

        switch (currentProductType) {
            case "Mug":
                tvTitle.setText("Design Your Mug");
                ivBaseProduct.setImageResource(R.drawable.blank_mug);
                currentPrice = 12.00;
                break;
            case "Water Bottle":
                tvTitle.setText("Design Your Water Bottle");
                ivBaseProduct.setImageResource(R.drawable.blank_bottle);
                currentPrice = 18.00;
                break;
            case "Phone Case":
                tvTitle.setText("Design Your Phone Case");
                ivBaseProduct.setImageResource(R.drawable.blank_phone);
                currentPrice = 15.00;
                break;
            case "Pillow":
                tvTitle.setText("Design Your Pillow");
                ivBaseProduct.setImageResource(R.drawable.blank_pillow);
                currentPrice = 22.00;
                break;
            case "Poster":
                tvTitle.setText("Design Your Poster");
                ivBaseProduct.setImageResource(R.drawable.blank_poster);
                currentPrice = 10.00;
                break;
            default:
                tvTitle.setText("Design Your T-Shirt");
                ivBaseProduct.setImageResource(R.drawable.blank_tshirt);
                currentPrice = 25.00;
                break;
        }

        tvProductPrice.setText(String.format("Total: $%.2f", currentPrice));

        String[] printerTypes = {"Direct to Garment (DTG)", "Transfer Printing", "Laser Engraving"};
        ArrayAdapter<String> adapter = new ArrayAdapter<>(this, android.R.layout.simple_spinner_dropdown_item, printerTypes);
        spinnerStudioPrinter.setAdapter(adapter);

        spinnerStudioPrinter.setOnItemSelectedListener(new AdapterView.OnItemSelectedListener() {
            @Override
            public void onItemSelected(AdapterView<?> parent, View view, int position, long id) {
                String selected = printerTypes[position];
                if (selected.equals("Direct to Garment (DTG)")) {
                    tvStudioWarning.setText("Note: Not suitable for synthetic materials or polyester.");
                } else if (selected.equals("Transfer Printing")) {
                    tvStudioWarning.setText("Note: Not suitable for uneven shapes or melting plastic.");
                } else if (selected.equals("Laser Engraving")) {
                    tvStudioWarning.setText("Note: No color output. Not suitable for fabric and paper.");
                }
            }

            @Override
            public void onNothingSelected(AdapterView<?> parent) {
                tvStudioWarning.setText("");
            }
        });

        setupDraggable(customText);
        setupDraggable(customImage);

        textSizeSlider.setOnSeekBarChangeListener(new SeekBar.OnSeekBarChangeListener() {
            @Override
            public void onProgressChanged(SeekBar seekBar, int progress, boolean fromUser) {
                customText.setTextSize(TypedValue.COMPLEX_UNIT_SP, Math.max(progress, 10));
            }
            @Override public void onStartTrackingTouch(SeekBar seekBar) {}
            @Override public void onStopTrackingTouch(SeekBar seekBar) {}
        });

        imageSizeSlider.setOnSeekBarChangeListener(new SeekBar.OnSeekBarChangeListener() {
            @Override
            public void onProgressChanged(SeekBar seekBar, int progress, boolean fromUser) {
                float scale = Math.max(progress / 100f, 0.1f);
                customImage.setScaleX(scale);
                customImage.setScaleY(scale);
            }
            @Override public void onStartTrackingTouch(SeekBar seekBar) {}
            @Override public void onStopTrackingTouch(SeekBar seekBar) {}
        });

        btnUploadDesign.setOnClickListener(v -> galleryLauncher.launch("image/*"));

        btnAddText.setOnClickListener(v -> {
            customText.requestFocus();
            InputMethodManager imm = (InputMethodManager) getSystemService(Context.INPUT_METHOD_SERVICE);
            if (imm != null) {
                imm.showSoftInput(customText, InputMethodManager.SHOW_IMPLICIT);
            }
        });

        btnAddToCart.setOnClickListener(v -> uploadImageAndSaveData());

        fabChat.setOnClickListener(v -> {
            Intent intent = new Intent(DesignStudioActivity.this, ChatActivity.class);
            startActivity(intent);
        });
    }

    @SuppressLint("ClickableViewAccessibility")
    private void setupDraggable(View view) {
        view.setOnTouchListener((v, event) -> {
            View container = findViewById(R.id.shirt_container);

            switch (event.getAction()) {
                case MotionEvent.ACTION_DOWN:
                    dX = v.getX() - event.getRawX();
                    dY = v.getY() - event.getRawY();
                    break;
                case MotionEvent.ACTION_MOVE:
                    float newX = event.getRawX() + dX;
                    float newY = event.getRawY() + dY;

                    float minX = 20;
                    float maxX = container.getWidth() - v.getWidth() - 20;
                    float minY = 20;
                    float maxY = container.getHeight() - v.getHeight() - 20;

                    if (newX < minX) newX = minX;
                    if (newX > maxX) newX = maxX;
                    if (newY < minY) newY = minY;
                    if (newY > maxY) newY = maxY;

                    v.animate()
                            .x(newX)
                            .y(newY)
                            .setDuration(0)
                            .start();
                    break;
                default:
                    return false;
            }
            return true;
        });
    }

    private void uploadImageAndSaveData() {
        Toast.makeText(this, "Adding to cart...", Toast.LENGTH_SHORT).show();
        if (selectedImageUri != null) {
            StorageReference fileRef = storage.getReference().child("cart/design_" + System.currentTimeMillis() + ".jpg");
            fileRef.putFile(selectedImageUri).addOnSuccessListener(taskSnapshot -> {
                fileRef.getDownloadUrl().addOnSuccessListener(uri -> {
                    saveFinalDataToFirestore(uri.toString());
                });
            }).addOnFailureListener(e -> Toast.makeText(this, "Image Upload Failed", Toast.LENGTH_SHORT).show());
        } else {
            saveFinalDataToFirestore(null);
        }
    }

    private void saveFinalDataToFirestore(String imageUrl) {

        String chosenPrinter = spinnerStudioPrinter.getSelectedItem().toString();

        Map<String, Object> cartItem = new HashMap<>();
        cartItem.put("text", customText.getText().toString());
        cartItem.put("textSize", textSizeSlider.getProgress());
        cartItem.put("imageScale", imageSizeSlider.getProgress());
        cartItem.put("textX", customText.getX());
        cartItem.put("textY", customText.getY());
        cartItem.put("imageX", customImage.getX());
        cartItem.put("imageY", customImage.getY());

        cartItem.put("itemType", currentProductType);
        cartItem.put("price", currentPrice);

        cartItem.put("printingMethod", chosenPrinter);

        if (imageUrl != null) cartItem.put("imageUrl", imageUrl);

        db.collection("cart_items")
                .add(cartItem)
                .addOnSuccessListener(documentReference -> {
                    Toast.makeText(this, "Added to Cart!", Toast.LENGTH_LONG).show();
                })
                .addOnFailureListener(e -> Toast.makeText(this, "Failed to add to cart", Toast.LENGTH_SHORT).show());
    }
}