/* $Header: /Users/blentz/rails_rcs/cvs/18xx/rails/ui/swing/ReportWindow.java,v 1.14 2009/11/29 15:46:14 evos Exp $*/
package rails.ui.swing;

import java.awt.*;
import java.awt.event.*;
import java.io.*;

import javax.swing.*;

import org.apache.log4j.Logger;

import rails.game.GameManagerI;
import rails.game.ReportBuffer;
import rails.ui.swing.elements.ActionMenuItem;
import rails.util.*;

/**
 * This is the UI for the LogWindow. It displays logged messages to the user
 * during the rails.game.
 */
public class ReportWindow extends AbstractReportWindow implements ActionListener, KeyListener {

    private static final long serialVersionUID = 1L;
    private JTextArea reportText;
    private JScrollPane messageScroller;
    private JScrollBar vbar;
    private JPanel messagePanel;
    private ReportWindow messageWindow;
    private JMenuBar menuBar;
    private JMenu fileMenu, editMenu;
    private JMenuItem saveItem, loadItem, printItem;
    private JMenuItem findItem, findBackItem, findNextItem, findPrevItem;

    private GameManagerI gameManager;

    private String reportDirectory = Config.get("report.directory");
    private String reportFile;

    private boolean editable = "yes".equalsIgnoreCase(Config.get("report.window.editable"));

    protected static final String SAVE_CMD = "Save";
    protected static final String LOAD_CMD = "Load";
    protected static final String PRINT_CMD = "Print";
    protected static final String FIND_CMD = "Find";
    protected static final String FIND_BACK_CMD = "FindBack";
    protected static final String FIND_NEXT_CMD = "FindNext";
    protected static final String FIND_PREV_CMD = "FindPrev";

    protected static Logger log =
        Logger.getLogger(ReportWindow.class.getPackage().getName());


    public ReportWindow(GameUIManager gameUIManager) {
        super(gameUIManager);
        messageWindow = this;
        this.gameManager = gameUIManager.getGameManager();

        reportText = new JTextArea();
        reportText.setEditable(editable);
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
        loadItem.setActionCommand(LOAD_CMD);
        loadItem.setMnemonic(KeyEvent.VK_L);
        loadItem.setAccelerator(KeyStroke.getKeyStroke(KeyEvent.VK_L,
                ActionEvent.ALT_MASK));
        loadItem.addActionListener(this);
        loadItem.setEnabled(true);
        fileMenu.add(loadItem);

        saveItem = new ActionMenuItem(LocalText.getText("SAVE"));
        saveItem.setActionCommand(SAVE_CMD);
        saveItem.setMnemonic(KeyEvent.VK_S);
        saveItem.setAccelerator(KeyStroke.getKeyStroke(KeyEvent.VK_S,
                ActionEvent.ALT_MASK));
        saveItem.addActionListener(this);
        saveItem.setEnabled(true);
        fileMenu.add(saveItem);

        printItem = new ActionMenuItem(LocalText.getText("PRINT"));
        printItem.setActionCommand(PRINT_CMD);
        printItem.setMnemonic(KeyEvent.VK_P);
        printItem.setAccelerator(KeyStroke.getKeyStroke(KeyEvent.VK_P,
                ActionEvent.ALT_MASK));
        printItem.addActionListener(this);
        printItem.setEnabled(false);
        fileMenu.add(printItem);

        findItem = new ActionMenuItem(LocalText.getText("FIND"));
        findItem.setActionCommand(FIND_CMD);
        findItem.setMnemonic(KeyEvent.VK_F);
        findItem.setAccelerator(KeyStroke.getKeyStroke(KeyEvent.VK_F,
                ActionEvent.CTRL_MASK));
        findItem.addActionListener(this);
        findItem.setEnabled(true);
        editMenu.add(findItem);

        findBackItem = new ActionMenuItem(LocalText.getText("FIND_BACK"));
        findBackItem.setActionCommand(FIND_BACK_CMD);
        findBackItem.setMnemonic(KeyEvent.VK_B);
        findBackItem.setAccelerator(KeyStroke.getKeyStroke(KeyEvent.VK_F,
                ActionEvent.CTRL_MASK | ActionEvent.SHIFT_MASK));
        findBackItem.addActionListener(this);
        findBackItem.setEnabled(true);
        editMenu.add(findBackItem);

        findNextItem = new ActionMenuItem(LocalText.getText("FIND_NEXT"));
        findNextItem.setActionCommand(FIND_NEXT_CMD);
        findNextItem.setMnemonic(KeyEvent.VK_N);
        findNextItem.setAccelerator(KeyStroke.getKeyStroke(KeyEvent.VK_G,
                ActionEvent.CTRL_MASK));
        findNextItem.addActionListener(this);
        findNextItem.setEnabled(true);
        editMenu.add(findNextItem);

        findPrevItem = new ActionMenuItem(LocalText.getText("FIND_PREV"));
        findPrevItem.setActionCommand(FIND_PREV_CMD);
        findPrevItem.setMnemonic(KeyEvent.VK_P);
        findPrevItem.setAccelerator(KeyStroke.getKeyStroke(KeyEvent.VK_G,
                ActionEvent.CTRL_MASK | ActionEvent.SHIFT_MASK));
        findPrevItem.addActionListener(this);
        findPrevItem.setEnabled(true);
        editMenu.add(findPrevItem);

        menuBar.add(fileMenu);
        menuBar.add(editMenu);

        setJMenuBar(menuBar);

        setContentPane(messagePanel);

        addKeyListener(this);

        // default report window settings
        super.init();

    }

    /* (non-Javadoc)
     * @see rails.ui.swing.ReportWindowI#updateLog()
     */
    @Override
    public void updateLog() {
        String newText = ReportBuffer.get();
        if (newText.length() > 0) {
            reportText.append(newText);
            scrollDown();
        }
    }

    @Override
    public void scrollDown () {
        SwingUtilities.invokeLater(new Runnable() {
            public void run() {
                messageWindow.vbar.setValue(messageWindow.vbar.getMaximum());
            }
        });
    }

    public void actionPerformed(ActionEvent actor) {
        String command = actor.getActionCommand();
        if (LOAD_CMD.equalsIgnoreCase(command)) {
            loadReportFile();
        } else if (SAVE_CMD.equalsIgnoreCase(command)) {
            saveReportFile();
        } else if (FIND_CMD.equalsIgnoreCase(command)) {
            findText(false);
        } else if (FIND_BACK_CMD.equalsIgnoreCase(command)) {
            findText(true);
        } else if (FIND_NEXT_CMD.equalsIgnoreCase(command)) {
            findNext(false);
        } else if (FIND_PREV_CMD.equalsIgnoreCase(command)) {
            findNext(true);
        }
    }

    private void loadReportFile() {

        JFileChooser jfc = new JFileChooser();
        if (Util.hasValue(reportDirectory))
        jfc.setCurrentDirectory(new File(reportDirectory));
        File selectedFile;

        if (jfc.showOpenDialog(getContentPane()) == JFileChooser.APPROVE_OPTION) {
            selectedFile = jfc.getSelectedFile();
            reportFile = selectedFile.getPath();
            reportDirectory = selectedFile.getParent();
        } else {
            return;
        }

        try {
            BufferedReader in = new BufferedReader (new FileReader(selectedFile));
            String line;
            StringBuffer b = new StringBuffer();
            while ((line = in.readLine()) != null) b.append(line).append("\n");
            in.close();
            reportText.setText(b.toString());
        } catch (IOException e) {
            log.error ("Error whilst reading file "+reportFile, e);
            JOptionPane.showMessageDialog(this,
                    e.getMessage(), "", JOptionPane.ERROR_MESSAGE);
        }
    }

    private void saveReportFile () {

        JFileChooser jfc = new JFileChooser();
        if (Util.hasValue(reportDirectory)) {
            jfc.setCurrentDirectory(new File(reportDirectory));
        }
        if (Util.hasValue(reportFile)) {
            jfc.setSelectedFile(new File(reportFile));
        }
        if (jfc.showSaveDialog(this) == JFileChooser.APPROVE_OPTION) {
            File selectedFile = jfc.getSelectedFile();
            String filepath = selectedFile.getPath();
            reportDirectory = selectedFile.getParent();
            if (!selectedFile.getName().equalsIgnoreCase(reportFile)) {
                reportFile = filepath;
            }

            try {
                PrintWriter out = new PrintWriter (new FileWriter (new File (reportFile)));
                out.print(reportText.getText());
                out.close();
            } catch (IOException e) {
                log.error ("Error whilst writing file "+reportFile, e);
                JOptionPane.showMessageDialog(this,
                        e.getMessage(), "", JOptionPane.ERROR_MESSAGE);
            }
        }
    }

    private void findText(boolean backwards) {

        String text = reportText.getText();
        String target = JOptionPane.showInputDialog(reportText,
                LocalText.getText("EnterSearch"));
        if (!Util.hasValue(target)) return;

        int startPos = editable
                ? reportText.getCaretPosition()
                : backwards ? text.length() : 0;
        int foundPos = backwards
                ? text.lastIndexOf(target, startPos)
                : text.indexOf(target, startPos);
        if (foundPos < 0) return;

        reportText.select(foundPos, foundPos + target.length());
    }

    private void findNext(boolean backwards) {

        String text = reportText.getText();
        String target = reportText.getSelectedText();
        if (!Util.hasValue(target)) return;

        int startPos = reportText.getSelectionStart();
        int foundPos = backwards
                ? text.lastIndexOf(target, startPos-1)
                : text.indexOf(target, startPos+1);
        if (foundPos < 0) return;

        reportText.select(foundPos, foundPos + target.length());
    }

    public void keyPressed(KeyEvent e) {
        if (e.getKeyCode() == KeyEvent.VK_F1) {
            HelpWindow.displayHelp(gameManager.getHelp());
            e.consume();
        }
    }

    public void keyReleased(KeyEvent e) {}

    public void keyTyped(KeyEvent e) {}

}
