package com.ecommerce.ui;

import javafx.geometry.Orientation;
import com.ecommerce.model.Product;
import com.ecommerce.model.ProductWithEmbedding;
import com.ecommerce.service.EmbeddingSearchService;
import com.ecommerce.service.ProductSearchService;
import com.google.gson.Gson;
import com.google.gson.reflect.TypeToken;
import javafx.application.Application;
import javafx.application.Platform;
import javafx.concurrent.Task;
import javafx.geometry.Insets;
import javafx.geometry.Pos;
import javafx.scene.Scene;
import javafx.scene.control.*;
import javafx.scene.image.ImageView;
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
    private Label loadingLabel = new Label("🔎 Đang tìm kiếm, vui lòng chờ...");

    @Override
    public void start(Stage stage) {
        searchService = new ProductSearchService();
        try {
            Path path = Paths.get(System.getProperty("user.dir"), "src", "main", "resources", "product_texts.json");
            searchService.loadProductsFromJson(path.toString());
            allEmbeddedProducts = loadEmbeddedProducts("src/main/resources/embedded_products.json");
        } catch (Exception e) {
            e.printStackTrace();
            System.err.println("❌ Không thể tải dữ liệu sản phẩm hoặc embedding.");
            return;
        }

        ImageView logoView = new ImageView("file:src/main/resources/logo.png");
        logoView.setFitWidth(120);
        logoView.setPreserveRatio(true);
        VBox logoBox = new VBox(logoView);
        logoBox.setAlignment(Pos.CENTER);
        logoBox.setPadding(new Insets(10, 0, 20, 0));
        logoBox.getStyleClass().add("logo-box");

        Label categoriesLabel = new Label("Danh mục");
        categoriesLabel.setStyle("-fx-font-size: 20px; -fx-font-weight: bold; margin-bottom: 10px;");
        VBox categoryBox = new VBox(15, categoriesLabel);
        String[] categories = {"Tủ lạnh", "Máy giặt", "Tivi", "Điều hòa"};
        for (String category : categories) {
            Button btn = new Button(category);
            btn.getStyleClass().add("category-button");
            btn.setMaxWidth(Double.MAX_VALUE);
            btn.setOnAction(e -> showByCategory(category));
            categoryBox.getChildren().add(btn);
        }
        categoryBox.setAlignment(Pos.TOP_CENTER);

        VBox sidebar = new VBox(logoBox, categoryBox);
        sidebar.setPadding(new Insets(20));
        sidebar.setStyle("-fx-background-color: #f0f0f0;");
        sidebar.setPrefWidth(180);
        sidebar.setAlignment(Pos.TOP_CENTER);

        Separator verticalSeparator = new Separator(Orientation.VERTICAL);
        verticalSeparator.setPrefHeight(600);

        TextField searchField = new TextField();
        searchField.setPromptText("VD: Tủ lạnh dưới 5 triệu, sản phẩm cho gia đình 5 người...");
        ToggleButton toggleLLM = new ToggleButton("Chế độ tìm kiếm nâng cao");
        toggleLLM.getStyleClass().add("llm-toggle");
        Button searchButton = new Button("🔍 Tìm kiếm");
        searchButton.getStyleClass().add("search-button");

        HBox searchBox = new HBox(10, searchField, toggleLLM, searchButton);
        searchBox.setAlignment(Pos.CENTER_LEFT);
        searchBox.setPadding(new Insets(10));
        HBox.setHgrow(searchField, Priority.ALWAYS);

        VBox topBar = new VBox(5, searchBox);
        topBar.setPadding(new Insets(0, 0, 10, 0));

        productFlow = new FlowPane();
        productFlow.setPadding(new Insets(20));
        productFlow.setHgap(20);
        productFlow.setVgap(20);
        productFlow.setPrefWrapLength(1000);
        productFlow.setAlignment(Pos.CENTER);

        loadingLabel.setVisible(false);
        loadingLabel.getStyleClass().add("loading-label");

        paginationBox = new VBox();
        paginationBox.setAlignment(Pos.CENTER);

        VBox productSection = new VBox(15, topBar, loadingLabel, productFlow, paginationBox);
        productSection.setPadding(new Insets(20));

        HBox comContent = new HBox(sidebar, verticalSeparator, productSection);
        comContent.setStyle("-fx-background: #f5f5f5;");

        ScrollPane scrollPane = new ScrollPane(comContent);
        scrollPane.setFitToWidth(true);

        BorderPane root = new BorderPane(scrollPane);

        Scene scene = new Scene(root, 1200, 850);
        scene.getStylesheets().add("file:src/main/resources/style.css");
        stage.setScene(scene);
        stage.setTitle("Tìm kiếm sản phẩm");
        stage.show();

        Runnable searchHandler = () -> {
            isShowingHot = false;
            searching = true;
            String query = searchField.getText();
            loadingLabel.setVisible(true);

            Task<Void> searchTask = new Task<>() {
                @Override
                protected Void call() {
                    List<Product> result;
                    if (toggleLLM.isSelected()) {
                        float[] queryVector = callPythonEmbeddingService(query);
                        List<String> knownCategories = Arrays.asList("Tủ lạnh", "Máy giặt", "Tivi", "Điều hòa");
                        String matchedCategory = knownCategories.stream()
                                .filter(cat -> query.toLowerCase().contains(cat.toLowerCase()))
                                .findFirst()
                                .orElse(null);

                        List<ProductWithEmbedding> matches = EmbeddingSearchService.searchByVector(
                                queryVector, query, allEmbeddedProducts, 0.4, matchedCategory
                        );
                        result = matches.stream().map(ProductWithEmbedding::toProduct).collect(Collectors.toList());
                    } else {
                        result = searchService.searchProducts(query);
                    }

                    Platform.runLater(() -> {
                        currentResults = result;
                        currentPage = 1;
                        updatePage();
                        loadingLabel.setVisible(false);
                    });
                    return null;
                }
            };
            new Thread(searchTask).start();
        };

        searchField.setOnAction(e -> searchHandler.run());
        searchButton.setOnAction(e -> searchHandler.run());
        showFeaturedProducts();
    }

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

            if (output.toString().contains("Traceback")) {
                System.err.println("❌ Python script error:\n" + output);
                return new float[0];
            }

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
                .collect(Collectors.toList());

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

            boolean isHot = isShowingHot && i < 3;
            boolean isFeatured = !isShowingHot && !searching && i < 12;
            boolean showBadge = isHot || isFeatured;

            productFlow.getChildren().add(new ProductCardView(p, isHot, showBadge));
        }

        paginationBox.getChildren().clear();
        HBox pagination = new HBox(10);
        pagination.setAlignment(Pos.CENTER);

        Button prev = new Button("Previous page");
        prev.getStyleClass().add("pagination-button");
        prev.setDisable(currentPage == 1);
        prev.setOnAction(e -> {
            currentPage--;
            updatePage();
        });

        Button next = new Button("Next page");
        prev.getStyleClass().add("pagination-button");
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
