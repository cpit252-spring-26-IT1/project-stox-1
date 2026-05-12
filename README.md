# stoX: Stocks Portfolio Manager
<p align="center">
  <img src="assets/logo.png" alt="stoX Logo" />
</p>

## Description
A desktop application designed to solve the fragmentation of asset tracking for modern investors.
By unifying live market data from both the US market and the Saudi Exchange (Tadawul), stoX provides a
single, cohesive dashboard to track holdings, organize portfolios hierarchically, and view real-time
lifetime profit/loss. 



## Features

### ✅ Implemented Features (Completed)
* **Project & Database Setup:** Core project structure initialized with embedded SQLite database integration and SQL queries for local data persistence.
* **Core Architecture:** Implementation of the core application logic, including the integration of Creational Design Patterns.
* **Composite Design Pattern:** The system uniformly calculates the value of individual stocks and entire grouped portfolios using the Composite structural pattern.
* **Interactive Dashboard:** A custom-styled JavaFX UI form allowing users to dynamically add, view, and remove stock assets.
* **API Market Integration:** Connecting to live market APIs (e.g., Finnhub/Sahmk) to fetch real-time stock prices.
* **API Data Caching & Optimization:** Building a caching mechanism and timeout handler has been deferred. Because live API fetches are not yet active in this stage, caching is currently out of scope and will be implemented in a future release.


### ⏳ Planned Features (Backlog)
* **Profit/Loss Tracking:** Automated calculation and display of portfolio performance over time.
* **Unit test implementation**

### 🛑 Deferred Features (Won't Implement / Future)
* **UI Improvements:**
  * Replace the application interface font for eye appealing reading.
  * Implement interactive charts and graphs to visualize portfolio performance.


## 🛠️ Prerequisites
### To run this project, you must have the following installed:
* **Java 17 or 21** (JDK)
* **Apache Maven**

### API **keys** from:
* [Finnhub](https://finnhub.io/) API for US market.
* [Sahmk](https://www.sahmk.sa/developers) API for Saudi Tadawul market.

## Usage

### To build and run the app, use:

- **Option 1: Run directly via Maven**
  - Open the terminal run the following command:
```bash
mvn clean javafx:run
```

- **Option 2: Build and run the executable Fat JAR**
  - Open the terminal run the following command:
```bash
mvn clean package
```

```bash
cd target
java -jar course-project-1.0-SNAPSHOT.jar
```

### Create a `.env` file, and place the project source code or the compiled `.jar` and the `.env` file in the same root folder:

Example:
```bash
my_folder/
 ├── stoX.jar
 └── .env
```

`.env` file format:
```bash
FINNHUB_API_KEY={place your Finnhub API key here}
SAHMK_API_KEY={place your Sahmk API key here}
```
## Screenshots
![stoX UI Dashboard](assets/ui-screenshot.png)
![Add Stock Submenu](assets/ui-screenshot2.png)

## License
This project is licensed under the MIT License. See the [LICENSE](LICENSE) file for details.
