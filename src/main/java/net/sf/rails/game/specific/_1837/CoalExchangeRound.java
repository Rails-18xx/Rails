package net.sf.rails.game.specific._1837;

import java.util.*;
import net.sf.rails.game.*;
import net.sf.rails.game.financial.*;
import net.sf.rails.game.model.PortfolioModel;
import net.sf.rails.game.state.*;
import rails.game.action.*;
import net.sf.rails.common.LocalText;
import java.awt.Color;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class CoalExchangeRound extends Round implements GuiTargetedAction {
    private static final Logger log = LoggerFactory.getLogger(CoalExchangeRound.class);

    // State variables for nested polling: Major -> Player (Clockwise from Director)
    protected final StringState majorOrderStr = StringState.create(this, "CER_MajorOrder", "");
    protected final IntegerState currentMajorIndex = IntegerState.create(this, "CER_MajorIdx", 0);
    
    protected final IntegerState currentPlayerIndex = IntegerState.create(this, "CER_PlayerIdx", 0);
    protected final IntegerState playersProcessedCount = IntegerState.create(this, "CER_ProcessedCount", 0);
    
    protected final ArrayListState<String> skippedMinors = new ArrayListState<>(this, "CER_Skipped");
    protected final GenericState<PublicCompany> companyOverLimit = new GenericState<>(this, "CER_CompOverLimit", null);

    public CoalExchangeRound(GameManager parent, String id) {
        super(parent, id);
    }

    public void start() {
        skippedMinors.clear();
        majorOrderStr.set("");
        currentMajorIndex.set(0);
        companyOverLimit.set(null);

        // 1. Collect and sort all floated Majors by Operating Order (Share Price Descending)
        List<PublicCompany> majors = getRoot().getCompanyManager().getPublicCompaniesByType("Major");
        if (majors != null) {
            List<PublicCompany> activeMajors = new ArrayList<>();
            for (PublicCompany c : majors) {
                if (!c.isClosed() && c.hasFloated()) {
                    activeMajors.add(c);
                }
            }
            activeMajors.sort((c1, c2) -> {
                int p1 = c1.getCurrentSpace() != null ? c1.getCurrentSpace().getPrice() : 0;
                int p2 = c2.getCurrentSpace() != null ? c2.getCurrentSpace().getPrice() : 0;
                return Integer.compare(p2, p1); // Descending order
            });
            
            StringBuilder sb = new StringBuilder();
            for (int i = 0; i < activeMajors.size(); i++) {
                sb.append(activeMajors.get(i).getId());
                if (i < activeMajors.size() - 1) sb.append(",");
            }
            majorOrderStr.set(sb.toString());
        }

        
        List<String> majorList = getMajorList();
        if (majorList.isEmpty()) {
            finishRound();
            return;
        }

        setupPlayerIndexForCurrentMajor();
        findNextActivePlayer();
    }
    
@Override 
    public String getPlayerName() { 
        Player p = getCurrentPlayer();
        return (p != null) ? p.getName() : ""; 
    }

    @Override
    public Player getCurrentPlayer() {
        List<Player> players = gameManager.getPlayers();
        if (players != null && currentPlayerIndex.value() >= 0 && currentPlayerIndex.value() < players.size()) {
            return players.get(currentPlayerIndex.value());
        }
        return null;
    }
    
    private List<String> getMajorList() {
        if (majorOrderStr.value() == null || majorOrderStr.value().isEmpty()) return new ArrayList<>();
        return Arrays.asList(majorOrderStr.value().split(","));
    }

    private void setupPlayerIndexForCurrentMajor() {
        List<String> majorList = getMajorList();
        if (currentMajorIndex.value() < majorList.size()) {
            String currentMajorId = majorList.get(currentMajorIndex.value());
            PublicCompany currentMajor = getRoot().getCompanyManager().getPublicCompany(currentMajorId);
            
            // Rule Implementation: Exchanges are made starting with the director and proceeding clockwise.
            int startIdx = 0;
            if (currentMajor != null && currentMajor.getPresident() != null) {
                startIdx = gameManager.getPlayers().indexOf(currentMajor.getPresident());
                if (startIdx == -1) startIdx = 0;
            }
            currentPlayerIndex.set(startIdx);
            playersProcessedCount.set(0);
        }
    }

    private void findNextActivePlayer() {
        List<Player> players = gameManager.getPlayers();
        List<String> majorList = getMajorList();

        // Outer Loop: Iterate through the Majors
        while (currentMajorIndex.value() < majorList.size()) {
            String currentMajorId = majorList.get(currentMajorIndex.value());
            PublicCompany currentMajor = getRoot().getCompanyManager().getPublicCompany(currentMajorId);

            // Inner Loop: Iterate through players starting from Director
            while (playersProcessedCount.value() < players.size()) {
                Player p = players.get(currentPlayerIndex.value());

                if (hasExchangeableMinorsForMajor(p, currentMajor)) {
                    getRoot().getPlayerManager().setCurrentPlayer(p);
                    setPossibleActions();
                    return; // Halt and wait for user input
                }

                // Advance to next player
                currentPlayerIndex.set((currentPlayerIndex.value() + 1) % players.size());
                playersProcessedCount.add(1);
            }

            // Finished polling all players for this Major. Advance to the next Major.
            currentMajorIndex.add(1);
            setupPlayerIndexForCurrentMajor();
        }

        // If we exit the loops, no more exchanges are possible.
        finishRound();
    }

    @Override
    public boolean setPossibleActions() {
        possibleActions.clear();
        Player p = gameManager.getCurrentPlayer();
        if (p == null) return false;

        // 1. Mandatory Discard Resolution
        if (companyOverLimit.value() != null) {
            PublicCompany comp = companyOverLimit.value();


            for (Train train : comp.getPortfolioModel().getUniqueTrains()) {
                Set<Train> singleTrainSet = new HashSet<>();
                singleTrainSet.add(train);
                DiscardTrain action = new DiscardTrain(comp, singleTrainSet);
                action.setLabel("Force Discard " + train.getName());
                possibleActions.add(action);
            }
            return true;
        }

        // 2. Exchange Actions (Restricted to the current Major only)
        List<String> majorList = getMajorList();
        String currentMajorId = majorList.get(currentMajorIndex.value());
        Set<String> processed = new HashSet<>();

        for (PublicCertificate cert : p.getPortfolioModel().getCertificates()) {
            PublicCompany comp = cert.getCompany();
            if (comp == null || comp.isClosed() || processed.contains(comp.getId()) || skippedMinors.contains(comp.getId())) continue;

            PublicCompany target = Merger1837.getMergeTarget(gameManager, comp);
            if (target != null && target.getId().equals(currentMajorId) && comp.getPresident() == p) {
                ExchangeMinorAction action = new ExchangeMinorAction(comp, target, false);

// Expand abbreviations by pulling the full company name if available.
                String minorName = (comp.getId() != null && !comp.getId().isEmpty()) ? comp.getId() : "Minor";
                action.setButtonLabel("Exchange " + comp.getId() + " (" + minorName + ") for " + target.getId() + " Share");

                possibleActions.add(action);
                processed.add(comp.getId());
            }
        }



        PublicCompany currentMajor = getRoot().getCompanyManager().getPublicCompany(currentMajorId);
        
        // Rule 13: Mandatory if IPO is empty (Sold Out)
        boolean isSoldOut = true;
        PortfolioModel ipo = net.sf.rails.game.financial.Bank.getIpo(gameManager).getPortfolioModel();
        for (PublicCertificate cert : ipo.getCertificates()) {
            if (cert.getCompany().equals(currentMajor)) {
                isSoldOut = false;
                break;
            }
        }

        // Rule 13: Mandatory if Phase 5 has started
        // Using your confirmed working syntax:
        boolean isPhase5 = getRoot().getPhaseManager().getCurrentPhase().getId().startsWith("5");

        boolean isMandatory = isSoldOut || isPhase5;

        // If mandatory, we only allow the "Done" action if there are no more exchangeable minors 
        // left for this player for this specific major.
        if (!isMandatory || !hasExchangeableMinorsForMajor(p, currentMajor)) {
            NullAction done = new NullAction(getRoot(), NullAction.Mode.DONE);

// The UI renderer defaults to the class name ("NullAction") if the button label is not explicitly set.
            // setLabel() is insufficient for UI rendering in this context.
            done.setButtonLabel("Skip / Done with " + currentMajorId);
            done.setLabel("Skip / Done with " + currentMajorId);
                        possibleActions.add(done);
        }


        return true;
    }

    private void advancePlayer() {
        currentPlayerIndex.set((currentPlayerIndex.value() + 1) % gameManager.getPlayers().size());
        playersProcessedCount.add(1);
        findNextActivePlayer();
    }

    @Override
    protected void finishRound() {
        gameManager.nextRound(this);
    }

    private boolean hasExchangeableMinorsForMajor(Player p, PublicCompany majorTarget) {
        if (p == null || majorTarget == null) return false;
        
        for (PublicCertificate cert : p.getPortfolioModel().getCertificates()) {
            PublicCompany comp = cert.getCompany();
            if (comp == null || comp.isClosed() || skippedMinors.contains(comp.getId())) continue;
            
            PublicCompany target = Merger1837.getMergeTarget(gameManager, comp);
            if (target != null && target.equals(majorTarget) && comp.getPresident() == p) {
                return true;
            }
        }
        return false;
    }

    @Override public net.sf.rails.game.state.Owner getActor() { return gameManager.getCurrentPlayer(); }
    @Override public String getGroupLabel() { return "Minor Exchanges"; }
    @Override public String getButtonLabel() { return "Exchange"; }
    @Override public Color getButtonColor() { return Color.ORANGE; }

    @Override
    public boolean process(PossibleAction action) {
        if (action instanceof DiscardTrain) {
            DiscardTrain discard = (DiscardTrain) action;
            Train train = discard.getSelectedTrain();
            if (train != null) {
                train.getCard().discard();
                PublicCompany comp = discard.getCompany();

                if (comp.getNumberOfTrains() <= comp.getCurrentTrainLimit()) {
                    companyOverLimit.set(null);
                    
                    List<String> majorList = getMajorList();
                    String currentMajorId = majorList.get(currentMajorIndex.value());
                    PublicCompany currentMajor = getRoot().getCompanyManager().getPublicCompany(currentMajorId);
                    
                    if (!hasExchangeableMinorsForMajor(gameManager.getCurrentPlayer(), currentMajor)) {
                        advancePlayer();
                    } else {
                        setPossibleActions();
                    }
                } else {
                    setPossibleActions();
                }
            }
            return true;
        }

        if (action instanceof ExchangeMinorAction) {
            ExchangeMinorAction exc = (ExchangeMinorAction) action;
            PublicCompany target = exc.getTargetMajor();
            
            Merger1837.mergeMinor(gameManager, exc.getMinor(), target);
            Merger1837.fixDirectorship(gameManager, target);
            
            if (target.getNumberOfTrains() > target.getCurrentTrainLimit()) {
                log.info("1837_LOGIC: Company " + target.getId() + " over train limit. Enforcing discard.");
                companyOverLimit.set(target);
                setPossibleActions(); 
                return true;
            }

            if (!hasExchangeableMinorsForMajor(gameManager.getCurrentPlayer(), target)) {
                advancePlayer();
            } else {
                setPossibleActions();
            }
            return true;
        }

        if (action instanceof NullAction) {
            Player p = gameManager.getCurrentPlayer();
            List<String> majorList = getMajorList();
            String currentMajorId = majorList.get(currentMajorIndex.value());
            
            if (p != null) {
                for (PublicCertificate cert : p.getPortfolioModel().getCertificates()) {
                    PublicCompany comp = cert.getCompany();
                    PublicCompany target = Merger1837.getMergeTarget(gameManager, comp);
                    if (comp != null && target != null && target.getId().equals(currentMajorId)) {
                        skippedMinors.add(comp.getId());
                    }
                }
            }
            advancePlayer();
            return true;
        }
        
        return false;
    }
}