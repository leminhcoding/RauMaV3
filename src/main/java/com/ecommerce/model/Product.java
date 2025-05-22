package com.ecommerce.model;

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

    public com.ecommerce.model.Specification[] extractSpecifications() {
        if (description == null || description.isEmpty()) return new Specification[0];

        String[][] patterns = {
                { "Loại sản phẩm", "Kiểu tủ: ([^\\.]+)" },
                { "Kiểu sản phẩm", "Loại Tivi: ([^\\.]+)" },
                { "Loại máy giặt", "Loại máy giặt: ([^\\.]+)" },
                { "Dung tích sử dụng", "Dung tích sử dụng: ([^\\.]+)" },
                { "Dung tích ngăn đá", "Dung tích ngăn đá: ([^\\.]+)" },
                { "Dung tích ngăn lạnh", "Dung tích ngăn lạnh: ([^\\.]+)" },
                { "Kích cỡ màn hình", "Kích cỡ màn hình: ([^\\.]+)" },
                { "Độ phân giải", "Độ phân giải: ([^\\.]+)" },
                { "Công nghệ hình ảnh", "Công nghệ hình ảnh: ([^\\.]+)" },
                { "Khối lượng giặt", "Khối lượng giặt: ([^\\.]+)" },
                { "Chất liệu cửa", "Chất liệu cửa tủ lạnh: ([^\\.]+)" },
                { "Chất liệu khay", "Chất liệu khay ngăn lạnh: ([^\\.]+)" },
                { "Năm ra mắt", "Năm ra mắt: ([^\\.]+)" },
                { "Nơi sản xuất", "Sản xuất tại: ([^\\.]+)" }
        };

        List<Specification> specsList = new ArrayList<>();
        for (String[] pattern : patterns) {
            Matcher matcher = Pattern.compile(pattern[1]).matcher(description);
            if (matcher.find()) {
                specsList.add(new Specification(pattern[0], matcher.group(1).trim()));
            }
        }

        if (description.contains("2 - 3 người")) specsList.add(new Specification("Số người sử dụng", "2 - 3 người"));
        if (description.contains("4 - 5 người")) specsList.add(new Specification("Số người sử dụng", "4 - 5 người"));
        if (description.contains("Trên 5 người")) specsList.add(new Specification("Số người sử dụng", "Trên 5 người"));

        return specsList.toArray(new Specification[0]);
    }

    public String extractKeyInfo() {
        if (description == null || description.isEmpty()) return "";

        List<String> keyInfo = new ArrayList<>();
        String[][] regexes = {
                { "Dung tích sử dụng", "Dung tích sử dụng: ([^\\.]+)" },
                { "Kích cỡ màn hình", "Kích cỡ màn hình: ([^\\.]+)" },
                { "Kiểu tủ", "Kiểu tủ: ([^\\.]+)" },
                { "Loại Tivi", "Loại Tivi: ([^\\.]+)" },
                { "Loại máy giặt", "Loại máy giặt: ([^\\.]+)" },
                { "Sản xuất", "Sản xuất tại: ([^\\.]+)" },
                { "Năm", "Năm ra mắt: ([^\\.]+)" }
        };

        for (String[] reg : regexes) {
            Matcher m = Pattern.compile(reg[1]).matcher(description);
            if (m.find()) keyInfo.add((reg[0].equals("Sản xuất") || reg[0].equals("Năm") ? reg[0] + ": " : "") + m.group(1).trim());
        }

        return String.join(" • ", keyInfo);
    }

    public long parsePrice() {
        if (price == null) return 0;
        String numeric = price.replaceAll("[^0-9]", "");
        try {
            return Long.parseLong(numeric);
        } catch (NumberFormatException e) {
            return 0;
        }
    }

    public void inferNameFromDescription() {
        if ((name == null || name.isEmpty()) && description != null && !description.isEmpty()) {
            String type = description.contains("Kiểu tủ:") ? "Tủ lạnh" :
                    description.contains("Loại Tivi:") ? "Tivi" :
                            description.contains("Loại máy giặt:") ? "Máy giặt" : "Sản phẩm";

            StringBuilder sb = new StringBuilder(type + " ");
            Matcher m1 = Pattern.compile("Dung tích sử dụng: ([^\\.]+)").matcher(description);
            Matcher m2 = Pattern.compile("Kích cỡ màn hình: ([^\\.]+)").matcher(description);
            if (m1.find()) sb.append(m1.group(1).trim());
            else if (m2.find()) sb.append(m2.group(1).trim());

            Matcher y = Pattern.compile("Năm ra mắt: ([^\\.]+)").matcher(description);
            if (y.find()) sb.append(" (").append(y.group(1).trim()).append(")");

            this.name = sb.toString();
        }
    }

    public boolean isFeatured() {
        try {
            return Float.parseFloat(rating) >= 4.8;
        } catch (Exception e) {
            return false;
        }
    }
    public String getDiemDanhGiaTrungBinh() {
        return rating;
    }

    public String getSoLuotDanhGia() {
        return ratingCount;
    }
}
