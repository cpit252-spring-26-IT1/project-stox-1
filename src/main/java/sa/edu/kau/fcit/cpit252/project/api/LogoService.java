package sa.edu.kau.fcit.cpit252.project.api;

import javafx.scene.image.Image;
import javafx.application.Platform;
import java.io.InputStream;
import java.net.HttpURLConnection;
import java.net.URL;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.CompletableFuture;
import java.util.function.Consumer;

/**
 * Service to fetch and cache stock brand logos.
 * Mimics a browser to bypass 403 Forbidden restrictions.
 */
public class LogoService {
    private static final String USER_AGENT = "Mozilla/5.0 (Windows NT 10.0; Win64; x64) AppleWebKit/537.36 (KHTML, like Gecko) Chrome/119.0.0.0 Safari/537.36";
    private static final ConcurrentHashMap<String, Image> logoCache = new ConcurrentHashMap<>();

    /**
     * Loads a logo asynchronously and provides it via a callback.
     * 
     * @param ticker   The stock ticker symbol.
     * @param market   The market description (used to determine URL).
     * @param onLoaded Callback invoked on the JavaFX Application Thread when the logo is loaded.
     */
    public static void loadLogoAsync(String ticker, String market, Consumer<Image> onLoaded) {
        if (ticker == null || ticker.isBlank()) return;

        String cacheKey = market + ":" + ticker;
        if (logoCache.containsKey(cacheKey)) {
            onLoaded.accept(logoCache.get(cacheKey));
            return;
        }

        CompletableFuture.runAsync(() -> {
            try {
                String urlString;
                // Determine URL based on market
                if (market != null && market.contains("Tadawul")) {
                    urlString = "https://www.tadawulgroup.sa/Resources/SEMOBILELOGOS/" + ticker + ".png";
                } else {
                    urlString = "https://eodhd.com/img/logos/US/" + ticker + ".png";
                }

                URL url = new URL(urlString);
                HttpURLConnection connection = (HttpURLConnection) url.openConnection();
                connection.setRequestProperty("User-Agent", USER_AGENT);
                connection.setConnectTimeout(5000);
                connection.setReadTimeout(5000);

                int responseCode = connection.getResponseCode();
                if (responseCode == 200) {
                    try (InputStream is = connection.getInputStream()) {
                        Image image = new Image(is, 24, 24, true, true);
                        if (!image.isError()) {
                            logoCache.put(cacheKey, image);
                            Platform.runLater(() -> onLoaded.accept(image));
                        }
                    }
                }
            } catch (Exception e) {
                // Log failure for debugging, but don't disrupt the UI
                System.err.println("Failed to load logo for " + ticker + ": " + e.getMessage());
            }
        });
    }
}
