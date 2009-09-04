/* $Header: /Users/blentz/rails_rcs/cvs/18xx/rails/game/special/ExchangeForShare.java,v 1.12 2009/09/04 18:56:16 evos Exp $ */
package rails.game.special;

import rails.game.*;
import rails.game.move.MoveSet;
import rails.util.*;

public class ExchangeForShare extends SpecialProperty {

    /** The public company of which a share can be obtained. */
    String publicCompanyName;
    PublicCompanyI publicCompany;
    /** The share size */
    int share;

    @Override
    public void configureFromXML(Tag tag) throws ConfigurationException {

        Tag swapTag = tag.getChild("ExchangeForShare");
        if (swapTag == null) {
            throw new ConfigurationException("<ExchangeForShare> tag missing");
        }

        publicCompanyName = swapTag.getAttributeAsString("company");
        if (!Util.hasValue(publicCompanyName))
            throw new ConfigurationException(
                    "ExchangeForShare: company name missing");
        share = swapTag.getAttributeAsInteger("share", 10);
    }

    public boolean isExecutionable() {

        return privateCompany.getPortfolio().getOwner() instanceof Player;
    }

    public boolean execute(StockRound round) {

        publicCompany =
                Game.getCompanyManager().getPublicCompany(publicCompanyName);

        Portfolio portfolio = privateCompany.getPortfolio();
        Player player = null;
        String errMsg = null;
        boolean ipoHasShare = Bank.getIpo().getShare(publicCompany) >= share;
        boolean poolHasShare = Bank.getPool().getShare(publicCompany) >= share;

        while (true) {

            /* Check if the private is owned by a player */
            if (!(portfolio.getOwner() instanceof Player)) {
                errMsg =
                        LocalText.getText("PrivateIsNotOwnedByAPlayer",
                                privateCompany.getName());
                break;
            }

            player = (Player) portfolio.getOwner();

            /* Check if a share is available */
            if (!ipoHasShare && !poolHasShare) {
                errMsg =
                        LocalText.getText("NoSharesAvailable",
                                publicCompanyName);
                break;
            }
            /* Check if the player has room for a share of this company */
            if (!round.mayPlayerBuyCompanyShare(player, publicCompany, 1)) {
                // TODO: Not nice to use '1' here, should be percentage.
                errMsg =
                        LocalText.getText("WouldExceedHoldLimit",
                                String.valueOf(round.getGameManager().getPlayerShareLimit()));
                break;
            }
            break;
        }
        if (errMsg != null) {
            DisplayBuffer.add(LocalText.getText(
                    "CannotSwapPrivateForCertificate",
                            player.getName(),
                            privateCompany.getName(),
                            share,
                            publicCompanyName,
                            errMsg ));
            return false;
        }

        MoveSet.start(true);

        Certificate cert =
                ipoHasShare ? Bank.getIpo().findCertificate(publicCompany,
                        false) : Bank.getPool().findCertificate(publicCompany,
                        false);
        //player.buy(cert, 0);
        cert.moveTo(player.getPortfolio());
        ReportBuffer.add(LocalText.getText("SwapsPrivateForCertificate",
                player.getName(),
                privateCompany.getName(),
                share,
                publicCompanyName ));
        setExercised();
        privateCompany.setClosed();

        return true;
    }

    /**
     * @return Returns the privateCompany.
     */
    public PrivateCompanyI getPrivateCompany() {
        return privateCompany;
    }

    /**
     * @return Returns the publicCompanyName.
     */
    public String getPublicCompanyName() {
        return publicCompanyName;
    }

    /**
     * @return Returns the share.
     */
    public int getShare() {
        return share;
    }

    public String getName() {
        return toString();
    }

    @Override
    public String toString() {
        return "Swap " + privateCompany.getName() + " for " + share
               + "% share of " + publicCompanyName;
    }

    @Override
    public String toMenu() {
        return LocalText.getText("SwapPrivateForCertificate",
                privateCompany.getName(),
                share,
                publicCompanyName );
    }
}
