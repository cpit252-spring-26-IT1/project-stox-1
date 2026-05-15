package sa.edu.kau.fcit.cpit252.project.model;

import org.junit.jupiter.api.Test;

import java.io.ByteArrayOutputStream;
import java.io.PrintStream;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

class StockTest {

    @Test
    void getValueReturnsQuantityTimesAverageBuyPrice() {
        Stock stock = new Stock("AAPL", "US Market - Finnhub", 10, 150);

        assertEquals(1500, stock.getValue(), 0.001);
    }

    @Test
    void getPnLReturnsPriceDifferenceTimesQuantity() {
        Stock stock = new Stock("AAPL", "US Market - Finnhub", 10, 150);
        stock.setCurrentPrice(175);

        assertEquals(250, stock.getPnL(), 0.001);
    }

    @Test
    void getPnLReturnsZeroWhenCurrentPriceIsUnavailable() {
        Stock stock = new Stock("AAPL", "US Market - Finnhub", 10, 150);

        assertEquals(0, stock.getPnL(), 0.001);
    }

    @Test
    void defaultConstructorAndSettersStoreAllStockFields() {
        Stock stock = new Stock();

        stock.setTicker("MSFT");
        stock.setMarket("US Market - Finnhub");
        stock.setQuantity(3);
        stock.setAverageBuyPrice(200);
        stock.setPortfolioName("Tech");
        stock.setCurrentPrice(225);

        assertEquals("MSFT", stock.getTicker());
        assertEquals("MSFT", stock.getName());
        assertEquals("US Market - Finnhub", stock.getMarket());
        assertEquals(3, stock.getQuantity(), 0.001);
        assertEquals(200, stock.getAverageBuyPrice(), 0.001);
        assertEquals("Tech", stock.getPortfolioName());
        assertEquals(225, stock.getCurrentPrice(), 0.001);
    }

    @Test
    void constructorWithoutPortfolioUsesMainPortfolio() {
        Stock stock = new Stock("AAPL", "US Market - Finnhub", 1, 100);

        assertEquals("Main Portfolio", stock.getPortfolioName());
    }

    @Test
    void getCurrencySymbolReturnsSarForTadawulAndUsdOtherwise() {
        Stock saudiStock = new Stock("2222", "Saudi Market - Tadawul", 1, 30);
        Stock usStock = new Stock("AAPL", "US Market - Finnhub", 1, 100);
        Stock unknownMarketStock = new Stock("CASH", null, 1, 1);

        assertEquals("SAR", saudiStock.getCurrencySymbol());
        assertEquals("USD", usStock.getCurrencySymbol());
        assertEquals("USD", unknownMarketStock.getCurrencySymbol());
    }

    @Test
    void displayPrintsStockNameAndValue() {
        Stock stock = new Stock("AAPL", "US Market - Finnhub", 2, 100);
        ByteArrayOutputStream output = new ByteArrayOutputStream();
        PrintStream originalOut = System.out;

        try {
            System.setOut(new PrintStream(output));
            stock.display("  ");
        } finally {
            System.setOut(originalOut);
        }

        assertTrue(output.toString().contains("Stock: AAPL | value: 200.0"));
    }

    @Test
    void toStringIncludesImportantFields() {
        Stock stock = new Stock("AAPL", "US Market - Finnhub", 2, 100, "Tech");

        String text = stock.toString();

        assertTrue(text.contains("ticker='AAPL'"));
        assertTrue(text.contains("market='US Market - Finnhub'"));
        assertTrue(text.contains("qty=2.0"));
        assertTrue(text.contains("avgBuy=100.0"));
        assertTrue(text.contains("portfolio='Tech'"));
    }
}
