package sa.edu.kau.fcit.cpit252.project.api;

import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

/**
 * Proxy Pattern implementation for PriceFetcher.
 * Intercepts API calls to return cached prices if fetched less than 5 minutes ago.
 */
public class CachedPriceFetcher implements PriceFetcher {
    private final PriceFetcher baseFetcher;
    private static final Map<String, CacheEntry> CACHE = new ConcurrentHashMap<>();
    private static final long CACHE_EXPIRY_MS = 5 * 60 * 1000; // 5 minutes

    public CachedPriceFetcher(PriceFetcher baseFetcher) {
        this.baseFetcher = baseFetcher;
    }

    @Override
    public double fetchPrice(String ticker) throws Exception {
        CacheEntry entry = CACHE.get(ticker);
        long now = System.currentTimeMillis();

        if (entry != null && (now - entry.timestamp) < CACHE_EXPIRY_MS) {
            System.out.println("Returning cached price for " + ticker);
            return entry.price;
        }

        System.out.println("Cache miss or expired for " + ticker + ". Fetching from API...");
        double price = baseFetcher.fetchPrice(ticker);
        CACHE.put(ticker, new CacheEntry(price, now));
        return price;
    }

    private static class CacheEntry {
        final double price;
        final long timestamp;

        CacheEntry(double price, long timestamp) {
            this.price = price;
            this.timestamp = timestamp;
        }
    }
}
