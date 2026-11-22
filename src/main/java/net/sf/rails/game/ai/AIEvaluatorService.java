package net.sf.rails.game.ai;

import net.sf.rails.game.MapHex;
import net.sf.rails.game.PublicCompany;
import net.sf.rails.game.Tile;
import net.sf.rails.game.Train;
// --- Add SLF4J imports ---
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import net.sf.rails.algorithms.NetworkTrain;
// --- End imports ---
import net.sf.rails.algorithms.RevenueAdapter;
// --- Add MockRevenueAdapter import ---
import net.sf.rails.game.ai.playground.MockRevenueAdapter;
// --- [NEW] Add imports for BiMap, Stop, and Station ---
import com.google.common.collect.BiMap;
import com.google.common.collect.HashBiMap;
import net.sf.rails.game.Stop;
import net.sf.rails.game.Station;
// --- End [NEW] ---
import net.sf.rails.game.HexSide;
import rails.game.action.LayTile;
import net.sf.rails.game.RailsRoot; // Needed to create LayTile

/**
 * Wraps the game's RevenueAdapter to calculate current and hypothetical
 * revenues.
 *
 */
public class AIEvaluatorService {

    // Make sure aiLog is correctly initialized
    private static final Logger aiLog = LoggerFactory.getLogger("AI_Decision_Log");
    private static final Logger log = LoggerFactory.getLogger(AIPlayer.class);

    public AIEvaluatorService() {
        // Constructor
    }

    /**
     * Calculates the best possible revenue for a company given a *hypothetical*
     * new tile lay.
     *
     */
    public int calculateMaxRevenue(GameContext context, TileLayOption option) {
        aiLog.debug("Calculating Hypothetical Max Revenue for tile {} on hex {}...",
                option.tile().getId(), option.hex().getId()); // Use getters from option

        // Initial check for a valid RevenueAdapter from the context
        RevenueAdapter initialAdapter = context.getRevenueAdapter();
        if (initialAdapter == null) {
            aiLog.error("Initial RevenueAdapter from context is null. Cannot calculate revenue.");
            return 0;
        }

        // --- Handle Playground Mock ---
        if (initialAdapter instanceof MockRevenueAdapter) {
            MockRevenueAdapter mock = (MockRevenueAdapter) initialAdapter;
            // Assuming the mock needs the tile ID for its logic
            return mock.calculateHypotheticalRevenue(option.tile().getId()); // Use getter
        }
        // --- End Playground Mock ---

        // --- Live Game Simulation Logic ---
        MapHex hex = option.hex(); // Get objects directly from the option
        Tile newTile = option.tile();
        int newOrientationInt = option.orientation();

        if (hex == null || newTile == null) {
            aiLog.error("Hex ({}) or Tile ({}) object is null in TileLayOption.",
                    option.hex(), option.tile());
            return 0;
        }

        HexSide newOrientation = HexSide.get(newOrientationInt);
        if (newOrientation == null) {
            aiLog.error("Invalid orientation value: {}", newOrientationInt);
            return 0;
        }

        // --- [MODIFIED] 1. Save Original State ---
        Tile originalTile = hex.getCurrentTile();
        HexSide originalOrientation = hex.getCurrentTileRotation();
        
        // Create a deep copy of the station-to-stop mapping
        // using the new public hex.getStopsMap() method
        BiMap<Station, Stop> originalStops = HashBiMap.create(hex.getStopsMap());
        // --- [END MODIFIED] ---


        // Get RailsRoot - needed for creating LayTile actions
        PublicCompany operatingCompany = context.getOperatingCompany();
        if (operatingCompany == null) {
            aiLog.error("OperatingCompany is null in GameContext. Cannot proceed.");
            return 0;
        }
        RailsRoot root = operatingCompany.getRoot();
        if (root == null) {
            aiLog.error("Cannot get RailsRoot from OperatingCompany to create temporary LayTile action.");
            return 0;
        }

        // Create *only* the apply action
        LayTile applyAction = new LayTile(root, LayTile.GENERIC);
        applyAction.setChosenHex(hex);
        applyAction.setLaidTile(newTile);
        applyAction.setOrientation(newOrientationInt);

        // [REMOVED] The 'revertAction' is deleted.

        try {
            // --- 2. APPLY HYPOTHECAL TILE ---
            aiLog.debug("  - TMR: Applying hypothetical tile {} ({}) to hex {}", newTile.getId(), newOrientationInt,
                    hex.getId());
            hex.upgrade(applyAction); // This call mutates the hex's state
            aiLog.debug(">>> SIM: Hex {} state AFTER APPLY: Tile={}, Rot={}", hex.getId(),
                    (hex.getCurrentTile() != null ? hex.getCurrentTile().getId() : "null"),
                    hex.getCurrentTileRotation());

            // 2. CREATE ADAPTER - *WITHOUT* IMMEDIATE POPULATION
            aiLog.debug(">>> SIM: Creating RevenueAdapter (populateNow=false)...");
            RevenueAdapter hypotheticalAdapter = RevenueAdapter.createRevenueAdapter(
                    root,
                    operatingCompany,
                    context.getCurrentPhase(),
                    false // *** Pass false to delay population ***
            );

// --- 3. EXPLICITLY CLEAR CACHE ---
            // The cache must be cleared so the adapter builds a new graph including the hypothetical tile,
            // rather than evaluating the original state and returning a false delta of 0.
            if (hypotheticalAdapter.getNetworkAdapterInternal() != null) {
                hypotheticalAdapter.getNetworkAdapterInternal().clearGraphCache();
            }

            // --- 4. SIMULATE TRAIN PURCHASE (if needed) ---
            if (hypotheticalAdapter.getTrains().isEmpty()) {
                Train trainToSimulate = null;
                if (!context.getAvailableTrains().isEmpty()) {
                    trainToSimulate = context.getAvailableTrains().iterator().next(); // Get first available
                }

                if (trainToSimulate != null) {
                    aiLog.debug("  - TMR: Company has no trains. Hypothetically adding train {} to adapter.",
                            trainToSimulate.getId());
                    hypotheticalAdapter.addTrain(trainToSimulate); // Add to adapter's internal list
                } else {
                    aiLog.warn("  - TMR: Company has no trains, and no trains are available to simulate!");
                }
            } else {
                aiLog.debug("  - TMR: Company already has trains ({}), not simulating purchase.",
                        hypotheticalAdapter.getTrains().size());
            }

            // 4. *** MANUALLY POPULATE ADAPTER NOW ***
            boolean originallyHadNoTrains = (context.getCompanyTrainCount(operatingCompany) == 0);
            NetworkTrain addedSimTrain = null; 
            if (originallyHadNoTrains && !hypotheticalAdapter.getTrains().isEmpty()) {
                addedSimTrain = hypotheticalAdapter.getTrains().get(0);
            }

            aiLog.debug(">>> SIM: Manually calling populateFromRails()...");
            hypotheticalAdapter.populateFromRails(); // This builds the graph and loads real trains

            // *** Re-apply simulated train if populateFromRails overwrote it ***
            if (originallyHadNoTrains && hypotheticalAdapter.getTrains().isEmpty() && addedSimTrain != null) {
                aiLog.debug(">>> SIM: Re-adding simulated train {} after population.", addedSimTrain.getTrainName());
                hypotheticalAdapter.addTrain(addedSimTrain);
            } else if (originallyHadNoTrains && !hypotheticalAdapter.getTrains().isEmpty()) {
                aiLog.warn(">>> SIM: Expected no trains after population for {}, but found {}. Using found trains.",
                        operatingCompany.getId(), hypotheticalAdapter.getTrains().size());
            }

            // --- 5. INITIALIZE CALCULATOR & CALCULATE REVENUE ---
            aiLog.debug(">>> SIM: Initializing revenue calculator with trains: {}", hypotheticalAdapter.getTrains());
            hypotheticalAdapter.initRevenueCalculator(true);
            int hypotheticalRevenueValue = hypotheticalAdapter.calculateRevenue();

            // Targeted logging to intercept and analyze the Wien (G17) / Tile 427 upgrade
            if ("G17".equals(hex.getId()) && "427".equals(newTile.getId())) {
                aiLog.info("========== WIEN (G17) / TILE 427 SIMULATION ==========");
                aiLog.info("Calculated Hypothetical Revenue: {}", hypotheticalRevenueValue);
                if (initialAdapter != null) {
                    initialAdapter.initRevenueCalculator(true);
                    aiLog.info("Original Base Revenue: {}", initialAdapter.calculateRevenue());
                }
                aiLog.info("Trains executing calculation: {}", hypotheticalAdapter.getTrains());
                aiLog.info("Simulated Hex Stops & Station Mapping: {}", hex.getStopsMap());
                aiLog.info("=======================================================");
            }


            aiLog.debug(">>> SIM: Calculated hypothetical revenue: {}", hypotheticalRevenueValue);
            return hypotheticalRevenueValue;

        } catch (Exception e) {
            aiLog.error("Exception during hypothetical revenue calculation for hex {}: {}", hex.getId(),
                    e.getMessage());
            e.printStackTrace(); // Print stack trace for debugging
            return 0; // Return 0 on any simulation error

        } finally {
            // --- [MODIFIED] 6. REVERT STATE MANUALLY ---
            // Use the now-public executeTileLay to revert state directly
            aiLog.debug("  - TMR: Reverting hex {} to original tile {}", hex.getId(),
                    (originalTile != null ? originalTile.getId() : "null"));
            try {
                // This is the clean, safe way to revert the state.
                hex.executeTileLay(originalTile, originalOrientation, originalStops.inverse());
                aiLog.debug(">>> SIM: Hex {} state AFTER REVERT: Tile={}, Rot={}", hex.getId(),
                        (hex.getCurrentTile() != null ? hex.getCurrentTile().getId() : "null"),
                        hex.getCurrentTileRotation());

                        // Flush the global cache again after the revert.
                // Failing to do so leaves a "ghost graph" of the reverted tile in the engine,
                // corrupting subsequent AI evaluations and human player revenue runs.
                if (initialAdapter != null && initialAdapter.getNetworkAdapterInternal() != null) {
                    initialAdapter.getNetworkAdapterInternal().clearGraphCache();
                }

            } catch (Exception revertEx) {
                // Log a critical error if reverting fails, as game state might be corrupted
                aiLog.error("CRITICAL: Exception during TMR revert (via executeTileLay) for hex {}: {}", hex.getId(), revertEx.getMessage());
                revertEx.printStackTrace();
            }
        }
    }

    /**
     * Calculates the best possible revenue for the company in the *current*
     * board state without simulation.
     */
    public int calculateCurrentMaxRevenue(GameContext context) {
        aiLog.debug("Calculating Current Max Revenue...");

        RevenueAdapter adapter = context.getRevenueAdapter();
        if (adapter == null) {
            aiLog.error("RevenueAdapter from context is null for current revenue calculation.");
            return 0;
        }

        // --- Handle Playground Mock ---
        if (adapter instanceof MockRevenueAdapter) {
            return adapter.calculateRevenue();
        }
        // --- End Playground Mock ---

        // --- Live Game Logic ---
        PublicCompany operatingCompany = context.getOperatingCompany();
        if (operatingCompany == null) {
            aiLog.error("OperatingCompany is null in context for current revenue calculation.");
            return 0;
        }
        RailsRoot root = operatingCompany.getRoot();
        if (root == null) {
            aiLog.error("Cannot get RailsRoot from OperatingCompany for current revenue calculation.");
            return 0;
        }

        // Create a *new* adapter to ensure it reflects the absolute current state
        RevenueAdapter currentAdapter = RevenueAdapter.createRevenueAdapter(
                root,
                operatingCompany,
                context.getCurrentPhase());

        // Explicitly clear its cache to be safe
        if (currentAdapter.getNetworkAdapterInternal() != null) {
            currentAdapter.getNetworkAdapterInternal().clearGraphCache();
        }

        aiLog.debug(">>> CURRENT: Initializing revenue calculator with trains: {}", currentAdapter.getTrains());
        currentAdapter.initRevenueCalculator(true); // Initialize the calculator
        int currentRevenueValue = currentAdapter.calculateRevenue();
        aiLog.debug(">>> CURRENT: Calculated current revenue: {}", currentRevenueValue);
        return currentRevenueValue;
    }

} // End of AIEvaluatorService class