package ui.hexmap;


import java.awt.*;
import java.awt.geom.*;
import java.awt.event.*;
import javax.swing.*;

/**
 * Class BattleMap implements the GUI for a Titan battlemap.
 * @version $Id: BattleMap.java,v 1.8 2005/09/22 21:35:07 wakko666 Exp $
 * @author David Ripton
 */

public final class BattleMap extends EWHexMap
{
    private Point2D.Double location;
    private JFrame battleFrame;
    private JLabel playerLabel;
    private Cursor defaultCursor;
    private int scale = 10;

    public BattleMap()
    {
        super();

        battleFrame = new JFrame();
        battleFrame.setDefaultCloseOperation(JFrame.DO_NOTHING_ON_CLOSE);

        Container contentPane = battleFrame.getContentPane();
        contentPane.setLayout(new BorderLayout());

        if (location == null)
        {
            location = new Point2D.Double(0, 2 * scale);
        }
        battleFrame.setLocation((int)location.getX(), (int)location.getY());

        contentPane.add(new JScrollPane(this), BorderLayout.CENTER);

        //setupPlayerLabel();
        //contentPane.add(playerLabel, BorderLayout.NORTH);

        defaultCursor = battleFrame.getCursor();

        // Do not call pack() or setVisible(true) until after
        // BattleDice is added to frame.
        
        battleFrame.setVisible(true);
        rescale();
    }

    JFrame getFrame()
    {
        return battleFrame;
    }

    public static BattleHex getEntrance(String terrain,
        String masterHexLabel,
        int entrySide)
    {
        return EWHexMap.getHexByLabel(terrain, "X" + entrySide);
    }

    void setDefaultCursor()
    {
        battleFrame.setCursor(defaultCursor);
    }

    void setWaitCursor()
    {
        battleFrame.setCursor(Cursor.getPredefinedCursor(Cursor.WAIT_CURSOR));
    }

    public void paintComponent(Graphics g)
    {
        super.paintComponent(g);
        /*
        Rectangle rectClip = g.getClipBounds();

        // Abort if called too early.
        if (rectClip == null)
        {
            return;
        }*/

    }

    void dispose()
    {
        Point p = battleFrame.getLocation();

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
