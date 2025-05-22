package main.ecommerce.ui;

import javafx.geometry.Orientation;
import main.ecommerce.model.Product;
import main.ecommerce.model.ProductWithEmbedding;
import main.ecommerce.service.EmbeddingSearchService;
import main.ecommerce.service.ProductSearchService;
import com.google.gson.Gson;
import com.google.gson.reflect.TypeToken;
import javafx.application.Application;
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

    @Override
    public void start(Stage stage) {
        searchService = new ProductSearchService();
        try {
            Path path = Paths.get(System.getProperty("user.dir"), "src", "main", "resources", "product_texts.json");
            searchService.loadProductsFromJson(path.toString());
            allEmbeddedProducts = loadEmbeddedProducts("src/main/resources/embedded_products.json");
        } catch (Exception e) {
            e.printStackTrace();
            System.err.println("\u274c Không thể tải dữ liệu sản phẩm hoặc embedding.");
            return;
        }

        // --- Sidebar (Logo + Categories) ---
        ImageView logoView = new ImageView("file:src/main/resources/logo.png");
        logoView.setFitWidth(120);
        logoView.setPreserveRatio(true);
        logoView.setSmooth(true);
        logoView.setCache(true);
        VBox logoBox = new VBox(logoView);
        logoBox.setAlignment(Pos.CENTER);
        logoBox.setPadding(new Insets(10, 0, 20, 0));

        Label categoriesLabel = new Label("Danh mục");
        categoriesLabel.getStyleClass().add("sidebar-title");
        categoriesLabel.setStyle("-fx-font-size: 20px; -fx-font-weight: bold; margin-bottom: 10px;");
        VBox categoryBox = new VBox(15);
        categoryBox.getChildren().add(categoriesLabel);
        String[] categories = {"Tủ lạnh", "Máy giặt", "Tivi", "Điều hòa"};
        for (String category : categories) {
            Button btn = new Button(category);
            btn.getStyleClass().add("category-button");
            btn.setMaxWidth(Double.MAX_VALUE);
            btn.setOnAction(e -> showByCategory(category));
            categoryBox.getChildren().add(btn);
        }
        categoryBox.setAlignment(Pos.TOP_CENTER);
        categoryBox.setPadding(new Insets(0, 0, 0, 0));

        VBox sidebar = new VBox(logoBox, categoryBox);
        sidebar.setPadding(new Insets(20, 10, 20, 10));
        sidebar.setStyle("-fx-background-color: #f0f0f0;");
        sidebar.setPrefWidth(180);
        sidebar.setAlignment(Pos.TOP_CENTER);

        // --- Separator (extends to the right) ---
        Separator verticalSeparator = new Separator();
        verticalSeparator.setOrientation(Orientation.VERTICAL);
        verticalSeparator.setPrefHeight(600); // Adjust as needed
        verticalSeparator.setStyle("-fx-background-color: #cccccc;");

        // --- Top Bar (Search) ---
        TextField searchField = new TextField();
        searchField.setPromptText("VD: Tủ lạnh dưới 5 triệu, sản phẩm cho gia đình 5 người...");
        searchField.getStyleClass().add("search-box");
        Button searchButton = new Button("\ud83d\udd0d Tìm kiếm");
        searchButton.getStyleClass().add("search-button");
        HBox searchBox = new HBox(searchField, searchButton);
        searchBox.setAlignment(Pos.CENTER_LEFT);
        searchBox.setSpacing(10);
        searchBox.setPadding(new Insets(10, 10, 10, 10));
        searchBox.setStyle("-fx-background-color: #fff;");
        HBox.setHgrow(searchField, Priority.ALWAYS);

        // --- Checkbox under search bar ---
        CheckBox useLLMCheckBox = new CheckBox("\ud83e\udde0 Tìm kiếm nâng cao");
        useLLMCheckBox.setSelected(false);
        useLLMCheckBox.getStyleClass().add("checkbox-llm");
        VBox topBar = new VBox(searchBox, useLLMCheckBox);
        topBar.setSpacing(5);
        topBar.setPadding(new Insets(0, 0, 10, 0));
        topBar.setStyle("-fx-background-color: #fff;");

        // --- Product FlowPane (keep old logic) ---
        productFlow = new FlowPane();
        productFlow.setPadding(new Insets(20));
        productFlow.setHgap(20);
        productFlow.setVgap(20);
        productFlow.setPrefWrapLength(1000);
        productFlow.setAlignment(Pos.CENTER);

        // --- Pagination (keep old logic) ---
        paginationBox = new VBox();
        paginationBox.setAlignment(Pos.CENTER);

        VBox productSection = new VBox(15,
                topBar,
                productFlow,
                paginationBox
        );
        productSection.setAlignment(Pos.TOP_CENTER);
        productSection.setPadding(new Insets(20));

        // --- Main horizontal layout: sidebar + separator + product section ---
        HBox mainContent = new HBox(sidebar, verticalSeparator, productSection);
        mainContent.setAlignment(Pos.TOP_LEFT);
        mainContent.setSpacing(0);
        mainContent.setStyle("-fx-background: #f5f5f5;");

        // --- ScrollPane for main content ---
        ScrollPane scrollPane = new ScrollPane(mainContent);
        scrollPane.setFitToWidth(true);
        scrollPane.setStyle("-fx-background: #f5f5f5;");

        // --- Main Layout ---
        BorderPane root = new BorderPane();
        root.setCenter(scrollPane);
        root.setTop(null);
        root.setLeft(null);
        root.setBottom(null);

        Scene scene = new Scene(root, 1200, 850);
        scene.getStylesheets().add("file:src/main/resources/style.css");
        stage.setScene(scene);
        stage.setTitle("Tìm kiếm sản phẩm");
        stage.show();

        // --- Handlers and product display logic (unchanged) ---
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
                    "/Library/Frameworks/Python.framework/Versions/3.13/bin/python3",
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

            boolean isHot = isShowingHot && i < 3;
            boolean isFeatured = !isShowingHot && !searching && i < 12; // ✅ i là index toàn cục
            boolean showBadge = isHot || isFeatured;

            productFlow.getChildren().add(new ProductCardView(p, isHot, showBadge));
        }

        paginationBox.getChildren().clear();
        HBox pagination = new HBox(10);
        pagination.setAlignment(Pos.CENTER);

        Button prev = new Button("Previous page");
        prev.setDisable(currentPage == 1);
        prev.setOnAction(e -> {
            currentPage--;
            updatePage();
        });

        Button next = new Button("Next page");
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
