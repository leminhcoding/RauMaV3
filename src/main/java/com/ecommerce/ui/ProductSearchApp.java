package com.ecommerce.ui;

import com.ecommerce.model.Product;
import com.ecommerce.model.ProductWithEmbedding;
import com.ecommerce.service.EmbeddingSearchService;
import com.ecommerce.service.ProductSearchService;
import com.google.gson.Gson;
import com.google.gson.reflect.TypeToken;
import javafx.application.Application;
import javafx.geometry.Insets;
import javafx.geometry.Pos;
import javafx.scene.Scene;
import javafx.scene.control.*;
import javafx.scene.layout.*;
import javafx.stage.Stage;

import java.io.*;
import java.lang.reflect.Type;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.*;
import java.util.stream.Collectors;

public class ProductSearchApp extends Application {
    private ProductSearchService searchService;
    private FlowPane productFlow;
    private List<Product> currentResults = new ArrayList<>();
    private int currentPage = 1;
    private final int itemsPerPage = 12;
    private VBox paginationBox;
    private boolean isShowingHot = false;
    private boolean searching = false;
    private List<ProductWithEmbedding> allEmbeddedProducts = new ArrayList<>();

    @Override
    public void start(Stage stage) {
        searchService = new ProductSearchService();
        try {
            Path path = Paths.get(System.getProperty("user.dir"), "src", "main", "resources", "product_texts.json");
            searchService.loadProductsFromJson(path.toString());

            allEmbeddedProducts = loadEmbeddedProducts("src/main/resources/embedded_products.json");

        } catch (Exception e) {
            e.printStackTrace();
            System.err.println("\u274c Kh\u00f4ng th\u1ec3 t\u1ea3i d\u1eef li\u1ec7u s\u1ea3n ph\u1ea9m ho\u1eb7c embedding.");
            return;
        }

        Label title = new Label("T\u00ecm ki\u1ebfm S\u1ea3n ph\u1ea9m Th\u00f4ng minh");
        title.getStyleClass().add("title");

        TextField searchField = new TextField();
        searchField.setPromptText("VD: T\u1ee7 l\u1ea1nh d\u01b0\u1edbi 5 tri\u1ec7u, s\u1ea3n ph\u1ea9m cho gia \u0111\u00ecnh 5 ng\u01b0\u1eddi...");
        searchField.getStyleClass().add("search-box");

        Button searchButton = new Button("\ud83d\udd0d T\u00ecm ki\u1ebfm");
        searchButton.getStyleClass().add("search-button");

        HBox searchBox = new HBox(searchField, searchButton);
        searchBox.setAlignment(Pos.CENTER);
        searchBox.setSpacing(10);
        searchBox.setPadding(new Insets(10));
        searchBox.getStyleClass().add("search-container");

        CheckBox useLLMCheckBox = new CheckBox("\ud83e\udde0 T\u00ecm ki\u1ebfm n\u00e2ng cao");
        useLLMCheckBox.setSelected(false);
        useLLMCheckBox.getStyleClass().add("checkbox-llm");

        Label suggestion = new Label("G\u1ee3i \u00fd: \"T\u1ee7 l\u1ea1nh d\u01b0\u1edbi 10 tri\u1ec7u\", \"S\u1ea3n ph\u1ea9m cho 4-5 ng\u01b0\u1eddi\"...");
        suggestion.getStyleClass().add("suggestion");

        VBox searchSection = new VBox(5, searchBox, useLLMCheckBox, suggestion);
        searchSection.setAlignment(Pos.CENTER);

        HBox categoryButtons = new HBox(10);
        categoryButtons.setAlignment(Pos.CENTER);
        categoryButtons.setPadding(new Insets(10));

        String[] categories = {"T\u1ee7 l\u1ea1nh", "M\u00e1y gi\u1eb7t", "Tivi", "\u0110i\u1ec1u h\u00f2a"};
        for (String category : categories) {
            Button btn = new Button(category);
            btn.getStyleClass().add("category-button");
            btn.setOnAction(e -> showByCategory(category));
            categoryButtons.getChildren().add(btn);
        }

        productFlow = new FlowPane();
        productFlow.setPadding(new Insets(20));
        productFlow.setHgap(20);
        productFlow.setVgap(20);
        productFlow.setPrefWrapLength(1000);
        productFlow.setAlignment(Pos.CENTER);

        paginationBox = new VBox();
        paginationBox.setAlignment(Pos.CENTER);

        VBox content = new VBox(15,
                title,
                searchSection,
                categoryButtons,
                productFlow,
                paginationBox
        );
        content.setAlignment(Pos.TOP_CENTER);
        content.setPadding(new Insets(20));

        ScrollPane scrollPane = new ScrollPane(content);
        scrollPane.setFitToWidth(true);
        scrollPane.setStyle("-fx-background: #f5f5f5;");

        Scene scene = new Scene(scrollPane, 1000, 800);
        scene.getStylesheets().add("file:src/main/resources/style.css");

        stage.setScene(scene);
        stage.setTitle("T\u00ecm ki\u1ebfm s\u1ea3n ph\u1ea9m");
        stage.show();

        Runnable searchHandler = () -> {
            isShowingHot = false;
            searching = true;

            String query = searchField.getText();

            if (useLLMCheckBox.isSelected()) {
                float[] queryVector = callPythonEmbeddingService(query);
                List<ProductWithEmbedding> matches = EmbeddingSearchService.searchByVector(queryVector, allEmbeddedProducts, 12);

                currentResults = matches.stream()
                        .map(p -> new Product(
                                p.getTenSanPham(),
                                p.getAnh(),
                                p.getGia(),
                                p.getMoTaSanPham(),
                                p.getDiemDanhGiaTrungBinh(),
                                p.getSoLuotDanhGia(),
                                p.getNguonDuLieu(),
                                p.getLoaiSanPham()
                        )).collect(Collectors.toList());
            } else {
                currentResults = searchService.searchProducts(query);
            }

            currentPage = 1;
            updatePage();
        };

        searchField.setOnAction(e -> searchHandler.run());
        searchButton.setOnAction(e -> searchHandler.run());

        showFeaturedProducts();
    }

    // --- Hàm phụ trợ ---

    public static List<ProductWithEmbedding> loadEmbeddedProducts(String filePath) {
        try (Reader reader = new FileReader(filePath)) {
            Gson gson = new Gson();
            Type listType = new TypeToken<List<ProductWithEmbedding>>() {}.getType();
            return gson.fromJson(reader, listType);
        } catch (IOException e) {
            e.printStackTrace();
            return new ArrayList<>();
        }
    }

    public static float[] callPythonEmbeddingService(String query) {
        try {
            ProcessBuilder pb = new ProcessBuilder(
                    "C:/Users/Dell/AppData/Local/Programs/Python/Python310/python.exe",
                    "embed_query.py",
                    query
            );
            pb.redirectErrorStream(true);
            Process process = pb.start();

            StringBuilder output = new StringBuilder();
            try (BufferedReader reader = new BufferedReader(new InputStreamReader(process.getInputStream()))) {
                String line;
                while ((line = reader.readLine()) != null) {
                    output.append(line).append("\n");
                }
            }

            // ✅ Nếu có dòng bắt đầu bằng "Traceback", tức là lỗi
            if (output.toString().contains("Traceback")) {
                System.err.println("❌ Python script error:\n" + output);
                return new float[0];
            }

            // ✅ Lấy dòng đầu tiên có chứa vector
            String[] parts = output.toString().trim().replace("[", "").replace("]", "").split(",");
            float[] vector = new float[parts.length];
            for (int i = 0; i < parts.length; i++) {
                vector[i] = Float.parseFloat(parts[i].trim());
            }
            return vector;

        } catch (Exception e) {
            e.printStackTrace();
            return new float[0];
        }
    }

    private void showFeaturedProducts() {
        isShowingHot = false;
        searching = false;

        currentResults = searchService.getAllProducts().stream()
                .sorted((p1, p2) -> {
                    try {
                        double r1 = Double.parseDouble(p1.getRating());
                        double r2 = Double.parseDouble(p2.getRating());
                        int v1 = Integer.parseInt(p1.getRatingCount().replaceAll("[^0-9]", ""));
                        int v2 = Integer.parseInt(p2.getRatingCount().replaceAll("[^0-9]", ""));
                        double score1 = r1 * Math.log10(1 + v1);
                        double score2 = r2 * Math.log10(1 + v2);
                        return Double.compare(score2, score1);
                    } catch (Exception e) {
                        return 0;
                    }
                })
                .collect(Collectors.toList()); // ❗ Không giới hạn 12 nữa

        currentPage = 1;
        updatePage();
    }

    private void showByCategory(String category) {
        isShowingHot = true;
        searching = false;
        currentResults = searchService.getAllProducts().stream()
                .filter(p -> p.getProductType().equalsIgnoreCase(category))
                .sorted((p1, p2) -> {
                    try {
                        double r1 = Double.parseDouble(p1.getRating());
                        double r2 = Double.parseDouble(p2.getRating());
                        int v1 = Integer.parseInt(p1.getRatingCount().replaceAll("[^0-9]", ""));
                        int v2 = Integer.parseInt(p2.getRatingCount().replaceAll("[^0-9]", ""));
                        double score1 = r1 * Math.log10(1 + v1);
                        double score2 = r2 * Math.log10(1 + v2);
                        return Double.compare(score2, score1);
                    } catch (Exception e) {
                        return 0;
                    }
                })
                .collect(Collectors.toList());
        currentPage = 1;
        updatePage();
    }

    private void updatePage() {
        productFlow.getChildren().clear();
        int totalPages = (int) Math.ceil((double) currentResults.size() / itemsPerPage);
        int from = (currentPage - 1) * itemsPerPage;
        int to = Math.min(from + itemsPerPage, currentResults.size());

        for (int i = from; i < to; i++) {
            Product p = currentResults.get(i);

            boolean isHot = isShowingHot && i < 4;
            boolean isFeatured = !isShowingHot && !searching && i < 12; // ✅ i là index toàn cục
            boolean showBadge = isHot || isFeatured;

            productFlow.getChildren().add(new ProductCardView(p, isHot, showBadge));
        }

        paginationBox.getChildren().clear();
        HBox pagination = new HBox(10);
        pagination.setAlignment(Pos.CENTER);

        Button prev = new Button("<<");
        prev.setDisable(currentPage == 1);
        prev.setOnAction(e -> {
            currentPage--;
            updatePage();
        });

        Button next = new Button(">>");
        next.setDisable(currentPage == totalPages);
        next.setOnAction(e -> {
            currentPage++;
            updatePage();
        });

        Label pageInfo = new Label("Trang " + currentPage + " / " + totalPages);

        pagination.getChildren().addAll(prev, pageInfo, next);
        paginationBox.getChildren().add(pagination);
    }

}
