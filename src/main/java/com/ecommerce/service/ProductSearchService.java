package com.ecommerce.service;

import com.ecommerce.model.Product;
import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.*;
import java.util.*;

public class ProductSearchService {
    private final List<Product> products;

    public ProductSearchService() {
        products = new ArrayList<>();
    }

    public void loadProductsFromJson(String jsonFilePath) throws IOException, JSONException {
        products.clear();
        String jsonContent = new String(Files.readAllBytes(Paths.get(jsonFilePath)), StandardCharsets.UTF_8);
        JSONArray jsonArray = new JSONArray(jsonContent);

        for (int i = 0; i < jsonArray.length(); i++) {
            JSONObject jsonProduct = jsonArray.getJSONObject(i);

            String name = jsonProduct.optString("Tên sản phẩm", "");
            String description = jsonProduct.optString("Mô tả sản phẩm", "");

            if (name.isEmpty() && description.isEmpty()) continue;

            Product product = new Product(
                    name,
                    jsonProduct.optString("Ảnh", "https://via.placeholder.com/400x300?text=No+Image"),
                    jsonProduct.optString("Giá", "0"),
                    description,
                    jsonProduct.optString("Điểm đánh giá trung bình", "0"),
                    jsonProduct.optString("Số lượt đánh giá", "0 đánh giá"),
                    jsonProduct.optString("Nguồn dữ liệu", ""),
                    jsonProduct.optString("Loại sản phẩm", "")
            );

            if (jsonProduct.has("Giá cũ")) {
                product.setOldPrice(jsonProduct.getString("Giá cũ"));
            }

            if (name.isEmpty() && !description.isEmpty()) {
                product.inferNameFromDescription();
            }

            products.add(product);
        }

        System.out.println("✅ Đã tải " + products.size() + " sản phẩm từ file JSON.");
    }

    public List<Product> searchProducts(String query) {
        if (query == null || query.trim().isEmpty()) {
            return new ArrayList<>(products);
        }

        List<Product> results = new ArrayList<>();
        Map<Product, Integer> productScores = new HashMap<>();
        String[] keywords = query.toLowerCase().split("\\s+");

        for (Product product : products) {
            int score = 0;
            String text = (product.getName() + " " + product.getDescription()).toLowerCase();

            for (String keyword : keywords) {
                if (text.contains(keyword)) score++;
                if (product.getName().toLowerCase().contains(keyword)) score += 2;
            }

            if (query.contains("dưới") || query.contains("nhỏ hơn") || query.contains("it hon")) {
                int priceLimit = extractPriceFromQuery(query, "dưới", "nhỏ hơn", "it hon");
                if (priceLimit > 0 && product.parsePrice() <= priceLimit) score += 3;
            }

            if (query.contains("trên") || query.contains("lớn hơn") || query.contains("lon hon")) {
                int priceLimit = extractPriceFromQuery(query, "trên", "lớn hơn", "lon hon");
                if (priceLimit > 0 && product.parsePrice() >= priceLimit) score += 3;
            }

            if (query.contains("người") || query.contains("nguoi")) {
                if ((query.contains("2") || query.contains("hai")) && (query.contains("3") || query.contains("ba")) &&
                        product.getDescription().contains("2 - 3 người")) score += 3;
                else if ((query.contains("4") || query.contains("bốn") || query.contains("bon")) &&
                        (query.contains("5") || query.contains("năm") || query.contains("nam")) &&
                        product.getDescription().contains("4 - 5 người")) score += 3;
                else if ((query.contains("5") || query.contains("năm") || query.contains("nam") ||
                        query.contains("nhiều") || query.contains("nhieu")) &&
                        product.getDescription().contains("Trên 5 người")) score += 3;
            }

            if ((query.contains("việt nam") || query.contains("viet nam")) &&
                    product.getDescription().contains("Sản xuất tại: Việt Nam")) score += 3;
            if ((query.contains("thái lan") || query.contains("thai lan")) &&
                    product.getDescription().contains("Sản xuất tại: Thái Lan")) score += 3;
            if ((query.contains("trung quốc") || query.contains("trung quoc") || query.contains("tàu") || query.contains("tau")) &&
                    product.getDescription().contains("Sản xuất tại: Trung Quốc")) score += 3;

            if (score > 0) productScores.put(product, score);
        }

        productScores.entrySet().stream()
                .sorted(Map.Entry.<Product, Integer>comparingByValue().reversed())
                .forEachOrdered(e -> results.add(e.getKey()));

        return results;
    }

    private int extractPriceFromQuery(String query, String... keywords) {
        int startPos = -1;
        for (String kw : keywords) {
            int pos = query.indexOf(kw);
            if (pos != -1) startPos = Math.max(startPos, pos + kw.length());
        }
        if (startPos == -1) return 0;

        StringBuilder num = new StringBuilder();
        for (int i = startPos; i < query.length(); i++) {
            char c = query.charAt(i);
            if (Character.isDigit(c)) num.append(c);
            else if (c == '.' || c == ',') continue;
            else break;
        }

        int base = 1;
        String tail = query.substring(startPos).toLowerCase();
        if (tail.contains("triệu") || tail.contains("trieu")) base = 1_000_000;
        else if (tail.contains("nghìn") || tail.contains("nghin") || tail.contains("k")) base = 1_000;

        try {
            return Integer.parseInt(num.toString()) * base;
        } catch (NumberFormatException e) {
            return 0;
        }
    }

    public List<Product> getAllProducts() {
        return new ArrayList<>(products);
    }

    public static String formatPrice(long price) {
        return String.format("%,d₫", price).replace(",", ".");
    }
    public static String formatPriceVietnamese(long price) {
        java.text.NumberFormat format = java.text.NumberFormat.getInstance(new java.util.Locale("vi", "VN"));
        return format.format(price) + " ₫";
    }
}