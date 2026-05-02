package sa.edu.kau.fcit.cpit252.project.model;

/**
 * Model class representing a Stock.
 */
public class Stock implements PortfolioComponent {
    private String ticker;
    private String market;
    private int quantity;
    private double averageBuyPrice;
    private String portfolioName;

    public Stock() {}

    public Stock(String ticker, String market, int quantity, double averageBuyPrice) {
        this.ticker = ticker;
        this.market = market;
        this.quantity = quantity;
        this.averageBuyPrice = averageBuyPrice;
        this.portfolioName = "Main Portfolio";
    }

    public Stock(String ticker, String market, int quantity, double averageBuyPrice, String portfolioName) {
        this.ticker = ticker;
        this.market = market;
        this.quantity = quantity;
        this.averageBuyPrice = averageBuyPrice;
        this.portfolioName = portfolioName;
    }

    public String getTicker() { return ticker; }
    public void setTicker(String ticker) { this.ticker = ticker; }

    public String getMarket() { return market; }
    public void setMarket(String market) { this.market = market; }

    public int getQuantity() { return quantity; }
    public void setQuantity(int quantity) { this.quantity = quantity; }

    public double getAverageBuyPrice() { return averageBuyPrice; }
    public void setAverageBuyPrice(double averageBuyPrice) { this.averageBuyPrice = averageBuyPrice; }

    public String getPortfolioName() { return portfolioName; }
    public void setPortfolioName(String portfolioName) { this.portfolioName = portfolioName; }

    @Override
    public String getName() { return ticker; }

    @Override
    public double getValue() { return quantity * averageBuyPrice; }

    @Override
    public void display(String indent) {
        System.out.println(indent + "Stock: " + ticker + " | value: " + getValue());
    }

    @Override
    public String toString() {
        return "Stock{ticker='" + ticker + "', market='" + market + "', qty=" + quantity +
               ", avgBuy=" + averageBuyPrice + ", portfolio='" + portfolioName + "'}";
    }
}
