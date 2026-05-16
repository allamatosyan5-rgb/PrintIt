package alla.matosyan.printit;

import android.content.Context;
import android.content.SharedPreferences;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;

import java.io.File;
import java.io.FileOutputStream;
import java.util.ArrayList;
import java.util.List;

public class CartManager {
    public static List<CartItem> cartList = new ArrayList<>();

    public static void saveCart(Context context) {
        try {
            SharedPreferences prefs = context.getSharedPreferences("PrintItPrefs", Context.MODE_PRIVATE);
            StringBuilder data = new StringBuilder();

            File dir = new File(context.getFilesDir(), "cart_images");
            if (!dir.exists()) dir.mkdirs();

            for (int i = 0; i < cartList.size(); i++) {
                CartItem item = cartList.get(i);
                String fileName = "design_" + System.currentTimeMillis() + "_" + i + ".jpg";
                File file = new File(dir, fileName);

                if (item.getImage() != null) {
                    FileOutputStream fos = new FileOutputStream(file);
                    item.getImage().compress(Bitmap.CompressFormat.JPEG, 80, fos);
                    fos.flush();
                    fos.close();
                }

                data.append(item.getName()).append("||")
                        .append(item.getPrice()).append("||")
                        .append(fileName).append(";;");
            }

            prefs.edit().putString("cart_data", data.toString()).commit();

        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    public static void loadCart(Context context) {
        try {
            cartList.clear();
            SharedPreferences prefs = context.getSharedPreferences("PrintItPrefs", Context.MODE_PRIVATE);
            String data = prefs.getString("cart_data", "");

            if (data.isEmpty()) {
                return;
            }

            File dir = new File(context.getFilesDir(), "cart_images");
            String[] items = data.split(";;");

            for (String itemStr : items) {
                if (itemStr.isEmpty()) continue;
                String[] parts = itemStr.split("\\|\\|");

                if (parts.length >= 3) {
                    String name = parts[0];
                    double price = Double.parseDouble(parts[1]);
                    String fileName = parts[2];

                    File file = new File(dir, fileName);
                    Bitmap bmp = null;
                    if (file.exists()) {
                        bmp = BitmapFactory.decodeFile(file.getAbsolutePath());
                    }

                    cartList.add(new CartItem(name, bmp, price));
                }
            }

        } catch (Exception e) {
            e.printStackTrace();
        }
    }
}