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
import net.sf.rails.algorithms.RevenueAdapter;


/**
 * Interface acting as a Facade to the game state for AI services.
 *
 */
public interface GameContext {

    // --- Core Objects ---
    GameManager getGameManager();
    PublicCompany getOperatingCompany();
    Player getCurrentPlayer();
    Phase getCurrentPhase();
    RevenueAdapter getRevenueAdapter();

    // --- Player State ---
    int getPlayerCash(Player player);
    int getPlayerCertificateLimit(Player player);
    int getAbsoluteORNumber();
    int getLastRevenue(String companyId);

    // --- Company State ---
    int getCompanyCash(PublicCompany company);
    int getCompanyTrainLimit(PublicCompany company);
    int getCompanySharePrice(PublicCompany company);
    int getCompanyTrainCount(PublicCompany company);
    boolean companyHasFloated(PublicCompany company);

    // --- Map & Tile State ---
    MapHex getMapHex(String hexId);
    Tile getTile(String tileId);
    int getTileAvailableCount(String tileId);
    boolean isHexOpen(String hexId);

    // --- Train State ---
    Set<Train> getAvailableTrains();

    // --- SCRIPTING: NEW METHODS FOR CONDITIONAL RULES ---
    Player getPresident(PublicCompany company);
    PublicCompany getPublicCompany(String companyId);
    PrivateCompany getPrivateCompany(String companyId);
    boolean doesPlayerOwnCompany(Player player, String companyId);
    /**
     * --- NEW AI HELPER METHOD ---
     * Gets a list of string IDs for all StartItems (Privates) owned by a player.
     * Required for the synergy-based "opening book" logic.
     * * @param player The player to check.
     * @return A List<String> of item IDs (e.g., ["M1", "M4"]).
     */
    List<String> getPlayerOwnedStartItemIds(Player player);
    
}