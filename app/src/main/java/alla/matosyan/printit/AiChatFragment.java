package alla.matosyan.printit;

import android.Manifest;
import android.app.AlertDialog;
import android.content.Context;
import android.content.pm.PackageManager;
import android.graphics.Color;
import android.net.Uri;
import android.os.Bundle;
import android.os.Environment;
import android.speech.tts.TextToSpeech;
import java.io.File;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.Locale;
import android.widget.Toast;

import androidx.activity.result.ActivityResultLauncher;
import androidx.activity.result.contract.ActivityResultContracts;
import androidx.core.content.ContextCompat;
import androidx.core.content.FileProvider;

import android.view.Gravity;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.EditText;
import android.widget.ImageButton;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.RelativeLayout;
import android.widget.TextView;
import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.fragment.app.Fragment;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseUser;
import com.google.firebase.firestore.FirebaseFirestore;

import org.json.JSONArray;
import org.json.JSONObject;
import okhttp3.Call;
import okhttp3.Callback;
import okhttp3.MediaType;
import okhttp3.OkHttpClient;
import okhttp3.Request;
import okhttp3.RequestBody;
import okhttp3.Response;
import java.io.IOException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class AiChatFragment extends Fragment {

    private RecyclerView rvChatMessages;
    private EditText etChatMessage;
    private ImageButton btnSendChat;
    private ImageButton btnAttach;
    private androidx.drawerlayout.widget.DrawerLayout drawerLayout;
    private LinearLayout layoutSidebarHistory;

    private RelativeLayout layoutImagePreview;
    private ImageView ivImagePreview;
    private ImageButton btnRemoveImage;
    private Uri pendingImageUri = null;

    private ChatAdapter chatAdapter;
    private List<ChatSession> allSessions = new ArrayList<>();
    private ChatSession currentSession;

    private TextToSpeech textToSpeech;

    private ActivityResultLauncher<String> galleryLauncher;
    private ActivityResultLauncher<Uri> cameraLauncher;
    private ActivityResultLauncher<String> requestCameraPermissionLauncher;
    private Uri photoUri;

    @Override
    public void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        galleryLauncher = registerForActivityResult(new ActivityResultContracts.GetContent(), uri -> {
            if (uri != null) {
                pendingImageUri = uri;
                layoutImagePreview.setVisibility(View.VISIBLE);
                ivImagePreview.setImageURI(uri);
            }
        });

        cameraLauncher = registerForActivityResult(new ActivityResultContracts.TakePicture(), success -> {
            if (success && photoUri != null) {
                pendingImageUri = photoUri;
                layoutImagePreview.setVisibility(View.VISIBLE);
                ivImagePreview.setImageURI(photoUri);
            }
        });

        requestCameraPermissionLauncher = registerForActivityResult(new ActivityResultContracts.RequestPermission(), isGranted -> {
            if (isGranted) openCamera();
            else Toast.makeText(getContext(), "Camera permission is required to take photos.", Toast.LENGTH_SHORT).show();
        });
    }

    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container, @Nullable Bundle savedInstanceState) {
        try {
            View view = inflater.inflate(R.layout.fragment_ai_chat, container, false);

            rvChatMessages = view.findViewById(R.id.rvChatMessages);
            etChatMessage = view.findViewById(R.id.etChatMessage);
            btnSendChat = view.findViewById(R.id.btnSendChat);
            btnAttach = view.findViewById(R.id.btnAttach);
            drawerLayout = view.findViewById(R.id.drawerLayout);
            layoutSidebarHistory = view.findViewById(R.id.layoutSidebarHistory);

            layoutImagePreview = view.findViewById(R.id.layoutImagePreview);
            ivImagePreview = view.findViewById(R.id.ivImagePreview);
            btnRemoveImage = view.findViewById(R.id.btnRemoveImage);

            rvChatMessages.setLayoutManager(new LinearLayoutManager(getContext()));

            textToSpeech = new TextToSpeech(getContext(), status -> {
                if (status == TextToSpeech.SUCCESS) textToSpeech.setLanguage(Locale.US);
            });

            loadAllSessions();

            btnRemoveImage.setOnClickListener(v -> {
                pendingImageUri = null;
                layoutImagePreview.setVisibility(View.GONE);
            });

            btnSendChat.setOnClickListener(v -> {
                String userText = etChatMessage.getText().toString().trim();
                if (!userText.isEmpty() || pendingImageUri != null) {
                    String finalMessageText = userText;
                    if (userText.isEmpty() && pendingImageUri != null) {
                        finalMessageText = "[Image Uploaded]";
                    }
                    String imageString = (pendingImageUri != null) ? pendingImageUri.toString() : null;

                    addMessage(finalMessageText, true, imageString);
                    etChatMessage.setText("");
                    pendingImageUri = null;
                    layoutImagePreview.setVisibility(View.GONE);
                    generateAiResponse();
                }
            });

            btnAttach.setOnClickListener(v -> {
                String[] options = {"Take Photo", "Choose from Gallery"};
                AlertDialog.Builder builder = new AlertDialog.Builder(getContext());
                builder.setTitle("Upload Design");
                builder.setItems(options, (dialog, which) -> {
                    if (which == 0) {
                        if (ContextCompat.checkSelfPermission(requireContext(), Manifest.permission.CAMERA) == PackageManager.PERMISSION_GRANTED) {
                            openCamera();
                        } else {
                            requestCameraPermissionLauncher.launch(Manifest.permission.CAMERA);
                        }
                    } else {
                        galleryLauncher.launch("image/*");
                    }
                });
                builder.show();
            });

            ImageButton btnOpenMenu = view.findViewById(R.id.btnOpenMenu);
            btnOpenMenu.setOnClickListener(v -> drawerLayout.openDrawer(androidx.core.view.GravityCompat.START));

            LinearLayout btnNewChat = view.findViewById(R.id.btnNewChat);
            btnNewChat.setOnClickListener(v -> {
                createNewSession();
                drawerLayout.closeDrawer(androidx.core.view.GravityCompat.START);
            });

            return view;

        } catch (Throwable e) {
            e.printStackTrace();
            TextView errorView = new TextView(inflater.getContext());
            errorView.setText("UI CRASH INTERCEPTED:\n\n" + e.toString());
            errorView.setTextColor(Color.RED);
            return errorView;
        }
    }

    private void openCamera() {
        if (getContext() == null) return;
        try {
            String timeStamp = new SimpleDateFormat("yyyyMMdd_HHmmss", Locale.getDefault()).format(new Date());
            File storageDir = getContext().getExternalFilesDir(Environment.DIRECTORY_PICTURES);
            File imageFile = File.createTempFile("PRINTIT_" + timeStamp + "_", ".jpg", storageDir);

            String authority = getContext().getPackageName() + ".fileprovider";
            photoUri = FileProvider.getUriForFile(getContext(), authority, imageFile);
            cameraLauncher.launch(photoUri);
        } catch (Exception e) {
            e.printStackTrace();
            Toast.makeText(getContext(), "Camera Error: " + e.getMessage(), Toast.LENGTH_LONG).show();
        }
    }

    @Override
    public void onDestroyView() {
        if (textToSpeech != null) {
            textToSpeech.stop();
            textToSpeech.shutdown();
        }
        super.onDestroyView();
    }

    private void createNewSession() {
        if (currentSession != null && currentSession.messages.size() <= 1) return;

        currentSession = new ChatSession();
        currentSession.id = String.valueOf(System.currentTimeMillis());
        currentSession.title = "New chat";
        currentSession.messages = new ArrayList<>();
        currentSession.isPinned = false;

        allSessions.add(0, currentSession);
        switchSession(currentSession);

        addMessage("Hello! I am your CAICON PrintIt Assistant. How can I help you with your custom merchandise today?", false, null);
    }

    private void switchSession(ChatSession session) {
        currentSession = session;
        chatAdapter = new ChatAdapter(currentSession.messages);
        rvChatMessages.setAdapter(chatAdapter);
        if (!currentSession.messages.isEmpty()) {
            rvChatMessages.scrollToPosition(currentSession.messages.size() - 1);
        }
        rebuildSidebarUI();
    }

    private void addMessage(String text, boolean isUser, String imageUri) {
        if (currentSession == null) return;

        currentSession.messages.add(new ChatMessage(text, isUser, imageUri));
        chatAdapter.notifyItemInserted(currentSession.messages.size() - 1);
        rvChatMessages.scrollToPosition(currentSession.messages.size() - 1);

        if (isUser && currentSession.messages.size() == 2) {
            String newTitle = text;
            if (newTitle.equals("[Image Uploaded]")) newTitle = "Photo Upload";
            else if (newTitle.length() > 22) newTitle = newTitle.substring(0, 22) + "...";
            currentSession.title = newTitle;
            rebuildSidebarUI();
        }
        saveAllSessions();
    }

    private void saveAllSessions() {
        if (currentSession == null) return;
        FirebaseUser user = FirebaseAuth.getInstance().getCurrentUser();
        if (user == null) return;

        List<Map<String, Object>> sessionsList = new ArrayList<>();
        for (ChatSession session : allSessions) {
            if (session.messages.size() > 1 || session.id.equals(currentSession.id)) {
                Map<String, Object> sessionMap = new HashMap<>();
                sessionMap.put("id", session.id);
                sessionMap.put("title", session.title);
                sessionMap.put("isPinned", session.isPinned);

                List<Map<String, Object>> messagesList = new ArrayList<>();
                for (ChatMessage msg : session.messages) {
                    Map<String, Object> msgMap = new HashMap<>();
                    msgMap.put("text", msg.text);
                    msgMap.put("isUser", msg.isUser);
                    if (msg.imageUri != null) msgMap.put("imageUri", msg.imageUri);
                    messagesList.add(msgMap);
                }
                sessionMap.put("messages", messagesList);
                sessionsList.add(sessionMap);
            }
        }

        Map<String, Object> data = new HashMap<>();
        data.put("sessionsData", sessionsList);

        FirebaseFirestore.getInstance().collection("Users").document(user.getUid())
                .collection("ChatData").document("AiHistory")
                .set(data)
                .addOnFailureListener(e -> {
                    if (getContext() != null) {
                        Toast.makeText(getContext(), "Failed to sync chat to cloud", Toast.LENGTH_SHORT).show();
                    }
                });
    }

    @SuppressWarnings("unchecked")
    private void loadAllSessions() {
        FirebaseUser user = FirebaseAuth.getInstance().getCurrentUser();
        if (user == null) {
            createNewSession();
            return;
        }

        FirebaseFirestore.getInstance().collection("Users").document(user.getUid())
                .collection("ChatData").document("AiHistory")
                .get()
                .addOnSuccessListener(documentSnapshot -> {
                    allSessions.clear();
                    if (documentSnapshot.exists() && documentSnapshot.contains("sessionsData")) {
                        List<?> sessionsList = (List<?>) documentSnapshot.get("sessionsData");
                        if (sessionsList != null) {
                            for (Object sessionObj : sessionsList) {
                                if (sessionObj instanceof Map) {
                                    Map<String, Object> sessionMap = (Map<String, Object>) sessionObj;
                                    ChatSession session = new ChatSession();
                                    session.id = (String) sessionMap.get("id");
                                    session.title = (String) sessionMap.get("title");
                                    Boolean isPinned = (Boolean) sessionMap.get("isPinned");
                                    session.isPinned = isPinned != null ? isPinned : false;
                                    session.messages = new ArrayList<>();

                                    List<?> msgsList = (List<?>) sessionMap.get("messages");
                                    if (msgsList != null) {
                                        for (Object msgObj : msgsList) {
                                            if (msgObj instanceof Map) {
                                                Map<String, Object> msgMap = (Map<String, Object>) msgObj;
                                                String text = (String) msgMap.get("text");
                                                Boolean isUser = (Boolean) msgMap.get("isUser");
                                                String imageUri = (String) msgMap.get("imageUri");
                                                session.messages.add(new ChatMessage(text, isUser != null ? isUser : false, imageUri));
                                            }
                                        }
                                    }
                                    if (session.messages.size() > 1) allSessions.add(session);
                                }
                            }
                        }
                    }
                    createNewSession();
                })
                .addOnFailureListener(e -> {
                    createNewSession();
                });
    }

    private void rebuildSidebarUI() {
        if (layoutSidebarHistory == null || getContext() == null || currentSession == null) return;
        layoutSidebarHistory.removeAllViews();

        List<ChatSession> sortedSessions = new ArrayList<>();
        for (ChatSession s : allSessions) if (s.isPinned) sortedSessions.add(s);
        for (ChatSession s : allSessions) if (!s.isPinned) sortedSessions.add(s);

        for (ChatSession session : sortedSessions) {
            if (session.messages.size() <= 1 && !session.id.equals(currentSession.id)) continue;

            LinearLayout rowLayout = new LinearLayout(getContext());
            rowLayout.setOrientation(LinearLayout.HORIZONTAL);
            rowLayout.setLayoutParams(new LinearLayout.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.WRAP_CONTENT));
            rowLayout.setGravity(Gravity.CENTER_VERTICAL);
            rowLayout.setPadding(16, 24, 16, 24);

            if (session.id.equals(currentSession.id)) {
                rowLayout.setBackgroundColor(Color.parseColor("#333333"));
            } else {
                rowLayout.setBackgroundColor(Color.TRANSPARENT);
            }

            TextView tvTitle = new TextView(getContext());
            tvTitle.setText(session.title);
            tvTitle.setTextColor(session.id.equals(currentSession.id) ? Color.parseColor("#8AB4F8") : Color.parseColor("#E0E0E0"));
            tvTitle.setTextSize(14f);
            tvTitle.setSingleLine(true);
            LinearLayout.LayoutParams textParams = new LinearLayout.LayoutParams(0, ViewGroup.LayoutParams.WRAP_CONTENT, 1.0f);
            tvTitle.setLayoutParams(textParams);

            rowLayout.setOnClickListener(v -> {
                switchSession(session);
                drawerLayout.closeDrawer(androidx.core.view.GravityCompat.START);
            });

            ImageView btnPin = new ImageView(getContext());
            btnPin.setImageResource(session.isPinned ? android.R.drawable.star_on : android.R.drawable.star_off);
            btnPin.setColorFilter(Color.parseColor("#E0E0E0"));
            btnPin.setPadding(8, 8, 8, 8);
            btnPin.setOnClickListener(v -> {
                session.isPinned = !session.isPinned;
                saveAllSessions();
                rebuildSidebarUI();
            });

            ImageView btnRename = new ImageView(getContext());
            btnRename.setImageResource(android.R.drawable.ic_menu_edit);
            btnRename.setColorFilter(Color.parseColor("#E0E0E0"));
            btnRename.setPadding(8, 8, 8, 8);
            btnRename.setOnClickListener(v -> {
                AlertDialog.Builder builder = new AlertDialog.Builder(getContext());
                builder.setTitle("Rename Chat");
                final EditText input = new EditText(getContext());
                input.setText(session.title);
                input.setPadding(40,40,40,40);
                builder.setView(input);
                builder.setPositiveButton("Save", (dialog, which) -> {
                    session.title = input.getText().toString();
                    saveAllSessions();
                    rebuildSidebarUI();
                });
                builder.setNegativeButton("Cancel", null);
                builder.show();
            });

            ImageView btnDelete = new ImageView(getContext());
            btnDelete.setImageResource(android.R.drawable.ic_menu_delete);
            btnDelete.setColorFilter(Color.parseColor("#E0E0E0"));
            btnDelete.setPadding(8, 8, 16, 8);
            btnDelete.setOnClickListener(v -> {
                AlertDialog.Builder builder = new AlertDialog.Builder(getContext());
                builder.setTitle("Delete Chat?");
                builder.setMessage("Are you sure you want to delete this conversation?");
                builder.setPositiveButton("Delete", (dialog, which) -> {
                    allSessions.remove(session);
                    if (session.id.equals(currentSession.id)) {
                        createNewSession();
                    } else {
                        saveAllSessions();
                        rebuildSidebarUI();
                    }
                });
                builder.setNegativeButton("Cancel", null);
                builder.show();
            });

            rowLayout.addView(tvTitle);
            rowLayout.addView(btnPin);
            rowLayout.addView(btnRename);
            rowLayout.addView(btnDelete);

            layoutSidebarHistory.addView(rowLayout);
        }
    }

    private String getBase64FromUri(String uriString) {
        try {
            Uri uri = Uri.parse(uriString);
            java.io.InputStream imageStream = getContext().getContentResolver().openInputStream(uri);
            android.graphics.Bitmap selectedImage = android.graphics.BitmapFactory.decodeStream(imageStream);

            int maxDim = 800;
            int width = selectedImage.getWidth();
            int height = selectedImage.getHeight();
            if (width > maxDim || height > maxDim) {
                float ratio = Math.min((float) maxDim / width, (float) maxDim / height);
                width = Math.round((float) ratio * width);
                height = Math.round((float) ratio * height);
                selectedImage = android.graphics.Bitmap.createScaledBitmap(selectedImage, width, height, false);
            }

            java.io.ByteArrayOutputStream baos = new java.io.ByteArrayOutputStream();
            selectedImage.compress(android.graphics.Bitmap.CompressFormat.JPEG, 80, baos);
            byte[] b = baos.toByteArray();
            return android.util.Base64.encodeToString(b, android.util.Base64.NO_WRAP);
        } catch (Exception e) {
            e.printStackTrace();
            return null;
        }
    }

    private void generateAiResponse() {
        String apiKey = BuildConfig.GEMINI_API_KEY;
        String url = "https://generativelanguage.googleapis.com/v1beta/models/gemini-2.5-flash:generateContent?key=" + apiKey;

        JSONObject jsonBody = new JSONObject();
        try {
            JSONArray contents = new JSONArray();
            JSONObject part = new JSONObject();
            JSONArray partsArray = new JSONArray();

            StringBuilder conversationMemory = new StringBuilder();
            conversationMemory.append("SYSTEM: You are an expert print designer and customer support AI for CAICON PrintIt. ");
            conversationMemory.append("When a user uploads an image, you MUST analyze the actual image provided. ");
            conversationMemory.append("1. Give specific professional advice on the design itself (colors, layout). ");
            conversationMemory.append("2. Analyze if the image resolution/sharpness looks high enough for high-quality physical printing. ");
            conversationMemory.append("3. Ask them what specific merchandise they want to print this on.\n\n");

            int startIndex = Math.max(0, currentSession.messages.size() - 6);
            for (int i = startIndex; i < currentSession.messages.size(); i++) {
                ChatMessage msg = currentSession.messages.get(i);
                String sender = msg.isUser ? "Customer: " : "Assistant: ";
                conversationMemory.append(sender).append(msg.text).append("\n");
            }
            conversationMemory.append("Assistant: ");

            JSONObject textObj = new JSONObject();
            textObj.put("text", conversationMemory.toString());
            partsArray.put(textObj);

            for (int i = startIndex; i < currentSession.messages.size(); i++) {
                ChatMessage msg = currentSession.messages.get(i);
                if (msg.imageUri != null) {
                    String base64Image = getBase64FromUri(msg.imageUri);
                    if (base64Image != null) {
                        JSONObject inlineDataObj = new JSONObject();
                        JSONObject inlineData = new JSONObject();
                        inlineData.put("mimeType", "image/jpeg");
                        inlineData.put("data", base64Image);
                        inlineDataObj.put("inlineData", inlineData);
                        partsArray.put(inlineDataObj);
                    }
                }
            }

            part.put("parts", partsArray);
            contents.put(part);

            JSONObject config = new JSONObject();
            config.put("temperature", 0.7);
            jsonBody.put("generationConfig", config);

            jsonBody.put("contents", contents);
        } catch (Exception e) {
            e.printStackTrace();
        }

        RequestBody body = RequestBody.create(jsonBody.toString(), MediaType.get("application/json; charset=utf-8"));
        Request request = new Request.Builder().url(url).post(body).build();

        OkHttpClient client = new OkHttpClient();
        client.newCall(request).enqueue(new Callback() {
            @Override
            public void onFailure(@NonNull Call call, @NonNull IOException e) {
                if (getActivity() != null) getActivity().runOnUiThread(() -> addMessage("Network Error.", false, null));
            }

            @Override
            public void onResponse(@NonNull Call call, @NonNull Response response) throws IOException {
                if (response.isSuccessful() && response.body() != null) {
                    try {
                        String responseData = response.body().string();
                        JSONObject jsonResponse = new JSONObject(responseData);

                        if (jsonResponse.has("candidates")) {
                            JSONArray candidates = jsonResponse.getJSONArray("candidates");
                            if (candidates.length() > 0) {
                                JSONObject firstCandidate = candidates.getJSONObject(0);

                                if (firstCandidate.has("finishReason") && !firstCandidate.getString("finishReason").equals("STOP")) {
                                    String reason = firstCandidate.getString("finishReason");
                                    if (getActivity() != null) getActivity().runOnUiThread(() -> addMessage("I cannot process that request right now. (Reason: " + reason + ")", false, null));
                                    return;
                                }

                                if (firstCandidate.has("content")) {
                                    String aiText = firstCandidate.getJSONObject("content")
                                            .getJSONArray("parts").getJSONObject(0).getString("text");

                                    final String cleanText = aiText.replace("**", "").trim();

                                    if (!cleanText.isEmpty()) {
                                        if (getActivity() != null) getActivity().runOnUiThread(() -> addMessage(cleanText, false, null));
                                        return;
                                    }
                                }
                            }
                        }
                        if (getActivity() != null) getActivity().runOnUiThread(() -> addMessage("Sorry, I received a blank response.", false, null));

                    } catch (Exception e) {
                        e.printStackTrace();
                        if (getActivity() != null) getActivity().runOnUiThread(() -> addMessage("Oops, I had trouble understanding that data.", false, null));
                    }
                } else {
                    int statusCode = response.code();
                    String customErrorMessage = (statusCode == 503) ? "I am currently helping a lot of customers! Please wait a moment."
                            : (statusCode == 429) ? "I have reached my message limit for right now."
                            : "System Error (" + statusCode + "). Please try again.";

                    if (getActivity() != null) getActivity().runOnUiThread(() -> addMessage(customErrorMessage, false, null));
                }
            }
        });
    }

    private static class ChatMessage {
        String text; boolean isUser; String imageUri;
        ChatMessage(String text, boolean isUser, String imageUri) {
            this.text = text; this.isUser = isUser; this.imageUri = imageUri;
        }
    }

    private static class ChatSession {
        String id; String title; boolean isPinned; List<ChatMessage> messages;
    }

    private class ChatAdapter extends RecyclerView.Adapter<ChatAdapter.ChatViewHolder> {
        private List<ChatMessage> messages;
        ChatAdapter(List<ChatMessage> messages) { this.messages = messages; }

        @NonNull @Override
        public ChatViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
            return new ChatViewHolder(LayoutInflater.from(parent.getContext()).inflate(R.layout.item_chat_bubble, parent, false));
        }

        @Override
        public void onBindViewHolder(@NonNull ChatViewHolder holder, int position) {
            ChatMessage message = messages.get(position);
            holder.tvChatMessage.setText(message.text);

            if (message.imageUri != null) {
                holder.ivAttachedImage.setVisibility(View.VISIBLE);
                try {
                    holder.ivAttachedImage.setImageURI(Uri.parse(message.imageUri));
                } catch (Exception e) {
                    holder.ivAttachedImage.setVisibility(View.GONE);
                }
            } else {
                holder.ivAttachedImage.setVisibility(View.GONE);
            }

            LinearLayout.LayoutParams params = (LinearLayout.LayoutParams) holder.layoutMessageContainer.getLayoutParams();

            if (message.isUser) {
                params.gravity = Gravity.END;
                holder.layoutMessageContainer.setLayoutParams(params);
                holder.layoutMessageContainer.setBackgroundColor(Color.parseColor("#1976D2"));
                holder.tvChatMessage.setTextColor(Color.WHITE);
                holder.layoutAiControls.setVisibility(View.GONE);
            } else {
                params.gravity = Gravity.START;
                holder.layoutMessageContainer.setLayoutParams(params);
                holder.layoutMessageContainer.setBackgroundColor(Color.parseColor("#E0E0E0"));
                holder.tvChatMessage.setTextColor(Color.BLACK);
                holder.layoutAiControls.setVisibility(View.VISIBLE);

                holder.btnListen.setOnClickListener(v -> {
                    if (textToSpeech != null) {
                        Toast.makeText(getContext(), "Reading aloud...", Toast.LENGTH_SHORT).show();
                        textToSpeech.speak(message.text, TextToSpeech.QUEUE_FLUSH, null, null);
                    }
                });
            }
        }
        @Override public int getItemCount() { return messages.size(); }

        class ChatViewHolder extends RecyclerView.ViewHolder {
            LinearLayout layoutMessageContainer;
            TextView tvChatMessage;
            ImageView ivAttachedImage;
            LinearLayout layoutAiControls;
            ImageButton btnListen;

            ChatViewHolder(@NonNull View itemView) {
                super(itemView);
                layoutMessageContainer = itemView.findViewById(R.id.layoutMessageContainer);
                tvChatMessage = itemView.findViewById(R.id.tvChatMessage);
                ivAttachedImage = itemView.findViewById(R.id.ivAttachedImage);
                layoutAiControls = itemView.findViewById(R.id.layoutAiControls);
                btnListen = itemView.findViewById(R.id.btnListen);
            }
        }
    }
}