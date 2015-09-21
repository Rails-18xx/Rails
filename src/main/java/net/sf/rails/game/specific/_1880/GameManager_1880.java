package net.sf.rails.game.specific._1880;

import net.sf.rails.game.Player;
import net.sf.rails.game.PublicCompany;
import net.sf.rails.game.RailsRoot;
import net.sf.rails.game.Round;
import net.sf.rails.game.StartRound;
import net.sf.rails.game.financial.ShareSellingRound;
import net.sf.rails.game.financial.StockRound;
import net.sf.rails.common.GuiDef;


public class GameManager_1880 extends net.sf.rails.game.GameManager {

    protected Class<? extends ShareSellingRound> shareSellingRoundClass
    = ShareSellingRound_1880.class;

    private final ParSlotManager parSlotManager;    
    private final OperatingRoundControl_1880 orControl;
    
    public GameManager_1880(RailsRoot parent, String id) {
        super(parent, id);
        orControl = new OperatingRoundControl_1880(parent, "OrControl");
        parSlotManager = new ParSlotManager(this, "ParSlotControl");
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
    public void nextRound(Round round) {
        if (round instanceof StartRound) { 
            if (((StartRound) round).getStartPacket().areAllSold()) { // This start round was "completed"
                // check if there are other StartPackets, otherwise stockRounds start 
                beginStartRound();
            } else {
                startOperatingRound(runIfStartPacketIsNotCompletelySold());
            }
            numOfORs.set(10);
        } else if (round instanceof StockRound) {
            relativeORNumber.set(1);
            orControl.startedFromStockRound();
            startOperatingRound(true);
        } else if (round instanceof OperatingRound_1880) {
            if (orControl.getFinalOperatingRoundSequenceNumber()> 3) {
                finishGame();
            } else if (getRoot().getCompanyManager().getNextUnfinishedStartPacket() != null) {
                beginStartRound();
            } else if (orControl.isExitingToStockRound() == true) {
                startStockRound();
            } else {
                orControl.startedFromOperatingRound();
                relativeORNumber.add(1);
                startOperatingRound(true);
            }
            
        }
    }// End of nextRound

    protected void startStockRound_1880(OperatingRound_1880 or) {
        interruptedRound = or;
        super.startStockRound();
    }

    /* (non-Javadoc)
     * @see rails.game.GameManager#startShareSellingRound(rails.game.Player, int, rails.game.PublicCompanyI, boolean)
     */
    @Override
    public void startShareSellingRound(Player player, int cashToRaise,
            PublicCompany cashNeedingCompany, boolean problemDumpOtherCompanies) {
        
    
        interruptedRound = getCurrentRound();

     // An id basd on interruptedRound and company id
        String id = "SSR_" + interruptedRound.getId() + "_" + cashNeedingCompany.getId();
        // check if other companies can be dumped
        createRound(shareSellingRoundClass, id).start(interruptedRound, player, cashToRaise, cashNeedingCompany,
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
    
    public ParSlotManager getParSlotManager() {
        return parSlotManager;
    }

    public OperatingRoundControl_1880 getORControl() {
        return orControl;
    }

}
