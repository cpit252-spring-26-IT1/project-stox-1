package sa.edu.kau.fcit.cpit252.project.api;

public interface PriceFetcher {
    /**
     * Fetches the current price of a stock ticker.
     *
     * @param ticker The stock ticker symbol.
     * @return The current price.
     * @throws Exception If an error occurs during fetching.
     */
    double fetchPrice(String ticker) throws Exception;
}
