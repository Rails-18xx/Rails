package net.sf.rails.game.specific._1826;

import net.sf.rails.common.DisplayBuffer;
import net.sf.rails.common.LocalText;
import net.sf.rails.common.ReportBuffer;
import net.sf.rails.game.*;
import net.sf.rails.game.financial.Bank;
import net.sf.rails.game.financial.BankPortfolio;
import net.sf.rails.game.financial.RailsMoneyOwner;
import net.sf.rails.game.financial.StockRound;
import net.sf.rails.game.model.BondsModel;
import net.sf.rails.game.state.Currency;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import rails.game.action.BuyBonds;
import rails.game.action.PossibleAction;
import rails.game.action.SellBonds;

public class StockRound_1826 extends StockRound {

    private static final Logger log = LoggerFactory.getLogger(StockRound.class);

    private PublicCompany_1826 sncf
            = (PublicCompany_1826)companyManager.getPublicCompany(PublicCompany_1826.SNCF);

    public StockRound_1826 (GameManager parent, String id) {
        super(parent, id);

    }

    public void start() {

        super.start();

        // Define the companies that have bonds.
        // This class is now specialized for 1826,
        // but can be generalised by scanning the
        // 'hasBonds()' values of all public companies here.
    }

        @Override
    protected void setGameSpecificActions() {

        if (companyBoughtThisTurnWrapper.value() != null) return;
        int price = sncf.getPriceOfBonds();

        int playerBonds = currentPlayer.getPortfolioModel().getBondsCount(sncf);
        if (playerBonds > 0) possibleActions.add(new SellBonds(sncf, price, playerBonds));

        int poolBonds = pool.getBondsCount(sncf);
        if (poolBonds > 0) possibleActions.add(new BuyBonds(pool.getParent(),
                currentPlayer, sncf, 1, price));
    }

    protected boolean processGameSpecificAction(PossibleAction action) {

        if (action instanceof BuyBonds) {
            return buyBonds ((BuyBonds)action);
        } else if (action instanceof SellBonds) {
            return sellBonds ((SellBonds)action);
        }

        return false;
    }

    /* Copy of buyBonds() inj TreasurySaheRound_1826.
     * Should both be merged into StockRound()?
     */
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
        }
        if (to instanceof Player) {
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
            hasActed.set(true);
            companyBoughtThisTurnWrapper.set(company);
            return true;
        }
        return false;
    }

    protected boolean sellBonds (SellBonds action) {

        PublicCompany company = action.getCompany();
        int maxNumber = action.getMaxNumber();
        int price = action.getPrice();
        int numberSold = action.getNumberSold();
        Player player = action.getPlayer();

        // Validations not done yet in the 'equals' check
        if (numberSold < 1 || numberSold > maxNumber) {
            DisplayBuffer.add(this, LocalText.getText(
                    "InvalidNumberOfBondsSold",
                    numberSold, company, 1, maxNumber));
            return false;
        }

        int amount = numberSold * price;
        BondsModel fromBonds = player.getPortfolioModel().getBondsModel(company);
        BondsModel toBonds = pool.getBondsModel(company);
        RailsMoneyOwner fromCashier = player;
        RailsMoneyOwner toCashier = (RailsMoneyOwner) pool.getParent();
        if (fromBonds != null && toBonds != null) {
            fromBonds.addBondsCount(-numberSold);
            toBonds.addBondsCount(numberSold);
            Currency.wire(toCashier, amount, fromCashier);
            ReportBuffer.add(this, LocalText.getText(
                    (numberSold == 1 ? "SoldBond" : "SoldBonds"),
                    player, numberSold, company, Bank.format(this, amount)));
            return true;
        }
        return false;
    }
}
