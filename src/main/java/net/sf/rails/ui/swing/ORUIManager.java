package net.sf.rails.ui.swing;

import java.util.*;

import javax.swing.JDialog;
import javax.swing.JOptionPane;

import net.sf.rails.algorithms.*;
import net.sf.rails.common.*;
import net.sf.rails.game.*;
import net.sf.rails.game.special.*;
import net.sf.rails.game.state.Owner;
import net.sf.rails.sound.SoundManager;
import net.sf.rails.ui.swing.elements.*;
import net.sf.rails.ui.swing.hexmap.GUIHex;
import net.sf.rails.ui.swing.hexmap.HexMap;
import net.sf.rails.util.Util;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import rails.game.action.*;
import rails.game.correct.*;

import com.google.common.collect.HashMultimap;
import com.google.common.collect.Iterables;
import com.google.common.collect.Lists;
import com.google.common.collect.Multimap;
import com.google.common.collect.SetMultimap;
import com.google.common.collect.Sets;

// FIXME: Add back corrections mechanisms

public class ORUIManager implements DialogOwner {

    private static Logger log =
            LoggerFactory.getLogger(ORUIManager.class);

    protected GameUIManager gameUIManager;

    protected ORWindow orWindow;
    protected ORPanel orPanel;
    private UpgradesPanel upgradePanel;
    private MapPanel mapPanel;
    private HexMap map;
    private MessagePanel messagePanel;
    private RemainingTilesWindow remainingTiles;

    private OperatingRound oRound;
    private PublicCompany[] companies;
    private PublicCompany orComp;
    private int orCompIndex;

    private GameDef.OrStep orStep;
    private LocalSteps localStep;

    private boolean privatesCanBeBoughtNow;

    private final SetMultimap<GUIHex, TileHexUpgrade> tileUpgrades = HashMultimap.create();

    private final SetMultimap<GUIHex, TokenStopUpgrade> tokenUpgrades = HashMultimap.create(); 

    /* Local substeps */
    public static enum LocalSteps {
        Inactive, SelectAHexForTile, SelectATile, RotateTile, SelectAHexForToken
        , SelectAToken, ConfirmToken, SetRevenue, SelectPayout }

    /* Keys of dialogs owned by this class */
    public static final String SELECT_DESTINATION_COMPANIES_DIALOG = "SelectDestinationCompanies";
    public static final String REPAY_LOANS_DIALOG = "RepayLoans";

    public ORUIManager() {

    }

    void setGameUIManager (GameUIManager gameUIManager) {
        this.gameUIManager = gameUIManager;
    }

    void init(ORWindow orWindow) {

        this.orWindow = orWindow;

        orPanel = orWindow.getORPanel();
        mapPanel = orWindow.getMapPanel();
        upgradePanel = orWindow.getUpgradePanel();
        map = mapPanel.getMap();
        messagePanel = orWindow.getMessagePanel();

    }

    void initOR(OperatingRound or) {
        oRound = or;
        companies = (oRound).getOperatingCompanies().toArray(new PublicCompany[0]);
        map.updateOffBoardToolTips(); // FIXME: This method is deleted
        orWindow.activate(oRound);
    }

    public void finish() {
        orWindow.finish();
        upgradePanel.setState(UpgradesPanel.States.Inactive);
        upgradePanel.init();
        if (!(gameUIManager.getCurrentRound() instanceof ShareSellingRound)) {
            setORCompanyTurn(-1);
        }
    }

    public void setMapRelatedActions(PossibleActions actions) {

        GUIHex selectedHex = mapPanel.getMap().getSelectedHex();
        LocalSteps nextSubStep = LocalSteps.Inactive;

        List<LayTile> tileActions = actions.getType(LayTile.class);
        List<LayToken> tokenActions = actions.getType(LayToken.class);
        
        if (tileActions.isEmpty() & !tileUpgrades.isEmpty()) {
            /* Finish tile laying step */
            if (selectedHex != null) {
                selectedHex.removeTile();
                selectedHex.setSelected(false);
                selectedHex = null;
            }
            // remove selectable indications
            for (GUIHex guiHex:tileUpgrades.keySet()) {
                guiHex.setSelectable(false);
            }
            tileUpgrades.clear();
        }

        if (tokenActions.isEmpty() && !tokenUpgrades.isEmpty()) {
            /* Finish token laying step */
            if (selectedHex != null) {
                selectedHex.removeToken();
                selectedHex.setSelected(false);
                selectedHex = null;
            }
            // remove selectable indications
            for (GUIHex guiHex:tokenUpgrades.keySet()) {
                guiHex.setSelectable(false);
            }
            tokenUpgrades.clear();
        }

        if (!tileActions.isEmpty()) {
            nextSubStep = LocalSteps.SelectAHexForTile;
            if (tileUpgrades != null) {
                for (GUIHex guiHex:tileUpgrades.keySet()) {
                    guiHex.setSelectable(false);
                }
            }

            tileUpgrades.clear();
            defineTileUpgrades(tileActions);
        }

        if (tokenActions.size() > 0) {
            nextSubStep = LocalSteps.SelectAHexForToken;
            // if  hexupgrades is not null, then remove indicators
            if (tokenUpgrades != null) {
                for (GUIHex guiHex:tokenUpgrades.keySet()) {
                    guiHex.setSelectable(false);
                }
            }

            tokenUpgrades.clear();
            defineTokenUpgrades(tokenActions);
        }

        setLocalStep(nextSubStep);
    }
    
    private void defineTileUpgrades(List<LayTile> actions) {
        for (LayTile layTile:actions) {
            switch (layTile.getType()) {
            case (LayTile.GENERIC):
                addGenericTileLays(layTile);
                break;
            case (LayTile.SPECIAL_PROPERTY):
                SpecialProperty sp = layTile.getSpecialProperty();
                if (sp == null || !(sp instanceof SpecialTileLay) ||
                        ((SpecialTileLay)sp).requiresConnection()) {
                    break;
                }
                // else fall through
            case (LayTile.LOCATION_SPECIFIC):
                if (layTile.getLocations() != null) {
                    addLocatedTileLays(layTile);
                } else {
                    addGenericTileLays(layTile);
                }
            default:
            }
        }

        // show selectable hexes if highlight is active
        if (gameUIManager.getGameParameterAsBoolean(GuiDef.Parm.ROUTE_HIGHLIGHT)) {
            for (GUIHex hex:tileUpgrades.keySet()) {
                for (TileHexUpgrade upgrade:tileUpgrades.get(hex)) {
                    if (upgrade.isValid()) {
                        hex.setSelectable(true);
                        break;
                    }
                }
            }
        }
    }

    private void addGenericTileLays(LayTile layTile) {
        NetworkGraph graph = getCompanyGraph();
        Map<MapHex, HexSidesSet> mapHexSides = graph.getReachableSides();
        Multimap<MapHex, Station> mapHexStations = graph.getPassableStations();
        
        for (MapHex hex:Sets.union(mapHexSides.keySet(), mapHexStations.keySet())) {
            GUIHex guiHex = map.getHex(hex);
            String routeAlgorithm = GameOption.getValue(gameUIManager.getRoot(), "RouteAlgorithm");
            Set<TileHexUpgrade> upgrades = TileHexUpgrade.create(hex, mapHexSides.get(hex), 
                    mapHexStations.get(hex), layTile, routeAlgorithm);
            TileHexUpgrade.validateAndEnable(upgrades, gameUIManager.getCurrentPhase());
            tileUpgrades.putAll(guiHex, upgrades);
        }
        log.debug("tileUpgrades = " + tileUpgrades);
        
        // scroll map to center over companies network
        String autoScroll = Config.getGameSpecific("map.autoscroll");
        if (Util.hasValue(autoScroll) &&  autoScroll.equalsIgnoreCase("no")) {
            // do nothing
        } else {
            mapPanel.scrollPaneShowRectangle(
                    NetworkVertex.getVertexMapCoverage(map, graph.getGraph().vertexSet()));
        }
    }
    
    private void addLocatedTileLays(LayTile layTile) {
        for (MapHex hex:layTile.getLocations()) {
            GUIHex guiHex = map.getHex(hex);
            Set<TileHexUpgrade> upgrades = TileHexUpgrade.createLocated(hex, layTile);
            TileHexUpgrade.validateAndEnable(upgrades, gameUIManager.getCurrentPhase());
            tileUpgrades.putAll(guiHex, upgrades);
        }
    }

    private void defineTokenUpgrades(List<LayToken> actions) {

        NetworkGraph graph = getCompanyGraph();
        List<Stop> stops = graph.getTokenableStops(orComp);

        for (LayToken layToken:actions) {
            if (layToken instanceof LayBaseToken) {
                LayBaseToken layBaseToken = (LayBaseToken)layToken;
                switch (layBaseToken.getType()) {
                case (LayBaseToken.GENERIC):
                    addGenericTokenLays(layToken, stops);
                    break;
                case (LayBaseToken.LOCATION_SPECIFIC):
                case (LayBaseToken.SPECIAL_PROPERTY):
                    if (layBaseToken.getLocations() != null) {
                        addLocatedTokenLays(layBaseToken);
                    } else {
                        addGenericTokenLays(layBaseToken, stops);
                    }
                break;
                case (LayBaseToken.HOME_CITY):
                    addLocatedTokenLays(layBaseToken);
                break;
                default:
                }
            }
        }
        
        // FIXME: Add highlighting for all available tokens
        if (gameUIManager.getGameParameterAsBoolean(GuiDef.Parm.ROUTE_HIGHLIGHT)) {
            for (GUIHex hex:tokenUpgrades.keySet()) {
                hex.setSelectable(true);
            }
        }    
        // standard tokens
        log.debug("tokenUpgrades = " + tokenUpgrades);
    }

    private void addGenericTokenLays(LayToken action, List<Stop> tokenableStops) {
        for (Stop stop:tokenableStops) {
            MapHex hex = stop.getParent();
            TokenStopUpgrade upgrade = TokenStopUpgrade.create(stop, action);
            GUIHex guiHex = map.getHex(hex);
            tokenUpgrades.put(guiHex, upgrade);
        }
    }
    
    private void addLocatedTokenLays(LayToken action) {
        for (MapHex hex:action.getLocations()) {
            for (Stop stop:hex.getStops()) {
                TokenStopUpgrade upgrade = TokenStopUpgrade.create(stop, action);
                GUIHex guiHex = map.getHex(hex);
                tokenUpgrades.put(guiHex, upgrade);
            }
        }
    }
    
    public void updateMessage() {

        // For now, this only has an effect during tile and token laying.
        // Perhaps we need to centralise message updating here in a later stage.
        log.debug("Calling updateMessage, subStep=" + localStep/*
         * , new
         * Exception("TRACE")
         */);
        if (localStep == LocalSteps.Inactive) return;

        String message = LocalText.getText(localStep.toString());
        SpecialProperty sp;

        /* Add any extra messages */
        String extraMessage = "";

        // FIXME: Add this text again on rewrite of the Correction Mechanism
//        if (localStep == ORUIManager.MAP_CORRECTION) {
//            if (mapCorrectionAction != null)
//                extraMessage = LocalText.getText("CorrectMap" + mapCorrectionAction.getStep().name());
//        }

        if (localStep == LocalSteps.SelectAHexForTile) {
            /* Compose prompt for tile laying */
            StringBuffer normalTileMessage = new StringBuffer(" ");

            List<LayTile> tileLays = getPossibleActions().getType(LayTile.class);
            int ii = 0;
            for (LayTile tileLay : tileLays) {
                Map<String, Integer> tileColours;
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

        } else if (localStep == LocalSteps.SelectAHexForToken) {

            /* Compose prompt for token laying */
            String locations;
            StringBuffer normalTokenMessage = new StringBuffer(" ");

            List<LayBaseToken> tokenLays =
                getPossibleActions().getType(LayBaseToken.class);
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
    // FIXME: Can this be really a list of actions?
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
                // FIXME: This has to be redefined to be able to use all types of token lays
//                LayBaseToken lbt = (LayBaseToken) actions.get(0);
//                map.selectHex(map.getHex(lbt.getLocations().get(0)));
//                layBaseToken (lbt);

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
    }

    /** Stub, can be overridden in subclasses */
    // FIXME: As above, really a list of actions?
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
                setLocalStep(LocalSteps.SelectPayout);
                updateStatus(action, true);

                // Locally update revenue if we don't inform the server yet.
                orPanel.setRevenue(orCompIndex, amount);
            }
        } else {
            // The revenue allocation has been selected
            orWindow.process(action);
        }
    }

    // FIXME: Rewrite of Bonus Tokens
    private void prepareBonusToken(LayBonusToken action) {

        // 
//        orWindow.requestFocus();
//
//        allowedTokenLays.clear();
//        allowedTokenLays.add(action);
//        setMapRelatedActions(allowedTokenLays);
//        setLocalAction(true);
//
//        log.debug("BonusTokens can be laid");
//
//        mapPanel.setAllowedTokenLays(allowedTokenLays);
//
//        orPanel.initTokenLayingStep();

    }

    private void buyBonusToken (BuyBonusToken action) {

        orWindow.process(action);
    }

    protected void reachDestinations (ReachDestinations action) {

        List<String> options = new ArrayList<String>();
        List<PublicCompany> companies = action.getPossibleCompanies();

        for (PublicCompany company : companies) {
            options.add(company.getId());
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

        // FIXME: Map Correction removed
//        if (mapCorrectionEnabled) {
//            triggerORPanelRepaint = true;
//            boolean checkClickedHex = false;
//            switch (mapCorrectionAction.getStep()) {
//            case SELECT_HEX:
//            case SELECT_TILE: // if tile is selected
//                checkClickedHex = true;
//                break;
//            case SELECT_ORIENTATION:
//                if (clickedHex == selectedHex) {
//                    selectedHex.forcedRotateTile();
//                } else
//                    checkClickedHex = true;
//                break;
//            }
//            if (checkClickedHex && clickedHex !=null && clickedHex != selectedHex) {
//                map.selectHex(clickedHex);
//                mapCorrectionAction.selectHex(clickedHex.getHex());
//                orWindow.process(mapCorrectionAction);
//            }
//        } else if (tokenLayingEnabled) {
        if (!tokenUpgrades.isEmpty()) {
            triggerORPanelRepaint = true;
            // if clickedHex == null, then go back to select hex step
            if (clickedHex == null) {
                upgradePanel.init();
                setLocalStep(LocalSteps.SelectAHexForToken);
                return true;
            }
            Set<TokenStopUpgrade> upgrades = tokenUpgrades.get(clickedHex);
            if (!upgrades.isEmpty()) {
                map.selectHex(clickedHex);
                setLocalStep(LocalSteps.SelectAToken);
            } else {
                JOptionPane.showMessageDialog(mapPanel, LocalText.getText(
                        "NoTokenPossible", clickedHex.toText()));
                setLocalStep(LocalSteps.SelectAHexForToken);
            }

        } else if (!tileUpgrades.isEmpty()) {
            triggerORPanelRepaint = true;
            if (localStep == LocalSteps.RotateTile
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
                    upgradePanel.init();
                    setLocalStep(LocalSteps.SelectAHexForTile);
                } else {
                    map.selectHex(clickedHex);
                    setLocalStep(LocalSteps.SelectATile);
                }
            }
        }

        if (triggerORPanelRepaint) orWindow.repaintORPanel();

        return triggerORPanelRepaint;
    }

    public void tileSelected(TileHexUpgrade upgrade) {
        Tile tile = upgrade.getUpgrade().getTargetTile();
        GUIHex hex = map.getSelectedHex();
        
        // if tile already selected, then it is identical as if the hex was clicked again
        // this activates the rotation step
        // FIXME: Check if this still works
        if (localStep == LocalSteps.RotateTile && hex.getProvisionalTile() == tile) {
            hexClicked(hex, hex);
            upgradePanel.showUpgrades();
            return;
        }

        // FIXME: Solve that during rewrite of Map Correction
        // map correction override
//        if (mapCorrectionEnabled) {
//            // paint tile
//            hex.forcedDropTile(upgrade, HexSide.defaultRotation());
//            // inform map correction manager
//            mapCorrectionAction.selectTile(tile);
//            orWindow.process(mapCorrectionAction);
//            return;
//        }

//        if (hex.dropTile(upgrade)) {
            /* Lay tile */
        hex.dropTile(upgrade);
        setLocalStep(LocalSteps.RotateTile);
//        } else {
//            // FIXME: This should not happen anymore
//            /* Tile cannot be laid in a valid orientation: refuse it */
//            JOptionPane.showMessageDialog(mapPanel,
//            "This tile cannot be laid in a valid orientation.");
//            upgradePanel.removeUpgrade(upgrade);
//            setLocalStep(ORUIManager.SELECT_TILE);
//            upgradePanel.showUpgrades(localStep);
//        }
    }

    void layTile() {

        GUIHex selectedHex = map.getSelectedHex();
        if (selectedHex != null && selectedHex.canFixTile()) {
            TileHexUpgrade upgrade = selectedHex.getUpgrade();
            LayTile allowance = upgrade.getAction();
            // FIXME: Removed a lot of checks here
//            List<LayTile> allowances =
//                map.getTileAllowancesForHex(selectedHex.getHexModel());
//            LayTile allowance = null;
//            Tile tile = selectedHex.getProvisionalTile();
//            if (allowances.size() == 1) {
//                allowance = allowances.get(0);
//            } else {
//                // Check which allowance applies
//                // We'll restrict to cases where we have both a special property
//                // and a normal 'blanket' allowance.
//                // First check which is which.
//                List<Tile> sp_tiles;
//                List<MapHex> sp_hexes;
//                LayTile gen_lt = null;
//                LayTile spec_lt = null;
//                for (LayTile lt : allowances) {
//                    if (lt.getType() == LayTile.SPECIAL_PROPERTY) {
//                        // Cases where a special property is used include:
//                        // 1. SP refers to specified tiles, (one of) which is chosen:
//                        // (examples: 18AL Lumber Terminal, 1889 Port)
//                        if ((sp_tiles = lt.getTiles()) != null
//                                && !sp_tiles.contains(tile)) continue;
//                        // 2. SP refers to specified hexes, (one of) which is chosen:
//                        // (example: 1830 hex B20)
//                        if ((sp_hexes = lt.getLocations()) != null
//                                && !sp_hexes.contains(selectedHex.getHexModel())) continue;
//                        spec_lt = lt;
//                    } else {
//                        // Default case: the generic allowance
//                        gen_lt = lt;
//                    }
//                }
//
//                allowance = spec_lt == null ? gen_lt :
//                    gen_lt == null ? spec_lt :
//                        spec_lt.getSpecialProperty().getPriority()
//                                == SpecialProperty.Priority.FIRST ? spec_lt : gen_lt;
//
//            }
            allowance.setChosenHex(selectedHex.getHex());
            int orientation = selectedHex.getProvisionalTileRotation().getTrackPointNumber();
            allowance.setOrientation(orientation);
            allowance.setLaidTile(upgrade.getUpgrade().getTargetTile());

            relayBaseTokens (allowance);

            if (orWindow.process(allowance)) {
                selectedHex.fixTile();
            } else {
                selectedHex.removeTile();
                setLocalStep(LocalSteps.SelectAHexForTile);
            }
            map.selectHex(null);
        }
    }

    public void layBaseToken(TokenStopUpgrade upgrade) {
        Stop stop = upgrade.getLocation();
        LayBaseToken action = (LayBaseToken) upgrade.getAction();
        
        action.setChosenHex(stop.getParent());
        action.setChosenStation(stop.getRelatedNumber());
        
        log.debug("Token action is: " + action);

        if (orWindow.process(action)) {
            upgradePanel.init();
            map.getSelectedHex().fixToken();
        } else {
            setLocalStep(LocalSteps.SelectAHexForToken);
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
    // FIXME: This has to be rewritten with the new tile mechanism
    protected void relayBaseTokens (LayTile action) {

        final MapHex hex = action.getChosenHex();
        Tile newTile = action.getLaidTile();
        Tile oldTile = hex.getCurrentTile();
        if (!action.isRelayBaseTokens()
                && !oldTile.relayBaseTokensOnUpgrade()) return;

        List<Stop> stopsToQuery = Lists.newArrayList(hex.getStops());

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
         * 
         * TODO: (Rails2.0) Check if this still works
         */
        if (stopsToQuery.size() == 2) {
            Collections.sort(stopsToQuery, new Comparator<Stop>() {
                public int compare (Stop s1, Stop s2) {
                    Set<BaseToken> tokens;
                    boolean stop1IsHome = !((tokens = s1.getBaseTokens()).isEmpty())
                    && Iterables.get(tokens, 0).getParent().getHomeHexes().contains(hex);
                    boolean stop2IsHome = !((tokens = s2.getBaseTokens()).isEmpty())
                    && Iterables.get(tokens, 0).getParent().getHomeHexes().contains(hex);
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
                // TODO: Rewrite this to make this code nicer
                PublicCompany company = (Iterables.get(oldStop.getBaseTokens(), 0)).getParent();

                List<String> prompts = new ArrayList<String>();
                Map<String, Integer> promptToCityMap = new HashMap<String, Integer>();
                String prompt;
                for (Station newStation : newTile.getStations()) {
                    if (newStation.getBaseSlots() > 0 && freeSlots[newStation.getNumber()] > 0) {
                        prompt = LocalText.getText("SelectStationForTokenOption",
                                newStation.getNumber(),
                                TrackConfig.getConnectionString(hex, newTile, 
                                        action.getOrientation(), newStation));
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
                                        company.getPresident().getId(),
                                        hex.getId(),
                                        company.getId()
                                ),
                                LocalText.getText("WhichStation"),
                                JOptionPane.PLAIN_MESSAGE, null,
                                prompts.toArray(), prompts.get(0));
                    if (selected == null) return;
                    action.addRelayBaseToken(company.getId(), promptToCityMap.get(selected));
                    --freeSlots[promptToCityMap.get(selected)];
                } else {
                    action.addRelayBaseToken(company.getId(), promptToCityMap.get(prompts.toArray() [0]));
                    --freeSlots[promptToCityMap.get(prompts.toArray()[0])];
                }
            }
        }
    }

    
    // FIXME: Might be required for the Correction Relay
//    private Station correctionRelayBaseToken(BaseToken token, List<Station> possibleStations){
//        GUIHex selectedHex = map.getSelectedHex();
//
//        PublicCompany company = token.getParent();
//        List<String> prompts = new ArrayList<String>();
//
//        Map<String, Station> promptToStationMap = new HashMap<String, Station>();
//        String prompt;
//        for (Station station:possibleStations) {
//            prompt = LocalText.getText(
//                    "SelectStationForTokenOption",
//                    station.getNumber(),
//                    selectedHex.getHex().getConnectionString(station));
//            prompts.add(prompt);
//            promptToStationMap.put(prompt, station);
//        }
//        String selected =
//            (String) JOptionPane.showInputDialog(orWindow,
//                    LocalText.getText("SelectStationForToken",
//                            "",
//                            selectedHex.toText(),
//                            company.getId()),
//                            LocalText.getText("WhichStation"),
//                            JOptionPane.PLAIN_MESSAGE, null,
//                            prompts.toArray(), prompts.get(0));
//        if (selected == null) return null;
//        Station station = promptToStationMap.get(selected);
//        return station;
//    }


    /**
     * Lay Token finished.
     *
     * @param action The LayBonusToken action object of the laid token.
     */
    // FIXME: This has to be rewritten
    public void layBonusToken(TokenStopUpgrade upgrade) {

        LayToken action = upgrade.getAction();
        
        // Assumption for now: always BonusToken
        // We might use it later for BaseTokens too.

        HexMap map = mapPanel.getMap();
        GUIHex selectedHex = map.getSelectedHex();

        if (selectedHex != null) {
            LayToken executedAction = (LayToken) action;

            executedAction.setChosenHex(selectedHex.getHex());

            if (orWindow.process(executedAction)) {
                upgradePanel.init();
                map.selectHex(null);
                //ensure painting the token (model update currently does not arrive at UI)
                map.repaintTokens(selectedHex.getBounds());
                selectedHex = null;
            }
        }
    }

    public void operatingCosts(){

        List<String> textOC = new ArrayList<String>();
        List<OperatingCost> actionOC = getPossibleActions().getType(OperatingCost.class);

        for (OperatingCost ac:actionOC) {

            String suggestedCostText;
            if (ac.isFreeEntryAllowed())
                suggestedCostText = LocalText.getText("OCAmountEntry");
            else
                suggestedCostText = gameUIManager.format(ac.getAmount());

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
        Train train;
        String usingPrivates = "";

        PossibleAction selectedAction;
        BuyTrain buyAction;

        String prompt;
        StringBuffer b;
        int cost;
        Owner from;

        List<BuyTrain> buyableTrains = getPossibleActions().getType(BuyTrain.class);
        for (BuyTrain bTrain : buyableTrains) {
            train = bTrain.getTrain();
            cost = bTrain.getFixedCost();
            from = bTrain.getFromOwner();

            /* Create a prompt per buying option */
            b = new StringBuffer();

            b.append(LocalText.getText("BUY_TRAIN_FROM",
                    bTrain.getType(),
                    from.getId() ));
            if (bTrain.isForExchange()) {
                b.append(" (").append(LocalText.getText("EXCHANGED")).append(
                ")");
            }
            if (cost > 0) {
                b.append(" ").append(
                        LocalText.getText("AT_PRICE", gameUIManager.format(cost)));
            }
            if (bTrain.hasSpecialProperty()) {
                String priv =
                        (bTrain.getSpecialProperty()).getOriginalCompany().getId();
                b.append(" ").append(LocalText.getText("USING_SP", priv));
                usingPrivates += ", " + priv;
            }
            if (bTrain.mustPresidentAddCash()) {
                b.append(" ").append(
                        LocalText.getText("YOU_MUST_ADD_CASH",
                                gameUIManager.format(bTrain.getPresidentCashToAdd())));
            } else if (bTrain.mayPresidentAddCash()) {
                b.append(" ").append(
                        LocalText.getText("YOU_MAY_ADD_CASH",
                                gameUIManager.format(bTrain.getPresidentCashToAdd())));
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
        Owner seller = buyAction.getFromOwner();
        int price = buyAction.getFixedCost();

        if (price == 0 && seller instanceof PublicCompany) {
            prompt = LocalText.getText("WHICH_TRAIN_PRICE",
                    orComp.getId(),
                    train.toText(),
                    seller.getId() );
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

        Train exchangedTrain = null;
        if (train != null && buyAction.isForExchange()) {
            Set<Train> oldTrains = buyAction.getTrainsForExchange();
            if (oldTrains.size() == 1) {
                exchangedTrain = Iterables.get(oldTrains,0);
            } else {
                List<String> oldTrainOptions =
                    new ArrayList<String>(oldTrains.size());
                String[] options = new String[oldTrains.size()];
                int jj = 0;
                for (int j = 0; j < oldTrains.size(); j++) {
                    options[jj + j] =
                            LocalText.getText("N_Train", Iterables.get(oldTrains, j).toText());
                    oldTrainOptions.add(options[jj + j]);
                }
                String exchangedTrainName =
                    (String) JOptionPane.showInputDialog(orWindow,
                            LocalText.getText("WHICH_TRAIN_EXCHANGE_FOR",
                                        gameUIManager.format(price)),
                                    LocalText.getText("WHICH_TRAIN_TO_EXCHANGE"),
                                    JOptionPane.QUESTION_MESSAGE, null, options,
                                    options[0]);
                if (exchangedTrainName != null) {
                    int index = oldTrainOptions.indexOf(exchangedTrainName);
                    if (index >= 0) {
                        exchangedTrain = Iterables.get(oldTrains, index);
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
        List<BuyPrivate> privates = getPossibleActions().getType(BuyPrivate.class);
        String chosenOption;
        BuyPrivate chosenAction = null;
        int minPrice = 0, maxPrice = 0;
        String priceRange;

        for (BuyPrivate action : privates) {
            minPrice = action.getMinimumPrice();
            maxPrice = action.getMaximumPrice();
            if (minPrice < maxPrice) {
                priceRange = gameUIManager.format(minPrice) + "..." 
                        + gameUIManager.format(maxPrice);
            } else {
                priceRange = gameUIManager.format(maxPrice);
            }

            privatesForSale.add(LocalText.getText("BuyPrivatePrompt",
                    action.getPrivateCompany().getId(),
                    action.getPrivateCompany().getOwner().getId(),
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
                                            gameUIManager.format(minPrice),
                                            gameUIManager.format(maxPrice) ),
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

//    public void executeUpgrade() {
        // map correction override
        // FIXME: Has to be changed during rewrite
//        if (mapCorrectionEnabled) {
//            if (selectedHex != null && selectedHex.getProvisionalTile() != null) {
//                if (mapCorrectionAction.getStep() == ActionStep.SELECT_ORIENTATION) {
//                    int orientation = selectedHex.getProvisionalTileRotation().getTrackPointNumber();
//                    mapCorrectionAction.selectOrientation(orientation);
//                } else if (mapCorrectionAction.getStep() == ActionStep.CONFIRM) {
//                    mapCorrectionAction.selectConfirmed();
//                }
//                if (orWindow.process(mapCorrectionAction)) {
//                    selectedHex.fixTile();
//                } else {
//                    selectedHex.removeTile();
//                }
//                map.selectHex(null);
//            }
//            return;
//        }

//    }

    public void layToken(TokenStopUpgrade upgrade) {
        LayToken action = upgrade.getAction();
        if (action instanceof LayBaseToken) {
            layBaseToken(upgrade);
        } else if (action instanceof LayBonusToken) {
            layBonusToken(upgrade);
        }
    }
    
//    public void cancelUpgrade() {
//        GUIHex selectedHex = mapPanel.getMap().getSelectedHex();

        // map correction override
        // FIXME: for rewrite
//        if (mapCorrectionEnabled) {
//            if (selectedHex != null) selectedHex.removeTile();
//            mapCorrectionAction.selectCancel();
//            orWindow.process(mapCorrectionAction);
//            map.selectHex(null);
//            return;
//        }
//
//    }
    
    public void cancelTileUpgrade() {
        GUIHex selectedHex = mapPanel.getMap().getSelectedHex();
        if (selectedHex != null) selectedHex.removeTile();
        orWindow.process(new NullAction(NullAction.Mode.SKIP));
    }

    public void cancelTokenUpgrade() {
        GUIHex selectedHex = mapPanel.getMap().getSelectedHex();
        if (selectedHex != null) selectedHex.removeToken();
        orWindow.process(new NullAction(NullAction.Mode.SKIP));
    }

    /** Default implementation.
     * The &lt;Loans&gt; attributes number and value <b>must</b>
     * have been configured in CompanyManager.xml */
    protected void takeLoans(TakeLoans action) {

        if (action.getMaxNumber() == 1) {

            String message = LocalText.getText("PleaseConfirm");
            String prompt = LocalText.getText("TakeLoanPrompt",
                    action.getCompanyName(),
                    gameUIManager.format(action.getPrice()));
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
                            gameUIManager.format(loanAmount),
                            gameUIManager.format(minNumber * loanAmount)));
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
                            gameUIManager.format(loanAmount),
                            gameUIManager.format(i * loanAmount));
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

        orPanel.resetActions();

        messagePanel.setMessage(null);

        if (actionToComplete != null) {
            log.debug("ExecutedAction: " + actionToComplete);
        }
        // End of possible action debug listing

        orStep = oRound.getStep();
        orComp = oRound.getOperatingCompany();
        log.debug("Or comp index = " + orCompIndex+" in round "+oRound.getRoundName());
        log.debug("OR company = " + orComp.getId());
        log.debug("OR step=" + orStep);

        if (oRound.getOperatingCompanyndex() != orCompIndex) {
            if (orCompIndex >= 0) orPanel.finishORCompanyTurn(orCompIndex);

            // Check if sequence has changed
            checkORCompanySequence(companies, oRound.getOperatingCompanies());
            setORCompanyTurn(oRound.getOperatingCompanyndex());
        }

        orPanel.initORCompanyTurn(orComp, orCompIndex);

        if (!myTurn) return;
        
        PossibleActions possibleActions = getPossibleActions();

        privatesCanBeBoughtNow = possibleActions.contains(BuyPrivate.class);
        orPanel.initPrivateBuying(privatesCanBeBoughtNow);

        // initialize operating costs actions
        orPanel.initOperatingCosts(possibleActions.contains(OperatingCost.class));

        // initial deactivation of revenue calculation
        if (!possibleActions.contains(SetDividend.class)) {
            orPanel.stopRevenueUpdate();
            orPanel.resetCurrentRevenueDisplay();
        }

// FIXME: Rewrite Map Correction
        //        if (possibleActions.contains(MapCorrectionAction.class)) {
//            orPanel.initTileLayingStep();
//            orWindow.requestFocus();
//
//            MapCorrectionAction action = (possibleActions.getType(MapCorrectionAction.class)).get(0);
//
//            mapCorrectionEnabled = true;
//            mapCorrectionAction = action;
//            updateUpgradesPanel(action);
//        } else if (orStep == GameDef.OrStep.LAY_TRACK) {
        if (orStep == GameDef.OrStep.LAY_TRACK) {
            //if (possibleActions.contains(LayTile.class)) {

            orPanel.initTileLayingStep();

            orWindow.requestFocus();

            //} else if (possibleActions.contains(LayBaseToken.class)) {
        } else if (orStep == GameDef.OrStep.LAY_TOKEN) {

            orWindow.requestFocus();

            orPanel.initTokenLayingStep();
            log.debug("BaseTokens can be laid");

        } else if (possibleActions.contains(SetDividend.class)
                && localStep == LocalSteps.SelectPayout) {

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
                            gameUIManager.format(action.getRequiredCash()))
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

        
        setMapRelatedActions(possibleActions);

        GameAction undoAction = null;
        GameAction redoAction = null;

        if (possibleActions.contains(NullAction.class)) {

            List<NullAction> actions =
                possibleActions.getType(NullAction.class);
            for (NullAction action : actions) {
                switch (action.getMode()) {
                case DONE:
                    orPanel.enableDone(action);
                    break;
                default:
                    break;
                }
            }
        }

        if (possibleActions.contains(GameAction.class)) {

            List<GameAction> actions =
                possibleActions.getType(GameAction.class);
            for (GameAction action : actions) {
                switch (action.getMode()) {
                case UNDO:
                    undoAction = action;
                    break;
                case REDO:
                    redoAction = action;
                    break;
                default:
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

                    // FIXME: Does this work
                    orWindow.requestFocus();
                    orPanel.initTokenLayingStep();
                } else {
                    SpecialTokenLay stl = tAction.getSpecialProperty();
                    if (stl != null) orPanel.addSpecialAction(tAction, stl.toMenu());
                }
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
                                gameUIManager.format(bbt.getValue()),
                            bbt.getSellerName(),
                                gameUIManager.format(bbt.getPrice()) );
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
                SpecialProperty sp = usp.getSpecialProperty();
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
    protected void checkORCompanySequence (PublicCompany[] oldCompanies, List<PublicCompany> newCompanies) {
        for (int i=0; i<newCompanies.size(); i++) {
            if (newCompanies.get(i) != oldCompanies[i]) {
                log.debug("Detected a OR company sequence change: "+oldCompanies[i].getId()
                        +" becomes "+newCompanies.get(i).getId());
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

    protected void setLocalStep(LocalSteps localStep) {
        log.debug("Setting upgrade step to " + localStep);

        // TODO: Check if this still fits
        SoundManager.notifyOfORLocalStep(localStep);

        this.localStep = localStep;

        updateMessage();
        updateUpgradesPanel();
    }

    public void updateUpgradesPanel() {

        GUIHex selectedHex = map.getSelectedHex();

        if (upgradePanel != null) {
            log.debug("Initial localStep is " + localStep);
            switch (localStep) {
            case Inactive:
                upgradePanel.setState(UpgradesPanel.States.Inactive);
                upgradePanel.init();
                break;
            case SelectAHexForTile:
                upgradePanel.setState(UpgradesPanel.States.Tile);
                upgradePanel.init();
                break;
            case SelectATile:
                upgradePanel.init();
                upgradePanel.addUpgrades(tileUpgrades.get(selectedHex));
                upgradePanel.showUpgrades();
                upgradePanel.setDoneEnabled(false);
                break;
            case RotateTile:
                upgradePanel.showUpgrades();
                upgradePanel.setDoneEnabled(true);
                break;
            case SelectAHexForToken:
                upgradePanel.setState(UpgradesPanel.States.Token);
                upgradePanel.init();
                break;
            case SelectAToken:
                upgradePanel.init();
                Set<TokenStopUpgrade> upgrades = tokenUpgrades.get(selectedHex);
                upgradePanel.addUpgrades(upgrades);
                upgradePanel.showUpgrades();
                if (upgrades.size() == 1) {
                    localStep = LocalSteps.ConfirmToken;
                }
                break;
            case ConfirmToken:
                upgradePanel.showUpgrades();
                upgradePanel.setDoneEnabled(true);
                break;
            default:
                upgradePanel.setState(UpgradesPanel.States.Inactive);
                upgradePanel.init();
            }
        }
        log.debug("Final localStep is " + localStep);
    }
    
    private void displayRemainingTiles() {

        if (remainingTiles == null) {
            remainingTiles = new RemainingTilesWindow(orWindow);
        } else {
            remainingTiles.activate();
        }
    }

    private NetworkGraph getCompanyGraph(){
        NetworkAdapter network = NetworkAdapter.create(gameUIManager.getRoot());
        return network.getRouteGraph(orComp, true);
    }

    // Message Panel helper methods
    // TODO: Remove them 
    public void setMessage(String message) {
        messagePanel.setMessage(message);
    }

    public void setInformation(String infoText) {
        messagePanel.setInformation(infoText);
    }

    public void setDetail(String detailText) {
        messagePanel.setDetail(detailText);
    }

    // Further Getters 
    
    public HexMap getMap() {
        return map;
    }

    public GameUIManager getGameUIManager () {
        return gameUIManager;
    }
    
    public ORWindow getORWindow() {
        return orWindow;
    }
    
    public ORPanel getORPanel() {
        return orPanel;
    }

   // TODO: Remove helper functions below
    
    // FIXME: Getting the possible actions inside ORUIManager methods should be removed
    // Better transfer them by method args
    protected PossibleActions getPossibleActions() {
        return gameUIManager.getGameManager().getPossibleActions();
    }
    
    protected TileManager getTileManager() {
        return gameUIManager.getRoot().getTileManager();
    }

}