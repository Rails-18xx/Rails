package tools;

import java.awt.Color;
import java.awt.Frame;
import java.awt.GridBagConstraints;
import java.awt.GridBagLayout;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.awt.event.KeyEvent;
import java.awt.event.KeyListener;
import java.io.File;
import java.util.ArrayList;
import java.util.List;
import java.util.SortedMap;
import java.util.TreeMap;

import javax.swing.BorderFactory;
import javax.swing.JButton;
import javax.swing.JComponent;
import javax.swing.JDialog;
import javax.swing.JFileChooser;
import javax.swing.JFrame;
import javax.swing.JLabel;
import javax.swing.JMenu;
import javax.swing.JMenuBar;
import javax.swing.JMenuItem;
import javax.swing.JOptionPane;
import javax.swing.JPanel;
import javax.swing.JScrollBar;
import javax.swing.JScrollPane;
import javax.swing.JTextArea;
import javax.swing.JTextField;
import javax.swing.KeyStroke;
import javax.swing.ScrollPaneConstants;
import javax.swing.SwingUtilities;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import rails.common.Config;
import rails.common.DisplayBuffer;
import rails.common.LocalText;
import rails.common.ConfigManager;
import rails.common.parser.ConfigurationException;
import rails.game.GameManager;
import rails.game.MapHex;
import rails.game.Tile;
import rails.game.Train;
import rails.game.action.*;
import rails.ui.swing.elements.ActionMenuItem;
import rails.util.GameFileIO;
import rails.util.Util;

public class ListAndFixSavedFiles extends JFrame
implements ActionListener, KeyListener {

    private static final long serialVersionUID = 1L;
    private JTextArea reportText;
    private JScrollPane messageScroller;
    private JScrollBar vbar;
    private JPanel messagePanel;
    private ListAndFixSavedFiles messageWindow;
    private JMenuBar menuBar;
    private JMenu fileMenu, editMenu;
    private JMenuItem saveItem, loadItem;
    private JMenuItem trimItem, deleteItem, correctItem;

    private int correctedIndex;
    private PossibleAction correctedAction;

    private StringBuffer headerText = new StringBuffer();

    private GameFileIO fileIO;

    private int vbarPos;

    private static String saveDirectory;
    private String filepath;
    private GameManager gameManager;

    protected static Logger log;

    /**
     * @param args
     */
    public static void main(String[] args) {

        // intialize configuration
        ConfigManager.initConfiguration(false);

        // delayed setting of logger
        log = LoggerFactory.getLogger(ListAndFixSavedFiles.class);

        saveDirectory = Config.get("save.directory");
        System.out.println("Save directory = " + saveDirectory);

        new ListAndFixSavedFiles ();

    }

    public ListAndFixSavedFiles () {

        super();

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

        correctItem = new ActionMenuItem("Correct");
        correctItem.setActionCommand("CORRECT");
        correctItem.setMnemonic(KeyEvent.VK_C);
        correctItem.setAccelerator(KeyStroke.getKeyStroke(KeyEvent.VK_C,
                ActionEvent.ALT_MASK));
        correctItem.addActionListener(this);
        correctItem.setEnabled(true);
        editMenu.add(correctItem);

        menuBar.add(fileMenu);
        menuBar.add(editMenu);

        setJMenuBar(menuBar);

        setContentPane(messagePanel);

        setSize(400, 400);
        setLocation(600, 400);
        setTitle("List and fix saved files");

        addKeyListener(this);


        setVisible(true);

        saveDirectory = Config.get("save.directory");

        load();

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
            fileIO = new GameFileIO();

            fileIO.loadGameData(filepath);
            add(fileIO.getGameDataAsText());
            try{
                fileIO.initGame();
                fileIO.loadActionsAndComments();
                setReportText(true);

            } catch (ConfigurationException e)  {
                log.error("Load failed", e);
                DisplayBuffer.add(LocalText.getText("LoadFailed", e.getMessage()));
            }

            gameManager = fileIO.getGame().getGameManager();
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
        for (PossibleAction action : fileIO.getActions()) {
            reportText.append("Action "+i+" "+action.getPlayerName()+": "+action.toString());
            reportText.append("\n");
            // check for comments for this action
            String comment = fileIO.getComments().get(i);
            if (comment!= null) {
                reportText.append("Comment to action " + i + ": " + comment + "\n");
            }
            i++;
        }
        scrollDown(vbarPos);
    }


    public void scrollDown (int pos) {
        SwingUtilities.invokeLater(new Runnable() {
            public void run() {
                if (vbarPos == -1)
                    messageWindow.vbar.setValue(messageWindow.vbar.getMaximum());
                else
                    messageWindow.vbar.setValue(vbarPos);
            }
        });
    }



    public void actionPerformed(ActionEvent actor) {
        String command = actor.getActionCommand();
        if ("TRIM".equalsIgnoreCase(command)) {
            String result = JOptionPane.showInputDialog("Enter last action number to be retained");
            if (Util.hasValue(result)) {
                try {
                    int index = Integer.parseInt(result);
                    // delete actions
                    int size = fileIO.getActions().size();
                    fileIO.getActions().subList(index + 1, size).clear();
                    // delete comments
                    for (int id = 0; id < size; id++) {
                        if (id > index) {
                            fileIO.getComments().remove(id);
                        }
                    }
                    setReportText(false);
                } catch (NumberFormatException e) {

                }
            }
        } else if ("DELETE".equalsIgnoreCase(command)) {
            String result = JOptionPane.showInputDialog("Enter action number to be deleted");
            if (Util.hasValue(result)) {
                try {
                    int index = Integer.parseInt(result);
                    fileIO.getActions().remove(index);
                    // delete and renumber in user Comments
                    SortedMap<Integer, String> newComments = new TreeMap<Integer, String>();
                    for (Integer id:fileIO.getComments().keySet()) {
                        if (id < index) {
                            newComments.put(id, fileIO.getComments().get(id));
                        } else if (id > index) {
                            newComments.put(id-1, fileIO.getComments().get(id));
                        }
                    }
                    fileIO.setComments(newComments);
                    setReportText(false);
                } catch (NumberFormatException e) {
                    log.error("Number format exception for '"+result+"'", e);
                }
            }
        } else if ("CORRECT".equalsIgnoreCase(command)) {
            String result = JOptionPane.showInputDialog("Enter action number to be corrected");
            if (Util.hasValue(result)) {
                try {
                    int index = Integer.parseInt(result);
                    correct (index);
                } catch (NumberFormatException e) {
                    log.error("Number format exception for '"+result+"'", e);
                }
            }
        } else if ("SAVE".equalsIgnoreCase(command)) {
            save();
        } else if ("LOAD".equalsIgnoreCase(command)) {
            load();
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
            fileIO.saveGame(selectedFile, true, "SaveFailed");
        }

    }

    private void correct (int index) {
        correctedAction = fileIO.getActions().get(index);
        correctedIndex = index;
        if (correctedAction instanceof BuyTrain) {
            new BuyTrainDialog ((BuyTrain)correctedAction);
        } else if (correctedAction instanceof LayTile) {
            new LayTileDialog ((LayTile)correctedAction);
        } else {
            JOptionPane.showMessageDialog(this, "Action type '" + correctedAction.getClass().getSimpleName()
                    + "' cannot yet be edited");
        }
    }

    protected void processCorrections (PossibleAction newAction) {
        if (newAction != null && !newAction.equalsAsAction(correctedAction)) {
            fileIO.getActions().set(correctedIndex, newAction);
            setReportText(false);
        }
    }

    private abstract class EditDialog extends JDialog implements ActionListener {

        private static final long serialVersionUID = 1L;
        List<Object> originalValues = new ArrayList<Object>();
        List<JComponent> inputElements = new ArrayList<JComponent>();
        GridBagConstraints gc = new GridBagConstraints();
        int length = 0;
        JButton okButton, cancelButton;

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

        public void actionPerformed(ActionEvent arg0) {

            if (arg0.getSource().equals(okButton)) {
                PossibleAction newAction = processInput();
                if (newAction != null) messageWindow.processCorrections(newAction);
            } else if (arg0.getSource().equals(cancelButton)) {;

            }
            this.setVisible(false);
            this.dispose();

        }

        abstract PossibleAction processInput();
    }

    private class BuyTrainDialog extends EditDialog {

        private static final long serialVersionUID = 1L;
        BuyTrain action;

        BuyTrainDialog (BuyTrain action) {
            super ("Edit BuyTrain");
            this.action = action;
            addLabel (this, "Train UID", null, action.getTrain().getId());  // 0
            addLabel (this, "From Portfolio", null, action.getFromOwner().getId());  // 1
            addTextField (this, "Price paid",
                    new Integer(action.getPricePaid()),
                    String.valueOf(action.getPricePaid()));  // 2
            addTextField (this, "Added cash",
                    new Integer(action.getAddedCash()),
                    String.valueOf(action.getAddedCash()));  // 3
            addTextField (this, "Exchange train UID",
                    action.getExchangedTrain(),
                    action.getExchangedTrain() != null ? action.getExchangedTrain().getId() : "");  // 4
            addTextField (this, "Fixed Price",
                    new Integer(action.getFixedCost()),
                    String.valueOf(action.getFixedCost()));  // 5
            finish();
        }

        @Override
        PossibleAction processInput() {

            log.debug("Action was "+action);
            try {
                int pricePaid = Integer.parseInt(((JTextField)inputElements.get(2)).getText());
                action.setPricePaid(pricePaid);
            } catch (NumberFormatException e) {
            }
            try {
                int addedCash = Integer.parseInt(((JTextField)inputElements.get(3)).getText());
                action.setAddedCash(addedCash);
            } catch (NumberFormatException e) {
            }
            String exchangedTrainID = ((JTextField)inputElements.get(4)).getText();
            Train exchangedTrain = gameManager.getTrainManager().getTrainByUniqueId(exchangedTrainID);
            if (exchangedTrain != null) action.setExchangedTrain(exchangedTrain);
            try {
                int fixedCost = Integer.parseInt(((JTextField)inputElements.get(5)).getText());
                action.setFixedCost(fixedCost);
            } catch (NumberFormatException e) {
            }

            log.debug("Action is  "+action);
            return action;

        }
    }

    private class LayTileDialog extends EditDialog {

        private static final long serialVersionUID = 1L;
        LayTile action;

        LayTileDialog (LayTile action) {
            super ("Edit LayTile");
            this.action = action;
            addTextField (this, "Tile laid",
                    action.getLaidTile(),
                    action.getLaidTile().getId());  // 0
            addTextField (this, "Hex laid",
                    action.getChosenHex(),
                    action.getChosenHex().getName());  // 1
            addTextField (this, "Orientation",
                    new Integer(action.getOrientation()),
                    String.valueOf(action.getOrientation()));  // 2
            finish();
        }

        @Override
        PossibleAction processInput() {

            log.debug("Action was "+action);
            try {
                int tileID = Integer.parseInt(((JTextField)inputElements.get(0)).getText());
                Tile tile = gameManager.getTileManager().getTile(tileID);
                if (tileID > 0 && tile != null) action.setLaidTile(tile);
            } catch (NumberFormatException e) {
            }
            String hexID = ((JTextField)inputElements.get(1)).getText();
            MapHex hex = gameManager.getMapManager().getHex(hexID);
            if (hexID != null && hex != null) action.setChosenHex(hex);
            try {
                int orientation = Integer.parseInt(((JTextField)inputElements.get(2)).getText());
                action.setOrientation(orientation);
            } catch (NumberFormatException e) {
            }

            log.debug("Action is  "+action);
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

    public void keyPressed(KeyEvent e) {
    }

    public void keyReleased(KeyEvent e) {}

    public void keyTyped(KeyEvent e) {}


}

