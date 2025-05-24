package ecommerce.logic;

import ecommerce.model.Product;
import ecommerce.service.ProductScorer;
import ecommerce.service.ProductSearchService;

import java.util.*;
import java.util.stream.Collectors;

public class SmartSuggestionEngine {

    public static List<Product> suggest(String userInput, ProductSearchService searchService) {
        String input = userInput.toLowerCase();

        String[] knownTypes = {"tủ lạnh", "máy giặt", "tivi", "điều hòa"};
        String productType = Arrays.stream(knownTypes)
                .filter(input::contains)
                .findFirst()
                .orElse("");

        long maxPrice = extractPrice(input);
        String brand = extractBrand(input);
        String people = extractPeopleKeyword(input);
        String origin = extractOrigin(input);

        return searchService.getAllProducts().stream()
                .filter(p -> productType.isEmpty() || p.getProductType().toLowerCase().contains(productType))
                .filter(p -> {
                    String raw = p.getPrice().replaceAll("[^0-9]", "");
                    if (raw.isEmpty()) return false;
                    return Long.parseLong(raw) <= maxPrice;
                })
                .filter(p -> brand.isEmpty() || p.getDescription().toLowerCase().contains("thương hiệu: " + brand))
                .filter(p -> people.isEmpty() || p.getDescription().toLowerCase().contains(people))
                .filter(p -> origin.isEmpty() || p.getDescription().toLowerCase().contains("sản xuất tại: " + origin))
                .sorted(Comparator.comparingDouble(ProductScorer::calculateScore).reversed())
                .limit(5)
                .collect(Collectors.toList());
    }

    private static long extractPrice(String text) {
        text = text.toLowerCase();
        if (!text.contains("dưới")) return Long.MAX_VALUE;
        String[] tokens = text.split("\\s+");
        for (int i = 0; i < tokens.length - 1; i++) {
            if (tokens[i].equals("dưới")) {
                String num = tokens[i + 1].replaceAll("[^0-9]", "");
                if (!num.isEmpty()) return Long.parseLong(num) * 1_000_000;
            }
        }
        return Long.MAX_VALUE;
    }

    private static String extractBrand(String input) {
        String[] brands = {"lg", "panasonic", "aqua", "toshiba", "samsung", "electrolux", "hitachi"};
        return Arrays.stream(brands).filter(input::contains).findFirst().orElse("");
    }

    private static String extractPeopleKeyword(String input) {
        if (input.contains("2") && input.contains("3")) return "2 - 3 người";
        if (input.contains("4") && input.contains("5")) return "4 - 5 người";
        if (input.contains("trên 5") || input.contains("nhiều")) return "trên 5 người";
        if (input.contains("4 người")) return "4 người";
        return "";
    }

    private static String extractOrigin(String input) {
        String[] origins = {"việt nam", "thái lan", "trung quốc", "malaysia", "nhật bản"};
        return Arrays.stream(origins).filter(input::contains).findFirst().orElse("");
    }
}
