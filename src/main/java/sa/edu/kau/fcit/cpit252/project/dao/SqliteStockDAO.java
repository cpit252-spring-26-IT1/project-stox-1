package sa.edu.kau.fcit.cpit252.project.dao;

import sa.edu.kau.fcit.cpit252.project.db.DatabaseConnection;
import sa.edu.kau.fcit.cpit252.project.model.Stock;

import java.sql.*;
import java.util.ArrayList;
import java.util.List;

/**
 * Concrete implementation of StockDAO for an embedded SQLite database.
 */
public class SqliteStockDAO implements StockDAO {

    public SqliteStockDAO() {
        createTableIfNotExists();
    }

    private void createTableIfNotExists() {
        String sql = "CREATE TABLE IF NOT EXISTS stocks (" +
                "id INTEGER PRIMARY KEY AUTOINCREMENT," +
                "ticker VARCHAR(10) NOT NULL," +
                "market VARCHAR(50)," +
                "quantity INTEGER NOT NULL," +
                "average_buy_price REAL NOT NULL," +
                "portfolio_name VARCHAR(100) NOT NULL DEFAULT 'Main Portfolio'," +
                "UNIQUE(ticker, portfolio_name)" +
                ")";

        Connection conn = DatabaseConnection.getConnection();
        try (Statement stmt = conn.createStatement()) {
            stmt.execute(sql);
        } catch (SQLException e) {
            System.err.println("Error creating stocks table:");
            e.printStackTrace();
        }
    }

    @Override
    public void addStock(Stock stock) {
        String portfolioName = (stock.getPortfolioName() != null && !stock.getPortfolioName().isBlank())
                ? stock.getPortfolioName()
                : "Main Portfolio";

        String sql = "INSERT INTO stocks (ticker, market, quantity, average_buy_price, portfolio_name) " +
                "VALUES (?, ?, ?, ?, ?) " +
                "ON CONFLICT (ticker, portfolio_name) " +
                "DO UPDATE SET " +
                "quantity = stocks.quantity + EXCLUDED.quantity, " +
                "average_buy_price = ((stocks.quantity * stocks.average_buy_price) + " +
                "(EXCLUDED.quantity * EXCLUDED.average_buy_price)) / (stocks.quantity + EXCLUDED.quantity)";

        Connection conn = DatabaseConnection.getConnection();
        try (PreparedStatement pstmt = conn.prepareStatement(sql)) {

            pstmt.setString(1, stock.getTicker().toUpperCase());
            pstmt.setString(2, stock.getMarket());
            pstmt.setInt(3, stock.getQuantity());
            pstmt.setDouble(4, stock.getAverageBuyPrice());
            pstmt.setString(5, portfolioName);

            pstmt.executeUpdate();
            System.out.println("Stock added/updated: " + stock.getTicker() + " in " + portfolioName);
        } catch (SQLException e) {
            System.err.println("Error adding stock:");
            e.printStackTrace();
        }
    }

    @Override
    public List<Stock> getPortfolio() {
        List<Stock> portfolio = new ArrayList<>();
        String sql = "SELECT ticker, market, quantity, average_buy_price, portfolio_name FROM stocks";

        Connection conn = DatabaseConnection.getConnection();
        try (Statement stmt = conn.createStatement();
             ResultSet rs = stmt.executeQuery(sql)) {

            while (rs.next()) {
                Stock stock = new Stock(
                        rs.getString("ticker"),
                        rs.getString("market"),
                        rs.getInt("quantity"),
                        rs.getDouble("average_buy_price"),
                        rs.getString("portfolio_name")
                );
                portfolio.add(stock);
            }
        } catch (SQLException e) {
            System.err.println("Error retrieving portfolio:");
            e.printStackTrace();
        }
        return portfolio;
    }

    @Override
    public List<Stock> getStocksByPortfolio(String portfolioName) {
        List<Stock> portfolio = new ArrayList<>();
        String sql = "SELECT ticker, market, quantity, average_buy_price, portfolio_name " +
                     "FROM stocks WHERE portfolio_name = ?";

        Connection conn = DatabaseConnection.getConnection();
        try (PreparedStatement pstmt = conn.prepareStatement(sql)) {
            pstmt.setString(1, portfolioName);
            ResultSet rs = pstmt.executeQuery();

            while (rs.next()) {
                Stock stock = new Stock(
                        rs.getString("ticker"),
                        rs.getString("market"),
                        rs.getInt("quantity"),
                        rs.getDouble("average_buy_price"),
                        rs.getString("portfolio_name")
                );
                portfolio.add(stock);
            }
        } catch (SQLException e) {
            System.err.println("Error retrieving portfolio by name:");
            e.printStackTrace();
        }
        return portfolio;
    }

    @Override
    public List<String> getAllPortfolioNames() {
        List<String> names = new ArrayList<>();
        String sql = "SELECT DISTINCT portfolio_name FROM stocks ORDER BY portfolio_name";

        Connection conn = DatabaseConnection.getConnection();
        try (Statement stmt = conn.createStatement();
             ResultSet rs = stmt.executeQuery(sql)) {

            while (rs.next()) {
                names.add(rs.getString("portfolio_name"));
            }
        } catch (SQLException e) {
            System.err.println("Error retrieving portfolio names:");
            e.printStackTrace();
        }
        return names;
    }

    @Override
    public void updateStock(Stock stock) {
        String sql = "UPDATE stocks SET market = ?, quantity = ?, average_buy_price = ? " +
                     "WHERE ticker = ? AND portfolio_name = ?";

        Connection conn = DatabaseConnection.getConnection();
        try (PreparedStatement pstmt = conn.prepareStatement(sql)) {

            pstmt.setString(1, stock.getMarket());
            pstmt.setInt(2, stock.getQuantity());
            pstmt.setDouble(3, stock.getAverageBuyPrice());
            pstmt.setString(4, stock.getTicker());
            pstmt.setString(5, stock.getPortfolioName());

            pstmt.executeUpdate();
            System.out.println("Stock updated: " + stock.getTicker());
        } catch (SQLException e) {
            System.err.println("Error updating stock:");
            e.printStackTrace();
        }
    }

    @Override
    public void removeStock(String ticker, String portfolioName) {
        String sql = "DELETE FROM stocks WHERE ticker = ? AND portfolio_name = ?";

        Connection conn = DatabaseConnection.getConnection();
        try (PreparedStatement pstmt = conn.prepareStatement(sql)) {

            pstmt.setString(1, ticker);
            pstmt.setString(2, portfolioName);

            pstmt.executeUpdate();
            System.out.println("Stock removed: " + ticker + " from " + portfolioName);
        } catch (SQLException e) {
            System.err.println("Error removing stock:");
            e.printStackTrace();
        }
    }
}
