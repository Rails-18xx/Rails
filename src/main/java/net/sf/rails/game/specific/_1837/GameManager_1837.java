package net.sf.rails.game.specific._1837;

import net.sf.rails.common.GameOption;
import net.sf.rails.game.*;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import net.sf.rails.common.GuiDef;
import net.sf.rails.game.financial.NationalFormationRound;
import net.sf.rails.game.state.GenericState;
import net.sf.rails.game.state.IntegerState;
import net.sf.rails.game.state.BooleanState;

import java.util.List;


/**
 * @author martin
 *
 */
public class GameManager_1837 extends GameManager {

    private static final Logger log = LoggerFactory.getLogger(GameManager_1837.class);

    protected final IntegerState cerNumber = IntegerState.create(this, "cerNumber");


    private Round previousRound = null;
    protected final GenericState<Player> playerToStartFCERound =
            new GenericState<>(this, "playerToStartFCERound");

    protected final GenericState<Player> playerToStartCERound =
            new GenericState<>(this, "playerToStartCERound");

    protected final BooleanState CoalRoundFollowedByOR =
            new BooleanState(this, "CoalRoundFollowedByOr");

    protected final BooleanState buyOnly =
            new BooleanState(this, "buyOnly", false);

    public GameManager_1837(RailsRoot parent, String id) {
        super(parent, id);

    }


    public void nextRound(Round round) {
        if (round instanceof StartRound) {
            // In version 2, any subsequent start round will be buy-only (no bidding).
            buyOnly.set(true);
            if (((StartRound) round).getStartPacket().areAllSold()) { // This start round was "completed"
                // check if there are other StartPackets, otherwise stockRounds start
                beginStartRound();
            } else {
                startOperatingRound(runIfStartPacketIsNotCompletelySold());
            }
        } else if (round instanceof CoalExchangeRound) {
            //Since the CoalExchangeRound can happen after both types of rounds we need to move the
            //round decision down to this class and cant call the superclass :(

            if (this.CoalRoundFollowedByOR.value()) {
                Phase currentPhase = getRoot().getPhaseManager().getCurrentPhase();
                if (currentPhase == null) log.error("Current Phase is null??", new Exception(""));
                numOfORs.set(currentPhase.getNumberOfOperatingRounds());
                log.info("Phase={} ORs={}", currentPhase.toText(), numOfORs);

                // Create a new OperatingRound (never more than one Stock Round)
                // OperatingRound.resetRelativeORNumber();
                relativeORNumber.set(1);
                startOperatingRound(true);
            } else {
                if (relativeORNumber.add(1) <= numOfORs.value()) {
                    // There will be another OR
                    startOperatingRound(true);
                } else {
                    if (gameOverPending.value()
                            && gameEndWhen == GameEnd.AFTER_SET_OF_ORS) {
                        finishGame();
                    } else {
                        // FIXME: This isn't a valid cast...
                        ((OperatingRound) round).checkForeignSales();
                        startStockRound();
                    }
                }

            }

            //super.nextRound(round);
            getCurrentRound().setPossibleActions();
        } else if ((round instanceof StockRound_1837) || (round instanceof OperatingRound_1837)) {
            // If any mergeable coal companies exist, start a CoalExchangeRound (CER)
            List<Company> possibleMergers =
                    getRoot().getCompanyManager().getCompaniesByType("Coal");
            boolean runCER = false;
            for (Company coalComp : possibleMergers) {
                if (!coalComp.isClosed()
                        && ((PublicCompany) coalComp).getRelatedPublicCompany().hasFloated()) {
                    runCER = true;
                    break;
                }
            }
            if (runCER) {
                if (round instanceof StockRound_1837) CoalRoundFollowedByOR.set(true);
                cerNumber.add(1);
                createRound(CoalExchangeRound.class, "CoalExchangeRound " + cerNumber.value())
                        .start(cerNumber.value());
            } else {
                super.nextRound(round);
            }
        } else if (round instanceof NationalFormationRound) {
            if (interruptedRound != null) {
                setRound(interruptedRound);
                interruptedRound.resume();
                interruptedRound = null;
            } else if (previousRound != null) {
                super.nextRound(previousRound);
                previousRound = null;
            }
        } else {
            Phase phase = getCurrentPhase();
            if ((phase.getId().equals("4E") || phase.getId().equals("5"))
                    && (!NationalFormationRound.nationalIsComplete((this), "Ug"))) {
                previousRound = round;
                startHungaryFormationRound(null);
            } else if ((phase.getId().equals("4"))
                    && (!NationalFormationRound.nationalIsComplete((this), "Sd"))) {
                previousRound = round;
                startSuedBahnFormationRound(null);
            } else if (((phase.getId().equals("4")) || (phase.getId().equals("4E")) ||
                    (phase.getId().equals("4+1")))
                    && (!NationalFormationRound.nationalIsComplete((this), "KK"))) {
                previousRound = round;
                startKuKFormationRound(null);
            } else {
                super.nextRound(round);
            }
        }

    }

    /* (non-Javadoc)
     * @see net.sf.rails.game.GameManager#runIfStartPacketIsNotCompletelySold()
     */
    @Override
    protected boolean runIfStartPacketIsNotCompletelySold() {
        //After the first Startpacket sold out there will be Operation Rounds
        StartPacket nextStartPacket = getRoot().getCompanyManager().getNextUnfinishedStartPacket();
        if (nextStartPacket.getId().equalsIgnoreCase("Coal Mines")) {
            return false;
        } else {
            return true;
        }
    }

    @Override
    protected void createStartRound(StartPacket startPacket) {
        String startRoundClassName = startPacket.getRoundClassName();
        startRoundNumber.add(1);
        String variant = GameOption.getValue(this, GameOption.VARIANT);
        if (variant.equalsIgnoreCase("1837-2ndEd.")
                && buyOnly.value()) {
            // For subsequent start rounds, we need the buy-only version.
            startRoundClassName += "_buying";
        }
        StartRound startRound = createRound(startRoundClassName,
                "startRound_" + startRoundNumber.value());
        startRound.start();
    }


    public void startHungaryFormationRound(OperatingRound_1837 or) {
        interruptedRound = or;
        String roundName;
        if (interruptedRound == null) {
            // after a round
            roundName = "HungaryFormationRound_after_" + previousRound.getId();
        } else {
            roundName = "HungaryFormationRound_in_" + or.getId();
        }
        this.setNationalToFound("Ug");
        createRound(NationalFormationRound.class, roundName).start();
    }

    public void startSuedBahnFormationRound(OperatingRound_1837 or) {
        interruptedRound = or;
        String roundName;
        if (interruptedRound == null) {
            // after a round
            roundName = "SuedBahnFormationRound_after_" + previousRound.getId();
        } else {
            roundName = "SuedBahnFormationRound_in_" + or.getId();
        }
        this.setNationalToFound("Sd");
        createRound(NationalFormationRound.class, roundName).start();
    }

    public void startKuKFormationRound(OperatingRound_1837 or) {
        interruptedRound = or;
        String roundName;
        if (interruptedRound == null) {
            // after a round
            roundName = "KuKFormationRound_after_" + previousRound.getId();
        } else {
            roundName = "KuKFormationRound_in_" + or.getId();
        }
        this.setNationalToFound("KK");
        createRound(NationalFormationRound.class, roundName).start();
    }

    /* (non-Javadoc)
     * @see net.sf.rails.game.GameManager#setGuiParameter(net.sf.rails.common.GuiDef.Parm, boolean)
     */
    @Override
    public void setGuiParameters() {
        super.setGuiParameters();
        //Flags the Game that a special Company income is needed...
        guiParameters.put(GuiDef.Parm.HAS_SPECIAL_COMPANY_INCOME, true);

    }


    public Player getPlayerToStartFCERound() {
        return playerToStartFCERound.value();
    }


    public void setPlayerToStartFCERound(Player president) {
        this.playerToStartFCERound.set(president);
    }

    public Player getPlayerToStartCERound() {
        return playerToStartCERound.value();
    }


    public void setPlayerToStartCERound(Player president) {
        this.playerToStartCERound.set(president);
    }

    public int getCERNumber() {
        return cerNumber.value();
    }

    public void setCoalRoundFollowedByOR(boolean b) {
        this.CoalRoundFollowedByOR.set(b);
    }

    public boolean isBuyOnly() {
        return buyOnly.value();
    }

}
