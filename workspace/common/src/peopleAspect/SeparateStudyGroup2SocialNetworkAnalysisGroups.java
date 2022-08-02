package peopleAspect;

import java.sql.*;
import java.sql.Date;
import java.text.SimpleDateFormat;
import java.time.LocalDate;
import java.time.ZoneId;
import java.util.*;
import java.util.regex.Pattern;

import java.io.*;
import java.text.DateFormat;
import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.regex.Matcher;
import java.util.regex.Pattern;



import javax.swing.*;
import javax.swing.table.*;
import javax.swing.tree.DefaultMutableTreeNode;
import javax.swing.tree.MutableTreeNode;

//import org.joda.time.DateTime;
//import org.joda.time.format.DateTimeFormat;

import connections.MysqlConnect;
//import miner.process.PythonSpecificMessageProcessing;
import utilities.PepUtils;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;

import com.joestelmach.natty.*;

//import readRepository.readMetadataFromWeb.GetProposalDetailsWebPage;

//to populate table for each member in all of python - his count of posts in each list
public class SeparateStudyGroup2SocialNetworkAnalysisGroups extends JFrame
{	
	static PythonSpecificMessageProcessing pms = new PythonSpecificMessageProcessing();
	static SeparateStudyGroup2Functions f = new SeparateStudyGroup2Functions();
	static ReturnDifferentCommunityMembers rdcm = new ReturnDifferentCommunityMembers();
	
	public static void main(String[] args) {		
		ArrayList<Double> vlist = new ArrayList<>();	
		MysqlConnectForQueries mc = new MysqlConnectForQueries();
		Connection connection = mc.connect();
		
		f.totalNumberOfMessages(connection);
		f.totalNumberOfUniqueMessages(connection);
		
		populateTable(0,connection);		//uses new table allmessages
	
		mc.disconnect();
	}
	
	//	social network analysis..get the number of posts made by each author (all community members
	private static void populateTable(Integer pepType, Connection connection) 
	{
		Integer seedingCount=0, totalDistinctCounter=0;
		String author,comma=",";
		ArrayList<String> distinctAuthors = new ArrayList<String>();
		
		try{
			Statement stmt = connection.createStatement();
			
			distinctAuthors = f.getAllDistinctAuthorsOrderedByName(connection,"allmessages"); //getAllDistinctAuthorsOrderedbyNumberPosts(connection);	//this we get from pep details table, odered by most posts author
			System.out.println("Distinct Authors Length "+distinctAuthors.size());
			//System.out.format("%50s%20s%10s%10s%10s%10s","Author","Seeding Posts","Null","I","S","P");
			System.out.println();
			//for each author, get the last date of post
			for (String au : distinctAuthors) 
			{																									//AND messageID < 100000   //AND folder = '" + list + "'
				String sql3 = "INSERT INTO memberPostCount (sendername, folder, postCount) select sendername, folder, count(distinct(messageid)) from allmessages where sendername = '"+au+"' 	group by sendername, folder;";  //there will be many rows since there is no seprate table to store this information
				PreparedStatement stmt3 = connection.prepareStatement(sql3);
				ResultSet rs3 = stmt3.executeQuery( sql3 );					    
						
				//System.out.format("%50s%20s%10s%10s%10s%10s",au+comma, seedingCount+comma,nullUniqueCount+comma,iUniqueCount+comma,sUniqueCount+comma,pUniqueCount);
	 		    System.out.println();			
				//System.out.println("totalDistinct Counter = "+totalDistinctCounter );
	 		    System.out.println("Done member = "+au );
			}
		}
		catch (SQLException e)	{
			System.out.println( e.getMessage() );
		}
		catch (Exception ex)	{
			System.out.println( ex.getMessage() );
		}
	}	
}
