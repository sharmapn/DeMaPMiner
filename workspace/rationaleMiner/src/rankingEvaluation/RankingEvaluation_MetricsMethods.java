package rankingEvaluation;

import java.io.BufferedWriter;
import java.io.IOException;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Iterator;
import java.util.LinkedHashMap;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;
import java.util.Set;
import java.util.stream.Collectors;

import org.apache.commons.lang3.ArrayUtils;

import opennlp.tools.util.Span;

public class RankingEvaluation_MetricsMethods extends ParameterSweeping_MemoryBased {
	
	//nov 2019..just commented these 2 functions in  lines 459 and 460
	//updateTableWithMessageType_UpdateMessageType
	
	
	//sort map according to this order
	private static boolean ASC = true;
    private static boolean DESC = false;
    
    static RankingEvaluation_MetricsCalculationForEachState emcfes = new RankingEvaluation_MetricsCalculationForEachState();
    
  //For each state we evaluate the reason sentences captured
	public static void forEachState(ArrayList<ScoresToSentences> List, HashMap<Integer, String> hmapMessages,
			double incrementCounter0, double incrementCounter1, double incrementCounter2, double incrementCounter3,double incrementCounter4, double incrementCounter5, double incrementCounter6, 
			double incrementCounter7,double incrementCounter8, double incrementCounter9, double incrementCounter10, double incrementCounter11,double incrementCounter12, double incrementCounter13, 
			Statement stmt2, String evalLevel,	boolean extractSentencesForWordCloud, boolean updateTrainingDataColumns, BufferedWriter bw,	BufferedWriter bwAO, BufferedWriter bwSO, String mid, 
			String accepted, Integer finalPlusAcceptedCounter, Integer finalMinusAcceptedCounter, String state, int[] selectedListItems ) throws SQLException, IOException {
		
		String author;
		int proposalNum,manualLabelCounter=0,notMatched=0;		
		double sentenceHintProbablity=0.0,sentenceLocationHintProbability=0.0,messageSubjectHintProbablityScore=0.0, dateDiffProbability=0.0, authorRoleProbability=0.0, negationTermPenalty=0.0,
			   restOfParagraphProbabilityScore=0.0, messageLocationProbabilityScore=0.0, 
			   messageTypeIsReasonMessageProbabilityScore=0.0,sentenceLocationInMessageProbabilityScore=0.0,sameMsgSubAsStateTripleProbabilityScore=0.0,
			   reasonLabelFoundUsingTripleExtractionProbabilityScore=0.0,messageContainsSpecialTermProbabilityScore=0.0,prevParagraphSpecialTermProbabilityScore=0.0;
				
		//april 2019..ranking evaluation metrics, we  store other evaluation metrics in each probability item
		Integer proposalCounter=0, matchedCounterForProposal=0, manualExtractedReasonSentenceCounterForAProposal=0;  
		
		//Evaluation Metrics Calculation For Each State
		emcfes.initEvaluationValuesForCurrStateToZero(); //Initialize all evaluation variables to zero
				
		//SECOND get all peps which have that label from committed state table
		//Jan we are assuming here that all main states are captured in the commit files, but that is not the case, look at PEP 510
		//String sqlCommittedStates = "SELECT pep,messageid,author,date2,email, datetimestamp from " + stateTable + " where pep > 0 and email like '%"+state+"%' order by pep asc ;" ; 	//and pep = 311		pep = 397  
		//just to be sure, we select only distinct peps, some peps have repeated same states committed twice like pep 0
		String sqlCommittedStates = "SELECT DISTINCT pep,email from " + stateTable + " where state = '"+state+"' order by pep asc ;" ; 	//and pep = 311		pep = 397  
		Statement stmtCS = connection.createStatement(); 		ResultSet rsCS = stmtCS.executeQuery(sqlCommittedStates); // date asc
		int totalDistinctProposalsCommittedForCurrentState = guih.returnRowCount(sqlCommittedStates); //System.out.println("\tManually Extracted Reasons records found: " + rowCount2);
		//then check if a manual sentence entry is found
		//if not we output that we were not abkle to manually find the reason sentence and leave that instance from out counting
		List<Integer> listOfProposalsNotRecorded = new ArrayList<Integer>();	
		
		//FOR EACH PROPOSAL, and for each reason sentence, we go through all candidate sentences and we match the sentence and establish the rank they are matched
		theouterloop:
		while (rsCS.next()) {	
			int pNum = rsCS.getInt("pep");					String stateEx = rsCS.getString("email");
			//feb 2019, if we want to limit to some selected peps and dont want them included in the results
			if (sel) //if we want to proces selected propoals only ...hard to find this code between all the classes so will put this variable in main calling class
				if (!ArrayUtils.contains( selectedListItems, pNum ) ) 
					continue;
			//if(pNum == 0) continue;  //skip pep 0 as it has so many mis-inserted states
			if(outputForDebug)
				System.out.println("Checking Proposal : " + pNum + " Label: "+state);
			proposalCounter++; matchedCounterForProposal=0;
			//initialise some of the evaluation metrics class values to zero
			////Evaluation Metrics Calculation For Each State
			emcfes.initEvaluationValuesForCurrProposalToZero();
			
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
			
//			System.out.println("-------------------------------------\nNew Proposal : " + pNum + " Label: "+state);
//	  		System.out.println("Proposal : " + pNum + " State Found: "+state);
			if(outputForDebug)
				System.out.println("New Proposal : " + pNum + " Label: "+state);
			//THIRD GET MANUALLY EXTRACTED CAUSE SENTENCE FOR THAT PEP and Label
			//IF a corresponding manual sentence for that label or sentence is not entered, then we displaya dn dont count
			String sqlManual = "SELECT pep,state,causeSentence, effectSentence, label, dmconcept from " + manualreasonextractionTable + " "
					+ " where consider = 1 and state like '%"+state+"%' and pep = "+pNum+" and LENGTH(causesentence) > 1 order by pep ;" ; 	//and pep = 311
			stmt2 = connection.createStatement(); 		ResultSet rsB = stmt2.executeQuery(sqlManual); // date asc
			Integer numManualEntertedSentencesForProposalState = guih.returnRowCount(sqlManual); //System.out.println("\tManually Extracted Reasons records found: " + rowCount2);
//			System.out.println("New Proposal : " + pNum + " Label: "+state + " numManualEntertedSentencesForProposalState: " + numManualEntertedSentencesForProposalState);
			//if atleast one reason sentence exists, we increment counter
			if (numManualEntertedSentencesForProposalState==0) 
				listTotalDistinctProposalsCommittedReasonSentenceNotManuallyEntered.add(pNum);
			else if (numManualEntertedSentencesForProposalState>0) 
				listTotalDistinctProposalsCommittedReasonSentenceManuallyEntered.add(pNum);						
			
			String manualLabel="",causeSentence="",effectSentence="",label="",causeSubcategory="",permanentMatchSentence="";
			//FOR EACH PROPOSAL ... GET ALL records but concentrate on the top ones
			boolean sentenceLabelFound = false; 
			String sqlAutomatic="", messageOrSection="";
			Integer messageID=null, ranking=1,id=0, datediff=0,anyRanking=1,rowCount=0,listOfMessageIDsForRankingCounter=0,calculatedRanking=0,rankingonTotalProb=0; 
			String headers[]= {"BDFL Pronouncement","Rejection Notice","Rejection","PEP Rejection","Pronouncement","Acceptance"};
			List<String> automaticSentenceOrParagraph = null;
			List<Integer> listOfMessageIDsForRanking = new ArrayList<Integer>();	//mar 2019, to keep track of which mids have been processed - only for 'sentencesfromdistinctmessages'
			//GET ALL MATCHES
			boolean matched=false, updated = false; //updated if for making sure we update the training data row once only, that being the highest ranked row details, which will happen as the sql resultset is ranked by  highest to lowest results
			sentenceHintProbablity=sentenceLocationHintProbability=restOfParagraphProbabilityScore=messageLocationProbabilityScore= 
					messageTypeIsReasonMessageProbabilityScore=sentenceLocationInMessageProbabilityScore=sameMsgSubAsStateTripleProbabilityScore=
					reasonLabelFoundUsingTripleExtractionProbabilityScore=messageContainsSpecialTermProbabilityScore=prevParagraphSpecialTermProbabilityScore=
					messageSubjectHintProbablityScore=dateDiffProbability=authorRoleProbability=negationTermPenalty=0.0;
			String autosentenceOrParagraphOrMessage="",email="",autolabel="",loc="";
			double probability = 0.0;
			
			//march 2019..a proposal and for that state
			ArrayList<ScoresToSentences> CurrProposalWithCurrStateList = new ArrayList<ScoresToSentences>();  
			Map<Integer, Integer> rankedMessageIDMap = new HashMap<Integer, Integer>(); //keep track of ranked messageids  for each manual sentence for each proposal
			manualExtractedReasonSentenceCounterForAProposal=0;
			//For each manually extracted/stored reason sentence
			while (rsB.next()) { //for each state we want to check, get all proposals //currDate.before(goToDate)) {
//##					System.out.println("\tManual found records: ");
				manualExtractedReasonSentenceCounterForAProposal++;		//for each proposal, for each reason sentence manually extracted
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
			    	else  	{			    		bw.write(causeSentence);		bw.newLine(); continue; /* continue to next record */ 	    	}
			    }
				//aug 2018 updated...for sentences starting with a heading like 'Rejection Notice', we skip these terms 
				//check manual sentence after two terms						
				for (String s: headers) {
					if(causeSentence.startsWith(s))			causeSentence = causeSentence.replaceAll(s, "");
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
				
				//CREATE A SUBSET ... March 2019 - the list is sorted by proposal number in last class
				CurrProposalWithCurrStateList = getOnlyCurrentProposal(List, state, proposalNum, CurrProposalWithCurrStateList);
				sqlAutomatic = createSQLForEvaluationLevel(evalLevel, state, proposalNum);  //different evaluation level has different sql statements
				//SSS march 2019 - criteria changed temporarily, ( rsA.next()  ) 
				//we dont want to consider duplicate sentences or paragraphs
				//april 2019, added ranking so that it can be use to assigned to repeated sentence. 
				//we assign the rank to the rank of the first instance of that sentence in the results regardless of messaage 
				Map<String, Integer> allSentencesForProposalHashMap = new HashMap<String, Integer>();
				
//						stmt = connection.createStatement(); 	
//SSSResultSet rsA = stmt.executeQuery(sqlAutomatic); // date asc
//						rsA = stmt.executeQuery(sqlAutomatic); 
//						rowCount = guih.returnRowCount(sqlAutomatic);			
//					System.out.println("\tAutomatic candidate records found : " + rowCount + " for label: "+ labels + " pep: "+ proposalNum);			
				ranking=1; 	//Integer	permanentMatch=0;	//Integer	permanentRanking=0;	//check all records one by one
				permanentMatchSentence="";
				//List<String> automaticSentenceOrParagraph = null;
				//GET ALL MATCHES
				matched=false; updated = false; //updated if for making sure we update the training data row once only, that being the highest ranked row details, which will happen as the sql resultset is ranked by  highest to lowest results
				
				sentenceHintProbablity=0.0; sentenceLocationHintProbability=0.0; messageSubjectHintProbablityScore=0.0; dateDiffProbability=0.0; authorRoleProbability=0.0; negationTermPenalty=0.0;
				id=0; datediff=0;
				rankingonTotalProb = 0;
				anyRanking=1; //sentenceCounterFromResults=1, paragraphCounterFromResults=1, messageCounterFromResults=1; //ranking starts at 1, not 0
				calculatedRanking=0; // duplicated sentences are skipped, therfore we need this counter
				listOfMessageIDsForRanking.clear();//clear this list for every proposal
				listOfMessageIDsForRankingCounter=0; //we make sure the value is reset to 1 here
				
//SSS						//we dont want to consider duplicate sentences or paragraphs
//SSS					List<String> allSentencesForProposalList = new ArrayList<String>();
				//System.out.print("\t new while "+ anyRanking); 
				
				// one pep may have multiple reason sentences
				// march 2019 - criteria changed temporarily, ( rsA.next()  ) 
				//ALL RESULTS ..WE NOW POPULATE THE SENTENCE LIST FOR CHECKING...ALREADY SORTED BY PROPOSAL AND THEN TOTAL PROBABILITY
//				System.out.println("Checking fro each manual sentence");
				outerloop: //Feb 2019, as soon as we find a match, we break out of checking the remaining records, whether it be sentences, paragraphs or messages
				for (int index=0; index < CurrProposalWithCurrStateList.size()-1 ; index++){  //List.size()-1    //CurrProposalWithCurrStateList.size()-					
					messageID = Integer.valueOf(CurrProposalWithCurrStateList.get(index).getMid()); 
//					System.out.println("New Record Being Checked Index " + s + " messageID: " + messageID  );
					//mid = messageID;
					//System.out.println("\t\tProposal s : " +s + " pnum " +p6 + " mid "+ m6);// + " count: " + counter + "firstIndex : "+firstIndex + " lastIndex: " + lastIndex + " List size : "+ (CurrProposalWithCurrStateList.size()-1));
					id = CurrProposalWithCurrStateList.get(index).getId();		datediff = CurrProposalWithCurrStateList.get(index).getDateDiff();
					//NOTE RANKING IS NOT COMPUTED HERE
					anyRanking = rankingonTotalProb = index+1; //rsA.getInt("rank"); //we add 1 as s starts from 0 and ranks should start at 1      
					CurrProposalWithCurrStateList.get(index).setRankByTotalProbability(anyRanking);	//we set the original ranking value ... for reference, as the other ranking value will change
					
					proposalNum =  CurrProposalWithCurrStateList.get(index).getProposalNum();
//					if(proposalNum==308)
//						System.out.println("Proposal "+ proposalNum+", Ranking based on TP AND Ranking "+ anyRanking ); 
					//April 2019..assign the rank
//$$				CurrProposalWithCurrStateList.get(s).setRank(anyRanking);
					//mid = rsA.getString("messageid");				
					//dateVal = CurrProposalWithCurrStateList.get(s).getDateVal();  dateval
					//march 2019..changed as we get it from hmap
					//HashMap<Integer,String> hmapMessages 
					//autosentenceOrParagraphOrMessage = CurrProposalWithCurrStateList.get(s).getSentenceOrParagraph();
					
					//this only applies to message based...we store and retrieve the message from hmap - hmap is sued for quick access to see if messageid exists
					// jan 2020
					if(evalLevel.equals("sentencesfromdistinctmessages")) {
						autosentenceOrParagraphOrMessage = 	hmapMessages.get(messageID);
					}
					//jan 2020 ..sentences are retrieved from arraylist..cant use hmaps as we dont have an identifier, as the message based one has that is the message id
					else if(evalLevel.equals("sentences")) { //for sentence based we get the sentence from teh list
						autosentenceOrParagraphOrMessage = CurrProposalWithCurrStateList.get(index).getSentenceOrParagraph();	
					}
					//original code commented..uncommented to hurry back
//$$				autosentenceOrParagraphOrMessage = 	hmapMessages.get(messageID);						
					
					//termsMatched = rsA.getString("termsMatched");	
					probability = CurrProposalWithCurrStateList.get(index).getTotalProbabilityScore();
					autolabel = CurrProposalWithCurrStateList.get(index).getLabel();				author = CurrProposalWithCurrStateList.get(index).getAuthor();					
					loc = CurrProposalWithCurrStateList.get(index).getLocation();
//							email = rsA.getString("email"); 
					//System.out.println("SQL Ranking " + anyRanking + " mid " + mid + " totalprobability "+probability );
					//probabilities
					if(evalLevel.equals("sentences") || evalLevel.equals("sentencesfromdistinctmessages") ) {								
						sentenceHintProbablity	= CurrProposalWithCurrStateList.get(index).getSentenceOrParagraphHintProbablity();						sentenceLocationHintProbability = CurrProposalWithCurrStateList.get(index).getSentenceLocationInMessageProbabilityScore();
						messageSubjectHintProbablityScore	= CurrProposalWithCurrStateList.get(index).getMessageSubjectHintProbablityScore();			dateDiffProbability = CurrProposalWithCurrStateList.get(index).getDateDiffProbability();
						authorRoleProbability	= CurrProposalWithCurrStateList.get(index).getAuthorRoleProbability();									negationTermPenalty = CurrProposalWithCurrStateList.get(index).getNegationTermPenalty();
						//march 2019																													// messageLocationProbabilityScore
						restOfParagraphProbabilityScore= CurrProposalWithCurrStateList.get(index).getRestOfParagraphProbabilityScore(); messageLocationProbabilityScore= CurrProposalWithCurrStateList.get(index).getMessageLocationProbabilityScore();
						messageTypeIsReasonMessageProbabilityScore= CurrProposalWithCurrStateList.get(index).getMesssageTypeIsReasonMessageProbabilityScore();
						sentenceLocationInMessageProbabilityScore= CurrProposalWithCurrStateList.get(index).getSentenceLocationInMessageProbabilityScore();
						sameMsgSubAsStateTripleProbabilityScore= CurrProposalWithCurrStateList.get(index).getSameMsgSubAsStateTripleProbabilityScore();
						reasonLabelFoundUsingTripleExtractionProbabilityScore= CurrProposalWithCurrStateList.get(index).getReasonLabelFoundUsingTripleExtractionProbabilityScore();
						messageContainsSpecialTermProbabilityScore= CurrProposalWithCurrStateList.get(index).getMessageContainsSpecialTermProbabilityScore();
						prevParagraphSpecialTermProbabilityScore= CurrProposalWithCurrStateList.get(index).getPrevParagraphSpecialTermProbabilityScore();
					}
					//jan 2020 we check if the sentence from the results is reaches here
					if(debugforsentenceslist)
						insertTrackingForSentences(proposalNum, evalLevel, causeSentence, anyRanking, autosentenceOrParagraphOrMessage, probability, messageID, -2);
				
					//we store all candidates in arraylist of sentences, so that we can check each sentence - even for paragraphs
					List<EvalPair> allCandidateSentencesList = new ArrayList<EvalPair>(); //we store all candidate sentences regardless of if from sentence table, paragraph or a message
					
					//# FOR EACH RESULT WE COMPUTE AND ASSIGN RANKING, BASED ON THE FACT THAT THE SENTENCE, OR MESSGE OF SENTENCE HAS BEEN ENCOUNTERED BEFORE
					//FOR SENTENCE OR PARAGRAPGH, WE SIMPLY CHECK IF THE THEY HAVE BEEN CHECKED BEFORE
					//THEN MAIN CHECKING ASSIGNING STARTS
					//SENTENCES ARE ASSIGNED WITH THE COMPUTED RANK ABOVE
					//SAME FOR PARAGRAPHS
					
					
					//Just to make sure, as we dont want to recheck. Check if sentence exists, if so continue to check in next sentence in ranked results
					//as its hard to get retrive distinct sentences using sql from a resultset of several columns - some messages may share same sentence - maybe quoted sentences from previous messages
					//commented 'sentencesfromdistinctmessages' from here since if a sentence has been encountered before, the messageid would be captured and assigned in the message checking code section marked #MCCS few lines below
					//April 2019, we have to add for the case, 'sentencesfromdistinctmessages' as look at pep 391, accepted as we can see many repeated sentences
					if(evalLevel.equals("sentences") ||  evalLevel.equals("paragraphs") || evalLevel.equals("sentencesfromdistinctmessages") ) {  // || 
						//if(allSentencesForProposalList.contains(autosentenceOrParagraphOrMessage)) //if previously found
						Integer sentenceFoundAtMessageRank = allSentencesForProposalHashMap.get(autosentenceOrParagraphOrMessage); //get the rank based on sentence
						if(sentenceFoundAtMessageRank != null) //if sentence found in hashmap
						{	System.out.println("\n\t evalLevel "+ evalLevel +" manualLabel " + manualLabel +" if CurrProposalWithCurrStateList.get(index).setRank(sentenceFoundAtMessageRank) " + sentenceFoundAtMessageRank);
							//same sentence can be from several messages, therefore we set the rank
							//here rather than setting it to the rank of the sentence, we set it to the rank of the message which contains the sentence
							CurrProposalWithCurrStateList.get(index).setRank(sentenceFoundAtMessageRank);
							continue;		//we just ignore the sentence and dont add to list for checking
						}	//if we have already encountered, obviously checked and stored this sentence in this list, go to next record/next sentence in ranked results
						else {	//System.out.println("\t else 1 ");
							calculatedRanking++;				//we only increment the ranking, if sentence is not found before
							anyRanking = calculatedRanking; 	//we assign it so that it can be used....but we dont do this for messages - that used the ranking returned from SQL
							allSentencesForProposalHashMap.put(autosentenceOrParagraphOrMessage,anyRanking);    //we will always store unique keys (autosentenceOrParagraphOrMessage) as its checked above
							//April 2019..assign the rank
//							System.out.println("\n\t evalLevel "+ evalLevel + " manualLabel " + manualLabel + " set Rank 123 " + anyRanking);
							CurrProposalWithCurrStateList.get(index).setRank(anyRanking);							
						}
					}
					
					//may 2020
					//System.out.println("\t set Ranked "+ evalLevel + " manualLabel " + manualLabel + " CurrProposalWithCurrStateList.get(index).setRank(anyRanking) " 
					//+ CurrProposalWithCurrStateList.get(index).getRank());
					
					//main checking and assignment of ranking
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
								if(outputForDebug)
									System.out.println("\n sentenceString.startsWith < && sentenceString.endsWith > continue "); 
								continue;
							}//	System.out.println("\n here 9");
							
							//allCandidateSentencesList.add(sentenceString);  //add to list
							EvalPair ev =  new EvalPair(anyRanking,messageID,sentenceString);
							allCandidateSentencesList.add(ev);	//increment these values as they are results within the while loop
						}
						//paragraphCounterFromResults++;
					}			
					//#MCCS message checking code section marked #MCCS
					else if(evalLevel.equals("messages") || evalLevel.equals("sentencesfromdistinctmessages")) {  //march 2019
							
			//			if(outputForDebug)
//								System.out.print("\t Processing Proposal: "+proposalNum+" Label: "+autolabel+" Message: " + messageID + " evalLevel: "+evalLevel+ " ranking "+ anyRanking);									
						//march 2019, in sentence level analysis we look at first instance of message, if already in list, we dont increment ranking
						if(evalLevel.equals("sentencesfromdistinctmessages")) {							
							if(autosentenceOrParagraphOrMessage==null) {  
								autosentenceOrParagraphOrMessage = email; //System.out.print("\t\t EMPTYYYYYYYYYYYYY"); 
							}
							//System.out.println("\n  first stage autosentenceOrParagraphOrMessage: "+autosentenceOrParagraphOrMessage.substring(0, 100));	
							Integer checkRank = rankedMessageIDMap.get(messageID); //get rank if the message has been encountered before
							if(checkRank==null) { //if not found we add
//								if(outputForDebug) 
//								System.out.println("\t\t  Current MessageID Not Found: " + messageID); //add to list								
								//System.out.println("\n  adding autosentenceOrParagraphOrMessage: "+autosentenceOrParagraphOrMessage.substring(0, 100));	
//										EvalPair ev =  new EvalPair(listOfMessageIDsForRankingCounter,messageID,autosentenceOrParagraphOrMessage);
//										allCandidateSentencesList.add(ev);						
//								listOfMessageIDsForRanking.add(messageID);	
//								listOfMessageIDsForRankingCounter++; //we increment the counter
//								anyRanking = listOfMessageIDsForRankingCounter;
								//April 2019..assign the rank
//								System.out.println("\t\t  Checking Last Ranked MessageID in Map: ");
								Integer lastrank = getLastRankedMessage(rankedMessageIDMap);   //CurrProposalWithCurrStateList.get(s-1).getRank(); //get rank of last record
//								System.out.println("\n\t\t  Last Ranked MessageID Returned: " + lastrank);
								CurrProposalWithCurrStateList.get(index).setRank(lastrank + 1); //assign rank as increment to previosu rank
								rankedMessageIDMap.put(messageID,lastrank + 1); 
//								System.out.println("\t\t  Added to Map, messageID," + messageID + " lastrank + 1:  " + (lastrank+1)); 
//								System.out.println("\t\t  Sorting Map ");
								//Map<Integer, Integer> sortedRankedMessageIDMapAsc = sortByValue(rankedMessageIDMap, ASC); //sort according to rank ascending order
								rankedMessageIDMap = sortByValue(rankedMessageIDMap, ASC);
								anyRanking = lastrank + 1;
							}
							else{	
								//if found //listOfMessageIDsForRanking.contains(messageID) 
								//f(outputForDebug) 	
								
//								System.out.println("\n\t\t Current MessageID Found: " + messageID + " Existing Rank: " + checkRank); 	
								
								//april 2091..we find in the list where is the first instance of that message id and assign that as rank
								CurrProposalWithCurrStateList.get(index).setRank(checkRank);  //assign the retrieved ranking
								//continue;
							} //dont add to list
							
						}
						// if(outputForDebug)
//								System.out.println("\t new rank : " + anyRanking);
						
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
									if(outputForDebug)
										System.out.println("\n sentenceString.startsWith < && sentenceString.endsWith > continue "); 
									continue;
								}//	System.out.println("\n here 9");
								//allCandidateSentencesList.add(sentenceString);  //add to list
								
								EvalPair ev =  new EvalPair(anyRanking,messageID,sentenceString);
								allCandidateSentencesList.add(ev);
								//April 2019..assign the rank
								//April 2019 commented
//$$							CurrProposalWithCurrStateList.get(s).setRank(anyRanking);
																		
							}
							pcounter++; 
						}
						 //messageCounterFromResults++;	//increment these values as they are results within the while loop
					}
//							System.out.println("\t here F, allCandidateSentencesList.size() "+ allCandidateSentencesList.size());
					//Feb 2019, now we have all sentences from sentence/paragraph/all sentences from a message
					//for all sentences found in sentences/paragraphs/messages, we check, as soon as one sentence is matched we exit
					for(EvalPair ev: allCandidateSentencesList) { 
						autosentenceOrParagraphOrMessage = ev.getSentence();//untested..trying cos of pep 345 only						
						ranking = ev.getID(); Integer msgid = ev.getMessageID();//ranking is already stored for each sentence based on if its sentence, paragraph or message
//						System.out.println("Ranking " + ranking + " msgid " + msgid + " " + autosentenceOrParagraphOrMessage);
/*						if(autosentenceOrParagraphOrMessage.toLowerCase().startsWith("after"))								
							System.out.println("\t\tsentenceString:  " + autosentenceOrParagraphOrMessage);
						else
							continue; 
*/						
						if(debugforsentenceslist)
							insertTrackingForSentences(proposalNum, evalLevel, causeSentence, anyRanking, autosentenceOrParagraphOrMessage, probability, messageID, -1);
						
						if(autosentenceOrParagraphOrMessage==null) { System.out.println("\t\tmid "+mid+"sentenceString:  " + autosentenceOrParagraphOrMessage);		autosentenceOrParagraphOrMessage = ""; } //QUICK AND DIRTY
						autosentenceOrParagraphOrMessage = autosentenceOrParagraphOrMessage.replaceAll("\\p{P}", " "); 	//remove all punctuation ..maybe we can use this here (term matching for candidates) but not in CIE triple extraction as it wmay need commas and other punctuation
						autosentenceOrParagraphOrMessage = autosentenceOrParagraphOrMessage.replaceAll("\\s+", " ").trim(); //remove double spaces and trim
						autosentenceOrParagraphOrMessage = autosentenceOrParagraphOrMessage.replaceAll("\'","").trim();						
						autosentenceOrParagraphOrMessage = autosentenceOrParagraphOrMessage.replaceAll(" n t ","n t ");	//change 'There has n t been a ' to 'There hasn t been a '
						if(autosentenceOrParagraphOrMessage==null || autosentenceOrParagraphOrMessage.isEmpty() || autosentenceOrParagraphOrMessage.length()==0 ) { continue; } //System.out.println("\t\tautosentenceOrParagraphOrMessage==null "); 
						else {
							if(autosentenceOrParagraphOrMessage.contains(" ")) 	automaticSentenceOrParagraph =  Arrays.asList(autosentenceOrParagraphOrMessage.toLowerCase().split(" "));	//autosentenceOrParagraphOrMessage.toLowerCase().split(" ");				
							else {
								automaticSentenceOrParagraph = new ArrayList<String>();		automaticSentenceOrParagraph.add(autosentenceOrParagraphOrMessage);
							}
							//System.out.println("\tChecking against ranking: "+ranking+", Automatic Sentence ("+sentenceOrParagraph+") ");
							List<String> commonTerms = findCommonElement(automaticSentenceOrParagraph,manualCauseSentenceTerms);	//if length of common terms > 70% of manual extracted sentence
							//if(Arrays.asList(automaticSentenceOrParagraph).containsAll(Arrays.asList(causeSentenceTerms))) {
							if(outputForDebug) 	
								System.out.println("\t\tAutomatic label ("+autolabel+") "+evalLevel+" ("+autosentenceOrParagraphOrMessage+") ");
							if(outputToFiles) {
								bwAO.write(proposalNum+","+state + ","+ ranking+ ","+causeSentence+"," +autosentenceOrParagraphOrMessage+","+probability+ "," +sentenceHintProbablity
										+ "," + messageSubjectHintProbablityScore + "," + authorRoleProbability + "," + sentenceLocationHintProbability + "," + dateDiffProbability + "," + negationTermPenalty);
								bwAO.newLine();
							}
							if(outputForDebug) { 
								System.out.print("\t\tcommonTermsSize: "+ commonTerms.size() + " manualCauseSentenceTerms.size(): "+ manualCauseSentenceTerms.size());	printList(commonTerms);
							}
							
							float  matchPercent = (commonTerms.size() * 100.0f /manualCauseSentenceTerms.size());
							boolean wordOrdersame = checkWordOrder(automaticSentenceOrParagraph,manualCauseSentenceTerms,3);	//get first three terms in both and make sure they are not numbers and punctuation and same in both	
//									System.out.println("\t\tmatchPercent: "+ matchPercent + " wordOrdersame: "+wordOrdersame ); 
							if(matchPercent > 60 && wordOrdersame ) { // && wordOrdersame || proposalNum==369) {	//
								//permanentMatch = match;	//permanentMatchSentence = autosentenceOrParagraphOrMessage; permanentRanking = ranking;
								//april 2019...For Ranking Evaluation...set matched
								// we assign the average precision, for each proposal, we going to treat as one query
								
								//may 2020 on rare occasions, eg pep 391 , some sentences get matched at the same rank
								//so we chack and set at the next rank..i am hoping thsi is rare and something wont be there in the next rank
								//happens at mbs where fpr pep 345 two sentences were matched in same message
								if(CurrProposalWithCurrStateList.get(index).getMatched()==1) {
									CurrProposalWithCurrStateList.get(index).setMatched(2);
									//System.out.println("\t\t\tincremented matched to value 2..as both matched at the same rank");
								}
								else
									CurrProposalWithCurrStateList.get(index).setMatched(1); //set matched to true
								
//$$							CurrProposalWithCurrStateList.get(s).setRank(ranking);  //set the ranking, although its already set
								matchedCounterForProposal++;	
								if(outputForDebug)
									System.out.println("\t\t\tMatched PEP ("+proposalNum+") MID ("+msgid+"): at Ranking: " + ranking + ", [index=("+index+")] Ranking on TP(Unique Msgs): " + rankingonTotalProb + ", Probability "+ probability +" EvalLevel: "+evalLevel+" "
											+ "terms percent match ("+matchPercent+") Found Automatic "+evalLevel+" ("+autosentenceOrParagraphOrMessage+") Manual Sentence matched : (" + manualCauseSentenceTerms+ ")");
								outputMatchedToFile(bwAO, bwSO, state, proposalNum, sentenceHintProbablity,	sentenceLocationHintProbability, messageSubjectHintProbablityScore,	dateDiffProbability, authorRoleProbability, negationTermPenalty, causeSentence,
										label, ranking, autosentenceOrParagraphOrMessage, probability);
								//					System.out.println("\tcommonTermsSize: "+ commonTerms.size() +" causeSentenceTerms.length: "+causeSentenceTerms.length); printList(commonTerms); 
//$$									//update row class column for machine learning , if sentence is matched
								if(evalLevel.equals("sentences")) { 
									//if(updateRowForMachineLearning) 
									//Nov 2019 commented both
									//updateRowForMachineLearning(id,1,ranking,ranking); 
									//updateTrainingDataTable(id,Integer.valueOf(mid),datediff,proposalNum,state,causeSentence) ; //dec 2018
									//updateTrainingDataTable(id,datediff) ; //dec 2018
									insertDateDiffValues(id,datediff) ; 
									
									//for debug
									if(debugforsentenceslist)
										insertTrackingForSentences(proposalNum, evalLevel, causeSentence, anyRanking, autosentenceOrParagraphOrMessage, probability, messageID, 1);
								}
								//oct 2018, we alos can update training data row here
								if(updateTrainingDataColumns && !updated) {			
									//feb 2020..we want to get the date diff so we set update the colu
								}
								//HOW MANY TIMES A SENTENCE HAS BEEN MATCHED - WITH DIFFERENT RANKINGS
								
								//break; //exit searching
								if(outputToFiles) 
									reeval.writeEvaluationResultsToExcelFile(proposalNum,msgid, state,label, causeSentence, autosentenceOrParagraphOrMessage, ranking, loc, matchPercent,
										sentenceHintProbablity,sentenceLocationHintProbability,messageSubjectHintProbablityScore,dateDiffProbability,authorRoleProbability,negationTermPenalty);
								//if it wasnt matched before, and as soon as rank found, we write only first ranking match record (above) in a separate excel sheet
								if(!matched) {	
									if(outputToFiles) 
										reeval.writeEvaluationResultsToExcelFile_UniqueRows(proposalNum,msgid, state,label, causeSentence, autosentenceOrParagraphOrMessage, ranking, loc, matchPercent,
											sentenceHintProbablity,sentenceLocationHintProbability,messageSubjectHintProbablityScore,dateDiffProbability,authorRoleProbability,negationTermPenalty);
	
									// may 2020, we try using hashmap now, as  dcg and ndcg cannot and should not be dine in categories
									//Integer getNumberOfSentencesMatchedAtThatRankForState = emcfes.smr.get(ranking);
									//emcfes.smr.put(ranking, getNumberOfSentencesMatchedAtThatRankForState + 1 );  
									emcfes.incrementNumberOfSentences_MatchedAtEachRank_ForState(ranking);
									
									if(ranking == 1) emcfes.incrementTop1();         								//top5++;
									if(ranking == 2) emcfes.incrementTop2(); 										// //Evaluation Metrics Calculation For Each State
									if(ranking == 3) emcfes.incrementTop3();
									if(ranking == 4) emcfes.incrementTop4();
									//top 3 here is inclusive of above
									if(ranking <= 5) emcfes.incrementTop5(); //top5++;
									else if (ranking > 5 && ranking <=10) emcfes.incrementTop10();    //top10++;			
									else if	(ranking > 10 && ranking <=15) emcfes.incrementTop15();  //top15++;
									else if	(ranking > 15 && ranking <=30) emcfes.incrementTop30();  //top30++;			
									else if	(ranking > 30 && ranking <=50) emcfes.incrementTop50();  //top50++;
									else if	(ranking > 50 && ranking <=100) emcfes.incrementTop100(); //top100++;
									else if (ranking > 100)  emcfes.incrementOutside100();				//outsideTop100++; 
								}
								matched=true;	
								break outerloop;	//Feb 2019, Since we doing these loops multiple times, for each manual sentence, as soon as we find a match, we break out of checking the remaining records, whether it be sentences, paragraphs or messages	
													//dec 2018, as soon as first match is found..we dont want to match all matches of same sentence which are below in order
							}else {
//								System.out.println("\t\tNOT MATCHED: "+autosentenceOrParagraphOrMessage);
								//jan 2020
								if(debugforsentenceslist)
									insertTrackingForSentences(proposalNum, evalLevel,causeSentence, anyRanking, autosentenceOrParagraphOrMessage, probability, messageID, 0);
							}
//									recordSetCounter++;	
							//if(evalLevel.equals("sentences")) {  //if we are checking sentences, ranking value is increased after every sentence,
							//	ranking++;						//paragraph and message level ranking is incremented differently
							//}
						} //end if sentence not empty	
					} //end checking for each sentence in canididate list
					allCandidateSentencesList.clear();
				} //end for each proposal all results - all sentences from sentence table, paragraph or messaages 
//				System.out.println("Ended For Each Proposal ");
				
				//March 2019...commented
//SSS					stmt.close();
				
				if(!matched) { //FOR EACH MANUAL SENTENCE IN A PROPOSAL, after going through all probable results for a proposal, if no match found we insert no match
					if(outputToFiles) {
						reeval.writeEvaluationResultsToExcelFile(proposalNum,0, state, label, causeSentence, "no match", -1, "na", 0,0,0,0,0,0,0);
						reeval.writeEvaluationResultsToExcelFile_UniqueRows(proposalNum,0, state, label,causeSentence, "no match", -1, "na", 0,0,0,0,0,0,0);							
						//write only the unmatched row in trainingdata to file
						//reevalNoMatch.writeEvaluationResultsToExcelFile_UniqueRows(proposalNum, state, causeSentence, "no match", -1, "na", 0,0,0,0,0,0,0);							
						bwAO.write(pNum+","+ state+ ", ,"+causeSentence+",NOT MATCHED,0,0,0,0,0,0,0");			bwAO.newLine();
						bwSO.write(pNum+","+ state+ ", ,"+causeSentence+",NOT MATCHED,0,0,0,0,0,0,0");			bwSO.newLine();
					}
					notMatched++;
					if(debugforsentenceslist)
						insertTrackingForSentences(proposalNum, evalLevel,causeSentence, anyRanking, autosentenceOrParagraphOrMessage, probability, messageID, -10);
					//march 2019, we write to eval table
					if(updateRowForMachineLearning) 
						updateRowForMachineLearning(id,0,ranking,ranking);
				} 
				//if(permanentMatch>0)
				//	System.out.println("\tRanking: " + ranking + " Found Automatic Sentence ("+permanentMatchSentence+")");
				manualLabelCounter++;
				
			}	//end for each manual reason in the proposal..
			stmt2.close();
			if(!sentenceLabelFound) {
				listOfProposalsNotRecorded.add(pNum);	
				if(outputToFiles) {
					bwAO.write(pNum+","+ state+ ",,"+evalLevel+" not captured manually for proposal,,0,0,0,0,0,0,0");						bwAO.newLine();
					bwSO.write(pNum+","+ state+ ",,"+evalLevel+" not captured manually for proposal,,0,0,0,0,0,0,0");						bwSO.newLine();
				}
				//System.out.println("\t Sentence not captured manually for proposal: "+pNum);
			}
			
			//Main Evaluation Metrics Calculation. For each Proposal,we will calculate metrics and increment where necessary. This will be output at the end of all proposals for that state
			emcfes.calculateEvaluationMetricsForEachProposal(CurrProposalWithCurrStateList, manualExtractedReasonSentenceCounterForAProposal, listTotalDistinctProposalsCommittedReasonSentenceManuallyEntered,
					numManualEntertedSentencesForProposalState,pNum,state,connection, evalLevel, outputForDebug); 
					
			
//			System.out.println("State "+state+" proposal " + pNum +": total matched sentences: " + matchedSent 
//					+ " sumOfAllPrecisionsForAllProposalsForCurrState: "+ sumOfAllPrecisionsForAllProposalsForCurrState + " ProposalCountre: " + proposalCounter );
//			System.out.println("\tFinal DCG Proposal: "+ pNum + ", rank: " + rank+ " dcg: " +dcg + " sum_dcg: "+ sum_dcg + ", dcgAtTop5: " + dcgAtTop5 + ", dcgAtTop10: " + dcgAtTop10 
//	        		+ ", dcgAtTop15: " + dcgAtTop15 + ", dcgAtTop30: " + dcgAtTop30 + ", dcgAtTop50: " + dcgAtTop50 + ", dcgAtTop100: " + dcgAtTop100 + ", dcgOutsideTop100: " + dcgOutsideTop100);
//			System.out.println("\t\tFinal DCG Proposal: "+ pNum + ", rank: " + rank+ " ndcg: " +ndcg + " sum_ndcg: "+ sum_ndcg + ", ndcgAtTop5: " + ndcgAtTop5 + ", ndcgAtTop10: " + ndcgAtTop10 
//	        		+ ", ndcgAtTop15: " + ndcgAtTop15 + ", ndcgAtTop30: " + ndcgAtTop30 + ", ndcgAtTop50: " + ndcgAtTop50 + ", ndcgAtTop100: " + ndcgAtTop100 + ", ndcgOutsideTop100: " + ndcgOutsideTop100);
		}//end for EACH PROPOSAL Found in each state
		stmtCS.close();
		
		//For each label, we want to show summary
		System.out.println("\nShowing results for "+ state + ", " + evalLevel + " level. manualLabelCounter: " + manualLabelCounter );
		emcfes.printForAllProposalInThatState(state, proposalCounter,evalLevel, listTotalDistinctProposalsCommittedReasonSentenceManuallyEntered, listTotalDistinctProposalsCommittedReasonSentenceNotManuallyEntered, 
				listOfProposalsNotRecorded,totalDistinctProposalsCommittedForCurrentState, manualLabelCounter, notMatched);			
		
		//FOR EACH STATE...insert values into database			
		//System.out.println("\t\t\t I VALUE 6 =  incrementCounter1: "+ incrementCounter1);
		emcfes.insertIntoDatabase(connection,incrementCounter0, incrementCounter1, incrementCounter2, incrementCounter3,incrementCounter4, incrementCounter5, incrementCounter6, incrementCounter7, incrementCounter8,
				incrementCounter9, incrementCounter10, incrementCounter11, incrementCounter12, /*incrementCounter13,*/	state, sentenceHintProbablity, sentenceLocationHintProbability, messageSubjectHintProbablityScore,
				dateDiffProbability, authorRoleProbability, negationTermPenalty, restOfParagraphProbabilityScore,	messageLocationProbabilityScore, messageTypeIsReasonMessageProbabilityScore,
				sentenceLocationInMessageProbabilityScore, sameMsgSubAsStateTripleProbabilityScore,	reasonLabelFoundUsingTripleExtractionProbabilityScore, messageContainsSpecialTermProbabilityScore,
				prevParagraphSpecialTermProbabilityScore, notMatched, evalLevel);	
		
		for (int p :listOfProposalsNotRecorded) {
//				System.out.println(p+",");
		}
		//return stmt2;
	}
	
	//jan 2020
	private static void insertTrackingForSQLRanking(int proposalNum, Integer anyRanking,
			String autosentenceOrParagraphOrMessage, double probability, int messageid, String location) throws SQLException {
		// the mysql insert statement
		String query = " insert into trackranking (proposal, ranking, messageid,  sentenceormessage, totalprobability, location)"  + " values (?, ?, ?, ?,?,?)";

		// create the mysql insert preparedstatement
		PreparedStatement preparedStmt = connection.prepareStatement(query);
		preparedStmt.setInt (1, proposalNum);       preparedStmt.setInt (2, anyRanking);   preparedStmt.setInt (3, messageid);	      
		preparedStmt.setString   (4, autosentenceOrParagraphOrMessage);		preparedStmt.setDouble(5, probability);	  preparedStmt.setString(6, location);	  //preparedStmt.setInt    (5, 5000);

		// execute the preparedstatement
		preparedStmt.execute();
	}
	
	//jan 2020 .. sentence based scheme was not matchingsome sentences so we want to see why
	//proposalNum, causeSentence, autosentenceOrParagraphOrMessage, anyRanking,probability, messageID, 0
	private static void insertTrackingForSentences(int proposalNum, String evalLevel, String trainingSentence, Integer anyRanking, String autosentenceOrParagraphOrMessage, 
			double probability, int messageid, int matched) throws SQLException {
		// the mysql insert statement
		String query = " insert into trackrankingforsentences (proposal, evalLevel, trainingsentence, matched,  rankedsentence, messageid, rank)"  + " values (?,?, ?, ?, ?,?,?)";

		// create the mysql insert preparedstatement
		PreparedStatement preparedStmt = connection.prepareStatement(query);
		preparedStmt.setInt (1, proposalNum);      preparedStmt.setString   (2, evalLevel); preparedStmt.setString   (3, trainingSentence);	 preparedStmt.setInt (4, matched);
		preparedStmt.setString   (5, autosentenceOrParagraphOrMessage);		preparedStmt.setInt (6, messageid);
		preparedStmt.setInt(7, anyRanking);	  //preparedStmt.setInt    (5, 5000);

		// execute the preparedstatement
		preparedStmt.execute();
	}
	
	
	private static String createSQLForEvaluationLevel(String evalLevel, String state, int proposalNum) {
		String sqlAutomatic="";
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
		/* 	sqlAutomatic = "SELECT id, @curRank := @curRank + 1 AS rank, proposal,messageid,dateValue, (select analysewords from allmessages w where w.messageid = x.messageid limit 1) as text,"
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
		*/	
		    
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
		return sqlAutomatic;
	}

	private static ArrayList<ScoresToSentences> getOnlyCurrentProposal(ArrayList<ScoresToSentences> List, String state, int proposalNum,	ArrayList<ScoresToSentences> CurrProposalWithCurrStateList) {
		Integer p = null,mi=null, counter=0, firstIndex=0, lastIndex=0;
		String rowlabel= "";
		boolean firstFound=false, lastFound=false;
		try	{
			//we sort the list by total probability
			CurrProposalWithCurrStateList.clear();
			if(!List.isEmpty()){
				counter =0;
				for (int x=0; x <List.size()-1; x++){
					p = List.get(x).getProposalNum();		mi = List.get(x).getMid();
					rowlabel = List.get(x).getLabel();	
					//System.out.println("\t\tProposal : "+p );
					if((p == proposalNum)  && rowlabel.toLowerCase().equals(state.toLowerCase())) {
						/*if(!firstFound) {//if first index not found, set first index
							firstFound=true; firstIndex=x;
						}
						if(firstFound) { //if first index found, keep incrementing last index
							lastFound=true;	lastIndex=x;
						} */
						CurrProposalWithCurrStateList.add(List.get(x)); //a new list for that proposal and state
						counter++;   						  					
					}
					//get first and last index of the records with current proposal number
 //  						  				if(firstFound && lastFound)
 // 						  					System.out.println("\t\tProposal : "+p + " count: " + counter + "firstIndex : "+firstIndex + " lastIndex: " + lastIndex + " List size : "+ (List.size()-1));
				}
//  						  			if(firstFound && lastFound) //now we output the firsta dn last index
//					  					System.out.println("\t\tProposal : "+proposalNum + " mid " + mi + " label (" + rowlabel + ")(state: "+state+")  count: " + counter + " firstIndex : "+firstIndex + " lastIndex: " + lastIndex + " List size : "+ (List.size()-1));
				//if(p==3152) {
 //  						  				for (int x=0; x <ProposalWithCurrStateList.size()-1; x++){
 //  						  					System.out.println("\t\t\tProposal : "+ ProposalWithCurrStateList.get(x).getProposalNum() + " mid " + ProposalWithCurrStateList.get(x).getMid() + " label (" + ProposalWithCurrStateList.get(x).getLabel() +")");
 //  						  				}
				//}
				
				//Sublist to ArrayList
			    /*
				 ArrayList<Probability> al2 = new ArrayList<Probability>(List.subList(1, 4));
			     System.out.println("SubList stored in ArrayList: "+al2);

			     //Sublist to List
			     java.util.List<Probability> subList = List.subList(1, 4);
			     System.out.println("SubList stored in List: "+subList);
				*/
				
			}
 //						  		System.out.println("\t\t\tPROPOSAL " + proposalNum + ", label: "+ state + ", list length: " + CurrProposalWithCurrStateList.size());
		}
		catch(Exception e){
			System.out.println("Exception " + e.toString());  			System.out.println(StackTraceToString(e)  );
		}
		return CurrProposalWithCurrStateList;
	}

	private static Integer getLastRankedMessage(Map<Integer, Integer> rankedMessageIDList) {
		//iterate over rankedMessageIDList to get last assigned message 
		Set set = rankedMessageIDList.entrySet(); Iterator iterator = set.iterator();
		Integer previousFinalRank=0;
		while(iterator.hasNext()) {
			Map.Entry mentry = (Map.Entry)iterator.next();
//			System.out.println("\t\t\t\t Map Contents, MessageID : "+ mentry.getKey() + " & Rank is: " + (Integer) mentry.getValue());
			previousFinalRank  = (Integer) mentry.getValue();  //System.out.println(mentry.getValue());
		}
//		System.out.println("\t\t\t\t Max Rank Returned is: " + previousFinalRank);
		return previousFinalRank;
	}

	private static void outputMatchedToFile(BufferedWriter bwAO, BufferedWriter bwSO, String state, int proposalNum,
			double sentenceHintProbablity, double sentenceLocationHintProbability,
			double messageSubjectHintProbablityScore, double dateDiffProbability, double authorRoleProbability,
			double negationTermPenalty, String causeSentence, String label, Integer ranking,
			String autosentenceOrParagraphOrMessage, double probability) throws IOException {
		if(outputToFiles) {
			bwAO.write(proposalNum+","+state+","+ label + ","+ ranking+ ","+causeSentence+"," +autosentenceOrParagraphOrMessage+","+probability+ "," +sentenceHintProbablity
					+ "," + messageSubjectHintProbablityScore + "," + authorRoleProbability + "," + sentenceLocationHintProbability + "," + dateDiffProbability + "," + negationTermPenalty);
			bwAO.newLine();
			bwSO.write(proposalNum+","+state +","+ label +","+ ranking+ ","+causeSentence+"," +autosentenceOrParagraphOrMessage+","+probability+ "," +sentenceHintProbablity
					+ "," + messageSubjectHintProbablityScore + "," + authorRoleProbability + "," + sentenceLocationHintProbability + "," + dateDiffProbability + "," + negationTermPenalty);
			bwSO.newLine();
		}
	}
	
	private static Map<Integer, Integer> sortByValue(Map<Integer, Integer> unsortMap, final boolean order)
    {
		List<Entry<Integer, Integer>> list = new LinkedList<>(unsortMap.entrySet());
        // Sorting the list based on values
        list.sort((o1, o2) -> order ? o1.getValue().compareTo(o2.getValue()) == 0
                ? o1.getKey().compareTo(o2.getKey())
                : o1.getValue().compareTo(o2.getValue()) : o2.getValue().compareTo(o1.getValue()) == 0
                ? o2.getKey().compareTo(o1.getKey())
                : o2.getValue().compareTo(o1.getValue()));
        return list.stream().collect(Collectors.toMap(Entry::getKey, Entry::getValue, (a, b) -> b, LinkedHashMap::new));
    }	
}
