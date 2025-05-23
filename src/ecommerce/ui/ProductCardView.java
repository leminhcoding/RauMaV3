package ecommerce.ui;

import ecommerce.model.Product;
import ecommerce.service.ProductSearchService;
import javafx.geometry.Insets;
import javafx.scene.control.Button;
import javafx.scene.control.Label;
import javafx.scene.image.Image;
import javafx.scene.image.ImageView;
import javafx.scene.layout.*;

public class ProductCardView extends VBox {
    public ProductCardView(Product p, boolean isHot, boolean showBadge) {
        getStyleClass().add("product-card");
        setPadding(new Insets(10));
        setSpacing(8);
        setPrefWidth(280);

        // Hình ảnh sản phẩm (load an toàn)
        ImageView image = new ImageView();
        try {
            java.net.HttpURLConnection conn = (java.net.HttpURLConnection) new java.net.URL(p.getImage()).openConnection();
            conn.setRequestProperty("User-Agent", "Mozilla/5.0");
            conn.setInstanceFollowRedirects(true);
            java.io.InputStream input = conn.getInputStream();
            Image img = new Image(input, 260, 180, true, true);
            if (img.isError()) throw new Exception();
            image.setImage(img);
        } catch (Exception e) {
            image.setImage(new Image("https://via.placeholder.com/260x180?text=No+Image"));
        }

        // Badge HOT hoặc Nổi bật
        Label badge = new Label(isHot ? "HOT" : "Nổi bật");
        badge.setVisible(showBadge);
        badge.getStyleClass().add(isHot ? "hot-badge" : "badge");

        StackPane imageWrap = new StackPane(image, badge);
        StackPane.setAlignment(badge, javafx.geometry.Pos.TOP_LEFT);
        StackPane.setMargin(badge, new Insets(5));

        // Tên sản phẩm
        Label name = new Label(p.getName());
        name.getStyleClass().add("product-name");
        name.setWrapText(true);

        // Đánh giá sao vàng
        Label rating = new Label("★ " + p.getRating() + "  (" + p.getRatingCount() + ")");
        rating.setStyle("-fx-text-fill: #f1c40f; -fx-font-weight: bold;");

        // Giá
        Label price = new Label(ProductSearchService.formatPriceVietnamese(p.parsePrice()));
        price.getStyleClass().add("product-price");

        // Mô tả nhanh
        String shortDesc = p.extractKeyInfo();
        if (shortDesc.isEmpty()) shortDesc = p.getDescription(); // fallback nếu không extract được
        Label desc = new Label(shortDesc);
        desc.setWrapText(true);
        desc.getStyleClass().add("product-desc");

        // Nút chi tiết
        Button detail = new Button("Xem chi tiết");
        detail.getStyleClass().add("detail-button");
        detail.setOnAction(e -> ProductDetailPopup.show(p));

        getChildren().addAll(imageWrap, name, rating, price, desc, detail);
    }
}