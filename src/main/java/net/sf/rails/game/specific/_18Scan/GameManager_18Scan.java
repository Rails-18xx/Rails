package net.sf.rails.game.specific._18Scan;

import net.sf.rails.common.LocalText;
import net.sf.rails.common.ReportBuffer;
import net.sf.rails.game.*;

import java.util.List;

/**
 * This class is needed because we must have two ORs if all players pass in the Initial Stock Round.
 * Meddling with GameManager resulted in breaking tests of other games, so we need a subclass.
 */
public class GameManager_18Scan extends GameManager {

    // Not sure if we need both of these
    private PublicCompany destinationCompany;
    private List<PublicCompany> destinationCompanies;

    public GameManager_18Scan (RailsRoot parent, String id) {
        super(parent, id);
    }

    // A kludge to make sure that the number of "Short OR"s is always 2.
    // The regular GameManager seems broken on the aspect of numbering and counting
    // short ORs. This can be fixed, but then saved test games won't pass because of
    // OR numbering differences.
    //
    // This fix relies on the conditions: (1) that this method is only called at the start
    // of the first of any sequence of short ORs, i.e. when a StartRound precedes a short OR,
    // and (2) that the argument for startOperatingRound in all other cases is true.
    protected boolean runIfStartPacketIsNotCompletelySold() {
        relativeORNumber.set(1);
        numOfORs.set(2);
        return true;
    }

    public void StartDestinationRuns (OperatingRound or, List<PublicCompany> companies) {

        setInterruptedRound(or);
        this.destinationCompanies = companies;
        startDestinationRun();
    }

    private void startDestinationRun () {
        for (PublicCompany company : destinationCompanies) {
            destinationCompany = company;
            createRound(DestinationRound_18Scan.class, "DestRun_" + company.getId())
                    .start(destinationCompany);
            break; // One at a time.
        }
    }

    @Override
    public void nextRound(Round round) {
        if (round instanceof DestinationRound_18Scan) {
            destinationCompanies.remove(destinationCompany);
            if (destinationCompanies.isEmpty()) {
                OperatingRound_18Scan interruptedRound = (OperatingRound_18Scan) getInterruptedRound();
                setRound(interruptedRound);
                interruptedRound.resume();
            } else {
                startDestinationRun();
            }
        } else {
            super.nextRound(round);
        }
    }

}
