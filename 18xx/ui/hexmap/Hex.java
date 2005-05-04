/**
 * package net.sf.colossus.client;
 * 
 * Class Hex describes one general hex.
 * @version $Id: Hex.java,v 1.1 2005/05/04 17:53:02 wakko666 Exp $
 * @author David Ripton
 */

package ui.hexmap;

public abstract class Hex
{
    // The hex vertexes are numbered like this:
    //
    //              0---------1
    //             /           \
    //            /             \
    //           /               \
    //          /                 \
    //         5                   2
    //          \                 /
    //           \               /
    //            \             /
    //             \           /
    //              4---------3

    // Game state variables
    private String baseName = "";
    private String label = "";  // Avoid null pointer in stringWidth()
    private int xCoord = -1;
    private int yCoord = -1;

    public String getTerrain()
    {
        return baseName;
    }

    public void setTerrain(String bn)
    {
        baseName = bn;
    }

    public String getLabel()
    {
        return label;
    }

    public void setLabel(String label)
    {
        this.label = label;
    }

    public abstract String getTerrainName();

    public String getDescription()
    {
        return getTerrainName() + " hex " + getLabel();
    }

    public String toString()
    {
        return getDescription();
    }

    public int getXCoord()
    {
        return xCoord;
    }

    public void setXCoord(int xCoord)
    {
        this.xCoord = xCoord;
    }

    public int getYCoord()
    {
        return yCoord;
    }

    public void setYCoord(int yCoord)
    {
        this.yCoord = yCoord;
    }
}
