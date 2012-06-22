package rails.game.specific._1856;

import rails.common.DisplayBuffer;
import rails.common.LocalText;
import rails.game.Bank;
import rails.game.GameManager;
import rails.game.Player;
import rails.game.PublicCertificate;
import rails.game.PublicCompany;
import rails.game.ReportBuffer;
import rails.game.StockRound;
import rails.game.action.BuyCertificate;
import rails.game.model.CashOwner;
import rails.game.model.PortfolioModel;
import rails.game.state.IntegerState;
import rails.game.state.PortfolioHolder;

public final class StockRound_1856 extends StockRound {

    /* Cope with multiple 5% share sales in one turn */
    private final IntegerState sharesSoldSoFar = IntegerState.create(this, "sharesSoldSoFar");
    private final IntegerState squaresDownSoFar = IntegerState.create(this, "squaresDownSoFar");

    private StockRound_1856 (GameManager parent, String id) {
        super(parent, id);
    }

    public static StockRound_1856 create(GameManager parent, String id){
        return new StockRound_1856(parent, id);
    }

    /**
     * Special 1856 code to check for company flotation.
     *
     * @param company
     */
    @Override
    protected void checkFlotation(PublicCompany company) {

        if (!company.hasStarted() || company.hasFloated()) return;

        int soldPercentage = getSoldPercentage(company);

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
    protected CashOwner getSharePriceRecipient(PublicCompany company, PortfolioHolder from, int price) {

        CashOwner recipient;

        if (price != 0
                && !company.getId().equalsIgnoreCase(PublicCompany_CGR.NAME)
                && from == ipo) {

            PublicCompany_1856 comp = (PublicCompany_1856)company;

            switch (comp.getTrainNumberAvailableAtStart()) {
            case 2:
            case 3:
            case 4:
                // Note, that the share has not yet been moved
                if (getSoldPercentage(comp) >= 50
                        && !comp.hasReachedDestination()) {
                    recipient = bank;
                    comp.addMoneyInEscrow(price);
                    ReportBuffer.addWaiting(LocalText.getText("HoldMoneyInEscrow",
                            Bank.format(price),
                            Bank.format(comp.getMoneyInEscrow()),
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
        } else {
            recipient = (CashOwner)from; // TODO: Remove this cast?
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
            company.getPresident().getPortfolioModel().getShareModel(company).update();
            if (currentPlayer != oldPresident) {
                // TODO: is this still required?
                oldPresident.getPortfolioModel().getShareModel(company).update();
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
            DisplayBuffer.add(LocalText.getText("MustBuyExtraShareAsPresident",
                    currentPlayer.getId(),
                    cgr.getId(),
                    cgr.getShareUnit()));
            if (lowestPrice > cash) {
                gameManager.startShareSellingRound(currentPlayer,
                        lowestPrice - cash, cgr, false);
            } else {
                // Player has enough cash
                if (cert1 != null && price1 <= cash) {
                    possibleActions.add(new BuyCertificate(cgr, 1, ipo, price1));
                }
                if (cert2 != null && price2 <= cash) {
                    possibleActions.add(new BuyCertificate(cgr, 1, pool, price2));
                }
            }

            return true;
        } else {
            return super.setPossibleActions();
        }
    }
}
