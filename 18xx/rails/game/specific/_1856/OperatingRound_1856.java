package rails.game.specific._1856;

import java.util.ArrayList;
import java.util.List;

import rails.game.Bank;
import rails.game.DisplayBuffer;
import rails.game.GameManagerI;
import rails.game.OperatingRound;
import rails.game.PublicCompanyI;
import rails.game.ReportBuffer;
import rails.game.TrainI;
import rails.game.TrainManager;
import rails.game.action.ReachDestinations;
import rails.game.action.TakeLoans;
import rails.game.move.CashMove;
import rails.game.state.IntegerState;
import rails.util.LocalText;

public class OperatingRound_1856 extends OperatingRound {
    
    public OperatingRound_1856 (GameManagerI gameManager) {
        super (gameManager);
        
    }

    /** 
     * Implements special rules for first time operating in 1856
     */
    protected boolean setNextOperatingCompany(boolean initial) {
        
        
        if (operatingCompanyIndexObject == null) {
            operatingCompanyIndexObject =
                    new IntegerState("OperatingCompanyIndex");
        }
        
        while (true) {
            if (initial) {
                operatingCompanyIndexObject.set(0);
                initial = false;
            } else {
                operatingCompanyIndexObject.add(1);
            }
            
            operatingCompanyIndex = operatingCompanyIndexObject.intValue();
            
            if (operatingCompanyIndex >= operatingCompanyArray.length) {
                return false;
            } else {
                operatingCompany = operatingCompanyArray[operatingCompanyIndex];
                
                // 1856 special: check if the company may operate
                if (!operatingCompany.hasOperated()) {
                    int soldPercentage 
                        = 100 - operatingCompany.getUnsoldPercentage();
                    
                    TrainI nextAvailableTrain = TrainManager.get().getAvailableNewTrains().get(0);
                    int trainNumber;
                    try { 
                        trainNumber = Integer.parseInt(nextAvailableTrain.getName());
                    } catch (NumberFormatException e) {
                        trainNumber = 6; // Diesel!
                    }
                    int floatPercentage = 10 * trainNumber;
                    
                    log.debug ("Float percentage is "+floatPercentage
                            +" sold percentage is "+soldPercentage);
                    

                    if (soldPercentage < floatPercentage) {
                        DisplayBuffer.add(LocalText.getText("MayNotYetOperate",
                                operatingCompany.getName(),
                                String.valueOf(soldPercentage),
                                String.valueOf(floatPercentage)
                        ));
                        // Company may not yet operate
                        continue;
                    }
                    
                }
                return true;
            }
        }
    }

    protected void setDestinationActions() {
        
        List<PublicCompanyI> possibleDestinations = new ArrayList<PublicCompanyI>();
        for (PublicCompanyI comp : operatingCompanyArray) {
            if (comp.hasDestination()
                    && ((PublicCompany_1856)comp).getTrainNumberAvailableAtStart() < 5
                    && !comp.hasReachedDestination()) {
                possibleDestinations.add (comp);
            }
        }
        if (possibleDestinations.size() > 0) {
            possibleActions.add(new ReachDestinations (possibleDestinations));
        }
    }
    
    protected void reachDestination (PublicCompanyI company) {

        PublicCompany_1856 comp = (PublicCompany_1856) company;
        int cashInEscrow = comp.getMoneyInEscrow();
        if (cashInEscrow > 0) {
            new CashMove (null, company, cashInEscrow);
            ReportBuffer.add(LocalText.getText("ReleasedFromEscrow",
                    company.getName(),
                    Bank.format(cashInEscrow)
            ));
        }

    }
    
    protected void setGameSpecificPossibleActions() {

        // Take a loan
        if ((loansThisRound == null
                || !loansThisRound.containsKey(operatingCompany)
                || loansThisRound.get(operatingCompany) == 0)
            && gameManager.getCurrentPhase().getIndex() 
                <= gameManager.getPhaseManager().getPhaseByName("4").getIndex()
            && operatingCompany.getCurrentNumberOfLoans() 
                < operatingCompany.sharesOwnedByPlayers()) {
            possibleActions.add(new TakeLoans(operatingCompany, 
                    1, operatingCompany.getValuePerLoan()));
        }
    }

    protected String validateTakeLoans (TakeLoans action) {
        
        String errMsg = super.validateTakeLoans(action);
        
        if (errMsg == null) {
            
            while (true) {
                // Still allowed in current phase?
                if (gameManager.getCurrentPhase().getIndex() 
                        > gameManager.getPhaseManager().getPhaseByName("4").getIndex()) {
                    errMsg = LocalText.getText("WrongPhase",
                            gameManager.getCurrentPhase().getName());
                    break;
                }
                // Exceeds number of shares in player hands?
                int newLoans = operatingCompany.getCurrentNumberOfLoans()
                        + action.getNumberTaken();
                int maxLoans = operatingCompany.sharesOwnedByPlayers();
                if (newLoans > maxLoans) {
                    errMsg = LocalText.getText("WouldExceedSharesAtPlayers",
                            newLoans, maxLoans);
                    break;
                }
                break;
            }
        }
        return errMsg;
    }

    protected int calculateLoanAmount (int numberOfLoans) {
        
        int amount = super.calculateLoanAmount(numberOfLoans);
        
        // Deduct interest immediately?
        if (stepObject.intValue() > STEP_PAYOUT) {
            int interest = numberOfLoans 
                    * operatingCompany.getValuePerLoan()
                    * operatingCompany.getLoanInterestPct() / 100;
            amount -= interest;
        }
        
        return amount;
    }
}
