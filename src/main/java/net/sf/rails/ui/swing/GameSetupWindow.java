package net.sf.rails.ui.swing;

import java.awt.*;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.awt.event.KeyEvent;
import java.awt.event.MouseAdapter;
import java.awt.event.MouseEvent;
import java.util.ArrayList;
import java.util.LinkedList;
import java.util.List;
import java.util.SortedMap;

import javax.swing.*;

import com.google.common.collect.ImmutableList;
import com.google.common.collect.Lists;
import com.google.common.collect.Maps;

import net.sf.rails.common.ConfigManager;
import net.sf.rails.common.GameInfo;
import net.sf.rails.common.GameOption;
import net.sf.rails.common.GameOptionsSet;
import net.sf.rails.common.LocalText;

/**
 * The Game Setup Window displays the first window presented to the user. This
 * window contains all of the options available for starting a new rails.game.
 */
public class GameSetupWindow extends JDialog {
    private static final long serialVersionUID = 1L;

    // Layout Panels
    private final JPanel galleryPane = new JPanel();
    private final JEditorPane gameDetailsPane = new JEditorPane("text/html", "");
    private final JPanel playersPane = new JPanel();
    private final JPanel buttonPane = new JPanel();
    private final JPanel optionsPane = new JPanel();

    // Buttons
    private final JButton newButton = new JButton(LocalText.getText("NewGame"));
    private final JButton loadButton = new JButton(LocalText.getText("LoadGame"));
    private final JButton optionButton = new JButton(LocalText.getText("OPTIONS"));
    private final JButton creditsButton = new JButton(LocalText.getText("CREDITS"));
    private final JButton randomizeButton = new JButton(LocalText.getText("RandomizePlayers"));
    private final JButton timeOptionsButton = new JButton(LocalText.getText("TIME_SETTINGS", "Time Settings"));


    // State tracking
    private final List<GameCard> gameCards = new ArrayList<>();
    private GameInfo selectedGameInfo;

    private DefaultListModel<String> rosterModel;
    private JList<String> rosterList;

    private static class PlayerInfo {
        private final JLabel number = new JLabel();
        private final JTextField name = new JTextField(14);
        private String fullName = "";
    }

    private final List<PlayerInfo> players = Lists.newArrayList();
    private final SortedMap<GameOption, JComponent> optionComponents = Maps.newTreeMap();
    private final GameSetupController controller;

    public GameSetupWindow(GameSetupController controller) {
        super();

        this.controller = controller;
        initialize();
        initLayout();
        GameInfo selectedGame = initGameList();
        initPlayersPane(selectedGame);

        this.setMinimumSize(new Dimension(950, 750));
        this.pack();
        this.setLocationRelativeTo(null); // Center on screen
        this.setVisible(false);
    }

    

    private void initialize() {
        newButton.setMnemonic(KeyEvent.VK_N);
        loadButton.setMnemonic(KeyEvent.VK_L);
        optionButton.setMnemonic(KeyEvent.VK_O);
        creditsButton.setMnemonic(KeyEvent.VK_E);
        randomizeButton.setMnemonic(KeyEvent.VK_R);
        timeOptionsButton.setMnemonic(KeyEvent.VK_T);

        this.setTitle("Rails: New Game");
        this.setDefaultCloseOperation(JFrame.DISPOSE_ON_CLOSE);

        newButton.addActionListener(e -> {
            if (getPlayers().isEmpty()) {
                JOptionPane.showMessageDialog(this,
                        "You must enter at least one player name!",
                        "Warning", JOptionPane.WARNING_MESSAGE);
                return;
            }
            controller.getNewAction().actionPerformed(e);
        });

        loadButton.addActionListener(controller.getLoadAction());
        optionButton.addActionListener(controller.getOptionPanelAction());
        creditsButton.addActionListener(controller.getCreditsAction());
        randomizeButton.addActionListener(this::performRandomizationEffect);
        timeOptionsButton.addActionListener(controller.getTimeOptionsAction());

        // Setup Buttons (Vertical layout for the right side)
        buttonPane.setLayout(new GridLayout(0, 1, 5, 5));
        buttonPane.setBorder(BorderFactory.createEmptyBorder(5, 5, 0, 0));
        buttonPane.add(timeOptionsButton);
        buttonPane.add(optionButton);
        buttonPane.add(loadButton);
        buttonPane.add(creditsButton);

        // Push the New Game button to the bottom using empty placeholders
        buttonPane.add(new JLabel());
        buttonPane.add(new JLabel());

        newButton.setFont(newButton.getFont().deriveFont(java.awt.Font.BOLD, 14f));
        buttonPane.add(newButton);

        optionsPane.setLayout(new FlowLayout());
        optionsPane.setVisible(false);
    }

    private void initLayout() {
        this.getContentPane().setLayout(new BorderLayout(10, 10));
        ((JPanel) this.getContentPane()).setBorder(BorderFactory.createEmptyBorder(10, 10, 10, 10));

        // TOP: Game Gallery (6 columns, shorter)
        galleryPane.setLayout(new GridLayout(0, 6, 10, 10));
        JScrollPane galleryScroll = new JScrollPane(galleryPane);
        galleryScroll.getVerticalScrollBar().setUnitIncrement(16);
        galleryScroll.setBorder(BorderFactory.createTitledBorder("Select a Game"));
        galleryScroll.setPreferredSize(new Dimension(900, 500)); // Increased height (about twice as tall)

        // MIDDLE: Game Details & BGG Button (Horizontal Panel)
        JPanel middlePanel = new JPanel(new BorderLayout(5, 5));
        
        gameDetailsPane.setEditable(false);
        JScrollPane detailsScroll = new JScrollPane(gameDetailsPane);
        detailsScroll.setPreferredSize(new Dimension(900, 150)); // Slightly taller for reading comfort
        
        detailsScroll.setBorder(BorderFactory.createTitledBorder("Game Details"));

        JButton bggButton = new JButton("Open BGG");
        bggButton.setToolTipText("Search for this game on BoardGameGeek");
        bggButton.addActionListener(e -> openBGGLink());
        JPanel bggPanel = new JPanel(new FlowLayout(FlowLayout.CENTER));
        bggPanel.setBorder(BorderFactory.createEmptyBorder(10, 5, 0, 5));
        bggPanel.add(bggButton);

        JPanel optionsWrapper = new JPanel(new BorderLayout());
        optionsWrapper.add(optionsPane, BorderLayout.NORTH);

        middlePanel.add(detailsScroll, BorderLayout.CENTER);
        middlePanel.add(bggPanel, BorderLayout.EAST);
        middlePanel.add(optionsWrapper, BorderLayout.SOUTH);

        // BOTTOM: Players & Buttons
        JPanel bottomPanel = new JPanel(new BorderLayout(10, 5));
        bottomPanel.add(playersPane, BorderLayout.CENTER);
        bottomPanel.add(buttonPane, BorderLayout.EAST);

        // Combine Middle and Bottom into one unit so Middle doesn't stretch vertically
        JPanel lowerMainPanel = new JPanel(new BorderLayout(10, 10));
        lowerMainPanel.add(middlePanel, BorderLayout.NORTH);
        lowerMainPanel.add(bottomPanel, BorderLayout.CENTER);

        // Main Split
      JSplitPane mainSplit = new JSplitPane(JSplitPane.VERTICAL_SPLIT, galleryScroll, lowerMainPanel);
        mainSplit.setResizeWeight(0.60); // Give 60% of the window to the top gallery
        mainSplit.setBorder(null);

        this.getContentPane().add(mainSplit, BorderLayout.CENTER);

    }

    private void openBGGLink() {
        if (selectedGameInfo == null)
            return;
        try {
            String query = java.net.URLEncoder.encode(selectedGameInfo.getName(), "UTF-8");
            java.net.URI uri = new java.net.URI(
                    "https://boardgamegeek.com/geeksearch.php?action=search&objecttype=boardgame&q=" + query);
            java.awt.Desktop.getDesktop().browse(uri);
        } catch (Exception ex) {
            org.slf4j.LoggerFactory.getLogger(GameSetupWindow.class).error("Could not open BGG link", ex);
            JOptionPane.showMessageDialog(this, "Failed to open browser.", "Error", JOptionPane.ERROR_MESSAGE);
        }
    }

    private GameInfo initGameList() {
        GameInfo defaultGame = controller.getDefaultGame();

        for (GameInfo game : controller.getGameList()) {
            GameCard card = new GameCard(game, this::onGameCardSelected);
            gameCards.add(card);
            galleryPane.add(card);

            if (game.equals(defaultGame)) {
                selectedGameInfo = game;
                card.setSelectedState(true);
                updateGameDetails(game);
            }
        }
        return selectedGameInfo;
    }

    private void onGameCardSelected(GameInfo game) {
        // Update visual state of cards
        for (GameCard card : gameCards) {
            card.setSelectedState(card.getGameInfo().equals(game));
        }

        selectedGameInfo = game;
        updateGameDetails(game);

        // Simulate Action Event to notify controller to update Players Pane and Options
        ActionEvent mockEvent = new ActionEvent(this, ActionEvent.ACTION_PERFORMED, "GameSelected");
        controller.getGameAction().actionPerformed(mockEvent);
    }

    private void updateGameDetails(GameInfo game) {
        if (game == null)
            return;

        // The XML already provides CDATA wrapped in <html> tags.
        // We use it directly to preserve formatting like <ul>, <h3>, and <b>.
        String rawDesc = game.getDescription();
        // System.out.println("DEBUG - Raw Description for " + game.getName() + ": " + rawDesc);
        
        String html = (rawDesc != null && !rawDesc.trim().isEmpty()) ? rawDesc : "<html><body>No description available.</body></html>";
        
        if (game.getNote() != null && !game.getNote().trim().isEmpty()) {
            String noteText = "<br><br><b>Status Note:</b> " + game.getNote();
            // Inject the note right before the closing HTML tags to keep structure valid
            if (html.toLowerCase().contains("</body>")) {
                html = html.replaceAll("(?i)</body>", noteText + "</body>");
            } else if (html.toLowerCase().contains("</html>")) {
                html = html.replaceAll("(?i)</html>", noteText + "</html>");
            } else {
                html += noteText;
            }
        }
        
        // System.out.println("DEBUG - Final HTML to render: " + html);
        
        // Explicitly set the content type again to ensure JEditorPane parses it as HTML
        gameDetailsPane.setContentType("text/html");
        gameDetailsPane.setText(html);

        // Scroll back to top whenever a new game is selected
        javax.swing.SwingUtilities.invokeLater(() -> gameDetailsPane.setCaretPosition(0));

    }



    public void toggleOptions() {
        if (optionsPane.isVisible()) {
            optionsPane.setVisible(false);
            optionButton.setText(LocalText.getText("OPTIONS"));
        } else {
            optionsPane.setVisible(true);
            optionButton.setText(LocalText.getText("HIDE_OPTIONS"));
        }
        this.revalidate(); // Revalidate layout when toggling
    }

    public void initOptions(GameInfo selectedGame) {
        optionsPane.removeAll();
        optionComponents.clear();

        GameOptionsSet.Builder availableOptions = controller.getAvailableOptions(selectedGame);

        if (availableOptions != null) {
            boolean trainOptionExists = false;
            boolean privateOptionExists = false;

            for (GameOption opt : availableOptions.getOptions()) {
                if ("RestrictTrainTradingToSameOwner".equals(opt.getName()))
                    trainOptionExists = true;
                if ("RestrictPrivateTradingToSameOwner".equals(opt.getName()))
                    privateOptionExists = true;
            }

            if (!trainOptionExists) {
                GameOption.Builder optBuilder = GameOption.builder("RestrictTrainTradingToSameOwner");
                optBuilder.setType(GameOption.OPTION_TYPE_TOGGLE);
                optBuilder.setDefaultValue(GameOption.OPTION_VALUE_YES);
                optBuilder.setOrdering(998);
                availableOptions.withOption(optBuilder.build());
            }

            if (!privateOptionExists) {
                GameOption.Builder optBuilder = GameOption.builder("RestrictPrivateTradingToSameOwner");
                optBuilder.setType(GameOption.OPTION_TYPE_TOGGLE);
                optBuilder.setDefaultValue(GameOption.OPTION_VALUE_YES);
                optBuilder.setOrdering(999);
                availableOptions.withOption(optBuilder.build());
            }
        }

        if (availableOptions == null || availableOptions.getOptions().isEmpty()) {
            JLabel label = new JLabel(LocalText.getText("NoGameOptions"));
            optionsPane.add(label);
        } else {
            List<GameOption> options = availableOptions.getOptions();
            optionsPane.setLayout(new GridLayout(((options.size() + 1) / 2), 2, 2, 2));

            for (GameOption option : options) {
                String selectedValue = option.getSelectedValue();
                if (option.isBoolean()) {
                    JCheckBox checkbox = new JCheckBox(option.getLocalisedName());
                    if (selectedValue.equalsIgnoreCase("yes"))
                        checkbox.setSelected(true);
                    checkbox.addActionListener(controller.getOptionChangeAction(option));
                    optionComponents.put(option, checkbox);
                    optionsPane.add(checkbox);
                } else if (!option.isHidden()) {
                    JPanel dropdownPanel = new JPanel();
                    dropdownPanel.setLayout(new BoxLayout(dropdownPanel, BoxLayout.LINE_AXIS));
                    dropdownPanel.add(new JLabel(LocalText.getText("SelectSomething", option.getLocalisedName())));
                    dropdownPanel.add(Box.createHorizontalGlue());

                    JComboBox<String> dropdown = new JComboBox<>();
                    for (String value : option.getAllowedValues())
                        dropdown.addItem(value);
                    if (selectedValue != null)
                        dropdown.setSelectedItem(selectedValue);

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

    public void hideOptions() {
        optionsPane.setVisible(false);
        optionsPane.removeAll();
        optionComponents.clear();
        optionButton.setText(LocalText.getText("OPTIONS"));
    }

    void initPlayersPane(GameInfo selectedGame) {
        playersPane.setVisible(false);

        List<String> prefilledPlayers = Lists.newArrayList();
        List<String> prefilledFullNames = Lists.newArrayList();
        for (PlayerInfo player : players) {
            if (player.name != null && player.name.getText().trim().length() > 0) {
                prefilledPlayers.add(player.name.getText().trim());
                prefilledFullNames.add(player.fullName);
            }
        }
        players.clear();
        playersPane.removeAll();

        int maxPlayers = selectedGame.getMaxPlayers();
        int minPlayers = selectedGame.getMinPlayers();

        playersPane.setLayout(new BorderLayout(10, 0));
        playersPane.setBorder(BorderFactory.createTitledBorder("Players Configuration"));

        // ACTIVE PLAYERS PANEL
        JPanel activePanel = new JPanel(new GridLayout(maxPlayers + 2, 1, 0, 2));
        activePanel.setBorder(BorderFactory.createTitledBorder("Active Players"));

        for (int i = 0; i < maxPlayers; i++) {
            PlayerInfo player = new PlayerInfo();
            player.name.setInputVerifier(controller.getPlayerNameVerifier());

            if (i < prefilledPlayers.size()) {
                player.name.setText(prefilledPlayers.get(i));
                player.fullName = prefilledFullNames.get(i);
            }
            if (i < minPlayers) {
                player.name.setBorder(BorderFactory.createLineBorder(Color.RED));
            }
            player.name.setEnabled(i < minPlayers || i <= prefilledPlayers.size());

            final int playerNr = i;
            player.name.addMouseListener(new MouseAdapter() {
                @Override
                public void mouseClicked(MouseEvent e) {
                    if (playerNr == getPlayerCount()) {
                        enablePlayer(playerNr);
                        setFocus(playerNr);
                    }
                }
            });

            JPanel slot = new JPanel(new BorderLayout(5, 0));
            JLabel numberLabel = new JLabel((i + 1) + ".");
            numberLabel.setPreferredSize(new Dimension(20, 20));
            slot.add(numberLabel, BorderLayout.WEST);
            slot.add(player.name, BorderLayout.CENTER);

            JButton clearBtn = new JButton("X");
            clearBtn.setMargin(new Insets(0, 0, 0, 0));
            clearBtn.setPreferredSize(new Dimension(24, 20));
            clearBtn.addActionListener(e -> {
                player.name.setText("");
                player.fullName = "";
                compactActivePlayers();
            });
            slot.add(clearBtn, BorderLayout.EAST);

            activePanel.add(slot);
            players.add(player);
        }

        JPanel buttonWrapper = new JPanel(new FlowLayout(FlowLayout.LEFT));
        buttonWrapper.add(randomizeButton);
        activePanel.add(buttonWrapper);

        // ROSTER PANEL
        JPanel rosterPanel = new JPanel(new BorderLayout(0, 5));
        rosterPanel.setBorder(BorderFactory.createTitledBorder("Player Roster"));

        rosterModel = new DefaultListModel<>();
        loadRoster(rosterModel);
        rosterList = new JList<>(rosterModel);
        rosterList.setSelectionMode(ListSelectionModel.SINGLE_SELECTION);
        rosterList.setVisibleRowCount(maxPlayers);

        rosterList.addMouseListener(new MouseAdapter() {
            public void mouseClicked(MouseEvent evt) {
                if (evt.getClickCount() == 2) {
                    String selected = rosterList.getSelectedValue();
                    if (selected != null)
                        addPlayerToActive(selected);
                }
            }
        });

        JScrollPane scrollPane = new JScrollPane(rosterList);
        rosterPanel.add(scrollPane, BorderLayout.CENTER);

        JButton addRosterBtn = new JButton("Add...");
        addRosterBtn.addActionListener(e -> {
            String name = JOptionPane.showInputDialog(GameSetupWindow.this, "Enter Player Name:");
            if (name != null && !name.trim().isEmpty()) {
                String cleanName = name.trim();
                if (!rosterModel.contains(cleanName)) {
                    rosterModel.addElement(cleanName);
                    saveRoster(rosterModel);
                } else {
                    JOptionPane.showMessageDialog(GameSetupWindow.this,
                            "Player '" + cleanName + "' is already in the roster!",
                            "Duplicate Player", JOptionPane.WARNING_MESSAGE);
                }
            }
        });

        JButton removeRosterBtn = new JButton("Remove");
        removeRosterBtn.addActionListener(e -> {
            int selectedIndex = rosterList.getSelectedIndex();
            if (selectedIndex != -1) {
                rosterModel.remove(selectedIndex);
                saveRoster(rosterModel);
            }
        });

        JPanel rosterBtnPanel = new JPanel(new FlowLayout(FlowLayout.CENTER));
        rosterBtnPanel.add(addRosterBtn);
        rosterBtnPanel.add(removeRosterBtn);
        rosterPanel.add(rosterBtnPanel, BorderLayout.SOUTH);

        playersPane.add(activePanel, BorderLayout.WEST);
        playersPane.add(rosterPanel, BorderLayout.CENTER);

        playersPane.setVisible(true);
    }

    private void compactActivePlayers() {
        List<String> currentNames = new ArrayList<>();
        List<String> currentFullNames = new ArrayList<>();
        for (PlayerInfo p : players) {
            if (!p.name.getText().trim().isEmpty()) {
                currentNames.add(p.name.getText().trim());
                currentFullNames.add(p.fullName);
            }
        }
        for (int i = 0; i < players.size(); i++) {
            if (i < currentNames.size()) {
                players.get(i).name.setText(currentNames.get(i));
                players.get(i).fullName = currentFullNames.get(i);
                players.get(i).name.setEnabled(true);
            } else {
                players.get(i).name.setText("");
                players.get(i).fullName = "";
                players.get(i).name.setEnabled(i == currentNames.size());
            }
        }
    }

    private String extractShortName(String rosterEntry) {
        int start = rosterEntry.lastIndexOf('(');
        int end = rosterEntry.lastIndexOf(')');
        if (start != -1 && end != -1 && end > start) {
            return rosterEntry.substring(start + 1, end).trim();
        }
        String[] parts = rosterEntry.trim().split("\\s+");
        if (parts.length > 0)
            return parts[0];
        return rosterEntry;
    }

    private void addPlayerToActive(String fullRosterName) {
        for (PlayerInfo player : players) {
            if (player.name.isEnabled() && !player.name.getText().trim().isEmpty()) {
                if (fullRosterName.equals(player.fullName)) {
                    JOptionPane.showMessageDialog(GameSetupWindow.this,
                            "Player '" + fullRosterName + "' is already in the game!",
                            "Duplicate Player", JOptionPane.WARNING_MESSAGE);
                    return;
                }
            }
        }

        PlayerInfo foundSlot = null;
        for (PlayerInfo player : players) {
            if (player.name.isEnabled() && player.name.getText().trim().isEmpty()) {
                foundSlot = player;
                break;
            }
        }
        if (foundSlot == null)
            return;
        final PlayerInfo targetSlot = foundSlot;

        String baseShortName = extractShortName(fullRosterName);
        boolean exactMatchFound = false;
        boolean baseNameUsedInNumbered = false;

        for (PlayerInfo p : players) {
            if (p == targetSlot || p.name.getText().trim().isEmpty())
                continue;
            String text = p.name.getText().trim();
            if (text.equals(baseShortName))
                exactMatchFound = true;
            else if (text.startsWith(baseShortName + " "))
                baseNameUsedInNumbered = true;
        }

        String finalShortName = baseShortName;

        if (exactMatchFound || baseNameUsedInNumbered) {
            if (exactMatchFound) {
                for (PlayerInfo p : players) {
                    if (p != targetSlot && p.name.getText().trim().equals(baseShortName)) {
                        int c = 1;
                        while (true) {
                            String test = baseShortName + " " + c;
                            boolean taken = players.stream()
                                    .anyMatch(other -> other != p && other.name.getText().trim().equals(test));
                            if (!taken) {
                                p.name.setText(test);
                                break;
                            }
                            c++;
                        }
                        break;
                    }
                }
            }
            int counter = 1;
            while (true) {
                String testName = baseShortName + " " + counter;
                boolean taken = players.stream()
                        .anyMatch(p -> p != targetSlot && p.name.getText().trim().equals(testName));
                if (!taken) {
                    finalShortName = testName;
                    break;
                }
                counter++;
            }
        }

        targetSlot.name.setText(finalShortName);
        targetSlot.fullName = fullRosterName;
        compactActivePlayers();
    }

    private void loadRoster(DefaultListModel<String> model) {
        java.io.File file = new java.io.File("PlayerNames18xx.txt");
        if (file.exists()) {
            try (java.io.BufferedReader br = new java.io.BufferedReader(new java.io.FileReader(file))) {
                String line;
                while ((line = br.readLine()) != null) {
                    if (!line.trim().isEmpty())
                        model.addElement(line.trim());
                }
            } catch (Exception e) {
                org.slf4j.LoggerFactory.getLogger(GameSetupWindow.class).error("Error loading roster file", e);
            }
        } 
    }

    private void saveRoster(DefaultListModel<String> model) {
        java.io.File file = new java.io.File("PlayerNames18xx.txt");
        try (java.io.PrintWriter pw = new java.io.PrintWriter(new java.io.FileWriter(file))) {
            for (int i = 0; i < model.size(); i++)
                pw.println(model.get(i));
        } catch (Exception e) {
            org.slf4j.LoggerFactory.getLogger(GameSetupWindow.class).error("Error saving roster file", e);
        }
    }

    GameInfo getSelectedGame() {
        return selectedGameInfo;
    }

    String getPlayerName(int i) {
        return players.get(i).name.getText();
    }

    int getPlayerCount() {
        return getPlayers().size();
    }

    ImmutableList<String> getPlayers() {
        ImmutableList.Builder<String> playerList = ImmutableList.builder();
        for (PlayerInfo player : players) {
            String name = player.name.getText();
            if (name != null && name.length() > 0)
                playerList.add(name);
        }
        return playerList.build();
    }

    ImmutableList<String> getFullNames() {
        ImmutableList.Builder<String> playerList = ImmutableList.builder();
        for (PlayerInfo player : players) {
            String name = player.name.getText();
            if (name != null && name.length() > 0) {
                String full = (player.fullName != null && !player.fullName.trim().isEmpty()) ? player.fullName : name;
                playerList.add(full);
            }
        }
        return playerList.build();
    }

    void setPlayers(List<String> newPlayers) {
        LinkedList<String> newPlayersCopy = Lists.newLinkedList(newPlayers);
        for (PlayerInfo player : players) {
            if (newPlayersCopy.isEmpty())
                player.name.setText(null);
            else
                player.name.setText(newPlayersCopy.pop());
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

    public boolean isPlayerEnabled(Integer playerNr) {
        return players.get(playerNr).name.isEnabled();
    }

    void setFocus(Integer playerNr) {
        final PlayerInfo focus = players.get(playerNr);
        EventQueue.invokeLater(() -> focus.name.requestFocusInWindow());
    }

    public boolean areOptionsVisible() {
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


    private void performRandomizationEffect(ActionEvent originalEvent) {
        randomizeButton.setEnabled(false);

        class PlayerIdentity {
            String shortName, fullName;

            PlayerIdentity(String s, String f) {
                shortName = s;
                fullName = f;
            }
        }

        List<PlayerIdentity> originalIdentities = new ArrayList<>();
        for (PlayerInfo p : players) {
            if (!p.name.getText().trim().isEmpty())
                originalIdentities.add(new PlayerIdentity(p.name.getText().trim(), p.fullName));
        }
        int activeCount = originalIdentities.size();

        if (activeCount < 2) {
            randomizeButton.setEnabled(true);
            return;
        }

        List<PlayerIdentity> finalIdentities = new ArrayList<>(originalIdentities);
        java.util.Collections.shuffle(finalIdentities);

        boolean[] locked = new boolean[activeCount];
        final int SHUFFLE_TICK_MS = 50;
        final int LOCK_DELAY_MS = 1000;

        Timer timer = new Timer(SHUFFLE_TICK_MS, null);

        timer.addActionListener(new ActionListener() {
            int lockedCount = 0;
            long lastLockTime = System.currentTimeMillis();

            @Override
            public void actionPerformed(ActionEvent e) {
                long now = System.currentTimeMillis();

                if (lockedCount < activeCount && (now - lastLockTime > LOCK_DELAY_MS)) {
                    List<Integer> availableIndices = Lists.newArrayList();
                    for (int i = 0; i < activeCount; i++)
                        if (!locked[i])
                            availableIndices.add(i);

                    if (!availableIndices.isEmpty()) {
                        int indexToLock = availableIndices.get((int) (Math.random() * availableIndices.size()));
                        locked[indexToLock] = true;

                        PlayerInfo pInfo = players.get(indexToLock);
                        pInfo.name.setText(finalIdentities.get(indexToLock).shortName);
                        pInfo.fullName = finalIdentities.get(indexToLock).fullName;
                        pInfo.name.setForeground(Color.BLACK);
                        pInfo.name.setFont(pInfo.name.getFont().deriveFont(Font.BOLD));

                        lockedCount++;
                        lastLockTime = now;
                    }
                }

                if (lockedCount < activeCount) {
                    for (int i = 0; i < activeCount; i++) {
                        if (!locked[i]) {
                            PlayerInfo pInfo = players.get(i);
                            String randomName = originalIdentities.get((int) (Math.random() * activeCount)).shortName;
                            pInfo.name.setText(randomName);
                            pInfo.name.setForeground(Color.GRAY);
                            pInfo.name.setFont(pInfo.name.getFont().deriveFont(Font.ITALIC));
                        }
                    }
                } else {
                    ((Timer) e.getSource()).stop();
                    for (int i = 0; i < activeCount; i++) {
                        players.get(i).name.setText(finalIdentities.get(i).shortName);
                        players.get(i).fullName = finalIdentities.get(i).fullName;
                        players.get(i).name.setForeground(Color.BLACK);
                        players.get(i).name.setFont(players.get(i).name.getFont().deriveFont(Font.PLAIN));
                    }
                    randomizeButton.setEnabled(true);
                }
            }
        });

        timer.start();
    }

    public void addConfigureProfile(String profile) {
        // No-op placeholder to preserve interface compilation bindings across controller updates
    }

    public void removeConfigureProfile(String profile) {
        // No-op placeholder to preserve interface compilation bindings across controller updates
    }

    public void changeConfigureProfile(String profile) {
        // No-op placeholder to preserve interface compilation bindings across controller updates
    }

}