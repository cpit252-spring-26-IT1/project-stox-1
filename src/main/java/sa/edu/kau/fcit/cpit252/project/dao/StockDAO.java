package sa.edu.kau.fcit.cpit252.project.dao;

import sa.edu.kau.fcit.cpit252.project.model.Stock;
import java.util.List;

/**
 * data access object interface for Stock operations.
 */
public interface StockDAO {

    /**
     * Add a new stock to the portfolio.
     */
    void addStock(Stock stock);

    /**
     * Retrieve all stocks in the portfolio.
     */
    List<Stock> getPortfolio();

    /**
     * Update an existing stock in the portfolio.
     */
    void updateStock(Stock stock);

    /**
     * Remove a stock from the portfolio by its ticker symbol.
     */
    void removeStock(String ticker);
}
