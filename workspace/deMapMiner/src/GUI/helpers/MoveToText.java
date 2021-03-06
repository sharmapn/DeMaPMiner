package GUI.helpers;

import java.awt.BorderLayout;
import java.awt.EventQueue;
import java.awt.GridBagConstraints;
import java.awt.GridBagLayout;
import java.awt.Rectangle;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.io.Reader;
import java.sql.Connection;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;

import javax.swing.JButton;
import javax.swing.JFrame;
import javax.swing.JPanel;
import javax.swing.JScrollPane;
import javax.swing.JTextArea;
import javax.swing.JTextField;
import javax.swing.UIManager;
import javax.swing.text.Document;

import connections.MysqlConnect;

import java.io.*;

public class MoveToText {
	
	static String first;
	static MysqlConnect mc = new MysqlConnect();
	static Connection connection = mc.connect();
	static String v_message = "", v_subject;
	
    public static void main(String[] args) {
//        new ShowMessageForID(123);
    }
    public void ShowMessageForID(Integer v_mid) {
    	
    	Statement stmt = null;			
		ResultSet rs;

		try {
			stmt = connection.createStatement();
			rs = stmt.executeQuery("select email,subject from allmessages where  messageID = " + v_mid);
			int sentenceCounter = 0;
			//				System.out.println("here tree 0" + rs.toString());
			while (rs.next()) {
				//					System.out.println("here tree 0");
				v_message = rs.getString(1);
				v_subject = rs.getString(2);
			}
		}
		catch (SQLException e) {

		}

		//JFrame frame = new JFrame(v_subject);
    	
        EventQueue.invokeLater(new Runnable() {
            @Override
            public void run() {
                try {
                    UIManager.setLookAndFeel(UIManager.getSystemLookAndFeelClassName());
                } 
                //catch (ClassNotFoundException | InstantiationException | IllegalAccessException | UnsupportedLookAndFeelException ex) {
                catch (Exception ex){
                }
                JFrame frame = new JFrame(v_subject);
                frame.setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
                frame.setLayout(new BorderLayout());
                frame.add(new FindTextPane());
                frame.setSize(400, 400);
                frame.setLocationRelativeTo(null);
                frame.setVisible(true);
            }
        });
    }
    public class FindTextPane extends JPanel {
        private JTextField findField;
        private JButton findButton;
        private JTextArea textArea;
        private int pos = 0;
        public FindTextPane() {
            setLayout(new BorderLayout());
            findButton = new JButton("Next");
            findField = new JTextField("Java", 10);
            textArea = new JTextArea();
            textArea.setWrapStyleWord(true);
            textArea.setLineWrap(true);
//            Reader reader = null;
//            try {
//                reader = new FileReader(new File("c:\\scripts\\Java.txt"));
//                textArea.read(reader, null);
//            } catch (Exception e) {
//                e.printStackTrace();
//            } finally {
//                try {
//                    reader.close();
//                } catch (Exception e) {
//                }
//            }
            textArea.setText(v_message);
            JPanel header = new JPanel(new GridBagLayout());
            GridBagConstraints gbc = new GridBagConstraints();
            gbc.gridx = 0;
            gbc.gridy = 0;
            gbc.anchor = GridBagConstraints.WEST;
            header.add(findField, gbc);
            gbc.gridx++;
            header.add(findButton, gbc);
            add(header, BorderLayout.NORTH);
            add(new JScrollPane(textArea));
            findButton.addActionListener(new ActionListener() {
                @Override
                public void actionPerformed(ActionEvent e) {
                    // Get the text to find...convert it to lower case for eaiser comparision
                    String find = findField.getText().toLowerCase();
                    // Focus the text area, otherwise the highlighting won't show up
                    textArea.requestFocusInWindow();
                    // Make sure we have a valid search term
                    if (find != null && find.length() > 0) {
                        Document document = textArea.getDocument();
                        int findLength = find.length();
                        try {
                            boolean found = false;
                            // Rest the search position if we're at the end of the document
                            if (pos + findLength > document.getLength()) {
                                pos = 0;
                            }
                            // While we haven't reached the end...
                            // "<=" Correction
                            while (pos + findLength <= document.getLength()) {
                                // Extract the text from teh docuemnt
                                String match = document.getText(pos, findLength).toLowerCase();
                                // Check to see if it matches or request
                                if (match.equals(find)) {
                                    found = true;
                                    break;
                                }
                                pos++;
                            }
                            // Did we find something...
                            if (found) {
                                // Get the rectangle of the where the text would be visible...
                                Rectangle viewRect = textArea.modelToView(pos);
                                // Scroll to make the rectangle visible
                                textArea.scrollRectToVisible(viewRect);
                                // Highlight the text
                                textArea.setCaretPosition(pos + findLength);
                                textArea.moveCaretPosition(pos);
                                // Move the search position beyond the current match
                                pos += findLength;
                            }
                        } catch (Exception exp) {
                            exp.printStackTrace();
                        }
                    }
                }
            });
        }
    }
}