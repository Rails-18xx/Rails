package net.sf.rails.game.specific._1856;

import net.sf.rails.common.DisplayBuffer;
import net.sf.rails.common.LocalText;
import net.sf.rails.common.ReportBuffer;
import net.sf.rails.game.BankPortfolio;
import net.sf.rails.game.Currency;
import net.sf.rails.game.GameManager;
import net.sf.rails.game.MoneyOwner;
import net.sf.rails.game.Player;
import net.sf.rails.game.PublicCertificate;
import net.sf.rails.game.PublicCompany;
import net.sf.rails.game.StockRound;
import net.sf.rails.game.action.BuyCertificate;
import net.sf.rails.game.model.PortfolioModel;
import net.sf.rails.game.state.IntegerState;
import net.sf.rails.game.state.Owner;

public class StockRound_1856 extends StockRound {

    /* Cope with multiple 5% share sales in one turn */
    private final IntegerState sharesSoldSoFar = IntegerState.create(this, "sharesSoldSoFar");
    private final IntegerState squaresDownSoFar = IntegerState.create(this, "squaresDownSoFar");

    /**
     * Constructed via Configure
     */
    public StockRound_1856 (GameManager parent, String id) {
        super(parent, id);
    }

    /**
     * Special 1856 code to check for company flotation.
     *
     * @param company
     */
    @Override
    protected void checkFlotation(PublicCompany company) {

        if (!company.hasStarted() || company.hasFloated()) return;

        int soldPercentage = company.getSoldPercentage();

        PublicCompany_1856 comp = (PublicCompany_1856) company;
        int trainNumberAtStart = comp.getTrainNumberAvailableAtStart();
        int floatPercentage = 10 * trainNumberAtStart;

        log.debug ("Floatpercentage is "+floatPercentage);

        if (soldPercentage >= floatPercentage) {
            // Company floats.
            // In 1856 this does not mean that the company will operate,
            // only that it will be added to the list of companies
            // being considered for an OR turn.
            // See OperatingRound_1856 for the actual check.
            if (!company.hasFloated()) {
                floatCompany(company);
            }
        }
    }

    @Override
	protected void initPlayer() {
        super.initPlayer();
        sharesSoldSoFar.set(0);
        squaresDownSoFar.set(0);
    }

    @Override
	protected void adjustSharePrice (PublicCompany company, int numberSold, boolean soldBefore) {

        if (!company.canSharePriceVary()) return;

        int numberOfSpaces = numberSold;
        if (company instanceof PublicCompany_CGR) {
            if (company.getShareUnit() == 5) {
                // Take care for selling 5% shares in multiple blocks per turn
                numberOfSpaces
                    = (sharesSoldSoFar.value() + numberSold)/2
                    - squaresDownSoFar.value();
                sharesSoldSoFar.add(numberSold);
                squaresDownSoFar.add(numberOfSpaces);
            }
        }

        super.adjustSharePrice (company, numberOfSpaces, soldBefore);
    }

    @Override
    protected MoneyOwner getSharePriceRecipient(PublicCompany company, Owner from, int price) {

        MoneyOwner recipient;

        if (price != 0
                && !company.getId().equalsIgnoreCase(PublicCompany_CGR.NAME)
                && from == ipo.getParent()) {

            PublicCompany_1856 comp = (PublicCompany_1856)company;

            switch (comp.getTrainNumberAvailableAtStart()) {
            case 2:
            case 3:
            case 4:
                // Note, that the share has not yet been moved
                if (comp.getSoldPercentage() >= 50
                        && !comp.hasReachedDestination()) {
                    recipient = bank;
                    comp.addMoneyInEscrow(price);
                    // FIXME (Rails2.0): This used to be addWaiting in ReportBuffer
                    // potentially the reporting is now incorrect
                    ReportBuffer.add(this, LocalText.getText("HoldMoneyInEscrow",
                            Currency.format(this, price),
                            Currency.format(this, comp.getMoneyInEscrow()),
                            comp.getId() ));
                    break;
                }
                // fall through
            case 5:
                recipient = comp;
                break;
            case 6:
            default:
                recipient = bank;
            }
        } else if (from instanceof BankPortfolio) {
            recipient = bank;
        } else {
            recipient = (MoneyOwner)from;
        }
        return recipient;
    }

    /** Check for the special condition that the CGR president
     * has just bought his second share.
     */
    @Override
    protected void gameSpecificChecks (PortfolioModel boughtFrom,
            PublicCompany company) {

        if (company.getId().equalsIgnoreCase(PublicCompany_CGR.NAME)
                && ((PublicCompany_CGR)company).hasTemporaryPresident()) {
            log.debug("Resetting temp. president");
            ipo.swapPresidentCertificate(company,
                    currentPlayer.getPortfolioModel());
            Player oldPresident = company.getPresident();
            ((PublicCompany_CGR)company).setTemporaryPresident(null);
            // TODO: is this still required?
            //company.getPresident().getPortfolioModel().getShareModel(company).update();
            if (currentPlayer != oldPresident) {
                // TODO: is this still required?
            //    oldPresident.getPortfolioModel().getShareModel(company).update();
            }
        }
    }

       @Override
    public void resume() {
    }

    /** Check if the player is president of CGR and must buy a second share */
    @Override
    public boolean setPossibleActions() {

        PublicCompany_CGR cgr =
            (PublicCompany_CGR)companyManager.getPublicCompany(PublicCompany_CGR.NAME);
        if (cgr.hasStarted() && cgr.getPresident() == currentPlayer
                && cgr.hasTemporaryPresident()) {

            // Player MUST buy an extra single certificate to obtain
            // the President's certificate
            int cash = currentPlayer.getCashValue();
            PublicCertificate cert1, cert2;
            int price1 = 0;
            int price2 = 0;
            int lowestPrice = 999;
            if ((cert1 = ipo.findCertificate(cgr, false)) != null) {
                price1 = lowestPrice = cgr.getParPriceModel().getPrice().getPrice();
            }
            if ((cert2 = pool.findCertificate(cgr, false)) != null) {
                price2 = cgr.getCurrentPriceModel().getPrice().getPrice();
                if (price2 < lowestPrice) lowestPrice = price2;
            }
            DisplayBuffer.add(this, LocalText.getText("MustBuyExtraShareAsPresident",
                    currentPlayer.getId(),
                    cgr.getId(),
                    cgr.getShareUnit()));
            if (lowestPrice > cash) {
                gameManager.startShareSellingRound(currentPlayer,
                        lowestPrice - cash, cgr, false);
            } else {
                // Player has enough cash
                if (cert1 != null && price1 <= cash) {
                    possibleActions.add(new BuyCertificate(cgr, 1, ipo.getParent(), price1));
                }
                if (cert2 != null && price2 <= cash) {
                    possibleActions.add(new BuyCertificate(cgr, 1, pool.getParent(), price2));
                }
            }

            return true;
        } else {
            return super.setPossibleActions();
        }
    }
}
