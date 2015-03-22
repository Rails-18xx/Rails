package net.sf.rails.ui.swing;

import java.awt.*;
import java.awt.event.*;
import java.util.*;
import java.util.List;

import javax.swing.*;

import net.sf.rails.common.Config;
import net.sf.rails.common.ConfigManager;
import net.sf.rails.common.GameInfo;
import net.sf.rails.common.GameOption;
import net.sf.rails.common.GameOptionsSet;
import net.sf.rails.common.LocalText;

import com.google.common.collect.ImmutableList;
import com.google.common.collect.Lists;
import com.google.common.collect.Maps;


/**
 * The Game Setup Window displays the first window presented to the user. This
 * window contains all of the options available for starting a new rails.game.
 */
public class GameSetupWindow extends JDialog {
    private static final long serialVersionUID = 1L;

    private final JPanel gameListPane = new JPanel();
    private final JPanel playersPane = new JPanel();
    private final JPanel buttonPane = new JPanel();
    private final JPanel optionsPane = new JPanel();
    
    private final JButton newButton = new JButton(LocalText.getText("NewGame"));
    private final JButton loadButton = new JButton(LocalText.getText("LoadGame"));
    private final JButton recentButton = new JButton(LocalText.getText("LoadRecentGame"));
    private final JButton recoveryButton = new JButton(LocalText.getText("RecoverGame"));
    private final JButton quitButton = new JButton(LocalText.getText("QUIT"));
    private final JButton optionButton = new JButton(LocalText.getText("OPTIONS"));
    private final JButton infoButton = new JButton(LocalText.getText("INFO"));
    private final JButton creditsButton = new JButton(LocalText.getText("CREDITS"));
    private final JButton configureButton= new JButton(LocalText.getText("CONFIG"));
    private final JButton randomizeButton = new JButton(LocalText.getText("RandomizePlayers"));
    
    private final JComboBox<String> configureBox = new JComboBox<String>();
    private final JComboBox<String> gameNameBox = new JComboBox<String>();
    
    private static class PlayerInfo {
        private final JLabel number = new JLabel();
        private final JTextField name = new JTextField();
    }
    private final List<PlayerInfo> players = Lists.newArrayList();
    
    private final SortedMap<GameOption, JComponent> optionComponents =
            Maps.newTreeMap();
    
    private final GameSetupController controller;
    
    public GameSetupWindow(GameSetupController controller) {
        super();
        
        this.controller = controller;
        initialize();
        initGridBag();
        GameInfo selectedGame = initGameList();
        initPlayersPane(selectedGame);
        initConfigBox();
        this.pack();
        this.setVisible(true);
    }

    private void initialize() {
        newButton.setMnemonic(KeyEvent.VK_N);
        loadButton.setMnemonic(KeyEvent.VK_L);
        recentButton.setMnemonic(KeyEvent.VK_D);
        recoveryButton.setMnemonic(KeyEvent.VK_R);
        quitButton.setMnemonic(KeyEvent.VK_Q);
        optionButton.setMnemonic(KeyEvent.VK_O);
        infoButton.setMnemonic(KeyEvent.VK_G);
        creditsButton.setMnemonic(KeyEvent.VK_E);
        configureButton.setMnemonic(KeyEvent.VK_C);
        randomizeButton.setMnemonic(KeyEvent.VK_R);

        this.getContentPane().setLayout(new GridBagLayout());
        this.setTitle("Rails: New Game");
        this.setDefaultCloseOperation(JFrame.DISPOSE_ON_CLOSE);

        gameListPane.add(new JLabel("Available Games:"));
        gameListPane.add(gameNameBox);
        gameListPane.add(optionButton); // empty slot
        gameListPane.setLayout(new GridLayout(2, 2));
        gameListPane.setBorder(BorderFactory.createLoweredBevelBorder());

        newButton.addActionListener(controller.newAction);
        loadButton.addActionListener(controller.loadAction);
        recentButton.addActionListener(controller.recentAction);
        recoveryButton.addActionListener(controller.recoveryAction);
        quitButton.addActionListener(controller.quitAction);
        optionButton.addActionListener(controller.optionPanelAction);
        infoButton.addActionListener(controller.infoAction);
        creditsButton.addActionListener(controller.creditsAction);
        configureButton.addActionListener(controller.configureAction);
        randomizeButton.addActionListener(controller.randomizeAction);

        buttonPane.add(configureButton);
        buttonPane.add(configureBox);
        buttonPane.add(newButton);
        buttonPane.add(loadButton);
        buttonPane.add(recentButton);
        recoveryButton.setEnabled(Config.get("save.recovery.active", "no").equalsIgnoreCase("yes"));
        buttonPane.add(recoveryButton);

        buttonPane.add(infoButton);
        buttonPane.add(quitButton);
        buttonPane.add(creditsButton);

        buttonPane.setLayout(new GridLayout(0, 2));
        buttonPane.setBorder(BorderFactory.createLoweredBevelBorder());

        optionsPane.setLayout(new FlowLayout());
        optionsPane.setVisible(false);
    }

    private void initGridBag() {
        GridBagConstraints gc;
        
        gc = new GridBagConstraints();
        gc.gridx = 0;
        gc.gridy = 0;
        gc.weightx = 0;
        gc.weighty = 0;
        gc.gridwidth = 1;
        gc.gridheight = 1;
        gc.ipadx = 0;
        gc.ipady = 0;
        gc.anchor = GridBagConstraints.CENTER;
        gc.fill = GridBagConstraints.HORIZONTAL;
        gc.insets = new Insets(0, 0, 0, 0);
        this.getContentPane().add(playersPane, gc);

        gc = new GridBagConstraints();
        gc.gridx = 0;
        gc.gridy = 1;
        gc.weightx = 0;
        gc.weighty = 0;
        gc.gridwidth = 1;
        gc.gridheight = 1;
        gc.ipadx = 0;
        gc.ipady = 0;
        gc.anchor = GridBagConstraints.CENTER;
        gc.fill = GridBagConstraints.BOTH;
        gc.insets = new Insets(0, 0, 0, 0);
        this.getContentPane().add(gameListPane, gc);

        gc = new GridBagConstraints();
        gc.gridx = 0;
        gc.gridy = 2;
        gc.weightx = 0;
        gc.weighty = 0;
        gc.gridwidth = 1;
        gc.gridheight = 1;
        gc.ipadx = 0;
        gc.ipady = 0;
        gc.anchor = GridBagConstraints.CENTER;
        gc.fill = GridBagConstraints.BOTH;
        gc.insets = new Insets(0, 0, 0, 0);
        this.getContentPane().add(optionsPane, gc);

        gc = new GridBagConstraints();
        gc.gridx = 0;
        gc.gridy = 3;
        gc.weightx = 0;
        gc.weighty = 0;
        gc.gridwidth = 1;
        gc.gridheight = 1;
        gc.ipadx = 0;
        gc.ipady = 0;
        gc.anchor = GridBagConstraints.CENTER;
        gc.fill = GridBagConstraints.HORIZONTAL;
        gc.insets = new Insets(0, 0, 0, 0);
        this.getContentPane().add(buttonPane, gc);
    }

    private GameInfo initGameList() {
        GameInfo selectedGame = null;
        for (GameInfo game : controller.getGameList()) {
            String gameName = game.getName();
            String gameText = gameName + " - " + game.getNote();
            gameNameBox.addItem(gameText);
            if (game.equals(controller.getDefaultGame())) {
                gameNameBox.setSelectedItem(gameText);
                selectedGame = game;
            }
        }
        gameNameBox.addActionListener(controller.gameAction);
        return selectedGame;
    }

    private void initConfigBox() {
        final ConfigManager cm = ConfigManager.getInstance();
        for (String profile:cm.getProfiles()) {
            configureBox.addItem(profile);
        }
        configureBox.setSelectedItem(cm.getActiveProfile());

        configureBox.addItemListener(new ItemListener() {
            public void itemStateChanged(ItemEvent arg0) {
                cm.changeProfile((String)configureBox.getSelectedItem());
            }
        }
        );
    }

    void toggleOptions() {
        if (optionsPane.isVisible()) {
            optionsPane.setVisible(false);
            optionButton.setText(LocalText.getText("OPTIONS"));
        } else {
            optionsPane.setVisible(true);
            optionButton.setText(LocalText.getText("HIDE_OPTIONS"));
        }
    }
    
    // TODO: Rewrite Options mechanism to allow for common options
    void initOptions(GameInfo selectedGame) {
        // clear all previous options
        optionsPane.removeAll();
        optionComponents.clear();

        GameOptionsSet.Builder availableOptions = controller.getAvailableOptions(selectedGame);
        if (availableOptions == null || availableOptions.getOptions().isEmpty()) {
            // no options available
            JLabel label = new JLabel(LocalText.getText("NoGameOptions"));
            optionsPane.add(label);
        } else  { 
            List<GameOption> options = availableOptions.getOptions();
            optionsPane.setLayout(
                    new GridLayout(((options.size() + 1) / 2), 2, 2, 2));

            for (GameOption option : options) {
                String selectedValue = option.getSelectedValue();
                if (option.isBoolean()) {
                    JCheckBox checkbox =
                            new JCheckBox(option.getLocalisedName());
                    if (selectedValue.equalsIgnoreCase("yes")) {
                        checkbox.setSelected(true);
                    }
                    // the action related to the action
                    checkbox.addActionListener(controller.getOptionChangeAction(option));
                    optionComponents.put(option, checkbox);

                    optionsPane.add(checkbox); 
                } else {
                    // put dropdown and label into one panel to align with checkboxes
                    JPanel dropdownPanel = new JPanel();
                    dropdownPanel.setLayout(new BoxLayout(dropdownPanel, BoxLayout.LINE_AXIS));
                    dropdownPanel.add(new JLabel(LocalText.getText("SelectSomething",
                            option.getLocalisedName())));
                    dropdownPanel.add(Box.createHorizontalGlue());
                    
                    JComboBox<String> dropdown = new JComboBox<String>();
                    for (String value : option.getAllowedValues()) {
                        dropdown.addItem(value);
                    }
                    if (selectedValue != null) {
                        dropdown.setSelectedItem(selectedValue);
                    }
                    // the action related to the action
                    dropdown.addActionListener(controller.getOptionChangeAction(option));
                    optionComponents.put(option, dropdown);

                    dropdownPanel.add(dropdown);
                    optionsPane.add(dropdownPanel);
                }
            }
            optionsPane.setVisible(true);
            optionButton.setText(LocalText.getText("HIDE_OPTIONS"));
        }
    }
    
    void hideOptions() {
        optionsPane.setVisible(false);
        optionsPane.removeAll();
        optionComponents.clear();
        optionButton.setText(LocalText.getText("OPTIONS"));
    }

    void initPlayersPane(GameInfo selectedGame) {
        playersPane.setVisible(false);

        // Remember names that have already been filled-in...
        List<String> prefilledPlayers = Lists.newArrayList();
        for (PlayerInfo player:players) {
            if (player.name != null
                    && player.name.getText().length() > 0) {
                prefilledPlayers.add(player.name.getText());
            }
        }
        // and remove existing players
        players.clear();
        
        // use default players if none provided so far
        if (prefilledPlayers.isEmpty()) {
            prefilledPlayers = Arrays.asList(Config.get("default_players").split(","));
        }
        
        // create playersPane
        playersPane.removeAll();

        int maxPlayers = selectedGame.getMaxPlayers();
        int minPlayers = selectedGame.getMinPlayers();

        playersPane.setLayout(new GridLayout(maxPlayers + 1, 0, 0, 2));
        playersPane.setBorder(BorderFactory.createLoweredBevelBorder());
        playersPane.add(new JLabel("Players:"));

        playersPane.add(randomizeButton);

        for (int i = 0; i < maxPlayers; i++) {
            
            PlayerInfo player = new PlayerInfo();
            
            player.number.setText(LocalText.getText("PlayerName", Integer.toString(i + 1)));
            player.name.setInputVerifier(controller.playerNameVerifier);

            /*
             * Prefill with any configured player names. This can be useful to
             * speed up testing purposes.
             */
            if (i < prefilledPlayers.size()) {
                player.name.setText(prefilledPlayers.get(i));
            }
            if (i < minPlayers) {
                player.name.setBorder(BorderFactory.createLineBorder(Color.RED));
            }
            if (i <= minPlayers || i <= prefilledPlayers.size()) {
                player.name.setEnabled(true);
                player.number.setForeground(Color.BLACK);
            } else {
                player.name.setEnabled(false);
                player.number.setForeground(Color.GRAY);
            }

            playersPane.add(player.number);
            playersPane.add(player.name);
            players.add(player);
        }
        playersPane.setVisible(true);
    }

    GameInfo getSelectedGame() {
        return controller.getGameList().get(gameNameBox.getSelectedIndex());
    }
    
    String getPlayerName(int i) {
        PlayerInfo player = players.get(i);
        return player.name.getText();
    }
    
    int getPlayerCount() {
        return getPlayers().size();
    }

    ImmutableList<String> getPlayers() {
        ImmutableList.Builder<String> playerList = ImmutableList.builder();
        for (PlayerInfo player:players) {
            String name = player.name.getText();
            if (name != null && name.length() > 0) {
                playerList.add(name);
            }
        }
        return playerList.build();
    }
    
    void setPlayers(List<String> newPlayers) {
        LinkedList<String> newPlayersCopy = Lists.newLinkedList(newPlayers);
        for (PlayerInfo player:players) {
            if (newPlayersCopy.isEmpty()) {
                player.name.setText(null);
            } else {
                player.name.setText(newPlayersCopy.pop());
            }
        }
    }
    
    void enablePlayer(Integer playerNr) {
        final PlayerInfo player = players.get(playerNr);
        player.name.setEnabled(true);
        player.number.setForeground(Color.BLACK);
    }
    
    void disablePlayer(Integer playerNr) {
        PlayerInfo player = players.get(playerNr);
        player.name.setEnabled(false);
        player.number.setForeground(Color.GRAY);
    }
    
    boolean isPlayerEnabled(Integer playerNr) {
        return players.get(playerNr).name.isEnabled();
    }
    
    void setFocus(Integer playerNr) {
        final PlayerInfo focus = players.get(playerNr);
        EventQueue.invokeLater(new Runnable() {
            public void run() {
                focus.name.requestFocusInWindow();
            }
        });
    }

    boolean areOptionsVisible() {
        return optionsPane.isVisible();
    }
    
    String getSelectedGameOption(GameOption option) {
        if (option.isBoolean()) {
            JCheckBox checkbox = (JCheckBox) optionComponents.get(option);
            return checkbox.isSelected() ? "yes" : "no";
        } else {
            JComboBox dropdown = (JComboBox) optionComponents.get(option);
            return (String) dropdown.getSelectedItem();
        }
    }
    
    void addConfigureProfile(String profile) {
        configureBox.addItem(profile);
    }
    
    void removeConfigureProfile(String profile) {
        configureBox.removeItem(profile);
    }
    
    void changeConfigureProfile(String profile) {
        configureBox.setSelectedItem(profile);
    }
    
}
