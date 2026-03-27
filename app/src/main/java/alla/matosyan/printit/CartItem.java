package alla.matosyan.printit;

public class CartItem {

    private String documentId;

    private String itemType;
    private String text;
    private int textSize;
    private int imageScale;
    private float textX;
    private float textY;
    private float imageX;
    private float imageY;
    private String imageUrl;


    private double price;

    public CartItem() {}

    public String getDocumentId() { return documentId; }
    public void setDocumentId(String documentId) { this.documentId = documentId; }

    public String getItemType() { return itemType; }
    public void setItemType(String itemType) { this.itemType = itemType; }

    public String getText() { return text; }
    public void setText(String text) { this.text = text; }

    public int getTextSize() { return textSize; }
    public void setTextSize(int textSize) { this.textSize = textSize; }

    public int getImageScale() { return imageScale; }
    public void setImageScale(int imageScale) { this.imageScale = imageScale; }

    public float getTextX() { return textX; }
    public void setTextX(float textX) { this.textX = textX; }

    public float getTextY() { return textY; }
    public void setTextY(float textY) { this.textY = textY; }

    public float getImageX() { return imageX; }
    public void setImageX(float imageX) { this.imageX = imageX; }

    public float getImageY() { return imageY; }
    public void setImageY(float imageY) { this.imageY = imageY; }

    public String getImageUrl() { return imageUrl; }
    public void setImageUrl(String imageUrl) { this.imageUrl = imageUrl; }

    public double getPrice() { return price; }
    public void setPrice(double price) { this.price = price; }
}