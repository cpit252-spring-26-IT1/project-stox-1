package sa.edu.kau.fcit.cpit252.project.ui;

import javafx.collections.FXCollections;
import javafx.collections.ObservableList;
import javafx.geometry.Insets;
import javafx.geometry.Pos;
import javafx.scene.Scene;
import javafx.scene.control.*;
import javafx.scene.control.cell.PropertyValueFactory;
import javafx.scene.layout.*;
import javafx.scene.paint.Color;
import javafx.scene.text.Font;
import javafx.scene.text.FontWeight;
import javafx.stage.Modality;
import javafx.stage.Stage;
import sa.edu.kau.fcit.cpit252.project.dao.StockDAO;
import sa.edu.kau.fcit.cpit252.project.dao.SqliteStockDAO;
import sa.edu.kau.fcit.cpit252.project.model.Portfolio;
import sa.edu.kau.fcit.cpit252.project.model.Stock;

import java.util.List;
import java.util.Map;
import java.util.concurrent.CompletableFuture;
import java.util.stream.Collectors;
import javafx.application.Platform;
import sa.edu.kau.fcit.cpit252.project.api.FetcherFactory;
import sa.edu.kau.fcit.cpit252.project.api.PriceFetcher;
import javafx.scene.image.ImageView;
import sa.edu.kau.fcit.cpit252.project.api.LogoService;
import javafx.geometry.Side;
import sa.edu.kau.fcit.cpit252.project.model.StockSuggestion;
import sa.edu.kau.fcit.cpit252.project.api.StockSuggestionService;
import javafx.scene.chart.*;

/**
 * Main JavaFX view for stoX.
 * Owns all UI construction and user interaction logic.
 * Communicates with the data layer through the StockDAO interface.
 */
public class MainView {

    // ── Palette ───────────────────────────────────────────────────────────────

    private static final String BG_DEEP = "#0d1117";
    private static final String BG_PANEL = "#161b22";
    private static final String BG_ROW = "#1c2128";
    private static final String ACCENT = "#00d4aa";
    private static final String ACCENT_DIM = "#00a882";
    private static final String TEXT_PRI = "#e6edf3";
    private static final String TEXT_SEC = "#8b949e";
    private static final String BORDER = "#30363d";
    private static final String DANGER = "#f85149";
    private static final String COLOR_PROFIT = "#3fb950";

    private static final String ALL_PORTFOLIOS = "All Portfolios";
    private static final String MARKET_US = "US Market - Finnhub";
    private static final String MARKET_SA = "Saudi Market - Tadawul";

    private static final String FILTER_US = "🇺🇸  US Market";
    private static final String FILTER_SA = "🇸🇦  Saudi Market";

    private static final String CURRENCY_ORIGINAL = "Original";
    private static final String CURRENCY_USD = "USD $";
    private static final String CURRENCY_SAR = "SAR ﷼";

    private static final double USD_TO_SAR = 3.75;
    private static final double SAR_TO_USD = 1.0 / USD_TO_SAR;

    // ── State ─────────────────────────────────────────────────────────────────

    private final StockDAO dao = new SqliteStockDAO();
    private final ObservableList<Stock> tableData = FXCollections.observableArrayList();

    private Stage primaryStage;
    private ComboBox<String> portfolioFilter;
    private ComboBox<String> currencySelector;
    private Label totalValueLabel;
    private Label totalMarketValueLabel;
    private Label stockCountLabel;
    private Label portfolioCountLabel;
    private Label marketValuePercentageLabel;
    private TableView<Stock> table;
    private boolean isRefreshingDropdown = false;
    private PieChart pieChart;
    private BarChart<String, Number> barChart;

    // ─────────────────────────────────────────────────────────────────────────

    public void show(Stage stage) {
        this.primaryStage = stage;

        // Programmatically load Plus Jakarta Sans fonts to register them globally in
        // JavaFX
        try {
            Font.loadFont(getClass().getResourceAsStream("/fonts/PlusJakartaSans-Regular.ttf"), 12);
            Font.loadFont(getClass().getResourceAsStream("/fonts/PlusJakartaSans-SemiBold.ttf"), 12);
            Font.loadFont(getClass().getResourceAsStream("/fonts/PlusJakartaSans-Bold.ttf"), 12);
        } catch (Exception e) {
            System.err.println("Could not load Plus Jakarta Sans fonts: " + e.getMessage());
        }

        stage.setTitle("stoX — Portfolio Manager");
        stage.setMinWidth(1280);
        stage.setMinHeight(720);

        BorderPane root = new BorderPane();
        root.setStyle("-fx-background-color:" + BG_DEEP + ";");
        root.setTop(buildHeader());
        root.setCenter(buildCenter());

        Scene scene = new Scene(root, 1000, 680);
        try {
            scene.getStylesheets().add(getClass().getResource("/styles.css").toExternalForm());
        } catch (Exception e) {
            System.err.println("Could not load styles.css stylesheet: " + e.getMessage());
        }
        stage.setScene(scene);
        stage.show();

        refreshDropdown();
        refreshView(ALL_PORTFOLIOS);
    }

    // ── Header ────────────────────────────────────────────────────────────────

    private HBox buildHeader() {
        HBox header = new HBox();
        header.setStyle("-fx-background-color:" + BG_PANEL +
                ";-fx-border-color:" + BORDER + ";-fx-border-width:0 0 1 0;");
        header.setPadding(new Insets(16, 24, 16, 24));
        header.setAlignment(Pos.CENTER_LEFT);

        Label logo = makeLabel("sto", "Georgia", 26, TEXT_PRI, true);
        Label logoX = makeLabel("X", "Georgia", 26, ACCENT, true);
        Label tag = makeLabel("  Portfolio Manager", "Courier New", 13, TEXT_SEC, false);

        Region spacer = new Region();
        HBox.setHgrow(spacer, Priority.ALWAYS);

        Button addBtn = makeButton("＋  Add Stock", ACCENT, BG_DEEP);
        addBtn.setOnAction(e -> openAddStockDialog());

        header.getChildren().addAll(logo, logoX, tag, spacer, addBtn);
        return header;
    }

    // ── Center ────────────────────────────────────────────────────────────────

    private VBox buildCenter() {
        VBox center = new VBox(16);
        center.setPadding(new Insets(20, 24, 20, 24));

        TabPane tabPane = new TabPane();
        tabPane.setTabClosingPolicy(TabPane.TabClosingPolicy.UNAVAILABLE);

        Tab tableTab = new Tab("📋  Asset List");
        tableTab.setContent(buildTable());

        Tab dashboardTab = new Tab("📊  Performance Dashboard");
        dashboardTab.setContent(buildDashboardView());

        tabPane.getTabs().addAll(tableTab, dashboardTab);
        VBox.setVgrow(tabPane, Priority.ALWAYS);

        center.getChildren().addAll(buildStatsRow(), buildFilterBar(), tabPane);
        return center;
    }

    private HBox buildDashboardView() {
        HBox dashboard = new HBox(16);
        dashboard.setPadding(new Insets(16, 0, 16, 0));

        // Left Card: Asset Allocation
        VBox allocationCard = new VBox(12);
        allocationCard.setStyle("-fx-background-color:" + BG_PANEL + ";-fx-background-radius:8;-fx-border-color:"
                + BORDER + ";-fx-border-radius:8;");
        allocationCard.setPadding(new Insets(16));
        HBox.setHgrow(allocationCard, Priority.ALWAYS);
        Label allocTitle = makeLabel("ASSET ALLOCATION", "Plus Jakarta Sans", 14, TEXT_SEC, true);

        pieChart = new PieChart();
        pieChart.setLegendSide(Side.RIGHT);
        pieChart.setLabelsVisible(true);
        VBox.setVgrow(pieChart, Priority.ALWAYS);
        allocationCard.getChildren().addAll(allocTitle, pieChart);

        // Right Card: Performance Analysis (Cost vs Market Value)
        VBox performanceCard = new VBox(12);
        performanceCard.setStyle("-fx-background-color:" + BG_PANEL + ";-fx-background-radius:8;-fx-border-color:"
                + BORDER + ";-fx-border-radius:8;");
        performanceCard.setPadding(new Insets(16));
        HBox.setHgrow(performanceCard, Priority.ALWAYS);
        Label perfTitle = makeLabel("COST VS CURRENT VALUE", "Plus Jakarta Sans", 14, TEXT_SEC, true);

        CategoryAxis xAxis = new CategoryAxis();
        NumberAxis yAxis = new NumberAxis();
        xAxis.setLabel("Ticker");
        yAxis.setLabel("Value");

        barChart = new BarChart<>(xAxis, yAxis);
        barChart.setAnimated(true);
        VBox.setVgrow(barChart, Priority.ALWAYS);
        performanceCard.getChildren().addAll(perfTitle, barChart);

        dashboard.getChildren().addAll(allocationCard, performanceCard);
        return dashboard;
    }

    private void updateCharts() {
        if (pieChart == null || barChart == null)
            return;

        List<Stock> stocks = tableData;
        if (stocks == null || stocks.isEmpty()) {
            pieChart.getData().clear();
            barChart.getData().clear();
            return;
        }

        String displayCurrency = currencySelector.getValue();
        boolean convertToUSD = false;
        boolean convertToSAR = false;
        if (CURRENCY_USD.equals(displayCurrency)) {
            convertToUSD = true;
        } else if (CURRENCY_SAR.equals(displayCurrency)) {
            convertToSAR = true;
        } else {
            convertToUSD = true; // Normalize mixed currencies to USD for consistent charts
        }

        double totalChartValue = 0.0;
        for (Stock s : stocks) {
            double price = s.getCurrentPrice() > 0 ? s.getCurrentPrice() : s.getAverageBuyPrice();
            double value = s.getQuantity() * price;
            if (convertToUSD && "SAR".equals(s.getCurrencySymbol())) {
                value *= SAR_TO_USD;
            } else if (convertToSAR && "USD".equals(s.getCurrencySymbol())) {
                value *= USD_TO_SAR;
            }
            totalChartValue += value;
        }

        // 1. Update Pie Chart
        ObservableList<PieChart.Data> pieData = FXCollections.observableArrayList();
        for (Stock s : stocks) {
            double price = s.getCurrentPrice() > 0 ? s.getCurrentPrice() : s.getAverageBuyPrice();
            double value = s.getQuantity() * price;
            if (convertToUSD && "SAR".equals(s.getCurrencySymbol())) {
                value *= SAR_TO_USD;
            } else if (convertToSAR && "USD".equals(s.getCurrencySymbol())) {
                value *= USD_TO_SAR;
            }
            if (value > 0) {
                pieData.add(new PieChart.Data(s.getTicker(), value));
            }
        }
        pieChart.setData(pieData);

        double finalTotal = totalChartValue;
        String currencyLabel = convertToSAR ? "SAR" : "USD";

        for (PieChart.Data data : pieChart.getData()) {
            double val = data.getPieValue();
            double percentage = finalTotal > 0 ? (val / finalTotal) * 100 : 0.0;

            Tooltip tooltip = new Tooltip(String.format("%s\nMarket Value: %s\nAllocation: %.1f%%",
                    data.getName(),
                    String.format("%s %,.2f", currencyLabel, val),
                    percentage));
            tooltip.setStyle("-fx-font-family: 'Plus Jakarta Sans'; -fx-font-size: 12px; " +
                    "-fx-background-color: #0d1117; -fx-text-fill: #e6edf3; " +
                    "-fx-border-color: #30363d; -fx-border-radius: 4px; -fx-padding: 8px;");

            if (data.getNode() != null) {
                Tooltip.install(data.getNode(), tooltip);
                data.getNode().setOnMouseEntered(e -> {
                    data.getNode().setScaleX(1.05);
                    data.getNode().setScaleY(1.05);
                    data.getNode().setCursor(javafx.scene.Cursor.HAND);
                });
                data.getNode().setOnMouseExited(e -> {
                    data.getNode().setScaleX(1.0);
                    data.getNode().setScaleY(1.0);
                });
            } else {
                data.nodeProperty().addListener((obs, oldNode, newNode) -> {
                    if (newNode != null) {
                        Tooltip.install(newNode, tooltip);
                        newNode.setOnMouseEntered(e -> {
                            newNode.setScaleX(1.05);
                            newNode.setScaleY(1.05);
                            newNode.setCursor(javafx.scene.Cursor.HAND);
                        });
                        newNode.setOnMouseExited(e -> {
                            newNode.setScaleX(1.0);
                            newNode.setScaleY(1.0);
                        });
                    }
                });
            }
        }

        // 2. Update Bar Chart
        barChart.getData().clear();

        XYChart.Series<String, Number> costSeries = new XYChart.Series<>();
        costSeries.setName("Total Cost Basis");

        XYChart.Series<String, Number> valueSeries = new XYChart.Series<>();
        valueSeries.setName("Current Market Value");

        for (Stock s : stocks) {
            double cost = s.getQuantity() * s.getAverageBuyPrice();
            if (convertToUSD && "SAR".equals(s.getCurrencySymbol())) {
                cost *= SAR_TO_USD;
            } else if (convertToSAR && "USD".equals(s.getCurrencySymbol())) {
                cost *= USD_TO_SAR;
            }
            costSeries.getData().add(new XYChart.Data<>(s.getTicker(), cost));

            double price = s.getCurrentPrice() > 0 ? s.getCurrentPrice() : s.getAverageBuyPrice();
            double curVal = s.getQuantity() * price;
            if (convertToUSD && "SAR".equals(s.getCurrencySymbol())) {
                curVal *= SAR_TO_USD;
            } else if (convertToSAR && "USD".equals(s.getCurrencySymbol())) {
                curVal *= USD_TO_SAR;
            }
            valueSeries.getData().add(new XYChart.Data<>(s.getTicker(), curVal));
        }

        barChart.getData().addAll(costSeries, valueSeries);

        for (XYChart.Series<String, Number> series : barChart.getData()) {
            for (XYChart.Data<String, Number> data : series.getData()) {
                Tooltip tooltip = new Tooltip(String.format("%s\n%s: %s",
                        data.getXValue(),
                        series.getName(),
                        String.format("%s %,.2f", currencyLabel, data.getYValue().doubleValue())));
                tooltip.setStyle("-fx-font-family: 'Plus Jakarta Sans'; -fx-font-size: 12px; " +
                        "-fx-background-color: #0d1117; -fx-text-fill: #e6edf3; " +
                        "-fx-border-color: #30363d; -fx-border-radius: 4px; -fx-padding: 8px;");

                if (data.getNode() != null) {
                    Tooltip.install(data.getNode(), tooltip);
                    data.getNode().setOnMouseEntered(e -> {
                        data.getNode().setOpacity(0.85);
                        data.getNode().setCursor(javafx.scene.Cursor.HAND);
                    });
                    data.getNode().setOnMouseExited(e -> {
                        data.getNode().setOpacity(1.0);
                    });
                } else {
                    data.nodeProperty().addListener((obs, oldNode, newNode) -> {
                        if (newNode != null) {
                            Tooltip.install(newNode, tooltip);
                            newNode.setOnMouseEntered(e -> {
                                newNode.setOpacity(0.85);
                                newNode.setCursor(javafx.scene.Cursor.HAND);
                            });
                            newNode.setOnMouseExited(e -> {
                                newNode.setOpacity(1.0);
                            });
                        }
                    });
                }
            }
        }
    }

    // ── Stats row — Total Value card + Portfolio count card ───────────────────

    private HBox buildStatsRow() {
        // Card 1: Total value
        totalValueLabel = makeLabel("$ 0.00", "Courier New", 30, ACCENT, true);
        stockCountLabel = makeLabel("0 assets", "Courier New", 13, TEXT_SEC, false);
        VBox valueCard = new VBox(3,
                makeLabel("TOTAL VALUE", "Courier New", 11, TEXT_SEC, false),
                totalValueLabel,
                stockCountLabel);
        styleCard(valueCard);

        // Card 2: Total market value
        totalMarketValueLabel = makeLabel("$ 0.00", "Courier New", 30, ACCENT, true);
        marketValuePercentageLabel = makeLabel("live prices", "Courier New", 13, TEXT_SEC, false);
        VBox marketValueCard = new VBox(3,
                makeLabel("TOTAL MARKET VALUE", "Courier New", 11, TEXT_SEC, false),
                totalMarketValueLabel,
                marketValuePercentageLabel);
        styleCard(marketValueCard);

        // Card 3 — portfolio count
        portfolioCountLabel = makeLabel("0", "Courier New", 30, ACCENT, true);
        VBox portfolioCard = new VBox(3,
                makeLabel("PORTFOLIOS", "Courier New", 11, TEXT_SEC, false),
                portfolioCountLabel,
                makeLabel("distinct portfolios", "Courier New", 13, TEXT_SEC, false));
        styleCard(portfolioCard);

        HBox row = new HBox(16, valueCard, marketValueCard, portfolioCard);
        return row;
    }

    private void styleCard(VBox card) {
        card.setStyle("-fx-background-color:" + BG_PANEL +
                ";-fx-background-radius:8;-fx-border-color:" + BORDER +
                ";-fx-border-radius:8;");
        card.setPadding(new Insets(14, 22, 14, 22));
    }

    // ── Filter bar ────────────────────────────────────────────────────────────

    private HBox buildFilterBar() {
        portfolioFilter = new ComboBox<>();
        portfolioFilter.setPrefWidth(260);
        portfolioFilter.setStyle(
                "-fx-background-color:" + BG_PANEL +
                        ";-fx-border-color:" + BORDER + ";-fx-border-radius:6;" +
                        "-fx-background-radius:6;-fx-text-fill:" + TEXT_PRI +
                        ";-fx-font-family:'Plus Jakarta Sans';-fx-font-size:13;");

        portfolioFilter.setOnAction(e -> {
            if (isRefreshingDropdown)
                return;
            String selected = portfolioFilter.getValue();
            if (selected != null) {
                // Strip the count badge "(n)" before querying — e.g. "Tech (3)" → "Tech"
                refreshView(stripBadge(selected));
            }
        });

        Button refreshBtn = makeButton("↻  Refresh", BG_ROW, ACCENT);
        refreshBtn.setOnAction(e -> {
            refreshDropdown();
            refreshView(currentFilter());
        });

        currencySelector = new ComboBox<>(FXCollections.observableArrayList(
                CURRENCY_ORIGINAL, CURRENCY_USD, CURRENCY_SAR));
        currencySelector.setValue(CURRENCY_ORIGINAL);
        currencySelector.setPrefWidth(130);
        currencySelector.setStyle(
                "-fx-background-color:" + BG_PANEL +
                        ";-fx-border-color:" + BORDER + ";-fx-border-radius:6;" +
                        "-fx-background-radius:6;-fx-text-fill:" + TEXT_PRI +
                        ";-fx-font-family:'Plus Jakarta Sans';-fx-font-size:13;");
        currencySelector.setButtonCell(new ListCell<>() {
            @Override
            protected void updateItem(String item, boolean empty) {
                super.updateItem(item, empty);
                setText(empty || item == null ? "" : item);
                setStyle("-fx-text-fill:" + TEXT_PRI + ";-fx-font-family:'Plus Jakarta Sans';" +
                        "-fx-font-size:13;-fx-background-color:transparent;");
            }
        });
        currencySelector.setOnAction(e -> refreshView(currentFilter()));

        HBox bar = new HBox(12,
                makeLabel("Portfolio:", "Plus Jakarta Sans", 13, TEXT_SEC, false),
                portfolioFilter,
                makeLabel("Currency:", "Plus Jakarta Sans", 13, TEXT_SEC, false),
                currencySelector,
                refreshBtn);
        bar.setAlignment(Pos.CENTER_LEFT);
        return bar;
    }

    // ── Table ─────────────────────────────────────────────────────────────────

    @SuppressWarnings("unchecked")
    private TableView<Stock> buildTable() {
        this.table = new TableView<>(tableData);
        table.setStyle("-fx-background-color:" + BG_PANEL + ";-fx-border-color:" + BORDER +
                ";-fx-border-radius:8;-fx-table-cell-border-color:" + BORDER + ";");
        table.setColumnResizePolicy(TableView.CONSTRAINED_RESIZE_POLICY);
        VBox.setVgrow(table, Priority.ALWAYS);
        table.setPlaceholder(makeLabel(
                "No stocks yet. Click '＋ Add Stock' to begin.",
                "Plus Jakarta Sans", 13, TEXT_SEC, false));

        TableColumn<Stock, Void> logoCol = new TableColumn<>("");
        logoCol.setMinWidth(40);
        logoCol.setMaxWidth(40);
        logoCol.setCellFactory(tc -> new TableCell<>() {
            private final ImageView iv = new ImageView();
            {
                iv.setFitWidth(24);
                iv.setFitHeight(24);
            }

            @Override
            protected void updateItem(Void item, boolean empty) {
                super.updateItem(item, empty);
                if (empty) {
                    setGraphic(null);
                } else {
                    Stock s = getTableView().getItems().get(getIndex());
                    iv.setImage(null);
                    LogoService.loadLogoAsync(s.getTicker(), s.getMarket(), iv::setImage);
                    setGraphic(iv);
                    setAlignment(Pos.CENTER);
                }
            }
        });

        TableColumn<Stock, String> tickerCol = makeCol("TICKER", "ticker", 80);
        tickerCol.setMaxWidth(80);

        TableColumn<Stock, String> nameCol = new TableColumn<>("STOCK NAME");
        nameCol.setCellValueFactory(cd -> new javafx.beans.property.SimpleStringProperty(
                StockSuggestionService.getStockName(cd.getValue().getTicker())));
        nameCol.setMinWidth(220);
        nameCol.setStyle("-fx-font-family:'Plus Jakarta Sans';");

        TableColumn<Stock, String> marketCol = makeCol("MARKET", "market", 140);
        TableColumn<Stock, String> portfolioCol = makeCol("PORTFOLIO", "portfolioName", 160);
        TableColumn<Stock, Double> priceCol = makeCol("CURRENT PRICE ($)", "currentPrice", 140);
        TableColumn<Stock, Double> qtyCol = makeCol("QTY", "quantity", 70);
        TableColumn<Stock, Double> avgCol = makeCol("AVG BUY ($)", "averageBuyPrice", 120);

        TableColumn<Stock, String> valueCol = new TableColumn<>("TOTAL VALUE");
        valueCol.setCellValueFactory(cd -> new javafx.beans.property.SimpleStringProperty(
                convertAndFormat(cd.getValue().getValue(), cd.getValue().getCurrencySymbol())));
        valueCol.setCellFactory(tc -> new TableCell<>() {
            @Override
            protected void updateItem(String item, boolean empty) {
                super.updateItem(item, empty);
                if (empty || item == null) {
                    setText(null);
                    return;
                }
                setText(item);
                setStyle("-fx-text-fill:" + ACCENT + ";-fx-font-family:'Plus Jakarta Sans';" +
                        "-fx-alignment:CENTER-RIGHT;");
            }
        });

        TableColumn<Stock, String> pnlCol = new TableColumn<>("P&L");
        pnlCol.setCellValueFactory(cd -> {
            Stock stock = cd.getValue();
            if (stock.getCurrentPrice() <= 0) {
                return new javafx.beans.property.SimpleStringProperty("—");
            }
            double pnl = stock.getPnL();
            String sign = pnl >= 0 ? "+" : "";
            return new javafx.beans.property.SimpleStringProperty(
                    sign + convertAndFormat(pnl, stock.getCurrencySymbol()));
        });
        pnlCol.setCellFactory(tc -> new TableCell<>() {
            @Override
            protected void updateItem(String item, boolean empty) {
                super.updateItem(item, empty);
                if (empty || item == null) {
                    setText(null);
                    setStyle("");
                    return;
                }
                setText(item);
                // Pick color based on the value
                String textColor;
                if (item.equals("—")) {
                    textColor = TEXT_SEC; // grey — still loading
                } else if (item.startsWith("+")) {
                    textColor = COLOR_PROFIT; // green — profit
                } else {
                    textColor = DANGER; // red — loss
                }
                setStyle("-fx-text-fill:" + textColor + ";" +
                        "-fx-font-family:'Plus Jakarta Sans';" +
                        "-fx-font-weight:bold;" +
                        "-fx-alignment:CENTER-RIGHT;");
            }
        });

        TableColumn<Stock, String> marketValueCol = new TableColumn<>("MARKET VALUE");
        marketValueCol.setCellValueFactory(cd -> {
            Stock s = cd.getValue();
            double price = s.getCurrentPrice() > 0 ? s.getCurrentPrice() : s.getAverageBuyPrice();
            double val = s.getQuantity() * price;
            return new javafx.beans.property.SimpleStringProperty(
                    convertAndFormat(val, s.getCurrencySymbol()));
        });
        marketValueCol.setCellFactory(tc -> new TableCell<>() {
            @Override
            protected void updateItem(String item, boolean empty) {
                super.updateItem(item, empty);
                if (empty || item == null) {
                    setText(null);
                    return;
                }
                setText(item);
                setStyle("-fx-text-fill:" + ACCENT + ";-fx-font-family:'Plus Jakarta Sans';" +
                        "-fx-alignment:CENTER-RIGHT;");
            }
        });

        // Delete column with confirmation dialog
        TableColumn<Stock, Void> deleteCol = new TableColumn<>("");
        deleteCol.setMinWidth(46);
        deleteCol.setMaxWidth(46);
        deleteCol.setCellFactory(tc -> new TableCell<>() {
            private final Button btn = new Button("✕");
            {
                btn.setStyle("-fx-background-color:transparent;-fx-text-fill:" + DANGER +
                        ";-fx-font-size:14;-fx-cursor:hand;");
                btn.setOnAction(e -> {
                    Stock s = getTableView().getItems().get(getIndex());
                    // Confirm before deleting
                    if (confirmDelete(s.getTicker(), s.getPortfolioName())) {
                        dao.removeStock(s.getTicker(), s.getPortfolioName());
                        refreshDropdown();
                        refreshView(currentFilter());
                    }
                });
            }

            @Override
            protected void updateItem(Void item, boolean empty) {
                super.updateItem(item, empty);
                setGraphic(empty ? null : btn);
            }
        });

        table.getColumns().addAll(
                logoCol, tickerCol, nameCol, marketCol, portfolioCol, priceCol, qtyCol, avgCol, valueCol, pnlCol,
                marketValueCol, deleteCol);
        return table;
    }

    // ── Add Stock Dialog ──────────────────────────────────────────────────────

    private void openAddStockDialog() {
        Stage dialog = new Stage();
        dialog.initModality(Modality.WINDOW_MODAL);
        dialog.initOwner(primaryStage);
        dialog.setTitle("Add Stock");
        dialog.setResizable(false);

        VBox form = new VBox(14);
        form.setPadding(new Insets(28));
        form.setStyle("-fx-background-color:" + BG_PANEL + ";");
        form.setPrefWidth(400);

        // Ticker field — restricted to English letters and digits only
        TextField tickerField = makeField("Ticker Symbol  (e.g. AAPL, 2222)");

        // Dynamic dark-themed suggestion ContextMenu
        ContextMenu autocompleteMenu = new ContextMenu();
        autocompleteMenu.setStyle(
                "-fx-background-color: " + BG_PANEL + ";" +
                        "-fx-border-color: " + BORDER + ";" +
                        "-fx-border-radius: 6;" +
                        "-fx-background-radius: 6;");

        final boolean[] isSelectingSuggestion = { false };

        tickerField.textProperty().addListener((obs, oldVal, newVal) -> {
            // 1. Enforce alphanumeric character and space input filtering (to allow searching by name)
            String filtered = newVal.replaceAll("[^A-Za-z0-9 ]", "");
            if (!filtered.equals(newVal)) {
                tickerField.setText(filtered);
                return;
            }

            if (isSelectingSuggestion[0])
                return;

            String query = filtered.trim();
            if (query.isEmpty()) {
                autocompleteMenu.hide();
                return;
            }

            // Fetch suggestions (up to 8 matched items)
            List<StockSuggestion> matches = StockSuggestionService.search(query, 8);
            if (matches.isEmpty()) {
                autocompleteMenu.hide();
                return;
            }

            autocompleteMenu.getItems().clear();
            for (StockSuggestion s : matches) {
                Label label = new Label(s.getTicker() + " - " + s.getName());
                label.setPrefWidth(280);
                label.setStyle(
                        "-fx-text-fill:" + TEXT_PRI + ";" +
                                "-fx-font-family:'Plus Jakarta Sans';" +
                                "-fx-font-size:13;" +
                                "-fx-padding: 6 12 6 12;" +
                                "-fx-background-color: transparent;");

                label.setOnMouseEntered(me -> {
                    label.setStyle(
                            "-fx-text-fill:" + ACCENT + ";" +
                                    "-fx-font-family:'Plus Jakarta Sans';" +
                                    "-fx-font-size:13;" +
                                    "-fx-padding: 6 12 6 12;" +
                                    "-fx-background-color:" + BG_ROW + ";");
                });

                label.setOnMouseExited(me -> {
                    label.setStyle(
                            "-fx-text-fill:" + TEXT_PRI + ";" +
                                    "-fx-font-family:'Plus Jakarta Sans';" +
                                    "-fx-font-size:13;" +
                                    "-fx-padding: 6 12 6 12;" +
                                    "-fx-background-color: transparent;");
                });

                CustomMenuItem item = new CustomMenuItem(label, true);
                item.setOnAction(e -> {
                    isSelectingSuggestion[0] = true;
                    tickerField.setText(s.getTicker());
                    isSelectingSuggestion[0] = false;
                    autocompleteMenu.hide();
                });
                autocompleteMenu.getItems().add(item);
            }

            if (!autocompleteMenu.isShowing() && tickerField.isFocused()) {
                autocompleteMenu.show(tickerField, Side.BOTTOM, 0, 0);
            }
        });

        // Ensure suggestions menu hides gracefully when focus is lost
        tickerField.focusedProperty().addListener((obs, oldVal, newVal) -> {
            if (!newVal) {
                Platform.runLater(() -> {
                    if (!autocompleteMenu.isFocused()) {
                        autocompleteMenu.hide();
                    }
                });
            }
        });

        // Market auto-detect preview label updates as user types
        Label marketPreview = makeLabel("", "Plus Jakarta Sans", 12, TEXT_SEC, false);
        tickerField.textProperty().addListener((obs, oldVal, newVal) -> {
            if (newVal.isBlank()) {
                marketPreview.setText("");
            } else if (newVal.trim().matches("\\d+")) {
                marketPreview.setText("⟶  " + MARKET_SA + "  (SAR ﷼)");
                marketPreview.setTextFill(Color.web(ACCENT));
            } else {
                marketPreview.setText("⟶  " + MARKET_US + "  (USD $)");
                marketPreview.setTextFill(Color.web(ACCENT));
            }
        });

        // Portfolio ComboBox with editable field (existing + new)
        ComboBox<String> portfolioBox = new ComboBox<>();
        portfolioBox.setEditable(true);
        portfolioBox.setPromptText("Select or type a portfolio name");
        portfolioBox.setMaxWidth(Double.MAX_VALUE);
        portfolioBox.setItems(FXCollections.observableArrayList(dao.getAllPortfolioNames()));
        portfolioBox.setStyle("-fx-background-color:" + BG_ROW + ";-fx-border-color:" + BORDER +
                ";-fx-border-radius:6;-fx-background-radius:6;" +
                "-fx-text-fill:" + TEXT_PRI + ";-fx-font-family:'Plus Jakarta Sans';");

        TextField qtyField = makeField("Quantity  (e.g. 10, 1.5, 0.25)");
        TextField priceField = makeField("Average Buy Price ($)");

        Label errorLabel = makeLabel("", "Plus Jakarta Sans", 12, DANGER, false);
        errorLabel.setWrapText(true);

        Button saveBtn = makeButton("Save Stock", ACCENT, BG_DEEP);
        saveBtn.setMaxWidth(Double.MAX_VALUE);
        saveBtn.setOnAction(e -> {
            String ticker = tickerField.getText().trim().toUpperCase();
            String portfolio = portfolioBox.getEditor().getText().trim();

            if (ticker.isEmpty()) {
                errorLabel.setText("Ticker symbol is required.");
                return;
            }
            if (portfolio.isEmpty()) {
                errorLabel.setText("Portfolio name is required.");
                return;
            }

            double qty;
            double price;
            try {
                qty = Double.parseDouble(qtyField.getText().trim());
            } catch (NumberFormatException ex) {
                errorLabel.setText("Quantity must be a number (e.g. 10, 1.5, 0.25).");
                return;
            }
            String qtyText = qtyField.getText().trim();
            if (qtyText.contains(".") && qtyText.length() - qtyText.indexOf(".") > 3) {
                errorLabel.setText("Quantity can have at most 2 decimal places.");
                return;
            }
            try {
                price = Double.parseDouble(priceField.getText().trim());
            } catch (NumberFormatException ex) {
                errorLabel.setText("Price must be a valid number.");
                return;
            }
            if (qty <= 0) {
                errorLabel.setText("Quantity must be greater than 0.");
                return;
            }
            if (price <= 0) {
                errorLabel.setText("Price must be greater than 0.");
                return;
            }

            // Auto-detect market from ticker
            String market = ticker.matches("\\d+") ? MARKET_SA : MARKET_US;

            dao.addStock(new Stock(ticker, market, qty, price, portfolio));
            refreshDropdown();
            // Switch the main filter to show the portfolio we just added to
            String badged = findBadged(portfolio);
            portfolioFilter.setValue(badged != null ? badged : ALL_PORTFOLIOS);
            refreshView(portfolio);
            dialog.close();
        });

        Button cancelBtn = makeButton("Cancel", BG_ROW, TEXT_SEC);
        cancelBtn.setMaxWidth(Double.MAX_VALUE);
        cancelBtn.setOnAction(e -> dialog.close());

        form.getChildren().addAll(
                makeLabel("Add New Stock", "Georgia", 18, TEXT_PRI, true),
                makeFieldGroup("Ticker Symbol", tickerField),
                marketPreview,
                makeFieldGroup("Portfolio", portfolioBox),
                makeFieldGroup("Quantity", qtyField),
                makeFieldGroup("Average Buy Price ($)", priceField),
                errorLabel, saveBtn, cancelBtn);

        dialog.setScene(new Scene(form));
        dialog.showAndWait();
    }

    // ── Confirmation dialog ───────────────────────────────────────────────────

    private boolean confirmDelete(String ticker, String portfolio) {
        Alert alert = new Alert(Alert.AlertType.CONFIRMATION);
        alert.initOwner(primaryStage);
        alert.initModality(Modality.WINDOW_MODAL);
        alert.setTitle("Confirm Delete");
        alert.setHeaderText("Remove " + ticker + " from \"" + portfolio + "\"?");
        alert.setContentText("This action cannot be undone.");

        alert.getDialogPane().setStyle(
                "-fx-background-color:" + BG_PANEL + ";" +
                        "-fx-border-color:" + BORDER + ";");
        alert.getDialogPane().lookup(".content.label").setStyle(
                "-fx-text-fill:" + TEXT_SEC + ";-fx-font-family:'Plus Jakarta Sans';");

        return alert.showAndWait()
                .filter(r -> r == ButtonType.OK)
                .isPresent();
    }

    // ── Data helpers ──────────────────────────────────────────────────────────

    private void refreshView(String portfolioName) {
        List<Stock> stocks;

        if (ALL_PORTFOLIOS.equals(portfolioName)) {
            stocks = dao.getPortfolio();
        } else if (FILTER_US.equals(portfolioName)) {
            // Fixed market filter — show all US stocks regardless of user portfolio
            stocks = dao.getPortfolio().stream()
                    .filter(s -> MARKET_US.equals(s.getMarket()))
                    .toList();
        } else if (FILTER_SA.equals(portfolioName)) {
            // Fixed market filter — show all Saudi stocks regardless of user portfolio
            stocks = dao.getPortfolio().stream()
                    .filter(s -> MARKET_SA.equals(s.getMarket()))
                    .toList();
        } else {
            stocks = dao.getStocksByPortfolio(portfolioName);
        }

        // Composite pattern: build the tree, getValue() works uniformly
        Portfolio rootPortfolio = new Portfolio(portfolioName);
        for (Stock stock : stocks) {
            rootPortfolio.add(stock);

            // Asynchronously fetch live prices
            CompletableFuture.runAsync(() -> {
                try {
                    PriceFetcher fetcher = FetcherFactory.getFetcher(stock.getMarket());
                    double livePrice = fetcher.fetchPrice(stock.getTicker());
                    Platform.runLater(() -> {
                        stock.setCurrentPrice(livePrice);
                        if (table != null)
                            table.refresh();
                        updateCharts();
                        totalMarketValueLabel.setText(calculateTotalMarketValue(stocks));
                        updatePercentageLabel(stocks);
                    });
                } catch (Exception e) {
                    System.err.println("Failed to fetch real price for " + stock.getTicker() + ": " + e.getMessage());
                }
            });
        }

        tableData.setAll(stocks);
        totalValueLabel.setText(calculateTotalValue(stocks));
        totalMarketValueLabel.setText(calculateTotalMarketValue(stocks));
        stockCountLabel.setText(stocks.size() + " asset" + (stocks.size() != 1 ? "s" : ""));
        updateCharts();
        updatePercentageLabel(stocks);
    }

    private String convertAndFormat(double amount, String originalCurrency) {
        String display = currencySelector.getValue();
        if (display == null || display.equals(CURRENCY_ORIGINAL)) {
            return originalCurrency + " " + String.format("%,.2f", amount);
        }
        double converted;
        String symbol;
        if (display.equals(CURRENCY_USD)) {
            converted = "SAR".equals(originalCurrency) ? amount * SAR_TO_USD : amount;
            symbol = "USD";
        } else {
            converted = "USD".equals(originalCurrency) ? amount * USD_TO_SAR : amount;
            symbol = "SAR";
        }
        return symbol + " " + String.format("%,.2f", converted);
    }

    private String calculateTotalValue(List<Stock> stocks) {
        String display = currencySelector.getValue();
        if (display == null || display.equals(CURRENCY_ORIGINAL)) {
            boolean hasUS = false, hasSA = false;
            double usdTotal = 0, sarTotal = 0;
            for (Stock s : stocks) {
                if (MARKET_US.equals(s.getMarket())) {
                    hasUS = true;
                    usdTotal += s.getValue();
                } else {
                    hasSA = true;
                    sarTotal += s.getValue();
                }
            }
            if (hasUS && hasSA)
                return String.format("USD %,.2f  +  SAR %,.2f", usdTotal, sarTotal);
            if (hasSA)
                return "SAR " + String.format("%,.2f", sarTotal);
            return "USD " + String.format("%,.2f", usdTotal);
        }
        double total = 0;
        String symbol = display.equals(CURRENCY_USD) ? "USD" : "SAR";
        for (Stock s : stocks) {
            double v = s.getValue();
            if (display.equals(CURRENCY_USD) && "SAR".equals(s.getCurrencySymbol()))
                v *= SAR_TO_USD;
            else if (display.equals(CURRENCY_SAR) && "USD".equals(s.getCurrencySymbol()))
                v *= USD_TO_SAR;
            total += v;
        }
        return symbol + " " + String.format("%,.2f", total);
    }

    private String calculateTotalMarketValue(List<Stock> stocks) {
        String display = currencySelector.getValue();
        if (display == null || display.equals(CURRENCY_ORIGINAL)) {
            boolean hasUS = false, hasSA = false;
            double usdTotal = 0, sarTotal = 0;
            for (Stock s : stocks) {
                double price = s.getCurrentPrice() > 0 ? s.getCurrentPrice() : s.getAverageBuyPrice();
                double val = s.getQuantity() * price;
                if (MARKET_US.equals(s.getMarket())) {
                    hasUS = true;
                    usdTotal += val;
                } else {
                    hasSA = true;
                    sarTotal += val;
                }
            }
            if (hasUS && hasSA)
                return String.format("USD %,.2f  +  SAR %,.2f", usdTotal, sarTotal);
            if (hasSA)
                return "SAR " + String.format("%,.2f", sarTotal);
            return "USD " + String.format("%,.2f", usdTotal);
        }
        double total = 0;
        String symbol = display.equals(CURRENCY_USD) ? "USD" : "SAR";
        for (Stock s : stocks) {
            double price = s.getCurrentPrice() > 0 ? s.getCurrentPrice() : s.getAverageBuyPrice();
            double v = s.getQuantity() * price;
            if (display.equals(CURRENCY_USD) && "SAR".equals(s.getCurrencySymbol()))
                v *= SAR_TO_USD;
            else if (display.equals(CURRENCY_SAR) && "USD".equals(s.getCurrencySymbol()))
                v *= USD_TO_SAR;
            total += v;
        }
        return symbol + " " + String.format("%,.2f", total);
    }

    private void updatePercentageLabel(List<Stock> stocks) {
        if (stocks.isEmpty()) {
            marketValuePercentageLabel.setText("live prices");
            marketValuePercentageLabel.setTextFill(Color.web(TEXT_SEC));
            return;
        }
        
        double totalCostUSD = 0;
        double totalMarketUSD = 0;
        
        for (Stock s : stocks) {
            double cost = s.getQuantity() * s.getAverageBuyPrice();
            double market = s.getQuantity() * (s.getCurrentPrice() > 0 ? s.getCurrentPrice() : s.getAverageBuyPrice());
            
            if ("SAR".equals(s.getCurrencySymbol())) {
                cost *= SAR_TO_USD;
                market *= SAR_TO_USD;
            }
            
            totalCostUSD += cost;
            totalMarketUSD += market;
        }
        
        if (totalCostUSD == 0) {
            marketValuePercentageLabel.setText("live prices  •  0.00%");
            marketValuePercentageLabel.setTextFill(Color.web(TEXT_SEC));
            return;
        }
        
        double percentage = ((totalMarketUSD - totalCostUSD) / totalCostUSD) * 100;
        String sign = percentage > 0 ? "+" : "";
        marketValuePercentageLabel.setText(String.format("live prices  •  %s%.2f%%", sign, percentage));
        
        if (percentage > 0) {
            marketValuePercentageLabel.setTextFill(Color.web(COLOR_PROFIT));
        } else if (percentage < 0) {
            marketValuePercentageLabel.setTextFill(Color.web(DANGER));
        } else {
            marketValuePercentageLabel.setTextFill(Color.web(TEXT_SEC));
        }
    }

    private void refreshDropdown() {
        isRefreshingDropdown = true;
        try {
            String current = currentFilter();

            // Count stocks per user portfolio for the badges
            List<Stock> all = dao.getPortfolio();
            Map<String, Long> counts = all.stream()
                    .collect(Collectors.groupingBy(Stock::getPortfolioName, Collectors.counting()));

            long usCount = all.stream().filter(s -> MARKET_US.equals(s.getMarket())).count();
            long saCount = all.stream().filter(s -> MARKET_SA.equals(s.getMarket())).count();

            ObservableList<String> items = FXCollections.observableArrayList();

            // ── Fixed items at the top ─────────────────────────────────────────
            items.add(ALL_PORTFOLIOS);
            items.add(FILTER_US + " (" + usCount + ")");
            items.add(FILTER_SA + " (" + saCount + ")");
            // ── Separator label (non-selectable visual divider) ────────────────
            items.add("── Your Portfolios ──");
            // ── User portfolios with badges ────────────────────────────────────
            for (String name : dao.getAllPortfolioNames()) {
                items.add(name + " (" + counts.getOrDefault(name, 0L) + ")");
            }

            portfolioFilter.setItems(items);

            // Make the separator non-selectable and style all cells explicitly
            portfolioFilter.setCellFactory(lv -> new ListCell<>() {
                @Override
                protected void updateItem(String item, boolean empty) {
                    super.updateItem(item, empty);
                    if (empty || item == null) {
                        setText(null);
                        setStyle("-fx-background-color:" + BG_PANEL + ";");
                        setDisable(false);
                        return;
                    }
                    if (item.startsWith("──")) {

                        setText(item);
                        setDisable(true);
                        setStyle(
                                "-fx-background-color:" + BG_DEEP + ";" +
                                        "-fx-text-fill:" + TEXT_SEC + ";" +
                                        "-fx-font-size:11;" +
                                        "-fx-font-family:'Plus Jakarta Sans';" +
                                        "-fx-opacity:1;");
                    } else {

                        setText(item);
                        setDisable(false);
                        setStyle(
                                "-fx-background-color:" + BG_PANEL + ";" +
                                        "-fx-text-fill:" + TEXT_PRI + ";" +
                                        "-fx-font-family:'Plus Jakarta Sans';" +
                                        "-fx-font-size:13;");

                        setOnMouseEntered(e -> {
                            if (!isDisabled())
                                setStyle(
                                        "-fx-background-color:" + BG_ROW + ";" +
                                                "-fx-text-fill:" + ACCENT + ";" +
                                                "-fx-font-family:'Plus Jakarta Sans';" +
                                                "-fx-font-size:13;");
                        });
                        setOnMouseExited(e -> setStyle(
                                "-fx-background-color:" + BG_PANEL + ";" +
                                        "-fx-text-fill:" + TEXT_PRI + ";" +
                                        "-fx-font-family:'Plus Jakarta Sans';" +
                                        "-fx-font-size:13;"));
                    }
                }
            });

            portfolioFilter.setButtonCell(new ListCell<>() {
                @Override
                protected void updateItem(String item, boolean empty) {
                    super.updateItem(item, empty);
                    setText(empty || item == null ? "" : item);
                    setStyle(
                            "-fx-text-fill:" + TEXT_PRI + ";" +
                                    "-fx-font-family:'Plus Jakarta Sans';" +
                                    "-fx-font-size:13;" +
                                    "-fx-background-color:transparent;");
                }
            });
            portfolioCountLabel.setText(String.valueOf(counts.size()));

            String restored = items.stream()
                    .filter(i -> !i.startsWith("──"))
                    .filter(i -> stripBadge(i).equals(current))
                    .findFirst()
                    .orElse(ALL_PORTFOLIOS);
            portfolioFilter.setValue(restored);
        } finally {
            isRefreshingDropdown = false;
        }
    }

    /** "Tech (3)" → "Tech" | "🇺🇸 US Market (6)" → "🇺🇸 US Market" */
    private String stripBadge(String item) {
        if (item == null)
            return ALL_PORTFOLIOS;
        return item.replaceAll("\\s*\\(\\d+\\)$", "").trim();
    }

    /** Find the badged version of a plain portfolio name in the dropdown items. */
    private String findBadged(String plainName) {
        return portfolioFilter.getItems().stream()
                .filter(i -> stripBadge(i).equals(plainName))
                .findFirst()
                .orElse(null);
    }

    /** Returns the current filter as a plain name (no badge). */
    private String currentFilter() {
        return stripBadge(portfolioFilter.getValue());
    }

    // ── UI factory helpers ────────────────────────────────────────────────────

    private Label makeLabel(String text, String font, double size, String color, boolean bold) {
        Label l = new Label(text);
        String finalFont = "Courier New".equals(font) ? "Plus Jakarta Sans" : font;
        l.setFont(bold ? Font.font(finalFont, FontWeight.BOLD, size) : Font.font(finalFont, size));
        l.setTextFill(Color.web(color));
        return l;
    }

    private Button makeButton(String text, String bg, String fg) {
        Button b = new Button(text);
        String base = "-fx-background-color:" + bg + ";-fx-text-fill:" + fg +
                ";-fx-background-radius:6;-fx-cursor:hand;-fx-padding:8 20 8 20;" +
                "-fx-font-family:'Plus Jakarta Sans';-fx-font-weight:bold;-fx-font-size:13;";
        String hover = "-fx-background-color:" + ACCENT_DIM + ";-fx-text-fill:" + BG_DEEP +
                ";-fx-background-radius:6;-fx-cursor:hand;-fx-padding:8 20 8 20;" +
                "-fx-font-family:'Plus Jakarta Sans';-fx-font-weight:bold;-fx-font-size:13;";
        b.setStyle(base);
        b.setOnMouseEntered(e -> b.setStyle(hover));
        b.setOnMouseExited(e -> b.setStyle(base));
        return b;
    }

    private TextField makeField(String prompt) {
        TextField tf = new TextField();
        tf.setPromptText(prompt);
        tf.setStyle("-fx-background-color:" + BG_ROW + ";-fx-border-color:" + BORDER +
                ";-fx-border-radius:6;-fx-background-radius:6;-fx-text-fill:" + TEXT_PRI +
                ";-fx-prompt-text-fill:" + TEXT_SEC +
                ";-fx-font-family:'Plus Jakarta Sans';-fx-padding:8 12 8 12;");
        return tf;
    }

    private VBox makeFieldGroup(String labelText, javafx.scene.Node field) {
        return new VBox(4, makeLabel(labelText, "Plus Jakarta Sans", 11, TEXT_SEC, false), field);
    }

    private <T> TableColumn<Stock, T> makeCol(String header, String property, double minWidth) {
        TableColumn<Stock, T> col = new TableColumn<>(header);
        col.setCellValueFactory(new PropertyValueFactory<>(property));
        col.setMinWidth(minWidth);
        col.setStyle("-fx-font-family:'Plus Jakarta Sans';");
        return col;
    }
}
