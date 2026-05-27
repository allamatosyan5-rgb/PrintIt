package alla.matosyan.printit;

import android.graphics.Bitmap;
import android.net.Uri;

public class CartItem {
    private String id;
    private String name;
    private Bitmap image;
    private double price;

    // Employee Production Assets (Raw Files)
    private String addedText;
    private Uri rawImageUri;

    // Employee Production Math (The Parameters)
    private String textColor;
    private float textSize;
    private String textFont;
    private String textType; // <--- Added Text Type
    private int imageSize;

    public CartItem(String name, Bitmap image, double price) {
        this.name = name;
        this.image = image;
        this.price = price;
    }

    public String getId() { return id; }
    public void setId(String id) { this.id = id; }

    public String getName() { return name; }
    public Bitmap getImage() { return image; }
    public double getPrice() { return price; }

    public String getAddedText() { return addedText; }
    public void setAddedText(String addedText) { this.addedText = addedText; }

    public Uri getRawImageUri() { return rawImageUri; }
    public void setRawImageUri(Uri rawImageUri) { this.rawImageUri = rawImageUri; }

    public String getTextColor() { return textColor; }
    public void setTextColor(String textColor) { this.textColor = textColor; }

    public float getTextSize() { return textSize; }
    public void setTextSize(float textSize) { this.textSize = textSize; }

    public String getTextFont() { return textFont; }
    public void setTextFont(String textFont) { this.textFont = textFont; }

    public String getTextType() { return textType; }
    public void setTextType(String textType) { this.textType = textType; }

    public int getImageSize() { return imageSize; }
    public void setImageSize(int imageSize) { this.imageSize = imageSize; }
}