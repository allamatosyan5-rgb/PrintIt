package alla.matosyan.printit;

public class Order {
    private String orderId;
    private String status;
    private String address;
    private String imageUrl;

    public Order(String orderId, String status, String address, String imageUrl) {
        this.orderId = orderId;
        this.status = status;
        this.address = address;
        this.imageUrl = imageUrl;
    }

    public String getOrderId() { return orderId; }
    public String getStatus() { return status; }
    public String getAddress() { return address; }
    public String getImageUrl() { return imageUrl; }
}