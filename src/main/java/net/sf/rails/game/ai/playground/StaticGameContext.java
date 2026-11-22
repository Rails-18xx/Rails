package net.sf.rails.game.ai.playground;

// [FIX] Import the REAL GameContext from the correct package
import net.sf.rails.game.ai.GameContext;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

// [FIX] Import REAL data classes
import net.sf.rails.game.ai.TileLayOption;
import net.sf.rails.game.ai.TokenLayOption;
import net.sf.rails.algorithms.RevenueAdapter;
// [FIX] Import REAL game objects (will be null, but needed for interface)
import net.sf.rails.game.GameManager;
import net.sf.rails.game.MapHex;
import net.sf.rails.game.Phase;
import net.sf.rails.game.Player;
import net.sf.rails.game.PrivateCompany; // --- [FIX] Added import ---
import net.sf.rails.game.PublicCompany;
import net.sf.rails.game.Tile;
import net.sf.rails.game.Train;
import net.sf.rails.algorithms.RevenueAdapter;
import java.util.ArrayList;
import java.util.List;
import net.sf.rails.game.Player;
import net.sf.rails.game.PrivateCompany; // Or path to your PrivateCompany class
import net.sf.rails.game.GameManager;

import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import net.sf.rails.game.Train;
import java.util.ArrayList;
import net.sf.rails.game.ai.BuyTrainOption;
import java.util.ArrayList;
import net.sf.rails.game.ai.TileLayOption;
import net.sf.rails.game.ai.playground.MockRevenueAdapter;
import net.sf.rails.game.RailsRoot;
import rails.game.action.BuyTrain;
import java.util.ArrayList;
import java.util.List;

/**
 * A static implementation of GameContext that holds parsed state data.
 * This is for the test harness ONLY.
 * [FIX] Correctly implements the GameContext interface.
 */
public class StaticGameContext implements GameContext {

    private List<BuyTrainOption> buyTrainOptions = new ArrayList<>();
    // Fields to hold the parsed state for the operating company
    private int opCompanyCash = 0;
    private int opCompanyTrainCount = 0;
    private int opCompanyTrainLimit = 0;
    private Set<Train> availableTrains = new HashSet<>(); // Mocked as empty for now
    private PublicCompany operatingCompany = null; // Stays null
    private String operatingCompanyId = null;
    private List<TileLayOption> tileLayOptions = new ArrayList<>();

    // --- Meta Info ---
    private String currentPlayerId;
    private String currentPhaseValue; // For parser
    private String round;

    private RailsRoot mockRailsRoot;
    private List<BuyTrainOption> parsedBuyTrainOptions = new ArrayList<>();

    // --- State Data Maps ---
    private Map<String, String> hexMap = new HashMap<>();
    private Map<String, Integer> availableTiles = new HashMap<>();
    private Map<String, String> playerData = new HashMap<>();
    private Map<String, String> companyData = new HashMap<>();
    private Map<String, String> privateData = new HashMap<>();

    // --- Legal Moves ---
    private List<TileLayOption> legalTileLays = new ArrayList<>();
    private List<TokenLayOption> legalTokenLays = new ArrayList<>();

    // --- Getters for REAL Interface (all are stubs) ---

    public StaticGameContext(RailsRoot root) {
        this.mockRailsRoot = root;
    }

    public void setRailsRoot(RailsRoot root) {
        this.mockRailsRoot = root;
    }

    public RailsRoot getRailsRoot() {
        if (this.mockRailsRoot == null) {
            System.err.println("StaticGameContext ERROR: RailsRoot has not been set!");
        }
        return this.mockRailsRoot;
    }

    public void addParsedBuyTrainOption(BuyTrainOption option) {
        this.parsedBuyTrainOptions.add(option);
    }

    public List<BuyTrainOption> getLegalTrainBuyOptions() {
        return this.parsedBuyTrainOptions;
    }

    @Override
    public GameManager getGameManager() {
        return null;
    }

    @Override
    public Player getCurrentPlayer() {
        return null;
    }

    @Override
    public Phase getCurrentPhase() {
        return null;
    }

    @Override
    public int getPlayerCash(Player player) {
        return 0;
    }

    @Override
    public int getPlayerCertificateLimit(Player player) {
        return 0;
    }

    @Override
    public int getCompanySharePrice(PublicCompany company) {
        return 0;
    }

    @Override
    public boolean companyHasFloated(PublicCompany company) {
        return false;
    }

    @Override
    public MapHex getMapHex(String hexId) {
        return null;
    }

    @Override
    public Tile getTile(String tileId) {
        return null;
    }

    @Override
    public int getTileAvailableCount(String tileId) {
        return 0;
    }

    @Override
    public boolean isHexOpen(String hexId) {
        return false;
    }

    @Override
    public RevenueAdapter getRevenueAdapter() {
        return new MockRevenueAdapter();
    }

    @Override
    public Set<Train> getAvailableTrains() {
        return new HashSet<Train>(); // Return empty set
    }

    // --- Getters for Static Data (used by parser) ---

    @Override
    public PublicCompany getOperatingCompany() {
        // [FIX] Try to find the company in the mock root if it's null
        if (this.operatingCompany == null && this.operatingCompanyId != null && this.mockRailsRoot != null) {
            this.operatingCompany = this.mockRailsRoot.getCompanyManager().getPublicCompany(this.operatingCompanyId);
        }
        return this.operatingCompany;
    }

    @Override
    public int getCompanyCash(PublicCompany company) {
        return this.opCompanyCash;
    }

    @Override
    public int getCompanyTrainLimit(PublicCompany company) {
        return this.opCompanyTrainLimit;
    }

    @Override
    public int getAbsoluteORNumber() {
        return 1; // Mocked for the playground
    }

    @Override
    public int getCompanyTrainCount(PublicCompany company) {
        return this.opCompanyTrainCount;
    }

    // --- [FIX] ADDED MISSING INTERFACE METHODS ---
    @Override
    public Player getPresident(PublicCompany company) {
        return null; // Stub
    }

    @Override
    public PublicCompany getPublicCompany(String companyId) {
        if (mockRailsRoot == null)
            return null;
        return mockRailsRoot.getCompanyManager().getPublicCompany(companyId); // Stub
    }

    @Override
    public PrivateCompany getPrivateCompany(String companyId) {
        if (mockRailsRoot == null)
            return null;
        return mockRailsRoot.getCompanyManager().getPrivateCompany(companyId); // Stub
    }

    @Override
    public boolean doesPlayerOwnCompany(Player player, String companyId) {
        return false; // Stub
    }

    public String getCurrentPlayerId() {
        return currentPlayerId;
    }

    public String getRound() {
        return round;
    }

    public String getCurrentPhaseValue() {
        return currentPhaseValue;
    }

    public Map<String, String> getHexMap() {
        return hexMap;
    }

    public Map<String, Integer> getAvailableTiles() {
        return availableTiles;
    }

    public Map<String, String> getPlayerData() {
        return playerData;
    }

    public Map<String, String> getCompanyData() {
        return companyData;
    }

    public Map<String, String> getPrivateData() {
        return privateData;
    }

    public List<TileLayOption> getLegalTileLays() {
        return legalTileLays;
    }

    public List<TokenLayOption> getLegalTokenLays() {
        return legalTokenLays;
    }

    // --- Setters (used by parser) ---
    public void setCurrentPlayerId(String id) {
        this.currentPlayerId = id;
    }

    public void setCurrentPhaseValue(String phase) {
        this.currentPhaseValue = phase;
    }

    public void setRound(String round) {
        this.round = round;
    }

    public void setOperatingCompanyData(int cash, int trainCount, int trainLimit) {
        this.opCompanyCash = cash;
        this.opCompanyTrainCount = trainCount;
        this.opCompanyTrainLimit = trainLimit;
    }

    public void setOperatingCompanyId(String id) {
        this.operatingCompanyId = id;
    }

    public String getOperatingCompanyId() {
        return this.operatingCompanyId;
    }

    public void addBuyTrainOption(BuyTrainOption option) {
        this.buyTrainOptions.add(option);
    }

    public List<BuyTrainOption> getBuyTrainOptions() {
        return this.buyTrainOptions;
    }

    public void addTileLayOption(TileLayOption option) {
        this.tileLayOptions.add(option);
    }

    public List<TileLayOption> getTileLayOptions() {
        return this.tileLayOptions;
    }

    // Add this method to the StaticGameContext class

    @Override
    public int getLastRevenue(String companyId) {
        // Static context doesn't track historical revenue for other companies.
        // Return 0 as a placeholder value.
        // aiLog.debug("StaticGameContext.getLastRevenue({}) called, returning 0
        // (placeholder).", companyId);
        return 0;
    }

    /**
 * --- NEW AI HELPER METHOD ---
 * Gets a list of string IDs for all StartItems (Privates) owned by a player.
 * Required for the synergy-based "opening book" logic.
 *
 * @param player The player to check.
 * @return A List<String> of item IDs (e.g., ["M1", "M4"]).
 */
@Override
public List<String> getPlayerOwnedStartItemIds(Player player) {
    List<String> ownedItemIds = new ArrayList<>();
    if (player == null) {
        return ownedItemIds; // Return empty list
    }
    
    // Get the GameManager instance from the context itself
    GameManager gm = this.getGameManager(); 
    if (gm == null) {
        return ownedItemIds;
    }

    // This is the corrected method name, paralleling getAllPublicCompanies()
    List<PrivateCompany> allPrivates = gm.getAllPrivateCompanies();

    if (allPrivates == null) {
        return ownedItemIds; // Return empty list
    }
    
    // Iterate and check ownership
    for (PrivateCompany privateItem : allPrivates) {
        // This assumes PrivateCompany has a getOwner() method
        if (privateItem != null && player.equals(privateItem.getOwner())) {
            ownedItemIds.add(privateItem.getId());
        }
    }
    
    return ownedItemIds;
}
}