/**
 * 
 */
package net.sf.rails.game.specific._1837;

import java.util.Map;
import java.util.Set;

import net.sf.rails.common.DisplayBuffer;
import net.sf.rails.common.LocalText;
import net.sf.rails.common.ReportBuffer;
import net.sf.rails.game.GameDef;
import net.sf.rails.game.GameManager;
import net.sf.rails.game.MapHex;
import net.sf.rails.game.OperatingRound;
import net.sf.rails.game.Phase;
import net.sf.rails.game.Player;
import net.sf.rails.game.PrivateCompany;
import net.sf.rails.game.PublicCompany;
import net.sf.rails.game.financial.Bank;
import net.sf.rails.game.financial.NationalFormationRound;
import net.sf.rails.game.special.ExchangeForShare;
import net.sf.rails.game.special.SpecialProperty;
import net.sf.rails.game.state.BooleanState;
import net.sf.rails.game.state.Currency;
import net.sf.rails.game.state.MoneyOwner;
import net.sf.rails.util.SequenceUtil;
import rails.game.action.SetDividend;

import com.google.common.collect.HashBasedTable;
import com.google.common.collect.Iterables;
import com.google.common.collect.Table;


/**
 * @author Martin
 *
 */
public class OperatingRound_1837 extends OperatingRound {

    private final BooleanState needSuedBahnFormationCall = BooleanState.create(this, "NeedSuedBahnFormationCall");
    private final BooleanState needHungaryFormationCall = BooleanState.create(this, "NeedHungaryFormationCall");
    private final BooleanState needKuKFormationCall = BooleanState.create(this, "NeedKuKFormationCall");
    

 
    /**
     * Registry of percentage of nationals revenue to be denied per player
     * because of having produced revenue in the same OR.
     */
    private final Table<Player, PublicCompany, Integer> deniedIncomeShare = HashBasedTable.create();
    /**
     * @param parent
     * @param id
     */
    public OperatingRound_1837(GameManager parent, String id) {
        super(parent, id);
    }


    @Override
    protected void newPhaseChecks() {
        Phase phase = Phase.getCurrent(this);
        if (phase.getId().equals("3")) {
          for(PrivateCompany comp:gameManager.getAllPrivateCompanies())  {
              comp.unblockHexes();
          }
        } 
        if (phase.getId().equals("4")
                && !companyManager.getPublicCompany("Sd").hasStarted()
                && !NationalFormationRound.nationalIsComplete(gameManager, "Sd")) {
            if (getStep() == GameDef.OrStep.DISCARD_TRAINS) {
                // Postpone until trains are discarded
                needSuedBahnFormationCall.set(true);
            } else {
                // Do it immediately
                ((GameManager_1837)gameManager).startSuedBahnFormationRound (this);
            }
        }
        if (phase.getId().equals("4+1")
                && !companyManager.getPublicCompany("KK").hasStarted()
                && !NationalFormationRound.nationalIsComplete(gameManager,"KK")) {
            if (getStep() == GameDef.OrStep.DISCARD_TRAINS) {
                // Postpone until trains are discarded
                needKuKFormationCall.set(true);
            } else {
                // Do it immediately
                ((GameManager_1837)gameManager).startKuKFormationRound (this);
            }
         if (phase.getId().equals("4E")
                    && !companyManager.getPublicCompany("Ug").hasStarted()
                    && !NationalFormationRound.nationalIsComplete(gameManager, "Ug")) {
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

        if (operatingCompany.value().getId().equalsIgnoreCase("Sd")) {
            for (Player player : deniedIncomeShare.rowKeySet()) {
                if (!sharesPerRecipient.containsKey(player)) continue;
                int share = deniedIncomeShare.get(player,operatingCompany.value());
                int shares = share / operatingCompany.value().getShareUnit();
                sharesPerRecipient.put (player, sharesPerRecipient.get(player) - shares);
                ReportBuffer.add(this, LocalText.getText("NoIncomeForPreviousOperation",
                        player.getId(),
                        share,
                        "Sd"));
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

    @Override
    public void resume() {
        PublicCompany suedbahn = companyManager.getPublicCompany("Sd");
//        PublicCompany hungary = companyManager.getPublicCompany("Ug");
 //       PublicCompany kuk = companyManager.getPublicCompany("KK");

        if ((suedbahn.hasFloated()) && (!suedbahn.hasOperated())
                // Suedbahn has just started. Check if it can operate this round
                // That's only the case if another Pre Suedbahn S2-S5 still hasnt
                // operated. Trains that have run this OR cant run again, shares that have
                // been exchanged cant get their income distributed again.
                && (operatingCompany.value().getType().getId().equals("Minor1")) ) {
            log.debug("a Pre Suedbahn has not operated: Suedbahn can operate");

            // Insert the Suedbahn before the first major company
            // with a lower current price that has not yet operated
            // and isn't currently operating

            int index = 0;
            int operatingCompanyndex = getOperatingCompanyndex();
            for (PublicCompany company : setOperatingCompanies()) {
                if (index > operatingCompanyndex
                        && company.hasStockPrice()
                        && company.hasFloated()
                        && !company.isClosed()
                        && company != operatingCompany.value()
                        && company.getCurrentSpace().getPrice()
                        < suedbahn.getCurrentSpace().getPrice()) {
                    log.debug("Suedbahn will operate before "+company.getId());
                    break;
                }
                index++;
            }
            // Insert SB at the found index (possibly at the end)
            operatingCompanies.add(index, suedbahn);
            log.debug("SU will operate at order position "+index);

        } else {

            log.debug("S1 has operated: SU cannot operate");

        }

        guiHints.setCurrentRoundType(getClass());
        super.resume();
    }

    /* (non-Javadoc)
     * @see net.sf.rails.game.OperatingRound#validateSetRevenueAndDividend(rails.game.action.SetDividend)
     */
    @Override
    protected String validateSetRevenueAndDividend(SetDividend action) {
        String errMsg = null;
        PublicCompany company;
        String companyName;
        int amount = 0;
        int revenueAllocation = -1;

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

            // Check chosen revenue distribution
            if (amount > 0) {
                // Check the allocation type index (see SetDividend for values)
                revenueAllocation = action.getRevenueAllocation();
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
            } else {
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
    
    public void splitRevenue(int amount, boolean roundUp) {
        int withheld = 0;
        if (amount > 0) { 
            int numberOfShares = operatingCompany.value().getNumberOfShares();
            if (roundUp) {
                // Withhold half of it          
               withheld =
                    (amount / (2 * numberOfShares)) * numberOfShares;
             
            } else {
                //withhold half of it and make sure the bank gets any remaining rounding victims
                withheld = 
                        (int) Math.ceil(amount * 0.5);
            }
            String withheldText = Currency.fromBank(withheld, operatingCompany.value());
            
            ReportBuffer.add(this, operatingCompany.value().getId() + LocalText.getText("Receives") + withheldText);
            // Payout the remainder
            int payed = amount - withheld;
            payout(payed, roundUp, true);
        }

    }


    /* 
     * Rounds up or down the individual payments based on the boolean value
     */
    public void payout(int amount, boolean roundUp, boolean b) {
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
            if (roundUp)  {
            part = (int) Math.ceil(amount * shares * operatingCompany.value().getShareUnit() / 100.0);
            }
            else {
                part = (int) Math.floor(amount * shares * operatingCompany.value().getShareUnit() / 100.0); 
            }
            
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
    
 

        /* (non-Javadoc)
     * @see net.sf.rails.game.OperatingRound#executeSetRevenueAndDividend(rails.game.action.SetDividend)
     */
    @Override
    protected void executeSetRevenueAndDividend(SetDividend action) {

        int amount = action.getActualRevenue();
        int revenueAllocation = action.getRevenueAllocation();

        operatingCompany.value().setLastRevenue(amount);
        operatingCompany.value().setLastRevenueAllocation(revenueAllocation);

        // Pay any debts from treasury, revenue and/or president's cash
        // The remaining dividend may be less that the original income
        amount = executeDeductions (action);

        if (amount == 0) {

            ReportBuffer.add(this, LocalText.getText("CompanyDoesNotPayDividend",
                    operatingCompany.value().getId()));
            withhold(amount);

        } else if (revenueAllocation == SetDividend.PAYOUT) {

            ReportBuffer.add(this, LocalText.getText("CompanyPaysOutFull",
                    operatingCompany.value().getId(), Bank.format(this, amount) ));

            payout(amount, false, false); //1837 is paying out the rounded down amount except to the bank..

        } else if (revenueAllocation == SetDividend.SPLIT) {

            ReportBuffer.add(this, LocalText.getText("CompanySplits",
                    operatingCompany.value().getId(), Bank.format(this, amount) ));

            splitRevenue(amount, false);

        } else if (revenueAllocation == SetDividend.WITHHOLD) {

            ReportBuffer.add(this, LocalText.getText("CompanyWithholds",
                    operatingCompany.value().getId(),
                    Bank.format(this, amount) ));

            withhold(amount);

        }

        // Rust any obsolete trains
        operatingCompany.value().getPortfolioModel().rustObsoleteTrains();

        // We have done the payout step, so continue from there
        nextStep(GameDef.OrStep.PAYOUT);
    }


    /* (non-Javadoc)
     * @see net.sf.rails.game.OperatingRound#gameSpecificTileLayAllowed(net.sf.rails.game.PublicCompany, net.sf.rails.game.MapHex, int)
     */
   
    @Override
    protected boolean gameSpecificTileLayAllowed(PublicCompany company,
            MapHex hex, int orientation) {
        boolean result = true;
        // FIXME: Removed hex.isBlockedForTileLays removed in Rails 2.0 beta preparation, needs fix
//        // Check if the Hex is blocked ?
//        for (PrivateCompany privComp : gameManager.getAllPrivateCompanies()) {
//            boolean isBlocked = hex.isBlockedForTileLays(privComp);
//            if (isBlocked) {
//                result = true;
//                break;
//            }
//        }
//
//        if (result == true) {
//            // Check if the Owner of the PublicCompany is owner of the Private Company that blocks
//            // the hex (1837)
//
//            ImmutableSet<PrivateCompany> compPrivatesOwned =
//                    company.getPresident().getPortfolioModel().getPrivateCompanies();
//
//            for (PrivateCompany privComp : compPrivatesOwned) {
//                // Check if the Hex is blocked by any of the privates owned by
//                // this PublicCompany
//                if (hex.isBlockedForTileLays(privComp)) {
//                    result = false;
//                }
//            }
//
//        }

        return result;
    }



    
}
