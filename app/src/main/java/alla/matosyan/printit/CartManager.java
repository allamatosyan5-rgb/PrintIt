package alla.matosyan.printit;

import android.content.Context;
import android.graphics.Bitmap;
import android.util.Log;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.firestore.FirebaseFirestore;
import com.google.firebase.storage.FirebaseStorage;
import com.google.firebase.storage.StorageReference;
import java.io.ByteArrayOutputStream;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;

public class CartManager {

    public static List<CartItem> cartList = new ArrayList<>();

    public static void saveItemToFirebase(CartItem item, Runnable onSuccess, Runnable onFailure) {
        FirebaseAuth mAuth = FirebaseAuth.getInstance();
        if (mAuth.getCurrentUser() == null) {
            if (onFailure != null) onFailure.run();
            return;
        }

        String userId = mAuth.getCurrentUser().getUid();
        String uniqueItemId = UUID.randomUUID().toString();

        item.setId(uniqueItemId);

        ByteArrayOutputStream baos = new ByteArrayOutputStream();
        item.getImage().compress(Bitmap.CompressFormat.JPEG, 85, baos);
        byte[] imageData = baos.toByteArray();

        StorageReference mockupRef = FirebaseStorage.getInstance().getReference()
                .child("Users/" + userId + "/CartImages/" + uniqueItemId + ".jpg");

        mockupRef.putBytes(imageData).addOnSuccessListener(taskSnapshot -> {
            mockupRef.getDownloadUrl().addOnSuccessListener(mockupUri -> {
                String mockupUrl = mockupUri.toString();

                if (item.getRawImageUri() != null) {
                    StorageReference rawRef = FirebaseStorage.getInstance().getReference()
                            .child("Users/" + userId + "/RawImages/" + uniqueItemId + "_RAW.jpg");

                    rawRef.putFile(item.getRawImageUri()).addOnSuccessListener(rawTask -> {
                        rawRef.getDownloadUrl().addOnSuccessListener(rawUri -> {
                            saveDataToFirestore(userId, uniqueItemId, item, mockupUrl, rawUri.toString(), onSuccess, onFailure);
                        });
                    }).addOnFailureListener(e -> {
                        Log.e("CartManager", "Failed to upload raw image", e);
                        saveDataToFirestore(userId, uniqueItemId, item, mockupUrl, null, onSuccess, onFailure);
                    });
                } else {
                    saveDataToFirestore(userId, uniqueItemId, item, mockupUrl, null, onSuccess, onFailure);
                }
            }).addOnFailureListener(e -> {
                Log.e("CartManager", "Failed to get mockup URL", e);
                if(onFailure != null) onFailure.run();
            });
        }).addOnFailureListener(e -> {
            Log.e("CartManager", "Failed to upload mockup image", e);
            if(onFailure != null) onFailure.run();
        });
    }

    private static void saveDataToFirestore(String userId, String itemId, CartItem item, String mockupUrl, String rawImageUrl, Runnable onSuccess, Runnable onFailure) {
        Map<String, Object> cartData = new HashMap<>();
        cartData.put("name", item.getName());
        cartData.put("price", item.getPrice());
        cartData.put("imageUrl", mockupUrl);
        cartData.put("timestamp", System.currentTimeMillis());

        cartData.put("rawText", item.getAddedText() != null ? item.getAddedText() : "");
        cartData.put("rawImageUrl", rawImageUrl != null ? rawImageUrl : "");

        cartData.put("textColor", item.getTextColor() != null ? item.getTextColor() : "#1A365D");
        cartData.put("textSize", item.getTextSize());
        cartData.put("textFont", item.getTextFont() != null ? item.getTextFont() : "Standard Bold");
        cartData.put("imageSizeWidth", item.getImageSize());

        FirebaseFirestore.getInstance().collection("Users").document(userId)
                .collection("Cart").document(itemId).set(cartData)
                .addOnSuccessListener(aVoid -> {
                    Log.d("CartManager", "SUCCESS: Item and parameters fully saved to cloud!");
                    if(onSuccess != null) onSuccess.run();
                })
                .addOnFailureListener(e -> {
                    Log.e("CartManager", "CRITICAL ERROR: Firestore rejected the save", e);
                    if(onFailure != null) onFailure.run();
                });
    }

    public static void loadCartFromFirebase(Runnable onComplete) {
        FirebaseAuth mAuth = FirebaseAuth.getInstance();
        if (mAuth.getCurrentUser() == null) {
            onComplete.run();
            return;
        }

        String userId = mAuth.getCurrentUser().getUid();
        FirebaseFirestore.getInstance().collection("Users").document(userId).collection("Cart")
                .get()
                .addOnSuccessListener(queryDocumentSnapshots -> {
                    cartList.clear();
                    if (queryDocumentSnapshots.isEmpty()) {
                        onComplete.run();
                        return;
                    }

                    int totalItems = queryDocumentSnapshots.size();
                    final int[] loadedCount = {0};

                    for (com.google.firebase.firestore.DocumentSnapshot doc : queryDocumentSnapshots) {
                        String name = doc.getString("name");
                        Double price = doc.getDouble("price");
                        String imageUrl = doc.getString("imageUrl");
                        String documentId = doc.getId();
                        String rawText = doc.getString("rawText");

                        if (imageUrl != null) {
                            FirebaseStorage.getInstance().getReferenceFromUrl(imageUrl)
                                    .getBytes(1024 * 1024 * 5).addOnSuccessListener(bytes -> {
                                        android.graphics.Bitmap bmp = android.graphics.BitmapFactory.decodeByteArray(bytes, 0, bytes.length);
                                        CartItem downloadedItem = new CartItem(name, bmp, price != null ? price : 0.0);
                                        downloadedItem.setId(documentId);
                                        downloadedItem.setAddedText(rawText);
                                        cartList.add(downloadedItem);

                                        loadedCount[0]++;
                                        if (loadedCount[0] == totalItems) onComplete.run();
                                    }).addOnFailureListener(e -> {
                                        loadedCount[0]++;
                                        if (loadedCount[0] == totalItems) onComplete.run();
                                    });
                        } else {
                            loadedCount[0]++;
                            if (loadedCount[0] == totalItems) onComplete.run();
                        }
                    }
                }).addOnFailureListener(e -> onComplete.run());
    }

    public static void deleteItemFromFirebase(String itemId) {
        if (itemId == null) return;

        FirebaseAuth mAuth = FirebaseAuth.getInstance();
        if (mAuth.getCurrentUser() != null) {
            String userId = mAuth.getCurrentUser().getUid();

            FirebaseFirestore.getInstance().collection("Users").document(userId)
                    .collection("Cart").document(itemId).delete();

            FirebaseStorage.getInstance().getReference()
                    .child("Users/" + userId + "/CartImages/" + itemId + ".jpg").delete();

            FirebaseStorage.getInstance().getReference()
                    .child("Users/" + userId + "/RawImages/" + itemId + "_RAW.jpg").delete()
                    .addOnFailureListener(e -> { /* Ignored if no raw image existed */ });
        }
    }


}