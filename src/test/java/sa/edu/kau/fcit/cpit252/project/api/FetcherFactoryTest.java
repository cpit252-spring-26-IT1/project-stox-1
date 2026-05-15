package sa.edu.kau.fcit.cpit252.project.api;

import org.junit.jupiter.api.Test;

import java.lang.reflect.Field;

import static org.junit.jupiter.api.Assertions.assertInstanceOf;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertThrows;

class FetcherFactoryTest {

    @Test
    void getFetcherReturnsCachedRetryFinnhubFetcherForUsMarket() throws Exception {
        PriceFetcher fetcher = FetcherFactory.getFetcher("US Market - Finnhub");

        assertInstanceOf(CachedPriceFetcher.class, fetcher);
        PriceFetcher retryFetcher = getBaseFetcher(fetcher);
        assertInstanceOf(RetryPriceFetcher.class, retryFetcher);
        assertInstanceOf(FinnhubFetcher.class, getBaseFetcher(retryFetcher));
    }

    @Test
    void getFetcherReturnsCachedRetrySahmkFetcherForSaudiMarket() throws Exception {
        PriceFetcher fetcher = FetcherFactory.getFetcher("Saudi Market - Tadawul");

        assertInstanceOf(CachedPriceFetcher.class, fetcher);
        PriceFetcher retryFetcher = getBaseFetcher(fetcher);
        assertInstanceOf(RetryPriceFetcher.class, retryFetcher);
        assertInstanceOf(SahmkFetcher.class, getBaseFetcher(retryFetcher));
    }

    @Test
    void getFetcherAcceptsShortMarketAliases() throws Exception {
        assertInstanceOf(FinnhubFetcher.class, getBaseFetcher(getBaseFetcher(FetcherFactory.getFetcher("US"))));
        assertInstanceOf(SahmkFetcher.class, getBaseFetcher(getBaseFetcher(FetcherFactory.getFetcher("Saudi"))));
    }

    @Test
    void getFetcherRejectsUnknownMarket() {
        assertThrows(IllegalArgumentException.class, () -> FetcherFactory.getFetcher("Unknown Market"));
    }

    @Test
    void getFetcherRejectsNullMarket() {
        assertThrows(IllegalArgumentException.class, () -> FetcherFactory.getFetcher(null));
    }

    @Test
    void factoryCanBeConstructed() {
        assertNotNull(new FetcherFactory());
    }

    private PriceFetcher getBaseFetcher(PriceFetcher fetcher) throws Exception {
        Field field = fetcher.getClass().getDeclaredField("baseFetcher");
        field.setAccessible(true);
        return (PriceFetcher) field.get(fetcher);
    }
}
