package sa.edu.kau.fcit.cpit252.project.dao;

import sa.edu.kau.fcit.cpit252.project.model.Stock;
import java.util.List;

/**
 * data access object interface for Stock operations.
 */
public interface StockDAO {

    /** Add a new stock to the portfolio. */
    void addStock(Stock stock);

    /** Retrieve all stocks in the portfolio. */
    List<Stock> getPortfolio();

    /** Retrieve stocks filtered by portfolio name. */
    List<Stock> getStocksByPortfolio(String portfolioName);

    /** Update an existing stock in the portfolio. */
    void updateStock(Stock stock);

    /** Remove a stock by ticker AND portfolio name to avoid cross-portfolio deletions. */
    void removeStock(String ticker, String portfolioName);

    /** Get all distinct portfolio names. */
    List<String> getAllPortfolioNames();
}
