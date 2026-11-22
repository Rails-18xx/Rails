package net.sf.rails.game.ai.playground;

import net.sf.rails.common.GameData;
import net.sf.rails.common.GameInfo;
import net.sf.rails.common.GameOptionsSet;
import net.sf.rails.game.GameManager;
import net.sf.rails.game.RailsRoot;
import net.sf.rails.game.Round;
import net.sf.rails.game.ai.AIEvaluatorService;
import net.sf.rails.game.ai.AIPathfinderService;
import net.sf.rails.game.ai.ExpertStrategyService;
import net.sf.rails.game.ai.GameContext;
import net.sf.rails.game.ai.LiveGameContext; 
import net.sf.rails.game.ai.data.StateVectorBuilder;
import rails.game.action.*; 

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.FileWriter;
import java.io.IOException;
import java.io.PrintWriter;
import java.util.*;

public class HeadlessRunner {

    private static final Logger log = LoggerFactory.getLogger(HeadlessRunner.class);
    private static final int MAX_GAMES = 1; 
    private static final int MAX_USEFUL_ACTIONS = 200; // Stop after 200 REAL moves
    
    private static final String CSV_FILE = "training_data_expert.csv";
    private static final String DECISION_LOG = "ai_decisions.log";
    private static final String GAME_NAME = "1835";
    private static final List<String> PLAYERS = Arrays.asList("Bot_1", "Bot_2", "Bot_3", "Bot_4");
    private static final double EPSILON = 0.10; 

    private static class GameStep {
        double[] state;
        String playerName;
        public GameStep(double[] s, String p) { this.state = s; this.playerName = p; }
    }

    public static void main(String[] args) {
        log.info(">>> Starting Narrative Headless Runner <<<");

        try (PrintWriter csvWriter = new PrintWriter(new FileWriter(CSV_FILE, false));
             PrintWriter decisionLog = new PrintWriter(new FileWriter(DECISION_LOG, false))) { 
            
            Random rng = new Random();

            for (int i = 0; i < MAX_GAMES; i++) {
                String msg = String.format("--- Starting Game #%d ---", i + 1);
                log.info(msg);
                decisionLog.println(msg);

                GameManager gameManager = bootstrapNewGame(GAME_NAME, PLAYERS);
                if (gameManager == null) return;
                
                RailsRoot root = gameManager.getRoot();
                StateVectorBuilder vectorBuilder = new StateVectorBuilder(root);
                
                AIEvaluatorService evaluator = new AIEvaluatorService();
                AIPathfinderService pathfinder = new AIPathfinderService();
                ExpertStrategyService expert = new ExpertStrategyService(evaluator, pathfinder);

                List<GameStep> gameHistory = new ArrayList<>();
                int usefulActionCount = 0; // Only count real moves

                // Loop until Game Over OR we have enough GOOD data
                while (!gameManager.isGameOver() && usefulActionCount < MAX_USEFUL_ACTIONS) {
                    
                    Round round = (Round) gameManager.getCurrentRound();
                    if (round == null) break;

                    // 1. Readable Round ID (SR_1, OR_2.1)
                    String roundId = gameManager.getORId(); 
                    if (round.getClass().getSimpleName().contains("Stock")) {
                        roundId = "SR_" + gameManager.getSRNumber();
                    } else if (round.getClass().getSimpleName().contains("Start")) {
                         roundId = "Start_" + gameManager.getStartRoundNumber();
                    } else {
                         roundId = "OR_" + gameManager.getORId();
                    }

                    double[] currentState = vectorBuilder.buildFullStateVector();

                    // 2. Get Actions & Clean List
                    List<PossibleAction> immutableActions = round.getPossibleActionsList();
                    if (immutableActions == null || immutableActions.isEmpty()) break;
                    List<PossibleAction> validActions = new ArrayList<>(immutableActions);
                    
                    // Filter System/Invalid Actions
                    validActions.removeIf(a -> a.getClass().getSimpleName().contains("Correction") 
                                            || a.toString().contains("UNDO") 
                                            || a.toString().contains("REDO"));
                    validActions.removeIf(a -> (a instanceof LayBaseToken) && ((LayBaseToken)a).getChosenHex() == null);
                    validActions.removeIf(a -> (a instanceof LayTile) && (((LayTile)a).getChosenHex() == null || ((LayTile)a).getLaidTile() == null));

                    if (validActions.isEmpty()) break;

                    PossibleAction selectedAction = null;
                    String decisionReason = "Random";

                    // 3. DECIDE
                    if (rng.nextDouble() > EPSILON) {
                        if (round.getClass().getSimpleName().contains("StartRound")) {
                            int startItemsAvailable = 0;
                            for (PossibleAction pa : validActions) if (pa instanceof BuyStartItem) startItemsAvailable++;
                            int pickNumber = 13 - startItemsAvailable + 1;
                            
                            GameContext context = new LiveGameContext(gameManager, null, gameManager.getCurrentPlayer(), gameManager.getCurrentPhase());
                            selectedAction = expert.getExpertBuyStartItemAction(validActions, context, pickNumber);
                            if (selectedAction != null) decisionReason = "Book";
                        } else {
                             for (PossibleAction pa : validActions) {
                                if (pa instanceof LayTile) {
                                    LayTile lt = (LayTile) pa;
                                    GameContext context = new LiveGameContext(gameManager, 
                                        (gameManager.getCurrentRound() instanceof net.sf.rails.game.OperatingRound) ? ((net.sf.rails.game.OperatingRound)gameManager.getCurrentRound()).getOperatingCompany() : null,
                                        gameManager.getCurrentPlayer(), 
                                        gameManager.getCurrentPhase());
                                    
                                    net.sf.rails.game.ai.TileLayOption option = new net.sf.rails.game.ai.TileLayOption(lt.getChosenHex(), lt.getLaidTile(), lt.getOrientation(), lt);
                                    double score = expert.scoreTileLay(option, context);
                                    
                                    if (score > 500.0) { 
                                        selectedAction = pa;
                                        decisionReason = "Expert(" + (int)score + ")";
                                        break;
                                    }
                                }
                            }
                        }
                    }

                    if (selectedAction == null) selectedAction = validActions.get(rng.nextInt(validActions.size()));

                    // 4. EXECUTE & LOG (Only Significant Moves)
                    if (gameManager.process(selectedAction)) {
                        String actor = gameManager.getCurrentPlayer().getName();
                        
                        boolean isInteresting = true;
                        if (selectedAction instanceof NullAction) isInteresting = false;
                        // Filter out generic SetDividend unless strictly required
                        if (selectedAction instanceof SetDividend) isInteresting = false; 

                        if (isInteresting) {
                            usefulActionCount++;
                            
                            String logLine = String.format("[%s] #%d %s | %s | %s", 
                                roundId, usefulActionCount, actor, decisionReason, formatAction(selectedAction));
                            
                            decisionLog.println(logLine);
                            decisionLog.flush();
                            System.out.print("M"); // M = Move
                            
                            // Only save interesting states to CSV
                            gameHistory.add(new GameStep(currentState, actor));
                        } else {
                            System.out.print("."); // . = Skip
                        }
                    } else {
                         if (!validActions.isEmpty()) gameManager.process(validActions.get(rng.nextInt(validActions.size())));
                    }
                }
                
                // Save CSV
                Map<String, Integer> finalCashMap = new HashMap<>();
                for (net.sf.rails.game.Player p : gameManager.getPlayers()) finalCashMap.put(p.getName(), p.getCash());
                for (GameStep step : gameHistory) {
                    StringBuilder sb = new StringBuilder();
                    for (double val : step.state) sb.append(String.format(Locale.US, "%.2f,", val));
                    sb.append(finalCashMap.getOrDefault(step.playerName, 0));
                    csvWriter.println(sb.toString());
                }
                csvWriter.flush();
            }
        } catch (Exception e) { e.printStackTrace(); }
    }

    private static String formatAction(PossibleAction a) {
        try {
            if (a instanceof LayTile) {
                LayTile lt = (LayTile) a;
                String tile = (lt.getLaidTile() != null) ? lt.getLaidTile().getId() : "?";
                String hex = (lt.getChosenHex() != null) ? lt.getChosenHex().getId() : "?";
                return "LayTile: " + tile + " on " + hex + " (Rot " + lt.getOrientation() + ")";
            }
            if (a instanceof BuyTrain) {
                BuyTrain bt = (BuyTrain) a;
                String t = (bt.getTrain() != null) ? bt.getTrain().getName() : "?";
                // --- FIX: Handle Owner Interface ---
                String seller = "IPO";
                if (bt.getFromOwner() != null) {
                    if (bt.getFromOwner() instanceof net.sf.rails.game.PublicCompany) {
                        seller = ((net.sf.rails.game.PublicCompany)bt.getFromOwner()).getId();
                    } else if (bt.getFromOwner() instanceof net.sf.rails.game.Player) {
                        seller = ((net.sf.rails.game.Player)bt.getFromOwner()).getName();
                    } else {
                        seller = bt.getFromOwner().toString();
                    }
                }
                return "BuyTrain: " + t + " from " + seller + " (" + bt.getPricePaid() + ")";
            }
            if (a instanceof BuyStartItem) {
                BuyStartItem bs = (BuyStartItem) a;
                String id = (bs.getStartItem() != null) ? bs.getStartItem().getId() : "?";
                return "BuyStart: " + id + " (" + bs.getPrice() + ")";
            }
            if (a instanceof LayBaseToken) {
                 LayBaseToken lb = (LayBaseToken) a;
                 String hex = (lb.getChosenHex() != null) ? lb.getChosenHex().getId() : "?";
                 return "LayToken: on " + hex;
            }
        } catch (Exception e) { return a.toString(); } 
        return a.toString();
    }
    
    private static GameManager bootstrapNewGame(String gameName, List<String> players) {
        try {
            GameInfo gameInfo = GameInfo.builder().withName(gameName).withMinPlayers(2).withMaxPlayers(4).withOrdering(1).build();
            Map<String, String> options = new HashMap<>();
            options.put("Variant", "Clemens"); 
            options.put("game.Variant", "Clemens"); 
            GameOptionsSet gameOptions = new GameOptionsSet(options);
            GameData gameData = new GameData(gameInfo, gameOptions, players);
            RailsRoot root = RailsRoot.create(gameData); 
            GameManager manager = new GameManager(root, "Headless_" + gameName);
            root.setGameManager(manager);
            manager.startGame();
            return manager;
        } catch (Exception e) { e.printStackTrace(); return null; }
    }
}