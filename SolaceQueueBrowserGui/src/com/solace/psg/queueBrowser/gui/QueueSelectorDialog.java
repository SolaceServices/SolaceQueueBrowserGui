package com.solace.psg.queueBrowser.gui;

import java.awt.BorderLayout;
import java.awt.FlowLayout;

import javax.swing.BorderFactory;
import javax.swing.JButton;
import javax.swing.JComboBox;
import javax.swing.JDialog;
import javax.swing.JFrame;
import javax.swing.JLabel;
import javax.swing.JPanel;
import javax.swing.SwingConstants;

public class QueueSelectorDialog {
    String selectedOption = "";

    public String selectQueues(JFrame parentFrame, String[] queues) {
        // Create a JDialog
        JDialog dialog = new JDialog(parentFrame, "Queue Selection", true);
        dialog.setLocationRelativeTo(parentFrame);
        dialog.setLocation(parentFrame.getLocation().x + 10, parentFrame.getLocation().y + 10);
        dialog.setIconImage(parentFrame.getIconImage());

        dialog.setSize(350, 200);
        dialog.setLayout(new BorderLayout());
        
        // Add a label with instructions (left-aligned)
        JLabel instructionLabel = new JLabel("Select the destination queue from thelist below, and then click 'Ok':");
        instructionLabel.setHorizontalAlignment(SwingConstants.LEFT); // Left-aligned
        instructionLabel.setBorder(BorderFactory.createEmptyBorder(10, 10, 10, 10)); // Add space around it
        dialog.add(instructionLabel, BorderLayout.NORTH);

        // Create a JPanel to center the combo box with padding
        JPanel comboPanel = new JPanel(new FlowLayout(FlowLayout.CENTER)); // Centered layout
        comboPanel.setBorder(BorderFactory.createEmptyBorder(10, 10, 10, 10)); // Add space around the combo box
        
        //String[] options = (String[]) queues.toArray(new String[0]);
        
        JComboBox<String> comboBox = new JComboBox<>(queues);
        comboPanel.add(comboBox);
        dialog.add(comboPanel, BorderLayout.CENTER);

        // Create a JPanel for the OK button (right-aligned)
        JPanel buttonPanel = new JPanel(new FlowLayout(FlowLayout.RIGHT)); // Right-aligned layout
        JButton okButton = new JButton("OK");
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
