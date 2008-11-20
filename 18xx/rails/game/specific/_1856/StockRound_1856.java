package rails.game.specific._1856;

import rails.game.Bank;
import rails.game.CashHolder;
import rails.game.Certificate;
import rails.game.Portfolio;
import rails.game.PublicCertificateI;
import rails.game.PublicCompanyI;
import rails.game.ReportBuffer;
import rails.game.StockRound;
import rails.game.move.CashMove;
import rails.util.LocalText;

public class StockRound_1856 extends StockRound {

    /**
     * Special 1856 code to check for company flotation.
     * 
     * @param company
     */
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

    protected CashHolder getSharePriceRecipient(Certificate cert, int price) {

        CashHolder recipient;
        Portfolio oldHolder = (Portfolio) cert.getHolder();
    
        if (price != 0
                && cert instanceof PublicCertificateI
                && oldHolder == Bank.getIpo()) {

            PublicCompany_1856 comp = (PublicCompany_1856)((PublicCertificateI) cert).getCompany();

            switch (comp.getTrainNumberAvailableAtStart()) {
            case 2:
            case 3:
            case 4:
                // Note, that the share has not yet been moved
                if (comp.getUnsoldPercentage() <= 50
                        && !comp.hasReachedDestination()) {
                    recipient = oldHolder.getOwner(); // i.e. the Bank
                    comp.addMoneyInEscrow(price);
                    ReportBuffer.addWaiting(LocalText.getText("HoldMoneyInEscrow", new String[] {
                            Bank.format(comp.getMoneyInEscrow()),
                            comp.getName() 
                            }));
                    break;
                }
                // fall through
            case 5:
                recipient = ((PublicCertificateI)cert).getCompany();
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

}
