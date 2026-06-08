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
import java.lang.Math;

class Assign2 {
   
   // StockData is a class used for holding stock information. 
   // Used in getStockData and doStrategy
   static class StockData {	   
	   String date;
      double openPrice;
      double highPrice;
      double lowPrice;
      double closePrice;
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
         System.out.println("Database connection closed.");

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
      // initialize resultset
      ResultSet stockInfo = null;
      // 2.3 
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
      // This is so while loop doesn't need if statements for edge cases for getting data from the first entry
      StockData firstStock = new StockData();
      firstStock.date = stockInfo.getString(2).trim();
      firstStock.openPrice = Double.parseDouble(stockInfo.getString(3));
      firstStock.highPrice = Double.parseDouble(stockInfo.getString(4).trim());
      firstStock.lowPrice = Double.parseDouble(stockInfo.getString(5).trim());
      firstStock.closePrice = Double.parseDouble(stockInfo.getString(6).trim());
      result.add(firstStock);

      int numSplits = 0;

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
            numSplits++;
         }
         // in case of 3:1 stock split
         else if(Math.abs((currClose/nextOpen) - 3) < 0.3) {
            totalDivisor = totalDivisor*3;
            System.out.println("3:1 split on " + currDate + " " + currClose + " --> " + nextOpen + " | totalDivisor: " + totalDivisor);
            numSplits++;
         }
         // in case of 3:2 stock split
         else if(Math.abs((currClose/nextOpen) - 1.5) < 0.15) {
            totalDivisor = totalDivisor*1.5;
            System.out.println("3:2 split on " + currDate + " " + currClose + " --> " + nextOpen + " | totalDivisor: " + totalDivisor);
            numSplits++;
         }
         
         // add current stock data to deque
         StockData currStock = new StockData();
         currStock.date = currDate;
         currStock.openPrice = Double.parseDouble(stockInfo.getString(3).trim()) / totalDivisor;
         currStock.highPrice = Double.parseDouble(stockInfo.getString(4).trim()) / totalDivisor;
         currStock.lowPrice = Double.parseDouble(stockInfo.getString(5).trim()) / totalDivisor;
         currStock.closePrice = currClose / totalDivisor;
         result.addFirst(currStock);
      }

      System.out.println(numSplits + " splits in " + result.size() + " trading days");
      
      return result;
   }
   
   // executes an investment strategy on the given data.
   // the details of this strategy are in the assignment instructions.
   // does no trading and quits early if given less than 51 days of data
   // ticker is a String given in the main method, taken from console input.
   // data deque is taken from getStockData
   static void doStrategy(String ticker, Deque<StockData> data) {
      //To Do: 
      // Apply Steps 2.6 to 2.10 explained in the assignment description 
      // data (which is a Deque) has all the information (after the split adjustment) you need to apply these steps
      
      // 2.8
      // if less than 51 days of data, do no trading and report no gain
      if(data.size() < 51) {
         printResults(0, 0);
         return;
      }

      // deque for moving average of closing prices
      Deque<Double> maDeque = new ArrayDeque<>();
      double maTotal = 0;

      // initialize variables used in for each loop
      double currCash = 0;
      double currShares = 0;
      double prevClose = 0; // previous closing price, used for determining if we should sell on a given day
      double currOpen = 0;
      int transNum = 0; // transaction number
      boolean buy = false;

      for(StockData dayData : data) {
         double currClose = dayData.closePrice;
         currOpen = dayData.openPrice;
         // if we need to buy today
         if(buy) {
            currShares += 100;
            currCash -= (100*currOpen) + 8.00; // remove cash for buying shares, plus $8 transaction fee
            transNum++;
            buy = false;
         }
         // check for exactly 50 entries in maDeque (for trading) BEFORE adjusting ma to keep ma accurate
         if(maDeque.size() == 50) {
            double ma = maTotal / 50.0;
            // 2.9
            // Execute investment strategy
            // buy criteria
            if(currClose < ma && (currClose / currOpen < 0.97000001)) {
               buy = true; // flip buy to true to buy at opening price on next day
            }
            // sell criteria
            else if(currShares >= 100 && currOpen > ma && (currOpen / prevClose > 1.00999999)) {
               currShares -= 100;
               // sell by average price on that day
               currCash += (100*(currOpen + currClose)/2) - 8.00; // include $8 transaction fee
               transNum++;
            }
         }
         
         // 2.7
         // add new closing price to maDeque. 
         maDeque.addLast(currClose);
         maTotal += currClose;

         // if adding sets it to over 50, pop first element and subtract from maTotal to keep size constant
         if(maDeque.size() > 50) {
            maTotal -= maDeque.pop();
         }
         prevClose = currClose; // update previous closing price to today's closing price, for sell determination
      }

      currCash += currOpen*currShares; // add remaining shares to currCash, ignoring transaction fee
      currCash = Math.round(currCash * 100.0) / 100.0; // round cash to 2 decimal places

      printResults(transNum, currCash); 
   }
   
   // helper method for doStrategy. Simply prints out results for trading
   static void printResults(int transNum, double netCash) {
      System.out.println("Transactions executed: " + transNum);
      System.out.println("Net cash: " + netCash + "\n");
   }
}