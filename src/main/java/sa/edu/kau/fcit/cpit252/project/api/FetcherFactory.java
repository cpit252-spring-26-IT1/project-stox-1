package sa.edu.kau.fcit.cpit252.project.api;

public class FetcherFactory {
    
    /**
     * Factory method to get the correct PriceFetcher strategy based on the market string.
     * 
     * @param market The market string (e.g., "US Market - Finnhub", "Saudi Market - Tadawul").
     * @return The corresponding PriceFetcher implementation.
     * @throws IllegalArgumentException if the market is unknown.
     */
    public static PriceFetcher getFetcher(String market) {
        if (market == null) {
            throw new IllegalArgumentException("Market cannot be null");
        }
        
        PriceFetcher baseFetcher;
        if (market.contains("Saudi Market - Tadawul") || market.contains("Saudi")) {
            baseFetcher = new SahmkFetcher();
        } else if (market.contains("US Market - Finnhub") || market.contains("US")) {
            baseFetcher = new FinnhubFetcher();
        } else {
            throw new IllegalArgumentException("Unknown market type: " + market);
        }

        // Wrap the base fetcher with Retry (Decorator) and then Cache (Proxy)
        PriceFetcher withRetry = new RetryPriceFetcher(baseFetcher);
        return new CachedPriceFetcher(withRetry);
    }
}
