package net.sf.rails.game;

import net.sf.rails.common.DisplayBuffer;
import net.sf.rails.common.LocalText;
import net.sf.rails.common.ReportBuffer;
import net.sf.rails.game.financial.Bank;
import net.sf.rails.game.financial.ShareSellingRound;
import net.sf.rails.game.round.RoundFacade;
import net.sf.rails.game.specific._1837.PublicCompany_1837;
import net.sf.rails.game.state.Currency;

/**
 * This class has generic static methods for bankruptcy-related processes
 * that may vary per game family.
 *
 * This class was developed for 1837, which shares bankruptcy rules with 1835.
 */
public class Bankruptcy {

    public enum Style {
        DEFAULT,  // Just a filler for the stub, there is no default process.
        _1835,    // Also covers 1837
        _18EU,
        _18Scan,
    }

    /**
     * This class cannot be instantiated.
     */
    private Bankruptcy() {}

    public static Player processCompanyAfterPlayerBankruptcy(
            GameManager gameManager, Player oldPresident, PublicCompany company, Style style) {

        switch (style) {
            case _1835:
                // Also used by 1837
                return processCompanyAfterPlayerBankruptcy_1835 (
                        gameManager, oldPresident, company);

            case _18EU:
                return processCompanyAfterPlayerBankruptcy_18EU (
                        gameManager, oldPresident, company);

            case _18Scan:
                return processCompanyAfterPlayerBankruptcy_18Scan (
                        gameManager, oldPresident, company);

            default:
                return null;
        }
     }

    /** For 1835 and 1837.
     * Will only be called if a normal presidency transfer
     * (to a player with at least 2 shares) has not been possible.
     * In such a case, someone with one share, or else the priority player,
     * is forced to take it.
     * @param bankrupt The bankrupt player
     * @param company The bankrupt company.
     * @return The new president
     */
    private static Player processCompanyAfterPlayerBankruptcy_1835 (
            GameManager gameManager, Player bankrupt, PublicCompany company) {

        if (company.getType().getId().matches("Minor[12]?|Coal")) {
            // This includes the types "Minor1" and Minor2" of 1837
            return processMinorAfterPlayerBankruptcy_1835(gameManager, bankrupt, company);
        } else {
            return processMajorAfterPlayerBankruptcy_1835(gameManager, company);
        }
    }

    private static Player processMajorAfterPlayerBankruptcy_1835(
            GameManager gameManager, PublicCompany company) {

        Player newPresident = null;

        // Is there any player with one share?
        PlayerManager pm = gameManager.getRoot().getPlayerManager();
        for (Player player : pm.getNextPlayers(false)) {
            int shares = player.getPortfolioModel().getShares(company);
            if (shares == 1) {
               newPresident = player;
               break;
            }
        }

        // Otherwise, the priority holder will have to do it.
        if (newPresident == null) newPresident = pm.getPriorityPlayer();

        ReportBuffer.add(gameManager,
                LocalText.getText("IS_NOW_PRES_OF",
                        newPresident.getId(),
                        company.getId()));
        /*
         * If during emergency train buying the president gets bankrupt,
         * and no other player can take the president certificate,
         * the Bank loans money to buy the intended train.
         * The company must withhold until the loan has been paid back fully.
         */
        if (company.hasFloated()) {
            RoundFacade currentRound = gameManager.getCurrentRound();
            RoundFacade interruptedRound = gameManager.getInterruptedRound();
            if (currentRound instanceof ShareSellingRound
                    && interruptedRound instanceof OperatingRound) {
                ShareSellingRound ssr = (ShareSellingRound) currentRound;
                int remainingCash = ssr.getRemainingCashToRaise();
                if (remainingCash > 0) {
                    Currency.fromBank(remainingCash, company);
                    company.setBankLoan(remainingCash);
                    String message = LocalText.getText("CompanyGetsLoanToBuyTrain",
                            company, Bank.format(gameManager, remainingCash));
                    ReportBuffer.add(ssr, message);
                    DisplayBuffer.add(ssr, message);
                }
            }
        }

        return newPresident;
    }

    /**
     * The 1837 rules (up to v3) do not say a word on how to handle
     * any remaining coal and minor companies when their owner goes bankrupt.
     * Even though bankruptcy is a very unlikely event in 1837,
     * and going bankrupt while owning minors is even more so,
     * something has to be done if it happens.
     *
     * The tentative rules implemented here are:
     * - Coal companies: exchange the charter for a reserved certificate
     *   of its related major company. This exchange can cause the major to float.
     * - Minors that start a national company: exchange for the national
     *   presidency certificate. This action only causes the national to float
     *   if its starting phase has been reached.
     * - Minors that do not start a national, and the second U1/U3 certificates:
     *   exchange for a non-presidency national share.
     * - All major and national certificates released this way go to the Pool
     *   and may be bought in a Stock Round at current price.
     * - Not yet started national companies may start in a subsequent national formation round,
     *   provided that the presidency has been bought in a preceding stock round,
     *   and that the voluntary start phase has been reached (Sd,KK: 4, Ug: 4E).
     *
     * This method has not yet been tested for 1835 and for the 1837 minors.
     *
     * @param gameManager The game manager
     * @param bankrupt The bankrupt player
     * @param minor Any coal or minor company in which the bankrupt has a share.
     * @return Null, except if a new president can be assigned (1837 U1/U3).
     */
    private static Player processMinorAfterPlayerBankruptcy_1835(
            GameManager gameManager, Player bankrupt, PublicCompany minor) {

        RoundFacade round = gameManager.getCurrentRound();
        if (round instanceof ShareSellingRound) {

            PublicCompany major = minor.getRelatedPublicCompany();
            boolean getPresidentCert = false;

            /* For now, only 1837 nationals configure their starting minor.
             * TODO 1835 and other games that are started by a preconfigured merging minor
             */
            if (major instanceof PublicCompany_1837) {
                // U1 and U3 are special cases
                if (minor.getId().matches("U[13]")
                        && minor.getPresident().equals(bankrupt)
                        && bankrupt.getPortfolioModel().getShare(minor) < 100) {
                    // Swap the president share first with the co-owner
                    Player newPresident = minor.findNextPotentialPresident(1);
                    bankrupt.getPortfolioModel().swapPresidentCertificate(minor,
                            newPresident.getPortfolioModel(), 1);
                    ReportBuffer.add(minor, LocalText.getText("IS_NOW_PRES_OF",
                            newPresident,
                            minor));
                    //In this particular case, we do not exchange the majors
                    return newPresident;
                } else {
                    PublicCompany_1837 startingMinor = ((PublicCompany_1837) major).getStartingMinor();
                    getPresidentCert = startingMinor != null && minor.equals(startingMinor);
                }
            }

            Mergers.mergeCompanies(gameManager, minor, major,
                    getPresidentCert, true);

            // Replace the home token, nationals only
            if (major.getType().getId().equals("National")) {
                Mergers.exchangeMinorToken(gameManager, minor, major);
            }

            // For 1837: if major presidency is in pool, mark it started but not floated
            if (getPresidentCert && major.getPresident() == null) {
                //major.start();
            }


        } else {
            // Error?
        }

        // TODO to be completed
        return null;
    }

    /** For 18EU:
     * The company is closed, but it will be restartable.
     * @param oldPresident The bankrupt player
     * @param company The bankrupt company
     * @return null (no new president could be assigned)
     */
    private static Player processCompanyAfterPlayerBankruptcy_18EU (
            GameManager gameManager, Player oldPresident, PublicCompany company) {

        company.setClosed();  // This also makes majors restartable
        ReportBuffer.add(gameManager, LocalText.getText("CompanyCloses", company.getId()));
        return null;
    }

    /** For 18Scan:
     * The president share of the bankrupt company goes to the Pool,
     * and can be bought in a stock round, restarting the company.
     * @param oldPresident The bankrupt player
     * @param company The bankrupt company
     * @return numm (no new president could be assigned)
     */
    private static Player processCompanyAfterPlayerBankruptcy_18Scan (
            GameManager gameManager, Player oldPresident, PublicCompany company) {

        ReportBuffer.add (gameManager, LocalText.getText(
                "PresidentShareToPool", company.getId()));
        company.setHibernating(true);
        return null;
    }
}

