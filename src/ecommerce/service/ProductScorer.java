package ecommerce.service;

import ecommerce.model.Product;

public class ProductScorer {

    public static double calculateScore(Product p) {
        try {
            double rating = Double.parseDouble(p.getRating());
            int count = Integer.parseInt(p.getRatingCount().replaceAll("[^0-9]", ""));
            return rating * Math.log10(1 + count);
        } catch (Exception e) {
            return 0;
        }
    }
}
