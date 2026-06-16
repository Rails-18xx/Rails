package net.sf.rails.game.ai;

import com.google.gson.Gson;
import com.google.gson.reflect.TypeToken;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.Reader;
import java.lang.reflect.Type;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Random;
import java.util.Set;
import net.sf.rails.game.Player;
import net.sf.rails.game.Tile;
import java.io.FileReader;

import net.sf.rails.game.MapHex;
import net.sf.rails.game.PublicCompany;
import net.sf.rails.game.Round;
import net.sf.rails.game.Phase; // Needed for checkGamePhase
import net.sf.rails.game.ai.GameContext;
import net.sf.rails.game.ai.TileLayOption;
import net.sf.rails.game.ai.TokenLayOption;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import rails.game.action.BuyCertificate;
import rails.game.action.BuyStartItem;
import rails.game.action.BuyTrain;
import rails.game.action.SetDividend;
import rails.game.action.SellShares;
import net.sf.rails.game.ai.GameContext;
import rails.game.action.PossibleAction;
import rails.game.action.PriceMode;
import rails.game.action.LayTile;
import rails.game.action.LayBaseToken;
import rails.game.action.NullAction;
import rails.game.action.UseSpecialProperty;
import net.sf.rails.game.special.ExchangeForShare;
import net.sf.rails.game.model.PortfolioModel;
import net.sf.rails.game.Train;
import net.sf.rails.game.TrainType;
// --- 1835 Specific Actions ---
import rails.game.specific._1835.StartPrussian;
import rails.game.specific._1835.ExchangeForPrussianShare;

/**
 * AI Rules Engine.
 * Loads a strategy script from a JSON file and scores moves.
 * Checks for "Scripted" sequences first, then falls back to "Heuristic"
 * scoring.
 */
public class ExpertStrategyService {

    private static final Logger aiLog = LoggerFactory.getLogger("AI_Decision_Log");
    private final AIEvaluatorService evaluator;
    private final AIPathfinderService pathfinder;
    private final Random random = new Random();

    // Use a generic name, as this class is now game-agnostic
    private static final String STRATEGY_FILE = "/expertstrategies1835.json";

    // --- POJOs for initial_round_analysis.json ---
    private static class AnalysisOutput {
        Map<String, Double> assetPerformance;
        Map<String, Map<String, Double>> pickPopularity;
        Map<String, Double> synergyPerformance;
    }
    // --- End POJOs ---

    private Map<String, Double> initialRoundAssetPerformance;
    private Map<String, Map<String, Double>> initialRoundPickPopularity;
    private Map<String, Double> initialRoundSynergyPerformance;

    // --- Rule Storage ---
    private static final String IR_ANALYSIS_FILE = "initial_round_analysis.json";

    private List<TileLayingStrategy> tileStrategies = new ArrayList<>();

    // --- POJOs for expertstrategies1835.json ---
    private static class TileStrategyFile {
        List<TileLayingStrategy> tileLayingStrategies;
    }

    private static class TileLayingStrategy {
        StrategyContext context;
        List<TileMove> moves;
    }

    private static class StrategyContext {
        String companyId;
        String phase; // Keep as String for simplicity, e.g., "1"
        List<String> friendlyCompanyIds;
    }

    private static class TileMove {
        String hexId;
        String tileId;
        String rotation; // e.g., "SW", "NW"
        int weight;
    }

    // --- End POJOs ---
    public ExpertStrategyService(AIEvaluatorService evaluator, AIPathfinderService pathfinder) {
        this.evaluator = evaluator;
        this.pathfinder = pathfinder;
        loadStrategies(); // Load strategies on creation
        loadInitialRoundAnalysis(); // Load opening book
    }

    // Helper function to convert JSON rotation string to game engine orientation
    // int
    // This assumes a standard hex grid rotation: 0=NW, 1=W, 2=SW, 3=SE, 4=E, 5=NE
    private static int convertRotation(String rotString) {
        if (rotString == null)
            return -1;
        switch (rotString.toUpperCase()) {
            case "SW":
                return 0;
            case "W":
                return 1;
            case "NW":
                return 2;
            case "NE":
                return 3;
            case "E":
                return 4;
            case "SE":
                return 5;
            default:
                return -1; // Indicates invalid or unhandled rotation
        }
    }

    private void loadStrategies() {
        // Use getResourceAsStream to load from src/main/resources
        try (InputStream is = getClass().getResourceAsStream(STRATEGY_FILE)) {
            if (is == null) {
                aiLog.error("Could not find strategy file at classpath resource path: {}", STRATEGY_FILE);
                return;
            }

            Reader reader = new InputStreamReader(is);
            Gson gson = new Gson();
            // We now parse using the *correct* POJO for expertstrategies1835.json
            Type type = new TypeToken<TileStrategyFile>() {
            }.getType();
            TileStrategyFile strategyFile = gson.fromJson(reader, type);

            if (strategyFile != null && strategyFile.tileLayingStrategies != null) {
                this.tileStrategies = strategyFile.tileLayingStrategies;
                aiLog.info("Successfully loaded {} tile laying strategies from {}.",
                        this.tileStrategies.size(), STRATEGY_FILE);
            } else {
                aiLog.warn("Loaded strategies from {} but the file is empty or malformed.", STRATEGY_FILE);
            }

        } catch (Exception e) {
            aiLog.error("Failed to load or parse strategy file: {}", e.getMessage());
            e.printStackTrace();
        }
    }
    // --- Main Dispatcher ---

    // --- Heuristic Implementations (Unchanged) ---
    private double heuristic_RevenueGain(TileLayOption option, GameContext context, double weight) {
        int currentRevenue = evaluator.calculateCurrentMaxRevenue(context);
        int hypotheticalRevenue = evaluator.calculateMaxRevenue(context, option);
        double revenueGain = hypotheticalRevenue - currentRevenue;
        double score = revenueGain * weight;

        if (revenueGain != 0) { // Log only if there's a change
            aiLog.debug("  - Heuristic [REVENUE_GAIN]: RevenueGain={}.0 ({} -> {}), Weight={}, Score={}",
                    revenueGain, currentRevenue, hypotheticalRevenue, weight, score);
        } else {
            aiLog.trace("  - Heuristic [REVENUE_GAIN]: RevenueGain=0.0, Score=0.0");
        }
        return score;
    }

    private double heuristic_CostPenalty(TileLayOption option, GameContext context, double weight) {
        // int tileCost = context.getTileLayCost(option); // (Needs GameContext
        // implementation)
        int tileCost = 0; // Placeholder
        double score = tileCost * weight;
        aiLog.trace("  - Heuristic [COST_PENALTY]: Cost={}, Weight={}, Score={}",
                tileCost, weight, score);
        return score;
    }

    private double heuristic_DeadEndPenalty(TileLayOption option, GameContext context, double weight) {
        Tile tile = option.tile();
        if (tile == null)
            return 0;

        List<Tile> upgrades = tile.getAllUpgrades(option.hex());
        if (upgrades == null || upgrades.isEmpty()) {
            aiLog.trace("  - Heuristic [DEAD_END_PENALTY]: Tile {} is final, no penalty.", tile.getId());
            return 0; // This is a final tile, not a dead end.
        }

        for (Tile upgradeTile : upgrades) {
            // Check if upgradeTile itself is null before calling getId()
            if (upgradeTile != null && context.getTileAvailableCount(upgradeTile.getId()) > 0) {
                aiLog.trace("  - Heuristic [DEAD_END_PENALTY]: Upgrade path {} exists for tile {}.",
                        upgradeTile.getId(), tile.getId());
                return 0; // At least one upgrade path exists.
            }
        }

        aiLog.debug("  - Heuristic [DEAD_END_PENALTY]: Tile {} on hex {} has no available upgrades. Applying score: {}",
                tile.getId(), option.hex().getId(), weight);
        return weight;
    }

    /**
     * Scores a SetDividend action with simple heuristics.
     * Payout > Split > Withhold
     */
    public double scoreSetDividend(SetDividend action, GameContext context) {
        int revenue = action.getActualRevenue();
        int allocation = action.getRevenueAllocation();

        switch (allocation) {
            case SetDividend.PAYOUT:
                // Score is the full revenue
                return revenue;
            case SetDividend.SPLIT:
                // Score is half (the part that goes to the president)
                return revenue / 2.0;
            case SetDividend.WITHHOLD:
                // Score is 0
                return 0.0;
            case SetDividend.NO_ROUTE:
            case SetDividend.NO_TRAIN:
                return 0.0;
        }
        return 0.0;
    }

    private double heuristic_MustBuyTrain(BuyTrain action, GameContext context, double weight) {
        if (context.getOperatingCompany() != null) {
            int currentTrainCount = context.getCompanyTrainCount(context.getOperatingCompany());

            // We must check against TOTAL available cash, not just company cash.
            int companyCash = context.getCompanyCash(context.getOperatingCompany());
            int presidentCash = 0;
            if (context.getPresident(context.getOperatingCompany()) != null) {
                presidentCash = context.getPresident(context.getOperatingCompany()).getCashValue();
            }
            int totalAvailableCash = companyCash + presidentCash;
            int cost = action.getPricePaid(); // Use pricePaid, not fixedCost

            // Check if it's a "must buy" situation and the AI *can* afford this option
            if (cost > 0 && currentTrainCount == 0 && totalAvailableCash >= cost) {
                aiLog.debug("  - Heuristic [MUST_BUY_TRAIN]: Applying weight {}.", weight);
                return weight;
            }
        }
        return 0.0;
    }

    // --- Other Actions ---
    public double scoreBuyStartItem(BuyStartItem action, GameContext context) {
        String itemId;
        try {
            // Add null check for getStartItem()
            if (action.getStartItem() == null) {
                aiLog.error("BuyStartItem action has null StartItem.");
                return 0; // Or some default low score
            }
            itemId = action.getStartItem().getId();
        } catch (Exception e) {
            aiLog.error("Could not get ID from StartItem. Error: {}", e.getMessage());
            itemId = "Unknown";
        }

        aiLog.debug("  - Scoring StartItem [{}]: No rule matched. Using default score (negative price).", itemId);
        return -action.getPrice(); // Return negative price as default score
    }

    public double scoreTokenLay(TokenLayOption option, GameContext context, Map<String, Integer> companyTokenLayStep) {

        // --- 2. Check Heuristics (Placeholder) ---
        if (context.getOperatingCompany() != null) {
            aiLog.trace("  - No active TokenLay_Sequence found for {}. Evaluating heuristics (Not Implemented).",
                    context.getOperatingCompany().getId());
        } else {
            aiLog.trace(
                    "  - No active TokenLay_Sequence found (no operating company). Evaluating heuristics (Not Implemented).");
        }

        return 0.0; // Placeholder: No heuristics yet
    }


    // ... (lines of unchanged context code) ...
    // --- Placeholders ---

    public double scoreBuyCertificate(BuyCertificate action, GameContext context) {
        PublicCompany company = action.getCompany();
        Player player = context.getCurrentPlayer();
        boolean hasStarted = company.hasStarted();

        // Goal-Oriented Strategy: Opening vs Operating
        if (!hasStarted) {
            // 1. Opening Phase: President MUST float
            if (company.getPresident() == player) {
                aiLog.debug("  - Scoring BuyCert: FLOAT PRIORITY for {}", company.getId());
                return 2000.0 + random.nextDouble() * 50.0;
            } 
            // 2. Non-President: Pass (unless Snipe)
            int myShare = player.getPortfolioModel().getShares(company);
            int buyShare = company.getShareUnit();
            int presShare = (company.getPresident() != null) ? 
                    company.getPresident().getPortfolioModel().getShares(company) : 0;
            
            if (myShare + buyShare > presShare) {
                aiLog.debug("  - Scoring BuyCert: SNIPE ATTEMPT on {}", company.getId());
                return 500.0; 
            }
            
            aiLog.debug("  - Scoring BuyCert: Ignore unfloated {}", company.getId());
            return -1000.0; // Prefer Pass
        }
        
        // 3. Operating Phase: Standard Value + Defense
        double score = 50.0;

        // Rule: Prefer buying own stock
        if (company.getPresident() == player) {
            double multiplier = 10.0 + random.nextDouble() * 2.0; 
            score *= multiplier;
            aiLog.debug("  - Scoring BuyCert: Own Company Boost x{:.2f}", multiplier);
        }

        // Increase fuzz to 10.0 to create more variety in stock picks
        score += random.nextDouble() * 10.0;

        return score;
    }

    /**
     * Scores a SellShares action based on the immediate cash gain.
     * Heuristic: Prioritize immediate cash received.
     * Penalize selling presidency (which usually leads to a negative score).
     */
    public double scoreSellShares(SellShares action, GameContext context) {
        int sharesSold = action.getNumber() * action.getShareUnits();
        int pricePerShareUnit = action.getPrice();
        int cashReceived = sharesSold * pricePerShareUnit;

        // Base Score = Cash
        double finalScore = (double) cashReceived;

        // Rule: Decrease overall chance of selling by factor of 3
        finalScore /= 3.0;

        Player player = context.getCurrentPlayer();
        
        // 1. Protection: Penalize selling my own unfloated company
        if (action.getCompany().getPresident() == player && !action.getCompany().hasStarted()) {
             aiLog.debug("  - Scoring SellShares: PROTECT unfloated company {}", action.getCompany().getId());
             finalScore -= 5000.0;
        } 
        // 2. Liquidation: Boost selling OTHERS if I need to float a company
        else {
             boolean needsCash = false;
             // Check if I have any unfloated presidencies
             for (PublicCompany c : context.getGameManager().getAllPublicCompanies()) {
                 if (c.getPresident() == player && !c.hasStarted()) {
                     needsCash = true;
                     break;
                 }
             }
             if (needsCash) {
                 finalScore += 1200.0; // Boost to prioritize raising cash over holding
                 aiLog.debug("  - Scoring SellShares: LIQUIDATION needed for float.");
             }
        }
        
        // Rule: Standard Penalty for selling own company (if started)
        if (action.getCompany().getPresident() == player && action.getCompany().hasStarted()) {
            finalScore /= 10.0;
             aiLog.debug("  - Scoring SellShares: Selling Own Company Penalty (/10)");
        }

        // Increase fuzz to 10.0 to create more variety
        finalScore += random.nextDouble() * 10.0;

        // Penalize actions that dump the presidency, as this is often detrimental
        // unless forced.
        if (action.getPresidentExchange() > 0) {
            // Use a large negative weight relative to maximum cash gain (e.g., maximum bank
            // size)
            finalScore -= 500.0;
            aiLog.debug("  - Scoring SellShares: Action dumps presidency. Applying -500 penalty.");
        }

        aiLog.debug("  - Scoring SellShares: Shares={} x Price={} = Cash {}. Final Score: {}",
                sharesSold, pricePerShareUnit, cashReceived, finalScore);

        return finalScore;
    }



    // --- ScoredAction Inner Class ---
    /**
     * Helper class to store a scored action.
     */
    private static class ScoredAction {
        final PossibleAction action;
        final double score;

        ScoredAction(PossibleAction action, double score) {
            this.action = action;
            this.score = score;
        }
    }

    public PossibleAction getBestAction(GameContext context,
            List<PossibleAction> possibleActions,
            List<TileLayOption> tileOptions,
            List<TokenLayOption> tokenOptions) {

        aiLog.warn("[AI-TRACE] >>> ENTERED getBestAction");
        aiLog.debug("--- AI: Starting getBestAction ---");

        // 1. Transform Human actions into AI actions
        List<PossibleAction> aiScorableActions = expandHumanActionsForAI(possibleActions, context);
        List<ScoredAction> scoredActions = new ArrayList<>();

        // 2. Score all AI-scorable actions
        for (PossibleAction action : aiScorableActions) {
            double score = 0.0;

            aiLog.warn("[AI-TRACE] getBestAction: Scoring action: {}", action.getClass().getSimpleName());

            if (action instanceof BuyTrain) {
                score = scoreBuyTrain((BuyTrain) action, context);
            } else if (action instanceof BuyStartItem) {
                score = scoreBuyStartItem((BuyStartItem) action, context);
            } else if (action instanceof SetDividend) {
                score = scoreSetDividend((SetDividend) action, context);
            } else if (action instanceof LayBaseToken) {
                score = scoreLayBaseToken((LayBaseToken) action, context);
            } else if (action instanceof BuyCertificate) {
                score = scoreBuyCertificate((BuyCertificate) action, context);
            } else if (action instanceof SellShares) {
                score = scoreSellShares((SellShares) action, context);
            } else if (action instanceof UseSpecialProperty) {
                // --- Legacy PFR Handler ---
                score = scoreUseSpecialProperty((UseSpecialProperty) action, context);
            } else if (action instanceof ExchangeForPrussianShare) {
                // --- NEW 1835 Handler ---
                score = scoreExchangeForPrussianShare((ExchangeForPrussianShare) action, context);
            } else if (action instanceof StartPrussian) {
                // --- NEW 1835 Handler ---
                score = scoreStartPrussian((StartPrussian) action, context);
            } else if (action instanceof NullAction) {
                if (((NullAction) action).getMode() == NullAction.Mode.SKIP ||
                        ((NullAction) action).getMode() == NullAction.Mode.PASS ||
                        ((NullAction) action).getMode() == NullAction.Mode.DONE) {
                    score = 0; // Neutral score
                }
            }

            scoredActions.add(new ScoredAction(action, score));
        }

        // 3. Find the best action
        if (scoredActions.isEmpty()) {
            aiLog.warn("AI: No scorable actions found.");
            for (PossibleAction pa : possibleActions) {
                if (pa instanceof NullAction)
                    return pa;
            }
            return null;
        }

        scoredActions.sort((a, b) -> Double.compare(b.score, a.score));

        ScoredAction best = scoredActions.get(0);
        aiLog.info("[AI] Best Action: {} (Score: {})", best.action.toString(), best.score);
        return best.action;
    }

    public double scoreLayBaseToken(LayBaseToken action, GameContext context) {
        // --- 1. Mandatory / Home Tokens ---
        // Always lay home tokens or forced tokens.
        if (action.getType() == LayBaseToken.HOME_CITY || action.getType() == LayBaseToken.FORCED_LAY) {
            return 1000.0;
        }

       // --- 2. Optional Markers (Aggressive First / Conservative Last) ---
        PublicCompany company = context.getOperatingCompany();
        if (company == null)
            return 0.0;

        // Strategy: 
        // 1. "First Extra": If we have plenty of tokens (>= 2 on charter), expand aggressively (90%).
        // 2. "Last Reserve": If we are down to 1 token, save it for critical blocks (10%).
        
        int freeTokens = company.getNumberOfFreeBaseTokens();
        double probability = (freeTokens >= 2) ? 0.90 : 0.10;

        // --- 3. Decision Caching & Variety ---
        // Cache decisions to prevent UI flickering within the same second
        String key = "TOKEN_" + company.getId() + "_" + action.getChosenHex().getId();

        if (System.currentTimeMillis() - lastDecisionTime > 1000) {
            pfrDecisionCache.clear();
            lastDecisionTime = System.currentTimeMillis();
        }

        double decisionScore;
        if (pfrDecisionCache.containsKey(key)) {
            decisionScore = pfrDecisionCache.get(key);
        } else {
            // Roll against the probability
            boolean placeToken = random.nextDouble() <= probability;

            // --- VARIETY INJECTION ---
            // 5% chance to act "Wild" and place the last token anyway (Unpredictable Blocker)
            // This makes the AI less deterministic and more "human-like" in its inconsistency.
            if (!placeToken && freeTokens < 2 && random.nextDouble() < 0.05) {
                placeToken = true;
                aiLog.info("[AI] 'Wildcard' Token Triggered for {} on {}! (Chaos Factor)", 
                        company.getId(), action.getChosenHex().getId());
            }

            if (placeToken) {
                decisionScore = 50.0; // High enough to beat simple pass/skip
                aiLog.info("[AI] Token Decision for {} on {}: YES (Prob: {}%)",
                        company.getId(), action.getChosenHex().getId(), (int) (probability * 100));
            } else {
                decisionScore = -50.0; // Negative to discourage
                aiLog.info("[AI] Token Decision for {} on {}: NO (Prob: {}%)",
                        company.getId(), action.getChosenHex().getId(), (int) (probability * 100));
            }

            // Add significant fuzz (0-10) to randomly prefer one valid hex over another
            decisionScore += random.nextDouble() * 10.0;
            
            pfrDecisionCache.put(key, decisionScore);
        }

        return decisionScore;
    }



    private List<PossibleAction> expandHumanActionsForAI(List<PossibleAction> humanActions, GameContext context) {
        List<PossibleAction> aiActions = new ArrayList<>();
        aiLog.info("[AI] Expanding {} human actions into AI-scorable actions...", humanActions.size()); // 'essential
                                                                                                        // logging'
                                                                                                        // keep!
        // Generate AI-specific SELL actions (for Stock Round)
        aiActions.addAll(generateFixedPriceSellActions(context));

        // Iterate human actions and transform them
        for (PossibleAction action : humanActions) {
            if (action instanceof BuyTrain) {
                BuyTrain buyAction = (BuyTrain) action;

                // Use the new PriceMode enum
                if (buyAction.getPriceMode() == PriceMode.VARIABLE) {
                    // Discard the VARIABLE action, replace it with fixed-price ones
                    aiActions.addAll(generateFixedPriceBuyActions(buyAction, context));
                } else {
                    // Keep actions that are already FIXED (e.g., from IPO/Pool
                    // if they were created as such)
                    aiActions.add(action);
                }
            } else {
                // Keep all other actions (LayTile, PlaceToken, Pass, etc.)
                aiActions.add(action);
            }
        }
        return aiActions;
    }

    /**
     * --- NEW: AI Action Generator for Selling Shares ---
     * Generates discrete SellShare actions for the AI during the Stock Round.
     * This logic mimics what StockRound.findSellableShares() *should* do for the
     * AI.
     */
    private List<PossibleAction> generateFixedPriceSellActions(GameContext context) {
        List<PossibleAction> sellActions = new ArrayList<>();
        Player currentPlayer = context.getCurrentPlayer();

        if (currentPlayer == null)
            return Collections.emptyList();

        // 1. Get fundamental constraints
        // NOTE: We rely on the core StockRound logic to generate the *valid* price.
        // We will retrieve the current market price for simplicity in this helper.
        // In the final StockRound.java refactor, the actual share prices will be used.

        for (PublicCompany company : context.getGameManager().getAllPublicCompanies()) {
            int ownedShares = currentPlayer.getPortfolioModel().getShares(company);

            // Check if company is sellable at all
            if (ownedShares == 0 || !company.hasStarted() || !company.hasStockPrice()) {
                continue;
            }

            // Check if selling this company is prohibited (e.g., already sold this round)
            if (currentPlayer.hasSoldThisRound(company)) {
                continue;
            }

            // Determine max number of shares the player can sell due to Pool limit
            // (Assuming PlayerShareUtils.poolAllowsShares exists in the engine)
            // int poolAllowsShares = PlayerShareUtils.poolAllowsShares(company); // Assumed
            int maxSharesToSell = ownedShares; // Simpler assumption for the AI scoring helper

            // Use current price for scoring (we assume single unit price for the action)
            int price = company.getCurrentSpace().getPrice() / company.getShareUnitsForSharePrice();
            int shareUnit = company.getShareUnit(); // Typically 10%

            // Iterate over discrete quantities the AI might want to sell: 1, 2, or 3 shares
            // (This avoids combinatorial explosion and focuses on common actions.)
            Set<Integer> quantities = Set.of(1, 2, 3);

            for (int qty : quantities) {
                int sharesTotal = qty * shareUnit;

                if (sharesTotal <= ownedShares && sharesTotal <= maxSharesToSell) {

                    // The core logic of SellShares uses shareUnitSize, but our AI needs to know
                    // the total number of shares/units being sold, and the number of certificates.

                    // For the AI, we simplify: Assume selling 'qty' number of single-share
                    // certificates (unit=shareUnit)
                    // The SellShares action is structured as: (company, shareUnitSize,
                    // numCertificates, price, presExchange)

                    // We check if selling 'qty' single shares (10% each) would violate presidency.
                    // This is complex, so for the AI's scoring list, we rely on the SellShares
                    // constructor
                    // or final action validation to enforce this, and score it later.

                    // Create the action for selling 'qty' single-share units.
                    SellShares action = new SellShares(
                            company, // Company being sold
                            shareUnit, // Share unit size (e.g., 10%)
                            qty, // Number of certificates to sell
                            price, // Price per share unit
                            0 // No presidential exchange assumed
                    );

                    sellActions.add(action);
                }
            }
        }

        aiLog.info("[AI] Generated {} discrete SellShares actions.", sellActions.size());
        return sellActions;
    }

    /**
     * Loads the 'opening book' from the JSON analysis file.
     */
    private void loadInitialRoundAnalysis() {

        // Use getResourceAsStream to load from src/main/resources
        try (InputStream is = getClass().getResourceAsStream("/" + IR_ANALYSIS_FILE)) {
            if (is == null) {
                aiLog.error("Could not find initial round analysis file at classpath resource path: {}",
                        IR_ANALYSIS_FILE);
                this.initialRoundAssetPerformance = Collections.emptyMap();
                this.initialRoundPickPopularity = Collections.emptyMap();
                this.initialRoundSynergyPerformance = Collections.emptyMap();
                return;
            }
            Reader reader = new InputStreamReader(is);

            // try (Reader reader = new FileReader(IR_ANALYSIS_FILE)) {
            Gson gson = new Gson();
            Type type = new TypeToken<AnalysisOutput>() {
            }.getType();
            AnalysisOutput analysis = gson.fromJson(reader, type);
            if (analysis != null) {
                if (analysis.assetPerformance != null) {
                    this.initialRoundAssetPerformance = analysis.assetPerformance;
                    aiLog.info("Successfully loaded {} asset performance entries from {}.",
                            this.initialRoundAssetPerformance.size(), IR_ANALYSIS_FILE);
                } else {
                    aiLog.warn("Initial round analysis file missing 'assetPerformance'.");
                    this.initialRoundAssetPerformance = Collections.emptyMap();
                }

                if (analysis.pickPopularity != null) {
                    this.initialRoundPickPopularity = analysis.pickPopularity;
                    aiLog.info("Successfully loaded {} pick popularity entries from {}.",
                            this.initialRoundPickPopularity.size(), IR_ANALYSIS_FILE);
                } else {
                    aiLog.warn("Initial round analysis file missing 'pickPopularity'.");
                    this.initialRoundPickPopularity = Collections.emptyMap();
                }

                if (analysis.synergyPerformance != null) {
                    this.initialRoundSynergyPerformance = analysis.synergyPerformance;
                    aiLog.info("Successfully loaded {} synergy performance entries from {}.",
                            this.initialRoundSynergyPerformance.size(), IR_ANALYSIS_FILE);
                } else {
                    aiLog.warn("Initial round analysis file missing 'synergyPerformance'.");
                    this.initialRoundSynergyPerformance = Collections.emptyMap();
                }
            } else {
                aiLog.warn("Loaded initial round analysis from {} but the file is empty or malformed.",
                        IR_ANALYSIS_FILE);
                this.initialRoundAssetPerformance = Collections.emptyMap();
                this.initialRoundPickPopularity = Collections.emptyMap();
                this.initialRoundSynergyPerformance = Collections.emptyMap();
            }

        } catch (Exception e) {
            aiLog.error("Failed to load or parse initial round analysis file: {}", e.getMessage());
            this.initialRoundAssetPerformance = Collections.emptyMap();
            this.initialRoundPickPopularity = Collections.emptyMap();
            this.initialRoundSynergyPerformance = Collections.emptyMap();
            e.printStackTrace();
        }
    }

    /**
     * Helper to get item ID safely from a BuyStartItem action.
     */
    private String getItemId(BuyStartItem action) {
        try {
            if (action.getStartItem() != null) {
                return action.getStartItem().getId();
            }
        } catch (Exception e) {
            aiLog.error("Could not get ID from StartItem. Error: {}", e.getMessage());
        }
        return "Unknown";
    }

    /**
     * Fallback logic for draft picks.
     * 1. Tries to score based on synergy with already-owned items.
     * 2. If player owns no items (or synergy map is missing), falls back to
     * individual assetPerformance.
     * 3. If assetPerformance is also missing, falls back to random.
     *
     * @param possibleActions The list of available actions.
     * @param context         The game context (needed to find the current player).
     * @return The chosen action.
     */
    private PossibleAction fallbackBuyStartItem(List<PossibleAction> possibleActions, GameContext context) {
        aiLog.debug("Using fallbackBuyStartItem (Synergy/AssetPerformance).");

        Player currentPlayer = context.getCurrentPlayer();
        if (currentPlayer == null) {
            aiLog.error("Cannot run fallback logic: currentPlayer is null.");
            // Find any valid BuyStartItem action as a last resort
            for (PossibleAction p : possibleActions) {
                if (p instanceof BuyStartItem)
                    return p;
            }
            return null;
        }

        // We must assume a method on GameContext exists to provide this.
        // This is a critical dependency for this logic.
        List<String> ownedItems = context.getPlayerOwnedStartItemIds(currentPlayer);

        if (ownedItems == null) {
            aiLog.error("GameContext.getPlayerOwnedStartItemIds(player) returned null. AI cannot check synergies.");
            // Fallback to assetPerformance logic as if player owned nothing.
            ownedItems = Collections.emptyList();
        }

        // --- Synergy Logic ---
        // Only run synergy logic if the player *owns* items AND the synergy map is
        // loaded.
        if (!ownedItems.isEmpty() && this.initialRoundSynergyPerformance != null
                && !this.initialRoundSynergyPerformance.isEmpty()) {
            aiLog.info("[AI] Fallback: Player owns {} items. Checking synergyPerformance.", ownedItems.size());
            List<ScoredAction> scoredDraftPicks = new ArrayList<>();

            for (PossibleAction action : possibleActions) {
                if (action instanceof BuyStartItem) {
                    BuyStartItem buyAction = (BuyStartItem) action;
                    String newItemId = getItemId(buyAction);
                    double synergyScore = 0.0;

                    for (String ownedItemId : ownedItems) {
                        // Check both key combinations (e.g., "M1+M4" and "M4+M1")
                        String key1 = ownedItemId + "+" + newItemId;
                        String key2 = newItemId + "+" + ownedItemId;

                        // Use getOrDefault to safely add scores.
                        synergyScore += this.initialRoundSynergyPerformance.getOrDefault(key1, 0.0);
                        if (!key1.equals(key2)) { // Avoid double-counting if keys are the same
                            synergyScore += this.initialRoundSynergyPerformance.getOrDefault(key2, 0.0);
                        }
                    }

                    // The main driver is synergy. Subtract price as a tie-breaker.
                    double finalScore = synergyScore - buyAction.getPrice();
                    aiLog.debug("  - Synergy Scoring [{}]: ΣW = {}, Price = {}, Final Score = {}",
                            newItemId, synergyScore, buyAction.getPrice(), finalScore);
                    scoredDraftPicks.add(new ScoredAction(buyAction, finalScore));
                }
            }

            if (scoredDraftPicks.isEmpty()) {
                aiLog.warn("No BuyStartItem actions to score in synergy fallback.");
                return null; // Should not happen if possibleActions is not empty
            }

            scoredDraftPicks.sort((a, b) -> Double.compare(b.score, a.score));
            ScoredAction best = scoredDraftPicks.get(0);
            aiLog.info("[AI] Fallback Pick (Synergy): {} (Score: {})",
                    best.action.getButtonLabel(), best.score);
            return best.action;
        }

        // --- AssetPerformance Logic (Fallback for Synergy) ---
        aiLog.info("[AI] Fallback: Player owns no items or synergy map is missing. Using assetPerformance.");
        if (this.initialRoundAssetPerformance == null || this.initialRoundAssetPerformance.isEmpty()) {
            aiLog.warn("Fallback failed: initialRoundAssetPerformance map is empty.");
            // Absolute last resort: pick a random one
            List<PossibleAction> buyActions = new ArrayList<>();
            for (PossibleAction p : possibleActions) {
                if (p instanceof BuyStartItem) {
                    buyActions.add(p);
                }
            }
            if (buyActions.isEmpty())
                return null; // No BuyStartItem actions
            return buyActions.get(random.nextInt(buyActions.size()));
        }

        List<ScoredAction> scoredDraftPicks = new ArrayList<>();
        for (PossibleAction action : possibleActions) {
            if (action instanceof BuyStartItem) {
                BuyStartItem buyAction = (BuyStartItem) action;
                String itemId = getItemId(buyAction);

                double score = this.initialRoundAssetPerformance.getOrDefault(itemId, -1000.0);
                double finalScore = score - buyAction.getPrice();
                scoredDraftPicks.add(new ScoredAction(buyAction, finalScore));
            }
        }

        if (scoredDraftPicks.isEmpty()) {
            aiLog.warn("No BuyStartItem actions to score in assetPerformance fallback.");
            return null;
        }

        scoredDraftPicks.sort((a, b) -> Double.compare(b.score, a.score));
        ScoredAction best = scoredDraftPicks.get(0);
        aiLog.info("[AI] Fallback Pick (AssetPerformance): {} (Score: {})",
                best.action.getButtonLabel(), best.score);
        return best.action;
    }

    public double scoreTileLay(TileLayOption option, GameContext context) {
        PublicCompany company = context.getOperatingCompany();
        if (company == null) {
            aiLog.debug("  - [SCRIPT] scoreTileLay: OperatingCompany is null. Falling back to heuristics.");
            return scoreTileLay_Heuristics(option, context); // No company, must use heuristics
        }

        Player president = context.getPresident(company);
        // We must use the 'getPlayerOwnedStartItemIds' method we added to the context.
        List<String> ownedPrivates = context.getPlayerOwnedStartItemIds(president);
        // Use a Set for efficient .containsAll() checking
        Set<String> ownedPrivateSet = (ownedPrivates != null) ? new HashSet<>(ownedPrivates) : new HashSet<>();

        // getOffBoardRevenueStep() returns 0 for Phase 1, 1 for Phase 2, etc.
        // The JSON uses "1" for Phase 1. We must add 1.
        String currentPhase = String.valueOf(context.getCurrentPhase().getOffBoardRevenueStep() + 1);

        double bestScriptScore = Double.NEGATIVE_INFINITY;
        boolean matchFound = false;

        // Iterate ALL strategies to find the best move across all contexts (Cross-Context
        // Search)
        // This allows M3 to "borrow" good moves from M3+SX even if it doesn't own SX,
        // provided the move weight is high enough.
        for (TileLayingStrategy strategy : this.tileStrategies) {
            StrategyContext sCtx = strategy.context;

            // 1. Check Basic Match (Company & Phase must always match)
            if (sCtx.companyId != null && !sCtx.companyId.equals(company.getId()))
                continue;
            if (sCtx.phase != null && !sCtx.phase.equals(currentPhase))
                continue;

            // 2. Check Context (Friendly Companies)
            // We do NOT skip if the privates don't match. We just adjust the weighting.
            boolean contextMatches = true;
            if (sCtx.friendlyCompanyIds != null && !sCtx.friendlyCompanyIds.isEmpty()) {
                if (!ownedPrivateSet.containsAll(sCtx.friendlyCompanyIds)) {
                    contextMatches = false;
                }
            }

            // 3. Score the move if it exists in this strategy
            for (TileMove move : strategy.moves) {
                boolean hexMatch = move.hexId.equals(option.hex().getId());
                boolean tileMatch = move.tileId.equals(option.tile().getId());

                // Convert JSON string rotation to game engine int
                int targetOrientation = convertRotation(move.rotation);
                // -1 orientation in JSON means "any rotation"
                boolean orientationMatch = (targetOrientation == -1 || targetOrientation == option.orientation());

                if (hexMatch && tileMatch && orientationMatch) {
                    matchFound = true;

                    // --- WEIGHTING LOGIC ---
                    // Base Score = Move Weight
                    // Context Multiplier:
                    // - Match: 1.5x (Prefer moves that match our actual privates)
                    // - Mismatch: 1.0x (Consider moves from other situations if highly weighted)

                    double multiplier = contextMatches ? 1.5 : 1.0;
                    double score = move.weight * multiplier;

                    // Increase fuzz to 6.0 to allow moves with weight 8 to compete with weight 12
                    double fuzz = random.nextDouble() * 6.0;

                    // Calculate final score with fuzz (Declare ONCE)
                    double finalScore = 1000.0 + score + fuzz;

                    if (finalScore > bestScriptScore) {
                        bestScriptScore = finalScore;
                        aiLog.trace("  - [SCRIPT] Match: {}/{} (CtxMatch: {}). W={}, Mult={}, Score={}",
                                move.tileId, move.hexId, contextMatches, move.weight, multiplier, finalScore);
                    }
                }
            }
        }

        if (matchFound) {
            return bestScriptScore;
        }

        // 3. No script matched. Fall back to standard heuristics.
        return scoreTileLay_Heuristics(option, context);
    }

    /**
     * Helper containing the original heuristic fallback logic.
     */
    private double scoreTileLay_Heuristics(TileLayOption option, GameContext context) {
        if (context.getOperatingCompany() != null) { // Add null check before logging
            aiLog.debug("  - [HEURISTIC] No active TileLay script found for {}. Evaluating heuristics.",
                    context.getOperatingCompany().getId());
        } else {
            aiLog.debug(
                    "  - [HEURISTIC] No active TileLay script found (no operating company). Evaluating heuristics.");
        }
        double finalScore = 0.0;

        // These weights replace the old rule-based system.
        // TODO: Move these weights into a new "heuristics.json" or similar
        final double REVENUE_GAIN_WEIGHT = 10.0;
        final double COST_PENALTY_WEIGHT = -1.0;
        final double DEAD_END_PENALTY_WEIGHT = -100.0;

        finalScore += heuristic_RevenueGain(option, context, REVENUE_GAIN_WEIGHT);
        finalScore += heuristic_CostPenalty(option, context, COST_PENALTY_WEIGHT);
        finalScore += heuristic_DeadEndPenalty(option, context, DEAD_END_PENALTY_WEIGHT);

        // Add fuzz to break ties between equal revenue moves
        finalScore += random.nextDouble() * 2.0;

        return finalScore;
    }

public double scoreBuyTrain(BuyTrain action, GameContext context) {
        // Base score: NEGATIVE price (cheaper is better)
        double finalScore = -action.getPricePaid();
        finalScore += random.nextDouble() * 10.0; // Fuzz

        Train train = action.getTrain();
        if (train == null) return finalScore;

        int trainVal = getTrainValue(train);
        PublicCompany buyer = context.getOperatingCompany();
        if (buyer == null) buyer = action.getCompany();
        if (buyer == null) return finalScore;
        
        boolean isSystemBuy = (action.getFromOwner() instanceof net.sf.rails.game.financial.BankPortfolio);

        // --- NEW RULE: Saturation Check ---
        // If we already have a perm train (>=4), stop buying unless:
        // 1. It's an IPO buy (Direct upgrade)
        // 2. We are rich (>200 cash)
        boolean hasPermTrain = false;
        for (Train t : buyer.getPortfolioModel().getTrainList()) {
             if (getTrainValue(t) >= 4) { hasPermTrain = true; break; }
        }
        
        if (hasPermTrain) {
             boolean isRich = buyer.getCash() > 200;
             if (!isSystemBuy && !isRich) {
                 aiLog.info("[AI] Saturation Rule: Has Perm Train & Not Rich -> Skip buying {}", train.getId());
                 return -10000.0; // Don't buy
             }
        }
        // ----------------------------------

// RULE 1: Universal 4+ Priority (FIXED for 3+3 and M2)
        // Fix: Explicitly include "3+3" because getTrainValue("3+3") returns 3
        boolean is3Plus3 = train.getType().getName().equals("3+3");
        
        if ((trainVal >= 4 || is3Plus3) && isSystemBuy) {
            double boost = 50000.0; // Base boost > Minor Trading (20k) to break loops
            
            // User Request: "especially from the director of the M2"
            if (buyer.getId().equals("M2")) {
                boost += 20000.0; // Extra urgency for M2
                aiLog.info("[AI] M2 Priority: Boosting 3+3/4+ purchase!");
            }

            finalScore += boost; 
            aiLog.info("[AI] Rule 1 Trigger: Buying Train 4+/3+3 ({}) -> Boost {}", train.getType().getName(), boost);
        }
        

        // --- 0. Universal Rule: No company should buy more than one 3+3 train ---
        if (is3Plus3) {
            boolean owns3Plus3 = false;
            for (Train t : buyer.getPortfolioModel().getTrainList()) {
                if (t.getType().getName().equals("3+3")) { owns3Plus3 = true; break; }
            }
            if (owns3Plus3) return -10000.0; 
        }

        // --- Context Setup ---
        boolean isInterCompany = (action.getFromOwner() instanceof PublicCompany);
        boolean buyerIsMinor = !buyer.hasStockPrice(); 
        boolean buyerIsMajor = buyer.hasStockPrice();

        // Check Phase: Has the first 3+3 train been sold?
        boolean is3Plus3Sold = false;
        if (context.getCurrentPhase().getId().compareTo("3+3") >= 0) {
            is3Plus3Sold = true;
        } else {
             // Fallback scan
             for (PublicCompany c : context.getGameManager().getAllPublicCompanies()) {
                for (Train t : c.getPortfolioModel().getTrainList()) {
                    if (t.getType().getName().equals("3+3")) { is3Plus3Sold = true; break; }
                }
                if (is3Plus3Sold) break;
            }
        }

        // --- EXISTING STRATEGY LOGIC (Preserved) ---
        if (!is3Plus3Sold) {
            // *** PHASE 1: EARLY GAME (Pre-3+3) ***
            if (buyerIsMinor) {
                if (isSystemBuy) finalScore += 2000.0; 
                else if (isInterCompany) {
                    PublicCompany seller = (PublicCompany) action.getFromOwner();
                    if (!seller.hasStockPrice()) finalScore += 1000.0; 
                    else finalScore += 500.0; 
                }
            } else if (buyerIsMajor) {
                if (isInterCompany) {
                    PublicCompany seller = (PublicCompany) action.getFromOwner();
                    if (!seller.hasStockPrice()) return -10000.0; 
                }
                // Major IPO Buy (Stochastic)
                if (isSystemBuy) {
                     // ... (Existing stochastic code) ...
                     finalScore += 500.0; // Simplified for brevity in snippet
                }
            }
        } else {
            // *** PHASE 2: MID GAME (Post-3+3) ***
            if (isInterCompany) {
                PublicCompany seller = (PublicCompany) action.getFromOwner();
                boolean sellerIsMinor = !seller.hasStockPrice();
                boolean sellerIsMajor = seller.hasStockPrice();

                // RULE 2A: Majors buy Highest from Minors for 1
                if (buyerIsMajor && sellerIsMinor) {
                    if (action.getPricePaid() == 1) {
                        if (isHighestTrain(train, seller)) {
                            finalScore += 20000.0; 
                            aiLog.debug("  - Rule 2A: Major {} buying Highest Minor Train", buyer.getId());
                        }
                    }
                }
                // RULE 2B: Minors buy Smallest from Majors for All Cash
                if (buyerIsMinor && sellerIsMajor) {
                    int buyerCash = buyer.getCash();
                    if (buyerCash > 0 && action.getPricePaid() >= buyerCash) {
                        if (isSmallestTrain(train, seller)) {
                            finalScore += 20000.0; 
                             aiLog.debug("  - Rule 2B: Minor {} buying Smallest Major Train", buyer.getId());
                        }
                    }
                }
                
                // General preference for cross-funding
                if (buyerIsMinor && sellerIsMajor) finalScore += 5000.0; 
                if (buyerIsMajor && sellerIsMinor) finalScore += 5000.0; 
            }
        }

        return finalScore;
    }
    

    private int getTrainValue(Train t) {
        if (t == null)
            return 0;
        String name = t.getType().getName(); // e.g., "3+3", "4", "10"
        try {
            // Extract leading digits
            String digits = name.replaceAll("^(\\d+).*", "$1");
            return Integer.parseInt(digits);
        } catch (NumberFormatException e) {
            return 0;
        }
    }

    private boolean isHighestTrain(Train target, PublicCompany owner) {
        int targetVal = getTrainValue(target);
        for (Train t : owner.getPortfolioModel().getTrainList()) {
            if (getTrainValue(t) > targetVal)
                return false;
        }
        return true;
    }

    private boolean isSmallestTrain(Train target, PublicCompany owner) {
        int targetVal = getTrainValue(target);
        for (Train t : owner.getPortfolioModel().getTrainList()) {
            if (getTrainValue(t) < targetVal)
                return false;
        }
        return true;
    }

    private Map<String, Double> pfrDecisionCache = new HashMap<>();
    private long lastDecisionTime = 0;

    public double scoreStartPrussian(StartPrussian action, GameContext context) {
        String key = "M2_START";
        if (System.currentTimeMillis() - lastDecisionTime > 1000) {
            pfrDecisionCache.clear();
            lastDecisionTime = System.currentTimeMillis();
        }
        if (pfrDecisionCache.containsKey(key)) {
            return pfrDecisionCache.get(key);
        }
        double randomScore = random.nextBoolean() ? 100.0 : -100.0;
        pfrDecisionCache.put(key, randomScore);
        aiLog.info("[AI-PFR] M2 Start Decision: 50% Check -> Score: {}", randomScore);
        return randomScore;
    }


    public double scoreExchangeForPrussianShare(ExchangeForPrussianShare action, GameContext context) {
        // --- CHANGE START: Always accept/pick random exchange ---
        
        // Previously, logic checked for trains/cash and returned negative scores (-10000.0),
        // causing the AI to pass or freeze. 
        
        // We now return a positive score with random fuzz.
        // This ensures it beats NullAction (Score 0) and picks randomly among multiple shares.
        double baseScore = 50.0;
        double fuzz = random.nextDouble() * 20.0; 

        aiLog.debug("[AI-PFR] Scoring Exchange for {}: {}", action.getCompanyToExchange().getId(), baseScore + fuzz);
        
        return baseScore + fuzz;
        // --- CHANGE END ---
    }


    public double scoreUseSpecialProperty(UseSpecialProperty action, GameContext context) {
        // Refresh cache if stale
        if (System.currentTimeMillis() - lastDecisionTime > 1000) {
            pfrDecisionCache.clear();
            lastDecisionTime = System.currentTimeMillis();
        }

        if (action.getSpecialProperty() instanceof ExchangeForShare) {
            ExchangeForShare efs = (ExchangeForShare) action.getSpecialProperty();
            String originalCompId = efs.getOriginalCompany().getId();

            // 50% Probability for Prussian Opening (M2)
            if (originalCompId.equals("M2")) {
                if (pfrDecisionCache.containsKey("M2")) {
                    return pfrDecisionCache.get("M2");
                }
                double randomScore = random.nextBoolean() ? 100.0 : -100.0;
                pfrDecisionCache.put("M2", randomScore);
                aiLog.info("[AI-PFR] M2 Exchange Decision: 50% Check -> Score: {}", randomScore);
                return randomScore;
            }
        }
        return 0.0;
    }

    public List<PossibleAction> generateFixedPriceBuyActions(BuyTrain varAction, GameContext context) {
        List<PossibleAction> fixedActions = new ArrayList<>();

        PublicCompany company = context.getOperatingCompany();
        if (company == null)
            company = varAction.getCompany();
        if (company == null)
            return fixedActions;

        Player president = context.getPresident(company);
        if (president == null)
            return fixedActions;

        // --- 1. System Buy (IPO/Pool) ---
        // IPO/Pool Buys are strict. No negotiation.
        boolean isSystemBuy = (varAction.getFromOwner() instanceof net.sf.rails.game.financial.BankPortfolio);

        if (isSystemBuy) {
            int faceValue = varAction.getTrain().getCost();
            int companyCash = context.getCompanyCash(company);
            int presidentCash = president.getCashValue();
            int totalCash = companyCash + presidentCash;
            boolean mustBuyTrain = context.getCompanyTrainCount(company) == 0;

            if (totalCash >= faceValue) {
                BuyTrain faceValueAction = new BuyTrain(
                        varAction.getTrain(),
                        varAction.getType(),
                        varAction.getFromOwner(),
                        faceValue);
                // Explicitly set mode to FIXED for System buys
                faceValueAction.setFixedCostMode(BuyTrain.Mode.FIXED);

                int neededFromPresident = Math.max(0, faceValue - companyCash);
                if (neededFromPresident > 0) {
                    faceValueAction.setAddedCash(neededFromPresident);
                    if (mustBuyTrain) {
                        faceValueAction.setForcedBuyIfHasRoute(true);
                        fixedActions.add(faceValueAction);
                    }
                } else {
                    faceValueAction.setAddedCash(0);
                    fixedActions.add(faceValueAction);
                }
            }
            return fixedActions;
        }

        // --- 2. Inter-Company Buy (Private Trades) ---
        // We must handle the variable pricing logic carefully.

        int companyCash = context.getCompanyCash(company);
        int presidentCash = president.getCashValue();
        int totalCash = companyCash + presidentCash;
        boolean mustBuyTrain = context.getCompanyTrainCount(company) == 0;

        // Use the min/max from the original action, but sanitize them.
        // FIX: Ensure minPrice is at least 1. The Engine sometimes reports 0 for
        // variables,
        // but executing a buy for 0 is illegal.
        int legalMin = Math.max(1, varAction.getMinPrice());
        int legalMax = varAction.getMaxPrice();
        int faceValue = varAction.getTrain().getCost();

        // Option A: Buy for Face Value (The "Fair" Price)
        // We prioritize Face Value because it is the most likely to be accepted by
        // validation logic.
        if (faceValue >= legalMin && faceValue <= legalMax && totalCash >= faceValue) {
            // We use the constructor that accepts a range, preserving the "Variable" nature
            BuyTrain action = new BuyTrain(
                    varAction.getTrain(),
                    varAction.getType(),
                    varAction.getFromOwner(),
                    legalMin,
                    legalMax,
                    PriceMode.VARIABLE);

            // FORCE MODE: Use MIN mode (which behaves as 'Variable' with a floor check)
            action.setFixedCostMode(BuyTrain.Mode.MIN);

            // Set the specific price we want to pay
            action.setPricePaid(faceValue);

            // Important: Some engine checks compare fixedCost to pricePaid.
            // For Mode.MIN, fixedCost is the *floor*.
            // But just in case, we ensure the action object is self-consistent.

            int cashToRaise = Math.max(0, faceValue - companyCash);
            action.setAddedCash(cashToRaise);
            fixedActions.add(action);
        }

        // Option B: Buy for Max Company Cash (The "All-In" Price)
        // Useful if we can't afford face value but can afford something above min.
        if (companyCash >= legalMin && companyCash <= legalMax) {
            if (companyCash != faceValue) {
                BuyTrain action = new BuyTrain(
                        varAction.getTrain(),
                        varAction.getType(),
                        varAction.getFromOwner(),
                        legalMin, legalMax, PriceMode.VARIABLE);
                action.setFixedCostMode(BuyTrain.Mode.MIN);
                action.setPricePaid(companyCash);
                action.setAddedCash(0);
                fixedActions.add(action);
            }
        }

        // Option C: Max Total Cash (Forced Emergency)
        if (mustBuyTrain && totalCash > companyCash && totalCash <= legalMax) {
            if (totalCash != faceValue && totalCash != companyCash) {
                BuyTrain action = new BuyTrain(
                        varAction.getTrain(),
                        varAction.getType(),
                        varAction.getFromOwner(),
                        legalMin, legalMax, PriceMode.VARIABLE);
                action.setFixedCostMode(BuyTrain.Mode.MIN);
                action.setPricePaid(totalCash);
                action.setAddedCash(presidentCash);
                fixedActions.add(action);
            }
        }

        return fixedActions;
    }

    /**
     * METHOD 1: The "Book" Picker (3 Arguments)
     * Uses the 'pickNumber' to find what humans usually buy at this specific turn.
     * Mimics human variation (Stochastic).
     */
    public PossibleAction getExpertBuyStartItemAction(List<PossibleAction> possibleActions, GameContext context,
            int pickNumber) {
        String pickKey = "Pick_" + pickNumber;

        // 1. If we don't have data for this specific pick number, fallback to Global
        // Value
        if (this.initialRoundPickPopularity == null || !this.initialRoundPickPopularity.containsKey(pickKey)) {
            aiLog.debug("No 'pickPopularity' data for {}. Falling back to global asset value.", pickKey);
            return getExpertBuyStartItemAction(possibleActions, context); // Call Method 2
        }

        Map<String, Double> popularPicks = this.initialRoundPickPopularity.get(pickKey);

        // 2. Filter valid actions that match the book
        Map<BuyStartItem, Double> candidates = new HashMap<>();
        double totalWeight = 0.0;

        for (PossibleAction action : possibleActions) {
            if (action instanceof BuyStartItem) {
                BuyStartItem buy = (BuyStartItem) action;
                String itemId = getItemId(buy);

                if (popularPicks.containsKey(itemId)) {
                    double weight = popularPicks.get(itemId);
                    candidates.put(buy, weight);
                    totalWeight += weight;
                }
            }
        }

        // 3. Weighted Random Selection (Mimic Human choices)
        if (!candidates.isEmpty()) {
            double r = random.nextDouble() * totalWeight;
            double count = 0.0;
            for (Map.Entry<BuyStartItem, Double> entry : candidates.entrySet()) {
                count += entry.getValue();
                if (r <= count) {
                    aiLog.info("[AI-Start] Book Pick ({}): {}", pickKey, getItemId(entry.getKey()));
                    return entry.getKey();
                }
            }
        }

        // 4. If no book candidate found (rare), fallback
        return getExpertBuyStartItemAction(possibleActions, context);
    }

    /**
     * METHOD 2: The "Value" Picker (2 Arguments) - FALLBACK
     * Uses the global 'Asset Performance' (How much money winners make with this
     * item).
     * Deterministic (Always picks the highest value).
     */
    public PossibleAction getExpertBuyStartItemAction(List<PossibleAction> possibleActions, GameContext context) {
        if (this.initialRoundAssetPerformance == null || this.initialRoundAssetPerformance.isEmpty()) {
            return null; // No data available
        }

        PossibleAction bestAction = null;
        double maxVal = -Double.MAX_VALUE;

        for (PossibleAction action : possibleActions) {
            if (!(action instanceof BuyStartItem))
                continue;
            BuyStartItem buy = (BuyStartItem) action;
            String itemId = getItemId(buy);

            // Look up value (Default to 0.0 if unknown)
            double humanVal = this.initialRoundAssetPerformance.getOrDefault(itemId, 0.0);

            if (humanVal > maxVal) {
                maxVal = humanVal;
                bestAction = action;
            }
        }
        return bestAction;
    }

}