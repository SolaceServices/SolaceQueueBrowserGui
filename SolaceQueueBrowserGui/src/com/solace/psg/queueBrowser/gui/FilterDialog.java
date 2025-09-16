package com.solace.psg.queueBrowser.gui;

import javax.swing.*;

import com.solace.psg.queueBrowser.gui.FilterSpecification.FilterCondition;
import com.solace.psg.queueBrowser.gui.headers.HeaderField;
import com.solace.psg.queueBrowser.gui.headers.PickListHeaderField;

import java.awt.*;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.util.ArrayList;

public class FilterDialog extends JDialog{
	public boolean cancelled = false;
	JPanel headerCards; 
	JComboBox<String> headerField;
    JComboBox<String> headerCondition;
    JTextField headerValue;
    JTextField propertyField;
    JComboBox<String> propertyCondition;
    JTextField propertyValue;
    JComboBox<String> bodyCondition;
    JTextField bodyValue;
    JDialog parent;
	JRadioButton yesHeaderButton;
	JRadioButton noHeaderButton;
	JComboBox<String> headerPicklistDropdown;
	
    FilterSpecification spec;
    
	public FilterDialog(JDialog dialog, FilterSpecification parentProvidedSpec) {
		super(dialog, "Message Filter", true);
        parent = dialog;
        spec = parentProvidedSpec;
	}
	private static String[] getHeaderFieldNames( ) {
		return new String[] {
			"Destination","Delivery Mode", "Reply-To Destination", "Time-To-Live (TTL)",
			"DMQ Eligible", "Immediate Acknowledgement", "Redelivery Flag",
			"Deliver-To-One", "Class of Service (CoS)", "Eliding Eligible",
			"Message ID", "Correlation ID", "Message Type", "Encoding", "Compression"
		};
	}
	private void clearAllControls() {
		headerField.setSelectedItem("Destination");
		headerCondition.setSelectedItem(FilterSpecification.FilterCondition.NONE);
		headerValue.setText("");
		
		propertyField.setText("");
		propertyCondition.setSelectedItem(FilterSpecification.FilterCondition.NONE);
		propertyValue.setText("");

		bodyCondition.setSelectedItem(FilterSpecification.FilterCondition.NONE);
		bodyValue.setText("");
	}
	private void initialPopulate() {
		
		String startingOutSelectedField = spec.headerField;
		if (startingOutSelectedField.isEmpty()) {
			startingOutSelectedField = spec.getDefaultHeaderFieldSelection();
		}
		headerField.setSelectedItem(spec.headerField);
		String currentSel = spec.headerCondition.getLabel();
		headerCondition.setSelectedItem(currentSel);
		headerValue.setText(spec.headerValue);
		
		propertyField.setText(spec.userPropField);
		propertyCondition.setSelectedItem(spec.userPropCondition.getLabel());
		propertyValue.setText(spec.userPropValue);

		bodyCondition.setSelectedItem(spec.bodyCondition.getLabel());
		bodyValue.setText(spec.bodyValue);
	}
	private static FilterCondition fromLabel(String label) {
	    for (FilterCondition f : FilterCondition.values()) {
	        if (f.getLabel().equalsIgnoreCase(label)) {
	            return f;
	        }
	    }
	    throw new IllegalArgumentException("No matching enum value for label: " + label);
	}
	
	private static String[] conditionValues() {
		ArrayList<String> list = new ArrayList<String>();
		for (FilterCondition f : FilterCondition.values()) {
			list.add(f.getLabel());
	    }
		String[] array = list.toArray(new String[0]);
		return array;
	}
	private void onApply() {
        this.spec.headerField = (String) headerField.getSelectedItem();
        String selected = (String) headerCondition.getSelectedItem();
        this.spec.headerCondition = fromLabel(selected);
        
        HeaderField selectedField = spec.find( this.spec.headerField);
		if (selectedField.eType.equals(HeaderField.eHeaderType.eFreeText)) {
	        this.spec.headerValue = headerValue.getText();
		}
		else if (selectedField.eType.equals(HeaderField.eHeaderType.eNumeric)) {
	        this.spec.headerValue = headerValue.getText();
		} 
		else if (selectedField.eType.equals(HeaderField.eHeaderType.eBoolean)) {
			this.spec.headerValue = "" + (yesHeaderButton.isSelected()); 
		} 
		else if (selectedField.eType.equals(HeaderField.eHeaderType.ePickList)) {
			this.spec.headerValue = (String) headerPicklistDropdown.getSelectedItem();
		}
        
        this.spec.userPropField = propertyField.getText();
        selected = (String) propertyCondition.getSelectedItem();
        this.spec.userPropCondition = fromLabel(selected);
        this.spec.userPropValue = propertyValue.getText();

        selected = (String) bodyCondition.getSelectedItem();
        this.spec.bodyCondition = fromLabel(selected);
        this.spec.bodyValue = bodyValue.getText();
        
        this.dispose();
	}
	private void onCancel() {
		cancelled = true;
		this.dispose();
	}
	
	private void onSelectHeaderField() {
	    CardLayout cl = (CardLayout) headerCards.getLayout();
        String selected = (String) headerField.getSelectedItem();

        // find the header field in the list, figure out the type and adjust the other controls as required
        HeaderField selectedField = spec.find(selected);
		
        headerCondition.removeAllItems();

		if (selectedField.eType.equals(HeaderField.eHeaderType.eFreeText)) {
    		ArrayList<String> list = new ArrayList<String>();
    		for (FilterCondition f : FilterCondition.values()) {
    			headerCondition.addItem(f.getLabel());
    	    }
    		String currentSel = spec.headerCondition.getLabel();
    		headerCondition.setSelectedItem(currentSel);

		    cl.show(headerCards, "Text");
        }
		else if (selectedField.eType.equals(HeaderField.eHeaderType.eNumeric)) {
			headerCondition.addItem(FilterCondition.EQUALS.name() );
		    cl.show(headerCards, "Text");
        }
		else if (selectedField.eType.equals(HeaderField.eHeaderType.eBoolean)) {
			headerCondition.addItem(FilterCondition.EQUALS.name() );
			yesHeaderButton.setSelected(true);
		    cl.show(headerCards, "Boolean");
        }
		else if (selectedField.eType.equals(HeaderField.eHeaderType.ePickList)) {
			headerCondition.addItem(FilterCondition.EQUALS.name() );
	        headerCondition.removeAllItems();
			headerCondition.addItem(FilterCondition.EQUALS.name() );
			
	        headerPicklistDropdown.removeAllItems();
	        System.out.println(headerPicklistDropdown.getItemCount()); // Should be 0

	        PickListHeaderField pick = (PickListHeaderField) selectedField;
	        for (String val : pick.values) {
	        	headerPicklistDropdown.addItem(val);
	        }
	        headerPicklistDropdown.setSelectedItem(pick.selectedValue);
	        
	        headerPicklistDropdown.revalidate();
	        headerPicklistDropdown.repaint();

		    cl.show(headerCards, "Dropdown");
        }

		headerCondition.revalidate(); // Refresh layout
		headerCondition.repaint();    // Force visual update

		//JOptionPane.showMessageDialog(null, f.eType.name());		
	}
	private JPanel headers() {
		headerCards = new JPanel(new CardLayout());
		
		ArrayList<String> dynamicHeaderNames = new ArrayList<String>();
		for (HeaderField f : spec.headers) {
			dynamicHeaderNames.add(f.name);
		}
		String[] options = dynamicHeaderNames.toArray(new String[0]);
 
		headerField = new JComboBox<>(options);
		headerField.addActionListener(new ActionListener() {
			@Override
			public void actionPerformed(ActionEvent e) {
				SwingUtilities.invokeLater(() -> {
					onSelectHeaderField();
				});
			}
		});
        headerCondition = new JComboBox<>(conditionValues());

		// 2. Create the input variants
		JPanel booleanPanel = new JPanel();
		yesHeaderButton = new JRadioButton("Yes");
		noHeaderButton = new JRadioButton("No");
		ButtonGroup group = new ButtonGroup();
		group.add(yesHeaderButton);
		group.add(noHeaderButton);
		booleanPanel.add(yesHeaderButton);
		booleanPanel.add(noHeaderButton);

		JPanel dropdownPanel = new JPanel();
		headerPicklistDropdown = new JComboBox<>(new String[] { });
		headerPicklistDropdown.setEditable(false);
		headerPicklistDropdown.setModel(new DefaultComboBoxModel<>());

		dropdownPanel.add(headerPicklistDropdown);

		JPanel textPanel = new JPanel();
		headerValue = new JTextField(20);
		textPanel.add(headerValue);

		// 3. CardLayout container
		headerCards.add(booleanPanel, "Boolean");
		headerCards.add(dropdownPanel, "Dropdown");
		headerCards.add(textPanel, "Text");

//		// 4. Listener to switch cards
//		fieldSelector.addActionListener(e -> {
//		    String selected = (String) fieldSelector.getSelectedItem();
//		    CardLayout cl = (CardLayout) headerCards.getLayout();
//		    cl.show(headerCards, selected);
//		});

		// 5. Add everything to your main panel
		JPanel mainPanel = new JPanel();
		//mainPanel.setLayout(new BorderLayout());
		mainPanel.setLayout(new FlowLayout());

		mainPanel.add(headerField);
		mainPanel.add(headerCondition);
		mainPanel.add(headerCards);
		return mainPanel;
	}
	public void run() {
//       SwingUtilities.invokeLater(() -> {
            //JFrame frame = new JFrame("Message Filter");
            this.setDefaultCloseOperation(JDialog.DISPOSE_ON_CLOSE);
            this.setSize(800, 600);
            this.setLayout(new GridLayout(4, 1));
            
            ImageIcon icon = new ImageIcon("config/filter.png");
			Image image = icon.getImage();
			this.setIconImage(image);

            // Header Filter Panel
            JPanel headerPanel = new JPanel(new FlowLayout());
            headerPanel.setBorder(BorderFactory.createTitledBorder("Header Filter"));
            headerField = new JComboBox<>(getHeaderFieldNames());
            headerCondition = new JComboBox<>(conditionValues());
            headerValue = new JTextField(20);
            
//            headerPanel.add(headerField);
//            headerPanel.add(headerCondition);
//            headerPanel.add(headerValue);
            headerPanel.add(this.headers());

            // Property Filter Panel
            JPanel propertyPanel = new JPanel(new FlowLayout());
            propertyPanel.setBorder(BorderFactory.createTitledBorder("User Property Filter"));
            propertyField = new JTextField(20);
            propertyCondition = new JComboBox<>(conditionValues());
            propertyValue = new JTextField(20);
            propertyPanel.add(propertyField);
            propertyPanel.add(propertyCondition);
            propertyPanel.add(propertyValue);

            // Body Filter Panel
            JPanel bodyPanel = new JPanel(new FlowLayout());
            bodyPanel.setBorder(BorderFactory.createTitledBorder("Body Filter"));
            bodyCondition = new JComboBox<>(conditionValues());
            bodyValue = new JTextField(20);
            bodyPanel.add(new JLabel("Body"));
            bodyPanel.add(bodyCondition);
            bodyPanel.add(bodyValue);

            JPanel buttonPanel = new JPanel(new FlowLayout(FlowLayout.RIGHT));

            JButton cancelButton = new JButton("Cancel");
            cancelButton.addActionListener(new ActionListener() {
    			@Override
    			public void actionPerformed(ActionEvent e) {
    				onCancel();
    			}
    		});

            JButton clearButton = new JButton("Clear Filter");
            clearButton.addActionListener(new ActionListener() {
    			@Override
    			public void actionPerformed(ActionEvent e) {
    				clearAllControls();
    			}
    		});

            JButton applyButton = new JButton("Apply");
            applyButton.addActionListener(new ActionListener() {
    			@Override
    			public void actionPerformed(ActionEvent e) {
    				onApply();
    			}
    		});

            buttonPanel.add(cancelButton);
            buttonPanel.add(clearButton);
            buttonPanel.add(applyButton);

            // Add panels to frame
            this.add(headerPanel);
            this.add(propertyPanel);
            this.add(bodyPanel);
            this.add(buttonPanel);

            this.pack();
    		this.setLocationRelativeTo(parent);

    		initialPopulate();
    		onSelectHeaderField();
    		
            // Show dialog
            this.setVisible(true);
 //       });
    }
    public static void main(String[] args) {
    	FilterDialog me = new FilterDialog((JDialog)null, new FilterSpecification());
    	me.run();
    	
    	System.out.println(me.spec.bodyValue);
    }
}
