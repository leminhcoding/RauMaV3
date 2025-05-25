// ✅ Giao diện JavaFX mới với accordion danh mục phụ và lọc giá
package ecommerce.ui;

import ecommerce.service.GeminiChatClient;
import javafx.application.Platform;
import javafx.geometry.Orientation;
import ecommerce.model.Product;
import ecommerce.service.ProductSearchService;
import ecommerce.service.ProductScorer;
import ecommerce.service.SearchHandlerService;
import ecommerce.logic.SmartSuggestionEngine;
import javafx.application.Application;
import javafx.concurrent.Task;
import javafx.geometry.Insets;
import javafx.geometry.Pos;
import javafx.scene.Scene;
import javafx.scene.control.*;
import javafx.scene.image.ImageView;
import javafx.scene.layout.*;
import javafx.stage.Stage;

import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.*;
import java.util.stream.Collectors;

public class ProductSearchApp extends Application {
    private String currentCategory = null;
    private TextField searchField;
    private Runnable searchHandler;
    private ProductSearchService searchService;
    private FlowPane productFlow;
    private List<Product> currentResults = new ArrayList<>();
    private int currentPage = 1;
    private final int itemsPerPage = 12;
    private VBox paginationBox;
    private boolean isShowingHot = false;
    private boolean searching = false;
    private Label loadingLabel = new Label("\uD83D\uDD0E Đang tìm kiếm, vui lòng chờ...");
    private List<CheckBox> priceCheckBoxes = new ArrayList<>();
    private final String[] priceLabels = {"Dưới 2 triệu", "2 - 4 triệu", "4 - 6 triệu", "6 - 10 triệu", "10 - 15 triệu", "15 - 20 triệu", "20 - 30 triệu", "Trên 30 triệu"};
    private final long[][] priceRanges = {
            {0, 2000000}, {2000000, 4000000}, {4000000, 6000000}, {6000000, 10000000},
            {10000000, 15000000}, {15000000, 20000000}, {20000000, 30000000}, {30000000, Long.MAX_VALUE}
    };

    @Override
    public void start(Stage stage) {
        searchService = new ProductSearchService();
        try {
            Path path = Paths.get(System.getProperty("user.dir"), "resources", "product_texts.json");
            searchService.loadProductsFromJson(path.toString());
        } catch (Exception e) {
            e.printStackTrace();
            System.err.println("❌ Không thể tải dữ liệu sản phẩm.");
            return;
        }

        ImageView logoView = new ImageView("file:resources/logo.png");
        logoView.setFitWidth(120);
        logoView.setPreserveRatio(true);
        VBox logoBox = new VBox(logoView);
        logoBox.setAlignment(Pos.CENTER);
        logoBox.setPadding(new Insets(10, 0, 20, 0));
        logoBox.getStyleClass().add("logo-box");

        Label categoriesLabel = new Label("Danh mục");
        categoriesLabel.setStyle("-fx-font-size: 20px; -fx-font-weight: bold; margin-bottom: 10px;");
        Map<String, List<String>> subCategories = new LinkedHashMap<>();
        subCategories.put("Tủ lạnh", Arrays.asList("Aqua", "Samsung", "LG", "Panasonic"));
        subCategories.put("Máy giặt", Arrays.asList("LG", "Toshiba", "Panasonic"));
        subCategories.put("Tivi", Arrays.asList("OLED", "QLED", "Android"));
        subCategories.put("Điều hòa", Arrays.asList("Nhật Bản", "Thái Lan", "Việt Nam"));

        Accordion accordion = new Accordion();
        for (Map.Entry<String, List<String>> entry : subCategories.entrySet()) {
            VBox subBox = new VBox(8);
            for (String sub : entry.getValue()) {
                Button subBtn = new Button(sub);
                subBtn.setMaxWidth(Double.MAX_VALUE);
                subBtn.getStyleClass().add("subcategory-button");
                subBtn.setOnAction(e -> {
                    searchField.setText(sub); // hoặc searchField.setText(entry.getKey()); nếu muốn tìm theo danh mục chính
                    currentCategory = entry.getKey();
                    searchHandler.run();
                });
                subBox.getChildren().add(subBtn);
            }
            TitledPane titledPane = new TitledPane(entry.getKey(), subBox);

// 👉 Gắn tìm kiếm khi double click vào danh mục lớn
            titledPane.setOnMouseClicked(event -> {
                if (event.getClickCount() == 2) {
                    searchField.setText(entry.getKey());
                    searchHandler.run();
                }
            });

            accordion.getPanes().add(titledPane);

        }
        VBox categoryBox = new VBox(15, categoriesLabel, accordion);
        categoryBox.setAlignment(Pos.TOP_CENTER);

        Label priceLabel = new Label("Lọc theo giá");
        priceLabel.setStyle("-fx-font-size: 16px; -fx-font-weight: bold; margin-top: 20px;");
        VBox priceBox = new VBox(8, priceLabel);
        priceBox.setPadding(new Insets(20, 0, 0, 0));
        for (String label : priceLabels) {
            CheckBox cb = new CheckBox(label);
            cb.getStyleClass().add("price-checkbox");
            priceCheckBoxes.add(cb);
            priceBox.getChildren().add(cb);

            // 👉 Gắn sự kiện khi click checkbox
            cb.setOnAction(e -> {
                if (searching) {
                    currentResults = currentResults.stream()
                            .filter(p -> currentCategory == null || p.getCategory().equalsIgnoreCase(currentCategory))
                            .filter(this::filterByPrice)
                            .sorted(Comparator.comparingDouble(ProductScorer::calculateScore).reversed())
                            .collect(Collectors.toList());
                    currentPage = 1;
                    updatePage();
                    searchHandler.run();
                } else {
                    showFeaturedProducts();
                }
            });
        }

        VBox sidebar = new VBox(logoBox, categoryBox, priceBox);
        sidebar.setPadding(new Insets(20));
        sidebar.setPrefWidth(220);
        sidebar.setAlignment(Pos.TOP_CENTER);
        sidebar.setStyle("-fx-background-color: #f0f0f0;");

        Separator verticalSeparator = new Separator(Orientation.VERTICAL);
        verticalSeparator.setStyle("-fx-background-color: #ced6e0; -fx-pref-width: 1.5px;");

        searchField = new TextField();
        searchField.getStyleClass().add("search-input");
        searchField.setPromptText("VD: Tủ lạnh dưới 5 triệu, sản phẩm cho gia đình 5 người...");
        ToggleButton toggleLLM = new ToggleButton("Tìm kiếm nâng cao");
        toggleLLM.getStyleClass().add("llm-toggle");
        Button searchButton = new Button("🔍 Tìm kiếm");
        searchButton.getStyleClass().add("search-button");

        HBox searchBox = new HBox(10, searchField, toggleLLM, searchButton);
        searchBox.setAlignment(Pos.CENTER_LEFT);
        searchBox.setPadding(new Insets(10));
        searchBox.getStyleClass().add("search-box");
        HBox.setHgrow(searchField, Priority.ALWAYS);

        VBox topBar = new VBox(5, searchBox);
        topBar.setPadding(new Insets(0, 0, 10, 0));
        topBar.getStyleClass().add("top-bar");

        productFlow = new FlowPane();
        productFlow.setPadding(new Insets(20));
        productFlow.setHgap(20);
        productFlow.setVgap(20);
        productFlow.setPrefWrapLength(1000);
        productFlow.setAlignment(Pos.CENTER);

        loadingLabel.setVisible(false);
        paginationBox = new VBox();
        paginationBox.setAlignment(Pos.CENTER);

        VBox productSection = new VBox(15, topBar, loadingLabel, productFlow, paginationBox);
        productSection.setPadding(new Insets(20));

        HBox comContent = new HBox(sidebar, verticalSeparator, productSection);
        ScrollPane scrollPane = new ScrollPane(comContent);
        scrollPane.setFitToWidth(true);

        BorderPane mainLayout = new BorderPane(scrollPane);

// Tạo chatbot thu nhỏ ở góc dưới bên phải
        VBox chatPane = new VBox(8);
        chatPane.setPadding(new Insets(10));
        chatPane.setStyle("-fx-background-color: #ffffff; -fx-border-color: #ccc; -fx-border-radius: 10; -fx-background-radius: 10;");
        chatPane.setPrefSize(300, 220);

        Label chatLabel = new Label("🤖 Chat với trợ lý sản phẩm");
        TextArea chatArea = new TextArea();
        chatArea.setWrapText(true);
        chatArea.setEditable(false);
        chatArea.setPrefHeight(120);

        HBox inputBox = new HBox(5);
        TextField inputField = new TextField();
        inputField.setPromptText("Nhập câu hỏi...");
        Button sendBtn = new Button("Gửi");
        inputBox.getChildren().addAll(inputField, sendBtn);
        HBox.setHgrow(inputField, Priority.ALWAYS);

        chatPane.getChildren().addAll(chatLabel, chatArea, inputBox);

// Đặt chatPane vào góc phải dưới
        AnchorPane floatingChat = new AnchorPane(chatPane);
        floatingChat.setPickOnBounds(false);

        chatPane.setMouseTransparent(false);
        AnchorPane.setRightAnchor(chatPane, 20.0);
        AnchorPane.setBottomAnchor(chatPane, 20.0);

// Dùng StackPane bao tất cả
        StackPane root = new StackPane();
        root.getChildren().addAll(mainLayout, floatingChat);
        Scene scene = new Scene(root, 1200, 850);  // ✅ root là StackPane bạn đã tạo ở trên
        scene.getStylesheets().add("file:resources/style.css");
        stage.setScene(scene);
        stage.setTitle("Tìm kiếm sản phẩm");
        stage.show();

        sendBtn.setOnAction(e -> {
            String userText = inputField.getText().trim();
            if (!userText.isEmpty()) {
                chatArea.appendText("Bạn: " + userText + "\n");
                inputField.clear();

                new Thread(() -> {
                    String reply = GeminiChatClient.sendPrompt(userText);
                    List<Product> suggestions = SmartSuggestionEngine.suggest(userText, searchService);

                    Platform.runLater(() -> {
                        chatArea.appendText("Bot: " + reply + "\n\n");
                        if (!suggestions.isEmpty()) {
                            chatArea.appendText("✅ Gợi ý sản phẩm:\n");
                            for (Product p : suggestions) {
                                chatArea.appendText("• " + p.getName() + " - " + p.getPrice() + "\n");
                            }
                            currentResults = suggestions;
                            currentPage = 1;
                            updatePage();
                        }
                    });
                }).start();
            }
        });

        searchHandler = () -> {
            isShowingHot = false;
            searching = true;
            String query = searchField.getText();
            loadingLabel.setVisible(true);

            Task<Void> searchTask = SearchHandlerService.createSearchTask(
                    query,
                    toggleLLM.isSelected(),
                    searchService.getAllProducts(),
                    new ArrayList<>(subCategories.keySet()),
                    searchService,
                    result -> {
                        currentResults = currentResults.stream()
                                .filter(p -> currentCategory == null || p.getCategory().equalsIgnoreCase(currentCategory))
                                .filter(this::filterByPrice)
                                .sorted(Comparator.comparingDouble(ProductScorer::calculateScore).reversed())
                                .collect(Collectors.toList());

                        currentPage = 1;
                        updatePage();
                        loadingLabel.setVisible(false);
                    }
            );

            new Thread(searchTask).start(); // ✅ đặt đúng chỗ
        };

// ✅ đặt ngoài searchHandler
        searchField.setOnAction(e -> searchHandler.run());
        searchButton.setOnAction(e -> searchHandler.run());
        showFeaturedProducts();
    }

    private boolean filterByPrice(Product p) {
        if (priceCheckBoxes.stream().noneMatch(CheckBox::isSelected)) return true;
        try {
            long price = Long.parseLong(p.getPrice().replaceAll("[^0-9]", ""));
            for (int i = 0; i < priceCheckBoxes.size(); i++) {
                if (priceCheckBoxes.get(i).isSelected()) {
                    long min = priceRanges[i][0];
                    long max = priceRanges[i][1];
                    if (price >= min && price < max) return true;
                }
            }
        } catch (Exception ignored) {}
        return false;
    }

    private void showFeaturedProducts() {
        isShowingHot = false;
        searching = false;
        currentResults = searchService.getAllProducts().stream()
                .sorted(Comparator.comparingDouble(ProductScorer::calculateScore).reversed())
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
        next.getStyleClass().add("pagination-button");
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
