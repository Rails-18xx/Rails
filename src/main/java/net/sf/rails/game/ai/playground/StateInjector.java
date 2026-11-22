package net.sf.rails.game.ai.playground;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import net.sf.rails.game.*;
// import net.sf.rails.game.model.CashModel;
import net.sf.rails.game.state.*;
import net.sf.rails.game.financial.StockSpace;

import java.io.File;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class StateInjector {
    private static final Logger log = LoggerFactory.getLogger(StateInjector.class);

    public static void inject(RailsRoot root, String jsonPath) {
        log.info("--- STARTING STATE INJECTION FROM {} ---", jsonPath);
        
        // try {
        //     ObjectMapper mapper = new ObjectMapper();
        //     JsonNode rootNode = mapper.readTree(new File(jsonPath));
        //     GameManager gm = root.getGameManager();

        //     // 1. INJECT PHASE
        //     if (rootNode.has("phase")) {
        //         JsonNode phaseNode = rootNode.get("phase");
        //         if (phaseNode.has("currentPhaseName")) {
        //             String phaseName = phaseNode.get("currentPhaseName").asText();
        //             log.info("Injecting Phase: {}", phaseName);
        //             for (Phase p : root.getPhaseManager().getPhases()) {
        //                 if (p.getName().equals(phaseName)) {
        //                     root.getPhaseManager().setCurrentPhase(p);
        //                     break;
        //                 }
        //             }
        //         }
        //     }

        //     // 2. INJECT PLAYERS (Cash & Certs)
        //     JsonNode players = rootNode.get("players");
        //     if (players != null) {
        //         for (JsonNode pNode : players) {
        //             String id = pNode.get("id").asText();
        //             int cash = pNode.get("cash").asInt();
        //             Player player = root.getPlayerManager().getPlayer(id);
                    
        //             if (player != null) {
        //                 log.info("Updating Player {}: Cash set to {}", id, cash);
        //                 // Direct cash setting
        //                 CashModel.setCash(player, cash);
                        
        //                 // Clear existing portfolio first? 
        //                 // Ideally we move everything to IPO then back.
        //                 // For now, let's assume start state is empty or we just add.
        //             }
        //         }
        //     }

        //     // 3. INJECT COMPANIES (Cash, Stock Price, Float)
        //     JsonNode publicCompanies = rootNode.get("publicCompanies");
        //     if (publicCompanies != null) {
        //         for (JsonNode cNode : publicCompanies) {
        //             String id = cNode.get("id").asText();
        //             PublicCompany company = root.getCompanyManager().getPublicCompany(id);
                    
        //             if (company != null) {
        //                 int cash = cNode.get("cash").asInt();
        //                 boolean floated = cNode.has("hasFloated") && cNode.get("hasFloated").asBoolean();
                        
        //                 log.info("Updating Company {}: Cash={}, Floated={}", id, cash, floated);
                        
        //                 CashModel.setCash(company, cash);
        //                 if (floated && !company.hasFloated()) {
        //                     // Force start requires a price.
        //                      if (cNode.has("stockPrice")) {
        //                          int price = cNode.get("stockPrice").asInt();
        //                          // Find closest start price space?
        //                          // Or just use null if the method allows it.
        //                          // company.start(null); 
        //                      }
        //                 }
                        
        //                 // Teleport Stock Price
        //                 if (cNode.has("stockRow") && cNode.has("stockCol")) {
        //                     int r = cNode.get("stockRow").asInt();
        //                     int c = cNode.get("stockCol").asInt();
        //                     StockSpace space = root.getStockMarket().getStockSpace(r, c);
        //                     if (space != null) {
        //                         company.setCurrentSpace(space);
        //                     }
        //                 }
        //             }
        //         }
        //     }

        //     // 4. INJECT MAP
        //     JsonNode mapNode = rootNode.get("map");
        //     if (mapNode != null && mapNode.has("hexes")) {
        //         log.info("Injecting Map State...");
        //         for (JsonNode hNode : mapNode.get("hexes")) {
        //             String hexId = hNode.get("id").asText();
        //             String tileId = hNode.get("tileId").asText();
                    
        //             MapHex hex = root.getMapManager().getHex(hexId);
        //             Tile tile = root.getTileManager().getTile(tileId);
                    
        //             if (hex != null && tile != null) {
        //                 int rotation = 0;
        //                 if (hNode.has("rotation")) {
        //                     rotation = hNode.get("rotation").asInt();
        //                 }
                        
        //                 // FORCE THE TILE
        //                 // This is the "Nuclear Option" - direct setter
        //                 hex.setCurrentTile(tile);
        //                 hex.setCurrentTileRotation(rotation); // Assuming this setter exists or similar
                        
        //                 // TOKENS
        //                 if (hNode.has("tokens")) {
        //                     for (JsonNode tNode : hNode.get("tokens")) {
        //                         String compId = tNode.get("companyId").asText();
        //                         PublicCompany comp = root.getCompanyManager().getPublicCompany(compId);
        //                         // int stationIndex = tNode.get("stationIndex").asInt();
                                
        //                         if (comp != null) {
        //                             // Logic to force token placement on specific stop
        //                             // Stop stop = hex.getStop(stationIndex);
        //                             // if (stop != null) stop.setToken(new BaseToken(comp));
        //                         }
        //                     }
        //                 }
        //             }
        //         }
        //     }
            
        //     // Force UI Refresh
        //     gm.getUIHints().clearVisibilityHints();
            
        //     log.info("--- INJECTION COMPLETE. READY TO PLAY. ---");

        // } catch (Exception e) {
        //     log.error("CRITICAL INJECTION FAILURE", e);
        //     e.printStackTrace();
        // }
    }
}