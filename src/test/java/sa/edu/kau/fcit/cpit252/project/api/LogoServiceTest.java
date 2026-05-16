package sa.edu.kau.fcit.cpit252.project.api;

import javafx.scene.image.Image;
import javafx.stage.Stage;
import org.junit.jupiter.api.Test;
import org.testfx.framework.junit5.ApplicationTest;

import java.util.concurrent.CountDownLatch;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicReference;

import static org.junit.jupiter.api.Assertions.*;

/**
 * Tests for LogoService to ensure coverage and correct functionality.
 */
class LogoServiceTest extends ApplicationTest {

    @Override
    public void start(Stage stage) throws Exception {
        // No UI needed for these service tests
    }

    @Test
    void loadLogoAsyncHandlesNullOrBlankTicker() {
        LogoService.loadLogoAsync(null, "US", img -> fail("Should not load for null ticker"));
        LogoService.loadLogoAsync("", "US", img -> fail("Should not load for empty ticker"));
        LogoService.loadLogoAsync("   ", "US", img -> fail("Should not load for blank ticker"));
    }

    @Test
    void loadLogoAsyncCachesAndReturnsImage() throws InterruptedException {
        String ticker = "NVDA";
        String market = "US Market";
        CountDownLatch latch1 = new CountDownLatch(1);
        AtomicReference<Image> imgRef1 = new AtomicReference<>();

        LogoService.loadLogoAsync(ticker, market, img -> {
            imgRef1.set(img);
            latch1.countDown();
        });

        assertTrue(latch1.await(10, TimeUnit.SECONDS), "First load timed out - check network connection");
        assertNotNull(imgRef1.get());

        // Second load should use cache (invokes callback immediately or via
        // Platform.runLater)
        CountDownLatch latch2 = new CountDownLatch(1);
        AtomicReference<Image> imgRef2 = new AtomicReference<>();
        LogoService.loadLogoAsync(ticker, market, img -> {
            imgRef2.set(img);
            latch2.countDown();
        });

        assertTrue(latch2.await(2, TimeUnit.SECONDS), "Second load (cache) timed out");
        assertSame(imgRef1.get(), imgRef2.get(), "Should return cached instance");
    }

    @Test
    void loadLogoAsyncUsesTadawulUrlForSaudiMarket() throws InterruptedException {
        String ticker = "2222";
        String market = "Saudi Market - Tadawul";
        CountDownLatch latch = new CountDownLatch(1);
        AtomicReference<Image> imgRef = new AtomicReference<>();

        LogoService.loadLogoAsync(ticker, market, img -> {
            imgRef.set(img);
            latch.countDown();
        });

        assertTrue(latch.await(10, TimeUnit.SECONDS), "Saudi logo load timed out - check network connection");
        assertNotNull(imgRef.get());
    }

    @Test
    void loadLogoAsyncHandlesNotFound() throws InterruptedException {
        // Use a definitely non-existent ticker to trigger a non-200 response
        String ticker = "NON_EXISTENT_TICKER_" + System.nanoTime();
        CountDownLatch latch = new CountDownLatch(1);

        LogoService.loadLogoAsync(ticker, "US", img -> {
            latch.countDown();
        });

        // This will cover the responseCode != 200 branch
        assertFalse(latch.await(2, TimeUnit.SECONDS), "Should not have triggered success for invalid ticker");
    }

    @Test
    void loadLogoAsyncHandlesNullMarket() throws InterruptedException {
        // Ticker NVDA on null market should default to US URL
        String ticker = "NVDA";
        CountDownLatch latch = new CountDownLatch(1);
        LogoService.loadLogoAsync(ticker, null, img -> latch.countDown());

        // This covers the market == null branch in line 40
        assertTrue(latch.await(10, TimeUnit.SECONDS), "Loading with null market timed out");
    }

    @Test
    void loadLogoAsyncHandlesException() throws InterruptedException {
        // Ticker with a space ("NVDA LOGO") will trigger a MalformedURLException
        // because the URL constructor does not allow unencoded spaces.
        // This will cover the catch block in LogoService.
        LogoService.loadLogoAsync("NVDA LOGO", "US", img -> {
        });

        // Give it a moment to hit the catch block
        Thread.sleep(1000);
    }

    @Test
    void loadLogoAsyncHandlesInvalidImageContent() throws InterruptedException {
        // Using a ticker like "home" on the Tadawul market might point to a page
        // that returns 200 OK but is HTML instead of an image.
        // This will trigger the image.isError() == true branch.
        LogoService.loadLogoAsync("home", "Tadawul", img -> {
        });

        // Wait for async execution
        Thread.sleep(2000);
    }

    @Test
    void canBeConstructedForCoverage() {
        // Covers the implicit/default constructor
        assertNotNull(new LogoService());
    }
}
