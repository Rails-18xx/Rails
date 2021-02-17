package net.sf.rails.game;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import net.sf.rails.algorithms.RevenueManager;
import net.sf.rails.common.Config;
import net.sf.rails.common.DisplayBuffer;
import net.sf.rails.common.GameData;
import net.sf.rails.common.GameOptionsSet;
import net.sf.rails.common.LocalText;
import net.sf.rails.common.ReportBuffer;
import net.sf.rails.common.ReportManager;
import net.sf.rails.common.ResourceLoader;
import net.sf.rails.common.parser.ComponentManager;
import net.sf.rails.common.parser.Configurable;
import net.sf.rails.common.parser.ConfigurationException;
import net.sf.rails.common.parser.Tag;
import net.sf.rails.common.parser.XMLTags;
import net.sf.rails.game.financial.Bank;
import net.sf.rails.game.financial.StockMarket;
import net.sf.rails.game.state.Root;

public class RailsRoot extends Root implements RailsItem {

    private static final Logger log = LoggerFactory.getLogger(RailsRoot.class);

    // Base XML file
    private static final String GAME_XML_FILE = "Game.xml";

    // Instance fields

    // Game data fields
    private final GameData gameData;

    // Component Managers
    private GameManager gameManager;
    private CompanyManager companyManager;
    private PlayerManager playerManager;
    private PhaseManager phaseManager;
    private TrainManager trainManager;
    private StockMarket stockMarket;
    private MapManager mapManager;
    private TileManager tileManager;
    private RevenueManager revenueManager;
    private Bank bank;
    private CertificateManager certificateManager;
    private PortfolioManager portfolioManager;

    // Other Managers
    private ReportManager reportManager;

    private RailsRoot(GameData gameData) {
        super();

        for (String playerName : gameData.getPlayers()) {
            log.debug("Player: {}", playerName);
        }
        for (String optionName : gameData.getGameOptions().getOptions().keySet()) {
            log.debug("Option: {}={}", optionName, gameData.getGameOptions().get(optionName));
        }

        this.gameData = gameData;
    }

    public static RailsRoot create(GameData gameData) throws ConfigurationException {
        RailsRoot root = new RailsRoot(gameData);
        log.debug("RailsRoot: instance created");
        root.init();
        log.debug("RailsRoot: instance initialized");
        root.initGameFromXML();
        log.debug("RailsRoot: game configuration initialized");
        root.finishConfiguration();
        log.debug("RailsRoot: game configuration finished");

        return root;
    }


    // feedback from ComponentManager
    public void setComponent(Configurable component) {
        if (component instanceof PlayerManager) {
            playerManager = (PlayerManager) component;
        } else if (component instanceof Bank) {
            bank = (Bank) component;
        } else if (component instanceof CompanyManager) {
            companyManager = (CompanyManager) component;
        } else if (component instanceof StockMarket) {
            stockMarket = (StockMarket) component;
        } else if (component instanceof GameManager) {
            gameManager = (GameManager) component;
        } else if (component instanceof PhaseManager) {
            phaseManager = (PhaseManager) component;
        } else if (component instanceof TrainManager) {
            trainManager = (TrainManager) component;
        } else if (component instanceof MapManager) {
            mapManager = (MapManager) component;
        } else if (component instanceof TileManager) {
            tileManager = (TileManager) component;
        } else if (component instanceof RevenueManager) {
            revenueManager = (RevenueManager) component;
        } else if (component instanceof CertificateManager) {
            certificateManager = (CertificateManager) component;
        } else if (component instanceof PortfolioManager) {
            portfolioManager = (PortfolioManager) component;
        }
    }

    private void initGameFromXML() throws ConfigurationException {
        String directory = "data" + ResourceLoader.SEPARATOR + gameData.getGameName();

        Tag componentManagerTag = Tag.findTopTagInFile(
                GAME_XML_FILE, directory, XMLTags.COMPONENT_MANAGER_ELEMENT_ID, gameData.getGameOptions() );

        ComponentManager componentManager = new ComponentManager();
        componentManager.start(this,  componentManagerTag);
        // The componentManager automatically returns results
    }

    private boolean finishConfiguration() {
        // creation of Report facilities
        reportManager = new ReportManager(this, "reportManager");

        /*
         * Initializations that involve relations between components can
         * only be done after all XML has been processed.
         */
        log.info("========== Start of rails.game {} ==========", gameData.getGameName());
        log.info("Rails version {}", Config.getVersion());
        ReportBuffer.add(this, LocalText.getText("GameIs", gameData.getGameName()));

        playerManager.initPlayers(gameData.getPlayers(), bank);
        gameManager.init();

        if ( certificateManager == null ) {
            certificateManager = new CertificateManager(this, "CertificateManager");
        }
        if ( portfolioManager == null ) {
            portfolioManager = new PortfolioManager(this, "PortfolioManager");
        }

        try {
            playerManager.finishConfiguration(this);
            companyManager.finishConfiguration(this); // Requires bank to be finished later!
            trainManager.finishConfiguration(this);
            phaseManager.finishConfiguration(this);
            tileManager.finishConfiguration(this);
            mapManager.finishConfiguration(this);
            bank.finishConfiguration(this);
            stockMarket.finishConfiguration(this);

            if (revenueManager != null)
                revenueManager.finishConfiguration(this);
        } catch (ConfigurationException e) {
            log.error(e.getMessage(), e);
            DisplayBuffer.add(this, e.getMessage());
            return false;
        }

        return true;
    }

    public String start() {
        // FIXME (Rails2.0): Should this not be part of the initial Setup configuration?
        int nbPlayers = gameData.getPlayers().size();
        if (nbPlayers < playerManager.getMinPlayers()
                || nbPlayers > playerManager.getMaxPlayers()) {
            return gameData.getGameName() +" is not configured to be played with "+ nbPlayers +" players\n"
                    + "Please enter a valid number of players, or add a <Players> entry to data/"+ gameData.getGameName() +"/Game.xml";
        }

        gameManager.startGame();
        return null;
    }

    /*----- Getters -----*/

    public GameManager getGameManager() {
        return gameManager;
    }

    public CompanyManager getCompanyManager() {
        return companyManager;
    }

    public PlayerManager getPlayerManager() {
        return playerManager;
    }

    public PhaseManager getPhaseManager() {
        return phaseManager;
    }

    public TrainManager getTrainManager() {
        return trainManager;
    }

    public StockMarket getStockMarket() {
        return stockMarket;
    }

    public MapManager getMapManager() {
        return mapManager;
    }

    public TileManager getTileManager() {
        return tileManager;
    }

    public RevenueManager getRevenueManager() {
        return revenueManager;
    }

    public Bank getBank() {
        return bank;
    }

    public ReportManager getReportManager() {
        return reportManager;
    }

    public CertificateManager getCertificateManager() {
        return certificateManager;
    }

    public PortfolioManager getPortfolioManager() {
        return portfolioManager;
    }

    /**
     * @return the gameName
     */
    public String getGameName() {
        return gameData.getGameName();
    }

    /**
     * @return the gameOptions
     */
    public GameOptionsSet getGameOptions() {
        return gameData.getGameOptions();
    }

    /**
     * @return the gameData
     */
    public GameData getGameData() {
        return gameData;
    }

    @Override
    public RailsRoot getParent() {
        throw new UnsupportedOperationException();
    }

    @Override
    public RailsRoot getRoot() {
        return this;
    }

}
