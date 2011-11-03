/* $Header: /Users/blentz/rails_rcs/cvs/18xx/rails/game/Game.java,v 1.56 2010/06/06 13:01:00 evos Exp $ */
package rails.game;

import java.io.*;
import java.util.*;

import org.apache.log4j.Logger;

import rails.algorithms.RevenueManager;
import rails.common.DisplayBuffer;
import rails.common.LocalText;
import rails.common.parser.*;
import rails.game.action.PossibleAction;
import rails.util.GameFileIO;

public class Game {
    public static final String version = "1.5.2+";

    /** The component Manager */
    protected GameManager gameManager;
    protected CompanyManagerI companyManager;
    protected PlayerManager playerManager;
    protected PhaseManager phaseManager;
    protected TrainManager trainManager;
    protected StockMarketI stockMarket;
    protected MapManager mapManager;
    protected TileManager tileManager;
    protected RevenueManager revenueManager;
    protected Bank bank;
    protected String name;

    protected List<String> directories = new ArrayList<String>();
    protected List<String> players;

    protected Map<String, String> gameOptions;

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


        this.players = players;

        log.info("========== Start of rails.game " + name + " ==========");
        log.info("Rails version "+version);
        ReportBuffer.add(LocalText.getText("GameIs", name));
    }

    public String start() {

        if (players.size() < playerManager.minPlayers
                || players.size() > playerManager.maxPlayers) {
            return name+" is not configured to be played with "+players.size()+" players\n"
            + "Please enter a valid number of players, or add a <Players> entry to data/"+name+"/Game.xml";
        }

        gameManager.startGame(gameOptions);
        return null;
    }

    public boolean setup() {
        GameFileParser gfp = new GameFileParser(name, gameOptions);
        playerManager = gfp.getPlayerManager();
        companyManager = gfp.getCompanyManager();
        trainManager = gfp.getTrainManager();
        phaseManager = gfp.getPhaseManager();
        stockMarket = gfp.getStockMarket();
        mapManager = gfp.getMapManager();
        tileManager = gfp.getTileManager();
        revenueManager = gfp.getRevenueManager();
        bank = gfp.getBank();
        gameManager = gfp.getGameManager();

        /*
         * Initializations that involve relations between components can
         * only be done after all XML has been processed.
         */
        playerManager.setPlayers(players, bank);
        gameManager.init(name, playerManager, companyManager,
                phaseManager, trainManager, stockMarket, mapManager,
                tileManager, revenueManager, bank);

        try {
            companyManager.finishConfiguration(gameManager);
            trainManager.finishConfiguration(gameManager);
            phaseManager.finishConfiguration(gameManager);
            tileManager.finishConfiguration(gameManager);
            mapManager.finishConfiguration(gameManager);
            bank.finishConfiguration(gameManager);
            stockMarket.finishConfiguration(gameManager);

            if (revenueManager != null)
                revenueManager.finishConfiguration(gameManager);
        } catch (ConfigurationException e) {
            log.fatal(e);
            System.out.println(e.getMessage());
            e.printStackTrace();
            DisplayBuffer.add(e.getMessage());
            return false;
        }

        return true;
    }


    public static Game load(String filepath)  {

        // use GameLoader object to load game
        GameFileIO gameLoader = new GameFileIO();
        gameLoader.loadGameData(filepath);
        try{
            gameLoader.initGame();
            gameLoader.loadActionsAndComments();
        } catch (ConfigurationException e)  {
            log.fatal("Load failed", e);
            DisplayBuffer.add(LocalText.getText("LoadFailed", e.getMessage()));
        }
        try{
            gameLoader.replayGame();
        } catch (Exception e) {
            log.fatal("Replay failed", e);
            DisplayBuffer.add(LocalText.getText("LoadFailed", e.getMessage()));
        }

        return gameLoader.getGame();
    }

    @SuppressWarnings("unchecked")
    public static Game load_old(String filepath) {

        Game game = null;

        log.debug("Loading game from file " + filepath);
        String filename = filepath.replaceAll(".*[/\\\\]", "");

        /*--- Remember to keep GameManager.reload() in sync with this code! ---*/
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

            String startError = game.start();
            if (startError != null) {
                DisplayBuffer.add(startError);
                return null;
            }
            GameManagerI gameManager = game.getGameManager();

            log.debug("Starting to execute loaded actions");

            gameManager.setReloading(true);

            Object actionObject = null;
            int actionIndex = 0;

            while (true) { // Single-pass loop.
                try {
                    actionObject = ois.readObject();
                } catch (EOFException e) {
                    // Allow saved file at start of game (with no actions).
                    break;
                }
                if (actionObject instanceof List) {
                    // Old-style: one List of PossibleActions
                    List<PossibleAction> executedActions =
                        (List<PossibleAction>) actionObject;
                    for (PossibleAction action : executedActions) {
                        ++actionIndex;
                        try {
                            if (!gameManager.processOnReload(action)) {
                                log.error ("Load interrupted");
                                DisplayBuffer.add(LocalText.getText("LoadInterrupted"));
                                break;
                            }
                        } catch (Exception e) {
                            log.fatal("Action "+actionIndex+" '"+action+"' reload exception", e);
                            throw new Exception ("Reload exception", e);
                        }
                    }
                } else if (actionObject instanceof PossibleAction) {
                    // New style: separate PossibleActionsObjects, since Rails 1.3.1
                    while (actionObject instanceof PossibleAction) {
                        ++actionIndex;
                        try {
                            if (!gameManager.processOnReload((PossibleAction)actionObject)) {
                                log.error ("Load interrupted");
                                DisplayBuffer.add(LocalText.getText("LoadInterrupted"));
                                break;
                            }
                        } catch (Exception e) {
                            log.fatal("Action "+actionIndex+" '"+((PossibleAction)actionObject).toString()
                                    +"' reload exception", e);
                            throw new Exception ("Reload exception", e);
                        }
                        try {
                            actionObject = ois.readObject();
                        } catch (EOFException e) {
                            break;
                        }
                    }
                }
                break;
            }

            // load user comments (is the last
            if (actionObject instanceof SortedMap) {
                ReportBuffer.setCommentItems((SortedMap<Integer, String>) actionObject);
                log.debug("Found sorted map");
            } else {
                try {
                    object = ois.readObject();
                    if (object instanceof SortedMap) {
                        ReportBuffer.setCommentItems((SortedMap<Integer, String>) object);
                    }
                } catch (IOException e) {
                    // continue without comments, if any IOException occurs
                    // sometimes not only the EOF Exception is raised
                    // but also the java.io.StreamCorruptedException: invalid type code
                }
            }

            ois.close();
            ois = null;

            gameManager.setReloading(false);
            gameManager.finishLoading();
            return game;

        } catch (Exception e) {
            log.fatal("Load failed", e);
            DisplayBuffer.add(LocalText.getText("LoadFailed", e.getMessage()));
        }

        return null;
    }

    /*----- Getters -----*/

    public GameManagerI getGameManager() {
        return gameManager;
    }

}
