package net.sf.rails.game.financial;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Set;

import rails.game.action.DiscardTrain;
import rails.game.action.FoldIntoNational;
import rails.game.action.PossibleAction;

import com.google.common.collect.Iterables;

import net.sf.rails.common.DisplayBuffer;
import net.sf.rails.common.LocalText;
import net.sf.rails.common.ReportBuffer;
import net.sf.rails.game.BaseToken;
import net.sf.rails.game.Company;
import net.sf.rails.game.GameManager;
import net.sf.rails.game.MapHex;
import net.sf.rails.game.Phase;
import net.sf.rails.game.Player;
import net.sf.rails.game.PrivateCompany;
import net.sf.rails.game.PublicCompany;
import net.sf.rails.game.Stop;
import net.sf.rails.game.Train;
import net.sf.rails.game.round.RoundFacade;
import net.sf.rails.game.special.ExchangeForShare;
import net.sf.rails.game.special.SpecialProperty;
import net.sf.rails.game.state.Currency;

public class NationalFormationRound extends StockRound {

    public NationalFormationRound(GameManager parent, String id) {
        super(parent, id);
        // TODO Auto-generated constructor stub
    }


    private static PublicCompany nationalToFound;
    private PublicCompany nationalStartingMinor;
    private Phase phase;
    private boolean startNational;
    private boolean forcedStart;
    private boolean mergeNational;
    private boolean forcedMerge;
    private List<Company> foldablePreNationals;

    protected enum Step {
            START,
            MERGE,
            DISCARD_TRAINS
        }

    Step step;


    public static boolean nationalIsComplete(GameManager gameManager, String nationalInFounding) {
         
        for (PublicCompany company : gameManager.getAllPublicCompanies()) {
            if (company.isRelatedToNational(nationalInFounding)) {
                 if (!company.isClosed()) return false;
            }
        }
        return true;
    }

    @Override
    public void start() {
    
        PublicCompany nationalToFound = gameManager.getNationalToFound();
        phase = Phase.getCurrent(this);
        startNational = !nationalToFound.hasStarted();
        forcedMerge = phase.getId().equals("5"); //TODO Make setable
        forcedStart = phase.getId().equals("4+4") || forcedMerge;//TODO Make setable
        mergeNational = !nationalIsComplete(gameManager, nationalToFound.getId());
    
        ReportBuffer.add(this, LocalText.getText("StartFormationRound", nationalToFound.getId()));
        log.debug("StartNational="+startNational+" forcedStart="+forcedStart
                +" mergeNational="+mergeNational+" forcedMerge="+forcedMerge);
    
        step = startNational ? Step.START : Step.MERGE;
    
        if (step == Step.START) { //Attention there might be more than one National in Merge at once..
            
            nationalStartingMinor = getRoot().getCompanyManager().getPublicCompany(nationalToFound.getFoundingStartCompany());
            setCurrentPlayer(nationalStartingMinor.getPresident());
            gameManager.setNationalFormationStartingPlayer( nationalToFound, currentPlayer);
            if (forcedStart) {
                executeStartNational(true, nationalToFound);
                step = Step.MERGE;
            }
        }
    
        if (step == Step.MERGE) {
            startingPlayer
            = gameManager.getNationalFormationStartingPlayer(nationalToFound);
            log.debug("Original National starting player was "+startingPlayer.getId());
            setCurrentPlayer(startingPlayer);
            if (forcedMerge) {
                Set<SpecialProperty> sps;
                setFoldablePreNationals(nationalToFound.getAlias());
                List<Company> foldables = new ArrayList<Company> ();
                for (PrivateCompany company : gameManager.getAllPrivateCompanies()) {
                    if (company.isClosed()) continue;
                    sps = company.getSpecialProperties();
                    if (sps != null && !sps.isEmpty() && Iterables.get(sps, 0) instanceof ExchangeForShare) {
                        foldables.add(company);
                    }
                }
                for (PublicCompany company : gameManager.getAllPublicCompanies()) {
                    if (company.isClosed()) continue;
                    sps = company.getSpecialProperties();
                    if (sps != null && !sps.isEmpty() && Iterables.get(sps, 0) instanceof ExchangeForShare) {
                        foldables.add(company);
                    }
                }
                executeExchange (foldables, false, true);
    
                // Check if the National must discard any trains
                if (nationalToFound.getNumberOfTrains() > nationalToFound.getCurrentTrainLimit()) {
                    step = Step.DISCARD_TRAINS;
                } else {
                    finishRound();
                }
            } else {
                findNextMergingPlayer(false);
            }
        }
    }

    private void setFoldablePreNationals(String nationalInFounding2) {
            
        foldablePreNationals = new ArrayList<Company> ();

        PublicCompany company;
        Set<SpecialProperty> sps;
        for (PublicCertificate cert : currentPlayer.getPortfolioModel().getCertificates()) {
            company = cert.getCompany();
            if (company.isRelatedToNational(nationalInFounding2)) {
                sps = company.getSpecialProperties();
                if (sps != null && !sps.isEmpty() && Iterables.get(sps, 0) instanceof ExchangeForShare) {
                    foldablePreNationals.add(company);
                }
             }
        }
    }

    @Override
    public boolean setPossibleActions() {
    
        if (step == Step.START) {
            Player startingMinorOwner = nationalStartingMinor.getPresident();
            startingPlayer = startingMinorOwner;
            setCurrentPlayer(startingMinorOwner);
            ReportBuffer.add(this, LocalText.getText("StartingPlayer", 
                    playerManager.getCurrentPlayer().getId()
                    ));
    
            possibleActions.add(new FoldIntoNational(nationalStartingMinor));
    
        } else if (step == Step.MERGE) {
    
            possibleActions.add(new FoldIntoNational(foldablePreNationals));
    
        } else if (step == Step.DISCARD_TRAINS) {
    
            if (nationalToFound.getNumberOfTrains() > nationalToFound.getCurrentTrainLimit()) {
                log.debug("+++ National " +nationalToFound.getLongName() +" has "+nationalToFound.getNumberOfTrains()+", limit is "+nationalToFound.getCurrentTrainLimit());
                possibleActions.add(new DiscardTrain(nationalToFound,
                        nationalToFound.getPortfolioModel().getUniqueTrains(), true));
            }
        }
        return true;
    
    }


    @Override
    protected boolean processGameSpecificAction(PossibleAction action) {
    
        if (action instanceof FoldIntoNational) {
    
            FoldIntoNational a = (FoldIntoNational) action;
    
            if (step == Step.START) {
                if (!startNational(a)) {
                    finishRound();
                } else {
                    step = Step.MERGE;
                    findNextMergingPlayer(false);
                }
    
            } else if (step == Step.MERGE) {
    
                mergeIntoNational (a);
    
            }
    
            return true;
    
        } else if (action instanceof DiscardTrain) {
    
            discardTrain ((DiscardTrain) action);
            return true;
    
        } else {
            return false;
        }
    }

    protected boolean findNextMergingPlayer(boolean skipCurrentPlayer) {
    
        while (true) {
    
            if (skipCurrentPlayer) {
                setNextPlayer();
                if (playerManager.getCurrentPlayer() == startingPlayer) {
                    if (nationalToFound.getNumberOfTrains() > nationalToFound.getCurrentTrainLimit()) {
                        step = Step.DISCARD_TRAINS;
                    } else {
                        finishRound();
                    }
                    return false;
                }
            }
    
            setFoldablePreNationals(nationalToFound.getAlias());
            if (!foldablePreNationals.isEmpty()) return true;
            skipCurrentPlayer = true;
        }
    }

    private boolean startNational(FoldIntoNational action) {
    
        // Validate
        String errMsg = null;
    
        List<Company> folded = action.getFoldedCompanies();
        boolean folding = folded != null && !folded.isEmpty();
    
        while (folding) {
            if (!(nationalStartingMinor.getId().equals(action.getFoldedCompanyNames()))) {
                errMsg = LocalText.getText("WrongCompany",
                        action.getFoldedCompanyNames(),
                        nationalStartingMinor.getId());
                break;
            }
            break;
        }
    
        if (errMsg != null) {
            DisplayBuffer.add(this, LocalText.getText("CannotMerge",
                    action.getFoldedCompanyNames(),
                    nationalToFound.getId(),
                    errMsg));
            return false;
        }
    
        // all actions linked during formation round to avoid serious undo problems
        
        // FIXME: changeStack.linkToPreviousMoveSet();
    
        if (folding) executeStartNational(false, nationalToFound);
    
        return folding;
    }

    private void executeStartNational(boolean display, PublicCompany comp) {
    
        nationalToFound.start();
        String message = LocalText.getText("START_MERGED_COMPANY",
                comp.getId(),
                Bank.format(this, nationalToFound.getIPOPrice()),
                nationalToFound.getStartSpace().toText());
        ReportBuffer.add(this, message);
        if (display) DisplayBuffer.add(this, message);
    
        // add money from sold shares
        // Move cash and shares where required
        int capFactor = nationalToFound.getSoldPercentage() / (nationalToFound.getShareUnit() * nationalToFound.getShareUnitsForSharePrice());
        int cash = capFactor * nationalToFound.getIPOPrice();
    
        if (cash > 0) {
            String cashText = Currency.fromBank(cash, nationalToFound);
            ReportBuffer.add(this, LocalText.getText("FloatsWithCash",
                nationalToFound.getId(),
                cashText ));
        } else {
            ReportBuffer.add(this, LocalText.getText("Floats",
                    nationalToFound.getId()));
        }
    
        executeExchange (Arrays.asList(new Company[]{nationalStartingMinor}), true, false);
        nationalToFound.setFloated();
    }

    private boolean mergeIntoNational(FoldIntoNational action) {
    
        // Validate
        // String errMsg = null;
    
        List<Company> folded = action.getFoldedCompanies();
        boolean folding = folded != null && !folded.isEmpty();
    
        while (folding) {
            // TODO Some validation needed
            break;
        }
    
        // TODO: This is now dead code, but won't be when some sensible validations exist 
        /*
        if (errMsg != null) {
            DisplayBuffer.add(this, LocalText.getText("CannotMerge",
                    action.getFoldedCompanyNames(),
                    PR_ID,
                    errMsg));
            return false; 
        }
        */
    
        // all actions linked during formation round to avoid serious undo problems
        
        // FIMXE: changeStack.linkToPreviousMoveSet();
    
        // Execute
        if (folding) executeExchange (folded, false, false);
    
        findNextMergingPlayer(true);
    
        return folding;
    }

    private void executeExchange(List<Company> companies, boolean president, boolean display) {
    
        ExchangeForShare efs;
        PublicCertificate cert;
        Player player;
        for (Company company : companies) {
            log.debug("Merging company "+company.getId());
            if (company instanceof PrivateCompany) {
                player = (Player)((PrivateCompany)company).getOwner();
            } else {
                player = ((PublicCompany)company).getPresident();
            }
            // Shortcut, sp should be checked
            efs = (ExchangeForShare) Iterables.get(company.getSpecialProperties(), 0);
            cert = unavailable.findCertificate(nationalToFound, efs.getShare()/nationalToFound.getShareUnit(),
                    president);
            cert.moveTo(player);
            //company.setClosed();
            String message = LocalText.getText("MERGE_MINOR_LOG",
                    player.getId(),
                    company.getId(),
                    nationalToFound.getId(),
                    company instanceof PrivateCompany ? "no"
                            : Bank.format(this, ((PublicCompany)company).getCash()),
                    company instanceof PrivateCompany ? "no"
                            : ((PublicCompany)company).getPortfolioModel().getTrainList().size());
            ReportBuffer.add(this, message);
            if (display) DisplayBuffer.add(this, message);
            message = LocalText.getText("GetShareForMinor",
                    player.getId(),
                    cert.getShare(),
                    nationalToFound.getId(),
                    ipo.getParent().getId(),
                    company.getId());
            ReportBuffer.add(this, message);
            if (display) DisplayBuffer.add(this, message);
    
            if (company instanceof PublicCompany) {
    
                PublicCompany minor = (PublicCompany) company;
    
                // Replace the home token
                BaseToken token = Iterables.get(minor.getAllBaseTokens(),0);
                Stop city = (Stop) token.getOwner();
                MapHex hex = city.getParent();
                token.moveTo(minor);
                if (!hex.hasTokenOfCompany(nationalToFound) && hex.layBaseToken(nationalToFound, city)) {
                    /* TODO: the false return value must be impossible. */
                    message = LocalText.getText("ExchangesBaseToken",
                            nationalToFound.getId(), minor.getId(),
                            city.getRelatedNumber());
                    ReportBuffer.add(this, message);
                    if (display) DisplayBuffer.add(this, message);
    
                    nationalToFound.layBaseToken(hex, 0);
                }
    
                // Move any cash
                if (minor.getCash() > 0) {
                    Currency.wireAll(minor, nationalToFound);
                }
    
                // Move any trains
                // TODO: Simplify code due to trainlist being immutable anyway
                List<Train> trains = new ArrayList<Train> (minor.getPortfolioModel().getTrainList());
                for (Train train : trains) {
                    nationalToFound.getPortfolioModel().addTrain(train);
                }
            }
    
            // Close the merged companies
            company.setClosed();
        }
    
    }

    public boolean discardTrain(DiscardTrain action) {
    
        Train train = action.getDiscardedTrain();
        PublicCompany company = action.getCompany();
    
        String errMsg = null;
    
        // Dummy loop to enable a quick jump out.
        while (true) {
            // Checks
            // Must be correct step
            if (company != nationalToFound) {
                errMsg = LocalText.getText("WrongCompany", company.getId(), nationalToFound.getId());
                break;
            }
    
            if (train == null && action.isForced()) {
                errMsg = LocalText.getText("NoTrainSpecified");
                break;
            }
    
            // Does the company own such a train?
            if (!company.getPortfolioModel().getTrainList().contains(train)) {
                errMsg =
                    LocalText.getText("CompanyDoesNotOwnTrain",
                                company.getId(),
                                train.toText() );
                break;
            }
    
            break;
        }
        if (errMsg != null) {
            DisplayBuffer.add(this, LocalText.getText("CannotDiscardTrain",
                    company.getId(),
                    (train != null ?train.toText() : "?"),
                    errMsg ));
            return false;
        }
    
        /* End of validation, start of execution */
        
        // FIXME: if (action.isForced()) changeStack.linkToPreviousMoveSet();
    
        train.discard();
    
        // We still might have another excess train
        // TODO: would be better to have DiscardTrain discard multiple trains
        if (nationalToFound.getNumberOfTrains() > nationalToFound.getCurrentTrainLimit()) {
            step = Step.DISCARD_TRAINS;
        } else {
            finishRound();
        }
    
        return true;
    }

    @Override
    protected void finishRound() {
        RoundFacade interruptedRound = gameManager.getInterruptedRound();
        ReportBuffer.add(this, " ");
        if (interruptedRound != null) {
            ReportBuffer.add(this, LocalText.getText("EndOfFormationRound", nationalToFound.getId(),
                    interruptedRound.getRoundName()));
        } else {
            ReportBuffer.add(this, LocalText.getText("EndOfFormationRoundNoInterrupt", nationalToFound.getId()));
        }
    
        if (nationalToFound.hasStarted()) nationalToFound.checkPresidency();
        nationalToFound.setOperated(); // To allow immediate share selling
        //        super.finishRound();
        // Inform GameManager
        gameManager.nextRound(this);
    }


    @Override
    public String toString() {
        return "1837 KuKFormationRound";
    }

}