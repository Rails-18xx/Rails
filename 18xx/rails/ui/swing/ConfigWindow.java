package rails.ui.swing;

import java.awt.Color;
import java.awt.Component;
import java.awt.GridLayout;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.util.Map;
import java.util.List;

import javax.swing.JButton;
import javax.swing.JColorChooser;
import javax.swing.JComponent;
import javax.swing.JFormattedTextField;
import javax.swing.JFrame;
import javax.swing.JLabel;
import javax.swing.JPanel;
import javax.swing.JTabbedPane;

import rails.util.Config;
import rails.util.ConfigItem;
import rails.util.LocalText;

class ConfigWindow extends JFrame {
    private static final long serialVersionUID = 1L;
    
    private JTabbedPane pane;
    
    ConfigWindow() {
       // JFrame properties
        setTitle(LocalText.getText("ConfigWindowTitle"));
//        setSize(400,300);
        
        // add profile panel
        add(setupProfilePanel(), "North");
        
        // configSetup pane
        setupConfigPane();
        add(pane, "Center");
 
        // buttons
        JPanel buttonPanel = new JPanel();
        
        JButton saveButton = new JButton(LocalText.getText("Save"));
        saveButton.addActionListener(
                new ActionListener() {
                    public void actionPerformed(ActionEvent arg0) {
                        ConfigWindow.this.saveConfig();
                    }
                }
        );
        buttonPanel.add(saveButton);

        JButton cancelButton = new JButton(LocalText.getText("Cancel"));
        cancelButton.addActionListener(
                new ActionListener() {
                    public void actionPerformed(ActionEvent arg0) {
                        ConfigWindow.this.cancelConfig();
                    }
                }
        );
        buttonPanel.add(cancelButton);

        add(buttonPanel, "South");
        
        this.pack();
    }

    
    private JComponent setupProfilePanel() {
        JComponent panel = new JPanel();
        panel.setLayout(new GridLayout(0,4));
        
        // default profile
        
        
        return panel;
    }
    
    private void setupConfigPane() {
        // create pane
        pane = new JTabbedPane();
        
        Map<String, List<ConfigItem>> configPanels = Config.getConfigPanels();
        
        for (String panelName:configPanels.keySet()) {
            JPanel newPanel = new JPanel();
            newPanel.setLayout(new GridLayout(0,3));
            for (ConfigItem item:configPanels.get(panelName)) {
                defineElement(newPanel, item);
            }
            pane.addTab(panelName, newPanel);
        }
    }
    
    private void defineElement(JPanel panel, ConfigItem item) {
        
        panel.add(new JLabel(LocalText.getText("Config." + item.name)));
        
        final String configValue = Config.get(item.name);
        switch (item.type) {
        case COLOR: {
            final JLabel label = new JLabel(configValue);
            Color selectedColor;
            try {
                selectedColor = Color.decode(configValue);
            } catch (NumberFormatException e) {
               selectedColor = Color.WHITE;
            }
            label.setOpaque(true);
            label.setBackground(selectedColor);
            panel.add(label);
            JButton button = new JButton("Color");
            final Color oldColor = selectedColor; 
            button.addActionListener(
                    new ActionListener() {
                        public void actionPerformed(ActionEvent e) {
                            Color selectedColor=JColorChooser.showDialog(ConfigWindow.this, "", oldColor);
                            label.setText(Integer.toHexString(selectedColor.getRGB()).substring(2));
                            label.setBackground(selectedColor);
                        }
                    }
            );
            panel.add(button);
            break;
        }
        case STRING:
        default: {
            JFormattedTextField textField = new JFormattedTextField();
            textField.setValue(configValue);
            panel.add(textField);
        }
        }
    }
    
    private void saveConfig() {
        this.dispose();
    }

    private void cancelConfig() {
        this.dispose();
    }

}
