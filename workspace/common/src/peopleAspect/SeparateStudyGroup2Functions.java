package peopleAspect;

import java.sql.Connection;
import java.sql.Date;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;
import java.util.ArrayList;

//import miner.process.PythonSpecificMessageProcessing;
import utilities.PepUtils;

public class SeparateStudyGroup2Functions {
	static PythonSpecificMessageProcessing pms = new PythonSpecificMessageProcessing();
	static String proposalIdentifier = "pep";
	
	public static ArrayList<String> getAllDistinctDevelopersOrderedbyNumberPosts(Connection conn){
		ArrayList<String> distinctAuthors = new ArrayList<String>();
		
		try	{
			//String query= "SELECT distinct(senderName) FROM allpeps;";
			//ordered by most posts author													//where messageID < 100000 
			String query = "select senderName, count(DISTINCT(messageID)) c from allmessages group by senderName order by c DESC";
			
			Statement st = conn.createStatement();			ResultSet rs = st.executeQuery(query);			
			 
			while (rs.next())  {
				distinctAuthors.add(rs.getString(1));
			}
			//System.out.println("\n\nTotal number of peps in "+ pepTypeString + " is " + counter + " list size " + peps.size());
			st.close();
		}	catch (Exception e)	{
			System.err.println("Got an exception 12! ");
			System.err.println(e.getMessage());
		}		
		return distinctAuthors;
	}
	
	public static ArrayList<String> getAllDistinctAuthorsOrderedbyNumberPosts(Connection conn){
		ArrayList<String> distinctAuthors = new ArrayList<String>();
		
		try	{
			//String query= "SELECT distinct(senderName) FROM allpeps;";
			//ordered by most posts author													//where messageID < 100000 
			String query = "select senderName, count(DISTINCT(messageID)) c from allmessages group by senderName order by c DESC";
			
			Statement st = conn.createStatement();			ResultSet rs = st.executeQuery(query);			
			 
			while (rs.next())  {
				distinctAuthors.add(rs.getString(1));
			}
			//System.out.println("\n\nTotal number of peps in "+ pepTypeString + " is " + counter + " list size " + peps.size());
			st.close();
		}	catch (Exception e)	{
			System.err.println("Got an exception 13! ");
			System.err.println(e.getMessage());
		}		
		return distinctAuthors;
	}
	//if suthors atored in a table already
	public static ArrayList<String> getAllDistinctAuthorsOrderedByName(Connection conn, String tablename){
		ArrayList<String> distinctAuthors = new ArrayList<String>();
		
		try	{
			//String query= "SELECT distinct(senderName) FROM allpeps;";
			//ordered by most posts author										//filter when stopped in middle - senderName like 'ge%' AND 
												//distinctauthorsallmessages	// senderName like 'gui%' AND //AND senderName  = 'guido.van.rossum'
			String query = "select senderName from "+tablename+" where senderName IS NOT NULL Order By SenderName ASC ;";
			
			Statement st = conn.createStatement();			ResultSet rs = st.executeQuery(query);			
			 
			while (rs.next())  {
				distinctAuthors.add(rs.getString(1));
			}
			//System.out.println("\n\nTotal number of peps in "+ pepTypeString + " is " + counter + " list size " + peps.size());
			st.close();
		}	catch (Exception e)	{
			System.err.println("Got an exception 14! ");			System.err.println(e.getMessage());
		}		
		return distinctAuthors;
	}	
	
	public static ArrayList<String> getAllDistinctPythonLists(Connection conn){
		ArrayList<String> distinctAuthors = new ArrayList<String>();
		
		try	{
			//String query= "SELECT distinct(senderName) FROM allpeps;";
			//ordered by most posts author
			String query = "select DISTINCT(folder) from allmessages";
			
			Statement st = conn.createStatement();			ResultSet rs = st.executeQuery(query);			
			 
			while (rs.next())  {
				distinctAuthors.add(rs.getString(1));
			}
			//System.out.println("\n\nTotal number of peps in "+ pepTypeString + " is " + counter + " list size " + peps.size());
			st.close();
		}	catch (Exception e)	{
			System.err.println("Got an exception 15! ");			System.err.println(e.getMessage());
		}		
		return distinctAuthors;
	}	
	
	public static ArrayList<Integer> getAllDistinctProposals(Connection conn){
		ArrayList<Integer> distinctProposals = new ArrayList<Integer>();
		
		try	{
			//String query= "SELECT distinct(senderName) FROM allpeps;";
			//ordered by most posts author
			String query = "select DISTINCT(pep) from pepdetails";
			
			Statement st = conn.createStatement();			ResultSet rs = st.executeQuery(query);			
			 
			while (rs.next())  {
				distinctProposals.add(rs.getInt(1));
			}
			//System.out.println("\n\nTotal number of peps in "+ pepTypeString + " is " + counter + " list size " + peps.size());
			st.close();
		}	catch (Exception e)	{
			System.err.println("Got an exception 16! ");			System.err.println(e.getMessage());
		}		
		return distinctProposals;
	}	
	
	public static String assignPEPTypes(Integer pepType) {
		String pepTypeString = null;
		if (pepType == 0)
			pepTypeString = "All";
		else if (pepType == 1)
			pepTypeString = "Standards Track";
		else if (pepType == 2)
			pepTypeString = "Informational";
		else if (pepType == 3)
			pepTypeString = "Process";
		else
			System.out.format("Incorrect pep type");
		return pepTypeString;
	}
	
	static Integer getNumberDiscussionsBetweenDates(Connection connection, Integer pepNumber, Date startDate,
			Date endDate, Integer count) throws SQLException {
		String sql3 = "SELECT count(email) from allpeps WHERE pep = " + pepNumber + " and (dateTime between '" + startDate + "' and '" +  endDate + "') and messageID <100000;";  //there will be many rows since there is no seprate table to store this information
		Statement stmt3 = connection.createStatement();
		ResultSet rs3 = stmt3.executeQuery( sql3 );
		if (rs3.next()){    
			count = rs3.getInt(1);						
		}
		else{
//						System.out.println("State end not found for pep " + pepNumber);
		}
		return count;
	}
	
	static Integer getNumberDiscussionsBeforeDate(Connection connection, Integer pepNumber, Date startDate, Integer count) throws SQLException {
		String sql3 = "SELECT count(email) from allpeps WHERE pep = " + pepNumber + " and dateTime < '" + startDate + "' and messageID <100000;";  //there will be many rows since there is no seprate table to store this information
		Statement stmt3 = connection.createStatement();
		ResultSet rs3 = stmt3.executeQuery( sql3 );
		if (rs3.next()){    
			count = rs3.getInt(1);						
		}
		else{
//						System.out.println("State end not found for pep " + pepNumber);
		}
		return count;
	}
	static Integer getNumberDiscussionsAfterDate(Connection connection, Integer pepNumber, Date endDate, Integer count) throws SQLException {
		String sql3 = "SELECT count(email) from allpeps WHERE pep = " + pepNumber + " and dateTime > '" + endDate + "' and messageID <100000;";  //there will be many rows since there is no seprate table to store this information
		Statement stmt3 = connection.createStatement();
		ResultSet rs3 = stmt3.executeQuery( sql3 );
		if (rs3.next()){    
			count = rs3.getInt(1);						
		}
		else{
//						System.out.println("State end not found for pep " + pepNumber);
		}
		return count;
	}

	static Date getEndDate(Integer pepNumber,  String endState, Connection connection) throws SQLException {
		//end date
		Date endDate = null;																			//and messageID <100000
		String sql2 = "SELECT dateTime from allpeps WHERE pep = " + pepNumber + " and email = 'Status : "+endState+"' ;";  //there will be many rows since there is no seprate table to store this information
		Statement stmt2 = connection.createStatement();
		ResultSet rs2 = stmt2.executeQuery( sql2 );
		if (rs2.next()){    
			endDate = rs2.getDate(1);						
		}
		else{
//						System.out.println("State end not found for pep " + pepNumber);
		}
		return endDate;
	}

	static Date getStartDate(Integer pepNumber, String startState, Connection connection) throws SQLException {
		//state start date
		Date startDate = null; 																				//and messageID <100000
		String sql1 = "SELECT dateTime from allpeps WHERE pep = " + pepNumber + " and email = 'Status : "+startState+"' ;";  //there will be many rows since there is no seprate table to store this information
		Statement stmt1 = connection.createStatement();
		ResultSet rs1 = stmt1.executeQuery( sql1 );
		if (rs1.next()){    
			startDate = rs1.getDate(1);
			//System.out.println("Pep " + pepNumber + " Startdate " + rs1.getDate(1));
		}
		else{
//						System.out.println("State start not found for pep " + pepNumber);
		}
		return startDate;
	}
	static Integer getDiscussionsForPEPNumber(Connection connection ,Integer pepNumber, Double version) {
		//Integer v_pep = 451;				
		Integer numberMessages = null;
		boolean showOutput=false;		
		
		try{																			//ONLY ARCHIVE DAT, IGNORE DANIEL DATA, GMANE DATA AND PE SUMMARY
			String sql2 = "SELECT count(*) from allpeps WHERE pep = " + pepNumber + " and messageID < 100000;";  //there will be many rows since there is no seprate table to store this information
			Statement stmt2 = connection.createStatement();
			ResultSet rs2 = stmt2.executeQuery( sql2 );
			if (rs2.next()){    
				numberMessages = rs2.getInt(1);
				System.out.print(numberMessages);
			}
			else{
				System.out.print("0");
			}
		}
		catch (SQLException e)
		{
			System.out.println( e.getMessage() );
		}
		catch (Exception ex)	{
			System.out.println( ex.getMessage() );
		}
		return numberMessages;
	}
	
	
	public static Integer totalNumberOfMessages(Connection connection) {
		Integer numberMessages=0;		
		try {
			String sql2 = "SELECT count(messageID) from allmessages;";  //there will be many rows since there is no seprate table to store this information
			Statement stmt2;
			stmt2 = connection.createStatement();			ResultSet rs2 = stmt2.executeQuery( sql2 );
			if (rs2.next()){    
				numberMessages = rs2.getInt(1);
			}
			System.out.println("Total number of messages " + numberMessages);
		} catch (SQLException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
		return numberMessages;
	}
	
	public static Integer totalNumberOfUniqueMessages(Connection connection) {
		Integer numberUniqueMessages=0;		
		try {
			String sql2 = "SELECT count(distinct(messageID)) from allmessages;";  //there will be many rows since there is no seprate table to store this information
			Statement stmt2;
			stmt2 = connection.createStatement();			ResultSet rs2 = stmt2.executeQuery( sql2 );
			if (rs2.next()){    
				numberUniqueMessages = rs2.getInt(1);
			}
			System.out.println("Total number of unique messages " + numberUniqueMessages);
		} catch (SQLException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
		return numberUniqueMessages;
	}
	
	public static ArrayList<Integer> returnAllUniqueMessages(Connection connection) {
		//Integer numberUniqueMessages=0;		
		ArrayList<Integer> numberUniqueMessages = new ArrayList<Integer>();
		try {								//messageID						//where messageid > 1494862;
			String sql2 = "SELECT distinct(emailmessageid) from allmessages ";  //there will be many rows since there is no seprate table to store this information
			Statement stmt2;
			stmt2 = connection.createStatement();			ResultSet rs2 = stmt2.executeQuery( sql2 );
			while (rs2.next()){    
				numberUniqueMessages.add(rs2.getInt(1));
			}
			System.out.println("all unique messages count: " + numberUniqueMessages.size());
		} catch (SQLException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
		return numberUniqueMessages;
	}
	
	
	
	public static ArrayList<String> returnAllUniqueEmailMessagesID(Connection connection) {
		//Integer numberUniqueMessages=0;		
		ArrayList<String> UniqueEmailMessagesIDs = new ArrayList<String>();
		try {								//messageID						//where messageid > 1494862;
																			//IS NULL to allow restart processing is fails in the middle 
			String sql2 = "SELECT distinct(emailmessageid) from allmessages WHERE countRepliesToThisMessage IS NULL";  //there will be many rows since there is no seprate table to store this information
			Statement stmt2;
			stmt2 = connection.createStatement();			ResultSet rs2 = stmt2.executeQuery( sql2 );
			while (rs2.next()){    
				UniqueEmailMessagesIDs.add(rs2.getString(1));
			}
			//System.out.println("all unique messages count: " + numberUniqueMessages.size());
		} catch (SQLException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
		return UniqueEmailMessagesIDs;
	}
		
	static void getDateTime(Connection connection ) {		
		Integer pepNumber, numberMessages = 0;
		boolean showOutput=false;
			
		try{
			Integer pep =318;
			Statement stmt = connection.createStatement();
							//, distinct messageID 
			String sql2 = "SELECT email, messageID from allpeps where pep ="+pep+" and and messageID <100000 order by date2 ;";  //there will be many rows since there is no seprate table to store this information
			Statement stmt2 = connection.createStatement();
			ResultSet rs2 = stmt2.executeQuery( sql2 );
			//for each message
			while (rs2.next()){ 
				String message = rs2.getString(1);		//get line with date on it
				Integer mid = rs2.getInt(2);
				String[] splitted = message.split("\n");
				
				//should only output one date for each message
				for (String m: splitted){
					if (m.contains("Date:")  && !m.contains("$Date:") ){					
						//now get date and  datetime
						 Date date9 = null; 
						 String dateString = m.replace("Date:", "");
						 
						 //natty get time
						 java.util.Date newDate = pms.returnDate(dateString);
	
	 			  		 System.out.println("MID " + mid + " Entire Line "+ m + " Date String " + dateString + " Extracted DATE " + date9 + " " +  newDate);						
						 break;  	//dont look any longer of found, dont wanmt to waste procesing time  and dont want to capture Last-Modified: $Date: 2004/09/01 15:02:22 $
					}
				}								 
				numberMessages++;
			//else{
			//	System.out.println("PEP " + 318 + " has no date ");
			}				
		}
		catch (SQLException e){
			System.out.println( e.getMessage() );
		}
		catch (Exception ex)	{
			System.out.println( ex.getMessage() );
		}
	}
	
	static ArrayList<Author> settleDistinctAuthors(ArrayList<Author> v_authorList, Connection connection){
		//get an author form distinct table
		//check next 10 enties
		//if one of them matches, delete it from table and add entry beside the key entry
		
		try {															//LIMIT 500
			String sql2 = "SELECT author from distinctauthors order by author ;";  //there will be many rows since there is no seprate table to store this information
			Statement stmt2 = connection.createStatement();
			ResultSet rs2 = stmt2.executeQuery( sql2 );
			
			//for each message
			String a = "Author", f = "Firstname", l = "Lastname";
			System.out.printf("%-40s %-20s %-20s \n",  a, f, l);
			while (rs2.next()){ 
				String permanentAuthor, author, firstName = null, lastName = null;
				permanentAuthor = rs2.getString(1);		//get line wi
				author = permanentAuthor;
				if (author==null || author.isEmpty() || author.length()==0)
				{}
				else
				{
					author = author.trim();
					
					if (author.contains("  ")){
						author = author.replace("  "," ");
					}
					if (author.contains(" "))	{
						String authorNames[] = author.split(" ");
					
						if (authorNames.length ==0){
								//check direct..but may not find
						}
						else if (authorNames.length ==1){
							//check direct..but may not find
						}
						else if (authorNames.length ==2){
							firstName = authorNames[0];
							lastName = authorNames[1];
						} 
						else {
							firstName = authorNames[0];
							lastName = authorNames[authorNames.length-1];
						}
					}
					
					Author au = new Author();					
					au.setData(author,firstName,lastName);
					//add to array
					v_authorList.add(au);
				}
				System.out.printf("%-40s %-20s %-20s \n",  author, firstName, lastName);
				//System.out.println("author: " + author+ " firstname "+ firstName + " lastname "+ lastName);
			}
		} catch (SQLException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}		
		return v_authorList;
	}
	
	public static void processAuthorList(ArrayList<Author> v_authorList){
		Boolean repeatedSentence = false,found = true;
  		
  		ArrayList<Integer> al = new ArrayList<Integer>(); 
  		
  		if(!v_authorList.isEmpty()) {
  			int counter =0;
  			for (int x=0; x <v_authorList.size(); x++)	{	
  				for (int y=0; y <10; y++)	{		//just check the next 10 rows //v_authorList.size()					        	
  					if(x !=y && v_authorList.get(x).getName() != null) 	{
  						String firstName = v_authorList.get(x).getFirstName().toLowerCase();
  						String lastName = v_authorList.get(x).getLastName().toLowerCase();	
  						
  						//the next elements from x
  						String compareFirstName = v_authorList.get(x+y).getFirstName().toLowerCase();
  						String compareLastName = v_authorList.get(x+y).getLastName().toLowerCase();
  						
  						//if same last name
  						if( lastName.toLowerCase().equals(compareLastName.toLowerCase()) )   {
  							repeatedSentence= true;
  							found=true;	
  							//mark for deletion by adding to array
  							System.out.println("\tauthor lastname matched--adding to remove list x: "+x + " y: "+ y + " xSentence: " + compareLastName);
  							//		  					al.add(y); //--delete the second instance, not the first
  							v_authorList.remove(x+y);
  							x--;
  						}  				
  					}
  				}
  			}
  			
  		}
  		
  		//output list
  		System.out.println("Final author list after removal");
  		
		if(!v_authorList.isEmpty()) {				
			for (int y=0; y <v_authorList.size(); y++)	{
				String author = v_authorList.get(y).getName().toLowerCase();
				String fName = v_authorList.get(y).getFirstName().toLowerCase();
				String lName = v_authorList.get(y).getLastName().toLowerCase();	
				
				System.out.printf("%-40s %-20s %-20s \n",  author, fName, lName);
			}
		}
		
	}
	
	static void getAuthor(Connection connection ) {		
		Integer pepNumber, numberMessages = 0;
		boolean showOutput=false;
		
		try{
			//actually all peps in flat file  -have to be read into seprate table - ????
			//get all unique peps, 
			PepUtils pu = new PepUtils();
			ArrayList<Integer> UniquePeps = pu.returnUniqueProposalsInDatabase(proposalIdentifier);				
			//Integer[] pep ={308,318};
			
			Statement stmt = connection.createStatement();
			String message;
			for (Integer p : UniquePeps)
			{
				System.out.println("\n-----------------Processing pep " + p); 
				//, distinct messageID 
				String sql2 = "SELECT email, messageID from allpeps where pep = "+p+" order by messageID ;";  //there will be many rows since there is no seprate table to store this information
				Statement stmt2 = connection.createStatement();
				ResultSet rs2 = stmt2.executeQuery( sql2 );
				//for each message
				while (rs2.next()){ 
					message = rs2.getString(1);		//get line with date on it
					Integer mid = rs2.getInt(2);
					String[] splitted = message.split("\n");						
					
					
					for (String m: splitted){					
						if (m.toLowerCase().contains("from:")   )
						{			//&& !m.contains("$Author:")
							String remove;
							String author = m.toLowerCase().replaceAll("from:", ""), permanent = m.replace("From:", "");
							author = pms.getAuthorFromString(author);
							
							//INSERT INTO Database  
							// duplicate entries for messageID would not be inserted to prevent duplicate records
							// duplicate messages do exist when same message has multiple peps mentioned so same message would be inserted for both peps
							try{
							      String query = " insert into authors (messageID,pep, author, initial)" + " values (?, ?, ?,?)";
							      // create the mysql insert preparedstatement
							      PreparedStatement preparedStmt = connection.prepareStatement(query);
							      preparedStmt.setInt (1, mid);
							      preparedStmt.setInt (2, p);
							      preparedStmt.setString   (3, author);
							      preparedStmt.setString   (4, permanent);
							      preparedStmt.execute();
							}
							catch (SQLException e){
								System.out.println( e.getMessage() + " DUPLICATE: mid "+ mid + " author " +author+ " p" );
							}
							catch (Exception ex)	{
								System.out.println( ex.getMessage() );
							}
							
							//System.out.println("MID " + mid + " | permanent " +permanent + " | from " + author);	
							System.out.printf("%-10s %-100s %-50s \n",  mid, permanent, author);
							
							//update database table
							
							break;
						}
						
					}
					//numberMessages++;
					//else{
					//	System.out.println("PEP " + 318 + " has no date ");
				}	
			}
		}
		catch (SQLException e){
			System.out.println( e.getMessage() );
		}
		catch (Exception ex)	{
			System.out.println( ex.getMessage() );
		}
	}	
	static boolean isDouble(String str) {
        try {
            Double.parseDouble(str);
            return true;
        } catch (NumberFormatException e) {
            return false;
        }
    }
}
