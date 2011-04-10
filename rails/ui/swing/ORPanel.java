/* $Header: /Users/blentz/rails_rcs/cvs/18xx/rails/ui/swing/ORPanel.java,v 1.71 2010/06/17 22:10:53 stefanfrey Exp $*/
package rails.ui.swing;

import java.awt.*;
import java.awt.event.*;
import java.util.ArrayList;
import java.util.List;

import javax.swing.*;

import org.apache.log4j.Logger;

import org.jgrapht.graph.SimpleGraph;

import rails.algorithms.*;
import rails.common.GuiDef;
import rails.game.*;
import rails.game.action.*;
import rails.ui.swing.elements.*;
import rails.util.LocalText;
import rails.util.Util;

public class ORPanel extends GridPanel
implements ActionListener, KeyListener, RevenueListener {

    private static final long serialVersionUID = 1L;

    public static final String OPERATING_COST_CMD = "OperatingCost";
    public static final String BUY_PRIVATE_CMD = "BuyPrivate";
    public static final String BUY_TRAIN_CMD = "BuyTrain";
    private static final String WITHHOLD_CMD = "Withhold";
    private static final String SPLIT_CMD = "Split";
    public static final String PAYOUT_CMD = "Payout";
    public static final String SET_REVENUE_CMD = "SetRevenue";
    private static final String LAY_TILE_CMD = "LayTile";
    private static final String DONE_CMD = "Done";
    private static final String UNDO_CMD = "Undo";
    private static final String REDO_CMD = "Redo";
    public static final String REM_TILES_CMD = "RemainingTiles";
    private static final String NETWORK_INFO_CMD = "NetworkInfo";
    public static final String TAKE_LOANS_CMD = "TakeLoans";
    public static final String REPAY_LOANS_CMD = "RepayLoans";

    ORWindow orWindow;
    ORUIManager orUIManager;

    private JPanel statusPanel;
    private JPanel buttonPanel;

    private JMenuBar menuBar;
    private JMenu infoMenu;
    private JMenuItem remainingTilesMenuItem;
    private JMenu trainsInfoMenu;
    private JMenu phasesInfoMenu;
    private JMenu specialMenu;
    private JMenu loansMenu;
    private JMenu zoomMenu;
    private JMenuItem zoomIn, zoomOut;
    private ActionMenuItem takeLoans;
    private ActionMenuItem repayLoans;

    // Grid elements per function
    private Caption leftCompName[];
    private int leftCompNameXOffset, leftCompNameYOffset;
    private Caption rightCompName[];
    private int rightCompNameXOffset, rightCompNameYOffset;
    private Field president[];
    private int presidentXOffset, presidentYOffset;
    private Field sharePrice[];
    private int sharePriceXOffset, sharePriceYOffset;
    private Field cash[];
    private int cashXOffset, cashYOffset;
    private Field privates[];
    private int privatesXOffset, privatesYOffset;
    private Field newPrivatesCost[];
    private Field[] compLoans;
    private int loansXOffset, loansYOffset;
    private Field tiles[];
    private int tilesXOffset, tilesYOffset;
    private Field tileCost[];
    private Field tokens[];
    private Field tokenCost[];
    private Field tokensLeft[];
    private Field tokenBonus[];
    private int tokensXOffset, tokensYOffset;
    private Field revenue[];
    private Spinner revenueSelect[];
    private Field decision[];
    private int revXOffset, revYOffset;
    private Field trains[];
    private int trainsXOffset, trainsYOffset;
    private Field newTrainCost[];

    private boolean privatesCanBeBought = false;
    private boolean bonusTokensExist = false;
    private boolean hasCompanyLoans = false;

    private Caption tileCaption, tokenCaption, revenueCaption, trainCaption,
            privatesCaption, loansCaption;

    private ActionButton buttonOC; // sfy: button for operating costs
    private ActionButton button1;
    private ActionButton button2;
    private ActionButton button3;
    private ActionButton undoButton;
    private ActionButton redoButton;

    // Current state
    private int playerIndex = -1;
    private int orCompIndex = -1;

    private PublicCompanyI orComp = null;
    
    private RevenueAdapter revenueAdapter = null;
    private Thread revenueThread = null;

    protected static Logger log =
            Logger.getLogger(ORPanel.class.getPackage().getName());

    public ORPanel(ORWindow parent, ORUIManager orUIManager) {
        super();

        orWindow = parent;
        this.orUIManager = orUIManager;
        GameUIManager gameUIManager = parent.gameUIManager;

        statusPanel = new JPanel();
        gb = new GridBagLayout();
        statusPanel.setLayout(gb);
        statusPanel.setBorder(BorderFactory.createEtchedBorder());
        statusPanel.setOpaque(true);

        gridPanel = statusPanel;
        parentFrame = parent;

        round = gameUIManager.getCurrentRound();
//        noMapMode = gameUIManager.getGameParameterAsBoolean(GuiDef.Parm.NO_MAP_MODE);
        privatesCanBeBought = gameUIManager.getGameParameterAsBoolean(GuiDef.Parm.CAN_ANY_COMPANY_BUY_PRIVATES);
        bonusTokensExist = gameUIManager.getGameParameterAsBoolean(GuiDef.Parm.DO_BONUS_TOKENS_EXIST);
        hasCompanyLoans = gameUIManager.getGameParameterAsBoolean(GuiDef.Parm.HAS_ANY_COMPANY_LOANS);

        initButtonPanel();
        gbc = new GridBagConstraints();

        players = gameUIManager.getPlayers().toArray(new Player[0]);

        if (round instanceof OperatingRound) {
            companies = ((OperatingRound) round).getOperatingCompanies().toArray(new PublicCompanyI[0]);
            nc = companies.length;
        }

        initFields();

        setLayout(new BorderLayout());
        add(statusPanel, BorderLayout.CENTER);
        add(buttonPanel, BorderLayout.SOUTH);

        menuBar = new JMenuBar();

        infoMenu = new JMenu(LocalText.getText("Info"));
        infoMenu.setEnabled(true);
        remainingTilesMenuItem =
                new JMenuItem(LocalText.getText("RemainingTiles"));
        remainingTilesMenuItem.addActionListener(this);
        remainingTilesMenuItem.setActionCommand(REM_TILES_CMD);
        infoMenu.add(remainingTilesMenuItem);
        menuBar.add(infoMenu);

        addCompanyInfo();
        addTrainsInfo();
        addPhasesInfo();
        addNetworkInfo();

        specialMenu = new JMenu(LocalText.getText("SPECIAL"));
        specialMenu.setBackground(Color.YELLOW);
        // Normally not seen because menu is not opaque
        specialMenu.setEnabled(false);
        menuBar.add(specialMenu);

        if (hasCompanyLoans) {
            loansMenu = new JMenu (LocalText.getText("LOANS"));
            loansMenu.setEnabled(true);

            takeLoans = new ActionMenuItem (LocalText.getText("TakeLoans"));
            takeLoans.addActionListener(this);
            takeLoans.setEnabled(false);
            loansMenu.add(takeLoans);
            menuItemsToReset.add(takeLoans);

            repayLoans = new ActionMenuItem (LocalText.getText("RepayLoans"));
            repayLoans.addActionListener(this);
            repayLoans.setEnabled(false);
            loansMenu.add(repayLoans);
            menuItemsToReset.add(repayLoans);

            menuBar.add(loansMenu);
        }

        zoomMenu = new JMenu("Zoom");
        zoomMenu.setEnabled(true);
        zoomIn = new JMenuItem("In");
        zoomIn.addActionListener(this);
        zoomIn.setEnabled(true);
        zoomMenu.add(zoomIn);
        zoomOut = new JMenuItem("Out");
        zoomOut.addActionListener(this);
        zoomOut.setEnabled(true);
        zoomMenu.add(zoomOut);
        menuBar.add(zoomMenu);

        add(menuBar, BorderLayout.NORTH);

        setVisible(true);

        addKeyListener(this);
    }

    public void recreate(OperatingRound or) {
        log.debug("ORPanel.recreate() called");

        companies = or.getOperatingCompanies().toArray(new PublicCompanyI[0]);
        nc = companies.length;

        // Remove old fields. Don't forget to deregister the Observers
        deRegisterObservers();
        statusPanel.removeAll();

        // Create new fields
        initFields();
        repaint();
    }

    private void initButtonPanel() {
        buttonPanel = new JPanel();

        // sfy: operatingcosts button
        buttonOC = new ActionButton(LocalText.getText("OCButtonLabel"));
        buttonOC.setActionCommand(OPERATING_COST_CMD);
        buttonOC.setMnemonic(KeyEvent.VK_O);
        buttonOC.addActionListener(this);
        buttonOC.setEnabled(false);
        buttonOC.setVisible(false);
        buttonPanel.add(buttonOC);

        button1 = new ActionButton(LocalText.getText("LayTile"));
        button1.setActionCommand(LAY_TILE_CMD);
        button1.setMnemonic(KeyEvent.VK_T);
        button1.addActionListener(this);
        button1.setEnabled(true);
        buttonPanel.add(button1);

        button2 = new ActionButton(LocalText.getText("BUY_PRIVATE"));
        button2.setActionCommand(BUY_PRIVATE_CMD);
        button2.setMnemonic(KeyEvent.VK_V);
        button2.addActionListener(this);
        button2.setEnabled(false);
        button2.setVisible(false);
        buttonPanel.add(button2);

        button3 = new ActionButton(LocalText.getText("Done"));
        button3.setActionCommand(DONE_CMD);
        button3.setMnemonic(KeyEvent.VK_D);
        button3.addActionListener(this);
        button3.setEnabled(true);
        buttonPanel.add(button3);

        undoButton = new ActionButton(LocalText.getText("UNDO"));
        undoButton.setActionCommand(UNDO_CMD);
        undoButton.setMnemonic(KeyEvent.VK_U);
        undoButton.addActionListener(this);
        undoButton.setEnabled(false);
        buttonPanel.add(undoButton);

        redoButton = new ActionButton(LocalText.getText("REDO"));
        redoButton.setActionCommand(REDO_CMD);
        redoButton.setMnemonic(KeyEvent.VK_R);
        redoButton.addActionListener(this);
        redoButton.setEnabled(false);
        buttonPanel.add(redoButton);

        buttonPanel.setOpaque(true);
    }

    private void initFields() {
        leftCompName = new Caption[nc];
        rightCompName = new Caption[nc];
        president = new Field[nc];
        sharePrice = new Field[nc];
        cash = new Field[nc];
        trains = new Field[nc];
        privates = new Field[nc];
        tiles = new Field[nc];
        tileCost = new Field[nc];
        tokens = new Field[nc];
        tokenCost = new Field[nc];
        tokensLeft = new Field[nc];
        if (bonusTokensExist) tokenBonus = new Field[nc];
        if (hasCompanyLoans) compLoans = new Field[nc];
        revenue = new Field[nc];
        revenueSelect = new Spinner[nc];
        decision = new Field[nc];
        newTrainCost = new Field[nc];
        newPrivatesCost = new Field[nc];

        leftCompNameXOffset = 0;
        leftCompNameYOffset = 2;
        int currentXOffset = leftCompNameXOffset;
        int lastXWidth = 0;

        /* Top titles */
        addField(new Caption("Company"), 0, 0, lastXWidth = 1, 2,
                WIDE_BOTTOM + WIDE_RIGHT);

        presidentXOffset = currentXOffset += lastXWidth;
        presidentYOffset = leftCompNameYOffset;
        addField(new Caption("President"), presidentXOffset, 0, lastXWidth = 1,
                2, WIDE_BOTTOM);

        sharePriceXOffset = currentXOffset += lastXWidth;
        sharePriceYOffset = leftCompNameYOffset;
        addField(new Caption("<html>Share<br>value</html>"), sharePriceXOffset,
                0, lastXWidth = 1, 2, WIDE_BOTTOM);

        cashXOffset = currentXOffset += lastXWidth;
        cashYOffset = leftCompNameYOffset;
        addField(new Caption("Treasury"), cashXOffset, 0, lastXWidth = 1, 2,
                WIDE_BOTTOM + WIDE_RIGHT);

        if (privatesCanBeBought) {
            privatesXOffset = currentXOffset += lastXWidth;
            privatesYOffset = leftCompNameYOffset;
            addField(privatesCaption = new Caption("Privates"),
                    privatesXOffset, 0, lastXWidth = 2, 1, WIDE_RIGHT);
            addField(new Caption("owned"), privatesXOffset, 1, 1, 1,
                    WIDE_BOTTOM);
            addField(new Caption("cost"), privatesXOffset + 1, 1, 1, 1,
                    WIDE_BOTTOM + WIDE_RIGHT);
        }

        if (hasCompanyLoans) {
            loansXOffset = currentXOffset += lastXWidth;
            loansYOffset = leftCompNameYOffset;
            addField (loansCaption = new Caption(LocalText.getText("LOANS")),
                    loansXOffset, 0, lastXWidth = 1, 2, WIDE_RIGHT);
        }

        tilesXOffset = currentXOffset += lastXWidth;
        tilesYOffset = leftCompNameYOffset;
        addField(tileCaption = new Caption("Tiles"), tilesXOffset, 0,
                lastXWidth = 2, 1, WIDE_RIGHT);
        addField(new Caption("laid"), tilesXOffset, 1, 1, 1, WIDE_BOTTOM);
        addField(new Caption("cost"), tilesXOffset + 1, 1, 1, 1, WIDE_BOTTOM
                                                                 + WIDE_RIGHT);

        tokensXOffset = currentXOffset += lastXWidth;
        tokensYOffset = leftCompNameYOffset;
        lastXWidth = bonusTokensExist ? 4 : 3;
        addField(tokenCaption = new Caption("Tokens"), tokensXOffset, 0,
                lastXWidth, 1, WIDE_RIGHT);
        addField(new Caption("laid"), tokensXOffset, 1, 1, 1, WIDE_BOTTOM);
        addField(new Caption("cost"), tokensXOffset + 1, 1, 1, 1, WIDE_BOTTOM);
        addField(new Caption("left"), tokensXOffset + 2, 1, 1, 1,
                WIDE_BOTTOM + (bonusTokensExist ? 0 : WIDE_RIGHT));
        if (bonusTokensExist) {
            addField(new Caption("bonus"), tokensXOffset + 3, 1, 1, 1,
                    WIDE_BOTTOM + WIDE_RIGHT);
        }

        revXOffset = currentXOffset += lastXWidth;
        revYOffset = leftCompNameYOffset;
        addField(revenueCaption = new Caption("Revenue"), revXOffset, 0,
                lastXWidth = 2, 1, WIDE_RIGHT);
        addField(new Caption("earned"), revXOffset, 1, 1, 1, WIDE_BOTTOM);
        addField(new Caption("payout"), revXOffset + 1, 1, 1, 1, WIDE_BOTTOM
                                                                 + WIDE_RIGHT);

        trainsXOffset = currentXOffset += lastXWidth;
        trainsYOffset = leftCompNameYOffset;
        addField(trainCaption = new Caption("Trains"), trainsXOffset, 0,
                lastXWidth = 2, 1, WIDE_RIGHT);
        addField(new Caption("owned"), trainsXOffset, 1, 1, 1, WIDE_BOTTOM);
        addField(new Caption("cost"), trainsXOffset + 1, 1, 1, 1, WIDE_BOTTOM
                                                                  + WIDE_RIGHT);

        rightCompNameXOffset = currentXOffset += lastXWidth;
        rightCompNameYOffset = leftCompNameYOffset;
        addField(new Caption("Company"), rightCompNameXOffset, 0, 1, 2,
                WIDE_BOTTOM);

        fields = new JComponent[1+currentXOffset][2+nc];
        rowVisibilityObservers = new RowVisibility[nc];

        for (int i = 0; i < nc; i++) {
            c = companies[i];
            rowVisibilityObservers[i]
                    = new RowVisibility (this, leftCompNameYOffset + i, c.getInGameModel(), true);
            observers.add(rowVisibilityObservers[i]);

            boolean visible = !c.isClosed();

            f = leftCompName[i] = new Caption(c.getName());
            f.setBackground(c.getBgColour());
            f.setForeground(c.getFgColour());
            addField(f, leftCompNameXOffset, leftCompNameYOffset + i, 1, 1,
                    WIDE_RIGHT, visible);

            f =
                    president[i] =
//                            new Field(c.hasStarted() && !c.isClosed()
//                                    ? c.getPresident().getNameAndPriority() : "");
                        new Field(c.getPresidentModel());
            addField(f, presidentXOffset, presidentYOffset + i, 1, 1, 0, visible);

            f = sharePrice[i] = new Field(c.getCurrentPriceModel());
            addField(f, sharePriceXOffset, sharePriceYOffset + i, 1, 1, 0, visible);

            f = cash[i] = new Field(c.getCashModel());
            addField(f, cashXOffset, cashYOffset + i, 1, 1, WIDE_RIGHT, visible);

            if (privatesCanBeBought) {
                f =
                        privates[i] =
                                new Field(
                                        c.getPortfolio().getPrivatesOwnedModel());
                addField(f, privatesXOffset, privatesYOffset + i, 1, 1,
                        WIDE_RIGHT, visible);

                f =
                        newPrivatesCost[i] =
                                new Field(c.getPrivatesSpentThisTurnModel());
                addField(f, privatesXOffset + 1, privatesYOffset + i, 1, 1,
                        WIDE_RIGHT, visible);
            }

            if (hasCompanyLoans) {
                //if (c.canLoan()) {
                if (c.getLoanValueModel() != null) {
                    f = compLoans[i] = new Field (c.getLoanValueModel());
                } else {
                    f = compLoans[i] = new Field ("");
                }
                addField (f, loansXOffset, loansYOffset + i, 1, 1, WIDE_RIGHT, visible);
            }

            f = tiles[i] = new Field(c.getTilesLaidThisTurnModel());
            addField(f, tilesXOffset, tilesYOffset + i, 1, 1, 0, visible);

            f = tileCost[i] = new Field(c.getTilesCostThisTurnModel());
            addField(f, tilesXOffset + 1, tilesYOffset + i, 1, 1, WIDE_RIGHT, visible);

            f = tokens[i] = new Field(c.getTokensLaidThisTurnModel());
            addField(f, tokensXOffset, tokensYOffset + i, 1, 1, 0, visible);

            f = tokenCost[i] = new Field(c.getTokensCostThisTurnModel());
            addField(f, tokensXOffset + 1, tokensYOffset + i, 1, 1, 0, visible);

            f = tokensLeft[i] = new Field(c.getBaseTokensModel());
            addField(f, tokensXOffset + 2, tokensYOffset + i, 1, 1,
                    bonusTokensExist ? 0 : WIDE_RIGHT, visible);

            if (bonusTokensExist) {
                f = tokenBonus[i] = new Field(c.getBonusTokensModel());
                addField(f, tokensXOffset + 3, tokensYOffset + i, 1, 1,
                        WIDE_RIGHT, visible);
            }

            f = revenue[i] = new Field(c.getLastRevenueModel());
            addField(f, revXOffset, revYOffset + i, 1, 1, 0, visible);
            f = revenueSelect[i] = new Spinner(0, 0, 0, 10);
            addField(f, revXOffset, revYOffset + i, 1, 1, 0,  false);
            // deactived below, as this caused problems by gridpanel rowvisibility function -- sfy
            //            revenue[i].addDependent(revenueSelect[i]);

            f = decision[i] = new Field(c.getLastRevenueAllocationModel());
            addField(f, revXOffset + 1, revYOffset + i, 1, 1, WIDE_RIGHT,  visible);

            f = trains[i] = new Field(c.getPortfolio().getTrainsModel());
            addField(f, trainsXOffset, trainsYOffset + i, 1, 1, 0,  visible);

            f = newTrainCost[i] = new Field(c.getTrainsSpentThisTurnModel());
            addField(f, trainsXOffset + 1, trainsYOffset + i, 1, 1, WIDE_RIGHT,  visible);

            f = rightCompName[i] = new Caption(c.getName());
            f.setBackground(companies[i].getBgColour());
            f.setForeground(companies[i].getFgColour());
            addField(f, rightCompNameXOffset, rightCompNameYOffset + i, 1, 1, 0,  visible);

        }

    }

    protected void addCompanyInfo() {

    	CompanyManagerI cm = orUIManager.getGameUIManager().getGameManager().getCompanyManager();
    	List<CompanyTypeI> comps = cm.getCompanyTypes();
    	JMenu compMenu, menu, item;

        compMenu = new JMenu(LocalText.getText("Companies"));
        compMenu.setEnabled(true);
        infoMenu.add(compMenu);

    	for (CompanyTypeI type : comps) {
    		menu = new JMenu (LocalText.getText(type.getName()));
    		menu.setEnabled(true);
            compMenu.add(menu);

    		for (CompanyI comp : type.getCompanies()) {
    			item = new JMenu(comp.getName());
    			item.setEnabled(true);
    			item.add(new JMenuItem(comp.getInfoText()));
    			menu.add(item);
    		}
    	}
    }

    protected void addTrainsInfo() {

        TrainManager tm = orWindow.getGameUIManager().getGameManager().getTrainManager();
        List<TrainTypeI> types = tm.getTrainTypes();
        JMenu item;

        trainsInfoMenu = new JMenu(LocalText.getText("TRAINS"));
        trainsInfoMenu.setEnabled(true);
        infoMenu.add(trainsInfoMenu);

        for (TrainTypeI type : types) {
            item = new JMenu (LocalText.getText("N_Train", type.getName()));
            item.setEnabled(true);
            item.add(new JMenuItem(type.getInfo()));
            trainsInfoMenu.add(item);
        }
    }

    protected void addPhasesInfo() {

        PhaseManager pm = orWindow.getGameUIManager().getGameManager().getPhaseManager();
        List<Phase> phases = pm.getPhases();
        JMenu item;
        StringBuffer b = new StringBuffer("<html>");

        phasesInfoMenu = new JMenu(LocalText.getText("Phases"));
        phasesInfoMenu.setEnabled(true);
        infoMenu.add(phasesInfoMenu);

        for (Phase phase : phases) {
            b.setLength(6);
            appendInfoText(b, LocalText.getText("PhaseTileColours", phase.getTileColoursString()));
            appendInfoText(b, LocalText.getText("PhaseNumberOfORs", phase.getNumberOfOperatingRounds()));
            appendInfoText(b, LocalText.getText("PhaseOffBoardStep", phase.getOffBoardRevenueStep()));
            if (phase.doPrivatesClose()) {
                appendInfoText(b, LocalText.getText("PhaseClosesAllPrivates"));
            }
            if (phase.getClosedObjects() != null) {
                for (Closeable object : phase.getClosedObjects()) {
                    if (Util.hasValue(object.getClosingInfo())) {
                        appendInfoText(b, LocalText.getText("PhaseRemoves", Util.lowerCaseFirst(object.getClosingInfo())));
                    }
                }
            }
            if (Util.hasValue(phase.getInfo())) {
                appendInfoText(b, phase.getInfo());
            }
            item = new JMenu (LocalText.getText("PhaseX", phase.getName()));
            item.setEnabled(true);
            item.add(new JMenuItem(b.toString()));
            phasesInfoMenu.add(item);
        }
    }

    protected void addNetworkInfo() {

        boolean route_highlight = orUIManager.gameUIManager.getGameParameterAsBoolean(GuiDef.Parm.ROUTE_HIGHLIGHT);
        boolean revenue_suggest = orUIManager.gameUIManager.getGameParameterAsBoolean(GuiDef.Parm.REVENUE_SUGGEST); 
        
        if (!route_highlight && !revenue_suggest) return; 
        
        JMenu networkMenu = new JMenu(LocalText.getText("NetworkInfo"));
        networkMenu.setEnabled(true);
        infoMenu.add(networkMenu);
        
        if (route_highlight) {
            JMenuItem item = new JMenuItem("Network");
            item.addActionListener(this);
            item.setActionCommand(NETWORK_INFO_CMD);
            networkMenu.add(item);
        }
        
        if (revenue_suggest) {
            CompanyManagerI cm = orUIManager.getGameUIManager().getGameManager().getCompanyManager();
            for (PublicCompanyI comp : cm.getAllPublicCompanies()) {
                if (!comp.hasFloated() || comp.isClosed()) continue;
                JMenuItem item = new JMenuItem(comp.getName());
                item.addActionListener(this);
                item.setActionCommand(NETWORK_INFO_CMD);
                networkMenu.add(item);
            }
        }
    }
    
    protected void executeNetworkInfo(String companyName) {
        GameManagerI gm = orUIManager.getGameUIManager().getGameManager();
        
        if (companyName.equals("Network")) {
            NetworkGraphBuilder nwGraph = NetworkGraphBuilder.create(gm);
            SimpleGraph<NetworkVertex, NetworkEdge> mapGraph = nwGraph.getMapGraph();
            
//            NetworkGraphBuilder.visualize(mapGraph, "Map Network");
            mapGraph = NetworkGraphBuilder.optimizeGraph(mapGraph);
            NetworkGraphBuilder.visualize(mapGraph, "Optimized Map Network");
        } else {
            CompanyManagerI cm = gm.getCompanyManager();
            PublicCompanyI company = cm.getPublicCompany(companyName);
//
//            NetworkGraphBuilder nwGraph = NetworkGraphBuilder.create(gm);
//            NetworkCompanyGraph companyGraph = NetworkCompanyGraph.create(nwGraph, company);
//            companyGraph.createRouteGraph(false);
//            companyGraph.createRevenueGraph(new ArrayList<NetworkVertex>());
//            Multigraph<NetworkVertex, NetworkEdge> graph= companyGraph.createPhaseTwoGraph();
//            NetworkGraphBuilder.visualize(graph, "Phase Two Company Network");
//            JOptionPane.showMessageDialog(orWindow, 
//                    "Vertices = " + graph.vertexSet().size() + ", Edges = " + graph.edgeSet().size());
            List<String> addTrainList = new ArrayList<String>();
            boolean anotherTrain = true;
            RevenueAdapter ra = null;
            while (anotherTrain) {
                // multi
                ra = RevenueAdapter.createRevenueAdapter(gm, company, gm.getCurrentPhase());
                for (String addTrain:addTrainList) {
                    ra.addTrainByString(addTrain);
                }
                ra.initRevenueCalculator(true); // true => multigraph, false => simplegraph
                log.debug("Revenue Adapter:" + ra);
                int revenueValue = ra.calculateRevenue();
                log.debug("Revenue Value:" + revenueValue);
                log.debug("Revenue Run:" + ra.getOptimalRunPrettyPrint(true));
                ra.drawOptimalRunAsPath(orUIManager.getMap());
                orUIManager.getMap().repaint();
                JOptionPane.showMessageDialog(orWindow, "RevenueValue = " + revenueValue +
                        "\nRevenueRun = \n" + ra.getOptimalRunPrettyPrint(true));
                
                String trainString =
                    JOptionPane.showInputDialog(null, "Enter train string (Examples: 5, 3+3, 4D, 6E, D)",
                    "Add another train to run?",
                    JOptionPane.QUESTION_MESSAGE);
                if (trainString == null || trainString.equals("")) {
                    anotherTrain = false;
                } else {
                    addTrainList.add(trainString);
                }

            }
            revenueAdapter = ra;
        }
    }
    
    private void appendInfoText (StringBuffer b, String text) {
        if (text == null || text.length() == 0) return;
        if (b.length() > 6) b.append("<br>");
        b.append(text);
    }

    public void finish() {

        buttonOC.setEnabled(false); // operatingcosts sfy
        button1.setEnabled(false);
        button2.setEnabled(false);
        button3.setEnabled(false);
        undoButton.setEnabled(false);
        redoButton.setEnabled(false);

    }

    public void actionPerformed(ActionEvent actor) {

        // What kind action has been taken?
        JComponent source = (JComponent) actor.getSource();
        String command = actor.getActionCommand();
        List<PossibleAction> executedActions = null;
        PossibleAction executedAction = null;

        if (source instanceof ActionTaker) {
            executedActions = ((ActionTaker) source).getPossibleActions();
            // In most cases we have only one
            if (!executedActions.isEmpty()) {
                executedAction = executedActions.get(0);
                // In all cases, the actions in the list must be
                // instances of the same class
                log.debug("Action taken is " + executedAction.toString());
            }

            if (executedAction instanceof SetDividend) {
                // Hide the spinner here, because we might not return
                // via InitPayoutStep, where this would otherwise be done.
                setSelect(revenue[orCompIndex], revenueSelect[orCompIndex],
                        false);
            }

            orUIManager.processAction(command, executedActions);
        } else if (source == zoomIn) {
            orWindow.getMapPanel().zoomIn();
            if (revenueAdapter != null) {
                revenueAdapter.drawOptimalRunAsPath(orUIManager.getMap());
                orUIManager.getMap().repaint();
            }
        } else if (source == zoomOut) {
            orWindow.getMapPanel().zoomOut();
            if (revenueAdapter != null) {
                revenueAdapter.drawOptimalRunAsPath(orUIManager.getMap());
                orUIManager.getMap().repaint();
            }
        } else if (command == NETWORK_INFO_CMD) {
            JMenuItem item = (JMenuItem)actor.getSource();
            executeNetworkInfo(item.getText());
        } else {
            orUIManager.processAction(command, null);
        }
    }

    public int getRevenue(int orCompIndex) {
        return ((Integer) revenueSelect[orCompIndex].getValue()).intValue();

    }

    public void setRevenue(int orCompIndex, int amount) {
        revenue[orCompIndex].setText(Bank.format(amount));
    }

    public void resetActions() {
        tileCaption.setHighlight(false);
        tokenCaption.setHighlight(false);
        revenueCaption.setHighlight(false);
        trainCaption.setHighlight(false);
        if (privatesCanBeBought) privatesCaption.setHighlight(false);
        for (int i = 0; i < president.length; i++) {
            president[i].setHighlight(false);
        }

        if (hasCompanyLoans) {
            loansCaption.setHighlight(false);
        }
        
        for (JMenuItem item : menuItemsToReset) {
            item.setEnabled(false);
            if (item instanceof ActionMenuItem) {
                ((ActionMenuItem)item).clearPossibleActions();
            }
        }
        undoButton.setEnabled(false);
        
    }

    public void resetORCompanyTurn(int orCompIndex) {

        for (int i = 0; i < nc; i++) {
            setSelect(revenue[i], revenueSelect[i], false);
        }
    }

    public void resetCurrentRevenueDisplay() {
        setSelect(revenue[orCompIndex], revenueSelect[orCompIndex], false);
    }
    
    
    public void initORCompanyTurn(PublicCompanyI orComp, int orCompIndex) {

        this.orComp = orComp;
        this.orCompIndex = orCompIndex;
        president[orCompIndex].setHighlight(true);

        buttonOC.clearPossibleActions();
        button1.clearPossibleActions();
        button2.clearPossibleActions();
        button3.clearPossibleActions();

        buttonOC.setEnabled(false);
        button1.setEnabled(false);
        button2.setEnabled(false);
        button3.setEnabled(false);
    }

    public void initTileLayingStep() {

        tileCaption.setHighlight(true);
        button1.setVisible(false);
    }

    public void initTokenLayingStep() {

        tokenCaption.setHighlight(true);
        button1.setEnabled(false);
        button1.setVisible(false);
        button3.setEnabled(false);
    }

    public void initRevenueEntryStep(int orCompIndex, SetDividend action) {

        revenueCaption.setHighlight(true);
        revenueSelect[orCompIndex].setValue(action.getPresetRevenue());
        
        setSelect(revenue[orCompIndex], revenueSelect[orCompIndex], true);

        button1.setText(LocalText.getText("SET_REVENUE"));
        button1.setActionCommand(SET_REVENUE_CMD);
        button1.setPossibleAction(action);
        button1.setMnemonic(KeyEvent.VK_R);
        button1.setEnabled(true);
        button1.setVisible(true);

        // initialize and start the revenue adapter
        if (orUIManager.gameUIManager.getGameParameterAsBoolean(GuiDef.Parm.REVENUE_SUGGEST)) {
            revenueAdapter = initRevenueCalculation(orComp);
            revenueThread = new Thread(revenueAdapter);
            revenueThread.start();
        }
    }

    private RevenueAdapter initRevenueCalculation(PublicCompanyI company){
        GameManagerI gm = orUIManager.getGameUIManager().getGameManager();
        RevenueAdapter ra = RevenueAdapter.createRevenueAdapter(gm, company, gm.getCurrentPhase());
        ra.initRevenueCalculator(true);
        ra.addRevenueListener(this);
        return ra;
    }
    
    public void revenueUpdate(int bestRevenue, boolean finalResult) {
        revenueSelect[orCompIndex].setValue(bestRevenue);
        if (finalResult) {
            revenueAdapter.drawOptimalRunAsPath(orUIManager.getMap());
            orUIManager.getMap().repaint();
            orUIManager.addInformation("Best Run Value = " + bestRevenue +
                    " with " + Util.convertToHtml(revenueAdapter.getOptimalRunPrettyPrint(false)));
            orUIManager.addDetail(Util.convertToHtml(revenueAdapter.getOptimalRunPrettyPrint(true)));
        }
    }
    
    public void stopRevenueUpdate() {
        orUIManager.getMap().setTrainPaths(null);
        if (revenueThread != null) {
            revenueThread.interrupt();
            revenueThread = null;
        }
        if (revenueAdapter != null) {
            revenueAdapter.removeRevenueListener();
            revenueAdapter = null;
        }
    }
    
    
    public void initPayoutStep(int orCompIndex, SetDividend action,
            boolean withhold, boolean split, boolean payout) {

        SetDividend clonedAction;

        setSelect(revenue[orCompIndex], revenueSelect[orCompIndex], false);

        if (withhold) {
            button1.setText(LocalText.getText("WITHHOLD"));
            button1.setActionCommand(WITHHOLD_CMD);
            clonedAction = (SetDividend) action.clone();
            clonedAction.setRevenueAllocation(SetDividend.WITHHOLD);
            button1.setPossibleAction(clonedAction);
            button1.setMnemonic(KeyEvent.VK_W);
            button1.setEnabled(true);
            button1.setVisible(true);
        } else {
            button1.setVisible(false);
        }

        if (split) {
            button2.setText(LocalText.getText("SPLIT"));
            button2.setActionCommand(SPLIT_CMD);
            clonedAction = (SetDividend) action.clone();
            clonedAction.setRevenueAllocation(SetDividend.SPLIT);
            button2.setPossibleAction(clonedAction);
            button2.setMnemonic(KeyEvent.VK_S);
            button2.setEnabled(true);
            button2.setVisible(true);
        } else {
            button2.setVisible(false);
        }

        if (payout) {
            button3.setText(LocalText.getText("PAYOUT"));
            button3.setActionCommand(PAYOUT_CMD);
            clonedAction = (SetDividend) action.clone();
            clonedAction.setRevenueAllocation(SetDividend.PAYOUT);
            button3.setPossibleAction(clonedAction);
            button3.setMnemonic(KeyEvent.VK_P);
            button3.setEnabled(true);
        } else {
            button3.setVisible(false);
        }
    }

    public void initTrainBuying(boolean enabled) {

        trainCaption.setHighlight(true);

        button1.setText(LocalText.getText("BUY_TRAIN"));
        button1.setActionCommand(BUY_TRAIN_CMD);
        button1.setMnemonic(KeyEvent.VK_T);
        button1.setEnabled(enabled);
        button1.setVisible(true);
    }

    // operating costs sfy
    public void initOperatingCosts(boolean enabled) {

        buttonOC.setEnabled(enabled);
        buttonOC.setVisible(enabled);
        tileCaption.setHighlight(enabled);
        tokenCaption.setHighlight(enabled);
    }


    public void initPrivateBuying(boolean enabled) {

        if (privatesCanBeBought) {
            if (enabled) {
                button2.setText(LocalText.getText("BUY_PRIVATE"));
                button2.setActionCommand(BUY_PRIVATE_CMD);
                button2.setMnemonic(KeyEvent.VK_V);
            }
            button2.setEnabled(enabled);
            button2.setVisible(enabled);
            privatesCaption.setHighlight(enabled);
        } else {
            button2.setVisible(false);
        }
    }

    public void initSpecialActions() {

        specialMenu.removeAll();
        specialMenu.setEnabled(false);
        specialMenu.setOpaque(false);
    }

    public void addSpecialAction(PossibleAction action, String text) {

        ActionMenuItem item = new ActionMenuItem(text);
        item.addActionListener(this);
        item.addPossibleAction(action);
        specialMenu.add(item);
        specialMenu.setEnabled(true);
        specialMenu.setOpaque(true);
    }

    public void enableDone(NullAction action) {

        button3.setText(LocalText.getText("Done"));
        button3.setActionCommand(DONE_CMD);
        button3.setMnemonic(KeyEvent.VK_D);
        button3.setPossibleAction(action);
        button3.setEnabled(true);
    }

    public void enableUndo(GameAction action) {
        undoButton.setEnabled(action != null);
        if (action != null) undoButton.setPossibleAction(action);
    }

    public void enableRedo(GameAction action) {
        redoButton.setEnabled(action != null);
        if (action != null) redoButton.setPossibleAction(action);
    }

    public void enableLoanTaking (TakeLoans action) {
        if (action != null) takeLoans.addPossibleAction(action);
        takeLoans.setEnabled(action != null);
    }

    public void enableLoanRepayment (RepayLoans action) {

        repayLoans.setPossibleAction(action);
        repayLoans.setEnabled(true);

        loansCaption.setHighlight(true);

        button1.setText(LocalText.getText("RepayLoans"));
        button1.setActionCommand(REPAY_LOANS_CMD);
        button1.setPossibleAction(action);
        button1.setMnemonic(KeyEvent.VK_R);
        button1.setEnabled(true);
        button1.setVisible(true);
    }

    public void finishORCompanyTurn(int orCompIndex) {

        for (Field field : president) {
            field.setHighlight(false);
        }

        button1.setEnabled(false);

        orCompIndex = -1;
    }

    // TEMPORARY
    public PublicCompanyI getORComp() {
        return orComp;
    }

    public String getORPlayer() {
        if (playerIndex >= 0)
            return players[playerIndex].getName();
        else
            return "";
    }

    private void setSelect(JComponent f, JComponent s, boolean active) {
        f.setVisible(!active);
        s.setVisible(active);
    }

    public PublicCompanyI[] getOperatingCompanies() {
        return companies;
    }


}