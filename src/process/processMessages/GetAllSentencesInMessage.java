package Process.processMessages;

import java.io.FileInputStream;
import java.io.FileWriter;
import java.io.IOException;
import java.io.InputStream;
import java.io.Reader;
import java.io.StringReader;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Timestamp;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Calendar;
import java.util.Date;
import java.util.List;
import java.util.regex.Matcher;

import org.apache.commons.lang.StringUtils;

import EnglishOrJavaCode.EnglishOrCode;
import opennlp.tools.sentdetect.SentenceDetectorME;
import opennlp.tools.sentdetect.SentenceModel;
import opennlp.tools.util.Span;
import stanfordParser.pair;
import utilities.NattyReturnDateFromText;
//import wekaExamples.MyFilteredClassifier;
import callIELibraries.JavaOllieWrapperGUIInDev;
import Process.ReasonExtraction.Evaluation.Probability;
import Process.processLabels.TripleProcessingResult;
import Read.readMetadataFromWeb.GetProposalDetails;
import Read.readMetadataFromWeb.GetProposalDetailsWebPage;
import callIELibraries.ClausIECaller;
import callIELibraries.ReVerbFindRelations;
import callIELibraries.UseDISCOSentenceSim;
//import coreference.Coreference;
import de.mpii.clause.driver.ClausIEMain;
import edu.stanford.nlp.ling.HasWord;
import edu.stanford.nlp.ling.Sentence;
import edu.stanford.nlp.process.DocumentPreprocessor;
import info.debatty.java.stringsimilarity.Levenshtein;

public class GetAllSentencesInMessage {

	//Three ways to capture states and Reasons
	// 1. Check triple labels
	// 2. Capture Label Doubles, for all singles and doubles, - try match, and - if matched, find reason,  and - insert in results
	// 3. isolated Terms single liners - these terms should exists on its own and not part of any sentence ? sometimes they are

	static String sentenceTermsForCheckingDots[]      = null;
	static Integer sentenceCounterForTopicModelling   = 0;
	static String tableNameToStore_sentenceLabelling  = "allsentences";
	static String tableNameToStore_paragraphLabelling = "allparagraphs";
	//Jan 2019, by default we set this - a value for processmining, but for relation extraction we use a lower value, maybe 50, as we will RE for all sentences, some of wjhich woudl be too long - code probably
	static Integer sentenceLengthLimit				  = 500;			
	static PythonSpecificMessageProcessing pm = new PythonSpecificMessageProcessing();
	
		
	// sometime this can be moved to checksentence.java file
	public static void getAllSentencesInMessage(Message m, ProcessingRequiredParameters prp)
	{ 
		//String str = "This is how I tried to split a paragraph into a sentence. But, there is a problem. My paragraph includes dates like Jan.13, 2014 , words like U.S and numbers like 2.2. They all got splitted by the above code."; 
		Integer sentenceCounter = 0, insertDiscussionCounter = 0, lineNumber=0;
		ArrayList<String> allSentenceList = new ArrayList<String>(); 
		String previousParagraph = "", nextParagraph = ""; 

		//EXTRACT PARAMETERS
		ArrayList<SinglesDoublesTriples> singlesDoublesTriplesList = prp.getSinglesAndDoubles();
		//temporarily set prevsentence
		String prevSentence = "";
		FileWriter writerAll = prp.getWriterAll();		FileWriter writerForDisco = prp.getWriterForDisco();		Integer v_pepNumber = prp.getPEPNumber();
		Integer v_messageid = m.getMessage_ID();		String msgSubject = m.getSubject();  String msgAuthorRole = m.getAuthorsRole(); //last two variables added 2018 dec
		String[] reasonTerms = prp.getReasonTerms(); 	ClausIEMain cm = prp.getCm(); 
		String [] reasons = prp.getActualReasons();		String[] coreferenceSubjects = prp.getCoreferenceSubjects();
		Date dt = m.getM_date();
		String author = m.getAuthor(); 	String authorsRole = m.getAuthorsRole();
		Timestamp v_datetimestamp = m.getDateTimeStamp();		SentenceDetectorME detector = prp.getDetector();
		boolean checkMessageSubject=prp.getCheckMessageSubject();		boolean performCoreference = prp.getPerformCoreference();  //System.out.println("\t xcx prp.getPerformCoreference()  "+prp.getPerformCoreference());		
		//call garbage collector      
		System.gc();              //OR call 
		Runtime.getRuntime().gc();
		boolean v_messageEmpty = false;		String v_message = m.getMessage();		String subject= m.getSubject(); String authorRole = m.getAuthorsRole();
		//dec 2018
//		Coreference cr = new Coreference(); cr.init();
		//jan 2019
		String allVerbsInMsgSubject="", allNounsInMsgSubject="";
		
		if (v_message == "null" || v_message == null || v_message == "" || v_message.isEmpty() || v_message.length() == 0){	
			if(prp.getOutputfordebug())
				System.out.println("Message empty returning : "+ v_messageid);	
			return;	
			}
		//System.out.println("v_message not Empty");
		try 
		{   // System.out.println("\t here Aa  ");
			//we want to check the subject also ...so simply we ad the subject at the beginning of the message	       
			if ( subject==null || subject == "" || subject.isEmpty() )	{}
			else{  //System.out.println("\t original subject  " + subject);
				//remove everything in square brackets. 
				//e.g. [Python-Dev] Propose rejection of PEPs 239 and 240 -- a builtin  rational type and rational literals
				if(subject.contains("[") && subject.contains("]")) {
					subject = subject.replaceAll("\\[.*?\\]","");
				}	//System.out.println("\t removed everything from inside brackets subject  " + subject);
				//jan 2019
				String cleanedMsgSubject = subject.toLowerCase().replace("re:", "").replace("fw:", "").replace("fwd:", "").trim(), finalMsg=""; //get rid of fwd and reply
//				System.out.println("\t remoived fwd, re, fw subject  " + cleanedMsgSubject);
				if (!prp.getSentenceParagraphWritingToTable() )
				v_message = subject.toLowerCase() + ". \n\n" + v_message;   //we add the message subject to message body to allow this will check it for triples, and later doubles and singles
																			//but we dont need this in case we are slitting teh message into sentences for sentence labelling
				//jan 2019, relation extraction for message subject
				//message subject, moved to called class
				if(prp.getRelationExtraction()) {	 				
					//remove pep tiles from msg subject
					//String subjectToClean="", subjectCleaned="";
					/*for (int i=0; i<500; i++) {
						String pt =  prp.getPd().getPepTitleForPep(i, prp.getConn());	//go through and check for each pep title and remove
						if(pt.length() >1 && cleanedMsgSubject.contains(pt))
							cleanedMsgSubject = cleanedMsgSubject.replace(pt," ");
					}
					*/
					//jan 2019, we remove re , fwd, for extracting nouns
					
					
					
					cleanedMsgSubject = cleanedMsgSubject + " re"; //add some term, just to make it go in this for loop for checking, rre will be removed
					cleanedMsgSubject = cleanedMsgSubject.replaceAll("\\s{2,}", " ").trim(); //remove double spaces
//					System.out.println("\t removed double spaces cleanedMsgSubject subject  " + cleanedMsgSubject);
					if (cleanedMsgSubject.contains(" ")) {
						for (String f: cleanedMsgSubject.split(" ")) {
							f= f.trim();
							if(f.equals("re")  || f.equals("fwd")   || f.equals("fw")   || f.equals("cvs")) 	{ continue; }   //do nothing and continue;
							if(f.equals("re:") || f.equals("fwd:")  || f.equals("fw:")  || f.equals("cvs:"))	{ continue; }   //do nothing and continue;
							else if(f.contains("/")) 				{ continue; }	//we remove any string with a forwardslash							
							else if (StringUtils.isNumeric(f)) 		{ continue; }   //remove any string which is number or contaisn number
							else if (f.matches(".*\\d+.*"))			{ continue; }   //str contains a number inside...eg. ssd65d
							else {
								finalMsg = finalMsg + " " + f;
							}							
						}
					} //System.out.println("\t after for loop subject  " + finalMsg);
					//just make sure
					//cleanedMsgSubject= cleanedMsgSubject.trim();
					//cleanedMsgSubject= cleanedMsgSubject.replaceAll(" re ", " ").replaceAll(" fw ", " ").replaceAll( " fwd ", " ").replaceAll(" cvs ", " ");
					//if (cleanedMsgSubject.startsWith("re"))	cleanedMsgSubject = cleanedMsgSubject.replaceFirst("re ", " "); 
					//if (cleanedMsgSubject.startsWith("fw"))	cleanedMsgSubject = cleanedMsgSubject.replaceFirst("fw ", " ");
					//if (cleanedMsgSubject.startsWith("fwd"))	cleanedMsgSubject = cleanedMsgSubject.replaceFirst("fwd ", " "); 
					//cleanedMsgSubject = cleanedMsgSubject.replaceAll("\\s{2,}", " ").trim(); //remove double spaces
					
					pair p = new pair();
					p = prp.getStanfordNLP().returnAllVerbsAndNounsInSentence(finalMsg,p);
					allVerbsInMsgSubject = p.getVerb(); 	allNounsInMsgSubject = p.getNoun();
					//same code above doesnt work for some reason
					//if (allNounsInMsgSubject.startsWith("re"))	allNounsInMsgSubject = allNounsInMsgSubject.replaceFirst("re ", " "); 
					//if (allNounsInMsgSubject.startsWith("fw"))	allNounsInMsgSubject = allNounsInMsgSubject.replaceFirst("fw ", " ");
					//if (allNounsInMsgSubject.startsWith("fwd"))	allNounsInMsgSubject = allNounsInMsgSubject.replaceFirst("fwd ", " "); 
					
//					System.out.println("\t msgSubject Processed:  " + msgSubject + " allVerbsInMsgSubject " + allVerbsInMsgSubject + " allNounsInMsgSubject "+allNounsInMsgSubject);
				}
			}

			//CHECK MESSAGE SUBJECT for doubles, triples
			//e.g. [Python-Dev] Propose rejection of PEPs 239 and 240 -- a builtin  rational type and rational literals
			//methid will then send result for insertion in databse
			//Jan 2019,I dont know how succesful this scheme is based on verbs, etc, so we comment for now
			Boolean doubleFound = false; checkMessageSubject = true; //temp set this value
			if(!prp.getRelationExtraction() && checkMessageSubject) {	//we dont check for triples if we doing relation extraction
				
				//checkMessageSubjectForDoublesTriples(prp, singlesDoublesTriplesList, v_pepNumber, v_messageid, dt, v_datetimestamp,author,authorRole,subject);
				
				//Jan 2019, but I am sure about these so we use this direct string matching
				//SINGLE DOUBLE CHECKING - Mainly double checking
				//part of the code in get all sentences which has been implemented for checking for message subjects only
				//making this code below redundant..but may be useful
				//For teh sentence: "python dev  pep 289   generator expressions  second draft", the above code will catch triple terms but wont match triple labels
				// bUt in the following code, it will identify double "second draft"
				
				for (SinglesDoublesTriples d : prp.getSinglesAndDoubles())	{				 
					String label = d.getLabel(); String singleDoubleTerms = d.getSingleOrDoubleTerms();		//System.out.println("\n\tTesting double:" + singleDoubleTerms);				
					
					if(subject ==null || subject.isEmpty() || subject.length()==0)
						continue;  //just skip empty subject
					
					if( subject.toLowerCase().contains(singleDoubleTerms)) {
						System.out.println("\n\tFound double in MsG Subject (" + singleDoubleTerms +")");
																				//pass the label instead of the single or double
						//double_Found = cpd.processSinglesAndDoubles(entireParagraph,label,prevSentence, CurrentSentenceString, nextSentence, v_pepNumber, m.getM_date(), m.getMessage_ID(), m.getAuthor(), writerAll, writerForDisco,
						//		previousParagraph, nextParagraph,reasonTerms, cm, reasons, prp.getReasonLabels());
						try { 
							NattyReturnDateFromText ndf = new NattyReturnDateFromText();//now set the date according to natty	
							ndf.returnEventDateInText(subject);
							TripleProcessingResult resultObject =  new TripleProcessingResult(); //INSERT RESULT INTO DATABASE
							resultObject.setMetadata(v_pepNumber, dt,v_datetimestamp, v_messageid, label, author,authorsRole,subject,"", subject,lineNumber,true); //set msgSubjectContainsLabel= true
							resultObject.setLabelSubject(subject);
							resultObject.setParagraphCounter(0);   resultObject.setSentenceCounter(0);	resultObject.setSentenceCounterInParagraph(0);
							CheckInsertResult cir = new CheckInsertResult();
							//cir.connect();
							System.out.println("Going to write Double Result to database");
							cir.checkInsertResult(resultObject, prp);								
						} catch (Exception e) {							e.printStackTrace();						}		
						break; //just one double is enough from a message subject
					}	
				}				 
			}
			
			//we are going to process only the first x number of lines
			/* Integer linesToAnalyse = prp.getLinesToAnalyse(); String minimisedMessages="";
			if(m.getFolder().contains("checkins")|| m.getFolder().contains("commits")) {} 	//we check all lines for messages from these mailing lists
			else {
				if (linesToAnalyse.equals(-1)) {} //process entire message
				else {
					//split message into lines and get the first 25 lines. Dont worry if it ends in between a sentence or paragrapgh as full stop will be added.
					for (String l: v_message.split("\\r?\\n")) {	//for each line
						minimisedMessages = minimisedMessages + l; //addd lines to message 
					}
					v_message = minimisedMessages;
				}
			}
			*/
			//System.out.println("\t here a " );	
			//Now check message
			String PreviousSentenceString = "", originalCurrentSentence ="";
			//dec 2018 added
			v_message = v_message.replaceAll("\r?\n\\+","\r\n");	v_message = v_message.replaceAll("\r?\n\\-","\r\n");
			v_message = v_message.replaceAll("\r?\n>","\r\n");		v_message = v_message.replaceAll("\r?\n >","\r\n");
			v_message = v_message.replaceAll("\r?\n>>","\r\n");		v_message = v_message.replaceAll("\r?\n> >","\r\n");	v_message = v_message.replaceAll("\r?\n> > >","\r\n");
			v_message = v_message.replaceAll("\r?\n ","\r\n");
			
			String[] paragraphs = v_message.split("\\r?\\n\\r?\\n"); //mar 2018...new improved way implemented   "\\n\\n");
			//String[] paragraphs =  v_message.split("\\n\\n");
			String[] lines = v_message.split("\\r?\\n");
			//String[] lines = v_message.split("\\n");
			//System.out.println("----- Now Processing new Message ID: ("+v_message_ID+") total paragraphs.." + paragraphs.length);
			Integer count=0,paragraphCounter=0,sentenceCounterinParagraph=0,sentenceCounterinMessage=0;        		   
			Boolean permanentMessageHasLabel = false;		//if message has any label captured set this to true
			//System.out.println("\t message : " + v_message);	
			
			//START dec 2018, remove empty or null paragraphs - those that are just empty lines
			paragraphs = removeNullOrEmpty(paragraphs);			
			String paraMinusCurrSentence=""; //paragraph minus curr sentence		
			String firstParagraph = paragraphs[0],lastParagraph=paragraphs[paragraphs.length-1], messageUntilLastParagraph="";//,previousParagraph=""; //default values
			Integer totalParagraphs = paragraphs.length-1;			
			String endingSentimentWords[] = {"regards","best regards","cheers","thanks"};			
			//we make one round to get the first and last paragraph
			Integer pcounter=0,messageEndIndex= v_message.length();  //paragraph counter
			boolean firstParagraphFound=false, endFoundInMessage=false,endFoundInParagraph=false;
			String authorFirstName= "";
			if(author== null || author.isEmpty() || author.equals("")) {		author="";	authorFirstName="";	} 
			else { //get author firstname for locating end of message 
				author = author.toLowerCase();
				if(author.contains(" ")) {
					//if (m.getAuthor().split(" ").length > 1) {
						authorFirstName = author.split(" ")[0];
					//}
				}
				else {	authorFirstName = author;	}
				authorFirstName = authorFirstName.toLowerCase();
			}
			//GetMainMessageRemoveOtherParts
			//not sure about this
			/*String authorName = m.getAuthor(); authorFirstName= "";
			if(authorName== null || authorName.isEmpty() || authorName.equals("")) {	authorName="";	}
			else {
				if(authorName.contains(" ")) {
					//if (m.getAuthor().split(" ").length > 1) {
						authorFirstName = m.getAuthor().split(" ")[0];
					//}
				}
				else {
					authorFirstName = authorName;
				}
			} */
			//end not sure about this
			 
			Levenshtein levenshtein = new Levenshtein(); //for author firstname checking
			boolean pepTextAttachedFound= false; //in pep 479, reason was attached in pep text in side message
//			System.out.println("START we make one round to get the first and last paragraph, author: " +author + " authorFirstName: " + authorFirstName);
			//WE make a first paragraph split so that that we can allocate teh kast paragraph and keep that while processing the paragraphs again, so we know
			outerloop:  //DEC 2018...marker used to break 
			for(String para: paragraphs) { //System.out.println("Paragraph: "+pcounter + " [" + para +"]");
				if(para.equals("") || para.trim().length() <1) {	
					previousParagraph = para;  //just assign current para as previos para
					pcounter++;   continue; 
				}
				para = para.toLowerCase();				
				//if terms found in first loop
				if (pcounter >= 500) break;	//some messages like 328159 , have code and therefore about 10,000 paragraphs									
				Integer l = para.split(" ").length;	//oct 2018, find what type of ,message it is				
				//get first paragraph, but not short ones (less than 5 terms)
				if(!endFoundInMessage && !firstParagraphFound && paragraphCounter<=4) {
					if(l > 5) {	//we dont consider short greetings at first paragraph
						//nov 2018 added check as these are headers
						//Author: jesse.noller
						//Date:
						if( (para.toLowerCase().contains("author: ") && para.toLowerCase().contains("date: ") )
							||
							(para.toLowerCase().contains("from: ") && para.toLowerCase().contains("date: ") )
						  )	{	} //these are not first paragraphs
						else {
							firstParagraph = para; 					firstParagraphFound=true; //System.out.println("firstParagraph: "+firstParagraph);
						}
					}
				}
				endFoundInParagraph = false; //end is
				//PEP: 443
				//Title: Single-dispatch generic functions
				//Version: $Revision$
				//Last-Modified: $Date$
				//some message sdont have any name towaards teh end, but teh author proposes a pep, so we just look for this combination
//				System.out.println("\nxxxxxxxxxxxpara " +para);
				
				if(para.contains("pep: ") && para.contains("title: ") && para.contains("version: ")) {
					lastParagraph = previousParagraph; endFoundInMessage=true;  //System.out.println("\nPEP Text Found, Para " +para + "lastParagraph 2: "+lastParagraph);
					messageEndIndex = v_message.indexOf(para);   //get index of paragraph
					//We had to comment this cos of pep 479 when reason was in the pep section
					//break; //we better break or else it will keep assigning if in the next round it enters any other if/else block
					//we counter by adding the clause in the if section
					pepTextAttachedFound = true;
				}
				// we ignore the fromline and assign the previous paragraph
				if(para.toLowerCase().contains("from ") && (para.contains(" at ") || para.contains("@")) //contains email
						&& (para.toLowerCase().contains(" mon") ||  para.toLowerCase().contains(" tue") ||  para.toLowerCase().contains(" wed") ||  para.toLowerCase().contains(" thu") ||  para.toLowerCase().contains(" fri") ||  para.toLowerCase().contains(" sat") ||  para.toLowerCase().contains(" sun") )  
				  ) {   //above code to check if para contains any day of week
					lastParagraph = previousParagraph; endFoundInMessage=true;  //System.out.println("\n Ending From Found, Para " +para + "lastParagraph 2: "+lastParagraph);
					messageEndIndex = v_message.indexOf(para);   //get index of paragraph
					//We had to comment this cos of pep 479 when reason was in the pep section
					//break; //we better break or else it will keep assigning if in the next round it enters any other if/else block
					//we counter by adding the clause in the if section
					//pepTextAttachedFound = true;
				}
			
				//oct 2018 ..get last paragraph....lets check to see if its message ending
				if(!endFoundInMessage && l < 7) { //if end not found in message till yet, and length of paragraph is less than 7 terms
					String terms[] = para.split(" "); //get terms
					for (String t : terms) {
						t= t.toLowerCase();
						t = t.replaceAll("[-+.^:,]",""); //remove special characters like: - + ^ . : ,
						if(t.contains("."))		t= t.replace(".", "");
						if(t.contains(","))		t= t.replace(",", "");
						
						for (String ew: endingSentimentWords) {	//paragraph with sentiment words, more likely to be end of message
							if(t.contains(ew)) {
								endFoundInMessage=true;	lastParagraph = previousParagraph;	//System.out.println("endingSentimentWords " +ew + " \nlastParagraph 1: "+lastParagraph);	//firstParagraphFound=true;
								break outerloop; //we better break or else it will keep assigning if in the next round it enters any other if/else block
							}
						}
						
						if(t.contains(author) ||   t.contains(authorFirstName)) {	//firstname of author
							lastParagraph = previousParagraph; endFoundInMessage=true; 	 //System.out.println("author found in para " + para + "lastParagraph 3: "+lastParagraph);
							messageEndIndex = v_message.indexOf(para);   //get index of paragraph
							break outerloop; //we better break or else it will keep assigning if in the next round it enters any other if/else block
						}
						//try levestein distance
						if(levenshtein.distance(t,authorFirstName) < 3) {
							lastParagraph = previousParagraph; endFoundInMessage=true; 	//System.out.println("author found in para ED " +para + "lastParagraph 4: "+lastParagraph);
							messageEndIndex = v_message.indexOf(para);   //get index of paragraph
							break outerloop; //we better break or else it will keep assigning if in the next round it enters any other if/else block
						} 
						//debug = debug+t;
					}
				}
				pcounter++;  	previousParagraph = para; 
				//System.out.println(pcounter+ " Current Paragraph: "+para);
				//System.out.println(pcounter+ " First Paragraph: "+firstParagraph);
				//System.out.println(pcounter+ " Last Paragraph: "+lastParagraph);
			} //System.out.println("END we make one round to get the first and last paragraph" );
			if(!endFoundInMessage) {
				//nov 2018, even after processing, we see last paragraph assigned to [from jack@oratrix.nl  fri aug 17 10:20:14 2001], thne we assign it to second last paragraph
				if(lastParagraph.trim().toLowerCase().startsWith("from ")) {
					lastParagraph = paragraphs[paragraphs.length-2]; //System.out.println("----------------");
				}else {
					lastParagraph = previousParagraph; //assign the last paragraph as the last paragraph
				}
			}			
			//END DEC 2018
			firstParagraph = prp.psmp.removeLRBAndRRB(firstParagraph);				lastParagraph = prp.psmp.removeLRBAndRRB(lastParagraph);										
			firstParagraph = prp.psmp.removeDivider(firstParagraph);				lastParagraph = prp.psmp.removeDivider(lastParagraph);
			firstParagraph = prp.psmp.removeUnwantedText(firstParagraph);			lastParagraph = prp.psmp.removeUnwantedText(lastParagraph);	
			firstParagraph = prp.psmp.removeDoubleSpacesAndTrim(firstParagraph);	lastParagraph = prp.psmp.removeDoubleSpacesAndTrim(lastParagraph);
						
			//oct 2018...we want to get the last sentence in a message, therefore we have to first get the main message and then get the last sentence from that
			//we keep processing paragraphs until we come to a paragraph which has very few words and contains the firstname of the message author
			//also may have terms like 'regards', ..terms used in signature 
			//so we have a integer to store the index until that point
			boolean isFirstParagraph=false, isLastParagraph = false;
			Integer numberOfLongSentences=0; //if more than 10 sentences are long in message, we skip message
			
			//the Main paragraph split
			for (String entireParagraph: paragraphs) {	//System.out.println("paragragraphcounter: " +paragraphCounter + " " + entireParagraph);
				//oct 2018 ..lets check to see if its message ending
				if(entireParagraph.split(" ").length < 5) {
					String terms[] = entireParagraph.split(" "); //get terms
					for (String t : terms) {
						if(t.contains(author) ||   t.contains(authorFirstName)) {	//firstname of author
							messageEndIndex = v_message.indexOf(entireParagraph);   //get index of paragraph
						}
					}
				}
				
				sentenceCounterinMessage++;
				sentenceCounterinParagraph=0;	//System.out.println("paragragraphcounter: " +paragraphCounter + " " + entireParagraph);
//$$				System.out.println(">>>>>>>>>>>>>>>>>>>>>>>> \n Original Paragraph.length: " + entireParagraph.length() + " paragraph: "+ entireParagraph);
				//check make sure currents paragraph is not headings considered as a paragraph
				//this cannot be done at sentence level, as snlp sentence extractor strips out the newline characters
				//split paragraph using newline
				//if newline character is capital and the line length is < 30 and contains semi-colon , it is heading
				
//				Category: Macintosh
//				Group: None
//				Status: Open
//				Resolution: None
//				Priority: 5
//				Submitted By: Bob Ippolito (etrepum)
//				Assigned to: Nobody/Anonymous (nobody)
//				Summary: Enhance PackageManager functionality
				entireParagraph = prp.getPms().stripHeadingsInParagraph(entireParagraph);
//$$			System.out.println("\n\t Strip headings Paragraph.length: " + entireParagraph.length() + " paragraph: "+ entireParagraph);
				//july 2018
				//snlp stops when it encounters dots as fullstops like these...we therefore remove the dot in '.py'
				//' Perhaps also let you  
				//  choose an arbitrary .py file and scan for 'distutils' -- but I've....' 
				//remove every instance of 'dot directly followed by a letter'
					//entireParagraph = entireParagraph=entireParagraph.replaceAll("\\.\\W", "");  this doesnt work as it affects sentences being separated properly
				//find all indexes of dots, and check the character after if its letter, then remove the dot
				entireParagraph = findIndexesOfDots(entireParagraph);
//				System.out.println("\nentireParagraph: " +paragraphCounter + " " + entireParagraph+"\n");
//$$			System.out.println("\n\t New Paragraph.length: " + entireParagraph.length() + " paragraph: "+ entireParagraph);
				if (paragraphs.length ==1){	     				
					nextParagraph = ""; previousParagraph = "";
				}
				//paragraph length = 2,3,4 onwards
				else {		
					if (paragraphCounter==0)	previousParagraph = "";
					else previousParagraph = paragraphs[paragraphCounter-1];
					if (paragraphCounter==paragraphs.length-1)		nextParagraph = "";
					else  nextParagraph = paragraphs[paragraphCounter+1];
					/*
					Reader reader = new StringReader(entireParagraph);
					DocumentPreprocessor dp = new DocumentPreprocessor(reader);

					//sometimes a paragrapgh has sentences but are not captured as sentences. So if sentence identified then check, so we just add a full-stop at end of paragraph	        				   
					if(!entireParagraph.endsWith("."))
						entireParagraph = entireParagraph + ".";

					for (List<HasWord> eachSentence : dp) 
					*/
					//dec 2018
					//perform coreference on paragraph level, and send for triple term matching and triple matching
					
					//moved this here instead of every sentence
					//remove unnecessary words like "Python Update ..."																	
					entireParagraph = prp.psmp.removeLRBAndRRB(entireParagraph);	entireParagraph = prp.psmp.removeDivider(entireParagraph);
					entireParagraph = prp.psmp.removeUnwantedText(entireParagraph);	entireParagraph = prp.psmp.removeDoubleSpacesAndTrim(entireParagraph);
					
					//jan 2019..perform coreference on paragraph //write first and last paragraphs to db table
					//if we want to perform coreference	..also we only do coref once, therefore we check if its not done before						
					String corefParagraph="";
					if(performCoreference) {// && coreferenceOnEverySentenceNowOnwards==false) { // && (currSentenceTillParaEnd.length() < 5)) { //!prp.getRelationExtraction() && performCoreference)  // we dont need coreference checking for relation extrcation
						//if curr sentence contains coresubject
						// coreference on curr sentence and every sentence afterwards
						// and send sentence for IE matching and triple extraction
						for(String corefSubject : coreferenceSubjects){	//System.out.println(" corefSubject 0: " +corefSubject);
							if(entireParagraph.contains(corefSubject))	{
								//coreferenceOnEverySentenceNowOnwards= true; if para contains two sentences with corefsibject, one after another, the coref is incorrect
								//System.out.println(" performing coreference for CurrentSentenceString xcx: "+CurrentSentenceString);
								//coreference on curr sentence and every sentence afterwards
								//a dn send for IE checking adn matching
//								corefParagraph = coreferenceOnCurrentSentenceAndRestOfParagraph(m, prp, sentenceCounter, previousParagraph, nextParagraph,	prevSentence, coreferenceSubjects, PreviousSentenceString, 
//										sentenceCounterinParagraph, paragraphCounter,entireParagraph, nextSentence, CurrentSentenceString, currSentenceTillParaEnd, lineNumber, corefSubject, detector);
								try {
									//corefParagraph = prp.getStanfordNLP().resolveCoRef(entireParagraph);
									corefParagraph = prp.getCoref().getdCoreferencedText(entireParagraph);
//									System.out.println("\tCOREF corefSubject ");//+corefSubject+" currSentenceTillParaEnd sent: [" + currSentenceTillParaEnd + "] Received [" +coReferencedcurrSentenceTillParaEnd +"]");
								}
								catch(Exception ex) {
									System.out.println(m.getMessage_ID() + " error corefrencing paragraph  ("+entireParagraph+")" + ex.toString() + "\n" ); //System.out.println(Thread.currentThread().getStackTrace()  );		//+  " \n" + e.printStackTrace()
									System.out.println(StackTraceToString(ex)  ); 
								}
								break;	//we dont want to coreference for each term, just one term is enough for invoking coref
							}
						}
					}							
					 //end coreference	
					
					//jan 2019, we want to do process mining on both; original paragraph and coref paragraph
					String paras[] = {entireParagraph,corefParagraph}; boolean isCorefParagraph = false;	Integer counterPara=0;//we want to record if we are working with coreferenced paragraph
					for(String thisParagraph: paras) {
					if(counterPara==1)	isCorefParagraph=true;		//we want to record IN DB Table if we are working with coreferenced paragraph
					//insert into paragraphs table is done right at the end so that we can capture if para is first or last
					
					//new code oct 2018
					Span[] spans = detector.sentPosDetect(thisParagraph);   //Detecting the position of the sentences in the paragraph  
					boolean firstParaFound=false,lastParaFound=false;     boolean coreferenceOnEverySentenceNowOnwards = false;
					
						//Printing the sentences and their spans of a paragraph 
					String CurrentSentenceString="",currSentenceTillParaEnd=""; 
					for (Span span : spans) {
						CurrentSentenceString = thisParagraph.substring(span.getStart(), span.getEnd()); //System.out.println("\tCurrentSentenceString: "+CurrentSentenceString);
						currSentenceTillParaEnd = thisParagraph.substring(span.getStart(), thisParagraph.length()); //System.out.println("\tcurrSentenceTillParaEnd: "+currSentenceTillParaEnd);
						String nextSen = "";	//dec 2018, new way of finding next sentence, we use opennlp on remainder of paragraph
						Span[] spansN = detector.sentPosDetect(currSentenceTillParaEnd);
						for (Span spanNew : spansN) {
							nextSen = currSentenceTillParaEnd.substring(spanNew.getStart(), spanNew.getEnd()); 
							break; //we just want the next sentence ..so we break after finding it.
						}
						//CurrentSentenceString=CurrentSentenceString.toLowerCase().trim();
						paraMinusCurrSentence = thisParagraph.replace(CurrentSentenceString, "");
						//isFirstParagraph = isLastParagraph = false;
						//find out if curr sentence is in first paragraph or last paragraph
						
						firstParagraph = firstParagraph.replaceAll("\n"," ");	lastParagraph = lastParagraph.replaceAll("\n"," ");						
						//System.out.println("\n\tfirstParagraph: "+firstParagraph);						System.out.println("\n\tlastParagraph: "+lastParagraph);
						//System.out.println("\n\tCurrentSentenceString: "+CurrentSentenceString);
						if(!firstParaFound) { 
							if(firstParagraph.contains(CurrentSentenceString.trim().toLowerCase())) {	isFirstParagraph= true;		firstParaFound=true;  
						}
							/*System.out.println("\nisFirstParagraph= true"); */  	}
//						else {isFirstParagraph= false;	/*System.out.println("isFirstParagraph= false"); */ }
						if(!lastParaFound) {
							if( lastParagraph.contains(CurrentSentenceString.trim().toLowerCase())) {	isLastParagraph= true;		lastParaFound=true; 
						}
							/*System.out.println("\nisLastParagraph= true"); 	*/	}
//						else {isLastParagraph= false;	/*System.out.println("isLastParagraph= false"); */ }
												
						boolean dependency = true, v_stateFound = false, foundLabel = false, double_Found = false,found=false;
						String nextSentence = ""; //CurrentSentenceString = Sentence.listToString(eachSentence);
						originalCurrentSentence = CurrentSentenceString;
						++sentenceCounter;		//System.out.println("\tsentenceCounterinParagraph: " +sentenceCounterinParagraph + " : " +CurrentSentenceString );
						//find location (starting line of teh sentence)						
						for(String l: lines) {							
							if(l.equals(CurrentSentenceString)) {
//								System.out.println("\t sentence exists in list  "+CurrentSentenceString); break;	
							}
							lineNumber++;
						}
						
						
						//jan 2019, remove links
						if(CurrentSentenceString == null || CurrentSentenceString.isEmpty() || CurrentSentenceString.length() == 0)
						{}
						else {
							if (CurrentSentenceString.contains("https") && CurrentSentenceString.contains("html")) {
								String link = StringUtils.substringBetween(CurrentSentenceString, "https", "html");
								if(CurrentSentenceString.contains(link))
									CurrentSentenceString = CurrentSentenceString.replace(link, "");
								CurrentSentenceString = CurrentSentenceString.replaceAll("httpshtml", " ");
								//String link = CurrentSentenceString.substring(CurrentSentenceString.indexOf("https") + 1, CurrentSentenceString.indexOf("html"));
								if(prp.getOutputfordebug())
								System.out.println("\t\t\t Removed Link ("+link+") from sentence: "+CurrentSentenceString);
							}
						}
//$$						System.out.println(" \t\t\t here c CurrentSentenceString: "+CurrentSentenceString);
						//dec 2018
						//pep 308, "The results shown below are for the complementary, UNOFFICIAL vote on the issue of adding a ternary operator to Python PEP308 ."
						//shifted this code insode function to check for tripel terms in sentence
						//CurrentSentenceString = CurrentSentenceString.replace(".", " .").replace(",", " ,").replace("!", " !").replace("-", " -").replace(";", " ;").replace(":", " :");
						
						//System.out.println("\t here aw01 00  ");
						//get the next previous sentence and paragraphs 
						//have to go over teh entire mesage again, but since it only for captured triples, it will be less instances of these
						nextSentence=nextSen; //getNextSentenceInCurrentParagraph(entireParagraph,sentenceCounterinParagraph,nextParagraph);						
						allSentenceList.add(CurrentSentenceString);
						
						if ( CurrentSentenceString==null || CurrentSentenceString.trim().equals("") || CurrentSentenceString.isEmpty() || CurrentSentenceString.split(" ").length < 3){
							continue; //onto next sentence System.out.println("\t sentence is empty  ");
						}
						//if number of terms in sentence is less than 75 
						//- we dont want to check extremely looong sentences - if they are indeed sentences - code (without ending comma) can take unecesary time in CIE processing
						if(prp.getRelationExtraction()) sentenceLengthLimit = 50; //otherwise its 500.
						if (CurrentSentenceString.split(" ").length > sentenceLengthLimit) { 
							System.out.println("\t sentence length > sentenceLengthLimit "+sentenceLengthLimit+", skipping "); 	
							numberOfLongSentences++;							continue; 
						} //normally 100
						if(numberOfLongSentences> 15) {
							System.out.println("\t More than 15 long sentences in message, skipping message "); 
							return; // we dont process teh message any longer, we just return
						}
						//dec 2018
						//int counterDots = 0;	String w[] = {CurrentSentenceString};
					    //for( int i = 0; i < CurrentSentenceString.length()-1; i++ )					    	
					     //   if( w[i].matches(".*[.,!?]") )
					    //        count++; 
						int counterDots = CurrentSentenceString.length() - CurrentSentenceString.replaceAll("[.,!?]+", "").length();
						if (counterDots > 10) //we don't want to process these sentences as they are not sentences but code and take too long 
							continue;
						//System.out.println("\t here a0  ");
						//Check triple labels						
						//{	//System.out.println(" here c CurrentSentenceString: "+CurrentSentenceString); //System.out.println("\t here aw01  ");
						//Check if sentence is Englsh or code
						boolean isEnglish = prp.getEnglishOrCode().isEnglish(CurrentSentenceString);	//can conmtinue to process onto next sentence if not
						//Coreference
						//we pass this sentence with the previous sentence for coreference checking,
						//once the sentence is checked for triple terms, then the updated second sentence should be checked for triple terms again
						//and if any triples are found, we extract and match triple			
						//PreviousSentenceString
						//Corefernce
						
						//jan 2019 ..sentence shortening/simplification
						CurrentSentenceString = prp.getSentenceSimplification().simplifySentence(CurrentSentenceString);
						
						//jan 2019, deleted as we now do paragraph level coref, which is done able
						//if we want to perform coreference	..also we only do coref once, therefore we check if its not done before						
						/*if(performCoreference) {// && coreferenceOnEverySentenceNowOnwards==false) { // && (currSentenceTillParaEnd.length() < 5)) { //!prp.getRelationExtraction() && performCoreference)  // we dont need coreference checking for relation extrcation
							
							//if curr sentence contains coresubject
							// coreference on curr sentence and every sentence afterwards
							// and send sentence for IE matching and triple extraction
							for(String corefSubject : coreferenceSubjects){	//System.out.println(" corefSubject 0: " +corefSubject);
								if(CurrentSentenceString.contains(corefSubject))	{
									//coreferenceOnEverySentenceNowOnwards= true; if para contains two sentences with corefsibject, one after another, the coref is incorrect
									//System.out.println(" performing coreference for CurrentSentenceString xcx: "+CurrentSentenceString);
									//coreference on curr sentence and every sentence afterwards
									//a dn send for IE checking adn matching
									coreferenceOnCurrentSentenceAndRestOfParagraph(m, prp, sentenceCounter, previousParagraph, nextParagraph,	prevSentence, coreferenceSubjects, PreviousSentenceString, 
											sentenceCounterinParagraph, paragraphCounter,thisParagraph, nextSentence, CurrentSentenceString, currSentenceTillParaEnd, lineNumber, corefSubject, detector);
									break;	//we dont want to coreference for each term, just one term is enough for invoking coref
								}
							}
						}		*/					
						 //end coreference	
													
						//SentenceExtractionForTopicModelling
						//JAN 2019, Refined approach. We use the sentences extracted in the Relation Extraction section
						// we select from database table rather than file
						// we select only sentences from first and last paragraph and 
						//as a first attempt look at those sentences which contain the term 'jep'
						
						//we assume that sentences have already been written to datase table
						//here write to a table here and then have another script to select from allsenetnces table
						//where we do the processing,  included in this section, like removing punctuation
						
						/*
						if(prp.getSentenceExtractionForTopicModelling()) {	//remove all terms with dots - mostly links
							try {	//HERE WE WANT TO DO WHAT WE WANT TO DO
								sentenceTermsForCheckingDots = CurrentSentenceString.split(" ");
								for(String s : sentenceTermsForCheckingDots) {
									if (s.length() > 2 && s.contains(".")) {	//end of sentence would contain a dot and we dont want to replace that ...maybe its needed in CIE
										CurrentSentenceString = CurrentSentenceString.replace(s, " ");
									}
								}
								CurrentSentenceString = CurrentSentenceString.replaceAll("[^a-zA-Z ]", "").toLowerCase();	//remove all punctuation
								CurrentSentenceString = CurrentSentenceString.replaceAll("\\s+", " ").trim(); //remove double spaces and trim								
								if(CurrentSentenceString.split(" ").length >5) {
									sentenceCounterForTopicModelling++;
									CurrentSentenceString = CurrentSentenceString + ".";	///add fullstop as removed in earlier code
									CurrentSentenceString = CurrentSentenceString.replaceAll("\\s+", " ").trim();	//remove double spaces and trim
									StringBuilder sb = new StringBuilder();
									sb.append(sentenceCounterForTopicModelling);	sb.append(" 	X	");		sb.append(CurrentSentenceString);			sb.append('\n');
									prp.getPwForTopicModellingSentences().write(sb.toString());	//write to file
								}
						
							}  //end try       
							catch (Exception e){ 
								System.out.println(m.getMessage_ID() + " error inserting record______here 231  " + e.toString() + "\n" ); //System.out.println(Thread.currentThread().getStackTrace()  );		//+  " \n" + e.printStackTrace()
								System.out.println(StackTraceToString(e)  );									//continue;
							} 
						} */
						
						//Sentence writing to table, can exist independent of any purpose of our script, may seem to go together with relation exraction
						//Note, paragraph writing to table in later section below
						//but since jan 2019, we keep it independent so it can be done in any purpose or on its own
						////writing to sentence table is included here as, if we want to write all sentences, even process mining will capture all sentences
						//but they can exists on their own when either is needed without the other
						//IN MOST CASES, ESPECIALLY FOR RELATION EXTRACTION, We write sentences in different table with the sentence counter 
						//dec 2018...at the moment we are putting this code here as sentence writing to tablea nd relation extraction both go together
						//JAN 2019, WE DECIDE SENTENCE AND PARAGRAPH WRITING USING SAME VARIABLE = value retruned by prp.getSentenceWritingToTable();
						if(prp.getSentenceParagraphWritingToTable()) {	//if we want to just write each sentence to a database table
							//System.out.println("\t getSentenceParagraphWritingToTable() " ); 
							try { /*
								CurrentSentenceString = CurrentSentenceString.replaceAll("[^a-zA-Z ]", "").toLowerCase();	//remove all punctuation
								CurrentSentenceString = CurrentSentenceString.replaceAll("\\s+", " ").trim(); //remove double spaces and trim
								//boolean isEnglishOrCode = prp.getEnglishOrCode().isEnglish(CurrentSentenceString);									
																
								java.util.Calendar cal = Calendar.getInstance();
								cal.setTime(dt);  //v_dateC
								cal.set(Calendar.HOUR_OF_DAY, 0);			cal.set(Calendar.MINUTE, 0);			cal.set(Calendar.SECOND, 0);		cal.set(Calendar.MILLISECOND, 0);
								java.sql.Date sqlDate = new java.sql.Date(cal.getTime().getTime()); // your sql date
								//    System.out.println("utilDate:" + v_dateC);
								//    System.out.println("sqlDate:" + sqlDate);
								
								Timestamp now = null;
								if (m.getDateTimeStamp()!=null){	now = new Timestamp(m.getDateTimeStamp().getTime());			}								
								*/
								String lastDir = m.getFolder().replace("c:\\datasets\\","");
								
							 // to create unique id for sentence joining with paragraph with we store paragraphcounter+sentencecounter. Fields pepnumber, messageid will be stored in seprate columns
							  PreparedStatement pstmt = null;	
						      String query = "insert into "+tableNameToStore_sentenceLabelling+"(proposal, sentence, previousSentence, nextSentence, entireParagraph, previousParagraph, " //6
						      		+ "nextParagraph,  messageID, " //8
						      		+ "paragraphCounter, sentenceCounterinParagraph, sentenceCounterinMessage,isFirstParagraph,isLastParagraph,isEnglishOrCode,msgSubject, msgAuthorRole, isCorefParagraph, lastDir) " //17
						      		+ "values(?,?,?,?,?, ?,?,?,?,?, ?,?,?,?,?, ?,?,?)";
						      pstmt = prp.getConn().prepareStatement(query); // create a statement
						      pstmt.setInt(1, prp.getPep()); 		 pstmt.setString(2, CurrentSentenceString.trim());	pstmt.setString(3, "" /*PreviousSentenceString.trim()*/ );	 
						      pstmt.setString(4, "" /*nextSentence.trim()*/ );
						      pstmt.setString(5, "" /*thisParagraph.trim()*/ ); pstmt.setString(6, "" /*previousParagraph.trim()*/ );	pstmt.setString(7,"" /*nextParagraph.trim()*/ );		
						      pstmt.setInt(8, m.getMessage_ID());	 pstmt.setInt(9, paragraphCounter);		
						      pstmt.setInt(10, sentenceCounterinParagraph);	 		pstmt.setInt(11, sentenceCounter);    pstmt.setBoolean(12,isFirstParagraph); 	pstmt.setBoolean(13,isLastParagraph); 	
						      pstmt.setBoolean(14,isEnglish); /*isEnglishOrCode); */	pstmt.setString(15, "" /*msgSubject*/);	pstmt.setString(16, msgAuthorRole);		pstmt.setBoolean(17, isCorefParagraph);
						      pstmt.setString(18, m.getFolder());
						      pstmt.executeUpdate(); // execute insert statement	
//									      messageInsertedIntoTable = true;
//$$			     		 		System.out.println("\t inserted row in db  combination: "+r.getCombination() + " sentence " + r.getSentenceString());
						    } 
						    catch (Exception e) {
						      e.printStackTrace();
						      System.out.println(m.getMessage_ID() + " error inserting sentence in allsentences table, sentenceCounterinMessage:  "+sentenceCounterinMessage +" e: "+ e.toString() + "\n" );								  
							  System.out.println(StackTraceToString(e)  );	
						    }
							//p.finalize(); 				//System.gc();
						}
						
						//Relation extraction ...//if we want to do relation extraction						
						// dec 2018, we add code to extract additional fields isfirstparagraph, islastparagraph and messageauthorrole
						if(prp.getRelationExtraction()) {	//now for relation extraction
							try {
								//Long sentences give error in clausIE
								//so we only look at sentences which have less than 50 terms, note average english sentence has 14 terms
								//come up with a suitable length. maybe 
								if (CurrentSentenceString.split(" ").length < 100){
									if(prp.isReplaceCommas()  ){																		//relations_reverb
//&&										rvr.extractRelations(CurrentSentenceString, v_proposalNumber, v_message_ID,author, v_date, "relations");
										//reverb currently stopped at 240 //and also middle of 7
										//maybe for 240 onwards, code it to insert in all three types of relation tables
									}
									if (prp.isCheckClauseIETrue()){
										//clausIE
										//cie completed proposal 4..middle of proposal 5 - jave hout of memory
										//jan 2019, added line to get all verbs
										pair p = new pair();
										//String allVerbsInSentence = prp.getStanfordNLP().returnAllVerbsInSentence(CurrentSentenceString,p); //its lowercased and returned
//										System.out.println("\t Sentence Processed:  " + CurrentSentenceString + " allVerbsInMsgSubject " + allVerbsInMsgSubject + " allNounsInMsgSubject " +allNounsInMsgSubject);
										p = prp.getStanfordNLP().returnAllVerbsAndNounsInSentence(CurrentSentenceString,p);
										String allVerbsInSentence = p.getVerb(); 	String allNounsInSentence = p.getNoun();
										
										//String allNounsInSentence = prp.getStanfordNLP().returnAllVerbsInSentence(CurrentSentenceString);
										prp.getCm().extractClauseIERelations(prp.getConn(), prp.getProposalIdentifier(),CurrentSentenceString, PreviousSentenceString.trim(),nextSentence.trim(),
												thisParagraph.trim(),previousParagraph.trim(), nextParagraph.trim(), v_pepNumber, v_messageid,author, dt, prp.getTableToStoreExtractedRelations(),
												prp.getOutputRelationExtractionDetails(), sentenceCounter, paragraphCounter, lineNumber,isEnglish, isFirstParagraph, isLastParagraph,
												msgSubject, msgAuthorRole,isCorefParagraph,allVerbsInSentence,allNounsInSentence,allVerbsInMsgSubject,allNounsInMsgSubject);	//last two fields added dec 2018
//$$										prp.getCm().dispose();
//$$$									System.out.println("\t Sentence Processed:  " + CurrentSentenceString);
									}
									if(prp.isCheckOllieTrue()){
										//Ollie
//$$										jw.extractOllieRelationsw(CurrentSentenceString, v_pepNumber, v_messageid,author, dt, "relations_ollie",prp.getConn());
									}
								}
							}  //end try       
							catch (Exception e){ 
								System.out.println(m.getMessage_ID() + " error inserting record______here 232  " + e.toString() + "\n" );
								//System.out.println(Thread.currentThread().getStackTrace()  );		//+  " \n" + e.printStackTrace()
								System.out.println(StackTraceToString(e)  );	
								//continue;
							} 
						}
						
						//April 2019
						//first step in any OSSD Repository exploration as any other method doesnt show what operations are performed on jeps.
						//Triple extraction may be a way, but we want whole sentences where terms may not be dependen on each other through sub-verb-obj dependency
						if(prp.getCurrentAndNearbySentences()) {
							if(CurrentSentenceString.toLowerCase().contains("jep"))
								if(CurrentSentenceString.toLowerCase().contains("rejected") || CurrentSentenceString.toLowerCase().contains("completed"))
								{
									
								}	//write to file
									
						}
						
						//Process Mining
						if(prp.getProcessMining()) {
							try {
								//3. If or not coreference found above, send the current sentence for term checking and if S,V,O text exists, triple matching
								if(prp.getOutputfordebug())
									System.out.println("\t here a getProcessMining CurrentSentenceString" +CurrentSentenceString );
								SentenceAndNearby s = new SentenceAndNearby(CurrentSentenceString, PreviousSentenceString, prevSentence,nextSentence, paragraphCounter, sentenceCounter, sentenceCounterinParagraph,
										previousParagraph, thisParagraph,nextParagraph, lineNumber);
								s.setFirstParagraph(isFirstParagraph);	s.setLastParagraph(isLastParagraph);
								//System.out.println("SETTING isFirstParagraph:"+isFirstParagraph + " isLastParagraph" + isLastParagraph);
								foundLabel = prp.cesdl.checkEachSentenceForLabelTerms_ThenMatchAgaistDifferentLibrariesForTriples(prp,m,s);
									
								//maybe shud check if foundLabel is false??
	
								//DO MORE PROCESSSING..LOOK FOR MORE DATA for following cases in the same sentence
								//2. Capture Label Doubles, for all singles and doubles, - try match, and - if matched, find reason,  and - insert in results
								//check for BDFL Pronouncement, First Draft, second draft, RESULTS OF THE VOTE 
								//								System.out.println("After Checking sentence for triples in All Libraries, now checking for doubles");
								ProcessSinglesAndDoubles cpd = new ProcessSinglesAndDoubles();
								//assign tghe singles and doubles back from parameter object						
	
								if (found==false) {
									//System.out.println("\n\tDidn't Find double in sentence ");	
								}
	
								//3. isolated Terms single liners - these terms should exists on its own and not part of any sentence ? sometimes they are							
								//now we check for isolated terms - which exists on their own in a line, like bdfl pronouncement, rejection notice
								String[] isolatedTerms = prp.getIsolatedTerms();
								/* Jan 2019, since we are not looking for thse at the moment, we comment these, maybe useful later
								for (String it: isolatedTerms)	{									
									if (originalCurrentSentence.trim().contains(it)) {
										System.out.println("\n\tFound only Isolated Term in original sentence (" + it + ")");
										found = true;
										double_Found = cpd.processSinglesAndDoubles(thisParagraph,
												!it.contains(" ") ? it : it.replace(" ", "_"), prevSentence,
												CurrentSentenceString, nextSentence, v_pepNumber, m.getM_date(), m.getDateTimeStamp(), m.getMessage_ID(), m.getAuthor(),m.getAuthorsRole(), writerAll,
												writerForDisco, previousParagraph, nextParagraph, reasonTerms, cm,	reasons, prp.getReasonLabels(), prp);
									}										
								} 
								*/
							}  //end try       
							catch (Exception e){ 
								System.out.println(m.getMessage_ID() + " error inserting record______here 2377  " + e.toString() + "\n" );
								//System.out.println(Thread.currentThread().getStackTrace()  );		//+  " \n" + e.printStackTrace()
								System.out.println(StackTraceToString(e)  );	
								//continue;
							} 
						}
						
						//Feb 2019...we attempt to integrate reason extraction 
						if(prp.isReasonExtraction()) {
							
						}
						
						PreviousSentenceString = CurrentSentenceString;
						sentenceCounterinParagraph++;
					}//end of for loop for sentence
					counterPara++;	//we want to record if we are working with coreferenced paragraph
					//jan 2019, here we write the paragraph to 'allparagraphs' table, both the original and corefewrenced one
					//temp disabled
					if(prp.getSentenceParagraphWritingToTable()) { //)==false) {	//we use same variable as sentence writing
						//System.out.println("\t getSentenceParagraphWritingToTable");
						try { 
						  if (thisParagraph==null || thisParagraph.isEmpty() || thisParagraph.equals("") || thisParagraph.length()==0) { }	//do nothing - dont insert
						  else {
							  // to create unique id for sentence joining with paragraph with we store paragraphcounter+sentencecounter. Fields pepnumber, messageid will be stored in seprate columns
							  PreparedStatement pstmt = null;	
						      String query = "insert into "+tableNameToStore_paragraphLabelling+"(proposal,messageID, paragraphCounter, paragraph, isCorefParagraph, previousParagraph, nextParagraph, "
						      		+ "isFirstParagraph,isLastParagraph,msgSubject, msgAuthorRole) "
						      		+ "values(?,?,?,?,?, ?,?,?,?,?, ?)";
						      pstmt = prp.getConn().prepareStatement(query); // create a statement
						      pstmt.setInt(1, prp.getPep()); 		pstmt.setInt(2, m.getMessage_ID()); 		pstmt.setInt(3, paragraphCounter);	pstmt.setString(4, thisParagraph.trim());
						      pstmt.setBoolean(5, isCorefParagraph);	pstmt.setString(6, "" /*previousParagraph.trim()*/);	 pstmt.setString(7, "" /*nextParagraph.trim()*/);
						      pstmt.setBoolean(8,isFirstParagraph); 	pstmt.setBoolean(9,isLastParagraph);
						      pstmt.setString(10, msgSubject);	pstmt.setString(11, msgAuthorRole);
						      pstmt.executeUpdate(); // execute insert statement	
	//								      messageInsertedIntoTable = true;
	//$$			     		 		System.out.println("\t inserted row in db  combination: "+r.getCombination() + " sentence " + r.getSentenceString());
						  }
					    } 
					    catch (Exception e) {
					      e.printStackTrace();
					      System.out.println(m.getMessage_ID() + " error inserting sentence in allparagraphs table, paragraphCounterinMessage:  "+paragraphCounter +" e: "+ e.toString() + "\n" );								  
						  System.out.println(StackTraceToString(e)  );	
					    }
					}
					}//end for paragraph and coref paragraph
				}//end else if
				paragraphCounter++;				
			} //end for loop for paragraphs	        		
		}  //end try       
		catch (Exception e){ 
			System.out.println(m.getMessage_ID() + " error inserting record______here 23  " + e.toString() + "\n" );
			//System.out.println(Thread.currentThread().getStackTrace()  );		//+  " \n" + e.printStackTrace()
			System.out.println(StackTraceToString(e)  );	
			//continue;
		} 
		//return m.getWordsFoundList();
	}

	private static String findIndexesOfDots(String entireParagraph) {
		for(int s=2; s< entireParagraph.length()-3; s++) {		//no sentence would be 3 chars long only, so we can have thuis value as 3
			char kMinus1 = entireParagraph.charAt(s-1);	
			char k = entireParagraph.charAt(s);	//entireParagraph.substring(s,s+1);
			char kPlus1 = entireParagraph.charAt(s+1);	//entireParagraph.substring(s,s+2);	
			char kPlus2 = entireParagraph.charAt(s+2);	//entireParagraph.substring(s,s+3);
			//System.out.println("xxxxxxx k (" +k + ") kplus1 (" + kPlus1+ ") kplus2 ("+ kPlus2+ ")" );
			if(  k=='.' && Character.isLetter(kPlus1)) {		//kMinus1 == ' ' &&
				StringBuilder sb = new StringBuilder(),sb2 = new StringBuilder();
			    sb.append(kMinus1);		sb.append(k);  	 sb.append(kPlus1);
			    sb2.append(kMinus1);	sb2.append(' '); sb2.append(kPlus1);
			    String str = sb.toString(),str2 = sb2.toString();					
				entireParagraph = entireParagraph.replace(str, str2);
				//entireParagraph= entireParagraph.substring(0,k)+" "+entireParagraph.substring(k);
//						System.out.println("@@@@@@@@@@@@@@@@@@@@@ kMinus1 ("+kMinus1+") k(" +k + ") kplus1 (" + kPlus1+ ") kplus2 ("+ kPlus2+ ")" );
			}
		//	int dot1 = input.indexOf(".");
		//	int dot2 = input.indexOf(".", dot1 + 1);
			//String substr = input.substring(0, dot2);
		}
		return entireParagraph;
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

	//we coreference the sentence and send it for triple matching anc checking from here
	private static void coreferenceOnCurrentSentenceAndRestOfParagraph(Message m, ProcessingRequiredParameters prp,Integer sentenceCounter, String previousParagraph, String nextParagraph, String prevSentence,
			String[] coreferenceSubjects, String PreviousSentenceString, Integer sentenceCounterInParagraph,
			Integer paragraphCounter,String entireParagraph, String nextSentence, String CurrentSentenceString, String currSentenceTillParaEnd,
			Integer lineNum, String corefSubject, SentenceDetectorME detector) {
		boolean foundLabel;		String coReferencedcurrSentenceTillParaEnd ="";			
//		try {		
			//we only perform coreference for sentences which have the prominent terms, pep, proposal and BDFL	
			//There arew three ways to check for coreference 
			//1.check in coreferenced current sentence, which has been coreferenced because previous sentence contained prominent subject terms
			//System.out.println(" inside function ");
//			for(String corefSubject : coreferenceSubjects){	//System.out.println(" corefSubject 0: " +corefSubject);
//				if(PreviousSentenceString.contains(corefSubject))	{ //System.out.println(" contains corefSubject a: " +corefSubject);
//					String strToSend = PreviousSentenceString + " " + CurrentSentenceString;
//					if (PreviousSentenceString==null || PreviousSentenceString.isEmpty() || PreviousSentenceString =="")
//						strToSend = strToSend.substring(3);	//remove first two character = '' quotes
//					if(strToSend.contains("''"))
//						strToSend = strToSend.substring(3);	
					//using method
					//coReferencedSentence = prp.getCoref().returnCoRefText(strToSend);
					//directly using objects
					/*
					prp.getSCNLP().dos.writeUTF("C"+strToSend);
					 prp.getSCNLP().dis.readUTF();
					*/
					//new code embedded in current project in stanfordParser package
					//System.out.println("\t strToSend a: " +strToSend);
				try {
					coReferencedcurrSentenceTillParaEnd = prp.getStanfordNLP().resolveCoRef(currSentenceTillParaEnd);				
					System.out.println("\tCOREF corefSubject "+corefSubject+" currSentenceTillParaEnd sent: [" + currSentenceTillParaEnd + "] Received [" +coReferencedcurrSentenceTillParaEnd +"]");
					if(coReferencedcurrSentenceTillParaEnd.equals(currSentenceTillParaEnd)) {}
					else {	//we only insert rows if coreferenced text is different from original text
						try {
							//split into sentences
							Span[] spans = detector.sentPosDetect(entireParagraph);   //Detecting the position of the sentences in the paragraph  
							//Printing the sentences and their spans of a paragraph 
							String coreRefCurrentSentenceString=""; //,currSentenceTillParaEnd="";
							for (Span span : spans) {
								coreRefCurrentSentenceString = entireParagraph.substring(span.getStart(), span.getEnd()); 							
								SentenceAndNearby a = new SentenceAndNearby(coreRefCurrentSentenceString, PreviousSentenceString, prevSentence,nextSentence, paragraphCounter, sentenceCounter, sentenceCounterInParagraph,
									previousParagraph, entireParagraph,nextParagraph,lineNum);
								foundLabel = prp.cesdl.checkEachSentenceForLabelTerms_ThenMatchAgaistDifferentLibrariesForTriples(prp,m,a);
							}
						
							//String filename= "c:\\scripts\\snlp\\coreferencedSentences.txt";	    FileWriter fw = new FileWriter(filename,true); //the true will append the new data
						    //fw.write("PEP " + prp.getPep() + " | "+ corefSubject + " | " + strToSend + " | " + coReferencedSentence);//appends the string to the file
						    //fw.write("\r\n");	fw.close();					
						    String sql = "INSERT INTO debug_coref (proposal, messageID,corefSubject, isPrevSentOrSentOrPara,text, coref) VALUES (?,?,?,?,?,?)";					 
							PreparedStatement statement = prp.getConn().prepareStatement(sql);			
							statement.setInt	(1, prp.getPep() );			statement.setInt	(2,prp.getMessageID());			statement.setString (3, corefSubject);	
							statement.setInt	(4, 3);						statement.setString (5, currSentenceTillParaEnd);	statement.setString (6, coReferencedcurrSentenceTillParaEnd);	
							int rowsInserted = statement.executeUpdate();						
							if (rowsInserted > 0) {
							//    System.out.println("A new result record was inserted in DB successfully!");
							}
						
						}
						//catch(IOException ioe)	{		    System.err.println("IOException: " + ioe.getMessage());			}
						catch (SQLException e) {		
							e.printStackTrace();	System.out.println("Exception" + e.toString());						e.printStackTrace();
						} catch (Exception ex) {
							System.out.println("Exception" + ex.toString());						ex.printStackTrace();
						}
					}
					//now we remove the previousSentenceString
//					if(coReferencedSentence.contains(PreviousSentenceString))
//						coReferencedSentence = coReferencedSentence.replace(PreviousSentenceString, "");
					
		
				} catch (Exception e1) {
					// TODO Auto-generated catch block
					System.out.println("Coreference Application Server cannot be contacted");
					e1.printStackTrace();
					System.out.println(StackTraceToString(e1)  );	
				}  //sometimes "it" is referring to the term pep in a previous sentence
//				}

				//2.check if current sentence contains prominent subjects and then
				//System.out.println("checking CurrentSentenceString : " +CurrentSentenceString + " corefSubject: "+corefSubject);
				//dec 2018, dont think we need to do both
				/* if(CurrentSentenceString.contains(corefSubject))	{ //System.out.println(" \n\tcontains corefSubject b: " +corefSubject);
					//String strToSend = PreviousSentenceString + " " + CurrentSentenceString;
					//if (PreviousSentenceString==null || PreviousSentenceString.isEmpty() || PreviousSentenceString =="")
					//	strToSend = strToSend.substring(3);	//remove first two character = '' quotes
					//if(strToSend.contains("''"))
					//	strToSend = strToSend.substring(3);	
					//using method
					//coReferencedSentence = prp.getCoref().returnCoRefText(strToSend);
					//directly using objects
					// old way using sockets
					//prp.getSCNLP().dos.writeUTF("C"+CurrentSentenceString);
					//coReferencedSentence = prp.getSCNLP().dis.readUTF();
								
//					System.out.println(" CurrentSentenceString a: " +CurrentSentenceString);
					coReferencedSentence = prp.getStanfordNLP().resolveCoRef(CurrentSentenceString);				
					System.out.println("\tCOREF subject "+corefSubject+", coReferencedSentence sent: [" + CurrentSentenceString + "] Received [" +coReferencedSentence +"]");
					
//					try {
//						String filename= "c:\\scripts\\snlp\\coreferencedSentences.txt";					    FileWriter fw = new FileWriter(filename,true); //the true will append the new data
//					    fw.write("PEP " + prp.getPep() + " | "+ corefSubject + " | " + CurrentSentenceString + " | " + coReferencedSentence);//appends the string to the file
//					    fw.write("\r\n");					    fw.close();
//					}
//					catch(IOException ioe)		{		    System.err.println("IOException: " + ioe.getMessage());				}
					
					if(coReferencedSentence.equals(CurrentSentenceString)) {}
					else {	//we only insert rows if coreferenced text is different from original text
						try {
							//String filename= "c:\\scripts\\snlp\\coreferencedSentences.txt";	    FileWriter fw = new FileWriter(filename,true); //the true will append the new data
						    //fw.write("PEP " + prp.getPep() + " | "+ corefSubject + " | " + strToSend + " | " + coReferencedSentence);//appends the string to the file
						    //fw.write("\r\n");	fw.close();					
						    String sql = "INSERT INTO debug_coref (proposal, messageID,corefSubject, isPrevSentOrSentOrPara,text, coref) VALUES (?,?,?,?,?,?)";					 
							PreparedStatement statement = prp.getConn().prepareStatement(sql);			
							statement.setInt	(1, prp.getPep() );			statement.setInt	(2,prp.getMessageID());			statement.setString (3, corefSubject);	
							statement.setInt	(4, 2);						statement.setString (5, CurrentSentenceString);					statement.setString (6, coReferencedSentence);	
							int rowsInserted = statement.executeUpdate();						
							if (rowsInserted > 0) {
							//    System.out.println("A new result record was inserted in DB successfully!");
							}
						
						}
						//catch(IOException ioe)	{		    System.err.println("IOException: " + ioe.getMessage());			}
						catch (SQLException e) {		
							e.printStackTrace();	System.out.println("Exception" + e.toString());						e.printStackTrace();
						} catch (Exception ex) {
							System.out.println("Exception" + ex.toString());						ex.printStackTrace();
						}
					}
//										System.out.println("Currentb sentence contains prominent coref subjects, therefore coreferencing, coReferencedSentence sent: [" + CurrentSentenceString + "] Received [" +coReferencedSentence +"]");
					//now we remove the previousSentenceString
					if(coReferencedSentence.contains(PreviousSentenceString))
						coReferencedSentence = coReferencedSentence.replace(PreviousSentenceString, "");					
					SentenceAndNearby a = new SentenceAndNearby(coReferencedSentence, PreviousSentenceString, prevSentence,nextSentence, paragraphCounter, sentenceCounter, previousParagraph, 
							entireParagraph,nextParagraph, lineNum);
					foundLabel = prp.cesdl.checkEachSentenceForLabelTerms_ThenMatchAgaistDifferentLibrariesForTriples(prp,m,a);
				}
				*/
//			}
//
//		} catch (Exception e1) {
//			// TODO Auto-generated catch block
//			System.out.println("Coreference Application Server cannot be contacted");
//			e1.printStackTrace();
//			System.out.println(StackTraceToString(e1)  );	
//		}  //sometimes "it" is referring to the term pep in a previous sentence 
	}

	private static void checkMessageSubjectForDoublesTriples(ProcessingRequiredParameters prp, ArrayList<SinglesDoublesTriples> singlesDoublesTriplesList, Integer v_pepNumber, Integer v_messageid,
			Date dt,Timestamp v_datetimestamp, String author,String authorRole, String subject) {
		boolean f=false,df=false;
		//extract check doubles/triples in message subject line
		//subject "Propose rejection of PEPs 239 and 240 -- a builtin  rational type and rational literals"
		//extract double/tripls for sentence
		//for each of the double and triple
		//try matching
		//if matched, insert label into database - no reason checking, as no next previous in message subject

		//For teh sentence: "python dev  pep 289   generator expressions  second draft", the triples extraction code will catch triple terms but wont match triple labels
		// bUt in the following code, it will identify double "second draft"
		try{
			TripleProcessingResult ro = new TripleProcessingResult();
			//to insert result into db
			CheckInsertResult cir = new CheckInsertResult();

			//String printString3 = "PEP 308 Acceptance";//"Propose rejection of PEP";//			
			/*	old way using sockets
			prp.getSCNLP().dos.writeUTF("T"+subject);
			String triple = prp.getSCNLP().dis.readUTF();
			*/
			
			Boolean showGraphs=false;
			//String text = "Propose rejection of PEP";
			String triple= prp.getStanfordNLP().matchDoublesTripleInSentence(subject,showGraphs);			
			//System.out.println(" coReferencedSentence sent: [" + strToSend + "] Received [" +coReferencedSentence +"]");
			
			try {
				String filename= "c:\\scripts\\snlp\\matchDoublesTripleInSentence.txt";
			    FileWriter fw = new FileWriter(filename,true); //the true will append the new data
			    fw.write("PEP " + prp.getPep() + " | "+ subject + " | " + triple ); //+ " | " + coReferencedSentence);//appends the string to the file
			    fw.write("\r\n");			    fw.close();
			}
			catch(IOException ioe)	{
			    System.err.println("IOException: " + ioe.getMessage());
			}
			
//			System.out.println("## First checking message subject line, SVO Returned: [" + triple +"] subject ["+subject+"] messageid ("+v_messageid+")");
			String[] svoArray = triple.split(",");
			Integer len = svoArray.length;
			String s="",v="",o="";
			if(len==3){
				s = svoArray[0];				v = svoArray[1];				o = svoArray[2];
			}
			//				for(String pos: svoArray){
			//					System.out.print(pos+ " ");
			//				}

			for (SinglesDoublesTriples d : singlesDoublesTriplesList)	{
				//remove underscore 
				String label = d.getLabel();				String singleDoubleTerms = d.getSingle();
				//if (d.contains(" "))
				//	processedD = d.replace(" ","_");

				//first check singles
				if(d.isSingle()){
					if(subject.toLowerCase().contains(singleDoubleTerms)) {
						System.out.println("## MID "+v_messageid+" Found single (" + singleDoubleTerms +") in message subject line (" + subject +")");
						f = true;
						ro.setStateData(v_pepNumber,dt,v_datetimestamp,v_messageid, author,authorRole, label ,subject,label, label,label);			
						cir.insertResultStatesInDatabase(ro, false,prp);	//insert into database
					}
				}
				//then check for doubles and triples 
				// if the extracted s,v,o from message subject line only has two terms
				else if(d.isDouble()){
					//svoArray - has subject, verb, object
					String sub = d.getSubject();					String vb = d.getVerb();

					if(s.equals(sub) && v.equals(vb)){
						System.out.println("## Double matched in message subject line s:" +s+ " v " + v);
						ro.setStateData(v_pepNumber,dt,v_datetimestamp,v_messageid, author,authorRole, label ,subject,label, label,label);				
						cir.insertResultStatesInDatabase(ro, false,prp);	//insert into database
					}
				}
				// if the extracted s,v,o from message subject line has three terms
				else if(d.isTriple()){
					String sub = d.getSubject();					String vb = d.getVerb();					String ob = d.getObject();

					//double
					//if verb is empty or null, check subject and object
					if(vb.isEmpty() || vb==null || vb.equals("")){
						if( (s.equals(sub) && o.equals(ob)) || (s.equals(ob) && o.equals(sub))  ){
							System.out.println("## Double matched in message subject line s:" +s+ " o " + o);
							f = true;
							ro.setStateData(v_pepNumber,dt,v_datetimestamp,v_messageid, author,authorRole, label,subject,label, label,label);			
							cir.insertResultStatesInDatabase(ro, false,prp);	//insert into database
						}
					}
					//if object is empty, just check subject and verb
					if(ob.isEmpty() || ob==null || ob.equals("")){
						if(s.equals(sub) && v.equals(vb)){
							System.out.println("## Double matched in message subject line s:" +s+ " v " + v);
							f = true;
							ro.setStateData(v_pepNumber,dt,v_datetimestamp,v_messageid, author,authorRole, label,subject,label, label,label);				
							cir.insertResultStatesInDatabase(ro, false,prp);	//insert into database
						}
					}

					//triple
					if( (s.equals(sub) && v.equals(vb) && o.equals(ob))  || (s.equals(ob) && v.equals(vb) && o.equals(sub)) ){
						System.out.println("## Triple matched in message subject line, s:" +s+ " v " + v+ " o" +o);
						f = true;
						ro.setStateData(v_pepNumber,dt,v_datetimestamp,v_messageid, author,authorRole, label ,subject,label, label,label);			
						cir.insertResultStatesInDatabase(ro, false,prp);	//insert into database
					}
				}
			}

			//we will output only the first match ..maybe set the input file so main triples are set on tops
		}
		catch (Exception e) {
			System.out.println( e.getMessage() );
			System.out.println(StackTraceToString(e)  );	
		}
	}
	
	public static String getNextSentenceInCurrentParagraph(String entireParagraph,int v_sentenceCounterinParagraph, String nextParagraph) {
		String v_nextSentence = "";	Integer sentenceCounter; 		 
		PythonSpecificMessageProcessing pm = new PythonSpecificMessageProcessing();
		Boolean currSentenceFound = false;
		Integer counter=0;
		try {
			Reader reader = new StringReader(entireParagraph);
			DocumentPreprocessor dp = new DocumentPreprocessor(reader);
	
			for (List<HasWord> eachSentence : dp) 		{	
				//if found in previous round, get next sentence and return
				if (currSentenceFound){
					String CurrentSentenceString = Sentence.listToString(eachSentence);				v_nextSentence = CurrentSentenceString;
					return v_nextSentence;				
				}
				// as soon as sentence matched
	//			if (CurrentSentenceString.equals(v_currentSentenceString)){
	//				currSentenceFound = true;						
	//			}
				if(v_sentenceCounterinParagraph == counter) {				currSentenceFound = true;			}
				counter++;
			}
			
			//if it was last sentence in paragrapgh, we return the first sentence in next paragraph
			reader = new StringReader(nextParagraph);		dp = new DocumentPreprocessor(reader);
			for (List<HasWord> eachSentence : dp) {
				String CurrentSentenceString = Sentence.listToString(eachSentence);
				return CurrentSentenceString;
			}	
		}
		catch (Exception e){ 
			System.out.println("getNextSentenceInCurrentParagraph______here 23199  " + e.toString() + "\n" );
			//System.out.println(Thread.currentThread().getStackTrace()  );		//+  " \n" + e.printStackTrace()
			System.out.println(StackTraceToString(e)  );	
			//continue;
		}
		return v_nextSentence;
	}

	//these two functions borrows from evaluation class
	//check that first n words of manual sentence exists in automatic sentnec
	//pass both manual and automatic sentence as parameter
	//pass number of terms to check
	public static boolean checkWordOrder(List<String> automaticSentenceOrParagraph, List<String> manualCauseSentenceTerms, int numOfTermsToCheck ){
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
	
	public static String StackTraceToString(Exception ex) {
		String result = ex.toString() + "\n";
		StackTraceElement[] trace = ex.getStackTrace();
		for (int i=0;i<trace.length;i++) {
			result += trace[i].toString() + "\n";
		}
		return result;
	}

	@Override
	protected void finalize() throws Throwable
	{
		//        System.out.println("From Finalize Method, i = "+i);
	}

	
	
}
