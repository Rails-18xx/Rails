package rails.ui.swing;

import java.util.*;

import javax.swing.JDialog;
import javax.swing.JOptionPane;

import org.apache.log4j.Logger;
import org.jgrapht.graph.SimpleGraph;

import rails.algorithms.*;
import rails.common.*;
import rails.game.*;
import rails.game.action.*;
import rails.game.correct.*;
import rails.game.correct.MapCorrectionManager.ActionStep;
import rails.game.special.*;
import rails.sound.SoundManager;
import rails.ui.swing.elements.*;
import rails.ui.swing.hexmap.GUIHex;
import rails.ui.swing.hexmap.HexMap;
import rails.util.Util;

public class ORUIManager implements DialogOwner {

    protected ORWindow orWindow;
    protected ORPanel orPanel;
    private UpgradesPanel upgradePanel;
    private MapPanel mapPanel;
    private HexMap map;
    private MessagePanel messagePanel;
    private RemainingTilesWindow remainingTiles;

    public GameUIManager gameUIManager;
    protected TileManager tileManager;

    private OperatingRound oRound;
    private PublicCompanyI[] companies;
    private PublicCompanyI orComp;
    private int orCompIndex;

    private GameDef.OrStep orStep;
    private int localStep;

    protected PossibleActions possibleActions = PossibleActions.getInstance();
    private boolean privatesCanBeBoughtNow;
    public List<PossibleAction> mapRelatedActions =
        new ArrayList<PossibleAction>();

    private boolean tileLayingEnabled = false;
    public List<LayTile> allowedTileLays = new ArrayList<LayTile>();
    public List<TileI> tileUpgrades;
    private List<MapHex> hexUpgrades;

    private boolean tokenLayingEnabled = false;
    public List<LayToken> allowedTokenLays = new ArrayList<LayToken>();
    public List<? extends TokenI> tokenLays;
    private int selectedTokenIndex;
    private LayToken selectedTokenAllowance;

    // map corrections
    private boolean mapCorrectionEnabled = false;
    private MapCorrectionAction mapCorrectionAction = null;

    /**
     * Will be set true if a cancelled action does not need to be reported to
     * the server, because it does not change the OR turn step. For instance, if
     * a bonus token lay is locally initiated but cancelled.
     */
    protected boolean localAction = false;

    /* Local substeps */
    public static final int INACTIVE = 0;
    public static final int SELECT_HEX_FOR_TILE = 1;
    public static final int SELECT_TILE = 2;
    public static final int ROTATE_OR_CONFIRM_TILE = 3;
    public static final int SELECT_HEX_FOR_TOKEN = 4;
    public static final int SELECT_TOKEN = 5;
    public static final int CONFIRM_TOKEN = 6;
    public static final int SET_REVENUE = 7;
    public static final int SELECT_PAYOUT = 8;
    public static final int MAP_CORRECTION = 9;

    /* Message key per substep */
    protected static final String[] messageKey =
        new String[] { "Inactive", "SelectAHexForTile", "SelectATile",
        "RotateTile", "SelectAHexForToken", "SelectAToken",
        "ConfirmToken", "SetRevenue", "SelectPayout",
    "CorrectMap" };

    /* Keys of dialogs owned by this class */
    public static final String SELECT_DESTINATION_COMPANIES_DIALOG = "SelectDestinationCompanies";
    public static final String REPAY_LOANS_DIALOG = "RepayLoans";

    protected static Logger log =
        Logger.getLogger(ORUIManager.class.getPackage().getName());

    public ORUIManager() {

    }

    public void setGameUIManager (GameUIManager gameUIManager) {
        this.gameUIManager = gameUIManager;
        this.tileManager = gameUIManager.getGameManager().getTileManager();
    }

    public void init(ORWindow orWindow) {

        this.orWindow = orWindow;

        orPanel = orWindow.getORPanel();
        mapPanel = orWindow.getMapPanel();
        upgradePanel = orWindow.getUpgradePanel();
        map = mapPanel.getMap();
        messagePanel = orWindow.getMessagePanel();

    }



    public void initOR(OperatingRound or) {
        oRound = or;
        companies = (oRound).getOperatingCompanies().toArray(new PublicCompanyI[0]);
        map.updateOffBoardToolTips();
        orWindow.activate(oRound);
    }

    public void finish() {
        orWindow.finish();
        if (!(gameUIManager.getCurrentRound() instanceof ShareSellingRound)) {
            setORCompanyTurn(-1);
        }
    }

    private SimpleGraph<NetworkVertex, NetworkEdge> getCompanyGraph(){
        NetworkGraphBuilder nwGraph = NetworkGraphBuilder.create(gameUIManager.getGameManager());
        NetworkCompanyGraph companyGraph = NetworkCompanyGraph.create(nwGraph, orComp);
        SimpleGraph<NetworkVertex, NetworkEdge> graph = companyGraph.createRouteGraph(true);
        return graph;
    }

    public <T extends PossibleAction> void setMapRelatedActions(List<T> actions) {

        GUIHex selectedHex = mapPanel.getMap().getSelectedHex();
        int nextSubStep = ORUIManager.INACTIVE;

        allowedTileLays.clear();
        allowedTokenLays.clear();

        for (T action : actions) {
            if (action instanceof LayTile) {
                allowedTileLays.add((LayTile) action);
            } else if (action instanceof LayToken) {
                allowedTokenLays.add((LayToken) action);
            }
        }

        // moved the check for finishing steps to the beginning
        if (allowedTileLays.size() == 0 && tileLayingEnabled) {
            /* Finish tile laying step */
            if (selectedHex != null) {
                selectedHex.removeTile();
                selectedHex.setSelected(false);
                selectedHex = null;
            }
            // remove selectable indications
            for (MapHex hex:hexUpgrades) {
                GUIHex guiHex = map.getHexByName(hex.getName());
                guiHex.setSelectable(false);
            }
            hexUpgrades = null;
        }

        if (allowedTokenLays.size() == 0 && tokenLayingEnabled) {
            /* Finish token laying step */
            if (selectedHex != null) {
                selectedHex.removeToken();
                selectedHex.setSelected(false);
                selectedHex = null;
            }
            // remove selectable indications
            for (MapHex hex:hexUpgrades) {
                GUIHex guiHex = map.getHexByName(hex.getName());
                guiHex.setSelectable(false);
            }
            hexUpgrades = null;
        }

        if (allowedTileLays.size() > 0) {
            nextSubStep = ORUIManager.SELECT_HEX_FOR_TILE;
            mapPanel.setAllowedTileLays(allowedTileLays);
            // if  hexupgrades is not null, then remove indicators
            if (hexUpgrades != null) {
                for (MapHex hex:hexUpgrades) {
                    GUIHex guiHex = map.getHexByName(hex.getName());
                    guiHex.setSelectable(false);
                }
            }

            // check actions for allowed hexes
            boolean mapHexes = false;
            hexUpgrades = new ArrayList<MapHex>();

            if (gameUIManager.getGameParameterAsBoolean(GuiDef.Parm.ROUTE_HIGHLIGHT)) {
                for (LayTile layTile:allowedTileLays) {
                    switch (layTile.getType()) {
                    case (LayTile.GENERIC):
                        mapHexes = true;
                    break;
                    case (LayTile.SPECIAL_PROPERTY):
                        SpecialPropertyI sp = layTile.getSpecialProperty();
                    if (sp == null || !(sp instanceof SpecialTileLay) ||
                            ((SpecialTileLay)sp).requiresConnection())
                        break;
                    // else fall through
                    case (LayTile.LOCATION_SPECIFIC): // NOT YET USED
                        if (layTile.getLocations() != null) {
                            hexUpgrades.addAll(layTile.getLocations());
                        } else {
                            mapHexes = true;
                        }
                    }
                }

                // standard upgrades
                if (mapHexes) {
                    // generate network graph to indicate the allowed tiles
                    SimpleGraph<NetworkVertex, NetworkEdge> companyGraph = getCompanyGraph();
                    List<MapHex> mapHexUpgrades = NetworkGraphBuilder.getMapHexes(companyGraph);
                    for (MapHex hex:mapHexUpgrades) {
                        if (hex.isUpgradeableNow(gameUIManager.getCurrentPhase()))
                            hexUpgrades.add(hex);
                    }
                    String autoScroll = Config.getGameSpecific("map.autoscroll");
                    if (Util.hasValue(autoScroll) &&  autoScroll.equalsIgnoreCase("no")) {
                        // do nothing
                    } else {
                        mapPanel.scrollPaneShowRectangle(
                                NetworkVertex.getVertexMapCoverage(map, companyGraph.vertexSet()));
                    }
                }

                // activate upgrades
                for (MapHex hex:hexUpgrades) {
                    GUIHex guiHex = map.getHexByName(hex.getName());
                    guiHex.setSelectable(true);
                }

            }
        }

        if (allowedTokenLays.size() > 0) {
            nextSubStep = ORUIManager.SELECT_HEX_FOR_TOKEN;
            mapPanel.setAllowedTokenLays(allowedTokenLays);
            // if  hexupgrades is not null, then remove indicators
            if (hexUpgrades != null) {
                for (MapHex hex:hexUpgrades) {
                    GUIHex guiHex = map.getHexByName(hex.getName());
                    guiHex.setSelectable(false);
                }
            }

            // check actions for allowed hexes
            boolean mapHexes = false;
            hexUpgrades = new ArrayList<MapHex>();
            if (gameUIManager.getGameParameterAsBoolean(GuiDef.Parm.ROUTE_HIGHLIGHT)) {

                for (LayToken layToken:allowedTokenLays) {
                    SpecialPropertyI sp = layToken.getSpecialProperty();
                    if (sp == null) {
                        mapHexes = true;
                    } else if (layToken.getLocations() != null)
                        hexUpgrades.addAll(layToken.getLocations());
                }

                // standard tokens
                if (mapHexes) {
                    // generate network graph to indicate the token lays
                    hexUpgrades.addAll(NetworkGraphBuilder.getTokenableStationHexes(getCompanyGraph(), orComp));
                    for (MapHex hex:hexUpgrades) {
                        GUIHex guiHex = map.getHexByName(hex.getName());
                        guiHex.setSelectable(true);
                    }
                }
            }
        }

        setLocalStep(nextSubStep);
        tileLayingEnabled = allowedTileLays.size() > 0;
        tokenLayingEnabled = allowedTokenLays.size() > 0;
        upgradePanel.setTileMode(tileLayingEnabled);
        upgradePanel.setTokenMode(tokenLayingEnabled);

        setLocalAction(false);
    }

    public void updateMessage() {

        // For now, this only has an effect during tile and token laying.
        // Perhaps we need to centralise message updating here in a later stage.
        log.debug("Calling updateMessage, subStep=" + localStep/*
         * , new
         * Exception("TRACE")
         */);
        if (localStep == ORUIManager.INACTIVE) return;

        String message = LocalText.getText(ORUIManager.messageKey[localStep]);
        SpecialProperty sp;

        /* Add any extra messages */
        String extraMessage = "";

        if (localStep == ORUIManager.MAP_CORRECTION) {
            if (mapCorrectionAction != null)
                extraMessage = LocalText.getText("CorrectMap" + mapCorrectionAction.getStep().name());
        }

        if (localStep == ORUIManager.SELECT_HEX_FOR_TILE) {
            /* Compose prompt for tile laying */
            StringBuffer normalTileMessage = new StringBuffer(" ");

            List<LayTile> tileLays = possibleActions.getType(LayTile.class);
            log.debug("There are " + tileLays.size() + " TileLay objects");
            int ii = 0;
            for (LayTile tileLay : tileLays) {
                Map<String, Integer> tileColours;
                log.debug("TileLay object " + (++ii) + ": " + tileLay);
                sp = tileLay.getSpecialProperty();
                /*
                 * A LayTile object contais either: 1. a special property
                 * (specifying a location) 2. a location (perhaps a list of?)
                 * where a specified set of tiles may be laid, or 3. a map
                 * specifying how many tiles of any colour may be laid
                 * "anywhere". The last option is only a stopgap as we can't yet
                 * determine connectivity.
                 */
                if (sp != null && sp instanceof SpecialTileLay) {
                    SpecialTileLay stl = (SpecialTileLay) sp;
                    extraMessage += "<br>" + stl.getHelp();
                } else if ((tileColours = tileLay.getTileColours()) != null) {
                    int number;
                    for (String colour : tileColours.keySet()) {
                        number = tileColours.get(colour);
                        if (normalTileMessage.length() > 1) {
                            normalTileMessage.append(" ").append(
                                    LocalText.getText("OR")).append(" ");
                        }
                        normalTileMessage.append(number).append(" ").append(
                                colour);
                    }
                }
            }
            if (normalTileMessage.length() > 1) {
                message +=
                    " "
                    + LocalText.getText("TileColours",
                            normalTileMessage);
            }

        } else if (localStep == ORUIManager.SELECT_HEX_FOR_TOKEN) {

            /* Compose prompt for token laying */
            String locations;
            StringBuffer normalTokenMessage = new StringBuffer(" ");

            List<LayBaseToken> tokenLays =
                possibleActions.getType(LayBaseToken.class);
            log.debug("There are " + tokenLays.size() + " TokenLay objects");
            int ii = 0;
            for (LayBaseToken tokenLay : tokenLays) {

                log.debug("TokenLay object " + (++ii) + ": " + tokenLay);
                sp = tokenLay.getSpecialProperty();
                if (sp != null && sp instanceof SpecialTokenLay) {
                    extraMessage += "<br>" + sp.getHelp();
                } else if ((locations = tokenLay.getLocationNameString()) != null) {
                    if (normalTokenMessage.length() > 1) {
                        normalTokenMessage.append(" ").append(
                                LocalText.getText("OR")).append(" ");
                    }
                    normalTokenMessage.append(locations);
                }
            }
            if (normalTokenMessage.length() > 1) {
                message += " " + LocalText.getText("NormalToken",
                        normalTokenMessage);
            }
        }
        if (extraMessage.length() > 0) {
            message += "<font color=\"red\">" + extraMessage + "</font>";
        }

        setMessage(message);

    }

    /**
     * Processes button presses and menu selection actions
     *
     * @param command
     * @param actions
     */
    public void processAction(String command, List<PossibleAction> actions) {

        if (actions != null && actions.size() > 0
                && !processGameSpecificActions(actions)) {

            Class<? extends PossibleAction> actionType =
                actions.get(0).getClass();

            if (actionType == SetDividend.class) {

                setDividend(command, (SetDividend) actions.get(0));

            } else if (actionType == LayBonusToken.class) {

                prepareBonusToken((LayBonusToken) actions.get(0));

            } else if (actionType == LayBaseToken.class) {

                /* Only used outside the token laying step */
                // Can currently handle only one location!
                LayBaseToken lbt = (LayBaseToken) actions.get(0);
                map.selectHex(map.getHexByName(lbt.getLocations().get(0).getName()));
                layBaseToken (lbt);

            } else if (actionType == BuyBonusToken.class) {

                buyBonusToken ((BuyBonusToken)actions.get(0));

            } else if (actionType == NullAction.class
                    || actionType == GameAction.class ) {

                orWindow.process(actions.get(0));

            } else if (actionType == ReachDestinations.class) {

                reachDestinations ((ReachDestinations) actions.get(0));

            } else if (actionType == TakeLoans.class) {

                takeLoans ((TakeLoans)actions.get(0));

            } else if (actionType == RepayLoans.class) {

                repayLoans ((RepayLoans)actions.get(0));

            } else if (actionType == UseSpecialProperty.class) {

                useSpecialProperty ((UseSpecialProperty)actions.get(0));

            } else if (actions.get(0) instanceof CorrectionAction) {

                processCorrectionAction((CorrectionAction)actions.get(0));

            } else if (actionType == ClosePrivate.class) {

                gameUIManager.processAction(actions.get(0));

            }

        } else if (command.equals(ORPanel.OPERATING_COST_CMD)) {

            operatingCosts();

        } else if (command.equals(ORPanel.BUY_TRAIN_CMD)) {

            buyTrain();

        } else if (command.equals(ORPanel.BUY_PRIVATE_CMD)) {

            buyPrivate();

        } else if (command.equals(ORPanel.REM_TILES_CMD)) {

            displayRemainingTiles();

        }

        gameUIManager.reportWindow.updateLog();
    }

    /** Stub, can be overridden in subclasses */
    protected boolean processGameSpecificActions(List<PossibleAction> actions) {

        return false;

    }

    private void setDividend(String command, SetDividend action) {

        int amount;

        if (command.equals(ORPanel.SET_REVENUE_CMD)) {
            amount = orPanel.getRevenue(orCompIndex);
            orPanel.stopRevenueUpdate();
            log.debug("Set revenue amount is " + amount);
            action.setActualRevenue(amount);

            // notify sound manager of set revenue amount as soon as
            // set revenue is pressed (not waiting for the completion
            // of the set dividend action)
            SoundManager.notifyOfSetRevenue(amount);

            if (amount == 0 || action.getRevenueAllocation() != SetDividend.UNKNOWN) {
                log.debug("Allocation is known: "
                        + action.getRevenueAllocation());
                orWindow.process(action);
            } else {
                log.debug("Allocation is unknown, asking for it");
                setLocalStep(SELECT_PAYOUT);
                updateStatus(action, true);

                // Locally update revenue if we don't inform the server yet.
                orPanel.setRevenue(orCompIndex, amount);
            }
        } else {
            // The revenue allocation has been selected
            orWindow.process(action);
        }
    }

    private void prepareBonusToken(LayBonusToken action) {

        orWindow.requestFocus();

        List<LayToken> actions = new ArrayList<LayToken>();
        actions.add(action);
        setMapRelatedActions(actions);
        allowedTokenLays = actions;
        setLocalAction(true);

        log.debug("BonusTokens can be laid");

        mapPanel.setAllowedTokenLays(actions);

        orPanel.initTokenLayingStep();

    }

    private void buyBonusToken (BuyBonusToken action) {

        orWindow.process(action);
    }

    protected void reachDestinations (ReachDestinations action) {

        List<String> options = new ArrayList<String>();
        List<PublicCompanyI> companies = action.getPossibleCompanies();

        for (PublicCompanyI company : companies) {
            options.add(company.getName());
        }

        if (options.size() > 0) {
            orWindow.setVisible(true);
            orWindow.toFront();

            CheckBoxDialog dialog = new CheckBoxDialog(SELECT_DESTINATION_COMPANIES_DIALOG,
                    this,
                    orWindow,
                    LocalText.getText("DestinationsReached"),
                    LocalText.getText("DestinationsReachedPrompt"),
                    options.toArray(new String[0]));
            setCurrentDialog (dialog, action);
        }
    }

    public void dialogActionPerformed () {

        JDialog currentDialog = getCurrentDialog();
        PossibleAction currentDialogAction = getCurrentDialogAction();

        if (currentDialog instanceof CheckBoxDialog
                && currentDialogAction instanceof ReachDestinations) {

            CheckBoxDialog dialog = (CheckBoxDialog) currentDialog;
            ReachDestinations action = (ReachDestinations) currentDialogAction;

            boolean[] destined = dialog.getSelectedOptions();
            String[] options = dialog.getOptions();

            for (int index=0; index < options.length; index++) {
                if (destined[index]) {
                    action.addReachedCompany(action.getPossibleCompanies().get(index));
                }
            }

            // Prevent that a null action gets processed
            if (action.getReachedCompanies() == null
                    || action.getReachedCompanies().isEmpty()) currentDialogAction = null;

        } else {
            currentDialogAction = null;
        }

        // Required even if no action is executed, to update the UI, re-enable buttons etc.
        gameUIManager.processAction(currentDialogAction);
    }

    public JDialog getCurrentDialog() {
        return gameUIManager.getCurrentDialog();
    }

    public PossibleAction getCurrentDialogAction () {
        return gameUIManager.getCurrentDialogAction();
    }

    public void setCurrentDialog (JDialog dialog, PossibleAction action) {
        gameUIManager.setCurrentDialog(dialog, action);
        if (!(dialog instanceof MessageDialog)) orPanel.disableButtons();
    }

    /**
     * @return True if the map panel expected hex clicks for actions / corrections
     */
    public boolean hexClicked(GUIHex clickedHex, GUIHex selectedHex) {

        boolean triggerORPanelRepaint = false;

        if (mapCorrectionEnabled) {
            triggerORPanelRepaint = true;
            boolean checkClickedHex = false;
            switch (mapCorrectionAction.getStep()) {
            case SELECT_HEX:
            case SELECT_TILE: // if tile is selected
                checkClickedHex = true;
                break;
            case SELECT_ORIENTATION:
                if (clickedHex == selectedHex) {
                    selectedHex.forcedRotateTile();
                } else
                    checkClickedHex = true;
                break;
            }
            if (checkClickedHex && clickedHex !=null && clickedHex != selectedHex) {
                map.selectHex(clickedHex);
                mapCorrectionAction.selectHex(clickedHex.getHexModel());
                orWindow.process(mapCorrectionAction);
            }
        } else if (tokenLayingEnabled) {
            triggerORPanelRepaint = true;
            // if clickedHex == null, then go back to select hex step
            if (clickedHex == null) {
                upgradePanel.setPossibleTokenLays(null);
                setLocalStep(SELECT_HEX_FOR_TOKEN);
                return true;
            }
            List<LayToken> allowances =
                map.getTokenAllowanceForHex(clickedHex.getHexModel());
            if (allowances.size() > 0) {
                log.debug("Hex " + clickedHex.getName()
                        + " clicked, allowances:");
                for (LayToken allowance : allowances) {
                    log.debug(allowance.toString());
                }
                map.selectHex(clickedHex);
                setLocalStep(SELECT_TOKEN);
            } else {
                JOptionPane.showMessageDialog(mapPanel, LocalText.getText(
                        "NoTokenPossible", clickedHex.getName()));
                setLocalStep(ORUIManager.SELECT_HEX_FOR_TOKEN);
            }

        } else if (tileLayingEnabled) {
            triggerORPanelRepaint = true;
            if (localStep == ROTATE_OR_CONFIRM_TILE
                    && clickedHex == selectedHex) {
                selectedHex.rotateTile();
                //directly inform sound framework of "rotate tile" local step
                //as notification via "set local step" does not occur
                SoundManager.notifyOfORLocalStep(localStep);
                return true;

            } else {
                if (selectedHex != null && clickedHex != selectedHex) {
                    selectedHex.removeTile();
                    map.selectHex(null);
                }
                // if clickedHex == null, then go back to select hex step
                if (clickedHex == null) {
                    upgradePanel.setTileUpgrades(null);
                    setLocalStep(SELECT_HEX_FOR_TILE);
                } else {
                    if (clickedHex.getHexModel().isUpgradeableNow())
                        /*
                         * Direct call to Model to be replaced later by use of
                         * allowedTilesPerHex. Would not work yet.
                         */
                    {
                        map.selectHex(clickedHex);
                        setLocalStep(SELECT_TILE);
                    } else {
                        JOptionPane.showMessageDialog(mapPanel,
                        "This hex cannot be upgraded now");
                    }
                }
            }
        }

        if (triggerORPanelRepaint) orWindow.repaintORPanel();

        return triggerORPanelRepaint;
    }

    public void tileSelected(int tileId) {

        TileI tile = gameUIManager.getGameManager().getTileManager().getTile(tileId);
        GUIHex hex = map.getSelectedHex();

        // map correction override
        if (mapCorrectionEnabled) {
            // paint tile
            hex.forcedDropTile(tileId, 0);
            // inform map correction manager
            mapCorrectionAction.selectTile(tile);
            orWindow.process(mapCorrectionAction);
            return;
        }

        // Check if the new tile must be connected to some other track
        boolean mustConnect = getMustConnectRequirement(hex,tile);

        if (hex.dropTile(tileId, mustConnect)) {
            /* Lay tile */
            setLocalStep(ORUIManager.ROTATE_OR_CONFIRM_TILE);
        } else {
            /* Tile cannot be laid in a valid orientation: refuse it */
            JOptionPane.showMessageDialog(mapPanel,
            "This tile cannot be laid in a valid orientation.");
            tileUpgrades.remove(tile);
            setLocalStep(ORUIManager.SELECT_TILE);
            upgradePanel.showUpgrades();
        }
    }

    /**
     * @return True if the indicated tile must be connected to some other track if
     * placed on indicated hex.
     */
    public boolean getMustConnectRequirement (GUIHex hex,TileI tile) {
        if (tile == null || hex == null) return false;
        return tile.getColourName().equalsIgnoreCase(Tile.YELLOW_COLOUR_NAME)
        // Does not apply to the current company's home hex(es)
        && !hex.getHexModel().isHomeFor(orComp)
        // Does not apply to special tile lays
        && !isUnconnectedTileLayTarget(hex.getHexModel());
    }

    public void addTileUpgradeIfValid(GUIHex hex, int tileId) {
        addTileUpgradeIfValid (hex,
                gameUIManager.getGameManager().getTileManager().getTile(tileId));
    }

    public void addTileUpgradeIfValid(GUIHex hex, TileI tile) {
        if (!tileUpgrades.contains(tile) && isTileUpgradeValid(hex,tile)) {
            tileUpgrades.add(tile);
        }
    }

    public boolean isTileUpgradeValid(GUIHex hex, TileI tile) {
        // Check if the new tile must be connected to some other track
        return hex.isTileUpgradeValid(tile.getId(),
                getMustConnectRequirement(hex,tile));
    }

    protected boolean isUnconnectedTileLayTarget(MapHex hex) {

        for (LayTile action : possibleActions.getType(LayTile.class)) {
            if (action.getType() == LayTile.SPECIAL_PROPERTY
                    && action.getSpecialProperty().getLocations().contains(hex)) {
                // log.debug(hex.getName()+" is a special property target");
                return true;
            }
        }
        // log.debug(hex.getName()+" is NOT a special property target");
        return false;
    }

    public void tokenSelected(LayToken tokenAllowance) {

        if (tokenAllowance != null && allowedTokenLays.contains(tokenAllowance)) {
            selectedTokenAllowance = tokenAllowance;
            selectedTokenIndex = allowedTokenLays.indexOf(tokenAllowance);
        } else {
            selectedTokenAllowance = null;
            selectedTokenIndex = -1;
        }
        upgradePanel.setSelectedTokenIndex(selectedTokenIndex);
    }

    private void layTile() {

        GUIHex selectedHex = map.getSelectedHex();

        if (selectedHex != null && selectedHex.canFixTile()) {
            List<LayTile> allowances =
                map.getTileAllowancesForHex(selectedHex.getHexModel());
            LayTile allowance = null;
            TileI tile = selectedHex.getProvisionalTile();
            if (allowances.size() == 1) {
                allowance = allowances.get(0);
            } else {
                // Check which allowance applies
                // We'll restrict to cases where we have both a special property
                // and a normal 'blanket' allowance.
                // First check which is which.
                List<TileI> sp_tiles;
                List<MapHex> sp_hexes;
                LayTile gen_lt = null;
                LayTile spec_lt = null;
                for (LayTile lt : allowances) {
                    if (lt.getType() == LayTile.SPECIAL_PROPERTY) {
                        // Cases where a special property is used include:
                        // 1. SP refers to specified tiles, (one of) which is chosen:
                        // (examples: 18AL Lumber Terminal, 1889 Port)
                        if ((sp_tiles = lt.getTiles()) != null
                                && !sp_tiles.contains(tile)) continue;
                        // 2. SP refers to specified hexes, (one of) which is chosen:
                        // (example: 1830 hex B20)
                        if ((sp_hexes = lt.getLocations()) != null
                                && !sp_hexes.contains(selectedHex.getModel())) continue;
                        spec_lt = lt;
                    } else {
                        // Default case: the generic allowance
                        gen_lt = lt;
                    }
                }

                allowance = spec_lt == null ? gen_lt :
                    gen_lt == null ? spec_lt :
                        spec_lt.getSpecialProperty().getPriority()
                        == SpecialPropertyI.Priority.FIRST ? spec_lt : gen_lt;

            }
            allowance.setChosenHex(selectedHex.getHexModel());
            allowance.setOrientation(selectedHex.getProvisionalTileRotation());
            allowance.setLaidTile(tile);

            relayBaseTokens (allowance);

            if (orWindow.process(allowance)) {
                selectedHex.fixTile();
            } else {
                selectedHex.removeTile();
                setLocalStep(SELECT_HEX_FOR_TILE);
            }
            map.selectHex(null);
        }
    }

    public void layBaseToken(LayBaseToken action) {

        GUIHex selectedHex = map.getSelectedHex();
        LayBaseToken allowance;

        if (selectedHex != null) {
            if (action != null) {
                allowance = action;
            } else {
                List<LayBaseToken> allowances =
                    map.getBaseTokenAllowanceForHex(selectedHex.getHexModel());
                // Pick the first one (unknown if we will ever need more than one)
                allowance = allowances.get(0);
            }
            int station;
            List<Stop> stations = selectedHex.getHexModel().getStops();

            switch (stations.size()) {
            case 0: // No stations
                return;

            case 1:
                station = 1;
                break;

            default:
                // Check what connections each city has.
                // Also remove any cities with no room.
                List<String> prompts = new ArrayList<String>();
                Map<String, Stop> promptToCityMap = new HashMap<String, Stop>();
                String prompt;
                for (Stop city : stations) {
                    if (city.hasTokenSlotsLeft()) {
                        prompt = LocalText.getText(
                                "SelectStationForTokenOption",
                                city.getNumber(),
                                ((MapHex) selectedHex.getModel()).getConnectionString(
                                        selectedHex.getCurrentTile(),
                                        ((MapHex) selectedHex.getModel()).getCurrentTileRotation(),
                                        city.getRelatedStation().getNumber())) ;
                        prompts.add(prompt);
                        promptToCityMap.put(prompt, city);
                    }
                }
                if (prompts.isEmpty()) {
                    return;
                }
                // If more than one City to choose from, ask the player. Otherwise use Element zero (first) for the station.
                if (prompts.size() > 1) {
                    String selected =
                        (String) JOptionPane.showInputDialog(orWindow,
                                LocalText.getText("SelectStationForToken",
                                        action.getPlayerName(),
                                        selectedHex.getName(),
                                        action.getCompanyName()),
                                        LocalText.getText("WhichStation"),
                                        JOptionPane.PLAIN_MESSAGE, null,
                                        prompts.toArray(), prompts.get(0));
                    if (selected == null) return;
                    station = promptToCityMap.get(selected).getNumber();
                } else {
                    station = promptToCityMap.get(prompts.toArray() [0]).getNumber();
                }
            }

            allowance.setChosenHex(selectedHex.getHexModel());
            allowance.setChosenStation(station);

            if (orWindow.process(allowance)) {
                upgradePanel.clear();
                selectedHex.fixToken();
            } else {
                setLocalStep(ORUIManager.SELECT_HEX_FOR_TOKEN);
            }
        }
    }

    /**
     * Manually relay the tokens.
     * This is only needed in special cases,
     * such as the 1830 Erie home token.
     * If applicable, the TileSet entry for the <i>old</i> tile
     * should specify <code>relayBaseTokens="yes"</code> as an
     * attribute in the Upgrade tag.
     * @param action The LayTile PossibleAction.
     */
    protected void relayBaseTokens (LayTile action) {

        final MapHex hex = action.getChosenHex();
        TileI newTile = action.getLaidTile();
        TileI oldTile = hex.getCurrentTile();
        if (!action.isRelayBaseTokens()
                && !oldTile.relayBaseTokensOnUpgrade()) return;

        List<Stop> stopsToQuery = hex.getStops();

        /* Check which tokens must be relaid, and in which sequence.
         * Ideally, the game engine should instruct the UI what to do
         * if there is more than one stop and more than one token.
         * TODO LayTile does not yet allow that.
         * 
         * For now, the only case that needs special handling is the 1835 BA home hex L6,
         * where it it possible to have two tokens laid before even one tile.
         * Let's generalise this case to: two stops, both tokened.
         * We consider single-slot stops only.
         * In fact, all we need to do is
         * 1. Sort the stops so that the home company gets queried first,
         * 2. Count down the number of free slots per new station, so that full stations are skipped,
         * It's already taken care for, that a choice-between-one is handled automatically.
         * [EV, jun2012]
         */
        if (stopsToQuery.size() == 2) {
            Collections.sort(stopsToQuery, new Comparator<Stop>() {
                public int compare (Stop s1, Stop s2) {
                    List<TokenI> tokens;
                    boolean stop1IsHome = !((tokens = s1.getTokens()).isEmpty())
                    && ((BaseToken)tokens.get(0)).getCompany().getHomeHexes().contains(hex);
                    boolean stop2IsHome = !((tokens = s2.getTokens()).isEmpty())
                    && ((BaseToken)tokens.get(0)).getCompany().getHomeHexes().contains(hex);
                    if (stop1IsHome && !stop2IsHome) {
                        return -1;
                    } else if (stop2IsHome && !stop1IsHome) {
                        return 1;
                    } else {
                        return 0; // Doesn't matter
                    }
                }
            });
        }

        // Array to enable counting down the free token slots per new station
        int[] freeSlots = new int[1 + newTile.getStations().size()];
        for (Station newStation : newTile.getStations()) {
            freeSlots[newStation.getNumber()] = newStation.getBaseSlots();
        }

        for (Stop oldStop : stopsToQuery) {
            if (oldStop.hasTokens()) {
                // Assume only 1 token (no exceptions known)
                PublicCompanyI company = ((BaseToken)oldStop.getTokens().get(0)).getCompany();

                List<String> prompts = new ArrayList<String>();
                Map<String, Integer> promptToCityMap = new HashMap<String, Integer>();
                String prompt;
                for (Station newStation : newTile.getStations()) {
                    if (newStation.getBaseSlots() > 0 && freeSlots[newStation.getNumber()] > 0) {
                        prompt = LocalText.getText("SelectStationForTokenOption",
                                newStation.getNumber(),
                                hex.getConnectionString(
                                        newTile,
                                        action.getOrientation(),
                                        newStation.getNumber()) );
                        prompts.add(prompt);
                        promptToCityMap.put(prompt, newStation.getNumber());
                    }
                }
                if (prompts.isEmpty()) {
                    continue;
                }
                if (prompts.size () > 1) {
                    String selected =
                        (String) JOptionPane.showInputDialog(orWindow,
                                LocalText.getText("SelectStationForToken",
                                        //action.getPlayerName(),
                                        /* In some cases, it's not the acting player that must take this action,
                                         * but it's always the president.
                                         * TODO WARNING: THE NEXT LINE BREAKS THE CLIENT/SERVER SEPARATION
                                         * so this is a provisional fix only.
                                         * It is for 1835, and intends to address the BA president if the BA home token
                                         * must be laid in case another company, having the turn, lays a green tile in L6
                                         * using the PfB when the BA has started but not yet operated.
                                         */
                                        company.getPresident().getName(),
                                        hex.getName(),
                                        company.getName()
                                ),
                                LocalText.getText("WhichStation"),
                                JOptionPane.PLAIN_MESSAGE, null,
                                prompts.toArray(), prompts.get(0));
                    if (selected == null) return;
                    action.addRelayBaseToken(company.getName(), promptToCityMap.get(selected));
                    --freeSlots[promptToCityMap.get(selected)];
                } else {
                    action.addRelayBaseToken(company.getName(), promptToCityMap.get(prompts.toArray()[0]));
                    --freeSlots[promptToCityMap.get(prompts.toArray()[0])];
                }
            }
        }
    }

    private Station correctionRelayBaseToken(BaseToken token, List<Station> possibleStations){
        GUIHex selectedHex = map.getSelectedHex();

        PublicCompanyI company = token.getCompany();
        List<String> prompts = new ArrayList<String>();

        Map<String, Station> promptToStationMap = new HashMap<String, Station>();
        String prompt;
        for (Station station:possibleStations) {
            prompt = LocalText.getText(
                    "SelectStationForTokenOption",
                    station.getNumber(),
                    ((MapHex) selectedHex.getModel()).getConnectionString(
                            selectedHex.getProvisionalTile(),
                            selectedHex.getProvisionalTileRotation(),
                            station.getNumber())) ;
            prompts.add(prompt);
            promptToStationMap.put(prompt, station);
        }
        String selected =
            (String) JOptionPane.showInputDialog(orWindow,
                    LocalText.getText("SelectStationForToken",
                            "",
                            selectedHex.getName(),
                            company.getName()),
                            LocalText.getText("WhichStation"),
                            JOptionPane.PLAIN_MESSAGE, null,
                            prompts.toArray(), prompts.get(0));
        if (selected == null) return null;
        Station station = promptToStationMap.get(selected);
        return station;
    }


    /**
     * Lay Token finished.
     *
     * @param action The LayBonusToken action object of the laid token.
     */
    public void layBonusToken(PossibleAction action) {

        // Assumption for now: always BonusToken
        // We might use it later for BaseTokens too.

        HexMap map = mapPanel.getMap();
        GUIHex selectedHex = map.getSelectedHex();

        if (selectedHex != null) {
            LayToken executedAction = (LayToken) action;

            executedAction.setChosenHex(selectedHex.getHexModel());

            if (orWindow.process(executedAction)) {
                upgradePanel.clear();
                map.selectHex(null);
                //ensure painting the token (model update currently does not arrive at UI)
                map.repaintTokens(selectedHex.getBounds());
                selectedHex = null;
            }
        }
    }

    public void operatingCosts(){

        List<String> textOC = new ArrayList<String>();
        List<OperatingCost> actionOC = possibleActions.getType(OperatingCost.class);

        for (OperatingCost ac:actionOC) {

            String suggestedCostText;
            if (ac.isFreeEntryAllowed())
                suggestedCostText = LocalText.getText("OCAmountEntry");
            else
                suggestedCostText = Bank.format(ac.getAmount());

            OperatingCost.OCType t = ac.getOCType();
            if (t == OperatingCost.OCType.LAY_TILE)
                textOC.add(LocalText.getText("OCLayTile",
                        suggestedCostText ));

            if (t == OperatingCost.OCType.LAY_BASE_TOKEN)
                textOC.add(LocalText.getText("OCLayBaseToken",
                        suggestedCostText ));
        }

        if (!textOC.isEmpty()) {
            String chosenOption = (String) JOptionPane.showInputDialog(orWindow,
                    LocalText.getText("OCSelectMessage"),
                    LocalText.getText("OCSelectTitle"),
                    JOptionPane.QUESTION_MESSAGE, null,
                    textOC.toArray(), textOC.get(0));
            if (chosenOption != null) {
                int index = textOC.indexOf(chosenOption);
                OperatingCost chosenAction = actionOC.get(index);
                if (chosenAction.isFreeEntryAllowed()) {
                    String costString = (String) JOptionPane.showInputDialog(orWindow,
                            LocalText.getText("OCDialogMessage", chosenOption),
                            LocalText.getText("OCDialogTitle"),
                            JOptionPane.QUESTION_MESSAGE, null,
                            null, chosenAction.getAmount());
                    int cost;
                    try {
                        cost = Integer.parseInt(costString);
                    } catch (NumberFormatException e) {
                        cost = 0;
                    }
                    chosenAction.setAmount(cost);
                } else {
                    chosenAction.setAmount(chosenAction.getAmount());
                }

                if (orWindow.process(chosenAction)) {
                    updateMessage();
                }
            }
        }
    }

    public void buyTrain() {

        List<String> prompts = new ArrayList<String>();
        Map<String, PossibleAction> promptToTrain =
            new HashMap<String, PossibleAction>();
        TrainI train;
        String usingPrivates = "";

        PossibleAction selectedAction;
        BuyTrain buyAction;

        String prompt;
        StringBuffer b;
        int cost;
        Portfolio from;

        List<BuyTrain> buyableTrains = possibleActions.getType(BuyTrain.class);
        for (BuyTrain bTrain : buyableTrains) {
            train = bTrain.getTrain();
            cost = bTrain.getFixedCost();
            from = bTrain.getFromPortfolio();

            /* Create a prompt per buying option */
            b = new StringBuffer();

            b.append(LocalText.getText("BUY_TRAIN_FROM",
                    bTrain.getType(),
                    from.getName() ));
            if (bTrain.isForExchange()) {
                b.append(" (").append(LocalText.getText("EXCHANGED")).append(
                ")");
            }
            if (cost > 0) {
                b.append(" ").append(
                        LocalText.getText("AT_PRICE", Bank.format(cost)));
            }
            if (bTrain.hasSpecialProperty()) {
                String priv =
                    (bTrain.getSpecialProperty()).getOriginalCompany().getName();
                b.append(" ").append(LocalText.getText("USING_SP", priv));
                usingPrivates += ", " + priv;
            }
            if (bTrain.mustPresidentAddCash()) {
                b.append(" ").append(
                        LocalText.getText("YOU_MUST_ADD_CASH",
                                Bank.format(bTrain.getPresidentCashToAdd())));
            } else if (bTrain.mayPresidentAddCash()) {
                b.append(" ").append(
                        LocalText.getText("YOU_MAY_ADD_CASH",
                                Bank.format(bTrain.getPresidentCashToAdd())));
            }

            if (bTrain.getExtraMessage() != null) {
                b.append(" (").append(bTrain.getExtraMessage()+")");
            }
            prompt = b.toString();
            prompts.add(prompt);
            promptToTrain.put(prompt, bTrain);
        }

        if (prompts.size() == 0) {
            JOptionPane.showMessageDialog(orWindow,
                    LocalText.getText("CannotBuyAnyTrain"));
            return;
        }

        StringBuffer msgbuf =
            new StringBuffer(LocalText.getText("SelectTrain"));
        if (usingPrivates.length() > 0) {
            msgbuf.append("<br><font color=\"red\">");
            msgbuf.append(LocalText.getText("SelectCheapTrain",
                    usingPrivates.substring(2)));
            msgbuf.append("</font>");
        }
        setMessage(msgbuf.toString());

        String selectedActionText =
            (String) JOptionPane.showInputDialog(orWindow,
                    LocalText.getText("BUY_WHICH_TRAIN"),
                    LocalText.getText("WHICH_TRAIN"),
                    JOptionPane.QUESTION_MESSAGE, null, prompts.toArray(),
                    prompts.get(0));
        if (!Util.hasValue(selectedActionText)) return;

        selectedAction = promptToTrain.get(selectedActionText);
        if (selectedAction == null) return;

        buyAction = (BuyTrain) selectedAction;
        train = buyAction.getTrain();
        Portfolio seller = buyAction.getFromPortfolio();
        int price = buyAction.getFixedCost();

        if (price == 0 && seller.getOwner() instanceof PublicCompanyI) {
            prompt = LocalText.getText("WHICH_TRAIN_PRICE",
                    orComp.getName(),
                    train.getName(),
                    seller.getName() );
            String response;
            for (;;) {
                response =
                    JOptionPane.showInputDialog(orWindow, prompt,
                            LocalText.getText("WHICH_PRICE"),
                            JOptionPane.QUESTION_MESSAGE);
                if (response == null) return; // Cancel
                try {
                    price = Integer.parseInt(response);
                } catch (NumberFormatException e) {
                    // Price stays 0, this is handled below
                }
                if (price > 0) break; // Got a good (or bad, but valid) price.

                if (!prompt.startsWith("Please")) {
                    prompt =
                        LocalText.getText("ENTER_PRICE_OR_CANCEL") + "\n"
                        + prompt;
                }
            }
        }

        TrainI exchangedTrain = null;
        if (train != null && buyAction.isForExchange()) {
            List<TrainI> oldTrains = buyAction.getTrainsForExchange();
            if (oldTrains.size() == 1) {
                exchangedTrain = oldTrains.get(0);
            } else {
                List<String> oldTrainOptions =
                    new ArrayList<String>(oldTrains.size());
                String[] options = new String[oldTrains.size()];
                int jj = 0;
                for (int j = 0; j < oldTrains.size(); j++) {
                    options[jj + j] =
                        LocalText.getText("N_Train", oldTrains.get(j).getName());
                    oldTrainOptions.add(options[jj + j]);
                }
                String exchangedTrainName =
                    (String) JOptionPane.showInputDialog(orWindow,
                            LocalText.getText("WHICH_TRAIN_EXCHANGE_FOR",
                                    Bank.format(price)),
                                    LocalText.getText("WHICH_TRAIN_TO_EXCHANGE"),
                                    JOptionPane.QUESTION_MESSAGE, null, options,
                                    options[0]);
                if (exchangedTrainName != null) {
                    int index = oldTrainOptions.indexOf(exchangedTrainName);
                    if (index >= 0) {
                        exchangedTrain = oldTrains.get(index);
                    }
                }
                if (exchangedTrain == null) {
                    // No valid train selected - cancel the buy action
                    train = null;
                }
            }
        }

        if (train != null) {
            // Remember the old off-board revenue step
            int oldOffBoardRevenueStep =
                gameUIManager.getCurrentPhase().getOffBoardRevenueStep();

            buyAction.setPricePaid(price);
            buyAction.setExchangedTrain(exchangedTrain);

            if (orWindow.process(selectedAction)) {

                // Check if any trains must be discarded
                // Keep looping until all relevant companies have acted

                // TODO This must be split off from here, as in the future
                // different clients may handle the discards of each company.
                /*
                while (possibleActions.contains(DiscardTrain.class)) {
                    // Check if there are any forced discards;
                    // otherwise, nothing to do here
                    DiscardTrain dt =
                            possibleActions.getType(DiscardTrain.class).get(0);
                    if (dt == null) break;

                    gameUIManager.discardTrains(dt);
                }
                 */
            }

            int newOffBoardRevenueStep =
                gameUIManager.getCurrentPhase().getOffBoardRevenueStep();
            if (newOffBoardRevenueStep != oldOffBoardRevenueStep) {
                map.updateOffBoardToolTips();
            }

        }

    }

    public void buyPrivate() {

        int amount, index;
        List<String> privatesForSale = new ArrayList<String>();
        List<BuyPrivate> privates = possibleActions.getType(BuyPrivate.class);
        String chosenOption;
        BuyPrivate chosenAction = null;
        int minPrice = 0, maxPrice = 0;
        String priceRange;

        for (BuyPrivate action : privates) {
            minPrice = action.getMinimumPrice();
            maxPrice = action.getMaximumPrice();
            if (minPrice < maxPrice) {
                priceRange = Bank.format(minPrice) + "..." + Bank.format(maxPrice);
            } else {
                priceRange = Bank.format(maxPrice);
            }

            privatesForSale.add(LocalText.getText("BuyPrivatePrompt",
                    action.getPrivateCompany().getName(),
                    action.getPrivateCompany().getPortfolio().getName(),
                    priceRange ));
        }

        if (privatesForSale.size() > 0) {
            chosenOption =
                (String) JOptionPane.showInputDialog(orWindow,
                        LocalText.getText("BUY_WHICH_PRIVATE"),
                        LocalText.getText("WHICH_PRIVATE"),
                        JOptionPane.QUESTION_MESSAGE, null,
                        privatesForSale.toArray(), privatesForSale.get(0));
            if (chosenOption != null) {
                index = privatesForSale.indexOf(chosenOption);
                chosenAction = privates.get(index);
                minPrice = chosenAction.getMinimumPrice();
                maxPrice = chosenAction.getMaximumPrice();
                if (minPrice < maxPrice) {
                    String price =
                        JOptionPane.showInputDialog(orWindow,
                                LocalText.getText("WHICH_PRIVATE_PRICE",
                                        chosenOption,
                                        Bank.format(minPrice),
                                        Bank.format(maxPrice) ),
                                        LocalText.getText("WHICH_PRICE"),
                                        JOptionPane.QUESTION_MESSAGE);
                    try {
                        amount = Integer.parseInt(price);
                    } catch (NumberFormatException e) {
                        amount = 0; // This will generally be refused.
                    }
                    chosenAction.setPrice(amount);
                } else {
                    chosenAction.setPrice(maxPrice);
                }
                if (orWindow.process(chosenAction)) {
                    updateMessage();
                }
            }
        }

    }

    public void executeUpgrade() {

        GUIHex selectedHex = map.getSelectedHex();

        // map correction override
        if (mapCorrectionEnabled) {
            if (selectedHex != null && selectedHex.getProvisionalTile() != null) {
                if (mapCorrectionAction.getStep() == ActionStep.SELECT_ORIENTATION) {
                    mapCorrectionAction.selectOrientation(selectedHex.getProvisionalTileRotation());
                } else if (mapCorrectionAction.getStep() == ActionStep.CONFIRM) {
                    mapCorrectionAction.selectConfirmed();
                }
                if (orWindow.process(mapCorrectionAction)) {
                    selectedHex.fixTile();
                } else {
                    selectedHex.removeTile();
                }
                map.selectHex(null);
            }
            return;
        }

        if (tileLayingEnabled) {
            if (selectedHex == null) {
                orWindow.displayORUIMessage(LocalText.getText("SelectAHexForToken"));
            } else if (selectedHex.getProvisionalTile() == null) {
                orWindow.displayORUIMessage(LocalText.getText("SelectATile"));
            } else {
                layTile();
            }
        } else if (tokenLayingEnabled) {
            if (selectedHex == null) {
                orWindow.displayORUIMessage(LocalText.getText("SelectAHexForTile"));
            } else if (selectedTokenAllowance == null) {
                orWindow.displayORUIMessage(LocalText.getText("SelectAToken"));
            } else if (selectedTokenAllowance instanceof LayBaseToken) {
                layBaseToken((LayBaseToken)selectedTokenAllowance);
            } else {
                layBonusToken(selectedTokenAllowance);
            }
        }
    }

    public void cancelUpgrade() {
        GUIHex selectedHex = mapPanel.getMap().getSelectedHex();

        // map correction override
        if (mapCorrectionEnabled) {
            if (selectedHex != null) selectedHex.removeTile();
            mapCorrectionAction.selectCancel();
            orWindow.process(mapCorrectionAction);
            map.selectHex(null);
            return;
        }

        if (tokenLayingEnabled) {
            if (selectedHex != null) selectedHex.removeToken();
            if (!localAction)
                orWindow.process(new NullAction(NullAction.SKIP));
        } else if (tileLayingEnabled) {
            if (selectedHex != null) selectedHex.removeTile();
            if (!localAction)
                orWindow.process(new NullAction(NullAction.SKIP));
        }

    }

    /** Default implementation.
     * The &lt;Loans&gt; attributes number and value <b>must</b>
     * have been configured in CompanyManager.xml */
    protected void takeLoans(TakeLoans action) {

        if (action.getMaxNumber() == 1) {

            String message = LocalText.getText("PleaseConfirm");
            String prompt = LocalText.getText("TakeLoanPrompt",
                    action.getCompanyName(),
                    Bank.format(action.getPrice()));
            if (JOptionPane.showConfirmDialog(orWindow, prompt,
                    message, JOptionPane.OK_CANCEL_OPTION,
                    JOptionPane.QUESTION_MESSAGE)
                    == JOptionPane.OK_OPTION) {
                action.setNumberTaken(1);
                orWindow.process(action);
            }

        } else {
            // For now we disregard the case of multiple loans
        }

    }

    protected void repayLoans (RepayLoans action) {

        int minNumber = action.getMinNumber();
        int maxNumber = action.getMaxNumber();
        int loanAmount = action.getPrice();
        int numberRepaid = 0;

        if (minNumber == maxNumber) {
            // No choice, just tell him
            JOptionPane.showMessageDialog (orWindow,
                    LocalText.getText("RepayLoan",
                            minNumber,
                            Bank.format(loanAmount),
                            Bank.format(minNumber * loanAmount)));
            numberRepaid = minNumber;
            action.setNumberTaken(numberRepaid);
            orWindow.process(action);
        } else {
            //List<String> options = new ArrayList<String>();
            String[] options = new String[maxNumber-minNumber+1];
            for (int i=minNumber, j=0; i<=maxNumber; i++, j++) {
                if (i == 0) {
                    options[j] = LocalText.getText("None");
                } else {
                    options[j] = LocalText.getText("RepayLoan",
                            i,
                            Bank.format(loanAmount),
                            Bank.format(i * loanAmount));
                }
            }
            RadioButtonDialog currentDialog = new RadioButtonDialog (REPAY_LOANS_DIALOG,
                    gameUIManager,
                    orWindow,
                    LocalText.getText("Select"),
                    LocalText.getText("SelectLoansToRepay", action.getCompanyName()),
                    options,
                    0);
            setCurrentDialog (currentDialog, action);
        }
    }

    /** Used to process some special properties from the 'Special' menu */
    /* In fact currently not used */
    protected void useSpecialProperty (UseSpecialProperty action) {

        gameUIManager.processAction(action);

    }

    protected void processCorrectionAction(CorrectionAction action) {

        gameUIManager.processAction(action);

    }


    public void updateStatus(boolean myTurn) {

        updateStatus(null, myTurn);

    }

    public void updateStatus(PossibleAction actionToComplete, boolean myTurn) {

        mapRelatedActions.clear();

        orPanel.resetActions();

        messagePanel.setMessage(null);

        if (actionToComplete != null) {
            log.debug("ExecutedAction: " + actionToComplete);
        }
        // End of possible action debug listing

        orStep = oRound.getStep();
        orComp = oRound.getOperatingCompany();
        log.debug("Or comp index = " + orCompIndex+" in round "+oRound.getRoundName());
        log.debug("OR company = " + orComp.getName());
        log.debug("OR step=" + orStep);

        if (oRound.getOperatingCompanyIndex() != orCompIndex) {
            if (orCompIndex >= 0) orPanel.finishORCompanyTurn(orCompIndex);

            // Check if sequence has changed
            checkORCompanySequence(companies, oRound.getOperatingCompanies());
            setORCompanyTurn(oRound.getOperatingCompanyIndex());
        }

        orPanel.initORCompanyTurn(orComp, orCompIndex);

        if (!myTurn) return;

        privatesCanBeBoughtNow = possibleActions.contains(BuyPrivate.class);
        orPanel.initPrivateBuying(privatesCanBeBoughtNow);

        // initialize operating costs actions
        orPanel.initOperatingCosts(possibleActions.contains(OperatingCost.class));

        // initial deactivation of MapTileCorrection Actions
        mapCorrectionEnabled = false;
        mapCorrectionAction = null;

        // initial deactivation of revenue calculation
        if (!possibleActions.contains(SetDividend.class)) {
            orPanel.stopRevenueUpdate();
            orPanel.resetCurrentRevenueDisplay();
        }

        if (possibleActions.contains(MapCorrectionAction.class)) {
            orPanel.initTileLayingStep();
            orWindow.requestFocus();

            MapCorrectionAction action = (possibleActions.getType(MapCorrectionAction.class)).get(0);

            mapCorrectionEnabled = true;
            mapCorrectionAction = action;
            updateUpgradesPanel(action);
        } else if (orStep == GameDef.OrStep.LAY_TRACK) {
            //if (possibleActions.contains(LayTile.class)) {

            orPanel.initTileLayingStep();

            orWindow.requestFocus();

            log.debug("Tiles can be laid");
            mapRelatedActions.addAll(possibleActions.getType(LayTile.class));

            //} else if (possibleActions.contains(LayBaseToken.class)) {
        } else if (orStep == GameDef.OrStep.LAY_TOKEN) {

            orWindow.requestFocus();

            // Include bonus tokens
            List<LayToken> possibleTokenLays =
                possibleActions.getType(LayToken.class);
            mapRelatedActions.addAll(possibleTokenLays);
            allowedTokenLays = possibleTokenLays;

            orPanel.initTokenLayingStep();

            log.debug("BaseTokens can be laid");

        } else if (possibleActions.contains(SetDividend.class)
                && localStep == SELECT_PAYOUT) {

            SetDividend action;
            if (actionToComplete != null) {
                action = (SetDividend) actionToComplete;
            } else {
                action = possibleActions.getType(SetDividend.class).get(0);
            }

            log.debug("Payout action before cloning: " + action);

            orPanel.initPayoutStep(orCompIndex, action,
                    action.isAllocationAllowed(SetDividend.WITHHOLD),
                    action.isAllocationAllowed(SetDividend.SPLIT),
                    action.isAllocationAllowed(SetDividend.PAYOUT));

            setMessage(LocalText.getText("SelectPayout"));

        } else if (possibleActions.contains(SetDividend.class)) {

            SetDividend action =
                possibleActions.getType(SetDividend.class).get(0);

            orPanel.initRevenueEntryStep(orCompIndex, action);

            String message = LocalText.getText("EnterRevenue");
            if (action.getRequiredCash() > 0) {
                message += "<br><font color=\"red\">"
                    + LocalText.getText("WarningNeedCash",
                            Bank.format(action.getRequiredCash()))
                            + "</font>";
            }
            setMessage(message);

        } else if (orStep == GameDef.OrStep.BUY_TRAIN) {

            boolean canBuyTrain = possibleActions.contains(BuyTrain.class);
            orPanel.initTrainBuying(canBuyTrain);

            StringBuffer b = new StringBuffer(LocalText.getText("BuyTrain"));

            // TEMPORARY extra message about having no route
            for (BuyTrain bTrain : possibleActions.getType(BuyTrain.class)) {
                if (bTrain.isForcedBuyIfNoRoute()) {
                    b.append("<br><font color=\"red\">");
                    b.append(LocalText.getText("MustBuyTrainIfNoRoute"));
                    b.append("</font>");
                    break;
                }
            }

            setMessage(b.toString());

        } else if (possibleActions.contains(DiscardTrain.class)) {

            gameUIManager.discardTrains(possibleActions.getType(DiscardTrain.class).get(0));

        } else if (possibleActions.contains(RepayLoans.class)) {

            orPanel.enableLoanRepayment (possibleActions.getType(RepayLoans.class).get(0));

        } else if (orStep == GameDef.OrStep.FINAL) {
            // Does not occur???
            orPanel.finishORCompanyTurn(orCompIndex);
        }

        if (possibleActions.contains(TakeLoans.class)) {
            orPanel.enableLoanTaking (possibleActions.getType(TakeLoans.class).get(0));
        }

        if (!mapCorrectionEnabled)
            setMapRelatedActions(mapRelatedActions);

        GameAction undoAction = null;
        GameAction redoAction = null;

        if (possibleActions.contains(NullAction.class)) {

            List<NullAction> actions =
                possibleActions.getType(NullAction.class);
            for (NullAction action : actions) {
                switch (action.getMode()) {
                case NullAction.DONE:
                    orPanel.enableDone(action);
                    break;
                }
            }
        }

        if (possibleActions.contains(GameAction.class)) {

            List<GameAction> actions =
                possibleActions.getType(GameAction.class);
            for (GameAction action : actions) {
                switch (action.getMode()) {
                case GameAction.UNDO:
                    undoAction = action;
                    break;
                case GameAction.REDO:
                    redoAction = action;
                    break;
                }
            }
        }
        orPanel.enableUndo(undoAction);
        orPanel.enableRedo(redoAction);

        orPanel.initSpecialActions();

        // Bonus tokens (and sometimes base tokens) can be laid anytime,
        // so we must also handle these outside the token laying step.
        if (possibleActions.contains(LayToken.class)
                && orStep != GameDef.OrStep.LAY_TOKEN) {

            List<LayToken> tokenActions =
                possibleActions.getType(LayToken.class);
            for (LayToken tAction : tokenActions) {

                if (tAction instanceof LayBaseToken
                        && ((LayBaseToken)tAction).getType() == LayBaseToken.HOME_CITY) {
                    // Forced action: select home city
                    LayBaseToken lbt = (LayBaseToken)tAction;
                    map.setSelectedHex(map.getHexByName(lbt.getChosenHex().getName()));
                    layBaseToken (lbt);
                    return;

                }
                SpecialTokenLay stl = tAction.getSpecialProperty();
                if (stl != null) orPanel.addSpecialAction(tAction, stl.toMenu());
            }
        }

        // Can bonus tokens be bought?
        if (possibleActions.contains(BuyBonusToken.class)) {

            List<BuyBonusToken> bonusTokenActions =
                possibleActions.getType(BuyBonusToken.class);
            for (BuyBonusToken bbt : bonusTokenActions) {
                String text =
                    LocalText.getText("BuyBonusToken",
                            bbt.getName(),
                            Bank.format(bbt.getValue()),
                            bbt.getSellerName(),
                            Bank.format(bbt.getPrice()) );
                orPanel.addSpecialAction(bbt, text);
            }
        }

        if (possibleActions.contains(ReachDestinations.class)) {
            orPanel.addSpecialAction(possibleActions.getType(ReachDestinations.class).get(0),
                    LocalText.getText("DestinationsReached"));
        }

        // Any other special properties, to be shown in the "Special" menu.
        // Example: 18AL AssignNamedTrains
        if (possibleActions.contains(UseSpecialProperty.class)) {
            for (UseSpecialProperty usp : possibleActions.getType(UseSpecialProperty.class)) {
                SpecialPropertyI sp = usp.getSpecialProperty();
                orPanel.addSpecialAction(usp, sp.toMenu());
            }
        }

        // Close Private
        if (possibleActions.contains(ClosePrivate.class)) {
            for (ClosePrivate action: possibleActions.getType(ClosePrivate.class)) {
                orPanel.addSpecialAction(action, action.getInfo());
            }
        }


        checkForGameSpecificActions();

        orPanel.redisplay();
    }

    /** Stub, can be overridden by game-specific subclasses */
    protected void checkForGameSpecificActions() {

    }

    /** Redraw the ORPanel if the company operating order has changed */
    protected void checkORCompanySequence (PublicCompanyI[] oldCompanies, List<PublicCompanyI> newCompanies) {
        for (int i=0; i<newCompanies.size(); i++) {
            if (newCompanies.get(i) != oldCompanies[i]) {
                log.debug("Detected a OR company sequence change: "+oldCompanies[i].getName()
                        +" becomes "+newCompanies.get(i).getName());
                orPanel.recreate(oRound);
                break;
            }
        }
        return;
    }

    public void setORCompanyTurn(int orCompIndex) {

        this.orCompIndex = orCompIndex;
        orComp = orCompIndex >= 0 ? companies[orCompIndex] : null;

        if (orCompIndex >= 0) {
            // Give a new company the turn.
        }
    }

    public void setLocalStep(int localStep) {
        log.debug("Setting upgrade step to " + localStep + " "
                + ORUIManager.messageKey[localStep]);

        SoundManager.notifyOfORLocalStep(localStep);

        this.localStep = localStep;

        updateMessage();
        updateUpgradesPanel();
    }

    public void updateUpgradesPanel() {

        if (upgradePanel != null) {
            log.debug("Initial localStep is " + localStep + " "
                    + ORUIManager.messageKey[localStep]);
            switch (localStep) {
            case INACTIVE:
                upgradePanel.setTileUpgrades(null);
                upgradePanel.setPossibleTokenLays(null);
                upgradePanel.setTokenMode(false);
                upgradePanel.setDoneEnabled(false);
                upgradePanel.setCancelEnabled(false);
                break;
            case SELECT_HEX_FOR_TILE:
                upgradePanel.setDoneText("LayTile");
                upgradePanel.setCancelText("NoTile");
                upgradePanel.setDoneEnabled(false);
                upgradePanel.setCancelEnabled(true);
                break;
            case SELECT_TILE:
                upgradePanel.populate(gameUIManager.getCurrentPhase());
                upgradePanel.setDoneEnabled(false);
                break;
            case ROTATE_OR_CONFIRM_TILE:
                upgradePanel.setDoneEnabled(true);
                break;
            case SELECT_HEX_FOR_TOKEN:
                upgradePanel.setTileUpgrades(null);
                upgradePanel.setDoneEnabled(false);
                upgradePanel.setCancelEnabled(true);
                upgradePanel.setDoneText("LayToken");
                upgradePanel.setCancelText("NoToken");
                break;
            case SELECT_TOKEN:
                List<LayToken> allowances =
                    map.getTokenAllowanceForHex(mapPanel.getMap().getSelectedHex().getHexModel());
                log.debug("Allowed tokens for hex "
                        + mapPanel.getMap().getSelectedHex().getName()
                        + " are:");
                for (LayToken allowance : allowances) {
                    log.debug("  " + allowance.toString());
                }
                upgradePanel.setPossibleTokenLays(allowances);
                if (allowances.size() > 1) {
                    upgradePanel.setDoneEnabled(false);
                    break;
                } else {
                    // Only one token possible: skip this step and fall through
                    tokenSelected(allowances.get(0));
                    localStep = CONFIRM_TOKEN;
                }
            case CONFIRM_TOKEN:
                upgradePanel.setDoneEnabled(true);
                break;
            case MAP_CORRECTION:
                return; // this is done in their own initialization
            default:
                upgradePanel.setDoneEnabled(false);
                upgradePanel.setCancelEnabled(false);
                break;
            }
        }
        log.debug("Final localStep is " + localStep + " "
                + messageKey[localStep]);
        upgradePanel.showUpgrades(); // ??

    }

    public void updateUpgradesPanel(MapCorrectionAction action) {
        setLocalStep(MAP_CORRECTION);
        GUIHex selectedHex = map.getSelectedHex();

        boolean showTilesInUpgrade = true;
        switch (action.getStep()) {
        case SELECT_HEX:
            // done text will be used by token lay
            upgradePanel.setDoneText("LayTile");
            upgradePanel.setDoneEnabled(false);
            upgradePanel.setCancelText("Cancel");
            upgradePanel.setCancelEnabled(false);
            tileUpgrades = new ArrayList<TileI>();
            showTilesInUpgrade = true;
            // checks if there is still a hex selected (due to errors)
            if (selectedHex != null) {
                map.selectHex(null);
            }
            break;
        case SELECT_TILE:
            upgradePanel.setDoneText("LayTile");
            upgradePanel.setDoneEnabled(false);
            upgradePanel.setCancelText("Cancel");
            upgradePanel.setCancelEnabled(true);
            tileUpgrades = action.getTiles();
            showTilesInUpgrade = true;
            // checks if there is still a tile displayed (due to errors)
            if (selectedHex != null && selectedHex.canFixTile()){
                selectedHex.removeTile();
            }
            // activate hex selection
            map.selectHex(null);
            map.selectHex(map.getHexByName(action.getLocation().getName()));
            break;
        case SELECT_ORIENTATION:
            upgradePanel.setDoneText("LayTile");
            upgradePanel.setDoneEnabled(true);
            upgradePanel.setCancelText("Cancel");
            upgradePanel.setCancelEnabled(true);
            // next step already set to finished => preprinted tile with fixed orientation
            //            if (action.getNextStep() == MapCorrectionManager.ActionStep.FINISHED) {
            //                selectedHex.setTileOrientation(action.getOrientation());
            //                map.repaint(selectedHex.getBounds());
            //                if (orWindow.process(mapCorrectionAction)) {
            //                    selectedHex.fixTile();
            //                } else {
            //                    selectedHex.removeTile();
            //                }
            //                map.selectHex(null);
            //                return;            }

            //            }
            showTilesInUpgrade = true;
            break;
        case RELAY_BASETOKENS:
            // define open slots for each station
            List<Station> possibleStations = new ArrayList<Station>(action.getPossibleStations());
            Map<Station, Integer> openSlots = new HashMap<Station, Integer>();
            for (Station station:possibleStations) {
                openSlots.put(station, station.getBaseSlots());
            }
            // ask for the new stations
            List<Station> chosenStations = new ArrayList<Station>();
            for (BaseToken token:action.getTokensToRelay()) {
                Station chosenStation = correctionRelayBaseToken(token, possibleStations);
                chosenStations.add(chosenStation);
                // check the number of available slots
                openSlots.put(chosenStation, openSlots.get(chosenStation) - 1);
                if (openSlots.get(chosenStation) == 0) {
                    possibleStations.remove(chosenStation);
                }
            }
            action.selectRelayBaseTokens(chosenStations);
            if (orWindow.process(mapCorrectionAction)) {
                selectedHex.fixTile();
            } else {
                selectedHex.removeTile();
            }
            map.selectHex(null);
            return;
        case CONFIRM:
            // only wait for laytile or cancel command, for those lays that do not need adjustment of orientation
            upgradePanel.setDoneText("LayTile");
            upgradePanel.setDoneEnabled(true);
            upgradePanel.setCancelText("Cancel");
            upgradePanel.setCancelEnabled(true);
            showTilesInUpgrade = true;
        }

        log.debug("Active map tile correction");
        if (showTilesInUpgrade) {
            upgradePanel.showCorrectionTileUpgrades();
        } else {
            upgradePanel.showCorrectionTokenUpgrades(action);
        }
    }

    public void setMessage(String message) {
        messagePanel.setMessage(message);
    }

    public void setInformation(String infoText) {
        messagePanel.setInformation(infoText);
    }

    public void setDetail(String detailText) {
        messagePanel.setDetail(detailText);
    }

    public void setLocalAction(boolean value) {
        localAction = value;
    }

    // TEMPORARY
    public ORWindow getORWindow() {
        return orWindow;
    }

    // TEMPORARY
    public MapPanel getMapPanel() {
        return orWindow.getMapPanel();
    }

    // TEMPORARY
    public HexMap getMap() {
        return map;
    }

    // TEMPORARY
    public ORPanel getORPanel() {
        return orPanel;
    }

    public RemainingTilesWindow getRemainingTilesWindows() {
        return remainingTiles;
    }
    
    public void setRemainingTilesWindows(RemainingTilesWindow rtw) {
        remainingTiles = rtw;
    }
    
    public GameUIManager getGameUIManager () {
        return gameUIManager;
    }

    public TileManager getTileManager() {
        return tileManager;
    }

    private void displayRemainingTiles() {

        if (remainingTiles == null) {
            remainingTiles = new RemainingTilesWindow(orWindow);
        } else {
            remainingTiles.activate();
        }
    }

}
