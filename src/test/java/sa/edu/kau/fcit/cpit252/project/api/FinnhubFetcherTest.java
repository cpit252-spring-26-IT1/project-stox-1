package sa.edu.kau.fcit.cpit252.project.api;

import com.sun.net.httpserver.HttpServer;
import org.junit.jupiter.api.AfterAll;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;

import java.io.OutputStream;
import java.net.InetSocketAddress;

import static org.junit.jupiter.api.Assertions.*;

class FinnhubFetcherTest {
    private static HttpServer server;
    private static int port;

    @BeforeAll
    static void startServer() throws Exception {
        server = HttpServer.create(new InetSocketAddress(0), 0);
        port = server.getAddress().getPort();
        server.start();
    }

    @AfterAll
    static void stopServer() {
        server.stop(0);
    }

    @Test
    void fetchPriceReturnsCorrectValue() throws Exception {
        String path = "/quote";
        server.createContext(path, exchange -> {
            String response = "{\"c\": 150.25}";
            exchange.sendResponseHeaders(200, response.length());
            try (OutputStream os = exchange.getResponseBody()) {
                os.write(response.getBytes());
            }
        });

        FinnhubFetcher fetcher = new FinnhubFetcher();
        fetcher.setBaseUrl("http://localhost:" + port + path + "?symbol=%s&token=%s");
        
        double price = fetcher.fetchPrice("AAPL");
        assertEquals(150.25, price);
        server.removeContext(path);
    }

    @Test
    void fetchPriceThrowsExceptionOnNon200() throws Exception {
        String path = "/error";
        server.createContext(path, exchange -> {
            exchange.sendResponseHeaders(500, 0);
            exchange.close();
        });

        FinnhubFetcher fetcher = new FinnhubFetcher();
        fetcher.setBaseUrl("http://localhost:" + port + path + "?symbol=%s&token=%s");
        
        assertThrows(RuntimeException.class, () -> fetcher.fetchPrice("AAPL"));
        server.removeContext(path);
    }

    @Test
    void fetchPriceThrowsExceptionOnMissingField() throws Exception {
        String path = "/missing";
        server.createContext(path, exchange -> {
            String response = "{\"wrong\": 100}";
            exchange.sendResponseHeaders(200, response.length());
            try (OutputStream os = exchange.getResponseBody()) {
                os.write(response.getBytes());
            }
        });

        FinnhubFetcher fetcher = new FinnhubFetcher();
        fetcher.setBaseUrl("http://localhost:" + port + path + "?symbol=%s&token=%s");
        
        assertThrows(RuntimeException.class, () -> fetcher.fetchPrice("AAPL"));
        server.removeContext(path);
    }
}
