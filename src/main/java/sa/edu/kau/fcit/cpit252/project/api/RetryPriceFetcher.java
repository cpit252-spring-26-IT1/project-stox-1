package sa.edu.kau.fcit.cpit252.project.api;

/**
 * Decorator Pattern implementation for PriceFetcher.
 * Wraps a PriceFetcher to provide automatic retry logic on failure.
 */
public class RetryPriceFetcher implements PriceFetcher {
    private final PriceFetcher baseFetcher;
    private static final int MAX_RETRIES = 3;

    public RetryPriceFetcher(PriceFetcher baseFetcher) {
        this.baseFetcher = baseFetcher;
    }

    @Override
    public double fetchPrice(String ticker) throws Exception {
        int attempts = 0;
        while (true) {
            try {
                return baseFetcher.fetchPrice(ticker);
            } catch (Exception e) {
                attempts++;
                System.err.println("Attempt " + attempts + " failed for ticker: " + ticker + " - " + e.getMessage());
                if (attempts > MAX_RETRIES) {
                    System.err.println("Max retries (" + MAX_RETRIES + ") exceeded for " + ticker + ". Failing gracefully.");
                    throw e;
                }
                // Optional: add a small delay between retries
                try {
                    Thread.sleep(500);
                } catch (InterruptedException ie) {
                    Thread.currentThread().interrupt();
                    throw new RuntimeException("Retry interrupted", ie);
                }
            }
        }
    }
}
