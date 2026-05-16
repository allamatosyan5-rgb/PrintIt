package alla.matosyan.printit;

import android.graphics.Bitmap;

public class CartItem {
    private String name;
    private Bitmap image;
    private double price;

    public CartItem(String name, Bitmap image, double price) {
        this.name = name;
        this.image = image;
        this.price = price;
    }

    public String getName() {
        return name;
    }

    public Bitmap getImage() {
        return image;
    }

    public double getPrice() {
        return price;
    }
}