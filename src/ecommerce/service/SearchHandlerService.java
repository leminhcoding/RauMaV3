package ecommerce.service;

import ecommerce.model.Product;
import ecommerce.model.ProductWithEmbedding;
import javafx.application.Platform;
import javafx.concurrent.Task;

import java.util.*;
import java.util.function.Consumer;
import java.util.stream.Collectors;

public class SearchHandlerService {

    public static Task<Void> createSearchTask(
            String query,
            boolean useLLM,
            List<ProductWithEmbedding> allEmbeddedProducts,
            List<String> knownCategories,
            ProductSearchService searchService,
            Consumer<List<Product>> callback
    ) {
        return new Task<>() {
            @Override
            protected Void call() {
                List<Product> result;

                if (useLLM) {
                    float[] queryVector = EmbeddingClient.getEmbedding(query);
                    String matchedCategory = knownCategories.stream()
                            .filter(cat -> query.toLowerCase().contains(cat.toLowerCase()))
                            .findFirst()
                            .orElse(null);

                    List<ProductWithEmbedding> matches = EmbeddingSearchService.searchByVector(
                            queryVector, query, allEmbeddedProducts, 0.4, matchedCategory
                    );
                    result = matches.stream().map(ProductWithEmbedding::toProduct).collect(Collectors.toList());
                } else {
                    result = searchService.searchProducts(query);
                }

                Platform.runLater(() -> callback.accept(result));
                return null;
            }
        };
    }
}
