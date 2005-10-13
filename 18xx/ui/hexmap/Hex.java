/**
 * package net.sf.colossus.client;
 * 
 * Class Hex describes one general hex.
 * @version $Id: Hex.java,v 1.4 2005/10/13 18:57:03 wakko666 Exp $
 * @author David Ripton
 */

package ui.hexmap;

public abstract class Hex
{
    /** The hex vertexes are numbered like this:
    
                  0---------1
                 /           \
                /             \
               /               \
              /                 \
             5                   2
              \                 /
               \               /
                \             /
                 \           /
                  4---------3
    
	   Some basic hexagon math as a reminder:
	
	   Length of each side of the hexagon: 1	   
	   Distance to center: 1
	   Size of each interior angle: 120 degrees
	   Vertical distance from 1 to 2: COS(30), SIN(60), SQRT(3)/2
	   Horizontal distance from 1 to 2: SIN(30), COS(60), 1/2
	   Vertical distance from 1 to 3: SQRT(3)
	   Horizontal distance from 5 to 2: 2
	   
	*/   
	
	
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
