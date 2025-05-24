package ecommerce.service;

import ecommerce.model.Product;
import org.json.JSONArray;
import org.json.JSONObject;

import java.io.BufferedReader;
import java.io.InputStreamReader;
import java.io.OutputStream;
import java.net.HttpURLConnection;
import java.net.URL;
import java.util.ArrayList;
import java.util.List;

public class EmbeddingClient {

    public static List<Product> searchSemantic(String query) {
        List<Product> results = new ArrayList<>();

        try {
            URL url = new URL("http://localhost:8000/embed");
            HttpURLConnection conn = (HttpURLConnection) url.openConnection();
            conn.setRequestMethod("POST");
            conn.setRequestProperty("Content-Type", "application/json; utf-8");
            conn.setDoOutput(true);

            // Gửi JSON query
            String jsonInput = "{\"query\": \"" + query + "\"}";
            try (OutputStream os = conn.getOutputStream()) {
                byte[] input = jsonInput.getBytes("utf-8");
                os.write(input, 0, input.length);
            }

            // Đọc kết quả trả về
            StringBuilder response = new StringBuilder();
            try (BufferedReader br = new BufferedReader(new InputStreamReader(conn.getInputStream(), "utf-8"))) {
                String responseLine;
                while ((responseLine = br.readLine()) != null) {
                    response.append(responseLine.trim());
                }
            }

            JSONObject jsonResponse = new JSONObject(response.toString());
            JSONArray metadatas = jsonResponse.getJSONArray("metadatas");

            for (int i = 0; i < metadatas.length(); i++) {
                JSONArray inner = metadatas.getJSONArray(i);
                for (int j = 0; j < inner.length(); j++) {
                    JSONObject jsonProduct = inner.getJSONObject(j);
                    Product p = Product.fromJson(jsonProduct);
                    results.add(p);
                }
            }

        } catch (Exception e) {
            e.printStackTrace();
        }

        return results;
    }
}
