// --- START FIX ---
package net.sf.rails.game.specific._1870;

import net.sf.rails.game.Player;
import net.sf.rails.game.PublicCompany;
import net.sf.rails.game.state.Owner;
import net.sf.rails.game.financial.StockRound;
import net.sf.rails.game.state.StringState;
import net.sf.rails.common.ReportBuffer;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import net.sf.rails.game.specific._1870.action.ExchangeMKT_1870;
import net.sf.rails.game.specific._1870.action.RedeemShare_1870;
import net.sf.rails.game.specific._1870.action.ReissueShares_1870;
import java.util.List;

public class StockRound_1870 extends StockRound {

    private static final Logger log = LoggerFactory.getLogger(StockRound_1870.class);
    private final StringState jumpedToPlayer = StringState.create(this, "jumpedToPlayer");
    // Track actions per round to enforce 1870 limits. We use StringState to avoid
    // ArrayListState missing symbols.
    private final net.sf.rails.game.state.StringState redeemedThisRound = net.sf.rails.game.state.StringState
            .create(this, "redeemedThisRound", "");
    private final net.sf.rails.game.state.StringState reissuedThisRound = net.sf.rails.game.state.StringState
            .create(this, "reissuedThisRound", "");

    private final net.sf.rails.game.state.StringState companiesThatHaveReissued = net.sf.rails.game.state.StringState
.create(this, "companiesThatHaveReissued", "");

    private final StringState protectingCompanyId = StringState.create(this, "protectingCompanyId", "");
    private final net.sf.rails.game.state.IntegerState protectingShares = net.sf.rails.game.state.IntegerState
            .create(this, "protectingShares", 0);

    public static class ProtectShare_1870 extends rails.game.action.PossibleAction {
        private static final long serialVersionUID = 1L;
        private final String companyId;
        private final int shares;

        public ProtectShare_1870(PublicCompany company, int shares) {
            super(company.getRoot());
            this.companyId = company.getId();
            this.shares = shares;
            setButtonLabel("Protect " + companyId + " Price");
        }

        public String getCompanyId() {
            return companyId;
        }

        public int getShares() {
            return shares;
        }
    }

    public static class DeclineProtection_1870 extends rails.game.action.PossibleAction {
        private static final long serialVersionUID = 1L;
        private final String companyId;
        private final int shares;

        public DeclineProtection_1870(PublicCompany company, int shares) {
            super(company.getRoot());
            this.companyId = company.getId();
            this.shares = shares;
            setButtonLabel("Decline Protection");
        }

        public String getCompanyId() {
            return companyId;
        }

        public int getShares() {
            return shares;
        }
    }

    public StockRound_1870(net.sf.rails.game.GameManager parent, String id) {

        super(parent, id);
        // 1870 Rule: Certificates cannot be mathematically split.
        // A 20% president's certificate is indivisible and cannot be sold as a 10% share.
        this.certificateSplitAllowed = false;
    }

    public void setNextPlayerAfterProtection(Player protectingPresident) {
        List<Player> players = playerManager.getPlayers();
        int pIndex = players.indexOf(protectingPresident);
        int nextIndex = (pIndex + 1) % players.size();
        Player nextPlayer = players.get(nextIndex);

        // Advance the engine's internal pointer immediately
        setCurrentPlayer(nextPlayer);

        // Wipe the seller's turn state so the new player starts completely fresh
        hasActed.set(false);
        companyBoughtThisTurnWrapper.set(null);
        hasSoldThisTurnBeforeBuying.set(false);
        sellPrices.clear();
        numPasses.set(0);

        // Clear jumpedToPlayer so resume() doesn't double-fire
        jumpedToPlayer.set(null);

        ReportBuffer.add(this, "=> TURN JUMP: Play resumes with " + nextPlayer.getName()
                + " (player to the left of the catcher).");
    }

    public void processPassedProtection(PublicCompany company, int sharesSold) {
        // President declined the protection. Proceed with the normal price drop.
        // We pass 'null' for the seller because the transaction is already complete, we
        // just need the drop.
        super.adjustSharePrice(company, null, sharesSold, false);
    }

    @Override
    public void resume() {
        // Apply the turn order jump BEFORE calling super.resume()
        if (jumpedToPlayer.value() != null && !jumpedToPlayer.value().isEmpty()) {
            Player targetPlayer = playerManager.getPlayerByName(jumpedToPlayer.value());
            if (targetPlayer != null) {
                // Advance the engine's internal pointer
                setCurrentPlayer(targetPlayer);

                // Wipe the seller's turn state so the new player starts completely fresh
                hasActed.set(false);
                companyBoughtThisTurnWrapper.set(null);
                hasSoldThisTurnBeforeBuying.set(false);
                sellPrices.clear();
                numPasses.set(0);

                ReportBuffer.add(this, "=> TURN JUMP: Play resumes with " + targetPlayer.getName()
                        + " (player to the left of the catcher).");
            }
            jumpedToPlayer.set(null);
        }

        super.resume();
    }

    @Override
    public Player getCurrentPlayer() {
        // 1. If we are in the middle of a price protection decision, the President acts
        if (protectingCompanyId.value() != null && !protectingCompanyId.value().isEmpty()) {
            PublicCompany comp = getRoot().getCompanyManager().getPublicCompany(protectingCompanyId.value());
            if (comp != null && comp.getPresident() != null) {
                return comp.getPresident();
            }
        }

        // 2. 1870 Rule: If we jumped to a specific player after Price Protection,
        // return them.
        String name = jumpedToPlayer.value();
        if (name != null && !name.isEmpty()) {
            for (Player p : playerManager.getPlayers()) {
                if (p.getName().equals(name))
                    return p;
            }
        }

        Player p = super.getCurrentPlayer();
        if (p == null) {
            // Fallback: If the round doesn't have a current player set, ask playerManager
            p = playerManager.getCurrentPlayer();
        }
        return p;
    }


    @Override
    protected void finishRound() {
        if (raiseIfSoldOut) {
            try {
                for (PublicCompany company : getRoot().getCompanyManager().getAllPublicCompanies()) {
                    if (company.hasStarted() && company.hasStockPrice() && !company.isClosed()) {
                        // 1870 Rule: Stock rises if 100% of shares are in player OR company hands
                        int ipoShares = ipo.getShares(company);
                        int poolShares = pool.getShares(company);
                        if (ipoShares == 0 && poolShares == 0) {
                            net.sf.rails.common.ReportBuffer.add(this, net.sf.rails.common.LocalText.getText("SoldOut", company.getId()));
                            stockMarket.soldOut(company);
                        }
                    }
                }
            } catch (Exception e) {
                log.error("Error in 1870 finishRound", e);
            }
        }

        // Disable base class sold out check to prevent duplicate bumps or incorrect logic
        boolean originalRaise = raiseIfSoldOut;
        raiseIfSoldOut = false;
        
        super.finishRound();
        
        raiseIfSoldOut = originalRaise;
    }

    
@Override
    protected void adjustSharePrice(PublicCompany comp, Owner player, int shares, boolean soldBefore) {
        Player president = comp.getPresident();
        int marketPrice = comp.getCurrentSpace().getPrice() / comp.getShareUnitsForSharePrice();

     // 1870 Price Protection Rule: President can protect if they aren't the seller,
        // haven't sold shares already this round, and have enough cash to buy the shares at market price. We check the president's sell status to prevent gaming the protection by selling shares and then protecting to reset the price.
        boolean presidentHasSold = president != null && president.hasSoldThisRound(comp);
        

        if (president != null && president != player && !presidentHasSold && president.getCash() >= (marketPrice * shares)) {
            float currentCerts = president.getPortfolioModel().getCertificateCount();
            int certLimit = getRoot().getGameManager().getPlayerCertificateLimit(president);
            
            if ((currentCerts + shares) <= certLimit) {

                protectingCompanyId.set(comp.getId());
                protectingShares.set(shares);
                ReportBuffer.add(this,
                        "Price protection opportunity: " + president.getName() + " may protect " + comp.getId() + ".");
                return; // Intercept! Do not drop the price yet.

            }

        }

        // Otherwise, execute normal price drop
        super.adjustSharePrice(comp, player, shares, soldBefore);
    }

    @Override
    public boolean setPossibleActions() {
        boolean result = super.setPossibleActions();

        // 1. 1870 Rule: Companies do not buy privates in Stock Rounds
        // We collect in a separate list to avoid ConcurrentModificationException or
        // ImmutableList crashes
        java.util.List<rails.game.action.PossibleAction> actionsToRemove = new java.util.ArrayList<>();

        for (rails.game.action.PossibleAction action : possibleActions.getList()) {
            if (action instanceof rails.game.action.BuyPrivate) {
                actionsToRemove.add(action);
            }
            // 1870 Rule: Explicitly enforce no stock selling in the first round to close any loopholes.
if (action instanceof rails.game.action.SellShares && getRoot().getGameManager().getSRNumber() == 1) {
                actionsToRemove.add(action);
            }
        }
        if (!actionsToRemove.isEmpty()) {
            log.info("Removing restricted actions (BuyPrivate/SellShares) from Stock Round per 1870 rules.");
            possibleActions.removeAll(actionsToRemove);
        }

        Player currentPlayer = getCurrentPlayer();
        if (currentPlayer == null) {
            log.info("setPossibleActions: No current player found.");
            return result;
        }

        // --- PRICE PROTECTION OVERRIDE ---
        if (protectingCompanyId.value() != null && !protectingCompanyId.value().isEmpty()) {
            possibleActions.clear(); // Clear normal actions
            PublicCompany comp = getRoot().getCompanyManager().getPublicCompany(protectingCompanyId.value());
            if (comp != null) {
                possibleActions.add(new ProtectShare_1870(comp, protectingShares.value()));
                possibleActions.add(new DeclineProtection_1870(comp, protectingShares.value()));
            }
            return true;
        }

        // 1870 Rule: Players can legally hold >60% of a company if acquired via price
        // protection.
        // The base engine thinks this is illegal, forces a sell, and suppresses the
        // Pass/Done buttons.
        // We restore Pass/Done as long as the player is still within their overall
        // certificate limit.
        float certCount = currentPlayer.getPortfolioModel().getCertificateCount();
        int certLimit = getRoot().getGameManager().getPlayerCertificateLimit(currentPlayer);

        if (certCount <= certLimit) {
            boolean hasNullAction = false;
            for (rails.game.action.PossibleAction pa : possibleActions.getList()) {
                if (pa instanceof rails.game.action.NullAction) {
                    hasNullAction = true;
                    break;
                }
            }

            if (!hasNullAction) {
                if (hasActed.value()) {
                    possibleActions
                            .add(new rails.game.action.NullAction(getRoot(), rails.game.action.NullAction.Mode.DONE));
                } else {
                    possibleActions
                            .add(new rails.game.action.NullAction(getRoot(), rails.game.action.NullAction.Mode.PASS));
                }
                result = true;
            }
        }

        boolean addedCustomAction = false;

        // 2. MKT Exchange Check
        for (net.sf.rails.game.PrivateCompany priv : currentPlayer.getPortfolioModel().getPrivateCompanies()) {
            if ("MKT".equals(priv.getId())) {
                PublicCompany mktPub = getRoot().getCompanyManager().getPublicCompany("MKT");
                if (mktPub != null && !mktPub.hasFloated()) {
                    net.sf.rails.game.specific._1870.action.ExchangeMKT_1870 mktAct = new net.sf.rails.game.specific._1870.action.ExchangeMKT_1870(
                            priv);
                    if (!possibleActions.getList().contains(mktAct)) {
                        possibleActions.add(mktAct);
                        addedCustomAction = true;
                    }
                }
            }
        }

        // 3. Share Redemption & Reissue Logic
        log.info("Starting 1870 President Action evaluation for " + currentPlayer.getName());

        boolean noNormalActionsTaken = !hasActed.value() && !hasSoldThisTurnBeforeBuying.value()
                && sellPrices.isEmpty();
        if (noNormalActionsTaken) {
            for (PublicCompany company : getRoot().getCompanyManager().getAllPublicCompanies()) {
                if (company.isClosed())
                    continue;

                if (company.getPresident() != currentPlayer) {
                    // This is a common place where it "skips" if ownership just changed
                    continue;
                }

                // Gatekeeper A: One action per round limit
                boolean reissued = reissuedThisRound.value().contains(company.getId() + ",");
                boolean redeemed = redeemedThisRound.value().contains(company.getId() + ",");
                boolean actedThisRound = reissued || redeemed;

                // Gatekeeper B: Must have operated at least once
                if (!actedThisRound && company.hasOperated()) {

                    // --- REISSUE EVALUATION ---
                    int sharesInTreasury = company.getPortfolioModel().getShares(company);
                    int sharesInIpo = ipo.getShares(company);

                    if (sharesInIpo <= 0 && sharesInTreasury > 0) {
                        net.sf.rails.game.specific._1870.action.ReissueShares_1870 rei = new net.sf.rails.game.specific._1870.action.ReissueShares_1870(
                                company);
                        if (!possibleActions.getList().contains(rei)) {
                            possibleActions.add(rei);
                            addedCustomAction = true;
                        }
                    }

                    // --- REDEEM EVALUATION ---
                    int marketPrice = company.getCurrentSpace().getPrice() / company.getShareUnitsForSharePrice();
                    boolean hasCash = company.getCash() >= marketPrice;
                    int poolShares = pool.getShares(company);

                    int playerShares = 0;
                    for (Player p : playerManager.getPlayers()) {
                        playerShares += p.getPortfolioModel().getShares(company);
                    }

                    if (sharesInTreasury < 4 && hasCash && (playerShares + poolShares >= 7)) {
                        // Check players for non-president shares
                        boolean playersHoldRedeemable = false;
                        for (Player p : playerManager.getPlayers()) {
                            for (net.sf.rails.game.financial.PublicCertificate cert : p.getPortfolioModel()
                                    .getCertificates(company)) {
                                if (!cert.isPresidentShare()) {
                                    playersHoldRedeemable = true;
                                    break;
                                }
                            }
                            if (playersHoldRedeemable)
                                break;
                        }

                        if (poolShares > 0 || playersHoldRedeemable) {
                            net.sf.rails.game.specific._1870.action.RedeemShare_1870 red = new net.sf.rails.game.specific._1870.action.RedeemShare_1870(
                                    company);
                            if (!possibleActions.getList().contains(red)) {
                                possibleActions.add(red);
                                addedCustomAction = true;
                            }
                        }
                    }
                }
            }
        }

        return result || addedCustomAction;
    }

    @Override
    public boolean checkAgainstHoldLimit(net.sf.rails.game.Player player, net.sf.rails.game.PublicCompany company,
            int number) {
        boolean baseCheck = super.checkAgainstHoldLimit(player, company, number);

        // If the base check passes (e.g., under 60% limit, or the stock token is in the
        // Green/Brown zone where limits are lifted), it is legal.
        if (baseCheck) {
            return true;
        }

        // 1870 Rule Exception: The purchasing president may hold shares in excess of
        // the normal 60% limit if acquired through price protection.
        // We permit passive holding (number == 0) for the President to bypass the
        // forced sell trigger.
        // Active buying (number > 0) outside of Green/Brown zones remains blocked by
        // returning false.
        if (number == 0 && company.getPresident() == player) {
            return true;
        }

        return false;
    }

    @Override
    public boolean process(rails.game.action.PossibleAction action) {
      if (action instanceof rails.game.action.BuyCertificate) {
            rails.game.action.BuyCertificate buyAction = (rails.game.action.BuyCertificate) action;
            PublicCompany comp = buyAction.getCompany();
            
            // Check if the purchase is from the IPO by comparing the unique portfolio names
            boolean fromIPO = false;
            if (buyAction.getFromName() != null && ipo != null) {
                fromIPO = buyAction.getFromName().equals(ipo.getUniqueName());
            } else if (buyAction.getFromPortfolio() == ipo) {
                fromIPO = true;
            }
            
            boolean result = super.process(action);
            
            if (result && fromIPO && companiesThatHaveReissued.value().contains(comp.getId() + ",")) {
                // 1870 rules limit purchases to 1 certificate per turn, but we'll check numberBought just in case.
                int certsBought = buyAction.getNumberBought() > 0 ? buyAction.getNumberBought() : 1;
                int cost = buyAction.getPrice() * certsBought;
                
                // The base engine wired the player's money to the Bank.
                // We wire it from the Bank to the Company to complete the reissue transaction.
                net.sf.rails.game.state.Currency.wire(getRoot().getBank(), cost, comp);
                net.sf.rails.common.ReportBuffer.add(this, "Payment for reissued share transferred to " + comp.getId() + " treasury.");
            }
            return result;
        }
        
        if (action instanceof net.sf.rails.game.specific._1870.action.ReissueShares_1870) {
            String companyId = ((net.sf.rails.game.specific._1870.action.ReissueShares_1870) action).getCompanyId();
            PublicCompany company = getRoot().getCompanyManager().getPublicCompany(companyId);
            reissuedThisRound.set(reissuedThisRound.value() + company.getId() + ",");

            if (!companiesThatHaveReissued.value().contains(company.getId() + ",")) {
companiesThatHaveReissued.set(companiesThatHaveReissued.value() + company.getId() + ",");
}

            int sharesToReissue = company.getPortfolioModel().getShares(company);
            for (net.sf.rails.game.financial.PublicCertificate cert : company.getPortfolioModel()
                    .getCertificates(company)) {
                cert.moveTo(ipo);
            }

            int currentPrice = company.getCurrentSpace().getPrice();
            int oldPar = company.getParPrice();
            int newPar = calculateNewPar(oldPar, currentPrice);

net.sf.rails.game.financial.StockSpace newParSpace = stockMarket.getStartSpace(newPar);

        // If getStartSpace returns null (e.g., because spaces > $100 aren't explicitly marked as StartSpaces in the XML),
        // we must search the stock market grid for a space matching the new par price.
        if (newParSpace == null) {
            int bestRow = Integer.MAX_VALUE;
            for (int r = 0; r < 40; r++) {
                for (int c = 0; c < 40; c++) {
                    try {
                        net.sf.rails.game.financial.StockSpace space = stockMarket.getStockSpace(r, c);
                        if (space != null && space.getPrice() == newPar) {
                            // Prefer the uppermost row for par spaces
                            if (r < bestRow) {
                                bestRow = r;
                                newParSpace = space;
                            }
                        }
                    } catch (Exception e) {
                        // Ignore potential out of bounds exceptions
                    }
                }
            }
        }
        
        // 1870 Rule: "If the new par value is greater than $200, the par value token should be placed 
        // on the top row of the stock market in the square closest to the new par value."
        // If we didn't find an exact match (e.g. target is $220), we find the closest space in the top row (row 0).
        if (newParSpace == null && newPar > 200) {
            int minDiff = Integer.MAX_VALUE;
            for (int c = 0; c < 40; c++) {
                try {
                    net.sf.rails.game.financial.StockSpace space = stockMarket.getStockSpace(0, c);
                    if (space != null) {
                        int diff = Math.abs(space.getPrice() - newPar);
                        if (diff < minDiff) {
                            minDiff = diff;
                            newParSpace = space;
                        }
                    }
                } catch (Exception e) {
                    // Ignore potential out of bounds exceptions
                }
            }
        }

        if (newParSpace != null) {
            company.setParSpace(newParSpace);
        } else {
            log.error("Could not find a valid par space for price: " + newPar);
        }

            net.sf.rails.common.ReportBuffer.add(this, company.getId() + " reissues " + sharesToReissue
                    + " share(s) to IPO. Par adjusted to " + getRoot().getBank().getCurrency().format(newPar) + ".");

numPasses.set(0);
            List<Player> players = playerManager.getPlayers();
            int pIndex = players.indexOf(getCurrentPlayer());
            playerManager.setPriorityPlayer(players.get((pIndex + 1) % players.size()));

            hasActed.set(true);
            super.process(new rails.game.action.NullAction(getRoot(), rails.game.action.NullAction.Mode.DONE));
            return true;


        } else if (action instanceof net.sf.rails.game.specific._1870.action.RedeemShare_1870) {
            String companyId = ((net.sf.rails.game.specific._1870.action.RedeemShare_1870) action).getCompanyId();
            PublicCompany company = getRoot().getCompanyManager().getPublicCompany(companyId);
            redeemedThisRound.set(redeemedThisRound.value() + company.getId() + ",");

            int marketPrice = company.getCurrentSpace().getPrice() / company.getShareUnitsForSharePrice();

            boolean fromPool = false;
            net.sf.rails.game.financial.PublicCertificate certToMove = null;
            if (pool.getShares(company) > 0) {
                certToMove = pool.getCertificates(company).iterator().next();
                fromPool = true;
            } else {
                for (net.sf.rails.game.financial.PublicCertificate cert : getCurrentPlayer().getPortfolioModel()
                        .getCertificates(company)) {
                    if (!cert.isPresidentShare()) {
                        certToMove = cert;
                        break;
                    }
                }
            }

            if (certToMove != null) {
                if (fromPool) {
                    net.sf.rails.game.state.Currency.wire(company, marketPrice, getRoot().getBank());
                } else {
                    net.sf.rails.game.state.Currency.wire(company, marketPrice, getCurrentPlayer());
                }
                certToMove.moveTo(company.getPortfolioModel());
                net.sf.rails.common.ReportBuffer.add(this, company.getId() + " redeems a share for "
                        + getRoot().getBank().getCurrency().format(marketPrice) + ".");
            }

numPasses.set(0);
            List<Player> players = playerManager.getPlayers();
            int pIndex = players.indexOf(getCurrentPlayer());
            playerManager.setPriorityPlayer(players.get((pIndex + 1) % players.size()));

            hasActed.set(true);
            super.process(new rails.game.action.NullAction(getRoot(), rails.game.action.NullAction.Mode.DONE));
            return true;
        } else if (action instanceof ExchangeMKT_1870) {

            String mktPrivateId = ((ExchangeMKT_1870) action).getMktPrivateId();
            net.sf.rails.game.PrivateCompany mktPriv = getRoot().getCompanyManager().getPrivateCompany(mktPrivateId);
            PublicCompany mktPub = getRoot().getCompanyManager().getPublicCompany("MKT");

            // 1. Close Private
            mktPriv.close();

            // 2. Set Par to $100
            mktPub.setParSpace(stockMarket.getStartSpace(100));

            // 3. Move President's Share to Player
            mktPub.getPresidentsShare().moveTo(getCurrentPlayer().getPortfolioModel());

            // 4. Record
            net.sf.rails.common.ReportBuffer.add(this, getCurrentPlayer().getName()
                    + " exchanges MKT private for MKT public president's share. Par set at $100.");

            return true;
        } else if (action instanceof ProtectShare_1870) {
            ProtectShare_1870 prot = (ProtectShare_1870) action;
            PublicCompany company = getRoot().getCompanyManager().getPublicCompany(prot.getCompanyId());
            Player president = company.getPresident();
            int sharesToProtect = prot.getShares();
            int marketPrice = company.getCurrentSpace().getPrice() / company.getShareUnitsForSharePrice();
            int totalCost = marketPrice * sharesToProtect;

            // 1. President pays Bank
            net.sf.rails.game.state.Currency.wire(president, totalCost, getRoot().getBank());

            // 2. Move shares from pool to President
            int moved = 0;
            for (net.sf.rails.game.financial.PublicCertificate cert : pool.getCertificates(company)) {
                if (moved >= sharesToProtect)
                    break;
                int certShares = cert.getShares();
                cert.moveTo(president.getPortfolioModel());
                moved += certShares;
            }

            net.sf.rails.common.ReportBuffer.add(this,
                    president.getName() + " protects " + company.getId() + " by buying " + sharesToProtect
                            + " shares for " + getRoot().getBank().getCurrency().format(totalCost) + ".");

            // 3. Clear protection state
            protectingCompanyId.set("");
            protectingShares.set(0);

            // 4. Turn jumps to the player left of the President
            setNextPlayerAfterProtection(president);

            return true;

        } else if (action instanceof DeclineProtection_1870) {
            DeclineProtection_1870 decl = (DeclineProtection_1870) action;
            PublicCompany company = getRoot().getCompanyManager().getPublicCompany(decl.getCompanyId());
            int sharesToDrop = decl.getShares();

            net.sf.rails.common.ReportBuffer.add(this,
                    company.getPresident().getName() + " declines to protect " + company.getId() + ".");

            // 1. Clear protection state
            protectingCompanyId.set("");
            protectingShares.set(0);

            // 2. Proceed with the delayed price drop
            processPassedProtection(company, sharesToDrop);

            return true;
        }
        return super.process(action);
    }

    

    private int calculateNewPar(int oldPar, int marketPrice) {
        int target = (int) Math.round(marketPrice * 0.75);
        int[] pars = { 68, 72, 76, 82, 90, 100, 110, 120, 140, 160, 180, 200, 220, 250, 275, 300, 350, 400 };
        int closest = pars[0];
        int minDiff = Integer.MAX_VALUE;
        for (int p : pars) {
            int diff = Math.abs(target - p);
            if (diff < minDiff) {
                closest = p;
                minDiff = diff;
            } else if (diff == minDiff) {
                // Break ties by mimicking the historical printed table in the PDF rules
                if (marketPrice == 140 && p == 110)
                    closest = p;
                if (marketPrice == 200 && p == 140)
                    closest = p;
            }
        }
        return Math.max(oldPar, closest);
    }

}
// --- END FIX ---