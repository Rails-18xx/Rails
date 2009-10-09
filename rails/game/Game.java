/* $Header: /Users/blentz/rails_rcs/cvs/18xx/rails/game/Game.java,v 1.32 2009/10/09 22:29:01 evos Exp $ */
package rails.game;

import java.io.*;
import java.util.*;

import org.apache.log4j.Logger;

import rails.game.action.PossibleAction;
import rails.util.LocalText;
import rails.util.Tag;

public class Game {
    public static final String version = "1.0.5";

    /**
     * Game is a singleton class.
     */
    protected static Game instance;

    /** The component Manager */
    protected ComponentManager componentManager;
    protected GameManager gameManager;
    protected CompanyManagerI companyManager;
    protected PlayerManager playerManager;
    protected PhaseManager phaseManager;
    protected TrainManager trainManager;
    protected StockMarketI stockMarket;
    protected MapManager mapManager;
    protected TileManager tileManager;
    protected Bank bank;
    // protected ArrayList companyList;
    protected String name;
    // protected Element componentManagerElement;
    protected Tag componentManagerTag;
    protected static String GAME_XML_FILE = "Game.xml";
    protected List<String> directories = new ArrayList<String>();
    protected Map<String, String> gameOptions;

    protected List<String> players;

    protected static Logger log =
            Logger.getLogger(Game.class.getPackage().getName());

    // The new Game entry point
    public Game(String name, List<String> players, Map<String, String> options) {

        instance = this;

        this.name = name;
        this.gameOptions = options;

        gameOptions.put(GameOption.NUMBER_OF_PLAYERS,
                String.valueOf(players.size()));

        for (String playerName : players) {
            log.debug("Player: " + playerName);
        }
        for (String optionName : gameOptions.keySet()) {
            log.debug("Option: " + optionName + "="
                      + gameOptions.get(optionName));
        }
        directories.add("data");
        directories.add("data/" + name);

        //playerManager = new PlayerManager(players);
        this.players = players;
    }

    public void start() {
        gameManager.startGame();
    }

    public boolean setup() {

        ReportBuffer.add(LocalText.getText("GameIs", name));

        try {
            // Have the ComponentManager work through the other rails.game files
            // componentManagerTag = XmlUtils.findElementInFile(GAME_XML_FILE,
            // directories,
            // ComponentManager.ELEMENT_ID);
            componentManagerTag =
                    Tag.findTopTagInFile(GAME_XML_FILE, directories,
                            ComponentManager.ELEMENT_ID);
            if (componentManagerTag == null) {
                throw new ConfigurationException(
                        "No Game XML element found in file " + GAME_XML_FILE);
            }

            ComponentManager.configureInstance(name, componentManagerTag);

            componentManager = ComponentManager.getInstance();

            log.info("========== Start of rails.game " + name + " ==========");

            // Have the ComponentManager work through the other rails.game files
            componentManager.finishPreparation();

            playerManager = (PlayerManager) componentManager.findComponent("PlayerManager");
            if (playerManager == null) {
                throw new ConfigurationException(
                        "No PlayerManager XML element found in file " + GAME_XML_FILE);
            }

            bank = (Bank) componentManager.findComponent("Bank");
            if (bank == null) {
                throw new ConfigurationException(
                        "No Bank XML element found in file " + GAME_XML_FILE);
            }

            companyManager =
                    (CompanyManagerI) componentManager.findComponent(CompanyManagerI.COMPONENT_NAME);
            if (companyManager == null) {
                throw new ConfigurationException(
                        "No CompanyManager XML element found in file "
                                + GAME_XML_FILE);
            }
            stockMarket =
                    (StockMarketI) componentManager.findComponent(StockMarketI.COMPONENT_NAME);
            if (stockMarket == null) {
                throw new ConfigurationException(
                        "No StockMarket XML element found in file "
                                + GAME_XML_FILE);
            }
            gameManager =
                    (GameManager) componentManager.findComponent("GameManager");
            if (gameManager == null) {
                throw new ConfigurationException(
                        "No GameManager XML element found in file "
                                + GAME_XML_FILE);
            }

            phaseManager =
                (PhaseManager) componentManager.findComponent("PhaseManager");
            if (phaseManager == null) {
                throw new ConfigurationException(
                        "No PhaseManager XML element found in file "
                                + GAME_XML_FILE);
            }

            trainManager =
                (TrainManager) componentManager.findComponent("TrainManager");
            if (trainManager == null) {
                throw new ConfigurationException(
                        "No TrainManager XML element found in file "
                                + GAME_XML_FILE);
            }

            mapManager =
                (MapManager) componentManager.findComponent("Map");
            if (mapManager == null) {
                throw new ConfigurationException(
                        "No Map XML element found in file "
                                + GAME_XML_FILE);
            }

            tileManager =
                (TileManager) componentManager.findComponent("TileManager");
            if (tileManager == null) {
                throw new ConfigurationException(
                        "No TileManager XML element found in file "
                                + GAME_XML_FILE);
            }

            /*
             * Initialisations that involve relations between components can
             * only be done after all XML has been processed.
             */
            playerManager.setPlayers(players, bank);
            gameManager.init(playerManager, companyManager,
                    phaseManager, trainManager, stockMarket, mapManager, 
                    tileManager, bank);

            companyManager.finishConfiguration(gameManager);
            trainManager.finishConfiguration(gameManager);
            phaseManager.finishConfiguration(gameManager);
            mapManager.finishConfiguration(gameManager);
            bank.finishConfiguration(gameManager);
            //StartPacket.init();
            //companyManager.initCompanies(gameManager);
            stockMarket.finishConfiguration(gameManager);
        } catch (Exception e) {
            String message =
                    LocalText.getText("GameSetupFailed", GAME_XML_FILE);
            log.fatal(message, e);
            DisplayBuffer.add(message + ":\n " + e.getMessage());
            return false;
        }

        return true;
    }

    @SuppressWarnings("unchecked")
    public static Game load(String filepath) {

        Game game = null;

        log.debug("Loading game from file " + filepath);

        try {
            ObjectInputStream ois =
                    new ObjectInputStream(new FileInputStream(
                            new File(filepath)));
            long versionID = (Long) ois.readObject();
            long saveFileVersionID = GameManager.saveFileVersionID;
            if (versionID != saveFileVersionID) {
                throw new Exception("Save version " + versionID
                                    + " is incompatible with current version "
                                    + saveFileVersionID);
            }
            String name = (String) ois.readObject();
            Map<String, String> selectedGameOptions =
                    (Map<String, String>) ois.readObject();
            List<String> playerNames = (List<String>) ois.readObject();

            game = new Game(name, playerNames, selectedGameOptions);

            if (!game.setup()) {
                throw new ConfigurationException("Error in setting up " + name);
            }

            List<PossibleAction> executedActions =
                    (List<PossibleAction>) ois.readObject();
            ois.close();
            log.debug("Number of loaded actions: " + executedActions.size());

            game.start();

            log.debug("Starting to execute loaded actions");

            instance.gameManager.processOnReload(executedActions);

            return game;

        } catch (Exception e) {
            log.error("Load failed", e);
            DisplayBuffer.add(LocalText.getText("LoadFailed", e.getMessage()));
        }

        return null;
    }

    /*----- Getters -----*/

    public static String getGameOption(String optionName) {
        return instance.gameOptions.get(optionName);
    }

    public static Map<String, String> getGameOptions() {
        return instance.gameOptions;
    }

    /**
     * @return The company manager
     */
    public static CompanyManagerI getCompanyManager() {
        return instance.companyManager;
    }

    /**
     * @return The company manager
     */
    public static StockMarketI getStockMarket() {
        return instance.stockMarket;
    }

    public GameManagerI getGameManager() {
        return gameManager;
    }

    /**
     * @return Game Name
     */
    public static String getName() {
        return instance.name;
    }
}
