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

/**
 * Main JavaFX view for stoX.
 * Owns all UI construction and user interaction logic.
 * Communicates with the data layer through the StockDAO interface.
 */
public class MainView {

    // ── Palette ───────────────────────────────────────────────────────────────

    private static final String BG_DEEP    = "#0d1117";
    private static final String BG_PANEL   = "#161b22";
    private static final String BG_ROW     = "#1c2128";
    private static final String ACCENT     = "#00d4aa";
    private static final String ACCENT_DIM = "#00a882";
    private static final String TEXT_PRI   = "#e6edf3";
    private static final String TEXT_SEC   = "#8b949e";
    private static final String BORDER     = "#30363d";
    private static final String DANGER     = "#f85149";

    private static final String ALL_PORTFOLIOS = "All Portfolios";
    private static final String MARKET_US      = "US Market - Finnhub";
    private static final String MARKET_SA      = "Saudi Market - Tadawul";


    private static final String FILTER_US = "🇺🇸  US Market";
    private static final String FILTER_SA = "🇸🇦  Saudi Market";

    // ── State ─────────────────────────────────────────────────────────────────

    private final StockDAO dao = new SqliteStockDAO();
    private final ObservableList<Stock> tableData = FXCollections.observableArrayList();

    private Stage primaryStage;
    private ComboBox<String> portfolioFilter;
    private Label totalValueLabel;
    private Label stockCountLabel;
    private Label portfolioCountLabel;
    private TableView<Stock> table;
    private boolean isRefreshingDropdown = false;

    // ─────────────────────────────────────────────────────────────────────────

    public void show(Stage stage) {
        this.primaryStage = stage;

        stage.setTitle("stoX — Portfolio Manager");
        stage.setMinWidth(900);
        stage.setMinHeight(620);

        BorderPane root = new BorderPane();
        root.setStyle("-fx-background-color:" + BG_DEEP + ";");
        root.setTop(buildHeader());
        root.setCenter(buildCenter());

        stage.setScene(new Scene(root, 1000, 680));
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

        Label logo  = makeLabel("sto", "Georgia", 26, TEXT_PRI, true);
        Label logoX = makeLabel("X",   "Georgia", 26, ACCENT,   true);
        Label tag   = makeLabel("  Portfolio Manager", "Courier New", 13, TEXT_SEC, false);

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
        center.getChildren().addAll(buildStatsRow(), buildFilterBar(), buildTable());
        return center;
    }

    // ── Stats row — Total Value card + Portfolio count card ───────────────────

    private HBox buildStatsRow() {
        // Card 1: Total value
        totalValueLabel    = makeLabel("$ 0.00",      "Courier New", 30, ACCENT,    true);
        stockCountLabel    = makeLabel("0 assets",    "Courier New", 13, TEXT_SEC,  false);
        VBox valueCard = new VBox(3,
                makeLabel("TOTAL VALUE", "Courier New", 11, TEXT_SEC, false),
                totalValueLabel,
                stockCountLabel);
        styleCard(valueCard);

        // Card 2 — portfolio count
        portfolioCountLabel = makeLabel("0", "Courier New", 30, ACCENT, true);
        VBox portfolioCard = new VBox(3,
                makeLabel("PORTFOLIOS", "Courier New", 11, TEXT_SEC, false),
                portfolioCountLabel,
                makeLabel("distinct portfolios", "Courier New", 13, TEXT_SEC, false));
        styleCard(portfolioCard);

        HBox row = new HBox(16, valueCard, portfolioCard);
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
                ";-fx-font-family:'Courier New';-fx-font-size:13;");

        portfolioFilter.setOnAction(e -> {
            if (isRefreshingDropdown) return;
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

        HBox bar = new HBox(12,
                makeLabel("Portfolio:", "Courier New", 13, TEXT_SEC, false),
                portfolioFilter,
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
                "Courier New", 13, TEXT_SEC, false));

        TableColumn<Stock, String>  tickerCol    = makeCol("TICKER",       "ticker",          100);
        TableColumn<Stock, String>  marketCol    = makeCol("MARKET",       "market",          140);
        TableColumn<Stock, String>  portfolioCol = makeCol("PORTFOLIO",    "portfolioName",   160);
        TableColumn<Stock, Double>  priceCol     = makeCol("CURRENT PRICE ($)", "currentPrice", 140);
        TableColumn<Stock, Integer> qtyCol       = makeCol("QTY",          "quantity",         70);
        TableColumn<Stock, Double>  avgCol       = makeCol("AVG BUY ($)",  "averageBuyPrice", 120);

        // Computed total value column
        TableColumn<Stock, String> valueCol = new TableColumn<>("TOTAL VALUE ($)");
        valueCol.setCellValueFactory(cd ->
            new javafx.beans.property.SimpleStringProperty(
                String.format("%,.2f", cd.getValue().getValue())));
        valueCol.setCellFactory(tc -> new TableCell<>() {
            @Override protected void updateItem(String item, boolean empty) {
                super.updateItem(item, empty);
                if (empty || item == null) { setText(null); return; }
                setText(item);
                setStyle("-fx-text-fill:" + ACCENT + ";-fx-font-family:'Courier New';" +
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
            @Override protected void updateItem(Void item, boolean empty) {
                super.updateItem(item, empty);
                setGraphic(empty ? null : btn);
            }
        });

        table.getColumns().addAll(
                tickerCol, marketCol, portfolioCol, priceCol, qtyCol, avgCol, valueCol, deleteCol);
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
        tickerField.textProperty().addListener((obs, oldVal, newVal) -> {
            // Strip anything that is not A-Z, a-z, or 0-9
            String filtered = newVal.replaceAll("[^A-Za-z0-9]", "");
            if (!filtered.equals(newVal)) tickerField.setText(filtered);
        });

        // Market auto-detect preview label updates as user types
        Label marketPreview = makeLabel("", "Courier New", 12, TEXT_SEC, false);
        tickerField.textProperty().addListener((obs, oldVal, newVal) -> {
            if (newVal.isBlank()) {
                marketPreview.setText("");
            } else if (newVal.trim().matches("\\d+")) {
                marketPreview.setText("⟶  " + MARKET_SA);
                marketPreview.setTextFill(Color.web(ACCENT));
            } else {
                marketPreview.setText("⟶  " + MARKET_US);
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
                              "-fx-text-fill:" + TEXT_PRI + ";-fx-font-family:'Courier New';");

        TextField qtyField   = makeField("Quantity");
        TextField priceField = makeField("Average Buy Price ($)");

        Label errorLabel = makeLabel("", "Courier New", 12, DANGER, false);
        errorLabel.setWrapText(true);

        Button saveBtn = makeButton("Save Stock", ACCENT, BG_DEEP);
        saveBtn.setMaxWidth(Double.MAX_VALUE);
        saveBtn.setOnAction(e -> {
            String ticker    = tickerField.getText().trim().toUpperCase();
            String portfolio = portfolioBox.getEditor().getText().trim();

            if (ticker.isEmpty())    { errorLabel.setText("Ticker symbol is required.");  return; }
            if (portfolio.isEmpty()) { errorLabel.setText("Portfolio name is required."); return; }

            int qty; double price;
            try { qty   = Integer.parseInt(qtyField.getText().trim()); }
            catch (NumberFormatException ex) { errorLabel.setText("Quantity must be a whole number."); return; }
            try { price = Double.parseDouble(priceField.getText().trim()); }
            catch (NumberFormatException ex) { errorLabel.setText("Price must be a valid number.");    return; }
            if (qty   <= 0) { errorLabel.setText("Quantity must be greater than 0."); return; }
            if (price <= 0) { errorLabel.setText("Price must be greater than 0.");    return; }

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
                "-fx-text-fill:" + TEXT_SEC + ";-fx-font-family:'Courier New';");

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
                        if (table != null) table.refresh();
                    });
                } catch (Exception e) {
                    System.err.println("Failed to fetch real price for " + stock.getTicker() + ": " + e.getMessage());
                }
            });
        }

        tableData.setAll(stocks);
        totalValueLabel.setText(String.format("$ %,.2f", rootPortfolio.getValue()));
        stockCountLabel.setText(stocks.size() + " asset" + (stocks.size() != 1 ? "s" : ""));
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
            @Override protected void updateItem(String item, boolean empty) {
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
                        "-fx-font-family:'Courier New';" +
                        "-fx-opacity:1;"
                    );
                } else {

                    setText(item);
                    setDisable(false);
                    setStyle(
                        "-fx-background-color:" + BG_PANEL + ";" +
                        "-fx-text-fill:" + TEXT_PRI + ";" +
                        "-fx-font-family:'Courier New';" +
                        "-fx-font-size:13;"
                    );

                    setOnMouseEntered(e -> {
                        if (!isDisabled()) setStyle(
                            "-fx-background-color:" + BG_ROW + ";" +
                            "-fx-text-fill:" + ACCENT + ";" +
                            "-fx-font-family:'Courier New';" +
                            "-fx-font-size:13;"
                        );
                    });
                    setOnMouseExited(e -> setStyle(
                        "-fx-background-color:" + BG_PANEL + ";" +
                        "-fx-text-fill:" + TEXT_PRI + ";" +
                        "-fx-font-family:'Courier New';" +
                        "-fx-font-size:13;"
                    ));
                }
            }
        });


        portfolioFilter.setButtonCell(new ListCell<>() {
            @Override protected void updateItem(String item, boolean empty) {
                super.updateItem(item, empty);
                setText(empty || item == null ? "" : item);
                setStyle(
                    "-fx-text-fill:" + TEXT_PRI + ";" +
                    "-fx-font-family:'Courier New';" +
                    "-fx-font-size:13;" +
                    "-fx-background-color:transparent;"
                );
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

    /** "Tech (3)" → "Tech"  |  "🇺🇸  US Market (6)" → "🇺🇸  US Market" */
    private String stripBadge(String item) {
        if (item == null) return ALL_PORTFOLIOS;
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
        l.setFont(bold ? Font.font(font, FontWeight.BOLD, size) : Font.font(font, size));
        l.setTextFill(Color.web(color));
        return l;
    }

    private Button makeButton(String text, String bg, String fg) {
        Button b = new Button(text);
        String base  = "-fx-background-color:" + bg + ";-fx-text-fill:" + fg +
                       ";-fx-background-radius:6;-fx-cursor:hand;-fx-padding:8 20 8 20;" +
                       "-fx-font-family:'Courier New';-fx-font-weight:bold;-fx-font-size:13;";
        String hover = "-fx-background-color:" + ACCENT_DIM + ";-fx-text-fill:" + BG_DEEP +
                       ";-fx-background-radius:6;-fx-cursor:hand;-fx-padding:8 20 8 20;" +
                       "-fx-font-family:'Courier New';-fx-font-weight:bold;-fx-font-size:13;";
        b.setStyle(base);
        b.setOnMouseEntered(e -> b.setStyle(hover));
        b.setOnMouseExited(e  -> b.setStyle(base));
        return b;
    }

    private TextField makeField(String prompt) {
        TextField tf = new TextField();
        tf.setPromptText(prompt);
        tf.setStyle("-fx-background-color:" + BG_ROW + ";-fx-border-color:" + BORDER +
                    ";-fx-border-radius:6;-fx-background-radius:6;-fx-text-fill:" + TEXT_PRI +
                    ";-fx-prompt-text-fill:" + TEXT_SEC +
                    ";-fx-font-family:'Courier New';-fx-padding:8 12 8 12;");
        return tf;
    }

    private VBox makeFieldGroup(String labelText, javafx.scene.Node field) {
        return new VBox(4, makeLabel(labelText, "Courier New", 11, TEXT_SEC, false), field);
    }

    private <T> TableColumn<Stock, T> makeCol(String header, String property, double minWidth) {
        TableColumn<Stock, T> col = new TableColumn<>(header);
        col.setCellValueFactory(new PropertyValueFactory<>(property));
        col.setMinWidth(minWidth);
        col.setStyle("-fx-font-family:'Courier New';");
        return col;
    }
}
