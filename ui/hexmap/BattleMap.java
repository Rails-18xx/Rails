package ui.hexmap;


import java.awt.*;
import java.awt.event.*;
import java.util.*;
import javax.swing.*;
import game.*;

/**
 * Class BattleMap implements the GUI for a Titan battlemap.
 * @version $Id: BattleMap.java,v 1.2 2005/06/28 05:25:40 wakko666 Exp $
 * @author David Ripton
 */

public final class BattleMap extends HexMap implements MouseListener,
        WindowListener
{
    private Point location;
    private JFrame battleFrame;
    private JMenuBar menuBar;
    private JMenu phaseMenu;
    private JLabel playerLabel;
    private Cursor defaultCursor;

    /** tag of the selected critter, or -1 if no critter is selected. */
    private int selectedCritterTag = -1;

    private static final String undoLast = "Undo Last";
    private static final String undoAll = "Undo All";
    private static final String doneWithPhase = "Done";
    private static final String concedeBattle = "Concede Battle";

    private AbstractAction undoLastAction;
    private AbstractAction undoAllAction;
    private AbstractAction doneWithPhaseAction;
    private AbstractAction concedeBattleAction;

    public BattleMap()
    {
        super();

        battleFrame = new JFrame();
        battleFrame.setDefaultCloseOperation(JFrame.DO_NOTHING_ON_CLOSE);

        Container contentPane = battleFrame.getContentPane();
        contentPane.setLayout(new BorderLayout());

        setupIcon();

        battleFrame.addWindowListener(this);
        addMouseListener(this);

        setupActions();
        setupTopMenu();

        if (location == null)
        {
            location = new Point(0, 4); // * Scale.get());
        }
        battleFrame.setLocation(location);

        contentPane.add(new JScrollPane(this), BorderLayout.CENTER);

        setupPlayerLabel();
        contentPane.add(playerLabel, BorderLayout.NORTH);

        defaultCursor = battleFrame.getCursor();

        // Do not call pack() or setVisible(true) until after
        // BattleDice is added to frame.
    }

    private void setupActions()
    {
        undoLastAction = new AbstractAction(undoLast)
        {
            public void actionPerformed(ActionEvent e)
            {
                    selectedCritterTag = -1;
            }
        };

        undoAllAction = new AbstractAction(undoAll)
        {
            public void actionPerformed(ActionEvent e)
            {
                    selectedCritterTag = -1;
                }
        };

        doneWithPhaseAction = new AbstractAction(doneWithPhase)
        {
            public void actionPerformed(ActionEvent e)
            {
                int phase = 0; //client.getBattlePhase();
                switch (phase)
                {
                    default:
                        Log.error("Bogus phase");
                }
            }
        };

        concedeBattleAction = new AbstractAction(concedeBattle)
        {
            public void actionPerformed(ActionEvent e)
            {
                String[] options = new String[2];
                options[0] = "Yes";
                options[1] = "No";
                int answer = JOptionPane.showOptionDialog(battleFrame,
                    "Are you sure you wish to concede the battle?",
                    "Confirm Concession?",
                    JOptionPane.YES_NO_OPTION, JOptionPane.QUESTION_MESSAGE,
                    null, options, options[1]);

                if (answer == JOptionPane.YES_OPTION)
                {
                }
                ;
            }
        };
    }

    private void setupTopMenu()
    {
        menuBar = new JMenuBar();
        battleFrame.setJMenuBar(menuBar);

        // Phase menu items change by phase and will be set up later.
        phaseMenu = new JMenu("Phase");
        phaseMenu.setMnemonic(KeyEvent.VK_P);
        menuBar.add(phaseMenu);
    }

    void setupSummonMenu()
    {
        if (battleFrame == null)
        {
            return;
        }

        phaseMenu.removeAll();

        reqFocus();
    }

    void setupRecruitMenu()
    {
        if (battleFrame == null)
        {
            return;
        }

        if (phaseMenu != null)
        {
            phaseMenu.removeAll();
        }

        reqFocus();
    }

    void setupMoveMenu()
    {
        if (battleFrame == null)
        {
            return;
        }

        phaseMenu.removeAll();

        JMenuItem mi;

        mi = phaseMenu.add(undoLastAction);
        mi.setMnemonic(KeyEvent.VK_U);
        mi.setAccelerator(KeyStroke.getKeyStroke(KeyEvent.VK_U, 0));

        mi = phaseMenu.add(undoAllAction);
        mi.setMnemonic(KeyEvent.VK_A);
        mi.setAccelerator(KeyStroke.getKeyStroke(KeyEvent.VK_A, 0));

        mi = phaseMenu.add(doneWithPhaseAction);
        mi.setMnemonic(KeyEvent.VK_D);
        mi.setAccelerator(KeyStroke.getKeyStroke(KeyEvent.VK_D, 0));

        phaseMenu.addSeparator();

        mi = phaseMenu.add(concedeBattleAction);
        mi.setMnemonic(KeyEvent.VK_C);
        mi.setAccelerator(KeyStroke.getKeyStroke(KeyEvent.VK_C, 0));

            reqFocus();
    }

    void setupFightMenu()
    {
        if (battleFrame == null)
        {
            return;
        }

        phaseMenu.removeAll();

        JMenuItem mi;

        mi = phaseMenu.add(doneWithPhaseAction);
        mi.setMnemonic(KeyEvent.VK_D);
        mi.setAccelerator(KeyStroke.getKeyStroke(KeyEvent.VK_D, 0));

        phaseMenu.addSeparator();

        mi = phaseMenu.add(concedeBattleAction);
        mi.setMnemonic(KeyEvent.VK_C);
        mi.setAccelerator(KeyStroke.getKeyStroke(KeyEvent.VK_C, 0));

            reqFocus();
    }

    JFrame getFrame()
    {
        return battleFrame;
    }

    private void setupIcon()
    {
        ArrayList directories = new java.util.ArrayList();

            Log.error("ERROR: Couldn't find Colossus icon");
            dispose();
    }

    /** Show which player owns this board. */
    void setupPlayerLabel()
    {
        playerLabel.repaint();
    }

    public static BattleHex getEntrance(String terrain,
        String masterHexLabel,
        int entrySide)
    {
        return HexMap.getHexByLabel(terrain, "X" + entrySide);
    }

    void setDefaultCursor()
    {
        battleFrame.setCursor(defaultCursor);
    }

    void setWaitCursor()
    {
        battleFrame.setCursor(Cursor.getPredefinedCursor(Cursor.WAIT_CURSOR));
    }

    public void mousePressed(MouseEvent e)
    {
        Point point = e.getPoint();

        GUIBattleHex hex = getHexContainingPoint(point);
        String hexLabel = "";
        if (hex != null)
        {
            hexLabel = hex.getHexModel().getLabel();
        }
    }

    public void windowClosing(WindowEvent e)
    {
        String[] options = new String[2];
        options[0] = "Yes";
        options[1] = "No";
        int answer = JOptionPane.showOptionDialog(battleFrame,
            "Are you sure you wish to quit?",
            "Quit Game?",
            JOptionPane.YES_NO_OPTION, JOptionPane.QUESTION_MESSAGE,
            null, options, options[1]);

        if (answer == JOptionPane.YES_OPTION)
        {
            System.exit(0);
        }
    }

    public void paintComponent(Graphics g)
    {
        super.paintComponent(g);
        Rectangle rectClip = g.getClipBounds();

        // Abort if called too early.
        if (rectClip == null)
        {
            return;
        }

    }

    void dispose()
    {
        location = battleFrame.getLocation();

        if (battleFrame != null)
        {
            battleFrame.dispose();
        }
    }

    void rescale()
    {
        setupHexes();

        setSize(getPreferredSize());
        battleFrame.pack();
        repaint();
    }

    public static String entrySideName(int side)
    {
        switch (side)
        {
            case 1:
            case 3:
            case 5:
            default:
                return "";
        }
    }

    public static int entrySideNum(String side)
    {
        if (side == null)
        {
            return -1;
        }
        else
        {
            return -1;
        }
    }

    void reqFocus()
    {
            requestFocus();
            getFrame().toFront();
    }
}
