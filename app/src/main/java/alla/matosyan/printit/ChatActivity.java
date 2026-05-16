package alla.matosyan.printit;

import android.os.Bundle;
import android.view.View;
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

                addUserMessageToScreen(message);

                etChatInput.setText("");

                simulateAiResponse();
            }
        });
    }

    private void addUserMessageToScreen(String message) {
        TextView userText = new TextView(this);
        userText.setText(message);
        userText.setBackgroundColor(getResources().getColor(R.color.caicon_blue_primary));
        userText.setTextColor(getResources().getColor(android.R.color.white));
        userText.setPadding(30, 20, 30, 20);

        LinearLayout.LayoutParams params = new LinearLayout.LayoutParams(
                LinearLayout.LayoutParams.WRAP_CONTENT,
                LinearLayout.LayoutParams.WRAP_CONTENT
        );
        params.gravity = android.view.Gravity.END;
        params.setMargins(0, 16, 0, 16);
        userText.setLayoutParams(params);

        chatMessagesContainer.addView(userText);
        scrollToBottom();
    }

    private void simulateAiResponse() {
        TextView aiText = new TextView(this);
        aiText.setText("I am an AI placeholder! Soon, I will be connected to a real brain to answer that.");
        aiText.setBackgroundColor(0xFFE0E0E0);
        aiText.setTextColor(0xFF000000);
        aiText.setPadding(30, 20, 30, 20);

        LinearLayout.LayoutParams params = new LinearLayout.LayoutParams(
                LinearLayout.LayoutParams.WRAP_CONTENT,
                LinearLayout.LayoutParams.WRAP_CONTENT
        );
        params.gravity = android.view.Gravity.START;
        params.setMargins(0, 16, 0, 16);
        aiText.setLayoutParams(params);

        chatMessagesContainer.postDelayed(() -> {
            chatMessagesContainer.addView(aiText);
            scrollToBottom();
        }, 1000);
    }

    private void scrollToBottom() {
        chatScroll.post(() -> chatScroll.fullScroll(View.FOCUS_DOWN));
    }
}