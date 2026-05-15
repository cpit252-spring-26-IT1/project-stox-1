package sa.edu.kau.fcit.cpit252.project.model;

import org.junit.jupiter.api.Test;

import java.io.ByteArrayOutputStream;
import java.io.PrintStream;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

class PortfolioTest {

    @Test
    void getValueReturnsSumOfAllStockValues() {
        Portfolio portfolio = new Portfolio("Main Portfolio");
        portfolio.add(new Stock("AAPL", "US Market - Finnhub", 2, 150));
        portfolio.add(new Stock("2222", "Saudi Market - Tadawul", 5, 30));

        assertEquals(450, portfolio.getValue(), 0.001);
    }

    @Test
    void getValueIncludesNestedPortfolioValues() {
        Portfolio parent = new Portfolio("All Holdings");
        Portfolio retirement = new Portfolio("Retirement");

        parent.add(new Stock("MSFT", "US Market - Finnhub", 1, 300));
        retirement.add(new Stock("AAPL", "US Market - Finnhub", 2, 100));
        parent.add(retirement);

        assertEquals(500, parent.getValue(), 0.001);
    }

    @Test
    void getNameReturnsPortfolioName() {
        Portfolio portfolio = new Portfolio("Growth");

        assertEquals("Growth", portfolio.getName());
    }

    @Test
    void removeExcludesComponentFromTotalValue() {
        Portfolio portfolio = new Portfolio("Main Portfolio");
        Stock stock = new Stock("AAPL", "US Market - Finnhub", 2, 150);

        portfolio.add(stock);
        portfolio.remove(stock);

        assertEquals(0, portfolio.getValue(), 0.001);
    }

    @Test
    void displayPrintsPortfolioAndChildStocks() {
        Portfolio portfolio = new Portfolio("Main Portfolio");
        portfolio.add(new Stock("AAPL", "US Market - Finnhub", 2, 150));
        ByteArrayOutputStream output = new ByteArrayOutputStream();
        PrintStream originalOut = System.out;

        try {
            System.setOut(new PrintStream(output));
            portfolio.display("");
        } finally {
            System.setOut(originalOut);
        }

        String text = output.toString();
        assertTrue(text.contains("Portfolio: Main Portfolio | Total value 300.0"));
        assertTrue(text.contains("Stock: AAPL | value: 300.0"));
    }
}
