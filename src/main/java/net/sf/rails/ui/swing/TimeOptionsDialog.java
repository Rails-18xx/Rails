package net.sf.rails.ui.swing;

import net.sf.rails.common.LocalText;
import net.sf.rails.game.GameManager;
import net.sf.rails.game.Player;
import net.sf.rails.game.GameManager.TimeConsequence;

import javax.swing.*;
import java.awt.*;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.util.List;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class TimeOptionsDialog extends JDialog implements ActionListener {

    private static final long serialVersionUID = 1L;
    
    private static final Logger logger = LoggerFactory.getLogger(TimeOptionsDialog.class);

    private final GameManager gameManager;
    private final List<Player> players;

    private final JCheckBox enableTimeManagement;
    private final JSpinner startingTimeSpinner;
    private final JSpinner srIncrementSpinner;
    
    private final JSpinner yellowIncrementSpinner;
    private final JSpinner greenIncrementSpinner;
    private final JSpinner brownIncrementSpinner;
    
    private final JSpinner undoPenaltySpinner;      // Minor (Self-Correction)
    private final JSpinner majorUndoPenaltySpinner; // Major (Disruption)
    
    private final JSpinner operatorMultiplierSpinner;
    private final JComboBox<String> operatorNameComboBox;
    private final JComboBox<TimeConsequence> consequenceComboBox;
    private final JButton okButton = new JButton("OK");
    private final JButton cancelButton = new JButton(LocalText.getText("Cancel"));
    
    private final JButton hardcorePresetButton = new JButton("90m (Hardcore)");
    private final JButton champPresetButton = new JButton("100m (Championship)");
    private final JButton standardPresetButton = new JButton("120m (Standard)");

    // Constants for Presets (Seconds)
    // 90m Hardcore
    private static final int HC_START = 300; 
    private static final int HC_YEL = 30;
    private static final int HC_GRN = 60;
    private static final int HC_BRN = 30;

    // 100m Championship (Recommended)
    private static final int CH_START = 400;
    private static final int CH_YEL = 40;
    private static final int CH_GRN = 80;
    private static final int CH_BRN = 40;

    // 120m Standard
    private static final int ST_START = 500; 
    private static final int ST_YEL = 50;
    private static final int ST_GRN = 100;
    private static final int ST_BRN = 50;

    private static final int MINOR_PENALTY_PRESET = 0; 
    private static final int MAJOR_PENALTY_PRESET = 0; 
    private static final double MODERATOR_BONUS = 1.1;

    public TimeOptionsDialog(Dialog owner, GameManager gm) {
        super(owner, LocalText.getText("TIME_SETTINGS"), true);

        this.gameManager = gm;
        this.players = (gm != null && gm.getRoot() != null && gm.getRoot().getPlayerManager() != null)
                ? gm.getRoot().getPlayerManager().getPlayers()
                : List.of();

        enableTimeManagement = new JCheckBox(LocalText.getText("EnableTimeManagement"), gm.isTimeManagementEnabled());

        int startingMinutes = gm.getTimeMgmtStartingSeconds() / 60;
        startingTimeSpinner = new JSpinner(new SpinnerNumberModel(startingMinutes, 1, 999, 1));

        srIncrementSpinner = new JSpinner(new SpinnerNumberModel(gm.getTimeMgmtShareRoundIncrement(), 0, 3600, 5));
        
        yellowIncrementSpinner = new JSpinner(new SpinnerNumberModel(gm.getTimeMgmtYellowIncrement(), 0, 600, 5));
        greenIncrementSpinner = new JSpinner(new SpinnerNumberModel(gm.getTimeMgmtGreenIncrement(), 0, 600, 5));
        brownIncrementSpinner = new JSpinner(new SpinnerNumberModel(gm.getTimeMgmtBrownIncrement(), 0, 600, 5));

        undoPenaltySpinner = new JSpinner(new SpinnerNumberModel(gm.getTimeMgmtUndoPenalty(), 0, 3600, 5));
        majorUndoPenaltySpinner = new JSpinner(new SpinnerNumberModel(gm.getTimeMgmtMajorUndoPenalty(), 0, 3600, 5));

        operatorMultiplierSpinner = new JSpinner(new SpinnerNumberModel(
                gm.getTimeMgmtOperatorMultiplier(), 
                1.0, 
                10.0, 
                0.1));

        JSpinner.NumberEditor editor = new JSpinner.NumberEditor(operatorMultiplierSpinner, "0.0#");
        operatorMultiplierSpinner.setEditor(editor);

        String[] playerNames = new String[players.size() + 1];
        playerNames[0] = LocalText.getText("NoOperator");
        int selectedIndex = 0;
        for (int i = 0; i < players.size(); i++) {
            String name = players.get(i).getId();
            playerNames[i + 1] = name;
            if (name.equals(gm.getTimeMgmtOperatorName())) {
                selectedIndex = i + 1;
            }
        }
        operatorNameComboBox = new JComboBox<>(playerNames);
        operatorNameComboBox.setSelectedIndex(selectedIndex);

        consequenceComboBox = new JComboBox<>(TimeConsequence.values());
        consequenceComboBox.setSelectedItem(gm.getTimeConsequence());

        JPanel contentPanel = new JPanel(new GridBagLayout());
        contentPanel.setBorder(BorderFactory.createEmptyBorder(10, 10, 10, 10));
        GridBagConstraints gbc = new GridBagConstraints();
        gbc.insets = new Insets(5, 5, 5, 5);
        gbc.fill = GridBagConstraints.HORIZONTAL;

        JPanel presetPanel = new JPanel(new FlowLayout(FlowLayout.CENTER));
        hardcorePresetButton.addActionListener(this);
        champPresetButton.addActionListener(this);
        standardPresetButton.addActionListener(this);
        presetPanel.add(hardcorePresetButton);
        presetPanel.add(champPresetButton);
        presetPanel.add(standardPresetButton);
        
        gbc.gridx = 0; gbc.gridy = 0; gbc.gridwidth = 2;
        contentPanel.add(presetPanel, gbc);

        gbc.gridy = 1;
        contentPanel.add(enableTimeManagement, gbc);

        gbc.gridwidth = 1;
        gbc.weightx = 0.5;

        int row = 2;

        gbc.gridy = row; gbc.gridx = 0; contentPanel.add(new JLabel(LocalText.getText("StartingTime") + " (min):"), gbc);
        gbc.gridx = 1; contentPanel.add(startingTimeSpinner, gbc);

        gbc.gridy = ++row; gbc.gridx = 0; contentPanel.add(new JLabel(LocalText.getText("SRIncrement") + ":"), gbc);
        gbc.gridx = 1; contentPanel.add(srIncrementSpinner, gbc);

        gbc.gridy = ++row; gbc.gridx = 0; contentPanel.add(new JLabel("Yellow Phase Increment:"), gbc); 
        gbc.gridx = 1; contentPanel.add(yellowIncrementSpinner, gbc);

        gbc.gridy = ++row; gbc.gridx = 0; contentPanel.add(new JLabel("Green Phase Increment:"), gbc); 
        gbc.gridx = 1; contentPanel.add(greenIncrementSpinner, gbc);

        gbc.gridy = ++row; gbc.gridx = 0; contentPanel.add(new JLabel("Brown Phase Increment:"), gbc); 
        gbc.gridx = 1; contentPanel.add(brownIncrementSpinner, gbc);

        gbc.gridy = ++row; gbc.gridx = 0; contentPanel.add(new JLabel("Penalty (Self-Correction):"), gbc);
        gbc.gridx = 1; contentPanel.add(undoPenaltySpinner, gbc);

        gbc.gridy = ++row; gbc.gridx = 0; contentPanel.add(new JLabel("Penalty (Disruption):"), gbc);
        gbc.gridx = 1; contentPanel.add(majorUndoPenaltySpinner, gbc);

        gbc.gridy = ++row; gbc.gridx = 0; contentPanel.add(new JLabel(LocalText.getText("OperatorMultiplier") + ":"), gbc);
        gbc.gridx = 1; contentPanel.add(operatorMultiplierSpinner, gbc);

        gbc.gridy = ++row; gbc.gridx = 0; contentPanel.add(new JLabel(LocalText.getText("OperatorName") + ":"), gbc);
        gbc.gridx = 1; contentPanel.add(operatorNameComboBox, gbc);

        gbc.gridy = ++row; gbc.gridx = 0; contentPanel.add(new JLabel(LocalText.getText("TimeConsequenceLabel") + ":"), gbc);
        gbc.gridx = 1; contentPanel.add(consequenceComboBox, gbc);

        JPanel buttonPanel = new JPanel(new FlowLayout(FlowLayout.RIGHT));
        okButton.addActionListener(this);
        cancelButton.addActionListener(this);
        buttonPanel.add(okButton);
        buttonPanel.add(cancelButton);

        getContentPane().setLayout(new BorderLayout());
        getContentPane().add(contentPanel, BorderLayout.CENTER);
        getContentPane().add(buttonPanel, BorderLayout.SOUTH);

        pack();
        setLocationRelativeTo(owner);
    }

    @Override
    public void actionPerformed(ActionEvent e) {
        Object source = e.getSource();
        if (source == okButton) {
            saveSettings();
            dispose();
        } else if (source == cancelButton) {
            dispose();
        } else if (source == hardcorePresetButton) {
            applyPreset(HC_START, 30, HC_YEL, HC_GRN, HC_BRN);
        } else if (source == champPresetButton) {
            applyPreset(CH_START, 35, CH_YEL, CH_GRN, CH_BRN);
        } else if (source == standardPresetButton) {
            applyPreset(ST_START, 40, ST_YEL, ST_GRN, ST_BRN);
        }
    }

    private void applyPreset(int startSec, int srInc, int yellow, int green, int brown) {
        enableTimeManagement.setSelected(true);
        startingTimeSpinner.setValue(startSec / 60);
        srIncrementSpinner.setValue(srInc);
        
        yellowIncrementSpinner.setValue(yellow);
        greenIncrementSpinner.setValue(green);
        brownIncrementSpinner.setValue(brown);
        
        // Defaults per policy
        undoPenaltySpinner.setValue(MINOR_PENALTY_PRESET); 
        majorUndoPenaltySpinner.setValue(MAJOR_PENALTY_PRESET);
        operatorMultiplierSpinner.setValue(MODERATOR_BONUS);
    }

    private void saveSettings() {
        gameManager.setTimeManagementEnabled(enableTimeManagement.isSelected());
        
        int startingMinutes = (Integer) startingTimeSpinner.getValue();
        gameManager.setTimeMgmtStartingSeconds(startingMinutes * 60);
        
        gameManager.setTimeMgmtShareRoundIncrement((Integer) srIncrementSpinner.getValue());
        
        gameManager.setTimeMgmtYellowIncrement((Integer) yellowIncrementSpinner.getValue());
        gameManager.setTimeMgmtGreenIncrement((Integer) greenIncrementSpinner.getValue());
        gameManager.setTimeMgmtBrownIncrement((Integer) brownIncrementSpinner.getValue());
        
        gameManager.setTimeMgmtUndoPenalty((Integer) undoPenaltySpinner.getValue());
        gameManager.setTimeMgmtMajorUndoPenalty((Integer) majorUndoPenaltySpinner.getValue());

        Double multiplier = (Double) operatorMultiplierSpinner.getValue();
        gameManager.setTimeMgmtOperatorMultiplier(multiplier);

        String selectedName = (String) operatorNameComboBox.getSelectedItem();
        String nameToSave = "";
        if (selectedName != null && !selectedName.equals(LocalText.getText("NoOperator"))) {
            nameToSave = selectedName;
        }
        
        gameManager.setTimeMgmtOperatorName(nameToSave);
        
        TimeConsequence selectedConsequence = (TimeConsequence) consequenceComboBox.getSelectedItem();
        gameManager.setTimeConsequence(selectedConsequence);
    }
}