package rails.ui.swing;

import java.awt.Color;
import java.awt.EventQueue;
import java.awt.GridBagConstraints;
import java.awt.GridBagLayout;
import java.awt.GridLayout;
import java.awt.Insets;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.awt.event.FocusEvent;
import java.awt.event.FocusListener;
import java.awt.event.ItemEvent;
import java.awt.event.ItemListener;
import java.awt.event.WindowAdapter;
import java.awt.event.WindowEvent;
import java.io.File;
import java.util.Map;
import java.util.List;

import javax.swing.BorderFactory;
import javax.swing.JButton;
import javax.swing.JColorChooser;
import javax.swing.JComboBox;
import javax.swing.JFileChooser;
import javax.swing.filechooser.FileFilter;
import javax.swing.JComponent;
import javax.swing.JFormattedTextField;
import javax.swing.JFrame;
import javax.swing.JLabel;
import javax.swing.JOptionPane;
import javax.swing.JPanel;
import javax.swing.JTabbedPane;
import javax.swing.SwingConstants;
import javax.swing.WindowConstants;
import javax.swing.border.Border;

import rails.game.ConfigurationException;
import rails.util.Config;
import rails.util.ConfigItem;
import rails.util.LocalText;
import rails.util.Util;

class ConfigWindow extends JFrame {
    private static final long serialVersionUID = 1L;

    private static final String CONFIG_EXTENSION = ".rails_config";
    private static final String CONFIG_DESCRIPTION = "Rails configuration files ( *.rails_config )";
    
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

        // hide on close and inform  
        this.setDefaultCloseOperation(WindowConstants.DO_NOTHING_ON_CLOSE);
        addWindowListener(new WindowAdapter() {
            @Override
            public void windowClosing(WindowEvent e) {
                cancelConfig();
            }
        });
    }
    
    public void init() {
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
                changeProfile((String)comboBoxUser.getSelectedItem());
            }
        }
        );
        profilePanel.add(comboBoxUser);

        JPanel buttonPanel = new JPanel(); 
        
        // button to create a new profile
        JButton newButton = new JButton(LocalText.getText("NEW"));
        newButton.addActionListener(
                new ActionListener() {
                    public void actionPerformed(ActionEvent arg0) {
                        newProfile();
                    }
                }
        );
        buttonPanel.add(newButton);
        
        // button to load a new profile
        JButton loadButton = new JButton(LocalText.getText("LOAD"));
        loadButton.addActionListener(
                new ActionListener() {
                    public void actionPerformed(ActionEvent arg0) {
                        loadProfile();
                    }
                }
        );
        buttonPanel.add(loadButton);
        
        profilePanel.add(buttonPanel);

    }
    
    private void setupConfigPane() {
        configPane.removeAll();
        
        Border etched = BorderFactory.createEtchedBorder();
        Border titled = BorderFactory.createTitledBorder(etched, LocalText.getText("CONFIG_SETTINGS"));
        configPane.setBorder(titled);
        
        Map<String, List<ConfigItem>> configPanels = Config.getConfigPanels();
        
        for (String panelName:configPanels.keySet()) {
            JPanel newPanel = new JPanel();
            newPanel.setLayout(new GridBagLayout());
            GridBagConstraints gbc = new GridBagConstraints();
            gbc.gridwidth = 1;
            gbc.gridheight = 1;
            gbc.gridx = 0;
            gbc.weightx = 0.8;
            gbc.weighty = 0.8;
            gbc.insets = new Insets(10,10,10,10);
            gbc.anchor = GridBagConstraints.WEST;
            for (ConfigItem item:configPanels.get(panelName)) {
                gbc.gridx = 0;
                defineElement(newPanel, item, gbc);
                gbc.gridy ++;
            }
            configPane.addTab(panelName, newPanel);
        }
    }
    
    
    private void addToGridBag(JComponent container, JComponent element, GridBagConstraints gbc) {
        container.add(element, gbc);
        gbc.gridx ++;
    }
    
    private void defineElement(JPanel panel, final ConfigItem item, GridBagConstraints gbc) {
        
        // standard components
        final String configValue = item.getCurrentValue();
        final String toolTip = item.helpText;

        // item label
        JLabel label = new JLabel(LocalText.getText("Config." + item.name));
        label.setToolTipText(toolTip);
        gbc.fill = GridBagConstraints.NONE;
        addToGridBag(panel, label, gbc);
        
        switch (item.type) {
        case LIST:
            final JComboBox comboBox = new JComboBox(item.allowedValues.toArray());
            comboBox.setSelectedItem(configValue);
            comboBox.setToolTipText(toolTip);
            comboBox.addFocusListener(new FocusListener() {
                public void focusGained(FocusEvent arg0) {
                    // do nothing
                }
                public void focusLost(FocusEvent arg0) {
                  item.setNewValue((String)comboBox.getSelectedItem());
                }
            }
            );
            gbc.fill = GridBagConstraints.HORIZONTAL;
            addToGridBag(panel, comboBox, gbc);
            break;
        case DIRECTORY:
            final JLabel dirLabel = new JLabel(configValue);
            dirLabel.setHorizontalAlignment(SwingConstants.CENTER);
            dirLabel.setToolTipText(toolTip);
            gbc.fill = GridBagConstraints.HORIZONTAL;
            addToGridBag(panel, dirLabel, gbc);
            JButton dirButton = new JButton("Choose...");
            dirButton.addActionListener(
                    new ActionListener() {
                        public void actionPerformed(ActionEvent e) {
                            JFileChooser fc = new JFileChooser(dirLabel.getText());
                            fc.setFileSelectionMode(JFileChooser.DIRECTORIES_ONLY);
                            int state = fc.showOpenDialog(ConfigWindow.this);
                            if ( state == JFileChooser.APPROVE_OPTION ){
                                File file = fc.getSelectedFile();
                                dirLabel.setText(file.getAbsolutePath());
                                item.setNewValue(file.getAbsolutePath());
                            }
                        }
                    }
            );
            gbc.fill = GridBagConstraints.NONE;
            addToGridBag(panel, dirButton, gbc);
            break;
        case COLOR:
            final JLabel colorLabel = new JLabel(configValue);
            Color selectedColor;
            try {
                selectedColor = Util.parseColour(configValue);
            } catch (ConfigurationException e) {
               selectedColor = Color.WHITE;
            }
            colorLabel.setOpaque(true);
            colorLabel.setHorizontalAlignment(SwingConstants.CENTER);
            colorLabel.setBackground(selectedColor);
            if (Util.isDark(selectedColor)) {
                colorLabel.setForeground(Color.WHITE);
            } else {
                colorLabel.setForeground(Color.BLACK);
            }
            colorLabel.setToolTipText(toolTip);
            gbc.fill = GridBagConstraints.HORIZONTAL;
            addToGridBag(panel, colorLabel, gbc);
            JButton colorButton = new JButton("Choose...");
            colorButton.addActionListener(
                    new ActionListener() {
                        public void actionPerformed(ActionEvent e) {
                            Color selectedColor=JColorChooser.showDialog(ConfigWindow.this, "", colorLabel.getBackground());
                            if (selectedColor == null) return;
                            String newValue = Integer.toHexString(selectedColor.getRGB()).substring(3); 
                            colorLabel.setText(newValue);
                            item.setNewValue(newValue);
                            colorLabel.setBackground(selectedColor);
                            if (Util.isDark(selectedColor)) {
                                colorLabel.setForeground(Color.WHITE);
                            } else {
                                colorLabel.setForeground(Color.BLACK);
                            }
                        }
                    }
            );
            gbc.fill = GridBagConstraints.NONE;
            addToGridBag(panel, colorButton, gbc);
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
            gbc.fill = GridBagConstraints.HORIZONTAL;
            addToGridBag(panel, textField, gbc);
            break;
        }
    }

    private void setupButtonPanel() {
        buttonPanel.removeAll();
        
        // saveas button
        JButton saveAsButton = new JButton(LocalText.getText("SAVEAS"));
        saveAsButton.addActionListener(
                new ActionListener() {
                    public void actionPerformed(ActionEvent arg0) {
                        ConfigWindow.this.saveAsConfig();
                    }
                }
        );
        buttonPanel.add(saveAsButton);

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
    
    private void newProfile() {
        String newProfile = JOptionPane.showInputDialog(ConfigWindow.this, LocalText.getText("CONFIG_NEW_MESSAGE"),
                LocalText.getText("CONFIG_NEW_TITLE"), JOptionPane.QUESTION_MESSAGE);
        if (Util.hasValue(newProfile)) {
            String defaultProfile = (String)JOptionPane.showInputDialog(ConfigWindow.this, LocalText.getText("CONFIG_DEFAULT_MESSAGE"),
                    LocalText.getText("CONFIG_DEFAULT_TITLE"), JOptionPane.QUESTION_MESSAGE, null, 
                    Config.getDefaultProfiles().toArray(), Config.getDefaultProfileSelection());
                Config.createUserProfile(newProfile, defaultProfile);
        }
        EventQueue.invokeLater(new Runnable() {
            public void run() {
                init();
                ConfigWindow.this.repaint();
            }
        });
    }
    
    private void loadProfile() {
        String directory = Config.get("save.directory");

        JFileChooser fc = new JFileChooser(directory);
        fc.setFileFilter(
                new FileFilter() {
                    public boolean accept( File f ){
                        return f.isDirectory() ||
                        f.getName().toLowerCase().endsWith( ".rails_config" );
                    }
                    public String getDescription() {
                        return CONFIG_DESCRIPTION;
                    }
                }
        );
        int state = fc.showOpenDialog(this);
        if ( state == JFileChooser.APPROVE_OPTION )
        {
            File file = fc.getSelectedFile();
            if (Config.loadProfileFromFile(file)) {
                changeProfile(Config.getActiveProfileName());
            }
        }
    }
    
    private void changeProfile(String profileName) {
        Config.changeActiveProfile(profileName);
        EventQueue.invokeLater(new Runnable() {
            public void run() {
                init();
                ConfigWindow.this.repaint();
            }
        });
    }
    
    private void saveConfig() {
        Config.updateProfile(); // transfer the configitem to the active profile
        Config.saveActiveProfile();
        JOptionPane.showMessageDialog(ConfigWindow.this, LocalText.getText("CONFIG_SAVE_MESSAGE"),
                LocalText.getText("CONFIG_SAVE_TITLE"), JOptionPane.INFORMATION_MESSAGE);
    }

    private void saveAsConfig() {
        String directory = Config.get("save.directory");
        String filepath;
        if (Util.hasValue(directory)) {
            filepath = directory + File.separator + Config.getActiveProfileName() + CONFIG_EXTENSION;
        } else {
            filepath = Config.getActiveProfileName() + CONFIG_EXTENSION;
        }
        JFileChooser fc = new JFileChooser();
        fc.setSelectedFile(new File(filepath));
        fc.setFileFilter(
                new FileFilter() {
                    public boolean accept( File f ){
                        return f.isDirectory() ||
                        f.getName().toLowerCase().endsWith( ".rails_config" );
                    }
                    public String getDescription() {
                        return CONFIG_DESCRIPTION;
                    }
                }
        );
        int state = fc.showSaveDialog(this);
        if ( state == JFileChooser.APPROVE_OPTION )
        {
            File file = fc.getSelectedFile();
            Config.setActiveFilepath(file.getPath());
            saveConfig();
            EventQueue.invokeLater(new Runnable() {
                public void run() {
                    setupButtonPanel();
                    ConfigWindow.this.pack();
                    ConfigWindow.this.repaint();
                }
            });
        }
    }

    private void cancelConfig() {
        StatusWindow.uncheckMenuItemBox(StatusWindow.CONFIG_CMD);
        this.setVisible(false);
    }

}
