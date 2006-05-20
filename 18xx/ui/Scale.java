package ui;

import java.awt.Dimension;
import java.awt.Toolkit;

/**
 * Class Scale holds static information used to scale all GUI elements.
 */

public final class Scale
{
	public static int scale = 15;

    static
    {
        fitScreenRes();
    }

    public static int get()
    {
        return scale;
    }

    public static void set(int scale)
    {
        Scale.scale = scale;
    }

    /** Set the scale so that the MasterBoard fits on the screen.
     *  Default scale should be 15 for screen resolutions with
     *  height 1000 or more.  For less, scale it down linearly. */
    public static void fitScreenRes()
    {
        Dimension d = Toolkit.getDefaultToolkit().getScreenSize();
        if (d.height < 1000)
        {
            scale = scale * d.height / 1000;
        }
    }
}
