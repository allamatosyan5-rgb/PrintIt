package alla.matosyan.printit;

import android.graphics.Color;
import android.os.Bundle;
import android.view.Gravity;
import android.widget.EditText;
import android.widget.ImageButton;
import android.widget.LinearLayout;
import android.widget.ScrollView;
import android.widget.TextView;
import androidx.appcompat.app.AppCompatActivity;

import org.json.JSONArray;
import org.json.JSONObject;
import java.io.IOException;

import okhttp3.Call;
import okhttp3.Callback;
import okhttp3.MediaType;
import okhttp3.OkHttpClient;
import okhttp3.Request;
import okhttp3.RequestBody;
import okhttp3.Response;

public class ChatActivity extends AppCompatActivity {

    private EditText etChatInput;
    private ImageButton btnSendMessage;
    private LinearLayout chatMessagesContainer;
    private ScrollView chatScroll;

    private final OkHttpClient client = new OkHttpClient();
    public static final MediaType JSON = MediaType.get("application/json; charset=utf-8");

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
                callGeminiAPI(message);
            }
        });
    }

    private void callGeminiAPI(String userQuestion) {
        String apiKey = "AIzaSyDuyCmnvFbDwFt-uX6-fx8qXkpetgSSAEE";

        String url = "https://generativelanguage.googleapis.com/v1beta/models/gemini-1.5-flash:generateContent?key=" + apiKey;


        String promptContext = "You are a helpful AI assistant for a custom printing app called PrintIt. Keep your answers short, friendly, and to the point. The user asks: " + userQuestion;

        String jsonBody = "{\"contents\": [{\"parts\": [{\"text\": \"" + promptContext + "\"}]}]}";
        RequestBody body = RequestBody.create(jsonBody, JSON);
        Request request = new Request.Builder().url(url).post(body).build();

        client.newCall(request).enqueue(new Callback() {
            @Override
            public void onFailure(Call call, IOException e) {
                runOnUiThread(() -> addAIMessage("Sorry, I can't connect to the internet right now."));
            }

            @Override
            public void onResponse(Call call, Response response) throws IOException {
                if (response.isSuccessful()) {
                    try {
                        String responseData = response.body().string();
                        JSONObject jsonObject = new JSONObject(responseData);
                        JSONArray candidates = jsonObject.getJSONArray("candidates");
                        JSONObject content = candidates.getJSONObject(0).getJSONObject("content");
                        JSONArray parts = content.getJSONArray("parts");
                        String aiReply = parts.getJSONObject(0).getString("text");
                        runOnUiThread(() -> addAIMessage(aiReply));

                    } catch (Exception e) {
                        runOnUiThread(() -> addAIMessage("Oops, I had trouble reading that."));
                    }
                } else {
                    runOnUiThread(() -> addAIMessage("API Error: Check your API key."));
                }
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

    private void scrollToBottom() {
        chatScroll.post(() -> chatScroll.fullScroll(ScrollView.FOCUS_DOWN));
    }
}