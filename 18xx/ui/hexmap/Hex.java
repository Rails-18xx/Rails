/**
 * package net.sf.colossus.client;
 * 
 * Class Hex describes one general hex.
 * @version $Id: Hex.java,v 1.2 2005/07/21 11:15:23 wakko666 Exp $
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
    private double xCoord = -1;
    private double yCoord = -1;

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

    public double getXCoord()
    {
        return xCoord;
    }

    public void setXCoord(double xCoord)
    {
        this.xCoord = xCoord;
    }

    public double getYCoord()
    {
        return yCoord;
    }

    public void setYCoord(double yCoord)
    {
        this.yCoord = yCoord;
    }
}
