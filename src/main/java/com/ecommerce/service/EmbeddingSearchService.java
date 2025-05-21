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
    public static List<ProductWithEmbedding> searchByVector(float[] query, List<ProductWithEmbedding> products, int topK) {
        return products.stream()
                .map(p -> new AbstractMap.SimpleEntry<>(p, cosineSimilarity(query, p.getEmbedding())))
                .sorted((a, b) -> Double.compare(b.getValue(), a.getValue()))
                .limit(topK)
                .map(Map.Entry::getKey)
                .collect(Collectors.toList());
    }

    // Dùng trong chế độ tìm kiếm nâng cao (RAG)
    public List<Product> searchByEmbedding(float[] queryEmbedding, List<ProductWithEmbedding> allProducts, int limit) {
        List<AbstractMap.SimpleEntry<Product, Double>> scored = new ArrayList<>();

        for (ProductWithEmbedding item : allProducts) {
            double sim = cosineSimilarity(queryEmbedding, item.getEmbedding());

            if (sim >= 0.6) {  // ✅ lọc ngưỡng cosine ≥ 0.6
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
