package alla.matosyan.printit;

public class ChatMessage {
    private String sender;
    private String messageText;
    private long timestamp;

    public ChatMessage() {
    }

    public ChatMessage(String sender, String messageText, long timestamp) {
        this.sender = sender;
        this.messageText = messageText;
        this.timestamp = timestamp;
    }

    public String getSender() { return sender; }
    public String getMessageText() { return messageText; }
    public long getTimestamp() { return timestamp; }
}