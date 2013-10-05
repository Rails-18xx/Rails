/**
 * 
 */
package rails.game.specific._1880;

import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;

import rails.common.DisplayBuffer;
import rails.common.LocalText;
import rails.common.GuiDef;
import rails.game.Bank;
import rails.game.BaseToken;
import rails.game.CashHolder;
import rails.game.GameDef;
import rails.game.GameDef.OrStep;
import rails.game.GameManagerI;
import rails.game.MapHex;
import rails.game.OperatingRound;
import rails.game.PhaseI;
import rails.game.Player;
import rails.game.Portfolio;
import rails.game.PrivateCompanyI;
import rails.game.PublicCertificateI;
import rails.game.PublicCompanyI;
import rails.game.ReportBuffer;
import rails.game.Stop;
import rails.game.TileI;
import rails.game.TrainI;
import rails.game.TrainType;
import rails.game.action.BuyTrain;
import rails.game.action.DiscardTrain;
import rails.game.action.LayTile;
import rails.game.action.NullAction;
import rails.game.action.PossibleAction;
import rails.game.action.SetDividend;
import rails.game.action.UseSpecialProperty;
import rails.game.move.CashMove;
import rails.game.move.ObjectMove;
import rails.game.special.SpecialTileLay;
import rails.game.special.SpecialTrainBuy;
import rails.game.specific._1880.PublicCompany_1880;
import rails.game.specific._1880.GameManager_1880;
import rails.game.state.BooleanState;
import rails.game.state.EnumState;
import rails.util.SequenceUtil;

/**
 * @author Martin
 * 
 */
public class OperatingRound_1880 extends OperatingRound {

    private OperatingRoundControl_1880 orControl;
    private ParSlotManager_1880 parSlotManager;

    List<Investor_1880> investorsToClose = new ArrayList<Investor_1880>();
    PossibleAction manditoryNextAction = null;
    private PublicCompanyI firstCompanyBeforePrivates;
    private BooleanState trainPurchasedThisTurn = new BooleanState ("trainPurchaseThisTurn",false);
    
    /**
     * @param gameManager
     */
    public OperatingRound_1880(GameManagerI gameManager_1880) {
        super(gameManager_1880);
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
        if (operatingCompany.get().canRunTrains()) {
            if (operatingCompany.get() instanceof PublicCompany_1880) {
                allowedRevenueActions =
                        new int[] { SetDividend.PAYOUT, SetDividend.WITHHOLD };
            } else { // Investors in 1880 are not allowed to hand out Cash
                     // except
                     // in Closing
                allowedRevenueActions = new int[] { SetDividend.WITHHOLD };
            }

            possibleActions.add(new SetDividend(
                    operatingCompany.get().getLastRevenue(), true,
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
        for (String colour : getCurrentPhase().getTileColours()) {
            int allowedNumber =
                    operatingCompany.get().getNumberOfTileLays(colour);
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

        ReportBuffer.add(LocalText.getText("START_OR", thisOrNumber));

        for (Player player : gameManager.getPlayers()) {
            player.setWorthAtORStart();
        }

        if ((operatingCompanies.size() > 0)
            && (gameManager.getAbsoluteORNumber() >= 1)) {
            // even if the BCR is sold she doesn't operate until all privates
            // have been sold
            // the absolute OR value is not incremented if not the startpacket
            // has been sold completely

            StringBuilder msg = new StringBuilder();
            for (PublicCompanyI company : operatingCompanies.viewList()) {
                msg.append(",").append(company.getName());
            }
            if (msg.length() > 0) msg.deleteCharAt(0);
            log.info("Initial operating sequence is " + msg.toString());

            if (stepObject == null) {
                stepObject =
                        new EnumState<GameDef.OrStep>("ORStep",
                                GameDef.OrStep.INITIAL);
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
        ReportBuffer.add(text);
        DisplayBuffer.add(text);
        finishRound();
    }

    private boolean trainTypeCanEndOR(TrainType type) {
        if (type.getName().equals("2R") == false) {
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
                && (trainTypeCanEndOR(action.getType()) == true)) {
            orControl.orExitToStockRound(operatingCompany.get(), currentStep);
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
        if (action.getFromPortfolio() == ipo) {
            SpecialTrainBuy stb = action.getSpecialProperty();
            if ((stb == null) || (stb.isExercised() == false)) {
                trainPurchasedThisTurn.set(true);
                orControl.trainPurchased((PublicCompany_1880) operatingCompany.get());
            } 

            // If there are no more trains of this type, and this type causes an
            // OR end, end it.
            if ((ipo.getTrainsPerType(action.getType()).length == 0)
                && (trainTypeCanEndOR(action.getType()) == true)) {
                orControl.orExitToStockRound(operatingCompany.get(),
                        GameDef.OrStep.BUY_TRAIN);
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
        PhaseI newPhase = getCurrentPhase();
        if (newPhase.getName().equals("8")) {
            ((GameManager_1880) gameManager).numOfORs.set(2);
            // After the first 8 has been bought there will be a last
            // Stockround and two ORs.
        } else if (newPhase.getName().equals("8e")) {
            return;
        }

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
            case NullAction.DONE: // Making Sure that the NullAction.DONE is in
                                  // the Buy_Train Step..
                if (getStep() != GameDef.OrStep.BUY_TRAIN) {
                    result = done();
                    break;
                }
                
                if (operatingCompany.get() == orControl.lastCompanyToBuyTrain()) {
                    if (trainPurchasedThisTurn.booleanValue() == false) {
                        // The current Company is the Company that has bought
                        // the last train and that purchase was not in this OR..
                        // we now discard the remaining active trains of that
                        // Subphase and start a stockround...
                        List<TrainI> trains =
                                trainManager.getAvailableNewTrains();
                        TrainType activeTrainTypeToDiscard =
                                trains.get(0).getType();
                        TrainI[] trainsToDiscard =
                                bank.getIpo().getTrainsPerType(
                                        activeTrainTypeToDiscard);
                        // If we need to do a rocket exchange, then leave one 4-train
                        int firstTrainToDiscard = 0;
                        if ((activeTrainTypeToDiscard.getName().equals("4")) && 
                                (checkForForcedRocketExchange() == true)) {
                            firstTrainToDiscard = 1;                            
                        }
                        
                        for (int i = firstTrainToDiscard; i < trainsToDiscard.length; i++) {
                            new ObjectMove(trainsToDiscard[i], ipo, scrapHeap);
                        }
                        // Need to make next train available !
                        trainManager.checkTrainAvailability(trainsToDiscard[0],
                                ipo);
                        orControl.orExitToStockRound(operatingCompany.get(),
                                OrStep.BUY_TRAIN);
                        setActionForPrivateExchange(activeTrainTypeToDiscard);
                        if (manditoryNextAction == null) {
                            finishOR();
                        }
                        return true;
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
        } else if (action instanceof CloseInvestor_1880) {
            closeInvestor(action);
            result = done();
            return result;
        } else if ((action instanceof UseSpecialProperty)
                   && (((UseSpecialProperty) action).getSpecialProperty() instanceof SpecialTrainBuy)) {
            BuyTrain buyTrain =
                    new BuyTrain(trainManager.getAvailableNewTrains().get(0),
                            ipo, 0); // TODO get from special action
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
            return super.process(action);
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
        if ((getStep() == GameDef.OrStep.INITIAL)
            && (operatingCompany.get().getTypeName().equals("Major"))) {
            initTurn();
            if ((noMapMode)
                || (!((PublicCompany_1880) operatingCompany.get()).hasBuildingRightForPhase(gameManager.getCurrentPhase()))) {
                nextStep(GameDef.OrStep.LAY_TRACK);
            } else {
                initNormalTileLays(); // new: only called once per turn ?
                setStep(GameDef.OrStep.LAY_TRACK);
            }
        }

        if ((getStep() == GameDef.OrStep.BUY_TRAIN)
            && (operatingCompany.get() instanceof Investor_1880)) {
            setStep(GameDef.OrStep.TRADE_SHARES);
        }

        if ((getStep() == GameDef.OrStep.TRADE_SHARES)
            && (operatingCompany.get() instanceof Investor_1880)) {
            Investor_1880 investor = (Investor_1880) operatingCompany.get();
            if (investor.isConnectedToLinkedCompany() == true) {
                possibleActions.add(new CloseInvestor_1880(investor));
                return true;
            } else {
                nextStep(GameDef.OrStep.FINAL);
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
            PublicCompanyI company = action.getCompany();
            int initialPlayerCash = player.getCash();
            int trainCost = action.getFixedCost();
                                
            // Give the company enough money to buy the train, then deduct 
            // that amount from the player.
            int amountOwed = (trainCost - company.getCash());            
            company.addCash(amountOwed);
            player.addCash(-amountOwed);

            // Perform the buy action
            BuyTrain newTrainBuy = new BuyTrain(action.getTrain(), action.getFromPortfolio(), trainCost);
            newTrainBuy.setPricePaid(trainCost);
            buyTrain (newTrainBuy);
            
            // The player has to pay a 50% penalty for any additional debt he took on.
            int additionalDebt = -player.getCash();
            if (initialPlayerCash < 0) {
                additionalDebt = additionalDebt - (-initialPlayerCash);                
            }
            
            int penalty = (additionalDebt / 2);

            ReportBuffer.add(LocalText.getText("DebtPenalty", player.getName(),
                    Bank.format(penalty)));

            player.addCash(-penalty);
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

        Map<CashHolder, Integer> sharesPerRecipient = countSharesPerRecipient();

        // Calculate, round up, report and add the cash

        // Define a precise sequence for the reporting
        Set<CashHolder> recipientSet = sharesPerRecipient.keySet();
        for (CashHolder recipient : SequenceUtil.sortCashHolders(recipientSet)) {
            if (recipient instanceof Bank) continue;
            if (recipient instanceof Investor_1880) continue;
            shares = (sharesPerRecipient.get(recipient));
            if (shares == 0) continue;
            part =
                    (int) Math.ceil(amount * shares
                                    * operatingCompany.get().getShareUnit()
                                    / 100.0);
            ReportBuffer.add(LocalText.getText("Payout", recipient.getName(),
                    Bank.format(part), shares,
                    operatingCompany.get().getShareUnit()));
            pay(bank, recipient, part);
        }

        // Move the token
        operatingCompany.get().payout(amount);

    }

    private void closeInvestor(PossibleAction action) {
        CloseInvestor_1880 closeInvestorAction = (CloseInvestor_1880) action;
        Investor_1880 investor = closeInvestorAction.getInvestor();
        Player investorOwner = investor.getPresident();
        PublicCompany_1880 linkedCompany =
                (PublicCompany_1880) investor.getLinkedCompany();
        ReportBuffer.add(LocalText.getText("FIConnected", investor.getName(),
                linkedCompany.getName()));

        // The owner gets $50
        ReportBuffer.add(LocalText.getText("FIConnectedPayout",
                investorOwner.getName()));
        new CashMove(bank, investorOwner, 50);

        // Pick where the treasury goes
        if (closeInvestorAction.getTreasuryToLinkedCompany() == true) {
            ReportBuffer.add(LocalText.getText("FIConnectedTreasuryToCompany",
                    linkedCompany.getName(), investor.getName(),
                    investor.getCash()));
            new CashMove(investor, linkedCompany, investor.getCash());
        } else {
            ReportBuffer.add(LocalText.getText("FIConnectedTreasuryToOwner",
                    investorOwner.getName(), investor.getName(),
                    (investor.getCash() / 5)));
            new CashMove(investor, investorOwner, (investor.getCash() / 5));
            new CashMove(investor, bank, investor.getCash());
        }

        BaseToken token = (BaseToken) investor.getTokens().get(0);
        MapHex hex = investor.getHomeHexes().get(0);
        Stop city = (Stop) token.getHolder();
        token.moveTo(token.getCompany());

        // Pick if the token gets replaced
        if (closeInvestorAction.getReplaceToken() == true) {
            if (hex.layBaseToken(linkedCompany, city.getNumber())) {
                ReportBuffer.add(LocalText.getText("FIConnectedReplaceToken",
                        linkedCompany.getName(), investor.getName()));
                linkedCompany.layBaseToken(hex, 0); // (should this be
                                                    // city.getNumber as well?)
            }
        } else {
            ReportBuffer.add(LocalText.getText("FIConnectedDontReplaceToken",
                    linkedCompany.getName(), investor.getName()));
        }
        // Move the certificate
        ReportBuffer.add(LocalText.getText("FIConnectedMoveCert",
                investorOwner.getName(), linkedCompany.getName(),
                investor.getName()));
        Portfolio investorPortfolio = investor.getPortfolio();
        List<PublicCertificateI> investorCerts =
                investorPortfolio.getCertificates();
        investorCerts.get(0).moveTo(investorOwner.getPortfolio()); // should
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
    public List<PublicCompanyI> setOperatingCompanies() {
        // These are Initialized here - there's no opportunity to do this in the
        // constructor
        // for OperatingRound, and this function gets called from that
        // constructor. Yuck.
        orControl = ((GameManager_1880) gameManager).getORControl();
        parSlotManager = ((GameManager_1880) gameManager).getParSlotManager();

        List<PublicCompanyI> companyList = new ArrayList<PublicCompanyI>();

        // Put in Foreign Investors first
        for (Investor_1880 investor : Investor_1880.getInvestors(companyManager)) {
            if (investor.isClosed() == false) {
                companyList.add(investor);
            }
        }

        // Now the share companies in par slot order
        List<PublicCompanyI> companies =
                parSlotManager.getCompaniesInParSlotOrder();
        for (PublicCompanyI company : companies) {
            if (!canCompanyOperateThisRound(company)) continue;
            if (!company.hasFloated()) continue;
            companyList.add(company);
        }

        // Save the first company in the order. It is before this company that
        // privates
        // pay out.
        firstCompanyBeforePrivates = companyList.get(0);

        // Skip ahead if we have to
        PublicCompanyI firstCompany = orControl.getFirstCompanyToRun();
        if (firstCompany != null) {
            while (companyList.get(0) != firstCompany) {
                Collections.rotate(companyList, 1);
            }
        }

        return new ArrayList<PublicCompanyI>(companyList);
    }

    /*
     * (non-Javadoc)
     * 
     * @see rails.game.Round#setOperatingCompanies(java.util.List,
     * rails.game.PublicCompanyI)
     */
    @Override
    public List<PublicCompanyI> setOperatingCompanies(
            List<PublicCompanyI> oldOperatingCompanies,
            PublicCompanyI lastOperatingCompany) {
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

        PublicCompanyI company = action.getCompany();
        String companyName = company.getName();
        TileI tile = action.getLaidTile();
        MapHex hex = action.getChosenHex();
        int orientation = action.getOrientation();

        // Dummy loop to enable a quick jump out.
        while (true) {
            // Checks
            // Must be correct company.
            if (!companyName.equals(operatingCompany.get().getName())) {
                errMsg =
                        LocalText.getText("WrongCompany", companyName,
                                operatingCompany.get().getName());
                break;
            }
            // Must be correct step
            if (getStep() != GameDef.OrStep.LAY_TRACK) {
                errMsg = LocalText.getText("WrongActionNoTileLay");
                break;
            }

            if (tile == null) break;

            if (!getCurrentPhase().isTileColourAllowed(tile.getColourName())) {
                errMsg =
                        LocalText.getText("TileNotYetAvailable",
                                tile.getExternalId());
                break;
            }
            if (tile.countFreeTiles() == 0) {
                errMsg =
                        LocalText.getText("TileNotAvailable",
                                tile.getExternalId());
                break;
            }

            /*
             * Check if the current tile is allowed via the LayTile allowance.
             * (currently the set if tiles is always null, which means that this
             * check is redundant. This may change in the future.
             */
            if (action != null) {
                List<TileI> tiles = action.getTiles();
                if (tiles != null && !tiles.isEmpty() && !tiles.contains(tile)) {
                    errMsg =
                            LocalText.getText("TileMayNotBeLaidInHex",
                                    tile.getExternalId(), hex.getName());
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
                                tile.getColourName());
                break;
            }

            cost = getTileCost(hex);

            // Amount must be non-negative multiple of 10
            if (cost < 0) {
                errMsg =
                        LocalText.getText("NegativeAmountNotAllowed",
                                Bank.format(cost));
                break;
            }
            if (cost % 10 != 0) {
                errMsg =
                        LocalText.getText("AmountMustBeMultipleOf10",
                                Bank.format(cost));
                break;
            }
            // Does the company have the money?
            if (cost > operatingCompany.get().getCash()) {
                errMsg =
                        LocalText.getText("NotEnoughMoney", companyName,
                                Bank.format(operatingCompany.get().getCash()),
                                Bank.format(cost));
                break;
            }
            break;
        }
        if (errMsg != null) {
            DisplayBuffer.add(LocalText.getText("CannotLayTileOn", companyName,
                    tile.getExternalId(), hex.getName(), Bank.format(cost),
                    errMsg));
            return false;
        }

        /* End of validation, start of execution */
        moveStack.start(true);

        if (tile != null) {
            if (cost > 0) new CashMove(operatingCompany.get(), bank, cost);
            operatingCompany.get().layTile(hex, tile, orientation, cost);

            if (cost == 0) {
                ReportBuffer.add(LocalText.getText("LaysTileAt", companyName,
                        tile.getExternalId(), hex.getName(),
                        hex.getOrientationName(orientation)));
            } else {
                ReportBuffer.add(LocalText.getText("LaysTileAtFor",
                        companyName, tile.getExternalId(), hex.getName(),
                        hex.getOrientationName(orientation), Bank.format(cost)));
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
            PrivateCompanyI riverFerry = companyManager.getPrivateCompany("CC");            
            if (riverFerry.getPortfolio().getOwner() == getCurrentPlayer()) {
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
        if (!operatingCompany.get().isClosed()) {
            operatingCompany.get().setOperated();
            companiesOperatedThisRound.add(operatingCompany.get());

            for (PrivateCompanyI priv : new ArrayList<PrivateCompanyI>(
                    operatingCompany.get().getPortfolio().getPrivateCompanies())) {
                priv.checkClosingIfExercised(true);
            }
        }

        if (!finishTurnSpecials()) return;

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

    protected void setOperatingCompany(PublicCompanyI company) {
        if (company == firstCompanyBeforePrivates) {
            super.privatesPayOut();
        }
        trainPurchasedThisTurn.set(false);
        super.setOperatingCompany(company);
    }


    private void setActionForPrivateExchange(TrainType soldOutTrainType) {
        PrivateCompanyI company = companyManager.getPrivateCompany("WR");
        if (company.isClosed() == false) {
            manditoryNextAction = ExchangeForCash.getAction(company, soldOutTrainType);
        }
    }

    private boolean exchangeForCash(ExchangeForCash action) {
        if (action.getExchangeCompany() == true) {
            ReportBuffer.add(LocalText.getText("WrExchanged",
                    action.getOwnerName(), action.getCashValue()));
            Player player =
                    playerManager.getPlayerByName(action.getOwnerName());
            new CashMove(bank, player, action.getCashValue());
            companyManager.getPrivateCompany("WR").close();
        }
        finishOR();
        return true;
    }
    
    private boolean checkForForcedRocketExchange() {
        PrivateCompanyI rocket = companyManager.getPrivateCompany("RC");

        if (rocket.isClosed() == true) {
            return false;
        }
        
        Player rocketOwner = (Player) rocket.getPortfolio().getOwner();
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
            ReportBuffer.add(LocalText.getText("RocketLost", rocketOwner));
            rocket.close();
        }
        manditoryNextAction = action;
        return (action != null);        
    }
    
    private boolean forcedRocketExchange(ForcedRocketExchange action) {
        moveStack.start(true);
        TrainI train = trainManager.getAvailableNewTrains().get(0);
        PublicCompanyI company = companyManager.getPublicCompany(action.getCompanyToReceiveTrain());
        String trainNameToReplace = action.getTrainToReplace();
        
        TrainI replacementTrain = null;
        for (TrainI companyTrain : company.getPortfolio().getTrainList()) {
            if (companyTrain.getName().equals(trainNameToReplace)) {
                replacementTrain = companyTrain;
                break;
            }
        }

        if (replacementTrain != null) {
            ReportBuffer.add(LocalText.getText("RocketPlacedScrappingTrain", company.getName(), trainNameToReplace));
            replacementTrain.moveTo(scrapHeap);
        } else {
            ReportBuffer.add(LocalText.getText("RocketPlaced", company.getName()));            
        }
        
        company.buyTrain(train, 0);
        companyManager.getPrivateCompany("RC").close();
        train.getCertType().addToBoughtFromIPO();
        trainManager.checkTrainAvailability(train, ipo); 
        // If there are no available trains now, time for a stock round.
        if (train.getType() != trainManager.getAvailableNewTrains().get(0).getType()) {
            finishOR();
        }
        return true;
    }


    private boolean addBuildingPermit(PossibleAction action) {
        AddBuildingPermit addPermit = (AddBuildingPermit) ((UseSpecialProperty) action).getSpecialProperty();
        ((PublicCompany_1880) operatingCompany.get()).addBuildingPermit(addPermit.getPermitName());
        addPermit.setExercised();
        ReportBuffer.add(LocalText.getText("AddedRights", operatingCompany.get().getName(), addPermit.getPermitName()));            
        return true;
    }
    
    public boolean discardTrain(DiscardTrain action) {
        if (super.discardTrain(action) == false) {
            return false;
        }
        
        action.getDiscardedTrain().moveTo(scrapHeap);
        return true;
    }

}