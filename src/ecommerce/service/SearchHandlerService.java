package ecommerce.service;

import ecommerce.model.Product;
import javafx.application.Platform;
import javafx.concurrent.Task;

import java.util.List;
import java.util.function.Consumer;

public class SearchHandlerService {

    public static Task<Void> createSearchTask(
            String query,
            boolean useLLM,
            List<Product> allProducts,
            List<String> knownCategories,
            ProductSearchService searchService,
            Consumer<List<Product>> callback
    ) {
        return new Task<>() {
            @Override
            protected Void call() {
                List<Product> result;

                if (useLLM) {
                    // Dùng ChromaDB qua Flask API
                    result = EmbeddingClient.searchSemantic(query);
                } else {
                    result = searchService.searchProducts(query);
                }

                Platform.runLater(() -> callback.accept(result));
                return null;
            }
        };
    }
}
