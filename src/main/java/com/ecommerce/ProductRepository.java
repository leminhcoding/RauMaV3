package com.ecommerce;

import com.ecommerce.model.Product;
import org.json.JSONArray;
import org.json.JSONObject;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.List;

public class ProductRepository {
    public static List<Product> loadProducts(String path) {
        List<Product> products = new ArrayList<>();
        try {
            String content = Files.readString(Paths.get(path));
            JSONArray array = new JSONArray(content);
            for (int i = 0; i < array.length(); i++) {
                JSONObject obj = array.getJSONObject(i);
                products.add(Product.fromJson(obj));
            }
        } catch (IOException e) {
            e.printStackTrace();
        }
        return products;
    }
}