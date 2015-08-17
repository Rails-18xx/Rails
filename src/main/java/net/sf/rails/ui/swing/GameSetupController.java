package net.sf.rails.ui.swing;

import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.io.File;
import java.util.Arrays;
import java.util.Collections;
import java.util.Comparator;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.SortedSet;
import java.util.TreeSet;

import javax.swing.AbstractAction;
import javax.swing.Action;
import javax.swing.InputVerifier;
import javax.swing.JCheckBox;
import javax.swing.JComboBox;
import javax.swing.JComponent;
import javax.swing.JFileChooser;
import javax.swing.JLabel;
import javax.swing.JOptionPane;

import net.sf.rails.common.Config;
import net.sf.rails.common.GameData;
import net.sf.rails.common.GameInfo;
import net.sf.rails.common.GameOption;
import net.sf.rails.common.GameOptionsSet;
import net.sf.rails.common.LocalText;
import net.sf.rails.common.parser.ConfigurationException;
import net.sf.rails.common.parser.GameInfoParser;
import net.sf.rails.common.parser.GameOptionsParser;
import net.sf.rails.game.RailsRoot;
import net.sf.rails.sound.SoundManager;
import net.sf.rails.util.GameLoader;
import net.sf.rails.util.GameSaver;
import net.sf.rails.util.SystemOS;
import net.sf.rails.util.Util;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.google.common.collect.ComparisonChain;
import com.google.common.collect.ImmutableList;
import com.google.common.collect.ImmutableSet;
import com.google.common.collect.Lists;
import com.google.common.collect.Maps;
import com.google.common.collect.Sets;


/** Controller of the GameSetupWindow */
public class GameSetupController {

    private static final Logger log =
            LoggerFactory.getLogger(GameSetupController.class);
    
    private final SortedSet<GameInfo> gameList;
    
    private final String credits;
    
    private final Map<GameInfo, GameOptionsSet.Builder> gameOptions =
            Maps.newHashMap();
            
    // UI references
    private final GameSetupWindow window;
    private ConfigWindow configWindow;
    private GameUIManager gameUIManager;
    
    // Actions
    final ActionListener newAction = new NewAction();
    final ActionListener loadAction = new LoadAction();
    final ActionListener recentAction = new RecentAction();
    final ActionListener recoveryAction = new RecoveryAction();
    final ActionListener quitAction = new QuitAction();
    final ActionListener optionPanelAction = new OptionPanelAction();
    final ActionListener infoAction = new InfoAction();
    final ActionListener creditsAction = new CreditsAction();
    final ActionListener gameAction = new GameAction();
    final ActionListener configureAction = new ConfigureAction();
    final ActionListener randomizeAction = new RandomizeAction();
    final InputVerifier playerNameVerifier = new PlayerNameVerifier();
      
    private GameSetupController(SortedSet<GameInfo> gameList, String credits) {
        this.gameList = gameList;
        this.credits = credits;

        window = new GameSetupWindow(this);

        // Notify the sound manager about having started the setup menu
        SoundManager.notifyOfGameSetup();
    }

    ImmutableList<GameInfo> getGameList() {
        return ImmutableList.copyOf(gameList);
    }

    // Return default game, if none is set, returns the first
    GameInfo getDefaultGame() {
        GameInfo defaultGame = GameInfo.findGame(gameList, Config.get("default_game"));
        if (defaultGame == null) {
            defaultGame = gameList.first();
        }
        return defaultGame;  
    }
    
    GameOptionsSet.Builder getAvailableOptions(GameInfo game) {
        if (!gameOptions.containsKey(game)) {
            return loadOptions(game);
        }
        return gameOptions.get(game);
    }
    
    
    Action getOptionChangeAction(GameOption option) {
        return new OptionChangeAction(option);
    }
    
    private GameOptionsSet.Builder loadOptions(GameInfo game) {
        log.debug("Load Game Options of " + game.getName());
        GameOptionsSet.Builder loadGameOptions = null;
        try {
            loadGameOptions = GameOptionsParser.load(game.getName());
        } catch (ConfigurationException e) {
            log.error(e.getMessage());
            loadGameOptions = GameOptionsSet.builder();
        }
        gameOptions.put(game, loadGameOptions);
        return loadGameOptions;
    }

    private void prepareGameUIInit() {
        window.setVisible(false);
        if (configWindow != null) {
            configWindow.dispose();
            configWindow = null;
        }
    }
    
    private void loadAndStartGame(File gameFile) {
        prepareGameUIInit();
        GameLoader.loadAndStartGame(gameFile);
    }
    
    // Action inner classes
    private class NewAction extends AbstractAction {
        private static final long serialVersionUID = 0L;

        public void actionPerformed(ActionEvent e) {
            //start in new thread so that swing thread is not used for game setup
            new Thread() {
                @Override
                public void run() {
                    startNewGame();
                }
            }.start();
        }
        
        private void startNewGame() {
            
            GameInfo selectedGame = window.getSelectedGame();
            List<String> players = window.getPlayers();
            GameOptionsSet.Builder selectedOptions = getAvailableOptions(selectedGame);

            // check against number of available players
            if (players.size() < selectedGame.getMinPlayers()) {
                if (JOptionPane.showConfirmDialog(window,
                        "Not enough players. Continue Anyway?",
                        "Are you sure?", JOptionPane.YES_NO_OPTION) == JOptionPane.NO_OPTION) {
                    return;
                }
            }

            SplashWindow splashWindow = new SplashWindow(false, selectedGame.getName());
            
            RailsRoot railsRoot = null;
            try {
                GameData gameData = GameData.create(selectedGame, selectedOptions, players);
                railsRoot = RailsRoot.create(gameData);
            } catch (ConfigurationException e) {
                // TODO: Fix this behavior, give more information?
                // Simply exit
                System.exit(-1);
            }

            String startError = railsRoot.start();
            if (startError != null) {
                JOptionPane.showMessageDialog(window, startError, "",
                        JOptionPane.ERROR_MESSAGE);
                System.exit(-1);
            }
            prepareGameUIInit();
            gameUIManager = GameLoader.startGameUIManager (railsRoot, false, splashWindow);
            gameUIManager.gameUIInit(true); // true indicates new game

            gameUIManager.notifyOfSplashFinalization();
            splashWindow.finalizeGameInit();
            splashWindow = null;
        }
    }
    
    private class LoadAction extends AbstractAction {
        private static final long serialVersionUID = 0L;

        public void actionPerformed(ActionEvent e) {
            String saveDirectory = Config.get("save.directory");
            JFileChooser jfc = new JFileChooser();
            jfc.setCurrentDirectory(new File(saveDirectory));

            if (jfc.showOpenDialog(window.getContentPane()) == JFileChooser.APPROVE_OPTION) {
                final File selectedFile = jfc.getSelectedFile();
                //start in new thread so that swing thread is not used for game setup
                new Thread() {
                    @Override
                    public void run() {
                        loadAndStartGame(selectedFile);
                    }
                }.start();
            } else { // cancel pressed
                return;
            }
        }
        
    }
    
    private class RecentAction extends AbstractAction {
        private static final long serialVersionUID = 0L;

        public void actionPerformed(ActionEvent arg0) {
            File saveDirectory = new File(Config.get("save.directory"));

            // define recent files
            SortedSet<File> recentFiles = new TreeSet<File>(new Comparator<File> (){
                public int compare (File a, File b) {
                    return ComparisonChain.start()
                            .compare(b.lastModified(), a.lastModified())
                            .compare(a.getName(), b.getName())
                            .result();
                }
            });
            
            // define saved file extension
            String savedFileExtension = Config.get("save.filename.extension");
            if (!Util.hasValue(savedFileExtension)) {
                savedFileExtension = GameUIManager.DEFAULT_SAVE_EXTENSION;
            }
            savedFileExtension = "." + savedFileExtension;

            // get recent files
            getRecentFiles(recentFiles, saveDirectory, savedFileExtension);
            if (recentFiles == null || recentFiles.size() == 0) return;
            File[] files = recentFiles.toArray(new File[]{});
            
            int numOptions = 20;
            numOptions = Math.min(numOptions, recentFiles.size());
            String[] options = new String[numOptions];
            int dirPathLength = saveDirectory.getPath().length();
            for (int i=0; i<numOptions;i++) {
                // Get path relative to saveDirectory
                options[i] = files[i].getPath().substring(dirPathLength+1);
            }
            String text = LocalText.getText("Select");
            String result = (String) JOptionPane.showInputDialog(window, text, text,
                    JOptionPane.OK_CANCEL_OPTION,
                    null, options, options[0]);
            if (result == null) return;
            final File selectedFile = files[Arrays.asList(options).indexOf(result)];
            if (selectedFile != null) {
                new Thread() {
                    @Override
                    public void run() {
                        loadAndStartGame(selectedFile);
                    }
                }.start();
            } else { // cancel pressed
                return;
            }
            
        }

        private void getRecentFiles (SortedSet<File> recentFiles, File dir, String savedFileExtension) {
            if (!dir.exists() || !dir.isDirectory()) return;
            for (File entry : dir.listFiles()) {
                if (entry.isFile() && entry.getName().endsWith(savedFileExtension)) {
                    recentFiles.add(entry);
                } else if (entry.isDirectory()){
                    getRecentFiles(recentFiles, entry, savedFileExtension);
                }
            }
        }
    }
    
    private class RecoveryAction extends AbstractAction {
        private static final long serialVersionUID = 0L;

        public void actionPerformed(ActionEvent arg0) {
            new Thread() {
                @Override
                public void run() {
                    String filePath = SystemOS.get().getConfigurationFolder(GameSaver.autosaveFolder, true).getAbsolutePath() 
                            + File.separator + GameSaver.autosaveFile;
                    loadAndStartGame(new File(filePath));
                }
            }.start();
        }
    }
    
    private class QuitAction extends AbstractAction {
        private static final long serialVersionUID = 0L;

        public void actionPerformed(ActionEvent arg0) {
            System.exit(0);
        }
    }

    private class OptionPanelAction extends AbstractAction {
        private static final long serialVersionUID = 0L;

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

    private class OptionChangeAction extends AbstractAction {
        private static final long serialVersionUID = 0L;
       
        private final GameOption option;
        
        private OptionChangeAction(GameOption option) {
            this.option = option;
        }

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
            log.debug("GameOption " + option + " set to " + value +  " for game " + game);
        }
    }

    private class InfoAction extends AbstractAction {
        private static final long serialVersionUID = 0L;

        public void actionPerformed(ActionEvent arg0) {
            GameInfo game = window.getSelectedGame();
            JOptionPane.showMessageDialog(window,
                    game.getDescription(),
                    "Information about " + game.getName(),
                    JOptionPane.INFORMATION_MESSAGE);
        }
    }

    private class CreditsAction extends AbstractAction {
        private static final long serialVersionUID = 0L;

        public void actionPerformed(ActionEvent arg0) {
            JOptionPane.showMessageDialog(window,
                    new JLabel(credits), //enable html rendering
                    LocalText.getText("CREDITS"),
                    JOptionPane.INFORMATION_MESSAGE);
        }
    }
    
    private class GameAction extends AbstractAction {
        private static final long serialVersionUID = 0L;

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

        public void actionPerformed(ActionEvent arg0) {
            List<String> players = Lists.newArrayList(window.getPlayers());
            Collections.shuffle(players);
            window.setPlayers(players);
        }
    }
    
    private class PlayerNameVerifier extends InputVerifier {

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

    public static Builder builder() {
        return new Builder();
    }
    
    public static class Builder {
        private SortedSet<GameInfo> gameList = Sets.newTreeSet();
        private String credits = "Credits";
        
        private Builder() {}
        
        public void start() {
            initialize();
            new GameSetupController(gameList, credits);
        }
        
        public void initialize() {
            GameInfoParser gip = new GameInfoParser();
            Set<GameInfo> gameInfoList = ImmutableSet.of();
            try {
                gameInfoList = gip.processGameList();
                credits = gip.getCredits();
            } catch (ConfigurationException e) {
                log.error(e.getMessage());
            }
            // convert list to map
            for (GameInfo game:gameInfoList) {
                gameList.add(game);
            }
        }
    }

}
