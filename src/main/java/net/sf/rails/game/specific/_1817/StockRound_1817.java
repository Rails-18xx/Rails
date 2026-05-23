package net.sf.rails.game.specific._1817;

import java.util.ArrayList;
import java.util.List;

import net.sf.rails.game.Company;
import net.sf.rails.game.GameManager;
import net.sf.rails.game.PublicCompany;
import rails.game.action.PossibleAction;
import rails.game.action.StartCompany;
import net.sf.rails.game.financial.StockRound;
import net.sf.rails.game.specific._1817.action.Initiate1817IPO;
import net.sf.rails.game.specific._1817.action.Short1817;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import net.sf.rails.game.specific._1817.action.TakeLoans_1817;
import net.sf.rails.game.state.BooleanState;
import net.sf.rails.game.state.GenericState;
import net.sf.rails.game.state.IntegerState;
import net.sf.rails.game.specific._1817.action.LayNYHomeToken;
import net.sf.rails.game.MapHex;
import net.sf.rails.game.Stop;
// --- END FIX ---

/**
 * 1817 specific Stock Round logic.
 * Triggers certificate adjustment on start and handles the IPO auction
 * shortcut.
 */
public class StockRound_1817 extends StockRound {

    private static final Logger log = LoggerFactory.getLogger(StockRound_1817.class);

    protected final BooleanState waitingForE22Start;
    protected final GenericState<PublicCompany> pendingE22Company;
    protected final IntegerState pendingE22Bid;

    // Track companies shorted by players during the current stock round turn.
    // In Rails, we map Player -> List of shorted company IDs.
    protected final net.sf.rails.game.state.HashMapState<String, String> shortedThisRound;

    protected final net.sf.rails.game.state.ArrayListState<String> ipoedThisRound;

    // State variables to enforce strict 1817 stock round sequence
    protected final net.sf.rails.game.state.BooleanState playerHasSold;
    protected final net.sf.rails.game.state.BooleanState playerHasShorted;
    protected final net.sf.rails.game.state.BooleanState playerHasBoughtOrIPOed;

    // State variables to enforce mutual exclusivity and company action sequence
    protected final net.sf.rails.game.state.StringState activeCompanyId;
    protected final net.sf.rails.game.state.BooleanState companyHasTakenLoan;
    protected final net.sf.rails.game.state.BooleanState companyHasBoughtShare;
    protected final net.sf.rails.game.state.IntegerState auctionCounter;

    public StockRound_1817(GameManager parent, String id) {
        super(parent, id);
        waitingForE22Start = new BooleanState(this, "waitingForE22Start_" + id, false);
        pendingE22Company = new GenericState<>(this, "pendingE22Company_" + id);
        pendingE22Bid = IntegerState.create(this, "pendingE22Bid_" + id, 0);
        shortedThisRound = net.sf.rails.game.state.HashMapState.create(this, "shortedThisRound_" + id);
        ipoedThisRound = new net.sf.rails.game.state.ArrayListState<>(this, "ipoedThisRound_" + id);

        playerHasSold = new net.sf.rails.game.state.BooleanState(this, "playerHasSold_" + id, false);
        playerHasShorted = new net.sf.rails.game.state.BooleanState(this, "playerHasShorted_" + id, false);
        playerHasBoughtOrIPOed = new net.sf.rails.game.state.BooleanState(this, "playerHasBoughtOrIPOed_" + id, false);

        activeCompanyId = net.sf.rails.game.state.StringState.create(this, "activeCompanyId_" + id, "");
        companyHasTakenLoan = new net.sf.rails.game.state.BooleanState(this, "companyHasTakenLoan_" + id, false);
        companyHasBoughtShare = new net.sf.rails.game.state.BooleanState(this, "companyHasBoughtShare_" + id, false);
auctionCounter = net.sf.rails.game.state.IntegerState.create(this, "auctionCounter_" + id, 0);

    }

    @Override
    public void start() {
        super.start();
        shortedThisRound.clear();
        resetPlayerTurnStates();
        // Disable base class sold-out logic to prevent double upward movement in 1817
        raiseIfSoldOut = false;

        ipoedThisRound.clear();
        // Initial certificate setup for all companies
        if (gameManager.getSRNumber() == 1) {
            for (PublicCompany comp : gameManager.getAllPublicCompanies()) {
                if (comp.getPresident() == null && !comp.isClosed()) {
                    ((PublicCompany_1817) comp).adjustCertificates();
                }
            }
        }

        // 1817 Rule 7.4: Bank Market Clearing
        // At the start of every stock round, the bank closes any short positions
        // in the open market by purchasing stock from the company treasury.
        net.sf.rails.game.model.PortfolioModel openMarket = pool;

        net.sf.rails.game.financial.BankPortfolio osiBank = getRoot().getBank().getOSI();

        for (PublicCompany comp : gameManager.getAllPublicCompanies()) {
            if (!(comp instanceof PublicCompany_1817))
                continue;

            boolean keepClearing = true;
            while (keepClearing) {
                net.sf.rails.game.financial.PublicCertificate shortCert = null;
                for (net.sf.rails.game.financial.PublicCertificate c : openMarket.getCertificates()) {
                    if (c instanceof net.sf.rails.game.specific._1817.ShortCertificate && c.getCompany() == comp) {
                        shortCert = c;
                        break;
                    }
                }

                if (shortCert != null) {
                    // Search treasury for a non-president share to close the short
                    net.sf.rails.game.financial.PublicCertificate treasuryShare = comp.getPortfolioModel()
                            .findCertificate(comp, false);
                    if (treasuryShare != null) {
                        net.sf.rails.game.financial.BankPortfolio unavailableBank = getRoot().getBank()
                                .getUnavailable();
                        shortCert.moveTo(osiBank);
                        treasuryShare.moveTo(unavailableBank);
                        log.info(
                                "Bank auto-closed orphaned short share for " + comp.getId() + " using treasury stock.");
                    } else {
                        keepClearing = false;
                    }
                } else {
                    keepClearing = false;
                }
            }
        }

    }


    @Override
    public void setBuyableCerts() {
        if (waitingForE22Start.value()) {
            possibleActions.clear();
            MapHex nyHex = getRoot().getMapManager().getHex("E22");
            PublicCompany comp = pendingE22Company.value();
            for (Stop stop : nyHex.getStops()) {
                if (stop.hasTokenSlotsLeft()) {
                    possibleActions.add(new LayNYHomeToken(nyHex, comp, stop));
                }
            }
            return;
        }
        
        super.setBuyableCerts();

        if (possibleActions == null)
            return;

        net.sf.rails.game.Player currentPlayer = gameManager.getCurrentPlayer();
        if (currentPlayer == null) return;

// --- START FIX ---
        String activeComp = activeCompanyId.value();
        boolean actingForSelf = (activeComp == null || activeComp.isEmpty());
        boolean actingForCompany = !actingForSelf;
        boolean noPersonalActions = !playerHasSold.value() && !playerHasShorted.value() && !playerHasBoughtOrIPOed.value();

        // 1. Filter default Rails actions based on 1817 state
        List<PossibleAction> actionsToRemove = new ArrayList<>();
        for (PossibleAction action : possibleActions.getList()) {
            if (action instanceof StartCompany) {
                // We completely replace standard StartCompany with Initiate1817IPO
                actionsToRemove.add(action);
            } else if (action instanceof rails.game.action.BuyCertificate) {
                // Remove buys if player acted for a company, OR if already bought/IPOed
                if (actingForCompany || playerHasBoughtOrIPOed.value()) {
                    actionsToRemove.add(action);
                } else {
                    // Rule 5.3: Filter out BuyCertificate actions for companies shorted this round
                    rails.game.action.BuyCertificate buyAction = (rails.game.action.BuyCertificate) action;
                    String key = currentPlayer.getId() + "_" + buyAction.getCompany().getId();
                    if (shortedThisRound.containsKey(key)) {
                        actionsToRemove.add(action);
                    }
                }
            }
        }
        for (PossibleAction action : actionsToRemove) {
            possibleActions.remove(action);
        }

        // 2. COMPANY ACTIONS
        if (noPersonalActions) {
            for (PublicCompany comp : gameManager.getAllPublicCompanies()) {
                if (actingForCompany && !comp.getId().equals(activeComp)) continue;

                if (comp.getPresident() == currentPlayer && !comp.isClosed()) {
                    // Action 1: Take Loans (must precede buying open market stock)
                    if (!companyHasBoughtShare.value()) {
                        int maxLoans = (comp instanceof PublicCompany_1817) ? ((PublicCompany_1817) comp).getShareCount() : 0;
                        if (comp.getNumberOfBonds() < maxLoans) {
                            possibleActions.add(new TakeLoans_1817(getRoot(), comp.getId()));
                        }
                    }

                    // Action 2: Company Buys Open Market Share
                    if (comp.isBuyable()) {
                        int price = comp.getMarketPrice();
                        if (comp.getCash() >= price) {
                            net.sf.rails.game.financial.PublicCertificate poolCert = pool.findCertificate(comp, 1, false);
                            if (poolCert != null) {
                                possibleActions.add(new net.sf.rails.game.specific._1817.action.CompanyBuyOpenMarketShare_1817(
                                        gameManager.getRoot(), comp.getId(), price));
                            }
                        }
                    }
                }
            }
        }

        // 3. PLAYER ACTIONS (Short & IPO)
        if (actingForSelf) {
            // Short Action (Must not have Bought/IPOed, max 1 per turn)
            if (!playerHasBoughtOrIPOed.value() && !playerHasShorted.value()) {
                for (PublicCompany comp : gameManager.getAllPublicCompanies()) {
                    if (comp instanceof PublicCompany_1817 && !comp.isClosed()) {
                        PublicCompany_1817 comp1817 = (PublicCompany_1817) comp;
                        boolean isLargeEnough = comp1817.getShareCount() > 2;

                        int regularSharesOwned = 0;
                        for (net.sf.rails.game.financial.PublicCertificate c : currentPlayer.getPortfolioModel().getCertificates()) {
                            if (!(c instanceof net.sf.rails.game.specific._1817.ShortCertificate) && c.getCompany() == comp) {
                                regularSharesOwned++;
                            }
                        }
                        boolean ownsZeroShares = (regularSharesOwned == 0);

                        int activeShorts = 0;
                        int availableShorts = 0;
                        net.sf.rails.game.model.PortfolioModel osiPortfolio = getRoot().getBank().getOSI().getPortfolioModel();
                        for (net.sf.rails.game.financial.PublicCertificate c : osiPortfolio.getCertificates()) {
                            if (c instanceof net.sf.rails.game.specific._1817.ShortCertificate && c.getCompany() == comp1817) availableShorts++;
                        }
                        net.sf.rails.game.model.PortfolioModel unavailablePortfolio = getRoot().getBank().getUnavailable().getPortfolioModel();
                        for (net.sf.rails.game.financial.PublicCertificate c : unavailablePortfolio.getCertificates()) {
                            if (c instanceof net.sf.rails.game.specific._1817.ShortCertificate && c.getCompany() == comp1817) availableShorts++;
                        }
                        for (net.sf.rails.game.Player p : getRoot().getPlayerManager().getPlayers()) {
                            for (net.sf.rails.game.financial.PublicCertificate c : p.getPortfolioModel().getCertificates()) {
                                if (c instanceof net.sf.rails.game.specific._1817.ShortCertificate && c.getCompany() == comp1817) activeShorts++;
                            }
                        }

                        boolean underShortLimit = (activeShorts < 5 && availableShorts > 0);
                        boolean notInAcquisitionZone = comp.isBuyable();
                        String phaseId = (gameManager.getCurrentPhase() != null) ? gameManager.getCurrentPhase().getId() : "";
                        boolean notPhase8 = (phaseId == null || !phaseId.startsWith("8"));
                        boolean notIpoedThisRound = !ipoedThisRound.contains(comp.getId());

                        if (isLargeEnough && ownsZeroShares && underShortLimit && notInAcquisitionZone && notPhase8 && notIpoedThisRound) {
                            possibleActions.add(new net.sf.rails.game.specific._1817.action.Short1817(gameManager.getRoot(), comp.getId()));
                        }
                    }
                }
            }

            // IPO Action (Replaces standard Buy, cannot happen if already bought/IPOed)
            if (!playerHasBoughtOrIPOed.value()) {

                // Rule 5.5: Purchasing power = cash + face value of privates [cite: 516, 518]
                int purchasingPower = currentPlayer.getCash();
                for (net.sf.rails.game.PrivateCompany pc : currentPlayer.getPortfolioModel().getPrivateCompanies()) {
                    purchasingPower += pc.getBasePrice(); // face value [cite: 517]
                }

                // Minimum starting bid for an IPO is $100 
                if (purchasingPower >= 100) {

                    for (PublicCompany comp : gameManager.getAllPublicCompanies()) {
                        if (!comp.hasFloated() && !comp.isClosed()) {
                            boolean alreadyAdded = false;
                            for (PossibleAction action : possibleActions.getList()) {
                                if (action instanceof Initiate1817IPO && ((Initiate1817IPO) action).getCompanyName().equals(comp.getId())) {
                                    alreadyAdded = true;
                                    break;
                                }
                            }
                            if (!alreadyAdded) {
possibleActions.add(new Initiate1817IPO(gameManager.getRoot(), comp.getId()));
                            }
                        }
                    }
                }
            }
        }
    }

    @Override
    public boolean mayCurrentPlayerSellAnything() {
        if (!super.mayCurrentPlayerSellAnything()) {
            return false;
        }

        // Rule: Cannot sell personal shares if acting for a company
        String activeComp = activeCompanyId.value();
        if (activeComp != null && !activeComp.isEmpty()) {
            return false;
        }

        // Rule: Sequence is strictly Sell -> Short -> Buy/IPO. 
        // Cannot sell if already shorted or bought/IPOed.
        if (playerHasShorted.value() || playerHasBoughtOrIPOed.value()) {
            return false;
        }

        return true;
    }

    @Override
    public boolean sellShares(rails.game.action.SellShares action) {
        boolean result = super.sellShares(action);
        if (result) {
            playerHasSold.set(true);
        }
        return result;
    }

    @Override
    public boolean buyShares(String playerName, rails.game.action.BuyCertificate action) {
        boolean result = super.buyShares(playerName, action);
        if (result) {
            playerHasBoughtOrIPOed.set(true);
        }
        return result;
    }



    public void registerIpo(String compId) {
        ipoedThisRound.add(compId);
    }

private void startAuctionRound(PublicCompany_1817 comp, String hexId, int stationNumber, int bid) {
        net.sf.rails.game.Player initiator = gameManager.getCurrentPlayer();
        log.info("IPO INITIATED: Company " + comp.getId() + " by Player " + initiator.getName() +
                " at Hex " + hexId + " Station " + stationNumber + " with starting bid $" + bid);

        net.sf.rails.common.ReportBuffer.add(this,
                initiator.getName() + " initiates an IPO for " + comp.getId() + " at " + hexId
                        + " with a starting bid of " + net.sf.rails.game.financial.Bank.format(this, bid) + ".");


        auctionCounter.set(auctionCounter.value() + 1);
        String uniqueAuctionId = "Auction_" + comp.getId() + "_" + getId() + "_" + auctionCounter.value();

        gameManager.setInterruptedRound(this);
        AuctionRound_1817 auctionRound = gameManager.createRound(AuctionRound_1817.class, uniqueAuctionId);
        auctionRound.setupAuction(comp, hexId, stationNumber, bid, initiator, gameManager.getPlayers());
    }

    @Override
    public boolean mayPlayerSellShareOfCompany(net.sf.rails.game.PublicCompany company) {
        if (!super.mayPlayerSellShareOfCompany(company)) {
            return false;
        }

        if (company instanceof PublicCompany_1817) {
            // Rule 5.2: Shares of a 2-share company may not be sold.
            if (((PublicCompany_1817) company).getShareCount() == 2) {
                return false;
            }
        }

        // Rule 5.2: Shares in a company may not be sold during the stock round it formed via IPO.
        if (ipoedThisRound.contains(company.getId())) {
            return false;
        }

        // Rule 5.3: Short certificates are liabilities, not sellable assets.
        // Due to Mandatory Reconciliation, if a player holds a short, they hold NO
        // regular shares.
        net.sf.rails.game.Player currentPlayer = gameManager.getCurrentPlayer();
        if (currentPlayer != null) {
            for (net.sf.rails.game.financial.PublicCertificate c : currentPlayer.getPortfolioModel()
                    .getCertificates(company)) {
                if (c instanceof net.sf.rails.game.specific._1817.ShortCertificate) {
                    return false;
                }
            }
        }

        return true;
    }


    @Override
    protected boolean processGameSpecificAction(PossibleAction action) {
        if (action instanceof Initiate1817IPO) {
            try {
                Initiate1817IPO ipoAction = (Initiate1817IPO) action;
                PublicCompany_1817 comp = (PublicCompany_1817) ipoAction.getCompany();

                int bid = ipoAction.getBid();


                // Server-side validation: calculate total purchasing power
                int purchasingPower = currentPlayer.getCash();
                for (net.sf.rails.game.PrivateCompany pc : currentPlayer.getPortfolioModel().getPrivateCompanies()) {
                    purchasingPower += pc.getBasePrice();
                }

                // Rule 5.5: Reject if bid exceeds equity or violates strict limits
                if (bid > purchasingPower) {
                    net.sf.rails.common.ReportBuffer.add(this, currentPlayer.getName() + " attempted an illegal IPO bid of $" + bid + ". Max allowed by purchasing power is $" + purchasingPower + ".");
                    return false;
                }
                if (bid < 100 || bid > 400 || bid % 5 != 0) {
                    net.sf.rails.common.ReportBuffer.add(this, currentPlayer.getName() + " attempted an illegal IPO bid. Bid must be a multiple of $5 between $100 and $400.");
                    return false;
                }

                
                String hexId = ipoAction.getHexId();

                if ("E22".equals(hexId)) {
                    log.info("E22 IPO Initiated. Pausing Stock Round to prompt for North/South location.");
                    waitingForE22Start.set(true);
                    pendingE22Company.set(comp);
                    pendingE22Bid.set(bid);
                    return true;
                } else {
                    startAuctionRound(comp, hexId, 0, bid);
                    playerHasBoughtOrIPOed.set(true);
                    hasActed.set(true);
                    companyBoughtThisTurnWrapper.set(comp);
                    return true;
                }
            } catch (Exception e) {
                log.error("Failed to transition to 1817 Auction", e);
                return false;
            }
        }

        if (action instanceof LayNYHomeToken) {
            LayNYHomeToken layAction = (LayNYHomeToken) action;
            PublicCompany_1817 comp = (PublicCompany_1817) pendingE22Company.value();
            int bid = pendingE22Bid.value();
            int stationNumber = layAction.getChosenStation();

            log.info("E22 Location selected: Station {}", stationNumber);

            waitingForE22Start.set(false);
            startAuctionRound(comp, "E22", stationNumber, bid);
            playerHasBoughtOrIPOed.set(true);
            hasActed.set(true);
            companyBoughtThisTurnWrapper.set(comp);
            return true;

        }

        if (action instanceof net.sf.rails.game.specific._1817.action.Short1817) {
            net.sf.rails.game.specific._1817.action.Short1817 sAction = (net.sf.rails.game.specific._1817.action.Short1817) action;
            PublicCompany comp = companyManager.getPublicCompany(sAction.getCompanyId());

            if (comp != null && comp.hasStockPrice()) {
                // 1. Calculate price (The market price is the per-share value in 1817)
                int price = comp.getCurrentSpace().getPrice();

                // 2. Find the ShortCertificate to give to the player
                net.sf.rails.game.financial.PublicCertificate shortCert = null;
                for (net.sf.rails.game.financial.PublicCertificate c : getRoot().getBank().getOSI()
                        .getPortfolioModel().getCertificates()) {
                    if (c instanceof net.sf.rails.game.specific._1817.ShortCertificate && c.getCompany() == comp) {
                        shortCert = c;
                        break;
                    }
                }

                if (shortCert != null) {
                    // 3. Complete the sale: Player gets the Liability (Short Cert) and the Cash
                    shortCert.moveTo(currentPlayer.getPortfolioModel());
                    net.sf.rails.game.state.Currency.fromBank(price, currentPlayer);

                    // 3b. Deposit one explicitly REGULAR share from the Unavailable portfolio
                    // directly to the Open Market (Pool) to simulate the sale
                    net.sf.rails.game.financial.PublicCertificate regCertToSell = getRoot().getBank().getUnavailable()
                            .getPortfolioModel().findCertificate(comp, 1, false);
                    if (regCertToSell != null) {
                        regCertToSell.moveTo(pool);
                    }

                    // Rule 5.3: Short selling is considered a sale and prevents the player
                    // from purchasing this stock for the remainder of the stock round.
                    shortedThisRound.put(currentPlayer.getId() + "_" + comp.getId(), "shorted");

playerHasShorted.set(true);
                    
                    // We DO NOT set companyBoughtThisTurnWrapper here. 
                    // That wrapper is used by the base engine to lock out cross-company buying,
                    // which we now handle explicitly with our state machine.

                    // 5. Record the action and report
                    String logMsg = net.sf.rails.common.LocalText.getText("SHORT_SELL_LOG",
                            currentPlayer.getId(), comp.getId(), net.sf.rails.game.financial.Bank.format(this, price));
                    if (logMsg.startsWith("Missing text")) {
                        logMsg = currentPlayer.getId() + " sells short " + comp.getId() + " for "
                                + net.sf.rails.game.financial.Bank.format(this, price);
                    }
                    net.sf.rails.common.ReportBuffer.add(this, logMsg);

                    hasActed.set(true);
                    return true;
                }
            }
        }

        if (action instanceof net.sf.rails.game.specific._1817.action.TakeLoans_1817) {
            net.sf.rails.game.specific._1817.action.TakeLoans_1817 tlAction = (net.sf.rails.game.specific._1817.action.TakeLoans_1817) action;
            PublicCompany comp = companyManager.getPublicCompany(tlAction.getCompanyId());
            if (comp instanceof net.sf.rails.game.specific._1817.PublicCompany_1817) {
                net.sf.rails.game.specific._1817.PublicCompany_1817 comp1817 = (net.sf.rails.game.specific._1817.PublicCompany_1817) comp;

                // 1. Execute the centralized loan logic (adds bond, adds cash, moves stock
                // left)
                comp1817.executeLoan();
                activeCompanyId.set(comp.getId());
                companyHasTakenLoan.set(true);
                hasActed.set(true);
                return true;


            }
        }
        if (action instanceof net.sf.rails.game.specific._1817.action.CompanyBuyOpenMarketShare_1817) {
            net.sf.rails.game.specific._1817.action.CompanyBuyOpenMarketShare_1817 cbAction = (net.sf.rails.game.specific._1817.action.CompanyBuyOpenMarketShare_1817) action;
            PublicCompany comp = companyManager.getPublicCompany(cbAction.getCompanyId());

            if (comp != null && comp.isBuyable()) {
                int price = comp.getMarketPrice();
                net.sf.rails.game.financial.PublicCertificate poolCert = pool.findCertificate(comp, 1, false);

                if (poolCert != null && comp.getCash() >= price) {
                    // Move the certificate to the company treasury
                    poolCert.moveTo(comp.getPortfolioModel());

                    // 1817 Rule 5.6: The company pays the bank for the stock purchase
                    // Use static Currency utility to move cash from the owner to the bank.
                    net.sf.rails.game.state.Currency.toBank(comp, price);

                    net.sf.rails.common.ReportBuffer.add(this,
                            comp.getId() + " buys one share from the open market for "
                                    + net.sf.rails.game.financial.Bank.format(this, price));
                    
                    
                    activeCompanyId.set(comp.getId());
                    companyHasBoughtShare.set(true);

                    companyBoughtThisTurnWrapper.set(comp);
                    return true;
                }
            }
        }

        return super.processGameSpecificAction(action);
    }

    protected void resetPlayerTurnStates() {
        playerHasSold.set(false);
        playerHasShorted.set(false);
        playerHasBoughtOrIPOed.set(false);
        activeCompanyId.set(null); 
        companyHasTakenLoan.set(false);
        companyHasBoughtShare.set(false);
    }

    /**
     * Exempts 1817 2-share companies from the standard 60% global hold limit.
     * Prevents the engine from deadlocking when a player holds 100% of a new
     * company.
     */
    @Override
    public boolean checkAgainstHoldLimit(net.sf.rails.game.Player player, net.sf.rails.game.PublicCompany company,
            int number) {
        if (company instanceof PublicCompany_1817) {
            // Exempt 1817 2-share companies
            if (((PublicCompany_1817) company).getShareCount() == 2) {
                return true;
            }

            // Rule 5.1: A player may close a short position by purchasing a matching share
            // even if he is at or above the certificate limit.
            for (net.sf.rails.game.financial.PublicCertificate c : player.getPortfolioModel().getCertificates()) {
                if (c instanceof net.sf.rails.game.specific._1817.ShortCertificate && c.getCompany() == company) {
                    return true;
                }
            }
        }
        return super.checkAgainstHoldLimit(player, company, number);
    }

    @Override
    public boolean process(rails.game.action.PossibleAction action) {
        
boolean isEndingTurn = (action instanceof rails.game.action.NullAction);

        boolean result = super.process(action);

        // Rule 5.1 Mandatory Reconciliation: Check after any successful action that
        // could create a conflicting long/short state
        if (result) {
            if (isEndingTurn) {
                resetPlayerTurnStates();
            }

            if (gameManager.getCurrentPlayer() != null) {
                net.sf.rails.game.PublicCompany comp = null;
                if (action instanceof rails.game.action.BuyCertificate) {
                    comp = ((rails.game.action.BuyCertificate) action).getCompany();
                } else if (action instanceof net.sf.rails.game.specific._1817.action.Short1817) {
                    comp = companyManager
                            .getPublicCompany(((net.sf.rails.game.specific._1817.action.Short1817) action).getCompanyId());
                }

                if (comp != null) {
                    reconcileShorts(gameManager.getCurrentPlayer(), comp);
                }
            }
        }

        return result;
    }

    private void reconcileShorts(net.sf.rails.game.Player player, net.sf.rails.game.PublicCompany comp) {
        if (player == null || comp == null)
            return;

        // Use a loop because a player could theoretically acquire multiple pairs (e.g.,
        // via merger)
        boolean foundPair = true;
        while (foundPair) {
            net.sf.rails.game.financial.PublicCertificate shortCert = null;
            net.sf.rails.game.financial.PublicCertificate regularCert = null;

            for (net.sf.rails.game.financial.PublicCertificate c : player.getPortfolioModel().getCertificates()) {
                if (c.getCompany() == comp) {
                    if (c instanceof net.sf.rails.game.specific._1817.ShortCertificate) {
                        shortCert = c;
                    } else if (!c.isPresidentShare()) {
                        // Only common shares are returned to the Open Short Interest section.
                        regularCert = c;
                    }
                }
            }

            if (shortCert != null && regularCert != null) {
                net.sf.rails.game.financial.BankPortfolio osiBank = getRoot().getBank().getOSI();
                net.sf.rails.game.financial.BankPortfolio unavailableBank = getRoot().getBank().getUnavailable();
                shortCert.moveTo(osiBank);
                regularCert.moveTo(unavailableBank);
                log.info(
                        "Mandatory Reconciliation: " + player.getName() + " short position closed for " + comp.getId());
                net.sf.rails.common.ReportBuffer.add(this,
                        player.getName() + " automatically closes a short position in " + comp.getId());
            } else {
                foundPair = false;
            }
        }
    }

    @Override
    protected void finishRound() {

        // Rule 5.7: Examine price in operating order.
        // gameManager.getAllPublicCompanies() returns definition order. We must
        // explicitly sort it.
        List<PublicCompany> operatingOrder = new java.util.ArrayList<>(gameManager.getAllPublicCompanies());
        java.util.Collections.sort(operatingOrder);

        for (PublicCompany comp : operatingOrder) {
            if (!(comp instanceof PublicCompany_1817) || comp.isClosed()) {
                continue;
            }

            PublicCompany_1817 comp1817 = (PublicCompany_1817) comp;

            // 1. Upward Movement: 5-share and 10-share companies
            if (comp1817.getShareCount() > 2) {
                int totalPlayerOwnedShares = 0;
                for (net.sf.rails.game.Player p : getRoot().getPlayerManager().getPlayers()) {
                    // We sum the actual share count, not certificate count
                    totalPlayerOwnedShares += p.getPortfolioModel().getShares(comp);
                }

                // If players own 100% or more, move right (up)
                if (totalPlayerOwnedShares >= comp1817.getShareCount()) {
                    // Use the specific moveRight logic for 1817's 2D market
                    stockMarket.moveRight(comp, 1);
                }
            }

            // 2. Downward Movement: Move left for EACH share in the open market
            // This applies to all company sizes (2, 5, and 10).
            int openMarketShares = pool.getShares(comp);
            if (openMarketShares > 0) {
                if (stockMarket instanceof StockMarket_1817) {
                    ((StockMarket_1817) stockMarket).moveLeftOrDown(comp, openMarketShares);
                } else {
                    for (int i = 0; i < openMarketShares; i++) {
                        stockMarket.withhold(comp);
                    }
                }
            }
        }


        super.finishRound();
    }


}
