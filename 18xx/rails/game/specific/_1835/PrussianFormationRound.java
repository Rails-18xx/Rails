package rails.game.specific._1835;

import java.util.List;

import rails.common.GuiDef;
import rails.game.*;
import rails.game.action.MergeCompanies;
import rails.game.action.NullAction;
import rails.game.action.StartCompany;
import rails.util.LocalText;

public class PrussianFormationRound extends StockRound {
    
    private PublicCompanyI prussian;
    private PhaseI phase;

	private boolean startPr;
	private boolean forcedStart;
	private boolean mergePr;
	private boolean forcedMerge;
    
    private enum Step {
        START,
        MERGE
    };
    
    Step step;

	public static String PR_ID = "Pr";
    public static String M2_ID = "M2";

    public PrussianFormationRound (GameManagerI gameManager) {
        super (gameManager);

        guiHints.setVisibilityHint(GuiDef.Panel.MAP, true);
        guiHints.setVisibilityHint(GuiDef.Panel.STATUS, true);

    }

 	public void start() {

        prussian = companyManager.getCompanyByName(PR_ID);
        phase = getCurrentPhase();
		startPr = !prussian.hasStarted();
        forcedStart = phase.getName().equals("4+4");
 		mergePr = !prussianIsComplete(gameManager);
 		forcedMerge = phase.getName().equals("5");

        ReportBuffer.add(LocalText.getText("StartFormationRound", PR_ID));
        log.debug("StartPr="+startPr+" forcedStart="+forcedStart
        		+" mergePr="+mergePr+" forcedMerge="+forcedMerge);
        
        step = startPr ? Step.START : Step.MERGE;

    }

    public boolean setPossibleActions() {
        
        if (step == Step.START) {
            PublicCompanyI m2 = companyManager.getCompanyByName(M2_ID);
            Player m2Owner = m2.getPresident();
            setCurrentPlayer(m2Owner);
            ReportBuffer.add(LocalText.getText("StartingPlayer", 
                    getCurrentPlayer().getName()));

            possibleActions.add(new MergeCompanies(m2, prussian));
            
            if (!forcedStart) {
                possibleActions.add(new NullAction(NullAction.PASS));
            }
        }
        
        return true;
        
    }
    
    public static boolean prussianIsComplete(GameManagerI gameManager) {
        List<PublicCertificateI> unissued 
                = gameManager.getBank().getUnavailable().getCertificatesPerCompany(PR_ID);
        return unissued == null || unissued.isEmpty();
    }
    @Override
    public String toString() {
        return "1835 PrussianFormationRound";
    }

}
