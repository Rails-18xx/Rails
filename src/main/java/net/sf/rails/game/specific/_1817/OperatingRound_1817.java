package net.sf.rails.game.specific._1817;

import net.sf.rails.game.OperatingRound;
import net.sf.rails.game.GameManager;
import net.sf.rails.game.BonusToken;
import net.sf.rails.game.Company;
import net.sf.rails.game.PublicCompany;
import net.sf.rails.game.MapHex;
import net.sf.rails.game.special.SpecialBaseTokenLay;
import net.sf.rails.game.special.SpecialProperty;
import net.sf.rails.game.special.SpecialTileLay;

import rails.game.action.LayTile;
import rails.game.action.PossibleAction;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import net.sf.rails.game.StartRound;
import net.sf.rails.game.StartItem;
import net.sf.rails.game.Player;
import net.sf.rails.game.RailsItem;
import net.sf.rails.game.financial.Bank;
import net.sf.rails.game.model.BondsModel;
import net.sf.rails.common.LocalText;
import net.sf.rails.common.DisplayBuffer;
import net.sf.rails.common.ReportBuffer;
import net.sf.rails.common.GameOption;

import rails.game.action.BuyStartItem;
import rails.game.action.StartItemAction;
import rails.game.action.NullAction;

import java.util.ArrayList;
import java.util.Collection;
import java.util.List;

/**
 * Operating Round specifically for 1830.
 * Handles the "Use it or Lose it" rule for the Delaware & Hudson private
 * company.
 */
public class OperatingRound_1817 extends OperatingRound {

    private static final Logger log = LoggerFactory.getLogger(OperatingRound_1817.class);

    // State to track the loan blackout period (between paying interest and repaying
    // loans)
    protected final net.sf.rails.game.state.BooleanState loanBlackoutPeriod = new net.sf.rails.game.state.BooleanState(
            this, "loanBlackoutPeriod", false);

    protected final net.sf.rails.game.state.IntegerState tilesLaidThisTurn = net.sf.rails.game.state.IntegerState
            .create(this, "tilesLaidThisTurn", 0);
    protected final net.sf.rails.game.state.IntegerState upgradesThisTurn = net.sf.rails.game.state.IntegerState
            .create(this, "upgradesThisTurn", 0);
    protected final net.sf.rails.game.state.ArrayListState<MapHex> hexesLaidThisTurn = new net.sf.rails.game.state.ArrayListState<>(
            this, "hexesLaidThisTurn");
    private String lastLaidTileColour = null;
    private final java.util.Map<String, Integer> hexBaseCosts = new java.util.HashMap<>();

    protected final net.sf.rails.game.state.BooleanState trainBuyingDone = new net.sf.rails.game.state.BooleanState(
            this, "trainBuyingDone", false);

    protected final net.sf.rails.game.state.BooleanState interestPaidThisTurn = new net.sf.rails.game.state.BooleanState(
            this, "interestPaidThisTurn", false);
    protected final net.sf.rails.game.state.BooleanState repayPhaseDoneThisTurn = new net.sf.rails.game.state.BooleanState(
            this, "repayPhaseDoneThisTurn", false);

    // 1817 Financial Sequence Flags
    protected final net.sf.rails.game.state.BooleanState loansRepaidThisTurn = new net.sf.rails.game.state.BooleanState(
            this, "loansRepaidThisTurn", false);

    private static final String[] BRIDGE_CITIES = { "H3", "G6", "H9" };

    public OperatingRound_1817(GameManager gameManager, String roundId) {
        super(gameManager, roundId);
    }

    @Override
    public void start() {
        super.start();

        // 1817 Rule 1.2.5: The loan fee is set at the start of each operating round.
        if (gameManager instanceof GameManager_1817) {
            net.sf.rails.game.model.BondsModel bm = ((GameManager_1817) gameManager).getBondsModel();
            if (bm instanceof BondsModel_1817) {
                ((BondsModel_1817) bm).updateInterestRate();
            }
        }

    }

    @Override
    protected void finishTurn() {
        // This method is called when the player clicks "Done" or the turn is forced to
        // end.

        // Rule 6.10: Check for Liquidation if no trains
        PublicCompany comp = operatingCompany.value();
        if (comp != null && !comp.isClosed() && comp.getPortfolioModel().getNumberOfTrains() == 0) {
            net.sf.rails.common.ReportBuffer.add(this,
                    comp.getId() + " ends its turn without a train and goes into liquidation.");
            handleImmediateLiquidation(
                    new net.sf.rails.game.specific._1817.action.LiquidateCompany_1817(getRoot(), comp.getId(), 0));
        }

        super.finishTurn();
    }

    /**
     * Helper to check if the current company has a token on the specified hex name.
     */
    private boolean hasTokenOnHex(Company company, String hexName) {
        if (company == null)
            return false;
        // gameManager is protected in the superclass, so we can access it directly
        MapHex hex = gameManager.getRoot().getMapManager().getHex(hexName);
        if (hex == null)
            return false;

        // Use hasTokenOfCompany, not hasStation
        return hex.hasTokenOfCompany((PublicCompany) company);
    }

    /**
     * Finds the SpecialBaseTokenLay property for D&H and marks it as exercised.
     */
    private void expireDhTokenAbility(Company company) {
        Collection<SpecialProperty> specials = company.getSpecialProperties();

        if (specials == null)
            return;

    }

    @Override
    protected boolean gameSpecificTileLayAllowed(PublicCompany company, MapHex hex, int orientation) {
        if (!super.gameSpecificTileLayAllowed(company, hex, orientation)) {
            return false;
        }
        // Rule 6.3: A Company may not lay a yellow tile and upgrade it during the same
        // turn.
        if (hexesLaidThisTurn.contains(hex)) {
            return false;
        }

        if (hasCoalMineToken(hex)) {
            return false;
        }
        return true;
    }

    @Override
    public int getTileLayCost(PublicCompany company, MapHex hex, int standardCost) {
        int cost = super.getTileLayCost(company, hex, standardCost);
        // Capture the raw terrain cost for internal tracking
        hexBaseCosts.put(hex.getId(), standardCost);

        // Rule 6.3: Second tile operation costs an additional $20.
        if (tilesLaidThisTurn.value() > 0) {
            cost += 20;
        }

        // Rule 1.2.6: Waive $15 mountain fee upfront if Coal Mine is available.
        if (canPotentiallyLayCoalMine(company, hex, standardCost)) {
            cost -= 15;
        }

        // Rule 1.2.6: Bridge companies allow laying yellow on rivers ($10) for free
        
        if (standardCost == 10) {
            for (net.sf.rails.game.PrivateCompany priv : company.getPortfolioModel().getPrivateCompanies()) {
                if ("OBC40".equals(priv.getId()) || "UBC80".equals(priv.getId())) {
                    cost -= 10;
                    break;
                }
            }
        }

        return cost;
    }

    @Override
    public boolean layTile(LayTile action) {
        MapHex hex = action.getChosenHex();

        lastLaidTileColour = null;
        // super.layTile() calls registerNormalTileLay() -> updateAllowedTileColours()
        boolean success = super.layTile(action);

        if (success && action.getType() != LayTile.CORRECTION) {
            // Rule 6.3: Prevent upgrading the exact same hex twice in one turn.
            hexesLaidThisTurn.add(hex);

            SpecialProperty specialProp = action.getSpecialProperty();
            if (specialProp != null && specialProp.getParent() != null) {
                if ("PSM40".equals(specialProp.getParent().getId())) {
                    specialProp.setExercised(true);
                    tilesLaidThisTurn.set(tilesLaidThisTurn.value() + 1);
                }
            }

            // Rule 1.2.6: Mountain Engineers Bonus
            if ("Yellow".equalsIgnoreCase(lastLaidTileColour)) {
                PublicCompany activeCompany = operatingCompany.value();
                Integer baseCost = hexBaseCosts.get(hex.getId());

                // Using the intercepted standardCost = 15 to identify mountains
                if (baseCost != null && baseCost == 15) {
                    for (net.sf.rails.game.PrivateCompany priv : activeCompany.getPortfolioModel()
                            .getPrivateCompanies()) {
                        if ("MNE40".equals(priv.getId())) {
                            int bonus = 20;

                            net.sf.rails.common.ReportBuffer.add(gameManager,
                                    activeCompany.getId() + " receives $" + bonus
                                            + " from Mountain Engineers for tile on " + hex.getId());
                            if (activeCompany instanceof PublicCompany_1817) {
                                ((PublicCompany_1817) activeCompany).addCashFromBank(bonus,
                                        gameManager.getRoot().getBank());
                            }
                        }
                    }
                }
            }

            // The UI's MapWindow click listener checks the base class's
            // normalTileLaidThisTurn flag.
            // If true, it swallows mouse clicks, assuming the normal lay step is over.
            // We must reset this flag to trick the UI into accepting the second click.
            if (tilesLaidThisTurn.value() < 2) {
                normalTileLaidThisTurn.set(false);
            }
        }

        return success;
    }

    @Override
    protected void updateAllowedTileColours(String colour, int oldAllowedNumber) {

        lastLaidTileColour = colour;
        // 1. Update Rule 6.3 counters based on the tile JUST laid.
        // 'colour' is provided by the base engine as the color of the placed tile.
        if (!"Yellow".equalsIgnoreCase(colour)) {
            upgradesThisTurn.set(upgradesThisTurn.value() + 1);
        }
        tilesLaidThisTurn.set(tilesLaidThisTurn.value() + 1);

        // 2. Clear the map to define the NEXT operation's limits.
        tileLaysPerColour.clear();

        // 3. If we've only used one operation, allow a second one.
        if (tilesLaidThisTurn.value() < 2) {
            // Bypassing base engine case-sensitivity inconsistencies
            tileLaysPerColour.put("yellow", 1);
            tileLaysPerColour.put("Yellow", 1);

            if (upgradesThisTurn.value() == 0) {
                net.sf.rails.game.Phase currentPhase = net.sf.rails.game.Phase.getCurrent(this);
                if (currentPhase.isTileColourAllowed("Green") || currentPhase.isTileColourAllowed("green")) {
                    tileLaysPerColour.put("Green", 1);
                    tileLaysPerColour.put("green", 1);
                }
                if (currentPhase.isTileColourAllowed("Brown") || currentPhase.isTileColourAllowed("brown")) {
                    tileLaysPerColour.put("Brown", 1);
                    tileLaysPerColour.put("brown", 1);
                }
                if (currentPhase.isTileColourAllowed("gray") || currentPhase.isTileColourAllowed("gray")) {
                    tileLaysPerColour.put("gray", 1);
                    tileLaysPerColour.put("gray", 1);
                }
            }
        }

    }

    @Override
    public boolean processGameSpecificAction(PossibleAction action) {
        if (action instanceof net.sf.rails.game.specific._1817.action.DeclineCoalToken_1817) {
            if (offeringCoalMineHex.value() != null) {
                // Rule 1.2.6: If the token is declined, the deferred $15 fee must now be paid.
                PublicCompany comp = operatingCompany.value();
                if (comp != null) {
                    net.sf.rails.game.state.Currency.toBank(comp, 15);
                    net.sf.rails.common.ReportBuffer.add(this,
                            comp.getId() + " declines Coal Mine placement and pays the deferred $15 mountain fee.");
                }
                resolvedCoalMineHexes.add(offeringCoalMineHex.value());
                offeringCoalMineHex.set(null); // Release the interrupt lock
            }
            return true;
        }
        if (action instanceof net.sf.rails.game.specific._1817.action.LayCoalToken_1817) {
            net.sf.rails.game.specific._1817.action.LayCoalToken_1817 coalAction = (net.sf.rails.game.specific._1817.action.LayCoalToken_1817) action;
            PublicCompany comp = companyManager.getPublicCompany(coalAction.getCompanyId());
            MapHex hex = gameManager.getRoot().getMapManager().getHex(coalAction.getHexId());

            if (comp != null && hex != null) {

                // Identify the specific coal mine private company to use as the token's parent
                net.sf.rails.game.PrivateCompany coalPrivate = getAvailableCoalMine(comp);

                if (coalPrivate != null) {
                    // Use the factory to ensure a unique internal URI/ID
                    net.sf.rails.game.BonusToken coalToken = net.sf.rails.game.BonusToken.create(coalPrivate);

                    if (coalToken != null) {
                        // The name "CoalMine" is what the SVG renderer and Modifier look for
                        coalToken.setName("CoalMine");
                        coalToken.setValue(10);
                        // Explicitly direct the coal mine to the 'CoalMine' slot

                        hex.layBonusToken(coalToken, gameManager.getRoot().getPhaseManager());
                    }
                }

                net.sf.rails.common.ReportBuffer
                        .add(this, comp.getId() + " places a Coal Mine on " + hex.getId() + " and is refunded $15.");

                // Release the interrupt lock
                if (offeringCoalMineHex.value() != null) {
                    resolvedCoalMineHexes.add(offeringCoalMineHex.value());
                    offeringCoalMineHex.set(null);
                }

                return true;
            }

            return false;
        }

        if (action instanceof net.sf.rails.game.specific._1817.action.TakeLoans_1817) {
            net.sf.rails.game.specific._1817.action.TakeLoans_1817 tlAction = (net.sf.rails.game.specific._1817.action.TakeLoans_1817) action;
            PublicCompany comp = companyManager.getPublicCompany(tlAction.getCompanyId());

            int count = tlAction.getLoansToTake();

            if (comp.isClosed() || (comp.hasStockPrice() && comp.getCurrentSpace().getPrice() == 0)) {
                log.error("1817_ERROR: Company {} in liquidation cannot take loans.", comp.getId());
                return false;
            }

            // Rule 6.1: Cannot take loans between paying interest and repaying loans
            if (interestPaidThisTurn.value() && !repayPhaseDoneThisTurn.value()) {
                log.error("1817_ERROR: Company {} attempted to take loans during the blackout period.", comp.getId());
                return false;
            }

            if (comp instanceof net.sf.rails.game.specific._1817.PublicCompany_1817) {
                net.sf.rails.game.specific._1817.PublicCompany_1817 comp1817 = (net.sf.rails.game.specific._1817.PublicCompany_1817) comp;

                // Execute the centralized loan logic
                comp1817.executeLoan();

                net.sf.rails.common.ReportBuffer.add(this, comp.getId() + " took 1 loan.");
                return true;
            }
            return false;
            // --- END FIX ---
        }

        if (action instanceof net.sf.rails.game.specific._1817.action.LayBridgeToken_1817) {
            net.sf.rails.game.specific._1817.action.LayBridgeToken_1817 ba = (net.sf.rails.game.specific._1817.action.LayBridgeToken_1817) action;
            PublicCompany comp = companyManager.getPublicCompany(ba.getCompanyId());

            MapHex hex = gameManager.getRoot().getMapManager().getHex(ba.getHexId());

            // Find the correct private company with inventory
            net.sf.rails.game.PrivateCompany bridgePriv = null;
            for (net.sf.rails.game.PrivateCompany p : comp.getPortfolioModel().getPrivateCompanies()) {

                String pid = p.getId();
                // Aligning with XML IDs: OBC40 and UBC80
                if ("OBC40".equals(pid) || "UBC80".equals(pid)) {
                    bridgePriv = p;
                    break;
                }
            }

            if (bridgePriv != null && hex != null) {
                net.sf.rails.game.BonusToken bridgeToken = net.sf.rails.game.BonusToken.create(bridgePriv);
                bridgeToken.setName("Bridge");
                bridgeToken.setValue(10);
                hex.layBonusToken(bridgeToken, gameManager.getRoot().getPhaseManager());

                net.sf.rails.common.ReportBuffer.add(this, comp.getId() + " places a Bridge Token on " + hex.getId());
                return true;
            }

            return false;
        }

        if (action instanceof net.sf.rails.game.specific._1817.action.PayLoanInterest_1817) {
            net.sf.rails.game.specific._1817.action.PayLoanInterest_1817 payAction = (net.sf.rails.game.specific._1817.action.PayLoanInterest_1817) action;
            PublicCompany comp = companyManager.getPublicCompany(payAction.getCompanyName());

            if (comp.getCash() >= payAction.getInterestDue()) {
                net.sf.rails.game.state.Currency.toBank(comp, payAction.getInterestDue());
                net.sf.rails.common.ReportBuffer.add(this,
                        comp.getId() + " pays $" + payAction.getInterestDue() + " in loan interest.");
                interestPaidThisTurn.set(true);
                return true;
            } else {
                log.error("1817_ERROR: {} does not have enough cash (${}) to pay interest (${}).", comp.getId(),
                        comp.getCash(), payAction.getInterestDue());
            }
            return false;
        }

        if (action instanceof net.sf.rails.game.specific._1817.action.RepayLoans_1817) {
            net.sf.rails.game.specific._1817.action.RepayLoans_1817 rlAction = (net.sf.rails.game.specific._1817.action.RepayLoans_1817) action;
            net.sf.rails.game.PublicCompany comp = companyManager.getPublicCompany(rlAction.getCompanyId());
            int count = rlAction.getLoansToRepay();

            if (count > 0) {
                int cost = count * 100;
                if (comp.getCash() >= cost) {
                    net.sf.rails.game.state.Currency.toBank(comp, cost);
                    if (comp instanceof net.sf.rails.game.specific._1817.PublicCompany_1817) {
                        net.sf.rails.game.specific._1817.PublicCompany_1817 comp1817 = (net.sf.rails.game.specific._1817.PublicCompany_1817) comp;
                        comp1817.setNumberOfBonds(comp1817.getNumberOfBonds() - count);

                        // Rule 6.9: Stock price is moved one space to the right as each loan is paid
                        // off.
                        net.sf.rails.game.financial.StockMarket market = getRoot().getStockMarket();
                        net.sf.rails.game.financial.StockSpace space = comp.getCurrentSpace();


if (market instanceof net.sf.rails.game.specific._1817.StockMarket_1817) {
                            net.sf.rails.game.specific._1817.StockMarket_1817 m1817 = (net.sf.rails.game.specific._1817.StockMarket_1817) market;
                            int row = space.getRow();
                            int col = space.getColumn();

                            for (int i = 0; i < count; i++) {
                                log.info("1817_OR: RepayLoans tracking movement. Current space: row={}, col={}", row, col);
                                int nextRow = row + 1;
                                while (nextRow < m1817.getNumberOfRows() && m1817.getStockSpace(nextRow, col) == null) {
                                    nextRow++;
                                }
                                if (nextRow < m1817.getNumberOfRows() && m1817.getStockSpace(nextRow, col) != null) {
                                    row = nextRow;
                                    log.info("1817_OR: Found valid rightward space at row={}, col={}", row, col);
                                } else {
                                    log.warn("1817_OR: No valid rightward (upward) space found from row {}", row);
                                    break;
                                }
                            }

                            net.sf.rails.game.financial.StockSpace target = m1817.getStockSpace(row, col);
                            if (target != null && target != space) {
                                m1817.correctStockPrice(comp, target);
                                net.sf.rails.common.ReportBuffer.add(this, 
                                    comp.getId() + " stock price moves from $" + space.getPrice() + " to $" + target.getPrice() + " due to loan repayment.");
                            }
                        }









                        
                    }
                    net.sf.rails.common.ReportBuffer.add(this,
                            comp.getId() + " repays " + count + " loan(s) for $" + cost + ".");
                    return true;
                } else {
                    log.error("1817_ERROR: {} lacks cash to repay {} loans. Needed: {}, Has: {}", comp.getId(), count,
                            cost, comp.getCash());
                }
            }
            return true;
        }
        if (action instanceof net.sf.rails.game.specific._1817.action.LiquidateCompany_1817) {
            return handleImmediateLiquidation((net.sf.rails.game.specific._1817.action.LiquidateCompany_1817) action);
        }

        return super.processGameSpecificAction(action);
    }

    private boolean hasCoalMineToken(MapHex hex) {
        if (hex == null || hex.getBonusTokens() == null)
            return false;
        for (BonusToken t : hex.getBonusTokens()) {

            if ("CoalMine".equals(t.getName()))
                return true;
        }
        return false;
    }

    @Override
    protected void initTurn() {
        super.initTurn();
        interestPaidThisTurn.set(false);
        repayPhaseDoneThisTurn.set(false);
        trainBuyingDone.set(false);
        tilesLaidThisTurn.set(0);
        upgradesThisTurn.set(0);
        hexesLaidThisTurn.clear();
        offeringCoalMineHex.set(null);

    }

    /**
     * Checks if the company is eligible for a mountain fee waiver based on owning
     * a coal mine with remaining token
     */
    private boolean canPotentiallyLayCoalMine(PublicCompany company, MapHex hex, int standardCost) {
        if (standardCost != 15)
            return false;
        if (getAvailableCoalMine(company) == null)
            return false;

        // NOTE: Future implementation should include geographic adjacency checks.
        return true;
    }

    @Override
    protected void privatesPayOut() {
        int count = 0;
        for (net.sf.rails.game.PrivateCompany priv : companyManager.getAllPrivateCompanies()) {
            if (!priv.isClosed()) {
                int revenue = priv.getRevenueByPhase(net.sf.rails.game.Phase.getCurrent(this));
                if (revenue > 0 && priv.getOwner() instanceof PublicCompany) {
                    PublicCompany comp = (PublicCompany) priv.getOwner();
                    if (!comp.isClosed()) {
                        boolean hasRealTrain = false;
                        if (comp.getPortfolioModel() != null && comp.getPortfolioModel().getTrainList() != null) {
                            for (net.sf.rails.game.Train t : comp.getPortfolioModel().getTrainList()) {
                                if (t.getName() != null && t.getName().matches(".*\\d.*")) {
                                    hasRealTrain = true;
                                    break;
                                }
                            }
                        }
                        if (hasRealTrain) {
                            if (count++ == 0)
                                net.sf.rails.common.ReportBuffer.add(this, "");
                            String revText = net.sf.rails.game.state.Currency.fromBank(revenue, comp);
                            net.sf.rails.common.ReportBuffer.add(this,
                                    net.sf.rails.common.LocalText.getText("ReceivesFor",
                                            comp.getId(), revText, priv.getId() + " (Mail Contract)"));
                        }
                    }
                }
            }
        }
    }

    @Override
    public boolean process(PossibleAction action) {
        if (action instanceof rails.game.action.NullAction) {
            rails.game.action.NullAction nullAction = (rails.game.action.NullAction) action;
            if (nullAction.getMode() == rails.game.action.NullAction.Mode.DONE
                    || nullAction.getMode() == rails.game.action.NullAction.Mode.SKIP) {
                if (getStep() == net.sf.rails.game.GameDef.OrStep.BUY_TRAIN) {

                    if (!trainBuyingDone.value()) {
                        trainBuyingDone.set(true);
                        return true; // Stay in OR to process interest
                    }

                    if (interestPaidThisTurn.value() && !repayPhaseDoneThisTurn.value()) {
                        repayPhaseDoneThisTurn.set(true);
                        return true; // Stay in OR for post-repayment loan window
                    }
                }
            }
        }
        if (action instanceof rails.game.action.SetDividend) {
            rails.game.action.SetDividend sd = (rails.game.action.SetDividend) action;
            PublicCompany comp = sd.getCompany();
            net.sf.rails.game.financial.StockSpace oldSpace = comp.getCurrentSpace();

            // 1. Record cash states before base engine messes it up
            int preCompCash = comp.getCash();
            net.sf.rails.game.financial.Bank bank = getRoot().getBank();
            List<net.sf.rails.game.Player> players = getRoot().getPlayerManager().getPlayers();
            java.util.Map<net.sf.rails.game.Player, Integer> prePlayerCash = new java.util.HashMap<>();
            for (net.sf.rails.game.Player p : players)
                prePlayerCash.put(p, p.getCash());

            // Execute the superclass logic ONCE to advance game state
            boolean result = super.process(sd);

            if (result && oldSpace != null) {
                log.info("1817_OR: Processing Dividend Movement for {}. Revenue: {}, Alloc: {}",
                        comp.getId(), sd.getActualRevenue(), sd.getRevenueAllocation());

                // 2. Revert base engine cash changes
                int diffComp = comp.getCash() - preCompCash;
                if (diffComp > 0)
                    net.sf.rails.game.state.Currency.toBank(comp, diffComp);
                else if (diffComp < 0)
                    net.sf.rails.game.state.Currency.wire(bank, -diffComp, comp);

                for (net.sf.rails.game.Player p : players) {
                    int diffP = p.getCash() - prePlayerCash.get(p);
                    if (diffP > 0)
                        net.sf.rails.game.state.Currency.toBank(p, diffP);
                    else if (diffP < 0)
                        net.sf.rails.game.state.Currency.wire(bank, -diffP, p);
                }

                // 3. Apply strict 1817 cash distributions
                int revenue = sd.getActualRevenue();
                int alloc = sd.getRevenueAllocation();
                int shareCount = (comp instanceof net.sf.rails.game.specific._1817.PublicCompany_1817)
                        ? ((net.sf.rails.game.specific._1817.PublicCompany_1817) comp).getShareCount()
                        : 10;

                int trueTreasuryAmount = 0;
                int truePerShare = 0;

                if (alloc == rails.game.action.SetDividend.PAYOUT) {
                    trueTreasuryAmount = 0;
                    truePerShare = revenue / shareCount;
                } else if (alloc == rails.game.action.SetDividend.SPLIT) {
                    if (shareCount == 10) {
                        // Rule 6.6: Round half-pay UP to nearest $10 for shareholders
                        // and DOWN to nearest $10 for treasury.
                        int half = revenue / 2;
                        int paidToShareholdersTotal = ((half + 9) / 10) * 10;
                        truePerShare = paidToShareholdersTotal / 10;
                        trueTreasuryAmount = revenue - paidToShareholdersTotal;
                    } else {
                        // 2-share and 5-share use standard integer division
                        trueTreasuryAmount = revenue / 2;
                        int totalToShares = revenue - trueTreasuryAmount;
                        truePerShare = totalToShares / shareCount;
                    }
                } else {
                    trueTreasuryAmount = revenue;
                    truePerShare = 0;
                }

                if (trueTreasuryAmount > 0) {
                    net.sf.rails.game.state.Currency.wire(bank, trueTreasuryAmount, comp);
                }

                // Pay treasury for its own shares
                int treasuryShares = 0;
                for (net.sf.rails.game.financial.PublicCertificate cert : comp.getCertificates()) {
                    if (cert.getOwner() == comp && !cert.isPresidentShare()) {
                        treasuryShares += cert.getShare() / comp.getShareUnit();
                    }
                }
                if (treasuryShares > 0 && truePerShare > 0) {
                    net.sf.rails.game.state.Currency.wire(bank, treasuryShares * truePerShare, comp);
                }

                for (net.sf.rails.game.Player p : players) {
                    int sharesOwned = 0;
                    int shortShares = 0;
                    for (net.sf.rails.game.financial.PublicCertificate cert : p.getPortfolioModel()
                            .getCertificates(comp)) {
                        if (cert instanceof net.sf.rails.game.specific._1817.ShortCertificate) {
                            shortShares += 1;
                        } else {
                            sharesOwned += cert.getShare() / comp.getShareUnit();
                        }
                    }

                    if (sharesOwned > 0 && truePerShare > 0) {
                        net.sf.rails.game.state.Currency.wire(bank, sharesOwned * truePerShare, p);
                    }
                    if (shortShares > 0 && truePerShare > 0) {
                        int debt = shortShares * truePerShare;
                        // Rule 7.4: Cash Crisis Trigger

                        // Deduct the cash first
                        net.sf.rails.game.state.Currency.toBank(p, debt);
                        net.sf.rails.common.ReportBuffer.add(this,
                                p.getName() + " pays $" + debt + " for short shares of " + comp.getId());

                        // Rule 7.4: Cash Crisis Trigger Check
                        if (p.getCash() < 0) {
                            log.warn("1817_OR: CASH CRISIS triggered for {}. Shortfall: ${}", p.getName(),
                                    Math.abs(p.getCash()));
                            net.sf.rails.common.ReportBuffer.add(this, "CASH CRISIS: " + p.getName()
                                    + " must raise funds. Shortfall: $" + Math.abs(p.getCash()));

                            // Interrupt the current round and spawn the Cash Crisis wrapper
                            CashCrisisRound_1817 crisisRound = gameManager.createRound(CashCrisisRound_1817.class,
                                    "CashCrisis_" + p.getId());
                            gameManager.setInterruptedRound(this);
                            crisisRound.start(p);
                        }

                    }
                }

                int paidToShareholders = truePerShare * shareCount;

                int refPrice = Math.max(40, oldSpace.getPrice());
                int moves = 0;
                if (paidToShareholders == 0)
                    moves = -1;
                else if (paidToShareholders >= refPrice * 2)
                    moves = 2;
                else if (paidToShareholders >= refPrice)
                    moves = 1;

                net.sf.rails.game.financial.StockMarket market = getRoot().getStockMarket();
                if (market instanceof net.sf.rails.game.specific._1817.StockMarket_1817) {
                    net.sf.rails.game.specific._1817.StockMarket_1817 m1817 = (net.sf.rails.game.specific._1817.StockMarket_1817) market;
                    int row = oldSpace.getRow();
                    int col = oldSpace.getColumn();

                    if (moves > 0) {
                        // Rule 6.6: Move RIGHT (increase col)[cite: 193, 636, 637].
                        for (int i = 0; i < moves; i++) {
                            int nextCol = col + 1;
                            while (nextCol < m1817.getNumberOfColumns() && m1817.getStockSpace(row, nextCol) == null) {
                                nextCol++;
                            }
                            if (nextCol < m1817.getNumberOfColumns() && m1817.getStockSpace(row, nextCol) != null) {
                                col = nextCol;
                            }
                        }
                    } else if (moves < 0) {
                        // Rule 6.6: Move LEFT (decrease col)
                        for (int i = 0; i < -moves; i++) {
                            int prevCol = col - 1;
                            while (prevCol >= 0 && m1817.getStockSpace(row, prevCol) == null) {
                                prevCol--;
                            }
                            if (prevCol >= 0) {
                                net.sf.rails.game.financial.StockSpace pot = m1817.getStockSpace(row, prevCol);
                                // Prevent moving into A1 unless specifically required by liquidation
                                if (pot != null && !pot.getId().equalsIgnoreCase("A1")) {
                                    col = prevCol;
                                }
                            }
                        }
                    }

                    net.sf.rails.game.financial.StockSpace target = m1817.getStockSpace(row, col);
                    if (target != null && oldSpace != target) {
                        m1817.correctStockPrice(comp, target);
                        // --- DELETE --- net.sf.rails.game.financial.Bank.format(this, ...)
                        net.sf.rails.common.ReportBuffer.add(this,
                                comp.getId() + " stock price moves from $" + oldSpace.getPrice() + " to $"
                                        + target.getPrice() + ".");
                    }

                }

            }
            net.sf.rails.game.PhaseManager pm = getRoot().getPhaseManager();
            if (pm.hasReachedPhase("4")) {
                java.util.Set<net.sf.rails.game.Train> companyTrains = comp.getPortfolioModel().getTrainList();
                java.util.List<net.sf.rails.game.Train> toDiscard = new java.util.ArrayList<>();

                for (net.sf.rails.game.Train t : companyTrains) {
                    if (t.getName() != null && t.getName().contains("2+")) {
                        toDiscard.add(t);
                    }
                }

                for (net.sf.rails.game.Train t : toDiscard) {
                    getRoot().getTrainManager().trashTrain(t);
                    log.info("1817_OR: 2+ train ({}) belonging to {} rusted and was removed from play.",
                            t.getId(), comp.getId());
                    net.sf.rails.common.ReportBuffer.add(this,
                            comp.getId() + " discards rusted 2+ train after its final run.");
                }
            }
            return result;
        }

        return super.process(action);
    }

    public boolean setPossibleActions() {

        boolean actionsAdded = false;
        PublicCompany comp = operatingCompany.value();
        net.sf.rails.game.GameDef.OrStep step = getStep();

        // --- 2. COAL MINE INTERRUPT ---
        if ("Yellow".equalsIgnoreCase(lastLaidTileColour) && !hexesLaidThisTurn.isEmpty()) {
            MapHex lastHex = hexesLaidThisTurn.get(hexesLaidThisTurn.size() - 1);
            if (offeringCoalMineHex.value() == null && !resolvedCoalMineHexes.contains(lastHex.getId())) {
                Integer baseCost = hexBaseCosts.get(lastHex.getId());
                if (baseCost != null && baseCost == 15 && !hasCoalMineToken(lastHex)) {
                    if (getAvailableCoalMine(comp) != null) {
                        offeringCoalMineHex.set(lastHex.getId());
                    } else {
                        resolvedCoalMineHexes.add(lastHex.getId());
                    }
                } else {
                    resolvedCoalMineHexes.add(lastHex.getId());
                }
            }
        }

        // Force the interrupt if a coal mine is being offered
        if (offeringCoalMineHex.value() != null) {
            possibleActions.clear();
            possibleActions.add(new net.sf.rails.game.specific._1817.action.LayCoalToken_1817(getRoot(), comp.getId(),
                    offeringCoalMineHex.value()));

            // If the company has less than $15, they MUST place the coal token to waive the
            // fee.
            if (comp.getCash() >= 15) {
                possibleActions.add(new net.sf.rails.game.specific._1817.action.DeclineCoalToken_1817(getRoot()));
            }
            return true;
        }

        actionsAdded |= super.setPossibleActions();

        // 1817 Rule 6.6: Companies may pay full, pay half, or withhold dividends.
        for (rails.game.action.PossibleAction pa : possibleActions.getList()) {
            if (pa instanceof rails.game.action.SetDividend) {
                rails.game.action.SetDividend sd = (rails.game.action.SetDividend) pa;
                int[] currentAllocs = sd.getAllowedRevenueAllocations();
                boolean hasSplit = false;
                if (currentAllocs != null) {
                    for (int alloc : currentAllocs) {
                        if (alloc == rails.game.action.SetDividend.SPLIT) {
                            hasSplit = true;
                            break;
                        }
                    }
                }
                if (!hasSplit) {
                    int len = (currentAllocs == null) ? 0 : currentAllocs.length;
                    int[] newAllocs = new int[len + 1];
                    if (currentAllocs != null) {
                        System.arraycopy(currentAllocs, 0, newAllocs, 0, len);
                    }
                    newAllocs[len] = rails.game.action.SetDividend.SPLIT;
                    sd.setAllowedRevenueAllocations(newAllocs);
                }
            }
        }

        // --- 4. BRIDGE ACTIONS ---
        // Evaluation moved here to prevent erasure by super.setPossibleActions()
        java.util.Collection<net.sf.rails.game.PrivateCompany> privates = comp.getPortfolioModel()
                .getPrivateCompanies();
        if (privates != null && !privates.isEmpty()) {
            for (net.sf.rails.game.PrivateCompany p : privates) {
                String pid = p.getId();
                if ("OBC40".equals(pid) || "UBC80".equals(pid)) {
                    int placedCount = getPlacedBridgeCount(pid);
                    int limit = getBridgeLimit(pid);
                    if (placedCount < limit) {
                        for (String hexId : BRIDGE_CITIES) {
                            MapHex cityHex = gameManager.getRoot().getMapManager().getHex(hexId);
                            if (cityHex == null)
                                continue;
                            boolean bridgeExists = false;
                            if (cityHex.getBonusTokens() != null) {
                                for (net.sf.rails.game.BonusToken t : cityHex.getBonusTokens()) {
                                    if ("Bridge".equals(t.getName())) {
                                        bridgeExists = true;
                                        break;
                                    }
                                }
                            }
                            if (!bridgeExists) {
                                possibleActions.add(new net.sf.rails.game.specific._1817.action.LayBridgeToken_1817(
                                        getRoot(), comp.getId(), hexId));
                                actionsAdded = true;
                            }
                        }
                    }
                }
            }
        }

        // --- 5. 1817 FINANCIAL LOGIC ---
        // Only proceed for specific 1817 financial classes.
        if (!(comp instanceof net.sf.rails.game.specific._1817.PublicCompany_1817)) {
            return actionsAdded;
        }

        net.sf.rails.game.specific._1817.PublicCompany_1817 comp1817 = (net.sf.rails.game.specific._1817.PublicCompany_1817) comp;
        int currentLoans = comp1817.getNumberOfBonds();
        int maxLoans = comp1817.getShareCount();

        // Take Loans availability
        boolean inBlackout = (interestPaidThisTurn.value() && !repayPhaseDoneThisTurn.value());
        if (!inBlackout && currentLoans < maxLoans) {
            possibleActions.add(
                    new net.sf.rails.game.specific._1817.action.TakeLoans_1817(getRoot(), comp1817.getId()));
            actionsAdded = true;
        }

        if (step == net.sf.rails.game.GameDef.OrStep.BUY_TRAIN) {

            // Interest Phase
            if (trainBuyingDone.value() && !interestPaidThisTurn.value()) {
                if (currentLoans == 0) {
                    interestPaidThisTurn.set(true);
                } else {
                    possibleActions.clear();
                    net.sf.rails.game.model.BondsModel baseBm = ((GameManager_1817) gameManager).getBondsModel();
                    int interestPerLoan = (baseBm instanceof BondsModel_1817)
                            ? ((BondsModel_1817) baseBm).getInterestRate()
                            : 5;
                    int interestDue = currentLoans * interestPerLoan;

                    if (comp1817.getCash() >= interestDue) {
                        possibleActions.add(new net.sf.rails.game.specific._1817.action.PayLoanInterest_1817(getRoot(),
                                comp1817.getId(), interestDue));
                    } else {
                        possibleActions.add(new net.sf.rails.game.specific._1817.action.LiquidateCompany_1817(getRoot(),
                                comp1817.getId(), interestDue));
                    }
                    return true;
                }
            }

            // Repayment Phase
            if (interestPaidThisTurn.value() && !repayPhaseDoneThisTurn.value()) {
                if (currentLoans == 0 || comp1817.getCash() < 100) {
                    repayPhaseDoneThisTurn.set(true);
                } else {
                    possibleActions.clear();
                    int maxRepay = Math.min(currentLoans, comp1817.getCash() / 100);
                    possibleActions.add(new net.sf.rails.game.specific._1817.action.RepayLoans_1817(getRoot(),
                            comp1817.getId(), maxRepay));
                    possibleActions.add(new NullAction(getRoot(), NullAction.Mode.DONE));
                    return true;
                }
            }

            // Post-Repayment Final Window
            if (repayPhaseDoneThisTurn.value()) {
                for (rails.game.action.BuyTrain pa : possibleActions.getType(rails.game.action.BuyTrain.class)) {
                    possibleActions.remove(pa);
                }
                if (!possibleActions.contains(NullAction.class)) {
                    possibleActions.add(new NullAction(getRoot(), NullAction.Mode.DONE));
                }
            }
        }

        // 2. Inspect the generated actions
        for (rails.game.action.PossibleAction pa : possibleActions.getList()) {
            if (pa instanceof LayTile) {
                LayTile lt = (LayTile) pa;
                SpecialProperty sp = lt.getSpecialProperty();
                // Intentionally left blank; filtering is handled organically by the base engine
                // since connects="no" is already specified in CompanyManager.xml for PSM40_Lay.
            } else {
            }
        }

        return actionsAdded;
    }

    /**
     * Handles the immediate consequences of failing an interest payment (Rule 6.8).
     * The President pays the shortfall, and the marker moves to the liquidation
     * space.
     */
    private boolean handleImmediateLiquidation(net.sf.rails.game.specific._1817.action.LiquidateCompany_1817 action) {
        PublicCompany comp = companyManager.getPublicCompany(action.getCompanyName());
        if (comp == null)
            return false;

        int totalInterestDue = action.getShortfall();
        int compCash = comp.getCash();
        int playerShortfall = Math.max(0, totalInterestDue - compCash);
        net.sf.rails.game.Player president = (net.sf.rails.game.Player) comp.getPresident();

        // 1. Drain company treasury to pay what it can (Rule 6.8)
        if (compCash > 0) {
            net.sf.rails.game.state.Currency.toBank(comp, Math.min(compCash, totalInterestDue));
        }

        // 2. President personally pays the remaining shortfall (Rule 6.8)
        if (playerShortfall > 0 && president != null) {
            net.sf.rails.game.state.Currency.toBank(president, playerShortfall);
            net.sf.rails.common.ReportBuffer.add(gameManager,
                    president.getName() + " personally pays $" + playerShortfall + " interest shortfall for "
                            + comp.getId() + ".");
        }

        // 3. Move marker to the Red Liquidation space (price 0)
        net.sf.rails.game.financial.StockMarket market = (net.sf.rails.game.financial.StockMarket) getRoot()
                .getStockMarket();
        net.sf.rails.game.financial.StockSpace liquidationSpace = null;
        for (int r = 0; r < market.getNumberOfRows(); r++) {
            for (int c = 0; c < market.getNumberOfColumns(); c++) {
                net.sf.rails.game.financial.StockSpace ss = market.getStockSpace(r, c);
                if (ss != null && ss.getPrice() == 0) {
                    liquidationSpace = ss;
                    break;
                }
            }
            if (liquidationSpace != null)
                break;
        }

        if (liquidationSpace != null) {
            market.correctStockPrice(comp, liquidationSpace);
        } else {
            log.error("1817_ERROR: Could not find a StockSpace with price 0 for Liquidation.");
        }

        net.sf.rails.common.ReportBuffer.add(gameManager, comp.getId() + " is moved to liquidation.");

        // 5. Finalize the turn flags to prevent further actions
        interestPaidThisTurn.set(true);
        repayPhaseDoneThisTurn.set(true);
        trainBuyingDone.set(true);

        return true;
    }

    // State to track if the current tile lay is accompanied by a coal mine token
    // placement
    protected final net.sf.rails.game.state.BooleanState isLayingCoalMine = new net.sf.rails.game.state.BooleanState(
            this, "isLayingCoalMine", false);

    protected final net.sf.rails.game.state.StringState offeringCoalMineHex = net.sf.rails.game.state.StringState
            .create(
                    this, "offeringCoalMineHex", null);

    // State to ensure we don't ask about the same hex twice
    protected final net.sf.rails.game.state.ArrayListState<String> resolvedCoalMineHexes = new net.sf.rails.game.state.ArrayListState<>(
            this, "resolvedCoalMineHexes");

    private int getPlacedCoalTokensCount(String privId) {
        int count = 0;
        for (MapHex hex : gameManager.getRoot().getMapManager().getHexes()) {
            if (hex.getBonusTokens() != null) {
                for (net.sf.rails.game.BonusToken t : hex.getBonusTokens()) {
                    if (t.getParent() != null && privId.equals(t.getParent().getId())) {
                        count++;
                    }
                }
            }
        }
        return count;
    }

    private net.sf.rails.game.PrivateCompany getAvailableCoalMine(PublicCompany comp) {
        if (comp == null || comp.getPortfolioModel() == null)
            return null;
        for (net.sf.rails.game.PrivateCompany priv : comp.getPortfolioModel().getPrivateCompanies()) {
            if (priv != null) {
                String id = priv.getId();
                int limit = getCoalTokenLimit(id);

                if (limit > 0) {
                    if (getPlacedCoalTokensCount(id) < limit) {
                        return priv;
                    }
                }
            }
        }
        return null;
    }

    private int getBridgeLimit(String privId) {
        if ("OBC40".equals(privId))
            return 1; // Was "OHB"
        if ("UBC80".equals(privId))
            return 2; // Was "UNB"
        return 0;
    }

    private int getCoalTokenLimit(String privId) {
        if ("MNM30".equals(privId))
            return 1; // Was "MIN30"
        if ("CLM60".equals(privId))
            return 2; // Was "COA60"
        if ("MJM90".equals(privId))
            return 3; // Was "MJM90" (Correct)
        return 0;
    }

    private int getPlacedBridgeCount(String privId) {
        int count = 0;
        for (MapHex h : gameManager.getRoot().getMapManager().getHexes()) {
            if (h.getBonusTokens() != null) {
                for (net.sf.rails.game.BonusToken t : h.getBonusTokens()) {
                    // Check if token is named Bridge and belongs to this private
                    if ("Bridge".equals(t.getName()) && t.getParent() != null && privId.equals(t.getParent().getId())) {
                        count++;
                    }
                }
            }
        }
        return count;
    }

    @Override
    public void setBuyableTrains() {
        // 1. MUST call super first to populate core step actions (e.g., 'Done')
        super.setBuyableTrains();

        PublicCompany comp = operatingCompany.value();
        if (comp == null)
            return;

        net.sf.rails.game.PhaseManager pm = getRoot().getPhaseManager();
        int trainLimit = 2;
        if (!pm.hasReachedPhase("4")) {
            trainLimit = 4;
        } else if (!pm.hasReachedPhase("6")) {
            trainLimit = 3;
        }

        int currentTrains = comp.getPortfolioModel().getNumberOfTrains();
        boolean isAtCapacity = (currentTrains >= trainLimit);

        int cash = comp.getCash();
        List<PossibleAction> toRemove = new ArrayList<>();
        net.sf.rails.game.Player currentPresident = comp.getPresident();

        for (PossibleAction pa : possibleActions.getList()) {
            if (pa instanceof rails.game.action.BuyTrain) {
                rails.game.action.BuyTrain bt = (rails.game.action.BuyTrain) pa;

                // 2. Filter out ALL BuyTrain actions if at limit, leaving 'Done' intact
                if (isAtCapacity) {
                    toRemove.add(bt);
                    continue;
                }

                bt.setForcedBuyIfHasRoute(false);
                bt.setForcedBuyIfNoRoute(false);
                bt.setPresidentMustAddCash(0);

                // Rule 6.7: President may not contribute cash.
                // This is already set above, but we must ensure the engine doesn't
                // prompt for it if cash < price.
                if (bt.getFixedCost() > cash) {
                    toRemove.add(bt);
                    continue;
                }

                int cost = bt.getFixedCost();
                if (bt.getFixedCostMode() == rails.game.action.BuyTrain.Mode.FIXED ||
                        bt.getFixedCostMode() == rails.game.action.BuyTrain.Mode.MIN) {
                    if (cost > cash) {
                        toRemove.add(bt);
                    }
                } else if (bt.getFixedCostMode() != rails.game.action.BuyTrain.Mode.FREE) {
                    if (cash < 1) {
                        toRemove.add(bt);
                    }
                }
            }
        }

        for (PossibleAction pa : toRemove) {
            possibleActions.remove(pa);
        }

        // 3. Inject cross-company actions ONLY if space permits
        if (currentPresident != null && !isAtCapacity) {
            for (PublicCompany otherComp : gameManager.getAllPublicCompanies()) {
                if (otherComp != comp && otherComp.hasStarted() && !otherComp.isClosed()
                        && otherComp.getPresident() == currentPresident) {
                    for (net.sf.rails.game.Train t : otherComp.getPortfolioModel().getTrainList()) {
                        if (t.getName() != null && t.getName().matches(".*\\d.*")) {
                            boolean exists = false;
                            for (PossibleAction pa : possibleActions.getList()) {
                                if (pa instanceof rails.game.action.BuyTrain) {
                                    if (((rails.game.action.BuyTrain) pa).getTrain() == t) {
                                        exists = true;
                                        break;
                                    }
                                }
                            }
                            if (!exists && cash >= 1) {
                                rails.game.action.BuyTrain newBt = new rails.game.action.BuyTrain(t, otherComp, 1);
                                newBt.setFixedCostMode(rails.game.action.BuyTrain.Mode.MIN);
                                newBt.setFixedCost(1);
                                newBt.setForcedBuyIfHasRoute(false);
                                newBt.setForcedBuyIfNoRoute(false);
                                newBt.setPresidentMustAddCash(0);

                                String cleanTrainName = t.getName().split("_")[0];
                                String label = "Buy '" + cleanTrainName + "' from " + otherComp.getId();
                                newBt.setButtonLabel(label);
                                possibleActions.add(newBt);
                            }
                        }
                    }
                }
            }
        }
    }

    @Override
    public List<PublicCompany> setOperatingCompanies(List<PublicCompany> oldOperatingCompanies,
            PublicCompany lastOperatingCompany) {
        // 1817 strictly locks the operating order at the beginning of the round based
        // on
        // the GameManager's mathematically correct token-stacking algorithm.
        // It must not dynamically resort companies mid-round.
        List<PublicCompany> authoritativeOrder = gameManager.getCompaniesInRunningOrder();
        List<PublicCompany> activeCompanies = new ArrayList<>();

        for (PublicCompany comp : authoritativeOrder) {
            if (canCompanyOperateThisRound(comp)) {
                activeCompanies.add(comp);
            }
        }
        return activeCompanies;
    }

}