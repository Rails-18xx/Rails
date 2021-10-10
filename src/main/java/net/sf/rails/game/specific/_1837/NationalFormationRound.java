package net.sf.rails.game.specific._1837;

import java.util.ArrayList;
import java.util.List;

import net.sf.rails.game.*;
import net.sf.rails.game.financial.Bank;
import net.sf.rails.game.state.ArrayListMultimapState;
import net.sf.rails.game.state.ArrayListState;
import net.sf.rails.game.state.GenericState;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import rails.game.action.DiscardTrain;
import rails.game.action.FoldIntoNational;
import rails.game.action.PossibleAction;

import net.sf.rails.common.DisplayBuffer;
import net.sf.rails.common.LocalText;
import net.sf.rails.common.ReportBuffer;
import net.sf.rails.game.round.RoundFacade;

public class NationalFormationRound extends StockRound_1837 {
    private static final Logger log = LoggerFactory.getLogger(NationalFormationRound.class);

    public NationalFormationRound(GameManager parent, String id) {
        super(parent, id);
        // TODO Auto-generated constructor stub
    }

    private PublicCompany_1837 national;
    private List<PublicCompany_1837> minors;
    private PublicCompany_1837 startingMinor;
    private boolean startNational;
    private boolean forcedStart;
    private boolean mergeNational;
    private boolean forcedMerge;

    private String nfrReportName;
    private boolean atNewPhase;

    private ArrayListState<Player> currentPlayerOrder; // To exchange minors
    private ArrayListMultimapState<Player, Company> minorsPerPlayer;
    private ArrayListState<PublicCompany> closedMinors;

    protected enum Step {
            START,
            MERGE,
            DISCARD_TRAINS
        }

    private GenericState<Step> step;

    public static boolean nationalIsComplete(PublicCompany_1837 national) {

        for (PublicCompany company : national.getMinors()) {
            if (!company.isClosed()) return false;
        }
        return true;
    }


    public void start(PublicCompany_1837 national, boolean atNewPhase, String nfrReportName) {
        this.national = national;
        this.atNewPhase = atNewPhase;
        this.nfrReportName = nfrReportName;
        PhaseManager phaseManager = getRoot().getPhaseManager();
        startNational = phaseManager.hasReachedPhase(national.getFormationStartPhase());
        forcedStart = phaseManager.hasReachedPhase(national.getForcedStartPhase());
        forcedMerge = phaseManager.hasReachedPhase(national.getForcedMergePhase());

        minors = national.getMinors();
        startingMinor = national.getStartingMinor();
        currentPlayerOrder = new ArrayListState<>(this, "PlayerOrder_"+getId());
        minorsPerPlayer = ArrayListMultimapState.create(this, "MinorsPerPlayer_"+getId());
        closedMinors = new ArrayListState<>(this, "ClosedMinorsPerMajor_"+getId());
        step = new GenericState<>(this, getId()+"_step",
                (national.hasStarted() ? Step.MERGE : Step.START));

        for (PublicCompany_1837 minor : minors) {
            if (!minor.isClosed()) minorsPerPlayer.put (minor.getPresident(), minor);
        }

        Player startingPlayer;
        if (national.hasStarted()) {
            startingPlayer = national.getPresident();
        } else {
            startingPlayer = startingMinor.getPresident();
        }

        currentPlayerOrder.clear();
        for (Player player : playerManager.getNextPlayersAfter(
                startingPlayer, true, false)) {
            for (PublicCompany_1837 minor : minors) {
                if (!minor.isClosed() && player == minor.getPresident()) {
                    currentPlayerOrder.add(player);
                    // Once in the list is enough
                    break;
                }
            }
        }

        ReportBuffer.add(this, LocalText.getText("StartFormationRound", national.getId(), nfrReportName));

        start();
    }

    @Override
    public void start() {
        startNational = !national.hasStarted();
        mergeNational = !nationalIsComplete(national);

        log.debug("StartNational={} forcedStart={} mergeNational={} forcedMerge={}", startNational, forcedStart, mergeNational, forcedMerge);

        //step.set (startNational ? Step.START : Step.MERGE);

        if (step.value() == Step.START) {

            setCurrentPlayer(startingMinor.getPresident());
            //gameManager.setNationalFormationStartingPlayer( national, currentPlayer);
            if (forcedStart) {
                executeStartNational(true);

                // The starting minor president becomes the initial major president
                national.setPresident(currentPlayer);
                ReportBuffer.add(this, LocalText.getText("IS_NOW_PRES_OF",
                        currentPlayer.getId(),
                        national.getId()));

                startNational = false;
                step.set (Step.MERGE);
            }
        }

        if (step.value() == Step.MERGE) {
            if (forcedMerge) {  // TODO Below code to be replaced
                 for (PublicCompany_1837 minor : minors) {
                    if (!minor.isClosed()) {
                        mergeCompanies(minor, national,
                                minor == startingMinor,
                                forcedMerge);
                        // Replace the home token
                        exchangeMinorToken (minor);
                    }
                }
                national.checkPresidency();

            /*
            Set<SpecialProperty> sps;
            //setFoldablePreNationals(national.getAlias());
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
            //executeExchange (foldables, false, true);  ????
            `*/
                // Check if the National must discard any trains
                if (national.getNumberOfTrains() > national.getCurrentTrainLimit()) {
                    step.set(Step.DISCARD_TRAINS);
                } else {
                    finishRound();
                }
            } else {
                findNextMergingPlayer(false);
            }
        }
    }

    /*
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
    }*/

    @Override
    public boolean setPossibleActions() {

        if (step.value() == Step.START) {
            startingPlayer = startingMinor.getPresident();
            setCurrentPlayer(startingPlayer); // Duplicate
            ReportBuffer.add(this, LocalText.getText("StartingPlayer",
                    startingPlayer.getId()
                    ));

            possibleActions.add(new FoldIntoNational(national, startingMinor));

        } else if (step.value() == Step.MERGE) {

            List<Company> minors = minorsPerPlayer.get (currentPlayer);
            possibleActions.add(new FoldIntoNational(national, minors));

        } else if (step.value() == Step.DISCARD_TRAINS) {

            if (national.getNumberOfTrains() > national.getCurrentTrainLimit()) {
                log.debug("+++ National {} has {}, limit is {}", national.getLongName(), national.getNumberOfTrains(), national.getCurrentTrainLimit());
                possibleActions.add(new DiscardTrain(national,
                        national.getPortfolioModel().getUniqueTrains(), true));
            }
        }
        return true;

    }


    @Override
    protected boolean processGameSpecificAction(PossibleAction action) {

        if (action instanceof FoldIntoNational) {

            foldIntoNational((FoldIntoNational) action);

            /*
            FoldIntoNational a = (FoldIntoNational) action;

            if (step.value() == Step.START) {
                if (!startNational(a)) {
                    finishRound();
                } else {
                    step.set (Step.MERGE);
                    findNextMergingPlayer(false);
                }

            } else if (step.value() == Step.MERGE) {

                mergeIntoNational (a);

            }*/

            return true;

        } else if (action instanceof DiscardTrain) {

            discardTrain ((DiscardTrain) action);
            return true;

        } else {
            return false;
        }
    }

    protected boolean findNextMergingPlayer(boolean skipCurrentPlayer) {

        /*
        while (true) {

            if (skipCurrentPlayer) {
                setNextPlayer();
                if (playerManager.getCurrentPlayer() == startingPlayer) {
                    if (national.getNumberOfTrains() > national.getCurrentTrainLimit()) {
                        step = Step.DISCARD_TRAINS;
                    } else {
                        finishRound();
                    }
                    return false;
                }
            }

            setFoldablePreNationals(national.getAlias());
            if (!foldablePreNationals.isEmpty()) return true;
            skipCurrentPlayer = true;
        }
         */
        if (currentPlayerOrder.isEmpty()) {
            return false;
        } else {
            // Find the next player to act with the current major
            Player nextPlayer = currentPlayerOrder.get(0);
            setCurrentPlayer(nextPlayer);
            return true;
        }

    }

    private boolean foldIntoNational(FoldIntoNational action) {

        // Validate
        String errMsg = null;

        List<PublicCompany_1837> folded = new ArrayList<>();
        if (action.getFoldedCompanies() != null) {
            for (Company comp : action.getFoldedCompanies()) {
                folded.add ((PublicCompany_1837) comp);
            }
        }
        boolean folding = !folded.isEmpty();
        boolean toStart = !national.hasStarted();

        if (!folding) {
            ReportBuffer.add (this, LocalText.getText("NoMerge",
                    currentPlayer.getId(), action.getFoldableCompanyNames(), national));
            currentPlayerOrder.remove(currentPlayer);

            // Does not want to start, or if started, does not want to merge
            if (toStart
                    // If already started, is there another player to merge?
                    || !findNextMergingPlayer(true)) {
                if (national.hasStarted()) {
                    national.checkPresidency();
                }
                finishRound();
            }
            return true;
        }

        boolean starting = folded.contains(startingMinor);

CHECK:  while (true) {

            if (!starting && !national.hasStarted()) {
                errMsg = LocalText.getText("NotYetStarted",
                        national.getId());
                break;
            }

            for (Company comp : folded) {
                if (!minors.contains(comp)) {
                    errMsg = LocalText.getText("WrongCompany", comp.getId(),
                            action.getFoldableCompanyNames());
                    break CHECK;
                }
            }

            if (starting && !toStart) {
                errMsg = LocalText.getText("CompanyAlreadyStarted", national);
                break;
            } else if (!starting && toStart) {
                errMsg = LocalText.getText("WrongCompany",
                        action.getFoldedCompanyNames(),
                        startingMinor.getId());
                break;
            }
            break;
        }

        if (errMsg != null) {
            DisplayBuffer.add(this, LocalText.getText("CannotMerge",
                    action.getFoldedCompanyNames(),
                    national.getId(),
                    errMsg));
            return false;
        }

        // all actions linked during formation round to avoid serious undo problems

        // FIXME: changeStack.linkToPreviousMoveSet();

        if (folding) {
            if (starting) {
                executeStartNational(false);
                step.set(Step.MERGE);
            }
            executeMergeMinors(folded);
        }

        // Remove a current player who has no more minors to check
        if (minorsPerPlayer.get(currentPlayer).isEmpty()) {
            currentPlayerOrder.remove (currentPlayer);
            if (!findNextMergingPlayer(true)) {
                if (national.hasStarted()) {
                    national.checkPresidency();
                }
                finishRound();
            }
        }

        return folding;
    }

    private void executeStartNational(boolean display) {

        national.start();
        String message = LocalText.getText("START_MERGED_COMPANY",
                national.getId(),
                Bank.format(this, national.getIPOPrice()),
                national.getStartSpace());
        ReportBuffer.add(this, message);
        if (display) DisplayBuffer.add(this, message);

        floatCompany(national);
    }

        // add money from sold shares
        // Move cash and shares where required
        /* THIS IS DONE BELOW BY floatCompany()
        int capFactor = national.getSoldPercentage() / (national.getShareUnit() * national.getShareUnitsForSharePrice());
        int cash = capFactor * national.getIPOPrice();

        if (cash > 0) {
            String cashText = Currency.fromBank(cash, national);
            ReportBuffer.add(this, LocalText.getText("FloatsWithCash",
                    national.getId(),
                cashText ));
        } else {
            ReportBuffer.add(this, LocalText.getText("Floats",
                    national.getId()));
        }
        */
        //executeExchange (Arrays.asList(new Company[]{nationalStartingMinor}), true, false);
        //((GameManager_1837)gameManager).mergeCompanies(startingMinor, national,
        //        true, false);

    private void executeMergeMinors (List<PublicCompany_1837> minorsToMerge) {

        for (PublicCompany_1837 minor : minorsToMerge) {

            mergeCompanies(minor, national,
                    minor == startingMinor, false);

            // Replace the home token
            exchangeMinorToken (minor);

            closedMinors.add (minor);
            // Remove minor
            minorsPerPlayer.remove(currentPlayer, minor);
        }

        if (atNewPhase) national.setOperated();
    }

    private void exchangeMinorToken (PublicCompany_1837 minor) {

        MapHex hex = minor.getHomeHexes().get(0);
        if (hex.isOpen()) {  // 1837 S5 Italian home hex has already been closed here
            Stop city = hex.getRelatedStop(minor.getHomeCityNumber());
            if (!city.hasTokenOf(national) && hex.layBaseToken(national, city)) {
                /* TODO: the false return value must be impossible. */
                String message = LocalText.getText("ExchangesBaseToken2",
                        national.getId(), minor.getId(),
                        hex.getId() +
                                (hex.getStops().size() > 1
                                ? "/" + hex.getConnectionString(city.getRelatedStation())
                                : "")
                        );
                ReportBuffer.add(this, message);
                national.layBaseToken(hex, 0);
            } else {
                log.error("Cannot lay {} token on {} home {}", national, minor, hex);
            }
        }

    }

    /**
     * Merge a minor with its related national company.
     * @param action A MergeCompanies action selected by the minor owner.
     * @return True if the merge is successful, and new possible action(s) can be selected.
     */
    /* NOT USED
    public boolean executeMerge (MergeCompanies action) {

        PublicCompany minor = action.getMergingCompany();
        PublicCompany major = action.getSelectedTargetCompany();

        boolean result = mergeCompanies(minor, major);
        closedMinors.add (minor);
        minorsPerPlayer.remove (currentPlayer, minor);

        MapHex minorHome = minor.getHomeHexes().get(0);
        Stop minorStop = minorHome.getStopOfBaseToken(minor);
        minorHome.layBaseToken(major, minorStop);
        major.layBaseToken(minorHome, 0);

        // TODO: to be moved outside this method
        if (result) {
            minorsPerPlayer.remove(currentPlayer, minor);
            if (minorsPerPlayer.get(currentPlayer).isEmpty()) {
                step.set(Step.DISCARD_TRAINS);
            }
        }
        return result;
    }*/


    /**
     * Merge a minor into a national that has already started.
     * @param action
     * @return True if one or more companies are to be folded
     */

    /* No longer used
    private boolean mergeIntoNational(FoldIntoNational action) {

        // Validate
        String errMsg = null;

        List<Company> folded = action.getFoldedCompanies();
        boolean folding = folded != null && !folded.isEmpty();
        PublicCompany_1837 national = action.getNationalCompany();
        List<PublicCompany_1837> minors = national.getMinors();

CHECK:  while (folding) {

            if (!national.hasStarted()) {
                errMsg = LocalText.getText("NotYetStarted",
                        national.getId());
                break;
            }
            for (Company comp : folded) {
                if (!minors.contains(comp) || comp == national.getStartingMinor()) {
                    errMsg = LocalText.getText("WrongCompany", comp.getId(),
                            action.getFoldableCompanyNames());
                    break CHECK;

                }
            }
            break;
        }

        if (errMsg != null) {
            DisplayBuffer.add(this, LocalText.getText("CannotMerge",
                    action.getFoldedCompanyNames(),
                    national.getId(),
                    errMsg));
            return false;
        }


        // all actions linked during formation round to avoid serious undo problems

        // FIMXE: changeStack.linkToPreviousMoveSet();

        // Execute
        //if (folding) executeExchange (folded, false, false);
        if (folding) {
            for (Company comp : folded) {
                if (comp instanceof PublicCompany) {
                    PublicCompany minor = (PublicCompany) comp;
                    mergeCompanies((PublicCompany) comp, national); // Also closes minor

                    // Replace the home token
                    MapHex hex = minor.getHomeHexes().get(0);
                    Stop city = hex.getRelatedStop(minor.getHomeCityNumber());
                    if (hex.layBaseToken(national, city)) {
                        /* TODO: the false return value must be impossible. *//*
                        String message = LocalText.getText("ExchangesBaseToken",
                                national.getId(), minor.getId(),
                                hex.getId(), city.getRelatedStationNumber());
                        ReportBuffer.add(this, message);
                        //if (display) DisplayBuffer.add(this, message);

                        national.layBaseToken(hex, 0);
                    }

                } else {
                    // Private companies to be dealt with if/when
                    // 1835 is going to be handled by this code
                }
            }
        } else {
            ReportBuffer.add (this, LocalText.getText("NoMerge",
                    currentPlayer.getId(), action.getFoldableCompanyNames(), national));
        }

        currentPlayerOrder.remove(currentPlayer);
        if (currentPlayerOrder.isEmpty()) {
            finishRound();
        } else {
            findNextMergingPlayer(true);
        }

        return folding;
    }*/

    /*
    private void executeExchange(List<Company> companies, boolean president, boolean display) {

        ExchangeForShare efs;
        PublicCertificate cert;
        Player player;
        for (Company company : companies) {
            log.debug("Merging company {}", company.getId());
            if (company instanceof PrivateCompany) {
                player = (Player)((PrivateCompany)company).getOwner();
            } else {
                player = ((PublicCompany)company).getPresident();
            }
            // Shortcut, sp should be checked
            efs = (ExchangeForShare) Iterables.get(company.getSpecialProperties(), 0);
            cert = unavailable.findCertificate(national, efs.getShare()/national.getShareUnit(),
                    president);
            cert.moveTo(player);
            //company.setClosed();
            String message = LocalText.getText("MERGE_MINOR_LOG",
                    player.getId(),
                    company.getId(),
                    national.getId(),
                    company instanceof PrivateCompany ? "no"
                            : Bank.format(this, ((PublicCompany)company).getCash()),
                    company instanceof PrivateCompany ? "no"
                            : ((PublicCompany)company).getPortfolioModel().getTrainList().size());
            ReportBuffer.add(this, message);
            if (display) DisplayBuffer.add(this, message);
            message = LocalText.getText("GetShareForMinor",
                    player.getId(),
                    cert.getShare(),
                    national.getId(),
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
                if (hex.layBaseToken(national, city)) {
                    // TODO: the false return value must be impossible.
                    message = LocalText.getText("ExchangesBaseToken",
                            national.getId(), minor.getId(),
                            city.getRelatedStationNumber());
                    ReportBuffer.add(this, message);
                    if (display) DisplayBuffer.add(this, message);

                    national.layBaseToken(hex, 0);
                }

                // Move any cash
                if (minor.getCash() > 0) {
                    Currency.wireAll(minor, national);
                }

                // Move any trains
                // TODO: Simplify code due to trainlist being immutable anyway
                List<Train> trains = new ArrayList<Train> (minor.getPortfolioModel().getTrainList());
                for (Train train : trains) {
                    national.getPortfolioModel().addTrain(train);
                }
            }

            // Close the merged companies
            company.setClosed();
        }

    }*/

    public boolean discardTrain(DiscardTrain action) {

        Train train = action.getDiscardedTrain();
        PublicCompany company = action.getCompany();

        String errMsg = null;

        // Dummy loop to enable a quick jump out.
        while (true) {
            // Checks
            // Must be correct step
            if (company != national) {
                errMsg = LocalText.getText("WrongCompany", company.getId(), national.getId());
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
        if (national.getNumberOfTrains() > national.getCurrentTrainLimit()) {
            step.set (Step.DISCARD_TRAINS);
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
            ReportBuffer.add(this, LocalText.getText("EndOfFormationRound",
                    national.getId(), interruptedRound.getRoundName()));
        } else {
            ReportBuffer.add(this, LocalText.getText("EndOfFormationRoundNoInterrupt",
                    national.getId(), nfrReportName));
        }

        if (national.hasStarted()) national.checkPresidency();
        national.setOperated(); // FIXME: only if anything has been merged! And not between rounds!
        //        super.finishRound();
        // Inform GameManager

        gameManager.nextRound(this);
    }

    public PublicCompany_1837 getNational() {
        return national;
    }

    @Override
    public String toString() {
        return getId();
    }

}
