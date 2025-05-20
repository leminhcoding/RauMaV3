package com.ecommerce.service;

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

    public static List<ProductWithEmbedding> searchByVector(float[] query, List<ProductWithEmbedding> products, int topK) {
        return products.stream()
                .map(p -> new AbstractMap.SimpleEntry<>(p, cosineSimilarity(query, p.getEmbedding())))
                .sorted((a, b) -> Double.compare(b.getValue(), a.getValue()))
                .limit(topK)
                .map(Map.Entry::getKey)
                .collect(Collectors.toList());
    }
}
