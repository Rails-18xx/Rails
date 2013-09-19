/**
 * 
 */
package rails.game.specific._1880;

import rails.common.GuiDef;
import rails.game.GameManager;
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

    private ParSlotManager_1880 parSlotManager;    
    private OperatingRoundControl_1880 orControl = new OperatingRoundControl_1880();
    
    /**
     * 
     */
    
    public GameManager_1880() {
        super();
        parSlotManager = new ParSlotManager_1880(this);
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
            } else if (orControl.orEnded() == true) {
                startStockRound();
            } else {
                startOperatingRound(true);
            }
            
        }
    }// End of nextRound

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
    
    public ParSlotManager_1880 getParSlotManager() {
        return parSlotManager;
    }

    public OperatingRoundControl_1880 getORControl() {
        return orControl;
    }
}
