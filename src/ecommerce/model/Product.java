package ecommerce.model;

import org.json.JSONObject;
import java.util.ArrayList;
import java.util.List;
import java.util.regex.*;

public class Product {
    private String name;
    private String image;
    private String price;
    private String oldPrice;
    private String description;
    private String rating;
    private String ratingCount;
    private String source;
    private String productType;

    public Product(String name, String image, String price, String description,
                   String rating, String ratingCount, String source, String productType) {
        this.name = name;
        this.image = image;
        this.price = price;
        this.description = description;
        this.rating = rating;
        this.ratingCount = ratingCount;
        this.source = source;
        this.productType = productType;
    }

    public static Product fromJson(JSONObject json) {
        return new Product(
                json.optString("Tên sản phẩm"),
                json.optString("Ảnh"),
                json.optString("Giá"),
                json.optString("Mô tả sản phẩm"),
                json.optString("Đánh giá"),
                json.optString("Số lượng đánh giá"),
                json.optString("Nguồn"),
                json.optString("Loại sản phẩm")
        );
    }

    // Getters and Setters
    public String getName() { return name; }
    public void setName(String name) { this.name = name; }

    public String getImage() { return image; }
    public void setImage(String image) { this.image = image; }

    public String getPrice() { return price; }
    public void setPrice(String price) { this.price = price; }

    public String getOldPrice() { return oldPrice; }
    public void setOldPrice(String oldPrice) { this.oldPrice = oldPrice; }

    public String getDescription() { return description; }
    public void setDescription(String description) { this.description = description; }

    public String getRating() { return rating; }
    public void setRating(String rating) { this.rating = rating; }

    public String getRatingCount() { return ratingCount; }
    public void setRatingCount(String ratingCount) { this.ratingCount = ratingCount; }

    public String getSource() { return source; }
    public void setSource(String source) { this.source = source; }

    public String getProductType() { return productType; }
    public void setProductType(String productType) { this.productType = productType; }
}
