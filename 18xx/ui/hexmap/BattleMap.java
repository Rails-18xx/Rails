package ui.hexmap;


import java.awt.*;
import java.awt.geom.*;
import java.awt.event.*;
import javax.swing.*;

/**
 * Class BattleMap implements the GUI for a Titan battlemap.
 * @version $Id: BattleMap.java,v 1.6 2005/08/04 07:59:47 wakko666 Exp $
 * @author David Ripton
 */

public final class BattleMap extends EWHexMap implements MouseListener,
        WindowListener
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

        battleFrame.addWindowListener(this);
        addMouseListener(this);

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
        battleFrame.pack();
        battleFrame.setSize(600, 600);
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

    public void mousePressed(MouseEvent e)
    {
        Point point = e.getPoint();

        GUIHex hex = getHexContainingPoint(point);
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
