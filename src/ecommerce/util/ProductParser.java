package ecommerce.util;

import ecommerce.model.Product;
import ecommerce.model.Specification;

import java.util.ArrayList;
import java.util.List;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

public class ProductParser {

    public static Specification[] extractSpecifications(Product product) {
        String description = product.getDescription();
        if (description == null || description.isEmpty()) return new Specification[0];

        String[][] patterns = {
                { "Loại sản phẩm", "Kiểu tủ: ([^\\.]+)" },
                { "Kiểu sản phẩm", "Loại Tivi: ([^\\.]+)" },
                { "Loại máy giặt", "Loại máy giặt: ([^\\.]+)" },
                { "Loại điều hòa", "Loại điều hòa: ([^\\.]+)" },
                { "Công suất", "Công suất: ([^\\.]+)" },
                { "Gas sử dụng", "Gas sử dụng: ([^\\.]+)" },
                { "Kháng khuẩn", "Kháng khuẩn khử mùi: ([^\\.]+)" },
                { "Công nghệ Inverter", "Công nghệ Inverter: ([^\\.]+)" },
                { "Xuất xứ", "Xuất xứ \\(Made in\\): ([^\\.]+)" },
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

    public static String extractKeyInfo(Product product) {
        String description = product.getDescription();
        if (description == null || description.isEmpty()) return "";

        List<String> keyInfo = new ArrayList<>();
        String[][] regexes = {
                { "Dung tích sử dụng", "Dung tích sử dụng: ([^\\.]+)" },
                { "Kích cỡ màn hình", "Kích cỡ màn hình: ([^\\.]+)" },
                { "Kiểu tủ", "Kiểu tủ: ([^\\.]+)" },
                { "Loại Tivi", "Loại Tivi: ([^\\.]+)" },
                { "Loại máy giặt", "Loại máy giặt: ([^\\.]+)" },
                { "Loại điều hòa", "Loại điều hòa: ([^\\.]+)" },
                { "Công suất", "Công suất: ([^\\.]+)" },
                { "Sản xuất", "Sản xuất tại: ([^\\.]+)" },
                { "Công nghệ Inverter", "Công nghệ Inverter: ([^\\.]+)" },
                { "Gas", "Gas sử dụng: ([^\\.]+)" },
                { "Kháng khuẩn", "Kháng khuẩn khử mùi: ([^\\.]+)" },
                { "Xuất xứ", "Xuất xứ \\(Made in\\): ([^\\.]+)" },
                { "Năm", "Năm ra mắt: ([^\\.]+)" }
        };

        for (String[] reg : regexes) {
            Matcher m = Pattern.compile(reg[1]).matcher(description);
            if (m.find()) keyInfo.add((reg[0].equals("Sản xuất") || reg[0].equals("Năm") ? reg[0] + ": " : "") + m.group(1).trim());
        }

        return String.join(" • ", keyInfo);
    }

    public static long parsePrice(String priceText) {
        if (priceText == null) return 0;
        String numeric = priceText.replaceAll("[^0-9]", "");
        try {
            return Long.parseLong(numeric);
        } catch (NumberFormatException e) {
            return 0;
        }
    }

    public static void inferNameFromDescription(Product product) {
        String name = product.getName();
        String description = product.getDescription();
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

            product.setName(sb.toString());
        }
    }
}
