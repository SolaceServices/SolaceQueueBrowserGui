package com.solace.psg.queueBrowser.gui.samples;

import javax.swing.*;
import javax.swing.table.*;
import java.awt.*;

public class CheckboxTableExample {
    public static void main(String[] args) {
        JFrame frame = new JFrame("Checkbox Column");
        frame.setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
        frame.setSize(400, 300);

        // Sample data: first column is Boolean (checkbox)
        Object[][] data = {
            {false, "Apple"},
            {true, "Banana"},
            {false, "Cherry"}
        };

        String[] columns = {"Select", "Fruit"};

        // Create table with custom model
        DefaultTableModel model = new DefaultTableModel(data, columns) {
            @Override
            public Class<?> getColumnClass(int columnIndex) {
                return columnIndex == 0 ? Boolean.class : String.class;
            }

            @Override
            public boolean isCellEditable(int row, int column) {
                return column == 0; // Only checkbox column is editable
            }
        };

        JTable table = new JTable(model);
        table.setFillsViewportHeight(true);

        frame.add(new JScrollPane(table), BorderLayout.CENTER);
        frame.setVisible(true);
    }
}