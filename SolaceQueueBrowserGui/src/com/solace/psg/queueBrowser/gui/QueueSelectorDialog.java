package com.solace.psg.queueBrowser.gui;

import java.awt.BorderLayout;
import java.awt.Color;
import java.awt.Cursor;
import java.awt.Dimension;
import java.awt.FlowLayout;
import java.awt.Font;

import javax.swing.BorderFactory;
import javax.swing.JButton;
import javax.swing.JComboBox;
import javax.swing.JDialog;
import javax.swing.JFrame;
import javax.swing.JLabel;
import javax.swing.JPanel;
import javax.swing.SwingConstants;
import javax.swing.border.EmptyBorder;

public class QueueSelectorDialog {
    // Modern color scheme (consistent with other components)
    private static final Color PRIMARY_COLOR = new Color(0x2196F3);
    private static final Color SUCCESS_COLOR = new Color(0x4CAF50);
    private static final Color SURFACE_COLOR = new Color(0xFAFAFA);

    String selectedOption = "";

    private JButton createStyledButton(String text, Color backgroundColor) {
        JButton button = new JButton(text);
        button.setBackground(backgroundColor);
        button.setForeground(Color.WHITE);
        button.setFocusPainted(false);
        button.setBorderPainted(false);
        button.setFont(new Font("Segoe UI", Font.BOLD, 12));
        button.setPreferredSize(new Dimension(button.getPreferredSize().width + 16, 32));
        button.setCursor(new Cursor(Cursor.HAND_CURSOR));
        return button;
    }

    public String selectQueues(JFrame parentFrame, String[] queues) {
        // Create a JDialog
        JDialog dialog = new JDialog(parentFrame, "Queue Selection", true);
        dialog.setLocationRelativeTo(parentFrame);
        dialog.setLocation(parentFrame.getLocation().x + 10, parentFrame.getLocation().y + 10);
        dialog.setIconImage(parentFrame.getIconImage());

        dialog.setSize(400, 220);
        dialog.setLayout(new BorderLayout());
        dialog.getContentPane().setBackground(SURFACE_COLOR);
        
        // Add a label with instructions (left-aligned)
        JLabel instructionLabel = new JLabel("Select the destination queue from the list below, and then click 'Ok':");
        instructionLabel.setHorizontalAlignment(SwingConstants.LEFT);
        instructionLabel.setFont(new Font("Segoe UI", Font.PLAIN, 14));
        instructionLabel.setBorder(new EmptyBorder(16, 16, 12, 16));
        dialog.add(instructionLabel, BorderLayout.NORTH);

        // Create a JPanel to center the combo box with padding
        JPanel comboPanel = new JPanel(new FlowLayout(FlowLayout.CENTER));
        comboPanel.setBackground(SURFACE_COLOR);
        comboPanel.setBorder(new EmptyBorder(8, 16, 16, 16));
        
        //String[] options = (String[]) queues.toArray(new String[0]);
        
        JComboBox<String> comboBox = new JComboBox<>(queues);
        comboBox.setFont(new Font("Segoe UI", Font.PLAIN, 12));
        comboBox.setPreferredSize(new Dimension(300, 32));
        comboPanel.add(comboBox);
        dialog.add(comboPanel, BorderLayout.CENTER);

        // Create a JPanel for the OK button (right-aligned)
        JPanel buttonPanel = new JPanel(new FlowLayout(FlowLayout.RIGHT, 12, 12));
        buttonPanel.setBackground(SURFACE_COLOR);
        JButton okButton = createStyledButton("OK", SUCCESS_COLOR);
        buttonPanel.add(okButton);
        dialog.add(buttonPanel, BorderLayout.SOUTH);

        // Listener for the OK button
        okButton.addActionListener(e -> {
            selectedOption = (String) comboBox.getSelectedItem();
            System.out.println("Selected Option: " + selectedOption);
            dialog.dispose(); // Close the dialog
        });

        // Center the dialog relative to the parent and make it visible
        dialog.setLocationRelativeTo(parentFrame);
        dialog.setVisible(true);
        
        return selectedOption; 
    }
}
