package com.solace.psg.queueBrowser.gui;

import java.awt.BorderLayout;
import java.awt.FlowLayout;

import javax.swing.JDialog;
import javax.swing.JPanel;
import javax.swing.JProgressBar;

public class SpinnerDialog extends JDialog {
	private static final long serialVersionUID = 1L;

	public SpinnerDialog(JDialog dialog) {
        super(dialog, "Applying filter...", false); // false = non-modal
        
        this.setSize(600, 300);
        setLayout(new BorderLayout());

        JProgressBar spinner = new JProgressBar();
        spinner.setIndeterminate(true);
        spinner.setStringPainted(true);
        spinner.setString("Please wait...");

        JPanel panel = new JPanel(new FlowLayout(FlowLayout.RIGHT));

        panel.add(spinner, BorderLayout.CENTER);
        add(panel);
        //setUndecorated(true); // Optional: remove window borders
        //pack();
        setLocationRelativeTo(dialog);

        setVisible(true);
    }
}