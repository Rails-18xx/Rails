package ui.hexmap;


import java.awt.*;
import java.awt.event.*;
import javax.swing.*;

/**
 * Class BattleMap implements the GUI for a Titan battlemap.
 * @version $Id: BattleMap.java,v 1.4 2005/07/20 12:44:51 wakko666 Exp $
 * @author David Ripton
 */

public final class BattleMap extends HexMap implements MouseListener,
        WindowListener
{
    private Point location;
    private JFrame battleFrame;
    private JLabel playerLabel;
    private Cursor defaultCursor;
    private int scale = 13;

    public BattleMap()
    {
        super();

        battleFrame = new JFrame();
        battleFrame.setDefaultCloseOperation(JFrame.DO_NOTHING_ON_CLOSE);

        Container contentPane = battleFrame.getContentPane();
        contentPane.setLayout(new BorderLayout());

        battleFrame.addWindowListener(this);
        addMouseListener(this);

        if (location == null)
        {
            location = new Point(0, 4 * scale);
        }
        battleFrame.setLocation(location);

        contentPane.add(new JScrollPane(this), BorderLayout.CENTER);

        //setupPlayerLabel();
        //contentPane.add(playerLabel, BorderLayout.NORTH);

        defaultCursor = battleFrame.getCursor();

        // Do not call pack() or setVisible(true) until after
        // BattleDice is added to frame.
        
        battleFrame.setVisible(true);
        battleFrame.setSize(600, 600);
        battleFrame.repaint();
    }

    JFrame getFrame()
    {
        return battleFrame;
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

        GUIEWHex hex = getHexContainingPoint(point);
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
