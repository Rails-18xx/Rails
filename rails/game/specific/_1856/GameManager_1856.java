/* $Header: /Users/blentz/rails_rcs/cvs/18xx/rails/game/specific/_1856/GameManager_1856.java,v 1.2 2009/09/02 21:47:47 evos Exp $ */
package rails.game.specific._1856;

import java.util.List;

import rails.common.DisplayBuffer;
import rails.common.LocalText;
import rails.game.*;
import rails.game.state.BooleanState;

public class GameManager_1856 extends GameManager {

    private static int[][] certLimitsTable = {
            {14, 19, 21, 26, 29, 31, 36, 40},
            {10, 13, 15, 18, 20, 22, 25, 28},
            {8, 10, 12, 14, 16, 18, 20, 22},
            {7, 8, 10, 11, 13, 15, 16, 18},
            {6, 7, 8, 10, 11, 12, 14, 15}
    };

    private Player playerToStartCGRFRound = null;
    private BooleanState cgrFormationPassed = new BooleanState ("CgrFormationPassed", false);

    public void startCGRFormationRound(OperatingRound_1856 or,
            Player playerToStartCGRFRound) {

        this.playerToStartCGRFRound = playerToStartCGRFRound;
        interruptedRound = or;

        if (this.playerToStartCGRFRound != null) {
            createRound (CGRFormationRound.class).start (this.playerToStartCGRFRound);
            this.playerToStartCGRFRound = null;
        } else {
            resetCertificateLimit (true);
        }
    }
    
    /* Must be called each time AFTER companies have closed from train phase 6 */
    public void resetCertificateLimit (boolean atEndOfCGRFormation) {
        
        List<PublicCompanyI> availableCompanies;

        if (atEndOfCGRFormation) cgrFormationPassed.set(true);
        if (!cgrFormationPassed.booleanValue()) return;
        
        // Determine the new certificate limit.
        // Make sure that only available Companies are counted.
        availableCompanies = getAllPublicCompanies();
        int validCompanies = availableCompanies.size(); //12, including the CGR

        for (PublicCompanyI c : availableCompanies) {
            if (c.getName().equals(PublicCompany_CGR.NAME)) {
                // If the CGR has not formed, it does not count as available
                if (!c.hasStarted()) validCompanies--;
            } else {
                // If a company has been closed before, during or after CGR formation, it does not count
                if (c.isClosed()) validCompanies--;
            }
        }
        // Some checks to be sure
        int numCompanies = Math.min(11, Math.max(4, validCompanies));
        int numPlayers = Math.min(6, Math.max(2,getNumberOfPlayers()));
        
        int newCertLimit = certLimitsTable[numPlayers-2][numCompanies-4];
        setPlayerCertificateLimit(newCertLimit);
        String message = LocalText.getText("CertificateLimit",
                newCertLimit,
                numPlayers,
                numCompanies);
        DisplayBuffer.add(message);
        ReportBuffer.add(message);
        
    }

    @Override
    public void nextRound(RoundI round) {
        if (round instanceof CGRFormationRound) {
            setRound(interruptedRound);
            ((OperatingRound_1856)interruptedRound).resume(((CGRFormationRound)round).getMergingCompanies());
        } else {
            super.nextRound(round);
        }

    }

}
