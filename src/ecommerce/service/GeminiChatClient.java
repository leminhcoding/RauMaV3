package ecommerce.service;

import org.json.JSONArray;
import org.json.JSONObject;

import java.io.*;
import java.net.HttpURLConnection;
import java.net.URL;
import java.nio.charset.StandardCharsets;
import java.util.stream.Collectors;

public class GeminiChatClient {
    private static final String API_KEY = "AIzaSyD5BrN5M2luc9S2PWLgBL7WwkSUFYspWGo"; // ✅ thay bằng key bạn lấy
    private static final String MODEL = "gemini-2.0-flash";

    public static String sendPrompt(String prompt) {
        try {
            String endpoint = String.format(
                    "https://generativelanguage.googleapis.com/v1beta/models/%s:generateContent?key=%s",
                    MODEL, API_KEY);

            String body = """
                {
                  "contents": [{"parts": [{"text": "%s"}]}]
                }
                """.formatted(prompt);

            URL url = new URL(endpoint);
            HttpURLConnection conn = (HttpURLConnection) url.openConnection();
            conn.setRequestMethod("POST");
            conn.setRequestProperty("Content-Type", "application/json; charset=UTF-8");
            conn.setDoOutput(true);

            try (OutputStream os = conn.getOutputStream()) {
                os.write(body.getBytes(StandardCharsets.UTF_8));
            }

            try (BufferedReader reader = new BufferedReader(
                    new InputStreamReader(conn.getInputStream(), StandardCharsets.UTF_8))) {
                String json = reader.lines().collect(Collectors.joining());
                return extractReply(json);
            }

        } catch (Exception e) {
            e.printStackTrace();
            return "❌ Đã xảy ra lỗi khi gọi Gemini.";
        }
    }

    private static String extractReply(String json) {
        try {
            JSONObject obj = new JSONObject(json);
            JSONArray candidates = obj.getJSONArray("candidates");
            if (candidates.length() == 0) return "❌ Không có phản hồi.";

            JSONObject content = candidates.getJSONObject(0).getJSONObject("content");
            JSONArray parts = content.getJSONArray("parts");
            if (parts.length() == 0) return "❌ Không có nội dung.";

            return parts.getJSONObject(0).getString("text");
        } catch (Exception e) {
            e.printStackTrace();
            return "❌ Lỗi xử lý phản hồi.";
        }
    }
}
