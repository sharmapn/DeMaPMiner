package GUI;

import java.awt.Color;
import java.awt.Component;
import java.awt.Dimension;
import java.awt.Font;
import java.awt.GraphicsEnvironment;
import java.awt.Rectangle;
import java.awt.Toolkit;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.awt.event.FocusEvent;
import java.awt.event.FocusListener;
import java.io.FileInputStream;
import java.io.InputStream;
import java.io.Reader;
import java.io.StringReader;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;
import java.sql.Timestamp;
import java.text.DateFormat;
import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.List;

import javax.swing.JButton;
import javax.swing.JCheckBox;
import javax.swing.JComboBox;
import javax.swing.JFrame;
import javax.swing.JOptionPane;
import javax.swing.JTable;
import javax.swing.JTextArea;
import javax.swing.ListSelectionModel;
import javax.swing.SwingUtilities;
import javax.swing.event.ChangeEvent;
import javax.swing.event.ChangeListener;
import javax.swing.event.DocumentEvent;
import javax.swing.event.DocumentListener;
import javax.swing.event.ListSelectionEvent;
import javax.swing.event.ListSelectionListener;
import javax.swing.table.DefaultTableCellRenderer;
import javax.swing.table.DefaultTableModel;
import javax.swing.table.TableColumn;
import javax.swing.text.BadLocationException;
import javax.swing.text.DefaultHighlighter;
import javax.swing.text.Document;
import javax.swing.text.Highlighter;
import javax.swing.text.SimpleAttributeSet;
import javax.swing.text.StyleConstants;
import javax.swing.text.StyledDocument;

import readRepository.postReadingUpdates.UpdateAllMessages_EliminateMessagesNotBelongingToCurrentProposal;
import connections.MysqlConnect;
import edu.stanford.nlp.ling.HasWord;
import edu.stanford.nlp.ling.Sentence;
import edu.stanford.nlp.process.DocumentPreprocessor;
import javaStringSimilarity.info.debatty.java.stringsimilarity.Levenshtein;
import GUI.helpers.DisplayNormativeStates;
import opennlp.tools.sentdetect.SentenceDetectorME;
import opennlp.tools.sentdetect.SentenceModel;
import opennlp.tools.util.Span;
import utilities.TableCellLongTextRenderer;
import utilities.Utilities;
import utilities.WordSearcher;


public class GUI_ElementMethods extends GUI_Elements {
	
	static boolean comboBoxInitiallyUpdated = false; //combobox updates when initially updated, then again when item selected, thus a way to differentiate betrween the two
	
	//main functiosn to set the values of the elements in the GUI elements class
	String manuallyReasonExtractedTable = "trainingData", stateDataTableName = "results",stateData_PostProcessedTableName = "results_postprocessed",proposalDetailsTableName = "details";
	static String authorcorrected = "", bdfl_delegatecorrected = "";
	public void initialiseElements() {
		try {
			MysqlConnect mc = new MysqlConnect();			connection = mc.connect();
			if (connection == null){				System.exit(0);			}
			statement = connection.createStatement();			
			messageAuthorCombo = setMessageAuthorsComboBox();	/*set combo box values */ 		combinationCombo = setCombinationComboBox(); 
			locationCombo = setLocationComboBox();			stateText = setValuesStates();	//populate states
				
			//code added in jan 2020 ..for roles
			influenceCompany = setValuesCompany();	//populate roles
			influenceRoles = setValuesRoles();	//populate roles
			//we choose based on the proposal number ..so this method call is shifted to whne the proposal number is entered
			/*
			influencePersonCombo = setDummyValues(); //setValuesPerson(""); //Bitcoin developer"); //initially lets have dummy data
			influenceRoles.addActionListener (new ActionListener () {
			    public void actionPerformed(ActionEvent e) {
			        //doSomething();
			    	//test2.setText(influenceRoles.getSelectedItem() + " selected"); 
			    	//only below line added in jan 2020 ..for roles
			    	String p = (String) influenceRoles.getSelectedItem();
			    	System.out.println("\t	Develoer chosen: " + p);
					setValuesPerson("",influencePersonCombo);	//populate roles
					influencePersonCombo.repaint();
			    }
			});
			*/	
			
			//may 2021 ..just copy will modify later
			sentimentCompany = setValuesCompany();	//populate roles
			sentimentRoles = setValuesRoles();	//populate roles
			////we choose based on the proposal number ..so this method call is shifted to whne the proposal number is entered
			/*
			sentimentPersonCombo = setDummyValues(); //setValuesPerson(""); //Bitcoin developer"); //initially lets have dummy data
			sentimentRoles.addActionListener (new ActionListener () {
			    public void actionPerformed(ActionEvent e) {
			        //doSomething();
			    	//test2.setText(influenceRoles.getSelectedItem() + " selected"); 
			    	//only below line added in jan 2020 ..for roles
			    	String p = (String) sentimentRoles.getSelectedItem();
			    	System.out.println("\t	Develoer chosen: " + p);
			    	setValuesPerson("",sentimentPersonCombo);	//populate roles
			    	sentimentPersonCombo.repaint();
			    }
			});		
				*/
			
			dmConcept = setValuesDMConcept(); //dec 2019
			causeCategory = setValuesReasons(); /*populate reasons */ 			causeSubCategory = setValuesSubReasons();			location = setValuesFolder();
			buildGUI();			setValues(); //searchKeyList, searcher);
			
			ShowStateTableColumnHeaders();			
			
			setButtonsEventListners(); //wordListTable, statusCheck, addParameters, statusChangedCheck, searcher, location); //, sfromCBox, sToCBox);	//very looong funtion
			setValuesForWordSearcher(); //searcher);			
		} catch (SQLException e) {
			e.printStackTrace();// TODO Auto-generated catch block
		}
	}
	
	public void queryAndDisplayDataInGridForStates(Integer proposal) {		
		if (automaticReasonExtraction)
			showCandidate.doClick();
		else //(manualReasonExtraction)
			extractDataForStateJTable.doClick(); // we call this function so that the function inside the button listener gets executed
	}
	//extract Data For StateJTable .... within the curr resultset
	

	//Query and show Data for First table on Left
	//committed state cs 
	public void queryAndDisplayDataInGridForCommittedStates(Integer v_pepNum) {
		String[] columnNames = {"MessageID", "Date", "State"};
		JButton button = new JButton("See Message Details of Row");	
		String v_messageID = "",pepTitle="",email = "",forOutput_nextSentence = "",output = "";
		Statement stmt = null;		ResultSet rs;	
		Date dt;
		Integer forOutput_pepNumber = null, resultCounter=0;	
		try {
			stmt = connection.createStatement();			
			if (v_pepNum != null && v_pepNum <= 0) {
				return;
			}			
			//empty table contents, in cases where new search is issued
			csmodel.setRowCount(0);
			//csJTable.setRowHeight(30); 
			//select * from pepstates_danieldata_datetimestamp order by pep,datetimestamp ;  
			//somehow i didnot update the above table so for vidoe capturing purpose I just use the old table = `pepstates_danieldata_datetimestamp_firstoneused'
			//rs = stmt.executeQuery("select messageID_allmessages, date2,email, pepTitle from "+committedStateTableName+" where pep = " + v_pepNum + " order by dateTimeStamp");
			
			//jan 2021...for peps was 'pepstates_danieldata_datetimestamp_firstoneused'
			//for bips we changed
			rs = stmt.executeQuery("select messageID_allmessages, date2,email, "+proposalIdentifier+"Title from "+ proposalIdentifier+committedStateTableName +" where "+proposalIdentifier+" = " + v_pepNum + " order by dateTimeStamp");
			//rs.beforeFirst();
			while (rs.next()) {
				//v_message = rs.getString(1).toLowerCase();
				v_messageID = rs.getString(1);	 dt = rs.getDate(2);	pepTitle = rs.getString(4); email = rs.getString(3);	
				csmodel.addRow(new Object[]{v_messageID,dt,pepTitle,email});
				resultCounter++;
				//$$    		System.out.println(resultCounter+" commietd ststes rows");
			}

			csJTable.getColumnModel().getColumn(2).setCellRenderer(new TableCellLongTextRenderer ());  
			//csJTable.getColumnModel().getColumn(3).setCellRenderer(new TableCellLongTextRenderer ());
			csJTable.setRowHeight(20);
		} catch (SQLException e) {
			e.printStackTrace();
		}
	}
	//show proposal details values, author and bdfl delegate values
	public void queryAndDisplayProposalDetailsAuthorBDFL(Integer v_pepNum) {
		Statement stmt = null;		ResultSet rs;	
		try {
			stmt = connection.createStatement();			
			if (v_pepNum != null && v_pepNum <= 0) {
				return;
			}
			rs = stmt.executeQuery("select authorcorrected, bdfl_delegatecorrected from " +proposalIdentifier+proposalDetailsTableName+" where "+ proposalIdentifier +" = " + v_pepNum + " LIMIT 1"); //limit just in case, nut should be ok
			if (rs.next()) {
				authorcorrected = rs.getString("authorcorrected");		bdfl_delegatecorrected = rs.getString("bdfl_delegatecorrected");
				proposalDetailsText.setText(authorcorrected+ " " +bdfl_delegatecorrected);
			}
		} catch (SQLException e) {
			e.printStackTrace();
		}
	}
	
	//may 2020
	public void emptyresontable() {
		extractReasonDataForReasonJTable.doClick();
		if(!gettingScreenshotForSubstatesOnly)
			model.setRowCount(0);	//empty table contents, in cases where new search is issued
		System.out.println("\t	New proposal JTable cleared  ");
	}
	
	//For Extracted States And SubStates
	//for table on the left
	public void queryAndDisplayDataInGridForExtractedStatesAndSubStates(Integer v_pepNum) {
		String[] columnNames = {"MessageID", "Date", "State"};
		JButton button = new JButton("See Message Details of Row");	
		String v_messageID = "",currentSentence="",label = "",forOutput_nextSentence = "",output = "";
		Statement stmt = null;		ResultSet rs;			Date dt;
		Integer forOutput_pepNumber = null, resultCounter=0;	
		try {
			stmt = connection.createStatement();			
			if (v_pepNum != null && v_pepNum <= 0) {
				return;
			}			
			//empty table contents, in cases where new search is issued
			essmodel.setRowCount(0);
			//csJTable.setRowHeight(30); 
			//select * from pepstates_danieldata_datetimestamp order by pep,datetimestamp ;
			rs = stmt.executeQuery("select messageID, date,clausie, currentSentence from "+extractedStateSubStateTableName+" where " + proposalIdentifier +" = " + v_pepNum + " order by timeStamp");
			//rs.beforeFirst();
			while (rs.next()) {
				//v_message = rs.getString(1).toLowerCase();
				v_messageID = rs.getString(1);	 dt = rs.getDate(2);	 label = rs.getString(3);	currentSentence = rs.getString(4);
				essmodel.addRow(new Object[]{v_messageID,dt,label,currentSentence});
				resultCounter++;
				//$$    		System.out.println(resultCounter+" commietd ststes rows");
			}

			essJTable.getColumnModel().getColumn(2).setCellRenderer(new TableCellLongTextRenderer ());  
			//csJTable.getColumnModel().getColumn(3).setCellRenderer(new TableCellLongTextRenderer ());
			essJTable.setRowHeight(20);
		} catch (SQLException e) {
			e.printStackTrace();
		}
	}

	//message distribution by mailing list for pep
	/*
	public void queryAndDisplayDataInGridForPEPMailingListDistribution(Integer v_pepNum) {
		String[] columnNames = {"Mailing List", "Count"};
		JButton button = new JButton("See Message Details of Row");	
		String folder = ""; Integer counter;
		Statement stmt = null;		ResultSet rs;	

		Integer forOutput_pepNumber = null, resultCounter=0;	
		try {
			stmt = connection.createStatement();			
			if (v_pepNum != null && v_pepNum <= 0) {
				return;
			}			
			//empty table contents, in cases where new search is issued
			mldmodel.setRowCount(0);
			rs = stmt.executeQuery("select folder, count(messageid) from allmessages where pep = " + v_pepNum + " group by folder");			
			while (rs.next()) {
				folder = rs.getString(1);				counter = rs.getInt(2);				
				mldmodel.addRow(new Object[]{folder, counter});
			}

			mldJTable.getColumnModel().getColumn(1).setCellRenderer(new TableCellLongTextRenderer ());
			mldJTable.setRowHeight(20);
		} catch (SQLException e) {
			e.printStackTrace();
		}
	}
	*/
	//dec 2019 ..this function has headers set for all tables
	//yyy
	
	//The Grid on the right uses different models which need to be set
	protected  void ShowStateTableColumnHeaders() { 
		
		stateJTable.setModel(new DefaultTableModel());			model = (DefaultTableModel)stateJTable.getModel();// Model for Table
		
		//stateJTable.setAutoResizeMode(JTable.AUTO_RESIZE_OFF);	System.out.println("stateJTable.getColumnModel().getColumn(0) "+stateJTable.getColumnModel());
		//stateJTable.getColumnModel().getColumn(0).setPreferredWidth(20);
		
		
//		DefaultTableModel model30rows = new DefaultTableModel(40, 5);
//		stateJTable.setModel(model30rows);
		
		if (manualExtraction) {
			model.addColumn("Date"); model.addColumn("MID"); model.addColumn("Author"); model.addColumn("Sentence"); model.addColumn("Terms Matched");	model.addColumn("Level");
		}
		if (autoSuggestCandidateSentences) {
			//model.addColumn("Date"); model.addColumn("MID"); model.addColumn("Label"); model.addColumn("Author"); model.addColumn("Sentence"); model.addColumn("Terms Matched"); model.addColumn("Probability");
			//May 2020,,we want to reduvce the
			//if(!gettingscreenshot)
			model.addColumn("Date"); model.addColumn("MID"); /*model.addColumn("Label");*/ model.addColumn("Author"); model.addColumn("Sentence/Message"); model.addColumn("Score");
		}
		//Left side first table...//ShowCommittedStatesColumnHeaders() {
		csJTable.setModel(new DefaultTableModel());
		csmodel = (DefaultTableModel)csJTable.getModel();	// Model for Table
		csmodel.addColumn("MessageID");	csmodel.addColumn("Date");	csmodel.addColumn("Title"); csmodel.addColumn("State");
		//Left side second table...
		essJTable.setModel(new DefaultTableModel());
		essmodel = (DefaultTableModel)	essJTable.getModel();	// Model for Table
		essmodel.addColumn("MessageID");	essmodel.addColumn("Date");	essmodel.addColumn("Title"); essmodel.addColumn("State");
		//ShowMailingListDistribtionColumnHeaders() {
//		mldJTable.setModel(new DefaultTableModel());		 
		// Model for Table
//		mldmodel = (DefaultTableModel)mldJTable.getModel();
//		mldmodel.addColumn("Mailing List");	mldmodel.addColumn("Counter");	
		//model.setRowCount(30);
		//dec 2019 msg and sentence level scheme table results
		//msgSentJTable.setModel(new DefaultTableModel());
		//msgSentmodel = (DefaultTableModel)	msgSentJTable.getModel();	
		//msgSentmodel.addColumn("Date"); msgSentmodel.addColumn("MID"); msgSentmodel.addColumn("Label"); msgSentmodel.addColumn("Author"); msgSentmodel.addColumn("Sentence/Message"); msgSentmodel.addColumn("Probability");
		
	}

	public void setValuesForWordSearcher() { //final WordSearcher searcher) {
		tf.addActionListener(new ActionListener() {
			public void actionPerformed(ActionEvent evt) {
				word = tf.getText().trim();     int offset = searcher.search(word);                  //get text from keyword box				
				if (offset != -1) {
					try {	textPane.scrollRectToVisible(textPane.modelToView(offset));
					} catch (BadLocationException e) {	}
				}
				searchWords = searchText.getText().trim();     int offset2 = searcher.search(searchWords);                  //get text from keyword box				
				if (offset2 != -1) {
					try {	textPane.scrollRectToVisible(textPane.modelToView(offset));
					} catch (BadLocationException g) {	}
				} 
			}
		});

		//search for the words in mysql column ..like vote, etc
		searchText.addActionListener(new ActionListener() {
			public void actionPerformed(ActionEvent evt) {
				searchWords = tf.getText().trim();      int offset = searcher.search(searchWords);                 //get text from keyword box				
				if (offset != -1) {
					try {	textPane.scrollRectToVisible(textPane.modelToView(offset));
					} catch (BadLocationException e) {	}
				}          
			}
		});

		textPane.getDocument().addDocumentListener(new DocumentListener() {
			public void insertUpdate(DocumentEvent evt) {				searcher.search(word);			}
			public void removeUpdate(DocumentEvent evt) {				searcher.search(word);			}
			public void changedUpdate(DocumentEvent evt) {			}
		});
	}

	
	//The critical functions
	public void setButtonsEventListners() //JTable wordListTable, JCheckBox statusCheck, JCheckBox addParameters,JCheckBox statusChangedCheck, final WordSearcher searcher, JComboBox<String> location)
	//	,JComboBox<String> sfromCBox, JComboBox<String> sToCBox 
	{

		SQLButton.addActionListener(new ActionListener() {
			public void actionPerformed(ActionEvent e) {
				rowCount = 0;	globalCurrentRowInRecordset=0;
				try {
					String sqlText = tf.getText();
					rs = statement.executeQuery(sqlText);				rowCount = guih.returnRowCount(sqlText);		System.out.println("resultset rowCount = " + rowCount);  
					if (rs.next()) {
						globalCurrentRowInRecordset++;
						messageIDText.setText(rs.getString("messageid"));	proposalDetailsText.setText(authorcorrected+ " " +bdfl_delegatecorrected);	dateText.setText(rs.getString("date2"));
						locationText.setText(rs.getString("folder"));	tsText.setText(rs.getString("file"));		activeTSText.setText(rs.getString("email")); 	analyseWordsText.setText(rs.getString("analysewords")); 
						wordsText.setText(rs.getString("wordslist"));	rowCountText.setText(String.valueOf(globalCurrentRowInRecordset) + "/" + String.valueOf(rowCount));	
						activeTSText.setCaretPosition(0);									analyseWordsText.setCaretPosition(0);		markedMessageText.setCaretPosition(0);
					}
					int i = statement.executeUpdate(freeQueryText.getText()); 					errorText.append("Rows affected = " + i);				
				} catch (SQLException insertException) {
					displaySQLErrors(insertException);	}
				repaint();
			}
		});

		// search proposals		
		freeQueryButton.addActionListener(new ActionListener() {
			public void actionPerformed(ActionEvent e) {
				rowCount = 0;	globalCurrentRowInRecordset=0;
				try {
					//may 2020
					//empty reasons jtable
					//model.setRowCount(0);
					
					b4bgA1.clearSelection();					
					bg3.clearSelection();
					emptyresontable();
					model.setRowCount(0);
								
					js3.setVisible(false);	// panel for the jtable
					
					
					//code from beofre
					String proposal = proposalNumberText.getText(), src = searchText.getText();
					if (proposal.isEmpty() ) {	//
						JOptionPane.showMessageDialog(null, "Error", "InfoBox: " + "Required", JOptionPane.INFORMATION_MESSAGE);
					}					

					//if (folderCheck){//if we want to check specific mailing lists	//folderChanged == true
					//		rs = statement.executeQuery("SELECT * FROM "+messagesTableName+" WHERE "+proposalIdentifier+" = " + Integer.parseInt(proposalNumberText.getText()) + " AND folder LIKE '"+ selectedFolder + "' AND email LIKE '%" + searchText.getText()  + "%' AND statusChanged =1 order by dateTimeStamp, messageid asc, Proposal asc;");
					//}
					else{
						//System.out.println("here 2 "+messagesTableName + " "+proposalIdentifier + " Integer.parseInt(proposalNumberText.getText()) " + Integer.parseInt(proposalNumberText.getText())  + " searchText.getText()  " + searchText.getText() );
						String sqlQuery = "SELECT * FROM "+messagesTableName+" WHERE "+proposalIdentifier+"=" + Integer.parseInt(proposal) + " order by dateTimeStamp asc;";	//and folder NOT LIKE '%checkins%' 
						//"  AND (email LIKE '%" + searchText.getText()  + "%'  or subject LIKE '%" + searchText.getText()  + "%')
						//july 2018..we combine the pep summary 
						rs = statement.executeQuery(sqlQuery);
						rowCount = guih.returnRowCount(sqlQuery);
						//System.out.println("rowCount "+rowCount + " ");
						//String executedQuery = rs.getStatement().toString().;
						System.out.println("proposalIdentifier: "+proposalIdentifier+" resultset rowCount = " + rowCount  + " val: "+searchText.getText()); 

						while (rs.next()) {
							globalCurrentRowInRecordset++;
							String msg = rs.getString("email"),sub = rs.getString("subject");	String mid = rs.getString("messageid");
							if(sub==null || sub.isEmpty()) {sub= " "; }
							//check if proposal at teh end of message

							if (msg.contains(src) || sub.contains(src)) {
								mid = rs.getString("messageid");
								messageIDText.setText(mid);	proposalDetailsText.setText(authorcorrected+ " " +bdfl_delegatecorrected);	dateText.setText(rs.getString("date2"));
								locationText.setText(rs.getString("folder"));		tsText.setText(rs.getString("file"));	
								activeTSText.setText(msg);		analyseWordsText.setText(rs.getString("analysewords"));
								String newMessage=""; newMessage = assignNumbersToSentences_FindMessageEnd(msg, newMessage,rs.getString("author"));	//marked sentence
								markedMessageText.setText(newMessage);			
								msgNumberInFile.setText(rs.getString("msgNumInFile")); authorRole.setText(rs.getString("authorsRole")); messageType.setText(rs.getString("messageType"));
								wordsText.setText(sub);	rowCountText.setText(String.valueOf(globalCurrentRowInRecordset) + "/" + String.valueOf(rowCount));	
								activeTSText.setCaretPosition(0);				analyseWordsText.setCaretPosition(0);			markedMessageText.setCaretPosition(0);						
								causeMID.setText(mid);	effectMID.setText(mid);	//added for reasons 
								break;
							}
						}
						
						//may 2021..we add facility for showing all distinct message subject for the propsoal
						//added may 2021..threads
						//proposalNumberText.setText("");
						System.out.println("tin tin"); 
						msgSubjectCombo.removeAllItems();
						//msgSubjectCombo = setValuesRetrieveMsgSubjectsForProposal();
						setValuesRetrieveMsgSubjectsForProposal(msgSubjectCombo);
												
						msgAuthorCombo.removeAllItems();
						//msgSubjectCombo = setValuesRetrieveMsgSubjectsForProposal();
						setValuesRetrieveMsgAuthorsForProposal(msgAuthorCombo);
						
						msgDateCombo.removeAllItems();
						//msgSubjectCombo = setValuesRetrieveMsgSubjectsForProposal();
						setValuesRetrieveMsgDatesForProposal(msgDateCombo);
											
						//may 2021 for roles, we set the members in the drop down box
						influencePersonCombo.removeAllItems();
						setValuesPerson("",influencePersonCombo); //Bitcoin developer"); //initially lets have dummy data
						//setValuesRetrieveMsgAuthorsForProposal(influencePersonCombo);
						sentimentPersonCombo.removeAllItems();
						setValuesPerson("",sentimentPersonCombo); //Bitcoin developer"); //initially lets have dummy data
						
						
						revalidate();
						repaint();
						
						//show right side panel..automatic and manual reason candidates
						queryAndDisplayDataInGridForStates(Integer.parseInt(proposalNumberText.getText()));		//show states ..terms matched etc
						int ew = Integer.parseInt(proposalNumberText.getText());
						queryAndDisplayDataInGridForCommittedStates(ew); //show committed states
						queryAndDisplayProposalDetailsAuthorBDFL(ew);
						queryAndDisplayDataInGridForExtractedStatesAndSubStates(ew);
						//not sure if the below works or not
						int i = statement.executeUpdate(freeQueryText.getText()); 					errorText.append("Rows affected = " + i);
						//show message distribution for pep
//						queryAndDisplayDataInGridForPEPMailingListDistribution(ew);
						
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
						}*/
						
						//may 2021
//						getContentPane().revalidate(); // for JFrame up to Java7 is there only validate()
//						getContentPane().repaint();
						
						pack();
						//		setSize(1800, 1100);		setLocation(0, 0);
						
						
						//private void makeFrameFullSize(JFrame aFrame) {
						//may 2020..comment
						/*
						    Dimension screenSize = Toolkit.getDefaultToolkit().getScreenSize();
						    Component component = (Component) e.getSource();
					        JFrame frame = (JFrame) SwingUtilities.getRoot(component);
						    frame.setSize(screenSize.width, screenSize.height);
						//}
						    setExtendedState(frame.MAXIMIZED_BOTH);
						    
						    frame.setExtendedState(JFrame.MAXIMIZED_BOTH);
						    GraphicsEnvironment env = GraphicsEnvironment.getLocalGraphicsEnvironment();
						    frame.setMaximizedBounds(env.getMaximumWindowBounds());
						  */  
						    
						//setVisible(true);
					}
					
					//may 2020 clear jtable on right
					//DefaultTableModel dm = (DefaultTableModel)stateJTable.getModel();
					//dm.getDataVector().removeAllElements();
					//dm.fireTableDataChanged();
					//while (dm.getRowCount() > 0) {
					//	dm.removeRow(0);
					//}
					
					
				} catch (SQLException insertException) {
					displaySQLErrors(insertException);
				}
				tf.setText("Proposal " + proposalDetailsText.getText());				word = tf.getText().trim();  
				searcher.init(activeTSText);
				updateHighlightFields(searcher);
			}
		});
		//searching a proposal with searchtext may return multiple results and this button moves forward to the next record which contains the search result
			
		nextResultRecordButton.addActionListener(new ActionListener() {
			public void actionPerformed(ActionEvent e) {
				//rowCount = 0;	//globalCurrentRowInRecordset=0;
				try {
					String proposal = proposalNumberText.getText(), src = searchText.getText();
					if (proposal.isEmpty()) {
						JOptionPane.showMessageDialog(null, "Error", "InfoBox: " + "Required", JOptionPane.INFORMATION_MESSAGE);
					}
					else{
						while (rs.next()) {
							globalCurrentRowInRecordset++;
							String msg = rs.getString("email"),sub = rs.getString("subject");
							if (msg.contains(src) || sub.contains(src)) {
								String mid = rs.getString("messageid");
								messageIDText.setText(mid);	proposalDetailsText.setText(authorcorrected+ " " +bdfl_delegatecorrected);	dateText.setText(rs.getString("date2"));
								locationText.setText(rs.getString("folder"));		tsText.setText(rs.getString("file"));	
								activeTSText.setText(msg);		analyseWordsText.setText(rs.getString("analysewords"));
								String newMessage=""; newMessage = assignNumbersToSentences_FindMessageEnd(msg, newMessage,rs.getString("author"));	//marked sentence
								markedMessageText.setText(newMessage);			
								msgNumberInFile.setText(rs.getString("msgNumInFile")); authorRole.setText(rs.getString("authorsRole")); messageType.setText(rs.getString("messageType"));
								wordsText.setText(sub);	rowCountText.setText(String.valueOf(globalCurrentRowInRecordset) + "/" + String.valueOf(rowCount));	
								activeTSText.setCaretPosition(0);				analyseWordsText.setCaretPosition(0);			markedMessageText.setCaretPosition(0);						
								causeMID.setText(mid);	effectMID.setText(mid);	//added for reasons 
								break;
							}
						}
					}
				} catch (SQLException insertException) {
					displaySQLErrors(insertException);
				}
				tf.setText("Proposal " + proposalDetailsText.getText());				word = tf.getText().trim();  
				searcher.init(activeTSText);
				updateHighlightFields(searcher);
			}
		});

		// -- match author and senderemail in allmessages with authorCorrected, authorEmail in pepdetails
		msgByAuthorButton.addActionListener(new ActionListener() {
			public void actionPerformed(ActionEvent e) {
				rowCount = 0;	globalCurrentRowInRecordset=0;
				try {	System.out.println("All Author Messages");
				if (proposalNumberText.getText().isEmpty()) {
					JOptionPane.showMessageDialog(null, "Error", "InfoBox: " + "Required", JOptionPane.INFORMATION_MESSAGE);
				}					

				else{	
					String authorCorrected="",authorEmail="";
					//String sql = "select authorCorrected, authorEmail from pepdetails where pep = "+ Integer.parseInt(proposalNumberText.getText()) +";";
					String sql = "select authorCorrected, authorEmail from pepdetails where pep = "+ Integer.parseInt(proposalNumberText.getText()) +";";
					rs = statement.executeQuery(sql);	//reuse this rs variable
					rowCount = guih.returnRowCount(sql);	System.out.println("All Author Messages : Rowcount: "+ rowCount);
					String str = ""; 
					while (rs.next()) {
						authorCorrected= rs.getString("authorCorrected"); 	authorEmail= rs.getString("authorEmail");
						if (authorCorrected.contains(",")) {
							for (String s : authorCorrected.split(",")) {
								System.out.println("\tMultiple Authors, authorCorrected: "+ s + ", authorEmail: "+ authorEmail);	//HAVE TO SPLIT AUTHOR EMAIL
								str = str + " AND ( author LIKE '%" + s  + "%' OR senderemail LIKE '%" + authorEmail  + "%' )";
							}
						} else {
							System.out.println("Single Author, authorCorrected: "+ authorCorrected + ", authorEmail: "+ authorEmail);
							str = str + " AND ( author LIKE '%" + authorCorrected  + "%' OR senderemail LIKE '%" + authorEmail  + "%' )";
						}
					}
					String sqlQuery = "SELECT * FROM "+messagesTableName+" WHERE "+proposalIdentifier+"=" + Integer.parseInt(proposalNumberText.getText())  
					+   str  //+ " AND senderemail LIKE '%" + authorCorrected  + "%' "
					+ " AND (email LIKE '%" + searchText.getText()  + "%' or subject LIKE '%" + searchText.getText()  + "%') "
					+ " order by dateTimeStamp asc;";	//and folder NOT LIKE '%checkins%' 
					rs = statement.executeQuery(sqlQuery);
					rowCount = guih.returnRowCount(sqlQuery);
					System.out.println("proposalIdentifier: "+proposalIdentifier+" resultset rowCount = " + rowCount  + " val: "+searchText.getText()); 

					if (rs.next()) {
						globalCurrentRowInRecordset++;
						String mid = rs.getString("messageid");
						messageIDText.setText(mid);	proposalDetailsText.setText(authorcorrected+ " " +bdfl_delegatecorrected);	dateText.setText(rs.getString("date2"));
						locationText.setText(rs.getString("folder"));		tsText.setText(rs.getString("file"));	
						activeTSText.setText(rs.getString("email"));		analyseWordsText.setText(rs.getString("analysewords"));
						String msg = rs.getString("email"), newMessage=""; newMessage = assignNumbersToSentences_FindMessageEnd(msg, newMessage,rs.getString("author"));	//marked sentence
						markedMessageText.setText(newMessage);			
						msgNumberInFile.setText(rs.getString("msgNumInFile")); authorRole.setText(rs.getString("authorsRole")); messageType.setText(rs.getString("messageType"));
						wordsText.setText(rs.getString("subject"));	rowCountText.setText(String.valueOf(globalCurrentRowInRecordset) + "/" + String.valueOf(rowCount));	
						activeTSText.setCaretPosition(0);				analyseWordsText.setCaretPosition(0);			markedMessageText.setCaretPosition(0);						
						causeMID.setText(mid);	effectMID.setText(mid);	//added for reasons 
					}
					queryAndDisplayDataInGridForStates(Integer.parseInt(proposalNumberText.getText()));		//show states
					int ew = Integer.parseInt(proposalNumberText.getText());
					queryAndDisplayDataInGridForCommittedStates(ew); //show committed states
					queryAndDisplayProposalDetailsAuthorBDFL(ew);
					//not sure if the below works or not
					int i = statement.executeUpdate(freeQueryText.getText()); 					errorText.append("Rows affected = " + i);
					//show message distribution for pep
//					queryAndDisplayDataInGridForPEPMailingListDistribution(ew);
				}
				} catch (SQLException insertException) {
					displaySQLErrors(insertException);
				}
				tf.setText("Proposal " + proposalDetailsText.getText());				word = tf.getText().trim(); 	searcher.init(activeTSText);
				updateHighlightFields(searcher);
			}
		});
		
		msgByBDFLButton.addActionListener(new ActionListener() {
			public void actionPerformed(ActionEvent e) {
				rowCount = 0;	globalCurrentRowInRecordset=0;

				try {	System.out.println("All BDFL Messages");
				if (proposalNumberText.getText().isEmpty()) {
					JOptionPane.showMessageDialog(null, "Error", "InfoBox: " + "Required", JOptionPane.INFORMATION_MESSAGE);
				}					

				else{	
					String bdfl_delegate=""; //,authorEmail="";
					String sql = "select bdfl_delegate from pepdetails where pep = "+ Integer.parseInt(proposalNumberText.getText()) +";"; 
					rs = statement.executeQuery(sql);	//reuse this rs variable
					rowCount = guih.returnRowCount(sql);	System.out.println("All Author Messages : Rowcount: "+ rowCount);
					String str = ""; 
					while (rs.next()) {
						bdfl_delegate= rs.getString("bdfl_delegate"); 	//authorEmail= rs.getString("authorEmail");
						if (bdfl_delegate==null || bdfl_delegate.isEmpty() || bdfl_delegate =="") {
							System.out.println("No bdfl_delegate, adding Guido: "); //, authorEmail: "+ authorEmail);
							bdfl_delegate = "Guido"; 
						}
						if (bdfl_delegate.contains(",")) {
							for (String s : bdfl_delegate.split(",")) {
								System.out.println("\tMultiple bdfl_delegates, bdfl_delegate: "+ s ); //, authorEmail: "+ authorEmail);
								str = str + " AND author LIKE '%" + s  + "%' "; // OR senderemail LIKE '%" + authorEmail  + "%' )";
							}
						} else {
							System.out.println("Single bdfl_delegate, bdfl_delegate: "+ bdfl_delegate ); //, authorEmail: "+ authorEmail);
							str = str + " AND author LIKE '%" + bdfl_delegate  + "%' "; // OR senderemail LIKE '%" + authorEmail  + "%' )";
						}
					}
					String sqlQuery = "SELECT * FROM "+messagesTableName+" WHERE "+proposalIdentifier+"=" + Integer.parseInt(proposalNumberText.getText())  
					+   str  //+ " AND senderemail LIKE '%" + authorCorrected  + "%' "
					+ " AND (email LIKE '%" + searchText.getText()  + "%' or subject LIKE '%" + searchText.getText()  + "%') "
					+ " order by dateTimeStamp asc;";	//and folder NOT LIKE '%checkins%' 
					rs = statement.executeQuery(sqlQuery);
					rowCount = guih.returnRowCount(sqlQuery);
					System.out.println("proposalIdentifier: "+proposalIdentifier+" resultset rowCount = " + rowCount  + " val: "+searchText.getText()); 

					if (rs.next()) {
						globalCurrentRowInRecordset++;
						String mid = rs.getString("messageid");
						messageIDText.setText(mid);	proposalDetailsText.setText(authorcorrected+ " " +bdfl_delegatecorrected);	dateText.setText(rs.getString("date2"));
						locationText.setText(rs.getString("folder"));		tsText.setText(rs.getString("file"));	
						activeTSText.setText(rs.getString("email"));		analyseWordsText.setText(rs.getString("analysewords"));
						String msg = rs.getString("email"), newMessage=""; newMessage = assignNumbersToSentences_FindMessageEnd(msg, newMessage,rs.getString("author"));	//marked sentence
						markedMessageText.setText(newMessage);			
						msgNumberInFile.setText(rs.getString("msgNumInFile")); authorRole.setText(rs.getString("authorsRole")); messageType.setText(rs.getString("messageType"));
						wordsText.setText(rs.getString("subject"));	rowCountText.setText(String.valueOf(globalCurrentRowInRecordset) + "/" + String.valueOf(rowCount));	
						activeTSText.setCaretPosition(0);				analyseWordsText.setCaretPosition(0);			markedMessageText.setCaretPosition(0);						
						causeMID.setText(mid);	effectMID.setText(mid);	//added for reasons 
					}
					queryAndDisplayDataInGridForStates(Integer.parseInt(proposalNumberText.getText()));		//show states
					int ew = Integer.parseInt(proposalNumberText.getText());
					queryAndDisplayDataInGridForCommittedStates(ew); //show committed states
					queryAndDisplayProposalDetailsAuthorBDFL(ew);
					//not sure if the below works or not
					int i = statement.executeUpdate(freeQueryText.getText()); 					errorText.append("Rows affected = " + i);
					//show message distribution for pep
//					queryAndDisplayDataInGridForPEPMailingListDistribution(ew);
				}
				} catch (SQLException insertException) {
					displaySQLErrors(insertException);
				}	
				tf.setText("Proposal " + proposalDetailsText.getText());				word = tf.getText().trim(); 			searcher.init(activeTSText);
				updateHighlightFields(searcher);
			}
		});

		
		//may 2021 ..search for messages from the selected message subject
		
		
		//search for messages with same message subject	...before it was for the 'searchSameMessageSubjects' object
		//msgSubjectCombo
		msgSubjectCombo.addActionListener(new ActionListener() {
		public void actionPerformed(ActionEvent e) {
			//first occasion, the combobox is updated with message subjects and then later user selects the subject to see the messages
			if(!comboBoxInitiallyUpdated) //return if not updated yet 
				return;
			//else
			//	comboBoxInitiallyUpdated = true;
			
			rowCount = 0;	globalCurrentRowInRecordset=0;
			//may 2021 original code
			//String sub = String.valueOf(msgSubjectCombo.getSelectedItem());  //wordsText.getText();
			MessageSubject ms = (MessageSubject) msgSubjectCombo.getSelectedItem();
	        //display.append("\nYou selected the sizecode " + sizecode.label);
			String sub = ms.subjectShort, subLong = ms.subject; 
	        
			try {
				System.out.println("here search Same Message Subjects " + sub);
				if(sub.isEmpty() ||  sub.equals("")) {
				//System.out.println("here b "); 
				} else {
					System.out.println("here c! ");
					String query = "SELECT * FROM "+messagesTableName+" WHERE subject = '"+subLong+"' order by dateTimeStamp asc;";
					//check to see if we want to see all retrieved messages where status has changed 
					//rs = statement.executeQuery("SELECT * FROM "+messagesTableName+" WHERE "+proposalIdentifier+" + Integer.parseInt(proposalNumberText.getText()) + " AND email LIKE '%" + searchText.getText()  + "%' AND statusChanged =1 order by dateTimeStamp, messageid asc, Proposal asc;");
					rs = statement.executeQuery(query);						rowCount = guih.returnRowCount(query);
					System.out.println("here d");
					System.out.println("similar message subjects - resultset rowCount = " + rowCount);  
					if (rs.next()) {
						globalCurrentRowInRecordset++;
						String mid = rs.getString("messageid");
						messageIDText.setText(mid);	proposalDetailsText.setText(authorcorrected+ " " +bdfl_delegatecorrected);	dateText.setText(rs.getString("date2"));
						locationText.setText(rs.getString("folder"));		tsText.setText(rs.getString("file"));	
						activeTSText.setText(rs.getString("email"));		analyseWordsText.setText(rs.getString("analysewords"));
						String msg = rs.getString("email"), newMessage=""; newMessage = assignNumbersToSentences_FindMessageEnd(msg, newMessage,rs.getString("author"));	//marked sentence
						markedMessageText.setText(newMessage);			
						msgNumberInFile.setText(rs.getString("msgNumInFile"));   authorRole.setText(rs.getString("authorsRole")); messageType.setText(rs.getString("messageType"));
						wordsText.setText(rs.getString("subject"));		rowCountText.setText(String.valueOf(globalCurrentRowInRecordset) + "/" + String.valueOf(rowCount));		
						activeTSText.setCaretPosition(0);					analyseWordsText.setCaretPosition(0);		markedMessageText.setCaretPosition(0);
						causeMID.setText(mid);	effectMID.setText(mid);	//added for reasons 
					}
				}
			} catch (SQLException insertException) {					
				displaySQLErrors(insertException);	
			}
			//tf.setText("Proposal " + proposalDetailsText.getText());				word = tf.getText().trim();   	updateHighlightFields(searcher);
		}
		});
		
		//may 2021 search for messages with same author	...before it was for the 'searchSameMessageSubjects' object
		//msgSubjectCombo
		msgAuthorCombo.addActionListener(new ActionListener() {
		public void actionPerformed(ActionEvent e) {
			//first occasion, the combobox is updated with message subjects and then later user selects the subject to see the messages
			if(!comboBoxInitiallyUpdated) //return if not updated yet 
				return;
			//else
			//	comboBoxInitiallyUpdated = true;
			
			rowCount = 0;	globalCurrentRowInRecordset=0;
			//may 2021 original code
			//String sub = String.valueOf(msgSubjectCombo.getSelectedItem());  //wordsText.getText();
			MessageAuthor ms = (MessageAuthor) msgAuthorCombo.getSelectedItem();
	        //display.append("\nYou selected the sizecode " + sizecode.label);
			String ashort = ms.authorShort, aLong = ms.author; 
	        
			try {
				System.out.println("here search Same Message Authors sub " + ashort + " subLong " + aLong);
				if(aLong.isEmpty() ||  aLong.equals("")) {
				//System.out.println("here b "); 
				} else {
					System.out.println("here c! ");
					String query = "SELECT * FROM "+messagesTableName+" WHERE " + proposalIdentifier+ "=" + Integer.parseInt(proposalNumberText.getText()) + " and sendername = '"+aLong+"' order by dateTimeStamp asc;";
					//check to see if we want to see all retrieved messages where status has changed 
					//rs = statement.executeQuery("SELECT * FROM "+messagesTableName+" WHERE "+proposalIdentifier+" + Integer.parseInt(proposalNumberText.getText()) + " AND email LIKE '%" + searchText.getText()  + "%' AND statusChanged =1 order by dateTimeStamp, messageid asc, Proposal asc;");
					rs = statement.executeQuery(query);						rowCount = guih.returnRowCount(query);
					System.out.println("here d");
					System.out.println("similar message authors - resultset rowCount = " + rowCount);  
					if (rs.next()) {
						globalCurrentRowInRecordset++;
						String mid = rs.getString("messageid");
						messageIDText.setText(mid);	proposalDetailsText.setText(authorcorrected+ " " +bdfl_delegatecorrected);	dateText.setText(rs.getString("date2"));
						locationText.setText(rs.getString("folder"));		tsText.setText(rs.getString("file"));	
						activeTSText.setText(rs.getString("email"));		analyseWordsText.setText(rs.getString("analysewords"));
						String msg = rs.getString("email"), newMessage=""; newMessage = assignNumbersToSentences_FindMessageEnd(msg, newMessage,rs.getString("author"));	//marked sentence
						markedMessageText.setText(newMessage);			
						msgNumberInFile.setText(rs.getString("msgNumInFile"));   authorRole.setText(rs.getString("authorsRole")); messageType.setText(rs.getString("messageType"));
						wordsText.setText(rs.getString("subject"));		rowCountText.setText(String.valueOf(globalCurrentRowInRecordset) + "/" + String.valueOf(rowCount));		
						activeTSText.setCaretPosition(0);					analyseWordsText.setCaretPosition(0);		markedMessageText.setCaretPosition(0);
						causeMID.setText(mid);	effectMID.setText(mid);	//added for reasons 
					}
				}
			} catch (SQLException insertException) {					
				displaySQLErrors(insertException);	
			}
			//tf.setText("Proposal " + proposalDetailsText.getText());				word = tf.getText().trim();   	updateHighlightFields(searcher);
		}
		});
		
		//may 2021
		msgDateCombo.addActionListener(new ActionListener() {
			public void actionPerformed(ActionEvent e) {
				//first occasion, the combobox is updated with message subjects and then later user selects the subject to see the messages
				if(!comboBoxInitiallyUpdated) //return if not updated yet 
					return;
				//else
				//	comboBoxInitiallyUpdated = true;
				
				rowCount = 0;	globalCurrentRowInRecordset=0;
				
		        
				try {
					//may 2021 original code
					//String sub = String.valueOf(msgSubjectCombo.getSelectedItem());  //wordsText.getText();
					MessageDate md = (MessageDate) msgDateCombo.getSelectedItem();
			        //display.append("\nYou selected the sizecode " + sizecode.label);
					Date dat = md.dt; //, aLong = md.dt; 
					
					
					System.out.println("here search Same Message Date sub " + dat);// + " subLong " + aLong);
					if(dat == null ||  dat == null) {
					    System.out.println("here bhjk "); 
					} else {
//						System.out.println("here c! ");
						String query = "SELECT * FROM "+messagesTableName+" WHERE " + proposalIdentifier+ "=" + Integer.parseInt(proposalNumberText.getText()) + " and date2 = '"+dat+"' order by dateTimeStamp asc;";
						//check to see if we want to see all retrieved messages where status has changed 
						//rs = statement.executeQuery("SELECT * FROM "+messagesTableName+" WHERE "+proposalIdentifier+" + Integer.parseInt(proposalNumberText.getText()) + " AND email LIKE '%" + searchText.getText()  + "%' AND statusChanged =1 order by dateTimeStamp, messageid asc, Proposal asc;");
						rs = statement.executeQuery(query);						rowCount = guih.returnRowCount(query);
//						System.out.println("here d");
						System.out.println("similar message date - resultset rowCount = " + rowCount);  
						if (rs.next()) {
							globalCurrentRowInRecordset++;
							String mid = rs.getString("messageid");
							messageIDText.setText(mid);	proposalDetailsText.setText(authorcorrected+ " " +bdfl_delegatecorrected);	dateText.setText(rs.getString("date2"));
							locationText.setText(rs.getString("folder"));		tsText.setText(rs.getString("file"));	
							activeTSText.setText(rs.getString("email"));		analyseWordsText.setText(rs.getString("analysewords"));
							String msg = rs.getString("email"), newMessage=""; newMessage = assignNumbersToSentences_FindMessageEnd(msg, newMessage,rs.getString("author"));	//marked sentence
							markedMessageText.setText(newMessage);			
							msgNumberInFile.setText(rs.getString("msgNumInFile"));   authorRole.setText(rs.getString("authorsRole")); messageType.setText(rs.getString("messageType"));
							wordsText.setText(rs.getString("subject"));		rowCountText.setText(String.valueOf(globalCurrentRowInRecordset) + "/" + String.valueOf(rowCount));		
							activeTSText.setCaretPosition(0);					analyseWordsText.setCaretPosition(0);		markedMessageText.setCaretPosition(0);
							causeMID.setText(mid);	effectMID.setText(mid);	//added for reasons 
						}
					}
				} catch (SQLException insertException) {					
					displaySQLErrors(insertException);	
				}
				catch (Exception ex) {					
					//displaySQLErrors(insertException);	
					ex.printStackTrace();	System.out.println(StackTraceToString(ex)  );	
				}
				//tf.setText("Proposal " + proposalDetailsText.getText());				word = tf.getText().trim();   	updateHighlightFields(searcher);
			}
			});
		
		//see if message is for that particular proposal		
		ifMessageForTheProposal.addActionListener(new ActionListener() {
			public void actionPerformed(ActionEvent e) {
				UpdateAllMessages_EliminateMessagesNotBelongingToCurrentProposal u = new UpdateAllMessages_EliminateMessagesNotBelongingToCurrentProposal();
				boolean allowZero = false;	int option =0; 
				try {
					Statement stmt = null;//					System.out.println();
					stmt = connection.createStatement();
					// results table or postprocessed table										//5											//10
					ResultSet rs = stmt.executeQuery("SELECT pep,date2,subject,datetimestamp, messageid, email from allmessages where messageid = "+ Integer.parseInt(messageIDText.getText()) +" order by datetimestamp asc");  //date asc
					if (rs.next()) {					
						Integer pepNumber =  Integer.parseInt(proposalNumberText.getText()); // rs.getInt("pep"); 
						Date dt = rs.getDate("date2"); 	Timestamp dts = rs.getTimestamp("datetimestamp");	String email = rs.getString("email");
						Integer message_ID = rs.getInt("messageID"); String subject = rs.getString("subject");

						boolean process = u.processMessage(message_ID, email, pepNumber,subject, prp, option, allowZero, email.toLowerCase());
						JOptionPane.showMessageDialog(null, process, "InfoBox: " + "process or Not", JOptionPane.INFORMATION_MESSAGE);

					}	//end while
				} catch (Exception ex) {
					// TODO Auto-generated catch block

				}							
			}
		});
		
		//May 2021...changed to Influence ...method name and variables inside are changed to 'influence'
		//           for reasons, have to create new function 
		//Main SBS and MBS tab
		//dec 2019 ..the sentence level and message level based on sentence level scheme shown in a tab 
		//tab added to the left of the previous left tab on right side of panel
		//extractMsgSentReasonDataForReasonJTable.addActionListener(new ActionListener()		{
		extractMsgSentInfluenceDataForReasonJTable.addActionListener(new ActionListener()		{
			public void actionPerformed(ActionEvent e) {
				try {	System.out.println("\t	Influence ... Main SBS and MBS ");
				if (proposalNumberText.getText().isEmpty()) {
					JOptionPane.showMessageDialog(null, "Error", "InfoBox: " + "Required", JOptionPane.INFORMATION_MESSAGE);
					return;
				}	//combinationCombo  locationCombo 
				//String messageAuthor_str = (String) messageAuthorCombo.getSelectedItem();				    String combination_str   = (String) combinationCombo.getSelectedItem();
				String location_str 	= (String)  locationCombo.getSelectedItem();				    	int proposal = Integer.parseInt(proposalNumberText.getText());
				String state_chosen = "";
				
				//may 2021 original code
				//String sub = String.valueOf(msgSubjectCombo.getSelectedItem());  //wordsText.getText();
				MessageAuthor ms = (MessageAuthor) influencePersonCombo.getSelectedItem();
		        //display.append("\nYou selected the sizecode " + sizecode.label);
				String ashort = ms.authorShort, aLong = ms.author; 
				
				String sqlString = "";
				//ArrayList<Timestamp> dtss = new ArrayList<Timestamp>();
				System.out.println("\t	here proposal: "+proposal+"  aLong: " + aLong);
				model.setRowCount(0);	//empty table contents, in cases where new search is issued
				
				/*
				if (messageAuthor_str.contains("All"))  { System.out.println("\t message author = all: "); messageAuthor_str = "%%"; 	} else {	System.out.println("\t message author = "+messageAuthor_str);	}
				if (location_str.contains("All")) 		{ System.out.println("\t location = all: "); 	  location_str = "%%"; 			} else {	System.out.println("\t location = "+location_str);	}
				if (combination_str.contains("All")){ 
					combination_str = " and termsMatched LIKE '%%' ";	System.out.println("\t combination_str = all: "); //combination_str = "%%";
				}     
				else if (combination_str.contains("Not Nulls")) {	
					combination_str = " and termsMatched IS NOT NULL "; 	System.out.println("\t combination_str = "+combination_str);	
				}
				*/
				//get and set state
				if (bstates_influ_a.isSelected()) { state_chosen /* = "accepted";  }	else if (b4rB2.isSelected()) { state_chosen */ = "final";  } else if (b4rB3.isSelected()) { state_chosen = "rejected";  }
				//Message level based on sentence level
				//Jan 2020, we use subquery to get only the unique messages
				if (bscheme_influ_a.isSelected()) {	//nearbyStates = "All Messages";
					String sentenceOrParagraph = "", mid="", dateVal="", termsMatched = "",level="", folderML="", author="";	Boolean reason= false;
					int arrayCounter=0, recordSetCounter=0, proposalNum = -1;
					System.out.println("\t	here  Messages Level Scheme for proposal "+ proposal);
					//String sql25 = "select pep, messageID, author, date,datetimestamp, label,subject, relation, object, currentSentence, clausie,allReasons 
					//from " + stateDataTableName + " where pep = " + proposal + " order by date;";	//" and " + str + 
					
					String sql25 = " SELECT * FROM " 
							+ " ( "
								+ " SELECT  id, @curRank := @curRank + 1 AS rank, messageTypeIsReasonMessage as RM,messageType as MT, proposal, messageid, dateValue, "
								+ " datediff, sentence, termsMatched, (select analysewords from allmessages w where w.messageid = x.messageid limit 1) as message, "
								//do we really need these
								//+ " (select count(*) from "+reasonCandidatesTable+" s where s.proposal = x.proposal and s.messageID= x.messageID) as sentNum, "
								//+ " (select count(*) from "+reasonCandidatesTable+" s where s.proposal = x.proposal and s.messageID= x.messageID and s.totalprobability > 1.0) as numSentThresholdNum, "
								//+ " (select SUM(s.totalprobability) from "+reasonCandidatesTable+" s where s.proposal = x.proposal and s.messageID= x.messageID) as sumSentProb, "
								+ " ROUND( (sentenceHintProbablity+sentenceLocationHintProbability+restOfParagraphProbabilityScore+messageLocationProbabilityScore+messageSubjectHintProbablityScore " + 
								"	+ dateDiffProbability+authorRoleProbability+messageTypeIsReasonMessageProbabilityScore+sentenceLocationInMessageProbabilityScore+sameMsgSubAsStateTripleProbabilityScore " + 
								"	+ reasonLabelFoundUsingTripleExtractionProbabilityScore+messageContainsSpecialTermProbabilityScore+prevParagraphSpecialTermProbabilityScore-negationTermPenalty), 1) " + //Round to 1 DP
								"   as TotalProbability, "
							//	+ "  "  // already computed and stored probability 
								+ " label, authorRole, location, " 
								+ "	sentenceHintProbablity,sentenceLocationHintProbability,restOfParagraphProbabilityScore,messageTypeIsReasonMessageProbabilityScore,messageLocationProbabilityScore, "  
								+ "	messageSubjectHintProbablityScore,negationTermPenalty,dateDiffProbability,authorRoleProbability,sentenceLocationInMessageProbabilityScore, " 
								+ "	sameMsgSubAsStateTripleProbabilityScore, reasonLabelFoundUsingTripleExtractionProbabilityScore,messageContainsSpecialTermProbabilityScore " 
								// it is likely a number of messages will have same totalprobability beuacse we consider at message level and few fields
								// pep 201 is an example where about all top 40 records have the same total probability of 1.4
								// to get even better results, we can add the following criteria
								// ranking of highest ranked sentence from this message
								// number of sentences from this message in top candidates
								+ " from autoextractedinfluencecandidatesentences x, (SELECT @curRank := 0) r  " // sentence like '%unprecedented%' and "
//								+ " where location = 'sentence' " 
//								+ " AND (label = '"+state_chosen+"') " // OR label like 'Status : "+state_chosen+"')  "	
								+ " WHERE proposal = " + proposal
								//19 may 2021
								+ " AND clusterBySenderFullName = '" + aLong   ///  LIKE '%Nick Coghlan%' " // + member
								// and message like '%pep%' // and messageid = 318242 // and messsageTypeIsReasonMessage =1; 
								+ "' GROUP BY sentence "	// jan 2020 ... this is necessary to remove duplicate sentences
								+ " order by label asc, TotalProbability desc, dateValue desc "
							+ " )  AS messages "  
							+ " GROUP BY messages.messageid "  
							+ " order by label asc, TotalProbability desc, dateValue desc; ";
					
					
					
					// results table or postprocessed table	
					Statement stmt21 = connection.createStatement();	ResultSet rsA = stmt21.executeQuery(sql25);  //date asc
					int rowCount = guih.returnRowCount(sql25);	//System.out.println("\t	found records: "+ rowCount);
					double tprob = 0.0;
					while (rsA.next()) { System.out.println("\t	found records: "); //currDate.before(goToDate)) {
						proposalNum = rsA.getInt("proposal"); 			mid = rsA.getString("messageid");		dateVal = rsA.getString("dateValue");   //rsA.getTimestamp("datetimestamp").toString();	
						sentenceOrParagraph = rsA.getString("message");	
						String clausie = rsA.getString("MT");	String label = rsA.getString("label");		tprob = 	rsA.getDouble("TotalProbability");	author = rsA.getString("authorRole");
						//String subject = rsA.getString("subject");	String relation = rsA.getString("relation");	String object = rsA.getString("object");
						recordSetCounter++;	//termsMatched = subject +" " + relation + " "+object;
						model.addRow(new Object[]{dateVal, mid,/*label,*/author,sentenceOrParagraph,String.valueOf(tprob)});
						
					}
					System.out.println("\t	Message Based scheme returns " + recordSetCounter + " results ");
				}
				//sentence level only
				//Jan 2020 ...we use gropu by to gte only the distinct sentences
				else if (bscheme_influ_b.isSelected())  {	//extract messages near state ..select all states for this proposal and their dates
					String sentenceOrParagraph = "", mid="", dateVal="", termsMatched = "",level="", folderML="", author="";	Boolean reason= false;
					int arrayCounter=0, recordSetCounter=0, proposalNum = -1;
					System.out.println("\t	here  Sentence Level Scheme for proposal "+ proposal);
										
					String sql25 = "SELECT @curRank := @curRank + 1 AS rank, messageTypeIsReasonMessage as RM,messageType as MT, proposal,messageid,dateValue,datediff, sentence,termsMatched, " 
							+ " ROUND( (sentenceHintProbablity+sentenceLocationHintProbability+restOfParagraphProbabilityScore+messageLocationProbabilityScore+messageSubjectHintProbablityScore " 
							+ " +dateDiffProbability+authorRoleProbability+messageTypeIsReasonMessageProbabilityScore+sentenceLocationInMessageProbabilityScore+sameMsgSubAsStateTripleProbabilityScore "  
							+ " +reasonLabelFoundUsingTripleExtractionProbabilityScore+messageContainsSpecialTermProbabilityScore+prevParagraphSpecialTermProbabilityScore-negationTermPenalty), 1) as TotalProbability, " //Round to 1 DP
							+ " label, authorRole, location, " 
							+ " sentenceHintProbablity,sentenceLocationHintProbability,restOfParagraphProbabilityScore,messageTypeIsReasonMessageProbabilityScore,messageLocationProbabilityScore, "  
							+ " messageSubjectHintProbablityScore,negationTermPenalty,dateDiffProbability,authorRoleProbability,sentenceLocationInMessageProbabilityScore, "  
							+ " sameMsgSubAsStateTripleProbabilityScore, reasonLabelFoundUsingTripleExtractionProbabilityScore,messageContainsSpecialTermProbabilityScore "  
							+ " from autoextractedinfluencecandidatesentences, (SELECT @curRank := 0) r "  
//							+ " where location = 'sentence'  and " //-- messageid = 2074922 and "  
//							+ " (label = '"+state_chosen+"' OR label like 'Status : "+state_chosen+"')  and  " //-- sentence like '% seemed generally%' and "  
							+ " WHERE proposal = " + proposal  //-- and messsageTypeIsReasonMessage =1; "  
							//19 may 2021
							+ " AND clusterBySenderFullName = '" + aLong   ///  LIKE '%Nick Coghlan%' " // + member
							+ "' GROUP by sentence "  //group by sentence -- jan 2020 ... this is necesary to remove duplicate sentences  
							+ " ORDER by label asc, TotalProbability desc, dateValue desc;";
														
					// results table or postprocessed table	
					Statement stmt21 = connection.createStatement();	ResultSet rsA = stmt21.executeQuery(sql25);  //date asc
					int rowCount = guih.returnRowCount(sql25);	//System.out.println("\t	found records: "+ rowCount);
					double tprob = 0.0;
					while (rsA.next()) { //System.out.println("\t	found records: "); //currDate.before(goToDate)) {
						proposalNum = rsA.getInt("proposal"); 			mid = rsA.getString("messageid");		dateVal = rsA.getString("dateValue");   //rsA.getTimestamp("datetimestamp").toString();	
						sentenceOrParagraph = rsA.getString("sentence");	
						String clausie = rsA.getString("MT");	String label = rsA.getString("label");		tprob = 	rsA.getDouble("TotalProbability");	author = rsA.getString("authorRole");
						//String subject = rsA.getString("subject");	String relation = rsA.getString("relation");	String object = rsA.getString("object");
						recordSetCounter++;	//termsMatched = subject +" " + relation + " "+object;
						model.addRow(new Object[]{dateVal, mid,/*label,*/author,sentenceOrParagraph,String.valueOf(tprob)});
					}
					System.out.println("\t	Sentence Based scheme returns " + recordSetCounter + " results ");
				}
				
				
				for (int i =0; i<5;i++) {
					stateJTable.setDefaultRenderer(stateJTable.getColumnClass(i), new StateJTableRendererColumn());
				}
				System.out.println(" One turn processing Finished");	
				
				//april 2020
				model.fireTableDataChanged();
				
				} catch (SQLException insertException) {
					displaySQLErrors(insertException);			
				}   
				catch (Exception e1){ 
					System.out.println(" ______here  " + e1.toString() + "\n" );
					System.out.println(StackTraceToString(e1)  );	
					//continue;
				}
			}
		});
		
		//PEP Narrative Tab
		//COMBINATIONS OF REASON SENTENCES WITH SUSTATES Results or Results PostProcessed
		//Table on FIRST Left TAB on the Right Side of GUI which shows all possible candidate reason sentences. 
		//cell action listener allows click and show message based on message id
		extractReasonDataForReasonJTable.addActionListener(new ActionListener()		{
			public void actionPerformed(ActionEvent e) {
				try {	System.out.println("\t	here b12 ");
				if (proposalNumberText.getText().isEmpty()) {
					JOptionPane.showMessageDialog(null, "Error", "InfoBox: " + "Required", JOptionPane.INFORMATION_MESSAGE);
					return;
				}	//combinationCombo  locationCombo 
				String messageAuthor_str = (String) messageAuthorCombo.getSelectedItem();				    String combination_str   = (String) combinationCombo.getSelectedItem();
				String location_str 	= (String)  locationCombo.getSelectedItem();				    	int proposal = Integer.parseInt(proposalNumberText.getText());
				
				String sqlString = "";
				//ArrayList<Timestamp> dtss = new ArrayList<Timestamp>();
				
				model.setRowCount(0);	//empty table contents, in cases where new search is issued
				if (messageAuthor_str.contains("All"))  { System.out.println("\t message author = all: "); messageAuthor_str = "%%"; 	} else {	System.out.println("\t message author = "+messageAuthor_str);	}
				if (location_str.contains("All")) 		{ System.out.println("\t location = all: "); 	  location_str = "%%"; 			} else {	System.out.println("\t location = "+location_str);	}
				if (combination_str.contains("All")){ 
					combination_str = " and termsMatched LIKE '%%' ";	System.out.println("\t combination_str = all: "); //combination_str = "%%";
				}     
				else if (combination_str.contains("Not Nulls")) {	
					combination_str = " and termsMatched IS NOT NULL "; 	System.out.println("\t combination_str = "+combination_str);	
				}
				
				if (rA1.isSelected()) {	//nearbyStates = "All Messages";
					String sentenceOrParagraph = "", mid="", dateVal="", termsMatched = "",level="", folderML="", author="";	Boolean reason= false;
					int arrayCounter=0, recordSetCounter=0, proposalNum = -1;
					System.out.println("\t	here aa 231 All Messages");
					String sql5 = "select pep, messageID, author, date,datetimestamp, label,subject, relation, object, currentSentence, clausie,allReasons from " + stateDataTableName + " where pep = " + proposal + " order by date;";	//" and " + str + 
					// results table or postprocessed table	
					Statement stmt21 = connection.createStatement();	ResultSet rsA = stmt21.executeQuery(sql5);  //date asc
					int rowCount = guih.returnRowCount(sql5);	//System.out.println("\t	found records: "+ rowCount);
					while (rsA.next()) { //System.out.println("\t	found records: "); //currDate.before(goToDate)) {
						try {
							proposalNum = rsA.getInt("pep"); 			mid = rsA.getString("messageid");		dateVal = rsA.getString("date");   rsA.getTimestamp("datetimestamp").toString();	sentenceOrParagraph = rsA.getString("currentSentence");	
							String clausie = rsA.getString("clausie");	String label = rsA.getString("label");		reason = 	rsA.getBoolean("allReasons");	author = rsA.getString("author");
							String subject = rsA.getString("subject");	String relation = rsA.getString("relation");	String object = rsA.getString("object");
							recordSetCounter++;	termsMatched = subject +" " + relation + " "+object;
							model.addRow(new Object[]{dateVal, mid,label,author,sentenceOrParagraph,termsMatched, ""});
						}
						catch (Exception ex) {
							
						}
					}	
				}
				else if (rA2.isSelected())  {	//extract messages near state ..select all states for this proposal and their dates
					String sentenceOrParagraph = "", mid="", dateVal="", termsMatched = "",level="", folderML="", author="";	Boolean reason= false;
					int arrayCounter=0, recordSetCounter=0, proposalNum = -1;
					System.out.println("\t	here aa 232 All Messages");
					String sql5 = "select pep, messageID, author, date,timestamp, label,subject, relation, object, currentSentence, clausie,allReasons from " + stateData_PostProcessedTableName + " where pep = " + proposal + " order by date;";	//" and " + str + 
					// results table or postprocessed table	
					Statement stmt21 = connection.createStatement();	ResultSet rsA = stmt21.executeQuery(sql5);  //date asc
					int rowCount = guih.returnRowCount(sql5);	//System.out.println("\t	found records: "+ rowCount);
					while (rsA.next()) { //System.out.println("\t	found records: "); //currDate.before(goToDate)) {
						proposalNum = rsA.getInt("pep"); 			mid = rsA.getString("messageid");		dateVal = rsA.getString("date");   rsA.getTimestamp("timestamp").toString();	sentenceOrParagraph = rsA.getString("currentSentence");	
						String clausie = rsA.getString("clausie");	String label = rsA.getString("label");		reason = 	rsA.getBoolean("allReasons");	author = rsA.getString("author");
						String subject = rsA.getString("subject");	String relation = rsA.getString("relation");	String object = rsA.getString("object");
						recordSetCounter++;	termsMatched = subject +" " + relation + " "+object;
						model.addRow(new Object[]{dateVal, mid,label,author,sentenceOrParagraph,termsMatched, ""});
					}	
				}
				
				System.out.println("\t	here c ");
				for (int i =0; i<5;i++) {
					stateJTable.setDefaultRenderer(stateJTable.getColumnClass(i), new StateJTableRendererColumn());
				}
				System.out.println(" One turn processing Finished");				
				} catch (SQLException insertException) {
					displaySQLErrors(insertException);			
				}   
				catch (Exception e1){ 
					System.out.println(" ______here  " + e1.toString() + "\n" );
					System.out.println(StackTraceToString(e1)  );	
					//continue;
				}
			}
		});
		
		//Manual reason extraction ...which tab??..not sure
		//FIRST VERSION OF Table on Right (Tab on the far right) which shows all possible candidate sentences. Has option combo boxes.
		//cell action listener allows click and show message based on message id
		extractDataForStateJTable.addActionListener(new ActionListener()		{
			public void actionPerformed(ActionEvent e) {
				try {	System.out.println("\t	here a345 ");
				if (proposalNumberText.getText().isEmpty()) {
					JOptionPane.showMessageDialog(null, "Error", "InfoBox: " + "Required", JOptionPane.INFORMATION_MESSAGE);
					return;
				}	//combinationCombo  locationCombo 
				String messageAuthor_str = (String) messageAuthorCombo.getSelectedItem();				    String combination_str   = (String) combinationCombo.getSelectedItem();
				String location_str 	= (String)  locationCombo.getSelectedItem();				    	int proposal = Integer.parseInt(proposalNumberText.getText());
				
				String sqlString = "";
				//ArrayList<Timestamp> dtss = new ArrayList<Timestamp>();
				
				model.setRowCount(0);	//empty table contents, in cases where new search is issued
				if (messageAuthor_str.contains("All"))  { System.out.println("\t message author = all: "); messageAuthor_str = "%%"; 	} else {	System.out.println("\t message author = "+messageAuthor_str);	}
				if (location_str.contains("All")) 		{ System.out.println("\t location = all: "); 	  location_str = "%%"; 			} else {	System.out.println("\t location = "+location_str);	}
				if (combination_str.contains("All")){ 
					combination_str = " and termsMatched LIKE '%%' ";	System.out.println("\t combination_str = all: "); //combination_str = "%%";
				}     
				else if (combination_str.contains("Not Nulls")) {	
					combination_str = " and termsMatched IS NOT NULL "; 	System.out.println("\t combination_str = "+combination_str);	
				}
				
				//manual reason extraction table
				if (r1.isSelected()) {	//nearbyStates = "All Messages";
						String sentenceOrParagraph = "", mid="", dateVal="", termsMatched = "",level="", folderML="", author="";	Boolean reason= false;
						int arrayCounter=0, recordSetCounter=0, proposalNum = -1;
						System.out.println("\t	here aa 231 All Messages");
						String sql5 = "SELECT proposal,messageid,datetimestamp, sentence,termsMatched, level, reason, author from "+manualreasonextractionTable+" where "
//								+ " author LIKE '"+ messageAuthor_str + "' and level LIKE '" + location_str + "'" + combination_str 
								+ "  proposal = "+ proposal + " order by datetimestamp asc";	//and
						
						// results table or postprocessed table	
						Statement stmt21 = connection.createStatement();	ResultSet rsA = stmt21.executeQuery(sql5);  //date asc
						int rowCount = guih.returnRowCount(sql5);	//System.out.println("\t	found records: "+ rowCount);
						while (rsA.next()) { //System.out.println("\t	found records: "); //currDate.before(goToDate)) {
							proposalNum = rsA.getInt("proposal"); 			mid = rsA.getString("messageid");	dateVal = rsA.getString("datetimestamp");	sentenceOrParagraph = rsA.getString("sentence");	
							termsMatched = rsA.getString("termsMatched");	level = rsA.getString("level");		reason = 	rsA.getBoolean("reason");	author = rsA.getString("author");
							recordSetCounter++;	
							model.addRow(new Object[]{dateVal, mid,author,sentenceOrParagraph,termsMatched, level});
						}	
				}
				//extract messages near state ..select all states for this proposal and their dates
				else {	
					// SELECT * FROM table WHERE date < '2011-09-21 08:21:22' LIMIT x (5,10.15)
					String sql6 = "SELECT pep, email, messageid, datetimestamp from "+ committedStateTableName +" where " 
							+ "  pep = "+ proposal + " order by datetimestamp asc ";
					Statement stmt216 = connection.createStatement();	ResultSet rsA6 = stmt216.executeQuery(sql6);  //date asc	
					System.out.println("\t	here ab 232 Nearby States");
				
					String sentenceOrParagraph = "", mid="", dateVal="", termsMatched = "",level="", folderML="", author="";	Boolean reason= false;
					int arrayCounter=0, recordSetCounter=0, proposalNum = -1; String limitStr = "";
					
					if(r.isSelected()){ limitStr = ""; } else if(ra.isSelected()){ limitStr = " limit 5"; }	else if(rb.isSelected()){ limitStr = " limit 10 "; }	else if(rc.isSelected()){ limitStr = " limit 15 "; }
					System.out.println("\t limitStr: "+ limitStr);
					
					while (rsA6.next()) {
						Timestamp dts = rsA6.getTimestamp("datetimestamp"); System.out.println("\t	datetimestamp: "+ dts); //populate a array
						String sql5 = "SELECT messageid,datetimestamp, sentence,termsMatched, level, reason, author from " + manualreasonextractionTable 
								+ " 	WHERE  messageid IN ("
								+ " 	SELECT * FROM "
								+ "			(SELECT DISTINCT messageid from manualreasonextraction "
								+ "			WHERE (datetimestamp <= '" +dts+ "' and proposal =  "+ proposal + " and author LIKE '"+ messageAuthor_str + "' and level LIKE '" + location_str + "'" + combination_str + ")" 
								+ "			OR    (datetimestamp = '" +dts+ "' AND termsMatched LIKE 'Status:%' )"    //include the state change records ..DRAFT, ACCEPTED, FINAL, etc
								+ " 		order by datetimestamp desc  "+limitStr+" ) AS t "
								+ " ) order by datetimestamp ; ";
						// results table or postprocessed table	
						Statement stmt21 = connection.createStatement();	ResultSet rsA = stmt21.executeQuery(sql5);  //date asc
						int lastMID = 0, newMID=0,counter=0;
						while (rsA.next() ) { //&& counter< 5) { //currDate.before(goToDate)) {
							mid = rsA.getString("messageid");	dateVal = rsA.getString("datetimestamp");	sentenceOrParagraph = rsA.getString("sentence");	
							termsMatched = rsA.getString("termsMatched");	level = rsA.getString("level");		reason = 	rsA.getBoolean("reason");	author = rsA.getString("author");
							recordSetCounter++;	newMID = Integer.parseInt(mid);
							model.addRow(new Object[]{dateVal, mid,author,sentenceOrParagraph,termsMatched, level});
						}	//end while loop for each message
					}
				}
				
				System.out.println("\t	here c ");
				for (int i =0; i<5;i++) {
					stateJTable.setDefaultRenderer(stateJTable.getColumnClass(i), new StateJTableRendererColumn());
				}
				System.out.println(" One turn processing Finished");				
				} catch (SQLException insertException) {
					displaySQLErrors(insertException);			
				}   
				catch (Exception e1){ 
					System.out.println(" ______here  " + e1.toString() + "\n" );
					System.out.println(StackTraceToString(e1)  );	
					//continue;
				}
			}
		});
		
		
		//FIRST TABLE ON LEFT SIDE OF GUI ...FOR COMMITTED States .. ON CLICK SHOW MESSAGE
		//CELL selection...we allow selection of messages based on message id from the extracted state and substate table		
		csJTable.setRowHeight(20);				csJTable.setFont(new Font("Arial", Font.PLAIN, 10));			csJTable.setCellSelectionEnabled(true);	//apr 2018 added for click selection 
		ListSelectionModel cellSelectionModel0 = csJTable.getSelectionModel();			cellSelectionModel0.setSelectionMode(ListSelectionModel.SINGLE_SELECTION);			
		cellSelectionModel0.addListSelectionListener(new ListSelectionListener() {
			public void valueChanged(ListSelectionEvent e) {
				try {	//System.out.println(" hereeeee f");
					if(rs == null) {	JOptionPane.showMessageDialog(null, "Resultset is Null", "InfoBox: " + "Required", JOptionPane.INFORMATION_MESSAGE);	return; }
					rs.beforeFirst(); 
					globalCurrentRowInRecordset = 0;
					while (rs.next()) { 
						globalCurrentRowInRecordset++;
						int mid = Integer.parseInt(rs.getString("messageid"));
						//selected row details
						int i = csJTable.getSelectedRow();	//System.out.println(" i " + i);
						Integer p = Integer.parseInt( csmodel.getValueAt(i, 0).toString() );
//						System.out.println("p " + p + " i " + i);
						if (p==null) {
							JOptionPane.showMessageDialog(null, "Error", "InfoBox: " + "Required", JOptionPane.INFORMATION_MESSAGE);
							return;
						}
						if(mid==p) {
							messageIDText.setText(rs.getString("messageid"));	proposalDetailsText.setText(authorcorrected+ " " +bdfl_delegatecorrected);	dateText.setText(rs.getString("date2"));
							locationText.setText(rs.getString("folder"));	tsText.setText(rs.getString("file"));	
							activeTSText.setText(rs.getString("email"));		analyseWordsText.setText(rs.getString("analysewords"));
							String msg = rs.getString("email"), newMessage=""; newMessage = assignNumbersToSentences_FindMessageEnd(msg, newMessage,rs.getString("author"));	//marked sentence
							markedMessageText.setText(newMessage);			
							wordsText.setText(rs.getString("subject"));    rowCountText.setText(String.valueOf(globalCurrentRowInRecordset) + "/" + String.valueOf(rowCount));								
							activeTSText.setCaretPosition(0);			   analyseWordsText.setCaretPosition(0);		markedMessageText.setCaretPosition(0);

							//wordsearcher highlighter
							final WordSearcher searcher_a0 = new WordSearcher(activeTSText, Color.blue); //was red before
							//highlight the proposal number
							final WordSearcher searcher_a1 = new WordSearcher(activeTSText, Color.red); //was green before
							int offset1 = searcher_a1.search(proposalDetailsText.getText());
							if (offset1 != -1) {
								try {
									activeTSText.scrollRectToVisible(activeTSText.modelToView(offset1));
								} catch (BadLocationException e2) {
								}
							}

							String terms="";//highlight the terms in the stateJTable
							if (csmodel.getValueAt(i, 3)==null) {}
							else { 
								terms = csmodel.getValueAt(i, 3).toString(); 
							}
							//highlighter.getText().trim(); 
							System.out.println("terms"+ terms);
							if(terms.contains(" ")) {	//multiple terms
								String allTerms[] = terms.split(" ");
								for (String t : allTerms) {
									int offset = searcher_a0.search(t);	//for each term
									if (offset != -1) {
										try {
											activeTSText.scrollRectToVisible(activeTSText.modelToView(offset));
										} catch (BadLocationException e2) { }
									}
								}
							}
							else {	//only one term
								int offset = searcher_a0.search(terms);	//for each term
								if (offset != -1) {
									try {
										activeTSText.scrollRectToVisible(activeTSText.modelToView(offset));
									} catch (BadLocationException e3) {}
								}
							}							
							break;
						}			
					}
				} catch (SQLException e1) {
					// TODO Auto-generated catch block
					e1.printStackTrace();
				}
				repaint();
			}
		});
		
		//SECOND TABLE ON LEFT FOR OUR EXTRACTED States and Substates... ON CLICK SHOW MESSAGE
		//CELL selection...we allow selection of messages based on message id from the extracted state and substate table		
		essJTable.setRowHeight(20);				essJTable.setFont(new Font("Arial", Font.PLAIN, 10));			essJTable.setCellSelectionEnabled(true);	//apr 2018 added for click selection 
		ListSelectionModel cellSelectionModel7 = essJTable.getSelectionModel();			cellSelectionModel7.setSelectionMode(ListSelectionModel.SINGLE_SELECTION);			
		cellSelectionModel7.addListSelectionListener(new ListSelectionListener() {
			public void valueChanged(ListSelectionEvent e) {
				try {	//System.out.println(" hereeeee f");
					if(rs == null) {	JOptionPane.showMessageDialog(null, "Resultset is Null", "InfoBox: " + "Required", JOptionPane.INFORMATION_MESSAGE);	return; }
					rs.beforeFirst(); 
					globalCurrentRowInRecordset = 0;
					while (rs.next()) { 
						globalCurrentRowInRecordset++;
						int mid = Integer.parseInt(rs.getString("messageid"));      //get the messageid
						//selected row details
						int i = essJTable.getSelectedRow();	//System.out.println(" i " + i);
						Integer p = Integer.parseInt( essmodel.getValueAt(i, 0).toString() );
//						System.out.println("p " + p + " i " + i);
						if (p==null) {
							JOptionPane.showMessageDialog(null, "Error", "InfoBox: " + "Required", JOptionPane.INFORMATION_MESSAGE);
							return;
						}
						if(mid==p) {
							messageIDText.setText(rs.getString("messageid"));	proposalDetailsText.setText(authorcorrected+ " " +bdfl_delegatecorrected);	dateText.setText(rs.getString("date2"));
							locationText.setText(rs.getString("folder"));		tsText.setText(rs.getString("file"));	
							activeTSText.setText(rs.getString("email"));		analyseWordsText.setText(rs.getString("analysewords"));
							String msg = rs.getString("email"), newMessage=""; 	newMessage = assignNumbersToSentences_FindMessageEnd(msg, newMessage,rs.getString("author"));	//marked sentence
							markedMessageText.setText(newMessage);			
							wordsText.setText(rs.getString("subject"));    		rowCountText.setText(String.valueOf(globalCurrentRowInRecordset) + "/" + String.valueOf(rowCount));								
							activeTSText.setCaretPosition(0);			   		analyseWordsText.setCaretPosition(0);		markedMessageText.setCaretPosition(0);

							//wordsearcher highlighter
//							final WordSearcher searcher_b0 = new WordSearcher(activeTSText, Color.blue);
							//highlight the proposal number
//							final WordSearcher searcher_b1 = new WordSearcher(activeTSText, Color.red);
//							int offset1 = searcher_b1.search(proposalDetailsText.getText());
//							if (offset1 != -1) {
//								try {
//									activeTSText.scrollRectToVisible(activeTSText.modelToView(offset1));
//								} catch (BadLocationException e2) {
//								}
//							}

							String sentence="";//highlight the terms in the stateJTable
							if (essmodel.getValueAt(i, 3)==null) {}
							else { 
								sentence = essmodel.getValueAt(i, 3).toString(); //.toLowerCase(); 
							}
							//debug 
							//sentence ="Guido";
							System.out.println("sentence: "+ sentence);
							
							//May 2020 ... new code
							final WordSearcher searcher_c3 = new WordSearcher(activeTSText, Color.blue);
							
							//may 2020
							String terms = sentence;
							//may 2020
							//toLowerCase().
//$$						terms = terms.replaceAll("\\p{P}", " "); //remove all punctuation ..maybe we can use this here (term matching for candidates) but not in CIE triple extraction as it wmay need commas and other punctuation
							terms = terms.replaceAll("\\r?\\n", " ");
							terms = terms.replaceAll("\\s+", " ").trim(); //remove double spaces and trim
							terms = terms.replaceAll("\\s+", " ").trim(); 
							terms = terms.replaceAll("\\s+", " ").trim(); //remove double spaces and trim
							System.out.println("new sentence " + terms);
														
							System.out.println("ui MID Matched, processed Sentence extracted from JTable: "+ terms);
							
							if(terms.contains(" ")) {	//multiple terms
								System.out.println("HCARE B1: "+ terms);
								String allTerms[] = terms.split(" ");
								//for (String t : allTerms) {
									if(allTerms.length > 1) {	//letter I returns too many underlines
										int offset = searcher_c3.searchAllWordsInParagraph3(allTerms, sentence);	//for all terms, message passed in wordseracher initialiser 
										if (offset != -1) {
											try {	activeTSText.scrollRectToVisible(activeTSText.modelToView(offset));
											} catch (BadLocationException e2) { }
										}
									}
								//}
							}
							
							/* old code
							//highlighter.getText().trim(); 
							System.out.println("terms"+ terms);
							if(terms.contains(" ")) {	//multiple terms
								String allTerms[] = terms.split(" ");
								for (String t : allTerms) {
									int offset = searcher_b0.search(t);	//for each term
									if (offset != -1) {
										try {
											activeTSText.scrollRectToVisible(activeTSText.modelToView(offset));
										} catch (BadLocationException e2) { }
									}
								}
							}
							else {	//only one term
								int offset = searcher_b0.search(terms);	//for each term
								if (offset != -1) {
									try {
										activeTSText.scrollRectToVisible(activeTSText.modelToView(offset));
									} catch (BadLocationException e3) {}
								}
							}							
							*/
							break;
						}			
					}
				} catch (SQLException e1) {
					// TODO Auto-generated catch block
					e1.printStackTrace();
				}
				repaint();
			}
		});
		
		//May 2020 .... JTable (on the right) Cell selection changes the entire recordset, all values change, including the message shown in the middle
		//up and down arrows allow changing of the message
		//april 2020...we have two instances of the same cell actionlister code block for the reasons jtable
		//one is inside the jtp..so that upon change the jtable is updated 
		//one here....
		//but rather than duplicate the code here ...can we just fire the trigger to jtp tab change?
		ListSelectionModel cellSelectionModel6 = stateJTable.getSelectionModel();			cellSelectionModel6.setSelectionMode(ListSelectionModel.SINGLE_SELECTION);			
		cellSelectionModel6.addListSelectionListener(new ListSelectionListener() {
			public void valueChanged(ListSelectionEvent e) {
				//call jtp change listerner
				
				//as soon as cell selection is changed, we go through the entire recordset, starting from the beginning 
				//and go the the specific message being selected in the cell -- could be any message
				try {	//System.out.println(" hereeeee f");
					if(rs == null) {	JOptionPane.showMessageDialog(null, "Resultset is Null", "InfoBox: " + "Required", JOptionPane.INFORMATION_MESSAGE);	return; }
					rs.beforeFirst(); System.out.println(" resultset reset to First a");
					globalCurrentRowInRecordset = 0;
					while (rs.next()) { 
						globalCurrentRowInRecordset++;
						Integer mid = Integer.parseInt(rs.getString("messageid")); //System.out.print(mid+", ");
						//selected row details
						Integer i = stateJTable.getSelectedRow();	//System.out.println(" Selected Row, i = " + i);
						//dec 2019..if i == -1, it gives error so we change it
						//if (i== -1) i = 1; 
						Integer p=null;
						try {
							p = Integer.parseInt( model.getValueAt(i, 1).toString() );
	//   								System.out.println("Going through Recordsets....MessageID From JTable: " + p + " Row " + i);
							if (p==null) {
								JOptionPane.showMessageDialog(null, "Error", "InfoBox: " + "Required", JOptionPane.INFORMATION_MESSAGE);
								return;
							}
						}
						catch(Exception ex) {
							
						}
						if(mid.equals(p)) {	System.out.println();    									
							messageIDText.setText(rs.getString("messageid"));	proposalDetailsText.setText(authorcorrected+ " " +bdfl_delegatecorrected);	dateText.setText(rs.getString("date2"));
							locationText.setText(rs.getString("folder"));	tsText.setText(rs.getString("file"));	
							activeTSText.setText(rs.getString("email"));		analyseWordsText.setText(rs.getString("analysewords"));
							String msg = rs.getString("email"), newMessage=""; newMessage = assignNumbersToSentences_FindMessageEnd(msg, newMessage,rs.getString("author"));	//marked sentence
							markedMessageText.setText(newMessage);			
							wordsText.setText(rs.getString("subject"));    rowCountText.setText(String.valueOf(globalCurrentRowInRecordset) + "/" + String.valueOf(rowCount));								
							activeTSText.setCaretPosition(0);			   analyseWordsText.setCaretPosition(0);		markedMessageText.setCaretPosition(0);
							
							
							// get the currently selected index for this tabbedpane
					        /*
							int selectedIndex = tabbedPane.getSelectedIndex(); 		        System.out.println("Default Index:" + selectedIndex);
					        // select the last tab
					        tabbedPane.setSelectedIndex(tabbedPane.getTabCount()-1);    							 
					        selectedIndex = tabbedPane.getSelectedIndex(); 			        System.out.println("New Index:" + selectedIndex);
							*/
							
							//highlight the proposal number 
							//may 2020 ...do we need this?
							final WordSearcher searcher1 = new WordSearcher(activeTSText, Color.blue);
							int offset1 = searcher1.search(proposalDetailsText.getText());
							if (offset1 != -1) {
								try {
									activeTSText.scrollRectToVisible(activeTSText.modelToView(offset1));
								} catch (BadLocationException e2) {
								}
							}
							//HIGHLIGHT TERMS IN THE MESSAGE, BASED ON TERMS IN RIGHT JTABLE 	
							//wordsearcher highlighter ..highlight terms/sentence
							//dec 2019..we change this so tht instead of terms matched we look for sentence matches
							
							final WordSearcher searcher_c0 = new WordSearcher(activeTSText, Color.blue);
							String originalTerms="",terms="", fullSentence="";//highlight the terms in the stateJTable
							//we want to capture all terms in sentence so we pass column 4, or 5 if we want to show terms matched only
							if (model.getValueAt(i, 3)==null) {}		// terms matched is in cell 6 here, 0-5 = 6  ..original value was 4
							else {  
									//dec 2019
									//originalTerms = model.getValueAt(i, 5).toString().toLowerCase();  //5
									//terms = model.getValueAt(i, 5).toString().toLowerCase();	//5
									//fullSentence = model.getValueAt(i, 6).toString().toLowerCase(); //6
									fullSentence = model.getValueAt(i, 3).toString().toLowerCase(); //6
							}
							//highlighter.getText().trim();
							//dec 2019 change to sentence ..save chaging all code changes just by assigning
							
							System.out.println("\t Sentence extracted from JTable: "+ fullSentence);
							terms = fullSentence;
							terms = terms.replaceAll("\\s+", " ").trim(); //remove double spaces and trim
							System.out.println("\t Sentence processed: "+ terms);
							
							/*
							if(terms.contains(" ")) {	//multiple terms
								//System.out.println("HCARE B: "+ terms);
								String allTerms[] = terms.split(" ");
								//for (String t : allTerms) {
									if(allTerms.length > 1) {	//letter I returns too many underlines
										int offset = searcher_c0.searchAllWordsInParagraph(allTerms);	//for all terms, message passed in wordseracher initialiser 
										if (offset != -1) {
											try {	activeTSText.scrollRectToVisible(activeTSText.modelToView(offset));
											} catch (BadLocationException e2) { }
										}
									}
								//}
							}
							else {	//only one term
								//System.out.println("HCARE C: "+ terms);
								int offset = searcher_c0.search(terms);	//for each term
								if (offset != -1) {
									try {		activeTSText.scrollRectToVisible(activeTSText.modelToView(offset));
									} catch (BadLocationException e3) {}
								}
							}
							*/
							//april 2020
							
							//System.out.println("Proposal: " + proposal + " Message id= "+ mid);
							//Message level based on sentence level
							//Jan 2020, we use subquery to get only the unique messages
							if (b4rA1.isSelected()) 
							{	//nearbyStates = "All Messages";
								//System.out.println("HCARE D0 - Message level selected: "+ terms);
								//get and set state
								String state_chosen="";
								if (b4rB1.isSelected()) { 
									state_chosen = "accepted";  
									//System.out.println("HCARE D0 - Accepted state: "+ terms);
								}	
								else if (b4rB2.isSelected()) { 
									state_chosen = "final";  
								} 
								else if (b4rB3.isSelected()) { 
									state_chosen = "rejected";  
									//System.out.println("HCARE D0 - Rejected state: "+ terms);
								}
								
								// April 2020 ..Get the sentences in the messages so that they can be underlined 
								// query the autoextractedsentences table to get the sentence for the message using the pep, state and messageid
								// SELECT * FROM autoextractedreasoncandidatesentences 
								// WHERE proposal = 308 AND label = 'accepted'
								// AND messageid = 
								try {
									Statement statement2 = connection.createStatement();	ResultSet rs2; 
									String query2;																					//STR_TO_DATE('01-01-2012', '%d-%m-%Y');
									query2 = "SELECT * FROM autoextractedreasoncandidatesentences WHERE proposal = "+ Integer.parseInt(proposalNumberText.getText()) + " AND messageid = " + mid 
											+ " and label = '" + state_chosen +"' ;";
									System.out.println("SELECT * FROM autoextractedreasoncandidatesentences WHERE proposal = "+ Integer.parseInt(proposalNumberText.getText()) + " AND messageid = " + mid 
											+ " and label = '" + state_chosen +"' "); 
									//check to see if we want to see all retrieved messages where status has changed 
									//rs = statement.executeQuery("SELECT * FROM "+messagesTableName+" WHERE "+proposalIdentifier+" + Integer.parseInt(proposalNumberText.getText()) + " AND email LIKE '%" + searchText.getText()  + "%' AND statusChanged =1 order by dateTimeStamp, messageid asc, Proposal asc;");
									rs2 = statement2.executeQuery(query2);
									rowCount = guih.returnRowCount(" SELECT * FROM autoextractedreasoncandidatesentences WHERE proposal ="+ Integer.parseInt(proposalNumberText.getText()) + " AND messageid = " + mid 
											+ " and label = '" + state_chosen +"' ;" );

									System.out.println("resultset rowCount = " + rowCount);  
									while (rs2.next()) {
										globalCurrentRowInRecordset++;
										String sentence = rs2.getString("sentence");
										//System.out.println("Proposal: " + Integer.parseInt(proposalNumberText.getText())  + " Message id= "+ mid + " Label "+ state_chosen + "  sentence (" + sentence + ")");  
										
										terms = sentence;
    									terms = terms.replaceAll("\\s+", " ").trim(); //remove double spaces and trim
    									System.out.println("Reasons MID Matched, Sentence extracted from JTable: "+ terms);
    									
    									if(terms.contains(" ")) {	//multiple terms
    										System.out.println("HCARE B2: "+ terms);
    										String allTerms[] = terms.split(" ");
    										//for (String t : allTerms) {
    											if(allTerms.length > 1) {	//letter I returns too many underlines
    												
    												int offset = 0; 
    												Integer pNumber= Integer.parseInt(proposalNumberText.getText());  
    												
    												if(pNumber.equals(572) && p.equals(8184365)) {
    													System.out.println("P2: "+ pNumber);
    													offset = searcher_c0.searchAllWordsInParagraph3(allTerms, sentence);	//for all terms, message passed in wordseracher initialiser 
    												}
    												else 
    													offset = searcher_c0.searchAllWordsInParagraph2(allTerms, sentence);	//for all terms, message passed in wordseracher initialiser 
    												
    												if (offset != -1) {
    													try {	activeTSText.scrollRectToVisible(activeTSText.modelToView(offset));
    													} catch (BadLocationException e2) { }
    												}
    											}
    										//}
    									}
    									// april 2020..just comment as see
//    									else {	//only one term
//    										//System.out.println("HCARE C: "+ terms);
//    										int offset = searcher_c0.search(terms);	//for each term
//    										if (offset != -1) {
//    											try {		activeTSText.scrollRectToVisible(activeTSText.modelToView(offset));
//    											} catch (BadLocationException e3) {}
//    										}
//    									}
										

//										highlightSentence(originalTerms, terms);
									}
								} 
								catch (SQLException insertException) {					displaySQLErrors(insertException);	}
								
								
								
							}
							//sentence level only
							//Jan 2020 ...we use Group by to get only the distinct sentences
							else if (b4rA2.isSelected())  {	//extract messages near state ..select all states for this proposal and their dates
								System.out.println("HCARE D0 - Sentence level selected: "+ terms);
								terms = fullSentence;
								terms = terms.replaceAll("\\s+", " ").trim(); //remove double spaces and trim
								System.out.println("ui MID Matched, processed Sentence extracted from JTable: "+ terms);
								
								if(terms.contains(" ")) {	//multiple terms
									System.out.println("HCARE B3: "+ terms);
									String allTerms[] = terms.split(" ");
									//for (String t : allTerms) {
										if(allTerms.length > 1) {	//letter I returns too many underlines
											int offset = 0; 
											Integer pNumber= Integer.parseInt(proposalNumberText.getText());  
											System.out.println("P: "+ pNumber);
											if(pNumber.equals(572)) {
												offset = searcher_c0.searchAllWordsInParagraph4(allTerms, fullSentence);	//for all terms, message passed in wordseracher initialiser 
											}
											else 
												offset = searcher_c0.searchAllWordsInParagraph2(allTerms, fullSentence);	//for all terms, message passed in wordseracher initialiser
											if (offset != -1) {
												try {	activeTSText.scrollRectToVisible(activeTSText.modelToView(offset));
												} catch (BadLocationException e2) { }
											}
										}
									//}
								}
							}	
								
							//oct 2018 add new way of blue highlighting
							//utilities.val vb = new utilities.val(); 
//   									highlightSentence(originalTerms, terms);
							
							
							
							break;
						}			
					}
				} catch (SQLException e1) {
					// TODO Auto-generated catch block
					e1.printStackTrace();
				}
				repaint();
				
			}
		});
		
		//Tabbed Pane on right side of frame, should update same jtable 3 different ways when selected
		//if first tab is selected, update base on automatic reason candate extraction results
		//if tab 2 is selcted, show all manually extratected sentences
		//if tab 3, then show manual extraction first attempt
		//oct 2018, if tab 4, we show statea and substates results
		//if tab 5, we show statea and substates results
		jtp2.addChangeListener(new ChangeListener() {
	        public void stateChanged(ChangeEvent e) {
	        	
	        	
	        	int index = jtp2.getSelectedIndex();		            System.out.println("Selected Tab: " + index);
	            if(index==0) {	//FOR FIRST TAB
	            	String pep = proposalNumberText.getText();	            	
	            	//Right Side Panel...show jtable for first tabbed pane, show candidate sentences based on automatic approach.
    			    try {	//System.out.println("\t	here abc 1234 ");
    				if (proposalNumberText.getText().isEmpty()) {
    					JOptionPane.showMessageDialog(null, "Error: No Proposal Number Entered", "InfoBox: " + "Required", JOptionPane.INFORMATION_MESSAGE);
    					return;
    				}	//combinationCombo  locationCombo 
    				String messageAuthor_str = (String) messageAuthorCombo.getSelectedItem();				    String combination_str   = (String) combinationCombo.getSelectedItem();
    				String location_str 	= (String)  locationCombo.getSelectedItem();				    	int proposal = Integer.parseInt(proposalNumberText.getText());
    				
    				String sqlString = "";
    				//ArrayList<Timestamp> dtss = new ArrayList<Timestamp>();
    				
    				model.setRowCount(0);	//empty table contents, in cases where new search is issued
    				if (messageAuthor_str.contains("All"))  { System.out.println("\t message author = all: "); messageAuthor_str = "%%"; 	} else {	System.out.println("\t message author = "+messageAuthor_str);	}
    				if (location_str.contains("All")) 		{ System.out.println("\t location = all: "); 	  location_str = "%%"; 			} else {	System.out.println("\t location = "+location_str);	}
    				if (combination_str.contains("All")){ 
    					combination_str = " and termsMatched LIKE '%%' ";	System.out.println("\t combination_str = all: "); //combination_str = "%%";
    				}     
    				else if (combination_str.contains("Not Nulls")) {	
    					combination_str = " and termsMatched IS NOT NULL "; 	System.out.println("\t combination_str = "+combination_str);	
    				}
    				
    				if (r1.isSelected()) {	//nearbyStates = "All Messages";
    						String sentenceOrParagraph = "", mid="", dateVal="", termsMatched = "",level="", folderML="", author="";	Boolean reason= false;
    						int arrayCounter=0, recordSetCounter=0, proposalNum = -1;
    						System.out.println("\t	here aa 231 All Messages, proposal : "+ proposal + " reasonCandidatesTable: " + reasonCandidatesTable);
    						String sql5 = "SELECT proposal,messageid,dateValue, sentence,termsMatched, probability, label, authorRole, location, "
    								+ " (sentenceHintProbablity+sentenceLocationHintProbability+restOfParagraphProbabilityScore+messageLocationProbabilityScore+messageSubjectHintProbablityScore+"
    								+ "  dateDiffProbability+authorRoleProbability+messsageTypeIsReasonMessageProbabilityScore-negationTermPenalty) as TotalProbability " //nov 2018 added 'messsageTypeIsReasonMessageProbabilityScore'
    								+ " from " + reasonCandidatesTable + " where "
//	        								+ " author LIKE '"+ messageAuthor_str + "' and level LIKE '" + location_str + "'" + combination_str 
    //$$								+ "  probability like '%high%' and location LIKE '%sentence%' and "
    //$$								+ "  proposal = "+ proposal + " order by label asc, FIELD(probability, 'very high','high','some','low'), dateValue desc "; //datetimestamp asc";	//and
    									+ "	 location = 'sentence' and "
    									+ "  proposal = "+ proposal + " order by label asc, TotalProbability desc, dateValue desc ";
    								
    						// results table or postprocessed table	
    						Statement stmt21 = connection.createStatement();	ResultSet rsA = stmt21.executeQuery(sql5);  //date asc
    						int rowCount = guih.returnRowCount(sql5);	//System.out.println("\t	found records: "+ rowCount);
    						while (rsA.next()) {  //currDate.before(goToDate)) { System.out.println("\t	found records: ");
    							proposalNum = rsA.getInt("proposal"); 			mid = rsA.getString("messageid");	dateVal = rsA.getString("dateValue");	sentenceOrParagraph = rsA.getString("sentence");	
    							termsMatched = rsA.getString("termsMatched");	String probability = rsA.getString("TotalProbability");		String label = 	rsA.getString("label");	author = rsA.getString("authorRole");
    							String loc =rsA.getString("location"); 
    							recordSetCounter++;	
    							model.addRow(new Object[]{dateVal, mid,label,author,sentenceOrParagraph,termsMatched, probability});
    						}	
    				}
    				else {	//extract messages near state ..select all states for this proposal and their dates
    					// SELECT * FROM table WHERE date < '2011-09-21 08:21:22' LIMIT x (5,10.15)
    					String sql6 = "SELECT pep, email, messageid, datetimestamp from pepstates_danieldata_datetimestamp where " 
    							+ "  pep = "+ proposal + " order by datetimestamp asc ";
    					Statement stmt216 = connection.createStatement();	ResultSet rsA6 = stmt216.executeQuery(sql6);  //date asc	
    					System.out.println("\t	here ab 232 Nearby States");
    				
    					String sentenceOrParagraph = "", mid="", dateVal="", termsMatched = "",level="", folderML="", author="";	Boolean reason= false;
    					int arrayCounter=0, recordSetCounter=0, proposalNum = -1; String limitStr = "";
    					
    					if(r.isSelected()){ limitStr = ""; } else if(ra.isSelected()){ limitStr = " limit 5"; }	else if(rb.isSelected()){ limitStr = " limit 10 "; }	else if(rc.isSelected()){ limitStr = " limit 15 "; }
    					System.out.println("\t limitStr: "+ limitStr);
    					
    					while (rsA6.next()) {
    						Timestamp dts = rsA6.getTimestamp("datetimestamp"); System.out.println("\t	datetimestamp: "+ dts); //populate a array
    						String sql5 = "SELECT messageid,datetimestamp, sentence,termsMatched, level, reason, author from manualreasonextraction "
    								+ " 	WHERE  messageid IN ("
    								+ " 	SELECT * FROM "
    								+ "			(SELECT DISTINCT messageid from manualreasonextraction "
    								+ "			WHERE (datetimestamp <= '" +dts+ "' and proposal =  "+ proposal + " and author LIKE '"+ messageAuthor_str + "' and level LIKE '" + location_str + "'" + combination_str + ")" 
    								+ "			OR    (datetimestamp = '" +dts+ "' AND termsMatched LIKE 'Status:%' )"    //include the state change records ..DRAFT, ACCEPTED, FINAL, etc
    								+ " 		order by datetimestamp desc  "+limitStr+" ) AS t "
    								+ " ) order by datetimestamp ; ";
    						// results table or postprocessed table	
    						Statement stmt21 = connection.createStatement();	ResultSet rsA = stmt21.executeQuery(sql5);  //date asc
    						int lastMID = 0, newMID=0,counter=0;
    						while (rsA.next() ) { //&& counter< 5) { //currDate.before(goToDate)) {
    							mid = rsA.getString("messageid");	dateVal = rsA.getString("datetimestamp");	sentenceOrParagraph = rsA.getString("sentence");	
    							termsMatched = rsA.getString("termsMatched");	level = rsA.getString("level");		reason = 	rsA.getBoolean("reason");	author = rsA.getString("author");
    							recordSetCounter++;	newMID = Integer.parseInt(mid);
    							model.addRow(new Object[]{dateVal, mid,"",author,sentenceOrParagraph,termsMatched, level});
    						}	//end while loop for each message
    					}
    				}
    				
//    				System.out.println("\t	here c ");
    				for (int i =0; i<5;i++) { //show all committed states
    					stateJTable.setDefaultRenderer(stateJTable.getColumnClass(i), new StateJTableRendererColumn());
    				}
    				
    				} catch (SQLException insertException) {
    					displaySQLErrors(insertException);			
    				}   
    				catch (Exception e1){ 
    					System.out.println(" ______here  " + e1.toString() + "\n" );
    					System.out.println(StackTraceToString(e1)  );	
    					//continue;
    				}
    			    
	            }
	            else if(index==1) {	//Right Side Panel...show jtable for second tabbed pane, show manually extracted sentences
	            	String pep = proposalNumberText.getText();
	            	if(pep==null || pep.isEmpty() || pep.equals("")) {}
	            	       			
    				try {	System.out.println("\t	Manually Extracted Sentences ");
    				if (proposalNumberText.getText().isEmpty()) {
    					JOptionPane.showMessageDialog(null, "Error: No Proposal Number Entered", "InfoBox: " + "Required", JOptionPane.INFORMATION_MESSAGE);
    					return;
    				}	//combinationCombo  locationCombo 
    			
    				int proposal = Integer.parseInt(proposalNumberText.getText());				
    				String sqlString = "";			
    				model.setRowCount(0);	//empty table contents, in cases where new search is issued				
    				System.out.println("\t	proposal: "+proposal);
    				if(proposal<0) {}
    				else {	//nearbyStates = "All Messages";
    						String causeSentence = "", mid="", dateVal="", termsMatched = "",level="", folderML="", author="",probability="";	Boolean reason= false;
    						int arrayCounter=0, recordSetCounter=0, proposalNum = -1;
    						System.out.println("\t	here aa 231 All Messages, proposal : "+ proposal + " reasonCandidatesTable: " + reasonCandidatesTable);
    						String sql5 = "SELECT pep, messageid, label, causesentence from "+manuallyReasonExtractedTable+" where pep = "+ proposal + ";";
    								
    						// results table or postprocessed table	
    						Statement stmt21 = connection.createStatement();	ResultSet rsA = stmt21.executeQuery(sql5);  //date asc
    						int rowCount = guih.returnRowCount(sql5);	//System.out.println("\t	found records: "+ rowCount);
//    						System.out.println("\t	ee ");
    						while (rsA.next()) { //System.out.println("\t	found records: "); //currDate.before(goToDate)) {
    							proposalNum = rsA.getInt("pep"); 			mid = rsA.getString("messageid");	String label = 	rsA.getString("label");	causeSentence = rsA.getString("causesentence");
    							recordSetCounter++;	
//  							System.out.println("\t	ffff ");
    							model.addRow(new Object[]{dateVal, mid,label,author,causeSentence,termsMatched, probability});
    						}	
    				}
    				for (int i =0; i<5;i++) { //show all committed states
    					stateJTable.setDefaultRenderer(stateJTable.getColumnClass(i), new StateJTableRendererColumn());
    				}
    				//System.out.println(" One turn processing Finished");
    				
    				//actionlistener for the JTable on the right
    				stateJTable.setRowHeight(20);				stateJTable.setFont(new Font("Arial", Font.PLAIN, 10));			stateJTable.setCellSelectionEnabled(true);	//apr 2018 added for click selection 
    				ListSelectionModel cellSelectionModel6 = stateJTable.getSelectionModel();			cellSelectionModel6.setSelectionMode(ListSelectionModel.SINGLE_SELECTION);			
    				cellSelectionModel6.addListSelectionListener(new ListSelectionListener() {
    					public void valueChanged(ListSelectionEvent e) {
    						try {	//System.out.println(" hereeeee f");
    							if(rs == null) {	JOptionPane.showMessageDialog(null, "Resultset is Null", "InfoBox: " + "Required", JOptionPane.INFORMATION_MESSAGE);	return; }
    							rs.beforeFirst(); System.out.println(" resultset reset to First b");
    							globalCurrentRowInRecordset = 0;
    							while (rs.next()) { 
    								globalCurrentRowInRecordset++;
    								Integer mid = Integer.parseInt(rs.getString("messageid")); //System.out.print(mid+", ");
    								//selected row details
    								Integer i = stateJTable.getSelectedRow();	//System.out.println(" Selected Row, i = " + i);
    								//dec 2019..if i == -1, it gives error so we change it
    								//if (i== -1) i = 1;    								
    								Integer p = Integer.parseInt( model.getValueAt(i, 1).toString() );
 //   								System.out.println("Going through Recordsets....MessageID From JTable: " + p + " Row " + i);
    								if (p==null) {
    									JOptionPane.showMessageDialog(null, "Error", "InfoBox: " + "Required", JOptionPane.INFORMATION_MESSAGE);
    									return;
    								}
    								if(mid.equals(p)) {	System.out.println();    									
    									messageIDText.setText(rs.getString("messageid"));	proposalDetailsText.setText(authorcorrected+ " " +bdfl_delegatecorrected);	dateText.setText(rs.getString("date2"));
    									locationText.setText(rs.getString("folder"));	tsText.setText(rs.getString("file"));	
    									activeTSText.setText(rs.getString("email"));		analyseWordsText.setText(rs.getString("analysewords"));
    									String msg = rs.getString("email"), newMessage=""; newMessage = assignNumbersToSentences_FindMessageEnd(msg, newMessage,rs.getString("author"));	//marked sentence
    									markedMessageText.setText(newMessage);			
    									wordsText.setText(rs.getString("subject"));    rowCountText.setText(String.valueOf(globalCurrentRowInRecordset) + "/" + String.valueOf(rowCount));								
    									activeTSText.setCaretPosition(0);			   analyseWordsText.setCaretPosition(0);		markedMessageText.setCaretPosition(0);
    									
    									
    									// get the currently selected index for this tabbedpane
    							        /*
    									int selectedIndex = tabbedPane.getSelectedIndex(); 		        System.out.println("Default Index:" + selectedIndex);
    							        // select the last tab
    							        tabbedPane.setSelectedIndex(tabbedPane.getTabCount()-1);    							 
    							        selectedIndex = tabbedPane.getSelectedIndex(); 			        System.out.println("New Index:" + selectedIndex);
    									*/
    									
    									//highlight the proposal number
    									final WordSearcher searcher1 = new WordSearcher(activeTSText, Color.blue);
    									int offset1 = searcher1.search(proposalDetailsText.getText());
    									if (offset1 != -1) {
    										try {
    											activeTSText.scrollRectToVisible(activeTSText.modelToView(offset1));
    										} catch (BadLocationException e2) {
    										}
    									}
    									//HIGHLIGHT TERMS IN THE MESSAGE, BASED ON TERMS IN RIGHT JTABLE 	
    									//wordsearcher highlighter ..highlight terms/sentence
    									//dec 2019..we change this so tht instead of terms matched we look for sentence matches
    									
    									final WordSearcher searcher_c0 = new WordSearcher(activeTSText, Color.blue);
    									String originalTerms="",terms="", fullSentence="";//highlight the terms in the stateJTable
    									//we want to capture all terms in sentence so we pass column 4, or 5 if we want to show terms matched only
    									if (model.getValueAt(i, 3)==null) {}		// terms matched is in cell 6 here, 0-5 = 6  ..original value was 4
    									else {  
    											//dec 2019
    											//originalTerms = model.getValueAt(i, 5).toString().toLowerCase();  //5
    											//terms = model.getValueAt(i, 5).toString().toLowerCase();	//5
    											//fullSentence = model.getValueAt(i, 6).toString().toLowerCase(); //6
    											fullSentence = model.getValueAt(i, 3).toString().toLowerCase(); //6
    									}
    									//highlighter.getText().trim();
    									//dec 2019 change to sentence ..save chaging all code changes just by assigning
    									
    									
    									terms = fullSentence;
    									terms = terms.replaceAll("\\s+", " ").trim(); //remove double spaces and trim
    									System.out.println("Second MID Matched, Sentence extracted from JTable: "+ terms);
    									
    									if(terms.contains(" ")) {	//multiple terms
    										//System.out.println("HCARE B: "+ terms);
    										String allTerms[] = terms.split(" ");
    										//for (String t : allTerms) {
    											if(allTerms.length > 1) {	//letter I returns too many underlines
    												int offset = searcher_c0.searchAllWordsInParagraph(allTerms);	//for all terms, message passed in wordseracher initialiser 
    												if (offset != -1) {
    													try {	activeTSText.scrollRectToVisible(activeTSText.modelToView(offset));
    													} catch (BadLocationException e2) { }
    												}
    											}
    										//}
    									}
    									else {	//only one term
    										//System.out.println("HCARE C: "+ terms);
    										int offset = searcher_c0.search(terms);	//for each term
    										if (offset != -1) {
    											try {		activeTSText.scrollRectToVisible(activeTSText.modelToView(offset));
    											} catch (BadLocationException e3) {}
    										}
    									}
    									
    									//april 2020
    									
    									//System.out.println("Proposal: " + proposal + " Message id= "+ mid);
    									//Message level based on sentence level
    									//Jan 2020, we use subquery to get only the unique messages
    									if (b4rA1.isSelected()) {	//nearbyStates = "All Messages";
    										//System.out.println("HCARE D0 - Message level selected: "+ terms);
    										//get and set state
    										String state_chosen="";
        									if (b4rB1.isSelected()) { 
        										state_chosen = "accepted";  
        										//System.out.println("HCARE D0 - Accepted state: "+ terms);
        									}	
        									else if (b4rB2.isSelected()) { 
        										state_chosen = "final";  
        									} 
        									else if (b4rB3.isSelected()) { 
        										state_chosen = "rejected";  
        										//System.out.println("HCARE D0 - Rejected state: "+ terms);
        									}
        									
        									//query the autoextractedsentences table to gte the sentence for the message using the pep, state and messageid
        									// SELECT * FROM autoextractedreasoncandidatesentences 
        									// WHERE proposal = 308 AND label = 'accepted'
        									// AND messageid = 
        									try {
        										Statement statement2 = connection.createStatement();	ResultSet rs2; 
	        									String query2;																					//STR_TO_DATE('01-01-2012', '%d-%m-%Y');
	        									query2 = "SELECT * FROM autoextractedreasoncandidatesentences WHERE proposal ="+ Integer.parseInt(proposalNumberText.getText()) + " AND messageid = " + mid 
	        											+ " and label = '" + state_chosen +"' ;";
	        									System.out.println("SELECT * FROM autoextractedreasoncandidatesentences WHERE proposal ="+ Integer.parseInt(proposalNumberText.getText()) + " AND messageid = " + mid 
	        											+ " and label = '" + state_chosen +"' "); 
	        									//check to see if we want to see all retrieved messages where status has changed 
	        									//rs = statement.executeQuery("SELECT * FROM "+messagesTableName+" WHERE "+proposalIdentifier+" + Integer.parseInt(proposalNumberText.getText()) + " AND email LIKE '%" + searchText.getText()  + "%' AND statusChanged =1 order by dateTimeStamp, messageid asc, Proposal asc;");
	        									rs2 = statement2.executeQuery(query2);
	        									rowCount = guih.returnRowCount(" SELECT * FROM autoextractedreasoncandidatesentences WHERE proposal ="+ Integer.parseInt(proposalNumberText.getText()) + " AND messageid = " + mid 
	        											+ " and label = '" + state_chosen +"' ;" );
	
	        									System.out.println("resultset rowCount = " + rowCount);  
	        									while (rs2.next()) {
	        										globalCurrentRowInRecordset++;
	        										String sentence = rs2.getString("sentence");
	        										System.out.println("Proposal: " + proposal + " Message id= "+ mid + " Label "+ state_chosen + "  sentence (" + sentence + ")");  
	        										
	        										terms = sentence;
	            									terms = terms.replaceAll("\\s+", " ").trim(); //remove double spaces and trim
	            									System.out.println("Third MID Matched, Sentence extracted from JTable: "+ terms);
	            									
	            									if(terms.contains(" ")) {	//multiple terms
	            										//System.out.println("HCARE B: "+ terms);
	            										String allTerms[] = terms.split(" ");
	            										//for (String t : allTerms) {
	            											if(allTerms.length > 1) {	//letter I returns too many underlines
	            												int offset = searcher_c0.searchAllWordsInParagraph2(allTerms, sentence);	//for all terms, message passed in wordseracher initialiser 
	            												if (offset != -1) {
	            													try {	activeTSText.scrollRectToVisible(activeTSText.modelToView(offset));
	            													} catch (BadLocationException e2) { }
	            												}
	            											}
	            										//}
	            									}
	            									// april 2020..just comment as see
//	            									else {	//only one term
//	            										//System.out.println("HCARE C: "+ terms);
//	            										int offset = searcher_c0.search(terms);	//for each term
//	            										if (offset != -1) {
//	            											try {		activeTSText.scrollRectToVisible(activeTSText.modelToView(offset));
//	            											} catch (BadLocationException e3) {}
//	            										}
//	            									}
	        										
 
//	        										highlightSentence(originalTerms, terms);
	        									}
        									} 
        									catch (SQLException insertException) {					displaySQLErrors(insertException);	}
        									
        									
        									
    									}
    									//sentence level only
    									//Jan 2020 ...we use gropu by to gte only the distinct sentences
    									else if (b4rA2.isSelected())  {	//extract messages near state ..select all states for this proposal and their dates
    										//System.out.println("HCARE D0 - Sentence level selected: "+ terms);
    									}	
    										
    									//oct 2018 add new way of blue highlighting
    									//utilities.val vb = new utilities.val(); 
 //   									highlightSentence(originalTerms, terms);
    									
    									
    									
    									break;
    								}			
    							}
    						} catch (SQLException e1) {
    							// TODO Auto-generated catch block
    							e1.printStackTrace();
    						}
    						repaint();
    					}

						private void highlightSentence(String sentence, String terms) {
							try {
								//System.out.println("HCARE D: "+ terms);
								//we get the first terms from the sentence we want to match
								String splittedSentence[] = sentence.split(" ");
								String punctuations[] = {"!","#","$","%","&","'","(",")","*","+",",","-",".","/",":",";","<","=",">","?","@","\"","\\",",","^","_","`","{","|","}","~"}; 
								
								//String r = searcher_c0.returnSentenceWithTerms(terms.replace(",", " ")); //findField.getText().toLowerCase();
								String doc = activeTSText.getText();
								InputStream inputStream = new FileInputStream("C://lib//openNLP//en-sent.bin"); 
							    SentenceModel model = new SentenceModel(inputStream); 	    									
								SentenceDetectorME detector = new SentenceDetectorME(model);  //Instantiating the SentenceDetectorME class 
								    
							    String[] paragraphs = doc.split("\\n\\n"); //"\\r?\\n\\r?\\n");		
								//String[] paragraphs = v_message.split("\\r?\\n\\r?\\n"); //mar 2018...new improved way implemented   "\\n\\n");
								
								int pos = 0;	    									
								activeTSText.requestFocusInWindow();	// Focus the text area, otherwise the highlighting won't show up	
								terms = terms.replace(",", " ");
								
								int cumulativeCount=0;
								
								for (String para: paragraphs){ System.out.println("new para: "+para);	
									cumulativeCount+= para.length();
									//sometimes a paragrapgh has sentences but are not captured as sentences. So if sentence identified then check, so we just add a full-stop at end of paragraph	        				   
									if(!para.endsWith("."))
										para = para + ".";
									 
									
								    Span[] spans = detector.sentPosDetect(para);   //Detecting the position of the sentences in the paragraph  
								     
								    String sen = para;
								    
								    //Printing the sentences and their spans of a paragraph 
								    for (Span span : spans) {        
								    	String extracted = sen.substring(span.getStart(), span.getEnd()); //get each sentence
								        System.out.println("\t\t\t Span: " + extracted +" : ");//+ span.toString());  
								       
								        boolean found = false; //true
								        /* for (String t : terms.split(" ")) {
								        	if(!extracted.contains(t))	{
								        		found =false;
								        	}
								        } */	
								        //nov 2018 ..new way of doing, in each sentence, we just look for terms from the sentence column, if three terms from the sentence exist, we highlight
								        // if any of these terms contain any punctuation, we try matcig the next term in the sentence
								        int mark=3, addedCounter = 0;
								        //for (int g=0; g< splitted.length; g++) {
								        	if(extracted==null || extracted.isEmpty())
								        	{}
								        	else {
//								        		if(splittedSentence[0]==null || splittedSentence[0].isEmpty() || splittedSentence[1]==null || splittedSentence[1].isEmpty() || splittedSentence[2]==null || splittedSentence[2].isEmpty()) 
//								        		{}
//								        		else {
//										        	if(extracted.contains(splittedSentence[0]) &&  extracted.contains(splittedSentence[1]) &&  extracted.contains(splittedSentence[2]) ){
//										        		found = true;
//										        	}
//								        		}
								        		
								        		//we find if extracted sentence has all sentence terms of reason sentence
								        		Integer numTerms = splittedSentence.length - 1;
								        		Integer counterOfFound=0;
								        		for(String s : splittedSentence) {
								        			if(s==null || s.equals("") || s.isEmpty()) 
								        			{}
								        			else {
								        				if(extracted.contains(s)){
								        					counterOfFound++;
								        				} 
								        			}
								        			if(counterOfFound == numTerms - 2) //we allow for 2 terms less...
								        				found = true;
								        		}
								        		
								        		
								        	}
								        	addedCounter++;
								        //}
								        if(found) {	 	    								        	 	    								        
								        	System.out.println("Found sentence with all terms ff: "+ extracted +" "+ span);
								        	//int finalStart = span.getStart() + cumulativeCount;
								        	//int finalEnd = span.getEnd() + cumulativeCount;
								        	int index = activeTSText.getText().indexOf(extracted);
								        	
								        	Rectangle viewRect;
											
											viewRect = activeTSText.modelToView(index); //vb.getstart());
											// Scroll to make the rectangle visible
											activeTSText.scrollRectToVisible(viewRect);
											// Highlight the text
											activeTSText.setCaretPosition(index+ extracted.length()); //vb.getstart() + vb.getend());
											activeTSText.moveCaretPosition(index); //vb.getend());	   
											break;
								        	
								        }	 	    								        
								    }
								}								
							}
   //									
							catch (Exception e1) {
								// TODO Auto-generated catch block
								e1.printStackTrace();
							}
						}
    				});
    				} catch (SQLException insertException) {
    					displaySQLErrors(insertException);			
    				}   
    				catch (Exception e1){ 
    					System.out.println(" ______here 342 " + e1.toString() + "\n" );
    					System.out.println(StackTraceToString(e1)  );	
    					//continue;
    				}	        			
	            }
	            else if (index==2) {	//show first attempt at automating teh manual sentence extraction
	            	System.out.println("Selected tab 3" );
	            }
	            else if (index==3) {	//show first attempt at automating teh manual sentence extraction
	            	System.out.println("Selected tab 4" );
	            }
	            else if (index==4) {	//show first attempt at automating teh manual sentence extraction
	            	System.out.println("Selected tab 5" );
	            }
	        }
		});	//End tabbed pane selection


		//search with dates option
		//Do freeQueryButton
		searchWithDatesQueryButton.addActionListener(new ActionListener() {
			public void actionPerformed(ActionEvent e) {
				rowCount = 0;	globalCurrentRowInRecordset=0;
				try {
					String query;																					//STR_TO_DATE('01-01-2012', '%d-%m-%Y');
					query = "SELECT * FROM "+messagesTableName+" WHERE "+proposalIdentifier+ "="+ Integer.parseInt(proposalNumberText.getText()) + " AND date2  BETWEEN STR_TO_DATE('" + DateFromText.getText() + "', '%d-%m-%Y') AND STR_TO_DATE('" + DateToText.getText() + "', '%d-%m-%Y') order by dateTimeStamp;";
					//check to see if we want to see all retrieved messages where status has changed 
					//rs = statement.executeQuery("SELECT * FROM "+messagesTableName+" WHERE "+proposalIdentifier+" + Integer.parseInt(proposalNumberText.getText()) + " AND email LIKE '%" + searchText.getText()  + "%' AND statusChanged =1 order by dateTimeStamp, messageid asc, Proposal asc;");
					rs = statement.executeQuery(query);
					rowCount = guih.returnRowCount("SELECT * FROM "+messagesTableName+" WHERE "+proposalIdentifier+ "=" + Integer.parseInt(proposalNumberText.getText()) + " AND date2  BETWEEN STR_TO_DATE('" + DateFromText.getText() + "', '%d-%m-%Y') AND STR_TO_DATE('" + DateToText.getText() + "', '%d-%m-%Y') order by dateTimeStamp;");

					System.out.println("resultset rowCount = " + rowCount);  
					if (rs.next()) {
						globalCurrentRowInRecordset++;
						String mid = rs.getString("messageid");
						messageIDText.setText(mid);	proposalDetailsText.setText(authorcorrected+ " " +bdfl_delegatecorrected);	dateText.setText(rs.getString("date2"));
						locationText.setText(rs.getString("folder"));		tsText.setText(rs.getString("file"));	
						activeTSText.setText(rs.getString("email"));		analyseWordsText.setText(rs.getString("analysewords"));
						String msg = rs.getString("email"), newMessage=""; newMessage = assignNumbersToSentences_FindMessageEnd(msg, newMessage,rs.getString("author"));	//marked sentence
						markedMessageText.setText(newMessage);			
						msgNumberInFile.setText(rs.getString("msgNumInFile"));   authorRole.setText(rs.getString("authorsRole")); messageType.setText(rs.getString("messageType"));
						wordsText.setText(rs.getString("subject"));		rowCountText.setText(String.valueOf(globalCurrentRowInRecordset) + "/" + String.valueOf(rowCount));		
						activeTSText.setCaretPosition(0);					analyseWordsText.setCaretPosition(0);		markedMessageText.setCaretPosition(0);
						causeMID.setText(mid);	effectMID.setText(mid);	//added for reasons 
					}
				} catch (SQLException insertException) {					displaySQLErrors(insertException);	}
				tf.setText("Proposal " + proposalDetailsText.getText());				word = tf.getText().trim();    
				updateHighlightFields(searcher);
			}
		});

		//Search specific list folder, for example ideas folder
		//most of the times no Proposal number is assiged to the proposal so just allow search by title
		//Do freeQueryButton
		locationQueryButton.addActionListener(new ActionListener() {
			public void actionPerformed(ActionEvent e) {
				rowCount = 0;    globalCurrentRowInRecordset=0;	  
				//    	  locationText    	  
				try {        	
					if(addParameters.isSelected()){  //check to see if we want to see all retrieved messages where status has changed 
						String temp = (String) location.getSelectedItem();
						//JOptionPane.showMessageDialog(null,"hello","TITLE",JOptionPane.WARNING_MESSAGE);
						rs = statement.executeQuery("SELECT * FROM "+messagesTableName+" WHERE ( email LIKE '%" + searchText.getText()  + "%' OR subject LIKE '%" + searchText.getText()  + "%') AND folder LIKE '%" + (String) location.getSelectedItem() + "%' order by dateTimeStamp, Proposal asc;");
						//JOptionPane.showMessageDialog(null,statement.toString(),"TITLE",JOptionPane.WARNING_MESSAGE);
						rowCount = guih.returnRowCount("SELECT * FROM "+messagesTableName+" WHERE ( email LIKE '%" + searchText.getText()  + "%' OR subject LIKE '%" + searchText.getText()  + "%') AND folder LIKE '%" + (String) location.getSelectedItem() + "%' order by dateTimeStamp, Proposal asc;");
						System.out.println(temp);
					}

					//	if (folderChanged == true){
					//		rs = statement.executeQuery("SELECT * FROM "+messagesTableName+" WHERE "+proposalIdentifier+" + Integer.parseInt(proposalNumberText.getText()) + " AND folder LIKE '"+ selectedFolder + "' AND email LIKE '%" + searchText.getText()  + "%' AND statusChanged =1 order by dateTimeStamp, messageid asc, Proposal asc;");
					//	}
					else{
						//JOptionPane.showMessageDialog(null, "Please select the option List Folder", "InfoBox: ", JOptionPane.INFORMATION_MESSAGE);        	    
						JOptionPane.showMessageDialog(null, "Please select the option List Folder");
					}																													
					// if (freeQueryText.getText().toUpperCase().indexOf("SELECT") >= 0) {
					//  rs = statement.executeQuery(freeQueryText.getText());

					System.out.println("resultset rowCount = " + rowCount); 					
					if (rs.next()) {
						populateFields();	//added for reasons 
					}
					int i = statement.executeUpdate(freeQueryText.getText()); 					errorText.append("Rows affected = " + i);
				} catch (SQLException insertException) {
					displaySQLErrors(insertException);
				}
				tf.setText("Proposal " + proposalDetailsText.getText());
				if (word==null || word.isEmpty() || word.equals("")) { 	word= "pep"; }
				word = searchText.getText().trim();   
				updateHighlightFields(searcher);
			}
		});

		//Do freeQueryButton
		gotoMessageIdButton.addActionListener(new ActionListener() {
			public void actionPerformed(ActionEvent e) {
				rowCount = 0;	globalCurrentRowInRecordset=0;
				try {
					int v_mid =Integer.parseInt(messageIdText.getText()) ;
					//can be used with proposal number	Integer.parseInt(proposalNumberText.getText())
					if (proposalNumberText.getText().trim().isEmpty() || proposalNumberText.getText().equals("")) {
						//JOptionPane.showMessageDialog (null, "a");
						//rs = statement.executeQuery("SELECT * FROM "+messagesTableName+" WHERE "+proposalIdentifier+" + Integer.parseInt(proposalNumberText.getText()) + " AND email LIKE '%" + searchText.getText()  + "%' AND statusChanged =1 order by dateTimeStamp, messageid asc, Proposal asc;");
						rs = statement.executeQuery("SELECT * FROM "+messagesTableName+" WHERE messageID =  " + v_mid+ ";");
						rowCount = guih.returnRowCount("SELECT * FROM "+messagesTableName+" WHERE messageID =  " + v_mid + ";");
					}
					else {	//messageIdText.getText().trim().length() >0 
						JOptionPane.showMessageDialog (null, "b");
						//rs = statement.executeQuery("SELECT * FROM "+messagesTableName+" WHERE "+proposalIdentifier+" + Integer.parseInt(proposalNumberText.getText()) + " AND email LIKE '%" + searchText.getText()  + "%' AND statusChanged =1 order by dateTimeStamp, messageid asc, Proposal asc;");
						rs = statement.executeQuery(   "SELECT * FROM "+messagesTableName+" WHERE "+proposalIdentifier+ "="+ Integer.parseInt(proposalNumberText.getText()) + " order by dateTimeStamp asc;");
						rowCount = guih.returnRowCount("SELECT * FROM "+messagesTableName+" WHERE "+proposalIdentifier+ "="+ Integer.parseInt(proposalNumberText.getText()) + " order by dateTimeStamp asc;");
						System.out.println("resultset");
						int midInRecord =0;
						while (rs.next() && midInRecord<v_mid) {
							midInRecord = Integer.parseInt(rs.getString("messageid")); System.out.println("midInRecord " +midInRecord);							
							//rs.next();
							//midInRecord = Integer.parseInt(rs.getString("messageid"));
							globalCurrentRowInRecordset++;	System.out.println("midInRecord " +midInRecord);
						}
					}					

					if (rowCount ==0)
						JOptionPane.showMessageDialog (null, "no record found");
					System.out.println("resultset rowCount = " + rowCount);

					if(rs.next()) {						
						String Message = rs.getString("email");			String mid = rs.getString("messageid");
						Integer x_Proposal =Integer.parseInt(rs.getString(proposalIdentifier));
						messageIDText.setText(mid);	proposalDetailsText.setText(authorcorrected+ " " +bdfl_delegatecorrected);	dateText.setText(rs.getString("date2"));locationText.setText(rs.getString("folder"));	tsText.setText(rs.getString("file"));
						wordsText.setText(rs.getString("subject"));		rowCountText.setText(String.valueOf(globalCurrentRowInRecordset) + "/" + String.valueOf(rowCount));	
						activeTSText.setText(rs.getString("email"));	analyseWordsText.setText(rs.getString("analysewords"));
						String msg = rs.getString("email"), newMessage=""; newMessage = assignNumbersToSentences_FindMessageEnd(msg, newMessage,rs.getString("author"));	//marked sentence
						markedMessageText.setText(newMessage);			
						msgNumberInFile.setText(rs.getString("msgNumInFile")); 		authorRole.setText(rs.getString("authorsRole")); 		messageType.setText(rs.getString("messageType"));			
						activeTSText.setCaretPosition(0);	analyseWordsText.setCaretPosition(0);	markedMessageText.setCaretPosition(0);
						causeMID.setText(mid);	effectMID.setText(mid);	//added for reasons 
						//classify
						String wordsFoundList = null;
						Utilities u = new Utilities();						Message = u.removeQuotedText(Message);
						Integer i = x_Proposal-1;						System.out.println("x_Proposal = " + x_Proposal); 	Integer k=1, max = x_Proposal;						//Integer min = 0;
						//Proposal[] p = new Proposal[max];							p[i] = new Proposal();						p[i].setProposalNumber(i);

						try {		  //  System.out.println("kkkkk");	        	
							//	            	wordsFoundList = u.checkEntireMessage ( Message,  wordsFoundList );  //1. Check entire message.		            	
							//		            wordsFoundList = u.check_sentence(Message,  wordsFoundList, p, i, k ); //2. Check sentences after Splitting message text into sentences
						}
						catch (Exception e2){ 
							System.out.println("error = " + e2.toString());  	            	
						}              
						//  classificationText.setText(wordsFoundList); 
						System.out.println("wordsFoundList = " + wordsFoundList);  	 
						//						classificationText.setText(wordsFoundList); 						classificationWords.setText(wordsFoundList); 
					}
					//  } 
					//  else {
					int i = statement.executeUpdate(freeQueryText.getText());				errorText.append("Rows affected = " + i);
					// loadAccounts();
					//    }
				} catch (SQLException insertException) {
					displaySQLErrors(insertException);
				}
				tf.setText("Proposal " + proposalDetailsText.getText());				Proposal_global = Integer.parseInt(proposalNumberText.getText()); 
				repaint();
			}
		});
	}

	public void setValues() { //List<String> searchKeyList, final WordSearcher searcher) {
		//Do Get Account Button
		getAccountButton.addActionListener(new ActionListener() {
			public void actionPerformed(ActionEvent e) {
				try {
					rs.beforeFirst();	globalCurrentRowInRecordset=0;
					while (rs.next()) {
						globalCurrentRowInRecordset++;
						if (rs.getString("id").equals(
								accountNumberList.getSelectedValue()))
							break;
					}
					if (!rs.isAfterLast()) {
						messageIDText.setText(rs.getString("messageid"));	proposalDetailsText.setText(authorcorrected+ " " +bdfl_delegatecorrected);	dateText.setText(rs.getString("date2"));
						locationText.setText(rs.getString("folder"));		tsText.setText(rs.getString("file"));	
						activeTSText.setText(rs.getString("email"));		analyseWordsText.setText(rs.getString("analysewords"));
						String msg = rs.getString("email"), newMessage=""; newMessage = assignNumbersToSentences_FindMessageEnd(msg, newMessage,rs.getString("author"));	//marked sentence
						markedMessageText.setText(newMessage);				rowCountText.setText(String.valueOf(globalCurrentRowInRecordset) + "/" + String.valueOf(rowCount));	
						msgNumberInFile.setText(rs.getString("msgNumInFile"));  wordsText.setText(rs.getString("subject"));		authorRole.setText(rs.getString("authorsRole")); messageType.setText(rs.getString("messageType"));
						activeTSText.setCaretPosition(0);					analyseWordsText.setCaretPosition(0);	markedMessageText.setCaretPosition(0);
					}
				} catch (SQLException selectException) {
					displaySQLErrors(selectException); }
			}
		});

		//Do Next Button		
		nextButton.addActionListener(new ActionListener() {
			public void actionPerformed(ActionEvent e) {
				try {
					if (!rs.isLast()) {
						rs.next();	
						globalCurrentRowInRecordset++;
						String mid = rs.getString("messageid");
						messageIDText.setText(mid);	proposalDetailsText.setText(authorcorrected+ " " +bdfl_delegatecorrected);	dateText.setText(rs.getString("date2"));
						locationText.setText(rs.getString("folder"));	tsText.setText(rs.getString("file"));		
						activeTSText.setText(rs.getString("email"));	analyseWordsText.setText(rs.getString("analysewords"));
						String msg = rs.getString("email"), newMessage=""; newMessage = assignNumbersToSentences_FindMessageEnd(msg, newMessage,rs.getString("author"));	//marked sentence
						markedMessageText.setText(newMessage);			rowCountText.setText(String.valueOf(globalCurrentRowInRecordset) + "/" + String.valueOf(rowCount));	
						msgNumberInFile.setText(rs.getString("msgNumInFile"));   authorRole.setText(rs.getString("authorsRole")); messageType.setText(rs.getString("messageType"));
						wordsText.setText(rs.getString("subject"));	
						activeTSText.setCaretPosition(0);				analyseWordsText.setCaretPosition(0);	markedMessageText.setCaretPosition(0);
						causeMID.setText(mid);	effectMID.setText(mid);	//added for reasons 
					}	

					String i = proposalDetailsText.getText();		tf.setText(i);
					searchKeyList.add("Proposal" + ": " + i);		searchKeyList.add("Proposal " + i + " ");	searchKeyList.add("Proposal " + i + "\\?");	searchKeyList.add("Proposal " + i + "\\."); 
					searchKeyList.add("Proposal " + i + "\\,"); 	searchKeyList.add("Proposal " + i + "\\;");
					if (word==null || word.isEmpty() || word.equals("")) {
						word= "pep";
					}

					//highlight the proposal number
					final WordSearcher searcher1 = new WordSearcher(activeTSText, Color.blue);
					int offset1 = searcher1.search(proposalDetailsText.getText());
					if (offset1 != -1) {
						try {
							activeTSText.scrollRectToVisible(activeTSText.modelToView(offset1));
						} catch (BadLocationException e2) {
						}
					}					

					word = tf.getText().trim();    int offset = searcher.search(word);                   //get text from keyword box				
					if (offset != -1) {						
						textPane.scrollRectToVisible(textPane.modelToView(offset));			//throw badlocation exception				
					} 

					searchWords = searchText.getText().trim();                       //get text from keyword box
					int offset2 = searcher.search(searchWords);
					if (offset2 != -1) {						
						textPane.scrollRectToVisible(textPane.modelToView(offset));							
					}
					Proposal_global = Integer.parseInt(proposalNumberText.getText()); 

					//classify text
					String textToClassify = activeTSText.getText();
					//***       TextClassifier tc = new TextClassifier(textToClassify);        
					//JOptionPane.showMessageDialog(null, tc.returnClassification(), "InfoBox: " + "Classification", JOptionPane.INFORMATION_MESSAGE);
					//***        classificationText.setText(tc.returnClassification()); 
				} 
				catch (BadLocationException g) {
				}
				catch (SQLException insertException) {
					displaySQLErrors(insertException);
				}
				catch (Exception ie) {}
			}
		});

		//gotomid within the curr resultset
		goToMIDButton.addActionListener(new ActionListener() {
			public void actionPerformed(ActionEvent e) {
				try {
					DateFormat dateFormat = new SimpleDateFormat("yyyy-MM-dd");	//2012-12-13
					Date goToDate,currDate;	
					goToDate = dateFormat.parse(DateFromText.getText()); //"2013-12-4");	
					currDate = dateFormat.parse(rs.getString("date2")); 
					System.out.println("goToDate "+goToDate); // Wed Dec 04 00:00:00 CST 2013				        System.out.println("currDate "+currDate);
					// String output = dateFormat.format(date); 
					//System.out.println(output); // 2013-12-04
					globalCurrentRowInRecordset=0;
					while (currDate.before(goToDate)) {
						rs.next();
						globalCurrentRowInRecordset++; 		            System.out.println("Date1 is before Date2");
						goToDate = dateFormat.parse(DateFromText.getText()); //"2013-12-4");
						currDate = dateFormat.parse(rs.getString("date2")); 
					}

					//currDate = rs.getString("date2");
					messageIDText.setText(rs.getString("messageid"));	proposalDetailsText.setText(authorcorrected+ " " +bdfl_delegatecorrected);	dateText.setText(rs.getString("date2"));
					locationText.setText(rs.getString("folder"));	tsText.setText(rs.getString("file"));		
					activeTSText.setText(rs.getString("email"));	analyseWordsText.setText(rs.getString("analysewords"));
					String msg = rs.getString("email"), newMessage=""; newMessage = assignNumbersToSentences_FindMessageEnd(msg, newMessage,rs.getString("author"));	//marked sentence
					markedMessageText.setText(newMessage);			rowCountText.setText(String.valueOf(globalCurrentRowInRecordset) + "/" + String.valueOf(rowCount));	
					msgNumberInFile.setText(rs.getString("msgNumInFile"));  authorRole.setText(rs.getString("authorsRole")); messageType.setText(rs.getString("messageType"));
					wordsText.setText(rs.getString("subject"));	
					activeTSText.setCaretPosition(0);				analyseWordsText.setCaretPosition(0);		markedMessageText.setCaretPosition(0);
				} catch (SQLException insertException) {
					displaySQLErrors(insertException);				}   
				catch (ParseException ex) {
					ex.printStackTrace();    }

				String i = proposalDetailsText.getText();		tf.setText(i);
				searchKeyList.add("Proposal" + ": " + i);		searchKeyList.add("Proposal " + i + " ");	searchKeyList.add("Proposal " + i + "\\?");	searchKeyList.add("Proposal " + i + "\\."); 
				searchKeyList.add("Proposal " + i + "\\,"); 		searchKeyList.add("Proposal " + i + "\\;");

				if (word==null || word.isEmpty() || word.equals("")) {
					word= "pep";
				}

				word = tf.getText().trim();    int offset = searcher.search(word);                   //get text from keyword box				
				if (offset != -1) {
					try {
						textPane.scrollRectToVisible(textPane.modelToView(offset));
					} catch (BadLocationException g) {
					}
				} 

				searchWords = searchText.getText().trim();                       //get text from keyword box
				int offset2 = searcher.search(searchWords);
				if (offset2 != -1) {
					try {
						textPane.scrollRectToVisible(textPane.modelToView(offset));
					} catch (BadLocationException g) {	}
				}
				Proposal_global = Integer.parseInt(proposalNumberText.getText()); 

				//classify text
				String textToClassify = activeTSText.getText();
				//***       TextClassifier tc = new TextClassifier(textToClassify);        
				//JOptionPane.showMessageDialog(null, tc.returnClassification(), "InfoBox: " + "Classification", JOptionPane.INFORMATION_MESSAGE);
				//***        classificationText.setText(tc.returnClassification());        
			}
		});

		//gotomid within the curr resultset
		searchWithRowQueryButton.addActionListener(new ActionListener() {
			public void actionPerformed(ActionEvent e) {
				try {
					int gotoMID = Integer.parseInt(rowCountText.getText());			        
					globalCurrentRowInRecordset=0;
					while (globalCurrentRowInRecordset <= gotoMID) {
						rs.next();
						globalCurrentRowInRecordset++; 
					}
					String mid = rs.getString("messageid");
					messageIDText.setText(mid);	proposalDetailsText.setText(authorcorrected+ " " +bdfl_delegatecorrected);	dateText.setText(rs.getString("date2"));
					locationText.setText(rs.getString("folder"));	tsText.setText(rs.getString("file"));		
					activeTSText.setText(rs.getString("email"));	analyseWordsText.setText(rs.getString("analysewords"));
					String msg = rs.getString("email"), newMessage=""; newMessage = assignNumbersToSentences_FindMessageEnd(msg, newMessage,rs.getString("author"));	//marked sentence
					markedMessageText.setText(newMessage);			rowCountText.setText(String.valueOf(globalCurrentRowInRecordset) + "/" + String.valueOf(rowCount));	
					msgNumberInFile.setText(rs.getString("msgNumInFile"));  authorRole.setText(rs.getString("authorsRole")); messageType.setText(rs.getString("messageType"));
					wordsText.setText(rs.getString("subject"));	
					activeTSText.setCaretPosition(0);				analyseWordsText.setCaretPosition(0);		markedMessageText.setCaretPosition(0);
					causeMID.setText(mid);	effectMID.setText(mid);	//added for reasons 
				} catch (Exception insertException) { }

				String i = proposalDetailsText.getText();		tf.setText(i);
				searchKeyList.add("Proposal" + ": " + i);		searchKeyList.add("Proposal " + i + " ");	searchKeyList.add("Proposal " + i + "\\?");	searchKeyList.add("Proposal " + i + "\\."); 
				searchKeyList.add("Proposal " + i + "\\,"); 		searchKeyList.add("Proposal " + i + "\\;");

				if (word==null || word.isEmpty() || word.equals("")) {
					word= "pep";
				}

				word = tf.getText().trim();    int offset = searcher.search(word);                   //get text from keyword box				
				if (offset != -1) {
					try {
						textPane.scrollRectToVisible(textPane.modelToView(offset));
					} catch (BadLocationException g) {
					}
				} 

				searchWords = searchText.getText().trim();                       //get text from keyword box
				int offset2 = searcher.search(searchWords);
				if (offset2 != -1) {
					try {
						textPane.scrollRectToVisible(textPane.modelToView(offset));
					} catch (BadLocationException g) {
					}
				}
				Proposal_global = Integer.parseInt(proposalNumberText.getText()); 

				//classify text
				String textToClassify = activeTSText.getText();
				//***       TextClassifier tc = new TextClassifier(textToClassify);        
				//JOptionPane.showMessageDialog(null, tc.returnClassification(), "InfoBox: " + "Classification", JOptionPane.INFORMATION_MESSAGE);
				//***        classificationText.setText(tc.returnClassification());        
			}
		});

		//Do Next Button
		previousButton.addActionListener(new ActionListener() {
			public void actionPerformed(ActionEvent e) {
				try {
					if (!rs.isFirst()) {
						rs.previous();	globalCurrentRowInRecordset--;
						String mid = rs.getString("messageid");
						messageIDText.setText(mid);	proposalDetailsText.setText(authorcorrected+ " " +bdfl_delegatecorrected);		dateText.setText(rs.getString("date2"));
						locationText.setText(rs.getString("folder"));		tsText.setText(rs.getString("file"));
						rowCountText.setText(String.valueOf(globalCurrentRowInRecordset) + "/" + String.valueOf(rowCount));	
						activeTSText.setText(rs.getString("email"));		analyseWordsText.setText(rs.getString("analysewords"));
						String msg = rs.getString("email"), newMessage=""; newMessage = assignNumbersToSentences_FindMessageEnd(msg, newMessage,rs.getString("author"));	//marked sentence
						markedMessageText.setText(newMessage);			
						msgNumberInFile.setText(rs.getString("msgNumInFile"));  	authorRole.setText(rs.getString("authorsRole")); 	messageType.setText(rs.getString("messageType"));				
						wordsText.setText(rs.getString("subject"));		
						activeTSText.setCaretPosition(0);					analyseWordsText.setCaretPosition(0);		markedMessageText.setCaretPosition(0);
						causeMID.setText(mid);	effectMID.setText(mid);	//added for reasons 
					}
				} catch (SQLException insertException) {
					displaySQLErrors(insertException);
				}
				tf.setText(proposalDetailsText.getText());				word = tf.getText().trim();                       //get text from keyword box
				if (word==null || word.isEmpty() || word.equals("")) {
					word= "pep";
				}
				int offset = searcher.search(word);
				if (offset != -1) {
					try {
						textPane.scrollRectToVisible(textPane.modelToView(offset));
					} catch (BadLocationException g) {
					}
				}

				searchWords = searchText.getText().trim();                       //get text from keyword box
				int offset2 = searcher.search(searchWords);
				if (offset2 != -1) {
					try {
						textPane.scrollRectToVisible(textPane.modelToView(offset));
					} catch (BadLocationException g) {
					}
				} 
				Proposal_global = Integer.parseInt(proposalNumberText.getText()); 

				//highlight the proposal number
				final WordSearcher searcher1 = new WordSearcher(activeTSText, Color.blue);
				int offset1 = searcher1.search(proposalDetailsText.getText());
				if (offset1 != -1) {
					try {
						activeTSText.scrollRectToVisible(activeTSText.modelToView(offset1));
					} catch (BadLocationException e2) {
					}
				}

				//classify text
				String textToClassify = activeTSText.getText();
				//***        TextClassifier tc = new TextClassifier(textToClassify);        
				//JOptionPane.showMessageDialog(null, tc.returnClassification(), "InfoBox: " + "Classification", JOptionPane.INFORMATION_MESSAGE);
				//***        classificationText.setText(tc.returnClassification());  
			}
		});
		
		lastButton.addActionListener(new ActionListener() {
			public void actionPerformed(ActionEvent e) {
				try {
					rs.last();	globalCurrentRowInRecordset = rowCount;
					messageIDText.setText(rs.getString("messageid"));	proposalDetailsText.setText(authorcorrected+ " " +bdfl_delegatecorrected);	dateText.setText(rs.getString("date2"));
					locationText.setText(rs.getString("folder"));	tsText.setText(rs.getString("file"));	
					activeTSText.setText(rs.getString("email"));	analyseWordsText.setText(rs.getString("analysewords"));
					rowCountText.setText(String.valueOf(globalCurrentRowInRecordset) + "/" + String.valueOf(rowCount));	
					String msg = rs.getString("email"), newMessage=""; newMessage = assignNumbersToSentences_FindMessageEnd(msg, newMessage,rs.getString("author"));	//marked sentence
					markedMessageText.setText(newMessage);			
					msgNumberInFile.setText(rs.getString("msgNumInFile"));  authorRole.setText(rs.getString("authorsRole")); messageType.setText(rs.getString("messageType"));
					wordsText.setText(rs.getString("subject"));		
					activeTSText.setCaretPosition(0);						analyseWordsText.setCaretPosition(0);
				} catch (SQLException insertException) {
					displaySQLErrors(insertException);
				}
			}
		});
		
		firstButton.addActionListener(new ActionListener() {
			public void actionPerformed(ActionEvent e) {
				try {
					rs.beforeFirst();	globalCurrentRowInRecordset=0;					 
					messageIDText.setText(rs.getString("messageid"));	proposalDetailsText.setText(authorcorrected+ " " +bdfl_delegatecorrected);		dateText.setText(rs.getString("date2"));	locationText.setText(rs.getString("folder"));						tsText.setText(rs.getString("file"));
					activeTSText.setText(rs.getString("email"));		analyseWordsText.setText(rs.getString("analysewords"));
					rowCountText.setText(String.valueOf(globalCurrentRowInRecordset) + "/" + String.valueOf(rowCount));	
					String msg = rs.getString("email"), newMessage=""; newMessage = assignNumbersToSentences_FindMessageEnd(msg, newMessage,rs.getString("author"));	//marked sentence
					markedMessageText.setText(newMessage);			
					msgNumberInFile.setText(rs.getString("msgNumInFile"));   authorRole.setText(rs.getString("authorsRole")); messageType.setText(rs.getString("messageType"));
					wordsText.setText(rs.getString("subject"));		
					activeTSText.setCaretPosition(0);					analyseWordsText.setCaretPosition(0);		markedMessageText.setCaretPosition(0);
				} catch (SQLException insertException) {
					displaySQLErrors(insertException);
				}
			}
		});

		//for inserting cause and effect
		insertAndShow.addActionListener(new ActionListener() {
			public void actionPerformed(ActionEvent e) {
				PreparedStatement preparedStatement = null;	//causeParagraphNum, causeSentenceNum,effectParagraphNum, effectSentenceNum
				//may 2020.. the columns need to be changed..but we dont use thos panel for data input anyway
				String insertTableSQL = "insert into trainingdata (pep, state,folder,file,msgNumberInFile,messageID, causeSentence, effectSentence, causeMessageID,effectMessageID, notes, label, "
						+ " communityReviewMessageID,proposalAuthorReviewMessageID,bdfldelegatePronouncementMessageID, communityReview,proposalAuthorReview,bdfldelegatePronouncement, dmconcept ) "
						+ " VALUES (?,?,?,?,?,?,?,?,?,?,?,?,?,?,?,?,?,?,?)";
				try {
					preparedStatement = connection.prepareStatement(insertTableSQL);
					preparedStatement.setInt(1, Integer.valueOf(proposalNumberText.getText()));			preparedStatement.setString(2, stateText.getItemAt(stateText.getSelectedIndex()));
					preparedStatement.setString(3, locationText.getText());		preparedStatement.setString(4, tsText.getText());	
					String ms = msgNumberInFile.getText(); int msint; 	if (ms==null || ms.equals("")) {msint = 0;} 	else {msint = Integer.valueOf(ms);}
					preparedStatement.setInt(5, msint);	preparedStatement.setInt(6, Integer.valueOf(messageIDText.getText()));	//messageIdText
					preparedStatement.setString(7, cause.getText());		preparedStatement.setString(8, effect.getText());
					String ef = effectMID.getText(), ca = causeMID.getText(); 
					int eff, cau;
					if (ef.isEmpty() || ef.equals("")) 	{eff=0;} 			else { 	eff = Integer.valueOf(ef); 		}	preparedStatement.setInt(10,eff );	
					if (ca.isEmpty() || ca.equals("")) 	{cau=0;}			else { 	cau = Integer.valueOf(ca); 		}	preparedStatement.setInt(9, cau);
					preparedStatement.setString(11, notesText.getText()); 	preparedStatement.setString(12, causeCategory.getItemAt(causeCategory.getSelectedIndex())); //label
					int aa=0; String a = communityCauseMID.getText(); if(a.equals("")) { aa=0; }	else { aa = Integer.valueOf(a); } 	preparedStatement.setInt(13, aa);
					int ab=0; String b = communityCauseMID.getText(); if(b.equals("")) { ab=0; }	else { ab = Integer.valueOf(b); }   preparedStatement.setInt(14, ab);
					int ac=0; String c = communityCauseMID.getText(); if(c.equals("")) { ac=0; }	else { ac = Integer.valueOf(c); } 	preparedStatement.setInt(15, ac);
					preparedStatement.setString(16, communityCauseSentence.getText());	preparedStatement.setString(17, authorCauseSentence.getText()); 
					preparedStatement.setString(18, bdfldelegateCauseSentence.getText());
					preparedStatement.setString(19, dmConcept.getItemAt(dmConcept.getSelectedIndex()));
					preparedStatement.executeUpdate();			// execute insert SQL stetement
					System.out.println("Record is inserted into Cause Effect table!");
					cause.setText("");	effect.setText("");  notesText.setText(""); stateText.setSelectedIndex(0);	causeCategory.setSelectedIndex(0);

					communityCauseSentence.setText(""); authorCauseSentence.setText(""); bdfldelegateCauseSentence.setText(""); 
					causeMID.setText(""); effectMID.setText(""); notesText.setText(""); communityCauseMID.setText(""); authorCauseMID.setText("");	bdfldelegateCauseMID.setText("");
					
				} catch (SQLException ea) { 		System.out.println(ea.getMessage()); ea.printStackTrace(); 				System.out.println(StackTraceToString(ea)  );		 }
			}
		});		
	}

	private void displaySQLErrors(SQLException e) {
		errorText.append("SQLException: " + e.getMessage() + "\n");		errorText.append("SQLState:     " + e.getSQLState() + "\n");	errorText.append("VendorError:  " + e.getErrorCode() + "\n");
	}

	private static class ButtonHandler implements ActionListener {
		public void actionPerformed(ActionEvent e) {
			// System.exit(0);
			// DisplayNormativeStates dns = new DisplayNormativeStates(451);   
			DisplayNormativeStates dns = new DisplayNormativeStates(Proposal_global);
			// dns.main(Proposal_global);
		}
	}
	
	//this function, if applied to the analyse words, would better identify first paragraph, as analuysewords removes the email header
	public String assignNumbersToSentences_FindMessageEnd(String msg, String newMessage, String messageAuthor) {
		if (msg ==null || msg.isEmpty() || msg.equals("")) {}
		else 
			msg = msg.toLowerCase(); 
		if (newMessage ==null || newMessage.isEmpty() || newMessage.equals("")) {}
		else
			newMessage= newMessage.toLowerCase(); 
		if (messageAuthor ==null || messageAuthor.isEmpty() || messageAuthor.equals("")) { messageAuthor=""; }
		else
			messageAuthor = messageAuthor.toLowerCase();
			
		String[] paragraphs = msg.split("\\n\\n");
		String newSentenceWithNumbers="", firstParagraph="",lastParagraph="", messageUntilLastParagraph="";
		//System.out.println("----- Now Processing new Message ID: ("+v_message_ID+") total paragraphs.." + paragraphs.length);
		Integer count=0,paragraphCounter=1,sentenceCounterinParagraph=1;        		   
		Boolean permanentMessageHasLabel = false;		//if message has any label captured set this to true
		
		//oct 2018...we want to get the last sentence in a message, therefore we have to first get the main message and then get the last sentence from that
		//we keep processing paragraphs until we come to a paragraph which has very few words and contains the firstname of the message author
		//also may have terms like 'regards', ..terms used in signature 
		//so we have a integer to store the index until that point
		Integer messageEndIndex= msg.length(); 
		boolean firstParagraphFound=false, endFoundInMessage=false,endFoundInParagraph=false;
		//GetMainMessageRemoveOtherParts
		String authorFirstName= "";
		if (messageAuthor ==null || messageAuthor.isEmpty() || messageAuthor.equals("")) {}
		else {
			if(messageAuthor.contains(" ")) {
				//if (m.getAuthor().split(" ").length > 1) {
					authorFirstName = messageAuthor.split(" ")[0];
				//}
			}
			else {
				authorFirstName = messageAuthor;
			}
		}
		Levenshtein levenshtein = new Levenshtein();
        //System.out.println("levenshtein.distance "+ levenshtein.distance("apple", "appl3")   );
		String debug = ""; String endingSentimentWords[] = {"regards","best regards","cheers","thanks"};
		for (String entireParagraph: paragraphs) {
			Integer l = entireParagraph.split(" ").length;
			
			//get first paragraph
			if(!firstParagraphFound && paragraphCounter<=2) {
				if(l > 5) {	//we dont consider short greetings at first paragraph
					firstParagraph = entireParagraph;
					firstParagraphFound=true;
				}
			}
			
			endFoundInParagraph = false; //end is
		
			//oct 2018 ..lets check to see if its message ending
			if(!endFoundInMessage && l < 7) {
				String terms[] = entireParagraph.split(" "); //get terms
				for (String t : terms) {
					t= t.toLowerCase();
					t = t.replaceAll("[-+.^:,]",""); //remove special characters like: - + ^ . : ,
					if(t.contains("."))		t= t.replace(".", "");
					if(t.contains(","))		t= t.replace(",", "");
					
					for (String ew: endingSentimentWords) {
						if(t.contains(ew)) {
							endFoundInMessage=true; endFoundInParagraph=true;
							lastParagraph = entireParagraph;
							firstParagraphFound=true;
						}
					}
					
					if(t.contains(messageAuthor) ||   t.contains(authorFirstName)) {	//firstname of author
						endFoundInMessage=true; endFoundInParagraph =true;
						messageEndIndex = msg.indexOf(entireParagraph);   //get index of paragraph
					}
					//try levestein distance
					if(levenshtein.distance(t,authorFirstName) < 3) {
						endFoundInMessage=true; endFoundInParagraph = true;
						messageEndIndex = msg.indexOf(entireParagraph);   //get index of paragraph
					} 
					debug = debug+t;
				}
			}
			
			//sometimes a paragrapgh has sentences but are not captured as sentences. So if sentence identified then check, so we just add a full-stop at end of paragraph	        				   
			if(!entireParagraph.endsWith("."))
				entireParagraph = entireParagraph + ".";
			Reader reader = new StringReader(entireParagraph);
			DocumentPreprocessor dp = new DocumentPreprocessor(reader);
			entireParagraph="";sentenceCounterinParagraph=1;
			for (List<HasWord> eachSentence : dp) {  
				boolean dependency = true, v_stateFound = false, foundLabel = false, double_Found = false,found=false;
				String nextSentence = "", CurrentSentenceString = Sentence.listToString(eachSentence);

				//System.out.println(" here c CurrentSentenceString: "+CurrentSentenceString);
				//remove unnecessary words like "Python Update ..."																	
				//				CurrentSentenceString = prp.psmp.removeLRBAndRRB(CurrentSentenceString);								
				//				CurrentSentenceString = prp.psmp.removeDivider(CurrentSentenceString);
				//				CurrentSentenceString = prp.psmp.removeUnwantedText(CurrentSentenceString);	
				//				CurrentSentenceString = prp.psmp.removeDoubleSpacesAndTrim(CurrentSentenceString);

				//newSentenceWithNumbers = CurrentSentenceString +"[" +sentenceCounterinParagraph + "]";
				//newMessage =newMessage+newSentenceWithNumbers;

				//mar 2018..try
				CurrentSentenceString.replace("\n","\\n\\n");
				
				entireParagraph = entireParagraph + CurrentSentenceString +" {SC: " +sentenceCounterinParagraph + "} ";
				if(endFoundInParagraph)
					entireParagraph = "["+ entireParagraph + "]";
				sentenceCounterinParagraph++;
			}

			newMessage =newMessage+entireParagraph + "{PC: " +paragraphCounter + "}"+ authorFirstName +"(" +l+  ") (debug="+debug+") " + "\n\n";
			paragraphCounter++;
		}
		return newMessage;
	}

	public void updateHighlightFields(final WordSearcher searcher) {
		int offset = searcher.search(word);                     //get text from keyword box				
		if (offset != -1) {
			try {
				textPane.scrollRectToVisible(textPane.modelToView(offset));
			} catch (BadLocationException g) {}
		}   

		searchWords = searchText.getText().trim();                       //get text from keyword box
		int offset2 = searcher.search(searchWords);
		if (offset2 != -1) {
			try {
				textPane.scrollRectToVisible(textPane.modelToView(offset));
			} catch (BadLocationException g) {}
		}
		Proposal_global = Integer.parseInt(proposalNumberText.getText());				repaint();
	}
	
	public void addListenersToLabels() {
		causeMID.addFocusListener(new FocusListener(){
			public void focusGained(FocusEvent e){	   causeMID.setText(""); 	} 	public void focusLost(FocusEvent e) {	  if (causeMID.getText()=="") causeMID.setText("Cause - MessageID ");			}
		});
		effectMID.addFocusListener(new FocusListener(){
			public void focusGained(FocusEvent e){	   effectMID.setText(""); } 	public void focusLost(FocusEvent e) {	   if (effectMID.getText()=="") effectMID.setText("Effect - MessageID ");			}
		});
		cause.addFocusListener(new FocusListener(){
			public void focusGained(FocusEvent e){	   cause.setText("");   } 	public void focusLost(FocusEvent e) {	   if (cause.getText()=="") cause.setText("Cause - Paragraph and Sentence: ");			}
		});
		effect.addFocusListener(new FocusListener(){
			public void focusGained(FocusEvent e){	   effect.setText("");   } 	public void focusLost(FocusEvent e) {	   if (effect.getText()=="") effect.setText("Effect - Paragraph and Sentence:  ");			}
		});
		communityCauseSentence.addFocusListener(new FocusListener(){
			public void focusGained(FocusEvent e){	   communityCauseSentence.setText(""); 	} 	public void focusLost(FocusEvent e) {	   if (communityCauseSentence.getText()=="") communityCauseSentence.setText("Community Cause Sentence  ");	}
		});
		authorCauseSentence.addFocusListener(new FocusListener(){
			public void focusGained(FocusEvent e){	   authorCauseSentence.setText("");   } 	public void focusLost(FocusEvent e) {	   if (authorCauseSentence.getText()=="") authorCauseSentence.setText("Author Cause Sentence ");			}
		});
		bdfldelegateCauseSentence.addFocusListener(new FocusListener(){
			public void focusGained(FocusEvent e){	   bdfldelegateCauseSentence.setText("");   } 	public void focusLost(FocusEvent e) {	   if (bdfldelegateCauseSentence.getText()=="") bdfldelegateCauseSentence.setText("BDFL-delgate Cause Sentence ");			}
		});
		notesText.addFocusListener(new FocusListener(){
			public void focusGained(FocusEvent e){	   notesText.setText("");   } 	public void focusLost(FocusEvent e) {	   if (notesText.getText()=="") notesText.setText("Notes ");			}
		});
		communityCauseMID.addFocusListener(new FocusListener(){
			public void focusGained(FocusEvent e){	   communityCauseMID.setText("");   } 	public void focusLost(FocusEvent e) {	  if (communityCauseMID.getText()=="")  communityCauseMID.setText("communityCause MID ");			}
		});
		authorCauseMID.addFocusListener(new FocusListener(){
			public void focusGained(FocusEvent e){	   authorCauseMID.setText("");   } 	public void focusLost(FocusEvent e) {	   		if (authorCauseMID.getText()=="") authorCauseMID.setText("authorCauseMID ");			}
		});
		bdfldelegateCauseMID.addFocusListener(new FocusListener(){
			public void focusGained(FocusEvent e){	   bdfldelegateCauseMID.setText("");   } 	public void focusLost(FocusEvent e) {	   if (bdfldelegateCauseMID.getText()=="") bdfldelegateCauseMID.setText("bdfldelegateCauseMID ");			}
		});
	}
	
	// Search for a word and return the offset of the first occurrence. Highlights are added for all occurrences found.
	public int searchAllWordsInParagraph(String[] words, JTextArea jtx) {
		int firstOffset = -1;
		Highlighter.HighlightPainter cyanPainter = null;
		if (words == null || words.equals("") ) {			return -1;		}		
		

		// Look for the word we are given - insensitive search
		String content = jtx.getText();
		
		
		//july 2018 ..we going to split the message into paragraphs		
		String sentence="";
		for (String w: words ) {	 
			sentence = sentence + w +" "; 
		} 
		sentence=sentence.trim();
		
		String[] paragraphs = content.split("\\n"); //"\\r?\\n\\r?\\n"); 	
		for (String para: paragraphs){ //System.out.println("new para");
			 
			para = para.replaceAll("\\p{P}", " "); //remove all punctuation ..maybe we can use this here (term matching for candidates) but not in CIE triple extraction as it wmay need commas and other punctuation
			para = para.replaceAll("\\r?\\n", " ");
			para = para.replaceAll("\\s+", " ").toLowerCase().trim(); //remove double spaces and trim
			
			String[] paraTerms = para.split(" ");
			boolean foundAll = true;
			for (String w: words ) {
			    if (!para.contains(w.trim())) {
			        foundAll = false;			        //System.out.println( "The value is not found! ("+ w+")");
			    }			   
			}			
			
		//	if (Arrays.asList(paraTerms).containsAll(Arrays.asList(words))) 
		//	{ //if all words is found in the paragraph
				if(foundAll) {	
//						SimpleAttributeSet sas = new SimpleAttributeSet();
//		                StyleConstants.setBackground(sas, Color.RED);
//		                StyledDocument doc = (StyledDocument) jtx.getDocument();		                
					System.out.println("\tAll terms in para, words: "+ sentence);
					System.out.println("\tParagraph contains all words on paragraph : " + para);	
					for (String word : words) {	//make sure all words in same paragraph
						word = word.trim().toLowerCase();
						int lastIndex = 0, wordSize = word.length();
						while ((lastIndex = para.indexOf(word, lastIndex)) != -1) {
							int endIndex = lastIndex + wordSize;
							try {
								//make sure that its a word on its own
								//just check to make sure that previous and next char is space char
								//	if(para.charAt(lastIndex-1) == ' ' || para.charAt(endIndex+1) == ' ' ) {
//									System.out.println("\tTerm: "+ para.substring(lastIndex, endIndex));
								//highlighterX.addHighlight(lastIndex, endIndex, painter);
								//doc.setCharacterAttributes(lastIndex, endIndex, sas, false);
								// jtx.getHighlighter().addHighlight(lastIndex, endIndex, DefaultHighlighter.DefaultPainter);
								 jtx.getHighlighter().addHighlight(lastIndex, endIndex, cyanPainter);
						            
								//added aug 2018
								
								//	}
							} catch (Exception e) {//BadLocation
								// Nothing to do
							}
							if (firstOffset == -1) {
								firstOffset = lastIndex;
							}
							lastIndex = endIndex;
						}
					}
				}			
				else {
				//	System.out.println("\nall terms not in para, words: ");
					for (String w: words) {
					//	System.out.print(w+" ");	
					}
					//System.out.println();
				}
		//	}
		}
		return firstOffset;
	}

	//set values for committed states JTable on left
	protected static JComboBox<String> setValuesStates() {
//			//String[] statusTo = new String[] {"None","Draft","Open","Active","Pending","Closed","Final","Accepted","Deferred","Replaced","Rejected","Postponed","Incomplete","Superseded","Withdrawn"};
//			//JComboBox<String> sToCBox = new JComboBox<>(statusTo);		String selectedsTo = (String) sToCBox.getSelectedItem();    
		JComboBox<String> sToCBox = new JComboBox<>();
		
		Statement stmt = null;		ResultSet rs;
		Integer forOutput_pepNumber = null, resultCounter=0;	
		try {
			stmt = connection.createStatement();			
						
			//empty table contents, in cases where new search is issued
//				mldmodel.setRowCount(0);
			//rs = stmt.executeQuery("select distinct(state) as state from states_reason_reasonsublabels");			
			//dec 2019 direct query pepstates table 
			rs = stmt.executeQuery("select distinct(state) as state from "+proposalIdentifier+committedStateTableName+";");	
			while (rs.next()) {
				String state = rs.getString("state");				
				sToCBox.addItem(state);
//					//folder = rs.getString(1);				counter = rs.getInt(2);				
//					//mldmodel.addRow(new Object[]{folder, counter});
			}

//				mldJTable.getColumnModel().getColumn(1).setCellRenderer(new TableCellLongTextRenderer ());
//				mldJTable.setRowHeight(20);
		} catch (SQLException e) {
			e.printStackTrace();
		}
		
		sToCBox.addActionListener(new ActionListener() {
			public void actionPerformed(ActionEvent e) {          
				String selectedsTo = (String) sToCBox.getSelectedItem();
			}
		});
		return sToCBox;
	}
	
	//jan 2021 set values for roles
	protected static JComboBox<String> setValuesRoles() {
//				//String[] statusTo = new String[] {"None","Draft","Open","Active","Pending","Closed","Final","Accepted","Deferred","Replaced","Rejected","Postponed","Incomplete","Superseded","Withdrawn"};
//				//JComboBox<String> sToCBox = new JComboBox<>(statusTo);		String selectedsTo = (String) sToCBox.getSelectedItem();    
		JComboBox<String> sToCBox = new JComboBox<>();
		
		Statement stmt = null;		ResultSet rs;
		Integer forOutput_pepNumber = null, resultCounter=0;	
		try {
			stmt = connection.createStatement();			
						
			//empty table contents, in cases where new search is issued
//					mldmodel.setRowCount(0);
			//rs = stmt.executeQuery("select distinct(state) as state from states_reason_reasonsublabels");			
			//dec 2019 direct query pepstates table 
			rs = stmt.executeQuery("select distinct(role) as role from "+roleTableName+";");
			//rs = stmt.executeQuery("select distinct(clusterbysenderfullname) as role from "+roleTableName+" WHERE " + proposalIdentifier +" = "+proposal+" ;");
			while (rs.next()) {
				String state = rs.getString("role");				
				sToCBox.addItem(state);
//						//folder = rs.getString(1);				counter = rs.getInt(2);				
//						//mldmodel.addRow(new Object[]{folder, counter});
			}

//					mldJTable.getColumnModel().getColumn(1).setCellRenderer(new TableCellLongTextRenderer ());
//					mldJTable.setRowHeight(20);
		} catch (SQLException e) {
			e.printStackTrace();
		}
		
		//may 2021 dummy
		
		sToCBox.addActionListener(new ActionListener() {
			public void actionPerformed(ActionEvent e) {          
				String selectedsTo = (String) sToCBox.getSelectedItem();
			}
		});
		return sToCBox;
	}
	
	
	//may 2021 set values for roles dummy method
	protected static JComboBox<String> setDummyValues() {
//					//String[] statusTo = new String[] {"None","Draft","Open","Active","Pending","Closed","Final","Accepted","Deferred","Replaced","Rejected","Postponed","Incomplete","Superseded","Withdrawn"};
//					//JComboBox<String> sToCBox = new JComboBox<>(statusTo);		String selectedsTo = (String) sToCBox.getSelectedItem();    
		JComboBox<String> sToCBox = new JComboBox<>();
		
		Statement stmt = null;		ResultSet rs;
		Integer forOutput_pepNumber = null, resultCounter=0;	
		try {			
				sToCBox.addItem("");
		} catch (Exception e) {
			e.printStackTrace();
		}
		return sToCBox;
	}

	//may 2021 ..upon user entering the proposal number, we retrieve all distinct message subjects
	//jan 2021 set values for roles
	protected static void setValuesRetrieveMsgSubjectsForProposal(JComboBox<MessageSubject> st) {
//					//String[] statusTo = new String[] {"None","Draft","Open","Active","Pending","Closed","Final","Accepted","Deferred","Replaced","Rejected","Postponed","Incomplete","Superseded","Withdrawn"};
//					//JComboBox<String> sToCBox = new JComboBox<>(statusTo);		String selectedsTo = (String) sToCBox.getSelectedItem();    
		JComboBox<String> sToCBox = new JComboBox<>();
		
		Statement stmt = null;		ResultSet rs;
		Integer forOutput_pepNumber = null, resultCounter=0;	
		try {
			stmt = connection.createStatement();			
						
			//empty table contents, in cases where new search is issued
//						mldmodel.setRowCount(0);
			//rs = stmt.executeQuery("select distinct(state) as state from states_reason_reasonsublabels");			
			String proposal = proposalNumberText.getText(); //, src = searchText.getText();
			if (proposal.isEmpty() ) {	//
				JOptionPane.showMessageDialog(null, "Error", "InfoBox: " + "Required", JOptionPane.INFORMATION_MESSAGE);
			}					
			
			// select distinct(subject) as msgsubjects, COUNT(*) AS cnt from allmessages WHERE BIP = 141 GROUP BY subject ORDER BY cnt desc
			rs = stmt.executeQuery("select distinct(subject) as msgsubject, count(*) as cnt from " + messagesTableName+" WHERE "+proposalIdentifier+"=" + Integer.parseInt(proposal) + " GROUP BY subject order by cnt desc;");	
			while (rs.next()) {
				String msgsubject = rs.getString("msgsubject");  Integer cnt = rs.getInt("cnt");
				
				if( msgsubject == null || msgsubject.isEmpty())
					msgsubject = "";
				
				String shortmsgsubject = msgsubject.replace("[bitcoin-dev]","").replace("[bitcoin-core-dev]", "").replace("[bitcoin-discuss]","").replace("Re:", "");
				shortmsgsubject = shortmsgsubject.replace("[Python-Dev]","").replace("[Python-ideas]", "").replace("[Python-list]","").replace("[Python-3000]","").replace("[Python-checkins]","").replace("Re:", "");
				shortmsgsubject = shortmsgsubject.trim();
				
				Integer l = shortmsgsubject.length();
				if (l<15)
					shortmsgsubject  = shortmsgsubject.substring(0,l);
				else
					shortmsgsubject  = shortmsgsubject.substring(0,15);
					
				
				//we store an object in combobox
				MessageSubject msubject = new MessageSubject(msgsubject,shortmsgsubject, cnt);
				st.addItem(msubject);
				
	//			System.out.println("herek21 " + msgsubject);			
				//sToCBox.addItem(msgsubject);  
//main old one				st.addItem(msgsubject);  //.substring(0, 7) 25
//							//folder = rs.getString(1);				counter = rs.getInt(2);				
//							//mldmodel.addRow(new Object[]{folder, counter});
			}
			comboBoxInitiallyUpdated = true; System.out.println("comboBoxInitiallyUpdated " + comboBoxInitiallyUpdated);

//						mldJTable.getColumnModel().getColumn(1).setCellRenderer(new TableCellLongTextRenderer ());
//						mldJTable.setRowHeight(20);
		} catch (SQLException e) {
			e.printStackTrace();  StackTraceToString(e);
		} catch (Exception e1) {
			e1.printStackTrace(); StackTraceToString(e1);
		}
		
//		sToCBox.addActionListener(new ActionListener() {
//			public void actionPerformed(ActionEvent e) {          
//				String selectedsTo = (String) sToCBox.getSelectedItem();
//			}
//		});
		//return sToCBox;
	}
	
	//setValuesRetrieveMsgAuthorsForProposal
	protected static void setValuesRetrieveMsgAuthorsForProposal(JComboBox<MessageAuthor> st) {
		//		//String[] statusTo = new String[] {"None","Draft","Open","Active","Pending","Closed","Final","Accepted","Deferred","Replaced","Rejected","Postponed","Incomplete","Superseded","Withdrawn"};
		//		//JComboBox<String> sToCBox = new JComboBox<>(statusTo);		String selectedsTo = (String) sToCBox.getSelectedItem();    
		JComboBox<String> sToCBox = new JComboBox<>();
		
		Statement stmt = null;		ResultSet rs;
		Integer forOutput_pepNumber = null, resultCounter=0;	
		try {
			stmt = connection.createStatement();			
						
			//empty table contents, in cases where new search is issued
			//			mldmodel.setRowCount(0);
			//rs = stmt.executeQuery("select distinct(state) as state from states_reason_reasonsublabels");			
			String proposal = proposalNumberText.getText(); //, src = searchText.getText();
			if (proposal.isEmpty() ) {	//
				JOptionPane.showMessageDialog(null, "Error", "InfoBox: " + "Required", JOptionPane.INFORMATION_MESSAGE);
			}					
			
			// select distinct(subject) as msgsubjects, COUNT(*) AS cnt from allmessages WHERE BIP = 141 GROUP BY subject ORDER BY cnt desc
			rs = stmt.executeQuery("select distinct(sendername) as sendername, count(*) as cnt from " + messagesTableName+" WHERE "+proposalIdentifier+"=" + Integer.parseInt(proposal) + " GROUP BY sendername order by cnt desc;");	
			while (rs.next()) {
				String msgsender = rs.getString("sendername");  Integer cnt = rs.getInt("cnt");
				//String shortmsgsubject = msgsubject.replace("[bitcoin-dev]","").replace("[bitcoin-core-dev]", "").replace("[bitcoin-discuss]","").replace("Re:", "");
				//shortmsgsubject = shortmsgsubject.trim();
				
				if( msgsender == null || msgsender.isEmpty())
					msgsender = "";
				
				String shortmsgauthor ="";
				//Integer l = msgsender.length();
				if (msgsender.contains(" ")) {
					shortmsgauthor = shortmsgauthor.split("\\s+")[0];
					//shortmsgsubject  = shortmsgsubject.substring(0,l);
				}
				else
					shortmsgauthor  = msgsender; 
					
				
				//we store an object in combobox
				MessageAuthor mauthor = new MessageAuthor(msgsender,shortmsgauthor, cnt);
				st.addItem(mauthor);
				
	//			System.out.println("msgsender " + msgsender + " shortmsgauthor " + shortmsgauthor + " cnt " + cnt);			
				//sToCBox.addItem(msgsubject);  
				//main old one				st.addItem(msgsubject);  //.substring(0, 7) 25
				//				//folder = rs.getString(1);				counter = rs.getInt(2);				
				//				//mldmodel.addRow(new Object[]{folder, counter});
			}
			
			comboBoxInitiallyUpdated = true; System.out.println("comboBoxInitiallyUpdated " + comboBoxInitiallyUpdated);
			
			//			mldJTable.getColumnModel().getColumn(1).setCellRenderer(new TableCellLongTextRenderer ());
			//			mldJTable.setRowHeight(20);
		} catch (SQLException e) {
			e.printStackTrace();  StackTraceToString(e);
		} catch (Exception e1) {
			e1.printStackTrace(); StackTraceToString(e1);
		}
		
		//sToCBox.addActionListener(new ActionListener() {
		//public void actionPerformed(ActionEvent e) {          
		//	String selectedsTo = (String) sToCBox.getSelectedItem();
		//}
		//});
		//return sToCBox;
	}
	
	
	protected static void setValuesRetrieveMsgDatesForProposal(JComboBox<MessageDate> st) {
		//		//String[] statusTo = new String[] {"None","Draft","Open","Active","Pending","Closed","Final","Accepted","Deferred","Replaced","Rejected","Postponed","Incomplete","Superseded","Withdrawn"};
		//		//JComboBox<String> sToCBox = new JComboBox<>(statusTo);		String selectedsTo = (String) sToCBox.getSelectedItem();    
		JComboBox<String> sToCBox = new JComboBox<>();
		
		Statement stmt = null;		ResultSet rs;
		Integer forOutput_pepNumber = null, resultCounter=0;	
		try {
			stmt = connection.createStatement();			
						
			//empty table contents, in cases where new search is issued
			//			mldmodel.setRowCount(0);
			//rs = stmt.executeQuery("select distinct(state) as state from states_reason_reasonsublabels");			
			String proposal = proposalNumberText.getText(); //, src = searchText.getText();
			if (proposal.isEmpty() ) {	//
				JOptionPane.showMessageDialog(null, "Error", "InfoBox: " + "Required", JOptionPane.INFORMATION_MESSAGE);
			}					
			
			// select distinct(subject) as msgsubjects, COUNT(*) AS cnt from allmessages WHERE BIP = 141 GROUP BY subject ORDER BY cnt desc
			rs = stmt.executeQuery("select distinct(date2) as dt, count(*) as cnt from " + messagesTableName+" WHERE "+proposalIdentifier+"=" + Integer.parseInt(proposal) + " GROUP BY dt order by cnt desc;");	
			while (rs.next()) {
				Date dt = rs.getDate("dt");  Integer cnt = rs.getInt("cnt");
				
				//we store an object in combobox
				MessageDate mdate = new MessageDate(dt, cnt);
				st.addItem(mdate);
				
	//			System.out.println("msgsender " + msgsender + " shortmsgauthor " + shortmsgauthor + " cnt " + cnt);			
				//sToCBox.addItem(msgsubject);  
				//main old one				st.addItem(msgsubject);  //.substring(0, 7) 25
				//				//folder = rs.getString(1);				counter = rs.getInt(2);				
				//				//mldmodel.addRow(new Object[]{folder, counter});
			}
			
			comboBoxInitiallyUpdated = true; System.out.println("comboBoxInitiallyUpdated " + comboBoxInitiallyUpdated);
			
			//			mldJTable.getColumnModel().getColumn(1).setCellRenderer(new TableCellLongTextRenderer ());
			//			mldJTable.setRowHeight(20);
		} catch (SQLException e) {
			e.printStackTrace();  StackTraceToString(e);
		} catch (Exception e1) {
			e1.printStackTrace(); StackTraceToString(e1);
		}
		
		//sToCBox.addActionListener(new ActionListener() {
		//public void actionPerformed(ActionEvent e) {          
		//	String selectedsTo = (String) sToCBox.getSelectedItem();
		//}
		//});
		//return sToCBox;
	}
	
	
	//jan 2021 set values for roles
	protected static JComboBox<String> setValuesPerson(String role, JComboBox<MessageAuthor> st) {
//					//String[] statusTo = new String[] {"None","Draft","Open","Active","Pending","Closed","Final","Accepted","Deferred","Replaced","Rejected","Postponed","Incomplete","Superseded","Withdrawn"};
//					//JComboBox<String> sToCBox = new JComboBox<>(statusTo);		String selectedsTo = (String) sToCBox.getSelectedItem();    
		JComboBox<String> sToCBox = new JComboBox<>();
		
		Statement stmt = null;		ResultSet rs;
		Integer forOutput_pepNumber = null, resultCounter=0;	
		try {
			stmt = connection.createStatement();			
						
			//empty table contents, in cases where new search is issued
//						mldmodel.setRowCount(0);
			//rs = stmt.executeQuery("select distinct(state) as state from states_reason_reasonsublabels");			
			//dec 2019 direct query pepstates table 
			//rs = stmt.executeQuery("select distinct(clusterBySenderFullName) as developer from "+roleTableName+" where " + proposalIdentifier + " = " + proposalNumberText.getText() + ";");      //       description like '%" +role+ "%' ;");
			
			String pn = "";
			if (proposalNumberText.getText()==null || proposalNumberText.getText().isEmpty())
				pn = "9";
			else
				pn = proposalNumberText.getText();
			//rs = stmt.executeQuery("select distinct(clusterBySenderFullName) as sendername from "+messagesTableName+" where " + proposalIdentifier + " = " + pn + ";");      //       description like '%" +role+ "%' ;");
			rs = stmt.executeQuery("select distinct(clusterBySenderFullName) as sendername, count(*) as cnt from " + messagesTableName+" WHERE "+proposalIdentifier+"=" + Integer.parseInt(pn) + " GROUP BY sendername order by cnt desc;");	
			while (rs.next()) {
				String msgsender = rs.getString("sendername");  Integer cnt = rs.getInt("cnt");
				//String shortmsgsubject = msgsubject.replace("[bitcoin-dev]","").replace("[bitcoin-core-dev]", "").replace("[bitcoin-discuss]","").replace("Re:", "");
				//shortmsgsubject = shortmsgsubject.trim();
				
				if( msgsender == null || msgsender.isEmpty())
					msgsender = "";
				
				String shortmsgauthor ="";
				//Integer l = msgsender.length();
				if (msgsender.contains(" ")) {
					shortmsgauthor = shortmsgauthor.split("\\s+")[0];
					//shortmsgsubject  = shortmsgsubject.substring(0,l);
				}
				else
					shortmsgauthor  = msgsender; 
					
				
				//we store an object in combobox
				MessageAuthor mauthor = new MessageAuthor(msgsender,shortmsgauthor, cnt);
				st.addItem(mauthor);
				System.out.println("devs chosen XTY : " + mauthor);
//							//folder = rs.getString(1);				counter = rs.getInt(2);				
//							//mldmodel.addRow(new Object[]{folder, counter});
			}

//						mldJTable.getColumnModel().getColumn(1).setCellRenderer(new TableCellLongTextRenderer ());
//						mldJTable.setRowHeight(20);
		} 		
		catch (SQLException e) {
			e.printStackTrace();
		}
		catch (Exception e1) {
			e1.printStackTrace();
		}
		
		sToCBox.addActionListener(new ActionListener() {
			public void actionPerformed(ActionEvent e) {          
				String selectedsTo = (String) sToCBox.getSelectedItem();
			}
		});
		return sToCBox;
	}
	
	protected static JComboBox<String> setValuesCompany() {
		//		//String[] statusTo = new String[] {"None","Draft","Open","Active","Pending","Closed","Final","Accepted","Deferred","Replaced","Rejected","Postponed","Incomplete","Superseded","Withdrawn"};
		//		//JComboBox<String> sToCBox = new JComboBox<>(statusTo);		String selectedsTo = (String) sToCBox.getSelectedItem();    
		JComboBox<String> sToCBox = new JComboBox<>();
		
		Statement stmt = null;		ResultSet rs;
		Integer forOutput_pepNumber = null, resultCounter=0;	
		try {
			stmt = connection.createStatement();			
						
			//empty table contents, in cases where new search is issued
		//			mldmodel.setRowCount(0);
			//rs = stmt.executeQuery("select distinct(state) as state from states_reason_reasonsublabels");			
			//dec 2019 direct query pepstates table 
			rs = stmt.executeQuery("select distinct(company) as company from " +companiesTableName+ "; ");	
			while (rs.next()) {
				String state = rs.getString("company");				
				sToCBox.addItem(state);
		//				//folder = rs.getString(1);				counter = rs.getInt(2);				
		//				//mldmodel.addRow(new Object[]{folder, counter});
			}
		
		//			mldJTable.getColumnModel().getColumn(1).setCellRenderer(new TableCellLongTextRenderer ());
		//			mldJTable.setRowHeight(20);
		} catch (SQLException e) {
			e.printStackTrace();
		}
		
		sToCBox.addActionListener(new ActionListener() {
			public void actionPerformed(ActionEvent e) {          
				String selectedsTo = (String) sToCBox.getSelectedItem();
			}
		});
		return sToCBox;
	}
	
	//set values for Reasons
	protected static JComboBox<String> setValuesReasons() {
		//String[] reasonsCategory = new String[] {"None","Not Stated","Basic, No Community Status","Consensus","No Consensus","Little interest","BDFL Pronouncement over Community Split","BDFL pronouncement over Consensus"};    
		//JComboBox<String> sfromCBox = new JComboBox<>(reasonsCategory);			String selectedsFrom = (String) sfromCBox.getSelectedItem();    
		
		JComboBox<String> sToCBox = new JComboBox<>();
		Statement stmt = null;		ResultSet rs;
		Integer forOutput_pepNumber = null, resultCounter=0;	
		try {
			stmt = connection.createStatement();			
					
			//rs = stmt.executeQuery("select distinct(reason) as reason from states_reason_reasonsublabels");
			//dec 2019 ..we just do direct query
			rs = stmt.executeQuery("select distinct(label) as reason from trainingdata");
			while (rs.next()) {
				String reason = rs.getString("reason");					
				sToCBox.addItem(reason);
			}
		} catch (SQLException e) {
			e.printStackTrace();
		}
		
		sToCBox.addActionListener(new ActionListener() {
			public void actionPerformed(ActionEvent e) {          
				
				String selectedsFrom;
				selectedsFrom = (String) sToCBox.getSelectedItem();
			}
		});
		return sToCBox;
	}
	//dec 2019 ..Set values for DM Concept
	protected static JComboBox<String> setValuesDMConcept() {
		//String[] reasonsCategory = new String[] {"None","Not Stated","Basic, No Community Status","Consensus","No Consensus","Little interest","BDFL Pronouncement over Community Split","BDFL pronouncement over Consensus"};    
		//JComboBox<String> sfromCBox = new JComboBox<>(reasonsCategory);			String selectedsFrom = (String) sfromCBox.getSelectedItem();    
		
		JComboBox<String> sToCBox = new JComboBox<>();
		Statement stmt = null;		ResultSet rs;
		Integer forOutput_pepNumber = null, resultCounter=0;	
		try {
			stmt = connection.createStatement();			
					
			//rs = stmt.executeQuery("select distinct(reason) as reason from states_reason_reasonsublabels");
			//dec 2019 ..we just do direct query
			rs = stmt.executeQuery("select distinct(dmconcept) as dmconcept from trainingdata");
			while (rs.next()) {
				String dmconcept = rs.getString("dmconcept");					
				sToCBox.addItem(dmconcept);
			}
		} catch (SQLException e) {
			e.printStackTrace();
		}
		
		sToCBox.addActionListener(new ActionListener() {
			public void actionPerformed(ActionEvent e) {          
				
				String selectedsFrom;
				selectedsFrom = (String) sToCBox.getSelectedItem();
			}
		});
		return sToCBox;
	}
	//set values for subreasons..not sure if its used any longer
	protected static JComboBox<String> setValuesSubReasons() {
		//String[] reasonsCategory = new String[] {"None","Not Stated","Basic, No Community Status","Consensus","No Consensus","Little interest","BDFL Pronouncement over Community Split","BDFL pronouncement over Consensus"};    
		//JComboBox<String> sfromCBox = new JComboBox<>(reasonsCategory);			String selectedsFrom = (String) sfromCBox.getSelectedItem();    
		
		JComboBox<String> sToCBox = new JComboBox<>();
		Statement stmt = null;		ResultSet rs;
		Integer forOutput_pepNumber = null, resultCounter=0;	
		try {
			stmt = connection.createStatement();			
					
			rs = stmt.executeQuery("select distinct(subReason) as subReason from states_reason_reasonsublabels");			
			while (rs.next()) {
				String subReason = rs.getString("subReason");					
				sToCBox.addItem(subReason);
			}
		} catch (SQLException e) {
			e.printStackTrace();
		}
		
		sToCBox.addActionListener(new ActionListener() {
			public void actionPerformed(ActionEvent e) {          
				String selectedsFrom = (String) sToCBox.getSelectedItem();
				// update();				
			}
		});
		return sToCBox;
	}	
		
		
	// recordset is a global variable and this function is called from several functions
	public void populateFields() throws SQLException {
		globalCurrentRowInRecordset++;
		String mid = rs.getString("messageid");
		messageIDText.setText(mid);		proposalDetailsText.setText(authorcorrected+ " " +bdfl_delegatecorrected);	dateText.setText(rs.getString("date2"));
		locationText.setText(rs.getString("folder"));	tsText.setText(rs.getString("file"));	
		activeTSText.setText(rs.getString("email"));		analyseWordsText.setText(rs.getString("analysewords"));
		String msg = rs.getString("email"), newMessage=""; newMessage = assignNumbersToSentences_FindMessageEnd(msg, newMessage,rs.getString("author"));	//marked sentence
		markedMessageText.setText(newMessage);			
		msgNumberInFile.setText(rs.getString("msgNumInFile"));  authorRole.setText(rs.getString("authorsRole"));  messageType.setText(rs.getString("messageType"));
		wordsText.setText(rs.getString("subject"));	rowCountText.setText(String.valueOf(globalCurrentRowInRecordset) + "/" + String.valueOf(rowCount));							
		activeTSText.setCaretPosition(0);		analyseWordsText.setCaretPosition(0);		markedMessageText.setCaretPosition(0);
		causeMID.setText(mid);	effectMID.setText(mid);
	}
	
	protected JComboBox<String> setValuesFolder() {
		String[] folders = new String[] {"%%","python-dev", "python-lists",	"python-ideas","distutils", "python-committers","python-announce-list", "python-checkins"};
		JComboBox<String> location = new JComboBox<>(folders);			String selectedFolder = (String) location.getSelectedItem();    
		location.addActionListener(new ActionListener() {
			public void actionPerformed(ActionEvent e) {          
				String selectedFolder = (String) location.getSelectedItem();				folderChanged = true;
				//update();
			}
		});
		return location;
	}

	protected static JComboBox<String> setMessageAuthorsComboBox() {
		String[] messageAuthors = new String[] {"All","proposalAuthor","bdfl","otherCommunityMember"};    
		JComboBox<String> sfromCBox = new JComboBox<>(messageAuthors);			String selectedsFrom = (String) sfromCBox.getSelectedItem();    
		sfromCBox.addActionListener(new ActionListener() {
			public void actionPerformed(ActionEvent e) {          
				String selectedsFrom = (String) sfromCBox.getSelectedItem();
				// update();				
			}
		});
		return sfromCBox;
	}

//	protected static JComboBox<String> setMsgSubjectComboBox() {
//			//rightTabInRightSideOfFrame.add(stateOnly); rightTabInRightSideOfFrame.add(stateAndReason); rightTabInRightSideOfFrame.add(stateReasonWithCue);	
//			//rightTabInRightSideOfFrame.add(sentences); rightTabInRightSideOfFrame.add(sentenceAndAdjacentSentences); rightTabInRightSideOfFrame.add(entireParagraphs); 
//			//rightTabInRightSideOfFrame.add(adjacentParagraphs); rightTabInRightSideOfFrame.add(entireMessage);
//			String[] messageAuthors = new String[] {"All","sentence","Entire messageOrSection","paragraph"}; // "Sent &Adj.Sent.", "Adja. Para"};    
//			JComboBox<String> sfromCBox = new JComboBox<>(messageAuthors);			String selectedsFrom = (String) sfromCBox.getSelectedItem();    
//			sfromCBox.addActionListener(new ActionListener() {
//				public void actionPerformed(ActionEvent e) {          
//					String selectedsFrom = (String) sfromCBox.getSelectedItem();
//					// update();				
//				}
//			});
//			return sfromCBox;
//		}
	
	protected static JComboBox<String> setLocationComboBox() {
		//rightTabInRightSideOfFrame.add(stateOnly); rightTabInRightSideOfFrame.add(stateAndReason); rightTabInRightSideOfFrame.add(stateReasonWithCue);	
		//rightTabInRightSideOfFrame.add(sentences); rightTabInRightSideOfFrame.add(sentenceAndAdjacentSentences); rightTabInRightSideOfFrame.add(entireParagraphs); 
		//rightTabInRightSideOfFrame.add(adjacentParagraphs); rightTabInRightSideOfFrame.add(entireMessage);
		String[] messageAuthors = new String[] {"All","sentence","Entire messageOrSection","paragraph"}; // "Sent &Adj.Sent.", "Adja. Para"};    
		JComboBox<String> sfromCBox = new JComboBox<>(messageAuthors);			String selectedsFrom = (String) sfromCBox.getSelectedItem();    
		sfromCBox.addActionListener(new ActionListener() {
			public void actionPerformed(ActionEvent e) {          
				String selectedsFrom = (String) sfromCBox.getSelectedItem();
				// update();				
			}
		});
		return sfromCBox;
	}

	protected static JComboBox<String> setCombinationComboBox() {
		String[] messageAuthors = new String[] {"All","Not Nulls","State Only","State&Reason","StateReasonWithCue"};    
		JComboBox<String> sfromCBox = new JComboBox<>(messageAuthors);			String selectedsFrom = (String) sfromCBox.getSelectedItem();    
		sfromCBox.addActionListener(new ActionListener() {
		public void actionPerformed(ActionEvent e) {          
			String selectedsFrom = (String) sfromCBox.getSelectedItem();
			// update();				
		}
	});
		return sfromCBox;
	}	
	
	

	public static String StackTraceToString(Exception ex) {
		String result = ex.toString() + "\n";
		StackTraceElement[] trace = ex.getStackTrace();
		for (int i=0;i<trace.length;i++) {
			result += trace[i].toString() + "\n";
		}
		return result;
	}
	
	//Table cell renderer
	class StateJTableRendererColumn extends DefaultTableCellRenderer {
		public Component getTableCellRendererComponent(JTable table, Object value, boolean isSelected,boolean hasFocus, int row, int column)
		{
			Object columnValue=table.getValueAt(row,table.getColumnModel().getColumnIndex("Score"));
			String g = (String) columnValue; //String g = (String) columnValue;
			boolean found= false, proceed = false;
			if (g== null || g.isEmpty()) 
			{ proceed = false; }
			else { proceed = true; }
//			System.out.println("Column Index = " + column);
			if (column == 5 && proceed ) {
//				for(String re: reasonsList) {	// System.out.print("\t re: "+ re + ","); 
					double d = Double.parseDouble(g); 
					if (d >= 3.0) { //.equals("very high")) {
						setBackground(java.awt.Color.yellow);	//pink, green, red, blue
						found= true; 	
						//break;
					}
					else {	setBackground(Color.white); 	}					
//				}
/*				for(String st: states) {	// System.out.print("\t re: "+ re + ","); 
					if (g.contains(st)) {
						setBackground(java.awt.Color.green);	//pink, green, red, blue
						found= true; 	break;
					}
					//						else {
					//							setBackground(Color.white);
					//						}					
				}
				for(String spt: specialTerms) {	// System.out.print("\t re: "+ re + ","); 
					if (g.contains(spt)) {
						setBackground(java.awt.Color.green);	//pink, green, red, blue
						found= true; 	break;
					}
					//						else {
					//							setBackground(Color.white);
					//						}					
				} */
				if(!found) {	setBackground(Color.white);        }
			}
			else {     setBackground(Color.white);   }		        
			if (column == 1) {        	}		        
			return super.getTableCellRendererComponent(table, value, isSelected,hasFocus, row, column);
		}
	}

	class StateJTableRendererRow extends DefaultTableCellRenderer {
		public Component getTableCellRendererComponent(JTable table, Object value, boolean isSelected,boolean hasFocus, int row, int column)
		{
			Object columnValue		=	table.getValueAt(row,table.getColumnModel().getColumnIndex("MID"));
			Integer g 				= 	Integer.valueOf((String) columnValue);

			if(table.getValueAt(row-1,table.getColumnModel().getColumnIndex("MID")) != null){
				Object columnValueMinus1 = table.getValueAt(row-1,table.getColumnModel().getColumnIndex("MID"));		        
				if (column == 1) {			        	
					Integer gMinus1  = Integer.valueOf((String) columnValueMinus1);			        	
					if(g==gMinus1) {	setBackground(Color.gray);     	}
					else { setBackground(Color.white);    }
				}
			}		        		        
			return super.getTableCellRendererComponent(table, value, isSelected,hasFocus, row, column);
		}
	}
	
}