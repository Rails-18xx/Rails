/**
 * 
 */
package net.sf.rails.game.specific._1837;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Set;

import net.sf.rails.common.DisplayBuffer;
import net.sf.rails.common.GuiDef;
import net.sf.rails.common.LocalText;
import net.sf.rails.common.ReportBuffer;
import net.sf.rails.game.Bank;
import net.sf.rails.game.BaseToken;
import net.sf.rails.game.Company;
import net.sf.rails.game.GameManager;
import net.sf.rails.game.MapHex;
import net.sf.rails.game.Phase;
import net.sf.rails.game.Player;
import net.sf.rails.game.PrivateCompany;
import net.sf.rails.game.PublicCertificate;
import net.sf.rails.game.PublicCompany;
import net.sf.rails.game.Round;
import net.sf.rails.game.StockRound;
import net.sf.rails.game.Stop;
import net.sf.rails.game.Train;
import net.sf.rails.game.special.ExchangeForShare;
import net.sf.rails.game.special.SpecialProperty;
import net.sf.rails.game.state.Currency;
import rails.game.action.DiscardTrain;
import rails.game.action.PossibleAction;
import rails.game.specific._1837.FoldIntoSuedbahn;

import com.google.common.collect.Iterables;

/**
 * @author martin
 * @date 27.07.2014 @time 12:51:08
 */
public class SuedBahnFormationRound extends StockRound {

    private PublicCompany Suedbahn;
    private PublicCompany S1;
    private Phase phase;

    private boolean startPr;
    private boolean forcedStart;
    private boolean mergePr;
    private boolean forcedMerge;

    private List<Company> foldablePreSuedbahns;

    private enum Step {
        START,
        MERGE,
        DISCARD_TRAINS
    };

    Step step;

    private static String SU_ID = GameManager_1837.SU_ID;
    private static String S1_ID = GameManager_1837.S1_ID;

    /**
     * Constructed via Configure
     */
    public SuedBahnFormationRound (GameManager parent, String id) {
        super(parent, id);

        guiHints.setVisibilityHint(GuiDef.Panel.MAP, true);
        guiHints.setVisibilityHint(GuiDef.Panel.STATUS, true);
    }
    
    @Override
    public void start() {

        Suedbahn = companyManager.getPublicCompany(SU_ID);
        phase = getCurrentPhase();
        startPr = !Suedbahn.hasStarted();
        forcedMerge = phase.getId().equals("5");
        forcedStart = phase.getId().equals("4+4") || forcedMerge;
        mergePr = !SuedbahnIsComplete(gameManager);

        ReportBuffer.add(this, LocalText.getText("StartFormationRound", SU_ID));
        log.debug("StartPr="+startPr+" forcedStart="+forcedStart
                +" mergePr="+mergePr+" forcedMerge="+forcedMerge);

        step = startPr ? Step.START : Step.MERGE;

        if (step == Step.START) {
            S1 = companyManager.getPublicCompany(S1_ID);
            setCurrentPlayer(S1.getPresident());
            ((GameManager_1837)gameManager).setSuedbahnFormationStartingPlayer(currentPlayer);
            if (forcedStart) {
                executeStartSuedbahn(true);
                step = Step.MERGE;
            }
        }

        if (step == Step.MERGE) {
            startingPlayer
            = ((GameManager_1837)gameManager).getSuedbahnFormationStartingPlayer();
            log.debug("Original Suedbahn starting player was "+startingPlayer.getId());
            setCurrentPlayer(startingPlayer);
            if (forcedMerge) {
                Set<SpecialProperty> sps;
                setFoldablePreSuedbahns();
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

                // Check if the PR must discard any trains
                if (Suedbahn.getNumberOfTrains() > Suedbahn.getCurrentTrainLimit()) {
                    step = Step.DISCARD_TRAINS;
                } else {
                    finishRound();
                }
            } else {
                findNextMergingPlayer(false);
            }
        }
    }

    @Override
    public boolean setPossibleActions() {

        if (step == Step.START) {
            Player S1Owner = S1.getPresident();
            startingPlayer = S1Owner;
            setCurrentPlayer(S1Owner);
            ReportBuffer.add(this, LocalText.getText("StartingPlayer",
                    playerManager.getCurrentPlayer().getId()));

            possibleActions.add(new FoldIntoSuedbahn(S1));

        } else if (step == Step.MERGE) {

            possibleActions.add(new FoldIntoSuedbahn(foldablePreSuedbahns));

        } else if (step == Step.DISCARD_TRAINS) {

            if (Suedbahn.getNumberOfTrains() > Suedbahn.getCurrentTrainLimit()) {
                log.debug("+++ PR has "+Suedbahn.getNumberOfTrains()+", limit is "+Suedbahn.getCurrentTrainLimit());
                possibleActions.add(new DiscardTrain(Suedbahn,
                        Suedbahn.getPortfolioModel().getUniqueTrains(), true));
            }
        }
        return true;

    }

    private void setFoldablePreSuedbahns () {

        foldablePreSuedbahns = new ArrayList<Company> ();
        SpecialProperty sp;
        for (PrivateCompany company : currentPlayer.getPortfolioModel().getPrivateCompanies()) {
            sp = Iterables.get(company.getSpecialProperties(), 0);
            if (sp instanceof ExchangeForShare) {
                foldablePreSuedbahns.add(company);
            }
        }
        PublicCompany company;
        Set<SpecialProperty> sps;
        for (PublicCertificate cert : currentPlayer.getPortfolioModel().getCertificates()) {
            if (!cert.isPresidentShare()) continue;
            company = cert.getCompany();
            sps = company.getSpecialProperties();
            if (sps != null && !sps.isEmpty() && Iterables.get(sps, 0) instanceof ExchangeForShare) {
                foldablePreSuedbahns.add(company);
            }
        }
    }

    @Override
    protected boolean processGameSpecificAction(PossibleAction action) {

        if (action instanceof FoldIntoSuedbahn) {

            FoldIntoSuedbahn a = (FoldIntoSuedbahn) action;

            if (step == Step.START) {
                if (!startSuedbahn(a)) {
                    finishRound();
                } else {
                    step = Step.MERGE;
                    findNextMergingPlayer(false);
                }

            } else if (step == Step.MERGE) {

                mergeIntoSuedbahn (a);

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
                    if (Suedbahn.getNumberOfTrains() > Suedbahn.getCurrentTrainLimit()) {
                        step = Step.DISCARD_TRAINS;
                    } else {
                        finishRound();
                    }
                    return false;
                }
            }

            setFoldablePreSuedbahns();
            if (!foldablePreSuedbahns.isEmpty()) return true;
            skipCurrentPlayer = true;
        }
    }

    private boolean startSuedbahn (FoldIntoSuedbahn action) {

        // Validate
        String errMsg = null;

        List<Company> folded = action.getFoldedCompanies();
        boolean folding = folded != null && !folded.isEmpty();

        while (folding) {
            if (!(S1_ID.equals(action.getFoldedCompanyNames()))) {
                errMsg = LocalText.getText("WrongCompany",
                        action.getFoldedCompanyNames(),
                        GameManager_1837.S1_ID);
                break;
            }
            break;
        }

        if (errMsg != null) {
            DisplayBuffer.add(this, LocalText.getText("CannotMerge",
                    action.getFoldedCompanyNames(),
                    SU_ID,
                    errMsg));
            return false;
        }

        // all actions linked during formation round to avoid serious undo problems
        
        // FIXME: changeStack.linkToPreviousMoveSet();

        if (folding) executeStartSuedbahn(false);

        return folding;
    }

    private void executeStartSuedbahn (boolean display) {

        Suedbahn.start();
        String message = LocalText.getText("START_MERGED_COMPANY",
                SU_ID,
                Bank.format(this, Suedbahn.getIPOPrice()),
                Suedbahn.getStartSpace().toText());
        ReportBuffer.add(this, message);
        if (display) DisplayBuffer.add(this, message);

        // add money from sold shares
        // Move cash and shares where required
        int capFactor = Suedbahn.getSoldPercentage() / (Suedbahn.getShareUnit() * Suedbahn.getShareUnitsForSharePrice());
        int cash = capFactor * Suedbahn.getIPOPrice();

        if (cash > 0) {
            String cashText = Currency.fromBank(cash, Suedbahn);
            ReportBuffer.add(this, LocalText.getText("FloatsWithCash",
                Suedbahn.getId(),
                cashText ));
        } else {
            ReportBuffer.add(this, LocalText.getText("Floats",
                    Suedbahn.getId()));
        }

        executeExchange (Arrays.asList(new Company[]{S1}), true, false);
        Suedbahn.setFloated();
    }

    private boolean mergeIntoSuedbahn (FoldIntoSuedbahn action) {

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

    private void executeExchange (List<Company> companies, boolean president,
            boolean display) {

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
            cert = unavailable.findCertificate(Suedbahn, efs.getShare()/Suedbahn.getShareUnit(),
                    president);
            cert.moveTo(player);
            //company.setClosed();
            String message = LocalText.getText("MERGE_MINOR_LOG",
                    player.getId(),
                    company.getId(),
                    SU_ID,
                    company instanceof PrivateCompany ? "no"
                            : Bank.format(this, ((PublicCompany)company).getCash()),
                    company instanceof PrivateCompany ? "no"
                            : ((PublicCompany)company).getPortfolioModel().getTrainList().size());
            ReportBuffer.add(this, message);
            if (display) DisplayBuffer.add(this, message);
            message = LocalText.getText("GetShareForMinor",
                    player.getId(),
                    cert.getShare(),
                    SU_ID,
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
                if (!hex.hasTokenOfCompany(Suedbahn) && hex.layBaseToken(Suedbahn, city)) {
                    /* TODO: the false return value must be impossible. */
                    message = LocalText.getText("ExchangesBaseToken",
                            SU_ID, minor.getId(),
                            city.getSpecificId());
                    ReportBuffer.add(this, message);
                    if (display) DisplayBuffer.add(this, message);

                    Suedbahn.layBaseToken(hex, 0);
                }

                // Move any cash
                if (minor.getCash() > 0) {
                    Currency.wireAll(minor, Suedbahn);
                }

                // Move any trains
                // TODO: Simplify code due to trainlist being immutable anyway
                List<Train> trains = new ArrayList<Train> (minor.getPortfolioModel().getTrainList());
                for (Train train : trains) {
                    Suedbahn.getPortfolioModel().addTrain(train);
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
            if (company != Suedbahn) {
                errMsg = LocalText.getText("WrongCompany", company.getId(), Suedbahn.getId());
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

        pool.addTrain(train);
        ReportBuffer.add(this, LocalText.getText("CompanyDiscardsTrain",
                company.getId(),
                train.toText() ));

        // We still might have another excess train
        // TODO: would be better to have DiscardTrain discard multiple trains
        if (Suedbahn.getNumberOfTrains() > Suedbahn.getCurrentTrainLimit()) {
            step = Step.DISCARD_TRAINS;
        } else {
            finishRound();
        }

        return true;
    }

    @Override
    protected void finishRound() {
        Round interruptedRound = gameManager.getInterruptedRound();
        ReportBuffer.add(this, " ");
        if (interruptedRound != null) {
            ReportBuffer.add(this, LocalText.getText("EndOfFormationRound", SU_ID,
                    interruptedRound.getRoundName()));
        } else {
            ReportBuffer.add(this, LocalText.getText("EndOfFormationRoundNoInterrupt", SU_ID));
        }

        if (Suedbahn.hasStarted()) Suedbahn.checkPresidency();
        Suedbahn.setOperated(); // To allow immediate share selling
        //        super.finishRound();
        // Inform GameManager
        gameManager.nextRound(this);
    }

    public static boolean SuedbahnIsComplete(GameManager gameManager) {

        for (PublicCompany company : gameManager.getAllPublicCompanies()) {
            if (!company.getType().getId().equalsIgnoreCase("Minor")) continue;
            if (!company.isClosed()) return false;
        }
        return true;
    }

    @Override
    public String toString() {
        return "1837 SuedbahnFormationRound";
    }
}
