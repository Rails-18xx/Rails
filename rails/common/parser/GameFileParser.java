package rails.common.parser;

import java.util.Map;

import org.w3c.dom.Document;
import org.w3c.dom.Element;

import rails.algorithms.RevenueManager;
import rails.common.DisplayBuffer;
import rails.common.LocalText;
import rails.common.parser.ComponentManager;
import rails.game.Bank;
import rails.game.CompanyManagerI;
import rails.game.GameManager;
import rails.game.MapManager;
import rails.game.PhaseManager;
import rails.game.PlayerManager;
import rails.game.StockMarketI;
import rails.game.TileManager;
import rails.game.TrainManager;
import rails.game.state.Context;

public class GameFileParser extends XMLParser {
    private static String GAME_XML_FILE = "Game.xml";
    
    private Tag componentManagerTag;

    private ComponentManager componentManager;
    private GameManager gameManager;
    private CompanyManagerI companyManager;
    private PlayerManager playerManager;
    private PhaseManager phaseManager;
    private TrainManager trainManager;
    private StockMarketI stockMarket;
    private MapManager mapManager;
    private TileManager tileManager;
    private RevenueManager revenueManager;
    private Bank bank;
    
    public GameFileParser(Context context, String name, Map<String, String> gameOptions) {
        
        directories.add("data/" + name);
        
        try {
            componentManagerTag = Tag.findTopTagInFile(GAME_XML_FILE, directories, XMLTags.COMPONENT_MANAGER_ELEMENT_ID);
            componentManagerTag.setGameOptions(gameOptions);
            
            //XXX: Ultimately calls everyone's configureFromXML() methods.
            componentManager = new ComponentManager(context, name, componentManagerTag, gameOptions);
            
            playerManager = (PlayerManager) componentManager.findComponent("PlayerManager");
            bank = (Bank) componentManager.findComponent("Bank");
            companyManager = (CompanyManagerI) componentManager.findComponent(CompanyManagerI.COMPONENT_NAME);
            stockMarket = (StockMarketI) componentManager.findComponent(StockMarketI.COMPONENT_NAME);
            gameManager = (GameManager) componentManager.findComponent("GameManager");
            phaseManager = (PhaseManager) componentManager.findComponent("PhaseManager");
            trainManager = (TrainManager) componentManager.findComponent("TrainManager");
            mapManager = (MapManager) componentManager.findComponent("Map");
            tileManager = (TileManager) componentManager.findComponent("TileManager");
            revenueManager = (RevenueManager) componentManager.findComponent("RevenueManager");
        } catch (Exception e) {
            String message =
                    LocalText.getText("GameSetupFailed", GAME_XML_FILE);
            log.fatal(message, e);
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
    public CompanyManagerI getCompanyManager() {
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
    public StockMarketI getStockMarket() {
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



    
