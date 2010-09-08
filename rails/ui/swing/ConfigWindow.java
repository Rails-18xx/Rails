package rails.ui.swing;

import java.awt.Color;
import java.awt.EventQueue;
import java.awt.Font;
import java.awt.GraphicsEnvironment;
import java.awt.GridBagConstraints;
import java.awt.GridBagLayout;
import java.awt.Insets;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.awt.event.FocusEvent;
import java.awt.event.FocusListener;
import java.awt.event.ItemEvent;
import java.awt.event.ItemListener;
import java.awt.event.MouseEvent;
import java.awt.event.MouseListener;
import java.awt.event.WindowAdapter;
import java.awt.event.WindowEvent;
import java.beans.PropertyChangeEvent;
import java.beans.PropertyChangeListener;
import java.io.File;
import java.util.Map;
import java.util.List;

import javax.swing.BorderFactory;
import javax.swing.JButton;
import javax.swing.JColorChooser;
import javax.swing.JComboBox;
import javax.swing.JFileChooser;
import javax.swing.filechooser.FileFilter;
import javax.swing.JCheckBox;
import javax.swing.JComponent;
import javax.swing.JDialog;
import javax.swing.JFormattedTextField;
import javax.swing.JFrame;
import javax.swing.JLabel;
import javax.swing.JOptionPane;
import javax.swing.JPanel;
import javax.swing.JSpinner;
import javax.swing.JTabbedPane;
import javax.swing.SpinnerNumberModel;
import javax.swing.SwingConstants;
import javax.swing.UIManager;
import javax.swing.WindowConstants;
import javax.swing.border.Border;

import rails.game.ConfigurationException;
import rails.ui.swing.elements.RailsIcon;
import rails.util.Config;
import rails.util.ConfigItem;
import rails.util.LocalText;
import rails.util.Util;

class ConfigWindow extends JFrame {
    private static final long serialVersionUID = 1L;

    private static final String CONFIG_EXTENSION = ".rails_config";
    private static final String LEGACY_EXTENSION = ".properties";
    private static final String CONFIG_DESCRIPTION = "Rails configuration files ( *.rails_config, *.properties)";
    
    private JPanel profilePanel;
    private JTabbedPane configPane;
    private JPanel buttonPanel;
    
    private boolean fromStatusWindow;
    
    ConfigWindow(boolean fromStatusWindow) {
        // store for handling of close
        this.fromStatusWindow = fromStatusWindow;

        // JFrame properties
        setTitle(LocalText.getText("CONFIG_WINDOW_TITLE"));
        
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
                closeConfig(false);
            }
        });
    }
    
    public void init() {
        setupProfilePanel();
        setupConfigPane();
        setupButtonPanel();
        this.pack();
        setSize(600,400);
    }

    private void setupProfilePanel() {
        profilePanel.removeAll();

        String activeProfile = Config.getActiveProfileName();
        String defaultProfile = Config.getDefaultProfileName();
        Border etched = BorderFactory.createEtchedBorder();
        Border titled = BorderFactory.createTitledBorder(etched, LocalText.getText("CONFIG_CURRENT_PROFILE", activeProfile, defaultProfile));
        profilePanel.setBorder(titled);
        
        JLabel userLabel = new JLabel(LocalText.getText("CONFIG_SELECT_PROFILE"));
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
        JButton importButton = new JButton(LocalText.getText("IMPORT"));
        importButton.addActionListener(
                new ActionListener() {
                    public void actionPerformed(ActionEvent arg0) {
                        importProfile();
                    }
                }
        );
        buttonPanel.add(importButton);
        
        // saveas button
//        JButton saveAsButton = new JButton(LocalText.getText("SAVEAS"));
//        saveAsButton.addActionListener(
//                new ActionListener() {
//                    public void actionPerformed(ActionEvent arg0) {
//                        ConfigWindow.this.saveAsConfig();
//                    }
//                }
//        );
//        buttonPanel.add(saveAsButton);
        
        profilePanel.add(buttonPanel);

    }
    
    
    
    private void setupConfigPane() {
        configPane.removeAll();
        
        Border etched = BorderFactory.createEtchedBorder();
        Border titled = BorderFactory.createTitledBorder(etched, LocalText.getText("CONFIG_SETTINGS"));
        configPane.setBorder(titled);
        
        Map<String, List<ConfigItem>> configSections = Config.getConfigSections();
        int maxElements = Config.getMaxElementsInPanels();
       
        for (String sectionName:configSections.keySet()) {
            JPanel newPanel = new JPanel();
            newPanel.setLayout(new GridBagLayout());
            GridBagConstraints gbc = new GridBagConstraints();
            gbc.gridwidth = 1;
            gbc.gridheight = 1;
            gbc.weightx = 0.8;
            gbc.weighty = 0.8;
            gbc.insets = new Insets(5,5,5,5);
            gbc.anchor = GridBagConstraints.WEST;
            for (ConfigItem item:configSections.get(sectionName)) {
                gbc.gridx = 0;
                gbc.gridy++;
                defineElement(newPanel, item, gbc);
            }
            // fill up to maxElements
            gbc.gridx = 0;
            while (++gbc.gridy < maxElements) {
                JLabel emptyLabel = new JLabel("");
                newPanel.add(emptyLabel, gbc);
            }
            configPane.addTab(LocalText.getText("Config.section." + sectionName), newPanel);
        }
    }
    
    
    private void addToGridBag(JComponent container, JComponent element, GridBagConstraints gbc) {
        container.add(element, gbc);
        gbc.gridx ++;
    }
    
    private void addEmptyLabel(JComponent container, GridBagConstraints gbc) {
        JLabel label = new JLabel("");
        addToGridBag(container, label, gbc );
    }
    
    private void defineElement(JPanel panel, final ConfigItem item, GridBagConstraints gbc) {
        
        // current value (based on current changes and properties)
        String configValue = item.getCurrentValue();

        // item label, toolTip and infoText
        final String itemLabel = LocalText.getText("Config.label." + item.name);
        final String toolTip = LocalText.getTextWithDefault("Config.toolTip." + item.name, null);
        final String infoText = LocalText.getTextWithDefault("Config.infoText." + item.name, null);
  
        // define label        
        JLabel label = new JLabel(itemLabel);
        label.setToolTipText(toolTip);
        gbc.fill = GridBagConstraints.NONE;
        addToGridBag(panel, label, gbc);
        
        switch (item.type) {
        case BOOLEAN:
            final JCheckBox checkBox = new JCheckBox();
            boolean selected = Util.parseBoolean(configValue);
            checkBox.setSelected(selected);
            checkBox.addFocusListener(new FocusListener() {
                public void focusGained(FocusEvent arg0) {
                    // do nothing
                }
                public void focusLost(FocusEvent arg0) {
                    if (checkBox.isSelected()) {
                        item.setNewValue("yes");
                    } else {
                        item.setNewValue("no");
                    }
                }
            }
            );
            gbc.fill = GridBagConstraints.HORIZONTAL;
            addToGridBag(panel, checkBox, gbc);
            break;
        case PERCENT: // percent uses a spinner with 5 changes
        case INTEGER:
            int spinnerStepSize;
            final int spinnerMultiple;
            if (item.type == ConfigItem.ConfigType.PERCENT) {
                spinnerStepSize = 5;
                spinnerMultiple = 100;
            } else {
                spinnerStepSize = 1;
                spinnerMultiple = 1;
            }
            int spinnerValue;
            try {
                spinnerValue = (int)Math.round(Double.parseDouble(configValue) * spinnerMultiple);
            } catch (NumberFormatException e) {
                spinnerValue = 0;
            }
            final JSpinner spinner = new JSpinner(new SpinnerNumberModel
                   (spinnerValue, Integer.MIN_VALUE, Integer.MAX_VALUE, spinnerStepSize));
            ((JSpinner.DefaultEditor)spinner.getEditor()).getTextField().
                addFocusListener(new FocusListener() {
                public void focusGained(FocusEvent arg0) {
                    // do nothing
                }
                public void focusLost(FocusEvent arg0) {
                    Integer value = (Integer)spinner.getValue();
                    if (item.type == ConfigItem.ConfigType.PERCENT) {
                        Double adjValue = (double)value / spinnerMultiple;
                        item.setNewValue(adjValue.toString());
                    } else {
                        item.setNewValue(value.toString());
                    }
                    
                }
            }
            );
            gbc.fill = GridBagConstraints.HORIZONTAL;
            addToGridBag(panel, spinner, gbc);
            addEmptyLabel(panel, gbc);
        break;
        case FONT: // fonts are a special list
            if (!Util.hasValue(configValue)) {
                configValue = ((Font)UIManager.getDefaults().get("Label.font")).getFamily();
            }
        case LIST:
            String[] allowedValues; 
            if (item.type == ConfigItem.ConfigType.FONT) {
                allowedValues = GraphicsEnvironment.getLocalGraphicsEnvironment().
                getAvailableFontFamilyNames(); 
            } else {
                allowedValues = (String[])item.allowedValues.toArray();
            }
            final JComboBox comboBox = new JComboBox(allowedValues);
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
            addEmptyLabel(panel, gbc);
            break;
        case DIRECTORY:
        case FILE:
            final JLabel dirLabel = new JLabel(configValue);
            dirLabel.setHorizontalAlignment(SwingConstants.CENTER);
            dirLabel.setToolTipText(toolTip);
            gbc.fill = GridBagConstraints.HORIZONTAL;
            addToGridBag(panel, dirLabel, gbc);
            JButton dirButton = new JButton("Choose...");
            dirButton.addActionListener(
                    new ActionListener() {
                        public void actionPerformed(ActionEvent e) {
                            JFileChooser fc = new JFileChooser();
                            if (item.type == ConfigItem.ConfigType.DIRECTORY) { 
                                fc.setFileSelectionMode(JFileChooser.DIRECTORIES_ONLY);
                            } else {
                                fc.setFileSelectionMode(JFileChooser.FILES_ONLY);
                            }
                            fc.setSelectedFile(new File(dirLabel.getText()));
                            int state = fc.showOpenDialog(ConfigWindow.this);
                            if ( state == JFileChooser.APPROVE_OPTION ){
                                File file = fc.getSelectedFile();
                                dirLabel.setText(file.getPath());
                                item.setNewValue(file.getPath());
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
                            String newValue = Integer.toHexString(selectedColor.getRGB()).substring(2); 
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
            addEmptyLabel(panel, gbc);
            break;
        }
        // add info icon for infoText 
        if (infoText != null) {
            JLabel infoIcon = new JLabel(RailsIcon.INFO.icon);
            infoIcon.addMouseListener(new MouseListener() {
                public void mousePressed(MouseEvent e) {
                    final JDialog dialog = new JDialog(ConfigWindow.this, false);
                    final JOptionPane optionPane = new JOptionPane();
                    optionPane.setMessageType(JOptionPane.INFORMATION_MESSAGE);
                    optionPane.setMessage(infoText);
                    optionPane.addPropertyChangeListener(JOptionPane.VALUE_PROPERTY,
                            new PropertyChangeListener() {
                                public void propertyChange(PropertyChangeEvent e) {
                                       dialog.dispose();
                                   }
                            }
                    );
                    dialog.setTitle(LocalText.getText("CONFIG_INFO_TITLE", itemLabel));
                    dialog.getContentPane().add(optionPane);
                    dialog.pack();
                    dialog.setVisible(true);
                }

                public void mouseClicked(MouseEvent e) {
                }
                public void mouseReleased(MouseEvent e) {
                }

                public void mouseEntered(MouseEvent e) {
                }

                public void mouseExited(MouseEvent e) {
                }
            });
            gbc.fill = GridBagConstraints.NONE;
            addToGridBag(panel, infoIcon, gbc);
            addEmptyLabel(panel, gbc);
        }
    }

    private void setupButtonPanel() {
        buttonPanel.removeAll();

        String activeFilePath = Config.getActiveFilepath();
        Border etched = BorderFactory.createEtchedBorder();
        Border titled = BorderFactory.createTitledBorder(etched, LocalText.getText("CONFIG_CURRENT_PATH", activeFilePath));
        buttonPanel.setBorder(titled);
        
        // save button
        JButton saveButton = new JButton(LocalText.getText("SAVE_AND_APPLY"));
        saveButton.addActionListener(
                new ActionListener() {
                    public void actionPerformed(ActionEvent arg0) {
                        if (Config.isFilePathDefined()) {
                            ConfigWindow.this.saveConfig();
                        } else {
                            ConfigWindow.this.saveAsConfig();
                        }
                    }
                }
        );
        buttonPanel.add(saveButton);

//        JButton applyButton = new JButton(LocalText.getText("APPLY"));
//        applyButton.addActionListener(
//                new ActionListener() {
//                    public void actionPerformed(ActionEvent arg0) {
//                        ConfigWindow.this.applyConfig();
//                    }
//                }
//        );
//        buttonPanel.add(applyButton);
        

        JButton cancelButton = new JButton(LocalText.getText("CANCEL"));
        cancelButton.addActionListener(
                new ActionListener() {
                    public void actionPerformed(ActionEvent arg0) {
                        ConfigWindow.this.closeConfig(true);
                    }
                }
        );
        buttonPanel.add(cancelButton);
        
    }
    
    private void newProfile() {
        List<String> allProfileNames = Config.getAllProfiles();
        String newProfile = null;
        do {
            newProfile = JOptionPane.showInputDialog(ConfigWindow.this, LocalText.getText("CONFIG_NEW_MESSAGE"),
                LocalText.getText("CONFIG_NEW_TITLE"), JOptionPane.QUESTION_MESSAGE);
        } while (allProfileNames.contains(newProfile)); 
        
        if (Util.hasValue(newProfile)) {
            String defaultProfile = (String)JOptionPane.showInputDialog(ConfigWindow.this, LocalText.getText("CONFIG_DEFAULT_MESSAGE"),
                    LocalText.getText("CONFIG_DEFAULT_TITLE"), JOptionPane.QUESTION_MESSAGE, null, 
                    Config.getDefaultProfiles(true).toArray(), Config.getDefaultProfileSelection());
            if (Util.hasValue(defaultProfile)) {
                Config.createUserProfile(newProfile, defaultProfile);
            }
        }
        EventQueue.invokeLater(new Runnable() {
            public void run() {
                init();
                ConfigWindow.this.repaint();
            }
        });
    }
    
    private void importProfile() {
        String directory = Config.get("save.directory");

        JFileChooser fc = new JFileChooser(directory);
        fc.setFileFilter(
                new FileFilter() {
                    public boolean accept( File f ){
                        return f.isDirectory() ||
                        f.getName().toLowerCase().endsWith( CONFIG_EXTENSION) ||
                        f.getName().toLowerCase().endsWith( LEGACY_EXTENSION)
                        ;
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
            if (Config.importProfileFromFile(file)) {
                repaintLater();
            } else {
                JOptionPane.showMessageDialog(ConfigWindow.this, LocalText.getText("CONFIG_LOAD_ERROR_MESSAGE", Config.getActiveProfileName()),
                        LocalText.getText("CONFIG_LOAD_TITLE"), JOptionPane.ERROR_MESSAGE);
            }
        }
    }
    
    private void changeProfile(String profileName) {
        Config.changeActiveProfile(profileName);
        repaintLater();
    }
    
    private void repaintLater() {
        EventQueue.invokeLater(new Runnable() {
            public void run() {
                init();
                ConfigWindow.this.repaint();
            }
        });
    }
    
    private boolean saveConfig() {
        Config.updateProfile(fromStatusWindow); // transfer the configitem to the active profile

        if (fromStatusWindow) {
            JOptionPane.showMessageDialog(ConfigWindow.this, LocalText.getText("CONFIG_APPLY_MESSAGE"),
                LocalText.getText("CONFIG_APPLY_TITLE"), JOptionPane.INFORMATION_MESSAGE);
        }

        if (Config.saveActiveProfile()) {
            JOptionPane.showMessageDialog(ConfigWindow.this, LocalText.getText("CONFIG_SAVE_CONFIRM_MESSAGE", Config.getActiveProfileName()),
                LocalText.getText("CONFIG_SAVE_TITLE"), JOptionPane.INFORMATION_MESSAGE);
            return true;
        } else {
            JOptionPane.showMessageDialog(ConfigWindow.this, LocalText.getText("CONFIG_SAVE_ERROR_MESSAGE", Config.getActiveProfileName()),
                    LocalText.getText("CONFIG_SAVE_TITLE"), JOptionPane.ERROR_MESSAGE);
            return false;
        }
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
        if ( state == JFileChooser.APPROVE_OPTION ) {
            File file = fc.getSelectedFile();
            if (!Config.setActiveFilepath(file.getPath())) {
                JOptionPane.showMessageDialog(ConfigWindow.this, LocalText.getText("CONFIG_PROFILE_ERROR_MESSAGE", Config.getActiveProfileName()),
                        LocalText.getText("CONFIG_SAVE_TITLE"), JOptionPane.ERROR_MESSAGE);
            }
            saveConfig();
            // update panel for file path
            EventQueue.invokeLater(new Runnable() {
                public void run() {
                    setupButtonPanel();
                    ConfigWindow.this.pack();
                    ConfigWindow.this.repaint();
                }
            });
        }
    }
    
//    private void applyConfig() {
//        Config.updateProfile(fromStatusWindow); // transfer the configitem to the active profile
//        JOptionPane.showMessageDialog(ConfigWindow.this, LocalText.getText("CONFIG_APPLY_MESSAGE"),
//                LocalText.getText("CONFIG_APPLY_TITLE"), JOptionPane.INFORMATION_MESSAGE);
//    }
    
    private void closeConfig(boolean cancel) {
        if (cancel) Config.revertProfile();
        this.setVisible(false);
        if (fromStatusWindow) {
            StatusWindow.uncheckMenuItemBox(StatusWindow.CONFIG_CMD);
        }
    }

}
