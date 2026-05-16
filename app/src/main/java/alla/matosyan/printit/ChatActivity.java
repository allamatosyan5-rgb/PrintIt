package alla.matosyan.printit;

import android.os.Bundle;
import android.view.View;
import android.widget.EditText;
import android.widget.ImageButton;
import android.widget.LinearLayout;
import android.widget.TextView;
import androidx.appcompat.app.AppCompatActivity;
import androidx.drawerlayout.widget.DrawerLayout;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

public class ChatActivity extends AppCompatActivity {

    private DrawerLayout drawerLayout;
    private ImageButton btnOpenMenu;
    private RecyclerView rvChatMessages;
    private ImageButton btnAttach;
    private EditText etChatMessage;
    private ImageButton btnSendChat;
    private LinearLayout btnNewChat;
    private LinearLayout layoutSidebarHistory;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.fragment_ai_chat);

        drawerLayout = findViewById(R.id.drawerLayout);
        btnOpenMenu = findViewById(R.id.btnOpenMenu);
        rvChatMessages = findViewById(R.id.rvChatMessages);
        btnAttach = findViewById(R.id.btnAttach);
        etChatMessage = findViewById(R.id.etChatMessage);
        btnSendChat = findViewById(R.id.btnSendChat);
        btnNewChat = findViewById(R.id.btnNewChat);
        layoutSidebarHistory = findViewById(R.id.layoutSidebarHistory);

        rvChatMessages.setLayoutManager(new LinearLayoutManager(this));

        btnOpenMenu.setOnClickListener(v -> drawerLayout.openDrawer(android.view.Gravity.LEFT));

        btnSendChat.setOnClickListener(v -> {
            String message = etChatMessage.getText().toString().trim();
            if (!message.isEmpty()) {
                addUserMessageToScreen(message);
                etChatMessage.setText("");
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

        rvChatMessages.addView(userText);
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

        rvChatMessages.postDelayed(() -> {
            rvChatMessages.addView(aiText);
            scrollToBottom();
        }, 1000);
    }

    private void scrollToBottom() {
        if (rvChatMessages.getAdapter() != null) {
            rvChatMessages.scrollToPosition(rvChatMessages.getAdapter().getItemCount() - 1);
        }
    }
}