package net.sf.rails.ui.swing;

import com.google.common.collect.Lists;
import net.sf.rails.algorithms.*;
import net.sf.rails.common.Config;
import net.sf.rails.common.GuiDef;
import net.sf.rails.common.LocalText;
import net.sf.rails.game.*;
import net.sf.rails.game.financial.Bank;
import net.sf.rails.ui.swing.elements.*;
import net.sf.rails.ui.swing.hexmap.HexHighlightMouseListener;
import net.sf.rails.util.Util;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import rails.game.action.*;

import javax.swing.*;
import java.awt.*;
import java.awt.event.*;
import java.util.ArrayList;
import java.util.List;


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
    private static final String DONE_CMD = "Done";
    private static final String UNDO_CMD = "Undo";
    private static final String REDO_CMD = "Redo";
    public static final String REM_TILES_CMD = "RemainingTiles";
    private static final String NETWORK_INFO_CMD = "NetworkInfo";
    public static final String TAKE_LOANS_CMD = "TakeLoans";
    public static final String REPAY_LOANS_CMD = "RepayLoans";

    private ORWindow orWindow;
    private ORUIManager orUIManager;

    private JPanel statusPanel;
    private JPanel buttonPanel;

    private JMenuBar menuBar;
    private JMenu infoMenu;
    private JMenuItem remainingTilesMenuItem;
    private JMenu trainsInfoMenu;
    private JMenu phasesInfoMenu;
    private JMenu networkInfoMenu;
    private JMenu specialMenu;
    private JMenu loansMenu;
    private JMenu zoomMenu;
    private JMenuItem zoomIn, zoomOut, fitToWindow, fitToWidth, fitToHeight, calibrateMap;
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
    private int rightsXOffset, rightsYOffset;
    private Field rights[];
    /**
     * For the direct Income to be entered during the OR Phase
     */
    private Spinner directIncomeSelect[];
    private Field directIncomeRevenue[];
    private int bonusRevXOffset, bonusRevYOffset;

    private boolean privatesCanBeBought = false;
    private boolean bonusTokensExist = false;
    private boolean hasCompanyLoans = false;
    private boolean hasRights = false;
    private boolean hasDirectCompanyIncomeInOr = false;

    // Configured properties
    private boolean showAllCompanies = "always".equalsIgnoreCase(
            Config.get("orPanel.showAllCompanies",""));
    private boolean showSpinner = "yes".equalsIgnoreCase(
            Config.get("orPanel.showSpinner",""));

    private Caption tileCaption, tokenCaption, revenueCaption, trainCaption,
    privatesCaption, loansCaption, directIncomeCaption;

    private ActionButton buttonOC; // sfy: button for operating costs
    private ActionButton button1;
    private ActionButton button2;
    private ActionButton button3;
    private ActionButton undoButton;
    private ActionButton redoButton;

    // Current state
    private int orCompIndex = -1;

    private int nc;
    private PublicCompany[] companies;

    private PublicCompany orComp = null;

    private boolean isRevenueValueToBeSet = false;
    private RevenueAdapter revenueAdapter = null;
    private Thread revenueThread = null;

    private List<JFrame> openWindows = new ArrayList<>();

    protected static final Logger log = LoggerFactory.getLogger(ORPanel.class);

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
        hasRights = gameUIManager.getGameParameterAsBoolean(GuiDef.Parm.HAS_ANY_RIGHTS);
        hasDirectCompanyIncomeInOr= gameUIManager.getGameParameterAsBoolean(GuiDef.Parm.HAS_SPECIAL_COMPANY_INCOME);

        initButtonPanel();
        gbc = new GridBagConstraints();

        players = gameUIManager.getPlayerManager();

        if (round instanceof OperatingRound) {
            companies = ((OperatingRound) round).getOperatingCompanies().toArray(new PublicCompany[0]);
            nc = companies.length;
        }
        log.debug("companies found: {} in {}", nc, round);

        initFields();

        setLayout(new BorderLayout());
        add(statusPanel, BorderLayout.CENTER);

        //only add button panel directly for conventional layout
        if (!parent.isDockingFrameworkEnabled()) {
            add(buttonPanel, BorderLayout.SOUTH);
        }

        menuBar = new JMenuBar();

        infoMenu = new JMenu(LocalText.getText("Info"));
        infoMenu.setEnabled(true);

        //only add remaining tiles display option for conventional layout
        //as this is always included as a dockable panel in the docking frame layout
        if (!parent.isDockingFrameworkEnabled()) {
            remainingTilesMenuItem = new JMenuItem(LocalText.getText("RemainingTiles"));
            remainingTilesMenuItem.addActionListener(this);
            remainingTilesMenuItem.setActionCommand(REM_TILES_CMD);
            infoMenu.add(remainingTilesMenuItem);
        }

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
        fitToWindow = createFitToMenuItem("Fit to window");
        if (fitToWindow.isSelected()) orWindow.getMapPanel().fitToWindow();
        zoomMenu.add(fitToWindow);
        fitToWidth = createFitToMenuItem("Fit to width");
        if (fitToWidth.isSelected()) orWindow.getMapPanel().fitToWidth();
        zoomMenu.add(fitToWidth);
        fitToHeight = createFitToMenuItem("Fit to height");
        if (fitToHeight.isSelected()) orWindow.getMapPanel().fitToHeight();
        zoomMenu.add(fitToHeight);
        calibrateMap = new JMenuItem("CalibrateMap");
        calibrateMap.addActionListener(this);
        calibrateMap.setEnabled(Config.isDevelop());
        zoomMenu.add(calibrateMap);
        menuBar.add(zoomMenu);

        // only add menu bar for conventional layout
        // (otherwise part of DockingFrame)
        if (!parent.isDockingFrameworkEnabled()) {
            add(menuBar, BorderLayout.NORTH);
        }

        setVisible(true);

        addKeyListener(this);
    }

    private JCheckBoxMenuItem createFitToMenuItem(String name) {
        JCheckBoxMenuItem menuItem = new JCheckBoxMenuItem(name);
        menuItem.addActionListener(this);
        menuItem.setEnabled(true);

        //check whether this is the default fit to option
        if (name.equalsIgnoreCase(Config.get("map.defaultZoomFitOption"))) {
            menuItem.setSelected(true);
        }
        return menuItem;
    }

    public void recreate(OperatingRound or) {
        log.debug("ORPanel.recreate() called");

        companies = or.getOperatingCompanies().toArray(new PublicCompany[0]);
        nc = companies.length;

        // Remove old fields. Don't forget to deregister the Observers
        deRegisterObservers();
        statusPanel.removeAll();

        // Create new fields
        initFields();

        // update the networkInfo menu
        // TODO: This relies on a recreate as soon as companies have changed
        addNetworkInfo();

        repaint();
    }

    private void initButtonPanel() {

        // sfy: operatingcosts button
        buttonOC = new ActionButton(RailsIcon.OPERATING_COST);
        buttonOC.setActionCommand(OPERATING_COST_CMD);
        buttonOC.setMnemonic(KeyEvent.VK_O);
        buttonOC.addActionListener(this);
        buttonOC.setEnabled(false);
        buttonOC.setVisible(false);

        button1 = new ActionButton(null);
        button1.addActionListener(this);
        button1.setEnabled(false);

        button2 = new ActionButton(RailsIcon.BUY_PRIVATE);
        button2.setActionCommand(BUY_PRIVATE_CMD);
        button2.setMnemonic(KeyEvent.VK_V);
        button2.addActionListener(this);
        button2.setEnabled(false);
        button2.setVisible(false);

        button3 = new ActionButton(RailsIcon.DONE);
        button3.setActionCommand(DONE_CMD);
        button3.setMnemonic(KeyEvent.VK_D);
        button3.addActionListener(this);
        button3.setEnabled(false);

        undoButton = new ActionButton(RailsIcon.UNDO);
        undoButton.setActionCommand(UNDO_CMD);
        undoButton.setMnemonic(KeyEvent.VK_U);
        undoButton.addActionListener(this);
        undoButton.setEnabled(false);

        redoButton = new ActionButton(RailsIcon.REDO);
        redoButton.setActionCommand(REDO_CMD);
        redoButton.setMnemonic(KeyEvent.VK_R);
        redoButton.addActionListener(this);
        redoButton.setEnabled(false);

        //choose button panel layout depending on whether panel becomes a dockable
        if (orWindow.isDockingFrameworkEnabled()) {

            //customized panel for dockable layout
            //the minimal size is defined by the size of one button
            //(aim here: user can choose whether buttons are laid out
            //           vertically or horizontally or in a grid, since
            //           the minimal size's restriction is minimal indeed.)
            buttonPanel = new JPanel() {
                private static final long serialVersionUID = 1L;
                @Override
                public Dimension getMinimumSize() {
                    int width = 0;
                    int height = 0;
                    if (getComponents().length != 0) {
                        //getting the first component is sufficient as their
                        //size is all the same
                        width = getComponents()[0].getPreferredSize().width;
                        height = getComponents()[0].getPreferredSize().height;
                    }
                    //add a margin
                    width += 10;
                    height += 10;
                    return new Dimension(width,height);
                }
                @Override
                public Dimension getPreferredSize() {
                    return getMinimumSize();
                }
            };

        } else {
            //plain panel for conventional layout
            buttonPanel = new JPanel();
        }

        buttonPanel.add(buttonOC);
        buttonPanel.add(button1);
        buttonPanel.add(button2);
        buttonPanel.add(button3);
        buttonPanel.add(undoButton);
        buttonPanel.add(redoButton);

        //for dockable button panel, ensure that all buttons have the same size
        //(necessary, otherwise vertical/box layout will look ugly)
        if (orWindow.isDockingFrameworkEnabled()) {

            //get maximum size
            Dimension maxSize = new Dimension();
            for (Component c : buttonPanel.getComponents() ) {
                if (c.getPreferredSize().width > maxSize.width)
                    maxSize.width = c.getPreferredSize().width;
                if (c.getPreferredSize().height > maxSize.height)
                    maxSize.height = c.getPreferredSize().height;
            }
            //apply maximum size to all buttons
            for (Component c : buttonPanel.getComponents() ) {
                c.setPreferredSize(maxSize);
            }

        }

        buttonPanel.setOpaque(true);

    }

    public MouseListener getCompanyCaptionMouseClickListener() {
        return new MouseListener() {
            @Override
            public void mouseClicked(MouseEvent e) {
                if (e.getComponent() instanceof Caption) {
                    Caption c = (Caption)e.getComponent();
                    executeNetworkInfo(c.getText());
                }
            }
            @Override
            public void mouseExited(MouseEvent e) {}
            @Override
            public void mouseEntered(MouseEvent e) {}
            @Override
            public void mousePressed(MouseEvent e) {}
            @Override
            public void mouseReleased(MouseEvent e) {}
        };
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
        if (hasRights) rights = new Field[nc];
        revenue = new Field[nc];
        revenueSelect = new Spinner[nc];
        if (hasDirectCompanyIncomeInOr) {
            directIncomeRevenue = new Field[nc];
            directIncomeSelect = new Spinner[nc];
        }
        decision = new Field[nc];
        newTrainCost = new Field[nc];
        newPrivatesCost = new Field[nc];

        leftCompNameXOffset = 0;
        leftCompNameYOffset = 2;
        int currentXOffset = leftCompNameXOffset;
        int lastXWidth = 0;

        MouseListener companyCaptionMouseClickListener = getCompanyCaptionMouseClickListener();

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
                    loansXOffset, 0, lastXWidth = 1, 2, WIDE_BOTTOM + WIDE_RIGHT);
        }

        if (hasRights) {
            rightsXOffset = currentXOffset += lastXWidth;
            rightsYOffset = leftCompNameYOffset;
            addField (new Caption(LocalText.getText("RIGHTS")),
                    rightsXOffset, 0, lastXWidth = 1, 2, WIDE_RIGHT);
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
        addField(revenueCaption = new Caption("Revenue"), revXOffset, 0, lastXWidth = 2, 1, WIDE_RIGHT);
        addField(new Caption("earned"), revXOffset, 1, 1, 1, WIDE_BOTTOM);
        addField(new Caption("payout"), revXOffset + 1, 1, 1, 1, WIDE_BOTTOM + WIDE_RIGHT);

        if (hasDirectCompanyIncomeInOr) {
            bonusRevXOffset = currentXOffset += lastXWidth;
            bonusRevYOffset =leftCompNameYOffset;
            addField(directIncomeCaption = new Caption("CompanyIncome"), bonusRevXOffset, 0, lastXWidth = 3, 1, WIDE_RIGHT);
            addField(new Caption("income"), revXOffset, 1, 1, 1, WIDE_BOTTOM + WIDE_RIGHT);
        }

        trainsXOffset = currentXOffset += lastXWidth;
        trainsYOffset = leftCompNameYOffset;
        addField(trainCaption = new Caption("Trains"), trainsXOffset, 0, lastXWidth = 2, 1, WIDE_RIGHT);
        addField(new Caption("owned"), trainsXOffset, 1, 1, 1, WIDE_BOTTOM);
        addField(new Caption("cost"), trainsXOffset + 1, 1, 1, 1, WIDE_BOTTOM + WIDE_RIGHT);

        rightCompNameXOffset = currentXOffset += lastXWidth;
        rightCompNameYOffset = leftCompNameYOffset;
        addField(new Caption("Company"), rightCompNameXOffset, 0, 1, 2, WIDE_BOTTOM);

        fields = new JComponent[1+currentXOffset][2+nc];
        rowVisibilityObservers = new RowVisibility[nc];

        for (int i = 0; i < nc; i++) {
            c = companies[i];
            rowVisibilityObservers[i] = new RowVisibility(this, leftCompNameYOffset + i, c.getInGameModel(), true);
            observers.add(rowVisibilityObservers[i]);

            boolean visible = !c.isClosed();

            f = leftCompName[i] = new Caption(c.getId());
            f.setBackground(c.getBgColour());
            f.setForeground(c.getFgColour());
            HexHighlightMouseListener.addMouseListener(f,
                    orUIManager,c,false);
            f.addMouseListener(companyCaptionMouseClickListener);
            f.setToolTipText(LocalText.getText("NetworkInfoDialogTitle", c.getId()));
            addField(f, leftCompNameXOffset, leftCompNameYOffset + i, 1, 1,
                    WIDE_RIGHT, visible);

            f = president[i] =
                    //                            new Field(c.hasStarted() && !c.isClosed()
                    //                                    ? c.getPresident().getNameAndPriority() : "");
                    new Field(c.getPresidentModel());
            addField(f, presidentXOffset, presidentYOffset + i, 1, 1, 0, visible);

            if (c.hasStockPrice()) {
                f = sharePrice[i] = new Field(c.getCurrentPriceModel());
                ((Field) f).setColorModel(c.getCurrentPriceModel());
                addField(f, sharePriceXOffset, sharePriceYOffset + i, 1, 1, 0, visible);
            }
            f = cash[i] = new Field(c.getPurseMoneyModel());
            addField(f, cashXOffset, cashYOffset + i, 1, 1, WIDE_RIGHT, visible);

            if (privatesCanBeBought) {
                f =
                        privates[i] =
                                new Field(
                                        c.getPortfolioModel().getPrivatesOwnedModel());
                HexHighlightMouseListener.addMouseListener(f,
                        orUIManager, c.getPortfolioModel());
                addField(f, privatesXOffset, privatesYOffset + i, 1, 1,
                        0, visible);

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

            if (hasRights) {
                f = rights[i] = new Field (c.getRightsModel());
                addField (f, rightsXOffset, rightsYOffset + i, 1, 1, WIDE_RIGHT, visible);
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

            f = revenueSelect[i] = new Spinner(0, 0, 0,
                    orUIManager.getGameUIManager().getGameManager().getRevenueSpinnerIncrement());
            //align spinner size with field size
            //(so that changes to visibility don't affect panel sizing)
            f.setPreferredSize(revenue[i].getPreferredSize());
            addField(f, revXOffset, revYOffset + i, 1, 1, 0,  false);
            // deactived below, as this caused problems by gridpanel rowvisibility function -- sfy
            //            revenue[i].addDependent(revenueSelect[i]);

            f = decision[i] = new Field(c.getLastRevenueAllocationModel());
            addField(f, revXOffset + 1, revYOffset + i, 1, 1, WIDE_RIGHT,  visible);

            if(hasDirectCompanyIncomeInOr) {
                f = directIncomeRevenue[i] = new Field(c.getLastDirectIncomeModel());
                addField(f, bonusRevXOffset, bonusRevYOffset + i, 1, 1, 0, visible);

                f = directIncomeSelect[i] = new Spinner(0, 0, 0,
                        orUIManager.getGameUIManager().getGameManager().getRevenueSpinnerIncrement());

                f.setPreferredSize(directIncomeRevenue[i].getPreferredSize());
                addField(f, bonusRevXOffset, bonusRevYOffset + i, 1, 1, 0,  false);
            }

            f = trains[i] = new Field(c.getPortfolioModel().getTrainsModel());
            addField(f, trainsXOffset, trainsYOffset + i, 1, 1, 0,  visible);

            f = newTrainCost[i] = new Field(c.getTrainsSpentThisTurnModel());
            addField(f, trainsXOffset + 1, trainsYOffset + i, 1, 1, WIDE_RIGHT,  visible);

            f = rightCompName[i] = new Caption(c.getId());
            f.setBackground(companies[i].getBgColour());
            f.setForeground(companies[i].getFgColour());
            HexHighlightMouseListener.addMouseListener(f, orUIManager,c,false);
            f.addMouseListener(companyCaptionMouseClickListener);
            f.setToolTipText(LocalText.getText("NetworkInfoDialogTitle",c.getId()));
            addField(f, rightCompNameXOffset, rightCompNameYOffset + i, 1, 1, 0,  visible);
        }

    }

    protected void addCompanyInfo() {

    	CompanyManager cm = orUIManager.getGameUIManager().getGameManager().getRoot().getCompanyManager();
    	List<CompanyType> comps = cm.getCompanyTypes();
        JMenu compMenu, menu, item;

        compMenu = new JMenu(LocalText.getText("Companies"));
        compMenu.setEnabled(true);
        infoMenu.add(compMenu);

    	for (CompanyType type : comps) {
    		menu = new JMenu (LocalText.getText(type.getId()));
            menu.setEnabled(true);
            compMenu.add(menu);

    		for (Company comp : type.getCompanies()) {
    			item = new JMenu(comp.getId());
                item.setEnabled(true);
                JMenuItem menuItem = new JMenuItem(comp.getInfoText());
    			if (comp instanceof PrivateCompany) {
                    //highlighting on menu items always enabled irrespective of config
                    HexHighlightMouseListener.addMouseListener(menuItem,
                            orUIManager,(PrivateCompany)comp,true);
                    HexHighlightMouseListener.addMouseListener(item,
                            orUIManager,(PrivateCompany)comp,true);
                }
                if (comp instanceof PublicCompany) {
                    //highlighting on menu items always enabled irrespective of config
                    HexHighlightMouseListener.addMouseListener(menuItem,
                            orUIManager,(PublicCompany)comp,true);
                    HexHighlightMouseListener.addMouseListener(item,
                            orUIManager,(PublicCompany)comp,true);
                }
                item.add(menuItem);
                menu.add(item);
            }
        }
    }

    protected void addTrainsInfo() {

        TrainManager tm = orWindow.getGameUIManager().getRoot().getTrainManager();
        List<TrainType> types = tm.getTrainTypes();
        JMenu item;

        trainsInfoMenu = new JMenu(LocalText.getText("TRAINS"));
        trainsInfoMenu.setEnabled(true);
        infoMenu.add(trainsInfoMenu);

        for (TrainType type : types) {
            item = new JMenu (LocalText.getText("N_Train", type.getName()));
            item.setEnabled(true);
            item.add(new JMenuItem(type.getInfo()));
            trainsInfoMenu.add(item);
        }
    }

    protected void addPhasesInfo() {

        PhaseManager pm = orWindow.getGameUIManager().getRoot().getPhaseManager();
        List<Phase> phases = pm.getPhases();
        JMenu item;
        StringBuffer b;
        b = new StringBuffer("<html>");

        phasesInfoMenu = new JMenu(LocalText.getText("Phases"));
        phasesInfoMenu.setEnabled(true);
        infoMenu.add(phasesInfoMenu);

        for (Phase phase : phases) {
            b.setLength(6);
            appendInfoText(b, LocalText.getText("PhaseTileColours", phase.getTileColoursString()));
            appendInfoText(b, LocalText.getText("PhaseNumberOfORs", phase.getNumberOfOperatingRounds()));
            appendInfoText(b, LocalText.getText("PhaseOffBoardStep", phase.getOffBoardRevenueStep()));
            appendInfoText(b, LocalText.getText("PhaseTrainLimitStep", phase.getTrainLimitStep()));
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
            item = new JMenu (LocalText.getText("PhaseX", phase.toText()));
            item.setEnabled(true);
            item.add(new JMenuItem(b.toString()));
            phasesInfoMenu.add(item);
        }
    }

    protected void addNetworkInfo() {
        if (networkInfoMenu != null) infoMenu.remove(networkInfoMenu);
        networkInfoMenu = createNetworkInfo();
        if (networkInfoMenu == null) return;
        networkInfoMenu.setEnabled(true);
        infoMenu.add(networkInfoMenu);
    }

    protected JMenu createNetworkInfo() {

        boolean route_highlight = orUIManager.gameUIManager.getGameParameterAsBoolean(GuiDef.Parm.ROUTE_HIGHLIGHT);
        boolean revenue_suggest = orUIManager.gameUIManager.getGameParameterAsBoolean(GuiDef.Parm.REVENUE_SUGGEST);

        if (!route_highlight && !revenue_suggest) return null;

        JMenu networkMenu = new JMenu(LocalText.getText("NetworkInfo"));

        //network graphs only for developers
        if (route_highlight && Config.isDevelop()) {
            JMenuItem item = new JMenuItem("Network");
            item.addActionListener(this);
            item.setActionCommand(NETWORK_INFO_CMD);
            networkMenu.add(item);
        }

        if (revenue_suggest) {
            CompanyManager cm = orUIManager.getGameUIManager().getGameManager().getRoot().getCompanyManager();
            for (PublicCompany comp : cm.getAllPublicCompanies()) {
                if (!comp.hasFloated() || comp.isClosed()) continue;
                JMenuItem item = new JMenuItem(comp.getId());
                item.addActionListener(this);
                item.setActionCommand(NETWORK_INFO_CMD);
                networkMenu.add(item);
            }
        }

        return networkMenu;
    }

    protected void executeNetworkInfo(String companyName) {
        RailsRoot root = orUIManager.getGameUIManager().getRoot();

        if (companyName.equals("Network")) {
            NetworkAdapter network = NetworkAdapter.create(root);
            NetworkGraph mapGraph = network.getMapGraph();
            mapGraph.optimizeGraph();
            JFrame mapWindow = mapGraph.visualize("Optimized Map Network");
            if ( mapWindow != null ) {
                openWindows.add(mapWindow);
            }
        } else {
            CompanyManager cm = root.getCompanyManager();
            PublicCompany company = cm.getPublicCompany(companyName);
            //handle the case of invalid parameters
            //could occur if the method is not invoked by the menu (but by the click listener)
            if (company == null) return;
            if ( Config.getBoolean("map.route.window.display", true) ) {
                NetworkAdapter network = NetworkAdapter.create(root);
                NetworkGraph routeGraph = network.getRevenueGraph(company, Lists.<NetworkVertex>newArrayList());
                JFrame mapWindow = routeGraph.visualize("Route Network for " + company);
                if ( mapWindow != null ) {
                    openWindows.add(mapWindow);
                }
            }
            List<String> addTrainList = new ArrayList<String>();
            boolean anotherTrain = true;
            RevenueAdapter ra;
            while (anotherTrain) {
                // multi
                ra = RevenueAdapter.createRevenueAdapter(root, company, root.getPhaseManager().getCurrentPhase());
                for (String addTrain:addTrainList) {
                    ra.addTrainByString(addTrain);
                }
                ra.initRevenueCalculator(true); // true => multigraph, false => simplegraph
                log.debug("Revenue Adapter:{}", ra);
                int revenueValue = ra.calculateRevenue();
                log.debug("Revenue Value:{}", revenueValue);
                log.debug("Revenue Run:{}", ra.getOptimalRunPrettyPrint(true));
                //try-catch clause temporary workaround as revenue adapter's
                //convertRcRun might erroneously raise exceptions
                try {ra.drawOptimalRunAsPath(orUIManager.getMap());}
                catch (Exception e) {}
                /**
                 * TODO: Here the automatic calculation of the Special Company Income needs to be implemented
                 * 1837: Coal Mines
                 * 1853: Mail Contract
                 * 1822: Mail Contract
                 * 1822CA: Mail Contract
                 * 1854 old/new : Mail Contract ?
                 */


                if (!Config.isDevelop()) {
                    //parent component is ORPanel so that dialog won't hide the routes painted on the map
                    JOptionPane.showMessageDialog(this,
                            LocalText.getText("NetworkInfoDialogMessage",company.getId(),
                                    orUIManager.getGameUIManager().format(revenueValue)) ,
                            LocalText.getText("NetworkInfoDialogTitle",company.getId()),
                            JOptionPane.INFORMATION_MESSAGE);
                    //train simulation only for developers
                    break;
                }

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
            //clean up the paths on the map
            orUIManager.getMap().setTrainPaths(null);
            //but retain paths already existing before
            if (revenueAdapter != null) {
                //try-catch clause temporary workaround as revenue adapter's
                //convertRcRun might erroneously raise exceptions
                try {revenueAdapter.drawOptimalRunAsPath(orUIManager.getMap());}
                catch (Exception e) {}
            }
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

        disableRoutesDisplay();

        //clear all highlighting (president column and beyond)
        resetActions();

    }

    public void redrawRoutes() {
        if (revenueAdapter != null && isDisplayRoutes()) {
            //try-catch clause temporary workaround as revenue adapter's
            //convertRcRun might erroneously raise exceptions
            try {revenueAdapter.drawOptimalRunAsPath(orUIManager.getMap());}
            catch (Exception e) {}
        }
    }

    @Override
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
                log.debug("Action taken is {}", executedAction);
            }

            if (executedAction instanceof SetDividend) {
                // Hide the spinner here, because we might not return
                // via InitPayoutStep, where this would otherwise be done.
                /*
                if(hasDirectCompanyIncomeInOr) {
                    setSelect(revenue[orCompIndex], revenueSelect[orCompIndex],
                            directIncomeSelect[orCompIndex], directIncomeRevenue[orCompIndex], false);
                } else {
                    setSelect(revenue[orCompIndex], revenueSelect[orCompIndex], false);
                }*/

                SetDividend action = (SetDividend) executedAction;
                // The spinner value may have changed, copy value to the text version
                if (revenueSelect[orCompIndex].isVisible()) {
                    revenue[orCompIndex].setText(format(getRevenue(orCompIndex)));
                }
                selectRevenueSpinner(false);
                // If revenue is zero, the company apparently has no route,
                // because without a train we wouldn't be here.
                if (getRevenue(orCompIndex) == 0) {
                    decision[orCompIndex].setText(LocalText.getText("NoRoute"));
                /*  Does not work well.
                 *  TODO to be revisited
                } else if (action.getAllowedAllocations().length == 1) {
                    int allocation = action.getAllowedAllocations()[0];
                    if (allocation >= 0 && allocation < SetDividend.NUM_OPTIONS) {
                        decision[orCompIndex].setText(SetDividend.getAllocationNameKey(allocation));
                    }*/
                }
            }

            orUIManager.processAction(command, executedActions);
        } else if (source == zoomIn) {
            fitToWindow.setSelected(false);
            fitToWidth.setSelected(false);
            fitToHeight.setSelected(false);
            orWindow.getMapPanel().zoom(true);
        } else if (source == zoomOut) {
            fitToWindow.setSelected(false);
            fitToWidth.setSelected(false);
            fitToHeight.setSelected(false);
            orWindow.getMapPanel().zoom(false);
        } else if (source == fitToWindow) {
            if (fitToWindow.isSelected()) {
                fitToWidth.setSelected(false);
                fitToHeight.setSelected(false);
                orWindow.getMapPanel().fitToWindow();
            } else {
                orWindow.getMapPanel().removeFitToOption();
            }
        } else if (source == fitToWidth) {
            if (fitToWidth.isSelected()) {
                fitToWindow.setSelected(false);
                fitToHeight.setSelected(false);
                orWindow.getMapPanel().fitToWidth();
            } else {
                orWindow.getMapPanel().removeFitToOption();
            }
        } else if (source == fitToHeight) {
            if (fitToHeight.isSelected()) {
                fitToWindow.setSelected(false);
                fitToWidth.setSelected(false);
                orWindow.getMapPanel().fitToHeight();
            } else {
                orWindow.getMapPanel().removeFitToOption();
            }
        } else if (source == calibrateMap) {
            MapManager mapManager = orUIManager.getMap().getMapManager();
            String offsetX = JOptionPane.showInputDialog(this, "Change translation in X-dimension", mapManager.getMapXOffset());
            try {
                mapManager.setMapXOffset(Integer.parseInt(offsetX));
            } catch (NumberFormatException e) {} // do nothing
            String offsetY = JOptionPane.showInputDialog(this, "Change translation in Y-dimension", mapManager.getMapYOffset());
            try {
                mapManager.setMapYOffset(Integer.parseInt(offsetY));
            } catch (NumberFormatException e) {} // do nothing
            String scale = JOptionPane.showInputDialog(this, "Change scale", mapManager.getMapScale());
            try {
                mapManager.setMapScale(Float.parseFloat(scale));
            } catch (NumberFormatException e) {} // do nothing
            orWindow.getMapPanel().zoom(true);
            orWindow.getMapPanel().zoom(false);
        } else if (command == NETWORK_INFO_CMD) {
            JMenuItem item = (JMenuItem)actor.getSource();
            executeNetworkInfo(item.getText());
        } else {
            orUIManager.processAction(command, null);
        }
    }



    public int getRevenue(int orCompIndex) {
        return (Integer) revenueSelect[orCompIndex].getValue();
    }

    public void setRevenue(int orCompIndex, int amount) {
        revenueSelect[orCompIndex].setValue(amount);
        revenue[orCompIndex].setText(format(amount));
    }

    public void resetActions() {
        tileCaption.setHighlight(false);
        tokenCaption.setHighlight(false);
        revenueCaption.setHighlight(false);
        if(hasDirectCompanyIncomeInOr) {
            directIncomeCaption.setHighlight(false);
        }
        trainCaption.setHighlight(false);
        if (privatesCanBeBought) privatesCaption.setHighlight(false);
        for ( Field field : president ) {
            field.setHighlight(false);
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

        removeAllHighlights();
    }

    public void resetORCompanyTurn(int orCompIndex) {
        for (int i = 0; i < nc; i++) {
            /*
            if (hasDirectCompanyIncomeInOr) {
                setSelect(revenue[i], revenueSelect[i], directIncomeSelect[i], directIncomeRevenue[i], false);
            } else {
            setSelect(revenue[i], revenueSelect[i], false);
            }*/
            selectRevenueSpinner(false);
        }
    }

    // No longer used?
    public void resetCurrentRevenueDisplay() {
        if (hasDirectCompanyIncomeInOr) {
            setSelect(revenue[orCompIndex], revenueSelect[orCompIndex],
                    directIncomeSelect[orCompIndex], directIncomeRevenue[orCompIndex], false);
        } else {
        setSelect(revenue[orCompIndex], revenueSelect[orCompIndex], false);
        }
    }

    /**
     *
     * @return True if route should be displayed (at least for the set revenue step)
     */
    private boolean isDisplayRoutes() {
        return (orUIManager.gameUIManager.getGameParameterAsBoolean(GuiDef.Parm.ROUTE_HIGHLIGHT));
    }

    private boolean isSuggestRevenue() {
        return (orUIManager.gameUIManager.getGameParameterAsBoolean(GuiDef.Parm.REVENUE_SUGGEST));
    }

    /**
     *
     * @return True if the routes of the currently active company should be displayed.
     * As a prerequisite of this feature, route highlighting has to be enabled/supported.
     */
    private boolean isDisplayCurrentRoutes() {
        return (isDisplayRoutes()
                && "yes".equalsIgnoreCase(Config.get("map.displayCurrentRoutes")));
    }

    /**
     * any routes currently displayed on the map are removed
     * In addition, revenue adapter and its thread are interrupted / removed.
     */
    private void disableRoutesDisplay() {
        clearRevenueAdapter();
        orUIManager.getMap().setTrainPaths(null);
    }

    private void clearRevenueAdapter() {
        if (revenueThread != null) {
            revenueThread.interrupt();
            revenueThread = null;
        }
        if (revenueAdapter != null) {
            revenueAdapter.removeRevenueListener();
            revenueAdapter = null;
        }
    }

    private void updateCurrentRoutes(boolean isSetRevenueStep) {

        // initialize and start the revenue adapter if routes to be displayed
        // or revenue to be suggested in the revenue step
        if (isDisplayCurrentRoutes() || (isSuggestRevenue() && isSetRevenueStep)) {

            //only consider revenue quantification for the set revenue step and only
            //if suggest option is on
            isRevenueValueToBeSet = isSetRevenueStep ? isSuggestRevenue() : false;

            RailsRoot root = orUIManager.getGameUIManager().getRoot();
            revenueAdapter = RevenueAdapter.createRevenueAdapter(root, orComp, root.getPhaseManager().getCurrentPhase());
            revenueAdapter.initRevenueCalculator(true);
            revenueAdapter.addRevenueListener(this);
            revenueThread = new Thread(revenueAdapter);
            revenueThread.start();
        } else {

            //remove current routes also if display option is not active
            //(as it could have just been turned off)
            clearRevenueAdapter();
            disableRoutesDisplay();
        }

    }

    public void initORCompanyTurn(PublicCompany orComp, int orCompIndex) {

        this.orComp = orComp;
        this.orCompIndex = orCompIndex;
        president[orCompIndex].setHighlight(true);

        removeAllHighlights();

        buttonOC.clearPossibleActions();
        button1.clearPossibleActions();
        button2.clearPossibleActions();
        button3.clearPossibleActions();

        buttonOC.setEnabled(false);
        button1.setEnabled(false);
        button2.setEnabled(false);
        button3.setEnabled(false);

        updateCurrentRoutes(false);
    }

    public void initTileLayingStep() {

        tileCaption.setHighlight(true);
        setHighlight(tiles[orCompIndex],true);
        setHighlight(tileCost[orCompIndex],true);
        button1.setVisible(false);

        setCompanyVisibility(false);
        selectRevenueSpinner(false);
    }

    public void initTokenLayingStep() {

        tokenCaption.setHighlight(true);
        setHighlight(tokens[orCompIndex],true);
        setHighlight(tokenCost[orCompIndex],true);
        setHighlight(tokensLeft[orCompIndex],true);
        if (tokenBonus != null) setHighlight(tokenBonus[orCompIndex],true);
        button1.setEnabled(false);
        button1.setVisible(false);
        button3.setEnabled(false);

        setCompanyVisibility(false);
        selectRevenueSpinner(false);
    }

    public void initRevenueEntryStep(int orCompIndex, SetDividend action) {

        revenueCaption.setHighlight(true);
        setHighlight(revenueSelect[orCompIndex],true);
        //decision[orCompIndex].setText("");  // Somehow this prevents the actual decision to be shown
        setRevenue (orCompIndex, action.getPresetRevenue());
        //revenueSelect[orCompIndex].setValue(action.getPresetRevenue());
        //revenue[orCompIndex].setText(format(action.getPresetRevenue()));
        selectRevenueSpinner (true);
        /*
        if ( hasDirectCompanyIncomeInOr) {
            directIncomeSelect[orCompIndex].setValue(action.getPresetCompanyTreasuryRevenue());
            setSelect(revenue[orCompIndex], revenueSelect[orCompIndex], directIncomeSelect[orCompIndex],
                    directIncomeRevenue[orCompIndex], true);
        } else {
            setSelect(revenue[orCompIndex], revenueSelect[orCompIndex],true);
        }*/
        button1.setRailsIcon(RailsIcon.SET_REVENUE);
        button1.setActionCommand(SET_REVENUE_CMD);
        button1.setPossibleAction(action);
        button1.setMnemonic(KeyEvent.VK_R);
        button1.setEnabled(true);
        button1.setVisible(true);

        //indicate interest in setting revenue values (and not only displaying routes)
        updateCurrentRoutes(true);

        //setCompanyVisibility(false);
    }

    @Override
    public void revenueUpdate(int bestRevenue, boolean finalResult) {
        if (isRevenueValueToBeSet) {
            setRevenue (orCompIndex, bestRevenue);
            //revenueSelect[orCompIndex].setValue(bestRevenue);
            //revenue[orCompIndex].setText(format(bestRevenue));
            /**
             * This needs to have another value for the automatic setting of a Direct Income for a Company
             */
            if(hasDirectCompanyIncomeInOr) {
                directIncomeSelect[orCompIndex].setValue(0);
            }
        }
        if (finalResult) {
            orUIManager.getMap().setTrainPaths(null);
            //try-catch clause temporary workaround as revenue adapter's
            //convertRcRun might erroneously raise exceptions
            //leaving on exception is admissible as exception only occur
            //if revenue would be 0.
            try {
                revenueAdapter.drawOptimalRunAsPath(orUIManager.getMap());

                if (isRevenueValueToBeSet) {
                    orUIManager.getMessagePanel().setInformation("Best Run Value = " + bestRevenue +
                            " with " + Util.convertToHtml(revenueAdapter.getOptimalRunPrettyPrint(false)));
                    orUIManager.getMessagePanel().setDetail(
                            Util.convertToHtml(revenueAdapter.getOptimalRunPrettyPrint(true)));
                }
            }
            catch (Exception e) {}
        }
        //selectRevenueSpinner (false);
    }

    public void stopRevenueUpdate() {
        isRevenueValueToBeSet = false;
    }


    public void initPayoutStep(int orCompIndex, SetDividend action,
            boolean withhold, boolean split, boolean payout) {

        setHighlight(decision[orCompIndex],true);

        SetDividend clonedAction;
        /*
        if(hasDirectCompanyIncomeInOr) {
            setSelect(revenue[orCompIndex], revenueSelect[orCompIndex],
                    directIncomeSelect[orCompIndex], directIncomeRevenue[orCompIndex], false);
        } else {
            setSelect(revenue[orCompIndex], revenueSelect[orCompIndex], false);
        }
        */
        selectRevenueSpinner(false);
        if (withhold) {
            button1.setRailsIcon(RailsIcon.WITHHOLD);
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
            button2.setRailsIcon(RailsIcon.SPLIT);
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
            button3.setRailsIcon(RailsIcon.PAYOUT);
            button3.setActionCommand(PAYOUT_CMD);
            clonedAction = (SetDividend) action.clone();
            clonedAction.setRevenueAllocation(SetDividend.PAYOUT);
            button3.setPossibleAction(clonedAction);
            button3.setMnemonic(KeyEvent.VK_P);
            button3.setEnabled(true);
        } else {
            button3.setVisible(false);
        }

        setCompanyVisibility(false);
    }

    public void initTrainBuying(boolean enabled) {

        trainCaption.setHighlight(true);
        setHighlight(trains[orCompIndex],true);
        setHighlight(newTrainCost[orCompIndex],true);

        button1.setRailsIcon(RailsIcon.BUY_TRAIN);
        button1.setActionCommand(BUY_TRAIN_CMD);
        button1.setMnemonic(KeyEvent.VK_T);
        button1.setEnabled(enabled);
        button1.setVisible(true);

        setCompanyVisibility(true);
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
                button2.setRailsIcon(RailsIcon.BUY_PRIVATE);
                button2.setActionCommand(BUY_PRIVATE_CMD);
                button2.setMnemonic(KeyEvent.VK_V);
            }
            button2.setEnabled(enabled);
            button2.setVisible(enabled);
            privatesCaption.setHighlight(enabled);
            setHighlight(privates[orCompIndex],enabled);
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

        button3.setRailsIcon(RailsIcon.DONE);
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
        setHighlight(compLoans[orCompIndex],true);

        button1.setRailsIcon(RailsIcon.REPAY_LOANS);
        button1.setActionCommand(REPAY_LOANS_CMD);
        button1.setPossibleAction(action);
        button1.setMnemonic(KeyEvent.VK_R);
        button1.setEnabled(true);
        button1.setVisible(true);
    }

    public void disableButtons () {
        button1.setEnabled(false);
        button2.setEnabled(false);
        button3.setEnabled(false);
        undoButton.setEnabled(false);
        redoButton.setEnabled(false);
    }

    public void finishORCompanyTurn(int orCompIndex) {
        //clear all highlighting (president column and beyond)
        resetActions();

        button1.setEnabled(false);

        orUIManager.getMap().setTrainPaths(null);
    }

    private void setSelect(JComponent f, JComponent s, boolean active) {
        f.setVisible(!active);
        s.setVisible(active);
    }

    private void setSelect(JComponent f, JComponent s, JComponent s2,
            JComponent f2, boolean active) {
        f.setVisible(!active);
        s.setVisible(active);
        f2.setVisible(!active);
        s2.setVisible(active);
    }

    // EV: to replace the above two methods
    private void selectRevenueSpinner (boolean active) {
        selectRevenueSpinner (orCompIndex, active);
    }

    private void selectRevenueSpinner (int compIndex, boolean active) {
        boolean revActive = active && showSpinner;
        revenue[compIndex].setVisible(!revActive);
        revenueSelect[compIndex].setVisible(revActive);
        if (revActive) {
            int oldValue = parseOldValue(revenue[compIndex].getText());
            revenueSelect[compIndex].setValue(oldValue);
        }
        boolean dciActive = !active && showSpinner;
        if (hasDirectCompanyIncomeInOr) {
            directIncomeRevenue[compIndex].setVisible(dciActive);
            directIncomeSelect[compIndex].setVisible(!dciActive);
            if (!dciActive) {
                int oldValue = parseOldValue(directIncomeRevenue[compIndex].getText());
                directIncomeSelect[compIndex].setValue(oldValue);
            }
        }
    }

    public String format(int amount) {
        return orUIManager.getGameUIManager().format(amount);
    }

    private int parseOldValue (String text) {
        if (Util.hasValue(text)) {
            return Integer.parseInt(text.replaceAll("[^0-9]", ""));
        } else {
            return 0;
        }
    }

    public PublicCompany[] getOperatingCompanies() {
        return companies;
    }

    public JPanel getButtonPanel() {
        return buttonPanel;
    }

    public JMenuBar getMenuBar() {
        return menuBar;
    }

    /**
     * Adds buttons to the button panel (adjusting their size to the standard size)
     * @param index The position where to add the buttons
     */
    public void addToButtonPanel(RailsIconButton[] buttons, int index) {
        //get standard size
        Dimension standardSize = null;
        Component[] existingButtons = buttonPanel.getComponents();
        if (existingButtons != null && existingButtons.length > 0) {
            standardSize = existingButtons[0].getPreferredSize();
        }

        //apply sizing to new buttons
        //add buttons to the panel
        for (int i=buttons.length-1 ; i >= 0 ; i--) {
            buttons[i].setPreferredSize(standardSize);
            buttonPanel.add(buttons[i],index);
        }
    }

    /**
     * Control visibility of companies in the ORPanel.
     * @param showAll True if all active companies must be shown,
     *                false if only the currently operating company is shown.
     */
    private void setCompanyVisibility(boolean showAll) {
        for (int i = 0; i < nc; i++) {
            boolean visible = rowVisibilityObservers[i].lastValue() && (showAll || (i == orCompIndex));
            setRowVisibility(i + leftCompNameYOffset, visible);
        }
        selectRevenueSpinner(!showAll);
    }

    public int getCompanyTreasuryBonusRevenue(int orCompIndex) {
        return (Integer) directIncomeSelect[orCompIndex].getValue();
    }

    public void setTreasuryBonusRevenue(int orCompIndex2, int bonusAmount) {
        directIncomeRevenue[orCompIndex2].setText(orUIManager.getGameUIManager().format(bonusAmount));

    }

    public void dispose() {
        for ( JFrame frame : openWindows ) {
            frame.dispose();
        }
        openWindows.clear();
    }
}
