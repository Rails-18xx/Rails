package net.sf.rails.game.specific._1817;

import net.sf.rails.game.Round;
import net.sf.rails.game.GameManager;
import net.sf.rails.game.PublicCompany;
import net.sf.rails.game.state.GenericState;
import net.sf.rails.game.state.ArrayListState;
import net.sf.rails.game.financial.StockSpace;
import net.sf.rails.game.specific._1817.action.TakeLoans_1817;
import net.sf.rails.game.financial.Bank;
import net.sf.rails.game.financial.PublicCertificate;
import net.sf.rails.game.financial.BankPortfolio;
import rails.game.action.PossibleAction;
import rails.game.action.NullAction;
import net.sf.rails.game.Player;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.List;

public class MergerAndAcquisitionRound_1817 extends Round {

    private static final Logger log = LoggerFactory.getLogger(MergerAndAcquisitionRound_1817.class);

    public enum MaAStep {
        START,
        COMPANY_ACTIONS,
        POST_MERGER_PRESIDENT,
        POST_MERGER_PLAYERS,
        LOAN_TAKING, // New: President decides on loans
        TOKEN_PURCHASE, // New: Automatic logic for mandatory tokens
        SALES_LIQUIDATION, // Forced auction for red-zone
        SALES_ACQUISITION, // Forced offering for gray-zone
        SALES_FRIENDLY, // Optional offering for white/green
        SALES_AUCTION, // The bidding process
        SALES_SELECT_BUYER, // President selects which company makes the purchase
        NEXT_COMPANY,
        DISCARD_TRAINS, // Handles train limit enforcement
        FINISHED
    }

    private final GenericState<MaAStep> currentStep;
    private final GenericState<MaAStep> stepToResume;
    private final GenericState<PublicCompany> operatingCompany;
    private final ArrayListState<PublicCompany> operatingCompanies;
    private final GenericState<Integer> companyIndex;
    private final GenericState<Integer> currentPlayerIndex;
    private final GenericState<Integer> playersProcessed;
    private final GameManager gameManagerRef;

    private final ArrayListState<String> validMergerPairs;
    private final ArrayListState<String> mergedThisRound;
    private final ArrayListState<String> startedAboveAcquisition;

    private final GenericState<Integer> highestBid;
    private final GenericState<net.sf.rails.game.Player> highestBiddingPlayer;
    private final ArrayListState<net.sf.rails.game.Player> activeBidders;
    private final GenericState<Integer> auctionPlayerIndex;
    private final GenericState<Integer> mandatoryTokenCost;

    public int getHighestBid() {
        return highestBid.value();
    }

    public net.sf.rails.game.Player getHighestBiddingPlayer() {
        return highestBiddingPlayer.value();
    }

    public PublicCompany getOperatingCompany() {
        return operatingCompany.value();
    }

    public net.sf.rails.game.Player getActingPlayer() {
        MaAStep step = currentStep.value();
        if (step == MaAStep.COMPANY_ACTIONS || step == MaAStep.POST_MERGER_PRESIDENT ||
                step == MaAStep.LOAN_TAKING || step == MaAStep.SALES_FRIENDLY || step == MaAStep.DISCARD_TRAINS) {
            return operatingCompany.value() != null ? operatingCompany.value().getPresident() : null;
        } else if (step == MaAStep.POST_MERGER_PLAYERS) {
            List<net.sf.rails.game.Player> players = gameManagerRef.getRoot().getPlayerManager().getPlayers();
            int idx = currentPlayerIndex.value();
            if (idx >= 0 && idx < players.size()) {
                return players.get(idx);
            }
        } else if (step == MaAStep.SALES_SELECT_BUYER) {
            return highestBiddingPlayer.value();
        } else if (step == MaAStep.SALES_AUCTION) {
            if (activeBidders.isEmpty())
                return null;
            int idx = auctionPlayerIndex.value();
            if (idx >= activeBidders.size())
                idx = 0;
            return activeBidders.get(idx);
        }
        return null;
    }

    @Override
    public Player getCurrentPlayer() {
        return getActingPlayer();
    }

    public MaAStep getCurrentStep() {
        return currentStep.value();
    }

    public MergerAndAcquisitionRound_1817(GameManager parent, String id) {
        super(parent, id);
        this.gameManagerRef = parent;
        currentStep = new GenericState<>(this, "currentStep", MaAStep.START);
        stepToResume = new GenericState<>(this, "stepToResume", MaAStep.START);
        operatingCompany = new GenericState<>(this, "operatingCompany", null);
        operatingCompanies = new ArrayListState<>(this, "operatingCompanies");
        companyIndex = new GenericState<>(this, "companyIndex", 0);
        currentPlayerIndex = new GenericState<>(this, "currentPlayerIndex", 0);
        playersProcessed = new GenericState<>(this, "playersProcessed", 0);
        validMergerPairs = new ArrayListState<>(this, "validMergerPairs");
        mergedThisRound = new ArrayListState<>(this, "mergedThisRound");
        highestBid = new GenericState<>(this, "highestBid", 0);
        highestBiddingPlayer = new GenericState<>(this, "highestBiddingPlayer", null);
        activeBidders = new ArrayListState<>(this, "activeBidders");
        auctionPlayerIndex = new GenericState<>(this, "auctionPlayerIndex", 0);
        mandatoryTokenCost = new GenericState<>(this, "mandatoryTokenCost", 0);
        startedAboveAcquisition = new ArrayListState<>(this, "startedAboveAcquisition");
    }

    public void start() {
        operatingCompanies.clear();
        startedAboveAcquisition.clear();

        List<PublicCompany> sortedComps = gameManagerRef.getCompaniesInRunningOrder();
        for (PublicCompany comp : sortedComps) {
            if (comp.hasFloated() && !comp.isClosed()) {
                operatingCompanies.add(comp);
                if (comp.getCurrentSpace() != null && comp.getCurrentSpace().getPrice() > 30) {
                    startedAboveAcquisition.add(comp.getId());
                }
            }
        }

        calculateValidMergerPairs();
        currentStep.set(MaAStep.NEXT_COMPANY);
        processNextCompany();
    }

    private void calculateValidMergerPairs() {
        validMergerPairs.clear();
        List<PublicCompany> companies = operatingCompanies.view();
        for (int i = 0; i < companies.size(); i++) {
            for (int j = 0; j < companies.size(); j++) {
                if (i == j)
                    continue;
                PublicCompany c1 = companies.get(i);
                PublicCompany c2 = companies.get(j);

                if (mergedThisRound.contains(c1.getId()) || mergedThisRound.contains(c2.getId()))
                    continue;

                if (c1.getCurrentSpace() == null || c1.getCurrentSpace().getPrice() <= 30 ||
                        c2.getCurrentSpace() == null || c2.getCurrentSpace().getPrice() <= 30) {
                    continue;
                }

                int shares1 = ((PublicCompany_1817) c1).getShareCount();
                int shares2 = ((PublicCompany_1817) c2).getShareCount();

                boolean isTwoShare = (shares1 == 2 && shares2 == 2);
                boolean isFiveShare = (shares1 == 5 && shares2 == 5);
                boolean samePresident = (c1.getPresident() != null && c1.getPresident().equals(c2.getPresident()));

                if ((isTwoShare || isFiveShare) && samePresident) {
                    validMergerPairs.add(c1.getId() + "," + c2.getId());
                    if (i < j) {
                    }
                }

            }
        }
    }

    private void processNextCompany() {
        int index = companyIndex.value();
        while (index < operatingCompanies.size()) {
            PublicCompany comp = operatingCompanies.get(index);

            boolean canMerge = false;
            for (String pair : validMergerPairs.view()) {
                if (pair.startsWith(comp.getId() + ",")) {
                    canMerge = true;
                    break;
                }
            }

            boolean canConvert = false;
            if (comp instanceof PublicCompany_1817 && comp.getCurrentSpace() != null
                    && comp.getCurrentSpace().getPrice() > 30) {
                int shares = ((PublicCompany_1817) comp).getShareCount();
                if (shares == 2 || shares == 5) {
                    canConvert = true;
                }
            }

            boolean canLoan = false;
            if (comp instanceof PublicCompany_1817
                    && comp.getNumberOfBonds() < ((PublicCompany_1817) comp).getShareCount()) {
                canLoan = true;
            }

            if (mergedThisRound.contains(comp.getId()) || (!canMerge && !canConvert && !canLoan)) {

                index++;
            } else {
                break;
            }
        }
        companyIndex.set(index);

        if (index >= operatingCompanies.size()) {
            startSalesPhase();

            return;
        }

        operatingCompany.set(operatingCompanies.get(index));
        currentStep.set(MaAStep.COMPANY_ACTIONS);
        setPossibleActions();
    }

    private void addPassAction(PublicCompany company) {
        if (company.getPresident() != null) {
            possibleActions.add(new NullAction(gameManagerRef.getRoot(), NullAction.Mode.PASS));
        }
    }

    @Override
    public boolean process(PossibleAction action) {

        if (action instanceof NullAction && ((NullAction) action).getMode() == NullAction.Mode.PASS) {
            if (currentStep.value() == MaAStep.COMPANY_ACTIONS) {
                companyIndex.set(companyIndex.value() + 1);
                currentStep.set(MaAStep.NEXT_COMPANY);
                processNextCompany();
                return true;
            } else if (currentStep.value() == MaAStep.POST_MERGER_PRESIDENT) {
                initPostMergerPlayers();
                return true;

            } else if (currentStep.value() == MaAStep.POST_MERGER_PLAYERS) {
                advancePostMergerPlayer();
                return true;
            } else if (currentStep.value() == MaAStep.LOAN_TAKING) {
                currentStep.set(MaAStep.TOKEN_PURCHASE);
                executeMandatoryTokenPurchase();
                return true;

            } else if (currentStep.value() == MaAStep.SALES_AUCTION) {
                net.sf.rails.game.Player passingPlayer = activeBidders.get(auctionPlayerIndex.value());

                net.sf.rails.common.ReportBuffer.add(this, passingPlayer.getName() + " passes.");
                activeBidders.remove(auctionPlayerIndex.value());

                if (activeBidders.isEmpty()
                        || (activeBidders.size() == 1 && activeBidders.get(0).equals(highestBiddingPlayer.value()))) {
                    finalizeAuction();
                } else {
                    if (auctionPlayerIndex.value() >= activeBidders.size()) {
                        auctionPlayerIndex.set(0);
                    }
                    setPossibleActions();
                }
                return true;
            } else if (currentStep.value() == MaAStep.SALES_FRIENDLY) {
                companyIndex.set(companyIndex.value() + 1);
                processNextSale();
                return true;
            }
        } else if (action instanceof NullAction && ((NullAction) action).getMode() == NullAction.Mode.DONE) {
            if (currentStep.value() == MaAStep.POST_MERGER_PRESIDENT) {
                initPostMergerPlayers();
                return true;
            } else if (currentStep.value() == MaAStep.POST_MERGER_PLAYERS) {
                advancePostMergerPlayer();
                return true;
            } else if (currentStep.value() == MaAStep.LOAN_TAKING) {

                currentStep.set(MaAStep.TOKEN_PURCHASE);
                executeMandatoryTokenPurchase();
                return true;

            } else if (currentStep.value() == MaAStep.SALES_FRIENDLY) {

                int sharePrice = operatingCompany.value().getCurrentSpace().getPrice();
                int shareCount = ((PublicCompany_1817) operatingCompany.value()).getShareCount();
                int minBid = sharePrice * shareCount;

                startAuction(operatingCompany.value(), minBid);

                return true;
            }

        } else if (action instanceof MergeCompanies_1817) {
            MergeCompanies_1817 mergeAction = (MergeCompanies_1817) action;

            PublicCompany initiator = mergeAction.getInitiatingCompany();
            PublicCompany target = mergeAction.getTargetCompany();

            int shares = ((PublicCompany_1817) initiator).getShareCount();
            if (shares == 2) {
                execute2ShareMerger(initiator, target);
            } else if (shares == 5) {
                execute5ShareMerger(initiator, target);
            }
            return true;
        } else if (action instanceof net.sf.rails.game.specific._1817.action.OfferCompanyForSale_1817) {
            int minBid = operatingCompany.value().getMarketPrice()
                    * ((PublicCompany_1817) operatingCompany.value()).getShareCount();
            net.sf.rails.common.ReportBuffer.add(this,
                    "--- FRIENDLY SALE OFFERING: " + operatingCompany.value().getId() + " ---");
            net.sf.rails.common.ReportBuffer.add(this,
                    "Pre-Event State:\n" + buildCompanyStateReport(operatingCompany.value()));
            startAuction(operatingCompany.value(), minBid);
            startAuction(operatingCompany.value(), minBid);
            return true;

        } else if (action instanceof net.sf.rails.game.specific._1817.action.ConvertCompany_1817) {
            net.sf.rails.game.specific._1817.action.ConvertCompany_1817 convertAction = (net.sf.rails.game.specific._1817.action.ConvertCompany_1817) action;
            PublicCompany comp = convertAction.getCompany();
            executeConversion(comp);
            return true;
        } else if (action instanceof rails.game.action.BuyCertificate) {
            PublicCompany comp = operatingCompany.value();
            net.sf.rails.game.Player player;
            if (currentStep.value() == MaAStep.POST_MERGER_PRESIDENT) {
                player = comp.getPresident();
            } else {
                player = gameManagerRef.getRoot().getPlayerManager().getPlayers().get(currentPlayerIndex.value());
            }

            for (net.sf.rails.game.financial.PublicCertificate cert : comp.getCertificates()) {
                if (cert.getOwner() == comp && !cert.isPresidentShare()) {

                    net.sf.rails.game.state.Currency.wire(player, comp.getMarketPrice(), comp);

                    cert.moveTo(player.getPortfolioModel());

                    net.sf.rails.common.ReportBuffer.add(this,
                            player.getName() + " buys 1 treasury share of " + comp.getId() + " for $"
                                    + comp.getMarketPrice() + ".");
                    break;
                }
            }

            if (currentStep.value() == MaAStep.POST_MERGER_PLAYERS) {
                advancePostMergerPlayer();
            } else {
                if (hasValidTreasuryBuy(comp, player)) {
                    setPossibleActions();
                } else {
                    initPostMergerPlayers();
                }
            }
            return true;

        } else if (action instanceof rails.game.action.SellShares) {
            rails.game.action.SellShares sellAction = (rails.game.action.SellShares) action;
            PublicCompany comp = operatingCompany.value();
            net.sf.rails.game.Player player = gameManagerRef.getRoot().getPlayerManager().getPlayers()
                    .get(currentPlayerIndex.value());
            int numberToSell = sellAction.getNumber();

            int sold = 0;
            for (net.sf.rails.game.financial.PublicCertificate cert : new java.util.ArrayList<>(
                    player.getPortfolioModel().getCertificates())) {
                if (sold >= numberToSell)
                    break;
                if (cert.getCompany() == comp && !cert.isPresidentShare()) {
                    cert.moveTo(gameManagerRef.getRoot().getBank().getPool());
                    net.sf.rails.game.state.Currency.wire(gameManagerRef.getRoot().getBank(), comp.getMarketPrice(),
                            player);
                    sold++;
                }
            }

            if (sold > 0) {
                net.sf.rails.common.ReportBuffer.add(this,
                        player.getName() + " sells " + sold + " share(s) of " + comp.getId()
                                + " to the open market for $" + (comp.getMarketPrice() * sold) + ".");
            }

            if (currentStep.value() == MaAStep.POST_MERGER_PLAYERS)
                advancePostMergerPlayer();
            return true;
        } else if (action instanceof TakeLoans_1817) {

            TakeLoans_1817 loanAction = (TakeLoans_1817) action;
            PublicCompany comp = operatingCompany.value();

            if (comp instanceof PublicCompany_1817) {
                ((PublicCompany_1817) comp).executeLoan();
            }
            net.sf.rails.common.ReportBuffer.add(this, comp.getId() + " takes a loan.");

            if (currentStep.value() == MaAStep.COMPANY_ACTIONS || currentStep.value() == MaAStep.LOAN_TAKING) {
                setPossibleActions();
            } else {
                currentStep.set(MaAStep.TOKEN_PURCHASE);
                executeMandatoryTokenPurchase();
            }
            return true;
        } else if (action instanceof net.sf.rails.game.specific._1817.action.BidOnCompany_1817) {
            net.sf.rails.game.specific._1817.action.BidOnCompany_1817 bidAction = (net.sf.rails.game.specific._1817.action.BidOnCompany_1817) action;
            int bidAmount = bidAction.getBidAmount();
            net.sf.rails.game.Player bidder = activeBidders.get(auctionPlayerIndex.value());

            String saleType = "";
            int price = operatingCompany.value().getMarketPrice();
            if (price == 0)
                saleType = "liquidation";
            else if (price <= 30)
                saleType = "acquisition";
            else
                saleType = "friendly sale";

            net.sf.rails.common.ReportBuffer.add(this, bidder.getName() + " bids $" + bidAmount +
                    " for " + operatingCompany.value().getId() + " (" + saleType + ").");

            highestBid.set(bidAmount);
            highestBiddingPlayer.set(bidder);

            auctionPlayerIndex.set((auctionPlayerIndex.value() + 1) % activeBidders.size());

            if (activeBidders.size() == 1) {
                finalizeAuction();
            } else {
                setPossibleActions();
            }
            return true;
        } else if (action instanceof net.sf.rails.game.specific._1817.action.SelectPurchasingCompany_1817) {
            net.sf.rails.game.specific._1817.action.SelectPurchasingCompany_1817 selectAction = (net.sf.rails.game.specific._1817.action.SelectPurchasingCompany_1817) action;
            PublicCompany target = operatingCompany.value();
            PublicCompany predator = gameManagerRef.getRoot().getCompanyManager()
                    .getPublicCompany(selectAction.getCompanyId());
            int finalBid = highestBid.value();

            executeSale(target, predator, finalBid);
            return true;

        } else if (action instanceof rails.game.action.DiscardTrain) {
            rails.game.action.DiscardTrain discardAction = (rails.game.action.DiscardTrain) action;
            PublicCompany comp = operatingCompany.value();

            // 1. Move train using base class helper (fixes 'getPortfolio' error and
            // 'Bank/Pool' crash)
            executeDiscardTrain(discardAction);

            // 2. Check if limit is now satisfied
            if (!enforceTrainLimit(comp)) {
                net.sf.rails.common.ReportBuffer.add(this, "Final Post-Event State:\n" + buildCompanyStateReport(comp));

                // 3. Resume the correct loop based on saved state
                if (stepToResume.value() == MaAStep.POST_MERGER_PRESIDENT) {
                    // Merger path: continue to share trading
                    calculateValidMergerPairs();
                    startPostMergerPhase();
                } else {
                    // Sales path: advance to next company in the reverse operating order
                    companyIndex.set(companyIndex.value() + 1);
                    processNextSale();
                }
            } else {
                // Still over limit: enforceTrainLimit() has already re-generated buttons
                setPossibleActions();
            }

            return true;
        }

        return false;
    }

    private void executeConversion(PublicCompany comp) {
        log.info(">>> Executing Conversion for: " + comp.getId());
        PublicCompany_1817 comp1817 = (PublicCompany_1817) comp;

        // Capture pre-conversion state for accurate PBM reporting
        String preReport = buildCompanyStateReport(comp);

        int currentShares = comp1817.getShareCount();
        int currentMarkers = comp.getBaseTokens().nbAllTokens();
        int cost = 0;

        if (currentShares == 2) {
            net.sf.rails.common.ReportBuffer.add(this,
                    "--- CONVERSION EVENT: " + comp.getId() + " converts from a 2-share to a 5-share company ---");
            net.sf.rails.common.ReportBuffer.add(this, "Pre-Conversion State:\n" + preReport);
            cost = 50;
        } else if (currentShares == 5) {
            net.sf.rails.common.ReportBuffer.add(this,
                    "--- CONVERSION EVENT: " + comp.getId() + " converts from a 5-share to a 10-share company ---");
            net.sf.rails.common.ReportBuffer.add(this, "Pre-Conversion State:\n" + preReport);
            if (currentMarkers <= 6) {
                cost = 100;
            } else if (currentMarkers == 7) {
                cost = 50;
            }
        }

        // 1. Record old ownership of normal and short shares
        java.util.List<net.sf.rails.game.state.Owner> normalOwners = new java.util.ArrayList<>();
        java.util.List<net.sf.rails.game.state.Owner> shortOwners = new java.util.ArrayList<>();
        for (net.sf.rails.game.financial.PublicCertificate cert : comp.getCertificates()) {
            if (!cert.isPresidentShare()) {
                if (cert instanceof net.sf.rails.game.specific._1817.ShortCertificate) {
                    net.sf.rails.game.state.Owner owner = cert.getOwner();
                    if (owner != null && owner != gameManagerRef.getRoot().getBank().getUnavailable() && owner != gameManagerRef.getRoot().getBank().getOSI()) {
                        shortOwners.add(owner);
                    }
                } else {
                    net.sf.rails.game.state.Owner owner = cert.getOwner();
                    if (owner != null && owner != comp) {
                        int shareUnits = cert.getShare() / comp.getShareUnit();
                        for (int i = 0; i < shareUnits; i++) {
                            normalOwners.add(owner);
                        }
                    }
                }
            }
        }

        // 2. Adjust share counts
        if (currentShares == 2) {
            comp1817.setShareCount(5);
            log.info("CONVERSION: Upgraded " + comp.getId() + " from 2-share to 5-share.");
        } else if (currentShares == 5) {
            comp1817.setShareCount(10);
            log.info("CONVERSION: Upgraded " + comp.getId() + " from 5-share to 10-share.");
        }

        // 3. Restore ownership using the newly generated certificates
        java.util.List<net.sf.rails.game.financial.PublicCertificate> availableCerts = new java.util.ArrayList<>();
        java.util.List<net.sf.rails.game.financial.PublicCertificate> availableShorts = new java.util.ArrayList<>();
        net.sf.rails.game.financial.BankPortfolio unavailableBank = gameManagerRef.getRoot().getBank().getUnavailable();

        for (net.sf.rails.game.financial.PublicCertificate cert : comp.getCertificates()) {
            if (!cert.isPresidentShare()) {
                if (cert instanceof net.sf.rails.game.specific._1817.ShortCertificate) {
                    if (cert.getOwner() == unavailableBank || cert.getOwner() == gameManagerRef.getRoot().getBank().getOSI()) {
                        availableShorts.add(cert);
                    }
                } else if (cert.getOwner() == comp) {
                    availableCerts.add(cert);
                }
            }
        }

        int certIdx = 0;
        for (net.sf.rails.game.state.Owner owner : normalOwners) {
            if (certIdx < availableCerts.size()) {
                net.sf.rails.game.financial.PublicCertificate newCert = availableCerts.get(certIdx++);
                if (owner instanceof net.sf.rails.game.Player) {
                    newCert.moveTo(((net.sf.rails.game.Player) owner).getPortfolioModel());
                } else if (owner instanceof net.sf.rails.game.financial.Bank) {
                    newCert.moveTo(gameManagerRef.getRoot().getBank().getPool());
                } else if (owner instanceof net.sf.rails.game.PublicCompany) {
                    newCert.moveTo(((net.sf.rails.game.PublicCompany) owner).getPortfolioModel());
                }
            }
        }

        int shortIdx = 0;
        for (net.sf.rails.game.state.Owner owner : shortOwners) {
            if (shortIdx < availableShorts.size()) {
                net.sf.rails.game.financial.PublicCertificate newShort = availableShorts.get(shortIdx++);
                if (owner instanceof net.sf.rails.game.Player) {
                    newShort.moveTo(((net.sf.rails.game.Player) owner).getPortfolioModel());
                }
            }
        }

        // 4. Execute Mandatory Token Purchase Inline
        if (cost > 0) {
            int loansTaken = 0;
            while (comp.getCash() < cost && comp1817.getNumberOfBonds() < comp1817.getShareCount()) {
                comp1817.setNumberOfBonds(comp1817.getNumberOfBonds() + 1);
                net.sf.rails.game.financial.Bank bank = gameManagerRef.getRoot().getBank();
                comp1817.addCashFromBank(100, bank);
                loansTaken++;
                
                net.sf.rails.game.financial.StockMarket market = gameManagerRef.getRoot().getStockMarket();
                if (market instanceof StockMarket_1817) {
                    ((StockMarket_1817) market).moveLeftOrDown(comp, 1);
                }
            }
            if (loansTaken > 0) {
                net.sf.rails.common.ReportBuffer.add(this,
                        comp.getId() + " automatically takes " + loansTaken + " loan(s) to fund mandatory station markers.");
            }

            if (comp.getCash() >= cost) {
                net.sf.rails.game.state.Currency.wire(comp, cost, gameManagerRef.getRoot().getBank());
                net.sf.rails.common.ReportBuffer.add(this,
                        comp.getId() + " purchases mandatory station markers for $" + cost + ".");
            }
        }

        // Mark as merged/converted this round so it cannot merge again
        mergedThisRound.add(comp.getId());

        // Post-Conversion Report
        net.sf.rails.common.ReportBuffer.add(this,
                "Post-Conversion State:\n" + buildCompanyStateReport(comp));

        // Initiate Post-Merger Phase
        calculateValidMergerPairs();
        startPostMergerPhase();
    }

    private void execute2ShareMerger(PublicCompany initiator, PublicCompany target) {
        log.info(">>> Executing Merger: " + initiator.getId() + " absorbs " + target.getId());

        // Capture pre-merger state for accurate PBM reporting
        String initiatorPreReport = buildCompanyStateReport(initiator);
        String targetPreReport = buildCompanyStateReport(target);

        net.sf.rails.common.ReportBuffer.add(this,
                "--- MERGER EVENT: " + initiator.getId() + " (Survivor) absorbs " + target.getId()
                        + " (Disappearing) to form a 5-share company ---");
        net.sf.rails.common.ReportBuffer.add(this, "Pre-Merger State (Survivor):\n" + initiatorPreReport);
        net.sf.rails.common.ReportBuffer.add(this, "Pre-Merger State (Disappearing):\n" + targetPreReport);

        int initialCash = initiator.getCash();
        int targetCash = target.getCash();
        int initialTrains = initiator.getTrains().size();
        int targetTrains = target.getTrains().size();

        // 1. Core Asset Consolidation (Cash, Trains, Privates)
        initiator.transferAssetsFrom(target);

        log.info("MERGER ASSETS: " + initiator.getId() + " cash changed from " + initialCash + " to "
                + initiator.getCash() + " (Absorbed: " + targetCash + ")");
        log.info("MERGER ASSETS: " + initiator.getId() + " trains changed from " + initialTrains + " to "
                + initiator.getTrains().size() + " (Absorbed: " + targetTrains + ")");

        // 2. Loan Consolidation
        if (initiator.canLoan() && target.canLoan()) {
            int targetLoans = target.getCurrentNumberOfLoans();
            log.info("MERGER LOANS: Transferring " + targetLoans + " loans from " + target.getId() + " to "
                    + initiator.getId());
            initiator.addLoans(targetLoans);
            target.addLoans(-targetLoans);
        }

        // 3. Bond Consolidation
        int targetBonds = target.getNumberOfBonds();
        log.info("MERGER BONDS: Transferring " + targetBonds + " bonds from " + target.getId() + " to "
                + initiator.getId());
        initiator.setNumberOfBonds(initiator.getNumberOfBonds() + targetBonds);
        target.setNumberOfBonds(0);

        // 4. Share Structure Update (Moved BEFORE Token Conversion)
        ((PublicCompany_1817) initiator).setShareCount(10);

        // 5. Station Marker Conversion

        // 5.1 Market Value Adjustment and UI Marker Move
        StockSpace oldSpace = initiator.getCurrentSpace();

        int rawSum = initiator.getMarketPrice() + target.getMarketPrice();
        StockMarket_1817 sm = (StockMarket_1817) gameManagerRef.getRoot().getStockMarket();
        StockSpace newSpace = sm.getFloorSpace(rawSum);
        int actualNewPrice = newSpace != null ? newSpace.getPrice() : rawSum;

        // Explicitly remove and log the target's marker removal
        StockSpace targetSpace = target.getCurrentSpace();
        if (targetSpace != null) {
            targetSpace.removeToken(target);
            target.setCurrentSpace(null);
            net.sf.rails.common.ReportBuffer.add(this,
                    "MARKET: " + target.getId() + " marker removed from the stock market.");
        }

        if (newSpace != null) {
            if (oldSpace != null) {
                oldSpace.removeToken(initiator);
            }
            newSpace.addToken(initiator);
            initiator.setCurrentSpace(newSpace);
            log.info("MERGER MARKET: Moved " + initiator.getId() + " to space " + newSpace.getId());
            net.sf.rails.common.ReportBuffer.add(this, "MARKET: " + initiator.getId() + " marker placed at $"
                    + actualNewPrice + ", operating after any existing companies.");
        } else {
            log.error("MERGER MARKET: FAILED to find valid stock space for price " + rawSum);
        }

        int overlappingMarkers = 0;
        int retainedMarkers = 0;
        List<net.sf.rails.game.BaseToken> targetTokens = new java.util.ArrayList<>(target.getLaidBaseTokens());
        for (net.sf.rails.game.BaseToken targetToken : targetTokens) {
            if (targetToken.getOwner() instanceof net.sf.rails.game.Stop) {
                net.sf.rails.game.Stop stop = (net.sf.rails.game.Stop) targetToken.getOwner();
                boolean hexHasInitiatorToken = false;
                for (net.sf.rails.game.Stop s : stop.getHex().getStops()) {
                    if (s.hasTokenOf(initiator)) {
                        hexHasInitiatorToken = true;
                        break;
                    }
                }

                targetToken.moveTo(target);

                if (!hexHasInitiatorToken) {
                    net.sf.rails.game.BaseToken newToken = initiator.getNextBaseToken();
                    if (newToken != null) {
                        newToken.moveTo(stop);
                        retainedMarkers++;
                    } else {
                        log.warn("MERGER TOKENS: " + initiator.getId() + " lacked tokens to replace marker at "
                                + stop.getHex().getId());
                    }
                } else {
                    overlappingMarkers++;
                }
            }
        }

        if (overlappingMarkers > 0) {
            net.sf.rails.common.ReportBuffer.add(this, "MARKER CONFLICT: " + overlappingMarkers
                    + " overlapping marker(s) removed and returned to charter.");
        } else {
            net.sf.rails.common.ReportBuffer.add(this,
                    "MARKER CONFLICT: None. All " + retainedMarkers + " target marker(s) successfully retained.");
        }

        // 6. Share Structure Update
        ((PublicCompany_1817) initiator).setShareCount(5);
        log.info("MERGER SHARES: Upgraded " + initiator.getId()
                + " to a 5-share company. Certificates and tokens automatically adjusted.");

        // The President already holds the Initiator's President's Certificate.
        // Upon upgrading to a 5-share company, this certificate represents 40% (2
        // shares).
        // The 4 original shares (2 from Target, 2 from Initiator) exchange 2-for-1 into
        // these 2 new shares.
        // The remaining 3 shares stay in the Initiator's treasury. No extra shares are
        // transferred.
        log.info("MERGER SHARES: President retains the 40% President's Certificate. 3 shares remain in Treasury.");

        // Post-Merger Report
        net.sf.rails.common.ReportBuffer.add(this,
                "Post-Merger State (Combined):\n" + buildCompanyStateReport(initiator));

        // 7. Remove Disappearing Company
        ((PublicCompany_1817) target).resetForReuse();

        // 8. Update State tracking
        mergedThisRound.add(initiator.getId());
        mergedThisRound.add(target.getId());

        // 8.5. Check Train Limit (1817 Rules 7.1.2 & 7.2.4)
        if (enforceTrainLimit(initiator)) {
            log.info("MERGER: Initiator {} exceeds limit. Switching to DISCARD_TRAINS.", initiator.getId());
            operatingCompany.set(initiator);
            stepToResume.set(MaAStep.POST_MERGER_PRESIDENT);
            currentStep.set(MaAStep.DISCARD_TRAINS);
            setPossibleActions();
            return;
        }

        // 9. Initiate Post-Merger Phase
        calculateValidMergerPairs();
        startPostMergerPhase();
    }

    private void execute5ShareMerger(PublicCompany initiator, PublicCompany target) {
        log.info(">>> Executing 5-Share Merger: " + initiator.getId() + " absorbs " + target.getId());

        // Capture pre-merger state for accurate PBM reporting
        String initiatorPreReport = buildCompanyStateReport(initiator);
        String targetPreReport = buildCompanyStateReport(target);
        Player originalPresident = initiator.getPresident();

        net.sf.rails.common.ReportBuffer.add(this,
                "--- MERGER EVENT: " + initiator.getId() + " (Survivor) absorbs " + target.getId()
                        + " (Disappearing) ---");
        net.sf.rails.common.ReportBuffer.add(this, "Pre-Merger State (Survivor):\n" + initiatorPreReport);
        net.sf.rails.common.ReportBuffer.add(this, "Pre-Merger State (Disappearing):\n" + targetPreReport);
        // 1. Core Asset Consolidation
        initiator.transferAssetsFrom(target);

        // 2. Loan Consolidation
        if (initiator.canLoan() && target.canLoan()) {
            int targetLoans = target.getCurrentNumberOfLoans();
            initiator.addLoans(targetLoans);
            target.addLoans(-targetLoans);
        }

        // 3. Bond Consolidation
        int targetBonds = target.getNumberOfBonds();
        initiator.setNumberOfBonds(initiator.getNumberOfBonds() + targetBonds);
        target.setNumberOfBonds(0);

        // 4. Station Marker Conversion
        for (net.sf.rails.game.BaseToken targetToken : target.getLaidBaseTokens()) {
            if (targetToken.getOwner() instanceof net.sf.rails.game.Stop) {
                net.sf.rails.game.Stop stop = (net.sf.rails.game.Stop) targetToken.getOwner();
                boolean hexHasInitiatorToken = false;
                for (net.sf.rails.game.Stop s : stop.getHex().getStops()) {
                    if (s.hasTokenOf(initiator)) {
                        hexHasInitiatorToken = true;
                        break;
                    }
                }
                targetToken.moveTo(target);

                if (!hexHasInitiatorToken) {
                    net.sf.rails.game.BaseToken newToken = initiator.getNextBaseToken();
                    if (newToken != null)
                        newToken.moveTo(stop);
                }
            }
        }

        // 6. Market Value Adjustment and UI Marker Move
        StockSpace oldSpace = initiator.getCurrentSpace();

        // Rule 7.1.3: New stock value is the average of the two 5-share values
        int rawAvg = (initiator.getMarketPrice() + target.getMarketPrice()) / 2;
        StockMarket_1817 sm = (StockMarket_1817) gameManagerRef.getRoot().getStockMarket();
        StockSpace newSpace = sm.getFloorSpace(rawAvg);
        int actualNewPrice = newSpace != null ? newSpace.getPrice() : rawAvg;

        // Explicitly remove target marker
        StockSpace targetSpace = target.getCurrentSpace();
        if (targetSpace != null) {
            targetSpace.removeToken(target);
            target.setCurrentSpace(null);
            net.sf.rails.common.ReportBuffer.add(this,
                    "MARKET: " + target.getId() + " marker removed from the stock market.");
        }

        if (newSpace != null) {
            if (oldSpace != null) {
                oldSpace.removeToken(initiator);
            }
            newSpace.addToken(initiator);
            initiator.setCurrentSpace(newSpace);
            log.info("MERGER MARKET: Moved " + initiator.getId() + " to space " + newSpace.getId());
            net.sf.rails.common.ReportBuffer.add(this, "MARKET: " + initiator.getId() + " marker placed at $"
                    + actualNewPrice + ", operating after any existing companies.");
        }

        // 6. Share Structure Update
        ((PublicCompany_1817) initiator).setShareCount(10);

        // 1-for-1 ownership exchange mapping
        List<PublicCertificate> availableCerts = new java.util.ArrayList<>();
        for (PublicCertificate cert : initiator.getCertificates()) {
            if (cert.getOwner() == initiator && !cert.isPresidentShare()) {
                availableCerts.add(cert);
            }
        }

        int certIndex = 0;
        for (PublicCertificate tCert : target.getCertificates()) {
            net.sf.rails.game.state.Owner owner = tCert.getOwner();
            if (owner != null && owner != target && owner != initiator) {
                int shareUnits = tCert.getShare() / target.getShareUnit();
                for (int i = 0; i < shareUnits; i++) {
                    if (certIndex < availableCerts.size()) {
                        PublicCertificate newICert = availableCerts.get(certIndex);
                        if (owner instanceof net.sf.rails.game.Player) {
                            newICert.moveTo(((net.sf.rails.game.Player) owner).getPortfolioModel());
                        } else if (owner instanceof net.sf.rails.game.financial.Bank) {
                            newICert.moveTo(gameManagerRef.getRoot().getBank().getPool());
                        } else if (owner instanceof net.sf.rails.game.PublicCompany) {
                            newICert.moveTo(((net.sf.rails.game.PublicCompany) owner).getPortfolioModel());
                        }
                        certIndex++;
                    }
                }
            } else if (owner == target || owner == initiator) {
                // Keep target's treasury shares in initiator's treasury
                int shareUnits = tCert.getShare() / target.getShareUnit();
                certIndex += shareUnits;
            }
        }
        // Short Share Conversion (Rule 7.1.3)
        net.sf.rails.game.financial.BankPortfolio unavailableBank = gameManagerRef.getRoot().getBank().getUnavailable();
        List<net.sf.rails.game.financial.PublicCertificate> initiatorShorts = new java.util.ArrayList<>();
        for (net.sf.rails.game.financial.PublicCertificate cert : unavailableBank.getPortfolioModel()
                .getCertificates()) {
            if (cert instanceof net.sf.rails.game.specific._1817.ShortCertificate && cert.getCompany() == initiator) {
                initiatorShorts.add(cert);
            }
        }

        int shortIndex = 0;
        for (net.sf.rails.game.Player p : gameManagerRef.getRoot().getPlayerManager().getPlayers()) {
            List<net.sf.rails.game.financial.PublicCertificate> playerTargetShorts = new java.util.ArrayList<>();
            for (net.sf.rails.game.financial.PublicCertificate c : p.getPortfolioModel().getCertificates()) {
                if (c.getCompany() == target && c instanceof net.sf.rails.game.specific._1817.ShortCertificate) {
                    playerTargetShorts.add(c);
                }
            }

            for (net.sf.rails.game.financial.PublicCertificate oldShort : playerTargetShorts) {
                if (shortIndex < initiatorShorts.size()) {
                    // Standard swap
                    net.sf.rails.game.financial.PublicCertificate newShort = initiatorShorts.get(shortIndex);
                    oldShort.moveTo(unavailableBank);
                    newShort.moveTo(p.getPortfolioModel());
                    shortIndex++;
                    log.info("MERGER SHORTS: Swapped " + p.getName() + "'s " + target.getId() + " short for "
                            + initiator.getId() + " short.");
                } else {
                    // Excess shorts (Rule 7.1.3)
                    log.warn("MERGER SHORTS: Excess short for " + p.getName() + ". Retaining " + target.getId()
                            + " certificate as " + initiator.getId() + " liability.");
                }
            }
        }

        log.info("MERGER SHARES: Upgraded " + initiator.getId() + " to 10-share. 1-for-1 ownership exchange complete.");

        // Re-evaluate Presidency (Rule 7.1.3)
        Player newPresident = originalPresident;
        int maxShares = countPlayerShares(initiator, originalPresident);
        for (Player p : gameManagerRef.getRoot().getPlayerManager().getPlayers()) {
            int pShares = countPlayerShares(initiator, p);
            if (pShares > maxShares) {
                maxShares = pShares;
                newPresident = p;
            }
        }
        if (newPresident != initiator.getPresident()) {
            initiator.setPresident(newPresident);
            net.sf.rails.common.ReportBuffer.add(this,
                    "PRESIDENCY CHANGE: " + newPresident.getName() + " assumes control of the new 10-share company.");
        }

        // Post-Merger Report
        net.sf.rails.common.ReportBuffer.add(this,
                "Post-Merger State (Combined):\n" + buildCompanyStateReport(initiator));

        // 7. Remove Disappearing Company
        ((PublicCompany_1817) target).resetForReuse();
        // 8. Update State tracking
        mergedThisRound.add(initiator.getId());
        mergedThisRound.add(target.getId());

        // Rule 7.1.3 & 7.2.4: Check for excess trains after consolidation
        if (enforceTrainLimit(initiator)) {
            log.info("MERGER: Initiator {} exceeds limit. Pausing for discard.", initiator.getId());
            operatingCompany.set(initiator);
            stepToResume.set(MaAStep.POST_MERGER_PRESIDENT);
            currentStep.set(MaAStep.DISCARD_TRAINS);
            return; // Buttons generated by enforceTrainLimit()
        }

        // 9. Initiate Post-Merger Phase
        calculateValidMergerPairs();
        startPostMergerPhase();
    }

    private void endPostMergerPhase() {

        // Report the final state only after all mandatory token purchases, loans, and
        // post-merger trading are complete
        if (operatingCompany.value() != null) {
            net.sf.rails.common.ReportBuffer.add(this,
                    "Final Post-Event State:\n" + buildCompanyStateReport(operatingCompany.value()));
        }

        companyIndex.set(companyIndex.value() + 1);
        currentStep.set(MaAStep.NEXT_COMPANY);
        processNextCompany();

    }

    private void initPostMergerPlayers() {
        PublicCompany comp = operatingCompany.value();
        net.sf.rails.game.Player president = comp.getPresident();
        List<net.sf.rails.game.Player> players = gameManagerRef.getRoot().getPlayerManager().getPlayers();

        int presIndex = players.indexOf(president);
        currentPlayerIndex.set((presIndex + 1) % players.size());
        playersProcessed.set(1);

        skipInvalidPostMergerPlayers(comp, players);
    }

    private void advancePostMergerPlayer() {
        PublicCompany comp = operatingCompany.value();
        List<net.sf.rails.game.Player> players = gameManagerRef.getRoot().getPlayerManager().getPlayers();

        currentPlayerIndex.set((currentPlayerIndex.value() + 1) % players.size());
        playersProcessed.set(playersProcessed.value() + 1);

        skipInvalidPostMergerPlayers(comp, players);
    }

    private void skipInvalidPostMergerPlayers(PublicCompany comp, List<net.sf.rails.game.Player> players) {
        while (playersProcessed.value() < players.size()) {
            net.sf.rails.game.Player p = players.get(currentPlayerIndex.value());
            if (hasValidTreasuryBuy(comp, p) || hasValidSell(comp, p)) {
                break;
            }
            currentPlayerIndex.set((currentPlayerIndex.value() + 1) % players.size());
            playersProcessed.set(playersProcessed.value() + 1);
        }

        if (playersProcessed.value() >= players.size()) {
            currentStep.set(MaAStep.LOAN_TAKING);
            setPossibleActions();
        } else {
            currentStep.set(MaAStep.POST_MERGER_PLAYERS);
            setPossibleActions();
        }
    }

    private void setupPostMergerPresidentActions() {
        PublicCompany comp = operatingCompany.value();
        net.sf.rails.game.Player president = comp.getPresident();
        if (president != null) {
            addTreasuryBuyActions(comp, president);
            possibleActions.add(new NullAction(gameManagerRef.getRoot(), NullAction.Mode.DONE));
        }
    }

    private void setupPostMergerPlayerActions() {
        PublicCompany comp = operatingCompany.value();
        List<net.sf.rails.game.Player> players = gameManagerRef.getRoot().getPlayerManager().getPlayers();
        int idx = currentPlayerIndex.value();
        if (idx >= 0 && idx < players.size()) {
            net.sf.rails.game.Player player = players.get(idx);

            addTreasuryBuyActions(comp, player);
            addSellActions(comp, player);

            possibleActions.add(new NullAction(gameManagerRef.getRoot(), NullAction.Mode.PASS));
        }
    }

    private void addSellActions(PublicCompany comp, net.sf.rails.game.Player player) {
        int sharesOwned = 0;
        for (net.sf.rails.game.financial.PublicCertificate cert : player.getPortfolioModel().getCertificates()) {
            if (cert.getCompany() == comp && !cert.isPresidentShare()) {
                sharesOwned += cert.getShare() / comp.getShareUnit();
            }
        }

        if (sharesOwned > 0) {
            int price = comp.getMarketPrice();
            for (int i = 1; i <= sharesOwned; i++) {
                possibleActions.add(new rails.game.action.SellShares(comp, comp.getShareUnit(), i, price));
            }
        }
    }

    private boolean hasValidSell(PublicCompany comp, net.sf.rails.game.Player player) {
        for (net.sf.rails.game.financial.PublicCertificate cert : player.getPortfolioModel().getCertificates()) {
            if (cert.getCompany() == comp && !cert.isPresidentShare()) {
                return true;
            }
        }
        return false;
    }

private void updateCurrentPlayer() {
        net.sf.rails.game.Player p = getActingPlayer();
        if (p != null && !p.equals(gameManagerRef.getRoot().getPlayerManager().getCurrentPlayer())) {
            gameManagerRef.getRoot().getPlayerManager().setCurrentPlayer(p);
        }
    }

    @Override
    public boolean setPossibleActions() {
        updateCurrentPlayer();
        possibleActions.clear();

        if (currentStep.value() == MaAStep.DISCARD_TRAINS) {
            // Re-generate buttons using master logic from Round.java
            enforceTrainLimit(operatingCompany.value());
            return true;
        }

        if (currentStep.value() == MaAStep.COMPANY_ACTIONS) {
            setupCompanyActions(operatingCompany.value());
        } else if (currentStep.value() == MaAStep.POST_MERGER_PRESIDENT) {
            setupPostMergerPresidentActions();
        } else if (currentStep.value() == MaAStep.POST_MERGER_PLAYERS) {
            setupPostMergerPlayerActions();
        } else if (currentStep.value() == MaAStep.LOAN_TAKING) {
            setupPostMergerLoanActions();
            // Provide escape buttons if the company is not forced into another action
            if (currentStep.value() == MaAStep.LOAN_TAKING) {
                possibleActions.add(new NullAction(gameManagerRef.getRoot(), NullAction.Mode.PASS));
                possibleActions.add(new NullAction(gameManagerRef.getRoot(), NullAction.Mode.DONE));
            }
        } else if (currentStep.value() == MaAStep.SALES_FRIENDLY) {
            // President's decision: DONE to offer for sale, PASS to decline and keep
            updateCurrentPlayer();
            possibleActions.add(
                    new net.sf.rails.game.specific._1817.action.OfferCompanyForSale_1817(operatingCompany.value()));
            possibleActions.add(new NullAction(gameManagerRef.getRoot(), NullAction.Mode.PASS));
        } else if (currentStep.value() == MaAStep.SALES_AUCTION) {
            setupAuctionActions();
        } else if (currentStep.value() == MaAStep.SALES_SELECT_BUYER) {
            net.sf.rails.game.Player winner = highestBiddingPlayer.value();
            gameManagerRef.getRoot().getPlayerManager().setCurrentPlayer(winner);
            PublicCompany target = operatingCompany.value();

            // Winning president must choose which of their eligible companies makes the buy
            for (PublicCompany comp : gameManagerRef.getRoot().getCompanyManager().getAllPublicCompanies()) {
                if (comp.getPresident() != null && comp.getPresident().equals(winner) && !comp.equals(target)) {
                    // Predators must not be in the liquidation or acquisition zones ($30 or less)
                    if (comp.getCurrentSpace() != null && comp.getCurrentSpace().getPrice() > 30) {
                        possibleActions.add(new net.sf.rails.game.specific._1817.action.SelectPurchasingCompany_1817(
                                gameManagerRef.getRoot(), comp.getId()));
                    }
                }
            }
        }
        return true;
    }

    private int countPlayerShares(PublicCompany comp, net.sf.rails.game.Player player) {
        int count = 0;
        // Efficiently iterate only through certificates of this specific company
        for (net.sf.rails.game.financial.PublicCertificate cert : player.getPortfolioModel().getCertificates(comp)) {
            // cert.getShare() is the percentage (e.g., 20 or 40)
            // comp.getShareUnit() is the percentage per share (e.g., 20 for 5-share)
            count += cert.getShare() / comp.getShareUnit();
        }
        return count;
    }

    private int getPlayerSharePercentage(PublicCompany comp, net.sf.rails.game.Player player) {
        int percentage = 0;
        for (net.sf.rails.game.financial.PublicCertificate cert : player.getPortfolioModel().getCertificates(comp)) {
            percentage += cert.getShare();
        }
        return percentage;
    }

    private void addTreasuryBuyActions(PublicCompany comp, net.sf.rails.game.Player player) {
        int price = comp.getMarketPrice();

        // 1817 Rule 5.2: 60% Maximum ownership limit
        if (player.getCash() < price || getPlayerSharePercentage(comp, player) >= 60)
            return;

        for (net.sf.rails.game.financial.PublicCertificate cert : comp.getCertificates()) {
            if (cert.getOwner() == comp && !cert.isPresidentShare()) {
                possibleActions.add(new rails.game.action.BuyCertificate(comp, cert.getShare(), comp, price));
                break;
            }
        }
    }

    private boolean hasValidTreasuryBuy(PublicCompany comp, net.sf.rails.game.Player player) {
        int price = comp.getMarketPrice();

        if (player.getCash() >= price && getPlayerSharePercentage(comp, player) < 60) {
            for (net.sf.rails.game.financial.PublicCertificate cert : comp.getCertificates()) {
                if (cert.getOwner() == comp && !cert.isPresidentShare()) {
                    return true;
                }
            }
        }
        return false;
    }

    private void startPostMergerPhase() {
        PublicCompany comp = operatingCompany.value();
        net.sf.rails.game.Player president = comp.getPresident();

        if (president != null && hasValidTreasuryBuy(comp, president)) {
            currentStep.set(MaAStep.POST_MERGER_PRESIDENT);
            setPossibleActions();
        } else {
            if (president != null) {

            }
            initPostMergerPlayers();
        }
    }

    private void setupPostMergerLoanActions() {
        PublicCompany comp = operatingCompany.value();
        if (comp instanceof PublicCompany_1817) {
            PublicCompany_1817 comp1817 = (PublicCompany_1817) comp;
            // Limit is based on 2, 5, or 10 shares
            int limit = comp1817.getShareCount();
            int current = comp.getNumberOfBonds();

            if (current < limit) {

                possibleActions.add(new TakeLoans_1817(getRoot(), comp.getId()));
                // Add a DONE action so the player can choose NOT to take a loan
                possibleActions.add(new NullAction(gameManagerRef.getRoot(), NullAction.Mode.DONE));

            } else {
                // If at limit, skip to token purchase automatically
                currentStep.set(MaAStep.TOKEN_PURCHASE);
                executeMandatoryTokenPurchase();
            }
        }
    }

    private void executeMandatoryTokenPurchase() {
        PublicCompany comp = operatingCompany.value();
        PublicCompany_1817 comp1817 = (PublicCompany_1817) comp;

        int cost = mandatoryTokenCost.value();
        mandatoryTokenCost.set(0);

        if (cost > 0) {
            // Case 2: Cash < cost -> take a loan to cover the fee (Rule 7.1.1)
            if (comp.getCash() < cost) {

                comp1817.setNumberOfBonds(comp1817.getNumberOfBonds() + 1);

                net.sf.rails.game.financial.Bank bank = gameManagerRef.getRoot().getBank();
                comp1817.addCashFromBank(100, bank);

                // Rule 1.2.5 & 6.1: Move stock one space left per loan
                net.sf.rails.game.financial.StockMarket market = gameManagerRef.getRoot().getStockMarket();
                if (market instanceof StockMarket_1817) {
                    ((StockMarket_1817) market).moveLeftOrDown(comp, 1);
                }
                net.sf.rails.common.ReportBuffer.add(this,
                        comp.getId() + " automatically takes a loan to fund mandatory station markers.");
            }

            // Case 1: Cash >= cost -> Buy the token (Rule 7.1.1)
            if (comp.getCash() >= cost) {
                net.sf.rails.game.state.Currency.wire(comp, cost, gameManagerRef.getRoot().getBank());
                net.sf.rails.common.ReportBuffer.add(this,
                        comp.getId() + " purchases mandatory station markers for $" + cost + ".");
            } else {
                // Rule 7.1.1 Fallback: Liquidation if Bank is empty
                net.sf.rails.game.financial.StockSpace liquidationSpace = gameManagerRef.getRoot().getStockMarket()
                        .getStartSpace(0);
                if (liquidationSpace != null) {
                    if (comp.getCurrentSpace() != null) {
                        comp.getCurrentSpace().removeToken(comp);
                    }
                    liquidationSpace.addToken(comp);
                    comp.setCurrentSpace(liquidationSpace);
                }
            }
        }
        // Finalize this company and move to the next in the M&A order
        endPostMergerPhase();
    }

    private void setupCompanyActions(PublicCompany company) {
        for (String pair : validMergerPairs.view()) {
            String[] ids = pair.split(",");

            if (ids[0].equals(company.getId())) {
                PublicCompany target = gameManagerRef.getRoot().getCompanyManager().getPublicCompany(ids[1]);
                if (target != null) {
                    possibleActions.add(new MergeCompanies_1817(company, target));

                }
            }
        }

        // Add TakeLoans_1817 action if the company is capable
        if (company instanceof PublicCompany_1817) {
            PublicCompany_1817 comp1817 = (PublicCompany_1817) company;
            int maxLoans = comp1817.getShareCount();
            if (company.getNumberOfBonds() < maxLoans) {
                possibleActions.add(new net.sf.rails.game.specific._1817.action.TakeLoans_1817(getRoot(),
                        company.getId()));

            }

            // Check for Conversions (White zone only, > $30)
            if (company.getCurrentSpace() != null && company.getCurrentSpace().getPrice() > 30) {
                if (comp1817.getShareCount() == 2) {
                    int availableFunds = comp1817.getCash() + (5 - comp1817.getNumberOfBonds()) * 100;
                    if (availableFunds >= 50) {
                        possibleActions.add(new net.sf.rails.game.specific._1817.action.ConvertCompany_1817(company));
                    } 
                    //else net.sf.rails.common.ReportBuffer.add(this, company.getId() + " cannot afford conversion markers (" + (availableFunds / 50) + " max affordable, 1 required). Action unavailable.");
                } else if (comp1817.getShareCount() == 5) {
                    int currentMarkers = company.getBaseTokens().nbAllTokens();
                    int cost = 0;
                    if (currentMarkers <= 6) cost = 100;
                    else if (currentMarkers == 7) cost = 50;
                    
                    int availableFunds = comp1817.getCash() + (10 - comp1817.getNumberOfBonds()) * 100;
                    if (availableFunds >= cost) {
                        possibleActions.add(new net.sf.rails.game.specific._1817.action.ConvertCompany_1817(company));
                    } 
                    //else if (cost > 0) net.sf.rails.common.ReportBuffer.add(this, company.getId() + " cannot afford conversion markers (" + (availableFunds / 50) + " max affordable, " + (cost / 50) + " required). Action unavailable.");
                }
            }

        }

        addPassAction(company);
    }

    /**
     * Initiates the sales sequence in reverse operating order.
     */
    private void startSalesPhase() {

        List<PublicCompany> sortedComps = new java.util.ArrayList<>(gameManagerRef.getCompaniesInRunningOrder());
        java.util.Collections.reverse(sortedComps);

        operatingCompanies.clear();
        for (PublicCompany c : sortedComps) {
            if (c.hasFloated() && !c.isClosed()) {
                operatingCompanies.add(c);
            }
        }

        companyIndex.set(0);
        currentStep.set(MaAStep.SALES_LIQUIDATION);
        processNextSale();
    }

    private void processNextSale() {
        if (companyIndex.value() >= operatingCompanies.size()) {
            // Move to next logical phase of the round
            currentStep.set(MaAStep.FINISHED);
            gameManagerRef.nextRound(this);
            return;
        }

        PublicCompany comp = operatingCompanies.get(companyIndex.value());
        operatingCompany.set(comp);

        int price = comp.getMarketPrice();

        boolean isRed = false;
        boolean isGray = false;
        boolean droppedToGrayThisRound = false;

        if (comp.hasStockPrice() && comp.getCurrentSpace() != null) {
            int currentPrice = comp.getCurrentSpace().getPrice();
            isRed = (currentPrice == 0);
            isGray = (currentPrice > 0 && currentPrice <= 30);
            if (isGray && startedAboveAcquisition.contains(comp.getId())) {
                droppedToGrayThisRound = true;
            }
        }
        if (droppedToGrayThisRound) {

            companyIndex.set(companyIndex.value() + 1);
            processNextSale();
            return;
        }

        if (isRed) {
            net.sf.rails.common.ReportBuffer.add(this, "--- LIQUIDATION OFFERING: " + comp.getId() + " ---");
            net.sf.rails.common.ReportBuffer.add(this, "Pre-Event State:\n" + buildCompanyStateReport(comp));
            startAuction(comp, 0);
        } else if (isGray) {
            net.sf.rails.common.ReportBuffer.add(this, "--- ACQUISITION OFFERING: " + comp.getId() + " ---");
            net.sf.rails.common.ReportBuffer.add(this, "Pre-Event State:\n" + buildCompanyStateReport(comp));
            startAuction(comp, 10);

        } else {
            currentStep.set(MaAStep.SALES_FRIENDLY);
            setPossibleActions();
        }
    }

    private void startAuction(PublicCompany target, int minBid) {

        // Initialize state variables
        operatingCompany.set(target);
        highestBid.set(minBid);
        highestBiddingPlayer.set(null);
        currentStep.set(MaAStep.SALES_AUCTION);

        // Rule 7.2.4: Bank makes the initial $0 bid for liquidations
        if (minBid == 0 && target.getCurrentSpace().getPrice() == 0) {
            net.sf.rails.common.ReportBuffer.add(this, "Bank bids $0.");
        }

        // 1817 Rule 7.2.1: Bidding starts with the player to the left of the president
        Player president = target.getPresident();
        List<Player> players = gameManagerRef.getRoot().getPlayerManager().getPlayers();
        int presIndex = players.indexOf(president);

        activeBidders.clear();
        for (int i = 0; i < players.size(); i++) {
            activeBidders.add(players.get((presIndex + 1 + i) % players.size()));
        }
        auctionPlayerIndex.set(0);

        setPossibleActions();
    }

    private int calculateMaxPurchasingPower(net.sf.rails.game.Player player, PublicCompany target) {
        int maxPower = 0;

        int targetTotalCash = target.getCash();
        boolean isRed = (target.getCurrentSpace() != null && target.getCurrentSpace().getPrice() == 0);
        boolean isGray = (target.getCurrentSpace() != null && target.getCurrentSpace().getPrice() > 0
                && target.getCurrentSpace().getPrice() <= 30);

        if (!isRed && !isGray) {
            int treasurySharesSold = 0;
            for (PublicCertificate cert : target.getCertificates()) {
                if (cert.getOwner() == target && !cert.isPresidentShare()) {
                    treasurySharesSold++;
                }
            }
            targetTotalCash += treasurySharesSold * target.getMarketPrice();
        }

        int targetLoans = (target instanceof PublicCompany_1817) ? ((PublicCompany_1817) target).getNumberOfBonds() : 0;

        // Iterate through all companies to find the one with the highest liquidity/loan
        // capacity
        for (PublicCompany comp : gameManagerRef.getCompaniesInRunningOrder()) {
            if (comp.getPresident() == player && comp instanceof PublicCompany_1817 && !comp.equals(target)) {
                if (comp.getCurrentSpace() != null && comp.getCurrentSpace().getPrice() > 30) {
                    PublicCompany_1817 comp1817 = (PublicCompany_1817) comp;

                    // Rule 7.2.4: Purchasing power includes current treasury plus remaining loan
                    // capacity
                    // 1817 loans are $100 each. Acquired target loans reduce available capacity.

                    int netLoanCapacity = comp1817.getShareCount() - comp1817.getNumberOfBonds() - targetLoans;
                    int power;
                    if (netLoanCapacity >= 0) {
                        power = comp.getCash() + targetTotalCash + (netLoanCapacity * 100);
                    } else {
                        // Company must pay off excess loans immediately using available cash
                        power = comp.getCash() + targetTotalCash - (Math.abs(netLoanCapacity) * 100);
                    }

                    if (power > maxPower) {
                        maxPower = power;
                    }

                }
            }
        }
        return maxPower;
    }

    // ... (lines of unchanged context code preceding the method) ...
    private void executeSale(PublicCompany target, PublicCompany predator, int finalBid) {

        boolean isTargetRed = (target.getCurrentSpace().getPrice() == 0);
        if (isTargetRed) {
            executeLiquidationSale(target, predator, finalBid);
            return;
        }

        net.sf.rails.common.ReportBuffer.add(this, (predator != null ? predator.getId() : "Bank") + " buys "
                + target.getId() + " for $" + finalBid + ".");
        boolean isRed = (target.getCurrentSpace().getPrice() == 0);
        boolean isGray = (target.getCurrentSpace().getPrice() > 0 && target.getCurrentSpace().getPrice() <= 30);

        // 1. Treasury Share Sell-off (Rule 7.2.4)
        int treasurySharesSold = 0;
        for (PublicCertificate cert : new java.util.ArrayList<>(target.getCertificates())) {
            if (cert.getOwner() == target && !cert.isPresidentShare()) {
                cert.moveTo(gameManagerRef.getRoot().getBank().getPool());
                treasurySharesSold++;
            }
        }

        if (!isRed && !isGray && treasurySharesSold > 0) {
            int cashInfusion = treasurySharesSold * target.getMarketPrice();
            net.sf.rails.game.state.Currency.wire(gameManagerRef.getRoot().getBank(), cashInfusion, target);
            net.sf.rails.common.ReportBuffer.add(this, "TREASURY SELL-OFF: " + treasurySharesSold
                    + " shares sold to the open market for $" + cashInfusion + ".");
        }

        // 2. Payout Calculation (Rule 7.2.4)
        int shareCount = ((PublicCompany_1817) target).getShareCount();
        int payoutPerShare = finalBid / shareCount;

        // 3. Execution of Sale Assets (Rule 7.2.4)
        if (predator != null) {

            // Check for Train Station before transfer
            boolean targetHasTrainStation = false;
            for (net.sf.rails.game.PrivateCompany pc : target.getPrivates()) {
                if ("STA80".equals(pc.getId())) {
                    targetHasTrainStation = true;
                    break;
                }
            }

            // Predator absorbs target assets first (including cash and treasury sales
            // infusion)
            predator.transferAssetsFrom(target);

            // Apply Train Station capacity increase
            if (targetHasTrainStation && predator instanceof PublicCompany_1817) {
                ((PublicCompany_1817) predator).addTokenCapacity(1);
                net.sf.rails.common.ReportBuffer.add(this,
                        predator.getId() + " receives an extra station marker from the acquired Train Station.");
            }

            // Convert station markers from target to predator
            List<net.sf.rails.game.BaseToken> targetTokens = new java.util.ArrayList<>(target.getLaidBaseTokens());
            for (net.sf.rails.game.BaseToken targetToken : targetTokens) {
                if (targetToken.getOwner() instanceof net.sf.rails.game.Stop) {
                    net.sf.rails.game.Stop stop = (net.sf.rails.game.Stop) targetToken.getOwner();
                    boolean hexHasPredatorToken = false;
                    for (net.sf.rails.game.Stop s : stop.getHex().getStops()) {
                        if (s.hasTokenOf(predator)) {
                            hexHasPredatorToken = true;
                            break;
                        }
                    }

                    targetToken.moveTo(target);

                    if (!hexHasPredatorToken) {
                        if (predator instanceof PublicCompany_1817) {
                            ((PublicCompany_1817) predator).addTokenCapacity(1);
                        }
                        net.sf.rails.game.BaseToken newToken = predator.getNextBaseToken();
                        if (newToken != null) {
                            newToken.moveTo(stop);
                        } else {

                        }
                    } else {
                        net.sf.rails.common.ReportBuffer.add(this,
                                "MARKER CONFLICT: Overlapping marker removed and returned to charter.");
                    }
                }
            }

            // --- START FIX ---
            // Inherit loans from target
            if (predator instanceof PublicCompany_1817 && target instanceof PublicCompany_1817) {
                int acquiredBonds = ((PublicCompany_1817) target).getNumberOfBonds();
                if (acquiredBonds > 0) {
                    ((PublicCompany_1817) predator).setNumberOfBonds(predator.getNumberOfBonds() + acquiredBonds);
                    String loanStr = (acquiredBonds == 1) ? "1 loan" : acquiredBonds + " loans";
                    net.sf.rails.common.ReportBuffer.add(this,
                            predator.getId() + " inherits " + loanStr + " from " + target.getId() + ".");

                    // Rule 7.2.4 Step 7: Penalty for inherited loans not paid off
                    if (gameManagerRef.getRoot().getStockMarket() instanceof StockMarket_1817) {
                        ((StockMarket_1817) gameManagerRef.getRoot().getStockMarket()).moveLeftOrDown(predator,
                                acquiredBonds);
                        net.sf.rails.common.ReportBuffer.add(this,
                                predator.getId() + " price drops " + acquiredBonds + " space(s) for inherited debt.");
                    }
                }
            }

            // Auto-take new loans if cash is still insufficient to pay the final bid
            if (predator instanceof PublicCompany_1817) {
                PublicCompany_1817 p1817 = (PublicCompany_1817) predator;
                int loansTaken = 0;
                while (p1817.getCash() < finalBid && p1817.getNumberOfBonds() < p1817.getShareCount()) {
                    p1817.setNumberOfBonds(p1817.getNumberOfBonds() + 1);
                    p1817.addCashFromBank(100, gameManagerRef.getRoot().getBank());
                    loansTaken++;
                    if (gameManagerRef.getRoot().getStockMarket() instanceof StockMarket_1817) {
                        ((StockMarket_1817) gameManagerRef.getRoot().getStockMarket()).moveLeftOrDown(p1817, 1);
                    }
                }
                if (loansTaken > 0) {

                    net.sf.rails.common.ReportBuffer.add(this,
                            p1817.getId() + " automatically takes " + loansTaken + " loan(s) to fund the purchase.");
                }
            }

            // Predator pays the Bank (Rule 7.2.4)
            net.sf.rails.game.state.Currency.wire(predator, finalBid, gameManagerRef.getRoot().getBank());
        }

        // 4. Shareholder Settlement (Rule 7.2.4)
        settleWithShareholders(target, payoutPerShare);

        ((PublicCompany_1817) target).resetForReuse();
        if (predator != null) {
            net.sf.rails.common.ReportBuffer.add(this,
                    "Final Post-Event State (Predator):\n" + buildCompanyStateReport(predator));

            if (enforceTrainLimit(predator)) {
                operatingCompany.set(predator);
                stepToResume.set(MaAStep.NEXT_COMPANY);
                currentStep.set(MaAStep.DISCARD_TRAINS);
                return;
            }
        }

        companyIndex.set(companyIndex.value() + 1);
        processNextSale();
    }

    private void setupAuctionActions() {
        if (activeBidders.isEmpty())
            return;

        net.sf.rails.game.Player activePlayer = activeBidders.get(auctionPlayerIndex.value());

        PublicCompany target = operatingCompany.value();
        int currentHighestBid = highestBid.value();
        net.sf.rails.game.Player president = target.getPresident();

        possibleActions.add(new NullAction(gameManagerRef.getRoot(), NullAction.Mode.PASS));

        boolean isPresident = activePlayer.equals(president);

        boolean isLiquidation = (target.getCurrentSpace() != null && target.getCurrentSpace().getPrice() == 0);
        boolean hasPreviousBid = highestBiddingPlayer.value() != null || isLiquidation;

        int minNextBid = hasPreviousBid ? currentHighestBid + 10 : currentHighestBid;

        boolean canAfford = false;
        int maxPurchasingPower = calculateMaxPurchasingPower(activePlayer, target);

        for (PublicCompany comp : gameManagerRef.getRoot().getCompanyManager().getAllPublicCompanies()) {
            if (comp.getPresident() != null && comp.getPresident().equals(activePlayer) && !comp.equals(target)) {
                if (comp.getCurrentSpace() != null && comp.getCurrentSpace().getPrice() > 30) {
                    if (maxPurchasingPower >= minNextBid) {
                        canAfford = true;
                        break;
                    }
                }
            }
        }

        if (canAfford) {
            int actualMaxBid = maxPurchasingPower;
            if (isPresident) {
                actualMaxBid = Math.min(actualMaxBid, currentHighestBid + 10);
            }
            if (minNextBid <= actualMaxBid) {
                possibleActions.add(new net.sf.rails.game.specific._1817.action.BidOnCompany_1817(
                        gameManagerRef.getRoot(), target.getId(), minNextBid, actualMaxBid));
            }
        }

    }

    private void finalizeAuction() {
        PublicCompany target = operatingCompany.value();
        net.sf.rails.game.Player winner = highestBiddingPlayer.value();
        int finalBid = highestBid.value();

        if (winner == null) {

            // Rule 7.2.4.5: Only companies in liquidation (Red Zone) must be sold to the
            // Bank.
            // Acquisitions (Gray) and Friendly sales survive if there are no bids.
            boolean isRed = (target.getCurrentSpace() != null && target.getCurrentSpace().getPrice() == 0);

            if (isRed) {
                executeSale(target, null, finalBid);
            } else {
                companyIndex.set(companyIndex.value() + 1);
                processNextSale();
            }
        } else {

            currentStep.set(MaAStep.SALES_SELECT_BUYER);
            setPossibleActions();
        }
    }

    private void settleWithShareholders(PublicCompany target, int payoutPerShare) {

        for (Player p : gameManagerRef.getRoot().getPlayerManager().getPlayers()) {
            // Long positions receive cash
            int sharesOwned = countPlayerShares(target, p);
            if (sharesOwned > 0) {
                int totalPayout = sharesOwned * payoutPerShare;
                net.sf.rails.game.state.Currency.wire(gameManagerRef.getRoot().getBank(), totalPayout, p);

                String shareStr = (sharesOwned == 1) ? "1 share" : sharesOwned + " shares";
                net.sf.rails.common.ReportBuffer.add(this,
                        p.getName() + " receives $" + totalPayout + " for " + shareStr + " of " + target.getId() + ".");
            }

            // Short positions pay cash (Rule 7.2.4)
            java.util.List<net.sf.rails.game.financial.PublicCertificate> shortsToClose = new java.util.ArrayList<>();
            for (net.sf.rails.game.financial.PublicCertificate cert : p.getPortfolioModel().getCertificates()) {
                if (cert.getCompany() == target && cert instanceof net.sf.rails.game.specific._1817.ShortCertificate) {
                    shortsToClose.add(cert);
                }
            }

            if (!shortsToClose.isEmpty()) {
                int shortCount = shortsToClose.size();
                int totalDebt = shortCount * payoutPerShare;

                net.sf.rails.game.financial.BankPortfolio unavailableBank = gameManagerRef.getRoot().getBank()
                        .getUnavailable();
                for (net.sf.rails.game.financial.PublicCertificate shortCert : shortsToClose) {
                    shortCert.moveTo(unavailableBank);
                }

                if (totalDebt > 0) {
                    net.sf.rails.game.state.Currency.wire(p, totalDebt, gameManagerRef.getRoot().getBank());

                    String shortStr = (shortCount == 1) ? "1 short share" : shortCount + " short shares";
                    net.sf.rails.common.ReportBuffer.add(this, p.getName() + " pays $" + totalDebt + " to the Bank for "
                            + shortStr + " of " + target.getId() + ".");

                }
            }
        }
    }

    private void reconcileShorts(net.sf.rails.game.Player player, net.sf.rails.game.PublicCompany comp) {
        if (player == null || comp == null)
            return;

        boolean foundPair = true;
        while (foundPair) {
            net.sf.rails.game.financial.PublicCertificate shortCert = null;
            net.sf.rails.game.financial.PublicCertificate regularCert = null;

            for (net.sf.rails.game.financial.PublicCertificate c : player.getPortfolioModel().getCertificates()) {
                if (c.getCompany() == comp) {
                    if (c instanceof net.sf.rails.game.specific._1817.ShortCertificate) {
                        shortCert = c;
                    } else if (!c.isPresidentShare()) {
                        regularCert = c;
                    }
                }
            }

            if (shortCert != null && regularCert != null) {
                net.sf.rails.game.financial.BankPortfolio unavailableBank = gameManagerRef.getRoot().getBank()
                        .getUnavailable();
                shortCert.moveTo(unavailableBank);
                regularCert.moveTo(unavailableBank);
                log.info("Mandatory Reconciliation: " + player.getName() + " short position closed for " + comp.getId()
                        + " post-merger.");
                net.sf.rails.common.ReportBuffer.add(this,
                        player.getName() + " automatically closes a short position in " + comp.getId());
            } else {
                foundPair = false;
            }
        }
    }

    private String buildCompanyStateReport(PublicCompany comp) {
        if (comp == null)
            return "None";

        StringBuilder report = new StringBuilder();
        int shareCount = (comp instanceof PublicCompany_1817) ? ((PublicCompany_1817) comp).getShareCount() : 0;
        int price = comp.hasStockPrice() && comp.getCurrentSpace() != null ? comp.getCurrentSpace().getPrice() : 0;

        String presName = comp.getPresident() != null ? comp.getPresident().getName() : "None";
        report.append(comp.getId()).append(" (").append(shareCount).append("-share, Price: $").append(price)
                .append(", President: ").append(presName).append(")\n");
        report.append("  Cash: $").append(comp.getCash());

        int loans = 0;
        if (comp instanceof PublicCompany_1817) {
            loans = ((PublicCompany_1817) comp).getNumberOfBonds();
        } else {
            try {
                loans = comp.getCurrentNumberOfLoans();
            } catch (Exception e) {
            }
        }

        report.append(" | Loans: ").append(loans);

        List<String> trains = new java.util.ArrayList<>();
        for (net.sf.rails.game.Train t : comp.getTrains()) {
            trains.add(t.getName().split("_")[0]); // Remove the '_2' irrelevant detail
        }
        report.append(" | Trains: ").append(trains.isEmpty() ? "None" : String.join(", ", trains));

        List<String> privates = new java.util.ArrayList<>();

        for (net.sf.rails.game.PrivateCompany p : comp.getPrivates())
            privates.add(p.getId());
        report.append("\n  Privates: ").append(privates.isEmpty() ? "None" : String.join(", ", privates));

        List<String> tokens = new java.util.ArrayList<>();
        for (net.sf.rails.game.BaseToken t : comp.getLaidBaseTokens()) {
            if (t.getOwner() instanceof net.sf.rails.game.Stop) {
                tokens.add(((net.sf.rails.game.Stop) t.getOwner()).getHex().getId());
            }
        }
        report.append(" | Tokens: ").append(tokens.isEmpty() ? "None" : String.join(", ", tokens));

        // Share & Short Distribution Tracking
        int treasury = 0;
        for (net.sf.rails.game.financial.PublicCertificate cert : comp.getCertificates()) {
            if (cert.getOwner() == comp && !cert.isPresidentShare())
                treasury += cert.getShare() / comp.getShareUnit();
        }

        int pool = 0;
        for (net.sf.rails.game.financial.PublicCertificate cert : gameManagerRef.getRoot().getBank().getPool()
                .getPortfolioModel().getCertificates()) {
            if (cert.getCompany() == comp && !(cert instanceof net.sf.rails.game.specific._1817.ShortCertificate)) {
                pool += cert.getShare() / comp.getShareUnit();
            }
        }

        int osiShorts = 0;
        int osiRegular = 0;
        int playerShorts = 0;

        if (shareCount > 2) {
            // House Rule: Look in OSI portfolio instead of Unavailable
            net.sf.rails.game.financial.BankPortfolio osiBank = gameManagerRef.getRoot().getBank().getOSI();
            for (net.sf.rails.game.financial.PublicCertificate cert : osiBank.getPortfolioModel().getCertificates()) {
                if (cert.getCompany() == comp) {
                    if (cert instanceof net.sf.rails.game.specific._1817.ShortCertificate) {
                        osiShorts++;
                    } else {
                        osiRegular += cert.getShare() / comp.getShareUnit();
                    }
                }
            }

            for (net.sf.rails.game.Player p : gameManagerRef.getRoot().getPlayerManager().getPlayers()) {
                for (net.sf.rails.game.financial.PublicCertificate cert : p.getPortfolioModel().getCertificates()) {
                    if (cert.getCompany() == comp
                            && cert instanceof net.sf.rails.game.specific._1817.ShortCertificate) {
                        playerShorts++;
                    }
                }
            }
        }

        report.append("\n  Shares -> Treasury: ").append(treasury).append(" | Pool: ").append(pool);
        if (shareCount > 2) {
            report.append(" | OSI: ").append(osiRegular);
            report.append("\n  Shorts -> Players: ").append(playerShorts).append(" | OSI (Available): ")
                    .append(osiShorts);
        }

        return report.toString();
    }

    private void executeLiquidationSale(PublicCompany target, PublicCompany predator, int finalBid) {
        net.sf.rails.common.ReportBuffer.add(this, "--- LIQUIDATION EVENT: " + target.getId() + " ---");
        net.sf.rails.common.ReportBuffer.add(this, "Pre-Liquidation State:\n" + buildCompanyStateReport(target));

        Player president = target.getPresident();
        int setAsideCash = target.getCash();
        int targetLoans = (target instanceof PublicCompany_1817) ? ((PublicCompany_1817) target).getNumberOfBonds() : 0;

        log.info("LIQUIDATION: President " + (president != null ? president.getName() : "None") + " sets aside $"
                + setAsideCash + " and " + targetLoans + " loans.");
        if (president != null) {
            net.sf.rails.common.ReportBuffer.add(this, president.getName() + " sets aside $" + setAsideCash + " and "
                    + targetLoans + " loans to settle debts.");
        }

        if (setAsideCash > 0) {
            net.sf.rails.game.state.Currency.wire(target, setAsideCash, gameManagerRef.getRoot().getBank());
        }

        // Rule 7.2.4 Step 6: Treasury shares to Open Market, zero compensation
        for (PublicCertificate cert : new java.util.ArrayList<>(target.getCertificates())) {
            if (cert.getOwner() == target && !cert.isPresidentShare()) {
                cert.moveTo(gameManagerRef.getRoot().getBank().getPool());
            }
        }

        // Rule 7.2.4 Steps 5 & 6: Asset Transfer
        if (predator != null) {
            net.sf.rails.common.ReportBuffer.add(this,
                    predator.getId() + " buys assets of " + target.getId() + " for $" + finalBid + ".");

            // 1. Identify special privates before they are moved
            boolean targetHasTrainStation = false;
            for (net.sf.rails.game.PrivateCompany p : target.getPrivates()) {
                if ("STA80".equals(p.getId())) {
                    targetHasTrainStation = true;
                    break;
                }
            }
            // 2. Use framework-validated bulk transfer for trains and privates
            predator.transferAssetsFrom(target);

            if (targetHasTrainStation && predator instanceof PublicCompany_1817) {
                ((PublicCompany_1817) predator).addTokenCapacity(1);
                net.sf.rails.common.ReportBuffer.add(this,
                        predator.getId() + " receives an extra station marker from the acquired Train Station.");
            }

            List<net.sf.rails.game.BaseToken> targetTokens = new java.util.ArrayList<>(target.getLaidBaseTokens());
            int overlappingMarkers = 0;
            for (net.sf.rails.game.BaseToken targetToken : targetTokens) {
                if (targetToken.getOwner() instanceof net.sf.rails.game.Stop) {
                    net.sf.rails.game.Stop stop = (net.sf.rails.game.Stop) targetToken.getOwner();
                    boolean hexHasPredatorToken = false;
                    for (net.sf.rails.game.Stop s : stop.getHex().getStops()) {
                        if (s.hasTokenOf(predator)) {
                            hexHasPredatorToken = true;
                            break;
                        }
                    }
                    targetToken.moveTo(target);
                    if (!hexHasPredatorToken) {
                        if (predator instanceof PublicCompany_1817) {
                            ((PublicCompany_1817) predator).addTokenCapacity(1);
                        }
                        net.sf.rails.game.BaseToken newToken = predator.getNextBaseToken();
                        if (newToken != null)
                            newToken.moveTo(stop);

                    } else {
                        overlappingMarkers++;
                    }
                }
            }

            if (overlappingMarkers > 0) {
                net.sf.rails.common.ReportBuffer.add(this, "MARKER CONFLICT: " + overlappingMarkers
                        + " overlapping marker(s) removed and returned to charter.");
            }

            enforceTrainLimit(predator);

            // Predator takes new loans if short on cash to pay final bid
            if (predator instanceof PublicCompany_1817) {
                PublicCompany_1817 p1817 = (PublicCompany_1817) predator;
                int loansTaken = 0;
                while (p1817.getCash() < finalBid && p1817.getNumberOfBonds() < p1817.getShareCount()) {
                    p1817.setNumberOfBonds(p1817.getNumberOfBonds() + 1);
                    p1817.addCashFromBank(100, gameManagerRef.getRoot().getBank());
                    loansTaken++;
                    if (gameManagerRef.getRoot().getStockMarket() instanceof StockMarket_1817) {
                        ((StockMarket_1817) gameManagerRef.getRoot().getStockMarket()).moveLeftOrDown(p1817, 1);
                    }
                }
                if (loansTaken > 0) {
                    String loanText = (loansTaken == 1) ? "1 loan" : loansTaken + " loans";
                    net.sf.rails.common.ReportBuffer.add(this,
                            p1817.getId() + " automatically takes " + loanText + " to fund the purchase.");
                }
            }
            net.sf.rails.game.state.Currency.wire(predator, finalBid, gameManagerRef.getRoot().getBank());
        } else {
            net.sf.rails.common.ReportBuffer.add(this,
                    "Bank buys assets of " + target.getId() + " for $0. Assets removed from game.");
            for (net.sf.rails.game.BaseToken targetToken : new java.util.ArrayList<>(target.getLaidBaseTokens())) {
                targetToken.moveTo(target);
            }

            log.info("LIQUIDATION: Attempting to remove " + target.getTrains().size() + " trains from play.");
            for (net.sf.rails.game.Train t : new java.util.ArrayList<>(target.getTrains())) {
                try {
                    t.moveTo(gameManagerRef.getRoot().getBank().getScrapHeap());
                } catch (IllegalArgumentException e) {
                    log.warn("LIQUIDATION: Bypassing crash. Leaving train " + t.getName() + " on closed charter.");
                }
            }

            for (net.sf.rails.game.PrivateCompany p : new java.util.ArrayList<>(target.getPrivates())) {
                p.setClosed();
            }
        }

        // Rule 7.2.4 Step 8: Presidential Debt Settlement
        int totalFunds = setAsideCash + finalBid;
        int totalDebt = targetLoans * 100;
        int surplus = 0;

        if (totalFunds >= totalDebt) {
            surplus = totalFunds - totalDebt;
            log.info("LIQUIDATION: Debts cleared. Surplus: $" + surplus);
            if (targetLoans > 0 && target instanceof PublicCompany_1817) {
                ((PublicCompany_1817) target).setNumberOfBonds(0);
            }
        } else {
            int deficit = totalDebt - totalFunds;
            log.warn("LIQUIDATION: Deficit of $" + deficit + ". President must pay.");
            if (president != null) {
                if (president.getCash() >= deficit) {
                    net.sf.rails.game.state.Currency.wire(president, deficit, gameManagerRef.getRoot().getBank());
                    net.sf.rails.common.ReportBuffer.add(this,
                            president.getName() + " pays deficit of $" + deficit + " out of pocket.");
                } else {
                    log.error("CASH CRISIS: " + president.getName() + " cannot cover deficit of $" + deficit);
                    gameManagerRef.startShareSellingRound(president, deficit, target, true);
                    return;
                }
            }
            if (targetLoans > 0 && target instanceof PublicCompany_1817) {
                ((PublicCompany_1817) target).setNumberOfBonds(0);
            }
        }

        // Rule 7.2.4 Step 9: Shareholder Settlement
        int shareCount = ((PublicCompany_1817) target).getShareCount();
        int payoutPerShare = surplus / shareCount;
        net.sf.rails.common.ReportBuffer.add(this, "LIQUIDATION: Surplus per share payout is $" + payoutPerShare + ".");

        settleWithShareholders(target, payoutPerShare);

        ((PublicCompany_1817) target).resetForReuse();
        if (predator != null) {
            net.sf.rails.common.ReportBuffer.add(this, "Final Post-Event State:\n" + buildCompanyStateReport(predator));

            if (enforceTrainLimit(predator)) {
                log.info("LIQUIDATION: Predator {} exceeds limit. Pausing for discard.", predator.getId());
                operatingCompany.set(predator);
                stepToResume.set(MaAStep.NEXT_COMPANY);
                currentStep.set(MaAStep.DISCARD_TRAINS);
                return;
            }
        }

        companyIndex.set(companyIndex.value() + 1);
        processNextSale();
    }

    private void setupDiscardActions(PublicCompany comp) {
        for (net.sf.rails.game.Train train : comp.getTrains()) {
            possibleActions.add(new rails.game.action.DiscardTrain(comp, train));
        }
    }

}