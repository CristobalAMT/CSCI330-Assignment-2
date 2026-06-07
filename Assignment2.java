/* 
Programmer: Cristobal Miranda
This program is intended to connect to an SQL database containing millions of data points on several companies.
You can input a ticker symbol and, optionally, a start/end date into the console.
Your output to the console will contain every time that stock had a split and how much money you'd get by using a hard-coded
investment strategy for the particular stock throughout the given duration or the entire timeframe.
*/

import java.io.FileInputStream;
import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.*;

class Assign2 {
   
   static class StockData {	   
	   // To Do: 
	   // Create this class which should contain the information  (date, open price, high price, low price, close price) for a particular ticker
      String date;
      double openPrice;
      double highPrice;
      double lowPrice;
      double closePrice;

      // test method to ensure price adjustment worked
      // prints out all five instance variables of StockData
      void print() {
         System.out.println(date + " - open: " + openPrice + " | high: " + highPrice + " | low: " + lowPrice + " | close: " + closePrice);
      }

   }
   
   static Connection conn;
   static final String prompt = "Enter ticker symbol [start/end dates]: ";
   
   public static void main(String[] args) throws Exception {
	  // String paramsFile = "connectionparams/ConnectionParameters_LabComputer.txt";
	  String paramsFile = "connectionparams/ConnectionParameters_RemoteComputer.txt";

      if (args.length >= 1) {
         paramsFile = args[0];
      }
      
      Properties connectprops = new Properties();
      connectprops.load(new FileInputStream(paramsFile));
      // step 1: connect to the database
      try {
         Class.forName("com.mysql.cj.jdbc.Driver");
         String dburl = connectprops.getProperty("dburl");
         String username = connectprops.getProperty("user");
         conn = DriverManager.getConnection(dburl, connectprops);
         System.out.println("Database connection is established");
         
         Scanner in = new Scanner(System.in);
         System.out.print(prompt);
         // Step 2.1: request ticker symbol and start/end dates from System.in
         String input = in.nextLine().trim();
         
         while (input.length() > 0) {
            String[] params = input.split("\\s+");
            String ticker = params[0];
            String startdate = null, enddate = null;
            // Step 2.2: Get full company name from table and print to console.
            if (getName(ticker)) {
               if (params.length >= 3) {
                  startdate = params[1];
                  enddate = params[2];
               }               
               // Step 2.3-2.5: Get pricevolume data and adjust for splits
               Deque<StockData> data = getStockData(ticker, startdate, enddate);
               System.out.println();
               System.out.println("Executing investment strategy");
               // Step 2.6-2.10: execute investment strategy 
               doStrategy(ticker, data);
            } 
            
            System.out.println();
            System.out.print(prompt);
            input = in.nextLine().trim();
         }

         // Close the database connection
         conn.close();

      } catch (SQLException ex) {
         System.out.printf("SQLException: %s%nSQLState: %s%nVendorError: %s%n",
                           ex.getMessage(), ex.getSQLState(), ex.getErrorCode());
      }
   }
   
   // gets the name of the company using the given ticker from the company table, prints it, and returns true.
   // if the given ticker doesn't correspond to a company, it returns false.
   // if true, the main loop begins obtaining data for the company and executes the investment strategy.
   // if false, the main loop simply loops again.
   static boolean getName(String ticker) throws SQLException {
      // create preparedStatement 
      PreparedStatement nameStatement = conn.prepareStatement(
      "select Name from company where Ticker = ?");
      nameStatement.setString(1, ticker);
      ResultSet name = nameStatement.executeQuery(); // results from executing statement
      // if we got a name, then print the company name corresponding to the given ticker and return true.
      if(name.next()) {
         System.out.println(name.getString(1));
         return true;
      }
      // otherwise, say we didn't get a result and return false.
      System.out.println("Ticker '" + ticker + "' not found in database.");
      return false;
   }

   // returns a deque object containing the stock data (trading date, opening price, high price, low price, and closing price)
      // of every single trading day in a given time frame (if a time frame is given). 
      // stock prices are adjusted for splits. The method also prints when splits occur to console.
      // The deque is ordered from earliest trading day to the latest trading day.
   static Deque<StockData> getStockData(String ticker, String start, String end) throws SQLException{	  
      ResultSet stockInfo = null;
      // create and execute preparedStatement when a date range is given
      if(start != null && end != null) {
         PreparedStatement stockStatement = conn.prepareStatement(
            "select * from pricevolume" +
            " where Ticker = ? AND" +
            " TransDate BETWEEN ? AND ?" +
            " order by TransDate DESC"
         );
         stockStatement.setString(1, ticker);
         stockStatement.setString(2, start);
         stockStatement.setString(3, end);
         stockInfo = stockStatement.executeQuery();
      }
      // create and execute preparedStatement for no date range
      else {
         PreparedStatement stockStatement = conn.prepareStatement(
            "select * from pricevolume" +
            " where Ticker = ?" +
            " order by TransDate DESC"
         );
         stockStatement.setString(1, ticker);
         stockInfo = stockStatement.executeQuery();
      }

      Deque<StockData> result = new ArrayDeque<>();
      
      // totalDivisor normalizes the stock data by adjusting for stock splits
      double totalDivisor = 1;
      stockInfo.next();

      // Add first stock to deque
      StockData firstStock = new StockData();
      firstStock.date = stockInfo.getString(2).trim();
      firstStock.openPrice = Double.parseDouble(stockInfo.getString(3));
      firstStock.highPrice = Double.parseDouble(stockInfo.getString(4).trim());
      firstStock.lowPrice = Double.parseDouble(stockInfo.getString(5).trim());
      firstStock.closePrice = Double.parseDouble(stockInfo.getString(6).trim());
      result.add(firstStock);

      // while there is more data, add stock to deque while adjusting for splits
      // step 2.4/2.5
      while(stockInfo.next()) {
         // check for stock splits 

         double currClose = Double.parseDouble(stockInfo.getString(6).trim());
         double nextOpen = result.getFirst().openPrice * totalDivisor;
         String currDate = stockInfo.getString(2).trim();
         // in case of 2:1 stock split
         if(Math.abs((currClose/nextOpen) - 2) < 0.2) {
            totalDivisor = totalDivisor*2;
            System.out.println("2:1 split on " + currDate + " " + currClose + " --> " + nextOpen + " | totalDivisor: " + totalDivisor);
         }
         // in case of 3:1 stock split
         else if(Math.abs((currClose/nextOpen) - 3) < 0.3) {
            totalDivisor = totalDivisor*3;
            System.out.println("3:1 split on " + currDate + " " + currClose + " --> " + nextOpen + " | totalDivisor: " + totalDivisor);
         }
         // in case of 3:2 stock split
         else if(Math.abs((currClose/nextOpen) - 1.5) < 0.15) {
            totalDivisor = totalDivisor*1.5;
            System.out.println("3:2 split on " + currDate + " " + currClose + " --> " + nextOpen + " | totalDivisor: " + totalDivisor);
         }
         
         // add current stock data to deque
         StockData currStock = new StockData();
         currStock.date = currDate;
         currStock.openPrice = Double.parseDouble(stockInfo.getString(3).trim()) / totalDivisor;
         currStock.highPrice = Double.parseDouble(stockInfo.getString(4).trim()) / totalDivisor;
         currStock.lowPrice = Double.parseDouble(stockInfo.getString(5).trim()) / totalDivisor;
         currStock.closePrice = currClose / totalDivisor;
         // currStock.print(); // for testing purposes
         result.addFirst(currStock);
      }
      
      return result;
   }
   
   static void doStrategy(String ticker, Deque<StockData> data) {
	  //To Do: 
	  // Apply Steps 2.6 to 2.10 explained in the assignment description 
	  // data (which is a Deque) has all the information (after the split adjustment) you need to apply these steps

     // deque for moving average of closing prices
     Deque<Double> maDeque = new ArrayDeque<>();
     double maTotal = 0;
     
     double currentCash = 0;
     double currentShares = 0;
     boolean enoughData = false;
     for(StockData dayData : data) {
      // 2.7
      // check for exactly 50 entries in maDeque BEFORE adjusting ma 
      if(maDeque.size() == 50) {
         double ma = maTotal / 50.0;
         enoughData = true; // flip enoughData to true to show that we traded

      }

      // add new closing price to maDeque. 
      maDeque.addLast(maTotal);
      maTotal += dayData.closePrice;

      // if adding sets it to over 50, pop first element and subtract from maTotal
      if(maDeque.size() > 50) {
         maTotal -= maDeque.pop();
      }
     }
     // 2.8 
     // if there's less than 51 days of data, do no trading and print a gain of 0.
     if(!enoughData) {

     }
   }
}