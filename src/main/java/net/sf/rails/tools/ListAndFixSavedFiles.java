package net.sf.rails.tools;

import java.awt.*;
import java.awt.event.*;
import java.io.BufferedReader;
import java.io.File;
import java.io.FileReader;
import java.io.IOException;
import java.util.*;
import java.util.List;

import javax.swing.*;

import net.sf.rails.game.*;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import net.sf.rails.common.Config;
import net.sf.rails.common.ConfigManager;
import net.sf.rails.common.LocalText;
import net.sf.rails.ui.swing.elements.ActionMenuItem;
import net.sf.rails.util.GameLoader;
import net.sf.rails.util.GameSaver;
import net.sf.rails.util.Util;
import rails.game.action.*;
import rails.game.specific._18EU.StartCompany_18EU;


public class ListAndFixSavedFiles extends JFrame implements ActionListener, KeyListener {
    private static final long serialVersionUID = 1L;

    private JTextArea reportText;
    private JScrollPane messageScroller;
    private JScrollBar vbar;
    private JPanel messagePanel;
    private ListAndFixSavedFiles messageWindow;
    private JMenuBar menuBar;
    private JMenu fileMenu, editMenu, taskMenu;
    private JMenuItem saveItem, loadItem, closeItem, exitItem;
    private JMenuItem trimItem, deleteItem, editItem, copyItem, pasteItem;
    private JMenuItem changeBuyTrainFromFile;

    private int editedIndex;
    private PossibleAction editedAction;

    private StringBuffer headerText = new StringBuffer();

    private GameLoader gameLoader;

    private int vbarPos;

    private String saveDirectory;
    private String filepath;
    private RailsRoot root;

    /**
     *  An action to be copied to another file
     */
    private PossibleAction copiedAction;
    private Class<? extends PossibleAction> oldActionClass;

    private static Logger log;

    public static void main(String[] args) {
        // initialize configuration
        ConfigManager.initConfiguration(false);

        // delayed setting of logger (see also ConfigManager)
        log = LoggerFactory.getLogger(ListAndFixSavedFiles.class);

        String saveDirectory = Config.get("save.directory");
        log.warn("Save directory = {}", saveDirectory);

        new ListAndFixSavedFiles (saveDirectory);
    }

    public ListAndFixSavedFiles (String saveDirectory) {
        super();

        this.saveDirectory = saveDirectory;
        messageWindow = this;

        reportText = new JTextArea();
        reportText.setEditable(false);
        reportText.setLineWrap(false);
        reportText.setBackground(Color.WHITE);
        reportText.setOpaque(true);
        reportText.setBorder(BorderFactory.createEmptyBorder(5, 5, 5, 5));

        messagePanel = new JPanel(new GridBagLayout());
        messageScroller =
            new JScrollPane(reportText,
                    ScrollPaneConstants.VERTICAL_SCROLLBAR_ALWAYS,
                    ScrollPaneConstants.HORIZONTAL_SCROLLBAR_AS_NEEDED);
        vbar = messageScroller.getVerticalScrollBar();
        GridBagConstraints gbc = new GridBagConstraints();
        gbc.gridx = gbc.gridy = 0;
        gbc.weightx = gbc.weighty = 1.0;
        gbc.fill = GridBagConstraints.BOTH;
        messagePanel.add(messageScroller, gbc);

        menuBar = new JMenuBar();
        fileMenu = new JMenu(LocalText.getText("FILE"));
        fileMenu.setMnemonic(KeyEvent.VK_F);
        editMenu = new JMenu(LocalText.getText("EDIT"));
        editMenu.setMnemonic(KeyEvent.VK_E);
        taskMenu = new JMenu(LocalText.getText("TASK"));

        loadItem = new ActionMenuItem(LocalText.getText("LOAD"));
        loadItem.setActionCommand("LOAD");
        loadItem.setMnemonic(KeyEvent.VK_L);
        loadItem.setAccelerator(KeyStroke.getKeyStroke(KeyEvent.VK_L,
                ActionEvent.ALT_MASK));
        loadItem.addActionListener(this);
        loadItem.setEnabled(true);
        fileMenu.add(loadItem);

        saveItem = new ActionMenuItem(LocalText.getText("SAVE"));
        saveItem.setActionCommand("SAVE");
        saveItem.setMnemonic(KeyEvent.VK_S);
        saveItem.setAccelerator(KeyStroke.getKeyStroke(KeyEvent.VK_S,
                ActionEvent.ALT_MASK));
        saveItem.addActionListener(this);
        saveItem.setEnabled(true);
        fileMenu.add(saveItem);

        closeItem = new ActionMenuItem(LocalText.getText("CLOSE"));
        closeItem.setActionCommand("CLOSE");
        closeItem.setMnemonic(KeyEvent.VK_C);
        closeItem.setAccelerator(KeyStroke.getKeyStroke(KeyEvent.VK_C,
                ActionEvent.ALT_MASK));
        closeItem.addActionListener(this);
        closeItem.setEnabled(true);
        fileMenu.add(closeItem);

        exitItem = new ActionMenuItem(LocalText.getText("EXIT"));
        exitItem.setActionCommand("EXIT");
        exitItem.setMnemonic(KeyEvent.VK_X);
        exitItem.setAccelerator(KeyStroke.getKeyStroke(KeyEvent.VK_X,
                ActionEvent.ALT_MASK));
        exitItem.addActionListener(this);
        exitItem.setEnabled(true);
        fileMenu.add(exitItem);

        trimItem = new ActionMenuItem("Trim");
        trimItem.setActionCommand("TRIM");
        trimItem.setMnemonic(KeyEvent.VK_T);
        trimItem.setAccelerator(KeyStroke.getKeyStroke(KeyEvent.VK_T,
                ActionEvent.ALT_MASK));
        trimItem.addActionListener(this);
        trimItem.setEnabled(true);
        editMenu.add(trimItem);

        deleteItem = new ActionMenuItem("Delete");
        deleteItem.setActionCommand("DELETE");
        deleteItem.setMnemonic(KeyEvent.VK_D);
        deleteItem.setAccelerator(KeyStroke.getKeyStroke(KeyEvent.VK_D,
                ActionEvent.ALT_MASK));
        deleteItem.addActionListener(this);
        deleteItem.setEnabled(true);
        editMenu.add(deleteItem);

        editItem = new ActionMenuItem("Edit");
        editItem.setActionCommand("EDIT");
        editItem.setMnemonic(KeyEvent.VK_E);
        editItem.setAccelerator(KeyStroke.getKeyStroke(KeyEvent.VK_E,
                ActionEvent.ALT_MASK));
        editItem.addActionListener(this);
        editItem.setEnabled(true);
        editMenu.add(editItem);

        copyItem = new ActionMenuItem("Copy");
        copyItem.setActionCommand("COPY");
        copyItem.setMnemonic(KeyEvent.VK_C);
        copyItem.setAccelerator(KeyStroke.getKeyStroke(KeyEvent.VK_C,
                ActionEvent.ALT_MASK));
        copyItem.addActionListener(this);
        copyItem.setEnabled(true);
        editMenu.add(copyItem);

        pasteItem = new ActionMenuItem("Paste");
        pasteItem.setActionCommand("PASTE");
        pasteItem.setMnemonic(KeyEvent.VK_P);
        pasteItem.setAccelerator(KeyStroke.getKeyStroke(KeyEvent.VK_P,
                ActionEvent.ALT_MASK));
        pasteItem.addActionListener(this);
        pasteItem.setEnabled(true);
        editMenu.add(pasteItem);

        changeBuyTrainFromFile = new ActionMenuItem("UpdateBuyTrainFromFile");
        changeBuyTrainFromFile.setActionCommand("UPDATE_BUYTRAIN");
        changeBuyTrainFromFile.setMnemonic(KeyEvent.VK_U);
        changeBuyTrainFromFile.setAccelerator(KeyStroke.getKeyStroke(KeyEvent.VK_U,
                ActionEvent.ALT_MASK));
        changeBuyTrainFromFile.addActionListener(this);
        changeBuyTrainFromFile.setEnabled(true);
        taskMenu.add(changeBuyTrainFromFile);

        menuBar.add(fileMenu);
        menuBar.add(editMenu);
        menuBar.add(taskMenu);

        setJMenuBar(menuBar);

        setContentPane(messagePanel);

        setSize(400, 400);
        setLocation(600, 400);
        setTitle("List and fix saved files");

        addKeyListener(this);

        setVisible(true);

        saveDirectory = Config.get("save.directory");

//        load();
    }

    private void load() {
        JFileChooser jfc = new JFileChooser();
        jfc.setCurrentDirectory(new File(saveDirectory));

        if (jfc.showOpenDialog(getContentPane()) == JFileChooser.APPROVE_OPTION) {
            File selectedFile = jfc.getSelectedFile();
            filepath = selectedFile.getPath();
            saveDirectory = selectedFile.getParent();

            // clear header text
            headerText = new StringBuffer();

            // use GameLoader object to load game
            gameLoader = new GameLoader();
            try{
                gameLoader.createFromFile(selectedFile);
                add(gameLoader.getGameDataAsText());
                //gameLoader.getRoot().start();
                setReportText(true);
            } catch (Exception e)  {
                log.error("exception", e);
            }
            root = gameLoader.getRoot();
            setTitle(selectedFile.getName());
        }


    }

    public void add (String text) {
        if (text.length() > 0) {
            headerText.append(text);
            headerText.append("\n");
        }
    }

    private void setReportText(boolean initial) {
        if (initial)
            vbarPos = -1;
        else
            vbarPos = this.vbar.getValue();

        reportText.setText(headerText.toString());
        // append actionText
        int i=0;
        List<PossibleAction> actions = gameLoader.getActions();
        if (actions != null) {
            log.info("Actions={}", actions.size());
            for (PossibleAction action : actions) {
                reportText.append("Action " + i + " " + action.getPlayerName() + "(" + action.getPlayerIndex() + "): " + action.toString());
                reportText.append("\n");
                i++;
            }
        }
        scrollDown(vbarPos);
    }


    public void scrollDown (int pos) {
        SwingUtilities.invokeLater(() -> {
            if (vbarPos == -1)
                messageWindow.vbar.setValue(messageWindow.vbar.getMaximum());
            else
                messageWindow.vbar.setValue(vbarPos);
        });
    }

    @Override
    public void actionPerformed(ActionEvent actor) {
        String command = actor.getActionCommand();
        if ("TRIM".equalsIgnoreCase(command)) {
            String result = JOptionPane.showInputDialog("Enter last action number to be retained");
            if (Util.hasValue(result)) {
                try {
                    int index = Integer.parseInt(result);
                    // delete actions
                    int size = gameLoader.getActions().size();
                    gameLoader.getActions().subList(index + 1, size).clear();
                    setReportText(false);
                } catch (NumberFormatException e) {

                }
            }
        } else if ("DELETE".equalsIgnoreCase(command)) {
            String result = JOptionPane.showInputDialog("Enter action number to be deleted");
            if (Util.hasValue(result)) {
                try {
                    int index = Integer.parseInt(result);
                    gameLoader.getActions().remove(index);
                    setReportText(false);
                } catch (NumberFormatException e) {
                    log.error("Number format exception for '{}'", result, e);
                }
            }
        } else if ("EDIT".equalsIgnoreCase(command)) {
            String result = JOptionPane.showInputDialog("Enter action number to be corrected");
            if (Util.hasValue(result)) {
                try {
                    int index = Integer.parseInt(result);
                    edit(index);
                } catch (NumberFormatException e) {
                    log.error("Number format exception for '{}'", result, e);
                }
            }
        } else if ("COPY".equalsIgnoreCase(command)) {
            String result = JOptionPane.showInputDialog("Enter action number to be copied");
            if (Util.hasValue(result)) {
                try {
                    int index = Integer.parseInt(result);
                    copy (index);
                } catch (NumberFormatException e) {
                    log.error("Number format exception for '{}'", result, e);
                }
            }
        } else if ("PASTE".equalsIgnoreCase(command)) {
            String result = JOptionPane.showInputDialog("Enter action number after which to paste");
            if (Util.hasValue(result)) {
                try {
                    int index = Integer.parseInt(result);
                    paste (index);
                } catch (NumberFormatException e) {
                    log.error("Number format exception for '{}'", result, e);
                }
            }
        } else if ("UPDATE_BUYTRAIN".equalsIgnoreCase(command)) {
            updateBuyTrainFromFile();
        } else if ("SAVE".equalsIgnoreCase(command)) {
            save();
        } else if ("LOAD".equalsIgnoreCase(command)) {
            load();
        } else if ("CLOSE".equalsIgnoreCase(command)) {
            reportText.setText("");
            gameLoader = null;
            setTitle("List and fix saved files");
        } else if ("EXIT".equalsIgnoreCase(command)) {
            System.exit(0);
        }
    }

    private void save() {
        JFileChooser jfc = new JFileChooser();
        if (Util.hasValue(saveDirectory)) {
            jfc.setCurrentDirectory(new File(saveDirectory));
        }
        if (Util.hasValue(filepath)) {
            jfc.setSelectedFile(new File(filepath));
        }
        if (jfc.showSaveDialog(this) == JFileChooser.APPROVE_OPTION) {
            File selectedFile = jfc.getSelectedFile();
            GameSaver gameSaver = new GameSaver(gameLoader);
            try {
                gameSaver.saveGame(selectedFile);
            } catch (IOException e) {
                String message = LocalText.getText("SaveFailed", e.getMessage());
                log.error(message);
            }
        }
    }

    private void edit(int index) {
        editedAction = gameLoader.getActions().get(index);
        editedIndex = index;
        if (editedAction instanceof BuyTrain) {
            new BuyTrainDialog ((BuyTrain) editedAction);
        } else if (editedAction instanceof LayTile) {
            new LayTileDialog((LayTile) editedAction);
        } else if (editedAction instanceof StartCompany_18EU) {
            new StartCompany18EUDialog((StartCompany_18EU) editedAction);
        } else if (editedAction instanceof BuyCertificate) {
            new BuyCertificateDialog ((BuyCertificate) editedAction);
        } else if (editedAction instanceof BuyBonds) {
            new BuyBondDialog ((BuyBonds) editedAction);
        } else if (editedAction instanceof SellShares) {
            new SellSharesDialog ((SellShares) editedAction);
        } else if (editedAction instanceof SetDividend) {
            new SetDividendDialog((SetDividend) editedAction);
        } else if (editedAction instanceof LayBaseToken) {
            new LayBaseTokenDialog ((LayBaseToken)editedAction);
        } else if (editedAction instanceof LayBonusToken) {
            new LayBonusTokenDialog ((LayBonusToken)editedAction);
        } else if (editedAction instanceof DiscardTrain) {
            new DiscardTrainDialog((DiscardTrain) editedAction);
        } else if (editedAction instanceof NullAction) {
            new NullActionDialog((NullAction) editedAction);
        } else {
            JOptionPane.showMessageDialog(this,
                    "Action type '" + editedAction.getClass().getSimpleName()
                    + "' cannot yet be edited");
        }
    }

    private void copy (int index) {
        copiedAction = gameLoader.getActions().get(index);
    }

    private void paste (int index) {
        gameLoader.getActions().add(index+1, copiedAction);
        setReportText(false);
    }

    protected void processCorrections (PossibleAction newAction) {
        if (newAction != null /*&& (!newAction.equalsAsAction(editedAction)*/) {
            gameLoader.getActions().set(editedIndex, newAction);
            setReportText(false);
        }
    }

    private abstract class EditDialog extends JDialog implements ActionListener {

        private static final long serialVersionUID = 1L;
        private List<Object> originalValues = new ArrayList<>();
        protected List<JComponent> inputElements = new ArrayList<>();
        private GridBagConstraints gc = new GridBagConstraints();
        private int length = 0;
        private JButton okButton, cancelButton;

        EditDialog (String title) {
            super((Frame) null, title, false); // Non-modal
            getContentPane().setLayout(new GridBagLayout());
            setDefaultCloseOperation(JFrame.DISPOSE_ON_CLOSE);
        }

        void finish() {
            addField (this, okButton = new JButton("OK"), ++length, 0);
            okButton.addActionListener(this);
            addField (this, cancelButton = new JButton("Cancel"), length, 1);
            cancelButton.addActionListener(this);
            pack();

            // Center on window
            int x = (int) messageWindow.getLocationOnScreen().getX()
            + (messageWindow.getWidth() - getWidth()) / 2;
            int y = (int) messageWindow.getLocationOnScreen().getY()
            + (messageWindow.getHeight() - getHeight()) / 2;
            setLocation(x, y);

            setVisible(true);
            setAlwaysOnTop(true);
        }

        @Override
        public void actionPerformed(ActionEvent arg0) {
            if (arg0.getSource().equals(okButton)) {
                PossibleAction newAction = processInput();
                if (newAction != null) messageWindow.processCorrections(newAction);
            } else if (arg0.getSource().equals(cancelButton)) {

            }
            this.setVisible(false);
            this.dispose();

        }

        abstract PossibleAction processInput();
    }

    private class BuyTrainDialog extends EditDialog {
        private static final long serialVersionUID = 1L;
        private BuyTrain action;

        BuyTrainDialog (BuyTrain action) {
            super ("Edit BuyTrain");
            this.action = action;
            addLabel (this, "From", null, action.getFromOwner().getId());  // 1
            //addLabel (this, "Train UID", null, action.getTrain().getId());  // 0
            addTextField (this, "Train UID",
                    action.getTrain().getId(),
                    String.valueOf(action.getTrain().getId()));  // 1
            addTextField (this, "Price paid",
                    action.getPricePaid(),
                    String.valueOf(action.getPricePaid()));  // 2
            addTextField (this, "Pres.cash to add",
                    action.getPresidentCashToAdd(),
                    String.valueOf(action.getPresidentCashToAdd()));  // 3
            addTextField (this, "Added cash",
                    action.getAddedCash(),
                    String.valueOf(action.getAddedCash()));  // 4
            addTextField (this, "Loans to take",
                    action.getLoansToTake(),
                    String.valueOf(action.getLoansToTake()));  // 5
            addTextField (this, "Trains for exchange",
                    action.getTrainsForExchange(),
                    action.getTrainsForExchange() != null ? action.getTrainsForExchange().toString() : "[]");  // 6
            addTextField (this, "Exchange train UID",
                    action.getExchangedTrain(),
                    action.getExchangedTrain() != null ? action.getExchangedTrain().getId() : "");  // 7
            addTextField (this, "Fixed Price",
                    action.getFixedCost(),
                    String.valueOf(action.getFixedCost()));  // 8
            addTextField (this, "Mode",
                    action.getFixedCostMode(),
                    action.getFixedCostMode().toString()); // 9
            finish();
        }

        @Override
        PossibleAction processInput() {
            log.debug("Action was {}", action);
            String trainID = ((JTextField)inputElements.get(1)).getText();
            Train train = root.getTrainManager().getTrainByUniqueId(trainID);
            if (train != null) action.setTrain(train);
            try {
                int pricePaid = Integer.parseInt(((JTextField)inputElements.get(2)).getText());
                action.setPricePaid(pricePaid);
            } catch (NumberFormatException e) {
            }
            try {
                int presCash = Integer.parseInt(((JTextField)inputElements.get(3)).getText());
                action.setPresidentCashToAdd(presCash);
            } catch (NumberFormatException e) {
            }
            try {
                int addedCash = Integer.parseInt(((JTextField)inputElements.get(4)).getText());
                action.setAddedCash(addedCash);
            } catch (NumberFormatException e) {
            }
            try {
                int loansToTake = Integer.parseInt(((JTextField)inputElements.get(5)).getText());
                action.setLoansToTake(loansToTake);
            } catch (NumberFormatException e) {
            }
            String trainsForExchangeInput = ((JTextField)inputElements.get(6)).getText();
            String trainsForExchangeIds = trainsForExchangeInput
                    .replaceAll(".*\\[(.*)\\].*", "$1");
            if (Util.hasValue(trainsForExchangeIds)) {
                Set<Train> trainsForExchange = new HashSet<>();
                for (String trainId : trainsForExchangeIds.split(",")) {
                    trainsForExchange.add(root.getTrainManager().getTrainByUniqueId(trainId));
                }
                 action.setTrainsForExchange(trainsForExchange);
            } else {
                action.setTrainsForExchange(null);
            }

            String exchangedTrainID = ((JTextField) inputElements.get(7)).getText();
            if (Util.hasValue(exchangedTrainID)) {
                Train exchangedTrain = root.getTrainManager().getTrainByUniqueId(exchangedTrainID);
                if (exchangedTrain != null) action.setExchangedTrain(exchangedTrain);
            } else {
                action.setExchangedTrain(null);
            }

            try {
                int fixedCost = Integer.parseInt(((JTextField)inputElements.get(8)).getText());
                action.setFixedCost(fixedCost);
            } catch (NumberFormatException e) {
            }

            String modeName = ((JTextField) inputElements.get(9)).getText();
            if (modeName.length() == 0) {
                action.setFixedCostMode(null);
            } else {
                try {
                    BuyTrain.Mode mode = BuyTrain.Mode.valueOf(modeName);
                    action.setFixedCostMode(mode);
                } catch (Exception e) {

                }
            }

            log.debug("Action is  {}", action);
            return action;

        }
    }

    private class LayTileDialog extends EditDialog {
        private static final long serialVersionUID = 1L;
        private LayTile action;

        LayTileDialog (LayTile action) {
            super ("Edit LayTile");
            this.action = action;
            addTextField (this, "Tile laid",
                    action.getLaidTile(),
                    action.getLaidTile().toText());  // 0
            addTextField (this, "Hex laid",
                    action.getChosenHex(),
                    action.getChosenHex().getId());  // 1
            addTextField (this, "Orientation",
                    action.getOrientation(),
                    String.valueOf(action.getOrientation()));  // 2
            addTextField (this, "Type",
                    action.getType(),
                    String.valueOf(action.getType()));  // 3
            List<Tile> tiles = action.getTiles();
            if (tiles == null) tiles = new ArrayList<>();
            String tileString = "";
            for (Tile tile : tiles) {
                if (tileString.length() > 0) tileString += ",";
                tileString += tile.getId();
            }
            addTextField (this, "Tiles",
                    tiles,
                    tileString); // 4
            addTextField (this, "Locations",
                    action.getLocations(),
                    action.getLocationNames()); // 5
            finish();
        }

        @Override
        PossibleAction processInput() {
            log.info("Action was {}, elements={}", action, inputElements);
            try {
                String tileID = ((JTextField)inputElements.get(0)).getText();
                Tile tile = root.getTileManager().getTile(tileID);
                if (tile != null) action.setLaidTile(tile);
            } catch (NumberFormatException e) {
            }
            String hexID = ((JTextField)inputElements.get(1)).getText();
            MapHex hex = root.getMapManager().getHex(hexID);
            if (hexID != null && hex != null) action.setChosenHex(hex);
            try {
                int orientation = Integer.parseInt(((JTextField)inputElements.get(2)).getText());
                action.setOrientation(orientation);
            } catch (NumberFormatException e) {
            }
            try {
                int type = Integer.parseInt(((JTextField)inputElements.get(3)).getText());
                action.setType(type);
            } catch (NumberFormatException e) {
            }
            String tileNames = ((JTextField)inputElements.get(4)).getText();
            List<Tile> tiles = new ArrayList<>();
            for (String tileName : tileNames.split(",")) {
                Tile tile = root.getTileManager().getTile(tileName);
                tiles.add (tile);
            }
            action.setTiles(tiles);

            String locationNames = ((JTextField)inputElements.get(5)).getText();
            action.setLocationsByName(List.of(locationNames.split(",")));

            log.info("Action is {}", action);
            return action;

        }
    }

    private class BuyBondDialog extends EditDialog {
        private static final long serialVersionUID = 1L;
        private BuyBonds action;

        BuyBondDialog(BuyBonds action) {
            super("Edit BuyCertificate");
            this.action = action;
            addTextField (this, "Bond value",
                    action.getPrice(),
                    String.valueOf(action.getPrice()));
            addTextField (this, "Maximum",
                    action.getMaxNumber(),
                    String.valueOf(action.getMaxNumber()));
            addTextField (this, "Bought",
                    action.getNumberBought(),
                    String.valueOf(action.getNumberBought()));

            finish();
        }

        @Override
        PossibleAction processInput() {
            log.info("Action was {}", action);
            String input = "";
            try {
                input = ((JTextField)inputElements.get(0)).getText();
                int bondValue = Integer.valueOf(input);
                action.setPrice(bondValue);
            } catch (NumberFormatException e) {
                log.error ("Error in price: {}", input, e);
            }
            try {
                input = ((JTextField)inputElements.get(1)).getText();
                int maxNumber = Integer.valueOf(input);
                action.setMaxNumber(maxNumber);
            } catch (NumberFormatException e) {
                log.error ("Error in max: {}", input, e);
            }
            input = ((JTextField)inputElements.get(2)).getText();
            try{
                int bought = Integer.valueOf(input);
                action.setNumberBought(bought);
            } catch (NumberFormatException e) {
                log.error ("Error in bought: {}", input, e);
            }
            log.info("Action is {}", action);
            return action;
        }
    }

    private class BuyCertificateDialog extends EditDialog {
        private static final long serialVersionUID = 1L;
        private BuyCertificate action;

        BuyCertificateDialog(BuyCertificate action) {
            super("Edit BuyCertificate");
            this.action = action;
            addTextField(this, "President",
                    action.isPresident(),
                    String.valueOf(action.isPresident()));
            addTextField(this, "Share size",
                    action.getSharePerCertificate(),
                    String.valueOf(action.getSharePerCertificate()));
            addTextField (this, "Buy price",
                    action.getPrice(),
                    String.valueOf(action.getPrice()));
            addTextField (this, "From (e.g. Bank_Pool)",
                    action.getFromPortfolio(),
                    action.getFromPortfolio() != null
                            ? action.getFromPortfolio().getUniqueName()
                            : "null");
            // NOTE: enter pool as "Bank_Pool"
            finish();
        }

        @Override
        PossibleAction processInput() {
            log.info("Action was {}", action);
            String input = "";
            try {
                input = ((JTextField)inputElements.get(0)).getText();
                boolean president = Boolean.valueOf(input);
                action.setPresident(president);
            } catch (NumberFormatException e) {
                log.error ("Error in president: {}", input, e);
            }
            try {
                input = ((JTextField)inputElements.get(1)).getText();
                int shareSize = Integer.valueOf(input);
                action.setSharePerCert(shareSize);
            } catch (NumberFormatException e) {
                log.error ("Error in share size: {}", input, e);
            }
            try {
                input = ((JTextField)inputElements.get(2)).getText();
                int price = Integer.valueOf(input);
                action.setPrice(price);
            } catch (NumberFormatException e) {
                log.error ("Error in price: {}", input, e);
            }
            input = ((JTextField)inputElements.get(3)).getText();
            action.setFromByName(input);
            if (action.getFromPortfolio() == null) {
                log.error ("Error in from: {}", input);
            }
            log.info("Action is {}", action);
            return action;

        }
    }

    private class StartCompany18EUDialog extends EditDialog {
        private static final long serialVersionUID = 1L;
        private StartCompany_18EU action;

        StartCompany18EUDialog(StartCompany_18EU action) {
            super("Edit StartCompany");
            this.action = action;
            addTextField(this, "President",
                    action.isPresident(),
                    String.valueOf(action.isPresident()));
            addTextField(this, "Share size",
                    action.getSharePerCertificate(),
                    String.valueOf(action.getSharePerCertificate()));
            addTextField (this, "Buy price",
                    action.getPrice(),
                    String.valueOf(action.getPrice()));
            addTextField (this, "Home station",
                    action.getSelectedHomeStation(),
                    action.getSelectedHomeStation().getStationComposedId());
            finish();
        }

        @Override
        PossibleAction processInput() {
            log.info("Action was {}", action);
            String input = "";
            try {
                input = ((JTextField)inputElements.get(0)).getText();
                boolean president = Boolean.valueOf(input);
                action.setPresident(president);
            } catch (NumberFormatException e) {
                log.error ("Error in president: {}", input, e);
            }
            try {
                input = ((JTextField)inputElements.get(1)).getText();
                int shareSize = Integer.valueOf(input);
                action.setSharePerCert(shareSize);
            } catch (NumberFormatException e) {
                log.error ("Error in share size: {}", input, e);
            }
            try {
                input = ((JTextField)inputElements.get(2)).getText();
                int price = Integer.valueOf(input);
                action.setPrice(price);
            } catch (NumberFormatException e) {
                log.error ("Error in price: {}", input, e);
            }
            input = ((JTextField)inputElements.get(3)).getText();
            action.clearHomeStation();
            action.setHomeStationName(input);
            log.info("Action is {}", action);
            return action;

        }
    }

    private class SellSharesDialog extends EditDialog {
        private static final long serialVersionUID = 1L;
        private SellShares action;

        SellSharesDialog(SellShares action) {
            super("Edit SellShares");
            this.action = action;
            addTextField(this, "Company",
                    action.getCompanyName(),
                    action.getCompanyName());
            addTextField(this, "Share size",
                    action.getShareUnits(),
                    String.valueOf(action.getShareUnits()));
            addTextField (this, "Share qty",
                    action.getNumber(),
                    String.valueOf(action.getNumber()));
            addTextField (this, "Share",
                    action.getShare(),
                    String.valueOf(action.getShare()));
            addTextField (this, "Price",
                    action.getPrice(),
                    String.valueOf(action.getPrice()));
            addTextField (this, "Pres exch",
                    action.getPresidentExchange(),
                    String.valueOf(action.getPresidentExchange()));
            finish();
        }

        @Override
        PossibleAction processInput() {
            log.info("Action was {}", action);
            String input = "";
            String companyName = ((JTextField)inputElements.get(0)).getText();
            action.setCompanyName(companyName);
            try {
                input = ((JTextField)inputElements.get(1)).getText();
                int shareSize = Integer.valueOf(input);
                action.setShareUnits(shareSize);
            } catch (NumberFormatException e) {
                log.error ("Error in share size: {}", input, e);
            }
            try {
                input = ((JTextField)inputElements.get(2)).getText();
                int number = Integer.valueOf(input);
                action.setNumber(number);
            } catch (NumberFormatException e) {
                log.error ("Error in number: {}", input, e);
            }
            try {
                input = ((JTextField)inputElements.get(3)).getText();
                int share = Integer.valueOf(input);
                action.setShare(share);
            } catch (NumberFormatException e) {
                log.error ("Error in number: {}", input, e);
            }
            try {
                input = ((JTextField)inputElements.get(4)).getText();
                int price = Integer.valueOf(input);
                action.setPrice(price);
            } catch (NumberFormatException e) {
                log.error ("Error in number: {}", input, e);
            }
            try {
                input = ((JTextField)inputElements.get(5)).getText();
                int presExchange = Integer.valueOf(input);
                action.setPresidentExchange(presExchange);
            } catch (NumberFormatException e) {
                log.error ("Error in number: {}", input, e);
            }
            log.info("Action is {}", action);
            return action;

        }
    }

    private class SetDividendDialog extends EditDialog {
        private static final long serialVersionUID = 1L;
        private SetDividend action;

        SetDividendDialog(SetDividend action) {
            super("Edit SetDividend");
            this.action = action;
            addTextField(this, "Preset revenue",
                    action.getPresetRevenue(),
                    String.valueOf(action.getPresetRevenue()));  // 0
            addTextField(this, "Actual revenue",
                    action.getActualRevenue(),
                    String.valueOf(action.getActualRevenue()));
            finish();
        }

        @Override
        PossibleAction processInput() {
            log.info("Action was {}", action);
            String input = "";
            try {
                input = ((JTextField)inputElements.get(0)).getText();
                int presetRevenue = Integer.valueOf(input);
                action.setPresetRevenue(presetRevenue);
            } catch (NumberFormatException e) {
                log.error ("Error in presetRevenue: {}", input, e);
            }
            try {
                input = ((JTextField)inputElements.get(1)).getText();
                int actualRevenue = Integer.valueOf(input);
                action.setActualRevenue(actualRevenue);
            } catch (NumberFormatException e) {
                log.error ("Error in actualRevenue: {}", input, e);
            }

            log.info("Action is {}", action);
            return action;

        }
    }

    private class LayBaseTokenDialog extends EditDialog {
        private static final long serialVersionUID = 1L;
        private LayBaseToken action;

        LayBaseTokenDialog(LayBaseToken action) {
            super("Edit LayBaseToken");
            this.action = action;
            addTextField(this, "Locations",  // 0
                    action.getLocationNameString(),
                    action.getLocationNameString());
            addTextField(this, "Station",
                    // getChosenStation is deprecated, but chosenStop is still derived from it,
                    // and therefore must be used in ListAndFixSavedFiles
                    action.getChosenStation(),
                    String.valueOf(action.getChosenStation()));  // 1
            addTextField(this, "cost",
                    action.getCost(),
                    String.valueOf(action.getCost()));  // 2
            finish();
        }

        @Override
        PossibleAction processInput() {
            log.info("Action was {}", action);
            String input = "";
            input = ((JTextField)inputElements.get(0)).getText();

            action.setLocationNames(input);
            try {
                input = ((JTextField)inputElements.get(1)).getText();
                int chosenStation = Integer.valueOf(input);
                action.setChosenStation(chosenStation);
            } catch (NumberFormatException e) {
                log.error ("Error in chosenStop: {}", input, e);
            }
            try {
                input = ((JTextField)inputElements.get(2)).getText();
                int cost = Integer.valueOf(input);
                action.setCost(cost);
            } catch (NumberFormatException e) {
                log.error ("Error in chosenStop: {}", input, e);
            }

            log.info("Action is {}", action);
            return action;

        }
    }

    private class LayBonusTokenDialog extends EditDialog {
        private static final long serialVersionUID = 1L;
        private LayBonusToken action;

        LayBonusTokenDialog(LayBonusToken action) {
            super("Edit LayBonusToken");
            this.action = action;
            addTextField(this, "Locations",
                    action.getLocationNameString(),
                    action.getLocationNameString());
            finish();
        }

        @Override
        PossibleAction processInput() {
            log.info("Action was {}", action);
            String input = "";

            input = ((JTextField)inputElements.get(0)).getText();
            action.setLocationNames(input);

            log.info("Action is {}", action);
            return action;

        }
    }

    private class DiscardTrainDialog extends EditDialog {
        private static final long serialVersionUID = 1L;
        private DiscardTrain action;

        DiscardTrainDialog (DiscardTrain action) {
            super ("Edit DiscardTrain");
            this.action = action;
            addLabel (this, "Company", null, action.getCompany().getId()); // 0
            addTextField (this, "Trains",
                    action.getOwnedTrains(),
                    String.valueOf(action.getOwnedTrains()));  // 1
            addTextField (this, "Forced",
                    action.isForced(),
                    String.valueOf(action.isForced()));  // 2
            addTextField (this, "Discarded",
                    action.getDiscardedTrain(),
                    String.valueOf(action.getDiscardedTrain()));  // 3
            finish();
        }

        @Override
        PossibleAction processInput() {

            String ownedTrainsInput = ((JTextField)inputElements.get(1)).getText();
            String ownedTrainsIds = ownedTrainsInput
                    .replaceAll("\\[(.*)\\]", "$1").replaceAll("\\s+", "");
            Set<Train> ownedTrains = new HashSet<>();
            for (String trainId : ownedTrainsIds.split(",")) {
                ownedTrains.add(root.getTrainManager().getTrainByUniqueId(trainId));
            }
            action.setOwnedTrains(ownedTrains);

            action.setForced(Boolean.valueOf(((JTextField)inputElements.get(2)).getText()));

            String trainID = ((JTextField)inputElements.get(3)).getText();
            Train train = root.getTrainManager().getTrainByUniqueId(trainID);
            if (train != null) action.setDiscardedTrain(train);

            log.info("Action is  {}", action);
            return action;

        }
    }

    private class NullActionDialog extends EditDialog {
        private static final long serialVersionUID = 1L;
        private NullAction action;

        NullActionDialog (NullAction action) {
            super ("Edit NullAction");
            this.action = action;
            //addLabel (this, "Company", null, action.getCompany().getId()); // 0
            addTextField (this, "Mode",
                    action.getMode(),
                    String.valueOf(action.getMode()));  // 0
            finish();
        }

        @Override
        PossibleAction processInput() {

            String modeName = ((JTextField)inputElements.get(0)).getText();
            NullAction.Mode mode = NullAction.Mode.valueOf(modeName);

            action.setMode(mode);

            log.info("Action is  {}", action);
            return action;
        }
    }


    protected void addLabel (EditDialog owner, String caption, Object initialObject, String initialValue) {
        JComponent element = new JLabel (initialValue);
        int index = owner.length++;
        addField (owner, new JLabel (caption), index, 0);
        addField (owner, element, index, 1);
        owner.originalValues.add(initialObject);
        owner.inputElements.add(element);
    }

    protected void addTextField (EditDialog owner, String caption, Object initialObject, String initialValue) {
        JComponent element = new JTextField (initialValue);
        int index = owner.length++;
        addField (owner, new JLabel (caption), index, 0);
        addField (owner, element, index, 1);
        owner.originalValues.add(initialObject);
        owner.inputElements.add(element);
    }

    protected void addField(EditDialog owner, JComponent comp, int y, int x) {
        GridBagConstraints gbc = owner.gc;
        gbc.gridx = x;
        gbc.gridy = y;
        gbc.gridwidth = 1;
        gbc.gridheight = 1;
        gbc.weightx = gbc.weighty = 0.5;
        gbc.fill = GridBagConstraints.BOTH;

        owner.getContentPane().add(comp, gbc);

        comp.setVisible(true);
    }

    void updateBuyTrainFromFile() {
        /*
         *  The correction file must have the same name as that .rails file,
         * but with extension .def
         * Each line must contain 3 fields, comma-separated, no spaces:
         * - action number
         * - original train UID (e.g. 2_7)
         * - new train UID
         * Lines starting with # are ignored.         *
         */

        String defFilePath = filepath.replace(".rails", ".def");
        File defFile = new File (defFilePath);
        BufferedReader in;
        TrainManager tm = root.getTrainManager();
        try {
            in = new BufferedReader(new FileReader(defFilePath));
            String line;
            while ((line = in.readLine()) != null) {
                System.out.println(line);
                if (line.startsWith("#")) continue;
                String[] fields = line.split(",");
                // Find action number
                int index = Integer.parseInt(fields[0]);
                BuyTrain action = (BuyTrain) gameLoader.getActions().get(index);
                String trainId = action.getTrain().getId();
                String from = fields[1];
                String to = fields[2];
                Train newTrain = tm.getTrainByUniqueId(to);
                PublicCompany newOwner = (PublicCompany) newTrain.getCard().getOwner();
                System.out.println("Action " + index + " of " + action.getOwner() + " has " + trainId + ": from "
                        + from + " to " + to + " > card " + newTrain.getCard().getId() + " owner " + newOwner);
                action.setTrain(newTrain);
                //newOwner.buyTrain(newTrain, 0);
            }
            in.close();
        } catch (IOException e) {
            System.out.println("Error while reading "+filepath+":"+e.toString());
        }


    }

    @Override
    public void keyPressed(KeyEvent e) { }

    @Override
    public void keyReleased(KeyEvent e) {}

    @Override
    public void keyTyped(KeyEvent e) {}


}

