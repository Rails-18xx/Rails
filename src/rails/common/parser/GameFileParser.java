package rails.common.parser;

import java.util.Map;

import rails.algorithms.RevenueManager;
import rails.common.DisplayBuffer;
import rails.common.LocalText;
import rails.common.parser.ComponentManager;
import rails.game.Bank;
import rails.game.CompanyManager;
import rails.game.GameManager;
import rails.game.MapManager;
import rails.game.PhaseManager;
import rails.game.PlayerManager;
import rails.game.StockMarket;
import rails.game.TileManager;
import rails.game.TrainManager;
import rails.game.state.Manager;

public class GameFileParser extends XMLParser {
    private static String GAME_XML_FILE = "Game.xml";
    
    private Tag componentManagerTag;

    private ComponentManager componentManager;
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
    
    public GameFileParser(Manager context, String name, Map<String, String> gameOptions) {
        
        directories.add("data/" + name);
        
        try {
            componentManagerTag = Tag.findTopTagInFile(GAME_XML_FILE, directories, XMLTags.COMPONENT_MANAGER_ELEMENT_ID);
            componentManagerTag.setGameOptions(gameOptions);
            
            //XXX: Ultimately calls everyone's configureFromXML() methods.
            componentManager = new ComponentManager(context, name, componentManagerTag, gameOptions);
            
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
        } catch (Exception e) {
            String message =
                    LocalText.getText("GameSetupFailed", GAME_XML_FILE);
            log.error(message, e);
            System.out.println(e.getMessage());
            e.printStackTrace();
            DisplayBuffer.add(message + ":\n " + e.getMessage());
        }
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



    
