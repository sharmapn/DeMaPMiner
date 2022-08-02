package connections;

import java.io.IOException;
import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;
import java.util.ArrayList;
import java.util.Properties;



public class MysqlConnect {
    // init database constants
    private static final String DATABASE_DRIVER = "com.mysql.jdbc.Driver";
    private static final String DATABASE_URL = "jdbc:mysql://localhost:3306/";  //peps_bck
    private static final String USERNAME = "root";
    private static final String PASSWORD = "root";
    private static final String MAX_POOL = "250";
    
    // init connection object
    private static Connection connection;
    // init properties object
    private static Properties properties;
    
    static String proposalIdentifier,databaseName;

    public static void main(String[] args) throws IOException {
    	Connection connection = connect();
    }   
    
    // create properties
    private static Properties getProperties() {
        if (properties == null) {
            properties = new Properties();
            properties.setProperty("user", USERNAME);
            properties.setProperty("password", PASSWORD);
            properties.setProperty("MaxPooledStatements", MAX_POOL);
        }
        return properties;
    }

    // connect database
    public static Connection connect() {
        if (connection == null) {
            try {
            	//read properties file
        		PropertiesFile wpf = new PropertiesFile();
        		// wpf.WriteToPropertiesFile("includeEmptyRows", includeEmptyRows.toString());
        		//includeStateData
        		databaseName = wpf.readFromPropertiesFile("database",false).toLowerCase();
            	
                Class.forName(DATABASE_DRIVER);
                connection = DriverManager.getConnection(DATABASE_URL+databaseName, getProperties());
                
                if (connection != null) {
        			System.out.println("\t Connection Successfull with "+databaseName + " database!");        			
        		} else {
        			System.out.println("Failed to make connection!");        		
        		}
                
            } catch (ClassNotFoundException | SQLException e) {
                e.printStackTrace();
            }
        }
        return connection;
    }

    // disconnect database
    public void disconnect() {
        if (connection != null) {
            try {
                connection.close();
                connection = null;
            } catch (SQLException e) {
                e.printStackTrace();
                System.out.println("\t DisConnection UnSuccessfull!");  
            }
        }
    }
    
	public static void testConnection() {
		final String JDBC_DRIVER = "com.mysql.jdbc.Driver";
		try {
			Class.forName("com.mysql.jdbc.Driver").newInstance();
		} catch (Exception e) {
			System.err.println("Unable to find and load driver");
			System.exit(1);
		}
	}
	
//	public static Connection getDBConnection() {
//		System.out.println("-------- MySQL JDBC Connection Testing ------------");
//
//		try {
//			Class.forName("com.mysql.jdbc.Driver");
//		} catch (ClassNotFoundException e) {
//			System.out.println("Where is your MySQL JDBC Driver?");
//			e.printStackTrace();
//		}
//
//		System.out.println("\t Driver Registered!");
//		Connection connection = null;
//
//		try {
//			connection = DriverManager.getConnection(DATABASE_URL, getProperties());
//
//		} catch (SQLException e) {
//			System.out.println("Connection Failed! Check output console");
//			e.printStackTrace();
//		}
//
//		if (connection != null) {
//			System.out.println("\t Connection Successfull!");
//		} else {
//			System.out.println("Failed to make connection!");
//		}
//		
//		return connection;
//	}
	
	public static Integer checkIfDatabaseEmpty( Connection conn){

		Statement stmt = null;
		Integer rowcount =0;
		try{	        
			// conn = DriverManager.getConnection(DATABASE_URL, getProperties());
			stmt = conn.createStatement();	        

			// the mysql insert statement
			ResultSet resultSet = stmt.executeQuery("SELECT COUNT(*) FROM allmessages");

			// Get the number of rows from the result set
			resultSet.next();
			rowcount = resultSet.getInt(1);

			if (rowcount ==0)
				System.out.println("\t No records in Table");  
			else 
				System.out.println("\t " + rowcount + " records in Table"); 
			//	        conn.close();
		}catch(SQLException se){
			//Handle errors for JDBC
			se.printStackTrace();
		}catch(Exception e){
			//Handle errors for Class.forName
			e.printStackTrace();
		}finally{
			//finally block used to close resources
			try{
				if(stmt!=null)
					stmt.close();
			}catch(SQLException se2){
			}// nothing we can do

		}//end try	
		return rowcount;
	}
	
	public static void emptyDatabaseTable(Connection conn, String tableName){
		  
	     Statement stmt = null;

	     try{	        
	       // conn = DriverManager.getConnection(DATABASE_URL, getProperties());
	        stmt = conn.createStatement();	        
		    
		// the mysql DELETE statement
	      int resultCount = stmt.executeUpdate("DELETE FROM "+ tableName + ";");
	      
	      if (resultCount ==0)
	    	   System.out.println("\t No records in Table");  
	      else 
	    	  System.out.println("\t " + resultCount + " records in Table"); 
//	        conn.close();
	     }catch(SQLException se){
	        //Handle errors for JDBC
	        se.printStackTrace();
	     }catch(Exception e){
	        //Handle errors for Class.forName
	        e.printStackTrace();
	     }finally{
	        //finally block used to close resources
	        try{
	           if(stmt!=null)
	              stmt.close();
	        }catch(SQLException se2){
	        }// nothing we can do
	        
	     }//end try	     
	}
	
	public static ArrayList returnUniquePepsInDatabase(Connection conn){
		   //Utilities u = new Utilities();
		   //testConnection();       
	       Integer counter =0;
	       
	       ArrayList<Integer> uniquePeps = new ArrayList<>();
		   //numbers.add(308);
		   
	       Integer pepNumber;
	       
		   //  Connect to an MySQL Database, run query, get result set

	       String sql = "SELECT distinct pep from allmessages ;"; 
	       //can add messageid later
	       try (Connection v_connection = conn;
	               Statement stmt = v_connection.createStatement();
	               ResultSet rs = stmt.executeQuery( sql ))
	           {
	           	 while (rs.next()){     //check every message       
	           		 pepNumber = rs.getInt(1);
	           		 uniquePeps.add(pepNumber);
	           	 }
	           	 stmt.close();
	           }
	       catch (SQLException e)
	       {
	           System.out.println( e.getMessage() );
	       }
	       
		return uniquePeps;
	   } 
	
	
	public static Integer returnSQLRowCount(String sql, Connection conn){
		   //Utilities u = new Utilities();
		   testConnection();       
	       Integer counter =0;
	       
	       ArrayList<Integer> uniquePeps = new ArrayList<>();
		   //numbers.add(308);
		   
	       Integer pepNumber;
	       
		   //  Connect to an MySQL Database, run query, get result set

	       
	       Integer count = 0;
	       
	       //can add messageid later
	       try (Connection connection = conn;
	               Statement stmt = connection.createStatement();
	               ResultSet rs = stmt.executeQuery( sql ))
	           {
	           	 while (rs.next()){     //check every message       
	           		count++;
	           	 }
	           	 stmt.close();
	           }
	       catch (SQLException e)
	       {
	           System.out.println( e.getMessage() );
	       }
	       
		return count;
	   } 
	 
	 public static Integer returnRSCount (String v_sql, Connection conn){   
		   //testConnection();       
	       //  Connect to an MySQL Database, run query, get result set

	       //check for parent messages - i.e. he message to which this message is a reply
	       //get the message id of these messages
	       String sql = v_sql; 
	       Integer counter = null; 	       //String v_inReplyTo;
	       String v_Message;
		   //now for each of the above message, 
	       Statement stmt = null;
		   try{		   
			   Connection connection = conn;
	           stmt = connection.createStatement( ResultSet.TYPE_SCROLL_INSENSITIVE, ResultSet.CONCUR_READ_ONLY);
	           ResultSet rs = stmt.executeQuery( sql );
			   if (rs.next()) {
	               rs.last(); 	               counter = rs.getRow();
	              // return rs.getRow();
	           } 
	           else {
	          //     System.out.println("No Data");
	               counter = 0;
	           }
		   }   
	           catch (SQLException e1){
	               System.out.println( e1.getMessage() );
	            }
	     	   catch(Exception e){
	     	        //Handle errors for Class.forName
	     	        e.printStackTrace();
	     	   }
	            finally{
	     	        //finally block used to close resources
	                   try{
	     		           if( stmt != null)
	     		              stmt.close();
	                   	}
	                   catch(SQLException se2){
	                   	
	                   }// nothing we can do
	               }  
		   return counter;		 
	   }	
}
