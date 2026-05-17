package sa.edu.kau.fcit.cpit252.project.model;

/**
 * Represents a stock suggestion item loaded from CSV.
 */
public class StockSuggestion {
    private final String ticker;
    private final String name;
    private final String market;

    public StockSuggestion(String ticker, String name, String market) {
        this.ticker = ticker;
        this.name = name;
        this.market = market;
    }

    public String getTicker() {
        return ticker;
    }

    public String getName() {
        return name;
    }

    public String getMarket() {
        return market;
    }

    @Override
    public String toString() {
        return ticker + " - " + name;
    }
}
