package net.sf.rails.game.specific._1856;

import com.google.common.collect.Iterables;
import net.sf.rails.common.DisplayBuffer;
import net.sf.rails.common.LocalText;
import net.sf.rails.common.ReportBuffer;
import net.sf.rails.game.*;
import net.sf.rails.game.state.BooleanState;

import java.util.List;


public class GameManager_1856 extends GameManager {

    private Player playerToStartCGRFRound;

    private static final int[][] certLimitsTable = {
            {14, 19, 21, 26, 29, 31, 36, 40},
            {10, 13, 15, 18, 20, 22, 25, 28},
            {8, 10, 12, 14, 16, 18, 20, 22},
            {7, 8, 10, 11, 13, 15, 16, 18},
            {6, 7, 8, 10, 11, 12, 14, 15}
    };

    private final BooleanState cgrFormationPassed = new BooleanState(this, "CgrFormationPassed");

    public GameManager_1856(RailsRoot parent, String id) {
        super(parent, id);
    }

    public int getNextTrainNumberFromIpo() {
        Train nextAvailableTrain
                = Iterables.get(getRoot().getTrainManager().getAvailableNewTrains(), 0);
        int trainNumber;
        try {
            trainNumber = Integer.parseInt(nextAvailableTrain.toText());
            // Added for Issue #309: 8-train in variant
            trainNumber = Math.min (trainNumber, 6);
        } catch (NumberFormatException e) {
            trainNumber = 6; // Diesel!
        }
        return trainNumber;
    }

    public void startCGRFormationRound(OperatingRound_1856 or, Player playerToStartCGRFRound) {
        this.playerToStartCGRFRound = playerToStartCGRFRound;
        this.interruptedRound = or;

        if (this.playerToStartCGRFRound != null) {
            // TODO: this id will not work
            createRound(CGRFormationRound.class, "CGRFormationRound").start(this.playerToStartCGRFRound);
            this.playerToStartCGRFRound = null;
        } else {
            resetCertificateLimit(true);
        }
    }

    @Override
    public void nextRound(Round round) {
        if (round instanceof CGRFormationRound) {
            setRound(interruptedRound);
            ((OperatingRound_1856) interruptedRound).resume(((CGRFormationRound) round).getMergingCompanies());
        } else {
            super.nextRound(round);
        }

    }

    /* Must be called each time AFTER companies have closed from train phase 6 */
    public void resetCertificateLimit(boolean atEndOfCGRFormation) {

        List<PublicCompany> availableCompanies;

        if (atEndOfCGRFormation) cgrFormationPassed.set(true);
        if (!cgrFormationPassed.value()) return;

        // Determine the new certificate limit.
        // Make sure that only available Companies are counted.
        availableCompanies = getAllPublicCompanies();
        int validCompanies = availableCompanies.size(); //12, including the CGR

        for (PublicCompany c : availableCompanies) {
            if (c.getLongName().equals(PublicCompany_CGR.NAME)) {
                // If the CGR has not formed, it does not count as available
                if (!c.hasStarted()) validCompanies--;
            } else {
                // If a company has been closed before, during or after CGR formation, it does not count
                if (c.isClosed()) validCompanies--;
            }
        }
        // Some checks to be sure
        int numCompanies = Math.min(11, Math.max(4, validCompanies));
        int numPlayers = getRoot().getPlayerManager().getNumberOfPlayers();

        int newCertLimit = certLimitsTable[numPlayers - 2][numCompanies - 4];
        getRoot().getPlayerManager().setPlayerCertificateLimit(newCertLimit);
        String message = LocalText.getText("CertificateLimit",
                newCertLimit,
                numPlayers,
                numCompanies);
        DisplayBuffer.add(this, message);
        ReportBuffer.add(this, message);

    }

}
