package alla.matosyan.printit;

import android.Manifest;
import android.content.pm.PackageManager;
import android.graphics.Bitmap;
import android.graphics.Canvas;
import android.graphics.Typeface;
import android.net.Uri;
import android.os.Bundle;
import android.text.SpannableString;
import android.text.SpannableStringBuilder;
import android.text.Spanned;
import android.text.style.ForegroundColorSpan;
import android.text.style.RelativeSizeSpan;
import android.text.style.StyleSpan;
import android.view.View;
import android.widget.Button;
import android.widget.EditText;
import android.widget.ImageView;
import android.widget.RelativeLayout;
import android.widget.FrameLayout;
import android.widget.TextView;
import android.widget.Toast;
import androidx.activity.result.ActivityResultLauncher;
import androidx.activity.result.contract.ActivityResultContracts;
import androidx.appcompat.app.AlertDialog;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.content.ContextCompat;
import androidx.core.content.FileProvider;
import java.io.File;
import java.io.InputStream;

public class DesignStudioActivity extends AppCompatActivity {

    private Button btnUploadDesign, btnAddText, btnAddToCart;
    private TextView tvDesignTitle;
    private RelativeLayout designCanvas;
    private FrameLayout safeZone;
    private ImageView ivProductBackground;
    private String selectedProduct;
    private String selectedPrintType = "";
    private Uri currentPhotoUri;

    private final ActivityResultLauncher<String> galleryLauncher = registerForActivityResult(
            new ActivityResultContracts.GetContent(),
            uri -> { if (uri != null) processImageUpload(uri, null); }
    );

    private final ActivityResultLauncher<Uri> cameraLauncher = registerForActivityResult(
            new ActivityResultContracts.TakePicture(),
            success -> {
                if (success && currentPhotoUri != null) {
                    processImageUpload(currentPhotoUri, null);
                }
            }
    );

    private final ActivityResultLauncher<String> permissionLauncher = registerForActivityResult(
            new ActivityResultContracts.RequestPermission(),
            isGranted -> {
                if (isGranted) {
                    currentPhotoUri = createImageFileUri();
                    if (currentPhotoUri != null) {
                        cameraLauncher.launch(currentPhotoUri);
                    }
                } else {
                    Toast.makeText(this, "Camera permission is required to take a photo.", Toast.LENGTH_SHORT).show();
                }
            }
    );

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setRequestedOrientation(android.content.pm.ActivityInfo.SCREEN_ORIENTATION_PORTRAIT);
        setContentView(R.layout.activity_design_studio);

        btnUploadDesign = findViewById(R.id.btnUploadDesign);
        btnAddText = findViewById(R.id.btnAddText);
        btnAddToCart = findViewById(R.id.btnAddToCart);
        tvDesignTitle = findViewById(R.id.tvDesignTitle);
        designCanvas = findViewById(R.id.designCanvas);
        safeZone = findViewById(R.id.safeZone);
        ivProductBackground = findViewById(R.id.ivProductBackground);

        selectedProduct = getIntent().getStringExtra("PRODUCT_TYPE");

        if (selectedProduct != null) {
            selectedPrintType = getDefaultPrintType(selectedProduct);
            updateTitleDisplay();
            tvDesignTitle.setTextAlignment(View.TEXT_ALIGNMENT_CENTER);
            tvDesignTitle.setOnClickListener(v -> showPrintTypeDialog());

            RelativeLayout.LayoutParams safeParams = new RelativeLayout.LayoutParams(
                    RelativeLayout.LayoutParams.WRAP_CONTENT,
                    RelativeLayout.LayoutParams.WRAP_CONTENT
            );
            safeParams.addRule(RelativeLayout.CENTER_IN_PARENT, RelativeLayout.TRUE);

            if (selectedProduct.equalsIgnoreCase("Mug")) {
                ivProductBackground.setImageResource(R.drawable.blank_mug);
                safeParams.removeRule(RelativeLayout.CENTER_IN_PARENT);
                safeParams.removeRule(RelativeLayout.CENTER_HORIZONTAL);
                safeParams.width = dpToPx(260);
                safeParams.topMargin = dpToPx(150);
                safeParams.height = dpToPx(270);
                safeParams.setMarginStart(dpToPx(196));
            } else if (selectedProduct.equalsIgnoreCase("T-Shirt")) {
                ivProductBackground.setImageResource(R.drawable.blank_tshirt);
                safeParams.removeRule(RelativeLayout.CENTER_IN_PARENT);
                safeParams.addRule(RelativeLayout.CENTER_HORIZONTAL, RelativeLayout.TRUE);
                safeParams.width = dpToPx(160);
                safeParams.height = dpToPx(287);
                safeParams.topMargin = dpToPx(150);
            } else if (selectedProduct.equalsIgnoreCase("Phone Case")) {
                ivProductBackground.setImageResource(R.drawable.blank_phone);
                safeParams.removeRule(RelativeLayout.CENTER_IN_PARENT);
                safeParams.addRule(RelativeLayout.CENTER_HORIZONTAL, RelativeLayout.TRUE);
                safeParams.width = dpToPx(205);
                safeParams.topMargin = dpToPx(175);
                safeParams.height = dpToPx(270);
            } else if (selectedProduct.equalsIgnoreCase("Water Bottle")) {
                ivProductBackground.setImageResource(R.drawable.blank_bottle);
                safeParams.removeRule(RelativeLayout.CENTER_IN_PARENT);
                safeParams.addRule(RelativeLayout.CENTER_HORIZONTAL, RelativeLayout.TRUE);
                safeParams.width = dpToPx(140);
                safeParams.topMargin = dpToPx(163);
                safeParams.height = dpToPx(300);
            } else if (selectedProduct.equalsIgnoreCase("Pillow")) {
                ivProductBackground.setImageResource(R.drawable.blank_pillow);
                safeParams.removeRule(RelativeLayout.CENTER_IN_PARENT);
                safeParams.addRule(RelativeLayout.CENTER_HORIZONTAL, RelativeLayout.TRUE);
                safeParams.width = dpToPx(200);
                safeParams.topMargin = dpToPx(160);
                safeParams.height = dpToPx(220);
            } else if (selectedProduct.equalsIgnoreCase("Poster")) {
                ivProductBackground.setImageResource(R.drawable.blank_poster);
                safeParams.removeRule(RelativeLayout.CENTER_IN_PARENT);
                safeParams.addRule(RelativeLayout.CENTER_HORIZONTAL, RelativeLayout.TRUE);
                safeParams.width = dpToPx(270);
                safeParams.topMargin = dpToPx(80);
                safeParams.height = dpToPx(400);
            } else {
                ivProductBackground.setBackgroundColor(android.graphics.Color.LTGRAY);
            }

            safeZone.setLayoutParams(safeParams);
        } else {
            tvDesignTitle.setText("Design Studio");
            ivProductBackground.setBackgroundColor(android.graphics.Color.LTGRAY);
        }

        btnUploadDesign.setOnClickListener(v -> showImageSourceDialog());
        btnAddText.setOnClickListener(v -> showAddTextDialog());

        btnAddToCart.setOnClickListener(v -> {
            Bitmap finalProductImage = captureDesign(designCanvas);
            String finalItemName = (selectedProduct != null ? selectedProduct : "Item") + " (" + selectedPrintType + ")";

            double basePrice = 0.0;
            if (selectedProduct != null) {
                if (selectedProduct.equalsIgnoreCase("T-Shirt") || selectedProduct.equalsIgnoreCase("T-Shirts")) basePrice = 18.00;
                else if (selectedProduct.equalsIgnoreCase("Mug")) basePrice = 12.00;
                else if (selectedProduct.equalsIgnoreCase("Bottle") || selectedProduct.equalsIgnoreCase("Bottles") || selectedProduct.equalsIgnoreCase("Water Bottle")) basePrice = 15.00;
                else if (selectedProduct.equalsIgnoreCase("Phone Case") || selectedProduct.equalsIgnoreCase("Phone Cases")) basePrice = 10.00;
                else if (selectedProduct.equalsIgnoreCase("Pillow") || selectedProduct.equalsIgnoreCase("Pillows")) basePrice = 20.00;
                else if (selectedProduct.equalsIgnoreCase("Poster") || selectedProduct.equalsIgnoreCase("Posters")) basePrice = 8.00;
            }

            double printMarkup = 0.0;
            if (selectedPrintType.equals("Laser Engraving")) printMarkup = 4.00;
            else if (selectedPrintType.equals("DTG")) printMarkup = 2.50;

            double finalPrice = basePrice + printMarkup;

            CartManager.cartList.add(new CartItem(finalItemName, finalProductImage, finalPrice));

            CartManager.saveCart(this);

            Toast.makeText(this, "Added to Cart! ($" + String.format("%.2f", finalPrice) + ")", Toast.LENGTH_SHORT).show();
            finish();
        });
    }

    private String getDefaultPrintType(String product) {
        if (product == null) return "Standard Print";

        String p = product.toLowerCase().trim();

        switch (p) {
            case "t-shirt":
            case "pillow":
                return "DTG";

            case "mug":
            case "phone case":
            case "poster":
                return "Transfer Printing";


            case "water bottle":
                return "Laser Engraving";

            default:
                return "Standard Print";
        }
    }

    private void showPrintTypeDialog() {
        String[] options;
        if (selectedProduct.equalsIgnoreCase("T-Shirt")) {
            options = new String[]{"DTG", "Transfer Printing"};
        } else if (selectedProduct.equalsIgnoreCase("Mug")) {
            options = new String[]{"Transfer Printing", "Laser Engraving"};
        } else if (selectedProduct.equalsIgnoreCase("Water Bottle")) {
            options = new String[]{"Laser Engraving", "Transfer Printing"};
        } else if (selectedProduct.equalsIgnoreCase("Phone Case")) {
            options = new String[]{"Transfer Printing", "Laser Engraving"};
        } else if (selectedProduct.equalsIgnoreCase("Pillow")) {
            options = new String[]{"DTG", "Transfer Printing"};
        } else if (selectedProduct.equalsIgnoreCase("Poster")) {
            options = new String[]{"Transfer Printing", "DTG"};
        } else {
            options = new String[]{"Standard Print"};
        }

        new AlertDialog.Builder(this)
                .setTitle("Choose Print Technology")
                .setItems(options, (dialog, which) -> {
                    selectedPrintType = options[which];
                    updateTitleDisplay();
                    Toast.makeText(this, "Switched to " + selectedPrintType, Toast.LENGTH_SHORT).show();
                }).show();
    }

    private void updateTitleDisplay() {
        String size = "";
        if (selectedProduct.equalsIgnoreCase("Mug")) size = "16cm (H) x 8cm (Ø)";
        else if (selectedProduct.equalsIgnoreCase("T-Shirt") || selectedProduct.equalsIgnoreCase("T-Shirts")) size = "60cm (H) x 45cm (W)";
        else if (selectedProduct.equalsIgnoreCase("Phone Case") || selectedProduct.equalsIgnoreCase("Phone Cases")) size = "14cm x 8cm";
        else if (selectedProduct.equalsIgnoreCase("Bottle") || selectedProduct.equalsIgnoreCase("Bottles") || selectedProduct.equalsIgnoreCase("Water Bottle")) size = "20cm (H) x 6cm (Ø)";
        else if (selectedProduct.equalsIgnoreCase("Pillow") || selectedProduct.equalsIgnoreCase("Pillows")) size = "40cm x 40cm";
        else if (selectedProduct.equalsIgnoreCase("Poster") || selectedProduct.equalsIgnoreCase("Posters")) size = "70cm x 50cm";

        String line1 = "Designing: " + selectedProduct + "\n";
        String line2 = "Size: " + size + " | Print: " + selectedPrintType + "\n";
        String line3 = "✏️ Tap here to change print method";

        SpannableStringBuilder builder = new SpannableStringBuilder();

        SpannableString sp1 = new SpannableString(line1);
        sp1.setSpan(new StyleSpan(Typeface.BOLD), 0, line1.length(), Spanned.SPAN_EXCLUSIVE_EXCLUSIVE);
        builder.append(sp1);

        SpannableString sp2 = new SpannableString(line2);
        sp2.setSpan(new RelativeSizeSpan(0.8f), 0, line2.length(), Spanned.SPAN_EXCLUSIVE_EXCLUSIVE);
        builder.append(sp2);

        SpannableString sp3 = new SpannableString(line3);
        sp3.setSpan(new ForegroundColorSpan(android.graphics.Color.parseColor("#0066CC")), 0, line3.length(), Spanned.SPAN_EXCLUSIVE_EXCLUSIVE);
        sp3.setSpan(new StyleSpan(Typeface.BOLD), 0, line3.length(), Spanned.SPAN_EXCLUSIVE_EXCLUSIVE);
        builder.append(sp3);

        tvDesignTitle.setText(builder);
        tvDesignTitle.setTextSize(16f);
    }

    private void showImageSourceDialog() {
        String[] options = {"Take Photo", "Choose from Gallery"};
        new AlertDialog.Builder(this)
                .setTitle("Upload Image")
                .setItems(options, (dialog, which) -> {
                    if (which == 0) {
                        if (ContextCompat.checkSelfPermission(this, Manifest.permission.CAMERA) == PackageManager.PERMISSION_GRANTED) {
                            currentPhotoUri = createImageFileUri();
                            if (currentPhotoUri != null) {
                                cameraLauncher.launch(currentPhotoUri);
                            }
                        } else {
                            permissionLauncher.launch(Manifest.permission.CAMERA);
                        }
                    } else {
                        galleryLauncher.launch("image/*");
                    }
                }).show();
    }

    private Uri createImageFileUri() {
        try {
            File imagePath = new File(getCacheDir(), "images");
            imagePath.mkdirs();
            File newFile = new File(imagePath, "camera_image_" + System.currentTimeMillis() + ".jpg");
            return FileProvider.getUriForFile(this, getPackageName() + ".fileprovider", newFile);
        } catch (Exception e) {
            e.printStackTrace();
            return null;
        }
    }

    private void processImageUpload(Uri uri, Bitmap bitmap) {
        int width = 0;
        int height = 0;

        if (bitmap != null) {
            width = bitmap.getWidth();
            height = bitmap.getHeight();
        } else if (uri != null) {
            try {
                InputStream input = getContentResolver().openInputStream(uri);
                android.graphics.BitmapFactory.Options options = new android.graphics.BitmapFactory.Options();
                options.inJustDecodeBounds = true;
                android.graphics.BitmapFactory.decodeStream(input, null, options);
                width = options.outWidth;
                height = options.outHeight;
                if (input != null) input.close();
            } catch (Exception e) {
                width = 1000; height = 1000;
            }
        }

        if (width < 1000 || height < 1000) {
            new AlertDialog.Builder(this)
                    .setTitle("Low Resolution")
                    .setMessage("This image is too small for high-quality printing. Please choose another high-resolution photo.")
                    .setPositiveButton("OK", null)
                    .show();
            return;
        }

        addImageToCanvas(uri, bitmap);
    }

    private void showAddTextDialog() {
        AlertDialog.Builder builder = new AlertDialog.Builder(this);
        builder.setTitle("Enter your text");
        final EditText input = new EditText(this);
        builder.setView(input);
        builder.setPositiveButton("Add", (dialog, which) -> {
            String userText = input.getText().toString();
            if (!userText.isEmpty()) addTextToCanvas(userText);
        });
        builder.setNegativeButton("Cancel", (dialog, which) -> dialog.cancel());
        builder.show();
    }

    private void addImageToCanvas(Uri uri, Bitmap bitmap) {
        ImageView newImage = new ImageView(this);
        newImage.setScaleType(ImageView.ScaleType.FIT_CENTER);
        newImage.setAdjustViewBounds(true);

        RelativeLayout.LayoutParams params = new RelativeLayout.LayoutParams(dpToPx(100), dpToPx(100));
        params.addRule(RelativeLayout.CENTER_IN_PARENT, RelativeLayout.TRUE);
        newImage.setLayoutParams(params);

        if (uri != null) newImage.setImageURI(uri);
        if (bitmap != null) newImage.setImageBitmap(bitmap);

        newImage.setOnTouchListener(new DragAndScaleListener(this));

        newImage.setOnClickListener(v -> {
            String[] options = {"Change Size", "Delete Image"};
            new AlertDialog.Builder(this)
                    .setTitle("Edit Image")
                    .setItems(options, (dialog, which) -> {
                        if (which == 0) {
                            showSizeSliderDialog(newImage, true);
                        } else if (which == 1) {
                            safeZone.removeView(newImage);
                        }
                    }).show();
        });

        safeZone.addView(newImage);
    }

    private void addTextToCanvas(String text) {
        TextView newText = new TextView(this);
        newText.setText(text);

        float startingSize = 32f;
        android.text.TextPaint testPaint = new android.text.TextPaint();
        testPaint.setTypeface(Typeface.DEFAULT_BOLD);
        testPaint.setTextSize(startingSize * getResources().getDisplayMetrics().scaledDensity);

        int maxAllowedWidth = safeZone.getWidth() - dpToPx(20);
        if (maxAllowedWidth <= 0) maxAllowedWidth = dpToPx(150);
        while (testPaint.measureText(text) > maxAllowedWidth && startingSize > 12f) {
            startingSize -= 1f;
            testPaint.setTextSize(startingSize * getResources().getDisplayMetrics().scaledDensity);
        }

        newText.setTextSize(startingSize);

        newText.setTextColor(ContextCompat.getColor(this, R.color.caicon_blue_primary));
        newText.setTypeface(null, Typeface.BOLD);
        newText.setSingleLine(true);
        newText.setMaxLines(1);
        newText.setPadding(dpToPx(10), dpToPx(10), dpToPx(10), dpToPx(10));

        RelativeLayout.LayoutParams params = new RelativeLayout.LayoutParams(
                RelativeLayout.LayoutParams.WRAP_CONTENT,
                RelativeLayout.LayoutParams.WRAP_CONTENT);
        params.addRule(RelativeLayout.CENTER_IN_PARENT, RelativeLayout.TRUE);
        newText.setLayoutParams(params);

        newText.setOnTouchListener(new DragAndScaleListener(this));

        newText.setOnClickListener(v -> {
            String[] options = {"Change Size", "Change Color", "Change Font", "Delete Text"};
            new AlertDialog.Builder(this)
                    .setTitle("Edit Text")
                    .setItems(options, (dialog, which) -> {
                        if (which == 0) {
                            showSizeSliderDialog(newText, false);
                        } else if (which == 1) {
                            showColorPickerDialog(newText);
                        } else if (which == 2) {
                            showFontPickerDialog(newText);
                        } else if (which == 3) {
                            safeZone.removeView(newText);
                        }
                    }).show();
        });

        safeZone.addView(newText);
    }
    private void showSizeSliderDialog(View targetView, boolean isImage) {
        AlertDialog.Builder builder = new AlertDialog.Builder(this);
        builder.setTitle("Adjust Size");

        android.widget.LinearLayout layout = new android.widget.LinearLayout(this);
        layout.setOrientation(android.widget.LinearLayout.VERTICAL);
        layout.setPadding(60, 50, 60, 50);

        android.widget.SeekBar seekBar = new android.widget.SeekBar(this);
        seekBar.setMax(100);

        if (isImage) {
            ImageView iv = (ImageView) targetView;
            int currentWidth = iv.getLayoutParams().width;

            int maxWidth = Math.min(safeZone.getWidth(), safeZone.getHeight());
            int minWidth = dpToPx(40);

            int progress = (int) (((float)(currentWidth - minWidth) / (maxWidth - minWidth)) * 100);
            seekBar.setProgress(Math.max(0, Math.min(100, progress)));

            seekBar.setOnSeekBarChangeListener(new android.widget.SeekBar.OnSeekBarChangeListener() {
                @Override
                public void onProgressChanged(android.widget.SeekBar seekBar, int progress, boolean fromUser) {
                    int newSize = minWidth + (int) (((float)progress / 100) * (maxWidth - minWidth));
                    iv.getLayoutParams().width = newSize;
                    iv.getLayoutParams().height = newSize;
                    iv.requestLayout();

                    iv.post(() -> {
                        float maxAllowedX = safeZone.getWidth() - iv.getWidth();
                        float maxAllowedY = safeZone.getHeight() - iv.getHeight();

                        if (iv.getX() > maxAllowedX) iv.setX(Math.max(0, maxAllowedX));
                        if (iv.getY() > maxAllowedY) iv.setY(Math.max(0, maxAllowedY));
                    });
                }
                @Override public void onStartTrackingTouch(android.widget.SeekBar seekBar) {}
                @Override public void onStopTrackingTouch(android.widget.SeekBar seekBar) {}
            });
        } else {
            TextView tv = (TextView) targetView;
            float currentSize = tv.getTextSize();
            float minSize = dpToPx(12);

            android.text.TextPaint testPaint = new android.text.TextPaint(tv.getPaint());
            float calculatedMaxSize = minSize;

            float maxAllowedWidth = safeZone.getWidth() - dpToPx(30);

            testPaint.setTextSize(calculatedMaxSize);
            while (testPaint.measureText(tv.getText().toString()) < maxAllowedWidth && calculatedMaxSize < dpToPx(150)) {
                calculatedMaxSize += 2f;
                testPaint.setTextSize(calculatedMaxSize);
            }

            float maxSize = calculatedMaxSize;

            int progress = (int) (((currentSize - minSize) / (maxSize - minSize)) * 100);
            seekBar.setProgress(Math.max(0, Math.min(100, progress)));

            seekBar.setOnSeekBarChangeListener(new android.widget.SeekBar.OnSeekBarChangeListener() {
                @Override
                public void onProgressChanged(android.widget.SeekBar seekBar, int progress, boolean fromUser) {
                    float newSize = minSize + (((float)progress / 100) * (maxSize - minSize));
                    tv.setTextSize(android.util.TypedValue.COMPLEX_UNIT_PX, newSize);

                    tv.post(() -> {
                        float maxAllowedX = safeZone.getWidth() - tv.getWidth();
                        float maxAllowedY = safeZone.getHeight() - tv.getHeight();

                        if (tv.getX() > maxAllowedX) tv.setX(Math.max(0, maxAllowedX));
                        if (tv.getY() > maxAllowedY) tv.setY(Math.max(0, maxAllowedY));
                    });
                }
                @Override public void onStartTrackingTouch(android.widget.SeekBar seekBar) {}
                @Override public void onStopTrackingTouch(android.widget.SeekBar seekBar) {}
            });
        }

        layout.addView(seekBar);
        builder.setView(layout);
        builder.setPositiveButton("Done", null);
        builder.show();
    }

    private void showColorPickerDialog(TextView targetText) {
        AlertDialog.Builder builder = new AlertDialog.Builder(this);
        builder.setTitle("Choose Text Color");

        android.widget.HorizontalScrollView scrollView = new android.widget.HorizontalScrollView(this);
        scrollView.setHorizontalScrollBarEnabled(false);

        android.widget.LinearLayout colorContainer = new android.widget.LinearLayout(this);
        colorContainer.setOrientation(android.widget.LinearLayout.HORIZONTAL);
        colorContainer.setPadding(60, 50, 60, 50);

        String[] hexColors = {"#000000", "#FFFFFF", "#1976D2", "#D32F2F", "#388E3C", "#FBC02D", "#8E24AA", "#FF9800", "#757575"};

        for (String hex : hexColors) {
            View colorButton = new View(this);
            android.widget.LinearLayout.LayoutParams params = new android.widget.LinearLayout.LayoutParams(dpToPx(45), dpToPx(45));
            params.setMargins(0, 0, dpToPx(20), 0);
            colorButton.setLayoutParams(params);

            android.graphics.drawable.GradientDrawable shape = new android.graphics.drawable.GradientDrawable();
            shape.setShape(android.graphics.drawable.GradientDrawable.OVAL);
            shape.setColor(android.graphics.Color.parseColor(hex));
            shape.setStroke(dpToPx(2), android.graphics.Color.parseColor("#E0E0E0"));
            colorButton.setBackground(shape);

            colorButton.setOnClickListener(v -> {
                targetText.setTextColor(android.graphics.Color.parseColor(hex));
                Toast.makeText(this, "Color applied!", Toast.LENGTH_SHORT).show();
            });

            colorContainer.addView(colorButton);
        }

        scrollView.addView(colorContainer);
        builder.setView(scrollView);
        builder.setPositiveButton("Done", null);
        builder.show();
    }

    private void showFontPickerDialog(TextView targetText) {
        String[] fonts = {"Standard Bold", "Serif", "Monospace", "Cursive"};
        Typeface[] typefaces = {
                Typeface.defaultFromStyle(Typeface.BOLD),
                Typeface.SERIF,
                Typeface.MONOSPACE,
                Typeface.create("cursive", Typeface.NORMAL)
        };

        new AlertDialog.Builder(this)
                .setTitle("Choose Font")
                .setItems(fonts, (dialog, which) -> {
                    targetText.setTypeface(typefaces[which]);
                }).show();
    }

    private Bitmap captureDesign(View view) {
        Bitmap bitmap = Bitmap.createBitmap(view.getWidth(), view.getHeight(), Bitmap.Config.ARGB_8888);
        Canvas canvas = new Canvas(bitmap);
        view.draw(canvas);
        return bitmap;
    }

    private int dpToPx(int dp) {
        float density = getResources().getDisplayMetrics().density;
        return Math.round((float) dp * density);
    }
}