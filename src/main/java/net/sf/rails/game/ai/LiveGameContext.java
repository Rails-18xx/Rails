package net.sf.rails.game.ai;

import java.util.List;
import java.util.Set;

import net.sf.rails.algorithms.RevenueAdapter;
import net.sf.rails.game.GameManager;
import net.sf.rails.game.MapHex;
import net.sf.rails.game.Phase;
import net.sf.rails.game.Player;
import net.sf.rails.game.PrivateCompany; // --- SCRIPTING: Added import
import net.sf.rails.game.PublicCompany;
import net.sf.rails.game.Tile;
import net.sf.rails.game.Train;
import net.sf.rails.game.model.PortfolioModel;
import net.sf.rails.algorithms.RevenueAdapter;
import net.sf.rails.game.RailsRoot;

import java.util.ArrayList;
import java.util.List;
import net.sf.rails.game.Player;
import net.sf.rails.game.PrivateCompany; // Or path to your PrivateCompany class
import net.sf.rails.game.GameManager;

/**
 * Live implementation of GameContext.
 * Wraps the live GameManager and other managers to provide data to AI services.
 */
public class LiveGameContext implements GameContext {

    private final GameManager gameManager;
    private final PublicCompany operatingCompany;
    private final Player currentPlayer;
    private final Phase currentPhase;
    private final RevenueAdapter revenueAdapter;

    @Override
    public RevenueAdapter getRevenueAdapter() {
        return this.revenueAdapter;
    }

    public LiveGameContext(GameManager gameManager, PublicCompany operatingCompany, Player currentPlayer,
            Phase currentPhase) {
        this.gameManager = gameManager;
        this.operatingCompany = operatingCompany;
        this.currentPlayer = currentPlayer;
        this.currentPhase = currentPhase;
        RailsRoot root = this.gameManager.getRoot();

        // --- SCRIPTING: Handle null company during SR/IR ---
        if (this.operatingCompany != null) {
            this.revenueAdapter = RevenueAdapter.createRevenueAdapter(root, this.operatingCompany, this.currentPhase);
        } else {
            this.revenueAdapter = null; // No revenue adapter if no company is operating
        }
    }

    // --- Core Objects ---
    @Override
    public GameManager getGameManager() {
        return this.gameManager;
    }

    @Override
    public PublicCompany getOperatingCompany() {
        return this.operatingCompany;
    }

    @Override
    public Player getCurrentPlayer() {
        return this.currentPlayer;
    }

    @Override
    public Phase getCurrentPhase() {
        return this.currentPhase;
    }

    @Override
    public int getAbsoluteORNumber() {
        return this.gameManager.getAbsoluteORNumber();
    }

    // --- Player State ---
    @Override
    public int getPlayerCash(Player player) {
        return player.getCash();
    }

    @Override
    public int getPlayerCertificateLimit(Player player) {
        return gameManager.getRoot().getPlayerManager().getPlayerCertificateLimit(player);
    }

    // --- Company State ---
    @Override
    public int getCompanyCash(PublicCompany company) {
        return company.getCash();
    }

    @Override
    public int getCompanyTrainLimit(PublicCompany company) {
        return company.getCurrentTrainLimit();
    }

    @Override
    public int getCompanySharePrice(PublicCompany company) {
        if (!company.hasStockPrice())
            return 0;
        return company.getCurrentSpace().getPrice();
    }

    @Override
    public int getCompanyTrainCount(PublicCompany company) {
        PortfolioModel portfolio = company.getPortfolioModel();
        return (portfolio != null) ? portfolio.getNumberOfTrains() : 0;
    }

    @Override
    public boolean companyHasFloated(PublicCompany company) {
        return company.hasFloated();
    }

    // --- Map & Tile State ---
    @Override
    public MapHex getMapHex(String hexId) {
        return gameManager.getRoot().getMapManager().getHex(hexId);
    }

    @Override
    public Tile getTile(String tileId) {
        return gameManager.getRoot().getTileManager().getTile(tileId);
    }

    @Override
    public int getTileAvailableCount(String tileId) {
        Tile tile = getTile(tileId);
        if (tile == null)
            return 0;
        return tile.getFreeCount();
    }

    @Override
    public boolean isHexOpen(String hexId) {
        MapHex hex = getMapHex(hexId);
        if (hex == null)
            return false;
        return hex.isOpen();
    }

    // --- Train State ---
    @Override
    public Set<Train> getAvailableTrains() {
        return gameManager.getRoot().getTrainManager().getAvailableNewTrains();
    }

    // --- SCRIPTING: NEW METHOD IMPLEMENTATIONS ---

    @Override
    public Player getPresident(PublicCompany company) {
        if (company == null)
            return null;
        return company.getPresident();
    }

    @Override
    public PublicCompany getPublicCompany(String companyId) {
        if (companyId == null)
            return null;
        return gameManager.getRoot().getCompanyManager().getPublicCompany(companyId);
    }

    @Override
    public PrivateCompany getPrivateCompany(String companyId) {
        if (companyId == null)
            return null;
        return gameManager.getRoot().getCompanyManager().getPrivateCompany(companyId);
    }

    /**
     * Checks if a player is the president of a specific public company.
     */
    @Override
    public boolean doesPlayerOwnCompany(Player player, String companyId) {
        if (player == null || companyId == null)
            return false;
        PublicCompany company = getPublicCompany(companyId);
        if (company == null)
            return false;
        return player.equals(company.getPresident());
    }

    @Override
    public int getLastRevenue(String companyId) {
        if (gameManager == null || gameManager.getRoot() == null)
            return 0;
        PublicCompany company = gameManager.getRoot().getCompanyManager().getPublicCompany(companyId);
        if (company != null) {
            return company.getLastRevenue(); // Assumes PublicCompany stores this
        }
        return 0; // Return 0 if company not found
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