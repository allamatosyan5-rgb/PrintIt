package alla.matosyan.printit;

import android.graphics.Color;
import android.os.Bundle;
import android.os.Handler;
import android.view.Gravity;
import android.widget.EditText;
import android.widget.ImageButton;
import android.widget.LinearLayout;
import android.widget.ScrollView;
import android.widget.TextView;
import androidx.appcompat.app.AppCompatActivity;

public class ChatActivity extends AppCompatActivity {

    private EditText etChatInput;
    private ImageButton btnSendMessage;
    private LinearLayout chatMessagesContainer;
    private ScrollView chatScroll;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_chat);

        etChatInput = findViewById(R.id.et_chat_input);
        btnSendMessage = findViewById(R.id.btn_send_message);
        chatMessagesContainer = findViewById(R.id.chat_messages_container);
        chatScroll = findViewById(R.id.chat_scroll);

        btnSendMessage.setOnClickListener(v -> {
            String message = etChatInput.getText().toString().trim();
            if (!message.isEmpty()) {
                addUserMessage(message);
                etChatInput.setText("");

                new Handler().postDelayed(() -> generateAIResponse(message.toLowerCase()), 1000);
            }
        });
    }

    private void addUserMessage(String text) {
        TextView userText = new TextView(this);
        userText.setText(text);
        userText.setBackgroundColor(Color.parseColor("#003366"));
        userText.setTextColor(Color.WHITE);
        userText.setPadding(32, 24, 32, 24);
        userText.setTextSize(16f);

        LinearLayout.LayoutParams params = new LinearLayout.LayoutParams(
                LinearLayout.LayoutParams.WRAP_CONTENT,
                LinearLayout.LayoutParams.WRAP_CONTENT
        );
        params.gravity = Gravity.END;
        params.setMargins(100, 16, 0, 16);
        userText.setLayoutParams(params);

        chatMessagesContainer.addView(userText);
        scrollToBottom();
    }

    private void addAIMessage(String text) {
        TextView aiText = new TextView(this);
        aiText.setText(text);
        aiText.setBackgroundColor(Color.parseColor("#E0E0E0"));
        aiText.setTextColor(Color.BLACK);
        aiText.setPadding(32, 24, 32, 24);
        aiText.setTextSize(16f);

        LinearLayout.LayoutParams params = new LinearLayout.LayoutParams(
                LinearLayout.LayoutParams.WRAP_CONTENT,
                LinearLayout.LayoutParams.WRAP_CONTENT
        );
        params.gravity = Gravity.START;
        params.setMargins(0, 8, 100, 16);
        aiText.setLayoutParams(params);

        chatMessagesContainer.addView(aiText);
        scrollToBottom();
    }

    private void generateAIResponse(String input) {
        String aiReply;

        if (input.contains("price") || input.contains("cost") || input.contains("how much")) {
            aiReply = "Our base prices are: T-Shirts ($25), Mugs ($12), Bottles ($18), Pillows ($22), and Posters ($10). Does that help?";
        } else if (input.contains("dtg") || input.contains("t-shirt") || input.contains("fabric")) {
            aiReply = "For fabrics like T-Shirts and Pillows, we use DTG (Direct to Garment) printing. It ensures vibrant colors that last long!";
        } else if (input.contains("laser") || input.contains("bottle") || input.contains("metal")) {
            aiReply = "For metal items like Water Bottles, we exclusively use Laser Engraving. It looks premium and never fades.";
        } else if (input.contains("hello") || input.contains("hi")) {
            aiReply = "Hello there! How can I assist you with your PrintIt design today?";
        } else {
            aiReply = "That's a great question! I can help you with pricing, printing technologies (like DTG, Transfer or Laser), or material choices. What would you like to know?";
        }

        addAIMessage(aiReply);
    }

    private void scrollToBottom() {
        chatScroll.post(() -> chatScroll.fullScroll(ScrollView.FOCUS_DOWN));
    }
}