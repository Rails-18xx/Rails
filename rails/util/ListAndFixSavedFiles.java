package rails.util;

import java.awt.*;
import java.awt.event.*;
import java.io.*;
import java.util.*;
import java.util.List;

import javax.swing.*;

import org.apache.log4j.Logger;

import rails.common.DisplayBuffer;
import rails.common.LocalText;
import rails.common.parser.Config;
import rails.common.parser.ConfigurationException;
import rails.game.*;
import rails.game.action.PossibleAction;
import rails.ui.swing.elements.ActionMenuItem;

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
    private JMenuItem trimItem, deleteItem;

    private StringBuffer headerText = new StringBuffer();

    private GameFileIO fileIO;

    private int vbarPos;

    private static String saveDirectory;
    private String filepath;

    protected static Logger log;

    /**
     * @param args
     */
    public static void main(String[] args) {

        // intialize configuration
        Config.setConfigSelection();

        // delayed setting of logger
        log = Logger.getLogger(ListAndFixSavedFiles.class.getPackage().getName());

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
            
            // use GameLoader object to load game
            fileIO = new GameFileIO();

            fileIO.loadGameData(filepath);
            add(fileIO.getGameDataAsText());
            try{
                fileIO.initGame();
                fileIO.loadActionsAndComments();
                setReportText(true);
                
            } catch (ConfigurationException e)  {
                log.fatal("Load failed", e);
                DisplayBuffer.add(LocalText.getText("LoadFailed", e.getMessage()));
            }
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

    public void keyPressed(KeyEvent e) {
    }

    public void keyReleased(KeyEvent e) {}

    public void keyTyped(KeyEvent e) {}

}
