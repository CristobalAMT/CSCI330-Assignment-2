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
import java.sql.SQLException;
import java.util.*;

class Assign2Skeleton {
   
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
      try {
         Class.forName("com.mysql.cj.jdbc.Driver");
         String dburl = connectprops.getProperty("dburl");
         String username = connectprops.getProperty("user");
         conn = DriverManager.getConnection(dburl, connectprops);
         System.out.println("Database connection is established");
         
         Scanner in = new Scanner(System.in);
         System.out.print(prompt);
         String input = in.nextLine().trim();
         
         while (input.length() > 0) {
            String[] params = input.split("\\s+");
            String ticker = params[0];
            String startdate = null, enddate = null;
            if (getName(ticker)) {
               if (params.length >= 3) {
                  startdate = params[1];
                  enddate = params[2];
               }               
               Deque<StockData> data = getStockData(ticker, startdate, enddate);
               System.out.println();
               System.out.println("Executing investment strategy");
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
   
   static boolean getName(String ticker) throws SQLException {
	  // To Do: 
	  // Execute the first query and print the company name of the ticker user provided (e.g., INTC to Intel Corp.) 
	  // Please don't forget to use a prepared statement
     return false;
   }

   static Deque<StockData> getStockData(String ticker, String start, String end) {	  
	  // To Do: 
	  // Execute the second query, which will return stock information of the ticker (descending on the transaction date)
	  // Please don't forget to use a prepared statement	   

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