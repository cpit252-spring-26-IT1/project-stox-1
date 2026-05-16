package sa.edu.kau.fcit.cpit252.project.api;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;

import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;

public class FinnhubFetcher implements PriceFetcher {

    // Environment variable for API key via EnvConfig
    private static final String API_KEY = EnvConfig.get("FINNHUB_API_KEY", "demo");
    private String baseUrl = "https://finnhub.io/api/v1/quote?symbol=%s&token=%s";

    private final HttpClient httpClient;
    private final ObjectMapper objectMapper;

    public FinnhubFetcher() {
        this.httpClient = HttpClient.newHttpClient();
        this.objectMapper = new ObjectMapper();
    }

    // For testing purposes
    void setBaseUrl(String baseUrl) {
        this.baseUrl = baseUrl;
    }

    @Override
    public double fetchPrice(String ticker) throws Exception {
        String url = String.format(baseUrl, ticker, API_KEY);

        HttpRequest request = HttpRequest.newBuilder()
                .uri(URI.create(url))
                .GET()
                .build();

        HttpResponse<String> response = httpClient.send(request, HttpResponse.BodyHandlers.ofString());

        if (response.statusCode() != 200) {
            throw new RuntimeException("Finnhub API returned status " + response.statusCode());
        }

        JsonNode root = objectMapper.readTree(response.body());

        if (!root.has("c") || root.get("c").isNull()) {
            throw new RuntimeException("Could not parse Finnhub response: " + response.body());
        }

        return root.get("c").asDouble();
    }
}
