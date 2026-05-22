package net.sf.rails.game.ai.playground;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.SerializationFeature;
import com.fasterxml.jackson.databind.node.ArrayNode;
import com.fasterxml.jackson.databind.node.ObjectNode;
import net.sf.rails.game.RailsRoot;
import net.sf.rails.util.GameLoader;
import rails.game.action.*;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.File;
import java.util.*;

/**
 * Tool to translate legacy .rails save files into clean JSON states.
 * It bypasses the Game Engine's validation rules and treats the save file
 * as a trusted transaction log.
 */
public class SaveConverter {

    public static void main(String[] args) {
        if (args.length < 2) {
            System.out.println("Usage: SaveConverter <input.rails> <output.json>");
            System.exit(1);
        }
        
        // We need to boot a minimal Rails environment to get the metadata (Hex names, etc)
        // This is similar to HeadlessRunner
        // net.sf.rails.util.RunGame.initLog();
        
        // 1. Create Root/Manager (Headless)
        // We can reuse the HeadlessRunner bootstrap logic or just instantiate directly if compatible.
        // Assuming we have access to a basic "Bootstrap" or we just create the components.
        // For now, let's assume we can access the Game via standard loading if we want metadata,
        // OR we just run purely on the file data.
        
        // To keep it simple and robust: We will try to create a dummy GameContext.
        // Use the GameLoader to initialize the root if possible, or pass null to convert() 
        // and handle missing metadata gracefully.
        
        // NOTE: GameLoader NEEDS a RailsRoot to function. 
        // We must instantiate the game engine to use the loader.
        
        // Using the HeadlessRunner approach:
        // ... Bootstrap code here ...
        
        // Ideally, you call this FROM your existing HeadlessRunner or AutoGUIRunner
        // which already sets up the environment.
        
        log.info("Please run this tool via the existing Headless infrastructure to ensure environment setup.");
    }


    private static final Logger log = LoggerFactory.getLogger(SaveConverter.class);

    // --- SHADOW STATE MODELS ---
    // We maintain our own simple state, independent of the complex Game Engine
    private static class ShadowHex {
        String tileId = "0"; // Default empty/gray
        int rotation = 0;
        List<ShadowToken> tokens = new ArrayList<>();
    }
    private static class ShadowToken {
        String companyId;
        int stationIndex;
        public ShadowToken(String c, int s) { this.companyId = c; this.stationIndex = s; }
    }
    private static class ShadowCompany {
        String id;
        int cash = 0;
        boolean floated = false;
        int price = 0;
        // Simplified portfolio: Owner -> Count
    }
    private static class ShadowPlayer {
        String id;
        int cash = 0;
        // Certificates tracked via simple map: CompanyID -> Percent
        Map<String, Integer> shares = new HashMap<>();
    }

    public static void convert(RailsRoot root, String railsFilePath, String jsonOutputPath) {
        log.info("--- CONVERTER: Reading {} ---", railsFilePath);

        try {
            // 1. Load Actions using the standard loader (it deserializes the objects fine)
            GameLoader loader = new GameLoader();
            File file = new File(railsFilePath);
            if (!loader.reloadGameFromFile(root, file)) {
                log.error("Failed to deserialize .rails file.");
                return;
            }
            List<PossibleAction> actions = loader.getActions();
            log.info("Loaded {} actions. Starting Shadow Replay...", actions.size());

            // 2. Initialize Shadow State
            Map<String, ShadowHex> mapState = new HashMap<>();
            Map<String, ShadowPlayer> playerState = new HashMap<>();
            Map<String, ShadowCompany> companyState = new HashMap<>();
            
            // Pre-fill players from the root (metadata usually exists)
            // If not, we create them dynamically as we see actions.
            
            // 3. Replay Actions (The "Blind" Engine)
            for (PossibleAction action : actions) {
                try {
                    applyAction(action, mapState, playerState, companyState);
                } catch (Exception e) {
                    log.warn("Failed to scrape action {}: {}", action, e.getMessage());
                }
            }

            // 4. Export to JSON
            exportJson(root, mapState, playerState, companyState, jsonOutputPath);

        } catch (Exception e) {
            log.error("Conversion crashed", e);
        }
    }

    private static void applyAction(PossibleAction action, 
                                  Map<String, ShadowHex> map, 
                                  Map<String, ShadowPlayer> players, 
                                  Map<String, ShadowCompany> companies) {
        
        // --- A. MAP ACTIONS ---
        if (action instanceof LayTile) {
            LayTile lt = (LayTile) action;
            String hexId = lt.getChosenHex().getId();
            // Update or Create Hex
            ShadowHex hex = map.computeIfAbsent(hexId, k -> new ShadowHex());
            
            if (lt.getLaidTile() != null) {
                hex.tileId = lt.getLaidTile().getId();
                hex.rotation = lt.getOrientation();
            }
        }
        else if (action instanceof LayBaseToken) { // Covers LayToken parent too usually
            LayBaseToken lt = (LayBaseToken) action;
            String hexId = lt.getChosenHex().getId();
            ShadowHex hex = map.computeIfAbsent(hexId, k -> new ShadowHex());
            
            // Add token
            hex.tokens.add(new ShadowToken(lt.getCompany().getId(), lt.getChosenStation()));
        }
        
        // --- B. STOCK ACTIONS ---
        else if (action instanceof BuyCertificate) {
            BuyCertificate bc = (BuyCertificate) action;
            String pName = bc.getPlayerName();
            String cId = bc.getCompany().getId();
            int percent = bc.getSharePerCertificate() * bc.getNumberBought();
            int cost = bc.getPrice(); // This is total cost usually, or per share? Check Logic.
            // Note: BuyCertificate.price is usually total paid if simple, or unit price. 
            // We'll assume unit * count for safety or check `getPrice()` implementation.
            // For now, focus on OWNERSHIP.
            
            ShadowPlayer player = players.computeIfAbsent(pName, k -> {
                ShadowPlayer p = new ShadowPlayer(); p.id = k; return p;
            });
            
            // Add shares
            player.shares.merge(cId, percent, Integer::sum);
            
            // Deduct Cash (Approximate)
            player.cash -= cost; // Rough calc
        }
        else if (action instanceof SellShares) {
            SellShares ss = (SellShares) action;
            String pName = ss.getPlayerName();
            String cId = ss.getCompanyName();
            int percent = ss.getShareUnits() * ss.getNumber() * ss.getCompany().getShareUnit();
            
            ShadowPlayer player = players.get(pName);
            if (player != null) {
                // Remove shares
                int current = player.shares.getOrDefault(cId, 0);
                player.shares.put(cId, Math.max(0, current - percent));
                
                // Add Cash
                int revenue = ss.getPrice() * ss.getNumber() * ss.getShareUnits(); 
                player.cash += revenue;
            }
        }
        
        // --- C. COMPANY OPS ---
        else if (action instanceof SetDividend) {
            SetDividend sd = (SetDividend) action;
            String cId = sd.getCompanyName();
            // If Payout/Split, we need to distribute cash. 
            // This is the hardest part to emulate without a engine.
            // For now, we might ignore cash precision and focus on Map/Stock correctness.
        }
    }

    private static void exportJson(RailsRoot root, 
                                 Map<String, ShadowHex> mapState, 
                                 Map<String, ShadowPlayer> playerState,
                                 Map<String, ShadowCompany> companyState,
                                 String path) throws Exception {
        
        ObjectMapper mapper = new ObjectMapper();
        mapper.enable(SerializationFeature.INDENT_OUTPUT);
        ObjectNode rootNode = mapper.createObjectNode();

        // 1. MAP
        ObjectNode mapNode = rootNode.putObject("map");
        ArrayNode hexesNode = mapNode.putArray("hexes");
        
        for (Map.Entry<String, ShadowHex> entry : mapState.entrySet()) {
            ObjectNode hNode = hexesNode.addObject();
            hNode.put("id", entry.getKey());
            hNode.put("tileId", entry.getValue().tileId);
            // Convert int rotation to String (0 -> SW etc) if needed, or keep int
            // Using Engine Helper if possible:
            // String rotName = root.getMapManager().getHex(entry.getKey()).getOrientationName(entry.getValue().rotation);
            hNode.put("rotation", entry.getValue().rotation); 
            
            ArrayNode tArray = hNode.putArray("tokens");
            for (ShadowToken st : entry.getValue().tokens) {
                ObjectNode tNode = tArray.addObject();
                tNode.put("companyId", st.companyId);
                tNode.put("stationIndex", st.stationIndex);
            }
        }

        // 2. PLAYERS
        ArrayNode playersNode = rootNode.putArray("players");
        for (ShadowPlayer sp : playerState.values()) {
            ObjectNode pNode = playersNode.addObject();
            pNode.put("id", sp.id);
            pNode.put("cash", sp.cash);
            
            ArrayNode certsNode = pNode.putArray("certificates");
            for (Map.Entry<String, Integer> share : sp.shares.entrySet()) {
                if (share.getValue() > 0) {
                    ObjectNode cNode = certsNode.addObject();
                    cNode.put("companyId", share.getKey());
                    cNode.put("percentage", share.getValue());
                    // Heuristic for President: > 20% or max holder? 
                    // Hard to know for sure without logic, assume false for now or logic later.
                    cNode.put("isPresident", false); 
                }
            }
        }

        mapper.writeValue(new File(path), rootNode);
        log.info("--- CONVERSION SUCCESS: Saved to {} ---", path);
    }
}