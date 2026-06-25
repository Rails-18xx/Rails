package net.sf.rails.game.specific._1856;

import java.util.*;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.google.common.collect.Lists;

import rails.game.action.*;
import net.sf.rails.common.*;
import net.sf.rails.game.*;
import net.sf.rails.game.financial.Bank;
import net.sf.rails.game.financial.PublicCertificate;
import net.sf.rails.game.financial.StockSpace;
import net.sf.rails.game.special.SellBonusToken;
import net.sf.rails.game.state.ArrayListMultimapState;
import net.sf.rails.game.state.ArrayListState;
import net.sf.rails.game.state.BooleanState;
import net.sf.rails.game.state.Currency;
import net.sf.rails.game.state.GenericState;
import net.sf.rails.game.state.Portfolio;

// Rails 2.0 refactoring

public class CGRFormationRound extends SwitchableUIRound {

    private static final Logger log = LoggerFactory.getLogger(CGRFormationRound.class);

    // static variables
    private final PublicCompany_CGR cgr;
    java.util.List<String> options = new java.util.ArrayList<>();

    // initialized in start() method only
    private Player startingPlayer;
    private int maxLoansToRepayByPresident = 0;

    /*
     * effects from the merger, processed at the end
     * thus no need for state variables
     */
    private Set<Train> trainsToDiscardFrom = null;
    private boolean forcedTrainDiscard = true;
    private List<ExchangeableToken> tokensToExchangeFrom = null;

    // dynamic variables
    private final GenericState<Steps> step = new GenericState<>(this, "step");

    private final ArrayListMultimapState<Player, PublicCompany> companiesToRepayLoans = ArrayListMultimapState
            .create(this, "companiesToRepayLoans");

    private final GenericState<PublicCompany> currentCompany = new GenericState<>(this, "currentCompany");

    private final ArrayListState<PublicCompany> mergingCompanies = new ArrayListState<>(this, "mergingCompanies");

    private final BooleanState cgrHasDiscardedTrains = new BooleanState(this, "cgrHasDiscardedTrains");

    /**
     * Constructed via Configure
     */
    public CGRFormationRound(GameManager parent, String id) {
        super(parent, id);

        guiHints.setVisibilityHint(GuiDef.Panel.MAP, true);
        guiHints.setVisibilityHint(GuiDef.Panel.STATUS, true);

        cgr = (PublicCompany_CGR) getRoot().getCompanyManager().getPublicCompany(PublicCompany_CGR.NAME);
    }

    private void formCGR() {
        ReportBuffer.add(this, "");

        Player temporaryPresident = null;
        Player newPresident = null;
        Player firstCGRowner = null;
        int maxShares = 0;
        int cgrSharesUsed = 0;

        // Exchange the player shares
        for (Player player : playerManager.getNextPlayersAfter(startingPlayer, true, false)) {
            int oldShares = 0, newShares = 0;
            List<PublicCertificate> certs = Lists.newArrayList();
            PublicCertificate poolCert = null;

            // count number of shares for the players in oldShares
            log.debug(player.getPortfolioModel().getCertificates().toString());
            for (PublicCertificate cert : player.getPortfolioModel().getCertificates()) {
                if (mergingCompanies.contains(cert.getCompany())) {
                    log.debug("merge cert= {}", cert);
                    certs.add((cert));
                    oldShares++;
                    if (cert.isPresidentShare()) {
                        oldShares++;
                    }
                }
            }

            if (oldShares > 0) {

                int count = oldShares;
                // no president assigned so far, assign president if there are enough oldShares
                if (count >= 4 && temporaryPresident == null && cgrSharesUsed <= 18) {
                    PublicCertificate cgrCert = cgr.getPresidentsShare();
                    cgrCert.moveTo(player);
                    count -= 4;
                    cgrSharesUsed += 2;
                    newShares += 2;
                    temporaryPresident = player;
                }

                // now convert the remaining shares
                while (count >= 2 && cgrSharesUsed <= 19) {
                    PublicCertificate cgrCert = unavailable.findCertificate(cgr, false);
                    cgrCert.moveTo(player);
                    count -= 2;
                    cgrSharesUsed++;
                    newShares++;
                }

                String message = LocalText.getText("SharesReplacedForShares",
                        player.getId(),
                        oldShares,
                        newShares,
                        PublicCompany_CGR.NAME);
                DisplayBuffer.add(this, message, false);
                ReportBuffer.add(this, message);

                if (count == 1) {
                    // Should work OK even if this is a president's share.
                    // In the pool we will treat all certs equally.
                    poolCert = certs.get(certs.size() - 1);
                    poolCert.moveTo(pool.getParent());
                    certs.remove(poolCert);

                    message = LocalText.getText("HasPutShareInPool",
                            player.getId());
                    DisplayBuffer.add(this, message, false);
                    ReportBuffer.add(this, message);

                }
                // Note: old shares are removed when company is closed

                if (firstCGRowner == null)
                    firstCGRowner = player;

                // Check for presidency
                if (newShares > maxShares) {
                    maxShares = newShares;
                    newPresident = player;
                }
            }
        }

        // Exchange the pool shares
        int oldShares = 0, newShares = 0;
        List<PublicCertificate> certs = Lists.newArrayList();
        for (PublicCertificate cert : pool.getCertificates()) {
            if (mergingCompanies.contains(cert.getCompany())) {
                certs.add((cert));
                oldShares++;
            }
        }
        int count = oldShares;
        while (count >= 2 && cgrSharesUsed <= 19) {
            PublicCertificate cgrCert = unavailable.findCertificate(cgr, false);
            cgrCert.moveTo(pool.getParent());
            count -= 2;
            cgrSharesUsed++;
            newShares++;
        }

        String message = LocalText.getText("SharesReplacedForShares",
                LocalText.getText("POOL"),
                oldShares,
                newShares,
                PublicCompany_CGR.NAME);
        DisplayBuffer.add(this, message);
        ReportBuffer.add(this, message);

        Portfolio.moveAll(certs, scrapHeap.getParent());
        log.info("{} CGR shares are now in play", cgrSharesUsed);

        // If no more than 10 shares are in play, the CGR share
        // unit becomes 10%; otherwise it stays 5%.
        if (cgrSharesUsed <= 10) {
            cgr.setShareUnit(10);
            // All superfluous shares have been removed
        }
        message = LocalText.getText("CompanyHasShares",
                cgr.toText(), 100 / cgr.getShareUnit(), cgr.getShareUnit());
        DisplayBuffer.add(this, " ");
        ReportBuffer.add(this, " ");
        DisplayBuffer.add(this, message);
        ReportBuffer.add(this, message);

        // Move the remaining CGR shares to the ipo.
        Portfolio.moveAll(unavailable.getCertificates(cgr), ipo.getParent());

        // Assign the new president
        if (newPresident.getPortfolioModel().getShare(cgr) == cgr.getShareUnit()) {
            // Nobody has 2 shares, then takes the first player who has got one share
            log.debug("Nobody has two shares, creating a temp.pres.: {}", firstCGRowner.getId());
            cgr.setTemporaryPresident(firstCGRowner);
            newPresident = firstCGRowner;
        } else if (temporaryPresident != null && temporaryPresident != newPresident) {
            log.debug("Moving pres.share from {} to {}", temporaryPresident.getId(), newPresident.getId());
            temporaryPresident.getPortfolioModel().swapPresidentCertificate(cgr,
                    newPresident.getPortfolioModel(), 1);
        }

// Explicitly set the active round player to the CGR President so subsequent discard/token steps validate correctly
        playerManager.setCurrentPlayer(newPresident);


        message = LocalText.getText("IS_NOW_PRES_OF",
                newPresident.getId(), cgr.toText());
        ReportBuffer.add(this, message);
        DisplayBuffer.add(this, message);

        // Determine the CGR starting price,
        // and close the absorbed companies.
        int lowestPrice = 999;
        int totalPrice = 0;
        int price;
        int numberMerged = mergingCompanies.size();
        for (PublicCompany comp : mergingCompanies) {
            price = comp.getMarketPrice();
            totalPrice += price;
            if (price < lowestPrice)
                lowestPrice = price;
        }
        if (numberMerged >= 3) {
            totalPrice -= lowestPrice;
            numberMerged--;
        }
        int cgrPrice = Math.max(100, (((totalPrice / numberMerged) / 5)) * 5);

        // Find the correct start space and start the CGR
        if (cgrPrice == 100) {
            cgr.start(100);
        } else {
            int prevColPrice = 100;
            int colPrice;
            StockSpace startSpace;
            for (int col = 6; col <= stockMarket.getNumberOfColumns(); col++) {
                colPrice = stockMarket.getStockSpace(0, col).getPrice();
                if (cgrPrice > colPrice)
                    continue;
                if (cgrPrice - prevColPrice < colPrice - cgrPrice) {
                    startSpace = stockMarket.getStockSpace(0, col - 1);
                } else {
                    startSpace = stockMarket.getStockSpace(0, col);
                }
                cgr.start(startSpace);
                message = LocalText.getText("START_MERGED_COMPANY",
                        PublicCompany_CGR.NAME,
                        Bank.format(this, startSpace.getPrice()),
                        startSpace.getId());
                DisplayBuffer.add(this, message);
                ReportBuffer.add(this, message);
                break;
            }
        }
        cgr.setFloated();
        ReportBuffer.add(this, LocalText.getText("Floats", PublicCompany_CGR.NAME));

        // Collect the old token spots, and move cash and trains
        List<BaseToken> homeTokens = new ArrayList<BaseToken>();
        List<BaseToken> nonHomeTokens = new ArrayList<BaseToken>();
        BaseToken bt;
        MapHex hex;
        Stop stop;
        for (PublicCompany comp : mergingCompanies) {

            // Exchange home tokens and collect non-home tokens
            List<MapHex> homeHexes = comp.getHomeHexes();
            for (BaseToken token : comp.getAllBaseTokens()) {
                bt = token;
                if (!bt.isPlaced())
                    continue;
                stop = (Stop) bt.getOwner();
                hex = stop.getParent();
                if (homeHexes != null && homeHexes.contains(hex)) {
                    homeTokens.add(bt);
                } else {
                    nonHomeTokens.add(bt);
                }
            }

            // Move any remaining cash
            if (comp.getCash() > 0) {
                Currency.wireAll(comp, cgr);
            }

            // Move any remaining trains
            Set<Train> trains = comp.getPortfolioModel().getTrainList();
            for (Train train : trains) {
                cgr.getPortfolioModel().addTrain(train);
                if (train.isPermanent())
                    cgr.setHadPermanentTrain(true);
            }

            // Move any still valid bonuses
            if (comp.getBonuses() != null) {
                List<Bonus> bonuses = new ArrayList<Bonus>(comp.getBonuses());
                bonuses: for (Bonus bonus : bonuses) {
                    comp.removeBonus(bonus);
                    // Only add if the CGR does not already have the same bonus
                    if (cgr.getBonuses() != null) {
                        for (Bonus b : cgr.getBonuses()) {
                            if (b.getLocations().equals(bonus.getLocations())) {
                                // if (b.equals(bonus)) { //String Mismatch due too different special property
                                // names..
                                // Remove this duplicate bonus token.
                                // Check if it should be made available again.
                                List<SellBonusToken> commonSP = gameManager.getSpecialProperties(SellBonusToken.class,
                                        true);
                                if (commonSP != null) {
                                    for (SellBonusToken sp : commonSP) {
                                        if (sp.getId().equalsIgnoreCase(b.getName())) {
                                            sp.makeResellable();
                                            log.debug("BonusToken {} made sellable again", b.getName());
                                            break;
                                        }
                                    }
                                }
                                log.debug("Duplicate BonusToken {} not added to {}", b.getName(), cgr.getId());
                                continue bonuses;
                            }
                        }
                    }
                    cgr.addBonus(new Bonus(cgr, bonus.getName(), bonus.getValue(), bonus.getLocations()));
                }
            }
        }

        // Replace the home tokens
        ReportBuffer.add(this, "");
        for (BaseToken token : homeTokens) {
            stop = (Stop) token.getOwner();
            hex = stop.getParent();
            // return token to home
            token.moveTo(token.getParent());
            if (hex.layBaseToken(cgr, stop)) {
                /* TODO: the false return value must be impossible. */
                ReportBuffer.add(this, LocalText.getText("ExchangesBaseToken",
                        cgr.toText(), token.getParent().getId(),
                        stop.getStationComposedId()));
                cgr.layBaseToken(hex, 0);
            }
        }

        // Clean up any non-home tokens on cities now having a CGR token
        for (BaseToken token : new ArrayList<>(nonHomeTokens)) {
            stop = (Stop) token.getOwner();
            hex = stop.getParent();
            Set<BaseToken> otherTokens = hex.getBaseTokens();
            if (otherTokens != null) {
                for (BaseToken token2 : otherTokens) {
                    if (token2.getParent() == cgr
                            || nonHomeTokens.contains(token2) && token2 != token) {
                        ReportBuffer.add(this, LocalText.getText("DiscardsBaseToken",
                                cgr.toText(), token.getParent().getId(),
                                stop.getStationComposedId()));
                        // return token to home
                        token.moveTo(token.getParent());
                        nonHomeTokens.remove(token);
                        break;
                    }
                }
            }
        }

        // Prepare replacing the other tokens, if possible
        if (homeTokens.size() + nonHomeTokens.size() > cgr.getNumberOfBaseTokens()) {
            // CGR cannot replace all tokens, must choose
            // First collect old names per city
            Map<String, String> oldTokens = new HashMap<>();
            String cityName;
            for (BaseToken token : nonHomeTokens) {
                if (token.getOwner() instanceof Stop) {
                    cityName = ((Stop) token.getOwner()).getStationComposedId();
                    if (oldTokens.containsKey(cityName)) {
                        oldTokens.put(cityName,
                                oldTokens.get(cityName) + "," + token.getParent().getId());
                    } else {
                        oldTokens.put(cityName, token.getParent().getId());
                    }
                }
            }
            // Then create list of exchange spots. Sort it on hexname/city number
            tokensToExchangeFrom = new ArrayList<>();
            for (String key : new TreeSet<>(oldTokens.keySet())) {
                tokensToExchangeFrom.add(new ExchangeableToken(
                        key, oldTokens.get(key)));
            }
        } else {
            executeExchangeTokens(nonHomeTokens);
        }

        // Close the merged companies
        for (PublicCompany comp : mergingCompanies) {
            comp.setClosed();
        }

        // Check the trains, autodiscard any excess non-permanent trains
        // int trainLimit = cgr.getTrainLimit(gameManager.getCurrentPlayerIndex());
        int trainLimit = cgr.getCurrentTrainLimit();
        Set<Train> trains = cgr.getPortfolioModel().getTrainList();
        if (cgr.getNumberOfTrains() > trainLimit) {
            ReportBuffer.add(this, "");
            int numberToDiscard = cgr.getNumberOfTrains() - trainLimit;
            List<Train> trainsToDiscard = new ArrayList<Train>(4);
            for (Train train : trains) {
                if (!train.isPermanent()) {
                    trainsToDiscard.add(train);
                    if (--numberToDiscard == 0)
                        break;
                }
            }
            for (Train train : trainsToDiscard) {
                train.discard();
            }
        }

    }

    private void executeExchangeTokens(List<BaseToken> exchangedTokens) {
        Stop stop;
        MapHex hex;
        ReportBuffer.add(this, "");
        for (BaseToken token : exchangedTokens) {
            // Remove old token
            stop = (Stop) token.getOwner();
            hex = stop.getParent();
            // return token to Company
            token.moveTo(token.getParent());
            // Replace it with a CGR token
            if (hex.layBaseToken(cgr, stop)) {
                cgr.layBaseToken(hex, 0);
            } else {
                log.error("Error in laying CGR token on {} {}", hex.getId(), hex.getStopName());
            }
        }
    }

    private boolean checkForTrainsToDiscard() {

        // Check if CGR must discard trains
        if (cgr.getNumberOfTrains() > cgr.getCurrentTrainLimit()) {
            log.debug("CGR must discard trains");
            trainsToDiscardFrom = cgr.getPortfolioModel().getTrainList();
            forcedTrainDiscard = true;

            return true;
        } else if (!this.cgrHasDiscardedTrains.value()) {
            // Check if CGR still has non-permanent trains
            // these may be discarded voluntarily
            trainsToDiscardFrom = new HashSet<Train>();
            for (Train train : cgr.getPortfolioModel().getTrainList()) {
                if (!train.isPermanent()) {
                    trainsToDiscardFrom.add(train);
                }
            }
            if (!trainsToDiscardFrom.isEmpty()) {

                // Add a default done option to the actions register so the UI finished turn
                // validates successfully
                possibleActions.add(new NullAction(getRoot(), NullAction.Mode.DONE));
                forcedTrainDiscard = false;
                return true;
            }
        }
        return false;
    }

    /**
     * Expose the operating company to the UI Manager so the ORPanel
     * correctly displays the treasury and assets of the company currently paying
     * loans.
     */
    public PublicCompany getOperatingCompany() {
        PublicCompany comp = currentCompany.value();
        if (step.value() == Steps.STEP_REPAY_LOANS && comp != null) {
            return comp;
        } else {
            return cgr;
        }
    }

    public boolean discardTrain(DiscardTrain action) {

        Train train = action.getDiscardedTrain();
        PublicCompany company = action.getCompany();
        String companyName = company.getId();

        String errMsg = null;

        // Dummy loop to enable a quick jump out.
        while (true) {
            // Checks
            // Must be CGR
            if (company != cgr) {
                errMsg = LocalText.getText("WrongCompany",
                        company.getId(),
                        cgr.toText());
                break;
            }
            // Must be correct step
            if (step.value() != Steps.STEP_DISCARD_TRAINS) {
                errMsg = LocalText.getText("WrongActionNoDiscardTrain");
                break;
            }

            // Does the company own such a train?

            if (train != null && !company.getPortfolioModel().getTrainList().contains(train)) {
                errMsg = LocalText.getText("CompanyDoesNotOwnTrain",
                        company.getId(),
                        train.toText());
                break;
            }

            break;
        }
        if (errMsg != null) {
            DisplayBuffer.add(this, LocalText.getText("CannotDiscardTrain",
                    companyName,
                    train.toText(),
                    errMsg));
            return false;
        }

        /* End of validation, start of execution */
        // new: link always, see below commented

        if (train != null) {
            train.discard();
        } else {
            cgrHasDiscardedTrains.set(true);
        }

        return true;
    }

    protected boolean exchangeTokens(ExchangeTokens action, boolean linkedMoveSet) {

        String errMsg = null;

        List<ExchangeableToken> tokens = action.getTokensToExchange();
        int min = action.getMinNumberToExchange();
        int max = action.getMaxNumberToExchange();
        int exchanged = 0;

        checks: {

            for (ExchangeableToken token : tokens) {
                if (token.isSelected())
                    exchanged++;
            }
            if (exchanged < min || exchanged > max) {
                errMsg = LocalText.getText("WrongNumberOfTokensExchanged",
                        action.getCompany(),
                        min, max, exchanged);
                break checks;
            }
        }

        if (errMsg != null) {
            DisplayBuffer.add(this, LocalText.getText("CannotExchangeTokens",
                    action.getCompany(),
                    action.toString(),
                    errMsg));

            return false;
        }

        // FIMXE: if (linkedMoveSet) changeStack.linkToPreviousMoveSet();

        if (exchanged > 0) {
            MapHex hex;
            Stop stop;
            String stopName, hexName;
            int stationNumber;
            String[] ct;
            PublicCompany comp = action.getCompany();

            ReportBuffer.add(this, "");

            for (ExchangeableToken token : tokens) {
                stopName = token.getCityName();
                ct = stopName.split("/");
                hexName = ct[0];
                try {
                    stationNumber = Integer.parseInt(ct[1]);
                } catch (NumberFormatException e) {
                    stationNumber = 1;
                }
                hex = mapManager.getHex(hexName);
                stop = hex.getRelatedStop(stationNumber);

                if (token.isSelected()) {

                    // For now we'll assume that the old token(s) have already been removed.
                    // This is true in the 1856 CGR formation.
                    if (hex.layBaseToken(comp, stop)) {
                        /* TODO: the false return value must be impossible. */
                        ReportBuffer.add(this, LocalText.getText("ExchangesBaseToken",
                                comp.getId(),
                                token.getOldCompanyName(),
                                stop.getStationComposedId()));
                        comp.layBaseToken(hex, 0);
                    }
                } else {
                    ReportBuffer.add(this, LocalText.getText("NoBaseTokenExchange",
                            comp.getId(),
                            token.getOldCompanyName(),
                            stop.getStationComposedId()));
                }
            }
        }

        return true;
    }

    public List<PublicCompany> getMergingCompanies() {
        return mergingCompanies.view();
    }

    @Override
    public String toString() {
        return "1856 CGRFormationRound";
    }

    @Override
    protected void finishRound() {

        super.finishRound();

        // In any case we must recalculate the certificate limit
        ((GameManager_1856) gameManager).resetCertificateLimit(true);
    }

    // Step Objects to control progress
    private enum Steps {
        STEP_REPAY_LOANS, STEP_DISCARD_TRAINS, STEP_EXCHANGE_TOKENS
    }

    public boolean repayLoans(RepayLoans action) {
        log.info("[CGR_DIAG] --- repayLoans() Executing Action ---");
        if (action == null) {
            log.info("[CGR_DIAG] ERROR: Action is null!");
            return false;
        }

        PublicCompany company = action.getCompany();
        log.info("[CGR_DIAG] Action received for Company={}, maxNumber={}", company.getId(), action.getMaxNumber());

        int numberRepaid = action.getMaxNumber();
        
        
       int initialLoans = company.getCurrentNumberOfLoans();
        int repayment = numberRepaid * company.getValuePerLoan();
        int repaymentByPresident = 0;

        if (repayment > 0) {
            int repaymentByCompany = Math.min(repayment, company.getCash());
            repaymentByPresident = repayment - repaymentByCompany;

            company.addLoans(-numberRepaid);
            if (repaymentByCompany > 0) {
                String repayCompanyText = Currency.toBank(company, repaymentByCompany);
                ReportBuffer.add(this, LocalText.getText("CompanyRepaysLoans",
                        company.getId(),
                        repayCompanyText,
                        numberRepaid,
                        Bank.format(this, company.getValuePerLoan())));
            }
            if (repaymentByPresident > 0) {
                Player president = company.getPresident();
                String repayPresidentText = Currency.toBank(president, repaymentByPresident);
                ReportBuffer.add(this, LocalText.getText("CompanyRepaysLoansWithPresCash",
                        company.getId(),
                        repayPresidentText,
                        Bank.format(this, repayment),
                        numberRepaid,
                        Bank.format(this, company.getValuePerLoan()),
                        president.getId()));
            }
        }

        // A company merges if it fails to repay ALL of its initial loans.
        if (numberRepaid < initialLoans) {
            log.info("[CGR_DIAG] Company {} failed to repay all loans (Initial: {}, Repaid: {}). Flagging for merger.", company.getId(), initialLoans, numberRepaid);
            if (!mergingCompanies.contains(company)) {
                mergingCompanies.add(company);
            }
            String message = LocalText.getText("WillMergeInto",
                    company.getId(),
                    PublicCompany_CGR.NAME);
            ReportBuffer.add(this, message);
        }

        log.info("[CGR_DIAG] Clearing currentCompany state.");
        currentCompany.set(null);

        // 1. Process game logic transitions synchronously so the engine stays in sync
        log.info("[CGR_DIAG] Advancing to next company intervention or forming CGR...");
        if (!setNextCompanyNeedingPresidentIntervention()) {
            log.info("[CGR_DIAG] No more companies need intervention. Forming CGR.");
            if (mergingCompanies.isEmpty()) {
                finishRound();
            } else {
                formCGR();
                step.set(Steps.STEP_EXCHANGE_TOKENS);
                // Force an immediate layout action calculation to cascade step transitions
                setPossibleActions();
            }
        }

        // 2. Safely request the UI repaint on the Event Dispatch Thread separately
        if (gameManager != null && gameManager.getGameUIManager() != null
                && gameManager.getGameUIManager().getStatusWindow() != null) {
            javax.swing.SwingUtilities.invokeLater(() -> {
                try {
                    log.info("[CGR_DIAG] EDT: Refreshing Status Window graphics.");
                    Object status = gameManager.getGameUIManager().getStatusWindow().getGameStatus();
                    if (status instanceof java.util.Observer) {
                        ((java.util.Observer) status).update(null, "ForceUpdate");
                    }
                } catch (Exception e) {
                    log.warn("UI Status Frame sync failed on EDT", e);
                }
            });
        }

        return true;
        // --- END FIX ---
    }

    @Override
    public boolean setPossibleActions() {
        if (step.value() == Steps.STEP_REPAY_LOANS) {
            PublicCompany comp = currentCompany.value();

            // If the company has been cleared, do not pull the next company instantly.
            // Yield execution here so the status window successfully renders the
            // empty/folded state.
            if (comp == null) {
                log.info("[CGR_DIAG] setPossibleActions: currentCompany is null. Advancing formation round state.");
                possibleActions.clear();
                if (!setNextCompanyNeedingPresidentIntervention()) {
                    if (mergingCompanies.isEmpty()) {
                        finishRound();
                    } else {
                        formCGR();
                        step.set(Steps.STEP_EXCHANGE_TOKENS);
                        setPossibleActions();
                    }
                }
                // Catch-all safety guard: If buttons are empty after transitioning, 
                // supply a valid Done action to unlock the UI button completion path.
                if (possibleActions.isEmpty()) {
                    possibleActions.add(new NullAction(getRoot(), NullAction.Mode.DONE));
                }

if (gameManager != null && gameManager.getGameUIManager() != null) {
            javax.swing.SwingUtilities.invokeLater(() -> {
                try {
                    log.info("[CGR_DIAG] EDT: Refreshing all UI components and Status Window graphics.");
                    // Call updateUI to handle the full visual rehydration across panels
                    gameManager.getGameUIManager().updateUI();
                    
                    if (gameManager.getGameUIManager().getStatusWindow() != null) {
                        Object status = gameManager.getGameUIManager().getStatusWindow().getGameStatus();
                        if (status instanceof java.util.Observer) {
                            ((java.util.Observer) status).update(null, "ForceUpdate");
                        }
                    }
                } catch (Exception e) {
                    log.warn("UI Status Frame sync failed on EDT", e);
                }
            });
        }
                return true;
            }

            log.info("[CGR_DIAG] setPossibleActions establishing buttons for company {}", comp.getId());
            possibleActions.clear();

            int loans = comp.getCurrentNumberOfLoans();
            int val = comp.getValuePerLoan();
            int treasuryAffords = comp.getCash() / val;
            int totalAffords = (comp.getCash() + comp.getPresident().getCash()) / val;
            int treasuryRepay = Math.min(loans, treasuryAffords);

            if (totalAffords < loans) {
                RepayLoans mergeAction = new RepayLoans(comp, treasuryRepay, treasuryRepay, val);
                mergeAction.setCustomLabel("OK - Must Merge into CGR");
                possibleActions.add(mergeAction);
            } else if (treasuryAffords >= loans) {
                RepayLoans safeAction = new RepayLoans(comp, loans, loans, val);
                safeAction.setCustomLabel("Repay All from Treasury (" + loans + " loans)");
                possibleActions.add(safeAction);
            } else {
                RepayLoans payAction = new RepayLoans(comp, loans, loans, val);
                int presNeeded = (loans - treasuryRepay) * val;
                payAction.setCustomLabel("Repay All (Needs $" + presNeeded + " President Cash)");
                possibleActions.add(payAction);

                RepayLoans refuseAction = new RepayLoans(comp, treasuryRepay, treasuryRepay, val);
                refuseAction.setCustomLabel("Refuse - Merge into CGR");
                possibleActions.add(refuseAction);
            }
            guiHints.setActivePanel(GuiDef.Panel.STATUS);
            log.info("[CGR_DIAG] Generated {} action buttons.", possibleActions.size());
        } else if (step.value() == Steps.STEP_EXCHANGE_TOKENS) {
            if (tokensToExchangeFrom == null || tokensToExchangeFrom.isEmpty()) {
                step.set(Steps.STEP_DISCARD_TRAINS);
                return setPossibleActions();
            }
        } else if (step.value() == Steps.STEP_DISCARD_TRAINS) {
            if (!checkForTrainsToDiscard()) {
                finishRound();
            }
            else {
                // Ensure the status panel becomes active so the engine binds the controls 
                // to the newly assigned active CGR President player
                guiHints.setActivePanel(GuiDef.Panel.STATUS);
            }
            return true;
        }
        return true;
    }

    @Override
    public boolean process(PossibleAction action) {
        log.info("[CGR_DIAG] --- process() intercepting action: {} ---",
                (action != null ? action.getClass().getSimpleName() : "null"));
        if (action instanceof RepayLoans) {
            return repayLoans((RepayLoans) action);
        }
        return super.process(action);
    }

    public void start(Player startingPlayer) {
        log.info("[CGR_DIAG] --- start() called ---");
        // store starting player
        this.startingPlayer = startingPlayer;

        ReportBuffer.add(this, LocalText.getText("StartFormationRound",
                PublicCompany_CGR.NAME));
        ReportBuffer.add(this, LocalText.getText("StartingPlayer",
                startingPlayer.getId()));

        guiHints.setCurrentRoundType(getClass());

        // Collect companies having loans
        for (PublicCompany company : setOperatingCompanies()) {
            if (company.getCurrentNumberOfLoans() > 0) {
                log.info("[CGR_DIAG] Queuing {} for president {}", company.getId(), company.getPresident().getId());
                companiesToRepayLoans.put(company.getPresident(), company);
            }
        }

        if (companiesToRepayLoans.isEmpty()) {
            ReportBuffer.add(this, LocalText.getText("DoesNotForm", cgr.toText()));
            finishRound();
            return;
        }

        step.set(Steps.STEP_REPAY_LOANS);
        playerManager.setCurrentPlayer(startingPlayer);

        // Explicitly set the first company to prevent UI NullPointerExceptions on
        // reload
        if (!setNextCompanyNeedingPresidentIntervention()) {
            if (mergingCompanies.isEmpty()) {
                finishRound();
            } else {
                formCGR();
                step.set(Steps.STEP_EXCHANGE_TOKENS);
            }
        }

        process(null);
    }

    private boolean setNextCompanyNeedingPresidentIntervention() {
        log.info("[CGR_DIAG] --- setNextCompanyNeedingPresidentIntervention() ---");

        if (!companiesToRepayLoans.containsKey(playerManager.getCurrentPlayer())) {
            playerManager.setCurrentToNextPlayer();
            if (playerManager.getCurrentPlayer().equals(startingPlayer)) {
                log.info("[CGR_DIAG] Wrapped around to starting player. No more companies.");
                return false;
            }
            return setNextCompanyNeedingPresidentIntervention(); // Recursive wrap check
        }

        // select player and company to act
        Player player = playerManager.getCurrentPlayer();
        java.util.List<PublicCompany> comps = companiesToRepayLoans.get(player);

        if (comps == null || comps.isEmpty()) {
            playerManager.setCurrentToNextPlayer();
            if (playerManager.getCurrentPlayer().equals(startingPlayer)) {
                return false;
            }
            return setNextCompanyNeedingPresidentIntervention();
        }

        PublicCompany company = comps.get(0);
        log.info("[CGR_DIAG] Selected company {}, popping from queue.", company.getId());
        companiesToRepayLoans.remove(player, company);
        currentCompany.set(company);

        int numberOfLoans = company.getCurrentNumberOfLoans();
        if (numberOfLoans == 0) {
            log.info("[CGR_DIAG] Company {} has 0 loans, skipping.", company.getId());
            return setNextCompanyNeedingPresidentIntervention();
        }

        log.info("[CGR_DIAG] Yielding UI control for company {}", company.getId());
        return true;
    }

}
