/* $Header: /Users/blentz/rails_rcs/cvs/18xx/rails/game/Game.java,v 1.25 2009/09/06 12:27:31 evos Exp $ */
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
    protected GameManagerI gameManager;
    protected CompanyManagerI companyManager;
    protected PlayerManager playerManager;
    protected PhaseManager phaseManager;
    protected TrainManagerI trainManager;
    protected StockMarketI stockMarket;
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
        gameManager.startGame(playerManager, companyManager,
                phaseManager, trainManager, stockMarket);
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
                    (GameManagerI) componentManager.findComponent("GameManager");
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

            /*
             * Initialisations that involve relations between components can
             * only be done after all XML has been processed.
             */
            playerManager.setPlayers(players, playerManager.getStartCash());

            companyManager.initCompanies(gameManager);
            bank.initCertificates();
            StartPacket.init();
            //companyManager.initCompanies(gameManager);
            stockMarket.init();
        } catch (Exception e) {
            String message =
                    LocalText.getText("GameSetupFailed", GAME_XML_FILE);
            log.fatal(message, e);
            DisplayBuffer.add(message + ":\n " + e.getMessage());
            return false;
        }

        // We need to do this assignment after we've loaded all the XML data.
        MapManager.assignHomesAndDestinations();

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

    /**
     * @return The compinent manager (maybe this getter is not needed)
     */
    public static ComponentManager getComponentManager() {
        return instance.componentManager;
    }

    /* Do the Bank properly later */
    public static Bank getBank() {
        return instance.bank;
    }

    /**
     * @return Returns the playerManager.
     */
    public static PlayerManager getPlayerManager() {
        return instance.playerManager;
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

    public static Logger getLogger() {
        return log;
    }
}
