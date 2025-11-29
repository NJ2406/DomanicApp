package com.example.domaniconeapk;

import android.app.Activity;
import android.content.Intent;
import android.os.Bundle;
import android.speech.RecognizerIntent;
import android.speech.tts.TextToSpeech;
import android.widget.*;
import androidx.annotation.Nullable;
import androidx.appcompat.app.AppCompatActivity;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.google.gson.JsonObject;

import java.io.IOException;
import java.util.ArrayList;
import java.util.Locale;

import okhttp3.Call;
import okhttp3.Callback;
import okhttp3.MediaType;
import okhttp3.OkHttpClient;
import okhttp3.Request;
import okhttp3.RequestBody;
import okhttp3.Response;

public class MainActivity extends AppCompatActivity {
    private static final int REQ_CODE_SPEECH_INPUT = 100;
    EditText etMessage;
    Button btnSend, btnMonitor;
    ImageButton btnMic;
    TextToSpeech tts;
    RecyclerView rvChat;
    ChatAdapter chatAdapter;

    // placeholder replaced by GitHub Action with your BACKEND_BASE secret
    private static final String BACKEND = "BACKEND_BASE_PLACEHOLDER";

    // optional: token header (workflow can replace BACKEND_AUTH_TOKEN_PLACEHOLDER with a token)
    private static final String BACKEND_AUTH_TOKEN = "BACKEND_AUTH_TOKEN_PLACEHOLDER";

    private final OkHttpClient http = new OkHttpClient();
    private static final MediaType JSON = MediaType.get("application/json; charset=utf-8");

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);
        etMessage = findViewById(R.id.etMessage);
        btnSend = findViewById(R.id.btnSend);
        btnMic = findViewById(R.id.btnMic);
        btnMonitor = findViewById(R.id.btnMonitor);
        rvChat = findViewById(R.id.rvChat);

        tts = new TextToSpeech(this, status -> { if (status==TextToSpeech.SUCCESS) tts.setLanguage(Locale.US); });

        chatAdapter = new ChatAdapter();
        rvChat.setLayoutManager(new LinearLayoutManager(this));
        rvChat.setAdapter(chatAdapter);

        btnMic.setOnClickListener(v -> startVoiceInput());
        btnSend.setOnClickListener(v -> {
            String text = etMessage.getText().toString().trim();
            if (text.isEmpty()) return;
            onUserMessage(text);
            etMessage.setText("");
        });
        btnMonitor.setOnClickListener(v -> startActivity(new Intent(this, MonitorActivity.class)));
    }

    private void startVoiceInput() {
        Intent intent = new Intent(RecognizerIntent.ACTION_RECOGNIZE_SPEECH);
        intent.putExtra(RecognizerIntent.EXTRA_LANGUAGE_MODEL, RecognizerIntent.LANGUAGE_MODEL_FREE_FORM);
        intent.putExtra(RecognizerIntent.EXTRA_LANGUAGE, Locale.getDefault());
        try { startActivityForResult(intent, REQ_CODE_SPEECH_INPUT); } catch (Exception e) { Toast.makeText(this, "Speech not supported", Toast.LENGTH_SHORT).show(); }
    }

    @Override
    protected void onActivityResult(int requestCode, int resultCode, @Nullable Intent data) {
        super.onActivityResult(requestCode, resultCode, data);
        if (requestCode == REQ_CODE_SPEECH_INPUT && resultCode == Activity.RESULT_OK && data != null) {
            ArrayList<String> result = data.getStringArrayListExtra(RecognizerIntent.EXTRA_RESULTS);
            if (result != null && result.size() > 0) {
                String text = result.get(0);
                etMessage.setText(text);
                onUserMessage(text);
            }
        }
    }

    private void onUserMessage(String text) {
        chatAdapter.addMessage(new ChatMessage("You", text));
        String lower = text.toLowerCase();
        if (lower.contains("turn on") || lower.contains("switch on") || lower.contains("on")) {
            String device = extractDevice(lower);
            String action = "TURN_ON";
            String resp = "Okay — turning on " + device + ". (Note: will call " + BACKEND + " when configured)";
            chatAdapter.addMessage(new ChatMessage("Domanic", resp));
            tts.speak(resp, TextToSpeech.QUEUE_ADD, null, "DomanicUtter");
            LogStore.logAction(new LogStore.Action(device, action, resp));
            if (!isBackendConfigured()) {
                LogStore.logAction(new LogStore.Action("network", "SKIP", "Backend not configured; not sending"));
            } else {
                sendDeviceCommandToBackend(device, action, text);
            }

        } else if (lower.contains("turn off") || lower.contains("switch off") || lower.contains("off")) {
            String device = extractDevice(lower);
            String action = "TURN_OFF";
            String resp = "Okay — turning off " + device + ". (Note: will call " + BACKEND + " when configured)";
            chatAdapter.addMessage(new ChatMessage("Domanic", resp));
            tts.speak(resp, TextToSpeech.QUEUE_ADD, null, "DomanicUtter");
            LogStore.logAction(new LogStore.Action(device, action, resp));
            if (!isBackendConfigured()) {
                LogStore.logAction(new LogStore.Action("network", "SKIP", "Backend not configured; not sending"));
            } else {
                sendDeviceCommandToBackend(device, action, text);
            }

        } else if (lower.contains("status") || lower.contains("what is") || lower.contains("running")) {
            String resp = "All systems nominal. (Offline mode)";
            chatAdapter.addMessage(new ChatMessage("Domanic", resp));
            tts.speak(resp, TextToSpeech.QUEUE_ADD, null, "DomanicUtter");
            LogStore.logAction(new LogStore.Action("system", "STATUS", resp));
        } else if (lower.contains("clear log")) {
            LogStore.clear();
            String resp = "Activity log cleared.";
            chatAdapter.addMessage(new ChatMessage("Domanic", resp));
            tts.speak(resp, TextToSpeech.QUEUE_ADD, null, "DomanicUtter");
        } else {
            String resp = cannedResponse(text);
            chatAdapter.addMessage(new ChatMessage("Domanic", resp));
            tts.speak(resp, TextToSpeech.QUEUE_ADD, null, "DomanicUtter");
            LogStore.logAction(new LogStore.Action("assistant", "REPLY", resp));
        }
        rvChat.scrollToPosition(chatAdapter.getItemCount()-1);
    }

    private boolean isBackendConfigured() {
        return BACKEND != null && !BACKEND.isEmpty() && !BACKEND.equals("BACKEND_BASE_PLACEHOLDER");
    }

    private void sendDeviceCommandToBackend(String deviceId, String action, String rawText) {
        try {
            String url = BACKEND;
            if (!url.endsWith("/")) url = url + "/";
            url = url + "api/devices/" + deviceId + "/command";

            JsonObject payload = new JsonObject();
            JsonObject inner = new JsonObject();
            inner.addProperty("action", action);
            inner.addProperty("source", "domanic");
            inner.addProperty("raw", rawText);
            payload.add("payload", inner);

            RequestBody body = RequestBody.create(payload.toString(), JSON);
            Request.Builder reqBuilder = new Request.Builder()
                    .url(url)
                    .post(body);

            // Attach optional auth header if provided (workflow can replace placeholder with token)
            if (BACKEND_AUTH_TOKEN != null && !BACKEND_AUTH_TOKEN.isEmpty() && !BACKEND_AUTH_TOKEN.equals("BACKEND_AUTH_TOKEN_PLACEHOLDER")) {
                reqBuilder.addHeader("Authorization", "Bearer " + BACKEND_AUTH_TOKEN);
            }

            Request req = reqBuilder.build();
            http.newCall(req).enqueue(new Callback() {
                @Override
                public void onFailure(Call call, IOException e) {
                    String msg = "Network failed: " + e.getMessage();
                    LogStore.logAction(new LogStore.Action(deviceId, "NET_FAIL", msg));
                    runOnUiThread(() -> Toast.makeText(MainActivity.this, "Failed to send command to backend", Toast.LENGTH_SHORT).show());
                }

                @Override
                public void onResponse(Call call, Response response) throws IOException {
                    String bodyStr = response.body() != null ? response.body().string() : "";
                    if (response.isSuccessful()) {
                        String msg = "Sent to backend OK. Response: " + (bodyStr.length() > 200 ? bodyStr.substring(0, 200) + "..." : bodyStr);
                        LogStore.logAction(new LogStore.Action(deviceId, "NET_OK", msg));
                    } else {
                        String msg = "Backend error " + response.code() + ": " + bodyStr;
                        LogStore.logAction(new LogStore.Action(deviceId, "NET_ERR", msg));
                    }
                }
            });
        } catch (Exception ex) {
            LogStore.logAction(new LogStore.Action(deviceId, "NET_EXCEPTION", ex.getMessage()));
        }
    }

    private String extractDevice(String text) {
        String[] parts = text.split(" ");
        if (parts.length == 0) return "device";
        return parts[parts.length-1].replaceAll("[^a-zA-Z0-9_-]", "");
    }

    private String cannedResponse(String user) {
        String lower = user.toLowerCase();
        if (lower.contains("hello") || lower.contains("hi")) return "Hey — Domanic here. How can I help?";
        if (lower.contains("your name")) return "I'm Domanic, your personal assistant.";
        return "I heard you. I can control devices and keep a log. Try: 'Turn on lamp' or 'Turn off fan'.";
    }
                                                         }
