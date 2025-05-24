package ecommerce.ui;

import javafx.geometry.Orientation;
import ecommerce.model.Product;
import ecommerce.model.ProductWithEmbedding;
import ecommerce.service.ProductSearchService;
import ecommerce.service.ProductScorer;
import ecommerce.service.SearchHandlerService;
import com.google.gson.Gson;
import com.google.gson.reflect.TypeToken;
import javafx.application.Application;
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
            Path path = Paths.get(System.getProperty("user.dir"), "src", "resources", "product_texts.json");
            searchService.loadProductsFromJson(path.toString());
            allEmbeddedProducts = loadEmbeddedProducts("src/resources/embedded_products.json");
        } catch (Exception e) {
            e.printStackTrace();
            System.err.println("❌ Không thể tải dữ liệu sản phẩm hoặc embedding.");
            return;
        }

        ImageView logoView = new ImageView("file:src/resources/logo.png");
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
        sidebar.getStyleClass().add("sidebar");

        Separator verticalSeparator = new Separator(Orientation.VERTICAL);
        verticalSeparator.setStyle("-fx-background-color: #ced6e0; -fx-pref-width: 1.5px;");

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
        searchBox.getStyleClass().add("search-box");
        searchBox.getStyleClass().add("search-bar-wrapper");
        searchBox.getStyleClass().add("search-container");
        searchField.getStyleClass().add("search-input");

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
        scrollPane.getStyleClass().add("scroll-pane");

        BorderPane root = new BorderPane(scrollPane);

        Scene scene = new Scene(root, 1200, 850);
        scene.getStylesheets().add("file:src/resources/style.css");
        stage.setScene(scene);
        stage.setTitle("Tìm kiếm sản phẩm");
        stage.show();

        Runnable searchHandler = () -> {
            isShowingHot = false;
            searching = true;
            String query = searchField.getText();
            loadingLabel.setVisible(true);

            Task<Void> searchTask = SearchHandlerService.createSearchTask(
                    query,
                    toggleLLM.isSelected(),
                    allEmbeddedProducts,
                    Arrays.asList("Tủ lạnh", "Máy giặt", "Tivi", "Điều hòa"),
                    searchService,
                    result -> {
                        currentResults = result;
                        currentPage = 1;
                        updatePage();
                        loadingLabel.setVisible(false);
                    }
            );
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

    private void showFeaturedProducts() {
        isShowingHot = false;
        searching = false;

        currentResults = searchService.getAllProducts().stream()
                .sorted((p1, p2) -> Double.compare(ProductScorer.calculateScore(p2), ProductScorer.calculateScore(p1)))
                .collect(Collectors.toList());

        currentPage = 1;
        updatePage();
    }

    private void showByCategory(String category) {
        isShowingHot = true;
        searching = false;
        currentResults = searchService.getAllProducts().stream()
                .filter(p -> p.getProductType().equalsIgnoreCase(category))
                .sorted((p1, p2) -> Double.compare(ProductScorer.calculateScore(p2), ProductScorer.calculateScore(p1)))
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
