package net.sf.rails.game.specific._1835;

import java.util.*;

import rails.game.action.DiscardTrain;
import rails.game.action.PossibleAction;
import rails.game.specific._1835.FoldIntoPrussian;
import net.sf.rails.common.DisplayBuffer;
import net.sf.rails.common.GuiDef;
import net.sf.rails.common.LocalText;
import net.sf.rails.common.ReportBuffer;
import net.sf.rails.game.*;
import net.sf.rails.game.financial.Bank;
import net.sf.rails.game.financial.PublicCertificate;
import net.sf.rails.game.financial.StockRound;
import net.sf.rails.game.round.RoundFacade;
import net.sf.rails.game.special.ExchangeForShare;
import net.sf.rails.game.special.SpecialProperty;
import net.sf.rails.game.state.Currency;

import com.google.common.collect.Iterables;


public class PrussianFormationRound extends StockRound {

    private PublicCompany prussian;
    private PublicCompany m2;
    private Phase phase;

    private boolean startPr;
    private boolean forcedStart;
    private boolean mergePr;
    private boolean forcedMerge;

    private List<Company> foldablePrePrussians;

    private enum Step {
        START,
        MERGE,
        DISCARD_TRAINS
    };

    Step step;

    private static String PR_ID = GameManager_1835.PR_ID;
    private static String M2_ID = GameManager_1835.M2_ID;

    /**
     * Constructed via Configure
     */
    // change: PrussianFormationRound is a triggered MergerRound
    // requires: make an independent round type with the merger and related activities
    public PrussianFormationRound (GameManager parent, String id) {
        super(parent, id);

        guiHints.setVisibilityHint(GuiDef.Panel.MAP, true);
        guiHints.setVisibilityHint(GuiDef.Panel.STATUS, true);
    }
    
    @Override
    public void start() {

        prussian = companyManager.getPublicCompany(PR_ID);
        phase = Phase.getCurrent(this);
        startPr = !prussian.hasStarted();
        forcedMerge = phase.getId().equals("5");
        forcedStart = phase.getId().equals("4+4") || forcedMerge;
        mergePr = !prussianIsComplete(gameManager);

        ReportBuffer.add(this, LocalText.getText("StartFormationRound", PR_ID));
        log.debug("StartPr="+startPr+" forcedStart="+forcedStart
                +" mergePr="+mergePr+" forcedMerge="+forcedMerge);

        step = startPr ? Step.START : Step.MERGE;

        if (step == Step.START) {
            m2 = companyManager.getPublicCompany(M2_ID);
            setCurrentPlayer(m2.getPresident());
            ((GameManager_1835)gameManager).setPrussianFormationStartingPlayer(currentPlayer);
            if (forcedStart) {
                executeStartPrussian(true);
                step = Step.MERGE;
            }
        }

        if (step == Step.MERGE) {
            startingPlayer
            = ((GameManager_1835)gameManager).getPrussianFormationStartingPlayer();
            log.debug("Original Prussian starting player was "+startingPlayer.getId());
            setCurrentPlayer(startingPlayer);
            if (forcedMerge) {
                Set<SpecialProperty> sps;
                setFoldablePrePrussians();
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
                if (prussian.getNumberOfTrains() > prussian.getCurrentTrainLimit()) {
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
            Player m2Owner = m2.getPresident();
            startingPlayer = m2Owner;
            setCurrentPlayer(m2Owner);
            ReportBuffer.add(this, LocalText.getText("StartingPlayer",
                    playerManager.getCurrentPlayer().getId()));

            possibleActions.add(new FoldIntoPrussian(m2));

        } else if (step == Step.MERGE) {

            possibleActions.add(new FoldIntoPrussian(foldablePrePrussians));

        } else if (step == Step.DISCARD_TRAINS) {

            if (prussian.getNumberOfTrains() > prussian.getCurrentTrainLimit()) {
                log.debug("+++ PR has "+prussian.getNumberOfTrains()+", limit is "+prussian.getCurrentTrainLimit());
                possibleActions.add(new DiscardTrain(prussian,
                        prussian.getPortfolioModel().getUniqueTrains(), true));
            }
        }
        return true;

    }

    private void setFoldablePrePrussians () {

        foldablePrePrussians = new ArrayList<Company> ();
        SpecialProperty sp;
        for (PrivateCompany company : currentPlayer.getPortfolioModel().getPrivateCompanies()) {
            sp = Iterables.get(company.getSpecialProperties(), 0);
            if (sp instanceof ExchangeForShare) {
                foldablePrePrussians.add(company);
            }
        }
        PublicCompany company;
        Set<SpecialProperty> sps;
        for (PublicCertificate cert : currentPlayer.getPortfolioModel().getCertificates()) {
            if (!cert.isPresidentShare()) continue;
            company = cert.getCompany();
            sps = company.getSpecialProperties();
            if (sps != null && !sps.isEmpty() && Iterables.get(sps, 0) instanceof ExchangeForShare) {
                foldablePrePrussians.add(company);
            }
        }
    }

    @Override
    protected boolean processGameSpecificAction(PossibleAction action) {

        if (action instanceof FoldIntoPrussian) {

            FoldIntoPrussian a = (FoldIntoPrussian) action;

            if (step == Step.START) {
                if (!startPrussian(a)) {
                    finishRound();
                } else {
                    step = Step.MERGE;
                    findNextMergingPlayer(false);
                }

            } else if (step == Step.MERGE) {

                mergeIntoPrussian (a);

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
                    if (prussian.getNumberOfTrains() > prussian.getCurrentTrainLimit()) {
                        step = Step.DISCARD_TRAINS;
                    } else {
                        finishRound();
                    }
                    return false;
                }
            }

            setFoldablePrePrussians();
            if (!foldablePrePrussians.isEmpty()) return true;
            skipCurrentPlayer = true;
        }
    }

    private boolean startPrussian (FoldIntoPrussian action) {

        // Validate
        String errMsg = null;

        List<Company> folded = action.getFoldedCompanies();
        boolean folding = folded != null && !folded.isEmpty();

        while (folding) {
            if (!(M2_ID.equals(action.getFoldedCompanyNames()))) {
                errMsg = LocalText.getText("WrongCompany",
                        action.getFoldedCompanyNames(),
                        GameManager_1835.M2_ID);
                break;
            }
            break;
        }

        if (errMsg != null) {
            DisplayBuffer.add(this, LocalText.getText("CannotMerge",
                    action.getFoldedCompanyNames(),
                    PR_ID,
                    errMsg));
            return false;
        }

        // all actions linked during formation round to avoid serious undo problems
        
        // FIXME: changeStack.linkToPreviousMoveSet();

        if (folding) executeStartPrussian(false);

        return folding;
    }

    private void executeStartPrussian (boolean display) {

        prussian.start();
        String message = LocalText.getText("START_MERGED_COMPANY",
                PR_ID,
                Bank.format(this, prussian.getIPOPrice()),
                prussian.getStartSpace().toText());
        ReportBuffer.add(this, message);
        if (display) DisplayBuffer.add(this, message);

        // add money from sold shares
        // Move cash and shares where required
        int capFactor = prussian.getSoldPercentage() / (prussian.getShareUnit() * prussian.getShareUnitsForSharePrice());
        int cash = capFactor * prussian.getIPOPrice();

        if (cash > 0) {
            String cashText = Currency.fromBank(cash, prussian);
            ReportBuffer.add(this, LocalText.getText("FloatsWithCash",
                prussian.getId(),
                cashText ));
        } else {
            ReportBuffer.add(this, LocalText.getText("Floats",
                    prussian.getId()));
        }

        executeExchange (Arrays.asList(new Company[]{m2}), true, false);
        prussian.setFloated();
    }

    private boolean mergeIntoPrussian (FoldIntoPrussian action) {

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
            cert = unavailable.findCertificate(prussian, efs.getShare()/prussian.getShareUnit(),
                    president);
            cert.moveTo(player);
            //company.setClosed();
            String message = LocalText.getText("MERGE_MINOR_LOG",
                    player.getId(),
                    company.getId(),
                    PR_ID,
                    company instanceof PrivateCompany ? "no"
                            : Bank.format(this, ((PublicCompany)company).getCash()),
                    company instanceof PrivateCompany ? "no"
                            : ((PublicCompany)company).getPortfolioModel().getTrainList().size());
            ReportBuffer.add(this, message);
            if (display) DisplayBuffer.add(this, message);
            message = LocalText.getText("GetShareForMinor",
                    player.getId(),
                    cert.getShare(),
                    PR_ID,
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
                if (!hex.hasTokenOfCompany(prussian) && hex.layBaseToken(prussian, city)) {
                    /* TODO: the false return value must be impossible. */
                    message = LocalText.getText("ExchangesBaseToken",
                            PR_ID, minor.getId(),
                            city.getSpecificId());
                    ReportBuffer.add(this, message);
                    if (display) DisplayBuffer.add(this, message);

                    prussian.layBaseToken(hex, 0);
                }

                // Move any cash
                if (minor.getCash() > 0) {
                    Currency.wireAll(minor, prussian);
                }

                // Move any trains
                // TODO: Simplify code due to trainlist being immutable anyway
                List<Train> trains = new ArrayList<Train> (minor.getPortfolioModel().getTrainList());
                for (Train train : trains) {
                    prussian.getPortfolioModel().addTrain(train);
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
            if (company != prussian) {
                errMsg = LocalText.getText("WrongCompany", company.getId(), prussian.getId());
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
        
        train.discard();
        
        // We still might have another excess train
        // TODO: would be better to have DiscardTrain discard multiple trains
        if (prussian.getNumberOfTrains() > prussian.getCurrentTrainLimit()) {
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
            ReportBuffer.add(this, LocalText.getText("EndOfFormationRound", PR_ID,
                    interruptedRound.getRoundName()));
        } else {
            ReportBuffer.add(this, LocalText.getText("EndOfFormationRoundNoInterrupt", PR_ID));
        }

        if (prussian.hasStarted()) prussian.checkPresidency();
        prussian.setOperated(); // To allow immediate share selling
        //        super.finishRound();
        // Inform GameManager
        gameManager.nextRound(this);
    }

    public static boolean prussianIsComplete(GameManager gameManager) {

        for (PublicCompany company : gameManager.getAllPublicCompanies()) {
            if (!company.getType().getId().equalsIgnoreCase("Minor")) continue;
            if (!company.isClosed()) return false;
        }
        return true;
    }

    @Override
    public String toString() {
        return "1835 PrussianFormationRound";
    }

}
