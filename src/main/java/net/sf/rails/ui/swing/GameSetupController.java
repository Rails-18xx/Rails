package net.sf.rails.ui.swing;

import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.io.File;
import java.util.Arrays;
import java.util.Collections;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.SortedSet;
import java.util.TreeSet;
import java.util.regex.Pattern;

import javax.swing.*;
import javax.swing.filechooser.FileFilter;

import org.apache.commons.lang3.StringUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.google.common.collect.ComparisonChain;
import com.google.common.collect.ImmutableList;
import com.google.common.collect.Lists;
import com.google.common.collect.Maps;
import com.google.common.collect.Sets;

import net.sf.rails.ui.swing.TimeOptionsDialog;
import net.sf.rails.common.Config;
import net.sf.rails.common.GameData;
import net.sf.rails.common.GameInfo;
import net.sf.rails.common.GameOption;
import net.sf.rails.common.GameOptionsSet;
import net.sf.rails.common.LocalText;
import net.sf.rails.common.parser.ConfigurationException;
import net.sf.rails.common.parser.GameInfoParser;
import net.sf.rails.common.parser.GameOptionsParser;
import net.sf.rails.game.GameManager; // IMPORTANT: Must be importable
import net.sf.rails.game.RailsRoot;
import net.sf.rails.sound.SoundManager;
import net.sf.rails.util.GameLoader;
import net.sf.rails.util.GameSaver;
import net.sf.rails.util.SystemOS;


/** Controller of the GameSetupWindow */
public class GameSetupController {

    private static final Logger log = LoggerFactory.getLogger(GameSetupController.class);

    private final SortedSet<GameInfo> gameList = Sets.newTreeSet();

    private String credits;

    private final Map<GameInfo, GameOptionsSet.Builder> gameOptions = Maps.newHashMap();

    // UI references
    public final GameSetupWindow window; 
    private ConfigWindow configWindow;
    private GameUIManager gameUIManager;
    
    private GameManager defaultGameManager = null;
    
    private final String savedFileExtension;

    // Actions
    private final ActionListener newAction = new NewAction();
    private final ActionListener loadAction = new LoadAction();
    private final ActionListener optionPanelAction = new OptionPanelAction();
    private final ActionListener creditsAction = new CreditsAction();
    private final ActionListener gameAction = new GameAction();
    private final ActionListener configureAction = new ConfigureAction();
    private final ActionListener randomizeAction = new RandomizeAction();
    private final ActionListener timeOptionsAction = new TimeOptionsAction(); 
    private final InputVerifier playerNameVerifier = new PlayerNameVerifier();

    private static final GameSetupController instance = new GameSetupController();

    private GameSetupController() {
        GameInfoParser gip = new GameInfoParser();
        try {
            gameList.addAll(gip.processGameList());
            credits = gip.getCredits();
        } catch (ConfigurationException e) {
            log.error("Unable to initialize Game setup controller", e);
        }

        window = new GameSetupWindow(this);
        
        savedFileExtension = "." + StringUtils.defaultIfBlank(Config.get("save.filename.extension"), GameUIManager.DEFAULT_SAVE_EXTENSION);

        // Notify the sound manager about having started the setup menu
        SoundManager.notifyOfGameSetup();
    }
    
    /**
     * Public method to access the template GameManager for TimeOptionsDialog.
     * This manager holds the time settings configuration chosen by the user.
     */
    public GameManager getDefaultGameManager() {
        GameManager gm = defaultGameManager; 
        
        if (gm == null) {
            try {
                GameInfo selectedGame = window.getSelectedGame();
                
                GameData gameData = GameData.create(selectedGame, getAvailableOptions(selectedGame), window.getPlayers());
                
                RailsRoot railsRoot = RailsRoot.create(gameData);
                // FIX 1: Correctly use the getManager method signature 
                    gm = railsRoot.getGameManager();
                defaultGameManager = gm; 
            } catch (ConfigurationException e) {
                log.error("Unable to create default GameManager for options dialog", e);
                return null;
            }
        }
        return gm;
    }


    public static GameSetupController getInstance() {
        return instance;
    }

    public void show() {
        window.setVisible(true);
    }

    protected ImmutableList<GameInfo> getGameList() {
        return ImmutableList.copyOf(gameList);
    }

    protected GameInfo getDefaultGame() {
        GameInfo defaultGame = GameInfo.findGame(gameList, Config.get("default_game"));        
        if (defaultGame == null) {
            defaultGame = gameList.first();
        }
        return defaultGame;
    }

    protected GameOptionsSet.Builder getAvailableOptions(GameInfo game) {
        if (!gameOptions.containsKey(game)) {
            return loadOptions(game);
        }
        return gameOptions.get(game);
    }

    protected Action getOptionChangeAction(GameOption option) {
        return new OptionChangeAction(option);
    }

    private GameOptionsSet.Builder loadOptions(GameInfo game) {
        log.debug("Load Game Options of {}", game.getName());
        GameOptionsSet.Builder loadGameOptions;
        try {
            loadGameOptions = GameOptionsParser.load(game.getName()); 
        } catch (ConfigurationException e) {
            log.error(e.getMessage());
            // FIX 2: Correct GameInfo method name should be getID()
            loadGameOptions = GameOptionsSet.builder();
        }
        gameOptions.put(game, loadGameOptions);
        return loadGameOptions;
    }

    /** FIX 3: Implements the missing helper method prepareGameUIInit() */
    public void prepareGameUIInit() {
        window.setVisible(false);
        if (configWindow != null) {
            configWindow.dispose();
            configWindow = null;
        }
    }
    
    /** FIX 4: Implements the missing helper method loadAndStartGame(File) */
    private void loadAndStartGame(File gameFile) {
        prepareGameUIInit();
        GameLoader.loadAndStartGame(gameFile);
    }


    // Existing Actions remain here...

    private class NewAction extends AbstractAction {
        private static final long serialVersionUID = 0L;

        @Override
        public void actionPerformed(ActionEvent e) {
            //start in new thread so that swing thread is not used for game setup
            new Thread(this::startNewGame).start();
        }

        
        private void startNewGame() {
            GameInfo selectedGame = window.getSelectedGame();
            List<String> players = window.getPlayers();
            List<String> fullNames = window.getFullNames(); // NEW: Fetch full names
            GameOptionsSet.Builder selectedOptions = getAvailableOptions(selectedGame);

            // check against number of available players
            if (players.size() < selectedGame.getMinPlayers()) {
                if (JOptionPane.showConfirmDialog(window,
                        "Not enough players. Continue Anyway?",
                        "Are you sure?", JOptionPane.YES_NO_OPTION) == JOptionPane.NO_OPTION) {
                    return;
                }
            }
            Set<String> playerNames = new HashSet<>();
            for ( String player : players ) {
                if ( playerNames.contains(player) ) {
                    JOptionPane.showMessageDialog(window, "All players must have unique names");
                    return;
                }
                playerNames.add(player);
            }

            SplashWindow splashWindow = new SplashWindow(false, selectedGame.getName());

            RailsRoot railsRoot = null;
            try {
                // Let the engine build the game using the SHORT names (for UI tabs, logs, etc.)
                GameData gameData = GameData.create(selectedGame, selectedOptions, players);
                railsRoot = RailsRoot.create(gameData);

// NEW: Inject the FULL names into the newly created Player objects
                List<net.sf.rails.game.Player> createdPlayers = railsRoot.getPlayerManager().getPlayers();
                for (int i = 0; i < createdPlayers.size(); i++) {
                    if (i < fullNames.size() && fullNames.get(i) != null && !fullNames.get(i).trim().isEmpty()) {
                        String fName = fullNames.get(i);
                        createdPlayers.get(i).setFullName(fName);
                        // Store locally so it survives save/reload cycles without breaking file formats
                        Config.set("player.fullname." + createdPlayers.get(i).getName(), fName);
                    }
                }
            } catch (ConfigurationException e) {
                log.error("unable to continue", e);
                // TODO: Fix this behavior, give more information?
                // Simply exit
                System.exit(-1);
            }

            String startError = railsRoot.start();
            if (startError != null) {
                JOptionPane.showMessageDialog(window, startError, "", JOptionPane.ERROR_MESSAGE);
                System.exit(-1);
            }
            // FIX 3 usage
            prepareGameUIInit();
            gameUIManager = GameLoader.startGameUIManager (railsRoot, false, splashWindow);
            gameUIManager.gameUIInit(true); // true indicates new game

            splashWindow.finalizeGameInit();
            gameUIManager.notifyOfSplashFinalization();
        }
    }

    private boolean isOurs(File f) {
        String ext = StringUtils.substringAfterLast(f.getName(), ".");
        if ( StringUtils.isBlank(ext) ) {
            // ignore files with no extensions
            return false;
        }
        switch (ext) {
            case GameUIManager.DEFAULT_SAVE_EXTENSION:
            case GameUIManager.DEFAULT_SAVE_POLLING_EXTENSION:
                return true;
            default:
                return ext.equals(savedFileExtension);
        }
    }

    private class LoadAction extends AbstractAction {
        private static final long serialVersionUID = 0L;

        @Override
        public void actionPerformed(ActionEvent e) {
            String saveDirectory = Config.get("save.directory");
            JFileChooser jfc = new JFileChooser();
            jfc.setCurrentDirectory(new File(saveDirectory));
            jfc.setFileFilter(new FileFilter() {
                @Override
                public boolean accept(File f) {
                    return isOurs(f) || f.isDirectory();
                }

                @Override
                public String getDescription() {
                    return "Rails files";
                }
            });
            jfc.setAcceptAllFileFilterUsed(false);

            if (jfc.showOpenDialog(window.getContentPane()) == JFileChooser.APPROVE_OPTION) {
                final File selectedFile = jfc.getSelectedFile();
                //start in new thread so that swing thread is not used for game setup
                new Thread(() -> loadAndStartGame(selectedFile)).start();
            }
        }

    }

    private class OptionPanelAction extends AbstractAction {
        private static final long serialVersionUID = 0L;

        @Override
        public void actionPerformed(ActionEvent arg0) {
            if (window.areOptionsVisible()) {
                window.hideOptions();
                window.pack();
            } else {
                window.initOptions(window.getSelectedGame());
                window.pack();
            }
        }
    }

    private class TimeOptionsAction extends AbstractAction {
        private static final long serialVersionUID = 0L;

        @Override
        public void actionPerformed(ActionEvent arg0) {
            GameManager gm = getDefaultGameManager();
            
            if (gm != null) {
                // FIX 5: Fix type incompatibility error (GameSetupWindow extends JDialog, which is a Window, but the
                // TimeOptionsDialog constructor expects Frame or Dialog. JDialog works as a parent for JDialog).
                // Since GameSetupWindow extends JDialog, it can be passed directly.
                // The correct constructor is TimeOptionsDialog(Dialog owner, GameManager gm)
                TimeOptionsDialog dialog = new TimeOptionsDialog(window, gm);
                dialog.setVisible(true);
            } else {
                JOptionPane.showMessageDialog(window, 
                                              "Could not initialize game manager for settings.", 
                                              "Error", 
                                              JOptionPane.ERROR_MESSAGE);
            }
        }
    }

    private class OptionChangeAction extends AbstractAction {
        private static final long serialVersionUID = 0L;

        private final GameOption option;

        private OptionChangeAction(GameOption option) {
            this.option = option;
        }

        @Override
        public void actionPerformed(ActionEvent arg0) {
            Object source = arg0.getSource();
            String value = null;
            if (source instanceof JCheckBox) {
                if (((JCheckBox) source).isSelected()) {
                    value = GameOption.OPTION_VALUE_YES;
                } else {
                    value = GameOption.OPTION_VALUE_NO;
                }
            } else if (source instanceof JComboBox) {
                value =  String.valueOf(((JComboBox)source).getSelectedItem());
            }
            GameInfo game  = window.getSelectedGame();
            option.setSelectedValue(value);
            log.debug("GameOption {} set to {} for game {}", option, value, game);
        }
    }

    private class CreditsAction extends AbstractAction {
        private static final long serialVersionUID = 0L;

        @Override
        public void actionPerformed(ActionEvent arg0) {
            JOptionPane.showMessageDialog(window,
                    new JLabel(credits), //enable html rendering
                    LocalText.getText("CREDITS"),
                    JOptionPane.INFORMATION_MESSAGE);
        }
    }

    private class GameAction extends AbstractAction {
        private static final long serialVersionUID = 0L;

        @Override
        public void actionPerformed(ActionEvent arg0) {
            window.initPlayersPane(window.getSelectedGame());
            if (window.areOptionsVisible()) {
                window.initOptions(window.getSelectedGame());
            }
            window.pack();
        }
    }

    private class ConfigureAction extends AbstractAction {
        private static final long serialVersionUID = 0L;

        @Override
        public void actionPerformed(ActionEvent arg0) {
            // start configureWindow
            if (configWindow == null) {
                configWindow = new ConfigWindow(window);
                configWindow.init(true);
                configWindow.setVisible(true);
            } else {
                configWindow.setVisible(true);
            }
        }
    }

    private class RandomizeAction extends AbstractAction {
        private static final long serialVersionUID = 0L;

        @Override
        public void actionPerformed(ActionEvent arg0) {
            List<String> players = Lists.newArrayList(window.getPlayers());
            Collections.shuffle(players);
            window.setPlayers(players);
        }
    }

    private class PlayerNameVerifier extends InputVerifier {

        @Override
        public boolean verify(JComponent arg0) {
            // normalize players
            List<String> players = window.getPlayers();
            window.setPlayers(players);

            // check if the next player has to be enabled
            int nextPlayerNr = players.size();

            if (nextPlayerNr < window.getSelectedGame().getMaxPlayers()) {
                if (!window.isPlayerEnabled(nextPlayerNr)) {
                    window.enablePlayer(nextPlayerNr);
                    window.setFocus(nextPlayerNr);
                }
            }

            while (++nextPlayerNr < window.getSelectedGame().getMaxPlayers()) {
                window.disablePlayer(nextPlayerNr);
            }

            return true;
        }

    }

    public ActionListener getNewAction() {
        return newAction;
    }

    public ActionListener getLoadAction() {
        return loadAction;
    }

    public ActionListener getOptionPanelAction() {
        return optionPanelAction;
    }

    public ActionListener getCreditsAction() {
        return creditsAction;
    }

    public ActionListener getConfigureAction() {
        return configureAction;
    }

    public ActionListener getRandomizeAction() {
        return randomizeAction;
    }

    public ActionListener getGameAction() {
        return gameAction;
    }

    public ActionListener getTimeOptionsAction() {
        return timeOptionsAction;
    }

    public InputVerifier getPlayerNameVerifier() {
        return playerNameVerifier;
    }
}
