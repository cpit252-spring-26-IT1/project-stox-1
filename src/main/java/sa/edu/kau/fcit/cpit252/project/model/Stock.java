package sa.edu.kau.fcit.cpit252.project.model;

/**
 * Model class representing a Stock.
 */
public class Stock {
    private String ticker;
    private String market;
    private int quantity;
    private double averageBuyPrice;

    public Stock() {}

    public Stock(String ticker, String market, int quantity, double averageBuyPrice) {
        this.ticker = ticker;
        this.market = market;
        this.quantity = quantity;
        this.averageBuyPrice = averageBuyPrice;
    }

    public String getTicker() {
        return ticker;
    }

    public void setTicker(String ticker) {
        this.ticker = ticker;
    }

    public String getMarket() {
        return market;
    }

    public void setMarket(String market) {
        this.market = market;
    }

    public int getQuantity() {
        return quantity;
    }

    public void setQuantity(int quantity) {
        this.quantity = quantity;
    }

    public double getAverageBuyPrice() {
        return averageBuyPrice;
    }

    public void setAverageBuyPrice(double averageBuyPrice) {
        this.averageBuyPrice = averageBuyPrice;
    }

    @Override
    public String toString() {
        return "Stock{" +
                "ticker='" + ticker + '\'' +
                ", market='" + market + '\'' +
                ", quantity=" + quantity +
                ", averageBuyPrice=" + averageBuyPrice +
                '}';
    }
}
