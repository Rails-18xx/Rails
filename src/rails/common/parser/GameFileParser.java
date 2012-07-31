package rails.common.parser;

import java.util.Map;

import rails.algorithms.RevenueManager;
import rails.common.parser.ComponentManager;
import rails.game.Bank;
import rails.game.CompanyManager;
import rails.game.GameManager;
import rails.game.GameRoot;
import rails.game.MapManager;
import rails.game.PhaseManager;
import rails.game.PlayerManager;
import rails.game.StockMarket;
import rails.game.TileManager;
import rails.game.TrainManager;

public class GameFileParser extends XMLParser {
    public static final String GAME_XML_FILE = "Game.xml";
    
    private Tag componentManagerTag;

    private final ComponentManager componentManager;
    private final GameManager gameManager;
    private final CompanyManager companyManager;
    private final PlayerManager playerManager;
    private final PhaseManager phaseManager;
    private final TrainManager trainManager;
    private final StockMarket stockMarket;
    private final MapManager mapManager;
    private final TileManager tileManager;
    private final RevenueManager revenueManager;
    private final Bank bank;
    
    public GameFileParser(GameRoot root, String name, Map<String, String> gameOptions) 
        throws ConfigurationException {
        
        directories.add("data/" + name);
        
        componentManagerTag = Tag.findTopTagInFile(GAME_XML_FILE, directories, XMLTags.COMPONENT_MANAGER_ELEMENT_ID);
        componentManagerTag.setGameOptions(gameOptions);
        
        componentManager = new ComponentManager(root, name, componentManagerTag, gameOptions);
        
        playerManager = (PlayerManager) componentManager.findComponent("PlayerManager");
        bank = (Bank) componentManager.findComponent("Bank");
        companyManager = (CompanyManager) componentManager.findComponent(CompanyManager.COMPONENT_NAME);
        stockMarket = (StockMarket) componentManager.findComponent(StockMarket.COMPONENT_NAME);
        gameManager = (GameManager) componentManager.findComponent("GameManager");
        phaseManager = (PhaseManager) componentManager.findComponent("PhaseManager");
        trainManager = (TrainManager) componentManager.findComponent("TrainManager");
        mapManager = (MapManager) componentManager.findComponent("Map");
        tileManager = (TileManager) componentManager.findComponent("TileManager");
        revenueManager = (RevenueManager) componentManager.findComponent("RevenueManager");
    }
    
    /**
     * @return the componentManager
     */
    public ComponentManager getComponentManager() {
        return componentManager;
    }

    /**
     * @return the gameManager
     */
    public GameManager getGameManager() {
        return gameManager;
    }

    /**
     * @return the companyManager
     */
    public CompanyManager getCompanyManager() {
        return companyManager;
    }

    /**
     * @return the playerManager
     */
    public PlayerManager getPlayerManager() {
        return playerManager;
    }

    /**
     * @return the phaseManager
     */
    public PhaseManager getPhaseManager() {
        return phaseManager;
    }

    /**
     * @return the trainManager
     */
    public TrainManager getTrainManager() {
        return trainManager;
    }

    /**
     * @return the stockMarket
     */
    public StockMarket getStockMarket() {
        return stockMarket;
    }

    /**
     * @return the mapManager
     */
    public MapManager getMapManager() {
        return mapManager;
    }

    /**
     * @return the tileManager
     */
    public TileManager getTileManager() {
        return tileManager;
    }

    /**
     * @return the revenueManager
     */
    public RevenueManager getRevenueManager() {
        return revenueManager;
    }

    /**
     * @return the bank
     */
    public Bank getBank() {
        return bank;
    }
}



    
