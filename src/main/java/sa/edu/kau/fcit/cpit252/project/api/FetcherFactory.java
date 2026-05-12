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
        
        if (market.contains("Saudi Market - Tadawul") || market.contains("Saudi")) {
            return new SahmkFetcher();
        } else if (market.contains("US Market - Finnhub") || market.contains("US")) {
            return new FinnhubFetcher();
        }
        
        throw new IllegalArgumentException("Unknown market type: " + market);
    }
}
