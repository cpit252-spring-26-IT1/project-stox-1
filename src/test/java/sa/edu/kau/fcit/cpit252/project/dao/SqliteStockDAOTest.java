package sa.edu.kau.fcit.cpit252.project.dao;

import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;
import sa.edu.kau.fcit.cpit252.project.db.DatabaseConnection;
import sa.edu.kau.fcit.cpit252.project.model.Stock;

import java.io.ByteArrayOutputStream;
import java.io.PrintStream;
import java.lang.reflect.Field;
import java.nio.file.Path;
import java.sql.Connection;
import java.sql.DriverManager;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertDoesNotThrow;
import static org.junit.jupiter.api.Assertions.assertTrue;

class SqliteStockDAOTest {

    @TempDir
    Path tempDir;

    private Connection connection;
    private SqliteStockDAO dao;

    @BeforeEach
    void setUp() throws Exception {
        Class.forName("org.sqlite.JDBC");
        connection = DriverManager.getConnection("jdbc:sqlite:" + tempDir.resolve("stox-test.db"));
        setDatabaseConnection(connection);
        dao = new SqliteStockDAO();
    }

    @AfterEach
    void tearDown() throws Exception {
        setDatabaseConnection(null);
        if (connection != null) {
            connection.close();
        }
    }

    @Test
    void addStockInsertsUppercaseTickerAndDefaultPortfolio() {
        dao.addStock(new Stock("aapl", "US Market - Finnhub", 2, 150, ""));

        Stock stored = dao.getPortfolio().get(0);

        assertEquals("AAPL", stored.getTicker());
        assertEquals("Main Portfolio", stored.getPortfolioName());
        assertEquals(2, stored.getQuantity(), 0.001);
        assertEquals(150, stored.getAverageBuyPrice(), 0.001);
    }

    @Test
    void addStockUsesDefaultPortfolioWhenPortfolioNameIsNull() {
        dao.addStock(new Stock("MSFT", "US Market - Finnhub", 1, 250, null));

        Stock stored = dao.getPortfolio().get(0);

        assertEquals("Main Portfolio", stored.getPortfolioName());
    }

    @Test
    void addStockMergesDuplicateTickerInSamePortfolioUsingWeightedAverage() {
        dao.addStock(new Stock("AAPL", "US Market - Finnhub", 2, 100, "Tech"));
        dao.addStock(new Stock("AAPL", "US Market - Finnhub", 3, 200, "Tech"));

        Stock stored = dao.getStocksByPortfolio("Tech").get(0);

        assertEquals(5, stored.getQuantity(), 0.001);
        assertEquals(160, stored.getAverageBuyPrice(), 0.001);
    }

    @Test
    void getStocksByPortfolioFiltersPortfolioNames() {
        dao.addStock(new Stock("AAPL", "US Market - Finnhub", 1, 100, "Tech"));
        dao.addStock(new Stock("2222", "Saudi Market - Tadawul", 1, 30, "Saudi"));

        List<Stock> techStocks = dao.getStocksByPortfolio("Tech");

        assertEquals(1, techStocks.size());
        assertEquals("AAPL", techStocks.get(0).getTicker());
    }

    @Test
    void getAllPortfolioNamesReturnsSortedDistinctNames() {
        dao.addStock(new Stock("AAPL", "US Market - Finnhub", 1, 100, "Tech"));
        dao.addStock(new Stock("MSFT", "US Market - Finnhub", 1, 200, "Tech"));
        dao.addStock(new Stock("2222", "Saudi Market - Tadawul", 1, 30, "Saudi"));

        assertEquals(List.of("Saudi", "Tech"), dao.getAllPortfolioNames());
    }

    @Test
    void updateStockChangesMatchingTickerAndPortfolio() {
        dao.addStock(new Stock("AAPL", "US Market - Finnhub", 1, 100, "Tech"));

        dao.updateStock(new Stock("AAPL", "US Market - Finnhub", 4, 125, "Tech"));

        Stock stored = dao.getStocksByPortfolio("Tech").get(0);
        assertEquals(4, stored.getQuantity(), 0.001);
        assertEquals(125, stored.getAverageBuyPrice(), 0.001);
    }

    @Test
    void removeStockDeletesOnlyMatchingTickerAndPortfolio() {
        dao.addStock(new Stock("AAPL", "US Market - Finnhub", 1, 100, "Tech"));
        dao.addStock(new Stock("AAPL", "US Market - Finnhub", 2, 150, "Retirement"));

        dao.removeStock("AAPL", "Tech");

        assertTrue(dao.getStocksByPortfolio("Tech").isEmpty());
        assertEquals(1, dao.getStocksByPortfolio("Retirement").size());
    }

    @Test
    void daoMethodsHandleSqlExceptionsWithoutThrowing() throws Exception {
        connection.close();
        PrintStream originalErr = System.err;

        try {
            System.setErr(new PrintStream(new ByteArrayOutputStream()));

            assertDoesNotThrow(SqliteStockDAO::new);
            assertDoesNotThrow(() -> dao.addStock(new Stock("AAPL", "US Market - Finnhub", 1, 100, "Tech")));
            assertDoesNotThrow(() -> assertTrue(dao.getPortfolio().isEmpty()));
            assertDoesNotThrow(() -> assertTrue(dao.getStocksByPortfolio("Tech").isEmpty()));
            assertDoesNotThrow(() -> assertTrue(dao.getAllPortfolioNames().isEmpty()));
            assertDoesNotThrow(() -> dao.updateStock(new Stock("AAPL", "US Market - Finnhub", 2, 200, "Tech")));
            assertDoesNotThrow(() -> dao.removeStock("AAPL", "Tech"));
        } finally {
            System.setErr(originalErr);
        }
    }

    private void setDatabaseConnection(Connection connection) throws Exception {
        Field field = DatabaseConnection.class.getDeclaredField("connection");
        field.setAccessible(true);
        field.set(null, connection);
    }
}
