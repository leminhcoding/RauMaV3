package com.ecommerce.service;

import com.ecommerce.model.Product;
import com.ecommerce.model.ProductWithEmbedding;

import java.util.*;
import java.util.stream.Collectors;

public class EmbeddingSearchService {

    public static double cosineSimilarity(float[] a, float[] b) {
        double dot = 0, normA = 0, normB = 0;
        for (int i = 0; i < a.length; i++) {
            dot += a[i] * b[i];
            normA += a[i] * a[i];
            normB += b[i] * b[i];
        }
        return dot / (Math.sqrt(normA) * Math.sqrt(normB));
    }

    // Dùng để lấy top K gần nhất (tùy chọn)
    public static List<ProductWithEmbedding> searchByVector(
            float[] queryEmbedding,
            String rawQuery,
            List<ProductWithEmbedding> products,
            double threshold,
            String preferredCategory  // ví dụ: "Tủ lạnh"
    ) {
        String queryLower = rawQuery.toLowerCase();

        List<Map.Entry<ProductWithEmbedding, Double>> scored = new ArrayList<>();

        for (ProductWithEmbedding p : products) {
            double score = cosineSimilarity(queryEmbedding, p.getEmbedding());

            if (score >= threshold) {
                if (p.getTenSanPham() != null && p.getTenSanPham().toLowerCase().contains(queryLower)) {
                    score += 0.2;  // bonus nếu tên khớp từ khoá
                }
                if (p.getRating() >= 4.5) {
                    score += 0.05; // ⭐ Ưu tiên sản phẩm có rating cao
                }
                if (p.getReviewCount() >= 100) {
                    score += 0.03; // 🗳️ Ưu tiên sản phẩm có nhiều đánh giá
                }

                int gia;
                try {
                    gia = Integer.parseInt(p.getGia().replaceAll("\\D", "")); // loại bỏ mọi ký tự không phải số
                } catch (NumberFormatException e) {
                    gia = Integer.MAX_VALUE; // nếu lỗi thì gán giá rất cao để không được chọn
                }

                if (queryLower.contains("giá rẻ") && gia < 5000000) {
                    score += 0.04; // 💸 Ưu tiên nếu người dùng muốn giá rẻ và giá < 5 triệu
                }

                scored.add(new AbstractMap.SimpleEntry<>(p, score));
            }
        }

        // Sắp xếp: ưu tiên đúng danh mục, rồi theo điểm giảm dần
        scored.sort((a, b) -> {
            ProductWithEmbedding p1 = a.getKey();
            ProductWithEmbedding p2 = b.getKey();

            boolean cat1 = preferredCategory != null && preferredCategory.equalsIgnoreCase(p1.getLoaiSanPham());
            boolean cat2 = preferredCategory != null && preferredCategory.equalsIgnoreCase(p2.getLoaiSanPham());

            if (cat1 != cat2) {
                return Boolean.compare(cat2, cat1); // true lên trước
            }

            return Double.compare(b.getValue(), a.getValue()); // điểm giảm dần
        });

        return scored.stream()
                .map(Map.Entry::getKey)
                .collect(Collectors.toList());
    }

    // Dùng trong chế độ tìm kiếm nâng cao (RAG)
    public List<Product> searchByEmbedding(float[] queryEmbedding, List<ProductWithEmbedding> allProducts, int limit) {
        List<AbstractMap.SimpleEntry<Product, Double>> scored = new ArrayList<>();

        for (ProductWithEmbedding item : allProducts) {
            double sim = cosineSimilarity(queryEmbedding, item.getEmbedding());

            if (sim >= 0.4) {  // ✅ lọc ngưỡng cosine ≥ 0.4
                scored.add(new AbstractMap.SimpleEntry<>(item.toProduct(), sim));
            }
        }

        // Sắp xếp theo độ tương đồng giảm dần
        scored.sort((a, b) -> Double.compare(b.getValue(), a.getValue()));

        return scored.stream()
                .limit(limit)
                .map(Map.Entry::getKey)
                .collect(Collectors.toList());
    }
}
