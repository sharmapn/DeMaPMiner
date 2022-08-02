package rankingEvaluation;

import java.io.IOException;
import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Random;

import connections.MysqlConnect;
import miner.processLabels.TripleProcessingResult;

//Dec 2019. This is the main script. Trust me. Do all the changes for parameter values here. 
//dec 2019 make sure add memory size in eclipse - using run configurations for this script
//in arguments -> vm arguments add these 2 lines
//-Xms1024m
//-Xmx4096m

//RUN THIS After running the reason extraction and BEFORE Running this sript 'Run This BeforeEvaluation.sql'..which initialises the ORIG values as below
//UPDATE autoextractedreasoncandidatesentences SET origsentenceHintProbablity = sentenceHintProbablity;

//After running the dynamic parameter allocation, we will find the optimal parameter values.
//Once we have them we update the reasults table 'autoextractedreasoncandidatesentences' incrementing and decreasing each value based on out findings

//once we have have set the values, we can gradually remove one parameter after another to see if they affect the total number of top values matches
//UPDATE autoextractedreasoncandidatesentences SET sentenceHintProbablity 	= sentenceHintProbablity + INCREMENT/DECREMENT VALUE;

//Note change the level..'sentence' or 'sentencesfromdistinctmessages' in the 'Evaluation_MemoryBased.java'

//Note when checking feature importance, we change the value of 'startvalueForChecking' for the variable we checking
//so basically we just start at highre value

//Feb 2020..after we have assigned all different weights patterns and 
//found the weights which needs to be adjusted, we update them before getting the best results


public class ParameterSweeping_AndRankingEvaluation_Main {
	static Connection connection;
	static String tablenameToLoadResultsForEvaluation 			= "autoextractedreasoncandidatesentences",
				  tablenameToTransferResultsForEvaluation_Sent 	= "autoextractedreasoncandidatesentences_nodupsranked_sent_b2",
				  tablenameToTransferResultsForEvaluation_Msg 	= "autoextractedreasoncandidatesentences_nodupsranked_msg_b2";
	//Dec 2019
	//We carry out each step one by one, not all together. First it removes duplicates and last it assignsFinalWeights
	static Boolean 
					//remove duplicate sentences and messages in the results table "autoextractedreasoncandidatesentences" and the "autoextractedreasoncandidatemessages"
					//we only remove duplicates once ever, ideally
					removeDuplicates_Rank_TransferToNewTable = false, 
				    //this is where we compute the main processing occurs
					weightAllocation = false,   
					//remove variables one by one from total probability
					removeFeatures = false,		
				    //set this if we want to check feature importance so above value would be automatically set for each variable; //and true = weightAllocation
					trydifferentweights = false,
				    //compute ranking evaluation using optimised heuristic values
					assignFinalWeights = true;
	//static ArrayList<messageText> messageTextList = new ArrayList<messageText>(); //hold the text of messages as we need to keep this small so having unique ones help
	
	//static ArrayList<messageText> messageTextList = new ArrayList<messageText>(); //hold the text of messages as we need to keep this small so having unique ones help
	
	//Note scheme 1 = sentences based approach and scheme 2 = sentencesfromdistinctmessages
	
	//jan 2020..we only set and control this from here
	//main variable for all funcions
	static String evaluationLevels[] = {"sentences","sentencesfromdistinctmessages"}; //"sentences", 
	static boolean outputDetails = false;  //debug info
	
	public static void main(String[] args) throws SQLException, IOException {
		connections.MysqlConnect mc = new MysqlConnect();		connection = mc.connect();		
		long t0=0,t1=0, t2=0, t3 = 0, t4 = 0, t5=0, t6=0, t7=0, t8=0;
		
		ArrayList<ScoresToSentences> List_Sent = new ArrayList<ScoresToSentences>();  //This arrayList will hold all the table data sentence based scheme 
		ArrayList<ScoresToSentences> List_Msg = new ArrayList<ScoresToSentences>();  //This arrayList will hold all the table data for Message based on sentence based scheme 
		//we want to store unique text values in a hmap 
		HashMap<Integer,String> hmapSentences =new HashMap<Integer,String>(); 
		HashMap<Integer,String> hmapMessages =new HashMap<Integer,String>(); 	//this will hold the messages
		
		//empty tracking table
		emptyTrackingTables(connection); 
		
		//Jan 2020..Before processing we remove duplicates. 
		//The results are stored in different dataset -  as this new table will have duplicates removed
		//only needed done once
		if(removeDuplicates_Rank_TransferToNewTable)
		{
			//we process 50 peps at a time
			for(int i = 0; i < 4000; i += 50) { //50 peps at a time  ..will go until 49
				System.out.println(" i = " + i + ", (i + 50) = " + (i+50)); 
				
				t0 = System.currentTimeMillis(); 	System.out.println("\tSentences Transfer Start: "+ t0);
				removeDuplicates_Rank_TransferToNewTable(1, i, (i + 50) );  //pass the scheme we want to remove duplicates from and transfer to new table
				t1 = System.currentTimeMillis();	System.out.println("\tFinished Transferring Sentences: "+ t1 + " Time Taken: " +  (t1- t0)/1000 + " seconds"); 
				System.out.println("\tMessages Transfer Start: "+ t1);
				removeDuplicates_Rank_TransferToNewTable(2, i, (i + 50));
				t2 = System.currentTimeMillis(); System.out.println("\tFinished Transferring Array: "+ t2 + " Time Taken: " +  (t2- t1)/1000 + " seconds");
				
			}
		}
		
		//Populating and Sorting, Sorted by proposal and then by total score
		//done beforehand for most uses of this script
		for(String evalL : evaluationLevels) { 
			System.out.println("Evaluation Level: "+evalL);
			//Populate Array...main array of autoextracted records is populated here
			t3 = System.currentTimeMillis(); System.out.println("\tPopulate Array Start a: ");
			if(evalL.equals("sentences")) { 
				//Populate sentence based
				List_Sent = populateArray(List_Sent, hmapSentences,1);  //Populate sentence based array a
				t4 = System.currentTimeMillis();
				System.out.println("\tPopulated "+evalL+" Array, Time Taken: "+ (t4- t3)/1000 + " seconds, Sentence Array Size all data " + List_Sent.size() + " List Size hmapMessages " + hmapSentences.size());
				//Sort sentence based
				List_Sent = sortArrayList(List_Sent);  //SORTED BY proposal and then by total probability
				t5 = System.currentTimeMillis(); 
				System.out.println("\tFinished Sorting Array: "+ t5 + " Time Taken: " +  (t5- t4)/1000 + " seconds");					
			}
			else if(evalL.equals("sentencesfromdistinctmessages")) {
				//Populate message based				
				List_Msg  = populateArray(List_Msg, hmapMessages,2); //Populate message based on sentence based array
				t4 = System.currentTimeMillis();
				System.out.println("\t Populated "+evalL+" Array, Time Taken: "+ (t4- t3)/1000 + " seconds , Message Array Size all data " + List_Msg.size() + " List Size hmapMessages " + hmapMessages.size());
				//sort message based
				List_Msg = sortArrayList(List_Msg);  //SORTED BY proposal and then by total probability
				t5 = System.currentTimeMillis(); 
				System.out.println("\tFinished Sorting Array: "+ t5 + " Time Taken: " +  (t5- t4)/1000 + " seconds");				
			}
		}
		
		//Main Processing for Dynamic weight allocation here		
		//march 2019 - Dynamic weight allocation - The fields are updated in this order
		String fields[] = {"sentenceHintProbablity","sentenceLocationHintProbability","restOfParagraphProbabilityScore","messageLocationProbabilityScore","messageSubjectHintProbablityScore", //5
				"dateDiffProbability","authorRoleProbability","messageTypeIsReasonMessageProbabilityScore","sentenceLocationInMessageProbabilityScore","sameMsgSubAsStateTripleProbabilityScore", //10
				"reasonLabelFoundUsingTripleExtractionProbabilityScore","messageContainsSpecialTermProbabilityScore","prevParagraphSpecialTermProbabilityScore","negationTermPenalty"};
		
		//debug value or values for single loop, runCounterLimit = 1, incrementValue = 0.0, startValue = 0, startValue_fi = -1 (this is always -1)
		//try different weights, runCounterLimit = 2, incrementValue = 0.3, startValue = -1, startValue_fi = -1 (this is always -1)
		
		int runCounterLimit=3,      //run x times only for each field ..normally 7 times...but during debug and for single loop... we just keep it to 1
				loopCount=0;        //keep track of total loops executed
		double incrementValue= 0.3; //how much to increment each value each time, normally 0.3, and try -0.3 ..but for debug and single loop we keep it to 0.0
		
		//dec 2019 .. we have to try -ve values as well so easy way is to start loops at negative values
		//so, for (int incrementCounter11 = 0; becomes for (int incrementCounter11 = startValue; where startvalue = negative
		int startValue = -2; //-1 //used only in 'weightAllocation' loop. Its For loops in different weight allocation..Normally 0...but can be -1; // we will do 3 rounds of feeding in negative values ...try -3
		// we will skip for zero..i think it would be useless o try zero combinations
		
		int startValue_fi = -2;  //used only in 'featureImportanceChecking' loop. ... feature importance checking //if we are checking for variable importance, we put this value instead of the above one so we start at different value so the loop just runs once
										// can be set to 2 as well and -1 as well if we want to set negative values ..fi stands for feature importance
		
		//Main calculation of weights
		//This is done for one loop to get initial values
		//variable To Check List  = 0,1,2,3,4,5,6,7,8,9,10,11,12,13 // lets try 2 variables at first ,5,7,3,4};  
		if(weightAllocation) {  // ==true
			for(String evalLevel : evaluationLevels) { 
				System.out.println("Evaluation Level: "+evalLevel);
				//Populate Array...main array of autoextracted records is populated here
				t4 = System.currentTimeMillis(); System.out.println("\tPopulate Array Start b: ");
				if(evalLevel.equals("sentences")) { 				
					//Execute sentence based
					loopCount = initial_executionLoop(t5, List_Sent, hmapSentences, startValue, runCounterLimit, loopCount, incrementValue,evalLevel); 
					t6 = System.currentTimeMillis(); System.out.println("\tend processing ("+evalLevel+"): Elapsed time =" + (t6- t4) + " milliseconds");
					//System.out.println("\tNumber of Loops Executed: "+ loopCount);		
					//System.out.println("\tFinished Total processing  - Elapsed time =" + (t6- t4) + " milliseconds"); ///1000
					
					//List_Sent = null; hmapSentences = null; //we clear memory				
				}
				else if(evalLevel.equals("sentencesfromdistinctmessages")) {				
					//Execute message based
					loopCount = initial_executionLoop(t5, List_Msg, hmapMessages, startValue, runCounterLimit, loopCount, incrementValue,evalLevel); //increment values
					t7 = System.currentTimeMillis(); System.out.println("\tend processing ("+evalLevel+"): Elapsed time =" + (t7- t6) + " milliseconds");
					
					
					//List_Msg = null; hmapMessages = null; //we clear memory	
				}			
			}
		}		
		
		//Remove features that we dont need so that optimum values computation can be easier		
		if (removeFeatures) {
			System.out.println("\t Removing Features");
			for(String evalLevel : evaluationLevels) {
				if(evalLevel.equals("sentences")) { 	
					loopCount = executionLoop_removefeature(t5, List_Msg,List_Sent, hmapMessages,hmapSentences,  runCounterLimit, loopCount, incrementValue,startValue_fi, 1);
				}
				else if(evalLevel.equals("sentencesfromdistinctmessages")) {
					loopCount = executionLoop_removefeature(t5, List_Msg,List_Sent, hmapMessages,hmapSentences,  runCounterLimit, loopCount, incrementValue,startValue_fi, 2);
				}
			}
		}
		
		//Jan 2020..these are the fields in which order they are passed as variables and also inserted in the db 
		//Last stage..once we have found the a limited number of influential features we can compute optimum values for each variable, 
		//note: total probability is reset after we remove each variable as we test one at a time 
		// we increase and decrease each variables value to see if affects the baseline results 
		//incrementCounter0 - sentenceHintProbablity IncrementValue 		//incrementCounter1 - sentenceLocationHintProbability
		//incrementCounter2 - restOfParagraphProbabilityScore 				//incrementCounter3 - messageLocationProbabilityScore
		//incrementCounter4 - messageSubjectHintProbablityScore 			//incrementCounter5 - dateDiffProbability
		//incrementCounter6 - authorRoleProbability 						//incrementCounter7 - messageTypeIsReasonMessageProbabilityScore
		//incrementCounter8 - sentenceLocationInMessageProbabilityScore   - not used anymore
		//incrementCounter8 - sameMsgSubAsStateTripleProbabilityScore 		//incrementCounter9 - reasonLabelFoundUsingTripleExtractionProbabilityScore
		//incrementCounter10- messageContainsSpecialTermProbabilityScore 	//incrementCounter11- prevParagraphSpecialTermProbabilityScore
		//incrementCounter12- negationTermPenalty 
		
		//MAIN OPTIMISATION...through our analysis we have chosen the 6 influential features
		//we will just have to comment the loops other than those 
		//List<Integer> variableToCheckList = Arrays.asList(9,1,5,7,3,4); // lets try 2 variables at first ,5,7,3,4}; 
		//variable To Check List 9,0,5,7,3,4 //we only check some features not all as it would take a long time
		if (trydifferentweights) {
			System.out.println("\t Try Different Weights");
			/*
			for(String evalLevel : evaluationLevels) {
				if(evalLevel.equals("sentences")) { 
					loopCount = executionLoop_incrementdecrementvalues(t5, List_Msg,List_Sent,  hmapMessages, hmapSentences,  runCounterLimit, loopCount, incrementValue,startValue_fi,1,variableToCheckList); //increment values
				}
				else if(evalLevel.equals("sentencesfromdistinctmessages")) {
					loopCount = executionLoop_incrementdecrementvalues(t5, List_Msg,List_Sent,  hmapMessages, hmapSentences,  runCounterLimit, loopCount, incrementValue,startValue_fi,2,variableToCheckList); //in
				}
			}
			*/			
			for(String evalLevel : evaluationLevels) { 
				System.out.println("\tEvaluation Level: "+evalLevel);
				//Populate Array...main array of autoextracted records is populated here
				t4 = System.currentTimeMillis(); System.out.println("\tPopulate Array Start c: ");
				if(evalLevel.equals("sentences")) { 				
					//Execute sentence based
					loopCount = selectedFeatures_executionLoop(t5, List_Sent, hmapSentences, startValue, runCounterLimit, loopCount, incrementValue,evalLevel); 
					t6 = System.currentTimeMillis(); System.out.println("\tend processing ("+evalLevel+"): Elapsed time =" + (t6- t4) + " milliseconds");
					//System.out.println("\tNumber of Loops Executed: "+ loopCount);		
					//System.out.println("\tFinished Total processing  - Elapsed time =" + (t6- t4) + " milliseconds"); ///1000
					
					//List_Sent = null; hmapSentences = null; //we clear memory				
				}
				else if(evalLevel.equals("sentencesfromdistinctmessages")) {				
					//Execute message based
					loopCount = selectedFeatures_executionLoop(t5, List_Msg, hmapMessages,startValue, runCounterLimit, loopCount, incrementValue,evalLevel); //increment values
					t7 = System.currentTimeMillis(); System.out.println("\tend processing ("+evalLevel+"): Elapsed time =" + (t7- t6) + " milliseconds");
					//List_Msg = null; hmapMessages = null; //we clear memory	
				}			
			}
			
		}
		//the above shows that altering all variables values matter
		
		
		//feb 2020..now we have the final weight values, we assign that
		//for example we have found that increasing dd and y by +0.6, gives best results
		//FINAL Increment and decrement if chosen variables ..calls the 'RankingEvaluation_ParameterSweeping_MemoryBased.main function'
		if(assignFinalWeights) {
			System.out.println("\t Assign Final Weights");
			for(String evalLevel : evaluationLevels) { 
				System.out.println("\n #######  Assign Final Weights -  Beginning Evaluation Level: '"+evalLevel+"'");
				//Populate Array...main array of autoextracted records is populated here
				t4 = System.currentTimeMillis(); System.out.println("\tPopulate Array Start d: ");
				if(evalLevel.equals("sentences")) { 				
					//Execute sentence based, weights are updated here as well
					final_executionLoop(t5, List_Sent, hmapSentences, evalLevel); 
					t6 = System.currentTimeMillis(); System.out.println("\tend processing ('"+evalLevel+"'): Elapsed time =" + (t6- t4) + " milliseconds");
					//System.out.println("\tNumber of Loops Executed: "+ loopCount);		
					//System.out.println("\tFinished Total processing  - Elapsed time =" + (t6- t4) + " milliseconds"); ///1000
					
					//List_Sent = null; hmapSentences = null; //we clear memory				
				}
				else if(evalLevel.equals("sentencesfromdistinctmessages")) {				
					//Execute message based, weights are updated here as well
					final_executionLoop(t5, List_Msg, hmapMessages, evalLevel); //increment values
					t7 = System.currentTimeMillis(); System.out.println("\tend processing ('"+evalLevel+"'): Elapsed time =" + (t7- t6) + " milliseconds");
					//List_Msg = null; hmapMessages = null; //we clear memory	
				}			
			}
		}
		
		t7 = System.currentTimeMillis();
		System.out.println("Finished Total processing  - Elapsed time =" + (t7- t3) + " milliseconds"); ///1000
	}

	public static void emptyTrackingTables( Connection conn) throws SQLException {
		  String query = "DELETE FROM TRACKFOREACHROW;", query2 = "DELETE FROM TRACKFOREACHPROPOSAL;";
		  try {
		    PreparedStatement st = conn.prepareStatement(query);	// set all the preparedstatement parameters
		    PreparedStatement st2 = conn.prepareStatement(query2);	// set all the preparedstatement parameters
		    st.execute();	st2.execute();
		  } 
		  catch (SQLException se) {		    // log exception
		    throw se;
		  }
	}
	
	//for (String fieldValue: fields) {
	// One off script
//	String query = "ALTER TABLE " + tablename + " add column ORIG"+f+" DOUBLE"; // ALTER TABLE tablename add column origsentenceHintProbablity DOUBLE
//    PreparedStatement preparedStmt = connection.prepareStatement(query);
//    //preparedStmt.setInt   (1, 6000);		      preparedStmt.setString(2, "Fred");	 // execute the java preparedstatement
//    preparedStmt.executeUpdate();			
	/*
	String query2 = "UPDATE "+tablename+" SET orig"+f+" = "+f+ ";";
	PreparedStatement preparedStmt2 = connection.prepareStatement(query2);	
	preparedStmt2.executeUpdate(); System.out.println("Populated orig"+f+ " field value");
	runCounter++;			preparedStmt2.close();
	*/
	
	//dec 2019. we see which features matter by removing them from calculation by deducting from total probability
	//we play with the variable values from memory
	private static int executionLoop_removefeature(long t5, ArrayList<ScoresToSentences> List_Msg, ArrayList<ScoresToSentences> List_Sent,
			HashMap<Integer, String> hmapMessages,HashMap<Integer, String> hmapSentences, int runCounterLimit, int loopCount, double incrementValue, int startValue_fi, int scheme) 
			throws IOException, SQLException 
	{
		long t6;		long t7;		long t8;
		int incrementCounter=1;
		//Dec 2019.. I think this variable is least important thats why its first - but last incremented in loop
		
		//need this to pass for showing out..not used in computation
		double incrementValue1=0,incrementValue2=0,incrementValue3=0,incrementValue6=0, incrementValue7=0, 
				incrementValue4=0,incrementValue5=0, incrementValue8=0, incrementValue0=0,
				incrementValue9=0,incrementValue10=0,incrementValue11=0,incrementValue12=0,incrementValue13=0;
		///original and actual
		//for (int i = 0; i < 13; i++) {	//for each field, set the different weights
		//debug 13
		for (int i = 0; i < 13; i++) {	System.out.println("\t here b");
			t6 = System.currentTimeMillis(); 
			if(outputDetails)
				System.out.println("\tLast For Loop ended: "+ t6 + " Total Time for one loop =" + (t6- t5) + " milliseconds");
			
			t5 = t6;
			//reset weights, back to original
			//For sentence based
			if(scheme ==1) {
				List_Sent = resetWeightsBackToOriginal_fs(List_Sent); //increment value = 0 ..we dont really need to pass this value but its used so many times in the fn
				//we send which field we want to increment // uses the same incrementValue of the other loop for diff weight allocation
				List_Sent = removeSpecifiedField(List_Sent, i); //run counter limit has to be one as multiplication by zero gives zero
				//we try to use the same runcounterlimit variable but for this case where we only increment one variables value compared to all variables, we have to start it from 1
			}
			//for message based
			else if (scheme==2)	{
				//we may need to do this seprately as the probability values in the messsage list is a differet one
				List_Msg = resetWeightsBackToOriginal_fs(List_Msg); //increment value = 0 ..we dont really need to pass this value but its used so many times in the fn
				//we send which field we want to increment // uses the same incrementValue of the other loop for diff weight allocation
				List_Msg = removeSpecifiedField(List_Msg, i); //run counter limit has to be one as multiplication by zero gives zero
				//we try to use the same runcounterlimit variable but for this case where we only increment one variables value compared to all variables, we have to start it from 1
			}
			//increments values here is just for db inserting to show which fields' value we changed
			if (i==0) //just for inserting in db to show which value has been removed
				incrementValue0 =  -1;
			if (i==1) 
				incrementValue1 =  -1;
			if (i==2) 
				incrementValue2 =  -1;
			if (i==3) 
				incrementValue3 =  -1;
			if (i==4) 
				incrementValue4 =  -1;
			if (i==5) 
				incrementValue5 =  -1;
			if (i==6) 
				incrementValue6 =  -1;
			if (i==7) 
				incrementValue7 =  -1;
			if (i==8) 
				incrementValue8 =  -1;
			if (i==9) 
				incrementValue9 =  -1;
			if (i==10) 
				incrementValue10 =  -1;
			if (i==11) 
				incrementValue11 =  -1;
			if (i==12) 
				incrementValue12 =  -1;
			//if (i==13) //should not reach here
			//	incrementValue13 =  -1;
			
			t7 = System.currentTimeMillis();
			
//					List = incrementSpecifiedField(List,  incrementCounter13 * incrementValue, 13);
			//sentence based
			if(scheme == 1) {
				List_Sent = sortArrayList(List_Sent);  //Sort again after updating total probability. Note had been sorted BY proposal and then by total probability
				ParameterSweeping_MemoryBased.main(List_Sent,hmapSentences, incrementValue0,incrementValue1,	incrementValue2,incrementValue3,incrementValue4,incrementValue5,
					incrementValue6,incrementValue7,incrementValue8, incrementValue9,incrementValue10,incrementValue11,
					incrementValue12,incrementValue13, "sentences"); //Run the program which will compute vales and then will write the result to database
			}
			//System.out.println("\t\t\t I VALUE 2 = " +i + " incrementValue1: "+ incrementValue1);	
			//message based
			if(scheme == 2) {
				List_Msg = sortArrayList(List_Msg);  //Sort again after updating total probability. Note had been sorted BY proposal and then by total probability
				ParameterSweeping_MemoryBased.main(List_Msg,hmapMessages, incrementValue0,incrementValue1,	incrementValue2,incrementValue3,incrementValue4,incrementValue5,
					incrementValue6,incrementValue7,incrementValue8, incrementValue9,incrementValue10,incrementValue11,
					incrementValue12,incrementValue13, "sentencesfromdistinctmessages"); //Run the program which will compute vales and then will write the result to database
			}
			
			loopCount++; t8 = System.currentTimeMillis(); 
			System.out.println("Track: " + incrementCounter + " " +incrementCounter+ " " + incrementCounter+ " " +incrementCounter+ " " +incrementCounter+ " " +incrementCounter+ " " +
					incrementCounter+ " " + incrementCounter+ " " + incrementCounter+ " " +incrementCounter+ " " +incrementCounter+ " " +incrementCounter+ " " +
					incrementCounter+ " " + incrementCounter 
					+" Finished One process(Sort + Excecute), Time Taken: " + (t8- t7) + " milliseconds, " +  (t8- t7)/1000 + " seconds");
			//System.out.println("\t\t\t I VALUE 3 = " +i + " incrementValue1: "+ incrementValue1);
			//reset increment values
			incrementValue0=incrementValue1=incrementValue2=incrementValue3=incrementValue6=incrementValue7= 
			incrementValue4=incrementValue5=incrementValue8=incrementValue9=incrementValue10=incrementValue11=
			incrementValue12=0;
		}
		
		return loopCount;
	}
	
	//feature selection. we remove one feature at a time to see which features matter
	//march 2019 - we dont increment values for 0.0
	public static ArrayList <ScoresToSentences> removeSpecifiedField(ArrayList <ScoresToSentences> list, int fieldToRemove) throws IOException{
		ArrayList<Integer> al = new ArrayList<Integer>(); 
  		double sentenceHintProbablity=0.0,sentenceLocationHintProbability=0.0,messageSubjectHintProbablityScore=0.0, dateDiffProbability=0.0, authorRoleProbability=0.0, negationTermPenalty=0.0,
				   restOfParagraphProbabilityScore=0.0, messageLocationProbabilityScore=0.0,  messageTypeIsReasonMessageProbabilityScore=0.0,sentenceLocationInMessageProbabilityScore=0.0,sameMsgSubAsStateTripleProbabilityScore=0.0,
				   reasonLabelFoundUsingTripleExtractionProbabilityScore=0.0,messageContainsSpecialTermProbabilityScore=0.0,prevParagraphSpecialTermProbabilityScore=0.0;
    	try
  		{
  			boolean f = false, g= false, h=false;
	  		if(!list.isEmpty()){
	  			int counter =0;
	  			for (int x=0; x <list.size()-1; x++){  //for all records retrieved for the table
		  			
	  				//sentenceHintProbablity IncrementValue
	  				if(fieldToRemove==0) {	  					
	  					sentenceHintProbablity= list.get(x).getORIGsentenceHintProbablity();		//.toLowerCase()
		  				if(sentenceHintProbablity==0.0) {} //same as checking for null
		  				else {
		  					list.get(x).setSentenceOrParagraphHintProbablity(0.0); //sentenceHintProbablityIncrementValue);
		  					list.get(x).setTotalProbabilityScore( minus( list.get(x).getTotalProbabilityScore(), sentenceHintProbablity) );  
		  				}		  					
	  				}	  				
	  				//sentenceLocationHintProbability
	  				else if(fieldToRemove==1) {
	  					sentenceLocationHintProbability= list.get(x).getORIGsentenceLocationHintProbability();		//.toLowerCase()
		  				if(sentenceLocationHintProbability==0.0) {} //same as checking for null
		  				else {
		  					list.get(x).setSentenceLocationInMessageProbabilityScore(0.0); //sentenceLocationHintProbabilityIncrementValue);
		  					list.get(x).setTotalProbabilityScore(minus(list.get(x).getTotalProbabilityScore(), sentenceLocationHintProbability) );  
		  				}
	  				}
	  				//restOfParagraphProbabilityScore
	  				else if(fieldToRemove==2) {	 
	  					restOfParagraphProbabilityScore= list.get(x).getORIGrestOfParagraphProbabilityScore();		//.toLowerCase()
		  				if(restOfParagraphProbabilityScore==0.0) {} //same as checking for null
		  				else {
		  					list.get(x).setSentenceOrParagraphHintProbablity(0.0); //restOfParagraphProbabilityScoreIncrementValue);
		  					list.get(x).setTotalProbabilityScore(minus( list.get(x).getTotalProbabilityScore(),restOfParagraphProbabilityScore) );  
		  				}	  					
		  			}
		  			//messageLocationProbabilityScore
	  				else if(fieldToRemove==3) {	  					
	  					messageLocationProbabilityScore= list.get(x).getORIGmessageLocationProbabilityScore();		//.toLowerCase()
		  				if(messageLocationProbabilityScore==0.0) {} //same as checking for null
		  				else { 
		  					list.get(x).setSentenceOrParagraphHintProbablity(0.0); //messageLocationProbabilityScoreIncrementValue);
		  					list.get(x).setTotalProbabilityScore(minus( list.get(x).getTotalProbabilityScore(),messageLocationProbabilityScore) );  
		  				}	  					
		  			}
	  				//messageSubjectHintProbablityScore
	  				else if(fieldToRemove==4) {	  					
	  					messageSubjectHintProbablityScore= list.get(x).getORIGmessageSubjectHintProbablityScore();		//.toLowerCase()
		  				if(messageSubjectHintProbablityScore==0.0) {} //same as checking for null
		  				else {
		  					list.get(x).setSentenceOrParagraphHintProbablity(0.0); //sentenceHintProbablityIncrementValue);
		  					list.get(x).setTotalProbabilityScore( minus( list.get(x).getTotalProbabilityScore(),messageSubjectHintProbablityScore) );  
		  				}  					
	  				}
	  				//dateDiffProbability
	  				else if(fieldToRemove==5) {	  					
	  					dateDiffProbability= list.get(x).getORIGdateDiffProbability();		//.toLowerCase()
		  				if(dateDiffProbability==0.0) {} //same as checking for null
		  				else { 
		  					list.get(x).setDateDiffProbability(0.0); //dateDiffProbabilityIncrementValue);
		  					list.get(x).setTotalProbabilityScore( minus( list.get(x).getTotalProbabilityScore(),dateDiffProbability) );  
		  				}	  					
	  				}
	  				//authorRoleProbability
	  				else if(fieldToRemove==6) {	  					
	  					authorRoleProbability= list.get(x).getORIGauthorRoleProbability();		//.toLowerCase()
		  				if(authorRoleProbability==0.0) {} //same as checking for null
		  				else {
		  					list.get(x).setAuthorRoleProbability(0.0); //authorRoleProbabilityIncrementValue);
		  					list.get(x).setTotalProbabilityScore(minus( list.get(x).getTotalProbabilityScore(),authorRoleProbability) );  
		  				}	 	  					
	  				}	  				
	  				//messageTypeIsReasonMessageProbabilityScore
	  				else if(fieldToRemove==7) {	  					
	  					messageTypeIsReasonMessageProbabilityScore= list.get(x).getORIGmessageTypeIsReasonMessageProbabilityScore();		//.toLowerCase()
		  				if(messageTypeIsReasonMessageProbabilityScore==0.0) {} //same as checking for null
		  				else {
		  					list.get(x).setMesssageTypeIsReasonMessageProbabilityScore(0.0); //messageTypeIsReasonMessageProbabilityScoreIncrementValue);
		  					list.get(x).setTotalProbabilityScore(minus (list.get(x).getTotalProbabilityScore(),messageTypeIsReasonMessageProbabilityScore) ); 
		  				} 					
	  				}
	  				//sentenceLocationInMessageProbabilityScore
	  				/*Repeated ...else if(fieldToRemove==8) {	  					
	  					sentenceLocationInMessageProbabilityScore= list.get(x).getORIGsentenceLocationInMessageProbabilityScore();		//.toLowerCase()
		  				if(sentenceLocationInMessageProbabilityScore==0.0) {} //same as checking for null
		  				else {
		  					list.get(x).setSentenceLocationInMessageProbabilityScore(0.0);
		  					list.get(x).setTotalProbabilityScore(minus( list.get(x).getTotalProbabilityScore(),sentenceLocationInMessageProbabilityScore) );  
		  				}  					
		  			}
	  				*/
	  				//sameMsgSubAsStateTripleProbabilityScore
	  				else if(fieldToRemove==8) {	  					
	  					sameMsgSubAsStateTripleProbabilityScore= list.get(x).getORIGsameMsgSubAsStateTripleProbabilityScore();		//.toLowerCase()
		  				if(sameMsgSubAsStateTripleProbabilityScore==0.0) {} //same as checking for null
		  				else {
		  					list.get(x).setSameMsgSubAsStateTripleProbabilityScore(0.0); //sameMsgSubAsStateTripleProbabilityScoreIncrementValue);
		  					list.get(x).setTotalProbabilityScore( minus( list.get(x).getTotalProbabilityScore(),sameMsgSubAsStateTripleProbabilityScore) );  
		  				}	
		  			}
	  				//reasonLabelFoundUsingTripleExtractionProbabilityScore
	  				else if(fieldToRemove==9) {	  					
	  					reasonLabelFoundUsingTripleExtractionProbabilityScore= list.get(x).getORIGreasonLabelFoundUsingTripleExtractionProbabilityScore();		//.toLowerCase()
		  				if(reasonLabelFoundUsingTripleExtractionProbabilityScore==0.0) {} //same as checking for null
		  				else {
		  					list.get(x).setReasonLabelFoundUsingTripleExtractionProbabilityScore(0.0); //reasonLabelFoundUsingTripleExtractionProbabilityScoreIncrementValue);	  					
		  					list.get(x).setTotalProbabilityScore( minus( list.get(x).getTotalProbabilityScore(),reasonLabelFoundUsingTripleExtractionProbabilityScore) );  
		  				}
		  			}
	  				//messageContainsSpecialTermProbabilityScore
	  				else if(fieldToRemove==10) {
	  					messageContainsSpecialTermProbabilityScore= list.get(x).getORIGmessageContainsSpecialTermProbabilityScore();		//.toLowerCase()
		  				if(messageContainsSpecialTermProbabilityScore==0.0) {} //same as checking for null
		  				else {
		  					list.get(x).setMessageContainsSpecialTermProbabilityScore(0.0); //messageContainsSpecialTermProbabilityScoreIncrementValue);
		  					list.get(x).setTotalProbabilityScore( minus( list.get(x).getTotalProbabilityScore(),messageContainsSpecialTermProbabilityScore) );  
		  				}	  					
		  			}
	  				//prevParagraphSpecialTermProbabilityScore
	  				else if(fieldToRemove==11) {	  					
	  					prevParagraphSpecialTermProbabilityScore= list.get(x).getORIGprevParagraphSpecialTermProbabilityScore();		//.toLowerCase()
		  				if(prevParagraphSpecialTermProbabilityScore==0.0) {} //same as checking for null
		  				else {
		  					list.get(x).setPrevParagraphSpecialTermProbabilityScore(0.0); //prevParagraphSpecialTermProbabilityScoreIncrementValue);
		  					list.get(x).setTotalProbabilityScore( minus( list.get(x).getTotalProbabilityScore(),prevParagraphSpecialTermProbabilityScore) );  
	  					}	  					
	  				}
		  			//negationTermPenalty
	  				else if(fieldToRemove==12) {	  					
	  					negationTermPenalty= list.get(x).getORIGnegationTermPenalty();		//.toLowerCase()
		  				if(negationTermPenalty==0.0) {} //same as checking for null
		  				else {
		  					list.get(x).setNegationTermPenalty(0.0); //negationTermPenaltyIncrementValue);
		  					list.get(x).setTotalProbabilityScore(minus( list.get(x).getTotalProbabilityScore(),negationTermPenalty) );  
		  				}  					
		  			}
	  			}
	  	    }
  		}
  		catch(Exception e){
  			System.out.println("Exception " + e.toString());  			System.out.println(StackTraceToString(e)  );
  		}
  		return list;
    }
	
	static double minus(double a, double b) {
	    return a-b;
	}
	
	//dec 2019. First Try to get values ..we try all the different parameters .
	private static int initial_executionLoop(long t5, ArrayList<ScoresToSentences> List, HashMap<Integer, String> hmap, 
			int startValue, 		 //start from, for initial results we can set it to 0, else for increment and decrement for different weights we set it to -2 (so we can try from -2*0.3 =0.6)
			int runCounterLimit,     //limit to, for initial results we can set it to 1, else for increment and decrement for different weights we set it to 2 or 3 with startvalue = -2
			int loopCount,           //just to keep count
			double incrementValue,   //for initial results, whee one loop is required, this value is 0, else for increment and decrement for different weights we set it to -0.3 or -0.6 
			String evalLevel        //not needed as we do both schemes in the same loop
			//List<Integer> listFeaturesToChange
		) throws IOException, SQLException {
		
		long t6;		long t7;		long t8;
		
		//Jan 2020..these are the fields in which order they are passed as variables and also inserted in the db 
		// incrementCounter0  - sentenceHintProbablity, 			 					incrementCounter1  - sentenceLocationInMessageProbabilityScore
		// incrementCounter2  - restOfParagraphProbabilityScore		 					incrementCounter3  - messageLocationProbabilityScore 
		// incrementCounter4  - messageSubjectHintProbablityScore 	 					incrementCounter5  - dateDiffProbability
		// incrementCounter6  - authorRoleProbability 				 					incrementCounter7  - messageTypeIsReasonMessageProbabilityScore 
		// incrementCounter8  - sameMsgSubAsStateTripleProbabilityScore					incrementCounter9  - reasonLabelFoundUsingTripleExtractionProbabilityScore
		// incrementCounter10 - messageContainsSpecialTermProbabilityScore 				incrementCounter11 - prevParagraphSpecialTermProbabilityScore
		// incrementCounter12 - negationTermPenalty
		
		
		for (int incrementCounter11 = startValue; incrementCounter11 < runCounterLimit; incrementCounter11++) {	//for each field, get the different weights we have currently
			t6 = System.currentTimeMillis(); System.out.println("\tLast For Loop ended: "+ t6 + " Total Time for one loop =" + (t6- t5) + " milliseconds");
			t5 = t6;			
			List = incrementSpecifiedField(List, incrementCounter11 * incrementValue, 11);
			//DONT HAVE TO DO IT HERE...AT FINAL LOOP.. List = sortArrayList(List);  //Sort again after updating total probability. Note had been sorted BY proposal and then by total probability
			
			//this is always 0.8 for all records

//			  loop1visitedOnce=true; //increment always, but not the first instance
			
			  //sentenceLocationHintProbability			  
			  for (int incrementCounter1 = startValue; incrementCounter1 < runCounterLimit; incrementCounter1++) {				
			  List = incrementSpecifiedField(List, incrementCounter1 * incrementValue, 1);
	//			
				//restOfParagraphProbabilityScore
				for (int incrementCounter2 = startValue; incrementCounter2 < runCounterLimit; incrementCounter2++) {
					List = incrementSpecifiedField(List, incrementCounter2 * incrementValue, 2);
					
					//messageLocationProbabilityScore
					for (int incrementCounter3 = startValue; incrementCounter3< runCounterLimit; incrementCounter3++) { 
						List = incrementSpecifiedField(List, incrementCounter3*incrementValue, 3);
						
						//messageSubjectHintProbablityScore
						for (int incrementCounter4 = startValue; incrementCounter4< runCounterLimit; incrementCounter4++) {
							List = incrementSpecifiedField(List,  incrementCounter4*incrementValue, 4);
							
							//dateDiffProbability
							for (int incrementCounter5 = startValue; incrementCounter5< runCounterLimit; incrementCounter5++) {
								List = incrementSpecifiedField(List, incrementCounter5*incrementValue, 5);
								
								//authorRoleProbability
								for (int incrementCounter6 = startValue; incrementCounter6< runCounterLimit; incrementCounter6++) {
									List = incrementSpecifiedField(List, incrementCounter6*incrementValue, 6);
									
									//messageTypeIsReasonMessageProbabilityScore
									for (int incrementCounter7 = startValue; incrementCounter7< runCounterLimit; incrementCounter7++) {
										List = incrementSpecifiedField(List, incrementCounter7*incrementValue, 7);
										
										//changed sameMsgSubAsStateTripleProbabilityScore //not longer .. sentenceLocationInMessageProbabilityScore										
										for (int incrementCounter8 = startValue; incrementCounter8< runCounterLimit; incrementCounter8++) { 
											List = incrementSpecifiedField(List, incrementCounter8*incrementValue, 8);
											
											//reasonLabelFoundUsingTripleExtractionProbabilityScore .. no longer sameMsgSubAsStateTripleProbabilityScore											
											for (int incrementCounter9 = startValue; incrementCounter9< runCounterLimit; incrementCounter9++) {
												List = incrementSpecifiedField(List, incrementCounter9*incrementValue, 9);
												System.out.println("\t HERE 9, startValue: "+ startValue + ", runCounterLimit "+ runCounterLimit);
												//messageContainsSpecialTermProbabilityScore ..no longer reasonLabelFoundUsingTripleExtractionProbabilityScore												
												for (int incrementCounter10 = startValue; incrementCounter10< runCounterLimit; incrementCounter10++){  
													List = incrementSpecifiedField(List, incrementCounter10*incrementValue, 10);
													
													//sentenceHintProbablity -- CHANGED TO SEE THIS value CHANGING Total probability REGULARLY													
													for (int incrementCounter0 = startValue; incrementCounter0< runCounterLimit; incrementCounter0++) { 
														List = incrementSpecifiedField(List, incrementCounter0 * incrementValue, 0);
														//DONT HAVE TO DO IT HERE...AT FINAL LOOP.. List = sortArrayList(List);  //Sort again after updating total probability. Note had been sorted BY proposal and then by total probability
														
														//prevParagraphSpecialTermProbabilityScore														
														for (int incrementCounter12 = startValue; incrementCounter12< runCounterLimit; incrementCounter12++) {
															List = incrementSpecifiedField(List,  incrementCounter12 * incrementValue, 12);
															
															//negationTermPenalty
															for (int incrementCounter13 = startValue; incrementCounter13< runCounterLimit; incrementCounter13++){ 
																t7 = System.currentTimeMillis();
																//System.out.println("For Loop 13 for  increment counter: "+ incrementCounter13);
																
																//for last loop we allow so that the increment value here can be reset to 0.0
																// // never used assignWeights(fields[1], incrementCounter1); //loop9=true;   CHANGED 13 to 1
																
																List = incrementSpecifiedField(List,  incrementCounter13 * incrementValue, 13);
																List = sortArrayList(List);  //Sort again after updating total probability. Note had been sorted BY proposal and then by total probability
																ParameterSweeping_MemoryBased.main(List, hmap, incrementCounter0*incrementValue,incrementCounter1*incrementValue,incrementCounter2*incrementValue,
																		incrementCounter3*incrementValue,incrementCounter4*incrementValue,incrementCounter5*incrementValue,
																		incrementCounter6*incrementValue,incrementCounter7*incrementValue,incrementCounter8*incrementValue,
																		incrementCounter9*incrementValue,incrementCounter10*incrementValue,incrementCounter11*incrementValue,
																		incrementCounter12*incrementValue,incrementCounter13*incrementValue, evalLevel  ); //Run the program which will compute vales and then will write the result to database
																
																loopCount++; t8 = System.currentTimeMillis(); 
																System.out.println("Track: " + incrementCounter0 + " " +incrementCounter1+ " " + incrementCounter2+ " " +incrementCounter3+ " " +incrementCounter4+ " " +incrementCounter5+ " " +
																		incrementCounter6+ " " + incrementCounter7+ " " + incrementCounter8+ " " +incrementCounter9+ " " +incrementCounter10+ " " +incrementCounter11+ " " +
																		incrementCounter12+ " " + incrementCounter13 
																		+" Finished One process(Sort + Excecute), Time Taken: " + (t8- t7) + " milliseconds, " +  (t8- t7)/1000 + " seconds");
															} //13
														} //12
													} //11
												} //10  reasonLabelFoundUsingTripleExtractionProbabilityScore
											} //09  sameMsgSubAsStateTripleProbabilityScore
										} //08
									} //07  messageTypeIsReasonMessageProbabilityScore
								} //06 authorRoleProbability
							} //5
						} //4 
					} //3 messageLocationProbabilityScore
				} //2 restOfParagraphProbabilityScore
			} //1
		} //0
		return loopCount;
	}
		
	//Once we have influential features then to get optimised values we will check which features dont matter at all 		
	//March 2019. Most influenctial fields put last so effect can be seen regularly rather than put in first - which would be exeutred last
	//Dec 2019. Note we have to increment variable values in both lists
	//variable sent .. t5, List_Sent, hmapSentences, startValue, runCounterLimit, loopCount, incrementValue,evalLevel,variableToCheckList
	private static int selectedFeatures_executionLoop(long t5, ArrayList<ScoresToSentences> List, HashMap<Integer, String> hmap, 
			int startValue, 		 //start from, for initial results we can set it to 0, else for increment and decrement for different weights we set it to -2 (so we can try from -2*0.3 =0.6)
			int runCounterLimit,     //limit to, for initial results we can set it to 1, else for increment and decrement for different weights we set it to 2 or 3 with startvalue = -2
			int loopCount,           //just to keep count
			double incrementValue,   //for initial results, whee one loop is required, this value is 0, else for increment and decrement for different weights we set it to -0.3 or -0.6 
			String evalLevel        //not needed as we do both schemes in the same loop
			//List<Integer> listFeaturesToChange
		) throws IOException, SQLException {
		
		long t6;		long t7;		long t8;
		
		//Jan 2020..these are the fields in which order they are passed as variables and also inserted in the db 
		// incrementCounter0  - sentenceHintProbablity, 			 					incrementCounter1  - sentenceLocationInMessageProbabilityScore
		// incrementCounter2  - restOfParagraphProbabilityScore		 					incrementCounter3  - messageLocationProbabilityScore 
		// incrementCounter4  - messageSubjectHintProbablityScore 	 					incrementCounter5  - dateDiffProbability
		// incrementCounter6  - authorRoleProbability 				 					incrementCounter7  - messageTypeIsReasonMessageProbabilityScore 
		// incrementCounter8  - sameMsgSubAsStateTripleProbabilityScore					incrementCounter9  - reasonLabelFoundUsingTripleExtractionProbabilityScore
		// incrementCounter10 - messageContainsSpecialTermProbabilityScore 				incrementCounter11 - prevParagraphSpecialTermProbabilityScore
		// incrementCounter12 - negationTermPenalty
		
		//since we have removd these loops, lets initialise them as tehy are needed to be passed in function
		
		Integer incrementCounter1=0,incrementCounter2=0,incrementCounter6=0,incrementCounter8=0,incrementCounter10=0,incrementCounter11=0,incrementCounter12=0,incrementCounter13=0;
			t7 = System.currentTimeMillis();
			//messageLocationProbabilityScore
			for (int incrementCounter3 = startValue; incrementCounter3< runCounterLimit; incrementCounter3++) { 
				List = incrementSpecifiedField(List, incrementCounter3*incrementValue, 3);
				
				//messageSubjectHintProbablityScore
				for (int incrementCounter4 = startValue; incrementCounter4< runCounterLimit; incrementCounter4++) {
					List = incrementSpecifiedField(List,  incrementCounter4*incrementValue, 4);
					
					//dateDiffProbability
					for (int incrementCounter5 = startValue; incrementCounter5< runCounterLimit; incrementCounter5++) {
						List = incrementSpecifiedField(List, incrementCounter5*incrementValue, 5);
							
							//messageTypeIsReasonMessageProbabilityScore
							for (int incrementCounter7 = startValue; incrementCounter7< runCounterLimit; incrementCounter7++) {
								List = incrementSpecifiedField(List, incrementCounter7*incrementValue, 7);
									
									//reasonLabelFoundUsingTripleExtractionProbabilityScore .. no longer sameMsgSubAsStateTripleProbabilityScore											
									for (int incrementCounter9 = startValue; incrementCounter9< runCounterLimit; incrementCounter9++) {
										List = incrementSpecifiedField(List, incrementCounter9*incrementValue, 9);
										//System.out.println("\t HERE 9, startValue: "+ startValue + ", runCounterLimit "+ runCounterLimit);										
											
											//sentenceHintProbablity -- CHANGED TO SEE THIS value CHANGING Total probability REGULARLY													
											for (int incrementCounter0 = startValue; incrementCounter0< runCounterLimit; incrementCounter0++) { 
												List = incrementSpecifiedField(List, incrementCounter0 * incrementValue, 0);
												//DONT HAVE TO DO IT HERE...AT FINAL LOOP.. List = sortArrayList(List);  //Sort again after updating total probability. Note had been sorted BY proposal and then by total probability
																								
														//System.out.println("For Loop 13 for  increment counter: "+ incrementCounter13);
														
														//for last loop we allow so that the increment value here can be reset to 0.0
														// // never used assignWeights(fields[1], incrementCounter1); //loop9=true;   CHANGED 13 to 1
														
														List = incrementSpecifiedField(List,  incrementCounter13 * incrementValue, 13);
														List = sortArrayList(List);  //Sort again after updating total probability. Note had been sorted BY proposal and then by total probability
														ParameterSweeping_MemoryBased.main(List, hmap, incrementCounter0*incrementValue,incrementCounter1*incrementValue,incrementCounter2*incrementValue,
																incrementCounter3*incrementValue,incrementCounter4*incrementValue,incrementCounter5*incrementValue,
																incrementCounter6*incrementValue,incrementCounter7*incrementValue,incrementCounter8*incrementValue,
																incrementCounter9*incrementValue,incrementCounter10*incrementValue,incrementCounter11*incrementValue,
																incrementCounter12*incrementValue,incrementCounter13*incrementValue, evalLevel  ); //Run the program which will compute vales and then will write the result to database
														
														loopCount++; t8 = System.currentTimeMillis(); 
														System.out.println("Track: " + incrementCounter0 + " " +incrementCounter1+ " " + incrementCounter2+ " " +incrementCounter3+ " " +incrementCounter4+ " " +incrementCounter5+ " " +
																incrementCounter6+ " " + incrementCounter7+ " " + incrementCounter8+ " " +incrementCounter9+ " " +incrementCounter10+ " " +incrementCounter11+ " " +
																incrementCounter12+ " " + incrementCounter13 
																+" Finished One process(Sort + Excecute), Time Taken: " + (t8- t7) + " milliseconds, " +  (t8- t7)/1000 + " seconds");
																									
											} //09  sameMsgSubAsStateTripleProbabilityScore										
									} //07  messageTypeIsReasonMessageProbabilityScore								
							} //5
						} //4 
					} //3 messageLocationProbabilityScore
		} //0
		return loopCount;
	}
	
	//feb 2020..FINAL STEP...once we know which selected variables values need to be incremented or decremented
	//for both message abd sentence based: datediff and mshp needs to be incremented by 0.6
	//additionally for sentence based (both accepted anbd rejected), shp needs to be incremented by 0.6 
	//for sentence - accepted the mshp needs to be either 0 and -0.3 for best results 
	
	//so we will run these results for sentence and message based differently
	
	//runCounterLimit = 1
	//startValue 
	private static void final_executionLoop(long t5, ArrayList<ScoresToSentences> List, HashMap<Integer, String> hmap, 
			//int startValue, 		 //start from, for initial results we can set it to 0, else for increment and decrement for different weights we set it to -2 (so we can try from -2*0.3 =0.6)
			//int runCounterLimit,     //limit to, for initial results we can set it to 1, else for increment and decrement for different weights we set it to 2 or 3 with startvalue = -2
			//int loopCount,           //just to keep count
			//double incrementValue,   //for initial results, whee one loop is required, this value is 0, else for increment and decrement for different weights we set it to -0.3 or -0.6 
			String evalLevel        //not needed as we do both schemes in the same loop
			//List<Integer> listFeaturesToChange
		) throws IOException, SQLException {
		
		long t6;		long t7;		long t8;
		
		//Jan 2020..these are the fields in which order they are passed as variables and also inserted in the db 
		// incrementCounter0  - sentenceHintProbablity, 			 					incrementCounter1  - sentenceLocationInMessageProbabilityScore
		// incrementCounter2  - restOfParagraphProbabilityScore		 					incrementCounter3  - messageLocationProbabilityScore 
		// incrementCounter4  - messageSubjectHintProbablityScore 	 					incrementCounter5  - dateDiffProbability
		// incrementCounter6  - authorRoleProbability 				 					incrementCounter7  - messageTypeIsReasonMessageProbabilityScore 
		// incrementCounter8  - sameMsgSubAsStateTripleProbabilityScore					incrementCounter9  - reasonLabelFoundUsingTripleExtractionProbabilityScore
		// incrementCounter10 - messageContainsSpecialTermProbabilityScore 				incrementCounter11 - prevParagraphSpecialTermProbabilityScore
		// incrementCounter12 - negationTermPenalty
			
		//double incrementValue1=0,incrementValue2=0,incrementValue3=0,incrementValue6=0, incrementValue7=0, 
		//		incrementValue4=0,incrementValue5=0, incrementValue8=0, incrementValue0=0,
		//		incrementValue9=0,incrementValue10=0,incrementValue11=0,incrementValue12=0,incrementValue13=0;
		
		//int incrementCounter1=0,incrementCounter2=0,incrementCounter3=0,incrementCounter6=0, incrementCounter7=0, 
		//		incrementCounter4=0,incrementCounter5=0, incrementCounter8=0, incrementCounter0=0,
		//		incrementCounter9=0,incrementCounter10=0,incrementCounter11=0,incrementCounter12=0,incrementCounter13=0;
		
		
		t7 = System.currentTimeMillis();
		
		//Sentence based
		if(evalLevel.equals("sentences"))   
		{	
			//System.out.println(" evalLevel equals sentences");
			List = incrementSpecifiedField(List,  0.6, 0); //shp	
			List = incrementSpecifiedField(List,  0.6, 5); //ddp
			List = sortArrayList(List);  //Sort again after updating total probability. Note had been sorted BY proposal and then by total probability
			ParameterSweeping_MemoryBased.main(List, hmap, 0,0,0,0,
					0.6,0.6,  //4 and 5 increased by 0.6 before - we just pass these values for db storage
					0,0,0,0,0,0,0,0, evalLevel  ); //Run the program which will compute vales and then will write the result to database
		}
		
		//Message based
		else if(evalLevel.equals("sentencesfromdistinctmessages"))   
		{	
			//System.out.println(" evalLevel equals sentencesfromdistinctmessages");
			List = incrementSpecifiedField(List,  0.6, 4); //mshp
			List = incrementSpecifiedField(List,  0.6, 5); //ddp					
			List = sortArrayList(List);  //Sort again after updating total probability. Note had been sorted BY proposal and then by total probability
			ParameterSweeping_MemoryBased.main(List, hmap, 
					0.6,  // 0 shp
					0,0,0,0,
					0.6,  // 5 ddp
					0,0,0,0,0,0,0,0, evalLevel  ); //Run the program which will compute vales and then will write the result to database
		}
		t8 = System.currentTimeMillis(); 
		System.out.println(" Finished One process(Sort + Excecute), Time Taken: " + (t8- t7) + " milliseconds, " +  (t8- t7)/1000 + " seconds");

	}
	
	
	public static ArrayList<ScoresToSentences> populateArray(ArrayList<ScoresToSentences> List, HashMap<Integer,String> hmapMessages, Integer scheme){// ArrayList<messageText> messageTextList) {
		System.out.println("\tPopulating ArrayList for "+scheme+" scheme:  ");
			
		//ArrayList<Integer> proposalsInResults = new ArrayList<Integer>(); 
		ArrayList<Integer> distinctPEPsInResults = new ArrayList<Integer>();   // we hold all different PEPs here which we find in the results so we can read data in chunks
		
		try {
			distinctPEPsInResults= getDistinctProposalsInResults(distinctPEPsInResults);
			int count=0;
			String sentenceOrParagraph = "", mid = "", dateVal = "", termsMatched = "", level = "", folderML = "",	author = "";
			Integer proposalNum= null, messageID=null, ranking=1,id=0, datediff=0,anyRanking=1,rowCount=0,listOfMessageIDsForRankingCounter=1,calculatedRanking=0; 
			double sentenceHintProbablity=0.0,/*sentenceLocationHintProbability=0.0, */ messageSubjectHintProbablityScore=0.0, dateDiffProbability=0.0, authorRoleProbability=0.0, negationTermPenalty=0.0,
					   restOfParagraphProbabilityScore=0.0, messageLocationProbabilityScore=0.0, 
					   messageTypeIsReasonMessageProbabilityScore=0.0,sentenceLocationInMessageProbabilityScore=0.0,sameMsgSubAsStateTripleProbabilityScore=0.0,
					   reasonLabelFoundUsingTripleExtractionProbabilityScore=0.0,messageContainsSpecialTermProbabilityScore=0.0,prevParagraphSpecialTermProbabilityScore=0.0,
					   //ORIG Values
					   ORIGsentenceHintProbablity=0.0,ORIGsentenceLocationHintProbability=0.0,ORIGmessageSubjectHintProbablityScore=0.0, ORIGdateDiffProbability=0.0, ORIGauthorRoleProbability=0.0, 
					   ORIGnegationTermPenalty=0.0, ORIGrestOfParagraphProbabilityScore=0.0, ORIGmessageLocationProbabilityScore=0.0, 
					   ORIGmessageTypeIsReasonMessageProbabilityScore=0.0,ORIGsentenceLocationInMessageProbabilityScore=0.0,ORIGsameMsgSubAsStateTripleProbabilityScore=0.0,
					   ORIGreasonLabelFoundUsingTripleExtractionProbabilityScore=0.0,ORIGmessageContainsSpecialTermProbabilityScore=0.0,ORIGprevParagraphSpecialTermProbabilityScore=0.0;
			String autosentenceOrParagraphOrMessage="",email="",autolabel="",loc="",probability="";
		
			boolean messageTextFound = false;
						
			String query1 = ""; ResultSet rs1 =null; PreparedStatement ps1 = null; 
			ArrayList<String> labelsForProposal = new ArrayList<String>(); 
			
			//main reason is to eliminate 	duplicate reason sentences for each pep we need to see that we dont 
			//for each proposal ... we read data in chunks ...
			for (Integer p : distinctPEPsInResults)
			{	
				//Jan 2020 debug
//				if (p!= 391) 
//					continue;	
				labelsForProposal.clear();
				labelsForProposal = getLabelsForProposal(p);
				if(outputDetails)
					System.out.println("\tProcessing Proposal # In Results for "+scheme+" scheme " + p );
				
				//for each proposal, we get results for the label
				for (String lab : labelsForProposal)
				{	//System.out.println("lab " + lab);
					
					if(scheme==1) {  //sentence based
						//System.out.println("Populating SentenceBased ArrayList :  ");
						//Sentence Based		
						/* we have already removed duplicates
						query1 =  " SELECT *, @curRank := @curRank + 1 AS rank,messageTypeIsReasonMessage as RM,messageType as MT, proposal,messageid,dateValue,datediff, sentence as text,termsMatched, " 
								+ " (sentenceHintProbablity+sentenceLocationHintProbability+restOfParagraphProbabilityScore+messageLocationProbabilityScore+messageSubjectHintProbablityScore+ " 
								+ " dateDiffProbability+authorRoleProbability+messageTypeIsReasonMessageProbabilityScore+sentenceLocationInMessageProbabilityScore+sameMsgSubAsStateTripleProbabilityScore+ " 
								+ " reasonLabelFoundUsingTripleExtractionProbabilityScore+messageContainsSpecialTermProbabilityScore+prevParagraphSpecialTermProbabilityScore-negationTermPenalty) as TotalProbability "  
								+ " FROM "+tablenameToLoadResultsForEvaluation+", (SELECT @curRank := 0) r  " 
								+ " WHERE proposal = " + p +" and label = '"+lab+"' "
								+ " AND location = 'sentence' "
								+ " GROUP by sentence " // messageid,  -- jan 2020 ... this is necesary to remove duplicate sentences"
								+ " ORDER by TotalProbability desc, dateValue desc; ";			//we just add this but this is taken care of later in the code
						*/	
						query1 = "SELECT id, @curRank := @curRank + 1 AS rank, proposal,messageid,dateValue, sentence as text,termsMatched, probability, label, authorRole, location,datediff, messageTypeIsReasonMessage as RM, messageType as MT, "
								+ "  (sentenceHintProbablity+sentenceLocationHintProbability+restOfParagraphProbabilityScore+messageLocationProbabilityScore+messageSubjectHintProbablityScore+dateDiffProbability+authorRoleProbability+"
								+ "  +messageTypeIsReasonMessageProbabilityScore+sentenceLocationInMessageProbabilityScore+sameMsgSubAsStateTripleProbabilityScore"
								+ "	 +reasonLabelFoundUsingTripleExtractionProbabilityScore+messageContainsSpecialTermProbabilityScore+prevParagraphSpecialTermProbabilityScore-negationTermPenalty) as TotalProbability, "
								//+ "  sentenceHintProbablity,sentenceLocationHintProbability,messageSubjectHintProbablityScore,negationTermPenalty,dateDiffProbability,authorRoleProbability,restOfParagraphProbabilityScore "
								+ "  sentenceHintProbablity,sentenceLocationHintProbability,restOfParagraphProbabilityScore,messageLocationProbabilityScore,messageSubjectHintProbablityScore,dateDiffProbability,authorRoleProbability, "  
								+ "  messageTypeIsReasonMessageProbabilityScore,sentenceLocationInMessageProbabilityScore,sameMsgSubAsStateTripleProbabilityScore, " 
								+ "  reasonLabelFoundUsingTripleExtractionProbabilityScore,messageContainsSpecialTermProbabilityScore,prevParagraphSpecialTermProbabilityScore,negationTermPenalty, "
								
								+ "  ORIGsentenceHintProbablity, ORIGmessageSubjectHintProbablityScore, ORIGsentenceLocationHintProbability, ORIGdateDiffProbability, "  
								+ "  ORIGnegationTermPenalty,ORIGauthorRoleProbability,ORIGrestOfParagraphProbabilityScore, ORIGmessageLocationProbabilityScore, " 
								+ "  ORIGmessageTypeIsReasonMessageProbabilityScore, ORIGsentenceLocationInMessageProbabilityScore, " 
								+ "  ORIGsameMsgSubAsStateTripleProbabilityScore, ORIGreasonLabelFoundUsingTripleExtractionProbabilityScore, "  
								+ "  ORIGmessageContainsSpecialTermProbabilityScore, ORIGprevParagraphSpecialTermProbabilityScore "
								
								+ "  FROM " +tablenameToTransferResultsForEvaluation_Sent+ ", (SELECT @curRank := 0) r  "
								+ "  WHERE location = 'sentence' " 
								+ "  AND proposal = " + p +" AND label = '"+lab+"' "
								+ "  order by proposal, label asc, TotalProbability desc, dateValue desc; ";	
					}
					// we dont call this while populating new table/..this is not needed
					else if(scheme==2) {  //message based
						/* we have already removed duplicates
						query1 =" SELECT * FROM " 
								+ " ( " 
									+ " SELECT *, (select analysewords from allmessages w where w.messageid = x.messageid limit 1) as text from "+tablenameToLoadResultsForEvaluation+" x "
								    + " WHERE x.proposal = " + p +" and x.label = '"+lab+"'"
								    + " GROUP BY sentence "	//-- jan 2020 ... this is necesary to remove duplicate sentences " 
								    + " order by label asc, TotalProbability desc, dateValue desc "
								+ " ) as messages "  
								+ " GROUP BY messages.messageid " 
								+ " ORDER BY messages.label asc, messages.TotalProbability desc, messages.dateValue desc;";
						*/
						query1 = " SELECT *, (select analysewords from allmessages w where w.messageid = x.messageid limit 1) as text from " +tablenameToTransferResultsForEvaluation_Msg+ " x"
							   + " WHERE proposal = " + p +" and label = '"+lab+"' "+ ";";
					}
					
					ps1 = connection.prepareStatement(query1);
					//ps.setString(0, f);	 //System.out.println("f "+f);
					// process the results
					rs1 = ps1.executeQuery();
					
					sentenceOrParagraph = mid = dateVal = termsMatched = level = folderML = author = "";
					proposalNum= messageID=null;
					ranking=1; id=datediff=0; anyRanking=1; rowCount=0; listOfMessageIDsForRankingCounter=1; calculatedRanking=0; 
					/*sentenceLocationHintProbability=0.0, */ 
					sentenceHintProbablity=messageSubjectHintProbablityScore=dateDiffProbability=authorRoleProbability= negationTermPenalty= 
							   restOfParagraphProbabilityScore=messageLocationProbabilityScore= 
							   messageTypeIsReasonMessageProbabilityScore=sentenceLocationInMessageProbabilityScore=sameMsgSubAsStateTripleProbabilityScore=
							   reasonLabelFoundUsingTripleExtractionProbabilityScore=messageContainsSpecialTermProbabilityScore=prevParagraphSpecialTermProbabilityScore=
							   //ORIG Values
							   ORIGsentenceHintProbablity=ORIGsentenceLocationHintProbability=ORIGmessageSubjectHintProbablityScore=ORIGdateDiffProbability=ORIGauthorRoleProbability= 
							   ORIGnegationTermPenalty=ORIGrestOfParagraphProbabilityScore= ORIGmessageLocationProbabilityScore= 
							   ORIGmessageTypeIsReasonMessageProbabilityScore=ORIGsentenceLocationInMessageProbabilityScore=ORIGsameMsgSubAsStateTripleProbabilityScore=
							   ORIGreasonLabelFoundUsingTripleExtractionProbabilityScore=ORIGmessageContainsSpecialTermProbabilityScore=ORIGprevParagraphSpecialTermProbabilityScore=0.0;
					autosentenceOrParagraphOrMessage=email=autolabel=loc=probability="";
					messageTextFound = false;
										
					//all results for that proposal and for that label
					while ( rs1.next() )    { //System.out.println("f "+rs.getDouble(f));
						//val = rs.getDouble(f);
						//then for different weight in that field we increment by 0.1 using SQL
						//for the moment just double the values		
						
						id = rs1.getInt("id");		datediff = rs1.getInt("datediff");
		//				anyRanking = rs.getInt("rank");       //System.out.print("\t ranking "+ anyRanking); 		
						proposalNum = rs1.getInt("proposal");						mid = rs1.getString("messageid");				dateVal = rs1.getString("dateValue");
//						System.out.println("mid "+mid);
						//march 2019, storing all text in the list overuns memory, so we store only the unique ones in seprate list
						messageTextFound = false;
						/*for (messageText m : messageTextList) {
							if(m.getMessageID()==Integer.valueOf(mid)) {
								messageTextFound = true;
							}
						}
						if (!messageTextFound) {
							messageText mt = new messageText(Integer.valueOf(mid), rs.getString("text"));
							messageTextList.add(mt);
						} */
		//				HashMap<Integer,String> hmapMessages
						
						
						//jan 2020
						//we only do this for message based, we cant do this for sentence based as a message can have multiple sentences in results to which we cannot assign an id as we did for messages
						//if(hmapMessages.get(Integer.valueOf(mid)) == null) //check if mesage id is already inserted
						//hmapMessages.put(Integer.valueOf(mid),rs1.getString("text"));
						
						if(scheme.equals(1) ) { //sentence based scheme
							autosentenceOrParagraphOrMessage = rs1.getString("text");	
						}
						else if(scheme.equals(2)) {	//message based scheme
							if(hmapMessages.get(Integer.valueOf(mid)) == null) //check if mesage id is already inserted
								hmapMessages.put(Integer.valueOf(mid),rs1.getString("text"));
							//else
							//	continue;  //else we dont insert - only for message based
							autosentenceOrParagraphOrMessage = "";  //set this to empty
						}
						//jan 2020 commented
						//autosentenceOrParagraphOrMessage = ""; //rs.getString("text");	
						termsMatched = rs1.getString("termsMatched");	probability = rs1.getString("TotalProbability");
						autolabel = rs1.getString("label");					author = rs1.getString("authorRole");					loc = rs1.getString("location");
		//				email = rsA.getString("email"); 
						//System.out.println("SQL Ranking " + anyRanking + " mid " + mid + " totalprobability "+probability );
						//probabilities
						//if(evalLevel.equals("sentences") || evalLevel.equals("sentencesfromdistinctmessages") ) {
							sentenceHintProbablity	= rs1.getDouble("sentenceHintProbablity");							//sentenceLocationHintProbability = rs.getDouble("sentenceLocationHintProbability");
							messageSubjectHintProbablityScore	= rs1.getDouble("messageSubjectHintProbablityScore");	dateDiffProbability = rs1.getDouble("dateDiffProbability");
							authorRoleProbability	= rs1.getDouble("authorRoleProbability");							negationTermPenalty = rs1.getDouble("negationTermPenalty");
							//march 2019																													// messageLocationProbabilityScore
							restOfParagraphProbabilityScore= rs1.getDouble("restOfParagraphProbabilityScore"); messageLocationProbabilityScore= rs1.getDouble("messageLocationProbabilityScore"); 
							messageTypeIsReasonMessageProbabilityScore= rs1.getDouble("messageTypeIsReasonMessageProbabilityScore");
							sentenceLocationInMessageProbabilityScore= rs1.getDouble("sentenceLocationInMessageProbabilityScore"); 
							sameMsgSubAsStateTripleProbabilityScore= rs1.getDouble("sameMsgSubAsStateTripleProbabilityScore");
							reasonLabelFoundUsingTripleExtractionProbabilityScore= rs1.getDouble("reasonLabelFoundUsingTripleExtractionProbabilityScore");
							messageContainsSpecialTermProbabilityScore= rs1.getDouble("messageContainsSpecialTermProbabilityScore");
							prevParagraphSpecialTermProbabilityScore= rs1.getDouble("prevParagraphSpecialTermProbabilityScore");
							
						//march 2019..now read and keep these original values
							ORIGsentenceHintProbablity	= rs1.getDouble("ORIGsentenceHintProbablity");							ORIGsentenceLocationHintProbability = rs1.getDouble("ORIGsentenceLocationHintProbability");
							ORIGmessageSubjectHintProbablityScore	= rs1.getDouble("ORIGmessageSubjectHintProbablityScore");	ORIGdateDiffProbability = rs1.getDouble("ORIGdateDiffProbability");
							ORIGauthorRoleProbability	= rs1.getDouble("ORIGauthorRoleProbability");							ORIGnegationTermPenalty = rs1.getDouble("ORIGnegationTermPenalty");
							//march 2019																													// messageLocationProbabilityScore
							ORIGrestOfParagraphProbabilityScore= rs1.getDouble("ORIGrestOfParagraphProbabilityScore"); ORIGmessageLocationProbabilityScore= rs1.getDouble("ORIGmessageLocationProbabilityScore"); 
							ORIGmessageTypeIsReasonMessageProbabilityScore= rs1.getDouble("ORIGmessageTypeIsReasonMessageProbabilityScore");
							ORIGsentenceLocationInMessageProbabilityScore= rs1.getDouble("ORIGsentenceLocationInMessageProbabilityScore"); 
							ORIGsameMsgSubAsStateTripleProbabilityScore= rs1.getDouble("ORIGsameMsgSubAsStateTripleProbabilityScore");
							ORIGreasonLabelFoundUsingTripleExtractionProbabilityScore= rs1.getDouble("ORIGreasonLabelFoundUsingTripleExtractionProbabilityScore");
							ORIGmessageContainsSpecialTermProbabilityScore= rs1.getDouble("ORIGmessageContainsSpecialTermProbabilityScore");
							ORIGprevParagraphSpecialTermProbabilityScore= rs1.getDouble("ORIGprevParagraphSpecialTermProbabilityScore");
							
						//}
		//					System.out.println("Proposal: " + proposalNum + " mid " + mid );
						/*	
							  finalProbability,	 //the final score of all the below probabilities
							  reasonProbabilityScore,		     //the probability score based on the  addition of = messageSubjectHintProbablityScore + sentenceOrParagraphHintProbablity ..maybe doesnt matter
							  sentenceOrParagraphHintProbablity, //THE MAIN DETERMINANT.. based on different combination of terms found in a sentence
							  messageSubjectHintProbablityScore, //if terms (like pep&accepted OR pep&resolution) are found in the message subject, a probability is assigned
							  negationTermPenalty,				 //a negative word like 'no' in a  sentence gets a '-0.8' as negation penalty
							  messageLocationProbabilityScore,	 //the location of the message also gives if different scores..state message, pep summary or 'other messages'
							  termsLocationWithMessageProbabilityScore, // if the combination is found in a sentence, adjacent sentences or paragraph ALSO FOR STATE MESSAGE sentence and nearby sentences, paragraph or nearby paragraphs
							  dateDiffProbability, 	  			 //less than 7 days from state message gets high probability, all others don't get any
							  authorRoleProbability,  			 //different probabilities assigned to  message author based on author {bdfl, proposalauthor, pep editors, core developers and other members}
							  restOfParagraphProbabilityScore,	 //
							  //november 2018
							  messsageTypeIsReasonMessageProbabilityScore,  //all these get 0.9 -(proposalAuthorAskingReview, BDFL Reviewing, BDFLPronouncement, communityMemberReviewing), else zero
							  sentenceLocationInMessageProbabilityScore,    //if its isFirstParagraph,  isLastParagraph,. then they get 0.9 probability score, else zero
							  sameMsgSubAsStateTripleProbabilityScore,  	//if message subject has same subject as state triple then we give more weight..0.9, else zero				
							  reasonLabelFoundUsingTripleExtractionProbabilityScore, //Feb 2019 
							  messageContainsSpecialTermProbabilityScore, //Feb 2019 
							  prevParagraphSpecialTermProbabilityScore;	  //feb 2019
						*/	
							
						ScoresToSentences p0 = new ScoresToSentences(); 
						
						p0.setDateDiff(datediff);	
						//NOTE RANK IS NOT SET HERE
						//anyRanking = rsA.getInt("rank");       //System.out.print("\t ranking "+ anyRanking); 		
						p0.setSentenceOrParagraph(autosentenceOrParagraphOrMessage);			
						//termsMatched = rsA.getString("termsMatched");	
						
						//new way
						double prob = sentenceHintProbablity+ /*sentenceLocationHintProbability+*/ restOfParagraphProbabilityScore+messageLocationProbabilityScore+messageSubjectHintProbablityScore
								+dateDiffProbability+authorRoleProbability+messageTypeIsReasonMessageProbabilityScore+sentenceLocationInMessageProbabilityScore+sameMsgSubAsStateTripleProbabilityScore
								+reasonLabelFoundUsingTripleExtractionProbabilityScore+messageContainsSpecialTermProbabilityScore+prevParagraphSpecialTermProbabilityScore-negationTermPenalty;
						
						p0.setTotalProbabilityScore(prob);	
						p0.setOrigTotalProbabilityScore(prob);   //dec 2019
						
						//old way
						//p0.setTotalProbabilityScore(sentenceHintProbablity+ /*sentenceLocationHintProbability+*/ restOfParagraphProbabilityScore+messageLocationProbabilityScore+messageSubjectHintProbablityScore
						//		+dateDiffProbability+authorRoleProbability+messageTypeIsReasonMessageProbabilityScore+sentenceLocationInMessageProbabilityScore+sameMsgSubAsStateTripleProbabilityScore
						//		+reasonLabelFoundUsingTripleExtractionProbabilityScore+messageContainsSpecialTermProbabilityScore+prevParagraphSpecialTermProbabilityScore-negationTermPenalty);
						
						
						p0.setAuthorsRole(author);				p0.setLocation(loc);
						//date value p0.setDateVal(dateVal);	
						 
						
						//one probability object per row
						p0.setId(id); 				p0.setProposalNum(proposalNum); 				p0.setMid(Integer.valueOf(mid)); 
		//$$				System.out.println("Proposal Num : " + p0.getProposalNum() + " mid " + p0.getMid());
						//p0.setDateDiff(datediff); p0.setAuthor(author);
						p0.setLabel(autolabel);
						p0.setDateDiffProbability(dateDiffProbability);  								p0.setORIGdateDiffProbability(ORIGdateDiffProbability);
						p0.setAuthorRoleProbability(authorRoleProbability);								p0.setORIGdateDiffProbability(ORIGdateDiffProbability);
						p0.setSentenceOrParagraphHintProbablity(sentenceHintProbablity);     			p0.setORIGsentenceHintProbablity(ORIGsentenceHintProbablity);
						p0.setMessageSubjectHintProbablityScore(messageSubjectHintProbablityScore);		p0.setORIGmessageSubjectHintProbablityScore(ORIGmessageSubjectHintProbablityScore);
						p0.setSentenceLocationInMessageProbabilityScore(sentenceLocationInMessageProbabilityScore);	p0.setORIGsentenceLocationInMessageProbabilityScore(ORIGsentenceLocationInMessageProbabilityScore);	
						p0.setRestOfParagraphProbabilityScore(restOfParagraphProbabilityScore);			p0.setORIGrestOfParagraphProbabilityScore(ORIGrestOfParagraphProbabilityScore);
						p0.setMesssageTypeIsReasonMessageProbabilityScore(messageTypeIsReasonMessageProbabilityScore);	p0.setORIGmessageTypeIsReasonMessageProbabilityScore(ORIGmessageTypeIsReasonMessageProbabilityScore);
		//				p0.setSentenceLocationInMessageProbabilityScore(sentenceLocationInMessageProbabilityScore);		p0.setORIGsentenceLocationInMessageProbabilityScore(ORIGsentenceLocationInMessageProbabilityScore);
						p0.setMessageLocationProbabilityScore(messageLocationProbabilityScore);			p0.setORIGmessageLocationProbabilityScore(ORIGmessageLocationProbabilityScore);
						//p0.setrestofparagraph
						p0.setSameMsgSubAsStateTripleProbabilityScore(sameMsgSubAsStateTripleProbabilityScore);			p0.setORIGsameMsgSubAsStateTripleProbabilityScore(ORIGsameMsgSubAsStateTripleProbabilityScore);
						p0.setReasonLabelFoundUsingTripleExtractionProbabilityScore(reasonLabelFoundUsingTripleExtractionProbabilityScore); p0.setORIGreasonLabelFoundUsingTripleExtractionProbabilityScore(ORIGreasonLabelFoundUsingTripleExtractionProbabilityScore);
						
						p0.setMessageContainsSpecialTermProbabilityScore(messageContainsSpecialTermProbabilityScore);	p0.setORIGmessageContainsSpecialTermProbabilityScore(ORIGmessageContainsSpecialTermProbabilityScore);
						p0.setPrevParagraphSpecialTermProbabilityScore(prevParagraphSpecialTermProbabilityScore);		p0.setORIGprevParagraphSpecialTermProbabilityScore(ORIGprevParagraphSpecialTermProbabilityScore);
						p0.setNegationTermPenalty(negationTermPenalty);									p0.setORIGnegationTermPenalty(ORIGnegationTermPenalty);
						
						p0.setFinalProbability(sentenceHintProbablity+restOfParagraphProbabilityScore+messageLocationProbabilityScore+messageSubjectHintProbablityScore
					    		  +dateDiffProbability+authorRoleProbability+messageTypeIsReasonMessageProbabilityScore+sentenceLocationInMessageProbabilityScore+sameMsgSubAsStateTripleProbabilityScore
					    		  +reasonLabelFoundUsingTripleExtractionProbabilityScore+messageContainsSpecialTermProbabilityScore+prevParagraphSpecialTermProbabilityScore-negationTermPenalty);
						
						List.add(p0);
						
						//p=null;
						count++;
				} //end while for all results for that proposal and for that label
				rs1.close();			ps1.close();
			} //end for each label
				
			
//				System.out.println("\tTotal Read in id in Array : " + count);
			} //end for each label in proposal
			
		} //end try
		
		catch (SQLException e) {		
			System.out.println(StackTraceToString(e)  );	System.out.println("Exception" + e.toString());						e.printStackTrace();
		} catch (Exception ex) {
			System.out.println(StackTraceToString(ex)  );    System.out.println("Exception" + ex.toString());						ex.printStackTrace();
		}
		return List;
	}
	
	//Jan 2020 ..populate message level scheme in chunks 
	private static void removeDuplicates_Rank_TransferToNewTable(int scheme, int startCounter, int endCounter){// ArrayList<messageText> messageTextList) {
		if (outputDetails)
			System.out.println("\tPopulating ArrayList for "+scheme+" scheme:  ");
			
		//ArrayList<Integer> proposalsInResults = new ArrayList<Integer>(); 
		ArrayList<Integer> distinctPEPsInResults = new ArrayList<Integer>();   // we hold all different PEPs here which we find in the results so we can read data in chunks
		
		try {
			distinctPEPsInResults= getDistinctProposalsInResults(distinctPEPsInResults);
			int c=0;						
			String query1 = ""; int rs1 =0; PreparedStatement ps1 = null;
			ArrayList<String> labelsForProposal = new ArrayList<String>(); 
			
			//main reason is to eliminate 	duplicate reason sentences for each pep we need to see that we dont 
			//for each proposal ... we read data in chunks ...
			for (Integer p : distinctPEPsInResults)
			{	
				//Jan 2020 debug
				if (p >= startCounter && p < endCounter) {}  //we do it in chunks...we enter 300, 400, 500, 3100 here one at a time  // != 391 
				else {	
					continue;				
				}
				// debug
	//			if( p!= 391)
	//				continue;
				
				labelsForProposal.clear();
				labelsForProposal = getLabelsForProposal(p);
				if (outputDetails) 
					System.out.println("\tProcessing Proposal # In Results for "+scheme+" scheme, proposal: " + p + " labels count: " + labelsForProposal.size()  );
				
				//for each proposal, we get results for the label
				for (String lab : labelsForProposal)
				{	
					if(outputDetails) 
						System.out.println("\tlab " + lab);
					
					if(scheme==1) {
						//System.out.println("Populating SentenceBased ArrayList :  ");
						//Sentence Based		
						query1 =  " INSERT INTO "+tablenameToTransferResultsForEvaluation_Sent+" "
								+ " ( proposal,messageid,dateValue,datediff, sentence, label, location, termsMatched, messageTypeIsReasonMessage,messageType, "
								//+ " (rank,messageTypeIsReasonMessage as RM,messageType as MT, proposal,messageid,dateValue,datediff, sentence as text,termsMatched, " + 
								+ "	  sentenceHintProbablity,sentenceLocationHintProbability,restOfParagraphProbabilityScore,messageLocationProbabilityScore,messageSubjectHintProbablityScore,  "  
								+ "	  dateDiffProbability,authorRoleProbability,messageTypeIsReasonMessageProbabilityScore,sentenceLocationInMessageProbabilityScore,sameMsgSubAsStateTripleProbabilityScore, " 
								+ "   reasonLabelFoundUsingTripleExtractionProbabilityScore,messageContainsSpecialTermProbabilityScore,prevParagraphSpecialTermProbabilityScore,negationTermPenalty,  "
								
								+ "   ORIGsentenceHintProbablity,ORIGsentenceLocationHintProbability,ORIGmessageSubjectHintProbablityScore,ORIGdateDiffProbability,ORIGauthorRoleProbability,"  
								+ "	  ORIGnegationTermPenalty,ORIGrestOfParagraphProbabilityScore,ORIGmessageLocationProbabilityScore," 
								+ "	  ORIGmessageTypeIsReasonMessageProbabilityScore,ORIGsentenceLocationInMessageProbabilityScore,ORIGsameMsgSubAsStateTripleProbabilityScore," 
								+ "	  ORIGreasonLabelFoundUsingTripleExtractionProbabilityScore,ORIGmessageContainsSpecialTermProbabilityScore,ORIGprevParagraphSpecialTermProbabilityScore"
								+ " ) "
								+ "  SELECT proposal,messageid,dateValue,datediff, sentence,  label, location, termsMatched, messageTypeIsReasonMessage,messageType, "
//								+ " SELECT *, @curRank := @curRank + 1 AS rank,messageTypeIsReasonMessage as RM,messageType as MT, proposal,messageid,dateValue,datediff, sentence as text,termsMatched, " 
								+ " sentenceHintProbablity,sentenceLocationHintProbability,restOfParagraphProbabilityScore,messageLocationProbabilityScore,messageSubjectHintProbablityScore, " 
								+ " dateDiffProbability,authorRoleProbability,messageTypeIsReasonMessageProbabilityScore,sentenceLocationInMessageProbabilityScore,sameMsgSubAsStateTripleProbabilityScore, " 
								+ " reasonLabelFoundUsingTripleExtractionProbabilityScore,messageContainsSpecialTermProbabilityScore,prevParagraphSpecialTermProbabilityScore,negationTermPenalty, "
								
								+ " ORIGsentenceHintProbablity,ORIGsentenceLocationHintProbability,ORIGmessageSubjectHintProbablityScore,ORIGdateDiffProbability,ORIGauthorRoleProbability,"  
								+ "	ORIGnegationTermPenalty,ORIGrestOfParagraphProbabilityScore,ORIGmessageLocationProbabilityScore," 
								+ "	ORIGmessageTypeIsReasonMessageProbabilityScore,ORIGsentenceLocationInMessageProbabilityScore,ORIGsameMsgSubAsStateTripleProbabilityScore," 
								+ "	ORIGreasonLabelFoundUsingTripleExtractionProbabilityScore,ORIGmessageContainsSpecialTermProbabilityScore,ORIGprevParagraphSpecialTermProbabilityScore "
								
								+ " FROM "+tablenameToLoadResultsForEvaluation+"   " //, (SELECT @curRank := 0) r  " 
								+ " WHERE proposal = " + p +" and label = '"+lab+"' "
								+ " AND location = 'sentence' "
								+ " GROUP by sentence " // messageid,  -- jan 2020 ... this is necesary to remove duplicate sentences"
								+ " ORDER by TotalProbability desc, dateValue desc; ";			//we just add this but this is taken care of later in the code
								
					}
					/*
					String query10 =" SELECT * FROM " 
							+ " ( " 
								+ " SELECT *, (select analysewords from allmessages w where w.messageid = x.messageid limit 1) as text from "+tablenameToLoadResultsForEvaluation+" x "
							    + " WHERE x.proposal = " + p +" and x.label = '"+lab+"'"
							    + " GROUP BY sentence "	//-- jan 2020 ... this is necesary to remove duplicate sentences " 
							    + " order by label asc, TotalProbability desc, dateValue desc "
							+ " ) as messages "  
							+ " GROUP BY messages.messageid " 
							+ " ORDER BY messages.label asc, messages.TotalProbability desc, messages.dateValue desc;";
					//double val = 0.0, incrementValue = d; Integer round = 0;
					*/
					
					else if(scheme==2) { //message based
						query1 = " INSERT INTO "+tablenameToTransferResultsForEvaluation_Msg+" " 
								+ " ( proposal,messageid,dateValue,datediff, sentence,  label, location, termsMatched, messageTypeIsReasonMessage,messageType, "
								//+ " (rank,messageTypeIsReasonMessage as RM,messageType as MT, proposal,messageid,dateValue,datediff, sentence as text,termsMatched, " + 
								+ "	  sentenceHintProbablity,sentenceLocationHintProbability,restOfParagraphProbabilityScore,messageLocationProbabilityScore,messageSubjectHintProbablityScore,  "  
								+ "	  dateDiffProbability,authorRoleProbability,messageTypeIsReasonMessageProbabilityScore,sentenceLocationInMessageProbabilityScore,sameMsgSubAsStateTripleProbabilityScore, " 
								+ "   reasonLabelFoundUsingTripleExtractionProbabilityScore,messageContainsSpecialTermProbabilityScore,prevParagraphSpecialTermProbabilityScore,negationTermPenalty,  "
								
								+ "   ORIGsentenceHintProbablity,ORIGsentenceLocationHintProbability,ORIGmessageSubjectHintProbablityScore,ORIGdateDiffProbability,ORIGauthorRoleProbability,"  
								+ "	  ORIGnegationTermPenalty,ORIGrestOfParagraphProbabilityScore,ORIGmessageLocationProbabilityScore," 
								+ "	  ORIGmessageTypeIsReasonMessageProbabilityScore,ORIGsentenceLocationInMessageProbabilityScore,ORIGsameMsgSubAsStateTripleProbabilityScore," 
								+ "	  ORIGreasonLabelFoundUsingTripleExtractionProbabilityScore,ORIGmessageContainsSpecialTermProbabilityScore,ORIGprevParagraphSpecialTermProbabilityScore"
								+ " ) "								
								
								+ "  SELECT proposal,messageid,dateValue,datediff, sentence,  label, location, termsMatched, messageTypeIsReasonMessage,messageType, "
								//+ " SELECT *, @curRank := @curRank + 1 AS rank,messageTypeIsReasonMessage as RM,messageType as MT, proposal,messageid,dateValue,datediff, sentence as text,termsMatched, " 
								+ " sentenceHintProbablity,sentenceLocationHintProbability,restOfParagraphProbabilityScore,messageLocationProbabilityScore,messageSubjectHintProbablityScore, " 
								+ " dateDiffProbability,authorRoleProbability,messageTypeIsReasonMessageProbabilityScore,sentenceLocationInMessageProbabilityScore,sameMsgSubAsStateTripleProbabilityScore, " 
								+ " reasonLabelFoundUsingTripleExtractionProbabilityScore,messageContainsSpecialTermProbabilityScore,prevParagraphSpecialTermProbabilityScore,negationTermPenalty, "
								
								+ " ORIGsentenceHintProbablity,ORIGsentenceLocationHintProbability,ORIGmessageSubjectHintProbablityScore,ORIGdateDiffProbability,ORIGauthorRoleProbability,"  
								+ "	ORIGnegationTermPenalty,ORIGrestOfParagraphProbabilityScore,ORIGmessageLocationProbabilityScore," 
								+ "	ORIGmessageTypeIsReasonMessageProbabilityScore,ORIGsentenceLocationInMessageProbabilityScore,ORIGsameMsgSubAsStateTripleProbabilityScore," 
								+ "	ORIGreasonLabelFoundUsingTripleExtractionProbabilityScore,ORIGmessageContainsSpecialTermProbabilityScore,ORIGprevParagraphSpecialTermProbabilityScore "
								+ " FROM "
								+ " ( "
									+ " select *  " //*analysewords from allmessages w where w.messageid = x.messageid limit 1) as text */ "
									+ " from "+tablenameToLoadResultsForEvaluation+" x "
								    + " WHERE x.proposal = " + p +" and x.label = '"+lab+"'"
								    + " GROUP BY sentence "	//-- jan 2020 ... this is necesary to remove duplicate sentences " 
								    + " order by label asc, TotalProbability desc, dateValue desc "
								+ " ) as messages "  
								+ " GROUP BY messages.messageid " 
								+ " ORDER BY messages.label asc, messages.TotalProbability desc, messages.dateValue desc;";
						//double val = 0.0, incrementValue = d; Integer round = 0;
					}
					
					ps1 = connection.prepareStatement(query1);
					//ps.setString(0, f);	 //System.out.println("f "+f);
					// process the results
					rs1 = ps1.executeUpdate();
					System.out.println("Total Rows inserted for PEP "+p+" for "+scheme+" scheme for label "+lab+"  = " + rs1);
					//all results for that proposal and for that label					
					//rs1.close();			
					ps1.close();	
			} //end for each label
//$$		System.out.println("Total Read in id : " + c);
			} //end for each label in proposal			
		} //end try
		
		catch (SQLException e) {		
			System.out.println(StackTraceToString(e)  );	System.out.println("Exception" + e.toString());						e.printStackTrace();
		} catch (Exception ex) {
			System.out.println(StackTraceToString(ex)  );    System.out.println("Exception" + ex.toString());						ex.printStackTrace();
		}
	}

	//feature selection. we have to reset weights for all variables back to original
	//march 2019 - we dont increment values for 0.0	
	public static ArrayList <ScoresToSentences> resetWeightsBackToOriginal_fs(ArrayList <ScoresToSentences> list) throws IOException{
		ArrayList<Integer> al = new ArrayList<Integer>(); 
  		double sentenceHintProbablity=0.0,sentenceLocationHintProbability=0.0,messageSubjectHintProbablityScore=0.0, dateDiffProbability=0.0, authorRoleProbability=0.0, negationTermPenalty=0.0,
				   restOfParagraphProbabilityScore=0.0, messageLocationProbabilityScore=0.0,  messageTypeIsReasonMessageProbabilityScore=0.0,sentenceLocationInMessageProbabilityScore=0.0,sameMsgSubAsStateTripleProbabilityScore=0.0,
				   reasonLabelFoundUsingTripleExtractionProbabilityScore=0.0,messageContainsSpecialTermProbabilityScore=0.0,prevParagraphSpecialTermProbabilityScore=0.0;
    	try
  		{
  			
    		boolean f = false, g= false, h=false;
	  		if(!list.isEmpty())
	  		{
	  			int counter =0;
	  			for (int x=0; x <list.size()-1; x++)
	  			{ 
	  				//reset total probability
	  				list.get(x).setTotalProbabilityScore(list.get(x).getOrigTotalProbabilityScore());
	  				
	  				//for all records retrieved for the table		  			
	  				//sentenceHintProbablity IncrementValue	  				
	  				list.get(x).setSentenceOrParagraphHintProbablity(list.get(x).getORIGsentenceHintProbablity()); //sentenceHintProbablityIncrementValue);
	  				//list.get(x).setTotalProbabilityScore( Double.sum( list.get(x).getTotalProbabilityScore(),IncrementValue) );  
	  				  				
	  				//sentenceLocationHintProbability	  				
  					list.get(x).setSentenceLocationInMessageProbabilityScore(list.get(x).getORIGsentenceLocationHintProbability()); //sentenceLocationHintProbabilityIncrementValue);
	  							  				
	  				//restOfParagraphProbabilityScore
	  				list.get(x).setSentenceOrParagraphHintProbablity(list.get(x).getORIGrestOfParagraphProbabilityScore()); //restOfParagraphProbabilityScoreIncrementValue);
	  							  				
		  			//messageLocationProbabilityScore	  				
	  				list.get(x).setSentenceOrParagraphHintProbablity(list.get(x).getORIGmessageLocationProbabilityScore()); //messageLocationProbabilityScoreIncrementValue);
	  					
		  			
	  				//messageSubjectHintProbablityScore	  			
	  				list.get(x).setSentenceOrParagraphHintProbablity(list.get(x).getORIGmessageSubjectHintProbablityScore()); //sentenceHintProbablityIncrementValue);
	  						  				
	  				//dateDiffProbability
	  				list.get(x).setDateDiffProbability(list.get(x).getORIGdateDiffProbability()); //dateDiffProbabilityIncrementValue);
	  						  				
	  				//authorRoleProbability
	  				list.get(x).setAuthorRoleProbability(list.get(x).getORIGauthorRoleProbability()); //authorRoleProbabilityIncrementValue);
	  					  					  				
	  				//messageTypeIsReasonMessageProbabilityScore
	  				list.get(x).setMesssageTypeIsReasonMessageProbabilityScore(list.get(x).getORIGmessageTypeIsReasonMessageProbabilityScore()); //messageTypeIsReasonMessageProbabilityScoreIncrementValue);
	  					
	  				//sameMsgSubAsStateTripleProbabilityScore
	  				list.get(x).setSameMsgSubAsStateTripleProbabilityScore(list.get(x).getORIGsameMsgSubAsStateTripleProbabilityScore()); //sameMsgSubAsStateTripleProbabilityScoreIncrementValue);
	  					
	  				//reasonLabelFoundUsingTripleExtractionProbabilityScore		  			
	  				list.get(x).setReasonLabelFoundUsingTripleExtractionProbabilityScore(list.get(x).getORIGreasonLabelFoundUsingTripleExtractionProbabilityScore()); //reasonLabelFoundUsingTripleExtractionProbabilityScoreIncrementValue);
	  			
	  				//messageContainsSpecialTermProbabilityScore
	  				list.get(x).setMessageContainsSpecialTermProbabilityScore(list.get(x).getORIGmessageContainsSpecialTermProbabilityScore()); //messageContainsSpecialTermProbabilityScoreIncrementValue);
	  					
		  			//prevParagraphSpecialTermProbabilityScore		  			
	  				list.get(x).setPrevParagraphSpecialTermProbabilityScore(list.get(x).getORIGprevParagraphSpecialTermProbabilityScore()); //prevParagraphSpecialTermProbabilityScoreIncrementValue);
	  					  				
		  			//negationTermPenalty	  				
	  				list.get(x).setNegationTermPenalty(list.get(x).getORIGnegationTermPenalty()); //negationTermPenaltyIncrementValue);
	  					
	  			}
	  			
	  			
	  		}
    		
    		
  		}  //end  try
  		catch(Exception e){
  			System.out.println("Exception " + e.toString());  			System.out.println(StackTraceToString(e)  );
  		}
  		return list;
    }
	
	
	public static ArrayList<Integer> getDistinctProposalsInResults(ArrayList<Integer> proposalsInResults) throws SQLException {
		String queryk = "SELECT distinct proposal from "+tablenameToLoadResultsForEvaluation+" ;";
		PreparedStatement psk = connection.prepareStatement(queryk);
		ResultSet rsk = psk.executeQuery();
		//ArrayList<String> labelsForProposal = new ArrayList<String>(); 
		Integer p =null;
		while ( rsk.next() )    { 
			p = rsk.getInt("proposal");
			proposalsInResults.add(p);
		}
		rsk.close();
		return proposalsInResults;
	}
	
	public static ArrayList<String> getLabelsForProposal(Integer p) throws SQLException {
		String queryk = "SELECT distinct label from "+tablenameToLoadResultsForEvaluation+" where proposal = " +p+ ";";
		PreparedStatement psk = connection.prepareStatement(queryk);
		ResultSet rsk = psk.executeQuery();
		ArrayList<String> labelsForProposal = new ArrayList<String>(); 
		String lab ="";
		while ( rsk.next() )    { 
			lab = rsk.getString("label");
			labelsForProposal.add(lab);
		}
		rsk.close();
		return labelsForProposal;
	}
	
	public static void showRanking(ArrayList <ScoresToSentences> list) {
		int p5, m5; String label ="";
		double prob;
		for (int x=0; x < list.size()-1; x++){  //List.size()-1			
				p5 = list.get(x).getProposalNum();					m5 = list.get(x).getMid();
				label = list.get(x).getLabel();     prob = list.get(x).getTotalProbabilityScore();
				if(p5==308 && label.toLowerCase().equals("accepted")) {
					System.out.println("\t\tProposal : "+p5 + " mid "+ m5 + " total probability " + prob);
				}
				/*if(p.equals(proposalNum) || p == proposalNum) {
					if(!firstFound) {//if first index not found, set first index
						firstFound=true; firstIndex=x;
					}
					if(firstFound) { //if first index found, keep incrementing last index
						lastFound=true;	lastIndex=x;
					}
					counter++;   						  					
				} */
				//get first and last index of the records with current proposal number
//				if(firstFound && lastFound)
//					System.out.println("\t\tProposal : "+p5 + " mid "+ m5);// + " count: " + counter + "firstIndex : "+firstIndex + " lastIndex: " + lastIndex + " List size : "+ (List.size()-1));
		}
	}
	
	//sorting done in probability class
	public static ArrayList <ScoresToSentences> sortArrayList(ArrayList <ScoresToSentences> list){
	//1. Probability records in ascending order using total probability 
		Collections.sort(list);
   // System.out.println(List);
    //2. Probability ids in reverse order
//$$	Collections.sort(List, Collections.reverseOrder());
		return list;
	}
	
	//update value of all fields
	//march 2019 - we dont increment values for 0.0
	public static ArrayList <ScoresToSentences> incrementSpecifiedField(ArrayList <ScoresToSentences> list, double IncrementValue, int fieldToUpdate) throws IOException{
		/*	
			double sentenceHintProbablityIncrementValue,
			double sentenceLocationHintProbabilityIncrementValue, double messageSubjectHintProbablityScoreIncrementValue,
			double dateDiffProbabilityIncrementValue, double authorRoleProbabilityIncrementValue,
			double negationTermPenaltyIncrementValue, double restOfParagraphProbabilityScoreIncrementValue, double messageLocationProbabilityScoreIncrementValue, 
			double messageTypeIsReasonMessageProbabilityScoreIncrementValue,double sentenceLocationInMessageProbabilityScoreIncrementValue,
			double sameMsgSubAsStateTripleProbabilityScoreIncrementValue, double reasonLabelFoundUsingTripleExtractionProbabilityScoreIncrementValue, 
			double messageContainsSpecialTermProbabilityScoreIncrementValue, double prevParagraphSpecialTermProbabilityScoreIncrementValue,
			int fieldToUpdate) throws IOException{
		*/
 //   	System.out.println("Function Update field value()");	
    	ArrayList<Integer> al = new ArrayList<Integer>(); 
  		
    	//The fields are updated in this order
    	String fields[] = {"sentenceHintProbablity","sentenceLocationHintProbability","restOfParagraphProbabilityScore","messageLocationProbabilityScore","messageSubjectHintProbablityScore", //5
				"dateDiffProbability","authorRoleProbability","messageTypeIsReasonMessageProbabilityScore","sentenceLocationInMessageProbabilityScore","sameMsgSubAsStateTripleProbabilityScore", //10
				"reasonLabelFoundUsingTripleExtractionProbabilityScore","messageContainsSpecialTermProbabilityScore","prevParagraphSpecialTermProbabilityScore","negationTermPenalty"};
    	
    	double sentenceHintProbablity=0.0,sentenceLocationHintProbability=0.0,messageSubjectHintProbablityScore=0.0, dateDiffProbability=0.0, authorRoleProbability=0.0, negationTermPenalty=0.0,
				   restOfParagraphProbabilityScore=0.0, messageLocationProbabilityScore=0.0,  messageTypeIsReasonMessageProbabilityScore=0.0,sentenceLocationInMessageProbabilityScore=0.0,sameMsgSubAsStateTripleProbabilityScore=0.0,
				   reasonLabelFoundUsingTripleExtractionProbabilityScore=0.0,messageContainsSpecialTermProbabilityScore=0.0,prevParagraphSpecialTermProbabilityScore=0.0;
    	
  		try
  		{	
  			//Jan 2020..these are the fields in which order they are passed as variables and also inserted in the db 
  			// incrementCounter0  - sentenceHintProbablity, 			 					incrementCounter1  - sentenceLocationInMessageProbabilityScore
  			// incrementCounter2  - restOfParagraphProbabilityScore		 					incrementCounter3  - messageLocationProbabilityScore 
  			// incrementCounter4  - messageSubjectHintProbablityScore 	 					incrementCounter5  - dateDiffProbability
  			// incrementCounter6  - authorRoleProbability 				 					incrementCounter7  - messageTypeIsReasonMessageProbabilityScore 
  			// incrementCounter8  - sameMsgSubAsStateTripleProbabilityScore					incrementCounter9  - reasonLabelFoundUsingTripleExtractionProbabilityScore
  			// incrementCounter10 - messageContainsSpecialTermProbabilityScore 				incrementCounter11 - prevParagraphSpecialTermProbabilityScore
  			// incrementCounter12 - negationTermPenalty
  			
  			
  			boolean f = false, g= false, h=false;
	  		if(!list.isEmpty()){
	  			int counter =0;
	  			for (int x=0; x <list.size()-1; x++){
		  			
	  				//sentenceHintProbablity IncrementValue
	  				if(fieldToUpdate==0) {
	  					/*if(!f) {
	  						System.out.println("p" + list.get(x).getProposalNum()+ " mid "+ list.get(x).getMid() + " label "+ list.get(x).getLabel() + " first retreived updated value: " +  list.get(x).getSentenceOrParagraphHintProbablity());
	  						System.out.println("INITIAL list.get(x).setTotalProbabilityScore(): "+list.get(x).getTotalProbabilityScore());
	  					} */
	  					sentenceHintProbablity= list.get(x).getORIGsentenceHintProbablity();		//.toLowerCase()
		  				if(sentenceHintProbablity==0.0) {} //same as checking for null
		  				else {
		  					list.get(x).setSentenceOrParagraphHintProbablity(Double.sum(sentenceHintProbablity,IncrementValue)); //sentenceHintProbablityIncrementValue);
		  					list.get(x).setTotalProbabilityScore( Double.sum( list.get(x).getTotalProbabilityScore(),IncrementValue) );  
		  				}
		  				/*if(!f) {
		  					System.out.println("list.get(x).getORIGsentenceHintProbablity(): "+list.get(x).getORIGsentenceHintProbablity());
	  						System.out.println("IncrementValue: " + IncrementValue+ " sentenceHintProbablity: "+ sentenceHintProbablity + " new shp  : " +Double.sum(sentenceHintProbablity,IncrementValue));
	  						System.out.println("retreived updated value: " +  list.get(x).getSentenceOrParagraphHintProbablity());
	  						System.out.println("FINAL list.get(x).getTotalProbabilityScore(): " +  list.get(x).getTotalProbabilityScore());
	  						f = true;
		  				} */
	  				}
	  				
	  				//sentenceLocationHintProbability
	  				else if(fieldToUpdate==1) {
	  					sentenceLocationHintProbability= list.get(x).getORIGsentenceLocationHintProbability();		//.toLowerCase()
		  				if(sentenceLocationHintProbability==0.0) {} //same as checking for null
		  				else {
		  					list.get(x).setSentenceLocationInMessageProbabilityScore(Double.sum(sentenceLocationHintProbability,IncrementValue)); //sentenceLocationHintProbabilityIncrementValue);
		  					list.get(x).setTotalProbabilityScore( Double.sum( list.get(x).getTotalProbabilityScore(),IncrementValue) );  
		  				}
		  				/*if(!g)
	  						System.out.println("new slhp : " +sentenceLocationHintProbability + IncrementValue );
		  				g=true;*/
	  				}
	  				//restOfParagraphProbabilityScore
	  				else if(fieldToUpdate==2) {	  					
		  				restOfParagraphProbabilityScore= list.get(x).getORIGrestOfParagraphProbabilityScore();		//.toLowerCase()
		  				if(restOfParagraphProbabilityScore==0.0) {} //same as checking for null
		  				else {
		  					list.get(x).setSentenceOrParagraphHintProbablity(Double.sum(restOfParagraphProbabilityScore,IncrementValue)); //restOfParagraphProbabilityScoreIncrementValue);
		  					list.get(x).setTotalProbabilityScore( Double.sum( list.get(x).getTotalProbabilityScore(),IncrementValue) );  
		  				}
		  				/*if(!h)
	  						System.out.println("new ropps : " +restOfParagraphProbabilityScore + IncrementValue);
		  				h=true;*/
		  			}
		  			//messageLocationProbabilityScore
	  				else if(fieldToUpdate==3) {
		  				messageLocationProbabilityScore= list.get(x).getORIGmessageLocationProbabilityScore();		//.toLowerCase()
		  				if(messageLocationProbabilityScore==0.0) {} //same as checking for null
		  				else { 
		  					list.get(x).setSentenceOrParagraphHintProbablity(Double.sum(messageLocationProbabilityScore,IncrementValue)); //messageLocationProbabilityScoreIncrementValue);
		  					list.get(x).setTotalProbabilityScore( Double.sum( list.get(x).getTotalProbabilityScore(),IncrementValue) );  
		  				}
		  			}
	  				//messageSubjectHintProbablityScore
	  				else if(fieldToUpdate==4) {
		  				messageSubjectHintProbablityScore= list.get(x).getORIGmessageSubjectHintProbablityScore();		//.toLowerCase()
		  				if(messageSubjectHintProbablityScore==0.0) {} //same as checking for null
		  				else {
		  					list.get(x).setSentenceOrParagraphHintProbablity(Double.sum(messageSubjectHintProbablityScore,IncrementValue)); //sentenceHintProbablityIncrementValue);
		  					list.get(x).setTotalProbabilityScore( Double.sum( list.get(x).getTotalProbabilityScore(),IncrementValue) );  
		  				}
	  				}
	  				//dateDiffProbability
	  				else if(fieldToUpdate==5) {
		  				dateDiffProbability= list.get(x).getORIGdateDiffProbability();		//.toLowerCase()
		  				if(dateDiffProbability==0.0) {} //same as checking for null
		  				else { 
		  					list.get(x).setDateDiffProbability(Double.sum(dateDiffProbability,IncrementValue)); //dateDiffProbabilityIncrementValue);
		  					list.get(x).setTotalProbabilityScore( Double.sum( list.get(x).getTotalProbabilityScore(),IncrementValue) );  
		  				}
	  				}
	  				//authorRoleProbability
	  				else if(fieldToUpdate==6) {
		  				authorRoleProbability= list.get(x).getORIGauthorRoleProbability();		//.toLowerCase()
		  				if(authorRoleProbability==0.0) {} //same as checking for null
		  				else {
		  					list.get(x).setAuthorRoleProbability(Double.sum(authorRoleProbability,IncrementValue)); //authorRoleProbabilityIncrementValue);
		  					list.get(x).setTotalProbabilityScore( Double.sum( list.get(x).getTotalProbabilityScore(),IncrementValue) );  
		  				}
	  				}	  				
	  				//messageTypeIsReasonMessageProbabilityScore
	  				else if(fieldToUpdate==7) {
		  				messageTypeIsReasonMessageProbabilityScore= list.get(x).getORIGmessageTypeIsReasonMessageProbabilityScore();		//.toLowerCase()
		  				if(messageTypeIsReasonMessageProbabilityScore==0.0) {} //same as checking for null
		  				else 
		  					list.get(x).setMesssageTypeIsReasonMessageProbabilityScore(Double.sum(messageTypeIsReasonMessageProbabilityScore, IncrementValue)); //messageTypeIsReasonMessageProbabilityScoreIncrementValue);
		  			}
	  				//sameMsgSubAsStateTripleProbabilityScore ...was before sentenceLocationInMessageProbabilityScore
	  				/*else if(fieldToUpdate==8) {
			  			sentenceLocationInMessageProbabilityScore= list.get(x).getORIGsentenceLocationInMessageProbabilityScore();		//.toLowerCase()
		  				if(sentenceLocationInMessageProbabilityScore==0.0) {} //same as checking for null
		  				else {
		  					list.get(x).setSentenceLocationInMessageProbabilityScore(Double.sum(sentenceHintProbablity,IncrementValue));
		  					list.get(x).setTotalProbabilityScore( Double.sum( list.get(x).getTotalProbabilityScore(),IncrementValue) );  
		  				}
		  			}
	  				*/
	  				//sameMsgSubAsStateTripleProbabilityScore
	  				else if(fieldToUpdate==8) {
			  			sameMsgSubAsStateTripleProbabilityScore= list.get(x).getORIGsameMsgSubAsStateTripleProbabilityScore();		//.toLowerCase()
		  				if(sameMsgSubAsStateTripleProbabilityScore==0.0) {} //same as checking for null
		  				else {
		  					list.get(x).setSameMsgSubAsStateTripleProbabilityScore(Double.sum(sameMsgSubAsStateTripleProbabilityScore,IncrementValue)); //sameMsgSubAsStateTripleProbabilityScoreIncrementValue);
		  					list.get(x).setTotalProbabilityScore( Double.sum( list.get(x).getTotalProbabilityScore(),IncrementValue) );  
		  				}
		  			}
	  				//reasonLabelFoundUsingTripleExtractionProbabilityScore
	  				else if(fieldToUpdate==9) {
			  			reasonLabelFoundUsingTripleExtractionProbabilityScore= list.get(x).getORIGreasonLabelFoundUsingTripleExtractionProbabilityScore();		//.toLowerCase()
		  				if(reasonLabelFoundUsingTripleExtractionProbabilityScore==0.0) {} //same as checking for null
		  				else 
		  					list.get(x).setReasonLabelFoundUsingTripleExtractionProbabilityScore(Double.sum(reasonLabelFoundUsingTripleExtractionProbabilityScore,IncrementValue)); //reasonLabelFoundUsingTripleExtractionProbabilityScoreIncrementValue);
		  			}
	  				//messageContainsSpecialTermProbabilityScore
	  				else if(fieldToUpdate==10) {
			  			messageContainsSpecialTermProbabilityScore= list.get(x).getORIGmessageContainsSpecialTermProbabilityScore();		//.toLowerCase()
		  				if(messageContainsSpecialTermProbabilityScore==0.0) {} //same as checking for null
		  				else {
		  					list.get(x).setMessageContainsSpecialTermProbabilityScore(Double.sum(messageContainsSpecialTermProbabilityScore, IncrementValue)); //messageContainsSpecialTermProbabilityScoreIncrementValue);
		  					list.get(x).setTotalProbabilityScore( Double.sum( list.get(x).getTotalProbabilityScore(),IncrementValue) );  
		  				}
		  			}
	  				//prevParagraphSpecialTermProbabilityScore
	  				else if(fieldToUpdate==11) {
			  			prevParagraphSpecialTermProbabilityScore= list.get(x).getORIGprevParagraphSpecialTermProbabilityScore();		//.toLowerCase()
		  				if(prevParagraphSpecialTermProbabilityScore==0.0) {} //same as checking for null
		  				else {
		  					list.get(x).setPrevParagraphSpecialTermProbabilityScore(Double.sum(prevParagraphSpecialTermProbabilityScore, IncrementValue)); //prevParagraphSpecialTermProbabilityScoreIncrementValue);
		  					list.get(x).setTotalProbabilityScore( Double.sum( list.get(x).getTotalProbabilityScore(),IncrementValue) );  
	  					}
	  				}
		  			//negationTermPenalty
	  				else if(fieldToUpdate==12) {
		  				negationTermPenalty= list.get(x).getORIGnegationTermPenalty();		//.toLowerCase()
		  				if(negationTermPenalty==0.0) {} //same as checking for null
		  				else {
		  					list.get(x).setNegationTermPenalty(Double.sum(negationTermPenalty, IncrementValue)); //negationTermPenaltyIncrementValue);
		  					list.get(x).setTotalProbabilityScore( Double.sum( list.get(x).getTotalProbabilityScore(),IncrementValue) );  
		  				}
		  			}
	  						
	  			}
	  	    }
  		}
  		catch(Exception e){
  			System.out.println("Exception " + e.toString());  			System.out.println(StackTraceToString(e)  );
  		}
  		return list;
    }
	
	private static void assignWeights(String f, double d) throws SQLException {
		System.out.println("\t\tCalled assignWeights Function for field :  " + f);
		//for each field, get the different weights we have currently
		String query0 = "SELECT DISTINCT "+f+" from "+tablenameToLoadResultsForEvaluation+";";
		double val = 0.0, incrementValue = d; Integer round = 0;
		try {	
			PreparedStatement ps = connection.prepareStatement(query0);
			//ps.setString(0, f);	 //System.out.println("f "+f);
			// process the results
			ResultSet rs = ps.executeQuery();
			while ( rs.next() )    { //System.out.println("f "+rs.getDouble(f));
				val = rs.getDouble(f);
				//then for different weight in that field we increment by 0.1 using SQL
				//for the moment just double the values					
				String query2 = "UPDATE "+tablenameToLoadResultsForEvaluation+" SET "+f+" = "+(val + incrementValue) + " WHERE "+f+" = "+val+";";
				PreparedStatement preparedStmt2 = connection.prepareStatement(query2);	
				preparedStmt2.executeUpdate(); 					
				preparedStmt2.close();
				System.out.println("\t\tIncremented ("+f+") orig: "+val+ ", Round "+round+", new: " + (val + incrementValue) + ", increment val: "+ incrementValue);
				round++;
			}
			rs.close();			ps.close();
		}
		catch (SQLException se)  {	throw se;
		}
	}
	private static void resetWeights(String f) throws SQLException {
		System.out.println("Called Reset Function for field :  "+f);		
		try {			
			String query2 = "UPDATE "+tablenameToLoadResultsForEvaluation+" SET "+f+" = orig"+f+";";
			PreparedStatement preparedStmt2 = connection.prepareStatement(query2);	
			preparedStmt2.executeUpdate(); 					
			preparedStmt2.close();
			System.out.println("Reset ("+f+") ");
		}
		catch (SQLException se)  {	throw se;
		}
	}
	
	//Returns an unordered list of employees

    private static ArrayList<ScoresToSentences> getUnsortedEmployeeList()
    {
    	ArrayList<ScoresToSentences> list = new ArrayList<>();
    	Random rand = new Random(10);
        
        for(int i = 0; i < 5; i++)      {
        	ScoresToSentences p = new ScoresToSentences();
            p.setId(rand.nextInt(100));
            list.add(p);
        }
        return list;
    }
    
    public static String StackTraceToString(Exception ex) {
		String result = ex.toString() + "\n";
		StackTraceElement[] trace = ex.getStackTrace();
		for (int i=0;i<trace.length;i++) {
			result += trace[i].toString() + "\n";
		}
		return result;
	}
}
