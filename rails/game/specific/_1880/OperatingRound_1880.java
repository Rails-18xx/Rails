/**
 * 
 */
package rails.game.specific._1880;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

import rails.common.DisplayBuffer;
import rails.common.GuiDef;
import rails.common.LocalText;
import rails.game.Bank;
import rails.game.Bonus;
import rails.game.GameDef;
import rails.game.GameManagerI;
import rails.game.MapHex;
import rails.game.OperatingRound;
import rails.game.PhaseI;
import rails.game.Player;
import rails.game.Portfolio;
import rails.game.PrivateCompanyI;
import rails.game.PublicCompanyI;
import rails.game.ReportBuffer;
import rails.game.TrainCertificateType;
import rails.game.TrainI;
import rails.game.TrainManager;
import rails.game.TrainType;
import rails.game.action.BuyBonusToken;
import rails.game.action.BuyPrivate;
import rails.game.action.BuyTrain;
import rails.game.action.DiscardTrain;
import rails.game.action.LayBaseToken;
import rails.game.action.NullAction;
import rails.game.action.PossibleAction;
import rails.game.action.PossibleORAction;
import rails.game.action.SetDividend;
import rails.game.action.UseSpecialProperty;
import rails.game.correct.ClosePrivate;
import rails.game.move.AddToList;
import rails.game.move.CashMove;
import rails.game.move.ObjectMove;
import rails.game.special.SellBonusToken;
import rails.game.special.SpecialPropertyI;
import rails.game.special.SpecialTileLay;
import rails.game.special.SpecialTokenLay;
import rails.game.special.SpecialTrainBuy;
import rails.game.specific._1880.PublicCompany_1880;
import rails.game.specific._1880.GameManager_1880;
import rails.game.state.EnumState;

/**
 * @author Martin
 *
 */
public class OperatingRound_1880 extends OperatingRound {

    
    protected GameDef.OrStep[] steps =
            new GameDef.OrStep[] {
                GameDef.OrStep.INITIAL,
                GameDef.OrStep.LAY_TRACK,
                GameDef.OrStep.LAY_TOKEN,
                GameDef.OrStep.CALC_REVENUE,
                GameDef.OrStep.PAYOUT,
                GameDef.OrStep.BUY_TRAIN,
                GameDef.OrStep.TRADE_SHARES,
                GameDef.OrStep.FINAL };

    /**
     * @param gameManager
     */
    public OperatingRound_1880(GameManagerI gameManager_1880) {
        super(gameManager_1880);
        // TODO Auto-generated constructor stub
    }

    @Override
    public void processPhaseAction (String name, String value) {
        if (name.equalsIgnoreCase("RaisingCertAvailability")){
            for (PublicCompanyI company : gameManager.getAllPublicCompanies()){
                if (!company.hasFloated()){
                    ((PublicCompany_1880) company).setFloatPercentage(30);
                }
            } 
        }
        if (name.equalsIgnoreCase("CommunistTakeOver")) {
            for (PublicCompanyI company : getOperatingCompanies()) {
                if (company.hasFloated()) {
                    ((PublicCompany_1880)company).setCommunistTakeOver(true);
                }
            }
            for (PublicCompanyI company : gameManager.getAllPublicCompanies()){
                if (!company.hasFloated()){
                    ((PublicCompany_1880) company).setFloatPercentage(40);
                }
            }
        }
        if (name.equalsIgnoreCase("ShanghaiExchangeOpen")) {
            for (PublicCompanyI company : getOperatingCompanies()) {
                if (company.hasFloated()) {
                    ((PublicCompany_1880)company).setCommunistTakeOver(false);
                }
            }
            for (PublicCompanyI company : gameManager.getAllPublicCompanies()){
                if (!company.hasFloated()){
                    ((PublicCompany_1880) company).setFloatPercentage(60);
                }
            }
        }
    }
    
    @Override
    protected void prepareRevenueAndDividendAction () {

        int[] allowedRevenueActions = new int[] {};
        // There is only revenue if there are any trains
        if (operatingCompany.get().canRunTrains()) {
            if (operatingCompany.get().hasStockPrice()) {
            allowedRevenueActions =
                operatingCompany.get().isSplitAlways()
                ? new int[] { SetDividend.SPLIT }
            : operatingCompany.get().isSplitAllowed()
            ? new int[] { SetDividend.PAYOUT,
                    SetDividend.SPLIT,
                    SetDividend.WITHHOLD }
                : new int[] { SetDividend.PAYOUT,
                    SetDividend.WITHHOLD };
            }
            else { //Minors in 1880 are not allowed to hand out Cash except in Closing
                allowedRevenueActions = new int[] {SetDividend.WITHHOLD };
            }

            possibleActions.add(new SetDividend(
                    operatingCompany.get().getLastRevenue(), true,
                    allowedRevenueActions));
        }
    }

    /* (non-Javadoc)
     * @see rails.game.OperatingRound#initNormalTileLays()
     */
    @Override
    protected void initNormalTileLays() {
         /**
         * Create a List of allowed normal tile lays (see LayTile class). This
         * method should be called only once per company turn in an OR: at the start
         * of the tile laying step.
         */
            String opCompany = operatingCompany.get().getName();
            // duplicate the phase colours
            Map<String, Integer> newTileColours = new HashMap<String, Integer>();
            for (String colour : getCurrentPhase().getTileColours()) {
                int allowedNumber = operatingCompany.get().getNumberOfTileLays(colour);
                // Replace the null map value with the allowed number of lays
                newTileColours.put(colour, new Integer(allowedNumber));
            }
            // store to state
            tileLaysPerColour.initFromMap(newTileColours);
        }

    /* (non-Javadoc)
     * @see rails.game.OperatingRound#start()
     */
    @Override
    public void start() {
        thisOrNumber = gameManager.getORId();

        ReportBuffer.add(LocalText.getText("START_OR", thisOrNumber));

        for (Player player : gameManager.getPlayers()) {
            player.setWorthAtORStart();
        }

        privatesPayOut();

        if ((operatingCompanies.size() > 0) && (gameManager.getAbsoluteORNumber()>= 1)){
            // even if the BCR is sold she doesn't operate until all privates have been sold
            //the absolute OR value is not incremented if not the startpacket has been sold completely

            StringBuilder msg = new StringBuilder();
            for (PublicCompanyI company : operatingCompanies.viewList()) {
                msg.append(",").append(company.getName());
            }
            if (msg.length() > 0) msg.deleteCharAt(0);
            log.info("Initial operating sequence is "+msg.toString());

            if (stepObject == null) {
                stepObject = new EnumState<GameDef.OrStep>("ORStep",  GameDef.OrStep.INITIAL);
                stepObject.addObserver(this);
            }

            if (setNextOperatingCompany(true)){
                setStep(GameDef.OrStep.INITIAL);
            }
            return;
        }

        // No operating companies yet: close the round.
        String text = LocalText.getText("ShortORExecuted");
        ReportBuffer.add(text);
        DisplayBuffer.add(text);
        finishRound();
    }

    /* (non-Javadoc)
     * @see rails.game.OperatingRound#buyTrain(rails.game.action.BuyTrain)
     */
    @Override
    public boolean buyTrain(BuyTrain action) {

        SpecialTrainBuy stb = null;
        PublicCompany_1880 oldLastTrainBuyingCompany = null;
        TrainManager TrainMgr=gameManager.getTrainManager();
        List<TrainI> trains;
        boolean lastTrainOfType = false;
        
        stb = action.getSpecialProperty();
        
        trains=TrainMgr.getAvailableNewTrains();
        
        if ((trains.size() == 1)&& (ipo.getTrainsPerType(trains.get(0).getType()).length==1 )){ // Last available train of a type is on for grabs..
            lastTrainOfType=true;
        }
        
        if (stb != null) { // A special Train buying right that gets exercised doesnt prolong the train rush
               
            oldLastTrainBuyingCompany= ((GameManager_1880) gameManager).getLastTrainBuyingCompany();
          
            if (super.buyTrain(action)) {
            	if (stb.isExercised()){
            		((GameManager_1880) gameManager).setLastTrainBuyingCompany(oldLastTrainBuyingCompany); 
            	} else {
            		((GameManager_1880) gameManager).setLastTrainBuyingCompany((PublicCompany_1880) operatingCompany.get());
            	}
            	//Check: Did we just buy the last Train of that Type ? Then we fire up the Stockround
            	if (lastTrainOfType) {
            	  ((GameManager_1880) gameManager).startStockRound_1880(this);
            	}
            	return true;
            } else {
            	return false;
            }
        } else {
        	if (super.buyTrain(action)) {
        		((GameManager_1880) gameManager).setLastTrainBuyingCompany((PublicCompany_1880) operatingCompany.get());
        		//Check: Did we just buy the last Train of that Type ? Then we fire up the Stockround
        		if (lastTrainOfType) {
                    ((GameManager_1880) gameManager).startStockRound_1880(this);
                  }
        		return true;
        	} else {
        		return false;
        	}
        }
    }

    /* (non-Javadoc)
	 * @see rails.game.OperatingRound#newPhaseChecks()
	 */
	@Override
	protected void newPhaseChecks() {
		PhaseI newPhase = getCurrentPhase();
		if (newPhase.getName()=="8") {
		((GameManager_1880) gameManager).numOfORs.set(2); // After the first 8 has been bought  there will be a last Stockround and two ORs.
		}
		else {
			if (newPhase.getName() == "8e") {
				return;
			}
		}
	
	}
	
	@Override
	public void resume () {

    	guiHints.setActivePanel(GuiDef.Panel.MAP);
        guiHints.setCurrentRoundType(getClass());

    	 if (getOperatingCompany() != null) {
             setStep(GameDef.OrStep.BUY_TRAIN);
         } else {
             finishOR();
         }
         wasInterrupted.set(true);
    }

	/* (non-Javadoc)
	 * @see rails.game.OperatingRound#process(rails.game.action.PossibleAction)
	 */
	@Override
	public boolean process(PossibleAction action) {
		
		boolean result = false;
		
		if (action instanceof PossibleORAction
                && !(action instanceof DiscardTrain)) {
            PublicCompanyI company = ((PossibleORAction) action).getCompany();
            if (company != operatingCompany.get()) {
                DisplayBuffer.add(LocalText.getText("WrongCompany",
                        company.getName(),
                        operatingCompany.get().getName() ));
                return false;
            }
        }
		
		 selectedAction = action;
		 
		 if (selectedAction instanceof NullAction) {

	            NullAction nullAction = (NullAction) action;
	            switch (nullAction.getMode()) {
	            case NullAction.DONE: //Making Sure that the NullAction.DONE is in the Buy_Train Step..
	            	 if (getStep() != GameDef.OrStep.BUY_TRAIN){
	            		 result = done();
	            		 break;
	            	 }
	            	 if (operatingCompany.get() == ((GameManager_1880) gameManager).getLastTrainBuyingCompany()){
	            		 if (trainsBoughtThisTurn.isEmpty()) {
	            			 // The current Company is the Company that has bought the last train and that purchase was not in this OR..
	            			 // we now discard the remaining active trains of that Subphase and start a stockround...
	            			 List<TrainI> trains = trainManager.getAvailableNewTrains();
	            			 TrainType currentType = trains.get(0).getType();
	            			 for (TrainI train: trains){
	            				new ObjectMove(train,ipo,scrapHeap);
	            			 }
	            			 ((GameManager_1880) gameManager).startStockRound_1880(this);
	            		 }
	            	 }
	            	 result = done();
	            	 break;
	            case NullAction.PASS:
	                result = done();
	                break;
	            case NullAction.SKIP:
	                skip();
	                result = true;
	                break;
	            }
	            return result;
		 }
		 else {
			 return super.process(action);
	          }
	}

}
    

