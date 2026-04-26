package sa.edu.kau.fcit.cpit252.project;

import java.util.List;

import javafx.application.Application;
import javafx.scene.Scene;
import javafx.scene.control.Label;
import javafx.scene.layout.StackPane;
import javafx.stage.Stage;
import sa.edu.kau.fcit.cpit252.project.dao.SqliteStockDAO;
import sa.edu.kau.fcit.cpit252.project.db.DatabaseConnection;
import sa.edu.kau.fcit.cpit252.project.model.Portfolio;
import sa.edu.kau.fcit.cpit252.project.model.Stock;

/**
 * Main JavaFX Application
 */
public class App extends Application {

    @Override
    public void start(Stage stage) {
        // Test database connection
        System.out.println("Testing Database Connection on Startup...");
        DatabaseConnection.getConnection();

        // 2. Create the DAO object.
        // The DAO is responsible for saving and reading Stock objects from SQLite.
        SqliteStockDAO dao = new SqliteStockDAO();

        // 3. Create sample Stock objects.
        // Stock represents the Leaf in the Composite Pattern.
        Stock apple = new Stock("AAPL", "US", 10, 175.50);
        Stock aramco = new Stock("2222", "Tadawul", 50, 30.20);
        Stock nvidia = new Stock("NVDA", "US", 5, 800.00);

        // 4. Save sample stocks into the database.
        dao.addStock(apple);
        dao.addStock(aramco);
        dao.addStock(nvidia);

        // 5. Read all stocks from the database.
        List<Stock> myPortfolio = dao.getPortfolio();

        // 6. Create Portfolio objects.
        // Portfolio represents the Composite in the Composite Pattern.
        Portfolio mainPortfolio = new Portfolio("Main Portfolio");
        Portfolio usPortfolio = new Portfolio("US Market Portfolio");
        Portfolio tadawulPortfolio = new Portfolio("Tadawul Portfolio");

        // 7. Build the Composite tree.
        // If the stock is from the US market, add it to the US portfolio.
        // Otherwise, add it to the Tadawul portfolio.
        for (Stock stock : myPortfolio) {
            if (stock.getMarket().equalsIgnoreCase("US")) {
                usPortfolio.add(stock);
            } else {
                tadawulPortfolio.add(stock);
            }
        }

        // 8. Add the sub-portfolios inside the main portfolio.
        // Now the structure is:
        //
        // Main Portfolio
        //   US Market Portfolio
        //     AAPL
        //     NVDA
        //   Tadawul Portfolio
        //     2222
        mainPortfolio.add(usPortfolio);
        mainPortfolio.add(tadawulPortfolio);

        // 9. Print the portfolio hierarchy in the console.
        // This proves that the Composite Pattern is working.
        System.out.println("\n--- Composite Portfolio Structure ---");
        mainPortfolio.display("");
        System.out.println("-------------------------------------");

        // 10. Calculate the total value using the same getValue() call.
        // This works because both Stock and Portfolio implement the same interface.
        double totalValue = mainPortfolio.getValue();

        // 11. Show a simple JavaFX screen.
        Label label = new Label(
                "Welcome to Stox! \nPortfolio size: " + myPortfolio.size() +"\n"+
                   "Total Portfolio value" + totalValue);
        Scene scene = new Scene(new StackPane(label), 640, 480);

        stage.setTitle("Stox Application");
        stage.setScene(scene);
        stage.show();
    }

    public static void main(String[] args) {
        launch();
    }
}