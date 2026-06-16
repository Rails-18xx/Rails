package net.sf.rails.game.specific._1817;

import java.util.List;
import java.util.ArrayList;
import net.sf.rails.game.GameManager;
import net.sf.rails.game.MapHex;
import net.sf.rails.game.Player;
import net.sf.rails.game.Round;
import net.sf.rails.game.state.ArrayListState;
import net.sf.rails.game.state.GenericState;
import net.sf.rails.game.state.IntegerState;
import net.sf.rails.game.state.Currency;
import net.sf.rails.game.state.BooleanState;
import net.sf.rails.game.specific._1817.action.Bid1817IPO;
import net.sf.rails.game.specific._1817.action.LayNYHomeToken;
import net.sf.rails.game.specific._1817.action.SettleIPO_1817;
import rails.game.action.PossibleAction;
import rails.game.action.LayBaseToken;
import rails.game.action.NullAction;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import net.sf.rails.game.Stop;

public class AuctionRound_1817 extends Round {

    private static final Logger log = LoggerFactory.getLogger(AuctionRound_1817.class);

    public enum AuctionType {
        IPO,
        LIQUIDATION,
        ACQUISITION,
        FRIENDLY_SALE
    }

    protected final GenericState<AuctionType> auctionType;
    protected final IntegerState minNextBidIncrement; // Usually 5 for IPO, 10 for M&A
    protected final GenericState<PublicCompany_1817> auctionedCompany;
    protected final GenericState<String> targetHexId;
    protected final IntegerState currentBid;
    protected final GenericState<Player> highestBidder;
    protected final GenericState<Player> initiator;
    protected final ArrayListState<Player> activeBidders;
    protected final IntegerState currentPlayerIndex;
    protected final IntegerState targetStationNumber;

    public AuctionRound_1817(GameManager parent, String id) {
        super(parent, id);
        this.auctionType = new GenericState<>(this, "auctionType_" + id, AuctionType.IPO);
        this.minNextBidIncrement = IntegerState.create(this, "minNextBidIncrement_" + id, 5);
        this.auctionedCompany = new GenericState<>(this, "auctionedCompany_" + id);
        this.targetHexId = new GenericState<>(this, "targetHexId_" + id);
        this.currentBid = IntegerState.create(this, "currentBid_" + id, 0);
        this.highestBidder = new GenericState<>(this, "highestBidder_" + id);
        this.initiator = new GenericState<>(this, "initiator_" + id);
        this.activeBidders = new ArrayListState<>(this, "activeBidders_" + id);
        this.currentPlayerIndex = IntegerState.create(this, "currentPlayerIndex_" + id, 0);
        this.targetStationNumber = IntegerState.create(this, "targetStationNumber_" + id, -1);

    }

    public AuctionType getAuctionType() {
        return auctionType.value();
    }

    // Existing setup for IPO
    public void setupAuction(PublicCompany_1817 company, String hexId, int stationNumber, int startingBid,
            Player startingPlayer,
            List<Player> allPlayers) {
        setupAuctionInternal(AuctionType.IPO, company, hexId, stationNumber, startingBid, startingPlayer, allPlayers,
                5);
    }

    // New setup for M&A
    public void setupMAAuction(AuctionType type, PublicCompany_1817 company, int startingBid, Player startingPlayer,
            List<Player> allPlayers) {
        setupAuctionInternal(type, company, null, -1, startingBid, startingPlayer, allPlayers, 10);
    }

    private void setupAuctionInternal(AuctionType type, PublicCompany_1817 company, String hexId, int stationNumber,
            int startingBid, Player startingPlayer, List<Player> allPlayers, int bidIncrement) {
        log.info("AUCTION_LOG: === NEW {} AUCTION INITIATED ===", type);
        log.info("AUCTION_LOG: Company: {} | Target Hex: {} | Initiator: {} | Starting Bid: ${}",
                company.getId(), hexId, startingPlayer.getName(), startingBid);
        this.auctionType.set(type);
        this.minNextBidIncrement.set(bidIncrement);
        this.auctionedCompany.set(company);
        this.targetHexId.set(hexId);
        this.targetStationNumber.set(stationNumber);

        // In Liquidation, the Bank makes the initial $0 bid, so there might not be an
        // initiating player yet.
net.sf.rails.common.ReportBuffer.add(this, "--- " + type + " AUCTION: " + company.getId() + " ---");

        if (type == AuctionType.LIQUIDATION && startingBid == 0) {
            this.currentBid.set(0);
            this.highestBidder.set(null); // Bank is highest bidder initially
            this.initiator.set(startingPlayer); // The player who was supposed to act next starts the bidding
            net.sf.rails.common.ReportBuffer.add(this, "Bank opens bidding at $0.");
        } else {
            this.currentBid.set(startingBid);
            this.initiator.set(startingPlayer);
            this.highestBidder.set(startingPlayer);
            net.sf.rails.common.ReportBuffer.add(this, startingPlayer.getName() + " opens bidding at $" + startingBid + ".");
        }

        this.activeBidders.clear();
        for (Player p : allPlayers) {
            this.activeBidders.add(p);
        }

        int startIndex = allPlayers.indexOf(startingPlayer);
        // If not liquidation, the initiator already bid, so next player acts.
        if (type != AuctionType.LIQUIDATION || startingBid > 0) {
            startIndex = (startIndex + 1) % allPlayers.size();
        }

        this.currentPlayerIndex.set(startIndex);
        advanceToNextValidBidder();
        log.info("AUCTION_LOG: Active bidders post-prune: {}. Next to act: {}",
                this.activeBidders.size(),
                (this.activeBidders.isEmpty() ? "None" : getActingPlayer().getName()));
    }

    public int getCurrentBid() {
        return currentBid.value();
    }

    public Player getHighestBidder() {
        return highestBidder.value();
    }

    public PublicCompany_1817 getAuctionedCompany() {
        return auctionedCompany.value();
    }

    public Player getActingPlayer() {
        // 1. If the hex is not yet selected, the initiator must act
        if (activeBidders != null && activeBidders.size() <= 1) {
            return highestBidder.value();
        }
        if (activeBidders == null || activeBidders.isEmpty())
            return null;
        int index = currentPlayerIndex.value() % activeBidders.size();
        return activeBidders.get(index);
    }

@Override
    public Player getCurrentPlayer() {
        return getActingPlayer();
    }

    @Override
    public boolean setPossibleActions() {
        possibleActions.clear();
        if (activeBidders.size() <= 1) {
            resolveAuction();

            Player winner = highestBidder.value();

            if (auctionType.value() == AuctionType.IPO) {
                if (winner != null && auctionedCompany.value() != null) {
                    // Rule 5.5: Winning player declares if this is for a 2, 5, or 10-share company 
                    List<Integer> validSizes = getValidShareSizes();
                    for (Integer size : validSizes) {
                        possibleActions.add(new SettleIPO_1817(
                                gameManager.getRoot(),
                                auctionedCompany.value().getId(),
                                new java.util.ArrayList<String>(),
                                currentBid.value(),
                                size));
                    }
                    log.info("AUCTION_LOG: Generated " + validSizes.size() + " size options for winner " + winner.getName());
                }
            
            } else {
                // It's an M&A Auction.
                // The winner must now select which of their companies makes the purchase.
                if (winner != null) {
                    for (net.sf.rails.game.PublicCompany comp : gameManager.getRoot().getCompanyManager()
                            .getAllPublicCompanies()) {
                        if (comp.getPresident() != null && comp.getPresident().equals(winner)
                                && !comp.equals(auctionedCompany.value())) {
                            // Predators must not be in the liquidation or acquisition zones ($30 or less)
                            if (comp.getCurrentSpace() != null && comp.getCurrentSpace().getPrice() > 30) {
                                possibleActions
                                        .add(new net.sf.rails.game.specific._1817.action.SelectPurchasingCompany_1817(
                                                gameManager.getRoot(), comp.getId()));
                            }
                        }
                    }
                } else if (auctionType.value() == AuctionType.LIQUIDATION) {
                    // Bank won the liquidation (Bid was $0, no player bid).
                    // We need a specific action to finalize the bank liquidation.
                    possibleActions.add(new net.sf.rails.game.specific._1817.action.SelectPurchasingCompany_1817(
                            gameManager.getRoot(), "BANK"));
                }
            }
            return true;
        }

        // 2. Standard Bidding Phase
        Player currentPlayer = getActingPlayer();
        possibleActions.add(new NullAction(gameManager.getRoot(), NullAction.Mode.PASS));

        int minNextBid = currentBid.value() == 0 && highestBidder.value() == null ? currentBid.value()
                : currentBid.value() + minNextBidIncrement.value();

        // M&A auctions restrict bidding if you don't have an eligible predator company
        boolean canAfford = false;
        if (auctionType.value() != AuctionType.IPO) {
            for (net.sf.rails.game.PublicCompany comp : gameManager.getRoot().getCompanyManager()
                    .getAllPublicCompanies()) {
                if (comp.getPresident() != null && comp.getPresident().equals(currentPlayer)
                        && !comp.equals(auctionedCompany.value())) {
                    if (comp.getCurrentSpace() != null && comp.getCurrentSpace().getPrice() > 30) {
                        canAfford = true;
                        break;
                    }
                }
            }
        } else {
            // IPO bidding is based on purchase power (cash + private companies)
            // Rule 5.5: Players cannot bid more than they can pay
            canAfford = currentPlayer != null && getPurchasePower(currentPlayer) >= minNextBid;
        }
        if (currentPlayer != null && canAfford) {
            int actualMaxBid = 9990;

            if (auctionType.value() == AuctionType.IPO) {
                // IPO bids are limited by the $400 rule AND personal purchasing power
                actualMaxBid = Math.min(400, getPurchasePower(currentPlayer));
            } else if (currentPlayer.equals(auctionedCompany.value().getPresident()) && highestBidder.value() != null) {
                // The president of the company being sold in M&A can only bid +10
                actualMaxBid = currentBid.value() + 10;
            }

            // Only generate the bid action if the player can legally place a higher bid
            if (minNextBid <= actualMaxBid) {
                possibleActions.add(new Bid1817IPO(gameManager.getRoot(), minNextBid, actualMaxBid));
            }

        }

        return true;
    }

    private int getPurchasePower(Player player) {
        if (player == null)
            return 0;
        int power = player.getCash();
        for (net.sf.rails.game.PrivateCompany pc : player.getPortfolioModel().getPrivateCompanies()) {
            power += pc.getBasePrice();
        }
        return power;
    }

    @Override
    public boolean process(PossibleAction action) {
        Player actor = getActingPlayer();

        // During reload, the state tracking the active player can desync.
        // Force sync the internal state to the recorded action's player.
        if (action != null && action.getPlayerName() != null && actor != null && !action.getPlayerName().equals(actor.getName())) {
            for (int i = 0; i < activeBidders.size(); i++) {
                if (activeBidders.get(i).getName().equals(action.getPlayerName())) {
                    actor = activeBidders.get(i);
                    this.currentPlayerIndex.set(i);
                    log.warn("AUCTION_LOG: Actor desync detected. Synced active player to " + actor.getName());
                    break;
                }
            }
        }

        // --- Handle Settlement Action ---
        if (action instanceof SettleIPO_1817) {
            SettleIPO_1817 settle = (SettleIPO_1817) action;
            PublicCompany_1817 comp = auctionedCompany.value();
            Player winner = highestBidder.value();

            log.info("AUCTION_LOG: Settling IPO for {}. Size: {}-share. Cash: ${}. Privates: {}",
                    comp.getId(), settle.getShareSize(), settle.getCashAmount(), settle.getPrivateCompanyIds());

int cashToTransfer = currentBid.value();

            // 1. Transfer Private Companies from Winner to Company FIRST so capacity checks succeed
            for (String pId : settle.getPrivateCompanyIds()) {
                net.sf.rails.game.PrivateCompany pc = gameManager.getRoot().getCompanyManager().getPrivateCompany(pId);
                if (pc != null) {
                    pc.moveTo(comp.getPortfolioModel());
                    cashToTransfer -= pc.getBasePrice();
                    if ("STA80".equals(pId)) {
                        net.sf.rails.common.ReportBuffer.add(this, comp.getId() + " receives an extra station marker from the Train Station.");
                    }
                }
            }

            // 2. Set Company Size (updates internal loan limits and certificates)
            comp.setShareCount(settle.getShareSize());

            // 3. Transfer Cash from Winner to Company Treasury
            if (cashToTransfer > 0) {
                net.sf.rails.game.state.Currency.wire(winner, cashToTransfer, comp);
            }

            // 4. Issue the President's Certificate and set Presidency
            net.sf.rails.game.financial.PublicCertificate presCert = comp.getPresidentsShare();
            if (presCert != null) {
                presCert.moveTo(winner.getPortfolioModel());
            }
            comp.setPresident(winner);



            // 5. Calculate Par Price (Winning Bid / 2)
            int parPrice = currentBid.value() / 2;

            StringBuilder report = new StringBuilder();
            report.append(winner.getName()).append(" wins the auction for ").append(comp.getId())
                  .append(" with a bid of ").append(net.sf.rails.game.financial.Bank.format(this, currentBid.value()))
                  .append(" and establishes it as a ").append(settle.getShareSize()).append("-share company.");
            
            if (!settle.getPrivateCompanyIds().isEmpty()) {
                report.append(" Trades in privates: ").append(String.join(", ", settle.getPrivateCompanyIds())).append(".");
            }
            if (settle.getCashAmount() > 0) {
                report.append(" Pays ").append(net.sf.rails.game.financial.Bank.format(this, settle.getCashAmount())).append(" in cash.");
            }
            report.append(" Par price is set to ").append(net.sf.rails.game.financial.Bank.format(this, parPrice)).append(".");
            net.sf.rails.common.ReportBuffer.add(this, report.toString());


            net.sf.rails.game.financial.StockMarket sm = gameManager.getRoot().getStockMarket();
            net.sf.rails.game.financial.StockSpace startSpace = sm.getStartSpace(parPrice);

            if (startSpace == null) {
                // Robust Fallback: Search for the highest price P such that P <= parPrice
                int bestPriceFound = -1;
                for (int r = 0; r < sm.getNumberOfRows(); r++) {
                    for (int c = 0; c < sm.getNumberOfColumns(); c++) {
                        net.sf.rails.game.financial.StockSpace ss = sm.getStockSpace(r, c);
                        if (ss != null) {
                            int spacePrice = ss.getPrice();
                            if (spacePrice <= parPrice && spacePrice > bestPriceFound) {
                                startSpace = ss;
                                bestPriceFound = spacePrice;
                            }
                        }
                    }
                }

                if (startSpace != null) {
                    log.info("AUCTION_LOG: Exact price ${} not found. Starting at {} (${})",
                            parPrice, startSpace.getId(), startSpace.getPrice());
                }
            }   

            if (startSpace != null) {
                comp.start(startSpace);
            } else {
                log.error("AUCTION_LOG: No StockSpace found for price ${}", parPrice);
            }


            // 6. Float the company BEFORE laying the token so the engine accepts it
            comp.setFloated();

            // 6.1 Buy Mandatory Station Markers
            int tokenCost = 0;
            int tokensBought = 0;
            if (settle.getShareSize() == 5) {
                tokenCost = 50;
                tokensBought = 1;
            } else if (settle.getShareSize() == 10) {
                tokenCost = 150;
                tokensBought = 3;
            }

            if (tokenCost > 0) {
                // Auto-loan BEFORE paying if cash is insufficient
                int loansTaken = 0;
                while (comp.getCash() < tokenCost && comp.getCurrentNumberOfLoans() < settle.getShareSize()) {
                    comp.executeLoan();
                    loansTaken++;
                }

                if (loansTaken > 0) {
                    net.sf.rails.common.ReportBuffer.add(this, "AUTOMATIC ACTION: " + comp.getId() + 
                        " takes " + loansTaken + " loan(s) to cover mandatory IPO station markers.");
                }

                // Pay the fee or liquidate if impossible
                if (comp.getCash() >= tokenCost) {
                    net.sf.rails.game.state.Currency.toBank(comp, tokenCost);
                    net.sf.rails.common.ReportBuffer.add(this, comp.getId() + 
                        " purchases " + tokensBought + " additional station marker(s) for " + 
                        net.sf.rails.game.financial.Bank.format(this, tokenCost) + ".");
                } else {
                    net.sf.rails.common.ReportBuffer.add(this, "LIQUIDATION PENALTY: " + comp.getId() + 
                        " failed to secure funds for mandatory IPO station markers.");
                    net.sf.rails.game.financial.StockSpace liquidationSpace = gameManager.getRoot().getStockMarket().getStartSpace(0);
                    if (liquidationSpace != null) {
                        if (comp.getCurrentSpace() != null) comp.getCurrentSpace().removeToken(comp);
                        liquidationSpace.addToken(comp);
                        comp.setCurrentSpace(liquidationSpace);
                    }
                }
            }

            // 6.2 Register IPO to prevent shorting in current round
            net.sf.rails.game.round.RoundFacade parentRound = gameManager.getInterruptedRound();
            if (parentRound instanceof StockRound_1817) {
                ((StockRound_1817) parentRound).registerIpo(comp.getId());
            }

            // 7. Lay the base token on the map using the specific station number passed
            // from the Stock Round
            net.sf.rails.game.MapHex homeHex = getRoot().getMapManager().getHex(targetHexId.value());
            if (homeHex != null) {
                comp.setHomeHex(homeHex);
                net.sf.rails.game.Stop targetStop = null;

                if (homeHex.getStops() != null) {
                    for (net.sf.rails.game.Stop stop : homeHex.getStops()) {
                        if (stop.getRelatedStationNumber() == targetStationNumber.value()) {
                            targetStop = stop;
                            break;
                        }
                    }
                }

                // Fallback for non-E22 hexes where stationNumber defaults to 0 but might not
                // perfectly match
                if (targetStop == null && homeHex.getStops() != null && !homeHex.getStops().isEmpty()) {
                    for (net.sf.rails.game.Stop stop : homeHex.getStops()) {
                        if (stop.hasTokenSlotsLeft()) {
                            targetStop = stop;
                            break;
                        }
                    }
                }

                if (targetStop != null) {
                    comp.setHomeCityNumber(targetStop.getRelatedStationNumber());
                    homeHex.layBaseToken(comp, targetStop);
                    net.sf.rails.common.ReportBuffer.add(this, comp.getId() + " lays its home token on " + homeHex.getId() + ".");
                }
            }

            // 8. Close the Auction Round
            gameManager.nextRound(this);
            return true;

        }

        // --- Handle Bidding Actions ---
        if (action instanceof NullAction && ((NullAction) action).getMode() == NullAction.Mode.PASS) {
            log.info("AUCTION_LOG: Player {} passed.", actor.getName());
            net.sf.rails.common.ReportBuffer.add(this, actor.getName() + " passes.");
            int index = activeBidders.indexOf(actor);
            activeBidders.remove(actor);
            if (!activeBidders.isEmpty()) {
                currentPlayerIndex.set(index % activeBidders.size());
            }
            advanceToNextValidBidder();

            return true;
        }

        if (action instanceof net.sf.rails.game.specific._1817.action.SelectPurchasingCompany_1817) {
            net.sf.rails.game.specific._1817.action.SelectPurchasingCompany_1817 spc = (net.sf.rails.game.specific._1817.action.SelectPurchasingCompany_1817) action;
            String predatorId = spc.getCompanyId();
            PublicCompany_1817 predator = null;

            if (!"BANK".equals(predatorId)) {
                predator = (PublicCompany_1817) gameManager.getRoot().getCompanyManager().getPublicCompany(predatorId);
            }

            executeSale(auctionedCompany.value(), predator, currentBid.value(), auctionType.value());

            // Return to the M&A Round
            gameManager.nextRound(this);
            return true;
        }

        if (action instanceof Bid1817IPO) {
            int amount = ((Bid1817IPO) action).getBidAmount();
            log.info("AUCTION_LOG: Player {} BID ${}.", actor.getName(), amount);
net.sf.rails.common.ReportBuffer.add(this, actor.getName() + " bids $" + amount + ".");
            this.currentBid.set(amount);
            this.highestBidder.set(actor);

            int index = activeBidders.indexOf(actor);
            this.currentPlayerIndex.set((index + 1) % activeBidders.size());
            advanceToNextValidBidder();
            return true;
        }

        return super.process(action);
    }

    private void resolveAuction() {
        Player winner = highestBidder.value();

        if (winner != null) {
            net.sf.rails.common.ReportBuffer.add(this, winner.getName() + " wins the auction with a bid of $" + currentBid.value() + ".");
        } else {
            net.sf.rails.common.ReportBuffer.add(this, "Auction ends with no player bids.");
        }

        log.info("AUCTION_LOG: Auction finished. Waiting for settlement from {}",
                (winner != null ? winner.getName() : "None"));
    }

private void advanceToNextValidBidder() {
        // IPO bid increments are $5 [cite: 415, 507]
        int minNextBid = currentBid.value() + minNextBidIncrement.value();

        while (activeBidders.size() > 1) {
            int index = currentPlayerIndex.value() % activeBidders.size();
            Player candidate = activeBidders.get(index);

            // A player is valid if they can afford the next minimum increment 
            boolean canBid = getPurchasePower(candidate) >= minNextBid;
            
            // Auto-resolve IPO auctions if the next minimum bid exceeds the $400 limit
            if (auctionType.value() == AuctionType.IPO && minNextBid > 400) {
                canBid = false;
            }

            if (canBid) {
                return;
            }

            activeBidders.remove(index);

            if (!activeBidders.isEmpty()) {
                currentPlayerIndex.set(index % activeBidders.size());
            }
        }
    }

    private void executeSale(PublicCompany_1817 target, PublicCompany_1817 predator, int finalBid, AuctionType type) {
        log.info(">>> Executing M&A Sale: {} bought by {} for ${}", target.getId(),
                (predator != null ? predator.getId() : "BANK"), finalBid);

        // 1. Treasury Processing (Shares to Open Market)
        int treasuryShares = 0;
        for (net.sf.rails.game.financial.PublicCertificate cert : new java.util.ArrayList<>(target.getCertificates())) {
            if (cert.getOwner() == target && !cert.isPresidentShare()) {
                cert.moveTo(gameManager.getRoot().getBank().getPool());
                treasuryShares += (cert.getShare() / target.getShareUnit());
            }
        }

        // Friendly Sale Treasury Compensation 
        if (type == AuctionType.FRIENDLY_SALE && treasuryShares > 0) {
            int infusion = treasuryShares * target.getMarketPrice();
            target.setCash_AI(target.getCash() + infusion);
            log.info("M&A SALE: Friendly sale treasury compensation: ${}", infusion);
        }

        // Snapshot pre-transfer values
        int targetCash = target.getCash();
        int targetLoans = target.getCurrentNumberOfLoans();

        // 2. Predator Transfer (Trains, Stations, Privates, Cash, Loans) [cite: 835]
        if (predator != null) {
            predator.setCash_AI(predator.getCash() - finalBid); // Predator pays Bank [cite: 836]
            predator.transferAssetsFrom(target);

            if (targetLoans > 0) {
                predator.addLoans(targetLoans);
                target.addLoans(-targetLoans); // Clear from target

                // Move predator stock left 1 space per loan 
                for (int i = 0; i < targetLoans; i++) {
                    // Note: Ensure your 1817 StockMarket implementation handles this move.
                    // gameManager.getRoot().getStockMarket().moveLeft(predator);
                }
            }
        } else {

            // Bank Liquidation: Assets (Trains, Tokens, Privates) are discarded.
            for (net.sf.rails.game.BaseToken token : new java.util.ArrayList<>(target.getLaidBaseTokens())) {
                // Moving the token back to the company automatically removes it from the MapHex
                // Stop
                token.moveTo(target);
            }

            for (net.sf.rails.game.Train train : new java.util.ArrayList<>(target.getTrains())) {
                train.moveTo(gameManager.getRoot().getBank().getPool()); // Discard train
            }

            for (net.sf.rails.game.PrivateCompany pc : new java.util.ArrayList<>(target.getPrivates())) {
                pc.setClosed(); // Close private company
            }

            target.setCash_AI(0); // Bank takes the cash for debt settlement
        }

        // 3. Debt Resolution (Liquidation specific)
        int surplusForShareholders = finalBid;
        if (type == AuctionType.LIQUIDATION) {
            int totalDebt = targetLoans * 100;
            int availableFunds = targetCash + finalBid;

            if (availableFunds >= totalDebt) {
                surplusForShareholders = availableFunds - totalDebt;
                log.info("M&A SALE: Liquidation debts cleared. Surplus: ${}", surplusForShareholders);
            } else {
                surplusForShareholders = 0;
                int shortfall = totalDebt - availableFunds;
                Player president = target.getPresident();
                log.warn("M&A SALE: Liquidation shortfall of ${}. President {} must pay.", shortfall,
                        president.getName());
                // Deduct from president. If cash goes negative, Cash Crisis handles it later.
                president.setCash_AI(president.getCash() - shortfall);
            }
        }

        // 4. Shareholder and Short Seller Settlement
        int shareCount = target.getShareCount();
        int payoutPerShare = surplusForShareholders / shareCount;

        for (Player p : gameManager.getRoot().getPlayerManager().getPlayers()) {
            int sharesOwned = 0;

            List<net.sf.rails.game.financial.PublicCertificate> certs = new java.util.ArrayList<>(
                    p.getPortfolioModel().getCertificates(target));
            for (net.sf.rails.game.financial.PublicCertificate cert : certs) {
                // TODO: Implement specific ShortCertificate_1817 logic here once the class is
                // created.
                // if (cert instanceof
                // net.sf.rails.game.specific._1817.financial.ShortCertificate_1817) {
                // int shortShares = cert.getShare() / target.getShareUnit();
                // int debt = shortShares * payoutPerShare;
                // p.setCash_AI(p.getCash() - debt);
                // cert.moveTo(gameManager.getRoot().getBank().getPool());
                // continue;
                // }

                // Assume all remaining certs are standard long shares for now
                sharesOwned += cert.getShare() / target.getShareUnit();
                cert.moveTo(gameManager.getRoot().getBank().getPool()); // Return to bank
            }

            if (sharesOwned > 0) {
                p.setCash_AI(p.getCash() + (sharesOwned * payoutPerShare));
                log.info("M&A SALE: Paid {} ${} for {} shares.", p.getName(), sharesOwned * payoutPerShare,
                        sharesOwned);
            }
        }
        // 5. Close Company [cite: 852]
        target.setClosed();
    }


    private List<Integer> getValidShareSizes() {
        List<Integer> sizes = new ArrayList<>();
        net.sf.rails.game.PhaseManager pm = gameManager.getRoot().getPhaseManager();

        if (pm == null || !pm.hasReachedPhase("3")) {
            sizes.add(2); // Phase 2: 2-share only
        } else if (!pm.hasReachedPhase("4")) {
            sizes.add(2);
            sizes.add(5); // Phase 3: 2-share or 5-share
        } else if (!pm.hasReachedPhase("5")) {
            sizes.add(5); // Phase 4: 5-share only
        } else if (!pm.hasReachedPhase("6")) {
            sizes.add(5);
            sizes.add(10); // Phase 5: 5-share or 10-share
        } else {
            sizes.add(10); // Phases 6+: 10-share only
        }
        return sizes;
    }


}