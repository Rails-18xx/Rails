package rails.ui.swing;

import java.awt.Color;
import java.awt.GridBagConstraints;
import java.awt.GridBagLayout;
import java.awt.Insets;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import javax.swing.BorderFactory;
import javax.swing.ButtonGroup;
import javax.swing.JComponent;
import javax.swing.JOptionPane;
import javax.swing.JPanel;
import javax.swing.UIManager;

import org.apache.log4j.Logger;

import rails.common.Defs;
import rails.game.Bank;
import rails.game.Player;
import rails.game.Portfolio;
import rails.game.PublicCertificateI;
import rails.game.PublicCompanyI;
import rails.game.action.BuyCertificate;
import rails.game.action.NullAction;
import rails.game.action.PossibleAction;
import rails.game.action.PossibleActions;
import rails.game.action.SellShares;
import rails.game.action.StartCompany;
import rails.ui.swing.elements.Caption;
import rails.ui.swing.elements.ClickField;
import rails.ui.swing.elements.Field;
import rails.ui.swing.elements.RadioButtonDialog;
import rails.util.LocalText;

/**
 * This class is incorporated into StatusWindow and displays the bulk of
 * rails.game status information.
 */
public class GameStatus extends JPanel implements ActionListener {
    private static final long serialVersionUID = 1L;

    private static final int NARROW_GAP = 1;
    private static final int WIDE_GAP = 3;
    private static final int WIDE_LEFT = 1;
    private static final int WIDE_RIGHT = 2;
    private static final int WIDE_TOP = 4;
    private static final int WIDE_BOTTOM = 8;

    private static final String BUY_FROM_IPO_CMD = "BuyFromIPO";
    private static final String BUY_FROM_POOL_CMD = "BuyFromPool";
    private static final String SELL_CMD = "Sell";

    private static GameStatus gameStatus;
    protected StatusWindow parent;

    private GridBagConstraints gbc;
    private Color buttonHighlight = new Color(255, 160, 80);

    // Grid elements per function
    protected Field certPerPlayer[][];
    protected ClickField certPerPlayerButton[][];
    protected int certPerPlayerXOffset, certPerPlayerYOffset;
    protected Field certInIPO[];
    protected ClickField certInIPOButton[];
    protected int certInIPOXOffset, certInIPOYOffset;
    protected Field certInPool[];
    protected ClickField certInPoolButton[];
    protected int certInPoolXOffset, certInPoolYOffset;
    protected Field certInTreasury[];
    protected ClickField certInTreasuryButton[];
    protected int certInTreasuryXOffset, certInTreasuryYOffset;
    protected Field parPrice[];
    protected int parPriceXOffset, parPriceYOffset;
    protected Field currPrice[];
    protected int currPriceXOffset, currPriceYOffset;
    protected Field compCash[];
    protected int compCashXOffset, compCashYOffset;
    protected Field compRevenue[];
    protected int compRevenueXOffset, compRevenueYOffset;
    protected Field compTrains[];
    protected int compTrainsXOffset, compTrainsYOffset;
    protected Field compTokens[];
    protected int compTokensXOffset, compTokensYOffset;
    protected Field compPrivates[];
    protected int compPrivatesXOffset, compPrivatesYOffset;
    protected Field playerCash[];
    protected int playerCashXOffset, playerCashYOffset;
    protected Field playerPrivates[];
    protected int playerPrivatesXOffset, playerPrivatesYOffset;
    protected Field playerWorth[];
    protected int playerWorthXOffset, playerWorthYOffset;
    protected Field playerCertCount[];
    protected int playerCertCountXOffset, playerCertCountYOffset;
    protected int certLimitXOffset, certLimitYOffset;
    protected Field bankCash;
    protected int bankCashXOffset, bankCashYOffset;
    protected Field poolTrains;
    protected int poolTrainsXOffset, poolTrainsYOffset;
    protected Field newTrains;
    protected int newTrainsXOffset, newTrainsYOffset;
    protected Field futureTrains;
    protected int futureTrainsXOffset, futureTrainsYOffset, futureTrainsWidth;
    protected int rightCompCaptionXOffset;

    private Caption[] upperPlayerCaption;
    private Caption[] lowerPlayerCaption;
    private Caption treasurySharesCaption;

    protected int np; // Number of players
    protected GridBagLayout gb;

    protected int nc; // Number of companies
    protected Player[] players;
    protected PublicCompanyI[] companies;
    //protected CompanyManagerI cm;
    protected Portfolio ipo, pool;
    
    protected GameUIManager gameUIManager;

    protected PossibleActions possibleActions = PossibleActions.getInstance();

    protected boolean hasParPrices = false;
    protected boolean compCanBuyPrivates = false;
    protected boolean compCanHoldOwnShares = false;
    protected boolean compCanHoldForeignShares = false; // NOT YET USED

    private PublicCompanyI c;
    private JComponent f;

    // Current actor.
    // Players: 0, 1, 2, ...
    // Company (from treasury): -1.
    protected int actorIndex = -2;

    protected ButtonGroup buySellGroup = new ButtonGroup();
    protected ClickField dummyButton; // To be selected if none else is.

    protected Map<PublicCompanyI, Integer> companyIndex =
            new HashMap<PublicCompanyI, Integer>();
    protected Map<Player, Integer> playerIndex = new HashMap<Player, Integer>();

    protected static Logger log =
            Logger.getLogger(GameStatus.class.getPackage().getName());

    public GameStatus() {
        super();
    }

    public void init(StatusWindow parent, GameUIManager gameUIManager) {

        gameStatus = this;
        this.parent = parent;
        this.gameUIManager = gameUIManager;

        gb = new GridBagLayout();
        this.setLayout(gb);
        UIManager.put("ToggleButton.select", buttonHighlight);

        gbc = new GridBagConstraints();
        setSize(800, 300);
        setLocation(0, 450);
        setBorder(BorderFactory.createEtchedBorder());
        setOpaque(false);

        players = gameUIManager.getPlayers().toArray(new Player[0]);
        np = gameUIManager.getNumberOfPlayers();
        companies = gameUIManager.getAllPublicCompanies().toArray(new PublicCompanyI[0]);
        nc = companies.length;

        hasParPrices = gameUIManager.getCommonParameterAsBoolean(Defs.Parm.HAS_ANY_PAR_PRICE);
        compCanBuyPrivates = gameUIManager.getCommonParameterAsBoolean(Defs.Parm.CAN_ANY_COMPANY_BUY_PRIVATES);
        compCanHoldOwnShares = gameUIManager.getCommonParameterAsBoolean(Defs.Parm.CAN_ANY_COMPANY_HOLD_OWN_SHARES);

        ipo = Bank.getIpo();
        pool = Bank.getPool();

        certPerPlayer = new Field[nc][np];
        certPerPlayerButton = new ClickField[nc][np];
        certInIPO = new Field[nc];
        certInIPOButton = new ClickField[nc];
        certInPool = new Field[nc];
        certInPoolButton = new ClickField[nc];
        if (compCanHoldOwnShares) {
            certInTreasury = new Field[nc];
            certInTreasuryButton = new ClickField[nc];
        }
        parPrice = new Field[nc];
        currPrice = new Field[nc];
        compCash = new Field[nc];
        compRevenue = new Field[nc];
        compTrains = new Field[nc];
        compTokens = new Field[nc];
        compPrivates = new Field[nc];
        playerCash = new Field[np];
        playerPrivates = new Field[np];
        playerWorth = new Field[np];
        playerCertCount = new Field[np];
        upperPlayerCaption = new Caption[np];
        lowerPlayerCaption = new Caption[np];

        int lastX = 0;
        int lastY = 1;
        certPerPlayerXOffset = ++lastX;
        certPerPlayerYOffset = ++lastY;
        certInIPOXOffset = (lastX += np);
        certInIPOYOffset = lastY;
        certInPoolXOffset = ++lastX;
        certInPoolYOffset = lastY;
        if (compCanHoldOwnShares) {
            certInTreasuryXOffset = ++lastX;
            certInTreasuryYOffset = lastY;
        }
        if (hasParPrices) {
            parPriceXOffset = ++lastX;
            parPriceYOffset = lastY;
        }
        currPriceXOffset = ++lastX;
        currPriceYOffset = lastY;
        compCashXOffset = ++lastX;
        compCashYOffset = lastY;
        compRevenueXOffset = ++lastX;
        compRevenueYOffset = lastY;
        compTrainsXOffset = ++lastX;
        compTrainsYOffset = lastY;
        compTokensXOffset = ++lastX;
        compTokensYOffset = lastY;
        if (compCanBuyPrivates) {
            compPrivatesXOffset = ++lastX;
            compPrivatesYOffset = lastY;
        }
        rightCompCaptionXOffset = ++lastX;

        playerCashXOffset = certPerPlayerXOffset;
        playerCashYOffset = (lastY += nc);
        playerPrivatesXOffset = certPerPlayerXOffset;
        playerPrivatesYOffset = ++lastY;
        playerWorthXOffset = certPerPlayerXOffset;
        playerWorthYOffset = ++lastY;
        playerCertCountXOffset = certPerPlayerXOffset;
        playerCertCountYOffset = ++lastY;
        certLimitXOffset = certInPoolXOffset;
        certLimitYOffset = playerCertCountYOffset;
        bankCashXOffset = certInPoolXOffset;
        bankCashYOffset = playerPrivatesYOffset;
        poolTrainsXOffset = bankCashXOffset + 2;
        poolTrainsYOffset = playerPrivatesYOffset;
        newTrainsXOffset = poolTrainsXOffset + 1;
        newTrainsYOffset = playerPrivatesYOffset;
        futureTrainsXOffset = newTrainsXOffset + 1;
        futureTrainsYOffset = playerPrivatesYOffset;
        futureTrainsWidth = rightCompCaptionXOffset - futureTrainsXOffset;

        addField(new Caption(LocalText.getText("COMPANY")), 0, 0, 1, 2,
                WIDE_RIGHT + WIDE_BOTTOM);
        addField(new Caption(LocalText.getText("PLAYERS")),
                certPerPlayerXOffset, 0, np, 1, 0);
        for (int i = 0; i < np; i++) {
            playerIndex.put(players[i], new Integer(i));
            f = upperPlayerCaption[i] = new Caption(players[i].getNameAndPriority());
            addField(f, certPerPlayerXOffset + i, 1, 1, 1, WIDE_BOTTOM);
        }
        addField(new Caption(LocalText.getText("BANK_SHARES")),
                certInIPOXOffset, 0, 2, 1, WIDE_LEFT + WIDE_RIGHT);
        addField(new Caption(LocalText.getText("IPO")), certInIPOXOffset, 1, 1,
                1, WIDE_LEFT + WIDE_BOTTOM);
        addField(new Caption(LocalText.getText("POOL")), certInPoolXOffset, 1,
                1, 1, WIDE_RIGHT + WIDE_BOTTOM);

        if (compCanHoldOwnShares) {
            addField(treasurySharesCaption =
                    new Caption(LocalText.getText("TREASURY_SHARES")),
                    certInTreasuryXOffset, 0, 1, 2, WIDE_RIGHT + WIDE_BOTTOM);
        }

        if (this.hasParPrices) {
            addField(new Caption(LocalText.getText("PRICES")), parPriceXOffset,
                    0, 2, 1, WIDE_RIGHT);
            addField(new Caption(LocalText.getText("PAR")), parPriceXOffset, 1,
                    1, 1, WIDE_BOTTOM);
            addField(new Caption(LocalText.getText("CURRENT")),
                    currPriceXOffset, 1, 1, 1, WIDE_RIGHT + WIDE_BOTTOM);
        } else {
            addField(new Caption(LocalText.getText("CURRENT_PRICE")),
                    currPriceXOffset, 0, 1, 2, WIDE_RIGHT + WIDE_BOTTOM);

        }
        addField(new Caption(LocalText.getText("COMPANY_DETAILS")),
                compCashXOffset, 0, this.compCanBuyPrivates ? 5 : 4, 1, 0);
        addField(new Caption(LocalText.getText("CASH")), compCashXOffset, 1, 1,
                1, WIDE_BOTTOM);
        addField(new Caption(LocalText.getText("REVENUE")), compRevenueXOffset,
                1, 1, 1, WIDE_BOTTOM);
        addField(new Caption(LocalText.getText("TRAINS")), compTrainsXOffset,
                1, 1, 1, WIDE_BOTTOM);
        addField(new Caption(LocalText.getText("TOKENS")), compTokensXOffset,
                1, 1, 1, WIDE_BOTTOM);
        if (compCanBuyPrivates) {
            addField(new Caption(LocalText.getText("PRIVATES")),
                    compPrivatesXOffset, 1, 1, 1, WIDE_BOTTOM);
        }
        addField(new Caption(LocalText.getText("COMPANY")),
                rightCompCaptionXOffset, 0, 1, 2, WIDE_LEFT + WIDE_BOTTOM);

        for (int i = 0; i < nc; i++) {
            c = companies[i];
            companyIndex.put(c, new Integer(i));
            f = new Caption(c.getName());
            f.setForeground(c.getFgColour());
            f.setBackground(c.getBgColour());
            addField(f, 0, certPerPlayerYOffset + i, 1, 1, WIDE_RIGHT);

            for (int j = 0; j < np; j++) {
                f =
                        certPerPlayer[i][j] =
                                new Field(
                                        players[j].getPortfolio().getShareModel(
                                                c));
                addField(f, certPerPlayerXOffset + j, certPerPlayerYOffset + i,
                        1, 1, 0);
                f =
                        certPerPlayerButton[i][j] =
                                new ClickField("", SELL_CMD,
                                        LocalText.getText("ClickForSell"),
                                        this, buySellGroup);
                addField(f, certPerPlayerXOffset + j, certPerPlayerYOffset + i,
                        1, 1, 0);
            }
            f = certInIPO[i] = new Field(Bank.getIpo().getShareModel(c));
            addField(f, certInIPOXOffset, certInIPOYOffset + i, 1, 1, WIDE_LEFT);
            f =
                    certInIPOButton[i] =
                            new ClickField(
                                    certInIPO[i].getText(),
                                    BUY_FROM_IPO_CMD,
                                    LocalText.getText("ClickToSelectForBuying"),
                                    this, buySellGroup);
            f.setVisible(false);
            addField(f, certInIPOXOffset, certInIPOYOffset + i, 1, 1, WIDE_LEFT);
            certInIPO[i].setPreferredSize(certInIPOButton[i].getPreferredSize());

            f = certInPool[i] = new Field(Bank.getPool().getShareModel(c));
            addField(f, certInPoolXOffset, certInPoolYOffset + i, 1, 1,
                    WIDE_RIGHT);
            f =
                    certInPoolButton[i] =
                            new ClickField(
                                    certInPool[i].getText(),
                                    BUY_FROM_POOL_CMD,
                                    LocalText.getText("ClickToSelectForBuying"),
                                    this, buySellGroup);
            f.setVisible(false);
            addField(f, certInPoolXOffset, certInPoolYOffset + i, 1, 1,
                    WIDE_RIGHT);
            certInPool[i].setPreferredSize(certInIPOButton[i].getPreferredSize());/* sic */

            if (compCanHoldOwnShares) {
                f =
                        certInTreasury[i] =
                                new Field(c.getPortfolio().getShareModel(c));
                addField(f, certInTreasuryXOffset, certInTreasuryYOffset + i,
                        1, 1, WIDE_RIGHT);
                f =
                        certInTreasuryButton[i] =
                                new ClickField(
                                        certInTreasury[i].getText(),
                                        BUY_FROM_POOL_CMD,
                                        LocalText.getText("ClickToSelectForBuying"),
                                        this, buySellGroup);
                f.setVisible(false);
                addField(f, certInTreasuryXOffset, certInTreasuryYOffset + i,
                        1, 1, WIDE_RIGHT);
                certInTreasury[i].setPreferredSize(certInTreasuryButton[i].getPreferredSize());/* sic */
            }

            if (this.hasParPrices) {
                f = parPrice[i] = new Field(c.getParPriceModel());
                addField(f, parPriceXOffset, parPriceYOffset + i, 1, 1, 0);
            }

            f = currPrice[i] = new Field(c.getCurrentPriceModel());
            addField(f, currPriceXOffset, currPriceYOffset + i, 1, 1,
                    WIDE_RIGHT);

            f = compCash[i] = new Field(c.getCashModel());
            addField(f, compCashXOffset, compCashYOffset + i, 1, 1, 0);

            f = compRevenue[i] = new Field(c.getLastRevenueModel());
            addField(f, compRevenueXOffset, compRevenueYOffset + i, 1, 1, 0);

            f = compTrains[i] = new Field(c.getPortfolio().getTrainsModel());
            addField(f, compTrainsXOffset, compTrainsYOffset + i, 1, 1, 0);

            f = compTokens[i] = new Field(c.getBaseTokensModel());
            addField(f, compTokensXOffset, compTokensYOffset + i, 1, 1, 0);

            if (this.compCanBuyPrivates) {
                f =
                        compPrivates[i] =
                                new Field(
                                        c.getPortfolio().getPrivatesOwnedModel());
                addField(f, compPrivatesXOffset, compPrivatesYOffset + i, 1, 1,
                        0);
            }

            f = new Caption(c.getName());
            f.setForeground(c.getFgColour());
            f.setBackground(c.getBgColour());
            addField(f, rightCompCaptionXOffset, certPerPlayerYOffset + i, 1,
                    1, WIDE_LEFT);
        }

        // Player possessions
        addField(new Caption(LocalText.getText("CASH")), 0, playerCashYOffset,
                1, 1, WIDE_TOP + WIDE_RIGHT);
        for (int i = 0; i < np; i++) {
            f = playerCash[i] = new Field(players[i].getCashModel());
            addField(f, playerCashXOffset + i, playerCashYOffset, 1, 1,
                    WIDE_TOP);
        }

        addField(new Caption("Privates"), 0, playerPrivatesYOffset, 1, 1,
                WIDE_RIGHT);
        for (int i = 0; i < np; i++) {
            f =
                    playerPrivates[i] =
                            new Field(
                                    players[i].getPortfolio().getPrivatesOwnedModel());
            addField(f, playerPrivatesXOffset + i, playerPrivatesYOffset, 1, 1,
                    0);
        }

        addField(new Caption(LocalText.getText("WORTH")), 0,
                playerWorthYOffset, 1, 1, WIDE_RIGHT);
        for (int i = 0; i < np; i++) {
            f = playerWorth[i] = new Field(players[i].getWorthModel()/*
                                                                         * ,
                                                                         * true
                                                                         */);
            addField(f, playerWorthXOffset + i, playerWorthYOffset, 1, 1, 0);
        }

        addField(new Caption("Certs"), 0, playerCertCountYOffset, 1, 1,
                WIDE_RIGHT + WIDE_TOP);
        for (int i = 0; i < np; i++) {
            f =
                    playerCertCount[i] =
                            new Field(players[i].getCertCountModel(), true);
            addField(f, playerCertCountXOffset + i, playerCertCountYOffset, 1,
                    1, WIDE_TOP);
        }

        for (int i = 0; i < np; i++) {
            f = lowerPlayerCaption[i] = new Caption(players[i].getName());
            addField(f, i + 1, playerCertCountYOffset + 1, 1, 1, WIDE_TOP);
        }

        // Certificate Limit
        addField(new Caption(LocalText.getText("LIMIT")), certLimitXOffset - 1,
                certLimitYOffset, 1, 1, WIDE_TOP + WIDE_LEFT);
        addField(new Field("" + Player.getCertLimit()), certLimitXOffset,
                certLimitYOffset, 1, 1, WIDE_TOP);

        // Bank
        addField(new Caption(LocalText.getText("Bank")), bankCashXOffset - 1,
                bankCashYOffset - 1, 1, 2, WIDE_TOP + WIDE_LEFT);
        addField(new Caption(LocalText.getText("CASH")), bankCashXOffset,
                bankCashYOffset - 1, 1, 1, WIDE_TOP);
        bankCash = new Field(Bank.getInstance().getCashModel());
        addField(bankCash, bankCashXOffset, bankCashYOffset, 1, 1, 0);

        // Trains
        addField(new Caption(LocalText.getText("TRAINS")),
                poolTrainsXOffset - 1, poolTrainsYOffset - 1, 1, 2, WIDE_TOP
                                                                    + WIDE_LEFT);
        addField(new Caption(LocalText.getText("USED")), poolTrainsXOffset,
                poolTrainsYOffset - 1, 1, 1, WIDE_TOP);
        poolTrains = new Field(Bank.getPool().getTrainsModel());
        addField(poolTrains, poolTrainsXOffset, poolTrainsYOffset, 1, 1, 0);

        // New trains
        addField(new Caption(LocalText.getText("NEW")), newTrainsXOffset,
                newTrainsYOffset - 1, 1, 1, WIDE_TOP);
        newTrains = new Field(Bank.getIpo().getTrainsModel());
        addField(newTrains, newTrainsXOffset, newTrainsYOffset, 1, 1, 0);

        dummyButton = new ClickField("", "", "", this, buySellGroup);

        // Future trains
        addField(new Caption(LocalText.getText("Future")), futureTrainsXOffset,
                futureTrainsYOffset - 1, futureTrainsWidth, 1, WIDE_TOP);
        futureTrains = new Field(Bank.getUnavailable().getTrainsModel());
        addField(futureTrains, futureTrainsXOffset, futureTrainsYOffset,
                futureTrainsWidth, 1, 0);

        dummyButton = new ClickField("", "", "", this, buySellGroup);

    }

    private void addField(JComponent comp, int x, int y, int width, int height,
            int wideGapPositions) {

        int padTop, padLeft, padBottom, padRight;
        gbc.gridx = x;
        gbc.gridy = y;
        gbc.gridwidth = width;
        gbc.gridheight = height;
        gbc.weightx = gbc.weighty = 0.5;
        gbc.fill = GridBagConstraints.BOTH;
        padTop = (wideGapPositions & WIDE_TOP) > 0 ? WIDE_GAP : NARROW_GAP;
        padLeft = (wideGapPositions & WIDE_LEFT) > 0 ? WIDE_GAP : NARROW_GAP;
        padBottom =
                (wideGapPositions & WIDE_BOTTOM) > 0 ? WIDE_GAP : NARROW_GAP;
        padRight = (wideGapPositions & WIDE_RIGHT) > 0 ? WIDE_GAP : NARROW_GAP;
        gbc.insets = new Insets(padTop, padLeft, padBottom, padRight);

        add(comp, gbc);

    }

    public static GameStatus getInstance() {
        return gameStatus;
    }

    @Override
    public void repaint() {
        super.repaint();
    }

    public void actionPerformed(ActionEvent actor) {
        JComponent source = (JComponent) actor.getSource();
        List<PossibleAction> actions;
        PossibleAction chosenAction = null;

        if (source instanceof ClickField) {
            gbc = gb.getConstraints(source);
            actions = ((ClickField) source).getPossibleActions();

            // Assume that we will have either sell or buy actions
            // under one ClickField, not both. This seems guaranteed.
            log.debug("Action is " + actions.get(0).toString());

            if (actions == null || actions.size() == 0) {

                log.warn("No ClickField action found");

            } else if (actions.get(0) instanceof SellShares) {

                List<String> options = new ArrayList<String>();
                List<SellShares> sellActions = new ArrayList<SellShares>();
                List<Integer> sellAmounts = new ArrayList<Integer>();
                SellShares sale;
                for (PossibleAction action : actions) {
                    sale = (SellShares) action;

                    for (int i = 1; i <= sale.getMaximumNumber(); i++) {
                        options.add(LocalText.getText("SellShares",
                                new String[] {
                                        String.valueOf(i * sale.getShare()),
                                        sale.getCompanyName(),
                                        Bank.format(i * sale.getShareUnits()
                                                    * sale.getPrice()) }));
                        sellActions.add(sale);
                        sellAmounts.add(i);
                    }
                }
                int index = 0;
                if (options.size() > 1) {
                    String message = LocalText.getText("PleaseSelect");
                    String sp =
                            (String) JOptionPane.showInputDialog(this, message,
                                    message, JOptionPane.QUESTION_MESSAGE,
                                    null, options.toArray(new String[0]),
                                    options.get(0));
                    index = options.indexOf(sp);
                } else if (options.size() == 1) {
                    String message = LocalText.getText("PleaseConfirm");
                    int result =
                            JOptionPane.showConfirmDialog(this, options.get(0),
                                    message, JOptionPane.OK_CANCEL_OPTION,
                                    JOptionPane.QUESTION_MESSAGE);
                    index = (result == JOptionPane.OK_OPTION ? 0 : -1);
                }
                if (index < 0) {
                    // cancelled
                } else {
                    chosenAction = sellActions.get(index);
                    ((SellShares) chosenAction).setNumberSold(sellAmounts.get(index));
                }
            } else if (actions.get(0) instanceof BuyCertificate) {
                boolean startCompany = false;

                List<String> options = new ArrayList<String>();
                List<BuyCertificate> buyActions =
                        new ArrayList<BuyCertificate>();
                List<Integer> buyAmounts = new ArrayList<Integer>();
                BuyCertificate buy;
                PublicCertificateI cert;
                String companyName = "";
                String playerName = "";

                for (PossibleAction action : actions) {
                    buy = (BuyCertificate) action;
                    cert = buy.getCertificate();
                    playerName = buy.getPlayerName ();
                    PublicCompanyI company = cert.getCompany();
                    companyName = company.getName();

                    if (buy instanceof StartCompany) {

                        startCompany = true;
                        int[] startPrices;
                        if (((StartCompany) buy).mustSelectAPrice()) {
                            startPrices =
                                    ((StartCompany) buy).getStartPrices();
                            Arrays.sort(startPrices);
                            for (int i = 0; i < startPrices.length; i++) {
                                options.add(LocalText.getText("StartCompany",
                                        new String[] {
                                                Bank.format(startPrices[i]),
                                                String.valueOf(cert.getShare()),
                                                Bank.format(cert.getShares()
                                                            * startPrices[i]) }));
                                buyActions.add(buy);
                                buyAmounts.add(startPrices[i]);
                            }
                        } else {
                            startPrices = new int[] {((StartCompany) buy).getPrice()};
                            options.add(LocalText.getText("StartCompanyFixed", new String[]{
                                    companyName,
                                    String.valueOf(cert.getShare()),
                                    Bank.format(startPrices[0])}));
                            buyActions.add(buy);
                            buyAmounts.add(startPrices[0]);
                        }
 
                    } else {

                        options.add(LocalText.getText("BuyCertificate",
                                new String[] {
                                        String.valueOf(cert.getShare()),
                                        cert.getCompany().getName(),
                                        cert.getPortfolio().getName(),
                                        Bank.format(cert.getShares()
                                                    * buy.getPrice()) }));
                        buyActions.add(buy);
                        buyAmounts.add(1);
                        for (int i = 2; i <= buy.getMaximumNumber(); i++) {
                            options.add(LocalText.getText("BuyCertificates",
                                    new String[] {
                                            String.valueOf(i),
                                            String.valueOf(cert.getShare()),
                                            cert.getCompany().getName(),
                                            cert.getPortfolio().getName(),
                                            Bank.format(i * cert.getShares()
                                                        * buy.getPrice()) }));
                            buyActions.add(buy);
                            buyAmounts.add(i);
                        }
                    }
                }
                int index = 0;
                if (options.size() > 1) {
                    if (startCompany) {
                        index =
                                new RadioButtonDialog(this,
                                        LocalText.getText("PleaseSelect"),
                                        LocalText.getText("WHICH_START_PRICE", new String[] {
                                                playerName,
                                                companyName}),
                                        options.toArray(new String[0]), -1).getSelectedOption();
                    } else {
                        String sp =
                                (String) JOptionPane.showInputDialog(this,
                                        LocalText.getText(startCompany
                                                ? "WHICH_PRICE"
                                                : "HOW_MANY_SHARES"),
                                        LocalText.getText("PleaseSelect"),
                                        JOptionPane.QUESTION_MESSAGE, null,
                                        options.toArray(new String[0]),
                                        options.get(0));
                        index = options.indexOf(sp);
                    }
                } else if (options.size() == 1) {
                    int result =
                            JOptionPane.showConfirmDialog(this, options.get(0),
                                    LocalText.getText("PleaseConfirm"),
                                    JOptionPane.OK_CANCEL_OPTION,
                                    JOptionPane.QUESTION_MESSAGE);
                    index = (result == JOptionPane.OK_OPTION ? 0 : -1);
                }
                if (index < 0) {
                    // cancelled
                } else if (startCompany) {
                    chosenAction = buyActions.get(index);
                    ((StartCompany) chosenAction).setStartPrice(buyAmounts.get(index));
                    ((StartCompany) chosenAction).setNumberBought(((StartCompany) chosenAction).getCertificate().getShares());
                } else {
                    chosenAction = buyActions.get(index);
                    ((BuyCertificate) chosenAction).setNumberBought(buyAmounts.get(index));
                }
            } else {

                chosenAction =
                        processGameSpecificActions(actor, actions.get(0));

            }
        } else {
            log.warn("Action from unknown source: " + source.toString());
        }

        chosenAction = processGameSpecificFollowUpActions(actor, chosenAction);

        if (chosenAction != null)
            ((StatusWindow) parent).process(chosenAction);

        repaint();

    }

    /** Stub allowing game-specific extensions */
    protected PossibleAction processGameSpecificActions(ActionEvent actor,
            PossibleAction chosenAction) {
        return chosenAction;
    }

    protected PossibleAction processGameSpecificFollowUpActions(
            ActionEvent actor, PossibleAction chosenAction) {
        return chosenAction;
    }

    public void initTurn(int actorIndex) {
        int i, j;

        dummyButton.setSelected(true);

        // Reset previous highlights
        if ((j = this.actorIndex) >= 0) {
            upperPlayerCaption[j].setHighlight(false);
            lowerPlayerCaption[j].setHighlight(false);
            for (i = 0; i < nc; i++) {
                setPlayerCertButton(i, j, false);
            }
        } else if (j == -1 && compCanHoldOwnShares) {
            treasurySharesCaption.setHighlight(false);
        }
        for (i = 0; i < nc; i++) {
            setIPOCertButton(i, false);
            setPoolCertButton(i, false);
            if (compCanHoldOwnShares) setTreasuryCertButton(i, false);
        }

        this.actorIndex = actorIndex;

        // Set new highlights
        if ((j = this.actorIndex) >= -1) {
            if (j >= 0) {
                upperPlayerCaption[j].setHighlight(true);
                lowerPlayerCaption[j].setHighlight(true);
            } else if (j == -1 && treasurySharesCaption != null) {
                treasurySharesCaption.setHighlight(true);
            }

            PublicCertificateI cert;
            Portfolio holder;
            int index;

            List<BuyCertificate> buyableCerts =
                    possibleActions.getType(BuyCertificate.class);
            if (buyableCerts != null) {
                for (BuyCertificate bCert : buyableCerts) {
                    cert = bCert.getCertificate();
                    index = cert.getCompany().getPublicNumber();
                    holder = bCert.getFromPortfolio();
                    if (holder == ipo) {
                        setIPOCertButton(index, true, bCert);
                    } else if (holder == pool) {
                        setPoolCertButton(index, true, bCert);
                    } else if (compCanHoldOwnShares) {
                        setTreasuryCertButton(index, true, bCert);
                    }
                }
            }

            PublicCompanyI company;
            List<SellShares> sellableShares =
                    possibleActions.getType(SellShares.class);
            if (sellableShares != null) {
                for (SellShares share : sellableShares) {
                    company = share.getCompany();
                    index = company.getPublicNumber();
                    if (j >= 0) {
                        setPlayerCertButton(index, j, true, share);
                    } else if (j == -1) {
                        setTreasuryCertButton(index, true, share);
                    }
                }
            }

            initGameSpecificActions();

            List<NullAction> nullActions =
                    possibleActions.getType(NullAction.class);
            if (nullActions != null) {
                for (NullAction na : nullActions) {
                    ((StatusWindow) parent).setPassButton(na);
                }
            }

        }

        repaint();
    }

    /** Stub, can be overridden by game-specific subclasses */
    protected void initGameSpecificActions() {

    }

    public void setPriorityPlayer(int index) {

        for (int j = 0; j < np; j++) {
            upperPlayerCaption[j].setText(players[j].getName()
                                          + (j == index ? " PD" : ""));
        }
    }

    public String getSRPlayer() {
        if (actorIndex >= 0)
            return players[actorIndex].getName();
        else
            return "";
    }

    protected void setPlayerCertButton(int i, int j, boolean clickable, Object o) {

        setPlayerCertButton(i, j, clickable);
        if (clickable && o != null) {
            if (o instanceof PossibleAction)
                certPerPlayerButton[i][j].addPossibleAction((PossibleAction) o);
        }
    }

    protected void setPlayerCertButton(int i, int j, boolean clickable) {
        if (clickable) {
            certPerPlayerButton[i][j].setText(certPerPlayer[i][j].getText());
        } else {
            certPerPlayerButton[i][j].clearPossibleActions();
        }
        certPerPlayer[i][j].setVisible(!clickable);
        certPerPlayerButton[i][j].setVisible(clickable);
    }

    private void setIPOCertButton(int i, boolean clickable, Object o) {

        setIPOCertButton(i, clickable);
        if (clickable && o != null) {
            if (o instanceof PossibleAction)
                certInIPOButton[i].addPossibleAction((PossibleAction) o);
        }
    }

    private void setIPOCertButton(int i, boolean clickable) {
        if (clickable) {
            certInIPOButton[i].setText(certInIPO[i].getText());
        } else {
            certInIPOButton[i].clearPossibleActions();
        }
        certInIPO[i].setVisible(!clickable);
        certInIPOButton[i].setVisible(clickable);
    }

    private void setPoolCertButton(int i, boolean clickable, Object o) {

        setPoolCertButton(i, clickable);
        if (clickable && o != null) {
            if (o instanceof PossibleAction)
                certInPoolButton[i].addPossibleAction((PossibleAction) o);
        }
    }

    private void setPoolCertButton(int i, boolean clickable) {
        if (clickable) {
            certInPoolButton[i].setText(certInPool[i].getText());
        } else {
            certInPoolButton[i].clearPossibleActions();
        }
        certInPool[i].setVisible(!clickable);
        certInPoolButton[i].setVisible(clickable);
    }

    private void setTreasuryCertButton(int i, boolean clickable, Object o) {

        setTreasuryCertButton(i, clickable);
        if (clickable && o != null) {
            if (o instanceof PossibleAction)
                certInTreasuryButton[i].addPossibleAction((PossibleAction) o);
        }
    }

    private void setTreasuryCertButton(int i, boolean clickable) {
        if (clickable) {
            certInTreasuryButton[i].setText(certInTreasury[i].getText());
        } else {
            certInTreasuryButton[i].clearPossibleActions();
        }
        certInTreasury[i].setVisible(!clickable);
        certInTreasuryButton[i].setVisible(clickable);
    }

}
