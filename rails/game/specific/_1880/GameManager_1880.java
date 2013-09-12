/**
 * 
 */
package rails.game.specific._1880;

import rails.common.GuiDef;
import rails.game.GameDef.OrStep;
import rails.game.GameManager;
import rails.game.OperatingRound;
import rails.game.PhaseI;
import rails.game.Player;
import rails.game.PublicCompanyI;
import rails.game.RoundI;
import rails.game.ShareSellingRound;
import rails.game.StartPacket;
import rails.game.StartRound;
import rails.game.StockRound;
import rails.game.state.IntegerState;

/**
 * @author Martin Brumm
 * @date 21.1.2012
 * 
 */



public class GameManager_1880 extends GameManager {

    protected Class<? extends ShareSellingRound> shareSellingRoundClass
    = ShareSellingRound_1880.class;
    
    public IntegerState numOfORs = new IntegerState("numOfORs");
    //Keeps track of the company that purchased the last train
    private PublicCompany_1880 lastTrainBuyingCompany;
    private ParSlots_1880 parSlots = new ParSlots_1880();
    
    private PublicCompanyI firstCompanyToOperate = null;
    private boolean skipFirstCompanyToOperate = false;
    private OrStep nextOperatingPhase = OrStep.INITIAL;
    /**
     * 
     */
    
    
    public GameManager_1880() {
        super();
    }

    @Override
    protected void setGuiParameters () {
        super.setGuiParameters();
        guiParameters.put(GuiDef.Parm.PLAYER_ORDER_VARIES, true);
        guiParameters.put(GuiDef.Parm.HAS_ANY_PAR_PRICE, false);
    }

    /* (non-Javadoc)
     * @see rails.game.GameManager#nextRound(rails.game.RoundI)
     */
    @Override
    public void nextRound(RoundI round) {
        if (round instanceof StartRound) { 
            if (((StartRound) round).getStartPacket().areAllSold()) { // This start round was "completed"
                StartPacket nextStartPacket = companyManager.getNextUnfinishedStartPacket();
                if (nextStartPacket == null) {
                    startStockRound(); // All start rounds complete - start stock rounds
                } else {
                    startStartRound(nextStartPacket); // Start next start round
                }
            } else {
                startOperatingRound(runIfStartPacketIsNotCompletelySold());
            }
            numOfORs.set(10);
        } else if (round instanceof StockRound) {
            relativeORNumber.set(1);
            startOperatingRound(true);
        } else if (round instanceof OperatingRound_1880) {
            if (gameOverPending.booleanValue() && !gameEndsAfterSetOfORs) {
                finishGame();
            } else if (companyManager.getNextUnfinishedStartPacket() != null) {
                continueStartRound(companyManager.getNextUnfinishedStartPacket());
            } else if (gameOverPending.booleanValue() && gameEndsAfterSetOfORs) {
                finishGame();
            } else if (firstCompanyToOperate != null) {
                startStockRound();
            } else {
                startOperatingRound(true);
            }
            
        }
    }// End of nextRound

    /**
     * @return the lastTrainBuyingCompany
     */
    public PublicCompany_1880 getLastTrainBuyingCompany() {
        return lastTrainBuyingCompany;
    }

    /**
     * @param lastTrainBuyingCompany the lastTrainBuyingCompany to set
     */
    public void setLastTrainBuyingCompany(PublicCompany_1880 lastTrainBuyingCompany) {
        this.lastTrainBuyingCompany = lastTrainBuyingCompany;
    }

    /* (non-Javadoc)
     * @see rails.game.GameManager#startStockRound()
     */

    protected void startStockRound_1880(OperatingRound_1880 or) {
        // TODO Auto-generated method stub
        interruptedRound = or;
        super.startStockRound();
    }

    /* (non-Javadoc)
     * @see rails.game.GameManager#startShareSellingRound(rails.game.Player, int, rails.game.PublicCompanyI, boolean)
     */
    @Override
    public void startShareSellingRound(Player player, int cashToRaise,
            PublicCompanyI cashNeedingCompany, boolean problemDumpOtherCompanies) {
        
    
        interruptedRound = getCurrentRound();

        // check if other companies can be dumped
        createRound (shareSellingRoundClass, interruptedRound)
        .start(player, cashToRaise, cashNeedingCompany,
                !problemDumpOtherCompanies || forcedSellingCompanyDump);
        // the last parameter indicates if the dump of other companies is allowed, either this is explicit or
        // the action does not require that check

    }

    /* (non-Javadoc)
     * @see rails.game.GameManager#finishShareSellingRound()
     */
    @Override
    public void finishShareSellingRound() {

        possibleActions.clear(); //Do we need this here ? 
        
        super.finishShareSellingRound();
    }
    
    public ParSlots_1880 getParSlots() {
        return parSlots;
    }

    public PublicCompanyI getFirstCompanyToOperate() {
        return firstCompanyToOperate;
    }

    public void setFirstCompanyToOperate(PublicCompanyI firstCompanyToOperate) {
        this.firstCompanyToOperate = firstCompanyToOperate;
    }

    public OrStep getNextOperatingPhase() {
        return nextOperatingPhase;
    }

    public void setNextOperatingPhase(OrStep orStep) {
        this.nextOperatingPhase = orStep;
    }

    public boolean getSkipFirstCompanyToOperate() {
        return skipFirstCompanyToOperate;
    }

    public void setSkipFirstCompanyToOperate(boolean skipFirstCompanyToOperate) {
        this.skipFirstCompanyToOperate = skipFirstCompanyToOperate;
    }
}
