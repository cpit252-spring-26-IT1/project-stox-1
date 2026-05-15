package sa.edu.kau.fcit.cpit252.project.api;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.lang.reflect.Field;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.assertEquals;

class CachedPriceFetcherTest {

    @BeforeEach
    void clearSharedCache() throws Exception {
        getCache().clear();
    }

    @Test
    void fetchPriceUsesCachedValueForRepeatedTicker() throws Exception {
        CountingPriceFetcher baseFetcher = new CountingPriceFetcher(123.45);
        CachedPriceFetcher cachedFetcher = new CachedPriceFetcher(baseFetcher);
        String ticker = "CACHE_TEST_" + System.nanoTime();

        double firstPrice = cachedFetcher.fetchPrice(ticker);
        double secondPrice = cachedFetcher.fetchPrice(ticker);

        assertEquals(123.45, firstPrice, 0.001);
        assertEquals(123.45, secondPrice, 0.001);
        assertEquals(1, baseFetcher.callCount);
    }

    @Test
    void fetchPriceCallsBaseFetcherForDifferentTickers() throws Exception {
        CountingPriceFetcher baseFetcher = new CountingPriceFetcher(50);
        CachedPriceFetcher cachedFetcher = new CachedPriceFetcher(baseFetcher);

        cachedFetcher.fetchPrice("CACHE_TEST_A_" + System.nanoTime());
        cachedFetcher.fetchPrice("CACHE_TEST_B_" + System.nanoTime());

        assertEquals(2, baseFetcher.callCount);
    }

    @Test
    void fetchPriceRefreshesExpiredCacheEntry() throws Exception {
        CountingPriceFetcher baseFetcher = new CountingPriceFetcher(10, 20);
        CachedPriceFetcher cachedFetcher = new CachedPriceFetcher(baseFetcher);
        String ticker = "CACHE_EXPIRED_TEST";

        assertEquals(10, cachedFetcher.fetchPrice(ticker), 0.001);
        expireCacheEntry(ticker);

        assertEquals(20, cachedFetcher.fetchPrice(ticker), 0.001);
        assertEquals(2, baseFetcher.callCount);
    }

    @SuppressWarnings("unchecked")
    private Map<String, Object> getCache() throws Exception {
        Field cacheField = CachedPriceFetcher.class.getDeclaredField("CACHE");
        cacheField.setAccessible(true);
        return (Map<String, Object>) cacheField.get(null);
    }

    private void expireCacheEntry(String ticker) throws Exception {
        Object entry = getCache().get(ticker);
        Field timestampField = entry.getClass().getDeclaredField("timestamp");
        timestampField.setAccessible(true);
        timestampField.setLong(entry, 0L);
    }

    private static class CountingPriceFetcher implements PriceFetcher {
        private final double[] prices;
        private int callCount;

        private CountingPriceFetcher(double... prices) {
            this.prices = prices;
        }

        @Override
        public double fetchPrice(String ticker) {
            callCount++;
            return prices[Math.min(callCount - 1, prices.length - 1)];
        }
    }
}
