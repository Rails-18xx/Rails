package net.sf.rails.ui.swing;


import java.util.*;

import javax.swing.JDialog;
import javax.swing.JOptionPane;

import net.sf.rails.algorithms.NetworkAdapter;
import net.sf.rails.algorithms.NetworkGraph;
import net.sf.rails.algorithms.NetworkVertex;
import net.sf.rails.common.Config;
import net.sf.rails.common.GameOption;
import net.sf.rails.common.GuiDef;
import net.sf.rails.common.LocalText;
import net.sf.rails.game.*;
import net.sf.rails.game.financial.ShareSellingRound;
import net.sf.rails.game.round.RoundFacade;
import net.sf.rails.game.special.SpecialProperty;
import net.sf.rails.game.special.SpecialSingleTileLay;
import net.sf.rails.game.special.SpecialTileLay;
import net.sf.rails.game.special.SpecialBaseTokenLay;
import net.sf.rails.game.state.Owner;
import net.sf.rails.sound.SoundManager;
import net.sf.rails.ui.swing.elements.*;
import net.sf.rails.ui.swing.hexmap.GUIHex;
import net.sf.rails.ui.swing.hexmap.HexMap;
import net.sf.rails.ui.swing.hexmap.HexUpgrade;
import net.sf.rails.ui.swing.hexmap.TileHexUpgrade;
import net.sf.rails.ui.swing.hexmap.TokenHexUpgrade;
import net.sf.rails.util.Util;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import rails.game.action.*;
import rails.game.correct.ClosePrivate;
import rails.game.correct.OperatingCost;

import com.google.common.collect.Iterables;
import com.google.common.collect.Lists;
import com.google.common.collect.Multimap;
import com.google.common.collect.Sets;

import static net.sf.rails.ui.swing.GameUIManager.EXCHANGE_TOKENS_DIALOG;

// FIXME: Add back corrections mechanisms
// Rails 2.0, Even better add a new mechanism that allows to use the standard mechanism for corrections
public class ORUIManager implements DialogOwner {

    private static final Logger log = LoggerFactory.getLogger(ORUIManager.class);

    protected GameUIManager gameUIManager;
    protected NetworkAdapter networkAdapter;

    protected ORWindow orWindow;
    protected ORPanel orPanel;
    private UpgradesPanel upgradePanel;
    private MapPanel mapPanel;
    private HexMap map;
    protected MessagePanel messagePanel;
    private RemainingTilesWindow remainingTiles;

    protected OperatingRound oRound;
    private List<PublicCompany> companies;

    // TODO: Remove storage of those variables
    // replace it by either action.getCompany() or oRound.getOperatingCompany()
    protected PublicCompany orComp;
    protected int orCompIndex;

    private LocalSteps localStep;

    private boolean privatesCanBeBoughtNow;

    protected final GUIHexUpgrades hexUpgrades = GUIHexUpgrades.create();

    /* Local substeps */
    public enum LocalSteps {
        INACTIVE, SELECT_HEX, SELECT_UPGRADE, SET_REVENUE, SELECT_PAYOUT
    }

    /* Keys of dialogs owned by this class */
    public static final String SELECT_DESTINATION_COMPANIES_DIALOG = "SelectDestinationCompanies";
    public static final String REPAY_LOANS_DIALOG = "RepayLoans";
    public static final String GOT_PERMISSION_DIALOG = "AskedPermissionDialog";
    public static final String TOKEN_EXCHANGE_DIALOG = "SelectTokensToExchange";

    public ORUIManager() {

    }

    void setGameUIManager (GameUIManager gameUIManager) {
        this.gameUIManager = gameUIManager;
        this.networkAdapter = NetworkAdapter.create(gameUIManager.getRoot());
    }

    void init(ORWindow orWindow) {

        this.orWindow = orWindow;

        orPanel = orWindow.getORPanel();
        mapPanel = orWindow.getMapPanel();
        upgradePanel = orWindow.getUpgradePanel();
        upgradePanel.setHexUpgrades(hexUpgrades);
        map = mapPanel.getMap();
        messagePanel = orWindow.getMessagePanel();

    }

    protected void initOR(OperatingRound or) {
        oRound = or;
        companies = or.getOperatingCompanies();
        orWindow.activate(oRound);
    }

    public void finish() {
        orWindow.finish();
        upgradePanel.setInactive();
        // TODO: Is this still required, do we need to store in ORUIManager the active OperatingCompany?
        if (!(gameUIManager.getCurrentRound() instanceof ShareSellingRound)) {
            orComp = null;
        }
    }

    public void setMapRelatedActions(PossibleActions actions) {

        GUIHex selectedHex = map.getSelectedHex();

        // clean map, if there are map upgrades
        if (hexUpgrades.hasElements()) {
            /* Finish tile laying step */
            if (selectedHex != null) {
                selectedHex.setUpgrade(null);
                selectedHex.setState(GUIHex.State.NORMAL);
                map.setSelectedHex(null);
            }
            // remove selectable indications
            for (GUIHex guiHex:hexUpgrades.getHexes()) {
                guiHex.setState(GUIHex.State.NORMAL);
            }
            hexUpgrades.clear();
        }

        List<LayTile> tileActions = actions.getType(LayTile.class);
        if (!tileActions.isEmpty()) {
            defineTileUpgrades(tileActions);
        }

        List<LayToken> tokenActions = actions.getType(LayToken.class);
        if (!tokenActions.isEmpty()) {
            defineTokenUpgrades(tokenActions);
        }

        // build and finalize hexUpgrades
        hexUpgrades.build();

        // show selectable hexes if highlight is active
        if (gameUIManager.getGameParameterAsBoolean(GuiDef.Parm.ROUTE_HIGHLIGHT)) {
            checkHexVisibilityOnUI(actions);
        }

        // TODO: This really is too early, the special actions are not yet defined here.
        // This is now fixed at line 1545, see also line 1449 for an earlier attempt.
        LocalSteps nextSubStep;
        if (tileActions.isEmpty() && tokenActions.isEmpty()) {
            nextSubStep = LocalSteps.INACTIVE;
        } else {
            nextSubStep = LocalSteps.SELECT_HEX;
        }
        setLocalStep(nextSubStep);
    }

    protected void checkHexVisibilityOnUI(PossibleActions actions) {

       // SpecialTileLay sp = (SpecialTileLay)layTile.getSpecialProperty();

            for (GUIHex hex:hexUpgrades.getHexes()) {
                boolean invalids = false;
                for (HexUpgrade upgrade:hexUpgrades.getUpgrades(hex)) {
                    if (upgrade.isValid()) {
                        hex.setState(GUIHex.State.SELECTABLE);
                        invalids = false;
                        break;
                    } else {
                        if (upgrade.isVisible() && !upgrade.isValid()) {
                            invalids = true;
                        }
                    }
                }
                // end of single hex-loop
                if (invalids) {
                    hex.setState(GUIHex.State.INVALIDS);
                }
            }
        }

    private void defineTileUpgrades(List<LayTile> actions) {
        for (LayTile layTile:actions) {

            switch (layTile.getType()) {
            case (LayTile.GENERIC):
            case (LayTile.GENERIC_EXCL_LOCATIONS) :
                addConnectedTileLays(layTile);
                break;
            case (LayTile.SPECIAL_PROPERTY):
                SpecialTileLay sp = layTile.getSpecialProperty();
                if (sp.requiresConnection()) {
                    addConnectedTileLays(layTile);
                    //MBr: 20210120 - So far no Private has connected and neighbours as power,
                    // so we dont need to add this here.
                } else {
                    //MBr: 20210120 - Introducing the hook for the new private power for 18Chesapeake and also 1844
                    if (((SpecialSingleTileLay) sp).hasNeighbours()) {
                        addNeighbouredTileLays(layTile);
                    } else {
                        addLocatedTileLays(layTile);
                    }
                }
                break;
            case (LayTile.LOCATION_SPECIFIC):
                addLocatedTileLays(layTile);
                break;
            case (LayTile.CORRECTION):
                addCorrectionTileLays(layTile);
            default:
            }
        }

    }

    private void addNeighbouredTileLays(LayTile layTile) {
    }

    private void addConnectedTileLays(LayTile layTile) {
        NetworkGraph graph = networkAdapter.getRouteGraph(layTile.getCompany(), true, false);
        Map<MapHex, HexSidesSet> mapHexSides = graph.getReachableSides();
        Multimap<MapHex, Station> mapHexStations = graph.getPassableStations();
        Phase currentPhase = gameUIManager.getCurrentPhase();

        boolean allLocations = (layTile.getLocations() == null
                || layTile.getLocations().isEmpty());

        for (MapHex hex:Sets.union(mapHexSides.keySet(), mapHexStations.keySet())) {

            // For the initial Belgium exclusion in 1826
            log.debug("Type={} hex={} locations={} allLocations={}",
                    layTile.getType(), hex, layTile.getLocations(), allLocations);
            if (layTile.getType() == LayTile.GENERIC_EXCL_LOCATIONS
                    && !allLocations
                    && layTile.getLocations().contains(hex)) {
                log.debug ("SKIP");
                continue;
            }
            log.debug(" OK");
            // Accept an immediate tile lay on reserved hexes if the reserving company
            // president is the current player.
            EnumSet<TileHexUpgrade.Invalids> allowances
                    = EnumSet.noneOf(TileHexUpgrade.Invalids.class);
            if (hex.isReservedForCompany())  {
                // For now we accept this action, but will later check for permission
                allowances.add(TileHexUpgrade.Invalids.HEX_RESERVED);
            }
            if (allLocations
                    || layTile.getType() != LayTile.GENERIC_EXCL_LOCATIONS && layTile.getLocations().contains(hex)
                    || layTile.getType() == LayTile.GENERIC_EXCL_LOCATIONS && !layTile.getLocations().contains(hex))
            {
                GUIHex guiHex = map.getHex(hex);
                String routeAlgorithm = GameOption.getValue(gameUIManager.getRoot(),
                        "RouteAlgorithm");
                Set<TileHexUpgrade> upgrades = TileHexUpgrade.create(guiHex,
                        mapHexSides.get(hex),
                        mapHexStations.get(hex), layTile, routeAlgorithm);
                TileHexUpgrade.validates(upgrades, currentPhase, allowances);
                gameSpecificTileUpgradeValidation (upgrades, layTile, currentPhase);
                hexUpgrades.putAll(guiHex, upgrades);
            }
        }

        // scroll map to center over companies network
        String autoScroll = Config.getGameSpecific(gameUIManager.getRoot().getGameName(), "map.autoscroll");
        if (Util.hasValue(autoScroll) &&  autoScroll.equalsIgnoreCase("no")) {
            // do nothing
        } else {
            mapPanel.scrollPaneShowRectangle(
                    NetworkVertex.getVertexMapCoverage(map, graph.getGraph().vertexSet()));
        }
    }

    /**
     * Stub to do additional validation.
     * Used in SOH to prevent showing an upgrade that
     * incorrectly uses a private special property.
     * @param upgrades
     * @param layTile
     */
    protected void gameSpecificTileUpgradeValidation (Set<TileHexUpgrade> upgrades,
                                                      LayTile layTile,
                                                      Phase currentPhase) {
    }

    private void addLocatedTileLays(LayTile layTile) {
        if (layTile.getLocations() != null) {
            for (MapHex hex : layTile.getLocations()) {
                GUIHex guiHex = map.getHex(hex);
                Set<TileHexUpgrade> upgrades = TileHexUpgrade.createLocated(guiHex, layTile);
                TileHexUpgrade.validates(upgrades, gameUIManager.getCurrentPhase());
                hexUpgrades.putAll(guiHex, upgrades);
            }
        }
    }

    private void addCorrectionTileLays(LayTile layTile) {
        EnumSet<TileHexUpgrade.Invalids> allowances
                = EnumSet.of(TileHexUpgrade.Invalids.HEX_RESERVED);
        for (GUIHex hex:map.getHexes()) {
            Set<TileHexUpgrade> upgrades = TileHexUpgrade.createCorrection(hex, layTile);
            TileHexUpgrade.validates(upgrades, gameUIManager.getCurrentPhase(), allowances);
            hexUpgrades.putAll(hex, upgrades);
        }
    }

    private void defineTokenUpgrades(List<LayToken> actions) {

        for (LayToken layToken:actions) {
            if (layToken instanceof LayBaseToken) {
                LayBaseToken layBaseToken = (LayBaseToken)layToken;
                switch (layBaseToken.getType()) {
                case (LayBaseToken.GENERIC):
                    addGenericTokenLays(layBaseToken);
                    break;
                case (LayBaseToken.LOCATION_SPECIFIC):
                case (LayBaseToken.SPECIAL_PROPERTY):
                    if (layBaseToken.getLocations() != null) {
                        addLocatedTokenLays(layBaseToken);
                    } else {
                        addGenericTokenLays(layBaseToken);
                    }
                break;
                case LayBaseToken.FORCED_LAY :
                case LayBaseToken.HOME_CITY:
                case LayBaseToken.NON_CITY:
                    addLocatedTokenLays(layBaseToken);
                break;
                case (LayTile.CORRECTION):
                    addCorrectionTokenLays(layBaseToken);
                default:
                }
            } else if (layToken instanceof LayBonusToken) {
                // Assumption: BonusTokens are always located
                addLocatedTokenLays(layToken);
            }
        }
    }

    private void addGenericTokenLays(LayBaseToken action) {
        PublicCompany company = action.getCompany();
        if (company.getBaseTokenLayCostMethod() == PublicCompany.BaseCostMethod.ROUTE_DISTANCE) {
            // Currently only used by 1826.
            // Did originally work with all games, but somehow failed with 1837 in a later stage
            Map<Stop, Integer> tokenableStops = new Routes(company).getTokenLayRouteDistances(
                    PublicCompany.INCL_START_HEX, PublicCompany.FROM_HOME_ONLY);
            for (Stop stop : tokenableStops.keySet()) {
                MapHex hex = stop.getParent();
                GUIHex guiHex = map.getHex(hex);
                TokenHexUpgrade upgrade = TokenHexUpgrade.create(this, guiHex, tokenableStops.keySet(), action);
                TokenHexUpgrade.validates(upgrade);
                hexUpgrades.put(guiHex, upgrade);
            }
        } else { // The old method
            NetworkGraph graph = networkAdapter.getRouteGraph(company, true, false);
            Multimap<MapHex, Stop> hexStops = graph.getTokenableStops(company);
            for (MapHex hex:hexStops.keySet()) {
                GUIHex guiHex = map.getHex(hex);
                TokenHexUpgrade upgrade = TokenHexUpgrade.create(this, guiHex, hexStops.get(hex), action);
                TokenHexUpgrade.validates(upgrade);
                hexUpgrades.put(guiHex, upgrade);
            }

        }
    }

    protected void addLocatedTokenLays(LayToken action) {
        for (MapHex hex:action.getLocations()) {
            GUIHex guiHex = map.getHex(hex);
            TokenHexUpgrade upgrade = TokenHexUpgrade.create(
                    this, guiHex, hex.getTokenableStops(action.getCompany()), action);
            TokenHexUpgrade.validates(upgrade);
            hexUpgrades.put(guiHex, upgrade);
        }
    }

    private void addCorrectionTokenLays(LayToken action) {
        for (GUIHex guiHex:map.getHexes()) {
            MapHex hex = guiHex.getHex();
            List<Stop> tokenableStops = Lists.newArrayList();
            for (Stop stop:hex.getStops()) {
                if (stop.isTokenableFor(action.getCompany())) {
                    tokenableStops.add(stop);
                }
            }
            if (!tokenableStops.isEmpty()) {
                TokenHexUpgrade upgrade = TokenHexUpgrade.create(this, guiHex, tokenableStops, action);
                TokenHexUpgrade.validates(upgrade);
                hexUpgrades.put(guiHex, upgrade);
            }
        }
    }

    public void updateMessage() {

        // For now, this only has an effect during tile and token laying.
        // Perhaps we need to centralise message updating here in a later stage.
        log.debug("Calling updateMessage, subStep={}", localStep);

        if (localStep == LocalSteps.INACTIVE ) return;

        StringBuilder message = new StringBuilder("<font color='red'>" + LocalText.getText(localStep.toString()) + "</font>");
        String extraMessage = "";

        // Activity Messages
        boolean correctionActive = false;
        List<LayTile> tileLays = getPossibleActions().getType(LayTile.class);
        if (!tileLays.isEmpty()) {
            /* Compose prompt for tile laying */

            for (LayTile tileLay : tileLays) {
                if (tileLay.isCorrection()) {
                    correctionActive = true;
                    continue;
                }
                Map<String, Integer> tileColours;
                SpecialProperty sp = tileLay.getSpecialProperty();
                // For special tile lays add special message
                if (sp instanceof SpecialTileLay) {
                    SpecialTileLay stl = (SpecialTileLay) sp;
                    extraMessage += "<BR>" + stl.getHelp();
                } else if ((tileColours = tileLay.getTileColours()) != null) {
                    int number;
                    StringBuilder normalTileMessage = new StringBuilder();
                    for (String colour : tileColours.keySet()) {
                        number = tileColours.get(colour);
                        if (normalTileMessage.length() > 1) {
                            normalTileMessage.append(" ").append(
                                    LocalText.getText("OR")).append(" ");
                        }
                        normalTileMessage.append(number).append(" ").append(
                                colour);
                    }
                    message.append("<BR>").append(LocalText.getText("TileColours", normalTileMessage));
                }
            }
        }

        List<LayBaseToken> tokenLays =
                getPossibleActions().getType(LayBaseToken.class);

        if (!tokenLays.isEmpty()) {

            /* Compose prompt for token laying */
            String locations;
            StringBuilder normalTokenMessage = new StringBuilder();

            for (LayBaseToken tokenLay : tokenLays) {
                if (tokenLay.isCorrection()) {
                    correctionActive = true;
                    continue;
                }
                SpecialProperty sp = tokenLay.getSpecialProperty();
                if ( sp instanceof SpecialBaseTokenLay ) {
                    extraMessage += "<BR>" + sp.getHelp();
                } else if ((locations = tokenLay.getLocationNameString()) != null) {
                    if (normalTokenMessage.length() > 1) {
                        normalTokenMessage.append(" ").append(
                                LocalText.getText("OR")).append(" ");
                    }
                    normalTokenMessage.append(locations);
                    message.append("<BR>").append(LocalText.getText("NormalLocatedToken",
                            normalTokenMessage));
                } else {
                   message.append("<BR> ").append(LocalText.getText("NormalToken",
                           normalTokenMessage));
               }
            }
        }

        if (tileLays.isEmpty() && tokenLays.isEmpty() && orPanel.hasSpecialActions()) {
            // Special actions only. Maybe we need a separate substep for this case.
            // Restart the message
            message = new StringBuilder("<font color='red'>" + LocalText.getText("YouHaveSpecialActions") + "</font>");
            message.append("<br>").append(LocalText.getText("YouCannotLayTokens"));
        }

        if (correctionActive) {
            message.append("<BR>").append(LocalText.getText("CorrectMapActive"));
        }

        if (extraMessage.length() > 0) {
            message.append("<font color='orange'>").append(extraMessage).append("</font>");
        }

        messagePanel.setMessage(message.toString());
    }

    /**
     * Processes button presses and menu selection actions
     */
    // FIXME: Can this be really a list of actions?
    // Answer: Not yet, but who knows? (EV)
    public void processAction(String command, List<PossibleAction> actions) {

        if (actions != null && actions.size() > 0 && actions.get(0) != null
                && !processGameSpecificActions(actions)) {

            Class<? extends PossibleAction> actionType =
                actions.get(0).getClass();

            if (actionType == SetDividend.class) {

                setDividend(command, (SetDividend) actions.get(0));


            } else if (actionType == BuyBonusToken.class) {

                buyBonusToken ((BuyBonusToken)actions.get(0));

            } else if (actionType == NullAction.class
                    || actionType == GameAction.class ) {

                orWindow.process(actions.get(0));

            } else if (actionType == ReachDestinations.class) {

                reachDestinations((ReachDestinations) actions.get(0));

            } else if (actionType == GrowCompany.class) {

                orWindow.process(actions.get(0));

            } else if (actionType == TakeLoans.class) {

                takeLoans ((TakeLoans)actions.get(0));

            } else if (actionType == RepayLoans.class) {

                repayLoans((RepayLoans) actions.get(0));

            } else if (actionType == UseSpecialProperty.class) {

                useSpecialProperty ((UseSpecialProperty)actions.get(0));

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

        } else if (command.equals(ORPanel.CONFIRM_CMD)) {

            confirmUpgrade();
        }
    }

    /** Stub, can be overridden in subclasses */
    // FIXME: As above, really a list of actions?
    protected boolean processGameSpecificActions(List<PossibleAction> actions) {

        return false;

    }

    protected void setDividend(String command, SetDividend action) {

        int amount;
        int dividend;
        int treasuryAmount = 0;
        boolean hasDirectCompanyIncomeInOR
                = gameUIManager.getGameParameterAsBoolean(GuiDef.Parm.HAS_SPECIAL_COMPANY_INCOME);

        if (command.equals(ORPanel.SET_REVENUE_CMD)) {
            dividend = amount = orPanel.getRevenue(orCompIndex);
            if (hasDirectCompanyIncomeInOR) {
                treasuryAmount = orPanel.getCompanyTreasuryBonusRevenue(orCompIndex);
                dividend -= treasuryAmount;
                orPanel.setDividend(orCompIndex, dividend);
            }

            orPanel.stopRevenueUpdate();
            log.debug("Set revenue amount is {}", amount);
            action.setActualRevenue(amount);
            if (hasDirectCompanyIncomeInOR) {
                log.debug("The Bonus for the company treasury is {}", treasuryAmount);
                action.setActualCompanyTreasuryRevenue(treasuryAmount);
            }

            // notify sound manager of set revenue amount as soon as
            // set revenue is pressed (not waiting for the completion
            // of the set dividend action)
            SoundManager.notifyOfSetRevenue(amount);

            if (amount == 0 || action.getRevenueAllocation() != SetDividend.UNKNOWN) {
                log.debug("Allocation is known: {}", action.getRevenueAllocation());
                orWindow.process(action);
            } else {
                log.debug("Allocation is unknown, asking for it");
                setLocalStep(LocalSteps.SELECT_PAYOUT);
                updateStatus(action, true);

                // Locally update revenue if we don't inform the server yet.
                orPanel.setRevenue(orCompIndex, amount);
                if (hasDirectCompanyIncomeInOR) {
                    orPanel.setTreasuryBonusRevenue(orCompIndex, treasuryAmount);
                }
            }
        } else {
            // The revenue allocation has been selected
            orWindow.process(action);
        }
    }

    private void buyBonusToken (BuyBonusToken action) {

        orWindow.process(action);
    }

    protected void reachDestinations (ReachDestinations action) {

        List<String> options = new ArrayList<>();
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

    /**
     * @return True if the map panel expected hex clicks for actions / corrections
     */
    public boolean hexClicked(GUIHex clickedHex, GUIHex selectedHex, boolean rightClick) {

        // protection if localStep is not defined (outside operating rounds)
        if (localStep == null) {
            return false;
        }

        // if selectedHex is clicked again ==> change Upgrade, or Upgrade-Selection
        if (selectedHex == clickedHex) {
            // should not occur (as a hex is selected), however let us define that in case
            if (localStep == LocalSteps.SELECT_UPGRADE) {
                if (rightClick) { // right-click => next upgrade
                    upgradePanel.nextUpgrade();
                } else {
                    upgradePanel.nextSelection();
                }
                return true;
            }
            return false;
        }

        // if clickedHex is not on map => deactivate upgrade selection and use
        if (clickedHex == null) {
            if (localStep == LocalSteps.SELECT_UPGRADE) {
                if (selectedHex != null) {
                    map.selectHex(null);
                }
                setLocalStep(LocalSteps.SELECT_HEX);
                return true;
            }
            return false;
        }

        // otherwise a clickedHex is defined ==> select the hex if upgrades are provided
        if (hexUpgrades.containsVisible(clickedHex)) {
            switch (localStep) {
                case SELECT_HEX:
                    if (!gotPermission(clickedHex)) return false;
                    // if permitted, falls through
                case SELECT_UPGRADE:
                    map.selectHex(clickedHex);
                    setLocalStep(LocalSteps.SELECT_UPGRADE);
                    return true;
                default:
                    return false;
            }
        }

        // otherwise the clicked hex is not contained, so go back to SelectHex
        switch (localStep) {
            case SELECT_HEX:
            case SELECT_UPGRADE:
                map.selectHex(null);
                setLocalStep(LocalSteps.SELECT_HEX);
                return false;
            default:
                return false;
        }
    }

    protected boolean gotPermission(GUIHex guiHex) {

        // Check if the clicked hex is reserved for a company
        MapHex hex = guiHex.getHex();
        if (!hex.isReservedForCompany() || !hex.isPreprintedTileCurrent()) return true;

        // Check if this is a tile upgrade
        HexUpgrade hexUpgrade = (HexUpgrade) hexUpgrades.getUpgrades(guiHex).toArray()[0];
        if (!(hexUpgrade instanceof TileHexUpgrade)) return true;

        // Check if permission from another player is required
        TileHexUpgrade upgrade = (TileHexUpgrade) hexUpgrade;
        LayTile action = upgrade.getAction();
        Player thisPlayer = action.getPlayer();
        Player otherPlayer = guiHex.getHex().getReservedForCompany().getPresident();
        if (thisPlayer.equals(otherPlayer)) return true;

        // We have to, so start a dialog.
        // The current player should have got permission off-game.
        ConfirmationDialog dialog = new ConfirmationDialog(GOT_PERMISSION_DIALOG,
                this,
                orWindow,
                LocalText.getText("GotPermission"),
                LocalText.getText("GotPermissionDialog", otherPlayer, upgrade.getHex().getHex()),
                LocalText.getText("Yes"),
                LocalText.getText("No"));
        setCurrentDialog (dialog, action);
        return true;
    }

    // FIXME: Inform SoundManager about Rotation of Tile (selection of upgrade)

    public void confirmUpgrade() {
        HexUpgrade upgrade = hexUpgrades.getActiveUpgrade();
        if (upgrade instanceof TileHexUpgrade) {
            layTile((TileHexUpgrade)upgrade);
        }
        if (upgrade instanceof TokenHexUpgrade) {
            layToken((TokenHexUpgrade)upgrade);
        }
    }

    public void skipUpgrade() {
        if (getPossibleActions().containsCorrections()) {
            // skip on corrections => return to Select Hex
            map.selectHex(null);
            setLocalStep(LocalSteps.SELECT_HEX);
        } else {
            orWindow.process(new NullAction(gameUIManager.getRoot(), NullAction.Mode.SKIP));
        }
    }

    protected void layTile(TileHexUpgrade upgrade) {
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
        allowance.setChosenHex(upgrade.getHex().getHex());
        int orientation = upgrade.getCurrentRotation().getTrackPointNumber();
        allowance.setOrientation(orientation);
        Tile targetTile = upgrade.getUpgrade().getTargetTile();
        allowance.setLaidTile(targetTile);
        allowance.setRelayBaseTokens(upgrade.isRelayBaseTokens());

        relayBaseTokens (allowance);

        if (!orWindow.process(allowance)) {
            setLocalStep(LocalSteps.SELECT_HEX);
        }
    }

    private void layToken(TokenHexUpgrade upgrade) {
        LayToken action = upgrade.getAction();
        if (action instanceof LayBaseToken) {
            layBaseToken(upgrade);
        } else if (action instanceof LayBonusToken) {
            layBonusToken(upgrade);
        }
    }


    private void layBaseToken(TokenHexUpgrade upgrade) {
        MapHex hex = upgrade.getHex().getHex();
        LayBaseToken action = (LayBaseToken) upgrade.getAction();

        action.setChosenHex(hex);
        if (upgrade.getSelectedStop() != null) { // Added for 18Scan, still necessary?
            action.setChosenStation(upgrade.getSelectedStop().getRelatedStationNumber());
        }

        if (!orWindow.process(action)) {
            setLocalStep(LocalSteps.SELECT_HEX);
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
    private void relayBaseTokens (LayTile action) {

        final MapHex hex = action.getChosenHex();
        Tile newTile = action.getLaidTile();
        Tile oldTile = hex.getCurrentTile();

        // Check if manual token relay is required.
        // This was an emergency measure in cases where automatic relay
        // did not work (e.g. 1837 tile 427). Now probably obsolete.
        if (!action.isRelayBaseTokens()
                && !oldTile.relayBaseTokensOnUpgrade()) return; // is deprecated

        List<Stop> stopsToQuery = Lists.newArrayList(hex.getStops());

        /* Check which tokens must be relaid, and in which sequence.
         * Ideally, the game engine should instruct the UI what to do
         * if there is more than one stop and more than one token.
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
            Collections.sort(stopsToQuery, new Comparator<>() {
                @Override
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

        // Ask the user to specify new token positions
        for (Stop oldStop : stopsToQuery) {
            if (oldStop.hasTokens()) {
                // Assume only 1 token (no exceptions known)
                // TODO: Rewrite this to make this code nicer
                PublicCompany company = (Iterables.get(oldStop.getBaseTokens(), 0)).getParent();

                List<String> prompts = new ArrayList<>();
                Map<String, Integer> promptToCityMap = new HashMap<>();
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

    /**
     * Lay Token finished.
     *
     * @param upgrade The LayBonusToken action object of the laid token.
     */
    // FIXME: This has to be rewritten
    public void layBonusToken(TokenHexUpgrade upgrade) {

        LayToken action = upgrade.getAction();

        // Assumption for now: always BonusToken
        // We might use it later for BaseTokens too.

        HexMap map = mapPanel.getMap();
        GUIHex selectedHex = map.getSelectedHex();

        if (selectedHex != null) {
            LayToken executedAction = action;

            executedAction.setChosenHex(selectedHex.getHex());

            if (orWindow.process(executedAction)) {
                // FIXME: Should this be setInactive(), please check
                upgradePanel.setActive();
                map.selectHex(null);
                //ensure painting the token (model update currently does not arrive at UI)
                map.repaintTokens(selectedHex.getBounds());
            }
        }
    }

    public void operatingCosts(){

        List<String> textOC = new ArrayList<>();
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

        List<String> prompts = new ArrayList<>();
        Map<String, PossibleAction> promptToTrain = new HashMap<>();
        Train train;
        StringBuilder usingPrivates = new StringBuilder();

        PossibleAction selectedAction;
        BuyTrain buyAction;

        String prompt;
        StringBuffer b;
        int cost;
        Owner from;

        List<BuyTrain> buyableTrains = getPossibleActions().getType(BuyTrain.class);
        for (BuyTrain bTrain : buyableTrains) {
            cost = bTrain.getFixedCost();
            from = bTrain.getFromOwner();

            /* Create a prompt per buying option */
            b = new StringBuffer();

            b.append(LocalText.getText("BUY_TRAIN_FROM",
                    bTrain.getType(),
                    from.getId() ));
            if (bTrain.isForExchange()) {
                String exchTrainTypes = bTrain.getTrainsForExchange().toString()
                        // Replacing e.g. "[4_0]" by "4", or "[4_0, 5_0, 6_0]" by "4,5 or 6"
                        .replaceAll("[\\[ ]?(\\w+)_\\d+(,)?\\s?]?", "$1$2")
                        .replaceFirst(",(\\w+)$"," or $1");
                b.append(" (").append(LocalText.getText("DiscardingTrain", exchTrainTypes)).append(")");
            }
            //if (cost > 0) {
                BuyTrain.Mode mode = bTrain.getFixedCostMode();
                if (/*mode == null ||*/ mode == BuyTrain.Mode.FIXED) {
                    b.append(" ").append(
                            LocalText.getText("AT_PRICE", gameUIManager.format(cost)));
                } else if (mode == BuyTrain.Mode.MAX) {
                    b.append(" ").append(
                            LocalText.getText("AT_MAX_PRICE", gameUIManager.format(cost)));
                } else if (mode == BuyTrain.Mode.MIN) {
                    b.append(" ").append(
                            LocalText.getText("AT_MIN_PRICE", gameUIManager.format(cost)));
                }
            //}
            if (bTrain.hasSpecialProperty()) {
                String priv =
                        (bTrain.getSpecialProperty()).getOriginalCompany().getId();
                b.append(" ").append(LocalText.getText("USING_SP", priv));
                usingPrivates.append(", ").append(priv);
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
                b.append(" (").append(bTrain.getExtraMessage()).append(")");
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

        StringBuilder msgbuf =
            new StringBuilder(LocalText.getText("SelectTrain"));
        if (usingPrivates.length() > 0) {
            msgbuf.append("<br><font color=\"red\">");
            msgbuf.append(LocalText.getText("SelectCheapTrain",
                    usingPrivates.substring(2)));
            msgbuf.append("</font>");
        }
        messagePanel.setMessage(msgbuf.toString());

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
        PublicCompany company = buyAction.getCompany();
        Owner seller = buyAction.getFromOwner();
        int fixedCost = buyAction.getFixedCost();
        BuyTrain.Mode mode = buyAction.getFixedCostMode();
        log.debug("From {} cost {} mode {}", seller, fixedCost, mode);

        // The relationship between fixedCost and mode is explained
        // in the Javadoc of the Mode enum in the BuyTrain class.
        if (seller instanceof PublicCompany
                && !company.mustTradeTrainsAtFixedPrice()
                && !((PublicCompany) seller).mustTradeTrainsAtFixedPrice()
                && (fixedCost == 0 || mode != null && mode != BuyTrain.Mode.FIXED)) {
            String remark = "";
            String priceText;
            //if (fixedCost > 0 && mode != null) {
                priceText = gameUIManager.format(fixedCost);
                switch (mode) {
                    case MIN:
                        remark = LocalText.getText("OrMore", priceText);
                        break;
                    case MAX:
                        remark = LocalText.getText("OrLess", priceText);
                    default:
                }
            //}
            prompt = LocalText.getText("WHICH_TRAIN_PRICE",
                    buyAction.getCompany().getId(),
                    train.toText(),
                    seller.getId(),
                    remark);
            String response;
            for (;;) {
                response =
                    JOptionPane.showInputDialog(orWindow, prompt,
                            LocalText.getText("WHICH_PRICE"),
                            JOptionPane.QUESTION_MESSAGE);
                if (response == null) return; // Cancel
                int enteredPrice;
                try {
                    enteredPrice = Integer.parseInt(response);
                } catch (NumberFormatException e) {
                    // Price stays 0, this is handled below
                    enteredPrice = 0;
                }
                if (enteredPrice > 0
                        && (mode == BuyTrain.Mode.MIN && enteredPrice >= fixedCost
                            || mode == BuyTrain.Mode.MAX && enteredPrice <= fixedCost
                            || mode == BuyTrain.Mode.FREE)) {
                    fixedCost = enteredPrice;
                    break; // Got a valid price.
                }
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
                    new ArrayList<>(oldTrains.size());
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
                                        gameUIManager.format(fixedCost)),
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

            buyAction.setPricePaid(fixedCost);
            buyAction.setExchangedTrain(exchangedTrain);
            if (buyAction.mustPresidentAddCash()) {
                buyAction.setAddedCash(buyAction.getPresidentCashToAdd());
            }
            orWindow.process(buyAction);
        }
    }

    public void buyPrivate() {

        int amount, index;
        List<String> privatesForSale = new ArrayList<>();
        List<BuyPrivate> privates = getPossibleActions().getType(BuyPrivate.class);
        String chosenOption;
        BuyPrivate chosenAction;
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

    /** Used to process some < properties from the 'Special' menu */
    /* In fact currently not used */
    protected void useSpecialProperty (UseSpecialProperty action) {
        gameUIManager.processAction(action);
    }

    public void updateStatus(boolean myTurn) {
        updateStatus(null, myTurn);
    }

    public void updateStatus(PossibleAction actionToComplete, boolean myTurn) {
        orPanel.resetActions();

        messagePanel.setMessage(null);

        RoundFacade currentRound = gameUIManager.getCurrentRound();
        if (!(currentRound instanceof OperatingRound)) {
            log.debug("early return: {}", currentRound);
            return;
        }

        if (actionToComplete != null) {
            log.debug("ExecutedAction: {}", actionToComplete);
        }
        // End of possible action debug listing

        PublicCompany orComp = oRound.getOperatingCompany();
        log.debug("OR company = {} in round {} index={}", orComp.getId(),
                oRound.getRoundName(),oRound.getOperatingCompanyIndex());

        GameDef.OrStep orStep = oRound.getStep();
        log.debug("OR step={}", orStep);

        if (oRound.getOperatingCompanyIndex() != orCompIndex) {
            if (orCompIndex >= 0) {
                orPanel.finishORCompanyTurn(orCompIndex);
            }
            // Check if sequence has changed
            checkORCompanySequence(companies, oRound.getOperatingCompanies());
            orCompIndex = oRound.getOperatingCompanyIndex();
        }

        orPanel.initORCompanyTurn(orComp, orCompIndex);

        //orPanel.initPrivateBuying(false);


        if (!myTurn) return;

        PossibleActions possibleActions = getPossibleActions();

        privatesCanBeBoughtNow = possibleActions.contains(BuyPrivate.class);
        orPanel.initPrivateBuying(privatesCanBeBoughtNow);

        // initialize operating costs actions
        orPanel.initOperatingCosts(possibleActions.contains(OperatingCost.class));

        // initial deactivation of revenue calculation
        if (!possibleActions.contains(SetDividend.class)) {
            orPanel.stopRevenueUpdate();
            //orPanel.resetCurrentRevenueDisplay();
        }

        if (orStep == GameDef.OrStep.LAY_TRACK) {
            //if (possibleActions.contains(LayTile.class)) {

            orPanel.initTileLayingStep();
            orPanel.setupConfirm();

            orWindow.requestFocus();

            //} else if (possibleActions.contains(LayBaseToken.class)) {
        } else if (orStep == GameDef.OrStep.LAY_TOKEN) {

            orWindow.requestFocus();

            orPanel.initTokenLayingStep();
            orPanel.setupConfirm();
            log.debug("BaseTokens can be laid or bonus tokens bought");

        } else if (possibleActions.contains(SetDividend.class)
                && localStep == LocalSteps.SELECT_PAYOUT ) {

            SetDividend action;
            if (actionToComplete != null) {
                action = (SetDividend) actionToComplete;
            } else {
                action = possibleActions.getType(SetDividend.class).get(0);
            }

            log.debug("Payout action before cloning: {}", action);

            orPanel.initPayoutStep(orCompIndex, action,
                    action.isAllocationAllowed(SetDividend.WITHHOLD),
                    action.isAllocationAllowed(SetDividend.SPLIT),
                    action.isAllocationAllowed(SetDividend.PAYOUT));

            messagePanel.setMessage(LocalText.getText("SelectPayout"));

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
            messagePanel.setMessage(message);

        } else if (orStep == GameDef.OrStep.BUY_TRAIN) {

            boolean canBuyTrain = possibleActions.contains(BuyTrain.class);
            orPanel.initTrainBuying(canBuyTrain);

            StringBuilder b = new StringBuilder(LocalText.getText("BuyTrain"));

            // TEMPORARY extra message about having no route
            for (BuyTrain bTrain : possibleActions.getType(BuyTrain.class)) {
                if (bTrain.isForcedBuyIfHasRoute()) {
                    b.append("<br><font color=\"red\">");
                    if (bTrain.isForcedBuyIfNoRoute()) {
                        b.append(LocalText.getText("MustBuyTrainIfNoRoute"));
                    } else {
                        b.append(LocalText.getText("MustBuyTrainIfHasRoute"));
                    }
                    b.append("</font>");
                    break;
                }
            }

            messagePanel.setMessage(b.toString());

        } else if (possibleActions.contains(DiscardTrain.class)) {

            gameUIManager.discardTrains(possibleActions.getType(DiscardTrain.class).get(0));

        } else if (possibleActions.contains(RepayLoans.class)) {

            orPanel.enableLoanRepayment(possibleActions.getType(RepayLoans.class).get(0));

        } else if (possibleActions.contains(ExchangeTokens2.class)) {

            prepareExchangeTokens (possibleActions.getType(ExchangeTokens2.class).get(0));

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
                case SKIP:
                    // Was disabled, because actionless steps
                    // are normally skipped. But sometines (18Scan) not,
                    // because of a confusion about bonus token layability.
                    // See OperatingRound.nextStep().
                    orPanel.enableSkip(action);
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

        if (possibleActions.contains(GrowCompany.class)) {
            GrowCompany action = possibleActions.getType(GrowCompany.class).get(0);
            orPanel.addSpecialAction(possibleActions.getType(GrowCompany.class).get(0),
                    LocalText.getText("GrowCompany",
                            action.getCompany(), 100 / action.getNewShareUnit()));
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

        checkForGameSpecificActions(orComp, orStep, possibleActions);

        // If special actions exist, check if Skip button is activated
        if (orPanel.hasSpecialActions()) {
            upgradePanel.setActive();
            updateMessage(); // Does not work even here !?
        }

        orPanel.redisplay();
    }

    /** Stub, can be overridden by game-specific subclasses */
    protected void checkForGameSpecificActions(PublicCompany orComp,
                                               GameDef.OrStep orStep,
                                               PossibleActions possibleActions) {}

    /** Redraw the ORPanel if the company operating order has changed */
    protected void checkORCompanySequence (List<PublicCompany> oldCompanies, List<PublicCompany> newCompanies) {
        if (!Iterables.elementsEqual(oldCompanies, newCompanies)) {
                log.debug("Detected a OR company sequence change");
                orPanel.recreate(oRound);
        }
    }

    protected void setLocalStep(LocalSteps localStep) {
        log.debug("Setting upgrade step to {}", localStep);

        SoundManager.notifyOfORLocalStep(localStep);
        this.localStep = localStep;

        updateMessage();
        updateUpgradesPanel();
    }

    public void updateUpgradesPanel() {

        if (upgradePanel != null) {
            log.debug("Initial localStep is {}", localStep);
            switch (localStep) {
            case INACTIVE:
                upgradePanel.setInactive();
                break;
            case SELECT_HEX:
                upgradePanel.setActive();
                break;
            case SELECT_UPGRADE:
                upgradePanel.setSelect(map.getSelectedHex());
                break;
            default:
                upgradePanel.setInactive();
            }
        }
        log.debug("Final localStep is {}", localStep);
    }

    private void displayRemainingTiles() {
        if (remainingTiles == null) {
            remainingTiles = new RemainingTilesWindow(orWindow);
        } else {
            remainingTiles.activate();
        }
    }

    /* If the token exchange limits are *per merged company*,
     * we need separator lines. This is used in 1826
     */
    private Integer[] separatorLines = null;
    public Integer[] getSeparatorLines() {return separatorLines;}
    public void clearSeparatorLines() {separatorLines = null; }

    private void prepareExchangeTokens (ExchangeTokens2 action) {
        prepareExchangeTokens(action, null);
    }

    private void prepareExchangeTokens (ExchangeTokens2 action, String errMsg) {

        List<String> options = new ArrayList<>();
        List<ExchangeTokens2.Location> locations = action.getLocations();
        List<Integer> sepLinesAfterOption = new ArrayList<>();

        PublicCompany newCompany = action.getNewCompany();
        int minimumExchanges = action.getMinNumberToExchange();
        int maximumExchanges = action.getMaxNumberToExchange();
        boolean perCompany = action.isExchangeCountPerCompany();

        ExchangeTokens2.Location location;
        PublicCompany oldCompany;
        PublicCompany prevOldCompany = null;
        Stop stop;

        for (int i=0; i<locations.size(); i++) {
            location = locations.get(i);
            oldCompany = location.getOldCompany();
            if (prevOldCompany != null && !oldCompany.equals(prevOldCompany)) {
                sepLinesAfterOption.add(i-1);
            }
            stop = location.getStop();
            options.add(LocalText.getText("SelectTokenExchangeOption",
                    oldCompany.getId(), stop.getStopComposedId()));
            prevOldCompany = oldCompany;
        }
        if (sepLinesAfterOption.size() > 0) {
            separatorLines = sepLinesAfterOption.toArray(new Integer[0]);
        }

        if (options.size() > 0) {
            orWindow.setVisible(true);
            orWindow.toFront();

            String title = LocalText.getText("SelectTokensToExchange");
            String prompt;
            if (perCompany) {
                prompt = LocalText.getText("SelectTokensToExchangePerComp",
                        maximumExchanges, newCompany);
            } else {
                prompt = LocalText.getText("SelectTokensToExchangeAllComps",
                        (minimumExchanges == maximumExchanges
                                ? maximumExchanges + ""
                                : minimumExchanges + "-" + maximumExchanges),
                        newCompany);
            }

            if (errMsg != null && errMsg.length() > 0) {
                prompt = "<html><font color=\"red\">" + errMsg + "</font><br>"
                        + prompt + "</html>";
            }

            CheckBoxDialog dialog = new CheckBoxDialog(EXCHANGE_TOKENS_DIALOG,
                    this,
                    orWindow,
                    title,
                    prompt,
                    options.toArray(new String[0]));
            setCurrentDialog (dialog, action);
        }
    }

    // Further Getters
    public MessagePanel getMessagePanel() {
        return messagePanel;
    }

    public UpgradesPanel getUpgradePanel() {
        return upgradePanel;
    }

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

    // FIXME: Getting the possible actions inside ORUIManager methods should be removed
    // Better transfer them by method args
    protected PossibleActions getPossibleActions() {
        return gameUIManager.getGameManager().getPossibleActions();
    }

    // DialogOwner interface methods
    @Override
    public void dialogActionPerformed () {

        JDialog currentDialog = getCurrentDialog();
        PossibleAction currentDialogAction = getCurrentDialogAction();

        if (currentDialog instanceof CheckBoxDialog) {

            CheckBoxDialog dialog = (CheckBoxDialog) currentDialog;

            if (currentDialogAction instanceof ReachDestinations) {
                ReachDestinations action = (ReachDestinations) currentDialogAction;

                boolean[] destined = dialog.getSelectedOptions();
                String[] options = dialog.getOptions();

                for (int index = 0; index < options.length; index++) {
                    if (destined[index]) {
                        action.addReachedCompany(action.getPossibleCompanies().get(index));
                    }
                }

                // Prevent that a null action gets processed
                if (action.getReachedCompanies() == null
                        || action.getReachedCompanies().isEmpty()) currentDialogAction = null;

            } else if (currentDialogAction instanceof ExchangeTokens2) {
                ExchangeTokens2 action = (ExchangeTokens2) currentDialogAction;
                boolean[] selected = dialog.getSelectedOptions();

                for (int i=0; i<action.getLocations().size(); i++) {
                    if (selected[i]) action.getLocations().get(i).setSelected();
                }

                int maxCount = action.getMaxNumberToExchange();
                int minCount = action.getMinNumberToExchange();
                PublicCompany newCompany = action.getNewCompany();
                String errMsg = "";

                // Some prevalidation
                if (action.isExchangeCountPerCompany()) {

                    Map<PublicCompany, Integer> counts = new HashMap<>();
                    for (ExchangeTokens2.Location location : action.getLocations()) {
                        PublicCompany company = location.getOldCompany();
                        int prevCount = (counts.containsKey(company) ? counts.get(company) : 0);
                        if (location.isSelected()) counts.put(company, prevCount + 1);
                    }
                    for (PublicCompany company : counts.keySet()) {
                        int count = counts.get(company);
                        if (count < minCount || count > maxCount) {
                            if (errMsg.length() > 0) errMsg += "<br>";
                            errMsg += LocalText.getText("WrongNumberOfTokensExchanged2",
                                    newCompany, minCount, maxCount, company, count);
                        }
                    }
                } else {
                    int count = 0;
                    for (ExchangeTokens2.Location location : action.getLocations()) {
                        if (location.isSelected()) count++;
                    }
                    if (count < minCount || count > maxCount) {
                        errMsg = LocalText.getText("WrongNumberOfTokensExchanged",
                                newCompany, minCount, maxCount, count);
                    }
                }
                if (errMsg.length() > 0) {
                    action.clearSelections();
                    prepareExchangeTokens(action, errMsg);
                    return;
                }
            }

        } else if (currentDialog instanceof ConfirmationDialog
                && currentDialogAction instanceof LayTile) {

            ConfirmationDialog dialog = (ConfirmationDialog) currentDialog;
            boolean gotPermission = dialog.getAnswer();
            if (gotPermission) {
                return;
            } else {
                currentDialogAction = null;
            }

        } else {
            currentDialogAction = null;
        }

        // Required even if no action is executed, to update the UI, re-enable buttons etc.
        gameUIManager.processAction(currentDialogAction);
    }

    @Override
    public JDialog getCurrentDialog() {
        return gameUIManager.getCurrentDialog();
    }

    @Override
    public PossibleAction getCurrentDialogAction () {
        return gameUIManager.getCurrentDialogAction();
    }

    @Override
    public void setCurrentDialog (JDialog dialog, PossibleAction action) {
        gameUIManager.setCurrentDialog(dialog, action);
        if (!(dialog instanceof MessageDialog)) orPanel.disableButtons();
    }

    /**
     * @return the hexUpgrades
     */
    protected GUIHexUpgrades getHexUpgrades() {
        return hexUpgrades;
    }
}
