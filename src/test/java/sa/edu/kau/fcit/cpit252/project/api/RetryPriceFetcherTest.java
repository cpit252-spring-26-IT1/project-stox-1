package sa.edu.kau.fcit.cpit252.project.api;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertSame;
import static org.junit.jupiter.api.Assertions.assertThrows;

class RetryPriceFetcherTest {

    @Test
    void fetchPriceRetriesFailuresAndReturnsSuccessfulPrice() throws Exception {
        FailingThenSuccessfulFetcher baseFetcher = new FailingThenSuccessfulFetcher(2, 88.75);
        RetryPriceFetcher retryFetcher = new RetryPriceFetcher(baseFetcher);

        double price = retryFetcher.fetchPrice("AAPL");

        assertEquals(88.75, price, 0.001);
        assertEquals(3, baseFetcher.callCount);
    }

    @Test
    void fetchPriceThrowsAfterRetryLimitIsExceeded() {
        AlwaysFailingFetcher baseFetcher = new AlwaysFailingFetcher();
        RetryPriceFetcher retryFetcher = new RetryPriceFetcher(baseFetcher);

        Exception thrown = assertThrows(Exception.class, () -> retryFetcher.fetchPrice("AAPL"));

        assertSame(baseFetcher.failure, thrown);
        assertEquals(4, baseFetcher.callCount);
    }

    @Test
    void fetchPriceRestoresInterruptStatusWhenRetrySleepIsInterrupted() {
        AlwaysFailingFetcher baseFetcher = new AlwaysFailingFetcher();
        RetryPriceFetcher retryFetcher = new RetryPriceFetcher(baseFetcher);

        Thread.currentThread().interrupt();
        RuntimeException thrown = assertThrows(RuntimeException.class, () -> retryFetcher.fetchPrice("AAPL"));

        assertEquals("Retry interrupted", thrown.getMessage());
        assertEquals(1, baseFetcher.callCount);
        assertEquals(true, Thread.interrupted());
    }

    private static class FailingThenSuccessfulFetcher implements PriceFetcher {
        private final int failuresBeforeSuccess;
        private final double successPrice;
        private int callCount;

        private FailingThenSuccessfulFetcher(int failuresBeforeSuccess, double successPrice) {
            this.failuresBeforeSuccess = failuresBeforeSuccess;
            this.successPrice = successPrice;
        }

        @Override
        public double fetchPrice(String ticker) throws Exception {
            callCount++;
            if (callCount <= failuresBeforeSuccess) {
                throw new Exception("Temporary failure");
            }
            return successPrice;
        }
    }

    private static class AlwaysFailingFetcher implements PriceFetcher {
        private final Exception failure = new Exception("Permanent failure");
        private int callCount;

        @Override
        public double fetchPrice(String ticker) throws Exception {
            callCount++;
            throw failure;
        }
    }
}
