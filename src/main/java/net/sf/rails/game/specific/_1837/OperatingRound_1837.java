package net.sf.rails.game.specific._1837;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Set;

import net.sf.rails.game.*;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import net.sf.rails.common.DisplayBuffer;
import net.sf.rails.common.LocalText;
import net.sf.rails.common.ReportBuffer;
import net.sf.rails.game.financial.Bank;
import net.sf.rails.game.financial.NationalFormationRound;
import net.sf.rails.game.special.ExchangeForShare;
import net.sf.rails.game.special.SpecialProperty;
import net.sf.rails.game.state.BooleanState;
import net.sf.rails.game.state.Currency;
import net.sf.rails.game.state.MoneyOwner;
import net.sf.rails.util.SequenceUtil;
import rails.game.action.BuyTrain;
import rails.game.action.LayTile;
import rails.game.action.PossibleAction;
import rails.game.action.SetDividend;

import com.google.common.collect.HashBasedTable;
import com.google.common.collect.Iterables;
import com.google.common.collect.Table;
import rails.game.specific._1837.SetHomeHexLocation;


/**
 * @author Martin
 *
 */
public class OperatingRound_1837 extends OperatingRound {
    private static final Logger log = LoggerFactory.getLogger(OperatingRound_1837.class);

    private final BooleanState needHungaryFormationCall = new BooleanState(this, "NeedHungaryFormationCall");
    private final BooleanState needKuKFormationCall = new BooleanState(this, "NeedKuKFormationCall");

    /**
     * Registry of percentage of nationals revenue to be denied per player
     * because of having produced revenue in the same OR.
     */
    private final Table<Player, PublicCompany, Integer> deniedIncomeShare = HashBasedTable.create();

    public OperatingRound_1837(GameManager parent, String id) {
        super(parent, id);
    }


    @Override
    protected void newPhaseChecks() {
        Phase phase = Phase.getCurrent(this);

        if (phase.getId().equals("3")) {
            // Unblock the hexes blocked by private companies
            for(PrivateCompany comp:gameManager.getAllPrivateCompanies())  {
                comp.unblockHexes();
            }
            // Open the Bosnian territory
            MapManager map = getRoot().getMapManager();
            for (String bzhHex : GameDef_1837.BzHHexes.split(",")){
                map.getHex(bzhHex).setOpen(true);
            }
            String report = LocalText.getText("TerritoryIsOpened", "Bosnian");
            ReportBuffer.add (this, report);
            DisplayBuffer.add (this, report);

        } else if (phase.getId().equals("4")) {
            // Form the Südbahn, and exchange shares and tokens of its minors
            formSd();

            // Close the Italian territory
            MapManager map = getRoot().getMapManager();
            for (String itHex : GameDef_1837.ItalyHexes.split(",")){
                MapHex hex = map.getHex(itHex);
                hex.setOpen(false);
                hex.clear();
            }
            String report = LocalText.getText("TerritoryIsClosed", "Italian");
            ReportBuffer.add (this, report);
            DisplayBuffer.add (this, report);

            // Lay the new Bozen (Bolzano) tile
            LayTile action = new LayTile(getRoot(), LayTile.CORRECTION);
            MapHex hex = map.getHex(GameDef_1837.bozenHex);
            Tile tile = getRoot().getTileManager().getTile(GameDef_1837.newBozenTile);
            int orientation = GameDef_1837.newBozenTileOrientation;
            action.setChosenHex(hex);
            action.setLaidTile(tile);
            action.setOrientation(orientation);
            hex.upgrade (action);
            report = LocalText.getText("LaysTileAt", "Rails",
                            tile.getId(),
                            hex.getId(),
                            hex.getOrientationName(HexSide.get(orientation)));
            ReportBuffer.add (this, report);

        } else if (phase.getId().equals("4+1")
                && !companyManager.getPublicCompany(GameDef_1837.KK).hasStarted()
                && !NationalFormationRound.nationalIsComplete(gameManager,GameDef_1837.KK)) {
            if (getStep() == GameDef.OrStep.DISCARD_TRAINS) {
                // Postpone until trains are discarded
                needKuKFormationCall.set(true);
            } else {
                // Do it immediately
                ((GameManager_1837)gameManager).startKuKFormationRound (this);
            }
            if (phase.getId().equals("4E")
                    && !companyManager.getPublicCompany(GameDef_1837.Ug).hasStarted()
                    && !NationalFormationRound.nationalIsComplete(gameManager, GameDef_1837.Ug)) {
                if (getStep() == GameDef.OrStep.DISCARD_TRAINS) {
                    // Postpone until trains are discarded
                    needHungaryFormationCall.set(true);
                } else {
                    // Do it immediately
                    ((GameManager_1837)gameManager).startHungaryFormationRound (this);
                }
            }

        }

    }

    /**
     * Form the Südbahn, and exchange all shares and tokens of
     * its related minors by those of the Sd.
     */
    private void formSd() {
        PublicCompany sd = companyManager.getPublicCompany(GameDef_1837.Sd);
        PublicCompany sdMinor;
        for (String minorName : GameDef_1837.SdMinors.split(",")) {
            sdMinor = companyManager.getPublicCompany(minorName);
            boolean willBecomeMajorPresident = GameDef_1837.S1.equals(sdMinor.getId());
            MapHex minorHome = sdMinor.getHomeHexes().get(0);
            Stop minorStop = minorHome.getStopOfBaseToken(sdMinor);
            ((GameManager_1837)gameManager).mergeCompanies(sdMinor, sd,
                    willBecomeMajorPresident, true);
            minorHome.layBaseToken(sd, minorStop);
            sd.layBaseToken(sdMinor.getHomeHexes().get(0), 0);

        }
        floatCompany(sd); // Will also finance it
        sd.setOperated(); // May not operate this round
        // NOTE: this might need to be refined in case none of the Sd minors has been operated.
    }

    private void addIncomeDenialShare (Player player, PublicCompany company, int share) {

        if (!deniedIncomeShare.contains(player, company)) {
            deniedIncomeShare.put(player, company, share);
        } else {
            deniedIncomeShare.put(player, company, share + deniedIncomeShare.get(player, company));
        }
        //log.debug("+++ Denied "+share+"% share of PR income to "+player.getName());
    }

    /** Count the number of shares per revenue recipient<p>
     * A special rule applies to 1835 to prevent black privates and minors providing
     * income twice during an OR.
     */
    @Override
    protected  Map<MoneyOwner, Integer>  countSharesPerRecipient () {

        Map<MoneyOwner, Integer> sharesPerRecipient = super.countSharesPerRecipient();

        if (operatingCompany.value().getId().equalsIgnoreCase(GameDef_1837.Sd)) {
            for (Player player : deniedIncomeShare.rowKeySet()) {
                if (!sharesPerRecipient.containsKey(player)) continue;
                int share = deniedIncomeShare.get(player,operatingCompany.value());
                int shares = share / operatingCompany.value().getShareUnit();
                sharesPerRecipient.put (player, sharesPerRecipient.get(player) - shares);
                ReportBuffer.add(this, LocalText.getText("NoIncomeForPreviousOperation",
                        player.getId(),
                        share,
                        GameDef_1837.Sd));
            }
        }


        return sharesPerRecipient;
    }
    /**
     * Register black minors as having operated
     * for the purpose of denying income after conversion to a PR share
     */
    @Override
    protected void initTurn() {

        super.initTurn();

        Set<SpecialProperty> sps = operatingCompany.value().getSpecialProperties();
        if (sps != null && !sps.isEmpty()) {
            ExchangeForShare efs = (ExchangeForShare) Iterables.get(sps, 0);
            addIncomeDenialShare (operatingCompany.value().getPresident(), operatingCompany.value(), efs.getShare());
        }
    }

    // PROBABLY NO LONGER NEEDED!
    // Südbahn may not run if formed in an OR. Could it be formed elsewhere??
    // Check again after KK/Ug formation has been fixed
    @Override
    public void resume() {
        PublicCompany suedbahn = companyManager.getPublicCompany(GameDef_1837.Sd);
        //        PublicCompany hungary = companyManager.getPublicCompany("Ug");
        //       PublicCompany kuk = companyManager.getPublicCompany("KK");

        if ((suedbahn.hasFloated()) && (!suedbahn.hasOperated())
                // Suedbahn has just started. Check if it can operate this round
                // That's only the case if another Pre Suedbahn S2-S5 still hasnt
                // operated. Trains that have run this OR cant run again, shares that have
                // been exchanged cant get their income distributed again.
                && (operatingCompany.value().getType().getId().equals(GameDef_1837.Minor1)) ) {
            log.debug("a Pre Suedbahn has not operated: Suedbahn can operate");

            // Insert the Suedbahn before the first major company
            // with a lower current price that has not yet operated
            // and isn't currently operating

            int index = 0;
            int operatingCompanyIndex = getOperatingCompanyIndex();
            for (PublicCompany company : setOperatingCompanies()) {
                if (index > operatingCompanyIndex
                        && company.hasStockPrice()
                        && company.hasFloated()
                        && !company.isClosed()
                        && company != operatingCompany.value()
                        && company.getCurrentSpace().getPrice()
                        < suedbahn.getCurrentSpace().getPrice()) {
                    log.debug("Suedbahn will operate before {}", company.getId());
                    break;
                }
                index++;
            }
            // Insert SB at the found index (possibly at the end)
            operatingCompanies.add(index, suedbahn);
            log.debug("SU will operate at order position {}", index);

        } else {

            log.debug("S1 has operated: Sd cannot operate");

        }

        guiHints.setCurrentRoundType(getClass());
        super.resume();
    }

    public boolean setPossibleActions() {

        PublicCompany company = operatingCompany.value();
        if (GameDef_1837.S5.equalsIgnoreCase(company.getId())
            && (company.getHomeHexes().isEmpty()
                || !company.hasLaidHomeBaseTokens())) {

            initTurn();
            possibleActions.clear();
            possibleActions.add(new SetHomeHexLocation(getRoot(),
                    company, GameDef_1837.S5homes));
            return true;
        } else if (company.isClosed()) {
            // This can occur if a Sd minor buys the first 4-train
            finishTurn();
            possibleActions.clear();
            super.setPossibleActions();
            return true;
        } else {
            return super.setPossibleActions();
        }
    }

    public boolean processGameSpecificAction (PossibleAction action) {
        if (action instanceof SetHomeHexLocation) {
            SetHomeHexLocation selectHome = (SetHomeHexLocation) action;
            PublicCompany company = selectHome.getCompany();
            MapHex chosenHome = selectHome.getSelectedHomeHex();
            company.setHomeHex(chosenHome);
            company.layHomeBaseTokens();
            return true;
        } else {
            return false;
        }
    }

    /* (non-Javadoc)
     * @see net.sf.rails.game.OperatingRound#validateSetRevenueAndDividend(rails.game.action.SetDividend)
     */
    @Override
    protected String validateSetRevenueAndDividend(SetDividend action) {
        String errMsg = null;
        PublicCompany company;
        String companyName;
        int amount, directAmount;
        int revenueAllocation;

        // Dummy loop to enable a quick jump out.
        while (true) {

            // Checks
            // Must be correct company.
            company = action.getCompany();
            companyName = company.getId();
            if (company != operatingCompany.value()) {
                errMsg =
                        LocalText.getText("WrongCompany",
                                companyName,
                                operatingCompany.value().getId() );
                break;
            }
            // Must be correct step
            if (getStep() != GameDef.OrStep.CALC_REVENUE) {
                errMsg = LocalText.getText("WrongActionNoRevenue");
                break;
            }

            // Amount must be non-negative multiple of 5
            amount = action.getActualRevenue();
            if (amount < 0) {
                errMsg =
                        LocalText.getText("NegativeAmountNotAllowed",
                                String.valueOf(amount));
                break;
            }
            if (amount % 5 != 0) {
                errMsg =
                        LocalText.getText("AmountMustBeMultipleOf5",
                                String.valueOf(amount));
                break;
            }

            // Direct revenue must be non-negative multiple of 5,
            // and at least 10 less than the total revenue
            directAmount = action.getActualCompanyTreasuryRevenue();
            if (directAmount < 0) {
                errMsg =
                        LocalText.getText("NegativeAmountNotAllowed",
                                String.valueOf(amount));
                break;
            }
            if (directAmount % 5 != 0) {
                errMsg =
                        LocalText.getText("AmountMustBeMultipleOf5",
                                String.valueOf(amount));
                break;
            }
            if (amount > 0 && amount - directAmount < 10) {
                errMsg = LocalText.getText("WrongDirectRevenue",
                        String.valueOf(directAmount),
                        "10",
                        String.valueOf(amount));
                break;
            }

            // Check chosen revenue distribution
            revenueAllocation = action.getRevenueAllocation();
            if (amount > 0) {
                // Check the allocation type index (see SetDividend for values)
                if (revenueAllocation < 0
                        || revenueAllocation >= SetDividend.NUM_OPTIONS) {
                    errMsg =
                            LocalText.getText("InvalidAllocationTypeIndex",
                                    String.valueOf(revenueAllocation));
                    break;
                }

                // Validate the chosen allocation type
                int[] allowedAllocations =
                        ((SetDividend) selectedAction).getAllowedAllocations();
                boolean valid = false;
                for (int aa : allowedAllocations) {
                    if (revenueAllocation == aa) {
                        valid = true;
                        break;
                    }
                }
                if (!valid) {
                    errMsg =
                            LocalText.getText(SetDividend.getAllocationNameKey(revenueAllocation));
                    break;
                }
            } else if (revenueAllocation != SetDividend.NO_ROUTE){
                // If there is no revenue, use withhold.
                action.setRevenueAllocation(SetDividend.WITHHOLD);
            }

            if (amount == 0 && operatingCompany.value().getNumberOfTrains() == 0) {
                DisplayBuffer.add(this, LocalText.getText("RevenueWithNoTrains",
                        operatingCompany.value().getId(),
                        Bank.format(this, 0) ));
            }

            break;
        }

        return errMsg;
    }

    /* (non-Javadoc)
     * @see net.sf.rails.game.OperatingRound#splitRevenue(int)
     */

    public void splitRevenue(int amount) {
        if (amount > 0) {
            int withheld = calculateCompanyIncomeFromSplit(amount);
            String withheldText = Currency.fromBank(withheld, operatingCompany.value());

            ReportBuffer.add(this, LocalText.getText("Receives",
                    operatingCompany.value().getId(), withheldText));
            // Payout the remainder
            int payed = amount - withheld;
            payout(payed, true);
        }

    }

    @Override
    protected int calculateCompanyIncomeFromSplit (int revenue) {
        return roundIncome(0.5 * revenue, Rounding.UP, ToMultipleOf.ONE);
    }

    /*
     * Rounds up or down the individual payments based on the boolean value
     */
    public void payout(int amount, boolean b) {
        if (amount == 0) return;

        int part;
        int shares;

        Map<MoneyOwner, Integer> sharesPerRecipient = countSharesPerRecipient();

        // Calculate, round up or down, report and add the cash

        // Define a precise sequence for the reporting
        Set<MoneyOwner> recipientSet = sharesPerRecipient.keySet();
        for (MoneyOwner recipient : SequenceUtil.sortCashHolders(recipientSet)) {
            if (recipient instanceof Bank) continue;

            shares = (sharesPerRecipient.get(recipient));
            if (shares == 0) continue;

            double payoutPerShare = amount * operatingCompany.value().getShareUnit() / 100.0;
            part = calculateShareholderPayout(payoutPerShare, shares);

            String partText = Currency.fromBank(part, recipient);
            ReportBuffer.add(this, LocalText.getText("Payout",
                    recipient.getId(),
                    partText,
                    shares,
                    operatingCompany.value().getShareUnit()));
        }

        // Move the token
        ((PublicCompany_1837) operatingCompany.value()).payout(amount, b);
    }

    @Override
    protected int calculateShareholderPayout (double payoutPerShare, int numberOfShares) {
        return roundShareholderPayout(payoutPerShare, numberOfShares,
                Rounding.DOWN, Multiplication.BEFORE_ROUNDING);
    }

    /* (non-Javadoc)
     * @see net.sf.rails.game.OperatingRound#gameSpecificTileLayAllowed(net.sf.rails.game.PublicCompany, net.sf.rails.game.MapHex, int)
     */
    @Override
    protected int processSpecialRevenue(int earnings, int specialRevenue) {
        int dividend = earnings;
        PublicCompany company = operatingCompany.value();
        if (specialRevenue > 0) {
            dividend -= specialRevenue;
            company.setLastDirectIncome(specialRevenue);
            ReportBuffer.add(this, LocalText.getText("CompanyDividesEarnings",
                    company,
                    Bank.format(this, earnings),
                    Bank.format(this, dividend),
                    Bank.format(this, specialRevenue)));
            Currency.fromBank(specialRevenue, company);
        }
        company.setLastDividend(dividend);
        return dividend;
    }

    @Override
    protected boolean gameSpecificTileLayAllowed(PublicCompany company,
            MapHex hex, int orientation) {
        RailsRoot root = gameManager.getRoot();
        List<MapHex> italyMapHexes = new ArrayList<> ();
        // 1. check Phase

        int phaseIndex = root.getPhaseManager().getCurrentPhase().getIndex();
        if (phaseIndex < 3) {
            // Check if the Hex is blocked by a private ?
            if (hex.isBlockedByPrivateCompany()) {
                if (company.getPresident().getPortfolioModel().getPrivateCompanies().contains(hex.getBlockingPrivateCompany())) {
                    // Check if the Owner of the PublicCompany is owner of the Private Company that blocks
                    // the hex (1837)
                    return true;
                }
                return false;
            }
        }
        if (phaseIndex >= 4 ) {
            log.debug("Italy inactive, index of phase = {}", phaseIndex);

            // 2. retrieve Italy vertices ...
            String [] italyHexes = GameDef_1837.ItalyHexes.split(",");
            for (String italyHex:italyHexes){
                italyMapHexes.add(root.getMapManager().getHex(italyHex));
            }
            if (italyMapHexes.contains(hex)) {
                return false;
            }
        }
        return true;
    }


    protected void prepareRevenueAndDividendAction() {

        // There is only revenue if there are any trains
        if (operatingCompany.value().hasTrains()) {
            int[] allowedRevenueActions =
                    operatingCompany.value().isSplitAlways()
                    ? new int[] { SetDividend.SPLIT }
            : operatingCompany.value().isSplitAllowed()
            ? new int[] { SetDividend.PAYOUT,
                    SetDividend.SPLIT,
                    SetDividend.WITHHOLD } : new int[] {
                            SetDividend.PAYOUT,
                            SetDividend.WITHHOLD };

            possibleActions.add(new SetDividend(getRoot(),
                    operatingCompany.value().getLastRevenue(),
                    operatingCompany.value().getLastDirectIncome(),
                    true, allowedRevenueActions,0));
        }
    }

    /**
     * Can the operating company buy a train now?
     * In 1837 it is allowed if another (different) train is scrapped.
     *
     * @return True if the company is allowed to buy a train
     */
    protected boolean canBuyTrainNow() {
        return isBelowTrainLimit();
    }

    /**
     * New standard method to allow discarding trains when at the train limit.
     * Note: 18EU has a different procedure for discarding Pullmann trains.
     * @param company Operating company
     * @param newTrain Train to get via exchange
     */
    @Override
    protected void addOtherExchangesAtTrainLimit(PublicCompany company, Train newTrain) {
        // May only discard train if at train limit.
        if (isBelowTrainLimit()) return;

        Set<Train> oldTrains = company.getPortfolioModel().getUniqueTrains();

        for (Train oldTrain : oldTrains) {
            // May not exchange for same type
            if (oldTrain.getType().equals(newTrain.getType())) continue;
            // New train cost is raised with half the old train cost
            int price = newTrain.getCost() + oldTrain.getCost() / 2;
            BuyTrain buyTrain = new BuyTrain (newTrain, bank.getIpo(), price);
            buyTrain.setTrainForExchange(oldTrain);
            possibleActions.add(buyTrain);
        }
    }
}
