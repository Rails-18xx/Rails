package ui.hexmap;

//This is only here because GUIMapHex depends on it for now.
//This will be going away or changing radically.

import java.awt.Color;

/**
 * Class BattleHex holds game state for battle hex.
 * @version $Id: BattleHex.java,v 1.3 2005/06/30 03:41:58 wakko666 Exp $
 * @author David Ripton
 * @author Romain Dolbeau
 */

public class BattleHex 
    extends Hex
    //implements net.sf.colossus.util.Terrains  // B_xxx constants
{

    /** Valid elevations are 0, 1, and 2.  Also 3 for JDG Badlands. */
    private int elevation;

    // Hexside terrain types are:
    // d, c, s, w, space
    // dune, cliff, slope, wall, no obstacle
    // also
    // r
    // river
    /**
     * The array of all the valid terrain type for a BattleHex Side.
     */
    private static final char[] allHexsides = { ' ', 'd', 'c', 's', 'w', 'r' };
    //private static final String[] allHexsides =
    //{ "Nothing", "Dune", "Cliff", "Slope", "Wall", "River"};

    /**
     * Hold the type of the six side of the BattleHex.
     * The hexside is marked only in the higher hex.
     */
    private char[] hexsides = new char[6];

    /**
     * Links to the neighbors of the BattleHex.
     * Neighbors have one hex side in common.
     * Non-existant neighbor are marked with <b>null</b>.
     */
    private BattleHex[] neighbors = new BattleHex[6];

    private int xCoord;
    private int yCoord;

    // Hex labels are:
    // A1-A3, B1-B4, C1-C5, D1-D6, E1-E5, F1-F4.
    // Letters increase left to right; numbers increase bottom to top.

    /** Movement costs */
    public static final int IMPASSIBLE_COST = 99;
    private static final int SLOW_COST = 2;
    private static final int NORMAL_COST = 1;
    private static final int SLOW_INCREMENT_COST = SLOW_COST - NORMAL_COST;

    public BattleHex(int xCoord, int yCoord)
    {
        this.xCoord = xCoord;
        this.yCoord = yCoord;

        for (int i = 0; i < 6; i++)
        {
            hexsides[i] = ' ';
        }

        //setTerrain(H_PLAINS);
        assignLabel();
    }

    public String getTerrainName()
    {
        if (elevation == 0)
        {
            return(getTerrain());
        }
        else
        {
            return(getTerrain() + " (" + elevation + ")");
        }
    }

    public Color getTerrainColor()
    {
       /*
        if (getTerrain().equals(H_PLAINS))
        {
            switch (elevation)
            {
                case 0:
                    return HTMLColor.lightOlive;

                case 1:
                    return HTMLColor.darkYellow;

                case 2:
                    return Color.yellow;

                default:
                case 3:
                    return HTMLColor.lightYellow;
            }
        }
        else if (getTerrain().equals(H_TOWER))
        {
            switch (elevation)
            {
                case 0:
                    return HTMLColor.dimGray;

                case 1:
                    return HTMLColor.darkGray;

                case 2:
                    return Color.gray;

                default:
                case 3:
                    return HTMLColor.lightGray;
            }
        }
        else if (getTerrain().equals(H_BRAMBLES))
        {
            switch (elevation)
            {
                case 0:
                    return Color.green;

                case 1:
                    return HTMLColor.brambleGreen1;

                case 2:
                    return HTMLColor.brambleGreen2;

                default:
                case 3:
                    return HTMLColor.darkGreen;
            }
        }
        else if (getTerrain().equals(H_SAND))
        {
            return Color.orange;
        }
        else if (getTerrain().equals(H_TREE))
        {
            return HTMLColor.brown;
        }
        else if (getTerrain().equals(H_BOG))
        {
            return Color.gray;
        }
        else if (getTerrain().equals(H_VOLCANO))
        {
            switch (elevation)
            {
                case 3:
                    return Color.red;

                default:
                case 2:
                    return HTMLColor.darkRed;
            }
        }
        else if (getTerrain().equals(H_DRIFT))
        {
            return Color.blue;
        }
        else if (getTerrain().equals(H_LAKE))
        {
            return HTMLColor.skyBlue;
        }
        else if (getTerrain().equals(H_STONE))
        {
            return HTMLColor.dimGray;
        }
        else
        {
        */
            return Color.white;
       /*
        }
        */
    }

    private void assignLabel()
    {
        String label;
        if (xCoord < 0)   // towi: changed from ?== -1? to ?< 0?.
        {
            label = "X" + yCoord;
        }
        else
        {                
            final int yLabel = 6 - yCoord - (int)Math.abs(((xCoord - 3) / 2));
            label = "" + _intXCoordToXLabel(xCoord) + yLabel;
        }
        setLabel(label);
    }

    /** a char for an int: 0:'A'=0, 1:'B', ... int(w):'W', else:'?', <0:undef.
     * towi: support from 'A'..'W'; old "switch" was 'A'..'F'. 
     * */ 
    private final static char _intXCoordToXLabel(final int x) 
    {
        return (x < (int)'X')    // 'X' is used for -1 
            ? (char)((int)'A' + x)
            : '?' ; 
    }

    final void testXLabel() 
    {            
    }

    public void setHexside(int i, char hexside)
    {
        this.hexsides[i] = hexside;
    }

    public char getHexside(int i)
    {
        if (i >= 0 && i <= 5)
        {
            return hexsides[i];
        }
        else
        {
            //Log.warn("Called BattleHex.getHexside() with " + i);
            return '?';
        }
    }

    public String getHexsideName(int i)
    {
        switch (hexsides[i])
        {
            default:
            case ' ':
                return("Nothing");

            case 'd':
                return("Dune");

            case 'c':
                return("Cliff");

            case 's':
                return("Slope");

            case 'w':
                return("Wall");

            case 'r':
                return("River");
        }
    }

    /** Return the flip side of hexside i. */
    public char getOppositeHexside(int i)
    {
        char hexside = ' ';

        BattleHex neighbor = getNeighbor(i);
        if (neighbor != null)
        {
            hexside = neighbor.getHexside((i + 3) % 6);
        }

        return hexside;
    }

    public int getElevation()
    {
        return elevation;
    }

    public void setElevation(int elevation)
    {
        this.elevation = elevation;
    }

    public BattleHex getNeighbor(int i)
    {
        if (i < 0 || i > 6)
        {
            return null;
        }
        else
        {
            return neighbors[i];
        }
    }

    public void setNeighbor(int i, BattleHex hex)
    {
        if (i >= 0 && i < 6)
        {
            neighbors[i] = hex;
        }
    }

    public int getXCoord()
    {
        return xCoord;
    }

    public int getYCoord()
    {
        return yCoord;
    }

    public boolean isEntrance()
    {
        return (xCoord == -1);
    }


    public boolean isCliff(int hexside)
    {
        return getHexside(hexside) == 'c' ||
                getOppositeHexside(hexside) == 'c';
    }

    public static char[] getHexsides()
    {
        return allHexsides;
    }
}

