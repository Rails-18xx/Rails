package rails.util;

import java.awt.*;
import java.awt.event.*;
import java.io.*;
import java.util.*;
import java.util.List;

import javax.swing.*;

import org.apache.log4j.Logger;

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
    private JMenuItem trimItem;

    private List<Object> savedObjects = new ArrayList<Object>(512);
    private List<PossibleAction> executedActions;

    private static String saveDirectory;
    private String filepath;

    protected static Logger log =
        Logger.getLogger(ListAndFixSavedFiles.class.getPackage().getName());

    /**
     * @param args
     */
    public static void main(String[] args) {
    // TODO Auto-generated method stub

        String myConfigFile = System.getProperty("configfile");
        System.out.println("Cmdline configfile setting = " + myConfigFile);

        /* If not, use the default configuration file name */
        if (!Util.hasValue(myConfigFile)) {
            myConfigFile = "my.properties";
        }

        /*
         * Set the system property that tells log4j to use this file. (Note:
         * this MUST be done before updating Config)
         */
        System.setProperty("log4j.configuration", myConfigFile);
        /* Tell the properties loader to read this file. */
        Config.setConfigFile(myConfigFile);
        System.out.println("Configuration file = " + myConfigFile);
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

            log.debug("Loading game from file " + filepath);
            String filename = filepath.replaceAll(".*[/\\\\]", "");

            try {
                ObjectInputStream ois =
                        new ObjectInputStream(new FileInputStream(
                                new File(filepath)));

                // New in 1.0.7: Rails version & save date/time.
                // Allow for older saved file versions.

                Object object = ois.readObject();
                savedObjects.add(object);
                if (object instanceof String) {
                    add((String)object+" saved file "+filename);
                    object = ois.readObject();
                    savedObjects.add(object);
                } else {
                    add("Reading Rails (pre-1.0.7) saved file "+filename);
                }
                if (object instanceof String) {
                    add("File was saved at "+(String)object);
                    object = ois.readObject();
                    savedObjects.add(object);
                }

                long versionID = (Long) object;
                add("Saved versionID="+versionID+" (object="+object+")");
                long saveFileVersionID = GameManager.saveFileVersionID;
                String name = (String) ois.readObject();
                savedObjects.add(name);
                add("Saved game="+name);

                Map<String, String> selectedGameOptions =
                        (Map<String, String>) ois.readObject();
                savedObjects.add(selectedGameOptions);
                for (String key : selectedGameOptions.keySet()) {
                    add("Option "+key+"="+selectedGameOptions.get(key));
                }

                List<String> playerNames = (List<String>) ois.readObject();
                savedObjects.add(playerNames);
                int i=1;
                for (String player : playerNames) {
                    add("Player "+(i++)+": "+player);
                }

                Game game = new Game(name, playerNames, selectedGameOptions);

                if (!game.setup()) {
                    throw new ConfigurationException("Error in setting up " + name);
                }

                executedActions =
                        (List<PossibleAction>) ois.readObject();
                savedObjects.add(executedActions);
                i=1;
                for (PossibleAction action : executedActions) {
                    add("Action "+(i++)+": "+action.toString());
                }
                ois.close();
            } catch (Exception e) {
                System.out.println(e.getMessage());
                e.printStackTrace();
            }
        }

    }

    public void add (String text) {
        if (text.length() > 0) {
            reportText.append(text);
            reportText.append("\n");
            scrollDown();
        }
    }

    public void scrollDown () {
        SwingUtilities.invokeLater(new Runnable() {
            public void run() {
                messageWindow.vbar.setValue(messageWindow.vbar.getMaximum());
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
                    List<PossibleAction> toRemove = executedActions.subList(index, executedActions.size());
                    toRemove.clear();

                } catch (NumberFormatException e) {

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
            filepath = selectedFile.getPath();
            saveDirectory = selectedFile.getParent();

            try {
                try {
                    ObjectOutputStream oos =
                            new ObjectOutputStream(new FileOutputStream(new File(
                                    filepath)));
                    for (Object object : savedObjects) {
                        oos.writeObject(object);
                    }
                    oos.close();
                } catch (IOException e) {
                    log.error("Save failed", e);
                    DisplayBuffer.add(LocalText.getText("SaveFailed", e.getMessage()));
                }
           } catch (Exception e) {
                System.out.println ("Error whilst writing file "+filepath);
                e.printStackTrace();
            }
        }

    }

    public void keyPressed(KeyEvent e) {
    }

    public void keyReleased(KeyEvent e) {}

    public void keyTyped(KeyEvent e) {}

}
