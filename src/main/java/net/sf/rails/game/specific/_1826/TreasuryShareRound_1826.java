package net.sf.rails.game.specific._1826;

import net.sf.rails.common.DisplayBuffer;
import net.sf.rails.common.LocalText;
import net.sf.rails.common.ReportBuffer;
import net.sf.rails.game.GameManager;
import net.sf.rails.game.Player;
import net.sf.rails.game.PublicCompany;
import net.sf.rails.game.RailsOwner;
import net.sf.rails.game.financial.Bank;
import net.sf.rails.game.financial.BankPortfolio;
import net.sf.rails.game.financial.RailsMoneyOwner;
import net.sf.rails.game.financial.TreasuryShareRound;
import net.sf.rails.game.model.BondsModel;
import net.sf.rails.game.round.RoundFacade;
import net.sf.rails.game.state.Currency;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import rails.game.action.BuyBonds;
import rails.game.action.PossibleAction;


public class TreasuryShareRound_1826 extends TreasuryShareRound {

    private static final Logger log = LoggerFactory.getLogger(TreasuryShareRound_1826.class);

    /**
     * Created via Configure
     */
    public TreasuryShareRound_1826 (GameManager parent, String id) {
        super(parent, id);
    }

    @Override
    public void start(RoundFacade parentRound) {
        super.start(parentRound);
    }

    @Override
    public void addGameSpecificPossibleActions() {

        PublicCompany company = operatingCompany;

        // Check if any bonds can be bought
        // First check if any bonds are NOT in its Treasury
        int bondsCount = company.getPortfolioModel().getBondsCount(company);
        if (company.hasBonds() && bondsCount < company.getNumberOfBonds()) {

            // Scan all potential owners: the Pool first
            int ownedNumber = pool.getBondsCount(company);
            int companyCanBuy = company.getCash()/company.getPriceOfBonds();
            int maxNumber = Math.min (ownedNumber, companyCanBuy);
            if (maxNumber > 0) {
                possibleActions.add (new BuyBonds(pool.getParent(),
                        company, company, maxNumber, company.getPriceOfBonds()));
                return;
            }
            // If the pool is empty, then all players qualify to be bought from
            for (Player player : playerManager.getPlayers()) {
                ownedNumber = player.getPortfolioModel().getBondsCount(company);
                maxNumber = Math.min (ownedNumber, companyCanBuy);
                if (maxNumber > 0) {
                    possibleActions.add(new BuyBonds(player,
                            company, company, maxNumber, company.getPriceOfBonds()));
                }
            }
        }
    }

    @Override
    protected boolean processGameSpecificAction(PossibleAction action) {

        if (action instanceof BuyBonds) {
            return buyBonds ((BuyBonds)action);
        }

        return false;
    }

    protected boolean buyBonds (BuyBonds action) {

        PublicCompany company = action.getCompany();
        RailsOwner from = action.getFrom();
        RailsOwner to = action.getTo();
        int maxNumber = action.getMaxNumber();
        int price = action.getPrice();
        int numberBought = action.getNumberBought();

        // Validations not done yet in the 'equals' check
        if (numberBought < 1 || numberBought > maxNumber) {
            DisplayBuffer.add(this, LocalText.getText(
                    "InvalidNumberOfBondsBought",
                    numberBought, company, 1, maxNumber));
            return false;
        }

        BondsModel fromBonds = null;
        BondsModel toBonds = null;
        RailsMoneyOwner fromCashier = null;
        RailsMoneyOwner toCashier = null;
        int amount = numberBought * price;
        if (from instanceof BankPortfolio) {
            fromBonds = ((BankPortfolio) from).getPortfolioModel().getBondsModel(company);
            fromCashier = (Bank) from.getParent();
        } else if (from instanceof Player) {
            fromBonds = ((Player) from).getPortfolioModel().getBondsModel(company);
            fromCashier = (Player) from;
        }
        if (to instanceof PublicCompany) {
            toBonds = ((PublicCompany) to).getPortfolioModel().getBondsModel(company);
            toCashier = (PublicCompany) to;
        } else if (to instanceof Player) {
            toBonds = ((Player) to).getPortfolioModel().getBondsModel(company);
            toCashier = (Player) to;
        }
        if (fromBonds != null && toBonds != null) {
            fromBonds.addBondsCount(-numberBought);
            toBonds.addBondsCount(numberBought);
            Currency.wire(toCashier, amount, fromCashier);
            ReportBuffer.add(this, LocalText.getText(
                    (numberBought == 1 ? "BoughtBond" : "BoughtBonds"),
                    to, numberBought, company, from, Bank.format(this, amount)));
            return true;
        }
    return false;
    }
}
