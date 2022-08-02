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

public class SeparateStudyGroupFunctions {
	static PythonSpecificMessageProcessing pms = new PythonSpecificMessageProcessing();
	static String proposalIdentifier = "pep";
	static String emailDataTable = "allmessages";
	static String stateDataTable = "pepstates_danieldata_datetimestamp";
	//march 2020..we only consider peps discussed before '2017-03-10'
	static String pepcreatedbeforedate = "'2017-03-10'";
	
	static String assignPEPTypes(Integer pepType) {
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
	

	public static void insertPairwiseStateIntoDB(Connection connection, String pairwisestate, Integer count, String thePEPType, Integer k, String folder, String tablename, String author) throws SQLException {
		try {
			//PreparedStatement statement3 = connection.prepareStatement("INSERT INTO pepcountsRQ2bysubphases (pep, peptype,pairwise,msgcount,folder) values (?, ?,?,?,?)");
			PreparedStatement statement3 = connection.prepareStatement("INSERT INTO "+tablename+" (pep, peptype,pairwise,msgcount,folder, author) values (?, ?,?,?,?,?)");
			statement3.setInt(1, k);							statement3.setString(2, thePEPType);
			statement3.setString(3, pairwisestate);			statement3.setInt(4, count);
			statement3.setString(5, folder);				statement3.setString(6, author);
			int row3 = statement3.executeUpdate();
		}
		catch (SQLException e)	{
			System.out.println( e.getMessage() );
		}
		catch (Exception ex)	{
			System.out.println( ex.getMessage() );
		}
	}
		
	static Integer getNumberDiscussionsBetweenDates(Connection connection, Integer pepNumber, Date startDate, Date endDate) throws SQLException {
		Integer count = 0;
		//and messageID <100000
		String sql3 = "SELECT count(email) from " + emailDataTable + " WHERE pepnum2020 = " + pepNumber + " and (dateTimeStamp between '" + startDate + "' and '" +  endDate + "') ;";  //there will be many rows since there is no seprate table to store this information
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
	
	//System.out.println(pepNumber + ","+ ml + ","+ thePEPType+","+ stateList.get(k)  + "-"+ stateList.get(k+1)  +","  +count);		//can add "Startdate: " + startDate + ", Enddate " + endDate + ",
	//ocal_insertPairwiseStateIntoDB(connection, stateList.get(k)  + "-"+ stateList.get(k+1) , count, thePEPType, pepNumber,ml);
	//march 09 2021 ...changed two instances of 'author' to 'clusterbysenderfullname'
	static Integer getNumberDiscussions_AuthorBetweenDates(Connection connection, Integer pepNumber, Date startDate, Date endDate, String thePEPType, String startState, 
			String endState, String tablename, String author) throws SQLException {
		Integer count = 0; String folder ="", v_author="";
		//and messageID <100000
		String sql3 = " SELECT clusterbysenderfullname, folder, count(email) from " + emailDataTable + " "
					+ "	WHERE clusterbysenderfullname = '"+author+"' and  pepnum2020 = " + pepNumber + " and (dateTimeStamp between '" + startDate + "' and '" +  endDate + "') "
					+ " group by author, folder ;";  //there will be many rows since there is no seprate table to store this information
		Statement stmt3 = connection.createStatement();
		ResultSet rs3 = stmt3.executeQuery( sql3 );
		while (rs3.next()){    
			v_author = rs3.getString(1);			folder = rs3.getString(2).replace("C:\\datasets\\","");		count = rs3.getInt(3);					
			//JAN 2021
			//folder = folder.substring(12);
			System.out.println("\t"+pepNumber + "," + v_author + ","+ folder+ ","+ thePEPType+","+ startState  +" "+endState  +","  +count);		//can add "Startdate: " + startDate + ", Enddate " + endDate + ",
			insertPairwiseStateIntoDB(connection, startState+"-"+ endState, count, thePEPType, pepNumber,folder, tablename, v_author);
			//insertPairwiseStateIntoDB(Connection connection, String pairwisestate, Integer count, String thePEPType, Integer k, String folder)
		}
		return count;
	}
		
	static Integer getNumberDiscussions_AuthorsRoleBetweenDates(Connection connection, Integer pepNumber, Date startDate, Date endDate, String thePEPType, String startState, 
			String endState, String tablename, String author) throws SQLException {
		Integer count = 0; String authorsrole ="", folder = "";
		//and messageID <100000
		String sql3 = "SELECT authorsrole, folder, count(email)  from " + emailDataTable + " "
				+ " WHERE (authorsrole = 'proposalauthor' OR authorsrole = 'bdfl_delegate') and  pepnum2020 = " + pepNumber + " and (dateTimeStamp between '" + startDate + "' and '" +  endDate + "') "
				+ " GROUP BY authorsrole, folder";  //there will be many rows since there is no seprate table to store this information
		Statement stmt3 = connection.createStatement();
		ResultSet rs3 = stmt3.executeQuery( sql3 );
		while (rs3.next()){    
			authorsrole = rs3.getString(1);	 folder = rs3.getString(2).replace("C:\\datasets\\",""); 	count = rs3.getInt(3);					
			//JAN 2021
			//folder = folder.substring(12);
			System.out.println("\t"+pepNumber +  ","+ authorsrole +  ","+ folder  + ","+ thePEPType+","+ startState  +" "+endState  +","  +count);		//can add "Startdate: " + startDate + ", Enddate " + endDate + ",
			insertPairwiseStateIntoDB(connection, startState+"-"+ endState, count, thePEPType, pepNumber,folder, tablename, authorsrole);
			//insertPairwiseStateIntoDB(Connection connection, String pairwisestate, Integer count, String thePEPType, Integer k, String folder)
		}
		return count;
	}
	
	//march 09 2021 ...changed three instances of 'author' to 'clusterbysenderfullname'
	static Integer getNumberDiscussions_AuthorBeforeDates(Connection connection, Integer pepNumber, Date startDate, String state, String thePEPType, String tablename, String author) throws SQLException {
		
		Integer count = 0; String folder ="", v_author = "";
		//and messageID <100000;
		//folder like '%"+folder+"%' 
		String sql3 = "SELECT clusterbysenderfullname, folder, count(email) from " + emailDataTable + " WHERE  clusterbysenderfullname = '" + author + "' and pepnum2020 = " + pepNumber + " and dateTimeStamp < '" + startDate + "' "
				+ "	group by clusterbysenderfullname, folder ";  //there will be many rows since there is no seprate table to store this information
		Statement stmt3 = connection.createStatement();
		ResultSet rs3 = stmt3.executeQuery( sql3 );
		while (rs3.next()){    
			v_author = rs3.getString(1); folder = rs3.getString(2).replace("C:\\datasets\\",""); 	count = rs3.getInt(3);					
			//JAN 2021
			//folder = folder.substring(12);
			System.out.println("\t"+pepNumber + ","+ author+  ","+ folder+ ","+ thePEPType+","+  "PREDRAFT-"+ state  +","  +count);		//can add "Startdate: " + startDate + ", Enddate " + endDate + ",
			insertPairwiseStateIntoDB(connection, "PREDRAFT-"+ state, count, thePEPType, pepNumber,folder,tablename, author);
			//insertPairwiseStateIntoDB(Connection connection, String pairwisestate, Integer count, String thePEPType, Integer k, String folder)
		}
		//else{
//						System.out.println("State end not found for pep " + pepNumber);
		//}
		return count;
	}
	
	static Integer getNumberDiscussions_AuthorRoleBeforeDates(Connection connection, Integer pepNumber, Date startDate, String state, String thePEPType, String tablename, String author) throws SQLException {
		
		Integer count = 0; String folder ="", authorsrole="";
		//and messageID <100000;
		//folder like '%"+folder+"%' 
		String sql3 = "SELECT authorsrole, folder, count(email) from " + emailDataTable + " "
				+	  " WHERE  (authorsrole = 'proposalauthor' or authorsrole = 'bdfl_delegate') and pepnum2020 = " + pepNumber + " and dateTimeStamp < '" + startDate + "' group by authorsrole, folder ";  //there will be many rows since there is no seprate table to store this information
		Statement stmt3 = connection.createStatement();
		ResultSet rs3 = stmt3.executeQuery( sql3 );
		while (rs3.next()){    
			authorsrole  = rs3.getString(1);	  folder = rs3.getString(2).replace("C:\\datasets\\","");		count = rs3.getInt(3);			
			//JAN 2021
			//folder = folder.substring(12);
			System.out.println("\t"+pepNumber +  ","+ authorsrole+ ","+ folder+","+ thePEPType+","+  "PREDRAFT-"+ state  +","  +count);		//can add "Startdate: " + startDate + ", Enddate " + endDate + ",
			insertPairwiseStateIntoDB(connection, "PREDRAFT-"+ state, count, thePEPType, pepNumber,folder,tablename, authorsrole);
			//insertPairwiseStateIntoDB(Connection connection, String pairwisestate, Integer count, String thePEPType, Integer k, String folder)
		}
		//else{
//						System.out.println("State end not found for pep " + pepNumber);
		//}
		return count;
	}
	
	static Integer getNumberDiscussionsBeforeDate(Connection connection, Integer pepNumber, Date startDate) throws SQLException {
		Integer count = 0;
		//and messageID <100000;
		//folder like '%"+folder+"%' 
		String sql3 = "SELECT count(email) from " + emailDataTable + " WHERE  pepnum2020 = " + pepNumber + " and dateTimeStamp < '" + startDate + "' ";  //there will be many rows since there is no seprate table to store this information
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
	
	//march 09 2021 ...changed three instances of 'author' to 'clusterbysenderfullname'
	static Integer getNumberDiscussions_AuthorAfterDate(Connection connection, Integer pepNumber, Date endDate, String state, String thePEPType, String tablename, String author) throws SQLException {
		Integer count = 0; String folder ="";
		//and messageID <100000
		//folder like '%"+folder+"%' 
		//String sql3 = "SELECT count(email) from " + emailDataTable + " WHERE pep = " + pepNumber + " and dateTimeStamp > '" + endDate + "' ;";  //there will be many rows since there is no seprate table to store this information
		//marc 2020
		//(dateTimeStamp between '" + startDate + "' and '" +  endDate + "')
		String sql3 = "SELECT  clusterbysenderfullname, folder, count(email) from " + emailDataTable + " "
					+ " WHERE clusterbysenderfullname = '" + author + "' and pepnum2020 = " + pepNumber + " and (dateTimeStamp between '" + endDate + "' and " +  pepcreatedbeforedate + ") group by clusterbysenderfullname, folder ;";  //there will be many rows since there is no seprate table to store this information
		Statement stmt3 = connection.createStatement();
		ResultSet rs3 = stmt3.executeQuery( sql3 );
		while (rs3.next()){    
			author = rs3.getString(1);	  folder = rs3.getString(2).replace("C:\\datasets\\","");	count = rs3.getInt(3);	
			//JAN 2021
			//folder = folder.substring(12);
			System.out.println("\t"+pepNumber +  ","+ author+"," + folder+ ","+ thePEPType+","+  state  +"-FINALPOST,"  +count);		//can add "Startdate: " + startDate + ", Enddate " + endDate + ",
			insertPairwiseStateIntoDB(connection, state  +"-FINALPOST", count, thePEPType, pepNumber,folder,tablename, author);
			//insertPairwiseStateIntoDB(Connection connection, String pairwisestate, Integer count, String thePEPType, Integer k, String folder)
		}
		return count;
	}
	
	static Integer getNumberDiscussions_AuthorsRoleAfterDate(Connection connection, Integer pepNumber, Date endDate, String state, String thePEPType, String tablename, String author) throws SQLException {
		Integer count = 0; String folder = "", authorsrole ="";
		//and messageID <100000
		//folder like '%"+folder+"%' 
		//String sql3 = "SELECT count(email) from " + emailDataTable + " WHERE pep = " + pepNumber + " and dateTimeStamp > '" + endDate + "' ;";  //there will be many rows since there is no seprate table to store this information
		//marc 2020
		//(dateTimeStamp between '" + startDate + "' and '" +  endDate + "')
		String sql3 = " SELECT  authorsrole, folder, count(email) from " + emailDataTable + " "
					+ " WHERE (authorsrole = 'proposalauthor' or authorsrole = 'bdfl_delegate')  and pepnum2020 = " + pepNumber + " and (dateTimeStamp between '" + endDate + "' and " +  pepcreatedbeforedate + ") "
					+ " group by authorsrole, folder ;";  //there will be many rows since there is no seprate table to store this information
		Statement stmt3 = connection.createStatement();
		ResultSet rs3 = stmt3.executeQuery( sql3 );
		while (rs3.next()){    
			authorsrole  = rs3.getString(1);	  folder = rs3.getString(2).replace("C:\\datasets\\","");	count = rs3.getInt(3);				
			//JAN 2021
			//folder = folder.substring(12);
			System.out.println("\t"+pepNumber +  ","+ authorsrole+ ","+ folder+","+ thePEPType+","+  state  +"-FINALPOST,"  +count);		//can add "Startdate: " + startDate + ", Enddate " + endDate + ",
			insertPairwiseStateIntoDB(connection, state  +"-FINALPOST", count, thePEPType, pepNumber,folder,tablename, authorsrole);
			//insertPairwiseStateIntoDB(Connection connection, String pairwisestate, Integer count, String thePEPType, Integer k, String folder)
		}
		return count;
	}
	
	
	static Integer getNumberDiscussionsAfterDate(Connection connection, Integer pepNumber, Date endDate) throws SQLException {
		Integer count = 0;
		//and messageID <100000
		//folder like '%"+folder+"%' 
		//String sql3 = "SELECT count(email) from " + emailDataTable + " WHERE pep = " + pepNumber + " and dateTimeStamp > '" + endDate + "' ;";  //there will be many rows since there is no seprate table to store this information
		//marc 2020
		//(dateTimeStamp between '" + startDate + "' and '" +  endDate + "')
		String sql3 = "SELECT count(email) from " + emailDataTable + " WHERE pepnum2020 = " + pepNumber + " and (dateTimeStamp between '" + endDate + "' and " +  pepcreatedbeforedate + ") ;";  //there will be many rows since there is no seprate table to store this information
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
		String sql2 = "SELECT dateTimestamp from " + stateDataTable + " WHERE pep = " + pepNumber + " and email = 'Status : "+endState+"' ;";  //there will be many rows since there is no seprate table to store this information
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
		String sql1 = "SELECT dateTimestamp from " + stateDataTable + " WHERE pep = " + pepNumber + " and email = 'Status : "+startState+"' ;";  //there will be many rows since there is no seprate table to store this information
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
																						//and messageID < 100000
			String sql2 = "SELECT count(*) from " + stateDataTable + " WHERE pep = " + pepNumber + " ;";  //there will be many rows since there is no seprate table to store this information
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
	
	
	static void totalNumberOfMessages(Connection connection) {
		Integer numberMessages=0;		
		try {
			String sql2 = "SELECT count(messageID) from "+emailDataTable+" where messageID <100000;";  //there will be many rows since there is no seprate table to store this information
			Statement stmt2;
			stmt2 = connection.createStatement();
			ResultSet rs2 = stmt2.executeQuery( sql2 );
			if (rs2.next()){    
				numberMessages = rs2.getInt(1);
			}
			System.out.println("Total number of messages " + numberMessages);
		} catch (SQLException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
	}
	
	static void totalNumberOfUniqueMessages(Connection connection) {
		Integer numberUniqueMessages=0;		
		try {
			//where messageID <100000;
			String sql2 = "SELECT count(distinct(messageID)) from "+emailDataTable+" ";  //there will be many rows since there is no seprate table to store this information
			Statement stmt2;
			stmt2 = connection.createStatement();
			ResultSet rs2 = stmt2.executeQuery( sql2 );
			if (rs2.next()){    
				numberUniqueMessages = rs2.getInt(1);
			}
			System.out.println("Total number of unique messages " + numberUniqueMessages);
		} catch (SQLException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
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
					if (author.contains(" "))
					{
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
		Boolean repeatedSentence = false;
  		Boolean found = true;
  		
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
