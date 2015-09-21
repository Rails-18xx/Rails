package net.sf.rails.game.specific._1856;

import rails.game.action.BuyCertificate;
import net.sf.rails.common.DisplayBuffer;
import net.sf.rails.common.LocalText;
import net.sf.rails.common.ReportBuffer;
import net.sf.rails.game.GameManager;
import net.sf.rails.game.Player;
import net.sf.rails.game.PublicCompany;
import net.sf.rails.game.financial.Bank;
import net.sf.rails.game.financial.BankPortfolio;
import net.sf.rails.game.financial.PublicCertificate;
import net.sf.rails.game.financial.StockRound;
import net.sf.rails.game.model.PortfolioModel;
import net.sf.rails.game.state.IntegerState;
import net.sf.rails.game.state.MoneyOwner;
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
    // change: checks required number of shares in the hand of the president
    // requires: add a floatation strategy to publicCompany
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

    // change: see adjustSharePrice
    // requires: add an adjustSharePrice strategy (or a general implementation)
    @Override
	protected void initPlayer() {
        super.initPlayer();
        sharesSoldSoFar.set(0);
        squaresDownSoFar.set(0);
    }

    // change: sharePrice adjustment is one row per two shares for CGR
    // requires: add an adjustSharePrice strategy (or a general implementation)
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

    // change: money moves to different sources depending on phases 
    // requires: add an getSharePriceRecipient strategy (or a general implementation)
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
                            Bank.format(this, price),
                            Bank.format(this, comp.getMoneyInEscrow()),
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
    // change: update president of CGR after buying second share 
    // requires: trigger on the portfolio of the president of CGR? or a modifier on shareSelling
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

    // change: what is the importance of this?
    // requires: ?
    @Override
    public void resume() {
    }

    /** Check if the player is president of CGR and must buy a second share */
    // change: enforced action
    // requires: a modifier for the StockRound
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
                    possibleActions.add(new BuyCertificate(cgr, cert1.getShare(), ipo.getParent(), price1));
                }
                if (cert2 != null && price2 <= cash) {
                    possibleActions.add(new BuyCertificate(cgr, cert2.getShare(), pool.getParent(), price2));
                }
            }

            return true;
        } else {
            return super.setPossibleActions();
        }
    }
}
