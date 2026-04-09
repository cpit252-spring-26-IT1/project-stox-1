package sa.edu.kau.fcit.cpit252.project.dao;

import sa.edu.kau.fcit.cpit252.project.db.DatabaseConnection;
import sa.edu.kau.fcit.cpit252.project.model.Stock;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;
import java.util.ArrayList;
import java.util.List;

/**
 * Concrete implementation of StockDAO for PostgreSQL.
 */
public class PostgresStockDAO implements StockDAO {

    public PostgresStockDAO() {
        createTableIfNotExists();
    }

    private void createTableIfNotExists() {
        String sql = "CREATE TABLE IF NOT EXISTS stocks (" +
                     "id SERIAL PRIMARY KEY," +
                     "ticker VARCHAR(10) UNIQUE NOT NULL," +
                     "market VARCHAR(50)," +
                     "quantity INTEGER NOT NULL," +
                     "average_buy_price NUMERIC(10, 2) NOT NULL" +
                     ")";
                     
        try (Connection conn = DatabaseConnection.getConnection();
             Statement stmt = conn.createStatement()) {
            stmt.execute(sql);
        } catch (SQLException e) {
            System.err.println("Error creating stocks table:");
            e.printStackTrace();
        }
    }

    @Override
    public void addStock(Stock stock) {
        String sql = "INSERT INTO stocks (ticker, market, quantity, average_buy_price) " +
                     "VALUES (?, ?, ?, ?) " +
                     "ON CONFLICT (ticker) " +
                     "DO UPDATE SET " +
                     "quantity = stocks.quantity + EXCLUDED.quantity, " +
                     "average_buy_price = ((stocks.quantity * stocks.average_buy_price) + (EXCLUDED.quantity * EXCLUDED.average_buy_price)) / (stocks.quantity + EXCLUDED.quantity)";
                     
        try (Connection conn = DatabaseConnection.getConnection();
             PreparedStatement pstmt = conn.prepareStatement(sql)) {
            
            pstmt.setString(1, stock.getTicker());
            pstmt.setString(2, stock.getMarket());
            pstmt.setInt(3, stock.getQuantity());
            pstmt.setDouble(4, stock.getAverageBuyPrice());
            
            pstmt.executeUpdate();
            System.out.println("Stock added successfully: " + stock.getTicker());
        } catch (SQLException e) {
            System.err.println("Error adding stock:");
            e.printStackTrace();
        }
    }

    @Override
    public List<Stock> getPortfolio() {
        List<Stock> portfolio = new ArrayList<>();
        String sql = "SELECT ticker, market, quantity, average_buy_price FROM stocks";
        
        try (Connection conn = DatabaseConnection.getConnection();
             Statement stmt = conn.createStatement();
             ResultSet rs = stmt.executeQuery(sql)) {
            
            while (rs.next()) {
                Stock stock = new Stock(
                        rs.getString("ticker"),
                        rs.getString("market"),
                        rs.getInt("quantity"),
                        rs.getDouble("average_buy_price")
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
    public void updateStock(Stock stock) {
        String sql = "UPDATE stocks SET market = ?, quantity = ?, average_buy_price = ? WHERE ticker = ?";
        
        try (Connection conn = DatabaseConnection.getConnection();
             PreparedStatement pstmt = conn.prepareStatement(sql)) {
            
            pstmt.setString(1, stock.getMarket());
            pstmt.setInt(2, stock.getQuantity());
            pstmt.setDouble(3, stock.getAverageBuyPrice());
            pstmt.setString(4, stock.getTicker());
            
            pstmt.executeUpdate();
            System.out.println("Stock updated successfully: " + stock.getTicker());
        } catch (SQLException e) {
            System.err.println("Error updating stock:");
            e.printStackTrace();
        }
    }

    @Override
    public void removeStock(String ticker) {
        String sql = "DELETE FROM stocks WHERE ticker = ?";
        
        try (Connection conn = DatabaseConnection.getConnection();
             PreparedStatement pstmt = conn.prepareStatement(sql)) {
            
            pstmt.setString(1, ticker);
            pstmt.executeUpdate();
            System.out.println("Stock removed successfully: " + ticker);
        } catch (SQLException e) {
            System.err.println("Error removing stock:");
            e.printStackTrace();
        }
    }
}
