package net.sf.rails.game.ai;

import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Random;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import net.sf.rails.game.GameManager;
import net.sf.rails.game.Player;
import net.sf.rails.game.PublicCompany;
import net.sf.rails.game.model.PortfolioModel;
import net.sf.rails.game.Phase;
import net.sf.rails.game.Train;
import rails.game.action.*;
import net.sf.rails.game.MapHex;
import net.sf.rails.game.Stop;
import net.sf.rails.game.BaseToken;
import net.sf.rails.game.Station;
import net.sf.rails.game.ai.Actor;
import net.sf.rails.game.ai.TileLayOption;
import net.sf.rails.game.ai.TokenLayOption;
import net.sf.rails.game.ai.data.StateVectorBuilder;
import net.sf.rails.game.CompanyType;
import rails.game.specific._1835.StartPrussian;
import rails.game.specific._1835.ExchangeForPrussianShare;
import java.util.Collections;
import java.util.Comparator;

public class AIPlayer implements Actor {

    private static final Logger aiLog = LoggerFactory.getLogger("AI_Decision_Log");
    private static final Logger log = LoggerFactory.getLogger(AIPlayer.class);

    private GameManager gameManager;
    private Random random = new Random();

    // --- AI Service Instances ---
    private final AIEvaluatorService evaluator;
    private final AIPathfinderService pathfinder;
    private final ExpertStrategyService expertStrategyService;

    // Static memory for quotas/round tracking
    private static Map<String, Integer> companyTileLayStep = new HashMap<>();
    private static Map<String, Integer> trainsBoughtThisRound = new HashMap<>();
    private static Map<String, Map<Integer, Integer>> trainBuyQuotas = new HashMap<>();
    private static int lastSeenOR = -1;

    // Neural Network Components
    private NeuralNetEvaluator nnEvaluator;
    private StateVectorBuilder vectorBuilder;
    private static final String MODEL_PATH = "python/rails_1835_v1.onnx";
    private boolean nnInitialized = false;

    private enum AiRoundType {
        INITIAL_AUCTION,
        SHARE_ROUND,
        OPERATING_ROUND,
        FORMATION_ROUND,
        UNKNOWN
    }

    public AIPlayer(String name, GameManager gameManager) {
        if (gameManager == null) {
            throw new IllegalArgumentException("GameManager cannot be null for AIPlayer");
        }
        this.gameManager = gameManager;
        this.evaluator = new AIEvaluatorService();
        this.pathfinder = new AIPathfinderService();
        this.expertStrategyService = new ExpertStrategyService(this.evaluator, this.pathfinder);

        if (trainBuyQuotas.isEmpty()) {
            populateTrainBuyQuotas();
        }
        // // Initialize NN lazily or here (catch exception to avoid crash if model
        // missing)
        // try {
        // this.nnEvaluator = new NeuralNetEvaluator(MODEL_PATH);
        // this.vectorBuilder = new StateVectorBuilder(gameManager.getRoot());
        // this.nnInitialized = true;
        // log.info("AI Neural Network loaded successfully: {}", MODEL_PATH);
        // } catch (Exception e) {
        // log.error("Failed to load AI Neural Network. Falling back to Expert/Random.",
        // e);
        // this.nnInitialized = false;
        // }

    }

    private void populateTrainBuyQuotas() {
        trainBuyQuotas.put("M1", Map.of(1, 1));
        trainBuyQuotas.put("M2", Map.of(1, 1, 2, 0, 3, 0));
        trainBuyQuotas.put("M3", Map.of(1, 1));
        trainBuyQuotas.put("M4", Map.of(1, 2));
        trainBuyQuotas.put("M5", Map.of(1, 1));
        trainBuyQuotas.put("M6", Map.of(1, 1));
        trainBuyQuotas.put("BY", Map.of(1, 2, 2, 2));
        trainBuyQuotas.put("SX", Map.of(1, 3));
        aiLog.info("AI train buy quotas populated.");
    }

    // --- HELPER METHODS (Restored) ---

    private AiRoundType detectRoundType(PossibleActions possibleActions) {
        if (possibleActions.contains(BuyStartItem.class))
            return AiRoundType.INITIAL_AUCTION;
        if (possibleActions.contains(BuyCertificate.class) || possibleActions.contains(SellShares.class))
            return AiRoundType.SHARE_ROUND;
        if (!possibleActions.getType(StartPrussian.class).isEmpty() ||
                !possibleActions.getType(ExchangeForPrussianShare.class).isEmpty()) {
            return AiRoundType.FORMATION_ROUND;
        }
        if (possibleActions.contains(LayTile.class) || possibleActions.contains(LayBaseToken.class) ||
                possibleActions.contains(SetDividend.class) || possibleActions.contains(BuyTrain.class) ||
                possibleActions.contains(DiscardTrain.class)) {
            return AiRoundType.OPERATING_ROUND;
        }
        return AiRoundType.UNKNOWN;
    }

    private PossibleAction handleInitialRound(PossibleActions possibleActions) {
        Player currentPlayer = gameManager.getCurrentPlayer();
        Phase currentPhase = gameManager.getCurrentPhase();
        GameContext context = new LiveGameContext(this.gameManager, null, currentPlayer, currentPhase);

        List<PossibleAction> buyActions = new ArrayList<>(possibleActions.getType(BuyStartItem.class));
        if (buyActions.isEmpty())
            return findFallbackAction(possibleActions.getList());

        // In Clemens, all unsold items are available.
        // So: Pick Number = (Total Items - Available Items) + 1
        final int TOTAL_START_ITEMS = 13;
        int availableItems = buyActions.size();
        int pickNumber = TOTAL_START_ITEMS - availableItems + 1;

        // Call the 3-argument method we just restored
        PossibleAction bestAction = expertStrategyService.getExpertBuyStartItemAction(possibleActions.getList(),
                context, pickNumber);

        if (bestAction != null) {
            BuyStartItem action = (BuyStartItem) bestAction;
            if (action.getPrice() <= currentPlayer.getCash()) {
                action.select();
                action.setAIAction(true);
                return action;
            }
        }
        return findFallbackAction(possibleActions.getList());
    }

    private PossibleAction handleShareRound(PossibleActions possibleActions) {
        // : Wrap in ArrayList to allow sorting ---
        List<BuyCertificate> buyCertActions = new ArrayList<>(possibleActions.getType(BuyCertificate.class));

        List<SellShares> sellActions = possibleActions.getType(SellShares.class);
        Player currentPlayer = gameManager.getCurrentPlayer();

        if (currentPlayer == null)
            return findFallbackAction(possibleActions.getList());

        int certLimit = gameManager.getPlayerCertificateLimit(currentPlayer);
        int currentCerts = currentPlayer.getPortfolioModel().getCertificates().size();
        boolean forceSell = (currentCerts >= certLimit);

        if (!sellActions.isEmpty() && forceSell) {
            SellShares actionToSell = sellActions.get(0);
            actionToSell.setNumber(1);
            actionToSell.setAIAction(true);
            return actionToSell;
        }

        if (currentCerts >= certLimit)
            return findFallbackAction(possibleActions.getList());

        if (!buyCertActions.isEmpty()) {
            // STRATEGY: Director Protection & Magic Companies
            // 1. Identify "Magic" companies (PR, BY, SX).
            // 2. Identify "My" companies (Where I am President).

            // Sort priority:
            // 1. Defend my Presidency (if < 60%)
            // 2. Buy Magic Companies (PR > BY/SX)
            // 3. Buy High Value (Greedy)

            buyCertActions.sort((a, b) -> {
                PublicCompany companyA = a.getCompany();
                PublicCompany companyB = b.getCompany();

                boolean amPrezA = companyA.getPresident() == currentPlayer;
                boolean amPrezB = companyB.getPresident() == currentPlayer;

                // Priority 1: Defend my own company
                if (amPrezA && !amPrezB)
                    return -1;
                if (!amPrezA && amPrezB)
                    return 1;

                // Priority 2: Magic Companies
                boolean magicA = companyA.getId().equals("PR") || companyA.getId().equals("BY")
                        || companyA.getId().equals("SX");
                boolean magicB = companyB.getId().equals("PR") || companyB.getId().equals("BY")
                        || companyB.getId().equals("SX");

                if (magicA && !magicB)
                    return -1;
                if (!magicA && magicB)
                    return 1;

                // Priority 3: Price (Higher is better)
                return Integer.compare(b.getPrice(), a.getPrice());
            });

            for (BuyCertificate action : buyCertActions) {
                PublicCompany company = action.getCompany();
                boolean amPrez = company.getPresident() == currentPlayer;
                boolean isMagic = company.getId().equals("PR") || company.getId().equals("BY")
                        || company.getId().equals("SX");
                boolean isOtherPrez = company.getPresident() != null && company.getPresident() != currentPlayer;

                // ANTI-HERD: Do not buy into another player's company unless it's Magic
                // (PR/BY/SX)
                if (isOtherPrez && !isMagic) {
                    continue;
                }

                if (action.getPrice() <= currentPlayer.getCash()) {
                    action.setNumberBought(action.getMaximumNumber());
                    action.setAIAction(true);
                    return action;
                }
            }
        }
        return findFallbackAction(possibleActions.getList());
    }

    private PossibleAction handleDiscardTrain(List<DiscardTrain> actions, String logPrefix) {
        // Find cheapest train
        DiscardTrain bestAction = null;
        int minTrainValue = Integer.MAX_VALUE;

        for (DiscardTrain action : actions) {
            Train train = action.getDiscardedTrain();
            if (train != null && train.getCost() < minTrainValue) {
                minTrainValue = train.getCost();
                bestAction = action;
            }
        }

        if (bestAction == null)
            bestAction = actions.get(0);
        bestAction.setAIAction(true);
        return bestAction;
    }

    private BuyTrain findCheapestAffordableTrain(List<BuyTrain> trainActions, int totalAvailableCash) {
        return trainActions.stream()
                .filter(action -> action.getPricePaid() > 0)
                .filter(action -> action.getPricePaid() <= totalAvailableCash)
                .min(Comparator.comparing(BuyTrain::getPricePaid))
                .orElse(null);
    }

    private TileLayOption findFreeTileLay(List<TileLayOption> tileLays) {
        for (TileLayOption option : tileLays) {
            LayTile action = option.originatingAction();
            action.setChosenHex(option.hex());
            action.setLaidTile(option.tile());
            action.setOrientation(option.orientation());
            if (action.getCost() == 0)
                return option;
        }
        return null;
    }

    private PossibleAction configureTileLayAction(TileLayOption option) {
        LayTile tileAction = option.originatingAction();
        tileAction.setChosenHex(option.hex());
        tileAction.setLaidTile(option.tile());
        tileAction.setOrientation(option.orientation());
        tileAction.setAIAction(true);
        return tileAction;
    }

    private PossibleAction configureTokenLayAction(TokenLayOption option) {
        LayBaseToken tokenAction = option.originatingAction();
        tokenAction.setChosenHex(option.hex());
        if (option.stop() != null) {
            tokenAction.setChosenStation(option.stop().getRelatedStationNumber());
        }
        tokenAction.setAIAction(true);
        return tokenAction;
    }

    private PossibleAction configureBaseTokenLayAction(LayBaseToken action) {
        action.setAIAction(true);
        return action;
    }

    private PossibleAction configureTrainAction(BuyTrain action) {
        // FIX: Trust the pricePaid set by ExpertStrategyService.
        // Only default to fixedCost if pricePaid wasn't set.
        if (action.getPricePaid() == 0) {
            action.setPricePaid(action.getFixedCost());
        }
        action.setAIAction(true);
        return action;
    }

    private String getActionDescription(PossibleAction action) {
        if (action instanceof LayTile)
            return "Tile " + ((LayTile) action).getLaidTile().getId();
        if (action instanceof BuyTrain)
            return "Train " + ((BuyTrain) action).getTrain().getId();
        return action.toString();
    }

    public boolean isHuman() {
        return false;
    }

    public String getName() {
        return "AIPlayer";
    }

    public PossibleAction chooseMove(
            PublicCompany operatingCompany,
            PossibleActions possibleActions,
            List<TileLayOption> validTileLays,
            List<TokenLayOption> validTokenLays) {

        Player currentPlayer = gameManager.getCurrentPlayer();
        Phase currentPhase = gameManager.getCurrentPhase();
        GameContext context = new LiveGameContext(this.gameManager, operatingCompany, currentPlayer, currentPhase);

        String logPrefix = (operatingCompany != null && currentPlayer != null)
                ? String.format("[%s (%s)] ", operatingCompany.getId(), currentPlayer.getName().substring(0, 3))
                : (currentPlayer != null ? String.format("[Player %s] ", currentPlayer.getName().substring(0, 3))
                        : "[SYS] ");

        if (operatingCompany != null) {
            int currentOR = context.getAbsoluteORNumber();
            if (currentOR > lastSeenOR) {
                aiLog.info("--- NEW OPERATING ROUND {} DETECTED. Clearing train buy counters. ---", currentOR);
                trainsBoughtThisRound.clear();
                lastSeenOR = currentOR;
            }
        }

        aiLog.info("{}DEBUG: START chooseMove. Available actions: {}", logPrefix, possibleActions.getList().size());

        // 1. Try Neural Network Decision first (if available)
        // if (nnInitialized) {
        // PossibleAction nnAction = getBestNeuralAction(possibleActions.getList());
        // if (nnAction != null) {
        // // If NN found a valid move, use it.
        // return nnAction;
        // }
        // }

        AiRoundType roundType = detectRoundType(possibleActions);
        switch (roundType) {
            case INITIAL_AUCTION:
                return handleInitialRound(possibleActions);
            case SHARE_ROUND:
                return handleShareRound(possibleActions);
            default:
                break;
        }

        // ... (Mandatory Actions - keep unchanged) ...
        List<SetDividend> dividendActions = possibleActions.getType(SetDividend.class);
        if (!dividendActions.isEmpty())
            return handleSetDividend(dividendActions.get(0), operatingCompany, logPrefix);

        List<DiscardTrain> discardActions = possibleActions.getType(DiscardTrain.class);
        if (!discardActions.isEmpty()) {
            // A discard is voluntary if a NullAction (Pass/Done) is also available
            boolean isVoluntary = !possibleActions.getType(NullAction.class).isEmpty();
            boolean canAffordFreshTrain = false;

            if (operatingCompany != null) {
                List<BuyTrain> buyTrainActions = possibleActions.getType(BuyTrain.class);
                for (BuyTrain bt : buyTrainActions) {
                    boolean isFromBank = bt.getFromOwner() == null
                            || bt.getFromOwner().getId().contains("Bank")
                            || bt.getFromOwner().getId().contains("IPO");

                    // Note: Use getPricePaid() or getFixedCost() based on availability
                    int cost = (bt.getPricePaid() > 0) ? bt.getPricePaid() : bt.getFixedCost();
                    if (isFromBank && cost <= operatingCompany.getCash()) {
                        canAffordFreshTrain = true;
                        break;
                    }
                }
            }

            // Forced discards (MUST) always happen.
            // Voluntary discards (MAY) only happen in 10% of cases AND if a fresh train is
            // affordable.
            boolean shouldExecuteDiscard = !isVoluntary || (canAffordFreshTrain && random.nextDouble() < 0.10);

            if (shouldExecuteDiscard) {
                return handleDiscardTrain(discardActions, logPrefix);
            } else {
                aiLog.debug("{}  - AI bypassing voluntary train discard (10% gate or unaffordable).", logPrefix);
            }
        }

        // --- 4. Score All Possible Actions ---
        PossibleAction bestAction = null;
        double bestOverallScore = Double.NEGATIVE_INFINITY;

        if (!validTileLays.isEmpty() && operatingCompany != null) {

            // 1. OPTIMIZATION: Sort moves to evaluate high-potential hexes (Cities/Towns)
            // first.
            // This ensures that if we time out, we likely already checked the best
            // upgrades.
            Collections.sort(validTileLays, new Comparator<TileLayOption>() {
                @Override
                public int compare(TileLayOption o1, TileLayOption o2) {
                    // Prioritize hexes that actually have tokens (Stations/Towns)
                    boolean h1HasTokens = hasTokensSafe(o1.hex());
                    boolean h2HasTokens = hasTokensSafe(o2.hex());

                    if (h1HasTokens && !h2HasTokens)
                        return -1; // o1 first
                    if (!h1HasTokens && h2HasTokens)
                        return 1; // o2 first

                    // Fallback: Deterministic sort by Hex ID
                    return o1.hex().getId().compareTo(o2.hex().getId());
                }
            });

            long startTime = System.currentTimeMillis();
            long TIME_LIMIT_MS = 8000; // Stop after 8 seconds (leaving buffer for overhead)

            for (TileLayOption option : validTileLays) {
                // 2. TIMEOUT CHECK
                if (System.currentTimeMillis() - startTime > TIME_LIMIT_MS) {
                    aiLog.warn("AI CRITICAL: Tile search timed out after {}ms. Returning best found so far.",
                            TIME_LIMIT_MS);
                    break; // EXIT LOOP IMMEDIATELY
                }

                // CRITICAL FIX: Snapshot hex state before simulation
                MapHex targetHex = option.hex();
                List<TokenSnapshot> hexSnapshot = captureTokenState(targetHex);

                double score = -10000.0;
                try {
                    score = expertStrategyService.scoreTileLay(option, context);
                } finally {
                    // FORCE RESTORE (With Smart Restore optimization if you applied the previous
                    // fix)
                    restoreTokenState(targetHex, hexSnapshot);
                }

                if (score > bestOverallScore) {
                    bestOverallScore = score;
                    bestAction = configureTileLayAction(option);
                }
            }
        }

        if (!validTokenLays.isEmpty() && operatingCompany != null) {
            for (TokenLayOption option : validTokenLays) {
                double score = expertStrategyService.scoreTokenLay(option, context, companyTileLayStep);
                if (score > bestOverallScore) {
                    bestOverallScore = score;
                    bestAction = configureTokenLayAction(option);
                }
            }
        }
        List<LayBaseToken> baseTokenActions = possibleActions.getType(LayBaseToken.class);
        if (!baseTokenActions.isEmpty() && operatingCompany != null) {
            for (LayBaseToken tokenAction : baseTokenActions) {
                if (tokenAction.getChosenHex() == null)
                    continue;
                String label = tokenAction.getButtonLabel();
                if (label == null || label.equalsIgnoreCase("Base Marker"))
                    continue;
                double score = expertStrategyService.scoreLayBaseToken(tokenAction, context);
                if (score > bestOverallScore) {
                    bestOverallScore = score;
                    bestAction = configureBaseTokenLayAction(tokenAction);
                }
            }
        }

        // Score Train Buys
        List<BuyTrain> buyTrainActions = possibleActions.getType(BuyTrain.class);
        List<BuyTrain> aiScorableBuyTrainActions = new ArrayList<>();
        if (!buyTrainActions.isEmpty()) {
            for (BuyTrain action : buyTrainActions) {
                // Log original actions to debug "is not allowed"
                // aiLog.debug("{} - Original Action: {}", logPrefix, action.toString());

                if (action.getPriceMode() == PriceMode.VARIABLE) {
                    List<PossibleAction> fixed = expertStrategyService.generateFixedPriceBuyActions(action, context);
                    for (PossibleAction fa : fixed)
                        aiScorableBuyTrainActions.add((BuyTrain) fa);
                } else {
                    aiScorableBuyTrainActions.add(action);
                }
            }
        }


if (!aiScorableBuyTrainActions.isEmpty() && operatingCompany != null) {
            boolean hasTrains = !operatingCompany.getPortfolioModel().getTrainList().isEmpty();
            
            // 1. Find exactly ONE representative IPO train to simulate (the cheapest affordable one)
            BuyTrain ipoTrainToSimulate = null;
            boolean permanentTrainAvailable = false;
            
            for (BuyTrain action : aiScorableBuyTrainActions) {
                boolean isFromBank = action.getFromOwner() == null
                        || action.getFromOwner().getId().contains("Bank")
                        || action.getFromOwner().getId().contains("IPO");
                        
                if (isFromBank) {
                    String tName = action.getTrain().getName();
                    boolean isPermanent = !tName.startsWith("2") && !tName.startsWith("3");
                    if (isPermanent) permanentTrainAvailable = true;
                    
                    int cost = (action.getPricePaid() > 0) ? action.getPricePaid() : action.getFixedCost();
                    if (cost <= operatingCompany.getCash()) {
                        if (ipoTrainToSimulate == null || cost < ipoTrainToSimulate.getFixedCost()) {
                            ipoTrainToSimulate = action;
                        }
                    }
                }
            }

            // 2. Simulate 1 additional run IF an affordable IPO train exists
            boolean ipoIncreasesRevenue = false;
            if (ipoTrainToSimulate != null) {
                int currentMaxRevenue = evaluator.calculateCurrentMaxRevenue(context);
                try {
                    net.sf.rails.algorithms.RevenueAdapter hypoAdapter = net.sf.rails.algorithms.RevenueAdapter.createRevenueAdapter(
                            gameManager.getRoot(), operatingCompany, gameManager.getCurrentPhase(), false);
                    
                    if (hypoAdapter.getNetworkAdapterInternal() != null) {
                        hypoAdapter.getNetworkAdapterInternal().clearGraphCache();
                    }
                    
                    hypoAdapter.populateFromRails();
                    hypoAdapter.addTrain(ipoTrainToSimulate.getTrain()); 
                    hypoAdapter.initRevenueCalculator(true);
                    
                    int hypoRevenue = hypoAdapter.calculateRevenue();
                    if (hypoRevenue > currentMaxRevenue) {
                        ipoIncreasesRevenue = true;
                        aiLog.debug("{}SIMULATION: IPO Train {} increases revenue ({} -> {}).", logPrefix, ipoTrainToSimulate.getTrain().getName(), currentMaxRevenue, hypoRevenue);
                    }
                } catch (Exception e) {
                    aiLog.error("{}SIMULATION ERROR for IPO train: {}", logPrefix, e.getMessage());
                }
            }

            // 3. Decision Gate
            boolean shouldEvaluate = !hasTrains || permanentTrainAvailable || ipoIncreasesRevenue || (random.nextDouble() < 0.10);

            if (shouldEvaluate) {
                for (BuyTrain trainAction : aiScorableBuyTrainActions) {
                    double score = expertStrategyService.scoreBuyTrain(trainAction, context);
                    boolean isFromBank = trainAction.getFromOwner() == null
                            || trainAction.getFromOwner().getId().contains("Bank")
                            || trainAction.getFromOwner().getId().contains("IPO");
                    
                    String tName = trainAction.getTrain().getName();
                    boolean isPermanent = !tName.startsWith("2") && !tName.startsWith("3");

                    // Priority Logic: Break the swapping cycle
                    if (isFromBank) {
                        if (isPermanent) {
                            score += 50000.0; // Critical phase-push / permanent fleet
                        } else if (ipoIncreasesRevenue) {
                            score += 20000.0; // Valid income generator from Bank
                        }
                    } else if (hasTrains) {
                        // Penalize inter-company swaps if we already have trains, stopping the cycle
                        score -= 10000.0; 
                    }

                    if (score > bestOverallScore) {
                        bestOverallScore = score;
                        bestAction = configureTrainAction(trainAction);
                    }
                }
            } else {
                aiLog.debug("{}  - AI bypassing optional train purchase.", logPrefix);
            }
        }



        // PRUSSIAN OPENING STRATEGY:
        // If we can form the Prussian (StartPrussian action) and we own M2
        // (Berlin-Potsdam), do it!
        // This is the specific 1835 mechanic where M2 converts to the Director's share
        // of PR.
        List<StartPrussian> prussianActions = possibleActions.getType(StartPrussian.class);
        if (!prussianActions.isEmpty()) {
            StartPrussian action = prussianActions.get(0);
            aiLog.info("{}STRATEGY: Opening the Prussian Railway immediately.", logPrefix);
            action.setAIAction(true);
            return action;
        }

        if (bestAction instanceof LayBaseToken) {
            if (((LayBaseToken) bestAction).getChosenHex() == null) {
                aiLog.error("{}CRITICAL: Discarding LayBaseToken with NULL HEX.", logPrefix);
                bestAction = null;
            }
        }

        // 4. Final Decision
        if (bestAction != null && bestOverallScore > -9000.0) {

            // CRITICAL FIX: Map "Synthetic" or "Stale" actions back to the live
            // 'possibleActions' list.
            // The AI Pathfinder/Strategy services often work with copies (Options) or
            // generate new Action instances (for pricing).
            // Returning an object not reference-equal to one in 'possibleActions' causes
            // the Engine to reject it,
            // leading to a fallback 'SKIP' and missing markers/actions.

            if (!possibleActions.getList().contains(bestAction)) {
                boolean mapped = false;

                // 1. Handle LayBaseToken (Fixes "Missing Marker" / Baden issue)
                if (bestAction instanceof LayBaseToken) {
                    LayBaseToken synthetic = (LayBaseToken) bestAction;
                    List<LayBaseToken> validTokens = possibleActions.getType(LayBaseToken.class);
                    // Attempt to find the matching valid action (e.g. matching Label or Type)
                    for (LayBaseToken original : validTokens) {
                        boolean labelMatch = (original.getButtonLabel() == null && synthetic.getButtonLabel() == null)
                                ||
                                (original.getButtonLabel() != null
                                        && original.getButtonLabel().equals(synthetic.getButtonLabel()));

                        if (labelMatch) {
                            original.setChosenHex(synthetic.getChosenHex());
                            original.setChosenStation(synthetic.getChosenStation());
                            bestAction = original;
                            mapped = true;
                            aiLog.debug("{}Mapped Stale LayBaseToken back to Original Action.", logPrefix);
                            break;
                        }
                    }
                    // Fallback: if only one valid token action exists, use it (safe default)
                    if (!mapped && validTokens.size() == 1) {
                        LayBaseToken original = validTokens.get(0);
                        original.setChosenHex(synthetic.getChosenHex());
                        original.setChosenStation(synthetic.getChosenStation());
                        bestAction = original;
                        mapped = true;
                    }
                }

                // 2. Handle LayTile (Fixes Stale Pathfinder Options)
                else if (bestAction instanceof LayTile) {
                    LayTile synthetic = (LayTile) bestAction;
                    List<LayTile> validTiles = possibleActions.getType(LayTile.class);
                    if (!validTiles.isEmpty()) {
                        LayTile original = validTiles.get(0); // Usually only one LayTile action is active
                        original.setChosenHex(synthetic.getChosenHex());
                        original.setLaidTile(synthetic.getLaidTile());
                        original.setOrientation(synthetic.getOrientation());
                        bestAction = original;
                        mapped = true;
                        aiLog.debug("{}Mapped Stale LayTile back to Original Action.", logPrefix);
                    }
                }

                // 3. Handle BuyTrain (Fixes Variable Price / Synthetic instances)
                else if (bestAction instanceof BuyTrain) {
                    BuyTrain synthetic = (BuyTrain) bestAction;
                    for (BuyTrain original : possibleActions.getType(BuyTrain.class)) {
                        if (original.getTrain() == synthetic.getTrain() &&
                                original.getFromOwner() == synthetic.getFromOwner()) {
                            original.setPricePaid(synthetic.getPricePaid());
                            original.setFixedCostMode(synthetic.getFixedCostMode());
                            original.setAddedCash(synthetic.getAddedCash());
                            original.setForcedBuyIfHasRoute(synthetic.isForcedBuyIfHasRoute());
                            bestAction = original;
                            mapped = true;
                            aiLog.debug("{}Mapped Synthetic BuyTrain back to Original Action.", logPrefix);
                            break;
                        }
                    }
                }

                if (!mapped) {
                    aiLog.warn("{}Could not map synthetic action {} to a valid PossibleAction! This may cause a SKIP.",
                            logPrefix, bestAction);
                }
            }

            bestAction.setAIAction(true);
            return bestAction;
        }

        // ... (Defaults/Fallback - keep unchanged) ...
        if (roundType == AiRoundType.FORMATION_ROUND) {
            PossibleAction pfrAction = expertStrategyService.getBestAction(context, possibleActions.getList(),
                    validTileLays, validTokenLays);
            if (pfrAction != null && !(pfrAction instanceof NullAction)) {
                pfrAction.setAIAction(true);
                return pfrAction;
            }
        }
        if (!validTileLays.isEmpty()) {
            TileLayOption freeTileLay = findFreeTileLay(validTileLays);
            if (freeTileLay != null)
                return configureTileLayAction(freeTileLay);
        }
        PossibleAction fallback = findFallbackAction(possibleActions.getList());
        if (fallback != null) {
            aiLog.info("{}AI Fallback: {}", logPrefix, fallback.toString());
            return fallback;
        }

        return null;
    }

    private PossibleAction handleSetDividend(SetDividend action, PublicCompany operatingCompany, String logPrefix) {
        int revenue = action.getPresetRevenue();
        int allocation = SetDividend.WITHHOLD;

        // FIX: Always calculate revenue using the adapter to ensure 'Direct Income' is
        // captured.
        // This is critical for 1837 Coal Companies. Even if the engine reports 0
        // (validly),
        // or a positive number, we need the breakdown of Total vs Treasury (Direct)
        // revenue
        // to make the correct Split/Withhold decision and set the action properties
        // correctly.
        if (operatingCompany != null) {
            try {
                // Use fully qualified name to ensure access without adding imports
                net.sf.rails.algorithms.RevenueAdapter ra = net.sf.rails.algorithms.RevenueAdapter.createRevenueAdapter(
                        gameManager.getRoot(),
                        operatingCompany,
                        gameManager.getCurrentPhase());

                // 'true' enables the complex calculator (Multigraph) required for 1837 Special
                // Revenues
                ra.initRevenueCalculator(true);

                int calculatedRevenue = ra.calculateRevenue();
                int directRevenue = ra.getSpecialRevenue();

                // Trust the calculated revenue
                revenue = calculatedRevenue;

                // IMPORTANT: Explicitly set the Direct Income on the action object.
                // If revenue is 0 (no run possible), directRevenue will correctly be 0.
                action.setActualRevenue(revenue);
                action.setActualCompanyTreasuryRevenue(directRevenue);

                aiLog.info("{}AI Revenue Check: Total={} Direct={} (Preset was {})", logPrefix, revenue, directRevenue,
                        action.getPresetRevenue());

            } catch (Exception e) {
                aiLog.error("{}AI Failed to calculate revenue during SetDividend.", logPrefix, e);
            }
        }

        boolean isMinor = false;
        if (operatingCompany != null && operatingCompany.getParent() instanceof CompanyType) {
            isMinor = "Minor".equals(((CompanyType) operatingCompany.getParent()).getId());
        }

        // --- NEW LOGIC: Check for Permanent Train in IPO ---
        boolean needToSaveForPermTrain = false;
        if (operatingCompany != null && !isMinor) {
            // Scan for available trains in IPO (Bank)
            // Note: We need access to Bank. We can try to find it via GameManager or just
            // check constraints.
            // A simpler heuristic: If current Phase allows trains >= 4.
            String phaseName = gameManager.getCurrentPhase().getRealName();
            // Phase names often "2", "3", "4", "5", etc.
            boolean isPermPhase = phaseName.compareTo("4") >= 0;

            if (isPermPhase) {
                // If we don't have a perm train yet, and we are short on cash, WITHHOLD.
                boolean hasPermTrain = false;
                for (Train t : operatingCompany.getPortfolioModel().getTrainList()) {
                    if (getTrainValue(t) >= 4) {
                        hasPermTrain = true;
                        break;
                    }
                }

                if (!hasPermTrain && operatingCompany.getCash() < 1000) { // < 1000 is a safe "need money" check
                    needToSaveForPermTrain = true;
                    aiLog.info("{}STRATEGY: Saving for Permanent Train (IPO) -> Withholding.", logPrefix);
                }
            }
        }
        // ---------------------------------------------------

        int[] allowedAllocations = action.getAllowedRevenueAllocations();
        boolean allowsSplit = false;
        boolean allowsPayout = false;
        for (int allowed : allowedAllocations) {
            if (allowed == SetDividend.SPLIT)
                allowsSplit = true;
            if (allowed == SetDividend.PAYOUT)
                allowsPayout = true;
        }

        if (isMinor) {
            allocation = allowsSplit ? SetDividend.SPLIT : SetDividend.WITHHOLD;
        } else {
            if (needToSaveForPermTrain) {
                allocation = SetDividend.WITHHOLD;
            } else {
                // Major Company Logic: Only withhold ~5% of the time
                boolean shouldWithhold = (random.nextDouble() < 0.05);
                if (shouldWithhold) {
                    allocation = SetDividend.WITHHOLD;
                    aiLog.info("{}STRATEGY: Random 5% Trigger -> Withholding dividend.", logPrefix);
                } else {
                    if (revenue > 0 && allowsPayout)
                        allocation = SetDividend.PAYOUT;
                    else if (revenue > 0 && allowsSplit)
                        allocation = SetDividend.SPLIT;
                    else
                        allocation = SetDividend.WITHHOLD;
                }
            }
        }

        action.setActualRevenue(revenue);
        action.setRevenueAllocation(allocation);
        action.setAIAction(true);
        return action;
    }

    // Helper needed for the check above
    private int getTrainValue(Train t) {
        if (t == null)
            return 0;
        String name = t.getType().getName();
        try {
            String digits = name.replaceAll("^(\\d+).*", "$1");
            return Integer.parseInt(digits);
        } catch (NumberFormatException e) {
            return 0;
        }
    }

    // Helper method for the AI sorting logic.
    // Ensure this is inside the AIPlayer class, but outside any other method.
    private boolean hasTokensSafe(MapHex hex) {
        if (hex == null || hex.getStops() == null)
            return false;
        for (Stop stop : hex.getStops()) {
            if (stop.hasTokens())
                return true;
        }
        return false;
    }
    // ... inside findFallbackAction ...

    // ... (lines of unchanged context code) ...
    private PossibleAction findFallbackAction(List<PossibleAction> actions) {
        // Priority 1: Check for explicit "DONE" Mode (The Fix for AI syncing)
        for (PossibleAction action : actions) {
            if (action instanceof NullAction) {
                NullAction na = (NullAction) action;
                if (na.getMode() == NullAction.Mode.DONE) {
                    aiLog.info("AI Priority: Found DONE action via Mode Enum.");
                    action.setAIAction(true);
                    return action;
                }
            }
        }

        // Priority 2: String Label fallback (Legacy support)
        for (PossibleAction action : actions) {
            if (action instanceof NullAction) {
                String label = action.getButtonLabel();
                if (label != null) {
                    String lower = label.toLowerCase();
                    if (lower.contains("done") || lower.contains("finish") || lower.contains("end")) {
                        action.setAIAction(true);
                        return action;
                    }
                }
            }
        }

        // Priority 3: Any valid NullAction that isn't purely "Skip" if we have other
        // options,
        // but often Skip/Pass is the only choice left.
        for (PossibleAction action : actions) {
            if (action instanceof NullAction) {
                action.setAIAction(true);
                return action;
            }
        }

        // ... (lines of unchanged context code) ...

        if (!actions.isEmpty()) {
            actions.get(0).setAIAction(true);
            return actions.get(0);
        }
        return null;
    }

    private static class TokenSnapshot {
        public final PublicCompany company;
        public final int stationIndex;

        public TokenSnapshot(PublicCompany company, int stationIndex) {
            this.company = company;
            this.stationIndex = stationIndex;
        }
    }

    private List<TokenSnapshot> captureTokenState(MapHex hex) {
        List<TokenSnapshot> snapshot = new ArrayList<>();
        if (hex == null || hex.getStops() == null)
            return snapshot;

        for (Stop stop : hex.getStops()) {
            if (stop.hasTokens()) {
                for (BaseToken token : stop.getBaseTokens()) {
                    if (token != null && token.getParent() != null) {
                        // Record which company has a token on which station index
                        snapshot.add(new TokenSnapshot(token.getParent(), stop.getRelatedStationNumber()));
                    }
                }
            }
        }
        return snapshot;
    }

    private void restoreTokenState(MapHex hex, List<TokenSnapshot> snapshot) {
        if (hex == null || hex.getCurrentTile() == null)
            return;

        // OPTIMIZATION: Capture current state and compare.
        // If the engine successfully reverted the state (or the hex was empty),
        // we skip the expensive remove/add cycle.
        List<TokenSnapshot> currentState = captureTokenState(hex);
        if (!isStateDifferent(snapshot, currentState)) {
            return; // Fast exit: State is consistent.
        }

        // --- Slow Path: Corruption detected, force restore ---
        // 1. Clear current tokens from the hex
        for (Stop stop : hex.getStops()) {
            List<BaseToken> tokens = new ArrayList<>(stop.getBaseTokens());
            for (BaseToken token : tokens) {
                token.moveTo(token.getParent());
            }
        }

        // 2. Restore tokens to their correct stations
        for (TokenSnapshot record : snapshot) {
            Station station = hex.getStation(record.stationIndex);
            if (station != null) {
                Stop targetStop = hex.getRelatedStop(station.getNumber());
                BaseToken tokenToPlace = record.company.getNextBaseToken();

                if (tokenToPlace != null && targetStop != null) {
                    tokenToPlace.moveTo(targetStop);
                }
            }
        }
    }

    private boolean isStateDifferent(List<TokenSnapshot> saved, List<TokenSnapshot> current) {
        if (saved.size() != current.size())
            return true;

        // Check if every token in 'saved' exists in 'current'
        // (Order doesn't strictly matter for validity, but exact matching is safer)
        for (TokenSnapshot s : saved) {
            boolean found = false;
            for (TokenSnapshot c : current) {
                if (c.company == s.company && c.stationIndex == s.stationIndex) {
                    found = true;
                    break;
                }
            }
            if (!found)
                return true;
        }
        return false;
    }

}