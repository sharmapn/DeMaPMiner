package main.helpers;

import java.awt.*;

import javax.swing.*;
import javax.swing.table.*;
import javax.swing.border.*;
import java.awt.BorderLayout;

import java.awt.Color;

import java.awt.Component;



import javax.swing.JFrame;

import javax.swing.JScrollPane;

import javax.swing.JTable;

import javax.swing.JTextField;

import javax.swing.table.DefaultTableModel;

import javax.swing.table.TableCellRenderer;



import java.awt.BorderLayout;
import java.awt.Color;
import java.awt.Component;
import javax.swing.JFrame;
import javax.swing.JScrollPane;
import javax.swing.JTable;
import javax.swing.JTextField;
import javax.swing.table.DefaultTableModel;
import javax.swing.table.TableCellRenderer;

public class EvenOddRowCellRenderer extends JFrame {
  DefaultTableModel tmodel = new DefaultTableModel(new Object[][] { { "some", "text" },
      { "any", "text" }, { "even", "more" }, { "text", "strings" }, { "and", "other" },
      { "text", "values" } }, new Object[] { "Column 1", "Column 2" });
  public EvenOddRowCellRenderer() {
    setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
    JTable table = new JTable(tmodel);
    table.setDefaultRenderer(Object.class, new MyRenderer());
    getContentPane().add(new JScrollPane(table), BorderLayout.CENTER);
    pack();
  }
  public static void main(String arg[]) {
    new EvenOddRowCellRenderer().setVisible(true);
  }
}
class MyRenderer implements TableCellRenderer {
  public Component getTableCellRendererComponent(JTable table, Object value, boolean isSelected,
      boolean hasFocus, int row, int column) {
    JTextField editor = new JTextField();
    if (value != null)
      editor.setText(value.toString());
    editor.setBackground((row % 2 == 0) ? Color.white : Color.cyan);
    return editor;
  }
}