package sa.edu.kau.fcit.cpit252.project.api;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertInstanceOf;

class NetworkFetcherSmokeTest {

    @Test
    void finnhubFetcherCanBeConstructedAsPriceFetcher() {
        assertInstanceOf(PriceFetcher.class, new FinnhubFetcher());
    }

    @Test
    void sahmkFetcherCanBeConstructedAsPriceFetcher() {
        assertInstanceOf(PriceFetcher.class, new SahmkFetcher());
    }
}
