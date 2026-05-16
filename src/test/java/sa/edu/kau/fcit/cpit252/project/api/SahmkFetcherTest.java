package sa.edu.kau.fcit.cpit252.project.api;

import com.sun.net.httpserver.HttpServer;
import org.junit.jupiter.api.AfterAll;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;

import java.io.OutputStream;
import java.net.InetSocketAddress;

import static org.junit.jupiter.api.Assertions.*;

class SahmkFetcherTest {
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
        String path = "/sahmk";
        server.createContext(path, exchange -> {
            String response = "{\"price\": 35.50}";
            exchange.sendResponseHeaders(200, response.length());
            try (OutputStream os = exchange.getResponseBody()) {
                os.write(response.getBytes());
            }
        });

        SahmkFetcher fetcher = new SahmkFetcher();
        fetcher.setBaseUrl("http://localhost:" + port + path + "/%s/");
        
        double price = fetcher.fetchPrice("2222");
        assertEquals(35.50, price);
        server.removeContext(path);
    }

    @Test
    void fetchPriceThrowsExceptionOnNon200() throws Exception {
        String path = "/sahmk-error";
        server.createContext(path, exchange -> {
            exchange.sendResponseHeaders(401, 0);
            exchange.close();
        });

        SahmkFetcher fetcher = new SahmkFetcher();
        fetcher.setBaseUrl("http://localhost:" + port + path + "/%s/");
        
        assertThrows(RuntimeException.class, () -> fetcher.fetchPrice("2222"));
        server.removeContext(path);
    }

    @Test
    void fetchPriceThrowsExceptionOnMissingField() throws Exception {
        String path = "/sahmk-missing";
        server.createContext(path, exchange -> {
            String response = "{\"status\": \"ok\"}";
            exchange.sendResponseHeaders(200, response.length());
            try (OutputStream os = exchange.getResponseBody()) {
                os.write(response.getBytes());
            }
        });

        SahmkFetcher fetcher = new SahmkFetcher();
        fetcher.setBaseUrl("http://localhost:" + port + path + "/%s/");
        
        assertThrows(RuntimeException.class, () -> fetcher.fetchPrice("2222"));
        server.removeContext(path);
    }
}
