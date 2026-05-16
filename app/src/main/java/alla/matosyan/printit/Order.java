package alla.matosyan.printit;

public class Order {
    private String orderId;
    private String status;
    private String address;

    public Order(String orderId, String status, String address) {
        this.orderId = orderId;
        this.status = status;
        this.address = address;
    }

    public String getOrderId() { return orderId; }
    public String getStatus() { return status; }
    public String getAddress() { return address; }
}