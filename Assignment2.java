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
               // Step 2.3 and 2.4: Get pricevolume data 
               Deque<StockData> data = getStockData(ticker, startdate, enddate);
               System.out.println();
               System.out.println("Executing investment strategy");
               // Step 2.5-2.10: execute investment strategy 
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
         System.out.print(name.getString(1));
         return true;
      }
      // otherwise, say we didn't get a result and return false.
      System.out.println("Ticker '" + ticker + "' not found in database.");
      return false;
   }

   static Deque<StockData> getStockData(String ticker, String start, String end) throws SQLException{	  
      // create and execute preparedStatement when a date range is given
      if(start != null && end != null) {
         PreparedStatement stockStatement = conn.prepareStatement(
            "select * from priceVolume" +
            " where Ticker = ? AND" +
            " TransDate BETWEEN ? AND ?" +
            " order by TransDate DESC"
         );
         stockStatement.setString(1, ticker);
         stockStatement.setString(2, start);
         stockStatement.setString(3, end);
         ResultSet stockData = stockStatement.executeQuery();
      }
      // create and execute preparedStatement for no date range
      else {
         PreparedStatement stockStatement = conn.prepareStatement(
            "select * from priceVolume" +
            " where Ticker = ?" +
            " order by TransDate DESC"
         );
         stockStatement.setString(1, ticker);
         ResultSet stockData = stockStatement.executeQuery();
      }

      Deque<StockData> result = new ArrayDeque<>();

	  // To Do: 
	  // Loop through all the dates of that company (descending order)
			// Find a split if there is any (2:1, 3:1, 3:2) and adjust the split accordingly
			// Include the adjusted data to the result (which is a Deque); You can use addFirst method for that purpose
	         
      return result;
   }
   
   static void doStrategy(String ticker, Deque<StockData> data) {
	  //To Do: 
	  // Apply Steps 2.6 to 2.10 explained in the assignment description 
	  // data (which is a Deque) has all the information (after the split adjustment) you need to apply these steps
   }
}