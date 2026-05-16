package sa.edu.kau.fcit.cpit252.project.api;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;

import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;

public class SahmkFetcher implements PriceFetcher {

    // Environment variable for API key via EnvConfig
    private static final String API_KEY = EnvConfig.get("SAHMK_API_KEY", "YOUR_API_KEY_HERE");
    private String baseUrl = "https://app.sahmk.sa/api/v1/quote/%s/";

    private final HttpClient httpClient;
    private final ObjectMapper objectMapper;

    public SahmkFetcher() {
        this.httpClient = HttpClient.newHttpClient();
        this.objectMapper = new ObjectMapper();
    }

    void setBaseUrl(String baseUrl) {
        this.baseUrl = baseUrl;
    }

    @Override
    public double fetchPrice(String ticker) throws Exception {
        String url = String.format(baseUrl, ticker);

        HttpRequest request = HttpRequest.newBuilder()
                .uri(URI.create(url))
                .header("X-API-Key", API_KEY)
                .GET()
                .build();

        HttpResponse<String> response = httpClient.send(request, HttpResponse.BodyHandlers.ofString());

        if (response.statusCode() != 200) {
            throw new RuntimeException("Sahmk API returned status " + response.statusCode());
        }

        JsonNode root = objectMapper.readTree(response.body());

        if (!root.has("price")) {
            throw new RuntimeException("Could not parse Sahmk response: " + response.body());
        }

        return root.get("price").asDouble();
    }
}
