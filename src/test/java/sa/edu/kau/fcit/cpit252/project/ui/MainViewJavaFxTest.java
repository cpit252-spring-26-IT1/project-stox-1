package sa.edu.kau.fcit.cpit252.project.ui;

import javafx.collections.FXCollections;
import javafx.scene.control.Button;
import javafx.scene.control.ComboBox;
import javafx.scene.control.Label;
import javafx.scene.control.ListCell;
import javafx.scene.control.TableCell;
import javafx.scene.control.TableColumn;
import javafx.scene.control.TableView;
import javafx.scene.control.TextField;
import javafx.scene.layout.VBox;
import javafx.stage.Stage;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;
import org.testfx.framework.junit5.ApplicationTest;
import org.testfx.util.WaitForAsyncUtils;
import sa.edu.kau.fcit.cpit252.project.api.CachedPriceFetcher;
import sa.edu.kau.fcit.cpit252.project.dao.SqliteStockDAO;
import sa.edu.kau.fcit.cpit252.project.db.DatabaseConnection;
import sa.edu.kau.fcit.cpit252.project.model.Stock;

import java.lang.reflect.Constructor;
import java.lang.reflect.Field;
import java.lang.reflect.Method;
import java.nio.file.Files;
import java.nio.file.Path;
import java.sql.Connection;
import java.sql.DriverManager;
import java.util.List;
import java.util.Map;
import java.util.concurrent.atomic.AtomicReference;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

class MainViewJavaFxTest extends ApplicationTest {

    private Connection connection;
    private MainView mainView;
    private Stage stage;

    @Override
    public void init() throws Exception {
        Class.forName("org.sqlite.JDBC");
        Path tempDir = Files.createTempDirectory("stox-main-view-test");
        connection = DriverManager.getConnection("jdbc:sqlite:" + tempDir.resolve("main-view-test.db"));
        setDatabaseConnection(connection);
        SqliteStockDAO dao = new SqliteStockDAO();
        dao.addStock(new Stock("AAPL", "Sandbox Market", 2, 100, "Tech"));
        dao.addStock(new Stock("2222", "Sandbox Market", 3, 10, "Saudi"));
    }

    @Override
    public void start(Stage stage) {
        this.stage = stage;
        mainView = new MainView();
        mainView.show(stage);
    }

    @AfterEach
    void tearDownDatabase() throws Exception {
        if (stage != null) {
            interact(stage::close);
        }
        setDatabaseConnection(null);
        if (connection != null) {
            connection.close();
        }
    }

    @Test
    void showBuildsMainSceneAndLoadsStocksFromDatabase() throws Exception {
        assertTrue(stage.isShowing());
        assertEquals("stoX — Portfolio Manager", stage.getTitle());
        assertNotNull(stage.getScene());

        TableView<Stock> table = getField("table", TableView.class);
        Label totalValueLabel = getField("totalValueLabel", Label.class);
        Label stockCountLabel = getField("stockCountLabel", Label.class);
        Label portfolioCountLabel = getField("portfolioCountLabel", Label.class);

        assertEquals(2, table.getItems().size());
        assertEquals("SAR 230.00", totalValueLabel.getText());
        assertEquals("2 assets", stockCountLabel.getText());
        assertEquals("2", portfolioCountLabel.getText());
    }

    @Test
    void currencySelectorRecalculatesTotals() throws Exception {
        ComboBox<String> currencySelector = getField("currencySelector", ComboBox.class);
        Label totalValueLabel = getField("totalValueLabel", Label.class);

        interact(() -> currencySelector.setValue("USD $"));
        assertEquals("USD 230.00", totalValueLabel.getText());

        interact(() -> currencySelector.setValue("SAR ﷼"));
        assertEquals("SAR 862.50", totalValueLabel.getText());
    }

    @Test
    void portfolioFilterShowsAllFixedAndUserPortfolioChoices() throws Exception {
        ComboBox<String> portfolioFilter = getField("portfolioFilter", ComboBox.class);

        assertTrue(portfolioFilter.getItems().contains("All Portfolios"));
        assertTrue(portfolioFilter.getItems().stream().anyMatch(item -> item.contains("US Market")));
        assertTrue(portfolioFilter.getItems().stream().anyMatch(item -> item.contains("Saudi Market")));
        assertTrue(portfolioFilter.getItems().contains("Tech (1)"));
        assertTrue(portfolioFilter.getItems().contains("Saudi (1)"));
    }

    @Test
    void selectingPortfolioFiltersRefreshesTableWithoutApiCalls() throws Exception {
        ComboBox<String> portfolioFilter = getField("portfolioFilter", ComboBox.class);
        TableView<Stock> table = getField("table", TableView.class);

        interact(() -> portfolioFilter.setValue("Tech (1)"));
        assertEquals(1, table.getItems().size());
        assertEquals("AAPL", table.getItems().get(0).getTicker());

        String saudiPortfolio = portfolioFilter.getItems().stream()
                .filter(item -> item.equals("Saudi (1)"))
                .findFirst()
                .orElseThrow();
        interact(() -> portfolioFilter.setValue(saudiPortfolio));
        assertEquals(1, table.getItems().size());
        assertEquals("2222", table.getItems().get(0).getTicker());
    }

    @Test
    void privateFormattingAndFilterHelpersCoverCurrencyBranches() throws Exception {
        ComboBox<String> currencySelector = getField("currencySelector", ComboBox.class);

        assertEquals("All Portfolios", invokeOnFx("stripBadge", new Class<?>[]{String.class}, new Object[]{null}));
        assertEquals("Tech", invokeOnFx("stripBadge", new Class<?>[]{String.class}, "Tech (12)"));
        assertEquals("Tech (1)", invokeOnFx("findBadged", new Class<?>[]{String.class}, "Tech"));
        assertEquals("Missing", invokeOnFx("stripBadge", new Class<?>[]{String.class}, "Missing"));

        interact(() -> currencySelector.setValue("Original"));
        assertEquals("USD 10.00", invokeOnFx("convertAndFormat", new Class<?>[]{double.class, String.class}, 10.0, "USD"));
        assertEquals("SAR 10.00", invokeOnFx("convertAndFormat", new Class<?>[]{double.class, String.class}, 10.0, "SAR"));

        interact(() -> currencySelector.setValue("USD $"));
        assertEquals("USD 10.00", invokeOnFx("convertAndFormat", new Class<?>[]{double.class, String.class}, 10.0, "USD"));
        assertEquals("USD 2.67", invokeOnFx("convertAndFormat", new Class<?>[]{double.class, String.class}, 10.0, "SAR"));

        interact(() -> currencySelector.setValue("SAR ﷼"));
        assertEquals("SAR 37.50", invokeOnFx("convertAndFormat", new Class<?>[]{double.class, String.class}, 10.0, "USD"));
        assertEquals("SAR 10.00", invokeOnFx("convertAndFormat", new Class<?>[]{double.class, String.class}, 10.0, "SAR"));
    }

    @Test
    void calculateTotalValueCoversSingleCurrencyAndConvertedTotals() throws Exception {
        ComboBox<String> currencySelector = getField("currencySelector", ComboBox.class);
        List<Stock> usStocks = List.of(new Stock("AAPL", "US Market - Finnhub", 2, 100, "Tech"));
        List<Stock> saudiStocks = List.of(new Stock("2222", "Saudi Market - Tadawul", 3, 10, "Saudi"));
        List<Stock> mixedStocks = List.of(
                new Stock("AAPL", "US Market - Finnhub", 2, 100, "Tech"),
                new Stock("2222", "Saudi Market - Tadawul", 3, 10, "Saudi"));

        interact(() -> currencySelector.setValue("Original"));
        assertEquals("USD 200.00", invokeOnFx("calculateTotalValue", new Class<?>[]{List.class}, usStocks));
        assertEquals("SAR 30.00", invokeOnFx("calculateTotalValue", new Class<?>[]{List.class}, saudiStocks));
        assertEquals("USD 200.00  +  SAR 30.00", invokeOnFx("calculateTotalValue", new Class<?>[]{List.class}, mixedStocks));

        interact(() -> currencySelector.setValue("USD $"));
        assertEquals("USD 208.00", invokeOnFx("calculateTotalValue", new Class<?>[]{List.class}, mixedStocks));

        interact(() -> currencySelector.setValue("SAR ﷼"));
        assertEquals("SAR 780.00", invokeOnFx("calculateTotalValue", new Class<?>[]{List.class}, mixedStocks));
    }

    @Test
    void uiFactoryHelpersCreateStyledControls() throws Exception {
        Label boldLabel = invokeOnFx("makeLabel",
                new Class<?>[]{String.class, String.class, double.class, String.class, boolean.class},
                "Hello", "Courier New", 12.0, "#ffffff", true);
        Label plainLabel = invokeOnFx("makeLabel",
                new Class<?>[]{String.class, String.class, double.class, String.class, boolean.class},
                "Hello", "Courier New", 12.0, "#ffffff", false);
        Button button = invokeOnFx("makeButton", new Class<?>[]{String.class, String.class, String.class},
                "Save", "#000000", "#ffffff");
        TextField field = invokeOnFx("makeField", new Class<?>[]{String.class}, "Ticker");
        VBox group = invokeOnFx("makeFieldGroup", new Class<?>[]{String.class, javafx.scene.Node.class}, "Ticker", field);
        TableColumn<Stock, String> column = invokeOnFx("makeCol", new Class<?>[]{String.class, String.class, double.class},
                "TICKER", "ticker", 100.0);

        assertEquals("Hello", boldLabel.getText());
        assertEquals("Hello", plainLabel.getText());
        assertEquals("Save", button.getText());
        assertEquals("Ticker", field.getPromptText());
        assertEquals(2, group.getChildren().size());
        assertEquals("TICKER", column.getText());
        assertEquals(100, column.getMinWidth(), 0.001);
    }

    @Test
    void tableCellFactoriesRenderValueAndPnlStates() throws Exception {
        TableView<Stock> table = getField("table", TableView.class);
        Stock profitStock = new Stock("AAPL", "US Market - Finnhub", 2, 100, "Tech");
        profitStock.setCurrentPrice(125);
        Stock lossStock = new Stock("MSFT", "US Market - Finnhub", 1, 100, "Tech");
        lossStock.setCurrentPrice(75);
        Stock loadingStock = new Stock("GOOG", "US Market - Finnhub", 1, 100, "Tech");

        interact(() -> table.setItems(FXCollections.observableArrayList(profitStock, lossStock, loadingStock)));

        TableColumn<Stock, ?> valueColumn = table.getColumns().get(8);
        TableColumn<Stock, ?> pnlColumn = table.getColumns().get(9);

        assertEquals("USD 200.00", valueColumn.getCellObservableValue(profitStock).getValue());
        assertEquals("+USD 50.00", pnlColumn.getCellObservableValue(profitStock).getValue());
        assertEquals("USD -25.00", pnlColumn.getCellObservableValue(lossStock).getValue());
        assertEquals("—", pnlColumn.getCellObservableValue(loadingStock).getValue());

        @SuppressWarnings({"unchecked", "rawtypes"})
        TableCell<Stock, String> pnlCell = (TableCell<Stock, String>) ((TableColumn) pnlColumn).getCellFactory().call((TableColumn) pnlColumn);
        interact(() -> {
            pnlCell.updateIndex(0);
            updateCellItem(pnlCell, "+USD 50.00", false);
            updateCellItem(pnlCell, "USD -25.00", false);
            updateCellItem(pnlCell, "—", false);
            updateCellItem(pnlCell, null, true);
        });

        @SuppressWarnings({"unchecked", "rawtypes"})
        TableCell<Stock, String> valueCell = (TableCell<Stock, String>) ((TableColumn) valueColumn).getCellFactory().call((TableColumn) valueColumn);
        interact(() -> {
            updateCellItem(valueCell, "USD 200.00", false);
            updateCellItem(valueCell, null, true);
        });
    }

    @Test
    void refreshViewHandlesUnknownMarketWithoutFailing() throws Exception {
        SqliteStockDAO dao = new SqliteStockDAO();
        dao.addStock(new Stock("BAD", "Crypto", 1, 100, "Other"));

        invokeOnFx("refreshDropdown", new Class<?>[]{});
        invokeOnFx("refreshView", new Class<?>[]{String.class}, "Other");

        TableView<Stock> table = getField("table", TableView.class);
        assertEquals(1, table.getItems().size());
        assertEquals("BAD", table.getItems().get(0).getTicker());
    }

    @Test
    void hoverHandlersOnButtonAndDropdownCellsCanRunSafely() throws Exception {
        Button button = invokeOnFx("makeButton", new Class<?>[]{String.class, String.class, String.class},
                "Hover", "#000000", "#ffffff");

        interact(() -> {
            button.getOnMouseEntered().handle(null);
            button.getOnMouseExited().handle(null);
        });

        ComboBox<String> portfolioFilter = getField("portfolioFilter", ComboBox.class);
        assertNotNull(portfolioFilter.getCellFactory());
        assertFalse(portfolioFilter.getItems().isEmpty());

        @SuppressWarnings("unchecked")
        ListCell<String> popupCell = (ListCell<String>) portfolioFilter.getCellFactory().call(null);
        interact(() -> {
            updateListCellItem(popupCell, null, true);
            updateListCellItem(popupCell, "── Your Portfolios ──", false);
            updateListCellItem(popupCell, "Tech (1)", false);
            popupCell.getOnMouseEntered().handle(null);
            popupCell.getOnMouseExited().handle(null);
        });

        ListCell<String> portfolioButtonCell = portfolioFilter.getButtonCell();
        interact(() -> {
            updateListCellItem(portfolioButtonCell, "All Portfolios", false);
            updateListCellItem(portfolioButtonCell, null, true);
        });

        ComboBox<String> currencySelector = getField("currencySelector", ComboBox.class);
        ListCell<String> currencyButtonCell = currencySelector.getButtonCell();
        interact(() -> {
            updateListCellItem(currencyButtonCell, "Original", false);
            updateListCellItem(currencyButtonCell, null, true);
        });
    }

    @Test
    void addStockDialogValidatesInputWithoutSavingOrCallingApis() {
        openAddStockDialog();

        Button saveButton = findButton("Save Stock");
        TextField tickerField = findTextField("Ticker Symbol");
        TextField qtyField = findTextField("Quantity");
        TextField priceField = findTextField("Average Buy Price");
        ComboBox<?> portfolioBox = lookup(node -> node instanceof ComboBox<?> comboBox && comboBox.isEditable())
                .queryAs(ComboBox.class);

        clickOn(saveButton);
        assertTrue(hasLabelText("Ticker symbol is required."));

        clickOn(tickerField).write("AAPL!");
        assertEquals("AAPL", tickerField.getText());
        assertTrue(hasLabelText("US Market"));

        clickOn(saveButton);
        assertTrue(hasLabelText("Portfolio name is required."));

        interact(() -> portfolioBox.getEditor().setText("Tech"));
        clickOn(saveButton);
        assertTrue(hasLabelText("Quantity must be a number"));

        clickOn(qtyField).write("1.234");
        clickOn(priceField).write("10");
        clickOn(saveButton);
        assertTrue(hasLabelText("Quantity can have at most 2 decimal places."));

        interact(() -> {
            qtyField.setText("-1");
            priceField.setText("10");
        });
        clickOn(saveButton);
        assertTrue(hasLabelText("Quantity must be greater than 0."));

        interact(() -> {
            qtyField.setText("1");
            priceField.setText("abc");
        });
        clickOn(saveButton);
        assertTrue(hasLabelText("Price must be a valid number."));

        interact(() -> priceField.setText("-1"));
        clickOn(saveButton);
        assertTrue(hasLabelText("Price must be greater than 0."));

        clickOn(findButton("Cancel"));
    }

    @Test
    void selectingMarketFiltersUsesCachedPricesWithoutRealApiCalls() throws Exception {
        SqliteStockDAO dao = new SqliteStockDAO();
        dao.addStock(new Stock("MSFT", "US Market - Finnhub", 1, 200, "Live US"));
        dao.addStock(new Stock("3333", "Saudi Market - Tadawul", 2, 30, "Live Saudi"));
        putCachedPrice("MSFT", 250);
        putCachedPrice("3333", 35);

        invokeOnFx("refreshDropdown", new Class<?>[]{});
        ComboBox<String> portfolioFilter = getField("portfolioFilter", ComboBox.class);
        TableView<Stock> table = getField("table", TableView.class);

        String usMarket = portfolioFilter.getItems().stream()
                .filter(item -> item.contains("US Market"))
                .findFirst()
                .orElseThrow();
        interact(() -> portfolioFilter.setValue(usMarket));
        WaitForAsyncUtils.sleep(250, java.util.concurrent.TimeUnit.MILLISECONDS);
        WaitForAsyncUtils.waitForFxEvents();
        assertEquals(1, table.getItems().size());
        assertEquals("MSFT", table.getItems().get(0).getTicker());
        assertEquals(250, table.getItems().get(0).getCurrentPrice(), 0.001);

        String saudiMarket = portfolioFilter.getItems().stream()
                .filter(item -> item.contains("Saudi Market"))
                .findFirst()
                .orElseThrow();
        interact(() -> portfolioFilter.setValue(saudiMarket));
        WaitForAsyncUtils.sleep(250, java.util.concurrent.TimeUnit.MILLISECONDS);
        WaitForAsyncUtils.waitForFxEvents();
        assertEquals(1, table.getItems().size());
        assertEquals("3333", table.getItems().get(0).getTicker());
        assertEquals(35, table.getItems().get(0).getCurrentPrice(), 0.001);
    }

    @Test
    void addStockDialogSavesValidInputUsingCachedPrice() throws Exception {
        putCachedPrice("TEST", 20);

        openAddStockDialog();
        TextField tickerField = findTextField("Ticker Symbol");
        TextField qtyField = findTextField("Quantity");
        TextField priceField = findTextField("Average Buy Price");
        ComboBox<?> portfolioBox = lookup(node -> node instanceof ComboBox<?> comboBox && comboBox.isEditable())
                .queryAs(ComboBox.class);

        clickOn(tickerField).write("TEST");
        interact(() -> portfolioBox.getEditor().setText("New Portfolio"));
        clickOn(qtyField).write("2");
        clickOn(priceField).write("10");
        clickOn(findButton("Save Stock"));

        TableView<Stock> table = getField("table", TableView.class);
        waitForCurrentPrice(table, "TEST", 20);

        assertEquals(1, table.getItems().size());
        assertEquals("TEST", table.getItems().get(0).getTicker());
        assertEquals(20, table.getItems().get(0).getCurrentPrice(), 0.001);
    }

    @Test
    void refreshButtonRefreshesDropdownAndCurrentFilter() throws Exception {
        ComboBox<String> portfolioFilter = getField("portfolioFilter", ComboBox.class);
        TableView<Stock> table = getField("table", TableView.class);

        interact(() -> portfolioFilter.setValue("Tech (1)"));
        assertEquals(1, table.getItems().size());

        clickOn(findButton("Refresh"));

        assertEquals(1, table.getItems().size());
        assertEquals("AAPL", table.getItems().get(0).getTicker());
    }

    @Test
    void addStockDialogShowsSaudiPreviewAndSavesNumericTickerFromCache() throws Exception {
        putCachedPrice("1234", 40);

        openAddStockDialog();
        TextField tickerField = findTextField("Ticker Symbol");
        TextField qtyField = findTextField("Quantity");
        TextField priceField = findTextField("Average Buy Price");
        ComboBox<?> portfolioBox = lookup(node -> node instanceof ComboBox<?> comboBox && comboBox.isEditable())
                .queryAs(ComboBox.class);

        clickOn(tickerField).write("1234");
        assertTrue(hasLabelText("Saudi Market"));

        interact(() -> {
            tickerField.clear();
            tickerField.setText("");
        });
        assertFalse(hasLabelText("Saudi Market"));

        clickOn(tickerField).write("1234");
        interact(() -> portfolioBox.getEditor().setText("Saudi Cache"));
        clickOn(qtyField).write("1");
        clickOn(priceField).write("30");
        clickOn(findButton("Save Stock"));

        TableView<Stock> table = getField("table", TableView.class);
        waitForCurrentPrice(table, "1234", 40);

        assertEquals(1, table.getItems().size());
        assertEquals("1234", table.getItems().get(0).getTicker());
        assertEquals("Saudi Market - Tadawul", table.getItems().get(0).getMarket());
        assertEquals(40, table.getItems().get(0).getCurrentPrice(), 0.001);
    }

    // ── updatePercentageLabel coverage ────────────────────────────────────────

    @Test
    void updatePercentageLabelShowsLivePricesWhenStockListIsEmpty() throws Exception {
        Label pctLabel = getField("marketValuePercentageLabel", Label.class);

        invokeOnFx("updatePercentageLabel", new Class<?>[]{List.class}, List.of());

        assertEquals("live prices", pctLabel.getText());
    }

    @Test
    void updatePercentageLabelShowsGreenPercentageWhenPortfolioIsInProfit() throws Exception {
        Label pctLabel = getField("marketValuePercentageLabel", Label.class);

        // Buy price 100, current price 150 → +50%
        Stock profitStock = new Stock("AAPL", "US Market - Finnhub", 2, 100, "Tech");
        profitStock.setCurrentPrice(150);

        invokeOnFx("updatePercentageLabel", new Class<?>[]{List.class}, List.of(profitStock));

        assertTrue(pctLabel.getText().contains("+50.00%"));
        assertTrue(pctLabel.getText().startsWith("live prices"));
        // Color should be the profit green
        assertEquals(javafx.scene.paint.Color.web("#3fb950"), pctLabel.getTextFill());
    }

    @Test
    void updatePercentageLabelShowsRedPercentageWhenPortfolioIsAtLoss() throws Exception {
        Label pctLabel = getField("marketValuePercentageLabel", Label.class);

        // SAR stock: buy 40, current 20 → -50%
        Stock lossStock = new Stock("2222", "Saudi Market - Tadawul", 1, 40, "Energy");
        lossStock.setCurrentPrice(20);

        invokeOnFx("updatePercentageLabel", new Class<?>[]{List.class}, List.of(lossStock));

        assertTrue(pctLabel.getText().contains("-50.00%"));
        assertEquals(javafx.scene.paint.Color.web("#f85149"), pctLabel.getTextFill());
    }

    @Test
    void updatePercentageLabelShowsGreyPercentageWhenPortfolioIsBreakeven() throws Exception {
        Label pctLabel = getField("marketValuePercentageLabel", Label.class);

        // Buy price == current price → 0%
        Stock breakevenStock = new Stock("MSFT", "US Market - Finnhub", 1, 100, "Tech");
        breakevenStock.setCurrentPrice(100);

        invokeOnFx("updatePercentageLabel", new Class<?>[]{List.class}, List.of(breakevenStock));

        assertTrue(pctLabel.getText().contains("0.00%"));
        assertEquals(javafx.scene.paint.Color.web("#8b949e"), pctLabel.getTextFill());
    }

    @Test
    void updatePercentageLabelShowsZeroWhenAverageBuyPriceIsZero() throws Exception {
        Label pctLabel = getField("marketValuePercentageLabel", Label.class);

        // Stock with averageBuyPrice = 0 → totalCostUSD == 0 branch
        Stock zeroCostStock = new Stock();
        zeroCostStock.setTicker("ZERO");
        zeroCostStock.setMarket("US Market - Finnhub");
        zeroCostStock.setQuantity(1);
        zeroCostStock.setAverageBuyPrice(0);
        zeroCostStock.setPortfolioName("Test");
        zeroCostStock.setCurrentPrice(0);

        invokeOnFx("updatePercentageLabel", new Class<?>[]{List.class}, List.of(zeroCostStock));

        assertTrue(pctLabel.getText().contains("0.00%"));
    }

    @Test
    void deleteButtonCancelKeepsStockInTable() throws Exception {
        TableView<Stock> table = getField("table", TableView.class);
        int initialSize = table.getItems().size();

        // Click the delete (✕) button on the first row
        interact(() -> table.scrollTo(0));
        WaitForAsyncUtils.waitForFxEvents();

        // Find and click the ✕ button
        Button deleteBtn = lookup(node -> node instanceof Button btn
                && "✕".equals(btn.getText())).queryAs(Button.class);
        clickOn(deleteBtn);
        WaitForAsyncUtils.waitForFxEvents();

        // Click Cancel in the confirmation dialog
        Button cancelBtn = lookup(node -> node instanceof Button btn
                && btn.getText() != null
                && btn.getText().contains("Cancel")).queryAs(Button.class);
        clickOn(cancelBtn);
        WaitForAsyncUtils.waitForFxEvents();

        // Stock should still be present
        assertEquals(initialSize, table.getItems().size());
    }

    @Test
    void deleteButtonOkRemovesStockFromTable() throws Exception {
        TableView<Stock> table = getField("table", TableView.class);
        int initialSize = table.getItems().size();

        // Click the delete (✕) button on the first row
        interact(() -> table.scrollTo(0));
        WaitForAsyncUtils.waitForFxEvents();

        Button deleteBtn = lookup(node -> node instanceof Button btn
                && "✕".equals(btn.getText())).queryAs(Button.class);
        clickOn(deleteBtn);
        WaitForAsyncUtils.waitForFxEvents();

        // Click OK in the confirmation dialog
        Button okBtn = lookup(node -> node instanceof Button btn
                && btn.getText() != null
                && btn.getText().equals("OK")).queryAs(Button.class);
        clickOn(okBtn);
        WaitForAsyncUtils.waitForFxEvents();

        // One stock should have been removed
        assertEquals(initialSize - 1, table.getItems().size());
    }

    @SuppressWarnings("unchecked")
    private <T> T getField(String name, Class<T> type) throws Exception {
        Field field = MainView.class.getDeclaredField(name);
        field.setAccessible(true);
        return (T) field.get(mainView);
    }

    @SuppressWarnings("unchecked")
    private <T> T invoke(String name, Class<?>[] parameterTypes, Object... args) throws Exception {
        Method method = MainView.class.getDeclaredMethod(name, parameterTypes);
        method.setAccessible(true);
        return (T) method.invoke(mainView, args);
    }

    private <T> T invokeOnFx(String name, Class<?>[] parameterTypes, Object... args) {
        AtomicReference<T> result = new AtomicReference<>();
        AtomicReference<Exception> failure = new AtomicReference<>();
        interact(() -> {
            try {
                result.set(invoke(name, parameterTypes, args));
            } catch (Exception e) {
                failure.set(e);
            }
        });
        if (failure.get() != null) {
            throw new AssertionError(failure.get());
        }
        return result.get();
    }

    private void setDatabaseConnection(Connection connection) throws Exception {
        Field field = DatabaseConnection.class.getDeclaredField("connection");
        field.setAccessible(true);
        field.set(null, connection);
    }

    private void updateCellItem(TableCell<Stock, String> cell, String item, boolean empty) {
        try {
            Method method = cell.getClass().getDeclaredMethod("updateItem", Object.class, boolean.class);
            method.setAccessible(true);
            method.invoke(cell, item, empty);
        } catch (NoSuchMethodException e) {
            try {
                Method method = cell.getClass().getDeclaredMethod("updateItem", String.class, boolean.class);
                method.setAccessible(true);
                method.invoke(cell, item, empty);
            } catch (Exception inner) {
                throw new AssertionError(inner);
            }
        } catch (Exception e) {
            throw new AssertionError(e);
        }
    }

    private void updateListCellItem(ListCell<String> cell, String item, boolean empty) {
        try {
            Method method = cell.getClass().getDeclaredMethod("updateItem", Object.class, boolean.class);
            method.setAccessible(true);
            method.invoke(cell, item, empty);
        } catch (NoSuchMethodException e) {
            try {
                Method method = cell.getClass().getDeclaredMethod("updateItem", String.class, boolean.class);
                method.setAccessible(true);
                method.invoke(cell, item, empty);
            } catch (Exception inner) {
                throw new AssertionError(inner);
            }
        } catch (Exception e) {
            throw new AssertionError(e);
        }
    }

    private Button findButton(String textPart) {
        return lookup(node -> node instanceof Button button
                && button.getText() != null
                && button.getText().contains(textPart)).queryAs(Button.class);
    }

    private void openAddStockDialog() {
        clickOn(findButton("Add Stock"));
        // Give the modal Stage time to render before querying its nodes
        WaitForAsyncUtils.sleep(300, java.util.concurrent.TimeUnit.MILLISECONDS);
        WaitForAsyncUtils.waitForFxEvents();
        findTextField("Ticker Symbol");
    }

    private TextField findTextField(String promptPart) {
        return lookup(node -> node instanceof TextField textField
                && textField.getPromptText() != null
                && textField.getPromptText().contains(promptPart)).queryAs(TextField.class);
    }

    private boolean hasLabelText(String textPart) {
        return !lookup(node -> node instanceof Label label
                && label.getText() != null
                && label.getText().contains(textPart)).queryAll().isEmpty();
    }

    private void waitForCurrentPrice(TableView<Stock> table, String ticker, double expectedPrice) {
        for (int i = 0; i < 20; i++) {
            WaitForAsyncUtils.sleep(100, java.util.concurrent.TimeUnit.MILLISECONDS);
            WaitForAsyncUtils.waitForFxEvents();
            boolean found = table.getItems().stream()
                    .anyMatch(stock -> ticker.equals(stock.getTicker())
                            && Math.abs(stock.getCurrentPrice() - expectedPrice) < 0.001);
            if (found) {
                return;
            }
        }
    }

    @SuppressWarnings("unchecked")
    private void putCachedPrice(String ticker, double price) throws Exception {
        Field cacheField = CachedPriceFetcher.class.getDeclaredField("CACHE");
        cacheField.setAccessible(true);
        Map<String, Object> cache = (Map<String, Object>) cacheField.get(null);

        Class<?> entryClass = Class.forName("sa.edu.kau.fcit.cpit252.project.api.CachedPriceFetcher$CacheEntry");
        Constructor<?> constructor = entryClass.getDeclaredConstructor(double.class, long.class);
        constructor.setAccessible(true);
        cache.put(ticker, constructor.newInstance(price, System.currentTimeMillis()));
    }
}
