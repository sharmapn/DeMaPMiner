package rankingEvaluation;

import java.io.BufferedWriter;
import java.io.FileWriter;
import java.io.IOException;
import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.ResultSetMetaData;
import java.sql.SQLException;
import java.sql.Statement;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Date;
import java.util.List;

import org.apache.commons.lang3.ArrayUtils;

import connections.MysqlConnect;
import edu.stanford.nlp.ling.Sentence;
import excelwrite.ExcelFile_Evaluation_AutoVsManual;
import GUI.helpers.GUIHelper;
import miner.process.ProcessMessageAndSentence;
import miner.process.PythonSpecificMessageProcessing;
//Feb 2019, updated to check paragraphs and messages?
import opennlp.tools.sentdetect.SentenceDetectorME;
import opennlp.tools.sentdetect.SentenceModel;
import opennlp.tools.util.Span;
import java.io.File;
import java.io.FileInputStream;
import java.io.InputStream;

//dec 2019..this is first script and a very basic on where only counts and average values of ranking are calculated - no ndcg is not calculated
//also here most of the evaluation data is written and read from database - thats why its called db based which is slower
//the other version of the same script is memory based and loads everything into memory for faster processing 

//tHIS SCRIPT IS STILL USED TO CHECK IF DYNAMICWEIGHTALLOCATION.JAVA WORKS FINE OR NOT


//Feb 2019, updated to check paragraphs and messages?
//This script is used to check the automatic extracted reasons using heuristics against the manually checked ones
//also this script is run to update the 'containsreason' column in the ml table
//oct 2018, alos update trainingdata columns, but only needed once, not always..for this set the flag 'updateTrainingDataColumns'
//just run this to make sure
//update trainingdata set label = trim(label);
public class RankingEvaluation_DBBased_FirstScriptBasicEvalMetrics {
	static PythonSpecificMessageProcessing pm = new PythonSpecificMessageProcessing();
	static String manualreasonextractionTable 		= "trainingData",				 
				  reasonCandidatesSentencesTable 	= "autoextractedreasoncandidateSentences", 
				  reasonCandidatesParagraphsTable 	= "autoextractedreasoncandidateParagraphs", 
				  reasonCandidatesMessagesTable 	= "autoextractedreasoncandidateMessages", 
				  stateTable				  		= "pepstates_danieldata_datetimestamp";
	
	static GUIHelper guih = new GUIHelper();
	static ExcelFile_Evaluation_AutoVsManual reeval        = new ExcelFile_Evaluation_AutoVsManual();
	static Connection connection;
	static 	List<Integer> listProposalReasonSentenceNotManuallyEntered = new ArrayList<Integer>();	//dec 2018
	static 	List<Integer> listProposalReasonSentenceManuallyEntered    = new ArrayList<Integer>();
	
	static boolean outputForDebug = false;
	//stopped at just the starting of 479
	
	//Feb 2019, Now this Evaluation can be done at different levels; sentence, paragraph and message
	//boolean message= false, paragaraph=false, sentence=true;
	static InputStream inputStream;
	static SentenceModel model;
	static SentenceDetectorME detector;
	
	private static void createDateDirectory(File theDir) {
		// if the directory does not exist, create it
		if (!theDir.exists()) {
//		    System.out.println("creating directory: " + theDir.getName());
		    boolean result = false;
		    try{		        theDir.mkdir();		        result = true;		    } 
		    catch(SecurityException se){	}	        //handle it		    	
		    if(result) {
//		        System.out.println("DIR created");
		    }
		}
	}
	
	public static void main(String[] args) throws SQLException, IOException { //before was args[]
		Statement stmt = null,stmt2=null;
		connections.MysqlConnect mc = new MysqlConnect();		connection = mc.connect();
		
		Date date = new Date();
		SimpleDateFormat dateFormat = new SimpleDateFormat("yyyy-MM-dd HH-mm-ss");
		String newdate = dateFormat.format(date);
		
		String fileDir  = "C:\\DeMapMiner\\results\\Reasons_evaluation\\2022\\eval";
		//String filename = "C:\\Users\\psharma\\Google Drive\\PhDOtago\\Code\\outputFiles\\postProcessResults_"  +  ".csv";		
		File theDir = new File(fileDir + newdate);	 
		//System.out.println("Output directory: " + fileDir);
		createDateDirectory(theDir);		
		int[] selectedListItems = {259}; //204,270,275,289,295,299,330,336,340,382,402,437,438,439,443,449,451,464,470,485,495,507,508,518,519,3103,3134,3139,3152}; //, }; 308, 318
		
		//feb 2019
		try {
			inputStream = new FileInputStream("C://lib//openNLP//en-sent.bin"); 
		    model = new SentenceModel(inputStream); 	    									
			detector = new SentenceDetectorME(model);  //Instantiating the SentenceDetectorME class
		}
		catch(Exception e){
			
		}
		
		
		//Run the program which will compute vales and then will write the result to database
				
		//We evaluate for all three ways: sentence, paragraph and message
		String evaluationLevels[] = {"sentences"}; //sentencesfromdistinctmessages"};//"sentences",/*"sentencesfromdistinctmessages",*/ "paragraphs","messages"}; //}; //, 
		//String evaluationLevels[] = {"messages"};
		for(String evalLevel: evaluationLevels) 
		{
			//reasonCandidatesTable=reasonCandidatesTable+evalLevel;	//change tablename
			String excelOutputfileName 				= theDir + "\\"    + "eval"+evalLevel+".xlsx", 
				   excelOutputOnlyUnmatchedfileName = theDir + "\\"    + "evalUnmatched"+evalLevel+".xlsx";
			reeval.initialiseExcelFile(excelOutputfileName);		
			
			//for wordcloud and knowledge graph
			boolean extractSentencesForWordCloud=false,updateTrainingDataColumns=true;
			FileWriter fw = new FileWriter("c:\\DeMapMiner\\datafiles\\outputFiles\\wordCloud"+evalLevel+".txt", true);
		    BufferedWriter bw = new BufferedWriter(fw);
		    
		    FileWriter fwAO = new FileWriter(theDir + "\\"    + "AllOut"+evalLevel+".txt", true);	    	BufferedWriter bwAO = new BufferedWriter(fwAO);
		    FileWriter fwSO = new FileWriter(theDir + "\\"    + "SelectedOut"+evalLevel+".txt", true);	BufferedWriter bwSO = new BufferedWriter(fwSO);
		    
			String sentenceOrParagraph = "", mid = "", dateVal = "", termsMatched = "", level = "", folderML = "",	author = "";
			Boolean reason = false;
			int arrayCounter = 0,proposalNum=0; //int recordSetCounter = 0; 
			
			//		for (int i = 0; i < 479; i++) 		{		//479
			String statesToCheck[] = {"accepted","final","rejected"};			//"accepted","final",  "final",
			
			//START WRITING HEADERS
			reeval.writeEvaluationResultsHeaderToExcelFile("Proposal","MID","State", "labelToCheck", "ManualCauseSentence", "AutoExtractedSentenceOrParagraph", "Ranking", "Location", "sentenceMatchPercent",
					"sentenceHintProbablity","sentenceLocationHintProbability","messageSubjectHintProbablityScore","dateDiffProbability","authorRoleProbability","negationTermPenalty");
			reeval.writeEvaluationResultsHeaderToExcelFile_UniqueRows("Proposal","MID","State", "labelToCheck", "ManualCauseSentence", "AutoExtractedSentenceOrParagraph", "Ranking", "Location", "sentenceMatchPercent",
					"sentenceHintProbablity","sentenceLocationHintProbability","messageSubjectHintProbablityScore","dateDiffProbability","authorRoleProbability","negationTermPenalty");
			
			Integer manualLabelCounter=0, top5=0, top10=0, top15=0,top30=0,top50=0,top100=0,outsideTop100=0,notMatched=0;
			bwAO.write("proposalNum,label,ranking,manualSentence,autosentence,probability,sentenceHintProbablity,"
					+ "messageSubjectHintProbablityScore,authorRoleProbability,sentenceLocationHintProbability,dateDiffProbability,negationTermPenalty");		
			bwAO.newLine();
			bwSO.write("proposalNum,label,ranking,manualSentence,autosentence,probability,sentenceHintProbablity,"
					+ "messageSubjectHintProbablityScore,authorRoleProbability,sentenceLocationHintProbability,dateDiffProbability,negationTermPenalty");		
			bwSO.newLine();
			//END WRITING HEADERS
			boolean foundaccepted=false; String accepted = "accepted";
			Integer finalPlusAcceptedCounter=0,finalMinusAcceptedCounter=0;
			//FIRST, WE START BY CHOOSING WHICH LABEL WE WANT TO DISCOVER	
			for(String state: statesToCheck) {
				
				double sentenceHintProbablity=0.0,sentenceLocationHintProbability=0.0,messageSubjectHintProbablityScore=0.0, dateDiffProbability=0.0, authorRoleProbability=0.0, negationTermPenalty=0.0,
					   restOfParagraphProbabilityScore=0.0, messageLocationProbabilityScore=0.0, 
					   messageTypeIsReasonMessageProbabilityScore=0.0,sentenceLocationInMessageProbabilityScore=0.0,sameMsgSubAsStateTripleProbabilityScore=0.0,
					   reasonLabelFoundUsingTripleExtractionProbabilityScore=0.0,messageContainsSpecialTermProbabilityScore=0.0,prevParagraphSpecialTermProbabilityScore=0.0;
								
				top5=top10=top15=top30=top50=top100=outsideTop100=notMatched=manualLabelCounter=0;
				//SECOND get all peps which have that label from committed state table
				//Jan we are assuming here that all main states are captured in the commit files, but that is not the case, look at PEP 510
				String sqlCommittedStates = "SELECT pep,messageid,author,date2,email, datetimestamp from " + stateTable + " where email like '%"+state+"%' order by pep asc;" ; 	//and pep = 311		pep = 397  
				Statement stmtCS = connection.createStatement(); 		ResultSet rsCS = stmtCS.executeQuery(sqlCommittedStates); // date asc
				int rowCountcCS = guih.returnRowCount(sqlCommittedStates); //System.out.println("\tManually Extracted Reasons records found: " + rowCount2);
				//then check if a manual sentence entry is found
				//if not we output that we were not abkle to manually find the reason sentence and leave that instance from out counting
				List<Integer> listOfProposalsNotRecorded = new ArrayList<Integer>();	
				theouterloop:
				while (rsCS.next()) {	//FOR EACH PROPOSAL				
					int pNum = rsCS.getInt("pep");					String stateEx = rsCS.getString("email");
					//System.out.println("New Proposal : " + pNum + " Label: "+state);
					//if that proposal has accepted state, we dont look for 'final'
					if(state.equals("final")) {
						String sqlCommittedStatesA = "SELECT pep from " + stateTable + " where pep = "+pNum+" and email like '%"+accepted+"%' ;" ; 	//and pep = 311		pep = 397  
						Statement stmtCSA = connection.createStatement(); 		ResultSet rsCSA = stmtCSA.executeQuery(sqlCommittedStatesA); // date asc
						int rowCountcCSA = guih.returnRowCount(sqlCommittedStatesA);
						stmtCSA.close();
						//if 'accepted' state is found in that propsal, we dont proceed
						if (rowCountcCSA >0) {
//							System.out.println("Accepted found for Final State for Proposal : " + pNum + " Label: "+state);
							finalPlusAcceptedCounter++; continue;
						} else {	
							finalMinusAcceptedCounter++; 
//							System.out.println("Accepted Not found for Final State for Proposal : " + pNum + " Label: "+state);
						}
						//if(stateEx.toLowerCase().contains("accepted"))
						//	foundaccepted=true;
						//if(state.equals("final") && foundaccepted)
						//	continue;	//dont check final if accepted state exists
					}
					
					//feb 2019, if we want to limit to some selected peps and dont want them included in the results
//					if (!ArrayUtils.contains( selectedListItems, pNum ) ) 
//					    continue;
					
//					System.out.println("Proposal : " + pNum + " State Found: "+state);
					if(outputForDebug)
						System.out.println("New Proposal : " + pNum + " Label: "+state);
					//THIRD GET MANUALLY EXTRACTED CAUSE SENTENCE FOR THAT PEP and Label
					//IF a corresponding manual sentence for that label or sentence is not entered, then we displaya dn dont count
					String sqlManual = "SELECT pep,state,causeSentence, effectSentence, label, dmconcept from " + manualreasonextractionTable + " "
							+ " where consider = 1 and state like '%"+state+"%' and pep = "+pNum+" and LENGTH(causesentence) > 1 order by pep ;" ; 	//and pep = 311
					stmt2 = connection.createStatement(); 		ResultSet rsB = stmt2.executeQuery(sqlManual); // date asc
					Integer rowCount2 = guih.returnRowCount(sqlManual); //System.out.println("\tManually Extracted Reasons records found: " + rowCount2);
					if (rowCount2==0) listProposalReasonSentenceNotManuallyEntered.add(pNum);
					else if (rowCount2>0) listProposalReasonSentenceManuallyEntered.add(pNum);						
					String manualLabel="",causeSentence="",effectSentence="",label="",causeSubcategory="",permanentMatchSentence="";
					//FOR EACH PROPOSAL ... GET ALL records but concentrate on the top ones
					boolean sentenceLabelFound = false; 
					String sqlAutomatic="", messageOrSection="";
					Integer messageID=null, ranking=1,id=0, datediff=0,anyRanking=1,rowCount=0,listOfMessageIDsForRankingCounter=1,calculatedRanking=0; 
					String headers[]= {"BDFL Pronouncement","Rejection Notice","Rejection","PEP Rejection","Pronouncement","Acceptance"};
					List<String> automaticSentenceOrParagraph = null;
					List<Integer> listOfMessageIDsForRanking = new ArrayList<Integer>();	//mar 2019, to keep track of which mids have been processed - only for 'sentencesfromdistinctmessages'
					//GET ALL MATCHES
					boolean matched=false, updated = false; //updated if for making sure we update the training data row once only, that being the highest ranked row details, which will happen as the sql resultset is ranked by  highest to lowest results
					sentenceHintProbablity=sentenceLocationHintProbability=restOfParagraphProbabilityScore=messageLocationProbabilityScore= 
							messageTypeIsReasonMessageProbabilityScore=sentenceLocationInMessageProbabilityScore=sameMsgSubAsStateTripleProbabilityScore=
							reasonLabelFoundUsingTripleExtractionProbabilityScore=messageContainsSpecialTermProbabilityScore=prevParagraphSpecialTermProbabilityScore=
							messageSubjectHintProbablityScore=dateDiffProbability=authorRoleProbability=negationTermPenalty=0.0;
					String autosentenceOrParagraphOrMessage="",email="",autolabel="",loc="",probability="";
					
					while (rsB.next()) { //for each state we want to check, get all proposals //currDate.before(goToDate)) {
//						System.out.println("\tManual found records: ");
						sentenceLabelFound=true;
						proposalNum = rsB.getInt("pep");						manualLabel = rsB.getString("state");		causeSentence = rsB.getString("causeSentence");	
						effectSentence = rsB.getString("effectSentence"); 		label = rsB.getString("label");				causeSubcategory = rsB.getString("dmconcept");
//$						recordSetCounter++;
						
						//process sentence
						causeSentence = pm.removeUnwantedText(causeSentence);		//a lot of things are removed here which may be important ..like punctuation for finding out of sentence is code or not		
						causeSentence = pm.removeLRBAndRRB(causeSentence);
						causeSentence = causeSentence.replaceAll("\\p{P}", " "); 	//remove all punctuation ..maybe we can use this here (term matching for candidates) but not in CIE triple extraction as it wmay need commas and other punctuation
						causeSentence = causeSentence.replaceAll("\\s+", " ").trim(); //remove double spaces and trim
						
						//append the sentence into a file for wordcloud and knowledge graph 					
					    if(extractSentencesForWordCloud) {
					    	if(causeSentence==null || causeSentence.isEmpty()) {}
					    	else  	{
					    		bw.write(causeSentence);				    bw.newLine(); continue; //continue to next record
					    	}
					    }
						//aug 2018 updated...for sentences starting with a heading like 'Rejection Notice', we skip these terms 
						//check manual sentence after two terms						
						for (String s: headers) {
							if(causeSentence.startsWith(s))
								causeSentence = causeSentence.replaceAll(s, "");
						}					
						causeSentence = causeSentence.trim();
						if(outputForDebug)
							System.out.println("\tChecking New Manual Cause Sentence a: "+causeSentence);	// + "("+proposalNum+")");	
	//	$				System.out.println("\tManual Effect Sentence a: "+effectSentence);	// + "("+proposalNum+")");	
						if(causeSentence == null || causeSentence.equals("") || causeSentence.isEmpty() || causeSentence.length()==0) 
						{ //System.out.println("Label exists, but Empty Sentence"); 
							continue;	
						}
						//System.out.println(); //else {				}
						List<String> manualCauseSentenceTerms = Arrays.asList(causeSentence.toLowerCase().split(" ")); //causeSentence.toLowerCase().split(" ");
						
						sqlAutomatic="";
						if(evalLevel.equals("sentences")) {	//GET THE TOP X sentences from automatic reason extraction based on total probability 
							sqlAutomatic = "SELECT id, @curRank := @curRank + 1 AS rank, proposal,messageid,dateValue, sentence as text,termsMatched, probability, label, authorRole, location,datediff, messageTypeIsReasonMessage as RM, messageType as MT, "
								+ "  (sentenceHintProbablity+sentenceLocationHintProbability+restOfParagraphProbabilityScore+messageLocationProbabilityScore+messageSubjectHintProbablityScore+dateDiffProbability+authorRoleProbability+"
								+ "  +messageTypeIsReasonMessageProbabilityScore+sentenceLocationInMessageProbabilityScore+sameMsgSubAsStateTripleProbabilityScore"
								+ "	 +reasonLabelFoundUsingTripleExtractionProbabilityScore+messageContainsSpecialTermProbabilityScore+prevParagraphSpecialTermProbabilityScore-negationTermPenalty) as TotalProbability, "
								//+ "  sentenceHintProbablity,sentenceLocationHintProbability,messageSubjectHintProbablityScore,negationTermPenalty,dateDiffProbability,authorRoleProbability,restOfParagraphProbabilityScore "
								+ "  sentenceHintProbablity,sentenceLocationHintProbability,restOfParagraphProbabilityScore,messageLocationProbabilityScore,messageSubjectHintProbablityScore,dateDiffProbability,authorRoleProbability, "  
								+ "  messageTypeIsReasonMessageProbabilityScore,sentenceLocationInMessageProbabilityScore,sameMsgSubAsStateTripleProbabilityScore, " 
								+ "  reasonLabelFoundUsingTripleExtractionProbabilityScore,messageContainsSpecialTermProbabilityScore,prevParagraphSpecialTermProbabilityScore,negationTermPenalty "
								//+ "  NULL as email "
								+ "  from "+reasonCandidatesSentencesTable+", (SELECT @curRank := 0) r  where "  //location LIKE '%sentence%' and   //autoextractedreasoncandidatesentences_dec
								+ "	 (label = '"+state+"' OR label like 'Status : "+state+"')  and "		//if we ned to match commit labels
//								+ "  messageid = 318242 and "
								+ "  proposal = "+ proposalNum + " order by label asc, TotalProbability desc, dateValue desc ";
						}	
						//we use teh same ordering from the sentence level results above, but add email field for checking..so that the first instance of the message will be checked
						//march 2019, we do it through arraylists now
   						else if(evalLevel.equals("sentencesfromdistinctmessages")) {
   						 	sqlAutomatic = "SELECT id, @curRank := @curRank + 1 AS rank, proposal,messageid,dateValue, (select analysewords from allmessages w where w.messageid = x.messageid limit 1) as text,"
//									+ "  (select email from allmessages w where w.messageid = x.messageid limit 1) as email, "   //sometimes the analysewords column returns empty
									+ "  termsMatched, probability, label, authorRole, location,datediff, messageTypeIsReasonMessage as RM, messageType as MT, "
									
									+ "  (sentenceHintProbablity+sentenceLocationHintProbability+restOfParagraphProbabilityScore+messageLocationProbabilityScore+messageSubjectHintProbablityScore+dateDiffProbability+authorRoleProbability+"
									+ "  +messageTypeIsReasonMessageProbabilityScore+sentenceLocationInMessageProbabilityScore+sameMsgSubAsStateTripleProbabilityScore"
									+ "	 +reasonLabelFoundUsingTripleExtractionProbabilityScore+messageContainsSpecialTermProbabilityScore+prevParagraphSpecialTermProbabilityScore-negationTermPenalty) as TotalProbability, "
									//+ "  sentenceHintProbablity,sentenceLocationHintProbability,messageSubjectHintProbablityScore,negationTermPenalty,dateDiffProbability,authorRoleProbability,restOfParagraphProbabilityScore "
									+ "  sentenceHintProbablity,sentenceLocationHintProbability,restOfParagraphProbabilityScore,messageLocationProbabilityScore,messageSubjectHintProbablityScore,dateDiffProbability,authorRoleProbability, "  
									+ "  messageTypeIsReasonMessageProbabilityScore,sentenceLocationInMessageProbabilityScore,sameMsgSubAsStateTripleProbabilityScore, " 
									+ "  reasonLabelFoundUsingTripleExtractionProbabilityScore,messageContainsSpecialTermProbabilityScore,prevParagraphSpecialTermProbabilityScore,negationTermPenalty "	
									
									+ "  from "+reasonCandidatesSentencesTable+" x, (SELECT @curRank := 0) r  where "  //location LIKE '%sentence%' and   //autoextractedreasoncandidatesentences_dec
									+ "	 (label = '"+state+"' OR label like 'Status : "+state+"')  and "		//if we ned to match commit labels
//									+ "  messageid = 318242 and "
									+ "  proposal = "+ proposalNum + " "
									//+ "  group by messageid " //main difference 
									+ "  order by label asc, TotalProbability desc, dateValue desc ";
							
						}	
						
						else if(evalLevel.equals("paragraphs")) {
							//Paragraph  ..below fields still to add 
							/* 	paragraphLocationInMessageProbabilityScore,
								sameMsgSubAsStateTripleProbabilityScore, reasonLabelFoundUsingTripleExtractionProbabilityScore,messageContainsSpecialTermProbabilityScore
							*/
							sqlAutomatic = "SELECT id, @curRank := @curRank + 1 AS rank, proposal,messageid,dateValue, paragraph as text,termsMatched, probability, label, authorRole, location,datediff, messageTypeIsReasonMessage as RM, messageType as MT, "
								+ "  (paragraphHintProbablity+paragraphLocationHintProbability+nearbyParagraphsProbabilityScore+messageLocationProbabilityScore+messageSubjectHintProbablityScore "  
								+ "  +dateDiffProbability+authorRoleProbability+messageTypeIsReasonMessageProbabilityScore+paragraphLocationInMessageProbabilityScore+sameMsgSubAsStateTripleProbabilityScore "  
								+ "	 +reasonLabelFoundUsingTripleExtractionProbabilityScore+messageContainsSpecialTermProbabilityScore+prevParagraphSpecialTermProbabilityScore-negationTermPenalty) as TotalProbability, "
								+ "  paragraphHintProbablity,paragraphLocationHintProbability,nearbyParagraphsProbabilityScore,messageSubjectHintProbablityScore,negationTermPenalty,dateDiffProbability,authorRoleProbability, "
								+ "  messageTypeIsReasonMessageProbabilityScore,messageLocationProbabilityScore "
//								+ "  '' as email "
								+ "  from "+reasonCandidatesParagraphsTable+", (SELECT @curRank := 0) r where "  //location LIKE '%sentence%' and   //autoextractedreasoncandidatesentences_dec
								+ "	 (label = '"+state+"' OR label like 'Status : "+state+"')  and "		//if we ned to match commit labels
//								+ "  messageid = 318242 and "
								+ "  proposal = "+ proposalNum + " order by label asc, TotalProbability desc, dateValue desc ";
							
						}						
						else if(evalLevel.equals("messages")) {			//not 'email' field but 'analysewords' field, as sometimes email fields are not complete , For example, check last para in pep 338, MID 144043,  
							sqlAutomatic = "SELECT id, @curRank := @curRank + 1 AS rank, proposal,messageid,dateValue, (select analysewords from allmessages w where w.messageid = x.messageid limit 1) as text,"
								+ "  termsMatched, probability, label, authorRole, location,datediff, messageTypeIsReasonMessage as RM,messageType as MT, "
								+ "  (messageLocationProbabilityScore+messageSubjectHintProbablityScore+dateDiffProbability+authorRoleProbability+"
								+ "  +messageTypeIsReasonMessageProbabilityScore+sameMsgSubAsStateTripleProbabilityScore"
								+ "	 +messageContainsSpecialTermProbabilityScore) as TotalProbability, "  //removed 'reasonLabelFoundUsingTripleExtractionProbabilityScore'
								+ "  messageTypeIsReasonMessageProbabilityScore,messageLocationProbabilityScore,messageSubjectHintProbablityScore,dateDiffProbability,authorRoleProbability, "
								+ "  sameMsgSubAsStateTripleProbabilityScore, messageContainsSpecialTermProbabilityScore "
//								+ "  '' as email "
								+ "  from "+reasonCandidatesMessagesTable+" x, (SELECT @curRank := 0) r where "  //location LIKE '%sentence%' and   //autoextractedreasoncandidatesentences_dec
								+ "	 (label = '"+state+"' OR label like 'Status : "+state+"')  and "		//if we ned to match commit labels
//								+ "  messageid = 117105 and "
								+ "  proposal = "+ proposalNum + " order by label asc, TotalProbability desc, dateValue desc ";							
						}
						

						stmt = connection.createStatement(); 	
						ResultSet rsA = stmt.executeQuery(sqlAutomatic); // date asc
						rsA = stmt.executeQuery(sqlAutomatic); 
						rowCount = guih.returnRowCount(sqlAutomatic);			
	//					System.out.println("\tAutomatic candidate records found : " + rowCount + " for label: "+ labels + " pep: "+ proposalNum);			
						ranking=1; 	//Integer	permanentMatch=0;	//Integer	permanentRanking=0;	//check all records one by one
						permanentMatchSentence="";
						//List<String> automaticSentenceOrParagraph = null;
						//GET ALL MATCHES
						matched=false; updated = false; //updated if for making sure we update the training data row once only, that being the highest ranked row details, which will happen as the sql resultset is ranked by  highest to lowest results
						
						sentenceHintProbablity=0.0; sentenceLocationHintProbability=0.0; messageSubjectHintProbablityScore=0.0; dateDiffProbability=0.0; authorRoleProbability=0.0; negationTermPenalty=0.0;
						id=0; datediff=0;						
						anyRanking=1; //sentenceCounterFromResults=1, paragraphCounterFromResults=1, messageCounterFromResults=1; //ranking starts at 1, not 0
						calculatedRanking=0; // duplicated sentences are skipped, therfore we need this counter
						listOfMessageIDsForRanking.clear();//clear this list for every proposal
						listOfMessageIDsForRankingCounter=1; //we make sure the value is reset to 1 here
						
						//we dont want to consider duplicate sentences or paragraphs
						List<String> allSentencesForProposalList = new ArrayList<String>();
						//System.out.print("\t new while "+ anyRanking); 
						
						//ResultSetMetaData meta = rsA.getMetaData();
			            //Object[] data = new Object[meta.getColumnCount()];
						
						
						//March 2019, arraylists
						
						
						// one pep may have multiple reason sentences
						// march 2019 - criteria changed temporarily, ( rsA.next()  ) 
						
						
						

						outerloop: //Feb 2019, as soon as we find a match, we break out of checking the remaining records, whether it be sentences, paragraphs or messages
						while (rsA.next()) {  //System.out.println("\t found records: "); //currDate.before(goToDate)) {
							//for (int index = 1; index <= meta.getColumnCount(); index++) {
							//	 	System.out.println("Column " + index + " is named " + meta.getColumnName(index));
				            //        data[index - 1] = rsA.getObject(meta.getColumnName(index));
			                //} 
							
							id = rsA.getInt("id");		datediff = rsA.getInt("datediff");
							anyRanking = rsA.getInt("rank");       //System.out.print("\t ranking "+ anyRanking); 		
							proposalNum = rsA.getInt("proposal");						mid = rsA.getString("messageid");				dateVal = rsA.getString("dateValue");
							autosentenceOrParagraphOrMessage = rsA.getString("text");	termsMatched = rsA.getString("termsMatched");	probability = rsA.getString("TotalProbability");
							autolabel = rsA.getString("label");					author = rsA.getString("authorRole");					loc = rsA.getString("location");
//							email = rsA.getString("email"); 
							//System.out.println("SQL Ranking " + anyRanking + " mid " + mid + " totalprobability "+probability );
							//probabilities
							if(evalLevel.equals("sentences") || evalLevel.equals("sentencesfromdistinctmessages") ) {
								sentenceHintProbablity	= rsA.getDouble("sentenceHintProbablity");							sentenceLocationHintProbability = rsA.getDouble("sentenceLocationHintProbability");
								messageSubjectHintProbablityScore	= rsA.getDouble("messageSubjectHintProbablityScore");	dateDiffProbability = rsA.getDouble("dateDiffProbability");
								authorRoleProbability	= rsA.getDouble("authorRoleProbability");							negationTermPenalty = rsA.getDouble("negationTermPenalty");
								//march 2019																													// messageLocationProbabilityScore
								restOfParagraphProbabilityScore= rsA.getDouble("restOfParagraphProbabilityScore"); messageLocationProbabilityScore= rsA.getDouble("messageLocationProbabilityScore"); 
								messageTypeIsReasonMessageProbabilityScore= rsA.getDouble("messageTypeIsReasonMessageProbabilityScore");
								sentenceLocationInMessageProbabilityScore= rsA.getDouble("sentenceLocationInMessageProbabilityScore"); 
								sameMsgSubAsStateTripleProbabilityScore= rsA.getDouble("sameMsgSubAsStateTripleProbabilityScore");
								reasonLabelFoundUsingTripleExtractionProbabilityScore= rsA.getDouble("reasonLabelFoundUsingTripleExtractionProbabilityScore");
								messageContainsSpecialTermProbabilityScore= rsA.getDouble("messageContainsSpecialTermProbabilityScore");
								prevParagraphSpecialTermProbabilityScore= rsA.getDouble("prevParagraphSpecialTermProbabilityScore");
							}
							messageID = Integer.valueOf(mid);
						
							//we store all candidates in arraylist of sentences, so that we can check each sentence - even for paragraphs
							List<EvalPair> allCandidateSentencesList = new ArrayList<EvalPair>(); //we store all candidate sentences regardless of if from sentence table, paragraph or a message
							
							//check if sentence exists, if so continue; 
							
							if(evalLevel.equals("sentences") || evalLevel.equals("sentencesfromdistinctmessages") ||  evalLevel.equals("paragraphs")) {	
								if(allSentencesForProposalList.contains(autosentenceOrParagraphOrMessage))
								{	//System.out.println("\t if continue ");
									continue;		}	//go to next record
								else {	//System.out.println("\t else 1 ");
									allSentencesForProposalList.add(autosentenceOrParagraphOrMessage);   
									calculatedRanking++;
									anyRanking = calculatedRanking; 	//we assign it so that it can be used....but we dont do this for messages - that used the ranking returned from SQL
								}
							}
							
							if(evalLevel.equals("sentences")) {									
								EvalPair ev =  new EvalPair(anyRanking,messageID,autosentenceOrParagraphOrMessage);
								allCandidateSentencesList.add(ev);	
								//sentenceCounterFromResults++;	//increment these values as they are results within the while loop					
							}
															
							else if(evalLevel.equals("paragraphs")) {								
								//split into sentences
								Span[] spans = detector.sentPosDetect(autosentenceOrParagraphOrMessage);   //Detecting the position of the sentences in the paragraph  
							      
								//Printing the sentences and their spans of a paragraph 
								for (Span span : spans) {        
									String CurrentSentenceString = autosentenceOrParagraphOrMessage.substring(span.getStart(), span.getEnd());
									CurrentSentenceString = pm.removeUnwantedText(CurrentSentenceString);		//a lot of things are removed here which may be important ..like punctuation for finding out of sentence is code or not		
									CurrentSentenceString = pm.removeLRBAndRRB(CurrentSentenceString);
									String sentenceString  = CurrentSentenceString;
									sentenceString = sentenceString.replaceAll("\\p{P}", " "); //remove all punctuation ..maybe we can use this here (term matching for candidates) but not in CIE triple extraction as it wmay need commas and other punctuation
									sentenceString = sentenceString.replaceAll("\\s+", " ").trim(); //remove double spaces and trim
									if(sentenceString.startsWith("<") && sentenceString.endsWith(">")) {
										System.out.println("\n sentenceString.startsWith < && sentenceString.endsWith > continue "); continue;
									}//	System.out.println("\n here 9");
									
									//allCandidateSentencesList.add(sentenceString);  //add to list
									EvalPair ev =  new EvalPair(anyRanking,messageID,sentenceString);
									allCandidateSentencesList.add(ev);	//increment these values as they are results within the while loop
								}
								//paragraphCounterFromResults++;
							}							
							else if(evalLevel.equals("messages") || evalLevel.equals("sentencesfromdistinctmessages")) {  //march 2019
								if(outputForDebug)
									System.out.print("\t Processing Proposal: "+proposalNum+" Label: "+autolabel+" Message: " + mid + " evalLevel: "+evalLevel+ " ranking "+ anyRanking);
								
								//march 2019, in sentence level analysis we look at first instance of message, if already in list, we dont increment ranking
								if(evalLevel.equals("sentencesfromdistinctmessages")) {
									//System.out.println("\n  first stage autosentenceOrParagraphOrMessage: "+autosentenceOrParagraphOrMessage.substring(0, 100));	
									if(listOfMessageIDsForRanking.contains(messageID)) {	
										if(outputForDebug) 
										System.out.println("\tcontinue: " + messageID); 	continue;
									} //dont add to list
									else { 
										if(outputForDebug) 
											System.out.print("\t\t  else2: " + messageID); //add to list
										if(autosentenceOrParagraphOrMessage==null)
											autosentenceOrParagraphOrMessage = email;
										//System.out.println("\n  adding autosentenceOrParagraphOrMessage: "+autosentenceOrParagraphOrMessage.substring(0, 100));	
//										EvalPair ev =  new EvalPair(listOfMessageIDsForRankingCounter,messageID,autosentenceOrParagraphOrMessage);
//										allCandidateSentencesList.add(ev);						
										listOfMessageIDsForRanking.add(messageID);	
										listOfMessageIDsForRankingCounter++; //we increment the counter
										anyRanking = listOfMessageIDsForRankingCounter;
									}
									
								}
								if(outputForDebug)
								System.out.println("\t new rank : " + anyRanking);
								
								//split into paragraphs
								//FINALLY DECIDED WE JUST STRIP THESE CHARACTERS OFF..BEST SOLUTION. We check for states and special terms before as they contain plus (+) signs which are removed here or else they wont be captured
								//this is only for manual reason extraction and wont be part of automated process as here we look at entire messge but in automated proces we look at analyse words only 
								messageOrSection = autosentenceOrParagraphOrMessage;
								/*messageOrSection = messageOrSection.replaceAll("\r?\n\\+","\r\n");		messageOrSection = messageOrSection.replaceAll("\r?\n\\-","\r\n");
								messageOrSection = messageOrSection.replaceAll("\r?\n>","\r\n");		messageOrSection = messageOrSection.replaceAll("\r?\n >","\r\n");
								messageOrSection = messageOrSection.replaceAll("\r?\n>>","\r\n");		messageOrSection = messageOrSection.replaceAll("\r?\n> >","\r\n");	
								messageOrSection = messageOrSection.replaceAll("\r?\n> > >","\r\n");	messageOrSection = messageOrSection.replaceAll("\r?\n ","\r\n");
								*/
								
								//START PARAGRAPH LEVEL
								String[] paragraphs = messageOrSection.split("\\r?\\n\\r?\\n"); 
								//nov 2018, remove empty or null paragraphs - those that are just empty lines
								paragraphs = removeNullOrEmpty(paragraphs);								
								Integer pcounter=0;
								
								for(String para: paragraphs) { //System.out.println("Paragraph: "+pcounter + " [" + para +"]");
									if(para.equals("") || para.trim().length() <1) 
									{	 continue; }
									
									if (pcounter >= 200)	//som,e messages like 328159 have code and therefore about 10,000 paragraphs, and 356987 have lots of paragraphs 
										break;
//XXXX Check maybe not good for checkin messages where a message will have several peps in different sections									
									//split into sentences
									Span[] spans = detector.sentPosDetect(para);   //Detecting the position of the sentences in the paragraph
									//Printing the sentences and their spans of a paragraph 
									for (Span span : spans) {        
										String CurrentSentenceString = para.substring(span.getStart(), span.getEnd());
										CurrentSentenceString = pm.removeUnwantedText(CurrentSentenceString);		//a lot of things are removed here which may be important ..like punctuation for finding out of sentence is code or not		
										CurrentSentenceString = pm.removeLRBAndRRB(CurrentSentenceString);
										String sentenceString  = CurrentSentenceString;
										sentenceString = sentenceString.replaceAll("\\p{P}", " "); //remove all punctuation ..maybe we can use this here (term matching for candidates) but not in CIE triple extraction as it wmay need commas and other punctuation
										sentenceString = sentenceString.replaceAll("\\s+", " ").trim(); //remove double spaces and trim
										if(sentenceString.startsWith("<") && sentenceString.endsWith(">")) {
											System.out.println("\n sentenceString.startsWith < && sentenceString.endsWith > continue "); continue;
										}//	System.out.println("\n here 9");
										//allCandidateSentencesList.add(sentenceString);  //add to list
										
										EvalPair ev =  new EvalPair(anyRanking,messageID,sentenceString);
										allCandidateSentencesList.add(ev);										
									}
									pcounter++; 
								}
								 //messageCounterFromResults++;	//increment these values as they are results within the while loop
							}
							
							//Feb 2019, now we have all sentences from sentence/paragraph/all sentences from a message
							//for all sentences found in sentences/paragraphs/messages, we check, as soon as one sentence is matched we exit
							for(EvalPair ev: allCandidateSentencesList) { 
								
								//untested..trying cos of pep 345 only
								autosentenceOrParagraphOrMessage = ev.getSentence();
								//ranking is already stored for each sentence based on if its sentence, paragraph or message
								ranking = ev.getID(); Integer msgid = ev.getMessageID();
								//System.out.println("Ranking " + ranking + " msgid " + msgid);
								
/*								if(autosentenceOrParagraphOrMessage.toLowerCase().startsWith("after"))								
									System.out.println("\t\tsentenceString:  " + autosentenceOrParagraphOrMessage);
								else
									continue; 
*/								if(autosentenceOrParagraphOrMessage==null) {
									System.out.println("\t\tmid "+mid+"sentenceString:  " + autosentenceOrParagraphOrMessage);
									autosentenceOrParagraphOrMessage = ""; //QUICK AND DIRTY
								}
								autosentenceOrParagraphOrMessage = autosentenceOrParagraphOrMessage.replaceAll("\\p{P}", " "); //remove all punctuation ..maybe we can use this here (term matching for candidates) but not in CIE triple extraction as it wmay need commas and other punctuation
								autosentenceOrParagraphOrMessage = autosentenceOrParagraphOrMessage.replaceAll("\\s+", " ").trim(); //remove double spaces and trim
								autosentenceOrParagraphOrMessage = autosentenceOrParagraphOrMessage.replaceAll("\'","").trim();						
								autosentenceOrParagraphOrMessage = autosentenceOrParagraphOrMessage.replaceAll(" n t ","n t ");//change 'There has n t been a ' to 'There hasn t been a '
								
								if(autosentenceOrParagraphOrMessage==null || autosentenceOrParagraphOrMessage.isEmpty() || autosentenceOrParagraphOrMessage.length()==0 ) {
									continue;
								}
								else {
									if(autosentenceOrParagraphOrMessage.contains(" ")) 
										automaticSentenceOrParagraph =  Arrays.asList(autosentenceOrParagraphOrMessage.toLowerCase().split(" "));	//autosentenceOrParagraphOrMessage.toLowerCase().split(" ");				
									else {
										automaticSentenceOrParagraph = new ArrayList<String>();
										automaticSentenceOrParagraph.add(autosentenceOrParagraphOrMessage);
									}
									//System.out.println("\tChecking against ranking: "+ranking+", Automatic Sentence ("+sentenceOrParagraph+") ");
									List<String> commonTerms = findCommonElement(automaticSentenceOrParagraph,manualCauseSentenceTerms);
									//if length of common terms > 70% of manual extracted sentence
									//if(Arrays.asList(automaticSentenceOrParagraph).containsAll(Arrays.asList(causeSentenceTerms))) {
									if(outputForDebug)
										System.out.println("\t\tAutomatic label ("+autolabel+") "+evalLevel+" ("+autosentenceOrParagraphOrMessage+") ");
									bwAO.write(proposalNum+","+state + ","+ ranking+ ","+causeSentence+"," +autosentenceOrParagraphOrMessage+","+probability+ "," +sentenceHintProbablity
											+ "," + messageSubjectHintProbablityScore + "," + authorRoleProbability + "," + sentenceLocationHintProbability + "," + dateDiffProbability + "," + negationTermPenalty);
									bwAO.newLine();
									if(outputForDebug) {
										System.out.print("\t\tcommonTermsSize: "+ commonTerms.size() + " manualCauseSentenceTerms.size(): "+ manualCauseSentenceTerms.size());  
										printList(commonTerms);
									}
									
									float  matchPercent = (commonTerms.size() * 100.0f /manualCauseSentenceTerms.size())  ;
									//get first three terms in both and make sure they are not numbers and punctuation and same in both							 
									boolean wordOrdersame = checkWordOrder(automaticSentenceOrParagraph,manualCauseSentenceTerms,3);
//									System.out.println("\t\tmatchPercent: "+ matchPercent + " wordOrdersame: "+wordOrdersame ); 
									if(matchPercent > 60 && wordOrdersame ) { // && wordOrdersame || proposalNum==369) {	//
										//permanentMatch = match;	
										//permanentMatchSentence = autosentenceOrParagraphOrMessage; permanentRanking = ranking;
																				
										if(outputForDebug)
											System.out.println("\t\t\tMatched Ranking ("+ranking+") MID ("+msgid+"): " + ranking + ", "+evalLevel+" terms percent match ("+matchPercent+") Found Automatic "+evalLevel+" ("+autosentenceOrParagraphOrMessage+")");
										bwAO.write(proposalNum+","+state+","+ label + ","+ ranking+ ","+causeSentence+"," +autosentenceOrParagraphOrMessage+","+probability+ "," +sentenceHintProbablity
												+ "," + messageSubjectHintProbablityScore + "," + authorRoleProbability + "," + sentenceLocationHintProbability + "," + dateDiffProbability + "," + negationTermPenalty);
										bwAO.newLine();
										bwSO.write(proposalNum+","+state +","+ label +","+ ranking+ ","+causeSentence+"," +autosentenceOrParagraphOrMessage+","+probability+ "," +sentenceHintProbablity
												+ "," + messageSubjectHintProbablityScore + "," + authorRoleProbability + "," + sentenceLocationHintProbability + "," + dateDiffProbability + "," + negationTermPenalty);
										bwSO.newLine();
										//					System.out.println("\tcommonTermsSize: "+ commonTerms.size() +" causeSentenceTerms.length: "+causeSentenceTerms.length); printList(commonTerms); 
//$$									//update row class column for machine learning , if sentence is matched
										//feb 2020...we make sure this is working
										if(evalLevel.equals("sentences")) {
											
											//oct 2018, we alos can update training data row here
											if(updateTrainingDataColumns && !updated) {											
												updateRowForMachineLearning(id,1,ranking,ranking);
												//trainingdata table
												updateTrainingDataTable(id,Integer.valueOf(mid),datediff,proposalNum,state,causeSentence) ; //dec 2018
											}
										}
										
										//HOW MANY TIMES A SENTENCE HAS BEEN MATCHED - WITH DIFFERENT RANKINGS
										
										//break; //exit searching
										reeval.writeEvaluationResultsToExcelFile(proposalNum,msgid, state,label, causeSentence, autosentenceOrParagraphOrMessage, ranking, loc, matchPercent,
												sentenceHintProbablity,sentenceLocationHintProbability,messageSubjectHintProbablityScore,dateDiffProbability,authorRoleProbability,negationTermPenalty);
										//if it wasnt matched before, and as soon as rank found, we write only first ranking match record (above) in a separate excel sheet
										if(!matched) {	
											reeval.writeEvaluationResultsToExcelFile_UniqueRows(proposalNum,msgid, state,label, causeSentence, autosentenceOrParagraphOrMessage, ranking, loc, matchPercent,
													sentenceHintProbablity,sentenceLocationHintProbability,messageSubjectHintProbablityScore,dateDiffProbability,authorRoleProbability,negationTermPenalty);
											if(ranking <= 5) top5++;
											else if (ranking > 5 && ranking <=10) top10++;			else if	(ranking > 10 && ranking <=15) top15++;
											else if	(ranking > 15 && ranking <=30) top30++;			else if	(ranking > 30 && ranking <=50) top50++;
											else if	(ranking > 50 && ranking <=100) top100++;
											else if (ranking > 100) { outsideTop100++; }
										}
										matched=true;	
										break outerloop;	//Feb 2019, as soon as we find a match, we break out of checking the remaining records, whether it be sentences, paragraphs or messages	
															//dec 2018, as soon as first match is found..we dont want to match all matches of same sentence which are below in order
									}else {
		//								System.out.println("\t\tNOT MATCHED: "+autosentenceOrParagraphOrMessage);
									}
//									recordSetCounter++;	
									//if(evalLevel.equals("sentences")) {  //if we are checking sentences, ranking value is increased after every sentence,
									//	ranking++;						//paragraph and message level ranking is incremented differently
									//}
								} //end if sentence not empty	
							} //end checking for each sentence in canididate list
							allCandidateSentencesList.clear();
						} //end for each proposal all results - all sentences from sentence table, paragraph or messaages 
						
						
						//March 2019...commented
						stmt.close();
						
						if(!matched) { //FOR EACH MANUAL SENTENCE IN A PROPOSAL, after going through all probable results for a proposal, if no match found we insert no match
							reeval.writeEvaluationResultsToExcelFile(proposalNum,0, state, label, causeSentence, "no match", -1, "na", 0,0,0,0,0,0,0);
							reeval.writeEvaluationResultsToExcelFile_UniqueRows(proposalNum,0, state, label,causeSentence, "no match", -1, "na", 0,0,0,0,0,0,0);							
							//write only the unmatched row in trainingdata to file
							//reevalNoMatch.writeEvaluationResultsToExcelFile_UniqueRows(proposalNum, state, causeSentence, "no match", -1, "na", 0,0,0,0,0,0,0);							
							bwAO.write(pNum+","+ state+ ", ,"+causeSentence+",NOT MATCHED,0,0,0,0,0,0,0");			bwAO.newLine();
							bwSO.write(pNum+","+ state+ ", ,"+causeSentence+",NOT MATCHED,0,0,0,0,0,0,0");			bwSO.newLine();
							notMatched++;
							//march 2019, we write to eval table
							updateRowForMachineLearning(id,0,ranking,ranking);
						} 
						//if(permanentMatch>0)
						//	System.out.println("\tRanking: " + ranking + " Found Automatic Sentence ("+permanentMatchSentence+")");
						manualLabelCounter++;
						
					}	//end for each manual reason in the proposal..
					stmt2.close();
					if(!sentenceLabelFound) {
						listOfProposalsNotRecorded.add(pNum);	
						bwAO.write(pNum+","+ state+ ",,"+evalLevel+" not captured manually for proposal,,0,0,0,0,0,0,0");						bwAO.newLine();
						bwSO.write(pNum+","+ state+ ",,"+evalLevel+" not captured manually for proposal,,0,0,0,0,0,0,0");						bwSO.newLine();
						//System.out.println("\t Sentence not captured manually for proposal: "+pNum);
					}
					
				}//end for committed states table entry
				stmtCS.close();
				//For each label, we want to show summary
				System.out.println("\t\tShowing results for "+ state + ", " + evalLevel + " level.");
				System.out.println("\tTotal Proposals with current Label and are committed: " +rowCountcCS + " Proposals with reason "+evalLevel+" not entered "+listProposalReasonSentenceNotManuallyEntered.size());
				System.out.println("\tTotal Proposals with Label Not Recorded Manually: " +listOfProposalsNotRecorded.size() + ", Recorded Manually: "+ +listProposalReasonSentenceManuallyEntered.size());// + " : " + listOfProposalsNotRecorded.toString());
				System.out.println("\tTotal "+evalLevel+" for Proposals with label in manual training data: " + manualLabelCounter);
				System.out.println("\tTotal Proposals within  top   5: 	" + top5  +             ", 	Percent: " + (top5 * 100.0f) 		/ manualLabelCounter );
				System.out.println("\tTotal Proposals within  top  10: 	" + top10 +             ", 	Percent: " + (top10 * 100.0f) 		/ manualLabelCounter );
				System.out.println("\tTotal Proposals within  top  15: 	" + top15 +             ", 	Percent: " + (top15 * 100.0f) 		/ manualLabelCounter );
				System.out.println("\tTotal Proposals within  top  30: 	" + top30 +             ", 	Percent: " + (top30 * 100.0f) 		/ manualLabelCounter );
				System.out.println("\tTotal Proposals within  top  50: 	" + top50 +             ", 	Percent: " + (top50 * 100.0f) 		/ manualLabelCounter );
				System.out.println("\tTotal Proposals within  top  100: " + top100 +             ", 	Percent: " + (top100 * 100.0f) 		/ manualLabelCounter );
				System.out.println("\tTotal Proposals outside top  100: " + outsideTop100 +             ", 	Percent: " + (outsideTop100 * 100.0f)/ manualLabelCounter );
				System.out.println("\tTotal Proposals not found:	    " + notMatched +             ", 	Percent: " + (notMatched * 100.0f) 	/ manualLabelCounter );
				System.out.println("\tTotal Proposals no manual entry:	" + listProposalReasonSentenceNotManuallyEntered.size());	//	 +  
						//", 	Percent: " + (listProposalReasonSentenceNotManuallyEntered.size() * 100.0f) 	/ manualLabelCounter );
				System.out.println("");
				
				//insert values into database						  
				try {
					// create the java mysql update preparedstatement
					String query = "insert into dynamicweightallocation (label, " //1
							+ " sentenceHintProbablity, sentenceLocationHintProbability, restOfParagraphProbabilityScore, messageLocationProbabilityScore, messageSubjectHintProbablityScore, " //6 
							+ " dateDiffProbability, authorRoleProbability, messageTypeIsReasonMessageProbabilityScore, sentenceLocationInMessageProbabilityScore, sameMsgSubAsStateTripleProbabilityScore," //11
							+ " reasonLabelFoundUsingTripleExtractionProbabilityScore, messageContainsSpecialTermProbabilityScore , prevParagraphSpecialTermProbabilityScore, negationTermPenalty, " //15
							+ " top5, top10, top15, top30, top50, top100,outsidetop100,notfound,totalProbability) VALUES (?,?,?,?,?,?,?,?,?,?,?,?,?,?,?,?,?,?,?,?,?,?,?,?)";
					PreparedStatement preparedStmt;
					preparedStmt = connection.prepareStatement(query);
					preparedStmt.setString(1, state);
					
					//sentenceHintProbablity=sentenceLocationHintProbability=messageSubjectHintProbablityScore=dateDiffProbability=authorRoleProbability=negationTermPenalty=0.0;
					
					Double totalProb = sentenceHintProbablity+sentenceLocationHintProbability+restOfParagraphProbabilityScore+messageLocationProbabilityScore+messageSubjectHintProbablityScore+dateDiffProbability+authorRoleProbability+
							+messageTypeIsReasonMessageProbabilityScore+sentenceLocationInMessageProbabilityScore+sameMsgSubAsStateTripleProbabilityScore
							+reasonLabelFoundUsingTripleExtractionProbabilityScore+messageContainsSpecialTermProbabilityScore+prevParagraphSpecialTermProbabilityScore-negationTermPenalty;
					
					preparedStmt.setDouble(2, sentenceHintProbablity);	preparedStmt.setDouble(3, sentenceLocationHintProbability); 		preparedStmt.setDouble(4, restOfParagraphProbabilityScore);
					preparedStmt.setDouble(5, messageLocationProbabilityScore);	preparedStmt.setDouble(6, messageSubjectHintProbablityScore);
					preparedStmt.setDouble(7, dateDiffProbability);	preparedStmt.setDouble(8, authorRoleProbability); 		preparedStmt.setDouble(9, messageTypeIsReasonMessageProbabilityScore);	
					preparedStmt.setDouble(10, sentenceLocationInMessageProbabilityScore);	preparedStmt.setDouble(11, sameMsgSubAsStateTripleProbabilityScore);
					preparedStmt.setDouble(12, reasonLabelFoundUsingTripleExtractionProbabilityScore);	preparedStmt.setDouble(13, messageContainsSpecialTermProbabilityScore); 	
					preparedStmt.setDouble(14, prevParagraphSpecialTermProbabilityScore);	preparedStmt.setDouble(15, negationTermPenalty);	
					
					preparedStmt.setInt(16, top5);			preparedStmt.setInt(17, top10); 		preparedStmt.setInt(18, top15);			preparedStmt.setInt(19, top30);
					preparedStmt.setInt(20, top50);			preparedStmt.setInt(21, top100); 	preparedStmt.setInt(22, outsideTop100);	preparedStmt.setInt(23, notMatched);
					preparedStmt.setDouble(24, totalProb); //CHANGE WITH TOTALPROBABILITY
					// execute the java preparedstatement
					preparedStmt.executeUpdate();	
					if(outputForDebug)
						System.out.println("\tInserted Evaluation values for Result record id:");
				} catch (SQLException e) {
					// TODO Auto-generated catch block
					e.printStackTrace();
				}	
					
				
				
				
				for (int p :listOfProposalsNotRecorded) {
	//				System.out.println(p+",");
				}
			}	//end for each label	
			reeval.writeAndCloseFile();//close excel file
			//reevalNoMatch.writeAndCloseFile();
			
			bw.close();		bwAO.close();	bwSO.close();
			
		} //end for each level, sentence, paragraph and message
		
			
			
	} 
	
	//march 2019, we extend this function to write more data in table for evaluation 
	public static void updateRowForMachineLearning(int id, Integer containsReason, Integer rankBySQL, Integer rankBySystem){		  
		try {
			// create the java mysql update preparedstatement
			String query = "update autoextractedreasoncandidatesentences set containsReason = ?, rankBySQL = ?, rankBySystem = ?  where id = ?";
			PreparedStatement preparedStmt;
			preparedStmt = connection.prepareStatement(query);
			preparedStmt.setInt(1, containsReason);			preparedStmt.setInt(2, rankBySQL);
			preparedStmt.setInt(3, rankBySystem);			preparedStmt.setInt(4, id);
			// execute the java preparedstatement
			preparedStmt.executeUpdate();	
			if(outputForDebug)
				System.out.println("\tUpdated Evaluation values for Result record id:"+ id);
		} catch (SQLException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}	
		
	}
	
	//dec 2018..we update teh trainigdata fields
	//we update all messagetype in the allmessages table
	//ithMessageType_UpdateMessageType
	public static void updateTrainingDataTable(Integer rowid,Integer mid, Integer datediff, Integer proposal, String label, String sentence) throws SQLException {
		  String updateQuery = "update trainingdata set extractedMessageid = ?, datediff = ? where pep = ? and label = ? and id = ?";
		  PreparedStatement preparedStmt = connection.prepareStatement(updateQuery);
		  preparedStmt.setInt (1, mid);			  preparedStmt.setInt(2, datediff); preparedStmt.setInt(3, proposal);	
		  preparedStmt.setString(4, label); preparedStmt.setInt(5, rowid);
		  int i = preparedStmt.executeUpdate();
		  if(i>0)   { 		          //System.out.println("updated authorsrole ="+authorRole+" where messageID = "+ mid);				  
		  }
		  else  {	         
			  if(outputForDebug)
				  System.out.println("stuck updateTableWithMessageType mid " + mid + " sentence  " + sentence);		  }
		  updateQuery =null;
	}
	
	public static void printList(List<String> terms) {
		System.out.print("\t{");
		for (String t: terms) {
			System.out.print(t+" ");
		}
		System.out.println("}");
	}
	//check that first n words of manual sentence exists in automatic sentnec
	//pass both manual and automatic sentence as parameter
	//pass number of terms to check
	public static boolean checkWordOrder(List<String> automaticSentenceOrParagraph, List<String> manualCauseSentenceTerms, int numOfTermsToCheck ){
		try {
			//feb 2019..check no of terms first			
			while(manualCauseSentenceTerms.size() < numOfTermsToCheck) {
				numOfTermsToCheck--;
			}
			List<String> manualTermsSubset = new ArrayList<String>(manualCauseSentenceTerms.subList(0, numOfTermsToCheck));
			List<String> autoTermsSubset;// = new ArrayList<String>(automaticSentenceOrParagraph.subList(0, numOfTermsToCheck));
			List<String> subsetCommonTerms = new ArrayList<String>();
			//find the first manual term in automatic..can be multiple occurence
			String firstManualTerm = manualTermsSubset.get(0); //get first manual term to check
	//$$		System.out.println(" firstManualTerm: "+firstManualTerm);
			for (int i=0; i< automaticSentenceOrParagraph.size()-numOfTermsToCheck; i++) {		//we dont want to reach arrayindex out of range therefore we minus
				String autoTerm = automaticSentenceOrParagraph.get(i);
				if(autoTerm.contains(firstManualTerm) ) {			// as soon as first manual term is matched in auto sentence
					//extract subset 
					autoTermsSubset = new ArrayList<String>(automaticSentenceOrParagraph.subList(i, i+numOfTermsToCheck));	//check next n number of terms
	//$$				System.out.println(" i: "+i+" manualTermsSubset: "+manualTermsSubset+", autoTermsSubset: "+autoTermsSubset.toString());
					//make sure next x terms are the same in both the subsets
					subsetCommonTerms=findCommonElement(manualTermsSubset,autoTermsSubset); //System.out.println(" subsetCommonTerms: "+subsetCommonTerms.toString());
					//if(subsetCommonTerms.size() == numOfTermsToCheck) 
					//	return true;
					for (int j=0; j< numOfTermsToCheck; j++) {
						if(!autoTermsSubset.get(j).equals(manualTermsSubset.get(j)))
							return false;				//if any of these elements is not matched, return false
					}
					return true;
				}
			}
		}
		catch (Exception e){
			System.out.println("Exception 300 (FILE) " + e.toString()); 
			System.out.println(StackTraceToString(e)  );
		}
		return false;
	}
	
	public static List<String> findCommonElement(List<String>  a, List<String>  b) {
        List<String> commonElements = new ArrayList<String>();
        for(String i: a) {
            for(String j: b) {
                    if(i.equals(j)) {  
                    //Check if the list already contains the common element
                      //  if(!commonElements.contains(a[i])) {
                            //add the common element into the list
                            commonElements.add(i);
                      //  }
                    }
            }
        }
        return commonElements;
	}
	
	public static String[] removeNullOrEmpty(String[] a) {
	    String[] tmp = new String[a.length];
	    int counter = 0;
	    for (String s : a) {
	        if (s == null || s.isEmpty() || s.equals("")) {}
	        else {
	            tmp[counter++] = s;
	        }
	    }
	    String[] ret = new String[counter];
	    System.arraycopy(tmp, 0, ret, 0, counter);
	    return ret;
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
