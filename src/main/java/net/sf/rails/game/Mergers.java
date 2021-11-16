package net.sf.rails.game;

import net.sf.rails.common.LocalText;
import net.sf.rails.common.ReportBuffer;
import net.sf.rails.game.financial.Bank;
import net.sf.rails.game.financial.PublicCertificate;
import net.sf.rails.game.model.PortfolioModel;
import net.sf.rails.game.state.Currency;
import net.sf.rails.game.state.MoneyOwner;
import net.sf.rails.game.state.Owner;
import rails.game.action.MergeCompanies;

import java.util.Set;

/**
 * This class has generic static methods to execute minor-to-major mergers.
 *
 * This class was developed for 1837, which shares merger rules with 1835.
 * Perhaps it will also be useful for other games (to be investigated).
 */
public class Mergers {

    /**
     * This class cannot be instantiated.
     */
    private Mergers() {}

    /**
     * Merge a minor into an already started company.
     * @param action The MergeCompanies chosen action
     * @return True if the merge was successful
     */
    public static boolean mergeCompanies(GameManager gameManager, MergeCompanies action) {

        PublicCompany minor = action.getMergingCompany();
        PublicCompany major = action.getSelectedTargetCompany();

        return Mergers.mergeCompanies(gameManager, minor, major, false,
                gameManager.getCurrentPlayer() == null);
    }

    /**
     * Complemented by a shorter version in subclass CoalExchangeRound.
     * TODO: to be reconsidered once Nationals formation has been tested.
     *
     * @param gameManager A link to the actual game instance
     * @param minor The minor (or coal company) to be merged...
     * @param major ...into the related major company
     * @param majorPresident True if the major company president share must be taken
     * @param autoMerge True if the merge is triggered by the Rails engine rather than
     *                  the player. This only makes a difference in the report text
     * @return True if the merge was successful
     */
    public static boolean mergeCompanies(GameManager gameManager, PublicCompany minor, PublicCompany major,
                                     boolean majorPresident, boolean autoMerge) {
        PublicCertificate cert = null;
        MoneyOwner cashDestination = null; // Bank
        PortfolioModel reserved =  gameManager.getRoot().getBank().getUnavailable().getPortfolioModel();
        PortfolioModel pool =  gameManager.getRoot().getBank().getPool().getPortfolioModel();

        // TODO Validation to be added?
        if (major != null) {
            cert = reserved.findCertificate(major, majorPresident);
            cashDestination = major;
        }
        //TODO: what happens if the major hasnt operated/founded/Started sofar in the FinalCoalExchangeRound ?

        // Save minor details that are needed after merging
        Set<Train> minorTrains = minor.getPortfolioModel().getTrainList();
        int minorTrainsNo = minorTrains.size();

        // Transfer the minor assets
        int minorCash = minor.getCash();
        if (cashDestination == null) {
            // Assets go to the bank
            if (minorCash > 0) {
                Currency.toBankAll(minor);
            }
            pool.transferAssetsFrom(minor.getPortfolioModel());
        } else {
            // Assets go to the major company
            major.transferAssetsFrom(minor);
            if (minor.hasOperated()) {
                gameManager.blockCertificate(cert);
                for (Train train : minorTrains) {
                    gameManager.blockTrain(train);
                }
            }
        }

        Owner recipient = minor.getPresident();
        String recipientName = recipient.getId();
        if (recipient instanceof Player && ((Player)recipient).isBankrupt()) {
            recipient = pool.getParent();
            recipientName = pool.getParent().toString();
        }

        ReportBuffer.add(gameManager, "");
        if (autoMerge) {
            ReportBuffer.add(gameManager, LocalText.getText("AutoMergeMinorLog",
                    minor.getId(), major.getId(),
                    Bank.format(gameManager, minorCash), minorTrainsNo));
        } else {
            ReportBuffer.add(gameManager, LocalText.getText("MERGE_MINOR_LOG",
                    recipientName, minor.getId(), major.getId(),
                    Bank.format(gameManager, minorCash), minorTrainsNo));
        }
        ReportBuffer.add(gameManager, LocalText.getText(
                (cert.isPresidentShare() ? "GetPresShareForMinor" : "GetShareForMinor"),
                recipientName, cert.getShare(), major.getId(),
                minor.getId()));
        cert.moveTo(recipient);

        // FIXME: CHeck if this still works correctly

        // Check if minors have more certs (1837 Ug minors 1 and 3),
        // these must be exchanged as well.
        if (minor.getCertificates().size() > 1) {
            for (PublicCertificate minorCert : minor.getCertificates()) {
                if (minorCert.isPresidentShare()) continue;
                Owner owner = minorCert.getOwner();
                if (owner instanceof Player) {
                    cert = reserved.findCertificate(major, false);
                    ReportBuffer.add(gameManager, LocalText.getText(
                            "GetShareForMinor",
                            recipientName, cert.getShare(), major.getId(),
                            minor.getId()));
                    cert.moveTo(owner);
                }
            }
        }

        minor.setClosed();
        ReportBuffer.add(gameManager, LocalText.getText("MinorCloses", minor.getId()));

        /*
        checkFlotation(major);

        hasActed.set(true);

         */


        return true;
    }

    public static void exchangeMinorToken (GameManager gameManager, PublicCompany minor,
                                           PublicCompany major) {

        MapHex hex = minor.getHomeHexes().get(0);
        if (hex.isOpen()) {  // 1837 S5 Italian home hex has already been closed here
            Stop stop = hex.getRelatedStop(minor.getHomeCityNumber());
            if (!stop.hasTokenOf(major) && hex.layBaseToken(major, stop)) {
                /* TODO: the false return value must be impossible. */
                String message = LocalText.getText("ExchangesBaseToken2",
                        major.getId(), minor.getId(),
                        hex.getId() +
                                (hex.getStops().size() > 1
                                        ? "/" + hex.getConnectionString(stop.getRelatedStation())
                                        : "")
                );
                ReportBuffer.add(gameManager, message);
                major.layBaseToken(hex, 0);
            } else {
                //log.error("Cannot lay {} token on {} home {}", national, minor, hex);
            }
        }

    }



}
