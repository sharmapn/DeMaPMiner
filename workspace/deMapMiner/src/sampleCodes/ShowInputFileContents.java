package sampleCodes;

import java.awt.BorderLayout;
import java.awt.Color;
import java.awt.event.KeyEvent;

import javax.swing.ImageIcon;
import javax.swing.JButton;
import javax.swing.JFrame;
import javax.swing.JTabbedPane;
import javax.swing.event.ChangeEvent;
import javax.swing.event.ChangeListener;

public class ShowInputFileContents {
  static Color colors[] = { Color.RED, Color.ORANGE, Color.YELLOW, Color.GREEN, Color.BLUE,
      Color.MAGENTA };

  static void add(JTabbedPane tabbedPane, String label) {
    int count = tabbedPane.getTabCount();
    JButton button = new JButton(label);
    //button.setBackground(colors[count]);
    tabbedPane.addTab(label, new ImageIcon("yourFile.gif"), button, label);
  }

  public static void main(String args[]) {
    JFrame frame = new JFrame("Tabbed Pane Sample");
    frame.setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);

    JTabbedPane tabbedPane = new JTabbedPane();
    tabbedPane.setTabLayoutPolicy(JTabbedPane.SCROLL_TAB_LAYOUT);
    String titles[] = { "A", "B", "C", "D", "E", "F" };
    for (int i = 0, n = titles.length; i < n; i++) {
      add(tabbedPane, titles[i]);
    }
    add(tabbedPane, "File Content");

    frame.add(tabbedPane, BorderLayout.CENTER);
    frame.setSize(800, 600);
    frame.setVisible(true);
  }
}
