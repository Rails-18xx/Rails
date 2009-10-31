package rails.game.specific._1856;

import rails.game.*;
import rails.game.action.BuyCertificate;
import rails.util.LocalText;

public class StockRound_1856 extends StockRound {

	/**
	 * Constructor with the GameManager, will call super class (StockRound's) Constructor to initialize
	 *
	 * @param aGameManager The GameManager Object needed to initialize the Stock Round
	 *
	 */
	public StockRound_1856 (GameManagerI aGameManager) {
		super (aGameManager);
	}

    /**
     * Special 1856 code to check for company flotation.
     *
     * @param company
     */
    @Override
    protected void checkFlotation(PublicCompanyI company) {

        if (!company.hasStarted() || company.hasFloated()) return;

        int unsoldPercentage = company.getUnsoldPercentage();

        PublicCompany_1856 comp = (PublicCompany_1856) company;
        int trainNumberAtStart = comp.getTrainNumberAvailableAtStart();
        int floatPercentage = 10 * trainNumberAtStart;

        log.debug ("Floatpercentage is "+floatPercentage);

        if (unsoldPercentage <= 100 - floatPercentage) {
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
    protected CashHolder getSharePriceRecipient(PublicCertificateI cert, int price) {

        CashHolder recipient;
        Portfolio oldHolder = (Portfolio) cert.getHolder();

        if (price != 0
                && !cert.getCompany().getName().equalsIgnoreCase(PublicCompany_CGR.NAME)
                && oldHolder == ipo) {

            PublicCompany_1856 comp = (PublicCompany_1856)(cert).getCompany();

            switch (comp.getTrainNumberAvailableAtStart()) {
            case 2:
            case 3:
            case 4:
                // Note, that the share has not yet been moved
                if (comp.getUnsoldPercentage() <= 50
                        && !comp.hasReachedDestination()) {
                    recipient = oldHolder.getOwner(); // i.e. the Bank
                    comp.addMoneyInEscrow(price);
                    ReportBuffer.addWaiting(LocalText.getText("HoldMoneyInEscrow",
                            Bank.format(comp.getMoneyInEscrow()),
                            comp.getName() ));
                    break;
                }
                // fall through
            case 5:
                recipient = (cert).getCompany();
                break;
            case 6:
            default:
                recipient = oldHolder.getOwner();
            }
        } else {
            recipient = oldHolder.getOwner();
        }
        return recipient;
    }

    /** Check for the special condition that the CGR president
     * has just bought his second share.
     */
    @Override
    protected void gameSpecificChecks (Portfolio boughtFrom,
            PublicCompanyI company) {

    	if (company.getName().equalsIgnoreCase(PublicCompany_CGR.NAME)
    			&& ((PublicCompany_CGR)company).hasTemporaryPresident()) {
    		log.debug("Resetting temp. president");
            ipo.swapPresidentCertificate(company,
                    currentPlayer.getPortfolio());
            Player oldPresident = company.getPresident();
    		((PublicCompany_CGR)company).setTemporaryPresident(null);
    		company.getPresident().getPortfolio().getShareModel(company).update();
    		if (currentPlayer != oldPresident) {
    			oldPresident.getPortfolio().getShareModel(company).update();
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
            (PublicCompany_CGR)companyManager.getCompanyByName(PublicCompany_CGR.NAME);
        if (cgr.hasStarted() && cgr.getPresident() == currentPlayer
                && cgr.hasTemporaryPresident()) {

            // Player MUST buy an extra single certificate to obtain
            // the President's certificate
            int cash = currentPlayer.getCash();
            PublicCertificateI cert1, cert2;
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
                    currentPlayer.getName(),
                    cgr.getName(),
                    cgr.getShareUnit()));
            if (lowestPrice > cash) {
                gameManager.startShareSellingRound(currentPlayer,
                        lowestPrice - cash, cgr);
            } else {
                // Player has enough cash
                if (cert1 != null && price1 <= cash) {
                    possibleActions.add(new BuyCertificate(cert1, ipo, price1));
                }
                if (cert2 != null && price2 <= cash) {
                    possibleActions.add(new BuyCertificate(cert2, pool, price2));
                }
            }

            return true;
        } else {
            return super.setPossibleActions();
        }
    }
}
