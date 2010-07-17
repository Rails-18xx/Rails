package rails.ui.swing;

import java.awt.Color;
import java.awt.Component;
import java.awt.EventQueue;
import java.awt.GridLayout;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.awt.event.FocusEvent;
import java.awt.event.FocusListener;
import java.awt.event.ItemEvent;
import java.awt.event.ItemListener;
import java.io.File;
import java.util.Map;
import java.util.List;

import javax.swing.BorderFactory;
import javax.swing.JButton;
import javax.swing.JColorChooser;
import javax.swing.JComboBox;
import javax.swing.JComponent;
import javax.swing.JFileChooser;
import javax.swing.filechooser.FileFilter;
import javax.swing.JFormattedTextField;
import javax.swing.JFrame;
import javax.swing.JLabel;
import javax.swing.JPanel;
import javax.swing.JSpinner;
import javax.swing.JTabbedPane;
import javax.swing.SpinnerListModel;
import javax.swing.SwingConstants;
import javax.swing.border.Border;
import javax.swing.event.DocumentEvent;
import javax.swing.event.DocumentListener;

import rails.game.ConfigurationException;
import rails.util.Config;
import rails.util.ConfigItem;
import rails.util.LocalText;
import rails.util.Util;

class ConfigWindow extends JFrame {
    private static final long serialVersionUID = 1L;
    
    private JPanel profilePanel;
    private JTabbedPane configPane;
    private JPanel buttonPanel;
    
    ConfigWindow() {
       // JFrame properties
        setTitle(LocalText.getText("ConfigWindowTitle"));
//        setSize(400,300);
        
        // add profile panel
        profilePanel = new JPanel();
        add(profilePanel, "North");
        
        // configSetup pane
        configPane = new JTabbedPane();
        add(configPane, "Center");
 
        // buttons
        buttonPanel = new JPanel();
        add(buttonPanel, "South");

        init();
    }
    
    private void init() {
        setupProfilePanel();
        setupConfigPane();
        setupButtonPanel();
        this.pack();
    }

    private void setupProfilePanel() {
        profilePanel.removeAll();
        
        profilePanel.setLayout(new GridLayout(0,4));
        
        String activeProfile = Config.getActiveProfileName();
        String defaultProfile = Config.getDefaultProfileName();
        Border etched = BorderFactory.createEtchedBorder();
        Border titled = BorderFactory.createTitledBorder(etched, LocalText.getText("CONFIG_CURRENT_PROFILE", activeProfile, defaultProfile));
        profilePanel.setBorder(titled);
        
        JLabel userLabel = new JLabel(LocalText.getText("CONFIG_SELECT_USER"));
        profilePanel.add(userLabel);
        final JComboBox comboBoxUser = new JComboBox(Config.getUserProfiles().toArray());
        comboBoxUser.setSelectedItem(Config.getActiveProfileName());
        comboBoxUser.addItemListener(new ItemListener() {
            public void itemStateChanged(ItemEvent arg0) {
                Config.changeActiveProfile((String)comboBoxUser.getSelectedItem());
                EventQueue.invokeLater(new Runnable() {
                    public void run() {
                        init();
                        ConfigWindow.this.repaint();
                    }
                }
                );
            }
        }
        );
        profilePanel.add(comboBoxUser);

    }
    
    private void setupConfigPane() {
        configPane.removeAll();
        
        Border etched = BorderFactory.createEtchedBorder();
        Border titled = BorderFactory.createTitledBorder(etched, LocalText.getText("CONFIG_SETTINGS"));
        configPane.setBorder(titled);
        
        Map<String, List<ConfigItem>> configPanels = Config.getConfigPanels();
        
        for (String panelName:configPanels.keySet()) {
            JPanel newPanel = new JPanel();
            newPanel.setLayout(new GridLayout(0,3));
            for (ConfigItem item:configPanels.get(panelName)) {
                defineElement(newPanel, item);
            }
            configPane.addTab(panelName, newPanel);
        }
    }
    
    private void defineElement(JPanel panel, final ConfigItem item) {
        // item label
        panel.add(new JLabel(LocalText.getText("Config." + item.name)));
        
        // standard components
        final String configValue = item.getCurrentValue();

        
        switch (item.type) {
        case COLOR:
            final JLabel label = new JLabel(configValue);
            Color selectedColor;
            try {
                selectedColor = Util.parseColour(configValue);
            } catch (ConfigurationException e) {
               selectedColor = Color.WHITE;
            }
            label.setOpaque(true);
            label.setHorizontalAlignment(SwingConstants.CENTER);
            label.setBackground(selectedColor);
            if (Util.isDark(selectedColor)) {
                label.setForeground(Color.WHITE);
            } else {
                label.setForeground(Color.BLACK);
            }
            panel.add(label);
            JButton button = new JButton("Choose...");
            final Color oldColor = selectedColor; 
            button.addActionListener(
                    new ActionListener() {
                        public void actionPerformed(ActionEvent e) {
                            Color selectedColor=JColorChooser.showDialog(ConfigWindow.this, "", oldColor);
                            if (selectedColor == null) return;
                            String newValue = Integer.toHexString(selectedColor.getRGB()).substring(3); 
                            label.setText(newValue);
                            item.setNewValue(newValue);
                            label.setBackground(selectedColor);
                            if (Util.isDark(selectedColor)) {
                                label.setForeground(Color.WHITE);
                            } else {
                                label.setForeground(Color.BLACK);
                            }
                        }
                    }
            );
            panel.add(button);
            break;
        case LIST:
            final JComboBox comboBox = new JComboBox(item.allowedValues.toArray());
            comboBox.setSelectedItem(configValue);
            comboBox.addFocusListener(new FocusListener() {
                public void focusGained(FocusEvent arg0) {
                    // do nothing
                }
                public void focusLost(FocusEvent arg0) {
                  item.setNewValue((String)comboBox.getSelectedItem());
                }
            }
            );
            panel.add(comboBox);
            break;
        case STRING:
        default: // default like String
            final JFormattedTextField textField = new JFormattedTextField();
            textField.setValue(configValue);
            textField.addFocusListener(new FocusListener() {
                public void focusGained(FocusEvent arg0) {
                    // do nothing
                }
                public void focusLost(FocusEvent arg0) {
                  item.setNewValue(textField.getText());
                }
            }
            );
            panel.add(textField);
            break;
        }
    }

    private void setupButtonPanel() {
        buttonPanel.removeAll();
        
        // save button
        if (Config.isFilePathDefined()) {
            JButton saveButton = new JButton(LocalText.getText("SAVE"));
            saveButton.addActionListener(
                    new ActionListener() {
                        public void actionPerformed(ActionEvent arg0) {
                            ConfigWindow.this.saveConfig();
                        }
                    }
            );
            buttonPanel.add(saveButton);
        }

        JButton saveAsButton = new JButton(LocalText.getText("SAVEAS"));
        saveAsButton.addActionListener(
                new ActionListener() {
                    public void actionPerformed(ActionEvent arg0) {
                        ConfigWindow.this.saveAsConfig();
                    }
                }
        );
        buttonPanel.add(saveAsButton);

        JButton cancelButton = new JButton(LocalText.getText("CANCEL"));
        cancelButton.addActionListener(
                new ActionListener() {
                    public void actionPerformed(ActionEvent arg0) {
                        ConfigWindow.this.cancelConfig();
                    }
                }
        );
        buttonPanel.add(cancelButton);
        
    }
    
    private void saveConfig() {
        Config.saveActiveProfile();
    }

    private void saveAsConfig() {
        JFileChooser fc = new JFileChooser();
        fc.setFileFilter(
                new FileFilter() {
                    public boolean accept( File f ){
                        return f.isDirectory() ||
                        f.getName().toLowerCase().endsWith( ".rails_config" );
                    }
                    public String getDescription() {
                        return "Rails Config";
                    }
                }
        );

        int state = fc.showOpenDialog( null );
        if ( state == JFileChooser.APPROVE_OPTION )
        {
            File file = fc.getSelectedFile();
            Config.setActiveFilepath(file.getPath());
            saveConfig();
            setupButtonPanel();
        }
    }

    private void cancelConfig() {
        this.dispose();
    }

}
