package sa.edu.kau.fcit.cpit252.project.api;

import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;
import sa.edu.kau.fcit.cpit252.project.model.StockSuggestion;

import java.util.List;

import static org.junit.jupiter.api.Assertions.*;

class StockSuggestionServiceTest {

    @BeforeAll
    static void setUp() throws InterruptedException {
        // Wait for the asynchronous CSV loading to complete (up to 2 seconds)
        int retries = 20;
        while (!StockSuggestionService.isLoaded() && retries > 0) {
            Thread.sleep(100);
            retries--;
        }
        assertTrue(StockSuggestionService.isLoaded(), "Stock suggestions dataset failed to load in time.");
    }

    @Test
    void searchReturnsExactTickerMatchesFirst() {
        // Query for exactly "AAPL"
        List<StockSuggestion> results = StockSuggestionService.search("AAPL", 5);
        assertFalse(results.isEmpty(), "Search for AAPL should not be empty");
        
        // The first match must be exactly AAPL
        StockSuggestion first = results.get(0);
        assertEquals("AAPL", first.getTicker());
        assertEquals("US Market - Finnhub", first.getMarket());
    }

    @Test
    void searchIsCaseInsensitive() {
        List<StockSuggestion> resultsUpper = StockSuggestionService.search("AAPL", 5);
        List<StockSuggestion> resultsLower = StockSuggestionService.search("aapl", 5);
        List<StockSuggestion> resultsMixed = StockSuggestionService.search("AaPl", 5);

        assertFalse(resultsUpper.isEmpty());
        assertEquals(resultsUpper.size(), resultsLower.size());
        assertEquals(resultsUpper.size(), resultsMixed.size());
        assertEquals(resultsUpper.get(0).getTicker(), resultsLower.get(0).getTicker());
        assertEquals(resultsUpper.get(0).getTicker(), resultsMixed.get(0).getTicker());
    }

    @Test
    void searchMatchesSaudiMarketSymbols() {
        // 2222 is Saudi Aramco
        List<StockSuggestion> results = StockSuggestionService.search("2222", 5);
        assertFalse(results.isEmpty(), "Search for 2222 should find Saudi Aramco");
        
        StockSuggestion aramco = results.get(0);
        assertEquals("2222", aramco.getTicker());
        assertTrue(aramco.getName().contains("Saudi Arabian Oil"), "Name should match Saudi Arabian Oil Co.");
        assertEquals("Saudi Market - Tadawul", aramco.getMarket());
    }

    @Test
    void searchMatchesCompanyNames() {
        // Search by company name e.g. "Apple"
        List<StockSuggestion> results = StockSuggestionService.search("Apple", 5);
        assertFalse(results.isEmpty(), "Search for 'Apple' should yield results");
        
        boolean foundAppleInc = false;
        for (StockSuggestion s : results) {
            if (s.getTicker().equals("AAPL")) {
                foundAppleInc = true;
                break;
            }
        }
        assertTrue(foundAppleInc, "Search for 'Apple' name should find AAPL");
    }

    @Test
    void searchReturnsEmptyListOnEmptyQuery() {
        assertTrue(StockSuggestionService.search("", 5).isEmpty());
        assertTrue(StockSuggestionService.search("   ", 5).isEmpty());
        assertTrue(StockSuggestionService.search(null, 5).isEmpty());
    }

    @Test
    void getStockNameResolvesTickersCorrectly() {
        // US Market lookup
        assertEquals("Apple Inc. Common Stock", StockSuggestionService.getStockName("AAPL"));
        assertEquals("Apple Inc. Common Stock", StockSuggestionService.getStockName("aapl")); // case insensitivity

        // Saudi Market lookup
        assertEquals("Saudi Arabian Oil Co.", StockSuggestionService.getStockName("2222"));

        // Fallbacks
        assertEquals("Unknown Stock", StockSuggestionService.getStockName("NONEXISTENT_TICKER"));
        assertEquals("Unknown Stock", StockSuggestionService.getStockName(null));
        assertEquals("Unknown Stock", StockSuggestionService.getStockName("   "));
    }
}
