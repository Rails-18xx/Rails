/* $Header: /Users/blentz/rails_rcs/cvs/18xx/rails/ui/swing/ReportWindow.java,v 1.13 2009/11/28 22:41:04 evos Exp $*/
package rails.ui.swing;

import java.awt.*;
import java.awt.event.*;
import java.io.BufferedReader;
import java.io.File;
import java.io.FileReader;
import java.io.FileWriter;
import java.io.IOException;
import java.io.PrintWriter;
import java.util.Date;

import javax.swing.*;

import org.apache.log4j.Logger;

import rails.game.DisplayBuffer;
import rails.game.Game;
import rails.game.GameManager;
import rails.game.GameManagerI;
import rails.game.ReportBuffer;
import rails.ui.swing.elements.ActionMenuItem;
import rails.util.Config;
import rails.util.LocalText;
import rails.util.Util;

/**
 * This is the UI for the LogWindow. It displays logged messages to the user
 * during the rails.game.
 */
public class ReportWindow extends JFrame implements ActionListener, KeyListener {

    private static final long serialVersionUID = 1L;
    private JTextArea reportText;
    private JScrollPane messageScroller;
    private JScrollBar vbar;
    private JPanel messagePanel;
    private ReportWindow messageWindow;
    private JMenuBar menuBar;
    private JMenu fileMenu, editMenu;
    private JMenuItem saveItem, loadItem, printItem, findItem;

    private GameManagerI gameManager;
    
    private String reportDirectory = Config.get("report.directory");
    private String reportFile;
    
    private boolean editable = "yes".equalsIgnoreCase(Config.get("report.window.editable"));

    protected static final String SAVE_CMD = "Save";
    protected static final String LOAD_CMD = "Load";
    protected static final String PRINT_CMD = "Print";
    protected static final String FIND_CMD = "Find";
    
    protected static Logger log =
        Logger.getLogger(ReportWindow.class.getPackage().getName());

   
    public ReportWindow(GameManagerI gameManager) {
        messageWindow = this;
        this.gameManager = gameManager;

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

        menuBar.add(fileMenu);
        
        setJMenuBar(menuBar);
        
        setContentPane(messagePanel);

        setSize(400, 400);
        setLocation(600, 400);
        setTitle(LocalText.getText("GameReportTitle"));

        final JFrame frame = this;
        addWindowListener(new WindowAdapter() {
            @Override
            public void windowClosing(WindowEvent e) {
                StatusWindow.uncheckMenuItemBox(StatusWindow.REPORT_CMD);
                frame.dispose();
            }
        });
        addKeyListener(this);
        setVisible("yes".equalsIgnoreCase(Config.get("report.window.open")));
    }

    public void addLog() {
        String newText = ReportBuffer.get();
        if (newText.length() > 0) {
            reportText.append(newText);
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
        if (LOAD_CMD.equalsIgnoreCase(command)) {
            loadReportFile();
        } else if (SAVE_CMD.equalsIgnoreCase(command)) {
            saveReportFile();
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
    
    public void keyPressed(KeyEvent e) {
        if (e.getKeyCode() == KeyEvent.VK_F1) {
            HelpWindow.displayHelp(gameManager.getHelp());
            e.consume();
        }
    }

    public void keyReleased(KeyEvent e) {}

    public void keyTyped(KeyEvent e) {}

}
