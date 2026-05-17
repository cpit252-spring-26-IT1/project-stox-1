package sa.edu.kau.fcit.cpit252.project.api;

import sa.edu.kau.fcit.cpit252.project.model.StockSuggestion;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.concurrent.CompletableFuture;

/**
 * Service to load stock datasets asynchronously from resources and provide ranked search suggestions.
 */
public class StockSuggestionService {
    private static final List<StockSuggestion> suggestions = new ArrayList<>();
    private static boolean loaded = false;

    static {
        // Start loading the CSV files in a background thread upon startup
        CompletableFuture.runAsync(() -> {
            try {
                loadSuggestions();
            } catch (Exception e) {
                System.err.println("Error loading stock datasets: " + e.getMessage());
            }
        });
    }

    /**
     * Loads stock datasets from the resources folder and populates the cache.
     */
    private static void loadSuggestions() {
        List<StockSuggestion> temp = new ArrayList<>();

        // 1. Load US Market Stocks
        try (InputStream is = StockSuggestionService.class.getResourceAsStream("/stocksList.csv")) {
            if (is != null) {
                try (BufferedReader reader = new BufferedReader(new InputStreamReader(is, "UTF-8"))) {
                    String line;
                    while ((line = reader.readLine()) != null) {
                        List<String> fields = parseLine(line);
                        if (fields.size() < 2) continue;
                        String symbol = fields.get(0);
                        String name = fields.get(1);
                        if (symbol.isEmpty() || symbol.equalsIgnoreCase("Symbol") || name.isEmpty()) {
                            continue;
                        }
                        temp.add(new StockSuggestion(symbol, name, "US Market - Finnhub"));
                    }
                }
            } else {
                System.err.println("Resource stocksList.csv not found!");
            }
        } catch (IOException e) {
            System.err.println("Error reading stocksList.csv: " + e.getMessage());
        }

        // 2. Load Saudi Market Stocks
        try (InputStream is = StockSuggestionService.class.getResourceAsStream("/stocksListSA.csv")) {
            if (is != null) {
                try (BufferedReader reader = new BufferedReader(new InputStreamReader(is, "UTF-8"))) {
                    String line;
                    while ((line = reader.readLine()) != null) {
                        List<String> fields = parseLine(line);
                        if (fields.size() < 2) continue;
                        String symbol = fields.get(0);
                        String name = fields.get(1);
                        if (symbol.isEmpty() || symbol.equalsIgnoreCase("Symbol") || name.isEmpty()) {
                            continue;
                        }
                        temp.add(new StockSuggestion(symbol, name, "Saudi Market - Tadawul"));
                    }
                }
            } else {
                System.err.println("Resource stocksListSA.csv not found!");
            }
        } catch (IOException e) {
            System.err.println("Error reading stocksListSA.csv: " + e.getMessage());
        }

        synchronized (suggestions) {
            suggestions.clear();
            suggestions.addAll(temp);
            loaded = true;
        }
        System.out.println("StockSuggestionService: Loaded " + temp.size() + " suggestions successfully.");
    }

    /**
     * Robust double-quote aware CSV line parser.
     * Correctly handles commas inside quotes (e.g. "Aseer Trading, Tourism and Manufacturing Co.").
     */
    private static List<String> parseLine(String csvLine) {
        List<String> result = new ArrayList<>();
        if (csvLine == null || csvLine.isEmpty()) {
            return result;
        }
        StringBuilder curVal = new StringBuilder();
        boolean inQuotes = false;
        char[] chars = csvLine.toCharArray();
        for (char ch : chars) {
            if (inQuotes) {
                if (ch == '\"') {
                    inQuotes = false;
                } else {
                    curVal.append(ch);
                }
            } else {
                if (ch == '\"') {
                    inQuotes = true;
                } else if (ch == ',') {
                    result.add(curVal.toString().trim());
                    curVal = new StringBuilder();
                } else {
                    curVal.append(ch);
                }
            }
        }
        result.add(curVal.toString().trim());
        return result;
    }

    /**
     * Searches suggestions by matching ticker or name, sorting them by matching tier/rank.
     */
    public static List<StockSuggestion> search(String query, int maxResults) {
        if (query == null || query.trim().isEmpty()) {
            return Collections.emptyList();
        }

        String lowerQuery = query.trim().toLowerCase();
        List<StockSuggestion> exactTickerMatches = new ArrayList<>();
        List<StockSuggestion> prefixTickerMatches = new ArrayList<>();
        List<StockSuggestion> prefixNameMatches = new ArrayList<>();
        List<StockSuggestion> containsTickerMatches = new ArrayList<>();
        List<StockSuggestion> containsNameMatches = new ArrayList<>();

        synchronized (suggestions) {
            for (StockSuggestion s : suggestions) {
                String lowerTicker = s.getTicker().toLowerCase();
                String lowerName = s.getName().toLowerCase();

                if (lowerTicker.equals(lowerQuery)) {
                    exactTickerMatches.add(s);
                } else if (lowerTicker.startsWith(lowerQuery)) {
                    prefixTickerMatches.add(s);
                } else if (lowerName.startsWith(lowerQuery)) {
                    prefixNameMatches.add(s);
                } else if (lowerTicker.contains(lowerQuery)) {
                    containsTickerMatches.add(s);
                } else if (lowerName.contains(lowerQuery)) {
                    containsNameMatches.add(s);
                }
            }
        }

        List<StockSuggestion> combined = new ArrayList<>();
        combined.addAll(exactTickerMatches);
        combined.addAll(prefixTickerMatches);
        combined.addAll(prefixNameMatches);
        combined.addAll(containsTickerMatches);
        combined.addAll(containsNameMatches);

        if (combined.size() > maxResults) {
            return combined.subList(0, maxResults);
        }
        return combined;
    }

    /**
     * Checks if the dataset loading has completed.
     */
    public static boolean isLoaded() {
        synchronized (suggestions) {
            return loaded;
        }
    }

    /**
     * Finds and returns the stock name for a given ticker, or "Unknown Stock" if not found.
     */
    public static String getStockName(String ticker) {
        if (ticker == null || ticker.trim().isEmpty()) {
            return "Unknown Stock";
        }
        String upperTicker = ticker.trim().toUpperCase();
        synchronized (suggestions) {
            for (StockSuggestion s : suggestions) {
                if (s.getTicker().equalsIgnoreCase(upperTicker)) {
                    return s.getName();
                }
            }
        }
        return "Unknown Stock";
    }
}
