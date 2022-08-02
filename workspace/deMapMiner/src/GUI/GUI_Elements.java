package GUI;

import java.awt.Container;
import java.awt.Dimension;
import java.awt.FlowLayout;
import java.awt.GridLayout;
import java.awt.TextArea;
import java.awt.TextField;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.awt.event.FocusEvent;
import java.awt.event.FocusListener;
import java.awt.event.MouseEvent;
import java.awt.event.WindowAdapter;
import java.awt.event.WindowEvent;
import java.net.MalformedURLException;
import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.ResultSetMetaData;
import java.sql.SQLException;
import java.sql.Statement;
import java.sql.Timestamp;
import java.text.ParseException;
import java.util.ArrayList;
import java.util.List;
import java.util.Vector;

import javax.swing.BorderFactory;
import javax.swing.BoxLayout;
import javax.swing.ButtonGroup;
import javax.swing.JButton;
import javax.swing.JComboBox;
import javax.swing.JFrame;
import javax.swing.JList;
import javax.swing.JOptionPane;
import javax.swing.JPanel;
import javax.swing.JRadioButton;
import javax.swing.JScrollPane;
import javax.swing.JTabbedPane;
import javax.swing.JTable;
import javax.swing.JTextArea;
import javax.swing.JTextField;
import javax.swing.JCheckBox;
import javax.swing.JTree;
import javax.swing.ListSelectionModel;

import javax.swing.*;
import javax.swing.table.*;

import java.awt.*;
import javax.swing.*;
import javax.swing.event.ChangeEvent;
import javax.swing.event.ChangeListener;

import readRepository.postReadingUpdates.UpdateAllMessages_EliminateMessagesNotBelongingToCurrentProposal;
import readRepository.readRepository.DateDemo;
import callIELibraries.ReVerbFindRelations;
import connections.MysqlConnect;

//highlight example
import java.awt.BorderLayout;
import java.awt.Color;
import java.awt.Component;
import java.awt.Font;
import java.io.BufferedReader;
import java.io.File;
import java.io.FileNotFoundException;
import java.io.FileReader;
import java.io.IOException;
import java.io.Reader;
import java.io.StringReader;

import javax.swing.JLabel;
import javax.swing.JTextPane;
import javax.swing.UIManager;
import javax.swing.border.Border;
import javax.swing.border.EmptyBorder;
import javax.swing.event.DocumentEvent;
import javax.swing.event.DocumentListener;
import javax.swing.event.ListSelectionListener;
import javax.swing.event.TreeSelectionEvent;
import javax.swing.event.TreeSelectionListener;
import javax.swing.table.DefaultTableCellRenderer;
import javax.swing.table.DefaultTableModel;
import javax.swing.table.TableCellRenderer;
import javax.swing.table.TableColumn;
import javax.swing.table.TableColumnModel;
import javax.swing.text.BadLocationException;
import javax.swing.text.DefaultCaret;
import javax.swing.text.Highlighter;
import javax.swing.tree.DefaultMutableTreeNode;
import javax.swing.tree.DefaultTreeModel;
import javax.swing.tree.MutableTreeNode;

import java.awt.BorderLayout;
import java.awt.Container;
import java.awt.Font;

import javax.swing.BorderFactory;
import javax.swing.JFrame;
import javax.swing.JLabel;
import javax.swing.border.BevelBorder;
//import org.apache.commons.lang.ArrayUtils;
import com.google.common.collect.ObjectArrays;

import java.text.DateFormat;
import java.text.SimpleDateFormat;
import java.util.Calendar;
import java.util.Date;

//import de.mpii.clause.driver.ClausIEMain;
//import de.mpii.clausie.ClausIE;
import edu.stanford.nlp.ling.HasWord;
import edu.stanford.nlp.ling.Sentence;
import edu.stanford.nlp.process.DocumentPreprocessor;
import GUI.helpers.DisplayNormativeStates;
import GUI.helpers.GUIHelper;
import GUI.helpers2.Console;
import GUI.helpers2.GUIGenerateCsv;
import GUI.InsertUpdateGet_CauseEffectSentences;
import miner.models.CustomUserObject;
import miner.postProcess.visualizeResults;
import miner.process.ProcessMessageAndSentence;
import miner.process.ProcessingRequiredParameters;
import miner.process.PythonSpecificMessageProcessing;
import connections.PropertiesFile;
import relationExtraction.relationsViewer;
import utilities.StateAndReasonLabels;
import utilities.ParagraphSentence;
import utilities.ReadFile;
import utilities.TableCellLongTextRenderer;
import utilities.UnderlineHighlighter;
import utilities.Utilities;
import utilities.WordSearcher;

import javax.swing.ListSelectionModel;
import javax.swing.event.ListSelectionEvent;
import javax.swing.event.ListSelectionListener;

//updated feb 2018 ..added goto button for a specific messageid from same resultset
//mar 2018..for every pep, the processing used to take place and took a nit of time ..1- 5 minutes
//so I changed to allow the processing to be done in batch and results stored in db so that the analysis can be faster
public class GUI_Elements extends JFrame {
		
	//june 2020..just to disable the reasons panel and all its components for screenshot
	public static boolean gettingScreenshotForSubstatesOnly = false;
	public static boolean addInfluenceTab = true, addSentimentTab = true;
	
	protected JButton getAccountButton, nextButton, previousButton, lastButton, firstButton, gotoButton, freeQueryButton, freeQueryButton2, searchWithDatesQueryButton,searchWithRowQueryButton, StatusChangedButton, 
	classifyButton, changeButton, gotoMessageIdButton, SQLButton, FindWordsButton, FindWordsSameSentenceButton, locationQueryButton,goToMIDButton,
	searchSameMessageSubjects,ifMessageForTheProposal,msgByAuthorButton,msgByBDFLButton,nextResultRecordButton,extractMsgSentReasonDataForReasonJTable, extractReasonDataForReasonJTable, extractDataForStateJTable,
	extractMsgSentInfluenceDataForReasonJTable, extractMsgSentSentimentDataForReasonJTable;
	public ButtonGroup  b4bgA1 = new ButtonGroup(), bgA1 = new ButtonGroup(); JRadioButton rA1=new JRadioButton("States"), rA2=new JRadioButton("RSA"), rA3=new JRadioButton("MT"),
			rA4=new JRadioButton("R"),rA5=new JRadioButton("-"), rA6=new JRadioButton("-");
	//dec 2019...message level and sentence level
	JRadioButton b4rA1=new JRadioButton("Message Based"), b4rA2=new JRadioButton("Sentence Based");
	JRadioButton b4rB1=new JRadioButton("Accepted"), b4rB2=new JRadioButton("Final"), b4rB3=new JRadioButton("Rejected");
	//jan 2021 ..we do same for influence
		//button groups i = influence
	public ButtonGroup  bg_influ_schemes = new ButtonGroup(), bg_influ_states = new ButtonGroup();
		//buttons
	JRadioButton bscheme_influ_a =new JRadioButton("Message Based"), bscheme_influ_b =new JRadioButton("Sentence Based");
	JRadioButton bstates_influ_a =new JRadioButton("Accepted"), bstates_influ_b =new JRadioButton("Final"), bstates_influ_c =new JRadioButton("Rejected");
	
	//may 2021 ..we do same for influence
		//button groups i = influence
	public ButtonGroup  bg_senti_schemes = new ButtonGroup(), bg_senti_states = new ButtonGroup();
		//buttons
	JRadioButton bscheme_senti_a =new JRadioButton("Message Based"), bscheme_senti_b =new JRadioButton("Sentence Based");
	JRadioButton bstates_senti_a =new JRadioButton("Accepted"), bstates_senti_b =new JRadioButton("Final"), bstates_senti_c =new JRadioButton("Rejected");
	
	public ButtonGroup bg1 = new ButtonGroup(); JRadioButton r1=new JRadioButton("All Messages"), r2=new JRadioButton("Nearby States");
	public ButtonGroup bg2 = new ButtonGroup(); JRadioButton r=new JRadioButton("None"), ra=new JRadioButton("5"),rb=new JRadioButton("10"), rc=new JRadioButton("15"); 
	public ButtonGroup bg3 = new ButtonGroup(); 
	protected JList accountNumberList;
	protected JTextField tsText, wordsText;
	protected static JTextField proposalNumberText;
	protected JTextField freeQueryText;
	protected JTextField proposalDetailsText;
	protected JTextField searchText;
	protected JTextField rowCountText;
	protected JTextField messageIdText;
	protected JTextField DateFromText;
	protected JTextField DateToText;
	protected JLabel messageIDText, dateText, locationText,msgNumberInFile, authorRole, messageType;
	protected JTextArea activeTSText, errorText, analyseWordsText, markedMessageText, classificationMessage, classificationWords, SQLText;
	protected static Connection connection;		protected Statement statement;	protected ResultSet rs;  
	protected static Integer Proposal_global;  
	boolean folderChanged = false;  
	protected JTree tree;  	protected JLabel selectedLabel = null;  
	static PythonSpecificMessageProcessing pm = new PythonSpecificMessageProcessing();
	public static String word,searchWords;
	public static Highlighter highlighter = new UnderlineHighlighter(null);
	static String messagesTableName = "allmessages";	
	static List<String> searchKeyList = new ArrayList<String>();
	
	static List<String> allStatesSubStates = null, terms;
	//{"Status: Draft","Status: Accepted","Status: Rejected","Status: Final","Status: Defer","Status: Withdraw","Status: Postpone","Status: Replace","Status: Incomplete",
	//"Status: Supercede","Status: Close","Status: Pending"},
	//committed state tableName - d         //bipstates_danieldata_datetimestamp															//people
	static String committedStateTableName = "states_danieldata_datetimestamp", extractedStateSubStateTableName = "results_postprocessed", roleTableName = "roles", companiesTableName = "companies";
	static JTable csJTable = new JTable() {   //committed states - daniel data   //left side bottom
		//dec 2019 ..color alternate rows
	    public Component prepareRenderer(TableCellRenderer renderer, int row, int column){
	        Component returnComp = super.prepareRenderer(renderer, row, column);
	        Color alternateColor = new Color(252,242,206);
	        Color whiteColor = Color.WHITE;
	        if (!returnComp.getBackground().equals(getSelectionBackground())){
	            Color bg = (row % 2 == 0 ? alternateColor : whiteColor);
	            returnComp .setBackground(bg);
	            bg = null;
	        }
	        return returnComp;
	    };	    
	};		
	static DefaultTableModel csmodel;	//associated model for the table
	
	static JTable essJTable = new JTable() {  //extracted states substates       //left side bottom
		//dec 2019 ..color alternate rows
	    public Component prepareRenderer(TableCellRenderer renderer, int row, int column){
	        Component returnComp = super.prepareRenderer(renderer, row, column);
	        Color alternateColor = new Color(252,242,206);
	        Color whiteColor = Color.WHITE;
	        if (!returnComp.getBackground().equals(getSelectionBackground())){
	            Color bg = (row % 2 == 0 ? alternateColor : whiteColor);
	            returnComp .setBackground(bg);
	            bg = null;
	        }
	        return returnComp;
	    };	    
	};	static DefaultTableModel essmodel;	//associated model for the table
	
	static JTable mldJTable = new JTable();		static DefaultTableModel mldmodel;	//table for message distribution ..doesnt work yet	
	//static JTable msgSentJTable = new JTable(); static DefaultTableModel msgSentmodel;  //new model for msg and sent based
	static JTable stateJTable = new JTable() {
	/*{  //manual state or substate extraction table on the right..is also used for showing candidate reason sentences 
	    //add tooltip to display the full cell text when not displayed
	    public String getToolTipText( MouseEvent e )   {
	    	String val="";
	    	//dec 2019 ddd
	    	try {
	    		int row = rowAtPoint( e.getPoint() );
		        int column = columnAtPoint( e.getPoint() );
		        Object value = getValueAt(row, column);
		        		        
		        if (value==null)
		        	val=null;
		        else 
		        	val=value.toString();
		       // System.out.println("Element in the given Row is :: "+row + " column: " + column + " value: " + val);
		        //return value == null ? null : value.toString();	           
	         } catch(ArrayIndexOutOfBoundsException e2) {
	            System.out.println("The index you have entered is invalid");
	         }
	    	return val;
	    } */
	    //dec 2019 ..color alternate rows
	    public Component prepareRenderer(TableCellRenderer renderer, int row, int column){
	        Component returnComp = super.prepareRenderer(renderer, row, column);
	        Color alternateColor = new Color(252,242,206);
	        Color whiteColor = Color.WHITE;
	        if (!returnComp.getBackground().equals(getSelectionBackground())){
	            Color bg = (row % 2 == 0 ? alternateColor : whiteColor);
	            returnComp .setBackground(bg);
	            bg = null;
	        }
	        return returnComp;
	    };	    
	};	 static DefaultTableModel model;		//associated model for the table
	
	
	static Integer globalCurrentRowInRecordset=0, rowCount=0;
	static String manualreasonextractionTable = "manualreasonextraction", reasonCandidatesTable= "autoextractedreasoncandidatesentences"; 
	//second table is the one where "auto extracted reasons" for suggestion which were computed using a java class, namely automaticreasonextractionmatching 
	static JTabbedPane jtp2 = new JTabbedPane(); //Tabbed Pane on right side bottom for reasons 
	
	//states and reasons
	static JRadioButton messageAuthorOnly = new JRadioButton("messageAuthorOnly"),BDFLOrDelegateOnly = new JRadioButton("BDFLOrDelegateOnly"),OtherCommunityMembers = new JRadioButton("OtherCommunityMembers"),
			termsMatchedMessageAuthorOnly = new JRadioButton("messageAuthorOnly"),termsMatchedBDFLOrDelegateOnly = new JRadioButton("BDFLOrDelegateOnly"),termsMatchedOtherCommunityMembers = new JRadioButton("OtherCommunityMembers"),
			stateOnly = new JRadioButton("StateOnly"), stateAndReason = new JRadioButton("State&Reason"), stateReasonWithCue = new JRadioButton("SRWithCue"),
			sentences = new JRadioButton("Sentences"),sentenceAndAdjacentSentences = new JRadioButton("Curr&AdjacentSentences"), 
			entireParagraphs = new JRadioButton("Paragraph"), adjacentParagraphs = new JRadioButton("ADParagraphs"),entireMessage = new JRadioButton("EntireMessage"); 

	static JComboBox<String> causeCategory,causeSubCategory, stateText, dmConcept, messageAuthorCombo,// = setMessageAuthorsComboBox(),
			combinationCombo,// = setCombinationComboBox(), 
			locationCombo, location, msgSubject,  // = setLocationComboBox(); //, nearbyStatesCombo = setNearbyStatesComboBox();
			influenceRoles, influenceCompany, //jan 2021
			sentimentRoles, sentimentCompany; //may 2021
	 
	static JComboBox<MessageSubject> msgSubjectCombo; //class created to store multiple values
	static JComboBox<MessageAuthor>  msgAuthorCombo, influencePersonCombo, sentimentPersonCombo; //class created to store multiple values
	static JComboBox<MessageDate>    msgDateCombo; //class created to store multiple values
	
	static JCheckBox addParameters = new JCheckBox("", false);	

	static ProcessMessageAndSentence pms = new ProcessMessageAndSentence();
	static ParagraphSentence ps = new ParagraphSentence();
	static ProcessingRequiredParameters prp = new ProcessingRequiredParameters();
	/* 	ClausIEMain cie = new ClausIEMain();   //clausIE   	
  		ReVerbFindRelations reverb = new ReVerbFindRelations();  //ReVerb  
  		ReVerbFindRelations rr  = new ReVerbFindRelations(); 	//ReVerb  	
  		JavaOllieWrapperGUI jo = new JavaOllieWrapperGUI();//Ollie
	 */
	//addded in july 2017
	public static JPanel main= new JPanel(), left= new JPanel(),middle= new JPanel(),right = new JPanel(), panel1,panel2,panel3,panel4,panel5,panel6,exploreAnalyseMessages;
	BorderLayout layout = new BorderLayout();
	
	final JTextField tf = new JTextField(20);	final JTextPane textPane = new JTextPane();
	GUIHelper guih = new GUIHelper();

	//mar 2018
	JButton insertAndShow  = new JButton("insert Cause & Effect And Show");
	static InsertUpdateGet_CauseEffectSentences iuce = new InsertUpdateGet_CauseEffectSentences();
	JTextField cause = new JTextField(40), effect = new JTextField(40), communityCauseSentence = new JTextField(40), authorCauseSentence = new JTextField(40), bdfldelegateCauseSentence = new JTextField(40); 
	final JTextField causeMID = new JTextField(10), effectMID = new JTextField(10), notesText =  new JTextField(30),communityCauseMID =  new JTextField(30), authorCauseMID =  new JTextField(30),	bdfldelegateCauseMID =  new JTextField(30);
	final WordSearcher searcher = new WordSearcher(activeTSText, Color.red);

	JScrollPane js3 = null;
	
	JButton showCandidate  = new JButton("Show Candidate Reason Sentences");	//jun 2018...extract reason candidate sentences

	static boolean automaticReasonExtraction = true;
	//Things updated for proposals
	//1.replaced "Proposal" with "proposal" in this file
	//2.added the proposal folder below. In this case it would point to JEPs
	//	public static File proposal_folder = new File("C:/OSSDRepositories/JEPs/");
	//3. added these two external functions as functions in this class
	//mru.initproposalNumberSearchKeyLists(statusList, ignoreList, proposalNumberSearchKeyList);
	//Map<Integer, String> proposalDetails = mru.populateAllproposalTitleandNumbers(new HashMap<Integer, String>(), conn);
	//4 identifier for matching in files
	public static String proposalIdentifier = "pep"; //null; 

	public static  StateAndReasonLabels l = new StateAndReasonLabels();	
	public static  ArrayList<ArrayList<String>>  reasonsTermsList = l.getReasonTerms();
	public static  ArrayList<ArrayList<String>>  specialOSCommunitySpecificTerms = l.getSpecialOSCommunitySpecificTerms();
	
	static boolean manualExtraction =false, // retrieve sample terms and allow the use to choose the keywords (provided with sentences)  
			autoSuggestCandidateSentences = true;  //using the keywords above, as part of our heuristics system, the script would output candidate sentences
	
	public static void main(String[] args) {
		/*
		allStatesSubStates = l.getAllStatesList_StatesSubstates();
		DeMAP_Miner_GUI exRepGUI = new DeMAP_Miner_GUI();
		exRepGUI.setDefaultCloseOperation(JFrame.DISPOSE_ON_CLOSE);
		//Ollie initialise		//JavaOllieWrapperGUI();

		PropertiesFile wpf = new PropertiesFile();	//Read from properties file
		proposalIdentifier = wpf.readFromPropertiesFile("proposalIdentifier",false).toLowerCase();	System.out.println("proposalIdentifier: " +proposalIdentifier);

		ReadFile rf = new ReadFile();	//add states from file to array
		String fileName = "c:\\scripts\\pepLabels\\pep.txt";
		//$$		terms = rf.readKeywordsFromFile(fileName);
		//statesAndTerms = ObjectArrays.concat(states, terms, String.class);
		exRepGUI.initDB();
		labelText = setValuesStates();	//populate states
		causeCategory = setValuesReasons(); //populate reasons
		causeSubCategory = setValuesSubReasons();
		exRepGUI.buildGUI();
		*/
	}

	protected void buildGUI() //look here to understand main tabbed sections initialised here
	{
		Container c = getContentPane();		c.setLayout(new FlowLayout()) ; //new BorderLayout());
		try {	UIManager.setLookAndFeel("com.sun.java.swing.plaf.windows.WindowsLookAndFeel");
		} catch (Exception evt) {}
		declare_Initialise_SetGUIElementValues();

		//MAIN TABS HERE //make the entire screen a tabbed pane
		//May 2020, I just comment out the tabs which 
		JTabbedPane mainTabbedPane = new JTabbedPane();
		/*mainTabbedPane.add( "Read In Data", panel1 );*/								mainTabbedPane.add("Analyse Proposals", main);
		visualizeResults vr = new visualizeResults(); 									//mainTabbedPane.add("ProMin Substates Results & PPR", vr );			//panel6
		ManuallyExtractedReasonsViewer merv = new ManuallyExtractedReasonsViewer();		
		//june 2020
		if(!gettingScreenshotForSubstatesOnly) {	
			mainTabbedPane.add("Manually Extracted Reasons", merv );	
	    }
		
	//	visualizeResults vrpp = new visualizeResults(); 			mainTabbedPane.add("Automated Analysis PostProcessing Results", vrpp );
		relationsViewer rv = new relationsViewer();										//mainTabbedPane.add("Relation Extraction Results", rv);   //COMMENT TO SAVE QUERY TIME
		c.add(mainTabbedPane);
		//right.add(stateJTable);//stateJTable.setBounds(100, 100, 100, 80); // add table in panel using add() method

		//pass the proposal number between tabs
		mainTabbedPane.addChangeListener(new ChangeListener() {
	        public void stateChanged(ChangeEvent e) {
	        	int index = mainTabbedPane.getSelectedIndex();		            System.out.println("Selected Tab: " + index);
	            if(index==1) {
	            	String pep = proposalNumberText.getText();
	            	if(pep==null || pep.isEmpty() || pep.equals("")) {}
	            	else {		merv.initiateFromMain(Integer.parseInt(pep));	}
	            }
	            else if(index==2) {
	            	String pep = proposalNumberText.getText();
	            	if(pep==null || pep.isEmpty() || pep.equals("")) {}
	            	else {		vr.initiateFromMain(Integer.parseInt(pep));	}
	            }
	        }
		});
		
		setDefaultCloseOperation(JFrame.DISPOSE_ON_CLOSE);		setLocationByPlatform(true);

		setTitle("DeMap Miner");
		show();
		//c.setEnabled(true);
		pack();
		//		setSize(1800, 1100);		setLocation(0, 0);
		setExtendedState(JFrame.MAXIMIZED_BOTH);
		
		setVisible(true);
		proposalNumberText.requestFocusInWindow();
	}

	protected void declare_Initialise_SetGUIElementValues() {
		//navigation
		freeQueryButton = new JButton("See Messages");
		nextResultRecordButton  = new JButton("Go to nextResult Record");
		msgByAuthorButton = new JButton("All Author Messages");
		msgByBDFLButton = new JButton("All BDFL Messages");
		extractReasonDataForReasonJTable = new JButton("Extract ReasonData For ReasonJTable");
		//extractMsgSentReasonDataForReasonJTable = new JButton("Extract Reasons:");
		extractMsgSentInfluenceDataForReasonJTable = new JButton("Extract Influence:"); //added Jan 2021
		extractMsgSentSentimentDataForReasonJTable = new JButton("Extract Sentiment:"); //added Jan 2021
		
		//Dimension dg = new Dimension(5,10);
		//extractMsgSentReasonDataForReasonJTable.setPreferredSize(dg);
		
		extractDataForStateJTable = new JButton("Extract Data For State Table");
		searchWithDatesQueryButton = new JButton("Search Dates");
		locationQueryButton = new JButton("Search List (keywords)");
		gotoMessageIdButton = new JButton("SearchMID (w/wo Prop.)");
		getAccountButton = new JButton("Get Account");
		nextButton = new JButton(">");
		goToMIDButton = new JButton("GoTo Date ->");
		searchWithRowQueryButton = new JButton("GoTo Row");
		lastButton = new JButton(">|");
		previousButton = new JButton("<");
		firstButton = new JButton("|<");
		
		JTable wordListTable = null; //table to display words
		JCheckBox statusCheck = new JCheckBox("", false);			JCheckBox statusChangedCheck = new JCheckBox("", false);	//statusCheck.add(new JLabel("Status "), "West");
		//JCheckBox addParameters = new JCheckBox("", false);	
		//  JFrame f = new JFrame("Highlight example");		//    final JTextPane textPane = new JTextPane();
		textPane.setHighlighter(highlighter);
//		JPanel pane = new JPanel();		pane.setLayout(new BorderLayout());		pane.add(new JLabel("Enter word/SQL: "), "North");		pane.add(tf, "Center");   //enter keyword		
		
		//now combo is declared on top and set in main class
		//JComboBox<String> location = setValuesFolder();		//JComboBox<String> sfromCBox = setValuesFolder1();	JComboBox<String> sToCBox = setValuesFolder2(); 

		accountNumberList = new JList();		accountNumberList.setVisibleRowCount(2);
		JScrollPane accountNumberListScrollPane = new JScrollPane(accountNumberList);
		proposalNumberText = new JTextField(3);			freeQueryText = new JTextField(40);		searchText = new JTextField(10);		searchText.add(new JLabel("ST "), "West");
		proposalNumberText.setNextFocusableComponent(freeQueryButton);
		//classificationText =  new JTextField(15);		classificationText.add(new JLabel("C "), "West");
		rowCountText = new JTextField(10);
		
		messageIDText = new JLabel();		dateText = new JLabel();		locationText = new JLabel();		tsText = new JTextField(10);			proposalDetailsText = new JTextField(10);		
		wordsText = new JTextField(10);		msgNumberInFile = new JLabel();   messageIdText = new JTextField(10);		DateFromText = new JTextField(10);		
		DateToText = new JTextField(10);
		
		messageIDText.setBorder(BorderFactory.createEtchedBorder());		dateText.setBorder(BorderFactory.createEtchedBorder());
		locationText.setBorder(BorderFactory.createEtchedBorder());			rowCountText.setBorder(BorderFactory.createEtchedBorder());
		msgNumberInFile.setBorder(BorderFactory.createEtchedBorder());     	
		
		searchSameMessageSubjects = new JButton("Search Messages with Same Subject");	authorRole = new JLabel(); 	messageType =new JLabel(); 
		authorRole.setBorder(BorderFactory.createEtchedBorder()); 
		messageType.setBorder(BorderFactory.createEtchedBorder());
		
		ifMessageForTheProposal = new JButton("If Message For The Proposal");

		//activeTSText = new JTextField(20);
		if(gettingScreenshotForSubstatesOnly)
			activeTSText = new JTextArea(70, 150); 
		else 
			activeTSText = new JTextArea(70, 55);
		
		activeTSText.setLineWrap(true);		activeTSText.setEditable(false);
		Font font = new Font("defaultFont", Font.PLAIN , 10);		activeTSText.setFont(font);

		//analyse words textfield
		analyseWordsText = new JTextArea(70, 55);		analyseWordsText.setLineWrap(true);		analyseWordsText.setEditable(false);	analyseWordsText.setFont(font);
		//Font font = new Font("defaultFont", Font.PLAIN , 10);

		//markedMessageText
		markedMessageText = new JTextArea(70, 55);		markedMessageText.setLineWrap(true);	markedMessageText.setEditable(false);	markedMessageText.setFont(font);

		classificationMessage = new JTextArea(5, 5);		classificationWords = new JTextArea(60, 35);
		SQLText = new JTextArea (10, 5);	errorText = new JTextArea(15, 5);		errorText.setEditable(false);
		SQLButton = new JButton("SQL");	FindWordsButton = new JButton("FindWords");		FindWordsSameSentenceButton = new JButton("FindWordsSS");

		//parameter panel
//		JPanel second = new JPanel();		//second.setLayout(new GridLayout(8, 8));						//datetimestamp  			//proposal						//messageSubject
		//JPanel secondlabels = new JPanel(new GridLayout(8,1));		JPanel secondcontrols = new JPanel(new GridLayout(8,8));  //JPanel secondButtons = new JPanel(new GridLayout(8,1));
		//second.add(secondlabels,BorderLayout.WEST);		second.add(secondcontrols, BorderLayout.EAST);		//second.add(secondButtons, BorderLayout.EAST);
//		second.add(new JLabel("MessageID:"));		second.add(new JTextField(2)); //messageIDText);
//		second.add(new JLabel("Date:"));			//secondcontrols.add(dateText);		
//		secondlabels.add(new JLabel("Location:"));		secondcontrols.add(locationText);		
//		secondlabels.add(new JLabel("MessageID:"));		secondcontrols.add(tsText);		
//		secondlabels.add(new JLabel("MessageID:"));		secondcontrols.add(proposalDetailsText);			
//		secondlabels.add(new JLabel("MessageID:"));		secondcontrols.add(wordsText);  
//		secondlabels.add(new JLabel("MessageID:"));		secondcontrols.add(searchSameMessageSubjects);	
//		secondlabels.add(new JLabel("MessageID:"));		secondcontrols.add(ifMessageForTheProposal);

		//SET VALUES
//		setButtonsEventListners(); //wordListTable, statusCheck, addParameters, statusChangedCheck, searcher, location); //, sfromCBox, sToCBox);	//very looong funtion
//		setValuesForWordSearcher(); //searcher);     
//		setValues(); //searchKeyList, searcher);

		//AssignValuesToGUIControls LeftMiddleAndRightPanels

		//unallocated which , left, centre or right
		//This is the start of adding panels		//create a left side panal and add the sub panels to it    
		JPanel left = new JPanel();		JPanel middle = new JPanel();  		JPanel center = new JPanel(); 	JTextArea output = new JTextArea("", 60, 60);	JPanel sixth = new JPanel(); JPanel seventh = new JPanel();
		JPanel eigth = new JPanel(new GridLayout(8,1));		JPanel labels7 = new JPanel(new GridLayout(5,1));		JPanel controls7 = new JPanel(new GridLayout(5,1)); 		JPanel tenth = new JPanel();
		sixth.setLayout(new BorderLayout(5,5));
		// no longer used
		//		output.setFont(new Font("Arial",Font.PLAIN,11));		
		//		output.setText(""); 
		//		Console.redirectOutput( output );	//setValues4(output, btnComputeSelectedText, btnComputeAllText);  

		//--------------------------------Left part
		JPanel fourth = new JPanel();
		fourth.add(firstButton);		fourth.add(previousButton);		fourth.add(nextButton);		fourth.add(lastButton);  fourth.add(goToMIDButton);	
		JPanel labels = new JPanel(new GridLayout(19,1));		JPanel controls = new JPanel(new GridLayout(19,1));	JPanel controlsButtons = new JPanel(new GridLayout(19,1));
		sixth.add(labels, BorderLayout.WEST);		sixth.add(controls, BorderLayout.CENTER); 		sixth.add(controlsButtons, BorderLayout.EAST);
		labels.add(new JLabel("Proposal:"));		controls.add(proposalNumberText);				controlsButtons.add(freeQueryButton);
		labels.add(new JLabel("With Text:"));		controls.add(searchText);						controlsButtons.add(nextResultRecordButton);
		labels.add(new JLabel("MessageID:"));		controls.add(messageIdText);					controlsButtons.add(gotoMessageIdButton);	
		labels.add(new JLabel("DateFrom:"));		controls.add(DateFromText);						controlsButtons.add(searchWithDatesQueryButton);
		labels.add(new JLabel("DateTo:"));			controls.add(DateToText);						controlsButtons.add(new JLabel(""));
		labels.add(new JLabel("Rowcount:"));		controls.add(rowCountText);						controlsButtons.add(searchWithRowQueryButton);
		//May 2021, 
		//msgSubjectCombo = "";
		//String[] messagesSub = new String[] {"Select Proposal"};    
		msgSubjectCombo = new JComboBox<MessageSubject>(); //(messagesSub);	
		msgAuthorCombo  = new JComboBox<MessageAuthor>(); //(messagesSub);	
		msgDateCombo    = new JComboBox<MessageDate>(); //(messagesSub);
		labels.add(new JLabel("ChooseMessageSubject:"));	controls.add(new JLabel(""));			controlsButtons.add(msgSubjectCombo); //msgSubjectCombo);			//controlsButtons.add(new JLabel(""));	
		labels.add(new JLabel("Only Author Messages:"));controls.add(new JLabel(""));				controlsButtons.add(msgByAuthorButton);
		labels.add(new JLabel("ChooseMessageAuthor:"));  controls.add(new JLabel(""));				controlsButtons.add(msgAuthorCombo);  //msgByBDFLButton
		labels.add(new JLabel("ChooseMessageDate:"));  controls.add(new JLabel(""));				controlsButtons.add(msgDateCombo);  //msgByBDFLButton
		labels.add(new JLabel("ChooseFolder"));		controls.add(addParameters);				    controlsButtons.add(location);			//controlsButtons.add(new JLabel(""));	
		labels.add(new JLabel("MSGNumberInFile"));	controls.add(msgNumberInFile);					controlsButtons.add(new JLabel(""));
		labels.add(new JLabel("AuthorRole"));		controls.add(authorRole);						controlsButtons.add(new JLabel(""));
		labels.add(new JLabel("MessageType"));		controls.add(messageType);						controlsButtons.add(new JLabel(""));
		//		searchText.setText("Use with Search Proposal");
//		labels.add(new JLabel("More Params")); 		controls.add(new JLabel(""));					controlsButtons.add(new JLabel(""));
		//another panel for setting values
		//seventh.setLayout(new BorderLayout(2,3)); 													  JPanel controlsButtons2 = new JPanel(new GridLayout(2,1));   
		//seventh.add(labels7, BorderLayout.WEST);		seventh.add(controls7, BorderLayout.CENTER);  seventh.add(controlsButtons2, BorderLayout.EAST);	
//		labels.add(new JLabel("ChooseFolder"));		controls.add(addParameters);				    controlsButtons.add(location);			//controlsButtons.add(new JLabel(""));				
		//		labels.add(new JLabel("Status From:"));		controls.add(statusChangedCheck);			    controlsButtons.add(sfromCBox);	
		//		labels.add(new JLabel("Status To:"));		controls.add(new JLabel(""));					controlsButtons.add(sToCBox);
		labels.add(new JLabel("MessageID:"));		controls.add(messageIDText); //messageIDText);
		labels.add(new JLabel("Date:"));			controls.add(dateText);		
		labels.add(new JLabel("Location:"));		controls.add(locationText);
		JTextField highlighter = new JTextField(10);
		labels.add(new JLabel("Find String: "));  controls.add(highlighter);
		//the query buttons are here - several shifted to another column in labels panel above
		/*
		eigth.add(freeQueryButton);		eigth.add(rspButton);	eigth.add(searchWithDatesQueryButton);	eigth.add(locationQueryButton);		eigth.add(wordsListButton);
		eigth.add(gotoMessageIdButton);	eigth.add(SQLButton);	eigth.add(FindWordsButton);				eigth.add(FindWordsSameSentenceButton);
		 */
		//tenth.add(scrollPaneAB);

		//add border
		/*pane.setBorder(BorderFactory.createCompoundBorder(new EmptyBorder(10,10,10,10), BorderFactory.createEtchedBorder()));	*/	fourth.setBorder(BorderFactory.createCompoundBorder(new EmptyBorder(10,10,10,10), BorderFactory.createEtchedBorder()));
		sixth.setBorder(BorderFactory.createCompoundBorder(new EmptyBorder(10,10,10,10), BorderFactory.createEtchedBorder()));		seventh.setBorder(BorderFactory.createCompoundBorder(new EmptyBorder(10,10,10,10), BorderFactory.createEtchedBorder()));
		eigth.setBorder(BorderFactory.createCompoundBorder(new EmptyBorder(10,10,10,10), BorderFactory.createEtchedBorder()));		//second.setBorder(BorderFactory.createCompoundBorder(new EmptyBorder(10,10,10,10), BorderFactory.createEtchedBorder()));
		//LEFT SIDE -- PARAMETERS
		left.setLayout(new BoxLayout(left, BoxLayout.PAGE_AXIS));
		JLabel headerLabel,blank;	headerLabel = new JLabel("Navigate Email Messages", JLabel.LEFT); 	
		Font f = headerLabel.getFont();
		headerLabel.setFont(f.deriveFont(f.getStyle() ^ Font.BOLD)); // bold
		left.add(headerLabel, BorderLayout.PAGE_END);		left.add(fourth, BorderLayout.PAGE_END);
		headerLabel = new JLabel("Parameters", JLabel.LEFT);		headerLabel.setFont(f.deriveFont(f.getStyle() ^ Font.BOLD)); // bold
		left.add(headerLabel, BorderLayout.PAGE_END);		left.add(sixth, BorderLayout.PAGE_END);
		headerLabel = new JLabel("Results Fields", JLabel.LEFT);		headerLabel.setFont(f.deriveFont(f.getStyle() ^ Font.BOLD)); // bold
		//left.add(headerLabel, BorderLayout.PAGE_END);		//left.add(second, BorderLayout.PAGE_END);
		headerLabel = new JLabel("Extracted Decision-making Processes");	blank = new JLabel("");
		headerLabel.setFont(f.deriveFont(f.getStyle() ^ Font.BOLD)); // bold		
		left.add(headerLabel, BorderLayout.PAGE_START);		/*left.add(headerLabel, BorderLayout.PAGE_END);	*/	//left.add(pane, BorderLayout.PAGE_END);	
		csJTable.setRowHeight(20); //add the committedstates jtable
		JScrollPane js=new JScrollPane(csJTable);		js.setVisible(true);
		essJTable.setRowHeight(20); //add the extracted states and substates jtable
		JScrollPane ess =new JScrollPane(essJTable);		ess.setVisible(true);
		
		//------------------------WORD HIGHLIGHTER
		//JPanel highlight = new JPanel(new GridLayout(3,2));
		//JTextField highlighter = new JTextField(10);
		//highlight.add(new JLabel("Search String: ")); highlight.add(highlighter);
		//JLabel headerLabel0 = new JLabel("Highlight Terms, ENTER", JLabel.LEFT);		headerLabel0.setFont(f.deriveFont(f.getStyle() ^ Font.BOLD));
		//JTextField highlighter = new JTextField(20);
		/*highlight.add(headerLabel0);	highlight.add(new JLabel("")); */ //highlight.add(new JLabel("Search String: ")); highlight.add(highlighter);	highlight.add(new JLabel("")); highlight.add(new JLabel(""));  
		
		
		//left.add(highlight);
		//wordsearcher highlighter
		JTabbedPane statestabbedPane = new JTabbedPane();	//tabbed pane to hold the states on left side of frame
		statestabbedPane.add(js,"    Main States");	statestabbedPane.add(ess,"With Sub-states");
		left.add(statestabbedPane);	

		main.setLayout(layout); 
		main.add(left,BorderLayout.WEST);
//		ShowStateTableColumnHeaders();	

		JScrollPane scroll = new JScrollPane (output); center.add(scroll);
		scroll.setVerticalScrollBarPolicy(JScrollPane.VERTICAL_SCROLLBAR_ALWAYS);		scroll.setHorizontalScrollBarPolicy(JScrollPane.HORIZONTAL_SCROLLBAR_ALWAYS);             
		DefaultCaret caret = (DefaultCaret)output.getCaret();		caret.setUpdatePolicy(DefaultCaret.ALWAYS_UPDATE);

		//-----------------------------------MIDDLE PART - Messages - tabbed pane
		JTabbedPane jtp = new JTabbedPane();		getContentPane().add(jtp);
		//jtp.setSize(300, 300);//(jtp.getPreferredSize());
		if(gettingScreenshotForSubstatesOnly)
			jtp.setPreferredSize(new Dimension(600,1000));
		
		
		JPanel jpA = new JPanel(),jpB = new JPanel(),jpC = new JPanel(),jpD = new JPanel();
		JScrollPane scrollPane2 = new JScrollPane(),jp = new JScrollPane(activeTSText), jp2 = new JScrollPane(analyseWordsText),jp3 = new JScrollPane(markedMessageText); 
		JLabel labelA = new JLabel();		labelA.setText(""); 	jpA.add(labelA);		jpA.add(jp);		
		JLabel labelB = new JLabel();		labelB.setText("");		jpB.add(labelB);		jpB.add(jp2);		
		JLabel labelC = new JLabel();		labelC.setText(""); 	jpC.add(labelC);		jpC.add(jp3);

		//folder distribution //dec 2019..not really needed
		/*mldJTable.setRowHeight(20); //add the committedstates jtable
		JScrollPane jsmld=new JScrollPane(mldJTable);		jsmld.setVisible(true);
		//right.add(mldJTable);
		jpD.add(jsmld); */
		
		
		
		//jtp.addTab("Message", jp);		   /* may 2020 jtp.addTab("Marked Message", jpC);  */            
		//commented to reduce gui's in screen capture
		//jtp.addTab("Quotes Removed", jpB);	
		//jtp.addTab("Message Distribution", jpD);			
		middle.add(jtp);
		if(gettingScreenshotForSubstatesOnly) {
			main.add(middle,BorderLayout.EAST);	//main.add(middle);
			jtp.addTab("Message", jp);
		}
		else {
			main.add(middle,BorderLayout.CENTER);
			jtp.addTab("Message", jpA);
		}
		//end tabbed pane
		//				main.add(left);//,BorderLayout.EAST //
		//add jtable
		// filling row and column data in JTable 
		//String col[] = {"Date", "MessageID","Terms matched","Sentence"};

		//----------------------------------RIGHT PART - State and Reason Term and location paramterers ...states information here ..		//paramters and results		
		JPanel  jj = new JPanel();	    jj.setLayout(new BorderLayout()); //     new BoxLayout(jj, BoxLayout.PAGE_AXIS)); //in this panel we will add the tabs and the j table
		stateJTable.setRowHeight(20);	//add JTable  
		
		
		//System.out.println("stateJTable.getColumnModel().getColumn(0) "+stateJTable.getColumnModel());
		//stateJTable.getColumnModel().getColumn(0).setPreferredWidth(20);
		//stateJTable.getColumnModel().getColumn(1).setPreferredWidth(12);
		//stateJTable.getColumnModel().getColumn(2).setPreferredWidth(12);
		
		//stateJTable.setSize(100, 100);
		//stateJTable.setPreferredScrollableViewportSize(stateJTable.getPreferredSize());
		//stateJTable.setFillsViewportHeight(true);
		js3 =new JScrollPane(stateJTable,JScrollPane.VERTICAL_SCROLLBAR_ALWAYS,JScrollPane.HORIZONTAL_SCROLLBAR_NEVER);    	// panel for the jtable	
		//js3.getViewport().setPreferredSize(new Dimension(320,300));
		js3.setVisible(true);	
		
		//stateJTable.setBounds(100, 100, 100, 80);
		//paramters
		ButtonGroup group = new ButtonGroup(), group0 = new ButtonGroup(), group1 = new ButtonGroup(),group2 = new ButtonGroup();		
		final WordSearcher searcher0 = new WordSearcher(activeTSText, Color.red);		
		//highlight the proposal number
		/*		final WordSearcher searcher1 = new WordSearcher(activeTSText, Color.green);
		int offset1 = searcher1.search(String.valueOf(proposalNum));
		if (offset1 != -1) {
            try {
            	activeTSText.scrollRectToVisible(activeTSText.modelToView(offset1));
            } catch (BadLocationException e2) {
            }
        }
		 */

		highlighter.addActionListener(new ActionListener() {
			@Override
			public void actionPerformed(ActionEvent evt) {
				String terms = highlighter.getText().trim(); 
				if(terms.contains(" ")) {	//multiple terms
					String allTerms[] = terms.split(" ");
					for (String t : allTerms) {
						int offset = searcher0.search(t);	//for each term
						if (offset != -1) {
							try {
								activeTSText.scrollRectToVisible(activeTSText.modelToView(offset));
							} catch (BadLocationException e) {       }
						}
					}
				}
				else {	//only one term
					int offset = searcher0.search(terms);	//for each term
					if (offset != -1) {
						try {
							activeTSText.scrollRectToVisible(activeTSText.modelToView(offset));
						} catch (BadLocationException e) {       }
					}
				}
			}
		});
		
		//Tabbed Section for Cause Effect and Reasons...on right side of frame
		//JTabbedPane jtp2 = new JTabbedPane(); 
		
		//JPanel StateSubstatesTabOnLeft = new JPanel(), StateSubstates_PostProcessedTabOnLeft = new JPanel();		
		JPanel beforefirstleftTabinRightSideOfFrame = new JPanel();	//dec 2019..added panel for heuristics based and sentence level message level based on sentence level
		JPanel firstleftTabinRightSideOfFrame = new JPanel(), secondLeftTabinRightSideOfFrame = new JPanel(),rightTabInRightSideOfFrame = new JPanel(),
				influenceTabRightSideOfFrame = new JPanel(), sentimentTabRightSideOfFrame = new JPanel();	//farrightTabInRightSideOfFrame = new JPanel(); 
		
		//DEC 2019 ...Sentence Level and Message level based on Sentence level heuristics based schemes added
		GridLayout experimentLayout129 = new GridLayout(0,2);	beforefirstleftTabinRightSideOfFrame.setLayout(experimentLayout129);
		//beforefirstleftTabinRightSideOfFrame.add(new JLabel("Message/Sentence Level Extraction Scheme:"));	//beforefirstleftTabinRightSideOfFrame.add(showCandidate); 
		//firstleftTabinRightSideOfFrame.add(new JLabel(""));	firstleftTabinRightSideOfFrame.add(new JLabel(""));
		JPanel b4jA1= new JPanel(); 
		//schemes...add to button in button group
		b4bgA1.add(b4rA1);	b4bgA1.add(b4rA2); 	// add radio buttons to button group  ....JPanel jA2= new JPanel(); JPanel jA3 = new JPanel(); 	
			//jpanel add radio buttons
		b4jA1.add(b4rA2); b4jA1.add(b4rA1);	 //add to panel ...message or sentence level radio butttons
		//states
		JPanel b4jA2 = new JPanel(); //b4jA2.add(b4rB1);	//b4jA2.add(b4rB2); 	// JPanel jA2= new JPanel(); JPanel jA3 = new JPanel(); 
			//...add to button in button group
		bg3.add(b4rB1);	bg3.add(b4rB3);		//add to button group
			//jpanel add radio buttons
		b4jA2.add(b4rB1); /*b4jA2.add(b4rB2);	*/ b4jA2.add(b4rB3);	 //accepted , final or rejected states
		
		//public ButtonGroup  bg_influ_schemes = new ButtonGroup(), bg_influ_states = new ButtonGroup();
		//jan 2021...same thing for influence radio button
		//for schemes
		JPanel b4jA1_influ = new JPanel(); 
		bg_influ_schemes.add(bscheme_influ_a); bg_influ_schemes.add(bscheme_influ_b); //add buttons to button group
		b4jA1_influ.add(bscheme_influ_a); b4jA1_influ.add(bscheme_influ_b);			  //add nuttons to jpanel
		//b4jA1_influ.add(b4rA2_influ); b4jA1_influ.add(b4rA1_influ);	
		//for states
		JPanel b4jA2_influ = new JPanel(); 
		bg_influ_states.add(bstates_influ_a);	bg_influ_states.add(bstates_influ_c);	
		b4jA2_influ.add(bstates_influ_a); b4jA2_influ.add(bstates_influ_c);	
		//b4jA2_influ.add(b4rB1_influ); /*b4jA2.add(b4rB2);	*/ b4jA2_influ.add(b4rB3_influ);
		
		
		//public ButtonGroup  bg_influ_schemes = new ButtonGroup(), bg_influ_states = new ButtonGroup();
		//may 2021...same thing for influence radio button
		//for schemes
		JPanel b4jA1_senti = new JPanel(); 
		bg_influ_schemes.add(bscheme_senti_a); bg_senti_schemes.add(bscheme_senti_b); //add buttons to button group
		b4jA1_senti.add(bscheme_senti_a); b4jA1_senti.add(bscheme_senti_b);			  //add nuttons to jpanel
		//b4jA1_influ.add(b4rA2_influ); b4jA1_influ.add(b4rA1_influ);	
		//for states
		JPanel b4jA2_senti = new JPanel(); 
		bg_senti_states.add(bstates_senti_a);	bg_senti_states.add(bstates_senti_c);	
		b4jA2_senti.add(bstates_senti_a); b4jA2_senti.add(bstates_senti_c);	
		//b4jA2_influ.add(b4rB1_influ); /*b4jA2.add(b4rB2);	*/ b4jA2_influ.add(b4rB3_influ);
		
		//firstleftTabinRightSideOfFrame.add(new JLabel("Nearby States: "));			firstleftTabinRightSideOfFrame.add(j1);
		beforefirstleftTabinRightSideOfFrame.add(new JLabel("    Scheme:"));			beforefirstleftTabinRightSideOfFrame.add(b4jA1);
		beforefirstleftTabinRightSideOfFrame.add(new JLabel("    State:"));				beforefirstleftTabinRightSideOfFrame.add(b4jA2); 
		//Extract Data
		//Dimension dg = new Dimension(5,10);
		//extractMsgSentReasonDataForReasonJTable.setPreferredSize(dg);
		//extractMsgSentReasonDataForReasonJTable.setSize(10,20);
		
		beforefirstleftTabinRightSideOfFrame.add(new JLabel("    "));					beforefirstleftTabinRightSideOfFrame.add(extractMsgSentInfluenceDataForReasonJTable);
		beforefirstleftTabinRightSideOfFrame.add(new JLabel(""));						beforefirstleftTabinRightSideOfFrame.add(new JPanel()); 
		
		//-------------------------AUTOMATIC SUGESTION OF CANDIDATE REASON SENTENCES 
		GridLayout experimentLayout12 = new GridLayout(0,2);	firstleftTabinRightSideOfFrame.setLayout(experimentLayout12);
		firstleftTabinRightSideOfFrame.add(new JLabel("Extract Candidate Reason Sentences:"));	firstleftTabinRightSideOfFrame.add(showCandidate); 
		//firstleftTabinRightSideOfFrame.add(new JLabel(""));	firstleftTabinRightSideOfFrame.add(new JLabel(""));
		JPanel jA1= new JPanel(); bgA1.add(rA1);	bgA1.add(rA2);  // bgA1.add(rA3);	bgA1.add(rA4);  bgA1.add(rA5);	bgA1.add(rA6); 	// JPanel jA2= new JPanel(); JPanel jA3 = new JPanel(); 	
		jA1.add(rA1); jA1.add(rA2);	 /* jA1.add(rA3); jA1.add(rA4); jA1.add(rA5); jA1.add(rA6); */ rA1.setSelected(true);
		//firstleftTabinRightSideOfFrame.add(new JLabel("Nearby States: "));			firstleftTabinRightSideOfFrame.add(j1);
		firstleftTabinRightSideOfFrame.add(new JLabel("States, Results of Substate Analysis (RSA)"));			firstleftTabinRightSideOfFrame.add(jA1);   //,MT,Reasons"
		firstleftTabinRightSideOfFrame.add(new JLabel("Extract Reasons :"));				firstleftTabinRightSideOfFrame.add(extractReasonDataForReasonJTable); 
		firstleftTabinRightSideOfFrame.add(new JLabel(""));								firstleftTabinRightSideOfFrame.add(new JPanel()); 
		firstleftTabinRightSideOfFrame.add(new JLabel(""));								firstleftTabinRightSideOfFrame.add(new JPanel()); 
		
		//-------------------------MANUAL EXTRACTION OF CAUSE EFFECT SECTION			
		//		JLabel headerLabel2 = new JLabel("Reason Extraction", JLabel.LEFT); 		
		GridLayout experimentLayout = new GridLayout(0,2);	                		secondLeftTabinRightSideOfFrame.setLayout(experimentLayout);
		secondLeftTabinRightSideOfFrame.add(new JLabel("State: "));					secondLeftTabinRightSideOfFrame.add(stateText);
		secondLeftTabinRightSideOfFrame.add(new JLabel("Decision-Making Scheme"));				secondLeftTabinRightSideOfFrame.add(dmConcept);
		secondLeftTabinRightSideOfFrame.add(new JLabel("Reason"));		secondLeftTabinRightSideOfFrame.add(causeCategory);
		//secondLeftTabinRightSideOfFrame.add(new JLabel("Cause Sub Category: "));	secondLeftTabinRightSideOfFrame.add(causeSubCategory);
		causeMID.setToolTipText("Cause - MessageID ");		secondLeftTabinRightSideOfFrame.add(causeMID);  cause.setToolTipText("Cause - Paragraph and Sentence: ");		secondLeftTabinRightSideOfFrame.add(cause);	
		effectMID.setToolTipText("Effect - MessageID ");	secondLeftTabinRightSideOfFrame.add(effectMID); effect.setToolTipText("Effect - Paragraph and Sentence: ");		secondLeftTabinRightSideOfFrame.add(effect);	
		//		cause.setText("Author Cause - Paragraph and Sentence: ");			leftTabinRightSideOfFrame.add(cause);					//author cause sentence or paragraph
		//		cause.setText("Community Cause - Paragraph and Sentence: ");		leftTabinRightSideOfFrame.add(cause);				//community cause sentence or paragraph
		communityCauseMID.setToolTipText("Community Cause - MessageID ");	secondLeftTabinRightSideOfFrame.add(communityCauseMID); communityCauseSentence.setToolTipText("Community Cause Sentence :");		secondLeftTabinRightSideOfFrame.add(communityCauseSentence);				
		authorCauseMID.setToolTipText("Author Cause - MessageID ");			secondLeftTabinRightSideOfFrame.add(authorCauseMID); authorCauseSentence.setToolTipText("Author Cause Sentence :");			secondLeftTabinRightSideOfFrame.add(authorCauseSentence);	
		bdfldelegateCauseMID.setToolTipText("BDFL Delegate Cause - MessageID ");	secondLeftTabinRightSideOfFrame.add(bdfldelegateCauseMID); bdfldelegateCauseSentence.setToolTipText("BDFL-delgate Cause Sentence :");secondLeftTabinRightSideOfFrame.add(bdfldelegateCauseSentence);	
		notesText.setToolTipText("Notes: ");		secondLeftTabinRightSideOfFrame.add(notesText);	insertAndShow.setToolTipText("Insert Cause And Effect and Show"); 			secondLeftTabinRightSideOfFrame.add(insertAndShow);
		
		//-------------------------AUTOMATED MANUAL EXTRACTION OF CAUSE EFFECT SECTION 
		stateOnly.setSelected(true);	    sentences.setSelected(true);
		//right tab of tabbed pane in right side of frame

		//addListenersToLabels();	//default text for textboxes which disapperas when clicked and reappears when left

		GridLayout experimentLayout2 = new GridLayout(0,2);	rightTabInRightSideOfFrame.setLayout(experimentLayout2);	//	farrightTabInRightSideOfFrame.setLayout(experimentLayout2);   
		// rightTabInRightSideOfFrame.add(new JLabel("Message Author: "));	rightTabInRightSideOfFrame.add(messageAuthorCombo);	  
		//	    JRadioButton r1=new JRadioButton("AllMessages");    	    JRadioButton r2=new JRadioButton("manualdataextraction"); ButtonGroup bg=new ButtonGroup(); bg.add(r1);bg.add(r2); 
		//	    JPanel j = new JPanel(); j.add(r1); j.add(r2);
		//	    JRadioButton ra=new JRadioButton("AllMessages");    	    JRadioButton rb=new JRadioButton("5msgsB4States"); 		  ButtonGroup bg2=new ButtonGroup(); bg2.add(ra);bg2.add(rb); 
		//	    JPanel j2 = new JPanel(); j2.add(ra); j2.add(rb);
		//	    rightTabInRightSideOfFrame.add(new JLabel("Data Source: "));	rightTabInRightSideOfFrame.add(j);		
		//	    rightTabInRightSideOfFrame.add(new JLabel("Messages: "));		rightTabInRightSideOfFrame.add(j2);	
		//rightTabInRightSideOfFrame.add(new JLabel("Nearby States: ")); 	rightTabInRightSideOfFrame.add(nearbyStatesCombo);	
		bg1.add(r1);	bg1.add(r2);  		JPanel j1 = new JPanel(); j1.add(r1); j1.add(r2);	r1.setSelected(true);
		rightTabInRightSideOfFrame.add(new JLabel("Nearby States: "));			rightTabInRightSideOfFrame.add(j1);
		bg2.add(ra);	bg2.add(rb); bg2.add(rc); 		JPanel j2 = new JPanel(); j2.add(ra); j2.add(rb);	j2.add(rc);  ra.setSelected(true);
		rightTabInRightSideOfFrame.add(new JLabel("Limit "));			rightTabInRightSideOfFrame.add(j2);
		rightTabInRightSideOfFrame.add(new JLabel("Message Author: "));	rightTabInRightSideOfFrame.add(messageAuthorCombo);	
		rightTabInRightSideOfFrame.add(new JLabel("Location: ")); 		rightTabInRightSideOfFrame.add(locationCombo);	
		rightTabInRightSideOfFrame.add(new JLabel("Combination: "));	rightTabInRightSideOfFrame.add(combinationCombo); 
		rightTabInRightSideOfFrame.add(new JLabel("Extract Data: "));	rightTabInRightSideOfFrame.add(extractDataForStateJTable); 
//		farrightTabInRightSideOfFrame.add(new JLabel("Extract Data: "));
		
		//dec 2019 added the sentence and message based on sentence level tab
		jtp2.addTab("Reasons", beforefirstleftTabinRightSideOfFrame);
		//this was here from before
		//may 2020, we just comment to hide these hard to explain tabs for thesis gui
		//jtp2.addTab("PEP Narrative", firstleftTabinRightSideOfFrame);
		jtp2.addTab("Enter Reasons Data", secondLeftTabinRightSideOfFrame);	
		//may 2020 comment
		//jtp2.addTab("Filter CER with Parameters", rightTabInRightSideOfFrame);	//jtp2.addTab("Update CER", farrightTabInRightSideOfFrame);
		//already commented before may 2020
		//jtp2.addTab("StateSubstate Results", StateSubstatesTabOnLeft);
		//jtp2.addTab("StateSubstate Results PP", StateSubstates_PostProcessedTabOnLeft);
		
		//jan 2021..influence tab
		//-------------------------INFLUENCE OF ROLES SECTION					
		GridLayout infuencefoRolesLayout = new GridLayout(0,2);	                	influenceTabRightSideOfFrame.setLayout(infuencefoRolesLayout);
		//add objects
		influenceTabRightSideOfFrame.add(new JLabel("Roles: "));					influenceTabRightSideOfFrame.add(influenceRoles);
		// 
		//JTextField test2 = new JTextField(20);
		influencePersonCombo = new JComboBox<MessageAuthor>(); 	
		sentimentPersonCombo       = new JComboBox<MessageAuthor>();	
		
		influenceTabRightSideOfFrame.add(new JLabel("Person: "));					influenceTabRightSideOfFrame.add(influencePersonCombo);
		influenceTabRightSideOfFrame.add(new JLabel("Company: "));					influenceTabRightSideOfFrame.add(influenceCompany);
		influenceTabRightSideOfFrame.add(new JLabel("    Scheme:"));				influenceTabRightSideOfFrame.add(b4jA1_influ);
		influenceTabRightSideOfFrame.add(new JLabel("    State:"));					influenceTabRightSideOfFrame.add(b4jA2_influ); 
		influenceTabRightSideOfFrame.add(new JLabel("    "));						influenceTabRightSideOfFrame.add(extractMsgSentInfluenceDataForReasonJTable);
		
		//may 2021..sentiment tab
		//-------------------------INFLUENCE OF ROLES SECTION					
		GridLayout sentimentfoRolesLayout = new GridLayout(0,2);	                sentimentTabRightSideOfFrame.setLayout(sentimentfoRolesLayout);
		//add objects
		sentimentTabRightSideOfFrame.add(new JLabel("Roles: "));					sentimentTabRightSideOfFrame.add(sentimentRoles);
		// 
		//JTextField test2 = new JTextField(20);
		sentimentTabRightSideOfFrame.add(new JLabel("Person: "));					sentimentTabRightSideOfFrame.add(sentimentPersonCombo);
		sentimentTabRightSideOfFrame.add(new JLabel("Company: "));					sentimentTabRightSideOfFrame.add(sentimentCompany);
		sentimentTabRightSideOfFrame.add(new JLabel("    Scheme:"));				sentimentTabRightSideOfFrame.add(b4jA1_senti);
		sentimentTabRightSideOfFrame.add(new JLabel("    State:"));					sentimentTabRightSideOfFrame.add(b4jA2_senti); 
		sentimentTabRightSideOfFrame.add(new JLabel("    "));						sentimentTabRightSideOfFrame.add(extractMsgSentSentimentDataForReasonJTable); //extractMsgSentSentimentDataForReasonJTable);
		//add to panel
		if (addInfluenceTab)
			jtp2.addTab("InfluenceOfRoles", influenceTabRightSideOfFrame);	
		if (addSentimentTab)
			jtp2.addTab("SentimentOfMembers", sentimentTabRightSideOfFrame);	
		
		
		
		
		jj.add(jtp2,BorderLayout.NORTH);	//ALL 3 tabbed panes are added here to the panel	
			
		    
	    JScrollPane newone =new JScrollPane(stateJTable);    //
		//newone.setBounds(600, 500, 400, 400);
	    
	    //jj.add(newone,BorderLayout.CENTER);
	    jj.add(newone,BorderLayout.CENTER);
	    //for(int p=0;p<5;p++)
	   	//jj.add(new JLabel("test: "),BorderLayout.SOUTH);
	    //jj.add(js3,BorderLayout.SOUTH);
	
		//jj.add(js3,BorderLayout.SOUTH);	//downbelow, the state jtable is added to the panel here through the scrollpane which contains the jtable
				
		//the same jtable is invoked by buttons in all 3 tabbed panes, as mostky same columns. We want to allow teh second tabbbed pane to use the jtable as well. 
		//One for manual, one for manual automated ad one for   
	    	    
	    //may 2020 new code
	    /*
	    JPanel  right2 = new JPanel();	   
	    right2.setLayout(new BoxLayout(right2, BoxLayout.PAGE_AXIS));
		JLabel headerLabelk = new JLabel("Navigate Records", JLabel.LEFT); 	
		headerLabelk.setFont(f.deriveFont(f.getStyle() ^ Font.BOLD)); // bold
		right2.add(jtp2, BorderLayout.PAGE_END);		//right2.add(fourth, BorderLayout.PAGE_END);
		headerLabelk = new JLabel("Parameters", JLabel.LEFT);		headerLabel.setFont(f.deriveFont(f.getStyle() ^ Font.BOLD)); // bold
		right2.add(newone, BorderLayout.PAGE_END);		//right2.add(sixth, BorderLayout.PAGE_END);
		main.add(right2,BorderLayout.EAST); 
	    */
	    
	    
	    if(gettingScreenshotForSubstatesOnly) {	    
		    for (Component cp : beforefirstleftTabinRightSideOfFrame.getComponents() ){
		        cp.setEnabled(false);
		    }
		    for (Component cp : secondLeftTabinRightSideOfFrame.getComponents() ){
		        cp.setEnabled(false);
		    }
		    stateJTable.setEnabled(false);

		    b4rA1.setEnabled(false);	b4rA2.setEnabled(false);
		    b4rB1.setEnabled(false);	b4rB2.setEnabled(false);	b4rB3.setEnabled(false);
		    

		    
		    Component[] com = beforefirstleftTabinRightSideOfFrame.getComponents();
		    for (int a = 0; a < com.length; a++) {
		       com[a].setEnabled(false);
		    }
	    }
	    
	    JPanel  right3 = new JPanel();	    right3.setLayout(new BorderLayout()); 
	    //jtp2.setEnabled(false);	    
	    //newone.setEnabled(false);
	    if(!gettingScreenshotForSubstatesOnly) {
	    	right3.add(jtp2, BorderLayout.NORTH);
	    	right3.add(newone, BorderLayout.CENTER);
	    	main.add(right3,BorderLayout.EAST); 
	    }
	    
	    if(gettingScreenshotForSubstatesOnly) {	
		    for (Component cp : right3.getComponents() ) // disable all components in the right panel, although hidden 
		    	cp.setEnabled(false);
		    //jan 2021..we try shifting the grid from bottom left to top right
		    //JTabbedPane statestabbedPane = new JTabbedPane();	//tabbed pane to hold the states on left side of frame
			 
	    } 	  
	   
	    
	    
		//old working code...all above is new
		//$$	right.add(jj) ; //stateJTable);// add table in panel using add() method
	    //main.add(right,BorderLayout.EAST); 
		proposalNumberText.requestFocusInWindow();  
		
		//may 2020
		//stateJTable.setAutoResizeMode(JTable.AUTO_RESIZE_OFF);
		
		/*TableColumn column = null;
		for (int i = 0; i < 5; i++) {
		    column = stateJTable.getColumnModel().getColumn(i);
		    if (i == 2) {
		        column.setPreferredWidth(20); //third column is bigger
		    } else {
		        column.setPreferredWidth(10);
		    }
		}*/
		//TableColumnModel tcm = stateJTable.getColumnModel();  
	    //for (int k = 0; k < tcm.getColumnCount(); k++) {
	    //    tcm.getColumn(k).setHeaderValue(columnNames[k]);
	    //}
		//TableColumnAdjuster tca = new TableColumnAdjuster(stateJTable);
		//tca.adjustColumns();
		
		/*TableColumn column = null;
		for (int h = 0; h < 5; h++) {
		    column = stateJTable.getColumnModel().getColumn(h);
		    if (h == 2) {
		        column.setPreferredWidth(100); //third column is bigger
		    } else {
		        column.setPreferredWidth(50);
		    }
		} */
		
	}
}
//A simple class that searches for a word in //a document and highlights occurrences of that word