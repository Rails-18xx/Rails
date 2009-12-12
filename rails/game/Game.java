/* $Header: /Users/blentz/rails_rcs/cvs/18xx/rails/game/Game.java,v 1.41 2009/12/12 22:05:20 wakko666 Exp $ */
package rails.game;

import java.io.*;
import java.util.*;

import org.apache.log4j.Logger;

import rails.game.action.PossibleAction;
import rails.util.LocalText;
import rails.util.Tag;

public class Game {
    public static final String version = "1.1.0";

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
    protected String name;
    protected Tag componentManagerTag;
    protected static String GAME_XML_FILE = "Game.xml";
    protected List<String> directories = new ArrayList<String>();
    protected Map<String, String> gameOptions;

    protected List<String> players;

    protected static Logger log =
            Logger.getLogger(Game.class.getPackage().getName());

    // The new Game entry point
    public Game(String name, List<String> players, Map<String, String> options) {

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

        this.players = players;
    }

    public String  start() {

    	if (players.size() < playerManager.minPlayers
    			|| players.size() > playerManager.maxPlayers) {
    		return name+" is not configured to be played with "+players.size()+" players\n"
    				+ "Please enter a valid number of players, or add a <Players> entry to data/"+name+"/Game.xml";
    	}

        gameManager.startGame(gameOptions);
        return null;
    }

    public boolean setup() {

        try {
            // Have the ComponentManager work through the other rails.game files
            componentManagerTag =
                    Tag.findTopTagInFile(GAME_XML_FILE, directories,
                            ComponentManager.ELEMENT_ID);
            if (componentManagerTag == null) {
                throw new ConfigurationException(
                        "No Game XML element found in file " + GAME_XML_FILE);
            }

            ComponentManager.configureInstance(name, componentManagerTag, gameOptions);

            componentManager = ComponentManager.getInstance();

            log.info("========== Start of rails.game " + name + " ==========");
            log.info("Rails version "+version);
            ReportBuffer.add(LocalText.getText("GameIs", name));

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
            gameManager.init(name, playerManager, companyManager,
                    phaseManager, trainManager, stockMarket, mapManager,
                    tileManager, bank);

            companyManager.finishConfiguration(gameManager);
            trainManager.finishConfiguration(gameManager);
            phaseManager.finishConfiguration(gameManager);
            mapManager.finishConfiguration(gameManager);
            bank.finishConfiguration(gameManager);
            stockMarket.finishConfiguration(gameManager);
            tileManager.finishConfiguration(gameManager);
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
        String filename = filepath.replaceAll(".*[/\\\\]", "");

        try {
            ObjectInputStream ois =
                    new ObjectInputStream(new FileInputStream(
                            new File(filepath)));

            // New in 1.0.7: Rails version & save date/time.
            // Allow for older saved file versions.
            Object object = ois.readObject();
            if (object instanceof String) {
            	log.info("Reading Rails "+(String)object+" saved file "+filename);
            	object = ois.readObject();
            } else {
            	log.info("Reading Rails (pre-1.0.7) saved file "+filename);
            }
            if (object instanceof String) {
            	log.info("File was saved at "+(String)object);
            	object = ois.readObject();
            }

            long versionID = (Long) object;
            log.debug("Saved versionID="+versionID+" (object="+object+")");
            long saveFileVersionID = GameManager.saveFileVersionID;
            if (versionID != saveFileVersionID) {
                throw new Exception("Save version " + versionID
                                    + " is incompatible with current version "
                                    + saveFileVersionID);
            }
            String name = (String) ois.readObject();
            log.debug("Saved game="+name);
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

            String startError = game.start();
            if (startError != null) {
                DisplayBuffer.add(startError);
                return null;
            }

            log.debug("Starting to execute loaded actions");

            if (!game.getGameManager().processOnReload(executedActions)) {
                log.error ("Load interrupted");
                DisplayBuffer.add(LocalText.getText("LoadInterrupted"));
            }

            return game;

        } catch (Exception e) {
            log.error("Load failed", e);
            DisplayBuffer.add(LocalText.getText("LoadFailed", e.getMessage()));
        }

        return null;
    }

    /*----- Getters -----*/

    public GameManagerI getGameManager() {
        return gameManager;
    }

}
