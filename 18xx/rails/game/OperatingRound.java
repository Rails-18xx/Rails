/* $Header: /Users/blentz/rails_rcs/cvs/18xx/rails/game/OperatingRound.java,v 1.85 2009/12/27 18:30:11 evos Exp $ */
package rails.game;

import java.util.*;

import rails.game.action.*;
import rails.game.move.CashMove;
import rails.game.move.MapChange;
import rails.game.special.*;
import rails.game.state.IntegerState;
import rails.util.LocalText;

/**
 * Implements a basic Operating Round. <p> A new instance must be created for
 * each new Operating Round. At the end of a round, the current instance should
 * be discarded. <p> Permanent memory is formed by static attributes.
 */
public class OperatingRound extends Round implements Observer {

    /* Transient memory (per round only) */
    protected IntegerState stepObject;

    protected boolean actionPossible = true;

    protected String actionNotPossibleMessage = "";

    protected TreeMap<Integer, PublicCompanyI> operatingCompanies;
    protected List<PublicCompanyI> companiesOperatedThisRound
    	= new ArrayList<PublicCompanyI> ();

    protected PublicCompanyI[] operatingCompanyArray;

    protected IntegerState operatingCompanyIndexObject;

    protected int operatingCompanyIndex;

    protected PublicCompanyI operatingCompany;

    // Non-persistent lists (are recreated after each user action)
    protected List<SpecialPropertyI> currentSpecialProperties = null;

    protected List<LayTile> currentSpecialTileLays = new ArrayList<LayTile>();

    protected List<LayTile> currentNormalTileLays = new ArrayList<LayTile>();

    protected Map<String, Integer> tileLaysPerColour =
            new HashMap<String, Integer>();

    protected List<LayBaseToken> currentNormalTokenLays =
            new ArrayList<LayBaseToken>();

    protected List<LayBaseToken> currentSpecialTokenLays =
            new ArrayList<LayBaseToken>();

    /** A List per player with owned companies that have excess trains */
    protected Map<Player, List<PublicCompanyI>> excessTrainCompanies = null;

    protected List<TrainTypeI> trainsBoughtThisTurn =
            new ArrayList<TrainTypeI>(4);

    protected Map<PublicCompanyI, Integer> loansThisRound = null;

    protected PhaseI currentPhase;

    protected String thisOrNumber;

    protected PossibleAction selectedAction = null;

    protected PossibleAction savedAction = null;

    protected int cashToBeRaisedByPresident = 0;

    protected int splitRule = SPLIT_NOT_ALLOWED; // To be made configurable

    /* Permanent memory */
    static protected List<Player> players;

    static protected int numberOfPlayers = 0;

    /* Constants */
    public static final int SPLIT_NOT_ALLOWED = 0;

    public static final int SPLIT_ROUND_UP = 1; // More money to the
    // shareholders

    public static final int SPLIT_ROUND_DOWN = 2; // More to the treasury

    public static final int STEP_INITIAL = 0;

    public static final int STEP_LAY_TRACK = 0;

    public static final int STEP_LAY_TOKEN = 1;

    public static final int STEP_CALC_REVENUE = 2;

    public static final int STEP_PAYOUT = 3;

    public static final int STEP_BUY_TRAIN = 4;

    public static final int STEP_TRADE_SHARES = 5;

    public static int STEP_FINAL = 6;

    protected static int[] steps =
            new int[] { STEP_LAY_TRACK, STEP_LAY_TOKEN, STEP_CALC_REVENUE,
                    STEP_PAYOUT, STEP_BUY_TRAIN, STEP_TRADE_SHARES, STEP_FINAL };

    // side steps
    public static final int STEP_DISCARD_TRAINS = -2;

    protected boolean doneAllowed = false;

    protected TrainManager trainManager = gameManager.getTrainManager();

    public static String[] stepNames =
            new String[] { "LayTrack", "LayToken", "EnterRevenue", "Payout",
                    "BuyTrain", "TradeShares", "Final" };

    /**
	 * Constructor with no parameters, call the super Class (Round's) Constructor with no parameters
	 *
     */
    public OperatingRound(GameManagerI gameManager) {
		super (gameManager);

        if (players == null) {
            players = gameManager.getPlayers();
            numberOfPlayers = players.size();
        }

        operatingCompanyArray = super.getOperatingCompanies();
    }

    public void start(boolean operate) {

        thisOrNumber = gameManager.getORId();

        ReportBuffer.add(LocalText.getText("START_OR", thisOrNumber));

        int count = 0;
        for (PrivateCompanyI priv : companyManager.getAllPrivateCompanies()) {
            if (!priv.isClosed()) {
                if (((Portfolio)priv.getHolder()).getOwner().getClass() != Bank.class) {
                    CashHolder recipient = ((Portfolio)priv.getHolder()).getOwner();
                    int revenue = priv.getRevenue();
                    if (count++ == 0) ReportBuffer.add("");
                    ReportBuffer.add(LocalText.getText("ReceivesFor",
                            recipient.getName(),
                            Bank.format(revenue),
                            priv.getName()));
                    new CashMove(bank, recipient, revenue);
                }

            }
        }

        if (operate) {

            StringBuffer msg = new StringBuffer();
            for (PublicCompanyI company : operatingCompanyArray) {
                msg.append(",").append(company.getName());
            }
            if (msg.length() > 0) msg.deleteCharAt(0);
            log.info("Initial operating sequence is "+msg.toString());

            if (stepObject == null) {
                stepObject = new IntegerState("ORStep", -1);
                stepObject.addObserver(this);
            }

            if (operatingCompanyArray.length > 0) {

                if (setNextOperatingCompany(true)) {
                    setStep(STEP_INITIAL);
                    return;
                }
            }

        }

        // No operating companies yet: close the round.
        String text = LocalText.getText("ShortORExecuted");
        ReportBuffer.add(text);
        DisplayBuffer.add(text);
        finishRound();
    }


    /*----- METHODS THAT PROCESS PLAYER ACTIONS -----*/

    @Override
    public boolean process(PossibleAction action) {

        boolean result = false;
        doneAllowed = false;

        /*--- Common OR checks ---*/
        /* Check operating company */
        if (action instanceof PossibleORAction
            && !(action instanceof DiscardTrain)) {
            PublicCompanyI company = ((PossibleORAction) action).getCompany();
            if (company != operatingCompany) {
                DisplayBuffer.add(LocalText.getText("WrongCompany",
                        company.getName(),
                        operatingCompany.getName() ));
                return false;
            }
        }

        selectedAction = action;

        if (selectedAction instanceof LayTile) {

            result = layTile((LayTile) selectedAction);

        } else if (selectedAction instanceof LayBaseToken) {

            result = layBaseToken((LayBaseToken) selectedAction);

        } else if (selectedAction instanceof LayBonusToken) {

            result = layBonusToken((LayBonusToken) selectedAction);

        } else if (selectedAction instanceof BuyBonusToken) {

        	result = buyBonusToken((BuyBonusToken) selectedAction);

        } else if (selectedAction instanceof SetDividend) {

            result = setRevenueAndDividend((SetDividend) selectedAction);

        } else if (selectedAction instanceof BuyTrain) {

            result = buyTrain((BuyTrain) selectedAction);

        } else if (selectedAction instanceof DiscardTrain) {

            result = discardTrain((DiscardTrain) selectedAction);

        } else if (selectedAction instanceof BuyPrivate) {

            result = buyPrivate((BuyPrivate) selectedAction);

        } else if (selectedAction instanceof ReachDestinations) {

            result = reachDestinations ((ReachDestinations) selectedAction);

        } else if (selectedAction instanceof TakeLoans) {

            result = takeLoans((TakeLoans) selectedAction);

        } else if (selectedAction instanceof RepayLoans) {

            result = repayLoans((RepayLoans) selectedAction);

        } else if (selectedAction instanceof ExchangeTokens) {

            result = exchangeTokens ((ExchangeTokens)selectedAction);

        } else if (selectedAction instanceof NullAction) {

            NullAction nullAction = (NullAction) action;
            switch (nullAction.getMode()) {
            case NullAction.PASS:
            case NullAction.DONE:
                result = done();
                break;
            case NullAction.SKIP:
                skip();
                result = true;
                break;
            }

        } else if (processGameSpecificAction(action)) {

            result = true;

        } else {

            DisplayBuffer.add(LocalText.getText("UnexpectedAction",
                    selectedAction.toString()));
            return false;
        }

        return result;
    }

    /** Stub, to be overridden in game-specific subclasses. */
    public boolean processGameSpecificAction(PossibleAction action) {
        return false;
    }

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
            if (!companyName.equals(operatingCompany.getName())) {
                errMsg =
                        LocalText.getText("WrongCompany",
                                companyName,
                                operatingCompany.getName() );
                break;
            }
            // Must be correct step
            if (getStep() != STEP_LAY_TRACK) {
                errMsg = LocalText.getText("WrongActionNoTileLay");
                break;
            }

            if (tile == null) break;

            if (!currentPhase.isTileColourAllowed(tile.getColourName())) {
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
                            LocalText.getText(
                                    "TileMayNotBeLaidInHex",
                                    tile.getExternalId(),
                                    hex.getName() );
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

            // Sort out cost
            if (stl != null && stl.isFree()) {
                cost = 0;
            } else {
                cost = hex.getTileCost();
            }

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
            if (cost > operatingCompany.getCash()) {
                errMsg =
                        LocalText.getText("NotEnoughMoney",
                                companyName,
                                Bank.format(operatingCompany.getCash()),
                                Bank.format(cost) );
                break;
            }
            break;
        }
        if (errMsg != null) {
            DisplayBuffer.add(LocalText.getText("CannotLayTileOn",
                    companyName,
                    tile.getExternalId(),
                    hex.getName(),
                    Bank.format(cost),
                    errMsg ));
            return false;
        }

        /* End of validation, start of execution */
        moveStack.start(true);

        if (tile != null) {
            if (cost > 0)
                new CashMove(operatingCompany, bank, cost);
            operatingCompany.layTile(hex, tile, orientation, cost);

            if (cost > 0) {
                ReportBuffer.add(LocalText.getText("LaysTileAt",
                        companyName,
                        tile.getExternalId(),
                        hex.getName() ));
            } else {
                ReportBuffer.add(LocalText.getText("LaysTileAtFor",
                        companyName,
                        tile.getExternalId(),
                        hex.getName(),
                        Bank.format(cost) ));
            }
            hex.upgrade(action);

            // Was a special property used?
            if (stl != null) {
                stl.setExercised();
                currentSpecialTileLays.remove(action);
                log.debug("This was a special tile lay, "
                          + (extra ? "" : " not") + " extra");

            }
            if (!extra) {
                log.debug("This was a normal tile lay");
                registerNormalTileLay(tile);
            }

            setSpecialTileLays();
            log.debug("There are now " + currentSpecialTileLays.size()
                      + " special tile lay objects");
        }

        if (tile == null || currentNormalTileLays.isEmpty()
            && currentSpecialTileLays.isEmpty()) {
            nextStep();
        }

        return true;
    }

    protected boolean validateNormalTileLay(TileI tile) {
        return checkNormalTileLay(tile, false);
    }

    protected void registerNormalTileLay(TileI tile) {
        checkNormalTileLay(tile, true);
    }

    protected boolean checkNormalTileLay(TileI tile, boolean update) {

        if (tileLaysPerColour.isEmpty()) return false;
        String colour = tile.getColourName();

        Integer oldAllowedNumberObject = tileLaysPerColour.get(colour);
        if (oldAllowedNumberObject == null) return false;
        int oldAllowedNumber = oldAllowedNumberObject.intValue();
        if (oldAllowedNumber <= 0) return false;

        if (!update) return true;

        /*
         * We will assume that in all cases the following assertions hold: 1. If
         * the allowed number for the colour of the just laid tile reaches zero,
         * all normal tile lays have been consumed. 2. If any colour is laid, no
         * different colours may be laid. THIS MAY NOT BE TRUE FOR ALL GAMES!
         */
        if (oldAllowedNumber <= 1) {
            tileLaysPerColour.clear();
            log.debug("No more normal tile lays allowed");
            currentNormalTileLays.clear();
        } else {
            tileLaysPerColour.clear(); // Remove all other colours
            tileLaysPerColour.put(colour, new Integer(oldAllowedNumber - 1));
            log.debug((oldAllowedNumber - 1) + " more " + colour
                      + " tile lays allowed");
        }

        return true;
    }

    public boolean layBaseToken(LayBaseToken action) {

        String errMsg = null;
        int cost = 0;
        SpecialTokenLay stl = null;
        boolean extra = false;

        MapHex hex = action.getChosenHex();
        int station = action.getChosenStation();
        String companyName = operatingCompany.getName();

        // Dummy loop to enable a quick jump out.
        while (true) {

            // Checks
            // Must be correct step
            if (getStep() != STEP_LAY_TOKEN) {
                errMsg = LocalText.getText("WrongActionNoTokenLay");
                break;
            }

            if (operatingCompany.getNumberOfFreeBaseTokens() == 0) {
                errMsg = LocalText.getText("HasNoTokensLeft", companyName);
                break;
            }

            if (!hex.hasTokenSlotsLeft(station)) {
                errMsg = LocalText.getText("CityHasNoEmptySlots");
                break;
            }

            /*
             * TODO: the below condition holds for 1830. in some games, separate
             * cities on one tile may hold tokens of the same company; this case
             * is not yet covered.
             */
            if (hex.hasTokenOfCompany(operatingCompany)) {
                errMsg =
                        LocalText.getText("TileAlreadyHasToken",
                                hex.getName(),
                                companyName );
                break;
            }

            if (action != null) {
                List<MapHex> locations = action.getLocations();
                if (locations != null && locations.size() > 0
                    && !locations.contains(hex) && !locations.contains(null)) {
                    errMsg =
                            LocalText.getText("TokenLayingHexMismatch",
                                    hex.getName(),
                                    action.getLocationNameString() );
                    break;
                }
                stl = action.getSpecialProperty();
                if (stl != null) extra = stl.isExtra();
            }

            cost = operatingCompany.getBaseTokenLayCost();
            if (stl != null && stl.isFree()) cost = 0;

            // Does the company have the money?
            if (cost > operatingCompany.getCash()) {
                errMsg = LocalText.getText("NotEnoughMoney",
                        companyName,
                        Bank.format(operatingCompany.getCash()),
                        Bank.format(cost));
                 break;
            }
            break;
        }
        if (errMsg != null) {
            DisplayBuffer.add(LocalText.getText("CannotLayBaseTokenOn",
                    companyName,
                    hex.getName(),
                    Bank.format(cost),
                    errMsg ));
            return false;
        }

        /* End of validation, start of execution */
        moveStack.start(true);

        if (hex.layBaseToken(operatingCompany, station)) {
            /* TODO: the false return value must be impossible. */

            operatingCompany.layBaseToken(hex, cost);

            if (cost > 0) {
                new CashMove(operatingCompany, bank, cost);
                ReportBuffer.add(LocalText.getText("LAYS_TOKEN_ON",
                        companyName,
                        hex.getName(),
                        Bank.format(cost) ));
            } else {
                ReportBuffer.add(LocalText.getText("LAYS_FREE_TOKEN_ON",
                        companyName,
                        hex.getName() ));
            }

            // Was a special property used?
            if (stl != null) {
                stl.setExercised();
                currentSpecialTokenLays.remove(action);
                log.debug("This was a special token lay, "
                          + (extra ? "" : " not") + " extra");

            }
            if (!extra) {
                currentNormalTokenLays.clear();
                log.debug("This was a normal token lay");
            }
            if (currentNormalTokenLays.isEmpty()) {
                log.debug("No more normal token lays are allowed");
            } else {
                log.debug("A normal token lay is still allowed");
            }
            setSpecialTokenLays();
            log.debug("There are now " + currentSpecialTokenLays.size()
                      + " special token lay objects");
            if (currentNormalTokenLays.isEmpty()
                && currentSpecialTokenLays.isEmpty()) {
                nextStep();
            }

        }

        return true;
    }

    public boolean layBonusToken(LayBonusToken action) {

        String errMsg = null;
        int cost = 0;
        SpecialTokenLay stl = null;
        boolean extra = false;

        MapHex hex = action.getChosenHex();
        BonusToken token = action.getToken();

        // Dummy loop to enable a quick jump out.
        while (true) {

            // Checks
            MapHex location = action.getChosenHex();
            if (location != hex) {
                errMsg =
                        LocalText.getText("TokenLayingHexMismatch",
                                hex.getName(),
                                location.getName() );
                break;
            }
            stl = action.getSpecialProperty();
            if (stl != null) extra = stl.isExtra();

            cost = 0; // Let's assume for now that bonus tokens are always
            // free
            if (stl != null && stl.isFree()) cost = 0;

            // Does the company have the money?
            if (cost > operatingCompany.getCash()) {
                errMsg =
                        LocalText.getText("NotEnoughMoney",
                                operatingCompany.getName());
                break;
            }
            break;
        }
        if (errMsg != null) {
            DisplayBuffer.add(LocalText.getText("CannotLayBonusTokenOn",
                    token.getName(),
                    hex.getName(),
                    Bank.format(cost),
                    errMsg ));
            return false;
        }

        /* End of validation, start of execution */
        moveStack.start(true);

        if (hex.layBonusToken(token, gameManager.getPhaseManager())) {
            /* TODO: the false return value must be impossible. */

       		operatingCompany.addBonus(new Bonus(operatingCompany,
        				token.getName(),
        				token.getValue(), Collections.singletonList(hex)));
            token.setUser(operatingCompany);

            ReportBuffer.add(LocalText.getText("LaysBonusTokenOn",
                    operatingCompany.getName(),
                    token.getName(),
                    Bank.format(token.getValue()),
                    hex.getName() ));

            // Was a special property used?
            if (stl != null) {
                stl.setExercised();
                currentSpecialTokenLays.remove(action);
                log.debug("This was a special token lay, "
                          + (extra ? "" : " not") + " extra");

            }

        }

        return true;
    }

    public boolean buyBonusToken(BuyBonusToken action) {

        String errMsg = null;
        int cost;
        SellBonusToken sbt = null;
        CashHolder seller = null;

        // Dummy loop to enable a quick jump out.
        while (true) {

            // Checks
            sbt = action.getSpecialProperty();
            cost = sbt.getPrice();
            seller = sbt.getSeller();

            // Does the company have the money?
            if (cost > operatingCompany.getCash()) {
                errMsg =
                        LocalText.getText("NotEnoughMoney",
                                operatingCompany.getName(),
                                Bank.format(operatingCompany.getCash()),
                                Bank.format(cost));
                break;
            }
            break;
        }
        if (errMsg != null) {
            DisplayBuffer.add(LocalText.getText("CannotBuyBonusToken",
            		operatingCompany.getName(),
                    sbt.getName(),
                    seller.getName(),
                    Bank.format(cost),
                    errMsg ));
            return false;
        }

        /* End of validation, start of execution */
        moveStack.start(true);

        new CashMove (operatingCompany, seller, cost);
  		operatingCompany.addBonus(new Bonus(operatingCompany,
    				sbt.getName(),
    				sbt.getValue(),
                    sbt.getLocations()));

        ReportBuffer.add(LocalText.getText("BuysBonusTokenFrom",
                operatingCompany.getName(),
                sbt.getName(),
                Bank.format(sbt.getValue()),
                seller.getName(),
                Bank.format(sbt.getValue())));

        sbt.setExercised();

        return true;
    }
    public boolean setRevenueAndDividend(SetDividend action) {

        String errMsg = validateSetRevenueAndDividend (action);

        if (errMsg != null) {
            DisplayBuffer.add(LocalText.getText(
                    "CannotProcessRevenue",
                    Bank.format (action.getActualRevenue()),
                    action.getCompanyName(),
                    errMsg
            ));
            return false;
        }

        moveStack.start(true);

        int remainingAmount = checkForDeductions (action);
        if (remainingAmount < 0) {
            // A share selling round will be run to raise cash to pay debts
            return true;
        }

        executeSetRevenueAndDividend (action);

        return true;

    }

    protected String validateSetRevenueAndDividend (SetDividend action) {

        String errMsg = null;
        PublicCompanyI company;
        String companyName;
        int amount = 0;
        int revenueAllocation = -1;

        // Dummy loop to enable a quick jump out.
        while (true) {

            // Checks
            // Must be correct company.
            company = action.getCompany();
            companyName = company.getName();
            if (company != operatingCompany) {
                errMsg =
                        LocalText.getText("WrongCompany",
                                companyName,
                                operatingCompany.getName() );
                break;
            }
            // Must be correct step
            if (getStep() != STEP_CALC_REVENUE) {
                errMsg = LocalText.getText("WrongActionNoRevenue");
                break;
            }

            // Amount must be non-negative multiple of 10
            amount = action.getActualRevenue();
            if (amount < 0) {
                errMsg =
                        LocalText.getText("NegativeAmountNotAllowed",
                                String.valueOf(amount));
                break;
            }
            if (amount % 10 != 0) {
                errMsg =
                        LocalText.getText("AmountMustBeMultipleOf10",
                                String.valueOf(amount));
                break;
            }

            // Check chosen revenue distribution
            if (amount > 0) {
                // Check the allocation type index (see SetDividend for values)
                revenueAllocation = action.getRevenueAllocation();
                if (revenueAllocation < 0
                    || revenueAllocation >= SetDividend.NUM_OPTIONS) {
                    errMsg =
                            LocalText.getText("InvalidAllocationTypeIndex",
                                    String.valueOf(revenueAllocation));
                    break;
                }

                // Validate the chosen allocation type
                int[] allowedAllocations =
                        ((SetDividend) selectedAction).getAllowedAllocations();
                boolean valid = false;
                for (int aa : allowedAllocations) {
                    if (revenueAllocation == aa) {
                        valid = true;
                        break;
                    }
                }
                if (!valid) {
                    errMsg =
                            LocalText.getText(SetDividend.getAllocationNameKey(revenueAllocation));
                    break;
                }
            } else {
                // If there is no revenue, use withhold.
                action.setRevenueAllocation(SetDividend.WITHHOLD);
            }

            ReportBuffer.add(LocalText.getText("CompanyRevenue",
                    action.getCompanyName(),
                    Bank.format(amount)));
            if (amount == 0 && operatingCompany.getNumberOfTrains() == 0) {
                DisplayBuffer.add(LocalText.getText("RevenueWithNoTrains",
                            operatingCompany.getName(),
                            Bank.format(0) ));
            }

            break;
        }

        return errMsg;
    }

    protected void executeSetRevenueAndDividend (SetDividend action) {

        int amount = action.getActualRevenue();
        int revenueAllocation = action.getRevenueAllocation();

        operatingCompany.setLastRevenue(amount);
        operatingCompany.setLastRevenueAllocation(revenueAllocation);

        if (amount == 0 && operatingCompany.getNumberOfTrains() == 0) {
            DisplayBuffer.add(LocalText.getText("RevenueWithNoTrains",
                        operatingCompany.getName(),
                        Bank.format(0) ));
        }

        // Pay any debts from treasury, revenue and/or president's cash
        // The remaining dividend may be less that the original income
        amount = executeDeductions (action);

        if (amount == 0) {

            ReportBuffer.add(LocalText.getText("CompanyDoesNotPayDividend",
                    operatingCompany.getName()));
            operatingCompany.withhold(amount);

        } else if (revenueAllocation == SetDividend.PAYOUT) {

            ReportBuffer.add(LocalText.getText("CompanyPaysOutFull",
                    operatingCompany.getName(), Bank.format(amount) ));

            operatingCompany.payout(amount);

        } else if (revenueAllocation == SetDividend.SPLIT) {

            ReportBuffer.add(LocalText.getText("CompanySplits",
                    operatingCompany.getName(), Bank.format(amount) ));

            operatingCompany.splitRevenue(amount);

        } else if (revenueAllocation == SetDividend.WITHHOLD) {

            ReportBuffer.add(LocalText.getText("CompanyWithholds",
                    operatingCompany.getName(),
                    Bank.format(amount) ));

            operatingCompany.withhold(amount);

        }

        // Rust any obsolete trains
        operatingCompany.getPortfolio().rustObsoleteTrains();

        // We have done the payout step, so continue from there
        nextStep(STEP_PAYOUT);
    }

    /** Default version, to be overridden if need be */
    protected int checkForDeductions (SetDividend action) {
        return action.getActualRevenue();
    }

    /** Default version, to be overridden if need be */
    protected int executeDeductions (SetDividend action) {
        return action.getActualRevenue();
    }

    /**
     * Internal method: change the OR state to the next step. If the currently
     * Operating Company is done, notify this.
     *
     * @param company The current company.
     */
    protected void nextStep() {
        nextStep(getStep());
    }

    /** Take the next step after a given one (see nextStep()) */
    protected void nextStep(int step) {
        // Cycle through the steps until we reach one where a user action is
        // expected.
        int stepIndex;
        for (stepIndex = 0; stepIndex < steps.length; stepIndex++) {
            if (steps[stepIndex] == step) break;
        }
        while (++stepIndex < steps.length) {
            step = steps[stepIndex];
            log.debug("Step " + stepNames[step]);

            if (step == STEP_LAY_TOKEN
                && operatingCompany.getNumberOfFreeBaseTokens() == 0) {
                continue;
            }

            if (step == STEP_CALC_REVENUE) {

                if (!operatingCompany.canRunTrains()) {
                    // No trains, then the revenue is zero.
                    executeSetRevenueAndDividend (
                            new SetDividend (0, false, new int[] {SetDividend.WITHHOLD}));
                    // TODO: This probably does not handle share selling correctly
                    continue;
                }
            }

            if (step == STEP_PAYOUT) {
                // This step is now obsolete
               continue;
            }

            if (step == STEP_TRADE_SHARES) {

                // Is company allowed to trade trasury shares?
                if (!operatingCompany.mayTradeShares()
                    || !operatingCompany.hasOperated()) {
                    continue;
                }

                gameManager.startTreasuryShareTradingRound();

            }

            if (!gameSpecificNextStep (step)) continue;

            // No reason found to skip this step
            break;
        }

        if (step == STEP_FINAL) {
            finishTurn();
        } else {
            setStep(step);
        }

    }

    /** Stub, can be overridden in subclasses to check for extra steps */
    protected boolean gameSpecificNextStep (int step) {
        return true;
    }

    protected void initTurn() {
        log.debug("Starting turn of "+operatingCompany.getName());
        ReportBuffer.add(LocalText.getText("CompanyOperates",
        		operatingCompany.getName(),
        		operatingCompany.getPresident().getName()));
        setCurrentPlayer(operatingCompany.getPresident());
        operatingCompany.initTurn();
        trainsBoughtThisTurn.clear();
    }

    /**
     * This method is only called at the start of each step (unlike
     * updateStatus(), which is called after each user action)
     */
    protected void prepareStep() {
        int step = stepObject.intValue();

        currentPhase = gameManager.getCurrentPhase();

        if (step == STEP_LAY_TRACK) {
            getNormalTileLays();
        } else if (step == STEP_LAY_TOKEN) {

        } else {
            currentSpecialProperties = null;
        }

    }

    protected <T extends SpecialPropertyI> List<T> getSpecialProperties(
            Class<T> clazz) {
        List<T> specialProperties = new ArrayList<T>();
        specialProperties.addAll(operatingCompany.getPortfolio().getSpecialProperties(
                clazz, false));
        specialProperties.addAll(operatingCompany.getPresident().getPortfolio().getSpecialProperties(
                clazz, false));
        return specialProperties;
    }

    /**
     * Create a List of allowed normal tile lays (see LayTile class). This
     * method should be called only once per company turn in an OR: at the start
     * of the tile laying step.
     */
    protected void getNormalTileLays() {

        tileLaysPerColour =
                new HashMap<String, Integer>(currentPhase.getTileColours()); // Clone
        // it.
        int allowedNumber;
        for (String colour : tileLaysPerColour.keySet()) {
            allowedNumber = operatingCompany.getNumberOfTileLays(colour);
            // Replace the null map value with the allowed number of lays
            tileLaysPerColour.put(colour, new Integer(allowedNumber));
        }
    }

    protected void setNormalTileLays() {

        /* Normal tile lays */
        currentNormalTileLays.clear();
        if (!tileLaysPerColour.isEmpty()) {
            currentNormalTileLays.add(new LayTile(tileLaysPerColour));
        }

    }

    /**
     * Create a List of allowed special tile lays (see LayTile class). This
     * method should be called before each user action in the tile laying step.
     */
    protected void setSpecialTileLays() {

        /* Special-property tile lays */
        currentSpecialTileLays.clear();
        /*
         * In 1835, this only applies to major companies. TODO: For now,
         * hardcode this, but it must become configurable later.
         */
        if (operatingCompany.getType().getName().equals("Minor")) return;

        for (SpecialTileLay stl : getSpecialProperties(SpecialTileLay.class)) {
            if (stl.isExtra() || !currentNormalTileLays.isEmpty()) {
                /*
                 * If the special tile lay is not extra, it is only allowed if
                 * normal tile lays are also (still) allowed
                 */
                currentSpecialTileLays.add(new LayTile(stl));
            }
        }
    }

    protected void setNormalTokenLays() {

        /* Normal token lays */
        currentNormalTokenLays.clear();

        /* For now, we allow one token of the currently operating company */
        if (operatingCompany.getNumberOfFreeBaseTokens() > 0) {
            currentNormalTokenLays.add(new LayBaseToken((List<MapHex>) null));
        }

    }

    /**
     * Create a List of allowed special token lays (see LayToken class). This
     * method should be called before each user action in the base token laying
     * step. TODO: Token preparation is practically identical to Tile
     * preparation, perhaps the two can be merged to one generic procedure.
     */
    protected void setSpecialTokenLays() {

        /* Special-property tile lays */
        currentSpecialTokenLays.clear();

        /*
         * In 1835, this only applies to major companies. TODO: For now,
         * hardcode this, but it must become configurable later.
         */
        if (operatingCompany.getType().getName().equals("Minor")) return;

        for (SpecialTokenLay stl : getSpecialProperties(SpecialTokenLay.class)) {
            log.debug("Spec.prop:" + stl);
            if (stl.getTokenClass().equals(BaseToken.class)
                && (stl.isExtra() || !currentNormalTokenLays.isEmpty())) {
                /*
                 * If the special tile lay is not extra, it is only allowed if
                 * normal tile lays are also (still) allowed
                 */
                currentSpecialTokenLays.add(new LayBaseToken(stl));
            }
        }
    }

    /**
     * TODO Should be merged with setSpecialTokenLays() in the future.
     * Assumptions: 1. Bonus tokens can be laid anytime during the OR. 2. Bonus
     * token laying is always extra. TODO This assumptions will be made
     * configurable conditions.
     */
    protected void setBonusTokenLays() {

        for (SpecialTokenLay stl : getSpecialProperties(SpecialTokenLay.class)) {
            if (stl.getTokenClass().equals(BonusToken.class)) {
                possibleActions.add(new LayBonusToken(stl,
                        (BonusToken) stl.getToken()));
            }
        }
    }

    /** Stub, can be overridden by subclasses */
    protected void setGameSpecificPossibleActions() {

    }

    @Override
    public List<SpecialPropertyI> getSpecialProperties() {
        return currentSpecialProperties;
    }

    public void skip() {
        log.debug("Skip step " + stepObject.intValue());
        moveStack.start(true);
        nextStep();
    }

    /**
     * The current Company is done operating.
     *
     * @param company Name of the company that finished operating.
     * @return False if an error is found.
     */
    public boolean done() {
        String errMsg = null;

         if (operatingCompany.getPortfolio().getNumberOfTrains() == 0
            && operatingCompany.mustOwnATrain()) {
            // FIXME: Need to check for valid route before throwing an
            // error.
        	/* Check TEMPORARILY disabled
            errMsg =
                    LocalText.getText("CompanyMustOwnATrain",
                            operatingCompany.getName());
            setStep(STEP_BUY_TRAIN);
            DisplayBuffer.add(errMsg);
            return false;
            */
        }

        moveStack.start(false);

        nextStep();

        if (getStep() == STEP_FINAL) {
            finishTurn();
        }

        return true;
    }

    protected void finishTurn() {

        operatingCompany.setOperated();
        companiesOperatedThisRound.add(operatingCompany);

        // Check if any privates must be closed
        // (now only applies to 1856 W&SR)
        for (PrivateCompanyI priv : operatingCompany.getPortfolio().getPrivateCompanies()) {
            priv.checkClosingIfExercised(true);
        }

        if (!finishTurnSpecials()) return;

        if (setNextOperatingCompany(false)) {
            setStep(STEP_INITIAL);
        } else {
            finishOR();
        }
    }

    /** Stub, may be overridden in subclasses
     * Return value:
     * TRUE = normal turn end;
     * FALSE = return immediately from finishTurn().
     */
    protected boolean finishTurnSpecials () {
    	return true;
    }

    protected boolean setNextOperatingCompany(boolean initial) {


        if (operatingCompanyIndexObject == null) {
            operatingCompanyIndexObject =
                    new IntegerState("OperatingCompanyIndex");
        }
        if (initial) {
            operatingCompanyIndexObject.set(0);
        } else {
            operatingCompanyIndexObject.add(1);
        }

        operatingCompanyIndex = operatingCompanyIndexObject.intValue();

        if (operatingCompanyIndex >= operatingCompanyArray.length) {
            operatingCompany = null;
            return false;
        } else {
            operatingCompany = operatingCompanyArray[operatingCompanyIndex];
            return true;
        }
    }

    protected void finishOR() {

        // Check if any privates must be closed
        // (now only applies to 1856 W&SR)
        for (PrivateCompanyI priv : gameManager.getAllPrivateCompanies()) {
            priv.checkClosingIfExercised(true);
        }

        // OR done. Inform GameManager.
        ReportBuffer.add(LocalText.getText("EndOfOperatingRound", thisOrNumber));
        finishRound();
    }

    public boolean buyTrain(BuyTrain action) {

        TrainI train = action.getTrain();
        PublicCompanyI company = action.getCompany();
        String companyName = company.getName();
        TrainI exchangedTrain = action.getExchangedTrain();
        SpecialTrainBuy stb = null;

        String errMsg = null;
        int presidentCash = action.getPresidentCashToAdd();
        boolean presidentMustSellShares = false;
        int price = action.getPricePaid();
        int actualPresidentCash = 0;
        Player currentPlayer = operatingCompany.getPresident();

        // Dummy loop to enable a quick jump out.
        while (true) {
            // Checks
            // Must be correct step
            if (getStep() != STEP_BUY_TRAIN) {
                errMsg = LocalText.getText("WrongActionNoTrainBuyingCost");
                break;
            }

            if (train == null) {
                errMsg = LocalText.getText("NoTrainSpecified");
                break;
            }

            // Amount must be non-negative
            if (price < 0) {
                errMsg =
                        LocalText.getText("NegativeAmountNotAllowed",
                                Bank.format(price));
                break;
            }

            // Fixed price must be honoured
            int fixedPrice = action.getFixedCost();
            if (fixedPrice != 0 && fixedPrice != price) {
                errMsg = LocalText.getText("FixedPriceNotPaid",
                        Bank.format(price),
                        Bank.format(fixedPrice));
            }

            // Does the company have room for another train?
            int trainLimit = operatingCompany.getCurrentTrainLimit();
            if (!isBelowTrainLimit() && !action.isForcedExchange()) {
                errMsg =
                        LocalText.getText("WouldExceedTrainLimit",
                                String.valueOf(trainLimit));
                break;
            }

            /* Check if this is an emergency buy */
            if (action.mustPresidentAddCash()) {
                // From the Bank
                presidentCash = action.getPresidentCashToAdd();
                if (currentPlayer.getCash() >= presidentCash) {
                    actualPresidentCash = presidentCash;
                } else {
                    presidentMustSellShares = true;
                    cashToBeRaisedByPresident =
                            presidentCash - currentPlayer.getCash();
                }
            } else if (action.mayPresidentAddCash()) {
                // From another company
                presidentCash = price - operatingCompany.getCash();
                if (presidentCash > action.getPresidentCashToAdd()) {
                    errMsg =
                            LocalText.getText("PresidentMayNotAddMoreThan",
                                    Bank.format(action.getPresidentCashToAdd()));
                    break;
                } else if (currentPlayer.getCash() >= presidentCash) {
                     actualPresidentCash = presidentCash;
                } else {
                    presidentMustSellShares = true;
                    cashToBeRaisedByPresident =
                            presidentCash - currentPlayer.getCash();
                }

            } else {
                // No forced buy - does the company have the money?
                if (price > operatingCompany.getCash()) {
                    errMsg =
                            LocalText.getText("NotEnoughMoney",
                                    companyName,
                                    Bank.format(operatingCompany.getCash()),
                                    Bank.format(price) );
                    break;
                }
            }

            stb = action.getSpecialProperty();
            // TODO Note: this is not yet validated

            break;
        }

        if (errMsg != null) {
            DisplayBuffer.add(LocalText.getText("CannotBuyTrainFor",
                    companyName,
                    train.getName(),
                    Bank.format(price),
                    errMsg ));
            return false;
        }

        /* End of validation, start of execution */
        moveStack.start(true);
        PhaseI previousPhase = getCurrentPhase();

        if (presidentMustSellShares) {
            savedAction = action;

            gameManager.startShareSellingRound(operatingCompany.getPresident(),
                    cashToBeRaisedByPresident, operatingCompany);

            return true;
        }

        if (actualPresidentCash > 0) {
            new CashMove(currentPlayer, operatingCompany, presidentCash);
        }

        Portfolio oldHolder = train.getHolder();

        if (exchangedTrain != null) {
            TrainI oldTrain =
                    operatingCompany.getPortfolio().getTrainOfType(
                            exchangedTrain.getType());
            pool.buyTrain(oldTrain, 0);
            ReportBuffer.add(LocalText.getText("ExchangesTrain",
                    companyName,
                    exchangedTrain.getName(),
                    train.getName(),
                    oldHolder.getName(),
                    Bank.format(price) ));
        } else if (stb == null) {
            ReportBuffer.add(LocalText.getText("BuysTrain",
                    companyName,
                    train.getName(),
                    oldHolder.getName(),
                    Bank.format(price) ));
        } else {
            ReportBuffer.add(LocalText.getText("BuysTrainUsingSP",
                    companyName,
                    train.getName(),
                    oldHolder.getName(),
                    Bank.format(price),
                    stb.getCompany().getName() ));
        }

        operatingCompany.buyTrain(train, price);
        if (oldHolder == ipo) {
            train.getType().addToBoughtFromIPO();
            // Clone the train if infinitely available
            if (train.getType().hasInfiniteAmount()) {
                ipo.addTrain(train.getType().cloneTrain());
            }

        }
        if (oldHolder.getOwner() instanceof Bank) {
            trainsBoughtThisTurn.add(train.getType());
        }

        if (stb != null) {
            stb.setExercised();
            log.debug("This was a special train buy");
        }

        // Check if the phase has changed.
        gameManager.getTrainManager().checkTrainAvailability(train, oldHolder);
        currentPhase = getCurrentPhase();

        // Check if any companies must discard trains
        if (currentPhase != previousPhase && checkForExcessTrains()) {
            stepObject.set(STEP_DISCARD_TRAINS);
        }

        return true;
    }

    public boolean checkForExcessTrains() {

        excessTrainCompanies = new HashMap<Player, List<PublicCompanyI>>();
        Player player;
        for (PublicCompanyI comp : operatingCompanyArray) {
            if (comp.getPortfolio().getNumberOfTrains() > comp.getTrainLimit(currentPhase.getIndex())) {
                player = comp.getPresident();
                if (!excessTrainCompanies.containsKey(player)) {
                    excessTrainCompanies.put(player,
                            new ArrayList<PublicCompanyI>(2));
                }
                excessTrainCompanies.get(player).add(comp);
            }

        }
        return !excessTrainCompanies.isEmpty();
    }

    @Override
	public void resume() {

        if (savedAction instanceof BuyTrain) {
            buyTrain ((BuyTrain)savedAction);
        } else if (savedAction instanceof SetDividend) {
            executeSetRevenueAndDividend ((SetDividend) savedAction);
        } else if (savedAction instanceof RepayLoans) {
            executeRepayLoans ((RepayLoans) savedAction);
        }
        savedAction = null;
        wasInterrupted.set(true);
    }

    public boolean discardTrain(DiscardTrain action) {

        TrainI train = action.getDiscardedTrain();
        PublicCompanyI company = action.getCompany();
        String companyName = company.getName();

        String errMsg = null;

        // Dummy loop to enable a quick jump out.
        while (true) {
            // Checks
            // Must be correct step
            if (getStep() != STEP_BUY_TRAIN && getStep() != STEP_DISCARD_TRAINS) {
                errMsg = LocalText.getText("WrongActionNoDiscardTrain");
                break;
            }

            if (train == null && action.isForced()) {
                errMsg = LocalText.getText("NoTrainSpecified");
                break;
            }

            // Does the company own such a train?

            if (!company.getPortfolio().getTrainList().contains(train)) {
                errMsg =
                        LocalText.getText("CompanyDoesNotOwnTrain",
                                company.getName(),
                                train.getName() );
                break;
            }

            break;
        }
        if (errMsg != null) {
            DisplayBuffer.add(LocalText.getText("CannotDiscardTrain",
                    companyName,
                    (train != null ?train.getName() : "?"),
                    errMsg ));
            return false;
        }

        /* End of validation, start of execution */
        moveStack.start(true);
        //
        if (action.isForced()) moveStack.linkToPreviousMoveSet();

        pool.buyTrain(train, 0);
        ReportBuffer.add(LocalText.getText("CompanyDiscardsTrain",
                companyName,
                train.getName() ));

        // Check if any more companies must discard trains,
        // otherwise continue train buying
        if (!checkForExcessTrains()) {
            stepObject.set(STEP_BUY_TRAIN);
        }

        //setPossibleActions();

        return true;
    }

    public boolean buyPrivate(BuyPrivate action) {

        String errMsg = null;
        PublicCompanyI publicCompany = action.getCompany();
        String publicCompanyName = publicCompany.getName();
        PrivateCompanyI privateCompany = action.getPrivateCompany();
        String privateCompanyName = privateCompany.getName();
        int price = action.getPrice();
        CashHolder owner = null;
        Player player = null;
        int basePrice;

        // Dummy loop to enable a quick jump out.
        while (true) {

            // Checks
            // Does private exist?
            if ((privateCompany =
                    companyManager.getPrivateCompany(
                            privateCompanyName)) == null) {
                errMsg =
                        LocalText.getText("PrivateDoesNotExist",
                                privateCompanyName);
                break;
            }
            // Is private still open?
            if (privateCompany.isClosed()) {
                errMsg =
                        LocalText.getText("PrivateIsAlreadyClosed",
                                privateCompanyName);
                break;
            }
            // Is private owned by a player?
            owner = privateCompany.getPortfolio().getOwner();
            if (!(owner instanceof Player)) {
                errMsg =
                        LocalText.getText("PrivateIsNotOwnedByAPlayer",
                                privateCompanyName);
                break;
            }
            player = (Player) owner;
            basePrice = privateCompany.getBasePrice();

            // Is private buying allowed?
            if (!currentPhase.isPrivateSellingAllowed()) {
                errMsg = LocalText.getText("PrivateBuyingIsNotAllowed");
                break;
            }

            // Price must be in the allowed range
            if (price < basePrice
                        * operatingCompany.getLowerPrivatePriceFactor()) {
                errMsg =
                        LocalText.getText("PriceBelowLowerLimit",
                                Bank.format(price),
                                Bank.format((int) (basePrice * operatingCompany.getLowerPrivatePriceFactor())),
                                privateCompanyName );
                break;
            }
            if (price > basePrice
                        * operatingCompany.getUpperPrivatePriceFactor()) {
                errMsg =
                        LocalText.getText("PriceAboveUpperLimit",
                                Bank.format(price),
                                Bank.format((int) (basePrice * operatingCompany.getUpperPrivatePriceFactor())),
                                privateCompanyName );
                break;
            }
            // Does the company have the money?
            if (price > operatingCompany.getCash()) {
                errMsg =
                        LocalText.getText("NotEnoughMoney",
                                publicCompanyName,
                                Bank.format(operatingCompany.getCash()),
                                Bank.format(price) );
                break;
            }
            break;
        }
        if (errMsg != null) {
            if (owner != null) {
                DisplayBuffer.add(LocalText.getText("CannotBuyPrivateFromFor",
                        publicCompanyName,
                        privateCompanyName,
                        owner.getName(),
                        Bank.format(price),
                        errMsg ));
            } else {
                DisplayBuffer.add(LocalText.getText("CannotBuyPrivateFor",
                        publicCompanyName,
                        privateCompanyName,
                        Bank.format(price),
                        errMsg ));
            }
            return false;
        }

        moveStack.start(true);

        operatingCompany.buyPrivate(privateCompany, player.getPortfolio(),
                price);

        return true;

    }

    public boolean reachDestinations (ReachDestinations action) {

        List<PublicCompanyI> destinedCompanies
            = action.getReachedCompanies();
        if (destinedCompanies != null) {
            for (PublicCompanyI company : destinedCompanies) {
                if (company.hasDestination()
                        && !company.hasReachedDestination()) {
                    if (!moveStack.isOpen()) moveStack.start(true);
                    company.setReachedDestination(true);
                    ReportBuffer.add(LocalText.getText("DestinationReached",
                        company.getName(),
                        company.getDestinationHex().getName()
                    ));
                    // Process any consequences of reaching a destination
                    // (default none)
                    reachDestination (company);
                }
            }
        }
        return true;
    }

    protected boolean takeLoans (TakeLoans action) {

        String errMsg = validateTakeLoans (action);

        if (errMsg != null) {
            DisplayBuffer.add(LocalText.getText("CannotTakeLoans",
                        action.getCompanyName(),
                        action.getNumberTaken(),
                        Bank.format(action.getPrice()),
                        errMsg));

            return false;
        }

        moveStack.start(true);

        executeTakeLoans (action);

        return true;

    }

    protected String validateTakeLoans (TakeLoans action) {

        String errMsg = null;
        PublicCompanyI company = action.getCompany();
        String companyName = company.getName();
        int number = action.getNumberTaken();

        // Dummy loop to enable a quick jump out.
        while (true) {

            // Checks
            // Is company operating?
            if (company != operatingCompany) {
                errMsg =
                        LocalText.getText("WrongCompany",
                                companyName,
                                action.getCompanyName());
                break;
            }
            // Does company allow any loans?
            if (company.getMaxNumberOfLoans() == 0) {
                errMsg = LocalText.getText("LoansNotAllowed",
                        companyName);
                break;
            }
            // Does the company exceed the maximum number of loans?
            if (company.getMaxNumberOfLoans() > 0
                    && company.getCurrentNumberOfLoans() + number >
                        company.getMaxNumberOfLoans()) {
                errMsg =
                        LocalText.getText("MoreLoansNotAllowed",
                                companyName,
                                company.getMaxNumberOfLoans());
                break;
            }
            break;
        }

        return errMsg;
    }

    protected void executeTakeLoans (TakeLoans action) {

        int number = action.getNumberTaken();
        int amount = calculateLoanAmount (number);
        operatingCompany.addLoans(number);
        new CashMove (bank, operatingCompany, amount);
        if (number == 1) {
            ReportBuffer.add(LocalText.getText("CompanyTakesLoan",
                operatingCompany.getName(),
                Bank.format(operatingCompany.getValuePerLoan()),
                Bank.format(amount)
            ));
        } else {
            ReportBuffer.add(LocalText.getText("CompanyTakesLoans",
                    operatingCompany.getName(),
                    number,
                    Bank.format(operatingCompany.getValuePerLoan()),
                    Bank.format(amount)
            ));
        }

        if (operatingCompany.getMaxLoansPerRound() > 0) {
            int oldLoansThisRound = 0;
            if (loansThisRound == null) {
                loansThisRound = new HashMap<PublicCompanyI, Integer>();
            } else if (loansThisRound.containsKey(operatingCompany)){
                oldLoansThisRound = loansThisRound.get(operatingCompany);
            }
            new MapChange<PublicCompanyI, Integer> (loansThisRound,
                    operatingCompany,
                    new Integer (oldLoansThisRound + number));
        }
    }

    /** Stub for applying any follow-up actions when
     * a company reaches it destinations.
     * Default version: no actions.
     * @param company
     */
    protected void reachDestination (PublicCompanyI company) {

    }

    protected boolean repayLoans (RepayLoans action) {

        String errMsg = validateRepayLoans (action);

        if (errMsg != null) {
            DisplayBuffer.add(LocalText.getText("CannotRepayLoans",
                        action.getCompanyName(),
                        action.getNumberRepaid(),
                        Bank.format(action.getPrice()),
                        errMsg));

            return false;
        }

        int repayment = action.getNumberRepaid() * operatingCompany.getValuePerLoan();
        if (repayment > 0 && repayment > operatingCompany.getCash()) {
            // President must contribute
            int remainder = repayment - operatingCompany.getCash();
            Player president = operatingCompany.getPresident();
            int presCash = president.getCash();
            if (remainder > presCash) {
                // Start a share selling round
                cashToBeRaisedByPresident = remainder - presCash;
                log.info("A share selling round must be started as the president cannot pay $"
                        + remainder + " loan repayment");
                log.info("President has $"+presCash+", so $"+cashToBeRaisedByPresident+" must be added");
                savedAction = action;
                moveStack.start(true);
                gameManager.startShareSellingRound(operatingCompany.getPresident(),
                        cashToBeRaisedByPresident, operatingCompany);
                return true;
            }
        }

        moveStack.start(true);

        if (repayment > 0) executeRepayLoans (action);

        doneAllowed = true;

        return true;
    }

    protected String validateRepayLoans (RepayLoans action) {

        String errMsg = null;

        return errMsg;
    }

    protected void executeRepayLoans (RepayLoans action) {

        int number = action.getNumberRepaid();
        int payment;
        int remainder = 0;

        operatingCompany.addLoans(-number);
        int amount = number * operatingCompany.getValuePerLoan();
        payment = Math.min(amount, operatingCompany.getCash());
        remainder = amount - payment;
        if (payment > 0) {
            new CashMove (operatingCompany, bank, payment);
            ReportBuffer.add (LocalText.getText("CompanyRepaysLoans",
                operatingCompany.getName(),
                Bank.format(payment),
                Bank.format(amount),
                number,
                Bank.format(operatingCompany.getValuePerLoan())));
        }
        if (remainder > 0) {
            Player president = operatingCompany.getPresident();
            if (president.getCash() >= remainder) {
                payment = remainder;
                new CashMove (president, bank, payment);
                ReportBuffer.add (LocalText.getText("CompanyRepaysLoansWithPresCash",
                        operatingCompany.getName(),
                        Bank.format(payment),
                        Bank.format(amount),
                        number,
                        Bank.format(operatingCompany.getValuePerLoan()),
                        president.getName()));
            }
        }
    }

    protected int calculateLoanAmount (int numberOfLoans) {
        return numberOfLoans * operatingCompany.getValuePerLoan();
    }

    /*----- METHODS TO BE CALLED TO SET UP THE NEXT TURN -----*/

    /**
     * Get the public company that has the turn to operate.
     *
     * @return The currently operating company object.
     */
    public PublicCompanyI getOperatingCompany() {
        return operatingCompany;
    }

    @Override
    public PublicCompanyI[] getOperatingCompanies() {
        return operatingCompanyArray;
    }

    /**
     * Get the current operating round step (i.e. the next action).
     *
     * @return The number that defines the next action.
     */
    public int getStep() {
        return stepObject.intValue();
    }

    /**
     * Bypass normal order of operations and explicitly set round step. This
     * should only be done for specific rails.game exceptions, such as forced
     * train purchases.
     *
     * @param step
     */
    protected void setStep(int step) {
        if (step == STEP_INITIAL) initTurn();

        stepObject.set(step);

    }

    public int getOperatingCompanyIndex() {
        return operatingCompanyIndexObject.intValue();
    }

    /**
     * To be called after each change, to re-establish the currently allowed
     * actions. (new method, intended to absorb code from several other
     * methods).
     *
     */
    @Override
    public boolean setPossibleActions() {

        operatingCompanyIndex = operatingCompanyIndexObject.intValue();
        operatingCompany = operatingCompanyArray[operatingCompanyIndex];

        /* Create a new list of possible actions for the UI */
        possibleActions.clear();
        selectedAction = null;

        int step = getStep();

        if (step == STEP_LAY_TRACK) {
            setNormalTileLays();
            setSpecialTileLays();
            log.debug("Normal tile lays: " + currentNormalTileLays.size());
            log.debug("Special tile lays: " + currentSpecialTileLays.size());

            possibleActions.addAll(currentNormalTileLays);
            possibleActions.addAll(currentSpecialTileLays);
            possibleActions.add(new NullAction(NullAction.SKIP));

        } else if (step == STEP_LAY_TOKEN) {
            setNormalTokenLays();
            setSpecialTokenLays();
            log.debug("Normal token lays: " + currentNormalTokenLays.size());
            log.debug("Special token lays: " + currentSpecialTokenLays.size());

            possibleActions.addAll(currentNormalTokenLays);
            possibleActions.addAll(currentSpecialTokenLays);
            possibleActions.add(new NullAction(NullAction.SKIP));
        } else if (step == STEP_CALC_REVENUE) {
            prepareRevenueAndDividendAction();
        } else if (step == STEP_BUY_TRAIN) {
            setBuyableTrains();
            // TODO Need route checking here.
            // TEMPORARILY allow not buying a train if none owned
            //if (!operatingCompany.mustOwnATrain()
            //        || operatingCompany.getPortfolio().getNumberOfTrains() > 0) {
                        doneAllowed = true;
            //}
        } else if (step == STEP_DISCARD_TRAINS) {
            setTrainsToDiscard();
        }

        // The following additional "common" actions are only available if the
        // primary action is not forced.
        if (step >= 0) {
            
            setBonusTokenLays();

            setDestinationActions();
    
            setGameSpecificPossibleActions();
    
            // Can private companies be bought?
            if (getCurrentPhase().isPrivateSellingAllowed()) {
    
                // Create a list of players with the current one in front
                int currentPlayerIndex = operatingCompany.getPresident().getIndex();
                Player player;
                int minPrice, maxPrice;
                for (int i = currentPlayerIndex; i < currentPlayerIndex
                                                     + numberOfPlayers; i++) {
                    player = players.get(i % numberOfPlayers);
                    for (PrivateCompanyI privComp : player.getPortfolio().getPrivateCompanies()) {
    
                        minPrice =
                                (int) (privComp.getBasePrice() * operatingCompany.getLowerPrivatePriceFactor());
                        maxPrice =
                                (int) (privComp.getBasePrice() * operatingCompany.getUpperPrivatePriceFactor());
                        possibleActions.add(new BuyPrivate(privComp, minPrice,
                                maxPrice));
                    }
                }
            }
    
            // Are there any "common" special properties,
            // i.e. properties that are available to everyone?
            List<SpecialPropertyI> commonSP = gameManager.getCommonSpecialProperties();
            if (commonSP != null) {
            	SellBonusToken sbt;
        loop:   for (SpecialPropertyI sp : commonSP) {
        			if (sp instanceof SellBonusToken) {
            			sbt = (SellBonusToken) sp;
            			// Can't buy if already owned
            			if (operatingCompany.getBonuses() != null) {
            				for (Bonus bonus : operatingCompany.getBonuses()) {
            					if (bonus.getName().equals(sp.getName())) continue loop;
            				}
            			}
            			possibleActions.add (new BuyBonusToken (sbt));
            		}
            	}
            }
        }

        if (doneAllowed) {
            possibleActions.add(new NullAction(NullAction.DONE));
        }

        for (PossibleAction pa : possibleActions.getList()) {
            try {
                log.debug(operatingCompany.getName() + " may: " + pa.toString());
            } catch (Exception e) {
                log.error("Error in toString() of " + pa.getClass(), e);
            }
        }

        return true;
    }

    protected void prepareRevenueAndDividendAction () {

        // There is only revenue if there are any trains
        if (operatingCompany.canRunTrains()) {
            int[] allowedRevenueActions =
                    operatingCompany.isSplitAlways()
                            ? new int[] { SetDividend.SPLIT }
                            : operatingCompany.isSplitAllowed()
                                    ? new int[] { SetDividend.PAYOUT,
                                            SetDividend.SPLIT,
                                            SetDividend.WITHHOLD }
                                    : new int[] { SetDividend.PAYOUT,
                                            SetDividend.WITHHOLD };

            possibleActions.add(new SetDividend(
                    operatingCompany.getLastRevenue(), true,
                    allowedRevenueActions));
        }
    }

    /**
     * Get a list of buyable trains for the currently operating company. Omit
     * trains that the company has no money for. If there is no cash to buy any
     * train from the Bank, prepare for emergency train buying.
     */
    public void setBuyableTrains() {

        if (operatingCompany == null) return;

        TrainManager trainMgr = gameManager.getTrainManager();

        int cash = operatingCompany.getCash();
        int cost;
        List<TrainI> trains;

        boolean hasTrains =
                operatingCompany.getPortfolio().getNumberOfTrains() > 0;
        boolean atTrainLimit =
                operatingCompany.getNumberOfTrains() >= operatingCompany.getCurrentTrainLimit();
        boolean canBuyTrainNow = isBelowTrainLimit();
        boolean presidentMayHelp = !hasTrains && operatingCompany.mustOwnATrain();
        TrainI cheapestTrain = null;
        int costOfCheapestTrain = 0;

        // First check if any more trains may be bought from the Bank
        // Postpone train limit checking, because an exchange might be possible
        if (currentPhase.canBuyMoreTrainsPerTurn()
            || trainsBoughtThisTurn.isEmpty()) {
            boolean mayBuyMoreOfEachType =
                    currentPhase.canBuyMoreTrainsPerTypePerTurn();

            /* New trains */
            trains = trainMgr.getAvailableNewTrains();
            for (TrainI train : trains) {
                if (!mayBuyMoreOfEachType
                    && trainsBoughtThisTurn.contains(train.getType())) {
                    continue;
                }
                cost = train.getCost();
                if (cost <= cash) {
                    if (canBuyTrainNow) {
                    	BuyTrain action = new BuyTrain(train, ipo, cost);
                    	action.setHasNoTrains(!hasTrains); // TEMPORARY
                        possibleActions.add(action);
                    }
                } else if (costOfCheapestTrain == 0
                           || cost < costOfCheapestTrain) {
                    cheapestTrain = train;
                    costOfCheapestTrain = cost;
                }
                // Even at train limit, exchange is allowed (per 1856)
                if (train.canBeExchanged() && hasTrains) {
                    cost = train.getType().getFirstExchangeCost();
                    if (cost <= cash) {
                        List<TrainI> exchangeableTrains =
                                operatingCompany.getPortfolio().getUniqueTrains();
                        BuyTrain action = new BuyTrain(train, ipo, cost);
                        action.setTrainsForExchange(exchangeableTrains);
                        if (atTrainLimit) action.setForcedExchange(true);
                        possibleActions.add(action);
                        canBuyTrainNow = true;
                    }
                }

                if (!canBuyTrainNow) continue;

                // Can a special property be used?
                // N.B. Assume that this never occurs in combination with
                // a train exchange, otherwise the below code must be duplicated
                // above.
                for (SpecialTrainBuy stb : getSpecialProperties(SpecialTrainBuy.class)) {
                    int reducedPrice = stb.getPrice(cost);
                    if (reducedPrice > cash) continue;
                    BuyTrain bt = new BuyTrain(train, ipo, reducedPrice);
                    bt.setSpecialProperty(stb);
                	bt.setHasNoTrains(!hasTrains); // TEMPORARY
                    possibleActions.add(bt);
                }

            }
            if (!canBuyTrainNow) return;

            /* Used trains */
            trains = pool.getUniqueTrains();
            for (TrainI train : trains) {
                if (!mayBuyMoreOfEachType
                    && trainsBoughtThisTurn.contains(train.getType())) {
                    continue;
                }
                cost = train.getCost();
                if (cost <= cash) {
                	BuyTrain bt = new BuyTrain(train, pool, cost);
                	bt.setHasNoTrains(!hasTrains); // TEMPORARY
                    possibleActions.add(bt);
                } else if (costOfCheapestTrain == 0
                           || cost < costOfCheapestTrain) {
                    cheapestTrain = train;
                    costOfCheapestTrain = cost;
                }
            }
            if (!hasTrains && possibleActions.getType(BuyTrain.class).isEmpty()
            		&& cheapestTrain != null && presidentMayHelp) {
            	BuyTrain bt = new BuyTrain(cheapestTrain,
                        cheapestTrain.getHolder(), costOfCheapestTrain);
            	bt.setPresidentMustAddCash(costOfCheapestTrain - cash);
            	bt.setHasNoTrains(!hasTrains); // TODO TEMPORARY
                possibleActions.add(bt);
            }
        }

        if (!canBuyTrainNow) return;

        /* Other company trains, sorted by president (current player first) */
        if (currentPhase.isTrainTradingAllowed()) {
            PublicCompanyI c;
            BuyTrain bt;
            Player p;
            Portfolio pf;
            int index;
            // Set up a list per player of presided companies
            List<List<PublicCompanyI>> companiesPerPlayer =
                    new ArrayList<List<PublicCompanyI>>(numberOfPlayers);
            for (int i = 0; i < numberOfPlayers; i++)
                companiesPerPlayer.add(new ArrayList<PublicCompanyI>(4));
            List<PublicCompanyI> companies;
            // Sort out which players preside over which companies.
            for (int j = 0; j < operatingCompanyArray.length; j++) {
                c = operatingCompanyArray[j];
                if (c.isClosed() || c == operatingCompany) continue;
                p = c.getPresident();
                index = p.getIndex();
                companiesPerPlayer.get(index).add(c);
            }
            // Scan trains per company per player, operating company president
            // first
            int currentPlayerIndex = operatingCompany.getPresident().getIndex();
            for (int i = currentPlayerIndex; i < currentPlayerIndex
                                                 + numberOfPlayers; i++) {
                companies = companiesPerPlayer.get(i % numberOfPlayers);
                for (PublicCompanyI company : companies) {
                    pf = company.getPortfolio();
                    trains = pf.getUniqueTrains();

                    for (TrainI train : trains) {
                        if (train.isObsolete()) continue;
                        if (i != currentPlayerIndex
                                    && trainMgr.buyAtFaceValueBetweenDifferentPresidents()
                                || operatingCompany.mustTradeTrainsAtFixedPrice()
                                || company.mustTradeTrainsAtFixedPrice()) {
                            if (cash >= train.getCost()) {
                                bt = new BuyTrain(train, pf, train.getCost());
                            } else {
                                continue;
                            }
                        } else {
                            bt = new BuyTrain(train, pf, 0);
                        }
                        if (presidentMayHelp && cash < train.getCost()) {
                            bt.setPresidentMayAddCash(train.getCost() - cash);
                        }
                        possibleActions.add(bt);
                    }
                }
            }
        }

        if (!operatingCompany.mustOwnATrain()
                || operatingCompany.getPortfolio().getNumberOfTrains() > 0) {
                    doneAllowed = true;
        }
    }

    /**
     * Returns whether or not the company is allowed to buy a train, considering
     * its train limit.
     *
     * @return
     */
    protected boolean isBelowTrainLimit() {
        return operatingCompany.getNumberOfTrains() < operatingCompany.getCurrentTrainLimit();
    }

    protected void setTrainsToDiscard() {

        // Scan the players in SR sequence, starting with the current player
        Player player;
        List<PublicCompanyI> list;
        int currentPlayerIndex = getCurrentPlayerIndex();
        for (int i = currentPlayerIndex; i < currentPlayerIndex
                                             + numberOfPlayers; i++) {
            player = gameManager.getPlayerByIndex(i);
            if (excessTrainCompanies.containsKey(player)) {
                list = excessTrainCompanies.get(player);
                for (PublicCompanyI comp : list) {
                    possibleActions.add(new DiscardTrain(comp,
                            comp.getPortfolio().getUniqueTrains(), true));
                    // We handle one company at at time.
                    // We come back here until all excess trains have been
                    // discarded.
                    setCurrentPlayer(player);
                    return;
                }
            }
        }
    }

    public int getCashToBeRaisedByPresident() {
        return cashToBeRaisedByPresident;
    }

    /**
     * This is currently a stub, as it is unclear if there is a common
     * rule for setting destination reaching options.
     * See OperatingRound_1856 for a first implementation
     * of such rules.
     */
    protected void setDestinationActions () {

    }

    public void payLoanInterest () {
        int amount = operatingCompany.getCurrentLoanValue()
            * operatingCompany.getLoanInterestPct() / 100;
        new CashMove (operatingCompany, bank, amount);
        DisplayBuffer.add(LocalText.getText("CompanyPaysLoanInterest",
                operatingCompany.getName(),
                Bank.format(amount),
                operatingCompany.getLoanInterestPct(),
                operatingCompany.getCurrentNumberOfLoans(),
                Bank.format(operatingCompany.getValuePerLoan())));
    }


    /* TODO This is just a start of a possible approach to a Help system */
    @Override
    public String getHelp() {
        int step = getStep();
        StringBuffer b = new StringBuffer();
        b.append("<big>Operating round: ").append(thisOrNumber).append(
                "</big><br>");
        b.append("<br><b>").append(operatingCompany.getName()).append(
                "</b> (president ").append(getCurrentPlayer().getName()).append(
                ") has the turn.");
        b.append("<br><br>Currently allowed actions:");
        if (step == STEP_LAY_TRACK) {
            b.append("<br> - Lay a tile");
            b.append("<br> - Press 'Done' if you do not want to lay a tile");
        } else if (step == STEP_LAY_TOKEN) {
            b.append("<br> - Lay a base token or press Done");
            b.append("<br> - Press 'Done' if you do not want to lay a base");
        } else if (step == STEP_CALC_REVENUE) {
            b.append("<br> - Enter new revenue amount");
            b.append("<br> - Press 'Done' if your revenue is zero");
        } else if (step == STEP_PAYOUT) {
            b.append("<br> - Choose how the revenue will be paid out");
        } else if (step == STEP_BUY_TRAIN) {
            b.append("<br> - Buy one or more trains");
            b.append("<br> - Press 'Done' to finish your turn");
        }
        /* TODO: The below if needs be refined. */
        if (getCurrentPhase().isPrivateSellingAllowed()
            && step != STEP_PAYOUT) {
            b.append("<br> - Buy one or more Privates");
        }

        if (step == STEP_LAY_TRACK) {
            b.append("<br><br><b>Tile laying</b> proceeds as follows:");
            b.append("<br><br> 1. On the map, select the hex that you want to lay a new tile upon.");
            b.append("<br>If tile laying is allowed on this hex, the current tile will shrink a bit <br>and a red background will show up around its edges;");
            b.append("<br>in addition, the tiles that can be laid on that hex will be displayed<br> in the 'upgrade panel' at the left hand side of the map.");
            b.append("<br>If tile laying is not allowed there, nothing will happen.");
            b.append("<br><br> 2. Select a tile in the upgrade panel.<br>This tile will be copied to the selected hex,<br>in some orientation");
            b.append("<br><br> 3. If you want to turn the tile just laid to a different orientation, click it.");
            b.append("<br>Repeatedly clicking the tile will rotate it through all allowed orientations.");
            b.append("<br><br> 4. Confirm tile laying by clicking 'Done'");
            b.append("<br><br>Before 'Done' has been pressed, you can change your mind<br>as often as you want");
            b.append(" (presuming that the other players don't get angry).");
            b.append("<br> - If you want to select another hex: repeat step 1");
            b.append("<br> - If you want to lay another tile on the currently selected hex: repeat step 2.");
            b.append("<br> - If you want to undo hex selection: click outside of the map hexes.");
            b.append("<br> - If you don't want to lay a tile after all: press 'Cancel'");
        }

        return b.toString();
    }

    /**
     * Update the status if the step has changed by an Undo or Redo
     */
    public void update(Observable observable, Object object) {
        if (observable == stepObject) {
            prepareStep();
        }
    }

    @Override
    public String toString() {
        return "OperatingRound " + thisOrNumber;
    }

    /** @Overrides */
    public boolean equals(RoundI round) {
        return round instanceof OperatingRound
               && thisOrNumber.equals(((OperatingRound) round).thisOrNumber);
    }

    @Override
	public String getRoundName() {
    	return toString();
    }

}
