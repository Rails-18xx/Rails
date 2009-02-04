package rails.game.specific._1856;

import java.util.*;

import rails.game.*;
import rails.game.action.RepayLoans;
import rails.game.move.CashMove;
import rails.util.LocalText;


public class CGRFormationRound extends OperatingRound {
    /* This isn't really a stock round, but it must subclass one of the
     * three base types, otherwise the not-subclassable GameUIManager
     * cannot handle it. StockRound has been chosen because the UI
     * should show the StatusWindow.
     */

    private Player startingPlayer;
    private Map<Player, List<PublicCompanyI>> companiesToRepayLoans = null;
    private PublicCompanyI currentCompany = null;
    private int maxLoansToRepayByPresident = 0;
    private List<PublicCompanyI> mergingCompanies = new ArrayList<PublicCompanyI>();

    public CGRFormationRound (GameManagerI gameManager) {
        super (gameManager);
    }

    @Override
    /** This class needs the game status window to show up
     * rather than the operating round window.
     */
    public Class<? extends RoundI> getRoundTypeForUI () {
        return StockRound.class;
    }

    public void start (Player startingPlayer) {

        this.startingPlayer = startingPlayer;

        Player president;

        companiesToRepayLoans = null;

        ReportBuffer.add(LocalText.getText("StartCGRFormationRound",
                startingPlayer.getName()));

        // Collect companies having loans
        for (PublicCompanyI company : getOperatingCompanies()) {
            if (company.getCurrentNumberOfLoans() > 0) {
                if (companiesToRepayLoans == null) {
                    companiesToRepayLoans
                        = new HashMap<Player, List<PublicCompanyI>>();
                }
                president = company.getPresident();
                if (!companiesToRepayLoans.containsKey(president)) {
                    companiesToRepayLoans.put (president, new ArrayList<PublicCompanyI>());
                }
                companiesToRepayLoans.get(president).add(company);
            }
        }

        setCurrentPlayer (startingPlayer);

        setNextCompanyNeedingPresidentIntervention();
    }

    private boolean setNextCompanyNeedingPresidentIntervention () {

        while (true) {

            while (!companiesToRepayLoans.containsKey(getCurrentPlayer())) {
                gameManager.setNextPlayer();
                if (getCurrentPlayer().equals(startingPlayer)) {
                    return false;
                }
            }

            // Player to act already has been selected
            Player player = getCurrentPlayer();
            if (companiesToRepayLoans.get(player).isEmpty()) {
                companiesToRepayLoans.remove(player);
                continue;
            }
            currentCompany = companiesToRepayLoans.get(player).get(0);
            companiesToRepayLoans.get(player).remove(currentCompany);

            int numberOfLoans = currentCompany.getCurrentNumberOfLoans();
            if (numberOfLoans == 0) continue;

            int compCash = currentCompany.getCash();
            int presCash = player.getCash();
            int valuePerLoan = currentCompany.getValuePerLoan();
            String message;
            int payment;

            message = LocalText.getText("CompanyHasLoans",
                    currentCompany.getName(),
                    player.getName(),
                    numberOfLoans,
                    Bank.format(valuePerLoan),
                    Bank.format(numberOfLoans * valuePerLoan));
            ReportBuffer.add(message);
            DisplayBuffer.add(message, false);

            // Let company repay all loans for which it has the cash
            int numberToRepay = Math.min(numberOfLoans,
                    compCash / valuePerLoan);
            if (numberToRepay > 0) {
                payment = numberToRepay * valuePerLoan;

                new CashMove (currentCompany, null, payment);
                currentCompany.addLoans(-numberToRepay);

                message = LocalText.getText("CompanyRepaysLoans",
                        currentCompany.getName(),
                        Bank.format(payment),
                        Bank.format(numberOfLoans * valuePerLoan),
                        numberToRepay,
                        Bank.format(valuePerLoan));
                ReportBuffer.add (message);
                DisplayBuffer.add(message, false);
            }

            // If that was all, we're done with this company
            numberOfLoans = currentCompany.getCurrentNumberOfLoans();
            if (numberOfLoans == 0) continue;

            // Check the president's cash
            // He should be involved if at least one extra loan could be repaid
            compCash = currentCompany.getCash();
            if ((compCash + presCash) / valuePerLoan > 0) {
                int maxNumber = Math.min((compCash + presCash)/valuePerLoan, numberOfLoans);
                if (maxNumber == numberOfLoans) {
                    DisplayBuffer.add(LocalText.getText("YouCanRepayAllLoans",
                            player.getName(),
                            maxNumber,
                            currentCompany.getName()),
                        false);
                } else {
                    DisplayBuffer.add(LocalText.getText("YouCannotRepayAllLoans",
                            player.getName(),
                            maxNumber,
                            numberOfLoans,
                            currentCompany.getName()),
                        false);
                }
                maxLoansToRepayByPresident = maxNumber;
                break;
            } else {
                // President cannot help, this company will merge into CGR anyway
                mergingCompanies.add(currentCompany);
                message = LocalText.getText("WillMergeInto",
                        currentCompany.getName(),
                        "CGR");
                DisplayBuffer.add(message, false);
                ReportBuffer.add(message);
                continue;
            }
        }
        return true;
    }

    @Override
    public boolean setPossibleActions() {

        RepayLoans action = new RepayLoans (currentCompany, 0,
                maxLoansToRepayByPresident,
                currentCompany.getValuePerLoan());
        possibleActions.add(action);
        operatingCompany = currentCompany;
        return true;

    }

    @Override
    protected boolean repayLoans (RepayLoans action) {

        boolean result = super.repayLoans(action);
        if (action.getCompany().getCurrentNumberOfLoans() > 0) {
            mergingCompanies.add(currentCompany);
            String message = LocalText.getText("WillMergeInto",
                    currentCompany.getName(),
                    "CGR");
            DisplayBuffer.add(message, true);
            ReportBuffer.add(message);
            
        }

        if (!setNextCompanyNeedingPresidentIntervention()) {

            gameManager.nextRound(this);
        }

        return result;

    }

    @Override
    public String toString() {
        return "1856 CGRFormationRound";
    }

}
