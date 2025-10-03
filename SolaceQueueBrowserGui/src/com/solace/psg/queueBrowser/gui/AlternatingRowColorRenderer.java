package com.solace.psg.queueBrowser.gui;

import java.awt.Color;
import java.awt.Component;

import javax.swing.JTable;
import javax.swing.table.DefaultTableCellRenderer;

public class AlternatingRowColorRenderer extends DefaultTableCellRenderer {
	private static final long serialVersionUID = 1L;

	// Modern alternating row colors
	private static final Color EVEN_ROW_COLOR = new Color(0xF8F9FA);  // Very light gray
	private static final Color ODD_ROW_COLOR = Color.WHITE;
	private static final Color HOVER_COLOR = new Color(0xE3F2FD);    // Light blue tint

	@Override
	public Component getTableCellRendererComponent(JTable table, Object value, boolean isSelected, boolean hasFocus,
			int row, int column) {
		Component c = super.getTableCellRendererComponent(table, value, isSelected, hasFocus, row, column);
		
		if (!isSelected) {
			// Modern alternating row colors with subtle distinction
			if (row % 2 == 0) {
				c.setBackground(EVEN_ROW_COLOR);
			} else {
				c.setBackground(ODD_ROW_COLOR);
			}
			// Ensure text is always visible on non-selected rows
			c.setForeground(Color.BLACK);
		} else {
			// Use table's selection background (set by FlatLaf theme)
			c.setBackground(table.getSelectionBackground());
			c.setForeground(table.getSelectionForeground());
		}
		
		// Set modern font
		c.setFont(new java.awt.Font("Segoe UI", java.awt.Font.PLAIN, 12));
		
		return c;
	}
}