package net.sf.rails.game.ai.data;

import net.sf.rails.game.GameManager;
import net.sf.rails.game.RailsRoot;
import net.sf.rails.game.MapHex;
import net.sf.rails.game.MapManager;
import net.sf.rails.game.PublicCompany;
import net.sf.rails.game.PrivateCompany;
import net.sf.rails.game.Player;
import net.sf.rails.game.PlayerManager;
import net.sf.rails.game.Station;
import net.sf.rails.game.Stop;
import net.sf.rails.game.CompanyManager;
import net.sf.rails.game.BaseToken;
import net.sf.rails.game.Train;
// import net.sf.rails.game.financial.StockSpace; // Unused

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.HashMap; // Added
import java.util.stream.Collectors;

/**
 * StateVectorBuilder (Deep State Version)
 * Fixed Accessors for PlayerManager and PortfolioModel.
 */
public class StateVectorBuilder {

    private final GameManager gameManager;
    private final MapManager mapManager;
    private final CompanyManager companyManager;
    private final PlayerManager playerManager;
    
    private final List<String> publicCompOrder; 
    private final List<String> privateCompOrder;
    private final List<String> playerOrder;
    
    // Cache players map locally since getPlayer(String) might be missing
    private final Map<String, Player> playerMap = new HashMap<>();

    private static final double NO_SLOT = 0.0;
    private static final double EMPTY_SLOT = -1.0; 
    private static final int MAX_STATIONS_PER_HEX = 3; 
    private static final int MAX_TRAINS_PER_COMP = 4;

    public StateVectorBuilder(RailsRoot root) {
        this.gameManager = root.getGameManager();
        this.mapManager = root.getMapManager();
        this.companyManager = root.getCompanyManager();
        this.playerManager = root.getPlayerManager();
        
        this.publicCompOrder = companyManager.getAllPublicCompanies().stream()
                .map(PublicCompany::getId).sorted().collect(Collectors.toList());
        
        this.privateCompOrder = companyManager.getAllPrivateCompanies().stream()
                .map(PrivateCompany::getId).sorted().collect(Collectors.toList());
                
        this.playerOrder = playerManager.getPlayers().stream()
                .map(Player::getName).sorted().collect(Collectors.toList());

        // Populate local map
        for (Player p : playerManager.getPlayers()) {
            playerMap.put(p.getName(), p);
        }
    }

   // Define absolute maximums to ensure vector size is constant for the Neural Network
    private static final int MAX_PLAYERS_SUPPORTED = 5; 

    public double[] buildFullStateVector() {
        List<Double> vector = new ArrayList<>();

        // --- 1. GLOBAL STATE ---
        // Feature 1: Phase Index
        vector.add((double) gameManager.getCurrentPhase().getIndex());
        
        // Feature 2: Round Type (0=Stock, 1=Operating)
        String roundName = gameManager.getCurrentRound().getClass().getSimpleName();
        double orIndex = 0.0; 
        if (roundName.contains("OperatingRound")) {
            orIndex = 1.0; 
        }
        vector.add(orIndex); 
        
        // Feature 3: Player Count (New Context for AI)
        // Allows the AI to understand if this is a tight 4-player game or a loose 3-player game.
        int playerCount = playerManager.getNumberOfPlayers();
        vector.add((double) playerCount);

        // Feature 4: Current Player Index
        String curPlayerName = gameManager.getCurrentPlayer() != null ? gameManager.getCurrentPlayer().getName() : "";
        double curPlayerIdx = (double) playerOrder.indexOf(curPlayerName);
        vector.add(curPlayerIdx);
        
        // Padding for future global features (Keep 5 slots reserved)
        for(int i=0; i<5; i++) vector.add(0.0);

        // --- 2. COMPANY STATE ---
        // (Iterate strictly through the pre-sorted list to maintain vector alignment)
        for (String compId : publicCompOrder) {
            PublicCompany company = (PublicCompany) companyManager.getPublicCompany(compId);
            
            if (company == null) {
                // If company doesn't exist (rare), pad with zeros
                for(int k=0; k<9; k++) vector.add(0.0);
            } else {
                vector.add(company.hasFloated() ? 1.0 : 0.0);
                vector.add((double) company.getCash());
                vector.add((double) company.getCurrentPrice()); 
                
                // Trains (Fixed slot count: MAX_TRAINS_PER_COMP)
                List<Double> trainIds = new ArrayList<>();
                if (company.getPortfolioModel() != null) {
                    for (Object item : company.getPortfolioModel().getTrainsModel().getPortfolio().items()) {
                        if (item instanceof Train) {
                            trainIds.add(encodeTrain(((Train)item).getName()));
                        }
                    }
                }
                for (int k=0; k<MAX_TRAINS_PER_COMP; k++) {
                    if (k < trainIds.size()) vector.add(trainIds.get(k));
                    else vector.add(0.0); // Empty train slot
                }
                
                vector.add((double) company.getNumberOfLaidBaseTokens());
                vector.add((double) company.getNumberOfBaseTokens());
            }
        }

        // --- 3. PLAYER STATE (PADDED) ---
        // Crucial: We iterate up to MAX_PLAYERS_SUPPORTED (5). 
        // If the actual game has fewer players, we fill the remaining slots with 0.0 (Ghost Players).
        for (int i = 0; i < MAX_PLAYERS_SUPPORTED; i++) {
            if (i < playerOrder.size()) {
                // --- REAL PLAYER ---
                String pName = playerOrder.get(i);
                Player p = playerMap.get(pName);
                
                if (p != null) {
                    vector.add((double) p.getCash());
                    vector.add((double) p.getWorth());
                    
                    // Shares Owned
                    for (String compId : publicCompOrder) {
                        PublicCompany c = (PublicCompany) companyManager.getPublicCompany(compId);
                        if (c != null) {
                            vector.add((double) p.getPortfolioModel().getShare(c)); 
                        } else {
                            vector.add(0.0);
                        }
                    }
                    
                    // Privates Owned
                    for (String privId : privateCompOrder) {
                        PrivateCompany pc = (PrivateCompany) companyManager.getPrivateCompany(privId);
                        boolean owns = false;
                        try {
                            if (p.getPortfolioModel().getPrivatesOwnedModel() != null 
                                && p.getPortfolioModel().getPrivatesOwnedModel().getPortfolio() != null) {
                                owns = p.getPortfolioModel().getPrivatesOwnedModel().getPortfolio().containsItem(pc);
                            }
                        } catch (Exception e) {
                            // Fallback or swallow
                        }
                        vector.add(owns ? 1.0 : 0.0);
                    }
                } else {
                    // Should not happen if playerOrder is synced, but handle safe
                    padZeroPlayer(vector);
                }
            } else {
                // --- GHOST PLAYER (PADDING) ---
                // Add zeros for Cash, Worth, All Shares, All Privates
                padZeroPlayer(vector);
            }
        }

        // --- 4. MAP STATE ---
        List<MapHex> allHexes = new ArrayList<>(mapManager.getHexes());
        allHexes.sort((h1, h2) -> h1.getId().compareTo(h2.getId()));

        for (MapHex hex : allHexes) {
            int tileId = 0;
            if (hex.getCurrentTile() != null) {
                try {
                    tileId = Integer.parseInt(hex.getCurrentTile().getId());
                } catch (Exception e) { tileId = -1; }
            }
            vector.add((double) tileId);
            
            vector.add((double) hex.getTileRotation());

            List<Station> stations = null;
            if (hex.getCurrentTile() != null) {
                stations = new ArrayList<>(hex.getCurrentTile().getStations());
            }
            Map<Station, Stop> stopMap = hex.getStopsMap();

            for (int i = 0; i < MAX_STATIONS_PER_HEX; i++) {
                double slotVal = NO_SLOT; 
                if (stations != null && i < stations.size()) {
                    Station s = stations.get(i);
                    Stop stop = stopMap.get(s);
                    if (stop != null) {
                        String ownerId = null;
                        List<BaseToken> tokens = stop.getTokens(); 
                        if (tokens != null) {
                            for (BaseToken t : tokens) {
                                if (t.getParent() != null) {
                                    ownerId = t.getParent().getId();
                                    break; 
                                }
                            }
                        }
                        if (ownerId != null) {
                            int ownerIdx = publicCompOrder.indexOf(ownerId);
                            slotVal = (double) (ownerIdx + 1); 
                        } else {
                            slotVal = EMPTY_SLOT; 
                        }
                    }
                }
                vector.add(slotVal);
            }
        }
        
        return vector.stream().mapToDouble(d -> d).toArray();
    }

    // Helper to fill zeros for a non-existent player
    private void padZeroPlayer(List<Double> vector) {
        vector.add(0.0); // Cash
        vector.add(0.0); // Worth
        // Zero shares for all public companies
        for (int k = 0; k < publicCompOrder.size(); k++) vector.add(0.0);
        // Zero privates for all private companies
        for (int k = 0; k < privateCompOrder.size(); k++) vector.add(0.0);
    }
    
    private double encodeTrain(String name) {
        if (name == null) return 0.0;
        String n = name.trim();
        if (n.equals("2")) return 20.0;
        if (n.equals("2+2")) return 22.0;
        if (n.equals("3")) return 30.0;
        if (n.equals("3+3")) return 33.0;
        if (n.equals("4")) return 40.0;
        if (n.equals("4+4")) return 44.0;
        if (n.equals("5")) return 50.0;
        if (n.equals("6")) return 60.0;
        if (n.contains("D") || n.contains("E")) return 99.0; 
        
        try {
            return Double.parseDouble(n) * 10;
        } catch (Exception e) { return 5.0; } 
    }
}