package net.sf.rails.game.specific._1880;

import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.SortedMap;
import java.util.TreeMap;

import net.sf.rails.common.DisplayBuffer;
import net.sf.rails.common.LocalText;
import net.sf.rails.common.ReportBuffer;
import net.sf.rails.common.GuiDef;
import net.sf.rails.game.BaseToken;
import net.sf.rails.game.GameDef;
import net.sf.rails.game.GameDef.OrStep;
import net.sf.rails.game.GameManager;
import net.sf.rails.game.MapHex;
import net.sf.rails.game.OperatingRound;
import net.sf.rails.game.Phase;
import net.sf.rails.game.Player;
import net.sf.rails.game.PrivateCompany;
import net.sf.rails.game.PublicCompany;
import net.sf.rails.game.Stop;
import net.sf.rails.game.Tile;
import net.sf.rails.game.Train;
import net.sf.rails.game.TrainManager;
import net.sf.rails.game.TrainType;
import net.sf.rails.game.financial.Bank;
import net.sf.rails.game.financial.PublicCertificate;
import net.sf.rails.game.model.PortfolioModel;
import net.sf.rails.game.special.SpecialTileLay;
import net.sf.rails.game.special.SpecialTrainBuy;
import net.sf.rails.game.state.ArrayListState;
import net.sf.rails.game.state.BooleanState;
import net.sf.rails.game.state.Currency;
import net.sf.rails.game.state.MoneyOwner;
import net.sf.rails.util.SequenceUtil;

import com.google.common.collect.Iterables;

import rails.game.action.BuyTrain;
import rails.game.action.DiscardTrain;
import rails.game.action.LayTile;
import rails.game.action.NullAction;
import rails.game.action.PossibleAction;
import rails.game.action.SetDividend;
import rails.game.action.UseSpecialProperty;
import rails.game.specific._1880.CloseInvestor_1880;
import rails.game.specific._1880.ExchangeForCash;
import rails.game.specific._1880.ForcedRocketExchange;

public class OperatingRound_1880 extends OperatingRound {

    private OperatingRoundControl_1880 orControl;
    private ParSlotManager parSlotManager;

    private final ArrayListState<Investor_1880> investorsToClose = ArrayListState.create(this, "investorsToClose");
    private final BooleanState trainPurchasedThisTurn = BooleanState.create(this, "trainPurchaseThisTurn");
    private PublicCompany firstCompanyBeforePrivates;
    PossibleAction manditoryNextAction = null;
    
    /**
     * @param gameManager
     */
    public OperatingRound_1880(GameManager gameManager_1880, String id) {
        super(gameManager_1880, id);
        orControl = ((GameManager_1880) gameManager_1880).getORControl();
        parSlotManager =
                ((GameManager_1880) gameManager_1880).getParSlotManager();
    }

    @Override
    public void processPhaseAction(String name, String value) {
        if (name.equalsIgnoreCase("RaisingCertAvailability")) {
            for (PublicCompany_1880 company : PublicCompany_1880.getPublicCompanies(companyManager)) {
                company.setAllCertsAvail(true);
                company.setFullFundingAvail();
                if (!company.hasFloated()) {
                    company.setFloatPercentage(30);
                } 
            }
        }
        if (name.equalsIgnoreCase("CommunistTakeOver")) {            
            for (PublicCompany_1880 company : PublicCompany_1880.getPublicCompanies(companyManager)) {
                company.stockPriceCannotMove();
                company.presidentCannotSellShare();
                if (!company.hasFloated()) {
                    company.setFloatPercentage(40);
                } 
            }
            checkForForcedRocketExchange();
        }
        
        if (name.equalsIgnoreCase("ShanghaiExchangeOpen")) {
            for (PublicCompany_1880 company : PublicCompany_1880.getPublicCompanies(companyManager)) {
                company.stockPriceCanMove();
                company.presidentCanSellShare();
                if (!company.hasFloated()) {
                    company.setFloatPercentage(60);
                }
            }
        }
    }



    @Override
    protected void prepareRevenueAndDividendAction() {
        int[] allowedRevenueActions = new int[] {};
        // There is only revenue if there are any trains
        if (operatingCompany.value().canRunTrains()) {
            if (operatingCompany.value() instanceof PublicCompany_1880) {
                allowedRevenueActions =
                        new int[] { SetDividend.PAYOUT, SetDividend.WITHHOLD };
            } else { // Investors in 1880 are not allowed to hand out Cash
                     // except
                     // in Closing
                allowedRevenueActions = new int[] { SetDividend.WITHHOLD };
            }

            possibleActions.add(new SetDividend(
                    operatingCompany.value().getLastRevenue(), true,
                    allowedRevenueActions));
        }
    }

    /*
     * (non-Javadoc)
     * 
     * @see rails.game.OperatingRound#initNormalTileLays()
     */
    @Override
    protected void initNormalTileLays() {
        /**
         * Create a List of allowed normal tile lays (see LayTile class). This
         * method should be called only once per company turn in an OR: at the
         * start of the tile laying step.
         */
        // duplicate the phase colours
        Map<String, Integer> newTileColours = new HashMap<String, Integer>();
        for (String colour : Phase.getCurrent(this).getTileColours()) {
            int allowedNumber =
                    operatingCompany.value().getNumberOfTileLays(colour);
            // Replace the null map value with the allowed number of lays
            newTileColours.put(colour, new Integer(allowedNumber));
        }
        // store to state
        tileLaysPerColour.initFromMap(newTileColours);
    }

    /*
     * (non-Javadoc)
     * 
     * @see rails.game.OperatingRound#start()
     */
    @Override
    public void start() {
        thisOrNumber = gameManager.getORId();
        

        ReportBuffer.add(this, LocalText.getText("START_OR", thisOrNumber));

        for (Player player : getRoot().getPlayerManager().getPlayers()) {
            player.setWorthAtORStart();
        }

        if ((operatingCompanies.size() > 0)
            && (gameManager.getAbsoluteORNumber() >= 1)) {
            // even if the BCR is sold she doesn't operate until all privates
            // have been sold
            // the absolute OR value is not incremented if not the startpacket
            // has been sold completely

            StringBuilder msg = new StringBuilder();
            for (PublicCompany company : operatingCompanies.view()) {
                msg.append(",").append(company.getId());
            }
            if (msg.length() > 0) msg.deleteCharAt(0);
            log.info("Initial operating sequence is " + msg.toString());

            if (stepObject == null) {
                 setStep (GameDef.OrStep.INITIAL);
                  stepObject.addObserver(this);
            }

            if (setNextOperatingCompany(true)) {
                if (orControl.getNextStep() != GameDef.OrStep.INITIAL) {
                    initTurn();
                    initNormalTileLays();
                }
                setStep(orControl.getNextStep());
                if (orControl.wasStartedFromStockRound() == true) {
                    trainPurchasedThisTurn.set(true);
                }
            } else {
                orControl.startNewOR();
                finishOR();
            }

            return;
        }

        // No operating companies yet: close the round.
        String text = LocalText.getText("ShortORExecuted");
        ReportBuffer.add(this, text);
        DisplayBuffer.add(this, text);
        finishRound();
    }

    private boolean trainTypeCanAffectOR(TrainType type) {

        if ((type.getName().equals("2R") == false) && (type.getName().equals("10") == false)
                && (type.getName().equals("8E") == false)) {
            return true;
                }
        return false;
    }

    public boolean specialBuyTrain(BuyTrain action) {
        // We might not be in the correct step...
        OrStep currentStep = getStep();
        setStep(GameDef.OrStep.BUY_TRAIN);
        boolean trainResults = super.buyTrain(action);
        setStep(currentStep);

        if (trainResults == false) {
            return false;
        }

        if ((ipo.getTrainsPerType(action.getType()).length == 0)
                && (trainTypeCanAffectOR(action.getType()) == true)) {
            orControl.orExitToStockRound(operatingCompany.value(), currentStep);
            setActionForPrivateExchange(action.getType());
            if (manditoryNextAction == null) {
                finishOR();
            }
        } 

        return true;
    }

    /*
     * (non-Javadoc)
     * 
     * @see rails.game.OperatingRound#buyTrain(rails.game.action.BuyTrain)
     */
    
    @Override
    public boolean buyTrain(BuyTrain action) {
        if (super.buyTrain(action) != true) {
            return false;
        }

        // If this train was not from the ipo, nothing else to do.
        if (action.getFromOwner() == Bank.getIpo(this)) {
            SpecialTrainBuy stb = action.getSpecialProperty();
            if ((stb == null) || (stb.isExercised() == false)) {
                if (trainTypeCanAffectOR(action.getType()) == true) {
                    trainPurchasedThisTurn.set(true);
                    orControl.trainPurchased((PublicCompany_1880) operatingCompany.value());
                    //Only Change that trainpurchase indicator if we are not in the last two phases...
                    if ((!getRoot().getPhaseManager().getCurrentPhase().getRealName().equals("D2"))&&
                            (!getRoot().getPhaseManager().getCurrentPhase().getRealName().equals("D3"))) {
                    ((GameManager_1880) getRoot().getGameManager()).getParSlotManager().trainPurchased((PublicCompany_1880) operatingCompany.value());
                    }
                }
            } 
            //If the train bought was a 8e Train no more trains will be discarded.
            if (action.getType().getName().equals("8E")) {
                orControl.setNoTrainsToDiscard(true);
            }

            // If there are no more trains of this type, and this type causes an
            // OR end, end it.
            if ((ipo.getTrainsPerType(action.getType()).length == 0)
                && (trainTypeCanAffectOR(action.getType()) == true)) {
                if (action.getType().getName().equals("8")) {
                    orControl.setLastCompanyToOperate(((PublicCompany_1880) operatingCompany.value()));
                    orControl.setFinalOperatingRoundSequence(true);
                }
                if (orControl.getFinalOperatingRoundSequenceNumber()<2){
                orControl.orExitToStockRound(operatingCompany.value(),
                        GameDef.OrStep.BUY_TRAIN);
                }
                setActionForPrivateExchange(action.getType());
                if (manditoryNextAction == null) {
                    finishOR();
                }
            } 
        }

        return true;
    }

    /*
     * (non-Javadoc)
     * 
     * @see rails.game.OperatingRound#newPhaseChecks()
     */
    @Override
    protected void newPhaseChecks() {
    }

    /*
     * (non-Javadoc)
     * 
     * @see rails.game.OperatingRound#process(rails.game.action.PossibleAction)
     */
    @Override
    public boolean process(PossibleAction action) {        
        boolean result = false;

        selectedAction = action;
        manditoryNextAction = null;

        if (selectedAction instanceof NullAction) {
            NullAction nullAction = (NullAction) action;
            switch (nullAction.getMode()) {
            case DONE: // Making Sure that the NullAction.DONE is in
                                  // the Buy_Train Step..
                if (getStep() != GameDef.OrStep.BUY_TRAIN) {
                    result = done(nullAction);
                    break;
                }
                
                if (operatingCompany.value() == orControl.lastCompanyToBuyTrain()) {
                    if ((orControl.isFinalOperatingRoundSequence()) && (!orControl.wasStartedFromStockRound())){
                        orControl.addFinalOperatingRoundSequenceNumber(1);
                    }
                    // Need to create the final Jumpoff Point there to end the game !
                    if ((orControl.getFinalOperatingRoundSequenceNumber() > 3) ) {
                    finishOR();
                    }
                     
                    if ((trainPurchasedThisTurn.value() == false) && (!orControl.noTrainsToDiscard())) {
                        // The current Company is the Company that has bought
                        // the last train and that purchase was not in this OR..
                        // we now discard the remaining active trains of that
                        // Subphase and start a stockround...
                        Set<Train> trains =
                                trainManager.getAvailableNewTrains(); 
                        TrainType activeTrainTypeToDiscard = null;
                        for (Train train : trains) {
                            if ((!train.getType().getName().equals("2R")) 
                                    && (!train.getType().getName().equals("10"))){
                                activeTrainTypeToDiscard =
                                        train.getType();
                                break;
                            }
                        }
                        if (activeTrainTypeToDiscard != null) {
                            if (activeTrainTypeToDiscard.getName().equals("8")) {
                                orControl.setLastCompanyToOperate(((PublicCompany_1880) operatingCompany.value()));
                                orControl.setFinalOperatingRoundSequence(true);
                            }
                           
                            Train[] trainsToDiscard =
                                    bank.getIpo().getPortfolioModel().getTrainsPerType(
                                            activeTrainTypeToDiscard);
                            // If we need to do a rocket exchange, then leave one 4-train
                            int firstTrainToDiscard = 0;
                            if ((activeTrainTypeToDiscard.getName().equals("4")) && 
                                    (checkForForcedRocketExchange() == true)) {
                                firstTrainToDiscard = 1;                            
                            }
                            
                            for (int i = firstTrainToDiscard; i < trainsToDiscard.length; i++) {
                                scrapHeap.addTrain(trainsToDiscard[i]);
                            }
                            // Need to make next train available !
                            trainManager.checkTrainAvailability(trainsToDiscard[0],
                                    ipo.getParent());
                            if (activeTrainTypeToDiscard.getName().equals("8E")) {
                                orControl.setNoTrainsToDiscard(true);
                                result = done(nullAction);
                                return result;
                            }
                            if (orControl.getFinalOperatingRoundSequenceNumber()<2) { // The last switch to a stock round happens on the purchase/retirement of the 8-trains.
                                orControl.orExitToStockRound(operatingCompany.value(),
                                        OrStep.BUY_TRAIN);
                                } else {
                                    orControl.startedFromOperatingRound();
                                }
                                if (!orControl.isFinalOperatingRoundSequence()) {
                                setActionForPrivateExchange(activeTrainTypeToDiscard);
                                }
                                if (manditoryNextAction == null) {
                                    finishOR();
                                }
                            }
                            return true;
                    }
                }
                result = done(nullAction);
                break;
            case PASS:
                result = done(nullAction);
                break;
            case SKIP:
                skip(nullAction);
                result = true;
                break;
            default:
                break;
            }
            return result;
        } else if (action instanceof CloseInvestor_1880) {
            closeInvestor(action);
            result = done(null);
            return result;
        } else if ((action instanceof UseSpecialProperty)
                   && (((UseSpecialProperty) action).getSpecialProperty() instanceof SpecialTrainBuy)) {
            Set<Train> trains =
                    trainManager.getAvailableNewTrains();
            BuyTrain buyTrain =
                    new BuyTrain(Iterables.get(trains,0),
                            ipo.getParent(), 0); // TODO get from special action
            buyTrain.setSpecialProperty((SpecialTrainBuy) ((UseSpecialProperty) action).getSpecialProperty()); // TODO
                                                                                                               // Fix.
            result = specialBuyTrain(buyTrain);
            return result;
        } else if ((action instanceof UseSpecialProperty)
                && (((UseSpecialProperty) action).getSpecialProperty() instanceof AddBuildingPermit)) {
            result = addBuildingPermit(action);
            return result;
        } else if (action instanceof ExchangeForCash) {
            result = exchangeForCash((ExchangeForCash) action);
            return result;
        } else if (action instanceof ForcedRocketExchange) {
            result = forcedRocketExchange((ForcedRocketExchange) action);
            return result;
        } else {
            result= super.process(action);
            if (operatingCompany.value() instanceof Investor_1880) {
                Investor_1880 investor = (Investor_1880)  (operatingCompany.value());
                if ((action instanceof SetDividend) && (investor.isConnectedToLinkedCompany() == false)) {
                    result = done(null);
                }
            }
            return result;
        }
    }

    /*
     * (non-Javadoc)
     * 
     * @see rails.game.OperatingRound#setPossibleActions()
     */
    @Override
    public boolean setPossibleActions() {
        if (manditoryNextAction != null) {
            possibleActions.add(manditoryNextAction);
            return true;
        }

        /*
         * Filter out the Tile Lay Step if the operating Company is not allowed
         * to build anymore because it doesnt possess the necessary building
         * right for this phase. Only Majors need to be prechecked.
         */
        if (getStep() == GameDef.OrStep.INITIAL) {
           if (operatingCompany.value() instanceof PublicCompany_1880)  {
            initTurn();
            if ((noMapMode)
                || (!((PublicCompany_1880) operatingCompany.value()).hasBuildingRightForPhase(gameManager.getCurrentPhase()))) {
                nextStep(GameDef.OrStep.LAY_TRACK);
            } else {
                initNormalTileLays(); // new: only called once per turn ?
                setStep(GameDef.OrStep.LAY_TRACK);
            }
          }
        }

        if ((getStep() == GameDef.OrStep.BUY_TRAIN)
            && (operatingCompany.value() instanceof Investor_1880)) {
            //Only way to get here is if this is a connected Investor...
            Investor_1880 investor = (Investor_1880) (operatingCompany.value());
            if (investor.isConnectedToLinkedCompany() ) {
            possibleActions.add(new CloseInvestor_1880((Investor_1880) operatingCompany.value()));
            return true;
            }
        }

        return super.setPossibleActions();
    }

    
    @Override
    public void resume() {
        guiHints.setActivePanel(GuiDef.Panel.MAP);
        guiHints.setCurrentRoundType(getClass());
        if (savedAction instanceof BuyTrain) {
            BuyTrain action = (BuyTrain) savedAction;
            
            // We are here because this player couldn't pay for a train.  
            Player player = playerManager.getPlayerByName(action.getPlayerName());
            PublicCompany company = action.getCompany();
            int initialPlayerCash = player.getCash();
            int trainCost = action.getFixedCost();
                                
            // Give the company enough money to buy the train, then deduct 
            // that amount from the player.
            int amountOwed = (trainCost - company.getCash());  
            Currency.wire(player, amountOwed, company);      

            // Perform the buy action
            BuyTrain newTrainBuy = new BuyTrain(action.getTrain(), action.getFromOwner(), trainCost);
            newTrainBuy.setPricePaid(trainCost);
            buyTrain (newTrainBuy);
            
            // The player has to pay a 50% penalty for any additional debt he took on.
            int additionalDebt = -player.getCash();
            if (initialPlayerCash < 0) {
                additionalDebt = additionalDebt - (-initialPlayerCash);                
            }
            
            int penalty = (additionalDebt / 2);

            ReportBuffer.add(this, LocalText.getText("DebtPenalty", player.getId(),
                   Bank.format(this, penalty)));
            Currency.wire(player, penalty, getRoot().getBank());
            }
        wasInterrupted.set(true);
    }

    /*
     * (non-Javadoc)
     * 
     * @see rails.game.OperatingRound#payout(int)
     */
    @Override
    public void payout(int amount) {

        if (amount == 0) return;

        int part;
        int shares;

        Map<MoneyOwner, Integer> sharesPerRecipient = countSharesPerRecipient();

        // Calculate, round up, report and add the cash

        // Define a precise sequence for the reporting
        Set<MoneyOwner> recipientSet = sharesPerRecipient.keySet();
        for (MoneyOwner recipient : SequenceUtil.sortCashHolders(recipientSet)) {
            if (recipient instanceof Bank) continue;
            if (recipient instanceof Investor_1880) continue;
            shares = (sharesPerRecipient.get(recipient));
            if (shares == 0) continue;
            part = (int) Math.ceil(amount * shares * operatingCompany.value().getShareUnit() / 100.0);
            
            String partText = Currency.fromBank(part, recipient);
            ReportBuffer.add(this,LocalText.getText("Payout",
                    recipient.getId(),
                    partText,
                    shares,
                    operatingCompany.value().getShareUnit()));
        }

        // Move the token
        operatingCompany.value().payout(amount);

    }


    private void closeInvestor(PossibleAction action) {
        CloseInvestor_1880 closeInvestorAction = (CloseInvestor_1880) action;
        Investor_1880 investor = closeInvestorAction.getInvestor();
        Player investorOwner = investor.getPresident();
        PublicCompany_1880 linkedCompany =
                (PublicCompany_1880) investor.getLinkedCompany();
        ReportBuffer.add(this, LocalText.getText("FIConnected", investor.getId(),
                linkedCompany.getId()));
        
        // The owner gets $50
        ReportBuffer.add(this, LocalText.getText("FIConnectedPayout",
                investorOwner.getId()));
        Currency.wire(bank, 50, investorOwner);

        // Pick where the treasury goes
        if (closeInvestorAction.getTreasuryToLinkedCompany() == true) {
            ReportBuffer.add(this, LocalText.getText("FIConnectedTreasuryToCompany",
                    linkedCompany.getId(), investor.getId(),
                    investor.getCash()));
            Currency.wireAll(investor,linkedCompany);
        } else {
            ReportBuffer.add(this, LocalText.getText("FIConnectedTreasuryToOwner",
                    investorOwner.getId(), investor.getId(),
                    (investor.getCash() / 5)));
            Currency.wire(investor, (investor.getCash() / 5), investorOwner);
            Currency.toBankAll(investor);
        }

        BaseToken token = Iterables.get(investor.getAllBaseTokens(),0);
        Stop city = (Stop) token.getOwner();
        MapHex hex = city.getParent();
        token.moveTo(investor);
        
        // Pick if the token gets replaced
        if (closeInvestorAction.getReplaceToken() == true) {
            if (hex.layBaseToken(linkedCompany, city)) {
                ReportBuffer.add(this, LocalText.getText("FIConnectedReplaceToken",
                        linkedCompany.getId(), investor.getId()));
                linkedCompany.layBaseToken(hex, 0); // (should this be
                                                    // city.getNumber as well?)
            }
        } else {
            ReportBuffer.add(this, LocalText.getText("FIConnectedDontReplaceToken",
                    linkedCompany.getId(), investor.getId()));
        }
        // Move the certificate
        ReportBuffer.add(this, LocalText.getText("FIConnectedMoveCert",
                investorOwner.getId(), linkedCompany.getId(),
                investor.getId()));
        PortfolioModel investorPortfolio = investor.getPortfolioModel();
        Set<PublicCertificate> investorCerts =
                investorPortfolio.getCertificates();
        Iterables.get(investorCerts,0).moveTo(investorOwner.getPortfolioModel()); // should
                                                                   // only be
                                                                   // one
        // Set the company to close at the end of the operating round. It's just
        // too
        // hard to do it immediately - the checks to see if the operating order
        // changed
        // conflict with the check to see if something closed.

        investorsToClose.add(investor);
    }

    /*
     * (non-Javadoc)
     * 
     * @see rails.game.Round#setOperatingCompanies()
     */
    @Override
    public List<PublicCompany> setOperatingCompanies() {
        // These are Initialized here - there's no opportunity to do this in the
        // constructor
        // for OperatingRound, and this function gets called from that
        // constructor. Yuck.
        orControl = ((GameManager_1880) gameManager).getORControl();
        parSlotManager = ((GameManager_1880) gameManager).getParSlotManager();

        List<PublicCompany> companyList = new ArrayList<PublicCompany>();

        // Put in Foreign Investors first
        for (Investor_1880 investor : Investor_1880.getInvestors(companyManager)) {
            if (investor.isClosed() == false) {
                companyList.add(investor);
            }
        }

        // Now the share companies in par slot order
        List<PublicCompany> companies =
                parSlotManager.getCompaniesInParSlotOrder();
        for (PublicCompany company : companies) {
            if (!canCompanyOperateThisRound(company)) continue;
            if (!company.hasFloated()) continue;
            companyList.add(company);
        }

        // Save the first company in the order. It is before this company that
        // privates
        // pay out.
        firstCompanyBeforePrivates = companyList.get(0);

        // Skip ahead if we have to
        //if (!orControl.isFinalOperatingRoundSequence()) {
            PublicCompany firstCompany = orControl.getFirstCompanyToRun();
            if (firstCompany != null) {
                while (companyList.get(0) != firstCompany) {
                    Collections.rotate(companyList, 1);
                }
            }
        //}

        return new ArrayList<PublicCompany>(companyList);
    }

    /*
     * (non-Javadoc)
     * 
     * @see rails.game.Round#setOperatingCompanies(java.util.List,
     * rails.game.PublicCompanyI)
     */
    @Override
    public List<PublicCompany> setOperatingCompanies(
            List<PublicCompany> oldOperatingCompanies,
            PublicCompany lastOperatingCompany) {
        // TODO Auto-generated method stub
        return setOperatingCompanies();
    }

    /*
     * ======================================= 4. LAYING TILES
     * =======================================
     */

    @Override
    public boolean layTile(LayTile action) {

        String errMsg = null;
        int cost = 0;
        SpecialTileLay stl = null;
        boolean extra = false;

        PublicCompany company = action.getCompany();
        String companyName = company.getId();
        Tile tile = action.getLaidTile();
        MapHex hex = action.getChosenHex();
        int orientation = action.getOrientation();

        // Dummy loop to enable a quick jump out.
        while (true) {
            // Checks
            // Must be correct company.
            if (!companyName.equals(operatingCompany.value().getId())) {
                errMsg =
                        LocalText.getText("WrongCompany", companyName,
                                operatingCompany.value().getId());
                break;
            }
            // Must be correct step
            if (getStep() != GameDef.OrStep.LAY_TRACK) {
                errMsg = LocalText.getText("WrongActionNoTileLay");
                break;
            }

            if (tile == null) break;

            if (!Phase.getCurrent(this).isTileColourAllowed(tile.getColourText())) {
                errMsg =
                        LocalText.getText("TileNotYetAvailable",
                                tile.toText());
                break;
            }
            if (tile.getFreeCount() == 0) {
                errMsg =
                        LocalText.getText("TileNotAvailable",
                                tile.toText());
                break;
            }

            /*
             * Check if the current tile is allowed via the LayTile allowance.
             * (currently the set if tiles is always null, which means that this
             * check is redundant. This may change in the future.
             */
            if (action != null) {
                List<Tile> tiles = action.getTiles();
                if (tiles != null && !tiles.isEmpty() && !tiles.contains(tile)) {
                    errMsg =
                            LocalText.getText("TileMayNotBeLaidInHex",
                                    tile.toText(), hex.getId());
                    break;
                }
                stl = action.getSpecialProperty();
                if (stl != null) extra = stl.isExtra();
            }

            /*
             * If this counts as a normal tile lay, check if the allowed number
             * of normal tile lays is not exceeded.
             */
            if (!extra && !validateNormalTileLay(tile)) {
                errMsg =
                        LocalText.getText("NumberOfNormalTileLaysExceeded",
                                tile.getColourText());
                break;
            }

            cost = getTileCost(hex);

            // Amount must be non-negative multiple of 10
            if (cost < 0) {
                errMsg =
                        LocalText.getText("NegativeAmountNotAllowed",
                                Bank.format(this, cost));
                break;
            }
            if (cost % 10 != 0) {
                errMsg =
                        LocalText.getText("AmountMustBeMultipleOf10",
                                Bank.format(this, cost));
                break;
            }
            // Does the company have the money?
            if (cost > operatingCompany.value().getCash()) {
                errMsg =
                        LocalText.getText("NotEnoughMoney", companyName,
                                Bank.format(this, operatingCompany.value().getCash()),
                                Bank.format(this, cost));
                break;
            }
            break;
        }
        if (errMsg != null) {
            DisplayBuffer.add(this, LocalText.getText("CannotLayTileOn", companyName,
                    tile.toText(), hex.getId(), Bank.format(this, cost),
                    errMsg));
            return false;
        }

        /* End of validation, start of execution */
        if (tile != null) {
            if (cost > 0) Currency.toBank(operatingCompany.value(),cost);
            operatingCompany.value().layTile(hex, tile, orientation, cost);

            if (cost == 0) {
                ReportBuffer.add(this, LocalText.getText("LaysTileAt", companyName,
                        tile.toText(), hex.getId(),
                        hex.getOrientationName(orientation)));
            } else {
                ReportBuffer.add(this, LocalText.getText("LaysTileAtFor",
                        companyName, tile.toText(), hex.getId(),
                        hex.getOrientationName(orientation), Bank.format(this, cost)));
            }
            hex.upgrade(action);

            // Was a special property used?
            if (stl != null) {
                stl.setExercised();
                // currentSpecialTileLays.remove(action);
                log.debug("This was a special tile lay, "
                          + (extra ? "" : " not") + " extra");

            }
            if (!extra) {
                log.debug("This was a normal tile lay");
                registerNormalTileLay(tile);
            }
        }

        if (tile == null || !areTileLaysPossible()) {
            nextStep();
        }

        return true;
    }

    
    // TODO: Make generic
    private int getTileCost(MapHex hex) {
        // Lucky us.  Tiles that cost 20, 50, and 60 happen to be rivers.  Tiles that cost
        // anything else are not.
        int baseCost = hex.getTileCost();
        if ((baseCost == 20) || (baseCost == 50) || (baseCost == 60)) {
            PrivateCompany riverFerry = companyManager.getPrivateCompany("CC");            
            if (riverFerry.getOwner() == playerManager.getCurrentPlayer()) { //TODO: Check if Correct !!
                baseCost = baseCost - 20;
            }
        }
        return baseCost;
    }

    /*
     * (non-Javadoc)
     * 
     * @see
     * rails.game.OperatingRound#processGameSpecificAction(rails.game.action
     * .PossibleAction)
     */
    @Override
    public boolean processGameSpecificAction(PossibleAction action) {
        return super.processGameSpecificAction(action);
    }

    /*
     * (non-Javadoc)
     * 
     * @see rails.game.OperatingRound#setGameSpecificPossibleActions()
     */
    @Override
    protected void setGameSpecificPossibleActions() {
        super.setGameSpecificPossibleActions();
    }

    protected void finishOR() {
        for (Investor_1880 investor : investorsToClose) {
            investor.setClosed();
        }
        super.finishOR();
    }

    protected void finishTurn() {
        if (!operatingCompany.value().isClosed()) {
            operatingCompany.value().setOperated();
            companiesOperatedThisRound.add(operatingCompany.value());

            for (PrivateCompany priv : new ArrayList<PrivateCompany>(
                    operatingCompany.value().getPortfolioModel().getPrivateCompanies())) {
                priv.checkClosingIfExercised(true);
            }
        }

        if (setNextOperatingCompany(false)) {
            setStep(GameDef.OrStep.INITIAL);
        } else {
            orControl.startNewOR();
            finishOR();
        }
    }

    @Override
    protected void privatesPayOut() {
        // Do nothing - this is now handled elsewhere because of the OR timing
    }

    protected void setOperatingCompany(PublicCompany company) {
        if (company == firstCompanyBeforePrivates) {
            super.privatesPayOut();
        }
        trainPurchasedThisTurn.set(false);
        super.setOperatingCompany(company);
    }


    private void setActionForPrivateExchange(TrainType soldOutTrainType) {
        PrivateCompany company = companyManager.getPrivateCompany("WR");
        if (company.isClosed() == false) {
            manditoryNextAction = ExchangeForCash.getAction(company, soldOutTrainType);
        }
    }

    private boolean exchangeForCash(ExchangeForCash action) {
        if (action.getExchangeCompany() == true) {
            ReportBuffer.add(this, LocalText.getText("WrExchanged",
                    action.getOwnerName(), action.getCashValue()));
            Player player =
                    playerManager.getPlayerByName(action.getOwnerName());
            Currency.wire (bank, action.getCashValue(), player);
            companyManager.getPrivateCompany("WR").close();
        }
        finishOR();
        return true;
    }
    
    private boolean checkForForcedRocketExchange() {
        PrivateCompany rocket = companyManager.getPrivateCompany("RC");

        if (rocket.isClosed() == true) {
            return false;
        }
        
        Player rocketOwner = (Player) rocket.getOwner(); //TODO : Check if correct !!
        List<PublicCompany_1880> ownedCompaniesWithSpace =
                new ArrayList<PublicCompany_1880>();
        List<PublicCompany_1880> ownedCompaniesFull =
                new ArrayList<PublicCompany_1880>();

        for (PublicCompany_1880 company : PublicCompany_1880.getPublicCompanies(companyManager)) {
            if (company.getPresident() == rocketOwner) {
                if (company.getNumberOfTrains() < company.getCurrentTrainLimit()) {
                    ownedCompaniesWithSpace.add(company);
                } else {
                    ownedCompaniesFull.add(company);
                }
            }
        }

        ForcedRocketExchange action = null;
        if (ownedCompaniesWithSpace.isEmpty() == false) {
            action = new ForcedRocketExchange();
            for (PublicCompany_1880 company : ownedCompaniesWithSpace) {
                action.addCompanyWithSpace(company);
            }
        } else if (ownedCompaniesFull.isEmpty() == false) {
            action = new ForcedRocketExchange();
            for (PublicCompany_1880 company : ownedCompaniesFull) {
                action.addCompanyWithNoSpace(company);
            }
        } else {
            ReportBuffer.add(this, LocalText.getText("RocketLost", rocketOwner));
            rocket.close();
        }
        manditoryNextAction = action;
        return (action != null);        
    }
    
    private boolean forcedRocketExchange(ForcedRocketExchange action) {
        Train train = Iterables.get(trainManager.getAvailableNewTrains(),0);
        PublicCompany company = companyManager.getPublicCompany(action.getCompanyToReceiveTrain());
        String trainNameToReplace = action.getTrainToReplace();
        
        Train replacementTrain = null;
        for (Train companyTrain : company.getPortfolioModel().getTrainList()) {
            if (companyTrain.getId().equals(trainNameToReplace)) {
                replacementTrain = companyTrain;
                break;
            }
        }

        if (replacementTrain != null) {
            ReportBuffer.add(this, LocalText.getText("RocketPlacedScrappingTrain", company.getId(), trainNameToReplace));
            replacementTrain.moveTo(scrapHeap);
        } else {
            ReportBuffer.add(this, LocalText.getText("RocketPlaced", company.getId()));            
        }
        
        company.buyTrain(train, 0);
        companyManager.getPrivateCompany("RC").close();
        train.getCertType().addToBoughtFromIPO();
        trainManager.checkTrainAvailability(train, ipo.getParent()); 
        // If there are no available trains now, time for a stock round.
        Set<Train> trains =
                trainManager.getAvailableNewTrains();
        if (train.getType() != Iterables.get(trains, 0).getType()) {
            finishOR();
        }
        return true;
    }


    private boolean addBuildingPermit(PossibleAction action) {
        AddBuildingPermit addPermit = (AddBuildingPermit) ((UseSpecialProperty) action).getSpecialProperty();
        ((PublicCompany_1880) operatingCompany.value()).addBuildingPermit(addPermit.getPermitName());
        addPermit.setExercised();
        ReportBuffer.add(this, LocalText.getText("AddedRights", operatingCompany.value().getId(), addPermit.getPermitName()));            
        return true;
    }
    
    /* (non-Javadoc)
     * @see rails.game.OperatingRound#setBuyableTrains()
     */
    @Override
    public void setBuyableTrains() {
        if (operatingCompany.getId() == null) return;

        int cash = operatingCompany.value().getCash();

        int cost = 0;
        Set<Train> trains;

       

            // Cannot buy a train without any cash, unless you have to
            if (cash == 0 && hasValidTrains()) return;

            boolean canBuyTrainNow = canBuyTrainNow();
            boolean mustBuyTrain = !hasValidTrains() && operatingCompany.value().mustOwnATrain();
            boolean emergency = false;

            SortedMap<Integer, Train> newEmergencyTrains = new TreeMap<Integer, Train>();
            SortedMap<Integer, Train> usedEmergencyTrains = new TreeMap<Integer, Train>();
            TrainManager trainMgr = getRoot().getTrainManager();

            // First check if any more trains may be bought from the Bank
            // Postpone train limit checking, because an exchange might be possible
            if (Phase.getCurrent(this).canBuyMoreTrainsPerTurn()
                    || trainsBoughtThisTurn.isEmpty()) {
                boolean mayBuyMoreOfEachType =
                        Phase.getCurrent(this).canBuyMoreTrainsPerTypePerTurn();

                /* New trains */
                trains = trainMgr.getAvailableNewTrains();
                for (Train train : trains) {
                    if (!operatingCompany.value().mayBuyTrainType(train)) continue;
                    if (!mayBuyMoreOfEachType
                            && trainsBoughtThisTurn.contains(train.getCertType())) {
                        continue;
                    }

                    // Allow dual trains (since jun 2011)
                    List<TrainType> types = train.getCertType().getPotentialTrainTypes();
                    for (TrainType type : types) {
                        cost = type.getCost();
                        if (cost <= cash) {
                            if (canBuyTrainNow) {
                                BuyTrain action = new BuyTrain(train, type, getRoot().getBank().getIpo(), cost);
                                action.setForcedBuyIfNoRoute(mustBuyTrain); // TEMPORARY
                                possibleActions.add(action);
                            }
                        } else if (mustBuyTrain) {
                            //TODO : Can't Finance a 2R Train
                            if (!train.getType().getName().equals("2R")) {
                            newEmergencyTrains.put(cost, train);
                            }
                        }
                    }

                    if (!canBuyTrainNow) continue;

                    // Can a special property be used?
                    // N.B. Assume that this never occurs in combination with
                    // dual trains or train exchanges,
                    // otherwise the below code must be duplicated above.
                    for (SpecialTrainBuy stb : getSpecialProperties(SpecialTrainBuy.class)) {
                        int reducedPrice = stb.getPrice(cost);
                        if (reducedPrice > cash) continue;
                        BuyTrain bt = new BuyTrain(train, getRoot().getBank().getIpo(), reducedPrice);
                        bt.setSpecialProperty(stb);
                        bt.setForcedBuyIfNoRoute(mustBuyTrain); // TEMPORARY
                        possibleActions.add(bt);
                    }

                }
                if (!canBuyTrainNow) return;

                /* Used trains */
                trains = getRoot().getBank().getPool().getPortfolioModel().getUniqueTrains();
                for (Train train : trains) {
                    if (!mayBuyMoreOfEachType
                            && trainsBoughtThisTurn.contains(train.getCertType())) {
                        continue;
                    }
                    cost = train.getCost();
                    if (cost <= cash) {
                        BuyTrain bt = new BuyTrain(train, getRoot().getBank().getPool(), cost);
                        bt.setForcedBuyIfNoRoute(mustBuyTrain); // TEMPORARY
                        possibleActions.add(bt);
                    } else if (mustBuyTrain) {
                        usedEmergencyTrains.put(cost, train);
                    }
                }
                if (!possibleActions.getType(BuyTrain.class).isEmpty()) { //Check if the train bought is a 2R as that doesn't fulfill the emergency conditions
                    List<PossibleAction> pActions=possibleActions.getList();
                    for (PossibleAction pAction : pActions) {
                        BuyTrain trainAction = ((BuyTrain) pAction);
                        if (trainAction.getType().getName().equals("2R") ){
                            emergency = mustBuyTrain;
                        } 
                        else {
                            emergency = mustBuyTrain && possibleActions.getType(BuyTrain.class).isEmpty();  
                        }
                    }
                } 
                else {
                    emergency = mustBuyTrain;
                }
                
                

                // If we must buy a train and haven't found one yet, the president must add cash.
                if (emergency && !newEmergencyTrains.isEmpty()) {
                        // All possible bank trains are buyable
                        for (Train train : newEmergencyTrains.values()) {
                            BuyTrain bt = new BuyTrain(train, getRoot().getBank().getIpo(), train.getCost());
                            bt.setPresidentMustAddCash(train.getCost() - cash);
                            bt.setForcedBuyIfNoRoute(mustBuyTrain); // TODO TEMPORARY
                            possibleActions.add(bt);
                        }
                        for (Train train : usedEmergencyTrains.values()) {
                            BuyTrain bt = new BuyTrain(train, getRoot().getBank().getPool(), train.getCost());
                            bt.setPresidentMustAddCash(train.getCost() - cash);
                            bt.setForcedBuyIfNoRoute(mustBuyTrain); // TODO TEMPORARY
                            possibleActions.add(bt);                        }
                    }
                }
            
            if (!canBuyTrainNow) return;

            /* Other company trains, sorted by president (current player first) */
            if (Phase.getCurrent(this).isTrainTradingAllowed()) {
                BuyTrain bt;
                Player p;
                PortfolioModel pfm;
                int index;
                int numberOfPlayers = playerManager.getNumberOfPlayers();
                int presidentCash = operatingCompany.value().getPresident().getCash();

                // Set up a list per player of presided companies
                List<List<PublicCompany>> companiesPerPlayer =
                    new ArrayList<List<PublicCompany>>(numberOfPlayers);
                for (int i = 0; i < numberOfPlayers; i++)
                    companiesPerPlayer.add(new ArrayList<PublicCompany>(4));
                List<PublicCompany> companies;
                // Sort out which players preside over which companies.
                //for (PublicCompanyI c : getOperatingCompanies()) {
                for (PublicCompany c : companyManager.getAllPublicCompanies()) {
                    if (!c.hasFloated()) continue;
                    if (c.isClosed() || c == operatingCompany.value()) continue;
                    p = c.getPresident();
                    index = p.getIndex();
                    companiesPerPlayer.get(index).add(c);
                }
                // Scan trains per company per player, operating company president
                // first
                int currentPlayerIndex = playerManager.getCurrentPlayer().getIndex();
                for (int i = currentPlayerIndex; i < currentPlayerIndex
                + numberOfPlayers; i++) {
                    companies = companiesPerPlayer.get(i % numberOfPlayers);
                    for (PublicCompany company : companies) {
                        pfm = company.getPortfolioModel();
                        trains = pfm.getUniqueTrains();

                        for (Train train : trains) {
                            if (train.isObsolete() || !train.isTradeable()) continue;
                            bt = null;
                            if (i != currentPlayerIndex
                                    && GameDef.getGameParameterAsBoolean(this, GameDef.Parm.FIXED_PRICE_TRAINS_BETWEEN_PRESIDENTS)
                                    || operatingCompany.value().mustTradeTrainsAtFixedPrice()
                                    || company.mustTradeTrainsAtFixedPrice()) {
                                // Fixed price
                                if ((cash >= train.getCost()) && (operatingCompany.value().mayBuyTrainType(train))) {
                                    bt = new BuyTrain(train, pfm.getParent(), train.getCost());
                                } else {
                                    continue;
                                }
                            } else if (cash > 0
                                    || emergency
                                    && GameDef.getGameParameterAsBoolean(this, GameDef.Parm.EMERGENCY_MAY_BUY_FROM_COMPANY)) {
                                bt = new BuyTrain(train, pfm.getParent(), 0);

                                // In some games the president may add extra cash up to the list price
                                if (emergency && cash < train.getCost()) {
                                    bt.setPresidentMayAddCash(Math.min(train.getCost() - cash,
                                            presidentCash));
                                }
                            }
                            if (bt != null) possibleActions.add(bt);
                        }
                    }
                }
            }

            if (!operatingCompany.value().mustOwnATrain()
                    || hasValidTrains()) {
                doneAllowed = true;
            }
    }

    private boolean hasValidTrains() {
        Set<Train> trains = operatingCompany.value().getPortfolioModel().getTrainList();
        int rTypeFound = 0;
        int numberOfTrainsFound = 0;
        
        numberOfTrainsFound = operatingCompany.value().getPortfolioModel().getNumberOfTrains();
        
        if (numberOfTrainsFound >0) {
            for (Train train : trains) {
                if (train.getType().getName().equals("2R")) rTypeFound++;
            }
            if (numberOfTrainsFound == rTypeFound) return false;
            return true;
        } 
        else { 
            return false;
        }
    }
    
 
}
