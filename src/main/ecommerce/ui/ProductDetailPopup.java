package main.ecommerce.ui;

import main.ecommerce.model.Product;
import main.ecommerce.Specification;
import main.ecommerce.service.ProductSearchService;
import javafx.geometry.Insets;
import javafx.geometry.Pos;
import javafx.scene.Scene;
import javafx.scene.control.*;
import javafx.scene.image.Image;
import javafx.scene.image.ImageView;
import javafx.scene.layout.*;
import javafx.stage.Modality;
import javafx.stage.Stage;

public class ProductDetailPopup {
    public static void show(Product p) {
        Stage popup = new Stage();
        popup.initModality(Modality.APPLICATION_MODAL);
        popup.setTitle(p.getName());

        // Hình ảnh lớn
        ImageView image = new ImageView();
        try {
            java.net.URL url = new java.net.URL(p.getImage());
            java.net.HttpURLConnection conn = (java.net.HttpURLConnection) url.openConnection();
            conn.setRequestProperty("User-Agent", "Mozilla/5.0");
            conn.setInstanceFollowRedirects(true);
            java.io.InputStream input = conn.getInputStream();
            Image img = new Image(input, 400, 300, true, true);
            if (img.isError()) throw new Exception("Lỗi ảnh popup");
            image.setImage(img);
        } catch (Exception e) {
            image.setImage(new Image("https://via.placeholder.com/400x300?text=No+Image"));
        }

        // Tiêu đề + giá + đánh giá
        Label name = new Label(p.getName());
        name.setStyle("-fx-font-size: 20px; -fx-font-weight: bold;");

        Label price = new Label(ProductSearchService.formatPriceVietnamese(p.parsePrice()));
        price.setStyle("-fx-font-size: 18px; -fx-text-fill: red;");

        Label rating = new Label("★ " + p.getRating() + " (" + p.getRatingCount() + ")");
        rating.setStyle("-fx-text-fill: #f1c40f;");

        // Thông số kỹ thuật
        Label specsTitle = new Label("Thông số kỹ thuật:");
        specsTitle.setStyle("-fx-font-weight: bold; -fx-font-size: 14px;");

        GridPane specsGrid = new GridPane();
        specsGrid.setVgap(6);
        specsGrid.setHgap(20);
        specsGrid.setPadding(new Insets(10));

        Specification[] specs = p.extractSpecifications();
        for (int i = 0; i < specs.length; i++) {
            Label label = new Label(specs[i].getLabel() + ":");
            label.setStyle("-fx-font-weight: bold;");
            Label value = new Label(specs[i].getValue());
            specsGrid.addRow(i, label, value);
        }

        VBox right = new VBox(10, name, price, rating, specsTitle, specsGrid);
        right.setPadding(new Insets(10));
        right.setAlignment(Pos.TOP_LEFT);

        VBox left = new VBox(image);
        left.setAlignment(Pos.TOP_CENTER);

        HBox content = new HBox(30, left, right);
        content.setPadding(new Insets(20));

        Button close = new Button("Đóng");
        close.setOnAction(e -> popup.close());

        VBox layout = new VBox(content, close);
        layout.setAlignment(Pos.CENTER);
        layout.setSpacing(10);
        layout.setPadding(new Insets(10));

        popup.setScene(new Scene(layout));
        popup.showAndWait();
    }
}
