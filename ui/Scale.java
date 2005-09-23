package ui;


import java.awt.Dimension;
import java.awt.Toolkit;


/**
 * Class Scale holds static information used to scale all GUI elements.
 * @version $Id: Scale.java,v 1.1 2005/09/23 17:39:41 wakko666 Exp $
 * @author David Ripton
 */

final class Scale
{
    static int scale = 15;

    static
    {
        fitScreenRes();
    }

    static int get()
    {
        return scale;
    }

    static void set(int scale)
    {
        Scale.scale = scale;
    }

    /** Set the scale so that the MasterBoard fits on the screen.
     *  Default scale should be 15 for screen resolutions with
     *  height 1000 or more.  For less, scale it down linearly. */
    static void fitScreenRes()
    {
        Dimension d = Toolkit.getDefaultToolkit().getScreenSize();
        if (d.height < 1000)
        {
            scale = scale * d.height / 1000;
        }
    }
}
