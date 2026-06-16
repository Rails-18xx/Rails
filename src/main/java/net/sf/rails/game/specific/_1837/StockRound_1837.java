package net.sf.rails.game.specific._1837;

import net.sf.rails.game.*;
import net.sf.rails.game.financial.*;
import net.sf.rails.game.model.PortfolioModel;
import net.sf.rails.game.state.*;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import rails.game.action.*;
import net.sf.rails.common.DisplayBuffer;
import net.sf.rails.common.LocalText;
import net.sf.rails.common.ReportBuffer;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

/**
 * @author martin
 *
 */
public class StockRound_1837 extends StockRound {
    private static final Logger log = LoggerFactory.getLogger(StockRound_1837.class);

    protected final ArrayListState<PublicCompany> compWithExcessTrains = new ArrayListState<>(this,
            "compWithExcessTrains");
    protected final IntegerState discardingCompanyIndex = IntegerState.create(
            this, "discardingCompanyIndex");
    protected final BooleanState discardingTrains = new BooleanState(this,
            "discardingTrains");
    protected PublicCompany[] discardingCompanies;
    protected final ArrayListState<String> triggeredNationals = new ArrayListState<>(this, "triggeredNationals");
    protected final ArrayListState<String> declinedNationals = new ArrayListState<>(this, "declinedNationals");

    public StockRound_1837(GameManager parent, String id) {
        super(parent, id);
    }

    /**
     * Share price goes down 1 space for any number of shares sold.
     */
    @Override
    protected void adjustSharePrice(PublicCompany company, Owner seller, int sharesSold, boolean soldBefore) {
        // No more changes if it has already dropped *in the same turn*
        if (!soldBefore) {
            super.adjustSharePrice(company, seller, 1, soldBefore);
        }
    }

    /**
     * Identifies potential voluntary mergers for the current player and adds them
     * as actions.
     * Uses NullAction(root, Mode.PASS) for the "No" option.
     */
    protected boolean setMergeActions() {
        boolean actionsAdded = false;

        if (currentPlayer == null)
            return false;

        Set<String> processedCompanies = new HashSet<>();

        // 1. Iterate over Certificates (PublicCertificate), NOT Tokens.
        // The Player object uses getPortfolioModel().getCertificates() to return
        // List<PublicCertificate>
        for (PublicCertificate cert : currentPlayer.getPortfolioModel().getCertificates()) {

            PublicCompany company = cert.getCompany();
            // Avoid nulls or processing the same company twice (if player holds 2 shares)
            if (company == null || processedCompanies.contains(company.getId()))
                continue;

            // 2. Check if it is a Minor or Coal company
            String type = company.getType().getId();
            if ("Minor".equals(type) || "Coal".equals(type)) {

                // 3. Find Target
                PublicCompany target = Merger1837.getMergeTarget(gameManager, company);
                // 4. Validate Merge Eligibility
                // - Target must exist
                // - Target must have floated (Rule: "Coal companies... if a corporation has
                // floated")
                // - Player must effectively own the minor (be the President)
                if (target != null && target.hasFloated() && company.getPresident() == currentPlayer) {

                    // 5. Use CORRECT Constructor: (source, target, forced=false)
                    possibleActions.add(new MergeCompanies(company, target, false));

                    processedCompanies.add(company.getId());
                    actionsAdded = true;
                }
            }
        }

        // Note: We do NOT add a NullAction (Pass) here because
        // super.setPossibleActions()
        // already adds the standard Stock Round "Pass" action.

        return actionsAdded;
    }

    public void setBuyableCerts() {
        super.setBuyableCerts();

        // If minors are for sale, the face value should be shown in the Par column.
        for (PossibleAction possibleAction : possibleActions.getList()) {
            if (possibleAction instanceof BuyCertificate) {
                BuyCertificate buyAction = (BuyCertificate) possibleAction;
                PublicCompany company = buyAction.getCompany();
                if (company.getType().getId().startsWith("Minor")
                        && company.getFixedPrice() > 0) {
                    company.getParPriceModel().setPrice(company.getFixedPrice());

                }
            }
        }
    }

    public boolean startCompany(String playerName, StartCompany action) {

        boolean result = super.startCompany(playerName, action);

        // For minors, reset Par column (now having its fixed price)
        PublicCompany company = action.getCompany();
        if (company.getType().getId().startsWith("Minor")) {
            company.getParPriceModel().setPrice(0);
        }

        return result;
    }

    /**
     * Merge a minor into an already started company.
     * 
     * @param action The MergeCompanies chosen action
     * @return True if the merge was successful
     */
    protected boolean mergeCompanies(MergeCompanies action) {

        PublicCompany minor = action.getMergingCompany();
        PublicCompany major = action.getSelectedTargetCompany();

        return mergeCompanies(minor, major, false,
                currentPlayer == null);
    }

    /**
     *
     * 
     * @param minor The minor (or coal company) to be merged...
     * @param major ...into the related major company
     * @return True if the merge was successful
     */
    protected boolean mergeCompanies(PublicCompany minor, PublicCompany major,
            boolean majorPresident, boolean autoMerge) {

        Merger1837.mergeMinor(gameManager, minor, major);

        // Re-evaluate 1837 directorship to handle 10% exchange share shifts
        Merger1837.fixDirectorship(gameManager, major);

        checkFlotation(major);
        hasActed.set(true);
        return true;
    }

    public boolean discardTrain(DiscardTrain action) {

        if (!action.process(this))
            return false;

        finishTurn();

        return true;
    }

    public boolean buyShares(String playerName, BuyCertificate action) {

        boolean result = super.buyShares(playerName, action);

        PublicCompany company = action.getCompany();

        // If the president certificate is bought from the Pool,
        // make sure that it is handled correctly
        if (action.isPresident()) {
            if (!company.hasStarted())
                company.start();
            if (company.getType().getId().equals("National")) {
                if (!company.hasFloated())
                    floatCompany(company);
            } else if (company.getType().getId().equals("Major")) {
                company.checkPresidencyOnBuy(action.getPlayer());
            }
        }

    

        return result;
    }

    @Override
    protected void gameSpecificChecks(PortfolioModel boughtFrom,
            PublicCompany company,
            boolean justStarted) {
        log.debug("1837_DEBUG: gameSpecificChecks. Company=" + company.getId()
                + ", SourceModel=" + (boughtFrom != null ? boughtFrom.toString() : "null"));

        super.gameSpecificChecks(boughtFrom, company, justStarted);

        if (justStarted) {
            StockSpace parSpace = company.getCurrentSpace();
            if (parSpace != null) {
                ((StockMarket_1837) stockMarket).addParSpaceUser(parSpace);
            }
        }

        if (boughtFrom != null) {
            PortfolioModel ipoModel = net.sf.rails.game.financial.Bank.getIpo(gameManager).getPortfolioModel();
            boolean isIPO = boughtFrom.equals(ipoModel);

            if (!isIPO) {
                if (companyBoughtThisTurnWrapper.value() == null) {
                    log.warn("1837_DEBUG: FORCING companyBoughtThisTurnWrapper for Pool buy");
                    companyBoughtThisTurnWrapper.set(company);
                }
            }
        }
    }

    // ... (lines of unchanged context code) ...
    protected boolean processGameSpecificAction(PossibleAction action) {
        log.debug("GameSpecificAction: {}", action);
        boolean result = false;

        if (action instanceof ExchangeMinorAction) {
            // Removed references to deleted variables: specialActionProcessed,
            // specialActionPhase
            ExchangeMinorAction exc = (ExchangeMinorAction) action;
            result = mergeCompanies(exc.getMinor(), exc.getTargetMajor(), false, false);
            if (result) {
                hasActed.set(true);
            }
            return result;
        }

        if (action instanceof MergeCompanies) {
            result = mergeCompanies((MergeCompanies) action);
        } else if (action instanceof DiscardTrain) {
            result = discardTrain((DiscardTrain) action);
        }

        return result;
    }

    @Override
    public void finishRound() {

        if (discardingTrains.value())
            return;

        super.finishRound();
    }

    protected boolean hasExchangeableMinors(Player p, String typeFilter) {
        if (p == null)
            return false;
        boolean found = false;

        for (PublicCertificate cert : p.getPortfolioModel().getCertificates()) {
            PublicCompany comp = cert.getCompany();
            if (comp == null)
                continue;

            String type = comp.getType().getId();

            if (!"Minor".equals(type) && !"Coal".equals(type))
                continue;

            PublicCompany target = Merger1837.getMergeTarget(gameManager, comp);

            boolean isPresident = (comp.getPresident() == p);
            boolean targetExists = (target != null);
            boolean targetFloated = (targetExists && target.hasFloated());
            boolean targetClosed = (targetExists && target.isClosed());
            boolean typeMatch = (typeFilter == null || typeFilter.equals(type));
            boolean compClosed = comp.isClosed();

            if (compClosed)
                continue;
            if (!typeMatch)
                continue;

            if (targetFloated && isPresident && !targetClosed) {

                found = true;
            }
        }

        return found;
    }


    @Override
    public void start() {
        triggeredNationals.clear();
        declinedNationals.clear();

        // All start-of-round CER and NFR triggers are now securely handled by GameManager_1837.nextRound
        super.start();

        if (discardingTrains.value()) {
            discardingTrains.set(false);
        }
    }

    @Override
    public boolean setPossibleActions() {
        if (compWithExcessTrains.isEmpty()) {
            discardingTrains.set(false);
            return super.setPossibleActions();
        }

        // Handle the first company in the list that requires a discard
        PublicCompany company = compWithExcessTrains.get(0);

        // Generate a discard action for EACH unique train type
        for (Train train : company.getPortfolioModel().getUniqueTrains()) {
            Set<Train> singleTrainSet = new HashSet<>();
            singleTrainSet.add(train);

            DiscardTrain action = new DiscardTrain(company, singleTrainSet);
            action.setLabel("Force Discard " + train.getName());
            possibleActions.add(action);
        }

        return true;
    }

    @Override
    public boolean process(PossibleAction action) {

        if (discardingTrains.value()) {
            if (action instanceof DiscardTrain) {
                DiscardTrain discard = (DiscardTrain) action;
                Train train = discard.getSelectedTrain();
                if (train != null) {
                    // Move train to Bank Pool (Standard discard)
                    train.getCard().discard();

                    // Re-evaluate limits
                    checkExcessTrains();
                    if (!discardingTrains.value()) {
                        finishTurn();
                    }
                }
                return true;
            }
            // Allow Undo/Redo/Pass while discarding? usually Undo only.
            // If we are strictly forcing discard, we might block others,
            // but standard 'super.process' handles undo.
            // For now, let's catch Discard and return.
        }

        return super.process(action);
    }

    @Override
    protected void finishTurn() {
        // Restore to standard Stock Round turn finish.
        super.finishTurn();
    }


    protected boolean setTrainDiscardActions() {
        if (compWithExcessTrains.isEmpty()) {
            discardingTrains.set(false);
            return super.setPossibleActions();
        }

        PublicCompany company = compWithExcessTrains.get(0);

        for (Train train : company.getPortfolioModel().getUniqueTrains()) {
            Set<Train> singleTrainSet = new HashSet<>();
            singleTrainSet.add(train);

            DiscardTrain action = new DiscardTrain(company, singleTrainSet);
            action.setLabel("Force Discard " + train.getName());
            possibleActions.add(action);
        }

        // Fix 1: Remove broken GameAction.
        // The ORPanel automatically provides Undo/Redo functionality.
        // We do not need to manually inject a FORCED_UNDO action here.

        return true;
    }

    private void checkExcessTrains() {
        compWithExcessTrains.clear();

        // Fix 2: Fetch Majors explicitly since getAllCompanies() is missing.
        // Only Major companies persist and have train limits in this phase.
        List<PublicCompany> companies = gameManager.getRoot().getCompanyManager().getPublicCompaniesByType("Major");

        if (companies != null) {
            for (PublicCompany comp : companies) {
                // Fix 3: Use getCurrentTrainLimit()
                if (!comp.isClosed() && comp.hasFloated() && comp.getNumberOfTrains() > comp.getCurrentTrainLimit()) {
                    compWithExcessTrains.add(comp);
                }
            }
        }

        if (!compWithExcessTrains.isEmpty()) {
            discardingTrains.set(true);
            setPossibleActions();
        } else {
            discardingTrains.set(false);
        }
    }

    public void setNationalFormationDeclined(String nationalId) {
        if (!declinedNationals.contains(nationalId)) {
            declinedNationals.add(nationalId);
        }
    }

    @Override
    public boolean mayPlayerSellShareOfCompany(PublicCompany company) {
        if (!super.mayPlayerSellShareOfCompany(company)) {
            return false;
        }
        
        // National companies (KK, UG, etc.) are sellable as soon as they are formed,
        // bypassing the standard 1837 restriction.
        if ("National".equals(company.getType().getId())) {
            return true;
        }
        
        // Enforce 1837 Rule 9.1: No shares may be sold in a corporation that has not yet operated.
        return company.hasOperated();
    }




    @Override
    public void resume() {
        log.info("1837_TRACE: Resuming StockRound " + getId());

        // Chain mid-turn cascading formations natively through GameManager
        GameManager_1837 gm = (GameManager_1837) gameManager;
        if (gm.checkAndRunKK(null, this, this)) return;
        if (gm.checkAndRunUG(null, this, this)) return;

        setPossibleActions();
    }


    
}